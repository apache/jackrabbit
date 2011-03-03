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
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.Privilege;

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

    public void testGetBits() throws RepositoryException {
        Privilege p1 = privilegeMgr.getPrivilege(Privilege.JCR_ADD_CHILD_NODES);
        Privilege p2 = privilegeMgr.getPrivilege(Privilege.JCR_REMOVE_CHILD_NODES);
        Privilege[] privs = new Privilege[] {p1, p2};

        int bits = getPrivilegeManagerImpl().getBits(privs);
        assertTrue(bits > PrivilegeRegistry.NO_PRIVILEGE);
        assertTrue(bits == (getPrivilegeManagerImpl().getBits(new Privilege[] {p1}) |
                getPrivilegeManagerImpl().getBits(new Privilege[] {p2})));
    }

    public void testGetBitsFromCustomPrivilege() throws AccessControlException {
        Privilege p = buildCustomPrivilege(Privilege.JCR_READ, null);
        try {
            getPrivilegeManagerImpl().getBits(new Privilege[] {p});
            fail("Retrieving bits from unknown privilege should fail.");
        } catch (AccessControlException e) {
            // ok
        }
    }

    public void testGetBitsFromCustomAggregatePrivilege() throws RepositoryException {
        Privilege p = buildCustomPrivilege("anyName", privilegeMgr.getPrivilege(Privilege.JCR_WRITE));
        try {
            getPrivilegeManagerImpl().getBits(new Privilege[] {p});
            fail("Retrieving bits from unknown privilege should fail.");
        } catch (AccessControlException e) {
            // ok
        }
    }

    public void testGetBitsFromNull() {
        try {
            getPrivilegeManagerImpl().getBits((Privilege[]) null);
            fail("Should throw AccessControlException");
        } catch (AccessControlException e) {
            // ok
        }
    }

    public void testGetBitsFromNullString() {
        try {
            getPrivilegeManagerImpl().getBits((String[]) null);
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

    public void testGetBitsFromEmptyStringArray() throws AccessControlException {
        try {
            getPrivilegeManagerImpl().getBits(new String[0]);
            fail("Should throw AccessControlException");
        } catch (AccessControlException e) {
            // ok
        }
    }

    public void testGetBitsWithInvalidPrivilege() {
        Privilege p = buildCustomPrivilege("anyName", null);
        try {
            getPrivilegeManagerImpl().getBits(new Privilege[] {p});
            fail();
        } catch (AccessControlException e) {
            // ok
        }
    }

    public void testGetPrivilegesFromBits() throws RepositoryException {
        Privilege[] pvs = getPrivilegeManagerImpl().getPrivileges(getPrivilegeManagerImpl().getBits(privilegesFromNames(new String[] {Privilege.JCR_READ_ACCESS_CONTROL})));

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
        int writeBits = getPrivilegeManagerImpl().getBits(privilegesFromNames(names));
        Privilege[] pvs = getPrivilegeManagerImpl().getPrivileges(writeBits);

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
        int writeBits = getPrivilegeManagerImpl().getBits(privilegesFromNames(names));
        Privilege[] pvs = getPrivilegeManagerImpl().getPrivileges(writeBits);

        assertTrue(pvs != null);
        assertTrue(pvs.length == 1);
        assertSamePrivilegeName(pvs[0].getName(), PrivilegeRegistry.REP_WRITE);
        assertTrue(pvs[0].isAggregate());

        names = new String[] {
                PrivilegeRegistry.REP_WRITE,
                Privilege.JCR_WRITE
        };
        writeBits = getPrivilegeManagerImpl().getBits(privilegesFromNames(names));
        pvs = getPrivilegeManagerImpl().getPrivileges(writeBits);

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
        int writeBits = getPrivilegeManagerImpl().getBits(privilegesFromNames(names));
        Privilege[] pvs = getPrivilegeManagerImpl().getPrivileges(writeBits);

        assertTrue(pvs != null);
        assertTrue(pvs.length == 2);
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