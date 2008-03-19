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
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.core.security.authorization.AccessControlEditor;
import org.apache.jackrabbit.core.security.authorization.PolicyTemplate;
import org.apache.jackrabbit.core.security.authorization.AccessControlConstants;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.core.security.authorization.acl.ACLEditor;
import org.apache.jackrabbit.core.security.jsr283.security.AccessControlException;
import org.apache.jackrabbit.core.security.jsr283.security.Privilege;
import org.apache.jackrabbit.core.security.principal.PrincipalImpl;
import org.apache.jackrabbit.core.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ItemNotFoundException;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

/**
 * <code>CombinedEditor</code>...
 */
class CombinedEditor extends ACLEditor {

    // TODO: must make sure, that store paths/globs do not contain remapped prefixes from the session

    private static Logger log = LoggerFactory.getLogger(CombinedEditor.class);

    private final SessionImpl session;
    private final NamePathResolver systemResolver;
    private final Path acRootPath;

    CombinedEditor(SessionImpl session, NamePathResolver systemResolver,
                   Path acRootPath) throws RepositoryException {
        super(session);
        this.session = session;
        this.systemResolver = systemResolver;
        this.acRootPath = acRootPath;
    }

    PolicyTemplateImpl editPolicyTemplate(Principal principal) throws RepositoryException {
        if (!session.getPrincipalManager().hasPrincipal(principal.getName())) {
            throw new AccessControlException("Unknown principal.");
        }
        NodeId nid = getAcId(principal);
        if (nid == null) {
            nid = createAcNode(principal).getNodeId();
        }

        PolicyTemplate pt = getPolicyTemplate(nid);
        if (pt instanceof PolicyTemplateImpl) {
            return (PolicyTemplateImpl) pt;
        } else {
            // should never get here.
            throw new AccessControlException();
        }
    }

    PolicyTemplateImpl getPolicyTemplate(Principal principal) throws RepositoryException {
        if (!session.getPrincipalManager().hasPrincipal(principal.getName())) {
            throw new AccessControlException("Unknown principal.");
        }

        NodeId nid = getAcId(principal);
        if (nid != null) {
            PolicyTemplate pt = getPolicyTemplate(nid);
            if (pt instanceof PolicyTemplateImpl) {
                return (PolicyTemplateImpl) pt;
            }
        }

        // no policy for the given principal
        log.debug("No combined policy template for Principal " + principal.getName());
        return null;
    }

    //------------------------------------------------< AccessControlEditor >---
    /**
     * @see AccessControlEditor#getPolicyTemplate(NodeId)
     */
    public PolicyTemplate getPolicyTemplate(NodeId id) throws AccessControlException, ItemNotFoundException, RepositoryException {
        checkProtectsNode(id);

        NodeImpl acNode = getAcNode(id);
        if (acNode != null) {
            if (isAccessControlled(acNode)) {
                return buildTemplate(acNode);
            } else {
                log.debug("No local policy defined for Node " + id);
                return null;
            }
        } else {
            // nodeID not below rep:accesscontrol -> delegate to ACLEditor
            return super.getPolicyTemplate(id);
        }
    }

    /**
     * @see AccessControlEditor#editPolicyTemplate(NodeId)
     */
    public PolicyTemplate editPolicyTemplate(NodeId id) throws AccessControlException, ItemNotFoundException, RepositoryException {
        checkProtectsNode(id);

        NodeImpl acNode = getAcNode(id);
        if (acNode != null) {
            return buildTemplate(acNode);
        } else {
            // nodeID not below rep:accesscontrol -> delegate to ACLEditor
            return super.editPolicyTemplate(id);
        }
    }

