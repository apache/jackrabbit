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
package org.apache.jackrabbit.test.api.security;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>AccessControlEntryTest</code>...
 */
public class AccessControlListTest extends AbstractAccessControlTest {

    private static Logger log = LoggerFactory.getLogger(AccessControlListTest.class);

    private String path;
    private Privilege[] privs;
    private Principal testPrincipal;

    private List<Privilege> privilegesToRestore = new ArrayList<Privilege>();

    protected void setUp() throws Exception {
        checkSupportedOption(Repository.OPTION_ACCESS_CONTROL_SUPPORTED);

        super.setUp();

        // TODO: make sure, entries to ADD are not present yet.
        try {
            // TODO: retrieve targetPath from configuration
            Node n = testRootNode.addNode(nodeName1, testNodeType);
            superuser.save();
            path = n.getPath();

            privs = acMgr.getSupportedPrivileges(path);

            if (privs.length == 0) {
                throw new NotExecutableException("No supported privileges at absPath " + path);
            }

            testPrincipal = getHelper().getKnownPrincipal(superuser);
            
            // remember existing entries for test-principal -> later restore.
            privilegesToRestore = currentPrivileges(getList(acMgr, path), testPrincipal);
        } catch (Exception e) {
            superuser.logout();
            throw e;
        }
    }

    protected void tearDown() throws Exception {
        try {
            // restore original entries (remove others).
            AccessControlList list = getList(acMgr, path);
            AccessControlEntry[] entries = list.getAccessControlEntries();
            for (int i = 0; i < entries.length; i++) {
                AccessControlEntry ace = entries[i];
                if (testPrincipal.equals(ace.getPrincipal())) {
                    list.removeAccessControlEntry(ace);
                }
            }
            if (!privilegesToRestore.isEmpty()) {
                list.addAccessControlEntry(testPrincipal, (Privilege[]) privilegesToRestore.toArray(new Privilege[privilegesToRestore.size()]));
            }
            if (list.getAccessControlEntries().length > 0 && acMgr.getPolicies(path).length > 0) {
                acMgr.setPolicy(path, list);
                superuser.save();                
            }
        } catch (Exception e) {
            log.warn("Unexpected error while removing test entries.", e);
        }
        super.tearDown();
    }

    private static AccessControlList getList(AccessControlManager acMgr, String path)
            throws NotExecutableException, AccessDeniedException, RepositoryException {
        for (AccessControlPolicyIterator it = acMgr.getApplicablePolicies(path); it.hasNext();) {
            AccessControlPolicy acp = it.nextAccessControlPolicy();
            if (acp instanceof AccessControlList) {
                return (AccessControlList) acp;
            }
        }
        AccessControlPolicy[] acps = acMgr.getPolicies(path);
        for (int i = 0; i < acps.length; i++) {
            if (acps[i] instanceof AccessControlList) {
                return (AccessControlList) acps[i] ;
            }
        }
        throw new NotExecutableException("No AccessControlList at " + path);
    }

    private static List<Privilege> currentPrivileges(AccessControlList acl, Principal principal) throws RepositoryException {
        List<Privilege> privileges = new ArrayList<Privilege>();
        AccessControlEntry[] entries = acl.getAccessControlEntries();
        for (int i = 0; i < entries.length; i++) {
            AccessControlEntry ace = entries[i];
            if (principal.equals(ace.getPrincipal())) {
                privileges.addAll(Arrays.asList(ace.getPrivileges()));
            }
        }
        return privileges;
    }

    public void testGetAccessControlEntries() throws RepositoryException, AccessDeniedException, NotExecutableException {
        checkCanReadAc(path);
        AccessControlList acl = getList(acMgr, path);

        // call must succeed.
        AccessControlEntry[] entries = acl.getAccessControlEntries();
        assertNotNull("AccessControlList#getAccessControlEntries must not return null.", entries);
        for (int i = 0; i < entries.length; i++) {
            assertNotNull("An ACE must contain a principal", entries[i].getPrincipal());
            Privilege[] privs = entries[i].getPrivileges();
            assertTrue("An ACE must contain at least a single privilege", privs != null && privs.length > 0);
        }
    }

    public void testAddAccessControlEntry() throws NotExecutableException, RepositoryException {
        checkCanModifyAc(path);

        Privilege[] privileges = new Privilege[] {privs[0]};
        AccessControlList acl = getList(acMgr, path);

        AccessControlEntry entry = null;
        if (acl.addAccessControlEntry(testPrincipal, privileges)) {
            AccessControlEntry[] aces = acl.getAccessControlEntries();
            for (int i = 0; i < aces.length; i++) {
                if (aces[i].getPrincipal().equals(testPrincipal) &&
                    Arrays.asList(privileges).equals(Arrays.asList(aces[i].getPrivileges()))) {
                    entry = aces[i];
                }
            }
            if (entry == null) throw new NotExecutableException();
        } else {
            throw new NotExecutableException();

        }
        assertEquals("Principal name of the ACE must be equal to the name of the passed Principal", testPrincipal.getName(), entry.getPrincipal().getName());
        assertEquals("Privileges of the ACE must be equal to the passed ones", Arrays.asList(privileges), Arrays.asList(entry.getPrivileges()));
    }

