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
package org.apache.jackrabbit.core.version.persistence;

import org.apache.commons.collections.ReferenceMap;
import org.apache.jackrabbit.core.Constants;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.LocalItemStateManager;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeReferencesId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PersistenceManager;
import org.apache.jackrabbit.core.state.SharedItemStateManager;
import org.apache.jackrabbit.core.state.UpdatableItemStateManager;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.util.uuid.UUID;
import org.apache.jackrabbit.core.version.InternalVersion;
import org.apache.jackrabbit.core.version.InternalVersionHistory;
import org.apache.jackrabbit.core.version.InternalVersionItem;
import org.apache.jackrabbit.core.version.PersistentVersionManager;
import org.apache.log4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This Class implements the persistent part of the versioning. the
 * current implementation uses the 'normal' repository content as storage.
 * <p/>
 * although the nodes need to be mapped again virtually in the real content,
 * the persistent nodes use a different structure as exposed later.
 * each versioning element (version history, version, freezes) is stored in a
 * persistent node state, where the name is the original UUID. eg. the name of
 * a persistentnodestate that represents a version, is the UUID of that version.
 * the hierarchy is somewhat similar, thus histories contain versions, contain
 * frozen nodes, etc.
 * <p/>
 * on startup, the entire structure is traversed, in order to get a mapping
 * from real to persistent uuids.
 * <p/>
 * todo: the persistence is not synchronized yet and could lead to multi-threading issues
 */
public class NativePVM implements PersistentVersionManager, Constants {

    /**
     * the logger
     */
    private static Logger log = Logger.getLogger(NativePVM.class);

    /**
     * root path for version storage
     */
    public static final QName VERSION_HISTORY_ROOT_NAME = new QName(NS_JCR_URI, "persistentVersionStorage");

    /**
     * name of the 'jcr:versionableId' property
     */
    public static final QName PROPNAME_VERSIONABLE_ID = new QName(NS_JCR_URI, "versionableId");

    /**
     * name of the 'jcr:versionLabels' node
     */
    public static final QName NODENAME_VERSION_LABELS = new QName(NS_JCR_URI, "versionLabels");
    /**
     * name of the 'jcr:name' property
     */
    public static final QName PROPNAME_NAME = new QName(NS_JCR_URI, "name");
    /**
     * name of the 'jcr:version' property
     */
    public static final QName PROPNAME_VERSION = new QName(NS_JCR_URI, "version");

    /**
     * the id of the persisten root node
     */
    private static final NodeId PERSISTENT_ROOT_ID = new NodeId("faceface-ab3b-48a9-b31b-e7d0a9c1c3b1");

    /**
     * The nodetype name of a persistent version
     */
    protected static final QName NT_REP_VERSION = new QName(NS_REP_URI, "version");

    /**
     * The nodetype name of a presistent version history
     */
    protected static final QName NT_REP_VERSION_HISTORY = new QName(NS_REP_URI, "versionHistory");

    /**
     * the nodetype name of a persistent frozen node
     */
    protected static final QName NT_REP_FROZEN = new QName(NS_REP_URI, "frozen");

    /**
     * the nodetype name of a persistent frozen history
     */
    protected static final QName NT_REP_FROZEN_HISTORY = new QName(NS_REP_URI, "frozenVersionHistory");

    /**
     * the persistent root node of the version histories
     */
    private final PersistentNode historyRoot;

    /**
     * the state manager for the version storage
     */
    private LocalItemStateManager stateMgr;

    /**
     * the persistence manager
     */
    private PersistenceManager pMgr;

    /**
     * map of versioned uuids. key=versionedUUID, value=externalId
     */
    private HashMap versionedUUIDs = new HashMap();

