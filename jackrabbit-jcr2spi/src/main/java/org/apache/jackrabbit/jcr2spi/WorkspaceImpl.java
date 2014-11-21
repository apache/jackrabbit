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
package org.apache.jackrabbit.jcr2spi;

import org.apache.jackrabbit.jcr2spi.config.RepositoryConfig;
import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyManager;
import org.apache.jackrabbit.jcr2spi.lock.LockManagerImpl;
import org.apache.jackrabbit.jcr2spi.lock.LockStateManager;
import org.apache.jackrabbit.jcr2spi.nodetype.EffectiveNodeTypeProvider;
import org.apache.jackrabbit.jcr2spi.nodetype.ItemDefinitionProvider;
import org.apache.jackrabbit.jcr2spi.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.jcr2spi.nodetype.NodeTypeDefinitionProvider;
import org.apache.jackrabbit.jcr2spi.observation.ObservationManagerImpl;
import org.apache.jackrabbit.jcr2spi.operation.Clone;
import org.apache.jackrabbit.jcr2spi.operation.Copy;
import org.apache.jackrabbit.jcr2spi.operation.Move;
import org.apache.jackrabbit.jcr2spi.operation.Operation;
import org.apache.jackrabbit.jcr2spi.operation.WorkspaceImport;
import org.apache.jackrabbit.jcr2spi.query.QueryManagerImpl;
import org.apache.jackrabbit.jcr2spi.security.AccessManager;
import org.apache.jackrabbit.jcr2spi.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.jcr2spi.state.ItemStateFactory;
import org.apache.jackrabbit.jcr2spi.state.ItemStateValidator;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.UpdatableItemStateManager;
import org.apache.jackrabbit.jcr2spi.version.VersionManager;
import org.apache.jackrabbit.jcr2spi.version.VersionManagerImpl;
import org.apache.jackrabbit.jcr2spi.xml.WorkspaceContentHandler;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.QueryManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import java.io.IOException;
import java.io.InputStream;

/**
 * <code>WorkspaceImpl</code>...
 */
public class WorkspaceImpl implements Workspace, ManagerProvider {

    private static Logger log = LoggerFactory.getLogger(WorkspaceImpl.class);

    /**
     * The name of this <code>Workspace</code>.
     */
    private final String name;
    /**
     * The Session that created this <code>Workspace</code> object.
     */
    protected final SessionImpl session;

    /**
     * WorkspaceManager acting as ItemStateManager on the workspace level
     * and as connection to the SPI implementation.
     */
    private final WorkspaceManager wspManager;

    private LockStateManager lockManager;
    private ObservationManager obsManager;
    private QueryManager qManager;
    private VersionManager versionManager;

    private LockManager jcrLockManager;
    private javax.jcr.version.VersionManager jcrVersionManager;

    public WorkspaceImpl(String name, SessionImpl session, RepositoryConfig config, SessionInfo sessionInfo) throws RepositoryException {
        this.name = name;
        this.session = session;
        wspManager = createManager(config, sessionInfo);
    }

    //----------------------------------------------------------< Workspace >---
    /**
     * @see javax.jcr.Workspace#getSession()
     */
    public Session getSession() {
        return session;
    }

    /**
     * @see javax.jcr.Workspace#getName()
     */
    public String getName() {
        return name;
    }

    /**
     * @see javax.jcr.Workspace#copy(String, String)
     */
    public void copy(String srcAbsPath, String destAbsPath) throws ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException {
        session.checkSupportedOption(Repository.LEVEL_2_SUPPORTED);
        session.checkIsAlive();

        // do within workspace copy
        Path srcPath = session.getQPath(srcAbsPath);
        Path destPath = session.getQPath(destAbsPath);

        Operation op = Copy.create(srcPath, destPath, getName(), this, this);
        getUpdatableItemStateManager().execute(op);
    }

    /**
     * @see javax.jcr.Workspace#copy(String, String, String)
     */
    public void copy(String srcWorkspace, String srcAbsPath, String destAbsPath) throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException {
        session.checkSupportedOption(Repository.LEVEL_2_SUPPORTED);
        session.checkIsAlive();

        // check workspace name
        if (getName().equals(srcWorkspace)) {
            // same as current workspace, delegate to within workspace copy method
            copy(srcAbsPath, destAbsPath);
            return;
        }

        // make sure the specified workspace is visible for the current session.
        session.checkAccessibleWorkspace(srcWorkspace);

        Path srcPath = session.getQPath(srcAbsPath);
        Path destPath = session.getQPath(destAbsPath);

        // copy (i.e. pull) subtree at srcAbsPath from srcWorkspace
        // to 'this' workspace at destAbsPath
        SessionImpl srcSession = null;
        try {
            // create session on other workspace for current subject
            // (may throw NoSuchWorkspaceException and AccessDeniedException)
            srcSession = session.switchWorkspace(srcWorkspace);
            WorkspaceImpl srcWsp = (WorkspaceImpl) srcSession.getWorkspace();

            // do cross-workspace copy
            Operation op = Copy.create(srcPath, destPath, srcWsp.getName(), srcWsp, this);
            getUpdatableItemStateManager().execute(op);
        } finally {
            if (srcSession != null) {
                // we don't need the other session anymore, logout
                srcSession.logout();
            }
        }
    }

