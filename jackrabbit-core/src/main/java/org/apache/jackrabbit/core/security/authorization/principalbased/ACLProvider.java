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

import org.apache.jackrabbit.core.security.authorization.AbstractAccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.AccessControlConstants;
import org.apache.jackrabbit.core.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.core.security.authorization.AccessControlEditor;
import org.apache.jackrabbit.core.security.authorization.CompiledPermissions;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.security.authorization.AbstractCompiledPermissions;
import org.apache.jackrabbit.core.security.authorization.AccessControlUtils;
import org.apache.jackrabbit.core.security.authorization.UnmodifiableAccessControlList;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.core.security.principal.PrincipalImpl;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.ItemImpl;
import org.apache.jackrabbit.core.observation.SynchronousEventListener;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.jsr283.security.AccessControlPolicy;
import org.apache.jackrabbit.api.jsr283.security.AccessControlEntry;
import org.apache.jackrabbit.api.jsr283.security.Privilege;
import org.apache.jackrabbit.api.jsr283.security.AccessControlManager;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.ValueFactory;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventIterator;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Arrays;
import java.util.HashMap;
import java.security.Principal;

/**
 * <code>CombinedProvider</code>...
 */
public class ACLProvider extends AbstractAccessControlProvider implements AccessControlConstants {

    private static Logger log = LoggerFactory.getLogger(ACLProvider.class);

    // TODO: add means to show effective-policy to a user.

    private ACLEditor editor;
    private NodeImpl acRoot;

