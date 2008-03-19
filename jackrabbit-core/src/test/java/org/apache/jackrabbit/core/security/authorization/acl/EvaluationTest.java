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

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.TestPrincipal;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.core.security.jsr283.security.AbstractAccessControlTest;
import org.apache.jackrabbit.core.security.jsr283.security.AccessControlEntry;
import org.apache.jackrabbit.core.security.jsr283.security.AccessControlManager;
import org.apache.jackrabbit.core.security.jsr283.security.AccessControlPolicy;
import org.apache.jackrabbit.core.security.jsr283.security.AccessControlPolicyIterator;
import org.apache.jackrabbit.core.security.jsr283.security.Privilege;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.util.Text;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.nodetype.ConstraintViolationException;
import java.security.Principal;

/**
 * <code>EvaluationTest</code>...
 */
public class EvaluationTest extends AbstractAccessControlTest {

    private User testUser;
    private SessionImpl testSession;
    private AccessControlManager testAcMgr;

    private String path;
    private String childNPath;
    private String childNPath2;
    private String childPPath;
    private String childchildPPath;
    private String siblingPath;

    protected void setUp() throws Exception {
        super.setUp();

        UserManager uMgr = getUserManager(superuser);
        Principal princ = new TestPrincipal("anyUser");
        Credentials creds = new SimpleCredentials("anyUser", "anyUser".toCharArray());

        Authorizable a = uMgr.getAuthorizable(princ);
        if (a == null) {
            testUser = uMgr.createUser("anyUser", creds, princ);
        } else if (a.isGroup()) {
            throw new NotExecutableException();
        } else {
            testUser = (User) a;
        }

        // TODO: remove cast once 283 is released.
        testSession = (SessionImpl) helper.getRepository().login(creds);
        testAcMgr = getAccessControlManager(testSession);

        // create some nodes below the test root in order to apply ac-stuff
        Node node = testRootNode.addNode(nodeName1, testNodeType);
        Node cn1 = node.addNode(nodeName2, testNodeType);
        Property cp1 = node.setProperty(propertyName1, "anyValue");
        Node cn2 = node.addNode(nodeName3, testNodeType);

        Property ccp1 = cn1.setProperty(propertyName1, "childNodeProperty");

        Node n2 = testRootNode.addNode(nodeName2, testNodeType);
        superuser.save();

        path = node.getPath();
        childNPath = cn1.getPath();
        childNPath2 = cn2.getPath();
        childPPath = cp1.getPath();
        childchildPPath = ccp1.getPath();
        siblingPath = n2.getPath();
    }

    protected void tearDown() throws Exception {
        super.tearDown();

        if (testSession != null && testSession.isLive()) {
            testSession.logout();
        }
        if (testUser != null) {
            testUser.remove();
        }
    }

    private static UserManager getUserManager(Session session) throws NotExecutableException {
        if (!(session instanceof JackrabbitSession)) {
            throw new NotExecutableException();
        }

        try {
            return ((JackrabbitSession) session).getUserManager();
        } catch (RepositoryException e) {
            throw new NotExecutableException();
        }
    }

    private static ACLTemplate getACLTemplate(AccessControlManager acM, String path) throws RepositoryException, AccessDeniedException {
        AccessControlPolicyIterator it = acM.getApplicablePolicies(path);
        while (it.hasNext()) {
            AccessControlPolicy acp = it.nextAccessControlPolicy();
            if (acp instanceof ACLTemplate) {
                return (ACLTemplate) acp;
            }
        }
        // TODO: change to NotExecutableException
        throw new RepositoryException();
    }

    private void givePrivileges(String nPath, int privileges) throws NotExecutableException, RepositoryException {
        ACLTemplate tmpl = getACLTemplate(acMgr, nPath);
        tmpl.setEntry(new ACEImpl(testUser.getPrincipal(), privileges, true));
        acMgr.setPolicy(nPath, tmpl);
        superuser.save();
    }

    private void withdrawPrivileges(String nPath, int privileges) throws NotExecutableException, RepositoryException {
        ACLTemplate tmpl = getACLTemplate(acMgr, nPath);
        tmpl.setEntry(new ACEImpl(testUser.getPrincipal(), privileges, false));
        acMgr.setPolicy(nPath, tmpl);
        superuser.save();
    }

    private void checkReadOnly(String path) throws RepositoryException {
        Privilege[] privs = testAcMgr.getPrivileges(path);
        assertTrue(privs.length == 1);
        assertEquals(PrivilegeRegistry.READ_PRIVILEGE, privs[0]);
    }

