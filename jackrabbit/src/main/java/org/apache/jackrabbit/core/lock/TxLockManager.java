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

import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.TransactionException;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.QName;
import org.apache.log4j.Logger;

import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * Session-local lock manager that implements the semantical changes inside
 * transactions. This manager validates lock/unlock operations inside its
 * view of the locking space.
 */
public class TxLockManager implements LockManager {

    /**
     * Logger instance for this class
     */
    private static final Logger log = Logger.getLogger(TxLockManager.class);

    /**
     * Shared lock manager.
     */
    private final SharedLockManager shared;

    /**
     * Map of locked nodes, indexed by their (internal) id.
     */
    private final Map lockedNodesMap = new HashMap();

    /**
     * Map of unlocked nodes, indexed by their (internal) id.
     */
    private final Map unlockedNodesMap = new HashMap();

    /**
     * List of lock/unlock operations.
     */
    private final List operations = new ArrayList();

    /**
     * Operation index.
     */
    private int opIndex;

    /**
     * Create a new instance of this class. Takes a <code>SharedLockManager</code>
     * as parameter.
     * @param shared shared lock manager
     */
    public TxLockManager(SharedLockManager shared) {
        this.shared = shared;
    }

    /**
     * Clear this object, releasing all its resources.
     */
    public void clear() {
        lockedNodesMap.clear();
        unlockedNodesMap.clear();
        operations.clear();
        opIndex = 0;
    }

    //----------------------------------------------------------< LockManager >

    /**
     * {@inheritDoc}
     */
    public Lock lock(NodeImpl node, boolean isDeep, boolean isSessionScoped)
            throws LockException, RepositoryException {

        String uuid = node.internalGetUUID();

        // check negative set first
        LockInfo info = (LockInfo) unlockedNodesMap.get(uuid);
        if (info != null) {

            // if settings are compatible, this is effectively a no-op
            if (info.deep == isDeep && info.sessionScoped == isSessionScoped) {
                unlockedNodesMap.remove(uuid);
                operations.remove(info);
                return getLock(node);
            }
        }

        // verify node is not already locked.
        if (isLocked(node)) {
            throw new LockException("Node locked.");
        }

        // create a new lock info for this node
        info = new LockInfo(node, new LockToken(node.internalGetUUID()),
                isSessionScoped, isDeep, node.getSession().getUserID());
        SessionImpl session = (SessionImpl) node.getSession();
        info.setLockHolder(session);
        info.setLive(true);
        session.addLockToken(info.lockToken.toString(), false);
        lockedNodesMap.put(uuid, info);
        operations.add(info);

        // add properties to content
        node.internalSetProperty(QName.JCR_LOCKOWNER,
                InternalValue.create(node.getSession().getUserID()));
        node.internalSetProperty(QName.JCR_LOCKISDEEP,
                InternalValue.create(info.deep));
        node.save();
        return new LockImpl(info, node);
    }

    /**
     * {@inheritDoc}
     */
    public Lock getLock(NodeImpl node) throws LockException, RepositoryException {
        LockInfo info = getLockInfo(node);
        if (info == null) {
            throw new LockException("Node not locked: " + node.safeGetJCRPath());
        }
        SessionImpl session = (SessionImpl) node.getSession();
        NodeImpl holder = (NodeImpl) session.getItemManager().getItem(
                new NodeId(info.getUUID()));
        return new LockImpl(info, holder);
    }

    /**
     * {@inheritDoc}
     */
    public void unlock(NodeImpl node) throws LockException, RepositoryException {
        String uuid = node.internalGetUUID();

        // check positive set first
        LockInfo info = (LockInfo) lockedNodesMap.get(uuid);
        if (info != null) {
            lockedNodesMap.remove(uuid);
            operations.remove(info);
            info.setLive(false);
        } else {
            info = getLockInfo(node);
            if (info == null || info.getUUID() != uuid) {
                throw new LockException("Node not locked.");
            } else if (info.getLockHolder() != node.getSession()) {
                throw new LockException("Node not locked by this session.");
            }
            unlockedNodesMap.put(uuid, info);
            info.setUnlock(true);
            operations.add(info);
        }

        // remove properties in content
        node.internalSetProperty(QName.JCR_LOCKOWNER, (InternalValue) null);
        node.internalSetProperty(QName.JCR_LOCKISDEEP, (InternalValue) null);
        node.save();
    }

