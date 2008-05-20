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

import junit.framework.TestCase;
import org.apache.jackrabbit.api.jsr283.security.AccessControlException;
import org.apache.jackrabbit.api.jsr283.security.Privilege;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <code>PrivilegeRegistryTest</code>...
 */
public class PrivilegeRegistryTest extends TestCase {

    public void testRegisteredPrivileges() {
        Privilege[] ps = PrivilegeRegistry.getRegisteredPrivileges();

        List l = new ArrayList(Arrays.asList(ps));
        assertTrue(l.remove(PrivilegeRegistry.READ_PRIVILEGE));
        assertTrue(l.remove(PrivilegeRegistry.ADD_CHILD_NODES_PRIVILEGE));
        assertTrue(l.remove(PrivilegeRegistry.REMOVE_CHILD_NODES_PRIVILEGE));
        assertTrue(l.remove(PrivilegeRegistry.MODIFY_PROPERTIES_PRIVILEGE));
        assertTrue(l.remove(PrivilegeRegistry.READ_AC_PRIVILEGE));
        assertTrue(l.remove(PrivilegeRegistry.MODIFY_AC_PRIVILEGE));
        assertTrue(l.remove(PrivilegeRegistry.WRITE_PRIVILEGE));
        assertTrue(l.remove(PrivilegeRegistry.ALL_PRIVILEGE));
        assertTrue(l.isEmpty());
    }

    public void testAllPrivilege() {
        Privilege p = PrivilegeRegistry.ALL_PRIVILEGE;
        assertEquals(p.getName(), Privilege.ALL);
        assertTrue(p.isAggregate());
        assertFalse(p.isAbstract());

        List l = new ArrayList(Arrays.asList(p.getAggregatePrivileges()));
        assertTrue(l.remove(PrivilegeRegistry.READ_PRIVILEGE));
        assertTrue(l.remove(PrivilegeRegistry.ADD_CHILD_NODES_PRIVILEGE));
        assertTrue(l.remove(PrivilegeRegistry.REMOVE_CHILD_NODES_PRIVILEGE));
        assertTrue(l.remove(PrivilegeRegistry.MODIFY_PROPERTIES_PRIVILEGE));
        assertTrue(l.remove(PrivilegeRegistry.READ_AC_PRIVILEGE));
        assertTrue(l.remove(PrivilegeRegistry.MODIFY_AC_PRIVILEGE));
        assertTrue(l.isEmpty());
    }

    public void testGetBits() throws AccessControlException {
        Privilege[] privs = new Privilege[] {PrivilegeRegistry.ADD_CHILD_NODES_PRIVILEGE,
                                             PrivilegeRegistry.REMOVE_CHILD_NODES_PRIVILEGE};

        int bits = PrivilegeRegistry.getBits(privs);
        assertTrue(bits > PrivilegeRegistry.NO_PRIVILEGE);
        assertTrue(bits == (PrivilegeRegistry.ADD_CHILD_NODES | PrivilegeRegistry.REMOVE_CHILD_NODES));
    }

    public void testGetBitsFromCustomPrivilege() throws AccessControlException {
        Privilege p = buildCustomPrivilege("anyName", PrivilegeRegistry.WRITE_PRIVILEGE);

        int bits = PrivilegeRegistry.getBits(new Privilege[] {p});

        assertTrue(bits > PrivilegeRegistry.NO_PRIVILEGE);
        assertTrue(bits == PrivilegeRegistry.WRITE);
    }

    public void testGetBitsFromCustomPrivilege2() throws AccessControlException {
        Privilege p = buildCustomPrivilege(Privilege.READ, null);

        int bits = PrivilegeRegistry.getBits(new Privilege[] {p});

        assertTrue(bits > PrivilegeRegistry.NO_PRIVILEGE);
        assertTrue(bits == PrivilegeRegistry.READ);
    }

    public void testGetBitsFromNull() {
        try {
            PrivilegeRegistry.getBits((Privilege[]) null);
            fail("Should throw AccessControlException");
        } catch (AccessControlException e) {
            // ok
        }
    }

