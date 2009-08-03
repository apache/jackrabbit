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
import java.util.Iterator;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

import org.apache.jackrabbit.core.InternalXAResource;
import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.TransactionContext;
import org.apache.jackrabbit.core.TransactionException;
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
import org.apache.jackrabbit.core.state.NodeReferencesId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.XAItemStateManager;
import org.apache.jackrabbit.core.state.ISMLocking.ReadLock;
import org.apache.jackrabbit.core.state.ISMLocking.WriteLock;
import org.apache.jackrabbit.core.virtual.VirtualItemStateProvider;
import org.apache.jackrabbit.core.virtual.VirtualNodeState;
import org.apache.jackrabbit.core.virtual.VirtualPropertyState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

/**
 * Implementation of a {@link VersionManager} that works in an XA environment.
 * Works as a filter between a version manager client and the global version
 * manager.
 */
public class XAVersionManager extends AbstractVersionManager
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
    private final VersionManagerImpl vMgr;

    /**
     * The session that uses this version manager.
     */
    private SessionImpl session;

    /**
     * Items that have been modified and are part of the XA environment.
     */
    private Map xaItems;

    /**
     * flag that indicates if the version manager was locked during prepare
     */
    private boolean vmgrLocked = false;

    /**
     * The global write lock on the version manager.
     */
    private WriteLock vmgrLock;

    /**
     * Creates a new instance of this class.
     */
    public XAVersionManager(VersionManagerImpl vMgr, NodeTypeRegistry ntReg,
                            SessionImpl session, ItemStateCacheFactory cacheFactory)
            throws RepositoryException {
        super(ntReg);
        this.vMgr = vMgr;
        this.session = session;
        this.stateMgr = XAItemStateManager.createInstance(vMgr.getSharedStateMgr(),
                this, CHANGE_LOG_ATTRIBUTE_NAME, cacheFactory);

        NodeState state;
        try {
            state = (NodeState) stateMgr.getItemState(vMgr.getHistoryRootId());
        } catch (ItemStateException e) {
            throw new RepositoryException("Unable to retrieve history root", e);
        }
        this.historyRoot = new NodeStateEx(stateMgr, ntReg, state, NameConstants.JCR_VERSIONSTORAGE);
    }

    //------------------------------------------< EventStateCollectionFactory >

    /**
     * @inheritDoc
     */
    public EventStateCollection createEventStateCollection()
            throws RepositoryException {
        return vMgr.getEscFactory().createEventStateCollection(session);
    }

    //-------------------------------------------------------< VersionManager >

    /**
     * {@inheritDoc}
     */
    public VirtualItemStateProvider getVirtualItemStateProvider() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    protected VersionHistoryInfo createVersionHistory(Session session, NodeState node)
            throws RepositoryException {

        if (isInXA()) {
            NodeStateEx state = createVersionHistory(node);
            InternalVersionHistory history =
                new InternalVersionHistoryImpl(vMgr, state);
            xaItems.put(state.getNodeId(), history);
            Name root = NameConstants.JCR_ROOTVERSION;
            return new VersionHistoryInfo(
                    state.getNodeId(),
                    state.getState().getChildNodeEntry(root, 1).getId());
        }
        return vMgr.createVersionHistory(session, node);
    }

    /**
     * {@inheritDoc}
     */
    public Version checkin(NodeImpl node, Calendar cal) throws RepositoryException {
        if (isInXA()) {
            InternalVersionHistory vh;
            InternalVersion version;
            if (node.isNodeType(NameConstants.MIX_VERSIONABLE)) {
                // in full versioning, the history id can be retrieved via
                // the property
                String histUUID = node.getProperty(NameConstants.JCR_VERSIONHISTORY).getString();
                vh = getVersionHistory(NodeId.valueOf(histUUID));
                version = checkin((InternalVersionHistoryImpl) vh, node, false, cal);
            } else {
                // in simple versioning the history id needs to be calculated
                vh = getVersionHistoryOfNode(node.getNodeId());
                version = checkin((InternalVersionHistoryImpl) vh, node, true, cal);
            }
            return (Version) ((SessionImpl) node.getSession()).getNodeById(version.getId());
        }
        return vMgr.checkin(node, cal);
    }

    /**
     * {@inheritDoc}
     */
    public void removeVersion(VersionHistory history, Name versionName)
            throws RepositoryException {

        if (isInXA()) {
            InternalVersionHistoryImpl vh = (InternalVersionHistoryImpl)
                    ((VersionHistoryImpl) history).getInternalVersionHistory();
            removeVersion(vh, versionName);
            return;
        }
        vMgr.removeVersion(history, versionName);
    }

    /**
     * {@inheritDoc}
     */
    public Version setVersionLabel(VersionHistory history, Name version,
                                   Name label, boolean move)
            throws RepositoryException {

        if (isInXA()) {
            InternalVersionHistoryImpl vh = (InternalVersionHistoryImpl)
                    ((VersionHistoryImpl) history).getInternalVersionHistory();
            InternalVersion v = setVersionLabel(vh, version, label, move);
            if (v == null) {
                return null;
            } else {
                return (Version) ((SessionImpl) history.getSession()).getNodeById(v.getId());
            }
        }
        return vMgr.setVersionLabel(history, version, label, move);
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
            Iterator iterator = references.modifiedRefs();
            while (iterator.hasNext()) {
                changeLog.modified((NodeReferences) iterator.next());
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
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
    public NodeReferences getNodeReferences(NodeReferencesId id)
            throws NoSuchItemStateException, ItemStateException {

        ChangeLog changeLog = ((XAItemStateManager) stateMgr).getChangeLog();
        if (changeLog != null) {
            return changeLog.get(id);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasNodeReferences(NodeReferencesId id) {
        ChangeLog changeLog = ((XAItemStateManager) stateMgr).getChangeLog();
        if (changeLog != null) {
            return changeLog.get(id) != null;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Not needed.
     */
    public void addListener(ItemStateListener listener) {
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Not needed.
     */
    public void removeListener(ItemStateListener listener) {
    }

    //-----------------------------------------------< AbstractVersionManager >

    /**
     * {@inheritDoc}
     */
     protected InternalVersionItem getItem(NodeId id) throws RepositoryException {
        InternalVersionItem item = null;
        if (xaItems != null) {
            item = (InternalVersionItem) xaItems.get(id);
        }
        if (item == null) {
            item = vMgr.getItem(id);
        }
        return item;
    }

    /**
     * {@inheritDoc}
     */
    protected boolean hasItem(NodeId id) {
        if (xaItems != null && xaItems.containsKey(id)) {
            return true;
        }
        return vMgr.hasItem(id);
    }

    /**
     * {@inheritDoc}
     */
    protected boolean hasItemReferences(NodeId id)
            throws RepositoryException {
        return session.getNodeById(id).getReferences().hasNext();
    }

    /**
     * {@inheritDoc}
     */
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
     * <p/>
     * Before modifying version history given, make a local copy of it.
     */
    protected InternalVersion checkin(InternalVersionHistoryImpl history,
            NodeImpl node, boolean simple, Calendar cal)
            throws RepositoryException {

        if (history.getVersionManager() != this) {
            history = makeLocalCopy(history);
            xaItems.put(history.getId(), history);
        }
        InternalVersion version = super.checkin(history, node, simple, cal);
        NodeId frozenNodeId = version.getFrozenNodeId();
        InternalVersionItem frozenNode = createInternalVersionItem(frozenNodeId);
        if (frozenNode != null) {
            xaItems.put(frozenNodeId, frozenNode);
        }
        return version;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Before modifying version history given, make a local copy of it.
     */
    protected void removeVersion(InternalVersionHistoryImpl history, Name name)
            throws VersionException, RepositoryException {

        if (history.getVersionManager() != this) {
            history = makeLocalCopy(history);
            xaItems.put(history.getId(), history);
            // also put 'successor' and 'predecessor' version items to xaItem sets
            InternalVersion v = history.getVersion(name);
            InternalVersion[] vs = v.getSuccessors();
            for (int i = 0; i < vs.length; i++) {
                xaItems.put(vs[i].getId(), vs[i]);
            }
            vs = v.getPredecessors();
            for (int i = 0; i < vs.length; i++) {
                xaItems.put(vs[i].getId(), vs[i]);
            }
        }
        super.removeVersion(history, name);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Before modifying version history given, make a local copy of it.
     */
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
     * <p/>
     * Put the version object into our cache.
     */
    protected void versionCreated(InternalVersion version) {
        xaItems.put(version.getId(), version);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Remove the version object from our cache.
     */
    protected void versionDestroyed(InternalVersion version) {
        xaItems.remove(version.getId());
    }

    //-------------------------------------------------------------------< XA >

    /**
     * {@inheritDoc}
     */
    public void associate(TransactionContext tx) {
        ((XAItemStateManager) stateMgr).associate(tx);

        Map xaItems = null;
        if (tx != null) {
            xaItems = (Map) tx.getAttribute(ITEMS_ATTRIBUTE_NAME);
            if (xaItems == null) {
                xaItems = new HashMap();
                tx.setAttribute(ITEMS_ATTRIBUTE_NAME, xaItems);
            }
        }
        this.xaItems = xaItems;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Delegate the call to our XA item state manager.
     */
    public void beforeOperation(TransactionContext tx) {
        ((XAItemStateManager) stateMgr).beforeOperation(tx);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Delegate the call to our XA item state manager.
     */
    public void prepare(TransactionContext tx) throws TransactionException {
        if (vmgrLocked) {
            ((XAItemStateManager) stateMgr).prepare(tx);
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Delegate the call to our XA item state manager. If successful, inform
     * global repository manager to update its caches.
     */
    public void commit(TransactionContext tx) throws TransactionException {
        if (vmgrLocked) {
            ((XAItemStateManager) stateMgr).commit(tx);
            Map xaItems = (Map) tx.getAttribute(ITEMS_ATTRIBUTE_NAME);
            vMgr.itemsUpdated(xaItems.values());
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Delegate the call to our XA item state manager.
     */
    public void rollback(TransactionContext tx) {
        if (vmgrLocked) {
            ((XAItemStateManager) stateMgr).rollback(tx);
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
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
            }

            public void rollback(TransactionContext tx) {
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
     */
    private boolean isInXA() {
        return xaItems != null;
    }

    /**
     * Make a local copy of an internal version item. This will recreate the
     * (global) version item with state information from our own state
     * manager.
     */
    private InternalVersionHistoryImpl makeLocalCopy(InternalVersionHistoryImpl history)
            throws RepositoryException {
        ReadLock lock = acquireReadLock();
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
     * Return a flag indicating whether an internal version item belongs to
     * a different XA environment.
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
