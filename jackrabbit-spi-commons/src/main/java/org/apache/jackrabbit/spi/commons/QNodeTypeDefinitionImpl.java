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
package org.apache.jackrabbit.spi.commons;

import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.value.ValueFormat;

import javax.jcr.PropertyType;
import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.nodetype.NodeDefinition;
import java.util.Collection;
import java.util.HashSet;
import java.util.Collections;
import java.io.Serializable;

/**
 * <code>QNodeTypeDefinitionImpl</code> implements a serializable qualified node
 * type definition.
 */
public class QNodeTypeDefinitionImpl implements QNodeTypeDefinition, Serializable {

    /**
     * The name of the node definition.
     */
    private final Name name;

    /**
     * The names of the declared super types of this node type definition.
     */
    private final Name[] supertypes;

    /**
     * The names of the supported mixins on this node type (or <code>null</code>)
     */
    private final Name[] supportedMixins;

    /**
     * Indicates whether this is a mixin node type definition.
     */
    private final boolean isMixin;

    /**
     * Indicates whether this is an abstract node type definition.
     */
    private final boolean isAbstract;

    /**
     * Indicates whether this is a queryable node type definition.
     */
    private final boolean isQueryable;

    /**
     * Indicates whether this node type definition has orderable child nodes.
     */
    private final boolean hasOrderableChildNodes;

    /**
     * The name of the primary item or <code>null</code> if none is defined.
     */
    private final Name primaryItemName;

    /**
     * The list of property definitions.
     */
    private final QPropertyDefinition[] propertyDefs;

    /**
     * The list of child node definitions.
     */
    private final QNodeDefinition[] childNodeDefs;

    /**
     * Unmodifiable collection of dependent node type <code>Name</code>s.
     * @see #getDependencies()
     */
    private transient Collection dependencies;

    /**
     * Copy constructor.
     *
     * @param nt the qualified node type definition.
     */
    public QNodeTypeDefinitionImpl(QNodeTypeDefinition nt) {
        this(nt.getName(), nt.getSupertypes(), nt.getSupportedMixinTypes(),
                nt.isMixin(), nt.isAbstract(), nt.isQueryable(),
                nt.hasOrderableChildNodes(), nt.getPrimaryItemName(),
                nt.getPropertyDefs(), nt.getChildNodeDefs());
    }

    /**
     * Creates a new serializable qualified node type definition.
     *
     * @param name                   the name of the node type
     * @param supertypes             the names of the supertypes
     * @param isMixin                if this is a mixin node type
     * @param hasOrderableChildNodes if this node type has orderable child
     *                               nodes.
     * @param primaryItemName        the name of the primary item, or
     *                               <code>null</code>.
     * @param declaredPropDefs       the declared property definitions.
     * @param declaredNodeDefs       the declared child node definitions.
     * @deprecated use {@link #QNodeTypeDefinitionImpl(Name, Name[], Name[], boolean, boolean, Name, QPropertyDefinition[], QNodeDefinition[])}
     */
    public QNodeTypeDefinitionImpl(Name name,
                                   Name[] supertypes,
                                   boolean isMixin,
                                   boolean hasOrderableChildNodes,
                                   Name primaryItemName,
                                   QPropertyDefinition[] declaredPropDefs,
                                   QNodeDefinition[] declaredNodeDefs) {
        this(name, supertypes, null, isMixin, false, false,
                hasOrderableChildNodes, primaryItemName,
                getSerializablePropertyDefs(declaredPropDefs),
                getSerializableNodeDefs(declaredNodeDefs));
    }

    /**
     * Creates a new serializable qualified node type definition. Same as
     * {@link #QNodeTypeDefinitionImpl(Name, Name[], Name[], boolean, boolean, boolean, boolean, Name, QPropertyDefinition[], QNodeDefinition[])}
     * but using <code>false</code> for both {@link #isAbstract()} and {@link #isQueryable)}.
     *
     * @param name                   the name of the node type
     * @param supertypes             the names of the supertypes
     * @param supportedMixins        the names of supported mixins (or <code>null</code>)
     * @param isMixin                if this is a mixin node type
     * @param hasOrderableChildNodes if this node type has orderable child
     *                               nodes.
     * @param primaryItemName        the name of the primary item, or
     *                               <code>null</code>.
     * @param declaredPropDefs       the declared property definitions.
     * @param declaredNodeDefs       the declared child node definitions.
     *
     */
    public QNodeTypeDefinitionImpl(Name name,
                                   Name[] supertypes,
                                   Name[] supportedMixins,
                                   boolean isMixin,
                                   boolean hasOrderableChildNodes,
                                   Name primaryItemName,
                                   QPropertyDefinition[] declaredPropDefs,
                                   QNodeDefinition[] declaredNodeDefs) {
        this(name, supertypes, supportedMixins, isMixin, false, false,
                hasOrderableChildNodes, primaryItemName,
                getSerializablePropertyDefs(declaredPropDefs),
                getSerializableNodeDefs(declaredNodeDefs));
    }

