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
package org.apache.jackrabbit.jcr2spi.lock;

import org.apache.jackrabbit.jcr2spi.ItemManager;
import org.apache.jackrabbit.jcr2spi.SessionListener;
import org.apache.jackrabbit.jcr2spi.WorkspaceManager;
import org.apache.jackrabbit.jcr2spi.config.CacheBehaviour;
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.jcr2spi.operation.LockOperation;
import org.apache.jackrabbit.jcr2spi.operation.LockRefresh;
import org.apache.jackrabbit.jcr2spi.operation.LockRelease;
import org.apache.jackrabbit.jcr2spi.operation.Operation;
import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.ItemStateLifeCycleListener;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.PropertyState;
import org.apache.jackrabbit.jcr2spi.state.Status;
import org.apache.jackrabbit.spi.LockInfo;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Date;

/**
 * <code>LockManagerImpl</code>...
 * TODO: TOBEFIXED. Lock objects obtained through this mgr are not informed if another session is or becomes lock-holder and removes the lock again.
 */
public class LockManagerImpl implements LockStateManager, SessionListener {

    private static Logger log = LoggerFactory.getLogger(LockManagerImpl.class);

    private static final long TIMEOUT_EXPIRED = -1;
    private static final long TIMEOUT_INFINITE = Long.MAX_VALUE;

    /**
     * WorkspaceManager used to apply and release locks as well as to retrieve
     * Lock information for a given NodeState.
     * NOTE: The workspace manager must not be used as ItemStateManager.
     */
    private final WorkspaceManager wspManager;
    private final ItemManager itemManager;
    private final CacheBehaviour cacheBehaviour;

    /**
     * Map holding all locks that where created by this <code>Session</code> upon
     * calls to {@link LockStateManager#lock(NodeState,boolean,boolean)} or to
     * {@link LockStateManager#getLock(NodeState)}. The map entries are removed
     * only if a lock ends his life by {@link Node#unlock()} or by implicit
     * unlock upon {@link Session#logout()}.
     */
    private final Map<NodeState, LockImpl> lockMap;

    public LockManagerImpl(WorkspaceManager wspManager, ItemManager itemManager,
                           CacheBehaviour cacheBehaviour) {
        this.wspManager = wspManager;
        this.itemManager = itemManager;
        this.cacheBehaviour = cacheBehaviour;
        // use hard references in order to make sure, that entries referring
        // to locks created by the current session are not removed.
        lockMap = new HashMap<NodeState, LockImpl>();
    }

    //----------------< org.apache.jackrabbit.jcr2spi.lock.LockStateManager >---
    /**
     * @see LockStateManager#lock(NodeState,boolean,boolean)
     */
    public Lock lock(NodeState nodeState, boolean isDeep, boolean isSessionScoped) throws LockException, RepositoryException {
        return lock(nodeState, isDeep, isSessionScoped, Long.MAX_VALUE, null);
    }

    /**
     * @see LockStateManager#lock(NodeState,boolean,boolean,long,String)
     */
    public Lock lock(NodeState nodeState, boolean isDeep, boolean isSessionScoped, long timeoutHint, String ownerHint) throws RepositoryException {
        // retrieve node first
        Node lhNode;
        Item item = itemManager.getItem(nodeState.getHierarchyEntry());
        if (item.isNode()) {
            lhNode = (Node) item;
        } else {
            throw new RepositoryException("Internal error: ItemManager returned Property from NodeState");
        }

        // execute the operation
        LockOperation op = LockOperation.create(nodeState, isDeep, isSessionScoped, timeoutHint, ownerHint);
        wspManager.execute(op);

        Lock lock = new LockImpl(new LockState(nodeState, op.getLockInfo()), lhNode);
        return lock;
    }

