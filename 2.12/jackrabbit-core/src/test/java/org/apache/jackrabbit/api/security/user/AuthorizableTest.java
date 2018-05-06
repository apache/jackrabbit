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
import org.apache.jackrabbit.util.Text;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * <code>UserTest</code>...
 */
public class AuthorizableTest extends AbstractUserTest {

    public void testGetId() throws NotExecutableException, RepositoryException {
        User user = getTestUser(superuser);
        assertNotNull(user.getID());
    }

    public void testGroupGetId() throws NotExecutableException, RepositoryException {
        Group gr = getTestGroup(superuser);
        assertNotNull(gr.getID());
    }

    public void testGetPrincipalNotNull() throws RepositoryException, NotExecutableException {
        User user = getTestUser(superuser);
        assertNotNull(user.getPrincipal());
    }

    public void testGroupGetPrincipalNotNull() throws RepositoryException, NotExecutableException {
        Group gr = getTestGroup(superuser);
        assertNotNull(gr.getPrincipal());
    }

    public void testSetProperty() throws NotExecutableException, RepositoryException {
        Authorizable auth = getTestUser(superuser);

        // TODO: retrieve propname and value from config
        String propName = "Fullname";
        Value v = superuser.getValueFactory().createValue("Super User");
        try {
            auth.setProperty(propName, v);
            save(superuser);
        } catch (RepositoryException e) {
            throw new NotExecutableException("Cannot test 'Authorizable.setProperty'.");
        }

        try {
            boolean found = false;
            for (Iterator<String> it = auth.getPropertyNames(); it.hasNext() && !found;) {
                found = propName.equals(it.next());
            }
            assertTrue(found);

            found = false;
            for (Iterator<String> it = auth.getPropertyNames("."); it.hasNext() && !found;) {
                found = propName.equals(it.next());
            }
            assertTrue(found);

            assertTrue(auth.hasProperty(propName));
            assertTrue(auth.hasProperty("./" + propName));
            
            assertTrue(auth.getProperty(propName).length == 1);

            assertEquals(v, auth.getProperty(propName)[0]);
            assertEquals(v, auth.getProperty("./" + propName)[0]);

            assertTrue(auth.removeProperty(propName));
            assertFalse(auth.hasProperty(propName));
            
            save(superuser);
        } finally {
            // try to remove the property again even if previous calls failed.
            auth.removeProperty(propName);
            save(superuser);
        }
    }

    public void testSetMultiValueProperty() throws NotExecutableException, RepositoryException {
        Authorizable auth = getTestUser(superuser);

        // TODO: retrieve propname and values from config
        String propName = "Fullname";
        Value[] v = new Value[] {superuser.getValueFactory().createValue("Super User")};
        try {
            auth.setProperty(propName, v);
            save(superuser);
        } catch (RepositoryException e) {
            throw new NotExecutableException("Cannot test 'Authorizable.setProperty'.");
        }

        try {
            boolean found = false;
            for (Iterator<String> it = auth.getPropertyNames(); it.hasNext() && !found;) {
                found = propName.equals(it.next());
            }
            assertTrue(found);

            found = false;
            for (Iterator<String> it = auth.getPropertyNames("."); it.hasNext() && !found;) {
                found = propName.equals(it.next());
            }
            assertTrue(found);
            
            assertTrue(auth.hasProperty(propName));
            assertTrue(auth.hasProperty("./" + propName));
            
            assertEquals(Arrays.asList(v), Arrays.asList(auth.getProperty(propName)));
            assertEquals(Arrays.asList(v), Arrays.asList(auth.getProperty("./" + propName)));

            assertTrue(auth.removeProperty(propName));
            assertFalse(auth.hasProperty(propName));
            
            save(superuser);
        } finally {
            // try to remove the property again even if previous calls failed.
            auth.removeProperty(propName);
            save(superuser);
        }
    }

    public void testSetPropertyByRelPath() throws NotExecutableException, RepositoryException {
        Authorizable auth = getTestUser(superuser);
        Value[] v = new Value[] {superuser.getValueFactory().createValue("Super User")};

        List<String> relPaths = new ArrayList<String>();
        relPaths.add("testing/Fullname");
        relPaths.add("testing/Email");
        relPaths.add("testing/testing/testing/Fullname");
        relPaths.add("testing/testing/testing/Email");

        for (String relPath : relPaths) {
            try {
                auth.setProperty(relPath, v);
                save(superuser);

                assertTrue(auth.hasProperty(relPath));
                String propName = Text.getName(relPath);
                assertFalse(auth.hasProperty(propName));
            } finally {
                // try to remove the property even if previous calls failed.
                auth.removeProperty(relPath);
                save(superuser);
            }
        }
    }

