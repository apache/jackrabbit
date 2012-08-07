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

import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlManager;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlPolicy;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * <code>AbstractRepositoryOperationTest</code>...
 */
public abstract class AbstractRepositoryOperationTest extends AbstractEvaluationTest {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            for (AccessControlPolicy policy : acMgr.getPolicies(null)) {
                acMgr.removePolicy(null, policy);
            }
            superuser.save();
        } finally {
            super.tearDown();
        }
    }

    private Workspace getTestWorkspace() throws RepositoryException {
        return getTestSession().getWorkspace();
    }

    private void assertDefaultPrivileges(Name privName) throws Exception {
        Privilege[] privs = privilegesFromName(privName.toString());
        // admin must be allowed
        assertTrue(superuser.getAccessControlManager().hasPrivileges(null, privs));
        // test user must not be allowed
        assertFalse(getTestACManager().hasPrivileges(null, privs));
    }

    private void assertPrivilege(Name privName, boolean isAllow) throws Exception {
        Privilege[] privs = privilegesFromName(privName.toString());
        assertEquals(isAllow, getTestACManager().hasPrivileges(null, privs));
    }

    private void assertPermission(int permission, boolean isAllow) throws Exception {
        AccessManager acMgr = ((SessionImpl) getTestSession()).getAccessManager();
        try {
            acMgr.checkRepositoryPermission(permission);
            if (!isAllow) {
                fail();
            }
        } catch (AccessDeniedException e) {
            if (isAllow) {
                fail();
            }
        }
    }

    private String getNewWorkspaceName(Workspace wsp) throws RepositoryException {
        List<String> awn = Arrays.asList(wsp.getAccessibleWorkspaceNames());
        String workspaceName = "new";
        int i = 0;
        while (awn.contains(workspaceName)) {
            workspaceName =  "new_" + i++;
        }
        return workspaceName;
    }

    private String getNewNamespacePrefix(Workspace wsp) throws RepositoryException {
        String prefix = "prefix";
        List<String> pfcs = Arrays.asList(wsp.getNamespaceRegistry().getPrefixes());
        int i = 0;
        while (pfcs.contains(prefix)) {
            prefix = "prefix" + i++;
        }
        return prefix;
    }

    private String getNewNamespaceURI(Workspace wsp) throws RepositoryException {
        String uri = "http://jackrabbit.apache.org/uri";
        List<String> uris = Arrays.asList(wsp.getNamespaceRegistry().getURIs());
        int i = 0;
        while (uris.contains(uri)) {
            uri = "http://jackrabbit.apache.org/uri_" + i++;
        }
        return uri;
    }

    private String getNewPrivilegeName(Workspace wsp) throws RepositoryException, NotExecutableException {
        String privName = null;
        AccessControlManager acMgr = wsp.getSession().getAccessControlManager();
        for (int i = 0; i < 100; i++) {
            try {
                Privilege p = acMgr.privilegeFromName(privName);
                privName = "privilege-" + i;
            } catch (Exception e) {
                break;
            }
        }

        if (privName == null) {
            throw new NotExecutableException("failed to define new privilege name.");
        }
        return privName;
    }

    public void testWorkspaceCreation() throws Exception {
        assertDefaultPrivileges(NameConstants.JCR_WORKSPACE_MANAGEMENT);

        String wspName = getNewWorkspaceName(superuser.getWorkspace());
        try {
            getTestWorkspace().createWorkspace(wspName);
            fail("Workspace creation should be denied.");
        } catch (AccessDeniedException e) {
            // success
        }

        wspName = getNewWorkspaceName(superuser.getWorkspace());
        try {
            Workspace wsp = getTestWorkspace();
            wsp.createWorkspace(wspName, wsp.getName());
            fail("Workspace creation should be denied.");
        } catch (AccessDeniedException e) {
            // success
        }
    }

    public void testWorkspaceCreationWithPrivilege() throws Exception {
        assertDefaultPrivileges(NameConstants.JCR_WORKSPACE_MANAGEMENT);
        assertPermission(Permission.WORKSPACE_MNGMT, false);

        modifyPrivileges(null, NameConstants.JCR_WORKSPACE_MANAGEMENT.toString(), true);
        // assert that permission have changed:
        assertPrivilege(NameConstants.JCR_WORKSPACE_MANAGEMENT, true);
        assertPermission(Permission.WORKSPACE_MNGMT, true);

        try {
            Workspace testWsp = getTestWorkspace();
            testWsp.createWorkspace(getNewWorkspaceName(superuser.getWorkspace()));
        } finally {
            modifyPrivileges(null, NameConstants.JCR_WORKSPACE_MANAGEMENT.toString(), false);
        }

        assertPrivilege(NameConstants.JCR_WORKSPACE_MANAGEMENT, false);
        assertPermission(Permission.WORKSPACE_MNGMT, false);
    }

    public void testWorkspaceDeletion() throws Exception {
        assertDefaultPrivileges(NameConstants.JCR_WORKSPACE_MANAGEMENT);
        assertPermission(Permission.WORKSPACE_MNGMT, false);

        Workspace wsp = superuser.getWorkspace();
        String workspaceName = getNewWorkspaceName(wsp);

        wsp.createWorkspace(workspaceName);
        try {
            Workspace testWsp = getTestWorkspace();
            List<String> wspNames = Arrays.asList(testWsp.getAccessibleWorkspaceNames());
            if (wspNames.contains(workspaceName)) {
                testWsp.deleteWorkspace(workspaceName);
                fail("Workspace deletion should be denied.");
            }
        } catch (AccessDeniedException e) {
            // success
        } finally {
            // clean up (not supported by jackrabbit-core)
            try {
                superuser.getWorkspace().deleteWorkspace(workspaceName);
            } catch (Exception e) {
                // workspace removal is not supported by jackrabbit-core.
            }
        }
    }

    public void testRegisterNodeType() throws Exception {
        assertDefaultPrivileges(NameConstants.JCR_NODE_TYPE_DEFINITION_MANAGEMENT);
        assertPermission(Permission.NODE_TYPE_DEF_MNGMT, false);

        Workspace testWsp = getTestWorkspace();
        NodeTypeManager ntm = testWsp.getNodeTypeManager();
        NodeTypeTemplate ntd = ntm.createNodeTypeTemplate();
        ntd.setName("testNodeType");
        ntd.setMixin(true);

        try {
            ntm.registerNodeType(ntd, true);
            fail("Node type registration should be denied.");
        } catch (AccessDeniedException e) {
            // success
        }
        try {
            ntm.registerNodeType(ntd, false);
            fail("Node type registration should be denied.");
        } catch (AccessDeniedException e) {
            // success
        }

        NodeTypeTemplate[] ntds = new NodeTypeTemplate[2];
        ntds[0] = ntd;
        ntds[1] = ntm.createNodeTypeTemplate();
        ntds[1].setName("anotherNodeType");
        ntds[1].setDeclaredSuperTypeNames(new String[] {"nt:file"});
        try {
            ntm.registerNodeTypes(ntds, true);
            fail("Node type registration should be denied.");
        } catch (AccessDeniedException e) {
            // success
        }

        try {
            ntm.registerNodeTypes(ntds, false);
            fail("Node type registration should be denied.");
        } catch (AccessDeniedException e) {
            // success
        }
    }

    public void testRegisterNodeTypeWithPrivilege() throws Exception {
        assertDefaultPrivileges(NameConstants.JCR_NODE_TYPE_DEFINITION_MANAGEMENT);
        assertPermission(Permission.NODE_TYPE_DEF_MNGMT, false);

        modifyPrivileges(null, NameConstants.JCR_NODE_TYPE_DEFINITION_MANAGEMENT.toString(), true);
        assertPrivilege(NameConstants.JCR_NODE_TYPE_DEFINITION_MANAGEMENT, true);
        assertPermission(Permission.NODE_TYPE_DEF_MNGMT, true);

        try {
            Workspace testWsp = getTestWorkspace();
            NodeTypeManager ntm = testWsp.getNodeTypeManager();
            NodeTypeTemplate ntd = ntm.createNodeTypeTemplate();
            ntd.setName("testNodeType");
            ntd.setMixin(true);
            ntm.registerNodeType(ntd, true);

            NodeTypeTemplate[] ntds = new NodeTypeTemplate[2];
            ntds[0] = ntd;
            ntds[1] = ntm.createNodeTypeTemplate();
            ntds[1].setName("anotherNodeType");
            ntds[1].setDeclaredSuperTypeNames(new String[] {"nt:file"});
            ntm.registerNodeTypes(ntds, true);
        } finally {
            modifyPrivileges(null, NameConstants.JCR_NODE_TYPE_DEFINITION_MANAGEMENT.toString(), false);
        }

        assertPrivilege(NameConstants.JCR_NODE_TYPE_DEFINITION_MANAGEMENT, false);
        assertPermission(Permission.NODE_TYPE_DEF_MNGMT, false);
    }

    public void testUnRegisterNodeType() throws Exception {
        assertDefaultPrivileges(NameConstants.JCR_NODE_TYPE_DEFINITION_MANAGEMENT);
        assertPermission(Permission.NODE_TYPE_DEF_MNGMT, false);

        NodeTypeManager ntm = superuser.getWorkspace().getNodeTypeManager();
        NodeTypeTemplate ntd = ntm.createNodeTypeTemplate();
        ntd.setName("testNodeType");
        ntd.setMixin(true);
        ntm.registerNodeType(ntd, true);

        Workspace testWsp = getTestWorkspace();
        try {
            try {
                NodeTypeManager testNtm = testWsp.getNodeTypeManager();
                testNtm.unregisterNodeType(ntd.getName());
                fail("Namespace unregistration should be denied.");
            } catch (AccessDeniedException e) {
                // success
            }
            try {
                NodeTypeManager testNtm = testWsp.getNodeTypeManager();
                testNtm.unregisterNodeTypes(new String[] {ntd.getName()});
                fail("Namespace unregistration should be denied.");
            } catch (AccessDeniedException e) {
                // success
            }
        } finally {
            // clean up (not supported by jackrabbit-core)
            try {
                ntm.unregisterNodeType(ntd.getName());
            } catch (Exception e) {
                // ns unregistration is not supported by jackrabbit-core.
            }
        }

    }

    public void testRegisterNamespace() throws Exception {
        assertDefaultPrivileges(NameConstants.JCR_NAMESPACE_MANAGEMENT);
        assertPermission(Permission.NODE_TYPE_DEF_MNGMT, false);

        try {
            Workspace testWsp = getTestWorkspace();
            testWsp.getNamespaceRegistry().registerNamespace(getNewNamespacePrefix(testWsp), getNewNamespaceURI(testWsp));
            fail("Namespace registration should be denied.");
        } catch (AccessDeniedException e) {
            // success
        }
    }

    public void testRegisterNamespaceWithPrivilege() throws Exception {
        assertDefaultPrivileges(NameConstants.JCR_NAMESPACE_MANAGEMENT);
        assertPermission(Permission.NAMESPACE_MNGMT, false);

        modifyPrivileges(null, NameConstants.JCR_NAMESPACE_MANAGEMENT.toString(), true);
        assertPrivilege(NameConstants.JCR_NAMESPACE_MANAGEMENT, true);
        assertPermission(Permission.NAMESPACE_MNGMT, true);

        try {
            Workspace testWsp = getTestWorkspace();
            testWsp.getNamespaceRegistry().registerNamespace(getNewNamespacePrefix(testWsp), getNewNamespaceURI(testWsp));
        } finally {
            modifyPrivileges(null, NameConstants.JCR_NAMESPACE_MANAGEMENT.toString(), false);
        }

        assertPrivilege(NameConstants.JCR_NAMESPACE_MANAGEMENT, false);
        assertPermission(Permission.NAMESPACE_MNGMT, false);
    }

    public void testUnregisterNamespace() throws Exception {
        assertDefaultPrivileges(NameConstants.JCR_NAMESPACE_MANAGEMENT);
        assertPermission(Permission.NAMESPACE_MNGMT, false);

        Workspace wsp = superuser.getWorkspace();
        String pfx = getNewNamespacePrefix(wsp);
        wsp.getNamespaceRegistry().registerNamespace(pfx, getNewNamespaceURI(wsp));

        try {
            Workspace testWsp = getTestWorkspace();
            testWsp.getNamespaceRegistry().unregisterNamespace(pfx);
            fail("Namespace unregistration should be denied.");
        } catch (AccessDeniedException e) {
            // success
        } finally {
            // clean up (not supported by jackrabbit-core)
            try {
                superuser.getWorkspace().getNamespaceRegistry().unregisterNamespace(pfx);
            } catch (Exception e) {
                // ns unregistration is not supported by jackrabbit-core.
            }
        }
    }

    public void testRegisterPrivilege() throws Exception {
        assertDefaultPrivileges(PrivilegeRegistry.REP_PRIVILEGE_MANAGEMENT_NAME);
        assertPermission(Permission.PRIVILEGE_MNGMT, false);

        try {
            Workspace testWsp = getTestWorkspace();
            ((JackrabbitWorkspace) testWsp).getPrivilegeManager().registerPrivilege(getNewPrivilegeName(testWsp), false, new String[0]);
            fail("Privilege registration should be denied.");
        } catch (AccessDeniedException e) {
            // success
        }
    }

    public void testRegisterPrivilegeWithPrivilege() throws Exception {
        assertDefaultPrivileges(PrivilegeRegistry.REP_PRIVILEGE_MANAGEMENT_NAME);
        assertPermission(Permission.PRIVILEGE_MNGMT, false);

        modifyPrivileges(null, PrivilegeRegistry.REP_PRIVILEGE_MANAGEMENT_NAME.toString(), true);
        assertPrivilege(PrivilegeRegistry.REP_PRIVILEGE_MANAGEMENT_NAME, true);
        assertPermission(Permission.PRIVILEGE_MNGMT, true);

        try {
            Workspace testWsp = getTestWorkspace();
            ((JackrabbitWorkspace) testWsp).getPrivilegeManager().registerPrivilege(getNewPrivilegeName(testWsp), false, new String[0]);        } finally {
            modifyPrivileges(null, PrivilegeRegistry.REP_PRIVILEGE_MANAGEMENT_NAME.toString(), false);
        }

        assertPrivilege(PrivilegeRegistry.REP_PRIVILEGE_MANAGEMENT_NAME, false);
        assertPermission(Permission.PRIVILEGE_MNGMT, false);
    }

    public void testRepoPolicyAPI() throws Exception {
        try {
            // initial state: no repo level policy
            AccessControlPolicy[] policies = acMgr.getPolicies(null);
            assertNotNull(policies);
            assertEquals(0, policies.length);

            AccessControlPolicy[] effective = acMgr.getEffectivePolicies(null);
            assertNotNull(effective);
            assertEquals(0, effective.length);

            AccessControlPolicyIterator it = acMgr.getApplicablePolicies(null);
            assertNotNull(it);
            assertTrue(it.hasNext());
            AccessControlPolicy acp = it.nextAccessControlPolicy();
            assertNotNull(acp);
            assertTrue(acp instanceof JackrabbitAccessControlPolicy);

            // modify the repo level policy
            modifyPrivileges(null, NameConstants.JCR_NODE_TYPE_DEFINITION_MANAGEMENT.toString(), false);
            modifyPrivileges(null, NameConstants.JCR_NAMESPACE_MANAGEMENT.toString(), true);

            AccessControlPolicy[] plcs = acMgr.getPolicies(null);
            assertNotNull(plcs);
            assertEquals(1, plcs.length);
            assertTrue(plcs[0] instanceof AccessControlList);

            AccessControlList acl = (AccessControlList) plcs[0];
            AccessControlEntry[] aces = acl.getAccessControlEntries();
            assertNotNull(aces);
            assertEquals(2, aces.length);

            assertPrivilege(NameConstants.JCR_NAMESPACE_MANAGEMENT, true);
            assertPermission(Permission.NAMESPACE_MNGMT, true);

            assertPrivilege(NameConstants.JCR_NODE_TYPE_DEFINITION_MANAGEMENT, false);
            assertPermission(Permission.NODE_TYPE_DEF_MNGMT, false);

            effective = acMgr.getEffectivePolicies(null);
            assertNotNull(effective);
            assertEquals(1, effective.length);
            assertTrue(effective[0] instanceof AccessControlList);

            acl = (AccessControlList) effective[0];
            aces = acl.getAccessControlEntries();
            assertNotNull(aces);
            assertEquals(2, aces.length);

            // change the policy: removing the second entry in the access control list
            acl = (AccessControlList) acMgr.getPolicies(null)[0];
            AccessControlEntry toRemove = acl.getAccessControlEntries()[1];
            acl.removeAccessControlEntry(toRemove);
            acMgr.setPolicy(null, acl);
            superuser.save();

            acl = (AccessControlList) acMgr.getPolicies(null)[0];
            aces = acl.getAccessControlEntries();
            assertNotNull(aces);
            assertEquals(1, aces.length);

            assertPrivilege(NameConstants.JCR_NAMESPACE_MANAGEMENT, false);
            assertPermission(Permission.NAMESPACE_MNGMT, false);

            assertPrivilege(NameConstants.JCR_NODE_TYPE_DEFINITION_MANAGEMENT, false);
            assertPermission(Permission.NODE_TYPE_DEF_MNGMT, false);

        } catch (UnsupportedRepositoryOperationException e) {
            throw new NotExecutableException();
        } finally {
            // remove it again
            for (AccessControlPolicy plc : acMgr.getPolicies(null)) {
                acMgr.removePolicy(null, plc);
            }
            superuser.save();

            // back to initial state: no repo level policy
            AccessControlPolicy[] policies = acMgr.getPolicies(null);
            assertNotNull(policies);
            assertEquals(0, policies.length);

            AccessControlPolicy[] effective = acMgr.getEffectivePolicies(null);
            assertNotNull(effective);
            assertEquals(0, effective.length);

            AccessControlPolicyIterator it = acMgr.getApplicablePolicies(null);
            assertNotNull(it);
            assertTrue(it.hasNext());
            AccessControlPolicy acp = it.nextAccessControlPolicy();
            assertNotNull(acp);
            assertTrue(acp instanceof JackrabbitAccessControlPolicy);
        }
    }

    public void testGetEffectivePoliciesByPrincipal() throws Exception {
        if (!(acMgr instanceof JackrabbitAccessControlManager)) {
            throw new NotExecutableException();
        }
        JackrabbitAccessControlManager jAcMgr = (JackrabbitAccessControlManager) acMgr;
        Set<Principal> principalSet = Collections.singleton(testUser.getPrincipal());

        try {
            // initial state: no repo level policy
            AccessControlPolicy[] policies = acMgr.getPolicies(null);
            assertNotNull(policies);
            assertEquals(0, policies.length);

            AccessControlPolicy[] effective = jAcMgr.getEffectivePolicies(principalSet);
            assertNotNull(effective);
            assertEquals(0, effective.length);

            AccessControlPolicyIterator it = acMgr.getApplicablePolicies(null);
            assertTrue(it.hasNext());

            // modify the repo level policy
            modifyPrivileges(null, NameConstants.JCR_NODE_TYPE_DEFINITION_MANAGEMENT.toString(), false);
            modifyPrivileges(null, NameConstants.JCR_NAMESPACE_MANAGEMENT.toString(), true);

            // verify that the effective policies for the given principal set
            // is properly calculated.
            AccessControlPolicy[] eff = jAcMgr.getEffectivePolicies(principalSet);
            assertNotNull(eff);
            assertEquals(1, eff.length);
            assertTrue(eff[0] instanceof AccessControlList);

            AccessControlList acl = (AccessControlList) eff[0];
            AccessControlEntry[] aces = acl.getAccessControlEntries();
            assertNotNull(aces);
            assertEquals(2, aces.length);
            for (AccessControlEntry ace : aces) {
                assertEquals(testUser.getPrincipal(), ace.getPrincipal());
            }

        } catch (UnsupportedRepositoryOperationException e) {
            throw new NotExecutableException();
        } finally {
            // remove it again
            for (AccessControlPolicy plc : acMgr.getPolicies(null)) {
                acMgr.removePolicy(null, plc);
            }
            superuser.save();

            // back to initial state: no repo level policy
            AccessControlPolicy[] policies = acMgr.getPolicies(null);
            assertNotNull(policies);
            assertEquals(0, policies.length);
        }
    }
}
