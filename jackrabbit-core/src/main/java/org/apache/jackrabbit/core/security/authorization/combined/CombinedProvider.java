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
import org.apache.jackrabbit.core.security.authorization.GlobPattern;
import org.apache.jackrabbit.core.security.authorization.acl.ACLEditor;
import org.apache.jackrabbit.core.security.jsr283.security.AccessControlEntry;
import org.apache.jackrabbit.core.security.principal.PrincipalImpl;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.util.Text;
import org.apache.commons.collections.map.ListOrderedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.observation.Event;
import javax.jcr.observation.ObservationManager;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventIterator;
import java.security.Principal;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * <code>CombinedProvider</code>...
 */
public class CombinedProvider extends AbstractAccessControlProvider implements AccessControlConstants {

    private static Logger log = LoggerFactory.getLogger(CombinedProvider.class);

    // TODO: add means to show effective-policy to a user.
    // TODO: TOBEFIXED add means to create user-based ACLs (currently editor is not exposed in the API)
    // TODO: TOBEFIXED proper evaluation of permissions respecting resource-based ACLs.
    // TODO: TOBEFIXED assert proper evaluation order of group/non-group principal-ACLs

    private SessionImpl session;
    private ObservationManager obsMgr;

    private CombinedEditor editor;
    private NodeImpl acRoot;

    protected CombinedProvider() {
        super("Combined AC policy", "Policy evaluating user-based and resource-based ACLs.");
    }
    //----------------------------------------------< AccessControlProvider >---
    /**
     * @see AccessControlProvider#init(javax.jcr.Session, java.util.Map)
     */
    public void init(Session systemSession, Map options) throws RepositoryException {
        if (initialized) {
            throw new IllegalStateException("already initialized");
        }
        if (!(systemSession instanceof SessionImpl)) {
            throw new RepositoryException("SessionImpl (system session) expected.");
        }
        session = (SessionImpl) systemSession;
        obsMgr = session.getWorkspace().getObservationManager();

        String rootPath = acRoot.getPath();
        editor = new CombinedEditor(session, session.getNamePathResolver(),
                session.getQPath(rootPath));
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
            PolicyTemplateImpl pt = editor.editPolicyTemplate(administrators);
            pt.setEntry(new PolicyEntryImpl(administrators, PrivilegeRegistry.ALL, true, rootPath, glob));
            editor.setPolicyTemplate(pt.getNodeId(), pt);

            Principal everyone = pMgr.getEveryone();
            // TODO: to be improved. how to define where everyone has read-access
            log.info("... Privilege.READ for everyone.");
            pt = editor.editPolicyTemplate(everyone);
            pt.setEntry(new PolicyEntryImpl(everyone, PrivilegeRegistry.READ, true, rootPath, glob));
            editor.setPolicyTemplate(pt.getNodeId(), pt);

            session.save();
            log.info("... done.");

        } catch (RepositoryException e) {
            log.error("Failed to set-up minimal access control for root node of workspace " + session.getWorkspace().getName());
            session.getRootNode().refresh(false);
            throw e;
        }


        NodeImpl root = (NodeImpl) session.getRootNode();
        if (root.hasNode(N_ACCESSCONTROL)) {
            // TODO: make sure its a node with the correct nodetype
            acRoot = root.getNode(N_ACCESSCONTROL);
            if (!acRoot.isNodeType(NT_REP_ACCESS_CONTROL)) {
                throw new RepositoryException("Error while initializing Access Control Provider: Found ac-root to be wrong node type " + acRoot.getPrimaryNodeType().getName());
            }
        } else {
            acRoot = root.addNode(N_ACCESSCONTROL, NT_REP_ACCESS_CONTROL, null);
        }
        initialized = true;
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

