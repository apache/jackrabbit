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

import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeTypeExistsException;
import javax.jcr.nodetype.InvalidNodeTypeDefinitionException;
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
     * Registers the specified node type definitions. If <code>allowUpdate</code>
     * is <code>true</code> existing node types will be updated, otherwise
     * an <code>NodeTypeExistsException</code> is thrown.
     *
     * @param ntDefs
     * @param allowUpdate
     * @throws NodeTypeExistsException
     * @throws InvalidNodeTypeDefinitionException
     * @throws RepositoryException
     */
    public void registerNodeTypes(Collection<QNodeTypeDefinition> ntDefs, boolean allowUpdate) throws NodeTypeExistsException, InvalidNodeTypeDefinitionException, RepositoryException;

    /**
     * Unregisters a collection of node types.
     *
     * @param nodeTypeNames a collection of <code>Name</code> objects denoting the
     *                node types to be unregistered
     * @throws NoSuchNodeTypeException if any of the specified names does not
     *                                 denote a registered node type.
     * @throws RepositoryException if another error occurs
     */
    public void unregisterNodeTypes(Collection<Name> nodeTypeNames)
        throws NoSuchNodeTypeException, RepositoryException;
}
