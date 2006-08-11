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
import org.apache.jackrabbit.jcr2spi.observation.InternalEventListener;
import org.apache.jackrabbit.jcr2spi.operation.Operation;
import org.apache.jackrabbit.jcr2spi.operation.LockOperation;
import org.apache.jackrabbit.jcr2spi.operation.LockRelease;
import org.apache.jackrabbit.jcr2spi.operation.LockRefresh;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.EventIterator;
import org.apache.jackrabbit.spi.Event;
import org.apache.jackrabbit.spi.LockInfo;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.Item;
import javax.jcr.Session;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

/**
 * <code>LockManagerImpl</code>...
 */
public class LockManagerImpl implements LockManager, SessionListener {

    private static Logger log = LoggerFactory.getLogger(LockManagerImpl.class);

    private final WorkspaceManager wspManager;
    private final ItemManager itemManager;

    /**
     * Map holding all locks that where created by this <code>Session</code> upon
     * calls to {@link LockManager#lock(NodeState,boolean,boolean)} or to
     * {@link LockManager#getLock(NodeState)}. The map entries are removed
     * only if a lock ends his life by {@link Node#unlock()} or by implicit
     * unlock upon {@link Session#logout()}.
     */
    private final Map lockMap;

    public LockManagerImpl(WorkspaceManager wspManager, ItemManager itemManager) {
        this.wspManager = wspManager;
        this.itemManager = itemManager;
        // use hard references in order to make sure, that entries refering
        // to locks created by the current session are not removed.
        lockMap = new HashMap();
    }

    /**
     * @see LockManager#lock(NodeState,boolean,boolean)
     */
    public Lock lock(NodeState nodeState, boolean isDeep, boolean isSessionScoped) throws LockException, RepositoryException {
        // retrieve node first
        Node lhNode;
        Item item = itemManager.getItem(nodeState);
        if (item.isNode()) {
            lhNode = (Node) item;
        } else {
            throw new RepositoryException("Internal error: ItemManager returned Property from NodeState");
        }

        // execute the operation
        Operation op = LockOperation.create(nodeState.getNodeId(), isDeep, isSessionScoped);
        wspManager.execute(op);

        Lock lock = new LockImpl(nodeState, lhNode, isSessionScoped);
        return lock;
    }

    /**
     * If the session created a lock on the node with the given id, we already
     * know the lock. Otherwise, we look in the node state and the states of
     * the ancestor nodes for properties indicating a lock.<br>
     * Note, that the flag indicating session-scoped lock cannot be retrieved
     * and the lock will always report 'false'.
     *
     * @see LockManager#getLock(NodeState)
     * @param nodeState
     */
    public Lock getLock(NodeState nodeState) throws LockException, RepositoryException {
        // shortcut: check if lock has been accessed before
        if (lockMap.containsKey(nodeState)) {
            return (Lock) lockMap.get(nodeState);
        }

        // try to retrieve parent state that holds a lock.
        NodeState lockHoldingState = getLockHoldingState(nodeState);
        if (lockHoldingState == null) {
            throw new LockException("Node with id '" + nodeState.getNodeId() + "' is not locked.");
        } else {
            // check lockMap again with the lockholding state
            if (lockMap.containsKey(lockHoldingState)) {
                return (Lock) lockMap.get(lockHoldingState);
            }

            // retrieve lock holding node. note that this may fail if the session
            // does not have permission to see this node.
            Item lockHoldingNode = itemManager.getItem(lockHoldingState);
            // TODO: we don;t know if lock is session scoped -> set flag to false
            // TODO: ev. add 'isSessionScoped' to RepositoryService lock-call.
            Lock l = new LockImpl(lockHoldingState, (Node)lockHoldingNode, false);
            return l;
        }
    }

    /**
     * @see LockManager#unlock(NodeState)
     * @param nodeState
     */
    public void unlock(NodeState nodeState) throws LockException, RepositoryException {
        // execute the operation. Note, that its possible that the session is
        // lock holder and still the lock was never accessed. thus the lockMap
        // does not provide sufficient and relyable information.
        Operation op = LockRelease.create(nodeState.getNodeId());
        wspManager.execute(op);

        // if unlock was successfull: clean up lock map and lock life cycle
        // in case the corresponding Lock object exists (and thus has been
        // added to the map.
        if (lockMap.containsKey(nodeState)) {
            LockImpl l = (LockImpl) lockMap.remove(nodeState);
            l.unlocked();
        }
    }

