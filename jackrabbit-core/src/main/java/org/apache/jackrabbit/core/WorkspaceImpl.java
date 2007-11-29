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

import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.lock.LockManager;
import org.apache.jackrabbit.core.observation.EventStateCollection;
import org.apache.jackrabbit.core.observation.EventStateCollectionFactory;
import org.apache.jackrabbit.core.observation.ObservationManagerImpl;
import org.apache.jackrabbit.core.query.QueryManagerImpl;
import org.apache.jackrabbit.core.state.LocalItemStateManager;
import org.apache.jackrabbit.core.state.SharedItemStateManager;
import org.apache.jackrabbit.core.version.DateVersionSelector;
import org.apache.jackrabbit.core.version.VersionImpl;
import org.apache.jackrabbit.core.version.VersionSelector;
import org.apache.jackrabbit.core.xml.ImportHandler;
import org.apache.jackrabbit.core.xml.Importer;
import org.apache.jackrabbit.core.xml.WorkspaceImporter;
import org.apache.jackrabbit.commons.AbstractWorkspace;
import org.apache.jackrabbit.conversion.NameException;
import org.apache.jackrabbit.spi.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.QueryManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

import java.util.HashMap;
import java.util.Iterator;

/**
 * A <code>WorkspaceImpl</code> ...
 */
