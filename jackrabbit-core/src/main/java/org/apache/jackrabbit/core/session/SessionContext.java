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
package org.apache.jackrabbit.core.session;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.ItemValidator;
import org.apache.jackrabbit.core.RepositoryContext;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.state.SessionItemStateManager;

public class SessionContext {

    private final RepositoryContext repositoryContext;

    private final SessionImpl session;

    /**
     * The state of this session.
     */
    private final SessionState state;

    /**
     * The item state manager of this session
     */
    private volatile SessionItemStateManager itemStateManager;

    /**
     * The item manager of this session
     */
    private volatile ItemManager itemManager;

    /**
     * The item validator of this session
     */
    private volatile ItemValidator itemValidator;

    /**
     * The access manager of this session
     */
    private volatile AccessManager accessManager;

    public SessionContext(
            RepositoryContext repositoryContext, SessionImpl session) {
        assert repositoryContext != null;
        assert session != null;
        this.repositoryContext = repositoryContext;
        this.session = session;
        this.state = new SessionState();
    }

    public RepositoryContext getRepositoryContext() {
        return repositoryContext;
    }

    /**
     * Returns the root node identifier of the repository.
     *
     * @return root node identifier
     */
    public NodeId getRootNodeId() {
        return repositoryContext.getRootNodeId();
    }

    /**
     * Returns the data store of this repository, or <code>null</code>
     * if a data store is not configured.
     *
     * @return data store, or <code>null</code>
     */
    public DataStore getDataStore() {
        return repositoryContext.getDataStore();
    }

    public SessionImpl getSessionImpl() {
        return session;
    }

    public SessionState getSessionState() {
        return state;
    }

    public SessionItemStateManager getItemStateManager() {
        assert itemStateManager != null;
        return itemStateManager;
    }

    public void setItemStateManager(SessionItemStateManager itemStateManager) {
        assert itemStateManager != null;
        this.itemStateManager = itemStateManager;
    }

    public HierarchyManager getHierarchyManager() {
        assert itemStateManager != null;
        return itemStateManager.getHierarchyMgr();
    }

    public ItemManager getItemManager() {
        assert itemManager != null;
        return itemManager;
    }

    public void setItemManager(ItemManager itemManager) {
        assert itemManager != null;
        this.itemManager = itemManager;
    }

    public ItemValidator getItemValidator() {
        assert itemValidator != null;
        return itemValidator;
    }

    public void setItemValidator(ItemValidator itemValidator) {
        assert itemValidator != null;
        this.itemValidator = itemValidator;
    }

    public AccessManager getAccessManager() {
        assert accessManager != null;
        return accessManager;
    }

    public void setAccessManager(AccessManager accessManager) {
        assert accessManager != null;
        this.accessManager = accessManager;
    }

}
