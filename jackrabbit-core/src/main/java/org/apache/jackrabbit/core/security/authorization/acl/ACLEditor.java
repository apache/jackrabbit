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

import org.apache.jackrabbit.api.jsr283.security.AccessControlEntry;
import org.apache.jackrabbit.api.jsr283.security.AccessControlException;
import org.apache.jackrabbit.api.jsr283.security.Privilege;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SecurityItemModifier;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.authorization.AccessControlConstants;
import org.apache.jackrabbit.core.security.authorization.AccessControlEditor;
import org.apache.jackrabbit.core.security.authorization.PolicyEntry;
import org.apache.jackrabbit.core.security.authorization.PolicyTemplate;
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
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private static final String DEFAULT_PERMISSION_NAME = "permission";

    protected final SessionImpl session;
    private final PrincipalManager principalManager;

    protected ACLEditor(Session editingSession) throws RepositoryException {
        if (editingSession instanceof SessionImpl) {
            session = ((SessionImpl) editingSession);
            principalManager = ((SessionImpl) editingSession).getPrincipalManager();
        } else {
            throw new IllegalArgumentException("org.apache.jackrabbit.core.SessionImpl expected. Found " + editingSession.getClass());
        }
    }

    //------------------------------------------------< AccessControlEditor >---
    /**
     * @see AccessControlEditor#getPolicyTemplate(String)
     * @param nodePath
     */
    public PolicyTemplate getPolicyTemplate(String nodePath) throws AccessControlException, PathNotFoundException, RepositoryException {
        checkProtectsNode(nodePath);

        PolicyTemplate tmpl = null;
        NodeImpl aclNode = getAclNode(nodePath);
        if (aclNode != null) {
            tmpl = new ACLTemplate(aclNode, Collections.EMPTY_SET);
        }
        return tmpl;
    }

    /**
     * @see AccessControlEditor#editPolicyTemplate(String)
     * @param nodePath
     */
    public PolicyTemplate editPolicyTemplate(String nodePath) throws AccessControlException, PathNotFoundException, RepositoryException {
        checkProtectsNode(nodePath);

        PolicyTemplate tmpl;
        NodeImpl aclNode = getAclNode(nodePath);
        if (aclNode == null) {
            tmpl = new ACLTemplate(nodePath);
        } else {
            tmpl = new ACLTemplate(aclNode, Collections.EMPTY_SET);
        }
        return tmpl;
    }

    /**
     * @see AccessControlEditor#editPolicyTemplate(Principal)
     */
    public PolicyTemplate editPolicyTemplate(Principal principal) throws AccessDeniedException, AccessControlException, RepositoryException {
        throw new AccessControlException("Unable to edit policy for principal " + principal.getName());
    }

    /**
     * @see AccessControlEditor#setPolicyTemplate(String,PolicyTemplate)
     */
    public void setPolicyTemplate(String nodePath, PolicyTemplate template) throws RepositoryException {
        checkProtectsNode(nodePath);

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

        PolicyEntry[] entries = template.getEntries();
        for (int i = 0; i < entries.length; i++) {
            ACEImpl ace = (ACEImpl) entries[i];
            // TODO: improve
            Name nodeName = getUniqueNodeName(aclNode, ace.isAllow() ? "allow" : "deny");
            Name ntName = (ace.isAllow()) ? NT_REP_GRANT_ACE : NT_REP_DENY_ACE;
            ValueFactory vf = session.getValueFactory();

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
     * @see AccessControlEditor#removePolicyTemplate(String)
     */
    public PolicyTemplate removePolicyTemplate(String nodePath) throws AccessControlException, RepositoryException {
        checkProtectsNode(nodePath);

        PolicyTemplate tmpl = null;
        NodeImpl aclNode = getAclNode(nodePath);
        if (aclNode != null) {
            // need to build the template in order to have a return value.
            tmpl = new ACLTemplate(aclNode, Collections.EMPTY_SET);
            removeSecurityItem(aclNode);
        }
        return tmpl;
    }

    /**
     * @see AccessControlEditor#getAccessControlEntries(String)
     */
    public AccessControlEntry[] getAccessControlEntries(String nodePath) throws AccessControlException, PathNotFoundException, RepositoryException {
        PolicyTemplate pt = getPolicyTemplate(nodePath);
        if (pt == null) {
            return new AccessControlEntry[0];
        } else {
            PolicyEntry[] entries = pt.getEntries();
            List l = new ArrayList();
            for (int i = 0; i < entries.length; i++) {
                if (entries[i].isAllow()) {
                    l.add(entries[i]);
                }
            }
            return (AccessControlEntry[]) l.toArray(new AccessControlEntry[l.size()]);
        }
    }

    /**
     * @see AccessControlEditor#addAccessControlEntry(String,Principal,Privilege[])
     */
    public AccessControlEntry addAccessControlEntry(String nodePath, Principal principal, Privilege[] privileges) throws AccessControlException, PathNotFoundException, RepositoryException {
        // JSR 283 requires that the principal is known TODO: check again.
        if (!principalManager.hasPrincipal(principal.getName())) {
            throw new AccessControlException("Principal " + principal.getName() + " does not exist.");
        }

        ACLTemplate pt = (ACLTemplate) editPolicyTemplate(nodePath);
        // TODO: check again. maybe these 'grant-ACE' should be stored/evaluated separated
        int privs = PrivilegeRegistry.getBits(privileges);
        /*
        since added access control entry may never remove privileges that are
        granted by the policy -> retrieve existing allow entries and add
        the new privileges to be granted.
        Reason: PolicyTemplate#setEntry does in fact overwrite (which is fine
        when editing the policy itself, but wrong when adding ACEs over the JCR-API.
        */
        ACEImpl[] existing = pt.getEntries(principal);
        for (int i = 0; i < existing.length; i++) {
            if (existing[i].isAllow()) {
                privs |= existing[i].getPrivilegeBits();
            }
        }

        pt.setEntry(new ACEImpl(principal, privs, true));
        setPolicyTemplate(nodePath, pt);
        ACEImpl[] tmpls = pt.getEntries(principal);
        for (int i = 0; i < tmpls.length; i++) {
            if (tmpls[i].isAllow()) {
                return tmpls[i];
            }
        }
        // should never get here
        throw new AccessControlException("Internal error: No access control entry added.");
    }


    /**
     * @see AccessControlEditor#removeAccessControlEntry(String,AccessControlEntry)
     */
    public boolean removeAccessControlEntry(String nodePath, AccessControlEntry entry) throws AccessControlException, PathNotFoundException, RepositoryException {
        if (!(entry instanceof ACEImpl)) {
            throw new AccessControlException("Unknown AccessControlEntry implementation.");
        }
        // TODO: check again. maybe these 'grant-ACE' should be removed separated
        PolicyTemplate pt = editPolicyTemplate(nodePath);
        boolean removed = pt.removeEntry((ACEImpl) entry);
        if (removed) {
            setPolicyTemplate(nodePath, pt);
        }
        return removed;
    }

    //--------------------------------------------------------------------------
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
        NodeImpl node = getNode(nodePath);
        if (ACLProvider.protectsNode(node)) {
            throw new AccessControlException("Node " + nodePath + " defines ACL or ACE itself.");
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
     * @param name if missing the {@link #DEFAULT_PERMISSION_NAME} is taken
     * @return
     * @throws RepositoryException
     */
    protected static Name getUniqueNodeName(Node node, String name) throws RepositoryException {
        if (name == null) {
            name = DEFAULT_PERMISSION_NAME;
        } else {
            try {
                NameParser.checkFormat(name);
            } catch (NameException e) {
                name = DEFAULT_PERMISSION_NAME;
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
     */
    private static Value[] getPrivilegeNames(Privilege[] privileges, ValueFactory valueFactory) {
        Value[] names = new Value[privileges.length];
        for (int i = 0; i < privileges.length; i++) {
            names[i] = valueFactory.createValue(privileges[i].getName());
        }
        return names;
    }
}