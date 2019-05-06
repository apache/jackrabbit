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

import java.security.Principal;
import java.util.Properties;
import java.util.Set;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.api.security.principal.GroupPrincipal;
import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.api.security.user.AbstractUserTest;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.core.security.TestPrincipal;
import org.apache.jackrabbit.core.security.principal.DefaultPrincipalProvider;
import org.apache.jackrabbit.core.security.principal.EveryonePrincipal;
import org.apache.jackrabbit.core.security.principal.PrincipalProvider;
import org.apache.jackrabbit.test.NotExecutableException;

/**
 * <code>DefaultPrincipalProviderTest</code>...
 */
public class DefaultPrincipalProviderTest extends AbstractUserTest {

    private PrincipalProvider principalProvider;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        if (!(userMgr instanceof UserManagerImpl)) {
            throw new NotExecutableException();
        }

        UserManagerImpl umgr = (UserManagerImpl) userMgr;
        // Workaround for testing cache behaviour that relies on observation:
        // - retrieve session attached to the userManager implementation.
        // - using superuser will not work if users are stored in a different workspace.
        Authorizable a = umgr.getAuthorizable(getPrincipalSetFromSession(superuser).iterator().next());
        Session s = ((AuthorizableImpl) a).getNode().getSession();
        principalProvider = new DefaultPrincipalProvider(s, umgr);
        principalProvider.init(new Properties());
    }

    @Override
    protected void tearDown() throws Exception {
        principalProvider.close();

        super.tearDown();
    }

    // TODO: add tests for 'findPrincipals'

    public void testUnknownUserMemberShip() throws RepositoryException {
        Principal userPrincipal = getTestPrincipal();

        PrincipalIterator pit = principalProvider.getGroupMembership(userPrincipal);

        // unknown user must be in 'everyone' group but nothing else
        assertTrue(pit.hasNext());
        assertEquals(EveryonePrincipal.getInstance(), pit.nextPrincipal());
        assertFalse(pit.hasNext());
    }

    public void testInheritedMemberShip() throws RepositoryException, NotExecutableException {
        Principal up = getTestPrincipal();

        User u = null;
        Group gr1 = null;
        Group gr2 = null;
        try {
            u = userMgr.createUser(up.getName(), buildPassword(up));
            gr1 = userMgr.createGroup(getTestPrincipal());
            gr2 = userMgr.createGroup(getTestPrincipal());
            save(superuser);


            gr1.addMember(gr2);
            gr2.addMember(u);
            save(superuser);

            PrincipalIterator it = principalProvider.getGroupMembership(u.getPrincipal());
            while (it.hasNext()) {
                Principal p = it.nextPrincipal();
                if (p.equals(gr1.getPrincipal())) {
                    // success return
                    return;
                }
            }

            fail("User principal " + up.getName() + " must have inherited group membership for " + gr1.getPrincipal().getName());

        } finally {
            if (gr2 != null && u != null) gr2.removeMember(u);
            if (gr1 != null && gr2 != null) gr1.removeMember(gr2);

            if (gr1 != null) gr1.remove();
            if (gr2 != null) gr2.remove();
            if (u != null) u.remove();
            save(superuser);
        }
    }

    /**
     *
     * @throws Exception
     */
    public void testCacheDoesntContainTestPrincipalImpl() throws Exception {
        Set<Principal> principals = getPrincipalSetFromSession(superuser);
        for (Principal p : principals) {
            Principal testPrinc = new TestPrincipal(p.getName());
            principalProvider.getGroupMembership(testPrinc);
            Principal fromProvider = principalProvider.getPrincipal(p.getName());

            assertNotSame(testPrinc, fromProvider);
            assertFalse(fromProvider instanceof TestPrincipal);
        }
    }

    /**
     * Test if cache is properly updated.
     * 
     * @throws Exception
     */
    public void testPrincipalCache() throws Exception {
        Principal testPrincipal = getTestPrincipal();
        String testName = testPrincipal.getName();

        assertNull(principalProvider.getPrincipal(testName));
        
        // create a user with the given principal name -> cache must be updated.
        Authorizable a = userMgr.createUser(testName, "pw");
        save(superuser);
        try {
            assertNotNull(principalProvider.getPrincipal(testName));
        } finally {
            a.remove();
            save(superuser);
        }

        // after removal -> entry must be removed from the cache.
        assertNull(principalProvider.getPrincipal(testName));

        // create a group with that name
        a = userMgr.createGroup(testPrincipal);
        save(superuser);        
        try {
            Principal p = principalProvider.getPrincipal(testName);
            assertNotNull(p);
            assertTrue(p instanceof GroupPrincipal);
        } finally {
            a.remove();
            save(superuser);
        }

        // recreate user again without filling cache with 'null' value
        a = userMgr.createUser(testName, "pw");
        save(superuser);
        try {
            Principal p = principalProvider.getPrincipal(testName);
            assertNotNull(p);
            assertFalse(p instanceof GroupPrincipal);
        } finally {
            a.remove();
            save(superuser);
        }
    }

    public void testEveryonePrincipal() throws Exception {
        Principal p = principalProvider.getPrincipal(EveryonePrincipal.NAME);
        assertNotNull(p);
        assertEquals(EveryonePrincipal.getInstance(), p);

        PrincipalIterator pit = principalProvider.findPrincipals(EveryonePrincipal.NAME);
        assertNotNull(pit);
        if (pit.getSize() == -1) {
            assertTrue(pit.hasNext());
            assertEquals(EveryonePrincipal.getInstance(), pit.nextPrincipal());
            assertFalse(pit.hasNext());
        } else {
            assertEquals(1, pit.getSize());
            assertEquals(EveryonePrincipal.getInstance(), pit.nextPrincipal());
        }
    }

    public void testEveryonePrincipal2() throws Exception {
        Group g = null;
        try {
            g = userMgr.createGroup(EveryonePrincipal.NAME);
            save(superuser);

            Principal p = principalProvider.getPrincipal(EveryonePrincipal.NAME);
            assertNotNull(p);
            assertEquals(EveryonePrincipal.getInstance(), p);

            PrincipalIterator pit = principalProvider.findPrincipals(EveryonePrincipal.NAME);
            assertNotNull(pit);
            if (pit.getSize() == -1) {
                assertTrue(pit.hasNext());
                assertEquals(EveryonePrincipal.getInstance(), pit.nextPrincipal());
                assertFalse(pit.hasNext());
            } else {
                assertEquals(1, pit.getSize());
                assertEquals(EveryonePrincipal.getInstance(), pit.nextPrincipal());
            }

        } finally {
            if (g != null) {
                g.remove();
                save(superuser);
            }
        }
    }

    /**
     * Test for: Principal assiocated with Group does not update members
     * @see <a href=https://issues.apache.org/jira/browse/JCR-3552>JCR-3552</a>
     */
    public void testGroupMembership() throws Exception {
        Group g = null;
        User u = null;
        Principal up = getTestPrincipal();
        try {
            // create a group and user, add the user to the group and assert membership
            g = userMgr.createGroup(getTestPrincipal());
            u = userMgr.createUser(up.getName(), buildPassword(up));
            save(superuser);
            g.addMember(u);
            save(superuser);

            Principal groupPrincipal = principalProvider.getPrincipal(g.getPrincipal().getName());
            assertTrue(groupPrincipal instanceof GroupPrincipal);
            assertTrue(((GroupPrincipal) groupPrincipal).isMember(u.getPrincipal()));

            // remove the user from the group and assert the user is no longer a member of the group
            g.removeMember(u);
            save(superuser);

            groupPrincipal = principalProvider.getPrincipal(g.getPrincipal().getName());
            assertFalse(((GroupPrincipal) groupPrincipal).isMember(u.getPrincipal()));
        } finally {
            if (null != g) { g.remove(); }
            if (null != u) { u.remove(); }
            save(superuser);
        }
    }
}