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
import java.util.Properties;

/**
 * Default implementation of the <code>UserManager</code> interface with the
 * following characteristics:
 *
 * <ul>
 * <li>Users and Groups are stored in the repository as JCR nodes.</li>
 * <li>Users are created below {@link UserConstants#USERS_PATH},<br>Groups are
 * created below {@link UserConstants#GROUPS_PATH}.</li>
 * <li>In order to structure the users and groups tree and void creating a flat
 * hierarchy, additional hierarchy nodes of type "rep:AuthorizableFolder" are
 * introduced.</li>
 * <li>The names of the hierarchy folders is determined from ID of the
 * authorizable to be created, consisting of the leading N chars where N is
 * the relative depth starting from the node at {@link UserConstants#USERS_PATH}
 * or {@link UserConstants#GROUPS_PATH}.</li>
 * <li>By default 2 levels (depth == 2) are created.</li>
 * <li>Searching authorizables by ID always starts looking at that specific
 * hierarchy level. Parent nodes are expected to consist of folder structure only.</li>
 * <li>If the ID contains invalid JCR chars that would prevent the creation of
 * a Node with that name, the names of authorizable node and the intermediate
 * hierarchy nodes are {@link Text#escapeIllegalJcrChars(String) escaped}.</li>
 * <li>Any intermediate path passed to either
 * {@link #createUser(String, String, Principal, String) createUser} or
 * {@link #createGroup(Principal, String) createGroup} are ignored. This allows
 * to directly find authorizables by ID without having to search or traverse
 * the complete tree.<br>
 * See also {@link #PARAM_COMPATIBILE_JR16}.
 * </li>
 * </ul>
 * Example: Creating an non-existing authorizable with ID 'aSmith' would result
 * in the following structure:
 * 
 * <pre>
 * + rep:security            [nt:unstructured]
 *   + rep:authorizables     [rep:AuthorizableFolder]
 *     + rep:users           [rep:AuthorizableFolder]
 *       + a                 [rep:AuthorizableFolder]
 *         + aS              [rep:AuthorizableFolder]
 *           + aSmith        [rep:User]
 * </pre>
 *
 * This <code>UserManager</code> is able to handle the following configuration
 * options:
 *
 * <ul>
 * <li>{@link #PARAM_COMPATIBILE_JR16}: If the param is present and its
 * value is <code>true</code> looking up authorizables by ID will use the
 * <code>NodeResolver</code> if not found otherwise.<br>
 * If the parameter is missing (or false) users and groups created
 * with a Jackrabbit repository &lt; v2.0 will not be found any more.<br>
 * By default this option is disabled.</li>
 * <li>{@link #PARAM_DEFAULT_DEPTH}: Parameter used to change the number of
 * levels that are used by default store authorizable nodes.<br>The default
 * number of levels is 2.
 * <p/>
 * <strong>NOTE:</strong> Changing the default depth once users and groups
 * have been created in the repository will cause inconsistencies, due to
 * the fact that the resolution of ID to an authorizable relies on the
 * structure defined by the default depth.<br>
 * It is recommended to remove all authorizable nodes that will not be
 * reachable any more, before this config option is changed.
 * <ul>
 * <li>If default depth is increased:<br>
 * All authorizables on levels &lt; default depth are not reachable any more.</li>
 * <li>If default depth is decreased:<br>
 * All authorizables on levels &gt; default depth aren't reachable any more
 * unless the {@link #PARAM_AUTO_EXPAND_TREE} flag is set to <code>true</code>.</li>
 * </ul>
 * </li>
 * <li>{@link #PARAM_AUTO_EXPAND_TREE}: If this parameter is present and its
 * value is <code>true</code>, the trees containing user and group nodes will
 * automatically created additional hierarchy levels if the number of nodes
 * on a given level exceeds the maximal allowed {@link #PARAM_AUTO_EXPAND_SIZE size}.
 * <br>By default this option is disabled.</li>
 * <li>{@link #PARAM_AUTO_EXPAND_SIZE}: This parameter only takes effect
 * if {@link #PARAM_AUTO_EXPAND_TREE} is enabled.<br>The default value is
 * 1000.</li>
 * </ul>
 */
