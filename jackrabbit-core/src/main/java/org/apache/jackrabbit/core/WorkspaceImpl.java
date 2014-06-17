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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.QueryManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionManager;

import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.api.security.authorization.PrivilegeManager;
import org.apache.jackrabbit.commons.AbstractWorkspace;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.lock.LockManager;
import org.apache.jackrabbit.core.lock.LockManagerImpl;
import org.apache.jackrabbit.core.lock.SessionLockManager;
import org.apache.jackrabbit.core.lock.XALockManager;
import org.apache.jackrabbit.core.observation.EventStateCollection;
import org.apache.jackrabbit.core.observation.EventStateCollectionFactory;
import org.apache.jackrabbit.core.observation.ObservationManagerImpl;
import org.apache.jackrabbit.core.query.QueryManagerImpl;
import org.apache.jackrabbit.core.retention.RetentionRegistry;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.core.state.ItemStateCacheFactory;
import org.apache.jackrabbit.core.state.LocalItemStateManager;
import org.apache.jackrabbit.core.state.SharedItemStateManager;
import org.apache.jackrabbit.core.state.XAItemStateManager;
import org.apache.jackrabbit.core.xml.ImportHandler;
import org.apache.jackrabbit.core.xml.Importer;
import org.apache.jackrabbit.core.xml.WorkspaceImporter;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;

/**
 * A <code>WorkspaceImpl</code> ...
 */
