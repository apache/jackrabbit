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
package org.apache.jackrabbit.core.security.authorization.combined;

import org.apache.jackrabbit.core.security.authorization.AbstractAccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.AccessControlEditor;
import org.apache.jackrabbit.core.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.CompiledPermissions;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.security.authorization.AccessControlUtils;
import org.apache.jackrabbit.core.security.authorization.AbstractCompiledPermissions;
import org.apache.jackrabbit.core.security.authorization.PrivilegeManagerImpl;
import org.apache.jackrabbit.core.security.authorization.principalbased.ACLProvider;
import org.apache.jackrabbit.core.ItemImpl;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import javax.jcr.security.AccessControlPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ItemNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Iterator;
import java.security.Principal;

/**
 * <code>CombinedProvider</code>...
 */
public class CombinedProvider extends AbstractAccessControlProvider {

    private static Logger log = LoggerFactory.getLogger(CombinedProvider.class);

    private AccessControlProvider[] providers;

    //-------------------------------------------------< AccessControlUtils >---
    /**
     * @see AccessControlUtils#isAcItem(Path)
     */
    @Override
    public boolean isAcItem(Path absPath) throws RepositoryException {
        for (AccessControlProvider provider : providers) {
            if (provider instanceof AccessControlUtils && ((AccessControlUtils) provider).isAcItem(absPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @see AccessControlUtils#isAcItem(ItemImpl)
     */
    @Override
    public boolean isAcItem(ItemImpl item) throws RepositoryException {
        for (AccessControlProvider provider : providers) {
            if (provider instanceof AccessControlUtils && ((AccessControlUtils) provider).isAcItem(item)) {
                return true;
            }
        }
        return false;
    }

    //----------------------------------------------< AccessControlProvider >---
    /**
     * @see AccessControlProvider#close()
     */
    @Override
    public void close() {
        for (AccessControlProvider provider : providers) {
            provider.close();
        }
        super.close();
    }

    /**
     * @see AccessControlProvider#init(javax.jcr.Session, java.util.Map)
     */
    @Override
    public void init(Session systemSession, Map configuration) throws RepositoryException {
        super.init(systemSession, configuration);

        // this provider combines the result of 2 (currently hard coded) AC-providers
        // TODO: make this configurable
        providers = new AccessControlProvider[2];

        // 1) a resource-based ACL provider, that is not initialized with default
        //    permissions and should only be used to overrule the permissions
        //    granted or denied by the default provider (see 2).
        providers[0] = new org.apache.jackrabbit.core.security.authorization.acl.ACLProvider();
        Map config = new HashMap(configuration);
        config.put(PARAM_OMIT_DEFAULT_PERMISSIONS, Boolean.TRUE);
        providers[0].init(session, config);

        // 2) the principal-base ACL provider which is intended to provide
        //    the default/standard permissions present at an item for a given
        //    set of principals.
        providers[1] = new ACLProvider();
        providers[1].init(session, configuration);
    }

    /**
     * @see AccessControlProvider#getEffectivePolicies(org.apache.jackrabbit.spi.Path,org.apache.jackrabbit.core.security.authorization.CompiledPermissions)
     */
    public AccessControlPolicy[] getEffectivePolicies(Path absPath, CompiledPermissions permissions)
            throws ItemNotFoundException, RepositoryException {
        List<AccessControlPolicy> l = new ArrayList<AccessControlPolicy>();
        for (AccessControlProvider provider : providers) {
            l.addAll(Arrays.asList(provider.getEffectivePolicies(absPath, permissions)));
        }
        return l.toArray(new AccessControlPolicy[l.size()]);
    }

    /**
     * @see AccessControlProvider#getEffectivePolicies(java.util.Set, CompiledPermissions)
     */
    public AccessControlPolicy[] getEffectivePolicies(Set<Principal> principals, CompiledPermissions permissions) throws RepositoryException {
        List<AccessControlPolicy> l = new ArrayList<AccessControlPolicy>();
        for (AccessControlProvider provider : providers) {
            l.addAll(Arrays.asList(provider.getEffectivePolicies(principals, permissions)));
        }
        return l.toArray(new AccessControlPolicy[l.size()]);
    }

    /**
     * @see AccessControlProvider#getEditor(javax.jcr.Session)
     */
    public AccessControlEditor getEditor(Session editingSession) {
        checkInitialized();
        List<AccessControlEditor> editors = new ArrayList<AccessControlEditor>();
        for (AccessControlProvider provider : providers) {
            try {
                editors.add(provider.getEditor(editingSession));
            } catch (RepositoryException e) {
                log.debug(e.getMessage());
                // ignore.
            }
        }
        if (!editors.isEmpty()) {
            return new CombinedEditor(editors.toArray(new AccessControlEditor[editors.size()]));
        } else {
            log.debug("None of the derived access control providers supports editing.");
            return null;
        }
    }

    /**
     * @see AccessControlProvider#compilePermissions(Set)
     */
    public CompiledPermissions compilePermissions(Set<Principal> principals) throws RepositoryException {
        checkInitialized();
        if (isAdminOrSystem(principals)) {
            return getAdminPermissions();
        } else {
            return new CompiledPermissionImpl(principals);
        }
    }

    /**
     * @see AccessControlProvider#canAccessRoot(Set)
     */
    public boolean canAccessRoot(Set<Principal> principals) throws RepositoryException {
        checkInitialized();
        if (isAdminOrSystem(principals)) {
            return true;
        } else {
            CompiledPermissions cp = new CompiledPermissionImpl(principals);
            try {
                Path rootPath = PathFactoryImpl.getInstance().getRootPath();
                return cp.grants(rootPath, Permission.READ);
            } finally {
                cp.close();
            }
        }
    }

    //-----------------------------------------------------< CompiledPolicy >---
    /**
     *
     */
    private class CompiledPermissionImpl extends AbstractCompiledPermissions  {

        private final List<AbstractCompiledPermissions> cPermissions;

        /**
         * @param principals the principals
         * @throws RepositoryException if an error occurs
         */
        private CompiledPermissionImpl(Set<Principal> principals) throws
                RepositoryException {
            this.cPermissions = new ArrayList<AbstractCompiledPermissions>();
            for (AccessControlProvider provider : providers) {
                CompiledPermissions cp = provider.compilePermissions(principals);
                if (cp instanceof AbstractCompiledPermissions) {
                    cPermissions.add((AbstractCompiledPermissions) cp);
                } else {
                    // TODO: deal with other implementations
                    log.warn("AbstractCompiledPermissions expected. Found " + cp.getClass().getName() + " -> ignore.");
                }
            }
        }

        //------------------------------------< AbstractCompiledPermissions >---      
        /**
         * @see AbstractCompiledPermissions#getResult(Path)
         */
        @Override
        public Result getResult(Path absPath) throws RepositoryException {
            // TODO: missing caching
            return buildResult(absPath);
        }

        /**
         * @see AbstractCompiledPermissions#buildResult(Path)
         */
        @Override
        protected Result buildResult(Path absPath) throws RepositoryException {
            Result res = null;
            for (AbstractCompiledPermissions acp : cPermissions) {
                Result other = acp.getResult(absPath);
                res = (res == null) ? other : res.combine(other);
            }
            return res;
        }

        @Override
        protected Result buildRepositoryResult() throws RepositoryException {
            Result res = null;
            for (AbstractCompiledPermissions acp : cPermissions) {
                Result other = acp.getResult(null);
                res = (res == null) ? other : res.combine(other);
            }
            return res;
        }

        /**
         * @see AbstractCompiledPermissions#getPrivilegeManagerImpl()
         */
        @Override
        protected PrivilegeManagerImpl getPrivilegeManagerImpl() throws RepositoryException {
            return CombinedProvider.this.getPrivilegeManagerImpl();
        }

        //--------------------------------------------< CompiledPermissions >---
        /**
         * @see CompiledPermissions#close()
         */
        @Override
        public synchronized void close() {
            // close all c-permissions retained in the list and clear the list.
            for (Iterator<AbstractCompiledPermissions> it = cPermissions.iterator(); it.hasNext();) {
                CompiledPermissions cp = it.next();
                cp.close();
                it.remove();
            }
            super.close();
        }

        /**
         * @see CompiledPermissions#canRead(Path, ItemId)
         */
        public boolean canRead(Path path, ItemId itemId) throws RepositoryException {
            Path p = (path == null) ? session.getItemManager().getItem(itemId).getPrimaryPath() : path;
            return grants(p, Permission.READ);
        }
    }
}