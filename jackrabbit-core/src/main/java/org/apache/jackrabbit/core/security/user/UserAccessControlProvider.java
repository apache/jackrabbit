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

import org.apache.jackrabbit.api.jsr283.security.AccessControlPolicy;
import org.apache.jackrabbit.api.jsr283.security.Privilege;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.ItemImpl;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.observation.SynchronousEventListener;
import org.apache.jackrabbit.core.security.authorization.AbstractAccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.AbstractCompiledPermissions;
import org.apache.jackrabbit.core.security.authorization.AccessControlEditor;
import org.apache.jackrabbit.core.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.CompiledPermissions;
import org.apache.jackrabbit.core.security.authorization.NamedAccessControlPolicyImpl;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.core.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.core.security.principal.PrincipalImpl;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import java.security.Principal;
import java.security.acl.Group;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of the <code>AccessControlProvider</code> interface that
 * is used to protected the 'security workspace' containing the user and
 * group data. It applies special care to make sure that modifying user data
 * (e.g. password), group membership and impersonation is properly controlled.
 * <p/>
 * This provider creates upon initialization the following 2 groups:
 * <ul>
 * <li>User administrator</li>
 * <li>Group administrator</li>
 * </ul>
 *
 * The default access control policy defined by this provider has the following
 * characteristics:
 * <ul>
 * <li>everybody has READ permission to all items,</li>
 *
 * <li>every known user is allowed to modify it's own properties except for
 * her/his group membership,</li>
 *
 * <li>members of the 'User administrator' group are allowed to create, modify
 * and remove those users whose node representation is within the subtree
 * defined by the node representation of the editing user,</li>
 *
 * <li>members of the 'Group administrator' group are allowed to create, modify
 * and remove groups,</li>
 *
 * <li>group membership can only be edited by members of the 'Group administrator'
 * and the 'User administrator' group. The range of users that can be added
 * as member to any Group is limited to those that are editable according to
 * the restrictions described above for the 'User administrator'.</li>
 * </ul>
 */