    /**
     * @see LockStateManager#unlock(NodeState)
     */
    public void unlock(NodeState nodeState) throws LockException, RepositoryException {
        // execute the operation. Note, that its possible that the session is
        // lock holder and still the lock was never accessed. thus the lockMap
        // does not provide sufficient and reliable information.
        Operation op = LockRelease.create(nodeState);
        wspManager.execute(op);

        // if unlock was successful: clean up lock map and lock life cycle
        // in case the corresponding Lock object exists (and thus has been
        // added to the map.
        if (lockMap.containsKey(nodeState)) {
            LockImpl l = lockMap.remove(nodeState);
            l.lockState.unlocked();
        }
    }

    /**
     * If the session created a lock on the node with the given state, we already
     * know the lock. Otherwise, the node state and its ancestors are searched
     * for properties indicating a lock.<br>
     * Note, that the flag indicating session-scoped lock cannot be retrieved
     * unless the current session is the lock holder.
     *
     * @see LockStateManager#getLock(NodeState)
     * @param nodeState
     */
    public Lock getLock(NodeState nodeState) throws LockException, RepositoryException {
        LockImpl l = getLockImpl(nodeState, false);
        // no-lock found or lock doesn't apply to this state -> throw
        if (l == null) {
            throw new LockException("Node with id '" + nodeState.getNodeId() + "' is not locked.");
        }

        // a lock exists either on the given node state or as deep lock inherited
        // from any of the ancestor states.
        return l;
    }

    /**
     * @see LockStateManager#isLocked(NodeState)
     */
    public boolean isLocked(NodeState nodeState) throws RepositoryException {
        LockImpl l = getLockImpl(nodeState, false);
        return l != null;
    }

    /**
     * @see LockStateManager#checkLock(NodeState)
     */
    public void checkLock(NodeState nodeState) throws LockException, RepositoryException {
        // shortcut: new status indicates that a new state was already added
        // thus, the parent state is not locked by foreign lock.
        if (nodeState.getStatus() == Status.NEW) {
            return;
        }

        LockImpl l = getLockImpl(nodeState, true);
        if (l != null && !l.isLockOwningSession()) {
            // lock is present and token is null -> session is not lock-holder.
            throw new LockException("Node with id '" + nodeState + "' is locked.");
        } // else: state is not locked at all || session is lock-holder
    }


    /**
     * Returns the lock tokens present on the <code>SessionInfo</code> this
     * manager has been created with.
     *
     * @see LockStateManager#getLockTokens()
     */
    public String[] getLockTokens() throws UnsupportedRepositoryOperationException, RepositoryException {
        return wspManager.getLockTokens();
    }

    /**
     * Delegates this call to {@link WorkspaceManager#addLockToken(String)}.
     * If this succeeds this method will inform all locks stored in the local
     * map in order to give them the chance to update their lock information.
     *
     * @see LockStateManager#addLockToken(String)
     */
    public void addLockToken(String lt) throws LockException, RepositoryException {
        wspManager.addLockToken(lt);
        notifyTokenAdded(lt);
    }

    /**
     * If the lock addressed by the token is session-scoped, this method will
     * throw a LockException, such as defined by JSR170 v.1.0.1 for
     * {@link Session#removeLockToken(String)}.<br>Otherwise the call is
     * delegated to {@link WorkspaceManager#removeLockToken(String)}.
     * All locks stored in the local lock map are notified by the removed
     * token in order have them updated their lock information.
     *
     * @see LockStateManager#removeLockToken(String)
     */
    public void removeLockToken(String lt) throws LockException, RepositoryException {
        // JSR170 v. 1.0.1 defines that the token of a session-scoped lock may
        // not be moved over to another session. Thus removal is not possible
        // and the lock is always present in the lock map.
        Iterator<LockImpl> it = lockMap.values().iterator();
        boolean found = false;
        // loop over cached locks to determine if the token belongs to a session
        // scoped lock, in which case the removal must fail immediately.
        while (it.hasNext() && !found) {
            LockImpl l = it.next();
            if (lt.equals(l.getLockToken())) {
                // break as soon as the lock associated with the given token was found.
                found = true;
                if (l.isSessionScoped()) {
                    throw new LockException("Cannot remove lock token associated with a session scoped lock.");
                }
            }
        }

        // remove lock token from sessionInfo. call will fail, if the session
        // is not lock holder.
        wspManager.removeLockToken(lt);
        // inform about this lt being removed from this session
        notifyTokenRemoved(lt);
    }