    public void testGetBitsFromEmptyArray() {
        try {
            PrivilegeRegistry.getBits(new Privilege[0]);
            fail("Should throw AccessControlException");
        } catch (AccessControlException e) {
            // ok
        }
    }

    public void testGetBitsWithInvalidPrivilege() {
        Privilege p = buildCustomPrivilege("anyName", null);
        try {
            PrivilegeRegistry.getBits(new Privilege[] {p});
            fail();
        } catch (AccessControlException e) {
            // ok
        }
    }

    public void testGetPrivilegesFromBits() throws AccessControlException {
        Privilege[] pvs = PrivilegeRegistry.getPrivileges(PrivilegeRegistry.READ_AC);

        assertTrue(pvs != null);
        assertTrue(pvs.length == 1);
        assertEquals(pvs[0].getName(), Privilege.READ_ACCESS_CONTROL);
    }

    public void testGetPrivilegesFromBits2() throws AccessControlException {
        int writeBits = PrivilegeRegistry.ADD_CHILD_NODES | PrivilegeRegistry.REMOVE_CHILD_NODES | PrivilegeRegistry.MODIFY_PROPERTIES;
        Privilege[] pvs = PrivilegeRegistry.getPrivileges(writeBits);

        assertTrue(pvs != null);
        assertTrue(pvs.length == 1);
        assertEquals(pvs[0].getName(), Privilege.WRITE);
        assertTrue(pvs[0].isAggregate());
        assertTrue(pvs[0].getDeclaredAggregatePrivileges().length == 3);
    }

    public void testGetPrivilegesFromNames() throws AccessControlException {
        Privilege[] p = PrivilegeRegistry.getPrivileges(new String[] {Privilege.READ});

        assertTrue(p != null && p.length == 1);
        assertEquals(p[0].getName(), PrivilegeRegistry.READ_PRIVILEGE.getName());
        assertEquals(p[0], PrivilegeRegistry.READ_PRIVILEGE);
        assertFalse(p[0].isAggregate());

        p = PrivilegeRegistry.getPrivileges(new String[] {Privilege.WRITE});

        assertTrue(p != null && p.length == 1);
        assertEquals(p[0].getName(), PrivilegeRegistry.WRITE_PRIVILEGE.getName());
        assertEquals(p[0], PrivilegeRegistry.WRITE_PRIVILEGE);
        assertTrue(p[0].isAggregate());

        p = PrivilegeRegistry.getPrivileges(new String[] {Privilege.READ,
                                                          Privilege.MODIFY_ACCESS_CONTROL});
        assertTrue(p != null);
        assertTrue(p.length == 2);

        List l = Arrays.asList(p);
        assertTrue(l.contains(PrivilegeRegistry.READ_PRIVILEGE) && l.contains(PrivilegeRegistry.MODIFY_AC_PRIVILEGE));
    }

    public void testGetPrivilegesFromInvalidNames() {
        try {
            PrivilegeRegistry.getPrivileges(new String[]{"unknown"});
            fail("invalid privilege name");
        } catch (AccessControlException e) {
            // OK
        }
    }

    public void testGetPrivilegesFromEmptyNames() {
        try {
            PrivilegeRegistry.getPrivileges(new String[0]);
            fail("invalid privilege name array");
        } catch (AccessControlException e) {
            // OK
        }
    }

    public void testGetPrivilegesFromNullNames() {
        try {
            PrivilegeRegistry.getPrivileges(null);
            fail("invalid privilege names (null)");
        } catch (AccessControlException e) {
            // OK
        }
    }

     private Privilege buildCustomPrivilege(final String name, final Privilege declaredAggr) {
        return new Privilege() {

            public String getName() {
                return name;
            }
            public String getDescription() {
                return null;
            }
            public boolean isAbstract() {
                return false;
            }
            public boolean isAggregate() {
                return declaredAggr != null;
            }
            public Privilege[] getDeclaredAggregatePrivileges() {
                return (declaredAggr ==  null) ? new Privilege[0] : new Privilege[] {declaredAggr};
            }
            public Privilege[] getAggregatePrivileges() {
                return (declaredAggr ==  null) ? new Privilege[0] : declaredAggr.getAggregatePrivileges();
            }
        };
    }
}