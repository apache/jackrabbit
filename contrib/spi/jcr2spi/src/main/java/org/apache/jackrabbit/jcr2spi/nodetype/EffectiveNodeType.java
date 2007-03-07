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

import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;

import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

/**
 * <code>EffectiveNodeType</code>...
 */
public interface EffectiveNodeType {

    public QName[] getAllNodeTypes();

    public QName[] getInheritedNodeTypes();

    public QName[] getMergedNodeTypes();

    /**
     * Determines whether this effective node type representation includes
     * (either through inheritance or aggregation) the given node type.
     *
     * @param nodeTypeName name of node type
     * @return <code>true</code> if the given node type is included, otherwise
     *         <code>false</code>
     */
    public boolean includesNodeType(QName nodeTypeName);

    /**
     * Determines whether this effective node type representation includes
     * (either through inheritance or aggregation) all of the given node types.
     *
     * @param nodeTypeNames array of node type names
     * @return <code>true</code> if all of the given node types are included,
     *         otherwise <code>false</code>
     */
    public boolean includesNodeTypes(QName[] nodeTypeNames);

    public QNodeDefinition[] getAllNodeDefs();

    public QPropertyDefinition[] getAllPropDefs();

    public QNodeDefinition[] getAutoCreateNodeDefs();

    public QPropertyDefinition[] getAutoCreatePropDefs();

    public QPropertyDefinition[] getMandatoryPropDefs();

    public QNodeDefinition[] getMandatoryNodeDefs();

    /**
     * Returns the applicable child node definition for a child node with the
     * specified name and node type.
     *
     * @param name
     * @param nodeTypeName
     * @return
     * @throws NoSuchNodeTypeException
     * @throws ConstraintViolationException if no applicable child node definition
     * could be found
     */
    public QNodeDefinition getApplicableNodeDefinition(QName name, QName nodeTypeName,
                                                       NodeTypeRegistry ntReg)
            throws NoSuchNodeTypeException, ConstraintViolationException;

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
     * @param name
     * @param type
     * @param multiValued
     * @return
     * @throws ConstraintViolationException if no applicable property definition
     *                                      could be found
     */
    public QPropertyDefinition getApplicablePropertyDefinition(QName name, int type,
                                                               boolean multiValued)
            throws ConstraintViolationException;

    /**
     * Returns all applicable property definitions for a property with the
     * specified name, type and multiValued characteristics.
     * @throws ConstraintViolationException if no applicable property definition
     *                                      could be found
     */
    public QPropertyDefinition[] getApplicablePropertyDefinitions(QName name, int type,
                                                                  boolean multiValued)
            throws ConstraintViolationException;

    /**
     * Returns the applicable property definition for a property with the
     * specified name and type. The multiValued flag is not taken into account
     * in the selection algorithm. Other than
     * <code>{@link #getApplicablePropertyDefinition(QName, int, boolean)}</code>
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
     * @param name
     * @param type
     * @return
     * @throws ConstraintViolationException if no applicable property definition
     *                                      could be found
     */
    public QPropertyDefinition getApplicablePropertyDefinition(QName name, int type)
            throws ConstraintViolationException;

    /**
     * @param name
     * @param ntReg
     * @throws ConstraintViolationException
     */
    public void checkAddNodeConstraints(QName name, NodeTypeRegistry ntReg)
            throws ConstraintViolationException;

    /**
     * @param name
     * @param nodeTypeName
     * @param ntReg
     * @throws ConstraintViolationException
     * @throws NoSuchNodeTypeException
     */
    public void checkAddNodeConstraints(QName name, QName nodeTypeName, NodeTypeRegistry ntReg)
            throws ConstraintViolationException, NoSuchNodeTypeException;

    /**
     * @param name
     * @throws ConstraintViolationException
     */
    public void checkRemoveItemConstraints(QName name) throws ConstraintViolationException;
}
