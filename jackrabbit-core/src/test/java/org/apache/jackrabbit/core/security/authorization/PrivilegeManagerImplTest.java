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

import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.api.security.authorization.PrivilegeManagerTest;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.AccessDeniedException;
import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.Privilege;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * <code>PrivilegeManagerTest</code>...
 */
public class PrivilegeManagerImplTest extends PrivilegeManagerTest {

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        if (!(privilegeMgr instanceof PrivilegeManagerImpl)) {
            throw new NotExecutableException("PrivilegeManagerImpl expected");
        }
    }

    private PrivilegeManagerImpl getPrivilegeManagerImpl() {
        return (PrivilegeManagerImpl) privilegeMgr;
    }

    private Privilege[] privilegesFromNames(String[] privNames)
            throws RepositoryException {
        Privilege[] privs = new Privilege[privNames.length];
        for (int i = 0; i < privNames.length; i++) {
            privs[i] = privilegeMgr.getPrivilege(privNames[i]);
        }
        return privs;
    }
    
    private void assertPrivilege(Privilege priv, String name, boolean isAggregate, boolean isAbstract) throws NamespaceException, IllegalNameException {
        assertNotNull(priv);
        assertPrivilegeName(name, priv.getName());
        assertEquals(isAggregate, priv.isAggregate());
        assertEquals(isAbstract, priv.isAbstract());
    }

    private void assertPrivilegeName(String name, String name2) throws NamespaceException, IllegalNameException {
        if (!((SessionImpl) superuser).getQName(name).equals(((SessionImpl) superuser).getQName(name2))) {
            fail();
        }
    }
    
    public void testGetRegisteredPrivileges() throws RepositoryException {
        Privilege[] registered = privilegeMgr.getRegisteredPrivileges();
        Set<Privilege> set = new HashSet<Privilege>();
        Privilege all = privilegeMgr.getPrivilege(Privilege.JCR_ALL);
        set.add(all);
        set.addAll(Arrays.asList(all.getAggregatePrivileges()));

        for (Privilege p : registered) {
            assertTrue(set.remove(p));
        }
        assertTrue(set.isEmpty());
    }
    
    public void testGetPrivilege() throws RepositoryException {
        assertPrivilege(privilegeMgr.getPrivilege(Privilege.JCR_READ), Privilege.JCR_READ, false, false);
        assertPrivilege(privilegeMgr.getPrivilege(Privilege.JCR_ADD_CHILD_NODES), Privilege.JCR_ADD_CHILD_NODES, false, false);
        assertPrivilege(privilegeMgr.getPrivilege(Privilege.JCR_REMOVE_CHILD_NODES), Privilege.JCR_REMOVE_CHILD_NODES, false, false);
        assertPrivilege(privilegeMgr.getPrivilege(Privilege.JCR_MODIFY_PROPERTIES), Privilege.JCR_MODIFY_PROPERTIES, false, false);
        assertPrivilege(privilegeMgr.getPrivilege(Privilege.JCR_REMOVE_NODE), Privilege.JCR_REMOVE_NODE, false, false);
        assertPrivilege(privilegeMgr.getPrivilege(Privilege.JCR_READ_ACCESS_CONTROL), Privilege.JCR_READ_ACCESS_CONTROL, false, false);
        assertPrivilege(privilegeMgr.getPrivilege(Privilege.JCR_MODIFY_ACCESS_CONTROL), Privilege.JCR_MODIFY_ACCESS_CONTROL, false, false);
        assertPrivilege(privilegeMgr.getPrivilege(Privilege.JCR_LIFECYCLE_MANAGEMENT), Privilege.JCR_LIFECYCLE_MANAGEMENT, false, false);
        assertPrivilege(privilegeMgr.getPrivilege(Privilege.JCR_LOCK_MANAGEMENT), Privilege.JCR_LOCK_MANAGEMENT, false, false);
        assertPrivilege(privilegeMgr.getPrivilege(Privilege.JCR_NODE_TYPE_MANAGEMENT), Privilege.JCR_NODE_TYPE_MANAGEMENT, false, false);
        assertPrivilege(privilegeMgr.getPrivilege(Privilege.JCR_RETENTION_MANAGEMENT), Privilege.JCR_RETENTION_MANAGEMENT, false, false);
        assertPrivilege(privilegeMgr.getPrivilege(Privilege.JCR_VERSION_MANAGEMENT), Privilege.JCR_VERSION_MANAGEMENT, false, false);

        // repo-level operation privileges
        assertPrivilege(privilegeMgr.getPrivilege(NameConstants.JCR_NAMESPACE_MANAGEMENT.toString()), NameConstants.JCR_NAMESPACE_MANAGEMENT.toString() , false, false);
        assertPrivilege(privilegeMgr.getPrivilege(NameConstants.JCR_NODE_TYPE_DEFINITION_MANAGEMENT.toString()), NameConstants.JCR_NODE_TYPE_DEFINITION_MANAGEMENT.toString(), false, false);
        assertPrivilege(privilegeMgr.getPrivilege(NameConstants.JCR_WORKSPACE_MANAGEMENT.toString()), NameConstants.JCR_WORKSPACE_MANAGEMENT.toString(), false, false);

        // aggregates
        assertPrivilege(privilegeMgr.getPrivilege(Privilege.JCR_ALL), Privilege.JCR_ALL, true, false);
        assertPrivilege(privilegeMgr.getPrivilege(Privilege.JCR_WRITE), Privilege.JCR_WRITE, true, false);
        assertPrivilege(privilegeMgr.getPrivilege(PrivilegeRegistry.REP_WRITE), PrivilegeRegistry.REP_WRITE, true, false);
    }

    public void testGetBits() throws RepositoryException {
        Privilege p1 = privilegeMgr.getPrivilege(Privilege.JCR_ADD_CHILD_NODES);
        Privilege p2 = privilegeMgr.getPrivilege(Privilege.JCR_REMOVE_CHILD_NODES);
        Privilege[] privs = new Privilege[] {p1, p2};

        PrivilegeBits bits = getPrivilegeManagerImpl().getBits(privs);
        assertFalse(bits.isEmpty());
        PrivilegeBits other = PrivilegeBits.getInstance(getPrivilegeManagerImpl().getBits(p1));
        other.add(getPrivilegeManagerImpl().getBits(p2));
        assertEquals(bits, other);
    }

    public void testGetBitsFromCustomPrivilege() throws AccessControlException {
        Privilege p = buildCustomPrivilege(Privilege.JCR_READ, null);
        try {
            getPrivilegeManagerImpl().getBits(p);
            fail("Retrieving bits from unknown privilege should fail.");
        } catch (AccessControlException e) {
            // ok
        }
    }

    public void testGetBitsFromCustomAggregatePrivilege() throws RepositoryException {
        Privilege p = buildCustomPrivilege("anyName", privilegeMgr.getPrivilege(Privilege.JCR_WRITE));
        try {
            getPrivilegeManagerImpl().getBits(p);
            fail("Retrieving bits from unknown privilege should fail.");
        } catch (AccessControlException e) {
            // ok
        }
    }

    public void testGetBitsFromNull() {
        try {
            getPrivilegeManagerImpl().getBits((Privilege) null);
            fail("Should throw AccessControlException");
        } catch (AccessControlException e) {
            // ok
        }

        try {
            getPrivilegeManagerImpl().getBits((Privilege[]) null);
            fail("Should throw AccessControlException");
        } catch (AccessControlException e) {
            // ok
        }
    }

    public void testGetBitsFromEmptyArray() throws AccessControlException {
        try {
            getPrivilegeManagerImpl().getBits(new Privilege[0]);
            fail("Should throw AccessControlException");
        } catch (AccessControlException e) {
            // ok
        }
    }

    public void testGetBitsFromArrayContainingNull() throws RepositoryException {
        try {
            getPrivilegeManagerImpl().getBits(privilegeMgr.getPrivilege(Privilege.JCR_READ), null);
            fail("Should throw AccessControlException");
        } catch (AccessControlException e) {
            // ok
        }
    }

    public void testGetBitsWithInvalidPrivilege() {
        Privilege p = buildCustomPrivilege("anyName", null);
        try {
            getPrivilegeManagerImpl().getBits(p);
            fail();
        } catch (AccessControlException e) {
            // ok
        }
    }

    public void testGetPrivilegesFromBits() throws RepositoryException {
        Set<Privilege> pvs = getPrivilegeManagerImpl().getPrivileges(getPrivilegeManagerImpl().getBits(privilegesFromNames(new String[] {Privilege.JCR_READ_ACCESS_CONTROL})));

        assertTrue(pvs != null);
        assertTrue(pvs.size() == 1);
        assertSamePrivilegeName(pvs.iterator().next().getName(), Privilege.JCR_READ_ACCESS_CONTROL);
    }

    public void testGetPrivilegesFromBits2() throws RepositoryException {
        String[] names = new String[] {
                Privilege.JCR_ADD_CHILD_NODES,
                Privilege.JCR_REMOVE_CHILD_NODES,
                Privilege.JCR_REMOVE_NODE,
                Privilege.JCR_MODIFY_PROPERTIES
        };
        PrivilegeBits writeBits = getPrivilegeManagerImpl().getBits(privilegesFromNames(names));
        Set<Privilege> pvs = getPrivilegeManagerImpl().getPrivileges(writeBits);

        assertTrue(pvs != null);
        assertTrue(pvs.size() == 1);
        Privilege p = pvs.iterator().next();
        assertSamePrivilegeName(p.getName(), Privilege.JCR_WRITE);
        assertTrue(p.isAggregate());
        assertTrue(p.getDeclaredAggregatePrivileges().length == names.length);
    }

    public void testGetPrivilegesFromBits3() throws RepositoryException {
        String[] names = new String[] {
                PrivilegeRegistry.REP_WRITE
        };
        PrivilegeBits writeBits = getPrivilegeManagerImpl().getBits(privilegesFromNames(names));
        Set<Privilege> pvs = getPrivilegeManagerImpl().getPrivileges(writeBits);

        assertTrue(pvs != null);
        assertTrue(pvs.size() == 1);
        Privilege p = pvs.iterator().next();
        assertSamePrivilegeName(p.getName(), PrivilegeRegistry.REP_WRITE);
        assertTrue(p.isAggregate());

        names = new String[] {
                PrivilegeRegistry.REP_WRITE,
                Privilege.JCR_WRITE
        };
        writeBits = getPrivilegeManagerImpl().getBits(privilegesFromNames(names));
        pvs = getPrivilegeManagerImpl().getPrivileges(writeBits);

        assertTrue(pvs != null);
        assertTrue(pvs.size() == 1);
        p = pvs.iterator().next();
        assertSamePrivilegeName(p.getName(), PrivilegeRegistry.REP_WRITE);
        assertTrue(p.isAggregate());
        assertTrue(p.getDeclaredAggregatePrivileges().length == names.length);
    }

    public void testGetPrivilegesFromBits4() throws RepositoryException {
        String[] names = new String[] {
                PrivilegeRegistry.REP_WRITE,
                Privilege.JCR_LIFECYCLE_MANAGEMENT
        };
        PrivilegeBits writeBits = getPrivilegeManagerImpl().getBits(privilegesFromNames(names));
        Set<Privilege> pvs = getPrivilegeManagerImpl().getPrivileges(writeBits);

        assertTrue(pvs != null);
        assertTrue(pvs.size() == 2);
    }

    public void testRegisterPrivilegeAsNonAdmin() throws RepositoryException {
        Session s = getHelper().getReadOnlySession();
        try {
            ((JackrabbitWorkspace) s.getWorkspace()).getPrivilegeManager().registerPrivilege("test", true, new String[0]);
            fail("Only admin is allowed to register privileges.");
        } catch (AccessDeniedException e) {
            // success
        } finally {
            s.logout();
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
