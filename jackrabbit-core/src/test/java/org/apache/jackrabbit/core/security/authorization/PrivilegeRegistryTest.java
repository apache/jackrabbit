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
import org.apache.jackrabbit.spi.commons.conversion.ParsingNameResolver;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.spi.Name;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <code>PrivilegeRegistryTest</code>...
 */
public class PrivilegeRegistryTest extends TestCase {

    private NameResolver resolver;
    private PrivilegeRegistry privilegeRegistry;

    protected void setUp() throws Exception {
        super.setUp();
        NamespaceResolver nsResolver = new NamespaceResolver() {
            public String getURI(String prefix) throws NamespaceException {
                if (Name.NS_JCR_PREFIX.equals(prefix)) {
                    return Name.NS_JCR_URI;
                } else if (Name.NS_EMPTY_PREFIX.equals(prefix)) {
                    return Name.NS_DEFAULT_URI;
                } else {
                    throw new NamespaceException();
                }
            }
            public String getPrefix(String uri) throws NamespaceException {
                if (Name.NS_JCR_URI.equals(uri)) {
                    return Name.NS_JCR_PREFIX;
                } else if (Name.NS_DEFAULT_URI.equals(uri)) {
                    return Name.NS_EMPTY_PREFIX;
                } else {
                    throw new NamespaceException();
                }
            }
        };
        resolver = new ParsingNameResolver(NameFactoryImpl.getInstance(), nsResolver);
        privilegeRegistry = new PrivilegeRegistry(resolver);
    }

    private void assertSamePrivilegeName(String expected, String present) throws RepositoryException {
        assertEquals("Privilege names are not the same", resolver.getQName(expected), resolver.getQName(present));
    }

    public void testRegisteredPrivileges() throws RepositoryException {
        Privilege[] ps = privilegeRegistry.getRegisteredPrivileges();

        List l = new ArrayList(Arrays.asList(ps));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_READ)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_ADD_CHILD_NODES)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_REMOVE_CHILD_NODES)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_MODIFY_PROPERTIES)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_REMOVE_NODE)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_READ_ACCESS_CONTROL)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_MODIFY_ACCESS_CONTROL)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_WRITE)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_ALL)));
        assertTrue(l.isEmpty());
    }

    public void testAllPrivilege() throws RepositoryException {
        Privilege p = privilegeRegistry.getPrivilege(Privilege.JCR_ALL);
        assertSamePrivilegeName(p.getName(), Privilege.JCR_ALL);
        assertTrue(p.isAggregate());
        assertFalse(p.isAbstract());

        List l = new ArrayList(Arrays.asList(p.getAggregatePrivileges()));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_READ)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_ADD_CHILD_NODES)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_REMOVE_CHILD_NODES)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_MODIFY_PROPERTIES)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_REMOVE_NODE)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_READ_ACCESS_CONTROL)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_MODIFY_ACCESS_CONTROL)));
        assertTrue(l.isEmpty());

        l = new ArrayList(Arrays.asList(p.getDeclaredAggregatePrivileges()));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_READ)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_WRITE)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_REMOVE_NODE)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_READ_ACCESS_CONTROL)));
        assertTrue(l.remove(privilegeRegistry.getPrivilege(Privilege.JCR_MODIFY_ACCESS_CONTROL)));
        assertTrue(l.isEmpty());
    }

    public void testGetBits() throws RepositoryException {
        Privilege[] privs = new Privilege[] {privilegeRegistry.getPrivilege(Privilege.JCR_ADD_CHILD_NODES),
                                             privilegeRegistry.getPrivilege(Privilege.JCR_REMOVE_CHILD_NODES)};

        int bits = PrivilegeRegistry.getBits(privs);
        assertTrue(bits > PrivilegeRegistry.NO_PRIVILEGE);
        assertTrue(bits == (PrivilegeRegistry.ADD_CHILD_NODES | PrivilegeRegistry.REMOVE_CHILD_NODES));
    }

    public void testGetBitsFromCustomPrivilege() throws AccessControlException {
        Privilege p = buildCustomPrivilege(Privilege.JCR_READ, null);
        try {
            int bits = PrivilegeRegistry.getBits(new Privilege[] {p});
            fail("Retrieving bits from unknown privilege should fail.");
        } catch (AccessControlException e) {
            // ok
        }
    }

    public void testGetBitsFromCustomAggregatePrivilege() throws RepositoryException {
        Privilege p = buildCustomPrivilege("anyName", privilegeRegistry.getPrivilege(Privilege.JCR_WRITE));
        try {
            int bits = PrivilegeRegistry.getBits(new Privilege[] {p});
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
        Privilege[] pvs = privilegeRegistry.getPrivileges(PrivilegeRegistry.READ_AC);

        assertTrue(pvs != null);
        assertTrue(pvs.length == 1);
        assertSamePrivilegeName(pvs[0].getName(), Privilege.JCR_READ_ACCESS_CONTROL);
    }

    public void testGetPrivilegesFromBits2() throws RepositoryException {
        int writeBits = PrivilegeRegistry.ADD_CHILD_NODES | PrivilegeRegistry.REMOVE_CHILD_NODES | PrivilegeRegistry.MODIFY_PROPERTIES;
        Privilege[] pvs = privilegeRegistry.getPrivileges(writeBits);

        assertTrue(pvs != null);
        assertTrue(pvs.length == 1);
        assertSamePrivilegeName(pvs[0].getName(), Privilege.JCR_WRITE);
        assertTrue(pvs[0].isAggregate());
        assertTrue(pvs[0].getDeclaredAggregatePrivileges().length == 3);
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