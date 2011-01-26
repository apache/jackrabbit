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

import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.core.security.authorization.AbstractEvaluationTest;
import org.apache.jackrabbit.core.security.authorization.AccessControlConstants;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * <code>ACWriteTest</code>...
 */
public class AcReadWriteTest extends AbstractEvaluationTest {

    protected String path;
    protected String childNPath;
    protected String childNPath2;
    protected String childPPath;
    protected String childchildPPath;
    protected String siblingPath;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

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
    
    @Override
    protected boolean isExecutable() {
        return EvaluationUtil.isExecutable(acMgr);
    }

    @Override
    protected JackrabbitAccessControlList getPolicy(AccessControlManager acM, String path, Principal principal) throws RepositoryException, AccessDeniedException, NotExecutableException {
        return EvaluationUtil.getPolicy(acM, path, principal);
    }

    @Override
    protected Map<String, Value> getRestrictions(Session s, String path) {
        return Collections.emptyMap();
    }


    public void testAccessControlPrivileges() throws RepositoryException, NotExecutableException {
        /* precondition:
          testuser must have READ-only permission on test-node and below
        */
        checkReadOnly(path);

        /* grant 'testUser' rep:write, rep:readAccessControl and
           rep:modifyAccessControl privileges at 'path' */
        Privilege[] privileges = privilegesFromNames(new String[] {
                PrivilegeRegistry.REP_WRITE,
                Privilege.JCR_READ_ACCESS_CONTROL,
                Privilege.JCR_MODIFY_ACCESS_CONTROL
        });
        JackrabbitAccessControlList tmpl = givePrivileges(path, privileges, getRestrictions(superuser, path));

        Session testSession = getTestSession();
        AccessControlManager testAcMgr = getTestACManager();
        /*
         testuser must have
         - permission to view AC items
         - permission to modify AC items
        */
        // the policy node however must be visible to the test-user
        assertTrue(testSession.itemExists(tmpl.getPath() + "/rep:policy"));

        testAcMgr.getPolicies(tmpl.getPath());
        testAcMgr.removePolicy(tmpl.getPath(), tmpl);
    }

    /**
     * Test if a new applicable policy can be applied within a individual
     * subtree where AC-modification is allowed.
     * 
     * @throws RepositoryException
     * @throws NotExecutableException
     * @see <a href="https://issues.apache.org/jira/browse/JCR-2869">JCR-2869</a>
     */
    public void testSetNewPolicy() throws RepositoryException, NotExecutableException {
        /* precondition:
          testuser must have READ-only permission on test-node and below
        */
        checkReadOnly(path);

        /* grant 'testUser' rep:write, rep:readAccessControl and
           rep:modifyAccessControl privileges at 'path' */
        Privilege[] privileges = privilegesFromNames(new String[] {
                PrivilegeRegistry.REP_WRITE,
                Privilege.JCR_READ_ACCESS_CONTROL,
                Privilege.JCR_MODIFY_ACCESS_CONTROL
        });
        JackrabbitAccessControlList tmpl = givePrivileges(path, privileges, getRestrictions(superuser, path));

        AccessControlManager testAcMgr = getTestACManager();
        /*
         testuser must be allowed to set a new policy at a child node.
        */
        AccessControlPolicyIterator it = testAcMgr.getApplicablePolicies(childNPath);
        while (it.hasNext()) {
            AccessControlPolicy plc = it.nextAccessControlPolicy();
            testAcMgr.setPolicy(childNPath, plc);
            testAcMgr.removePolicy(childNPath, plc);
        }
    }

    public void testSetModifiedPolicy() throws RepositoryException, NotExecutableException {
        /* precondition:
          testuser must have READ-only permission on test-node and below
        */
        checkReadOnly(path);

        /* grant 'testUser' rep:write, rep:readAccessControl and
           rep:modifyAccessControl privileges at 'path' */
        Privilege[] privileges = privilegesFromNames(new String[] {
                PrivilegeRegistry.REP_WRITE,
                Privilege.JCR_READ_ACCESS_CONTROL,
                Privilege.JCR_MODIFY_ACCESS_CONTROL
        });
        JackrabbitAccessControlList tmpl = givePrivileges(path, privileges, getRestrictions(superuser, path));

        /*
         testuser must be allowed to set (modified) policy at target node.
        */
        Session testSession = getTestSession();
        AccessControlManager testAcMgr = getTestACManager();

        AccessControlPolicy[] policies  = testAcMgr.getPolicies(path);

        assertEquals(1, policies.length);
        assertTrue(policies[0] instanceof AccessControlList);

        AccessControlList acl = (AccessControlList) policies[0];
        if (acl.addAccessControlEntry(testUser.getPrincipal(), new Privilege[] {testAcMgr.privilegeFromName(Privilege.JCR_LOCK_MANAGEMENT)})) {
            testAcMgr.setPolicy(path, acl);
            testSession.save();
        }
    }

