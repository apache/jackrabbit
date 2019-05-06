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

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;

import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.RepositoryImpl.WorkspaceInfo;
import org.apache.jackrabbit.core.cluster.ClusterNode;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.NodeIdFactory;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.security.JackrabbitSecurityManager;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.core.state.ItemStateCacheFactory;
import org.apache.jackrabbit.stats.RepositoryStatisticsImpl;
import org.apache.jackrabbit.core.stats.StatManager;
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
     * The privilege registry for this repository.
     */
    private PrivilegeRegistry privilegeRegistry;

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
     * The data store of this repository, or <code>null</code>.
     */
    private DataStore dataStore;

    /**
     * The cluster node instance of this repository, or <code>null</code>.
     */
    private ClusterNode clusterNode;

    /**
     * Workspace manager of this repository.
     */
    private WorkspaceManager workspaceManager;

    /**
     * Security manager of this repository;
     */
    private JackrabbitSecurityManager securityManager;

    /**
     * Item state cache factory of this repository.
     */
    private ItemStateCacheFactory itemStateCacheFactory;

    private NodeIdFactory nodeIdFactory;

    /**
     * Thread pool of this repository.
     */
    private final ScheduledExecutorService executor =
            new JackrabbitThreadPool();

    /**
     * Repository statistics collector.
     */
    private final RepositoryStatisticsImpl statistics;

    /**
     * The Statistics manager, handles statistics
     */
    private StatManager statManager;
    
    /**
     *  flag to indicate if GC is running
     */
    private volatile boolean gcRunning;

    /**
     * Creates a component context for the given repository.
     *
     * @param repository repository instance
     */
    RepositoryContext(RepositoryImpl repository) {
        assert repository != null;
        this.repository = repository;
        this.statistics = new RepositoryStatisticsImpl(executor);
        this.statManager = new StatManager();
    }

    /**
     * Starts a repository with the given configuration and returns
     * the internal component context of the started repository.
     *
     * @since Apache Jackrabbit 2.3.1
     * @param config repository configuration
     * @return component context of the repository
     * @throws RepositoryException if the repository could not be started
     */
    public static RepositoryContext create(RepositoryConfig config)
            throws RepositoryException {
        RepositoryImpl repository = RepositoryImpl.create(config);
        return repository.getRepositoryContext();
    }

    /**
     * Starts a repository in the given directory and returns the
     * internal component context of the started repository. If needed,
     * the directory is created and a default repository configuration
     * is installed inside it.
     *
     * @since Apache Jackrabbit 2.3.1
     * @see RepositoryConfig#install(File)
     * @param dir repository directory
     * @return component context of the repository
     * @throws RepositoryException if the repository could not be started
     * @throws IOException if the directory could not be initialized
     */
    public static RepositoryContext install(File dir)
            throws RepositoryException, IOException {
        return create(RepositoryConfig.install(dir));
    }

    public RepositoryConfig getRepositoryConfig() {
        return repository.getConfig();
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
     * Returns the thread pool of this repository.
     *
     * @return repository thread pool
     */
    public ScheduledExecutorService getExecutor() {
        return executor;
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
     * Returns the privilege registry of this repository.
     * 
     * @return the privilege registry of this repository.
     */
    public PrivilegeRegistry getPrivilegeRegistry() {
        return privilegeRegistry;
    }

    /**
     * Sets the privilege registry of this repository.
     *
     * @param privilegeRegistry
     */
    void setPrivilegeRegistry(PrivilegeRegistry privilegeRegistry) {
        assert privilegeRegistry != null;
        this.privilegeRegistry = privilegeRegistry;
    }

    /**
     * Returns the internal version manager of this repository.
     *
     * @return internal version manager
     */
    public InternalVersionManagerImpl getInternalVersionManager() {
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
    void setFileSystem(FileSystem fileSystem) {
        assert fileSystem != null;
        this.fileSystem = fileSystem;
    }

    /**
     * Returns the data store of this repository, or <code>null</code>
     * if a data store is not configured.
     *
     * @return data store, or <code>null</code>
     */
    public DataStore getDataStore() {
        return dataStore;
    }

    /**
     * Sets the data store of this repository.
     *
     * @param dataStore data store
     */
    void setDataStore(DataStore dataStore) {
        assert dataStore != null;
        this.dataStore = dataStore;
    }

    /**
     * Returns the cluster node instance of this repository, or
     * <code>null</code> if clustering is not enabled.
     *
     * @return cluster node
     */
    public ClusterNode getClusterNode() {
        return clusterNode;
    }

    /**
     * Sets the cluster node instance of this repository.
     *
     * @param clusterNode cluster node
     */
    void setClusterNode(ClusterNode clusterNode) {
        assert clusterNode != null;
        this.clusterNode = clusterNode;
    }

    /**
     * Returns the workspace manager of this repository.
     *
     * @return workspace manager
     */
    public WorkspaceManager getWorkspaceManager() {
        assert workspaceManager != null;
        return workspaceManager;
    }

    /**
     * Sets the workspace manager of this repository.
     *
     * @param workspaceManager workspace manager
     */
    void setWorkspaceManager(WorkspaceManager workspaceManager) {
        assert workspaceManager != null;
        this.workspaceManager = workspaceManager;
    }

    /**
     * Returns the {@link WorkspaceInfo} for the named workspace.
     *
     * @param workspaceName The name of the workspace whose {@link WorkspaceInfo}
     *                      is to be returned. This must not be <code>null</code>.
     * @return The {@link WorkspaceInfo} for the named workspace. This will
     *         never be <code>null</code>.
     * @throws NoSuchWorkspaceException If the named workspace does not exist.
     * @throws RepositoryException If this repository has been shut down.
     */
    public WorkspaceInfo getWorkspaceInfo(String workspaceName)
            throws NoSuchWorkspaceException, RepositoryException {
        return repository.getWorkspaceInfo(workspaceName);
    }

    /**
     * Returns the security manager of this repository.
     *
     * @return security manager
     */
    public JackrabbitSecurityManager getSecurityManager() {
        assert securityManager != null;
        return securityManager;
    }

    /**
     * Sets the security manager of this repository.
     *
     * @param securityManager security manager
     */
    void setSecurityManager(JackrabbitSecurityManager securityManager) {
        assert securityManager != null;
        this.securityManager = securityManager;
    }

    /**
     * Returns the item state cache factory of this repository.
     *
     * @return item state cache factory
     */
    public ItemStateCacheFactory getItemStateCacheFactory() {
        assert itemStateCacheFactory != null;
        return itemStateCacheFactory;
    }

    /**
     * Sets the item state cache factory of this repository.
     *
     * @param itemStateCacheFactory item state cache factory
     */
    void setItemStateCacheFactory(ItemStateCacheFactory itemStateCacheFactory) {
        assert itemStateCacheFactory != null;
        this.itemStateCacheFactory = itemStateCacheFactory;
    }

    public void setNodeIdFactory(NodeIdFactory nodeIdFactory) {
        this.nodeIdFactory = nodeIdFactory;
    }

    public NodeIdFactory getNodeIdFactory() {
        return nodeIdFactory;
    }

    /**
     * Returns the repository statistics collector.
     *
     * @return repository statistics collector
     */
    public RepositoryStatisticsImpl getRepositoryStatistics() {
        return statistics;
    }

    /**
     * @return the statistics manager object
     */
    public StatManager getStatManager() {
        return statManager;
    }

    /**
     * 
     * @return gcRunning status
     */
    public synchronized boolean isGcRunning() {
        return gcRunning;
    }

    /**
     * set gcRunnign status
     * @param gcRunning
     */
    public synchronized void setGcRunning(boolean gcRunning) {
        this.gcRunning = gcRunning;
    }
    
    

}
