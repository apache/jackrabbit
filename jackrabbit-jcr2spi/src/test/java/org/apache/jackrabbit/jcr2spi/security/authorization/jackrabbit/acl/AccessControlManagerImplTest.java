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
package org.apache.jackrabbit.jcr2spi.security.authorization.jackrabbit.acl;

import java.security.Principal;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.test.api.security.AbstractAccessControlTest;

public class AccessControlManagerImplTest extends AbstractAccessControlTest {    

    @Override
    public void setUp() throws Exception {
        super.setUp();        
    }
    
    private Principal getUnknownPrincipal() throws NotExecutableException, RepositoryException {
        return getHelper().getUnknownPrincipal(superuser);
    }
    
    public void testGetAndHasPrivileges() throws Exception {
        Privilege[] privileges = acMgr.getPrivileges(testRoot);
        assertNotNull(privileges);
        assertTrue(acMgr.hasPrivileges(testRoot, privileges));
    }
    
    /**
     * Tests the binding state of a policy.
     * @throws Exception
     */
    public void testGetPolicesAfterSetPoliciesCall() throws Exception {
        try {
            AccessControlPolicyIterator policies = acMgr.getApplicablePolicies(testRoot);
            AccessControlPolicy policy = null;
            while (policies.hasNext()) {
                policy = policies.nextAccessControlPolicy();
                acMgr.setPolicy(testRoot, policy);
                AccessControlPolicy[] acl = acMgr.getPolicies(testRoot);
                assertNotNull(acl);
            }
        } finally {
            superuser.refresh(false);
        }        
    }

    /**
     * This should be able to return the policies that has been transiently added
     * to the node at testRoot, as the getPolicies api specifies that the method should
     * take the transient changes into account.
     * @throws Exception
     */
    public void testRemovePolicyAfterASetPoliciesCall() throws Exception {
        try {
            AccessControlPolicyIterator policies = acMgr.getApplicablePolicies(testRoot);
            while (policies.hasNext()) {
                AccessControlList acl = (AccessControlListImpl) policies.nextAccessControlPolicy();
                
                // GRANT read privilege
                acl.addAccessControlEntry(getUnknownPrincipal(), privilegesFromName(Privilege.JCR_READ));
                
                acMgr.setPolicy(testRoot, acl);
                
                AccessControlPolicy[] transientPolicy = acMgr.getPolicies(testRoot);
                
                acMgr.removePolicy(testRoot, transientPolicy[0]);
                
                assertEquals(0, acMgr.getPolicies(testRoot).length);
            }

        } finally {
            superuser.refresh(false);
        }
    }
    
    /**
     * Test removing an effective policy.
     */
    public void testRemovePolicyAfterASaveCall() throws Exception {
        try {
            AccessControlList[] acl = (AccessControlList[]) acMgr.getPolicies(testRoot);
            if (acl.length > 0) {
                acMgr.removePolicy(testRoot, acl[0]);
            } else {                
                AccessControlPolicy policy = acMgr.getApplicablePolicies(testRoot).nextAccessControlPolicy();
                acMgr.setPolicy(testRoot, policy);
                acMgr.removePolicy(testRoot, policy);
            }

            // transient removal           
            AccessControlPolicy[] noPolicies = acMgr.getPolicies(testRoot);            
            assertEquals(0, noPolicies.length);

            // save changes -> removal of protected items on jcr-server
            superuser.save();
        } catch (Exception e) {
            throw new RepositoryException(e.getMessage());
        } finally {
            superuser.refresh(false);
        }
    }
    
    /**
     * JCR mandates that the path specified for getPrivileges method must
     * be absolute and points to an existing node.
     * @throws Exception
     */
    public void testGetPrivilegesOnNonExistingNode() throws Exception {
        try {
            acMgr.getPrivileges(getPathToNonExistingNode());
            fail("Must throw a PathNotFoundException");
        } catch (PathNotFoundException e) {
            // success
        }        
    }

    /**
     * Add an AccessControlList with four entries. This will result in having the result in:
     * Transient-space: An ACL node that has four child-nodes.
     * Persistent-state: An ACL node that has one child-node.
     * NOTE: That Jackrabbit-core tries to internally merge the entries that belongs to the same 
     * principal, which is not the case for the client-side ACM implementation.
     */
    public void testAddingFourAccessControlEntries() throws Exception {
        try {
            AccessControlList acl = (AccessControlList) getACL(testRoot);

            // check precondition,see JCR-3995
            if (testRootNode.hasNode("rep:policy")) {
                assertEquals("should not have any ace nodes at this point", 0,
                        testRootNode.getNode("rep:policy").getNodes().getSize());
            }

            acl.addAccessControlEntry(getUnknownPrincipal(), privilegesFromName(Privilege.JCR_READ));
            acl.addAccessControlEntry(getUnknownPrincipal(), privilegesFromName(Privilege.JCR_READ));
            acl.addAccessControlEntry(getUnknownPrincipal(), privilegesFromName(Privilege.JCR_READ));
            acl.addAccessControlEntry(getUnknownPrincipal(), privilegesFromName(Privilege.JCR_READ));

            acMgr.setPolicy(testRoot, acl);

            // Transient-space: Must contain FOUR ace nodes.
            assertEquals(4, testRootNode.getNode("rep:policy").getNodes().getSize());

            superuser.save();

            // Persistent-state: Must contain a single ace node -> entries were
            // merged
            assertEquals(1, testRootNode.getNode("rep:policy").getNodes().getSize());
        } finally {
            superuser.refresh(false);
        }
    }

    /**
     * Test retrieving a policy after a save call.
     * @throws Exception
     */
    public void testGetPoliciesAfterASaveCall() throws Exception {
        try {
            JackrabbitAccessControlList policy = (JackrabbitAccessControlList) getACL(testRoot);

            String aclPath = policy.getPath();
            assertEquals(aclPath, testRoot);

            // GRANT 'read' privilege to principal
            policy.addAccessControlEntry(getUnknownPrincipal(), privilegesFromName(Privilege.JCR_READ));

            // GRANT 'add_child_nodes' privilege
            policy.addAccessControlEntry(getUnknownPrincipal(), privilegesFromName(Privilege.JCR_ADD_CHILD_NODES));

            // bind the policy and save changes
            acMgr.setPolicy(testRoot, policy);
            superuser.save();

            Node aclNode = testRootNode.getNode("rep:policy");
            assertNotNull(aclNode);
            
            NodeIterator nit = aclNode.getNodes();
            
            // Jackrabbit-core will merge the two entries -> only a single aceNode will be created.
            assertEquals(1, nit.getSize());            
        } finally {
            superuser.refresh(false);
        }
    }
    
    private AccessControlPolicy getACL(String absPath) throws RepositoryException {
        AccessControlList acl = null;
        if (acMgr.getPolicies(absPath).length > 0) {
            acl = (AccessControlList) acMgr.getPolicies(absPath)[0];
        } else {
            acl = (AccessControlList) acMgr.getApplicablePolicies(absPath).nextAccessControlPolicy();
        }
        return acl;
    }

}
