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
import org.apache.jackrabbit.commons.AbstractSession;
import org.apache.jackrabbit.core.RepositoryImpl.WorkspaceInfo;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.data.GarbageCollector;
import org.apache.jackrabbit.core.lock.LockManager;
import org.apache.jackrabbit.core.nodetype.NodeDefinitionImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.persistence.IterablePersistenceManager;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.core.security.AMContext;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.jsr283.security.AccessControlManager;
import org.apache.jackrabbit.api.jsr283.retention.RetentionManager;
import org.apache.jackrabbit.core.security.authentication.AuthContext;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.LocalItemStateManager;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.SessionItemStateManager;
import org.apache.jackrabbit.core.state.SharedItemStateManager;
import org.apache.jackrabbit.core.util.Dumpable;
import org.apache.jackrabbit.core.version.VersionManager;
import org.apache.jackrabbit.core.version.VersionManagerImpl;
import org.apache.jackrabbit.core.xml.ImportHandler;
import org.apache.jackrabbit.core.xml.SessionImporter;
import org.apache.jackrabbit.core.retention.RetentionManagerImpl;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.uuid.UUID;
import org.apache.jackrabbit.value.ValueFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
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
import javax.jcr.lock.LockException;
import javax.jcr.lock.Lock;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import javax.jcr.version.VersionException;
import javax.security.auth.Subject;
import java.io.File;
import java.io.PrintStream;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;

/**
 * A <code>SessionImpl</code> ...
 */
