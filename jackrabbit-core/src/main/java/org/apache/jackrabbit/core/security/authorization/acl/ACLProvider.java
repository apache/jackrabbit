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

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.PropertyImpl;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.core.security.authorization.AbstractAccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.AbstractCompiledPermissions;
import org.apache.jackrabbit.core.security.authorization.AccessControlConstants;
import org.apache.jackrabbit.core.security.authorization.AccessControlEditor;
import org.apache.jackrabbit.core.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.CompiledPermissions;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.security.authorization.PolicyEntry;
import org.apache.jackrabbit.core.security.authorization.PolicyTemplate;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.core.security.jsr283.security.AccessControlEntry;
import org.apache.jackrabbit.core.security.jsr283.security.AccessControlPolicy;
import org.apache.jackrabbit.core.security.jsr283.security.Privilege;
import org.apache.jackrabbit.core.security.principal.PrincipalImpl;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.NodeIterator;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Arrays;

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
 * @see AccessControlProvider for additional information.
 */
public class ACLProvider extends AbstractAccessControlProvider implements AccessControlConstants {

    /**
     * the default logger
     */
    private static final Logger log = LoggerFactory.getLogger(ACLProvider.class);

    private AccessControlEditor systemEditor;

    /**
     * The node id of the root node
     */
    private NodeId rootNodeId;

    private String policyNodeName;

    //----------------------------------------------< AccessControlProvider >---
    /**
     * @see AccessControlProvider#init(Session, Map)
     */
    public void init(Session systemSession, Map options) throws RepositoryException {
        super.init(systemSession, options);

        // make sure the workspace of the given systemSession has a
        // minimal protection on the root node.
        NodeImpl root = (NodeImpl) session.getRootNode();
        rootNodeId = root.getNodeId();
        policyNodeName = resolver.getJCRName(N_POLICY);
        systemEditor = new ACLEditor(systemSession);

        if (!isAccessControlled(root)) {
            initRootACL(session, systemEditor);
        }
    }

    /**
     * @see AccessControlProvider#getPolicy(NodeId)
     */
    public AccessControlPolicy getPolicy(NodeId nodeId) throws ItemNotFoundException, RepositoryException {
        checkInitialized();
        return getACL(nodeId);
    }

    /**
     * @see AccessControlProvider#getAccessControlEntries(NodeId)
     */
    public AccessControlEntry[] getAccessControlEntries(NodeId nodeId) throws RepositoryException {
        checkInitialized();
        ACLImpl acl = getACL(nodeId);

        // TODO: check again what the expected return value would be.
        // TODO: check again if correct. call probably expensive.
        Map allowed = new HashMap();
        Map denied = new HashMap();
        for (Iterator it = acl.getEntries(); it.hasNext();) {
            ACEImpl ace = (ACEImpl) it.next();
            Principal pc = ace.getPrincipal();

            int pv = ace.getPrivilegeBits();

            int allowPv = (allowed.containsKey(pc)) ? ((Integer) allowed.get(pc)).intValue() : 0;
            int denyPv = (denied.containsKey(pc)) ? ((Integer) denied.get(pc)).intValue() : 0;

            // shortcut:
            if (allowPv == PrivilegeRegistry.ALL) {
                continue;
            }

            // if the ace is a granting ACE -> make sure the permissions
            // it grants are not denied by another ACE
            if (ace.isAllow()) {
                // determined those allow-priv from the current ace, that have
                // not been denied by an ace ealier in the evaluation.
                allowPv |= PrivilegeRegistry.diff(pv, denyPv);
                allowed.put(pc, new Integer(allowPv));
            } else {
                // determined those deny-priv from the current ace, that have
                // not been granted by an ace ealier in the evaluation.
                denyPv |= PrivilegeRegistry.diff(pv, allowPv);
                denied.put(pc, new Integer(denyPv));
            }
        }

        Set s = new HashSet();
        for (Iterator it = allowed.keySet().iterator(); it.hasNext();) {
            Principal p = (Principal) it.next();
            s.add(new ACEImpl(p, ((Integer) allowed.get(p)).intValue(), true));
        }
        return (AccessControlEntry[]) s.toArray(new AccessControlEntry[s.size()]);
    }

    /**
     * @see AccessControlProvider#getEditor(Session)
     */
    public AccessControlEditor getEditor(Session session) {
        checkInitialized();
        try {
            return new ACLEditor(session);
        } catch (RepositoryException e) {
            log.debug("Unable to create AccessControlEditor.", e.getMessage());
            return null;
        }
    }

    /**
     * @see AccessControlProvider#compilePermissions(Set)
     */
    public CompiledPermissions compilePermissions(Set principals) throws ItemNotFoundException, RepositoryException {
        checkInitialized();
        if (isAdminOrSystem(principals)) {
            return getAdminPermissions();
        } else {
            return new AclPermissions(principals);
        }
    }

