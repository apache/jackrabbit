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

import org.apache.jackrabbit.core.security.authorization.AbstractEvaluationTest;
import org.apache.jackrabbit.core.security.authorization.PolicyEntry;
import org.apache.jackrabbit.core.security.authorization.PolicyTemplate;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.core.security.jsr283.security.AccessControlEntry;
import org.apache.jackrabbit.core.security.jsr283.security.AccessControlManager;
import org.apache.jackrabbit.core.security.jsr283.security.AccessControlPolicy;
import org.apache.jackrabbit.core.security.jsr283.security.AccessControlPolicyIterator;
import org.apache.jackrabbit.core.security.jsr283.security.Privilege;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import java.security.Principal;

/**
 * <code>EvaluationTest</code>...
 */
public class EvaluationTest extends AbstractEvaluationTest {

    private String[] restrictions = new String[0];

    protected void setUp() throws Exception {
        super.setUp();

        try {
            AccessControlPolicy rootPolicy = acMgr.getPolicy("/");
            if (!(rootPolicy instanceof ACLTemplate)) {
                throw new NotExecutableException();
            }
        } catch (RepositoryException e) {
            throw new NotExecutableException();
        }
    }

    protected void clearACInfo() {
        // nop
    }

    protected PolicyTemplate getPolicyTemplate(AccessControlManager acM, String path) throws RepositoryException, AccessDeniedException, NotExecutableException {
        AccessControlPolicyIterator it = acM.getApplicablePolicies(path);
        while (it.hasNext()) {
            AccessControlPolicy acp = it.nextAccessControlPolicy();
            if (acp instanceof ACLTemplate) {
                return (ACLTemplate) acp;
            }
        }
        throw new NotExecutableException("ACLTemplate expected.");
    }

    protected PolicyEntry createEntry(Principal principal, int privileges, boolean isAllow, String[] restrictions) {
        return new ACEImpl(principal, privileges, isAllow);
    }

    protected String[] getRestrictions(String path) {
        return restrictions;
    }

    public void testAccessControlModification2() throws RepositoryException, NotExecutableException {
        /*
         precondition:
         testuser must have READ-only permission on test-node and below
        */
        checkReadOnly(path);

        // give 'testUser' READ_AC|MODIFY_AC privileges at 'path'
        PolicyTemplate tmpl = givePrivileges(path, PrivilegeRegistry.READ_AC |
                PrivilegeRegistry.MODIFY_AC, getRestrictions(path));
        /*
         testuser must
         - still have the inherited READ permission.
         - must have permission to view AC items at 'path' (and below)
         - must have permission to modify AC items at 'path'

         testuser must not have
         - permission to view AC items outside of the tree defined by path.
        */

        // make sure the 'rep:policy' node has been created.
        assertTrue(superuser.itemExists(tmpl.getPath() + "/rep:policy"));

        // test: MODIFY_AC granted at 'path'
        assertTrue(testAcMgr.hasPrivileges(path, new Privilege[] {
                PrivilegeRegistry.MODIFY_AC_PRIVILEGE}));

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
        AccessControlPolicy effPolicy = testAcMgr.getEffectivePolicy(path);
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
}