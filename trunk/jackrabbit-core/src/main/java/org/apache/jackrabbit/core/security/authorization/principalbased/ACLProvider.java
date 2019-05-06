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
package org.apache.jackrabbit.core.security.authorization.principalbased;

import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.core.cache.GrowingLRUMap;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.core.security.authorization.AbstractAccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.AbstractCompiledPermissions;
import org.apache.jackrabbit.core.security.authorization.AccessControlConstants;
import org.apache.jackrabbit.core.security.authorization.AccessControlEditor;
import org.apache.jackrabbit.core.security.authorization.AccessControlEntryImpl;
import org.apache.jackrabbit.core.security.authorization.AccessControlListener;
import org.apache.jackrabbit.core.security.authorization.AccessControlModifications;
import org.apache.jackrabbit.core.security.authorization.CompiledPermissions;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.security.authorization.PrivilegeBits;
import org.apache.jackrabbit.core.security.authorization.PrivilegeManagerImpl;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.core.security.authorization.UnmodifiableAccessControlList;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.Privilege;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <code>ACLProvider</code>...
 */
public class ACLProvider extends AbstractAccessControlProvider implements AccessControlConstants {

    private static Logger log = LoggerFactory.getLogger(ACLProvider.class);

    private NodeImpl acRoot;    
    private ACLEditor editor;

    private EntriesCache entriesCache;

    //----------------------------------------------< AccessControlProvider >---
    /**
     * @see org.apache.jackrabbit.core.security.authorization.AccessControlProvider#init(javax.jcr.Session, java.util.Map)
     */
    @Override
    public void init(Session systemSession, Map configuration) throws RepositoryException {
        super.init(systemSession, configuration);

        NodeImpl root = (NodeImpl) session.getRootNode();
        if (root.hasNode(N_ACCESSCONTROL)) {
            acRoot = root.getNode(N_ACCESSCONTROL);
            if (!acRoot.isNodeType(NT_REP_ACCESS_CONTROL)) {
                throw new RepositoryException("Error while initializing Access Control Provider: Found ac-root to be wrong node type " + acRoot.getPrimaryNodeType().getName());
            }
        } else {
            acRoot = root.addNode(N_ACCESSCONTROL, NT_REP_ACCESS_CONTROL, null);
        }

        editor = new ACLEditor(session, session.getQPath(acRoot.getPath()));
        entriesCache = new EntriesCache(session, editor, acRoot.getPath());

        // TODO: replace by configurable default policy (see JCR-2331)
        if (!configuration.containsKey(PARAM_OMIT_DEFAULT_PERMISSIONS)) {
            try {
                log.debug("Install initial permissions: ...");

                ValueFactory vf = session.getValueFactory();
                Map<String, Value> restrictions = new HashMap<String, Value>();
                restrictions.put(session.getJCRName(ACLTemplate.P_NODE_PATH), vf.createValue(root.getPath(), PropertyType.PATH));

                PrincipalManager pMgr = session.getPrincipalManager();
                AccessControlManager acMgr = session.getAccessControlManager();

                // initial default permissions for the administrators group                
                String pName = SecurityConstants.ADMINISTRATORS_NAME;
                if (pMgr.hasPrincipal(pName)) {
                    Principal administrators = pMgr.getPrincipal(pName);
                    installDefaultPermissions(administrators,
                        new Privilege[] {acMgr.privilegeFromName(Privilege.JCR_ALL)},
                        restrictions, editor);
                } else {
                    log.info("Administrators principal group is missing -> Not adding default permissions.");
                }

                // initialize default permissions for the everyone group
                installDefaultPermissions(pMgr.getEveryone(),
                        new Privilege[] {acMgr.privilegeFromName(Privilege.JCR_READ)},
                        restrictions, editor);

                session.save();
            } catch (RepositoryException e) {
                log.error("Failed to set-up minimal access control for root node of workspace " + session.getWorkspace().getName());
                session.getRootNode().refresh(false);
            }
        }
    }

    private static void installDefaultPermissions(Principal principal,
                                                  Privilege[] privs,
                                                  Map<String, Value> restrictions,
                                                  AccessControlEditor editor)
            throws RepositoryException, AccessControlException {
        AccessControlPolicy[] acls = editor.editAccessControlPolicies(principal);
        if (acls.length > 0) {
            ACLTemplate acl = (ACLTemplate) acls[0];
            if (acl.isEmpty()) {
                acl.addEntry(principal, privs, true, restrictions);
                editor.setPolicy(acl.getPath(), acl);
            } else {
                log.debug("... policy for principal '"+principal.getName()+"' already present.");
            }
        } else {
            log.debug("... policy for principal  '"+principal.getName()+"'  already present.");
        }
    }

