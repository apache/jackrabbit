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
package org.apache.jackrabbit.core.security.simple;

import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.api.security.authorization.PrivilegeManager;
import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.security.AMContext;
import org.apache.jackrabbit.core.security.AbstractAccessControlManager;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.AnonymousPrincipal;
import org.apache.jackrabbit.core.security.SystemPrincipal;
import org.apache.jackrabbit.core.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.NamedAccessControlPolicyImpl;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.security.authorization.WorkspaceAccessManager;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;

import javax.jcr.AccessDeniedException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.security.auth.Subject;
import java.security.Principal;
import java.util.Set;

/**
 * <code>SimpleAccessManager</code> ...
 */
public class SimpleAccessManager extends AbstractAccessControlManager implements AccessManager {

    /**
     * The policy returned upon {@link #getEffectivePolicies(String)}
     */
    private static final AccessControlPolicy POLICY = new NamedAccessControlPolicyImpl("Simple AccessControlPolicy");

    /**
     * Subject whose access rights this AccessManager should reflect
     */
    private Subject subject;

    /**
     * hierarchy manager used for ACL-based access control model
     */
    private HierarchyManager hierMgr;

    private NamePathResolver resolver;
    private WorkspaceAccessManager wspAccessMgr;
    private PrivilegeManager privilegeManager;

    private boolean initialized;

    private boolean system;
    private boolean anonymous;

    /**
     * Empty constructor
     */
    public SimpleAccessManager() {
        initialized = false;
        anonymous = false;
        system = false;
    }

    //------------------------------------------------------< AccessManager >---
    /**
     * {@inheritDoc}
     */
    public void init(AMContext context)
            throws AccessDeniedException, Exception {
        init(context, null, null);
    }