    /**
     * @see javax.jcr.Workspace#clone(String, String, String, boolean)
     */
    public void clone(String srcWorkspace, String srcAbsPath, String destAbsPath, boolean removeExisting) throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException {
        session.checkSupportedOption(Repository.LEVEL_2_SUPPORTED);
        session.checkIsAlive();

        // check workspace name
        if (getName().equals(srcWorkspace)) {
            // same as current workspace
            String msg = srcWorkspace + ": illegal workspace (same as current)";
            log.debug(msg);
            throw new RepositoryException(msg);
        }

        // make sure the specified workspace is visible for the current session.
        session.checkAccessibleWorkspace(srcWorkspace);

        Path srcPath = session.getQPath(srcAbsPath);
        Path destPath = session.getQPath(destAbsPath);

        // clone (i.e. pull) subtree at srcAbsPath from srcWorkspace
        // to 'this' workspace at destAbsPath

        SessionImpl srcSession = null;
        try {
            // create session on other workspace for current subject
            // (may throw NoSuchWorkspaceException and AccessDeniedException)
            srcSession = session.switchWorkspace(srcWorkspace);
            WorkspaceImpl srcWsp = (WorkspaceImpl) srcSession.getWorkspace();

            // do clone
            Operation op = Clone.create(srcPath, destPath, srcWsp.getName(), removeExisting, srcWsp, this);
            getUpdatableItemStateManager().execute(op);
        } finally {
            if (srcSession != null) {
                // we don't need the other session anymore, logout
                srcSession.logout();
            }
        }
    }

    /**
     * @see javax.jcr.Workspace#move(String, String)
     */
    public void move(String srcAbsPath, String destAbsPath) throws ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException {
        session.checkSupportedOption(Repository.LEVEL_2_SUPPORTED);
        session.checkIsAlive();

        Path srcPath = session.getQPath(srcAbsPath);
        Path destPath = session.getQPath(destAbsPath);

        Operation op = Move.create(srcPath, destPath, getHierarchyManager(), getPathResolver(), false);
        getUpdatableItemStateManager().execute(op);
    }

    /**
     * @see javax.jcr.Workspace#restore(Version[], boolean)
     */
    public void restore(Version[] versions, boolean removeExisting) throws ItemExistsException, UnsupportedRepositoryOperationException, VersionException, LockException, InvalidItemStateException, RepositoryException {
        getVersionManager().restore(versions, removeExisting);
    }

    /**
     * @see javax.jcr.Workspace#getQueryManager()
     */
    public QueryManager getQueryManager() throws RepositoryException {
        session.checkIsAlive();
        if (qManager == null) {
            qManager = new QueryManagerImpl(session, session,
                    session.getItemManager(), wspManager);
        }
        return qManager;
    }

    /**
     * @see javax.jcr.Workspace#getNamespaceRegistry()
     */
    public NamespaceRegistry getNamespaceRegistry() throws RepositoryException {
        session.checkIsAlive();
        return wspManager.getNamespaceRegistryImpl();
    }

    /**
     * @see javax.jcr.Workspace#getNodeTypeManager()
     */
    public NodeTypeManager getNodeTypeManager() throws RepositoryException {
        session.checkIsAlive();
        return session.getNodeTypeManager();
    }

    /**
     * @see javax.jcr.Workspace#getObservationManager()
     */
    public ObservationManager getObservationManager() throws UnsupportedRepositoryOperationException, RepositoryException {
        session.checkSupportedOption(Repository.OPTION_OBSERVATION_SUPPORTED);
        session.checkIsAlive();

        if (obsManager == null) {
            obsManager = createObservationManager(getNamePathResolver(), getNodeTypeRegistry());
        }
        return obsManager;
    }

    /**
     * @see javax.jcr.Workspace#getAccessibleWorkspaceNames()
     */
    public String[] getAccessibleWorkspaceNames() throws RepositoryException {
        session.checkIsAlive();
        return wspManager.getWorkspaceNames();
    }

