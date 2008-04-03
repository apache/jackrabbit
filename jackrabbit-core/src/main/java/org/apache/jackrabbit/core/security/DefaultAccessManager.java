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

import org.apache.jackrabbit.commons.iterator.AccessControlPolicyIteratorAdapter;
import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.security.authorization.PolicyTemplate;
import org.apache.jackrabbit.core.security.authorization.AccessControlEditor;
import org.apache.jackrabbit.core.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.core.security.authorization.WorkspaceAccessManager;
import org.apache.jackrabbit.core.security.authorization.CompiledPermissions;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.security.jsr283.security.AccessControlEntry;
import org.apache.jackrabbit.core.security.jsr283.security.AccessControlException;
import org.apache.jackrabbit.core.security.jsr283.security.AccessControlManager;
import org.apache.jackrabbit.core.security.jsr283.security.AccessControlPolicy;
import org.apache.jackrabbit.core.security.jsr283.security.AccessControlPolicyIterator;
import org.apache.jackrabbit.core.security.jsr283.security.Privilege;
import org.apache.jackrabbit.core.security.principal.AdminPrincipal;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.security.auth.Subject;
import java.security.Principal;
import java.util.Collections;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

/**
 * The <code>DefaultAccessManager</code> controls access by evaluating access
 * control policies for the <code>Subject</code> attached to the
 * <code>Session</code> this manager has been built for.<p>
 * Please note the following exceptional situations:<br>
 * This manager allows all privileges for a particular item if
 * <ul>
 * <li>the Session's Subject contains a {@link SystemPrincipal} <i>or</i>
 * an {@link AdminPrincipal}</li>
 * </ul>
 * <p/>
 * It allows to access all available workspaces if
 * <ul>
 * <li>no <code>WorkspaceAccessManager</code> is defined.</li>
 * </ul>
 * <p/>
 * How access control policies are matched to a particular item is defined by
 * the <code>AccessControlProvider</code> set to this AccessManager.
 *
 * @see AccessManager
 * @see AccessControlManager
 */
public class DefaultAccessManager extends AbstractAccessControlManager implements AccessManager {

    private static final Logger log = LoggerFactory.getLogger(DefaultAccessManager.class);

    private boolean initialized;

    private NamePathResolver resolver;

    private Set principals;

    private AccessControlProvider acProvider;

    private AccessControlEditor editor;

    /**
     * the workspace access
     */
    private WorkspaceAccess wspAccess;

    /**
     * access items for resolution of last persisted item in hierarchy
     */
    private HierarchyManager hierMgr;

