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
import org.apache.jackrabbit.core.version.*;
import org.apache.jackrabbit.core.util.uuid.UUID;
import org.apache.jackrabbit.core.nodetype.*;
import org.apache.jackrabbit.core.state.*;
import org.apache.jackrabbit.core.virtual.*;
import org.apache.log4j.Logger;
import org.apache.commons.collections.ReferenceMap;

import javax.jcr.RepositoryException;
import javax.jcr.PropertyType;
import javax.jcr.nodetype.ConstraintViolationException;
import java.util.Map;
import java.util.HashSet;

/**
 * This Class implements a virtual item state provider, in order to expose the
 * versions to the version storage.
 */
public class VersionItemStateProvider implements VirtualItemStateProvider {
    /**
     * the default logger
     */
    private static Logger log = Logger.getLogger(VersionItemStateProvider.class);
    /**
     * the root node
     */
    private final HistoryRootNodeState root;
    /**
     * the version manager
     */
    private final VersionManager vMgr;
    /**
     * the node type manager
     */
    private final NodeTypeManagerImpl ntMgr;
    /**
     * the version histories. key=ItemId, value=ItemState
     */
    private Map items = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT);
    /**
     * node def id for a unstructured node state
     */
    private NodeDefId NDEF_UNSTRUCTURED;
    /**
     * node def id for a version node state
     */
    private NodeDefId NDEF_VERSION;
    /**
     * node def id ofr a version history node state
     */
    private NodeDefId NDEF_VERSION_HISTORY;

    /**
     * creates a new version item state provide
     * @param vMgr
     * @param rootId
     * @param parentId
     * @throws RepositoryException
     */
    public VersionItemStateProvider(VersionManager vMgr, NodeTypeManagerImpl ntMgr, String rootId, String parentId)  throws RepositoryException {
        this.vMgr = vMgr;
        this.ntMgr = ntMgr;
        NDEF_UNSTRUCTURED = new NodeDefId(getNodeTypeManager().getNodeType(NodeTypeRegistry.NT_UNSTRUCTURED).getApplicableChildNodeDef(VersionManager.NODENAME_ROOTVERSION, NodeTypeRegistry.NT_UNSTRUCTURED).unwrap());
        NDEF_VERSION = new NodeDefId(getNodeTypeManager().getNodeType(NodeTypeRegistry.NT_VERSION_HISTORY).getApplicableChildNodeDef(VersionManager.NODENAME_ROOTVERSION, NodeTypeRegistry.NT_VERSION).unwrap());
        NDEF_VERSION_HISTORY = new NodeDefId(getNodeTypeManager().getNodeType(NodeTypeRegistry.NT_UNSTRUCTURED).getApplicableChildNodeDef(VersionManager.NODENAME_ROOTVERSION, NodeTypeRegistry.NT_VERSION_HISTORY).unwrap());

        this.root = new HistoryRootNodeState(this, vMgr, parentId, rootId);
        this.root.setDefinitionId(NDEF_UNSTRUCTURED);
    }

    //--------------------------------------------------< ItemStateProvider >---

    /**
     * @see ItemStateProvider#hasItemState(org.apache.jackrabbit.core.ItemId)
     */
    public boolean hasItemState(ItemId id) {

        // check cache
        if (items.containsKey(id)) {
            return true;
        } else if (id instanceof NodeId) {
            return hasNodeState((NodeId) id);
        } else {
            return hasPropertyState((PropertyId) id);
        }
    }

    /**
     * @see ItemStateProvider#getItemState(org.apache.jackrabbit.core.ItemId)
     */
    public ItemState getItemState(ItemId id)
            throws NoSuchItemStateException, ItemStateException {

        ItemState state = (ItemState) items.get(id);
        if (state==null) {
            if (id instanceof NodeId) {
                state = getNodeState((NodeId) id);
            } else {
                state = getPropertyState((PropertyId) id);
            }
            // add state to cache
            items.put(id, state);
            log.info("item added to cache. size=" + items.size());
        }
        return state;
    }

    /**
     * virtual item state provider do not have attics.
     *
     * @throws NoSuchItemStateException always
     */
    public ItemState getItemStateInAttic(ItemId id) throws NoSuchItemStateException {
        // never has states in attic
        throw new NoSuchItemStateException(id.toString());
    }

    /**
     * virtual item state provider do not have attics.
     *
     * @return <code>false</code>
     */
    public boolean hasItemStateInAttic(ItemId id) {
        // never has states in attic
        return false;
    }

    //-------------------------------------------< VirtualItemStateProvider >---

    /**
     * @see VirtualItemStateProvider#isVirtualRoot(org.apache.jackrabbit.core.ItemId)
     */
    public boolean isVirtualRoot(ItemId id) {
        return id.equals(root.getId());
    }

    /**
     * @see org.apache.jackrabbit.core.virtual.VirtualItemStateProvider#getVirtualRootId()
     */
    public NodeId getVirtualRootId() {
        return (NodeId) root.getId();
    }

    /**
     * @see VirtualItemStateProvider#hasNodeState(NodeId)
     */
    public boolean hasNodeState(NodeId id) {
        if (id.equals(root.getId())) {
            return true;
        }
        return vMgr.hasItem(id.getUUID());
    }

    /**
     * @see VirtualItemStateProvider#getNodeState(org.apache.jackrabbit.core.NodeId)
     */
    public VirtualNodeState getNodeState(NodeId id)
            throws NoSuchItemStateException, ItemStateException {

        // check if root
        if (id.equals(root.getId())) {
            return root;
        }

        try {
            InternalVersionItem vi = vMgr.getItem(id.getUUID());
            if (vi instanceof InternalVersionHistory) {
                VersionHistoryNodeState ns = new VersionHistoryNodeState(this, (InternalVersionHistory) vi, root.getUUID());
                ns.setDefinitionId(NDEF_VERSION_HISTORY);
                return ns;

            } else if (vi instanceof InternalVersion) {
                InternalVersion v = (InternalVersion) vi;
                VersionNodeState ns = new VersionNodeState(this, v, vi.getParent().getId());
                ns.setDefinitionId(NDEF_VERSION);
                ns.setPropertyValue(VersionManager.PROPNAME_CREATED, InternalValue.create(v.getCreated()));
                ns.setPropertyValue(VersionManager.PROPNAME_FROZEN_UUID, InternalValue.create(v.getFrozenNode().getFrozenUUID()));
                ns.setPropertyValue(VersionManager.PROPNAME_FROZEN_PRIMARY_TYPE, InternalValue.create(v.getFrozenNode().getFrozenPrimaryType()));
                ns.setPropertyValues(VersionManager.PROPNAME_FROZEN_MIXIN_TYPES, PropertyType.NAME, InternalValue.create(v.getFrozenNode().getFrozenMixinTypes()));
                ns.setPropertyValues(VersionManager.PROPNAME_VERSION_LABELS, PropertyType.STRING, InternalValue.create(v.getLabels()));
                ns.setPropertyValues(VersionManager.PROPNAME_PREDECESSORS, PropertyType.REFERENCE, new InternalValue[0]);
                ns.setPropertyValues(VersionManager.PROPNAME_SUCCESSORS, PropertyType.REFERENCE, new InternalValue[0]);
                return ns;

            } else if (vi instanceof InternalFrozenNode) {
                InternalFrozenNode fn = (InternalFrozenNode) vi;
                VirtualNodeState parent = getNodeState(new NodeId(fn.getParent().getId()));
                VirtualNodeState state = createNodeState(
                                parent,
                                VersionManager.NODENAME_FROZEN,
                                id.getUUID(),
                                fn.getFrozenPrimaryType());
                mapFrozenNode(state, fn);
                return state;

            } else if (vi instanceof InternalFrozenVersionHistory) {
                InternalFrozenVersionHistory fn = (InternalFrozenVersionHistory) vi;
                VirtualNodeState parent = getNodeState(new NodeId(fn.getParent().getId()));
                VirtualNodeState state = createNodeState(
                                parent,
                                VersionManager.NODENAME_FROZEN,
                                id.getUUID(),
                                NodeTypeRegistry.NT_FROZEN_VERSIONABLE_CHILD);
                mapFrozenNode(state, fn);
                return state;
            }
        } catch (RepositoryException e) {
            log.error("Unable to check for item:" + e.toString());
            throw new ItemStateException(e);
        }

        // not found, throw
        throw new NoSuchItemStateException(id.toString());
    }

    /**
     * @see VirtualItemStateProvider#hasPropertyState(org.apache.jackrabbit.core.PropertyId)
     */
    public boolean hasPropertyState(PropertyId id) {

        try {
            // get parent state
            NodeState parent = getNodeState(new NodeId(id.getParentUUID()));

            // handle some default prop states
            if (parent instanceof VirtualNodeState) {
                return ((VirtualNodeState) parent).hasPropertyEntry(id.getName());
            }
        } catch (ItemStateException e) {
            // ignore
        }
        return false;
    }

    /**
     * @see VirtualItemStateProvider#getPropertyState(org.apache.jackrabbit.core.PropertyId)
     */
    public VirtualPropertyState getPropertyState(PropertyId id)
            throws NoSuchItemStateException, ItemStateException {

        // get parent state
        NodeState parent = getNodeState(new NodeId(id.getParentUUID()));

        // handle some default prop states
        if (parent instanceof VirtualNodeState) {
            return ((VirtualNodeState) parent).getProperty(id.getName());
        }
        throw new NoSuchItemStateException(id.toString());
    }

    /**
     * @see VirtualItemStateProvider#createPropertyState(org.apache.jackrabbit.core.virtual.VirtualNodeState, org.apache.jackrabbit.core.QName, int, boolean)
     */
    public VirtualPropertyState createPropertyState(VirtualNodeState parent,
                                                    QName name, int type,
                                                    boolean multiValued)
            throws RepositoryException {
        PropertyDefImpl def = getApplicablePropertyDef(parent, name, type, multiValued);
        VirtualPropertyState prop = new VirtualPropertyState(name, parent.getUUID());
        prop.setType(type);
        prop.setMultiValued(multiValued);
        prop.setDefinitionId(new PropDefId(def.unwrap()));
        items.put(prop.getId(), prop);
        return prop;
    }

    /**
     * @see VirtualItemStateProvider#createNodeState(org.apache.jackrabbit.core.virtual.VirtualNodeState, org.apache.jackrabbit.core.QName, String, org.apache.jackrabbit.core.QName)
     */
    public VirtualNodeState createNodeState(VirtualNodeState parent, QName name,
                                            String uuid, QName nodeTypeName)
            throws RepositoryException {

        NodeTypeImpl nodeType = getNodeTypeManager().getNodeType(nodeTypeName);
        NodeDefImpl def;
        try {
            def = getApplicableChildNodeDef(parent, name, nodeType == null ? null : nodeType.getQName());
        } catch (RepositoryException re) {
            // hack, use nt:unstructured as parent
            try {
                NodeTypeRegistry ntReg = getNodeTypeManager().getNodeTypeRegistry();
                EffectiveNodeType ent = ntReg.buildEffectiveNodeType(new QName[]{NodeTypeRegistry.NT_UNSTRUCTURED});
                ChildNodeDef cnd = ent.getApplicableChildNodeDef(name, nodeTypeName);
                def = getNodeTypeManager().getNodeDef(new NodeDefId(cnd));
            } catch (NodeTypeConflictException e) {
                String msg = "no definition found in parent node's node type for new node";
                throw new ConstraintViolationException(msg, re);
            }
        }
        if (nodeType == null) {
            // use default node type
            nodeType = (NodeTypeImpl) def.getDefaultPrimaryType();
        }

        // create a new node state
        VirtualNodeState state = null;
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();	// version 4 uuid
        }
        state = new VirtualNodeState(this, parent.getUUID(), uuid, nodeTypeName, new QName[0]);
        state.setDefinitionId(new NodeDefId(def.unwrap()));

        items.put(state.getId(), state);
        return state;
    }

    //-----------------------------------------------------< internal stuff >---

    /**
     * returns the node type manager
     * @return
     */
    private NodeTypeManagerImpl getNodeTypeManager() {
        return ntMgr;
    }

    /**
     * mapps a frozen node
     * @param state
     * @param node
     * @return
     * @throws RepositoryException
     */
    private VirtualNodeState mapFrozenNode(VirtualNodeState state,
                                           InternalFrozenNode node)
            throws RepositoryException {

        // map native stuff
        state.setMixinNodeTypes(node.getFrozenMixinTypes());
        if (node.getFrozenUUID()!=null) {
            state.setPropertyValue(ItemImpl.PROPNAME_UUID, InternalValue.create(node.getFrozenUUID()));
        }

        // map properties
        PropertyState[] props = node.getFrozenProperties();
        for (int i=0; i<props.length; i++) {
            if (props[i].isMultiValued()) {
                state.setPropertyValues(props[i].getName(), props[i].getType(), props[i].getValues());
            } else {
                state.setPropertyValue(props[i].getName(), props[i].getValues()[0]);
            }
        }
        // map child nodes
        InternalFreeze[] nodes = node.getFrozenChildNodes();
        for (int i=0; i<nodes.length; i++) {
            state.addChildNodeEntry(nodes[i].getName(), nodes[i].getId());
        }
        return state;
    }

    /**
     * maps a frozen node
     * @param state
     * @param node
     * @return
     * @throws RepositoryException
     */
    private VirtualNodeState mapFrozenNode(VirtualNodeState state,
                                           InternalFrozenVersionHistory node)
            throws RepositoryException {

        // map properties
        state.setPropertyValue(VersionManager.PROPNAME_BASE_VERSION, InternalValue.create(node.getBaseVersionId()));
        state.setPropertyValue(VersionManager.PROPNAME_VERSION_HISTORY, InternalValue.create(node.getVersionHistoryId()));
        return state;
    }

    /**
     * retrieves the property definition for the given contraints
     *
     * @param propertyName
     * @param type
     * @param multiValued
     * @return
     * @throws RepositoryException
     */
    protected PropertyDefImpl getApplicablePropertyDef(NodeState parent, QName propertyName,
                                                       int type, boolean multiValued)
            throws RepositoryException {
        PropDef pd = getEffectiveNodeType(parent).getApplicablePropertyDef(propertyName, type, multiValued);
        return getNodeTypeManager().getPropDef(new PropDefId(pd));
    }

    /**
     * Retrieves the node definition for the given contraints.
     *
     * @param nodeName
     * @param nodeTypeName
     * @return
     * @throws RepositoryException
     */
    protected NodeDefImpl getApplicableChildNodeDef(NodeState parent, QName nodeName, QName nodeTypeName)
            throws RepositoryException {
        ChildNodeDef cnd = getEffectiveNodeType(parent).getApplicableChildNodeDef(nodeName, nodeTypeName);
        return getNodeTypeManager().getNodeDef(new NodeDefId(cnd));
    }

    /**
     * Returns the effective (i.e. merged and resolved) node type representation
     * of this node's primary and mixin node types.
     *
     * @return the effective node type
     * @throws RepositoryException
     */
    protected EffectiveNodeType getEffectiveNodeType(NodeState parent) throws RepositoryException {
        // build effective node type of mixins & primary type
        NodeTypeRegistry ntReg = getNodeTypeManager().getNodeTypeRegistry();
        // existing mixin's
        HashSet set = new HashSet(parent.getMixinTypeNames());
        // primary type
        set.add(parent.getNodeTypeName());
        try {
            return ntReg.buildEffectiveNodeType((QName[]) set.toArray(new QName[set.size()]));
        } catch (NodeTypeConflictException ntce) {
            String msg = "internal error: failed to build effective node type for node " + parent.getUUID();
            throw new RepositoryException(msg, ntce);
        }
    }
}
