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
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.TestPrincipal;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.jcr.nodetype.ConstraintViolationException;
import java.security.Principal;
import java.util.Iterator;
import java.util.Set;

/**
 * <code>UserManagerImplTest</code>...
 */
public class UserManagerImplTest extends AbstractUserTest {

    private String pPrincipalName;
    private String pUserID;

    protected void setUp() throws Exception {
        super.setUp();
        if (!(userMgr instanceof UserManagerImpl)) {
            throw new NotExecutableException("UserManagerImpl expected -> cannot perform test.");
        }
        NameResolver resolver = (SessionImpl) superuser;
        pPrincipalName = resolver.getJCRName(UserConstants.P_PRINCIPAL_NAME);
        pUserID = resolver.getJCRName(UserConstants.P_USERID);
    }

    private String getTestUserId(Principal p) throws RepositoryException {
        String hint = "UID" + p.getName();
        String userId = hint;
        int i = 0;
        while (userMgr.getAuthorizable(userId) != null) {
            userId = hint + i++;
        }
        return userId;
    }

    public void testCreateNodesDirectly() throws NotExecutableException, RepositoryException {
        User u = getTestUser(superuser);
        if (u instanceof UserImpl) {
            throw new NotExecutableException();
        }

        NodeImpl n = ((UserImpl)u).getNode();
        try {
            n.addNode("anyname", "rep:AuthorizableFolder");
            fail("security nodes must be protected.");
        } catch (ConstraintViolationException e) {
            // success
        } finally {
            n.refresh(false);
        }
        try {
            n.addNode("anyname", "rep:User");
            fail("security nodes must be protected.");
        } catch (ConstraintViolationException e) {
            // success
        } finally {
            n.refresh(false);
        }
        try {
            n.setProperty("rep:userId", "someotherUID");
            fail("security nodes must be protected.");
        } catch (ConstraintViolationException e) {
            // success
        } finally {
            n.refresh(false);
        }
    }


    public void testRemoveUserRemovesTree() throws RepositoryException {
        // create 2 new users. the second as child of the first.
        Principal p = getTestPrincipal();
        User u = userMgr.createUser(p.getName(), buildPassword(p));
        String uID = u.getID();
        p = getTestPrincipal();
        User u2 = userMgr.createUser(p.getName(), buildPassword(p), p, ((UserImpl)u).getNode().getPath());
        String u2ID = u2.getID();

        // removing the first user must also remove the child-users.
        u.remove();

        // make sure both users are gone
        assertNull(userMgr.getAuthorizable(uID));
        assertNull(userMgr.getAuthorizable(u2ID));
    }

    public void testPrincipalNameEqualsUserID() throws RepositoryException {
        Principal p = getTestPrincipal();
        User u = null;
        try {
            u = userMgr.createUser(p.getName(), buildPassword(p));

            String msg = "Implementation specific: User.getID() must return the userID pass to createUser.";
            assertEquals(msg, u.getID(), p.getName());
        } finally {
            if (u != null) {
                u.remove();
            }
        }
    }

    public void testUserIDFromSession() throws RepositoryException {
        Principal p = getTestPrincipal();
        User u = null;
        Session uSession = null;
        try {
            String uid = p.getName();
            String pw = buildPassword(p);
            u = userMgr.createUser(uid, pw);

            uSession = superuser.getRepository().login(new SimpleCredentials(uid, pw.toCharArray()));
            assertEquals(u.getID(), uSession.getUserID());
        } finally {
            if (uSession != null) {
                uSession.logout();
            }
            if (u != null) {
                u.remove();
            }
        }
    }

    public void testCreateUserIdDifferentFromPrincipalName() throws RepositoryException {
        Principal p = getTestPrincipal();
        String uid = getTestUserId(p);
        String pw = buildPassword(uid, true);

        User u = null;
        Session uSession = null;
        try {
            u = userMgr.createUser(uid, pw, p, null);

            String msg = "Creating a User with principal-name distinct from Principal-name must succeed as long as both are unique.";
            assertEquals(msg, u.getID(), uid);
            assertEquals(msg, p.getName(), u.getPrincipal().getName());
            assertFalse(msg, u.getID().equals(u.getPrincipal().getName()));

            // make sure the userID exposed by a Session corresponding to that
            // user is equal to the users ID.
            uSession = superuser.getRepository().login(new SimpleCredentials(uid, pw.toCharArray()));
            assertEquals(uid, uSession.getUserID());
        } finally {
            if (uSession != null) {
                uSession.logout();
            }
            if (u != null) {
                u.remove();
            }
        }
    }