    /**
     * Creates a new serializable qualified node type definition.
     *
     * @param name                   the name of the node type
     * @param supertypes             the names of the supertypes
     * @param supportedMixins        the names of supported mixins (or <code>null</code>)
     * @param isMixin                if this is a mixin node type
     * @param isAbstract             if this is an abstract node type definition.
     * @param isQueryable            if this is a queryable node type definition.
     * @param hasOrderableChildNodes if this node type has orderable child
     *                               nodes.
     * @param primaryItemName        the name of the primary item, or
     *                               <code>null</code>.
     * @param declaredPropDefs       the declared property definitions.
     * @param declaredNodeDefs       the declared child node definitions.
     */
    public QNodeTypeDefinitionImpl(Name name,
                                   Name[] supertypes,
                                   Name[] supportedMixins,
                                   boolean isMixin,
                                   boolean isAbstract,
                                   boolean isQueryable,
                                   boolean hasOrderableChildNodes,
                                   Name primaryItemName,
                                   QPropertyDefinition[] declaredPropDefs,
                                   QNodeDefinition[] declaredNodeDefs) {
        this.name = name;
        this.supertypes = supertypes;
        this.supportedMixins = supportedMixins;
        this.isMixin = isMixin;
        this.isAbstract = isAbstract;
        this.isQueryable = isQueryable;
        this.hasOrderableChildNodes = hasOrderableChildNodes;
        this.primaryItemName = primaryItemName;
        this.propertyDefs = getSerializablePropertyDefs(declaredPropDefs);
        this.childNodeDefs = getSerializableNodeDefs(declaredNodeDefs);
    }

    /**
     * Createa a new <code>QNodeTypeDefinitionImpl</code> from a JCR
     * NodeType definition.
     *
     * @param def
     * @param resolver
     * @param qValueFactory
     * @throws RepositoryException
     */
    public QNodeTypeDefinitionImpl(NodeTypeDefinition def,
                                   NamePathResolver resolver,
                                   QValueFactory qValueFactory) throws RepositoryException {
        this(resolver.getQName(def.getName()),
                getNames(def.getDeclaredSupertypeNames(), resolver), null, def.isMixin(),
                def.isAbstract(), def.isQueryable(), def.hasOrderableChildNodes(),
                resolver.getQName(def.getPrimaryItemName()),
                createQPropertyDefinitions(def.getDeclaredPropertyDefinitions(), resolver, qValueFactory),
                createQNodeDefinitions(def.getDeclaredChildNodeDefinitions(), resolver));
    }