    /**
     * {@inheritDoc}
     */
    public boolean holdsLock(NodeImpl node) throws RepositoryException {
        String uuid = node.internalGetUUID();

        if (lockedNodesMap.containsKey(uuid)) {
            return true;
        }
        return shared.holdsLock(node);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLocked(NodeImpl node) throws RepositoryException {
        LockInfo info = getLockInfo(node);
        return info != null;
    }

    /**
     * {@inheritDoc}
     */
    public void checkLock(NodeImpl node) throws LockException, RepositoryException {
        LockInfo info = getLockInfo(node);
        if (info != null && info.getLockHolder() != node.getSession()) {
            throw new LockException("Node locked.");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkLock(Path path, Session session)
            throws LockException, RepositoryException {

        SessionImpl sessionImpl = (SessionImpl) session;
        checkLock((NodeImpl) sessionImpl.getItemManager().getItem(path));
    }

    /**
     * {@inheritDoc}
     */
    public void lockTokenAdded(SessionImpl session, String lt) {
    }

    /**
     * {@inheritDoc}
     */
    public void lockTokenRemoved(SessionImpl session, String lt) {
    }

    //-----------------------------------------------------------< transaction >

    /**
     * Prepare transaction. This will lock the shared lock manager and feed
     * all locks.
     */
    public void prepare() throws TransactionException {
        if (operations.isEmpty()) {
            return;
        }

        shared.beginUpdate();

        try {
            while (opIndex < operations.size()) {
                try {
                    LockInfo info = (LockInfo) operations.get(opIndex);
                    info.update();
                } catch (RepositoryException e) {
                    throw new TransactionException("Unable to update.", e);
                }
                opIndex++;
            }
        } finally {
            if (opIndex < operations.size()) {
                while (opIndex > 0) {
                    try {
                        LockInfo info = (LockInfo) operations.get(opIndex - 1);
                        info.undo();
                    } catch (RepositoryException e) {
                        log.error("Unable to undo lock operation.", e);
                    }
                    opIndex--;
                }
            }
        }
    }

    /**
     * Commit transaction. This will finish the update and unlock the shared
     * lock manager.
     */
    public void commit() {
        if (!operations.isEmpty()) {
            shared.endUpdate();
        }
        clear();
    }

    /**
     * Rollback transaction. This will undo all updates and unlock the shared
     * lock manager.
     */
    public void rollback() {
        if (!operations.isEmpty() && opIndex == operations.size()) {
            while (opIndex > 0) {
                try {
                    LockInfo info = (LockInfo) operations.get(opIndex - 1);
                    info.undo();
                } catch (RepositoryException e) {
                    log.error("Unable to undo lock operation.", e);
                }
                opIndex--;
            }
            shared.cancelUpdate();
        }
        clear();
    }

    //--------------------------------------------------------------< internal >

    /**
     * Return the most appropriate lock information for a node. This is either
     * the lock info for the node itself, if it is locked, or a lock info for
     * one of its parents, if that one is deep locked.
     * @param node node
     * @return LockInfo lock info or <code>null</code> if node is not locked
     * @throws RepositoryException if an error occurs
     */
    private LockInfo getLockInfo(NodeImpl node) throws RepositoryException {
        String uuid = node.internalGetUUID();

        // check negative set
        if (unlockedNodesMap.containsKey(uuid)) {
            return null;
        }

        // check positive set, iteratively ascending in hierarchy
        if (!lockedNodesMap.isEmpty()) {
            NodeImpl current = node;
            for (;;) {
                LockInfo info = (LockInfo) lockedNodesMap.get(current.internalGetUUID());
                if (info != null) {
                    if (info.getUUID() == uuid || info.deep) {
                        return info;
                    }
                    break;
                }
                if (current.getDepth() == 0) {
                    break;
                }
                current = (NodeImpl) current.getParent();
            }
        }

        // ask parent and return a copy of its information
        AbstractLockInfo info = shared.getLockInfo(uuid);
        if (info == null) {
            return null;
        }
        return new LockInfo(node, info.lockToken, info.sessionScoped,
                info.deep, info.lockOwner);
    }

    /**
     * Information about a lock used inside transactions.
     */
    class LockInfo extends AbstractLockInfo {

        /**
         * Node being locked/unlocked.
         */
        private final NodeImpl node;

        /**
         * Flag indicating whether this info belongs to a unlock operation.
         */
        private boolean isUnlock;

        /**
         * Create a new instance of this class.
         * @param lockToken     lock token
         * @param sessionScoped whether lock token is session scoped
         * @param deep          whether lock is deep
         * @param lockOwner     owner of lock
         */
        public LockInfo(NodeImpl node, LockToken lockToken,
                        boolean sessionScoped, boolean deep, String lockOwner) {

            super(lockToken, sessionScoped, deep, lockOwner);

            this.node = node;
        }

        /**
         * Return a flag indicating whether this info belongs to a unlock operation.
         * @return <code>true</code> if this info belongs to an unlock operation;
         *         otherwise <code>false</code>
         */
        public boolean isUnlock() {
            return isUnlock;
        }

        /**
         * Set a flag indicating whether this info belongs to an unlock operation.
         * @param isUnlock <code>true</code> if this info belongs to an unlock operation;
         *                 otherwise <code>false</code>
         */
        public void setUnlock(boolean isUnlock) {
            this.isUnlock = isUnlock;
        }

        /**
         * Do operation.
         */
        public void update() throws LockException, RepositoryException {
            if (isUnlock) {
                shared.internalUnlock(node);
            } else {
                shared.internalLock(node, deep, sessionScoped);
            }
        }

        /**
         * Undo operation.
         */
        public void undo() throws LockException, RepositoryException {
            if (isUnlock) {
                shared.internalLock(node, deep, sessionScoped);
            } else {
                shared.internalUnlock(node);
            }
        }
    }
}
