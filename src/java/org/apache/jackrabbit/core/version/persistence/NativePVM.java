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

import org.apache.log4j.Logger;
import org.apache.jackrabbit.core.version.*;
import org.apache.jackrabbit.core.*;
import org.apache.jackrabbit.core.util.uuid.UUID;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.NodeDefId;
import org.apache.jackrabbit.core.state.PersistentItemStateProvider;
import org.apache.jackrabbit.core.state.PersistentNodeState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.commons.collections.ReferenceMap;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Iterator;

/**
 * This Class implements the persistent part of the versioning. the
 * current implementation uses the 'normal' repository content as storage.
 * <p>
 * although the nodes need to be mapped again virtually in the real content,
 * the persistent nodes use a different structure as exposed later.
 * each versioning element (version history, version, freezes) is stored in a
 * persistent node state, where the name is the original UUID. eg. the name of
 * a persistentnodestate that represents a version, is the UUID of that version.
 * the hierarchy is somewhat similar, thus histories contain versions, contain
 * frozen nodes, etc.
 * <p>
 * on startup, the entire structure is traversed, in order to get a mapping
 * from real to persistent uuids.
 * <p>
 * todo: the persistence is not synchronized yet and could lead to multi-threading issues
 */
public class NativePVM implements PersistentVersionManager {

    /**
     * the logger
     */
    private static Logger log = Logger.getLogger(PersistentVersionManager.class);

    /**
     * root path for version storage
     */
    public static final QName VERSION_HISTORY_ROOT_NAME = new QName(NamespaceRegistryImpl.NS_JCR_URI, "persistentVersionStorage");

    /**
     * name of the 'jcr:historyId' property
     */
    public static final QName PROPNAME_HISTORY_ID = new QName(NamespaceRegistryImpl.NS_JCR_URI, "historyId");
    /**
     * name of the 'jcr:versionId' property
     */
    public static final QName PROPNAME_VERSION_ID = new QName(NamespaceRegistryImpl.NS_JCR_URI, "versionId");
    /**
     * name of the 'jcr:versionName' property
     */
    public static final QName PROPNAME_VERSION_NAME = new QName(NamespaceRegistryImpl.NS_JCR_URI, "versionName");
    /**
     * name of the 'jcr:versionLabels' node
     */
    public static final QName NODENAME_VERSION_LABELS = new QName(NamespaceRegistryImpl.NS_JCR_URI, "versionLabels");
    /**
     * name of the 'jcr:name' property
     */
    public static final QName PROPNAME_NAME = new QName(NamespaceRegistryImpl.NS_JCR_URI, "name");
    /**
     * name of the 'jcr:version' property
     */
    public static final QName PROPNAME_VERSION = new QName(NamespaceRegistryImpl.NS_JCR_URI, "version");

    /**
     * the id of the persisten root node
     */
    private static final NodeId PERSISTENT_ROOT_ID = new NodeId("faceface-ab3b-48a9-b31b-e7d0a9c1c3b1");

    /**
     * The nodetype name of a persistent version
     */
    protected static final QName NT_REP_VERSION = new QName(NamespaceRegistryImpl.NS_REP_URI, "version");

    /**
     * The nodetype name of a presistent version history
     */
    protected static final QName NT_REP_VERSION_HISTORY = new QName(NamespaceRegistryImpl.NS_REP_URI, "versionHistory");

    /**
     * the nodetype name of a persistent frozen node
     */
    protected static final QName NT_REP_FROZEN = new QName(NamespaceRegistryImpl.NS_REP_URI, "frozen");

    /**
     * the nodetype name of a persistent frozen history
     */
    protected static final QName NT_REP_FROZEN_HISTORY = new QName(NamespaceRegistryImpl.NS_REP_URI, "frozenVersionHistory");

    /**
     * the persistent root node of the version histories
     */
    private final PersistentNode historyRoot;