    /**
     * @see org.apache.jackrabbit.core.security.authorization.AccessControlProvider#close()
     */
    @Override
    public void close() {
        super.close();        
        entriesCache.close();
    }

    /**
     * @see org.apache.jackrabbit.core.security.authorization.AccessControlProvider#getEffectivePolicies(org.apache.jackrabbit.spi.Path,org.apache.jackrabbit.core.security.authorization.CompiledPermissions)
     */
    public AccessControlPolicy[] getEffectivePolicies(Path absPath, CompiledPermissions permissions) throws ItemNotFoundException, RepositoryException {
        if (absPath == null) {
            // TODO: JCR-2774
            log.warn("TODO: JCR-2774 - Repository level permissions.");
            return new AccessControlPolicy[0];
        }

        String jcrPath = session.getJCRPath(absPath);
        String pName = ISO9075.encode(session.getJCRName(ACLTemplate.P_NODE_PATH));
        int ancestorCnt = absPath.getAncestorCount();

        // search all ACEs whose rep:nodePath property equals the specified
        // absPath or any of it's ancestors
        StringBuilder stmt = new StringBuilder("/jcr:root");
        stmt.append(acRoot.getPath());
        stmt.append("//element(*,");
        stmt.append(session.getJCRName(NT_REP_ACE));
        stmt.append(")[");
        for (int i = 0; i <= ancestorCnt; i++) {
            String path = Text.getRelativeParent(jcrPath, i);
            if (i > 0) {
                stmt.append(" or ");
            }
            stmt.append("@");
            stmt.append(pName);
            stmt.append("='");
            stmt.append(path.replaceAll("'", "''"));
            stmt.append("'");
        }
        stmt.append("]");
        
        QueryResult result;
        try {
            QueryManager qm = session.getWorkspace().getQueryManager();
            Query q = qm.createQuery(stmt.toString(), Query.XPATH);
            result = q.execute();
        } catch (RepositoryException e) {
            log.error("Unexpected error while searching effective policies. {}", e.getMessage());
            throw new UnsupportedOperationException("Retrieve effective policies at absPath '" +jcrPath+ "' not supported.", e);
        }

        /**
         * Loop over query results and verify that
         * - the corresponding ACE really takes effect on the specified absPath.
         * - the corresponding ACL can be read by the editing session.
         */
        Set<AccessControlPolicy> acls = new LinkedHashSet<AccessControlPolicy>();
        for (NodeIterator it = result.getNodes(); it.hasNext();) {
            Node aceNode = it.nextNode();
            String accessControlledNodePath = Text.getRelativeParent(aceNode.getPath(), 2);
            Path acPath = session.getQPath(accessControlledNodePath);

            AccessControlPolicy[] policies = editor.getPolicies(accessControlledNodePath);
            if (policies.length > 0) {
                ACLTemplate acl = (ACLTemplate) policies[0];
                for (AccessControlEntry ace : acl.getAccessControlEntries()) {
                    ACLTemplate.Entry entry = (ACLTemplate.Entry) ace;
                    if (entry.matches(jcrPath)) {
                        if (permissions.grants(acPath, Permission.READ_AC)) {
                            acls.add(new UnmodifiableAccessControlList(acl));
                            break;
                        } else {
                            throw new AccessDeniedException("Access denied at " + accessControlledNodePath);
                        }
                    }
                }
            }
        }
        return acls.toArray(new AccessControlPolicy[acls.size()]);
    }

    /**
     * @see org.apache.jackrabbit.core.security.authorization.AccessControlProvider#getEffectivePolicies(java.util.Set, CompiledPermissions)
     */
    public AccessControlPolicy[] getEffectivePolicies(Set<Principal> principals, CompiledPermissions permissions) throws RepositoryException {
        List<AccessControlPolicy> acls = new ArrayList<AccessControlPolicy>(principals.size());
        for (Principal principal : principals) {
            ACLTemplate acl = editor.getACL(principal);
            if (acl != null) {
                if (permissions.grants(session.getQPath(acl.getPath()), Permission.READ_AC)) {
                    acls.add(new UnmodifiableAccessControlList(acl));
                } else {
                    throw new AccessDeniedException("Access denied at " + acl.getPath());
                }
            }
        }
        return acls.toArray(new AccessControlPolicy[acls.size()]);
    }

