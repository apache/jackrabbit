/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import org.apache.commons.collections.BeanMap;
import org.apache.commons.collections.ReferenceMap;
import org.apache.jackrabbit.core.config.AccessManagerConfig;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.nodetype.NodeDefId;
import org.apache.jackrabbit.core.nodetype.NodeDefImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.observation.EventStateCollection;
import org.apache.jackrabbit.core.security.AMContext;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.SessionItemStateManager;
import org.apache.jackrabbit.core.state.UpdatableItemStateManager;
import org.apache.jackrabbit.core.version.VersionManager;
import org.apache.jackrabbit.core.xml.DocViewSAXEventGenerator;
import org.apache.jackrabbit.core.xml.ImportHandler;
import org.apache.jackrabbit.core.xml.Importer;
import org.apache.jackrabbit.core.xml.SessionImporter;
import org.apache.jackrabbit.core.xml.SysViewSAXEventGenerator;
import org.apache.log4j.Logger;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.NamespaceException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.AccessControlException;
import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A <code>SessionImpl</code> ...
 */
public class SessionImpl implements Session, Constants {

    private static Logger log = Logger.getLogger(SessionImpl.class);

    /**
     * prededfined action constants in checkPermission
     */
    public static final String READ_ACTION = "read";
    public static final String REMOVE_ACTION = "remove";
    public static final String ADD_NODE_ACTION = "add_node";
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
     * the LoginContext of this session (can be null if this
     * session was not instantiated through a login process)
     */
    protected LoginContext loginContext;

    /**
     * the Subject of this session
     */
    protected final Subject subject;

    /**
     * the user ID that was used to acquire this session
     */
    protected final String userId;

    /**
     * the attibutes of this session
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
     * the transient prefix/namespace mappings with session scope
     */
    protected final LocalNamespaceMappings nsMappings;

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
    protected SessionImpl(RepositoryImpl rep, LoginContext loginContext,
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
        Set principals = subject.getPrincipals();
        if (principals.isEmpty()) {
            String msg = "unable to instantiate Session: no principals found";
            log.error(msg);
            throw new RepositoryException(msg);
        } else {
            // use 1st principal in case there are more that one
            Principal principal = (Principal) principals.iterator().next();
            userId = principal.getName();
        }
        this.subject = subject;
        nsMappings = new LocalNamespaceMappings(rep.getNamespaceRegistry());
        ntMgr = new NodeTypeManagerImpl(rep.getNodeTypeRegistry(), getNamespaceResolver());
        String wspName = wspConfig.getName();
        wsp = new WorkspaceImpl(wspConfig, rep.getWorkspaceStateManager(wspName),
                rep, this);
        itemStateMgr = createSessionItemStateManager(wsp.getItemStateManager());
        hierMgr = itemStateMgr.getHierarchyMgr();
        itemMgr = createItemManager(itemStateMgr, hierMgr);
        accessMgr = createAccessManager(subject, hierMgr);
        versionMgr = rep.getVersionManager();
    }

    /**
     * Create the session item state manager.
     *
     * @return session item state manager
     */
    protected SessionItemStateManager createSessionItemStateManager(UpdatableItemStateManager manager) {
        return new SessionItemStateManager(rep.getRootNodeUUID(),
                manager, getNamespaceResolver());
    }

    /**
     * Create the item manager.
     *
     * @return item manager
     */
    protected ItemManager createItemManager(SessionItemStateManager itemStateMgr,
                                            HierarchyManager hierMgr) {
        return new ItemManager(itemStateMgr, hierMgr, this,
                ntMgr.getRootNodeDefinition(), rep.getRootNodeUUID());
    }

