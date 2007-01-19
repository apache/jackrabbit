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

import org.apache.jackrabbit.name.IllegalNameException;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.NoPrefixDeclaredException;
import org.apache.jackrabbit.name.UnknownPrefixException;
import org.apache.jackrabbit.name.NameException;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.NameFormat;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.value.ValueHelper;
import org.apache.jackrabbit.value.ValueFormat;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import java.util.ArrayList;

/**
 * A <code>NodeTypeImpl</code> ...
 */
public class NodeTypeImpl implements NodeType {

    private static Logger log = LoggerFactory.getLogger(NodeTypeImpl.class);

    private final QNodeTypeDefinition ntd;
    private final EffectiveNodeType ent;
    private final NodeTypeManagerImpl ntMgr;
    /**
     * Namespace resolver used to translate qualified names to JCR names
     */
    private final NamespaceResolver nsResolver;
    /**
     * ValueFactory used to convert JCR values to qualified ones in order to
     * determine whether a property specified by name and value(s) would be allowed.
     *
     * @see NodeType#canSetProperty(String, Value)
     * @see NodeType#canSetProperty(String, Value[])
     */
    private final ValueFactory valueFactory;
    /**
     * ValueFactory used to convert JCR values to qualified ones in order to
     * determine value constraints within the NodeType interface.
     */
    private final QValueFactory qValueFactory;

