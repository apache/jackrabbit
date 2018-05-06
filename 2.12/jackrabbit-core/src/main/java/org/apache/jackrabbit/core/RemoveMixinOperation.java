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
package org.apache.jackrabbit.core;

import static org.apache.jackrabbit.core.ItemValidator.CHECK_CHECKED_OUT;
import static org.apache.jackrabbit.core.ItemValidator.CHECK_CONSTRAINTS;
import static org.apache.jackrabbit.core.ItemValidator.CHECK_HOLD;
import static org.apache.jackrabbit.core.ItemValidator.CHECK_LOCK;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.MIX_REFERENCEABLE;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.NodeTypeConflictException;
import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.core.session.SessionWriteOperation;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.SessionItemStateManager;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.nodetype.NodeDefinitionImpl;
import org.apache.jackrabbit.spi.commons.nodetype.PropertyDefinitionImpl;
import org.apache.jackrabbit.value.ValueHelper;

/**
 * Session operation for removing a mixin type from a node.
 */
class RemoveMixinOperation implements SessionWriteOperation<Object> {

    private final NodeImpl node;

    private final Name mixinName;

    public RemoveMixinOperation(NodeImpl node, Name mixinName) {
        this.node = node;
        this.mixinName = mixinName;
    }

    public Object perform(SessionContext context) throws RepositoryException {
        SessionImpl session = context.getSessionImpl();
        ItemManager itemMgr = context.getItemManager();
        SessionItemStateManager stateMgr = context.getItemStateManager();

        context.getItemValidator().checkModify(
                node,
                CHECK_LOCK | CHECK_CHECKED_OUT | CHECK_CONSTRAINTS | CHECK_HOLD,
                Permission.NODE_TYPE_MNGMT);

        // check if mixin is assigned
        NodeState state = node.getNodeState();
        if (!state.getMixinTypeNames().contains(mixinName)) {
            throw new NoSuchNodeTypeException(
                    "Mixin " + context.getJCRName(mixinName)
                    + " not included in " + node);
        }

        NodeTypeManagerImpl ntMgr = context.getNodeTypeManager();
        NodeTypeRegistry ntReg = context.getNodeTypeRegistry();

        // build effective node type of remaining mixin's & primary type
        Set<Name> remainingMixins = new HashSet<Name>(state.getMixinTypeNames());
        // remove name of target mixin
        remainingMixins.remove(mixinName);
        EffectiveNodeType entResulting;
        try {
            // build effective node type representing primary type
            // including remaining mixin's
            entResulting = ntReg.getEffectiveNodeType(
                    state.getNodeTypeName(), remainingMixins);
        } catch (NodeTypeConflictException e) {
            throw new ConstraintViolationException(e.getMessage(), e);
        }

        // mix:referenceable needs special handling because it has
        // special semantics:
        // it can only be removed if there no more references to this node
        NodeTypeImpl mixin = ntMgr.getNodeType(mixinName);
        if (isReferenceable(mixin)
                && !entResulting.includesNodeType(MIX_REFERENCEABLE)) {
            if (node.getReferences().hasNext()) {
                throw new ConstraintViolationException(
                        mixinName + " can not be removed:"
                        + " the node is being referenced through at least"
                        + " one property of type REFERENCE");
            }
        }

        // mix:lockable: the mixin cannot be removed if the node is
        // currently locked even if the editing session is the lock holder.
        if ((NameConstants.MIX_LOCKABLE.equals(mixinName)
                || mixin.isDerivedFrom(NameConstants.MIX_LOCKABLE))
                && !entResulting.includesNodeType(NameConstants.MIX_LOCKABLE)
                && node.isLocked()) {
            throw new ConstraintViolationException(
                    mixinName + " can not be removed: the node is locked.");
        }

        NodeState thisState = (NodeState) node.getOrCreateTransientItemState();

        // collect information about properties and nodes which require further
        // action as a result of the mixin removal; we need to do this *before*
        // actually changing the assigned mixin types, otherwise we wouldn't
        // be able to retrieve the current definition of an item.
        Map<PropertyId, PropertyDefinition> affectedProps =
            new HashMap<PropertyId, PropertyDefinition>();
        Map<ChildNodeEntry, NodeDefinition> affectedNodes =
            new HashMap<ChildNodeEntry, NodeDefinition>();
        try {
            Set<Name> names = thisState.getPropertyNames();
            for (Name propName : names) {
                PropertyId propId =
                    new PropertyId(thisState.getNodeId(), propName);
                PropertyState propState =
                    (PropertyState) stateMgr.getItemState(propId);
                PropertyDefinition oldDef = itemMgr.getDefinition(propState);
                // check if property has been defined by mixin type
                // (or one of its supertypes)
                NodeTypeImpl declaringNT =
                    (NodeTypeImpl) oldDef.getDeclaringNodeType();
                if (!entResulting.includesNodeType(declaringNT.getQName())) {
                    // the resulting effective node type doesn't include the
                    // node type that declared this property
                    affectedProps.put(propId, oldDef);
                }
            }

            List<ChildNodeEntry> entries = thisState.getChildNodeEntries();
            for (ChildNodeEntry entry : entries) {
                NodeState nodeState =
                    (NodeState) stateMgr.getItemState(entry.getId());
                NodeDefinition oldDef = itemMgr.getDefinition(nodeState);
                // check if node has been defined by mixin type
                // (or one of its supertypes)
                NodeTypeImpl declaringNT =
                    (NodeTypeImpl) oldDef.getDeclaringNodeType();
                if (!entResulting.includesNodeType(declaringNT.getQName())) {
                    // the resulting effective node type doesn't include the
                    // node type that declared this child node
                    affectedNodes.put(entry, oldDef);
                }
            }
        } catch (ItemStateException e) {
            throw new RepositoryException(
                    "Failed to determine effect of removing mixin "
                    + context.getJCRName(mixinName), e);
        }

        // modify the state of this node
        thisState.setMixinTypeNames(remainingMixins);
        // set jcr:mixinTypes property
        node.setMixinTypesProperty(remainingMixins);

        // process affected nodes & properties:
        // 1. try to redefine item based on the resulting
        //    new effective node type (see JCR-2130)
        // 2. remove item if 1. fails
        boolean success = false;
        try {
        	for (Map.Entry<PropertyId, PropertyDefinition> entry : affectedProps.entrySet()) {
        		PropertyId id = entry.getKey();
                PropertyImpl prop = (PropertyImpl) itemMgr.getItem(id);
                PropertyDefinition oldDef = entry.getValue();

                if (oldDef.isProtected()) {
                    // remove 'orphaned' protected properties immediately
                    node.removeChildProperty(id.getName());
                    continue;
                }
                // try to find new applicable definition first and
                // redefine property if possible (JCR-2130)
                try {
                    PropertyDefinitionImpl newDef =
                        node.getApplicablePropertyDefinition(
                            id.getName(), prop.getType(),
                            oldDef.isMultiple(), false);
                    if (newDef.getRequiredType() != PropertyType.UNDEFINED
                            && newDef.getRequiredType() != prop.getType()) {
                        // value conversion required
                        if (oldDef.isMultiple()) {
                            // convert value
                            Value[] values =
                                ValueHelper.convert(
                                        prop.getValues(),
                                        newDef.getRequiredType(),
                                        session.getValueFactory());
                            // redefine property
                            prop.onRedefine(newDef.unwrap());
                            // set converted values
                            prop.setValue(values);
                        } else {
                            // convert value
                            Value value =
                                ValueHelper.convert(
                                        prop.getValue(),
                                        newDef.getRequiredType(),
                                        session.getValueFactory());
                            // redefine property
                            prop.onRedefine(newDef.unwrap());
                            // set converted values
                            prop.setValue(value);
                        }
                    } else {
                        // redefine property
                        prop.onRedefine(newDef.unwrap());
                    }
                } catch (ValueFormatException vfe) {
                    // value conversion failed, remove it
                    node.removeChildProperty(id.getName());
                } catch (ConstraintViolationException cve) {
                    // no suitable definition found for this property,
                    // remove it
                    node.removeChildProperty(id.getName());
                }
            }

            for (ChildNodeEntry entry : affectedNodes.keySet()) {
                NodeState nodeState = (NodeState) stateMgr.getItemState(entry.getId());
                NodeImpl childNode = (NodeImpl) itemMgr.getItem(entry.getId());
                NodeDefinition oldDef = affectedNodes.get(entry);

                if (oldDef.isProtected()) {
                    // remove 'orphaned' protected child node immediately
                    node.removeChildNode(entry.getId());
                    continue;
                }

                // try to find new applicable definition first and
                // redefine node if possible (JCR-2130)
                try {
                    NodeDefinitionImpl newDef =
                        node.getApplicableChildNodeDefinition(
                                entry.getName(),
                                nodeState.getNodeTypeName());
                    // redefine node
                    childNode.onRedefine(newDef.unwrap());
                } catch (ConstraintViolationException cve) {
                    // no suitable definition found for this child node,
                    // remove it
                    node.removeChildNode(entry.getId());
                }
            }
            success = true;
        } catch (ItemStateException e) {
            throw new RepositoryException(
                    "Failed to clean up child items defined by removed mixin "
                    + context.getJCRName(mixinName), e);
        } finally {
            if (!success) {
                // TODO JCR-1914: revert any changes made so far
            }
        }

        return this;
    }

    private boolean isReferenceable(NodeTypeImpl mixin) {
        return MIX_REFERENCEABLE.equals(mixinName)
            || mixin.isDerivedFrom(MIX_REFERENCEABLE);
    }

    //--------------------------------------------------------------< Object >

    /**
     * Returns a string representation of this operation.
     */
    public String toString() {
        return "node.removeMixin(" + mixinName + ")";
    }

}