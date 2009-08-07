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
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.Impersonation;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.security.principal.EveryonePrincipal;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.util.Text;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.security.Principal;
import java.util.Iterator;
import java.util.Set;

/**
 * <code>UserAdministratorTest</code>...
 */
public class UserAdministratorTest extends AbstractUserTest {

    // user 'above'
    private String uID;

    // user-admin 'below'
    private String otherUID;
    private String otherPath;
    private Session otherSession;

    // the user-admin group
    private Group uAdministrators;

    protected void setUp() throws Exception {
        super.setUp();

        // create a first user and retrieve the UserManager from the session
        // created for that new user.
        Principal p = getTestPrincipal();
        UserImpl u = (UserImpl) userMgr.createUser(p.getName(), buildPassword(p));
        uID = u.getID();

        // create a second user 'below' the first user.
        p = getTestPrincipal();
        String pw = buildPassword(p);
        Credentials otherCreds = buildCredentials(p.getName(), pw);
        User other = userMgr.createUser(p.getName(), pw, p, u.getNode().getPath());
        otherUID = other.getID();
        otherPath = ((UserImpl) other).getNode().getPath();

        // make other user a user-administrator:
        Authorizable ua = userMgr.getAuthorizable(UserConstants.USER_ADMIN_GROUP_NAME);
        if (ua == null || !ua.isGroup()) {
            throw new NotExecutableException("Cannot execute test. User-Admin name has been changed by config.");
        }
        uAdministrators = (Group) ua;
        uAdministrators.addMember(other);

        // create a session for the other user.
        otherSession = helper.getRepository().login(otherCreds);
    }

    protected void tearDown() throws Exception {
        try {
            if (otherSession != null) {
                otherSession.logout();
            }
        } finally {
            Authorizable a = userMgr.getAuthorizable(otherUID);
            if (a != null) {
                for (Iterator it = a.memberOf(); it.hasNext();) {
                    Group gr = (Group) it.next();
                    if (!gr.getPrincipal().equals(EveryonePrincipal.getInstance())) {
                        gr.removeMember(a);
                    }
                }
                a.remove();
            }
            a = userMgr.getAuthorizable(uID);
            if (a != null) {
                a.remove();
            }
        }
        super.tearDown();
    }

    private Group getGroupAdminGroup(UserManager uMgr) throws RepositoryException, NotExecutableException {
        Authorizable auth = uMgr.getAuthorizable(UserConstants.GROUP_ADMIN_GROUP_NAME);
        if (auth == null || !auth.isGroup()) {
            throw new NotExecutableException();
        }
        return (Group) auth;
    }

    public void testUserIsUserAdmin() throws RepositoryException, NotExecutableException {
        Set principals = getPrincipalSetFromSession(otherSession);
        boolean isAdmin = false;
        for (Iterator it = principals.iterator(); it.hasNext() && !isAdmin;) {
           isAdmin = UserConstants.USER_ADMIN_GROUP_NAME.equals(((Principal) it.next()).getName());
        }
        assertTrue(isAdmin);
    }

    public void testCreateUser() throws RepositoryException, NotExecutableException {
        UserManager umgr = getUserManager(otherSession);
        UserImpl u = null;
        // create a new user -> must succeed and user must be create below 'other'
        try {
            Principal p = getTestPrincipal();
            u = (UserImpl) umgr.createUser(p.getName(), buildPassword(p));
            assertTrue(Text.isDescendant(otherPath, u.getNode().getPath()));
        } finally {
            if (u != null) {
                u.remove();
            }
        }
    }

    public void testCreateUserWithIntermediatePath() throws RepositoryException, NotExecutableException {
        UserManager umgr = getUserManager(otherSession);
        UserImpl u = null;
        // create a new user with intermediate-path
        // -> must succeed and user must be create below 'other'
        try {
            Principal p = getTestPrincipal();
            u = (UserImpl) umgr.createUser(p.getName(), buildPassword(p), p, "/some/intermediate/path");
            assertTrue(Text.isDescendant(otherPath, u.getNode().getPath()));
            assertTrue(Text.isDescendant(otherPath + "/some/intermediate/path", u.getNode().getPath()));
        } finally {
            if (u != null) {
                u.remove();
            }
        }
    }

    public void testRemoveHimSelf() throws RepositoryException, NotExecutableException {
        UserManager umgr = getUserManager(otherSession);

        Authorizable himself = umgr.getAuthorizable(otherUID);
        try {
            himself.remove();
            fail("A UserAdministrator should not be allowed to remove himself.");
        } catch (AccessDeniedException e) {
            // success
        }
    }

    public void testRemoveParentUser() throws RepositoryException, NotExecutableException {
        UserManager umgr = getUserManager(otherSession);

        Authorizable parentuser = umgr.getAuthorizable(uID);
        try {
            parentuser.remove();
            fail("A UserAdministrator should not be allowed to remove a 'parent' user.");
        } catch (AccessDeniedException e) {
            // success
        }
    }