    //----------------------------------------------------< SessionListener >---
    /**
     * @see SessionListener#loggingOut(Session)
     */
    public void loggingOut(Session session) {
        // remove any session scoped locks:
        NodeState[] lhStates = lockMap.keySet().toArray(new NodeState[lockMap.size()]);
        for (NodeState nState : lhStates) {
            LockImpl l = lockMap.get(nState);
            if (l.isSessionScoped() && l.isLockOwningSession()) {
                try {
                    unlock(nState);
                } catch (RepositoryException e) {
                    log.warn("Error while unlocking session scoped lock. Cleaning up local lock status.");
                    // at least clean up local lock map and the locks life cycle
                    l.lockState.unlocked();
                }
            }
        }
    }

    /**
     * @see SessionListener#loggedOut(Session)
     */
    public void loggedOut(Session session) {
        // release all remaining locks without modifying their lock status
        LockImpl[] locks = lockMap.values().toArray(new LockImpl[lockMap.size()]);
        for (LockImpl lock : locks) {
            lock.lockState.release();
        }
    }

    //------------------------------------------------------------< private >---

    /**
     * Search nearest ancestor that is locked. Returns <code>null</code> if neither
     * the given state nor any of its ancestors is locked.
     * Note, that this methods does NOT check if the given node state would
     * be affected by the lock present on an ancestor state.
     * Note, that in certain cases it might not be possible to detect a lock
     * being present due to the fact that the hierarchy might be incomplete or
     * not even readable completely. For this reason it seem equally reasonable
     * to search for jcr:lockIsDeep property only and omitting all kind of
     * verification regarding nodetypes present.
     *
     * @param nodeState <code>NodeState</code> from which searching starts.
     * Note, that the given state must not have an overlaid state.
     * @return a state holding a lock or <code>null</code> if neither the
     * given state nor any of its ancestors is locked.
     */
    private NodeState getLockHoldingState(NodeState nodeState) {
        NodeEntry entry = nodeState.getNodeEntry();
        while (!entry.hasPropertyEntry(NameConstants.JCR_LOCKISDEEP)) {
            NodeEntry parent = entry.getParent();
            if (parent == null) {
                // reached root state without finding a locked node
                return null;
            }
            entry = parent;
        }
        try {
            return entry.getNodeState();
        } catch (RepositoryException e) {
            // may occur if the nodeState is not accessible or some generic
            // error occurred.
            // for this case, assume that no lock exists and delegate final
            // validation to the spi-implementation.
            log.warn("Error while accessing lock holding NodeState: {}", e.getMessage());
            return null;
        }
    }

    private LockState buildLockState(NodeState nodeState) throws RepositoryException {
        NodeId nId = nodeState.getNodeId();
        NodeState lockHoldingState;
        LockInfo lockInfo = wspManager.getLockInfo(nId);
        if (lockInfo == null) {
            // no lock present
            return null;
        }

        NodeId lockNodeId = lockInfo.getNodeId();
        if (lockNodeId.equals(nId)) {
            lockHoldingState = nodeState;
        } else {
            NodeEntry lockedEntry = wspManager.getHierarchyManager().getNodeEntry(lockNodeId);
            try {
                lockHoldingState = lockedEntry.getNodeState();
            } catch (RepositoryException e) {
                log.warn("Cannot build LockState");
                throw new RepositoryException("Cannot build LockState", e);
            }
        }

        if (lockHoldingState == null) {
            return null;
        } else {
            return new LockState(lockHoldingState, lockInfo);
        }
    }

