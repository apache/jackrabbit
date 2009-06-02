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

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;

import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

/**
 * <code>EffectiveNodeType</code>...
 */
public interface EffectiveNodeType {

    public Name[] getAllNodeTypes();

    public Name[] getInheritedNodeTypes();

    public Name[] getMergedNodeTypes();

    /**
     * Determines whether this effective node type representation includes
     * (either through inheritance or aggregation) the given node type.
     *
     * @param nodeTypeName name of node type
     * @return <code>true</code> if the given node type is included, otherwise
     *         <code>false</code>
     */
    public boolean includesNodeType(Name nodeTypeName);
    
    /**
     * Determines whether this effective node type supports adding
     * the specified mixin.
     * @param mixin name of mixin type
     * @return <code>true</code> if the mixin type is supported, otherwise
     *         <code>false</code>
     */
    public boolean supportsMixin(Name mixin);

    /**
     * Determines whether this effective node type representation includes
     * (either through inheritance or aggregation) all of the given node types.
     *
     * @param nodeTypeNames array of node type names
     * @return <code>true</code> if all of the given node types are included,
     *         otherwise <code>false</code>
     */
    public boolean includesNodeTypes(Name[] nodeTypeNames);

    public QNodeDefinition[] getAllQNodeDefinitions();

    public QPropertyDefinition[] getAllQPropertyDefinitions();

    public QNodeDefinition[] getAutoCreateQNodeDefinitions();

    public QPropertyDefinition[] getAutoCreateQPropertyDefinitions();

    public QNodeDefinition[] getMandatoryQNodeDefinitions();

    public QPropertyDefinition[] getMandatoryQPropertyDefinitions();

    public QNodeDefinition[] getNamedQNodeDefinitions(Name name);

    public QPropertyDefinition[] getNamedQPropertyDefinitions(Name name);

    public QNodeDefinition[] getUnnamedQNodeDefinitions();

    public QPropertyDefinition[] getUnnamedQPropertyDefinitions();

    /**
     * @param name
     * @param definitionProvider
     * @throws ConstraintViolationException
     */
    public void checkAddNodeConstraints(Name name, ItemDefinitionProvider definitionProvider)
            throws ConstraintViolationException;

    /**
     * @param name
     * @param nodeTypeDefinition
     *@param definitionProvider  @throws ConstraintViolationException  @throws NoSuchNodeTypeException
     */
    public void checkAddNodeConstraints(Name name, QNodeTypeDefinition nodeTypeDefinition, ItemDefinitionProvider definitionProvider)
            throws ConstraintViolationException, NoSuchNodeTypeException;

    /**
     * @param name
     * @throws ConstraintViolationException
     * @deprecated Use {@link #hasRemoveNodeConstraint(Name)} and
     * {@link #hasRemovePropertyConstraint(Name)} respectively.
     */
    public void checkRemoveItemConstraints(Name name) throws ConstraintViolationException;

    /**
     * Returns <code>true</code> if a single node definition matching the
     * specified <code>nodeName</code> is either mandatory or protected.
     *
     * @param nodeName
     * @return <code>true</code> if a single node definition matching the
     * specified <code>nodeName</code> is either mandatory or protected.
     */
    public boolean hasRemoveNodeConstraint(Name nodeName);

    /**
     * Returns <code>true</code> if a single property definition matching the
     * specified <code>propertyName</code> is either mandatory or protected.
     *
     * @param propertyName
     * @return <code>true</code> if a single property definition matching the
     * specified <code>propertyName</code> is either mandatory or protected.
     */
    public boolean hasRemovePropertyConstraint(Name propertyName);
}