public class UserManagerImpl extends ProtectedItemModifier
        implements UserManager, UserConstants, SessionListener {

    /**
     * Flag to enable a minimal backwards compatibility with Jackrabbit &lt;
     * v2.0<br>
     * If the param is present and its value is <code>true</code> looking up
     * authorizables by ID will use the <code>NodeResolver</code> if not found
     * otherwise.<br>
     * If the parameter is missing (or false) users and groups created
     * with a Jackrabbit repository &lt; v2.0 will not be found any more.<br>
     * By default this option is disabled.
     */
    public static final String PARAM_COMPATIBILE_JR16 = "compatibleJR16";

    /**
     * Parameter used to change the number of levels that are used by default
     * store authorizable nodes.<br>The default number of levels is 2.
     * <p/>
     * <strong>NOTE:</strong> Changing the default depth once users and groups
     * have been created in the repository will cause inconsistencies, due to
     * the fact that the resolution of ID to an authorizable relies on the
     * structure defined by the default depth.<br>
     * It is recommended to remove all authorizable nodes that will not be
     * reachable any more, before this config option is changed.
     * <ul>
     * <li>If default depth is increased:<br>
     * All authorizables on levels &lt; default depth are not reachable any more.</li>
     * <li>If default depth is decreased:<br>
     * All authorizables on levels &gt; default depth aren't reachable any more
     * unless the {@link #PARAM_AUTO_EXPAND_TREE} flag is set to <code>true</code>.</li>
     * </ul>
     */
    public static final String PARAM_DEFAULT_DEPTH = "defaultDepth";

    /**
     * If this parameter is present and its value is <code>true</code>, the trees
     * containing user and group nodes will automatically created additional
     * hierarchy levels if the number of nodes on a given level exceeds the
     * maximal allowed {@link #PARAM_AUTO_EXPAND_SIZE size}.
     * <br>By default this option is disabled.
     */
    public static final String PARAM_AUTO_EXPAND_TREE = "autoExpandTree";

    /**
     * This parameter only takes effect if {@link #PARAM_AUTO_EXPAND_TREE} is
     * enabled.<br>The default value is 1000.
     */
    public static final String PARAM_AUTO_EXPAND_SIZE = "autoExpandSize";

    private static final Logger log = LoggerFactory.getLogger(UserManagerImpl.class);

    private final SessionImpl session;
    private final String adminId;
    private final NodeResolver authResolver;
    private final IdResolver idResolver;

    /**
     * Flag indicating if {@link #getAuthorizable(String)} should find users or
     * groups created with Jackrabbit < 2.0.<br>
     * As of 2.0 authorizables are created using a defined logic that allows
     * to retrieve them without searching/traversing. If this flag is
     * <code>true</code> this method will try to find authorizables using the
     * <code>authResolver</code> if not found otherwise.
     */
    private final boolean compatibleJR16;

    /**
     * Create a new <code>UserManager</code> with the default configuration.
     *
     * @param session
     * @param adminId
     * @throws RepositoryException
     */
    public UserManagerImpl(SessionImpl session, String adminId) throws RepositoryException {
        this(session, adminId, null);
    }

    /**
     * Create a new <code>UserManager</code> for the given <code>session</code>.
     * Currently the following configuration options are respected:
     *
     * <ul>
     * <li>{@link #PARAM_COMPATIBILE_JR16}. By default this option is disabled.</li>
     * <li>{@link #PARAM_DEFAULT_DEPTH}. The default number of levels is 2.</li>
     * <li>{@link #PARAM_AUTO_EXPAND_TREE}. By default this option is disabled.</li>
     * <li>{@link #PARAM_AUTO_EXPAND_SIZE}. The default value is 1000.</li>
     * </ul>
     *
     * See the overall {@link UserManagerImpl introduction} for details.
     *
     * @param session
     * @param adminId
     * @param config
     * @throws RepositoryException
     */
    public UserManagerImpl(SessionImpl session, String adminId, Properties config) throws RepositoryException {
        this.session = session;
        this.adminId = adminId;

        NodeResolver nr;
        try {
            nr = new IndexNodeResolver(session, session);
        } catch (RepositoryException e) {
            log.debug("UserManager: no QueryManager available for workspace '" + session.getWorkspace().getName() + "' -> Use traversing node resolver.");
            nr = new TraversingNodeResolver(session, session);
        }
        authResolver = nr;

        idResolver = new IdResolver(config);
        boolean compatMode = false;
        if (config != null && config.containsKey(PARAM_COMPATIBILE_JR16)) {
            compatMode = Boolean.parseBoolean(config.get(PARAM_COMPATIBILE_JR16).toString());
        }
        compatibleJR16 = compatMode;
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
        } else {
            // another Principal -> search
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
     */
    public User createUser(String userID, String password) throws RepositoryException {
        return createUser(userID, password, new PrincipalImpl(userID), null);
    }

    /**
     *
     * @param userID
     * @param password
     * @param principal
     * @param intermediatePath Is always ignored.
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
        if (intermediatePath != null) {
            log.debug("Intermediate path param " + intermediatePath + " is ignored.");
        }

        NodeImpl parent = null;
        try {
            NodeImpl userNode = (NodeImpl) idResolver.createUserNode(userID);

            setProperty(userNode, P_USERID, getValue(userID), true);
            setProperty(userNode, P_PASSWORD, getValue(UserImpl.buildPasswordValue(password)), true);
            setProperty(userNode, P_PRINCIPAL_NAME, getValue(principal.getName()), true);
            session.save();

            log.debug("User created: " + userID + "; " + userNode.getPath());
            return createUser(userNode);
        } catch (RepositoryException e) {
            // something went wrong -> revert changes and rethrow
            session.refresh(false);
            log.debug("Failed to create new User, reverting changes.");
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
     * @param intermediatePath Is always ignored.
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
        if (intermediatePath != null) {
            log.debug("Intermediate path param " + intermediatePath + " is ignored.");
        }
        
        NodeImpl parent = null;
        try {
            String groupID = getGroupId(principal.getName());
            NodeImpl groupNode = (NodeImpl) idResolver.createGroupNode(groupID);

            setProperty(groupNode, P_PRINCIPAL_NAME, getValue(principal.getName()));
            session.save();

            log.debug("Group created: " + groupID + "; " + groupNode.getPath());

            return createGroup(groupNode);
        } catch (RepositoryException e) {
            session.refresh(false);
            log.debug("newInstance new Group failed, revert changes on parent");
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
     * Test if a user or group exists that has the given principals name as ID,
     * which might happen if userID != principal-name.
     * In this case: generate another ID for the group to be created.
     *
     * @param principalName to be used as hint for the groupid.
     * @return a group id.
     * @throws RepositoryException
     */
    private String getGroupId(String principalName) throws RepositoryException {
        String escHint = principalName;
        String groupID = escHint;
        int i = 0;
        while (getAuthorizable(groupID) != null) {
            groupID = escHint + "_" + i;
            i++;
        }
        return groupID;
    }

    private Value getValue(String strValue) throws RepositoryException {
        return session.getValueFactory().createValue(strValue);
    }

    /**
     * @param userID
     * @return true if the given userID belongs to the administrator user.
     */
    boolean isAdminId(String userID) {
        return (adminId != null) && adminId.equals(userID);
    }

    /**
     * Build the User object from the given user node.
     *
     * @param userNode
     * @return
     * @throws RepositoryException
     */
    User createUser(NodeImpl userNode) throws RepositoryException {
        if (userNode == null || !userNode.isNodeType(NT_REP_USER)) {
            throw new IllegalArgumentException();
        }
        if (!Text.isDescendant(USERS_PATH, userNode.getPath())) {
            throw new IllegalArgumentException("User has to be within the User Path");
        }
        User user = doCreateUser(userNode);
        return user;
    }

    /**
     * Build the user object from the given user node. May be overridden to
     * return a custom implementation.
     *
     * @param node user node
     * @return user object
     * @throws RepositoryException if an error occurs
     */
    protected User doCreateUser(NodeImpl node) throws RepositoryException {
        return new UserImpl(node, this);
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
        return group;
    }

    /**
     * Resolve the given <code>userID</code> to an rep:user node in the repository.
     *
     * @param userID A valid userID.
     * @return the node associated with the given userID or <code>null</code>.
     * @throws RepositoryException If an error occurs.
     */
    private NodeImpl getUserNode(String userID) throws RepositoryException {
        NodeImpl n = (NodeImpl) idResolver.findNode(userID, false);
        if (n == null && compatibleJR16) {
            // backwards-compatibiltiy with JR < 2.0 user structure that doesn't
            // allow to determine the auth-path from the id directly.
            // search for it the node belonging to that userID
            n = (NodeImpl) authResolver.findNode(P_USERID, userID, NT_REP_USER);
        } // else: no such user -> return null.
        return n;
    }

    /**
     * Resolve the given <code>groupID</code> to an rep:group node in the repository.
     *
     * @param groupID A valid groupID.
     * @return the node associated with the given userID or <code>null</code>.
     * @throws RepositoryException If an error occurs.
     */
    private NodeImpl getGroupNode(String groupID) throws RepositoryException {
        NodeImpl n = (NodeImpl) idResolver.findNode(groupID, true);
        if (n == null && compatibleJR16) {
            // backwards-compatibiltiy with JR < 2.0 group structure that doesn't
            // allow to determine the auth-path from the id directly
            // search for it the node belonging to that groupID.
            // NOTE: JR < 2.0 always returned groupIDs that didn't contain any
            // illegal JCR chars. Since Group.getID() now unescapes the node
            // name additional escaping is required.
            Name nodeName = session.getQName(Text.escapeIllegalJcrChars(groupID));
            n = (NodeImpl) authResolver.findNode(nodeName, NT_REP_GROUP);
        } // else: no such group -> return null.
        return n;
    }

    private static boolean isValidPrincipal(Principal principal) {
        return principal != null && principal.getName() != null && principal.getName().length() > 0;
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

    //--------------------------------------------------------------------------
    /**
     * Inner class creating and finding the JCR nodes corresponding the a given
     * authorizable ID with the following behavior:
     * <ul>
     * <li>Users are created below /rep:security/rep:authorizables/rep:users</li>
     * <li>Groups are created below /rep:security/rep:authorizables/rep:users</li>
     * <li>Below each category authorizables are created within a human readable
     * structure, whose depth is defined by the <code>defaultDepth</code> config
     * option.<br>
     * E.g. creating a user node for an ID 'aSmith' would result in the following
     * structure assuming defaultDepth == 2 is used:
     * <pre>
     * + rep:security            [nt:unstructured]
     *   + rep:authorizables     [rep:AuthorizableFolder]
     *     + rep:users           [rep:AuthorizableFolder]
     *       + a                 [rep:AuthorizableFolder]
     *         + aS              [rep:AuthorizableFolder]
     * ->        + aSmith        [rep:User]
     * </pre>
     * </li>
     * <li>In case of a user the node name is calculated from the specified UserID
     * {@link Text#escapeIllegalJcrChars(String) escaping} any illegal JCR chars.
     * In case of a Group the node name is calculated from the specified principal
     * name circumventing any conflicts with existing ids and escaping illegal chars.</li>
     * <li>The names of the intermediate folders are caculated from the leading
     * chars of the escaped node name.</li>
     * <li>If the escaped node name is shorter than the <code>defaultDepth</code>
     * the last char is repeated.<br>
     * E.g. creating a user node for an ID 'a' would result in the following
     * structure assuming defaultDepth == 2 is used:
     * <pre>
     * + rep:security            [nt:unstructured]
     *   + rep:authorizables     [rep:AuthorizableFolder]
     *     + rep:users           [rep:AuthorizableFolder]
     *       + a                 [rep:AuthorizableFolder]
     *         + aa              [rep:AuthorizableFolder]
     * ->        + a             [rep:User]
     * </pre>
     * </li>
     * <li>If the <code>autoExpandTree</code> option is <code>true</code> the
     * user tree will be automatically expanded using additional levels if
     * <code>autoExpandSize</code> is exceeded within a given level.</li>
     * </ul>
     *
     * The auto-expansion of the authorizable tree is defined by the following
     * steps and exceptional cases:
     * <ul>
     * <li>As long as <code>autoExpandSize</code> isn't reached authorizable
     * nodes are created within the structure defined by the
     * <code>defaultDepth</code>. (see above)</li>
     * <li>If <code>autoExpandSize</code> is reached additional intermediate
     * folders will be created.<br>
     * E.g. creating a user node for an ID 'aSmith1001' would result in the
     * following structure:
     * <pre>
     * + rep:security            [nt:unstructured]
     *   + rep:authorizables     [rep:AuthorizableFolder]
     *     + rep:users           [rep:AuthorizableFolder]
     *       + a                 [rep:AuthorizableFolder]
     *         + aS              [rep:AuthorizableFolder]
     *           + aSmith1       [rep:User]
     *           + aSmith2       [rep:User]
     *           [...]
     *           + aSmith1000    [rep:User]
     * ->        + aSm           [rep:AuthorizableFolder]
     * ->          + aSmith1001  [rep:User]
     * </pre>
     * </li>
     * <li>Conflicts: In order to prevent any conflicts that would arise from
     * creating a authorizable node that upon later expansion could conflict
     * with an authorizable folder, intermediate levels are always created if
     * the node name equals any of the names reserved for the next level of
     * folders.<br>
     * In the example above any attempt to create a user with ID 'aSm' would
     * result in an intermediate level irrespective if max-size has been
     * reached or not:
     * <pre>
     * + rep:security            [nt:unstructured]
     *   + rep:authorizables     [rep:AuthorizableFolder]
     *     + rep:users           [rep:AuthorizableFolder]
     *       + a                 [rep:AuthorizableFolder]
     *         + aS              [rep:AuthorizableFolder]
     * ->        + aSm           [rep:AuthorizableFolder]
     * ->          + aSm         [rep:User]
     * </pre>
     * </li>
     * <li>Special case: If the name of the authorizable node to be created is
     * shorter or equal to the length of the folder at level N, the authorizable
     * node is created even if max-size has been reached before.<br>
     * An attempt to create the users 'aS' and 'aSm' in a structure containing
     * tons of 'aSmith' users will therefore result in:
     * <pre>
     * + rep:security            [nt:unstructured]
     *   + rep:authorizables     [rep:AuthorizableFolder]
     *     + rep:users           [rep:AuthorizableFolder]
     *       + a                 [rep:AuthorizableFolder]
     *         + aS              [rep:AuthorizableFolder]
     *           + aSmith1       [rep:User]
     *           + aSmith2       [rep:User]
     *           [...]
     *           + aSmith1000    [rep:User]
     * ->        + aS            [rep:User]
     *           + aSm           [rep:AuthorizableFolder]
     *             + aSmith1001  [rep:User]
     * ->          + aSm         [rep:User]
     * </pre>
     * </li>
     * </ul>
     *
     * The configuration options:
     * <ul>
     * <li><strong>defaultDepth</strong>:<br>
     * <code>integer</code> defining the depth of the default structure that is
     * always created.<br>
     * Default value: 2</li>
     * <li><strong>autoExpandTree</strong>:<br>
     * <code>boolean</code> defining if the tree gets automatically expanded
     * if within a level the maximum number of child nodes is reached.<br>
     * Default value: <code>false</code></li>
     * <li><strong>autoExpandSize</strong>:<br>
     * <code>long</code> defining the maximum number of child nodes that are
     * allowed at a given level.<br>
     * Default value: 1000<br>
     * NOTE: that total number of child nodes may still be greater that
     * autoExpandSize.</li>
     * </ul>
     */
    private class IdResolver {

        private static final String DELIMITER = "/";
        private static final int DEFAULT_DEPTH = 2;
        private static final long DEFAULT_SIZE = 1000;
        
        private final int defaultDepth;
        private final boolean autoExpandTree;
        // best effort max-size of authorizables per folder. there may be
        // more nodes created if the editing session isn't allowed to see
        // all child nodes.
        private final long autoExpandSize;

        private IdResolver(Properties config) {
            int d = DEFAULT_DEPTH;
            boolean expand = false;
            long size = DEFAULT_SIZE;

            if (config != null) {
                if (config.containsKey(PARAM_DEFAULT_DEPTH)) {
                    try {
                        d = Integer.parseInt(config.get(PARAM_DEFAULT_DEPTH).toString());
                    } catch (NumberFormatException e) {
                        log.warn("Unable to parse defaultDepth config option", e);
                    }
                }
                if (config.containsKey(PARAM_AUTO_EXPAND_TREE)) {
                    expand = Boolean.parseBoolean(config.get(PARAM_AUTO_EXPAND_TREE).toString());
                }
                if (config.containsKey(PARAM_AUTO_EXPAND_SIZE)) {
                    try {
                        size = Integer.parseInt(config.get(PARAM_AUTO_EXPAND_SIZE).toString());
                    } catch (NumberFormatException e) {
                        log.warn("Unable to parse autoExpandSize config option", e);
                    }
                }
            }

            defaultDepth = d;
            autoExpandTree = expand;
            autoExpandSize = size;
        }

        public Node createUserNode(String userID) throws RepositoryException {
            return createAuthorizableNode(userID, false);
        }

        public Node createGroupNode(String groupID) throws RepositoryException {
            return createAuthorizableNode(groupID, true);
        }

        public Node findNode(String id, boolean isGroup) throws RepositoryException {
            String defaultFolderPath = getDefaultFolderPath(id, isGroup);
            String escapedId = Text.escapeIllegalJcrChars(id);

            if (session.nodeExists(defaultFolderPath)) {
                Node folder = session.getNode(defaultFolderPath);
                Name expectedNt = (isGroup) ? NT_REP_GROUP : NT_REP_USER;

                // traverse the potentially existing hierarchy looking for the
                // authorizable node.
                int segmLength = defaultDepth +1;
                while (folder != null) {
                    if (folder.hasNode(escapedId)) {
                        NodeImpl aNode = (NodeImpl) folder.getNode(escapedId);
                        if (aNode.isNodeType(expectedNt)) {
                            // done. found the right auth-node
                            return aNode;
                        } else {
                            folder = aNode;
                        }
                    } else {
                        // no child node with name 'escapedId' -> look for
                        // additional levels that may exist.
                        Node parent = folder;
                        folder = null;
                        if (id.length() >= segmLength) {
                            String folderName = Text.escapeIllegalJcrChars(id.substring(0, segmLength));
                            if (parent.hasNode(folderName)) {
                                NodeImpl f = (NodeImpl) parent.getNode(folderName);
                                if (f.isNodeType(NT_REP_AUTHORIZABLE_FOLDER)) {
                                    folder = f;
                                } // else: matching node isn't an authorizable-folder
                            } // else: failed to find a suitable next level
                        } // else: id is shorter than required length at the current level.
                    }
                    segmLength++;
                }
            } // else: no node at default-path

            // no matching node found -> authorizable doesn't exist.
            return null;
        }

        private Node createAuthorizableNode(String id, boolean isGroup) throws RepositoryException {
            String escapedId = Text.escapeIllegalJcrChars(id);

            // first create the default folder nodes, that are always present.
            Node folder = createDefaultFolderNodes(id, escapedId, isGroup);
            // eventually create additional intermediate folders.
            folder = createIntermediateFolderNodes(id, escapedId, folder);

            // finally create the authorizable node
            Name nodeName = session.getQName(escapedId);
            Name ntName = (isGroup) ? NT_REP_GROUP : NT_REP_USER;
            Node authNode = addNode((NodeImpl) folder, nodeName, ntName);

            return authNode;
        }

        private Node createDefaultFolderNodes(String id, String escapedId, boolean isGroup) throws RepositoryException {
            NodeImpl folder;
            // first create the levels that are always present -> see #getDefaultFolderPath
            String defaultPath = getDefaultFolderPath(id, isGroup);
            if (session.nodeExists(defaultPath)) {
                folder = (NodeImpl) session.getNode(defaultPath);
            } else {
                String[] segmts = defaultPath.split("/");
                folder = (NodeImpl) session.getRootNode();
                String repSecurity = SECURITY_ROOT_PATH.substring(1);

                for (String segment : segmts) {
                    if (segment.length() < 1) {
                        continue;
                    }
                    if (folder.hasNode(segment)) {
                        folder = (NodeImpl) folder.getNode(segment);
                    } else {
                        Name ntName;
                        if (repSecurity.equals(segment)) {
                            // rep:security node
                            ntName = NameConstants.NT_UNSTRUCTURED;
                        } else {
                            ntName = NT_REP_AUTHORIZABLE_FOLDER;
                        }
                        NodeImpl added = addNode(folder, session.getQName(segment), ntName);
                        folder.save();
                        folder = added;
                    }
                }
            }

            // validation check if authorizable to be create doesn't conflict.
            checkExists(escapedId, folder);
            return folder;
        }

        private String getDefaultFolderPath(String id, boolean isGroup) {
            StringBuilder bld = new StringBuilder();
            if (isGroup) {
                bld.append(GROUPS_PATH);
            } else {
                bld.append(USERS_PATH);
            }
            StringBuilder lastSegment = new StringBuilder(defaultDepth);
            int idLength = id.length();
            for (int i = 0; i < defaultDepth; i++) {
                if (idLength > i) {
                    lastSegment.append(id.charAt(i));
                } else {
                    // escapedID is too short -> append the last char again
                    lastSegment.append(id.charAt(idLength-1));
                }
                bld.append(DELIMITER).append(Text.escapeIllegalJcrChars(lastSegment.toString()));
            }
            return bld.toString();
        }

        private Node createIntermediateFolderNodes(String id, String escapedId, Node folder) throws RepositoryException {
            if (!autoExpandTree) {
                // additional folders are never created
                return folder;
            }

            // additional folders needs be created if
            // - the maximal size of child nodes is reached
            // - if the auth-node to be created potentially collides with any
            //   of the intermediate nodes.
            int segmLength = defaultDepth +1;
            int idLength = id.length();

            while (intermediateFolderNeeded(escapedId, folder)) {
                String folderName = Text.escapeIllegalJcrChars(id.substring(0, segmLength));
                // validation check on each intermediate level if authorizable
                // to be created doesn't conflict.
                checkExists(folderName, folder);

                if (folder.hasNode(folderName)) {
                    folder = folder.getNode(folderName);
                } else {
                    folder = addNode((NodeImpl) folder, session.getQName(folderName), NT_REP_AUTHORIZABLE_FOLDER);
                }
                segmLength++;
            }

            // final validation check if authorizable to be created doesn't conflict.
            checkExists(escapedId, folder);
            return folder;
        }

        private void checkExists(String nodeName, Node folder) throws RepositoryException {
            if (folder.hasNode(nodeName) &&
                    folder.getNode(nodeName).isNodeType(session.getJCRName(NT_REP_AUTHORIZABLE))) {
                throw new AuthorizableExistsException("Unable to create Group/User: Collision with existing authorizable.");
            }
        }

        private boolean intermediateFolderNeeded(String nodeName, Node folder) throws RepositoryException {
            // don't create additional intermediate folders for ids that are
            // shorter or equally long as the folder name. In this case the
            // MAX_SIZE flag is ignored.
            if (nodeName.length() <= folder.getName().length()) {
                return false;
            }

            // test for potential (or existing) collision in which case the
            // intermediate node is created irrespective of the MAX_SIZE and the
            // existing number of children.
            if (nodeName.length() == folder.getName().length()+1) {
                // max-size may not yet be reached yet on folder but the node to
                // be created potentially collides with an intermediate folder.
                // e.g.:
                // existing folder structure: a/ab
                // authID to be created     : abt
                // OR
                // existing collition that would result from
                // existing folder structure: a/ab/abt
                // authID to be create      : abt
                return true;
            }

            // last possibility: max-size is reached.
            if (folder.getNodes().getSize() >= autoExpandSize) {
                return true;
            }
            
            // no collision and no need to create an additional intermediate
            // folder due to max-size reached
            return false;
        }
    }
}
