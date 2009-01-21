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

import org.apache.jackrabbit.api.jsr283.security.AccessControlException;
import org.apache.jackrabbit.api.jsr283.security.Privilege;
import org.apache.jackrabbit.api.jsr283.security.AccessControlEntry;
import org.apache.jackrabbit.api.jsr283.security.AccessControlPolicy;
import org.apache.jackrabbit.api.jsr283.security.AccessControlList;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SecurityItemModifier;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.authorization.AccessControlConstants;
import org.apache.jackrabbit.core.security.authorization.AccessControlEditor;
import org.apache.jackrabbit.core.security.authorization.AccessControlUtils;
import org.apache.jackrabbit.core.security.authorization.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NameParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.PropertyType;
import javax.jcr.ValueFormatException;
import java.security.Principal;

/**
 * <code>ACLEditor</code>...
 */
public class ACLEditor extends SecurityItemModifier implements AccessControlEditor, AccessControlConstants {

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
    private final PrivilegeRegistry privilegeRegistry;
    private final AccessControlUtils utils;

    ACLEditor(Session editingSession, AccessControlUtils utils) {
        super(true);
        if (editingSession instanceof SessionImpl) {
            session = ((SessionImpl) editingSession);
            // TODO: review and find better solution
            privilegeRegistry = new PrivilegeRegistry(session);
        } else {
            throw new IllegalArgumentException("org.apache.jackrabbit.core.SessionImpl expected. Found " + editingSession.getClass());
        }
        this.utils = utils;
    }

    /**
     *
     * @param aclNode
     * @return
     * @throws RepositoryException
     */
    AccessControlList getACL(NodeImpl aclNode) throws RepositoryException {
        return new ACLTemplate(aclNode, privilegeRegistry);
    }

    //------------------------------------------------< AccessControlEditor >---
    /**
     * @see AccessControlEditor#getPolicies(String)
     * @param nodePath
     */
    public AccessControlPolicy[] getPolicies(String nodePath) throws AccessControlException, PathNotFoundException, RepositoryException {
        checkProtectsNode(nodePath);

        NodeImpl aclNode = getAclNode(nodePath);
        if (aclNode == null) {
            return new AccessControlPolicy[0];
        } else {
            return new AccessControlPolicy[] {getACL(aclNode)};
        }
    }

    /**
     * @see AccessControlEditor#editAccessControlPolicies(String)
     * @param nodePath
     */
    public AccessControlPolicy[] editAccessControlPolicies(String nodePath) throws AccessControlException, PathNotFoundException, RepositoryException {
        checkProtectsNode(nodePath);

        AccessControlPolicy acl;
        NodeImpl aclNode = getAclNode(nodePath);
        if (aclNode == null) {
            // create an empty acl
            acl = new ACLTemplate(nodePath, session.getPrincipalManager(), privilegeRegistry);
        } else {
            acl = getACL(aclNode);
        }
        return new AccessControlPolicy[] {acl};
    }

    /**
     * @see AccessControlEditor#editAccessControlPolicies(Principal)
     */
    public AccessControlPolicy[] editAccessControlPolicies(Principal principal) throws AccessDeniedException, AccessControlException, RepositoryException {
        if (!session.getPrincipalManager().hasPrincipal(principal.getName())) {
            throw new AccessControlException("Unknown principal.");
        }
        // TODO: impl. missing
        return new AccessControlPolicy[0];
    }

    /**
     * @see AccessControlEditor#setPolicy(String,AccessControlPolicy)
     */
    public void setPolicy(String nodePath, AccessControlPolicy policy) throws RepositoryException {
        checkProtectsNode(nodePath);
        checkValidPolicy(nodePath, policy);

        NodeImpl aclNode = getAclNode(nodePath);
        /* in order to assert that the parent (ac-controlled node) gets modified
           an existing ACL node is removed first and the recreated.
           this also asserts that all ACEs are cleared without having to
           access and removed the explicitely
         */
        if (aclNode != null) {
            removeSecurityItem(aclNode);
        }
        // now (re) create it
        aclNode = createAclNode(nodePath);

        AccessControlEntry[] entries = ((ACLTemplate) policy).getAccessControlEntries();
        for (int i = 0; i < entries.length; i++) {
            JackrabbitAccessControlEntry ace = (JackrabbitAccessControlEntry) entries[i];

            Name nodeName = getUniqueNodeName(aclNode, ace.isAllow() ? "allow" : "deny");
            Name ntName = (ace.isAllow()) ? NT_REP_GRANT_ACE : NT_REP_DENY_ACE;
            ValueFactory vf = session.getValueFactory();

            // create the ACE node
            NodeImpl aceNode = addSecurityNode(aclNode, nodeName, ntName);

            // write the rep:principalName property
            String principalName = ace.getPrincipal().getName();
            setSecurityProperty(aceNode, P_PRINCIPAL_NAME, vf.createValue(principalName));

            // ... and the rep:privileges property
            Privilege[] pvlgs = ace.getPrivileges();
            Value[] names = getPrivilegeNames(pvlgs, vf);
            setSecurityProperty(aceNode, P_PRIVILEGES, names);
        }
    }