    /**
     * Returns the Lock that applies to the given node state (directly or
     * by an inherited deep lock) or <code>null</code> if the state is not
     * locked at all.
     *
     * @param nodeState
     * @param lazyLockDiscovery If true, no extra check with the server is made in order to
     * determine, whether there is really no lock present. Otherwise, the server
     * is asked if a lock is present.
     * @return LockImpl that applies to the given state or <code>null</code>.
     * @throws RepositoryException
     */
    private LockImpl getLockImpl(NodeState nodeState, boolean lazyLockDiscovery) throws RepositoryException {
        NodeState nState = nodeState;
        // access first non-NEW state
        while (nState.getStatus() == Status.NEW) {
            nState = nState.getParent();
        }

        // shortcut: check if a given state holds a lock, which has been
        // store in the lock map. see below (LockImpl) for the conditions that
        // must be met in order a lock can be stored.
        LockImpl l = getLockFromMap(nState);
        if (l != null && l.lockState.appliesToNodeState(nodeState)) {
            return l;
        }

        LockState lState;
        if (lazyLockDiscovery) {
            // try to retrieve a state (ev. a parent state) that holds a lock.
            NodeState lockHoldingState = getLockHoldingState(nState);
            if (lockHoldingState == null) {
                // assume no lock is present (might not be correct due to incomplete hierarchy)
                return null;
            } else {
                // check lockMap again with the lock-holding state
                l = getLockFromMap(nState);
                if (l != null) {
                    return l;
                }
                lState = buildLockState(lockHoldingState);
            }
        } else {
            // need precise information about lock status -> retrieve lockInfo
            // from the persistent layer.
            lState = buildLockState(nState);
        }

        if (lState != null) {
            // Test again if a Lock object is stored in the lockmap. Otherwise
            // build the lock object and retrieve lock holding node. note that this
            // may fail if the session does not have permission to see this node.
            LockImpl lock = getLockFromMap(lState.lockHoldingState);
            if (lock != null) {
                lock.lockState.setLockInfo(lState.lockInfo);
            } else {
                Item lockHoldingNode = itemManager.getItem(lState.lockHoldingState.getHierarchyEntry());
                lock = new LockImpl(lState, (Node)lockHoldingNode);
            }
            // test if lock applies to the original nodestate
            if (lState.appliesToNodeState(nodeState)) {
                return lock;
            } else {
                return null; // lock exists but doesn't apply to the given state
            }
        } else {
            // no lock at all
            return null;
        }
    }

    private LockImpl getLockFromMap(NodeState nodeState) {
        try {
            LockImpl l = lockMap.get(nodeState);
            if (l != null && l.isLive()) {
                return l;
            }
        } catch (RepositoryException e) {
            // ignore
        }
        return null;
    }

    //----------------------------< Notification about modified lock-tokens >---
    /**
     * Notify all <code>Lock</code>s that have been accessed so far about the
     * new lock token present on the session and allow them to reload their
     * lock info.
     *
     * @param lt
     * @throws RepositoryException
     */
    private void notifyTokenAdded(String lt) throws RepositoryException {
        LockTokenListener[] listeners = lockMap.values().toArray(new LockTokenListener[lockMap.size()]);
        for (LockTokenListener listener : listeners) {
            listener.lockTokenAdded(lt);
        }
    }

    /**
     * Notify all <code>Lock</code>s that have been accessed so far about the
     * removed lock token and allow them to reload their lock info, if necessary.
     *
     * @param lt
     * @throws RepositoryException
     */
    private void notifyTokenRemoved(String lt) throws RepositoryException {
        LockTokenListener[] listeners = lockMap.values().toArray(new LockTokenListener[lockMap.size()]);
        for (LockTokenListener listener : listeners) {
            listener.lockTokenRemoved(lt);
        }
    }

