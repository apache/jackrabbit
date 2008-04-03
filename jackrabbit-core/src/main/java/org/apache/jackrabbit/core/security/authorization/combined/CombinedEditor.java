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

import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.authorization.AccessControlConstants;
import org.apache.jackrabbit.core.security.authorization.AccessControlEditor;
import org.apache.jackrabbit.core.security.authorization.PolicyTemplate;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.core.security.authorization.PolicyEntry;
import org.apache.jackrabbit.core.security.authorization.acl.ACLEditor;
import org.apache.jackrabbit.core.security.jsr283.security.AccessControlException;
import org.apache.jackrabbit.core.security.jsr283.security.Privilege;
import org.apache.jackrabbit.core.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.core.security.principal.PrincipalImpl;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NodeIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.PathNotFoundException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

/**
 * <code>CombinedEditor</code>...
 */
class CombinedEditor extends ACLEditor {

    private static Logger log = LoggerFactory.getLogger(CombinedEditor.class);

    private final NamePathResolver systemResolver;
    private final String acRootPath;

    CombinedEditor(SessionImpl session, NamePathResolver systemResolver,
                   Path acRootPath) throws RepositoryException {
        super(session);
        this.systemResolver = systemResolver;
        this.acRootPath = session.getJCRPath(acRootPath);
    }

    PolicyTemplate getPolicyTemplate(Principal principal) throws RepositoryException {
        if (!session.getPrincipalManager().hasPrincipal(principal.getName())) {
            throw new AccessControlException("Unknown principal.");
        }

        String nPath = getPathToAcNode(principal);
        if (session.nodeExists(nPath)) {
            return getPolicyTemplate(nPath);
        } else {
            // no policy for the given principal
            log.debug("No combined policy template for Principal " + principal.getName());
            return null;
        }
    }

    //------------------------------------------------< AccessControlEditor >---
    /**
     * @see AccessControlEditor#getPolicyTemplate(String)
     */
    public PolicyTemplate getPolicyTemplate(String nodePath) throws AccessControlException, PathNotFoundException, RepositoryException {
        checkProtectsNode(nodePath);

        NodeImpl acNode = getAcNode(nodePath);
        if (acNode != null) {
            return createTemplate(acNode);
        } else {
            // nodeID not below rep:accesscontrol -> delegate to ACLEditor
            return super.getPolicyTemplate(nodePath);
        }
    }

    /**
     * @see AccessControlEditor#editPolicyTemplate(String)
     */
    public PolicyTemplate editPolicyTemplate(String nodePath) throws AccessControlException, PathNotFoundException, RepositoryException {
        checkProtectsNode(nodePath);

        if (Text.isDescendant(acRootPath, nodePath)) {
            NodeImpl acNode = getAcNode(nodePath);
            if (acNode == null) {
                // check validity and create the ac node
                getPrincipal(nodePath);
                acNode = createAcNode(nodePath);
            }
            return createTemplate(acNode);
        } else {
            // nodeID not below rep:accesscontrol -> delegate to ACLEditor
            return super.editPolicyTemplate(nodePath);
        }
    }

    /**
     * @see AccessControlEditor#editPolicyTemplate(Principal)
     */
    public PolicyTemplate editPolicyTemplate(Principal principal) throws RepositoryException {
        if (!session.getPrincipalManager().hasPrincipal(principal.getName())) {
            throw new AccessControlException("Unknown principal.");
        }
        String nPath = getPathToAcNode(principal);
        if (!session.nodeExists(nPath)) {
            createAcNode(nPath);
        }
        return getPolicyTemplate(nPath);
    }

    /**
     * @see AccessControlEditor#setPolicyTemplate(String,PolicyTemplate)
     */
    public void setPolicyTemplate(String nodePath, PolicyTemplate template) throws AccessControlException, PathNotFoundException, RepositoryException {
        checkProtectsNode(nodePath);

        if (template instanceof PolicyTemplateImpl) {
            PolicyTemplateImpl at = (PolicyTemplateImpl) template;
            if (!nodePath.equals(at.getPath())) {
                throw new AccessControlException("Attempt to store PolicyTemplate to a wrong node.");
            }
            NodeImpl acNode = getAcNode(nodePath);
            if (acNode == null) {
                throw new PathNotFoundException("No such node " + nodePath);
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
            PolicyEntry[] aces = (PolicyEntry[]) template.getEntries();
            for (int i = 0; i < aces.length; i++) {
                PolicyEntryImpl ace = (PolicyEntryImpl) aces[i];

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
                    vs[j] = vf.createValue(privs[j].getName());
                }
                setSecurityProperty(aceNode, P_PRIVILEGES, vs);

                // remove local namespace remapping from the node path before
                // storing the path value.
                String pathValue = systemResolver.getJCRPath(session.getQPath(ace.getNodePath()));
                setSecurityProperty(aceNode, P_NODE_PATH, vf.createValue(pathValue, PropertyType.PATH));

                // TODO: TOBEFIXED respect namespace sensitive parts of the glob
                setSecurityProperty(aceNode, P_GLOB, vf.createValue(ace.getGlob()));
            }
        } else {
            // try super class
            super.setPolicyTemplate(nodePath, template);
        }
    }

