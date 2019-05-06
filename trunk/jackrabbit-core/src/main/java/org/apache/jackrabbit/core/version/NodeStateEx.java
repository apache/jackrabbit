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

import java.util.List;
import java.util.Set;

import javax.jcr.ItemExistsException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

import org.apache.jackrabbit.core.PropertyImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.NodeTypeConflictException;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.UpdatableItemStateManager;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

/**
 * This Class provides some basic node operations directly on the node state.
 */
public class NodeStateEx {

    /**
     * the underlying persistent state
     */
    private NodeState nodeState;

    /**
     * the state manager
     */
    private final UpdatableItemStateManager stateMgr;

    /**
     * the node type registry for resolving item defs
     */
    private final NodeTypeRegistry ntReg;

    /**
     * the cached name
     */
    private Name name;

    /**
     * the cached node definition
     */
    private QNodeDefinition def;

    /**
     * Creates a new persistent node
     *
     * @param stateMgr state manager
     * @param ntReg node type registry
     * @param nodeState underlying node state
     * @param name name (can be null)
     */
    public NodeStateEx(UpdatableItemStateManager stateMgr,
                       NodeTypeRegistry ntReg,
                       NodeState nodeState, Name name) {
        this.nodeState = nodeState;
        this.ntReg = ntReg;
        this.stateMgr = stateMgr;
        this.name = name;
    }

    /**
     * Creates a new persistent node
     *
     * @param stateMgr state manager
     * @param ntReg node type registry
     * @param nodeId node id
     * @throws RepositoryException if the node state can't be loaded
     */
    public NodeStateEx(UpdatableItemStateManager stateMgr,
                       NodeTypeRegistry ntReg,
                       NodeId nodeId) throws RepositoryException {
        try {
            this.ntReg = ntReg;
            this.stateMgr = stateMgr;
            this.nodeState = (NodeState) stateMgr.getItemState(nodeId);
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        }
    }


    /**
     * returns the name of this node
     *
     * @return the name of this node
     */
    public Name getName() {
        if (name == null) {
            try {
                NodeId parentId = nodeState.getParentId();
                NodeState parent = (NodeState) stateMgr.getItemState(parentId);
                name = parent.getChildNodeEntry(nodeState.getNodeId()).getName();
            } catch (ItemStateException e) {
                // should never occur
                throw new IllegalStateException(e.toString());
            }
        }
        return name;
    }

    /**
     * Returns the id of this node.
     *
     * @return the id of this node.
     */
    public NodeId getNodeId() {
        return nodeState.getNodeId();
    }

    /**
     * Returns the parent id of this node
     *
     * @return the parent id of this node
     */
    public NodeId getParentId() {
        return nodeState.getParentId();
    }

    /**
     * Returns the parent node of this node
     *
     * @return the parent node of this node or <code>null</code> if root node
     * @throws RepositoryException if an error occurs
     */
    public NodeStateEx getParent() throws RepositoryException {
        if (nodeState.getParentId() == null) {
            return null;
        }
        return getNode(nodeState.getParentId());
    }

    /**
     * Returns the underlaying node state.
     * @return the underlaying node state.
     */
    public NodeState getState() {
        return nodeState;
    }

    /**
     * Returns the properties of this node
     *
     * @return the properties of this node
     * @throws ItemStateException if an error occurs
     */
    public PropertyState[] getProperties() throws ItemStateException {
        Set<Name> set = nodeState.getPropertyNames();
        PropertyState[] props = new PropertyState[set.size()];
        int i = 0;
        for (Name propName : set) {
            PropertyId propId = new PropertyId(nodeState.getNodeId(), propName);
            props[i++] = (PropertyState) stateMgr.getItemState(propId);
        }
        return props;
    }

    /**
     * Checks if the given property exists
     *
     * @param name name of the property
     * @return <code>true</code> if the given property exists.
     */
    public boolean hasProperty(Name name) {
        PropertyId propId = new PropertyId(nodeState.getNodeId(), name);
        return stateMgr.hasItemState(propId);
    }

    /**
     * Returns the values of the given property or <code>null</code>
     *
     * @param name name of the property
     * @return the values of the given property.
     */
    public InternalValue[] getPropertyValues(Name name) {
        PropertyId propId = new PropertyId(nodeState.getNodeId(), name);
        try {
            PropertyState ps = (PropertyState) stateMgr.getItemState(propId);
            return ps.getValues();
        } catch (ItemStateException e) {
            return null;
        }
    }

