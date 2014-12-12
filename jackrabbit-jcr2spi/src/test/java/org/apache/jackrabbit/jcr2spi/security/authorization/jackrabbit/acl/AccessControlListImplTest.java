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
package org.apache.jackrabbit.jcr2spi.security.authorization.jackrabbit.acl;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.Privilege;

import junit.framework.Assert;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.value.QValueFactoryImpl;
import org.apache.jackrabbit.test.api.security.AbstractAccessControlTest;
import org.junit.Test;

/**
 * Tests the functionality of the JCR AccessControlList API implementation. The
 * purpose is to test the consistency of the access control list by a, adding ,
 * deleting and modifying entries in the list.
 */
public class AccessControlListImplTest extends AbstractAccessControlTest {

    private QValueFactory vFactory;

    private Principal unknownPrincipal;
    private Principal knownPrincipal;

    private NamePathResolver npResolver;
    @Override
    public void setUp() throws Exception {
        super.setUp();

        npResolver = new DefaultNamePathResolver(getHelper().getSuperuserSession());
        vFactory = QValueFactoryImpl.getInstance();

        unknownPrincipal = getHelper().getUnknownPrincipal(superuser);
        knownPrincipal = new Principal() {
            @Override
            public String getName() {
                return "everyone";
            }
        };
    }

    private JackrabbitAccessControlList createAccessControList(String aclPath)
            throws RepositoryException {
        return new AccessControlListImpl(aclPath, npResolver, vFactory);
    }

    private Map<String, Value> createEmptyRestriction() {
        return Collections.<String, Value> emptyMap();
    }

    @Test
    public void testAddingDifferentEntries() throws Exception {
        JackrabbitAccessControlList acl = createAccessControList(testRoot);

        // allow read to unknownPrincipal
        Privilege[] p = privilegesFromName(getJcrName(Privilege.JCR_READ));
        acl.addAccessControlEntry(unknownPrincipal, p);

        // allow addChildNodes to secondPrincipal
        p = privilegesFromName(getJcrName(Privilege.JCR_ADD_CHILD_NODES));
        acl.addAccessControlEntry(knownPrincipal, p);

        // deny modifyAccessControl to 'unknown' principal
        p = privilegesFromName(getJcrName(Privilege.JCR_MODIFY_ACCESS_CONTROL));
        acl.addEntry(unknownPrincipal, p, false);

        // deny jcr:nodeTypeManagement to secondPrincipal
        p = privilegesFromName(getJcrName(Privilege.JCR_NODE_TYPE_MANAGEMENT));
        acl.addEntry(knownPrincipal, p, false);

        // four different entries
        Assert.assertEquals(4, acl.size());
        
        // UnknownPrincipal entries
        AccessControlEntry[] pentries = getEntries(acl, unknownPrincipal);
        Assert.assertEquals(2, pentries.length);
        
        // secondPrincipal entries
        AccessControlEntry[] sentries = getEntries(acl, knownPrincipal);
        Assert.assertEquals(2, sentries.length);
        
    }

    @Test
    public void testMultipleEntryEffect() throws Exception {
        JackrabbitAccessControlList acl = createAccessControList(testRoot);
        Privilege[] privileges = privilegesFromName(getJcrName(Privilege.JCR_READ));

        // GRANT 'read' privilege to the Admin user -> list now contains one
        // allow entry
        boolean actual = acl.addAccessControlEntry(unknownPrincipal, privileges);
        assertTrue(actual);

        // policy contains a single entry
        assertEquals(1, acl.size());

        AccessControlEntry[] entries = acl.getAccessControlEntries();

        // ... and the entry grants a single privilege
        assertEquals(1, entries[0].getPrivileges().length);
        assertEquals("jcr:read", entries[0].getPrivileges()[0].getName());

        // GRANT 'add_child_node' privilege for the admin user -> same entry but
        // with an additional 'add_child_node' privilege.
        privileges = privilegesFromNames(new String[] {getJcrName(Privilege.JCR_ADD_CHILD_NODES), getJcrName(Privilege.JCR_READ) });

        actual = acl.addAccessControlEntry(unknownPrincipal, privileges);
        assertTrue(actual);

        // A new Entry was added -> entries count should still be 2.
        assertEquals(2, acl.size());

        // The single entry should now contain both 'read' and 'add_child_nodes'
        // privileges for the same principal.
        assertEquals(1, acl.getAccessControlEntries()[0].getPrivileges().length);
        assertEquals(2, acl.getAccessControlEntries()[1].getPrivileges().length);

        // adding a privilege that's already granted for the same principal ->
        // again modified as the client doesn't care about possible compaction the
        // server may want to make.
        privileges = privilegesFromNames(new String[] { getJcrName(Privilege.JCR_READ) });
        actual = acl.addAccessControlEntry(unknownPrincipal, privileges);
        assertTrue(actual);

        // revoke the read privilege
        actual = acl.addEntry(unknownPrincipal, privileges, false, createEmptyRestriction());
        assertTrue("Fail to revoke read privilege", actual);

        // should now be two entries -> an allow entry + a deny entry
        assertEquals(2, acl.size());

        // allow entry contains only a single privilege
        assertTrue(acl.getAccessControlEntries()[0].getPrivileges().length == 1);

        // ... and that privilege should not be a 'read' privilege -> was revoked
        String jcrName = acl.getAccessControlEntries()[0].getPrivileges()[0].getName();
        assertNotSame(getJcrName(getJcrName(Privilege.JCR_READ)), jcrName);

        // deny entry contains a single privilege -> 'read' privilege
        jcrName = acl.getAccessControlEntries()[1].getPrivileges()[0].getName();
        assertEquals(getJcrName(getJcrName(Privilege.JCR_READ)), jcrName);

        // remove the allow entry
        acl.removeAccessControlEntry(acl.getAccessControlEntries()[0]);

        // ... list should now only contain a single entry -> the deny entry
        assertTrue(acl.size() == 1);

        // remove the deny entry
        acl.removeAccessControlEntry(acl.getAccessControlEntries()[0]);

        // ... list must be empty at this point
        assertTrue(acl.isEmpty());

        // GRANT a read privilege
        privileges = privilegesFromNames(new String[] { getJcrName(Privilege.JCR_READ) });
        actual = acl.addAccessControlEntry(unknownPrincipal, privileges);
        assertTrue("New Entry -> grants read privilege", actual);

        actual = acl.addEntry(unknownPrincipal, privileges, false, createEmptyRestriction());
        assertTrue("Fail to revoke the read privilege", actual);

        Assert.assertEquals(1, acl.size());
    }
    
    
    // -------------------------------------------------------< utility methods >---
    private Name getQName(String name) throws RepositoryException {
        return npResolver.getQName(name);
    }

    private String getJcrName(String name) throws RepositoryException {
        return npResolver.getJCRName(getQName(name));
    }

    private AccessControlEntry[] getEntries(AccessControlList acl, Principal princ) throws RepositoryException {
        AccessControlEntry[] entries = acl.getAccessControlEntries();
        List<AccessControlEntry> entriesPerPrincipal = new ArrayList<AccessControlEntry>(2);
        for (AccessControlEntry entry : entries) {
            if (entry.getPrincipal().getName().equals(princ.getName())) {
                entriesPerPrincipal.add(entry);
            }
        }
        return entriesPerPrincipal.toArray(new AccessControlEntry[entriesPerPrincipal.size()]);
    }
 }
