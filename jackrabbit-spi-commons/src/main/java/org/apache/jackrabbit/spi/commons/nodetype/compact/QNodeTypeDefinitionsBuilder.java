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
package org.apache.jackrabbit.spi.commons.nodetype.compact;

import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QItemDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.nodetype.InvalidConstraintException;

/**
 * A builder for {@link QNodeTypeDefinition}s
 */
public abstract class QNodeTypeDefinitionsBuilder {

    /**
     * @return a new instance of a builder for a {@link QNodeTypeDefinition}
     */
    public abstract QNodeTypeDefinitionBuilder newQNodeTypeDefinition();

    /**
     * Returns a <code>Name</code> with the given namespace URI and
     * local part and validates the given parameters.
     *
     * @param namespaceURI namespace uri
     * @param localName local part
     * @throws IllegalArgumentException if <code>namespaceURI</code> or
     * <code>localName</code> is invalid.
     */
    public abstract Name createName(String namespaceURI, String localName) throws IllegalArgumentException;

    /**
     * A builder for a {@link QNodeTypeDefinition}
     */
    public abstract class QNodeTypeDefinitionBuilder {
        private Name name;
        private Name[] supertypes;
        private boolean isMixin;
        private boolean isOrderable;
        private Name primaryItemName;
        private QPropertyDefinition[] propertyDefinitions;
        private QNodeDefinition[] childNodeDefinitions;

        /**
         * Set the name of the node type definition being built
         * @param name
         */
        public void setName(Name name) {
            this.name = name;
        }

        /**
         * @return the name of the node type definition being built or <code>null</code> if not set.
         */
        public Name getName() {
            return name;
        }

        /**
         * Specifies the supertypes of the node type definition being built
         * @param supertypes
         */
        public void setSupertypes(Name[] supertypes) {
            this.supertypes = supertypes;
        }

        /**
         * Returns an array containing the names of the supertypes of the node type definition being
         * built.
         *
         * @return an array of supertype names
         */
        public Name[] getSuperTypes() {
            return supertypes;
        }

        /**
         * @param isMixin true if building a mixin node type definition; false otherwise.
         */
        public void setMixin(boolean isMixin) {
            this.isMixin = isMixin;
        }

        /**
         * @return true if building a mixin node type definition; false otherwise.
         */
        public boolean getMixin() {
            return isMixin;
        }

        /**
         * @param isOrderable true if building a node type having orderable child nodes; false
         *                otherwise.
         */
        public void setOrderableChildNodes(boolean isOrderable) {
            this.isOrderable = isOrderable;
        }

        /**
         * @return true if building a node type having orderable child nodes; false otherwise.
         */
        public boolean getOrderableChildNodes() {
            return isOrderable;
        }

        /**
         * @param primaryItemName  the name of the primary item or <code>null</code> if not set.
         */
        public void setPrimaryItemName(Name primaryItemName) {
            this.primaryItemName = primaryItemName;
        }

        /**
         * @return the name of the primary item or <code>null</code> if not set.
         */
        public Name getPrimaryItemName() {
            return primaryItemName;
        }

        /**
         * @param propDefs an array containing the property definitions of the node type definition
         *                being built.
         */
        public void setPropertyDefs(QPropertyDefinition[] propDefs) {
            propertyDefinitions = propDefs;
        }

        /**
         * @return an array containing the property definitions of the node type definition being
         *         built or <code>null</code> if not set.
         */
        public QPropertyDefinition[] getPropertyDefs() {
            return propertyDefinitions;
        }

        /**
         * @param childDefs an array containing the child node definitions of the node type
         *                definition being.
         */
        public void setChildNodeDefs(QNodeDefinition[] childDefs) {
            childNodeDefinitions = childDefs;
        }

        /**
         * @return an array containing the child node definitions of the node type definition being
         *         built or <code>null</code> if not set.
         */
        public QNodeDefinition[] getChildNodeDefs() {
            return childNodeDefinitions;
        }

        /**
         * @return  a new instance of a builder for a {@link QNodeDefinition}.
         */
        public abstract QPropertyDefinitionBuilder newQPropertyDefinition();

        /**
         * @return  a new instance of a builder for a {@link QNodeDefinition}.
         */
        public abstract QNodeDefinitionBuilder newQNodeDefinitionBuilder();

        /**
         * Creates a new {@link QNodeTypeDefinition} instance based on the state of this builder.
         *
         * @return a new {@link QNodeTypeDefinition} instance.
         * @throws IllegalStateException if the instance has not the necessary information to build
         *                 the QNodeTypeDefinition instance.
         */
        public abstract QNodeTypeDefinition build() throws IllegalStateException;
    }

    /**
     * A builder for a {@link QItemDefinition}
     */
    abstract class QItemDefinitionBuilder {
        private Name name;
        private Name declaringType;
        private boolean isAutocreated;
        private int onParentVersion;
        private boolean isProtected;
        private boolean isMandatory;

        /**
         * @param name  the name of the child item definition being build
         */
        public void setName(Name name) {
            this.name = name;
        }

        /**
         * @return the name of the child item definition being build.
         */
        public Name getName() {
            return name;
        }

        /**
         * @param type  the name of the declaring node type.
         */
        public void setDeclaringNodeType(Name type) {
            declaringType = type;
        }

        /**
         * @return the name of the declaring node type.
         */
        public Name getDeclaringNodeType() {
            return declaringType;
        }

        /**
         * @param autocreate  true if building a 'autocreate' child item definition, false otherwise.
         */
        public void setAutoCreated(boolean autocreate) {
            isAutocreated = autocreate;
        }

