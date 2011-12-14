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
package org.apache.jackrabbit.core.query;

import java.util.concurrent.ScheduledExecutorService;

import org.apache.jackrabbit.core.CachingHierarchyManager;
import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.NamespaceRegistryImpl;
import org.apache.jackrabbit.core.RepositoryContext;
import org.apache.jackrabbit.core.cluster.ClusterNode;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.SharedItemStateManager;

/**
 * Acts as an argument for the {@link QueryHandler} to keep the interface
 * stable. This class provides access to the environment where the query
 * handler is running in.
 */
public class QueryHandlerContext {

    /**
     * The workspace
     */
    private final String workspace;

    /**
     * Repository context.
     */
    private final RepositoryContext repositoryContext;

    /**
     * The persistent <code>ItemStateManager</code>
     */
    private final SharedItemStateManager stateMgr;

    /**
     * The hierarchy manager on top of {@link #stateMgr}.
     */
    private final CachingHierarchyManager hmgr;

    /**
     * The underlying persistence manager.
     */
    private final PersistenceManager pm;

    /**
     * The id of the root node.
     */
    private NodeId rootId;

    /**
     * PropertyType registry to look up the type of a property with a given name.
     */
    private final PropertyTypeRegistry propRegistry;

    /**
     * The query handler for the jcr:system tree
     */
    private final QueryHandler parentHandler;

    /**
     * id of the node that should be excluded from indexing.
     */
    private final NodeId excludedNodeId;

    /**
     * Creates a new context instance.
     *
     * @param workspace          the workspace name.
     * @param repositoryContext  the repository context.
     * @param stateMgr           provides persistent item states.
     * @param pm                 the underlying persistence manager.
     * @param rootId             the id of the root node.
     * @param parentHandler      the parent query handler or <code>null</code> it
     *                           there is no parent handler.
     * @param excludedNodeId     id of the node that should be excluded from
     *                           indexing. Any descendant of that node is also
     *                           excluded from indexing.
     */
    public QueryHandlerContext(
            String workspace,
            RepositoryContext repositoryContext,
            SharedItemStateManager stateMgr,
            PersistenceManager pm,
            NodeId rootId,
            QueryHandler parentHandler,
            NodeId excludedNodeId) {
        this.workspace = workspace;
        this.repositoryContext = repositoryContext;
        this.stateMgr = stateMgr;
        this.hmgr = new CachingHierarchyManager(rootId, stateMgr);
        this.stateMgr.addListener(hmgr);
        this.pm = pm;
        this.rootId = rootId;
        NodeTypeRegistry ntRegistry = repositoryContext.getNodeTypeRegistry();
        propRegistry = new PropertyTypeRegistry(ntRegistry);
        this.parentHandler = parentHandler;
        this.excludedNodeId = excludedNodeId;
        ntRegistry.addListener(propRegistry);
    }

    /**
     * Returns the persistent {@link ItemStateManager}
     * of the workspace this <code>QueryHandler</code> is based on.
     *
     * @return the persistent <code>ItemStateManager</code> of the current
     *         workspace.
     */
    public ItemStateManager getItemStateManager() {
        return stateMgr;
    }

    /**
     * Returns the hierarchy manager on top of the item state manager of this
     * query handler context.
     *
     * @return the hierarchy manager.
     */
    public HierarchyManager getHierarchyManager() {
        return hmgr;
    }

    /**
     * @return the underlying persistence manager.
     */
    public PersistenceManager getPersistenceManager() {
        return pm;
    }

    /**
     * Returns the id of the root node.
     * @return the idof the root node.
     */
    public NodeId getRootId() {
        return rootId;
    }

    /**
     * Returns the PropertyTypeRegistry for this repository.
     * @return the PropertyTypeRegistry for this repository.
     */
    public PropertyTypeRegistry getPropertyTypeRegistry() {
        return propRegistry;
    }

    /**
     * Returns the NodeTypeRegistry for this repository.
     * @return the NodeTypeRegistry for this repository.
     */
    public NodeTypeRegistry getNodeTypeRegistry() {
        return repositoryContext.getNodeTypeRegistry();
    }

    /**
     * Returns the NamespaceRegistryImpl for this repository.
     * @return the NamespaceRegistryImpl for this repository.
     */
    public NamespaceRegistryImpl getNamespaceRegistry() {
        return repositoryContext.getNamespaceRegistry();
    }

    /**
     * Returns the parent query handler.
     * @return the parent query handler.
     */
    public QueryHandler getParentHandler() {
        return parentHandler;
    }

    /**
     * Returns the id of the node that should be excluded from indexing. Any
     * descendant of this node is also excluded from indexing.
     *
     * @return the uuid of the excluded node.
     */
    public NodeId getExcludedNodeId() {
        return excludedNodeId;
    }

    /**
     * Destroys this context and releases resources.
     */
    public void destroy() {
        repositoryContext.getNodeTypeRegistry().removeListener(propRegistry);
    }

    /**
     * Returns the background task executor.
     *
     * @return background task executor
     */
    public ScheduledExecutorService getExecutor() {
        return repositoryContext.getExecutor();
    }

    /**
     * Returns the cluster node instance of this repository, or
     * <code>null</code> if clustering is not enabled.
     * 
     * @return cluster node
     */
    public ClusterNode getClusterNode() {
        return repositoryContext.getClusterNode();
    }

    public String getWorkspace() {
        return workspace;
    }
}
