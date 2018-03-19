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

import org.apache.jackrabbit.api.security.principal.GroupPrincipal;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.RepositoryException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * <code>GroupTest</code>...
 */
public class GroupTest extends AbstractUserTest {

    private static void assertTrueIsMember(Iterator<Authorizable> members, Authorizable auth) throws RepositoryException {
        boolean contained = false;
        while (members.hasNext() && !contained) {
            Object next = members.next();
            assertTrue(next instanceof Authorizable);
            contained = ((Authorizable)next).getID().equals(auth.getID());
        }
        assertTrue("The given set of members must contain '" + auth.getID() + "'", contained);
    }

    private static void assertFalseIsMember(Iterator<Authorizable> members, Authorizable auth) throws RepositoryException {
        boolean contained = false;
        while (members.hasNext() && !contained) {
            Object next = members.next();
            assertTrue(next instanceof Authorizable);
            contained = ((Authorizable)next).getID().equals(auth.getID());
        }
        assertFalse("The given set of members must not contain '" + auth.getID() + "'", contained);
    }

    private static void assertTrueMemberOfContainsGroup(Iterator<Group> groups, Group gr) throws RepositoryException {
        boolean contained = false;
        while (groups.hasNext() && !contained) {
            Object next = groups.next();
            assertTrue(next instanceof Group);
            contained = ((Group)next).getID().equals(gr.getID());
        }
        assertTrue("All members of a group must contain that group upon 'memberOf'.", contained);
    }

    private static void assertFalseMemberOfContainsGroup(Iterator<Group> groups, Group gr) throws RepositoryException {
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
        Iterator<Authorizable> it = gr.getDeclaredMembers();
        assertNotNull(it);
        while (it.hasNext()) {
            assertTrue(it.next() != null);
        }
    }

    public void testGetMembers() throws NotExecutableException, RepositoryException {
        Group gr = getTestGroup(superuser);
        Iterator<Authorizable> it = gr.getMembers();
        assertNotNull(it);
        while (it.hasNext()) {
            assertTrue(it.next() != null);
        }
    }

    public void testGetMembersAgainstIsMember() throws NotExecutableException, RepositoryException {
        Group gr = getTestGroup(superuser);

        Iterator<Authorizable> it = gr.getMembers();
        while (it.hasNext()) {
            Authorizable auth = it.next();
            assertTrue(gr.isMember(auth));
        }
    }

    public void testGetMembersAgainstMemberOf() throws NotExecutableException, RepositoryException {
        Group gr = getTestGroup(superuser);

        Iterator<Authorizable> it = gr.getMembers();
        while (it.hasNext()) {
            Authorizable auth = it.next();
            assertTrueMemberOfContainsGroup(auth.memberOf(), gr);
        }
    }

    public void testGetDeclaredMembersAgainstDeclaredMemberOf() throws NotExecutableException, RepositoryException {
        Group gr = getTestGroup(superuser);

        Iterator<Authorizable> it = gr.getDeclaredMembers();
        while (it.hasNext()) {
            Authorizable auth = it.next();
            assertTrueMemberOfContainsGroup(auth.declaredMemberOf(), gr);
        }
    }

    public void testGetMembersContainsDeclaredMembers() throws NotExecutableException, RepositoryException {
        Group gr = getTestGroup(superuser);
        List<String> l = new ArrayList<String>();
        for (Iterator<Authorizable> it = gr.getMembers(); it.hasNext();) {
            l.add(it.next().getID());
        }
        for (Iterator<Authorizable> it = gr.getDeclaredMembers(); it.hasNext();) {
            assertTrue("All declared members must also be part of the Iterator " +
                    "returned upon getMembers()",l.contains(it.next().getID()));
        }
    }

    public void testAddMember() throws NotExecutableException, RepositoryException {
        User auth = getTestUser(superuser);
        Group newGroup = null;
        try {
            newGroup = userMgr.createGroup(getTestPrincipal());
            save(superuser);

            assertFalse(newGroup.isMember(auth));
            assertFalse(newGroup.removeMember(auth));
            save(superuser);

            assertTrue(newGroup.addMember(auth));
            save(superuser);
            assertTrue(newGroup.isMember(auth));
            assertTrue(newGroup.isMember(userMgr.getAuthorizable(auth.getID())));

        } finally {
            if (newGroup != null) {
                newGroup.removeMember(auth);
                newGroup.remove();
                save(superuser);
            }
        }
    }

