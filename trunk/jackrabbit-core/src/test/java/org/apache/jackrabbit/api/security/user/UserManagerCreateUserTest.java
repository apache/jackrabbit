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

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>UserManagerCreateGroupTest</code>...
 */
public class UserManagerCreateUserTest extends AbstractUserTest {

    private static Logger log = LoggerFactory.getLogger(UserManagerCreateUserTest.class);

    private List<Authorizable> createdUsers = new ArrayList<Authorizable>();

    @Override
    protected void tearDown() throws Exception {
        // remove all created groups again
        for (Object createdUser : createdUsers) {
            Authorizable auth = (Authorizable) createdUser;
            try {
                auth.remove();
                superuser.save();
            } catch (RepositoryException e) {
                log.warn("Failed to remove User " + auth.getID() + " during tearDown.");
            }
        }
        super.tearDown();
    }

    private User createUser(String uid, String pw) throws RepositoryException, NotExecutableException {
        User u = userMgr.createUser(uid, pw);
        save(superuser);
        return u;
    }

    private User createUser(String uid, String pw, Principal p, String iPath) throws RepositoryException, NotExecutableException {
        User u = userMgr.createUser(uid, pw, p, iPath);
        save(superuser);
        return u;
    }

    public void testCreateUser() throws RepositoryException, NotExecutableException {
        Principal p = getTestPrincipal();
        String uid = p.getName();
        User user = createUser(uid, buildPassword(uid));
        createdUsers.add(user);

        assertNotNull(user.getID());
        assertEquals(p.getName(), user.getPrincipal().getName());
    }

    public void testCreateUserWithPath() throws RepositoryException, NotExecutableException {
        Principal p = getTestPrincipal();
        String uid = p.getName();
        User user = createUser(uid, buildPassword(uid), p, "/any/path/to/the/new/user");
        createdUsers.add(user);

        assertNotNull(user.getID());
        assertEquals(p.getName(), user.getPrincipal().getName());
    }

    public void testCreateUserWithPath2() throws RepositoryException, NotExecutableException {
        Principal p = getTestPrincipal();
        String uid = p.getName();
        User user = createUser(uid, buildPassword(uid), p, "any/path");
        createdUsers.add(user);

        assertNotNull(user.getID());
        assertEquals(p.getName(), user.getPrincipal().getName());
    }

    public void testCreateUserWithDifferentPrincipalName() throws RepositoryException, NotExecutableException {
        Principal p = getTestPrincipal();
        String uid = getTestPrincipal().getName();
        User user = createUser(uid, buildPassword(uid), p, "/any/path/to/the/new/user");
        createdUsers.add(user);

        assertNotNull(user.getID());
        assertEquals(p.getName(), user.getPrincipal().getName());
    }

    public void testCreateUserWithNullParamerters() throws RepositoryException {
        try {
            User user = createUser(null, null);
            createdUsers.add(user);

            fail("A User cannot be built from 'null' parameters");
        } catch (Exception e) {
            // ok
        }

        try {
            User user = createUser(null, null, null, null);
            createdUsers.add(user);

            fail("A User cannot be built from 'null' parameters");
        } catch (Exception e) {
            // ok
        }
    }

    public void testCreateUserWithNullUserID() throws RepositoryException {
        try {
            User user = createUser(null, "anyPW");
            createdUsers.add(user);

            fail("A User cannot be built with 'null' userID");
        } catch (Exception e) {
            // ok
        }
    }

    public void testCreateUserWithEmptyUserID() throws RepositoryException {
        try {
            User user = createUser("", "anyPW");
            createdUsers.add(user);

            fail("A User cannot be built with \"\" userID");
        } catch (Exception e) {
            // ok
        }
        try {
            User user = createUser("", "anyPW", getTestPrincipal(), null);
            createdUsers.add(user);

            fail("A User cannot be built with \"\" userID");
        } catch (Exception e) {
            // ok
        }
    }

