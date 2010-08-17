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
package org.apache.jackrabbit.core.security.authorization.acl;

import org.apache.commons.collections.map.LRUMap;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.core.ItemImpl;
import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.core.security.authorization.AccessControlListener;
import org.apache.jackrabbit.core.security.authorization.AbstractAccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.AbstractCompiledPermissions;
import org.apache.jackrabbit.core.security.authorization.AccessControlConstants;
import org.apache.jackrabbit.core.security.authorization.AccessControlEditor;
import org.apache.jackrabbit.core.security.authorization.AccessControlModifications;
import org.apache.jackrabbit.core.security.authorization.CompiledPermissions;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.core.security.authorization.UnmodifiableAccessControlList;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.Privilege;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The ACLProvider generates access control policies out of the items stored
 * in the workspace applying the following rules:
 * <ul>
 * <li>A <code>Node</code> is considered <i>access controlled</i> if an ACL has
 * been explicitely assigned to it by adding the mixin type
 * <code>rep:AccessControllable</code> and adding child node of type
 * <code>rep:acl</code> that forms the acl.</li>
 * <li>a Property is considered 'access controlled' if its parent Node is.</li>
 * <li>An ACL is never assigned to a <code>Property</code> item.</li>
 * <li>A <code>Node</code> that is not access controlled may inherit the ACL.
 * The ACL is inherited from the closest access controlled ancestor.</li>
 * <li>It may be possible that a given <code>Node</code> has no effective ACL, in
 * which case some a default policy is returned that grants READ privilege to
 * any principal and denies all other privileges.</li>
 * <li>an item is considered an <i>ACL item</i> if it is used to define an ACL.
 * ACL items inherit the ACL from node they defined the ACL for.</li>
 * </ul>
 *
 * @see org.apache.jackrabbit.core.security.authorization.AccessControlProvider for additional information.
 */
public class ACLProvider extends AbstractAccessControlProvider implements AccessControlConstants {

    /**
     * the default logger
     */
    private static final Logger log = LoggerFactory.getLogger(ACLProvider.class);

    /**
     * the system acl editor.
     */
    private ACLEditor systemEditor;

    /**
     * The node id of the root node
     */
    private NodeId rootNodeId;

    /**
     * Cache to ease the retrieval of ACEs defined for a given node. This cache
     * is used by the ACLPermissions created individually for each Session
     * instance.
     */
    private EntryCollector entryCollector;

    //----------------------------------------------< AccessControlProvider >---
    /**
     * @see org.apache.jackrabbit.core.security.authorization.AccessControlProvider#init(Session, Map)
     */
    @Override
    public void init(Session systemSession, Map configuration) throws RepositoryException {
        super.init(systemSession, configuration);

        // make sure the workspace of the given systemSession has a
        // minimal protection on the root node.
        NodeImpl root = (NodeImpl) session.getRootNode();
        rootNodeId = root.getNodeId();
        systemEditor = new ACLEditor(systemSession, this);

        // TODO: replace by configurable default policy (see JCR-2331)
        boolean initializedWithDefaults = !configuration.containsKey(PARAM_OMIT_DEFAULT_PERMISSIONS);
        if (initializedWithDefaults && !isAccessControlled(root)) {
            initRootACL(session, systemEditor);
        }

        entryCollector = createEntryCollector((SessionImpl) systemSession);
    }

    @Override
    public void close() {
        super.close();        
        entryCollector.close();
    }

    /**
     * @see org.apache.jackrabbit.core.security.authorization.AccessControlProvider#getEffectivePolicies(Path)
     * @param absPath absolute path
     */
    public AccessControlPolicy[] getEffectivePolicies(Path absPath) throws ItemNotFoundException, RepositoryException {
        checkInitialized();

        NodeImpl targetNode = (NodeImpl) session.getNode(session.getJCRPath(absPath));
        NodeImpl node = getNode(targetNode, isAcItem(targetNode));
        List<AccessControlList> acls = new ArrayList<AccessControlList>();

        // collect all ACLs effective at node
        collectAcls(node, acls);
        // if no effective ACLs are present -> add a default, empty acl.
        if (acls.isEmpty()) {
            // no access control information can be retrieved for the specified
            // node, since neither the node nor any of its parents is access
            // controlled.
            log.warn("No access controlled node present in item hierarchy starting from " + targetNode.getPath());
        }
        return acls.toArray(new AccessControlList[acls.size()]);
    }

