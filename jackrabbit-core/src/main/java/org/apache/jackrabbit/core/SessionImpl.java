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

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.map.ReferenceMap;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.commons.AbstractSession;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.data.GarbageCollector;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.lock.LockManager;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.retention.RetentionManagerImpl;
import org.apache.jackrabbit.core.retention.RetentionRegistry;
import org.apache.jackrabbit.core.security.AMContext;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.core.security.authentication.AuthContext;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.core.session.SessionOperation;
import org.apache.jackrabbit.core.session.SessionRefreshOperation;
import org.apache.jackrabbit.core.session.SessionSaveOperation;
import org.apache.jackrabbit.core.state.SessionItemStateManager;
import org.apache.jackrabbit.core.util.Dumpable;
import org.apache.jackrabbit.core.value.ValueFactoryImpl;
import org.apache.jackrabbit.core.version.InternalVersionManager;
import org.apache.jackrabbit.core.xml.ImportHandler;
import org.apache.jackrabbit.core.xml.SessionImporter;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.IdentifierResolver;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.NamespaceException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import javax.jcr.retention.RetentionManager;
import javax.jcr.security.AccessControlManager;
import javax.jcr.version.VersionException;
import javax.security.auth.Subject;
import java.io.File;
import java.io.PrintStream;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A <code>SessionImpl</code> ...
 */
