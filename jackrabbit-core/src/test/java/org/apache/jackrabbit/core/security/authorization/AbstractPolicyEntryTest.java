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
package org.apache.jackrabbit.core.security.authorization;

import org.apache.jackrabbit.api.jsr283.security.AccessControlException;
import org.apache.jackrabbit.api.jsr283.security.Privilege;
import org.apache.jackrabbit.test.JUnitTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * <code>AbstractPolicyEntryTest</code>...
 */
public abstract class AbstractPolicyEntryTest extends JUnitTest {

    private static Logger log = LoggerFactory.getLogger(AbstractPolicyEntryTest.class);
    protected Principal testPrincipal;

    protected void setUp() throws Exception {
        super.setUp();
        testPrincipal = new Principal() {
            public String getName() {
                return "TestPrincipal";
            }
        };
    }

    protected PolicyEntry createPolicyEntry(int privileges, boolean isAllow) {
        return createPolicyEntry(testPrincipal, privileges, isAllow);
    }

    protected abstract PolicyEntry createPolicyEntry(Principal principal, int privileges, boolean isAllow);

    public void testIsAllow() {
        PolicyEntry tmpl = createPolicyEntry(PrivilegeRegistry.READ, true);
        assertTrue(tmpl.isAllow());

        tmpl = createPolicyEntry(PrivilegeRegistry.READ, false);
        assertFalse(tmpl.isAllow());
    }

    public void testGetPrincipal() {
        PolicyEntry tmpl = createPolicyEntry(PrivilegeRegistry.READ, true);
        assertNotNull(tmpl.getPrincipal());
        assertEquals(testPrincipal.getName(), tmpl.getPrincipal().getName());
        assertSame(testPrincipal, tmpl.getPrincipal());
    }

    public void testGetPrivilegeBits() {
        PolicyEntry tmpl = createPolicyEntry(PrivilegeRegistry.READ, true);

        int privs = tmpl.getPrivilegeBits();
        assertTrue(privs == PrivilegeRegistry.READ);

        tmpl = createPolicyEntry(PrivilegeRegistry.WRITE, true);
        privs = tmpl.getPrivilegeBits();
        assertTrue(privs == PrivilegeRegistry.WRITE);
    }

    public void testGetPrivileges() throws AccessControlException {
        PolicyEntry tmpl = createPolicyEntry(PrivilegeRegistry.READ, true);

        Privilege[] privs = tmpl.getPrivileges();
        assertNotNull(privs);
        assertEquals(1, privs.length);
        assertEquals(privs[0].getName(), Privilege.READ);
        assertTrue(PrivilegeRegistry.getBits(privs) == tmpl.getPrivilegeBits());

        tmpl = createPolicyEntry(PrivilegeRegistry.WRITE, true);
        privs = tmpl.getPrivileges();
        assertNotNull(privs);
        assertEquals(1, privs.length);
        assertEquals(privs[0].getName(), Privilege.WRITE);
        assertTrue(PrivilegeRegistry.getBits(privs) == tmpl.getPrivilegeBits());

        tmpl = createPolicyEntry(PrivilegeRegistry.ADD_CHILD_NODES | PrivilegeRegistry.REMOVE_CHILD_NODES, true);
        privs = tmpl.getPrivileges();
        assertNotNull(privs);
        assertEquals(2, privs.length);

        Privilege[] param = PrivilegeRegistry.getPrivileges(new String[] {Privilege.ADD_CHILD_NODES, Privilege.REMOVE_CHILD_NODES});
        assertEquals(Arrays.asList(param), Arrays.asList(privs));
        assertTrue(PrivilegeRegistry.getBits(privs) == tmpl.getPrivilegeBits());
    }

    public void testEquals() {

        PolicyEntry ace = createPolicyEntry(PrivilegeRegistry.ALL, true);
        PolicyEntry ace2 = createPolicyEntry(PrivilegeRegistry.ALL, true);
        assertEquals(ace, ace2);

        ace2 = createPolicyEntry(PrivilegeRegistry.READ |
                PrivilegeRegistry.WRITE |
                PrivilegeRegistry.MODIFY_AC |
                PrivilegeRegistry.READ_AC, true);
        assertEquals(ace, ace2);
    }

    public void testNotEquals() {
        PolicyEntry ace = createPolicyEntry(PrivilegeRegistry.ALL, true);
        List otherAces = new ArrayList();
        // ACE template with different principal
        otherAces.add(createPolicyEntry(new Principal() {
            public String getName() {
                return "a name";
            } }, PrivilegeRegistry.ALL, true)
        );

        // ACE template with different privileges
        otherAces.add(createPolicyEntry(PrivilegeRegistry.READ, true));
        // ACE template with different 'allow' flag
        otherAces.add(createPolicyEntry(PrivilegeRegistry.ALL, false));
        // ACE template with different privileges and 'allows
        otherAces.add(createPolicyEntry(PrivilegeRegistry.WRITE, false));
        // other ace impl
        PolicyEntry pe = new PolicyEntry() {
            public boolean isAllow() {
                return true;
            }

            public int getPrivilegeBits() {
                return PrivilegeRegistry.ALL;
            }

            public Principal getPrincipal() {
                return testPrincipal;
            }
            public Privilege[] getPrivileges() {
                return PrivilegeRegistry.getPrivileges(PrivilegeRegistry.ALL);
            }
        };
        otherAces.add(pe);

        for (Iterator it = otherAces.iterator(); it.hasNext();) {
            assertFalse(ace.equals(it.next()));
        }
    }
}