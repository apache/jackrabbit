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

import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.AuthorizableExistsException;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.Query;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.ItemImpl;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.ProtectedItemModifier;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.SessionListener;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.security.principal.EveryonePrincipal;
import org.apache.jackrabbit.core.security.principal.PrincipalImpl;
import org.apache.jackrabbit.core.security.user.action.AuthorizableAction;
import org.apache.jackrabbit.core.session.SessionOperation;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

/**
 * Default implementation of the <code>UserManager</code> interface with the
 * following characteristics:
 *
 * <ul>
 * <li>Users and Groups are stored in the repository as JCR nodes.</li>
 * <li>Users are created below {@link UserConstants#USERS_PATH},<br>Groups are
 * created below {@link UserConstants#GROUPS_PATH} (unless otherwise configured).</li>
 * <li>The Id of an authorizable is stored in the jcr:uuid property (md5 hash).</li>
 * <li>In order to structure the users and groups tree and avoid creating a flat
 * hierarchy, additional hierarchy nodes of type "rep:AuthorizableFolder" are
 * introduced using
 *    <ul>
 *    <li>the specified intermediate path passed to the create methods</li>
 *    <li>or some built-in logic if the intermediate path is missing.</li>
 *    </ul>
 * </li>
 * </ul>
 *
 * The built-in logic applies the following rules:
 * <ul>
 * <li>The names of the hierarchy folders is determined from ID of the
 * authorizable to be created, consisting of the leading N chars where N is
 * the relative depth starting from the node at {@link #getUsersPath()}
 * or {@link #getGroupsPath()}.</li>
 * <li>By default 2 levels (depth == 2) are created.</li>
 * <li>Parent nodes are expected to consist of folder structure only.</li>
 * <li>If the ID contains invalid JCR chars that would prevent the creation of
 * a Node with that name, the names of authorizable node and the intermediate
 * hierarchy nodes are {@link Text#escapeIllegalJcrChars(String) escaped}.</li>
 * </ul>
 * Examples:
 * Creating an non-existing user with ID 'aSmith' without specifying an
 * intermediate path would result in the following structure:
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
 * Creating a non-existing user with ID 'aSmith' specifying an intermediate
 * path 'some/tree' would result in the following structure:
 *
 * <pre>
 * + rep:security            [nt:unstructured]
 *   + rep:authorizables     [rep:AuthorizableFolder]
 *     + rep:users           [rep:AuthorizableFolder]
 *       + some              [rep:AuthorizableFolder]
 *         + tree            [rep:AuthorizableFolder]
 *           + aSmith        [rep:User]
 * </pre>
 *
 * This <code>UserManager</code> is able to handle the following configuration
 * options:
 *
 * <ul>
 * <li>{@link #PARAM_USERS_PATH}: Defines where user nodes are created.
 * If missing set to {@link #USERS_PATH}.</li>
 * <li>{@link #PARAM_GROUPS_PATH}. Defines where group nodes are created.
 * If missing set to {@link #GROUPS_PATH}.</li>
 * <li>{@link #PARAM_COMPATIBLE_JR16}: If the param is present and its
 * value is <code>true</code> looking up authorizables by ID will use the
 * <code>NodeResolver</code> if not found otherwise.<br>
 * If the parameter is missing (or false) users and groups created
 * with a Jackrabbit repository &lt; v2.0 will not be found any more.<br>
 * By default this option is disabled.</li>
 * <li>{@link #PARAM_DEFAULT_DEPTH}: Parameter used to change the number of
 * levels that are used by default to store authorizable nodes.<br>The value is
 * expected to be a positive integer greater than zero. The default
 * number of levels is 2.
 * </li>
 * <li>{@link #PARAM_AUTO_EXPAND_TREE}: If this parameter is present and its
 * value is <code>true</code>, the trees containing user and group nodes will
 * automatically created additional hierarchy levels if the number of nodes
 * on a given level exceeds the maximal allowed {@link #PARAM_AUTO_EXPAND_SIZE size}.
 * <br>By default this option is disabled.</li>
 * <li>{@link #PARAM_AUTO_EXPAND_SIZE}: This parameter only takes effect
 * if {@link #PARAM_AUTO_EXPAND_TREE} is enabled.<br>The value is expected to be
 * a positive long greater than zero. The default value is 1000.</li>
 * </ul>
 */