    /**
     * Test for changed behavior that allows creating of users with 'null' password.
     *
     * @since Jackrabbit 2.7
     */
    public void testCreateUserWithNullPassword() throws RepositoryException, NotExecutableException {
        Principal p = getTestPrincipal();
        User user = createUser(p.getName(), null);
        createdUsers.add(user);
    }

    public void testCreateUserWithEmptyPassword() throws RepositoryException, NotExecutableException {
        Principal p = getTestPrincipal();
        User user = createUser(p.getName(), "");
        createdUsers.add(user);
    }

    public void testCreateUserWithNullPrincipal() throws RepositoryException {
        try {
            Principal p = getTestPrincipal();
            String uid = p.getName();
            User user = createUser(uid, buildPassword(uid), null, "/a/b/c");
            createdUsers.add(user);

            fail("A User cannot be built with 'null' Principal");
        } catch (Exception e) {
            // ok
        }
    }

    public void testCreateUserWithEmptyPrincipal() throws RepositoryException {
        try {
            Principal p = getTestPrincipal("");
            String uid = p.getName();
            User user = createUser(uid, buildPassword(uid), p, "/a/b/c");
            createdUsers.add(user);

            fail("A User cannot be built with ''-named Principal");
        } catch (Exception e) {
            // ok
        }
        try {
            Principal p = getTestPrincipal(null);
            String uid = p.getName();
            User user = createUser(uid, buildPassword(uid), p, "/a/b/c");
            createdUsers.add(user);

            fail("A User cannot be built with ''-named Principal");
        } catch (Exception e) {
            // ok
        }
    }

    public void testCreateTwiceWithSameUserID() throws RepositoryException, NotExecutableException {
        String uid = getTestPrincipal().getName();
        User user = createUser(uid, buildPassword(uid));
        createdUsers.add(user);

        try {
            User user2 = createUser(uid, buildPassword("anyPW"));
            createdUsers.add(user2);

            fail("Creating 2 users with the same UserID should throw AuthorizableExistsException.");
        } catch (AuthorizableExistsException e) {
            // success.
        }
    }

    public void testCreateTwiceWithSamePrincipal() throws RepositoryException, NotExecutableException {
        Principal p = getTestPrincipal();
        String uid = p.getName();
        User user = createUser(uid, buildPassword(uid), p, "a/b/c");
        createdUsers.add(user);

        try {
            uid = getTestPrincipal().getName();
            User user2 = createUser(uid, buildPassword(uid), p, null);
            createdUsers.add(user2);

            fail("Creating 2 users with the same Principal should throw AuthorizableExistsException.");
        } catch (AuthorizableExistsException e) {
            // success.
        }
    }

    public void testGetUserAfterCreation() throws RepositoryException, NotExecutableException {
        Principal p = getTestPrincipal();
        String uid = p.getName();

        User user = createUser(uid, buildPassword(uid));
        createdUsers.add(user);

        assertNotNull(userMgr.getAuthorizable(user.getID()));
        assertNotNull(userMgr.getAuthorizable(p));
    }


    public void testAutoSave() throws RepositoryException {
        boolean autosave = userMgr.isAutoSave();
        if (autosave) {
            try {
                userMgr.autoSave(false);
                autosave = false;
            } catch (RepositoryException e) {
                // cannot change autosave behavior
                // ignore -> test will behave differently.
            }
        }

        Principal p = getTestPrincipal();
        String uid = p.getName();

        User user = userMgr.createUser(uid, buildPassword(uid));
        superuser.refresh(false);

        if (!autosave) {
            // transient changes must be gone after the refresh-call.
            assertNull(userMgr.getAuthorizable(uid));
            assertNull(userMgr.getAuthorizable(p));
        } else {
            // changes are persisted automatically -> must not be gone.
            createdUsers.add(user);
            assertNotNull(userMgr.getAuthorizable(uid));
            assertNotNull(userMgr.getAuthorizable(p));
        }
    }
}