    //--------------------------------------------------------------------------
    private class LockState implements ItemStateLifeCycleListener {

        private final NodeState lockHoldingState;

        private LockInfo lockInfo;
        private boolean isLive = true;
        private long expiration = TIMEOUT_INFINITE;

        private LockState(NodeState lockHoldingState, LockInfo lockInfo) {
            this.lockHoldingState = lockHoldingState;
            setLockInfo(lockInfo);
        }

        private void refresh() throws RepositoryException {
            // lock is still alive -> send refresh-lock operation.
            Operation op = LockRefresh.create(lockHoldingState);
            wspManager.execute(op);
        }

        /**
         * Returns true, if the given node state is the lockholding state of
         * this Lock object OR if this Lock is deep.
         * Note, that in the latter case this method does not assert, that the
         * given node state is a child state of the lockholding state.
         *
         * @param nodeState that must be the same or a child of the lock holding
         * state stored within this lock object.
         * @return true if this lock applies to the given node state.
         */
        private boolean appliesToNodeState(NodeState nodeState) {
            if (nodeState.getStatus() == Status.NEW) {
                return lockInfo.isDeep();
            } else {
                if (lockHoldingState == nodeState) {
                    return true;
                } else {
                    return lockInfo != null && lockInfo.isDeep();
                }
            }
        }

        /**
         * Reload the lockInfo from the server.
         *
         * @throws RepositoryException
         */
        private void reloadLockInfo() throws RepositoryException {
            NodeId nId = lockHoldingState.getNodeEntry().getWorkspaceId();
            lockInfo = wspManager.getLockInfo(nId);
            if (lockInfo == null) {
                // lock has been released on the server
                unlocked();
            }
        }

        private void setLockInfo(LockInfo lockInfo) {
            this.lockInfo = lockInfo;
            long seconds = lockInfo.getSecondsRemaining();
            if (seconds <= TIMEOUT_EXPIRED) {
                expiration = TIMEOUT_EXPIRED;
                isLive = false;
            } else if (seconds < TIMEOUT_INFINITE) {
                // calculate timeout
                expiration = new Date().getTime()/1000 + lockInfo.getSecondsRemaining();
            } else {
                expiration = TIMEOUT_INFINITE;
            }
        }

        /**
         * @return <code>true</code> if the lock is still alive.
         */
        private boolean isLive() {
            if (isLive) {
                isLive = getSecondsRemaining() > 0;
            }
            return isLive;
        }

        /**
         * @return the number of seconds until the lock's timeout is reached,
         * {@link Long#MAX_VALUE} if timeout is infinite or undefined and
         * a negative value if timeout has already been reached or the lock
         * has been otherwise released.
         */
        private long getSecondsRemaining() {
            if (!isLive) {
                return TIMEOUT_EXPIRED;
            } else if (expiration == TIMEOUT_INFINITE) {
                return expiration;
            } else {
                long seconds = expiration - new Date().getTime()/1000;
                if (seconds <= 0) {
                    isLive = false;
                    return TIMEOUT_EXPIRED;
                } else {
                    return seconds;
                }
            }
        }

        /**
         * Release this lock by removing from the lock map and unregistering
         * it from event listening
         */
        private void release() {
            if (lockMap.containsKey(lockHoldingState)) {
                lockMap.remove(lockHoldingState);
            }
            stopListening();
        }

        /**
         * This lock has been removed by the current Session or by an external
         * unlock request. Since a lock will never come back to life after
         * unlocking, it is released an its status is reset accordingly.
         */
        private void unlocked() {
            if (isLive()) {
                release();
                isLive = false;
            }
        }

