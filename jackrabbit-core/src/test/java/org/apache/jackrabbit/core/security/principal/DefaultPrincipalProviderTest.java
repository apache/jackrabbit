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
package org.apache.jackrabbit.core.security.principal;

import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.api.security.user.AbstractUserTest;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.core.security.user.UserManagerImpl;
import org.apache.jackrabbit.core.security.TestPrincipal;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.RepositoryException;
import java.security.Principal;
import java.util.Properties;
import java.util.Set;

/**
 * <code>DefaultPrincipalProviderTest</code>...
 */
public class DefaultPrincipalProviderTest extends AbstractUserTest {

    private PrincipalProvider principalProvider;

    protected void setUp() throws Exception {
        super.setUp();

        if (!(userMgr instanceof UserManagerImpl)) {
            throw new NotExecutableException();
        }

        principalProvider = new DefaultPrincipalProvider(superuser, (UserManagerImpl) userMgr);
        principalProvider.init(new Properties());
    }

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
}