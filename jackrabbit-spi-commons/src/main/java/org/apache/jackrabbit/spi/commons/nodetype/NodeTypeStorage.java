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

import java.util.Iterator;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeTypeExistsException;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;

/**
 * <code>NodeTypeStorage</code> provides means for storing {@link QNodeTypeDefinition}s.
 */
public interface NodeTypeStorage {

    /**
     * Returns an Iterator over all node type definitions registered.
     *
     * @return
     * @throws RepositoryException
     */
    public Iterator<QNodeTypeDefinition> getAllDefinitions() throws RepositoryException;

    /**
     * Returns the <code>QNodeTypeDefinition</code>s for the given node type
     * names. The implementation is free to return additional definitions e.g.
     * dependencies.
     *
     * @param nodeTypeNames
     * @return
     * @throws NoSuchNodeTypeException
     * @throws RepositoryException
     */
    public Iterator<QNodeTypeDefinition> getDefinitions(Name[] nodeTypeNames) throws NoSuchNodeTypeException, RepositoryException;

    /**
     * Add all {@link QNodeTypeDefinition}s provided to the store. If <code>allowUpdate</code> is <code>true</code>
     * previously registered node QNodeTypeDefinitions will be overwritten.
     * @param nodeTypeDefs  QNodeTypeDefinitions to add to the store
     * @param allowUpdate   Whether to overwrite existing QNodeTypeDefinitions
     * @throws RepositoryException
     * @throws NodeTypeExistsException  If <code>allowUpdate</code> is <code>true</code> and a QNodeTypeDefinitions
     *   of that name already exists. In this case, none of the provided QNodeTypeDefinitions is registered.
     */
    public void registerNodeTypes(QNodeTypeDefinition[] nodeTypeDefs, boolean allowUpdate)
            throws RepositoryException, NodeTypeExistsException;

    /**
     * Remove all {@link QNodeTypeDefinition}s provided from the store.
     * @param nodeTypeNames   QNodeTypeDefinitions to remove from the store
     * @throws RepositoryException
     * @throws NoSuchNodeTypeException  If any of the QNodeTypeDefinitions does not exist. In this case
     * none of the provided is unregistered.
     */
    public void unregisterNodeTypes(Name[] nodeTypeNames) throws NoSuchNodeTypeException, RepositoryException;
}