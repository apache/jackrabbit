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

import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.security.Principal;
import java.util.Arrays;
import java.util.Iterator;

/**
 * <code>UserTest</code>...
 */
public class AuthorizableTest extends AbstractUserTest {

    private static Logger log = LoggerFactory.getLogger(AuthorizableTest.class);

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

    public void testGetPrincipals() throws NotExecutableException, RepositoryException {
        User user = getTestUser(superuser);
        assertNotNull(user.getPrincipals());
        assertTrue(user.getPrincipals().getSize() > 0);
    }

    public void testGroupGetPrincipals() throws NotExecutableException, RepositoryException {
        Group gr = getTestGroup(superuser);
        assertNotNull(gr.getPrincipals());
        assertTrue(gr.getPrincipals().getSize() > 0);
    }

    public void testGetPrincipalsContainsPrincipal() throws RepositoryException, NotExecutableException {
        Authorizable auth = getTestUser(superuser);
        Principal p = auth.getPrincipal();
        PrincipalIterator it = auth.getPrincipals();

        while (it.hasNext()) {
            if (it.nextPrincipal().equals(p)) {
                // main principal is indeed present in the iterator.
                return;
            }
        }
        fail("Main principal (Authorizable.getPrincipal()) must be present in the iterator obtained by Authorizable.getPrincipals()");
    }

    public void testSetProperty() throws NotExecutableException, RepositoryException {
        Authorizable auth = getTestUser(superuser);

        // TODO: retrieve propname and value from config
        String propName = "Fullname";
        Value v = superuser.getValueFactory().createValue("Super User");
        try {
            auth.setProperty(propName, v);
        } catch (RepositoryException e) {
            throw new NotExecutableException("Cannot test 'Authorizable.setProperty'.");
        }

        try {
            boolean found = false;
            for (Iterator it = auth.getPropertyNames(); it.hasNext() && !found;) {
                found = propName.equals(it.next().toString());
            }
            assertTrue(found);
            assertTrue(auth.hasProperty(propName));
            assertTrue(auth.getProperty(propName).length == 1);
            assertEquals(v, auth.getProperty(propName)[0]);
            assertTrue(auth.removeProperty(propName));
        } finally {
            // try to remove the property again even if previous calls failed.
            auth.removeProperty(propName);
        }
    }

    public void testSetMultiValueProperty() throws NotExecutableException, RepositoryException {
        Authorizable auth = getTestUser(superuser);

        // TODO: retrieve propname and values from config
        String propName = "Fullname";
        Value[] v = new Value[] {superuser.getValueFactory().createValue("Super User")};
        try {
            auth.setProperty(propName, v);
        } catch (RepositoryException e) {
            throw new NotExecutableException("Cannot test 'Authorizable.setProperty'.");
        }

        try {
            boolean found = false;
            for (Iterator it = auth.getPropertyNames(); it.hasNext() && !found;) {
                found = propName.equals(it.next().toString());
            }
            assertTrue(found);
            assertTrue(auth.hasProperty(propName));
            assertEquals(Arrays.asList(v), Arrays.asList(auth.getProperty(propName)));
            assertTrue(auth.removeProperty(propName));
        } finally {
            // try to remove the property again even if previous calls failed.
            auth.removeProperty(propName);
        }
    }

