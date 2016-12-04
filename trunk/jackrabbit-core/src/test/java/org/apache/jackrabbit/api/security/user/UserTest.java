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

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

/**
 * <code>UserTest</code>...
 */
public class UserTest extends AbstractUserTest {

    private static Logger log = LoggerFactory.getLogger(UserTest.class);

    public void testNotIsGroup() throws NotExecutableException, RepositoryException {
        User user = getTestUser(superuser);
        assertFalse(user.isGroup());
    }

    public void testSuperuserIsAdmin() throws NotExecutableException, RepositoryException {
        User user = getTestUser(superuser);
        assertTrue(user.isAdmin());
    }

    public void testReadOnlyIsntAdmin() throws NotExecutableException, RepositoryException {
        Session s = getHelper().getReadOnlySession();
        try {
            User user = getTestUser(s);
            assertFalse(user.isAdmin());
        } finally {
            s.logout();
        }
    }

    public void testUserHasCredentials() throws RepositoryException, NotExecutableException {
        User user = getTestUser(superuser);
        Credentials creds = user.getCredentials();
        assertTrue(creds != null);
    }

    public void testChangePassword() throws RepositoryException, NotExecutableException {
        String oldPw = getHelper().getProperty("javax.jcr.tck.superuser.pwd");
        if (oldPw == null) {
            // missing property
            throw new NotExecutableException();
        }

        User user = getTestUser(superuser);
        try {
            user.changePassword("pw");
            save(superuser);
            
            // make sure the user can login with the new pw
            Session s = getHelper().getRepository().login(new SimpleCredentials(user.getID(), "pw".toCharArray()));
            s.logout();
        } finally {
            user.changePassword(oldPw);
            save(superuser);
        }
    }

    public void testChangePassword2() throws RepositoryException, NotExecutableException {
        String oldPw = getHelper().getProperty("javax.jcr.tck.superuser.pwd");
        if (oldPw == null) {
            // missing property
            throw new NotExecutableException();
        }

        User user = getTestUser(superuser);
        try {
            user.changePassword("pw");
            save(superuser);

            Session s = getHelper().getRepository().login(new SimpleCredentials(user.getID(), oldPw.toCharArray()));
            s.logout();
            fail("superuser pw has changed. login must fail.");
        } catch (LoginException e) {
            // success
        } finally {
            user.changePassword(oldPw);
            save(superuser);
        }
    }

    public void testChangePasswordWithOldPassword() throws RepositoryException, NotExecutableException {
        String oldPw = getHelper().getProperty("javax.jcr.tck.superuser.pwd");
        if (oldPw == null) {
            // missing property
            throw new NotExecutableException();
        }

        User user = getTestUser(superuser);
        try {
            try {
                user.changePassword("pw", "wrongOldPw");
                save(superuser);
                fail("old password didn't match -> changePassword(String,String) should fail.");
            } catch (RepositoryException e) {
                // success.
            }

            user.changePassword("pw", oldPw);
            save(superuser);

            // make sure the user can login with the new pw
            Session s = getHelper().getRepository().login(new SimpleCredentials(user.getID(), "pw".toCharArray()));
            s.logout();
        } finally {
            user.changePassword(oldPw);
            save(superuser);
        }
    }

    public void testChangePasswordWithOldPassword2() throws RepositoryException, NotExecutableException {
        String oldPw = getHelper().getProperty("javax.jcr.tck.superuser.pwd");
        if (oldPw == null) {
            // missing property
            throw new NotExecutableException();
        }

        User user = getTestUser(superuser);
        try {
            user.changePassword("pw", oldPw);
            save(superuser);

            Session s = getHelper().getRepository().login(new SimpleCredentials(user.getID(), oldPw.toCharArray()));
            s.logout();
            fail("superuser pw has changed. login must fail.");
        } catch (LoginException e) {
            // success
        } finally {
            user.changePassword(oldPw);
            save(superuser);
        }
    }

    public void testDisable() throws Exception {
        boolean remove = false;
        Session s = getHelper().getReadOnlySession();

        User user = null;
        String userID = null;
        String pw = "";

        try {
            User readonlyUser = getTestUser(s);
            if (readonlyUser.isAdmin()) {
                // configured readonly user is admin
                // -> need to create another test user
                pw = "test";
                userID = getUserManager(superuser).createUser(getTestPrincipal().getName(), pw).getID();
                remove = true;
            } else {
                userID = readonlyUser.getID();
            }

            user = (User) getUserManager(superuser).getAuthorizable(userID);

            // by default a user isn't disabled
            assertFalse(user.isDisabled());
            assertNull(user.getDisabledReason());

            // disable user
            String reason = "readonly user is disabled!";
            user.disable(reason);
            save(superuser);
            assertTrue(user.isDisabled());
            assertEquals(reason, user.getDisabledReason());

            // user must still be retrievable from user manager
            assertNotNull(getUserManager(superuser).getAuthorizable(userID));
            // ... and from principal manager as well
            assertTrue(((JackrabbitSession) superuser).getPrincipalManager().hasPrincipal(user.getPrincipal().getName()));

            // -> login must fail
            try {
                Session ss = getHelper().getRepository().login(new SimpleCredentials(userID, pw.toCharArray()));
                ss.logout();
                fail("A disabled user must not be allowed to login any more");
            } catch (LoginException e) {
                // success
            }

            // -> impersonating this user must fail
            try {
                Session ss = superuser.impersonate(new SimpleCredentials(userID, new char[0]));
                ss.logout();
                fail("A disabled user cannot be impersonated any more.");
            } catch (LoginException e) {
                // success
            }
            
            // enable user again
            user.disable(null);
            save(superuser);
            assertFalse(user.isDisabled());

            // -> login must succeed again
            getHelper().getRepository().login(new SimpleCredentials(userID, pw.toCharArray())).logout();

        } finally {
            s.logout();

            if (user != null) {
                if (user.isDisabled()) {
                    user.disable(null);
                }

                if (remove) {
                    user.remove();
                    save(superuser);
                }
            }
        }
    }
}