    public void testAddAggregatePrivilege() throws NotExecutableException, RepositoryException {
        checkCanModifyAc(path);

        Privilege aggregate = null;
        for (int i = 0; i < privs.length; i++) {
            if (privs[i].isAggregate()) {
                aggregate = privs[i];
                break;
            }
        }
        if (aggregate == null) {
            throw new NotExecutableException("No aggregate privilege supported at " + path);
        }

        AccessControlList acl = getList(acMgr, path);
        acl.addAccessControlEntry(testPrincipal, new Privilege[] {aggregate});

        // make sure all privileges are present now
        List<Privilege> privs = currentPrivileges(acl, testPrincipal);
        assertTrue("Privileges added through 'addAccessControlEntry' must be " +
                "reflected upon getAccessControlEntries",
                privs.contains(aggregate) || privs.containsAll(Arrays.asList(aggregate.getAggregatePrivileges())));
    }

    public void testAddAggregatedPrivilegesSeparately() throws NotExecutableException, RepositoryException {
        checkCanModifyAc(path);

        Privilege aggregate = null;
        for (int i = 0; i < privs.length; i++) {
            if (privs[i].isAggregate()) {
                aggregate = privs[i];
                break;
            }
        }
        if (aggregate == null) {
            throw new NotExecutableException("No aggregate privilege supported at " + path);
        }

        AccessControlList acl = getList(acMgr, path);
        acl.addAccessControlEntry(testPrincipal, new Privilege[] {aggregate});

        Privilege[] privs = aggregate.getAggregatePrivileges();
        for (int i = 0; i < privs.length; i++) {
            boolean modified = acl.addAccessControlEntry(testPrincipal, new Privilege[] {privs[i]});
            assertFalse("Adding the aggregated privs individually later on must not modify the policy", modified);
        }
    }

    public void testAddAbstractPrivilege() throws NotExecutableException, RepositoryException {
        checkCanModifyAc(path);

        Privilege abstractPriv = null;
        Privilege[] allPrivs = acMgr.privilegeFromName(Privilege.JCR_ALL).getAggregatePrivileges();
        for (int i = 0; i < allPrivs.length; i++) {
            if (allPrivs[i].isAbstract()) {
                abstractPriv = allPrivs[i];
                break;
            }
        }
        if (abstractPriv == null) {
            throw new NotExecutableException("No abstract privilege found.");
        }

        AccessControlList acl = getList(acMgr, path);
        try {
            acl.addAccessControlEntry(testPrincipal, new Privilege[] {abstractPriv});
            fail("Adding an ACE with an abstract privilege must fail.");
        } catch (AccessControlException e) {
            // success
        }
    }

    public void testAddPrivilegesPresentInEntries() throws NotExecutableException, RepositoryException {
        checkCanModifyAc(path);

        AccessControlList acl = getList(acMgr, path);
        acl.addAccessControlEntry(testPrincipal, privs);

        Set<Privilege> assignedPrivs = new HashSet<Privilege>();
        AccessControlEntry[] entries = acl.getAccessControlEntries();
        for (int i = 0; i < entries.length; i++) {
            if (entries[i].getPrincipal().equals(testPrincipal)) {
                Privilege[] prvs = entries[i].getPrivileges();
                for (int j = 0; j < prvs.length; j++) {
                    if (prvs[j].isAggregate()) {
                        assignedPrivs.addAll(Arrays.asList(prvs[j].getAggregatePrivileges()));
                    } else {
                        assignedPrivs.add(prvs[j]);
                    }
                }
            }
        }

        Set<Privilege> expected = new HashSet<Privilege>();
        for (int i = 0; i < privs.length; i++) {
            if (privs[i].isAggregate()) {
                expected.addAll(Arrays.asList(privs[i].getAggregatePrivileges()));
            } else {
                expected.add(privs[i]);
            }
        }
        assertTrue("getAccessControlEntries must contain an entry or entries that grant at least the added privileges.", assignedPrivs.containsAll(expected));
    }