    public void testCreatingGroupWithNameMatchingExistingUserId() throws RepositoryException {
        Principal p = getTestPrincipal();
        String uid = getTestUserId(p);

        User u = null;
        Group gr = null;
        try {
            u = userMgr.createUser(uid, buildPassword(uid, true), p, null);
            gr = userMgr.createGroup(new TestPrincipal(uid));

            String msg = "Creating a Group with a principal-name that exists as UserID -> must create new GroupID but keep PrincipalName.";
            assertFalse(msg, gr.getID().equals(gr.getPrincipal().getName()));
            assertFalse(msg, gr.getID().equals(uid));
            assertFalse(msg, gr.getID().equals(u.getID()));
            assertEquals(msg, uid, gr.getPrincipal().getName());
        } finally {
            if (u != null) {
                u.remove();
            }
            if (gr != null) {
                gr.remove();
            }
        }
    }

    public void testFindAuthorizable() throws RepositoryException, NotExecutableException {
        Authorizable auth;
        Set principals = getPrincipalSetFromSession(superuser);
        for (Iterator it = principals.iterator(); it.hasNext();) {
            Principal p = (Principal) it.next();
            auth = userMgr.getAuthorizable(p);

            if (auth != null) {
                if (!auth.isGroup() && auth.hasProperty("rep:userId")) {
                    String val = auth.getProperty("rep:userId")[0].getString();
                    Iterator users = userMgr.findAuthorizables("rep:userId", val);

                    // the result must contain 1 authorizable
                    assertTrue(users.hasNext());
                    Authorizable first = (Authorizable) users.next();
                    assertEquals(first.getID(), val);

                    // since id is unique -> there should be no more auths in
                    // the iterator left
                    assertFalse(users.hasNext());
                }
            }
        }
    }

    public void testFindAuthorizableByAddedProperty() throws RepositoryException {
        Principal p = getTestPrincipal();
        Authorizable auth = null;

        try {
            auth= userMgr.createGroup(p);
            auth.setProperty("E-Mail", new Value[] { superuser.getValueFactory().createValue("anyVal")});

            boolean found = false;
            Iterator result = userMgr.findAuthorizables("E-Mail", "anyVal");
            while (result.hasNext()) {
                Authorizable a = (Authorizable) result.next();
                if (a.getID().equals(auth.getID())) {
                    found = true;
                }
            }

            assertTrue(found);
        } finally {
            // remove the create group again.
            if (auth != null) {
                auth.remove();
            }
        }
    }

    public void testFindUser() throws RepositoryException {
        User u = null;
        try {
            Principal p = getTestPrincipal();
            String uid = "UID" + p.getName();
            u = userMgr.createUser(uid, buildPassword(uid, false), p, null);

            boolean found = false;
            Iterator it = userMgr.findAuthorizables(pPrincipalName, null, UserManager.SEARCH_TYPE_USER);
            while (it.hasNext() && !found) {
                User nu = (User) it.next();
                found = nu.getID().equals(uid);
            }
            assertTrue("Searching for 'null' must find the created user.", found);

            it = userMgr.findAuthorizables(pPrincipalName, p.getName(), UserManager.SEARCH_TYPE_USER);
            found = false;
            while (it.hasNext() && !found) {
                User nu = (User) it.next();
                found = nu.getPrincipal().getName().equals(p.getName());
            }
            assertTrue("Searching for principal-name must find the created user.", found);

            it = userMgr.findAuthorizables(pUserID, uid, UserManager.SEARCH_TYPE_USER);
            found = false;
            while (it.hasNext() && !found) {
                User nu = (User) it.next();
                found = nu.getID().equals(uid);
            }
            assertTrue("Searching for user id must find the created user.", found);

            // but search groups should not find anything
            it = userMgr.findAuthorizables(pPrincipalName, p.getName(), UserManager.SEARCH_TYPE_GROUP);
            assertFalse(it.hasNext());

            it = userMgr.findAuthorizables(pPrincipalName, null, UserManager.SEARCH_TYPE_GROUP);
            while (it.hasNext()) {
                if (((Authorizable) it.next()).getPrincipal().getName().equals(p.getName())) {
                    fail("Searching for Groups should never find a user");
                }
            }
        } finally {
            if (u != null) {
                u.remove();
            }
        }
    }

