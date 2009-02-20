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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <code>UserManagerCreateGroupTest</code>...
 */
public class UserManagerCreateUserTest extends AbstractUserTest {

    private static Logger log = LoggerFactory.getLogger(UserManagerCreateUserTest.class);

    private List createdUsers = new ArrayList();

    protected void tearDown() throws Exception {
        // remove all created groups again
        for (Iterator it = createdUsers.iterator(); it.hasNext();) {
            Authorizable auth = (Authorizable) it.next();
            try {
                auth.remove();
            } catch (RepositoryException e) {
                log.error("Failed to remove User " + auth.getID() + " during tearDown.");
            }
        }
        super.tearDown();
    }

    public void testCreateUser() throws RepositoryException {
        Principal p = getTestPrincipal();
        String uid = p.getName();
        User user = userMgr.createUser(uid, buildPassword(uid, false));
        createdUsers.add(user);

        assertNotNull(user.getID());
        assertEquals(p.getName(), user.getPrincipal().getName());
    }

    public void testCreateUserWithPath() throws RepositoryException {
        Principal p = getTestPrincipal();
        String uid = p.getName();
        User user = userMgr.createUser(uid, buildPassword(uid, true), p, "/any/path/to/the/new/user");
        createdUsers.add(user);

        assertNotNull(user.getID());
        assertEquals(p.getName(), user.getPrincipal().getName());
    }

    public void testCreateUserWithPath2() throws RepositoryException {
        Principal p = getTestPrincipal();
        String uid = p.getName();
        User user = userMgr.createUser(uid, buildPassword(uid, true), p, "any/path");
        createdUsers.add(user);

        assertNotNull(user.getID());
        assertEquals(p.getName(), user.getPrincipal().getName());
    }

    public void testCreateUserWithDifferentPrincipalName() throws RepositoryException {
        Principal p = getTestPrincipal();
        String uid = getTestPrincipal().getName();
        User user = userMgr.createUser(uid, buildPassword(uid, true), p, "/any/path/to/the/new/user");
        createdUsers.add(user);

        assertNotNull(user.getID());
        assertEquals(p.getName(), user.getPrincipal().getName());
    }

    public void testCreateUserWithNullParamerters() throws RepositoryException {
        try {
            User user = userMgr.createUser(null, null);
            createdUsers.add(user);

            fail("A User cannot be built from 'null' parameters");
        } catch (Exception e) {
            // ok
        }

        try {
            User user = userMgr.createUser(null, null, null, null);
            createdUsers.add(user);

            fail("A User cannot be built from 'null' parameters");
        } catch (Exception e) {
            // ok
        }
    }

    public void testCreateUserWithNullUserID() throws RepositoryException {
        try {
            Principal p = getTestPrincipal();
            User user = userMgr.createUser(null, "anyPW");
            createdUsers.add(user);

            fail("A User cannot be built with 'null' userID");
        } catch (Exception e) {
            // ok
        }
    }

    public void testCreateUserWithEmptyUserID() throws RepositoryException {
        try {
            User user = userMgr.createUser("", "anyPW");
            createdUsers.add(user);

            fail("A User cannot be built with \"\" userID");
        } catch (Exception e) {
            // ok
        }
        try {
            User user = userMgr.createUser("", "anyPW", getTestPrincipal(), null);
            createdUsers.add(user);

            fail("A User cannot be built with \"\" userID");
        } catch (Exception e) {
            // ok
        }
    }

    public void testCreateUserWithNullPassword() throws RepositoryException {
        try {
            Principal p = getTestPrincipal();
            User user = userMgr.createUser(p.getName(), null);
            createdUsers.add(user);

            fail("A User cannot be built with 'null' password");
        } catch (Exception e) {
            // ok
        }
    }

    public void testCreateUserWithEmptyPassword() throws RepositoryException {
        Principal p = getTestPrincipal();
        User user = userMgr.createUser(p.getName(), "");
        createdUsers.add(user);
    }

    public void testCreateUserWithNullPrincipal() throws RepositoryException {
        try {
            Principal p = getTestPrincipal();
            String uid = p.getName();
            User user = userMgr.createUser(uid, buildPassword(uid, true), null, "/a/b/c");
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
            User user = userMgr.createUser(uid, buildPassword(uid, true), p, "/a/b/c");
            createdUsers.add(user);

            fail("A User cannot be built with ''-named Principal");
        } catch (Exception e) {
            // ok
        }
        try {
            Principal p = getTestPrincipal(null);
            String uid = p.getName();
            User user = userMgr.createUser(uid, buildPassword(uid, true), p, "/a/b/c");
            createdUsers.add(user);

            fail("A User cannot be built with ''-named Principal");
        } catch (Exception e) {
            // ok
        }
    }

    public void testCreateTwiceWithSameUserID() throws RepositoryException {
        String uid = getTestPrincipal().getName();
        User user = userMgr.createUser(uid, buildPassword(uid, false));
        createdUsers.add(user);

        try {
            User user2 = userMgr.createUser(uid, buildPassword("anyPW", true));
            createdUsers.add(user2);

            fail("Creating 2 users with the same UserID should throw AuthorizableExistsException.");
        } catch (AuthorizableExistsException e) {
            // success.
        }
    }

    public void testCreateTwiceWithSamePrincipal() throws RepositoryException {
        Principal p = getTestPrincipal();
        String uid = p.getName();
        User user = userMgr.createUser(uid, buildPassword(uid, true), p, "a/b/c");
        createdUsers.add(user);

        try {
            uid = getTestPrincipal().getName();
            User user2 = userMgr.createUser(uid, buildPassword(uid, false), p, null);
            createdUsers.add(user2);

            fail("Creating 2 users with the same Principal should throw AuthorizableExistsException.");
        } catch (AuthorizableExistsException e) {
            // success.
        }
    }

    public void testGetUserAfterCreation() throws RepositoryException {
        Principal p = getTestPrincipal();
        String uid = p.getName();

        User user = userMgr.createUser(uid, buildPassword(uid, false));
        createdUsers.add(user);

        assertNotNull(userMgr.getAuthorizable(user.getID()));
        assertNotNull(userMgr.getAuthorizable(p));
    }
}
