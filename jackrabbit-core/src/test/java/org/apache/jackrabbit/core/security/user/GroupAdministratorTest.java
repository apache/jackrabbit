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
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.util.Text;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.security.Principal;
import java.util.Iterator;

/**
 * <code>GroupAdministratorTest</code>...
 */
public class GroupAdministratorTest extends AbstractUserTest {

    // group-admin
    private String uID;
    private String uPath;
    private Session uSession;

    private String parentUID;
    private String childUID;
    private String grID;


    private Group groupAdmin;

    protected void setUp() throws Exception {
        super.setUp();

        // create a first user
        Principal p = getTestPrincipal();
        UserImpl pUser = (UserImpl) userMgr.createUser(p.getName(), buildPassword(p));
        parentUID = pUser.getID();

        // create a second user 'below' the first user and make it group-admin
        p = getTestPrincipal();
        String pw = buildPassword(p);
        Credentials creds = buildCredentials(p.getName(), pw);
        User user = userMgr.createUser(p.getName(), pw, p, pUser.getNode().getPath());
        uID = user.getID();
        uPath = ((UserImpl) user).getNode().getPath();

        // make other user a group-administrator:
        Authorizable grAdmin = userMgr.getAuthorizable(UserConstants.GROUP_ADMIN_GROUP_NAME);
        if (grAdmin == null || !grAdmin.isGroup()) {
            throw new NotExecutableException("Cannot execute test. Group-Admin name has been changed by config.");
        }
        groupAdmin = (Group) grAdmin;
        groupAdmin.addMember(user);
        grID = groupAdmin.getID();

        // create a session for the grou-admin user.
        uSession = helper.getRepository().login(creds);
    }

    protected void tearDown() throws Exception {
        try {
            if (uSession != null) {
                uSession.logout();
            }
        } finally {
            // remove group member ship
            groupAdmin.removeMember(userMgr.getAuthorizable(uID));

            // remove all users that have been created
            Authorizable a = userMgr.getAuthorizable(parentUID);
            if (a != null) {
                a.remove();
            }

        }
        super.tearDown();
    }

    private String getChildID() throws RepositoryException {
        if (childUID == null) {
            // create a third child user below
            Principal p = getTestPrincipal();
            childUID = userMgr.createUser(p.getName(), buildPassword(p), p, uPath).getID();
        }
        return childUID;
    }

    public void testIsGroupAdmin() throws RepositoryException, NotExecutableException {
        UserManager umgr = getUserManager(uSession);
        Group gr = (Group) umgr.getAuthorizable(grID);

        assertTrue(gr.isMember(umgr.getAuthorizable(uID)));
    }

    public void testCreateUser() throws RepositoryException, NotExecutableException {
        UserManager umgr = getUserManager(uSession);
        UserImpl u = null;
        // create a new user -> must succeed and user must be create below 'other'
        try {
            Principal p = getTestPrincipal();
            u = (UserImpl) umgr.createUser(p.getName(), buildPassword(p));
            fail("Group administrator should not be allowed to create a new user.");
            u.remove();
        } catch (AccessDeniedException e) {
            // success
        }
    }

    public void testRemoveSelf() throws RepositoryException, NotExecutableException {
        UserManager umgr = getUserManager(uSession);

        Authorizable himself = umgr.getAuthorizable(uID);
        try {
            himself.remove();
            fail("A GroupAdministrator should not be allowed to remove the own authorizable.");
        } catch (AccessDeniedException e) {
            // success
        }
    }

    public void testCreateGroup() throws RepositoryException, NotExecutableException {
        UserManager umgr = getUserManager(uSession);
        Group testGroup = null;
        try {
            testGroup = umgr.createGroup(getTestPrincipal());
            assertTrue(Text.isDescendant(UserConstants.GROUPS_PATH, ((GroupImpl)testGroup).getNode().getPath()));
        } finally {
            if (testGroup != null) {
                testGroup.remove();
            }
        }
    }

    public void testCreateGroupWithIntermediatePath() throws RepositoryException, NotExecutableException {
        UserManager umgr = getUserManager(uSession);
        Group testGroup = null;
        try {
            testGroup = umgr.createGroup(getTestPrincipal(), "/any/intermediate/path");
            assertTrue(Text.isDescendant(UserConstants.GROUPS_PATH + "/any/intermediate/path", ((GroupImpl)testGroup).getNode().getPath()));
        } finally {
            if (testGroup != null) {
                testGroup.remove();
            }
        }
    }

    public void testAddChildToGroup() throws RepositoryException, NotExecutableException {
        UserManager umgr = getUserManager(uSession);
        Authorizable cU = umgr.getAuthorizable(getChildID());
        Group gr = (Group) umgr.getAuthorizable(grID);

        // adding and removing the child-user as member of a group not
        // succeed as long editing session is not user-admin.
        try {
            assertFalse(gr.addMember(cU));
        } catch (AccessDeniedException e) {
            // ok
        } finally {
            gr.removeMember(cU);
        }
    }

