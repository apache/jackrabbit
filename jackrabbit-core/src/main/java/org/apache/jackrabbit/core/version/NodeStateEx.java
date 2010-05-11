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

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.PropertyImpl;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.NodeTypeConflictException;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.UpdatableItemStateManager;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.uuid.UUID;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

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
     * Creates a new persistent node
     *
     * @param stateMgr
     * @param nodeState
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
                // should never occurr
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
     */
    public PropertyState[] getProperties() throws ItemStateException {
        Set set = nodeState.getPropertyNames();
        PropertyState[] props = new PropertyState[set.size()];
        int i = 0;
        for (Iterator iter = set.iterator(); iter.hasNext();) {
            Name propName = (Name) iter.next();
            PropertyId propId = new PropertyId(nodeState.getNodeId(), propName);
            props[i++] = (PropertyState) stateMgr.getItemState(propId);
        }
        return props;
    }

    /**
     * Checks if the given property exists
     *
     * @param name
     * @return <code>true</code> if the given property exists.
     */
    public boolean hasProperty(Name name) {
        PropertyId propId = new PropertyId(nodeState.getNodeId(), name);
        return stateMgr.hasItemState(propId);
    }

    /**
     * Returns the values of the given property of <code>null</code>
     *
     * @param name
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
     * @param name
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
     * @param name
     * @param value
     * @throws RepositoryException
     */
    public void setPropertyValue(Name name, InternalValue value)
            throws RepositoryException {
        setPropertyValues(name, value.getType(), new InternalValue[]{value}, false);
    }

    /**
     * Sets the property values
     *
     * @param name
     * @param type
     * @param values
     * @throws RepositoryException
     */
    public void setPropertyValues(Name name, int type, InternalValue[] values)
            throws RepositoryException {
        setPropertyValues(name, type, values, true);
    }

    /**
     * Sets the property values
     *
     * @param name
     * @param type
     * @param values
     * @throws RepositoryException
     */
    public void setPropertyValues(Name name, int type, InternalValue[] values, boolean multiple)
            throws RepositoryException {

        PropertyState prop = getOrCreatePropertyState(name, type, multiple);
        prop.setValues(values);
    }


    /**
     * Retrieves or creates a new property state as child property of this node
     *
     * @param name
     * @param type
     * @param multiValued
     * @return the property state
     * @throws RepositoryException
     */
    private PropertyState getOrCreatePropertyState(Name name, int type, boolean multiValued)
            throws RepositoryException {

        PropertyId propId = new PropertyId(nodeState.getNodeId(), name);
        if (stateMgr.hasItemState(propId)) {
            try {
                PropertyState propState = (PropertyState) stateMgr.getItemState(propId);
                // someone calling this method will always alter the property state, so set status to modified
                if (propState.getStatus() == ItemState.STATUS_EXISTING) {
                    propState.setStatus(ItemState.STATUS_EXISTING_MODIFIED);
                }
                // although this is not quite correct, we mark node as modified aswell
                if (nodeState.getStatus() == ItemState.STATUS_EXISTING) {
                    nodeState.setStatus(ItemState.STATUS_EXISTING_MODIFIED);
                }
                return propState;
            } catch (ItemStateException e) {
                throw new RepositoryException("Unable to create property: " + e.toString());
            }
        } else {
            PropertyState propState = stateMgr.createNew(name, nodeState.getNodeId());
            propState.setType(type);
            propState.setMultiValued(multiValued);

            // need to store nodestate
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
     * @throws RepositoryException
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
     * @param name
     * @return <code>true</code> if the given child exists.
     */
    public boolean hasNode(Name name) {
        return nodeState.hasChildNodeEntry(name);
    }

    /**
     * removes the (first) child node with the given name.
     *
     * @param name
     * @return <code>true</code> if the child was removed
     * @throws RepositoryException
     */
    public boolean removeNode(Name name) throws RepositoryException {
        return removeNode(name, 1);
    }

    /**
     * removes the child node with the given name and 1-based index
     *
     * @param name
     * @param index
     * @return <code>true</code> if the child was removed.
     * @throws RepositoryException
     */
    public boolean removeNode(Name name, int index) throws RepositoryException {
        try {
            ChildNodeEntry entry = nodeState.getChildNodeEntry(name, index);
            if (entry == null) {
                return false;
            } else {
                removeNode(entry.getId());
                nodeState.removeChildNodeEntry(name, index);
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
     * @param id
     * @throws ItemStateException
     */
    private void removeNode(NodeId id) throws ItemStateException {
        NodeState state = (NodeState) stateMgr.getItemState(id);

        // remove properties
        Iterator iter = state.getPropertyNames().iterator();
        while (iter.hasNext()) {
            Name name = (Name) iter.next();
            PropertyId propId = new PropertyId(id, name);
            PropertyState propState = (PropertyState) stateMgr.getItemState(propId);
            stateMgr.destroy(propState);
        }
        state.removeAllPropertyNames();

        // remove child nodes
        iter = state.getChildNodeEntries().iterator();
        while (iter.hasNext()) {
            ChildNodeEntry entry = (ChildNodeEntry) iter.next();
            removeNode(entry.getId());
        }
        state.removeAllChildNodeEntries();

        // destroy the state itself
        stateMgr.destroy(state);
    }

    /**
     * removes the property with the given name
     *
     * @param name
     * @return <code>true</code> if the property was removed.
     * @throws RepositoryException
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
                nodeState.setStatus(ItemState.STATUS_EXISTING_MODIFIED);
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
     * @param name
     * @param index
     * @return the node state.
     * @throws RepositoryException
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
     * Adds a new child node with the given name
     *
     * @param nodeName
     * @param nodeTypeName
     * @return the node state
     * @throws NoSuchNodeTypeException
     * @throws ConstraintViolationException
     * @throws RepositoryException
     */
    public NodeStateEx addNode(Name nodeName, Name nodeTypeName,
                               NodeId id, boolean referenceable)
            throws NoSuchNodeTypeException, ConstraintViolationException, RepositoryException {

        NodeStateEx node = createChildNode(nodeName, nodeTypeName, id);
        if (referenceable) {
            node.setPropertyValue(NameConstants.JCR_UUID, InternalValue.create(node.getNodeId().getUUID().toString()));
        }
        return node;
    }

    /**
     * creates a new child node
     *
     * @param name
     * @param id
     * @return the newly created node.
     */
    private NodeStateEx createChildNode(Name name, Name nodeTypeName, NodeId id)
            throws RepositoryException {
        NodeId parentId = nodeState.getNodeId();
        // create a new node state
        if (id == null) {
            id = new NodeId(UUID.randomUUID());
        }
        NodeState state = stateMgr.createNew(id, nodeTypeName, parentId);

        // create Node instance wrapping new node state
        NodeStateEx node = new NodeStateEx(stateMgr, ntReg, state, name);
        node.setPropertyValue(NameConstants.JCR_PRIMARYTYPE, InternalValue.create(nodeTypeName));

        // add new child node entryn
        nodeState.addChildNodeEntry(name, id);
        if (nodeState.getStatus() == ItemState.STATUS_EXISTING) {
            nodeState.setStatus(ItemState.STATUS_EXISTING_MODIFIED);
        }
        return node;
    }

    /**
     * returns all child nodes
     *
     * @return the child nodes.
     * @throws RepositoryException
     */
    public NodeStateEx[] getChildNodes() throws RepositoryException {
        try {
            List entries = nodeState.getChildNodeEntries();
            NodeStateEx[] children = new NodeStateEx[entries.size()];
            for (int i = 0; i < entries.size(); i++) {
                ChildNodeEntry entry = (ChildNodeEntry) entries.get(i);
                NodeState state = (NodeState) stateMgr.getItemState(entry.getId());
                children[i] = new NodeStateEx(stateMgr, ntReg, state, entry.getName());
            }
            return children;
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * stores the persistent state recursively
     *
     * @throws RepositoryException
     */
    public void store() throws RepositoryException {
        try {
            store(nodeState);
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * stores the given persistent state recursively
     *
     * @param state
     * @throws ItemStateException
     */
    private void store(NodeState state)
            throws ItemStateException {

        if (state.getStatus() != ItemState.STATUS_EXISTING) {
            // first store all transient properties
            Set props = state.getPropertyNames();
            for (Iterator iter = props.iterator(); iter.hasNext();) {
                Name propName = (Name) iter.next();
                PropertyState pstate = (PropertyState) stateMgr.getItemState(
                        new PropertyId(state.getNodeId(), propName));
                if (pstate.getStatus() != ItemState.STATUS_EXISTING) {
                    stateMgr.store(pstate);
                }
            }
            // now store all child node entries
            List nodes = state.getChildNodeEntries();
            for (int i = 0; i < nodes.size(); i++) {
                ChildNodeEntry entry = (ChildNodeEntry) nodes.get(i);
                NodeState nstate = (NodeState) stateMgr.getItemState(entry.getId());
                store(nstate);
            }
            // and store itself
            stateMgr.store(state);
        }
    }

    /**
     * reloads the persistent state recursively
     *
     * @throws RepositoryException
     */
    public void reload() throws RepositoryException {
        try {
            reload(nodeState);
            // refetch nodestate if discarded
            nodeState = (NodeState) stateMgr.getItemState(nodeState.getNodeId());
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * reloads the given persistent state recursively
     *
     * @param state
     * @throws ItemStateException
     */
    private void reload(NodeState state) throws ItemStateException {
        if (state.getStatus() != ItemState.STATUS_EXISTING) {
            // first discard all all transient properties
            Set props = state.getPropertyNames();
            for (Iterator iter = props.iterator(); iter.hasNext();) {
                Name propName = (Name) iter.next();
                PropertyState pstate = (PropertyState) stateMgr.getItemState(
                        new PropertyId(state.getNodeId(), propName));
                if (pstate.getStatus() != ItemState.STATUS_EXISTING) {
                    pstate.discard();
                }
            }
            // now reload all child node entries
            List nodes = state.getChildNodeEntries();
            for (int i = 0; i < nodes.size(); i++) {
                ChildNodeEntry entry = (ChildNodeEntry) nodes.get(i);
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
     * @param prop
     * @throws RepositoryException
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

}
