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
package org.apache.jackrabbit.core.virtual;

import org.apache.jackrabbit.core.*;
import org.apache.jackrabbit.core.nodetype.*;
import org.apache.jackrabbit.core.state.*;
import org.apache.jackrabbit.core.util.uuid.UUID;
import org.apache.log4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/**
 * This Class implements a virtual item state provider.
 *
 * @author tripod
 * @version $Revision:$, $Date:$
 */
public class DefaultItemStateProvider implements VirtualItemStateProvider {

    /**
     * the default logger
     */
    private static Logger log = Logger.getLogger(DefaultItemStateProvider.class);

    /**
     * the nodetype manager that is used by this provider
     */
    private final NodeTypeManagerImpl ntMgr;

    /**
     * the virtual root state of this provider (does not have to be /)
     */
    //private final VirtualNodeState rootState;

    /**
     * the items of this provider
     */
    private final HashMap items = new HashMap();

    /**
     * Creates a new item state provider.
     *
     * @param ntMgr         the nodetype manager
     */
    public DefaultItemStateProvider(NodeTypeManagerImpl ntMgr) {
        this.ntMgr = ntMgr;
    }

    /**
     * @see ItemStateProvider#getItemState(org.apache.jackrabbit.core.ItemId)
     */
    public ItemState getItemState(ItemId id) throws NoSuchItemStateException {
        ItemState state = (ItemState) items.get(id);
        if (state == null) {
            throw new NoSuchItemStateException(id.toString());
        }
        return state;
    }