public class SessionImpl extends AbstractSession
        implements JackrabbitSession, NamespaceResolver, NamePathResolver, IdentifierResolver, Dumpable {

    /**
     * Name of the session attribute that controls whether the
     * {@link #refresh(boolean)} method will cause the repository to
     * synchronize itself to changes in other cluster nodes. This cluster
     * synchronization is enabled by default, unless an attribute with this
     * name is set (any non-null value) for this session.
     *
     * @since Apache Jackrabbit 1.6
     * @see <a href="https://issues.apache.org/jira/browse/JCR-1753">JCR-1753</a>
     */
    public static final String DISABLE_CLUSTER_SYNC_ON_REFRESH =
        "org.apache.jackrabbit.disableClusterSyncOnRefresh";

    private static Logger log = LoggerFactory.getLogger(SessionImpl.class);

    /**
     * The component context of this session.
     */
    protected final SessionContext context;

    /**
     * The component context of the repository that issued this session.
     */
    protected final RepositoryContext repositoryContext;

    /**
     * the AuthContext of this session (can be null if this
     * session was not instantiated through a login process)
     */
    protected AuthContext loginContext;

    /**
     * the Subject of this session
     */
    protected final Subject subject;

    /**
     * the user ID that was used to acquire this session
     */
    protected final String userId;

    /**
     * the attributes of this session
     */
    protected final Map<String, Object> attributes =
        new HashMap<String, Object>();

    /**
     * the node type manager
     */
    protected final NodeTypeManagerImpl ntMgr;

    /**
     * the Workspace associated with this session
     */
    protected final WorkspaceImpl wsp;

    /**
     * Name and Path resolver
     */
    protected NamePathResolver namePathResolver;

    /**
     * The version manager for this session
     */
    protected final InternalVersionManager versionMgr;

    /**
     * node type instance handler
     */
    protected final NodeTypeInstanceHandler ntInstanceHandler;

    /**
     * Listeners (weak references)
     */
    protected final Map<SessionListener, SessionListener> listeners =
        new ReferenceMap(ReferenceMap.WEAK, ReferenceMap.WEAK);

    /**
     * value factory
     */
    protected ValueFactory valueFactory;

    /**
     * Principal Manager
     */
    private PrincipalManager principalManager;

    /**
     * User Manager
     */
    private UserManager userManager;

    /**
     * Retention and Hold Manager
     */
    private RetentionManager retentionManager;

    /**
     * The stack trace knows who opened this session. It is logged
     * if the session is finalized, but Session.logout() was never called.
     */
    private Exception openStackTrace = new Exception("Stack Trace");

    /**
     * Protected constructor.
     *
     * @param repositoryContext repository context
     * @param loginContext
     * @param wspConfig
     * @throws AccessDeniedException if the subject of the given login context
     *                               is not granted access to the specified
     *                               workspace
     * @throws RepositoryException   if another error occurs
     */
    protected SessionImpl(
            RepositoryContext repositoryContext, AuthContext loginContext,
            WorkspaceConfig wspConfig)
            throws AccessDeniedException, RepositoryException {
        this(repositoryContext, loginContext.getSubject(), wspConfig);
        this.loginContext = loginContext;
    }

    /**
     * Protected constructor.
     *
     * @param repositoryContext repository context
     * @param subject
     * @param wspConfig
     * @throws AccessDeniedException if the given subject is not granted access
     *                               to the specified workspace
     * @throws RepositoryException   if another error occurs
     */
    protected SessionImpl(
            RepositoryContext repositoryContext, Subject subject,
            WorkspaceConfig wspConfig)
            throws AccessDeniedException, RepositoryException {
        this.context = new SessionContext(repositoryContext, this);
        this.repositoryContext = repositoryContext;
        this.subject = subject;

        userId = retrieveUserId(subject, wspConfig.getName());

        namePathResolver = new DefaultNamePathResolver(this, this, true);
        ntMgr = new NodeTypeManagerImpl(
                repositoryContext.getNodeTypeRegistry(), this,
                repositoryContext.getDataStore());
        wsp = createWorkspaceInstance(wspConfig);
        context.setItemStateManager(createSessionItemStateManager());
        context.setItemManager(createItemManager());
        context.setItemValidator(new ItemValidator(context));
        context.setAccessManager(createAccessManager(subject));
        versionMgr = createVersionManager();
        ntInstanceHandler = new NodeTypeInstanceHandler(userId);
    }

    /**
     * Retrieve the userID from the specified subject.
     *
     * @return the userID.
     */
    protected String retrieveUserId(Subject subject, String workspaceName) throws RepositoryException {
        return repositoryContext.getSecurityManager().getUserID(
                subject, workspaceName);
    }

    /**
     * Create the session item state manager.
     *
     * @return session item state manager
     */
    protected SessionItemStateManager createSessionItemStateManager() {
        SessionItemStateManager mgr = new SessionItemStateManager(
                context.getRootNodeId(),
                wsp.getItemStateManager(),
                repositoryContext.getNodeTypeRegistry());
        wsp.getItemStateManager().addListener(mgr);
        return mgr;
    }

    /**
     * Creates the workspace instance backing this session.
     *
     * @param wspConfig The workspace configuration
     * @return An instance of the {@link WorkspaceImpl} class or an extension
     *         thereof.
     * @throws RepositoryException if the workspace can not be accessed
     */
    protected WorkspaceImpl createWorkspaceInstance(WorkspaceConfig wspConfig)
            throws RepositoryException {
        return new WorkspaceImpl(wspConfig, context);
    }

    /**
     * Create the item manager.
     * @return item manager
     */
    protected ItemManager createItemManager() {
        ItemManager mgr =
            new ItemManager(context, ntMgr.getRootNodeDefinition());
        context.getItemStateManager().addListener(mgr);
        return mgr;
    }

    /**
     * Create the version manager. If we are not using XA, we may safely use
     * the repository version manager.
     * @return version manager
     */
    protected InternalVersionManager createVersionManager()
            throws RepositoryException {
        return repositoryContext.getInternalVersionManager();
    }

    /**
     * Create the access manager.
     *
     * @param subject
     * @return access manager
     * @throws AccessDeniedException if the current subject is not granted access
     *                               to the current workspace
     * @throws RepositoryException   if the access manager cannot be instantiated
     */
    protected AccessManager createAccessManager(Subject subject)
            throws AccessDeniedException, RepositoryException {
        String wspName = getWorkspace().getName();
        AMContext ctx = new AMContext(
                new File(repositoryContext.getRepository().getConfig().getHomeDir()),
                repositoryContext.getFileSystem(),
                this,
                getSubject(),
                context.getHierarchyManager(),
                this,
                wspName);
        return repositoryContext.getSecurityManager().getAccessManager(this, ctx);
    }

    private void perform(SessionOperation operation)
            throws RepositoryException {
        context.getSessionState().perform(operation);
    }

    /**
     * Performs a sanity check on this session.
     *
     * @throws RepositoryException if this session has been rendered invalid
     *                             for some reason (e.g. if this session has
     *                             been closed explicitly or if it has expired)
     */
    private void sanityCheck() throws RepositoryException {
        context.getSessionState().checkAlive();
    }

    /**
     * @return ItemValidator instance for this session.
     */
    public ItemValidator getValidator() {
        return context.getItemValidator();
    }

    /**
     * Returns the <code>Subject</code> associated with this session.
     *
     * @return the <code>Subject</code> associated with this session
     */
    public Subject getSubject() {
        return subject;
    }

    /**
      * Creates a new session with the same subject as this sessions but to a
      * different workspace. The returned session is a newly logged in session,
      * with the same subject but a different workspace. Even if the given
      * workspace is the same as this sessions one, the implementation must
      * return a new session object.
      *
      * @param workspaceName name of the workspace to acquire a session for.
      * @return A session to the requested workspace for the same authenticated
      *         subject.
      * @throws AccessDeniedException in case the current Subject is not allowed
      *         to access the requested Workspace
      * @throws NoSuchWorkspaceException If the named workspace does not exist.
      * @throws RepositoryException in any other exceptional state
      */
    public Session createSession(String workspaceName)
            throws AccessDeniedException, NoSuchWorkspaceException, RepositoryException {
        if (workspaceName == null) {
            workspaceName =
                repositoryContext.getWorkspaceManager().getDefaultWorkspaceName();
        }
        Subject old = getSubject();
        Subject newSubject = new Subject(old.isReadOnly(), old.getPrincipals(), old.getPublicCredentials(), old.getPrivateCredentials());
        return repositoryContext.getWorkspaceManager().createSession(
                newSubject, workspaceName);
    }

    /**
     * Returns the <code>AccessManager</code> associated with this session.
     *
     * @return the <code>AccessManager</code> associated with this session
     */
    public AccessManager getAccessManager() {
        return context.getAccessManager();
    }

    /**
     * Returns the <code>NodeTypeManager</code>.
     *
     * @return the <code>NodeTypeManager</code>
     */
    public NodeTypeManagerImpl getNodeTypeManager() {
        return ntMgr;
    }

    /**
     * Returns the <code>ItemManager</code> of this session.
     *
     * @return the <code>ItemManager</code>
     */
    public ItemManager getItemManager() {
        return context.getItemManager();
    }

    /**
     * Returns the <code>HierarchyManager</code> associated with this session.
     *
     * @return the <code>HierarchyManager</code> associated with this session
     */
    public HierarchyManager getHierarchyManager() {
        return context.getHierarchyManager();
    }

    /**
     * Returns the <code>InternalVersionManager</code> associated with this session.
     *
     * @return the <code>InternalVersionManager</code> associated with this session
     */
    public InternalVersionManager getInternalVersionManager() {
        return versionMgr;
    }


    /**
     * Returns the internal retention manager used for evaluation of effective
     * retention policies and holds.
     *
     * @return internal retention manager
     * @throws RepositoryException
     */
    protected RetentionRegistry getRetentionRegistry() throws RepositoryException {
        return wsp.getRetentionRegistry();
    }

    /**
     * Returns the node type instance handler for this session
     * @return the node type instance handler.
     */
    public NodeTypeInstanceHandler getNodeTypeInstanceHandler() {
        return ntInstanceHandler;
    }

    /**
     * Sets the named attribute. If the value is <code>null</code>, then
     * the named attribute is removed.
     *
     * @see <a href="https://issues.apache.org/jira/browse/JCR-1932">JCR-1932</a>
     * @param name attribute name
     * @param value attribute value
     * @since Apache Jackrabbit 1.6
     */
    protected void setAttribute(String name, Object value) {
        if (value != null) {
            attributes.put(name, value);
        } else {
            attributes.remove(name);
        }
    }

    /**
     * Retrieves the <code>Node</code> with the given id.
     *
     * @param id id of node to be retrieved
     * @return node with the given <code>NodeId</code>.
     * @throws ItemNotFoundException if no such node exists or if this
     * <code>Session</code> does not have permission to access the node.
     * @throws RepositoryException if another error occurs.
     */
    public NodeImpl getNodeById(NodeId id) throws ItemNotFoundException, RepositoryException {
        // check sanity of this session
        sanityCheck();

        try {
            return (NodeImpl) getItemManager().getItem(id);
        } catch (AccessDeniedException ade) {
            throw new ItemNotFoundException(id.toString());
        }
    }

    /**
     * Returns the names of all workspaces of this repository with respect of the
     * access rights of this session.
     *
     * @return the names of all accessible workspaces
     * @throws RepositoryException if an error occurs
     */
    protected String[] getWorkspaceNames() throws RepositoryException {
        // filter workspaces according to access rights
        List<String> names = new ArrayList<String>();
        for (String name : repositoryContext.getWorkspaceManager().getWorkspaceNames()) {
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
     * Creates a workspace with the given name.
     *
     * @param workspaceName name of the new workspace
     * @throws AccessDeniedException if the current session is not allowed to
     *                               create the workspace
     * @throws RepositoryException   if a workspace with the given name
     *                               already exists or if another error occurs
     */
    protected void createWorkspace(String workspaceName)
            throws AccessDeniedException, RepositoryException {
        // @todo verify that this session has the right privileges for this operation
        repositoryContext.getWorkspaceManager().createWorkspace(workspaceName);
    }

    /**
     * Creates a workspace with the given name and a workspace configuration
     * template.
     *
     * @param workspaceName  name of the new workspace
     * @param configTemplate the configuration template of the new workspace
     * @throws AccessDeniedException if the current session is not allowed to
     *                               create the workspace
     * @throws RepositoryException   if a workspace with the given name already
     *                               exists or if another error occurs
     */
    protected void createWorkspace(
            String workspaceName, InputSource configTemplate)
            throws AccessDeniedException, RepositoryException {
        // @todo verify that this session has the right privileges for this operation
        repositoryContext.getWorkspaceManager().createWorkspace(
                workspaceName, configTemplate);
    }

    /**
     * Notify the listeners that this session is about to be closed.
     */
    protected void notifyLoggingOut() {
        // copy listeners to array to avoid ConcurrentModificationException
        List<SessionListener> copy =
            new ArrayList<SessionListener>(listeners.values());
        for (SessionListener listener : copy) {
            if (listener != null) {
                listener.loggingOut(this);
            }
        }
    }

    /**
     * Notify the listeners that this session has been closed.
     */
    protected void notifyLoggedOut() {
        // copy listeners to array to avoid ConcurrentModificationException
        List<SessionListener> copy =
            new ArrayList<SessionListener>(listeners.values());
        for (SessionListener listener : copy) {
            if (listener != null) {
                listener.loggedOut(this);
            }
        }
    }

    /**
     * Add a <code>SessionListener</code>
     *
     * @param listener the new listener to be informed on modifications
     */
    public void addListener(SessionListener listener) {
        if (!listeners.containsKey(listener)) {
            listeners.put(listener, listener);
        }
    }

    /**
     * Remove a <code>SessionListener</code>
     *
     * @param listener an existing listener
     */
    public void removeListener(SessionListener listener) {
        listeners.remove(listener);
    }

    /**
     * Create a data store garbage collector for this repository.
     *
     * @throws RepositoryException
     */
    public GarbageCollector createDataStoreGarbageCollector() throws RepositoryException {
        final GarbageCollector gc =
            repositoryContext.getRepository().createDataStoreGarbageCollector();
        // Auto-close if the main session logs out
        addListener(new SessionListener() {
            public void loggedOut(SessionImpl session) {
            }
            public void loggingOut(SessionImpl session) {
                gc.close();
            }
        });
        return gc;
    }

    //---------------------------------------------------< NamespaceResolver >

    public String getPrefix(String uri) throws NamespaceException {
        try {
            return getNamespacePrefix(uri);
        } catch (NamespaceException e) {
            throw e;
        } catch (RepositoryException e) {
            throw new NamespaceException("Namespace not found: " + uri, e);
        }
    }

    public String getURI(String prefix) throws NamespaceException {
        try {
            return getNamespaceURI(prefix);
        } catch (NamespaceException e) {
            throw e;
        } catch (RepositoryException e) {
            throw new NamespaceException("Namespace not found: " + prefix, e);
        }
    }

    //--------------------------------------------------------< NameResolver >

    public String getJCRName(Name name) throws NamespaceException {
        return namePathResolver.getJCRName(name);
    }

    public Name getQName(String name) throws IllegalNameException, NamespaceException {
        return namePathResolver.getQName(name);
    }

    //--------------------------------------------------------< PathResolver >

    public String getJCRPath(Path path) throws NamespaceException {
        return namePathResolver.getJCRPath(path);
    }

    public Path getQPath(String path) throws MalformedPathException, IllegalNameException, NamespaceException {
        return namePathResolver.getQPath(path);
    }

    public Path getQPath(String path, boolean normalizeIdentifier) throws MalformedPathException, IllegalNameException, NamespaceException {
        return namePathResolver.getQPath(path, normalizeIdentifier);
    }

    //---------------------------------------------------< IdentifierResolver >
    /**
     * @see IdentifierResolver#getPath(String)
     */
    public Path getPath(String identifier) throws MalformedPathException {
        try {
            return context.getHierarchyManager().getPath(NodeId.valueOf(identifier));
        } catch (RepositoryException e) {
            throw new MalformedPathException("Identifier '" + identifier + "' cannot be resolved.");
        }
    }

    /**
     * @see IdentifierResolver#checkFormat(String)
     */
    public void checkFormat(String identifier) throws MalformedPathException {
        try {
            NodeId.valueOf(identifier);
        } catch (IllegalArgumentException e) {
            throw new MalformedPathException("Invalid identifier: " + identifier);
        }
    }

    //----------------------------------------------------< JackrabbitSession >
    /**
     * @see JackrabbitSession#getPrincipalManager()
     */
    public PrincipalManager getPrincipalManager() throws RepositoryException, AccessDeniedException {
        if (principalManager == null) {
            principalManager =
                repositoryContext.getSecurityManager().getPrincipalManager(this);
        }
        return principalManager;
    }

    /**
     * @see JackrabbitSession#getUserManager()
     */
    public UserManager getUserManager() throws AccessDeniedException, RepositoryException {
        if (userManager == null) {
            userManager =
                repositoryContext.getSecurityManager().getUserManager(this);
        }
        return userManager;
    }

    //--------------------------------------------------------------< Session >
    /**
     * {@inheritDoc}
     */
    public void checkPermission(String absPath, String actions)
            throws AccessControlException, RepositoryException {
        if (!hasPermission(absPath, actions)) {
            throw new AccessControlException(actions);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Workspace getWorkspace() {
        return getWorkspaceImpl();
    }

    WorkspaceImpl getWorkspaceImpl() {
        return wsp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Session impersonate(Credentials otherCredentials)
            throws LoginException, RepositoryException {
        // check sanity of this session
        sanityCheck();

        if (!(otherCredentials instanceof SimpleCredentials)) {
            String msg = "impersonate failed: incompatible credentials, SimpleCredentials expected";
            log.debug(msg);
            throw new RepositoryException(msg);
        }

        // set IMPERSONATOR_ATTRIBUTE attribute of given credentials
        // with subject of current session
        SimpleCredentials creds = (SimpleCredentials) otherCredentials;
        creds.setAttribute(SecurityConstants.IMPERSONATOR_ATTRIBUTE, subject);

        try {
            return getRepository().login(
                    otherCredentials, getWorkspace().getName());
        } catch (NoSuchWorkspaceException nswe) {
            // should never get here...
            String msg = "impersonate failed";
            log.error(msg, nswe);
            throw new RepositoryException(msg, nswe);
        } finally {
            // make sure IMPERSONATOR_ATTRIBUTE is removed
            creds.removeAttribute(SecurityConstants.IMPERSONATOR_ATTRIBUTE);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Node getRootNode() throws RepositoryException {
        // check sanity of this session
        sanityCheck();

        return getItemManager().getRootNode();
    }

    /**
     * {@inheritDoc}
     */
    public Node getNodeByUUID(String uuid) throws ItemNotFoundException, RepositoryException {
        try {
            NodeImpl node = getNodeById(new NodeId(uuid));
            if (node.isNodeType(NameConstants.MIX_REFERENCEABLE)) {
                return node;
            } else {
                // there is a node with that uuid but the node does not expose it
                throw new ItemNotFoundException(uuid);
            }
        } catch (IllegalArgumentException e) {
            // Assuming the exception is from UUID.fromString()
            throw new RepositoryException("Invalid UUID: " + uuid, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Item getItem(String absPath) throws PathNotFoundException, RepositoryException {
        // check sanity of this session
        sanityCheck();

        try {
            Path p = getQPath(absPath).getNormalizedPath();
            if (!p.isAbsolute()) {
                throw new RepositoryException("not an absolute path: " + absPath);
            }
            return getItemManager().getItem(p);
        } catch (AccessDeniedException ade) {
            throw new PathNotFoundException(absPath);
        } catch (NameException e) {
            String msg = "invalid path:" + absPath;
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean itemExists(String absPath) throws RepositoryException {
        // check sanity of this session
        sanityCheck();

        try {
            Path p = getQPath(absPath).getNormalizedPath();
            if (!p.isAbsolute()) {
                throw new RepositoryException("not an absolute path: " + absPath);
            }
            return getItemManager().itemExists(p);
        } catch (NameException e) {
            String msg = "invalid path:" + absPath;
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void save() throws RepositoryException {
        perform(new SessionSaveOperation(context));
    }

    /**
     * {@inheritDoc}
     */
    public void refresh(boolean keepChanges) throws RepositoryException {
        perform(new SessionRefreshOperation(
                context, keepChanges, clusterSyncOnRefresh()));
    }

    /**
     * Checks whether the {@link #refresh(boolean)} method should cause
     * cluster synchronization.
     * <p>
     * Subclasses can override this method to implement alternative
     * rules on when cluster synchronization should be done.
     *
     * @return <code>true</code> if the {@link #DISABLE_CLUSTER_SYNC_ON_REFRESH}
     *         attribute is <em>not</em> set, <code>false</code> otherwise
     * @since Apache Jackrabbit 1.6
     * @see <a href="https://issues.apache.org/jira/browse/JCR-1753">JCR-1753</a>
     */
    protected boolean clusterSyncOnRefresh() {
        return getAttribute(DISABLE_CLUSTER_SYNC_ON_REFRESH) == null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasPendingChanges() throws RepositoryException {
        // check sanity of this session
        sanityCheck();

        return context.getItemStateManager().hasAnyTransientItemStates();
    }

    /**
     * {@inheritDoc}
     */
    public void move(String srcAbsPath, String destAbsPath)
            throws RepositoryException {
        perform(new SessionMoveOperation(
                context, this, srcAbsPath, destAbsPath));
    }

    /**
     * {@inheritDoc}
     */
    public ContentHandler getImportContentHandler(String parentAbsPath,
                                                  int uuidBehavior)
            throws PathNotFoundException, ConstraintViolationException,
            VersionException, LockException, RepositoryException {
        // check sanity of this session
        sanityCheck();

        NodeImpl parent;
        try {
            Path p = getQPath(parentAbsPath).getNormalizedPath();
            if (!p.isAbsolute()) {
                throw new RepositoryException("not an absolute path: " + parentAbsPath);
            }
            parent = getItemManager().getNode(p);
        } catch (NameException e) {
            String msg = parentAbsPath + ": invalid path";
            log.debug(msg);
            throw new RepositoryException(msg, e);
        } catch (AccessDeniedException ade) {
            throw new PathNotFoundException(parentAbsPath);
        }

        // verify that parent node is checked-out, not locked and not protected
        // by either node type constraints nor by some retention or hold.
        int options = ItemValidator.CHECK_LOCK | ItemValidator.CHECK_CHECKED_OUT |
                ItemValidator.CHECK_CONSTRAINTS | ItemValidator.CHECK_HOLD | ItemValidator.CHECK_RETENTION;
        getValidator().checkModify(parent, options, Permission.NONE);

        SessionImporter importer = new SessionImporter(parent, this, uuidBehavior, wsp.getConfig().getImportConfig());
        return new ImportHandler(importer, this);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLive() {
        return context.getSessionState().isAlive();
    }

    /**
     * Utility method that removes all registered event listeners.
     */
    private void removeRegisteredEventListeners() {
        try {
            ObservationManager manager = getWorkspace().getObservationManager();
            // Use a copy to avoid modifying the set of registered listeners
            // while iterating over it
            Collection<EventListener> listeners =
                IteratorUtils.toList(manager.getRegisteredEventListeners());
            for (EventListener listener : listeners) {
                try {
                    manager.removeEventListener(listener);
                } catch (RepositoryException e) {
                    log.warn("Error removing event listener: " + listener, e);
                }
            }
        } catch (RepositoryException e) {
            log.warn("Error removing event listeners", e);
        }
    }

    /**
     * Invalidates this session and releases all associated resources.
     */
    @Override
    public void logout() {
        if (context.getSessionState().close()) {
            // JCR-798: Remove all registered event listeners to avoid concurrent
            // access to session internals by the event delivery or even listeners
            removeRegisteredEventListeners();

            // discard any pending changes first as those might
            // interfere with subsequent operations
            context.getItemStateManager().disposeAllTransientItemStates();

            // notify listeners that session is about to be closed
            notifyLoggingOut();

            // dispose session item state manager
            context.getItemStateManager().dispose();
            // dispose item manager
            context.getItemManager().dispose();
            // dispose workspace
            wsp.dispose();

            // logout JAAS subject
            if (loginContext != null) {
                try {
                    loginContext.logout();
                } catch (javax.security.auth.login.LoginException le) {
                    log.warn("failed to logout current subject: " + le.getMessage());
                }
                loginContext = null;
            }

            try {
                context.getAccessManager().close();
            } catch (Exception e) {
                log.warn("error while closing AccessManager", e);
            }

            // finally notify listeners that session has been closed
            notifyLoggedOut();
        }
    }


    /**
     * {@inheritDoc}
     */
    public Repository getRepository() {
        return repositoryContext.getRepository();
    }

    /**
     * {@inheritDoc}
     */
    public ValueFactory getValueFactory() {
        if (valueFactory == null) {
            valueFactory =
                new ValueFactoryImpl(this, context.getDataStore());
        }
        return valueFactory;
    }

    /**
     * {@inheritDoc}
     */
    public String getUserID() {
        return userId;
    }

    /**
     * {@inheritDoc}
     */
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    /**
     * {@inheritDoc}
     */
    public String[] getAttributeNames() {
        return attributes.keySet().toArray(new String[attributes.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNamespacePrefix(String prefix, String uri)
            throws NamespaceException, RepositoryException {
        super.setNamespacePrefix(prefix, uri);
        // Clear name and path caches
        namePathResolver = new DefaultNamePathResolver(this, true);
    }


    //------------------------------------------------------< locking support >
    /**
     * {@inheritDoc}
     */
    public void addLockToken(String lt) {
        try {
            wsp.getLockManager().addLockToken(lt);
        } catch (RepositoryException e) {
            log.debug("Error while adding lock token.");
        }
    }

    /**
     * {@inheritDoc}
     */
    public String[] getLockTokens() {
        try {
            return wsp.getLockManager().getLockTokens();
        } catch (RepositoryException e) {
            log.debug("Error while accessing lock tokens.");
            return new String[0];
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeLockToken(String lt) {
        try {
            wsp.getLockManager().removeLockToken(lt);
        } catch (RepositoryException e) {
            log.debug("Error while removing lock token.");
        }
    }

    /**
     * Return the lock manager for this session.
     * @return lock manager for this session
     */
    public LockManager getLockManager() throws RepositoryException {
        return wsp.getInternalLockManager();
    }

    /**
     * Returns all locks owned by this session.
     *
     * @return an array of <code>Lock</code>s
     */
    public Lock[] getLocks() {
        // check sanity of this session
        //sanityCheck();
        if (!isLive()) {
            log.error("failed to retrieve locks: session has been closed");
            return new Lock[0];
        }

        try {
            return getLockManager().getLocks(this);
        } catch (RepositoryException e) {
            log.error("Lock manager not available.", e);
            return new Lock[0];
        }
    }

    //--------------------------------------------------< new JSR 283 methods >
    /**
     * @see javax.jcr.Session#getNodeByIdentifier(String)
     * @since JCR 2.0
     */
    public Node getNodeByIdentifier(String id)
            throws ItemNotFoundException, RepositoryException {
        NodeId nodeId;
        try {
            nodeId = NodeId.valueOf(id);
        } catch (IllegalArgumentException iae) {
            throw new RepositoryException("invalid identifier: " + id);
        }
        return getNodeById(nodeId);
    }

    /**
     * @see javax.jcr.Session#getNode(String)
     * @since JCR 2.0
     */
    @Override
    public Node getNode(String absPath)
            throws PathNotFoundException, RepositoryException {
        // check sanity of this session
        sanityCheck();

        try {
            Path p = getQPath(absPath).getNormalizedPath();
            if (!p.isAbsolute()) {
                throw new RepositoryException("not an absolute path: " + absPath);
            }
            return getItemManager().getNode(p);
        } catch (AccessDeniedException ade) {
            throw new PathNotFoundException(absPath);
        } catch (NameException e) {
            String msg = "invalid path:" + absPath;
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * @see javax.jcr.Session#getProperty(String)
     * @since JCR 2.0
     */
    @Override
    public Property getProperty(String absPath)
            throws PathNotFoundException, RepositoryException {
        // check sanity of this session
        sanityCheck();

        try {
            Path p = getQPath(absPath).getNormalizedPath();
            if (!p.isAbsolute()) {
                throw new RepositoryException("not an absolute path: " + absPath);
            }
            return getItemManager().getProperty(p);
        } catch (AccessDeniedException ade) {
            throw new PathNotFoundException(absPath);
        } catch (NameException e) {
            String msg = "invalid path:" + absPath;
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * @see javax.jcr.Session#nodeExists(String)
     * @since JCR 2.0
     */
    @Override
    public boolean nodeExists(String absPath) throws RepositoryException {
        // check sanity of this session
        sanityCheck();

        try {
            Path p = getQPath(absPath).getNormalizedPath();
            if (!p.isAbsolute()) {
                throw new RepositoryException("not an absolute path: " + absPath);
            }
            return getItemManager().nodeExists(p);
        } catch (NameException e) {
            String msg = "invalid path:" + absPath;
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * @see javax.jcr.Session#propertyExists(String)
     * @since JCR 2.0
     */
    @Override
    public boolean propertyExists(String absPath) throws RepositoryException {
        // check sanity of this session
        sanityCheck();

        try {
            Path p = getQPath(absPath).getNormalizedPath();
            if (!p.isAbsolute()) {
                throw new RepositoryException("not an absolute path: " + absPath);
            }
            return getItemManager().propertyExists(p);
        } catch (NameException e) {
            String msg = "invalid path:" + absPath;
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * @see javax.jcr.Session#removeItem(String)
     * @since JCR 2.0
     */
    @Override
    public void removeItem(String absPath) throws VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        // check sanity of this session
        sanityCheck();
        Item item;
        try {
            Path p = getQPath(absPath).getNormalizedPath();
            if (!p.isAbsolute()) {
                throw new RepositoryException("not an absolute path: " + absPath);
            }
            item = getItemManager().getItem(p);
        } catch (AccessDeniedException e) {
            throw new PathNotFoundException(absPath);
        } catch (NameException e) {
            String msg = "invalid path:" + absPath;
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
        item.remove();
    }

    /**
     * @see javax.jcr.Session#hasPermission(String, String)
     * @since 2.0
     */
    public boolean hasPermission(String absPath, String actions) throws RepositoryException {
        // check sanity of this session
        sanityCheck();
        Path path = getQPath(absPath).getNormalizedPath();
        // test if path is absolute
        if (!path.isAbsolute()) {
            throw new RepositoryException("Absolute path expected. Was:" + absPath);
        }

        Set<String> s = new HashSet<String>(Arrays.asList(actions.split(",")));
        int permissions = 0;
        if (s.remove(ACTION_READ)) {
            permissions |= Permission.READ;
        }
        if (s.remove(ACTION_ADD_NODE)) {
            permissions |= Permission.ADD_NODE;
        }
        if (s.remove(ACTION_SET_PROPERTY)) {
            permissions |= Permission.SET_PROPERTY;
        }
        if (s.remove(ACTION_REMOVE)) {
            if (nodeExists(absPath)) {
                permissions |= (propertyExists(absPath)) ?
                        (Permission.REMOVE_NODE | Permission.REMOVE_PROPERTY) :
                        Permission.REMOVE_NODE;
            } else if (propertyExists(absPath)) {
                permissions |= Permission.REMOVE_PROPERTY;
            } else {
                // item doesn't exist -> check both permissions
                permissions = Permission.REMOVE_NODE | Permission.REMOVE_PROPERTY;
            }
        }
        if (!s.isEmpty()) {
            throw new IllegalArgumentException("Unknown actions: " + s);
        }
        try {
            return context.getAccessManager().isGranted(path, permissions);
        } catch (AccessDeniedException e) {
            return false;
        }
    }

    /**
     * @see javax.jcr.Session#hasCapability(String, Object, Object[])
     * @since JCR 2.0
     */
    public boolean hasCapability(String methodName, Object target, Object[] arguments)
            throws RepositoryException {
        // value of this method (as currently spec'ed) to jcr api clients
        // is rather limited...

        // here's therefore a minimal rather than best effort implementation;
        // returning true is always fine according to the spec...
        int options = ItemValidator.CHECK_CHECKED_OUT | ItemValidator.CHECK_LOCK |
                ItemValidator.CHECK_CONSTRAINTS | ItemValidator.CHECK_HOLD | ItemValidator.CHECK_RETENTION;
        if (target instanceof Node) {
            if (methodName.equals("addNode")
                    || methodName.equals("addMixin")
                    || methodName.equals("orderBefore")
                    || methodName.equals("removeMixin")
                    || methodName.equals("removeShare")
                    || methodName.equals("removeSharedSet")
                    || methodName.equals("setPrimaryType")
                    || methodName.equals("setProperty")
                    || methodName.equals("update")) {
                return getValidator().canModify((ItemImpl) target, options, Permission.NONE);
            } else if (methodName.equals("remove")) {
                try {
                    getValidator().checkRemove((ItemImpl) target, options, Permission.NONE);
                } catch (RepositoryException e) {
                    return false;
                }
            }
        } else if (target instanceof Property) {
            if (methodName.equals("setValue")
                    || methodName.equals("save")) {
                return getValidator().canModify((ItemImpl) target, options, Permission.NONE);
            } else if (methodName.equals("remove")) {
                try {
                    getValidator().checkRemove((ItemImpl) target, options, Permission.NONE);
                } catch (RepositoryException e) {
                    return false;
                }
            }
        } else if (target instanceof Workspace) {
            if (methodName.equals("clone")
                    || methodName.equals("copy")
                    || methodName.equals("createWorkspace")
                    || methodName.equals("deleteWorkspace")
                    || methodName.equals("getImportContentHandler")
                    || methodName.equals("importXML")
                    || methodName.equals("move")) {
                // todo minimal, best effort checks (e.g. permissions for write methods etc)
            }
        } else if (target instanceof Session) {
            if (methodName.equals("clone")
                    || methodName.equals("removeItem")
                    || methodName.equals("getImportContentHandler")
                    || methodName.equals("importXML")
                    || methodName.equals("save")) {
                // todo minimal, best effort checks (e.g. permissions for write methods etc)
            }
        }

        // we're unable to evaluate capability, return true (staying on the safe side)
        return true;
    }

    /**
     * @see javax.jcr.Session#getAccessControlManager()
     * @since JCR 2.0
     */
    public AccessControlManager getAccessControlManager()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        AccessManager accessMgr = context.getAccessManager();
        if (accessMgr instanceof AccessControlManager) {
            return (AccessControlManager) accessMgr;
        } else {
            throw new UnsupportedRepositoryOperationException(
                    "Access control discovery is not supported.");
        }
    }

    /**
     * @see javax.jcr.Session#getRetentionManager()
     * @since JCR 2.0
     */
    public RetentionManager getRetentionManager()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        // check sanity of this session
        sanityCheck();
        if (retentionManager == null) {
            // make sure the internal retention manager exists.
            getRetentionRegistry();
            // create the api level retention manager.
            retentionManager = new RetentionManagerImpl(this);
        }
        return retentionManager;
    }

    //-------------------------------------------------------------< Dumpable >
    /**
     * {@inheritDoc}
     */
    public void dump(PrintStream ps) {
        ps.print("Session: ");
        if (userId == null) {
            ps.print("unknown");
        } else {
            ps.print(userId);
        }
        ps.println(" (" + this + ")");
        ps.println();
        context.getItemManager().dump(ps);
        ps.println();
        context.getItemStateManager().dump(ps);
    }

    /**
     * Finalize the session. If the application doesn't call Session.logout(),
     * the session is closed automatically; however a warning is written to the log file,
     * together with the stack trace of where the session was opened.
     */
    @Override
    public void finalize() {
        if (isLive()) {
            log.warn("Unclosed session detected. The session was opened here: ", openStackTrace);
            logout();
        }
    }
}