    /**
     * The permissions that apply for the principals, that are present with
     * the session subject this manager has been created for.
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
        principals = (subject == null) ? Collections.EMPTY_SET : subject.getPrincipals();

        wspAccess = new WorkspaceAccess(wspAccessManager, isSystemOrAdmin(subject));

        if (acProvider != null) {
            editor = acProvider.getEditor(amContext.getSession());
            compiledPermissions = acProvider.compilePermissions(principals);
        } else {
            // TODO: review expected default-permissions here.
            log.warn("No AccessControlProvider defined -> no access is granted.");
            editor = null;
            compiledPermissions = new CompiledPermissions() {
                public void close() {
                    //nop
                }
                public boolean grants(Path absPath, int permissions) throws RepositoryException {
                    // deny everything
                    return false;
                }
                public int getPrivileges(Path absPath) throws RepositoryException {
                    return 0;
                }
            };
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
            throw new AccessDeniedException("Not sufficient privileges for permissions : " + permissions + " on " + hierMgr.getPath(id));
        }
    }

    /**
     * @see AccessManager#isGranted(ItemId, int)
     */
    public boolean isGranted(ItemId id, int actions)
            throws ItemNotFoundException, RepositoryException {
        checkInitialized();
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

    public boolean isGranted(Path absPath, int permissions) throws RepositoryException {
        checkInitialized();
        if (!absPath.isAbsolute()) {
            throw new RepositoryException("Absolute path expected");
        }
        return compiledPermissions.grants(absPath, permissions);
    }

    public boolean isGranted(Path parentPath, Name childName, int permissions) throws ItemNotFoundException, RepositoryException {
        Path p = PathFactoryImpl.getInstance().create(parentPath, childName, true);
        return isGranted(p, permissions);
    }

    /**
     * @see AccessManager#canAccess(String)
     */
    public boolean canAccess(String workspaceName) throws NoSuchWorkspaceException, RepositoryException {
        checkInitialized();
        return wspAccess.canAccess(workspaceName);
    }

    //-----------------------------------------------< AccessControlManager >---
    /**
     * @see AccessControlManager#hasPrivileges(String, Privilege[])
     */
    public boolean hasPrivileges(String absPath, Privilege[] privileges) throws PathNotFoundException, RepositoryException {
        checkInitialized();
        checkValidNodePath(absPath);
        if (privileges == null || privileges.length == 0) {
            // null or empty privilege array -> return true
            log.debug("No privileges defined for hasPrivilege test.");
            return true;
        } else {
            int privs = PrivilegeRegistry.getBits(privileges);
            return internalHasPrivileges(absPath, privs);
        }
    }

    /**
     * @see AccessControlManager#getPrivileges(String)
     */
    public Privilege[] getPrivileges(String absPath) throws PathNotFoundException, RepositoryException {
        checkInitialized();
        checkValidNodePath(absPath);
        int privs = compiledPermissions.getPrivileges(resolver.getQPath(absPath));
        return (privs == 0) ? new Privilege[0] : PrivilegeRegistry.getPrivileges(privs);
    }

    /**
     * @see AccessControlManager#getPolicy(String)
     */
    public AccessControlPolicy getPolicy(String absPath) throws PathNotFoundException, AccessDeniedException, RepositoryException {
        checkInitialized();
        checkPrivileges(absPath, PrivilegeRegistry.READ_AC);

        AccessControlPolicy policy = null;
        if (editor != null) {
            policy = editor.getPolicyTemplate(absPath);
        }
        return policy;
    }

    /**
     * @see AccessControlManager#getEffectivePolicy(String)
     */
    public AccessControlPolicy getEffectivePolicy(String absPath) throws PathNotFoundException, AccessDeniedException, RepositoryException {
        checkInitialized();
        checkPrivileges(absPath, PrivilegeRegistry.READ_AC);

        // TODO: acProvider may not retrieve the correct policy in case of transient modifications
        return acProvider.getPolicy(getNodeId(absPath));
    }

    /**
     * @see AccessControlManager#getApplicablePolicies(String)
     */
    public AccessControlPolicyIterator getApplicablePolicies(String absPath) throws PathNotFoundException, AccessDeniedException, RepositoryException {
        checkInitialized();
        checkPrivileges(absPath, PrivilegeRegistry.READ_AC);

        if (editor != null) {
            PolicyTemplate applicable = editor.editPolicyTemplate(absPath);
            if (applicable != null) {
                return new AccessControlPolicyIteratorAdapter(Collections.singletonList(applicable));
            }
        }
        // no applicable policies -> return empty iterator.
        return AccessControlPolicyIteratorAdapter.EMPTY;
    }

    /**
     * @see AccessControlManager#setPolicy(String, AccessControlPolicy)
     */
    public void setPolicy(String absPath, AccessControlPolicy policy) throws PathNotFoundException, AccessControlException, AccessDeniedException, RepositoryException {
        checkInitialized();
        if (policy instanceof PolicyTemplate) {
            checkPrivileges(absPath, PrivilegeRegistry.MODIFY_AC);
            if (editor == null) {
                throw new UnsupportedRepositoryOperationException("Modification of AccessControlPolicies is not supported. ");
            }
            editor.setPolicyTemplate(absPath, (PolicyTemplate) policy);
        } else {
            throw new AccessControlException("Access control policy '" + policy + "' not applicable");
        }
    }

    /**
     * @see AccessControlManager#removePolicy(String)
     */
    public AccessControlPolicy removePolicy(String absPath) throws PathNotFoundException, AccessControlException, AccessDeniedException, RepositoryException {
        checkInitialized();
        checkPrivileges(absPath, PrivilegeRegistry.MODIFY_AC);
        if (editor == null) {
            throw new UnsupportedRepositoryOperationException("Removal of AccessControlPolicies is not supported.");
        }
        return editor.removePolicyTemplate(absPath);
    }

    /**
     * @see AccessControlManager#getAccessControlEntries(String)
     */
    public AccessControlEntry[] getAccessControlEntries(String absPath) throws PathNotFoundException, AccessDeniedException, RepositoryException {
        checkInitialized();
        checkPrivileges(absPath, PrivilegeRegistry.READ_AC);

        AccessControlEntry[] entries = new AccessControlEntry[0];
        if (editor != null) {
            entries = editor.getAccessControlEntries(absPath);
        }
        return entries;
    }

    /**
     * @see AccessControlManager#getEffectiveAccessControlEntries(String)
     */
    public AccessControlEntry[] getEffectiveAccessControlEntries(String absPath) throws PathNotFoundException, AccessDeniedException, RepositoryException {
        checkInitialized();
        checkPrivileges(absPath, PrivilegeRegistry.READ_AC);

        return acProvider.getAccessControlEntries(getNodeId(absPath));
    }

    /**
     * @see AccessControlManager#addAccessControlEntry(String, Principal, Privilege[])
     */
    public AccessControlEntry addAccessControlEntry(String absPath, Principal principal, Privilege[] privileges) throws PathNotFoundException, AccessControlException, AccessDeniedException, RepositoryException {
        checkInitialized();
        checkPrivileges(absPath, PrivilegeRegistry.MODIFY_AC);

        if (editor == null) {
            throw new UnsupportedRepositoryOperationException("Adding access control entries is not supported.");
        } else {
            return editor.addAccessControlEntry(absPath, principal, privileges);
        }
    }

    /**
     * @see AccessControlManager#removeAccessControlEntry(String, AccessControlEntry)
     */
    public void removeAccessControlEntry(String absPath, AccessControlEntry ace) throws PathNotFoundException, AccessControlException, AccessDeniedException, RepositoryException {
        checkInitialized();
        checkPrivileges(absPath, PrivilegeRegistry.MODIFY_AC);
        if (editor == null) {
            throw new UnsupportedRepositoryOperationException("Removal of access control entries is not supported.");
        }
        if (!editor.removeAccessControlEntry(absPath, ace)) {
            throw new AccessControlException("AccessControlEntry " + ace + " has not been assigned though this API.");
        }
    }

    //-------------------------------------< JackrabbitAccessControlManager >---
    /**
     * @see JackrabbitAccessControlManager#editPolicy(String)
     */
    public PolicyTemplate editPolicy(String absPath) throws AccessDeniedException, AccessControlException, RepositoryException {
        checkInitialized();
        checkPrivileges(absPath, PrivilegeRegistry.MODIFY_AC);
        if (editor == null) {
            throw new UnsupportedRepositoryOperationException("Editing of access control policies is not supported.");
        }

        return editor.editPolicyTemplate(absPath);
    }

    public PolicyTemplate editPolicy(Principal principal) throws AccessDeniedException, AccessControlException, UnsupportedRepositoryOperationException, RepositoryException {
        checkInitialized();
        if (editor == null) {
            throw new UnsupportedRepositoryOperationException("Editing of access control policies is not supported.");
        }
        return editor.editPolicyTemplate(principal);
    }

    //---------------------------------------< AbstractAccessControlManager >---
    /**
     * @see AbstractAccessControlManager#checkInitialized()
     */
    protected void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }
    }

    /**
     * @see AbstractAccessControlManager#checkValidNodePath(String)
     */
    protected void checkValidNodePath(String absPath) throws PathNotFoundException, RepositoryException {
        Path p = resolver.getQPath(absPath);
        if (!p.isAbsolute()) {
            throw new RepositoryException("Absolute path expected.");
        }
        if (hierMgr.resolveNodePath(p) == null) {
            throw new PathNotFoundException("No such node " + absPath);
        }
    }

    /**
     * @see AbstractAccessControlManager#checkPrivileges(String, int)
     */
    protected void checkPrivileges(String absPath, int privileges) throws AccessDeniedException, RepositoryException {
        checkValidNodePath(absPath);
        if (!internalHasPrivileges(absPath, privileges)) {
            throw new AccessDeniedException("No privilege " + privileges + " at " + absPath);
        }
    }

    //------------------------------------------------------------< private >---
    /**
     *
     * @param absPath
     * @param privileges
     * @return
     * @throws RepositoryException
     */
    private boolean internalHasPrivileges(String absPath, int privileges) throws RepositoryException {
        Path p = resolver.getQPath(absPath);
        return (compiledPermissions.getPrivileges(p) | ~privileges) == -1;
    }

    private NodeId getNodeId(String absPath) throws RepositoryException {
        NodeId id = hierMgr.resolveNodePath(resolver.getQPath(absPath));
        if (id == null) {
            throw new PathNotFoundException(absPath);
        }
        return id;
    }

    /**
     * @return if created with system-privileges
     */
    private static boolean isSystemOrAdmin(Subject subject) {
        if (subject == null) {
            return false;
        } else {
            return !(subject.getPrincipals(SystemPrincipal.class).isEmpty() &&
                     subject.getPrincipals(AdminPrincipal.class).isEmpty());
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

        private final boolean isAdmin;
        // TODO: entries must be cleared if access permission to wsp change.
        private final List allowed;
        private final List denied;

        private WorkspaceAccess(WorkspaceAccessManager wspAccessManager,
                                boolean isAdmin) {
            this.wspAccessManager = wspAccessManager;
            this.isAdmin = isAdmin;
            if (!isAdmin) {
                allowed = new ArrayList(5);
                denied = new ArrayList(5);
            } else {
                allowed = denied = null;
            }
        }

        private boolean canAccess(String workspaceName) throws RepositoryException {
            if (isAdmin || wspAccessManager == null || allowed.contains(workspaceName)) {
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