    /**
     * @see javax.jcr.Workspace#getImportContentHandler(String, int)
     */
    public ContentHandler getImportContentHandler(String parentAbsPath, int uuidBehavior)
        throws PathNotFoundException, ConstraintViolationException, VersionException,
        LockException, RepositoryException {

        session.checkSupportedOption(Repository.LEVEL_2_SUPPORTED);
        session.checkIsAlive();

        Path parentPath = session.getQPath(parentAbsPath);
        NodeState parentState = getHierarchyManager().getNodeState(parentPath);

        // make sure the given import target is accessible, not locked and checked out.
        int options = ItemStateValidator.CHECK_ACCESS | ItemStateValidator.CHECK_LOCK | ItemStateValidator.CHECK_VERSIONING;
        getValidator().checkIsWritable(parentState, options);

        // build the content handler
        return new WorkspaceContentHandler(this, parentAbsPath, uuidBehavior);
    }

    /**
     * @see javax.jcr.Workspace#importXML(String, InputStream, int)
     */
    public void importXML(String parentAbsPath, InputStream in, int uuidBehavior)
        throws IOException, PathNotFoundException, ItemExistsException,
        ConstraintViolationException, InvalidSerializedDataException,
        LockException, RepositoryException {

        session.checkSupportedOption(Repository.LEVEL_2_SUPPORTED);
        session.checkIsAlive();

        Path parentPath = session.getQPath(parentAbsPath);
        NodeState parentState = getHierarchyManager().getNodeState(parentPath);
        // make sure the given import target is accessible, not locked and checked out.
        int options = ItemStateValidator.CHECK_ACCESS | ItemStateValidator.CHECK_LOCK | ItemStateValidator.CHECK_VERSIONING;
        getValidator().checkIsWritable(parentState, options);

        try {
            // run the import
            wspManager.execute(WorkspaceImport.create(parentState, in, uuidBehavior));
        } finally {
            in. close(); // JCR-2903
        }
    }

    /**
     * @see javax.jcr.Workspace#createWorkspace(String)
     */
    public void createWorkspace(String name) throws RepositoryException {
        session.checkIsAlive();
        session.checkSupportedOption(Repository.OPTION_WORKSPACE_MANAGEMENT_SUPPORTED);
        wspManager.createWorkspace(name, null);
    }


    /**
     * @see javax.jcr.Workspace#createWorkspace(String, String)
     */
    public void createWorkspace(String name, String srcWorkspace) throws RepositoryException {
        session.checkIsAlive();
        session.checkSupportedOption(Repository.OPTION_WORKSPACE_MANAGEMENT_SUPPORTED);
        wspManager.createWorkspace(name, srcWorkspace);
    }


    /**
     * @see javax.jcr.Workspace#deleteWorkspace(String)
     */
    public void deleteWorkspace(String name) throws RepositoryException {
        session.checkIsAlive();
        session.checkSupportedOption(Repository.OPTION_WORKSPACE_MANAGEMENT_SUPPORTED);
        wspManager.deleteWorkspace(name);
    }


    /**
     * @see javax.jcr.Workspace#getLockManager()
     */
    public LockManager getLockManager() throws RepositoryException {
        session.checkIsAlive();
        session.checkSupportedOption(Repository.OPTION_LOCKING_SUPPORTED);
        if (jcrLockManager == null) {
            jcrLockManager = new JcrLockManager(session);
        }
        return jcrLockManager;
    }

    /**
     * @see javax.jcr.Workspace#getVersionManager()
     */
    public synchronized javax.jcr.version.VersionManager getVersionManager()
            throws RepositoryException {
        session.checkIsAlive();
        session.checkSupportedOption(Repository.OPTION_VERSIONING_SUPPORTED);
        if (jcrVersionManager == null) {
            jcrVersionManager = new JcrVersionManager(session);
        }
        return jcrVersionManager;
    }

    //----------------------------------------------------< ManagerProvider >---
    /**
     * @see ManagerProvider#getNamePathResolver()
     */
    public org.apache.jackrabbit.spi.commons.conversion.NamePathResolver getNamePathResolver() {
        return session.getNamePathResolver();
    }

    /**
     * @see ManagerProvider#getNameResolver()
     */
    public NameResolver getNameResolver() {
        return session.getNameResolver();
    }

    /**
     * @see ManagerProvider#getPathResolver()
     */
    public PathResolver getPathResolver() {
        return session.getPathResolver();
    }

    /**
     * @see ManagerProvider#getNamespaceResolver()
     */
    public NamespaceResolver getNamespaceResolver() {
        return session.getNamespaceResolver();
    }

    /**
     * @see ManagerProvider#getHierarchyManager()
     */
    public HierarchyManager getHierarchyManager() {
        return wspManager.getHierarchyManager();
    }

    /**
     * @see ManagerProvider#getAccessManager()
     */
    public AccessManager getAccessManager() {
        return wspManager;
    }