    /**
     * Returns the value of the given property or <code>null</code>
     *
     * @param name name of the property
     * @return the value of the given property.
     */
    public InternalValue getPropertyValue(Name name) {
        PropertyId propId = new PropertyId(nodeState.getNodeId(), name);
        try {
            PropertyState ps = (PropertyState) stateMgr.getItemState(propId);
            return ps.getValues()[0];
        } catch (ItemStateException e) {
            return null;
        }
    }

    /**
     * Sets the property value
     *
     * @param name name of the property
     * @param value value to set
     * @throws RepositoryException if an error occurs
     */
    public void setPropertyValue(Name name, InternalValue value)
            throws RepositoryException {
        setPropertyValues(name, value.getType(), new InternalValue[]{value}, false);
    }

    /**
     * Sets the property values
     *
     * @param name name of the property
     * @param type property type
     * @param values values to set
     * @throws RepositoryException if an error occurs
     */
    public void setPropertyValues(Name name, int type, InternalValue[] values)
            throws RepositoryException {
        setPropertyValues(name, type, values, true);
    }

    /**
     * Sets the property values
     *
     * @param name name of the property
     * @param type type of the values
     * @param values values to set
     * @param multiple <code>true</code>for MV properties
     * @return the modified property state
     * @throws RepositoryException if an error occurs
     */
    public PropertyState setPropertyValues(Name name, int type, InternalValue[] values, boolean multiple)
            throws RepositoryException {
        PropertyId propId = new PropertyId(nodeState.getNodeId(), name);
        if (stateMgr.hasItemState(propId)) {
            try {
                PropertyState propState = (PropertyState) stateMgr.getItemState(propId);
                if (propState.getStatus() == ItemState.STATUS_EXISTING) {
                    propState.setStatus(ItemState.STATUS_EXISTING_MODIFIED);
                }
                // although this is not quite correct, we mark node as modified as well
                if (nodeState.getStatus() == ItemState.STATUS_EXISTING) {
                    nodeState.setStatus(ItemState.STATUS_EXISTING_MODIFIED);
                }
                propState.setType(type);
                propState.setMultiValued(multiple);
                propState.setValues(values);
                return propState;
            } catch (ItemStateException e) {
                throw new RepositoryException("Unable to create property: " + e.toString());
            }
        } else {
            PropertyState propState = stateMgr.createNew(name, nodeState.getNodeId());
            propState.setType(type);
            propState.setMultiValued(multiple);
            propState.setValues(values);

            // need to store node state
            nodeState.addPropertyName(name);
            if (nodeState.getStatus() == ItemState.STATUS_EXISTING) {
                nodeState.setStatus(ItemState.STATUS_EXISTING_MODIFIED);
            }
            return propState;
        }
    }

    /**
     * Returns the effective (i.e. merged and resolved) node type representation
     * of this node's primary and mixin node types.
     *
     * @return the effective node type
     * @throws RepositoryException if an error occurs
     */
    public EffectiveNodeType getEffectiveNodeType() throws RepositoryException {
        try {
            return ntReg.getEffectiveNodeType(
                    nodeState.getNodeTypeName(), nodeState.getMixinTypeNames());
        } catch (NodeTypeConflictException ntce) {
            String msg = "internal error: failed to build effective node type for node " + nodeState.getNodeId();
            throw new RepositoryException(msg, ntce);
        }
    }

    /**
     * checks if the given child node exists.
     *
     * @param name name of the node
     * @return <code>true</code> if the given child exists.
     */
    public boolean hasNode(Name name) {
        return nodeState.hasChildNodeEntry(name);
    }

    /**
     * removes the (first) child node with the given name.
     *
     * @param name name of the node
     * @return <code>true</code> if the child was removed
     * @throws RepositoryException if an error occurs
     */
    public boolean removeNode(Name name) throws RepositoryException {
        return removeNode(name, 1);
    }

    /**
     * removes the given child node
     *
     * @param node child node to remove
     * @return <code>true</code> if the child was removed
     * @throws RepositoryException if an error occurs
     */
    public boolean removeNode(NodeStateEx node) throws RepositoryException {
        // locate child node entry
        return removeNode(nodeState.getChildNodeEntry(node.getNodeId()));
    }