    /**
     * @see AccessControlEditor#setPolicyTemplate(NodeId, PolicyTemplate)
     */
    public void setPolicyTemplate(NodeId id, PolicyTemplate template) throws AccessControlException, ItemNotFoundException, RepositoryException {
        checkProtectsNode(id);

        if (template instanceof PolicyTemplateImpl) {
            PolicyTemplateImpl at = (PolicyTemplateImpl) template;
            if (!id.equals(at.getNodeId())) {
                throw new AccessControlException("Attempt to store PolicyTemplate to a wrong node.");
            }
            NodeImpl acNode = getAcNode(id);
            if (acNode == null) {
                throw new ItemNotFoundException("No such node " + id);
            }

            /*
             in order to assert that the parent (ac-controlled node) gets
             modified an existing ACL node is removed first and the recreated.
             this also asserts that all ACEs are cleared without having to
             access and removed the explicitely
            */
            NodeImpl aclNode;
            if (acNode.hasNode(N_POLICY)) {
                aclNode = acNode.getNode(N_POLICY);
                removeSecurityItem(aclNode);
            }
            /* now (re) create it */
            aclNode = addSecurityNode(acNode, N_POLICY, NT_REP_ACL);

            /* add all entries defined on the template */
            PolicyEntryImpl[] aces = (PolicyEntryImpl[]) template.getEntries();
            for (int i = 0; i < aces.length; i++) {
                PolicyEntryImpl ace = aces[i];

                // create the ACE node
                Name nodeName = getUniqueNodeName(aclNode, "entry");
                Name ntName = (ace.isAllow()) ? NT_REP_GRANT_ACE : NT_REP_DENY_ACE;
                NodeImpl aceNode = addSecurityNode(aclNode, nodeName, ntName);

                ValueFactory vf = session.getValueFactory();
                // write the rep:principalName property
                setSecurityProperty(aceNode, P_PRINCIPAL_NAME, vf.createValue(ace.getPrincipal().getName()));
                // ... and the rep:privileges property
                Privilege[] privs = ace.getPrivileges();
                Value[] vs = new Value[privs.length];
                for (int j = 0; j < privs.length; j++) {
                    vs[i] = vf.createValue(privs[j].getName());
                }
                setSecurityProperty(aceNode, P_PRIVILEGES, vs);
                setSecurityProperty(aceNode, P_NODE_PATH, vf.createValue(ace.getNodePath()));                
                setSecurityProperty(aceNode, P_GLOB, vf.createValue(ace.getGlob()));
            }
        } else {
            // try super class
            super.setPolicyTemplate(id, template);
        }
    }

    /**
     * @see AccessControlEditor#removePolicyTemplate(NodeId)
     */
    public PolicyTemplate removePolicyTemplate(NodeId id) throws AccessControlException, ItemNotFoundException, RepositoryException {
        checkProtectsNode(id);

        NodeImpl acNode = getAcNode(id);
        if (acNode != null) {
            if (isAccessControlled(acNode)) {
                // build the template in order to have a return value
                PolicyTemplate tmpl = buildTemplate(acNode);
                removeSecurityItem(acNode.getNode(N_POLICY));
                return tmpl;
            } else {
                log.debug("No policy present to remove at " + id);
                return null;
            }
        } else {
            // nodeID not below rep:accesscontrol -> delegate to ACLEditor
            return super.removePolicyTemplate(id);
        }
    }

    // TODO: check if get/add/remove entries are properly handled by super-class

    //------------------------------------------------------------< private >---
    /**
     *
     * @param nodeId
     * @return
     * @throws AccessControlException
     * @throws RepositoryException
     */
    private NodeImpl getAcNode(NodeId nodeId) throws AccessControlException, RepositoryException {
        NodeImpl n = session.getNodeById(nodeId);
        Path p = session.getHierarchyManager().getPath(n.getNodeId());
        if (p.isDescendantOf(acRootPath)) {
            return n;
        } else {
            // node outside of rep:accesscontrol tree -> not handled by this editor.
            return null;
        }
    }

    private NodeId getAcId(Principal principal) throws RepositoryException {
        Path acPath = session.getQPath(getPathToAcNode(principal));
        return session.getHierarchyManager().resolveNodePath(acPath);
    }

    private NodeImpl createAcNode(Principal principal) throws RepositoryException {
        String acPath = getPathToAcNode(principal);
        String[] segms = Text.explode(acPath, '/', false);
        NodeImpl node = (NodeImpl) session.getRootNode();
        for (int i = 0; i < segms.length; i++) {
            Name nName = session.getQName(segms[i]);
            if (node.hasNode(nName)) {
                node = node.getNode(nName);
                if (!node.isNodeType(NT_REP_ACCESS_CONTROL)) {
                    // should never get here.
                    throw new RepositoryException("Internal error: Unexpected nodetype " + node.getPrimaryNodeType().getName() + " below /rep:accessControl");
                }
            } else {
                node = addSecurityNode(node, nName, NT_REP_ACCESS_CONTROL);
            }
        }
        return node;
    }