    /**
     * Create the access manager.
     *
     * @return access manager
     * @throws AccessDeniedException if the current subject is not granted access
     *                               to the current workspace
     * @throws RepositoryException   if the access manager cannot be instantiated
     */
    protected AccessManager createAccessManager(Subject subject,
                                                HierarchyManager hierMgr)
            throws AccessDeniedException, RepositoryException {
        AccessManagerConfig amConfig = rep.getConfig().getAccessManagerConfig();
        String className = amConfig.getClassName();
        Map params = amConfig.getParameters();
        try {
            Class c = Class.forName(className);
            AccessManager accessMgr = (AccessManager) c.newInstance();
            /**
             * set the properties of the access manager object
             * from the param map
             */
            BeanMap bm = new BeanMap(accessMgr);
            Iterator iter = params.keySet().iterator();
            while (iter.hasNext()) {
                Object name = iter.next();
                Object value = params.get(name);
                bm.put(name, value);
            }
            AMContext ctx = new AMContext(new File(rep.getConfig().getHomeDir()),
                    rep.getConfig().getFileSystem(),
                    subject,
                    hierMgr,
                    wsp.getName());
            accessMgr.init(ctx);
            return accessMgr;
        } catch (AccessDeniedException ade) {
            // re-throw
            throw ade;
        } catch (Exception e) {
            // wrap in RepositoryException
            String msg = "failed to instantiate AccessManager implementation: " + className;
            log.error(msg, e);
            throw new RepositoryException(msg, e);
        }
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
    Subject getSubject() {
        return subject;
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
     * Returns the <code>NamespaceResolver</code> of this session.
     *
     * @return the <code>NamespaceResolver</code> of this session
     */
    public NamespaceResolver getNamespaceResolver() {
        return nsMappings;
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
    protected HierarchyManager getHierarchyManager() {
        return hierMgr;
    }

    /**
     * Returns the <code>VersionManager</code> associated with this session.
     *
     * @return the <code>VersionManager</code> associated with this session
     */
    protected VersionManager getVersionManager() {
        return versionMgr;
    }

    /**
     * Dispatch events belonging to a save operation.
     *
     * @param events events to dispatch as result of a successful save
     *               operation
     */
    protected void dispatch(EventStateCollection events) {
        events.dispatch();
    }

    /**
     * Dumps the state of this <code>Session</code> instance
     * (used for diagnostic purposes).
     *
     * @param ps
     * @throws RepositoryException
     */
    public void dump(PrintStream ps) throws RepositoryException {
        ps.println("Session: " + (userId == null ? "unknown" : userId) + " (" + this + ")");
        ps.println();
        itemMgr.dump(ps);
        ps.println();
        itemStateMgr.dump(ps);
    }

    /**
     * Returns the names of all workspaces of this repository with respect of the
     * access rights of this session.
     *
     * @return the names of all accessible workspaces
     */
    protected String[] getWorkspaceNames() {
        // @todo filter workspace names based on credentials of this session
        return rep.getWorkspaceNames();
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
     * Notify the listeners that this session is about to be closed.
     */
    protected void notifyLoggingOut() {
        // copy listeners to array to avoid ConcurrentModificationException
        SessionListener[] la = new SessionListener[listeners.size()];
        Iterator iter = listeners.values().iterator();
        int cnt = 0;
        while (iter.hasNext()) {
            la[cnt++] = (SessionListener) iter.next();
        }
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
        SessionListener[] la = new SessionListener[listeners.size()];
        Iterator iter = listeners.values().iterator();
        int cnt = 0;
        while (iter.hasNext()) {
            la[cnt++] = (SessionListener) iter.next();
        }
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

    //--------------------------------------------------------------< Session >
    /**
     * {@inheritDoc}
     */
    public void checkPermission(String absPath, String actions)
            throws AccessControlException {
        // check sanity of this session
        try {
            sanityCheck();
        } catch (RepositoryException re) {
            String msg = "failed to check permissions on " + absPath;
            log.warn(msg, re);
            throw new AccessControlException(actions);
        }

        // build the set of actions to be checked
        String[] strings = actions.split(",");
        HashSet set = new HashSet();
        for (int i = 0; i < strings.length; i++) {
            set.add(strings[i]);
        }

        Path targetPath = null;
        ItemId targetId = null;

        /**
         * "read" action:
         * requires READ permission on target item
         */
        if (set.contains(READ_ACTION)) {
            try {
                targetPath = Path.create(absPath, getNamespaceResolver(), true);
                targetId = hierMgr.resolvePath(targetPath);
                accessMgr.checkPermission(targetId, AccessManager.READ);
            } catch (PathNotFoundException pnfe) {
                // target does not exist, throw exception
                throw new AccessControlException(READ_ACTION);
            } catch (MalformedPathException mpe) {
                String msg = "invalid path: " + absPath;
                log.warn(msg, mpe);
                throw new AccessControlException(READ_ACTION);
            } catch (RepositoryException re) {
                String msg = "failed to check READ permission on " + absPath;
                log.warn(msg, re);
                throw new AccessControlException(READ_ACTION);
            }
        }

        Path parentPath = null;
        ItemId parentId = null;

        /**
         * "add_node" action:
         * requires WRITE permission on parent item
         */
        if (set.contains(ADD_NODE_ACTION)) {
            try {
                if (targetPath == null) {
                    targetPath = Path.create(absPath, getNamespaceResolver(), true);
                }
                parentPath = targetPath.getAncestor(1);
                parentId = hierMgr.resolvePath(parentPath);
                accessMgr.checkPermission(parentId, AccessManager.WRITE);
            } catch (PathNotFoundException pnfe) {
                // parent does not exist (i.e. / was specified), throw exception
                throw new AccessControlException(ADD_NODE_ACTION);
            } catch (MalformedPathException mpe) {
                String msg = "invalid path: " + absPath;
                log.warn(msg, mpe);
                throw new AccessControlException(ADD_NODE_ACTION);
            } catch (AccessDeniedException re) {
                // otherwise the RepositoryException catch clause will
                // log a warn message, which is not appropriate in this case.
                throw new AccessControlException(ADD_NODE_ACTION);
            } catch (RepositoryException re) {
                String msg = "failed to check WRITE permission on parent of " + absPath;
                log.warn(msg, re);
                throw new AccessControlException(ADD_NODE_ACTION);
            }
        }

        /**
         * "remove" action:
         * requires REMOVE permission on target item
         */
        if (set.contains(REMOVE_ACTION)) {
            try {
                if (targetPath == null) {
                    targetPath = Path.create(absPath, getNamespaceResolver(), true);
                }
                if (targetId == null) {
                    targetId = hierMgr.resolvePath(targetPath);
                }
                accessMgr.checkPermission(targetId, AccessManager.REMOVE);
            } catch (PathNotFoundException pnfe) {
                // parent does not exist, throw exception
                throw new AccessControlException(REMOVE_ACTION);
            } catch (MalformedPathException mpe) {
                String msg = "invalid path: " + absPath;
                log.warn(msg, mpe);
                throw new AccessControlException(REMOVE_ACTION);
            } catch (AccessDeniedException re) {
                // otherwise the RepositoryException catch clause will
                // log a warn message, which is not appropriate in this case.
                throw new AccessControlException(REMOVE_ACTION);
            } catch (RepositoryException re) {
                String msg = "failed to check REMOVE permission on " + absPath;
                log.warn(msg, re);
                throw new AccessControlException(REMOVE_ACTION);
            }
        }

        /**
         * "set_property" action:
         * requires WRITE permission on parent item if property is going to be
         * added or WRITE permission on target item if property is going to be
         * modified
         */
        if (set.contains(SET_PROPERTY_ACTION)) {
            try {
                if (targetPath == null) {
                    targetPath = Path.create(absPath, getNamespaceResolver(), true);
                }
                if (targetId == null) {
                    try {
                        targetId = hierMgr.resolvePath(targetPath);
                        // property does already exist,
                        // check WRITE permission on target
                        accessMgr.checkPermission(targetId, AccessManager.WRITE);
                    } catch (PathNotFoundException pnfe) {
                        // property does not exist yet,
                        // check WRITE permission on parent
                        if (parentPath == null) {
                            parentPath = targetPath.getAncestor(1);
                        }
                        if (parentId == null) {
                            parentId = hierMgr.resolvePath(parentPath);
                        }
                        accessMgr.checkPermission(parentId, AccessManager.WRITE);
                    }
                }
            } catch (PathNotFoundException pnfe) {
                // parent does not exist, throw exception
                throw new AccessControlException(SET_PROPERTY_ACTION);
            } catch (MalformedPathException mpe) {
                String msg = "invalid path: " + absPath;
                log.warn(msg, mpe);
                throw new AccessControlException(SET_PROPERTY_ACTION);
            } catch (AccessDeniedException re) {
                // otherwise the RepositoryException catch clause will
                // log a warn message, which is not appropriate in this case.
                throw new AccessControlException(SET_PROPERTY_ACTION);
            } catch (RepositoryException re) {
                String msg = "failed to check WRITE permission on parent of " + absPath;
                log.warn(msg, re);
                throw new AccessControlException(SET_PROPERTY_ACTION);
            }
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

        // @todo reimplement impersonate(Credentials) correctly

        // check if the credentials of this session allow to 'impersonate'
        // the user represented by tha supplied credentials

        // FIXME: the original purpose of this method is to enable
        // a 'superuser' to impersonate another user without needing
        // to know its password.
        try {
            return rep.login(otherCredentials, getWorkspace().getName());
        } catch (NoSuchWorkspaceException nswe) {
            // should never get here...
            String msg = "impersonate failed";
            log.debug(msg);
            throw new LoginException(msg, nswe);
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
        // check sanity of this session
        sanityCheck();

        try {
            NodeImpl node = (NodeImpl) getItemManager().getItem(new NodeId(uuid));
            if (node.isNodeType(MIX_REFERENCEABLE)) {
                return node;
            } else {
                // there is a node with that uuid but the node does not expose it
                throw new ItemNotFoundException(uuid);
            }
        } catch (AccessDeniedException ade) {
            throw new ItemNotFoundException(uuid);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Item getItem(String absPath) throws PathNotFoundException, RepositoryException {
        // check sanity of this session
        sanityCheck();

        try {
            return getItemManager().getItem(Path.create(absPath, getNamespaceResolver(), true));
        } catch (AccessDeniedException ade) {
            throw new PathNotFoundException(absPath);
        } catch (MalformedPathException mpe) {
            String msg = "invalid path:" + absPath;
            log.debug(msg);
            throw new RepositoryException(msg, mpe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean itemExists(String absPath) {
        try {
            // check sanity of this session
            sanityCheck();

            getItemManager().getItem(Path.create(absPath, getNamespaceResolver(), true));
            return true;
        } catch (RepositoryException re) {
            // fall through...
        } catch (MalformedPathException mpe) {
            // fall through...
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void save()
            throws AccessDeniedException, ConstraintViolationException,
            InvalidItemStateException, VersionException, LockException,
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
            VersionException, LockException, RepositoryException {
        // check sanity of this session
        sanityCheck();

        // check paths & get node instances

        Path srcPath;
        Path.PathElement srcName;
        Path srcParentPath;
        NodeImpl targetNode;
        NodeImpl srcParentNode;
        try {
            srcPath = Path.create(srcAbsPath, getNamespaceResolver(), true);
            srcName = srcPath.getNameElement();
            srcParentPath = srcPath.getAncestor(1);
            ItemImpl item = getItemManager().getItem(srcPath);
            if (!item.isNode()) {
                throw new PathNotFoundException(srcAbsPath);
            }
            targetNode = (NodeImpl) item;
            srcParentNode = (NodeImpl) getItemManager().getItem(srcParentPath);
        } catch (AccessDeniedException ade) {
            throw new PathNotFoundException(srcAbsPath);
        } catch (MalformedPathException mpe) {
            String msg = srcAbsPath + ": invalid path";
            log.debug(msg);
            throw new RepositoryException(msg, mpe);
        }

        Path destPath;
        Path.PathElement destName;
        Path destParentPath;
        NodeImpl destParentNode;
        try {
            destPath = Path.create(destAbsPath, getNamespaceResolver(), true);
            if (srcPath.isAncestorOf(destPath)) {
                String msg = destAbsPath + ": invalid destination path (cannot be descendant of source path)";
                log.debug(msg);
                throw new RepositoryException(msg);
            }
            destName = destPath.getNameElement();
            destParentPath = destPath.getAncestor(1);
            destParentNode = (NodeImpl) getItemManager().getItem(destParentPath);
        } catch (AccessDeniedException ade) {
            throw new PathNotFoundException(destAbsPath);
        } catch (MalformedPathException mpe) {
            String msg = destAbsPath + ": invalid path";
            log.debug(msg);
            throw new RepositoryException(msg, mpe);
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

        try {
            ItemImpl item = getItemManager().getItem(destPath);
            if (!item.isNode()) {
                // there's already a property with that name
                throw new ItemExistsException(item.safeGetJCRPath());
            } else {
                // there's already a node with that name
                // check same-name sibling setting of both new and existing node
                if (!destParentNode.getDefinition().allowSameNameSibs()
                        || !((NodeImpl) item).getDefinition().allowSameNameSibs()) {
                    throw new ItemExistsException(item.safeGetJCRPath());
                }
            }
        } catch (AccessDeniedException ade) {
            // FIXME by throwing ItemExistsException we're disclosing too much information
            throw new ItemExistsException(destAbsPath);
        } catch (PathNotFoundException pnfe) {
            // no name collision
        }

        // check constraints

        // get applicable definition of target node at new location
        NodeTypeImpl nt = (NodeTypeImpl) targetNode.getPrimaryNodeType();
        NodeDefImpl newTargetDef;
        try {
            newTargetDef = destParentNode.getApplicableChildNodeDef(destName.getName(), nt.getQName());
        } catch (RepositoryException re) {
            String msg = destAbsPath + ": no definition found in parent node's node type for new node";
            log.debug(msg);
            throw new ConstraintViolationException(msg, re);
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

        // do move operation

        String targetUUID = ((NodeState) targetNode.getItemState()).getUUID();
        // add target to new parent
        destParentNode.createChildNodeLink(destName.getName(), targetUUID);
        // remove target from old parent
        srcParentNode.removeChildNode(srcName.getName(),
                srcName.getIndex() == 0 ? 1 : srcName.getIndex());
        // change definition of target if necessary
        NodeDefImpl oldTargetDef = (NodeDefImpl) targetNode.getDefinition();
        NodeDefId oldTargetDefId = new NodeDefId(oldTargetDef.unwrap());
        NodeDefId newTargetDefId = new NodeDefId(newTargetDef.unwrap());
        if (!oldTargetDefId.equals(newTargetDefId)) {
            targetNode.onRedefine(newTargetDefId);
        }
    }

    /**
     * {@inheritDoc}
     */
    public ContentHandler getImportContentHandler(String parentAbsPath)
            throws PathNotFoundException, ConstraintViolationException,
            VersionException, LockException, RepositoryException {
        // check sanity of this session
        sanityCheck();

        Item item;
        try {
            item = getItemManager().getItem(Path.create(parentAbsPath, getNamespaceResolver(), true));
        } catch (MalformedPathException mpe) {
            String msg = parentAbsPath + ": invalid path";
            log.debug(msg);
            throw new RepositoryException(msg, mpe);
        } catch (AccessDeniedException ade) {
            throw new PathNotFoundException(parentAbsPath);
        }
        if (!item.isNode()) {
            throw new PathNotFoundException(parentAbsPath);
        }
        NodeImpl parent = (NodeImpl) item;

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

        SessionImporter importer = new SessionImporter(parent, this, Importer.IMPORT_UUID_CREATE_NEW);
        return new ImportHandler(importer, getNamespaceResolver(), rep.getNamespaceRegistry());
    }

    /**
     * {@inheritDoc}
     */
    public void importXML(String parentAbsPath, InputStream in)
            throws IOException, PathNotFoundException, ItemExistsException,
            ConstraintViolationException, VersionException,
            InvalidSerializedDataException, LockException, RepositoryException {
        // check sanity of this session
        sanityCheck();

        ImportHandler handler = (ImportHandler) getImportContentHandler(parentAbsPath);
        try {
            XMLReader parser = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
            parser.setContentHandler(handler);
            parser.setErrorHandler(handler);
            parser.parse(new InputSource(in));
        } catch (SAXException se) {
            // check for wrapped repository exception
            Exception e = se.getException();
            if (e != null && e instanceof RepositoryException) {
                throw (RepositoryException) e;
            } else {
                String msg = "failed to parse XML stream";
                log.debug(msg);
                throw new InvalidSerializedDataException(msg, se);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void exportDocView(String absPath, ContentHandler contentHandler,
                              boolean skipBinary, boolean noRecurse)
            throws InvalidSerializedDataException, PathNotFoundException,
            SAXException, RepositoryException {
        // check sanity of this session
        sanityCheck();

        Item item = getItem(absPath);
        if (!item.isNode()) {
            // there's a property, though not a node at the specified path
            throw new PathNotFoundException(absPath);
        }
        new DocViewSAXEventGenerator((NodeImpl) item, noRecurse, skipBinary,
                this, contentHandler).serialize();
    }

    /**
     * {@inheritDoc}
     */
    public void exportDocView(String absPath, OutputStream out,
                              boolean skipBinary, boolean noRecurse)
            throws InvalidSerializedDataException, IOException,
            PathNotFoundException, RepositoryException {
        boolean indenting = true;
        OutputFormat format = new OutputFormat("xml", "UTF-8", indenting);
        XMLSerializer serializer = new XMLSerializer(out, format);
        try {
            exportDocView(absPath, serializer.asContentHandler(), skipBinary, noRecurse);
        } catch (SAXException se) {
            throw new RepositoryException(se);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void exportSysView(String absPath, ContentHandler contentHandler,
                              boolean skipBinary, boolean noRecurse)
            throws PathNotFoundException, SAXException, RepositoryException {
        // check sanity of this session
        sanityCheck();

        Item item = getItem(absPath);
        if (!item.isNode()) {
            // there's a property, though not a node at the specified path
            throw new PathNotFoundException(absPath);
        }
        new SysViewSAXEventGenerator((NodeImpl) item, noRecurse, skipBinary,
                this, contentHandler).serialize();
    }

    /**
     * {@inheritDoc}
     */
    public void exportSysView(String absPath, OutputStream out,
                              boolean skipBinary, boolean noRecurse)
            throws IOException, PathNotFoundException, RepositoryException {
        boolean indenting = true;
        OutputFormat format = new OutputFormat("xml", "UTF-8", indenting);
        XMLSerializer serializer = new XMLSerializer(out, format);
        try {
            exportSysView(absPath, serializer.asContentHandler(), skipBinary, noRecurse);
        } catch (SAXException se) {
            throw new RepositoryException(se);
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

        // notify listeners that session is about to be closed
        notifyLoggingOut();

        // discard all transient changes
        itemStateMgr.disposeAllTransientItemStates();
        // dispose item manager
        itemMgr.dispose();
        // dispose workspace
        wsp.dispose();

        // @todo release session-scoped locks, free resources, prepare to get gc'ed etc.

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
    public String getUserId() {
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
        nsMappings.setNamespacePrefix(prefix, uri);
    }

    /**
     * {@inheritDoc}
     */
    public String[] getNamespacePrefixes()
            throws NamespaceException, RepositoryException {
        return nsMappings.getPrefixes();
    }

    /**
     * {@inheritDoc}
     */
    public String getNamespaceURI(String prefix)
            throws NamespaceException, RepositoryException {
        return nsMappings.getURI(prefix);
    }

    /**
     * {@inheritDoc}
     */
    public String getNamespacePrefix(String uri)
            throws NamespaceException, RepositoryException {
        return nsMappings.getPrefix(uri);
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
                    wsp.getLockManager().lockTokenAdded(this, lt);
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
                    wsp.getLockManager().lockTokenRemoved(this, lt);
                } catch (RepositoryException e) {
                    log.error("Lock manager not available.", e);
                }
            }
        }
    }
}
