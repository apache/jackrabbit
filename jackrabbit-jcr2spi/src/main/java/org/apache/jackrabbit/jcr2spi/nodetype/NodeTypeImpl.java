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
package org.apache.jackrabbit.jcr2spi.nodetype;

import java.util.ArrayList;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.jackrabbit.jcr2spi.ManagerProvider;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.nodetype.AbstractNodeType;
import org.apache.jackrabbit.spi.commons.nodetype.constraint.ValueConstraint;
import org.apache.jackrabbit.spi.commons.value.ValueFormat;
import org.apache.jackrabbit.value.ValueHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>NodeTypeImpl</code> ...
 */
public class NodeTypeImpl extends AbstractNodeType implements NodeTypeDefinition {

    private static Logger log = LoggerFactory.getLogger(NodeTypeImpl.class);

    private final EffectiveNodeType ent;
    private final NodeTypeManagerImpl ntMgr;
    private final ManagerProvider mgrProvider;

    /**
     * Package private constructor
     * <p>
     * Creates a valid node type instance.
     * We assume that the node type definition is valid and all referenced
     * node types (supertypes, required node types etc.) do exist and are valid.
     *
     * @param ent        the effective (i.e. merged and resolved) node type representation
     * @param ntd        the definition of this node type
     * @param ntMgr      the node type manager associated with this node type
     * @param mgrProvider the manager provider
     */
    NodeTypeImpl(EffectiveNodeType ent, QNodeTypeDefinition ntd,
                 NodeTypeManagerImpl ntMgr, ManagerProvider mgrProvider) {
        super(ntd, ntMgr, mgrProvider.getNamePathResolver());
        this.ent = ent;
        this.ntMgr = ntMgr;
        this.mgrProvider = mgrProvider;
    }

    private NamePathResolver resolver() {
        return mgrProvider.getNamePathResolver();
    }

    private ItemDefinitionProvider definitionProvider() {
        return mgrProvider.getItemDefinitionProvider();
    }

