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
package org.apache.jackrabbit.core.virtual;

import org.apache.commons.collections.ReferenceMap;
import org.apache.jackrabbit.core.Constants;
import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.version.VersionHistoryNodeState;
import org.apache.jackrabbit.core.nodetype.ChildNodeDef;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.NodeDefId;
import org.apache.jackrabbit.core.nodetype.NodeTypeConflictException;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.nodetype.PropDefId;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeReferencesId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.util.uuid.UUID;
import org.apache.log4j.Logger;

import javax.jcr.RepositoryException;
import java.util.HashSet;
import java.util.Map;
import java.util.Iterator;

/**
 * This Class implements a virtual item state provider, in order to expose the
 * versions to the version storage.
 */
abstract public class AbstractVISProvider implements VirtualItemStateProvider, Constants {
    /**
     * the default logger
     */
    private static Logger log = Logger.getLogger(AbstractVISProvider.class);

    /**
     * the root node
     */
    private VirtualNodeState root = null;

    /**
     * the root node id
     */
    protected final NodeId rootNodeId;

    /**
     * the node type registry
     */
    protected final NodeTypeRegistry ntReg;

    /**
     * the cache node states. key=ItemId, value=ItemState
     */
    private Map nodes = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT);

    /**
     * Creates an abstract virtual item state provider
     * @param ntReg
     * @param rootNodeId
     */
    public AbstractVISProvider(NodeTypeRegistry ntReg, NodeId rootNodeId) {
        this.ntReg = ntReg;
        this.rootNodeId = rootNodeId;
    }

    /**
     * Creates the root node state.
     * @return
     * @throws RepositoryException
     */
    abstract protected VirtualNodeState createRootNodeState() throws RepositoryException;

    //-----------------------------------------------------< ItemStateManager >

    /**
     * {@inheritDoc}
     */
    public boolean hasItemState(ItemId id) {
        if (id instanceof NodeId) {
            if (nodes.containsKey(id)) {
                return true;
            } else if (id.equals(rootNodeId)) {
                return true;
            } else {
                return internalHasNodeState((NodeId) id);
            }
        } else {
            return internalHasPropertyState((PropertyId) id);
        }
    }

    /**
     * {@inheritDoc}
     */
    public ItemState getItemState(ItemId id)
            throws NoSuchItemStateException, ItemStateException {

        if (id instanceof NodeId) {
            ItemState s;
            if (nodes.containsKey(id)) {
                s = (ItemState) nodes.get(id);
            } else if (id.equals(rootNodeId)) {
                s = getRootState();
            } else {
                s = cache(internalGetNodeState((NodeId) id));
            }
            return s;
        } else {
            return internalGetPropertyState((PropertyId) id);
        }
    }

    /**
     * {@inheritDoc}
     */
    public NodeReferences getNodeReferences(NodeReferencesId id)
            throws NoSuchItemStateException, ItemStateException {
        throw new NoSuchItemStateException(id.getUUID());
    }

    //-------------------------------------------< VirtualItemStateProvider >---

    /**
     * {@inheritDoc}
     */
    public boolean isVirtualRoot(ItemId id) {
        return id.equals(rootNodeId);
    }

    /**
     * {@inheritDoc}
     */
    public NodeId getVirtualRootId() {
        return rootNodeId;
    }

    /**
     * Returns the root state
     * @return the root state
     */
    public synchronized NodeState getRootState() throws ItemStateException {
        try {
            if (root == null) {
                root = createRootNodeState();
            }
            return root;
        } catch (RepositoryException e) {
            throw new ItemStateException(e);
        }
    }

    /**
     * Checks if this provide has the node state of the given node id
     * @param id
     * @return
     */
    abstract protected boolean internalHasNodeState(NodeId id);

    /**
     * Retrieves the node state with the given node id
     * @param id
     * @return
     * @throws NoSuchItemStateException
     * @throws ItemStateException
     */
    abstract protected VirtualNodeState internalGetNodeState(NodeId id)
            throws NoSuchItemStateException, ItemStateException;

    /**
     * Checks if this provider has the property state of the given id.
     * @param id
     * @return
     */
    protected boolean internalHasPropertyState(PropertyId id) {

        try {
            // get parent state
            NodeState parent = (NodeState) getItemState(new NodeId(id.getParentUUID()));

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
     * Retrieces the property state for the given id
     * @param id
     * @return
     * @throws NoSuchItemStateException
     * @throws ItemStateException
     */
    protected VirtualPropertyState internalGetPropertyState(PropertyId id)
            throws NoSuchItemStateException, ItemStateException {

        // get parent state
        NodeState parent = (NodeState) getItemState(new NodeId(id.getParentUUID()));

        // handle some default prop states
        if (parent instanceof VirtualNodeState) {
            return ((VirtualNodeState) parent).getProperty(id.getName());
        }
        throw new NoSuchItemStateException(id.toString());
    }

    /**
     * {@inheritDoc}
     */
    public VirtualPropertyState createPropertyState(VirtualNodeState parent,
                                                    QName name, int type,
                                                    boolean multiValued)
            throws RepositoryException {
        PropDef def = getApplicablePropertyDef(parent, name, type, multiValued);
        VirtualPropertyState prop = new VirtualPropertyState(name, parent.getUUID());
        prop.setType(type);
        prop.setMultiValued(multiValued);
        prop.setDefinitionId(new PropDefId(def));
        return prop;
    }

    /**
     * {@inheritDoc}
     */
    public VirtualNodeState createNodeState(VirtualNodeState parent, QName name,
                                            String uuid, QName nodeTypeName)
            throws RepositoryException {

        NodeDefId def;
        try {
            def = new NodeDefId(getApplicableChildNodeDef(parent, name, nodeTypeName));
        } catch (RepositoryException re) {
            // hack, use nt:unstructured as parent
            NodeTypeRegistry ntReg = getNodeTypeRegistry();
            EffectiveNodeType ent = ntReg.getEffectiveNodeType(NT_UNSTRUCTURED);
            ChildNodeDef cnd = ent.getApplicableChildNodeDef(name, nodeTypeName);
            ntReg.getNodeDef(new NodeDefId(cnd));
            def = new NodeDefId(cnd);
        }

        // create a new node state
        VirtualNodeState state = null;
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();	// version 4 uuid
        }
        state = new VirtualNodeState(this, parent.getUUID(), uuid, nodeTypeName, new QName[0]);
        state.setDefinitionId(def);

        cache(state);
        return state;
    }

    /**
     * returns the node type manager
     *
     * @return
     */
    protected NodeTypeRegistry getNodeTypeRegistry() {
        return ntReg;
    }

    /**
     * adds the node state to the cache
     * @param state
     */
    protected NodeState cache(NodeState state) {
        if (state != null) {
            nodes.put(state.getId(), state);
            state.addListener(this);
            log.debug("item added to cache. size=" + nodes.size());
        }
        return state;
    }

    /**
     * removes the nodes state from the cache
     * @param id
     */
    protected NodeState evict(NodeId id) {
        return (NodeState) nodes.remove(id);
    }

    /**
     * Removes the item and all hard refernces from the cache and discards the
     * states.
     * @param id
     */
    public void invalidateItem(ItemId id) {
        if (id.equals(rootNodeId)) {
            if (root != null) {
                root.discard();
                try {
                    root = createRootNodeState();
                } catch (RepositoryException e) {
                    // ignore
                }
            }
            return;
        }
        VirtualNodeState state = (VirtualNodeState) nodes.remove(id);
        if (state != null) {
            HashSet set = state.removeAllStateReferences();
            if (set != null) {
                Iterator iter = set.iterator();
                while (iter.hasNext()) {
                    invalidateItem(((NodeState) iter.next()).getId());
                }
            }
            state.discard();
        }
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
    protected PropDef getApplicablePropertyDef(NodeState parent, QName propertyName,
                                               int type, boolean multiValued)
            throws RepositoryException {
        return getEffectiveNodeType(parent).getApplicablePropertyDef(propertyName, type, multiValued);
    }

    /**
     * Retrieves the node definition for the given contraints.
     *
     * @param nodeName
     * @param nodeTypeName
     * @return
     * @throws RepositoryException
     */
    protected ChildNodeDef getApplicableChildNodeDef(NodeState parent, QName nodeName, QName nodeTypeName)
            throws RepositoryException {
        return getEffectiveNodeType(parent).getApplicableChildNodeDef(nodeName, nodeTypeName);
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
        NodeTypeRegistry ntReg = getNodeTypeRegistry();
        // existing mixin's
        HashSet set = new HashSet(parent.getMixinTypeNames());
        // primary type
        set.add(parent.getNodeTypeName());
        try {
            return ntReg.getEffectiveNodeType((QName[]) set.toArray(new QName[set.size()]));
        } catch (NodeTypeConflictException ntce) {
            String msg = "internal error: failed to build effective node type for node " + parent.getUUID();
            throw new RepositoryException(msg, ntce);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean setNodeReferences(NodeReferences refs) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void stateCreated(ItemState created) {
    }

    /**
     * {@inheritDoc}
     */
    public void stateModified(ItemState modified) {
    }

    /**
     * {@inheritDoc}
     */
    public void stateDestroyed(ItemState destroyed) {
        destroyed.removeListener(this);
        if (destroyed.isNode() && destroyed.getId().equals(rootNodeId)) {
            try {
                root = createRootNodeState();
            } catch (RepositoryException e) {
                // ignore
            }
        }
        evict((NodeId) destroyed.getId());
    }

    /**
     * {@inheritDoc}
     */
    public void stateDiscarded(ItemState discarded) {
        discarded.removeListener(this);
        if (discarded.isNode() && discarded.getId().equals(rootNodeId)) {
            try {
                root = createRootNodeState();
            } catch (RepositoryException e) {
                // ignore
            }
        }
        evict((NodeId) discarded.getId());
    }
}