    public void testRetrievePrivilegesOnAcNodes() throws NotExecutableException, RepositoryException {
        /* precondition:
          testuser must have READ-only permission on test-node and below
        */
        checkReadOnly(path);

        // give 'testUser' jcr:readAccessControl privileges at 'path'
        Privilege[] privileges = privilegesFromNames(new String[] {
                Privilege.JCR_READ_ACCESS_CONTROL
        });
        JackrabbitAccessControlList tmpl = givePrivileges(path, privileges, getRestrictions(superuser, path));

        /*
         testuser must be allowed to read ac-content at target node.
        */
        Session testSession = getTestSession();
        AccessControlManager testAcMgr = getTestACManager();

        assertTrue(testAcMgr.hasPrivileges(path, privileges));

        AccessControlPolicy[] policies  = testAcMgr.getPolicies(path);
        assertEquals(1, policies.length);
        assertTrue(policies[0] instanceof JackrabbitAccessControlList);

        String aclNodePath = null;
        Node n = superuser.getNode(path);
        for (NodeIterator itr = n.getNodes(); itr.hasNext();) {
            Node child = itr.nextNode();
            if (child.isNodeType("rep:Policy")) {
                aclNodePath = child.getPath();
            }
        }

        if (aclNodePath == null) {
            fail("Expected node at " + path + " to have an ACL child node.");
        }

        assertTrue(testAcMgr.hasPrivileges(aclNodePath, privileges));
        assertTrue(testSession.hasPermission(aclNodePath, Session.ACTION_READ));

        for (NodeIterator aceNodes = superuser.getNode(aclNodePath).getNodes(); aceNodes.hasNext();) {
            String aceNodePath = aceNodes.nextNode().getPath();
            assertTrue(testAcMgr.hasPrivileges(aceNodePath, privileges));
            assertTrue(testSession.hasPermission(aceNodePath, Session.ACTION_READ));
        }
    }

        public void testReadAccessControl() throws NotExecutableException, RepositoryException {
        /* precondition:
          testuser must have READ-only permission on test-node and below
        */
        checkReadOnly(path);

        /* give 'testUser' jcr:readAccessControl privileges at subtree below
           path excluding the node at path itself. */
        Privilege[] privileges = privilegesFromNames(new String[] {
                Privilege.JCR_READ_ACCESS_CONTROL
        });
        Map<String, Value> restrictions = new HashMap<String, Value>(getRestrictions(superuser, path));
        restrictions.put(AccessControlConstants.P_GLOB.toString(), vf.createValue("/" + nodeName2));
        JackrabbitAccessControlList tmpl = givePrivileges(path, privileges, restrictions);

        /*
         testuser must not be allowed to read AC content at the target node;
         however, retrieving potential AC content at 'childPath' is granted.
        */
        Session testSession = getTestSession();
        AccessControlManager testAcMgr = getTestACManager();

        assertFalse(testAcMgr.hasPrivileges(path, privileges));
        try {
            testAcMgr.getPolicies(path);
            fail("AccessDeniedException expected");
        } catch (AccessDeniedException e) {
            // success.
        }

        assertTrue(testAcMgr.hasPrivileges(childNPath, privileges));
        assertEquals(0, testAcMgr.getPolicies(childNPath).length);

        /* similarly reading the corresponding AC items at 'path' must be forbidden */    
        String aclNodePath = null;
        Node n = superuser.getNode(path);
        for (NodeIterator itr = n.getNodes(); itr.hasNext();) {
            Node child = itr.nextNode();
            if (child.isNodeType("rep:Policy")) {
                aclNodePath = child.getPath();
            }
        }
        if (aclNodePath == null) {
            fail("Expected node at " + path + " to have an ACL child node.");
        }

        assertFalse(testSession.nodeExists(aclNodePath));

        for (NodeIterator aceNodes = superuser.getNode(aclNodePath).getNodes(); aceNodes.hasNext();) {
            Node aceNode = aceNodes.nextNode();
            String aceNodePath = aceNode.getPath();
            assertFalse(testSession.nodeExists(aceNodePath));

            for (PropertyIterator it = aceNode.getProperties(); it.hasNext();) {
                assertFalse(testSession.propertyExists(it.nextProperty().getPath()));
            }
        }
    }
}