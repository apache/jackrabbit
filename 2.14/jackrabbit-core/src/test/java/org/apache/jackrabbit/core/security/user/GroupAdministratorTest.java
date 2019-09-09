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
import javax.jcr.Node;
import java.security.Principal;
import java.util.Iterator;

/**
 * <code>GroupAdministratorTest</code>...
 */
public class GroupAdministratorTest extends AbstractUserTest {

    // group-admin
    private String uID;
    private Session uSession;

    private String otherUID;
    private String otherUID2;
    private String grID;

    private String groupsPath;

    private Group groupAdmin;

    protected void setUp() throws Exception {
        super.setUp();

        // create a first user
        Principal p = getTestPrincipal();
        UserImpl pUser = (UserImpl) userMgr.createUser(p.getName(), buildPassword(p));
        save(superuser);
        otherUID = pUser.getID();

        // create a second user and make it group-admin
        p = getTestPrincipal();
        String pw = buildPassword(p);
        Credentials creds = buildCredentials(p.getName(), pw);
        User user = userMgr.createUser(p.getName(), pw);
        save(superuser);
        uID = user.getID();

        // make other user a group-administrator:
        Authorizable grAdmin = userMgr.getAuthorizable(UserConstants.GROUP_ADMIN_GROUP_NAME);
        if (grAdmin == null || !grAdmin.isGroup()) {
            throw new NotExecutableException("Cannot execute test. No group-administrator group found.");
        }
        groupAdmin = (Group) grAdmin;
        groupAdmin.addMember(user);
        save(superuser);
        grID = groupAdmin.getID();

        // create a session for the group-admin user.
        uSession = getHelper().getRepository().login(creds);

        groupsPath = (userMgr instanceof UserManagerImpl) ? ((UserManagerImpl) userMgr).getGroupsPath() : UserConstants.GROUPS_PATH;
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
            Authorizable a = userMgr.getAuthorizable(otherUID);
            if (a != null) {
                a.remove();
            }
            save(superuser);
        }
        super.tearDown();
    }

    private String getYetAnotherID() throws RepositoryException, NotExecutableException {
        if (otherUID2 == null) {
            // create a third user
            Principal p = getTestPrincipal();
            otherUID2 = userMgr.createUser(p.getName(), buildPassword(p)).getID();
            save(superuser);
        }
        return otherUID2;
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
            save(uSession);
            fail("Group administrator should not be allowed to create a new user.");
        } catch (AccessDeniedException e) {
            // success
        } finally {
            if (u != null) {
                u.remove();
                save(uSession);
            }
        }
    }

    public void testRemoveSelf() throws RepositoryException, NotExecutableException {
        UserManager umgr = getUserManager(uSession);

        Authorizable himself = umgr.getAuthorizable(uID);
        try {
            himself.remove();
            save(uSession);
            fail("A GroupAdministrator should not be allowed to remove the own authorizable.");
        } catch (AccessDeniedException e) {
            // success
        }
    }

    public void testRemoveGroupAdmin() throws RepositoryException, NotExecutableException {
        UserManager umgr = getUserManager(uSession);

        Authorizable groupAdmin = umgr.getAuthorizable(grID);
        try {
            groupAdmin.remove();
            save(uSession);
            fail("A GroupAdministrator should not be allowed to remove the group admin.");
        } catch (AccessDeniedException e) {
            // success
        }
    }

    public void testCreateGroup() throws RepositoryException, NotExecutableException {
        UserManager umgr = getUserManager(uSession);
        Group testGroup = null;
        try {
            testGroup = umgr.createGroup(getTestPrincipal());
            save(uSession);
            assertTrue(Text.isDescendant(groupsPath, ((GroupImpl)testGroup).getNode().getPath()));
        } finally {
            if (testGroup != null) {
                testGroup.remove();
                save(uSession);
            }
        }
    }

    public void testCreateGroupWithIntermediatePath() throws RepositoryException, NotExecutableException {
        UserManager umgr = getUserManager(uSession);
        Group testGroup = null;
        try {
            testGroup = umgr.createGroup(getTestPrincipal(), "/any/intermediate/path");
            save(uSession);
            assertTrue(Text.isDescendant(groupsPath + "/any/intermediate/path", ((GroupImpl)testGroup).getNode().getPath()));
        } finally {
            if (testGroup != null) {
                testGroup.remove();
                save(uSession);
            }
        }
    }

    public void testAddToGroup() throws RepositoryException, NotExecutableException {
        UserManager umgr = getUserManager(uSession);
        Authorizable cU = umgr.getAuthorizable(getYetAnotherID());
        Group gr = (Group) umgr.getAuthorizable(grID);

        // adding and removing the test user as member of a group must succeed.
        try {
            assertTrue("Modifying group membership requires GroupAdmin membership.",gr.addMember(cU));
            save(uSession);
        } finally {
            gr.removeMember(cU);
            save(uSession);
        }
    }

    public void testAddToGroup2() throws RepositoryException, NotExecutableException {
        UserManager umgr = getUserManager(uSession);
        Authorizable cU = umgr.getAuthorizable(getYetAnotherID());

        Group gr = (Group) umgr.getAuthorizable(groupAdmin.getID());
        assertTrue(gr.addMember(cU));
        save(uSession);
        assertTrue(gr.removeMember(cU));
        save(uSession);
    }

    public void testAddMembersToCreatedGroup() throws RepositoryException, NotExecutableException {
        UserManager umgr = getUserManager(uSession);
        Group testGroup = null;
        User self = (User) umgr.getAuthorizable(uID);
        try {
            // let groupadmin create a new group
            testGroup = umgr.createGroup(getTestPrincipal(), "/a/b/c/d");
            save(uSession);

            // editing session adds itself to that group -> must succeed.
            assertTrue(testGroup.addMember(self));
            save(uSession);

            // add child-user to test group
            Authorizable testUser = umgr.getAuthorizable(getYetAnotherID());
            assertFalse(testGroup.isMember(testUser));
            assertTrue(testGroup.addMember(testUser));
            save(uSession);
        } finally {
            if (testGroup != null) {
                for (Iterator<Authorizable> it = testGroup.getDeclaredMembers(); it.hasNext();) {
                    testGroup.removeMember(it.next());
                }
                testGroup.remove();
                save(uSession);
            }
        }
    }

    public void testAddMembersUserAdmins() throws RepositoryException, NotExecutableException {
        UserManager umgr = getUserManager(uSession);
        Authorizable auth = umgr.getAuthorizable(UserConstants.USER_ADMIN_GROUP_NAME);
        if (auth == null || !auth.isGroup()) {
            throw new NotExecutableException("Cannot execute test. No User-Admin group found.");
        }
        Group userAdmin = (Group) auth;
        Group testGroup = null;
        User self = (User) umgr.getAuthorizable(uID);
        try {

            userAdmin.addMember(self);
            save(uSession);

            userAdmin.removeMember(self);
            save(uSession);
            fail("Group admin cannot add member to user-admins");
        } catch (AccessDeniedException e) {
            // success
        }

        try {
            // let groupadmin create a new group
            testGroup = umgr.createGroup(getTestPrincipal(), "/a/b/c/d");
            save(uSession);

            userAdmin.addMember(testGroup);
            save(uSession);
            userAdmin.removeMember(testGroup);
            save(uSession);
            fail("Group admin cannot add member to user-admins");
        } catch (AccessDeniedException e) {
            // success
        } finally {
            if (testGroup != null) {
                testGroup.remove();
                save(uSession);
            }
        }
    }

    public void testAddOtherUserToGroup() throws RepositoryException, NotExecutableException {
        UserManager umgr = getUserManager(uSession);

        Authorizable pU = umgr.getAuthorizable(otherUID);
        Group gr = (Group) umgr.getAuthorizable(groupAdmin.getID());

        try {
            assertTrue(gr.addMember(pU));
            save(uSession);
        } finally {
            gr.removeMember(pU);
            save(uSession);
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
        Authorizable self = umgr.getAuthorizable(uID);

        Group gr = userMgr.createGroup(getTestPrincipal());
        save(superuser);

        try {
            assertTrue(((Group) umgr.getAuthorizable(gr.getID())).addMember(self));
            save(uSession);
            assertTrue(((Group) umgr.getAuthorizable(gr.getID())).removeMember(self));
            save(uSession);
        } finally {
            gr.remove();
            save(superuser);
        }
    }

    public void testRemoveMembersOfForeignGroup() throws RepositoryException, NotExecutableException {
        Group nGr = null;
        User nUs = null;
        User nUs2 = null;

        try {
            // let superuser create a group and a user a make user member of group
            nGr = userMgr.createGroup(getTestPrincipal());
            save(superuser);

            Principal p = getTestPrincipal();
            nUs = userMgr.createUser(p.getName(), buildPassword(p));
            save(superuser);

            p = getTestPrincipal();
            nUs2 = userMgr.createUser(p.getName(), buildPassword(p));
            save(superuser);
            nGr.addMember(nUs);
            nGr.addMember(nUs2);
            save(superuser);

            UserManager umgr = getUserManager(uSession);
            Group gr = (Group) umgr.getAuthorizable(nGr.getID());

            // removing any member must fail unless the testuser is user-admin
            Iterator<Authorizable> it = gr.getMembers();
            if (it.hasNext()) {
                Authorizable auth = it.next();

                String msg = "GroupAdmin must be able to modify group membership.";
                assertTrue(msg, gr.removeMember(auth));
                save(uSession);
            } else {
                fail("Must contain members....");
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
            save(superuser);
        }
    }

    public void testRemoveAllMembersOfForeignGroup() throws RepositoryException, NotExecutableException {
        Group nGr = null;
        User nUs = null;

        try {
            // let superuser create a group and a user a make user member of group
            nGr = userMgr.createGroup(getTestPrincipal());
            save(superuser);
            Principal p = getTestPrincipal();
            nUs = userMgr.createUser(p.getName(), buildPassword(p));
            nGr.addMember(nUs);
            save(superuser);

            UserManager umgr = getUserManager(uSession);
            Group gr = (Group) umgr.getAuthorizable(nGr.getID());

            // since only 1 single member -> removal rather than modification.
            // since uSession is not user-admin this must fail.
            for (Iterator<Authorizable> it = gr.getMembers(); it.hasNext();) {
                Authorizable auth = it.next();

                String msg = "GroupAdmin must be able to remove a member of another group.";
                assertTrue(msg, gr.removeMember(auth));
                save(uSession);
            }
        } catch (AccessDeniedException e) {
            // fine as well.
        } finally {
            // let superuser remove authorizables again
            if (nGr != null && nUs != null) nGr.removeMember(nUs);
            if (nGr != null) nGr.remove();
            if (nUs != null) nUs.remove();
            save(superuser);
        }
    }

    public void testImpersonationOfOtherUser() throws RepositoryException, NotExecutableException {
        UserManager umgr = getUserManager(uSession);
        Principal selfPrinc = umgr.getAuthorizable(uID).getPrincipal();

        User child = (User) umgr.getAuthorizable(getYetAnotherID());
        Impersonation impers = child.getImpersonation();
        assertFalse(impers.allows(buildSubject(selfPrinc)));
        try {
            assertFalse(impers.grantImpersonation(selfPrinc));
            save(uSession);
        } catch (AccessDeniedException e) {
            // ok.
        }
        assertFalse(impers.allows(buildSubject(selfPrinc)));

        User parent = (User) umgr.getAuthorizable(otherUID);
        impers = parent.getImpersonation();
        assertFalse(impers.allows(buildSubject(selfPrinc)));
        try {
            assertFalse(impers.grantImpersonation(selfPrinc));
            save(uSession);
        } catch (AccessDeniedException e) {
            // ok.
        }
        assertFalse(impers.allows(buildSubject(selfPrinc)));
    }

    public void testPersisted() throws NotExecutableException, RepositoryException {
        UserManager umgr = getUserManager(uSession);
        Group gr = null;
        try {
            Principal p = getTestPrincipal();
            gr = umgr.createGroup(p);
            save(uSession);

            // must be visible for the user-mgr attached to another session.
            Authorizable az = userMgr.getAuthorizable(gr.getID());
            assertNotNull(az);
            assertEquals(gr.getID(), az.getID());
        } finally {
            if (gr != null) {
                gr.remove();
                save(uSession);
            }
        }
    }

    public void testAddCustomNodeToGroupAdminNode() throws RepositoryException, NotExecutableException {
        UserManager umgr = getUserManager(uSession);
        Node groupAdminNode = ((AuthorizableImpl) umgr.getAuthorizable(grID)).getNode();
        Session s = groupAdminNode.getSession();

        Node n = groupAdminNode.addNode(nodeName1, ntUnstructured);
        save(uSession);

        n.setProperty(propertyName1, s.getValueFactory().createValue("anyValue"));
        save(uSession);

        n.remove();
        save(uSession);
    }
}