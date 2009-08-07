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
package org.apache.jackrabbit.core.security.user;

import org.apache.jackrabbit.api.security.user.AbstractUserTest;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.security.Principal;

/**
 * <code>ImpersonationImplTest</code>...
 */
public class ImpersonationImplTest extends AbstractUserTest {

    private static Logger log = LoggerFactory.getLogger(ImpersonationImplTest.class);

    private Credentials creds;
    private String uID;
    private Session uSession;
    private UserManager uMgr;

    private String otherUID;
    private Credentials otherCreds;

    protected void setUp() throws Exception {
        super.setUp();

        // create a first user and retrieve the UserManager from the session
        // created for that new user.
        Principal p = getTestPrincipal();
        String pw = buildPassword(p);
        creds = buildCredentials(p.getName(), pw);
        UserImpl u = (UserImpl) userMgr.createUser(p.getName(), pw);
        uID = u.getID();
        uSession = helper.getRepository().login(creds);
        uMgr = getUserManager(uSession);

        // create a second user 'below' the first user.
        p = getTestPrincipal();
        pw = buildPassword(p);
        otherCreds = buildCredentials(p.getName(), pw);
        User u2 = userMgr.createUser(p.getName(), pw, p, u.getNode().getPath());
        otherUID = u2.getID();
    }

    protected void tearDown() throws Exception {
        try {
            uSession.logout();
        } finally {
            Authorizable a = userMgr.getAuthorizable(uID);
            if (a != null) a.remove();
            a = userMgr.getAuthorizable(otherUID);
            if (a != null) a.remove();
        }
        super.tearDown();
    }

    public void testModifyOwnImpersonation() throws RepositoryException, NotExecutableException {
        User u = (User) uMgr.getAuthorizable(uID);

        Principal otherP = uMgr.getAuthorizable(otherUID).getPrincipal();

        assertTrue(u.getImpersonation().grantImpersonation(otherP));
        assertTrue(u.getImpersonation().allows(buildSubject(otherP)));
        assertTrue(u.getImpersonation().revokeImpersonation(otherP));
        assertFalse(u.getImpersonation().allows(buildSubject(otherP)));
    }

    public void testModifyOthersImpersonators() throws RepositoryException {
        Principal p = uMgr.getAuthorizable(uID).getPrincipal();

        User other = (User) uMgr.getAuthorizable(otherUID);
        try {
            boolean success = other.getImpersonation().grantImpersonation(p);
            assertFalse("A simple user may not add itself as impersonator to another user.",success);
        } catch (AccessDeniedException e) {
            // fine as well -> access denied.
        }
        assertFalse("A simple user may not add itself as impersonator to another user.", other.getImpersonation().allows(buildSubject(p)));
    }
}