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

import org.apache.jackrabbit.api.jsr283.security.AccessControlEntry;
import org.apache.jackrabbit.api.jsr283.security.AccessControlException;
import org.apache.jackrabbit.api.jsr283.security.Privilege;
import org.apache.jackrabbit.api.jsr283.security.AccessControlPolicy;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.principal.NoSuchPrincipalException;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.ProtectedItemModifier;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.authorization.AccessControlConstants;
import org.apache.jackrabbit.core.security.authorization.AccessControlEditor;
import org.apache.jackrabbit.core.security.authorization.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.core.security.principal.PrincipalImpl;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NameParser;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.PropertyType;
import java.security.Principal;

/**
 * <code>CombinedEditor</code>...
 */
public class ACLEditor extends ProtectedItemModifier implements AccessControlEditor, AccessControlConstants {

    private static Logger log = LoggerFactory.getLogger(ACLEditor.class);
    /**
     * Default name for ace nodes
     */
    private static final String DEFAULT_ACE_NAME = "ace";

    /**
     * the editing session
     */
    private final SessionImpl session;
    private final String acRootPath;

    ACLEditor(SessionImpl session, Path acRootPath) throws RepositoryException {
        super(Permission.MODIFY_AC);
        this.session = session;
        this.acRootPath = session.getJCRPath(acRootPath);
    }

    ACLTemplate getACL(Principal principal) throws RepositoryException {
        if (!session.getPrincipalManager().hasPrincipal(principal.getName())) {
            throw new AccessControlException("Unknown principal.");
        }
        String nPath = getPathToAcNode(principal);
        ACLTemplate acl = null;
        if (session.nodeExists(nPath)) {
            AccessControlPolicy[] plcs = getPolicies(nPath);
            if (plcs.length > 0) {
                acl = (ACLTemplate) plcs[0];
            }
        }
        if (acl == null) {
            // no policy for the given principal
            log.debug("No policy template for Principal " + principal.getName());
        }
        return acl;
    }

    //------------------------------------------------< AccessControlEditor >---
    /**
     * @see AccessControlEditor#getPolicies(String)
     */
    public AccessControlPolicy[] getPolicies(String nodePath) throws AccessControlException, PathNotFoundException, RepositoryException {
        checkProtectsNode(nodePath);

        NodeImpl acNode = getAcNode(nodePath);
        if (isAccessControlled(acNode)) {
            return new AccessControlPolicy[] {createTemplate(acNode)};
        } else {
            return new AccessControlPolicy[0];
        }
    }

    /**
     * @see AccessControlEditor#editAccessControlPolicies(String)
     */
    public AccessControlPolicy[] editAccessControlPolicies(String nodePath) throws AccessControlException, PathNotFoundException, RepositoryException {
        checkProtectsNode(nodePath);

        if (Text.isDescendant(acRootPath, nodePath)) {
            NodeImpl acNode = getAcNode(nodePath);
            if (acNode == null) {
                // check validity and create the ac node
                getPrincipal(nodePath);
                acNode = createAcNode(nodePath);
            }
            return new AccessControlPolicy[] {createTemplate(acNode)};
        } else {
            // nodePath not below rep:accesscontrol -> not editable
            return new AccessControlPolicy[0];
        }
    }

    /**
     * @see AccessControlEditor#editAccessControlPolicies(Principal)
     */
    public AccessControlPolicy[] editAccessControlPolicies(Principal principal) throws RepositoryException {
        if (!session.getPrincipalManager().hasPrincipal(principal.getName())) {
            throw new AccessControlException("Unknown principal.");
        }
        String nPath = getPathToAcNode(principal);
        NodeImpl acNode;
        if (!session.nodeExists(nPath)) {
            acNode = createAcNode(nPath);
        } else {
            acNode = (NodeImpl) session.getNode(nPath);
        }
        return new AccessControlPolicy[] {createTemplate(acNode)};
    }

