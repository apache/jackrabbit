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

import org.apache.jackrabbit.core.*;
import org.apache.jackrabbit.core.nodetype.*;
import org.apache.jackrabbit.core.state.*;
import org.apache.jackrabbit.core.util.uuid.UUID;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import java.util.HashSet;
import java.util.List;

/**
 * This Class provides some basic node operations directly on the persistent
 * state.
 */
class PersistentNode {

    /**
     * the underlaying persistent state
     */
    private PersistentNodeState nodeState;

    /**
     * the state manager
     */
    private final PersistentItemStateProvider stateMgr;

    /**
     * the cached name
     */
    private QName name = null;

    /**
     * Creates a new persistent node
     *
     * @param statemgr
     * @param nodeState
     */
    protected PersistentNode(PersistentItemStateProvider statemgr,
                             PersistentNodeState nodeState) {
        this.nodeState = nodeState;
        this.stateMgr = statemgr;
    }


    /**
     * returns the name of this node
     *
     * @return
     */
    protected QName getName() {
        if (name == null) {
            try {
                String parentId = nodeState.getParentUUID();
                NodeState parent = (NodeState) stateMgr.getItemState(new NodeId(parentId));
                name = ((NodeState.ChildNodeEntry) parent.getChildNodeEntries(nodeState.getUUID()).get(0)).getName();
            } catch (ItemStateException e) {
                // should never occurr
                throw new IllegalStateException(e.toString());
            }
        }
        return name;
    }

    /**
     * Returns the uuid of this node
     *
     * @return
     */
    protected String getUUID() {
        return nodeState.getUUID();
    }

    protected String getParentUUID() {
        return nodeState.getParentUUID();
    }

    protected PersistentNodeState getState() {
        return nodeState;
    }

    /**
     * Returns the properties of this node
     *
     * @return
     */
    protected PropertyState[] getProperties() throws ItemStateException {
        List list = nodeState.getPropertyEntries();
        PropertyState[] props = new PropertyState[list.size()];
        for (int i = 0; i < list.size(); i++) {
            NodeState.PropertyEntry entry = (NodeState.PropertyEntry) list.get(i);
            PropertyId propId = new PropertyId(nodeState.getUUID(), entry.getName());
            props[i] = (PropertyState) stateMgr.getItemState(propId);
        }
        return props;
    }

    /**
     * Checks if the given property exists
     *
     * @param name
     * @return
     */
    protected boolean hasProperty(QName name) {
        PropertyId propId = new PropertyId(nodeState.getUUID(), name);
        return stateMgr.hasItemState(propId);
    }