    public void testFindGroup() throws RepositoryException {
        Group gr = null;
        try {
            Principal p = getTestPrincipal();
            gr = userMgr.createGroup(p);

            boolean found = false;
            Iterator it = userMgr.findAuthorizables(pPrincipalName, null, UserManager.SEARCH_TYPE_GROUP);
            while (it.hasNext() && !found) {
                Group ng = (Group) it.next();
                found = ng.getPrincipal().getName().equals(p.getName());
            }
            assertTrue("Searching for \"\" must find the created group.", found);

            it = userMgr.findAuthorizables(pPrincipalName, p.getName(), UserManager.SEARCH_TYPE_GROUP);
            assertTrue(it.hasNext());
            Group ng = (Group) it.next();
            assertEquals("Searching for principal-name must find the created group.", p.getName(), ng.getPrincipal().getName());
            assertFalse("Only a single group must be found for a given principal name.", it.hasNext());

            // but search users should not find anything
            it = userMgr.findAuthorizables(pPrincipalName, p.getName(), UserManager.SEARCH_TYPE_USER);
            assertFalse(it.hasNext());

            it = userMgr.findAuthorizables(pPrincipalName, null, UserManager.SEARCH_TYPE_USER);
            while (it.hasNext()) {
                if (((Authorizable) it.next()).getPrincipal().getName().equals(p.getName())) {
                    fail("Searching for Users should never find a group");
                }
            }
        } finally {
            if (gr != null) {
                gr.remove();
            }
        }
    }

    public void testFindAllUsers() throws RepositoryException {
        Iterator it = userMgr.findAuthorizables(pPrincipalName, null, UserManager.SEARCH_TYPE_USER);
        while (it.hasNext()) {
            assertFalse(((Authorizable) it.next()).isGroup());
        }
    }

    public void testFindAllGroups() throws RepositoryException {
        Iterator it = userMgr.findAuthorizables(pPrincipalName, null, UserManager.SEARCH_TYPE_GROUP);
        while (it.hasNext()) {
            assertTrue(((Authorizable) it.next()).isGroup());
        }
    }

    public void testNewUserCanLogin() throws RepositoryException {
        String uid = getTestPrincipal().getName();
        String pw = buildPassword(uid, false);

        User u = null;
        Session s = null;
        try {
            u = userMgr.createUser(uid, pw);
            Credentials creds = new SimpleCredentials(uid, pw.toCharArray());
            s = superuser.getRepository().login(creds);
        } finally {
            if (u != null) {
                u.remove();
            }
            if (s != null) {
                s.logout();
            }
        }
    }

    public void testUnknownUserLogin() throws RepositoryException {
        String uid = getTestPrincipal().getName();
        assertNull(userMgr.getAuthorizable(uid));
        try {
            Session s = superuser.getRepository().login(new SimpleCredentials(uid, uid.toCharArray()));
            s.logout();

            fail("An unknown user should not be allowed to execute the login.");
        } catch (Exception e) {
            // ok.
        }
    }

    public void testCleanup() throws RepositoryException, NotExecutableException {
        Session s = helper.getSuperuserSession();
        try {
            UserManager umgr = getUserManager(s);
            s.logout();

            // after logging out the session, the user manager must have been
            // released as well and it's underlying session must not be available
            // any more -> accessing users must fail.
            try {
                umgr.getAuthorizable("any userid");
                fail("After having logged out the original session, the user manager must not be live any more.");
            } catch (RepositoryException e) {
                // success
            }
        } finally {
            if (s.isLive()) {
                s.logout();
            }
        }
    }

    public void testCleanupForAllWorkspaces() throws RepositoryException, NotExecutableException {
        String[] workspaceNames = superuser.getWorkspace().getAccessibleWorkspaceNames();

        for (int i = 0; i < workspaceNames.length; i++) {
            Session s = helper.getSuperuserSession(workspaceNames[i]);
            try {
                UserManager umgr = getUserManager(s);
                s.logout();

                // after logging out the session, the user manager must have been
                // released as well and it's underlying session must not be available
                // any more -> accessing users must fail.
                try {
                    umgr.getAuthorizable("any userid");
                    fail("After having logged out the original session, the user manager must not be live any more.");
                } catch (RepositoryException e) {
                    // success
                }
            } finally {
                if (s.isLive()) {
                    s.logout();
                }
            }
        }
    }
}