    public void testGrantedPermissions() throws RepositoryException, AccessDeniedException, NotExecutableException {
        /* precondition:
           testuser must have READ-only permission on test-node and below
         */
        checkReadOnly(path);

        // give 'testUser' ADD_CHILD_NODES|MODIFY_PROPERTIES privileges at 'path'
        givePrivileges(path, PrivilegeRegistry.ADD_CHILD_NODES | PrivilegeRegistry.MODIFY_PROPERTIES);
        /*
         testuser must now have
         - ADD_NODE permission for child node
         - SET_PROPERTY permission for child props
         - REMOVE permission for child-props
         - READ-only permission for the node at 'path'

         testuser must not have
         - REMOVE permission for child node
        */
        String nonExChildPath = path + "/anyItem";
        assertTrue(testSession.hasPermission(nonExChildPath, "read,add_node,set_property"));
        assertFalse(testSession.hasPermission(nonExChildPath, "remove"));

        Node testN = testSession.getNode(path);

        // must be allowed to add child node
        testN.addNode(nodeName3, testNodeType);
        testSession.save();

        // must be allowed to remove child-property
        testSession.getProperty(childPPath).remove();
        testSession.save();

        // must be allowed to set child property again
        testN.setProperty(Text.getName(childPPath), "othervalue");
        testSession.save();

        // must not be allowed to remove child nodes
        try {
            testSession.getNode(childNPath).remove();
            testSession.save();
            fail("test-user is not allowed to remove a node below " + path);
        } catch (AccessDeniedException e) {
            // success
        }

        // must have read-only access on 'testN' and it's sibling
        assertTrue(testSession.hasPermission(path, "read"));
        assertFalse(testSession.hasPermission(path, "add_node,set_property,remove"));
        checkReadOnly(siblingPath);
    }

    public void testDeniedPermission() throws RepositoryException, NotExecutableException, InterruptedException {
         /* precondition:
           testuser must have READ-only permission on test-node and below
         */
        checkReadOnly(path);

        // withdraw READ privilege to 'testUser' at 'path'
        withdrawPrivileges(childNPath, PrivilegeRegistry.READ);
        /*
         testuser must now have
         - READ-only permission for the child-props of path

         testuser must not have
         - any permission on child-node and all its subtree
        */

        // must still have read-access to path, ...
        assertTrue(testSession.hasPermission(path, "read"));
        Node n = testSession.getNode(path);
        // ... siblings of childN
        testSession.getNode(childNPath2);
        // ... and props of path
        assertTrue(n.getProperties().hasNext());

        // must not have access to 'childNPath'
        assertFalse(testSession.itemExists(childNPath));
        try {
            Node testN = testSession.getNode(childNPath);
            fail("Read access has been denied -> cannot retrieve child node.");
        } catch (PathNotFoundException e) {
            // ok.
        }

        // must not have access to subtree below 'childNPath'
        assertFalse(testSession.itemExists(childchildPPath));
        try {
            testSession.getItem(childchildPPath);
            fail("Read access has been denied -> cannot retrieve prop below child node.");
        } catch (PathNotFoundException e) {
            // ok.
        }
    }

    public void testAccessControlRead() throws NotExecutableException, RepositoryException {
        checkReadOnly(path);

        // re-grant READ in order to have an ACL-node
        givePrivileges(path, PrivilegeRegistry.READ);
        // make sure the 'rep:policy' node has been created.
        assertTrue(superuser.itemExists(path + "/rep:policy"));

        /*
         Testuser must still have READ-only access only and must not be
         allowed to view the acl-node that has been created.
        */
        assertFalse(testAcMgr.hasPrivileges(path, new Privilege[] {PrivilegeRegistry.READ_AC_PRIVILEGE}));
        assertFalse(testSession.itemExists(path + "/rep:policy"));
        Node n = testSession.getNode(path);
        assertFalse(n.hasNode("rep:policy"));
        try {
            n.getNode("rep:policy");
            fail("Accessing the rep:policy node must throw PathNotFoundException.");
        } catch (PathNotFoundException e) {
            // ok.
        }

        /* Finally the test user must not be allowed to remove the policy. */
        try {
            testAcMgr.removePolicy(path);
            fail("Test user must not be allowed to remove the access control policy.");
        } catch (AccessDeniedException e) {
            // success
        }
    }

    public void testAccessControlModification() throws RepositoryException, NotExecutableException {
        /* precondition:
          testuser must have READ-only permission on test-node and below
        */
        checkReadOnly(path);

        // give 'testUser' ADD_CHILD_NODES|MODIFY_PROPERTIES| REMOVE_CHILD_NODES privileges at 'path'
        givePrivileges(path, PrivilegeRegistry.ADD_CHILD_NODES | PrivilegeRegistry.REMOVE_CHILD_NODES | PrivilegeRegistry.MODIFY_PROPERTIES);
        /*
         testuser must not have
         - permission to view AC items
         - permission to modify AC items
        */

        // make sure the 'rep:policy' node has been created.
        assertTrue(superuser.itemExists(path + "/rep:policy"));

        assertFalse(testSession.itemExists(path + "/rep:policy"));
        try {
            testAcMgr.getPolicy(path);
            fail("test user must not have READ_AC privilege.");
        } catch (AccessDeniedException e) {
            // success
        }
        try {
            testAcMgr.getEffectivePolicy(path);
            fail("test user must not have READ_AC privilege.");
        } catch (AccessDeniedException e) {
            // success
        }
        try {
            testAcMgr.getAccessControlEntries(path);
            fail("test user must not have READ_AC privilege.");
        } catch (AccessDeniedException e) {
            // success
        }
        try {
            testAcMgr.removePolicy(path);
            fail("test user must not have MODIFY_AC privilege.");
        } catch (AccessDeniedException e) {
            // success
        }
    }

