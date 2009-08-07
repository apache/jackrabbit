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
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.commons.nodetype.InvalidNodeTypeDefException;

import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.RepositoryException;
import java.util.Collection;

/**
 * <code>NodeTypeRegistry</code>...
 *
 */
public interface NodeTypeRegistry {

    /**
     * Returns the node type definition of the node type with the given name.
     *
     * @param nodeTypeName name of node type whose definition should be returned.
     * @return the node type definition of the node type with the given name.
     * @throws NoSuchNodeTypeException if a node type with the given name
     *                                 does not exist
     */
    QNodeTypeDefinition getNodeTypeDefinition(Name nodeTypeName)
            throws NoSuchNodeTypeException;

    /**
     * Add a <code>NodeTypeRegistryListener</code>
     *
     * @param listener the new listener to be informed on (un)registration
     *                 of node types
     */
    void addListener(NodeTypeRegistryListener listener);

    /**
     * Remove a <code>NodeTypeRegistryListener</code>
     *
     * @param listener an existing listener
     */
    void removeListener(NodeTypeRegistryListener listener);

    /**
     * @param ntName
     * @return
     */
    boolean isRegistered(Name ntName);

    /**
     * Returns the names of all registered node types. That includes primary
     * and mixin node types.
     *
     * @return the names of all registered node types.
     */
    public Name[] getRegisteredNodeTypes() throws RepositoryException;

    /**
     * Validates the <code>NodeTypeDef</code> and returns
     * a registered <code>EffectiveNodeType</code> instance.
     * <p/>
     * The validation includes the following checks:
     * <ul>
     * <li>Supertypes must exist and be registered</li>
     * <li>Inheritance graph must not be circular</li>
     * <li>Aggregation of supertypes must not result in name conflicts,
     * ambiguities, etc.</li>
     * <li>Definitions of auto-created properties must specify a name</li>
     * <li>Default values in property definitions must satisfy value constraints
     * specified in the same property definition</li>
     * <li>Definitions of auto-created child-nodes must specify a name</li>
     * <li>Default node type in child-node definitions must exist and be
     * registered</li>
     * <li>The aggregation of the default node types in child-node definitions
     * must not result in name conflicts, ambiguities, etc.</li>
     * <li>Definitions of auto-created child-nodes must not specify default
     * node types which would lead to infinite child node creation
     * (e.g. node type 'A' defines auto-created child node with default
     * node type 'A' ...)</li>
     * <li>Node types specified as constraints in child-node definitions
     * must exist and be registered</li>
     * <li>The aggregation of the node types specified as constraints in
     * child-node definitions must not result in name conflicts, ambiguities,
     * etc.</li>
     * <li>Default node types in child-node definitions must satisfy
     * node type constraints specified in the same child-node definition</li>
     * </ul>
     *
     * @param ntDef the definition of the new node type
     * @return an <code>EffectiveNodeType</code> instance
     * @throws InvalidNodeTypeDefException
     * @throws RepositoryException
     */
    public EffectiveNodeType registerNodeType(QNodeTypeDefinition ntDef)
            throws InvalidNodeTypeDefException, RepositoryException;

    /**
     * Same as <code>{@link #registerNodeType(QNodeTypeDefinition)}</code> except
     * that a collection of <code>NodeTypeDef</code>s is registered instead of
     * just one.
     * <p/>
     * This method can be used to register a set of node types that have
     * dependencies on each other.
     * <p/>
     * Note that in the case an exception is thrown, some node types might have
     * been nevertheless successfully registered.
     *
     * @param ntDefs a collection of <code>NodeTypeDef<code>s
     * @throws InvalidNodeTypeDefException
     * @throws RepositoryException
     */
    public void registerNodeTypes(Collection ntDefs)
            throws InvalidNodeTypeDefException, RepositoryException;

    /**
     * @param nodeTypeName
     * @throws NoSuchNodeTypeException
     * @throws RepositoryException
     */
    public void unregisterNodeType(Name nodeTypeName)
            throws NoSuchNodeTypeException, RepositoryException;

    /**
     * Same as <code>{@link #unregisterNodeType(Name)}</code> except
     * that a set of node types is unregistered instead of just one.
     * <p/>
     * This method can be used to unregister a set of node types that depend on
     * each other.
     *
     * @param nodeTypeNames a collection of <code>Name</code> objects denoting the
     *                node types to be unregistered
     * @throws NoSuchNodeTypeException if any of the specified names does not
     *                                 denote a registered node type.
     * @throws RepositoryException if another error occurs
     * @see #unregisterNodeType(Name)
     */
    public void unregisterNodeTypes(Collection nodeTypeNames)
        throws NoSuchNodeTypeException, RepositoryException;

    /**
     * @param ntd
     * @return
     * @throws NoSuchNodeTypeException
     * @throws InvalidNodeTypeDefException
     * @throws RepositoryException
     */
    public EffectiveNodeType reregisterNodeType(QNodeTypeDefinition ntd)
            throws NoSuchNodeTypeException, InvalidNodeTypeDefException,
            RepositoryException;
}
