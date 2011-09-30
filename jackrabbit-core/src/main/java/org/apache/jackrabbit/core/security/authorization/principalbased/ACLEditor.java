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

import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.Privilege;
import javax.jcr.security.AccessControlPolicy;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlPolicy;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.ProtectedItemModifier;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.authorization.AccessControlConstants;
import org.apache.jackrabbit.core.security.authorization.AccessControlEditor;
import org.apache.jackrabbit.core.security.authorization.AccessControlEntryImpl;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
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
import javax.jcr.NodeIterator;
import java.security.Principal;
import java.util.Set;

/**
 * <code>ACLEditor</code>...
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
     * @see AccessControlEditor#getPolicies(Principal)
     */
    public JackrabbitAccessControlPolicy[] getPolicies(Principal principal) throws AccessControlException, RepositoryException {
        if (!session.getPrincipalManager().hasPrincipal(principal.getName())) {
            throw new AccessControlException("Cannot edit access control: " + principal.getName() +" isn't a known principal.");
        }
        JackrabbitAccessControlPolicy acl = getACL(principal);
        if (acl == null) {
            return new JackrabbitAccessControlPolicy[0];
        } else {
            return new JackrabbitAccessControlPolicy[] {acl};
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
                Principal p = getPrincipal(nodePath);
                if (p == null) {
                    throw new AccessControlException("Access control modification not allowed at " + nodePath);
                }
                acNode = createAcNode(nodePath);
            }

            if (!isAccessControlled(acNode)) {
                return new AccessControlPolicy[] {createTemplate(acNode)};
            } // else: acl has already been set before -> use getPolicies instead
        }

        // nodePath not below rep:policy -> not editable
        // or policy has been set before in which case getPolicies should be used instead.
        return new AccessControlPolicy[0];
    }

    /**
     * @see AccessControlEditor#editAccessControlPolicies(Principal)
     */
    public JackrabbitAccessControlPolicy[] editAccessControlPolicies(Principal principal) throws RepositoryException {
        if (!session.getPrincipalManager().hasPrincipal(principal.getName())) {
            throw new AccessControlException("Cannot edit access control: " + principal.getName() +" isn't a known principal.");
        }
        String nPath = getPathToAcNode(principal);
        NodeImpl acNode;
        if (!session.nodeExists(nPath)) {
            acNode = createAcNode(nPath);
        } else {
            acNode = (NodeImpl) session.getNode(nPath);
        }
        if (!isAccessControlled(acNode)) {
            return new JackrabbitAccessControlPolicy[] {createTemplate(acNode)};
        } else {
            // policy child node has already been created -> set policy has
            // been called before for this principal and getPolicy is used
            // to retrieve the ACL template.
            // no additional applicable policies present.
            return new JackrabbitAccessControlPolicy[0];
        }
    }

    /**
     * @see AccessControlEditor#setPolicy(String,AccessControlPolicy)
     */
    public void setPolicy(String nodePath, AccessControlPolicy policy)
            throws AccessControlException, PathNotFoundException, RepositoryException {
        checkProtectsNode(nodePath);
        checkValidPolicy(nodePath, policy);

        ACLTemplate acl = (ACLTemplate) policy;
        NodeImpl acNode = getAcNode(nodePath);
        if (acNode == null) {
            throw new PathNotFoundException("No such node " + nodePath);
        }

        // write the entries to the node
        NodeImpl aclNode;
        if (acNode.hasNode(N_POLICY)) {
            aclNode = acNode.getNode(N_POLICY);
            // remove all existing aces
            for (NodeIterator aceNodes = aclNode.getNodes(); aceNodes.hasNext();) {
                NodeImpl aceNode = (NodeImpl) aceNodes.nextNode();
                removeItem(aceNode);
            }
        } else {
            /* doesn't exist yet -> create */
            aclNode = addNode(acNode, N_POLICY, NT_REP_ACL);
        }

        /* add all new entries defined on the template */
        AccessControlEntry[] aces = acl.getAccessControlEntries();
        for (AccessControlEntry ace1 : aces) {
            AccessControlEntryImpl ace = (AccessControlEntryImpl) ace1;

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
            Set<Name> restrNames = ace.getRestrictions().keySet();
            for (Name restrName : restrNames) {
                Value value = ace.getRestriction(restrName);
                setProperty(aceNode, restrName, value);
            }
        }

        // mark the parent modified.
        markModified((NodeImpl) aclNode.getParent());
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
     * @param nodePath the node path
     * @return the node
     * @throws PathNotFoundException if the node does not exist
     * @throws RepositoryException if an error occurs
     */
    private NodeImpl getAcNode(String nodePath) throws PathNotFoundException,
            RepositoryException {
        if (Text.isDescendant(acRootPath, nodePath)) {
            return (NodeImpl) session.getNode(nodePath);
        } else {
            // node outside of rep:policy tree -> not handled by this editor.
            return null;
        }
    }

    private NodeImpl createAcNode(String acPath) throws RepositoryException {
        String[] segms = Text.explode(acPath, '/', false);
        StringBuilder currentPath = new StringBuilder();
        NodeImpl node = (NodeImpl) session.getRootNode();
        for (int i = 0; i < segms.length; i++) {
            if (i > 0) {
                currentPath.append('/').append(segms[i]);
            }
            Name nName = session.getQName(segms[i]);
            Name ntName;
            if (denotesPrincipalPath(currentPath.toString())) {
                ntName = NT_REP_PRINCIPAL_ACCESS_CONTROL;
            } else {
                ntName = (i < segms.length - 1) ? NT_REP_ACCESS_CONTROL : NT_REP_PRINCIPAL_ACCESS_CONTROL;
            }
            if (node.hasNode(nName)) {
                NodeImpl n = node.getNode(nName);
                if (!n.isNodeType(ntName)) {
                    // should never get here.
                    throw new RepositoryException("Error while creating access control node: Expected nodetype " + session.getJCRName(ntName) + " below /rep:accessControl, was " + node.getPrimaryNodeType().getName() + " instead");
                }
                node = n;
            } else {
                node = addNode(node, nName, ntName);
            }
        }
        return node;
    }

    private boolean denotesPrincipalPath(final String path) {
        if (path == null || path.length() == 0) {
            return false;
        }
        ItemBasedPrincipal princ = new ItemBasedPrincipal() {
            public String getPath() throws RepositoryException {
                return path;
            }
            public String getName() {
                return Text.getName(path);
            }
        };
        try {
            return session.getUserManager().getAuthorizable(princ) != null;
        } catch (RepositoryException e) {
            return false;
        }
    }

    /**
     * Check if the Node identified by <code>id</code> is itself part of ACL
     * defining content. It this case setting or modifying an AC-policy is
     * obviously not possible.
     *
     * @param nodePath the node path
     * @throws AccessControlException If the given id identifies a Node that
     * represents a ACL or ACE item.
     * @throws RepositoryException
     */
    private void checkProtectsNode(String nodePath) throws RepositoryException {
        if (nodePath == null) {
            // TODO: JCR-2774
            throw new UnsupportedRepositoryOperationException("JCR-2774");
        }
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
     * @param nodePath the node path
     * @param policy the policy
     * @throws AccessControlException if not allowed
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
     * @param principal the principal
     * @return the path
     * @throws RepositoryException if an error occurs
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

    /**
     * Returns the principal for the given path or null.
     *
     * @param pathToACNode
     * @return
     * @throws RepositoryException
     */
    private Principal getPrincipal(final String pathToACNode) throws RepositoryException {
        final String id = getPathName(pathToACNode);
        UserManager uMgr = session.getUserManager();
        Authorizable authorizable = uMgr.getAuthorizable(id);
        if (authorizable == null) {
            // human readable id in node name is different from the hashed id
            // use workaround to retrieve the principal
            if (pathToACNode.startsWith(acRootPath)) {
                final String principalPath = pathToACNode.substring(acRootPath.length());
                if (principalPath.indexOf('/', 1) > 0) {
                    // safe to build an item based principal
                    authorizable = uMgr.getAuthorizable(new ItemBasedPrincipal() {
                        public String getPath() throws RepositoryException {
                            return principalPath;
                        }
                        public String getName() {
                            return Text.getName(principalPath);
                        }
                    });
                } else {
                    // principal name was just appended to the acRootPath prefix
                    // see getPathToAcNode above -> try to retrieve principal by name.
                    return session.getPrincipalManager().getPrincipal(Text.getName(principalPath));
                }
            } // else: path doesn't start with acRootPath -> return null.
        }

        return (authorizable == null) ? null : authorizable.getPrincipal();
    }

    private static String getPathName(String pathToACNode) {
        return Text.unescapeIllegalJcrChars(Text.getName(pathToACNode));
    }

    /**
     *
     * @param node the node
     * @return <code>true</code> if access controlled
     * @throws RepositoryException if an error occurs
     */
    private static boolean isAccessControlled(NodeImpl node) throws RepositoryException {
        return node != null && node.isNodeType(NT_REP_PRINCIPAL_ACCESS_CONTROL) && node.hasNode(N_POLICY);
    }

    /**
     *
     * @param acNode the acl node
     * @return the polict
     * @throws RepositoryException if an error occurs
     */
    private JackrabbitAccessControlPolicy createTemplate(NodeImpl acNode) throws RepositoryException {
        if (!acNode.isNodeType(NT_REP_PRINCIPAL_ACCESS_CONTROL)) {
            String msg = "Unable to edit Access Control at "+ acNode.getPath()+ ". Expected node of type rep:PrinicipalAccessControl, was " + acNode.getPrimaryNodeType().getName();
            log.debug(msg);
            throw new AccessControlException(msg);
        }

        Principal principal = getPrincipal(acNode.getPath());
        if (principal == null) {
            // use fall back in order to be able to get/remove the policy
            String principalName = getPathName(acNode.getPath());
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
     * @return the name
     * @throws RepositoryException if an error occurs
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
        int i = 0;
        String check = name;
        while (node.hasNode(check)) {
            check = name + i;
            i++;
        }
        return ((SessionImpl) node.getSession()).getQName(check);
    }
}