    public void testAccessControlModification2() throws RepositoryException, NotExecutableException {
        /* precondition:
          testuser must have READ-only permission on test-node and below
        */
        checkReadOnly(path);

        // give 'testUser' READ_AC|MODIFY_AC privileges at 'path'
        givePrivileges(path, PrivilegeRegistry.READ_AC | PrivilegeRegistry.MODIFY_AC);
        /*
         testuser must
         - still have the inherited READ permission.
         - must have permission to view AC items at 'path' (and below)
         - must have permission to modify AC items at 'path'

         testuser must not have
         - permission to view AC items outside of the tree defined by path.
        */

        // make sure the 'rep:policy' node has been created.
        assertTrue(testSession.itemExists(path + "/rep:policy"));

        // test: READ_AC privilege does not apply outside of the tree.
        try {
            testAcMgr.getPolicy(siblingPath);
            fail("READ_AC privilege must not apply outside of the tree it has applied to.");
        } catch (AccessDeniedException e) {
            // success
        }

        // test: MODIFY_AC privilege does not apply outside of the tree.
        try {
            testAcMgr.addAccessControlEntry(siblingPath,
                testUser.getPrincipal(),
                new Privilege[] {PrivilegeRegistry.WRITE_PRIVILEGE});
            fail("MODIFY_AC privilege must not apply outside of the tree it has applied to.");
        } catch (AccessDeniedException e) {
            // success
        }

        // test if testuser can READ access control on the path and on the
        // entire subtree that gets the policy inherited.
        AccessControlPolicy policy = testAcMgr.getPolicy(path);
        AccessControlPolicy effPOnChild = testAcMgr.getEffectivePolicy(childNPath);

        // test if testuser can modify AC-items
        // 1) add an ac-entry
        AccessControlEntry entry = testAcMgr.addAccessControlEntry(path,
                testUser.getPrincipal(),
                new Privilege[] {PrivilegeRegistry.WRITE_PRIVILEGE});
        testSession.save();

        assertTrue(testAcMgr.hasPrivileges(path,
                new Privilege[] {PrivilegeRegistry.REMOVE_CHILD_NODES_PRIVILEGE}));

        // 2) remove the policy
        testAcMgr.removePolicy(path);
        testSession.save();

        // Finally: testuser removed the policy that granted him permission
        // to modify the AC content. Since testuser removed the policy, it's
        // privileges must be gone again...
        try {
            testAcMgr.getEffectivePolicy(childNPath);
            fail("READ_AC privilege has been revoked -> must throw again.");
        } catch (AccessDeniedException e) {
            // success
        }
        // ... and since the ACE is stored with the policy all right except
        // READ must be gone.
        checkReadOnly(path);
    }

    public void testACItemsAreProtected() throws NotExecutableException, RepositoryException {
        // make sure a rep:policy node is present at 'path'
        givePrivileges(path, PrivilegeRegistry.WRITE);
        Node n = ((SessionImpl) superuser).getNode(path);
        Node policyNode = n.getNode("rep:policy");

        assertTrue("The rep:policy node must be protected", policyNode.getDefinition().isProtected());
        try {
            policyNode.remove();
            fail("rep:policy node must be protected.");
        } catch (ConstraintViolationException e) {
            // success
        }
        Node aceNode = null;
        for (NodeIterator it = policyNode.getNodes(); it.hasNext();) {
            n = it.nextNode();
            if (n.isNodeType("rep:ACE")) {
                aceNode = n;
                break;
            }
        }
        if (aceNode == null) {
            fail("Child-node expected below rep:policy node.");
        }
        try {
            aceNode.remove();
            fail("ACE node must be protected.");
        } catch (ConstraintViolationException e) {
            // success
        }
        try {
            aceNode.setProperty("anyProperty", "anyValue");
            fail("ACE node must be protected.");
        } catch (ConstraintViolationException e) {
            // success
        }
        try {
            policyNode.setProperty("test", "anyvalue");
            fail("rep:policy node must be protected.");
        } catch (ConstraintViolationException e) {
            // success
        }
        try {
            policyNode.addNode("test", aceNode.getPrimaryNodeType().getName());
            fail("rep:policy node must be protected.");
        } catch (ConstraintViolationException e) {
            // success
        }
    }
}