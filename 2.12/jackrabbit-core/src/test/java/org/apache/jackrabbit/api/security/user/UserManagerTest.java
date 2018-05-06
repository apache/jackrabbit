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

import org.apache.jackrabbit.commons.jackrabbit.user.AuthorizableQueryManager;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.util.Text;
import org.junit.Test;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * <code>UserManagerTest</code>...
 */
public class UserManagerTest extends AbstractUserTest {

    // TODO: add generic tests for UserManager.findAuthorizables
    // TODO: test creating users/groups if root is locked OR checked-in.

    public void testGetAuthorizableByPrincipal() throws RepositoryException, NotExecutableException {
        Authorizable auth = null;
        Set<Principal> principals = getPrincipalSetFromSession(superuser);
        for (Iterator<Principal> it = principals.iterator(); it.hasNext() && auth == null;) {
            Principal p = it.next();
            auth = userMgr.getAuthorizable(p);
        }
        assertNotNull("At least one of the Sessions principal must be a known authorizable to the UserManager", auth);
    }

    public void testGetAuthorizableById() throws RepositoryException, NotExecutableException {
        Authorizable auth = null;
        for (Principal principal : getPrincipalSetFromSession(superuser)) {
            Principal p = principal;
            auth = userMgr.getAuthorizable(p);

            if (auth != null) {
                Authorizable authByID = userMgr.getAuthorizable(auth.getID());
                assertEquals("Equal ID expected", auth.getID(), authByID.getID());
            }
        }
    }

    public void testGetAuthorizableByPath() throws RepositoryException, NotExecutableException {
        String uid = superuser.getUserID();
        Authorizable a = userMgr.getAuthorizable(uid);
        if (a == null) {
            throw new NotExecutableException();
        }
        try {
            String path = a.getPath();
            Authorizable a2 = userMgr.getAuthorizableByPath(path);
            assertNotNull(a2);
            assertEquals(a.getID(), a2.getID());
        } catch (UnsupportedRepositoryOperationException e) {
            throw new NotExecutableException();
        }
    }


    public void testGetAuthorizableByIdAndType() throws NotExecutableException, RepositoryException {
        for (Principal principal : getPrincipalSetFromSession(superuser)) {
            Principal p = principal;
            Authorizable a = userMgr.getAuthorizable(p);
            if (a != null) {
                Authorizable authorizable = userMgr.getAuthorizable(a.getID(), a.getClass());
                assertEquals("Equal ID expected", a.getID(), authorizable.getID());

                authorizable = userMgr.getAuthorizable(a.getID(), Authorizable.class);
                assertEquals("Equal ID expected", a.getID(), authorizable.getID());
            }
        }
    }

    public void testGetAuthorizableByIdAndWrongType() throws NotExecutableException, RepositoryException {
        for (Principal principal : getPrincipalSetFromSession(superuser)) {
            Principal p = principal;
            Authorizable auth = userMgr.getAuthorizable(p);
            if (auth != null) {
                Class<? extends Authorizable> otherType = auth.isGroup() ? User.class : Group.class;
                try {
                    userMgr.getAuthorizable(auth.getID(), otherType);
                    fail("Wrong Authorizable type is not detected.");
                } catch (AuthorizableTypeException e) {
                    // success
                }
            }
        }
    }

    public void testGetNonExistingAuthorizableByIdAndType() throws NotExecutableException, RepositoryException {
        Authorizable auth = userMgr.getAuthorizable("nonExistingAuthorizable", User.class);
        assertNull(auth);

        auth = userMgr.getAuthorizable("nonExistingAuthorizable", Authorizable.class);
        assertNull(auth);
    }

    public void testGetAuthorizableByNullType() throws Exception {
        String uid = superuser.getUserID();
        Authorizable auth = userMgr.getAuthorizable(uid);
        if (auth != null) {
            try {
                userMgr.getAuthorizable(uid, null);
                fail("Null Authorizable type is not detected.");
            } catch (AuthorizableTypeException e) {
                // success
            }
        }
    }

    public void testGetNonExistingAuthorizableByNullType() throws Exception {
        assertNull(userMgr.getAuthorizable("nonExistingAuthorizable", null));
    }

    @Test
    public void testFindUserWithSpecialCharIdByPrincipalName() throws RepositoryException {
        List<String> ids = Arrays.asList("'", Text.escapeIllegalJcrChars("']"), Text.escape("']"));
        for (String id : ids) {
            User user = null;
            try {
                user = userMgr.createUser(id, "pw");
                superuser.save();

                boolean found = false;
                Iterator<Authorizable> it = userMgr.findAuthorizables("rep:principalName", id, UserManager.SEARCH_TYPE_USER);
                while (it.hasNext() && !found) {
                    Authorizable a = it.next();
                    found = id.equals(a.getID());
                }
                assertTrue(found);
            } finally {
                if (user != null) {
                    user.remove();
                    superuser.save();
                }
            }
        }
    }

    @Test
    public void testFindUserWithSpecialCharIdByPrincipalName2() throws RepositoryException {
        List<String> ids = Arrays.asList("]");
        for (String id : ids) {
            User user = null;
            try {
                user = userMgr.createUser(id, "pw");
                superuser.save();

                boolean found = false;
                Iterator<Authorizable> it = userMgr.findAuthorizables("rep:principalName", id, UserManager.SEARCH_TYPE_USER);
                while (it.hasNext() && !found) {
                    Authorizable a = it.next();
                    found = id.equals(a.getID());
                }
                assertTrue(found);
            } finally {
                if (user != null) {
                    user.remove();
                    superuser.save();
                }
            }
        }
    }

    @Test
    public void testQueryUserWithSpecialCharId() throws Exception {
        List<String> ids = Arrays.asList("'", "]");
        for (String id : ids) {
            User user = null;
            try {
                user = userMgr.createUser(id, "pw");
                superuser.save();

                boolean found = false;
                String query = "{\"condition\":[{\"named\":\"" + id + "\"}]}";
                AuthorizableQueryManager queryManager = new AuthorizableQueryManager(userMgr, superuser.getValueFactory());
                Iterator<Authorizable> it = queryManager.execute(query);
                while (it.hasNext() && !found) {
                    Authorizable a = it.next();
                    found = id.equals(a.getID());
                }
                assertTrue(found);
            } finally {
                if (user != null) {
                    user.remove();
                    superuser.save();
                }
            }
        }
    }

}