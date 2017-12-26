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

import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlPolicy;
import org.apache.jackrabbit.api.security.authorization.PrivilegeManager;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.ProtectedItemModifier;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.authorization.AccessControlConstants;
import org.apache.jackrabbit.core.security.authorization.AccessControlEditor;
import org.apache.jackrabbit.core.security.authorization.AccessControlEntryImpl;
import org.apache.jackrabbit.core.security.authorization.AccessControlUtils;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NameParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.NodeIterator;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.Privilege;
import java.security.Principal;
import java.util.Set;

/**
 * <code>ACLEditor</code>...
 */
public class ACLEditor extends ProtectedItemModifier implements AccessControlEditor, AccessControlConstants {

    /**
     * the default logger
     */
    private static final Logger log = LoggerFactory.getLogger(ACLEditor.class);
    /**
     * Default name for ace nodes
     */
    private static final String DEFAULT_ACE_NAME = "ace";
    /**
     * the editing session
     */
    private final SessionImpl session;
    private final AccessControlUtils utils;
    private final boolean allowUnknownPrincipals;

    ACLEditor(Session editingSession, AccessControlUtils utils, boolean allowUnknownPrincipals) {
        super(Permission.MODIFY_AC);
        if (editingSession instanceof SessionImpl) {
            session = ((SessionImpl) editingSession);
        } else {
            throw new IllegalArgumentException("org.apache.jackrabbit.core.SessionImpl expected. Found " + editingSession.getClass());
        }
        this.utils = utils;
        this.allowUnknownPrincipals = allowUnknownPrincipals;
    }

    /**
     *
     * @param aclNode the node
     * @param path
     * @return the control list
     * @throws RepositoryException if an error occurs
     */
    ACLTemplate getACL(NodeImpl aclNode, String path) throws RepositoryException {
        return new ACLTemplate(aclNode, path, allowUnknownPrincipals);
    }

    //------------------------------------------------< AccessControlEditor >---
    /**
     * @see AccessControlEditor#getPolicies(String)
     */
    public AccessControlPolicy[] getPolicies(String nodePath) throws AccessControlException, PathNotFoundException, RepositoryException {
        checkProtectsNode(nodePath);

        NodeImpl aclNode = getAclNode(nodePath);
        if (aclNode == null) {
            return new AccessControlPolicy[0];
        } else {
            return new AccessControlPolicy[] {getACL(aclNode, nodePath)};
        }
    }

    /**
     * Always returns an empty array as no applicable policies are exposed.
     * 
     * @see AccessControlEditor#getPolicies(Principal)
     */
    public JackrabbitAccessControlPolicy[] getPolicies(Principal principal) throws AccessControlException, RepositoryException {
        if (!session.getPrincipalManager().hasPrincipal(principal.getName())) {
            throw new AccessControlException("Unknown principal.");
        }
        // TODO: impl. missing
        return new JackrabbitAccessControlPolicy[0];
    }

    /**
     * @see AccessControlEditor#editAccessControlPolicies(String)
     */
    public AccessControlPolicy[] editAccessControlPolicies(String nodePath) throws AccessControlException, PathNotFoundException, RepositoryException {
        checkProtectsNode(nodePath);

        String mixin;
        Name aclName;
        NodeImpl controlledNode;

        if (nodePath == null) {
            controlledNode = (NodeImpl) session.getRootNode();
            mixin = session.getJCRName(NT_REP_REPO_ACCESS_CONTROLLABLE);
            aclName = N_REPO_POLICY;
        } else {
            controlledNode = getNode(nodePath);
            mixin = session.getJCRName(NT_REP_ACCESS_CONTROLLABLE);
            aclName = N_POLICY;
        }

        AccessControlPolicy acl = null;
        NodeImpl aclNode = getAclNode(controlledNode, nodePath);
        if (aclNode == null) {
            // create an empty acl unless the node is protected or cannot have
            // mixin set (e.g. due to a lock) or
            // has colliding rep:policy or rep:repoPolicy child node set.
            if (controlledNode.hasNode(aclName)) {
                // policy child node without node being access controlled
                log.warn("Colliding policy child without node being access controllable ({}).", nodePath);
            } else {
                PrivilegeManager privMgr = ((JackrabbitWorkspace) session.getWorkspace()).getPrivilegeManager();
                if (controlledNode.isNodeType(mixin) || controlledNode.canAddMixin(mixin)) {
                    acl = new ACLTemplate(nodePath, session.getPrincipalManager(),
                            privMgr, session.getValueFactory(), session, allowUnknownPrincipals);
                } else {
                    log.warn("Node {} cannot be made access controllable.", nodePath);
                }
            }
        } // else: acl already present -> getPolicies must be used.

        return (acl != null) ? new AccessControlPolicy[] {acl} : new AccessControlPolicy[0];
    }

