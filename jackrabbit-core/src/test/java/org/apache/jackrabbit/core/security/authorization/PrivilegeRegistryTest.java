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

import org.apache.jackrabbit.api.security.authorization.PrivilegeManager;
import org.apache.jackrabbit.commons.privilege.PrivilegeDefinition;
import org.apache.jackrabbit.commons.privilege.PrivilegeDefinitionWriter;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.Privilege;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    private int getBits(PrivilegeRegistry.Definition def) {
        return privilegeRegistry.getBits(new PrivilegeRegistry.Definition[] {def});
    }

    public void testGetAll() throws RepositoryException {

        PrivilegeRegistry.Definition[] defs = privilegeRegistry.getAll();

        List<PrivilegeRegistry.Definition> l = new ArrayList<PrivilegeRegistry.Definition>(Arrays.asList(defs));
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
        assertTrue(l.remove(privilegeRegistry.get(resolver.getQName(PrivilegeRegistry.REP_WRITE))));

        assertTrue(l.isEmpty());
    }


    public void testGet() throws RepositoryException {

        for (PrivilegeRegistry.Definition def : privilegeRegistry.getAll()) {

            PrivilegeRegistry.Definition d = privilegeRegistry.get(def.name);
            assertEquals(def, d);

            assertNotNull(d.name);
            assertEquals(d.name, d.getName());

            assertFalse(d.isAbstract);
            assertEquals(d.isAbstract, d.isAbstract());

            assertNotNull(d.declaredAggregateNames);
            List<Name> l = Arrays.asList(d.getDeclaredAggregateNames());
            assertTrue(d.declaredAggregateNames.containsAll(l));
            assertTrue(l.containsAll(d.declaredAggregateNames));

            assertTrue(getBits(d) > PrivilegeRegistry.NO_PRIVILEGE);
        }
    }

    public void testAggregates() throws RepositoryException {

        for (PrivilegeRegistry.Definition def : privilegeRegistry.getAll()) {
            if (def.declaredAggregateNames.isEmpty()) {
                continue; // ignore non aggregate
            }

            List<Name> l = Arrays.asList(def.getDeclaredAggregateNames());
            for (Name n : l) {
                PrivilegeRegistry.Definition d = privilegeRegistry.get(n);
                assertNotNull(d);
                Name[] names = privilegeRegistry.getNames(getBits(d));
                assertNotNull(names);
                assertEquals(1, names.length);
                assertEquals(d.name, names[0]);
            }
        }
    }

    public void testPrivilegeDefinition() throws RepositoryException {

        for (PrivilegeRegistry.Definition def : privilegeRegistry.getAll()) {

            assertNotNull(def.name);
            assertEquals(def.name, def.getName());

            assertFalse(def.isAbstract);
            assertEquals(def.isAbstract, def.isAbstract());

            assertNotNull(def.declaredAggregateNames);
            List<Name> l = Arrays.asList(def.getDeclaredAggregateNames());
            assertTrue(def.declaredAggregateNames.containsAll(l));
            assertTrue(l.containsAll(def.declaredAggregateNames));

            assertTrue(getBits(def) > PrivilegeRegistry.NO_PRIVILEGE);
        }
    }

    public void testJcrAll() throws RepositoryException {
        PrivilegeRegistry.Definition p = privilegeRegistry.get(NameConstants.JCR_ALL);
        assertEquals(p.getName(), NameConstants.JCR_ALL);
        assertFalse(p.declaredAggregateNames.isEmpty());
        assertFalse(p.isAbstract());

        Set<Name> l = new HashSet<Name>(p.declaredAggregateNames);
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
        assertTrue(l.isEmpty());
    }

    public void testJcrWrite() throws RepositoryException {
        Name rw = resolver.getQName(PrivilegeRegistry.REP_WRITE);
        PrivilegeRegistry.Definition p = privilegeRegistry.get(rw);

        assertEquals(p.getName(), rw);
        assertFalse(p.declaredAggregateNames.isEmpty());
        assertFalse(p.isAbstract());

        Set<Name> l = new HashSet<Name>(p.declaredAggregateNames);
        assertTrue(l.remove(NameConstants.JCR_WRITE));
        assertTrue(l.remove(NameConstants.JCR_NODE_TYPE_MANAGEMENT));
        assertTrue(l.isEmpty());
    }

    public void testRepWrite() throws RepositoryException {
        PrivilegeRegistry.Definition p = privilegeRegistry.get(NameConstants.JCR_WRITE);
        assertEquals(p.getName(), NameConstants.JCR_WRITE);
        assertFalse(p.declaredAggregateNames.isEmpty());
        assertFalse(p.isAbstract());

        Set<Name> l = new HashSet<Name>(p.declaredAggregateNames);
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
        assertTrue(l.isEmpty());

        l = new ArrayList<Privilege>(Arrays.asList(p.getDeclaredAggregatePrivileges()));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_READ)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_WRITE)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(PrivilegeRegistry.REP_WRITE)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_READ_ACCESS_CONTROL)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_MODIFY_ACCESS_CONTROL)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_LIFECYCLE_MANAGEMENT)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_LOCK_MANAGEMENT)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_RETENTION_MANAGEMENT)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_VERSION_MANAGEMENT)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_NODE_TYPE_MANAGEMENT)));
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

    public void testGetBitsFromCustomPrivilege() throws AccessControlException {
        Privilege p = buildCustomPrivilege(Privilege.JCR_READ, null);
        try {
            PrivilegeRegistry.getBits(new Privilege[] {p});
            fail("Retrieving bits from unknown privilege should fail.");
        } catch (AccessControlException e) {
            // ok
        }
    }

    public void testGetBitsFromCustomAggregatePrivilege() throws RepositoryException {
        Privilege p = buildCustomPrivilege("anyName", privilegeRegistry.getPrivilege(Privilege.JCR_WRITE));
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
        Privilege p = buildCustomPrivilege("anyName", null);
        try {
            PrivilegeRegistry.getBits(new Privilege[] {p});
            fail();
        } catch (AccessControlException e) {
            // ok
        }
    }

    public void testGetPrivilegesFromBits() throws RepositoryException {
        Privilege[] pvs = privilegeRegistry.getPrivileges(PrivilegeRegistry.getBits(privilegesFromNames(new String[] {Privilege.JCR_READ_ACCESS_CONTROL})));

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

    public void testInvalidCustomDefinitions() throws RepositoryException, FileSystemException, IOException {
        // setup the custom privilege file with cyclic references
        FileSystem fs = ((RepositoryImpl) superuser.getRepository()).getConfig().getFileSystem();
        FileSystemResource resource = new FileSystemResource(fs, "/privileges/custom_privileges.xml");
        if (!resource.exists()) {
            resource.makeParentDirs();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><privileges><privilege isAbstract=\"false\" name=\"test\"><contains name=\"test2\"/></privilege></privileges>");

        Writer writer = new OutputStreamWriter(resource.getOutputStream(), "utf-8");
        writer.write(sb.toString());
        writer.flush();
        writer.close();

        try {
            new PrivilegeRegistry(superuser.getWorkspace().getNamespaceRegistry(), fs);
            fail("Invalid names must be detected upon registry startup.");
        } catch (RepositoryException e) {
            // success
        } finally {
            fs.deleteFolder("/privileges");
        }
    }

    public void testCustomDefinitionsWithCyclicReferences() throws RepositoryException, FileSystemException, IOException {
        // setup the custom privilege file with cyclic references
        FileSystem fs = ((RepositoryImpl) superuser.getRepository()).getConfig().getFileSystem();
        FileSystemResource resource = new FileSystemResource(fs, "/privileges/custom_privileges.xml");
        if (!resource.exists()) {
            resource.makeParentDirs();
        }

        OutputStream out = resource.getOutputStream();
        try {
            List<PrivilegeDefinition> defs = new ArrayList<PrivilegeDefinition>();
            defs.add(new PrivilegeDefinition("test", false, new String[] {"test2"}));
            defs.add(new PrivilegeDefinition("test4", true, new String[] {"test5"}));
            defs.add(new PrivilegeDefinition("test5", false, new String[] {"test3"}));
            defs.add(new PrivilegeDefinition("test3", false, new String[] {"test"}));
            defs.add(new PrivilegeDefinition("test2", false, new String[] {"test4"}));
            PrivilegeDefinitionWriter pdw = new PrivilegeDefinitionWriter("text/xml");
            pdw.writeDefinitions(out, defs.toArray(new PrivilegeDefinition[defs.size()]), Collections.<String, String>emptyMap());

            new PrivilegeRegistry(superuser.getWorkspace().getNamespaceRegistry(), fs);
            fail("Cyclic definitions must be detected upon registry startup.");
        } catch (RepositoryException e) {
            // success
        } finally {
            out.close();
            fs.deleteFolder("/privileges");
        }
    }

    public void testRegisterBuiltInPrivilege() throws RepositoryException, IllegalNameException, FileSystemException {
        FileSystem fs = ((RepositoryImpl) superuser.getRepository()).getConfig().getFileSystem();
        try {
            PrivilegeRegistry pr = new PrivilegeRegistry(superuser.getWorkspace().getNamespaceRegistry(), fs);

            Map<Name, Set<Name>> builtIns = new HashMap<Name, Set<Name>>();
            builtIns.put(NameConstants.JCR_READ, Collections.<Name>emptySet());
            builtIns.put(NameConstants.JCR_LIFECYCLE_MANAGEMENT, Collections.singleton(NameConstants.JCR_ADD_CHILD_NODES));
            builtIns.put(PrivilegeRegistry.REP_WRITE_NAME, Collections.<Name>emptySet());
            builtIns.put(NameConstants.JCR_ALL, Collections.<Name>emptySet());

            for (Name builtInName : builtIns.keySet()) {
                try {
                    pr.registerDefinition(builtInName, false, builtIns.get(builtInName));
                    fail("Privilege name already in use -> Exception expected");
                } catch (RepositoryException e) {
                    // success
                }
            }
        } finally {
            fs.deleteFolder("/privileges");
        }
    }

    public void testRegisterInvalidNewAggregate() throws RepositoryException, IllegalNameException, FileSystemException {
        FileSystem fs = ((RepositoryImpl) superuser.getRepository()).getConfig().getFileSystem();
        try {
            PrivilegeRegistry pr = new PrivilegeRegistry(superuser.getWorkspace().getNamespaceRegistry(), fs);

            Map<Name, Set<Name>> newAggregates = new HashMap<Name, Set<Name>>();
            // same as jcr:read
            newAggregates.put(resolver.getQName("jcr:newAggregate"), Collections.singleton(NameConstants.JCR_READ));
            // aggregated combining built-in and an unknown privilege
            newAggregates.put(resolver.getQName("jcr:newAggregate"), createNameSet(NameConstants.JCR_READ, resolver.getQName("unknownPrivilege")));
            // aggregate containing unknown privilege
            newAggregates.put(resolver.getQName("newAggregate"), createNameSet(resolver.getQName("unknownPrivilege")));
            // aggregated combining built-in and custom
            newAggregates.put(resolver.getQName("newAggregate"), createNameSet(NameConstants.JCR_READ, resolver.getQName("unknownPrivilege")));
            // custom aggregated contains itself
            newAggregates.put(resolver.getQName("newAggregate"), createNameSet(resolver.getQName("newAggregate")));
            // same as rep:write
            newAggregates.put(resolver.getQName("repWriteAggregate"), createNameSet(NameConstants.JCR_MODIFY_PROPERTIES, NameConstants.JCR_ADD_CHILD_NODES, NameConstants.JCR_NODE_TYPE_MANAGEMENT, NameConstants.JCR_REMOVE_CHILD_NODES,NameConstants.JCR_REMOVE_NODE));
            // aggregating built-in -> currently not supported
            newAggregates.put(resolver.getQName("aggrBuiltIn"), createNameSet(NameConstants.JCR_MODIFY_PROPERTIES, NameConstants.JCR_READ));

            for (Name name : newAggregates.keySet()) {
                try {
                    pr.registerDefinition(name, true, newAggregates.get(name));
                    fail("New aggregate referring to unknown Privilege  -> Exception expected");
                } catch (RepositoryException e) {
                    // success
                }
            }
        } finally {
            fs.deleteFolder("/privileges");
        }
    }

    public void testRegisterInvalidNewAggregate2() throws RepositoryException, FileSystemException {
        FileSystem fs = ((RepositoryImpl) superuser.getRepository()).getConfig().getFileSystem();
        try {
            PrivilegeRegistry pr = new PrivilegeRegistry(superuser.getWorkspace().getNamespaceRegistry(), fs);

            Map<Name, Set<Name>> newCustomPrivs = new LinkedHashMap<Name, Set<Name>>();
            newCustomPrivs.put(resolver.getQName("new"), Collections.<Name>emptySet());
            newCustomPrivs.put(resolver.getQName("new2"), Collections.<Name>singleton(resolver.getQName("new")));

            for (Name name : newCustomPrivs.keySet()) {
                boolean isAbstract = true;
                Set<Name> aggrNames = newCustomPrivs.get(name);
                pr.registerDefinition(name, isAbstract, aggrNames);
            }

            Map<Name, Set<Name>> newAggregates = new HashMap<Name, Set<Name>>();
            // a new aggregate of custom and built-in privilege
            newAggregates.put(resolver.getQName("newA1"), createNameSet(resolver.getQName("new"), NameConstants.JCR_READ));
            // other illegal aggregates already represented by registered definition.
            newAggregates.put(resolver.getQName("newA2"), Collections.<Name>singleton(resolver.getQName("new")));
            newAggregates.put(resolver.getQName("newA3"), Collections.<Name>singleton(resolver.getQName("new2")));

            for (Name name : newAggregates.keySet()) {
                boolean isAbstract = false;
                Set<Name> aggrNames = newAggregates.get(name);

                try {
                    pr.registerDefinition(name, isAbstract, aggrNames);
                    fail("Invalid aggregation in definition '"+ name.toString()+"' : Exception expected");
                } catch (RepositoryException e) {
                    // success
                }
            }
        } finally {
            fs.deleteFolder("/privileges");
        }
    }

    public void testRegisterCustomPrivileges() throws RepositoryException, FileSystemException {
        FileSystem fs = ((RepositoryImpl) superuser.getRepository()).getConfig().getFileSystem();
        try {
            PrivilegeRegistry pr = new PrivilegeRegistry(superuser.getWorkspace().getNamespaceRegistry(), fs);

            Map<Name, Set<Name>> newCustomPrivs = new HashMap<Name, Set<Name>>();
            newCustomPrivs.put(resolver.getQName("new"), Collections.<Name>emptySet());
            newCustomPrivs.put(resolver.getQName("test:new"), Collections.<Name>emptySet());

            for (Name name : newCustomPrivs.keySet()) {
                boolean isAbstract = true;
                Set<Name> aggrNames = newCustomPrivs.get(name);

                pr.registerDefinition(name, isAbstract, aggrNames);

                // validate definition
                PrivilegeRegistry.Definition definition = pr.get(name);
                assertNotNull(definition);
                assertTrue(definition.isCustom());
                assertEquals(name, definition.getName());
                assertTrue(definition.isAbstract());
                assertTrue(definition.declaredAggregateNames.isEmpty());
                assertEquals(aggrNames.size(), definition.declaredAggregateNames.size());
                for (Name n : aggrNames) {
                    assertTrue(definition.declaredAggregateNames.contains(n));
                }
                assertEquals(PrivilegeRegistry.NO_PRIVILEGE, getBits(definition));

                List<Name> allAgg = Arrays.asList(pr.get(NameConstants.JCR_ALL).getDeclaredAggregateNames());
                assertTrue(allAgg.contains(name));

                // re-read the filesystem resource and check if definition is correct
                PrivilegeRegistry registry = new PrivilegeRegistry(superuser.getWorkspace().getNamespaceRegistry(), fs);
                PrivilegeRegistry.Definition def = registry.get(name);
                assertEquals(isAbstract, def.isAbstract);
                assertEquals(aggrNames.size(), def.declaredAggregateNames.size());
                for (Name n : aggrNames) {
                    assertTrue(def.declaredAggregateNames.contains(n));
                }

                assertPrivilege(pr, (SessionImpl) superuser, definition);
            }

            Map<Name, Set<Name>> newAggregates = new HashMap<Name, Set<Name>>();
            // a new aggregate of custom privileges
            newAggregates.put(resolver.getQName("newA2"), createNameSet(resolver.getQName("test:new"), resolver.getQName("new")));

            for (Name name : newAggregates.keySet()) {
                boolean isAbstract = false;
                Set<Name> aggrNames = newAggregates.get(name);
                pr.registerDefinition(name, isAbstract, aggrNames);
                PrivilegeRegistry.Definition definition = pr.get(name);

                assertNotNull(definition);
                assertTrue(definition.isCustom());                
                assertEquals(name, definition.getName());
                assertFalse(definition.isAbstract());
                assertFalse(definition.declaredAggregateNames.isEmpty());
                assertEquals(aggrNames.size(), definition.declaredAggregateNames.size());
                for (Name n : aggrNames) {
                    assertTrue(definition.declaredAggregateNames.contains(n));
                }

                assertEquals(PrivilegeRegistry.NO_PRIVILEGE, getBits(definition));

                List<Name> allAgg = Arrays.asList(pr.get(NameConstants.JCR_ALL).getDeclaredAggregateNames());
                assertTrue(allAgg.contains(name));

                // re-read the filesystem resource and check if definition is correct
                PrivilegeRegistry registry = new PrivilegeRegistry(superuser.getWorkspace().getNamespaceRegistry(), fs);
                PrivilegeRegistry.Definition def = registry.get(name);
                assertEquals(isAbstract, def.isAbstract);
                assertEquals(isAbstract, def.isAbstract);
                assertEquals(aggrNames.size(), def.declaredAggregateNames.size());
                for (Name n : aggrNames) {
                    assertTrue(def.declaredAggregateNames.contains(n));
                }

                assertPrivilege(registry, (SessionImpl) superuser, def);
            }
        } finally {
            fs.deleteFolder("/privileges");
        }
    }

    private static void assertPrivilege(PrivilegeRegistry registry, SessionImpl session, PrivilegeRegistry.Definition def) throws RepositoryException {

        PrivilegeManagerImpl pmgr = new PrivilegeManagerImpl(registry, session);
        Privilege p = pmgr.getPrivilege(session.getJCRName(def.getName()));

        assertNotNull(p);
        
        assertEquals(def.isCustom(), pmgr.isCustomPrivilege(p));
        assertEquals(def.isAbstract(), p.isAbstract());
        Name[] danames = def.getDeclaredAggregateNames();
        assertEquals(danames.length > 0, p.isAggregate());
        assertEquals(danames.length, p.getDeclaredAggregatePrivileges().length);
    }

    public void testCustomEquivalentDefinitions() throws RepositoryException, FileSystemException, IOException {
        // setup the custom privilege file with cyclic references
        FileSystem fs = ((RepositoryImpl) superuser.getRepository()).getConfig().getFileSystem();
        FileSystemResource resource = new FileSystemResource(fs, "/privileges/custom_privileges.xml");
        if (!resource.exists()) {
            resource.makeParentDirs();
        }

        OutputStream out = resource.getOutputStream();
        try {
            List<PrivilegeDefinition> defs = new ArrayList<PrivilegeDefinition>();
            defs.add(new PrivilegeDefinition("test", false, new String[] {"test2","test3"}));
            defs.add(new PrivilegeDefinition("test2", true, new String[] {"test4"}));
            defs.add(new PrivilegeDefinition("test3", true, new String[] {"test5"}));
            defs.add(new PrivilegeDefinition("test4", true, new String[0]));
            defs.add(new PrivilegeDefinition("test5", true, new String[0]));

            // the equivalent definition to 'test'
            defs.add(new PrivilegeDefinition("test6", false, new String[] {"test2","test5"}));
            
            PrivilegeDefinitionWriter pdw = new PrivilegeDefinitionWriter("text/xml");
            pdw.writeDefinitions(out, defs.toArray(new PrivilegeDefinition[defs.size()]), Collections.<String, String>emptyMap());

            new PrivilegeRegistry(superuser.getWorkspace().getNamespaceRegistry(), fs);
            fail("Equivalent definitions must be detected upon registry startup.");
        } catch (RepositoryException e) {
            // success
        } finally {
            out.close();
            fs.deleteFolder("/privileges");
        }
    }

    public void testRegister100CustomPrivileges() throws RepositoryException, FileSystemException {
        FileSystem fs = ((RepositoryImpl) superuser.getRepository()).getConfig().getFileSystem();
        try {
            PrivilegeRegistry pr = new PrivilegeRegistry(superuser.getWorkspace().getNamespaceRegistry(), fs);

            for (int i = 0; i < 100; i++) {
                boolean isAbstract = true;
                Name name = ((SessionImpl) superuser).getQName("test"+i);
                pr.registerDefinition(name, isAbstract, Collections.<Name>emptySet());
                PrivilegeRegistry.Definition definition = pr.get(name);

                assertNotNull(definition);
                assertEquals(name, definition.getName());
            }
        } finally {
            fs.deleteFolder("/privileges");
        }
    }

    private static Set<Name> createNameSet(Name... names) {
        Set<Name> set = new HashSet<Name>();
        set.addAll(Arrays.asList(names));
        return set;
    }

    private Privilege buildCustomPrivilege(final String name, final Privilege declaredAggr) {
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