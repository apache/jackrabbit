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

import org.apache.commons.collections.ReferenceMap;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.nodetype.*;
import org.apache.jackrabbit.core.observation.EventStateCollection;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.SessionItemStateManager;
import org.apache.jackrabbit.core.version.VersionManager;
import org.apache.jackrabbit.core.xml.ImportHandler;
import org.apache.log4j.Logger;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.apache.xerces.util.XMLChar;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.jcr.*;
import javax.jcr.version.VersionException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.OutputStream;
import java.security.AccessControlException;
import java.util.*;

/**
 * A <code>SessionImpl</code> ...
 */
public class SessionImpl implements Session {

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
    protected AccessManagerImpl accessMgr;

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
    protected final TransientNamespaceMappings nsMappings;

    /**
     * The version manager for this session
     */
    protected final VersionManager versionMgr;

    /**
     * Listeners (weak references)
     */
    protected final Map listeners = new ReferenceMap(ReferenceMap.WEAK, ReferenceMap.WEAK);

    /**
     * Protected constructor.
     *
     * @param rep
     * @param credentials
     * @param wspConfig
     */
    protected SessionImpl(RepositoryImpl rep, Credentials credentials,
                          WorkspaceConfig wspConfig)
            throws RepositoryException {
        alive = true;
        this.rep = rep;
        if (credentials instanceof SimpleCredentials) {
            SimpleCredentials sc = (SimpleCredentials) credentials;
            // clear password for security reasons
            char[] pwd = sc.getPassword();
            if (pwd != null) {
                for (int i = 0; i < pwd.length; i++) {
                    pwd[i] = 0;
                }
            }
            userId = sc.getUserId();
            String[] names = sc.getAttributeNames();
            for (int i = 0; i < names.length; i++) {
                attributes.put(names[i], sc.getAttribute(names[i]));
            }
        } else {
            userId = null;
        }
        nsMappings = new TransientNamespaceMappings(rep.getNamespaceRegistry());
        ntMgr = new NodeTypeManagerImpl(rep.getNodeTypeRegistry(), getNamespaceResolver());
        String wspName = wspConfig.getName();
        wsp = new WorkspaceImpl(wspConfig, rep.getWorkspaceStateManager(wspName),
                rep, this);
        itemStateMgr = createSessionItemStateManager(wsp.getItemStateManager());
        hierMgr = itemStateMgr.getHierarchyMgr();
        itemMgr = createItemManager(itemStateMgr, hierMgr);
        accessMgr = createAccessManager(credentials, hierMgr);
        versionMgr = rep.getVersionManager();

        // add virtual item managers only for normal sessions
        if (!(this instanceof SystemSession)) {
            try {
                itemStateMgr.addVirtualItemStateProvider(versionMgr.getVirtualItemStateProvider(this, itemStateMgr));
            } catch (Exception e) {
                log.error("Unable to add vmgr: " + e.toString(), e);
            }
        }
    }

    /**
     * Protected constructor.
     *
     * @param rep
     * @param userId
     * @param wspConfig
     */
    protected SessionImpl(RepositoryImpl rep, String userId, WorkspaceConfig wspConfig)
            throws RepositoryException {
        alive = true;
        this.rep = rep;
        this.userId = userId;
        nsMappings = new TransientNamespaceMappings(rep.getNamespaceRegistry());
        ntMgr = new NodeTypeManagerImpl(rep.getNodeTypeRegistry(), getNamespaceResolver());
        String wspName = wspConfig.getName();
        wsp = new WorkspaceImpl(wspConfig, rep.getWorkspaceStateManager(wspName),
                rep, this);
        itemStateMgr = createSessionItemStateManager(wsp.getItemStateManager());
        hierMgr = itemStateMgr.getHierarchyMgr();
        itemMgr = createItemManager(itemStateMgr, hierMgr);
        versionMgr = rep.getVersionManager();

        // add virtual item managers only for normal sessions
        if (!(this instanceof SystemSession)) {
            try {
                itemStateMgr.addVirtualItemStateProvider(versionMgr.getVirtualItemStateProvider(this, itemStateMgr));
            } catch (Exception e) {
                log.error("Unable to add vmgr: " + e.toString(), e);
            }
        }
    }

