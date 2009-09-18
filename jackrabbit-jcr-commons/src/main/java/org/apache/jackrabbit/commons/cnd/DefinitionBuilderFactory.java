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
package org.apache.jackrabbit.commons.cnd;

import javax.jcr.RepositoryException;
import javax.jcr.PropertyType;
import javax.jcr.query.qom.QueryObjectModelConstants;
import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.PropertyDefinition;

/**
 * Builder for node type definitions, node definitions and property definitions.
 * @param <T>  type of the node type definition
 * @param <N>  type of the namespace mapping
 */
public abstract class DefinitionBuilderFactory<T, N> {

    /**
     * Create a new instance of a {@link AbstractNodeTypeDefinitionBuilder}
     * @return
     * @throws RepositoryException
     */
    public abstract AbstractNodeTypeDefinitionBuilder<T> newNodeTypeDefinitionBuilder()
            throws RepositoryException;

    /**
     * Set the namespace mapping to use for the node type definition being built
     * @param nsMapping
     */
    public abstract void setNamespaceMapping(N nsMapping);

    /**
     * @return  the namespace mapping used for the node type definition being built
     */
    public abstract N getNamespaceMapping();

    /**
     * Add a mapping to the namespace map
     * @param prefix
     * @param uri
     * @throws RepositoryException
     */
    public abstract void setNamespace(String prefix, String uri) throws RepositoryException;

    /**
     * Builder for a node type definition of type T.
     * @param <T>
     */
    public static abstract class AbstractNodeTypeDefinitionBuilder<T> {

        /** See {@link #setName(String)} */
        protected String name;

        /** See {@link #setMixin(boolean)} */
        protected boolean isMixin;

        /** See {@link #setOrderableChildNodes(boolean)} */
        protected boolean isOrderable;

        /** See {@link #setAbstract(boolean)} */
        protected boolean isAbstract;

        /** See {@link #setQueryable(boolean)} */
        protected boolean queryable;

        /**
         * Set the name of the node type definition being built
         * @param name
         * @throws RepositoryException  if the name is not valid
         * @see NodeTypeDefinition#getName()
         */
        public void setName(String name) throws RepositoryException {
            this.name = name;
        }

        /**
         * Returns the name of the node type definition being built
         * @return
         */
        public String getName() {
            return name;
        }

        /**
         * Add the given name to the set of supertypes of the node type definition
         * being built
         * @param name  name of the the supertype
         * @throws RepositoryException  if the name is not valid
         * @see NodeTypeDefinition#getDeclaredSupertypeNames()
         */
        public abstract void addSupertype(String name) throws RepositoryException;

        /**
         * @param isMixin <code>true</code> if building a mixin node type
         * definition; <code>false</code> otherwise.
         * @throws RepositoryException
         * @see NodeTypeDefinition#isMixin()
         */
        public void setMixin(boolean isMixin) throws RepositoryException {
            this.isMixin = isMixin;
        }

        /**
         * @param isOrderable <code>true</code> if building a node type having
         * orderable child nodes; <code>false</code> otherwise.
         * @throws RepositoryException
         * @see NodeTypeDefinition#hasOrderableChildNodes()
         */
        public void setOrderableChildNodes(boolean isOrderable) throws RepositoryException {
            this.isOrderable = isOrderable;
        }

        /**
         * @param name  the name of the primary item.
         * @throws RepositoryException
         * @see NodeTypeDefinition#getPrimaryItemName()
         */
        public abstract void setPrimaryItemName(String name) throws RepositoryException;

        /**
         * @param isAbstract <code>true</code> if building a node type that is abstract.
         * @throws RepositoryException
         * @see NodeTypeDefinition#isAbstract()
         */
        public void setAbstract(boolean isAbstract) throws RepositoryException {
            this.isAbstract = isAbstract;
        }

        /**
         * @param queryable <code>true</code> if building a node type that is queryable
         * @throws RepositoryException
         * @see NodeTypeDefinition#isQueryable()
         */
        public void setQueryable(boolean queryable) throws RepositoryException {
            this.queryable = queryable;
        }

        /**
         * Create a new instance of a {@link DefinitionBuilderFactory.AbstractPropertyDefinitionBuilder}
         * which can be used to add property definitions to the node type definition being built.
         * @return
         * @throws RepositoryException
         */
        public abstract AbstractPropertyDefinitionBuilder<T> newPropertyDefinitionBuilder()
                throws RepositoryException;

        /**
         * Create a new instance fo a {@link DefinitionBuilderFactory.AbstractNodeDefinitionBuilder}
         * which can be used to add child node definitions to the node type definition being built.
         * @return
         * @throws RepositoryException
         */
        public abstract AbstractNodeDefinitionBuilder<T> newNodeDefinitionBuilder() throws RepositoryException;

        /**
         * Build this node type definition
         * @return
         * @throws RepositoryException
         */
        public abstract T build() throws RepositoryException;
    }

    /**
     * Builder for item definitions of type <code>T</code>
     * @param <T>
     */
    public static abstract class AbstractItemDefinitionBuilder<T> {

        /** See {@link #setName(String)} */
        protected String name;

        /** See {@link #setAutoCreated(boolean)} */
        protected boolean autocreate;

        /** See {@link #setOnParentVersion(int)} */
        protected int onParent;

        /** See {@link #setProtected(boolean)} */
        protected boolean isProtected;

        /** See {@link #setMandatory(boolean)} */
        protected boolean isMandatory;

        /**
         * @param name  the name of the child item definition being build
         * @throws RepositoryException
         * @see ItemDefinition#getName()
         */
        public void setName(String name) throws RepositoryException {
            this.name = name;
        }

        /**
         * Name of the child item definition being built
         * @return
         */
        public String getName() {
            return name;
        }