public class WorkspaceImpl extends AbstractWorkspace
        implements JackrabbitWorkspace, EventStateCollectionFactory {

    private static Logger log = LoggerFactory.getLogger(WorkspaceImpl.class);

    /**
     * The configuration of this <code>Workspace</code>
     */
    protected final WorkspaceConfig wspConfig;

    /**
     * The repository that created this workspace instance
     */
    protected final RepositoryImpl rep;

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
     * Protected constructor.
     *
     * @param wspConfig The workspace configuration
     * @param stateMgr  The shared item state manager
     * @param rep       The repository
     * @param session   The session
     */
    protected WorkspaceImpl(WorkspaceConfig wspConfig,
                            SharedItemStateManager stateMgr, RepositoryImpl rep,
                            SessionImpl session) {
        this.wspConfig = wspConfig;
        this.rep = rep;
        this.stateMgr = createItemStateManager(stateMgr);
        this.hierMgr = new CachingHierarchyManager(
                rep.getRootNodeId(), this.stateMgr, session);
        this.stateMgr.addListener(hierMgr);
        this.session = session;
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
        stateMgr.dispose();
    }

    /**
     * Performs a sanity check on this workspace and the associated session.
     *
     * @throws RepositoryException if this workspace has been rendered invalid
     *                             for some reason
     */
    public void sanityCheck() throws RepositoryException {
        // check session status
        session.sanityCheck();
    }

    //--------------------------------------------------< new JSR 283 methods >
    /**
     * Creates a new <code>Workspace</code> with the specified <code>name</code>
     * initialized with a <code>clone</code> of the content of the workspace
     * <code>srcWorkspace</code>. Semantically, this method is equivalent to
     * creating a new workspace and manually cloning <code>srcWorkspace</code>
     * to it; however, this method may assist some implementations in optimizing
     * subsequent <code>Node.update</code> and <code>Node.merge</code> calls
     * between the new workspace and its source.
     * <p/>
     * The new workspace can be accessed through a <code>login</code>
     * specifying its name.
     * <p/>
     * Throws an <code>AccessDeniedException</code> if the session through which
     * this <code>Workspace</code> object was acquired does not have permission
     * to create the new workspace.
     * <p/>
     * Throws an <code>UnsupportedRepositoryOperationException</code> if the repository does
     * not support the creation of workspaces.
     * <p/>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param name A <code>String</code>, the name of the new workspace.
     * @param srcWorkspace The name of the workspace from which the new workspace is to be cloned.
     * @throws AccessDeniedException if the session through which
     * this <code>Workspace</code> object was acquired does not have permission
     * to create the new workspace.
     * @throws UnsupportedRepositoryOperationException if the repository does
     * not support the creation of workspaces.
     * @throws RepositoryException if another error occurs.
     * @since JCR 2.0
     */
    public void createWorkspace(String name, String srcWorkspace)
            throws AccessDeniedException, UnsupportedRepositoryOperationException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        session.createWorkspace(name);

        SessionImpl tmpSession = null;
        try {
            // create a temporary session on new workspace for current subject
            tmpSession = rep.createSession(session.getSubject(), name);
            WorkspaceImpl newWsp = (WorkspaceImpl) tmpSession.getWorkspace();

            newWsp.clone(srcWorkspace, "/", "/", false);
        } finally {
            if (tmpSession != null) {
                // we don't need the temporary session anymore, logout
                tmpSession.logout();
            }
        }
    }

    /**
     * Deletes the workspace with the specified <code>name</code> from the
     * repository, deleting all content within it.
     * <p/>
     * Throws an <code>AccessDeniedException</code> if the session through which
     * this <code>Workspace</code> object was acquired does not have permission
     * to remove the workspace.
     * <p/>
     * Throws an <code>UnsupportedRepositoryOperationException</code> if the
     * repository does not support the removal of workspaces.
     *
     * @param name A <code>String</code>, the name of the workspace to be deleted.
     * @throws AccessDeniedException if the session through which
     * this <code>Workspace</code> object was acquired does not have permission
     * to remove the workspace.
     * @throws UnsupportedRepositoryOperationException if the
     * repository does not support the removal of workspaces.
     * @throws RepositoryException if another error occurs.
     * @since JCR 2.0
     */
    public void deleteWorkspace(String name) throws AccessDeniedException,
            UnsupportedRepositoryOperationException, RepositoryException {
        // check if workspace exists (will throw NoSuchWorkspaceException if not)
        rep.getWorkspaceInfo(name);
        // todo implement deleteWorkspace
        throw new UnsupportedRepositoryOperationException("not yet implemented");
    }

    //-------------------------------< JackrabbitWorkspace/new JSR 283 method >
    /**
     * Creates a new <code>Workspace</code> with the specified
     * <code>name</code>. The new workspace is empty, meaning it contains only
     * root node.
     * <p/>
     * The new workspace can be accessed through a <code>login</code>
     * specifying its name.
     * <p/>
     * Throws an <code>AccessDeniedException</code> if the session through which
     * this <code>Workspace</code> object was acquired does not have permission
     * to create the new workspace.
     * <p/>
     * Throws an <code>UnsupportedRepositoryOperationException</code> if the repository does
     * not support the creation of workspaces.
     * <p/>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param name A <code>String</code>, the name of the new workspace.
     * @throws AccessDeniedException if the session through which
     * this <code>Workspace</code> object was acquired does not have permission
     * to create the new workspace.
     * @throws UnsupportedRepositoryOperationException if the repository does
     * not support the creation of workspaces.
     * @throws RepositoryException if another error occurs.
     * @since JCR 2.0
     */
    public void createWorkspace(String name)
            throws AccessDeniedException,
            UnsupportedRepositoryOperationException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        session.createWorkspace(name);
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
    public void createWorkspace(String workspaceName, InputSource configTemplate)
            throws AccessDeniedException, RepositoryException {
        // check state of this instance
        sanityCheck();

        session.createWorkspace(workspaceName, configTemplate);
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
            srcPath = session.getQPath(srcAbsPath).getNormalizedPath();
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
            destPath = session.getQPath(destAbsPath).getNormalizedPath();
        } catch (NameException e) {
            String msg = "invalid path: " + destAbsPath;
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
        if (!destPath.isAbsolute()) {
            throw new RepositoryException("not an absolute path: " + destAbsPath);
        }

        BatchedItemOperations ops = new BatchedItemOperations(
                stateMgr, rep.getNodeTypeRegistry(), session.getLockManager(),
                session, hierMgr);

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
                    ((SessionImpl) srcWsp.getSession()).getAccessManager(),
                    destPath, flag);
            ops.update();
            succeeded = true;
            return session.getJCRPath(hierMgr.getPath(id));
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
    public synchronized LockManager getLockManager() throws RepositoryException {

        // check state of this instance
        sanityCheck();

        if (lockMgr == null) {
            lockMgr = rep.getLockManager(wspConfig.getName());
        }
        return lockMgr;
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
        // check state of this instance
        sanityCheck();

        return rep.getNamespaceRegistry();
    }

    /**
     * {@inheritDoc}
     */
    public NodeTypeManager getNodeTypeManager() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        return session.getNodeTypeManager();
    }

    /**
     * {@inheritDoc}
     */
    public void clone(String srcWorkspace, String srcAbsPath,
                      String destAbsPath, boolean removeExisting)
            throws NoSuchWorkspaceException, ConstraintViolationException,
            VersionException, AccessDeniedException, PathNotFoundException,
            ItemExistsException, LockException, RepositoryException {
        clone283(srcWorkspace, srcAbsPath, destAbsPath, removeExisting);
    }

    /**
     * {@inheritDoc}
     */
    public void copy(String srcAbsPath, String destAbsPath)
            throws ConstraintViolationException, VersionException,
            AccessDeniedException, PathNotFoundException, ItemExistsException,
            LockException, RepositoryException {
        copy283(srcAbsPath, destAbsPath);
    }

    /**
     * {@inheritDoc}
     */
    public void copy(String srcWorkspace, String srcAbsPath, String destAbsPath)
            throws NoSuchWorkspaceException, ConstraintViolationException,
            VersionException, AccessDeniedException, PathNotFoundException,
            ItemExistsException, LockException, RepositoryException {
        copy283(srcWorkspace, srcAbsPath, destAbsPath);
    }

    /**
     * {@inheritDoc}
     */
    public void move(String srcAbsPath, String destAbsPath)
            throws ConstraintViolationException, VersionException,
            AccessDeniedException, PathNotFoundException, ItemExistsException,
            LockException, RepositoryException {
        move283(srcAbsPath, destAbsPath);
    }

    /**
     * Returns the observation manager of this session. The observation manager
     * is lazily created if it does not exist yet.
     *
     * @return the observation manager of this session
     * @throws RepositoryException if a repository error occurs
     */
    public ObservationManager getObservationManager()
            throws RepositoryException {
        // check state of this instance
        sanityCheck();

        if (obsMgr == null) {
            try {
                obsMgr = new ObservationManagerImpl(
                        rep.getObservationDispatcher(wspConfig.getName()),
                        session, session.getItemManager());
            } catch (NoSuchWorkspaceException nswe) {
                // should never get here
                String msg = "internal error: failed to instantiate observation manager";
                log.debug(msg);
                throw new RepositoryException(msg, nswe);
            }
        }
        return obsMgr;
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
                searchManager = rep.getSearchManager(wspConfig.getName());
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
            queryManager = new QueryManagerImpl(session, session.getItemManager(), searchManager);
        }
        return queryManager;
    }

    /**
     * {@inheritDoc}
     */
    public void restore(Version[] versions, boolean removeExisting)
            throws ItemExistsException, UnsupportedRepositoryOperationException,
            VersionException, LockException, InvalidItemStateException,
            RepositoryException {

        // todo: perform restore operations direct on the node states

        // check state of this instance
        sanityCheck();

        // add all versions to map of versions to restore
        final HashMap toRestore = new HashMap();
        for (int i = 0; i < versions.length; i++) {
            VersionImpl v = (VersionImpl) versions[i];
            VersionHistory vh = v.getContainingHistory();
            // check for collision
            if (toRestore.containsKey(vh.getUUID())) {
                throw new VersionException("Unable to restore. Two or more versions have same version history.");
            }
            toRestore.put(vh.getUUID(), v);
        }

        // create a version selector to the set of versions
        VersionSelector vsel = new VersionSelector() {
            public Version select(VersionHistory versionHistory) throws RepositoryException {
                // try to select version as specified
                Version v = (Version) toRestore.get(versionHistory.getUUID());
                if (v == null) {
                    // select latest one
                    v = DateVersionSelector.selectByDate(versionHistory, null);
                }
                return v;
            }
        };

        // check for pending changes
        if (session.hasPendingChanges()) {
            String msg = "Unable to restore version. Session has pending changes.";
            log.debug(msg);
            throw new InvalidItemStateException(msg);
        }

        try {
            // now restore all versions that have a node in the ws
            int numRestored = 0;
            while (toRestore.size() > 0) {
                Version[] restored = null;
                Iterator iter = toRestore.values().iterator();
                while (iter.hasNext()) {
                    VersionImpl v = (VersionImpl) iter.next();
                    try {
                        NodeImpl node = (NodeImpl) session.getNodeByUUID(v.getFrozenNode().getFrozenUUID());
                        restored = node.internalRestore(v, vsel, removeExisting);
                        // remove restored versions from set
                        for (int i = 0; i < restored.length; i++) {
                            toRestore.remove(restored[i].getContainingHistory().getUUID());
                        }
                        numRestored += restored.length;
                        break;
                    } catch (ItemNotFoundException e) {
                        // ignore
                    }
                }
                if (restored == null) {
                    if (numRestored == 0) {
                        throw new VersionException("Unable to restore. At least one version needs"
                                + " existing versionable node in workspace.");
                    } else {
                        throw new VersionException("Unable to restore. All versions with non"
                                + " existing versionable nodes need parent.");
                    }
                }
            }
        } catch (RepositoryException e) {
            // revert session
            try {
                log.error("reverting changes applied during restore...");
                session.refresh(false);
            } catch (RepositoryException e1) {
                // ignore this
            }
            throw e;
        }
        session.save();
    }

    /**
     * {@inheritDoc}
     */
    public String[] getAccessibleWorkspaceNames() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        return session.getWorkspaceNames();
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
            parentPath = session.getQPath(parentAbsPath).getNormalizedPath();
        } catch (NameException e) {
            String msg = "invalid path: " + parentAbsPath;
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
        if (!parentPath.isAbsolute()) {
            throw new RepositoryException("not an absolute path: " + parentAbsPath);
        }

        Importer importer = new WorkspaceImporter(parentPath, this,
                rep.getNodeTypeRegistry(), uuidBehavior);
        return new ImportHandler(importer, session.getNamespaceResolver(),
                rep.getNamespaceRegistry());
    }

    /**
     * Create the persistent item state manager on top of the shared item
     * state manager. May be overridden by subclasses.
     * @param shared shared item state manager
     * @return local item state manager
     */
    protected LocalItemStateManager createItemStateManager(SharedItemStateManager shared) {
        return new LocalItemStateManager(shared, this, rep.getItemStateCacheFactory());
    }

    //---------------------------------< Workspace methods changed in JSR 283 >
    /**
     * This method copies the node at <code>srcAbsPath</code> to the new
     * location at <code>destAbsPath</code>. Returns the path of the node at its
     * new position. Note that the returned path will indicate the resulting
     * same-name sibling index of the destination, if necessary, unlike the
     * supplied <code>destAbsPath</code> parameter (see below).
     * <p/>
     * This operation is performed entirely within the persistent workspace, it
     * does not involve transient storage and therefore does not require a
     * <code>save</code>.
     * <p/>
     * The new copies of nodes are automatically given new identifiers and
     * referenceable nodes in particular are always given new referenceable
     * identifiers.
     * <p/>
     * When the source subtree in a <code>copy</code> operation includes both a reference
     * property (<code>P</code>) and the node to which it refers (<code>N</code>)
     * then not only does the new copy of the referenceable node (<code>N'<code>)
     * get a new identifier but the new copy of the reference property (<code>P'</code>)
     * is changed so that it points to <code>N'</code>, thus preserving the
     * reference within the subtree.
     * <p/>
     * The <code>destAbsPath</code> provided must not have an index on its final
     * element. If it does then a <code>RepositoryException</code> is thrown.
     * Strictly speaking, the <code>destAbsPath</code> parameter is actually an
     * <i>absolute path</i> to the parent node of the new location, appended
     * with the new <i>name</i> desired for the copied node. It does not specify
     * a position within the child node ordering. If ordering is supported by
     * the node type of the parent node of the new location, then the new copy
     * of the node is appended to the end of the child node list. The resulting
     * position within a same-name sibling set can, however, be determined from
     * the path returned by this method, which will include an index if one is
     * required.
     * <p/>
     * This method cannot be used to copy just an individual property by itself.
     * It copies an entire node and its subtree (including, of course, any properties contained therein).
     * <p/>
     * A <code>ConstraintViolationException</code> is thrown if the operation would violate a node-type
     * or other implementation-specific constraint.
     * <p/>
     * A <code>VersionException</code> is thrown if the parent node of <code>destAbsPath</code> is
     * versionable and checked-in, or is non-versionable but its nearest versionable ancestor is
     * checked-in.
     * <p/>
     * An <code>AccessDeniedException</code> is thrown if the current session (i.e. the session that
     * was used to acquire this <code>Workspace</code> object) does not have sufficient access rights
     * to complete the operation.
     * <p/>
     * A <code>PathNotFoundException</code> is thrown if the node at <code>srcAbsPath</code> or the
     * parent of <code>destAbsPath</code> does not exist.
     * <p/>
     * An <code>ItemExistException</code> is thrown if a node already exists at
     * <code>destAbsPath</code> and same-name siblings are not allowed. Note that
     * if a property already exists at <code>destAbsPath</code>, the operation
     * succeeds, since a node may have a child node and property with the same name.
     * <p/>
     * A <code>LockException</code> is thrown if a lock prevents the copy.
     *
     * @param srcAbsPath the path of the node to be copied.
     * @param destAbsPath the location to which the node at <code>srcAbsPath</code>
     * is to be copied.
     * @return the path of the node at its new position.
     * @throws ConstraintViolationException if the operation would violate a
     * node-type or other implementation-specific constraint.
     * @throws VersionException if the parent node of <code>destAbsPath</code> is
     * versionable and checked-in, or is non-versionable but its nearest versionable ancestor is
     * checked-in.
     * @throws AccessDeniedException if the current session does not have
     * sufficient access rights to complete the operation.
     * @throws PathNotFoundException if the node at <code>srcAbsPath</code> or
     * the parent of <code>destAbsPath</code> does not exist.
     * @throws ItemExistsException if a node already exists at
     * <code>destAbsPath</code> and same-name siblings are not allowed.
     * @throws LockException if a lock prevents the copy.
     * @throws RepositoryException if the last element of <code>destAbsPath</code>
     * has an index or if another error occurs.
     */
    public String copy283(String srcAbsPath, String destAbsPath)
            throws ConstraintViolationException, VersionException,
            AccessDeniedException, PathNotFoundException, ItemExistsException,
            LockException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // do intra-workspace copy
        return internalCopy(srcAbsPath, this, destAbsPath, BatchedItemOperations.COPY);
    }

    /**
     * This method copies the subtree at <code>srcAbsPath</code> in <code>srcWorkspace</code>
     * to <code>destAbsPath</code> in <code>this</code> workspace. Returns the
     * path of the node at its new position. Note that the returned path will
     * indicate the resulting same-name sibling index of the destination,
     * if necessary, unlike the supplied <code>destAbsPath</code> parameter
     * (see below).
     * <p/>
     * Unlike <code>clone</code>, this method does assign new referenceable
     * identifiers to the new copies of referenceable nodes. In the case of
     * non-referenceable nodes, this method <i>may</i> assign new identifiers. This
     * operation is performed entirely within the persistent workspace, it does
     * not involve transient storage and therefore does not require a <code>save</code>.
     * <p/>
     * When the source subtree in a <code>copy</code> operation includes both a reference
     * property (<code>P</code>) and the node to which it refers (<code>N</code>)
     * then not only does the new copy of the referenceable node (<code>N'<code>)
     * get a new identifier but the new copy of the reference property (<code>P'</code>)
     * is changed so that it points to <code>N'</code>, thus preserving the
     * reference within the subtree.
     * <p/>
     * The <code>destAbsPath</code> provided must not have an index on its final
     * element. If it does then a <code>RepositoryException</code> is thrown.
     * Strictly speaking, the <code>destAbsPath</code> parameter is actually an
     * <i>absolute path</i> to the parent node of the new location, appended
     * with the new <i>name</i> desired for the copied node. It does not specify
     * a position within the child node ordering. If ordering is supported by
     * the node type of the parent node of the new location, then the new copy
     * of the node is appended to the end of the child node list. The resulting
     * position within a same-name sibling set can, however, be determined from
     * the path returned by this method, which will include an index if one is
     * required.
     * <p/>
     * This method cannot be used to copy just an individual property by itself.
     * It copies an entire node and its subtree (including, of course, any properties contained therein).
     * <p/>
     * A <code>NoSuchWorkspaceException</code> is thrown if <code>srcWorkspace</code> does not
     * exist or if the current Session does not have permission to access it.
     * <p/>
     * A <code>ConstraintViolationException</code> is thrown if the operation would violate a node-type
     * or other implementation-specific constraint.
     * <p/>
     * A <code>VersionException</code> is thrown if the parent node of <code>destAbsPath</code> is
     * versionable and checked-in, or is non-versionable but its nearest versionable ancestor is
     * checked-in.
     * <p/>
     * An <code>AccessDeniedException</code> is thrown if the current session (i.e. the session that
     * was used to acquire this <code>Workspace</code> object) does not have sufficient access rights
     * to complete the operation.
     * <p/>
     * A <code>PathNotFoundException</code> is thrown if the node at <code>srcAbsPath</code> in
     * <code>srcWorkspace</code> or the parent of <code>destAbsPath</code> in this workspace does not exist.
     * <p/>
     * An <code>ItemExistException</code> is thrown if a node already exists at
     * <code>destAbsPath</code> and same-name siblings are not allowed. Note that
     * if a property already exists at <code>destAbsPath</code>, the operation
     * succeeds, since a node may have a child node and property with the same name.
     * <p/>
     * A <code>LockException</code> is thrown if a lock prevents the copy.
     *
     * @param srcWorkspace the name of the workspace from which the copy is to be made.
     * @param srcAbsPath the path of the node to be copied.
     * @param destAbsPath the location to which the node at <code>srcAbsPath</code>
     * is to be copied in <code>this</code> workspace.
     * @return the path of the node at its new position.
     * @throws NoSuchWorkspaceException if <code>srcWorkspace</code> does not
     * exist or if the current <code>Session</code> does not have permission to access it.
     * @throws ConstraintViolationException if the operation would violate a
     * node-type or other implementation-specific constraint
     * @throws VersionException if the parent node of <code>destAbsPath</code> is
     * versionable and checked-in, or is non-versionable but its nearest versionable ancestor is
     * checked-in.
     * @throws AccessDeniedException if the current session does have permission to access
     * <code>srcWorkspace</code> but otherwise does not have sufficient access rights to
     * complete the operation.
     * @throws PathNotFoundException if the node at <code>srcAbsPath</code> in <code>srcWorkspace</code> or
     * the parent of <code>destAbsPath</code> in this workspace does not exist.
     * @throws ItemExistsException if a node already exists at <code>destAbsPath</code>
     * and same-name siblings are not allowed.
     * @throws LockException if a lock prevents the copy.
     * @throws RepositoryException if the last element of <code>destAbsPath</code>
     * has an index or if another error occurs.
     */
    public String copy283(String srcWorkspace, String srcAbsPath, String destAbsPath)
            throws NoSuchWorkspaceException, ConstraintViolationException,
            VersionException, AccessDeniedException, PathNotFoundException,
            ItemExistsException, LockException, RepositoryException {

        // check state of this instance
        sanityCheck();

        // check workspace name
        if (getName().equals(srcWorkspace)) {
            // same as current workspace, delegate to intra-workspace copy method
            return copy283(srcAbsPath, destAbsPath);
        }

        // check authorization for specified workspace
        if (!session.getAccessManager().canAccess(srcWorkspace)) {
            throw new AccessDeniedException("not authorized to access " + srcWorkspace);
        }

        // copy (i.e. pull) subtree at srcAbsPath from srcWorkspace
        // to 'this' workspace at destAbsPath

        SessionImpl srcSession = null;
        try {
            // create session on other workspace for current subject
            // (may throw NoSuchWorkspaceException and AccessDeniedException)
            srcSession = rep.createSession(session.getSubject(), srcWorkspace);
            WorkspaceImpl srcWsp = (WorkspaceImpl) srcSession.getWorkspace();

            // do cross-workspace copy
            return internalCopy(srcAbsPath, srcWsp, destAbsPath, BatchedItemOperations.COPY);
        } finally {
            if (srcSession != null) {
                // we don't need the other session anymore, logout
                srcSession.logout();
            }
        }
    }

    /**
     * Clones the subtree at the node <code>srcAbsPath</code> in <code>srcWorkspace</code> to the new location at
     * <code>destAbsPath</code> in <code>this</code> workspace. Returns the path
     * of the node at its new position. Note that the returned
     * path will indicate the resulting same-name sibling index of the
     * destination, if necessary, unlike the supplied <code>destAbsPath</code>
     * parameter (see below).
     * <p/>
     * Unlike the signature of <code>copy</code> that copies between workspaces,
     * this method <i>does not</i> assign new identifiers to the newly cloned nodes
     * but preserves the identifiers of their respective source nodes. This applies
     * to both referenceable and non-referenceable nodes.
     * <p/>
     * In some implementations there may be cases where preservation of a
     * non-referenceable identifier is not possible, due to how non-referenceable
     * identifiers are constructed in that implementation. In such a case this
     * method will throw a <code>RepositoryException</code>.
     * <p/>
     * If <code>removeExisting</code> is true and an existing node in this workspace
     * (the destination workspace) has the same identifier as a node being cloned from
     * <code>srcWorkspace</code>, then the incoming node takes precedence, and the
     * existing node (and its subtree) is removed. If <code>removeExisting</code>
     * is false then an identifier collision causes this method to throw a
     * <code>ItemExistsException</code> and no changes are made.
     * <p/>
     * If successful, the change is persisted immediately, there is no need to call <code>save</code>.
     * <p/>
     * The <code>destAbsPath</code> provided must not have an index on its final
     * element. If it does then a <code>RepositoryException</code> is thrown.
     * Strictly speaking, the <code>destAbsPath</code> parameter is actually an
     * <i>absolute path</i> to the parent node of the new location, appended
     * with the new <i>name</i> desired for the cloned node. It does not specify
     * a position within the child node ordering. If ordering is supported by
     * the node type of the parent node of the new location, then the new clone
     * of the node is appended to the end of the child node list. The resulting
     * position within a same-name sibling set can, however, be determined from
     * the path returned by this method, which will include an index, if one is
     * required.
     * <p/>
     * This method cannot be used to clone just an individual property by itself. It clones an
     * entire node and its subtree (including, of course, any properties contained therein).
     * <p/>
     * A <code>NoSuchWorkspaceException</code> is thrown if <code>srcWorkspace</code> does not
     * exist or if the current <code>Session</code> does not have permission to access it.
     * <p/>
     * A <code>ConstraintViolationException</code> is thrown if the operation would violate a node-type
     * or other implementation-specific constraint or if <code>srcWorkspace</code> is the name of this workspace.
     * In other words, if an attempt is made to clone a subtree into the same workspace.
     * <p/>
     * A <code>VersionException</code> is thrown if the parent node of <code>destAbsPath</code> is
     * versionable and checked-in, or is non-versionable but its nearest versionable ancestor is
     * checked-in. This exception will also be thrown if <code>removeExisting</code> is <code>true</code>,
     * and an identifier conflict occurs that would require the moving and/or altering of a node that is checked-in.
     * <p/>
     * An <code>AccessDeniedException</code> is thrown if the current session (i.e. the session that
     * was used to acquire this <code>Workspace</code> object) does not have sufficient access rights
     * to complete the operation.
     * <p/>
     * A <code>PathNotFoundException</code> is thrown if the node at <code>srcAbsPath</code> in
     * <code>srcWorkspace</code> or the parent of <code>destAbsPath</code> in this workspace does not exist.
     * <p/>
     * An <code>ItemExistsException</code> is thrown if a node or property already exists at
     * <code>destAbsPath</code>
     * <p/>
     * An <code>ItemExistException</code> is thrown if a node already exists at
     * <code>destAbsPath</code> and same-name siblings are not allowed or if
     * <code>removeExisting</code> is <code>false</code> and an identifier conflict occurs.
     * <p/>
     * Note that if a property already exists at <code>destAbsPath</code>, the
     * operation succeeds, since a node may have a child node and property with
     * the same name.
     * <p/>
     * A <code>LockException</code> is thrown if a lock prevents the clone.
     *
     * @param srcWorkspace The name of the workspace from which the node is to be copied.
     * @param srcAbsPath the path of the node to be copied in <code>srcWorkspace</code>.
     * @param destAbsPath the location to which the node at <code>srcAbsPath</code>
     * is to be copied in <code>this</code> workspace.
     * @param removeExisting if <code>false</code> then this method throws an
     * <code>ItemExistsException</code> on identifier conflict with an incoming node.
     * If <code>true</code> then a identifier conflict is resolved by removing the existing node
     * from its location in this workspace and cloning (copying in) the one from
     * <code>srcWorkspace</code>.
     * @return the path of the node at its new position.
     * @throws NoSuchWorkspaceException if <code>destWorkspace</code> does not exist.
     * @throws ConstraintViolationException if the operation would violate a
     * node-type or other implementation-specific constraint.
     * @throws VersionException if the parent node of <code>destAbsPath</code> is
     * versionable and checked-in, or is non-versionable but its nearest versionable ancestor is
     * checked-in. This exception will also be thrown if <code>removeExisting</code> is <code>true</code>,
     * and an identifier conflict occurs that would require the moving and/or altering of a node that is checked-in.
     * @throws AccessDeniedException if the current session does not have
     * sufficient access rights to complete the operation.
     * @throws PathNotFoundException if the node at <code>srcAbsPath</code> in
     * <code>srcWorkspace</code> or the parent of <code>destAbsPath</code> in this workspace does not exist.
     * @throws ItemExistsException if a node already exists at
     * <code>destAbsPath</code> and same-name siblings are not allowed or if
     * <code>removeExisting</code> is <code>false</code> and an identifier conflict occurs.
     * @throws LockException if a lock prevents the clone.
     * @throws RepositoryException if the last element of <code>destAbsPath</code>
     * has an index or if another error occurs.
     */
    public String clone283(String srcWorkspace, String srcAbsPath,
                           String destAbsPath, boolean removeExisting)
            throws NoSuchWorkspaceException, ConstraintViolationException,
            VersionException, AccessDeniedException, PathNotFoundException,
            ItemExistsException, LockException, RepositoryException {

        // check state of this instance
        sanityCheck();

        // check workspace name
        if (getName().equals(srcWorkspace)) {
            // same as current workspace
            String msg = srcWorkspace + ": illegal workspace (same as current)";
            log.debug(msg);
            throw new RepositoryException(msg);
        }

        // check authorization for specified workspace
        if (!session.getAccessManager().canAccess(srcWorkspace)) {
            throw new AccessDeniedException("not authorized to access " + srcWorkspace);
        }

        // clone (i.e. pull) subtree at srcAbsPath from srcWorkspace
        // to 'this' workspace at destAbsPath

        SessionImpl srcSession = null;
        try {
            // create session on other workspace for current subject
            // (may throw NoSuchWorkspaceException and AccessDeniedException)
            srcSession = rep.createSession(session.getSubject(), srcWorkspace);
            WorkspaceImpl srcWsp = (WorkspaceImpl) srcSession.getWorkspace();

            // do cross-workspace copy
            int mode = BatchedItemOperations.CLONE;
            if (removeExisting) {
                mode = BatchedItemOperations.CLONE_REMOVE_EXISTING;
            }
            return internalCopy(srcAbsPath, srcWsp, destAbsPath, mode);
        } finally {
            if (srcSession != null) {
                // we don't need the other session anymore, logout
                srcSession.logout();
            }
        }
    }

    /**
     * Moves the node at <code>srcAbsPath</code> (and its entire subtree) to the
     * new location at <code>destAbsPath</code>. Returns the path of the node at
     * its new position. Note that the returned path will indicate the resulting
     * same-name sibling index of the destination, if necessary, unlike the
     * supplied <code>destAbsPath</code> parameter (see below).
     * <p/>
     * If successful,
     * the change is persisted immediately, there is no need to call <code>save</code>.
     * Note that this is in contrast to {@link Session#move} which operates within the
     * transient space and hence requires a <code>save</code>.
     * <p/>
     * The identifiers of referenceable nodes must not be changed by a <code>move</code>.
     * The identifiers of non-referenceable nodes <i>may</i> change.
     * <p/>
     * The <code>destAbsPath</code> provided must not
     * have an index on its final element. If it does then a <code>RepositoryException</code>
     * is thrown. Strictly speaking, the <code>destAbsPath</code> parameter is actually an <i>absolute path</i>
     * to the parent node of the new location, appended with the new <i>name</i> desired for the
     * moved node. It does not specify a position within the child node
     * ordering. If ordering is supported by the node type of
     * the parent node of the new location, then the newly moved node is appended to the end of the
     * child node list. The resulting position within a same-name sibling set can,
     * however, be determined from the path returned by this method,
     * which will include an index if one is required.
     * <p/>
     * This method cannot be used to move just an individual property by itself.
     * It moves an entire node and its subtree (including, of course, any properties contained therein).
     * <p/>
     * The identifiers of referenceable nodes must not be changed by a <code>move</code>.
     * The identifiers of non-referenceable nodes may change.
     * <p/>
     * A <code>ConstraintViolationException</code> is thrown if the operation would violate a node-type
     * or other implementation-specific constraint.
     * <p/>
     * A <code>VersionException</code> is thrown if the parent node of <code>destAbsPath</code>
     * or the parent node of <code>srcAbsPath</code> is versionable and checked-in, or is
     * non-versionable but its nearest versionable ancestor is checked-in.
     * <p/>
     * An <code>AccessDeniedException</code> is thrown if the current session (i.e. the session that
     * was used to acquire this <code>Workspace</code> object) does not have sufficient access rights
     * to complete the operation.
     * <p/>
     * A <code>PathNotFoundException</code> is thrown if the node at <code>srcAbsPath</code> or the
     * parent of <code>destAbsPath</code> does not exist.
     * <p/>
     * An <code>ItemExistException</code> is thrown if a node already exists at
     * <code>destAbsPath</code> and same-name siblings are not allowed.
     * <p/>
     * Note that if a property already exists at <code>destAbsPath</code>, the
     * operation succeeds, since a node may have a child node and property with
     * the same name
     * <p/>
     * A <code>LockException</code> if a lock prevents the move.
     *
     * @param srcAbsPath the path of the node to be moved.
     * @param destAbsPath the location to which the node at <code>srcAbsPath</code>
     * is to be moved.
     * @return the path of the node at its new position.
     * @throws ConstraintViolationException if the operation would violate a
     * node-type or other implementation-specific constraint
     * @throws VersionException if the parent node of <code>destAbsPath</code>
     * or the parent node of <code>srcAbsPath</code> is versionable and checked-in,
     * or is non-versionable but its nearest versionable ancestor is checked-in.
     * @throws AccessDeniedException if the current session (i.e. the session that
     * was used to aqcuire this <code>Workspace</code> object) does not have
     * sufficient access rights to complete the operation.
     * @throws PathNotFoundException if the node at <code>srcAbsPath</code> or
     * the parent of <code>destAbsPath</code> does not exist.
     * @throws ItemExistsException if a node already exists at
     * <code>destAbsPath</code> and same-name siblings are not allowed.
     * @throws LockException if a lock prevents the move.
     * @throws RepositoryException if the last element of <code>destAbsPath</code>
     *         has an index or if another error occurs.
     */
    public String move283(String srcAbsPath, String destAbsPath)
            throws ConstraintViolationException, VersionException,
            AccessDeniedException, PathNotFoundException, ItemExistsException,
            LockException, RepositoryException {

        // check state of this instance
        sanityCheck();

        // intra-workspace move...

        Path srcPath;
        try {
            srcPath = session.getQPath(srcAbsPath).getNormalizedPath();
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
            destPath = session.getQPath(destAbsPath).getNormalizedPath();
        } catch (NameException e) {
            String msg = "invalid path: " + destAbsPath;
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
        if (!destPath.isAbsolute()) {
            throw new RepositoryException("not an absolute path: " + destAbsPath);
        }

        BatchedItemOperations ops = new BatchedItemOperations(
                stateMgr, rep.getNodeTypeRegistry(), session.getLockManager(),
                session, hierMgr);

        try {
            ops.edit();
        } catch (IllegalStateException e) {
            String msg = "unable to start edit operation";
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }

        boolean succeeded = false;

        try {
            NodeId id = ops.move(srcPath, destPath);
            ops.update();
            succeeded = true;
            return session.getJCRPath(hierMgr.getPath(id));
        } finally {
            if (!succeeded) {
                // update operation failed, cancel all modifications
                ops.cancel();
            }
        }
    }

    //------------------------------------------< EventStateCollectionFactory >
    /**
     * {@inheritDoc}
     * <p/>
     * Implemented in this object and forwarded rather than {@link #obsMgr}
     * since creation of the latter is lazy.
     */
    public EventStateCollection createEventStateCollection()
            throws RepositoryException {

        return ((ObservationManagerImpl) getObservationManager()).createEventStateCollection();
    }
}


