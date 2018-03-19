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

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.principal.GroupPrincipal;
import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.AbstractUserTest;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.core.security.principal.EveryonePrincipal;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.RepositoryException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * <code>GroupImplTest</code>...
 */
public class GroupImplTest extends AbstractUserTest {

    public void testEveryoneGroup() throws RepositoryException, NotExecutableException {
        Group g = null;
        try {
            g = userMgr.createGroup(EveryonePrincipal.NAME);
            save(superuser);

            assertEquals(EveryonePrincipal.NAME, g.getPrincipal().getName());
            assertEquals(EveryonePrincipal.getInstance(), g.getPrincipal());

            assertTrue(g.isDeclaredMember(getTestUser(superuser)));
            assertTrue(g.isMember(getTestUser(superuser)));

            Iterator<Authorizable> it = g.getDeclaredMembers();
            assertTrue(it.hasNext());
            Set<Authorizable> members = new HashSet<Authorizable>();
            while (it.hasNext()) {
                members.add(it.next());
            }

            it = g.getMembers();
            assertTrue(it.hasNext());
            while (it.hasNext()) {
                assertTrue(members.contains(it.next()));
            }

            assertFalse(g.addMember(getTestUser(superuser)));
            assertFalse(g.removeMember(getTestUser(superuser)));

            PrincipalManager pMgr = ((JackrabbitSession) superuser).getPrincipalManager();
            Principal everyone = pMgr.getEveryone();

            assertTrue(everyone instanceof ItemBasedPrincipal);
            assertEquals(everyone, EveryonePrincipal.getInstance());

        } finally {
            if (g != null) {
                g.remove();
                save(superuser);
            }
        }
    }

    public void testEveryoneGroup2() throws RepositoryException, NotExecutableException {
        Group g = null;
        Group g2 = null;
        try {
            g = userMgr.createGroup(EveryonePrincipal.NAME);
            g2 = userMgr.createGroup("testGroup");
            save(superuser);

            assertFalse(g.addMember(g2));
            assertFalse(g.removeMember(g2));

            assertFalse(g2.addMember(g));
            assertFalse(g2.removeMember(g));
            
        } finally {
            if (g != null) {
                g.remove();
            }
            if (g2 != null) {
                g2.remove();
            }
            save(superuser);            
        }
    }

    public void testEveryoneGroupPrincipal() throws Exception {
        Group g = null;
        try {
            g = userMgr.createGroup(EveryonePrincipal.NAME);
            save(superuser);

            GroupPrincipal principal = (GroupPrincipal) g.getPrincipal();
            assertTrue(principal.isMember(new Principal() {

                public String getName() {
                    return "test";
                }
            }));

            assertFalse(principal.isMember(principal));

        } finally {
            if (g != null) {
                g.remove();
                save(superuser);
            }
        }
    }
}