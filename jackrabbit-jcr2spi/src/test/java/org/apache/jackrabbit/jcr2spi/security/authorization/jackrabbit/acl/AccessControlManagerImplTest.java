package org.apache.jackrabbit.jcr2spi.security.authorization.jackrabbit.acl;

import java.security.Principal;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.test.api.security.AbstractAccessControlTest;
import org.junit.Test;

public class AccessControlManagerImplTest extends AbstractAccessControlTest {    
    
    private NamePathResolver npResolver;
    public void setUp() throws Exception {
        super.setUp();        
        npResolver = new DefaultNamePathResolver(getHelper().getSuperuserSession());
    }
    
    private Principal getUnknownPrincipal() throws NotExecutableException, RepositoryException {
        return getHelper().getUnknownPrincipal(superuser);
    }
    
    @Test
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
                acl.addAccessControlEntry(getUnknownPrincipal(), privilegesFromName(npResolver.getJCRName(NameConstants.JCR_READ)));
                
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
    @Test
    public void testAddingFourAccessControlEntries() throws Exception {
        try {
            AccessControlList acl = (AccessControlList) getACL(testRoot);        
            
            acl.addAccessControlEntry(getUnknownPrincipal(), privilegesFromName(npResolver.getJCRName(NameConstants.JCR_READ)));
            acl.addAccessControlEntry(getUnknownPrincipal(), privilegesFromName(npResolver.getJCRName(NameConstants.JCR_READ)));
            acl.addAccessControlEntry(getUnknownPrincipal(), privilegesFromName(npResolver.getJCRName(NameConstants.JCR_READ)));
            acl.addAccessControlEntry(getUnknownPrincipal(), privilegesFromName(npResolver.getJCRName(NameConstants.JCR_READ)));
            
            acMgr.setPolicy(testRoot, acl);

            // Transient-space: Must contain FOUR ace nodes.
            assertEquals(4, testRootNode.getNode("rep:policy").getNodes().getSize());
            
            superuser.save();
            
            // Persistent-state: Must contain a single ace node -> entries were merged
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
            policy.addAccessControlEntry(getUnknownPrincipal(), privilegesFromName(npResolver.getJCRName(NameConstants.JCR_READ)));

            // GRANT 'add_child_nodes' privilege
            policy.addAccessControlEntry(getUnknownPrincipal(), privilegesFromName(npResolver.getJCRName(NameConstants.JCR_ADD_CHILD_NODES)));

            assertEquals(2, policy.getAccessControlEntries().length);

            // bind the policy and save changes
            acMgr.setPolicy(testRoot, policy);
            superuser.save();

            Node aclNode = testRootNode.getNode("rep:policy");
            assertNotNull(aclNode);
            
            NodeIterator nit = aclNode.getNodes();
            
            // Jackrabbit-core will match the two entries -> only a single aceNode will be created.
            assertEquals(1, nit.getSize());            
        } finally {
            superuser.refresh(false);
        }
    }
    
    private AccessControlPolicy getACL(String absPath) throws RepositoryException {
        AccessControlList acl = null;
        if (acMgr.getPolicies(testRoot).length > 0) {
            acl = (AccessControlList) acMgr.getPolicies(testRoot)[0];
        } else {
            acl = (AccessControlList) acMgr.getApplicablePolicies(testRoot).nextAccessControlPolicy();
        }
        return acl;
    }

}
