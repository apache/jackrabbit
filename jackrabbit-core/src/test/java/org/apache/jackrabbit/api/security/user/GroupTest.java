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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <code>GroupTest</code>...
 */
public class GroupTest extends AbstractUserTest {

    private static Logger log = LoggerFactory.getLogger(GroupTest.class);

    private static void assertTrueIsMember(Iterator members, Authorizable auth) throws RepositoryException {
        boolean contained = false;
        while (members.hasNext() && !contained) {
            Object next = members.next();
            assertTrue(next instanceof Authorizable);
            contained = ((Authorizable)next).getID().equals(auth.getID());
        }
        assertTrue("The given set of members must contain " + auth.getID(), contained);
    }

    private static void assertFalseIsMember(Iterator members, Authorizable auth) throws RepositoryException {
        boolean contained = false;
        while (members.hasNext() && !contained) {
            Object next = members.next();
            assertTrue(next instanceof Authorizable);
            contained = ((Authorizable)next).getID().equals(auth.getID());
        }
        assertFalse("The given set of members must not contain " + auth.getID(), contained);
    }

    private static void assertTrueMemberOfContainsGroup(Iterator groups, Group gr) throws RepositoryException {
        boolean contained = false;
        while (groups.hasNext() && !contained) {
            Object next = groups.next();
            assertTrue(next instanceof Group);
            contained = ((Group)next).getID().equals(gr.getID());
        }
        assertTrue("All members of a group must contain that group upon 'memberOf'.", contained);
    }

    private static void assertFalseMemberOfContainsGroup(Iterator groups, Group gr) throws RepositoryException {
        boolean contained = false;
        while (groups.hasNext() && !contained) {
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

    public void testGetDeclaredMembers() throws NotExecutableException, RepositoryException {
        Group gr = getTestGroup(superuser);
        Iterator it = gr.getDeclaredMembers();
        assertNotNull(it);
        while (it.hasNext()) {
            assertTrue(it.next() instanceof Authorizable);
        }
    }

    public void testGetMembers() throws NotExecutableException, RepositoryException {
        Group gr = getTestGroup(superuser);
        Iterator it = gr.getMembers();
        assertNotNull(it);
        while (it.hasNext()) {
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
            assertTrueMemberOfContainsGroup(auth.memberOf(), gr);
        }
    }

    public void testGetDeclaredMembersAgainstDeclaredMemberOf() throws NotExecutableException, RepositoryException {
        Group gr = getTestGroup(superuser);

        Iterator it = gr.getDeclaredMembers();
        while (it.hasNext()) {
            Authorizable auth = (Authorizable) it.next();
            assertTrueMemberOfContainsGroup(auth.declaredMemberOf(), gr);
        }
    }

    public void testGetMembersContainsDeclaredMembers() throws NotExecutableException, RepositoryException {
        Group gr = getTestGroup(superuser);
        List l = new ArrayList();
        for (Iterator it = gr.getMembers(); it.hasNext();) {
            l.add(((Authorizable) it.next()).getID());
        }
        for (Iterator it = gr.getDeclaredMembers(); it.hasNext();) {
            assertTrue("All declared members must also be part of the Iterator " +
                    "returned upon getMembers()",l.contains(((Authorizable) it.next()).getID()));
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

            assertFalseMemberOfContainsGroup(auth.memberOf(), newGroup);
            assertTrue(newGroup.addMember(auth));
            assertTrueMemberOfContainsGroup(auth.declaredMemberOf(), newGroup);
            assertTrueMemberOfContainsGroup(auth.memberOf(), newGroup);
        } finally {
            if (newGroup != null) {
                newGroup.removeMember(auth);
                newGroup.remove();
            }
        }
    }

    public void testAddMemberModifiesGetMembers() throws NotExecutableException, RepositoryException {
        User auth = getTestUser(superuser);
        Group newGroup = null;
        try {
            newGroup = userMgr.createGroup(getTestPrincipal());

            assertFalseIsMember(newGroup.getMembers(), auth);
            assertFalseIsMember(newGroup.getDeclaredMembers(), auth);
            assertTrue(newGroup.addMember(auth));
            assertTrueIsMember(newGroup.getMembers(), auth);
            assertTrueIsMember(newGroup.getDeclaredMembers(), auth);
        } finally {
            if (newGroup != null) {
                newGroup.removeMember(auth);
                newGroup.remove();
            }
        }
    }

    public void testIndirectMembers() throws NotExecutableException, RepositoryException {
        User auth = getTestUser(superuser);
        Group newGroup = null;
        Group newGroup2 = null;
        try {
            newGroup = userMgr.createGroup(getTestPrincipal());
            newGroup2 = userMgr.createGroup(getTestPrincipal());

            newGroup.addMember(newGroup2);
            assertTrue(newGroup.isMember(newGroup2));

            newGroup2.addMember(auth);

            // testuser must not be declared member of 'newGroup'
            assertFalseIsMember(newGroup.getDeclaredMembers(), auth);
            assertFalseMemberOfContainsGroup(auth.declaredMemberOf(), newGroup);

            // testuser must however be member of 'newGroup' (indirect).
            assertTrueIsMember(newGroup.getMembers(), auth);
            assertTrueMemberOfContainsGroup(auth.memberOf(), newGroup);

            // testuser cannot be removed from 'newGroup'
            assertFalse(newGroup.removeMember(auth));
        } finally {
            if (newGroup != null) {
                newGroup.removeMember(newGroup2);
                newGroup.remove();
            }
            if (newGroup2 != null) {
                newGroup2.removeMember(auth);
                newGroup2.remove();
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
     * TODO: uncomment once membership-relation is stored as weak ref.
     * Removing a GroupImpl must be possible even if there are still existing
     * members present.
     *
     * @throws RepositoryException
     * @throws NotExecutableException
     */
    /*
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
    */
}