    /**
     * @see AccessControlEditor#editAccessControlPolicies(Principal)
     */
    public JackrabbitAccessControlPolicy[] editAccessControlPolicies(Principal principal) throws AccessDeniedException, AccessControlException, RepositoryException {
        if (!session.getPrincipalManager().hasPrincipal(principal.getName())) {
            throw new AccessControlException("Unknown principal.");
        }
        // TODO: impl. missing
        return new JackrabbitAccessControlPolicy[0];
    }

    /**
     * @see AccessControlEditor#setPolicy(String,AccessControlPolicy)
     */
    public void setPolicy(String nodePath, AccessControlPolicy policy) throws RepositoryException {
        checkProtectsNode(nodePath);
        checkValidPolicy(nodePath, policy);

        NodeImpl aclNode = getAclNode(nodePath);
        if (aclNode != null) {
            // remove all existing aces
            for (NodeIterator aceNodes = aclNode.getNodes(); aceNodes.hasNext();) {
                NodeImpl aceNode = (NodeImpl) aceNodes.nextNode();
                removeItem(aceNode);
            }
        } else {
            // create the acl node
            aclNode = (nodePath == null) ? createRepoAclNode() : createAclNode(nodePath);
        }
        
        AccessControlEntry[] entries = ((ACLTemplate) policy).getAccessControlEntries();
        for (AccessControlEntry entry : entries) {
            AccessControlEntryImpl ace = (AccessControlEntryImpl) entry;

            Name nodeName = getUniqueNodeName(aclNode, ace.isAllow() ? "allow" : "deny");
            Name ntName = (ace.isAllow()) ? NT_REP_GRANT_ACE : NT_REP_DENY_ACE;
            ValueFactory vf = session.getValueFactory();

            // create the ACE node
            NodeImpl aceNode = addNode(aclNode, nodeName, ntName);

            // write the rep:principalName property
            String principalName = ace.getPrincipal().getName();
            setProperty(aceNode, P_PRINCIPAL_NAME, vf.createValue(principalName));

            // ... and the rep:privileges property
            Privilege[] pvlgs = ace.getPrivileges();
            Value[] names = getPrivilegeNames(pvlgs, vf);
            setProperty(aceNode, P_PRIVILEGES, names);

            // store the restrictions:
            Set<Name> restrNames = ace.getRestrictions().keySet();
            for (Name restrName : restrNames) {
                Value value = ace.getRestriction(restrName);
                setProperty(aceNode, restrName, value);
            }
        }

        // mark the parent modified.
        markModified(((NodeImpl)aclNode.getParent()));
    }

    /**
     * @see AccessControlEditor#removePolicy(String,AccessControlPolicy)
     */
    public synchronized void removePolicy(String nodePath, AccessControlPolicy policy) throws AccessControlException, RepositoryException {
        checkProtectsNode(nodePath);
        checkValidPolicy(nodePath, policy);

        NodeImpl aclNode = getAclNode(nodePath);
        if (aclNode != null) {
            removeItem(aclNode);
        } else {
            throw new AccessControlException("No policy to remove at " + nodePath);
        }
    }

    //--------------------------------------------------------------------------
    /**
     * Check if the Node identified by <code>nodePath</code> is itself part of ACL
     * defining content. It this case setting or modifying an AC-policy is
     * obviously not possible.
     *
     * @param nodePath the node path
     * @throws AccessControlException If the given nodePath identifies a Node that
     * represents a ACL or ACE item.
     * @throws RepositoryException
     */
    private void checkProtectsNode(String nodePath) throws RepositoryException {
        if (nodePath != null) {
            NodeImpl node = getNode(nodePath);
            if (utils.isAcItem(node)) {
                throw new AccessControlException("Node " + nodePath + " defines ACL or ACE itself.");
            }
        }
    }

