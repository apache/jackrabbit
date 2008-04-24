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
package org.apache.jackrabbit.core.security.authorization;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.TestPrincipal;
import org.apache.jackrabbit.core.security.jsr283.security.AbstractAccessControlTest;
import org.apache.jackrabbit.core.security.jsr283.security.AccessControlManager;
import org.apache.jackrabbit.core.security.jsr283.security.Privilege;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.test.JUnitTest;
import org.apache.jackrabbit.test.api.observation.EventResult;
import org.apache.jackrabbit.util.Text;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.observation.ObservationManager;
import javax.jcr.observation.Event;
import javax.jcr.nodetype.ConstraintViolationException;
import java.security.Principal;

/**
 * <code>AbstractEvaluationTest</code>...
 */
public abstract class AbstractEvaluationTest extends AbstractAccessControlTest {

    protected static final long DEFAULT_WAIT_TIMEOUT = 5000;

    protected Credentials creds;
    protected User testUser;
    protected SessionImpl testSession;
    protected AccessControlManager testAcMgr;

    protected String path;
    protected String childNPath;
    protected String childNPath2;
    protected String childPPath;
    protected String childchildPPath;
    protected String siblingPath;

    // TODO: test AC for moved node
    // TODO: test AC for moved AC-controlled node
    // TODO: test if combination of group and user permissions are properly evaluated