    public void testSetPropertyInvalidRelativePath() throws NotExecutableException, RepositoryException {
        Authorizable auth = getTestUser(superuser);
        Value[] v = new Value[] {superuser.getValueFactory().createValue("Super User")};

        List<String> invalidPaths = new ArrayList<String>();
        // try setting outside of tree defined by the user.
        invalidPaths.add("../testing/Fullname");
        invalidPaths.add("../../testing/Fullname");
        invalidPaths.add("testing/testing/../../../Fullname");
        // try absolute path -> must fail
        invalidPaths.add("/testing/Fullname");

        for (String invalidRelPath : invalidPaths) {
            try {
                auth.setProperty(invalidRelPath, v);
                fail("Modifications outside of the scope of the authorizable must fail.");
            } catch (Exception e) {
                // success.
            }
        }
    }

    public void testGetPropertyByInvalidRelativePath() throws NotExecutableException, RepositoryException {
        Authorizable auth = getTestUser(superuser);

        List<String> wrongPaths = new ArrayList<String>();
        wrongPaths.add("../jcr:primaryType");
        wrongPaths.add("../../jcr:primaryType");
        wrongPaths.add("../testing/jcr:primaryType");
        for (String path : wrongPaths) {
            assertNull(auth.getProperty(path));
        }

        List<String> invalidPaths = new ArrayList<String>();
        invalidPaths.add("/testing/jcr:primaryType");
        invalidPaths.add("..");
        invalidPaths.add(".");
        invalidPaths.add(null);
        for (String invalidPath : invalidPaths) {
            try {
                assertNull(auth.getProperty(invalidPath));
            } catch (Exception e) {
                // success
            }
        }
    }

    public void testHasPropertyByInvalidRelativePath() throws NotExecutableException, RepositoryException {
        Authorizable auth = getTestUser(superuser);

        List<String> wrongPaths = new ArrayList<String>();
        wrongPaths.add("../jcr:primaryType");
        wrongPaths.add("../../jcr:primaryType");
        wrongPaths.add("../testing/jcr:primaryType");
        for (String path : wrongPaths) {
            assertFalse(auth.hasProperty(path));
        }


        List<String> invalidPaths = new ArrayList<String>();
        invalidPaths.add("..");
        invalidPaths.add(".");
        invalidPaths.add(null);

        for (String invalidPath : invalidPaths) {
            try {
                assertFalse(auth.hasProperty(invalidPath));
            } catch (Exception e) {
                // success
            }
        }
    }
    
    public void testGetPropertyNames() throws NotExecutableException, RepositoryException {
        Authorizable auth = getTestUser(superuser);

        // TODO: retrieve propname and value from config
        String propName = "Fullname";
        Value v = superuser.getValueFactory().createValue("Super User");
        try {
            auth.setProperty(propName, v);
            save(superuser);
        } catch (RepositoryException e) {
            throw new NotExecutableException("Cannot test 'Authorizable.setProperty'.");
        }

        try {
            for (Iterator<String> it = auth.getPropertyNames(); it.hasNext();) {
                String name = it.next();
                assertTrue(auth.hasProperty(name));
                assertNotNull(auth.getProperty(name));
            }
        } finally {
            // try to remove the property again even if previous calls failed.
            auth.removeProperty(propName);
            save(superuser);
        }
    }

    public void testGetPropertyNamesByRelPath() throws NotExecutableException, RepositoryException {
        Authorizable auth = getTestUser(superuser);

        // TODO: retrieve propname and value from config
        String relPath = "testing/Fullname";
        Value v = superuser.getValueFactory().createValue("Super User");
        try {
            auth.setProperty(relPath, v);
            save(superuser);
        } catch (RepositoryException e) {
            throw new NotExecutableException("Cannot test 'Authorizable.setProperty'.");
        }

        try {
            for (Iterator<String> it = auth.getPropertyNames(); it.hasNext();) {
                String name = it.next();
                assertFalse("Fullname".equals(name));
            }

            for (Iterator<String> it = auth.getPropertyNames("testing"); it.hasNext();) {
                String name = it.next();
                String rp = "testing/" + name;
                
                assertFalse(auth.hasProperty(name));
                assertNull(auth.getProperty(name));

                assertTrue(auth.hasProperty(rp));
                assertNotNull(auth.getProperty(rp));
            }
            for (Iterator<String> it = auth.getPropertyNames("./testing"); it.hasNext();) {
                String name = it.next();
                String rp = "testing/" + name;

                assertFalse(auth.hasProperty(name));
                assertNull(auth.getProperty(name));

                assertTrue(auth.hasProperty(rp));
                assertNotNull(auth.getProperty(rp));
            }
        } finally {
            // try to remove the property again even if previous calls failed.
            auth.removeProperty(relPath);
            save(superuser);
        }
    }

