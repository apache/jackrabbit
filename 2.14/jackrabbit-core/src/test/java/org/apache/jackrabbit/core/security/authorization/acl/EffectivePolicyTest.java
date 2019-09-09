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
import org.apache.jackrabbit.api.security.JackrabbitAccessControlManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.authorization.AbstractEffectivePolicyTest;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import java.security.Principal;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * <code>EffectivePolicyTest</code>...
 */
public class EffectivePolicyTest extends AbstractEffectivePolicyTest {

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

    public void testEffectivePoliciesByPath() throws RepositoryException, NotExecutableException {
        /*
         precondition:
         testuser must have READ-only permission on test-node and below
        */
        checkReadOnly(path);

        // give 'testUser' READ_AC privileges at 'path'
        Privilege[] privileges = privilegesFromNames(new String[] {
                Privilege.JCR_READ_ACCESS_CONTROL,
        });

        givePrivileges(path, privileges, getRestrictions(superuser, path));

        Session testSession = getTestSession();
        AccessControlManager testAcMgr = getTestACManager();

        assertFalse(testAcMgr.hasPrivileges("/", privileges));
        assertTrue(testAcMgr.hasPrivileges(path, privileges));

        // since read-ac access is denied on the root that by default is
        // access controlled, getEffectivePolicies must fail due to missing
        // permissions to view all the effective policies.
        try {
            testAcMgr.getEffectivePolicies(path);
            fail();
        } catch (AccessDeniedException e) {
            // success
        }

        // ... and same on childNPath.
        try {
            testAcMgr.getEffectivePolicies(childNPath);
            fail();
        } catch (AccessDeniedException e) {
            // success
        }
    }
    
    public void testGetEffectivePoliciesByPrincipal() throws Exception {
        /*
         precondition:
         testuser must have READ-only permission on test-node and below
        */
        checkReadOnly(path);

        // give 'testUser' READ_AC privileges at 'path'
        Privilege[] privileges = privilegesFromNames(new String[] {
                Privilege.JCR_READ_ACCESS_CONTROL,
        });

        givePrivileges(path, privileges, getRestrictions(superuser, path));

        Session testSession = getTestSession();
        AccessControlManager testAcMgr = getTestACManager();

        // effective policies for testPrinicpal only on path -> must succeed.
        ((JackrabbitAccessControlManager) testAcMgr).getEffectivePolicies(Collections.singleton(testUser.getPrincipal()));

        // effective policies for a combination of principals -> must fail since
        // policy for 'everyone' at root node cannot be read by testuser
        Set<Principal> principals = ((SessionImpl) testSession).getSubject().getPrincipals();
        try {
            ((JackrabbitAccessControlManager) testAcMgr).getEffectivePolicies(principals);
            fail();
        } catch (AccessDeniedException e) {
            // success
        }

        withdrawPrivileges(childNPath, privileges, getRestrictions(superuser, childNPath));

        // the effective policies included the allowed acl at 'path' and
        // the denied acl at 'childNPath' -> must fail
        try {
            ((JackrabbitAccessControlManager) testAcMgr).getEffectivePolicies(Collections.singleton(testUser.getPrincipal()));
            fail();
        } catch (AccessDeniedException e) {
            // success
        }
    }
}