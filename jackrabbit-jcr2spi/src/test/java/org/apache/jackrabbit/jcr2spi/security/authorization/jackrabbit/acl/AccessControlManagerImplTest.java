package org.apache.jackrabbit.jcr2spi.security.authorization.jackrabbit.acl;

import java.security.Principal;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.test.api.security.AbstractAccessControlTest;

public class AccessControlManagerImplTest extends AbstractAccessControlTest {
        
    private Principal getUnknownPrincipal() throws NotExecutableException, RepositoryException {
        return getHelper().getUnknownPrincipal(superuser);
    }
    
    /**
     * Tests the binding state of a policy.
     * @throws Exception
     */
    public void testGetPolicesAfterSetPoliciesCall() throws Exception {
        AccessControlPolicyIterator policies = acMgr.getApplicablePolicies(testRoot);
        AccessControlPolicy policy = null;
        while (policies.hasNext()) {
            policy = policies.nextAccessControlPolicy();
            acMgr.setPolicy(testRoot, policy);
            AccessControlPolicy[] acl = acMgr.getPolicies(testRoot);
            assertNotNull(acl);
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
            AccessControlPolicy policy = policies.nextAccessControlPolicy();
            
            AccessControlListImpl acl = (AccessControlListImpl) policy;
            
            // GRANT read privilege
            acl.addAccessControlEntry(getUnknownPrincipal(), privilegesFromName(Privilege.JCR_READ));
            
            acMgr.setPolicy(testRoot, policy);
            
            AccessControlPolicy[] unsavePolicy = acMgr.getPolicies(testRoot);
            
            // MUST be able to get policies that are not ineffect for the node at 'testRoot'
            assertFalse(unsavePolicy.length == 0);

        } finally {
            superuser.refresh(false);
        }
    }
    
    /**
     * test removing an effective policy.
     */
    public void testRemovePolicyAfterASaveCall() {
        // TODO
    }
    
    public void testGetPrivilegesOnNonExistingNode() {
        // TODO
    }

    /**
     * Test retrieving a policy after a save call.
     * @throws Exception
     */
    public void testGetPoliciesAfterASaveCall() throws Exception {
        AccessControlPolicyIterator pi = acMgr.getApplicablePolicies(testRoot);
        assertTrue(pi.hasNext());
        AccessControlListImpl policy = (AccessControlListImpl) pi.nextAccessControlPolicy();

        String aclPath = policy.getPath();
        assertEquals(aclPath, testRoot);

        // GRANT 'read' privilege to principal
        policy.addAccessControlEntry(getUnknownPrincipal(), privilegesFromName(Privilege.JCR_READ));

        // GRANT 'add_child_nodes' privilege
        policy.addAccessControlEntry(getUnknownPrincipal(), privilegesFromName(Privilege.JCR_ADD_CHILD_NODES));

        assertEquals(1, policy.getAccessControlEntries().length);

        // bind the policy and save changes
        acMgr.setPolicy(testRoot, policy);
        superuser.save();

        Node aclNode = testRootNode.getNode("rep:policy");
        assertNotNull(aclNode);

        // only a single ACE node should be created by the manager
        NodeIterator nit = aclNode.getNodes();
        assertEquals(2, nit.getSize());

        AccessControlPolicy[] policies = acMgr.getPolicies(testRoot);

        // A single policy node at 'testRoot'
        assertEquals(1, policies.length);

        policy = (AccessControlListImpl) policies[0];

        //... and the policy contains 2 entries.
        assertEquals(2, policy.getAccessControlEntries().length);

        // revoke the read privilege
        policy.addEntry(getUnknownPrincipal(), privilegesFromName(Privilege.JCR_READ), false);

        assertEquals(3, policy.getAccessControlEntries().length);
    }

}
