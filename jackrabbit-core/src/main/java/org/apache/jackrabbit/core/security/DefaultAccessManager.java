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
package org.apache.jackrabbit.core.security;

import org.apache.jackrabbit.api.security.JackrabbitAccessControlPolicy;
import org.apache.jackrabbit.api.security.authorization.PrivilegeManager;
import org.apache.jackrabbit.commons.iterator.AccessControlPolicyIteratorAdapter;
import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.security.authorization.AccessControlEditor;
import org.apache.jackrabbit.core.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.CompiledPermissions;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.security.authorization.WorkspaceAccessManager;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;
import javax.security.auth.Subject;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * The <code>DefaultAccessManager</code> controls access by evaluating access
 * control policies for the <code>Subject</code> attached to the
 * <code>Session</code> this manager has been built for.<p>
 * Please note the following exceptional situations:<br>
 * This manager allows all privileges for a particular item if
 * <ul>
 * <li>the Session's represents a system session or a session associated with
 * the repository's administrator</li>
 * </ul>
 * <p>
 * It allows to access all available workspaces if
 * <ul>
 * <li>no <code>WorkspaceAccessManager</code> is defined.</li>
 * </ul>
 * <p>
 * How access control policies are matched to a particular item is defined by
 * the <code>AccessControlProvider</code> set to this AccessManager.
 *
 * @see AccessManager
 * @see javax.jcr.security.AccessControlManager
 */
public class DefaultAccessManager extends AbstractAccessControlManager implements AccessManager {

    private static final Logger log = LoggerFactory.getLogger(DefaultAccessManager.class);

    private boolean initialized;

    private NamePathResolver resolver;

    private Set<Principal> principals;

    private AccessControlProvider acProvider;

    private AccessControlEditor editor;

    /**
     * the workspace access
     */
    private WorkspaceAccess wspAccess;

    /**
     * the hierarchy manager used to resolve path from itemId
     */
    private HierarchyManager hierMgr;

    /**
     * The privilege manager
     */
    private PrivilegeManager privilegeManager;

    /**
     * The permissions that apply for the principals, that are present with
     * the session subject this manager has been created for.
     * TODO: if the users group-membership gets modified the compiledPermissions
     * TODO  should ev. be recalculated. currently those modifications are only
     * TODO  reflected upon re-login to the repository.
     */
    private CompiledPermissions compiledPermissions;

    //------------------------------------------------------< AccessManager >---
    /**
     * @see AccessManager#init(AMContext)
     */
    public void init(AMContext amContext) throws AccessDeniedException, Exception {
        init(amContext, null, null);
    }

    /**
     * @see AccessManager#init(AMContext, AccessControlProvider, WorkspaceAccessManager)
     */
    public void init(AMContext amContext, AccessControlProvider acProvider,
                     WorkspaceAccessManager wspAccessManager) throws AccessDeniedException, Exception {
        if (initialized) {
            throw new IllegalStateException("Already initialized.");
        }

        this.acProvider = acProvider;

        resolver = amContext.getNamePathResolver();
        hierMgr = amContext.getHierarchyManager();

        Subject subject = amContext.getSubject();
        if (subject == null) {
            principals = Collections.emptySet();
        } else {
            principals = subject.getPrincipals();
        }

        wspAccess = new WorkspaceAccess(wspAccessManager, isSystemOrAdmin(amContext.getSession()));
        privilegeManager = amContext.getPrivilegeManager();

        if (acProvider != null) {
            editor = acProvider.getEditor(amContext.getSession());
            compiledPermissions = acProvider.compilePermissions(principals);
        } else {
            log.warn("No AccessControlProvider defined -> no access is granted.");
            editor = null;
            compiledPermissions = CompiledPermissions.NO_PERMISSION;
        }

        initialized = true;

        if (!canAccess(amContext.getWorkspaceName())) {
            throw new AccessDeniedException("Not allowed to access Workspace " + amContext.getWorkspaceName());
        }
    }

