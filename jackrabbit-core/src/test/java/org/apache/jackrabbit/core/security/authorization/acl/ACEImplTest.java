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
package org.apache.jackrabbit.core.security.authorization.acl;

import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.core.security.authorization.PolicyEntry;
import org.apache.jackrabbit.core.security.jsr283.security.AccessControlException;
import org.apache.jackrabbit.core.security.jsr283.security.Privilege;
import org.apache.jackrabbit.test.JUnitTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * <code>ACEImplTest</code>...
 */
public class ACEImplTest extends JUnitTest {

    private static Logger log = LoggerFactory.getLogger(ACEImplTest.class);

    private Principal testPrincipal;

    protected void setUp() throws Exception {
        super.setUp();
        testPrincipal = new Principal() {
            public String getName() {
                return "TestPrincipal";
            }
        };
    }

    public void testIsAllow() {
        ACEImpl tmpl = new ACEImpl(testPrincipal, PrivilegeRegistry.READ, true);
        assertTrue(tmpl.isAllow());

        tmpl = new ACEImpl(testPrincipal, PrivilegeRegistry.READ, false);
        assertFalse(tmpl.isAllow());
    }

    public void testGetPrincipal() {
        ACEImpl tmpl = new ACEImpl(testPrincipal, PrivilegeRegistry.READ, true);
        assertNotNull(tmpl.getPrincipal());
        assertEquals(testPrincipal.getName(), tmpl.getPrincipal().getName());
        assertSame(testPrincipal, tmpl.getPrincipal());
    }

    public void testGetPrivileges() throws AccessControlException {
        ACEImpl tmpl = new ACEImpl(testPrincipal, PrivilegeRegistry.READ, true);

        Privilege[] privs = tmpl.getPrivileges();
        assertNotNull(privs);
        assertEquals(1, privs.length);
        assertEquals(privs[0].getName(), Privilege.READ);

        tmpl = new ACEImpl(testPrincipal, PrivilegeRegistry.WRITE, true);
        privs = tmpl.getPrivileges();
        assertNotNull(privs);
        assertEquals(1, privs.length);
        assertEquals(privs[0].getName(), Privilege.WRITE);

        tmpl = new ACEImpl(testPrincipal, PrivilegeRegistry.ADD_CHILD_NODES | PrivilegeRegistry.REMOVE_CHILD_NODES, true);
        privs = tmpl.getPrivileges();
        assertNotNull(privs);
        assertEquals(2, privs.length);

        Privilege[] param = PrivilegeRegistry.getPrivileges(new String[] {Privilege.ADD_CHILD_NODES, Privilege.REMOVE_CHILD_NODES});
        assertEquals(Arrays.asList(param), Arrays.asList(privs));
    }

    public void testEqual() {
        ACEImpl ace = new ACEImpl(testPrincipal, PrivilegeRegistry.ALL, true);

        ACEImpl ace2 = new ACEImpl(testPrincipal, PrivilegeRegistry.ALL, true);
        assertEquals(ace, ace2);

        ace2 = new ACEImpl(testPrincipal, PrivilegeRegistry.READ |
                PrivilegeRegistry.WRITE |
                PrivilegeRegistry.MODIFY_AC |
                PrivilegeRegistry.READ_AC, true);
        assertEquals(ace, ace2);
    }

    public void testNotEqual() {
        ACEImpl ace = new ACEImpl(testPrincipal, PrivilegeRegistry.ALL, true);
        List otherAces = new ArrayList();
        // ACE template with different principal
        otherAces.add(new ACEImpl(new Principal() {
            public String getName() {
                return "a name";
            } }, PrivilegeRegistry.ALL, true)
        );

        // ACE template with different privileges
        otherAces.add(new ACEImpl(testPrincipal, PrivilegeRegistry.READ, true));
        // ACE template with different 'allow' flag
        otherAces.add(new ACEImpl(testPrincipal, PrivilegeRegistry.ALL, false));
        // ACE template with different privileges and 'allows
        otherAces.add(new ACEImpl(testPrincipal, PrivilegeRegistry.WRITE, false));
        // other ace impl
        PolicyEntry pe = new PolicyEntry() {
            public boolean isAllow() {
                return true;
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