    /**
     * @see LockManager#isLocked(NodeState)
     * @param nodeState
     */
    public boolean isLocked(NodeState nodeState) throws RepositoryException {
        // shortcut: check if a given node holds a lock and lock has been
        // accessed before (thus is known to the manager).
        if (lockMap.containsKey(nodeState)) {
            return true;
        } else {
            // check if any lock is present (checking lock-specific properties)
            LockInfo lInfo = getLockInfo(nodeState);
            return lInfo != null;
        }
    }

    /**
     * @see LockManager#checkLock(NodeState)
     * @param nodeState
     */
    public void checkLock(NodeState nodeState) throws LockException, RepositoryException {
        LockInfo lInfo;
        // shortcut: check if a given node holds a lock and lock has been
        // accessed before (thus is known to the manager).
        if (lockMap.containsKey(nodeState)) {
            lInfo = ((LockImpl)lockMap.get(nodeState)).lockInfo;
        } else {
            // check if any lock is present (checking lock-specific properties)
            lInfo = getLockInfo(nodeState);
        }

        if (lInfo != null && lInfo.getLockToken() == null) {
            // lock is present and token is null -> session is not lock-holder.
            throw new LockException("Node with id '" + nodeState + "' is locked.");
        }
    }

    /**
     * Returns the lock tokens present on the <code>SessionInfo</code> this
     * manager has been created with.
     *
     * @see LockManager#getLockTokens()
     */
    public String[] getLockTokens() {
        return wspManager.getLockTokens();
    }

    /**
     * Delegates this call to {@link WorkspaceManager#addLockToken(String)}.
     * If this succeeds this method will inform all locks stored in the local
     * map in order to give them the chance to update their lock information.
     *
     * @see LockManager#addLockToken(String)
     */
    public void addLockToken(String lt) throws LockException, RepositoryException {
        wspManager.addLockToken(lt);
        notifyTokenAdded(lt);
    }

    /**
     * If the lock addressed by the token is session-scoped, this method will
     * throw a LockException, such as defined by JSR170 v.1.0.1 for
     * {@link Session#removeLockToken(String)}.<br>Otherwise the call is
     * delegated to {@link WorkspaceManager#removeLockToken(String)} and
     * all locks stored in the local lock map are notified by the removed
     * token in order to give them the chance to update their lock information.
     *
     * @see LockManager#removeLockToken(String)
     */
    public void removeLockToken(String lt) throws LockException, RepositoryException {
        // JSR170 v. 1.0.1 defines that the token of a session-scoped lock may
        // not be moved over to another session. thus removal ist not possible
        // and the lock is always present in the lock map.
        Iterator it = lockMap.values().iterator();
        boolean found = false;
        // loop over cached locks to determine if the token belongs to a session
        // scoped lock, in which case the removal must fail immediately.
        while (it.hasNext() && !found) {
            LockImpl l = (LockImpl) it.next();
            if (lt.equals(l.getLockToken())) {
                // break as soon as the lock associated with the given token was found.
                found = true;
                if (l.isSessionScoped()) {
                    throw new LockException("Cannot remove lock token associated with a session scoped lock.");
                }
            }
        }

        if (!found) {
            String msg = "Unable to remove lock token: lock is held by another session.";
            log.warn(msg);
            throw new RepositoryException(msg);
        }

        // remove lock token from sessionInfo
        wspManager.removeLockToken(lt);
        // inform about this lt being removed from this session
        notifyTokenRemoved(lt);
    }

    //----------------------------------------------------< SessionListener >---
    /**
     *
     * @param session
     * @see SessionListener#loggingOut(Session)
     */
    public void loggingOut(Session session) {
        // remove any session scoped locks:
        NodeState[] lhStates = (NodeState[]) lockMap.keySet().toArray(new NodeState[lockMap.size()]);
        for (int i = 0; i < lhStates.length; i++) {
            NodeState nState = (NodeState) lhStates[i];
            LockImpl l = (LockImpl) lockMap.get(nState);
            if (l.isSessionScoped()) {
                try {
                    unlock(nState);
                } catch (RepositoryException e) {
                    log.error("Error while unlocking session scoped lock. Cleaning up local lock status.");
                    // at least clean up local lock map and the locks life cycle
                    l.unlocked();
                }
            }
        }
    }

