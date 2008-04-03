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

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.core.security.authorization.AbstractAccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.AbstractCompiledPermissions;
import org.apache.jackrabbit.core.security.authorization.AccessControlConstants;
import org.apache.jackrabbit.core.security.authorization.AccessControlEditor;
import org.apache.jackrabbit.core.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.CompiledPermissions;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.security.authorization.PolicyEntry;
import org.apache.jackrabbit.core.security.authorization.PolicyTemplate;
import org.apache.jackrabbit.core.security.jsr283.security.AccessControlEntry;
import org.apache.jackrabbit.core.security.principal.PrincipalImpl;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.Item;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventIterator;
import java.security.Principal;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * <code>CombinedProvider</code>...
 */
public class CombinedProvider extends AbstractAccessControlProvider implements AccessControlConstants {

    private static Logger log = LoggerFactory.getLogger(CombinedProvider.class);

    // TODO: add means to show effective-policy to a user.
    // TODO: TOBEFIXED add means to create user-based ACLs (currently editor is not exposed in the API)
    // TODO: TOBEFIXED proper evaluation of permissions respecting resource-based ACLs.
    // TODO: TOBEFIXED assert proper evaluation order of group/non-group principal-ACLs

    private CombinedEditor editor;
    private NodeImpl acRoot;

    private String policyName;

    public CombinedProvider() {
        super("Combined AC policy", "Policy evaluating user-based and resource-based ACLs.");
    }
    //----------------------------------------------< AccessControlProvider >---
    /**
     * @see AccessControlProvider#init(javax.jcr.Session, java.util.Map)
     */
    public void init(Session systemSession, Map options) throws RepositoryException {
        super.init(systemSession, options);

        NodeImpl root = (NodeImpl) session.getRootNode();
        if (root.hasNode(N_ACCESSCONTROL)) {
            acRoot = root.getNode(N_ACCESSCONTROL);
            if (!acRoot.isNodeType(NT_REP_ACCESS_CONTROL)) {
                throw new RepositoryException("Error while initializing Access Control Provider: Found ac-root to be wrong node type " + acRoot.getPrimaryNodeType().getName());
            }
        } else {
            acRoot = root.addNode(N_ACCESSCONTROL, NT_REP_ACCESS_CONTROL, null);
        }

        policyName = session.getJCRName(AccessControlConstants.N_POLICY);

        editor = new CombinedEditor(session, resolver, resolver.getQPath(acRoot.getPath()));
        try {
            log.info("Install initial ACL:...");

            PrincipalManager pMgr = session.getPrincipalManager();
            log.info("... Privilege.ALL for administrators.");
            Principal administrators;
            String pName = SecurityConstants.ADMINISTRATORS_NAME;
            if (pMgr.hasPrincipal(pName)) {
                administrators = pMgr.getPrincipal(pName);
            } else {
                log.warn("Administrators principal group is missing.");
                administrators = new PrincipalImpl(pName);
            }

            String glob = GlobPattern.WILDCARD_ALL;
            PolicyTemplate pt = editor.editPolicyTemplate(administrators);
            pt.setEntry(new PolicyEntryImpl(administrators, PrivilegeRegistry.ALL, true, root.getPath(), glob));
            editor.setPolicyTemplate(pt.getPath(), pt);

            Principal everyone = pMgr.getEveryone();
            log.info("... Privilege.READ for everyone.");
            pt = editor.editPolicyTemplate(everyone);
            pt.setEntry(new PolicyEntryImpl(everyone, PrivilegeRegistry.READ, true, root.getPath(), glob));
            editor.setPolicyTemplate(pt.getPath(), pt);

            session.save();
            log.info("... done.");

        } catch (RepositoryException e) {
            log.error("Failed to set-up minimal access control for root node of workspace " + session.getWorkspace().getName());
            session.getRootNode().refresh(false);
            throw e;
        }
    }

