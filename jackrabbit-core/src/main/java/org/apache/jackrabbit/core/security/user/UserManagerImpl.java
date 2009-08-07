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
package org.apache.jackrabbit.core.security.user;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.AuthorizableExistsException;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.ItemImpl;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.ProtectedItemModifier;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.SessionListener;
import org.apache.jackrabbit.core.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.core.security.principal.PrincipalImpl;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.util.Text;
import org.apache.commons.collections.map.LRUMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Map;

/**
 * UserManagerImpl
 */
public class UserManagerImpl extends ProtectedItemModifier implements UserManager, UserConstants, SessionListener {

    private static final Logger log = LoggerFactory.getLogger(UserManagerImpl.class);

    private final SessionImpl session;
    private final String adminId;
    private final NodeResolver authResolver;

    /**
     * Simple unmanaged map from authorizableID to nodePath (node representing
     * the authorizable) used limit the number of calls to the
     * <code>NodeResolver</code> in order to find authorizable nodes by the
     * authorizable id.
     */
    private final Map idPathMap = new LRUMap(1000);

    public UserManagerImpl(SessionImpl session, String adminId) throws RepositoryException {
        super();
        this.session = session;
        this.adminId = adminId;

        NodeResolver nr;
        try {
            nr = new IndexNodeResolver(session, session);
        } catch (RepositoryException e) {
            log.debug("UserManger: no QueryManager available for workspace '" + session.getWorkspace().getName() + "' -> Use traversing node resolver.");
            nr = new TraversingNodeResolver(session, session);
        }
        authResolver = nr;
    }