    /**
     * @see ManagerProvider#getLockStateManager()
     */
    public LockStateManager getLockStateManager() {
        if (lockManager == null) {
            lockManager = createLockManager(wspManager, session.getItemManager());
        }
        return lockManager;
    }

    /**
     * @see ManagerProvider#getVersionStateManager()
     */
    public VersionManager getVersionStateManager() {
        if (versionManager == null) {
            versionManager = createVersionManager(wspManager);
        }
        return versionManager;
    }

    /**
     * @see ManagerProvider#getItemDefinitionProvider()
     */
    public ItemDefinitionProvider getItemDefinitionProvider() {
        return wspManager.getItemDefinitionProvider();
    }

    /**
     * @see ManagerProvider#getNodeTypeDefinitionProvider()
     */
    public NodeTypeDefinitionProvider getNodeTypeDefinitionProvider() {
        return session.getNodeTypeManager();
    }

    /**
     * @see ManagerProvider#getEffectiveNodeTypeProvider()
     */
    public EffectiveNodeTypeProvider getEffectiveNodeTypeProvider() {
        return wspManager.getEffectiveNodeTypeProvider();
    }

    /**
     * @see ManagerProvider#getJcrValueFactory()
     */
    public ValueFactory getJcrValueFactory() throws RepositoryException {
        return session.getJcrValueFactory();
    }

    /**
     * @see ManagerProvider#getQValueFactory()
     */
    public QValueFactory getQValueFactory() throws RepositoryException {
        return session.getQValueFactory();
    }

    /**
     * @see ManagerProvider#getAccessControlProvider() ()
     */
    public AccessControlProvider getAccessControlProvider() throws RepositoryException {
        return wspManager.getAccessControlProvider();
    }

    //------------------------------------< implementation specific methods >---
    void dispose() {
        // NOTE: wspManager has already been disposed upon SessionItemStateManager.dispose()
    }

    NameFactory getNameFactory() throws RepositoryException {
        return wspManager.getNameFactory();
    }

    PathFactory getPathFactory() throws RepositoryException {
        return wspManager.getPathFactory();
    }

    IdFactory getIdFactory() throws RepositoryException {
        return wspManager.getIdFactory();
    }

    NodeTypeRegistry getNodeTypeRegistry() {
        return wspManager.getNodeTypeRegistry();
    }

    /**
     * Returns the state manager associated with the workspace
     * represented by <i>this</i> <code>WorkspaceImpl</code> instance.
     *
     * @return the state manager of this workspace
     */
    UpdatableItemStateManager getUpdatableItemStateManager() {
        return wspManager;
    }

    ItemStateFactory getItemStateFactory() {
        return wspManager.getItemStateFactory();
    }

    /**
     * Returns the validator of the session
     *
     * @return validator
     */
    private ItemStateValidator getValidator() {
        return session.getValidator();
    }

    //-----------------------------------------------------< initialization >---
    /**
     * Create the workspace state manager. May be overridden by subclasses.
     *
     * @param config the RepositoryConfiguration
     * @param sessionInfo the SessionInfo used to create this instance.
     * @return workspace manager
     * @throws javax.jcr.RepositoryException If an error occurs
     */
    protected WorkspaceManager createManager(RepositoryConfig config,
                                             SessionInfo sessionInfo) throws RepositoryException {
        return new WorkspaceManager(config, sessionInfo, session.isSupportedOption(Repository.OPTION_OBSERVATION_SUPPORTED));
    }

    /**
     * Create the <code>LockManager</code>. May be overridden by subclasses.
     *
     * @param wspManager the workspace manager.
     * @param itemManager the item manager.
     * @return a new <code>LockStateManager</code> instance.
     */
    protected LockStateManager createLockManager(WorkspaceManager wspManager, ItemManager itemManager) {
        LockManagerImpl lMgr = new LockManagerImpl(wspManager, itemManager, session.getCacheBehaviour());
        session.addListener(lMgr);
        return lMgr;
    }

    /**
     * Create the <code>VersionManager</code>. May be overridden by subclasses.
     *
     * @param wspManager the workspace manager.
     * @return a new <code>VersionManager</code> instance.
     */
    protected VersionManager createVersionManager(WorkspaceManager wspManager) {
        return new VersionManagerImpl(wspManager);
    }

    /**
     * Create the <code>ObservationManager</code>. May be overridden by subclasses.
     *
     * @param resolver the namespace resolver.
     * @param ntRegistry the node type registry.
     * @return a new <code>ObservationManager</code> instance
     * @throws RepositoryException If an error occurs.
     */
    protected ObservationManager createObservationManager(NamePathResolver resolver, NodeTypeRegistry ntRegistry) throws RepositoryException {
        return new ObservationManagerImpl(wspManager, resolver, ntRegistry);
    }
}