    /**
     * Check if the specified policy can be set/removed from this editor.
     *
     * @param nodePath the node path
     * @param policy the policy
     * @throws AccessControlException if not allowed
     */
    private static void checkValidPolicy(String nodePath, AccessControlPolicy policy) throws AccessControlException {
        if (policy == null || !(policy instanceof ACLTemplate)) {
            throw new AccessControlException("Attempt to set/remove invalid policy " + policy);
        }
        ACLTemplate acl = (ACLTemplate) policy;
        boolean matchingPath = (nodePath == null) ? acl.getPath() == null : nodePath.equals(acl.getPath());
        if (!matchingPath) {
            throw new AccessControlException("Policy " + policy + " cannot be applied/removed from the node at " + nodePath);
        }
    }

    /**
     *
     * @param path the path
     * @return the node
     * @throws RepositoryException if an error occurs
     */
    private NodeImpl getNode(String path) throws RepositoryException {
        return (NodeImpl) session.getNode(path);
    }

    /**
     * Returns the rep:Policy node below the Node identified at the given
     * path or <code>null</code> if the node is not mix:AccessControllable
     * or if no policy node exists.
     *
     * @param nodePath the node path
     * @return node or <code>null</code>
     * @throws PathNotFoundException if not found
     * @throws RepositoryException if an error occurs
     */
    private NodeImpl getAclNode(String nodePath) throws PathNotFoundException, RepositoryException {
        NodeImpl controlledNode;
        if (nodePath == null) {
            controlledNode = (NodeImpl) session.getRootNode();
        } else {
            controlledNode = getNode(nodePath);
        }
        return getAclNode(controlledNode, nodePath);
    }

    /**
     * Returns the rep:Policy node below the given Node or <code>null</code>
     * if the node is not mix:AccessControllable or if no policy node exists.
     *
     * @param controlledNode the controlled node
     * @param nodePath
     * @return node or <code>null</code>
     * @throws RepositoryException if an error occurs
     */
    private NodeImpl getAclNode(NodeImpl controlledNode, String nodePath) throws RepositoryException {
        NodeImpl aclNode = null;
        if (nodePath == null) {
            if (ACLProvider.isRepoAccessControlled(controlledNode)) {
                aclNode = controlledNode.getNode(N_REPO_POLICY);
            }
        } else {
            if (ACLProvider.isAccessControlled(controlledNode)) {
                aclNode = controlledNode.getNode(N_POLICY);
            }
        }
        return aclNode;
    }

    /**
     *
     * @param nodePath the node path
     * @return the new node
     * @throws RepositoryException if an error occurs
     */
    private NodeImpl createAclNode(String nodePath) throws RepositoryException {
        NodeImpl protectedNode = getNode(nodePath);
        if (!protectedNode.isNodeType(NT_REP_ACCESS_CONTROLLABLE)) {
            protectedNode.addMixin(NT_REP_ACCESS_CONTROLLABLE);
        }
        return addNode(protectedNode, N_POLICY, NT_REP_ACL);
    }

    /**
     *
     * @return the new acl node used to store repository level privileges.
     * @throws RepositoryException if an error occurs
     */
    private NodeImpl createRepoAclNode() throws RepositoryException {
        NodeImpl root = (NodeImpl) session.getRootNode();
        if (!root.isNodeType(NT_REP_REPO_ACCESS_CONTROLLABLE)) {
            root.addMixin(NT_REP_REPO_ACCESS_CONTROLLABLE);
        }
        return addNode(root, N_REPO_POLICY, NT_REP_ACL);
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

    /**
     * Build an array of Value from the specified <code>privileges</code> using
     * the given <code>valueFactory</code>.
     *
     * @param privileges the privileges
     * @param valueFactory the value factory
     * @return an array of Value.
     * @throws ValueFormatException if an error occurs
     */
    private static Value[] getPrivilegeNames(Privilege[] privileges, ValueFactory valueFactory)
            throws ValueFormatException {
        Value[] names = new Value[privileges.length];
        for (int i = 0; i < privileges.length; i++) {
            names[i] = valueFactory.createValue(privileges[i].getName(), PropertyType.NAME);
        }
        return names;
    }
}