    public void testModifyImpersonationOfChildUser() throws RepositoryException, NotExecutableException {
        UserManager umgr = getUserManager(otherSession);
        Principal otherP = umgr.getAuthorizable(otherUID).getPrincipal();

        User u = null;
        // create a new user -> must succeed and user must be create below 'other'
        try {
            Principal p = getTestPrincipal();
            u = umgr.createUser(p.getName(), buildPassword(p));

            Impersonation impers = u.getImpersonation();
            assertFalse(impers.allows(buildSubject(otherP)));
            assertTrue(impers.grantImpersonation(otherP));
            assertTrue(impers.allows(buildSubject(otherP)));
        } finally {
            // impersonation get removed while removing the user u.
            if (u != null) {
                u.remove();
            }
        }
    }

    public void testModifyImpersonationOfParentUser() throws RepositoryException, NotExecutableException {
        UserManager umgr = getUserManager(otherSession);
        User u = (User) umgr.getAuthorizable(uID);
        Impersonation uImpl = u.getImpersonation();

        Principal otherP = umgr.getAuthorizable(otherUID).getPrincipal();

        if (!uImpl.allows(buildSubject(otherP))) {
            // ... trying to modify 'impersonators of 'uid' must not succeed.
            try {
                assertFalse(uImpl.grantImpersonation(otherP));
            } catch (AccessDeniedException e) {
                // success
            } finally {
                assertFalse(uImpl.allows(buildSubject(otherP)));
                uImpl.revokeImpersonation(otherP);
            }
        } else {
            throw new NotExecutableException("Cannot execute test. OtherP can already impersonate UID-user.");
        }
    }

    public void testModifyGroupForHimSelf() throws RepositoryException, NotExecutableException {
        UserManager umgr = getUserManager(otherSession);

        User userHimSelf = (User) umgr.getAuthorizable(otherUID);
        Group gr = getGroupAdminGroup(umgr);
        try {
            assertFalse(gr.addMember(userHimSelf));
        } catch (RepositoryException e) {
            // success as well.
        }
    }

    public void testModifyGroupForParentUser() throws RepositoryException, NotExecutableException {
        UserManager umgr = getUserManager(otherSession);

        User parentUser = (User) umgr.getAuthorizable(uID);
        if (parentUser == null) {
            throw new NotExecutableException();
        } else {
            Group gr = getGroupAdminGroup(umgr);
            try {
                assertFalse(gr.addMember(parentUser));
            } catch (RepositoryException e) {
                // success
            }
        }
    }

    public void testModifyGroupForChildUser() throws RepositoryException, NotExecutableException {
        UserManager umgr = getUserManager(otherSession);
        Principal cp = getTestPrincipal();
        User childU = null;
        try {
            childU = umgr.createUser(cp.getName(), buildPassword(cp));
            Group gr = getGroupAdminGroup(umgr);
            try {
                assertFalse(gr.addMember(childU));
            } catch (RepositoryException e) {
                // success
            }
        } finally {
            if (childU != null) {
                childU.remove();
            }
        }
    }

    public void testCreateGroup() throws RepositoryException, NotExecutableException {
        UserManager umgr = getUserManager(otherSession);
        try {
            Group testGroup = umgr.createGroup(getTestPrincipal());
            fail("UserAdmin should not be allowed to create a new Group.");
            testGroup.remove();
        } catch (RepositoryException e) {
            // success.
        }
    }

    public void testCreateGroupWithIntermediatePath() throws RepositoryException, NotExecutableException {
        UserManager umgr = getUserManager(otherSession);
        try {
            Group testGroup = umgr.createGroup(getTestPrincipal(), "/any/intermediate/path");
            fail("UserAdmin should not be allowed to create a new Group.");
            testGroup.remove();
        } catch (RepositoryException e) {
            // success.
        }
    }

    public void testAddToGroup() throws NotExecutableException, RepositoryException {
        UserManager umgr = getUserManager(otherSession);
        Group gr = getGroupAdminGroup(umgr);

        Authorizable auth = umgr.getAuthorizable(uID);
        try {
            assertFalse(gr.addMember(auth));
        } catch (AccessDeniedException e) {
            // success as well.
        }

        auth = umgr.getAuthorizable(otherUID);
        try {
            assertFalse(gr.addMember(auth));
        } catch (AccessDeniedException e) {
            // success as well.
        }

        // not even user-admin group
        gr = (Group) umgr.getAuthorizable(uAdministrators.getID());
        auth = umgr.getAuthorizable(otherUID);
        try {
            assertFalse(gr.addMember(auth));
        } catch (AccessDeniedException e) {
            // success
        }
    }
}