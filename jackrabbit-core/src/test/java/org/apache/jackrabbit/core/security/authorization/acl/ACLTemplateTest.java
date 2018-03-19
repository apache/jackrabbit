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
package org.apache.jackrabbit.core.security.authorization.acl;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.api.security.principal.GroupPrincipal;
import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.authorization.AbstractACLTemplateTest;
import org.apache.jackrabbit.core.security.authorization.PrivilegeBits;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.core.security.principal.PrincipalImpl;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.Privilege;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

/**
 * <code>ACLTemplateTest</code>...
 */
public class ACLTemplateTest extends AbstractACLTemplateTest {

    private Map<String, Value> emptyRestrictions = Collections.<String, Value>emptyMap();

    @Override
    protected String getTestPath() {
        return "/ab/c/d";
    }

    @Override
    protected JackrabbitAccessControlList createEmptyTemplate(String path) throws RepositoryException {
        SessionImpl sImpl = (SessionImpl) superuser;
        return new ACLTemplate(path, principalMgr, privilegeMgr, sImpl.getValueFactory(), sImpl, false);
    }

    @Override
    protected Principal getSecondPrincipal() throws Exception {
        return principalMgr.getEveryone();
    }

    public void testMultipleEntryEffect() throws RepositoryException, NotExecutableException {
        JackrabbitAccessControlList pt = createEmptyTemplate(getTestPath());
        Privilege[] privileges = privilegesFromName(Privilege.JCR_READ);
        pt.addEntry(testPrincipal, privileges, true, emptyRestrictions);

        // new entry extends privileges.
        privileges = privilegesFromNames(new String[] {
                Privilege.JCR_READ,
                Privilege.JCR_ADD_CHILD_NODES});
        assertTrue(pt.addEntry(testPrincipal,
                privileges,
                true, emptyRestrictions));

        // net-effect: only a single allow-entry with both privileges
        assertTrue(pt.size() == 1);
        assertSamePrivileges(privileges, pt.getAccessControlEntries()[0].getPrivileges());

        // adding just ADD_CHILD_NODES -> must not remove READ privilege
        Privilege[] achPrivs = privilegesFromName(Privilege.JCR_ADD_CHILD_NODES);
        assertFalse(pt.addEntry(testPrincipal, achPrivs, true, emptyRestrictions));
        // net-effect: only a single allow-entry with add_child_nodes + read privilege
        assertTrue(pt.size() == 1);
        assertSamePrivileges(privileges, pt.getAccessControlEntries()[0].getPrivileges());

        // revoke the 'READ' privilege
        privileges = privilegesFromName(Privilege.JCR_READ);
        assertTrue(pt.addEntry(testPrincipal, privileges, false, emptyRestrictions));
        // net-effect: 2 entries one allowing ADD_CHILD_NODES, the other denying READ
        assertTrue(pt.size() == 2);
        assertSamePrivileges(privilegesFromName(Privilege.JCR_ADD_CHILD_NODES),
                pt.getAccessControlEntries()[0].getPrivileges());
        assertSamePrivileges(privilegesFromName(Privilege.JCR_READ),
                pt.getAccessControlEntries()[1].getPrivileges());

        // remove the deny-READ entry
        pt.removeAccessControlEntry(pt.getAccessControlEntries()[1]);
        assertTrue(pt.size() == 1);
        assertSamePrivileges(privilegesFromName(Privilege.JCR_ADD_CHILD_NODES),
                pt.getAccessControlEntries()[0].getPrivileges());

        // remove the allow-ADD_CHILD_NODES entry
        pt.removeAccessControlEntry(pt.getAccessControlEntries()[0]);
        assertTrue(pt.isEmpty());
    }

