/*
 * Copyright 2004 The Apache Software Foundation.
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

import org.apache.jackrabbit.core.*;
import org.apache.jackrabbit.core.state.*;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.util.uuid.UUID;
import org.apache.jackrabbit.core.virtual.*;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.log4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.PropertyType;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeDef;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.Version;
import java.util.Iterator;

/**
 * This Class implements the session tied version manager. It is also repsonsible
 * for mapping the internal versions to the presentation layer using virtual
 * nodes and items.
 *
 * @author tripod
 * @version $Revision:$, $Date:$
 */
public class VersionManager {

    /**
     * the default logger
     */
    private static Logger log = Logger.getLogger(VersionManager.class);

    /**
     * root path for version storage
     */
    public static final QName VERSION_HISTORY_ROOT_NAME = new QName(NamespaceRegistryImpl.NS_JCR_URI, "versionStorage");
    /**
     * name of the 'jcr:frozenUUID' property
     */
    public static final QName PROPNAME_FROZEN_UUID = new QName(NamespaceRegistryImpl.NS_JCR_URI, "frozenUUID");
    /**
     * name of the 'jcr:frozenPrimaryType' property
     */
    public static final QName PROPNAME_FROZEN_PRIMARY_TYPE = new QName(NamespaceRegistryImpl.NS_JCR_URI, "frozenPrimaryType");
    /**
     * name of the 'jcr:frozenMixinTypes' property
     */
    public static final QName PROPNAME_FROZEN_MIXIN_TYPES = new QName(NamespaceRegistryImpl.NS_JCR_URI, "frozenMixinTypes");
    /**
     * name of the 'jcr:predecessors' property
     */
    public static final QName PROPNAME_PREDECESSORS = new QName(NamespaceRegistryImpl.NS_JCR_URI, "predecessors");
    /**
     * name of the 'jcr:versionLabels' property
     */
    public static final QName PROPNAME_VERSION_LABELS= new QName(NamespaceRegistryImpl.NS_JCR_URI, "versionLabels");
    /**
     * name of the 'jcr:successors' property
     */
    public static final QName PROPNAME_SUCCESSORS = new QName(NamespaceRegistryImpl.NS_JCR_URI, "successors");
    /**
     * name of the 'jcr:isCheckedOut' property
     */
    public static final QName PROPNAME_IS_CHECKED_OUT = new QName(NamespaceRegistryImpl.NS_JCR_URI, "isCheckedOut");
    /**
     * name of the 'jcr:versionHistory' property
     */
    public static final QName PROPNAME_VERSION_HISTORY = new QName(NamespaceRegistryImpl.NS_JCR_URI, "versionHistory");
    /**
     * name of the 'jcr:baseVersion' property
     */
    public static final QName PROPNAME_BASE_VERSION = new QName(NamespaceRegistryImpl.NS_JCR_URI, "baseVersion");
    /**
     * name of the 'jcr:created' property
     */
    public static final QName PROPNAME_CREATED = new QName(NamespaceRegistryImpl.NS_JCR_URI, "created");
    /**
     * the name of the 'jcr:rootVersion' node
     */
    public static final QName NODENAME_ROOTVERSION = new QName(NamespaceRegistryImpl.NS_JCR_URI, "rootVersion");

    /**
     * The version manager of the internal versions
     */
    private final PersistentVersionManager vMgr;

    /**
     * The virtual item manager that exposes the versions to the content
     */
    private DefaultItemStateProvider virtProvider;

    /**
     * The virtual history root
     */
    private VirtualNodeState historyRoot;


    /**
     * @param vMgr
     */
    protected VersionManager(PersistentVersionManager vMgr) {
        this.vMgr = vMgr;
    }