    //--------------------------------------------------------< UserManager >---
    /**
     * @see UserManager#getAuthorizable(String)
     */
    public Authorizable getAuthorizable(String id) throws RepositoryException {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("Invalid authorizable name '" + id + "'");
        }
        Authorizable authorz = null;
        NodeImpl n = getUserNode(id);
        if (n != null) {
            authorz = createUser(n);
        } else {
            n = getGroupNode(id);
            if (n != null) {
                authorz = createGroup(n);
            }
        }
        return authorz;
    }

    /**
     * @see UserManager#getAuthorizable(Principal)
     */
    public Authorizable getAuthorizable(Principal principal) throws RepositoryException {
        NodeImpl n = null;
        // shortcut that avoids executing a query.
        if (principal instanceof ItemBasedPrincipal) {
            String authPath = ((ItemBasedPrincipal) principal).getPath();
            if (session.itemExists(authPath)) {
                Item authItem = session.getItem(authPath);
                if (authItem.isNode()) {
                    n = (NodeImpl) authItem;
                }
            }
        }
        // another Principal -> search
        if (n == null) {
            String name = principal.getName();
            n = (NodeImpl) authResolver.findNode(P_PRINCIPAL_NAME, name, NT_REP_AUTHORIZABLE);
        }
        // build the corresponding authorizable object
        if (n != null) {
            if (n.isNodeType(NT_REP_USER)) {
               return createUser(n);
            } else if (n.isNodeType(NT_REP_GROUP)) {
               return createGroup(n);
            } else {
                log.warn("Unexpected user nodetype " + n.getPrimaryNodeType().getName());
            }
        }
        return null;
    }

    /**
     * @see UserManager#findAuthorizables(String,String)
     */
    public Iterator findAuthorizables(String propertyName, String value) throws RepositoryException {
        return findAuthorizables(propertyName, value, SEARCH_TYPE_AUTHORIZABLE);
    }

    /**
     * @see UserManager#findAuthorizables(String,String, int)
     */
    public Iterator findAuthorizables(String propertyName, String value, int searchType)
            throws RepositoryException {
        Name name = session.getQName(propertyName);
        Name ntName;
        switch (searchType) {
            case SEARCH_TYPE_AUTHORIZABLE:
                ntName = NT_REP_AUTHORIZABLE;
                break;
            case SEARCH_TYPE_GROUP:
                ntName = NT_REP_GROUP;
                break;
            case SEARCH_TYPE_USER:
                ntName = NT_REP_USER;
                break;
            default: throw new IllegalArgumentException("Invalid search type " + searchType);
        }
        NodeIterator nodes = authResolver.findNodes(name, value, ntName, true);
        return new AuthorizableIterator(nodes);
    }

    /**
     * Creates a new Node on the repository with the specified
     * <code>userName</code>.<br>
     * The User will be created relative to path of the User who represents the
     * Session this UserManager has been created for.<br>
     * If the {@link javax.jcr.Credentials Credentials} are of type
     * {@link javax.jcr.SimpleCredentials SimpleCredentials} they will be
     * crypted.
     *
     * @param userID
     * @param password
     * @see UserManager#createUser(String,String)
     * @inheritDoc
     */
    public User createUser(String userID, String password) throws RepositoryException {
        return createUser(userID, password, new PrincipalImpl(userID), null);
    }

    /**
     *
     * @param userID
     * @param password
     * @param principal
     * @param intermediatePath
     * @return
     * @throws AuthorizableExistsException
     * @throws RepositoryException
     */
    public User createUser(String userID, String password,
                           Principal principal, String intermediatePath)
            throws AuthorizableExistsException, RepositoryException {
        if (userID == null || userID.length() == 0) {
            throw new IllegalArgumentException("Cannot create user: UserID can neither be null nor empty String.");
        }
        if (password == null) {
            throw new IllegalArgumentException("Cannot create user: null password.");
        }
        if (!isValidPrincipal(principal)) {
            throw new IllegalArgumentException("Cannot create user: Principal may not be null and must have a valid name.");            
        }
        if (getAuthorizable(userID) != null) {
            throw new AuthorizableExistsException("User for '" + userID + "' already exists");
        }
        if (hasAuthorizableOrReferee(principal)) {
            throw new AuthorizableExistsException("Authorizable for '" + principal.getName() + "' already exists");
        }

        NodeImpl parent = null;
        try {
            String parentPath = getParentPath(intermediatePath, getCurrentUserPath());
            parent = createParentNode(parentPath);

            Name nodeName = session.getQName(Text.escapeIllegalJcrChars(userID));
            NodeImpl userNode = addNode(parent, nodeName, NT_REP_USER);

            setProperty(userNode, P_USERID, getValue(userID), true);
            setProperty(userNode, P_PASSWORD, getValue(UserImpl.buildPasswordValue(password)), true);
            setProperty(userNode, P_PRINCIPAL_NAME, getValue(principal.getName()), true);
            parent.save();

            log.debug("User created: " + userID + "; " + userNode.getPath());
            return createUser(userNode);
        } catch (RepositoryException e) {
            // something went wrong -> revert changes and rethrow
            if (parent != null) {
                parent.refresh(false);
                log.debug("Failed to create new User, reverting changes.");
            }
            throw e;
        }
    }

    /**
     * Create a new <code>Group</code> with the given <code>groupName</code>.
     * It will be created below the this UserManager's root Path.<br>
     * If non-existant elements of the Path will be created as <code>Nodes</code>
     * of type {@link #NT_REP_AUTHORIZABLE_FOLDER rep:AuthorizableFolder}
     *
     * @param principal
     * @see UserManager#createGroup(Principal);
     * @inheritDoc
     */
    public Group createGroup(Principal principal) throws RepositoryException {
        return createGroup(principal, null);
    }

    /**
     *
     * @param principal
     * @param intermediatePath
     * @return
     * @throws AuthorizableExistsException
     * @throws RepositoryException
     */
    public Group createGroup(Principal principal, String intermediatePath) throws AuthorizableExistsException, RepositoryException {
        if (!isValidPrincipal(principal)) {
            throw new IllegalArgumentException("Cannot create Group: Principal may not be null and must have a valid name.");
        }
        if (hasAuthorizableOrReferee(principal)) {
            throw new AuthorizableExistsException("Authorizable for '" + principal.getName() + "' already exists: ");
        }

        NodeImpl parent = null;
        try {
            String parentPath = getParentPath(intermediatePath, GROUPS_PATH);
            parent = createParentNode(parentPath);
            Name groupID = getGroupId(principal.getName());

            NodeImpl groupNode = addNode(parent, groupID, NT_REP_GROUP);
            setProperty(groupNode, P_PRINCIPAL_NAME, getValue(principal.getName()));
            parent.save();

            log.debug("Group created: " + groupID + "; " + groupNode.getPath());

            return createGroup(groupNode);
        } catch (RepositoryException e) {
            if (parent != null) {
                parent.refresh(false);
                log.debug("newInstance new Group failed, revert changes on parent");
            }
            throw e;
        }
    }

    //--------------------------------------------------------------------------
    /**
     *
     * @param principal
     * @return
     * @throws RepositoryException
     */
    boolean hasAuthorizableOrReferee(Principal principal) throws RepositoryException {
        Set s = new HashSet(2);
        s.add(P_PRINCIPAL_NAME);
        s.add(P_REFEREES);
        NodeIterator res = authResolver.findNodes(s, principal.getName(), NT_REP_AUTHORIZABLE, true, 1);
        return res.hasNext();
    }

    void setProtectedProperty(NodeImpl node, Name propName, Value value) throws RepositoryException, LockException, ConstraintViolationException, ItemExistsException, VersionException {
        setProperty(node, propName, value);
        node.save();
    }

    void setProtectedProperty(NodeImpl node, Name propName, Value[] values) throws RepositoryException, LockException, ConstraintViolationException, ItemExistsException, VersionException {
        setProperty(node, propName, values);
        node.save();
    }

    void removeProtectedItem(ItemImpl item, Node parent) throws RepositoryException, AccessDeniedException, VersionException {
        removeItem(item);
        parent.save();
    }

    /**
     * Escape illegal JCR characters and test if a user exists that has the
     * principals name as userId, which might happen if userID != principal-name.
     * In this case: generate another ID for the group to be created.
     *
     * @param principalName to be used as hint for the groupid.
     * @return a group id.
     * @throws RepositoryException
     */
    private Name getGroupId(String principalName) throws RepositoryException {
        String escHint = Text.escapeIllegalJcrChars(principalName);
        String groupID = escHint;
        int i = 0;
        while (getAuthorizable(groupID) != null) {
            groupID = escHint + "_" + i;
            i++;
        }
        return session.getQName(groupID);
    }

    private Value getValue(String strValue) throws RepositoryException {
        return session.getValueFactory().createValue(strValue);
    }

    /**
     * @return true if the given userID belongs to the administrator user.
     */
    boolean isAdminId(String userID) {
        return (adminId == null) ? false : adminId.equals(userID);
    }

    /**
     * Build the User object from the given user node.
     *
     * @param userNode
     * @return
     * @throws RepositoryException
     */
    User createUser(NodeImpl userNode) throws RepositoryException {
        User user = UserImpl.create(userNode, this);
        idPathMap.put(user.getID(), userNode.getPath());
        return user;
    }

    /**
     * Build the Group object from the given group node.
     *
     * @param groupNode
     * @return
     * @throws RepositoryException
     */
    Group createGroup(NodeImpl groupNode) throws RepositoryException {
        Group group = GroupImpl.create(groupNode, this);
        idPathMap.put(group.getID(), groupNode.getPath());
        return group;
    }

    /**
     * @param userID
     * @return the node associated with the given userID or <code>null</code>.
     */
    private NodeImpl getUserNode(String userID) throws RepositoryException {
        NodeImpl n = null;
        if (idPathMap.containsKey(userID)) {
            String path = idPathMap.get(userID).toString();
            if (session.itemExists(path)) {
                Item itm = session.getItem(path);
                // make sure the item really represents the node associated with
                // the given userID. if not the search below is execute.
                if (itm.isNode()) {
                    NodeImpl tmp = (NodeImpl) itm;
                    if (tmp.isNodeType(NT_REP_USER) && userID.equals(((NodeImpl) itm).getProperty(P_USERID).getString())) {
                        n = (NodeImpl) itm;
                    }
                }
            }
        }

        if (n == null) {
            // clear eventual previous entry
            idPathMap.remove(userID);
            // search for it the node belonging to that userID
            n = (NodeImpl) authResolver.findNode(P_USERID, userID, NT_REP_USER);
        }
        return n;
    }

    private NodeImpl getGroupNode(String groupID) throws RepositoryException {
        NodeImpl n = null;
        if (idPathMap.containsKey(groupID)) {
            String path = idPathMap.get(groupID).toString();
            if (session.itemExists(path)) {
                Item itm = session.getItem(path);
                // make sure the item really represents the node associated with
                // the given userID. if not the search below is execute.
                if (itm.isNode()) {
                    NodeImpl tmp = (NodeImpl) itm;
                    if (tmp.isNodeType(NT_REP_GROUP) && groupID.equals(tmp.getName())) {
                        n = (NodeImpl) itm;
                    }
                }
            }
        }
        if (n == null) {
            // clear eventual previous entry
            idPathMap.remove(groupID);
            // search for it the node belonging to that groupID
            Name nodeName = session.getQName(groupID);
            n = (NodeImpl) authResolver.findNode(nodeName, NT_REP_GROUP);
        }
        return n;
    }

    /**
     * @return the path refering to the node associated with the user this
     * <code>UserManager</code> has been built for.
     */
    private String getCurrentUserPath() {
        // fallback: default user-path
        String currentUserPath = USERS_PATH;
        String userId = session.getUserID();

        if (idPathMap.containsKey(userId)) {
            currentUserPath = idPathMap.get(userId).toString();
        } else {
            try {
                Node n = getUserNode(userId);
                if (n != null) {
                    currentUserPath = n.getPath();
                }
            } catch (RepositoryException e) {
                // should never get here
                log.error("Internal error: unable to build current user path.", e.getMessage());
            }
        }
        return currentUserPath;
    }

    private static boolean isValidPrincipal(Principal principal) {
        return principal != null && principal.getName() != null && principal.getName().length() > 0;
    }
    
    private static String getParentPath(String hint, String root) {
        StringBuffer b = new StringBuffer();
        if (hint == null || !hint.startsWith(root)) {
            b.append(root);
        }
        if (hint != null && hint.length() > 1) {
            if (!hint.startsWith("/")) {
                b.append("/");
            }
            b.append(hint);
        }
        return b.toString();
    }

    /**
     * @param path to the authorizable node to be created
     * @return
     * @throws RepositoryException
     */
    private NodeImpl createParentNode(String path) throws RepositoryException {
        NodeImpl parent = (NodeImpl) session.getRootNode();
        String[] elem = path.split("/");
        for (int i = 0; i < elem.length; i++) {
            String name = elem[i];
            if (name.length() < 1) {
                continue;
            }
            Name nName = session.getQName(name);
            if (!parent.hasNode(nName)) {
                Name ntName;
                if (i == 0) {
                    // rep:security node
                    ntName = NameConstants.NT_UNSTRUCTURED;
                } else {
                    ntName = NT_REP_AUTHORIZABLE_FOLDER;
                }
                NodeImpl added = addNode(parent, nName, ntName);
                parent.save();
                parent = added;
            } else {
                parent = parent.getNode(nName);
            }
        }
        return parent;
    }

    //----------------------------------------------------< SessionListener >---
    /**
     * @see SessionListener#loggingOut(org.apache.jackrabbit.core.SessionImpl)
     */
    public void loggingOut(SessionImpl session) {
        // nothing to do.
    }

    /**
     * @see SessionListener#loggedOut(org.apache.jackrabbit.core.SessionImpl) 
     */
    public void loggedOut(SessionImpl session) {
        // clear the map
        idPathMap.clear();
        // and logout the session unless it is the loggedout session itself.
        if (session != this.session) {
            this.session.logout();
        }
    }

    //--------------------------------------------------------------------------
    /**
     * Inner class
     */
    private final class AuthorizableIterator implements Iterator {

        private final Set served = new HashSet();

        private Authorizable next;
        private NodeIterator authNodeIter;

        private AuthorizableIterator(NodeIterator authNodeIter) {
            this.authNodeIter = authNodeIter;
            next = seekNext();
        }

        //-------------------------------------------------------< Iterator >---
        /**
         * @see Iterator#hasNext()
         */
        public boolean hasNext() {
            return next != null;
        }

        /**
         * @see Iterator#next()
         */
        public Object next() {
            Authorizable authr = next;
            if (authr == null) {
                throw new NoSuchElementException();
            }
            next = seekNext();
            return authr;
        }

        /**
         * @see Iterator#remove()
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }

        //----------------------------------------------------------------------
        private Authorizable seekNext() {
            while (authNodeIter.hasNext()) {
                NodeImpl node = (NodeImpl) authNodeIter.nextNode();
                try {
                    if (!served.contains(node.getUUID())) {
                        Authorizable authr;
                        if (node.isNodeType(NT_REP_USER)) {
                            authr = createUser(node);
                        } else if (node.isNodeType(NT_REP_GROUP)) {
                            authr = createGroup(node);
                        } else {
                            log.warn("Ignoring unexpected nodetype: " + node.getPrimaryNodeType().getName());
                            continue;
                        }
                        served.add(node.getUUID());
                        return authr;
                    }
                } catch (RepositoryException e) {
                    log.debug(e.getMessage());
                    // continue seeking next authorizable
                }
            }

            // no next authorizable -> iteration is completed.
            return null;
        }
    }
}