    public void testGetPropertyNames() throws NotExecutableException, RepositoryException {
        Authorizable auth = getTestUser(superuser);

        // TODO: retrieve propname and value from config
        String propName = "Fullname";
        Value v = superuser.getValueFactory().createValue("Super User");
        try {
            auth.setProperty(propName, v);
        } catch (RepositoryException e) {
            throw new NotExecutableException("Cannot test 'Authorizable.setProperty'.");
        }

        try {
            for (Iterator it = auth.getPropertyNames(); it.hasNext();) {
                String name = it.next().toString();
                assertTrue(auth.hasProperty(name));
                assertNotNull(auth.getProperty(name));
            }
        } finally {
            // try to remove the property again even if previous calls failed.
            auth.removeProperty(propName);
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
    }

    public void testMemberOf() throws NotExecutableException, RepositoryException {
        Authorizable auth = getTestUser(superuser);

        Iterator it = auth.memberOf();
        while (it.hasNext()) {
            Object group = it.next();
            assertTrue(group instanceof Group);
        }
    }

    public void testDeclaredMemberOf() throws NotExecutableException, RepositoryException {
        Authorizable auth = getTestUser(superuser);

        Iterator it = auth.declaredMemberOf();
        while (it.hasNext()) {
            Object group = it.next();
            assertTrue(group instanceof Group);
        }
    }

    public void testAddReferee() throws NotExecutableException, RepositoryException {
        Authorizable auth = getTestUser(superuser);

        Principal testPrincipal = getTestPrincipal();
        try {
            assertTrue(auth.addReferee(testPrincipal));
        } catch (AuthorizableExistsException e) {
            throw new NotExecutableException(e.getMessage());
        } finally {
            auth.removeReferee(testPrincipal);
        }
    }

    public void testAddRefereeTwice() throws NotExecutableException, RepositoryException {
        Authorizable auth = getTestUser(superuser);

        Principal testPrincipal = getTestPrincipal();
        try {
            auth.addReferee(testPrincipal);
            // adding same principal again must return false;
            assertFalse(auth.addReferee(testPrincipal));
        } finally {
            auth.removeReferee(testPrincipal);
        }
    }

    public void testGetPrincipalsExposeReferees() throws NotExecutableException, RepositoryException {
        Authorizable auth = getTestUser(superuser);

        Principal testPrincipal = getTestPrincipal();
        try {
            if (auth.addReferee(testPrincipal)) {
                // make sure the referee is part of principals-set
                boolean found = false;
                for (PrincipalIterator it = auth.getPrincipals(); it.hasNext() && !found;) {
                    found = testPrincipal.equals(it.nextPrincipal());
                }
                assertTrue("The referee added must be part of the 'getPrincipals()' iterator.", found);
            } else {
                throw new NotExecutableException();
            }
        } finally {
            auth.removeReferee(testPrincipal);
        }
    }

    public void testRemoveReferee() throws NotExecutableException, RepositoryException {
        Authorizable auth = getTestUser(superuser);

        Principal testPrincipal = getTestPrincipal();
        try {
            auth.addReferee(testPrincipal);
            assertTrue(auth.removeReferee(testPrincipal));
        } finally {
            auth.removeReferee(testPrincipal);
        }
    }

    public void testRefereeNotFoundByUserManager() throws NotExecutableException, RepositoryException {
        Authorizable auth = getTestUser(superuser);
        Principal testPrincipal = getTestPrincipal();

        try {
            assertTrue(auth.addReferee(testPrincipal));
            Authorizable a = userMgr.getAuthorizable(testPrincipal);
            assertNull(a);
        } catch (RepositoryException e) {
            auth.removeReferee(testPrincipal);
        }
    }

    public void testAddExistingReferee() throws NotExecutableException, RepositoryException {
        Authorizable auth = getTestUser(superuser);
        Authorizable auth2 = getTestGroup(superuser);
        Principal testPrincipal = getTestPrincipal();
        try {
            try {
                assertTrue(auth.addReferee(testPrincipal));
            } catch (AuthorizableExistsException e) {
                throw new NotExecutableException(e.getMessage());
            }

            // adding same principal to another authorizable must fail
            try {
                auth2.addReferee(testPrincipal);
                fail("Adding an existing referee-principal to another authorizable must fail.");
            } catch (AuthorizableExistsException e) {
                // success
            }
        } finally {
            auth.removeReferee(testPrincipal);
            auth2.removeReferee(testPrincipal);
        }
    }

    public void testAddMainPrincipalAsReferee() throws NotExecutableException, RepositoryException {
        Authorizable auth = getTestUser(superuser);
        Principal mainPrinc = auth.getPrincipal();
        try {
            // adding main principal as referee to another authorizable must
            // return false
            assertFalse(auth.addReferee(mainPrinc));
        } finally {
            auth.removeReferee(mainPrinc);
        }
    }

    public void testAddMainPrincipalAsOthersReferee() throws NotExecutableException, RepositoryException {
        Authorizable auth = getTestUser(superuser);
        Authorizable auth2 = getTestGroup(superuser);
        Principal mainPrinc = auth.getPrincipal();
        try {
            // adding main principal as referee to another authorizable must fail
            try {
                auth2.addReferee(mainPrinc);
                fail("Adding an existing main-principal as referee to another authorizable must fail.");
            } catch (AuthorizableExistsException e) {
                // success
            }
        } finally {
            auth2.removeReferee(mainPrinc);
        }
    }

    public void testRemoveNotExistingReferee() throws NotExecutableException, RepositoryException {
        Authorizable auth = getTestUser(superuser);
        Principal testPrincipal = getTestPrincipal();

        if (userMgr.getAuthorizable(testPrincipal) != null) {
            throw new NotExecutableException("test principal " + testPrincipal.getName() + " already associated with an authorizable.");
        }
        for (PrincipalIterator it = auth.getPrincipals(); it.hasNext();) {
            if (it.nextPrincipal().getName().equals(testPrincipal.getName())) {
                throw new NotExecutableException("test principal " + testPrincipal.getName() + " already referee.");
            }
        }
        assertFalse(auth.removeReferee(testPrincipal));
    }

    /**
     * Removing an authorizable that is still listed as member of a group.
     */
    /*
    public void testRemoveListedAuthorizable() throws RepositoryException, NotExecutableException {
        String newUserId = null;
        Group newGroup = null;

        try {
            Principal uP = getTestPrincipal();
            User newUser = userMgr.createUser(uP.getName(), buildCredentials(uP), uP);
            newUserId = newUser.getID();

            newGroup = userMgr.createGroup(getTestPrincipal());
            newGroup.addMember(newUser);

            // remove the new user that is still listed as member.
            newUser.remove();
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
        }
    }
    */
}