        /**
         * @param name the name of the declaring node type.
         * @throws RepositoryException
         * @see ItemDefinition#getDeclaringNodeType()
         */
        public abstract void setDeclaringNodeType(String name) throws RepositoryException;

        /**
         * @param autocreate <code>true</code> if building a 'autocreate' child item
         * definition, false otherwise.
         * @throws RepositoryException
         * @see ItemDefinition#isAutoCreated()
         */
        public void setAutoCreated(boolean autocreate) throws RepositoryException {
            this.autocreate = autocreate;
        }

        /**
         * @param onParent the 'onParentVersion' attribute of the child item definition being built
         * @throws RepositoryException
         * @see ItemDefinition#getOnParentVersion()
         */
        public void setOnParentVersion(int onParent) throws RepositoryException {
            this.onParent = onParent;
        }

        /**
         * @param isProtected <code>true</code> if building a 'protected' child
         * item definition, false otherwise.
         * @throws RepositoryException
         * @see ItemDefinition#isProtected()
         */
        public void setProtected(boolean isProtected) throws RepositoryException {
            this.isProtected = isProtected;
        }

        /**
         * @param isMandatory <code>true</code> if building a 'mandatory' child
         * item definition, false otherwise.
         * @throws RepositoryException
         */
        public void setMandatory(boolean isMandatory) throws RepositoryException {
            this.isMandatory = isMandatory;
        }

        /**
         * Build this item definition an add it to its parent node type definition
         * @throws RepositoryException
         */
        public abstract void build() throws RepositoryException;
    }

    /**
     * Builder for property definitions of type <code>T</code>
     * @param <T>
     */
    public static abstract class AbstractPropertyDefinitionBuilder<T> extends AbstractItemDefinitionBuilder<T> {

        private static final String[] ALL_OPERATORS = new String[]{
                QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO,
                QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN,
                QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN_OR_EQUAL_TO,
                QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN,
                QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN_OR_EQUAL_TO,
                QueryObjectModelConstants.JCR_OPERATOR_LIKE,
                QueryObjectModelConstants.JCR_OPERATOR_NOT_EQUAL_TO
        };

        /** See {@link #setRequiredType(int)} */
        protected int requiredType = PropertyType.UNDEFINED;

        /** See {@link #setMultiple(boolean)} */
        protected boolean isMultiple = false;

        /** See {@link #setFullTextSearchable(boolean)} */
        protected boolean fullTextSearchable = true;

        /** See {@link #setQueryOrderable(boolean)} */
        protected boolean queryOrderable = true;

        /** See {@link #setAvailableQueryOperators(String[])} */
        protected String[] queryOperators = ALL_OPERATORS;

        /**
         * @param type the required type of the property definition being built.
         * @throws RepositoryException
         * @see PropertyDefinition#getRequiredType()
         */
        public void setRequiredType(int type) throws RepositoryException {
            this.requiredType = type;
        }

        /**
         * The required type of the property definition being built.
         * @return
         */
        public int getRequiredType() {
            return requiredType;
        }

        /**
         * @param constraint  add a value constraint to the list of value constraints of the property
         * definition being built.
         * @throws RepositoryException
         * @see PropertyDefinition#getValueConstraints()
         */
        public abstract void addValueConstraint(String constraint) throws RepositoryException;

        /**
         * @param value  add a default value to the list of default values of the property definition
         * being built.
         * @throws RepositoryException
         * @see PropertyDefinition#getDefaultValues()
         */
        public abstract void addDefaultValues(String value) throws RepositoryException;

         /**
         * @param isMultiple true if building a 'multiple' property definition.
         * @throws RepositoryException
         * @see PropertyDefinition#isMultiple()
         */
        public void setMultiple(boolean isMultiple) throws RepositoryException {
            this.isMultiple = isMultiple;
        }

        /**
         * @param fullTextSearchable <code>true</code> if building a
         * 'fulltext searchable' property definition
         * @throws RepositoryException
         * @see PropertyDefinition#isFullTextSearchable()
         */
        public void setFullTextSearchable(boolean fullTextSearchable) throws RepositoryException {
            this.fullTextSearchable = fullTextSearchable;
        }

        /**
         * @param queryOrderable <code>true</code> if the property is orderable in a query
         * @throws RepositoryException
         * @see PropertyDefinition#isQueryOrderable()
         */
        public void setQueryOrderable(boolean queryOrderable) throws RepositoryException {
            this.queryOrderable = queryOrderable;
        }

        /**
         * @param queryOperators the query operators of the property
         * @throws RepositoryException
         * @see PropertyDefinition#getAvailableQueryOperators()
         */
        public void setAvailableQueryOperators(String[] queryOperators) throws RepositoryException {
            if (queryOperators == null) {
                throw new NullPointerException("queryOperators");
            }
            this.queryOperators = queryOperators;
        }
    }

    /**
     * Builder for child node definitions of type <code>T</code>
     * @param <T>
     */
    public static abstract class AbstractNodeDefinitionBuilder<T> extends AbstractItemDefinitionBuilder<T> {
        protected boolean allowSns;

        /**
         * @param name the name of the default primary type of the node definition being built.
         * @throws RepositoryException
         */
        public abstract void setDefaultPrimaryType(String name) throws RepositoryException;

        /**
         * @param name  add a required primary type to the list of names of the required primary types of
         * the node definition being built.
         * @throws RepositoryException
         */
        public abstract void addRequiredPrimaryType(String name) throws RepositoryException;

        /**
         * @param allowSns true if building a node definition with same name siblings, false otherwise.
         * @throws RepositoryException
         */
        public void setAllowsSameNameSiblings(boolean allowSns) throws RepositoryException {
            this.allowSns = allowSns;
        }

    }

}


