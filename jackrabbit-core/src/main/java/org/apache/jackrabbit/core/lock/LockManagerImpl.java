/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.lock;

import EDU.oswego.cs.dl.util.concurrent.ReentrantLock;
import org.apache.commons.collections.map.LinkedMap;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.SessionListener;
import org.apache.jackrabbit.core.TransactionContext;
import org.apache.jackrabbit.core.WorkspaceImpl;
import org.apache.jackrabbit.core.cluster.ClusterOperation;
import org.apache.jackrabbit.core.cluster.LockEventChannel;
import org.apache.jackrabbit.core.cluster.LockEventListener;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.core.observation.EventImpl;
import org.apache.jackrabbit.core.observation.SynchronousEventListener;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.UpdatableItemStateManager;
import org.apache.jackrabbit.core.util.Dumpable;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.PathMap;
import org.apache.jackrabbit.api.jsr283.Workspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.transaction.xa.Xid;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Provides the functionality needed for locking and unlocking nodes.
 */
public class LockManagerImpl implements LockManager, SynchronousEventListener,
        LockEventListener, Dumpable {

    /**
     * Logger
     */
    private static final Logger log = LoggerFactory.getLogger(LockManagerImpl.class);

    /**
     * Name of the lock file
     */
    private static final String LOCKS_FILE = "locks";

    /**
     * Path map containing all locks at the leaves.
     */
    private final PathMap lockMap = new PathMap();

    /**
     * Thread aware lock to path map.
     */
    private final ReentrantLock lockMapLock = new ReentrantLock();
    
    /**
     * Xid aware lock to path map.
     */
    private final ReentrantLock xidlockMapLock = new ReentrantLock(){

    	/**
    	 * The actice Xid of this {@link ReentrantLock}
    	 */
        private Xid activeXid;

        /**
         * Check if the given Xid comes from the same globalTX
         * @param otherXid
         * @return true if same globalTX otherwise false
         */
        boolean isSameGlobalTx(Xid otherXid) {
    	    return (activeXid == otherXid) || Arrays.equals(activeXid.getGlobalTransactionId(), otherXid.getGlobalTransactionId());
    	}
        
        /**
         * {@inheritDoc}
         */
        public void acquire() throws InterruptedException {
        	if (Thread.interrupted()) throw new InterruptedException();
        	Xid currentXid = TransactionContext.getCurrentXid();
            synchronized(this) {
            	if (currentXid == activeXid || (activeXid != null && isSameGlobalTx(currentXid))) { 
                ++holds_;
            	} else {
            		try {  
            			while (activeXid != null) 
            				wait(); 
            			activeXid = currentXid;
            			holds_ = 1;
            		} catch (InterruptedException ex) {
            			notify();
            			throw ex;
            		}
            	}
            }
        }
        
        /**
         * {@inheritDoc}
         */
        public synchronized void release()  {
        	Xid currentXid = TransactionContext.getCurrentXid();
            if (activeXid != null && !isSameGlobalTx(currentXid))
                throw new Error("Illegal Lock usage"); 

              if (--holds_ == 0) {
                activeXid = null;
                notify(); 
              }
        }
    };

    /**
     * System session
     */
    private final SessionImpl sysSession;

    /**
     * Locks file
     */
    private final FileSystemResource locksFile;

    /**
     * Flag indicating whether automatic saving is disabled.
     */
    private boolean savingDisabled;

    /**
     * Lock event channel.
     */
    private LockEventChannel eventChannel;

    /**
     * Create a new instance of this class.
     *
     * @param session system session
     * @param fs      file system for persisting locks
     * @throws RepositoryException if an error occurs
     */
    public LockManagerImpl(SessionImpl session, FileSystem fs)
            throws RepositoryException {

        this.sysSession = session;
        this.locksFile = new FileSystemResource(fs, FileSystem.SEPARATOR + LOCKS_FILE);

        session.getWorkspace().getObservationManager().
                addEventListener(this, Event.NODE_ADDED | Event.NODE_REMOVED,
                        "/", true, null, null, true);

        try {
            if (locksFile.exists()) {
                load();
            }
        } catch (FileSystemException e) {
            throw new RepositoryException("I/O error while reading locks from '"
                    + locksFile.getPath() + "'", e);
        }
    }

    /**
     * Close this lock manager. Writes back all changes.
     */
    public void close() {
        save();
    }

    /**
     * Read locks from locks file and populate path map
     */
    private void load() throws FileSystemException {
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(
                    new InputStreamReader(locksFile.getInputStream()));
            while (true) {
                String s = reader.readLine();
                if (s == null || s.equals("")) {
                    break;
                }
                reapplyLock(LockToken.parse(s));
            }
        } catch (IOException e) {
            throw new FileSystemException("error while reading locks file", e);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    /**
     * Reapply a lock given a lock token that was read from the locks file
     *
     * @param lockToken lock token to apply
     */
    private void reapplyLock(LockToken lockToken) {
        try {
            NodeImpl node = (NodeImpl)
                sysSession.getItemManager().getItem(lockToken.getId());
            Path path = getPath(sysSession, lockToken.getId());

            LockInfo info = new LockInfo(lockToken, false,
                    node.getProperty(NameConstants.JCR_LOCKISDEEP).getBoolean(),
                    node.getProperty(NameConstants.JCR_LOCKOWNER).getString());
            info.setLive(true);
            lockMap.put(path, info);
        } catch (RepositoryException e) {
            log.warn("Unable to recreate lock '" + lockToken
                    + "': " + e.getMessage());
            log.debug("Root cause: ", e);
        }
    }

    /**
     * Write locks to locks file
     */
    private void save() {
        if (savingDisabled) {
            return;
        }

        final ArrayList list = new ArrayList();

        lockMap.traverse(new PathMap.ElementVisitor() {
            public void elementVisited(PathMap.Element element) {
                LockInfo info = (LockInfo) element.get();
                if (!info.sessionScoped) {
                    list.add(info);
                }
            }
        }, false);


        BufferedWriter writer = null;

        try {
            writer = new BufferedWriter(
                    new OutputStreamWriter(locksFile.getOutputStream()));
            for (int i = 0; i < list.size(); i++) {
                AbstractLockInfo info = (AbstractLockInfo) list.get(i);
                writer.write(info.lockToken.toString());
                writer.newLine();
            }
        } catch (FileSystemException fse) {
            log.warn("I/O error while saving locks to '"
                    + locksFile.getPath() + "': " + fse.getMessage());
            log.debug("Root cause: ", fse);
        } catch (IOException ioe) {
            log.warn("I/O error while saving locks to '"
                    + locksFile.getPath() + "': " + ioe.getMessage());
            log.debug("Root cause: ", ioe);
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    static SessionLockManager getSessionLockManager(SessionImpl session) throws RepositoryException {
        Workspace wsp = (Workspace) session.getWorkspace();
        return (SessionLockManager) wsp.getLockManager();
    }

    /**
     * Internal <code>lock</code> implementation that takes the same parameters
     * as the public method.
     *
     * @param node node to lock
     * @param isDeep whether the lock applies to this node only
     * @param isSessionScoped whether the lock is session scoped
     * @param timeoutHint
     * @param ownerInfo
     * @return lock
     * @throws LockException       if the node is already locked
     * @throws RepositoryException if another error occurs
     */
    AbstractLockInfo internalLock(NodeImpl node, boolean isDeep,
                                  boolean isSessionScoped, long timeoutHint,
                                  String ownerInfo)
            throws LockException, RepositoryException {

        SessionImpl session = (SessionImpl) node.getSession();
        String lockOwner = (ownerInfo != null) ? ownerInfo : session.getUserID();
        LockInfo info = new LockInfo(new LockToken(node.getNodeId()),
                isSessionScoped, isDeep, lockOwner, timeoutHint);

        ClusterOperation operation = null;
        boolean successful = false;

        // Cluster is only informed about open-scoped locks
        if (eventChannel != null && !isSessionScoped) {
            operation = eventChannel.create(node.getNodeId(), isDeep, lockOwner);
        }

        acquire();

        try {
            // check whether node is already locked
            Path path = getPath(session, node.getId());
            PathMap.Element element = lockMap.map(path, false);

            LockInfo other = (LockInfo) element.get();
            if (other != null) {
                if (element.hasPath(path)) {
                    throw new LockException("Node already locked: " + node);
                } else if (other.deep) {
                    throw new LockException(
                            "Parent node has a deep lock: " + node);
                }
            }
            if (info.deep && element.hasPath(path)
                    && element.getChildrenCount() > 0) {
                throw new LockException("Some child node is locked.");
            }

            // create lock token
            info.setLockHolder(session);
            info.setLive(true);
            session.addListener(info);
            if (!info.isSessionScoped()) {
                getSessionLockManager(session).lockTokenAdded(info.lockToken.toString());
            }
            lockMap.put(path, info);

            if (!info.sessionScoped) {
                save();
                successful = true;
            }
            return info;

        } finally {
            release();
            if (operation != null) {
                operation.ended(successful);
            }
        }
    }

    /**
     * Unlock a node (internal implementation)
     * @param node node to unlock
     * @throws LockException       if the node can not be unlocked
     * @throws RepositoryException if another error occurs
     */
    boolean internalUnlock(NodeImpl node)
            throws LockException, RepositoryException {

        ClusterOperation operation = null;
        boolean successful = false;

        if (eventChannel != null) {
            operation = eventChannel.create(node.getNodeId());
        }

        acquire();

        try {
            SessionImpl session = (SessionImpl) node.getSession();
            // check whether node is locked by this session
            PathMap.Element element = lockMap.map(getPath(session, node.getId()), true);
            if (element == null) {
                throw new LockException("Node not locked: " + node);
            }
            AbstractLockInfo info = (AbstractLockInfo) element.get();
            if (info == null) {
                throw new LockException("Node not locked: " + node);
            }
            if (session != info.getLockHolder()) {
                throw new LockException("Node not locked by session: " + node);
            }

            getSessionLockManager(session).lockTokenRemoved(info.getLockToken(session));
            
            element.set(null);
            info.setLive(false);

            if (!info.sessionScoped) {
                save();
                successful = true;
            }
            return true;
        } finally {
            release();

            if (operation != null) {
                operation.ended(successful);
            }
        }
    }

    /**
     * Package-private low-level helper method returning all
     * <code>AbstractLockInfo</code>s associated with the specified
     * session.
     * @param session session
     * @return an array of <code>AbstractLockInfo</code>s
     */
    AbstractLockInfo[] getLockInfos(final SessionImpl session) {
        final ArrayList infos = new ArrayList();
        lockMap.traverse(new PathMap.ElementVisitor() {
            public void elementVisited(PathMap.Element element) {
                LockInfo info = (LockInfo) element.get();
                if (info.isLive() && info.getLockHolder().equals(session)) {
                    infos.add(info);
                }
            }
        }, false);
        return (AbstractLockInfo[]) infos.toArray(new AbstractLockInfo[infos.size()]);
    }

    /**
     * Helper method that copies all the active open-scoped locks from the
     * given source to this lock manager. This method is used when backing
     * up repositories, and only works correctly when the source lock manager
     * belongs to the original copy of the workspace being backed up.
     *
     * @see org.apache.jackrabbit.core.RepositoryCopier
     * @param source source lock manager
     */
    public void copyOpenScopedLocksFrom(LockManagerImpl source) {
        source.lockMap.traverse(new PathMap.ElementVisitor() {
            public void elementVisited(PathMap.Element element) {
                LockInfo info = (LockInfo) element.get();
                if (info.isLive() && !info.isSessionScoped()) {
                    try {
                        lockMap.put(element.getPath(), info);
                    } catch (MalformedPathException e) {
                        log.warn("Ignoring invalid lock path: " + info, e);
                    }
                }
            }
        }, false);
    }

    /**
     * Return the most appropriate lock information for a node. This is either
     * the lock info for the node itself, if it is locked, or a lock info for one
     * of its parents, if that is deep locked.
     * @return lock info or <code>null</code> if node is not locked
     * @throws RepositoryException if an error occurs
     */
    public AbstractLockInfo getLockInfo(NodeId id) throws RepositoryException {
        Path path;
        try {
            path = getPath(sysSession, id);
        } catch (ItemNotFoundException e) {
            return null;
        }

        acquire();
        try {
            PathMap.Element element = lockMap.map(path, false);
            AbstractLockInfo info = (AbstractLockInfo) element.get();
            if (info != null) {
                if (element.hasPath(path) || info.deep) {
                    return info;
                }
            }
            return null;
        } finally {
            release();
        }
    }

    //----------------------------------------------------------< LockManager >

    /**
     * {@inheritDoc}
     */
    public Lock lock(NodeImpl node, boolean isDeep, boolean isSessionScoped)
            throws LockException, RepositoryException {
        return lock(node, isDeep, isSessionScoped, AbstractLockInfo.TIMEOUT_INFINITE, null);
    }

    public Lock lock(NodeImpl node, boolean isDeep, boolean isSessionScoped, long timoutHint, String ownerInfo)
            throws LockException, RepositoryException {
        AbstractLockInfo info = internalLock(node, isDeep, isSessionScoped, timoutHint, ownerInfo);
        writeLockProperties(node, info.lockOwner, info.deep);

        return new LockImpl(info, node);
    }

    /**
     * {@inheritDoc}
     */
    public Lock getLock(NodeImpl node)
            throws LockException, RepositoryException {

        acquire();

        try {
            SessionImpl session = (SessionImpl) node.getSession();
            Path path = getPath(session, node.getId());

            PathMap.Element element = lockMap.map(path, false);
            AbstractLockInfo info = (AbstractLockInfo) element.get();
            if (info != null && (element.hasPath(path) || info.deep)) {
                Node lockHolder = (Node) session.getItemManager().getItem(info.getId());
                return new LockImpl(info, lockHolder);
            } else {
                throw new LockException("Node not locked: " + node);
            }
        } catch (ItemNotFoundException e) {
            throw new LockException("Node not locked: " + node);
        } finally {
            release();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Lock[] getLocks(SessionImpl session) throws RepositoryException {

        acquire();

        AbstractLockInfo[] infos = getLockInfos(session);

        try {
            Lock[] locks = new Lock[infos.length];
            for (int i = 0; i < infos.length; i++) {
                AbstractLockInfo info = infos[i];
                Node holder = (Node) session.getItemManager().getItem(info.getId());
                locks[i] = new LockImpl(info, holder);
            }
            return locks;
        } finally {
            release();
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * In order to prevent deadlocks from within the synchronous dispatching of
     * events, content modifications should not be made from within code
     * sections that hold monitors. (see #JCR-194)
     */
    public void unlock(NodeImpl node) throws LockException, RepositoryException {
        removeLockProperties(node);
        internalUnlock(node);
    }

    /**
     * {@inheritDoc}
     */
    public boolean holdsLock(NodeImpl node) throws RepositoryException {
        acquire();

        try {
            SessionImpl session = (SessionImpl) node.getSession();
            PathMap.Element element = lockMap.map(getPath(session, node.getId()), true);
            if (element == null) {
                return false;
            }
            return element.get() != null;
        } catch (ItemNotFoundException e) {
            return false;
        } finally {
            release();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLockHolder(Session session, NodeImpl node)
            throws RepositoryException {
        acquire();

        try {
            SessionImpl nodeSession = (SessionImpl) node.getSession();
            PathMap.Element element = lockMap.map(getPath(nodeSession, node.getId()), true);
            if (element == null) {
                return false;
            }
            AbstractLockInfo info = (AbstractLockInfo) element.get();
            return info != null && info.getLockHolder() == session;
        } catch (ItemNotFoundException e) {
            return false;
        } finally {
            release();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLocked(NodeImpl node) throws RepositoryException {
        acquire();

        try {
            SessionImpl session = (SessionImpl) node.getSession();
            Path path = getPath(session, node.getId());

            PathMap.Element element = lockMap.map(path, false);
            AbstractLockInfo info = (AbstractLockInfo) element.get();
            if (info == null) {
                return false;
            }
            if (element.hasPath(path)) {
                return true;
            } else {
                return info.deep;
            }
        } catch (ItemNotFoundException e) {
            return false;
        } finally {
            release();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkLock(NodeImpl node)
            throws LockException, RepositoryException {

        SessionImpl session = (SessionImpl) node.getSession();
        checkLock(getPath(session, node.getId()), session);
    }

    /**
     * {@inheritDoc}
     */
    public void checkLock(Path path, Session session)
            throws LockException, RepositoryException {

        PathMap.Element element = lockMap.map(path, false);
        AbstractLockInfo info = (AbstractLockInfo) element.get();
        if (info != null) {
            if (element.hasPath(path) || info.deep) {
                if (session != info.getLockHolder()) {
                    throw new LockException("Node locked.");
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void lockTokenAdded(SessionImpl session, String lt) throws LockException, RepositoryException {
        try {
            LockToken lockToken = LockToken.parse(lt);

            NodeImpl node = (NodeImpl)
                this.sysSession.getItemManager().getItem(lockToken.getId());
            PathMap.Element element = lockMap.map(node.getPrimaryPath(), true);
            if (element != null) {
                AbstractLockInfo info = (AbstractLockInfo) element.get();
                if (info != null) {
                    if (info.getLockHolder() == null) {
                        info.setLockHolder(session);
                        if (info instanceof LockInfo) {
                            session.addListener((LockInfo) info);
                        }
                    } else {
                        String msg = "Cannot add lock token: lock already held by other session.";
                        log.warn(msg);
                        throw new LockException(msg);
                    }
                }
            }
            // inform SessionLockManager
            getSessionLockManager(session).lockTokenAdded(lt);
        } catch (IllegalArgumentException e) {
            String msg = "Bad lock token: " + e.getMessage();
            log.warn(msg);
            throw new LockException(msg);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void lockTokenRemoved(SessionImpl session, String lt) throws LockException, RepositoryException {
        try {
            LockToken lockToken = LockToken.parse(lt);

            NodeImpl node = (NodeImpl)
                this.sysSession.getItemManager().getItem(lockToken.getId());
            PathMap.Element element = lockMap.map(node.getPrimaryPath(), true);
            if (element != null) {
                AbstractLockInfo info = (AbstractLockInfo) element.get();
                if (info != null) {
                    if (session == info.getLockHolder()) {
                        info.setLockHolder(null);
                    } else {
                        String msg = "Cannot remove lock token: lock held by other session.";
                        log.warn(msg);
                        throw new LockException(msg);
                    }
                }
            }
            // inform SessionLockManager
            getSessionLockManager(session).lockTokenRemoved(lt);
        } catch (IllegalArgumentException e) {
            String msg = "Bad lock token: " + e.getMessage();
            log.warn(msg);
            throw new LockException(msg);
        }
    }

    /**
     * Return the path of an item given its id. This method will lookup the
     * item inside the systme session.
     */
    private Path getPath(SessionImpl session, ItemId id) throws RepositoryException {
        return session.getHierarchyManager().getPath(id);
    }

    /**
     * Acquire lock on the lock map.
     */
    private void acquire() {
        for (;;) {
            try {
            	if (TransactionContext.getCurrentXid() == null) {
            		lockMapLock.acquire();
            	} else {
            		xidlockMapLock.acquire();
            	}
                break;
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    /**
     * Release lock on the lock map.
     */
    private void release() {
    	if (TransactionContext.getCurrentXid() == null) {
    		lockMapLock.release();
    	} else {
    		xidlockMapLock.release();
    	}
    }

    /**
     * Start an update operation. This will acquire the lock on the lock map
     * and disable saving the lock map file.
     */
    public void beginUpdate() {
        acquire();
        savingDisabled = true;
    }

    /**
     * End an update operation. This will save the lock map file and release
     * the lock on the lock map.
     */
    public void endUpdate() {
        savingDisabled = false;
        save();
        release();
    }

    /**
     * Cancel an update operation. This will release the lock on the lock map.
     */
    public void cancelUpdate() {
        savingDisabled = false;
        release();
    }

    /**
     * Add the lock related properties to the target node.
     *
     * @param node
     * @param lockOwner
     * @param isDeep
     */
    protected void writeLockProperties(NodeImpl node, String lockOwner, boolean isDeep) throws RepositoryException {
        boolean success = false;

        SessionImpl editingSession = (SessionImpl) node.getSession();
        WorkspaceImpl wsp = (WorkspaceImpl) editingSession.getWorkspace();
        UpdatableItemStateManager stateMgr = wsp.getItemStateManager();

        synchronized (stateMgr) {
            if (stateMgr.inEditMode()) {
                throw new RepositoryException("Unable to write lock properties.");
            }
            stateMgr.edit();
            try {
                // add properties to content
                NodeId nodeId = node.getNodeId();
                NodeState nodeState = (NodeState) stateMgr.getItemState(nodeId);

                PropertyState propState;
                if (!nodeState.hasPropertyName(NameConstants.JCR_LOCKOWNER)) {
                    propState = stateMgr.createNew(NameConstants.JCR_LOCKOWNER, nodeId);
                    propState.setType(PropertyType.STRING);
                    propState.setMultiValued(false);
                } else {
                    propState = (PropertyState) stateMgr.getItemState(new PropertyId(nodeId, NameConstants.JCR_LOCKOWNER));
                }
                propState.setValues(new InternalValue[] { InternalValue.create(lockOwner) });
                nodeState.addPropertyName(NameConstants.JCR_LOCKOWNER);
                stateMgr.store(nodeState);

                if (!nodeState.hasPropertyName(NameConstants.JCR_LOCKISDEEP)) {
                    propState = stateMgr.createNew(NameConstants.JCR_LOCKISDEEP, nodeId);
                    propState.setType(PropertyType.BOOLEAN);
                    propState.setMultiValued(false);
                } else {
                    propState = (PropertyState) stateMgr.getItemState(new PropertyId(nodeId, NameConstants.JCR_LOCKISDEEP));
                }
                propState.setValues(new InternalValue[] { InternalValue.create(isDeep) });
                nodeState.addPropertyName(NameConstants.JCR_LOCKISDEEP);
                stateMgr.store(nodeState);

                stateMgr.update();
                success = true;
            } catch (ItemStateException e) {
                throw new RepositoryException("Error while creating lock.", e);
            } finally {
                if (!success) {
                    // failed to set lock meta-data content, cleanup
                    stateMgr.cancel();
                    try {
                        unlock(node);
                    } catch (RepositoryException e) {
                        // cleanup failed
                        log.error("error while cleaning up after failed lock attempt", e);
                    }
                }
            }
        }
    }

    /**
     *
     * @param node
     * @throws RepositoryException
     */
    protected void removeLockProperties(NodeImpl node) throws RepositoryException {
        boolean success = false;

        SessionImpl editingSession = (SessionImpl) node.getSession();
        WorkspaceImpl wsp = (WorkspaceImpl) editingSession.getWorkspace();
        UpdatableItemStateManager stateMgr = wsp.getItemStateManager();

        synchronized (stateMgr) {
            try {
                // add properties to content
                NodeId nodeId = node.getNodeId();
                NodeState nodeState = (NodeState) stateMgr.getItemState(nodeId);

                if (stateMgr.inEditMode()) {
                    throw new RepositoryException("Unable to remove lock properties.");
                }
                stateMgr.edit();
                if (nodeState.hasPropertyName(NameConstants.JCR_LOCKOWNER)) {
                    PropertyState propState = (PropertyState) stateMgr.getItemState(new PropertyId(nodeId, NameConstants.JCR_LOCKOWNER));
                    nodeState.removePropertyName(NameConstants.JCR_LOCKOWNER);
                    stateMgr.destroy(propState);
                    stateMgr.store(nodeState);
                }

                if (nodeState.hasPropertyName(NameConstants.JCR_LOCKISDEEP)) {
                    PropertyState propState = (PropertyState) stateMgr.getItemState(new PropertyId(nodeId, NameConstants.JCR_LOCKISDEEP));
                    nodeState.removePropertyName(NameConstants.JCR_LOCKISDEEP);
                    stateMgr.destroy(propState);
                    stateMgr.store(nodeState);
                }

                stateMgr.update();
                success = true;
            } catch (ItemStateException e) {
                throw new RepositoryException("Error while removing lock.", e);
            } finally {
                if (!success) {
                    // failed to set lock meta-data content, cleanup
                    stateMgr.cancel();
                }
            }
        }
    }

    //----------------------------------------------< SynchronousEventListener >

    /**
     * Internal event class that holds old and new paths for moved nodes
     */
    private class HierarchyEvent {

        /**
         * ID recorded in event
         */
        private final NodeId id;

        /**
         * Path recorded in event
         */
        private final Path path;

        /**
         * Old path in move operation
         */
        private Path oldPath;

        /**
         * New path in move operation
         */
        private Path newPath;

        /**
         * Event type, may be {@link Event#NODE_ADDED},
         * {@link Event#NODE_REMOVED} or a combination of both
         */
        private int type;

        /**
         * Create a new instance of this class.
         *
         * @param id id
         * @param path path
         * @param type event type
         */
        public HierarchyEvent(NodeId id, Path path, int type) {
            this.id = id;
            this.path = path;
            this.type = type;
        }

        /**
         * Merge this event with another event. The result will be stored in
         * this event
         *
         * @param event other event to merge with
         */
        public void merge(HierarchyEvent event) {
            type |= event.type;
            if (event.type == Event.NODE_ADDED) {
                newPath = event.path;
                oldPath = path;
            } else {
                oldPath = event.path;
                newPath = path;
            }
        }

        /**
         * Return the event type. May be {@link Event#NODE_ADDED},
         * {@link Event#NODE_REMOVED} or a combination of both.\
         *
         * @return event type
         */
        public int getType() {
            return type;
        }

        /**
         * Return the old path if this is a move operation
         *
         * @return old path
         */
        public Path getOldPath() {
            return oldPath;
        }

        /**
         * Return the new path if this is a move operation
         *
         * @return new path
         */
        public Path getNewPath() {
            return newPath;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void onEvent(EventIterator events) {
        Iterator iter = consolidateEvents(events);
        while (iter.hasNext()) {
            HierarchyEvent event = (HierarchyEvent) iter.next();
            if (event.type == Event.NODE_ADDED) {
                nodeAdded(event.path);
            } else if (event.type == Event.NODE_REMOVED) {
                nodeRemoved(event.path);
            } else if (event.type == (Event.NODE_ADDED | Event.NODE_REMOVED)) {
                nodeMoved(event.getOldPath(), event.getNewPath());
            }
        }
    }

    /**
     * Consolidate an event iterator obtained from observation, merging
     * add and remove operations on nodes with the same UUID into a move
     * operation.
     */
    private Iterator consolidateEvents(EventIterator events) {
        LinkedMap eventMap = new LinkedMap();

        while (events.hasNext()) {
            EventImpl event = (EventImpl) events.nextEvent();
            HierarchyEvent he;

            try {
                he = new HierarchyEvent(event.getChildId(),
                        sysSession.getQPath(event.getPath()).getNormalizedPath(),
                        event.getType());
            } catch (MalformedPathException e) {
                log.info("Unable to get event's path: " + e.getMessage());
                continue;
            } catch (RepositoryException e) {
                log.info("Unable to get event's path: " + e.getMessage());
                continue;
            }

            HierarchyEvent heExisting = (HierarchyEvent) eventMap.get(he.id);
            if (heExisting != null) {
                heExisting.merge(he);
            } else {
                eventMap.put(he.id, he);
            }
        }
        return eventMap.values().iterator();
    }

    /**
     * Refresh a non-empty path element whose children might have changed
     * its position.
     */
    private void refresh(PathMap.Element element) {
        final ArrayList infos = new ArrayList();
        boolean needsSave = false;

        // save away non-empty children
        element.traverse(new PathMap.ElementVisitor() {
            public void elementVisited(PathMap.Element element) {
                LockInfo info = (LockInfo) element.get();
                infos.add(info);
            }
        }, false);

        // remove all children
        element.removeAll();

        // now re-insert at appropriate location or throw away if node
        // does no longer exist
        for (int i = 0; i < infos.size(); i++) {
            LockInfo info = (LockInfo) infos.get(i);
            try {
                NodeImpl node = (NodeImpl) sysSession.getItemManager().
                        getItem(info.getId());
                lockMap.put(node.getPrimaryPath(), info);
            } catch (RepositoryException e) {
                info.setLive(false);
                if (!info.sessionScoped) {
                    needsSave = true;
                }
            }
        }

        // save if required
        if (needsSave) {
            save();
        }
    }

    /**
     * Invoked when some node has been added. If the parent of that node
     * exists, shift all name siblings of the new node having an index greater
     * or equal.
     *
     * @param path path of added node
     */
    private void nodeAdded(Path path) {
        acquire();

        try {
            PathMap.Element parent = lockMap.map(path.getAncestor(1), true);
            if (parent != null) {
                refresh(parent);
            }
        } catch (PathNotFoundException e) {
            log.warn("Unable to determine path of added node's parent.", e);
        } finally {
            release();
        }
    }

    /**
     * Invoked when some node has been moved. Relink the child inside our
     * map to the new parent.
     *
     * @param oldPath old path
     * @param newPath new path
     */
    private void nodeMoved(Path oldPath, Path newPath) {
        acquire();

        try {
            PathMap.Element parent = lockMap.map(oldPath.getAncestor(1), true);
            if (parent != null) {
                refresh(parent);
            }
        } catch (PathNotFoundException e) {
            log.warn("Unable to determine path of moved node's parent.", e);
        } finally {
            release();
        }
    }

    /**
     * Invoked when some node has been removed. Remove the child from our
     * path map. Disable all locks contained in that subtree.
     *
     * @param path path of removed node
     */
    private void nodeRemoved(Path path) {
        acquire();

        try {
            PathMap.Element parent = lockMap.map(path.getAncestor(1), true);
            if (parent != null) {
                refresh(parent);
            }
        } catch (PathNotFoundException e) {
            log.warn("Unable to determine path of removed node's parent.", e);
        } finally {
            release();
        }
    }

    /**
     * Contains information about a lock and gets placed inside the child
     * information of a {@link org.apache.jackrabbit.spi.commons.name.PathMap}.
     */
    class LockInfo extends AbstractLockInfo implements SessionListener {

        /**
         * Create a new instance of this class.
         *
         * @param lockToken     lock token
         * @param sessionScoped whether lock token is session scoped
         * @param deep          whether lock is deep
         * @param lockOwner     owner of lock
         */
        public LockInfo(LockToken lockToken, boolean sessionScoped,
                        boolean deep, String lockOwner) {
            this(lockToken, sessionScoped, deep, lockOwner, TIMEOUT_INFINITE);
        }

        /**
         * Create a new instance of this class.
         *
         * @param lockToken     lock token
         * @param sessionScoped whether lock token is session scoped
         * @param deep          whether lock is deep
         * @param lockOwner     owner of lock
         * @param timeoutHint
         */
        public LockInfo(LockToken lockToken, boolean sessionScoped,
                        boolean deep, String lockOwner, long timeoutHint) {
            super(lockToken, sessionScoped, deep, lockOwner, timeoutHint);
        }

        /**
         * {@inheritDoc}
         * <p/>
         * When the owning session is logging out, we have to perform some
         * operations depending on the lock type.
         * (1) If the lock was session-scoped, we unlock the node.
         * (2) If the lock was open-scoped, we remove the lock token
         *     from the session and set the lockHolder field to <code>null</code>.
         */
        public void loggingOut(SessionImpl session) {
            if (live) {
                if (sessionScoped) {
                    // if no session currently holds lock, reassign
                    SessionImpl lockHolder = getLockHolder();
                    if (lockHolder == null) {
                        setLockHolder(session);
                    }
                    try {
                        NodeImpl node = (NodeImpl) session.getItemManager().getItem(getId());
                        node.unlock();
                    } catch (RepositoryException e) {
                        // Session is not allowed/able to unlock.
                        // Use system session present with lock-mgr as fallback
                        // in order to make sure, that session-scoped locks are
                        // properly cleaned.
                        SessionImpl systemSession = LockManagerImpl.this.sysSession;
                        setLockHolder(systemSession);
                        try {
                            NodeImpl node = (NodeImpl) systemSession.getItemManager().getItem(getId());
                            node.unlock();
                        } catch (RepositoryException re) {
                            log.warn("Unable to remove session-scoped lock on node '" + lockToken + "': " + e.getMessage());
                            log.debug("Root cause: ", e);
                        }
                    }
                } else {
                    if (session == lockHolder) {
                        session.removeLockToken(lockToken.toString());
                        lockHolder = null;
                    }
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public void loggedOut(SessionImpl session) {
        }
    }

    //----------------------------------------------------< LockEventListener >

    /**
     * Set a lock event channel
     *
     * @param eventChannel lock event channel
     */
    public void setEventChannel(LockEventChannel eventChannel) {
        this.eventChannel = eventChannel;
        eventChannel.setListener(this);
    }

    /**
     * {@inheritDoc}
     */
    public void externalLock(NodeId nodeId, boolean isDeep, String lockOwner) throws RepositoryException {
        acquire();

        try {
            Path path = getPath(sysSession, nodeId);

            // create lock token
            LockInfo info = new LockInfo(new LockToken(nodeId), false, isDeep, lockOwner);
            info.setLive(true);
            lockMap.put(path, info);

            save();
        } finally {
            release();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void externalUnlock(NodeId nodeId) throws RepositoryException {
        acquire();

        try {
            Path path = getPath(sysSession, nodeId);
            PathMap.Element element = lockMap.map(path, true);
            if (element == null) {
                throw new LockException("Node not locked: " + path.toString());
            }
            AbstractLockInfo info = (AbstractLockInfo) element.get();
            if (info == null) {
                throw new LockException("Node not locked: " + path.toString());
            }
            element.set(null);
            info.setLive(false);

            save();

        } finally {
            release();
        }
    }

    /**
     * Dump contents of path map and elements included to <code>PrintStream</code> given.
     *
     * @param ps print stream to dump to
     */
    public void dump(final PrintStream ps) {
        lockMap.traverse(new PathMap.ElementVisitor() {
            public void elementVisited(PathMap.Element element) {
                StringBuffer line = new StringBuffer();
                for (int i = 0; i < element.getDepth(); i++) {
                    line.append("--");
                }
                line.append(element.getName());
                int index = element.getIndex();
                if (index != 0 && index != 1) {
                    line.append('[');
                    line.append(index);
                    line.append(']');
                }
                line.append("  ");
                line.append(element.get());
                ps.println(line.toString());
            }
        }, true);
    }
}