public class UserAccessControlProvider extends AbstractAccessControlProvider
        implements UserConstants {

    private static Logger log = LoggerFactory.getLogger(UserAccessControlProvider.class);

    private final AccessControlPolicy policy;

    private Path groupsPath;
    private Path usersPath;

    private String userAdminGroup;
    private String groupAdminGroup;

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
    public boolean isAcItem(Path absPath) throws RepositoryException {
        return false;
    }

    /**
     * Always returns false, since this ac provider does not use content stored
     * in items to evaluate AC information.
     *
     * @see org.apache.jackrabbit.core.security.authorization.AccessControlUtils#isAcItem(ItemImpl)
     */
    public boolean isAcItem(ItemImpl item) throws RepositoryException {
        return false;
    }

    //----------------------------------------------< AccessControlProvider >---
    /**
     * @see AccessControlProvider#init(Session, Map)
     */
    public void init(Session systemSession, Map configuration) throws RepositoryException {
        super.init(systemSession, configuration);
        if (systemSession instanceof SessionImpl) {
            SessionImpl sImpl = (SessionImpl) systemSession;
            userAdminGroup = (configuration.containsKey(USER_ADMIN_GROUP_NAME)) ? configuration.get(USER_ADMIN_GROUP_NAME).toString() : USER_ADMIN_GROUP_NAME;
            groupAdminGroup = (configuration.containsKey(GROUP_ADMIN_GROUP_NAME)) ? configuration.get(GROUP_ADMIN_GROUP_NAME).toString() : GROUP_ADMIN_GROUP_NAME;

            // make sure the groups exist (and ev. create them).
            UserManager uMgr = sImpl.getUserManager();
            if (!initGroup(uMgr, userAdminGroup)) {
                log.warn("Unable to initialize User admininistrator group -> no user admins.");
                userAdminGroup = null;
            }
            if (!initGroup(uMgr, groupAdminGroup)) {
                log.warn("Unable to initialize Group admininistrator group -> no group admins.");
                groupAdminGroup = null;
            }

            usersPath = sImpl.getQPath(USERS_PATH);
            groupsPath = sImpl.getQPath(GROUPS_PATH);
        } else {
            throw new RepositoryException("SessionImpl (system session) expected.");
        }
    }

    /**
     * @see AccessControlProvider#getEffectivePolicies(Path)
     */
    public AccessControlPolicy[] getEffectivePolicies(Path absPath) throws ItemNotFoundException, RepositoryException {
        checkInitialized();
        return new AccessControlPolicy[] {policy};
    }

    /**
     * Always returns <code>null</code>.
     *
     * @see AccessControlProvider#getEditor(Session)
     */
    public AccessControlEditor getEditor(Session session) {
        checkInitialized();
        // not editable at all: policy is always the default and cannot be
        // changed using the JCR API.
        return null;
    }

    /**
     * @see AccessControlProvider#compilePermissions(Set)
     */
    public CompiledPermissions compilePermissions(Set principals) throws RepositoryException {
        checkInitialized();
        if (isAdminOrSystem(principals)) {
            return getAdminPermissions();
        } else {
            // determined the 'user' present in the given set of principals.
            ItemBasedPrincipal userPrincipal = getUserPrincipal(principals);
            NodeImpl userNode = getUserNode(userPrincipal);
            if (userNode == null) {
                // no 'user' within set of principals -> READ-only
                return getReadOnlyPermissions();
            } else {
                return new CompiledPermissionsImpl(principals, userNode.getPath());
            }
        }
    }

    /**
     * @see AccessControlProvider#canAccessRoot(Set)
     */
    public boolean canAccessRoot(Set principals) throws RepositoryException {
        checkInitialized();
        return true;
    }

    //------------------------------------------------------------< private >---

    private ItemBasedPrincipal getUserPrincipal(Set principals) {
        try {
            UserManager uMgr = session.getUserManager();
            for (Iterator it = principals.iterator(); it.hasNext();) {
                Principal p = (Principal) it.next();
                if (!(p instanceof Group) && p instanceof ItemBasedPrincipal
                        && uMgr.getAuthorizable(p) != null) {
                    return (ItemBasedPrincipal) p;
                }
            }
        } catch (RepositoryException e) {
            // should never get here
            log.error("Internal error while retrieving user principal", e.getMessage());
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
                log.warn("Error while retrieving user node.", e.getMessage());
            }
        }
        return userNode;
    }

    private Node getExistingNode(Path path) throws RepositoryException {
        String absPath = resolver.getJCRPath(path.getNormalizedPath());
        if (session.nodeExists(absPath)) {
            return session.getNode(absPath);
        } else if (session.propertyExists(absPath)) {
            return session.getProperty(absPath).getParent();
        } else {
            String pPath = Text.getRelativeParent(absPath, 1);
            if (session.nodeExists(pPath)) {
                return session.getNode(pPath);
            } else {
                throw new ItemNotFoundException("Unable to determine permissions: No item and no existing parent for target path " + absPath);
            }
        }
    }

    private int getPrivilegeBits(String privName) throws RepositoryException {
        Privilege[] privs = new Privilege[] {session.getAccessControlManager().privilegeFromName(privName)};
        return PrivilegeRegistry.getBits(privs);
    }

    private static boolean containsGroup(Set principals, String groupName) {
        for (Iterator it = principals.iterator(); it.hasNext() && groupName != null;) {
            Principal p = (Principal) it.next();
            if (p.getName().equals(groupName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean initGroup(UserManager uMgr, String principalName) {
        boolean success;
        Principal prnc = new PrincipalImpl(principalName);
        try {
            Authorizable auth = uMgr.getAuthorizable(prnc);
            if (auth == null) {
                success = (uMgr.createGroup(prnc) != null);
            } else {
                success = auth.isGroup();
                if (!success) {
                    log.warn("Cannot create group '" + principalName + "'; User with that principal already exists.");
                }
            }
        } catch (RepositoryException e) {
            // should never get here
            log.error("Error while initializing user/group administrators", e.getMessage());
            success = false;
        }
        return success;
    }

    //--------------------------------------------------------< inner class >---
    /**
     *
     */
    private class CompiledPermissionsImpl extends AbstractCompiledPermissions
            implements SynchronousEventListener {

        private final String userNodePath;

        private boolean isUserAdmin;
        private boolean isGroupAdmin;

        protected CompiledPermissionsImpl(Set principals, String userNodePath) throws RepositoryException {
            this.userNodePath = userNodePath;
            isUserAdmin = containsGroup(principals, userAdminGroup);
            isGroupAdmin = containsGroup(principals, groupAdminGroup);

            int events = Event.PROPERTY_CHANGED | Event.PROPERTY_ADDED | Event.PROPERTY_REMOVED;
            observationMgr.addEventListener(this, events, USERS_PATH, true, null, null, false);
        }

        //------------------------------------< AbstractCompiledPermissions >---
        /**
         * @see AbstractCompiledPermissions#buildResult(Path)
         */
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
                // calculated -> no permissions/priviles.
                log.debug("No node at " + userNodePath);
                return new Result(Permission.NONE, Permission.NONE, PrivilegeRegistry.NO_PRIVILEGE, PrivilegeRegistry.NO_PRIVILEGE);
            }

            // no explicit denied permissions:
            int denies = Permission.NONE;
            // default allow permission and default privileges
            int allows = Permission.READ;
            int privs;
            // Determine if for path, the set of privileges must be calculated:
            // Generally, privileges can only be determined for existing nodes.
            boolean calcPrivs = session.nodeExists(resolver.getJCRPath(path.getNormalizedPath()));
            if (calcPrivs) {
                privs = getPrivilegeBits(Privilege.JCR_READ);
            } else {
                privs = PrivilegeRegistry.NO_PRIVILEGE;
            }

            Path abs2Path = (4 > path.getLength()) ? null : path.subPath(0, 4);
            if (usersPath.equals(abs2Path)) {
                /*
                 below the user-tree
                 - determine position of target relative to the node of the editing user
                 - determine if the editing user is user/group-admin
                 - special treatment for rep:groups property
                 */
                NodeImpl node = (NodeImpl) getExistingNode(path);
                NodeImpl authN = null;
                // seek next rep:authorizable parent
                if (node.isNodeType(NT_REP_AUTHORIZABLE)) {
                    authN = node;
                } else if (node.isNodeType(NT_REP_AUTHORIZABLE_FOLDER)) {
                    NodeImpl parent = node;
                    while (authN == null && parent.getDepth() > 0) {
                        parent = (NodeImpl) parent.getParent();
                        if (parent.isNodeType(NT_REP_AUTHORIZABLE)) {
                            authN = parent;
                        } else if (!parent.isNodeType(NT_REP_AUTHORIZABLE_FOLDER)) {
                            // outside of user/group-tree
                            break;
                        }
                    }
                } // else: outside of user tree -> authN = null

                if (authN != null && authN.isNodeType(NT_REP_USER)) {
                    int relDepth = session.getHierarchyManager().getRelativeDepth(userNode.getNodeId(), authN.getNodeId());
                    boolean isGroupProp = P_GROUPS.equals(path.getNameElement().getName());
                    // only user-admin is allowed to modify users.
                    // for group membership (rep:groups) group-admin is required
                    // in addition.
                    boolean requiredGroups = isUserAdmin;
                    if (requiredGroups && isGroupProp) {
                        requiredGroups = isGroupAdmin;
                    }
                    switch (relDepth) {
                        case -1:
                            // authN is not below the userNode -> can't write anyway.
                            break;
                        case 0:
                            /*
                            authN is same node as userNode. 3 cases to distinguish
                            1) user is User-Admin -> R, W
                            2) user is NOT U-admin but nodeID is its own node.
                            3) special treatment for rep:group property which can
                               only be modified by group-administrators
                            */
                            Path aPath = session.getQPath(authN.getPath());
                            if (requiredGroups) {
                                // principals contain 'user-admin'
                                // -> user can modify items below the user-node except rep:group.
                                // principals contains 'user-admin' + 'group-admin'
                                // -> user can modify rep:group property as well.
                                if (path.equals(aPath)) {
                                    allows |= (Permission.ADD_NODE | Permission.REMOVE_PROPERTY | Permission.SET_PROPERTY);
                                } else {
                                    allows |= Permission.ALL;
                                }
                                if (calcPrivs) {
                                    // grant WRITE privilege
                                    // note: ac-read/modification is not included
                                    //       remove_node is not included
                                    privs |= getPrivilegeBits(PrivilegeRegistry.REP_WRITE);
                                    if (!path.equals(aPath)) {
                                       privs |= getPrivilegeBits(Privilege.JCR_REMOVE_NODE);
                                    }
                                }
                            } else if (userNode.isSame(node) && (!isGroupProp || isGroupAdmin)) {
                                // user can only read && write his own props
                                // except for the rep:group property.
                                allows |= (Permission.SET_PROPERTY | Permission.REMOVE_PROPERTY);
                                if (calcPrivs) {
                                    privs |= getPrivilegeBits(Privilege.JCR_MODIFY_PROPERTIES);
                                }
                            } // else some other node below but not U-admin -> read-only.
                            break;
                        default:
                            /*
                            authN is somewhere below the userNode, i.e.
                            1) nodeId points to an authorizable below userNode
                            2) nodeId points to an auth-folder below some authorizable below userNode.

                            In either case user-admin group-membership is
                            required in order to get write permission.
                            group-admin group-membership is required in addition
                            if rep:groups is the target item.
                            */
                            if (requiredGroups) {
                                allows = Permission.ALL;
                                if (calcPrivs) {
                                    // grant WRITE privilege
                                    // note: ac-read/modification is not included
                                    privs |= getPrivilegeBits(PrivilegeRegistry.REP_WRITE);
                                }
                            }
                    }
                } // no rep:User parent node found.
            } else if (groupsPath.equals(abs2Path)) {
                /*
                below group-tree:
                - test if the user is group-administrator.
                */
                if (isGroupAdmin) {
                    allows = Permission.ALL;
                    if (calcPrivs) {
                        privs |= getPrivilegeBits(PrivilegeRegistry.REP_WRITE);
                    }
                }
            } // else outside of user/group tree -> read only.
            return new Result(allows, denies, privs, PrivilegeRegistry.NO_PRIVILEGE);
        }

        //--------------------------------------------< CompiledPermissions >---
        /**
         * @see CompiledPermissions#close()
         */
        public void close() {
            try {
                observationMgr.removeEventListener(this);
            } catch (RepositoryException e) {
                log.error("Internal error: ", e.getMessage());
            }
            super.close();
        }

        /**
         * @see CompiledPermissions#grants(Path, int)
         */
        public boolean grants(Path absPath, int permissions) throws RepositoryException {
            if (permissions == Permission.READ) {
                // read is always granted
                return true;
            }
            // otherwise: retrieve from cache (or build)
            return super.grants(absPath, permissions);
        }

        /**
         * @see CompiledPermissions#canReadAll()
         */
        public boolean canReadAll() throws RepositoryException {
            return true;
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
                    String repGroups = session.getJCRName(UserConstants.P_GROUPS);
                    // TODO: add better evaluation.
                    if (repGroups.equals(Text.getName(evPath)) &&
                            userNodePath.equals(Text.getRelativeParent(evPath, 1))) {
                        // recalculate the is...Admin flags
                        switch (ev.getType()) {
                            case Event.PROPERTY_REMOVED:
                                isUserAdmin = false;
                                isGroupAdmin = false;
                                break;
                            case Event.PROPERTY_ADDED:
                            case Event.PROPERTY_CHANGED:
                                if (session.propertyExists(evPath)) {
                                    Value[] vs = session.getProperty(evPath).getValues();
                                    String princName = session.getJCRName(P_PRINCIPAL_NAME);
                                    for (int i = 0; i < vs.length; i++) {
                                        Node groupNode = session.getNodeByUUID(vs[i].getString());
                                        String pName = groupNode.getProperty(princName).getString();
                                        if (userAdminGroup.equals(pName)) {
                                            isUserAdmin = true;
                                        } else if (groupAdminGroup.equals(pName)) {
                                            isGroupAdmin = true;
                                        }
                                    }
                                }
                                break;
                                // default: other events are not relevant.
                        }
                        // invalidate the cached results
                        clearCache();
                        // only need to clear the cache once. stop processing
                        break;
                    }
                } catch (RepositoryException e) {
                    // should never get here
                    log.error("Internal error ", e.getMessage());
                }
            }
        }
    }
}
