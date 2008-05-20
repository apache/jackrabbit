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
package org.apache.jackrabbit.api.jsr283.security;

import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.TestPrincipal;
import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import java.security.Principal;
import java.security.acl.Group;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * <code>AccessControlEntryTest</code>...
 */
public class AccessControlEntryTest extends AbstractAccessControlTest {

    private static Logger log = LoggerFactory.getLogger(AccessControlEntryTest.class);

    private String path;
    private Map addedEntries = new HashMap();
    private Privilege[] privs;
    private Principal testPrincipal;

    protected void setUp() throws Exception {
        super.setUp();

        // TODO: test if options is supporte
        //checkSupportedOption(superuser, Repository.OPTION_ACCESS_CONTROL_ENTRY_SUPPORTED);

        Node n = testRootNode.addNode(nodeName1, testNodeType);
        superuser.save();
        path = n.getPath();

        privs = acMgr.getSupportedPrivileges(path);
        if (privs.length == 0) {
            throw new NotExecutableException("No supported privileges at absPath " + path);
        }

        // TODO: make sure, entries to ADD are not present yet.
        // TODO: retrieve principal name from tck-Configuration
        // TODO: get rid of SessionImpl dependency
        Session s = helper.getReadWriteSession();
        if (s instanceof SessionImpl) {
            for (Iterator it = ((SessionImpl) s).getSubject().getPrincipals().iterator(); it.hasNext();) {
                Principal p = (Principal) it.next();
                if (! (p instanceof Group)) {
                    testPrincipal = p;
                }
            }
            if (testPrincipal == null) {
                throw new NotExecutableException("Test principal missing.");
            }
        } else {
            throw new NotExecutableException("SessionImpl expected");
        }
    }

    protected void tearDown() throws Exception {
        try {
            for (Iterator it = addedEntries.keySet().iterator(); it.hasNext();) {
                String path = it.next().toString();
                acMgr.removeAccessControlEntry(path, (AccessControlEntry) addedEntries.get(path));
            }
            superuser.save();
        } catch (Exception e) {
            log.error("Unexpected error while removing test entries.", e);
        }
        super.tearDown();
    }

    private static AccessControlEntry addEntry(AccessControlManager acMgr,
                                               String path, Principal principal,
                                               Privilege[] privs) throws NotExecutableException, RepositoryException {
        try {
            return acMgr.addAccessControlEntry(path, principal, privs);
        } catch (UnsupportedRepositoryOperationException e) {
            throw new NotExecutableException(e.getMessage());
        }
    }

    public void testGetAccessControlEntries() throws RepositoryException, AccessDeniedException, NotExecutableException {
        checkCanReadAc(path);
        // call must succeed.
        AccessControlEntry[] entries = acMgr.getAccessControlEntries(path);
        for (int i = 0; i < entries.length; i++) {
            assertNotNull("An ACE must contain a principal", entries[i].getPrincipal());
            Privilege[] privs = entries[i].getPrivileges();
            assertTrue("An ACE must contain at least a single privilege", privs != null && privs.length > 0);
        }
    }

    public void testGetAccessControlEntriesForNonExistingNode() throws RepositoryException, AccessDeniedException, NotExecutableException {
        checkCanReadAc(path);
        String path = getPathToNonExistingNode();
        try {
            acMgr.getAccessControlEntries(path);
            fail("AccessControlManager.getAccessControlEntries for an invalid absPath must throw PathNotFoundException.");
        } catch (PathNotFoundException e) {
            // ok
        }
    }

    public void testGetAccessControlEntriesForProperty() throws RepositoryException, AccessDeniedException, NotExecutableException {
        checkCanReadAc(path);
        String path = getPathToProperty();
        try {
            acMgr.getAccessControlEntries(path);
            fail("AccessControlManager.getAccessControlEntries for a property path must throw PathNotFoundException.");
        } catch (PathNotFoundException e) {
            // ok
        }
    }

    public void testGetEffectiveAccessControlEntries() throws NotExecutableException, RepositoryException {
        checkCanReadAc(path);
        // call must succeed.
        AccessControlEntry[] entries = acMgr.getEffectiveAccessControlEntries(path);
        for (int i = 0; i < entries.length; i++) {
            assertNotNull("An ACE must contain a principal", entries[i].getPrincipal());
            Privilege[] privs = entries[i].getPrivileges();
            assertNotNull("An ACE must contain at least a single privilege", privs);
            assertTrue("An ACE must contain at least a single privilege", privs.length > 0);
        }
    }

