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

import org.apache.jackrabbit.api.security.authorization.PrivilegeManager;
import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.ItemValidator;
import org.apache.jackrabbit.core.RepositoryContext;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.WorkspaceImpl;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.NodeIdFactory;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.observation.ObservationManagerImpl;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.security.authorization.PrivilegeManagerImpl;
import org.apache.jackrabbit.core.state.SessionItemStateManager;
import org.apache.jackrabbit.core.value.ValueFactoryImpl;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;

import javax.jcr.AccessDeniedException;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFactory;

/**
 * Component context of a session. This class keeps track of the internal
 * components associated with a session.
 */
public class SessionContext implements NamePathResolver {

    /**
     * The repository context of this session.
     */
    private final RepositoryContext repositoryContext;

    /**
     * This session.
     */
    private final SessionImpl session;

    /**
     * The state of this session.
     */
    private final SessionState state;

    /**
     * The value factory of this session
     */
    private final ValueFactory valueFactory;

    /**
     * The item validator of this session
     */
    private final ItemValidator itemValidator;

    /**
     * Node type manager of this session
     */
    private final NodeTypeManagerImpl nodeTypeManager;

    /**
     * Privilege manager of this session.
     */
    private final PrivilegeManagerImpl privilegeManager;

    /**
     * The namespace registry exposed for this session context that includes
     * permission checks.
     */
    private final NamespaceRegistry nsRegistry;

    /**
     * The workspace of this session
     */
    private final WorkspaceImpl workspace;

    /**
     * The item state manager of this session
     */
    private volatile SessionItemStateManager itemStateManager;

    /**
     * The item manager of this session
     */
    private volatile ItemManager itemManager;

    /**
     * The access manager of this session
     */
    private volatile AccessManager accessManager;

    /**
     * The observation manager of this session.
     */
    private volatile ObservationManagerImpl observationManager;

    /**
     * Creates a component context for the given session.
     *
     * @param repositoryContext repository context of the session
     * @param session the session
     * @param workspaceConfig workspace configuration
     * @throws RepositoryException if the workspace can not be accessed
     */
    public SessionContext(
            RepositoryContext repositoryContext, SessionImpl session,
            WorkspaceConfig workspaceConfig) throws RepositoryException {
        assert repositoryContext != null;
        assert session != null;
        this.repositoryContext = repositoryContext;
        this.session = session;
        this.state = new SessionState(this);
        this.valueFactory =
            new ValueFactoryImpl(session, repositoryContext.getDataStore());
        this.itemValidator = new ItemValidator(this);
        this.nodeTypeManager = new NodeTypeManagerImpl(this);
        this.privilegeManager = new PrivilegeManagerImpl(repositoryContext.getPrivilegeRegistry(), session);
        this.nsRegistry = new PermissionAwareNamespaceRegistry();
        this.workspace = new WorkspaceImpl(this, workspaceConfig);
    }

    //-------------------------------------------< per-repository components >

    /**
     * Returns the repository context of the session.
     *
     * @return repository context
     */
    public RepositoryContext getRepositoryContext() {
        return repositoryContext;
    }

    /**
     * Returns this repository.
     *
     * @return repository
     */
    public RepositoryImpl getRepository() {
        return repositoryContext.getRepository();
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

    /**
     * Returns the node type registry of this repository.
     *
     * @return node type registry
     */
    public NodeTypeRegistry getNodeTypeRegistry() {
        return repositoryContext.getNodeTypeRegistry();
    }

    //----------------------------------------------< per-session components >

    /**
     * Returns this session.
     *
     * @return session
     */
    public SessionImpl getSessionImpl() {
        return session;
    }

    /**
     * Returns the state of this session.
     *
     * @return session state
     */
    public SessionState getSessionState() {
        return state;
    }

    /**
     * Returns the value factory of this session.
     *
     * @return value factory
     */
    public ValueFactory getValueFactory() {
        return valueFactory;
    }

    /**
     * Returns the item validator of this session.
     *
     * @return item validator
     */
    public ItemValidator getItemValidator() {
        return itemValidator;
    }

    /**
     * Returns the node type manager of this session.
     *
     * @return node type manager
     */
    public NodeTypeManagerImpl getNodeTypeManager() {
        return nodeTypeManager;
    }

    /**
     * Returns the privilege manager of this session.
     *
     * @return the privilege manager.
     */
    public PrivilegeManagerImpl getPrivilegeManager() {
        return privilegeManager;
    }

   /**
     * Returns a namespace registry instance which asserts that the editing
     * session is allowed to modify the namespace registry.
     *
     * @return
     */
    public NamespaceRegistry getNamespaceRegistry() {
        return nsRegistry;
    }

    /**
     * Returns the workspace of this session.
     *
     * @return workspace
     */
    public WorkspaceImpl getWorkspace() {
        return workspace;
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

    public AccessManager getAccessManager() {
        assert accessManager != null;
        return accessManager;
    }

    public void setAccessManager(AccessManager accessManager) {
        assert accessManager != null;
        this.accessManager = accessManager;
    }

    public ObservationManagerImpl getObservationManager() {
        assert observationManager != null;
        return observationManager;
    }

    public void setObservationManager(
            ObservationManagerImpl observationManager) {
        assert observationManager != null;
        this.observationManager = observationManager;
    }

    public NodeIdFactory getNodeIdFactory() {
        return repositoryContext.getNodeIdFactory();
    }

    //--------------------------------------------------------< NameResolver >

    public Name getQName(String name)
            throws IllegalNameException, NamespaceException {
        return session.getQName(name);
    }

    public String getJCRName(Name name) throws NamespaceException {
        return session.getJCRName(name);
    }

    //--------------------------------------------------------< PathResolver >

    public Path getQPath(String path)
            throws MalformedPathException, IllegalNameException,
            NamespaceException {
        return session.getQPath(path);
    }

    public Path getQPath(String path, boolean normalizeIdentifier)
            throws MalformedPathException, IllegalNameException,
            NamespaceException {
        return session.getQPath(path, normalizeIdentifier);
    }

    public String getJCRPath(Path path) throws NamespaceException {
        return session.getJCRPath(path);
    }

    //--------------------------------------------------------------< Object >

    /**
     * Dumps the session internals to a string.
     *
     * @return string representation of session internals
     */
    @Override
    public String toString() {
        return session + ":\n" + itemManager + "\n" + itemStateManager;
    }

    //--------------------------------------------------------------------------
    /**
     * Permission aware namespace registry implementation that makes sure that
     * modifications of the namespace registry are only allowed if the editing
     * session has the corresponding permissions.
     */
    private class PermissionAwareNamespaceRegistry implements NamespaceRegistry {

        private final NamespaceRegistry nsRegistry = repositoryContext.getNamespaceRegistry();        

        public void registerNamespace(String prefix, String uri) throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException {
            session.getAccessManager().checkRepositoryPermission(Permission.NAMESPACE_MNGMT);
            nsRegistry.registerNamespace(prefix, uri);
        }

        public void unregisterNamespace(String prefix) throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException {
            session.getAccessManager().checkRepositoryPermission(Permission.NAMESPACE_MNGMT);
            nsRegistry.unregisterNamespace(prefix);
        }

        public String[] getPrefixes() throws RepositoryException {
            return nsRegistry.getPrefixes();
        }

        public String[] getURIs() throws RepositoryException {
            return nsRegistry.getURIs();
        }

        public String getURI(String prefix) throws NamespaceException, RepositoryException {
            return nsRegistry.getURI(prefix);
        }

        public String getPrefix(String uri) throws NamespaceException, RepositoryException {
            return nsRegistry.getPrefix(uri);
        }
    }

}