    /**
     * @see AccessManager#close()
     */
    public void close() throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Manager is not initialized.");
        }
        initialized = false;
        compiledPermissions.close();

        hierMgr = null;
        acProvider = null;
        editor = null;
        wspAccess = null;
    }

    /**
     * @see AccessManager#checkPermission(ItemId, int)
     */
    public void checkPermission(ItemId id, int permissions) throws AccessDeniedException, ItemNotFoundException, RepositoryException {
        if (!isGranted(id, permissions)) {
            throw new AccessDeniedException("Access denied.");
        }
    }

    /**
     * @see AccessManager#checkPermission(Path, int)
     */
    public void checkPermission(Path absPath, int permissions) throws AccessDeniedException, RepositoryException {
        if (!isGranted(absPath, permissions)) {
            throw new AccessDeniedException("Access denied.");
        }
    }

    /**
     * @see AccessManager#checkRepositoryPermission(int)
     */
    public void checkRepositoryPermission(int permissions) throws AccessDeniedException, RepositoryException {
        checkInitialized();
        if (!compiledPermissions.grants(null, permissions)) {
            throw new AccessDeniedException("Access denied.");
        }
    }

    /**
     * @see AccessManager#isGranted(ItemId, int)
     */
    public boolean isGranted(ItemId id, int actions)
            throws ItemNotFoundException, RepositoryException {
        checkInitialized();
        if (actions == READ && compiledPermissions.canReadAll()) {
            return true;
        } else {
            int perm = 0;
            if ((actions & READ) == READ) {
                perm |= Permission.READ;
            }
            if ((actions & WRITE) == WRITE) {
                if (id.denotesNode()) {
                    // TODO: check again if correct
                    perm |= Permission.SET_PROPERTY;
                    perm |= Permission.ADD_NODE;
                } else {
                    perm |= Permission.SET_PROPERTY;
                }
            }
            if ((actions & REMOVE) == REMOVE) {
                perm |= (id.denotesNode()) ? Permission.REMOVE_NODE : Permission.REMOVE_PROPERTY;
            }
            
            Path path = hierMgr.getPath(id);
            return isGranted(path, perm);
        }
    }

    /**
     * @see AccessManager#isGranted(Path, int)
     */
    public boolean isGranted(Path absPath, int permissions) throws RepositoryException {
        checkInitialized();
        if (!absPath.isAbsolute()) {
            throw new RepositoryException("Absolute path expected");
        }
        return compiledPermissions.grants(absPath, permissions);
    }

    /**
     * @see AccessManager#isGranted(Path, Name, int)
     */
    public boolean isGranted(Path parentPath, Name childName, int permissions) throws RepositoryException {
        Path p = PathFactoryImpl.getInstance().create(parentPath, childName, true);
        return isGranted(p, permissions);
    }

    /**
     * @see AccessManager#canRead(org.apache.jackrabbit.spi.Path,org.apache.jackrabbit.core.id.ItemId)
     */
    public boolean canRead(Path itemPath, ItemId itemId) throws RepositoryException {
        checkInitialized();
        if (compiledPermissions.canReadAll()) {
            return true;
        } else {
            return compiledPermissions.canRead(itemPath, itemId);
        }
    }

    /**
     * @see AccessManager#canAccess(String)
     */
    public boolean canAccess(String workspaceName) throws RepositoryException {
        checkInitialized();
        return wspAccess.canAccess(workspaceName);
    }

    //-----------------------------------------------< AccessControlManager >---
    /**
     * @see javax.jcr.security.AccessControlManager#hasPrivileges(String, Privilege[])
     */
    public boolean hasPrivileges(String absPath, Privilege[] privileges) throws PathNotFoundException, RepositoryException {
        checkInitialized();
        checkValidNodePath(absPath);
        if (privileges == null || privileges.length == 0) {
            // null or empty privilege array -> return true
            log.debug("No privileges passed -> allowed.");
            return true;
        } else {
            Path p = getPath(absPath);
            return compiledPermissions.hasPrivileges(p, privileges);
        }
    }

    /**
     * @see javax.jcr.security.AccessControlManager#getPrivileges(String)
     */
    public Privilege[] getPrivileges(String absPath) throws PathNotFoundException, RepositoryException {
        checkInitialized();
        checkValidNodePath(absPath);
        Set<Privilege> privs = compiledPermissions.getPrivilegeSet(getPath(absPath));
        return privs.toArray(new Privilege[privs.size()]);
    }

    /**
     * @see javax.jcr.security.AccessControlManager#getPolicies(String)
     */
    @Override
    public AccessControlPolicy[] getPolicies(String absPath) throws PathNotFoundException, AccessDeniedException, RepositoryException {
        checkInitialized();
        checkPermission(absPath, Permission.READ_AC);

        AccessControlPolicy[] policies;
        if (editor != null) {
            policies = editor.getPolicies(absPath);
        } else {
            policies = new AccessControlPolicy[0];
        }
        return policies;
    }

    /**
     * @see javax.jcr.security.AccessControlManager#getEffectivePolicies(String)
     */
    public AccessControlPolicy[] getEffectivePolicies(String absPath) throws PathNotFoundException, AccessDeniedException, RepositoryException {
        checkInitialized();
        checkPermission(absPath, Permission.READ_AC);

        return acProvider.getEffectivePolicies(getPath(absPath), compiledPermissions);
    }

    /**
     * @see javax.jcr.security.AccessControlManager#getApplicablePolicies(String)
     */
    @Override
    public AccessControlPolicyIterator getApplicablePolicies(String absPath) throws PathNotFoundException, AccessDeniedException, RepositoryException {
        checkInitialized();
        checkPermission(absPath, Permission.READ_AC);

        if (editor != null) {
            try {
                AccessControlPolicy[] applicable = editor.editAccessControlPolicies(absPath);
                return new AccessControlPolicyIteratorAdapter(Arrays.asList(applicable));
            } catch (AccessControlException e) {
                log.debug("No applicable policy at " + absPath);
            }
        }
        // no applicable policies -> return empty iterator.
        return AccessControlPolicyIteratorAdapter.EMPTY;
    }

    /**
     * @see javax.jcr.security.AccessControlManager#setPolicy(String, AccessControlPolicy)
     */
    @Override
    public void setPolicy(String absPath, AccessControlPolicy policy) throws PathNotFoundException, AccessControlException, AccessDeniedException, RepositoryException {
        checkInitialized();
        checkPermission(absPath, Permission.MODIFY_AC);
        if (editor == null) {
            throw new UnsupportedRepositoryOperationException("Modification of AccessControlPolicies is not supported. ");
        }
        editor.setPolicy(absPath, policy);
    }

    /**
     * @see javax.jcr.security.AccessControlManager#removePolicy(String, AccessControlPolicy)
     */
    @Override
    public void removePolicy(String absPath, AccessControlPolicy policy) throws PathNotFoundException, AccessControlException, AccessDeniedException, RepositoryException {
        checkInitialized();
        checkPermission(absPath, Permission.MODIFY_AC);
        if (editor == null) {
            throw new UnsupportedRepositoryOperationException("Removal of AccessControlPolicies is not supported.");
        }
        editor.removePolicy(absPath, policy);
    }

    //-------------------------------------< JackrabbitAccessControlManager >---
    /**
     * @see org.apache.jackrabbit.api.security.JackrabbitAccessControlManager#getApplicablePolicies(Principal)
     */
    @Override
    public JackrabbitAccessControlPolicy[] getApplicablePolicies(Principal principal) throws AccessDeniedException, AccessControlException, UnsupportedRepositoryOperationException, RepositoryException {
        checkInitialized();
        if (editor == null) {
            throw new UnsupportedRepositoryOperationException("Editing of access control policies is not supported.");
        }
        return editor.editAccessControlPolicies(principal);
    }

    /**
     * @see org.apache.jackrabbit.api.security.JackrabbitAccessControlManager#getPolicies(Principal)
     */
    @Override
    public JackrabbitAccessControlPolicy[] getPolicies(Principal principal) throws AccessDeniedException, AccessControlException, UnsupportedRepositoryOperationException, RepositoryException {
        checkInitialized();
        if (editor == null) {
            throw new UnsupportedRepositoryOperationException("Editing of access control policies is not supported.");
        }
        return editor.getPolicies(principal);
    }

    /**
     * @see org.apache.jackrabbit.api.security.JackrabbitAccessControlManager#getEffectivePolicies(Set)
     */
    public AccessControlPolicy[] getEffectivePolicies(Set<Principal> principals) throws AccessDeniedException, AccessControlException, UnsupportedRepositoryOperationException, RepositoryException {
        checkInitialized();
        return acProvider.getEffectivePolicies(principals, compiledPermissions);
    }

    /**
     * @see org.apache.jackrabbit.api.security.JackrabbitAccessControlManager#hasPrivileges(String, Set, Privilege[])
     */
    public boolean hasPrivileges(String absPath, Set<Principal> principals, Privilege[] privileges) throws PathNotFoundException, RepositoryException {
        checkInitialized();
        checkValidNodePath(absPath);
        checkPermission(absPath, Permission.READ_AC);

        if (privileges == null || privileges.length == 0) {
            // null or empty privilege array -> return true
            log.debug("No privileges passed -> allowed.");
            return true;
        } else {
            Path p = getPath(absPath);
            CompiledPermissions perms = acProvider.compilePermissions(principals);
            try {
                return perms.hasPrivileges(p, privileges);
            } finally {
                perms.close();
            }
        }
    }

    /**
     * @see org.apache.jackrabbit.api.security.JackrabbitAccessControlManager#getPrivileges(String, Set)
     */
    public Privilege[] getPrivileges(String absPath, Set<Principal> principals) throws PathNotFoundException, RepositoryException {
        checkInitialized();
        checkValidNodePath(absPath);
        checkPermission(absPath, Permission.READ_AC);

        CompiledPermissions perms = acProvider.compilePermissions(principals);
        try {
            Set<Privilege> privs = perms.getPrivilegeSet(getPath(absPath));
            return privs.toArray(new Privilege[privs.size()]);
        } finally {
            perms.close();
        }
    }

    //---------------------------------------< AbstractAccessControlManager >---
    /**
     * @see AbstractAccessControlManager#checkInitialized()
     */
    @Override
    protected void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }
    }

    /**
     * @see AbstractAccessControlManager#checkValidNodePath(String)
     */
    @Override
    protected void checkValidNodePath(String absPath) throws PathNotFoundException, RepositoryException {
        Path p = getPath(absPath);
        if (p != null) {
            if (!p.isAbsolute()) {
                throw new RepositoryException("Absolute path expected.");
            }
            if (hierMgr.resolveNodePath(p) == null) {
                throw new PathNotFoundException("No such node " + absPath);
            }
        }
    }

    /**
     * @see AbstractAccessControlManager#checkPermission(String,int)
     */
    @Override
    protected void checkPermission(String absPath, int permission) throws AccessDeniedException, RepositoryException {
        checkValidNodePath(absPath);
        Path p = getPath(absPath);
        if (!compiledPermissions.grants(p, permission)) {
            throw new AccessDeniedException("Access denied at " + absPath);
        }
    }

    /**
     * @see AbstractAccessControlManager#getPrivilegeManager()
     */
    @Override
    protected PrivilegeManager getPrivilegeManager() throws RepositoryException {
        checkInitialized();
        return privilegeManager;
    }

    //------------------------------------------------------------< private >---
    private Path getPath(String absPath) throws RepositoryException {
        return (absPath == null) ? null : resolver.getQPath(absPath);
    }

    /**
     * @param s the session
     * @return if created with system-privileges
     */
    private static boolean isSystemOrAdmin(Session s) {
        if (s == null || !(s instanceof SessionImpl)) {
            return false;
        } else {
            SessionImpl sImpl = (SessionImpl) s;
            return sImpl.isSystem() || sImpl.isAdmin();
        }
    }

    //--------------------------------------------------------------------------
    /**
     * Simple wrapper around the repository's <code>WorkspaceAccessManager</code>
     * that remembers for which workspaces the access has already been
     * evaluated.
     */
    private class WorkspaceAccess {

        private final WorkspaceAccessManager wspAccessManager;

        private final boolean alwaysAllowed;
        // TODO: entries must be cleared if access permission to wsp changes.
        private final List <String>allowed;
        private final List<String> denied;

        private WorkspaceAccess(WorkspaceAccessManager wspAccessManager,
                                boolean alwaysAllowed) {
            this.wspAccessManager = wspAccessManager;
            this.alwaysAllowed = alwaysAllowed;
            if (!alwaysAllowed) {
                allowed = new ArrayList<String>(5);
                denied = new ArrayList<String>(5);
            } else {
                allowed = denied = null;
            }
        }

        private boolean canAccess(String workspaceName) throws RepositoryException {
            if (alwaysAllowed || wspAccessManager == null || allowed.contains(workspaceName)) {
                return true;
            } else if (denied.contains(workspaceName)) {
                return false;
            }

            // not yet tested -> ask the workspace-accessmanager.
            boolean canAccess = wspAccessManager.grants(principals, workspaceName);
            if (canAccess) {
                allowed.add(workspaceName);
            } else {
                denied.add(workspaceName);
            }
            return canAccess;
        }
    }
}