    /**
     * @see ItemStateProvider#hasItemState(org.apache.jackrabbit.core.ItemId)
     */
    public boolean hasItemState(ItemId id) {
        return items.containsKey(id);
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

    /**
     * Adds a node state
     *
     * @param parentId
     * @param name
     * @param id
     * @param nodeType
     * @return
     * @throws ItemStateException
     * @throws RepositoryException
     */
    public VirtualNodeState addNode(ItemId parentId, QName name, String id, QName nodeType)
            throws ItemStateException, RepositoryException {
        if (!(parentId instanceof NodeId)) {
            throw new ItemStateException("Parent must be a node");
        }
        VirtualNodeState parent = (VirtualNodeState) getItemState(parentId);
        return addNode(parent, name, id, nodeType, null);
    }

    public VirtualNodeState addOverlay(NodeState original) throws RepositoryException {
        VirtualNodeState ns= new VirtualNodeState(original.getUUID(), original.getNodeTypeName(), original.getParentUUID());
        ns.setDefinitionId(original.getDefinitionId());
        setPropertyValue(ns, ItemImpl.PROPNAME_PRIMARYTYPE, InternalValue.create(original.getNodeTypeName()));
        ns.setMixinTypeNames(original.getMixinTypeNames());
        items.put(ns.getId(), ns);
        return ns;
    }

    /**
     * Adds a node state
     *
     * @param parent
     * @param name
     * @param id
     * @param nodeTypeName
     * @param mixins
     * @return
     * @throws ItemStateException
     * @throws RepositoryException
     */
    public VirtualNodeState addNode(VirtualNodeState parent, QName name, String id, QName nodeTypeName, QName[] mixins)
            throws ItemStateException, RepositoryException {
        NodeTypeImpl nodeType = nodeTypeName == null ? null : ntMgr.getNodeType(nodeTypeName);
        NodeDefImpl def;
        try {
            def = getApplicableChildNodeDef(parent, name, nodeType == null ? null : nodeType.getQName());
        } catch (RepositoryException re) {
            String msg = "no definition found in parent node's node type for new node";
            throw new ConstraintViolationException(msg, re);
        }
        if (nodeType == null) {
            // use default node type
            nodeType = (NodeTypeImpl) def.getDefaultPrimaryType();
        }

        // default properties
        VirtualNodeState ns = createChildNode(parent, name, def, nodeType, id);
        setPropertyValue(ns, ItemImpl.PROPNAME_PRIMARYTYPE, InternalValue.create(nodeType.getQName()));
        if (mixins != null) {
            ns.setMixinTypeNames(new HashSet(Arrays.asList(mixins)));
        }
        if (getEffectiveNodeType(ns).includesNodeType(NodeTypeRegistry.MIX_REFERENCEABLE)) {
            setPropertyValue(ns, ItemImpl.PROPNAME_UUID, InternalValue.create(ns.getUUID()));
        }
        items.put(ns.getId(), ns);
        return ns;
    }

    /**
     * returns the child node of the given parent
     *
     * @param parent
     * @param name
     * @param index
     * @return
     * @throws NoSuchItemStateException
     */
    public VirtualNodeState getNode(VirtualNodeState parent, QName name, int index) throws NoSuchItemStateException {
        NodeState.ChildNodeEntry entry = parent.getChildNodeEntry(name, index);
        if (entry == null) {
            throw new NoSuchItemStateException(name.toString());
        }
        return (VirtualNodeState) getItemState(new NodeId(entry.getUUID()));
    }

    /**
     * Sets the property value
     *
     * @param name
     * @param value
     * @throws RepositoryException
     */
    public void setPropertyValue(VirtualNodeState parentState, QName name, InternalValue value)
            throws RepositoryException {
        setPropertyValues(parentState, name, value.getType(), new InternalValue[]{value}, false);
    }

    /**
     * Sets the property values
     *
     * @param name
     * @param type
     * @param values
     * @throws RepositoryException
     */
    public void setPropertyValues(VirtualNodeState parentState, QName name, int type, InternalValue[] values)
            throws RepositoryException {
        setPropertyValues(parentState, name, type, values, true);
    }

    /**
     * Sets the property values
     *
     * @param name
     * @param type
     * @param values
     * @throws RepositoryException
     */
    public void setPropertyValues(VirtualNodeState parentState, QName name, int type, InternalValue[] values, boolean multiple)
            throws RepositoryException {
        VirtualPropertyState prop = getOrCreatePropertyState(parentState, name, type, multiple);
        prop.setValues(values);
    }

    /**
     * creates a new child node
     *
     * @param name
     * @param def
     * @param nodeType
     * @param uuid
     * @return
     */
    private VirtualNodeState createChildNode(VirtualNodeState parentState,
                                             QName name, NodeDefImpl def,
                                             NodeTypeImpl nodeType, String uuid) {

        String parentUUID = parentState.getUUID();
        // create a new node state
        VirtualNodeState state = null;
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }
        state = new VirtualNodeState(uuid, nodeType.getQName(), parentUUID);
        state.setDefinitionId(new NodeDefId(def.unwrap()));

        // add new child node entry
        parentState.addChildNodeEntry(name, state.getUUID());

        return state;
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
    private VirtualPropertyState getOrCreatePropertyState(VirtualNodeState parentState, QName name, int type, boolean multiValued)
            throws RepositoryException {

        PropertyId propId = new PropertyId(parentState.getUUID(), name);
        if (hasItemState(propId)) {
            try {
                return (VirtualPropertyState) getItemState(propId);
            } catch (ItemStateException e) {
                throw new RepositoryException("Unable to create property: " + e.toString());
            }
        } else {
            try {
                PropertyDefImpl def = getApplicablePropertyDef(parentState, name, type, multiValued);
                VirtualPropertyState propState = createPropertyState(parentState.getUUID(), name);
                propState.setType(type);
                propState.setDefinitionId(new PropDefId(def.unwrap()));
                parentState.addPropertyEntry(name);
                return propState;
            } catch (ItemStateException e) {
                throw new RepositoryException("Unable to store property: " + e.toString());
            }
        }
    }

    /**
     * Creates a property state
     *
     * @param parentUUID
     * @param propName
     * @return
     * @throws ItemStateException
     */
    private VirtualPropertyState createPropertyState(String parentUUID, QName propName) throws ItemStateException {
        PropertyId id = new PropertyId(parentUUID, propName);
        // check cache
        if (hasItemState(id)) {
            String msg = "there's already a property state instance with id " + id;
            log.error(msg);
            throw new ItemStateException(msg);
        }

        VirtualPropertyState propState = new VirtualPropertyState(propName, parentUUID);
        items.put(id, propState);
        return propState;

    }

    /**
     * retrieves the property definition for the given contraints
     *
     * @param propertyName
     * @param type
     * @param multiValued
     * @return
     * @throws javax.jcr.RepositoryException
     */
    protected PropertyDefImpl getApplicablePropertyDef(VirtualNodeState parentState, QName propertyName,
                                                       int type, boolean multiValued)
            throws RepositoryException {
        PropDef pd = getEffectiveNodeType(parentState).getApplicablePropertyDef(propertyName, type, multiValued);
        return ntMgr.getPropDef(new PropDefId(pd));
    }

    /**
     * Retrieves the node definition for the given contraints.
     *
     * @param nodeName
     * @param nodeTypeName
     * @return
     * @throws RepositoryException
     */
    protected NodeDefImpl getApplicableChildNodeDef(VirtualNodeState parentState, QName nodeName, QName nodeTypeName)
            throws RepositoryException {
        ChildNodeDef cnd = getEffectiveNodeType(parentState).getApplicableChildNodeDef(nodeName, nodeTypeName);
        return ntMgr.getNodeDef(new NodeDefId(cnd));
    }

    /**
     * Returns the effective (i.e. merged and resolved) node type representation
     * of this node's primary and mixin node types.
     *
     * @return the effective node type
     * @throws RepositoryException
     */
    protected EffectiveNodeType getEffectiveNodeType(VirtualNodeState nodeState) throws RepositoryException {
        // build effective node type of mixins & primary type
        NodeTypeRegistry ntReg = ntMgr.getNodeTypeRegistry();
        // existing mixin's
        HashSet set = new HashSet(nodeState.getMixinTypeNames());
        // primary type
        set.add(nodeState.getNodeTypeName());
        try {
            return ntReg.buildEffectiveNodeType((QName[]) set.toArray(new QName[set.size()]));
        } catch (NodeTypeConflictException ntce) {
            String msg = "internal error: failed to build effective node type for node " + nodeState.getUUID();
            throw new RepositoryException(msg, ntce);
        }
    }

}