    public void testGetEffectiveAccessControlEntriesForNonExistingNode() throws RepositoryException, AccessDeniedException, NotExecutableException {
        checkCanReadAc(path);
        String path = getPathToNonExistingNode();
        try {
            acMgr.getEffectiveAccessControlEntries(path);
            fail("AccessControlManager.getAccessControlEntries for an invalid absPath must throw PathNotFoundException.");
        } catch (PathNotFoundException e) {
            // ok
        }
    }

    public void testGetEffectiveAccessControlEntriesForProperty() throws RepositoryException, AccessDeniedException, NotExecutableException {
        checkCanReadAc(path);
        String path = getPathToProperty();
        try {
            acMgr.getEffectiveAccessControlEntries(path);
            fail("AccessControlManager.getAccessControlEntries for a property path must throw PathNotFoundException.");
        } catch (PathNotFoundException e) {
            // ok
        }
    }

    public void testAddAccessControlEntry() throws NotExecutableException, RepositoryException {
        checkCanModifyAc(path);

        Privilege[] privileges = new Privilege[] {privs[0]};

        AccessControlEntry entry = addEntry(acMgr, path, testPrincipal, privileges);
        assertEquals("Principal name of the ACE must be equal to the name of the passed Principal", testPrincipal.getName(), entry.getPrincipal().getName());
        assertEquals("Privileges of the ACE must be equal to the passed ones", Arrays.asList(privileges), Arrays.asList(entry.getPrivileges()));
    }

    public void testAddAccessControlEntryAggregatePrivilege() throws NotExecutableException, RepositoryException {
        checkCanModifyAc(path);

        Privilege[] privileges = null;
        for (int i = 0; i < privs.length; i++) {
            if (privs[i].isAggregate()) {
                privileges = new Privilege[] {privs[i]};
                break;
            }
        }
        if (privileges == null) {
            throw new NotExecutableException("No aggregate privilege supported at " + path);
        }

        AccessControlEntry entry = addEntry(acMgr, path, testPrincipal, privileges);
        assertEquals("Principal name of the ACE must be equal to the name of the passed Principal", testPrincipal.getName(), entry.getPrincipal().getName());
        assertEquals("Privileges of the ACE must be equal to the passed ones", Arrays.asList(privileges), Arrays.asList(entry.getPrivileges()));
    }

    public void testAddAccessControlEntryPresentInEntriesArray() throws NotExecutableException, RepositoryException {
        checkCanModifyAc(path);

        AccessControlEntry entry = addEntry(acMgr, path, testPrincipal, privs);
        boolean found = false;
        AccessControlEntry[] entries = acMgr.getAccessControlEntries(path);
        for (int i = 0; i < entries.length; i++) {
            if (entries[i].equals(entry)) {
                found = true;
            }
        }
        assertTrue("getAccessControlEntries must reflect an added ACE", found);
    }

    public void testAddAccessControlEntryIsTransient() throws NotExecutableException, RepositoryException {
        checkCanModifyAc(path);

        AccessControlEntry entry = addEntry(acMgr, path, testPrincipal, privs);
        superuser.refresh(false);
        try {
            acMgr.removeAccessControlEntry(path, entry);
            fail("Reverting changes made to 'path' must remove the transiently added entry. Its removal must therefore fail.");
        } catch (AccessControlException e) {
            // success.
        }
    }

    public void testAddAccessControlEntryInvalidPrincipal() throws NotExecutableException, RepositoryException {
        checkCanModifyAc(path);
        try {
            // TODO: retrieve unknown principal name from config
            Principal invalidPrincipal = new TestPrincipal("an_unknown_principal");
            addEntry(acMgr, path, invalidPrincipal, privs);
            fail("Adding an entry with an unknown principal must throw AccessControlException.");
        } catch (AccessControlException e) {
            // success.
        } catch (UnsupportedRepositoryOperationException e) {
            throw new NotExecutableException(e.getMessage());
        } finally {
            superuser.refresh(false);
        }
    }

    public void testRemoveAccessControlEntry() throws NotExecutableException, RepositoryException {
        checkCanModifyAc(path);

        AccessControlEntry entry = addEntry(acMgr, path, testPrincipal, privs);
        acMgr.removeAccessControlEntry(path, entry);

        boolean found = false;
        AccessControlEntry[] entries = acMgr.getAccessControlEntries(path);
        for (int i = 0; i < entries.length; i++) {
            if (entries[i].equals(entry)) {
                found = true;
            }
        }
        assertFalse("After transiently removing an ACE it must not appear upon getAccessControlEntries any more.", found);
    }

