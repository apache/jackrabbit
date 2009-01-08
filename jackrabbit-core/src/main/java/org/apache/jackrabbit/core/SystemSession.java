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
package org.apache.jackrabbit.core;

import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.security.AMContext;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.SystemPrincipal;
import org.apache.jackrabbit.core.security.AbstractAccessControlManager;
import org.apache.jackrabbit.core.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.WorkspaceAccessManager;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.api.jsr283.security.AccessControlManager;
import org.apache.jackrabbit.api.jsr283.security.Privilege;
import org.apache.jackrabbit.api.jsr283.security.AccessControlPolicy;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;
import javax.jcr.PathNotFoundException;
import javax.security.auth.Subject;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A <code>SystemSession</code> ...
 */
class SystemSession extends SessionImpl {

    /**
     * Package private factory method
     *
     * @param rep
     * @param wspConfig
     * @return
     * @throws RepositoryException
     */
    static SystemSession create(RepositoryImpl rep, WorkspaceConfig wspConfig)
            throws RepositoryException {
        // create subject with SystemPrincipal
        Set principals = new HashSet();
        principals.add(new SystemPrincipal());
        Subject subject =
                new Subject(true, principals, Collections.EMPTY_SET,
                        Collections.EMPTY_SET);
        return new SystemSession(rep, subject, wspConfig);
    }

    /**
     * private constructor
     *
     * @param rep
     * @param wspConfig
     */
    private SystemSession(RepositoryImpl rep, Subject subject,
                          WorkspaceConfig wspConfig)
            throws RepositoryException {
        super(rep, subject, wspConfig);
    }

    /**
     * Always returns the name of the <code>SystemPrincipal</code>.
     *
     * @return the name of <code>SystemPrincipal</code>.
     */
    protected String retrieveUserId(Subject subject) throws RepositoryException {
        return new SystemPrincipal().getName();
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Overridden in order to create custom access manager
     *
     * @return access manager for system session
     * @throws AccessDeniedException is never thrown
     * @throws RepositoryException   is never thrown
     */
    protected AccessManager createAccessManager(Subject subject,
                                                HierarchyManager hierMgr)
            throws AccessDeniedException, RepositoryException {
        /**
         * use own AccessManager implementation rather than relying on
         * configurable AccessManager to handle SystemPrincipal privileges
         * correctly
         */
        return new SystemAccessManager();
    }

    //--------------------------------------------------------< inner classes >
    private class SystemAccessManager extends AbstractAccessControlManager implements AccessManager {

        private final PrivilegeRegistry privilegeRegistry;

        SystemAccessManager() {
            privilegeRegistry = new PrivilegeRegistry(SystemSession.this);
        }

        //----------------------------------------------------< AccessManager >
        /**
         * {@inheritDoc}
         *
         * @throws AccessDeniedException is never thrown
         * @throws Exception             is never thrown
         */
        public void init(AMContext context)
                throws AccessDeniedException, Exception {
            // nop
        }

        public void init(AMContext context, AccessControlProvider acProvider, WorkspaceAccessManager wspAccessMgr) throws AccessDeniedException, Exception {
            // nop
        }

        /**
         * {@inheritDoc}
         */
        public void close() throws Exception {
            // nop
        }

        /**
         * {@inheritDoc}
         *
         * @throws AccessDeniedException is never thrown
         * @throws ItemNotFoundException is never thrown
         * @throws RepositoryException   is never thrown
         */
        public void checkPermission(ItemId id, int permissions)
                throws AccessDeniedException, RepositoryException {
            // allow everything
        }

        /**
         * {@inheritDoc}
         */
        public void checkPermission(Path absPath, int permissions) throws AccessDeniedException, RepositoryException {
            // allow everything
        }

        /**
         * {@inheritDoc}
         *
         * @return always <code>true</code>
         * @throws ItemNotFoundException is never thrown
         * @throws RepositoryException   is never thrown
         */
        public boolean isGranted(ItemId id, int permissions) throws RepositoryException {
            // allow everything
            return true;
        }

        /**
         * Always returns true.
         *
         * @see AccessManager#isGranted(Path, int)
         */
        public boolean isGranted(Path absPath, int permissions) throws RepositoryException {
            // allow everything
            return true;
        }

        /**
         * Always returns true.
         *
         * @see AccessManager#isGranted(Path, Name, int)
         */
        public boolean isGranted(Path parentPath, Name childName, int permissions) throws RepositoryException {
            // allow everything
            return true;
        }

        /**
         * Always returns true.
         *
         * @see AccessManager#canRead(Path)
         * @param itemPath
         */
        public boolean canRead(Path itemPath) throws RepositoryException {
            return true;
        }

        /**
         * {@inheritDoc}
         *
         * @return always <code>true</code>
         * @throws NoSuchWorkspaceException is never thrown
         * @throws RepositoryException      is never thrown
         */
        public boolean canAccess(String workspaceName) throws RepositoryException {
            return true;
        }

        //-----------------------------------< AbstractAccessControlManager >---
        /**
         * @see AbstractAccessControlManager#checkInitialized()
         */
        protected void checkInitialized() throws IllegalStateException {
            // nop
        }

        /**
         * @see AbstractAccessControlManager#checkPermission(String,int)
         */
        protected void checkPermission(String absPath, int permission) throws
                AccessDeniedException, PathNotFoundException, RepositoryException {
            // allow everything
        }

        /**
         * @see AbstractAccessControlManager#getPrivilegeRegistry()
         */
        protected PrivilegeRegistry getPrivilegeRegistry()
                throws RepositoryException {
            return privilegeRegistry;
        }

        /**
         * @see AbstractAccessControlManager#checkValidNodePath(String)
         */
        protected void checkValidNodePath(String absPath)
                throws PathNotFoundException, RepositoryException {
            Path p = getQPath(absPath);
            if (!p.isAbsolute()) {
                throw new RepositoryException("Absolute path expected.");
            }
            if (hierMgr.resolveNodePath(p) == null) {
                throw new PathNotFoundException("No such node " + absPath);
            }
        }

        //-------------------------------------------< AccessControlManager >---
        /**
         * @see AccessControlManager#hasPrivileges(String, Privilege[])
         */
        public boolean hasPrivileges(String absPath, Privilege[] privileges)
                throws PathNotFoundException, RepositoryException {
            checkValidNodePath(absPath);
            // allow everything
            return true;
        }

        /**
         * @see AccessControlManager#getPrivileges(String)
         */
        public Privilege[] getPrivileges(String absPath)
                throws PathNotFoundException, RepositoryException {
            checkValidNodePath(absPath);
            return new Privilege[] {getPrivilegeRegistry().getPrivilege(Privilege.JCR_ALL)};
        }

        /**
         * @see AccessControlManager#getEffectivePolicies(String)
         */
        public AccessControlPolicy[] getEffectivePolicies(String absPath) throws
                PathNotFoundException, AccessDeniedException, RepositoryException {
            // TODO
            throw new UnsupportedOperationException();
        }
    }
}