    public void testAddAccessControlEntryAndSetPolicy() throws RepositoryException, NotExecutableException {
        checkCanModifyAc(path);

        AccessControlList acl = getList(acMgr, path);
        List<AccessControlEntry> originalAces = Arrays.asList(acl.getAccessControlEntries());

        if (!acl.addAccessControlEntry(testPrincipal, privs)) {
            throw new NotExecutableException();
        }

        // re-access ACL from AC-Manager -> must not yet have changed
        assertEquals("Before calling setPolicy any modifications to an ACL must not be reflected in the policies", originalAces, Arrays.asList(getList(acMgr, path).getAccessControlEntries()));

        // setting the modified policy -> policy must change.
        acMgr.setPolicy(path, acl);
        assertEquals("Before calling setPolicy any modifications to an ACL must not be reflected in the policies", Arrays.asList(acl.getAccessControlEntries()), Arrays.asList(getList(acMgr, path).getAccessControlEntries()));
    }

    public void testAddAccessControlEntryIsTransient() throws NotExecutableException, RepositoryException {
        checkCanModifyAc(path);

        AccessControlList acl = getList(acMgr, path);
        List<AccessControlEntry> originalAces = Arrays.asList(acl.getAccessControlEntries());

        if (!acl.addAccessControlEntry(testPrincipal, privs)) {
            throw new NotExecutableException();
        }
        // set the policy (see #testAddAccessControlEntryAndSetPolicy)
        acMgr.setPolicy(path, acl);

        // revert the changes made
        superuser.refresh(false);
        assertEquals("After calling Session.refresh() any changes to a nodes policies must be reverted.", originalAces, Arrays.asList(getList(acMgr, path).getAccessControlEntries()));
    }

    public void testAddAccessControlEntryInvalidPrincipal() throws NotExecutableException, RepositoryException {
        checkCanModifyAc(path);
        try {
            Principal invalidPrincipal = getHelper().getUnknownPrincipal(superuser);
            AccessControlList acl = getList(acMgr, path);
            acl.addAccessControlEntry(invalidPrincipal, privs);
            fail("Adding an entry with an unknown principal must throw AccessControlException.");
        } catch (AccessControlException e) {
            // success.
        } finally {
            superuser.refresh(false);
        }
    }

    public void testAddAccessControlEntryEmptyPrivilegeArray() throws NotExecutableException, RepositoryException {
        checkCanModifyAc(path);

        try {
            Privilege[] invalidPrivs = new Privilege[0];
            AccessControlList acl = getList(acMgr, path);
            acl.addAccessControlEntry(testPrincipal, invalidPrivs);
            fail("Adding an entry with an invalid privilege array must throw AccessControlException.");
        } catch (AccessControlException e) {
            // success.
        } finally {
            superuser.refresh(false);
        }
    }