    /**
     * Test if the Node identified by <code>id</code> is itself part of ACL
     * defining content. It this case setting or modifying an AC-policy is
     * obviously not possible.
     *
     * @param id
     * @throws AccessControlException If the given id identifies a Node that
     * represents a ACL or ACE item.
     * @throws RepositoryException
     */
    private void checkProtectsNode(NodeId id) throws RepositoryException {
        NodeImpl node = session.getNodeById(id);
        if (node.isNodeType(NT_REP_ACL) || node.isNodeType(NT_REP_ACE)) {
            throw new AccessControlException("Node " + id + " defines ACL or ACE.");
        }
    }

    private String getPathToAcNode(Principal principal) throws RepositoryException {
        StringBuffer princPath = new StringBuffer(session.getJCRPath(acRootPath));
        if (principal instanceof ItemBasedPrincipal) {
            princPath.append(((ItemBasedPrincipal) principal).getPath());
        } else {
            princPath.append("/");
            princPath.append(Text.escapeIllegalJcrChars(principal.getName()));
        }
        return princPath.toString();
    }

    /**
     *
     * @param node
     * @return
     * @throws RepositoryException
     */
    private boolean isAccessControlled(NodeImpl node) throws RepositoryException {
        return node.isNodeType(NT_REP_ACCESS_CONTROL) && node.hasNode(N_POLICY);
    }

    private PolicyTemplate buildTemplate(NodeImpl acNode) throws RepositoryException {
        Principal principal;
        String principalName = Text.unescapeIllegalJcrChars(acNode.getName());
        PrincipalManager pMgr = ((SessionImpl) acNode.getSession()).getPrincipalManager();
        if (pMgr.hasPrincipal(principalName)) {
            principal = pMgr.getPrincipal(principalName);
        } else {
            log.warn("Principal with name " + principalName + " unknown to PrincipalManager.");
            // TODO: rather throw?
            principal = new PrincipalImpl(principalName);
        }
        return new PolicyTemplateImpl(getEntries(acNode, principal), principal, acNode.getNodeId());
    }

    private List getEntries(NodeImpl acNode, Principal principal) throws RepositoryException {
        List entries = new ArrayList();
        if (acNode.isNodeType(NT_REP_ACCESS_CONTROL) && acNode.hasNode(N_POLICY)) {
            NodeImpl aclNode = acNode.getNode(N_POLICY);
            // loop over all entries in the aclNode for the princ-Principal
            // and compare if they apply to the Node with 'nodeId'
            for (NodeIterator aceNodes = aclNode.getNodes(); aceNodes.hasNext();) {
                NodeImpl aceNode = (NodeImpl) aceNodes.nextNode();
                PolicyEntryImpl ace = createFromNode(aceNode, principal);
                if (ace != null) {
                    entries.add(ace);
                }
            }
        }
        return entries;
    }

    private PolicyEntryImpl createFromNode(NodeImpl node, Principal principal) throws RepositoryException {
        if (!node.isNodeType(AccessControlConstants.NT_REP_ACE)) {
            log.warn("Unexpected nodetype. Was not rep:ACE.");
            return null;
        }

        boolean allow = node.isNodeType(NT_REP_GRANT_ACE);

        Value[] pValues = node.getProperty(P_PRIVILEGES).getValues();
        String[] pNames = new String[pValues.length];
        for (int i = 0; i < pValues.length; i++) {
            pNames[i] = pValues[i].getString();
        }
        int privileges = PrivilegeRegistry.getBits(pNames);

        String nodePath = node.getProperty(P_NODE_PATH).getString();
        String glob = node.getProperty(P_GLOB).getString();

        // TODO: mk sure principal and principal-name in node match

        return new PolicyEntryImpl(principal, privileges, allow, nodePath, glob);
    }
}