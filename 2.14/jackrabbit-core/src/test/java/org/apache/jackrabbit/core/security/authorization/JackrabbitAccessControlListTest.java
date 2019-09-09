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

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.test.api.security.AbstractAccessControlTest;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <code>JackrabbitAccessControlListTest</code>...
 */
public class JackrabbitAccessControlListTest extends AbstractAccessControlTest {

    private JackrabbitAccessControlList templ;
    private PrivilegeManagerImpl privilegeMgr;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Node n = testRootNode.addNode(nodeName1, testNodeType);
        superuser.save();

        AccessControlPolicyIterator it = acMgr.getApplicablePolicies(n.getPath());
        while (it.hasNext() && templ == null) {
            AccessControlPolicy p = it.nextAccessControlPolicy();
            if (p instanceof JackrabbitAccessControlList) {
                templ = (JackrabbitAccessControlList) p;
            }
        }
        if (templ == null) {
            superuser.logout();
            throw new NotExecutableException("No JackrabbitAccessControlList to test.");
        }

        privilegeMgr = (PrivilegeManagerImpl) ((JackrabbitWorkspace) superuser.getWorkspace()).getPrivilegeManager();
    }

    @Override
    protected void tearDown() throws Exception {
        // make sure transient ac-changes are reverted.
        superuser.refresh(false);
        super.tearDown();
    }

    private Principal getValidPrincipal() throws NotExecutableException, RepositoryException {
        if (!(superuser instanceof JackrabbitSession)) {
            throw new NotExecutableException();
        }

        PrincipalManager pMgr = ((JackrabbitSession) superuser).getPrincipalManager();
        PrincipalIterator it = pMgr.getPrincipals(PrincipalManager.SEARCH_TYPE_NOT_GROUP);
        if (it.hasNext()) {
            return it.nextPrincipal();
        } else {
            throw new NotExecutableException();
        }
    }

    public void testGetRestrictionNames() throws RepositoryException {
        assertNotNull(templ.getRestrictionNames());
    }

    public void testGetRestrictionType() throws RepositoryException {
        String[] names = templ.getRestrictionNames();
        for (String name : names) {
            int type = templ.getRestrictionType(name);
            assertTrue(type > PropertyType.UNDEFINED);
        }
    }

    public void testIsEmpty() throws RepositoryException {
        if (templ.isEmpty()) {
            assertEquals(0, templ.getAccessControlEntries().length);
        } else {
            assertTrue(templ.getAccessControlEntries().length > 0);
        }
    }

    public void testSize() {
        if (templ.isEmpty()) {
            assertEquals(0, templ.size());
        } else {
            assertTrue(templ.size() > 0);
        }
    }

    public void testAddEntry() throws NotExecutableException, RepositoryException {
        Principal princ = getValidPrincipal();
        Privilege[] priv = privilegesFromName(Privilege.JCR_ALL);

        List<AccessControlEntry> entriesBefore = Arrays.asList(templ.getAccessControlEntries());
        if (templ.addEntry(princ, priv, true, Collections.<String, Value>emptyMap())) {
            AccessControlEntry[] entries = templ.getAccessControlEntries();
            if (entries.length == 0) {
                fail("GrantPrivileges was successful -> at least 1 entry for principal.");
            }
            PrivilegeBits allows = PrivilegeBits.getInstance();
            for (AccessControlEntry en : entries) {
                PrivilegeBits bits = privilegeMgr.getBits(en.getPrivileges());
                if (en instanceof JackrabbitAccessControlEntry && ((JackrabbitAccessControlEntry) en).isAllow()) {
                    allows.add(bits);
                }
            }
            assertEquals(privilegeMgr.getBits(priv), allows);
        } else {
            AccessControlEntry[] entries = templ.getAccessControlEntries();
            assertEquals("Grant ALL not successful -> entries must not have changed.", entriesBefore, Arrays.asList(entries));
        }
    }

    public void testAddEntry2() throws NotExecutableException, RepositoryException {
        Principal princ = getValidPrincipal();
        Privilege[] privs = privilegesFromName(PrivilegeRegistry.REP_WRITE);

        templ.addEntry(princ, privs, true, Collections.<String, Value>emptyMap());
        AccessControlEntry[] entries = templ.getAccessControlEntries();
        assertTrue("GrantPrivileges was successful -> at least 1 entry for principal.", entries.length > 0);

        PrivilegeBits allows = PrivilegeBits.getInstance();        
        for (AccessControlEntry en : entries) {
            PrivilegeBits bits = privilegeMgr.getBits(en.getPrivileges());
            if (en instanceof JackrabbitAccessControlEntry && ((JackrabbitAccessControlEntry) en).isAllow()) {
                allows.add(bits);
            }
        }
        assertTrue("After successfully granting WRITE, the entries must reflect this", allows.includes(privilegeMgr.getBits(privs)));
    }

    public void testAllowWriteDenyRemove() throws NotExecutableException, RepositoryException {
        Principal princ = getValidPrincipal();
        Privilege[] grPriv = privilegesFromName(PrivilegeRegistry.REP_WRITE);
        Privilege[] dePriv = privilegesFromName(Privilege.JCR_REMOVE_CHILD_NODES);

        templ.addEntry(princ, grPriv, true, Collections.<String, Value>emptyMap());
        templ.addEntry(princ, dePriv, false, Collections.<String, Value>emptyMap());

        Set<Privilege> allows = new HashSet<Privilege>();
        Set<Privilege> denies = new HashSet<Privilege>();
        AccessControlEntry[] entries = templ.getAccessControlEntries();
        for (AccessControlEntry en : entries) {
            if (princ.equals(en.getPrincipal()) && en instanceof JackrabbitAccessControlEntry) {
                JackrabbitAccessControlEntry ace = (JackrabbitAccessControlEntry) en;
                Privilege[] privs = ace.getPrivileges();
                if (ace.isAllow()) {
                    allows.addAll(Arrays.asList(privs));
                } else {
                    denies.addAll(Arrays.asList(privs));
                }
            }
        }

        String[] expected = new String[] {Privilege.JCR_ADD_CHILD_NODES, Privilege.JCR_REMOVE_NODE, Privilege.JCR_MODIFY_PROPERTIES, Privilege.JCR_NODE_TYPE_MANAGEMENT};
        assertEquals(expected.length, allows.size());
        for (String name : expected) {
            assertTrue(allows.contains(acMgr.privilegeFromName(name)));
        }

        assertEquals(1, denies.size());
        assertEquals(acMgr.privilegeFromName(Privilege.JCR_REMOVE_CHILD_NODES), denies.iterator().next());
    }

    public void testRemoveEntry() throws NotExecutableException, RepositoryException {
        Principal princ = getValidPrincipal();
        Privilege[] grPriv = privilegesFromName(PrivilegeRegistry.REP_WRITE);

        templ.addEntry(princ, grPriv, true, Collections.<String, Value>emptyMap());
        AccessControlEntry[] entries = templ.getAccessControlEntries();
        int length = entries.length;
        assertTrue("Grant was both successful -> at least 1 entry.", length > 0);
        for (AccessControlEntry entry : entries) {
            templ.removeAccessControlEntry(entry);
            length = length - 1;
            assertEquals(length, templ.size());
            assertEquals(length, templ.getAccessControlEntries().length);
        }

        assertTrue(templ.isEmpty());
        assertEquals(0, templ.size());
        assertEquals(0, templ.getAccessControlEntries().length);
    }
}