        private void startListening() {
            // LockState must be aware of removal of the Node.
            lockHoldingState.addListener(this);

            // in case of CacheBehaviour.OBSERVATION this lockstate can also
            // be aware of another session removing the lock -> listen to
            // status changes of the jcr:lockIsDeep property.
            if (cacheBehaviour == CacheBehaviour.OBSERVATION) {
                try {
                    if (!lockHoldingState.hasPropertyName(NameConstants.JCR_LOCKISDEEP)) {
                        // force reloading of the lock holding node.
                        itemManager.getItem(lockHoldingState.getNodeEntry());
                    }
                    PropertyState ps = lockHoldingState.getPropertyState(NameConstants.JCR_LOCKISDEEP);
                    ps.addListener(this);
                } catch (RepositoryException e) {
                    log.warn("Unable to retrieve jcr:isDeep property after lock creation. {}", e.getMessage());
                }
            }
        }

        private void stopListening() {
            lockHoldingState.removeListener(this);

            if (cacheBehaviour == CacheBehaviour.OBSERVATION) {
                try {
                    if (lockHoldingState.hasPropertyName(NameConstants.JCR_LOCKISDEEP)) {
                        PropertyState ps = lockHoldingState.getPropertyState(NameConstants.JCR_LOCKISDEEP);
                        ps.removeListener(this);
                    }
                } catch (ItemNotFoundException e) {
                    log.debug("jcr:lockIsDeep doesn't exist any more.");
                } catch (Exception e) {
                    log.warn(e.getMessage());
                }
            }
        }

        //-------------------------------------< ItemStateLifeCycleListener >---
        /**
         * @see ItemStateLifeCycleListener#statusChanged(ItemState, int)
         */
        public void statusChanged(ItemState state, int previousStatus) {
            if (!isLive()) {
                // since we only monitor the removal of the lock (by means
                // of deletion of the jcr:lockIsDeep property, we are not interested
                // if the lock is not active any more.
                return;
            }

            switch (state.getStatus()) {
                case Status.REMOVED:
                    // this lock has been release by someone else (and not by
                    // a call to LockManager#unlock -> clean up and set isLive
                    // flag to false.
                    unlocked();
                    break;
               default:
                   // not interested
            }
        }
    }

    //---------------------------------------------------------------< Lock >---
    /**
     * Inner class implementing the {@link Lock} interface.
     */
    private class LockImpl implements javax.jcr.lock.Lock, LockTokenListener {

        private final LockState lockState;
        private final Node node;
        private boolean reloadInfo = false; // TODO: find better solution

        /**
         *
         * @param lockState
         * Note, that the given state must not have an overlaid state.
         * @param lockHoldingNode the lock holding <code>Node</code> itself.
         */
        public LockImpl(LockState lockState, Node lockHoldingNode) {
            this.lockState = lockState;
            this.node = lockHoldingNode;

            // if observation is supported OR if this is a session-scoped lock
            // hold by this session -> store lock in the map
            if (cacheBehaviour == CacheBehaviour.OBSERVATION) {
                lockMap.put(lockState.lockHoldingState, this);
                lockState.startListening();
            } else if (lockState.lockInfo.isLockOwner()) {
                lockMap.put(lockState.lockHoldingState, this);
                lockState.startListening();
                // open-scoped locks: the map entry and the lock information
                // stored therein may become outdated if the token is transferred
                // to another session -> info must be reloaded.
                if (!isSessionScoped()) {
                    reloadInfo = true;
                }
            } else {
                // foreign lock: info must be reloaded.
                reloadInfo = true;
            }
        }

        /**
         * @see Lock#getLockOwner()
         */
        public String getLockOwner() {
            LockInfo info = getLockInfo();
            if (info != null) {
                return info.getOwner();
            } else {
                return null;
            }
        }

        /**
         * @see Lock#isDeep()
         */
        public boolean isDeep() {
            LockInfo info = getLockInfo();
            return info != null && info.isDeep();
        }

        /**
         * @see Lock#getNode()
         */
        public Node getNode() {
            return node;
        }