    /**
     * returns the virtual item state provider that exposes the internal versions
     * as items.
     * @param base
     * @return
     */
    public VirtualItemStateProvider getVirtualItemStateProvider(SessionImpl session, ItemStateProvider base) {
        if (virtProvider==null) {
            try {
                // check, if workspace of session has history root
                NodeImpl systemRoot = ((RepositoryImpl) session.getRepository()).getSystemRootNode(session);
                if (!systemRoot.hasNode(VersionManager.VERSION_HISTORY_ROOT_NAME)) {
                    // if not exist, create
                    systemRoot.addNode(VersionManager.VERSION_HISTORY_ROOT_NAME, NodeTypeRegistry.NT_UNSTRUCTURED);
                }
                systemRoot.save();
                String rootId = systemRoot.getNode(VersionManager.VERSION_HISTORY_ROOT_NAME).internalGetUUID();

                virtProvider = new DefaultItemStateProvider(vMgr.getNodeTypeManager());
                // create a duplicate of the version history root name
                NodeState virtRootState = (NodeState) base.getItemState(new NodeId(rootId));
                historyRoot = virtProvider.addOverlay(virtRootState);
                Iterator iter = vMgr.getVersionHistories();
                while (iter.hasNext()) {
                    InternalVersionHistory vh = (InternalVersionHistory) iter.next();
                    mapVersionHistory(vh);
                }
            } catch (Exception e) {
                // todo: better error handling
                log.error("Error while initializing virtual items.", e);
                throw new IllegalStateException(e.toString());
            }
        }
        return virtProvider;
    }

    /**
     * Creates a new version history. This action is needed either when creating
     * a new 'mix:versionable' node or when adding the 'mix:versionalbe' mixin
     * to a node.
     *
     * @param node
     * @return
     * @throws RepositoryException
     */
    public VersionHistory createVersionHistory(NodeImpl node) throws RepositoryException {
        InternalVersionHistory history = vMgr.createVersionHistory(node);
        return (VersionHistory) node.getSession().getNodeByUUID(history.getId());
    }

    /**
     * Returns the base version of the given node. assuming mix:versionable
     *
     * @param node
     * @return
     * @throws RepositoryException
     */
    public Version getBaseVersion(NodeImpl node) throws RepositoryException {
        String histUUID = node.getProperty(VersionManager.PROPNAME_VERSION_HISTORY).getString();
        InternalVersionHistory history = vMgr.getVersionHistory(histUUID);
        InternalVersion version = history.getVersion(node.getProperty(PROPNAME_BASE_VERSION).getString());
        return version == null ? null : (Version) node.getSession().getNodeByUUID(version.getId());
    }

    /**
     * Returns the version history for the given node. assuming mix:versionable
     * and version history set in property
     *
     * @param node
     * @return
     * @throws RepositoryException
     */
    public VersionHistory getVersionHistory(NodeImpl node)
            throws RepositoryException {
        String histUUID = node.getProperty(VersionManager.PROPNAME_VERSION_HISTORY).getString();
        InternalVersionHistory history = vMgr.getVersionHistory(histUUID);
        return (VersionHistory) node.getSession().getNodeByUUID(history.getId());
    }


    /**
     * Creates a new VersionHistoryImpl instance. this is usually called by
     * the {@link ItemManager}.
     * @param session
     * @param state
     * @param def
     * @param itemMgr
     * @param listeners
     * @return
     * @throws RepositoryException
     */
    public VersionHistoryImpl createVersionHistoryInstance(SessionImpl session,
                                                           NodeState state, NodeDef def,
                                                           ItemManager itemMgr,
                                                           ItemLifeCycleListener[] listeners)
            throws RepositoryException {
        if (!state.getNodeTypeName().equals(NodeTypeRegistry.NT_VERSION_HISTORY)) {
            throw new RepositoryException("node not nt:versionhistory");
        }
        NodeId nodeId = (NodeId) state.getId();
        InternalVersionHistory history = vMgr.getVersionHistory(nodeId.getUUID());
        return new VersionHistoryImpl(itemMgr, session, nodeId, state, def, listeners, history);
    }

    /**
     * Creates a new VersionImpl instance. this is usually called by
     * the {@link ItemManager}.
     * @param session
     * @param state
     * @param def
     * @param itemMgr
     * @param listeners
     * @return
     * @throws RepositoryException
     */
    public VersionImpl createVersionInstance(SessionImpl session,
                                             NodeState state, NodeDef def,
                                             ItemManager itemMgr,
                                             ItemLifeCycleListener[] listeners)
            throws RepositoryException {
        if (!state.getNodeTypeName().equals(NodeTypeRegistry.NT_VERSION)) {
            throw new RepositoryException("node not nt:version");
        }
        NodeId nodeId = (NodeId) state.getId();
        String historyId = state.getParentUUID();
        InternalVersionHistory history = vMgr.getVersionHistory(historyId);
        InternalVersion version = history.getVersion(nodeId.getUUID());
        return new VersionImpl(itemMgr, session, nodeId, state, def, listeners, version);
    }