        /**
         * @return true if building a 'autocreate' child item definition, false otherwise.
         */
        public boolean getAutoCreated() {
            return isAutocreated;
        }

        /**
         * @param onParent the 'onParentVersion' attribute of the child item definition being built
         */
        public void setOnParentVersion(int onParent) {
            onParentVersion = onParent;
        }

        /**
         * @return the 'onParentVersion' attribute of the child item definition being built
         */
        public int getOnParentVersion() {
            return onParentVersion;
        }

        /**
         * @param isProtected true if building a 'protected' child item definition, false otherwise.
         */
        public void setProtected(boolean isProtected) {
            this.isProtected = isProtected;
        }

        /**
         * @return  true if building a 'protected' child item definition, false otherwise.
         */
        public boolean getProtected() {
            return isProtected;
        }

        /**
         * @param isMandatory true if building a 'mandatory' child item definition, false otherwise.
         */
        public void setMandatory(boolean isMandatory) {
            this.isMandatory = isMandatory;
        }

        /**
         * @return  true if building a 'mandatory' child item definition, false otherwise.
         */
        public boolean getMandatory() {
            return isMandatory;
        }
    }

    /**
     * A builder for a {@link QNodeDefinition}
     */
    public abstract class QPropertyDefinitionBuilder extends QItemDefinitionBuilder {
        private int requiredType;
        private String[] valueConstraints;
        private QValue[] defaultValues;
        private boolean isMultiple;

        /**
         * @param type the required type of the property definition being built.
         */
        public void setRequiredType(int type) {
            requiredType = type;
        }

        /**
         * @return the required type of the property definition being built.
         */
        public int getRequiredType() {
            return requiredType;
        }

        /**
         * @param constraints array of value constraints of the property definition being built.
         */
        public void setValueConstraints(String[] constraints) {
            valueConstraints = constraints;
        }

        /**
         * @return array of value constraints of the property definition being built.
         */
        public String[] getValueConstraints() {
            return valueConstraints;
        }

        /**
         * @param values array of default values of the property definition being built.
         */
        public void setDefaultValues(QValue[] values) {
            defaultValues = values;
        }

        /**
         * @return array of default values of the property definition being built or
         *         <code>null</code> if no default values are defined.
         */
        public QValue[] getDefaultValues() {
            return defaultValues;
        }

        /**
         * @param isMultiple true if building a 'multiple' property definition.
         */
        public void setMultiple(boolean isMultiple) {
            this.isMultiple = isMultiple;
        }

        /**
         * @return true if building a 'multiple' property definition.
         */
        public boolean getMultiple() {
            return isMultiple;
        }

        /**
         * Validate the given <code>constraint</code> and resolve any prefixes.
         *
         * @param constraint
         * @param resolver
         * @return A syntactically valid value constrained which refers to fully qualified names and
         *         paths only.
         * @throws InvalidConstraintException if <code>constraint</code> cannot be converted to a
         *                 valid value constrained.
         */
        public abstract String createValueConstraint(String constraint, NamePathResolver resolver)
                throws InvalidConstraintException;

        /**
         * Create a new <code>QValue</code> for <code>value</code> of the type this instance
         * represents using the given <code>resolver</code>.
         *
         * @param value
         * @param resolver
         * @return a new <code>QValue</code>.
         * @throws ValueFormatException If the given <code>value</code> cannot be converted to the
         *                 specified <code>type</code>.
         * @throws RepositoryException If another error occurs.
         */
        public abstract QValue createValue(String value, NamePathResolver resolver)
                throws ValueFormatException, RepositoryException;

        /**
         * Creates a new {@link QPropertyDefinition} instance based on the state of this builder.
         *
         * @return a new {@link QPropertyDefinition} instance.
         * @throws IllegalStateException if the instance has not the necessary information to build
         *                 the QPropertyDefinition instance.
         */
        public abstract QPropertyDefinition build() throws IllegalStateException;
    }

    /**
     * A builder for a {@link QNodeDefinition}
     */
    public abstract class QNodeDefinitionBuilder extends QItemDefinitionBuilder {
        private Name defaultPrimaryType;
        private Name[] requiredPrimaryTypes;
        private boolean allowsSameNameSiblings;

        /**
         * @param name the name of the default primary type of the node definition being built.
         */
        public void setDefaultPrimaryType(Name name) {
            defaultPrimaryType = name;
        }

        /**
         * @return the name of the default primary type of the node definition being built.
         */
        public Name getDefaultPrimaryType() {
            return defaultPrimaryType;
        }

        /**
         * @param names array of names of the required primary types of the node definition being
         *                built.
         */
        public void setRequiredPrimaryTypes(Name[] names) {
            requiredPrimaryTypes = names;
        }

        /**
         * @return array of names of the required primary types of the node definition being built.
         */
        public Name[] getRequiredPrimaryTypes() {
            return requiredPrimaryTypes;
        }

        /**
         * @param allowSns true if building a node definition with same name siblings, false
         *                otherwise.
         */
        public void setAllowsSameNameSiblings(boolean allowSns) {
            allowsSameNameSiblings = allowSns;
        }

        /**
         * @return true if building a node definition with same name siblings, false otherwise.
         */
        public boolean getAllowsSameNameSiblings() {
            return allowsSameNameSiblings;
        }

        /**
         * Creates a new {@link QNodeDefinition} instance based on the state of this builder.
         *
         * @return a new {@link QNodeDefinition} instance.
         * @throws IllegalStateException if the instance has not the necessary information to build
         *                 the QNodeDefinition instance.
         */
        public abstract QNodeDefinition build() throws IllegalStateException;
    }

}