    /**
     * {@inheritDoc}
     */
    public void init(AMContext context, AccessControlProvider acProvider, WorkspaceAccessManager wspAccessManager) throws AccessDeniedException, Exception {
        if (initialized) {
            throw new IllegalStateException("already initialized");
        }

        subject = context.getSubject();
        hierMgr = context.getHierarchyManager();
        resolver = context.getNamePathResolver();
        privilegeManager = ((JackrabbitWorkspace) context.getSession().getWorkspace()).getPrivilegeManager();
        wspAccessMgr = wspAccessManager;
        anonymous = !subject.getPrincipals(AnonymousPrincipal.class).isEmpty();
        system = !subject.getPrincipals(SystemPrincipal.class).isEmpty();

        // @todo check permission to access given workspace based on principals
        initialized = true;

        if (!canAccess(context.getWorkspaceName())) {
            throw new AccessDeniedException("Not allowed to access Workspace " + context.getWorkspaceName());
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void close() throws Exception {
        checkInitialized();
        initialized = false;
    }

    /**
     * {@inheritDoc}
     */
    public void checkPermission(ItemId id, int permissions)
            throws AccessDeniedException, RepositoryException {
        if (!isGranted(id, permissions)) {
            throw new AccessDeniedException("Access denied");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkPermission(Path absPath, int permissions) throws AccessDeniedException, RepositoryException {
        if (!isGranted(absPath, permissions)) {
            throw new AccessDeniedException("Access denied");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkRepositoryPermission(int permissions) throws AccessDeniedException, RepositoryException {
        if (!isGranted((ItemId) null, permissions)) {
            throw new AccessDeniedException("Access denied");
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isGranted(ItemId id, int permissions) throws RepositoryException {
        checkInitialized();
        if (system) {
            // system has always all permissions
            return true;
        } else if (anonymous) {
            // anonymous is always denied WRITE & REMOVE permissions
            if ((permissions & WRITE) == WRITE
                    || (permissions & REMOVE) == REMOVE) {
                return false;
            }
        }

        // @todo check permission based on principals
        return true;
    }

    public boolean isGranted(Path absPath, int permissions) throws RepositoryException {
        return internalIsGranted(absPath, permissions);
    }

    public boolean isGranted(Path parentPath, Name childName, int permissions) throws RepositoryException {
        return internalIsGranted(parentPath, permissions);
    }

    public boolean canRead(Path itemPath, ItemId itemId) throws RepositoryException {
        return true;
    }

    private boolean internalIsGranted(Path absPath, int permissions) throws RepositoryException {
        if (!absPath.isAbsolute()) {
            throw new RepositoryException("Absolute path expected");
        }
        checkInitialized();
        if (system) {
            // system has always all permissions
            return true;
        } else if (anonymous) {
            // anonymous is only granted READ permissions
            return permissions == Permission.READ;
        }

        // @todo check permission based on principals
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean canAccess(String workspaceName) throws RepositoryException {
        if (system || wspAccessMgr == null) {
            return true;
        }
        return wspAccessMgr.grants(subject.getPrincipals(), workspaceName);
    }

    //-----------------------------------------------< AccessControlManager >---
    /**
     * {@inheritDoc}
     */
    public boolean hasPrivileges(String absPath, Privilege[] privileges) throws PathNotFoundException, RepositoryException {
        checkInitialized();
        // make sure absPath points to an existing node
        checkValidNodePath(absPath);

        if (privileges == null || privileges.length == 0) {
            // null or empty privilege array -> return true
            return true;
        } else {
            if (system) {
                // system has always all permissions
                return true;
            } else if (anonymous) {
                if (privileges.length != 1 || !privileges[0].equals(privilegeManager.getPrivilege(Privilege.JCR_READ))) {
                    // anonymous is only granted READ permissions
                    return false;
                }
            }

            // @todo check permission based on principals
            return true;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Privilege[] getPrivileges(String absPath) throws PathNotFoundException, RepositoryException {
        checkInitialized();
        checkValidNodePath(absPath);

        Privilege priv;
        if (anonymous) {
            priv = privilegeManager.getPrivilege(Privilege.JCR_READ);
        } else if (system) {
            priv = privilegeManager.getPrivilege(Privilege.JCR_ALL);
        } else {
            // @todo check permission based on principals
            priv = privilegeManager.getPrivilege(Privilege.JCR_ALL);
        }
        return new Privilege[] {priv};
    }

    /**
     * {@inheritDoc}
     */
    public AccessControlPolicy[] getEffectivePolicies(String absPath) throws PathNotFoundException, AccessDeniedException, RepositoryException {
        checkInitialized();
        checkPermission(absPath, Permission.READ_AC);

        return new AccessControlPolicy[] {POLICY};
    }

    //---------------------------------------< AbstractAccessControlManager >---
    /**
     * @see AbstractAccessControlManager#checkInitialized()
     */
    @Override
    protected void checkInitialized() throws IllegalStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }
    }

    /**
     * @see AbstractAccessControlManager#checkPermission(String,int)
     */
    @Override
    protected void checkPermission(String absPath, int permission) throws AccessDeniedException, PathNotFoundException, RepositoryException {
        checkValidNodePath(absPath);
        if (anonymous && permission != Permission.READ) {
            throw new AccessDeniedException("Anonymous may only READ.");
        }
    }

    /**
     * @see AbstractAccessControlManager#getPrivilegeManager()
     */
    @Override
    protected PrivilegeManager getPrivilegeManager() throws RepositoryException {
        return privilegeManager;
    }

    /**
     * @see AbstractAccessControlManager#checkValidNodePath(String)
     */
    @Override
    protected void checkValidNodePath(String absPath) throws PathNotFoundException, RepositoryException {
        if (absPath != null) {
            Path path = resolver.getQPath(absPath);
            if (!path.isAbsolute()) {
                throw new RepositoryException("Absolute path expected. Found: " + absPath);
            }

            if (hierMgr.resolveNodePath(path) == null) {
                throw new PathNotFoundException(absPath);
            }
        }
    }

    /**
     * @see org.apache.jackrabbit.api.security.JackrabbitAccessControlManager#getEffectivePolicies(Set)
     */
    public AccessControlPolicy[] getEffectivePolicies(Set<Principal> principals) throws AccessDeniedException, AccessControlException, UnsupportedRepositoryOperationException, RepositoryException {
        checkInitialized();
        /*
         TOBEFIXED:
         check permissions on the root node as a workaround to only expose
         effective policies for principals that are allowed to see ac content.
        */
        checkPermission(resolver.getQPath("/"), Permission.READ_AC);

        return new AccessControlPolicy[] {POLICY};
    }

    /**
     * @see org.apache.jackrabbit.api.security.JackrabbitAccessControlManager#hasPrivileges(String, Set, Privilege[])
     */
    public boolean hasPrivileges(String absPath, Set<Principal> principals, Privilege[] privileges) throws PathNotFoundException, RepositoryException {
        if (anonymous) {
            // anonymous doesn't have READ_AC privilege
            throw new AccessDeniedException();
        }
        
        if (principals.size() == 1) {
            Principal princ = principals.iterator().next();
            if (princ instanceof AnonymousPrincipal) {
                return privileges.length == 1 && privileges[0].equals(privilegeManager.getPrivilege(Privilege.JCR_READ));
            }
        }

        // @todo check permission based on principals
        return true;
    }

    /**
     * @see org.apache.jackrabbit.api.security.JackrabbitAccessControlManager#getPrivileges(String, Set)
     */
    public Privilege[] getPrivileges(String absPath, Set<Principal> principals) throws PathNotFoundException, RepositoryException {
        if (anonymous) {
            // anonymous doesn't have READ_AC privilege
            throw new AccessDeniedException();
        }

        if (principals.size() == 1) {
            Principal princ = principals.iterator().next();
            if (princ instanceof AnonymousPrincipal) {
                return new Privilege[] {privilegeManager.getPrivilege(Privilege.JCR_READ)};
            }
        }

        // @todo check permission based on principals
        return new Privilege[] {privilegeManager.getPrivilege(Privilege.JCR_ALL)};
    }
}