    /**
     * Returns the applicable property definition for a property with the
     * specified name and type.
     *
     * @param propertyName
     * @param type
     * @param multiValued
     * @return
     * @throws RepositoryException if no applicable property definition
     *                             could be found
     */
    private QPropertyDefinition getApplicablePropDef(Name propertyName, int type, boolean multiValued)
            throws RepositoryException {
        return definitionProvider().getQPropertyDefinition(ntd.getName(), propertyName, type, multiValued);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNodeType(Name nodeTypeName) {
        return ent.includesNodeType(nodeTypeName);
    }

    /**
     * Tests if the value constraints defined in the property definition
     * <code>def</code> are satisfied by the the specified <code>values</code>.
     * <p>
     * Note that the <i>protected</i> flag is not checked. Also note that no
     * type conversions are attempted if the type of the given values does not
     * match the required type as specified in the given definition.
     *
     * @param def    The definition of the property
     * @param values An array of <code>QValue</code> objects.
     * @throws ConstraintViolationException If a constraint is violated.
     * @throws RepositoryException If another error occurs.
     */
    private static void checkSetPropertyValueConstraints(QPropertyDefinition def,
                                                        QValue[] values)
            throws ConstraintViolationException, RepositoryException {
        ValueConstraint.checkValueConstraints(def, values);
    }

    //-------------------------------------------------< NodeTypeDefinition >---

    /**
     * @see javax.jcr.nodetype.NodeTypeDefinition#hasOrderableChildNodes()
     */
    public boolean hasOrderableChildNodes() {
        return ntd.hasOrderableChildNodes();
    }

    //-----------------------------------------------------------< NodeType >---

    /**
     * @see javax.jcr.nodetype.NodeType#getSupertypes()
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
     * @see javax.jcr.nodetype.NodeType#getChildNodeDefinitions()
     */
    public NodeDefinition[] getChildNodeDefinitions() {
        QNodeDefinition[] cnda = ent.getAllQNodeDefinitions();
        NodeDefinition[] nodeDefs = new NodeDefinition[cnda.length];
        for (int i = 0; i < cnda.length; i++) {
            nodeDefs[i] = ntMgr.getNodeDefinition(cnda[i]);
        }
        return nodeDefs;
    }

    /**
     * @see javax.jcr.nodetype.NodeType#getPropertyDefinitions()
     */
    public PropertyDefinition[] getPropertyDefinitions() {
        QPropertyDefinition[] pda = ent.getAllQPropertyDefinitions();
        PropertyDefinition[] propDefs = new PropertyDefinition[pda.length];
        for (int i = 0; i < pda.length; i++) {
            propDefs[i] = ntMgr.getPropertyDefinition(pda[i]);
        }
        return propDefs;
    }

    /**
     * @see javax.jcr.nodetype.NodeType#canSetProperty(String, Value)
     */
    public boolean canSetProperty(String propertyName, Value value) {
        if (value == null) {
            // setting a property to null is equivalent of removing it
            return canRemoveItem(propertyName);
        }
        try {
            Name name = resolver().getQName(propertyName);
            QPropertyDefinition def;
            try {
                // try to get definition that matches the given value type
                def = getApplicablePropDef(name, value.getType(), false);
            } catch (ConstraintViolationException cve) {
                // fallback: ignore type
                def = getApplicablePropDef(name, PropertyType.UNDEFINED, false);
            }
            if (def.isProtected()) {
                return false;
            }
            if (def.isMultiple()) {
                return false;
            }
            Value v;
            if (def.getRequiredType() != PropertyType.UNDEFINED
                    && def.getRequiredType() != value.getType()) {
                // type conversion required
                v =  ValueHelper.convert(value, def.getRequiredType(), mgrProvider.getJcrValueFactory());
            } else {
                // no type conversion required
                v = value;
            }
            // create QValue from Value
            QValue qValue = ValueFormat.getQValue(v, resolver(), mgrProvider.getQValueFactory());
            checkSetPropertyValueConstraints(def, new QValue[]{qValue});
            return true;
        } catch (NameException re) {
            // fall through
        } catch (RepositoryException e) {
            // fall through
        }
        return false;
    }

    /**
     * @see javax.jcr.nodetype.NodeType#canSetProperty(String, Value[])
     */
    public boolean canSetProperty(String propertyName, Value[] values) {
        if (values == null) {
            // setting a property to null is equivalent of removing it
            return canRemoveItem(propertyName);
        }
        try {
            Name name = resolver().getQName(propertyName);
            // determine type of values
            int type = PropertyType.UNDEFINED;
            for (int i = 0; i < values.length; i++) {
                if (values[i] == null) {
                    // skip null values as those would be purged
                    continue;
                }
                if (type == PropertyType.UNDEFINED) {
                    type = values[i].getType();
                } else if (type != values[i].getType()) {
                    // inhomogeneous types
                    return false;
                }
            }
            QPropertyDefinition def;
            try {
                // try to get definition that matches the given value type
                def = getApplicablePropDef(name, type, true);
            } catch (ConstraintViolationException cve) {
                // fallback: ignore type
                def = getApplicablePropDef(name, PropertyType.UNDEFINED, true);
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

            ArrayList<QValue> list = new ArrayList<QValue>();
            // convert values and compact array (purge null entries)
            for (int i = 0; i < values.length; i++) {
                if (values[i] != null) {
                    // create QValue from Value and perform
                    // type conversion as necessary
                    Value v = ValueHelper.convert(values[i], targetType, mgrProvider.getJcrValueFactory());
                    QValue qValue = ValueFormat.getQValue(v, resolver(), mgrProvider.getQValueFactory());
                    list.add(qValue);
                }
            }
            QValue[] internalValues = list.toArray(new QValue[list.size()]);
            checkSetPropertyValueConstraints(def, internalValues);
            return true;
        } catch (NameException be) {
            // implementation specific exception, fall through
        } catch (RepositoryException re) {
            // fall through
        }
        return false;
    }

    /**
     * @see javax.jcr.nodetype.NodeType#canAddChildNode(String)
     */
    public boolean canAddChildNode(String childNodeName) {
        try {
            ent.checkAddNodeConstraints(resolver().getQName(childNodeName), definitionProvider());
            return true;
        } catch (NameException be) {
            // implementation specific exception, fall through
        } catch (RepositoryException re) {
            // fall through
        }
        return false;
    }

    /**
     * @see javax.jcr.nodetype.NodeType#canAddChildNode(String, String)
     */
    public boolean canAddChildNode(String childNodeName, String nodeTypeName) {
        try {
            Name ntName = resolver().getQName(nodeTypeName);
            QNodeTypeDefinition def = ntMgr.getNodeTypeDefinition(ntName);
            ent.checkAddNodeConstraints(resolver().getQName(childNodeName), def, definitionProvider());
            return true;
        } catch (NameException be) {
            // implementation specific exception, fall through
        } catch (RepositoryException re) {
            // fall through
        }
        return false;
    }

    /**
     * @see javax.jcr.nodetype.NodeType#canRemoveItem(String)
     */
    public boolean canRemoveItem(String itemName) {
        try {
            ent.checkRemoveItemConstraints(resolver().getQName(itemName));
            return true;
        } catch (NameException be) {
            // implementation specific exception, fall through
        } catch (RepositoryException re) {
            // fall through
        }
        return false;
    }

    /**
     * @see javax.jcr.nodetype.NodeType#canRemoveNode(String)
     */
    public boolean canRemoveNode(String nodeName) {
        Name name;
        try {
            name = resolver().getQName(nodeName);
        } catch (RepositoryException e) {
            // should never get here
            log.warn("Unable to determine if there are any remove constraints for a node with name " + nodeName);
            return false;
        }
        return !ent.hasRemoveNodeConstraint(name);

    }

    /**
     * @see javax.jcr.nodetype.NodeType#canRemoveProperty(String)
     */
    public boolean canRemoveProperty(String propertyName) {
        Name name;
        try {
            name = resolver().getQName(propertyName);
        } catch (RepositoryException e) {
            // should never get here
            log.warn("Unable to determine if there are any remove constraints for a property with name " + propertyName);
            return false;
        }
        return !ent.hasRemovePropertyConstraint(name);
    }
}