    /**
     *
     * @param session
     * @see SessionListener#loggedOut(Session)
     */
    public void loggedOut(Session session) {
        // release all remaining locks without modifying their lock status
        LockImpl[] locks = (LockImpl[]) lockMap.values().toArray(new LockImpl[lockMap.size()]);
        for (int i = 0; i < locks.length; i++) {
            locks[i].release();
        }
    }

    //------------------------------------------------------------< private >---
    /**
     *
     * @param nodeState
     * @return
     * @throws LockException
     * @throws RepositoryException
     */
    private LockInfo getLockInfo(NodeState nodeState) throws RepositoryException {
        try {
            return wspManager.getLockInfo(nodeState.getNodeId());
        } catch (LockException e) {
            log.debug("No lock present on node with id '" + nodeState.getNodeId() + "'", e);
            return null;
        }
    }

    /**
     * Search nearest ancestor that is locked. Returns <code>null</code> if no
     * lock ancestor could be found.
     *
     * @param nodeState <code>NodeState</code> from which searching starts.
     * @return lock ancestor node or <code>null</code>.
     */
    private NodeState getLockHoldingState(NodeState nodeState) {
        /**
         * FIXME should not only rely on existence of jcr:lockIsDeep property
         * but also verify that node.isNodeType("mix:lockable")==true;
         * this would have a negative impact on performance though...
         */
        while (!nodeState.hasPropertyName(QName.JCR_LOCKISDEEP)) {
            NodeState parentState = nodeState.getParent();
            if (parentState == null) {
                // reached root state without finding a locked node
                return null;
            }
            nodeState = parentState;
        }
        return nodeState;
    }

