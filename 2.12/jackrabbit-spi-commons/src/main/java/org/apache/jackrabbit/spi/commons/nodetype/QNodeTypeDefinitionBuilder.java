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
package org.apache.jackrabbit.spi.commons.nodetype;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import javax.jcr.nodetype.NodeTypeDefinition;

import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.QNodeTypeDefinitionImpl;

/**
 * A builder for {@link QNodeTypeDefinition}.
 */
public class QNodeTypeDefinitionBuilder {

    private Name name = null;
    private List<Name> supertypes = new ArrayList<Name>();
    private boolean isMixin = false;
    private boolean isOrderable = false;
    private Name primaryItemName = null;
    private List<QPropertyDefinition> propertyDefinitions = new ArrayList<QPropertyDefinition>();
    private List<QNodeDefinition> childNodeDefinitions = new ArrayList<QNodeDefinition>();
    private boolean isAbstract = false;
    private boolean isQueryable = true;
    private List<Name> supportedMixins = null;


    /**
     * Set the name of the node type definition being built
     * @param name the name
     * @see NodeTypeDefinition#getName()
     */
    public void setName(Name name) {
        this.name = name;
    }

    /**
     * @return the name of the node type definition being built or
     * <code>null</code> if not set.
     * @see NodeTypeDefinition#getName()
     */
    public Name getName() {
        return name;
    }

    /**
     * Specifies the supertypes of the node type definition being built
     * @param supertypes the supertypes
     * @see NodeTypeDefinition#getDeclaredSupertypeNames()
     */
    public void setSupertypes(Name[] supertypes) {
        this.supertypes.clear();
        this.supertypes.addAll(Arrays.asList(supertypes));
    }

    /**
     * Returns an array containing the names of the supertypes of the node
     * type definition being built.
     *
     * @return an array of supertype names
     * @see NodeTypeDefinition#getDeclaredSupertypeNames()
     */
    public Name[] getSuperTypes() {
        if (supertypes.size() > 0
                || isMixin() || NameConstants.NT_BASE.equals(getName())) {
            return supertypes.toArray(new Name[supertypes.size()]);
        } else {
            return new Name[] { NameConstants.NT_BASE };
        }
    }

    /**
     * @param isMixin <code>true</code> if building a mixin node type
     * definition; <code>false</code> otherwise.
     * @see NodeTypeDefinition#isMixin()
     */
    public void setMixin(boolean isMixin) {
        this.isMixin = isMixin;
    }

    /**
     * @return <code>true</code> if building a mixin node type definition;
     * <code>false</code> otherwise.
     * @see NodeTypeDefinition#isMixin()
     */
    public boolean isMixin() {
        return isMixin;
    }

    /**
     * Sets the names of additional mixin types supported on this node type.
     *
     * @param names an array of mixin type names, or <code>null</code> when
     *              there are no known constraints
     */
    public void setSupportedMixinTypes(Name[] names) {
        if (names == null) {
            supportedMixins = null;
        } else {
            supportedMixins = new ArrayList<Name>(Arrays.asList(names));
        }
    }

    /**
     * Returns an array containing the names of additional mixin types supported
     * on this node type.
     *
     * @return an array of mixin type names, or <code>null</code> when there are
     *         no known constraints.
     */
    public Name[] getSupportedMixinTypes() {
        if (supportedMixins == null) {
            return null;
        } else {
            return supportedMixins.toArray(new Name[supportedMixins.size()]);
        }
    }

    /**
     * @param isOrderable <code>true</code> if building a node type having
     * orderable child nodes; <code>false</code> otherwise.
     * @see NodeTypeDefinition#hasOrderableChildNodes()
     */
    public void setOrderableChildNodes(boolean isOrderable) {
        this.isOrderable = isOrderable;
    }

    /**
     * @return <code>true</code> if building a node type having orderable
     * child nodes; <code>false</code> otherwise.
     * @see NodeTypeDefinition#hasOrderableChildNodes()
     */
    public boolean hasOrderableChildNodes() {
        return isOrderable;
    }

    /**
     * @param primaryItemName the name of the primary item or
     * <code>null</code> if not set.
     * @see NodeTypeDefinition#getPrimaryItemName()
     */
    public void setPrimaryItemName(Name primaryItemName) {
        this.primaryItemName = primaryItemName;
    }

    /**
     * @return the name of the primary item or <code>null</code> if not set.
     * @see NodeTypeDefinition#getPrimaryItemName()
     */
    public Name getPrimaryItemName() {
        return primaryItemName;
    }

    /**
     * @return <code>true</code> if the node type is abstract.
     * @see NodeTypeDefinition#isAbstract()
     */
    public boolean isAbstract() {
        return isAbstract;
    }

    /**
     * @param isAbstract <code>true</code> if building a node type that is abstract.
     * @see NodeTypeDefinition#isAbstract()
     */
    public void setAbstract(boolean isAbstract) {
        this.isAbstract = isAbstract;
    }

    /**
     * @return <code>true</code> if the node type is queryable
     * @see NodeTypeDefinition#isQueryable()
     */
    public boolean isQueryable() {
        return isQueryable;
    }

    /**
     * @param queryable <code>true</code> if building a node type that is queryable
     * @see NodeTypeDefinition#isQueryable()
     */
    public void setQueryable(boolean queryable) {
        isQueryable = queryable;
    }

    /**
     * @param propDefs an array containing the property definitions of the node type definition
     *                being built.
     * @see NodeTypeDefinition#getDeclaredPropertyDefinitions()
     */
    public void setPropertyDefs(QPropertyDefinition[] propDefs) {
        propertyDefinitions.clear();
        propertyDefinitions.addAll(Arrays.asList(propDefs));
    }

    /**
     * @return an array containing the property definitions of the node type
     *         definition being built.
     * @see NodeTypeDefinition#getDeclaredPropertyDefinitions()
     */
    public QPropertyDefinition[] getPropertyDefs() {
        return propertyDefinitions.toArray(new QPropertyDefinition[propertyDefinitions.size()]);
    }

    /**
     * @param childDefs an array containing the child node definitions of the node type
     *                definition being.
     * @see NodeTypeDefinition#getDeclaredChildNodeDefinitions()
     */
    public void setChildNodeDefs(QNodeDefinition[] childDefs) {
        childNodeDefinitions.clear();
        childNodeDefinitions.addAll(Arrays.asList(childDefs));
    }

    /**
     * @return an array containing the child node definitions of the node type
     *         definition being built.
     * @see NodeTypeDefinition#getDeclaredChildNodeDefinitions()
     */
    public QNodeDefinition[] getChildNodeDefs() {
        return childNodeDefinitions.toArray(new QNodeDefinition[childNodeDefinitions.size()]);
    }

    /**
     * Creates a new {@link QNodeTypeDefinition} instance based on the state of this builder.
     *
     * @return a new {@link QNodeTypeDefinition} instance.
     * @throws IllegalStateException if the instance has not the necessary information to build
     *                 the QNodeTypeDefinition instance.
     */
    public QNodeTypeDefinition build() throws IllegalStateException {
        return new QNodeTypeDefinitionImpl(getName(), getSuperTypes(),
                getSupportedMixinTypes(), isMixin(), isAbstract(),
                isQueryable(), hasOrderableChildNodes(), getPrimaryItemName(),
                getPropertyDefs(), getChildNodeDefs());
    }
}