    public void testAddAccessControlEntryInvalidPrivilege() throws NotExecutableException, RepositoryException {
        checkCanModifyAc(path);

        try {
            Privilege[] invalidPrivs = new Privilege[] {new Privilege() {
                public String getName() {
                    return null;
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
            }};
            AccessControlList acl = getList(acMgr, path);
            acl.addAccessControlEntry(testPrincipal, invalidPrivs);
            fail("Adding an entry with an invalid privilege must throw AccessControlException.");
        } catch (AccessControlException e) {
            // success.
        } finally {
            superuser.refresh(false);
        }
    }

    public void testRemoveAccessControlEntry() throws NotExecutableException, RepositoryException {
        checkCanModifyAc(path);

        AccessControlList acl = getList(acMgr, path);
        AccessControlEntry[] entries = acl.getAccessControlEntries();
        if (entries.length > 0) {
            AccessControlEntry ace = entries[0];
            acl.removeAccessControlEntry(ace);

            // retrieve entries again:
            List<AccessControlEntry> remainingEntries = Arrays.asList(acl.getAccessControlEntries());
            assertFalse("AccessControlList.getAccessControlEntries still returns a removed ACE.", remainingEntries.contains(ace));
        }
    }

    public void testRemoveAddedAccessControlEntry() throws NotExecutableException, RepositoryException {
        checkCanModifyAc(path);

        AccessControlList acl = getList(acMgr, path);
        acl.addAccessControlEntry(testPrincipal, privs);

        AccessControlEntry[] aces = acl.getAccessControlEntries();
        for (int i = 0; i < aces.length; i++) {
            acl.removeAccessControlEntry(aces[i]);
        }
        assertEquals("After removing all ACEs the ACL must be empty", 0, acl.getAccessControlEntries().length);
    }

    public void testRemoveAccessControlEntryAndSetPolicy() throws NotExecutableException, RepositoryException {
        checkCanModifyAc(path);

        // add a new ACE that can be removed later on.
        AccessControlList acl = getList(acMgr, path);
        if (!acl.addAccessControlEntry(testPrincipal, privs)) {
            throw new NotExecutableException();
        } else {
            acMgr.setPolicy(path, acl);
        }

        // try to re-access the modifiable ACL in order to remove the ACE
        // added before.
        acl = getList(acMgr, path);
        AccessControlEntry ace = null;
        AccessControlEntry[] aces = acl.getAccessControlEntries();
        if (aces.length == 0) {
            throw new NotExecutableException();
        } else {
            ace = aces[0];
            acl.removeAccessControlEntry(ace);
        }

        // before setting the policy again -> no changes visible.
        assertEquals("Removal of an ACE must only be visible upon 'setPolicy'", Arrays.asList(aces), Arrays.asList(getList(acMgr, path).getAccessControlEntries()));

        // set policy again.
        acMgr.setPolicy(path, acl);
        assertEquals("After 'setPolicy' the ACE-removal must be visible to the editing session.", Arrays.asList(acl.getAccessControlEntries()), Arrays.asList(getList(acMgr, path).getAccessControlEntries()));
    }

    public void testRemoveAccessControlEntryIsTransient() throws NotExecutableException, RepositoryException {
        checkCanModifyAc(path);

        AccessControlList acl = getList(acMgr, path);
        // make sure an ACE is present and modifications are persisted.
        if (acl.addAccessControlEntry(testPrincipal, privs)) {
            acMgr.setPolicy(path, acl);
            superuser.save();
        } else {
            throw new NotExecutableException();
        }

        // retrieve ACL again -> transient removal of the ace
        acl = getList(acMgr, path);
        AccessControlEntry ace = acl.getAccessControlEntries()[0];
        acl.removeAccessControlEntry(ace);
        acMgr.setPolicy(path, acl);

        // revert changes -> removed entry must be present again.
        superuser.refresh(false);
        List<AccessControlEntry> entries = Arrays.asList(getList(acMgr, path).getAccessControlEntries());
        assertTrue("After reverting any changes the removed ACE should be present again.", entries.contains(ace));
    }

    public void testRemoveIllegalAccessControlEntry() throws NotExecutableException, RepositoryException {
        checkCanModifyAc(path);
        try {
            AccessControlEntry entry = new AccessControlEntry() {
                public Principal getPrincipal() {
                    return testPrincipal;
                }
                public Privilege[] getPrivileges() {
                    return privs;
                }
            };
            AccessControlList acl = getList(acMgr, path);
            acl.removeAccessControlEntry(entry);
            fail("AccessControlManager.removeAccessControlEntry with an unknown entry must throw AccessControlException.");
        } catch (AccessControlException e) {
            // ok
        }
    }

    public void testAddAccessControlEntryTwice() throws NotExecutableException, RepositoryException {
        checkCanModifyAc(path);
        AccessControlList acl = getList(acMgr, path);
        if (acl.addAccessControlEntry(testPrincipal, privs)) {
            assertFalse("Adding the same ACE twice should not modify the AC-List.",
                    acl.addAccessControlEntry(testPrincipal, privs));
        }
    }

    public void testAddAccessControlEntryAgain() throws NotExecutableException, RepositoryException {
        checkCanModifyAc(path);

        AccessControlList list = getList(acMgr, path);
        list.addAccessControlEntry(testPrincipal, privs);
        AccessControlEntry[] entries = list.getAccessControlEntries();
        if (entries.length > 0) {
            assertFalse("Adding an existing entry again must not modify the AC-List",
                    list.addAccessControlEntry(entries[0].getPrincipal(), entries[0].getPrivileges()));
        } else {
            throw new NotExecutableException();
        }
    }

    public void testExtendPrivileges() throws NotExecutableException, RepositoryException {
        checkCanModifyAc(path);
        // search 2 non-aggregated privileges
        List<Privilege> twoPrivs = new ArrayList<Privilege>(2);
        for (int i = 0; i < privs.length && twoPrivs.size() < 2; i++) {
            if (!privs[i].isAggregate()) {
                twoPrivs.add(privs[i]);
            }
        }
        if (twoPrivs.size() < 2) {
            throw new NotExecutableException("At least 2 supported, non-aggregate privileges required at " + path);
        }

        AccessControlList acl = getList(acMgr, path);
        Privilege privilege = twoPrivs.get(0);
        // add first privilege:
        acl.addAccessControlEntry(testPrincipal, new Privilege[] {privilege});

        // add a second privilege (but not specifying the privilege added before)
        // -> the first privilege must not be removed.
        Privilege privilege2 = twoPrivs.get(1);
        acl.addAccessControlEntry(testPrincipal, new Privilege[] {privilege2});

        List<Privilege> currentPrivileges = currentPrivileges(acl, testPrincipal);
        assertTrue("'AccessControlList.addAccessControlEntry' must not remove privileges added before", currentPrivileges.containsAll(twoPrivs));
    }
}
