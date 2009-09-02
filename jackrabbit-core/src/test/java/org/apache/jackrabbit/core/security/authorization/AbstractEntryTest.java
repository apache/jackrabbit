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

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.test.api.security.AbstractAccessControlTest;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry;

/**
 * <code>AbstractEntryTest</code>...
 */
public abstract class AbstractEntryTest extends AbstractAccessControlTest {

    protected Principal testPrincipal;

    protected void setUp() throws Exception {
        super.setUp();
        testPrincipal = new Principal() {
            public String getName() {
                return "TestPrincipal";
            }
        };
    }

    protected JackrabbitAccessControlEntry createEntry(String[] privilegeNames, boolean isAllow)
            throws RepositoryException, NotExecutableException {
        Privilege[] privs = privilegesFromNames(privilegeNames);
        return createEntry(testPrincipal, privs, isAllow);
    }

    protected abstract JackrabbitAccessControlEntry createEntry(Principal principal, Privilege[] privileges, boolean isAllow)
            throws RepositoryException;

    public void testIsAllow() throws RepositoryException, NotExecutableException {
        JackrabbitAccessControlEntry tmpl = createEntry(new String[] {Privilege.JCR_READ}, true);
        assertTrue(tmpl.isAllow());

        tmpl = createEntry(new String[] {Privilege.JCR_READ}, false);
        assertFalse(tmpl.isAllow());
    }

    public void testGetPrincipal() throws RepositoryException, NotExecutableException {
        JackrabbitAccessControlEntry tmpl = createEntry(new String[] {Privilege.JCR_READ}, true);
        assertNotNull(tmpl.getPrincipal());
        assertEquals(testPrincipal.getName(), tmpl.getPrincipal().getName());
        assertSame(testPrincipal, tmpl.getPrincipal());
    }

    public void testGetPrivilegeBits() throws RepositoryException, NotExecutableException {
        JackrabbitAccessControlEntry tmpl = createEntry(new String[] {Privilege.JCR_READ}, true);

        int privs = PrivilegeRegistry.getBits(tmpl.getPrivileges());
        assertEquals(1, tmpl.getPrivileges().length);
        assertEquals(getAccessControlManager(superuser).privilegeFromName(Privilege.JCR_READ),
                tmpl.getPrivileges()[0]);

        tmpl = createEntry(new String[] {PrivilegeRegistry.REP_WRITE}, true);
        assertEquals(getAccessControlManager(superuser).privilegeFromName(PrivilegeRegistry.REP_WRITE),
                tmpl.getPrivileges()[0]);
    }

    public void testGetPrivileges() throws RepositoryException, NotExecutableException {
        JackrabbitAccessControlEntry entry = createEntry(new String[] {Privilege.JCR_READ}, true);

        Privilege[] privs = entry.getPrivileges();
        assertNotNull(privs);
        assertEquals(1, privs.length);
        assertEquals(privs[0], acMgr.privilegeFromName(Privilege.JCR_READ));
        assertTrue(PrivilegeRegistry.getBits(privs) == PrivilegeRegistry.getBits(entry.getPrivileges()));

        entry = createEntry(new String[] {PrivilegeRegistry.REP_WRITE}, true);
        privs = entry.getPrivileges();
        assertNotNull(privs);
        assertEquals(1, privs.length);
        assertEquals(privs[0], acMgr.privilegeFromName(PrivilegeRegistry.REP_WRITE));
        assertTrue(PrivilegeRegistry.getBits(privs) == PrivilegeRegistry.getBits(entry.getPrivileges()));

        entry = createEntry(new String[] {Privilege.JCR_ADD_CHILD_NODES,
                Privilege.JCR_REMOVE_CHILD_NODES}, true);
        privs = entry.getPrivileges();
        assertNotNull(privs);
        assertEquals(2, privs.length);

        Privilege[] param = privilegesFromNames(new String[] {
                Privilege.JCR_ADD_CHILD_NODES,
                Privilege.JCR_REMOVE_CHILD_NODES
        });
        assertEquals(Arrays.asList(param), Arrays.asList(privs));
        assertEquals(PrivilegeRegistry.getBits(privs), PrivilegeRegistry.getBits(entry.getPrivileges()));
    }