    public void testMultipleEntryEffect2() throws RepositoryException, NotExecutableException {
        Privilege repwrite = getAccessControlManager(superuser).privilegeFromName(PrivilegeRegistry.REP_WRITE);

        JackrabbitAccessControlList pt = createEmptyTemplate(getTestPath());
        pt.addAccessControlEntry(testPrincipal, new Privilege[] {repwrite});

        // add deny entry for mod_props
        Privilege modProperties = getAccessControlManager(superuser).privilegeFromName(Privilege.JCR_MODIFY_PROPERTIES);
        assertTrue(pt.addEntry(testPrincipal, new Privilege[] {modProperties}, false, null));

        // net-effect: 2 entries with the allow entry being adjusted
        assertTrue(pt.size() == 2);
        AccessControlEntry[] entries = pt.getAccessControlEntries();
        for (AccessControlEntry entry1 : entries) {
            ACLTemplate.Entry entry = (ACLTemplate.Entry) entry1;
            PrivilegeBits privs = entry.getPrivilegeBits();
            if (entry.isAllow()) {
                Privilege[] result = privilegesFromNames(new String[] {
                        Privilege.JCR_ADD_CHILD_NODES,
                        Privilege.JCR_NODE_TYPE_MANAGEMENT,
                        Privilege.JCR_REMOVE_CHILD_NODES,
                        Privilege.JCR_REMOVE_NODE});    
                PrivilegeBits bits = privilegeMgr.getBits(result);
                assertEquals(privs, bits);
            } else {
                assertEquals(privs, privilegeMgr.getBits(modProperties));
            }
        }
    }

    public void testMultiplePrincipals() throws RepositoryException, NotExecutableException {
        PrincipalManager pMgr = ((JackrabbitSession) superuser).getPrincipalManager();
        Principal everyone = pMgr.getEveryone();
        Principal grPrincipal = null;
        PrincipalIterator it = pMgr.findPrincipals("", PrincipalManager.SEARCH_TYPE_GROUP);
        while (it.hasNext()) {
            GroupPrincipal gr = (GroupPrincipal) it.nextPrincipal();
            if (!everyone.equals(gr)) {
                grPrincipal = gr;
            }
        }
        if (grPrincipal == null || grPrincipal.equals(everyone)) {
            throw new NotExecutableException();
        }
        Privilege[] privs = privilegesFromName(Privilege.JCR_READ);

        JackrabbitAccessControlList pt = createEmptyTemplate(getTestPath());
        pt.addAccessControlEntry(testPrincipal, privs);
        assertFalse(pt.addAccessControlEntry(testPrincipal, privs));

        // add same privileges for another principal -> must modify as well.
        assertTrue(pt.addAccessControlEntry(everyone, privs));
        // .. 2 entries must be present.
        assertTrue(pt.getAccessControlEntries().length == 2);
    }

    public void testSetEntryForGroupPrincipal() throws RepositoryException, NotExecutableException {
        JackrabbitAccessControlList pt = createEmptyTemplate(getTestPath());
        Privilege[] privs = privilegesFromName(Privilege.JCR_READ);
        GroupPrincipal grPrincipal = (GroupPrincipal) principalMgr.getEveryone();

        // adding allow-entry must succeed
        assertTrue(pt.addAccessControlEntry(grPrincipal, privs));

        // adding deny-entry must succeed
        pt.addEntry(grPrincipal, privs, false, null);
    }

    public void testRevokeEffect() throws RepositoryException, NotExecutableException {
        JackrabbitAccessControlList pt = createEmptyTemplate(getTestPath());
        Privilege[] privileges = privilegesFromName(Privilege.JCR_READ);

        pt.addEntry(testPrincipal, privileges, true, emptyRestrictions);

        // same entry but with revers 'isAllow' flag
        assertTrue(pt.addEntry(testPrincipal, privileges, false, emptyRestrictions));

        // net-effect: only a single deny-read entry
        assertEquals(1, pt.size());
        assertSamePrivileges(privileges, pt.getAccessControlEntries()[0].getPrivileges());
    }

    public void testUpdateEntry() throws RepositoryException, NotExecutableException {
        JackrabbitAccessControlList pt = createEmptyTemplate(getTestPath());

        Privilege[] readPriv = privilegesFromName(Privilege.JCR_READ);
        Privilege[] writePriv = privilegesFromName(Privilege.JCR_WRITE);

        Principal principal2 = principalMgr.getEveryone();

        pt.addEntry(testPrincipal, readPriv, true, emptyRestrictions);
        pt.addEntry(principal2, readPriv, true, emptyRestrictions);
        pt.addEntry(testPrincipal, writePriv, false, emptyRestrictions);

        // adding an entry that should update the existing allow-entry for everyone.
        pt.addEntry(principal2, writePriv, true, emptyRestrictions);

        AccessControlEntry[] entries = pt.getAccessControlEntries();
        assertEquals(3, entries.length);
        JackrabbitAccessControlEntry princ2AllowEntry = (JackrabbitAccessControlEntry) entries[1];
        assertEquals(principal2, princ2AllowEntry.getPrincipal());
        assertTrue(princ2AllowEntry.isAllow());
        assertSamePrivileges(new Privilege[] {readPriv[0], writePriv[0]}, princ2AllowEntry.getPrivileges());
    }

