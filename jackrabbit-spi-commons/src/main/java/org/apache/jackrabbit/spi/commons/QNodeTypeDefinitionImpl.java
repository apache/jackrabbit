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
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;

import javax.jcr.PropertyType;
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
        this(nt.getName(), nt.getSupertypes(), nt.isMixin(),
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
        this.name = name;
        this.supertypes = supertypes;
        this.supportedMixins = null;
        this.isMixin = isMixin;
        this.hasOrderableChildNodes = hasOrderableChildNodes;
        this.primaryItemName = primaryItemName;
        this.propertyDefs = getSerializablePropertyDefs(declaredPropDefs);
        this.childNodeDefs = getSerializableNodeDefs(declaredNodeDefs);
    }

    /**
     * Creates a new serializable qualified node type definition.
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
     */
    public QNodeTypeDefinitionImpl(Name name,
                                   Name[] supertypes,
                                   Name[] supportedMixins,
                                   boolean isMixin,
                                   boolean hasOrderableChildNodes,
                                   Name primaryItemName,
                                   QPropertyDefinition[] declaredPropDefs,
                                   QNodeDefinition[] declaredNodeDefs) {
        this.name = name;
        this.supertypes = supertypes;
        this.supportedMixins = supportedMixins;
        this.isMixin = isMixin;
        this.hasOrderableChildNodes = hasOrderableChildNodes;
        this.primaryItemName = primaryItemName;
        this.propertyDefs = getSerializablePropertyDefs(declaredPropDefs);
        this.childNodeDefs = getSerializableNodeDefs(declaredNodeDefs);
    }

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
}
