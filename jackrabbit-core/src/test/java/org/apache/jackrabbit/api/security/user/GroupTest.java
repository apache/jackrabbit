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

import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.Iterator;

/**
 * <code>GroupTest</code>...
 */
public class GroupTest extends AbstractUserTest {

    private static Logger log = LoggerFactory.getLogger(GroupTest.class);

    private static void assertTrueMemberOfContainsGroup(Authorizable auth, Group gr) throws RepositoryException {
        boolean contained = false;
        for (Iterator groups = auth.memberOf(); groups.hasNext() && !contained;) {
            Object next = groups.next();
            assertTrue(next instanceof Group);
            contained = ((Group)next).getID().equals(gr.getID());
        }
        assertTrue("All members of a group must contain that group upon 'memberOf'.", contained);
    }

    private static void assertFalseMemberOfContainsGroup(Authorizable auth, Group gr) throws RepositoryException {
        boolean contained = false;
        for (Iterator groups = auth.memberOf(); groups.hasNext() && !contained;) {
            Object next = groups.next();
            assertTrue(next instanceof Group);
            contained = ((Group)next).getID().equals(gr.getID());
        }
        assertFalse("All members of a group must contain that group upon 'memberOf'.", contained);
    }

    public void testIsGroup() throws NotExecutableException, RepositoryException {
        Group gr = getTestGroup(superuser);
        assertTrue(gr.isGroup());
    }

    public void testGetMembersNotNull() throws NotExecutableException, RepositoryException {
        Group gr = getTestGroup(superuser);
        assertNotNull(gr.getMembers());
    }

    public void testGetMembersAreAuthorizable() throws NotExecutableException, RepositoryException {
        Group gr = getTestGroup(superuser);
        for (Iterator it = gr.getMembers(); it.hasNext();) {
            assertTrue(it.next() instanceof Authorizable);
        }
    }

    public void testGetMembersAgainstIsMember() throws NotExecutableException, RepositoryException {
        Group gr = getTestGroup(superuser);

        Iterator it = gr.getMembers();
        while (it.hasNext()) {
            Authorizable auth = (Authorizable) it.next();
            assertTrue(gr.isMember(auth));
        }
    }

    public void testGetMembersAgainstMemberOf() throws NotExecutableException, RepositoryException {
        Group gr = getTestGroup(superuser);

        Iterator it = gr.getMembers();
        while (it.hasNext()) {
            Authorizable auth = (Authorizable) it.next();
            assertTrueMemberOfContainsGroup(auth, gr);
        }
    }

    public void testAddMember() throws NotExecutableException, RepositoryException {
        User auth = getTestUser(superuser);
        Group newGroup = null;
        try {
            newGroup = userMgr.createGroup(getTestPrincipal());

            assertFalse(newGroup.isMember(auth));
            assertFalse(newGroup.removeMember(auth));

            assertTrue(newGroup.addMember(auth));
            assertTrue(newGroup.isMember(auth));
            assertTrue(newGroup.isMember(userMgr.getAuthorizable(auth.getID())));

        } finally {
            if (newGroup != null) {
                newGroup.removeMember(auth);
                newGroup.remove();
            }
        }
    }

    public void testAddMemberTwice() throws NotExecutableException, RepositoryException {
        User auth = getTestUser(superuser);
        Group newGroup = null;
        try {
            newGroup = userMgr.createGroup(getTestPrincipal());

            assertTrue(newGroup.addMember(auth));
            assertFalse(newGroup.addMember(auth));
            assertTrue(newGroup.isMember(auth));

        } finally {
            if (newGroup != null) {
                newGroup.removeMember(auth);
                newGroup.remove();
            }
        }
    }

    public void testAddMemberModifiesMemberOf() throws NotExecutableException, RepositoryException {
        User auth = getTestUser(superuser);
        Group newGroup = null;
        try {
            newGroup = userMgr.createGroup(getTestPrincipal());

            assertFalseMemberOfContainsGroup(auth, newGroup);
            assertTrue(newGroup.addMember(auth));
            assertTrueMemberOfContainsGroup(auth, newGroup);
        } finally {
            if (newGroup != null) {
                newGroup.removeMember(auth);
                newGroup.remove();
            }
        }
    }

    public void testRemoveMemberTwice() throws NotExecutableException, RepositoryException {
        User auth = getTestUser(superuser);
        Group newGroup = null;
        try {
            newGroup = userMgr.createGroup(getTestPrincipal());

            assertTrue(newGroup.addMember(auth));
            assertTrue(newGroup.removeMember(userMgr.getAuthorizable(auth.getID())));
            assertFalse(newGroup.removeMember(auth));
        } finally {
            if (newGroup != null) {
                newGroup.remove();
            }
        }
    }

    public void testAddItselfAsMember() throws RepositoryException {
        Group newGroup = null;
        try {
            newGroup = userMgr.createGroup(getTestPrincipal());

            assertFalse(newGroup.addMember(newGroup));
            newGroup.removeMember(newGroup);
        } finally {
            if (newGroup != null) {
                newGroup.remove();
            }
        }
    }

    /**
     * Removing a GroupImpl must be possible even if there are still exiting
     * members present.
     *
     * @throws RepositoryException
     * @throws NotExecutableException
     */
    public void testRemoveGroupIfMemberExist() throws RepositoryException, NotExecutableException {
        User auth = getTestUser(superuser);
        String newGroupId = null;

        try {
            Group newGroup = userMgr.createGroup(getTestPrincipal());
            newGroupId = newGroup.getID();

            assertTrue(newGroup.addMember(auth));
            newGroup.remove();

        } finally {
            Group gr = (Group) userMgr.getAuthorizable(newGroupId);
            if (gr != null) {
                gr.removeMember(auth);
                gr.remove();
            }
        }
    }
}