    public void testUpdateComplementaryEntry() throws RepositoryException, NotExecutableException {
        JackrabbitAccessControlList pt = createEmptyTemplate(getTestPath());

        Privilege[] readPriv = privilegesFromName(Privilege.JCR_READ);
        Privilege[] writePriv = privilegesFromName(Privilege.JCR_WRITE);
        Principal principal2 = principalMgr.getEveryone();

        pt.addEntry(testPrincipal, readPriv, true, emptyRestrictions);
        pt.addEntry(principal2, readPriv, true, emptyRestrictions);
        pt.addEntry(testPrincipal, writePriv, false, emptyRestrictions);
        pt.addEntry(principal2, writePriv, true, emptyRestrictions);
        // entry complementary to the first entry
        // -> must remove the allow-READ entry and update the deny-WRITE entry.
        pt.addEntry(testPrincipal, readPriv, false, emptyRestrictions);

        AccessControlEntry[] entries = pt.getAccessControlEntries();

        assertEquals(2, entries.length);

        JackrabbitAccessControlEntry first = (JackrabbitAccessControlEntry) entries[0];
        assertEquals(principal2, first.getPrincipal());

        JackrabbitAccessControlEntry second = (JackrabbitAccessControlEntry) entries[1];
        assertEquals(testPrincipal, second.getPrincipal());
        assertFalse(second.isAllow());
        assertSamePrivileges(new Privilege[] {readPriv[0], writePriv[0]}, second.getPrivileges());
    }

    public void testTwoEntriesPerPrincipal() throws RepositoryException, NotExecutableException {
        JackrabbitAccessControlList pt = createEmptyTemplate(getTestPath());

        Privilege[] readPriv = privilegesFromName(Privilege.JCR_READ);
        Privilege[] writePriv = privilegesFromName(Privilege.JCR_WRITE);
        Privilege[] acReadPriv = privilegesFromName(Privilege.JCR_READ_ACCESS_CONTROL);

        pt.addEntry(testPrincipal, readPriv, true, emptyRestrictions);
        pt.addEntry(testPrincipal, writePriv, true, emptyRestrictions);
        pt.addEntry(testPrincipal, acReadPriv, true, emptyRestrictions);

        pt.addEntry(testPrincipal, readPriv, false, emptyRestrictions);
        pt.addEntry(new PrincipalImpl(testPrincipal.getName()), readPriv, false, emptyRestrictions);
        pt.addEntry(new Principal() {
            public String getName() {
                return testPrincipal.getName();
            }
        }, readPriv, false, emptyRestrictions);

        AccessControlEntry[] entries = pt.getAccessControlEntries();
        assertEquals(2, entries.length);
    }

    /**
     * Test if new entries get appended at the end of the list.
     *
     * @throws RepositoryException
     * @throws NotExecutableException
     */
    public void testNewEntriesAppendedAtEnd() throws RepositoryException, NotExecutableException {
        JackrabbitAccessControlList pt = createEmptyTemplate(getTestPath());

        Privilege[] readPriv = privilegesFromName(Privilege.JCR_READ);
        Privilege[] writePriv = privilegesFromName(Privilege.JCR_WRITE);

        pt.addEntry(testPrincipal, readPriv, true, emptyRestrictions);
        pt.addEntry(principalMgr.getEveryone(), readPriv, true, emptyRestrictions);
        pt.addEntry(testPrincipal, writePriv, false, emptyRestrictions);

        AccessControlEntry[] entries = pt.getAccessControlEntries();

        assertEquals(3, entries.length);

        JackrabbitAccessControlEntry last = (JackrabbitAccessControlEntry) entries[2];
        assertEquals(testPrincipal, last.getPrincipal());
        assertEquals(false, last.isAllow());
        assertEquals(writePriv[0], last.getPrivileges()[0]);
    }

