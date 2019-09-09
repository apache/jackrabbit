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
import static org.apache.jackrabbit.spi.commons.name.NameConstants.MIX_SIMPLE_VERSIONABLE;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.MIX_VERSIONABLE;

import java.util.HashSet;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.NodeTypeConflictException;
import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.core.session.SessionWriteOperation;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.nodetype.NodeDefinitionImpl;
import org.apache.jackrabbit.spi.commons.nodetype.PropertyDefinitionImpl;

/**
 * Session operation for adding a mixin type to a node.
 */
class AddMixinOperation implements SessionWriteOperation<Object> {

    private final NodeImpl node;

    private final Name mixinName;

    public AddMixinOperation(NodeImpl node, Name mixinName) {
        this.node = node;
        this.mixinName = mixinName;
    }

    public Object perform(SessionContext context) throws RepositoryException {
        int permissions = Permission.NODE_TYPE_MNGMT;
        // special handling of mix:(simple)versionable. since adding the
        // mixin alters the version storage jcr:versionManagement privilege
        // is required in addition.
        if (MIX_VERSIONABLE.equals(mixinName)
                || MIX_SIMPLE_VERSIONABLE.equals(mixinName)) {
            permissions |= Permission.VERSION_MNGMT;
        }
        context.getItemValidator().checkModify(
                node,
                CHECK_LOCK | CHECK_CHECKED_OUT | CHECK_CONSTRAINTS | CHECK_HOLD,
                permissions);

        NodeTypeManagerImpl ntMgr = context.getNodeTypeManager();
        NodeTypeImpl mixin = ntMgr.getNodeType(mixinName);
        if (!mixin.isMixin()) {
            throw new RepositoryException(
                    context.getJCRName(mixinName) + " is not a mixin node type");
        }

        Name primaryTypeName = node.getNodeState().getNodeTypeName();
        NodeTypeImpl primaryType = ntMgr.getNodeType(primaryTypeName);
        if (primaryType.isDerivedFrom(mixinName)) {
            // new mixin is already included in primary type
            return this;
        }

        // build effective node type of mixin's & primary type in order
        // to detect conflicts
        NodeTypeRegistry ntReg = context.getNodeTypeRegistry();
        EffectiveNodeType entExisting;
        try {
            // existing mixin's
            Set<Name> mixins = new HashSet<Name>(
                    node.getNodeState().getMixinTypeNames());

            // build effective node type representing primary type including
            // existing mixin's
            entExisting = ntReg.getEffectiveNodeType(primaryTypeName, mixins);
            if (entExisting.includesNodeType(mixinName)) {
                // new mixin is already included in existing mixin type(s)
                return this;
            }

            // add new mixin
            mixins.add(mixinName);
            // try to build new effective node type (will throw in case
            // of conflicts)
            ntReg.getEffectiveNodeType(primaryTypeName, mixins);
        } catch (NodeTypeConflictException e) {
            throw new ConstraintViolationException(e.getMessage(), e);
        }

        // do the actual modifications implied by the new mixin;
        // try to revert the changes in case an exception occurs
        try {
            // modify the state of this node
            NodeState thisState =
                (NodeState) node.getOrCreateTransientItemState();
            // add mixin name
            Set<Name> mixins = new HashSet<Name>(thisState.getMixinTypeNames());
            mixins.add(mixinName);
            thisState.setMixinTypeNames(mixins);

            // set jcr:mixinTypes property
            node.setMixinTypesProperty(mixins);

            // add 'auto-create' properties defined in mixin type
            for (PropertyDefinition aPda : mixin.getAutoCreatedPropertyDefinitions()) {
                PropertyDefinitionImpl pd = (PropertyDefinitionImpl) aPda;
                // make sure that the property is not already defined
                // by primary type or existing mixin's
                NodeTypeImpl declaringNT =
                    (NodeTypeImpl) pd.getDeclaringNodeType();
                if (!entExisting.includesNodeType(declaringNT.getQName())) {
                    node.createChildProperty(
                            pd.unwrap().getName(), pd.getRequiredType(), pd);
                }
            }

            // recursively add 'auto-create' child nodes defined in mixin type
            for (NodeDefinition aNda : mixin.getAutoCreatedNodeDefinitions()) {
                NodeDefinitionImpl nd = (NodeDefinitionImpl) aNda;
                // make sure that the child node is not already defined
                // by primary type or existing mixin's
                NodeTypeImpl declaringNT =
                    (NodeTypeImpl) nd.getDeclaringNodeType();
                if (!entExisting.includesNodeType(declaringNT.getQName())) {
                    node.createChildNode(
                            nd.unwrap().getName(),
                            (NodeTypeImpl) nd.getDefaultPrimaryType(),
                            null);
                }
            }
        } catch (RepositoryException re) {
            // try to undo the modifications by removing the mixin
            try {
                node.removeMixin(mixinName);
            } catch (RepositoryException re1) {
                // silently ignore & fall through
            }
            throw re;
        }

        return this;
    }


    //--------------------------------------------------------------< Object >

    /**
     * Returns a string representation of this operation.
     */
    public String toString() {
        return "node.addMixin(" + mixinName + ")";
    }

}