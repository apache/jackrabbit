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
package org.apache.jackrabbit.core.security.authorization.principalbased;

import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlManager;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlPolicy;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.authorization.AbstractEffectivePolicyTest;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.Privilege;
import java.security.Principal;
import java.util.Collections;
import java.util.Map;

/**
 * <code>EffectivePolicyTest</code>...
 */
public class EffectivePolicyTest extends AbstractEffectivePolicyTest {

    @Override
    protected boolean isExecutable() {
        return EvaluationUtil.isExecutable((SessionImpl) superuser, acMgr);
    }

    @Override
    protected JackrabbitAccessControlList getPolicy(AccessControlManager acM, String path, Principal principal) throws RepositoryException, AccessDeniedException, NotExecutableException {
        return EvaluationUtil.getPolicy(acM, path, principal);
    }

    @Override
    protected Map<String, Value> getRestrictions(Session s, String path) throws RepositoryException, NotExecutableException {
        return EvaluationUtil.getRestrictions(s, path);
    }

    public void testGetEffectivePoliciesByPrincipal() throws Exception {
        Privilege[] privileges = privilegesFromNames(new String[] {
                Privilege.JCR_READ_ACCESS_CONTROL,
        });

        JackrabbitAccessControlManager jacMgr = (JackrabbitAccessControlManager) acMgr;

        Principal everyone = ((SessionImpl) superuser).getPrincipalManager().getEveryone();
        AccessControlPolicy[] acp = jacMgr.getEffectivePolicies(Collections.singleton(everyone));
        assertNotNull(acp);
        assertEquals(1, acp.length);
        assertTrue(acp[0] instanceof JackrabbitAccessControlPolicy);

        JackrabbitAccessControlPolicy jacp = (JackrabbitAccessControlPolicy) acp[0];

        assertFalse(jacMgr.hasPrivileges(jacp.getPath(), Collections.singleton(testUser.getPrincipal()), privileges));
        assertFalse(jacMgr.hasPrivileges(jacp.getPath(), Collections.singleton(everyone), privileges));


        acp = jacMgr.getApplicablePolicies(testUser.getPrincipal());
        if (acp.length == 0) {
            acp = jacMgr.getPolicies(testUser.getPrincipal());
        }

        assertNotNull(acp);
        assertEquals(1, acp.length);
        assertTrue(acp[0] instanceof JackrabbitAccessControlList);

        // let testuser read the ACL defined for 'testUser' principal.
        JackrabbitAccessControlList acl = (JackrabbitAccessControlList) acp[0];
        acl.addEntry(testUser.getPrincipal(), privileges, true, getRestrictions(superuser, acl.getPath()));
        jacMgr.setPolicy(acl.getPath(), acl);
        superuser.save();

        Session testSession = getTestSession();
        AccessControlManager testAcMgr = getTestACManager();

        // effective policies for testPrinicpal only on path -> must succeed.
        ((JackrabbitAccessControlManager) testAcMgr).getEffectivePolicies(Collections.singleton(testUser.getPrincipal()));

        // effective policies for a combination of principals -> must fail
        try {
            ((JackrabbitAccessControlManager) testAcMgr).getEffectivePolicies(((SessionImpl) testSession).getSubject().getPrincipals());
            fail();
        } catch (AccessDeniedException e) {
            // success
        }
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

        assertTrue(testAcMgr.hasPrivileges(path, privileges));

        // reading the policies stored at 'path' must succeed.
        // however, principalbased-ac stores ac information in a separate tree.
        // no policy must be present at 'path'.
        AccessControlPolicy[] policies = testAcMgr.getPolicies(path);
        assertNotNull(policies);
        assertEquals(0, policies.length);

        // since read-ac access denied on the acl storing node itself obtaining
        // the effective policy for 'path' must fail.
        try {
            testAcMgr.getEffectivePolicies(path);
            fail();
        } catch (AccessDeniedException e) {
            // success
        }
    }
}