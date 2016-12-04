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

import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.WorkspaceImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.data.core.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Encapsulates operations that happen in an XA environment.
 */
class XAEnvironment {

    /**
     * Logger instance for this class
     */
    private static final Logger log = LoggerFactory.getLogger(XAEnvironment.class);

    private static final int STATUS_PREPARING = 1;
    private static final int STATUS_PREPARED = 2;
    private static final int STATUS_COMMITTING = 3;
    private static final int STATUS_COMMITTED = 4;
    private static final int STATUS_ROLLING_BACK = 5;
    private static final int STATUS_ROLLED_BACK = 6;

    /**
     * Global lock manager.
     */
    private final LockManagerImpl lockMgr;

    /**
     * Map of locked nodes, indexed by their (internal) id.
     */
    private final Map<NodeId, XALockInfo> lockedNodesMap =
            new HashMap<NodeId, XALockInfo>();

    /**
     * Map of unlocked nodes, indexed by their (internal) id.
     */
    private final Map<NodeId, XALockInfo> unlockedNodesMap =
            new HashMap<NodeId, XALockInfo>();

    /**
     * List of lock/unlock operations.
     */
    private final List<XALockInfo> operations = new ArrayList<XALockInfo>();

    /**
     * Operation index.
     */
    private int opIndex;

    /**
     * Current status.
     */
    private int status;

    /**
     * Create a new instance of this class.
     * @param lockMgr global lock manager
     */
    public XAEnvironment(LockManagerImpl lockMgr) {
        this.lockMgr = lockMgr;
    }

    /**
     * Reset this environment.
     */
    public void reset() {
        lockedNodesMap.clear();
        unlockedNodesMap.clear();
        operations.clear();
        opIndex = 0;
    }

    /**
     * Lock some node.
     * @param node node to lock
     * @param isDeep <code>true</code> to deep lock this node;
     *               <code>false</code> otherwise
     * @param isSessionScoped <code>true</code> if lock should be session scoped;
     *                        <code>false</code> otherwise
     * @throws LockException if node is already locked
     * @throws RepositoryException if an error occurs
     */
    public LockInfo lock(NodeImpl node, boolean isDeep, boolean isSessionScoped)
            throws LockException, RepositoryException {
        return lock(node, isDeep, isSessionScoped, Long.MAX_VALUE, null);
    }

    /**
     * Lock some node.
     * @param node node to lock
     * @param isDeep <code>true</code> to deep lock this node;
     *               <code>false</code> otherwise
     * @param isSessionScoped <code>true</code> if lock should be session scoped;
     *                        <code>false</code> otherwise
     * @param timeoutHint
     * @param ownerInfo
     * @throws LockException if node is already locked
     * @throws RepositoryException if an error occurs
     */
    public LockInfo lock(NodeImpl node, boolean isDeep, boolean isSessionScoped, long timeoutHint, String ownerInfo)
            throws LockException, RepositoryException {

        NodeId id = node.getNodeId();

        // check negative set first
        XALockInfo info = unlockedNodesMap.get(id);
        if (info != null) {
            // if settings are compatible, this is effectively a no-op
            if (info.isDeep() == isDeep && info.isSessionScoped() == isSessionScoped) {
                unlockedNodesMap.remove(id);
                operations.remove(info);
                return lockMgr.getLockInfo(id);
            }
        }

        // verify node is not already locked.
        if (isLocked(node)) {
            throw new LockException("Node locked.");
        }

        // create a new lock info for this node
        String lockOwner = (ownerInfo != null) ? ownerInfo : node.getSession().getUserID();
        info = new XALockInfo(node, isSessionScoped, isDeep, timeoutHint, lockOwner);
        SessionImpl session = (SessionImpl) node.getSession();
        info.setLockHolder(session);
        info.setLive(true);

        LockManagerImpl.getSessionLockManager(session).lockTokenAdded(info.getLockToken());
        lockedNodesMap.put(id, info);
        operations.add(info);

        return info;
    }