    /**
     * @see AccessControlProvider#getAccessControlEntries(org.apache.jackrabbit.core.NodeId)
     */
    public AccessControlEntry[] getAccessControlEntries(NodeId nodeId) throws RepositoryException {
        checkInitialized();
        // TODO: TOBEFIXED
        return new AccessControlEntry[0];
    }

    /**
     * @see AccessControlProvider#getEditor(javax.jcr.Session)
     */
    public AccessControlEditor getEditor(Session editingSession) {
        checkInitialized();
        if (editingSession instanceof SessionImpl) {
            try {
                return new CombinedEditor((SessionImpl) editingSession,
                        session.getNamePathResolver(),
                        session.getQPath(acRoot.getPath()));
            } catch (RepositoryException e) {
                // should never get here
                log.error("Internal error:", e.getMessage());
            }
        }

        log.debug("Unable to build access control editor " + CombinedEditor.class.getName() + ".");
        return null;
    }

    /**
     * @see AccessControlProvider#compilePermissions(Set)
     */
    public CompiledPermissions compilePermissions(Set principals) throws ItemNotFoundException, RepositoryException {
        checkInitialized();
        if (isAdminOrSystem(principals)) {
            return getAdminPermissions();
        } else {
            // TODO: TOBEFIXED include the resource-based ACLs!
            return new CompiledPermissionImpl(principals);
        }
    }

