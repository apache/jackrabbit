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

import org.apache.jackrabbit.core.*;
import org.apache.jackrabbit.core.observation.SynchronousEventListener;
import org.apache.jackrabbit.core.observation.EventImpl;
import org.apache.log4j.Logger;

import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.PathNotFoundException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.*;

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
     * Name space resolver
     */
    private final NamespaceResolver nsResolver;

    /**`
     * Map of nodes that been removed and may be re-added as result
     * of a move operation
     */
    private final Map zombieNodes = new HashMap();

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

        ((WorkspaceImpl) session.getWorkspace()).getObservationManager().
                addEventListener(this, Event.NODE_ADDED|Event.NODE_REMOVED,
                        "/", true, null, null, true);

        if (locksFile.exists()) {
            try {
                load();
            } catch (IOException e) {
                throw new RepositoryException(
                        "I/O error while reading locks from '" +
                        locksFile.getPath() + "'", e);
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
            log.warn("I/O error while saving locks to '" +
                    locksFile.getPath() + "': " + e.getMessage());
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
            NodeImpl node = (NodeImpl) session.getNodeByUUID(lockToken.uuid);
            Path path = node.getPrimaryPath();

            LockInfo info = new LockInfo(lockToken, false,
                    node.getProperty(Constants.JCR_LOCKISDEEP).getBoolean(),
                    node.getProperty(Constants.JCR_LOCKOWNER).getString());
            lockMap.put(path, info);
        } catch (RepositoryException e) {
            log.warn("Unable to recreate lock '" + lockToken +
                    "': " + e.getMessage());
            log.debug("Root cause: ", e);
        }
    }

    /**
     * Write locks to locks file
     */
    private void save() throws IOException {
        final ArrayList list = new ArrayList();

        lockMap.traverse(new PathMap.ChildVisitor() {
            public void childVisited(PathMap.Child child) {
                LockInfo info = (LockInfo) child.get();
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

    //-----------------------------------------------------------< LockManager >

    /**
     * {@inheritDoc}
     */
    public synchronized Lock lock(NodeImpl node, boolean isDeep,
                                  boolean isSessionScoped)
            throws LockException, RepositoryException {

        Path path = node.getPrimaryPath();
        PathMap.Child child = lockMap.map(path, false);

        LockInfo info = (LockInfo) child.get();
        if (info != null) {
            if (child.hasPath(path)) {
                throw new LockException("Node already locked: " + path);
            } else if (info.deep) {
                throw new LockException("Parent node has deep lock.");
            }
        }
        if (isDeep && child.hasPath(path)) {
            throw new LockException("Some child node is locked.");
        }

        SessionImpl session = (SessionImpl) node.getSession();
        info = new LockInfo(new LockToken(node.internalGetUUID()),
                isSessionScoped, isDeep, session.getUserId());
        info.setLockHolder(session);
        if (isSessionScoped) {
            session.addListener(info);
        }
        session.addLockToken(info.lockToken.toString(), false);
        lockMap.put(path, info);
        return new LockImpl(info, node);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Lock getLock(NodeImpl node)
            throws LockException, RepositoryException {

        Path path = node.getPrimaryPath();

        PathMap.Child child = lockMap.map(path, false);
        LockInfo info = (LockInfo) child.get();
        if (info == null) {
            throw new LockException("Node not locked: " + path);
        }
        if (child.hasPath(path) || info.deep) {
            return new LockImpl(info, node);
        } else {
            throw new LockException("Node not locked: " + path);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void unlock(NodeImpl node)
            throws LockException, RepositoryException {

        Path path = node.getPrimaryPath();

        PathMap.Child child = lockMap.map(path, true);
        if (child == null) {
            throw new LockException("Node not locked: " + path);
        }

        LockInfo info = (LockInfo) child.get();
        if (info == null) {
            throw new LockException("Node not locked: " + path);
        }
        if (!node.getSession().equals(info.getLockHolder())) {
            throw new LockException("Node not locked by session: " + path);
        }
        child.remove();

        info.setLive(false);
        info.setLockHolder(null);
        session.removeLockToken(info.lockToken.toString(), false);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized boolean holdsLock(NodeImpl node) throws RepositoryException {
        PathMap.Child child = lockMap.map(node.getPrimaryPath(), true);
        if (child == null) {
            return false;
        }
        return child.get() != null;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized boolean isLocked(NodeImpl node) throws RepositoryException {
        Path path = node.getPrimaryPath();

        PathMap.Child child = lockMap.map(path, false);
        LockInfo info = (LockInfo) child.get();
        if (info == null) {
            return false;
        }
        if (child.hasPath(path)) {
            return true;
        } else {
            return info.deep;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkLock(NodeImpl node)
            throws LockException, RepositoryException {

        checkLock(node.getPrimaryPath(), node.getSession());
    }

    /**
     * {@inheritDoc}
     */
    public void checkLock(Path path, Session session)
            throws LockException, RepositoryException {

        PathMap.Child child = lockMap.map(path, false);
        LockInfo info = (LockInfo) child.get();
        if (info != null) {
            if (child.hasPath(path) || info.deep) {
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

            NodeImpl node = (NodeImpl) session.getItemManager().
                    getItem(new NodeId(lockToken.uuid));
            PathMap.Child child = lockMap.map(node.getPrimaryPath(), true);
            if (child != null) {
                LockInfo info = (LockInfo) child.get();
                if (info != null) {
                    if (info.getLockHolder() == null) {
                        info.setLockHolder(session);
                    } else {
                        log.warn("Adding lock token has no effect: " +
                                "lock already held by other session.");
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

            NodeImpl node = (NodeImpl) session.getItemManager().
                    getItem(new NodeId(lockToken.uuid));
            PathMap.Child child = lockMap.map(node.getPrimaryPath(), true);
            if (child != null) {
                LockInfo info = (LockInfo) child.get();
                if (info != null) {
                    if (session.equals(info.getLockHolder())) {
                        info.setLockHolder(null);
                    } else {
                        log.warn("Removing lock token has no effect: " +
                                "lock held by other session.");
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

    //----------------------------------------------< SynchronousEventListener >

    /**
     * {@inheritDoc}
     */
    public void onEvent(EventIterator events) {
        while (events.hasNext()) {
            EventImpl event = (EventImpl) events.nextEvent();
            switch (event.getType()) {
                case Event.NODE_ADDED:
                    try {
                        childAdded(event.getChildUUID(),
                                Path.create(event.getPath(), nsResolver, true));
                    } catch (MalformedPathException e) {
                        log.info("Unable to get event's path: " + e.getMessage());
                    } catch (RepositoryException e) {
                        log.info("Unable to get event's path: " + e.getMessage());
                    }
                    break;
                case Event.NODE_REMOVED:
                    try {
                        childRemoved(event.getChildUUID(),
                                Path.create(event.getPath(), nsResolver, true));
                    } catch (MalformedPathException e) {
                        log.info("Unable to get event's path: " + e.getMessage());
                    } catch (RepositoryException e) {
                        log.info("Unable to get event's path: " + e.getMessage());
                    }
                    break;
            }
        }
    }

    /**
     * Invoked when some child has been added.
     */
    private synchronized void childAdded(String uuid, Path path) {
        try {
            PathMap.Child parent = lockMap.map(path.getAncestor(1), true);
            if (parent != null) {
                parent.insertChild(path.getNameElement());
            }
            PathMap.Child zombie = (PathMap.Child) zombieNodes.remove(uuid);
            if (zombie != null) {
                lockMap.resurrect(path, zombie);
            }
        } catch (PathNotFoundException e) {
            log.warn("Added child does not have parent, ignoring event.");
        }
    }

    /**
     * Invoked when some child has been removed.
     */
    private synchronized void childRemoved(String uuid, Path path) {
        try {
            PathMap.Child parent = lockMap.map(path.getAncestor(1), true);
            if (parent != null) {
                PathMap.Child child = parent.removeChild(path.getNameElement());
                if (child != null) {
                    zombieNodes.put(uuid, child);
                }
            }
        } catch (PathNotFoundException e) {
            log.warn("Added child does not have parent, ignoring event.");
        }
    }
}
