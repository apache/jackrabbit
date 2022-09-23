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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.jcr.ItemNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.SessionListener;
import org.apache.jackrabbit.core.WorkspaceImpl;
import org.apache.jackrabbit.core.cluster.ClusterOperation;
import org.apache.jackrabbit.core.cluster.LockEventChannel;
import org.apache.jackrabbit.core.cluster.LockEventListener;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.observation.EventImpl;
import org.apache.jackrabbit.core.observation.SynchronousEventListener;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.UpdatableItemStateManager;
import org.apache.jackrabbit.core.util.XAReentrantLock;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.PathMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides the functionality needed for locking and unlocking nodes.
 */
public class LockManagerImpl
        implements LockManager, SynchronousEventListener, LockEventListener {

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
    private final PathMap<LockInfo> lockMap = new PathMap<LockInfo>();

    /**
     * XA/Thread aware lock to path map.
     */
    private final XAReentrantLock lockMapLock = new XAReentrantLock();
    
    /**
     * XA/Thread aware lock for lock properties
     */
    private XAReentrantLock lockPropertiesLock = new XAReentrantLock();

    /**
     * The periodically invoked lock timeout handler.
     */
    private final ScheduledFuture<?> timeoutHandler;

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
     * @param session  system session
     * @param fs       file system for persisting locks
     * @param executor scheduled executor service for handling lock timeouts
     * @throws RepositoryException if an error occurs
     */
    public LockManagerImpl(
            SessionImpl session, FileSystem fs,
            ScheduledExecutorService executor) throws RepositoryException {

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

        timeoutHandler = executor.scheduleWithFixedDelay(
                new TimeoutHandler(), 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Close this lock manager. Writes back all changes.
     */
    public void close() {
        timeoutHandler.cancel(false);
        save();
    }

    /**
     * Periodically (at one second delay) invoked timeout handler. Traverses
     * all locks and unlocks those that have expired.
     *
     * @see <a href="https://issues.apache.org/jira/browse/JCR-1590">JCR-1590</a>:
     *      JSR 283: Locking
     */
    private class TimeoutHandler implements Runnable {
        private final TimeoutHandlerVisitor visitor = new TimeoutHandlerVisitor();

        public void run() {
            lockMap.traverse(visitor, false);
        }
    }

    private class TimeoutHandlerVisitor implements
            PathMap.ElementVisitor<LockInfo> {
        public void elementVisited(PathMap.Element<LockInfo> element) {
            LockInfo info = element.get();
            if (info != null && info.isLive() && info.isExpired()) {
                NodeId id = info.getId();
                SessionImpl holder = info.getLockHolder();
                if (holder == null) {
                    info.setLockHolder(sysSession);
                    holder = sysSession;
                }
                try {
                    // FIXME: This session access is not thread-safe!
                    log.debug("Try to unlock expired lock. NodeId {}", id);
                    unlock(holder.getNodeById(id));
                } catch (RepositoryException e) {
                    log.warn("Unable to expire the lock. NodeId " + id, e);
                }
            }
        }
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
                reapplyLock(s);
            }
        } catch (IOException e) {
            throw new FileSystemException("error while reading locks file", e);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    /**
     * Reaply a lock given a lock token that was read from the locks file
     *
     * @param lockTokenLine lock token to apply
     */
    private void reapplyLock(String lockTokenLine) {
        String[] parts = lockTokenLine.split(",");
        String token = parts[0];
        long timeoutHint = Long.MAX_VALUE;
        if (parts.length > 1) {
            try {
                timeoutHint = Long.parseLong(parts[1]);
            } catch (NumberFormatException e) {
                log.warn("Unexpected timeout hint "
                        + parts[1] + " for lock token " + token, e);
            }
        }

        try {
        	acquire();
        	
            NodeId id = LockInfo.parseLockToken(parts[0]);
            NodeImpl node = (NodeImpl) sysSession.getItemManager().getItem(id);
            Path path = getPath(sysSession, id);

            InternalLockInfo info = new InternalLockInfo(
                    id, false,
                    node.getProperty(NameConstants.JCR_LOCKISDEEP).getBoolean(),
                    node.getProperty(NameConstants.JCR_LOCKOWNER).getString(),
                    timeoutHint);
            info.setLive(true);
            lockMap.put(path, info);
        } catch (RepositoryException e) {
            log.warn("Unable to recreate lock '" + token + "': " + e.getMessage());
            log.debug("Root cause: ", e);
        } finally {
        	release();
        }
    }

    /**
     * Write locks to locks file
     */
    private void save() {
        if (savingDisabled) {
            return;
        }

        final ArrayList<LockInfo> list = new ArrayList<LockInfo>();

        lockMap.traverse(new PathMap.ElementVisitor<LockInfo>() {
            public void elementVisited(PathMap.Element<LockInfo> element) {
                LockInfo info = element.get();
                if (!info.isSessionScoped()) {
                    list.add(info);
                }
            }
        }, false);


        BufferedWriter writer = null;

        try {
            writer = new BufferedWriter(
                    new OutputStreamWriter(locksFile.getOutputStream()));
            for (LockInfo info : list) {
                writer.write(info.getLockToken());

                // Store the timeout hint, if one is specified
                if (info.getTimeoutHint() != Long.MAX_VALUE) {
                    writer.write(',');
                    writer.write(Long.toString(info.getTimeoutHint()));
                }

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
        Workspace wsp = session.getWorkspace();
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
    LockInfo internalLock(
            NodeImpl node, boolean isDeep, boolean isSessionScoped,
            long timeoutHint, String ownerInfo)
            throws LockException, RepositoryException {

        SessionImpl session = (SessionImpl) node.getSession();
        String lockOwner = (ownerInfo != null) ? ownerInfo : session.getUserID();
        InternalLockInfo info = new InternalLockInfo(
                node.getNodeId(), isSessionScoped, isDeep, lockOwner, timeoutHint);

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
            PathMap.Element<LockInfo> element = lockMap.map(path, false);

            LockInfo other = element.get();
            if (other != null) {
                if (element.hasPath(path)) {
                    other.throwLockException(
                            "Node already locked: " + node, session);
                } else if (other.isDeep()) {
                    other.throwLockException(
                            "Parent node has a deep lock: " + node, session);
                }
            }
            if (info.isDeep() && element.hasPath(path)
                    && element.getChildrenCount() > 0) {
                info.throwLockException("Some child node is locked", session);
            }

            // create lock token
            info.setLockHolder(session);
            info.setLive(true);
            session.addListener(info);
            if (!info.isSessionScoped()) {
                getSessionLockManager(session).lockTokenAdded(info.getLockToken());
            }
            lockMap.put(path, info);

            if (!info.isSessionScoped()) {
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
            PathMap.Element<LockInfo> element =
                lockMap.map(getPath(session, node.getId()), true);
            if (element == null) {
                throw new LockException("Node not locked: " + node);
            }
            LockInfo info = element.get();
            if (info == null) {
                throw new LockException("Node not locked: " + node);
            }
            checkUnlock(info, session);

            getSessionLockManager(session).lockTokenRemoved(info.getLockToken());

            element.set(null);
            info.setLive(false);

            if (!info.isSessionScoped()) {
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
     * Package-private low-level helper method returning all locks
     * associated with the specified session.
     * @param session session
     * @return an array of <code>AbstractLockInfo</code>s
     */
    LockInfo[] getLockInfos(final SessionImpl session) {
        final ArrayList<LockInfo> infos = new ArrayList<LockInfo>();
        lockMap.traverse(new PathMap.ElementVisitor<LockInfo>() {
            public void elementVisited(PathMap.Element<LockInfo> element) {
                LockInfo info = element.get();
                if (info.isLive() && info.isLockHolder(session)) {
                    infos.add(info);
                }
            }
        }, false);
        return infos.toArray(new LockInfo[infos.size()]);
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
        source.lockMap.traverse(new PathMap.ElementVisitor<LockInfo>() {
            public void elementVisited(PathMap.Element<LockInfo> element) {
                LockInfo info = element.get();
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
    public LockInfo getLockInfo(NodeId id) throws RepositoryException {
        Path path;
        try {
            path = getPath(sysSession, id);
        } catch (ItemNotFoundException e) {
            return null;
        }

        acquire();
        try {
            PathMap.Element<LockInfo> element = lockMap.map(path, false);
            LockInfo info = element.get();
            if (info != null) {
                if (element.hasPath(path) || info.isDeep()) {
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
        return lock(node, isDeep, isSessionScoped, Long.MAX_VALUE, null);
    }

    public Lock lock(NodeImpl node, boolean isDeep, boolean isSessionScoped, long timoutHint, String ownerInfo)
            throws LockException, RepositoryException {
        LockInfo info = internalLock(node, isDeep, isSessionScoped, timoutHint, ownerInfo);
        writeLockProperties(node, info.getLockOwner(), info.isDeep());

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

            PathMap.Element<LockInfo> element = lockMap.map(path, false);
            LockInfo info = element.get();
            if (info != null && (element.hasPath(path) || info.isDeep())) {
                NodeImpl lockHolder = (NodeImpl)
                    session.getItemManager().getItem(info.getId());
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

        LockInfo[] infos = getLockInfos(session);

        try {
            Lock[] locks = new Lock[infos.length];
            for (int i = 0; i < infos.length; i++) {
                NodeImpl holder = (NodeImpl)
                    session.getItemManager().getItem(infos[i].getId());
                locks[i] = new LockImpl(infos[i], holder);
            }
            return locks;
        } finally {
            release();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
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
            PathMap.Element<LockInfo> element =
                lockMap.map(getPath(session, node.getId()), true);
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
    public boolean isLocked(NodeImpl node) throws RepositoryException {
        acquire();

        try {
            SessionImpl session = (SessionImpl) node.getSession();
            Path path = getPath(session, node.getId());

            PathMap.Element<LockInfo> element = lockMap.map(path, false);
            LockInfo info = element.get();
            if (info == null) {
                return false;
            }
            if (element.hasPath(path)) {
                return true;
            } else {
                return info.isDeep();
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

        acquire();
        try {
            PathMap.Element<LockInfo> element = lockMap.map(path, false);
            LockInfo info = element.get();
            if (info != null) {
                if (element.hasPath(path) || info.isDeep()) {
                    checkLock(info, session);
                }
            }
        } finally {
            release();
        }
    }

    /**
     * Check whether a lock info allows access to a session. May be overridden
     * by subclasses to allow access to nodes for sessions other than the
     * lock holder itself.
     * <p>
     * Default implementation allows access to the lock holder only.
     *
     * @param info info to check
     * @param session session
     * @throws LockException if write access to the specified path is not allowed
     * @throws RepositoryException if some other error occurs
     */
    protected void checkLock(LockInfo info, Session session)
            throws LockException, RepositoryException {

        if (!info.isLockHolder(session)) {
            throw new LockException("Node locked.");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkUnlock(Session session, NodeImpl node)
            throws LockException, RepositoryException {
        acquire();
        
        try {
            // check whether node is locked by this session
            PathMap.Element<LockInfo> element =
                lockMap.map(getPath((SessionImpl) session, node.getId()), true);
            if (element == null) {
                throw new LockException("Node not locked: " + node);
            }
            LockInfo info = element.get();
            if (info == null) {
                throw new LockException("Node not locked: " + node);
            }
            checkUnlock(info, session);
        } finally {
            release();
        }
    }

    /**
     * Check whether a session is allowed to unlock a node. May be overridden
     * by subclasses to allow this to sessions other than the lock holder
     * itself.
     * <p>
     * Default implementation allows unlocking to the lock holder only.
     *
     * @param info info to check
     * @param session session
     * @throws LockException if unlocking is denied
     * @throws RepositoryException if some other error occurs
     */
    protected void checkUnlock(LockInfo info, Session session)
            throws LockException, RepositoryException {

        if (!info.isLockHolder(session)) {
            throw new LockException("Node not locked by session: "
                    + info.getId());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addLockToken(SessionImpl session, String lt) throws LockException, RepositoryException {
        try {
            acquire();
            
            NodeId id = LockInfo.parseLockToken(lt);

            NodeImpl node = (NodeImpl) sysSession.getItemManager().getItem(id);
            Path path = node.getPrimaryPath();
            PathMap.Element<LockInfo> element = lockMap.map(path, true);
            if (element != null) {
                LockInfo info = element.get();
                if (info != null && !info.isLockHolder(session)) {
                    if (info.getLockHolder() == null) {
                        info.setLockHolder(session);
                        if (info instanceof InternalLockInfo) {
                            session.addListener((InternalLockInfo) info);
                        }
                    } else {
                        String msg = "Cannot add lock token: lock already held by other session.";
                        log.warn(msg);
                        info.throwLockException(msg, session);
                    }
                }
            }
            // inform SessionLockManager
            getSessionLockManager(session).lockTokenAdded(lt);
        } catch (IllegalArgumentException e) {
            String msg = "Bad lock token: " + e.getMessage();
            log.warn(msg);
            throw new LockException(msg);
        } finally {
            release();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeLockToken(SessionImpl session, String lt)
            throws LockException, RepositoryException {

        try {
            acquire();
            
            NodeId id = LockInfo.parseLockToken(lt);

            NodeImpl node = (NodeImpl) sysSession.getItemManager().getItem(id);
            PathMap.Element<LockInfo> element =
                lockMap.map(node.getPrimaryPath(), true);
            if (element != null) {
                LockInfo info = element.get();
                if (info != null) {
                    if (info.isLockHolder(session)) {
                        info.setLockHolder(null);
                    } else if (info.getLockHolder() != null) {
                        String msg = "Cannot remove lock token: lock held by other session.";
                        log.warn(msg);
                        info.throwLockException(msg, session);
                    }
                }
            }
            // inform SessionLockManager
            getSessionLockManager(session).lockTokenRemoved(lt);
        } catch (IllegalArgumentException e) {
            String msg = "Bad lock token: " + e.getMessage();
            log.warn(msg);
            throw new LockException(msg);
        } finally {
            release();
        }
    }

    /**
     * Return the path of an item given its id. This method will lookup the
     * item inside the system session.
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
           		lockMapLock.acquire();
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
   		lockMapLock.release();
    }

    /**
     * Acquire lock for modifying lock properties
     */
    private void acquireLockPropertiesLock() {
        for (;;) {
            try {
                lockPropertiesLock.acquire();
                break;
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    /**
     * Release lock on the lockPropertiesLock.
     */
    private void releaseLockPropertiesLock() {
        lockPropertiesLock.release();
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

        try {
            acquireLockPropertiesLock();

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
        } finally {
            releaseLockPropertiesLock();
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

        try {
            acquireLockPropertiesLock();

            // add properties to content
            if (stateMgr.inEditMode()) {
                throw new RepositoryException("Unable to remove lock properties.");
            }
            stateMgr.edit();
            try {
                NodeId nodeId = node.getNodeId();
                NodeState nodeState = (NodeState) stateMgr.getItemState(nodeId);
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
        } finally {
            releaseLockPropertiesLock();
        }
    }

    //----------------------------------------------< SynchronousEventListener >

    /**
     * Internal event class that holds old and new paths for moved nodes
     */
    private static class HierarchyEvent {

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
        Iterator<HierarchyEvent> iter = consolidateEvents(events);
        while (iter.hasNext()) {
            HierarchyEvent event = iter.next();
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
    private Iterator<HierarchyEvent> consolidateEvents(EventIterator events) {
        LinkedMap<NodeId, HierarchyEvent> eventMap = new LinkedMap<>();

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

            HierarchyEvent heExisting = eventMap.get(he.id);
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
    private void refresh(PathMap.Element<LockInfo> element) {
        final ArrayList<LockInfo> infos = new ArrayList<LockInfo>();
        boolean needsSave = false;

        // save away non-empty children
        element.traverse(new PathMap.ElementVisitor<LockInfo>() {
            public void elementVisited(PathMap.Element<LockInfo> element) {
                infos.add(element.get());
            }
        }, false);

        // remove all children
        element.removeAll();

        // now re-insert at appropriate location or throw away if node
        // does no longer exist
        for (int i = 0; i < infos.size(); i++) {
            LockInfo info = infos.get(i);
            try {
            	acquire();
            	
                NodeImpl node = (NodeImpl) sysSession.getItemManager().getItem(
                        info.getId());
                lockMap.put(node.getPrimaryPath(), info);
            } catch (RepositoryException e) {
                info.setLive(false);
                if (!info.isSessionScoped()) {
                    needsSave = true;
                }
            } finally {
            	release();
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
            PathMap.Element<LockInfo> parent =
                lockMap.map(path.getAncestor(1), true);
            if (parent != null) {
                refresh(parent);
            }
        } catch (RepositoryException e) {
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
            PathMap.Element<LockInfo> parent =
                lockMap.map(oldPath.getAncestor(1), true);
            if (parent != null) {
                refresh(parent);
            }
        } catch (RepositoryException e) {
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
            PathMap.Element<LockInfo> parent =
                lockMap.map(path.getAncestor(1), true);
            if (parent != null) {
                refresh(parent);
            }
        } catch (RepositoryException e) {
            log.warn("Unable to determine path of removed node's parent.", e);
        } finally {
            release();
        }
    }

    /**
     * Contains information about a lock and gets placed inside the child
     * information of a {@link org.apache.jackrabbit.spi.commons.name.PathMap}.
     */
    class InternalLockInfo extends LockInfo implements SessionListener {

        /**
         * Create a new instance of this class.
         *
         * @param lockToken     lock token
         * @param sessionScoped whether lock token is session scoped
         * @param deep          whether lock is deep
         * @param lockOwner     owner of lock
         * @param timeoutHint
         */
        public InternalLockInfo(NodeId lockToken, boolean sessionScoped,
                                boolean deep, String lockOwner, long timeoutHint) {
            super(lockToken, sessionScoped, deep, lockOwner, timeoutHint);
        }

        /**
         * {@inheritDoc}
         * <p>
         * When the owning session is logging out, we have to perform some
         * operations depending on the lock type.
         * (1) If the lock was session-scoped, we unlock the node.
         * (2) If the lock was open-scoped, we remove the lock token
         *     from the session and set the lockHolder field to <code>null</code>.
         */
        public void loggingOut(SessionImpl session) {
            if (isLive()) {
                if (isSessionScoped()) {
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
                            log.warn("Unable to remove session-scoped lock on node '" + getLockToken() + "': " + e.getMessage());
                            log.debug("Root cause: ", e);
                        }
                    }
                } else if (isLockHolder(session)) {
                    session.removeLockToken(getLockToken());
                    setLockHolder(null);
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
            InternalLockInfo info = new InternalLockInfo(
                    nodeId, false, isDeep, lockOwner, Long.MAX_VALUE);
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
            PathMap.Element<LockInfo> element = lockMap.map(path, true);
            if (element == null) {
                throw new LockException("Node not locked: " + path.toString());
            }
            LockInfo info = element.get();
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
     * Dump contents of path map and elements included to a string.
     */
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        lockMap.traverse(new PathMap.ElementVisitor<LockInfo>() {
            public void elementVisited(PathMap.Element<LockInfo> element) {
                for (int i = 0; i < element.getDepth(); i++) {
                    builder.append("--");
                }
                builder.append(element.getName());
                int index = element.getIndex();
                if (index != 0 && index != 1) {
                    builder.append('[');
                    builder.append(index);
                    builder.append(']');
                }
                builder.append("  ");
                builder.append(element.get());
                builder.append("\n");
            }
        }, true);
        return builder.toString();
    }
}