public class UserManagerImpl extends ProtectedItemModifier
        implements UserManager, UserConstants, SessionListener {

    /**
     * Configuration option to change the
     * {@link UserConstants#USERS_PATH default path} for creating users.
     */
    public static final String PARAM_USERS_PATH = "usersPath";

    /**
     * Configuration option to change the
     * {@link UserConstants#GROUPS_PATH default path} for creating groups.
     */
    public static final String PARAM_GROUPS_PATH = "groupsPath";

    /**
     * @deprecated Use {@link #PARAM_COMPATIBLE_JR16} instead.
     */
    public static final String PARAM_COMPATIBILE_JR16 = "compatibleJR16";

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
    public static final String PARAM_COMPATIBLE_JR16 = "compatibleJR16";

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

    /**
     * If this parameter is present group memberships are collected in a node
     * structure below {@link UserConstants#N_MEMBERS} instead of the default
     * multi valued property {@link UserConstants#P_MEMBERS}. Its value determines
     * the maximum number of member properties until additional intermediate nodes
     * are inserted. Valid values are integers > 4.
     */
    public static final String PARAM_GROUP_MEMBERSHIP_SPLIT_SIZE = "groupMembershipSplitSize";

    private static final Logger log = LoggerFactory.getLogger(UserManagerImpl.class);

    private final SessionImpl session;
    private final String adminId;
    private final NodeResolver authResolver;
    private final NodeCreator nodeCreator;

    /**
     * Configuration value defining the node where User nodes will be created.
     * Default value is {@link UserConstants#USERS_PATH}.
     */
    private final String usersPath;

    /**
     * Configuration value defining the node where Group nodes will be created.
     * Default value is {@link UserConstants#GROUPS_PATH}.
     */
    private final String groupsPath;

    /**
     * Flag indicating if {@link #getAuthorizable(String)} should be able to deal
     * with users or groups created with Jackrabbit < 2.0.<br>
     * As of 2.0 authorizables are created using a defined logic that allows
     * to retrieve them without searching/traversing. If this flag is
     * <code>true</code> this method will try to find authorizables using the
     * <code>authResolver</code> if not found otherwise.
     */
    private final boolean compatibleJR16;

    /**
     * Maximum number of properties on the group membership node structure under
     * {@link UserConstants#N_MEMBERS} until additional intermediate nodes are inserted.
     * If 0 (default), {@link UserConstants#P_MEMBERS} is used to record group
     * memberships.
     */
    private final int groupMembershipSplitSize;

    /**
     * The membership cache.
     */
    private final MembershipCache membershipCache;

    /**
     * Authorizable actions that will all be executed upon creation and removal
     * of authorizables in the order they are contained in the array.<p/>
     * Note, that if {@link #isAutoSave() autosave} is turned on, the configured
     * actions are executed before persisting the creation or removal.
     */
    private AuthorizableAction[] authorizableActions = new AuthorizableAction[0];

    /**
     * Create a new <code>UserManager</code> with the default configuration.
     *
     * @param session The editing/reading session.
     * @param adminId The user ID of the administrator.
     * @throws javax.jcr.RepositoryException If an error occurs.
     */
    public UserManagerImpl(SessionImpl session, String adminId) throws RepositoryException {
        this(session, adminId, null, null);
    }

    /**
     * Create a new <code>UserManager</code>
     *
     * @param session The editing/reading session.
     * @param adminId The user ID of the administrator.
     * @param config The configuration parameters.
     * @throws javax.jcr.RepositoryException If an error occurs.
     */
    public UserManagerImpl(SessionImpl session, String adminId, Properties config) throws RepositoryException {
        this(session, adminId, config, null);
    }

    /**
     * Create a new <code>UserManager</code> for the given <code>session</code>.
     * Currently the following configuration options are respected:
     *
     * <ul>
     * <li>{@link #PARAM_USERS_PATH}. If missing set to {@link UserConstants#USERS_PATH}.</li>
     * <li>{@link #PARAM_GROUPS_PATH}. If missing set to {@link UserConstants#GROUPS_PATH}.</li>
     * <li>{@link #PARAM_DEFAULT_DEPTH}. The default number of levels is 2.</li>
     * <li>{@link #PARAM_AUTO_EXPAND_TREE}. By default this option is disabled.</li>
     * <li>{@link #PARAM_AUTO_EXPAND_SIZE}. The default value is 1000.</li>
     * <li>{@link #PARAM_GROUP_MEMBERSHIP_SPLIT_SIZE}. The default is 0 which means use
     * {@link UserConstants#P_MEMBERS}.</li>
     * </ul>
     *
     * See the overall {@link UserManagerImpl introduction} for details.
     *
     * @param session The editing/reading session.
     * @param adminId The user ID of the administrator.
     * @param config The configuration parameters.
     * @param mCache Shared membership cache.
     * @throws javax.jcr.RepositoryException If an error occurs.
     */
    public UserManagerImpl(SessionImpl session, String adminId, Properties config,
                           MembershipCache mCache) throws RepositoryException {
        this.session = session;
        this.adminId = adminId;

        nodeCreator = new NodeCreator(config);

        Object param = (config != null) ? config.get(PARAM_USERS_PATH) : null;
        usersPath = (param != null) ? param.toString() : USERS_PATH;

        param = (config != null) ? config.get(PARAM_GROUPS_PATH) : null;
        groupsPath = (param != null) ? param.toString() : GROUPS_PATH;

        param = (config != null) ? config.get(PARAM_COMPATIBLE_JR16) : null;
        compatibleJR16 = (param != null) && Boolean.parseBoolean(param.toString());

        param = (config != null) ? config.get(PARAM_GROUP_MEMBERSHIP_SPLIT_SIZE) : null;
        groupMembershipSplitSize = parseMembershipSplitSize(param);

        if (mCache != null) {
            membershipCache = mCache;
        } else {
            membershipCache = new MembershipCache(session, groupsPath, groupMembershipSplitSize > 0);
        }

        NodeResolver nr;
        try {
            nr = new IndexNodeResolver(session, session);
        } catch (RepositoryException e) {
            log.debug("UserManager: no QueryManager available for workspace '" + session.getWorkspace().getName() + "' -> Use traversing node resolver.");
            nr = new TraversingNodeResolver(session, session);
        }
        authResolver = nr;
        authResolver.setSearchRoots(usersPath, groupsPath);
    }

    /**
     * Implementation specific methods revealing where users are created within
     * the content.
     *
     * @return root path for user content.
     * @see #PARAM_USERS_PATH For the corresponding configuration parameter.
     */
    public String getUsersPath() {
        return usersPath;
    }

    /**
     * Implementation specific methods revealing where groups are created within
     * the content.
     *
     * @return root path for group content.
     * @see #PARAM_GROUPS_PATH For the corresponding configuration parameter.
     */
    public String getGroupsPath() {
        return groupsPath;
    }

    /**
     * @return The membership cache present with this user manager instance.
     */
    public MembershipCache getMembershipCache() {
        return membershipCache;
    }  

    /**
     * Maximum number of properties on the group membership node structure under
     * {@link UserConstants#N_MEMBERS} until additional intermediate nodes are inserted.
     * If 0 (default), {@link UserConstants#P_MEMBERS} is used to record group
     * memberships.
     *
     * @return The maximum number of group members before splitting up the structure.
     */
    public int getGroupMembershipSplitSize() {
        return groupMembershipSplitSize;
    }

    /**
     * Set the authorizable actions that will be invoked upon authorizable
     * creation and removal.
     *
     * @param authorizableActions An array of authorizable actions.
     */
    public void setAuthorizableActions(AuthorizableAction[] authorizableActions) {
        if (authorizableActions != null) {
            this.authorizableActions = authorizableActions;
        }
    }

    //--------------------------------------------------------< UserManager >---
    /**
     * @see UserManager#getAuthorizable(String)
     */
    public Authorizable getAuthorizable(String id) throws RepositoryException {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("Invalid authorizable name '" + id + "'");
        }
        Authorizable a = internalGetAuthorizable(id);
        /**
         * Extra check for the existence of the administrator user that must
         * always exist.
         * In case it got removed if must be recreated using a system session.
         * Since a regular session may lack read permission on the admin-user's
         * node an explicit test for the current editing session being
         * a system session is performed.
         */
        if (a == null && adminId.equals(id) && session.isSystem()) {
            log.info("Admin user does not exist.");
            a = createAdmin();
        }

        return a;
    }

    /**
     * @see UserManager#getAuthorizable(Principal)
     */
    public Authorizable getAuthorizable(Principal principal) throws RepositoryException {
        NodeImpl n = null;
        // shortcuts that avoids executing a query.
        if (principal instanceof AuthorizableImpl.NodeBasedPrincipal) {
            NodeId nodeId = ((AuthorizableImpl.NodeBasedPrincipal) principal).getNodeId();
            try {
                n = session.getNodeById(nodeId);
            } catch (ItemNotFoundException e) {
                // no such authorizable -> null
            }
        } else if (principal instanceof ItemBasedPrincipal) {
            String authPath = ((ItemBasedPrincipal) principal).getPath();
            if (session.nodeExists(authPath)) {
                n = (NodeImpl) session.getNode(authPath);
            }
        } else {
            // another Principal implementation.
            // a) try short-cut that works in case of ID.equals(principalName) only.
            // b) execute query in case of pName mismatch or exception. however, query
            //    requires persisted user nodes (see known issue of UserImporter).
            String name = principal.getName();
            try {
                Authorizable a = internalGetAuthorizable(name);
                if (a != null && name.equals(a.getPrincipal().getName())) {
                    return a;
                }
            } catch (RepositoryException e) {
                // ignore and execute the query.
            }
            // authorizable whose ID matched the principal name -> search.
            n = (NodeImpl) authResolver.findNode(P_PRINCIPAL_NAME, name, NT_REP_AUTHORIZABLE);
        }
        // build the corresponding authorizable object
        return getAuthorizable(n);
    }

    /**
     * Always throws <code>UnsupportedRepositoryOperationException</code> since
     * this implementation of the user management API does not allow to retrieve
     * the path of an authorizable.
     * 
     * @see UserManager#getAuthorizableByPath(String)
     */
    public Authorizable getAuthorizableByPath(String path) throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @see UserManager#findAuthorizables(String,String)
     */
    public Iterator<Authorizable> findAuthorizables(String relPath, String value) throws RepositoryException {
        return findAuthorizables(relPath, value, SEARCH_TYPE_AUTHORIZABLE);
    }

    /**
     * @see UserManager#findAuthorizables(String,String, int)
     */
    public Iterator<Authorizable> findAuthorizables(String relPath, String value, int searchType)
            throws RepositoryException {
        if (searchType < SEARCH_TYPE_USER || searchType > SEARCH_TYPE_AUTHORIZABLE) {
            throw new IllegalArgumentException("Invalid search type " + searchType);
        }

        Path path = session.getQPath(relPath);
        NodeIterator nodes;
        if (relPath.indexOf('/') == -1) {
            // search for properties somewhere below an authorizable node
            nodes = authResolver.findNodes(path, value, searchType, true, Long.MAX_VALUE);
        } else {
            path = path.getNormalizedPath();
            if (path.getLength() == 1) {
                // only search below the authorizable node
                Name ntName;
                switch (searchType) {
                    case SEARCH_TYPE_GROUP:
                        ntName = NT_REP_GROUP;
                        break;
                    case SEARCH_TYPE_USER:
                        ntName = NT_REP_USER;
                        break;
                    default:
                        ntName = NT_REP_AUTHORIZABLE;
                }
                nodes = authResolver.findNodes(path.getName(), value, ntName, true);
            } else {
                // search below authorizable nodes but take some path constraints
                // into account.
                nodes = authResolver.findNodes(path, value, searchType, true, Long.MAX_VALUE);            
            }
        }
        return new AuthorizableIterator(nodes);
    }

    /**
     * @see UserManager#findAuthorizables(Query)
     */
    public Iterator<Authorizable> findAuthorizables(Query query) throws RepositoryException {
        XPathQueryBuilder builder = new XPathQueryBuilder();
        query.build(builder);
        return new XPathQueryEvaluator(builder, this, session).eval();
    }

    /**
     * @see UserManager#createUser(String,String)
     */
    public User createUser(String userID, String password) throws RepositoryException {
        return createUser(userID, password, new PrincipalImpl(userID), null);
    }

    /**
     * @see UserManager#createUser(String, String, java.security.Principal, String)
     */
    public User createUser(String userID, String password,
                           Principal principal, String intermediatePath)
            throws AuthorizableExistsException, RepositoryException {
        checkValidID(userID);
        if (password == null) {
            throw new IllegalArgumentException("Cannot create user: null password.");
        }
        // NOTE: principal validation during setPrincipal call.

        try {
            NodeImpl userNode = (NodeImpl) nodeCreator.createUserNode(userID, intermediatePath);
            setPrincipal(userNode, principal);
            setProperty(userNode, P_PASSWORD, getValue(UserImpl.buildPasswordValue(password)), true);

            User user = createUser(userNode);
            onCreate(user, password);
            if (isAutoSave()) {
                session.save();
            }

            log.debug("User created: " + userID + "; " + userNode.getPath());
            return user;
        } catch (RepositoryException e) {
            // something went wrong -> revert changes and re-throw
            session.refresh(false);
            log.debug("Failed to create new User, reverting changes.");
            throw e;
        }
    }

    /**
     * @see UserManager#createGroup(String)
     */
    public Group createGroup(String groupID)
    		throws AuthorizableExistsException, RepositoryException {
    	return createGroup(groupID, new PrincipalImpl(groupID), null);
    }
    
    /**
     * Same as {@link #createGroup(java.security.Principal, String)} where the
     * intermediate path is <code>null</code>.
     * @see UserManager#createGroup(Principal)
     */
    public Group createGroup(Principal principal) throws RepositoryException {
        return createGroup(principal, null);
    }

    /**
     * Same as {@link #createGroup(String, Principal, String)} where a groupID
     * is generated from the principal name. If the name conflicts with an
     * existing authorizable ID (may happen in cases where
     * principal name != ID) the principal name is expanded by a suffix;
     * otherwise the resulting group ID equals the principal name.
     *
     * @param principal A principal that doesn't yet represent an existing user
     * or group.
     * @param intermediatePath Is always ignored.
     * @return A new group.
     * @throws AuthorizableExistsException
     * @throws RepositoryException
     * @see UserManager#createGroup(java.security.Principal, String)
     */
    public Group createGroup(Principal principal, String intermediatePath) throws AuthorizableExistsException, RepositoryException {
        checkValidPrincipal(principal, true);
        
        String groupID = getGroupId(principal.getName());
        return createGroup(groupID, principal, intermediatePath);
    }

    /**
     * Create a new <code>Group</code> from the given <code>groupID</code> and
     * <code>principal</code>. It will be created below the defined
     * {@link #getGroupsPath() group path}.<br>
     * Non-existent elements of the Path will be created as nodes
     * of type {@link #NT_REP_AUTHORIZABLE_FOLDER rep:AuthorizableFolder}.
     *
     * @param groupID A groupID that hasn't been used before for another
     * user or group.
     * @param principal A principal that doesn't yet represent an existing user
     * or group.
     * @param intermediatePath Is always ignored.
     * @return A new group.
     * @throws AuthorizableExistsException
     * @throws RepositoryException
     * @see UserManager#createGroup(String, java.security.Principal, String)
     */
    public Group createGroup(String groupID, Principal principal, String intermediatePath) throws AuthorizableExistsException, RepositoryException {
        checkValidID(groupID);
        // NOTE: principal validation during setPrincipal call.
        try {
            NodeImpl groupNode = (NodeImpl) nodeCreator.createGroupNode(groupID, intermediatePath);
            
            if (principal != null) {
            	setPrincipal(groupNode, principal);
            }

            Group group = createGroup(groupNode);
            onCreate(group);
            if (isAutoSave()) {
                session.save();
            }

            log.debug("Group created: " + groupID + "; " + groupNode.getPath());
            return group;
        } catch (RepositoryException e) {
            session.refresh(false);
            log.debug("newInstance new Group failed, revert changes on parent");
            throw e;
        }
    }

    /**
     * Always returns <code>true</code> as by default the autoSave behavior
     * cannot be altered (see also {@link #autoSave(boolean)}.
     *
     * @return Always <code>true</code>.
     * @see org.apache.jackrabbit.api.security.user.UserManager#isAutoSave()
     */
    public boolean isAutoSave() {
        return true;
    }

    /**
     * Always throws <code>unsupportedRepositoryOperationException</code> as
     * modification of the autosave behavior is not supported.
     *
     * @see UserManager#autoSave(boolean)
     */
    public void autoSave(boolean enable) throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException("Cannot change autosave behavior.");
    }

    //--------------------------------------------------------------------------
    /**
     *
     * @param node The new user/group node.
     * @param principal A valid non-null principal.
     * @throws AuthorizableExistsException If there is already another user/group
     * with the same principal name.
     * @throws RepositoryException If another error occurs.
     */
    void setPrincipal(NodeImpl node, Principal principal) throws AuthorizableExistsException, RepositoryException {
        checkValidPrincipal(principal, node.isNodeType(NT_REP_GROUP));        
        /*
         Check if there is *another* authorizable with the same principal.
         The additional validation (nodes not be same) is required in order to
         circumvent problems with re-importing existing authorizable in which
         case the original user/group node is being recreated but the search
         used to look for an colliding authorizable still finds the persisted
         node.
        */
        Authorizable existing = getAuthorizable(principal);
        if (existing != null && !((AuthorizableImpl) existing).getNode().isSame(node)) {
            throw new AuthorizableExistsException("Authorizable for '" + principal.getName() + "' already exists: ");
        }
        if (!node.isNew() || node.hasProperty(P_PRINCIPAL_NAME)) {
            throw new RepositoryException("rep:principalName can only be set once on a new node.");
        }
        setProperty(node, P_PRINCIPAL_NAME, getValue(principal.getName()), true);
    }

    void setProtectedProperty(NodeImpl node, Name propName, Value value) throws RepositoryException, LockException, ConstraintViolationException, ItemExistsException, VersionException {
        setProperty(node, propName, value);
        if (isAutoSave()) {
            node.save();
        }
    }

    void setProtectedProperty(NodeImpl node, Name propName, Value[] values) throws RepositoryException, LockException, ConstraintViolationException, ItemExistsException, VersionException {
        setProperty(node, propName, values);
        if (isAutoSave()) {
            node.save();
        }
    }

    void setProtectedProperty(NodeImpl node, Name propName, Value[] values, int type) throws RepositoryException, LockException, ConstraintViolationException, ItemExistsException, VersionException {
        setProperty(node, propName, values, type);
        if (isAutoSave()) {
            node.save();
        }
    }

    void removeProtectedItem(ItemImpl item, Node parent) throws RepositoryException, AccessDeniedException, VersionException {
        removeItem(item);
        if (isAutoSave()) {
            parent.save();
        }
    }

    NodeImpl addProtectedNode(NodeImpl parent, Name name, Name ntName) throws RepositoryException {
        NodeImpl n = addNode(parent, name, ntName);
        if (isAutoSave()) {
            parent.save();
        }
        return n;
    }

    <T> T performProtectedOperation(SessionImpl session, SessionOperation<T> operation) throws RepositoryException {
        return performProtected(session, operation);
    }

    /**
     * Implementation specific method used to retrieve a user/group by Node.
     * <code>Null</code> is returned if
     * <pre>
     * - the passed node is <code>null</code>,
     * - doesn't have the correct node type or
     * - isn't placed underneath the configured user/group tree.
     * </pre>
     *
     * @param n A user/group node.
     * @return An authorizable or <code>null</code>.
     * @throws RepositoryException If an error occurs.
     */
    Authorizable getAuthorizable(NodeImpl n) throws RepositoryException {
        Authorizable authorz = null;
        if (n != null) {
            String path = n.getPath();
            if (n.isNodeType(NT_REP_USER)) {
                if (Text.isDescendant(usersPath, path)) {
                    authorz = createUser(n);
                } else {
                    /* user node outside of configured tree -> return null */
                    log.error("User node '" + path + "' outside of configured user tree ('" + usersPath + "') -> Not a valid user.");
                }
            } else if (n.isNodeType(NT_REP_GROUP)) {
                if (Text.isDescendant(groupsPath, path)) {
                    authorz = createGroup(n);
                } else {
                    /* group node outside of configured tree -> return null */
                    log.error("Group node '" + path + "' outside of configured group tree ('" + groupsPath + "') -> Not a valid group.");
                }
            } else {
                /* else some other node type -> return null. */
                log.warn("Unexpected user/group node type " + n.getPrimaryNodeType().getName());
            }
        } /* else no matching node -> return null */
        return authorz;
    }

    /**
     * Always throws <code>UnsupportedRepositoryOperationException</code> since
     * the node may reside in a different workspace than the editing <code>Session</code>.
     */
    String getPath(Node authorizableNode) throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * Returns the session associated with this user manager.
     *
     * @return the session.
     */
    SessionImpl getSession() {
        return session;
    }

    /**
     * Test if a user or group exists that has the given principals name as ID,
     * which might happen if userID != principal-name.
     * In this case: generate another ID for the group to be created.
     *
     * @param principalName to be used as hint for the group id.
     * @return a group id.
     * @throws RepositoryException If an error occurs.
     */
    private String getGroupId(String principalName) throws RepositoryException {
        String groupID = principalName;
        int i = 0;
        while (internalGetAuthorizable(groupID) != null) {
            groupID = principalName + "_" + i;
            i++;
        }
        return groupID;
    }

    /**
     * @param id The user or group ID.
     * @return The authorizable with the given <code>id</code> or <code>null</code>.
     * @throws RepositoryException If an error occurs.
     */
    private Authorizable internalGetAuthorizable(String id) throws RepositoryException {
        NodeId nodeId = buildNodeId(id);
        NodeImpl n = null;
        try {
            n = session.getNodeById(nodeId);
        } catch (ItemNotFoundException e) {
            if (compatibleJR16) {
                // backwards-compatibility with JR < 2.0 user/group structure that doesn't
                // allow to determine existence of an authorizable from the id directly.
                // search for it the node belonging to that id
                n = (NodeImpl) authResolver.findNode(P_USERID, id, NT_REP_USER);
                if (n == null) {
                    // no user -> look for group.
                    // NOTE: JR < 2.0 always returned groupIDs that didn't contain any
                    // illegal JCR chars. Since Group.getID() 'unescapes' the node
                    // name additional escaping is required.
                    Name nodeName = session.getQName(Text.escapeIllegalJcrChars(id));
                    n = (NodeImpl) authResolver.findNode(nodeName, NT_REP_GROUP);
                }
            } // else: no matching node found -> ignore exception.
        }

        return getAuthorizable(n);
    }

    private Value getValue(String strValue) {
        return session.getValueFactory().createValue(strValue);
    }

    /**
     * @param userID A userID.
     * @return true if the given userID belongs to the administrator user.
     */
    boolean isAdminId(String userID) {
        return (adminId != null) && adminId.equals(userID);
    }

    /**
     * Build the User object from the given user node.
     *
     * @param userNode The new user node.
     * @return An instance of <code>User</code>.
     * @throws RepositoryException If the node isn't a child of the configured
     * usersPath-node or if another error occurs.
     */
    User createUser(NodeImpl userNode) throws RepositoryException {
        if (userNode == null || !userNode.isNodeType(NT_REP_USER)) {
            throw new IllegalArgumentException();
        }
        if (!Text.isDescendant(usersPath, userNode.getPath())) {
            throw new RepositoryException("User has to be within the User Path");
        }
        return doCreateUser(userNode);
    }

    /**
     * Build the user object from the given user node. May be overridden to
     * return a custom implementation.
     *
     * @param node user node
     * @return the user object
     * @throws RepositoryException if an error occurs
     */
    protected User doCreateUser(NodeImpl node) throws RepositoryException {
        return new UserImpl(node, this);
    }


    /**
     * Build the Group object from the given group node.
     *
     * @param groupNode The new group node.
     * @return An instance of <code>Group</code>.
     * @throws RepositoryException If the node isn't a child of the configured
     * groupsPath-node or if another error occurs.
     */
    Group createGroup(NodeImpl groupNode) throws RepositoryException {
        if (groupNode == null || !groupNode.isNodeType(NT_REP_GROUP)) {
            throw new IllegalArgumentException();
        }
        if (!Text.isDescendant(groupsPath, groupNode.getPath())) {
            throw new RepositoryException("Group has to be within the Group Path");
        }
        return doCreateGroup(groupNode);
    }

    /**
     * Build the group object from the given group node. May be overridden to
     * return a custom implementation.
     *
     * @param node group node
     * @return A group
     * @throws RepositoryException if an error occurs
     */
    protected Group doCreateGroup(NodeImpl node) throws RepositoryException {
        return new GroupImpl(node, this);
    }

    /**
     * Create the administrator user. If the node to be created collides
     * with an existing node (ItemExistsException) the existing node gets removed
     * and the admin user node is (re)created.
     * <p/>
     * Collision with an existing node may occur under the following circumstances:
     *
     * <ul>
     * <li>The <code>usersPath</code> has been modified in the user manager
     * configuration after a successful repository start that already created
     * the administrator user.</li>
     * <li>The NodeId created by {@link #buildNodeId(String)} by coincidence
     * collides with another NodeId created during the regular node creation
     * process.</li>
     * </ul>
     *
     * @return The admin user.
     * @throws RepositoryException If an error occurs.
     */
    private User createAdmin() throws RepositoryException {
        User admin;
        try {
            admin = createUser(adminId, adminId);
            if (!isAutoSave()) {
                session.save();
            }
            log.info("... created admin user with id \'" + adminId + "\' and default pw.");
        } catch (ItemExistsException e) {
            NodeImpl conflictingNode = session.getNodeById(buildNodeId(adminId));
            String conflictPath = conflictingNode.getPath();
            log.error("Detected conflicting node " + conflictPath + " of node type " + conflictingNode.getPrimaryNodeType().getName() + ".");

            // TODO move conflicting node of type rep:User instead of removing and recreating.
            conflictingNode.remove();
            log.info("Removed conflicting node at " + conflictPath);

            admin = createUser(adminId, adminId);
            if (!isAutoSave()) {
                session.save();
            }
            log.info("Resolved conflict and (re)created admin user with id \'" + adminId + "\' and default pw.");
        }
        return admin;
    }

    /**
     * Creates a UUID from the given <code>id</code> String that is converted
     * to lower case before.
     *
     * @param id The user/group id that needs to be converted to a valid NodeId.
     * @return a new <code>NodeId</code>.
     * @throws RepositoryException If an error occurs.
     */
    private NodeId buildNodeId(String id) throws RepositoryException {
        try {
            UUID uuid = UUID.nameUUIDFromBytes(id.toLowerCase().getBytes("UTF-8"));
            return new NodeId(uuid);
        } catch (UnsupportedEncodingException e) {
            throw new RepositoryException("Unexpected error while build ID hash", e);
        }
    }

    /**
     * Checks if the specified <code>id</code> is a non-empty string and not yet
     * in use for another user or group.
     *
     * @param id The id of the user or group to be created.
     * @throws IllegalArgumentException If the specified id is null or empty string.
     * @throws AuthorizableExistsException If the id is already in use.
     * @throws RepositoryException If another error occurs.
     */
    private void checkValidID(String id) throws IllegalArgumentException, AuthorizableExistsException, RepositoryException {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("Cannot create authorizable: ID can neither be null nor empty String.");
        }
        if (internalGetAuthorizable(id) != null) {
            throw new AuthorizableExistsException("User or Group for '" + id + "' already exists");
        }
    }

    /**
     * Throws <code>IllegalArgumentException</code> if the specified principal
     * is <code>null</code> or if it's name is <code>null</code> or empty string.
     * @param principal The principal to be validated.
     * @param isGroup Flag indicating if the principal represents a group.
     */
    private static void checkValidPrincipal(Principal principal, boolean isGroup) {
        if (principal == null || principal.getName() == null || "".equals(principal.getName())) {
            throw new IllegalArgumentException("Principal may not be null and must have a valid name.");
        }
        if (!isGroup && EveryonePrincipal.NAME.equals(principal.getName())) {
            throw new IllegalArgumentException("'everyone' is a reserved group principal name.");
        }
    }

    private static int parseMembershipSplitSize(Object param) {
        int n = 0;
        if (param != null) {
            try {
                n = Integer.parseInt(param.toString());
                if (n < 4) {
                    n = 0;
                }
            }
            catch (NumberFormatException e) {
                n = 0;
            }
            if (n == 0) {
                log.warn("Invalid value {} for {}. Expected integer >= 4",
                        param.toString(), PARAM_GROUP_MEMBERSHIP_SPLIT_SIZE);
            }
        }

        return n;
    }

    //--------------------------------------------------------------------------
    /**
     * Let the configured <code>AuthorizableAction</code>s perform additional
     * tasks associated with the creation of the new user before the
     * corresponding new node is persisted.
     *
     * @param user The new user.
     * @param pw The password.
     * @throws RepositoryException If an exception occurs.
     */
    void onCreate(User user, String pw) throws RepositoryException {
        for (AuthorizableAction action : authorizableActions) {
            action.onCreate(user, pw, session);
        }
    }

    /**
     * Let the configured <code>AuthorizableAction</code>s perform additional
     * tasks associated with the creation of the new group before the
     * corresponding new node is persisted.
     *
     * @param group The new group.
     * @throws RepositoryException If an exception occurs.
     */
    void onCreate(Group group) throws RepositoryException {
        for (AuthorizableAction action : authorizableActions) {
            action.onCreate(group, session);
        }
    }

    /**
     * Let the configured <code>AuthorizableAction</code>s perform any clean
     * up tasks related to the authorizable removal (before the corresponding
     * node gets removed).
     *
     * @param authorizable The authorizable to be removed.
     * @throws RepositoryException If an exception occurs.
     */
    void onRemove(Authorizable authorizable) throws RepositoryException {
        for (AuthorizableAction action : authorizableActions) {
            action.onRemove(authorizable, session);
        }
    }

    /**
     * Let the configured <code>AuthorizableAction</code>s perform additional
     * tasks associated with password changing of a given user before the
     * corresponding property is being changed.
     *
     * @param user The target user.
     * @param password The new password.
     * @throws RepositoryException If an exception occurs.
     */
    void onPasswordChange(User user, String password) throws RepositoryException {
        for (AuthorizableAction action : authorizableActions) {
            action.onPasswordChange(user, password, session);
        }
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
        // and logout the session unless it is the logged-out session itself.
        if (session != this.session) {
            this.session.logout();
        }
    }

    //------------------------------------------------------< inner classes >---
    /**
     * Inner class
     */
    private final class AuthorizableIterator implements Iterator<Authorizable> {

        private final Set<String> served = new HashSet<String>();

        private Authorizable next;
        private final NodeIterator authNodeIter;

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
        public Authorizable next() {
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
                        Authorizable authr = getAuthorizable(node);
                        served.add(node.getUUID());
                        if (authr != null) {
                            return authr;
                        }
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
     * Inner class creating the JCR nodes corresponding the a given
     * authorizable ID with the following behavior:
     * <ul>
     * <li>Users are created below /rep:security/rep:authorizables/rep:users or
     * the corresponding path configured.</li>
     * <li>Groups are created below /rep:security/rep:authorizables/rep:groups or
     * the corresponding path configured.</li>
     * <li>Below each category authorizables are created within a human readable
     * structure based on the defined intermediate path or some internal logic
     * with a depth defined by the <code>defaultDepth</code> config option.<br>
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
     * <li>If no intermediate path is passed the names of the intermediate
     * folders are calculated from the leading chars of the escaped node name.</li>
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
     * <li>Special case: If <code>autoExpandTree</code> is enabled later on
     * AND any of the existing authorizable nodes collides with an intermediate
     * folder to be created the auto-expansion is aborted and the new
     * authorizable is inserted at the last valid level irrespective of
     * max-size being reached.
     * </li>
     * </ul>
     *
     * The configuration options:
     * <ul>
     * <li><strong>defaultDepth</strong>:<br>
     * A positive <code>integer</code> greater than zero defining the depth of
     * the default structure that is always created.<br>
     * Default value: 2</li>
     * <li><strong>autoExpandTree</strong>:<br>
     * <code>boolean</code> defining if the tree gets automatically expanded
     * if within a level the maximum number of child nodes is reached.<br>
     * Default value: <code>false</code></li>
     * <li><strong>autoExpandSize</strong>:<br>
     * A positive <code>long</code> greater than zero defining the maximum
     * number of child nodes that are allowed at a given level.<br>
     * Default value: 1000<br>
     * NOTE: that total number of child nodes may still be greater that
     * autoExpandSize.</li>
     * </ul>
     */
    private class NodeCreator {

        private static final String DELIMITER = "/";
        private static final int DEFAULT_DEPTH = 2;
        private static final long DEFAULT_SIZE = 1000;

        private final int defaultDepth;
        private final boolean autoExpandTree;
        // best effort max-size of authorizables per folder. there may be
        // more nodes created if the editing session isn't allowed to see
        // all child nodes.
        private final long autoExpandSize;

        private NodeCreator(Properties config) {
            int d = DEFAULT_DEPTH;
            boolean expand = false;
            long size = DEFAULT_SIZE;

            if (config != null) {
                if (config.containsKey(PARAM_DEFAULT_DEPTH)) {
                    try {
                        d = Integer.parseInt(config.get(PARAM_DEFAULT_DEPTH).toString());
                        if (d <= 0) {
                           log.warn("Invalid defaultDepth '" + d + "' -> using default.");
                           d = DEFAULT_DEPTH;
                        }
                    } catch (NumberFormatException e) {
                        log.warn("Unable to parse defaultDepth config parameter -> using default.", e);
                    }
                }
                if (config.containsKey(PARAM_AUTO_EXPAND_TREE)) {
                    expand = Boolean.parseBoolean(config.get(PARAM_AUTO_EXPAND_TREE).toString());
                }
                if (config.containsKey(PARAM_AUTO_EXPAND_SIZE)) {
                    try {
                        size = Integer.parseInt(config.get(PARAM_AUTO_EXPAND_SIZE).toString());
                        if (expand && size <= 0) {
                            log.warn("Invalid autoExpandSize '" + size + "' -> using default.");
                            size = DEFAULT_SIZE;
                        }
                    } catch (NumberFormatException e) {
                        log.warn("Unable to parse autoExpandSize config parameter -> using default.", e);
                    }
                }
            }

            defaultDepth = d;
            autoExpandTree = expand;
            autoExpandSize = size;
        }

        public Node createUserNode(String userID, String intermediatePath) throws RepositoryException {
            return createAuthorizableNode(userID, false, intermediatePath);
        }

        public Node createGroupNode(String groupID, String intermediatePath) throws RepositoryException {
            return createAuthorizableNode(groupID, true, intermediatePath);
        }

        private Node createAuthorizableNode(String id, boolean isGroup, String intermediatePath) throws RepositoryException {
            String escapedId = Text.escapeIllegalJcrChars(id);

            Node folder;
            // first create the default folder nodes, that are always present.
            folder = createDefaultFolderNodes(id, escapedId, isGroup, intermediatePath);
            // eventually create additional intermediate folders.
            if (intermediatePath == null) {
                // internal logic only
                folder = createIntermediateFolderNodes(id, escapedId, folder);
            }

            Name nodeName = session.getQName(escapedId);
            Name ntName = (isGroup) ? NT_REP_GROUP : NT_REP_USER;
            NodeId nid = buildNodeId(id);

            // check if there exists an colliding folder child node.
            while (((NodeImpl) folder).hasNode(nodeName)) {
                NodeImpl colliding = ((NodeImpl) folder).getNode(nodeName);
                if (colliding.isNodeType(NT_REP_AUTHORIZABLE_FOLDER)) {
                    log.warn("Existing folder node collides with user/group to be created. Expanding path: " + colliding.getPath());
                    folder = colliding;
                } else {
                    // should never get here as folder creation above already
                    // asserts that only rep:authorizable folders exist.
                    // similarly collisions with existing authorizable have been
                    // checked.
                    String msg = "Failed to create authorizable with id '" + id + "' : Detected conflicting node of unexpected nodetype '" + colliding.getPrimaryNodeType().getName() + "'.";
                    log.error(msg);
                    throw new ConstraintViolationException(msg);
                }
            }

            // check for collision with existing node outside of the user/group tree
            if (session.getItemManager().itemExists(nid)) {
                String msg = "Failed to create authorizable with id '" + id + "' : Detected conflict with existing node (NodeID: " + nid + ")";
                log.error(msg);
                throw new ItemExistsException(msg);
            }

            // finally create the authorizable node
            return addNode((NodeImpl) folder, nodeName, ntName, nid);
        }

        private Node createDefaultFolderNodes(String id, String escapedId,
                                              boolean isGroup, String intermediatePath) throws RepositoryException {

            String defaultPath = getDefaultFolderPath(id, isGroup, intermediatePath);

            // make sure users/groups are never nested and exclusively created
            // under a tree of rep:AuthorizableFolder(s) starting at usersPath
            // or groupsPath, respectively. ancestors of the usersPath/groupsPath
            // may or may not be rep:AuthorizableFolder(s).
            // therefore the shortcut Session.getNode(defaultPath) is omitted.
            String[] segmts = defaultPath.split("/");
            NodeImpl folder = (NodeImpl) session.getRootNode();
            String authRoot = (isGroup) ? groupsPath : usersPath;

            for (String segment : segmts) {
                if (segment.length() < 1) {
                    continue;
                }
                if (folder.hasNode(segment)) {
                    folder = (NodeImpl) folder.getNode(segment);
                    if (Text.isDescendantOrEqual(authRoot, folder.getPath()) &&
                            !folder.isNodeType(NT_REP_AUTHORIZABLE_FOLDER)) {
                        throw new ConstraintViolationException("Invalid intermediate path. Must be of type rep:AuthorizableFolder.");
                    }
                } else {
                    folder = addNode(folder, session.getQName(segment), NT_REP_AUTHORIZABLE_FOLDER);
                }
            }

            // validation check if authorizable to be created doesn't conflict.
            checkAuthorizableNodeExists(escapedId, folder);
            return folder;
        }

        private String getDefaultFolderPath(String id, boolean isGroup, String intermediatePath) {
            StringBuilder bld = new StringBuilder();
            if (isGroup) {
                bld.append(groupsPath);
            } else {
                bld.append(usersPath);
            }

            if (intermediatePath == null) {
                // internal logic
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
            } else {
                // structure defined by intermediate path
                if (intermediatePath.startsWith(bld.toString())) {
                    intermediatePath = intermediatePath.substring(bld.toString().length());
                }
                if (intermediatePath.length() > 0 && !"/".equals(intermediatePath)) {
                    if (!intermediatePath.startsWith("/")) {
                        bld.append("/");
                    }
                    bld.append(intermediatePath);
                }
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
            // - if the authorizable node to be created potentially collides with
            //   any of the intermediate nodes.
            int segmLength = defaultDepth +1;

            while (intermediateFolderNeeded(escapedId, folder)) {
                String folderName = Text.escapeIllegalJcrChars(id.substring(0, segmLength));
                if (folder.hasNode(folderName)) {
                    NodeImpl n = (NodeImpl) folder.getNode(folderName);
                    // validation check: folder must be of type rep:AuthorizableFolder
                    // and not an authorizable node.
                    if (n.isNodeType(NT_REP_AUTHORIZABLE_FOLDER)) {
                        // expected nodetype -> no violation
                        folder = n;
                    } else if (n.isNodeType(NT_REP_AUTHORIZABLE)){
                        /*
                         an authorizable node has been created before with the
                         name of the intermediate folder to be created.
                         this may only occur if the 'autoExpandTree' option has
                         been enabled later on.
                         Resolution:
                         - abort auto-expanding and create the authorizable
                           at the current level, ignoring that max-size is reached.
                         - note, that this behavior has been preferred over tmp.
                           removing and recreating the colliding authorizable node.
                        */
                        log.warn("Auto-expanding aborted. An existing authorizable node '" + n.getName() +"'conflicts with intermediate folder to be created.");
                        break;
                    } else {
                        // should never get here: some other, unexpected node type
                        String msg = "Failed to create authorizable node: Detected conflict with node of unexpected nodetype '" + n.getPrimaryNodeType().getName() + "'.";
                        log.error(msg);
                        throw new ConstraintViolationException(msg);
                    }
                } else {
                    // folder doesn't exist nor does another colliding child node.
                    folder = addNode((NodeImpl) folder, session.getQName(folderName), NT_REP_AUTHORIZABLE_FOLDER);
                }
                segmLength++;
            }

            // final validation check if authorizable to be created doesn't conflict.
            checkAuthorizableNodeExists(escapedId, folder);
            return folder;
        }

        private void checkAuthorizableNodeExists(String nodeName, Node folder) throws AuthorizableExistsException, RepositoryException {
            if (folder.hasNode(nodeName) &&
                    ((NodeImpl) folder.getNode(nodeName)).isNodeType(NT_REP_AUTHORIZABLE)) {
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
                // existing collision that would result from
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
