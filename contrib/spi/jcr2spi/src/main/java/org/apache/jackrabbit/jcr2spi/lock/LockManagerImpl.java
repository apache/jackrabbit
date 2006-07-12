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
import org.apache.jackrabbit.jcr2spi.IdKeyMap;
import org.apache.jackrabbit.jcr2spi.DefaultIdKeyMap;
import org.apache.jackrabbit.jcr2spi.observation.InternalEventListener;
import org.apache.jackrabbit.jcr2spi.operation.Operation;
import org.apache.jackrabbit.jcr2spi.operation.LockOperation;
import org.apache.jackrabbit.jcr2spi.operation.LockRelease;
import org.apache.jackrabbit.jcr2spi.operation.LockRefresh;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.ItemStateException;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.EventIterator;
import org.apache.jackrabbit.spi.Event;
import org.apache.jackrabbit.spi.LockInfo;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.ItemId;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.Item;
import javax.jcr.Session;

import java.util.Iterator;

/**
 * <code>LockManagerImpl</code>...
 */
public class LockManagerImpl implements LockManager, SessionListener {

    private static Logger log = LoggerFactory.getLogger(LockManagerImpl.class);

    private final WorkspaceManager wspManager;
    private final ItemManager itemManager;

    /**
     * Internal map holding all locks that where created by {@link #lock(NodeId, boolean, boolean)}
     * or accessed by {@link #getLock(NodeId)} until they end their life by
     * an unlock (be it by the current Session or external reported by means
     * of events).
     */
    // TODO: TO-BE-FIXED. With SPI_ItemId simple map cannot be used any more
    private final IdKeyMap lockMap = new DefaultIdKeyMap();

    public LockManagerImpl(WorkspaceManager wspManager, ItemManager itemManager) {
        this.wspManager = wspManager;
        this.itemManager = itemManager;
    }

    /**
     * @see LockManager#lock(NodeId, boolean, boolean)
     */
    public Lock lock(NodeId nodeId, boolean isDeep, boolean isSessionScoped) throws LockException, RepositoryException {
        // make sure the node is accessible before trying to create a lock.
        Node node = (Node) itemManager.getItem(nodeId);
        // execute the operation
        Operation op = LockOperation.create(nodeId, isDeep, isSessionScoped);
        wspManager.execute(op);

        Lock lock = new LockImpl(nodeId, node, isSessionScoped);
        return lock;
    }

    /**
     * If the session created a lock on the node with the given id, we already
     * know the lock. Otherwise, we look in the node state and the states of
     * the ancestor nodes for properties indicating a lock.<br>
     * Note, that the flag indicating session-scoped lock cannot be retrieved
     * and the lock will always report 'false'.
     *
     * @see LockManager#getLock(NodeId)
     */
    public Lock getLock(NodeId nodeId) throws LockException, RepositoryException {
        // shortcut: check if node holds a lock and lock has been accessed before
        if (lockMap.containsKey(nodeId)) {
            return (Lock) lockMap.get(nodeId);
        }

        // try to retrieve parent state that holds a lock.
        NodeState lockHoldingState = getLockHoldingState(nodeId);
        if (lockHoldingState == null) {
            throw new LockException("Node with id '" + nodeId + "' is not locked.");
        } else {
            NodeId lhNodeId = lockHoldingState.getNodeId();
            // check again lockMap with id of lockholding node
            if (lockMap.containsKey(lhNodeId)) {
                return (Lock) lockMap.get(lhNodeId);
            }

            // retrieve lock holding node. not that this may fail if the session
            // does not have permission to see this node.
            Item lockHoldingNode = itemManager.getItem(lhNodeId);
            // TODO: we don;t know if lock is session scoped -> set flag to false
            // TODO: ev. add 'isSessionScoped' to RepositoryService lock-call.
            Lock l = new LockImpl(lhNodeId, (Node)lockHoldingNode, false);
            return l;
        }
    }

    /**
     * @see LockManager#unlock(NodeId)
     */
    public void unlock(NodeId nodeId) throws LockException, RepositoryException {
        // execute the operation. Note, that its possible that the session is
        // lock holder and still the lock was never accessed. thus the lockMap
        // does not provide sufficient information.
        Operation op = LockRelease.create(nodeId);
        wspManager.execute(op);

        // if unlock was successfull: clean up lock map and lock life cycle
        // in case the corresponding Lock object exists (and thus has been
        // added to the map.
        if (lockMap.containsKey(nodeId)) {
            LockImpl l = (LockImpl) lockMap.remove(nodeId);
            l.unlocked();
        }
    }

    /**
     * @see LockManager#isLocked(NodeId)
     */
    public boolean isLocked(NodeId nodeId) throws RepositoryException {
        // shortcut: check if a given node holds a lock and lock has been
        // accessed before (thus is known to the manager).
        if (lockMap.containsKey(nodeId)) {
            return true;
        } else {
            // check if any lock is present (checking lock-specific properties)
            LockInfo lInfo = getLockInfo(nodeId);
            return lInfo != null;
        }
    }

