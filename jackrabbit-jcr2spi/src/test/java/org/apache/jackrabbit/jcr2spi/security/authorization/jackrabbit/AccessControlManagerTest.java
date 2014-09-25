package org.apache.jackrabbit.jcr2spi.security.authorization.jackrabbit;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.jcr2spi.security.authorization.jackrabbit.acl.AccessControlListImpl;
import org.junit.Test;

public class AccessControlManagerTest extends AbstractAccessControlTest {

    
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testSupportedPrivileges() throws Exception {               
        Privilege[] privs = getACManager().getSupportedPrivileges(path);
        assertTrue(privs.length > 0);
    }
    
    protected String getTestPath() {
        return path;
    }
    
    /**
     * Tests the binding state of a policy.
     * @throws Exception
     */
    @Test
    public void testGetPolicesAfterASetPoliciesCall() throws Exception {
        AccessControlPolicyIterator policies = getACManager().getApplicablePolicies(path);
        AccessControlPolicy policy = null;
        while (policies.hasNext()) {
            policy = policies.nextAccessControlPolicy();
            getACManager().setPolicy(path, policy);
            AccessControlPolicy[] acl = getACManager().getPolicies(path);
            assertNotNull(acl);
        }
    }

    /**
     * This should be able to return the policies that has been transiently added
     * to the node at path, as the getPolicies api specifies that the method should
     * take the transient changes into account.
     * @throws Exception
     */
    @Test
    public void testRemovePolicyAfterASetPoliciesCall() throws Exception {
        try {
            AccessControlPolicyIterator policies = getACManager().getApplicablePolicies(path);
            AccessControlPolicy policy = policies.nextAccessControlPolicy();
            
            AccessControlListImpl acl = (AccessControlListImpl) policy;
            
            // GRANT read privilege
            acl.addAccessControlEntry(getUnknownPrincipal(), getPrivileges(0));
            
            getACManager().setPolicy(path, policy);
            
            AccessControlPolicy[] unsavePolicy = getACManager().getPolicies(path);
            
            // MUST be able to get policies that are not ineffect for the node at 'path'
            assertFalse(unsavePolicy.length == 0);

        } finally {
            getSession().refresh(false);
        }
    }
    
    /**
     * test removing an effective policy.
     */
    @Test
    public void testRemovePolicyAfterASaveCall() {
        // TODO
    }
    
    @Test
    public void testGetPrivilegesOnNonExistingNode() {
        // TODO
    }
    
    /**
     * Test retrieving a policy after a save call.
     * @throws Exception
     */
    @Test
    public void testGetPoliciesAfterASaveCall() throws Exception {
        
        try {
            
            AccessControlPolicyIterator pi = getACManager().getApplicablePolicies(path);
            assertTrue(pi.hasNext());
            AccessControlListImpl policy = (AccessControlListImpl) pi.nextAccessControlPolicy();
             
            String aclPath = policy.getPath();                              
            assertEquals(aclPath, path);

            // GRANT 'read' privilege to principal
            policy.addAccessControlEntry(getUnknownPrincipal(), getPrivileges(0));
            
            // GRANT 'add_child_nodes' privilege
            policy.addAccessControlEntry(getUnknownPrincipal(), getPrivileges(1));

            assertEquals(1, policy.getAccessControlEntries().length);
            
            // bind the policy and save changes
            getACManager().setPolicy(path, policy);
            getSession().save();

            Node aclNode = node.getNode("rep:policy");
            assertNotNull(aclNode);
            
            // only a single ACE node should be created by the manager
            NodeIterator nit = aclNode.getNodes();
            assertEquals(1, nit.getSize());
            
            AccessControlPolicy[] policies = getACManager().getPolicies(path);
            
            // A single policy node at 'path'
            assertEquals(1, policies.length);
            
            policy = (AccessControlListImpl) policies[0];
            
            //... and the policy contains a single entry.
            assertEquals(1, policy.getAccessControlEntries().length);
            
            // revoke the read privilege
            policy.addEntry(getUnknownPrincipal(), getPrivileges(0), false);
            
            assertEquals(2, policy.getAccessControlEntries().length);
            
            // principal is now only GRANTED the 'addChildNodes' privilege
            assertEquals(getPrivileges(1)[0].getName(), policy.getAccessControlEntries()[0].getPrivileges()[0].getName());
            
            //... and he is revoked the 'read' privilege
            assertEquals(getPrivileges(0)[0].getName(), policy.getAccessControlEntries()[1].getPrivileges()[0].getName());
            
        } catch (RepositoryException e) {
            throw new Exception(e.getMessage());
        } finally {
            getSession().refresh(false);
        }
    }
    
    @Test
    public void testEntriesWithRestrictions() throws Exception {
        // Use policy = getApplicablePolicies()
        // create a restriction 3 different restrictions
        // create an entry and add the restrictions.
    }
    
}