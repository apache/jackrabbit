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
package org.apache.jackrabbit.core;

import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.version.InternalVersionManagerImpl;

/**
 * Internal component context of a Jackrabbit content repository.
 * A repository context consists of the internal repository-level
 * components and resources like the namespace and node type
 * registries. Access to these resources is available only to objects
 * with a reference to the context object.
 */
public class RepositoryContext {

    /**
     * The repository instance to which this context is associated.
     */
    private final RepositoryImpl repository;

    /**
     * The namespace registry of this repository.
     */
    private NamespaceRegistryImpl namespaceRegistry;

    /**
     * The node type registry of this repository.
     */
    private NodeTypeRegistry nodeTypeRegistry;

    /**
     * The internal version manager of this repository.
     */
    private InternalVersionManagerImpl internalVersionManager;

    /**
     * The root node identifier of this repository.
     */
    private NodeId rootNodeId;

    /**
     * The repository file system.
     */
    private FileSystem fileSystem;

    /**
     * Creates a component context for the given repository.
     *
     * @param repository repository instance
     */
    RepositoryContext(RepositoryImpl repository) {
        assert repository != null;
        this.repository = repository;
    }

    /**
     * Returns the repository instance to which this context is associated.
     *
     * @return repository instance
     */
    public RepositoryImpl getRepository() {
        return repository;
    }

    /**
     * Returns the namespace registry of this repository.
     *
     * @return namespace registry
     */
    public NamespaceRegistryImpl getNamespaceRegistry() {
        assert namespaceRegistry != null;
        return namespaceRegistry;
    }

    /**
     * Sets the namespace registry of this repository.
     *
     * @param namespaceRegistry namespace registry
     */
    void setNamespaceRegistry(NamespaceRegistryImpl namespaceRegistry) {
        assert namespaceRegistry != null;
        this.namespaceRegistry = namespaceRegistry;
    }

    /**
     * Returns the namespace registry of this repository.
     *
     * @return node type registry
     */
    public NodeTypeRegistry getNodeTypeRegistry() {
        assert nodeTypeRegistry != null;
        return nodeTypeRegistry;
    }

    /**
     * Sets the node type registry of this repository.
     *
     * @param nodeTypeRegistry node type registry
     */
    void setNodeTypeRegistry(NodeTypeRegistry nodeTypeRegistry) {
        assert nodeTypeRegistry != null;
        this.nodeTypeRegistry = nodeTypeRegistry;
    }

    /**
     * Returns the internal version manager of this repository.
     *
     * @return internal version manager
     */
    public InternalVersionManagerImpl getInternalVersionManager() {
        assert internalVersionManager != null;
        return internalVersionManager;
    }

    /**
     * Sets the internal version manager of this repository.
     *
     * @param internalVersionManager internal version manager
     */
    void setInternalVersionManager(
            InternalVersionManagerImpl internalVersionManager) {
        assert internalVersionManager != null;
        this.internalVersionManager = internalVersionManager;
    }

    /**
     * Returns the root node identifier of this repository.
     *
     * @return root node identifier
     */
    public NodeId getRootNodeId() {
        assert rootNodeId != null;
        return rootNodeId;
    }

    /**
     * Sets the root node identifier of this repository.
     *
     * @param rootNodeId root node identifier
     */
    void setRootNodeId(NodeId rootNodeId) {
        assert rootNodeId != null;
        this.rootNodeId = rootNodeId;
    }

    /**
     * Returns the repository file system.
     *
     * @return repository file system
     */
    public FileSystem getFileSystem() {
        assert fileSystem != null;
        return fileSystem;
    }

    /**
     * Sets the repository file system.
     *
     * @param fileSystem repository file system
     */
    public void setFileSystem(FileSystem fileSystem) {
        assert fileSystem != null;
        this.fileSystem = fileSystem;
    }

}
