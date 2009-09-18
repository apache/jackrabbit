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
package org.apache.jackrabbit.core.cluster;

import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import java.util.Collection;

/**
 * Interface used to receive information about incoming, external node type registry events.
 */
public interface NodeTypeEventListener {

    /**
     * Called when one or more node types have been externally registered.
     *
     * @param ntDefs node type definitions
     * @throws RepositoryException if an error occurs
     * @throws InvalidNodeTypeDefException if the node type definition is invalid
     */
    void externalRegistered(Collection<QNodeTypeDefinition> ntDefs)
        throws RepositoryException, InvalidNodeTypeDefException;

    /**
     * Called when a node type has been externally re-registered.
     *
     * @param ntDef node type definition
     * @throws RepositoryException if an error occurs
     * @throws NoSuchNodeTypeException if the node type had not yet been registered
     * @throws InvalidNodeTypeDefException if the node type definition is invalid
     */
    void externalReregistered(QNodeTypeDefinition ntDef)
        throws NoSuchNodeTypeException, InvalidNodeTypeDefException, RepositoryException;

    /**
     * Called when one or more node types have been externally unregistered.
     *
     * @param ntNames node type qnames
     * @throws RepositoryException if an error occurs
     * @throws NoSuchNodeTypeException if a node type is already unregistered
     */
    void externalUnregistered(Collection<Name> ntNames)
        throws RepositoryException, NoSuchNodeTypeException;

}