        /**
         * @see Lock#getLockToken()
         */
        public String getLockToken() {
            // shortcut for jsr 283 session scoped locks: they never expose
            // the lock token to the API users.
            if (isSessionScoped()) {
                return null;
            }

            updateLockInfo();
            LockInfo info = getLockInfo();
            if (info != null) {
                return info.getLockToken();
            } else {
                return null;
            }
        }

        /**
         * @see Lock#isLive()
         */
        public boolean isLive() throws RepositoryException {
            updateLockInfo();
            return lockState.isLive();
        }

        /**
         * @see Lock#isSessionScoped()
         */
        public boolean isSessionScoped() {
            LockInfo info = getLockInfo();
            return info != null && info.isSessionScoped();
        }

        /**
         * @see Lock#refresh()
         */
        public void refresh() throws LockException, RepositoryException {
            if (!isLive()) {
                throw new LockException("Lock is not alive any more.");
            }

            if (!isLockOwningSession()) {
                // shortcut, since lock is always updated if the session became
                // lock-holder of a foreign lock.
                throw new LockException("Session does not hold lock.");
            } else {
                lockState.refresh();
            }
        }

        /**
         * @see javax.jcr.lock.Lock#getSecondsRemaining()
         */
        public long getSecondsRemaining() throws RepositoryException {
            updateLockInfo();
            return lockState.getSecondsRemaining();
        }

        /**
         * @see javax.jcr.lock.Lock#isLockOwningSession()
         */
        public boolean isLockOwningSession(){
            LockInfo info = getLockInfo();
            return info != null && info.isLockOwner();
        }

        //----------------------------------------------< LockTokenListener >---
        /**
         * A lock token as been added to the current Session. If this Lock
         * object is not yet hold by the Session (thus does not know whether
         * the new lock token belongs to it), it must reload the LockInfo
         * from the server.
         *
         * @param lockToken
         * @throws RepositoryException
         * @see LockTokenListener#lockTokenAdded(String)
         */
        public void lockTokenAdded(String lockToken) throws RepositoryException {
            if (!isSessionScoped() && !isLockOwningSession()) {
                // unless this lock is session-scoped (token is never transferred)
                // and the session isn't the owner yet (token already present),
                // it could be that this affects this lock and session became
                // lock holder -> reload info to assert.
                lockState.reloadLockInfo();
            }
        }

        /**
         *
         * @param lockToken
         * @throws LockException
         * @see LockTokenListener#lockTokenRemoved(String)
         */
        public void lockTokenRemoved(String lockToken) throws RepositoryException {
            // reload lock info, if session gave away its lock-holder status
            // for this lock. this will never be true for session-scoped locks
            // that are not exposed (thus cannot be removed).
            if (lockToken.equals(getLockToken())) {
                lockState.reloadLockInfo();
            }
        }

        //--------------------------------------------------------< private >---
        /**
         * @return <code>LockInfo</code> stored within the <code>LockState</code>
         */
        private LockInfo getLockInfo() {
            return lockState.lockInfo;
        }

        /**
         * Make sure the lock info is really up to date.
         * TODO: find better solution.
         */
        private void updateLockInfo() {
            if (reloadInfo) {
                try {
                    lockState.reloadLockInfo();
                } catch (RepositoryException e) {
                    // may occur if session has been logged out. rather throw?
                    log.warn("Unable to determine lock status. {}", e.getMessage());
                }
            } // else: nothing to do.
        }
    }

    //--------------------------------------------------< LockTokenListener >---
    /**
     *
     */
    private interface LockTokenListener {

        /**
         *
         * @param lockToken
         * @throws LockException
         * @throws RepositoryException
         */
        void lockTokenAdded(String lockToken) throws RepositoryException;

        /**
         *
         * @param lockToken
         * @throws LockException
         * @throws RepositoryException
         */
        void lockTokenRemoved(String lockToken) throws RepositoryException;
    }
}