    public void testAddChildToGroup2() throws RepositoryException, NotExecutableException {
        UserManager umgr = getUserManager(uSession);
        Authorizable cU = umgr.getAuthorizable(getChildID());

        Authorizable auth = umgr.getAuthorizable(UserConstants.USER_ADMIN_GROUP_NAME);
        if (auth == null || !auth.isGroup()) {
            throw new NotExecutableException("Cannot execute test. User-Admin name has been changed by config.");
        }
        Group userAdmin = (Group)auth;
        User self = (User) umgr.getAuthorizable(uID);
        try {
            assertTrue(userAdmin.addMember(self));

            Group gr = (Group) umgr.getAuthorizable(groupAdmin.getID());
            assertTrue(gr.addMember(cU));
            assertTrue(gr.removeMember(cU));
        } finally {
            userAdmin.removeMember(self);
        }
    }

    public void testAddMembersToCreatedGroup() throws RepositoryException, NotExecutableException {
        UserManager umgr = getUserManager(uSession);
        Authorizable auth = umgr.getAuthorizable(UserConstants.USER_ADMIN_GROUP_NAME);
        if (auth == null || !auth.isGroup()) {
            throw new NotExecutableException("Cannot execute test. User-Admin name has been changed by config.");
        }
        Group userAdmin = (Group) auth;
        Group testGroup = null;
        User self = (User) umgr.getAuthorizable(uID);
        try {
            // let groupadmin create a new group
            testGroup = umgr.createGroup(getTestPrincipal(), "/a/b/c/d");

            // editing session adds itself to that group -> must succeed.
            assertTrue(testGroup.addMember(self));

            // editing session adds itself to user-admin group
            userAdmin.addMember(self);
            assertTrue(userAdmin.isMember(self));

            // add child-user to test group
            Authorizable testUser = umgr.getAuthorizable(getChildID());
            assertFalse(testGroup.isMember(testUser));
            assertTrue(testGroup.addMember(testUser));
        } finally {
            if (testGroup != null) {
                for (Iterator it = testGroup.getDeclaredMembers(); it.hasNext();) {
                    testGroup.removeMember((Authorizable) it.next());
                }
                testGroup.remove();
            }
            userAdmin.removeMember(self);            
        }
    }

    public void testAddMemberToForeignGroup() throws RepositoryException, NotExecutableException {
        try {
            // let superuser create child user below the user with uID.
            UserManager umgr = getUserManager(uSession);
            Authorizable cU = umgr.getAuthorizable(getChildID());
            Group uadminGr = (Group) umgr.getAuthorizable(UserConstants.USER_ADMIN_GROUP_NAME);
            if (uadminGr.isMember(cU)) {
                throw new RepositoryException("Test user is already member -> cannot execute.");
            }

            // adding to and removing a child user from a group the group-admin
            // is NOT member of must fail.
            uadminGr.addMember(cU);
            fail("A GroupAdmin should not be allowed to add a user to a group she/he is not member of.");

        } catch (AccessDeniedException e) {
            // success.
        }
    }

    public void testAddParentToGroup() throws RepositoryException, NotExecutableException {
        UserManager umgr = getUserManager(uSession);

        Authorizable pU = umgr.getAuthorizable(parentUID);
        Group gr = (Group) umgr.getAuthorizable(groupAdmin.getID());

        // adding and removing the parent-user as member of a group must
        // never succeed.
        try {
            assertFalse(gr.addMember(pU));
        } catch (AccessDeniedException e) {
            // ok
        } finally {
            gr.removeMember(pU);
        }

        // ... even if the editing user becomes member of the user-admin group
        Group uAdministrators = null;
        try {
            Authorizable userAdmin = userMgr.getAuthorizable(UserConstants.USER_ADMIN_GROUP_NAME);
            if (userAdmin == null || !userAdmin.isGroup()) {
                throw new NotExecutableException("Cannot execute test. User-Admin name has been changed by config.");
            }
            uAdministrators = (Group) userAdmin;
            uAdministrators.addMember(userMgr.getAuthorizable(uID));

            try {
                assertFalse(gr.addMember(pU));
                gr.removeMember(pU);
            } catch (AccessDeniedException e) {
                // fine as well.
            }
        } finally {
            // let superuser do the clean up.
            // remove testuser from u-admin group again.
            if (uAdministrators != null) {
                uAdministrators.removeMember(userMgr.getAuthorizable(uID));
            }
        }
    }

    public void testAddOwnAuthorizableAsGroupAdmin() throws RepositoryException, NotExecutableException {
        UserManager umgr = getUserManager(uSession);

        Authorizable user = umgr.getAuthorizable(uID);
        Group gr = (Group) umgr.getAuthorizable(groupAdmin.getID());

        // user is already group-admin -> adding must return false.
        // but should not throw exception.
        assertFalse(gr.addMember(user));
    }