        log.debug("Unable to createFromNode " + CombinedEditor.class.getName() + ".");
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
            // TODO: include the resource-based ACLs!
            return new CompiledPermissionImpl(principals);
        }
    }

    //----------------------------------------< private | package protected >---
    /**
     * Test if the given path points to a Node (or an existing or non existing
     * direct decendant of an existing Node) that stores AC-information
     *
     * @param path
     * @return
     * @throws RepositoryException
     */
    private boolean isAccessControlItem(Path path) throws ItemNotFoundException, RepositoryException {
        NodeImpl node;
        String absPath = session.getJCRPath(path);
        if (session.nodeExists(absPath)) {
            node = (NodeImpl) session.getNode(absPath);
        } else {
            // path points to existing prop or non-existing item (node or prop).
            String parentPath = Text.getRelativeParent(absPath, 1);
            if (session.nodeExists(parentPath)) {
                node = (NodeImpl) session.getNode(parentPath);
            } else {
                throw new ItemNotFoundException("No item exists at " + absPath + " nor at its direct ancestor.");
            }
        }
        return node.isNodeType(ACLEditor.NT_REP_ACL) || node.isNodeType(ACLEditor.NT_REP_ACE);
    }

    /**
     *
     * @param principals
     * @return
     * @throws RepositoryException
     */
    private ACLImpl getACL(Set principals) throws RepositoryException {
        // acNodes must be ordered in the same order as the principals
        // in order to obtain proper acl-evalution in case the given
        // principal-set is ordered.
        Map princToACEs = new ListOrderedMap();
        Set acPaths = new HashSet();
        // build acl-hierarchy assuming that principal-order determines the
        // acl-inheritance.
        for (Iterator it = principals.iterator(); it.hasNext();) {
            Principal princ = (Principal) it.next();
            PolicyTemplateImpl at = editor.getPolicyTemplate(princ);
            if (at == null) {
                log.debug("No matching ACL node found for principal " + princ.getName() + " -> principal ignored.");
            } else {
                // retrieve the ACEs from the node
                PolicyEntryImpl[] aces = (PolicyEntryImpl[]) at.getEntries();
                princToACEs.put(princ, aces);

                Path p = session.getHierarchyManager().getPath(at.getNodeId());
                acPaths.add(session.getJCRPath(p));
            }
        }
        return new ACLImpl(princToACEs, acPaths);
    }

    //-----------------------------------------------------< CompiledPolicy >---
    /**
     *
     */
    private class CompiledPermissionImpl extends AbstractCompiledPermissions
            implements EventListener {

        private final Set principals;
        private ACLImpl acl;

        /**
         * @param principals
         * @throws RepositoryException
         */
        private CompiledPermissionImpl(Set principals) throws RepositoryException {

            this.principals = principals;
            acl = getACL(principals);

            // TODO: describe
            // TODO: rather on CombinedProvider? -> but must keep references to the CompiledPermission then....?
            int events = Event.PROPERTY_CHANGED | Event.PROPERTY_ADDED |
                    Event.PROPERTY_REMOVED | Event.NODE_ADDED | Event.NODE_REMOVED;
            String[] ntNames = new String[] {
                    session.getJCRName(NT_REP_ACE)
            };
            obsMgr.addEventListener(this, events, acRoot.getPath(), true, null, ntNames, true);
        }

        //------------------------------------< AbstractCompiledPermissions >---
        /**
         * @see AbstractCompiledPermissions#buildResult(Path)
         */
        protected Result buildResult(Path absPath) throws RepositoryException {
            if (!absPath.isAbsolute()) {
                throw new RepositoryException("Absolute path expected.");
            }

            String jcrPath = session.getJCRPath(absPath);
            boolean isAclItem = isAccessControlItem(absPath);
            
            int permissions;
            if (session.itemExists(jcrPath)) {
                permissions = acl.getPermissions(session.getItem(jcrPath), isAclItem);
            } else {
                Node parent = session.getNode(Text.getRelativeParent(jcrPath, 1));
                String name = session.getJCRName(absPath.getNameElement().getName());
                permissions = acl.getPermissions(parent, name, isAclItem);
            }
            /* privileges can only be determined for existing nodes.
               not for properties and neither for non-existing nodes. */
            int privileges = (session.nodeExists(jcrPath)) ? acl.getPrivileges(jcrPath) : PrivilegeRegistry.NO_PRIVILEGE;
            return new Result(permissions, privileges);
        }

        //--------------------------------------------< CompiledPermissions >---
        /**
         * @see CompiledPermissions#close()
         */
        public void close() {
            try {
                obsMgr.removeEventListener(this);
            } catch (RepositoryException e) {
                log.error("Internal error: ", e.getMessage());
            }
            super.close();
        }

        //--------------------------------------------------< EventListener >---
        /**
         * @see EventListener#onEvent(EventIterator)
         */
        public void onEvent(EventIterator events) {
            Set acPaths = acl.getAcPaths();
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
                    // reload the acl
                    acl = getACL(principals);
                    clearCache();
                }
            } catch (RepositoryException e) {
                // should never get here
                log.warn("Internal error: ", e.getMessage());
            }
        }
    }
}