public class SessionImpl extends AbstractSession
        implements JackrabbitSession, NamespaceResolver, NamePathResolver, Dumpable {

    private static Logger log = LoggerFactory.getLogger(SessionImpl.class);

    /**
     * TODO deprecate as soon as present with Session interface (JSR 283)
     */
    public static final String READ_ACTION = "read";

    /**
     * TODO deprecate as soon as present with Session interface (JSR 283)
     */
    public static final String REMOVE_ACTION = "remove";

    /**
     * TODO deprecate as soon as present with Session interface (JSR 283)
     */
    public static final String ADD_NODE_ACTION = "add_node";

    /**
     * TODO deprecate as soon as present with Session interface (JSR 283)
     */
    public static final String SET_PROPERTY_ACTION = "set_property";

    /**
     * flag indicating whether this session is alive
     */
    protected boolean alive;

    /**
     * the repository that issued this session
     */
    protected final RepositoryImpl rep;

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
    protected final HashMap attributes = new HashMap();

    /**
     * the node type manager
     */
    protected final NodeTypeManagerImpl ntMgr;

    /**
     * the AccessManager associated with this session
     */
    protected AccessManager accessMgr;

    /**
     * the item state mgr associated with this session
     */
    protected final SessionItemStateManager itemStateMgr;

    /**
     * the HierarchyManager associated with this session
     */
    protected final HierarchyManager hierMgr;

    /**
     * the item mgr associated with this session
     */
    protected final ItemManager itemMgr;

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
    protected final VersionManager versionMgr;

    /**
     * Listeners (weak references)
     */
    protected final Map listeners = new ReferenceMap(ReferenceMap.WEAK, ReferenceMap.WEAK);

    /**
     * Lock tokens
     */
    protected final Set lockTokens = new HashSet();

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
     * Protected constructor.
     *
     * @param rep
     * @param loginContext
     * @param wspConfig
     * @throws AccessDeniedException if the subject of the given login context
     *                               is not granted access to the specified
     *                               workspace
     * @throws RepositoryException   if another error occurs
     */
    protected SessionImpl(RepositoryImpl rep, AuthContext loginContext,
                          WorkspaceConfig wspConfig)
            throws AccessDeniedException, RepositoryException {
        this(rep, loginContext.getSubject(), wspConfig);
        this.loginContext = loginContext;
    }

    /**
     * Protected constructor.
     *
     * @param rep
     * @param subject
     * @param wspConfig
     * @throws AccessDeniedException if the given subject is not granted access
     *                               to the specified workspace
     * @throws RepositoryException   if another error occurs
     */
    protected SessionImpl(RepositoryImpl rep, Subject subject,
                          WorkspaceConfig wspConfig)
            throws AccessDeniedException, RepositoryException {
        alive = true;
        this.rep = rep;
        this.subject = subject;

        userId = retrieveUserId(subject);

        namePathResolver = new DefaultNamePathResolver(this, true);
        ntMgr = new NodeTypeManagerImpl(rep.getNodeTypeRegistry(), this, rep.getDataStore());
        String wspName = wspConfig.getName();
        wsp = createWorkspaceInstance(wspConfig,
                rep.getWorkspaceStateManager(wspName), rep, this);
        itemStateMgr = createSessionItemStateManager(wsp.getItemStateManager());
        hierMgr = itemStateMgr.getHierarchyMgr();
        itemMgr = createItemManager(itemStateMgr, hierMgr);
        accessMgr = createAccessManager(subject, itemStateMgr.getHierarchyMgr());
        versionMgr = createVersionManager(rep);
    }

    /**
     * Retrieve the userID from the specified subject.
     *
     * @return the userID.
     */
    protected String retrieveUserId(Subject subject) throws RepositoryException {
        return rep.getSecurityManager().getUserID(subject);
    }

    /**
     * Create the session item state manager.
     *
     * @return session item state manager
     */
    protected SessionItemStateManager createSessionItemStateManager(LocalItemStateManager manager) {
        return new SessionItemStateManager(
                rep.getRootNodeId(), manager, rep.getNodeTypeRegistry());
    }

    /**
     * Creates the workspace instance backing this session.
     *
     * @param wspConfig The workspace configuration
     * @param stateMgr  The shared item state manager
     * @param rep       The repository
     * @param session   The session
     * @return An instance of the {@link WorkspaceImpl} class or an extension
     *         thereof.
     */
    protected WorkspaceImpl createWorkspaceInstance(WorkspaceConfig wspConfig,
                                                    SharedItemStateManager stateMgr,
                                                    RepositoryImpl rep,
                                                    SessionImpl session) {
        return new WorkspaceImpl(wspConfig, stateMgr, rep, session);
    }

    /**
     * Create the item manager.
     * @return item manager
     */
    protected ItemManager createItemManager(SessionItemStateManager itemStateMgr,
                                            HierarchyManager hierMgr) {
        return new ItemManager(itemStateMgr, hierMgr, this,
                ntMgr.getRootNodeDefinition(), rep.getRootNodeId());
    }

    /**
     * Create the version manager. If we are not using XA, we may safely use
     * the repository version manager.
     * @return version manager
     */
    protected VersionManager createVersionManager(RepositoryImpl rep)
            throws RepositoryException {

        return rep.getVersionManager();
    }

    /**
     * Create the access manager.
     *
     * @param subject
     * @param hierarchyManager
     * @return access manager
     * @throws AccessDeniedException if the current subject is not granted access
     *                               to the current workspace
     * @throws RepositoryException   if the access manager cannot be instantiated
     */
    protected AccessManager createAccessManager(Subject subject,
                                                HierarchyManager hierarchyManager)
            throws AccessDeniedException, RepositoryException {
        String wspName = getWorkspace().getName();
        AMContext ctx = new AMContext(new File(rep.getConfig().getHomeDir()),
                rep.getFileSystem(),
                this,
                getSubject(),
                hierarchyManager,
                this,
                wspName);
        return rep.getSecurityManager().getAccessManager(this, ctx);
    }

    /**
     * Performs a sanity check on this session.
     *
     * @throws RepositoryException if this session has been rendered invalid
     *                             for some reason (e.g. if this session has
     *                             been closed explicitly or if it has expired)
     */
    protected void sanityCheck() throws RepositoryException {
        // check session status
        if (!alive) {
            throw new RepositoryException("this session has been closed");
        }
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
            workspaceName = rep.getConfig().getDefaultWorkspaceName();
        }
        if (loginContext!=null) {
            return rep.createSession(loginContext, workspaceName);
        } else {
            return rep.createSession(getSubject(), workspaceName);
        }
    }

    /**
     * Returns the <code>AccessManager</code> associated with this session.
     *
     * @return the <code>AccessManager</code> associated with this session
     */
    public AccessManager getAccessManager() {
        return accessMgr;
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
        return itemMgr;
    }

    /**
     * Returns the <code>SessionItemStateManager</code> associated with this session.
     *
     * @return the <code>SessionItemStateManager</code> associated with this session
     */
    protected SessionItemStateManager getItemStateManager() {
        return itemStateMgr;
    }

    /**
     * Returns the <code>HierarchyManager</code> associated with this session.
     *
     * @return the <code>HierarchyManager</code> associated with this session
     */
    public HierarchyManager getHierarchyManager() {
        return hierMgr;
    }

    /**
     * Returns the <code>VersionManager</code> associated with this session.
     *
     * @return the <code>VersionManager</code> associated with this session
     */
    public VersionManager getVersionManager() {
        return versionMgr;
    }

    /**
     * Retrieves the referenceable node with the given <code>UUID</code>.
     *
     * @param uuid uuid of the node to be retrieved
     * @return referenceable node with the given uuid
     * @throws ItemNotFoundException if no node exists with the given uuid or
     * if the existing node is not referenceable.
     * @throws RepositoryException if another error occurs.
     * @see #getNodeByUUID(String)
     * @see #getNodeById(NodeId)
     */
    public Node getNodeByUUID(UUID uuid) throws ItemNotFoundException, RepositoryException {
        NodeImpl node = getNodeById(new NodeId(uuid));
        // since the uuid of a node is only exposed through jcr:uuid declared
        // by mix:referenceable it's rather unlikely that a client can possibly
        // know the internal uuid of a non-referenceable node; omitting the
        // check for mix:referenceable seems therefore to be a reasonable
        // compromise in order to improve performance.
/*
        if (node.isNodeType(Name.MIX_REFERENCEABLE)) {
            return node;
        } else {
            // there is a node with that uuid but the node does not expose it
            throw new ItemNotFoundException(uuid.toString());
        }
*/
        return node;
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
        ArrayList list = new ArrayList();
        String[] names = rep.getWorkspaceNames();
        for (int i = 0; i < names.length; i++) {
            try {
                if (getAccessManager().canAccess(names[i])) {
                    list.add(names[i]);
                }
            } catch (NoSuchWorkspaceException nswe) {
                // should never happen, ignore...
            }
        }
        return (String[]) list.toArray(new String[list.size()]);
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
        rep.createWorkspace(workspaceName);
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
    protected void createWorkspace(String workspaceName,
                                   InputSource configTemplate)
            throws AccessDeniedException, RepositoryException {
        // @todo verify that this session has the right privileges for this operation
        rep.createWorkspace(workspaceName, configTemplate);
    }

    /**
     * Notify the listeners that this session is about to be closed.
     */
    protected void notifyLoggingOut() {
        // copy listeners to array to avoid ConcurrentModificationException
        SessionListener[] la =
                (SessionListener[]) listeners.values().toArray(
                        new SessionListener[listeners.size()]);
        for (int i = 0; i < la.length; i++) {
            if (la[i] != null) {
                la[i].loggingOut(this);
            }
        }
    }

    /**
     * Notify the listeners that this session has been closed.
     */
    protected void notifyLoggedOut() {
        // copy listeners to array to avoid ConcurrentModificationException
        SessionListener[] la =
                (SessionListener[]) listeners.values().toArray(
                        new SessionListener[listeners.size()]);
        for (int i = 0; i < la.length; i++) {
            if (la[i] != null) {
                la[i].loggedOut(this);
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
     * @throws ItemStateException
     * @throws RepositoryException
     */
    public GarbageCollector createDataStoreGarbageCollector() throws RepositoryException, ItemStateException {
        ArrayList pmList = new ArrayList();
        VersionManagerImpl vm = (VersionManagerImpl) rep.getVersionManager();
        PersistenceManager pm = vm.getPersistenceManager();
        pmList.add(pm);
        String[] wspNames = rep.getWorkspaceNames();
        SystemSession[] sysSessions = new SystemSession[wspNames.length];
        for (int i = 0; i < wspNames.length; i++) {
            String wspName = wspNames[i];
            WorkspaceInfo wspInfo = rep.getWorkspaceInfo(wspName);
            sysSessions[i] = rep.getSystemSession(wspName);
            pm = wspInfo.getPersistenceManager();
            pmList.add(pm);
        }
        IterablePersistenceManager[] ipmList = new IterablePersistenceManager[pmList.size()];
        for (int i = 0; i < pmList.size(); i++) {
            pm = (PersistenceManager) pmList.get(i);
            if (!(pm instanceof IterablePersistenceManager)) {
                ipmList = null;
                break;
            }
            ipmList[i] = (IterablePersistenceManager) pm;
        }
        GarbageCollector gc = new GarbageCollector(this, ipmList, sysSessions);
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

    //----------------------------------------------------< JackrabbitSession >
    /**
     * @see JackrabbitSession#getPrincipalManager()
     */
    public PrincipalManager getPrincipalManager() throws RepositoryException, AccessDeniedException {
        if (principalManager == null) {
            principalManager = rep.getSecurityManager().getPrincipalManager(this);
        }
        return principalManager;
    }

    /**
     * @see JackrabbitSession#getUserManager()
     */
    public UserManager getUserManager() throws AccessDeniedException, RepositoryException {
        if (userManager == null) {
            userManager = rep.getSecurityManager().getUserManager(this);
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
        return wsp;
    }

    /**
     * {@inheritDoc}
     */
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
            return rep.login(otherCredentials, getWorkspace().getName());
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
            return getNodeByUUID(UUID.fromString(uuid));
        } catch (IllegalArgumentException e) {
            // Assuming the exception is from UUID.fromString()
            throw new RepositoryException("Invalid UUID: " + uuid, e);
        }
    }

    /**
     * {@inheritDoc}
     */
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
    public void save()
            throws AccessDeniedException, ItemExistsException,
            ConstraintViolationException, InvalidItemStateException,
            VersionException, LockException, NoSuchNodeTypeException,
            RepositoryException {
        // check sanity of this session
        sanityCheck();

        getItemManager().getRootNode().save();
    }

    /**
     * {@inheritDoc}
     */
    public void refresh(boolean keepChanges) throws RepositoryException {
        // check sanity of this session
        sanityCheck();

        if (!keepChanges) {
            // optimization
            itemStateMgr.disposeAllTransientItemStates();
            return;
        }
        getItemManager().getRootNode().refresh(keepChanges);
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasPendingChanges() throws RepositoryException {
        // check sanity of this session
        sanityCheck();

        return itemStateMgr.hasAnyTransientItemStates();
    }

    /**
     * {@inheritDoc}
     */
    public void move(String srcAbsPath, String destAbsPath)
            throws ItemExistsException, PathNotFoundException,
            VersionException, ConstraintViolationException, LockException,
            RepositoryException {
        // check sanity of this session
        sanityCheck();

        // check paths & get node instances

        Path srcPath;
        Path.Element srcName;
        Path srcParentPath;
        NodeImpl targetNode;
        NodeImpl srcParentNode;
        try {
            srcPath = getQPath(srcAbsPath).getNormalizedPath();
            if (!srcPath.isAbsolute()) {
                throw new RepositoryException("not an absolute path: " + srcAbsPath);
            }
            srcName = srcPath.getNameElement();
            srcParentPath = srcPath.getAncestor(1);
            targetNode = getItemManager().getNode(srcPath);
            srcParentNode = getItemManager().getNode(srcParentPath);
        } catch (AccessDeniedException ade) {
            throw new PathNotFoundException(srcAbsPath);
        } catch (NameException e) {
            String msg = srcAbsPath + ": invalid path";
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }

        Path destPath;
        Path.Element destName;
        Path destParentPath;
        NodeImpl destParentNode;
        try {
            destPath = getQPath(destAbsPath).getNormalizedPath();
            if (!destPath.isAbsolute()) {
                throw new RepositoryException("not an absolute path: " + destAbsPath);
            }
            if (srcPath.isAncestorOf(destPath)) {
                String msg = destAbsPath + ": invalid destination path (cannot be descendant of source path)";
                log.debug(msg);
                throw new RepositoryException(msg);
            }
            destName = destPath.getNameElement();
            destParentPath = destPath.getAncestor(1);
            destParentNode = getItemManager().getNode(destParentPath);
        } catch (AccessDeniedException ade) {
            throw new PathNotFoundException(destAbsPath);
        } catch (NameException e) {
            String msg = destAbsPath + ": invalid path";
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }

        if (hierMgr.isShareAncestor(targetNode.getNodeId(), destParentNode.getNodeId())) {
            String msg = destAbsPath + ": invalid destination path (share cycle detected)";
            log.debug(msg);
            throw new RepositoryException(msg);
        }

        int ind = destName.getIndex();
        if (ind > 0) {
            // subscript in name element
            String msg = destAbsPath + ": invalid destination path (subscript in name element is not allowed)";
            log.debug(msg);
            throw new RepositoryException(msg);
        }

        // verify that both source and destination parent nodes are checked-out
        if (!srcParentNode.internalIsCheckedOut()) {
            String msg = srcAbsPath + ": cannot move a child of a checked-in node";
            log.debug(msg);
            throw new VersionException(msg);
        }
        if (!destParentNode.internalIsCheckedOut()) {
            String msg = destAbsPath + ": cannot move a target to a checked-in node";
            log.debug(msg);
            throw new VersionException(msg);
        }

        // check for name collisions

        NodeImpl existing = null;
        try {
            existing = getItemManager().getNode(destPath);
            // there's already a node with that name:
            // check same-name sibling setting of existing node
            if (!existing.getDefinition().allowsSameNameSiblings()) {
                throw new ItemExistsException(
                        "Same name siblings are not allowed: " + existing);
            }
        } catch (AccessDeniedException ade) {
            // FIXME by throwing ItemExistsException we're disclosing too much information
            throw new ItemExistsException(destAbsPath);
        } catch (PathNotFoundException pnfe) {
            // no name collision, fall through
        }

        // check constraints

        // get applicable definition of target node at new location
        NodeTypeImpl nt = (NodeTypeImpl) targetNode.getPrimaryNodeType();
        NodeDefinitionImpl newTargetDef;
        try {
            newTargetDef = destParentNode.getApplicableChildNodeDefinition(destName.getName(), nt.getQName());
        } catch (RepositoryException re) {
            String msg = destAbsPath + ": no definition found in parent node's node type for new node";
            log.debug(msg);
            throw new ConstraintViolationException(msg, re);
        }
        // if there's already a node with that name also check same-name sibling
        // setting of new node; just checking same-name sibling setting on
        // existing node is not sufficient since same-name sibling nodes don't
        // necessarily have identical definitions
        if (existing != null && !newTargetDef.allowsSameNameSiblings()) {
            throw new ItemExistsException(
                    "Same name siblings not allowed: " + existing);
        }

        // check protected flag of old & new parent
        if (destParentNode.getDefinition().isProtected()) {
            String msg = destAbsPath + ": cannot add a child node to a protected node";
            log.debug(msg);
            throw new ConstraintViolationException(msg);
        }
        if (srcParentNode.getDefinition().isProtected()) {
            String msg = srcAbsPath + ": cannot remove a child node from a protected node";
            log.debug(msg);
            throw new ConstraintViolationException(msg);
        }

        // check lock status
        srcParentNode.checkLock();
        destParentNode.checkLock();

        NodeId targetId = targetNode.getNodeId();
        int index = srcName.getIndex();
        if (index == 0) {
            index = 1;
        }

        if (srcParentNode.isSame(destParentNode)) {
            // do rename
            destParentNode.renameChildNode(srcName.getName(), index, targetId, destName.getName());
        } else {
            // check shareable case
            if (((NodeState) targetNode.getItemState()).isShareable()) {
                String msg = "Moving a shareable node is not supported.";
                log.debug(msg);
                throw new UnsupportedRepositoryOperationException(msg);
            }

            // do move:
            // 1. remove child node entry from old parent
            NodeState srcParentState =
                    (NodeState) srcParentNode.getOrCreateTransientItemState();
            srcParentState.removeChildNodeEntry(srcName.getName(), index);
            // 2. re-parent target node
            NodeState targetState =
                    (NodeState) targetNode.getOrCreateTransientItemState();
            targetState.setParentId(destParentNode.getNodeId());
            // 3. add child node entry to new parent
            NodeState destParentState =
                    (NodeState) destParentNode.getOrCreateTransientItemState();
            destParentState.addChildNodeEntry(destName.getName(), targetId);
        }

        // change definition of target
        targetNode.onRedefine(newTargetDef.unwrap().getId());
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

        // verify that parent node is checked-out
        if (!parent.internalIsCheckedOut()) {
            String msg = parentAbsPath + ": cannot add a child to a checked-in node";
            log.debug(msg);
            throw new VersionException(msg);
        }

        // check protected flag of parent node
        if (parent.getDefinition().isProtected()) {
            String msg = parentAbsPath + ": cannot add a child to a protected node";
            log.debug(msg);
            throw new ConstraintViolationException(msg);
        }

        // check lock status
        parent.checkLock();

        SessionImporter importer = new SessionImporter(parent, this, uuidBehavior);
        return new ImportHandler(importer, this);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLive() {
        return alive;
    }

    /**
     * Utility method that removes all registered event listeners.
     */
    private void removeRegisteredEventListeners() {
        try {
            ObservationManager manager = getWorkspace().getObservationManager();
            // Use a copy to avoid modifying the set of registered listeners
            // while iterating over it
            Collection listeners =
                IteratorUtils.toList(manager.getRegisteredEventListeners());
            Iterator iterator = listeners.iterator();
            while (iterator.hasNext()) {
                EventListener listener = (EventListener) iterator.next();
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
     * {@inheritDoc}
     */
    public synchronized void logout() {
        if (!alive) {
            // ignore
            return;
        }

        // JCR-798: Remove all registered event listeners to avoid concurrent
        // access to session internals by the event delivery or even listeners
        removeRegisteredEventListeners();

        // discard any pending changes first as those might
        // interfere with subsequent operations
        itemStateMgr.disposeAllTransientItemStates();

        // notify listeners that session is about to be closed
        notifyLoggingOut();

        // dispose session item state manager
        itemStateMgr.dispose();
        // dispose item manager
        itemMgr.dispose();
        // dispose workspace
        wsp.dispose();

        // invalidate session
        alive = false;

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
            accessMgr.close();
        } catch (Exception e) {
            log.warn("error while closing AccessManager", e);
        }

        // finally notify listeners that session has been closed
        notifyLoggedOut();
    }

    /**
     * {@inheritDoc}
     */
    public Repository getRepository() {
        return rep;
    }

    /**
     * {@inheritDoc}
     */
    public ValueFactory getValueFactory()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        if (valueFactory == null) {
            valueFactory = ValueFactoryImpl.getInstance();
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
        return (String[]) attributes.keySet().toArray(new String[attributes.size()]);
    }

    /**
     * {@inheritDoc}
     */
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
        addLockToken(lt, true);
    }

    /**
     * Internal implementation of {@link #addLockToken(String)}. Additionally
     * takes a parameter indicating whether the lock manager needs to be
     * informed.
     */
    public void addLockToken(String lt, boolean notify) {
        synchronized (lockTokens) {
            if (lockTokens.add(lt) && notify) {
                try {
                    getLockManager().lockTokenAdded(this, lt);
                } catch (RepositoryException e) {
                    log.error("Lock manager not available.", e);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public String[] getLockTokens() {
        synchronized (lockTokens) {
            String[] result = new String[lockTokens.size()];
            lockTokens.toArray(result);
            return result;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeLockToken(String lt) {
        removeLockToken(lt, true);
    }

    /**
     * Internal implementation of {@link #removeLockToken(String)}. Additionally
     * takes a parameter indicating whether the lock manager needs to be
     * informed.
     */
    public void removeLockToken(String lt, boolean notify) {
        synchronized (lockTokens) {
            if (lockTokens.remove(lt) && notify) {
                try {
                    getLockManager().lockTokenRemoved(this, lt);
                } catch (RepositoryException e) {
                    log.error("Lock manager not available.", e);
                }
            }
        }
    }

    /**
     * Return the lock manager for this session.
     * @return lock manager for this session
     */
    public LockManager getLockManager() throws RepositoryException {
        return wsp.getLockManager();
    }

    /**
     * Returns all locks owned by this session.
     *
     * @return an array of <code>Lock</code>s
     */
    public Lock[] getLocks() {
        // check sanity of this session
        //sanityCheck();
        if (!alive) {
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
     * Returns the node specified by the given identifier. Applies to both
     * referenceable and non-referenceable nodes.
     * <p/>
     * An <code>ItemNotFoundException</code> is thrown if no node with the
     * specified identifier exists. This exception is also thrown if this
     * <code>Session<code> does not have read access to the node with the
     * specified identifier.
     * <p/>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param id An identifier.
     * @return A <code>Node</code>.
     * @throws ItemNotFoundException if the specified identifier is not found.
     * @throws RepositoryException if another error occurs.
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
     * Returns the node at the specified absolute path in the workspace.
     * If no node exists, then a <code>PathNotFoundException</code> is thrown.
     *
     * @param absPath An absolute path.
     * @return the specified <code>Node</code>.
     * @throws PathNotFoundException If no node exists.
     * @throws RepositoryException If another error occurs.
     * @since JCR 2.0
     */
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
     * Returns the property at the specified absolute path in the workspace.
     * If no property exists, then a <code>PathNotFoundException</code> is thrown.
     *
     * @param absPath An absolute path.
     * @return the specified <code>Property</code>.
     * @throws PathNotFoundException If no property exists.
     * @throws RepositoryException if another error occurs.
     * @since JCR 2.0
     */
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
     * Returns <code>true</code> if a node exists at <code>absPath</code>
     * and this <code>Session</code> has read access to it; otherwise returns
     * <code>false</code>.
     * <p/>
     * Throws a <code>RepositoryException</code> if <code>absPath</code>
     * is not a well-formed absolute path.
     *
     * @param absPath An absolute path.
     * @return a <code>boolean</code>
     * @throws RepositoryException if <code>absPath</code> is not a well-formed
     *         absolute path.
     * @since JCR 2.0
     */
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
     * Returns <code>true</code> if a property exists at <code>absPath</code>
     * and this <code>Session</code> has read access to it; otherwise returns
     * <code>false</code>.
     * <p/>
     * Throws a <code>RepositoryException</code> if <code>absPath</code>
     * is not a well-formed absolute path.
     *
     * @param absPath An absolute path.
     * @return a <code>boolean</code>
     * @throws RepositoryException if <code>absPath</code> is not a well-formed
     *         absolute path.
     * @since JCR 2.0
     */
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
     * @see Session#hasPermission(String, String)
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

        Set s = new HashSet(Arrays.asList(actions.split(",")));
        int permissions = 0;
        if (s.remove(SessionImpl.READ_ACTION)) {
            permissions |= Permission.READ;
        }
        if (s.remove(SessionImpl.ADD_NODE_ACTION)) {
            permissions |= Permission.ADD_NODE;
        }
        if (s.remove(SessionImpl.SET_PROPERTY_ACTION)) {
            permissions |= Permission.SET_PROPERTY;
        }
        if (s.remove(SessionImpl.REMOVE_ACTION)) {
            if (nodeExists(absPath)) {
                permissions |= (propertyExists(absPath)) ?
                        (Permission.REMOVE_NODE | Permission.REMOVE_PROPERTY) :
                        Permission.REMOVE_NODE;
            } else if (propertyExists(absPath)) {
                permissions |= Permission.REMOVE_PROPERTY;
            } else {
                // item does exist -> check both permissions
                permissions = Permission.REMOVE_NODE | Permission.REMOVE_PROPERTY;
            }
        }
        if (!s.isEmpty()) {
            StringBuffer sb = new StringBuffer();
            for (Iterator it = s.iterator(); it.hasNext();) {
                sb.append(it.next());
            }
            throw new IllegalArgumentException("Unknown actions: " + sb.toString());
        }
        try {
            return getAccessManager().isGranted(path, permissions);
        } catch (AccessDeniedException e) {
            return false;
        }
    }

    /**
     * @see Session#getAccessControlManager()
     * @since JCR 2.0
     */
    public AccessControlManager getAccessControlManager()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        if (accessMgr instanceof AccessControlManager) {
            return (AccessControlManager) accessMgr;
        } else {
            throw new UnsupportedRepositoryOperationException("Access control discovery is not supported.");
        }
    }

    /**
     * @see Session#getRetentionManager()
     * @since JCR 2.0
     */
    public synchronized RetentionManager getRetentionManager()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        if (retentionManager == null) {
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
        itemMgr.dump(ps);
        ps.println();
        itemStateMgr.dump(ps);
    }

}
