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

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.security.AMContext;
import org.apache.jackrabbit.core.security.AbstractAccessControlManager;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.AnonymousPrincipal;
import org.apache.jackrabbit.core.security.SystemPrincipal;
import org.apache.jackrabbit.core.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.security.authorization.PolicyTemplate;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.core.security.authorization.WorkspaceAccessManager;
import org.apache.jackrabbit.core.security.jsr283.security.AccessControlEntry;
import org.apache.jackrabbit.core.security.jsr283.security.AccessControlException;
import org.apache.jackrabbit.core.security.jsr283.security.AccessControlPolicy;
import org.apache.jackrabbit.core.security.jsr283.security.Privilege;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;

import javax.jcr.AccessDeniedException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.security.auth.Subject;
import java.security.Principal;

/**
 * <code>SimpleAccessManager</code> ...
 */
public class SimpleAccessManager extends AbstractAccessControlManager implements AccessManager {

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

    //--------------------------------------------------------< AccessManager >
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
    public boolean isGranted(ItemId id, int permissions) throws RepositoryException {
        checkInitialized();
        if (system) {
            // system has always all permissions
            return true;
        } else if (anonymous) {
            // anonymous is always denied WRITE & REMOVE premissions
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

    public boolean canRead(Path itemPath) throws RepositoryException {
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
            // anonymous is only granted READ premissions
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
            int bits = PrivilegeRegistry.getBits(privileges);
            if (system) {
                // system has always all permissions
                return true;
            } else if (anonymous) {
                if (bits != PrivilegeRegistry.READ) {
                    // anonymous is only granted READ premissions
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

        if (anonymous) {
            return new Privilege[] {PrivilegeRegistry.READ_PRIVILEGE};
        } else if (system) {
            return new Privilege[] {PrivilegeRegistry.ALL_PRIVILEGE};
        } else {
            // @todo check permission based on principals
            return new Privilege[] {PrivilegeRegistry.ALL_PRIVILEGE};
        }
    }

    /**
     * {@inheritDoc}
     */
    public AccessControlPolicy getEffectivePolicy(String absPath) throws PathNotFoundException, AccessDeniedException, RepositoryException {
        checkInitialized();
        checkPrivileges(absPath, PrivilegeRegistry.READ_AC);

        return new AccessControlPolicy() {
            public String getName() throws RepositoryException {
                return "Simple AccessControlPolicy";
            }
            public String getDescription() throws RepositoryException {
                return null;
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    public AccessControlEntry[] getEffectiveAccessControlEntries(String absPath) throws PathNotFoundException, AccessDeniedException, RepositoryException {
        checkInitialized();
        checkPrivileges(absPath, PrivilegeRegistry.READ_AC);

        return new AccessControlEntry[0];
    }

    //-------------------------------------< JackrabbitAccessControlManager >---
    /**
     * {@inheritDoc}
     */
    public PolicyTemplate editPolicy(String absPath) throws AccessDeniedException, AccessControlException, UnsupportedRepositoryOperationException, RepositoryException {
        checkInitialized();
        checkPrivileges(absPath, PrivilegeRegistry.MODIFY_AC);

        throw new UnsupportedRepositoryOperationException("Editing is not supported");
    }

    /**
     * {@inheritDoc}
     */
    public PolicyTemplate editPolicy(Principal principal) throws AccessDeniedException, AccessControlException, UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException("Editing is not supported");
    }

    //---------------------------------------< AbstractAccessControlManager >---
    /**
     * {@inheritDoc}
     */
    protected void checkInitialized() throws IllegalStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }
    }

    protected void checkPrivileges(String absPath, int privileges) throws AccessDeniedException, PathNotFoundException, RepositoryException {
        checkValidNodePath(absPath);
        if (anonymous && privileges != PrivilegeRegistry.READ) {
            throw new AccessDeniedException("Anonymous may only READ.");
        }
    }

    protected void checkValidNodePath(String absPath) throws PathNotFoundException, RepositoryException {
        Path path = resolver.getQPath(absPath);
        if (!path.isAbsolute()) {
            throw new RepositoryException("Absolute path expected. Found: " + absPath);
        }

        if (hierMgr.resolveNodePath(path) == null) {
            throw new PathNotFoundException(absPath);
        }
    }
}