    public void testEquals() throws RepositoryException, NotExecutableException  {

        JackrabbitAccessControlEntry ace = createEntry(new String[] {Privilege.JCR_ALL}, true);
        List<JackrabbitAccessControlEntry> equalAces = new ArrayList<JackrabbitAccessControlEntry>();
        equalAces.add(createEntry(new String[] {Privilege.JCR_ALL}, true));

        Privilege[] privs = acMgr.privilegeFromName(Privilege.JCR_ALL).getDeclaredAggregatePrivileges();
        equalAces.add(createEntry(testPrincipal, privs, true));

        privs = acMgr.privilegeFromName(Privilege.JCR_ALL).getAggregatePrivileges();
        equalAces.add(createEntry(testPrincipal, privs, true));

        for (JackrabbitAccessControlEntry equalAce : equalAces) {
            assertEquals(ace, equalAce);
        }
    }

    public void testNotEquals() throws RepositoryException, NotExecutableException  {
        JackrabbitAccessControlEntry ace = createEntry(new String[] {Privilege.JCR_ALL}, true);
        List<JackrabbitAccessControlEntry> otherAces = new ArrayList<JackrabbitAccessControlEntry>();

        try {
            // ACE template with different principal
            Principal princ = new Principal() {
                public String getName() {
                    return "a name";
                }
            };
            Privilege[] privs = new Privilege[] {
                    acMgr.privilegeFromName(Privilege.JCR_ALL)
            };
            otherAces.add(createEntry(princ, privs, true));
        } catch (RepositoryException e) {
        }

        // ACE template with different privileges
        try {
            otherAces.add(createEntry(new String[] {Privilege.JCR_READ}, true));
        } catch (RepositoryException e) {
        }
        // ACE template with different 'allow' flag
        try {
            otherAces.add(createEntry(new String[] {Privilege.JCR_ALL}, false));
        } catch (RepositoryException e) {
        }
        // ACE template with different privileges and 'allows
        try {
            otherAces.add(createEntry(new String[] {PrivilegeRegistry.REP_WRITE}, false));
        } catch (RepositoryException e) {
        }

        // other ace impl
        final Privilege[] privs = new Privilege[] {
                acMgr.privilegeFromName(Privilege.JCR_ALL)
        };
        JackrabbitAccessControlEntry pe = new JackrabbitAccessControlEntry() {
            public boolean isAllow() {
                return true;
            }
            public int getPrivilegeBits() throws AccessControlException {
                return PrivilegeRegistry.getBits(privs);
            }
            public String[] getRestrictionNames() {
                return new String[0];
            }
            public Value getRestriction(String restrictionName) {
                return null;
            }
            public Principal getPrincipal() {
                return testPrincipal;
            }
            public Privilege[] getPrivileges() {
                return privs;
            }
        };
        otherAces.add(pe);

        for (JackrabbitAccessControlEntry otherAce : otherAces) {
            assertFalse(ace.equals(otherAce));
        }
    }

    public void testNullPrincipal() throws RepositoryException {
        try {
            Privilege[] privs = new Privilege[] {
                    acMgr.privilegeFromName(Privilege.JCR_ALL)
            };
            createEntry(null, privs, true);
            fail("Principal must not be null");
        } catch (Exception e) {
            // success
        }
    }

    public void testInvalidPrivilege() throws RepositoryException,
            NotExecutableException {
        Privilege invalidPriv = new Privilege() {
                public String getName() {
                    return "";
                }
                public boolean isAbstract() {
                    return false;
                }
                public boolean isAggregate() {
                    return false;
                }
                public Privilege[] getDeclaredAggregatePrivileges() {
                    return new Privilege[0];
                }
                public Privilege[] getAggregatePrivileges() {
                    return new Privilege[0];
                }
            };
        try {
            Privilege[] privs = new Privilege[] {invalidPriv, privilegesFromName(Privilege.JCR_READ)[0]};
            createEntry(testPrincipal, privs, true);
            fail("Principal must not be null");
        } catch (AccessControlException e) {
            // success
        }
    }
}