public class WorkspaceImpl extends AbstractWorkspace
        implements JackrabbitWorkspace, javax.jcr.Workspace,
        EventStateCollectionFactory {

    private static Logger log = LoggerFactory.getLogger(WorkspaceImpl.class);

    /**
     * The component context of this session.
     */
    protected final SessionContext context;

    /**
     * The configuration of this <code>Workspace</code>
     */
    protected final WorkspaceConfig wspConfig;

    /**
     * The persistent state mgr associated with the workspace represented by <i>this</i>
     * <code>Workspace</code> instance.
     */
    protected final LocalItemStateManager stateMgr;

    /**
     * The hierarchy mgr that reflects persistent state only
     * (i.e. that is isolated from transient changes made through
     * the session).
     */
    protected final CachingHierarchyManager hierMgr;

    /**
     * The <code>ObservationManager</code> instance for this session.
     */
    protected ObservationManagerImpl obsMgr;

    /**
     * The <code>QueryManager</code> for this <code>Workspace</code>.
     */
    protected QueryManagerImpl queryManager;

    /**
     * the session that was used to acquire this <code>Workspace</code>
     */
    protected final SessionImpl session;

    /**
     * The <code>LockManager</code> for this <code>Workspace</code>
     */
    protected LockManager lockMgr;

    /**
     * The API LockManager for this workspace, used to create and release
     * locks and determine the lock status.
     */
    private javax.jcr.lock.LockManager jcr283LockManager;

    /**
     * The API Version manager for this workspace
     */
    protected VersionManagerImpl versionMgr;

    /**
     * The internal manager used to evaluate effective retention policies and
     * holds.
     */
    private RetentionRegistry retentionRegistry;

    /**
     * Creates a new workspace instance
     *
     * @param context component context of this session
     * @param wspConfig The workspace configuration
     * @throws RepositoryException if the workspace can not be accessed 
     */
    public WorkspaceImpl(
            SessionContext context, WorkspaceConfig wspConfig)
            throws RepositoryException {
        this.context = context;
        this.wspConfig = wspConfig;
        this.stateMgr = createItemStateManager();
        this.hierMgr = new CachingHierarchyManager(
                context.getRootNodeId(), this.stateMgr);
        this.stateMgr.addListener(hierMgr);
        this.session = context.getSessionImpl();
    }

    /**
     * The hierarchy manager that reflects workspace state only
     * (i.e. that is isolated from transient changes made through
     * the session)
     *
     * @return the hierarchy manager of this workspace
     */
    public HierarchyManager getHierarchyManager() {
        return hierMgr;
    }

    /**
     * Returns the item state manager associated with the workspace
     * represented by <i>this</i> <code>WorkspaceImpl</code> instance.
     *
     * @return the item state manager of this workspace
     */
    public LocalItemStateManager getItemStateManager() {
        return stateMgr;
    }

    /**
     * Disposes this <code>WorkspaceImpl</code> and frees resources.
     */
    void dispose() {
        if (obsMgr != null) {
            obsMgr.dispose();
            obsMgr = null;
        }
        // remove hierarchy manager as listener to avoid
        // unnecessary work during stateMgr.dispose()
        stateMgr.removeListener(hierMgr);
        stateMgr.dispose();
    }

    /**
     * Performs a sanity check on this workspace and the associated session.
     *
     * @throws RepositoryException if this workspace has been rendered invalid
     *                             for some reason
     */
    public void sanityCheck() throws RepositoryException {
        context.getSessionState().checkAlive();
    }

    //--------------------------------------------------< new JSR 283 methods >
    /**
     * {@inheritDoc}
     */
    public void createWorkspace(String name, String srcWorkspace)
            throws AccessDeniedException, RepositoryException {
        // check state of this instance
        sanityCheck();
        context.getAccessManager().checkRepositoryPermission(Permission.WORKSPACE_MNGMT);

        WorkspaceManager manager = context.getRepositoryContext().getWorkspaceManager();
        manager.createWorkspace(name);

        SessionImpl tmpSession = null;
        try {
            // create a temporary session on new workspace for current subject
            tmpSession = manager.createSession(session.getSubject(), name);
            WorkspaceImpl newWsp = (WorkspaceImpl) tmpSession.getWorkspace();

            // Workspace#clone(String, String, String, boolean) doesn't
            // allow to clone to "/"...
            //newWsp.clone(srcWorkspace, "/", "/", false);
           Node root = getSession().getRootNode();
           for (NodeIterator it = root.getNodes(); it.hasNext(); ) {
               Node child = it.nextNode();
               // skip nodes that already exist in newly created workspace
               if (!tmpSession.nodeExists(child.getPath())) {
                   newWsp.clone(srcWorkspace, child.getPath(), child.getPath(), false);
               }
           }
        } finally {
            if (tmpSession != null) {
                // we don't need the temporary session anymore, logout
                tmpSession.logout();
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Always throws <code>UnsupportedRepositoryOperationException</code> since
     * removal of workspaces is currently not supported.
     */
    public void deleteWorkspace(String name) throws AccessDeniedException,
            UnsupportedRepositoryOperationException, RepositoryException {
        // check if workspace exists (will throw NoSuchWorkspaceException if not)
        context.getRepository().getWorkspaceInfo(name);
        context.getAccessManager().checkRepositoryPermission(Permission.WORKSPACE_MNGMT);

        // todo implement deleteWorkspace
        throw new UnsupportedRepositoryOperationException("not yet implemented");
    }

    /**
     * @see javax.jcr.Workspace#getLockManager()
     * @see javax.jcr.lock.LockManager
     */
    public javax.jcr.lock.LockManager getLockManager() throws UnsupportedRepositoryOperationException, RepositoryException {
        if (jcr283LockManager == null) {
            jcr283LockManager =
                new SessionLockManager(context, getInternalLockManager());
        }
        return jcr283LockManager;
    }

    /**
     * @see javax.jcr.Workspace#getVersionManager()
     */
    public VersionManager getVersionManager() {
        return getVersionManagerImpl();
    }

    VersionManagerImpl getVersionManagerImpl() {
        if (versionMgr == null) {
            versionMgr = new VersionManagerImpl(context, stateMgr, hierMgr);
        }
        return versionMgr;
    }

    //-------------------------------< JackrabbitWorkspace/new JSR 283 method >

    /**
     * Creates a new <code>Workspace</code> with the specified
     * <code>name</code>. The new workspace is empty, meaning it contains only
     * root node.
     * <p>
     * The new workspace can be accessed through a <code>login</code>
     * specifying its name.
     * <p>
     * Throws an <code>AccessDeniedException</code> if the session through which
     * this <code>Workspace</code> object was acquired does not have permission
     * to create the new workspace.
     * <p>
     * Throws an <code>UnsupportedRepositoryOperationException</code> if the repository does
     * not support the creation of workspaces.
     * <p>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param name A <code>String</code>, the name of the new workspace.
     * @throws AccessDeniedException if the session through which
     * this <code>Workspace</code> object was acquired does not have permission
     * to create the new workspace.
     * @throws RepositoryException if another error occurs.
     * @since JCR 2.0
     */
    public void createWorkspace(String name)
            throws AccessDeniedException, RepositoryException {
        // check state of this instance
        sanityCheck();
        context.getAccessManager().checkRepositoryPermission(Permission.WORKSPACE_MNGMT);

        context.getRepositoryContext().getWorkspaceManager().createWorkspace(name);
    }

    //--------------------------------------------------< JackrabbitWorkspace >
    /**
     * Creates a workspace with the given name and a workspace configuration
     * template.
     *
     * @param workspaceName name of the new workspace
     * @param configTemplate the configuration template of the new workspace
     * @throws AccessDeniedException if the current session is not allowed to
     *                               create the workspace
     * @throws RepositoryException   if a workspace with the given name
     *                               already exists or if another error occurs
     * @see #getAccessibleWorkspaceNames()
     */
    public void createWorkspace(
            String workspaceName, InputSource configTemplate)
            throws AccessDeniedException, RepositoryException {
        // check state of this instance
        sanityCheck();
        context.getAccessManager().checkRepositoryPermission(Permission.WORKSPACE_MNGMT);
        context.getRepositoryContext().getWorkspaceManager().createWorkspace(
                workspaceName, configTemplate);
    }

    /**
     * Return the <code>PrivilegeManager</code>.
     *
     * @return
     * @throws RepositoryException
     * @see org.apache.jackrabbit.api.JackrabbitWorkspace#getPrivilegeManager() 
     */
    public PrivilegeManager getPrivilegeManager() throws RepositoryException {
        sanityCheck();
        return context.getPrivilegeManager();
    }


    /**
     * Returns the configuration of this workspace.
     * @return the workspace configuration
     */
    public WorkspaceConfig getConfig() {
        return wspConfig;
    }

    /**
     * @param srcAbsPath
     * @param srcWsp
     * @param destAbsPath
     * @param flag        one of
     *                    <ul>
     *                    <li><code>COPY</code></li>
     *                    <li><code>CLONE</code></li>
     *                    <li><code>CLONE_REMOVE_EXISTING</code></li>
     *                    </ul>
     * @return the path of the node at its new position
     * @throws ConstraintViolationException
     * @throws AccessDeniedException
     * @throws VersionException
     * @throws PathNotFoundException
     * @throws ItemExistsException
     * @throws LockException
     * @throws RepositoryException
     */
    private String internalCopy(String srcAbsPath,
                              WorkspaceImpl srcWsp,
                              String destAbsPath,
                              int flag)
            throws ConstraintViolationException, AccessDeniedException,
            VersionException, PathNotFoundException, ItemExistsException,
            LockException, RepositoryException {

        Path srcPath;
        try {
            srcPath = context.getQPath(srcAbsPath).getNormalizedPath();
        } catch (NameException e) {
            String msg = "invalid path: " + srcAbsPath;
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
        if (!srcPath.isAbsolute()) {
            throw new RepositoryException("not an absolute path: " + srcAbsPath);
        }

        Path destPath;
        try {
            destPath = context.getQPath(destAbsPath).getNormalizedPath();
        } catch (NameException e) {
            String msg = "invalid path: " + destAbsPath;
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
        if (!destPath.isAbsolute()) {
            throw new RepositoryException("not an absolute path: " + destAbsPath);
        }

        BatchedItemOperations ops =
            new BatchedItemOperations(stateMgr, context);

        try {
            ops.edit();
        } catch (IllegalStateException e) {
            String msg = "unable to start edit operation";
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }

        boolean succeeded = false;

        try {
            NodeId id = ops.copy(srcPath, srcWsp.getItemStateManager(),
                    srcWsp.getHierarchyManager(),
                    srcWsp.context.getAccessManager(),
                    destPath, flag);
            ops.update();
            succeeded = true;
            return context.getJCRPath(hierMgr.getPath(id));
        } finally {
            if (!succeeded) {
                // update operation failed, cancel all modifications
                ops.cancel();
            }
        }
    }

    /**
     * Handles a clone inside the same workspace, which is supported with
     * shareable nodes.
     *
     * @see {@link #clone()}
     *
     * @param srcAbsPath source path
     * @param destAbsPath destination path
     * @return the path of the node at its new position
     * @throws ConstraintViolationException
     * @throws AccessDeniedException
     * @throws VersionException
     * @throws PathNotFoundException
     * @throws ItemExistsException
     * @throws LockException
     * @throws RepositoryException
     */
    private String internalClone(String srcAbsPath, String destAbsPath)
            throws ConstraintViolationException, AccessDeniedException,
                   VersionException, PathNotFoundException, ItemExistsException,
                   LockException, RepositoryException {

        Path srcPath;
        try {
            srcPath = context.getQPath(srcAbsPath).getNormalizedPath();
        } catch (NameException e) {
            String msg = "invalid path: " + srcAbsPath;
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
        if (!srcPath.isAbsolute()) {
            throw new RepositoryException("not an absolute path: " + srcAbsPath);
        }

        Path destPath;
        try {
            destPath = context.getQPath(destAbsPath).getNormalizedPath();
        } catch (NameException e) {
            String msg = "invalid path: " + destAbsPath;
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
        if (!destPath.isAbsolute()) {
            throw new RepositoryException("not an absolute path: " + destAbsPath);
        }

        BatchedItemOperations ops =
            new BatchedItemOperations(stateMgr, context);

        try {
            ops.edit();
        } catch (IllegalStateException e) {
            String msg = "unable to start edit operation";
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }

        boolean succeeded = false;

        try {
            ItemId id = ops.clone(srcPath, destPath);
            ops.update();
            succeeded = true;
            return context.getJCRPath(hierMgr.getPath(id));
        } finally {
            if (!succeeded) {
                // update operation failed, cancel all modifications
                ops.cancel();
            }
        }
    }

    /**
     * Return the lock manager for this workspace. If not already done, creates
     * a new instance.
     *
     * @return lock manager for this workspace
     * @throws RepositoryException if an error occurs
     */
    public synchronized org.apache.jackrabbit.core.lock.LockManager getInternalLockManager() throws RepositoryException {

        // check state of this instance
        sanityCheck();

        if (lockMgr == null) {
            lockMgr =
                context.getRepository().getLockManager(wspConfig.getName());
            // FIXME Shouldn't need to use instanceof here
            if (context.getSessionImpl() instanceof XASessionImpl) {
                lockMgr = new XALockManager((LockManagerImpl) lockMgr);
            }
        }
        return lockMgr;
    }

    /**
     * Return the internal effective retention/hold manager for this workspace.
     * If not already done, creates a new instance.
     *
     * @return effective retention/hold manager for this workspace
     * @throws RepositoryException if an error occurs
     */
    synchronized RetentionRegistry getRetentionRegistry() throws RepositoryException {
        // check state of this instance
        sanityCheck();
        if (retentionRegistry == null) {
            retentionRegistry =
                context.getRepository().getRetentionRegistry(wspConfig.getName());
        }
        return retentionRegistry;
    }

    //------------------------------------------------------------< Workspace >
    /**
     * {@inheritDoc}
     */
    public String getName() {
        return wspConfig.getName();
    }

    /**
     * {@inheritDoc}
     */
    public Session getSession() {
        return session;
    }

    /**
     * {@inheritDoc}
     */
    public NamespaceRegistry getNamespaceRegistry() throws RepositoryException {
        return context.getNamespaceRegistry();
    }

    /**
     * {@inheritDoc}
     */
    public NodeTypeManager getNodeTypeManager() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        return context.getNodeTypeManager();
    }

    /**
     * {@inheritDoc}
     */
    public void clone(String srcWorkspace, String srcAbsPath,
                      String destAbsPath, boolean removeExisting)
            throws NoSuchWorkspaceException, ConstraintViolationException,
            VersionException, AccessDeniedException, PathNotFoundException,
            ItemExistsException, LockException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check workspace name
        if (getName().equals(srcWorkspace)) {
            // clone to same workspace is allowed for mix:shareable nodes,
            // but only if removeExisting is false
            if (!removeExisting) {
                internalClone(srcAbsPath, destAbsPath);
                return;
            }
            // same as current workspace
            String msg = srcWorkspace + ": illegal workspace (same as current)";
            log.debug(msg);
            throw new RepositoryException(msg);
        }

        // check authorization for specified workspace
        if (!context.getAccessManager().canAccess(srcWorkspace)) {
            throw new AccessDeniedException("not authorized to access " + srcWorkspace);
        }

        // clone (i.e. pull) subtree at srcAbsPath from srcWorkspace
        // to 'this' workspace at destAbsPath

        SessionImpl srcSession = null;
        try {
            // create session on other workspace for current subject
            // (may throw NoSuchWorkspaceException and AccessDeniedException)
            WorkspaceManager manager =
                context.getRepositoryContext().getWorkspaceManager();
            srcSession = manager.createSession(session.getSubject(), srcWorkspace);
            WorkspaceImpl srcWsp = (WorkspaceImpl) srcSession.getWorkspace();

            // do cross-workspace copy
            int mode = BatchedItemOperations.CLONE;
            if (removeExisting) {
                mode = BatchedItemOperations.CLONE_REMOVE_EXISTING;
            }
            internalCopy(srcAbsPath, srcWsp, destAbsPath, mode);
        } finally {
            if (srcSession != null) {
                // we don't need the other session anymore, logout
                srcSession.logout();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void copy(String srcAbsPath, String destAbsPath)
            throws ConstraintViolationException, VersionException,
            AccessDeniedException, PathNotFoundException, ItemExistsException,
            LockException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // do intra-workspace copy
        internalCopy(srcAbsPath, this, destAbsPath, BatchedItemOperations.COPY);
    }

    /**
     * {@inheritDoc}
     */
    public void copy(String srcWorkspace, String srcAbsPath, String destAbsPath)
            throws NoSuchWorkspaceException, ConstraintViolationException,
            VersionException, AccessDeniedException, PathNotFoundException,
            ItemExistsException, LockException, RepositoryException {

        // check state of this instance
        sanityCheck();

        // check workspace name
        if (getName().equals(srcWorkspace)) {
            // same as current workspace, delegate to intra-workspace copy method
            copy(srcAbsPath, destAbsPath);
            return;
        }

        // check authorization for specified workspace
        if (!context.getAccessManager().canAccess(srcWorkspace)) {
            throw new AccessDeniedException("not authorized to access " + srcWorkspace);
        }

        // copy (i.e. pull) subtree at srcAbsPath from srcWorkspace
        // to 'this' workspace at destAbsPath

        SessionImpl srcSession = null;
        try {
            // create session on other workspace for current subject
            // (may throw NoSuchWorkspaceException and AccessDeniedException)
            WorkspaceManager manager =
                context.getRepositoryContext().getWorkspaceManager();
            srcSession = manager.createSession(session.getSubject(), srcWorkspace);
            WorkspaceImpl srcWsp = (WorkspaceImpl) srcSession.getWorkspace();

            // do cross-workspace copy
            internalCopy(srcAbsPath, srcWsp, destAbsPath, BatchedItemOperations.COPY);
        } finally {
            if (srcSession != null) {
                // we don't need the other session anymore, logout
                srcSession.logout();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void move(String srcAbsPath, String destAbsPath)
            throws ConstraintViolationException, VersionException,
            AccessDeniedException, PathNotFoundException, ItemExistsException,
            LockException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // intra-workspace move...

        Path srcPath;
        try {
            srcPath = context.getQPath(srcAbsPath).getNormalizedPath();
        } catch (NameException e) {
            String msg = "invalid path: " + srcAbsPath;
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
        if (!srcPath.isAbsolute()) {
            throw new RepositoryException("not an absolute path: " + srcAbsPath);
        }

        Path destPath;
        try {
            destPath = context.getQPath(destAbsPath).getNormalizedPath();
        } catch (NameException e) {
            String msg = "invalid path: " + destAbsPath;
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
        if (!destPath.isAbsolute()) {
            throw new RepositoryException("not an absolute path: " + destAbsPath);
        }

        BatchedItemOperations ops =
            new BatchedItemOperations(stateMgr, context);

        try {
            ops.edit();
        } catch (IllegalStateException e) {
            String msg = "unable to start edit operation";
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }

        boolean succeeded = false;

        try {
            ops.move(srcPath, destPath);
            ops.update();
            succeeded = true;
        } finally {
            if (!succeeded) {
                // update operation failed, cancel all modifications
                ops.cancel();
            }
        }
    }

    /**
     * Returns the observation manager of this session.
     *
     * @return the observation manager of this session
     */
    public ObservationManager getObservationManager() {
        return context.getObservationManager();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized QueryManager getQueryManager() throws RepositoryException {

        // check state of this instance
        sanityCheck();

        if (queryManager == null) {
            SearchManager searchManager;
            try {
                searchManager =
                    context.getRepository().getSearchManager(wspConfig.getName());
                if (searchManager == null) {
                    String msg = "no search manager configured for this workspace";
                    log.debug(msg);
                    throw new RepositoryException(msg);
                }
            } catch (NoSuchWorkspaceException nswe) {
                // should never get here
                String msg = "internal error: failed to instantiate query manager";
                log.debug(msg);
                throw new RepositoryException(msg, nswe);
            }
            queryManager = new QueryManagerImpl(context, searchManager);
        }
        return queryManager;
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public void restore(Version[] versions, boolean removeExisting)
            throws ItemExistsException, UnsupportedRepositoryOperationException,
            VersionException, LockException, InvalidItemStateException,
            RepositoryException {
        // check state of this instance
        sanityCheck();
        getVersionManager().restore(versions, removeExisting);
    }

    /**
     * Returns the names of all workspaces of this repository with respect of the
     * access rights of this session.
     *
     * @return the names of all accessible workspaces
     * @throws RepositoryException if an error occurs
     */
    public String[] getAccessibleWorkspaceNames() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        // filter workspaces according to access rights
        List<String> names = new ArrayList<String>();
        WorkspaceManager manager =
            context.getRepositoryContext().getWorkspaceManager();
        for (String name : manager.getWorkspaceNames()) {
            try {
                if (context.getAccessManager().canAccess(name)) {
                    names.add(name);
                }
            } catch (NoSuchWorkspaceException e) {
                log.warn("Workspace disappeared unexpectedly: " + name, e);
            }
        }
        return names.toArray(new String[names.size()]);
    }


    /**
     * {@inheritDoc}
     */
    public ContentHandler getImportContentHandler(String parentAbsPath,
                                                  int uuidBehavior)
            throws PathNotFoundException, ConstraintViolationException,
            VersionException, LockException, RepositoryException {

        // check state of this instance
        sanityCheck();

        Path parentPath;
        try {
            parentPath = context.getQPath(parentAbsPath).getNormalizedPath();
        } catch (NameException e) {
            String msg = "invalid path: " + parentAbsPath;
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
        if (!parentPath.isAbsolute()) {
            throw new RepositoryException("not an absolute path: " + parentAbsPath);
        }

        Importer importer = new WorkspaceImporter(
                parentPath, this, context,
                uuidBehavior, wspConfig.getImportConfig());
        return new ImportHandler(importer, getSession());
    }

    /**
     * Returns the shared item state manager of this workspace.
     *
     * @return shared item state manager
     * @throws RepositoryException if the workspace can not be accessed
     */
    protected SharedItemStateManager getSharedItemStateManager()
            throws RepositoryException {
        WorkspaceManager manager =
            context.getRepositoryContext().getWorkspaceManager();
        return manager.getWorkspaceStateManager(getName());
    }

    /**
     * Create the persistent item state manager on top of the shared item
     * state manager.
     *
     * @return local item state manager
     * @throws RepositoryException if the workspace can not be accessed 
     */
    protected LocalItemStateManager createItemStateManager()
            throws RepositoryException {
        SharedItemStateManager sism = getSharedItemStateManager();
        ItemStateCacheFactory iscf =
            context.getRepositoryContext().getItemStateCacheFactory();

        // FIXME We should be able to avoid the instanceof operator here
        if (context.getSessionImpl() instanceof XASessionImpl) {
            return XAItemStateManager.createInstance(
                    sism, this, null, iscf);
        } else {
            return LocalItemStateManager.createInstance(
                    sism, this, iscf);
        }

    }

    //------------------------------------------< EventStateCollectionFactory >
    /**
     * {@inheritDoc}
     * <p>
     * Implemented in this object and forwarded rather than {@link #obsMgr}
     * since creation of the latter is lazy.
     */
    public EventStateCollection createEventStateCollection()
            throws RepositoryException {

        return ((ObservationManagerImpl) getObservationManager()).createEventStateCollection();
    }
}


