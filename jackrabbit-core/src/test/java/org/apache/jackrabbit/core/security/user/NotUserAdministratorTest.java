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
import org.apache.jackrabbit.api.security.user.AuthorizableExistsException;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.Impersonation;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.core.security.SecurityConstants;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import java.security.Principal;

/**
 * <code>NotUserAdministratorTest</code>...
 */
public class NotUserAdministratorTest extends AbstractUserTest {

    // test user that is NOT user admin
    private String uID;
    private Session uSession;
    private UserManager uMgr;

    protected void setUp() throws Exception {
        super.setUp();

        // create a first user and retrieve the UserManager from the session
        // created for that new user.
        Principal p = getTestPrincipal();
        String pw = buildPassword(p);

        UserImpl u = (UserImpl) userMgr.createUser(p.getName(), pw);
        save(superuser);
        
        uID = u.getID();

        // create a session for the other user.
        uSession = getHelper().getRepository().login(new SimpleCredentials(uID, pw.toCharArray()));
        uMgr = getUserManager(uSession);
    }

    protected void tearDown() throws Exception {
        try {
            if (uSession != null) {
                uSession.logout();
            }
        } finally {
            Authorizable a = userMgr.getAuthorizable(uID);
            if (a != null) {
                a.remove();
                save(superuser);
            }
        }
        super.tearDown();
    }

    public void testCreateUser() throws NotExecutableException {
        try {
            Principal p = getTestPrincipal();
            User u = uMgr.createUser(p.getName(), buildPassword(p));
            save(uSession);

            fail("A non-UserAdmin should not be allowed to create a new User.");

            // clean-up: let superuser remove the user created by fault.
            userMgr.getAuthorizable(u.getID()).remove();
        } catch (AuthorizableExistsException e) {
            // should never get here.
            fail(e.getMessage());
        } catch (RepositoryException e) {
            // success
        }
    }

    public void testCreateUserWithItermediatePath() throws NotExecutableException {
        try {
            Principal p = getTestPrincipal();
            User u = uMgr.createUser(p.getName(), buildPassword(p), p, "/any/intermediate/path");
            save(uSession);

            fail("A non-UserAdmin should not be allowed to create a new User.");

            // clean-up: let superuser remove the user created by fault.
            userMgr.getAuthorizable(u.getID()).remove();
        } catch (AuthorizableExistsException e) {
            // should never get here.
            fail(e.getMessage());
        } catch (RepositoryException e) {
            // success
        }
    }

    public void testRemoveOwnAuthorizable() throws RepositoryException, NotExecutableException {
        Authorizable himself = uMgr.getAuthorizable(uID);
        try {
            himself.remove();
            save(uSession);

            fail("A user should not be allowed to remove him/herself.");
        } catch (AccessDeniedException e) {
            // success
        }
    }

    public void testRemoveUser() throws RepositoryException, NotExecutableException {
        // let superuser create another user.
        Principal p = getTestPrincipal();
        String user2ID = userMgr.createUser(p.getName(), buildPassword(p)).getID();
        save(superuser);

        try {
            Authorizable a = uMgr.getAuthorizable(user2ID);
            a.remove();
            save(uSession);

            fail("A non-administrator user should not be allowed to remove another user.");
        } catch (AccessDeniedException e) {
            // success
        }

        // let superuser do clean up.
        Authorizable user2 = userMgr.getAuthorizable(user2ID);
        if (user2 != null) {
            user2.remove();
            save(superuser);
        }
    }

    public void testRemoveOtherUser() throws RepositoryException, NotExecutableException {
        // let superuser create another user.
        Principal p = getTestPrincipal();
        String user2ID = userMgr.createUser(p.getName(), buildPassword(p), p, "/any/intermediate/path").getID();
        save(superuser);

        try {
            Authorizable a = uMgr.getAuthorizable(user2ID);
            a.remove();
            save(uSession);

            fail("A non-administrator user should not be allowed to remove another user.");
        } catch (AccessDeniedException e) {
            // success
        }

        // let superuser do clean up.
        Authorizable user2 = userMgr.getAuthorizable(user2ID);
        if (user2 != null) {
            user2.remove();
            save(superuser);
        }
    }

    public void testModifyImpersonationOfAnotherUser() throws RepositoryException, NotExecutableException {
        // let superuser create another user.
        Principal p = getTestPrincipal();
        String user2ID = userMgr.createUser(p.getName(), buildPassword(p)).getID();
        save(superuser);

        try {
            Authorizable a = uMgr.getAuthorizable(user2ID);

            Impersonation impers = ((User) a).getImpersonation();
            Principal himselfP = uMgr.getAuthorizable(uID).getPrincipal();
            assertFalse(impers.allows(buildSubject(himselfP)));
            impers.grantImpersonation(himselfP);
            save(uSession);

            fail("A non-administrator user should not be allowed modify Impersonation of another user.");
        } catch (AccessDeniedException e) {
            // success
        }

        // let superuser do clean up.
        Authorizable user2 = userMgr.getAuthorizable(user2ID);
        if (user2 != null) {
            user2.remove();
            save(superuser);
        }
    }

    public void testAddToGroup() throws NotExecutableException, RepositoryException {
        Authorizable auth = uMgr.getAuthorizable(SecurityConstants.ADMINISTRATORS_NAME);
        if (auth == null || !auth.isGroup()) {
            throw new NotExecutableException("Couldn't find 'administrators' group");
        }

        Group gr = (Group) auth;
        try {
            auth = uMgr.getAuthorizable(uID);
            gr.addMember(auth);
            save(uSession);

            fail("a common user should not be allowed to modify any groups.");
        } catch (AccessDeniedException e) {
            // success
        } finally {
            if (gr.removeMember(auth)) {
                save(uSession);
            }
        }
    }
}