    //-----------------------------------------------------< CompiledPolicy >---
    /**
     *
     */
    private class CompiledPermissionImpl extends AbstractCompiledPermissions
            implements EventListener {

        private final Set principals;
        private final Set acPaths;
        private Entries entries;

        /**
         * @param principals
         * @throws RepositoryException
         */
        private CompiledPermissionImpl(Set principals) throws RepositoryException {

            this.principals = principals;
            acPaths = new HashSet(principals.size());
            entries = reload();

            // TODO: describe
            int events = Event.PROPERTY_CHANGED | Event.PROPERTY_ADDED |
                    Event.PROPERTY_REMOVED | Event.NODE_ADDED | Event.NODE_REMOVED;
            String[] ntNames = new String[] {
                    session.getJCRName(NT_REP_ACE)
            };
            observationMgr.addEventListener(this, events, acRoot.getPath(), true, null, ntNames, false);
        }

        //------------------------------------< AbstractCompiledPermissions >---
        /**
         * @see AbstractCompiledPermissions#buildResult(Path)
         */
        protected synchronized Result buildResult(Path absPath) throws RepositoryException {
            if (!absPath.isAbsolute()) {
                throw new RepositoryException("Absolute path expected.");
            }

            String jcrPath = session.getJCRPath(absPath);
            boolean isAclItem = false;
            /* Test if the given path points to a Node (or an existing or non
             * existing direct decendant of an existing Node) that stores
             * AC-information
             */
            String[] segments = Text.explode(jcrPath, '/', false);
            if (segments.length > 0) {
                for (int i = segments.length - 1; i >= 0 && !isAclItem; i--) {
                    isAclItem = policyName.equals(segments[i]);
                }
            }

            int permissions;
            if (session.itemExists(jcrPath)) {
                permissions = entries.getPermissions(session.getItem(jcrPath), isAclItem);
            } else {
                Node parent = null;
                String parentPath = jcrPath;
                while (parent == null) {
                    parentPath = Text.getRelativeParent(parentPath, 1);
                    if (parentPath.length() == 0) {
                        // root node reached
                        parent = session.getRootNode();
                    } else if (session.nodeExists(parentPath)) {
                        parent = session.getNode(parentPath);
                    }
                }
                String relPath = jcrPath.substring(parent.getPath().length() + 1);
                permissions = entries.getPermissions(parent, relPath, isAclItem);
            }
            /* TODO: privileges can only be determined for nodes. */
            int privileges = entries.getPrivileges(jcrPath);
            return new Result(permissions, privileges);
        }

        //--------------------------------------------< CompiledPermissions >---
        /**
         * @see CompiledPermissions#close()
         */
        public void close() {
            try {
                observationMgr.removeEventListener(this);
            } catch (RepositoryException e) {
                log.error("Internal error: ", e.getMessage());
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
                            reload = false;
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
        private Entries reload() throws RepositoryException {
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
                PolicyTemplate at = editor.getPolicyTemplate(princ);
                if (at == null || at.isEmpty()) {
                    log.debug("No matching ACL node found for principal " + princ.getName() + " -> principal ignored.");
                } else {
                    // retrieve the ACEs from the node
                    PolicyEntry[] aces = (PolicyEntry[]) at.getEntries();
                    allACEs.addAll(Arrays.asList(aces));
                    acPaths.add(at.getPath());
                }
            }
            return new Entries(allACEs);
        }
    }

    //--------------------------------------------------------------------------

    private static class Entries {

        private final List entries;

        private Entries(List entries) {
            this.entries = entries;
        }

        /**
         * Loop over all entries and evaluate allows/denies for those matching
         * the given jcrPath.
         *
         * @param target Existing target item for which the permissions will be
         * evaluated.
         * @param protectsACL
         * @return
         * @throws RepositoryException
         */
        private int getPermissions(Item target, boolean protectsACL) throws RepositoryException {
            int allows = 0;
            int denies = 0;
            for (Iterator it = entries.iterator(); it.hasNext() && allows != Permission.ALL;) {
                PolicyEntryImpl entr = (PolicyEntryImpl) it.next();
                if (entr.matches(target)) {
                    int privs = entr.getPrivilegeBits();
                    int permissions = Permission.calculatePermissions(privs, privs, protectsACL);
                    if (entr.isAllow()) {
                        allows |= Permission.diff(permissions, denies);
                    } else {
                        denies |= Permission.diff(permissions, allows);
                    }
                }
            }
            return allows;
        }

        /**
         * loop over all entries and evaluate allows/denies for those matching
         * the given jcrPath.
         *
         * @param parent Existing parent of the target to be evaluated.
         * @param relPath relative path to a non-existing child item to calculate the
         * permissions for.
         * @param protectsACL
         * @return
         * @throws RepositoryException
         */
        private int getPermissions(Node parent, String relPath, boolean protectsACL) throws RepositoryException {
            int allows = 0;
            int denies = 0;
            String jcrPath = parent.getPath() + "/" + relPath;

            for (Iterator it = entries.iterator(); it.hasNext() && allows != Permission.ALL;) {
                PolicyEntryImpl entr = (PolicyEntryImpl) it.next();
                if (entr.matches(jcrPath)) {
                    int privs = entr.getPrivilegeBits();
                    int permissions = Permission.calculatePermissions(privs, privs, protectsACL);
                    if (entr.isAllow()) {
                        allows |= Permission.diff(permissions, denies);
                    } else {
                        denies |= Permission.diff(permissions, allows);
                    }
                }
            }
            return allows;
        }

        private int getPrivileges(String nodePath) throws RepositoryException {
            // TODO: improve. avoid duplicate evaluation...            
            int allows = 0;
            int denies = 0;
            for (Iterator it = entries.iterator(); it.hasNext() && allows != Permission.ALL;) {
                PolicyEntryImpl entr = (PolicyEntryImpl) it.next();
                // loop over all entries and evaluate allows/denies for those
                // matching the given jcrPath
                // TODO: check again which ACEs must be respected.
                // TODO: maybe ancestor-defs only if glob = *?
                String np = entr.getNodePath();
                // TODO: TOBEFIXED Text.isDescendant that returns false if np==root-path
                if (np.equals(nodePath) || "/".equals(np) || Text.isDescendant(np, nodePath)) {
                    if (entr.isAllow()) {
                        allows |= PrivilegeRegistry.diff(entr.getPrivilegeBits(), denies);
                    } else {
                        denies |= PrivilegeRegistry.diff(entr.getPrivilegeBits(), allows);
                    }
                }
            }
            return allows;
        }
    }
}