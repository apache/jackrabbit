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

import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.ItemImpl;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.core.observation.SynchronousEventListener;
import org.apache.jackrabbit.core.security.AnonymousPrincipal;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.core.security.authorization.AbstractAccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.AbstractCompiledPermissions;
import org.apache.jackrabbit.core.security.authorization.AccessControlEditor;
import org.apache.jackrabbit.core.security.authorization.CompiledPermissions;
import org.apache.jackrabbit.core.security.authorization.NamedAccessControlPolicyImpl;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.security.authorization.PrivilegeBits;
import org.apache.jackrabbit.core.security.authorization.PrivilegeManagerImpl;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.core.security.principal.GroupPrincipals;
import org.apache.jackrabbit.core.security.principal.PrincipalImpl;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.Privilege;

import java.security.Principal;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of the <code>AccessControlProvider</code> interface that
 * is used to protected the 'security workspace' containing the user and
 * group data. It applies special care to make sure that modifying user data
 * (e.g. password), group membership and impersonation is properly controlled.
 * <p>
 * This provider creates upon initialization the following 2 groups:
 * <ul>
 * <li>User administrator</li>
 * <li>Group administrator</li>
 * </ul>
 *
 * The default access control policy defined by this provider has the following
 * characteristics:
 * <ul>
 * <li>All authenticated users have READ permission to all items. If {link #PARAM_ANONYMOUS_ACCESS}
 * is configured to be <code>true</code> this also applies to the anonymous user.</li>
 *
 * <li>every known user is allowed to modify it's own properties except for
 * her/his group membership,</li>
 *
 * <li>members of the 'User administrator' group are allowed to create, modify
 * and remove users,</li>
 *
 * <li>members of the 'Group administrator' group are allowed to create, modify
 * and remove groups,</li>
 *
 * <li>group membership can only be edited by members of the 'Group administrator'
 * and the 'User administrator' group.</li>
 * </ul>
 */
public class UserAccessControlProvider extends AbstractAccessControlProvider
        implements UserConstants {

    private static Logger log = LoggerFactory.getLogger(UserAccessControlProvider.class);
        
    /**
     * Constant for the name of the configuration option "anonymousId".
     * The option is a flag indicating the name of the anonymous user id.
     */
    public static final String PARAM_ANONYMOUS_ID = "anonymousId";
    
    /**
     * Constant for the name of the configuration option "anonymousAccess".
     */
    public static final String PARAM_ANONYMOUS_ACCESS = "anonymousAccess";

    private final AccessControlPolicy policy;

    private String groupsPath;
    private String usersPath;

    private Principal userAdminGroup;
    private Principal groupAdminGroup;

    private String userAdminGroupPath;
    private String groupAdminGroupPath;
    private String administratorsGroupPath;
    private boolean membersInProperty;
    
    private String anonymousId;   
    private boolean anonymousAccess;

    /**
     *
     */
    public UserAccessControlProvider() {
        policy = new NamedAccessControlPolicyImpl("userPolicy");
    }

    //-------------------------------------------------< AccessControlUtils >---
    /**
     * Always returns false, since this ac provider does not use content stored
     * in items to evaluate AC information.
     *
     * @see org.apache.jackrabbit.core.security.authorization.AccessControlUtils#isAcItem(Path)
     */
    @Override
    public boolean isAcItem(Path absPath) throws RepositoryException {
        return false;
    }

    /**
     * Always returns false, since this ac provider does not use content stored
     * in items to evaluate AC information.
     *
     * @see org.apache.jackrabbit.core.security.authorization.AccessControlUtils#isAcItem(ItemImpl)
     */
    @Override
    public boolean isAcItem(ItemImpl item) throws RepositoryException {
        return false;
    }

    //----------------------------------------------< AccessControlProvider >---
    /**
     * @see org.apache.jackrabbit.core.security.authorization.AccessControlProvider#init(Session, Map)
     */
    @Override
    public void init(Session systemSession, Map configuration) throws RepositoryException {
        super.init(systemSession, configuration);
        if (systemSession instanceof SessionImpl) {
            SessionImpl sImpl = (SessionImpl) systemSession;
            String userAdminName = (configuration.containsKey(USER_ADMIN_GROUP_NAME)) ? configuration.get(USER_ADMIN_GROUP_NAME).toString() : USER_ADMIN_GROUP_NAME;
            String groupAdminName = (configuration.containsKey(GROUP_ADMIN_GROUP_NAME)) ? configuration.get(GROUP_ADMIN_GROUP_NAME).toString() : GROUP_ADMIN_GROUP_NAME;

            // make sure the groups exist (and possibly create them).
            UserManager uMgr = sImpl.getUserManager();
            userAdminGroup = initGroup(uMgr, userAdminName);
            if (userAdminGroup != null && userAdminGroup instanceof ItemBasedPrincipal) {
                userAdminGroupPath = ((ItemBasedPrincipal) userAdminGroup).getPath();
            }
            groupAdminGroup = initGroup(uMgr, groupAdminName);
            if (groupAdminGroup != null && groupAdminGroup instanceof ItemBasedPrincipal) {
                groupAdminGroupPath = ((ItemBasedPrincipal) groupAdminGroup).getPath();
            }

            Principal administrators = initGroup(uMgr, SecurityConstants.ADMINISTRATORS_NAME);
            if (administrators != null && administrators instanceof ItemBasedPrincipal) {
                administratorsGroupPath = ((ItemBasedPrincipal) administrators).getPath();
            }
            usersPath = (uMgr instanceof UserManagerImpl) ? ((UserManagerImpl) uMgr).getUsersPath() : UserConstants.USERS_PATH;
            groupsPath = (uMgr instanceof UserManagerImpl) ? ((UserManagerImpl) uMgr).getGroupsPath() : UserConstants.GROUPS_PATH;

            membersInProperty = !(uMgr instanceof UserManagerImpl) || !((UserManagerImpl) uMgr).hasMemberSplitSize();

            if (configuration.containsKey(PARAM_ANONYMOUS_ID)) {
                anonymousId = (String) configuration.get(PARAM_ANONYMOUS_ID);
            } else {
                anonymousId = SecurityConstants.ANONYMOUS_ID;
            }
            
            if (configuration.containsKey(PARAM_ANONYMOUS_ACCESS)) {
                anonymousAccess = Boolean.parseBoolean((String) configuration.get(PARAM_ANONYMOUS_ACCESS));
            } else {
                anonymousAccess = true;
            }
            
        } else {
            throw new RepositoryException("SessionImpl (system session) expected.");
        }
    }

    /**
     * @see org.apache.jackrabbit.core.security.authorization.AccessControlProvider#getEffectivePolicies(org.apache.jackrabbit.spi.Path,org.apache.jackrabbit.core.security.authorization.CompiledPermissions)
     */
    public AccessControlPolicy[] getEffectivePolicies(Path absPath, CompiledPermissions permissions) throws ItemNotFoundException, RepositoryException {
        checkInitialized();
        return new AccessControlPolicy[] {policy};
    }

    /**
     * @see org.apache.jackrabbit.core.security.authorization.AccessControlProvider#getEffectivePolicies(java.util.Set, CompiledPermissions)
     */
    public AccessControlPolicy[] getEffectivePolicies(Set<Principal> principals, CompiledPermissions permission) throws ItemNotFoundException, RepositoryException {
        checkInitialized();
        return new AccessControlPolicy[] {policy};
    }

    /**
     * Always returns <code>null</code>.
     *
     * @see org.apache.jackrabbit.core.security.authorization.AccessControlProvider#getEditor(Session)
     */
    public AccessControlEditor getEditor(Session session) {
        checkInitialized();
        // not editable at all: policy is always the default and cannot be
        // changed using the JCR API.
        return null;
    }

    /**
     * @see org.apache.jackrabbit.core.security.authorization.AccessControlProvider#compilePermissions(Set)
     */
    public CompiledPermissions compilePermissions(Set<Principal> principals) throws RepositoryException {
        checkInitialized();
        if (isAdminOrSystem(principals)) {
            return getAdminPermissions();
        } else {
            if (!anonymousAccess && isAnonymous(principals))  {
                return CompiledPermissions.NO_PERMISSION;
            }
            
            // determined the 'user' present in the given set of principals.
            ItemBasedPrincipal userPrincipal = getUserPrincipal(principals);
            NodeImpl userNode = getUserNode(userPrincipal);
            if (userNode == null) {
                // no 'user' within set of principals -> no permissions in the
                // security workspace.
                return CompiledPermissions.NO_PERMISSION;
            } else {
                return new CompiledPermissionsImpl(principals, userNode.getPath());
            }
        }
    }

    /**
     * @see org.apache.jackrabbit.core.security.authorization.AccessControlProvider#canAccessRoot(Set)
     */
    public boolean canAccessRoot(Set<Principal> principals) throws RepositoryException {
        checkInitialized();
        if (!anonymousAccess && isAnonymous(principals))  {
            return false;
        }
        return true;
    }

    //------------------------------------------------------------< private >---

    private ItemBasedPrincipal getUserPrincipal(Set<Principal> principals) {
        try {
            UserManager uMgr = session.getUserManager();
            for (Principal p : principals) {
                if (!(GroupPrincipals.isGroup(p)) && p instanceof ItemBasedPrincipal
                        && uMgr.getAuthorizable(p) != null) {
                    return (ItemBasedPrincipal) p;
                }
            }
        } catch (RepositoryException e) {
            // should never get here
            log.error("Internal error while retrieving user principal: {}", e.getMessage());
        }
        // none of the principals in the set is assigned to a User.
        return null;
    }

    private NodeImpl getUserNode(ItemBasedPrincipal principal) {
        NodeImpl userNode = null;
        if (principal != null) {
            try {
                String path = principal.getPath();
                userNode = (NodeImpl) session.getNode(path);
            } catch (RepositoryException e) {
                log.warn("Error while retrieving user node. {}", e.getMessage());
            }
        }
        return userNode;
    }

    private Node getExistingNode(Path path) throws RepositoryException {
        String absPath = session.getJCRPath(path.getNormalizedPath());
        if (session.nodeExists(absPath)) {
            return session.getNode(absPath);
        } else if (session.propertyExists(absPath)) {
            return session.getProperty(absPath).getParent();
        } else {
            String pPath = Text.getRelativeParent(absPath, 1);
            while (!"/".equals(pPath)) {
                if (session.nodeExists(pPath)) {
                    return session.getNode(pPath);
                } else {
                    pPath = Text.getRelativeParent(pPath, 1);
                }
            }
            throw new ItemNotFoundException("Unable to determine permissions: No item and no existing parent for target path " + absPath);
        }
    }

    private static boolean containsGroup(Set<Principal> principals, Principal group) {
        for (Iterator<Principal> it = principals.iterator(); it.hasNext() && group != null;) {
            Principal p = it.next();
            if (p.getName().equals(group.getName())) {
                return true;
            }
        }
        return false;
    }

    private static Principal initGroup(UserManager uMgr, String principalName) {
        Principal prnc = new PrincipalImpl(principalName);
        try {
            Authorizable auth = uMgr.getAuthorizable(prnc);
            if (auth == null) {
                auth = uMgr.createGroup(prnc);
            } else {
                if (!auth.isGroup()) {
                    log.warn("Cannot create group '" + principalName + "'; User with that principal already exists.");
                    auth = null;
                }
            }
            if (auth != null) {
                return auth.getPrincipal();
            }
        } catch (RepositoryException e) {
            // should never get here
            log.error("Error while initializing user/group administrators: {}", e.getMessage());
        }
        return null;
    }

    private boolean isAnonymous(Set<Principal> principals) {
        for (Principal p : principals) {
            if (p instanceof AnonymousPrincipal) {
                return true;
            } else if (p.getName().equals(anonymousId)) {
                return true;
            }
        }
        return false;
    }

    //--------------------------------------------------------< inner class >---
    /**
     *
     */
    private class CompiledPermissionsImpl extends AbstractCompiledPermissions
            implements SynchronousEventListener {

        private final String userNodePath;
        private final Set<Principal> principals;

        protected CompiledPermissionsImpl(Set<Principal> principals, String userNodePath) throws RepositoryException {
            this.userNodePath = userNodePath;
            this.principals = principals;

            int events = Event.PROPERTY_CHANGED | Event.PROPERTY_ADDED | Event.PROPERTY_REMOVED;
            observationMgr.addEventListener(this, events, groupsPath, true, null, null, false);
        }

        private PrivilegeBits getPrivilegeBits(String... privNames) throws RepositoryException {
            PrivilegeManagerImpl impl = getPrivilegeManagerImpl();
            Name[] names = new Name[privNames.length];
            for (int i = 0; i < privNames.length; i++) {
                names[i] = session.getQName(privNames[i]);
            }
            return impl.getBits(names);
        }

        private PrivilegeBits assertModifiable(PrivilegeBits bits) {
            if (bits.isModifiable()) {
                return bits;
            } else {
                return PrivilegeBits.getInstance(bits);
            }
        }

        //------------------------------------< AbstractCompiledPermissions >---
        /**
         * @see AbstractCompiledPermissions#buildResult(Path)
         */
        @Override
        protected Result buildResult(Path path) throws RepositoryException {
            NodeImpl userNode = null;
            try {
                if (session.nodeExists(userNodePath)) {
                    userNode = (NodeImpl) session.getNode(userNodePath);
                }
            } catch (RepositoryException e) {
                // ignore
            }

            if (userNode == null) {
                // no Node corresponding to user for which the permissions are
                // calculated -> no permissions/privileges.
                log.debug("No node at " + userNodePath);
                return Result.EMPTY;
            }

            // no explicit denied permissions:
            int denies = Permission.NONE;
            // default allow permission and default privileges
            int allows = Permission.READ;
            PrivilegeBits privs;
            // Determine if for path, the set of privileges must be calculated:
            // Generally, privileges can only be determined for existing nodes.
            String jcrPath = session.getJCRPath(path.getNormalizedPath());
            boolean calcPrivs = session.nodeExists(jcrPath);
            if (calcPrivs) {
                privs = getPrivilegeBits(Privilege.JCR_READ);
            } else {
                privs = PrivilegeBits.EMPTY;
            }

            if (Text.isDescendant(usersPath, jcrPath)) {
                boolean isUserAdmin = containsGroup(principals, userAdminGroup);
                /*
                 below the user-tree
                 - determine position of target relative to the editing user
                 - target may not be below an existing user but only below an
                   authorizable folder.
                 - determine if the editing user is user-admin
                 */
                NodeImpl node = (NodeImpl) getExistingNode(path);
                if (node.isNodeType(NT_REP_AUTHORIZABLE_FOLDER)) {
                    // an authorizable folder -> must be user admin in order
                    // to have permission to write.
                    if (isUserAdmin) {
                        allows |= (Permission.ADD_NODE | Permission.REMOVE_NODE | Permission.SET_PROPERTY | Permission.REMOVE_PROPERTY | Permission.NODE_TYPE_MNGMT);
                        if (calcPrivs) {
                            // grant WRITE privilege
                            // note: ac-read/modification is not included
                            privs = assertModifiable(privs);
                            privs.add(getPrivilegeBits(PrivilegeRegistry.REP_WRITE));
                        }
                    }
                } else {
                    // rep:User node or some other custom node below an existing user.
                    // as the authorizable folder doesn't allow other residual
                    // child nodes.
                    boolean editingOwnUser = node.isSame(userNode);
                    if (editingOwnUser) {
                        // user can only read && write his own props
                        allows |= (Permission.SET_PROPERTY | Permission.REMOVE_PROPERTY);
                        if (calcPrivs) {
                            privs = assertModifiable(privs);
                            privs.add(getPrivilegeBits(Privilege.JCR_MODIFY_PROPERTIES));
                        }
                    } else if (isUserAdmin) {
                        allows |= (Permission.ADD_NODE | Permission.REMOVE_NODE | Permission.SET_PROPERTY | Permission.REMOVE_PROPERTY | Permission.NODE_TYPE_MNGMT);
                        if (calcPrivs) {
                            // grant WRITE privilege
                            // note: ac-read/modification is not included
                            privs = assertModifiable(privs);
                            privs.add(getPrivilegeBits(PrivilegeRegistry.REP_WRITE));
                        }
                    } // else: normal user that isn't allowed to modify another user.
                }
            } else if (Text.isDescendant(groupsPath, jcrPath)) {
                boolean isGroupAdmin = containsGroup(principals, groupAdminGroup);
                /*
                below group-tree:
                - test if the user is group-administrator.
                - make sure group-admin cannot modify user-admin or administrators
                - ... and cannot remove itself.
                */
                if (isGroupAdmin) {
                    if (!jcrPath.startsWith(administratorsGroupPath) &&
                            !jcrPath.startsWith(userAdminGroupPath)) {
                        if (jcrPath.equals(groupAdminGroupPath)) {
                            // no remove perm on group-admin node
                            allows |= (Permission.ADD_NODE | Permission.SET_PROPERTY | Permission.REMOVE_PROPERTY | Permission.NODE_TYPE_MNGMT);
                            if (calcPrivs) {
                                privs = assertModifiable(privs);
                                privs.add(getPrivilegeBits(Privilege.JCR_ADD_CHILD_NODES, Privilege.JCR_MODIFY_PROPERTIES, Privilege.JCR_NODE_TYPE_MANAGEMENT));
                            }
                        } else {
                            // complete write
                            allows |= (Permission.ADD_NODE | Permission.REMOVE_NODE | Permission.SET_PROPERTY | Permission.REMOVE_PROPERTY | Permission.NODE_TYPE_MNGMT);
                            if (calcPrivs) {
                                privs = assertModifiable(privs);
                                privs.add(getPrivilegeBits(PrivilegeRegistry.REP_WRITE));
                            }
                        }
                    }
                }
            } // else outside of user/group tree -> read only.
            return new Result(allows, denies, privs, PrivilegeBits.EMPTY);
        }

        @Override
        protected Result buildRepositoryResult() throws RepositoryException {
            log.warn("TODO: JCR-2774 - Repository level permissions.");
            return new Result(Permission.NONE, Permission.NONE, PrivilegeBits.EMPTY, PrivilegeBits.EMPTY);
        }

        @Override
        protected PrivilegeManagerImpl getPrivilegeManagerImpl() throws RepositoryException {
            return (PrivilegeManagerImpl) ((JackrabbitWorkspace) session.getWorkspace()).getPrivilegeManager();
        }

        //--------------------------------------------< CompiledPermissions >---
        /**
         * @see CompiledPermissions#close()
         */
        @Override
        public void close() {
            try {
                observationMgr.removeEventListener(this);
            } catch (RepositoryException e) {
                log.error("Internal error: {}", e.getMessage());
            }
            super.close();
        }

        /**
         * @see CompiledPermissions#grants(Path, int)
         */
        @Override
        public boolean grants(Path absPath, int permissions) throws RepositoryException {
            if (permissions == Permission.READ) {
                return canReadAll();
            }
            // otherwise: retrieve from cache (or build)
            return super.grants(absPath, permissions);
        }

        /**
         * @see CompiledPermissions#canReadAll()
         */
        @Override
        public boolean canReadAll() throws RepositoryException {
            // for consistency with 'grants(Path, int) this method only returns
            // true if there exists a node for 'userNodePath'
            return session.nodeExists(userNodePath);
        }

        /**
         * @see CompiledPermissions#canRead(Path, ItemId)
         */
        public boolean canRead(Path path, ItemId itemId) throws RepositoryException {
            return canReadAll();
        }

        //--------------------------------------------------< EventListener >---
        /**
         * Event listener is only interested in changes of group-membership
         * that effect the permission-evaluation.
         *
         * @see javax.jcr.observation.EventListener#onEvent(EventIterator)
         */
        public void onEvent(EventIterator events) {
            while (events.hasNext()) {
                Event ev = events.nextEvent();
                try {
                    String evPath = ev.getPath();
                    String repMembers = session.getJCRName(UserConstants.P_MEMBERS);
                    if (repMembers.equals(Text.getName(evPath))) {
                        // invalidate the cached results
                        clearCache();
                        // only need to clear the cache once. stop processing
                        break;
                    } else if (!membersInProperty) {
                        /* the affected property is not rep:Members and members are
                           stored in a tree structure (user manager configuration.
                           test if the parent node is of type rep:Members in order
                           to determine if any membership modification occurred.*/
                        Node parent = session.getNodeByIdentifier(ev.getIdentifier());
                        if (UserConstants.NT_REP_MEMBERS.equals(((NodeTypeImpl) parent.getPrimaryNodeType()).getQName())) {
                            clearCache();
                        }

                    } // else: not interested.
                } catch (RepositoryException e) {
                    // should never get here
                    log.warn("Internal error: {}", e.getMessage());
                    clearCache();
                }
            }
        }
    }
}
