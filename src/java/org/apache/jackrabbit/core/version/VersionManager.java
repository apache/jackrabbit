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
import org.apache.jackrabbit.core.nodetype.NodeDefId;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.PropDefId;
import org.apache.jackrabbit.core.state.ItemStateProvider;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.virtual.VirtualItemStateProvider;
import org.apache.log4j.Logger;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDef;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import java.util.HashMap;
import java.util.Iterator;

/**
 * This Class implements the session tied version manager. It is also repsonsible
 * for mapping the internal versions to the presentation layer using virtual
 * nodes and items.
 */
public class VersionManager {

    /**
     * the default logger
     */
    private static Logger log = Logger.getLogger(VersionManager.class);

    /**
     * root path for version storage
     */
    public static final QName NODENAME_HISTORY_ROOT = new QName(NamespaceRegistryImpl.NS_JCR_URI, "versionStorage");
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
    public static final QName PROPNAME_VERSION_LABELS = new QName(NamespaceRegistryImpl.NS_JCR_URI, "versionLabels");
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
    private VersionItemStateProvider virtProvider;

    /**
     * the definition id manager helper class
     */
    private DefinitionIdMgr idMgr;


    /**
     * @param vMgr
     */
    protected VersionManager(PersistentVersionManager vMgr) {
        this.vMgr = vMgr;
    }

    /**
     * returns the virtual item state provider that exposes the internal versions
     * as items.
     *
     * @param base
     * @return
     */
    public synchronized VirtualItemStateProvider getVirtualItemStateProvider(SessionImpl session, ItemStateProvider base) {
        if (virtProvider == null) {
            try {
                // init the definition id mgr
                idMgr = new DefinitionIdMgr(session.getNodeTypeManager());

                session.getNodeTypeManager().getNodeType(NodeTypeRegistry.NT_BASE).getApplicablePropertyDef(ItemImpl.PROPNAME_PRIMARYTYPE, PropertyType.NAME, false).unwrap();
                // check, if workspace of session has history root
                NodeImpl systemRoot = ((RepositoryImpl) session.getRepository()).getSystemRootNode(session);
                if (!systemRoot.hasNode(VersionManager.NODENAME_HISTORY_ROOT)) {
                    // if not exist, create
                    systemRoot.addNode(VersionManager.NODENAME_HISTORY_ROOT, NodeTypeRegistry.NT_UNSTRUCTURED);
                }
                systemRoot.save();
                String rootId = systemRoot.getNode(VersionManager.NODENAME_HISTORY_ROOT).internalGetUUID();

                NodeState virtRootState = (NodeState) base.getItemState(new NodeId(rootId));
                virtProvider = new VersionItemStateProvider(this, rootId, virtRootState.getParentUUID());
            } catch (Exception e) {
                // todo: better error handling
                log.error("Error while initializing virtual items.", e);
                throw new IllegalStateException(e.toString());
            }
        }
        return virtProvider;
    }

    /**
     * returns the node definition id for the given name
     *
     * @param name
     * @return
     */
    public NodeDefId getNodeDefId(QName name) {
        return idMgr.getNodeDefId(name);
    }