    /**
     * @see AccessControlEditor#removePolicy(String,AccessControlPolicy)
     */
    public synchronized void removePolicy(String nodePath, AccessControlPolicy policy) throws AccessControlException, RepositoryException {
        checkProtectsNode(nodePath);
        checkValidPolicy(nodePath, policy);

        NodeImpl aclNode = getAclNode(nodePath);
        if (aclNode != null) {
            removeSecurityItem(aclNode);
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
     * @param nodePath
     * @throws AccessControlException If the given nodePath identifies a Node that
     * represents a ACL or ACE item.
     * @throws RepositoryException
     */
    private void checkProtectsNode(String nodePath) throws RepositoryException {
        NodeImpl node = getNode(nodePath);
        if (utils.isAcItem(node)) {
            throw new AccessControlException("Node " + nodePath + " defines ACL or ACE itself.");
        }
    }

    /**
     * Check if the specified policy can be set/removed from this editor.
     *
     * @param nodePath
     * @param policy
     * @throws AccessControlException
     */
    private static void checkValidPolicy(String nodePath, AccessControlPolicy policy) throws AccessControlException {
        if (policy == null || !(policy instanceof ACLTemplate)) {
            throw new AccessControlException("Attempt to set/remove invalid policy " + policy);
        }
        ACLTemplate acl = (ACLTemplate) policy;
        if (!nodePath.equals(acl.getPath())) {
            throw new AccessControlException("Policy " + policy + " cannot be applied/removed from the node at " + nodePath);
        }
    }

    /**
     *
     * @param path
     * @return
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    private NodeImpl getNode(String path) throws PathNotFoundException, RepositoryException {
        return (NodeImpl) session.getNode(path);
    }

    /**
     * Returns the rep:Policy node below the Node identified by the given
     * id or <code>null</code> if the node is not mix:AccessControllable
     * or if no policy node exists.
     *
     * @param nodePath
     * @return node or <code>null</code>
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    private NodeImpl getAclNode(String nodePath) throws PathNotFoundException, RepositoryException {
        NodeImpl aclNode = null;
        NodeImpl protectedNode = getNode(nodePath);
        if (ACLProvider.isAccessControlled(protectedNode)) {
            aclNode = protectedNode.getNode(N_POLICY);
        }
        return aclNode;
    }

    /**
     *
     * @param nodePath
     * @return
     * @throws RepositoryException
     */
    private NodeImpl createAclNode(String nodePath) throws RepositoryException {
        NodeImpl protectedNode = getNode(nodePath);
        if (!protectedNode.isNodeType(NT_REP_ACCESS_CONTROLLABLE)) {
            protectedNode.addMixin(NT_REP_ACCESS_CONTROLLABLE);
        }
        return addSecurityNode(protectedNode, N_POLICY, NT_REP_ACL);
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

    /**
     * Build an array of Value from the specified <code>privileges</code> using
     * the given <code>valueFactory</code>.
     *
     * @param privileges
     * @param valueFactory
     * @return an array of Value.
     * @throws javax.jcr.ValueFormatException
     */
    private static Value[] getPrivilegeNames(Privilege[] privileges, ValueFactory valueFactory) throws ValueFormatException {
        Value[] names = new Value[privileges.length];
        for (int i = 0; i < privileges.length; i++) {
            names[i] = valueFactory.createValue(privileges[i].getName(), PropertyType.NAME);
        }
        return names;
    }
}