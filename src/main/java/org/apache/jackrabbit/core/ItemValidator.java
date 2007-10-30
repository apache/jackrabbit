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

import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.NodeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeConflictException;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.conversion.PathResolver;
import org.apache.jackrabbit.spi.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.nodetype.ConstraintViolationException;
import java.util.Set;

/**
 * Utility class for validating an item against constraints
 * specified by its definition.
 */
public class ItemValidator {

    /**
     * Logger instance for this class
     */
    private static Logger log = LoggerFactory.getLogger(ItemValidator.class);

    /**
     * node type registry
     */
    protected final NodeTypeRegistry ntReg;

    /**
     * hierarchy manager used for generating error msg's
     * that contain human readable paths
     *
     * @see #safeGetJCRPath(ItemId)
     */
    protected final HierarchyManager hierMgr;

    /**
     * Path resolver for outputting user-friendly error messages.
     */
    protected final PathResolver resolver;

    /**
     * Creates a new <code>ItemValidator</code> instance.
     *
     * @param ntReg      node type registry
     * @param hierMgr    hierarchy manager
     * @param resolver   path resolver
     */
    public ItemValidator(NodeTypeRegistry ntReg,
                         HierarchyManager hierMgr,
                         PathResolver resolver) {
        this.ntReg = ntReg;
        this.hierMgr = hierMgr;
        this.resolver = resolver;
    }

    /**
     * Checks whether the given node state satisfies the constraints specified
     * by its primary and mixin node types. The following validations/checks are
     * performed:
     * <ul>
     * <li>check if its node type satisfies the 'required node types' constraint
     * specified in its definition</li>
     * <li>check if all 'mandatory' child items exist</li>
     * <li>for every property: check if the property value satisfies the
     * value constraints specified in the property's definition</li>
     * </ul>
     *
     * @param nodeState state of node to be validated
     * @throws ConstraintViolationException if any of the validations fail
     * @throws RepositoryException          if another error occurs
     */
    public void validate(NodeState nodeState)
            throws ConstraintViolationException, RepositoryException {
        // effective primary node type
        EffectiveNodeType entPrimary =
                ntReg.getEffectiveNodeType(nodeState.getNodeTypeName());
        // effective node type (primary type incl. mixins)
        EffectiveNodeType entPrimaryAndMixins = getEffectiveNodeType(nodeState);
        NodeDef def = ntReg.getNodeDef(nodeState.getDefinitionId());

        // check if primary type satisfies the 'required node types' constraint
        Name[] requiredPrimaryTypes = def.getRequiredPrimaryTypes();
        for (int i = 0; i < requiredPrimaryTypes.length; i++) {
            if (!entPrimary.includesNodeType(requiredPrimaryTypes[i])) {
                String msg = safeGetJCRPath(nodeState.getNodeId())
                        + ": missing required primary type "
                        + requiredPrimaryTypes[i];
                log.debug(msg);
                throw new ConstraintViolationException(msg);
            }
        }
        // mandatory properties
        PropDef[] pda = entPrimaryAndMixins.getMandatoryPropDefs();
        for (int i = 0; i < pda.length; i++) {
            PropDef pd = pda[i];
            if (!nodeState.hasPropertyName(pd.getName())) {
                String msg = safeGetJCRPath(nodeState.getNodeId())
                        + ": mandatory property " + pd.getName()
                        + " does not exist";
                log.debug(msg);
                throw new ConstraintViolationException(msg);
            }
        }
        // mandatory child nodes
        NodeDef[] cnda = entPrimaryAndMixins.getMandatoryNodeDefs();
        for (int i = 0; i < cnda.length; i++) {
            NodeDef cnd = cnda[i];
            if (!nodeState.hasChildNodeEntry(cnd.getName())) {
                String msg = safeGetJCRPath(nodeState.getNodeId())
                        + ": mandatory child node " + cnd.getName()
                        + " does not exist";
                log.debug(msg);
                throw new ConstraintViolationException(msg);
            }
        }
    }

    /**
     * Checks whether the given property state satisfies the constraints
     * specified by its definition. The following validations/checks are
     * performed:
     * <ul>
     * <li>check if the type of the property values does comply with the
     * requiredType specified in the property's definition</li>
     * <li>check if the property values satisfy the value constraints
     * specified in the property's definition</li>
     * </ul>
     *
     * @param propState state of property to be validated
     * @throws ConstraintViolationException if any of the validations fail
     * @throws RepositoryException          if another error occurs
     */
    public void validate(PropertyState propState)
            throws ConstraintViolationException, RepositoryException {
        PropDef def = ntReg.getPropDef(propState.getDefinitionId());
        InternalValue[] values = propState.getValues();
        int type = PropertyType.UNDEFINED;
        for (int i = 0; i < values.length; i++) {
            if (type == PropertyType.UNDEFINED) {
                type = values[i].getType();
            } else if (type != values[i].getType()) {
                throw new ConstraintViolationException(safeGetJCRPath(propState.getPropertyId())
                        + ": inconsistent value types");
            }
            if (def.getRequiredType() != PropertyType.UNDEFINED
                    && def.getRequiredType() != type) {
                throw new ConstraintViolationException(safeGetJCRPath(propState.getPropertyId())
                        + ": requiredType constraint is not satisfied");
            }
        }
        EffectiveNodeType.checkSetPropertyValueConstraints(def, values);
    }

    //-------------------------------------------------< misc. helper methods >
    /**
     * Helper method that builds the effective (i.e. merged and resolved)
     * node type representation of the specified node's primary and mixin
     * node types.
     *
     * @param nodeState
     * @return the effective node type
     * @throws RepositoryException
     */
    public EffectiveNodeType getEffectiveNodeType(NodeState nodeState)
            throws RepositoryException {
        // mixin types
        Set set = nodeState.getMixinTypeNames();
        Name[] types = new Name[set.size() + 1];
        set.toArray(types);
        // primary type
        types[types.length - 1] = nodeState.getNodeTypeName();
        try {
            return ntReg.getEffectiveNodeType(types);
        } catch (NodeTypeConflictException ntce) {
            String msg = "internal error: failed to build effective node type for node "
                    + safeGetJCRPath(nodeState.getNodeId());
            log.debug(msg);
            throw new RepositoryException(msg, ntce);
        }
    }

    /**
     * Failsafe conversion of internal <code>Path</code> to JCR path for use in
     * error messages etc.
     *
     * @param path path to convert
     * @return JCR path
     */
    public String safeGetJCRPath(Path path) {
        try {
            return resolver.getJCRPath(path);
        } catch (NamespaceException e) {
            log.error("failed to convert {} to a JCR path", path);
            // return string representation of internal path as a fallback
            return path.toString();
        }
    }

    /**
     * Failsafe translation of internal <code>ItemId</code> to JCR path for use
     * in error messages etc.
     *
     * @param id id to translate
     * @return JCR path
     */
    public String safeGetJCRPath(ItemId id) {
        try {
            return safeGetJCRPath(hierMgr.getPath(id));
        } catch (ItemNotFoundException e) {
            // return string representation of id as a fallback
            return id.toString();
        } catch (RepositoryException e) {
            log.error(id + ": failed to build path");
            // return string representation of id as a fallback
            return id.toString();
        }
    }
}