    //------------------------------------------------< QNodeTypeDefinition >---
    /**
     * {@inheritDoc}
     */
    public Name getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public Name[] getSupertypes() {
        Name[] sTypes = new Name[supertypes.length];
        System.arraycopy(supertypes, 0, sTypes, 0, supertypes.length);
        return sTypes;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMixin() {
        return isMixin;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isAbstract() {
        return isAbstract;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isQueryable() {
        return isQueryable;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasOrderableChildNodes() {
        return hasOrderableChildNodes;
    }

    /**
     * {@inheritDoc}
     */
    public Name getPrimaryItemName() {
        return primaryItemName;
    }

    /**
     * {@inheritDoc}
     */
    public QPropertyDefinition[] getPropertyDefs() {
        QPropertyDefinition[] pDefs = new QPropertyDefinition[propertyDefs.length];
        System.arraycopy(propertyDefs, 0, pDefs, 0, propertyDefs.length);
        return pDefs;
    }

    /**
     * {@inheritDoc}
     */
    public QNodeDefinition[] getChildNodeDefs() {
        QNodeDefinition[] cnDefs = new QNodeDefinition[childNodeDefs.length];
        System.arraycopy(childNodeDefs, 0, cnDefs, 0, childNodeDefs.length);
        return cnDefs;
    }

    /**
     * {@inheritDoc}
     */
    public Collection getDependencies() {
        if (dependencies == null) {
            Collection deps = new HashSet();
            // supertypes
            for (int i = 0; i < supertypes.length; i++) {
                deps.add(supertypes[i]);
            }
            // child node definitions
            for (int i = 0; i < childNodeDefs.length; i++) {
                // default primary type
                Name ntName = childNodeDefs[i].getDefaultPrimaryType();
                if (ntName != null && !name.equals(ntName)) {
                    deps.add(ntName);
                }
                // required primary type
                Name[] ntNames = childNodeDefs[i].getRequiredPrimaryTypes();
                for (int j = 0; j < ntNames.length; j++) {
                    if (ntNames[j] != null && !name.equals(ntNames[j])) {
                        deps.add(ntNames[j]);
                    }
                }
            }
            // property definitions
            for (int i = 0; i < propertyDefs.length; i++) {
                // REFERENCE value constraints
                if (propertyDefs[i].getRequiredType() == PropertyType.REFERENCE) {
                    String[] ca = propertyDefs[i].getValueConstraints();
                    if (ca != null) {
                        for (int j = 0; j < ca.length; j++) {
                            NameFactory factory = NameFactoryImpl.getInstance();
                            Name ntName = factory.create(ca[j]);
                            if (!name.equals(ntName)) {
                                deps.add(ntName);
                            }
                        }
                    }
                }
            }
            dependencies = Collections.unmodifiableCollection(deps);
        }
        return dependencies;
    }
    
    public Name[] getSupportedMixinTypes() {
        if (supportedMixins == null) {
            return null;
        }
        else {
            Name[] mixins = new Name[supportedMixins.length];
            System.arraycopy(supportedMixins, 0, mixins, 0, supportedMixins.length);
            return mixins;
        }
    }
    
    //-------------------------------< internal >-------------------------------

    /**
     * Returns an array of serializable property definitions for
     * <code>propDefs</code>.
     *
     * @param propDefs the qualified property definitions.
     * @return an array of serializable property definitions.
     */
    private static QPropertyDefinition[] getSerializablePropertyDefs(
            QPropertyDefinition[] propDefs) {
        QPropertyDefinition[] serDefs = new QPropertyDefinition[propDefs.length];
        for (int i = 0; i < propDefs.length; i++) {
            if (propDefs[i] instanceof Serializable) {
                serDefs[i] = propDefs[i];
            } else {
                serDefs[i] = new QPropertyDefinitionImpl(propDefs[i]);
            }
        }
        return serDefs;
    }

    /**
     * Returns an array of serializable node definitions for
     * <code>nodeDefs</code>.
     *
     * @param nodeDefs the qualified node definitions.
     * @return an array of serializable node definitions.
     */
    private static QNodeDefinition[] getSerializableNodeDefs(
            QNodeDefinition[] nodeDefs) {
        QNodeDefinition[] serDefs = new QNodeDefinition[nodeDefs.length];
        for (int i = 0; i < nodeDefs.length; i++) {
            if (nodeDefs[i] instanceof Serializable) {
                serDefs[i] = nodeDefs[i];
            } else {
                serDefs[i] = new QNodeDefinitionImpl(nodeDefs[i]);
            }
        }
        return serDefs;
    }

    private static Name[] getNames(String[] jcrNames, NamePathResolver resolver) throws NamespaceException, IllegalNameException {
        Name[] names = new Name[jcrNames.length];
        for (int i = 0; i < jcrNames.length; i++) {
            names[i] = resolver.getQName(jcrNames[i]);
        }
        return names;
    }

    private static QPropertyDefinition[] createQPropertyDefinitions(PropertyDefinition[] pds,
                                                                    NamePathResolver resolver,
                                                                    QValueFactory qValueFactory) throws RepositoryException {
        QPropertyDefinition[] declaredPropDefs = new QPropertyDefinition[pds.length];
        for (int i = 0; i < pds.length; i++) {
            PropertyDefinition propDef = pds[i];
            Name name = resolver.getQName(propDef.getName());
            Name declName = resolver.getQName(propDef.getDeclaringNodeType().getName());
            QValue[] defVls = ValueFormat.getQValues(propDef.getDefaultValues(), resolver, qValueFactory);

            declaredPropDefs[i] = new QPropertyDefinitionImpl(
                    name, declName, propDef.isAutoCreated(), propDef.isMandatory(),
                    propDef.getOnParentVersion(), propDef.isProtected(),
                    defVls, propDef.isMultiple(),
                    propDef.getRequiredType(), propDef.getValueConstraints(),
                    getNames(propDef.getAvailableQueryOperators(), resolver),
                    propDef.isFullTextSearchable(),
                    propDef.isQueryOrderable());
        }
        return declaredPropDefs;
    }

    private static QNodeDefinition[] createQNodeDefinitions(NodeDefinition[] nds, NamePathResolver resolver) throws RepositoryException {
        QNodeDefinition[] declaredNodeDefs = new QNodeDefinition[nds.length];
        for (int i = 0; i < nds.length; i++) {
            NodeDefinition nodeDef = nds[i];
            Name name = resolver.getQName(nodeDef.getName());
            Name declName = resolver.getQName(nodeDef.getDeclaringNodeType().getName());
            Name defaultPrimaryType = resolver.getQName(nodeDef.getDefaultPrimaryTypeName());
            Name[] requiredPrimaryTypes = getNames(nodeDef.getRequiredPrimaryTypeNames(), resolver);

            declaredNodeDefs[i] = new QNodeDefinitionImpl(
                    name, declName, nodeDef.isAutoCreated(), nodeDef.isMandatory(),
                    nodeDef.getOnParentVersion(), nodeDef.isProtected(),
                    defaultPrimaryType, requiredPrimaryTypes,
                    nodeDef.allowsSameNameSiblings());
        }
        return declaredNodeDefs;
    }
}
