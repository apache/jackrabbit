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
    private String uPath;
    private Session uSession;
    private UserManager uMgr;

    protected void setUp() throws Exception {
        super.setUp();

        // create a first user and retrieve the UserManager from the session
        // created for that new user.
        Principal p = getTestPrincipal();
        String pw = buildPassword(p);
        UserImpl u = (UserImpl) userMgr.createUser(p.getName(), pw);
        uID = u.getID();
        uPath = u.getNode().getPath();

        // create a session for the other user.
        uSession = helper.getRepository().login(new SimpleCredentials(uID, pw.toCharArray()));
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
            }
        }
        super.tearDown();
    }

    public void testCreateUser() {
        try {
            Principal p = getTestPrincipal();
            User u = uMgr.createUser(p.getName(), buildPassword(p));
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

    public void testCreateUserWithItermediatePath() {
        try {
            Principal p = getTestPrincipal();
            User u = uMgr.createUser(p.getName(), buildPassword(p), p, "/any/intermediate/path");
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

    public void testRemoveOwnAuthorizable() throws RepositoryException {
        Authorizable himself = uMgr.getAuthorizable(uID);
        try {
            himself.remove();
            fail("A user should not be allowed to remove him/herself.");
        } catch (AccessDeniedException e) {
            // success
        }
    }

    public void testRemoveChildUser() throws RepositoryException {
        // let superuser create a child-user.
        Principal p = getTestPrincipal();
        String childID = userMgr.createUser(p.getName(), buildPassword(p), p, uPath).getID();
        try {
            Authorizable a = uMgr.getAuthorizable(childID);
            a.remove();
            fail("A non-administrator user should not be allowed to remove a child-user.");
        } catch (AccessDeniedException e) {
            // success
        }

        // let superuser do clean up.
        Authorizable child = userMgr.getAuthorizable(childID);
        if (child != null) {
            child.remove();
        }
    }

    public void testRemoveOtherUser() throws RepositoryException {
        // let superuser create a child-user.
        Principal p = getTestPrincipal();
        String childID = userMgr.createUser(p.getName(), buildPassword(p), p, "/any/intermediate/path").getID();
        try {
            Authorizable a = uMgr.getAuthorizable(childID);
            a.remove();
            fail("A non-administrator user should not be allowed to remove another user.");
        } catch (AccessDeniedException e) {
            // success
        }

        // let superuser do clean up.
        Authorizable child = userMgr.getAuthorizable(childID);
        if (child != null) {
            child.remove();
        }
    }

    public void testModifyImpersonation() throws RepositoryException {
        // let superuser create a child-user.
        Principal p = getTestPrincipal();
        Authorizable child = userMgr.createUser(p.getName(), buildPassword(p), p, uPath);
        try {
            p = child.getPrincipal();

            Authorizable himself = uMgr.getAuthorizable(uID);
            Impersonation impers = ((User) himself).getImpersonation();

            assertFalse(impers.allows(buildSubject(p)));
            assertTrue(impers.grantImpersonation(p));
            assertTrue(impers.allows(buildSubject(p)));
            assertTrue(impers.revokeImpersonation(p));
            assertFalse(impers.allows(buildSubject(p)));

        } finally {
            // let superuser do clean up.
            child.remove();
        }
    }

    public void testModifyImpersonationOfChildUser() throws RepositoryException {
        // let superuser create a child-user.
        Principal p = getTestPrincipal();
        String childID = userMgr.createUser(p.getName(), buildPassword(p), p, uPath).getID();
        try {
            Authorizable child = uMgr.getAuthorizable(childID);

            Impersonation impers = ((User) child).getImpersonation();
            Principal himselfP = uMgr.getAuthorizable(uID).getPrincipal();
            assertFalse(impers.allows(buildSubject(himselfP)));
            impers.grantImpersonation(himselfP);
            fail("A non-administrator user should not be allowed modify Impersonation of a child user.");
        } catch (AccessDeniedException e) {
            // success
        }

        // let superuser do clean up.
        Authorizable child = userMgr.getAuthorizable(childID);
        if (child != null) {
            child.remove();
        }
    }

    public void testAddToGroup() throws NotExecutableException, RepositoryException {
        Authorizable auth = userMgr.getAuthorizable(SecurityConstants.ADMINISTRATORS_NAME);
        if (auth == null || !auth.isGroup()) {
            throw new NotExecutableException("Couldn't find 'administrators' group");
        }

        Group gr = (Group) auth;
        try {
            auth = uMgr.getAuthorizable(uID);
            gr.addMember(auth);
            fail("a common user should not be allowed to modify any groups.");
            gr.removeMember(auth);
        } catch (AccessDeniedException e) {
            // success
        }
    }
}