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
import org.apache.jackrabbit.api.jsr283.security.AccessControlEntry;
import org.apache.jackrabbit.api.jsr283.security.AccessControlException;
import org.apache.jackrabbit.api.jsr283.security.Privilege;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.core.security.authorization.AbstractACLTemplateTest;
import org.apache.jackrabbit.core.security.authorization.JackrabbitAccessControlList;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.RepositoryException;
import java.security.Principal;
import java.security.acl.Group;
import java.util.Collections;

/**
 * <code>ACLTemplateTest</code>...
 */
public class ACLTemplateTest extends AbstractACLTemplateTest {

    protected String getTestPath() {
        return "/ab/c/d";
    }

    protected JackrabbitAccessControlList createEmptyTemplate(String path) throws RepositoryException {
        SessionImpl sImpl = (SessionImpl) superuser;
        PrincipalManager princicipalMgr = sImpl.getPrincipalManager();
        PrivilegeRegistry privilegeRegistry = new PrivilegeRegistry(sImpl);
        return new ACLTemplate(path, princicipalMgr, privilegeRegistry, sImpl.getValueFactory());
    }

    public void testMultipleEntryEffect() throws RepositoryException, NotExecutableException {
        JackrabbitAccessControlList pt = createEmptyTemplate(getTestPath());
        Privilege[] privileges = privilegesFromName(Privilege.JCR_READ);
        pt.addEntry(testPrincipal, privileges, true, Collections.EMPTY_MAP);

        // new entry extends privs.
        privileges = privilegesFromNames(new String[] {
                Privilege.JCR_READ,
                Privilege.JCR_ADD_CHILD_NODES});
        assertTrue(pt.addEntry(testPrincipal,
                privileges,
                true, Collections.EMPTY_MAP));

        // net-effect: only a single allow-entry with both privileges
        assertTrue(pt.size() == 1);
        assertSamePrivileges(privileges, pt.getAccessControlEntries()[0].getPrivileges());

        // adding just ADD_CHILD_NODES -> must not remove READ priv
        Privilege[] achPrivs = privilegesFromName(Privilege.JCR_ADD_CHILD_NODES);
        assertFalse(pt.addEntry(testPrincipal, achPrivs, true, Collections.EMPTY_MAP));
        // net-effect: only a single allow-entry with add_child_nodes + read priv
        assertTrue(pt.size() == 1);
        assertSamePrivileges(privileges, pt.getAccessControlEntries()[0].getPrivileges());

        // revoke the 'READ' privilege
        privileges = privilegesFromName(Privilege.JCR_READ);
        assertTrue(pt.addEntry(testPrincipal, privileges, false, Collections.EMPTY_MAP));
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
        Privilege[] privileges = privilegesFromName(PrivilegeRegistry.REP_WRITE);
        JackrabbitAccessControlList pt = createEmptyTemplate(getTestPath());
        pt.addAccessControlEntry(testPrincipal, privileges);

        // add deny entry for mod_props
        Privilege[] privileges2 = privilegesFromName(Privilege.JCR_MODIFY_PROPERTIES);
        assertTrue(pt.addEntry(testPrincipal, privileges2, false, null));

        // net-effect: 2 entries with the allow entry being adjusted
        assertTrue(pt.size() == 2);
        AccessControlEntry[] entries = pt.getAccessControlEntries();
        for (int i = 0; i < entries.length; i++) {
            ACLTemplate.Entry entry = (ACLTemplate.Entry) entries[i];
            int privs = entry.getPrivilegeBits();
            if (entry.isAllow()) {
                int bits = PrivilegeRegistry.getBits(privileges) ^ PrivilegeRegistry.getBits(privileges2);
                assertEquals(privs, bits);
            } else {
                assertEquals(privs, PrivilegeRegistry.getBits(privileges2));
            }
        }
    }

    public void testMultiplePrincipals() throws RepositoryException, NotExecutableException {
        PrincipalManager pMgr = ((JackrabbitSession) superuser).getPrincipalManager();
        Principal everyone = pMgr.getEveryone();
        Principal grPrincipal = null;
        PrincipalIterator it = pMgr.findPrincipals("", PrincipalManager.SEARCH_TYPE_GROUP);
        while (it.hasNext()) {
            Group gr = (Group) it.nextPrincipal();
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

        // add same privs for another principal -> must modify as well.
        assertTrue(pt.addAccessControlEntry(everyone, privs));
        // .. 2 entries must be present.
        assertTrue(pt.getAccessControlEntries().length == 2);
    }

    public void testSetEntryForGroupPrincipal() throws RepositoryException, NotExecutableException {
        JackrabbitAccessControlList pt = createEmptyTemplate(getTestPath());
        Privilege[] privs = privilegesFromName(Privilege.JCR_READ);
        Group grPrincipal = (Group) pMgr.getEveryone();

        // adding allow-entry must succeed
        assertTrue(pt.addAccessControlEntry(grPrincipal, privs));

        // adding deny-entry must succeed
        try {
            pt.addEntry(grPrincipal, privs, false, null);
            fail("Adding DENY-ace for a group principal should fail.");
        } catch (AccessControlException e) {
            // success
        }
    }

    public void testRevokeEffect() throws RepositoryException, NotExecutableException {
        JackrabbitAccessControlList pt = createEmptyTemplate(getTestPath());
        Privilege[] privileges = privilegesFromName(Privilege.JCR_READ);

        pt.addEntry(testPrincipal, privileges, true, Collections.EMPTY_MAP);

        // same entry but with revers 'isAllow' flag
        assertTrue(pt.addEntry(testPrincipal, privileges, false, Collections.EMPTY_MAP));

        // net-effect: only a single deny-read entry
        assertTrue(pt.size() == 1);
        assertSamePrivileges(privileges, pt.getAccessControlEntries()[0].getPrivileges());
    }
}