    public void testRestrictions() throws RepositoryException, NotExecutableException {
        JackrabbitAccessControlList pt = createEmptyTemplate(getTestPath());

        String restrName = ((SessionImpl) superuser).getJCRName(ACLTemplate.P_GLOB);
        
        String[] names = pt.getRestrictionNames();
        assertNotNull(names);
        assertEquals(1, names.length);
        assertEquals(restrName, names[0]);
        assertEquals(PropertyType.STRING, pt.getRestrictionType(names[0]));

        Privilege[] writePriv = privilegesFromName(Privilege.JCR_WRITE);

        // add entry without restr. -> must succeed
        assertTrue(pt.addAccessControlEntry(testPrincipal, writePriv));
        assertEquals(1, pt.getAccessControlEntries().length);

        // ... again -> no modification.
        assertFalse(pt.addAccessControlEntry(testPrincipal, writePriv));
        assertEquals(1, pt.getAccessControlEntries().length);

        // ... again using different method -> no modification.
        assertFalse(pt.addEntry(testPrincipal, writePriv, true));
        assertEquals(1, pt.getAccessControlEntries().length);

        // ... complementary entry -> must modify the acl
        assertTrue(pt.addEntry(testPrincipal, writePriv, false));
        assertEquals(1, pt.getAccessControlEntries().length);

        // add an entry with a restrictions:
        Map<String,Value> restrictions = Collections.singletonMap(restrName, superuser.getValueFactory().createValue("/.*"));
        assertTrue(pt.addEntry(testPrincipal, writePriv, false, restrictions));
        assertEquals(2, pt.getAccessControlEntries().length);

        // ... same again -> no modification.
        assertFalse(pt.addEntry(testPrincipal, writePriv, false, restrictions));
        assertEquals(2, pt.getAccessControlEntries().length);

        // ... complementary entry -> must modify the acl.
        assertTrue(pt.addEntry(testPrincipal, writePriv, true, restrictions));
        assertEquals(2, pt.getAccessControlEntries().length);        
    }

    public void testInsertionOrder() throws Exception {
        JackrabbitAccessControlList pt = createEmptyTemplate(getTestPath());

        Privilege[] readPriv = privilegesFromName(Privilege.JCR_READ);
        Privilege[] writePriv = privilegesFromName(Privilege.JCR_WRITE);
        Privilege[] addNodePriv = privilegesFromName(Privilege.JCR_ADD_CHILD_NODES);

        String restrName = ((SessionImpl) superuser).getJCRName(ACLTemplate.P_GLOB);
        Map<String,Value> restrictions = Collections.singletonMap(restrName, superuser.getValueFactory().createValue("/.*"));

        pt.addEntry(testPrincipal, readPriv, true, emptyRestrictions);
        pt.addEntry(testPrincipal, writePriv, false, emptyRestrictions);
        pt.addEntry(testPrincipal, addNodePriv, true, restrictions);

        AccessControlEntry[] entries = pt.getAccessControlEntries();
        assertTrue(Arrays.equals(readPriv, entries[0].getPrivileges()));
        assertTrue(Arrays.equals(writePriv, entries[1].getPrivileges()));
        assertTrue(Arrays.equals(addNodePriv, entries[2].getPrivileges()));
    }

    public void testInsertionOrder2() throws Exception {
        JackrabbitAccessControlList pt = createEmptyTemplate(getTestPath());

        Privilege[] readPriv = privilegesFromName(Privilege.JCR_READ);
        Privilege[] writePriv = privilegesFromName(Privilege.JCR_WRITE);
        Privilege[] addNodePriv = privilegesFromName(Privilege.JCR_ADD_CHILD_NODES);

        String restrName = ((SessionImpl) superuser).getJCRName(ACLTemplate.P_GLOB);
        Map<String,Value> restrictions = Collections.singletonMap(restrName, superuser.getValueFactory().createValue("/.*"));

        pt.addEntry(testPrincipal, readPriv, true, emptyRestrictions);
        pt.addEntry(testPrincipal, addNodePriv, true, restrictions);
        pt.addEntry(testPrincipal, writePriv, false, emptyRestrictions);

        AccessControlEntry[] entries = pt.getAccessControlEntries();
        assertTrue(Arrays.equals(readPriv, entries[0].getPrivileges()));
        assertTrue(Arrays.equals(addNodePriv, entries[1].getPrivileges()));
        assertTrue(Arrays.equals(writePriv, entries[2].getPrivileges()));
    }

}