    protected void setUp() throws Exception {
        super.setUp();

        UserManager uMgr = getUserManager(superuser);
        Principal princ = new TestPrincipal("anyUser");
        creds = new SimpleCredentials("anyUser", "anyUser".toCharArray());

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
        if (testSession != null && testSession.isLive()) {
            testSession.logout();
        }
        // make sure all ac info is removed
        clearACInfo();
        // remove the test user again.
        if (testUser != null) {
            testUser.remove();
        }
        super.tearDown();
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

    protected abstract void clearACInfo();

    protected abstract PolicyTemplate getPolicyTemplate(AccessControlManager acM, String path) throws RepositoryException, AccessDeniedException, NotExecutableException;

    protected abstract PolicyEntry createEntry(Principal principal, int privileges, boolean isAllow, String[] restrictions);

    protected abstract String[] getRestrictions(String path);

    protected PolicyTemplate givePrivileges(String nPath, int privileges, String[] restrictions) throws NotExecutableException, RepositoryException {
        PolicyTemplate tmpl = getPolicyTemplate(acMgr, nPath);
        tmpl.setEntry(createEntry(testUser.getPrincipal(), privileges, true, restrictions));
        acMgr.setPolicy(tmpl.getPath(), tmpl);
        superuser.save();
        return tmpl;
    }

    protected PolicyTemplate withdrawPrivileges(String nPath, int privileges, String[] restrictions) throws NotExecutableException, RepositoryException {
        PolicyTemplate tmpl = getPolicyTemplate(acMgr, nPath);
        tmpl.setEntry(createEntry(testUser.getPrincipal(), privileges, false, restrictions));
        acMgr.setPolicy(tmpl.getPath(), tmpl);
        superuser.save();
        return tmpl;
    }

    protected void checkReadOnly(String path) throws RepositoryException {
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
        givePrivileges(path, PrivilegeRegistry.ADD_CHILD_NODES |
                PrivilegeRegistry.MODIFY_PROPERTIES, getRestrictions(path));
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
        withdrawPrivileges(childNPath, PrivilegeRegistry.READ, getRestrictions(childNPath));
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

        /*
        testSession must not have access to 'childNPath'
        */
        assertFalse(testSession.itemExists(childNPath));
        try {
            Node testN = testSession.getNode(childNPath);
            fail("Read access has been denied -> cannot retrieve child node.");
        } catch (PathNotFoundException e) {
            // ok.
        }
        /*
        -> must not have access to subtree below 'childNPath'
        */
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
        PolicyTemplate tmpl = givePrivileges(path, PrivilegeRegistry.READ, getRestrictions(path));
        // make sure the 'rep:policy' node has been created.
        assertTrue(superuser.itemExists(tmpl.getPath() + "/rep:policy"));

        /*
         Testuser must still have READ-only access only and must not be
         allowed to view the acl-node that has been created.
        */
        assertFalse(testAcMgr.hasPrivileges(path, new Privilege[] {
                PrivilegeRegistry.READ_AC_PRIVILEGE
        }));
        assertFalse(testSession.itemExists(path + "/rep:policy"));

        Node n = testSession.getNode(tmpl.getPath());
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
        PolicyTemplate tmpl = givePrivileges(path,
                PrivilegeRegistry.ADD_CHILD_NODES |
                PrivilegeRegistry.REMOVE_CHILD_NODES |
                PrivilegeRegistry.MODIFY_PROPERTIES, getRestrictions(path));
        /*
         testuser must not have
         - permission to view AC items
         - permission to modify AC items
        */

        // make sure the 'rep:policy' node has been created.
        assertTrue(superuser.itemExists(tmpl.getPath() + "/rep:policy"));
        // the policy node however must not be visible to the test-user
        assertFalse(testSession.itemExists(tmpl.getPath() + "/rep:policy"));
        try {
            testAcMgr.getPolicy(tmpl.getPath());
            fail("test user must not have READ_AC privilege.");
        } catch (AccessDeniedException e) {
            // success
        }
        try {
            testAcMgr.getEffectivePolicy(tmpl.getPath());
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
            testAcMgr.getAccessControlEntries(tmpl.getPath());
            fail("test user must not have READ_AC privilege.");
        } catch (AccessDeniedException e) {
            // success
        }
        try {
            testAcMgr.removePolicy(tmpl.getPath());
            fail("test user must not have MODIFY_AC privilege.");
        } catch (AccessDeniedException e) {
            // success
        }
    }

    public void testWithDrawRead() throws RepositoryException, NotExecutableException {
        /*
         precondition:
         testuser must have READ-only permission on test-node and below
        */
        checkReadOnly(path);

        // give 'testUser' READ_AC|MODIFY_AC privileges at 'path'
        givePrivileges(path, PrivilegeRegistry.WRITE, getRestrictions(path));
        // withdraw the READ privilege
        withdrawPrivileges(path, PrivilegeRegistry.READ, getRestrictions(path));

        // test if login as testuser -> item at path must not exist.
        Session s = null;
        try {
            s = helper.getRepository().login(creds);
            assertFalse(s.itemExists(path));
        } finally {
            if (s != null) {
                s.logout();
            }
        }
    }

    public void testEventGeneration() throws RepositoryException, NotExecutableException {
        /*
         precondition:
         testuser must have READ-only permission on test-node and below
        */
        checkReadOnly(path);

        // withdraw the READ privilege
        withdrawPrivileges(path, PrivilegeRegistry.READ, getRestrictions(path));

        // testUser registers a eventlistener for 'path
        ObservationManager obsMgr = testSession.getWorkspace().getObservationManager();
        EventResult listener = new EventResult(((JUnitTest) this).log);
        try {
            obsMgr.addEventListener(listener, Event.NODE_REMOVED, path, true, new String[0], new String[0], true);

            // superuser removes the node with childNPath in order to provoke
            // events being generated
            superuser.getItem(childNPath).remove();
            superuser.save();

            obsMgr.removeEventListener(listener);
            // since the testUser does not have read-permission on the removed
            // node, no corresponding event must be generated.
            Event[] evts = listener.getEvents(DEFAULT_WAIT_TIMEOUT);
            for (int i = 0; i < evts.length; i++) {
                if (evts[i].getType() == Event.NODE_REMOVED &&
                        evts[i].getPath().equals(childNPath)) {
                    fail("TestUser does not have READ permission below " + path + " -> events below must not show up.");
                }
            }
        } finally {
            obsMgr.removeEventListener(listener);
        }
    }

    public void testInheritance() throws RepositoryException, NotExecutableException {
        /* precondition:
          testuser must have READ-only permission on test-node and below
        */
        checkReadOnly(path);

        // give 'modify-properties' privilege on 'path'
        givePrivileges(path, PrivilegeRegistry.MODIFY_PROPERTIES, getRestrictions(path));
        // give 'add-child-nodes' and 'remove-child-nodes' privilege on 'child-path'
        givePrivileges(childNPath, PrivilegeRegistry.ADD_CHILD_NODES |
                PrivilegeRegistry.REMOVE_CHILD_NODES, getRestrictions(childNPath));

        /*
        since permission evaluation respects inheritance through the node
        hierarchy, the following privileges must now be given at 'childNPath':
        - read
        - modify-properties
        - add-child-nodes
        - remove-child-nodes
        -> read + write
        */
        Privilege[] privs = new Privilege[] {
                PrivilegeRegistry.READ_PRIVILEGE,
                PrivilegeRegistry.WRITE_PRIVILEGE
        };
        assertTrue(testAcMgr.hasPrivileges(childNPath, privs));
        /*
        ... and the following permissions must be granted at any child item
        of child-path:
        - read
        - set-property
        - add-node
        - remove
        */
        String nonExistingItemPath = childNPath + "/anyItem";
        String actions = SessionImpl.ADD_NODE_ACTION + "," +
                SessionImpl.REMOVE_ACTION + "," +
                SessionImpl.SET_PROPERTY_ACTION + "," +
                SessionImpl.READ_ACTION;
        assertTrue(testSession.hasPermission(nonExistingItemPath, actions));

        /* try adding a new child node -> must succeed. */
        Node childN = testSession.getNode(childNPath);
        Node testChild = childN.addNode(nodeName2, testNodeType);

        /* test privileges on the 'new' child node */
        privs = testAcMgr.getPrivileges(testChild.getPath());
        int exptectedPrivs = PrivilegeRegistry.WRITE | PrivilegeRegistry.READ;
        assertTrue(exptectedPrivs == PrivilegeRegistry.getBits(privs));

        /* repeate test after save. */
        testSession.save();
        privs = testAcMgr.getPrivileges(testChild.getPath());
        assertTrue(exptectedPrivs == PrivilegeRegistry.getBits(privs));
    }

    public void testNewNodes() throws RepositoryException {
        /*
         precondition:
         testuser must have READ-only permission on test-node and below
        */
        checkReadOnly(path);

        /* create some new nodes below 'path' */
        Node n = testSession.getNode(path);
        for (int i = 0; i < 5; i++) {
            n = n.addNode(nodeName2, testNodeType);
        }

        /* make sure the same privileges/permissions are granted as at path. */
        Privilege[] privs = testAcMgr.getPrivileges(n.getPath());
        assertTrue(PrivilegeRegistry.READ == PrivilegeRegistry.getBits(privs));
        testSession.checkPermission(n.getPath(), SessionImpl.READ_ACTION);
    }

    public void testNonExistingItem() throws RepositoryException {
        /*
          precondition:
          testuser must have READ-only permission on the root node and below
        */
        String rootPath = testSession.getRootNode().getPath();
        checkReadOnly(rootPath);
        testSession.checkPermission(rootPath + "nonExistingItem", SessionImpl.READ_ACTION);
    }

    public void testACItemsAreProtected() throws NotExecutableException, RepositoryException {
        // search for a rep:policy node
        Node policyNode = findPolicyNode(superuser.getRootNode());
        if (policyNode == null) {
            throw new NotExecutableException("no policy node found.");
        }

        assertTrue("The rep:Policy node must be protected", policyNode.getDefinition().isProtected());
        try {
            policyNode.remove();
            fail("rep:Policy node must be protected.");
        } catch (ConstraintViolationException e) {
            // success
        }

        for (NodeIterator it = policyNode.getNodes(); it.hasNext();) {
            Node n = it.nextNode();
            if (n.isNodeType("rep:ACE")) {
                try {
                    n.remove();
                    fail("ACE node must be protected.");
                } catch (ConstraintViolationException e) {
                    // success
                }
                break;
            }
        }

        try {
            policyNode.setProperty("test", "anyvalue");
            fail("rep:policy node must be protected.");
        } catch (ConstraintViolationException e) {
            // success
        }
        try {
            policyNode.addNode("test", "rep:ACE");
            fail("rep:policy node must be protected.");
        } catch (ConstraintViolationException e) {
            // success
        }
    }

    private static Node findPolicyNode(Node start) throws RepositoryException {
        Node policyNode = null;
        if (start.isNodeType("rep:ACL")) {
            policyNode = start;
        }
        for (NodeIterator it = start.getNodes(); it.hasNext() && policyNode == null;) {
            policyNode = findPolicyNode(it.nextNode());
        }
        return policyNode;
    }
}