/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import org.apache.commons.collections.map.LinkedMap;
import org.apache.jackrabbit.Constants;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.PathMap;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.observation.EventImpl;
import org.apache.jackrabbit.core.observation.SynchronousEventListener;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.name.MalformedPathException;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.Path;
import org.apache.log4j.Logger;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ItemNotFoundException;
import javax.jcr.AccessDeniedException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;


/**
 * Provides the functionality needed for locking and unlocking nodes.
 */
public class LockManagerImpl implements LockManager, SynchronousEventListener {

    /**
     * Logger
     */
    private static final Logger log = Logger.getLogger(LockManagerImpl.class);

    /**
     * Path map containing all locks at the leaves
     */
    private final PathMap lockMap = new PathMap();

    /**
     * System session
     */
    private final SessionImpl session;

    /**
     * Locks file
     */
    private final File locksFile;

    /**
     * Monitor used when modifying content, too, in order to make modifications
     * in the lock map and modifications in the content atomic.
     */
    private final Object contentMonitor = new Object();

    /**
     * Name space resolver
     */
    private final NamespaceResolver nsResolver;

    /**
     * Create a new instance of this class.
     * @param session system session
     * @param locksFile file locks file to use
     * @throws RepositoryException if an error occurs
     */
    public LockManagerImpl(SessionImpl session, File locksFile)
            throws RepositoryException {

        this.session = session;
        this.nsResolver = session.getNamespaceResolver();
        this.locksFile = locksFile;

        session.getWorkspace().getObservationManager().
                addEventListener(this, Event.NODE_ADDED | Event.NODE_REMOVED,
                        "/", true, null, null, true);

        if (locksFile.exists()) {
            try {
                load();
            } catch (IOException e) {
                throw new RepositoryException(
                        "I/O error while reading locks from '"
                        + locksFile.getPath() + "'", e);
            }
        }
    }

    /**
     * Close this lock manager. Writes back all changes.
     */
    public void close() {
        try {
            save();
        } catch (IOException e) {
            log.warn("I/O error while saving locks to '"
                    + locksFile.getPath() + "': " + e.getMessage());
            log.debug("Root cause: ", e);
        }
    }