    /**
     * returns the property definition id for the given name
     *
     * @param name
     * @return
     */
    public PropDefId getPropDefId(QName name) {
        return idMgr.getPropDefId(name);
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

    //-----------------------------------------------------< internal stuff >---

    /**
     * Checks, if the version history with the given name exists
     *
     * @param name
     * @return
     */
    boolean hasVersionHistory(QName name) {
        // name is uuid of version history
        String id = name.getLocalName();
        return vMgr.hasVersionHistory(id);
    }

    /**
     * Returns the vesion history impl for the given name
     *
     * @param name
     * @return
     * @throws RepositoryException
     */
    InternalVersionHistory getVersionHistory(QName name) throws RepositoryException {
        // name is uuid of version history
        String id = name.getLocalName();
        return vMgr.getVersionHistory(id);
    }

    /**
     * Checks if the version history with the given id exists
     *
     * @param id
     * @return
     */
    boolean hasVersionHistory(String id) {
        return vMgr.hasVersionHistory(id);
    }

    /**
     * Returns the version history with the given id
     *
     * @param id
     * @return
     * @throws RepositoryException
     */
    InternalVersionHistory getVersionHistory(String id) throws RepositoryException {
        return vMgr.getVersionHistory(id);
    }

    /**
     * Returns the number of version histories
     *
     * @return
     * @throws RepositoryException
     */
    int getNumVersionHistories() throws RepositoryException {
        return vMgr.getNumVersionHistories();
    }

    /**
     * Returns an iterator over all {@link InternalVersionHistory}s.
     *
     * @return
     * @throws RepositoryException
     */
    Iterator getVersionHistories() throws RepositoryException {
        return vMgr.getVersionHistories();
    }

    /**
     * Checks if the version with the given id exists
     *
     * @param id
     * @return
     */
    boolean hasVersion(String id) {
        return vMgr.hasVersion(id);
    }

    /**
     * Returns the version with the given id
     *
     * @param id
     * @return
     * @throws RepositoryException
     */
    InternalVersion getVersion(String id) throws RepositoryException {
        return vMgr.getVersion(id);
    }

    /**
     * Creates a new VersionHistoryImpl instance. this is usually called by
     * the {@link ItemManager}.
     *
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
     *
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
     *
     * @param node
     * @return
     * @throws RepositoryException
     */
    public synchronized Version checkin(NodeImpl node) throws RepositoryException {
        InternalVersion version = vMgr.checkin(node);

        vMgr.onVersionHistoryModified(version.getVersionHistory());

        // invalidate predecessors 'sucessors' properties
        InternalVersion[] pred = version.getPredecessors();
        for (int i = 0; i < pred.length; i++) {
            onVersionModified(pred[i]);
        }

        return (Version) node.getSession().getNodeByUUID(version.getId());
    }

    /**
     * Called when a internal version has changed its internal structure, and
     * some of the properties has to be remapped to the content.
     *
     * @param v
     * @throws RepositoryException
     */
    protected synchronized void onVersionModified(InternalVersion v)
            throws RepositoryException {
    }

    /**
     * Called when a internal version history has changed its internal structure,
     * and the structure has to be remapped to the content.
     *
     * @param vh
     * @throws RepositoryException
     */
    protected synchronized void onVersionHistoryModified(InternalVersionHistory vh)
            throws RepositoryException {
    }

    /**
     * Helper class that generates and hold generic definition ids
     */
    private static class DefinitionIdMgr {

        private final HashMap ids = new HashMap();

        private final NodeTypeManagerImpl ntMgr;

        public DefinitionIdMgr(NodeTypeManagerImpl ntMgr)
                throws NoSuchNodeTypeException, RepositoryException {
            this.ntMgr = ntMgr;
            add(VersionManager.NODENAME_ROOTVERSION, NodeTypeRegistry.NT_VERSION, NodeTypeRegistry.NT_VERSION_HISTORY);
            add(VersionManager.NODENAME_HISTORY_ROOT, NodeTypeRegistry.NT_UNSTRUCTURED, NodeTypeRegistry.NT_UNSTRUCTURED);
            add(NodeTypeRegistry.NT_VERSION_HISTORY, NodeTypeRegistry.NT_VERSION_HISTORY, NodeTypeRegistry.NT_UNSTRUCTURED);
            add(ItemImpl.PROPNAME_PRIMARYTYPE, NodeTypeRegistry.NT_BASE, PropertyType.NAME, false);
            add(ItemImpl.PROPNAME_MIXINTYPES, NodeTypeRegistry.NT_BASE, PropertyType.NAME, true);
            add(ItemImpl.PROPNAME_UUID, NodeTypeRegistry.MIX_REFERENCEABLE, PropertyType.STRING, false);
            add(VersionManager.PROPNAME_CREATED, NodeTypeRegistry.NT_VERSION, PropertyType.DATE, false);
            add(VersionManager.PROPNAME_FROZEN_UUID, NodeTypeRegistry.NT_VERSION, PropertyType.STRING, false);
            add(VersionManager.PROPNAME_FROZEN_PRIMARY_TYPE, NodeTypeRegistry.NT_VERSION, PropertyType.NAME, false);
            add(VersionManager.PROPNAME_FROZEN_MIXIN_TYPES, NodeTypeRegistry.NT_VERSION, PropertyType.NAME, true);
            add(VersionManager.PROPNAME_VERSION_LABELS, NodeTypeRegistry.NT_VERSION, PropertyType.STRING, true);
            add(VersionManager.PROPNAME_PREDECESSORS, NodeTypeRegistry.NT_VERSION, PropertyType.REFERENCE, true);
            add(VersionManager.PROPNAME_SUCCESSORS, NodeTypeRegistry.NT_VERSION, PropertyType.REFERENCE, true);
        }

        private void add(QName nodeName, QName nt, QName parentNt)
                throws NoSuchNodeTypeException, RepositoryException {
            NodeDefId id = new NodeDefId(ntMgr.getNodeType(parentNt).getApplicableChildNodeDef(nodeName, nt).unwrap());
            ids.put(nodeName, id);
        }

        private void add(QName propName, QName nt, int type, boolean multivalued)
                throws NoSuchNodeTypeException, RepositoryException {
            PropDefId id = new PropDefId(ntMgr.getNodeType(nt).getApplicablePropertyDef(propName, type, multivalued).unwrap());
            ids.put(propName, id);
        }

        public NodeDefId getNodeDefId(QName name) {
            return (NodeDefId) ids.get(name);
        }

        public PropDefId getPropDefId(QName name) {
            return (PropDefId) ids.get(name);
        }
    }
}