    /**
     * Returns the values of the given property of <code>null</code>
     *
     * @param name
     * @return
     */
    protected InternalValue[] getPropertyValues(QName name) {
        PropertyId propId = new PropertyId(nodeState.getUUID(), name);
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
     * @return
     */
    protected InternalValue getPropertyValue(QName name) {
        PropertyId propId = new PropertyId(nodeState.getUUID(), name);
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
    protected void setPropertyValue(QName name, InternalValue value)
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
    protected void setPropertyValues(QName name, int type, InternalValue[] values)
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
    protected void setPropertyValues(QName name, int type, InternalValue[] values, boolean multiple)
            throws RepositoryException {
        PersistentPropertyState prop = getOrCreatePropertyState(name, type, multiple);
        prop.setValues(values);
    }

    /**
     * Retrieves or creates a new property state as child property of this node
     *
     * @param name
     * @param type
     * @param multiValued
     * @return
     * @throws RepositoryException
     */
    private PersistentPropertyState getOrCreatePropertyState(QName name, int type, boolean multiValued)
            throws RepositoryException {

        PropertyId propId = new PropertyId(nodeState.getUUID(), name);
        if (stateMgr.hasItemState(propId)) {
            try {
                PersistentPropertyState propState = (PersistentPropertyState) stateMgr.getItemState(propId);
                // someone calling this method will always alter the property state, so set status to modified
                if (propState.getStatus()==ItemState.STATUS_EXISTING) {
                    propState.setStatus(ItemState.STATUS_EXISTING_MODIFIED);
                }
                // although this is not quite correct, we mark node as modified aswell
                if (nodeState.getStatus()==ItemState.STATUS_EXISTING) {
                    nodeState.setStatus(ItemState.STATUS_EXISTING_MODIFIED);
                }
                return propState;
            } catch (ItemStateException e) {
                throw new RepositoryException("Unable to create property: " + e.toString());
            }
        } else {
            try {
                PersistentPropertyState propState = stateMgr.createPropertyState(nodeState.getUUID(), name);
                propState.setType(type);
                propState.setMultiValued(multiValued);
                propState.setDefinitionId(PropDefId.valueOf("0"));
                // need to store nodestate
                nodeState.addPropertyEntry(name);
                if (nodeState.getStatus()==ItemState.STATUS_EXISTING) {
                    nodeState.setStatus(ItemState.STATUS_EXISTING_MODIFIED);
                }
                return propState;
            } catch (ItemStateException e) {
                throw new RepositoryException("Unable to store property: " + e.toString());
            }
        }
    }

    /**
     * checks if the given child node exists.
     *
     * @param name
     * @return
     */
    protected boolean hasNode(QName name) {
        return nodeState.hasChildNodeEntry(name);
    }

    /**
     * removes the (first) child node with the given name.
     *
     * @param name
     * @return
     * @throws RepositoryException
     */
    protected boolean removeNode(QName name) throws RepositoryException {
        return removeNode(name, 1);
    }

    /**
     * removes the child node with the given name and 1-based index
     *
     * @param name
     * @param index
     * @return
     * @throws RepositoryException
     */
    protected boolean removeNode(QName name, int index) throws RepositoryException {
        if (nodeState.removeChildNodeEntry(name, index)) {
            if (nodeState.getStatus()==ItemState.STATUS_EXISTING) {
                nodeState.setStatus(ItemState.STATUS_EXISTING_MODIFIED);
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * retrieves the child node with the given name and 1-base index or
     * <code>null</code> if the node does not exist.
     *
     * @param name
     * @param index
     * @return
     * @throws RepositoryException
     */
    protected PersistentNode getNode(QName name, int index) throws RepositoryException {
        NodeState.ChildNodeEntry entry = nodeState.getChildNodeEntry(name, index);
        if (entry == null) {
            return null;
        }
        try {
            PersistentNodeState state = (PersistentNodeState) stateMgr.getItemState(new NodeId(entry.getUUID()));
            return new PersistentNode(stateMgr, state);
        } catch (ItemStateException e) {
            throw new RepositoryException("Unable to getNode: " + e.toString());
        }
    }

    /**
     * returns the node with the given uuid.
     *
     * @param uuid
     * @return
     * @throws RepositoryException
     */
    protected PersistentNode getNodeByUUID(String uuid) throws RepositoryException {
        try {
            PersistentNodeState state = (PersistentNodeState) stateMgr.getItemState(new NodeId(uuid));
            return new PersistentNode(stateMgr, state);
        } catch (ItemStateException e) {
            throw new RepositoryException("Unable to getNode: " + e.toString());
        }
    }

    /**
     * Adds a new child node with the given name
     *
     * @param nodeName
     * @param nodeTypeName
     * @return
     * @throws NoSuchNodeTypeException
     * @throws ConstraintViolationException
     * @throws RepositoryException
     */
    protected PersistentNode addNode(QName nodeName, QName nodeTypeName)
            throws NoSuchNodeTypeException, ConstraintViolationException, RepositoryException {
        return createChildNode(nodeName, nodeTypeName, null);
    }

    /**
     * creates a new child node
     *
     * @param name
     * @param uuid
     * @return
     * @throws RepositoryException
     */
    private PersistentNode createChildNode(QName name, QName nodeTypeName, String uuid)
            throws RepositoryException {

        String parentUUID = nodeState.getUUID();
        // create a new node state
        PersistentNodeState state = null;
        try {
            if (uuid == null) {
                uuid = UUID.randomUUID().toString();	// version 4 uuid
            }
            state = stateMgr.createNodeState(uuid, nodeTypeName, parentUUID);
            state.setDefinitionId(NodeDefId.valueOf("0"));
        } catch (ItemStateException ise) {
            String msg = "failed to add child node " + name + " to " + parentUUID;
            throw new RepositoryException(msg, ise);
        }

        // create Node instance wrapping new node state
        PersistentNode node = new PersistentNode(stateMgr, state);
        // add new child node entry
        nodeState.addChildNodeEntry(name, state.getUUID());
        if (nodeState.getStatus()==ItemState.STATUS_EXISTING) {
            nodeState.setStatus(ItemState.STATUS_EXISTING_MODIFIED);
        }
        return node;
    }

    /**
     * returns all child nodes
     *
     * @return
     * @throws RepositoryException
     */
    protected PersistentNode[] getChildNodes() throws RepositoryException {
        try {
            List entries = nodeState.getChildNodeEntries();
            PersistentNode[] children = new PersistentNode[entries.size()];
            for (int i = 0; i < entries.size(); i++) {
                NodeState.ChildNodeEntry entry = (NodeState.ChildNodeEntry) entries.get(i);
                PersistentNodeState state = (PersistentNodeState) stateMgr.getItemState(new NodeId(entry.getUUID()));
                children[i] = new PersistentNode(stateMgr, state);
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
    protected void store() throws RepositoryException {
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
    private void store(PersistentNodeState state) throws ItemStateException {
        if (state.isTransient()) {
            // first store all transient properties
            List props = state.getPropertyEntries();
            for (int i = 0; i < props.size(); i++) {
                NodeState.PropertyEntry entry = (NodeState.PropertyEntry) props.get(i);
                PersistentPropertyState pstate = (PersistentPropertyState) stateMgr.getItemState(new PropertyId(state.getUUID(), entry.getName()));
                if (pstate.isTransient()) {
                    pstate.store();
                }
            }
            // now store all child node entries
            List nodes = state.getChildNodeEntries();
            for (int i = 0; i < nodes.size(); i++) {
                NodeState.ChildNodeEntry entry = (NodeState.ChildNodeEntry) nodes.get(i);
                PersistentNodeState nstate = (PersistentNodeState) stateMgr.getItemState(new NodeId(entry.getUUID()));
                store(nstate);
            }
            // and store itself
            state.store();
        }
    }

    /**
     * reloads the persistent state recursively
     *
     * @throws RepositoryException
     */
    protected void reload() throws RepositoryException {
        try {
            reload(nodeState);
            // refetch nodestate if discarded
            nodeState = (PersistentNodeState) stateMgr.getItemState(nodeState.getId());
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
    private void reload(PersistentNodeState state) throws ItemStateException {
        if (state.isTransient()) {
            // first discard all all transient properties
            List props = state.getPropertyEntries();
            for (int i = 0; i < props.size(); i++) {
                NodeState.PropertyEntry entry = (NodeState.PropertyEntry) props.get(i);
                PersistentPropertyState pstate = (PersistentPropertyState) stateMgr.getItemState(new PropertyId(state.getUUID(), entry.getName()));
                if (pstate.isTransient()) {
                    pstate.discard();
                }
            }
            // now reload all child node entries
            List nodes = state.getChildNodeEntries();
            for (int i = 0; i < nodes.size(); i++) {
                NodeState.ChildNodeEntry entry = (NodeState.ChildNodeEntry) nodes.get(i);
                PersistentNodeState nstate = (PersistentNodeState) stateMgr.getItemState(new NodeId(entry.getUUID()));
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
    protected void copyFrom(PropertyImpl prop) throws RepositoryException {
        if (prop.getDefinition().isMultiple()) {
            InternalValue[] values = prop.internalGetValues();
            setPropertyValues(prop.getQName(), values[0].getType(), values);
        } else {
            setPropertyValue(prop.getQName(), prop.internalGetValue());
        }
    }

    /**
     * sets the mixing node type and adds the respective property
     *
     * @param mixins
     * @throws RepositoryException
     */
    protected void setMixinNodeTypes(QName[] mixins) throws RepositoryException {
        HashSet set = new HashSet();
        InternalValue[] values = new InternalValue[mixins.length];
        for (int i = 0; i < mixins.length; i++) {
            set.add(mixins[i]);
            values[i] = InternalValue.create(mixins[i]);
        }
        nodeState.setMixinTypeNames(set);
        setPropertyValues(ItemImpl.PROPNAME_MIXINTYPES, PropertyType.NAME, values);
    }
}