    public void testAddMembers() throws NotExecutableException, RepositoryException {
        User auth = getTestUser(superuser);
        Group newGroup = null;
        int size = 100;
        List<User> users = new ArrayList<User>(size);
        try {
            newGroup = userMgr.createGroup(getTestPrincipal());
            save(superuser);

            for (int k = 0; k < size; k++) {
                users.add(userMgr.createUser("user_" + k, "pass_" + k));
            }
            save(superuser);

            for (User user : users) {
                assertTrue(newGroup.addMember(user));
            }
            save(superuser);

            for (User user : users) {
                assertTrue(newGroup.isMember(user));
            }

            for (User user : users) {
                assertTrue(newGroup.removeMember(user));
            }
            save(superuser);

            for (User user : users) {
                assertFalse(newGroup.isMember(user));
            }
        } finally {
            for (User user : users) {
                user.remove();
                save(superuser);
            }
            if (newGroup != null) {
                newGroup.removeMember(auth);
                newGroup.remove();
                save(superuser);
            }
        }
    }

    public void testAddRemoveMember() throws NotExecutableException, RepositoryException {
        User auth = getTestUser(superuser);
        Group newGroup1 = null;
        Group newGroup2 = null;
        try {
            newGroup1 = userMgr.createGroup(getTestPrincipal());
            newGroup2 = userMgr.createGroup(getTestPrincipal());
            save(superuser);

            assertFalse(newGroup1.isMember(auth));
            assertFalse(newGroup1.removeMember(auth));
            save(superuser);
            assertFalse(newGroup2.isMember(auth));
            assertFalse(newGroup2.removeMember(auth));
            save(superuser);

            assertTrue(newGroup1.addMember(auth));
            save(superuser);
            assertTrue(newGroup1.isMember(auth));
            assertTrue(newGroup1.isMember(userMgr.getAuthorizable(auth.getID())));

            assertTrue(newGroup2.addMember(auth));
            save(superuser);
            assertTrue(newGroup2.isMember(auth));
            assertTrue(newGroup2.isMember(userMgr.getAuthorizable(auth.getID())));

            assertTrue(newGroup1.removeMember(auth));
            save(superuser);
            assertTrue(newGroup2.removeMember(auth));
            save(superuser);

            assertTrue(newGroup1.addMember(auth));
            save(superuser);
            assertTrue(newGroup1.isMember(auth));
            assertTrue(newGroup1.isMember(userMgr.getAuthorizable(auth.getID())));
            assertTrue(newGroup1.removeMember(auth));
            save(superuser);


        } finally {
            if (newGroup1 != null) {
                newGroup1.removeMember(auth);
                newGroup1.remove();
                save(superuser);
            }
            if (newGroup2 != null) {
                newGroup2.removeMember(auth);
                newGroup2.remove();
                save(superuser);
            }
        }
    }

    public void testIsDeclaredMember() throws RepositoryException, NotExecutableException {
        User auth = getTestUser(superuser);
        Group newGroup1 = null;
        Group newGroup2 = null;
        try {
            newGroup1 = userMgr.createGroup(getTestPrincipal());
            newGroup2 = userMgr.createGroup(getTestPrincipal());
            save(superuser);

            assertFalse(newGroup1.isDeclaredMember(auth));
            assertFalse(newGroup2.isDeclaredMember(auth));

            assertTrue(newGroup2.addMember(auth));
            save(superuser);
            assertTrue(newGroup2.isDeclaredMember(auth));
            assertTrue(newGroup2.isDeclaredMember(userMgr.getAuthorizable(auth.getID())));

            assertTrue(newGroup1.addMember(newGroup2));
            save(superuser);
            assertTrue(newGroup1.isDeclaredMember(newGroup2));
            assertTrue(newGroup1.isDeclaredMember(userMgr.getAuthorizable(newGroup2.getID())));
            assertTrue(newGroup1.isMember(auth));
            assertTrue(newGroup1.isMember(userMgr.getAuthorizable(auth.getID())));
            assertFalse(newGroup1.isDeclaredMember(auth));
            assertFalse(newGroup1.isDeclaredMember(userMgr.getAuthorizable(auth.getID())));
        } finally {
            if (newGroup1 != null) {
                newGroup1.remove();
                save(superuser);
            }
            if (newGroup2 != null) {
                newGroup2.remove();
                save(superuser);
            }
        }
    }

    public void testAddMemberTwice() throws NotExecutableException, RepositoryException {
        User auth = getTestUser(superuser);
        Group newGroup = null;
        try {
            newGroup = userMgr.createGroup(getTestPrincipal());
            save(superuser);

            assertTrue(newGroup.addMember(auth));
            save(superuser);
            assertFalse(newGroup.addMember(auth));
            save(superuser);
            assertTrue(newGroup.isMember(auth));

        } finally {
            if (newGroup != null) {
                newGroup.removeMember(auth);
                newGroup.remove();
                save(superuser);
            }
        }
    }

