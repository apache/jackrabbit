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
package org.apache.jackrabbit.core.security.authorization;

import java.security.Principal;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.ObservationManager;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.core.ItemImpl;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.core.security.SystemPrincipal;
import org.apache.jackrabbit.core.security.principal.AdminPrincipal;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;

/**
 * <code>AbstractAccessControlProvider</code>...
 */
public abstract class AbstractAccessControlProvider implements AccessControlProvider,
        AccessControlUtils, AccessControlConstants {

    /**
     * Constant for the name of the configuration option "omit-default-permission".
     * The option is a flag indicating whether default permissions should be
     * created upon initialization of this provider.
     * <p>
     * If this option is present in the configuration no initial ACL content
     * is created.<br>
     * If this configuration option is omitted the default permissions are
     * installed. Note however, that the initialization should not overwrite
     * previously installed AC content.
     */
    public static final String PARAM_OMIT_DEFAULT_PERMISSIONS = "omit-default-permission";

    /**
     * the system session this provider has been created for.
     */
    protected SessionImpl session;
    protected ObservationManager observationMgr;
    protected PrivilegeManagerImpl privilegeManager;

    private boolean initialized;

    protected AbstractAccessControlProvider() {
    }

    /**
     * Throws <code>IllegalStateException</code> if the provider has not
     * been initialized or has been closed.
     */
    protected void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Not initialized or already closed.");
        }
    }

    /**
     * @return the PrivilegeManager
     * @throws RepositoryException
     */
    protected PrivilegeManagerImpl getPrivilegeManagerImpl() throws RepositoryException {
        return privilegeManager;
    }

    /**
     * Returns compiled permissions for the administrator i.e. permissions
     * that grants everything and returns the int representation of {@link Privilege#JCR_ALL}
     * upon {@link CompiledPermissions#getPrivileges(Path)} for all
     * paths.
     *
     * @return an implementation of <code>CompiledPermissions</code> that
     * grants everything and always returns the int representation of
     * {@link Privilege#JCR_ALL} upon {@link CompiledPermissions#getPrivileges(Path)}.
     */
    protected CompiledPermissions getAdminPermissions() {
        return new CompiledPermissions() {
            public void close() {
                //nop
            }
            public boolean grants(Path absPath, int permissions) {
                return true;
            }
            public int getPrivileges(Path absPath) throws RepositoryException {
                return PrivilegeRegistry.getBits(new Privilege[] {getAllPrivilege()});
            }
            public boolean hasPrivileges(Path absPath, Privilege... privileges) {
                return true;
            }
            public Set<Privilege> getPrivilegeSet(Path absPath) throws RepositoryException {
                return Collections.singleton(getAllPrivilege());
            }
            public boolean canReadAll() {
                return true;
            }
            public boolean canRead(Path itemPath, ItemId itemId) {
                return true;
            }

            private Privilege getAllPrivilege() throws RepositoryException {
                return getPrivilegeManagerImpl().getPrivilege(Privilege.JCR_ALL);
            }
        };
    }

    /**
     * Returns compiled permissions for a read-only user i.e. permissions
     * that grants READ permission for all non-AC items.
     *
     * @return an implementation of <code>CompiledPermissions</code> that
     * grants READ permission for all non-AC items.
     */
    protected CompiledPermissions getReadOnlyPermissions() {
        return new CompiledPermissions() {
            public void close() {
                //nop
            }
            public boolean grants(Path absPath, int permissions) throws RepositoryException {
                if (isAcItem(absPath)) {
                    // read-only never has read-AC permission
                    return false;
                } else {
                    return permissions == Permission.READ;
                }
            }
            public int getPrivileges(Path absPath) throws RepositoryException {
                if (isAcItem(absPath)) {
                    return PrivilegeRegistry.NO_PRIVILEGE;
                } else {
                    return PrivilegeRegistry.getBits(new Privilege[] {getReadPrivilege()});
                }
            }
            public boolean hasPrivileges(Path absPath, Privilege... privileges) throws RepositoryException {
                if (isAcItem(absPath)) {
                    return false;
                } else {
                    return privileges != null && privileges.length == 1 && getReadPrivilege().equals(privileges[0]);
                }
            }
            public Set<Privilege> getPrivilegeSet(Path absPath) throws RepositoryException {
                if (isAcItem(absPath)) {
                    return Collections.emptySet();
                } else {
                    return Collections.singleton(getReadPrivilege());
                }
            }
            public boolean canReadAll() {
                return false;
            }
            public boolean canRead(Path itemPath, ItemId itemId) throws RepositoryException {
                if (itemPath != null) {
                    return !isAcItem(itemPath);
                } else {
                    return !isAcItem(session.getItemManager().getItem(itemId));
                }
            }

            private Privilege getReadPrivilege() throws RepositoryException {
                return getPrivilegeManagerImpl().getPrivilege(Privilege.JCR_READ);
            }
        };
    }

    //-------------------------------------------------< AccessControlUtils >---
    /**
     * @see org.apache.jackrabbit.core.security.authorization.AccessControlUtils#isAcItem(Path)
     */
    public boolean isAcItem(Path absPath) throws RepositoryException {
        Path.Element[] elems = absPath.getElements();
        // start looking for a rep:policy name starting from the last element.
        // NOTE: with the current content structure max. 3 levels must be looked
        // at as the rep:policy node may only have ACE nodes with properties.
        if (elems.length > 1) {
            for (int index = elems.length-1, j = 1; index >= 0 && j <= 3; index--, j++) {
                if (N_POLICY.equals(elems[index].getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Test if the given node is itself a rep:ACL or a rep:ACE node.
     * @see org.apache.jackrabbit.core.security.authorization.AccessControlUtils#isAcItem(org.apache.jackrabbit.core.ItemImpl)
     */
    public boolean isAcItem(ItemImpl item) throws RepositoryException {
        NodeImpl n = ((item.isNode()) ? (NodeImpl) item : (NodeImpl) item.getParent());
        Name ntName = ((NodeTypeImpl) n.getPrimaryNodeType()).getQName();
        return ntName.equals(NT_REP_ACL) ||
                ntName.equals(NT_REP_GRANT_ACE) ||
                ntName.equals(NT_REP_DENY_ACE);
    }

    /**
     * @see AccessControlUtils#isAdminOrSystem(Set)
     */
    public boolean isAdminOrSystem(Set<Principal> principals) {
        for (Principal p : principals) {
            if (p instanceof AdminPrincipal || p instanceof SystemPrincipal) {
                return true;
            }
        }
        return false;
    }

    /**
     * @see AccessControlUtils#isReadOnly(Set)
     */
    public boolean isReadOnly(Set<Principal> principals) {
        // TODO: find ways to determine read-only status
        return false;
    }

    //----------------------------------------------< AccessControlProvider >---
    /**
     * Tests if the given <code>systemSession</code> is a SessionImpl and
     * retrieves the observation manager. The it sets the internal 'initialized'
     * field to true.
     *
     * @throws RepositoryException If the specified session is not a
     * <code>SessionImpl</code> or if retrieving the observation manager fails.
     * @see AccessControlProvider#init(Session, Map)
     */
    public void init(Session systemSession, Map configuration) throws RepositoryException {
        if (initialized) {
            throw new IllegalStateException("already initialized");
        }
        if (!(systemSession instanceof SessionImpl)) {
            throw new RepositoryException("SessionImpl (system session) expected.");
        }
        session = (SessionImpl) systemSession;
        observationMgr = systemSession.getWorkspace().getObservationManager();
        privilegeManager = (PrivilegeManagerImpl) ((JackrabbitWorkspace) session.getWorkspace()).getPrivilegeManager();

        initialized = true;
    }

    /**
     * @see AccessControlProvider#close()
     */
    public void close() {
        checkInitialized();
        initialized = false;
    }

    /**
     * @see AccessControlProvider#isLive()
     */
    public boolean isLive() {
        return initialized && session.isLive();
    }
}