    /**
     * @see LockManager#checkLock(NodeId)
     */
    public void checkLock(NodeId nodeId) throws LockException, RepositoryException {
        LockInfo lInfo;
        // shortcut: check if a given node holds a lock and lock has been
        // accessed before (thus is known to the manager).
        if (lockMap.containsKey(nodeId)) {
            lInfo = ((LockImpl)lockMap.get(nodeId)).lockInfo;
        } else {
            // check if any lock is present (checking lock-specific properties)
            lInfo = getLockInfo(nodeId);
        }

        if (lInfo != null && lInfo.getLockToken() == null) {
            // lock is present and token is null -> session is not lock-holder.
            throw new LockException("Node with id '" + nodeId + "' is locked.");
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
        NodeId key = null;
        Iterator it = lockMap.values().iterator();
        while (it.hasNext() && key == null) {
            LockImpl l = (LockImpl) it.next();
            // break as soon as the lock associated with the given token is found.
            if (lt.equals(l.getLockToken())) {
                if (l.isSessionScoped()) {
                    throw new LockException("Cannot remove lock token associated with a session scoped lock.");
                }
                key = l.nodeId;
            }
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
        ItemId[] ids = (ItemId[]) lockMap.keySet().toArray(new ItemId[lockMap.size()]);
        for (int i = 0; i < ids.length; i++) {
            NodeId nId = (NodeId) ids[i];
            LockImpl l = (LockImpl) lockMap.get(nId);
            if (l.isSessionScoped()) {
                try {
                    unlock(nId);
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
     * @param nodeId
     * @return
     * @throws LockException
     * @throws RepositoryException
     */
    private LockInfo getLockInfo(NodeId nodeId) throws RepositoryException {
        try {
            return wspManager.getLockInfo(nodeId);
        } catch (LockException e) {
            log.debug("No lock present on node with id '" + nodeId + "'", e);
            return null;
        }
    }

    /**
     *
     * @param nodeId
     * @return
     * @throws RepositoryException
     */
    private NodeState getLockHoldingState(NodeId nodeId) throws RepositoryException {
        try {
            NodeState nodeState = (NodeState) wspManager.getItemState(nodeId);
            // search nearest ancestor that is locked
            /**
             * FIXME should not only rely on existence of jcr:lockOwner property
             * but also verify that node.isNodeType("mix:lockable")==true;
             * this would have a negative impact on performance though...
             */
            while (!nodeState.hasPropertyName(QName.JCR_LOCKOWNER)) {
                if (nodeState.getParentId() == null) {
                    // reached root state without finding a locked node
                    return null;
                }
                nodeState = (NodeState) wspManager.getItemState(nodeState.getParentId());
            }
            return nodeState;
        } catch (ItemStateException e) {
            // should not occur
            throw new RepositoryException(e);
        }
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

        private final NodeId nodeId;
        private final Node node;
        private final boolean isSessionScoped;

        private LockInfo lockInfo;
        private boolean isLive = true;

        /**
         *
         * @param lockHoldingId The Id of the lock holding <code>Node</code>.
         * @param lockHoldingNode the lock holding <code>Node</code> itself.
         * @param lockHoldingNode
         */
        public LockImpl(NodeId lockHoldingId, Node lockHoldingNode, boolean isSessionScoped) throws LockException, RepositoryException {
            this.nodeId = lockHoldingId;
            this.node = lockHoldingNode;
            this.isSessionScoped = isSessionScoped;

            // retrieve lock info from wsp-manager, in order to get the complete
            // lockInfo including lock-token, which is not available from the
            // child properties nor from the original lock request.
            this.lockInfo = wspManager.getLockInfo(nodeId);

            // register as internal listener to the wsp manager in order to get
            // informed if this lock ends his life.
            wspManager.addEventListener(this);
            // store lock in the map
            lockMap.put(nodeId, this);
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
                Operation op = LockRefresh.create(nodeId);
                wspManager.execute(op);
            }
        }

        //----------------------------------------------< InternalEventListener >---
        /**
         *
         * @param events
         * @param isLocal
         */
        public void onEvent(EventIterator events, boolean isLocal) {
            if (!isLive) {
                // since only we only monitor the removal of the lock (by means
                // of deletion of the jcr:lockOwner property, we are not interested
                // if the lock is not active any more.
                return;
            }

            while (events.hasNext()) {
                Event ev = events.nextEvent();
                // if the jcr:lockOwner property related to this Lock got removed,
                // we assume that the lock has been released.
                if (ev.getType() == Event.PROPERTY_REMOVED &&
                    nodeId.equals(ev.getParentId()) &&
                    QName.JCR_LOCKOWNER.equals(ev.getQPath().getNameElement().getName())) {
                    // TODO: check if removal jcr:lockIsDeep must be checked as well.

                    // this lock has been release by someone else (and not by
                    // a call to LockManager#unlock -> clean up and set isLive
                    // flag to false.
                    unlocked();
                    break;
                }
            }
        }

        /**
         *
         * @param lockToken
         * @throws LockException
         * @throws RepositoryException
         */
        public void lockTokenAdded(String lockToken) throws LockException, RepositoryException {
            if (getLockToken() == null) {
                // could be that this affects this lock and session became
                // lock holder -> releoad info
                reloadLockInfo();
            }
        }

        /**
         *
         * @param lockToken
         * @throws LockException
         * @throws RepositoryException
         */
        public void lockTokenRemoved(String lockToken) throws LockException, RepositoryException {
            // reload lock info, if session gave away its lock-holder state
            // for this lock
            if (lockToken.equals(getLockToken())) {
                reloadLockInfo();
            }
        }

        //--------------------------------------------------------< private >---
        /**
         *
         * @throws LockException
         * @throws RepositoryException
         */
        private void reloadLockInfo() throws LockException, RepositoryException {
            // re-check with server, since Session.addLockToken will not
            // automatically result in an update of the lock-map.
            lockInfo = wspManager.getLockInfo(nodeId);
        }

        /**
         * Release this lock by removing from the lock map and unregistering
         * it from event listening
         */
        private void release() {
            if (lockMap.containsKey(nodeId)) {
                lockMap.remove(nodeId);
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