    /**
     * @see AccessControlEditor#setPolicy(String,AccessControlPolicy)
     */
    public void setPolicy(String nodePath, AccessControlPolicy policy) throws AccessControlException, PathNotFoundException, RepositoryException {
        checkProtectsNode(nodePath);
        checkValidPolicy(nodePath, policy);

        ACLTemplate acl = (ACLTemplate) policy;
        NodeImpl acNode = getAcNode(nodePath);
        if (acNode == null) {
            throw new PathNotFoundException("No such node " + nodePath);
        }
        // write the entries to the node
        /*
         in order to assert that the parent (ac-controlled node) gets
         modified an existing ACL node is removed first and the recreated.
         this also asserts that all ACEs are cleared without having to
         access and removed the explicitely
        */
        NodeImpl aclNode;
        if (acNode.hasNode(N_POLICY)) {
            aclNode = acNode.getNode(N_POLICY);
            removeItem(aclNode);
        }
        /* now (re) create it */
        aclNode = addNode(acNode, N_POLICY, NT_REP_ACL);

        /* add all entries defined on the template */
        AccessControlEntry[] aces = acl.getAccessControlEntries();
        for (int i = 0; i < aces.length; i++) {
            JackrabbitAccessControlEntry ace = (JackrabbitAccessControlEntry) aces[i];

            // create the ACE node
            Name nodeName = getUniqueNodeName(aclNode, "entry");
            Name ntName = (ace.isAllow()) ? NT_REP_GRANT_ACE : NT_REP_DENY_ACE;
            NodeImpl aceNode = addNode(aclNode, nodeName, ntName);

            ValueFactory vf = session.getValueFactory();
            // write the rep:principalName property
            setProperty(aceNode, P_PRINCIPAL_NAME, vf.createValue(ace.getPrincipal().getName()));
            // ... and the rep:privileges property
            Privilege[] privs = ace.getPrivileges();
            Value[] vs = new Value[privs.length];
            for (int j = 0; j < privs.length; j++) {
                vs[j] = vf.createValue(privs[j].getName(), PropertyType.NAME);
            }
            setProperty(aceNode, P_PRIVILEGES, vs);

            // store the restrictions:
            String[] restrNames = ace.getRestrictionNames();
            for (int rnIndex = 0; rnIndex < restrNames.length; rnIndex++) {
                Name pName = session.getQName(restrNames[rnIndex]);
                Value value = ace.getRestriction(restrNames[rnIndex]);
                setProperty(aceNode, pName, value);
            }
        }
    }

    /**
     * @see AccessControlEditor#removePolicy(String,AccessControlPolicy)
     */
    public void removePolicy(String nodePath, AccessControlPolicy policy) throws AccessControlException, PathNotFoundException, RepositoryException {
        checkProtectsNode(nodePath);
        checkValidPolicy(nodePath, policy);

        NodeImpl acNode = getAcNode(nodePath);
        if (isAccessControlled(acNode)) {
            // build the template in order to have a return value
            AccessControlPolicy tmpl = createTemplate(acNode);
            if (tmpl.equals(policy)) {
                removeItem(acNode.getNode(N_POLICY));
                return;
            }
        }
        // node either not access-controlled or the passed policy didn't apply
        // to the node at 'nodePath' -> throw exception. no policy was removed
        throw new AccessControlException("Policy " + policy + " does not apply to " + nodePath);
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
                node = addNode(node, nName, NT_REP_ACCESS_CONTROL);
            }
        }
        return node;
    }

    /**
     * Check if the Node identified by <code>id</code> is itself part of ACL
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
     * Check if the specified policy can be set or removed at nodePath.
     *
     * @param nodePath
     * @param policy
     * @throws AccessControlException
     */
    private void checkValidPolicy(String nodePath, AccessControlPolicy policy)
            throws AccessControlException {
        if (policy == null || !(policy instanceof ACLTemplate)) {
            throw new AccessControlException("Attempt to set/remove invalid policy " + policy);
        }
        ACLTemplate acl = (ACLTemplate) policy;
        if (!nodePath.equals(acl.getPath())) {
            throw new AccessControlException("Policy " + policy + " is not applicable or does not apply to the node at " + nodePath);
        }
    }

    /**
     *
     * @param principal
     * @return
     * @throws RepositoryException
     */
    String getPathToAcNode(Principal principal) throws RepositoryException {
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
    private static boolean isAccessControlled(NodeImpl node) throws RepositoryException {
        return node != null && node.isNodeType(NT_REP_ACCESS_CONTROL) && node.hasNode(N_POLICY);
    }

    /**
     *
     * @param acNode
     * @return
     * @throws RepositoryException
     */
    private static AccessControlPolicy createTemplate(NodeImpl acNode) throws RepositoryException {
        if (!acNode.isNodeType(NT_REP_ACCESS_CONTROL)) {
            throw new RepositoryException("Expected node of type rep:AccessControl.");
        }

        Principal principal = null;
        String principalName = Text.unescapeIllegalJcrChars(acNode.getName());
        PrincipalManager pMgr = ((SessionImpl) acNode.getSession()).getPrincipalManager();
        if (pMgr.hasPrincipal(principalName)) {
            try {
                principal = pMgr.getPrincipal(principalName);
            } catch (NoSuchPrincipalException e) {
                // should not get here. 
            }
        }
        if (principal == null) {
            log.warn("Principal with name " + principalName + " unknown to PrincipalManager.");
            principal = new PrincipalImpl(principalName);
        }
        return new ACLTemplate(principal, acNode);
    }

    /**
     * Create a unique valid name for the Permission nodes to be save.
     *
     * @param node a name for the child is resolved
     * @param name if missing the {@link #DEFAULT_ACE_NAME} is taken
     * @return
     * @throws RepositoryException
     */
    protected static Name getUniqueNodeName(Node node, String name) throws RepositoryException {
        if (name == null) {
            name = DEFAULT_ACE_NAME;
        } else {
            try {
                NameParser.checkFormat(name);
            } catch (NameException e) {
                name = DEFAULT_ACE_NAME;
                log.debug("Invalid path name for Permission: " + name + ".");
            }
        }
        int i=0;
        String check = name;
        while (node.hasNode(check)) {
            check = name + i;
            i++;
        }
        return ((SessionImpl) node.getSession()).getQName(check);
    }
}
