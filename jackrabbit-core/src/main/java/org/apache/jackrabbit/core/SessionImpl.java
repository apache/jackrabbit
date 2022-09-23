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

import static org.apache.jackrabbit.core.ItemValidator.CHECK_CHECKED_OUT;
import static org.apache.jackrabbit.core.ItemValidator.CHECK_CONSTRAINTS;
import static org.apache.jackrabbit.core.ItemValidator.CHECK_HOLD;
import static org.apache.jackrabbit.core.ItemValidator.CHECK_LOCK;
import static org.apache.jackrabbit.core.ItemValidator.CHECK_RETENTION;

import java.io.File;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

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

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.map.AbstractReferenceMap.ReferenceStrength;
import org.apache.commons.collections4.map.ReferenceMap;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.commons.AbstractSession;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.gc.GarbageCollector;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.observation.ObservationManagerImpl;
import org.apache.jackrabbit.core.retention.RetentionManagerImpl;
import org.apache.jackrabbit.core.retention.RetentionRegistry;
import org.apache.jackrabbit.core.security.AMContext;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.core.security.SystemPrincipal;
import org.apache.jackrabbit.core.security.authentication.AuthContext;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.security.principal.AdminPrincipal;
import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.core.session.SessionItemOperation;
import org.apache.jackrabbit.core.session.SessionOperation;
import org.apache.jackrabbit.core.session.SessionRefreshOperation;
import org.apache.jackrabbit.core.session.SessionSaveOperation;
import org.apache.jackrabbit.core.state.SessionItemStateManager;
import org.apache.jackrabbit.core.version.InternalVersionManager;
import org.apache.jackrabbit.core.xml.ImportHandler;
import org.apache.jackrabbit.core.xml.SessionImporter;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.SessionExtensions;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.IdentifierResolver;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;

/**
 * A <code>SessionImpl</code> ...
 */
