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
package org.apache.jackrabbit.api.security.principal;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * <code>PrincipalManagerTest</code>...
 */
public class PrincipalManagerTest extends AbstractJCRTest {

    private PrincipalManager principalMgr;
    private GroupPrincipal everyone;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (!(superuser instanceof JackrabbitSession)) {
            superuser.logout();
            throw new NotExecutableException();
        }
        principalMgr = ((JackrabbitSession) superuser).getPrincipalManager();
        everyone = (GroupPrincipal) principalMgr.getEveryone();
    }

    private static Principal[] getPrincipals(Session session) {
        // TODO: get rid of SessionImpl dependency
        Set<Principal> princ = ((SessionImpl) session).getSubject().getPrincipals();
        return princ.toArray(new Principal[princ.size()]);
    }

    private static boolean isGroup(Principal p) {
        return p instanceof GroupPrincipal;
    }

    public void testGetEveryone() {
        Principal principal = principalMgr.getEveryone();
        assertTrue(principal != null);
        assertTrue(isGroup(principal));
    }

    public void testSuperUserIsEveryOne() {
        Principal[] pcpls = getPrincipals(superuser);
        for (Principal pcpl : pcpls) {
            if (!(pcpl.equals(everyone))) {
                assertTrue(everyone.isMember(pcpl));
            }
        }
    }

    public void testReadOnlyIsEveryOne() throws RepositoryException {
        Session s = getHelper().getReadOnlySession();
        try {
            Principal[] pcpls = getPrincipals(s);
            for (Principal pcpl : pcpls) {
                if (!(pcpl.equals(everyone))) {
                    assertTrue(everyone.isMember(pcpl));
                }
            }
        } finally {
            s.logout();
        }
    }

    public void testHasPrincipal() {
        assertTrue(principalMgr.hasPrincipal(everyone.getName()));

        Principal[] pcpls = getPrincipals(superuser);
        for (Principal pcpl : pcpls) {
            assertTrue(principalMgr.hasPrincipal(pcpl.getName()));
        }
    }

    public void testGetPrincipal() {
        Principal p = principalMgr.getPrincipal(everyone.getName());
        assertEquals(everyone, p);

        Principal[] pcpls = getPrincipals(superuser);
        for (Principal pcpl : pcpls) {
            Principal pp = principalMgr.getPrincipal(pcpl.getName());
            assertEquals("PrincipalManager.getPrincipal returned Principal with different Name", pcpl.getName(), pp.getName());
        }
    }

    public void testGetPrincipalGetName() {
        Principal[] pcpls = getPrincipals(superuser);
        for (Principal pcpl : pcpls) {
            Principal pp = principalMgr.getPrincipal(pcpl.getName());
            assertEquals("PrincipalManager.getPrincipal returned Principal with different Name", pcpl.getName(), pp.getName());
        }
    }

    public void testGetPrincipals() {
        PrincipalIterator it = principalMgr.getPrincipals(PrincipalManager.SEARCH_TYPE_NOT_GROUP);
        while (it.hasNext()) {
            Principal p = it.nextPrincipal();
            assertFalse(isGroup(p));
        }
    }

    public void testGetGroupPrincipals() {
        PrincipalIterator it = principalMgr.getPrincipals(PrincipalManager.SEARCH_TYPE_GROUP);
        while (it.hasNext()) {
            Principal p = it.nextPrincipal();
            assertTrue(isGroup(p));
        }
    }

    public void testGetAllPrincipals() {
        PrincipalIterator it = principalMgr.getPrincipals(PrincipalManager.SEARCH_TYPE_ALL);
        while (it.hasNext()) {
            Principal p = it.nextPrincipal();
            assertTrue(principalMgr.hasPrincipal(p.getName()));
            assertEquals(principalMgr.getPrincipal(p.getName()), p);
        }
    }

    public void testGroupMembers() {
        PrincipalIterator it = principalMgr.getPrincipals(PrincipalManager.SEARCH_TYPE_ALL);
        while (it.hasNext()) {
            Principal p = it.nextPrincipal();
            if (isGroup(p) && !p.equals(principalMgr.getEveryone())) {
                Enumeration<? extends Principal> en = ((GroupPrincipal) p).members();
                while (en.hasMoreElements()) {
                    Principal memb = en.nextElement();
                    assertTrue(principalMgr.hasPrincipal(memb.getName()));
                }
            }
        }
    }

    public void testGroupMembership() {
        testMembership(PrincipalManager.SEARCH_TYPE_NOT_GROUP);
        testMembership(PrincipalManager.SEARCH_TYPE_GROUP);
        testMembership(PrincipalManager.SEARCH_TYPE_ALL);
    }

    private void testMembership(int searchType) {
        PrincipalIterator it = principalMgr.getPrincipals(searchType);
        while (it.hasNext()) {
            Principal p = it.nextPrincipal();
            if (p.equals(everyone)) {
                for (PrincipalIterator membership = principalMgr.getGroupMembership(p); membership.hasNext();) {
                    Principal gr = membership.nextPrincipal();
                    assertTrue(isGroup(gr));
                    if (gr.equals(everyone)) {
                        fail("Everyone must never be a member of the EveryOne group.");
                    }
                }
            } else {
                boolean atleastEveryone = false;
                for (PrincipalIterator membership = principalMgr.getGroupMembership(p); membership.hasNext();) {
                    Principal gr = membership.nextPrincipal();
                    assertTrue(isGroup(gr));
                    if (gr.equals(everyone)) {
                        atleastEveryone = true;
                    }
                }
                assertTrue("All principals (except everyone) must be member of the everyone group.", atleastEveryone);

            }
        }
    }

    public void testGetMembersConsistentWithMembership() {
        Principal everyone = principalMgr.getEveryone();
        PrincipalIterator it = principalMgr.getPrincipals(PrincipalManager.SEARCH_TYPE_GROUP);
        while (it.hasNext()) {
            Principal p = it.nextPrincipal();
            if (p.equals(everyone)) {
                continue;
            }

            assertTrue(isGroup(p));

            Enumeration<? extends Principal> members = ((GroupPrincipal) p).members();
            while (members.hasMoreElements()) {
                Principal memb = members.nextElement();

                Principal group = null;
                PrincipalIterator mship = principalMgr.getGroupMembership(memb);
                while (mship.hasNext() && group == null) {
                    Principal gr = mship.nextPrincipal();
                    if (p.equals(gr)) {
                        group = gr;
                    }
                }
                assertNotNull("Group member " + memb.getName() + "does not reveal group upon getGroupMembership", p.getName());
            }
        }
    }

    public void testFindPrincipal() {
        Principal[] pcpls = getPrincipals(superuser);
        for (Principal pcpl : pcpls) {
            if (pcpl.equals(everyone)) {
                continue;
            }
            PrincipalIterator it = principalMgr.findPrincipals(pcpl.getName());
            // search must find at least a single principal
            assertTrue("findPrincipals does not find principal with filter " + pcpl.getName(), it.hasNext());
        }
    }

    public void testFindPrincipalByType() {
        Principal[] pcpls = getPrincipals(superuser);
        for (Principal pcpl : pcpls) {
            if (pcpl.equals(everyone)) {
                // special case covered by another test
                continue;
            }

            if (isGroup(pcpl)) {
                PrincipalIterator it = principalMgr.findPrincipals(pcpl.getName(),
                        PrincipalManager.SEARCH_TYPE_GROUP);
                // search must find at least a single matching group principal
                assertTrue("findPrincipals does not find principal with filter " + pcpl.getName(), it.hasNext());
            } else {
                PrincipalIterator it = principalMgr.findPrincipals(pcpl.getName(),
                        PrincipalManager.SEARCH_TYPE_NOT_GROUP);
                // search must find at least a single matching non-group principal
                assertTrue("findPrincipals does not find principal with filter " + pcpl.getName(), it.hasNext());
            }
        }
    }

    public void testFindPrincipalByTypeAll() {
        Principal[] pcpls = getPrincipals(superuser);
        for (Principal pcpl : pcpls) {
            if (pcpl.equals(everyone)) {
                // special case covered by another test
                continue;
            }

            PrincipalIterator it = principalMgr.findPrincipals(pcpl.getName(), PrincipalManager.SEARCH_TYPE_ALL);
            PrincipalIterator it2 = principalMgr.findPrincipals(pcpl.getName());

            // both search must reveal the same result and size
            assertTrue(it.getSize() == it2.getSize());

            Set<Principal> s1 = new HashSet<Principal>();
            Set<Principal> s2 = new HashSet<Principal>();
            while (it.hasNext() && it2.hasNext()) {
                s1.add(it.nextPrincipal());
                s2.add(it2.nextPrincipal());
            }

            assertEquals(s1, s2);
            assertFalse(it.hasNext() && it2.hasNext());
        }
    }

    public void testFindEveryone() {
        Principal everyone = principalMgr.getEveryone();

        boolean containedInResult = false;

        // untyped search -> everyone must be part of the result set
        PrincipalIterator it = principalMgr.findPrincipals(everyone.getName());
        while (it.hasNext()) {
            Principal p = it.nextPrincipal();
            if (p.getName().equals(everyone.getName())) {
                containedInResult = true;
            }
        }
        assertTrue(containedInResult);

        // search group only -> everyone must be part of the result set
        containedInResult = false;
        it = principalMgr.findPrincipals(everyone.getName(), PrincipalManager.SEARCH_TYPE_GROUP);
        while (it.hasNext()) {
            Principal p = it.nextPrincipal();
            if (p.getName().equals(everyone.getName())) {
                containedInResult = true;
            }
        }
        assertTrue(containedInResult);

        // search non-group only -> everyone should not be part of the result set
        containedInResult = false;
        it = principalMgr.findPrincipals(everyone.getName(), PrincipalManager.SEARCH_TYPE_NOT_GROUP);
        while (it.hasNext()) {
            Principal p = it.nextPrincipal();
            if (p.getName().equals(everyone.getName())) {
                containedInResult = true;
            }
        }
        assertFalse(containedInResult);
    }
}