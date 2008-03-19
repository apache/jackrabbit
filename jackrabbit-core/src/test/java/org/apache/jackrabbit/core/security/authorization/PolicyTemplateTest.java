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
import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.core.security.jsr283.security.AbstractAccessControlTest;
import org.apache.jackrabbit.core.security.jsr283.security.AccessControlException;
import org.apache.jackrabbit.core.security.jsr283.security.AccessControlPolicy;
import org.apache.jackrabbit.core.security.jsr283.security.AccessControlPolicyIterator;
import org.apache.jackrabbit.core.security.jsr283.security.Privilege;
import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.security.Principal;

/**
 * <code>PolicyTemplateTest</code>...
 */
public class PolicyTemplateTest extends AbstractAccessControlTest {

    private static Logger log = LoggerFactory.getLogger(PolicyTemplateTest.class);

    private PolicyTemplate templ;

    protected void setUp() throws Exception {
        super.setUp();

        Node n = testRootNode.addNode(nodeName1, testNodeType);
        superuser.save();

        AccessControlPolicyIterator it = acMgr.getApplicablePolicies(n.getPath());
        if (it.hasNext()) {
            AccessControlPolicy p = it.nextAccessControlPolicy();
            if (p instanceof PolicyTemplate) {
                templ = (PolicyTemplate) p;
            } else {
                throw new NotExecutableException("No PolicyTemplate to test.");
            }
        } else {
            throw new NotExecutableException("No PolicyTemplate to test.");
        }
    }

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
        PrincipalIterator it = pMgr.getPrincipals(PrincipalManager.SEARCH_TYPE_ALL);
        if (it.hasNext()) {
            return it.nextPrincipal();
        } else {
            throw new NotExecutableException();
        }
    }

    private static void assertSamePrivileges(Privilege[] privs1, Privilege[] privs2) throws AccessControlException {
        assertEquals(PrivilegeRegistry.getBits(privs1), PrivilegeRegistry.getBits(privs2));
    }

    public void testIsEmpty() {
        if (templ.isEmpty()) {
            assertEquals(0, templ.size());
            assertEquals(0, templ.getEntries().length);
        } else {
            assertTrue(templ.size() > 0);
            assertTrue(templ.getEntries().length > 0);
        }
    }

    // TODO:

    /*
    public void testGrantAll() throws NotExecutableException, RepositoryException {
        Principal princ = getValidPrincipal();
        Privilege[] priv = PrivilegeRegistry.getPrivileges(new String[] {Privilege.ALL});

        List entriesBefore = Arrays.asList(templ.getEntries(princ));
        if (templ.grantPrivileges(princ, priv)) {
            PolicyEntry[] entries = templ.getEntries(princ);
            if (entries.length == 0) {
                fail("GrantPrivileges was successful -> at least 1 entry for principal.");
            }
            for (int i = 0; i < entries.length; i++) {
                PolicyEntry en = entries[i];
                if (en.isAllow()) {
                    assertSamePrivileges(priv, en.getPrivileges());
                } else {
                    fail("Granting ALL privileges must remove any present 'deny' entries.");
                }
            }
        } else {
            PolicyEntry[] entries = templ.getEntries(princ);
            assertEquals("Grant ALL not successful -> entries must not have changed.", entriesBefore, Arrays.asList(entries));
        }
    }

    public void testGrantWrite() throws NotExecutableException, RepositoryException {
        Principal princ = getValidPrincipal();
        Privilege[] priv = PrivilegeRegistry.getPrivileges(new String[] {Privilege.WRITE});

        boolean writeIsGranted = false;
        if (templ.grantPrivileges(princ, priv)) {
            PolicyEntry[] entries = templ.getEntries(princ);
            assertTrue("GrantPrivileges was successful -> at least 1 entry for principal.", entries.length > 0);

            for (int i = 0; i < entries.length; i++) {
                PolicyEntry en = entries[i];
                int bits = PrivilegeRegistry.getBits(en.getPrivileges());
                if (en.isAllow()) {
                    writeIsGranted = (bits & PrivilegeRegistry.WRITE) > 0;
                } else {
                    fail("After successfully granting WRITE, no deny-WRITE must be present any more.");
                }
            }
            assertTrue("After successfully granting WRITE, the entries must reflect this", writeIsGranted);
        }
    }

    public void testGrantWriteDenyRemove() throws NotExecutableException, RepositoryException {
        Principal princ = getValidPrincipal();
        Privilege[] grPriv = PrivilegeRegistry.getPrivileges(new String[] {Privilege.WRITE});
        Privilege[] dePriv = PrivilegeRegistry.getPrivileges(new String[] {Privilege.REMOVE_CHILD_NODES});

        if (templ.grantPrivileges(princ, grPriv) && templ.denyPrivileges(princ, dePriv)) {
            PolicyEntry[] entries = templ.getEntries(princ);
            assertFalse("Grant & subsequent Deny were both successful -> at least 2 entry for principal.", entries.length < 2);

            for (int i = 0; i < entries.length; i++) {
                PolicyEntry en = entries[i];
                int bits = PrivilegeRegistry.getBits(en.getPrivileges());
                if (en.isAllow()) {
                    int remaining = PrivilegeRegistry.diff(PrivilegeRegistry.WRITE, PrivilegeRegistry.REMOVE_CHILD_NODES);
                    assertTrue((bits & remaining) > 0);
                } else {
                    assertTrue((bits & PrivilegeRegistry.REMOVE_CHILD_NODES) > 0);
                }
            }
        } else {
            throw new NotExecutableException();
        }
    }

    public void testRemoveEntry() throws NotExecutableException, RepositoryException {
        Principal princ = getValidPrincipal();
        Privilege[] grPriv = PrivilegeRegistry.getPrivileges(new String[] {Privilege.WRITE});

        if (templ.grantPrivileges(princ, grPriv)) {
            PolicyEntry[] entries = templ.getEntries();
            assertTrue("Grant was both successful -> at least 1 entry.", entries.length > 0);

            for (int i = 0; i < entries.length; i++) {
                PolicyEntry en = entries[i];
                assertTrue(templ.removeEntry(en));
            }

            assertTrue(templ.isEmpty());
            assertEquals(0, templ.size());
            assertEquals(0, templ.getEntries().length);
            assertEquals(0, templ.getEntries(princ).length);
        } else {
            throw new NotExecutableException();
        }
    }
    */
}