    //------------------------------------------------------------< private >---
    /**
     * Build the ACL that is effective on the Node identified by
     * <code>nodeId</code>. In contrast to {@link #getACL(NodeId, Set)}
     * the returned ACL contains all entries that apply to that node.
     *
     * @param nodeId
     * @return
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    private ACLImpl getACL(NodeId nodeId) throws ItemNotFoundException, RepositoryException {
        return getACL(nodeId, Collections.EMPTY_SET);
    }

    /**
     * Build the ACL that is effective on the Node identified by
     * <code>nodeId</code>, but only retrieve those entries that apply to
     * any of the principals whose name is present in the given
     * <code>principalNameFilter</code>.
     *
     * @param nodeId
     * @param principalNameFilter
     * @return
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    private ACLImpl getACL(NodeId nodeId, Set principalNameFilter) throws ItemNotFoundException, RepositoryException {
        // -> build the acl for the Node identified by 'id'
        NodeImpl node = session.getNodeById(nodeId);
        ACLImpl acl;
        // check for special ACL building item
        if (protectsNode(node)) {
            NodeImpl parentNode;
            if (node.isNodeType(NT_REP_ACL)) {
                parentNode = (NodeImpl) node.getParent();
            } else {
                parentNode = (NodeImpl) node.getParent().getParent();
            }
            ACLImpl baseACL = buildAcl(parentNode, principalNameFilter);
            acl = new ACLImpl(nodeId, baseACL, true);
        } else {
            // build Acl for non-protection node.
            acl = buildAcl(node, principalNameFilter);
        }
        return acl;
    }

    /**
     * Constructs the ACLImpl for a regular node, i.e. a node that does not
     * store itself ACL-related information. The ACL to be returned combines both
     * the base-ACL containing the inherited access control information
     * and the access control information provided with the given node itself.
     *
     * @param node the Node to build the ACL for, which must NOT be part of the
     * structure defined by mix:AccessControllable.
     * @param principalNameFilter
     * @return acl or <code>DefaultACL</code> if neither the node nor any of it's
     * parents is access controlled.
     * @throws RepositoryException
     */
    private ACLImpl buildAcl(NodeImpl node, Set principalNameFilter) throws RepositoryException {
        // preconditions:
        // - node is not null
        // - node is never an ACL building item
        NodeId id = (NodeId) node.getId();
        // retrieve the base-ACL (i.e. the ACL that belongs to parentNode)
        // for this find nearest access controlled parent.
        ACLImpl baseACL = null;
        NodeImpl parentNode = id.equals(rootNodeId) ? null : (NodeImpl) node.getParent();
        while (parentNode != null && baseACL == null) {
            if (isAccessControlled(parentNode)) {
                baseACL = buildAcl(parentNode, principalNameFilter);
            } else {
                parentNode = (rootNodeId.equals(parentNode.getId())) ? null
                        : (NodeImpl) parentNode.getParent();
            }
        }
        // the build the effective ACL from the specified Node and the base ACL
        ACLImpl acl;
        if (isAccessControlled(node)) {
            // build acl from access controlled node
            NodeImpl aclNode = node.getNode(N_POLICY);
            PolicyTemplate tmpl = new ACLTemplate(aclNode, principalNameFilter);
            List localEntries = Arrays.asList(tmpl.getEntries());

            acl = new ACLImpl(aclNode.getNodeId(), localEntries, baseACL, false);
        } else if (baseACL != null) {
            // build acl for a non-access controlled item that has a base acl
            acl = new ACLImpl(id, baseACL, false);
        } else {
            // no access control information can be retrieved for the specified
            // node, since neither the node nor any of its parents is access
            // controlled -> build a default policy.
            log.warn("No access controlled node present in item hierarchy starting from " + id);
            acl = new DefaultACL(id);
        }
        return acl;
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
     * @throws RepositoryException
     */
    private static void initRootACL(JackrabbitSession session, AccessControlEditor editor) throws RepositoryException {
        try {
            log.info("Install initial ACL:...");
            String rootPath = session.getRootNode().getPath();
            PolicyTemplate tmpl = editor.editPolicyTemplate(rootPath);
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
            PolicyEntry entr = new ACEImpl(administrators, PrivilegeRegistry.ALL, true);
            tmpl.setEntry(entr);

            Principal everyone = pMgr.getEveryone();
            // TODO: to be improved. how to define where everyone has read-access
            log.info("... Privilege.READ for everyone.");
            entr = new ACEImpl(everyone, PrivilegeRegistry.READ, true);
            tmpl.setEntry(entr);

            editor.setPolicyTemplate(rootPath, tmpl);
            session.save();
            log.info("... done.");

        } catch (RepositoryException e) {
            log.error("Failed to set-up minimal access control for root node of workspace " + session.getWorkspace().getName());
            session.getRootNode().refresh(false);
            throw e;
        }
    }

    /**
     * Test if the given node is access controlled. The node is access
     * controlled if it is of nodetype
     * {@link AccessControlConstants#NT_REP_ACCESS_CONTROLLABLE "rep:AccessControllable"}
     * and if it has a child node named
     * {@link AccessControlConstants#N_POLICY "rep:ACL"}.
     *
     * @param node
     * @return <code>true</code> if the node is access controlled;
     *         <code>false</code> otherwise.
     * @throws RepositoryException
     */
    static boolean isAccessControlled(NodeImpl node) throws RepositoryException {
        return node.isNodeType(NT_REP_ACCESS_CONTROLLABLE) && node.hasNode(N_POLICY);
    }

    /**
     * Test if the given node is itself a rep:ACL or a rep:ACE node.
     *
     * @param node
     * @return
     * @throws RepositoryException
     */
    static boolean protectsNode(NodeImpl node) throws RepositoryException {
        return node.isNodeType(NT_REP_ACL) || node.isNodeType(NT_REP_ACE);
    }

    //------------------------------------------------< CompiledPermissions >---
    /**
     *
     */
    private class AclPermissions extends AbstractCompiledPermissions implements EventListener {

        private final Set principalNames;
        private boolean readAllowed;

        private AclPermissions(Set principals) throws RepositoryException {
            principalNames = new HashSet(principals.size());
            for (Iterator it = principals.iterator(); it.hasNext();) {
                principalNames.add(((Principal) it.next()).getName());
            }

            /*
             Determine if there is any 'denyRead' entry (since the default
             is that everyone can READ everywhere -> makes evaluation for
             the most common check (can-read) easy.
            */
            readAllowed = readAllowedEveryWhere(principalNames);

            /*
             Make sure this AclPermission recalculates the permissions if
             any ACL concerning it is modified. interesting events are:
             - new ACE-entry for any of the principals (NODE_ADDED)
             - changing ACE-entry for any of the principals (PROPERTY_CHANGED)
               > new permissions granted/denied
               >
             - removed ACE-entry for any of the principals (NODE_REMOVED)
            */
            int events = Event.PROPERTY_CHANGED | Event.NODE_ADDED | Event.NODE_REMOVED;
            String[] ntNames = new String[] {
                    resolver.getJCRName(NT_REP_ACE),
                    resolver.getJCRName(NT_REP_ACL)
            };
            observationMgr.addEventListener(this, events, session.getRootNode().getPath(), true, null, ntNames, true);
        }

        /**
         * Search if there is any ACE that defines permissions for any of the
         * principals AND denies-READ.
         *
         * @param principalnames
         * @return true if read is allowed on all items.
         */
        private boolean readAllowedEveryWhere(Set principalnames) {
            try {
                QueryManager qm = session.getWorkspace().getQueryManager();
                StringBuffer stmt = new StringBuffer("/jcr:root");
                stmt.append("//element(*,");
                stmt.append(resolver.getJCRName(NT_REP_DENY_ACE));
                stmt.append(")[");

                // where the rep:principalName property exactly matches any of
                // the given principalsNames
                int i = 0;
                Iterator itr = principalnames.iterator();
                while (itr.hasNext()) {
                    stmt.append("@");
                    String pName = resolver.getJCRName(P_PRINCIPAL_NAME);
                    stmt.append(ISO9075.encode(pName));
                    stmt.append("='").append(itr.next().toString()).append("'");
                    if (++i < principalnames.size()) {
                        stmt.append(" or ");
                    }
                }
                // AND rep:privileges contains the READ privilege
                stmt.append(" and jcr:like(@");
                String pName = resolver.getJCRName(P_PRIVILEGES);
                stmt.append(ISO9075.encode(pName));
                stmt.append(",'%").append(Privilege.READ).append("%')");
                stmt.append("]");
                Query q = qm.createQuery(stmt.toString(), Query.XPATH);

                NodeIterator it = q.execute().getNodes();
                while (it.hasNext()) {
                    String path = it.nextNode().getPath();
                    // if there is a node that AND it is not below /accesscontrol
                    // we cannot use the short-cut within 'grants'
                    if (!Text.isDescendantOrEqual("/"+ N_ACCESSCONTROL, path)) {
                        return false;
                    }
                }
                // didn't find any matching ACE that denies READ for any
                // of the principals.
                return true;
            } catch (RepositoryException e) {
                log.error(e.toString());
                // unable to determine... -> no shortcut upon grants
                return false;
            }
        }

        //------------------------------------< AbstractCompiledPermissions >---
        /**
         * @see AbstractCompiledPermissions#buildResult(Path)
         */
        protected Result buildResult(Path absPath) throws RepositoryException {
            boolean existingNode = false;
            NodeId nid = null;
            String jcrPath = resolver.getJCRPath(absPath);

            if (session.nodeExists(jcrPath)) {
                nid = session.getHierarchyManager().resolveNodePath(absPath);
                existingNode = true;
            } else {
                // path points to existing prop or non-existing item (node or prop).
                // -> find the nearest persisted node
                Path parentPath = absPath.getAncestor(1);
                while (nid == null) {
                    nid = session.getHierarchyManager().resolveNodePath(parentPath);
                    if (parentPath.getDepth() == Path.ROOT_DEPTH) {
                        // root-node reached
                        break;
                    } else {
                        parentPath = parentPath.getAncestor(1);
                    }
                }
            }

            if (nid == null) {
                // should never get here
                throw new ItemNotFoundException("Item out of hierarchy.");
            }

            // build the ACL for the specified principals at path or at the
            // direct ancestor of path (that must be definition exist).
            ACLImpl acl = getACL(nid, principalNames);

            // privileges to expose
            int privileges = acl.getPrivileges();

            // calculate the permissions
            int permissions;
            if (existingNode || session.propertyExists(jcrPath)) {
                permissions = acl.getPermissions(session.getItem(jcrPath));
            } else {
                String name = resolver.getJCRName(absPath.getNameElement().getName());
                permissions = acl.getPermissions(name);
            }
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

        /**
         *
         * @param absPath
         * @param permissions
         * @return
         * @throws RepositoryException
         */
        public boolean grants(Path absPath, int permissions) throws RepositoryException {
            // common check
            if (permissions == Permission.READ && readAllowed &&
                    /* easy check if path doesn't point to AC-content */
                    resolver.getJCRPath(absPath).indexOf(policyNodeName) == -1) {
                return true;
            }
            return super.grants(absPath, permissions);
        }

        //--------------------------------------------------< EventListener >---
        /**
         * @see EventListener#onEvent(EventIterator)
         */
        public void onEvent(EventIterator events) {
            // only invalidate cache if any of the events affects the
            // nodes defining permissions for principals compiled here.
            boolean clearCache = false;
            while (events.hasNext() && !clearCache) {
                try {
                    Event ev = events.nextEvent();
                    String path = ev.getPath();
                    if (Text.isDescendantOrEqual("/"+ N_ACCESSCONTROL, path)) {
                        // access control change applies to the 'combined' acls
                        // -> ignore
                        continue;
                    }

                    switch (ev.getType()) {
                        case Event.NODE_ADDED:
                            // test if the new ACE-nodes affects the permission
                            // of any of the 'principals'.
                            NodeImpl n = (NodeImpl) session.getNode(path);
                            String pName = n.getProperty(P_PRINCIPAL_NAME).getString();
                            if (principalNames.contains(pName)) {
                                // new ACE entry for the principals -> clear cache
                                clearCache = true;
                                // if ace is a new DENY -> check if denies reading
                                if (readAllowed && n.isNodeType(NT_REP_DENY_ACE)) {
                                    Value[] vs = n.getProperty(P_PRIVILEGES).getValues();
                                    for (int i = 0; i < vs.length; i++) {
                                        if (Privilege.READ.equals(vs[i].getString())) {
                                            readAllowed = false;
                                        }
                                    }
                                }
                            }
                            break;
                        case Event.NODE_REMOVED:
                            // can't find out if the removed ACL/ACE node was
                            // relevant for the principals
                            clearCache = true;
                            break;
                        case Event.PROPERTY_CHANGED:
                            // test if the changed ACE_prop affects the permission
                            // of any of the 'principals' (most interesting are
                            // changed privileges.
                            PropertyImpl p = (PropertyImpl) session.getProperty(path);
                            if (P_PRIVILEGES.equals(p.getQName())) {
                                // test if principal-name sibling-prop matches
                                pName = ((NodeImpl) p.getParent()).getProperty(P_PRINCIPAL_NAME).toString();
                                clearCache = principalNames.contains(pName);
                            } else if (P_PRINCIPAL_NAME.equals(p.getQName())) {
                                // an ace change its principal-name. that should
                                // not happen. -> clear cache to be on the safe side.
                                clearCache = true;
                            }
                            break;
                        default:
                            // illegal event-type: should never occur. ignore
                    }
                } catch (RepositoryException e) {
                    // should not get here
                    log.warn("Internal error: ", e.getMessage());
                }
            }
            if (clearCache) {
                clearCache();
            }
        }
    }
}