    /**
     * Unlock some node.
     * @param node node to unlock
     * @throws LockException if the node is not locked
     * @throws RepositoryException if an error occurs
     */
    public void unlock(NodeImpl node) throws LockException, RepositoryException {
        NodeId id = node.getNodeId();

        // check positive set first
        LockInfo info = lockedNodesMap.get(id);
        if (info != null) {
            lockedNodesMap.remove(id);
            operations.remove(info);
            info.setLive(false);
        } else {
            info = getLockInfo(node);
            if (info == null || !info.getId().equals(id)) {
                throw new LockException("Node not locked.");
            } else if (!info.isLockHolder(node.getSession())) {
                throw new LockException("Node not locked by this session.");
            }
            XALockInfo xaInfo = new XALockInfo(node, info);
            unlockedNodesMap.put(id, xaInfo);
            operations.add(xaInfo);
        }

    }

    /**
     * Return a flag indicating whether the specified node is locked.
     * @return <code>true</code> if this node is locked;
     *         <code>false</code> otherwise
     * @throws RepositoryException if an error occurs
     */
    public boolean isLocked(NodeImpl node) throws RepositoryException {
        return getLockInfo(node) != null;
    }

    /**
     * Return the most appropriate lock information for a node. This is either
     * the lock info for the node itself, if it is locked, or a lock info for
     * one of its parents, if that one is deep locked.
     * @param node node
     * @return LockInfo lock info or <code>null</code> if node is not locked
     * @throws RepositoryException if an error occurs
     */
    public LockInfo getLockInfo(NodeImpl node) throws RepositoryException {
        NodeId id = node.getNodeId();

        // check negative set
        if (unlockedNodesMap.containsKey(id)) {
            return null;
        }

        // check positive set, iteratively ascending in hierarchy
        if (!lockedNodesMap.isEmpty()) {
            NodeImpl current = node;
            for (;;) {
                XALockInfo info = lockedNodesMap.get(current.getId());
                if (info != null) {
                    if (info.getId().equals(id) || info.isDeep()) {
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
        // ask parent
        return lockMgr.getLockInfo(id);
    }

    /**
     * Returns all locks associated with the specified session.
     * @param session session
     * @return locks associated with the session
     * @throws RepositoryException if an error occurs
     */
    public LockInfo[] getLockInfos(SessionImpl session)
            throws RepositoryException {
        ArrayList<LockInfo> result = new ArrayList<LockInfo>();

        // get lock information from global lock manager first
        for (LockInfo info : lockMgr.getLockInfos(session)) {
            // check negative set
            if (!unlockedNodesMap.containsKey(info.getId())) {
                result.add(info);
            }
        }

        // add 'uncommitted' lock information
        result.addAll(lockedNodesMap.values());

        return result.toArray(new LockInfo[result.size()]);
    }

    /**
     * Add lock token to this environment.
     * @param session
     * @param lt lock token
     * @throws RepositoryException
     */
    public void addLockToken(SessionImpl session, String lt) throws RepositoryException {
        try {
            NodeId id = LockInfo.parseLockToken(lt);
            NodeImpl node = (NodeImpl) session.getItemManager().getItem(id);
            LockInfo info = getLockInfo(node);
            if (info != null && !info.isLockHolder(session)) {
                if (info.getLockHolder() == null) {
                    info.setLockHolder(session);
                } else {
                    String msg = "Cannot add lock token: lock already held by other session.";
                    log.warn(msg);
                    throw new LockException(msg);
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
     * Remove lock token from this environment.
     * @param session
     * @param lt lock token
     * @throws RepositoryException
     */
    public void removeLockToken(SessionImpl session, String lt) throws RepositoryException {
        try {
            NodeId id = LockInfo.parseLockToken(lt);

            NodeImpl node = (NodeImpl) session.getItemManager().getItem(id);
            LockInfo info = getLockInfo(node);
            if (info != null) {
                if (info.isLockHolder(session)) {
                    info.setLockHolder(null);
                } else if (info.getLockHolder() != null) {
                    String msg = "Cannot remove lock token: lock held by other session.";
                    log.warn(msg);
                    throw new LockException(msg);
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

    static SessionLockManager getSessionLockManager(SessionImpl session) throws RepositoryException {
        Workspace wsp = session.getWorkspace();
        return (SessionLockManager) wsp.getLockManager();
    }

    /**
     * Prepare update. Locks global lock manager and feeds all lock/
     * unlock operations.
     */
    public void prepare() throws TransactionException {
        status = STATUS_PREPARING;
        if (!operations.isEmpty()) {
            lockMgr.beginUpdate();

            try {
                while (opIndex < operations.size()) {
                    try {
                        XALockInfo info = operations.get(opIndex);
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
                            XALockInfo info = operations.get(opIndex - 1);
                            info.undo();
                        } catch (RepositoryException e) {
                            log.error("Unable to undo lock operation.", e);
                        }
                        opIndex--;
                    }
                    lockMgr.cancelUpdate();
                }
            }
        }
        status = STATUS_PREPARED;
    }

    /**
     * Commit changes. This will finish the update and unlock the
     * global lock manager.
     */
    public void commit() {
        int oldStatus = status;

        status = STATUS_COMMITTING;
        if (oldStatus == STATUS_PREPARED) {
            if (!operations.isEmpty()) {
                lockMgr.endUpdate();
                reset();
            }
        }
        status = STATUS_COMMITTED;
    }

    /**
     * Rollback changes. This will undo all updates and unlock the
     * global lock manager.
     */
    public void rollback() {
        int oldStatus = status;

        status = STATUS_ROLLING_BACK;
        if (oldStatus == STATUS_PREPARED) {
            if (!operations.isEmpty()) {
                while (opIndex > 0) {
                    try {
                        XALockInfo info = operations.get(opIndex - 1);
                        info.undo();
                    } catch (RepositoryException e) {
                        log.error("Unable to undo lock operation.", e);
                    }
                    opIndex--;
                }
                lockMgr.cancelUpdate();
                reset();
            }
        }
        status = STATUS_ROLLED_BACK;
    }

    /**
     * Return a flag indicating whether a lock info belongs to a different
     * XA environment.
     */
    public boolean differentXAEnv(LockInfo info) {
        if (info instanceof XALockInfo) {
            XALockInfo lockInfo = (XALockInfo) info;
            return lockInfo.getXAEnv() != this;
        }
        return true;
    }

    /**
     * Information about a lock used inside transactions.
     */
    class XALockInfo extends LockInfo {

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
         * @param sessionScoped whether lock token is session scoped
         * @param deep          whether lock is deep
         * @param lockOwner     owner of lock
         */
        public XALockInfo(
                NodeImpl node,
                boolean sessionScoped, boolean deep, long timeoutHint, String lockOwner) {
            super(node.getNodeId(), sessionScoped, deep, lockOwner, timeoutHint);
            this.node = node;
        }

        /**
         * Create a new instance of this class. Used to signal an
         * unlock operation on some existing lock information.
         */
        public XALockInfo(NodeImpl node, LockInfo info) {
            super(info);
            this.node = node;
            this.isUnlock = true;
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
         * Do operation.
         */
        public void update() throws LockException, RepositoryException {
            if (isUnlock) {
                // Only if we have a valid ItemState try to unlock
                // JCR-2332
                if (((WorkspaceImpl) node.getSession().getWorkspace()).getItemStateManager().hasItemState(node.getId())) {
                    lockMgr.internalUnlock(node);
                }
            } else {
                LockInfo internalLock = lockMgr.internalLock(
                        node, isDeep(), isSessionScoped(),
                        getTimeoutHint(),
//                        getTimeoutTime(),
                        getLockOwner());
                LockInfo xaEnvLock = getLockInfo(node);
                // Check if the lockToken has been removed in the transaction ...
                if (xaEnvLock != null && xaEnvLock.getLockHolder() == null) {
                    //Remove lockToken from SessionLockManager
                    getSessionLockManager(internalLock.getLockHolder()).lockTokenRemoved(internalLock.getLockToken());
                    internalLock.setLockHolder(null);
                }
            }
        }

        /**
         * Undo operation.
         */
        public void undo() throws LockException, RepositoryException {
            if (isUnlock) {
                lockMgr.internalLock(
                        node, isDeep(), isSessionScoped(),
                        getTimeoutHint(), getLockOwner());
            } else {
                lockMgr.internalUnlock(node);
            }
        }

        /**
         * Return parent environment.
         */
        public XAEnvironment getXAEnv() {
            return XAEnvironment.this;
        }

        /**
         * {@inheritDoc}
         * <p>
         * As long as the XA environment is neither committed nor rolled back,
         * associated lock information is subject to change.
         */
        @Override
        public boolean mayChange() {
            if (status != STATUS_COMMITTED
                    && status != STATUS_ROLLED_BACK) {
                return true;
            }
            return super.mayChange();
        }
    }
}