    /**
     * the version histories. key=uuid, value=version history
     */
    private Map items = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT);

    /**
     * Creates a new NativePVM.
     *
     * @param pMgr
     * @param ntReg
     * @throws javax.jcr.RepositoryException
     */
    public NativePVM(PersistenceManager pMgr, NodeTypeRegistry ntReg) throws RepositoryException {
        try {
            this.pMgr = pMgr;
            SharedItemStateManager sharedStateMgr = new SharedItemStateManager(pMgr, PERSISTENT_ROOT_ID.getUUID(), ntReg);
            stateMgr = new LocalItemStateManager(sharedStateMgr, null);
            NodeState nodeState = (NodeState) stateMgr.getItemState(PERSISTENT_ROOT_ID);
            historyRoot = new PersistentNode(stateMgr, nodeState);
            initVersionedUUIDs(nodeState);
        } catch (ItemStateException e) {
            throw new RepositoryException("Unable to initialize PersistentVersionManager: " + e.toString(), e);
        }
    }

    /**
     * initiealies the map of versioned uuids
     * @param state
     * @throws ItemStateException
     */
    private void initVersionedUUIDs(NodeState state) throws ItemStateException {

        if (state.getNodeTypeName().equals(NT_REP_VERSION_HISTORY)) {
            PropertyState ps = (PropertyState) stateMgr.getItemState(new PropertyId(state.getUUID(), PROPNAME_VERSIONABLE_ID));
            String vid = (String) ps.getValues()[0].internalValue();
            versionedUUIDs.put(vid, state.getUUID());
            return;
        }
        Iterator iter = state.getChildNodeEntries().iterator();
        while (iter.hasNext()) {
            NodeState.ChildNodeEntry entry = (NodeState.ChildNodeEntry) iter.next();
            initVersionedUUIDs((NodeState) stateMgr.getItemState(new NodeId(entry.getUUID())));
        }
    }

    /**
     * Close this persistence version manager. After having closed a persistence
     * manager, further operations on this object are treated as illegal
     * and throw
     *
     * @throws Exception if an error occurs
     */
    public void close() throws Exception {
        // @todo check proper shutdown sequence
        this.pMgr.close();
        this.stateMgr = null;
    }

    /**
     * Retrusn the version history that corresponds to the versionable node of
     * the given uuid.
     *
     * @param uuid
     * @return
     */
    private InternalVersionHistoryImpl getHistoryByVersionableUUID(String uuid)
            throws RepositoryException {
        String id = (String) versionedUUIDs.get(uuid);
        return id == null ? null : (InternalVersionHistoryImpl) getVersionHistory(id);
    }

    /**
     * Creates a new Version History.
     *
     * @param node the node for which the version history is to be initialized
     * @return the newly created version history.
     * @throws RepositoryException
     */
    public InternalVersionHistory createVersionHistory(NodeImpl node)
            throws RepositoryException {

        // check if version history for that node already exists
        InternalVersionHistoryImpl hist = getHistoryByVersionableUUID(node.internalGetUUID());
        if (hist != null) {
            return hist;
        }

        boolean succeeded = false;
        try {
            stateMgr.edit();

            // create deep path
            String uuid = UUID.randomUUID().toString();
            PersistentNode root = historyRoot;
            for (int i = 0; i < 3; i++) {
                QName name = new QName(NS_DEFAULT_URI, uuid.substring(i * 2, i * 2 + 2));
                if (!root.hasNode(name)) {
                    root.addNode(name, NT_UNSTRUCTURED, null);
                    root.store();
                }
                root = root.getNode(name, 1);
            }
            QName historyNodeName = new QName(NS_DEFAULT_URI, uuid);

            // create new history node in the persistent state
            hist = InternalVersionHistoryImpl.create(this, root, uuid, historyNodeName, node);

            // end update
            stateMgr.update();
            succeeded = true;

        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        } finally {
            if (!succeeded) {
                // update operation failed, cancel all modifications
                stateMgr.cancel();
            }
        }

        versionedUUIDs.put(hist.getVersionableUUID(), hist.getId());
        
        log.info("Created new version history " + hist.getId() + " for " + node.safeGetJCRPath() + ". NumHistories=" + versionedUUIDs.size());
        return hist;
    }

    /**
     * returns the internal version history for the id
     *
     * @param histId the id of the history
     * @return
     * @throws RepositoryException
     */
    public InternalVersionHistory getVersionHistory(String histId)
            throws RepositoryException {

        return (InternalVersionHistory) getItem(histId);
    }

    /**
     * Checks if the versionhistory for the given id exists
     *
     * @param histId
     * @return
     */
    public boolean hasVersionHistory(String histId) {
        return hasItem(histId);
    }

    /**
     * returns an iterator over the external ids of the version histories
     *
     * @return
     * @throws RepositoryException
     */
    public Iterator getVersionHistoryIds() throws RepositoryException {
        return versionedUUIDs.values().iterator();
    }

    /**
     * returns the number of version histories
     *
     * @return
     * @throws RepositoryException
     */
    public int getNumVersionHistories() throws RepositoryException {
        return versionedUUIDs.size();
    }

    /**
     * returns the internal version for the id
     *
     * @param versionId
     * @return
     * @throws RepositoryException
     */
    public InternalVersion getVersion(String histId, String versionId)
            throws RepositoryException {
        InternalVersionHistory history = getVersionHistory(histId);
        return history.getVersion(versionId);
    }

    /**
     * returns the version with the given id
     *
     * @param versionId
     * @return
     * @throws RepositoryException
     */
    public InternalVersion getVersion(String versionId)
            throws RepositoryException {

        return (InternalVersion) getItem(versionId);
    }

    /**
     * Checks if the version with the given id exists
     *
     * @param versionId
     * @return
     */
    public boolean hasVersion(String versionId) {
        return hasItem(versionId);
    }

    /**
     * checks, if the item with the given external id exists
     *
     * @param uuid
     * @return
     */
    public boolean hasItem(String uuid) {
        NodeId id = new NodeId(uuid);
        if (items.containsKey(id)) {
            return true;
        }
        return stateMgr.hasItemState(id);
    }

    /**
     * returns the item with the given persistent id
     *
     * @param uuid
     * @return
     * @throws RepositoryException
     */
    public InternalVersionItem getItem(String uuid) throws RepositoryException {
        NodeId id = new NodeId(uuid);
        InternalVersionItem item = (InternalVersionItem) items.get(id);
        if (item == null) {
            try {
                NodeState state = (NodeState) stateMgr.getItemState(id);
                PersistentNode pNode = new PersistentNode(stateMgr, state);
                InternalVersionItem parent = pNode.getParentUUID() == null ? null : getItem(pNode.getParentUUID());
                QName ntName = state.getNodeTypeName();
                if (ntName.equals(NT_REP_FROZEN)) {
                    item = new InternalFrozenNodeImpl(this, pNode, parent);
                } else if (ntName.equals(NT_REP_FROZEN_HISTORY)) {
                    item = new InternalFrozenVHImpl(this, pNode, parent);
                } else if (ntName.equals(NT_REP_VERSION)) {
                    item = ((InternalVersionHistory) parent).getVersion(uuid);
                } else if (ntName.equals(NT_REP_VERSION_HISTORY)) {
                    item = new InternalVersionHistoryImpl(this, pNode);
                } else {
                    //return null;
                }
                if (item != null) {
                    items.put(id, item);
                }
            } catch (ItemStateException e) {
                // ignore
            }
        }
        return item;
    }

    /**
     * Checks in a node
     *
     * @param node
     * @return
     * @throws RepositoryException
     * @see javax.jcr.Node#checkin()
     */
    public InternalVersion checkin(NodeImpl node) throws RepositoryException {
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
                    versionName = pos < 0 ? "1.0" : versionName.substring(0, pos + 1) + (Integer.parseInt(versionName.substring(pos + 1)) + 1);
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

        boolean succeeded = false;
        try {
            stateMgr.edit();
            InternalVersionImpl v = history.checkin(new QName("", versionName), node);
            stateMgr.update();
            succeeded = true;

            // notify listeners
            history.notifyModifed();

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
     * {@inheritDoc}
     */
    public UpdatableItemStateManager getItemStateMgr() {
        return stateMgr;
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
    public void setItemReferences(InternalVersionItem item, List references) {
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

}
