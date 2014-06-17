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
package org.apache.jackrabbit.core.nodetype;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.nodetype.AbstractNodeType;
import org.apache.jackrabbit.value.ValueHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A <code>NodeTypeImpl</code> ...
 */
public class NodeTypeImpl extends AbstractNodeType implements NodeType, NodeTypeDefinition {

    private static Logger log = LoggerFactory.getLogger(NodeTypeImpl.class);

    private final EffectiveNodeType ent;
    private final NodeTypeManagerImpl ntMgr;
    // value factory used for type conversion
    private final ValueFactory valueFactory;
    private final DataStore store;

    /**
     * Package private constructor
     * <p>
     * Creates a valid node type instance. We assume that the node type
     * definition is valid and all referenced node types (supertypes, required
     * node types etc.) do exist and are valid.
     *
     * @param ent          the effective (i.e. merged and resolved) node type
     *                     representation
     * @param ntd          the definition of this node type
     * @param ntMgr        the node type manager associated with this node type
     * @param resolver     the name path resolver of the session.
     * @param valueFactory the value factory of the session.
     * @param store        the data store or <code>null</code> if none is
     *                     configured.
     */
    NodeTypeImpl(EffectiveNodeType ent,
                 QNodeTypeDefinition ntd,
                 NodeTypeManagerImpl ntMgr,
                 NamePathResolver resolver,
                 ValueFactory valueFactory,
                 DataStore store) {
        super(ntd, ntMgr, resolver);
        this.ent = ent;
        this.ntMgr = ntMgr;
        this.valueFactory = valueFactory;
        this.store = store;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNodeType(Name nodeTypeName) {
        return ent.includesNodeType(nodeTypeName);
    }

    /**
     * Checks if this node type is directly or indirectly derived from the
     * specified node type.
     *
     * @param nodeTypeName the name of a node type.
     * @return true if this node type is directly or indirectly derived from the
     *         specified node type, otherwise false.
     */
    public boolean isDerivedFrom(Name nodeTypeName) {
        return !nodeTypeName.equals(ntd.getName()) && ent.includesNodeType(nodeTypeName);
    }

    /**
     * Returns an array containing only those child node definitions of this
     * node type (including the child node definitions inherited from supertypes
     * of this node type) where <code>{@link NodeDefinition#isAutoCreated()}</code>
     * returns <code>true</code>.
     *
     * @return an array of child node definitions.
     * @see NodeDefinition#isAutoCreated
     */
    public NodeDefinition[] getAutoCreatedNodeDefinitions() {
        QNodeDefinition[] cnda = ent.getAutoCreateNodeDefs();
        NodeDefinition[] nodeDefs = new NodeDefinition[cnda.length];
        for (int i = 0; i < cnda.length; i++) {
            nodeDefs[i] = ntMgr.getNodeDefinition(cnda[i]);
        }
        return nodeDefs;
    }

    /**
     * Returns an array containing only those property definitions of this
     * node type (including the property definitions inherited from supertypes
     * of this node type) where <code>{@link PropertyDefinition#isAutoCreated()}</code>
     * returns <code>true</code>.
     *
     * @return an array of property definitions.
     * @see PropertyDefinition#isAutoCreated
     */
    public PropertyDefinition[] getAutoCreatedPropertyDefinitions() {
        QPropertyDefinition[] pda = ent.getAutoCreatePropDefs();
        PropertyDefinition[] propDefs = new PropertyDefinition[pda.length];
        for (int i = 0; i < pda.length; i++) {
            propDefs[i] = ntMgr.getPropertyDefinition(pda[i]);
        }
        return propDefs;
    }

    /**
     * Returns an array containing only those property definitions of this
     * node type (including the property definitions inherited from supertypes
     * of this node type) where <code>{@link PropertyDefinition#isMandatory()}</code>
     * returns <code>true</code>.
     *
     * @return an array of property definitions.
     * @see PropertyDefinition#isMandatory
     */
    public PropertyDefinition[] getMandatoryPropertyDefinitions() {
        QPropertyDefinition[] pda = ent.getMandatoryPropDefs();
        PropertyDefinition[] propDefs = new PropertyDefinition[pda.length];
        for (int i = 0; i < pda.length; i++) {
            propDefs[i] = ntMgr.getPropertyDefinition(pda[i]);
        }
        return propDefs;
    }

    /**
     * Returns an array containing only those child node definitions of this
     * node type (including the child node definitions inherited from supertypes
     * of this node type) where <code>{@link NodeDefinition#isMandatory()}</code>
     * returns <code>true</code>.
     *
     * @return an array of child node definitions.
     * @see NodeDefinition#isMandatory
     */
    public NodeDefinition[] getMandatoryNodeDefinitions() {
        QNodeDefinition[] cnda = ent.getMandatoryNodeDefs();
        NodeDefinition[] nodeDefs = new NodeDefinition[cnda.length];
        for (int i = 0; i < cnda.length; i++) {
            nodeDefs[i] = ntMgr.getNodeDefinition(cnda[i]);
        }
        return nodeDefs;
    }

    /**
     * Returns the <code>Name</code> of this node type.
     *
     * @return the name
     */
    public Name getQName() {
        return ntd.getName();
    }

    /**
     * Returns all <i>inherited</i> supertypes of this node type.
     *
     * @return an array of <code>NodeType</code> objects.
     * @see #getSupertypes
     * @see #getDeclaredSupertypes
     */
    public NodeType[] getInheritedSupertypes() {
        // declared supertypes
        Name[] ntNames = ntd.getSupertypes();
        Set<Name> declared = new HashSet<Name>();
        for (Name ntName : ntNames) {
            declared.add(ntName);
        }
        // all supertypes
        ntNames = ent.getInheritedNodeTypes();

        // filter from all supertypes those that are not declared
        List<NodeType> inherited = new ArrayList<NodeType>();
        for (Name ntName : ntNames) {
            if (!declared.contains(ntName)) {
                try {
                    inherited.add(ntMgr.getNodeType(ntName));
                } catch (NoSuchNodeTypeException e) {
                    // should never get here
                    log.error("undefined supertype", e);
                    return new NodeType[0];
                }
            }
        }

        return inherited.toArray(new NodeType[inherited.size()]);
    }


    //---------------------------------------------------< NodeTypeDefinition >

    /**
     * {@inheritDoc}
     */
    public boolean hasOrderableChildNodes() {
        return ent.hasOrderableChildNodes();
    }

    //-------------------------------------------------------------< NodeType >

    /**
     * {@inheritDoc}
     */
    public NodeType[] getSupertypes() {
        Name[] ntNames = ent.getInheritedNodeTypes();
        NodeType[] supertypes = new NodeType[ntNames.length];
        for (int i = 0; i < ntNames.length; i++) {
            try {
                supertypes[i] = ntMgr.getNodeType(ntNames[i]);
            } catch (NoSuchNodeTypeException e) {
                // should never get here
                log.error("undefined supertype", e);
                return new NodeType[0];
            }
        }
        return supertypes;
    }

    /**
     * {@inheritDoc}
     */
    public NodeDefinition[] getChildNodeDefinitions() {
        QNodeDefinition[] cnda = ent.getAllNodeDefs();
        NodeDefinition[] nodeDefs = new NodeDefinition[cnda.length];
        for (int i = 0; i < cnda.length; i++) {
            nodeDefs[i] = ntMgr.getNodeDefinition(cnda[i]);
        }
        return nodeDefs;
    }

    /**
     * {@inheritDoc}
     */
    public PropertyDefinition[] getPropertyDefinitions() {
        QPropertyDefinition[] pda = ent.getAllPropDefs();
        PropertyDefinition[] propDefs = new PropertyDefinition[pda.length];
        for (int i = 0; i < pda.length; i++) {
            propDefs[i] = ntMgr.getPropertyDefinition(pda[i]);
        }
        return propDefs;
    }

    /**
     * {@inheritDoc}
     */
    public boolean canSetProperty(String propertyName, Value value) {
        if (value == null) {
            // setting a property to null is equivalent of removing it
            return canRemoveItem(propertyName);
        }
        try {
            Name name = resolver.getQName(propertyName);
            QPropertyDefinition def;
            try {
                // try to get definition that matches the given value type
                def = ent.getApplicablePropertyDef(name, value.getType(), false);
            } catch (ConstraintViolationException cve) {
                // fallback: ignore type
                def = ent.getApplicablePropertyDef(name, PropertyType.UNDEFINED, false);
            }
            if (def.isProtected()) {
                return false;
            }
            if (def.isMultiple()) {
                return false;
            }
            int targetType;
            if (def.getRequiredType() != PropertyType.UNDEFINED
                    && def.getRequiredType() != value.getType()) {
                // type conversion required
                targetType = def.getRequiredType();
            } else {
                // no type conversion required
                targetType = value.getType();
            }
            // perform type conversion as necessary and create InternalValue
            // from (converted) Value
            InternalValue internalValue;
            if (targetType != value.getType()) {
                // type conversion required
                Value targetVal = ValueHelper.convert(
                        value, targetType,
                        valueFactory);
                internalValue = InternalValue.create(targetVal, resolver, store);
            } else {
                // no type conversion required
                internalValue = InternalValue.create(value, resolver, store);
            }
            EffectiveNodeType.checkSetPropertyValueConstraints(
                    def, new InternalValue[]{internalValue});
            return true;
        } catch (NameException be) {
            // implementation specific exception, fall through
        } catch (RepositoryException re) {
            // fall through
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean canSetProperty(String propertyName, Value[] values) {
        if (values == null) {
            // setting a property to null is equivalent of removing it
            return canRemoveItem(propertyName);
        }
        try {
            Name name = resolver.getQName(propertyName);
            // determine type of values
            int type = PropertyType.UNDEFINED;
            for (Value value : values) {
                if (value == null) {
                    // skip null values as those would be purged
                    continue;
                }
                if (type == PropertyType.UNDEFINED) {
                    type = value.getType();
                } else if (type != value.getType()) {
                    // inhomogeneous types
                    return false;
                }
            }
            QPropertyDefinition def;
            try {
                // try to get definition that matches the given value type
                def = ent.getApplicablePropertyDef(name, type, true);
            } catch (ConstraintViolationException cve) {
                // fallback: ignore type
                def = ent.getApplicablePropertyDef(name, PropertyType.UNDEFINED, true);
            }

            if (def.isProtected()) {
                return false;
            }
            if (!def.isMultiple()) {
                return false;
            }
            // determine target type
            int targetType;
            if (def.getRequiredType() != PropertyType.UNDEFINED
                    && def.getRequiredType() != type) {
                // type conversion required
                targetType = def.getRequiredType();
            } else {
                // no type conversion required
                targetType = type;
            }

            List<InternalValue> list = new ArrayList<InternalValue>();
            // convert values and compact array (purge null entries)
            for (Value value : values) {
                if (value != null) {
                    // perform type conversion as necessary and create InternalValue
                    // from (converted) Value
                    InternalValue internalValue;
                    if (targetType != type) {
                        // type conversion required
                        Value targetVal = ValueHelper.convert(value, targetType, valueFactory);
                        internalValue = InternalValue.create(targetVal, resolver, store);
                    } else {
                        // no type conversion required
                        internalValue = InternalValue.create(value, resolver, store);
                    }
                    list.add(internalValue);
                }
            }
            InternalValue[] internalValues = list.toArray(new InternalValue[list.size()]);
            EffectiveNodeType.checkSetPropertyValueConstraints(def, internalValues);
            return true;
        } catch (NameException be) {
            // implementation specific exception, fall through
        } catch (RepositoryException re) {
            // fall through
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean canAddChildNode(String childNodeName) {
        try {
            ent.checkAddNodeConstraints(resolver.getQName(childNodeName));
            return true;
        } catch (NameException be) {
            // implementation specific exception, fall through
        } catch (RepositoryException re) {
            // fall through
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean canAddChildNode(String childNodeName, String nodeTypeName) {
        try {
            ent.checkAddNodeConstraints(
                    resolver.getQName(childNodeName),
                    resolver.getQName(nodeTypeName),
                    ntMgr.getNodeTypeRegistry());
            return true;
        } catch (NameException be) {
            // implementation specific exception, fall through
        } catch (RepositoryException re) {
            // fall through
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean canRemoveItem(String itemName) {
        try {
            ent.checkRemoveItemConstraints(resolver.getQName(itemName));
            return true;
        } catch (NameException be) {
            // implementation specific exception, fall through
        } catch (RepositoryException re) {
            // fall through
        }
        return false;
    }

    //--------------------------------------------------< new JSR 283 methods >
    /**
     * Returns <code>true</code> if removing the child node called
     * <code>nodeName</code> is allowed by this node type. Returns
     * <code>false</code> otherwise.
     *
     * @param nodeName The name of the child node
     * @return a boolean
     * @since JCR 2.0
     */
    public boolean canRemoveNode(String nodeName) {
        try {
            ent.checkRemoveNodeConstraints(resolver.getQName(nodeName));
            return true;
        } catch (NameException be) {
            // implementation specific exception, fall through
        } catch (RepositoryException re) {
            // fall through
        }
        return false;
    }

    /**
     * Returns <code>true</code> if removing the property called
     * <code>propertyName</code> is allowed by this node type. Returns
     * <code>false</code> otherwise.
     *
     * @param propertyName The name of the property
     * @return a boolean
     * @since JCR 2.0
     */
    public boolean canRemoveProperty(String propertyName) {
        try {
            ent.checkRemovePropertyConstraints(resolver.getQName(propertyName));
            return true;
        } catch (NameException be) {
            // implementation specific exception, fall through
        } catch (RepositoryException re) {
            // fall through
        }
        return false;
    }
}