    //----------------------------< Notification about modified lock-tokens >---
    /**
     * Notify all <code>Lock</code>s that have been accessed so far about the
     * new lock token present on the session and allow them to reload their
     * lock info.
     *
     * @param lt
     * @throws LockException
     * @throws RepositoryException
     */
    private void notifyTokenAdded(String lt) throws LockException, RepositoryException {
        LockTokenListener[] listeners = (LockTokenListener[]) lockMap.values().toArray(new LockTokenListener[lockMap.size()]);
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].lockTokenAdded(lt);
        }
    }

    /**
     * Notify all <code>Lock</code>s that have been accessed so far about the
     * removed lock token and allow them to reload their lock info, if necessary.
     *
     * @param lt
     * @throws LockException
     * @throws RepositoryException
     */
    private void notifyTokenRemoved(String lt) throws LockException, RepositoryException {
        LockTokenListener[] listeners = (LockTokenListener[]) lockMap.values().toArray(new LockTokenListener[lockMap.size()]);
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].lockTokenRemoved(lt);
        }
    }

    //---------------------------------------------------------------< Lock >---
    /**
     * Inner class implementing the {@link Lock} interface.
     */
    private class LockImpl implements Lock, InternalEventListener, LockTokenListener {

        private final NodeState lockHoldingState;
        private final Node node;
        private final boolean isSessionScoped;

        private LockInfo lockInfo;
        private boolean isLive = true;

        /**
         *
         * @param lockHoldingState The NodeState of the lock holding <code>Node</code>.
         * @param lockHoldingNode the lock holding <code>Node</code> itself.
         * @param lockHoldingNode
         */
        public LockImpl(NodeState lockHoldingState, Node lockHoldingNode, boolean isSessionScoped) throws LockException, RepositoryException {
            this.lockHoldingState = lockHoldingState;
            this.node = lockHoldingNode;
            this.isSessionScoped = isSessionScoped;

            // retrieve lock info from wsp-manager, in order to get the complete
            // lockInfo including lock-token, which is not available from the
            // child properties nor from the original lock request.
            this.lockInfo = wspManager.getLockInfo(lockHoldingState.getNodeId());

            // register as internal listener to the wsp manager in order to get
            // informed if this lock ends his life.
            wspManager.addEventListener(this);
            // store lock in the map
            lockMap.put(lockHoldingState, this);
        }

        /**
         * @see Lock#getLockOwner()
         */
        public String getLockOwner() {
            return lockInfo.getOwner();
        }

        /**
         * @see Lock#isDeep()
         */
        public boolean isDeep() {
            return lockInfo.isDeep();
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
            return lockInfo.getLockToken();
        }

        /**
         * @see Lock#isLive()
         */
        public boolean isLive() throws RepositoryException {
            return isLive;
        }

        /**
         * @see Lock#isSessionScoped()
         */
        public boolean isSessionScoped() {
            return isSessionScoped;
        }

        /**
         * @see Lock#refresh()
         */
        public void refresh() throws LockException, RepositoryException {
            if (!isLive()) {
                throw new LockException("Lock is not alive any more.");
            }

            if (getLockToken() == null) {
                // shortcut, since lock is always updated if the session became
                // lock-holder of a foreign lock.
                throw new LockException("Session does not hold lock.");
            } else {
                // lock is still alive -> send refresh-lock operation.
                Operation op = LockRefresh.create(lockHoldingState.getNodeId());
                wspManager.execute(op);
            }
        }

        //------------------------------------------< InternalEventListener >---
        /**
         *
         * @param events
         * @param isLocal
         */
        public void onEvent(EventIterator events, boolean isLocal) {
            if (!isLive) {
                // since we only monitor the removal of the lock (by means
                // of deletion of the jcr:lockIsDeep property, we are not interested
                // if the lock is not active any more.
                return;
            }

            while (events.hasNext()) {
                Event ev = events.nextEvent();
                // if the jcr:lockIsDeep property related to this Lock got removed,
                // we assume that the lock has been released.
                // TODO: not correct to compare nodeIds
                if (ev.getType() == Event.PROPERTY_REMOVED
                    && QName.JCR_LOCKISDEEP.equals(ev.getQPath().getNameElement().getName())
                    && lockHoldingState.getNodeId().equals(ev.getParentId())) {

                    // this lock has been release by someone else (and not by
                    // a call to LockManager#unlock -> clean up and set isLive
                    // flag to false.
                    unlocked();
                    break;
                }
                // TODO: JSR 283 defines that Lock-Owner is reset upon moved lt
            }
        }

        //----------------------------------------------< LockTokenListener >---
        /**
         * A lock token as been added to the current Session. If this Lock
         * object is not yet hold by the Session (thus does not know whether
         * the new lock token belongs to it), it must reload the LockInfo
         * from the server.
         *
         * @param lockToken
         * @throws LockException
         * @throws RepositoryException
         * @see LockTokenListener#lockTokenAdded(String)
         */
        public void lockTokenAdded(String lockToken) throws LockException, RepositoryException {
            if (getLockToken() == null) {
                // could be that this affects this lock and session became
                // lock holder -> releoad info to assert.
                reloadLockInfo();
            }
        }

        /**
         *
         * @param lockToken
         * @throws LockException
         * @throws RepositoryException
         * @see LockTokenListener#lockTokenRemoved(String)
         */
        public void lockTokenRemoved(String lockToken) throws LockException, RepositoryException {
            // reload lock info, if session gave away its lock-holder status
            // for this lock.
            if (lockToken.equals(getLockToken())) {
                reloadLockInfo();
            }
        }

        //--------------------------------------------------------< private >---
        /**
         * Reload the lockInfo from the server.
         *
         * @throws LockException
         * @throws RepositoryException
         */
        private void reloadLockInfo() throws LockException, RepositoryException {
            lockInfo = wspManager.getLockInfo(lockHoldingState.getNodeId());
        }

        /**
         * Release this lock by removing from the lock map and unregistering
         * it from event listening
         */
        private void release() {
            if (lockMap.containsKey(lockHoldingState)) {
                lockMap.remove(lockHoldingState);
            }
            wspManager.removeEventListener(this);
        }

        /**
         * This lock has been removed by the current Session or by an external
         * unlock request. Since a lock will never come back to life after
         * unlocking, it is released an its status is reset accordingly.
         */
        private void unlocked() {
            if (isLive) {
                isLive = false;
                release();
            }
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
        void lockTokenAdded(String lockToken) throws LockException, RepositoryException;

        /**
         *
         * @param lockToken
         * @throws LockException
         * @throws RepositoryException
         */
        void lockTokenRemoved(String lockToken) throws LockException, RepositoryException;
    }
}