    /**
     * @see org.apache.jackrabbit.core.security.authorization.AccessControlProvider#getEditor(Session)
     */
    public AccessControlEditor getEditor(Session editingSession) {
        checkInitialized();
        if (editingSession instanceof SessionImpl) {
            try {
                return new ACLEditor((SessionImpl) editingSession, session.getQPath(acRoot.getPath()));
            } catch (RepositoryException e) {
                // should never get here
                log.error("Internal error: {}", e.getMessage());
            }
        }

        log.debug("Unable to build access control editor " + ACLEditor.class.getName() + ".");
        return null;
    }

    /**
     * @see org.apache.jackrabbit.core.security.authorization.AccessControlProvider#compilePermissions(Set)
     */
    public CompiledPermissions compilePermissions(Set<Principal> principals) throws RepositoryException {
        checkInitialized();
        if (isAdminOrSystem(principals)) {
            return getAdminPermissions();
        } else if (isReadOnly(principals)) {
            return getReadOnlyPermissions();
        } else {
            return new CompiledPermissionImpl(principals);
        }
    }

    /**
     * @see org.apache.jackrabbit.core.security.authorization.AccessControlProvider#canAccessRoot(Set)
     */
    public boolean canAccessRoot(Set<Principal> principals) throws RepositoryException {
        checkInitialized();
        if (isAdminOrSystem(principals)) {
            return true;
        } else {
            CompiledPermissionImpl cp = new CompiledPermissionImpl(principals, false);
            return cp.canRead(((NodeImpl) session.getRootNode()).getPrimaryPath());
        }
    }