    /**
     * Package private constructor
     * <p/>
     * Creates a valid node type instance.
     * We assume that the node type definition is valid and all referenced
     * node types (supertypes, required node types etc.) do exist and are valid.
     *
     * @param ent        the effective (i.e. merged and resolved) node type representation
     * @param ntd        the definition of this node type
     * @param ntMgr      the node type manager associated with this node type
     * @param nsResolver namespace resolver
     */
    NodeTypeImpl(EffectiveNodeType ent, QNodeTypeDefinition ntd,
                 NodeTypeManagerImpl ntMgr, NamespaceResolver nsResolver,
                 ValueFactory valueFactory, QValueFactory qValueFactory) {
        this.ent = ent;
        this.ntMgr = ntMgr;
        this.nsResolver = nsResolver;
        this.valueFactory = valueFactory;
        this.qValueFactory = qValueFactory;
        this.ntd = ntd;
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
    private QPropertyDefinition getApplicablePropDef(QName propertyName, int type, boolean multiValued)
            throws RepositoryException {
        return ent.getApplicablePropertyDefinition(propertyName, type, multiValued);
    }

    /**
     * Checks if this node type's name equals the given name or if this nodetype
     * is directly or indirectly derived from the specified node type.
     *
     * @param nodeTypeName
     * @return true if this node type is equal or directly or indirectly derived
     * from the specified node type, otherwise false.
     */
    public boolean isNodeType(QName nodeTypeName) {
        return getQName().equals(nodeTypeName) ||  ent.includesNodeType(nodeTypeName);
    }

    /**
     * Tests if the value constraints defined in the property definition
     * <code>def</code> are satisfied by the the specified <code>values</code>.
     * <p/>
     * Note that the <i>protected</i> flag is not checked. Also note that no
     * type conversions are attempted if the type of the given values does not
     * match the required type as specified in the given definition.
     *
     * @param def    The definiton of the property
     * @param values An array of <code>QValue</code> objects.
     * @throws ConstraintViolationException
     * @throws RepositoryException
     */
    private static void checkSetPropertyValueConstraints(QPropertyDefinition def,
                                                        QValue[] values)
            throws ConstraintViolationException, RepositoryException {
        ValueConstraint.checkValueConstraints(def, values);
    }

    /**
     * Returns the 'internal', i.e. the fully qualified name.
     *
     * @return the qualified name
     */
    private QName getQName() {
        return ntd.getQName();
    }

    //-----------------------------------------------------------< NodeType >---
    /**
     * {@inheritDoc}
     */
    public String getName() {
        try {
            return NameFormat.format(ntd.getQName(), nsResolver);
        } catch (NoPrefixDeclaredException npde) {
            // should never get here
            log.error("encountered unregistered namespace in node type name", npde);
            return ntd.getQName().toString();
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getPrimaryItemName() {
        try {
            QName piName = ntd.getPrimaryItemName();
            if (piName != null) {
                return NameFormat.format(piName, nsResolver);
            } else {
                return null;
            }
        } catch (NoPrefixDeclaredException npde) {
            // should never get here
            log.error("encountered unregistered namespace in name of primary item", npde);
            return ntd.getQName().toString();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMixin() {
        return ntd.isMixin();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isNodeType(String nodeTypeName) {
        QName ntName;
        try {
            ntName = NameFormat.parse(nodeTypeName, nsResolver);
        } catch (IllegalNameException ine) {
            log.warn("invalid node type name: " + nodeTypeName, ine);
            return false;
        } catch (UnknownPrefixException upe) {
            log.warn("invalid node type name: " + nodeTypeName, upe);
            return false;
        }
        return isNodeType(ntName);
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasOrderableChildNodes() {
        return ntd.hasOrderableChildNodes();
    }

    /**
     * {@inheritDoc}
     */
    public NodeType[] getSupertypes() {
        QName[] ntNames = ent.getInheritedNodeTypes();
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
    public NodeType[] getDeclaredSupertypes() {
        QName[] ntNames = ntd.getSupertypes();
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
    public NodeDefinition[] getDeclaredChildNodeDefinitions() {
        QNodeDefinition[] cnda = ntd.getChildNodeDefs();
        NodeDefinition[] nodeDefs = new NodeDefinition[cnda.length];
        for (int i = 0; i < cnda.length; i++) {
            nodeDefs[i] = ntMgr.getNodeDefinition(cnda[i]);
        }
        return nodeDefs;
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
            QName name = NameFormat.parse(propertyName, nsResolver);
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
                v =  ValueHelper.convert(value, def.getRequiredType(), valueFactory);
            } else {
                // no type conversion required
                v = value;
            }
            // create QValue from Value
            QValue qValue = ValueFormat.getQValue(v, nsResolver, qValueFactory);
            checkSetPropertyValueConstraints(def, new QValue[]{qValue});
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
            QName name = NameFormat.parse(propertyName, nsResolver);
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

            ArrayList list = new ArrayList();
            // convert values and compact array (purge null entries)
            for (int i = 0; i < values.length; i++) {
                if (values[i] != null) {
                    // create QValue from Value and perform
                    // type conversion as necessary
                    Value v = ValueHelper.convert(values[i], targetType, valueFactory);
                    QValue qValue = ValueFormat.getQValue(v, nsResolver, qValueFactory);
                    list.add(qValue);
                }
            }
            QValue[] internalValues = (QValue[]) list.toArray(new QValue[list.size()]);
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
     * {@inheritDoc}
     */
    public boolean canAddChildNode(String childNodeName) {
        try {
            ent.checkAddNodeConstraints(NameFormat.parse(childNodeName, nsResolver),
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
    public boolean canAddChildNode(String childNodeName, String nodeTypeName) {
        try {
            ent.checkAddNodeConstraints(NameFormat.parse(childNodeName, nsResolver),
                NameFormat.parse(nodeTypeName, nsResolver), ntMgr.getNodeTypeRegistry());
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
            ent.checkRemoveItemConstraints(NameFormat.parse(itemName, nsResolver));
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
    public PropertyDefinition[] getDeclaredPropertyDefinitions() {
        QPropertyDefinition[] pda = ntd.getPropertyDefs();
        PropertyDefinition[] propDefs = new PropertyDefinition[pda.length];
        for (int i = 0; i < pda.length; i++) {
            propDefs[i] = ntMgr.getPropertyDefinition(pda[i]);
        }
        return propDefs;
    }
}
