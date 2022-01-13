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
import java.util.Collections;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.authorization.PrivilegeCollection;
import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.core.security.TestPrincipal;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.test.api.security.AbstractAccessControlTest;

/**
 * <code>AbstractACLTemplateTest</code>...
 */
public abstract class AbstractACLTemplateTest extends AbstractAccessControlTest {

    protected Principal testPrincipal;
    protected PrincipalManager principalMgr;
    protected PrivilegeManagerImpl privilegeMgr;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        if (!(superuser instanceof JackrabbitSession)) {
            throw new NotExecutableException();
        }

        principalMgr = ((JackrabbitSession) superuser).getPrincipalManager();
        PrincipalIterator it = principalMgr.getPrincipals(PrincipalManager.SEARCH_TYPE_NOT_GROUP);
        if (it.hasNext()) {
            testPrincipal = it.nextPrincipal();
        } else {
            throw new NotExecutableException();
        }
        privilegeMgr = (PrivilegeManagerImpl) ((JackrabbitWorkspace) superuser.getWorkspace()).getPrivilegeManager();
    }

    protected void assertSamePrivileges(Privilege[] privs1, Privilege[] privs2) throws AccessControlException {
        assertEquals(privilegeMgr.getBits(privs1), privilegeMgr.getBits(privs2));
    }

    protected abstract String getTestPath();

    protected abstract JackrabbitAccessControlList createEmptyTemplate(String path) throws RepositoryException;

    protected abstract Principal getSecondPrincipal() throws Exception;

    public void testEmptyTemplate() throws RepositoryException {
        JackrabbitAccessControlList pt = createEmptyTemplate(getTestPath());

        assertNotNull(pt.getAccessControlEntries());
        assertTrue(pt.getAccessControlEntries().length == 0);
        assertTrue(pt.size() == pt.getAccessControlEntries().length);
        assertTrue(pt.isEmpty());
    }

    public void testGetPath() throws RepositoryException {
        JackrabbitAccessControlList pt = createEmptyTemplate(getTestPath());
        assertEquals(getTestPath(), pt.getPath());
    }

    public void testAddInvalidEntry() throws RepositoryException, NotExecutableException {
        Principal unknownPrincipal;
        if (!principalMgr.hasPrincipal("an unknown principal")) {
            unknownPrincipal = new TestPrincipal("an unknown principal");
        } else {
            throw new NotExecutableException();
        }
        JackrabbitAccessControlList pt = createEmptyTemplate(getTestPath());
        try {
            pt.addAccessControlEntry(unknownPrincipal, privilegesFromName(Privilege.JCR_READ));
            fail("Adding an ACE with an unknown principal should fail");
        } catch (AccessControlException e) {
            // success
        }
    }

    public void testAddInvalidEntry2() throws RepositoryException {
        JackrabbitAccessControlList pt = createEmptyTemplate(getTestPath());
        try {
            pt.addAccessControlEntry(testPrincipal, new Privilege[0]);
            fail("Adding an ACE with invalid privileges should fail");
        } catch (AccessControlException e) {
            // success
        }
    }

    public void testRemoveInvalidEntry() throws RepositoryException {
        JackrabbitAccessControlList pt = createEmptyTemplate(getTestPath());
        try {
            pt.removeAccessControlEntry(new JackrabbitAccessControlEntry() {
                public boolean isAllow() {
                    return false;
                }
                public String[] getRestrictionNames() {
                    return new String[0];
                }
                public Value getRestriction(String restrictionName) {
                    return null;
                }

                public Value[] getRestrictions(String restrictionName) throws RepositoryException {
                    return null;
                }

                @Override
                public PrivilegeCollection getPrivilegeCollection() throws RepositoryException {
                    throw new UnsupportedRepositoryOperationException();
                }

                public Principal getPrincipal() {
                    return testPrincipal;
                }

                public Privilege[] getPrivileges() {
                    try {
                        return privilegesFromName(Privilege.JCR_READ);
                    } catch (Exception e) {
                        return new Privilege[0];
                    }
                }
            });
            fail("Passing an unknown ACE should fail");
        } catch (AccessControlException e) {
            // success
        }
    }

    public void testRemoveInvalidEntry2() throws RepositoryException {
        JackrabbitAccessControlList pt = createEmptyTemplate(getTestPath());
        try {
            pt.removeAccessControlEntry(new JackrabbitAccessControlEntry() {
                public boolean isAllow() {
                    return false;
                }
                public int getPrivilegeBits() {
                    return 0;
                }
                public String[] getRestrictionNames() {
                    return new String[0];
                }
                public Value getRestriction(String restrictionName) {
                    return null;
                }

                public Value[] getRestrictions(String restrictionName) throws RepositoryException {
                    return null;
                }

                @Override
                public PrivilegeCollection getPrivilegeCollection() throws RepositoryException {
                    throw new UnsupportedRepositoryOperationException();
                }

                public Principal getPrincipal() {
                    return testPrincipal;
                }
                public Privilege[] getPrivileges() {
                    return new Privilege[0];
                }
            });
            fail("Passing a ACE with invalid privileges should fail");
        } catch (AccessControlException e) {
            // success
        }
    }

    public void testAddEntry() throws RepositoryException, NotExecutableException {
        JackrabbitAccessControlList pt = createEmptyTemplate(getTestPath());
        Privilege[] privs = privilegesFromName(Privilege.JCR_READ);
        assertTrue(pt.addEntry(testPrincipal, privs, true, Collections.<String, Value>emptyMap()));
    }

    public void testAddEntryTwice() throws RepositoryException, NotExecutableException {
        JackrabbitAccessControlList pt = createEmptyTemplate(getTestPath());
        Privilege[] privs = privilegesFromName(Privilege.JCR_READ);

        pt.addEntry(testPrincipal, privs, true, Collections.<String, Value>emptyMap());
        assertFalse(pt.addEntry(testPrincipal, privs, true, Collections.<String, Value>emptyMap()));
    }

    public void testEffect() throws RepositoryException, NotExecutableException {
        JackrabbitAccessControlList pt = createEmptyTemplate(getTestPath());
        Privilege[] read = privilegesFromName(Privilege.JCR_READ);
        Privilege[] modProp = privilegesFromName(Privilege.JCR_MODIFY_PROPERTIES);

        pt.addAccessControlEntry(testPrincipal, read);

        // add deny entry for mod_props
        assertTrue(pt.addEntry(testPrincipal, modProp, false, null));

        // test net-effect
        PrivilegeBits allows = PrivilegeBits.getInstance();
        PrivilegeBits denies = PrivilegeBits.getInstance();
        AccessControlEntry[] entries = pt.getAccessControlEntries();
        for (AccessControlEntry ace : entries) {
            if (testPrincipal.equals(ace.getPrincipal()) && ace instanceof JackrabbitAccessControlEntry) {
                PrivilegeBits entryBits = privilegeMgr.getBits(ace.getPrivileges());
                if (((JackrabbitAccessControlEntry) ace).isAllow()) {
                    allows.addDifference(entryBits, denies);
                } else {
                    denies.addDifference(entryBits, allows);
                }
            }
        }
        assertEquals(privilegeMgr.getBits(read), allows);
        assertEquals(privilegeMgr.getBits(modProp), denies);
    }

    public void testEffect2() throws RepositoryException, NotExecutableException {
        JackrabbitAccessControlList pt = createEmptyTemplate(getTestPath());
        pt.addEntry(testPrincipal, privilegesFromName(Privilege.JCR_READ), true, Collections.<String, Value>emptyMap());

        // same entry but with revers 'isAllow' flag
        assertTrue(pt.addEntry(testPrincipal, privilegesFromName(Privilege.JCR_READ), false, Collections.<String, Value>emptyMap()));

        // test net-effect
        PrivilegeBits allows = PrivilegeBits.getInstance();
        PrivilegeBits denies = PrivilegeBits.getInstance();
        AccessControlEntry[] entries = pt.getAccessControlEntries();
        for (AccessControlEntry ace : entries) {
            if (testPrincipal.equals(ace.getPrincipal()) && ace instanceof JackrabbitAccessControlEntry) {
                PrivilegeBits entryBits = privilegeMgr.getBits(ace.getPrivileges());
                if (((JackrabbitAccessControlEntry) ace).isAllow()) {
                    allows.addDifference(entryBits, denies);
                } else {
                    denies.addDifference(entryBits, allows);
                }
            }
        }

        assertTrue(allows.isEmpty());
        assertEquals(privilegeMgr.getBits(privilegesFromName(Privilege.JCR_READ)), denies);
    }

    public void testRemoveEntry() throws RepositoryException,
            NotExecutableException {
        JackrabbitAccessControlList pt = createEmptyTemplate(getTestPath());
        pt.addAccessControlEntry(testPrincipal, privilegesFromName(Privilege.JCR_READ));
        pt.removeAccessControlEntry(pt.getAccessControlEntries()[0]);
    }

    public void testRemoveNonExisting() throws RepositoryException {
        JackrabbitAccessControlList pt = createEmptyTemplate(getTestPath());
        try {
            pt.removeAccessControlEntry(new AccessControlEntry() {
                public Principal getPrincipal() {
                    return testPrincipal;
                }
                public Privilege[] getPrivileges() {
                    return new Privilege[0];
                }
            });
            fail("Attemt to remove a non-existing, custom ACE must throw AccessControlException.");
        } catch (AccessControlException e) {
            // success
        }
    }

    public void testReorder() throws Exception {
        Privilege[] read = privilegesFromName(Privilege.JCR_READ);
        Privilege[] write = privilegesFromName(Privilege.JCR_WRITE);

        Principal p2 = getSecondPrincipal();

        AbstractACLTemplate acl = (AbstractACLTemplate) createEmptyTemplate(getTestPath());
        acl.addAccessControlEntry(testPrincipal, read);
        acl.addEntry(testPrincipal, write, false);
        acl.addAccessControlEntry(p2, write);

        AccessControlEntry[] entries = acl.getAccessControlEntries();

        assertEquals(3, entries.length);
        AccessControlEntry aReadTP = entries[0];
        AccessControlEntry dWriteTP = entries[1];
        AccessControlEntry aWriteP2 = entries[2];

        // reorder aWriteP2 to the first position
        acl.orderBefore(aWriteP2, aReadTP);
        assertEquals(0, acl.getEntries().indexOf(aWriteP2));
        assertEquals(1, acl.getEntries().indexOf(aReadTP));
        assertEquals(2, acl.getEntries().indexOf(dWriteTP));

        // reorder aReadTP to the end of the list
        acl.orderBefore(aReadTP, null);
        assertEquals(0, acl.getEntries().indexOf(aWriteP2));
        assertEquals(1, acl.getEntries().indexOf(dWriteTP));
        assertEquals(2, acl.getEntries().indexOf(aReadTP));
    }

    public void testReorderInvalidElements() throws Exception {
        Privilege[] read = privilegesFromName(Privilege.JCR_READ);
        Privilege[] write = privilegesFromName(Privilege.JCR_WRITE);

        Principal p2 = getSecondPrincipal();

        AbstractACLTemplate acl = (AbstractACLTemplate) createEmptyTemplate(getTestPath());
        acl.addAccessControlEntry(testPrincipal, read);
        acl.addAccessControlEntry(p2, write);

        AbstractACLTemplate acl2 = (AbstractACLTemplate) createEmptyTemplate(getTestPath());
        acl2.addEntry(testPrincipal, write, false);

        AccessControlEntry invalid = acl2.getEntries().get(0);
        try {
            acl.orderBefore(invalid, acl.getEntries().get(0));
            fail("src entry not contained in list -> reorder should fail.");
        } catch (AccessControlException e) {
            // success
        }
        try {
            acl.orderBefore(acl.getEntries().get(0), invalid);
            fail("dest entry not contained in list -> reorder should fail.");
        } catch (AccessControlException e) {
            // success
        }
    }
}