    //-----------------------------------------------------< CompiledPolicy >---
    /**
     *
     */
    private class CompiledPermissionImpl extends AbstractCompiledPermissions
            implements AccessControlListener {

        private final Set<Principal> principals;
        private final Set<String> acPaths;
        private List<AccessControlEntry> entries;

        private boolean canReadAll;

        @SuppressWarnings("unchecked")
        private final Map<ItemId, Boolean> readCache = new GrowingLRUMap(1024, 5000);

        private final Object monitor = new Object();

        /**
         * @param principals the underlying principals
         * @throws RepositoryException if an error occurs
         */
        private CompiledPermissionImpl(Set<Principal> principals) throws RepositoryException {
            this(principals, true);
        }

        /**
         * @param principals the underlying principals
         * @param listenToEvents if <code>true</code> listens to events
         * @throws RepositoryException if an error occurs
         */
        private CompiledPermissionImpl(Set<Principal> principals, boolean listenToEvents) throws RepositoryException {

            this.principals = principals;
            acPaths = new HashSet<String>(principals.size());
            reload();

            if (listenToEvents) {
                /*
                 Make sure this AclPermission recalculates the permissions if
                 any ACL concerning it is modified.
                 */
                 entriesCache.addListener(this);
            }
        }
               
        /**
         * @throws RepositoryException if an error occurs
         */
        private void reload() throws RepositoryException {
            // reload the paths
            acPaths.clear();
            for (Principal p : principals) {
                acPaths.add(editor.getPathToAcNode(p));
            }

            // and retrieve the entries from the entry-collector.
            entries = entriesCache.getEntries(principals);
            
            // in addition: trivial check if read access is denied somewhere
            canReadAll = canRead(session.getQPath("/"));            
            if (canReadAll) {
                for (AccessControlEntry entry : entries) {
                    AccessControlEntryImpl ace = (AccessControlEntryImpl) entry;
                    if (!ace.isAllow() && ace.getPrivilegeBits().includesRead()) {
                        // found an ace that defines read deny for a sub tree
                        // -> canReadAll is false.
                        canReadAll = false;
                        break;
                    }
                }
            }
        }

        //------------------------------------< AbstractCompiledPermissions >---
        /**
         * @see AbstractCompiledPermissions#buildResult(Path)
         */
        @Override
        protected synchronized Result buildResult(Path absPath) throws RepositoryException {
            if (!absPath.isAbsolute()) {
                throw new RepositoryException("Absolute path expected.");
            }

            boolean isAcItem = isAcItem(absPath);
            String jcrPath = session.getJCRPath(absPath);

            // retrieve principal-based permissions and privileges
            return buildResult(jcrPath, isAcItem);
        }

        @Override
        protected Result buildRepositoryResult() throws RepositoryException {
            log.warn("TODO: JCR-2774 - Repository level permissions.");
            PrivilegeManagerImpl pm = getPrivilegeManagerImpl();
            return new Result(Permission.NONE, Permission.NONE, PrivilegeBits.EMPTY, PrivilegeBits.EMPTY);        
        }

        /**
         * @see AbstractCompiledPermissions#getPrivilegeManagerImpl()
         */
        @Override
        protected PrivilegeManagerImpl getPrivilegeManagerImpl() throws RepositoryException {
            return ACLProvider.this.getPrivilegeManagerImpl();
        }

        /**
         * Loop over all entries and evaluate allows/denies for those matching
         * the given jcrPath.
         * 
         * @param targetPath Path used for the evaluation; pointing to an
         * existing or non-existing item.
         * @param isAcItem the item.
         * @return the result
         * @throws RepositoryException if an error occurs
         */
        private Result buildResult(String targetPath,
                                   boolean isAcItem) throws RepositoryException {
            int allows = Permission.NONE;
            int denies = Permission.NONE;

            PrivilegeBits allowBits = PrivilegeBits.getInstance();
            PrivilegeBits denyBits = PrivilegeBits.getInstance();
            PrivilegeBits parentAllowBits = PrivilegeBits.getInstance();
            PrivilegeBits parentDenyBits = PrivilegeBits.getInstance();

            String parentPath = Text.getRelativeParent(targetPath, 1);
            for (AccessControlEntry entry : entries) {
                if (!(entry instanceof ACLTemplate.Entry)) {
                    log.warn("Unexpected AccessControlEntry instance -> ignore");
                    continue;
                }
                ACLTemplate.Entry entr = (ACLTemplate.Entry) entry;
                PrivilegeBits privs = entr.getPrivilegeBits();

                if (!"".equals(parentPath) && entr.matches(parentPath)) {
                    if (entr.isAllow()) {
                        parentAllowBits.addDifference(privs, parentDenyBits);
                    } else {
                        parentDenyBits.addDifference(privs, parentAllowBits);
                    }
                }

                boolean matches = entr.matches(targetPath);
                if (matches) {
                    if (entr.isAllow()) {
                        allowBits.addDifference(privs, denyBits);
                        int permissions = PrivilegeRegistry.calculatePermissions(allowBits, parentAllowBits, true, isAcItem);
                        allows |= Permission.diff(permissions, denies);
                    } else {
                        denyBits.addDifference(privs, allowBits);
                        int permissions = PrivilegeRegistry.calculatePermissions(denyBits, parentDenyBits, false, isAcItem);
                        denies |= Permission.diff(permissions, allows);
                    }
                }
            }

            return new Result(allows, denies, allowBits, denyBits);
        }

        //--------------------------------------------< CompiledPermissions >---
        /**
         * @see CompiledPermissions#close()
         */
        @Override
        public void close() {
            entriesCache.removeListener(this);
            super.close();
        }

        /**
         * @see CompiledPermissions#canRead(Path, ItemId)
         */
        public boolean canRead(Path path, ItemId itemId) throws RepositoryException {
            boolean canRead;
            if (path == null) {
                // only itemId: try to avoid expensive resolution from itemID to path
                synchronized (monitor) {
                    if (readCache.containsKey(itemId)) {
                        // id has been evaluated before -> shortcut
                        canRead = readCache.get(itemId);
                    } else {
                        canRead = canRead(session.getHierarchyManager().getPath(itemId));
                        readCache.put(itemId, canRead);
                        return canRead;
                    }
                }
            } else {
                // path param present:
                canRead = canRead(path);
            }
            return canRead;
        }

        private boolean canRead(Path path) throws RepositoryException {
            // first try if reading non-ac-items was always granted -> no eval
            // otherwise evaluate the permissions.
            return (canReadAll && !isAcItem(path)) || grants(path, Permission.READ);
        }

        //------------------------------------------< AccessControlListener >---
        /**
         * @see AccessControlListener#acModified(org.apache.jackrabbit.core.security.authorization.AccessControlModifications)
         */
        public void acModified(AccessControlModifications modifications) {
            try {
                boolean reload = false;
                Iterator keys = modifications.getNodeIdentifiers().iterator();
                while (keys.hasNext() && !reload) {
                    String path = keys.next().toString();
                    reload = acPaths.contains(path);
                }
                // eventually reload the ACL and clear the cache
                if (reload) {
                    clearCache();
                    // reload the ac-path list and the list of aces
                    reload();
                }
            } catch (RepositoryException e) {
                // should never get here
                log.warn("Internal error: {}", e.getMessage());
            }
        }
    }
}