    public void testAddMemberModifiesMemberOf() throws NotExecutableException, RepositoryException {
        User auth = getTestUser(superuser);
        Group newGroup = null;
        try {
            newGroup = userMgr.createGroup(getTestPrincipal());
            save(superuser);

            assertFalseMemberOfContainsGroup(auth.memberOf(), newGroup);
            assertTrue(newGroup.addMember(auth));
            save(superuser);

            assertTrueMemberOfContainsGroup(auth.declaredMemberOf(), newGroup);
            assertTrueMemberOfContainsGroup(auth.memberOf(), newGroup);
        } finally {
            if (newGroup != null) {
                newGroup.removeMember(auth);
                newGroup.remove();
                save(superuser);
            }
        }
    }

    public void testAddMemberModifiesGetMembers() throws NotExecutableException, RepositoryException {
        User auth = getTestUser(superuser);
        Group newGroup = null;
        try {
            newGroup = userMgr.createGroup(getTestPrincipal());
            save(superuser);

            assertFalseIsMember(newGroup.getMembers(), auth);
            assertFalseIsMember(newGroup.getDeclaredMembers(), auth);
            assertTrue(newGroup.addMember(auth));
            save(superuser);

            assertTrueIsMember(newGroup.getMembers(), auth);
            assertTrueIsMember(newGroup.getDeclaredMembers(), auth);
        } finally {
            if (newGroup != null) {
                newGroup.removeMember(auth);
                newGroup.remove();
                save(superuser);
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
            save(superuser);

            newGroup.addMember(newGroup2);
            save(superuser);
            assertTrue(newGroup.isMember(newGroup2));

            newGroup2.addMember(auth);
            save(superuser);

            // testuser must not be declared member of 'newGroup'
            assertFalseIsMember(newGroup.getDeclaredMembers(), auth);
            assertFalseMemberOfContainsGroup(auth.declaredMemberOf(), newGroup);

            // testuser must however be member of 'newGroup' (indirect).
            assertTrueIsMember(newGroup.getMembers(), auth);
            assertTrueMemberOfContainsGroup(auth.memberOf(), newGroup);

            // testuser cannot be removed from 'newGroup'
            assertFalse(newGroup.removeMember(auth));
            save(superuser);
        } finally {
            if (newGroup != null) {
                newGroup.removeMember(newGroup2);
                newGroup.remove();
                save(superuser);
            }
            if (newGroup2 != null) {
                newGroup2.removeMember(auth);
                newGroup2.remove();
                save(superuser);
            }
        }
    }

    public void testMembersInPrincipal() throws NotExecutableException, RepositoryException {
        User auth = getTestUser(superuser);
        Group newGroup = null;
        Group newGroup2 = null;
        try {
            newGroup = userMgr.createGroup(getTestPrincipal());
            newGroup2 = userMgr.createGroup(getTestPrincipal());
            save(superuser);

            newGroup.addMember(newGroup2);
            save(superuser);
            newGroup2.addMember(auth);
            save(superuser);

            GroupPrincipal ngPrincipal = (GroupPrincipal) newGroup.getPrincipal();
            GroupPrincipal ng2Principal = (GroupPrincipal) newGroup2.getPrincipal();

            assertFalse(ng2Principal.isMember(ngPrincipal));

            // newGroup2 must be member of newGroup's principal
            assertTrue(ngPrincipal.isMember(newGroup2.getPrincipal()));

            // testuser must be member of newGroup2's and newGroup's principal (indirect)
            assertTrue(ng2Principal.isMember(auth.getPrincipal()));
            assertTrue(ngPrincipal.isMember(auth.getPrincipal()));

        } finally {
            if (newGroup != null) {
                newGroup.removeMember(newGroup2);
                newGroup.remove();
                save(superuser);
            }
            if (newGroup2 != null) {
                newGroup2.removeMember(auth);
                newGroup2.remove();
                save(superuser);
            }
        }
    }

    public void testDeeplyNestedGroups() throws NotExecutableException, RepositoryException {
        Set<Group> groups = new HashSet<Group>();
        try {
            User auth = getTestUser(superuser);
            Group topGroup = userMgr.createGroup(getTestPrincipal());

            // Create chain of nested groups with auth member of bottom group
            Group bottomGroup = topGroup;
            for (int k = 0; k < 100; k++) {
                Group g = userMgr.createGroup(getTestPrincipal());
                groups.add(g);
                bottomGroup.addMember(g);
                bottomGroup = g;
            }
            bottomGroup.addMember(auth);

            // Check that every groups has exactly one member
            for (Group g : groups) {
                Iterator<Authorizable> declaredMembers = g.getDeclaredMembers();
                assertTrue(declaredMembers.hasNext());
                declaredMembers.next();
                assertFalse(declaredMembers.hasNext());
            }

            // Check that we get all members from the getMembers call
            HashSet<Group> allGroups = new HashSet<Group>(groups);
            for (Iterator<Authorizable> it = topGroup.getMembers(); it.hasNext(); ) {
                Authorizable a = it.next();
                assertTrue(a.equals(auth) || allGroups.remove(a));
            }
            assertTrue(allGroups.isEmpty());
        } finally {
            for (Group g : groups) {
                g.remove();
            }
        }
    }

    public void testInheritedMembers() throws Exception {
        Set<Authorizable> authorizables = new HashSet<Authorizable>();
        try {
            User testUser = userMgr.createUser(getTestPrincipal().getName(), "pw");
            authorizables.add(testUser);
            Group group1 = userMgr.createGroup(getTestPrincipal());
            authorizables.add(group1);
            Group group2 = userMgr.createGroup(getTestPrincipal());
            authorizables.add(group2);
            Group group3 = userMgr.createGroup(getTestPrincipal());

            group1.addMember(testUser);
            group2.addMember(testUser);
            group3.addMember(group1);
            group3.addMember(group2);

            Iterator<Authorizable> members = group3.getMembers();
            while (members.hasNext()) {
                Authorizable a = members.next();
                assertTrue(authorizables.contains(a));
                assertTrue(authorizables.remove(a));
            }

            assertTrue(authorizables.isEmpty());
        } finally {
            for (Authorizable a : authorizables) {
                a.remove();
            }
        }
    }

    public void testCyclicGroups() throws AuthorizableExistsException, RepositoryException, NotExecutableException {
        Group group1 = null;
        Group group2 = null;
        Group group3 = null;
        try {
            group1 = userMgr.createGroup(getTestPrincipal());
            group2 = userMgr.createGroup(getTestPrincipal());
            group3 = userMgr.createGroup(getTestPrincipal());

            group1.addMember(getTestUser(superuser));
            group2.addMember(getTestUser(superuser));

            assertTrue(group1.addMember(group2));
            assertTrue(group2.addMember(group3));
            assertFalse(group3.addMember(group1));
        }
        finally {
            if (group1 != null) group1.remove();
            if (group2 != null) group2.remove();
            if (group3 != null) group3.remove();
        }
    }

    public void testRemoveMemberTwice() throws NotExecutableException, RepositoryException {
        User auth = getTestUser(superuser);
        Group newGroup = null;
        try {
            newGroup = userMgr.createGroup(getTestPrincipal());
            save(superuser);

            assertTrue(newGroup.addMember(auth));
            save(superuser);
            assertTrue(newGroup.removeMember(userMgr.getAuthorizable(auth.getID())));
            save(superuser);
            assertFalse(newGroup.removeMember(auth));
            save(superuser);
        } finally {
            if (newGroup != null) {
                newGroup.remove();
                save(superuser);
            }
        }
    }

    public void testAddItselfAsMember() throws RepositoryException, NotExecutableException {
        Group newGroup = null;
        try {
            newGroup = userMgr.createGroup(getTestPrincipal());
            save(superuser);

            assertFalse(newGroup.addMember(newGroup));
            save(superuser);
            newGroup.removeMember(newGroup);
            save(superuser);
        } finally {
            if (newGroup != null) {
                newGroup.remove();
                save(superuser);
            }
        }
    }

    /**
     * Removing a GroupImpl must be possible even if there are still existing
     * members present.
     *
     * @throws RepositoryException if an error occurs
     * @throws NotExecutableException if not executable
     */
    public void testRemoveGroupIfMemberExist() throws RepositoryException, NotExecutableException {
        User auth = getTestUser(superuser);
        String newGroupId = null;

        try {
            Group newGroup = userMgr.createGroup(getTestPrincipal());
            save(superuser);
            newGroupId = newGroup.getID();

            assertTrue(newGroup.addMember(auth));
            newGroup.remove();
            save(superuser);
        } finally {
            Group gr = (Group) userMgr.getAuthorizable(newGroupId);
            if (gr != null) {
                gr.removeMember(auth);
                gr.remove();
                save(superuser);
            }
        }
    }

    public void testRemoveGroupClearsMembership() throws NotExecutableException, RepositoryException {
        User auth = getTestUser(superuser);
        Group newGroup = null;
        String groupId;
        try {
            newGroup = userMgr.createGroup(getTestPrincipal());
            groupId = newGroup.getID();
            save(superuser);

            assertTrue(newGroup.addMember(auth));
            save(superuser);

            boolean isMember = false;
            Iterator<Group> it = auth.declaredMemberOf();
            while (it.hasNext() && !isMember) {
                isMember = groupId.equals(it.next().getID());
            }
            assertTrue(isMember);

        } finally {
            if (newGroup != null) {
                newGroup.remove();
                save(superuser);
            }
        }

        Iterator<Group> it = auth.declaredMemberOf();
        while (it.hasNext()) {
            assertFalse(groupId.equals(it.next().getID()));
        }

        it = auth.memberOf();
        while (it.hasNext()) {
            assertFalse(groupId.equals(it.next().getID()));
        }
    }

}
