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

import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.PrivilegeDefinition;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.Privilege;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <code>PrivilegeRegistryTest</code>...
 */
public class PrivilegeRegistryTest extends AbstractJCRTest {

    private NameResolver resolver;
    private PrivilegeRegistry privilegeRegistry;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        resolver = ((SessionImpl) superuser);
        privilegeRegistry = new PrivilegeRegistry(resolver);
    }

    public void testGetAll() throws RepositoryException {

        PrivilegeDefinition[] defs = privilegeRegistry.getAll();

        List<PrivilegeDefinition> l = new ArrayList<PrivilegeDefinition>(Arrays.asList(defs));
        assertTrue(l.remove(privilegeRegistry.get(NameConstants.JCR_READ)));
        assertTrue(l.remove(privilegeRegistry.get(NameConstants.JCR_ADD_CHILD_NODES)));
        assertTrue(l.remove(privilegeRegistry.get(NameConstants.JCR_REMOVE_CHILD_NODES)));
        assertTrue(l.remove(privilegeRegistry.get(NameConstants.JCR_MODIFY_PROPERTIES)));
        assertTrue(l.remove(privilegeRegistry.get(NameConstants.JCR_REMOVE_NODE)));
        assertTrue(l.remove(privilegeRegistry.get(NameConstants.JCR_READ_ACCESS_CONTROL)));
        assertTrue(l.remove(privilegeRegistry.get(NameConstants.JCR_MODIFY_ACCESS_CONTROL)));
        assertTrue(l.remove(privilegeRegistry.get(NameConstants.JCR_WRITE)));
        assertTrue(l.remove(privilegeRegistry.get(NameConstants.JCR_ALL)));
        assertTrue(l.remove(privilegeRegistry.get(NameConstants.JCR_LIFECYCLE_MANAGEMENT)));
        assertTrue(l.remove(privilegeRegistry.get(NameConstants.JCR_LOCK_MANAGEMENT)));
        assertTrue(l.remove(privilegeRegistry.get(NameConstants.JCR_NODE_TYPE_MANAGEMENT)));
        assertTrue(l.remove(privilegeRegistry.get(NameConstants.JCR_RETENTION_MANAGEMENT)));
        assertTrue(l.remove(privilegeRegistry.get(NameConstants.JCR_VERSION_MANAGEMENT)));
        // including repo-level operation privileges
        assertTrue(l.remove(privilegeRegistry.get(NameConstants.JCR_NAMESPACE_MANAGEMENT)));
        assertTrue(l.remove(privilegeRegistry.get(NameConstants.JCR_NODE_TYPE_DEFINITION_MANAGEMENT)));
        assertTrue(l.remove(privilegeRegistry.get(NameConstants.JCR_WORKSPACE_MANAGEMENT)));
        assertTrue(l.remove(privilegeRegistry.get(resolver.getQName(PrivilegeRegistry.REP_PRIVILEGE_MANAGEMENT))));
        // and aggregates
        assertTrue(l.remove(privilegeRegistry.get(resolver.getQName(PrivilegeRegistry.REP_WRITE))));
                
        assertTrue(l.isEmpty());
    }


    public void testGet() throws RepositoryException {

        for (PrivilegeDefinition def : privilegeRegistry.getAll()) {

            PrivilegeDefinition d = privilegeRegistry.get(def.getName());
            assertEquals(def, d);

            assertNotNull(d.getName());
            assertEquals(def.getName(), d.getName());

            assertFalse(d.isAbstract());
            assertEquals(def.isAbstract(), d.isAbstract());

            assertNotNull(d.getDeclaredAggregateNames());
            assertTrue(def.getDeclaredAggregateNames().containsAll(d.getDeclaredAggregateNames()));
            assertTrue(d.getDeclaredAggregateNames().containsAll(def.getDeclaredAggregateNames()));

            assertFalse(privilegeRegistry.getBits(d).isEmpty());
        }
    }

    public void testAggregates() throws RepositoryException {

        for (PrivilegeDefinition def : privilegeRegistry.getAll()) {
            if (def.getDeclaredAggregateNames().isEmpty()) {
                continue; // ignore non aggregate
            }

            for (Name n : def.getDeclaredAggregateNames()) {
                PrivilegeDefinition d = privilegeRegistry.get(n);
                assertNotNull(d);
                Name[] names = privilegeRegistry.getNames(privilegeRegistry.getBits(d));
                assertNotNull(names);
                assertEquals(1, names.length);
                assertEquals(d.getName(), names[0]);
            }
        }
    }

    public void testPrivilegeDefinition() throws RepositoryException {

        for (PrivilegeDefinition def : privilegeRegistry.getAll()) {
            assertNotNull(def.getName());
            assertFalse(def.isAbstract());
            assertNotNull(def.getDeclaredAggregateNames());
            assertFalse(privilegeRegistry.getBits(def).isEmpty());
        }
    }

    public void testJcrAll() throws RepositoryException {
        PrivilegeDefinition p = privilegeRegistry.get(NameConstants.JCR_ALL);
        assertEquals(p.getName(), NameConstants.JCR_ALL);
        assertFalse(p.getDeclaredAggregateNames().isEmpty());
        assertFalse(p.isAbstract());

        Set<Name> l = new HashSet<Name>(p.getDeclaredAggregateNames());
        assertTrue(l.remove(NameConstants.JCR_READ));
        assertTrue(l.remove(NameConstants.JCR_WRITE));
        assertTrue(l.remove(resolver.getQName(PrivilegeRegistry.REP_WRITE)));        
        assertTrue(l.remove(NameConstants.JCR_READ_ACCESS_CONTROL));
        assertTrue(l.remove(NameConstants.JCR_MODIFY_ACCESS_CONTROL));
        assertTrue(l.remove(NameConstants.JCR_LIFECYCLE_MANAGEMENT));
        assertTrue(l.remove(NameConstants.JCR_LOCK_MANAGEMENT));
        assertTrue(l.remove(NameConstants.JCR_NODE_TYPE_MANAGEMENT));
        assertTrue(l.remove(NameConstants.JCR_RETENTION_MANAGEMENT));
        assertTrue(l.remove(NameConstants.JCR_VERSION_MANAGEMENT));
        // including repo-level operation privileges
        assertTrue(l.remove(NameConstants.JCR_NAMESPACE_MANAGEMENT));
        assertTrue(l.remove(NameConstants.JCR_NODE_TYPE_DEFINITION_MANAGEMENT));
        assertTrue(l.remove(NameConstants.JCR_WORKSPACE_MANAGEMENT));
        assertTrue(l.remove(resolver.getQName(PrivilegeRegistry.REP_PRIVILEGE_MANAGEMENT)));
        assertTrue(l.isEmpty());
    }

    public void testJcrWrite() throws RepositoryException {
        Name rw = resolver.getQName(PrivilegeRegistry.REP_WRITE);
        PrivilegeDefinition p = privilegeRegistry.get(rw);

        assertEquals(p.getName(), rw);
        assertFalse(p.getDeclaredAggregateNames().isEmpty());
        assertFalse(p.isAbstract());

        Set<Name> l = new HashSet<Name>(p.getDeclaredAggregateNames());
        assertTrue(l.remove(NameConstants.JCR_WRITE));
        assertTrue(l.remove(NameConstants.JCR_NODE_TYPE_MANAGEMENT));
        assertTrue(l.isEmpty());
    }

    public void testRepWrite() throws RepositoryException {
        PrivilegeDefinition p = privilegeRegistry.get(NameConstants.JCR_WRITE);
        assertEquals(p.getName(), NameConstants.JCR_WRITE);
        assertFalse(p.getDeclaredAggregateNames().isEmpty());
        assertFalse(p.isAbstract());

        Set<Name> l = new HashSet<Name>(p.getDeclaredAggregateNames());
        assertTrue(l.remove(NameConstants.JCR_MODIFY_PROPERTIES));
        assertTrue(l.remove(NameConstants.JCR_ADD_CHILD_NODES));
        assertTrue(l.remove(NameConstants.JCR_REMOVE_CHILD_NODES));
        assertTrue(l.remove(NameConstants.JCR_REMOVE_NODE));
        assertTrue(l.isEmpty());
    }

    private void assertSamePrivilegeName(String expected, String present) throws RepositoryException {
        assertEquals("Privilege names are not the same", resolver.getQName(expected), resolver.getQName(present));
    }

    private Privilege[] privilegesFromNames(String[] privNames)
            throws RepositoryException {
        Privilege[] privs = new Privilege[privNames.length];
        for (int i = 0; i < privNames.length; i++) {
            privs[i] = privilegeRegistry.getPrivilege(privNames[i]);
        }
        return privs;
    }

    public void testRegisteredPrivileges() throws RepositoryException {
        Privilege[] ps = privilegeRegistry.getRegisteredPrivileges();

        List<Privilege> l = new ArrayList<Privilege>(Arrays.asList(ps));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_READ)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_ADD_CHILD_NODES)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_REMOVE_CHILD_NODES)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_MODIFY_PROPERTIES)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_REMOVE_NODE)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_READ_ACCESS_CONTROL)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_MODIFY_ACCESS_CONTROL)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_WRITE)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_ALL)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_LIFECYCLE_MANAGEMENT)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_LOCK_MANAGEMENT)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_NODE_TYPE_MANAGEMENT)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_RETENTION_MANAGEMENT)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_VERSION_MANAGEMENT)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(PrivilegeRegistry.REP_WRITE)));
        // including repo-level operation privileges
        assertTrue(l.remove(privilegeRegistry.getPrivilege(NameConstants.JCR_NAMESPACE_MANAGEMENT.toString())));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(NameConstants.JCR_NODE_TYPE_DEFINITION_MANAGEMENT.toString())));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(NameConstants.JCR_WORKSPACE_MANAGEMENT.toString())));        
        assertTrue(l.remove(privilegeRegistry.getPrivilege(PrivilegeRegistry.REP_PRIVILEGE_MANAGEMENT)));
        assertTrue(l.isEmpty());
    }

    public void testAllPrivilege() throws RepositoryException {
        Privilege p = privilegeRegistry.getPrivilege(Privilege.JCR_ALL);
        assertSamePrivilegeName(p.getName(), Privilege.JCR_ALL);
        assertTrue(p.isAggregate());
        assertFalse(p.isAbstract());

        List<Privilege> l = new ArrayList<Privilege>(Arrays.asList(p.getAggregatePrivileges()));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_READ)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_ADD_CHILD_NODES)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_REMOVE_CHILD_NODES)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_MODIFY_PROPERTIES)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_REMOVE_NODE)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_READ_ACCESS_CONTROL)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_MODIFY_ACCESS_CONTROL)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_LIFECYCLE_MANAGEMENT)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_LOCK_MANAGEMENT)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_NODE_TYPE_MANAGEMENT)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_RETENTION_MANAGEMENT)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_VERSION_MANAGEMENT)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_WRITE)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(PrivilegeRegistry.REP_WRITE)));
        // including repo-level operation privileges
        assertTrue(l.remove(privilegeRegistry.getPrivilege(NameConstants.JCR_NAMESPACE_MANAGEMENT.toString())));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(NameConstants.JCR_NODE_TYPE_DEFINITION_MANAGEMENT.toString())));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(NameConstants.JCR_WORKSPACE_MANAGEMENT.toString())));        
        assertTrue(l.remove(privilegeRegistry.getPrivilege(PrivilegeRegistry.REP_PRIVILEGE_MANAGEMENT)));
        assertTrue(l.isEmpty());

        l = new ArrayList<Privilege>(Arrays.asList(p.getDeclaredAggregatePrivileges()));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_READ)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_WRITE)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(PrivilegeRegistry.REP_WRITE)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(PrivilegeRegistry.REP_PRIVILEGE_MANAGEMENT)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_READ_ACCESS_CONTROL)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_MODIFY_ACCESS_CONTROL)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_LIFECYCLE_MANAGEMENT)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_LOCK_MANAGEMENT)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_RETENTION_MANAGEMENT)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_VERSION_MANAGEMENT)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_NODE_TYPE_MANAGEMENT)));
        // including repo-level operation privileges
        assertTrue(l.remove(privilegeRegistry.getPrivilege(NameConstants.JCR_NAMESPACE_MANAGEMENT.toString())));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(NameConstants.JCR_NODE_TYPE_DEFINITION_MANAGEMENT.toString())));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(NameConstants.JCR_WORKSPACE_MANAGEMENT.toString())));
        assertTrue(l.isEmpty());
    }

    public void testGetBits() throws RepositoryException {
        Privilege p1 = privilegeRegistry.getPrivilege(Privilege.JCR_ADD_CHILD_NODES);
        Privilege p2 = privilegeRegistry.getPrivilege(Privilege.JCR_REMOVE_CHILD_NODES);
        Privilege[] privs = new Privilege[] {p1, p2};

        int bits = PrivilegeRegistry.getBits(privs);
        assertTrue(bits > PrivilegeRegistry.NO_PRIVILEGE);
        assertTrue(bits == (PrivilegeRegistry.getBits(new Privilege[] {p1}) |
                PrivilegeRegistry.getBits(new Privilege[] {p2})));
    }

    public void testGetBitsFromInvalidPrivilege() throws AccessControlException {
        Privilege p = buildUnregisteredPrivilege(Privilege.JCR_READ, null);
        try {
            PrivilegeRegistry.getBits(new Privilege[] {p});
            fail("Retrieving bits from unknown privilege should fail.");
        } catch (AccessControlException e) {
            // ok
        }
    }

    public void testGetBitsFromInvalidAggregatePrivilege() throws RepositoryException {
        Privilege p = buildUnregisteredPrivilege("anyName", privilegeRegistry.getPrivilege(Privilege.JCR_WRITE));
        try {
            PrivilegeRegistry.getBits(new Privilege[] {p});
            fail("Retrieving bits from unknown privilege should fail.");
        } catch (AccessControlException e) {
            // ok
        }
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
        Privilege p = buildUnregisteredPrivilege("anyName", null);
        try {
            PrivilegeRegistry.getBits(new Privilege[] {p});
            fail();
        } catch (AccessControlException e) {
            // ok
        }
    }

    public void testGetPrivilegesFromBits() throws RepositoryException {
        int bits = PrivilegeRegistry.getBits(privilegesFromNames(new String[] {Privilege.JCR_READ_ACCESS_CONTROL}));
        Privilege[] pvs = privilegeRegistry.getPrivileges(bits);

        assertTrue(pvs != null);
        assertTrue(pvs.length == 1);
        assertSamePrivilegeName(pvs[0].getName(), Privilege.JCR_READ_ACCESS_CONTROL);
    }

    public void testGetPrivilegesFromBits2() throws RepositoryException {
        String[] names = new String[] {
                Privilege.JCR_ADD_CHILD_NODES,
                Privilege.JCR_REMOVE_CHILD_NODES,
                Privilege.JCR_REMOVE_NODE,
                Privilege.JCR_MODIFY_PROPERTIES
        };
        int writeBits = PrivilegeRegistry.getBits(privilegesFromNames(names));
        Privilege[] pvs = privilegeRegistry.getPrivileges(writeBits);

        assertTrue(pvs != null);
        assertTrue(pvs.length == 1);
        assertSamePrivilegeName(pvs[0].getName(), Privilege.JCR_WRITE);
        assertTrue(pvs[0].isAggregate());
        assertTrue(pvs[0].getDeclaredAggregatePrivileges().length == names.length);
    }

    public void testGetPrivilegesFromBits3() throws RepositoryException {
        String[] names = new String[] {
                PrivilegeRegistry.REP_WRITE
        };
        int writeBits = PrivilegeRegistry.getBits(privilegesFromNames(names));
        Privilege[] pvs = privilegeRegistry.getPrivileges(writeBits);

        assertTrue(pvs != null);
        assertTrue(pvs.length == 1);
        assertSamePrivilegeName(pvs[0].getName(), PrivilegeRegistry.REP_WRITE);
        assertTrue(pvs[0].isAggregate());

        names = new String[] {
                PrivilegeRegistry.REP_WRITE,
                Privilege.JCR_WRITE
        };
        writeBits = PrivilegeRegistry.getBits(privilegesFromNames(names));
        pvs = privilegeRegistry.getPrivileges(writeBits);

        assertTrue(pvs != null);
        assertTrue(pvs.length == 1);
        assertSamePrivilegeName(pvs[0].getName(), PrivilegeRegistry.REP_WRITE);
        assertTrue(pvs[0].isAggregate());
        assertTrue(pvs[0].getDeclaredAggregatePrivileges().length == names.length);
    }

    public void testGetPrivilegesFromBits4() throws RepositoryException {
        String[] names = new String[] {
                PrivilegeRegistry.REP_WRITE,
                Privilege.JCR_LIFECYCLE_MANAGEMENT
        };
        int writeBits = PrivilegeRegistry.getBits(privilegesFromNames(names));
        Privilege[] pvs = privilegeRegistry.getPrivileges(writeBits);

        assertTrue(pvs != null);
        assertTrue(pvs.length == 2);
    }

    public void testGetPrivilegeFromName() throws AccessControlException, RepositoryException {
        Privilege p = privilegeRegistry.getPrivilege(Privilege.JCR_READ);

        assertTrue(p != null);
        assertSamePrivilegeName(Privilege.JCR_READ, p.getName());
        assertFalse(p.isAggregate());

        p = privilegeRegistry.getPrivilege(Privilege.JCR_WRITE);

        assertTrue(p != null);
        assertSamePrivilegeName(p.getName(), Privilege.JCR_WRITE);
        assertTrue(p.isAggregate());
    }

    public void testGetPrivilegesFromInvalidName() throws RepositoryException {
        try {
            privilegeRegistry.getPrivilege("unknown");
            fail("invalid privilege name");
        } catch (AccessControlException e) {
            // OK
        }
    }

    public void testGetPrivilegesFromEmptyNames() {
        try {
            privilegeRegistry.getPrivilege("");
            fail("invalid privilege name array");
        } catch (AccessControlException e) {
            // OK
        } catch (RepositoryException e) {
            // OK
        }
    }

    public void testGetPrivilegesFromNullNames() {
        try {
            privilegeRegistry.getPrivilege(null);
            fail("invalid privilege name (null)");
        } catch (Exception e) {
            // OK
        }
    }

    private Privilege buildUnregisteredPrivilege(final String name, final Privilege declaredAggr) {
        return new Privilege() {

            public String getName() {
                return name;
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