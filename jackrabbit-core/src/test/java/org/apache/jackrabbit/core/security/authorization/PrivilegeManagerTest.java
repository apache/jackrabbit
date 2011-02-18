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
import org.apache.jackrabbit.core.WorkspaceImpl;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.AccessDeniedException;
import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.Privilege;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <code>PrivilegeManagerTest</code>...
 */
public class PrivilegeManagerTest extends AbstractJCRTest {

    private NameResolver resolver;
    private PrivilegeManager privilegeMgr;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        resolver = (SessionImpl) superuser;
        privilegeMgr = ((WorkspaceImpl) superuser.getWorkspace()).getPrivilegeManager();
    }

    private void assertSamePrivilegeName(String expected, String present) throws NamespaceException, IllegalNameException {
        assertEquals("Privilege names are not the same", resolver.getQName(expected), resolver.getQName(present));
    }

    private Privilege[] privilegesFromNames(String[] privNames)
            throws RepositoryException {
        Privilege[] privs = new Privilege[privNames.length];
        for (int i = 0; i < privNames.length; i++) {
            privs[i] = privilegeMgr.getPrivilege(privNames[i]);
        }
        return privs;
    }

    public void testRegisteredPrivileges() throws RepositoryException {
        Privilege[] ps = privilegeMgr.getRegisteredPrivileges();

        List<Privilege> l = new ArrayList<Privilege>(Arrays.asList(ps));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_READ)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_ADD_CHILD_NODES)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_REMOVE_CHILD_NODES)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_MODIFY_PROPERTIES)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_REMOVE_NODE)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_READ_ACCESS_CONTROL)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_MODIFY_ACCESS_CONTROL)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_WRITE)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_ALL)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_LIFECYCLE_MANAGEMENT)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_LOCK_MANAGEMENT)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_NODE_TYPE_MANAGEMENT)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_RETENTION_MANAGEMENT)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_VERSION_MANAGEMENT)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(PrivilegeRegistry.REP_WRITE)));
        assertTrue(l.isEmpty());
    }

    public void testAllPrivilege() throws RepositoryException {
        Privilege p = privilegeMgr.getPrivilege(Privilege.JCR_ALL);
        assertSamePrivilegeName(p.getName(), Privilege.JCR_ALL);
        assertTrue(p.isAggregate());
        assertFalse(p.isAbstract());

        List<Privilege> l = new ArrayList<Privilege>(Arrays.asList(p.getAggregatePrivileges()));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_READ)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_ADD_CHILD_NODES)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_REMOVE_CHILD_NODES)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_MODIFY_PROPERTIES)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_REMOVE_NODE)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_READ_ACCESS_CONTROL)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_MODIFY_ACCESS_CONTROL)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_LIFECYCLE_MANAGEMENT)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_LOCK_MANAGEMENT)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_NODE_TYPE_MANAGEMENT)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_RETENTION_MANAGEMENT)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_VERSION_MANAGEMENT)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_WRITE)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(PrivilegeRegistry.REP_WRITE)));
        assertTrue(l.isEmpty());

        l = new ArrayList<Privilege>(Arrays.asList(p.getDeclaredAggregatePrivileges()));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_READ)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_WRITE)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(PrivilegeRegistry.REP_WRITE)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_READ_ACCESS_CONTROL)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_MODIFY_ACCESS_CONTROL)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_LIFECYCLE_MANAGEMENT)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_LOCK_MANAGEMENT)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_RETENTION_MANAGEMENT)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_VERSION_MANAGEMENT)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_NODE_TYPE_MANAGEMENT)));        
        assertTrue(l.isEmpty());
    }

    public void testGetBits() throws RepositoryException {
        Privilege p1 = privilegeMgr.getPrivilege(Privilege.JCR_ADD_CHILD_NODES);
        Privilege p2 = privilegeMgr.getPrivilege(Privilege.JCR_REMOVE_CHILD_NODES);
        Privilege[] privs = new Privilege[] {p1, p2};

        int bits = privilegeMgr.getBits(privs);
        assertTrue(bits > PrivilegeRegistry.NO_PRIVILEGE);
        assertTrue(bits == (privilegeMgr.getBits(new Privilege[] {p1}) |
                privilegeMgr.getBits(new Privilege[] {p2})));
    }

    public void testGetBitsFromCustomPrivilege() throws AccessControlException {
        Privilege p = buildCustomPrivilege(Privilege.JCR_READ, null);
        try {
            privilegeMgr.getBits(new Privilege[] {p});
            fail("Retrieving bits from unknown privilege should fail.");
        } catch (AccessControlException e) {
            // ok
        }
    }

    public void testGetBitsFromCustomAggregatePrivilege() throws RepositoryException {
        Privilege p = buildCustomPrivilege("anyName", privilegeMgr.getPrivilege(Privilege.JCR_WRITE));
        try {
            privilegeMgr.getBits(new Privilege[] {p});
            fail("Retrieving bits from unknown privilege should fail.");
        } catch (AccessControlException e) {
            // ok
        }
    }

    public void testGetBitsFromNull() {
        try {
            privilegeMgr.getBits((Privilege[]) null);
            fail("Should throw AccessControlException");
        } catch (AccessControlException e) {
            // ok
        }
    }

    public void testGetBitsFromNullString() {
        try {
            privilegeMgr.getBits((String[]) null);
            fail("Should throw AccessControlException");
        } catch (AccessControlException e) {
            // ok
        }
    }

    public void testGetBitsFromEmptyArray() throws AccessControlException {
        try {
            privilegeMgr.getBits(new Privilege[0]);
            fail("Should throw AccessControlException");
        } catch (AccessControlException e) {
            // ok
        }
    }

    public void testGetBitsFromEmptyStringArray() throws AccessControlException {
        try {
            privilegeMgr.getBits(new String[0]);
            fail("Should throw AccessControlException");
        } catch (AccessControlException e) {
            // ok
        }
    }

    public void testGetBitsWithInvalidPrivilege() {
        Privilege p = buildCustomPrivilege("anyName", null);
        try {
            privilegeMgr.getBits(new Privilege[] {p});
            fail();
        } catch (AccessControlException e) {
            // ok
        }
    }

    public void testGetPrivilegesFromBits() throws RepositoryException {
        Privilege[] pvs = privilegeMgr.getPrivileges(privilegeMgr.getBits(privilegesFromNames(new String[] {Privilege.JCR_READ_ACCESS_CONTROL})));

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
        int writeBits = privilegeMgr.getBits(privilegesFromNames(names));
        Privilege[] pvs = privilegeMgr.getPrivileges(writeBits);

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
        int writeBits = privilegeMgr.getBits(privilegesFromNames(names));
        Privilege[] pvs = privilegeMgr.getPrivileges(writeBits);

        assertTrue(pvs != null);
        assertTrue(pvs.length == 1);
        assertSamePrivilegeName(pvs[0].getName(), PrivilegeRegistry.REP_WRITE);
        assertTrue(pvs[0].isAggregate());

        names = new String[] {
                PrivilegeRegistry.REP_WRITE,
                Privilege.JCR_WRITE
        };
        writeBits = privilegeMgr.getBits(privilegesFromNames(names));
        pvs = privilegeMgr.getPrivileges(writeBits);

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
        int writeBits = privilegeMgr.getBits(privilegesFromNames(names));
        Privilege[] pvs = privilegeMgr.getPrivileges(writeBits);

        assertTrue(pvs != null);
        assertTrue(pvs.length == 2);
    }

    public void testGetPrivilegeFromName() throws AccessControlException, RepositoryException {
        Privilege p = privilegeMgr.getPrivilege(Privilege.JCR_READ);

        assertTrue(p != null);
        assertSamePrivilegeName(Privilege.JCR_READ, p.getName());
        assertFalse(p.isAggregate());

        p = privilegeMgr.getPrivilege(Privilege.JCR_WRITE);

        assertTrue(p != null);
        assertSamePrivilegeName(p.getName(), Privilege.JCR_WRITE);
        assertTrue(p.isAggregate());
    }

    public void testGetPrivilegesFromInvalidName() throws RepositoryException {
        try {
            privilegeMgr.getPrivilege("unknown");
            fail("invalid privilege name");
        } catch (AccessControlException e) {
            // OK
        }
    }

    public void testGetPrivilegesFromEmptyNames() {
        try {
            privilegeMgr.getPrivilege("");
            fail("invalid privilege name array");
        } catch (AccessControlException e) {
            // OK
        } catch (RepositoryException e) {
            // OK
        }
    }

    public void testGetPrivilegesFromNullNames() {
        try {
            privilegeMgr.getPrivilege(null);
            fail("invalid privilege name (null)");
        } catch (Exception e) {
            // OK
        }
    }

    public void testRegisterPrivilegeWithIllegalName() throws RepositoryException {
        Map<String, String[]> illegal = new HashMap<String, String[]>();
        illegal.put("invalid:privilegeName", new String[0]);
        illegal.put("jcr:newPrivilege", new String[] {"invalid:privilegeName"});
        illegal.put(".e:privilegeName", new String[0]);
        illegal.put("jcr:newPrivilege", new String[] {".e:privilegeName"});

        for (String illegalName : illegal.keySet()) {
            try {
                privilegeMgr.registerPrivilege(illegalName, true, illegal.get(illegalName));
                fail("Illegal name -> Exception expected");
            } catch (NamespaceException e) {
                // success
            } catch (IllegalNameException e) {
                // success
            }
        }
    }

    public void testRegisterPrivilegeAsNonAdmin() throws RepositoryException {
        Session s = getHelper().getReadOnlySession();
        try {
            ((WorkspaceImpl) s.getWorkspace()).getPrivilegeManager().registerPrivilege("test", true, new String[0]);
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