    public void testRemoveAccessControlEntryIsTransient() throws NotExecutableException, RepositoryException {
        checkCanModifyAc(path);

        AccessControlEntry entry = addEntry(acMgr, path, testPrincipal, privs);
        superuser.save();

        // remember for teardown.
        addedEntries.put(path, entry);

        // transient removal
        acMgr.removeAccessControlEntry(path, entry);

        // make sure 'getAccessControlEntries' does not contain the removed one
        AccessControlEntry[] entries = acMgr.getAccessControlEntries(path);
        for (int i = 0; i < entries.length; i++) {
            AccessControlEntry ace = entries[i];
            if (ace.equals(entry)) {
                fail("getAccessControlEntries still returns a removed ACE.");
            }
        }

        // revert removal -> entry must be present again.
        superuser.refresh(false);
        entries = acMgr.getAccessControlEntries(path);
        boolean found = false;
        for (int i = 0; i < entries.length; i++) {
            AccessControlEntry ace = entries[i];
            if (ace.equals(entry)) {
                found = true;
            }
        }
        assertTrue("After reverting any changes the remove ACE should be returned by getAccessControlEntries again", found);
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
            acMgr.removeAccessControlEntry(path, entry);
            fail("AccessControlManager.removeAccessControlEntry with an unknown entry must throw AccessControlException.");
        } catch (AccessControlException e) {
            // ok
        }
    }

    public void testRemoveAccessControlEntriesForProperty() throws RepositoryException, AccessDeniedException, NotExecutableException {
        checkCanReadAc(path);
        String path = getPathToProperty();
        try {
            AccessControlEntry entry = new AccessControlEntry() {
                public Principal getPrincipal() {
                    return testPrincipal;
                }
                public Privilege[] getPrivileges() {
                    return privs;
                }
            };
            acMgr.removeAccessControlEntry(path, entry);
            fail("AccessControlManager.getAccessControlEntries for a property path must throw PathNotFoundException.");
        } catch (PathNotFoundException e) {
            // ok
        }
    }

    public void testAddAccessControlEntryTwice() throws NotExecutableException, RepositoryException {
        checkCanModifyAc(path);

        AccessControlEntry entry = addEntry(acMgr, path, testPrincipal, privs);
        AccessControlEntry entry2 = addEntry(acMgr, path, testPrincipal, privs);

        assertEquals("Adding the same ACE twice must still return a valid ACE.", entry, entry2);
    }

    public void testAddAccessControlEntryAgain() throws NotExecutableException, RepositoryException {
        checkCanModifyAc(path);

        AccessControlEntry[] entries = acMgr.getAccessControlEntries(path);
        if (entries.length > 0) {
            AccessControlEntry ace = addEntry(acMgr, path, entries[0].getPrincipal(), entries[0].getPrivileges());
            assertEquals("Adding an existing entry again -> Principal must be equal.", entries[0].getPrincipal(), ace.getPrincipal());
            assertEquals("Adding an existing entry again -> Privileges must the same.", Arrays.asList(entries[0].getPrivileges()), Arrays.asList(ace.getPrivileges()));
        } else {
            throw new NotExecutableException();
        }
    }

    public void testExtendPrivileges() throws NotExecutableException, RepositoryException {
        checkCanModifyAc(path);
        if (privs.length == 1) {
            throw new NotExecutableException();
        }

        Privilege privilege = privs[0];
        // add a single privilege first:
        AccessControlEntry ace = addEntry(acMgr, path, testPrincipal, new Privilege[] {privilege});
        assertTrue("The resulting entry must contain the privilege added.",
                Arrays.asList(ace.getPrivileges()).contains(privilege));

        // add a second privilege (but not specifying the privilege added before)
        // -> the first privilege must not be removed.
        Privilege privilege2 = privs[1];
        AccessControlEntry ace2 = addEntry(acMgr, path, testPrincipal, new Privilege[] {privilege2});
        List pvlgs = Arrays.asList(ace2.getPrivileges());
        assertTrue("Calling 'addAccessControlEntry' must not removed privileges added before",
                pvlgs.contains(privilege) && pvlgs.contains(privilege2));
    }
}