    /**
     * @see AccessControlEditor#removePolicyTemplate(String)
     * @param nodePath
     */
    public PolicyTemplate removePolicyTemplate(String nodePath) throws AccessControlException, PathNotFoundException, RepositoryException {
        checkProtectsNode(nodePath);

        NodeImpl acNode = getAcNode(nodePath);
        if (acNode != null) {
            if (isAccessControlled(acNode)) {
                // build the template in order to have a return value
                PolicyTemplate tmpl = createTemplate(acNode);
                removeSecurityItem(acNode.getNode(N_POLICY));
                return tmpl;
            } else {
                log.debug("No policy present to remove at " + nodePath);
                return null;
            }
        } else {
            // nodeID not below rep:accesscontrol -> delegate to ACLEditor
            return super.removePolicyTemplate(nodePath);
        }
    }

    //------------------------------------------------------------< private >---
    /**
     *
     * @param nodePath
     * @return
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    private NodeImpl getAcNode(String nodePath) throws PathNotFoundException, RepositoryException {
        if (Text.isDescendant(acRootPath, nodePath)) {
            return (NodeImpl) session.getNode(nodePath);
        } else {
            // node outside of rep:accesscontrol tree -> not handled by this editor.
            return null;
        }
    }

    private NodeImpl createAcNode(String acPath) throws RepositoryException {
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
     * @param nodePath
     * @throws AccessControlException If the given id identifies a Node that
     * represents a ACL or ACE item.
     * @throws RepositoryException
     */
    private void checkProtectsNode(String nodePath) throws RepositoryException {
        if (session.nodeExists(nodePath)) {
            NodeImpl n = (NodeImpl) session.getNode(nodePath);
            if (n.isNodeType(NT_REP_ACL) || n.isNodeType(NT_REP_ACE)) {
                throw new AccessControlException("Node " + nodePath + " defines ACL or ACE.");
            }
        }
    }

    /**
     *
     * @param principal
     * @return
     * @throws RepositoryException
     */
    private String getPathToAcNode(Principal principal) throws RepositoryException {
        StringBuffer princPath = new StringBuffer(acRootPath);
        if (principal instanceof ItemBasedPrincipal) {
            princPath.append(((ItemBasedPrincipal) principal).getPath());
        } else {
            princPath.append("/");
            princPath.append(Text.escapeIllegalJcrChars(principal.getName()));
        }
        return princPath.toString();
    }

    private Principal getPrincipal(String pathToACNode) throws RepositoryException {
        String name = Text.unescapeIllegalJcrChars(Text.getName(pathToACNode));
        PrincipalManager pMgr = session.getPrincipalManager();
        if (!pMgr.hasPrincipal(name)) {
            throw new AccessControlException("Unknown principal.");
        }
        return pMgr.getPrincipal(name);
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

    /**
     *
     * @param acNode
     * @return
     * @throws RepositoryException
     */
    private PolicyTemplate createTemplate(NodeImpl acNode) throws RepositoryException {
        if (!acNode.isNodeType(NT_REP_ACCESS_CONTROL)) {
            throw new RepositoryException("Expected node of type rep:AccessControl.");
        }

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

        // build the list of policy entries;
        List entries = new ArrayList();
        if (acNode.hasNode(N_POLICY)) {
            NodeImpl aclNode = acNode.getNode(N_POLICY);
            // loop over all entries in the aclNode for the princ-Principal
            // and compare if they apply to the Node with 'nodeId'
            for (NodeIterator aceNodes = aclNode.getNodes(); aceNodes.hasNext();) {
                NodeImpl aceNode = (NodeImpl) aceNodes.nextNode();
                PolicyEntryImpl ace = createEntry(aceNode, principal);
                if (ace != null) {
                    entries.add(ace);
                }
            }
        }
        return new PolicyTemplateImpl(entries, principal, acNode.getPath());
    }

    /**
     *
     * @param node
     * @param principal
     * @return
     * @throws RepositoryException
     */
    private PolicyEntryImpl createEntry(NodeImpl node, Principal principal) throws RepositoryException {
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

        String pV = node.getProperty(P_NODE_PATH).getString();
        String nodePath = session.getJCRPath(systemResolver.getQPath(pV));

        // TODO: make sure local namespace remappings are respected.
        String glob = node.getProperty(P_GLOB).getString();

        return new PolicyEntryImpl(principal, privileges, allow, nodePath, glob);
    }
}