    /**
     * removes the child node with the given name and 1-based index
     *
     * @param name name of the child node
     * @param index index of the child node
     * @return <code>true</code> if the child was removed.
     * @throws RepositoryException if an error occurs
     */
    public boolean removeNode(Name name, int index) throws RepositoryException {
        return removeNode(nodeState.getChildNodeEntry(name, index));
    }

    /**
     * removes the child node with the given child node entry
     *
     * @param entry entry to remove
     * @return <code>true</code> if the child was removed.
     * @throws RepositoryException if an error occurs
     */
    public boolean removeNode(ChildNodeEntry entry) throws RepositoryException {
        try {
            if (entry == null) {
                return false;
            } else {
                removeNode(entry.getId());
                nodeState.removeChildNodeEntry(entry.getId());
                nodeState.setStatus(ItemState.STATUS_EXISTING_MODIFIED);
                return true;
            }
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * removes recursively the node with the given id
     *
     * @param id node id
     * @throws ItemStateException if an error occurs
     */
    private void removeNode(NodeId id) throws ItemStateException {
        NodeState state = (NodeState) stateMgr.getItemState(id);

        // remove properties
        for (Name name :  state.getPropertyNames()) {
            PropertyId propId = new PropertyId(id, name);
            PropertyState propState = (PropertyState) stateMgr.getItemState(propId);
            stateMgr.destroy(propState);
        }
        state.removeAllPropertyNames();

        // remove child nodes
        for (ChildNodeEntry entry : state.getChildNodeEntries()) {
            removeNode(entry.getId());
        }
        state.removeAllChildNodeEntries();

        // destroy the state itself
        stateMgr.destroy(state);
    }

    /**
     * removes the property with the given name
     *
     * @param name name of the property
     * @return <code>true</code> if the property was removed.
     * @throws RepositoryException if an error occurs
     */
    public boolean removeProperty(Name name) throws RepositoryException {
        try {
            if (!nodeState.hasPropertyName(name)) {
                return false;
            } else {
                PropertyId propId = new PropertyId(nodeState.getNodeId(), name);
                ItemState state = stateMgr.getItemState(propId);
                stateMgr.destroy(state);
                nodeState.removePropertyName(name);
                if (nodeState.getStatus() != ItemState.STATUS_NEW) {
                    nodeState.setStatus(ItemState.STATUS_EXISTING_MODIFIED);
                }
                return true;
            }
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * retrieves the child node with the given name and 1-base index or
     * <code>null</code> if the node does not exist.
     *
     * @param name name of the child node
     * @param index index of the child node
     * @return the node state.
     * @throws RepositoryException if an error occurs
     */
    public NodeStateEx getNode(Name name, int index) throws RepositoryException {
        ChildNodeEntry entry = nodeState.getChildNodeEntry(name, index);
        if (entry == null) {
            return null;
        }
        try {
            NodeState state = (NodeState) stateMgr.getItemState(entry.getId());
            return new NodeStateEx(stateMgr, ntReg, state, name);
        } catch (ItemStateException e) {
            throw new RepositoryException("Unable to getNode: " + e.toString());
        }
    }

    /**
     * Returns the node with the given id.
     * @param id node id
     * @return the new node state
     * @throws RepositoryException if an error occurs
     */
    public NodeStateEx getNode(NodeId id) throws RepositoryException {
        try {
            NodeState state = (NodeState) stateMgr.getItemState(id);
            return new NodeStateEx(stateMgr, ntReg, state, name);
        } catch (ItemStateException e) {
            throw new RepositoryException("Unable to getNode: " + e.toString());
        }
    }

    /**
     * Checks if the given node state exists
     * @param id node id
     * @return <code>true</code> if the node state exists
     */
    public boolean hasNode(NodeId id) {
        return stateMgr.hasItemState(id);
    }

    /**
     * Checks if the given property state exists
     * @param id property id
     * @return <code>true</code> if the property state exists
     */
    public boolean hasProperty(PropertyId id) {
        return stateMgr.hasItemState(id);
    }

    /**
     * Adds a new child node with the given name
     *
     * @param nodeName name of the new node
     * @param nodeTypeName node type name
     * @param id id of the new node
     * @return the node state
     * @throws NoSuchNodeTypeException if the node type does not exist
     * @throws ConstraintViolationException if there is a constraint violation
     * @throws RepositoryException if an error occurs
     */
    public NodeStateEx addNode(Name nodeName, Name nodeTypeName, NodeId id)
            throws NoSuchNodeTypeException, ConstraintViolationException, RepositoryException {
        return addNode(nodeName, nodeTypeName, id,
                ntReg.getEffectiveNodeType(nodeTypeName).includesNodeType(NameConstants.MIX_REFERENCEABLE));
    }

    /**
     * Adds a new child node with the given name
     *
     * @param nodeName name of the new node
     * @param nodeTypeName node type name
     * @param id id of the new node
     * @param referenceable if <code>true</code>, a UUID property is created
     * @return the node state
     * @throws NoSuchNodeTypeException if the node type does not exist
     * @throws ConstraintViolationException if there is a constraint violation
     * @throws RepositoryException if an error occurs
     */
    public NodeStateEx addNode(Name nodeName, Name nodeTypeName,
                               NodeId id, boolean referenceable)
            throws NoSuchNodeTypeException, ConstraintViolationException, RepositoryException {

        NodeStateEx node = createChildNode(nodeName, nodeTypeName, id);
        if (referenceable) {
            node.setPropertyValue(NameConstants.JCR_UUID, InternalValue.create(node.getNodeId().toString()));
        }
        return node;
    }

    /**
     * Sets the given mixin types
     * @param mixinTypeNames the mixin type names
     * @throws RepositoryException if an error occurs
     */
    public void setMixins(Set<Name> mixinTypeNames) throws RepositoryException {
        nodeState.setMixinTypeNames(mixinTypeNames);
        // update jcr:mixinTypes property
        setPropertyValues(NameConstants.JCR_MIXINTYPES, PropertyType.NAME,
                InternalValue.create(
                        mixinTypeNames.toArray(new Name[mixinTypeNames.size()]))
        );
    }
    /**
     * creates a new child node
     *
     * @param name name
     * @param nodeTypeName node type name
     * @param id id
     * @return the newly created node.
     * @throws RepositoryException if an error occurs
     */
    private NodeStateEx createChildNode(Name name, Name nodeTypeName, NodeId id)
            throws RepositoryException {
        NodeId parentId = nodeState.getNodeId();
        // create a new node state
        NodeState state = stateMgr.createNew(id, nodeTypeName, parentId);

        // create Node instance wrapping new node state
        NodeStateEx node = new NodeStateEx(stateMgr, ntReg, state, name);
        node.setPropertyValue(NameConstants.JCR_PRIMARYTYPE, InternalValue.create(nodeTypeName));

        // add new child node entry
        nodeState.addChildNodeEntry(name, state.getNodeId());
        if (nodeState.getStatus() == ItemState.STATUS_EXISTING) {
            nodeState.setStatus(ItemState.STATUS_EXISTING_MODIFIED);
        }
        return node;
    }

    /**
     * Moves the source node to this node using the given name.
     * @param src shareable source node
     * @param name name of new node
     * @param createShare if <code>true</code> a share is created instead.
     * @return child node
     * @throws RepositoryException if an error occurs
     */
    public NodeStateEx moveFrom(NodeStateEx src, Name name, boolean createShare)
            throws RepositoryException {
        if (name == null) {
            name = src.getName();
        }
        EffectiveNodeType ent = getEffectiveNodeType();
        // (4) check for name collisions
        QNodeDefinition def;
        try {
            def = ent.getApplicableChildNodeDef(name, nodeState.getNodeTypeName(), ntReg);
        } catch (RepositoryException re) {
            String msg = "no definition found in parent node's node type for new node";
            throw new ConstraintViolationException(msg, re);
        }
        ChildNodeEntry cne = nodeState.getChildNodeEntry(name, 1);
        if (cne != null) {
            // there's already a child node entry with that name;
            // check same-name sibling setting of new node
            if (!def.allowsSameNameSiblings()) {
                throw new ItemExistsException(getNodeId() + "/" + name);
            }
            NodeState existingChild;
            try {
                // check same-name sibling setting of existing node
                existingChild = (NodeState) stateMgr.getItemState(cne.getId());
            } catch (ItemStateException e) {
                throw new RepositoryException(e);
            }
            QNodeDefinition existingChildDef = ent.getApplicableChildNodeDef(
                    cne.getName(), existingChild.getNodeTypeName(), ntReg);
            if (!existingChildDef.allowsSameNameSiblings()) {
                throw new ItemExistsException(existingChild.toString());
            }
        } else {
            // check if 'add' is allowed
            if (getDefinition().isProtected()) {
                String msg = "not allowed to modify a protected node";
                throw new ConstraintViolationException(msg);
            }
        }

        if (createShare) {
            // (5) do clone operation
            NodeId parentId = getNodeId();
            src.addShareParent(parentId);
            // attach to this parent
            nodeState.addChildNodeEntry(name, src.getNodeId());
            if (nodeState.getStatus() == ItemState.STATUS_EXISTING) {
                nodeState.setStatus(ItemState.STATUS_EXISTING_MODIFIED);
            }
            return new NodeStateEx(stateMgr, ntReg, src.getState(), name);
        } else {
            // detach from parent
            NodeStateEx parent = getNode(src.getParentId());
            parent.nodeState.removeChildNodeEntry(src.getNodeId());
            if (parent.nodeState.getStatus() == ItemState.STATUS_EXISTING) {
                parent.nodeState.setStatus(ItemState.STATUS_EXISTING_MODIFIED);
            }
            // attach to this parent
            nodeState.addChildNodeEntry(name, src.getNodeId());
            if (nodeState.getStatus() == ItemState.STATUS_EXISTING) {
                nodeState.setStatus(ItemState.STATUS_EXISTING_MODIFIED);
            }
            NodeState srcState = src.getState();
            srcState.setParentId(getNodeId());

            if (srcState.getStatus() == ItemState.STATUS_EXISTING) {
                srcState.setStatus(ItemState.STATUS_EXISTING_MODIFIED);
            }
            return new NodeStateEx(stateMgr, ntReg, srcState, name);
        }
    }

    /**
     * Adds a share parent id
     * @param parentId the parent id
     * @throws RepositoryException if an error occurs
     */
    private void addShareParent(NodeId parentId) throws RepositoryException {
        // verify that we're shareable
        if (!nodeState.isShareable()) {
            String msg = this + " is not shareable.";
            throw new RepositoryException(msg);
        }

        // detect share cycle (TODO)
        // NodeId srcId = getNodeId();
        //HierarchyManager hierMgr = session.getHierarchyManager();
        //if (parentId.equals(srcId) || hierMgr.isAncestor(srcId, parentId)) {
        //    String msg = "This would create a share cycle.";
        //    log.debug(msg);
        //    throw new RepositoryException(msg);
        //}

        if (!nodeState.containsShare(parentId)) {
            if (nodeState.addShare(parentId)) {
                return;
            }
        }
        String msg = "Adding a shareable node twice to the same parent is not supported.";
        throw new UnsupportedRepositoryOperationException(msg);
    }

    /**
     * returns all child nodes
     *
     * @return the child nodes.
     * @throws RepositoryException if an error occurs
     */
    public NodeStateEx[] getChildNodes() throws RepositoryException {
        try {
            List<ChildNodeEntry> entries = nodeState.getChildNodeEntries();
            NodeStateEx[] children = new NodeStateEx[entries.size()];
            int i = 0;
            for (ChildNodeEntry entry : entries) {
                NodeState state = (NodeState) stateMgr.getItemState(entry.getId());
                children[i++] = new NodeStateEx(stateMgr, ntReg, state, entry.getName());
            }
            return children;
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * stores the persistent state recursively
     *
     * @throws RepositoryException if an error occurs
     */
    public void store() throws RepositoryException {
        store(true);
    }

    /**
     * Stores the persistent state and depending on the <code>recursively</code>
     * flag also stores the modified child nodes recursively.
     *
     *
     * @param recursively whether to store the nodes recursively or just this
     *                    single node.
     * @throws RepositoryException if an error occurs
     */
    public void store(boolean recursively) throws RepositoryException {
        try {
            store(nodeState, recursively);
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * stores the given persistent state recursively
     *
     * @param state node state to store
     * @throws ItemStateException if an error occurs
     */
    private void store(NodeState state, boolean recursively)
            throws ItemStateException {

        if (state.getStatus() != ItemState.STATUS_EXISTING) {
            // first store all transient properties
            for (Name propName : state.getPropertyNames()) {
                PropertyState pstate = (PropertyState) stateMgr.getItemState(
                        new PropertyId(state.getNodeId(), propName));
                if (pstate.getStatus() != ItemState.STATUS_EXISTING) {
                    stateMgr.store(pstate);
                }
            }
            if (recursively) {
                // now store all child node entries
                for (ChildNodeEntry entry : state.getChildNodeEntries()) {
                    NodeState nstate = (NodeState) stateMgr.getItemState(entry.getId());
                    store(nstate, true);
                }
            }
            // and store itself
            stateMgr.store(state);
        }
    }

    /**
     * reloads the persistent state recursively
     *
     * @throws RepositoryException if an error occurs
     */
    public void reload() throws RepositoryException {
        try {
            reload(nodeState);
            // refetch node state if discarded
            nodeState = (NodeState) stateMgr.getItemState(nodeState.getNodeId());
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * reloads the given persistent state recursively
     *
     * @param state node state
     * @throws ItemStateException if an error occurs
     */
    private void reload(NodeState state) throws ItemStateException {
        if (state.getStatus() != ItemState.STATUS_EXISTING) {
            // first discard all all transient properties
            for (Name propName : state.getPropertyNames()) {
                PropertyState pstate = (PropertyState) stateMgr.getItemState(
                        new PropertyId(state.getNodeId(), propName));
                if (pstate.getStatus() != ItemState.STATUS_EXISTING) {
                    pstate.discard();
                }
            }
            // now reload all child node entries
            for (ChildNodeEntry entry : state.getChildNodeEntries()) {
                NodeState nstate = (NodeState) stateMgr.getItemState(entry.getId());
                reload(nstate);
            }
            // and reload itself
            state.discard();
        }
    }

    /**
     * copies a property
     *
     * @param prop source property
     * @throws RepositoryException if an error occurs
     */
    public void copyFrom(PropertyImpl prop) throws RepositoryException {
        if (prop.isMultiple()) {
            InternalValue[] values = prop.internalGetValues();
            InternalValue[] copiedValues = new InternalValue[values.length];
            for (int i = 0; i < values.length; i++) {
                copiedValues[i] = values[i].createCopy();
            }
            setPropertyValues(prop.getQName(), prop.getType(), copiedValues);
        } else {
            setPropertyValue(prop.getQName(), prop.internalGetValue().createCopy());
        }
    }

    /**
     * copies a property
     *
     * @param prop source property
     * @throws RepositoryException if an error occurs
     */
    public void copyFrom(PropertyState prop) throws RepositoryException {
        InternalValue[] values = prop.getValues();
        InternalValue[] copiedValues = new InternalValue[values.length];
        for (int i = 0; i < values.length; i++) {
            copiedValues[i] = values[i].createCopy();
        }
        setPropertyValues(prop.getName(), prop.getType(), copiedValues, prop.isMultiValued());
    }

    /**
     * Returns the QNodeDefinition for this state
     * @return the node def
     * @throws RepositoryException if an error occurs
     */
    public QNodeDefinition getDefinition() throws RepositoryException {
        if (def == null) {
            EffectiveNodeType ent = getParent().getEffectiveNodeType();
            def = ent.getApplicableChildNodeDef(getName(),
                    nodeState.getNodeTypeName(), ntReg);
        }
        return def;
    }

    /**
     * Returns the property definition for the property state
     * @param prop the property state
     * @return the prop def
     * @throws RepositoryException if an error occurs
     */
    public QPropertyDefinition getDefinition(PropertyState prop)
            throws RepositoryException {
        return getEffectiveNodeType().getApplicablePropertyDef(
                prop.getName(), prop.getType(), prop.isMultiValued());
    }

    /**
     * Checks if this state has the indicated ancestor
     * @param nodeId the node id of the ancestor
     * @return <code>true</code> if it has the indicated ancestor
     * @throws RepositoryException if an error occurs
     */
    public boolean hasAncestor(NodeId nodeId) throws RepositoryException {
        if (nodeId.equals(nodeState.getParentId())) {
            return true;
        }
        NodeStateEx parent = getParent();
        return parent != null && parent.hasAncestor(nodeId);
    }
}