    /**
     * invokes the checkin() on the persistent version manager and remaps the
     * newly created version objects.
     * @param node
     * @return
     * @throws RepositoryException
     */
    public Version checkin(NodeImpl node) throws RepositoryException {
        try {
            InternalVersion version = vMgr.checkin(node);
            vMgr.onVersionHistoryModified(version.getVersionHistory());

            VirtualNodeState vhNode = (VirtualNodeState) virtProvider.getItemState(new NodeId(version.getVersionHistory().getId()));

            // invalidate predecessors 'sucessors' properties
            InternalVersion[] pred = version.getPredecessors();
            for (int i=0; i<pred.length; i++) {
                onVersionModified(pred[i]);
            }
            return (Version) node.getSession().getNodeByUUID(version.getId());
        } catch (NoSuchItemStateException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Called when a internal version has changed its internal structure, and
     * some of the properties has to be remapped to the content.
     * @param v
     * @throws RepositoryException
     */
    protected void onVersionModified(InternalVersion v) throws RepositoryException {
        try {
            VirtualNodeState ns = (VirtualNodeState) virtProvider.getItemState(new NodeId(v.getId()));
            mapDynamicProperties(ns, v);
        } catch (NoSuchItemStateException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Called when a internal version history has changed its internal structure,
     * and the structure has to be remapped to the content.
     * @param vh
     * @throws RepositoryException
     */
    protected void onVersionHistoryModified(InternalVersionHistory vh) throws RepositoryException {
        mapVersionHistory(vh);
    }

    /**
     * Maps the version history and it's versions to the content representation.
     * @param vh
     * @throws RepositoryException
     */
    private void mapVersionHistory(InternalVersionHistory vh)
            throws RepositoryException {
        try {
            String uuid = vh.getId();
            VirtualNodeState parent = historyRoot;
            QName historyNodeName = new QName(NamespaceRegistryImpl.NS_DEFAULT_URI, uuid.substring(0, 2));
            if (!parent.hasChildNodeEntry(historyNodeName)) {
                parent = virtProvider.addNode(parent, historyNodeName, null, NodeTypeRegistry.NT_UNSTRUCTURED, null);
            } else {
                parent = virtProvider.getNode(parent, historyNodeName, 1);
            }
            historyNodeName = new QName(NamespaceRegistryImpl.NS_DEFAULT_URI, uuid.substring(2, 4));
            if (!parent.hasChildNodeEntry(historyNodeName)) {
                parent = virtProvider.addNode(parent, historyNodeName, null, NodeTypeRegistry.NT_UNSTRUCTURED, null);
            } else {
                parent = virtProvider.getNode(parent, historyNodeName, 1);
            }

            historyNodeName = new QName(NamespaceRegistryImpl.NS_DEFAULT_URI, uuid.substring(4));
            VirtualNodeState vhNode;
            if (parent.hasChildNodeEntry(historyNodeName)) {
                vhNode = virtProvider.getNode(parent, historyNodeName, 1);
            } else {
                vhNode = virtProvider.addNode(parent, historyNodeName, vh.getId(), NodeTypeRegistry.NT_VERSION_HISTORY, null);
            }

            // add the versions
            Iterator iter = vh.getVersions();
            while (iter.hasNext()) {
                InternalVersion v = (InternalVersion) iter.next();
                mapVersion(vhNode, v);
            }

        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Maps the internal version to its respective content representation
     * @param vhNode
     * @param version
     * @throws RepositoryException
     */
    private void mapVersion(VirtualNodeState vhNode, InternalVersion version)
            throws RepositoryException {
        try {
            VirtualNodeState vNode;
            if (vhNode.hasChildNodeEntry(version.getName())) {
                vNode = virtProvider.getNode(vhNode, version.getName(), 1);
            } else {
                vNode = virtProvider.addNode(vhNode, version.getName(), version.getId(), NodeTypeRegistry.NT_VERSION, null);
                // initialize the version
                virtProvider.setPropertyValue(vNode, VersionManager.PROPNAME_CREATED, InternalValue.create(version.getCreated()));

                // initialize the primary properties
                InternalFrozenNode fNode = version.getFrozenNode();
                virtProvider.setPropertyValue(vNode, VersionManager.PROPNAME_FROZEN_UUID, InternalValue.create(fNode.getFrozenUUID()));
                virtProvider.setPropertyValue(vNode, VersionManager.PROPNAME_FROZEN_PRIMARY_TYPE, InternalValue.create(fNode.getFrozenPrimaryType()));
                virtProvider.setPropertyValues(vNode, VersionManager.PROPNAME_FROZEN_MIXIN_TYPES, PropertyType.NAME, InternalValue.create(fNode.getFrozenMixinTypes()));
                if (!version.isRootVersion()) {
                    // don't map for root verion
                    mapFrozenNode(vNode, PersistentVersionManager.NODENAME_FROZEN, fNode);
                }
            }

            // map dynamic ones
            mapDynamicProperties(vNode, version);

        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Maps those properties of the internal version to the content that might
     * change over time. such as labels and successors.
     * @param vNode
     * @param version
     * @throws RepositoryException
     */
    private void mapDynamicProperties(VirtualNodeState vNode, InternalVersion version) throws RepositoryException {
        // add version labels
        virtProvider.setPropertyValues(vNode, VersionManager.PROPNAME_VERSION_LABELS, PropertyType.STRING, InternalValue.create(version.internalGetLabels()));

        // add predecessors
        InternalVersion[] preds = version.getPredecessors();
        InternalValue[] predV = new InternalValue[preds.length];
        for (int i=0; i<preds.length; i++) {
            predV[i] = InternalValue.create(new UUID(preds[i].getId()));
        }
        virtProvider.setPropertyValues(vNode, VersionManager.PROPNAME_PREDECESSORS, PropertyType.REFERENCE, predV);

        // add successors
        InternalVersion[] succ= version.getSuccessors();
        InternalValue[] succV = new InternalValue[succ.length];
        for (int i=0; i<succ.length; i++) {
            succV[i] = InternalValue.create(new UUID(succ[i].getId()));
        }
        virtProvider.setPropertyValues(vNode, VersionManager.PROPNAME_SUCCESSORS, PropertyType.REFERENCE, succV);
    }

    /**
     * Maps the frozen content of an internal version to the content
     * representation.
     * @param parent
     * @param name
     * @param fNode
     * @throws RepositoryException
     */
    private void mapFrozenNode(VirtualNodeState parent, QName name, InternalFrozenNode fNode) throws RepositoryException {
        try {
            VirtualNodeState node = virtProvider.addNode(parent, name, null, fNode.getFrozenPrimaryType(), fNode.getFrozenMixinTypes());

            // initialize the content
            PersistentProperty[] props = fNode.getFrozenProperties();
            for (int i=0; i<props.length; i++) {
                virtProvider.setPropertyValues(node, props[i].getName(), props[i].getType(), props[i].getValues(), props[i].isMultiple());
            }
            InternalFreeze[] freezes = fNode.getFrozenChildNodes();
            for (int i=0; i<freezes.length; i++) {
                if (freezes[i] instanceof InternalFrozenVersionHistory) {
                    InternalFrozenVersionHistory vh = (InternalFrozenVersionHistory) freezes[i];
                    VirtualNodeState fChild = virtProvider.addNode(node.getId(), vh.getName(), null, NodeTypeRegistry.NT_FROZEN_VERSIONABLE_CHILD);
                    virtProvider.setPropertyValue(fChild, VersionManager.PROPNAME_VERSION_HISTORY, InternalValue.create(UUID.fromString(vh.getVersionHistoryId())));
                } else { // instance of InternalFrozenNode
                    InternalFrozenNode fn = (InternalFrozenNode) freezes[i];
                    mapFrozenNode(node, fn.getName(), fn);
                }
            }
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        }

    }
}
