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
package org.apache.jackrabbit.api.security.user;

import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.Subject;
import java.security.Principal;
import java.util.Collections;
import java.util.Set;

/**
 * <code>ImpersonationTest</code>...
 */
public class ImpersonationTest extends AbstractUserTest {

    private static Logger log = LoggerFactory.getLogger(ImpersonationTest.class);

    private User newUser;
    private Impersonation impersonation;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Principal test = getTestPrincipal();
        String pw = buildPassword(test);
        newUser = userMgr.createUser(test.getName(), pw);
        save(superuser);
        
        impersonation = newUser.getImpersonation();
    }

    @Override
    protected void tearDown() throws Exception {
        newUser.remove();
        save(superuser);
        super.tearDown();
    }

    public void testUnknownCannotImpersonate() throws RepositoryException {
        Principal test = getTestPrincipal();
        Subject subject = createSubject(test);
        assertFalse("An unknown principal should not be allowed to impersonate.", impersonation.allows(subject));
    }

    public void testGrantImpersonationUnknownUser() throws RepositoryException, NotExecutableException {
        Principal test = getTestPrincipal();
        try {
            assertFalse("Granting impersonation to an unknown principal should not be successful.", impersonation.grantImpersonation(test));
        }  finally {
            impersonation.revokeImpersonation(test);
            save(superuser);
        }
    }

    public void testImpersonateGroup() throws RepositoryException, NotExecutableException {
        Session s = getHelper().getReadOnlySession();
        try {
            Principal group = getTestGroup(s).getPrincipal();
            Subject subject = createSubject(group);
            assertFalse("An group principal should not be allowed to impersonate.", impersonation.allows(subject));
        } finally {
            s.logout();
        }
    }

    public void testGrantImpersonationToGroupPrincipal() throws RepositoryException, NotExecutableException {
        Session s = getHelper().getReadOnlySession();
        try {
            Principal group = getTestGroup(s).getPrincipal();
            try {
                assertFalse("Granting impersonation to a Group should not be successful.", impersonation.grantImpersonation(group));
            }  finally {
                impersonation.revokeImpersonation(group);
                save(superuser);
            }
        } finally {
            s.logout();
        }
    }

    public void testGrantImpersonation() throws RepositoryException, NotExecutableException {
        User u = null;
        Principal test = getTestPrincipal();
        try {
            u = userMgr.createUser(test.getName(), buildPassword(test));
            save(superuser);
            assertTrue("Admin should be allowed to edit impersonation and grant to another test-user.", impersonation.grantImpersonation(test));
        }  finally {
            impersonation.revokeImpersonation(test);
            if (u != null) {
                u.remove();
            }
            save(superuser);
        }
    }

    public void testGrantImpersonationTwice() throws RepositoryException, NotExecutableException {
        Principal test = getTestPrincipal();
        User u = null;
        try {
            u = userMgr.createUser(test.getName(), buildPassword(test));
            save(superuser);
            impersonation.grantImpersonation(test);
            save(superuser);
            // try again
            assertFalse("Granting impersonation twice should not succeed.", impersonation.grantImpersonation(test));
        }  finally {
            impersonation.revokeImpersonation(test);
            if (u != null) {
                u.remove();
            }
            save(superuser);
        }
    }

    public void testRevokeImpersonation() throws RepositoryException, NotExecutableException {
        User u = null;
        Principal test = getTestPrincipal();
        try {
            u = userMgr.createUser(test.getName(), buildPassword(test));
            save(superuser);
            impersonation.grantImpersonation(test);
            save(superuser);

            assertTrue(impersonation.revokeImpersonation(test));
        }  finally {
            if (u != null) {
                u.remove();
            }
        }
    }

    public void testRevokeImpersonationTwice() throws RepositoryException, NotExecutableException {
        User u = null;
        Principal test = getTestPrincipal();
        try {
            u = userMgr.createUser(test.getName(), buildPassword(test));
            save(superuser);
            impersonation.grantImpersonation(test);
            save(superuser);
            impersonation.revokeImpersonation(test);
            save(superuser);
            // try again
            assertFalse("Revoking impersonation twice should not succeed.", impersonation.revokeImpersonation(test));
        }  finally {
            if (u != null) {
                u.remove();
            }
        }
    }

    public void testAdministratorCanImpersonate() throws RepositoryException, NotExecutableException {
        User admin = getTestUser(superuser);
        Subject subject = createSubject(admin);
        assertTrue(impersonation.allows(subject));
    }

    public void testCannotGrantImpersonationForAdministrator() throws RepositoryException, NotExecutableException {
        User admin = getTestUser(superuser);
        try {
            assertFalse(impersonation.grantImpersonation(admin.getPrincipal()));
        } finally {
            impersonation.revokeImpersonation(admin.getPrincipal());
        }
    }

    public void testCannotRevokeImpersonationForAdministrator() throws RepositoryException, NotExecutableException {
        User admin = getTestUser(superuser);
        assertFalse(impersonation.revokeImpersonation(admin.getPrincipal()));
    }

    public void testImpersonatingOneself() throws RepositoryException {
        Subject subject = createSubject(newUser);
        assertFalse(impersonation.allows(subject));
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/JCR-2931">JCR-2931</a>
     */
    public void testAdminImpersonatingOneself() throws RepositoryException, NotExecutableException {
        User admin = getTestUser(superuser);
        Subject subject = createSubject(admin);
        assertTrue(admin.getImpersonation().allows(subject));
    }

    public void testGrantImpersonatingForOneself() throws RepositoryException {
        Principal main = newUser.getPrincipal();
        try {
            assertFalse(impersonation.grantImpersonation(main));
        } finally {
            impersonation.revokeImpersonation(main);
        }
    }

    public void testRevokeImpersonatingForOneself() throws RepositoryException {
        Principal main = newUser.getPrincipal();
        assertFalse(impersonation.revokeImpersonation(main));
    }

    private Subject createSubject(User u) throws RepositoryException {
        Principal main = u.getPrincipal();
        return createSubject(main);
    }

    private Subject createSubject(Principal p) throws RepositoryException {
        Set<Credentials> creds = Collections.singleton(buildCredentials(p.getName(), buildPassword(p)));
        return new Subject(true, Collections.singleton(p), creds, creds);
    }
}