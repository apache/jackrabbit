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
package org.apache.jackrabbit.core.version;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.observation.EventStateCollection;
import org.apache.jackrabbit.core.observation.EventStateCollectionFactory;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateCacheFactory;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateListener;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.XAItemStateManager;
import org.apache.jackrabbit.core.virtual.VirtualItemStateProvider;
import org.apache.jackrabbit.core.virtual.VirtualNodeState;
import org.apache.jackrabbit.core.virtual.VirtualPropertyState;
import org.apache.jackrabbit.data.core.InternalXAResource;
import org.apache.jackrabbit.data.core.TransactionContext;
import org.apache.jackrabbit.data.core.TransactionException;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

/**
 * Implementation of a {@link InternalVersionManager} that works in an XA environment.
 * Works as a filter between a version manager client and the global version
 * manager.
 */
public class InternalXAVersionManager extends InternalVersionManagerBase
        implements EventStateCollectionFactory, VirtualItemStateProvider, InternalXAResource {

    /**
     * Attribute name for associated change log.
     */
    private static final String CHANGE_LOG_ATTRIBUTE_NAME = "XAVersionManager.ChangeLog";

    /**
     * Attribute name for items.
     */
    private static final String ITEMS_ATTRIBUTE_NAME = "VersionItems";

    /**
     * Repository version manager.
     */
    private final InternalVersionManagerImpl vMgr;

    /**
     * The session that uses this version manager.
     */
    private SessionImpl session;

    /**
     * Items that have been modified and are part of the XA environment.
     */
    private Map<NodeId, InternalVersionItem> xaItems;

    /**
     * flag that indicates if the version manager was locked during prepare
     */
    private boolean vmgrLocked = false;

    /**
     * The global write lock on the version manager.
     */
    private VersioningLock.WriteLock vmgrLock;

    /**
     * Persistent root node of the version histories.
     */
    private final NodeStateEx historyRoot;

    /**
     * Persistent root node of the activities.
     */
    private final NodeStateEx activitiesRoot;

    /**
     * Creates a new instance of this class.
     *
     * @param vMgr the underlying version manager
     * @param ntReg node type registry
     * @param session the session
     * @param cacheFactory cache factory
     * @throws RepositoryException if a an error occurs
     */
    public InternalXAVersionManager(InternalVersionManagerImpl vMgr, NodeTypeRegistry ntReg,
                            SessionImpl session, ItemStateCacheFactory cacheFactory)
            throws RepositoryException {
        super(ntReg, vMgr.historiesId, vMgr.activitiesId, vMgr.getNodeIdFactory());
        this.vMgr = vMgr;
        this.session = session;
        this.stateMgr = XAItemStateManager.createInstance(vMgr.getSharedStateMgr(),
                this, CHANGE_LOG_ATTRIBUTE_NAME, cacheFactory);

        NodeState state;
        try {
            state = (NodeState) stateMgr.getItemState(historiesId);
        } catch (ItemStateException e) {
            throw new RepositoryException("Unable to retrieve history root", e);
        }
        this.historyRoot = new NodeStateEx(stateMgr, ntReg, state, NameConstants.JCR_VERSIONSTORAGE);
        try {
            state = (NodeState) stateMgr.getItemState(activitiesId);
        } catch (ItemStateException e) {
            throw new RepositoryException("Unable to retrieve activities root", e);
        }
        this.activitiesRoot = new NodeStateEx(stateMgr, ntReg, state, NameConstants.JCR_ACTIVITIES);
    }

    //------------------------------------------< EventStateCollectionFactory >

    /**
     * {@inheritDoc}
     */
    public EventStateCollection createEventStateCollection()
            throws RepositoryException {
        return vMgr.getEscFactory().createEventStateCollection(session);
    }

    //-------------------------------------------------------< InternalVersionManager >

    /**
     * {@inheritDoc}
     */
    public VirtualItemStateProvider getVirtualItemStateProvider() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected VersionHistoryInfo createVersionHistory(Session session,
                                                      NodeState node,
                                                      NodeId copiedFrom)
            throws RepositoryException {

        if (isInXA()) {
            NodeStateEx state = internalCreateVersionHistory(node, copiedFrom);
            InternalVersionHistory history =
                new InternalVersionHistoryImpl(vMgr, state);
            xaItems.put(state.getNodeId(), history);
            Name root = NameConstants.JCR_ROOTVERSION;
            return new VersionHistoryInfo(
                    state.getNodeId(),
                    state.getState().getChildNodeEntry(root, 1).getId());
        }
        return vMgr.createVersionHistory(session, node, copiedFrom);
    }

    /**
     * {@inheritDoc}
     */
    public NodeId createActivity(Session session, String title)
            throws RepositoryException {
        if (isInXA()) {
            NodeStateEx state = internalCreateActivity(title);
            InternalActivityImpl activity =
                new InternalActivityImpl(vMgr, state);
            xaItems.put(state.getNodeId(), activity);
            return state.getNodeId();
        } else {
            return vMgr.createActivity(session, title);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeActivity(Session session, NodeId nodeId)
            throws RepositoryException {

        if (isInXA()) {
            InternalActivityImpl act = (InternalActivityImpl) getItem(nodeId);
            internalRemoveActivity(act);
        }
        vMgr.removeActivity(session, nodeId);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Before modifying activity, make a local copy of it.
     */
    @Override
    protected void internalRemoveActivity(InternalActivityImpl activity)
            throws VersionException, RepositoryException {
        if (activity.getVersionManager() != this) {
            activity = makeLocalCopy(activity);
            xaItems.put(activity.getId(), activity);
        }
        super.internalRemoveActivity(activity);
    }

    /**
     * {@inheritDoc}
     */
    public NodeId canCheckout(NodeStateEx state, NodeId activityId) throws RepositoryException {
        return vMgr.canCheckout(state, activityId);
    }

    /**
     * {@inheritDoc}
     */
    public InternalVersion checkin(
            Session session, NodeStateEx node, Calendar created)
            throws RepositoryException {
        if (isInXA()) {
            return checkin(node, created);
        } else {
            return vMgr.checkin(session, node, created);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeVersion(Session session, InternalVersionHistory history,
                              Name versionName)
            throws RepositoryException {
        if (isInXA()) {
            internalRemoveVersion((InternalVersionHistoryImpl) history, versionName);
        } else {
            vMgr.removeVersion(session, history, versionName);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeVersionHistory(Session session, InternalVersionHistory history)
            throws RepositoryException {
        if (isInXA()) {
            internalRemoveVersionHistory((InternalVersionHistoryImpl) history);
        } else {
            vMgr.removeVersionHistory(session, history);
        }
    }

    /**
     * {@inheritDoc}
     */
    public InternalVersion setVersionLabel(Session session,
                                           InternalVersionHistory history,
                                           Name version,
                                           Name label, boolean move)
            throws RepositoryException {

        if (isInXA()) {
            return setVersionLabel((InternalVersionHistoryImpl) history,
                    version, label, move);
        } else {
            return vMgr.setVersionLabel(session, history, version, label, move);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws Exception {
        stateMgr.dispose();
    }

    //---------------------------------------------< VirtualItemStateProvider >

    /**
     * {@inheritDoc}
     */
    public boolean isVirtualRoot(ItemId id) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public NodeId getVirtualRootId() {
        // never used
        return null;
    }

    public NodeId[] getVirtualRootIds() {
        // never used
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public VirtualPropertyState createPropertyState(VirtualNodeState parent,
                                                    Name name, int type,
                                                    boolean multiValued)
            throws RepositoryException {

        throw new IllegalStateException("Read-only");
    }

    /**
     * {@inheritDoc}
     */
    public VirtualNodeState createNodeState(VirtualNodeState parent, Name name,
                                            NodeId id, Name nodeTypeName)
            throws RepositoryException {

        throw new IllegalStateException("Read-only");
    }

    /**
     * {@inheritDoc}
     */
    public boolean setNodeReferences(ChangeLog references) {
        ChangeLog changeLog = ((XAItemStateManager) stateMgr).getChangeLog();
        if (changeLog != null) {
            for (NodeReferences refs : references.modifiedRefs()) {
                changeLog.modified(refs);
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Return item states for changes only. Global version manager will return
     * other items.
     */
    public ItemState getItemState(ItemId id)
            throws NoSuchItemStateException, ItemStateException {

        ChangeLog changeLog = ((XAItemStateManager) stateMgr).getChangeLog();
        if (changeLog != null) {
            return changeLog.get(id);
        }
        throw new NoSuchItemStateException("State not in change log: " + id);
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasItemState(ItemId id) {
        ChangeLog changeLog = ((XAItemStateManager) stateMgr).getChangeLog();
        if (changeLog != null) {
            return changeLog.has(id);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public NodeReferences getNodeReferences(NodeId id)
            throws NoSuchItemStateException, ItemStateException {

        ChangeLog changeLog = ((XAItemStateManager) stateMgr).getChangeLog();
        if (changeLog != null) {
            return changeLog.getReferencesTo(id);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasNodeReferences(NodeId id) {
        ChangeLog changeLog = ((XAItemStateManager) stateMgr).getChangeLog();
        if (changeLog != null) {
            return changeLog.getReferencesTo(id) != null;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Not needed.
     */
    public void addListener(ItemStateListener listener) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * Not needed.
     */
    public void removeListener(ItemStateListener listener) {
    }

    //-----------------------------------------------< InternalVersionManagerBase >

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeStateEx getHistoryRoot() {
        return historyRoot;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeStateEx getActivitiesRoot() {
        return activitiesRoot;
    }

    /**
     * {@inheritDoc}
     */
     @Override
     protected InternalVersionItem getItem(NodeId id) throws RepositoryException {
        InternalVersionItem item = null;
        if (xaItems != null) {
            item = xaItems.get(id);
        }
        if (item == null) {
            item = vMgr.getItem(id);
        }
        return item;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasItem(NodeId id) {
        if (xaItems != null && xaItems.containsKey(id)) {
            return true;
        }
        return vMgr.hasItem(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasItemReferences(NodeId id)
            throws RepositoryException {
        return session.getNodeById(id).getReferences().hasNext();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeStateEx getNodeStateEx(NodeId parentNodeId)
            throws RepositoryException {
        try {
            NodeState state = (NodeState) stateMgr.getItemState(parentNodeId);
            return new NodeStateEx(stateMgr, ntReg, state, null);
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Before modifying version history given, make a local copy of it.
     */
    @Override
    protected InternalVersion internalCheckin(
            InternalVersionHistoryImpl history,
            NodeStateEx node, boolean simple, Calendar created)
            throws RepositoryException {

        if (history.getVersionManager() != this) {
            history = makeLocalCopy(history);
            xaItems.put(history.getId(), history);
        }
        InternalVersion version =
            super.internalCheckin(history, node, simple, created);
        NodeId frozenNodeId = version.getFrozenNodeId();
        InternalVersionItem frozenNode = createInternalVersionItem(frozenNodeId);
        if (frozenNode != null) {
            xaItems.put(frozenNodeId, frozenNode);
        }
        return version;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Before modifying version history given, make a local copy of it.
     */
    @Override
    protected void internalRemoveVersion(InternalVersionHistoryImpl history, Name name)
            throws VersionException, RepositoryException {

        if (history.getVersionManager() != this) {
            history = makeLocalCopy(history);
            xaItems.put(history.getId(), history);
            // also put 'successor' and 'predecessor' version items to xaItem sets
            InternalVersion v = history.getVersion(name);
            for (InternalVersion v1 : v.getSuccessors()) {
                xaItems.put(v1.getId(), v1);
            }
            for (InternalVersion v1 : v.getPredecessors()) {
                xaItems.put(v1.getId(), v1);
            }
        }
        super.internalRemoveVersion(history, name);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Before modifying version history given, make a local copy of it.
     */
    @Override
    protected InternalVersion setVersionLabel(InternalVersionHistoryImpl history,
                                              Name version, Name label,
                                              boolean move)
            throws RepositoryException {

        if (history.getVersionManager() != this) {
            history = makeLocalCopy(history);
            xaItems.put(history.getId(), history);
        }
        return super.setVersionLabel(history, version, label, move);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Put the version object into our cache.
     */
    @Override
    protected void versionCreated(InternalVersion version) {
        xaItems.put(version.getId(), version);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Remove the version object from our cache.
     */
    @Override
    protected void versionDestroyed(InternalVersion version) {
        xaItems.remove(version.getId());
    }

    //-------------------------------------------------------------------< XA >

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void associate(TransactionContext tx) {
        ((XAItemStateManager) stateMgr).associate(tx);

        Map<NodeId, InternalVersionItem> xaItems = null;
        if (tx != null) {
            xaItems = (Map<NodeId, InternalVersionItem>) tx.getAttribute(ITEMS_ATTRIBUTE_NAME);
            if (xaItems == null) {
                xaItems = new HashMap<NodeId, InternalVersionItem>();
                tx.setAttribute(ITEMS_ATTRIBUTE_NAME, xaItems);
            }
        }
        this.xaItems = xaItems;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Delegate the call to our XA item state manager.
     */
    public void beforeOperation(TransactionContext tx) {
        ((XAItemStateManager) stateMgr).beforeOperation(tx);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Delegate the call to our XA item state manager.
     */
    public void prepare(TransactionContext tx) throws TransactionException {
        if (vmgrLocked) {
            ((XAItemStateManager) stateMgr).prepare(tx);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Delegate the call to our XA item state manager. If successful, inform
     * global repository manager to update its caches.
     */
    @SuppressWarnings("unchecked")
    public void commit(TransactionContext tx) throws TransactionException {
        if (vmgrLocked) {
            ((XAItemStateManager) stateMgr).commit(tx);
            Map<NodeId, InternalVersionItem> xaItems =
                    (Map<NodeId, InternalVersionItem>) tx.getAttribute(ITEMS_ATTRIBUTE_NAME);
            vMgr.itemsUpdated(xaItems.values());
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Delegate the call to our XA item state manager.
     */
    public void rollback(TransactionContext tx) {
        if (vmgrLocked) {
            ((XAItemStateManager) stateMgr).rollback(tx);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Delegate the call to our XA item state manager.
     */
    public void afterOperation(TransactionContext tx) {
        ((XAItemStateManager) stateMgr).afterOperation(tx);
    }

    /**
     * Returns an {@link InternalXAResource} that acquires a write lock on the
     * version manager in {@link InternalXAResource#prepare(TransactionContext)}.
     *
     * @return an internal XA resource.
     */
    public InternalXAResource getXAResourceBegin() {
        return new InternalXAResource() {
            public void associate(TransactionContext tx) {
            }

            public void beforeOperation(TransactionContext tx) {
            }

            public void prepare(TransactionContext tx) {
                vmgrLock = vMgr.acquireWriteLock();
                vmgrLocked = true;
            }

            public void commit(TransactionContext tx) {
                // JCR-2712: Ensure that the transaction is prepared
                if (!vmgrLocked) {
                    prepare(tx);
                }
            }

            public void rollback(TransactionContext tx) {
                // JCR-2712: Ensure that the transaction is prepared
                if (!vmgrLocked) {
                    prepare(tx);
                }
            }

            public void afterOperation(TransactionContext tx) {
            }
        };
    }

    /**
     * Returns an {@link InternalXAResource} that releases the write lock on the
     * version manager in {@link InternalXAResource#commit(TransactionContext)}
     * or {@link InternalXAResource#rollback(TransactionContext)}.
     *
     * @return an internal XA resource.
     */
    public InternalXAResource getXAResourceEnd() {
        return new InternalXAResource() {
            public void associate(TransactionContext tx) {
            }

            public void beforeOperation(TransactionContext tx) {
            }

            public void prepare(TransactionContext tx) {
            }

            public void commit(TransactionContext tx) {
                internalReleaseWriteLock();
            }

            public void rollback(TransactionContext tx) {
                internalReleaseWriteLock();
            }

            public void afterOperation(TransactionContext tx) {
            }

            private void internalReleaseWriteLock() {
                if (vmgrLocked) {
                    vmgrLock.release();
                    vmgrLocked = false;
                }
            }
        };
    }

    //-------------------------------------------------------< implementation >

    /**
     * Return a flag indicating whether this version manager is currently
     * associated with an XA transaction.
     * @return <code>true</code> if the version manager is in a transaction
     */
    private boolean isInXA() {
        return xaItems != null;
    }

    /**
     * Make a local copy of an internal version item. This will recreate the
     * (global) version item with state information from our own state
     * manager.
     * @param history source
     * @return the new copy
     * @throws RepositoryException if an error occurs
     */
    private InternalVersionHistoryImpl makeLocalCopy(InternalVersionHistoryImpl history)
            throws RepositoryException {
        VersioningLock.ReadLock lock = acquireReadLock();
        try {
            NodeState state = (NodeState) stateMgr.getItemState(history.getId());
            NodeStateEx stateEx = new NodeStateEx(stateMgr, ntReg, state, null);
            return new InternalVersionHistoryImpl(this, stateEx);
        } catch (ItemStateException e) {
            throw new RepositoryException("Unable to make local copy", e);
        } finally {
            lock.release();
        }
    }

    /**
     * Make a local copy of an internal version item. This will recreate the
     * (global) version item with state information from our own state
     * manager.
     * @param act source
     * @return the new copy
     * @throws RepositoryException if an error occurs
     */
    private InternalActivityImpl makeLocalCopy(InternalActivityImpl act)
            throws RepositoryException {
        VersioningLock.ReadLock lock = acquireReadLock();
        try {
            NodeState state = (NodeState) stateMgr.getItemState(act.getId());
            NodeStateEx stateEx = new NodeStateEx(stateMgr, ntReg, state, null);
            return new InternalActivityImpl(this, stateEx);
        } catch (ItemStateException e) {
            throw new RepositoryException("Unable to make local copy", e);
        } finally {
            lock.release();
        }
    }

    /**
     * Return a flag indicating whether an internal version item belongs to
     * a different XA environment.
     * @param item the item to check
     * @return <code>true</code> if in a different env
     */
    boolean differentXAEnv(InternalVersionItemImpl item) {
        if (item.getVersionManager() == this) {
            if (xaItems == null || !xaItems.containsKey(item.getId())) {
                return true;
            }
        }
        return false;
    }
}