    public void testRemoveMembershipForOwnAuthorizable() throws RepositoryException, NotExecutableException {
        UserManager umgr = getUserManager(uSession);

        Authorizable user = umgr.getAuthorizable(uID);
        Group gr = (Group) umgr.getAuthorizable(groupAdmin.getID());

        // removing himself from group. should succeed.
        assertTrue(gr.removeMember(user));
    }

    public void testAddOwnAuthorizableToForeignGroup() throws RepositoryException, NotExecutableException {
        UserManager umgr = getUserManager(uSession);

        Authorizable user = umgr.getAuthorizable(uID);
        Group uadminGr = (Group) umgr.getAuthorizable(UserConstants.USER_ADMIN_GROUP_NAME);
        if (uadminGr.isMember(user)) {
            throw new RepositoryException("Test user is already member -> cannot execute.");
        }

        String msg = "GroupAdmin must be able to add its own authorizable to a group she/he is not yet member of.";
        assertTrue(msg, uadminGr.addMember(user));
        assertTrue(msg, uadminGr.removeMember(user));
    }

    public void testRemoveMembersOfForeignGroup() throws RepositoryException, NotExecutableException {
        Group nGr = null;
        User nUs = null;
        User nUs2 = null;

        try {
            // let superuser create a group and a user a make user member of group
            nGr = userMgr.createGroup(getTestPrincipal());
            Principal p = getTestPrincipal();
            nUs = userMgr.createUser(p.getName(), buildPassword(p));
            p = getTestPrincipal();
            nUs2 = userMgr.createUser(p.getName(), buildPassword(p));
            nGr.addMember(nUs);
            nGr.addMember(nUs2);

            Group gr = (Group) getUserManager(uSession).getAuthorizable(nGr.getID());

            // removing any member must fail unless the testuser is user-admin
            Iterator it = gr.getMembers();
            if (it.hasNext()) {
                Authorizable auth = (Authorizable) it.next();

                String msg = "GroupAdmin cannot remove members of other user unless he/she is user-admin.";
                assertFalse(msg, gr.removeMember(auth));
            } else {
                throw new RepositoryException("Must contain members....");
            }

        } catch (AccessDeniedException e) {
            // fine as well.
        } finally {
            // let superuser remove authorizables again
            if (nGr != null) {
                nGr.removeMember(nUs);
                nGr.removeMember(nUs2);
                nGr.remove();
            }
            if (nUs != null) nUs.remove();
            if (nUs2 != null) nUs2.remove();
        }
    }

    public void testRemoveAllMembersOfForeignGroup() throws RepositoryException, NotExecutableException {
        Group nGr = null;
        User nUs = null;

        try {
            // let superuser create a group and a user a make user member of group
            nGr = userMgr.createGroup(getTestPrincipal());
            Principal p = getTestPrincipal();
            nUs = userMgr.createUser(p.getName(), buildPassword(p));
            nGr.addMember(nUs);

            Group gr = (Group) getUserManager(uSession).getAuthorizable(nGr.getID());

            // since only 1 single member -> removal rather than modification.
            // since uSession is not user-admin this must fail.
            for (Iterator it = gr.getMembers(); it.hasNext();) {
                Authorizable auth = (Authorizable) it.next();

                String msg = "GroupAdmin cannot remove members of groups unless he/she is UserAdmin.";
                assertFalse(msg, gr.removeMember(auth));
            }
        } catch (AccessDeniedException e) {
            // fine as well.
        } finally {
            // let superuser remove authorizables again
            if (nGr != null && nUs != null) nGr.removeMember(nUs);
            if (nGr != null) nGr.remove();
            if (nUs != null) nUs.remove();
        }
    }

    public void testImpersonationOfOtherUser() throws RepositoryException, NotExecutableException {
        UserManager umgr = getUserManager(uSession);
        Principal selfPrinc = umgr.getAuthorizable(uID).getPrincipal();

        User child = (User) umgr.getAuthorizable(getChildID());
        Impersonation impers = child.getImpersonation();
        assertFalse(impers.allows(buildSubject(selfPrinc)));
        try {
            assertFalse(impers.grantImpersonation(selfPrinc));
        } catch (AccessDeniedException e) {
            // ok.
        }
        assertFalse(impers.allows(buildSubject(selfPrinc)));

        User parent = (User) umgr.getAuthorizable(parentUID);
        impers = parent.getImpersonation();
        assertFalse(impers.allows(buildSubject(selfPrinc)));
        try {
            assertFalse(impers.grantImpersonation(selfPrinc));
        } catch (AccessDeniedException e) {
            // ok.
        }
        assertFalse(impers.allows(buildSubject(selfPrinc)));
    }
}