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

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.core.ItemImpl;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.observation.SynchronousEventListener;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.core.security.authorization.AbstractAccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.AbstractCompiledPermissions;
import org.apache.jackrabbit.core.security.authorization.AccessControlConstants;
import org.apache.jackrabbit.core.security.authorization.AccessControlEditor;
import org.apache.jackrabbit.core.security.authorization.CompiledPermissions;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>CombinedProvider</code>...
 */
public class ACLProvider extends AbstractAccessControlProvider implements AccessControlConstants {

    private static Logger log = LoggerFactory.getLogger(ACLProvider.class);

    // TODO: add means to show effective-policy to a user.
    private static final AccessControlPolicy effectivePolicy = EffectivePrincipalBasedPolicy.getInstance();

    private ACLEditor editor;

    private NodeImpl acRoot;

    //-------------------------------------------------< AccessControlUtils >---
    /**
     * @see org.apache.jackrabbit.core.security.authorization.AccessControlUtils#isAcItem(Path)
     */
    public boolean isAcItem(Path absPath) throws RepositoryException {
        Path.Element[] elems = absPath.getElements();
        for (Path.Element elem : elems) {
            if (N_POLICY.equals(elem.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @see org.apache.jackrabbit.core.security.authorization.AccessControlUtils#isAcItem(ItemImpl)
     */
    public boolean isAcItem(ItemImpl item) throws RepositoryException {
        NodeImpl n = ((item.isNode()) ? (NodeImpl) item : (NodeImpl) item.getParent());
        return n.isNodeType(NT_REP_ACL) || n.isNodeType(NT_REP_ACE);
    }

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

        editor = new ACLEditor(session, resolver.getQPath(acRoot.getPath()));
        // TODO: replace by configurable default policy (see JCR-2331)
        if (!configuration.containsKey(PARAM_OMIT_DEFAULT_PERMISSIONS)) {
            try {
                log.debug("Install initial permissions: ...");

                ValueFactory vf = session.getValueFactory();
                Map<String, Value> restrictions = new HashMap<String, Value>();
                restrictions.put(session.getJCRName(ACLTemplate.P_NODE_PATH), vf.createValue(root.getPath(), PropertyType.PATH));
                restrictions.put(session.getJCRName(ACLTemplate.P_GLOB), vf.createValue(GlobPattern.WILDCARD_ALL));

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
     * @see org.apache.jackrabbit.core.security.authorization.AccessControlProvider#getEffectivePolicies(Path)
     */
    public AccessControlPolicy[] getEffectivePolicies(Path absPath)
            throws ItemNotFoundException, RepositoryException {
        /*
           since the per-node effect of the policies is defined by the
           rep:nodePath restriction present with the individual access control
           entries, returning the principal-based policy at 'absPath' (which for
           most nodes in the repository isn't available anyway) doesn't
           provide the desired information.
           As tmp. solution some default policy is returned indicating.
           TODO: add proper evaluation and return a set of ACLs that take effect on the node at abs path
        */
        return new AccessControlPolicy[] {effectivePolicy};
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
                log.error("Internal error: ", e.getMessage());
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
            return new ACLProvider.CompiledPermissionImpl(principals);
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
            CompiledPermissions cp = new CompiledPermissionImpl(principals, false);
            return cp.grants(PathFactoryImpl.getInstance().getRootPath(), Permission.READ);
        }
    }

    //-----------------------------------------------------< CompiledPolicy >---
    /**
     *
     */
    private class CompiledPermissionImpl extends AbstractCompiledPermissions
            implements SynchronousEventListener {

        private final Set<Principal> principals;
        private final Set<String> acPaths;
        private List<AccessControlEntry> entries;

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
            entries = reload();

            // TODO: describe
            if (listenToEvents) {
                int events = Event.PROPERTY_CHANGED | Event.PROPERTY_ADDED |
                        Event.PROPERTY_REMOVED | Event.NODE_ADDED | Event.NODE_REMOVED;
                String[] ntNames = new String[] {
                        session.getJCRName(NT_REP_ACE)
                };
                observationMgr.addEventListener(this, events, acRoot.getPath(), true, null, ntNames, false);
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
            Result result;
            if (session.itemExists(jcrPath)) {
                Item item = session.getItem(jcrPath);
                result = getResult(item, item.getPath(), isAcItem);
            } else {
                result = getResult(null, jcrPath, isAcItem);
            }
            return result;
        }


        /**
         * Loop over all entries and evaluate allows/denies for those matching
         * the given jcrPath.
         *
         * @param target Existing target item for which the permissions will be
         * evaluated or <code>null</code>.
         * @param targetPath Path used for the evaluation; pointing to an
         * existing or non-existing item.
         * @param isAcItem the item
         * @return the result
         * @throws RepositoryException if an error occurs
         */
        private Result getResult(Item target,
                                 String targetPath,
                                 boolean isAcItem) throws RepositoryException {
            int allows = Permission.NONE;
            int denies = Permission.NONE;
            int allowPrivileges = PrivilegeRegistry.NO_PRIVILEGE;
            int denyPrivileges = PrivilegeRegistry.NO_PRIVILEGE;
            int parentAllows = PrivilegeRegistry.NO_PRIVILEGE;
            int parentDenies = PrivilegeRegistry.NO_PRIVILEGE;

            String parentPath = Text.getRelativeParent(targetPath, 1);
            for (AccessControlEntry entry : entries) {
                if (!(entry instanceof ACLTemplate.Entry)) {
                    log.warn("Unexpected AccessControlEntry instance -> ignore");
                    continue;
                }
                ACLTemplate.Entry entr = (ACLTemplate.Entry) entry;
                int privs = entr.getPrivilegeBits();

                if (!"".equals(parentPath) && entr.matches(parentPath)) {
                    if (entr.isAllow()) {
                        parentAllows |= Permission.diff(privs, parentDenies);
                    } else {
                        parentDenies |= Permission.diff(privs, parentAllows);
                    }
                }

                boolean matches = (target != null) ? entr.matches(target) : entr.matches(targetPath);
                if (matches) {
                    if (entr.isAllow()) {
                        allowPrivileges |= Permission.diff(privs, denyPrivileges);
                        int permissions = PrivilegeRegistry.calculatePermissions(allowPrivileges, parentAllows, true, isAcItem);
                        allows |= Permission.diff(permissions, denies);
                    } else {
                        denyPrivileges |= Permission.diff(privs, allowPrivileges);
                        int permissions = PrivilegeRegistry.calculatePermissions(denyPrivileges, parentDenies, false, isAcItem);
                        denies |= Permission.diff(permissions, allows);
                    }
                }
            }
            return new Result(allows, denies, allowPrivileges, denyPrivileges);
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
                log.debug("Unable to unregister listener: ", e.getMessage());
            }
            super.close();
        }

        //--------------------------------------------------< EventListener >---
        /**
         * @see javax.jcr.observation.EventListener#onEvent(EventIterator)
         */
        public synchronized void onEvent(EventIterator events) {
            try {
                boolean reload = false;
                while (events.hasNext() && !reload) {
                    Event ev = events.nextEvent();
                    String path = ev.getPath();
                    // only invalidate cache if any of the events affects the
                    // nodes defining permissions for the principals.
                    switch (ev.getType()) {
                        case Event.NODE_ADDED:
                        case Event.NODE_REMOVED:
                        case Event.NODE_MOVED:
                            reload = acPaths.contains(Text.getRelativeParent(path, 2));
                            break;
                        case Event.PROPERTY_ADDED:
                        case Event.PROPERTY_CHANGED:
                        case Event.PROPERTY_REMOVED:
                            reload = acPaths.contains(Text.getRelativeParent(path, 3));
                            break;

                        default:
                            // illegal event-type: should never occur. ignore
                            break;
                    }
                }
                // eventually reload the ACL and clear the cache
                if (reload) {
                    clearCache();
                    // reload the acl
                    entries = reload();
                }
            } catch (RepositoryException e) {
                // should never get here
                log.warn("Internal error: ", e.getMessage());
            }
        }

        /**
         *
         * @return the aces
         * @throws RepositoryException if an error occurs
         */
        private List<AccessControlEntry> reload() throws RepositoryException {
            // reload the paths
            acPaths.clear();

            // acNodes must be ordered in the same order as the principals
            // in order to obtain proper acl-evaluation in case the given
            // principal-set is ordered.
            List<AccessControlEntry> allACEs = new ArrayList<AccessControlEntry>();
            // build acl-hierarchy assuming that principal-order determines the
            // acl-inheritance.
            for (Principal p : principals) {
                ACLTemplate acl = editor.getACL(p);
                if (acl == null || acl.isEmpty()) {
                    acPaths.add(editor.getPathToAcNode(p));
                } else {
                    // retrieve the ACEs from the node
                    AccessControlEntry[] aces = acl.getAccessControlEntries();
                    allACEs.addAll(Arrays.asList(aces));
                    acPaths.add(acl.getPath());
                }
            }

            return allACEs;
        }
    }

    //--------------------------------------------------------------------------
    /**
     * Dummy effective policy 
     */
    private static final class EffectivePrincipalBasedPolicy implements AccessControlPolicy {

        private static EffectivePrincipalBasedPolicy INSTANCE = new EffectivePrincipalBasedPolicy();
        private EffectivePrincipalBasedPolicy() {
        }

        private static EffectivePrincipalBasedPolicy getInstance() {
            return INSTANCE;
        }
    }
}
