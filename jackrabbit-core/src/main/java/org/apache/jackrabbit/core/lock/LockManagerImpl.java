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
import org.apache.jackrabbit.conversion.MalformedPathException;
import org.apache.jackrabbit.conversion.NamePathResolver;
import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.SessionListener;
import org.apache.jackrabbit.core.cluster.ClusterOperation;
import org.apache.jackrabbit.core.cluster.LockEventChannel;
import org.apache.jackrabbit.core.cluster.LockEventListener;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.core.observation.EventImpl;
import org.apache.jackrabbit.core.observation.SynchronousEventListener;
import org.apache.jackrabbit.core.util.Dumpable;
import org.apache.jackrabbit.name.NameConstants;
import org.apache.jackrabbit.name.PathMap;
import org.apache.jackrabbit.spi.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.ArrayList;
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
     * Lock to path map.
     */
    private final ReentrantLock lockMapLock = new ReentrantLock();

    /**
     * System session
     */
    private final SessionImpl session;

    /**
     * Locks file
     */
    private final FileSystemResource locksFile;

    /**
     * Flag indicating whether automatic saving is disabled.
     */
    private boolean savingDisabled;

    /**
     * Name and Path resolver
     */
    private final NamePathResolver resolver;

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

        this.session = session;
        this.resolver = session.getNamePathResolver();
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
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e2) {
                    /* ignore */
                }
            }
        }
    }

    /**
     * Reapply a lock given a lock token that was read from the locks file
     *
     * @param lockToken lock token to apply
     */
    private void reapplyLock(LockToken lockToken) {
        try {
            NodeImpl node = (NodeImpl) session.getItemManager().getItem(lockToken.id);
            Path path = getPath(lockToken.id);

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
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Internal <code>lock</code> implementation that takes the same parameters
     * as the public method but will not modify content.
     * @param node node to lock
     * @param isDeep whether the lock applies to this node only
     * @param isSessionScoped whether the lock is session scoped
     * @return lock
     * @throws LockException       if the node is already locked
     * @throws RepositoryException if another error occurs
     */
    AbstractLockInfo internalLock(NodeImpl node, boolean isDeep, boolean isSessionScoped)
            throws LockException, RepositoryException {

        SessionImpl session = (SessionImpl) node.getSession();
        LockInfo info = new LockInfo(new LockToken(node.getNodeId()),
                isSessionScoped, isDeep, session.getUserID());

        ClusterOperation operation = null;
        boolean successful = false;

        // Cluster is only informed about open-scoped locks
        if (eventChannel != null && !isSessionScoped) {
            operation = eventChannel.create(node.getNodeId(), isDeep, session.getUserID());
        }

        acquire();

        try {
            // check whether node is already locked
            Path path = getPath(node.getId());
            PathMap.Element element = lockMap.map(path, false);

            LockInfo other = (LockInfo) element.get();
            if (other != null) {
                if (element.hasPath(path)) {
                    throw new LockException("Node already locked: " + node.safeGetJCRPath());
                } else if (other.deep) {
                    throw new LockException("Parent node has deep lock.");
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
            session.addLockToken(info.lockToken.toString(), false);
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
    void internalUnlock(NodeImpl node)
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
            PathMap.Element element = lockMap.map(
                    getPath(node.getId()), true);
            if (element == null) {
                throw new LockException("Node not locked: " + node.safeGetJCRPath());
            }
            AbstractLockInfo info = (AbstractLockInfo) element.get();
            if (info == null) {
                throw new LockException("Node not locked: " + node.safeGetJCRPath());
            }
            if (session != info.getLockHolder()) {
                throw new LockException("Node not locked by session: " + node.safeGetJCRPath());
            }
            session.removeLockToken(info.getLockToken(session), false);

            element.set(null);
            info.setLive(false);

            if (!info.sessionScoped) {
                save();
                successful = true;
            }
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
     * Return the most appropriate lock information for a node. This is either
     * the lock info for the node itself, if it is locked, or a lock info for one
     * of its parents, if that is deep locked.
     * @return lock info or <code>null</code> if node is not locked
     * @throws RepositoryException if an error occurs
     */
    public AbstractLockInfo getLockInfo(NodeId id) throws RepositoryException {
        acquire();

        try {
            Path path = getPath(id);

            PathMap.Element element = lockMap.map(path, false);
            AbstractLockInfo info = (AbstractLockInfo) element.get();
            if (info != null) {
                if (element.hasPath(path) || info.deep) {
                    return info;
                }
            }
            return null;
        } catch (ItemNotFoundException e) {
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

        AbstractLockInfo info = internalLock(node, isDeep, isSessionScoped);
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
            Path path = getPath(node.getId());

            PathMap.Element element = lockMap.map(path, false);
            AbstractLockInfo info = (AbstractLockInfo) element.get();
            if (info == null) {
                throw new LockException("Node not locked: " + node.safeGetJCRPath());
            }
            if (element.hasPath(path) || info.deep) {
                Node lockHolder = (Node) session.getItemManager().getItem(info.getId());
                return new LockImpl(info, lockHolder);
            } else {
                throw new LockException("Node not locked: " + node.safeGetJCRPath());
            }
        } catch (ItemNotFoundException e) {
            throw new LockException("Node not locked: " + node.safeGetJCRPath());
        } finally {
            release();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Lock[] getLocks(SessionImpl session) throws RepositoryException {

        acquire();

        AbstractLockInfo infos[] = getLockInfos(session);

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
    public void unlock(NodeImpl node)
            throws LockException, RepositoryException {

        internalUnlock(node);
    }

    /**
     * {@inheritDoc}
     */
    public boolean holdsLock(NodeImpl node) throws RepositoryException {
        acquire();

        try {
            PathMap.Element element = lockMap.map(getPath(node.getId()), true);
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
            PathMap.Element element = lockMap.map(getPath(node.getId()), true);
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
            Path path = getPath(node.getId());

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
        checkLock(getPath(node.getId()), session);
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
    public void lockTokenAdded(SessionImpl session, String lt) {
        try {
            LockToken lockToken = LockToken.parse(lt);

            NodeImpl node = (NodeImpl) this.session.getItemManager().
                    getItem(lockToken.id);
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
                        log.warn("Adding lock token has no effect: "
                                + "lock already held by other session.");
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            log.warn("Bad lock token: " + e.getMessage());
        } catch (RepositoryException e) {
            log.warn("Unable to set lock holder: " + e.getMessage());
            log.debug("Root cause: ", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void lockTokenRemoved(SessionImpl session, String lt) {
        try {
            LockToken lockToken = LockToken.parse(lt);

            NodeImpl node = (NodeImpl) this.session.getItemManager().
                    getItem(lockToken.id);
            PathMap.Element element = lockMap.map(node.getPrimaryPath(), true);
            if (element != null) {
                AbstractLockInfo info = (AbstractLockInfo) element.get();
                if (info != null) {
                    if (session == info.getLockHolder()) {
                        info.setLockHolder(null);
                    } else {
                        log.warn("Removing lock token has no effect: "
                                + "lock held by other session.");
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            log.warn("Bad lock token: " + e.getMessage());
        } catch (RepositoryException e) {
            log.warn("Unable to reset lock holder: " + e.getMessage());
            log.debug("Root cause: ", e);
        }
    }

    /**
     * Return the path of an item given its id. This method will lookup the
     * item inside the systme session.
     */
    private Path getPath(ItemId id) throws RepositoryException {
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
            switch (event.type) {
                case Event.NODE_ADDED:
                    nodeAdded(event.path);
                    break;
                case Event.NODE_REMOVED:
                    nodeRemoved(event.path);
                    break;
                case Event.NODE_ADDED | Event.NODE_REMOVED:
                    nodeMoved(event.getOldPath(), event.getNewPath());
                    break;
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
                        resolver.getQPath(event.getPath()).getNormalizedPath(),
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
                NodeImpl node = (NodeImpl) session.getItemManager().
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
     * information of a {@link org.apache.jackrabbit.name.PathMap}.
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
            super(lockToken, sessionScoped, deep, lockOwner);
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
                        log.warn("Unable to remove session-scoped lock on node '"
                                + lockToken + "': " + e.getMessage());
                        log.debug("Root cause: ", e);
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
    public void externalLock(NodeId nodeId, boolean isDeep, String userId) throws RepositoryException {
        acquire();

        try {
            Path path = getPath(nodeId);

            // create lock token
            LockInfo info = new LockInfo(new LockToken(nodeId), false, isDeep, userId);
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
            Path path = getPath(nodeId);
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