    public void testGetPropertyNamesByInvalidRelPath() throws NotExecutableException, RepositoryException {
        Authorizable auth = getTestUser(superuser);

        List<String> invalidPaths = new ArrayList<String>();
        invalidPaths.add("../");
        invalidPaths.add("../../");
        invalidPaths.add("../testing");
        invalidPaths.add("/testing");
        invalidPaths.add(null);

        for (String invalidRelPath : invalidPaths) {
            try {
                auth.getPropertyNames(invalidRelPath);
                fail("Calling Authorizable#getPropertyNames with " + invalidRelPath + " must fail.");
            } catch (Exception e) {
                // success
            }
        }
    }

    public void testGetNotExistingProperty() throws RepositoryException, NotExecutableException {
        Authorizable auth = getTestUser(superuser);
        String hint = "Fullname";
        String propName = hint;
        int i = 0;
        while (auth.hasProperty(propName)) {
            propName = hint + i;
            i++;
        }
        assertNull(auth.getProperty(propName));
        assertFalse(auth.hasProperty(propName));
    }

    public void testRemoveNotExistingProperty() throws RepositoryException, NotExecutableException {
        Authorizable auth = getTestUser(superuser);
        String hint = "Fullname";
        String propName = hint;
        int i = 0;
        while (auth.hasProperty(propName)) {
            propName = hint + i;
            i++;
        }
        assertFalse(auth.removeProperty(propName));
        save(superuser);
    }

    public void testMemberOf() throws NotExecutableException, RepositoryException {
        Authorizable auth = getTestUser(superuser);

        Iterator<Group> it = auth.memberOf();
        while (it.hasNext()) {
            Object group = it.next();
            assertTrue(group instanceof Group);
        }
    }

    public void testDeclaredMemberOf() throws NotExecutableException, RepositoryException {
        Authorizable auth = getTestUser(superuser);

        Iterator<Group> it = auth.declaredMemberOf();
        while (it.hasNext()) {
            Object group = it.next();
            assertTrue(group instanceof Group);
        }
    }

    /**
     * Removing an authorizable that is still listed as member of a group.
     * @throws javax.jcr.RepositoryException
     * @throws org.apache.jackrabbit.test.NotExecutableException
     */
    public void testRemoveListedAuthorizable() throws RepositoryException, NotExecutableException {
        String newUserId = null;
        Group newGroup = null;

        try {
            Principal uP = getTestPrincipal();
            User newUser = userMgr.createUser(uP.getName(), uP.getName());
            save(superuser);
            newUserId = newUser.getID();

            newGroup = userMgr.createGroup(getTestPrincipal());
            newGroup.addMember(newUser);
            save(superuser);

            // remove the new user that is still listed as member.
            newUser.remove();
            save(superuser);
        } finally {
            if (newUserId != null) {
                Authorizable u = userMgr.getAuthorizable(newUserId);
                if (u != null) {
                    if (newGroup != null) {
                        newGroup.removeMember(u);
                    }
                    u.remove();
                }
            }
            if (newGroup != null) {
                newGroup.remove();
            }
            save(superuser);
        }
    }

    public void testRecreateUser() throws RepositoryException, NotExecutableException {
        String id = "bla";
        Authorizable auth = userMgr.getAuthorizable(id);
        if (auth == null) {
            auth = userMgr.createUser(id, id);
        }
        auth.remove();
        save(superuser);

        assertNull(userMgr.getAuthorizable(id));

        // recreate the user using another session.
        Session s2 = getHelper().getSuperuserSession();
        User u2 = null;
        try {
            UserManager umgr = ((JackrabbitSession) s2).getUserManager();
            assertNull(umgr.getAuthorizable(id));

            // recreation must succeed
            u2 = umgr.createUser(id, id);
            
            // must be present with both session.
            assertNotNull(umgr.getAuthorizable(id));
            assertNotNull(userMgr.getAuthorizable(id));

        } finally {
            if (u2 != null) {
                u2.remove();
                save(s2);
            }
            s2.logout();
        }
    }
}