    /**
     * the state manager for the version storage
     */
    private PersistentItemStateProvider stateMgr;

    /**
     * the nodetype manager for the version storage
     */
    private NodeTypeManagerImpl ntMgr;

    /**
     * mapping from virtual uuids to persistent ids of the persistent nodes
     * key=externalId, value=PersistentId
     */
    private HashMap idsByExternal = new HashMap();

    /**
     * mapping from virtual uuids to persistent ids of the persistent nodes
     * key=internalId, value=PersistentId
     */
    private HashMap idsByInternal= new HashMap();

    /**
     * list of histories for fast retrieval
     */
    private LinkedList histories = new LinkedList();

    /**
     * the version histories. key=uuid, value=version history
     */
    private Map items = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT);

    /**
     * Creates a new PersistentVersionManager.
     *
     * @param session
     * @throws javax.jcr.RepositoryException
     */
    public NativePVM(SessionImpl session) throws RepositoryException {
        this.stateMgr = ((WorkspaceImpl) session.getWorkspace()).getPersistentStateManager();
        this.ntMgr = session.getNodeTypeManager();

        try {
            NodeImpl systemRoot = ((RepositoryImpl) session.getRepository()).getSystemRootNode(session);
            // enable this to make the persistence storage visible
            if (true) {
                // check for versionhistory root
                if (!systemRoot.hasNode(VERSION_HISTORY_ROOT_NAME)) {
                    // if not exist, create
                    systemRoot.addNode(VERSION_HISTORY_ROOT_NAME, NodeTypeRegistry.NT_UNSTRUCTURED);
                    systemRoot.save();
                }
                PersistentNodeState nodeState = (PersistentNodeState) stateMgr.getItemState(new NodeId(systemRoot.getNode(VERSION_HISTORY_ROOT_NAME).internalGetUUID()));
                historyRoot = new PersistentNode(stateMgr, ntMgr, nodeState);
            } else {
                if (!stateMgr.hasItemState(PERSISTENT_ROOT_ID)) {
                    PersistentNodeState nodeState = stateMgr.createNodeState(PERSISTENT_ROOT_ID.getUUID(), NodeTypeRegistry.NT_UNSTRUCTURED, null);
                    nodeState.setDefinitionId(new NodeDefId(ntMgr.getRootNodeDefinition().unwrap()));
                    nodeState.store();
                    historyRoot = new PersistentNode(stateMgr, ntMgr, nodeState);
                } else {
                    PersistentNodeState nodeState = (PersistentNodeState) stateMgr.getItemState(PERSISTENT_ROOT_ID);
                    historyRoot = new PersistentNode(stateMgr, ntMgr, nodeState);
                }
            }
            initVirtualIds(historyRoot.getState());
            log.info("loaded " + idsByExternal.size() + " virtual ids.");
        } catch (ItemStateException e) {
            throw new RepositoryException("Unable to initialize PersistentVersionManager: " + e.toString());
        }
    }

    /**
     * initializes the internal item ids
     * @param parent
     * @throws RepositoryException
     * @throws ItemStateException
     */
    private void initVirtualIds(NodeState parent)
            throws RepositoryException, ItemStateException {

        Iterator iter = parent.getChildNodeEntries().iterator();
        while (iter.hasNext()) {
            NodeState.ChildNodeEntry entry = (NodeState.ChildNodeEntry) iter.next();
            String realUUID = entry.getName().getLocalName();
            initVirtualIds(realUUID, (NodeState) stateMgr.getItemState(new NodeId(entry.getUUID())));
        }
    }

    /**
     * initializes the internal item ids
     * @param realUUID
     * @param state
     * @throws ItemStateException
     * @throws RepositoryException
     */
    private void initVirtualIds(String realUUID, NodeState state)
            throws ItemStateException, RepositoryException {
        PersistentId id = new PersistentId(realUUID, state);
        if (id.type != PersistentId.TYPE_UNDEFINED) {
            synchronized(idsByExternal) {
                idsByExternal.put(id.externalId, id);
                idsByInternal.put(id.internalId, id);
            }
            if (id.type == PersistentId.TYPE_HISTORY) {
                histories.add(id.externalId);
            }
        }
        initVirtualIds(state);
    }

    /**
     * returns the persistentid for a given external uuid
     * @param uuid
     * @return
     */
    private PersistentId getIdByExternal(String uuid) {
        synchronized(idsByExternal) {
            return (PersistentId) idsByExternal.get(uuid);
        }
    }

    /**
     * returns the persustentid for a given internal uuid
     * @param uuid
     * @return
     */
    private PersistentId getIdByInternal(String uuid) {
        return (PersistentId) idsByInternal.get(uuid);
    }

    /**
     * returns the persustentid for a give internal uuid and item type
     * @param uuid
     * @param type
     * @return
     */
    private PersistentId getIdByExternal(String uuid, int type) {
        synchronized(idsByExternal) {
            PersistentId id = (PersistentId) idsByExternal.get(uuid);
            return id != null && id.type == type ? id : null;
        }
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

        // create deep path
        String uuid = UUID.randomUUID().toString();
        PersistentNode root = historyRoot;
        for (int i=0; i<3; i++) {
            QName name = new QName(NamespaceRegistryImpl.NS_DEFAULT_URI, uuid.substring(i*2, i*2+2));
            if (!root.hasNode(name)) {
                root.addNode(name, NodeTypeRegistry.NT_UNSTRUCTURED);
                root.store();
            }
            root = root.getNode(name, 1);
        }
        QName historyNodeName = new QName(NamespaceRegistryImpl.NS_DEFAULT_URI, uuid);

        // create new history node in the persistent state
        InternalVersionHistoryImpl hist = InternalVersionHistoryImpl.create(this, root, uuid, historyNodeName, node);
        try {
            initVirtualIds(hist.getId(), hist.getNode().getState());
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        }

        log.info("Created new version history " + uuid + ". NumHistories=" + histories.size());
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

        PersistentId pid = getIdByExternal(histId, PersistentId.TYPE_HISTORY);
        return pid==null ? null : (InternalVersionHistory) getItem(pid);
    }

    /**
     * Checks if the versionhistory for the given id exists
     *
     * @param histId
     * @return
     */
    public boolean hasVersionHistory(String histId) {
        return getIdByExternal(histId, PersistentId.TYPE_HISTORY) != null;
    }

    /**
     * returns an iterator over the external ids of the version histories
     *
     * @return
     * @throws RepositoryException
     */
    public Iterator getVersionHistoryIds() throws RepositoryException {
        return histories.iterator();
    }

    /**
     * returns the number of version histories
     *
     * @return
     * @throws RepositoryException
     */
    public int getNumVersionHistories() throws RepositoryException {
        return histories.size();
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

        PersistentId pid = getIdByExternal(versionId, PersistentId.TYPE_VERSION);
        return pid==null ? null : (InternalVersion) getItem(pid);
    }

    /**
     * Checks if the version with the given id exists
     *
     * @param versionId
     * @return
     */
    public boolean hasVersion(String versionId) {
        return getIdByExternal(versionId, PersistentId.TYPE_VERSION) != null;
    }

    /**
     * checks, if the item with the given external id exists
     * @param externalId
     * @return
     */
    public boolean hasItem(String externalId) {
        return getIdByExternal(externalId) != null;
    }

    /**
     * returns the item referred by the external id
     * @param externalId
     * @return
     * @throws RepositoryException
     */
    public InternalVersionItem getItemByExternal(String externalId)
            throws RepositoryException {
        PersistentId pid = getIdByExternal(externalId);
        return pid==null ? null : getItem(pid);
    }

    /**
     * returns the item referred by the internal id
     * @param internalId
     * @return
     * @throws RepositoryException
     */
    public InternalVersionItem getItemByInternal(String internalId)
            throws RepositoryException {
        PersistentId pid = getIdByInternal(internalId);
        return pid==null ? null : getItem(pid);
    }

    /**
     * returns the item with the given persistent id
     * @param pid
     * @return
     * @throws RepositoryException
     */
    private InternalVersionItem getItem(PersistentId pid)
            throws RepositoryException {

        InternalVersionItem item =(InternalVersionItem) items.get(pid);
        if (item==null) {
            PersistentNode pNode = historyRoot.getNodeByUUID(pid.internalId);
            if (pNode!=null) {
                InternalVersionItem parent = getItemByInternal(pNode.getParentUUID());
                if (pid.type == PersistentId.TYPE_FROZEN) {
                    item = new InternalFrozenNodeImpl(this, pNode, pid.externalId, parent);
                } else if (pid.type == PersistentId.TYPE_FROZEN_HISTORY) {
                    item = new InternalFrozenVHImpl(this, pNode, pid.externalId, parent);
                } else if (pid.type == PersistentId.TYPE_VERSION) {
                    item = ((InternalVersionHistory) parent).getVersion(pid.externalId);
                } else if (pid.type == PersistentId.TYPE_HISTORY) {
                    item = new InternalVersionHistoryImpl(this, pNode);
                } else {
                    //return null;
                }
            }
            if (item!=null) {
                items.put(pid, item);
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
        String histUUID = node.getProperty(VersionManager.PROPNAME_VERSION_HISTORY).getString();
        InternalVersionHistoryImpl history = (InternalVersionHistoryImpl) getVersionHistory(histUUID);

        // 0. resolve the predecessors
        Value[] values = node.getProperty(VersionManager.PROPNAME_PREDECESSORS).getValues();
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

        InternalVersionImpl v = history.checkin(new QName("", versionName), node);
        try {
            initVirtualIds(v.getId(), v.getNode().getState());
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        }
        return v;
    }

    /**
     * Helper class for persistent items
     */
    public static final class PersistentId {

        private static final int TYPE_UNDEFINED = 0;
        private static final int TYPE_HISTORY = 1;
        private static final int TYPE_VERSION = 2;
        private static final int TYPE_FROZEN = 3;
        private static final int TYPE_FROZEN_HISTORY = 4;

        /** the type of the persistent node */
        private final int type;

        /** the persistent uuid of the node */
        private final String externalId;

        /** the persistent uuid of the node */
        private final String internalId;

        public PersistentId(int type, String external, String internal) {
            this.type = type;
            this.internalId = internal;
            this.externalId = external;
        }

        public PersistentId(String external, NodeState state) {
            this.internalId = state.getUUID();
            if (state.getNodeTypeName().equals(NT_REP_VERSION)) {
                this.externalId = external;
                type = TYPE_VERSION;
            } else if (state.getNodeTypeName().equals(NT_REP_VERSION_HISTORY)) {
                this.externalId = external;
                type = TYPE_HISTORY;
            } else if (state.getNodeTypeName().equals(NT_REP_FROZEN)) {
                // ignore given externalid, and generate new one
                this.externalId = UUID.randomUUID().toString();
                type = TYPE_FROZEN;
            } else if (state.getNodeTypeName().equals(NT_REP_FROZEN_HISTORY)) {
                // ignore given externalid, and generate new one
                this.externalId = UUID.randomUUID().toString();
                type = TYPE_FROZEN_HISTORY;
            } else {
                // ignore given externalid, and generate new one
                this.externalId = UUID.randomUUID().toString();
                type = TYPE_UNDEFINED;
            }
        }

        public boolean isVersion() {
            return type == TYPE_VERSION;
        }

        public boolean isHistory() {
            return type == TYPE_HISTORY;
        }

        public boolean isFrozen() {
            return type == TYPE_FROZEN;
        }
    }

}