    /**
     * @see org.apache.jackrabbit.core.security.authorization.AccessControlProvider#getEditor(Session)
     */
    public AccessControlEditor getEditor(Session session) {
        checkInitialized();
        return new ACLEditor(session, this);
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
            return new AclPermissions(principals);
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
            CompiledPermissions cp = new AclPermissions(principals, false);
            return cp.canRead(null, rootNodeId);
        }
    }

    //----------------------------------------------------------< protected >---
    /**
     * Create the <code>EntryCollector</code> instance that is used by this
     * provider to gather the effective ACEs for a given list of principals at a
     * given node during AC evaluation.
     *
     * @param systemSession The system session to create the entry collector for.
     * @return A new instance of <code>CachingEntryCollector</code>.
     * @throws RepositoryException If an error occurs.
     * @see #retrieveResultEntries(NodeImpl, EntryFilter)
     */
    protected EntryCollector createEntryCollector(SessionImpl systemSession) throws RepositoryException {
        return new CachingEntryCollector(systemSession, systemEditor, rootNodeId);
    }

    /**
     * Retrieve an iterator of <code>AccessControlEntry</code> to be evaluated
     * upon {@link AbstractCompiledPermissions#buildResult}.
     *
     * @param node Target node.
     * @param filter The entry filter used to collect the access control entries.
     * @return an iterator of <code>AccessControlEntry</code>.
     * @throws RepositoryException If an error occurs.
     */
    protected Iterator<AccessControlEntry> retrieveResultEntries(NodeImpl node, EntryFilter filter) throws RepositoryException {
        Iterator<AccessControlEntry> itr = entryCollector.collectEntries(node, filter).iterator();
        return itr;
    }

    //------------------------------------------------------------< private >---
    /**
     * Returns the given <code>targetNode</code> unless the node itself stores
     * access control information in which case it's nearest non-ac-parent is
     * searched and returned.
     *
     * @param targetNode The node for which AC information needs to be retrieved.
     * @param isAcItem true if the specified target node defines access control
     * content; false otherwise.
     * @return the given <code>targetNode</code> or the nearest non-ac-parent
     * in case the <code>targetNode</code> itself defines access control content.
     * @throws RepositoryException if an error occurs
     */
    private NodeImpl getNode(NodeImpl targetNode, boolean isAcItem) throws RepositoryException {
        NodeImpl node;
        if (isAcItem) {
            Name ntName = ((NodeTypeImpl) targetNode.getPrimaryNodeType()).getQName();
            if (ntName.equals(NT_REP_ACL)) {
                node = (NodeImpl) targetNode.getParent();
            } else {
                node = (NodeImpl) targetNode.getParent().getParent();
            }
        } else {
            node = targetNode;
        }
        return node;
    }

    /**
     * Recursively collects all ACLs that are effective on the specified node.
     *
     * @param node the Node to collect the ACLs for, which must NOT be part of the
     * structure defined by mix:AccessControllable.
     * @param acls List used to collect the effective acls.
     * @throws RepositoryException if an error occurs
     */
    private void collectAcls(NodeImpl node, List<AccessControlList> acls) throws RepositoryException {
        // if the given node is access-controlled, construct a new ACL and add
        // it to the list
        if (isAccessControlled(node)) {
            // retrieve the entries for the access controlled node
            acls.add(new UnmodifiableAccessControlList(entryCollector.getEntries(node), node.getPath(), Collections.<String, Integer>emptyMap()));
        }
        // then, recursively look for access controlled parents up the hierarchy.
        if (!rootNodeId.equals(node.getId())) {
            NodeImpl parentNode = (NodeImpl) node.getParent();
            collectAcls(parentNode, acls);
        }
    }

    /**
     * Set-up minimal permissions for the workspace:
     *
     * <ul>
     * <li>adminstrators principal -> all privileges</li>
     * <li>everybody -> read privilege</li>
     * </ul>
     *
     * @param session to the workspace to set-up inital ACL to
     * @param editor for the specified session.
     * @throws RepositoryException If an error occurs.
     */
    private static void initRootACL(SessionImpl session, AccessControlEditor editor) throws RepositoryException {
        try {
            log.debug("Install initial ACL:...");
            String rootPath = session.getRootNode().getPath();
            AccessControlPolicy[] acls = editor.editAccessControlPolicies(rootPath);
            if (acls.length > 0) {
                ACLTemplate acl = (ACLTemplate) acls[0];
                
                PrincipalManager pMgr = session.getPrincipalManager();
                AccessControlManager acMgr = session.getAccessControlManager();

                String pName = SecurityConstants.ADMINISTRATORS_NAME;
                if (pMgr.hasPrincipal(pName)) {
                    Principal administrators = pMgr.getPrincipal(pName);
                    log.debug("... Privilege.ALL for administrators.");
                    Privilege[] privs = new Privilege[]{acMgr.privilegeFromName(Privilege.JCR_ALL)};
                    acl.addAccessControlEntry(administrators, privs);
                } else {
                    log.info("Administrators principal group is missing -> omitting initialization of default permissions.");
                }

                Principal everyone = pMgr.getEveryone();
                log.debug("... Privilege.READ for everyone.");
                Privilege[] privs = new Privilege[]{acMgr.privilegeFromName(Privilege.JCR_READ)};
                acl.addAccessControlEntry(everyone, privs);

                editor.setPolicy(rootPath, acl);
                session.save();
            } else {
                log.info("No applicable ACL available for the root node -> skip initialization of the root node's ACL.");
            }
        } catch (RepositoryException e) {
            log.error("Failed to set-up minimal access control for root node of workspace " + session.getWorkspace().getName());
            session.getRootNode().refresh(false);
        }
    }

    /**
     * Test if the given node is access controlled. The node is access
     * controlled if it is of nodetype
     * {@link AccessControlConstants#NT_REP_ACCESS_CONTROLLABLE "rep:AccessControllable"}
     * and if it has a child node named
     * {@link AccessControlConstants#N_POLICY "rep:ACL"}.
     *
     * @param node the node to be tested
     * @return <code>true</code> if the node is access controlled and has a
     * rep:policy child; <code>false</code> otherwise.
     * @throws RepositoryException if an error occurs
     */
    static boolean isAccessControlled(NodeImpl node) throws RepositoryException {
        return node.hasNode(N_POLICY) && node.isNodeType(NT_REP_ACCESS_CONTROLLABLE);
    }

    //------------------------------------------------< CompiledPermissions >---
    /**
     *
     */
    private class AclPermissions extends AbstractCompiledPermissions implements AccessControlListener {

        private final List<String> principalNames;
        private final Map<NodeId, Boolean> readCache = new LRUMap(1000);
        private final Object monitor = new Object();

        private AclPermissions(Set<Principal> principals) throws RepositoryException {
            this(principals, true);
        }

        private AclPermissions(Set<Principal> principals, boolean listenToEvents) throws RepositoryException {
            principalNames = new ArrayList<String>(principals.size());
            for (Principal princ : principals) {
                principalNames.add(princ.getName());
            }

            if (listenToEvents) {
                /*
                 Make sure this AclPermission recalculates the permissions if
                 any ACL concerning it is modified.
                 */
                 entryCollector.addListener(this);
            }
        }

        private Result buildResult(NodeImpl node, boolean existingNode, boolean isAcItem, EntryFilter filter) throws RepositoryException {
            // retrieve all ACEs at path or at the direct ancestor of path that
            // apply for the principal names.
            Iterator<AccessControlEntry> entries = retrieveResultEntries(getNode(node, isAcItem), filter);

            /*
             Calculate privileges and permissions:
             Since the ACEs only define privileges on a node and do not allow
             to add additional restrictions, the permissions can be determined
             without taking the given target name or target item into account.
             */
            int allows = Permission.NONE;
            int denies = Permission.NONE;

            int allowPrivileges = PrivilegeRegistry.NO_PRIVILEGE;
            int denyPrivileges = PrivilegeRegistry.NO_PRIVILEGE;
            int parentAllows = PrivilegeRegistry.NO_PRIVILEGE;
            int parentDenies = PrivilegeRegistry.NO_PRIVILEGE;

            while (entries.hasNext()) {
                ACLTemplate.Entry ace = (ACLTemplate.Entry) entries.next();
                /*
                 Determine if the ACE is defined on the node at absPath (locally):
                 Except for READ-privileges the permissions must be determined
                 from privileges defined for the parent. Consequently aces
                 defined locally must be treated different than inherited entries.
                 */
                int entryBits = ace.getPrivilegeBits();
                boolean isLocal = existingNode && ace.isLocal(node.getNodeId());
                if (!isLocal) {
                    if (ace.isAllow()) {
                        parentAllows |= Permission.diff(entryBits, parentDenies);
                    } else {
                        parentDenies |= Permission.diff(entryBits, parentAllows);
                    }
                }
                if (ace.isAllow()) {
                    allowPrivileges |= Permission.diff(entryBits, denyPrivileges);
                    int permissions = PrivilegeRegistry.calculatePermissions(allowPrivileges, parentAllows, true, isAcItem);
                    allows |= Permission.diff(permissions, denies);
                } else {
                    denyPrivileges |= Permission.diff(entryBits, allowPrivileges);
                    int permissions = PrivilegeRegistry.calculatePermissions(denyPrivileges, parentDenies, false, isAcItem);
                    denies |= Permission.diff(permissions, allows);
                }
            }
            return new Result(allows, denies, allowPrivileges, denyPrivileges);
        }

        //------------------------------------< AbstractCompiledPermissions >---
        /**
         * @see AbstractCompiledPermissions#buildResult(Path)
         */
        @Override
        protected Result buildResult(Path absPath) throws RepositoryException {
            boolean existingNode = false;
            NodeImpl node;

            ItemManager itemMgr = session.getItemManager();
            try {
                ItemImpl item = itemMgr.getItem(absPath);
                if (item.isNode()) {
                    node = (NodeImpl) item;
                    existingNode = true;
                } else {
                    node = (NodeImpl) item.getParent();
                }
            } catch (RepositoryException e) {
                // path points to a non-persisted item.
                // -> find the nearest persisted node starting from the root.
                Path.Element[] elems = absPath.getElements();
                NodeImpl parent = (NodeImpl) session.getRootNode();
                for (int i = 1; i < elems.length - 1; i++) {
                    Name name = elems[i].getName();
                    int index = elems[i].getIndex();
                    if (!parent.hasNode(name, index)) {
                        // last persisted node reached
                        break;
                    }
                    parent = parent.getNode(name, index);

                }
                node = parent;
            }

            if (node == null) {
                // should never get here
                throw new ItemNotFoundException("Item out of hierarchy.");
            }

            boolean isAcItem = isAcItem(absPath);
            return buildResult(node, existingNode, isAcItem, new EntryFilterImpl(principalNames));
        }

        /**
         * @see AbstractCompiledPermissions#clearCache()
         */
        @Override
        protected void clearCache() {
            synchronized (monitor) {
                readCache.clear();
            }
            super.clearCache();
        }

        //--------------------------------------------< CompiledPermissions >---
        /**
         * @see CompiledPermissions#close()
         */
        @Override
        public void close() {
            entryCollector.removeListener(this);
            super.close();
        }

        /**
         * @see CompiledPermissions#canRead(Path, ItemId)
         */
        public boolean canRead(Path path, ItemId itemId) throws RepositoryException {
            ItemId id = (itemId == null) ? session.getHierarchyManager().resolvePath(path) : itemId;
            /* currently READ access cannot be denied to individual properties.
               if the parent node is readable the properties are as well.
               this simplifies the canRead test as well as the caching.
             */
            boolean existingNode = false;
            NodeId nodeId;
            if (id.denotesNode()) {
                nodeId = (NodeId) id;
                // since method may only be called for existing nodes the
                // flag be set to true if the id identifies a node.
                existingNode = true;
            } else {
                nodeId = ((PropertyId) id).getParentId();
            }

            boolean canRead;
            synchronized (monitor) {
                if (readCache.containsKey(nodeId)) {
                    canRead = readCache.get(nodeId);
                } else {
                    ItemManager itemMgr = session.getItemManager();
                    NodeImpl node = (NodeImpl) itemMgr.getItem(nodeId);
                    Result result = buildResult(node, existingNode, isAcItem(node), new EntryFilterImpl(principalNames));

                    canRead = result.grants(Permission.READ);
                    readCache.put(nodeId, canRead);
                }
            }
            return canRead;
        }

        //----------------------------------------< ACLModificationListener >---
        /**
         * @see org.apache.jackrabbit.core.security.authorization.AccessControlListener#acModified(AccessControlModifications)
         */
        public void acModified(AccessControlModifications modifications) {
            // ignore the details of the modifications and clear all caches.
            clearCache();
        }
    }
}