    /**
     * Create the session item state manager.
     *
     * @return session item state manager
     */
    protected SessionItemStateManager createSessionItemStateManager(ItemStateManager manager) {
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
     */
    protected AccessManagerImpl createAccessManager(Credentials credentials,
                                                    HierarchyManager hierMgr) {
        return new AccessManagerImpl(credentials, hierMgr);
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
     * Returns the <code>AccessManager</code> associated with this session.
     *
     * @return the <code>AccessManager</code> associated with this session
     */
    public AccessManagerImpl getAccessManager() {
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
    protected ItemManager getItemManager() {
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
     * @throws RepositoryException if a workspace with the given name
     *                             already exists or if another error occurs
     */
    protected void createWorkspace(String workspaceName)
            throws AccessDeniedException, RepositoryException {
        // @todo verify that this session has the right privileges for this operation
        rep.createWorkspace(workspaceName);
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
     * @see Session#checkPermission(String, String)
     */
    public void checkPermission(String absPath, String actions)
            throws AccessControlException {
        // check sanity of this session
        try {
            sanityCheck();
        } catch (RepositoryException re) {
            String msg = "failed to check READ permission on " + absPath;
            log.warn(msg, re);
            throw new AccessControlException(READ_ACTION);
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
                // parent does not exist, throw exception
                throw new AccessControlException(ADD_NODE_ACTION);
            } catch (MalformedPathException mpe) {
                String msg = "invalid path: " + absPath;
                log.warn(msg, mpe);
                throw new AccessControlException(ADD_NODE_ACTION);
            } catch (RepositoryException re) {
                String msg = "failed to check WRITE permission on parent of " + absPath;
                log.warn(msg, re);
                throw new AccessControlException(ADD_NODE_ACTION);
            }
        }

        /**
         * "remove" action:
         * requires WRITE permission on parent item
         */
        if (set.contains(REMOVE_ACTION)) {
            try {
                if (targetPath == null) {
                    targetPath = Path.create(absPath, getNamespaceResolver(), true);
                }
                if (parentPath == null) {
                    parentPath = targetPath.getAncestor(1);
                }
                if (parentId == null) {
                    parentId = hierMgr.resolvePath(parentPath);
                }
                accessMgr.checkPermission(parentId, AccessManager.WRITE);
            } catch (PathNotFoundException pnfe) {
                // parent does not exist, throw exception
                throw new AccessControlException(REMOVE_ACTION);
            } catch (MalformedPathException mpe) {
                String msg = "invalid path: " + absPath;
                log.warn(msg, mpe);
                throw new AccessControlException(REMOVE_ACTION);
            } catch (RepositoryException re) {
                String msg = "failed to check WRITE permission on parent of " + absPath;
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
            } catch (RepositoryException re) {
                String msg = "failed to check WRITE permission on parent of " + absPath;
                log.warn(msg, re);
                throw new AccessControlException(SET_PROPERTY_ACTION);
            }
        }
    }

    /**
     * @see Session#getWorkspace()
     */
    public Workspace getWorkspace() {
        return wsp;
    }

    /**
     * @see Session#impersonate(Credentials)
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
            log.error(msg, nswe);
            throw new LoginException(msg, nswe);
        }
    }

    /**
     * @see Session#getRootNode
     */
    public Node getRootNode() throws RepositoryException {
        // check sanity of this session
        sanityCheck();

        return getItemManager().getRootNode();
    }

    /**
     * @see Session#getNodeByUUID(String)
     */
    public Node getNodeByUUID(String uuid) throws ItemNotFoundException, RepositoryException {
        // check sanity of this session
        sanityCheck();

        try {
            NodeImpl node = (NodeImpl) getItemManager().getItem(new NodeId(uuid));
            if (node.isNodeType(NodeTypeRegistry.MIX_REFERENCEABLE)) {
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
     * @see Session#getItem(String)
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
            log.error(msg);
            throw new RepositoryException(msg, mpe);
        }
    }

    /**
     * @see Session#itemExists(String)
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
     * @see Session#save
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
     * @see Session#refresh(boolean)
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
     * @see Session#hasPendingChanges
     */
    public boolean hasPendingChanges() throws RepositoryException {
        // check sanity of this session
        sanityCheck();

        return itemStateMgr.hasAnyTransientItemStates();
    }

    /**
     * @see Session#move(String, String)
     */
    public void move(String srcAbsPath, String destAbsPath)
            throws ItemExistsException, PathNotFoundException,
            VersionException, RepositoryException {
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
            log.error(msg, mpe);
            throw new RepositoryException(msg, mpe);
        }

        Path destPath;
        Path.PathElement destName;
        Path destParentPath;
        NodeImpl destParentNode;
        try {
            destPath = Path.create(destAbsPath, getNamespaceResolver(), true);
            destName = destPath.getNameElement();
            destParentPath = destPath.getAncestor(1);
            destParentNode = (NodeImpl) getItemManager().getItem(destParentPath);
        } catch (AccessDeniedException ade) {
            throw new PathNotFoundException(destAbsPath);
        } catch (MalformedPathException mpe) {
            String msg = destAbsPath + ": invalid path";
            log.error(msg, mpe);
            throw new RepositoryException(msg, mpe);
        }
        int ind = destName.getIndex();
        if (ind > 0) {
            // subscript in name element
            String msg = destAbsPath + ": invalid destination path (subscript in name element is not allowed)";
            log.error(msg);
            throw new RepositoryException(msg);
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
            log.error(msg, re);
            throw new ConstraintViolationException(msg, re);
        }
        // check protected flag of old & new parent
        if (destParentNode.getDefinition().isProtected()) {
            String msg = destAbsPath + ": cannot add a child node to a protected node";
            log.error(msg);
            throw new ConstraintViolationException(msg);
        }
        if (srcParentNode.getDefinition().isProtected()) {
            String msg = srcAbsPath + ": cannot remove a child node from a protected node";
            log.error(msg);
            throw new ConstraintViolationException(msg);
        }

        // do move operation

        String targetUUID = ((NodeState) targetNode.getItemState()).getUUID();
        // add target to new parent
        destParentNode.createChildNodeLink(destName.getName(), targetUUID);
        // remove target from old parent
        srcParentNode.removeChildNode(srcName.getName(), srcName.getIndex() == 0 ? 1 : srcName.getIndex());
        // change definition of target if necessary
        NodeDefImpl oldTargetDef = (NodeDefImpl) targetNode.getDefinition();
        NodeDefId oldTargetDefId = new NodeDefId(oldTargetDef.unwrap());
        NodeDefId newTargetDefId = new NodeDefId(newTargetDef.unwrap());
        if (!oldTargetDefId.equals(newTargetDefId)) {
            targetNode.onRedefine(newTargetDefId);
        }
    }

    /**
     * @see Session#getImportContentHandler(String)
     */
    public ContentHandler getImportContentHandler(String parentAbsPath)
            throws PathNotFoundException, ConstraintViolationException,
            VersionException, LockException, RepositoryException {
        // check sanity of this session
        sanityCheck();

        Item item = null;
        try {
            item = getItemManager().getItem(Path.create(parentAbsPath, getNamespaceResolver(), true));
        } catch (MalformedPathException mpe) {
            String msg = parentAbsPath + ": invalid path";
            log.error(msg, mpe);
            throw new RepositoryException(msg, mpe);
        } catch (AccessDeniedException ade) {
            throw new PathNotFoundException(parentAbsPath);
        }
        if (!item.isNode()) {
            String msg = parentAbsPath + ": node expected";
            log.error(msg);
            throw new RepositoryException(msg);
        }
        NodeImpl parent = (NodeImpl) item;

        // verify that parent node is checked-out
        if (!parent.internalIsCheckedOut()) {
            String msg = parentAbsPath + ": cannot add a child to a checked-in node";
            log.error(msg);
            throw new VersionException(msg);
        }

        // check protected flag of parent node
        if (parent.getDefinition().isProtected()) {
            String msg = parentAbsPath + ": cannot add a child to a protected node";
            log.error(msg);
            throw new ConstraintViolationException(msg);
        }

        return new ImportHandler(parent, rep.getNamespaceRegistry(), this);
    }

    /**
     * @see Session#importXML(String, InputStream)
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
                log.error(msg, se);
                throw new InvalidSerializedDataException(msg, se);
            }
        }
    }

    /**
     * @see Session#exportDocView(String, ContentHandler, boolean, boolean)
     */
    public void exportDocView(String absPath, ContentHandler contentHandler,
                              boolean skipBinary, boolean noRecurse)
            throws InvalidSerializedDataException, PathNotFoundException,
            SAXException, RepositoryException {
        // check sanity of this session
        sanityCheck();

        // @todo implement Session#exportDocView(String, ContentHandler, boolean, boolean)
        throw new RepositoryException("not yet implemented");
/*
        // check path & retrieve state
        Path path;
        Path.PathElement name;
        NodeState state;
        try {
            path = Path.create(absPath, session.getNamespaceResolver(), true);
            name = path.getNameElement();
            state = getNodeState(path, hierMgr, stateMgr);
        } catch (MalformedPathException mpe) {
            String msg = "invalid path: " + absPath;
            log.error(msg, mpe);
            throw new RepositoryException(msg, mpe);
        }

        // check read access
        if (!session.getAccessManager().isGranted(state.getId(), AccessManager.READ)) {
            throw new PathNotFoundException(absPath);
        }

        new DocViewSAXEventGenerator(state, name.getName(), noRecurse, binaryAsLink,
                stateMgr, rep.getNamespaceRegistry(),
                session.getAccessManager(), hierMgr, contentHandler).serialize();
*/
    }

    /**
     * @see Session#exportDocView(String, OutputStream, boolean, boolean)
     */
    public void exportDocView(String absPath, OutputStream out,
                              boolean skipBinary, boolean noRecurse)
            throws InvalidSerializedDataException, IOException,
            PathNotFoundException,  RepositoryException {
        OutputFormat format = new OutputFormat("xml", "UTF-8", true);
        XMLSerializer serializer = new XMLSerializer(out, format);
        try {
            exportDocView(absPath, serializer.asContentHandler(), skipBinary, noRecurse);
        } catch (SAXException se) {
            throw new RepositoryException(se);
        }
    }

    /**
     * @see Session#exportSysView(String, ContentHandler, boolean, boolean)
     */
    public void exportSysView(String absPath, ContentHandler contentHandler,
                              boolean skipBinary, boolean noRecurse)
            throws PathNotFoundException, SAXException, RepositoryException {
        // check sanity of this session
        sanityCheck();

        // @todo implement Session#exportSysView(String, ContentHandler, boolean, boolean)
        throw new RepositoryException("not yet implemented");
/*
        // check path & retrieve state
        Path path;
        Path.PathElement name;
        NodeState state;
        try {
            path = Path.create(absPath, session.getNamespaceResolver(), true);
            name = path.getNameElement();
            state = getNodeState(path, hierMgr, stateMgr);
        } catch (MalformedPathException mpe) {
            String msg = "invalid path: " + absPath;
            log.error(msg, mpe);
            throw new RepositoryException(msg, mpe);
        }

        // check read access
        if (!session.getAccessManager().isGranted(state.getId(), AccessManager.READ)) {
            throw new PathNotFoundException(absPath);
        }

        new SysViewSAXEventGenerator(state, name.getName(), noRecurse, binaryAsLink,
                stateMgr, rep.getNamespaceRegistry(),
                session.getAccessManager(), hierMgr, contentHandler).serialize();
    }
*/
    }

    /**
     * @see Session#exportSysView(String, OutputStream, boolean, boolean)
     */
    public void exportSysView(String absPath, OutputStream out,
                              boolean skipBinary, boolean noRecurse)
            throws IOException, PathNotFoundException, RepositoryException {
        OutputFormat format = new OutputFormat("xml", "UTF-8", true);
        XMLSerializer serializer = new XMLSerializer(out, format);
        try {
            exportSysView(absPath, serializer.asContentHandler(), skipBinary, noRecurse);
        } catch (SAXException se) {
            throw new RepositoryException(se);
        }
    }

    /**
     * @see Session#logout()
     */
    public synchronized void logout() {
        if (!alive) {
            // ignore
            return;
        }

        // discard all transient changes
        itemStateMgr.disposeAllTransientItemStates();
        // dispose item manager
        itemMgr.dispose();
        // dispose workspace
        wsp.dispose();

        // @todo release session-scoped locks, free resources, prepare to get gc'ed etc.

        // invalidate session
        alive = false;

        // finally notify listeners that session has been closed
        notifyLoggedOut();
    }

    /**
     * @see Session#getRepository
     */
    public Repository getRepository() {
        return rep;
    }

    /**
     * @see Session#getUserId
     */
    public String getUserId() {
        return userId;
    }

    /**
     * @see Session#getAttribute
     */
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    /**
     * @see Session#getAttributeNames
     */
    public String[] getAttributeNames() {
        return (String[]) attributes.keySet().toArray(new String[attributes.size()]);
    }

    /**
     * @see Session#setNamespacePrefix(String, String)
     */
    public void setNamespacePrefix(String prefix, String uri)
            throws NamespaceException, RepositoryException {
        nsMappings.setNamespacePrefix(prefix, uri);
    }

    /**
     * @see Session#getNamespacePrefixes
     */
    public String[] getNamespacePrefixes()
            throws NamespaceException, RepositoryException {
        return nsMappings.getPrefixes();
    }

    /**
     * @see Session#getNamespaceURI(String)
     */
    public String getNamespaceURI(String prefix)
            throws NamespaceException, RepositoryException {
        return nsMappings.getURI(prefix);
    }

    /**
     * @see Session#getNamespaceURI(String)
     */
    public String getNamespacePrefix(String uri)
            throws NamespaceException, RepositoryException {
        return nsMappings.getPrefix(uri);
    }

    //------------------------------------------------------< locking support >
    /**
     * @see Session#addLockToken(String)
     */
    public void addLockToken(String lt) {
        // @todo implement locking support
        throw new UnsupportedOperationException("Locking not implemented yet.");
    }

    /**
     * @see Session#getLockTokens()
     */
    public String[] getLockTokens() {
        // @todo implement locking support
        return new String[0];
    }

    /**
     * @see Session#removeLockToken(String)
     */
    public void removeLockToken(String lt) {
        // @todo implement locking support
        throw new UnsupportedOperationException("Locking not implemented yet.");
    }

    //--------------------------------------------------------< inner classes >
    class TransientNamespaceMappings implements NamespaceResolver {

        // the global persistent namespace registry
        private NamespaceRegistryImpl nsReg;

        // local prefix/namespace mappings
        private HashMap prefixToURI = new HashMap();
        private HashMap uriToPrefix = new HashMap();

        // prefixes in global namespace registry hidden by local mappings
        private Set hiddenPrefixes = new HashSet();

        TransientNamespaceMappings(NamespaceRegistryImpl nsReg) {
            this.nsReg = nsReg;
        }

        void setNamespacePrefix(String prefix, String uri)
                throws NamespaceException, RepositoryException {
            if (prefix == null || uri == null) {
                throw new IllegalArgumentException("prefix/uri can not be null");
            }
            if (NamespaceRegistryImpl.NS_EMPTY_PREFIX.equals(prefix)
                    || NamespaceRegistryImpl.NS_DEFAULT_URI.equals(uri)) {
                throw new NamespaceException("default namespace is reserved and can not be changed");
            }
            // special case: prefixes xml*
            if (prefix.toLowerCase().startsWith(NamespaceRegistryImpl.NS_XML_PREFIX)) {
                throw new NamespaceException("reserved prefix: " + prefix);
            }
            // check if the prefix is a valid XML prefix
            if (!XMLChar.isValidNCName(prefix)) {
                throw new NamespaceException("invalid prefix: " + prefix);
            }

            // check if namespace exists (the following call will
            // trigger a NamespaceException if it doesn't)
            String globalPrefix = nsReg.getPrefix(uri);

            // check new prefix for collision
            String globalURI = null;
            try {
                globalURI = nsReg.getURI(prefix);
            } catch (NamespaceException nse) {
                // ignore
            }
            if (globalURI != null) {
                // prefix is already mapped in global namespace registry;
                // check if it is redundant or if it refers to a namespace
                // that has been locally remapped, thus hiding it
                if (!hiddenPrefixes.contains(prefix)) {
                    if (uri.equals(globalURI) && prefix.equals(globalPrefix)) {
                        // redundant mapping, silently ignore
                        return;
                    }
                    // we don't allow to hide a namespace because we can't
                    // guarantee that there are no references to it
                    // (in names of nodes/properties/node types etc.)
                    throw new NamespaceException(prefix + ": prefix is already mapped to the namespace: " + globalURI);
                }
            }

            // check if namespace is already locally mapped
            String oldPrefix = (String) uriToPrefix.get(uri);
            if (oldPrefix != null) {
                if (oldPrefix.equals(prefix)) {
                    // redundant mapping, silently ignore
                    return;
                }
                // resurrect hidden global prefix
                hiddenPrefixes.remove(nsReg.getPrefix(uri));
                // remove old mapping
                uriToPrefix.remove(uri);
                prefixToURI.remove(oldPrefix);
            }

            // check if prefix is already locally mapped
            String oldURI = (String) prefixToURI.get(prefix);
            if (oldURI != null) {
                // resurrect hidden global prefix
                hiddenPrefixes.remove(nsReg.getPrefix(oldURI));
                // remove old mapping
                uriToPrefix.remove(oldURI);
                prefixToURI.remove(prefix);
            }

            if (!prefix.equals(globalPrefix)) {
                // store new mapping
                prefixToURI.put(prefix, uri);
                uriToPrefix.put(uri, prefix);
                hiddenPrefixes.add(globalPrefix);
            }
        }

        String[] getPrefixes() throws RepositoryException {
            if (prefixToURI.isEmpty()) {
                // shortcut
                return nsReg.getPrefixes();
            }

            HashSet prefixes = new HashSet();
            // global prefixes
            String[] globalPrefixes = nsReg.getPrefixes();
            for (int i = 0; i < globalPrefixes.length; i++) {
                if (!hiddenPrefixes.contains(globalPrefixes[i])) {
                    prefixes.add(globalPrefixes[i]);
                }
            }
            // local prefixes
            prefixes.addAll(prefixToURI.keySet());

            return (String[]) prefixes.toArray(new String[prefixes.size()]);
        }

        //------------------------------------------------< NamespaceResolver >
        /**
         * @see NamespaceResolver#getURI
         */
        public String getURI(String prefix) throws NamespaceException {
            if (prefixToURI.isEmpty()) {
                // shortcut
                return nsReg.getURI(prefix);
            }
            // check local mappings
            if (prefixToURI.containsKey(prefix)) {
                return (String) prefixToURI.get(prefix);
            }

            // check global mappings
            if (!hiddenPrefixes.contains(prefix)) {
                return nsReg.getURI(prefix);
            }

            throw new NamespaceException(prefix + ": unknown prefix");
        }

        /**
         * @see NamespaceResolver#getPrefix
         */
        public String getPrefix(String uri) throws NamespaceException {
            if (prefixToURI.isEmpty()) {
                // shortcut
                return nsReg.getPrefix(uri);
            }

            // check local mappings
            if (uriToPrefix.containsKey(uri)) {
                return (String) uriToPrefix.get(uri);
            }

            // check global mappings
            return nsReg.getPrefix(uri);
        }
    }
}
