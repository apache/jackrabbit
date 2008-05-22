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

import org.apache.jackrabbit.api.jsr283.security.AccessControlException;
import org.apache.jackrabbit.core.security.authorization.AbstractPolicyTemplateTest;
import org.apache.jackrabbit.core.security.authorization.PolicyEntry;
import org.apache.jackrabbit.core.security.authorization.PolicyTemplate;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.security.Principal;

/**
 * <code>ACLTemplateTest</code>...
 */
public class ACLTemplateTest extends AbstractPolicyTemplateTest {

    private static Logger log = LoggerFactory.getLogger(ACLTemplateTest.class);

    protected String getTestPath() {
        return "/ab/c/d";
    }

    protected PolicyTemplate createEmptyTemplate(String path) {
        return new ACLTemplate(path);
    }

    public void testAddEntry() throws RepositoryException {
        PolicyTemplate pt = createEmptyTemplate(getTestPath());
        assertTrue(pt.setEntry(new ACEImpl(testPrincipal, PrivilegeRegistry.READ, true)));
    }

    public void testAddEntryTwice() throws RepositoryException {
        PolicyTemplate pt = createEmptyTemplate(getTestPath());
        PolicyEntry pe = new ACEImpl(testPrincipal, PrivilegeRegistry.READ, true);

        pt.setEntry(pe);
        assertFalse(pt.setEntry(pe));
    }

    public void testRevokeEffect() throws RepositoryException {
        PolicyTemplate pt = createEmptyTemplate(getTestPath());
        PolicyEntry pe = new ACEImpl(testPrincipal, PrivilegeRegistry.READ, true);

        pt.setEntry(pe);

        // same entry but with revers 'isAllow' flag
        pe = new ACEImpl(testPrincipal, PrivilegeRegistry.READ, false);
        assertTrue(pt.setEntry(pe));

        // net-effect: only a single deny-read entry
        assertTrue(pt.size() == 1);
        assertEquals(pt.getEntries()[0], pe);
    }

    public void testEffect() throws RepositoryException {
        PolicyTemplate pt = createEmptyTemplate(getTestPath());
        PolicyEntry pe = new ACEImpl(testPrincipal, PrivilegeRegistry.READ, true);

        pt.setEntry(pe);

        // new entry extends privs.
        pe = new ACEImpl(testPrincipal, PrivilegeRegistry.READ | PrivilegeRegistry.ADD_CHILD_NODES, true);
        assertTrue(pt.setEntry(pe));

        // net-effect: only a single allow-entry with both privileges
        assertTrue(pt.size() == 1);
        assertEquals(pt.getEntries()[0], pe);

        // new entry revokes READ priv
        pe = new ACEImpl(testPrincipal, PrivilegeRegistry.ADD_CHILD_NODES, true);
        assertTrue(pt.setEntry(pe));
        // net-effect: only a single allow-entry with add_child_nodes priv
        assertTrue(pt.size() == 1);
        assertEquals(pt.getEntries()[0], pe);
    }

    public void testEffect2() throws RepositoryException {
        PolicyTemplate pt = createEmptyTemplate(getTestPath());
        PolicyEntry pe = new ACEImpl(testPrincipal, PrivilegeRegistry.READ, true);
        pt.setEntry(pe);

        // add deny entry for mod_props
        PolicyEntry pe2 = new ACEImpl(testPrincipal, PrivilegeRegistry.MODIFY_PROPERTIES, false);
        assertTrue(pt.setEntry(pe2));

        // net-effect: 2 entries
        assertTrue(pt.size() == 2);
        assertEquals(pt.getEntries()[0], pe);
        assertEquals(pt.getEntries()[1], pe2);
    }

    public void testEffect3() throws RepositoryException {
        PolicyTemplate pt = createEmptyTemplate(getTestPath());
        PolicyEntry pe = new ACEImpl(testPrincipal, PrivilegeRegistry.WRITE, true);

        pt.setEntry(pe);

        // add deny entry for mod_props
        PolicyEntry pe2 = new ACEImpl(testPrincipal, PrivilegeRegistry.MODIFY_PROPERTIES, false);
        assertTrue(pt.setEntry(pe2));

        // net-effect: 2 entries with the allow entry being adjusted
        assertTrue(pt.size() == 2);
        PolicyEntry[] entries = pt.getEntries();
        for (int i = 0; i < entries.length; i++) {
            int privs = entries[i].getPrivilegeBits();
            if (entries[i].isAllow()) {
                assertTrue(privs == (PrivilegeRegistry.ADD_CHILD_NODES | PrivilegeRegistry.REMOVE_CHILD_NODES));
            } else {
                assertTrue(privs == PrivilegeRegistry.MODIFY_PROPERTIES);
            }
        }
    }

    public void testMultiplePrincipals() throws RepositoryException {
        Principal princ2 = new Principal() {
            public String getName() {
                return "AnotherPrincipal";
            }
        };

        PolicyTemplate pt = createEmptyTemplate(getTestPath());
        PolicyEntry pe = new ACEImpl(testPrincipal, PrivilegeRegistry.READ, true);
        pt.setEntry(pe);

        // add deny entry for mod_props
        pe = new ACEImpl(princ2, PrivilegeRegistry.READ, true);
        assertTrue(pt.setEntry(pe));
        assertTrue(pt.getEntries().length == 2);
    }

    public void testRemoveEntry() throws RepositoryException {
        PolicyTemplate pt = createEmptyTemplate(getTestPath());
        PolicyEntry pe = new ACEImpl(testPrincipal, PrivilegeRegistry.READ, true);
        pt.setEntry(pe);

        assertTrue(pt.removeEntry(pe));
    }

    public void testRemoveNonExisting() throws RepositoryException {
        PolicyTemplate pt = createEmptyTemplate(getTestPath());
        PolicyEntry pe = new ACEImpl(testPrincipal, PrivilegeRegistry.READ, true);
        pt.setEntry(pe);
        PolicyEntry pe2 = new ACEImpl(testPrincipal, PrivilegeRegistry.READ, false);
        pt.setEntry(pe2);

        assertFalse(pt.removeEntry(pe));
    }

    public void testSetEntryForGroupPrincipal() throws RepositoryException {
        PolicyTemplate pt = createEmptyTemplate(getTestPath());

        // adding allow-entry must succeed
        PolicyEntry pe = new ACEImpl(testGroup, PrivilegeRegistry.READ, true);
        assertTrue(pt.setEntry(pe));

        // adding deny-entry must succeed
        pe = new ACEImpl(testGroup, PrivilegeRegistry.READ, false);
        try {
            pt.setEntry(pe);
            fail("Adding DENY-ace for a group principal should fail.");
        } catch (AccessControlException e) {
            // success
        }
    }
}