    //-------------------------------------------------< AccessControlUtils >---
    /**
     * @see AccessControlUtils#isAcItem(Path)
     */
    public boolean isAcItem(Path absPath) throws RepositoryException {
        Path.Element[] elems = absPath.getElements();
        for (int i = 0; i < elems.length; i++) {
            if (N_POLICY.equals(elems[i].getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @see AccessControlUtils#isAcItem(ItemImpl)
     */
    public boolean isAcItem(ItemImpl item) throws RepositoryException {
        NodeImpl n = ((item.isNode()) ? (NodeImpl) item : (NodeImpl) item.getParent());
        return n.isNodeType(NT_REP_ACL) || n.isNodeType(NT_REP_ACE);
    }

    //----------------------------------------------< AccessControlProvider >---
    /**
     * @see AccessControlProvider#init(javax.jcr.Session, java.util.Map)
     */
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
        if (!configuration.containsKey(PARAM_OMIT_DEFAULT_PERMISSIONS)) {
            try {
                log.debug("Install initial permissions: ...");

                ValueFactory vf = session.getValueFactory();
                Map restrictions = new HashMap();
                restrictions.put(session.getJCRName(ACLTemplate.P_NODE_PATH), vf.createValue(root.getPath(), PropertyType.PATH));
                restrictions.put(session.getJCRName(ACLTemplate.P_GLOB), vf.createValue(GlobPattern.WILDCARD_ALL));

                PrincipalManager pMgr = session.getPrincipalManager();
                AccessControlManager acMgr = session.getAccessControlManager();
                Principal administrators;
                String pName = SecurityConstants.ADMINISTRATORS_NAME;
                if (pMgr.hasPrincipal(pName)) {
                    administrators = pMgr.getPrincipal(pName);
                } else {
                    log.warn("Administrators principal group is missing.");
                    administrators = new PrincipalImpl(pName);
                }
                AccessControlPolicy[] acls = editor.editAccessControlPolicies(administrators);
                ACLTemplate acl = (ACLTemplate) acls[0];
                if (acl.isEmpty()) {
                    log.debug("... Privilege.ALL for administrators principal.");
                    acl.addEntry(administrators,
                            new Privilege[] {acMgr.privilegeFromName(Privilege.JCR_ALL)},
                            true, restrictions);
                    editor.setPolicy(acl.getPath(), acl);
                } else {
                    log.debug("... policy for administrators principal already present.");
                }

                Principal everyone = pMgr.getEveryone();
                acls = editor.editAccessControlPolicies(everyone);
                acl = (ACLTemplate) acls[0];
                if (acl.isEmpty()) {
                    log.debug("... Privilege.READ for everyone principal.");
                    acl.addEntry(everyone,
                            new Privilege[] {acMgr.privilegeFromName(Privilege.JCR_READ)},
                            true, restrictions);
                    editor.setPolicy(acl.getPath(), acl);
                } else {
                    log.debug("... policy for everyone principal already present.");
                }

                session.save();
            } catch (RepositoryException e) {
                log.error("Failed to set-up minimal access control for root node of workspace " + session.getWorkspace().getName());
                session.getRootNode().refresh(false);
            }
        }
    }

    /**
     * @see AccessControlProvider#getEffectivePolicies(Path)
     */
    public AccessControlPolicy[] getEffectivePolicies(Path absPath)
            throws ItemNotFoundException, RepositoryException {
        /* 
           TODO review
           since the per-node effect of the policies is defined by the
           rep:nodePath restriction, returning the principal-based
           policy at 'absPath' probably doesn't reveal what the caller expects.
           Maybe it would be better not to return an empty array as
           {@link AccessControlManager#getEffectivePolicies(String)
           is defined to express a best-effor estimate only.
        */
        AccessControlPolicy[] tmpls = editor.getPolicies(session.getJCRPath(absPath));
        AccessControlPolicy[] effectives = new AccessControlPolicy[tmpls.length];
        for (int i = 0; i < tmpls.length; i++) {
            effectives[i] = new UnmodifiableAccessControlList((ACLTemplate) tmpls[i]);
        }
        return effectives;
    }

    /**
     * @see AccessControlProvider#getEditor(Session)
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
     * @see AccessControlProvider#compilePermissions(Set)
     */
    public CompiledPermissions compilePermissions(Set principals) throws RepositoryException {
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
     * @see AccessControlProvider#canAccessRoot(Set)
     */
    public boolean canAccessRoot(Set principals) throws RepositoryException {
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

        private final Set principals;
        private final Set acPaths;
        private ACLProvider.Entries entries;

        /**
         * @param principals
         * @throws RepositoryException
         */
        private CompiledPermissionImpl(Set principals) throws RepositoryException {
            this(principals, true);
        }

        /**
         * @param principals
         * @throws RepositoryException
         */
        private CompiledPermissionImpl(Set principals, boolean listenToEvents) throws RepositoryException {

            this.principals = principals;
            acPaths = new HashSet(principals.size());
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
                result = entries.getResult(item, item.getPath(), isAcItem);
            } else {
                result = entries.getResult(null, jcrPath, isAcItem);
            }
            return result;
        }

        //--------------------------------------------< CompiledPermissions >---
        /**
         * @see CompiledPermissions#close()
         */
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
         * @see EventListener#onEvent(EventIterator)
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
         * @return
         * @throws RepositoryException
         */
        private ACLProvider.Entries reload() throws RepositoryException {
            // reload the paths
            acPaths.clear();

            // acNodes must be ordered in the same order as the principals
            // in order to obtain proper acl-evalution in case the given
            // principal-set is ordered.
            List allACEs = new ArrayList();
            // build acl-hierarchy assuming that principal-order determines the
            // acl-inheritance.
            for (Iterator it = principals.iterator(); it.hasNext();) {
                Principal princ = (Principal) it.next();
                ACLTemplate acl = editor.getACL(princ);
                if (acl == null || acl.isEmpty()) {
                    acPaths.add(editor.getPathToAcNode(princ));
                } else {
                    // retrieve the ACEs from the node
                    AccessControlEntry[] aces = acl.getAccessControlEntries();
                    allACEs.addAll(Arrays.asList(aces));
                    acPaths.add(acl.getPath());
                }
            }

            return new ACLProvider.Entries(allACEs);
        }
    }

    //--------------------------------------------------------------------------
    /**
     * Utility class that raps a list of access control entries and evaluates
     * them for a specified item/path.
     */
    private class Entries {

        private final List entries;

        /**
         *
         * @param entries
         */
        private Entries(List entries) {
            this.entries = entries;
        }

        /**
         * Loop over all entries and evaluate allows/denies for those matching
         * the given jcrPath.
         *
         * @param target Existing target item for which the permissions will be
         * evaluated or <code>null</code>.
         * @param targetPath Path used for the evaluation; pointing to an
         * existing or non-existing item.
         * @param isAcItem
         * @return
         * @throws RepositoryException
         */
        private AbstractCompiledPermissions.Result getResult(Item target,
                                                             String targetPath,
                                                             boolean isAcItem) throws RepositoryException {
            int allows = Permission.NONE;
            int denies = Permission.NONE;
            int allowPrivileges = PrivilegeRegistry.NO_PRIVILEGE;
            int denyPrivileges = PrivilegeRegistry.NO_PRIVILEGE;
            int parentAllows = PrivilegeRegistry.NO_PRIVILEGE;
            int parentDenies = PrivilegeRegistry.NO_PRIVILEGE;

            String parentPath = Text.getRelativeParent(targetPath, 1);
            for (Iterator it = entries.iterator(); it.hasNext() && allows != Permission.ALL;) {
                ACLTemplate.Entry entr = (ACLTemplate.Entry) it.next();
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
            return new AbstractCompiledPermissions.Result(allows, denies, allowPrivileges, denyPrivileges);
        }
    }
}
