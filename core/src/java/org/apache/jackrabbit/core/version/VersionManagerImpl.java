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
package org.apache.jackrabbit.core.version;

import org.apache.commons.collections.map.ReferenceMap;
import org.apache.jackrabbit.Constants;
import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.PropertyImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.observation.DelegatingObservationDispatcher;
import org.apache.jackrabbit.core.observation.EventState;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.LocalItemStateManager;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeReferencesId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PersistenceManager;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.SharedItemStateManager;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.core.virtual.VirtualItemStateProvider;
import org.apache.jackrabbit.core.virtual.VirtualNodeState;
import org.apache.jackrabbit.core.virtual.VirtualPropertyState;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.uuid.UUID;
import org.apache.log4j.Logger;

import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * This Class implements a VersionManager.
 */
public class VersionManagerImpl implements VersionManager,
        VirtualItemStateProvider, Constants {

    /**
     * the default logger
     */
    private static Logger log = Logger.getLogger(VersionManager.class);

    /**
     * The root node UUID for the version storage
     */
    private final String VERSION_STORAGE_NODE_UUID;

    /**
     * The persistence manager for the versions
     */
    private final PersistenceManager pMgr;

    /**
     * the state manager for the version storage
     */
    private LocalItemStateManager stateMgr;

    /**
     * the persistent root node of the version histories
     */
    private final NodeStateEx historyRoot;

    /**
     * the node type manager
     */
    private NodeTypeRegistry ntReg;

    /**
     * the observation manager
     */
    private DelegatingObservationDispatcher obsMgr;

    /**
     * Map of returned items. this is kept for invalidating
     */
    private ReferenceMap items = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.WEAK);

    /**
     * Map of returned items. this is kept for invalidating
     */
    private ReferenceMap versionItems = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.WEAK);

    /**
     * Creates a bew vesuion manager
     *
     */
    public VersionManagerImpl(PersistenceManager pMgr, NodeTypeRegistry ntReg,
                              DelegatingObservationDispatcher obsMgr, String rootUUID,
                              String rootParentUUID) throws RepositoryException {
        try {
            this.pMgr = pMgr;
            this.ntReg = ntReg;
            this.obsMgr = obsMgr;
            this.VERSION_STORAGE_NODE_UUID = rootUUID;

            // need to store the version storage root directly into the persistence manager
            if (!pMgr.exists(new NodeId(rootUUID))) {
                NodeState root = pMgr.createNew(new NodeId(rootUUID));
                root.setParentUUID(rootParentUUID);
                root.setDefinitionId(ntReg.getEffectiveNodeType(REP_SYSTEM).getApplicableChildNodeDef(
                        JCR_VERSIONSTORAGE, REP_VERSIONSTORAGE).getId());
                root.setNodeTypeName(REP_VERSIONSTORAGE);
                PropertyState pt = pMgr.createNew(new PropertyId(rootUUID, JCR_PRIMARYTYPE));
                pt.setDefinitionId(ntReg.getEffectiveNodeType(REP_SYSTEM).getApplicablePropertyDef(
                        JCR_PRIMARYTYPE, PropertyType.NAME, false).getId());
                pt.setMultiValued(false);
                pt.setType(PropertyType.NAME);
                pt.setValues(new InternalValue[]{InternalValue.create(REP_VERSIONSTORAGE)});
                root.addPropertyName(pt.getName());
                ChangeLog cl = new ChangeLog();
                cl.added(root);
                cl.added(pt);
                pMgr.store(cl);
            }
            SharedItemStateManager sharedStateMgr = new SharedItemStateManager(pMgr, VERSION_STORAGE_NODE_UUID, ntReg);
            stateMgr = new LocalItemStateManager(sharedStateMgr, null);
            NodeState nodeState = (NodeState) stateMgr.getItemState(new NodeId(VERSION_STORAGE_NODE_UUID));
            historyRoot = new NodeStateEx(stateMgr, ntReg, nodeState, JCR_VERSIONSTORAGE);
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * returns the virtual item state provider that exposes the internal versions
     * as items.
     *
     * @param base
     * @return
     */
    public synchronized ItemStateManager getItemStateProvider(ItemStateManager base) {
        return stateMgr;
    }

    /**
     * Close this version manager. After having closed a persistence
     * manager, further operations on this object are treated as illegal
     * and throw
     *
     * @throws Exception if an error occurs
     */
    public void close() throws Exception {
        pMgr.close();
    }

    /**
     * Creates a new version history. This action is needed either when creating
     * a new 'mix:versionable' node or when adding the 'mix:versionalbe' mixin
     * to a node.
     *
     * @param node
     * @return
     * @throws javax.jcr.RepositoryException
     */
    public VersionHistory createVersionHistory(Session session, NodeState node)
            throws RepositoryException {

        InternalVersionHistory history = createVersionHistory(node);
        if (history == null) {
            throw new VersionException("History already exists for node " + node.getUUID());
        }
        VersionHistoryImpl vh = (VersionHistoryImpl) session.getNodeByUUID(history.getId());

        // generate observation events
        List events = new ArrayList();
        recursiveAdd(events, (NodeImpl) vh.getParent(), vh);
        obsMgr.dispatch(events, (SessionImpl) session);

        return vh;
    }

    /**
     * Creates a new Version History.
     *
     * @param node the node for which the version history is to be initialized
     * @return the newly created version history.
     * @throws RepositoryException
     */
    private InternalVersionHistory createVersionHistory(NodeState node)
            throws RepositoryException {

        try {
            stateMgr.edit();
        } catch (IllegalStateException e) {
            throw new RepositoryException("Unable to start edit operation", e);
        }

        boolean succeeded = false;

        try {
            // create deep path
            String uuid = node.getUUID();
            NodeStateEx root = historyRoot;
            for (int i = 0; i < 3; i++) {
                QName name = new QName(NS_DEFAULT_URI, uuid.substring(i * 2, i * 2 + 2));
                if (!root.hasNode(name)) {
                    root.addNode(name, REP_VERSIONSTORAGE, null, false);
                    root.store();
                }
                root = root.getNode(name, 1);
            }
            QName historyNodeName = new QName(NS_DEFAULT_URI, uuid);
            if (root.hasNode(historyNodeName)) {
                // already exists
                return null;
            }

            // create new history node in the persistent state
            InternalVersionHistoryImpl hist = InternalVersionHistoryImpl.create(this, root, UUID.randomUUID().toString(), historyNodeName, node);

            // end update
            stateMgr.update();
            succeeded = true;

            log.info("Created new version history " + hist.getId() + " for " + node + ".");
            return hist;

        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        } finally {
            if (!succeeded) {
                // update operation failed, cancel all modifications
                stateMgr.cancel();
            }
        }
    }

    /**
     * Checks if the version history with the given id exists
     *
     * @param id
     * @return
     */
    public boolean hasVersionHistory(String id) {
        // todo: probably need to check if this item is really a history
        return hasItem(id);
    }

    /**
     * Returns the version history with the given id
     *
     * @param id
     * @return
     * @throws RepositoryException
     */
    public InternalVersionHistory getVersionHistory(String id) throws RepositoryException {
        return (InternalVersionHistory) getItem(id);
    }

    /**
     * Checks if the version with the given id exists
     *
     * @param id
     * @return
     */
    public boolean hasVersion(String id) {
        // todo: probably need to check if this item is really a version
        return hasItem(id);
    }

    /**
     * Returns the version with the given id
     *
     * @param id
     * @return
     * @throws RepositoryException
     */
    public InternalVersion getVersion(String id) throws RepositoryException {
        return (InternalVersion) getItem(id);
    }

    /**
     * checks, if the node with the given id exists
     *
     * @param id
     * @return
     */
    public boolean hasItem(String id) {
        return versionItems.containsKey(id) || stateMgr.hasItemState(new NodeId(id));
    }

    /**
     * Returns the item with the given persistent id
     *
     * @param uuid
     * @return
     * @throws RepositoryException
     */
    synchronized InternalVersionItem getItem(String uuid) throws RepositoryException {
        NodeId id = new NodeId(uuid);
        try {
            InternalVersionItem item = (InternalVersionItem) versionItems.get(id);
            if (item == null) {
                if (stateMgr.hasItemState(id)) {
                    NodeState state = (NodeState) stateMgr.getItemState(id);
                    NodeStateEx pNode = new NodeStateEx(stateMgr, ntReg, state, null);
                    String parentUUID = pNode.getParentUUID();
                    InternalVersionItem parent =
                            (parentUUID != null) ? getItem(parentUUID) : null;
                    QName ntName = state.getNodeTypeName();
                    if (ntName.equals(NT_FROZENNODE)) {
                        item = new InternalFrozenNodeImpl(this, pNode, parent);
                    } else if (ntName.equals(NT_VERSIONEDCHILD)) {
                        item = new InternalFrozenVHImpl(this, pNode, parent);
                    } else if (ntName.equals(NT_VERSION)) {
                        item = ((InternalVersionHistory) parent).getVersion(uuid);
                    } else if (ntName.equals(NT_VERSIONHISTORY)) {
                        item = new InternalVersionHistoryImpl(this, pNode);
                    } else {
                        //return null;
                    }
                }
                if (item != null) {
                    versionItems.put(id, item);
                }
            }
            return item;
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * invokes the checkin() on the persistent version manager and remaps the
     * newly created version objects.
     *
     * @param node
     * @return
     * @throws RepositoryException
     */
    public synchronized Version checkin(NodeImpl node) throws RepositoryException {
        SessionImpl session = (SessionImpl) node.getSession();
        InternalVersion version = internalCheckin(node);
        // need to recalc successor prop
        InternalVersion[] preds = version.getPredecessors();
        for (int i=0; i<preds.length; i++) {
            ItemState state = (ItemState) items.remove(new PropertyId(preds[i].getId(), JCR_SUCCESSORS));
            if (state != null) {
                state.discard();
            }
        }
        invalidateItem(new NodeId(version.getVersionHistory().getId()), true);
        VersionImpl v = (VersionImpl) session.getNodeByUUID(version.getId());

        // generate observation events
        List events = new ArrayList();
        recursiveAdd(events, (NodeImpl) v.getParent(), v);
        obsMgr.dispatch(events, session);

        return v;
    }

    /**
     * Checks in a node
     *
     * @param node
     * @return
     * @throws RepositoryException
     * @see javax.jcr.Node#checkin()
     */
    private InternalVersion internalCheckin(NodeImpl node) throws RepositoryException {
        // assuming node is versionable and checkout (check in nodeimpl)
        // To create a new version of a versionable node N, the client calls N.checkin.
        // This causes the following series of events:
        String histUUID = node.getProperty(Constants.JCR_VERSIONHISTORY).getString();
        InternalVersionHistoryImpl history = (InternalVersionHistoryImpl) getVersionHistory(histUUID);

        // 0. resolve the predecessors
        Value[] values = node.getProperty(Constants.JCR_PREDECESSORS).getValues();
        InternalVersion[] preds = new InternalVersion[values.length];
        for (int i = 0; i < values.length; i++) {
            preds[i] = history.getVersion(values[i].getString());
        }

        // 0.1 search a predecessor, suitable for generating the new name
        String versionName = null;
        int maxDots = Integer.MAX_VALUE;
        for (int i = 0; i < preds.length; i++) {
            // take the first pred. without a successor
            if (preds[i].getSuccessors().length == 0) {
                versionName = preds[i].getName().getLocalName(); //assuming no namespaces in version names
                // need to count the dots
                int pos = -1;
                int numDots = 0;
                while (versionName.indexOf('.', pos + 1) >= 0) {
                    pos = versionName.indexOf('.', pos + 1);
                    numDots++;
                }
                if (numDots < maxDots) {
                    maxDots = numDots;
                    if (pos < 0) {
                        versionName = "1.0";
                    } else {
                        versionName = versionName.substring(0, pos + 1)
                                + (Integer.parseInt(versionName.substring(pos + 1)) + 1);
                    }
                }
                break;
            }
        }
        // if no empty found, generate new name
        if (versionName == null) {
            versionName = preds[0].getName().getLocalName();
            do {
                versionName += ".1";
            } while (history.hasVersion(new QName("", versionName)));
        }

        try {
            stateMgr.edit();
        } catch (IllegalStateException e) {
            throw new RepositoryException("Unable to start edit operation.");
        }

        boolean succeeded = false;

        try {
            InternalVersionImpl v = history.checkin(new QName("", versionName), node);
            stateMgr.update();
            succeeded = true;

            return v;
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        } finally {
            if (!succeeded) {
                // update operation failed, cancel all modifications
                stateMgr.cancel();
            }
        }
    }


    /**
     * Removes the specified version from the history
     *
     * @param history the version history from where to remove the version.
     * @param name the name of the version to remove.
     * @throws VersionException if the version <code>history</code> does
     *  not have a version with <code>name</code>.
     * @throws RepositoryException if any other error occurs.
     */
    public void removeVersion(VersionHistory history, QName name)
            throws VersionException, RepositoryException {
        if (!((VersionHistoryImpl) history).hasNode(name)) {
            throw new VersionException("Version with name " + name.toString()
                    + " does not exist in this VersionHistory");
        }
        // generate observation events
        SessionImpl session = (SessionImpl) history.getSession();
        VersionImpl version = (VersionImpl) ((VersionHistoryImpl) history).getNode(name);
        List events = new ArrayList();
        recursiveRemove(events, (NodeImpl) history, version);

        InternalVersionHistoryImpl vh = (InternalVersionHistoryImpl)
                ((VersionHistoryImpl) history).getInternalVersionHistory();

        try {
            stateMgr.edit();
        } catch (IllegalStateException e) {
            throw new VersionException("Unable to start edit operation", e);
        }
        boolean succeeded = false;
        try {
            vh.removeVersion(name);
            stateMgr.update();
            succeeded = true;
        } catch (ItemStateException e) {
            log.error("Error while storing: " + e.toString());
        } finally {
            if (!succeeded) {
                // update operation failed, cancel all modifications
                stateMgr.cancel();
            }
        }

        invalidateItem(new NodeId(vh.getId()), true);
        obsMgr.dispatch(events, session);
    }

    /**
     * {@inheritDoc}
     */
    public Version setVersionLabel(VersionHistory history, QName version,
                                   QName label, boolean move)
            throws RepositoryException {
        SessionImpl session = (SessionImpl) history.getSession();

        InternalVersionHistoryImpl vh = (InternalVersionHistoryImpl)
                ((VersionHistoryImpl) history).getInternalVersionHistory();
        NodeImpl labelNode = ((VersionHistoryImpl) history).getNode(JCR_VERSIONLABELS);

        try {
            stateMgr.edit();
        } catch (IllegalStateException e) {
            throw new VersionException("Unable to start edit operation", e);
        }
        InternalVersion v = null;
        boolean success = false;
        try {
            v = vh.setVersionLabel(version, label, move);
            stateMgr.update();
            success = true;
        } catch(ItemStateException e) {
            log.error("Error while storing: " + e.toString());
        } finally {
            if (!success) {
                // update operation failed, cancel all modifications
                stateMgr.cancel();
            }
        }

        // collect observation events
        List events = new ArrayList();
        if (version == null && v != null) {
            // label removed
            events.add(EventState.propertyRemoved(
                    labelNode.internalGetUUID(),
                    labelNode.getPrimaryPath(),
                    Path.PathElement.fromString(label.toString()),
                    (NodeTypeImpl) labelNode.getPrimaryNodeType(),
                    labelNode.getSession()
            ));
        } else if (v == null) {
            // label added
            events.add(EventState.propertyAdded(
                    labelNode.internalGetUUID(),
                    labelNode.getPrimaryPath(),
                    Path.PathElement.fromString(label.toString()),
                    (NodeTypeImpl) labelNode.getPrimaryNodeType(),
                    labelNode.getSession()
            ));
        } else {
            // label modified
            events.add(EventState.propertyChanged(
                    labelNode.internalGetUUID(),
                    labelNode.getPrimaryPath(),
                    Path.PathElement.fromString(label.toString()),
                    (NodeTypeImpl) labelNode.getPrimaryNodeType(),
                    labelNode.getSession()
            ));
        }
        invalidateItem(new NodeId(labelNode.internalGetUUID()), true);
        obsMgr.dispatch(events, session);
        if (v == null) {
            return null;
        } else {
            return (VersionImpl) session.getNodeByUUID(v.getId());
        }
    }

    /**
     * Adds a subtree of itemstates as 'added' to a list of events
     *
     * @param events
     * @param parent
     * @param node
     * @throws RepositoryException
     */
    private void recursiveAdd(List events, NodeImpl parent, NodeImpl node)
            throws RepositoryException {

        events.add(EventState.childNodeAdded(
                parent.internalGetUUID(),
                parent.getPrimaryPath(),
                node.internalGetUUID(),
                node.getPrimaryPath().getNameElement(),
                (NodeTypeImpl) parent.getPrimaryNodeType(),
                node.getSession()
        ));

        PropertyIterator iter = node.getProperties();
        while (iter.hasNext()) {
            PropertyImpl prop = (PropertyImpl) iter.nextProperty();
            events.add(EventState.propertyAdded(
                    node.internalGetUUID(),
                    node.getPrimaryPath(),
                    prop.getPrimaryPath().getNameElement(),
                    (NodeTypeImpl) node.getPrimaryNodeType(),
                    node.getSession()
            ));
        }
        NodeIterator niter = node.getNodes();
        while (niter.hasNext()) {
            NodeImpl n = (NodeImpl) niter.nextNode();
            recursiveAdd(events, node, n);
        }
    }

    /**
     * Adds a subtree of itemstates as 'removed' to a list of events
     *
     * @param events
     * @param parent
     * @param node
     * @throws RepositoryException
     */
    private void recursiveRemove(List events, NodeImpl parent, NodeImpl node)
            throws RepositoryException {

        events.add(EventState.childNodeRemoved(
                parent.internalGetUUID(),
                parent.getPrimaryPath(),
                node.internalGetUUID(),
                node.getPrimaryPath().getNameElement(),
                (NodeTypeImpl) parent.getPrimaryNodeType(),
                node.getSession()
        ));
        NodeIterator niter = node.getNodes();
        while (niter.hasNext()) {
            NodeImpl n = (NodeImpl) niter.nextNode();
            recursiveRemove(events, node, n);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setItemReferences(InternalVersionItem item, List references) {
        // filter out version storage intern ones
        ArrayList refs = new ArrayList();
        Iterator iter = references.iterator();
        while (iter.hasNext()) {
            PropertyId id = (PropertyId) iter.next();
            if (!hasItem(id.getParentUUID())) {
                refs.add(id);
            }
        }
        internalSetItemReferences(item, refs);
    }

    /**
     * {@inheritDoc}
     */
    public List getItemReferences(InternalVersionItem item) {
        try {
            NodeReferences refs = pMgr.load(new NodeReferencesId(item.getId()));
            return refs.getReferences();
        } catch (ItemStateException e) {
            // ignore
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * {@inheritDoc}
     */
    private void internalSetItemReferences(InternalVersionItem item, List references) {
        try {
            ChangeLog log = new ChangeLog();
            NodeReferences refs = new NodeReferences(new NodeReferencesId(item.getId()));
            refs.addAllReferences(references);
            log.modified(refs);
            pMgr.store(log);
        } catch (ItemStateException e) {
            log.error("Error while storing", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public VirtualItemStateProvider getVirtualItemStateProvider() {
        return this;
    }

    /**
     * invalidates the item
     *
     * @param id
     */
    private void invalidateItem(ItemId id, boolean recursive) {
        ItemState state = (ItemState) items.get(id);
        if (state != null) {
            if (recursive && state instanceof NodeState) {
                NodeState nState = (NodeState) state;
                Iterator iter = nState.getPropertyNames().iterator();
                while (iter.hasNext()) {
                    QName propName = (QName) iter.next();
                    invalidateItem(new PropertyId(nState.getUUID(), propName), false);
                }
                iter = nState.getChildNodeEntries().iterator();
                while (iter.hasNext()) {
                    NodeState.ChildNodeEntry pe = (NodeState.ChildNodeEntry) iter.next();
                    invalidateItem(new NodeId(pe.getUUID()), true);
                }
            }
            state.notifyStateUpdated();
        }
    }


    //--------------------------------------------------------------------------
    //--------------------------------------------------------------------------

    public boolean isVirtualRoot(ItemId id) {
        return id.equals(historyRoot.getState().getId());
    }

    public NodeId getVirtualRootId() {
        return (NodeId) historyRoot.getState().getId();
    }

    public VirtualPropertyState createPropertyState(VirtualNodeState parent, QName name, int type, boolean multiValued) throws RepositoryException {
        throw new IllegalStateException("VersionManager should never create a VirtualPropertyState");
    }

    public VirtualNodeState createNodeState(VirtualNodeState parent, QName name, String uuid, QName nodeTypeName) throws RepositoryException {
        throw new IllegalStateException("VersionManager should never create a VirtualNodeState");
    }

    public boolean setNodeReferences(NodeReferences refs) {
        try {
            InternalVersionItem item = getItem(refs.getTargetId().getUUID());
            setItemReferences(item, refs.getReferences());
            return true;
        } catch (RepositoryException e) {
            log.error("Error while setting references: " + e.toString());
            return false;
        }
    }

    public synchronized ItemState getItemState(ItemId id)
            throws NoSuchItemStateException, ItemStateException {
        ItemState item = (ItemState) items.get(id);
        if (item == null) {
            item = stateMgr.getItemState(id);
            items.put(id, item);

            // special check for successors
            if (item instanceof PropertyState) {
                PropertyState prop = (PropertyState) item;
                if (prop.getName().equals(JCR_SUCCESSORS)) {
                    try {
                        InternalVersion v = getVersion(prop.getParentUUID());
                        if (v != null) {
                            InternalVersion[] succs = v.getSuccessors();
                            InternalValue[] succV = new InternalValue[succs.length];
                            for (int i = 0; i < succs.length; i++) {
                                succV[i] = InternalValue.create(new UUID(succs[i].getId()));
                            }
                            prop.setValues(succV);
                        }
                    } catch (RepositoryException e) {
                        log.warn("Unable to resolve jcr:successors property for " + id);
                    }
                }
            }
        }
        return item;
    }

    public boolean hasItemState(ItemId id) {
        return stateMgr.hasItemState(id);
    }

    public NodeReferences getNodeReferences(NodeReferencesId id)
            throws NoSuchItemStateException, ItemStateException {
        return stateMgr.getNodeReferences(id);
    }

    public boolean hasNodeReferences(NodeReferencesId id) {
        return stateMgr.hasNodeReferences(id);
    }

    public void stateCreated(ItemState created) {
        stateMgr.stateCreated(created);
    }

    public void stateModified(ItemState modified) {
        stateMgr.stateModified(modified);
    }

    public void stateDestroyed(ItemState destroyed) {
        items.remove(destroyed.getId());
        stateMgr.stateDestroyed(destroyed);
    }

    public void stateDiscarded(ItemState discarded) {
        items.remove(discarded.getId());
        stateMgr.stateDiscarded(discarded);
    }
}