    /**
     * Read locks from locks file and populate path map
     */
    private void load() throws IOException {
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(locksFile));
            while (true) {
                String s = reader.readLine();
                if (s == null || s.equals("")) {
                    break;
                }
                reapplyLock(LockToken.parse(s));
            }
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
     * @param lockToken lock token to apply
     */
    private void reapplyLock(LockToken lockToken) {
        try {
            NodeId id = new NodeId(lockToken.uuid);

            NodeImpl node = (NodeImpl) session.getItemManager().getItem(id);
            Path path = getPath(node.getId());

            LockInfo info = new LockInfo(this, lockToken, false,
                    node.getProperty(Constants.JCR_LOCKISDEEP).getBoolean(),
                    node.getProperty(Constants.JCR_LOCKOWNER).getString());
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
    private void save() throws IOException {
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
            writer = new BufferedWriter(new FileWriter(locksFile));
            for (int i = 0; i < list.size(); i++) {
                LockInfo info = (LockInfo) list.get(i);
                writer.write(info.lockToken.toString());
                writer.newLine();
            }
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
     * Internal <code>lock</code> implementation that takes as parameter
     * a lock info that will be used inside the path map.<p>
     * In order to prevent deadlocks from within the synchronous dispatching of
     * events, content modifications should not be made from within code
     * sections that hold monitors. (see #JCR-194)
     * @param node node to lock
     * @param info lock info
     * @throws LockException if the node is already locked
     * @throws RepositoryException if another error occurs
     * @return lock
     */
    Lock lock(NodeImpl node, LockInfo info)
            throws LockException,  RepositoryException {

        Lock lock;

        synchronized (contentMonitor) {
            synchronized (lockMap) {
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
                if (info.deep && element.hasPath(path) &&
                        element.getChildrenCount() > 0) {
                    throw new LockException("Some child node is locked.");
                }

                // create lock token
                SessionImpl session = (SessionImpl) node.getSession();
                info.setLockHolder(session);
                info.setLive(true);
                session.addListener(info);
                session.addLockToken(info.lockToken.toString(), false);
                lockMap.put(path, info);
                lock = new LockImpl(info, node);
            }

            // add properties to content
            node.internalSetProperty(Constants.JCR_LOCKOWNER,
                    InternalValue.create(node.getSession().getUserID()));
            node.internalSetProperty(Constants.JCR_LOCKISDEEP,
                    InternalValue.create(info.deep));
            node.save();

            return lock;
        }
    }

    /**
     * Unlock a node given by its info. Invoked when a session logs out and
     * all session scoped locks of that session must be unlocked.<p>
     * In order to prevent deadlocks from within the synchronous dispatching of
     * events, content modifications should not be made from within code
     * sections that hold monitors. (see #JCR-194)
     * @param info lock info
     */
    void unlock(LockInfo info) {
        // if no session currently holds lock, take system session
        SessionImpl session = info.getLockHolder();
        if (session == null) {
            session = this.session;
        }

        try {
            synchronized (contentMonitor) {
                // get node's path and remove child in path map
                NodeImpl node = (NodeImpl) session.getItemManager().getItem(
                        new NodeId(info.getUUID()));
                Path path = getPath(node.getId());

                synchronized (lockMap) {
                    PathMap.Element element = lockMap.map(path, true);
                    if (element != null) {
                        element.set(null);
                    }

                    // set live flag to false
                    info.setLive(false);
                }

                // remove properties in content
                node.internalSetProperty(Constants.JCR_LOCKOWNER, (InternalValue) null);
                node.internalSetProperty(Constants.JCR_LOCKISDEEP, (InternalValue) null);
                node.save();
            }

        } catch (RepositoryException e) {
            log.warn("Unable to unlock session-scoped lock on node '"
                    + info.lockToken + "': " + e.getMessage());
            log.debug("Root cause: ", e);
        }

    }

    //-----------------------------------------------------------< LockManager >

    /**
     * {@inheritDoc}
     */
    public Lock lock(NodeImpl node, boolean isDeep, boolean isSessionScoped)
            throws LockException, RepositoryException {

        // create lock info to use and pass to internal implementation
        LockInfo info = new LockInfo(this, new LockToken(node.internalGetUUID()),
                isSessionScoped, isDeep, node.getSession().getUserID());
        return lock(node, info);
    }

    /**
     * {@inheritDoc}
     */
    public Lock getLock(NodeImpl node)
            throws LockException, RepositoryException {

        synchronized (lockMap) {
            Path path = getPath(node.getId());

            PathMap.Element element = lockMap.map(path, false);
            LockInfo info = (LockInfo) element.get();
            if (info == null) {
                throw new LockException("Node not locked: " + node.safeGetJCRPath());
            }
            if (element.hasPath(path) || info.deep) {
                SessionImpl session = (SessionImpl) node.getSession();
                Node lockHolder = (Node) session.getItemManager().getItem(
                        new NodeId(info.getUUID()));
                return new LockImpl(info, lockHolder);
            } else {
                throw new LockException("Node not locked: " + node.safeGetJCRPath());
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * In order to prevent deadlocks from within the synchronous dispatching of
     * events, content modifications should not be made from within code
     * sections that hold monitors. (see #JCR-194)
     */
    public void unlock(NodeImpl node)
            throws LockException, RepositoryException {

        synchronized (contentMonitor) {
            synchronized (lockMap) {
                // check whether node is locked by this session
                PathMap.Element element = lockMap.map(getPath(node.getId()), true);
                if (element == null) {
                    throw new LockException("Node not locked: " + node.safeGetJCRPath());
                }

                LockInfo info = (LockInfo) element.get();
                if (info == null) {
                    throw new LockException("Node not locked: " + node.safeGetJCRPath());
                }
                if (!node.getSession().equals(info.getLockHolder())) {
                    throw new LockException("Node not locked by session: " + node.safeGetJCRPath());
                }

                // remove lock in path map
                element.set(null);
                info.setLive(false);
            }

            // remove properties in content
            node.internalSetProperty(Constants.JCR_LOCKOWNER, (InternalValue) null);
            node.internalSetProperty(Constants.JCR_LOCKISDEEP, (InternalValue) null);
            node.save();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean holdsLock(NodeImpl node) throws RepositoryException {
        synchronized (lockMap) {
            PathMap.Element element = lockMap.map(getPath(node.getId()), true);
            if (element == null) {
                return false;
            }
            return element.get() != null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLocked(NodeImpl node) throws RepositoryException {
        synchronized (lockMap) {
            Path path = getPath(node.getId());

            PathMap.Element element = lockMap.map(path, false);
            LockInfo info = (LockInfo) element.get();
            if (info == null) {
                return false;
            }
            if (element.hasPath(path)) {
                return true;
            } else {
                return info.deep;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkLock(NodeImpl node)
            throws LockException, RepositoryException {

        checkLock(getPath(node.getId()), node.getSession());
    }

    /**
     * {@inheritDoc}
     */
    public void checkLock(Path path, Session session)
            throws LockException, RepositoryException {

        PathMap.Element element = lockMap.map(path, false);
        LockInfo info = (LockInfo) element.get();
        if (info != null) {
            if (element.hasPath(path) || info.deep) {
                if (!session.equals(info.getLockHolder())) {
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
                    getItem(new NodeId(lockToken.uuid));
            PathMap.Element element = lockMap.map(node.getPrimaryPath(), true);
            if (element != null) {
                LockInfo info = (LockInfo) element.get();
                if (info != null) {
                    if (info.getLockHolder() == null) {
                        info.setLockHolder(session);
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
                    getItem(new NodeId(lockToken.uuid));
            PathMap.Element element = lockMap.map(node.getPrimaryPath(), true);
            if (element != null) {
                LockInfo info = (LockInfo) element.get();
                if (info != null) {
                    if (session.equals(info.getLockHolder())) {
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
     * item inside the system session and will therefore not find transiently
     * created but not yet saved items
     */
    private Path getPath(ItemId id)
            throws ItemNotFoundException, AccessDeniedException,
                   RepositoryException {

        return session.getHierarchyManager().getPath(id);
    }
    
    //----------------------------------------------< SynchronousEventListener >

    /**
     * Internal event class that holds old and new paths for moved nodes
     */
    private class HierarchyEvent {

        /**
         * UUID recorded in event
         */
        public final String uuid;

        /**
         * Path recorded in event
         */
        public final Path path;

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
         * @param uuid uuid
         * @param path path
         * @param type event type
         */
        public HierarchyEvent(String uuid, Path path, int type) {
            this.uuid = uuid;
            this.path = path;
            this.type = type;
        }

        /**
         * Merge this event with another event. The result will be stored in
         * this event
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
         * @return event type
         */
        public int getType() {
            return type;
        }

        /**
         * Return the old path if this is a move operation
         * @return old path
         */
        public Path getOldPath() {
            return oldPath;
        }

        /**
         * Return the new path if this is a move operation
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
                he = new HierarchyEvent(event.getChildUUID(),
                        Path.create(event.getPath(), nsResolver, true),
                        event.getType());
            } catch (MalformedPathException e) {
                log.info("Unable to get event's path: " + e.getMessage());
                continue;
            } catch (RepositoryException e) {
                log.info("Unable to get event's path: " + e.getMessage());
                continue;
            }

            HierarchyEvent heExisting = (HierarchyEvent) eventMap.get(he.uuid);
            if (heExisting != null) {
                heExisting.merge(he);
            } else {
                eventMap.put(he.uuid, he);
            }
        }
        return eventMap.values().iterator();
    }

    /**
     * Invoked when some node has been added. If the parent of that node
     * exists, shift all name siblings of the new node having an index greater
     * or equal.
     * @param path path of added node
     */
    private void nodeAdded(Path path) {
        try {
            synchronized (lockMap) {
                PathMap.Element parent = lockMap.map(path.getAncestor(1), true);
                if (parent != null) {
                    parent.insert(path.getNameElement());
                }
            }
        } catch (PathNotFoundException e) {
            log.warn("Unable to determine path of added node's parent.", e);
            return;
        }
    }

    /**
     * Invoked when some node has been moved. Relink the child inside our
     * map to the new parent.
     * @param oldPath old path
     */
    private void nodeMoved(Path oldPath, Path newPath) {
        synchronized (lockMap) {
            PathMap.Element element = lockMap.map(oldPath, true);
            if (element != null) {
                element.remove();
            }

            try {
                PathMap.Element parent = lockMap.map(newPath.getAncestor(1), true);
                if (parent != null) {
                    parent.insert(newPath.getNameElement());
                }
                if (element != null) {
                    lockMap.put(newPath, element);
                }
            } catch (PathNotFoundException e) {
                log.warn("Unable to determine path of moved node's parent.", e);
                return;
            }
        }
    }

    /**
     * Invoked when some node has been removed. Remove the child from our
     * path map. Disable all locks contained in that subtree.
     * @param path path of removed node
     */
    private void nodeRemoved(Path path) {
        synchronized (lockMap) {
            try {
                PathMap.Element parent = lockMap.map(path.getAncestor(1), true);
                if (parent != null) {
                    PathMap.Element element = parent.remove(path.getNameElement());
                    if (element != null) {
                        element.traverse(new PathMap.ElementVisitor() {
                            public void elementVisited(PathMap.Element element) {
                                LockInfo info = (LockInfo) element.get();
                                info.setLive(false);
                            }
                        }, false);
                    }
                }
            } catch (PathNotFoundException e) {
                log.warn("Unable to determine path of moved node's parent.", e);
                return;
            }
        }
    }
}
