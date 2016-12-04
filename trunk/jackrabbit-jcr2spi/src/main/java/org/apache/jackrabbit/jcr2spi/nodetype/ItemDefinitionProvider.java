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

import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.NodeId;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.ConstraintViolationException;

/**
 * <code>ItemDefinitionProvider</code>...
 */
public interface ItemDefinitionProvider {


    /**
     * Returns the <code>QNodeDefinition</code> for the root node.
     *
     * @return the <code>QNodeDefinition</code> for the root node.
     * @throws RepositoryException
     */
    public QNodeDefinition getRootNodeDefinition() throws RepositoryException;

    /**
     * Returns the <code>QNodeDefinition</code> for the specified node state.
     *
     * @param parentNodeTypeNames
     * @param nodeName
     * @param ntName
     * @param nodeId
     * @return the <code>QNodeDefinition</code> for the specified node state.
     * @throws RepositoryException
     */
    public QNodeDefinition getQNodeDefinition(Name[] parentNodeTypeNames,
                                              Name nodeName, Name ntName,
                                              NodeId nodeId) throws RepositoryException;

    /**
     * Returns the applicable child node definition for a child node with the
     * specified name and node type.
     *
     * @param parentNodeTypeNames
     * @param name
     * @param nodeTypeName
     * @return
     * @throws NoSuchNodeTypeException
     * @throws ConstraintViolationException if no applicable child node definition
     * could be found
     */
    public QNodeDefinition getQNodeDefinition(Name[] parentNodeTypeNames,
                                              Name name, Name nodeTypeName)
            throws NoSuchNodeTypeException, ConstraintViolationException;

    /**
     * Returns the applicable child node definition for a child node with the
     * specified name and node type.
     *
     * @param ent
     * @param name
     * @param nodeTypeName
     * @return
     * @throws NoSuchNodeTypeException
     * @throws ConstraintViolationException if no applicable child node definition
     * could be found
     */
    public QNodeDefinition getQNodeDefinition(EffectiveNodeType ent,
                                              Name name, Name nodeTypeName)
            throws NoSuchNodeTypeException, ConstraintViolationException;

    /**
     * Returns the <code>QPropertyDefinition</code> for the specified property state.
     * @param propertyState
     * @return the <code>QPropertyDefinition</code> for the specified property state.
     * @throws RepositoryException
     */
    /**
     * Returns the <code>QPropertyDefinition</code> for the specified parameters.
     *
     * @param parentNodeTypeNames
     * @param propertyName
     * @param propertyType
     * @param isMultiValued
     * @param propertyId Used to retrieve the definition from the persistent
     * layer if it cannot be determined from the information present.
     * @return
     * @throws RepositoryException
     */
    public QPropertyDefinition getQPropertyDefinition(Name[] parentNodeTypeNames,
                                                      Name propertyName,
                                                      int propertyType,
                                                      boolean isMultiValued,
                                                      PropertyId propertyId) throws RepositoryException;

    /**
     * Returns the applicable property definition for a property with the
     * specified name, type and multiValued characteristic. If there more than
     * one applicable definitions that would apply to the given params a
     * ConstraintViolationException is thrown.
     *
     * @param ntName
     * @param propName
     * @param type
     * @param multiValued
     * @return
     * @throws NoSuchNodeTypeException If no node type with name <code>ntName</code>
     * exists.
     * @throws ConstraintViolationException if no applicable property definition
     *                                      could be found
     */
    public QPropertyDefinition getQPropertyDefinition(Name ntName,
                                                      Name propName, int type,
                                                      boolean multiValued)
            throws ConstraintViolationException, NoSuchNodeTypeException;

    /**
     * Returns the applicable property definition for a property with the
     * specified name, type and multiValued characteristic. If there more than
     * one applicable definitions then the following rules are applied:
     * <ul>
     * <li>named definitions are preferred to residual definitions</li>
     * <li>definitions with specific required type are preferred to definitions
     * with required type UNDEFINED</li>
     * </ul>
     *
     * @param parentNodeTypeNames
     * @param name
     * @param type
     * @param multiValued
     * @return
     * @throws ConstraintViolationException if no applicable property definition
     * could be found.
     */
    public QPropertyDefinition getQPropertyDefinition(Name[] parentNodeTypeNames,
                                                      Name name, int type,
                                                      boolean multiValued)
            throws ConstraintViolationException, NoSuchNodeTypeException;

    /**
     * Returns the applicable property definition for a property with the
     * specified name and type. The multiValued flag is not taken into account
     * in the selection algorithm. Other than
     * <code>{@link #getQPropertyDefinition(Name[], Name, int, boolean)}</code>
     * this method does not take the multiValued flag into account in the
     * selection algorithm. If there more than one applicable definitions then
     * the following rules are applied:
     * <ul>
     * <li>named definitions are preferred to residual definitions</li>
     * <li>definitions with specific required type are preferred to definitions
     * with required type UNDEFINED</li>
     * <li>single-value definitions are preferred to multiple-value definitions</li>
     * </ul>
     *
     * @param parentNodeTypeNames
     * @param name
     * @param type
     * @return
     * @throws ConstraintViolationException if no applicable property definition
     *                                      could be found
     */
    public QPropertyDefinition getQPropertyDefinition(Name[] parentNodeTypeNames,
                                                      Name name, int type)
            throws ConstraintViolationException, NoSuchNodeTypeException;
}