public class SessionImpl extends AbstractSession
        implements JackrabbitSession, SessionExtensions, NamespaceResolver, NamePathResolver, IdentifierResolver {

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

    /**
     * Name of the session attribute that controls whether repository
     * inconsistencies should be automatically fixed when traversing over child
     * nodes, when trying to add a child node, or removing a child node.
     *
     * @since Apache Jackrabbit 2.2
     * @see <a href="https://issues.apache.org/jira/browse/JCR-2740">JCR-2740</a>
     */
    public static final String AUTO_FIX_CORRUPTIONS =
        "org.apache.jackrabbit.autoFixCorruptions";

    private static Logger log = LoggerFactory.getLogger(SessionImpl.class);

    /**
     * Session counter. Used to generate unique internal session names.
     */
    private static final AtomicLong SESSION_COUNTER = new AtomicLong();

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
     * Unique internal name of this session. Returned by the
     * {@link #toString()} method for use in logging and debugging.
     */
    private final String sessionName;

    /**
     * the attributes of this session
     */
    protected final Map<String, Object> attributes =
        new HashMap<String, Object>();

    /**
     * Name and Path resolver
     */
    protected NamePathResolver namePathResolver;

    /**
     * The version manager for this session
     */
    protected final InternalVersionManager versionMgr;

    /**
     * Listeners (weak references)
     */
    protected final Map<SessionListener, SessionListener> listeners =
        new ReferenceMap<>(ReferenceStrength.WEAK, ReferenceStrength.WEAK);

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
    private final Exception openStackTrace;

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
        this.context = new SessionContext(repositoryContext, this, wspConfig);
        this.repositoryContext = repositoryContext;
        this.subject = subject;

        this.userId = retrieveUserId(subject, wspConfig.getName());
        long count = SESSION_COUNTER.incrementAndGet();
        if (userId != null) {
            String user = Text.escapeIllegalJcrChars(userId);
            this.sessionName = "session-" + user + "-" + count;
        } else {
            this.sessionName = "session-" + count;
        }

        this.namePathResolver = new DefaultNamePathResolver(this, this, true);
        this.context.setItemStateManager(createSessionItemStateManager());
        this.context.setItemManager(createItemManager());
        this.context.setAccessManager(createAccessManager(subject));
        this.context.setObservationManager(createObservationManager(wspConfig.getName()));

        this.versionMgr = createVersionManager();

        // avoid building the stack trace when it won't be used anyway
        this.openStackTrace = log.isWarnEnabled() ? new Exception("Stack Trace") : null;
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
                context.getWorkspace().getItemStateManager());
        context.getWorkspace().getItemStateManager().addListener(mgr);
        return mgr;
    }

    /**
     * Create the item manager.
     * @return item manager
     */
    protected ItemManager createItemManager() {
        ItemManager mgr = new ItemManager(context);
        context.getItemStateManager().addListener(mgr);
        return mgr;
    }

    protected ObservationManagerImpl createObservationManager(String wspName)
            throws RepositoryException {
        try {
            return new ObservationManagerImpl(
                    context.getRepository().getObservationDispatcher(wspName),
                    this, context.getRepositoryContext().getClusterNode());
        } catch (NoSuchWorkspaceException e) {
            // should never get here
            throw new RepositoryException(
                    "Internal error: failed to create observation manager", e);
        }
    }
    /**
     * Create the version manager. If we are not using XA, we may safely use
     * the repository version manager.
     * @return version manager
     */
    protected InternalVersionManager createVersionManager()
            throws RepositoryException {
        return context.getRepositoryContext().getInternalVersionManager();
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
                new File(context.getRepository().getConfig().getHomeDir()),
                context.getRepositoryContext().getFileSystem(),
                this,
                subject,
                context.getHierarchyManager(),
                context.getPrivilegeManager(),
                this,
                wspName);
        return repositoryContext.getSecurityManager().getAccessManager(this, ctx);
    }

    private <T> T perform(SessionOperation<T> operation)
            throws RepositoryException {
        return context.getSessionState().perform(operation);
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
     * Returns a read only copy of the <code>Subject</code> associated with this
     * session.
     *
     * @return a read only copy of <code>Subject</code> associated with this session
     */
    public Subject getSubject() {
        Subject readOnly = new Subject(true, subject.getPrincipals(), subject.getPublicCredentials(), subject.getPrivateCredentials());
        return readOnly;
    }

    /**
     * Returns <code>true</code> if the subject contains a
     * <code>SystemPrincipal</code>; <code>false</code> otherwise.
     *
     * @return <code>true</code> if this is an system session.
     */
    public boolean isSystem() {
        // NOTE: for backwards compatibility evaluate subject for containing SystemPrincipal
        // TODO: Q: shouldn't 'isSystem' rather be covered by instances of SystemSession only?
        return (subject != null && !subject.getPrincipals(SystemPrincipal.class).isEmpty());
    }
    
    /**
     * Returns <code>true</code> if this session has been created for the
     * administrator. <code>False</code> otherwise.
     *
     * @return <code>true</code> if this is an admin session.
     */
    public boolean isAdmin() {
        // NOTE: don't replace by getUserManager()
        if (userManager != null) {
            try {
                Authorizable a = userManager.getAuthorizable(userId);
                if (a != null && !a.isGroup()) {
                    return ((User) a).isAdmin();
                }
            } catch (RepositoryException e) {
                // no user management -> use fallback
            }

        }
        // fallback: user manager not yet initialized or user mgt not supported
        // -> check for AdminPrincipal being present in the subject.
        return (subject != null && !subject.getPrincipals(AdminPrincipal.class).isEmpty());
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
        Subject newSubject = new Subject(subject.isReadOnly(), subject.getPrincipals(), subject.getPublicCredentials(), subject.getPrivateCredentials());
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
        return context.getNodeTypeManager();
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
        return context.getWorkspace().getRetentionRegistry();
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
    public void setAttribute(String name, Object value) {
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
     * @see JackrabbitSession#hasPermission(String, String...)
     */
    @Override
    public boolean hasPermission(String absPath, String... actions) throws RepositoryException {
        return hasPermission(absPath, Text.implode(actions, ","));
    }

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

    @Override
    public Item getItemOrNull(String absPath) throws RepositoryException {
        // TODO optimise, reduce to a single read operation
        if (itemExists(absPath)) {
            return getItem(absPath);
        } else {
            return null;
        }
    }

    @Override
    public Property getPropertyOrNull(String absPath) throws RepositoryException {
        // TODO optimise, reduce to a single read operation
        if (propertyExists(absPath)) {
            return getProperty(absPath);
        } else {
            return null;
        }
    }

    @Override
    public Node getNodeOrNull(String absPath) throws RepositoryException {
        // TODO optimise, reduce to a single read operation
        if (nodeExists(absPath)) {
            return getNode(absPath);
        } else {
            return null;
        }
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
        return context.getWorkspace();
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
    public Item getItem(String absPath) throws RepositoryException {
        return perform(SessionItemOperation.getItem(absPath));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean itemExists(String absPath) throws RepositoryException {
        if (absPath != null && absPath.startsWith("[") && absPath.endsWith("]")) {
            // an identifier segment has been specified (JCR-3014)
            try {
                NodeId id = NodeId.valueOf(absPath.substring(1, absPath.length() - 1));
                return getItemManager().itemExists(id);
            } catch (IllegalArgumentException e) {
                throw new MalformedPathException(absPath);
            }
        }
        return perform(SessionItemOperation.itemExists(absPath));
    }

    /**
     * {@inheritDoc}
     */
    public void save() throws RepositoryException {
        // JCR-3131: no need to perform save op when there's nothing to save...
        if (context.getItemStateManager().hasAnyTransientItemStates()) {
            perform(new SessionSaveOperation());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void refresh(boolean keepChanges) throws RepositoryException {
        perform(new SessionRefreshOperation(
                keepChanges, clusterSyncOnRefresh()));
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
     * Checks whether repository inconsistencies should be automatically fixed
     * when traversing over child nodes, when trying to add a child node, or
     * when removing a child node.
     *
     * @return <code>true</code> if the {@link #AUTO_FIX_CORRUPTIONS}
     *         attribute is set, <code>false</code> otherwise
     * @since Apache Jackrabbit 2.2
     * @see <a href="https://issues.apache.org/jira/browse/JCR-2740">JCR-2740</a>
     */
    protected boolean autoFixCorruptions() {
        return getAttribute(AUTO_FIX_CORRUPTIONS) != null;
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
        perform(new SessionMoveOperation(this, srcAbsPath, destAbsPath));
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
        context.getItemValidator().checkModify(parent, options, Permission.NONE);

        SessionImporter importer = new SessionImporter(
                parent, this, uuidBehavior,
                context.getWorkspace().getConfig().getImportConfig());
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
    @SuppressWarnings("unchecked")
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

            context.getPrivilegeManager().dispose();
            context.getNodeTypeManager().dispose();
            // dispose session item state manager
            context.getItemStateManager().dispose();
            // dispose item manager
            context.getItemManager().dispose();
            // dispose workspace
            context.getWorkspace().dispose();

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
        return context.getValueFactory();
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
            getWorkspace().getLockManager().addLockToken(lt);
        } catch (RepositoryException e) {
            log.debug("Error while adding lock token.");
        }
    }

    /**
     * {@inheritDoc}
     */
    public String[] getLockTokens() {
        try {
            return getWorkspace().getLockManager().getLockTokens();
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
            getWorkspace().getLockManager().removeLockToken(lt);
        } catch (RepositoryException e) {
            log.debug("Error while removing lock token.");
        }
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
            return context.getWorkspace().getInternalLockManager().getLocks(this);
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
            throw new RepositoryException("invalid identifier: " + id,iae);
        }
        return getNodeById(nodeId);
    }

    /**
     * @see javax.jcr.Session#getNode(String)
     * @since JCR 2.0
     */
    @Override
    public Node getNode(String absPath) throws RepositoryException {
        return perform(SessionItemOperation.getNode(absPath));
    }

    /**
     * @see javax.jcr.Session#getProperty(String)
     * @since JCR 2.0
     */
    @Override
    public Property getProperty(String absPath) throws RepositoryException {
        return perform(SessionItemOperation.getProperty(absPath));
    }

    /**
     * @see javax.jcr.Session#nodeExists(String)
     * @since JCR 2.0
     */
    @Override
    public boolean nodeExists(String absPath) throws RepositoryException {
        if (absPath != null && absPath.startsWith("[") && absPath.endsWith("]")) {
            // an identifier segment has been specified (JCR-3014)
            try {
                NodeId id = NodeId.valueOf(absPath.substring(1, absPath.length() - 1));
                return getItemManager().itemExists(id);
            } catch (IllegalArgumentException e) {
                throw new MalformedPathException(absPath);
            }
        }
        return perform(SessionItemOperation.nodeExists(absPath));
    }

    /**
     * @see javax.jcr.Session#propertyExists(String)
     * @since JCR 2.0
     */
    @Override
    public boolean propertyExists(String absPath) throws RepositoryException {
        return perform(SessionItemOperation.propertyExists(absPath));
    }

    /**
     * @see javax.jcr.Session#removeItem(String)
     * @since JCR 2.0
     */
    @Override
    public void removeItem(String absPath) throws RepositoryException {
        perform(SessionItemOperation.remove(absPath));
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
        ItemValidator validator = context.getItemValidator();
        int options =
            CHECK_CHECKED_OUT | CHECK_LOCK | CHECK_CONSTRAINTS
            | CHECK_HOLD | CHECK_RETENTION;
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
                return validator.canModify((ItemImpl) target, options, Permission.NONE);
            } else if (methodName.equals("remove")) {
                try {
                    validator.checkRemove((ItemImpl) target, options, Permission.NONE);
                } catch (RepositoryException e) {
                    return false;
                }
            }
        } else if (target instanceof Property) {
            if (methodName.equals("setValue")
                    || methodName.equals("save")) {
                return validator.canModify((ItemImpl) target, options, Permission.NONE);
            } else if (methodName.equals("remove")) {
                try {
                    validator.checkRemove((ItemImpl) target, options, Permission.NONE);
                } catch (RepositoryException e) {
                    return false;
                }
            }
// TODO: Add minimal, best effort checks for Workspace and Session operations
//        } else if (target instanceof Workspace) {
//            if (methodName.equals("clone")
//                    || methodName.equals("copy")
//                    || methodName.equals("createWorkspace")
//                    || methodName.equals("deleteWorkspace")
//                    || methodName.equals("getImportContentHandler")
//                    || methodName.equals("importXML")
//                    || methodName.equals("move")) {
//                // TODO minimal, best effort checks (e.g. permissions for write methods etc)
//            }
//        } else if (target instanceof Session) {
//            if (methodName.equals("clone")
//                    || methodName.equals("removeItem")
//                    || methodName.equals("getImportContentHandler")
//                    || methodName.equals("importXML")
//                    || methodName.equals("save")) {
//                // TODO minimal, best effort checks (e.g. permissions for write methods etc)
//            }
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

    //--------------------------------------------------------------< Object >

    /**
     * Returns the unique internal name of this session. The returned name
     * is especially useful for debugging and logging purposes.
     *
     * @see #sessionName
     * @return session name
     */
    @Override
    public String toString() {
        return sessionName;
    }

    /**
     * Finalize the session. If the application doesn't call Session.logout(),
     * the session is closed automatically; however a warning is written to the log file,
     * together with the stack trace of where the session was opened.
     */
    @Override
    public void finalize() {
        if (isLive()) {
            if (openStackTrace != null) {
                // Log a warning if and only if openStackTrace is not null
                // indicating that the warn level is enabled and the session has
                // been fully created
                log.warn("Unclosed session detected. The session was opened here: ", openStackTrace);
            }
            logout();
        }
    }

}
