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

/**
 * Tests the functionality of the JCR AccessControlList API implementation. The
 * purpose is to test the consistency of the access control list by a, adding ,
 * deleting and modifying entries in the list.
 */
public class AccessControlListImplTest extends AbstractAccessControlTest {

    private QValueFactory vFactory;

    private Principal unknownPrincipal;
    private Principal knownPrincipal;

    private NamePathResolver resolver;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        resolver = new DefaultNamePathResolver(superuser);
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
        return new AccessControlListImpl(aclPath, resolver, vFactory);
    }

    private Map<String, Value> createEmptyRestriction() {
        return Collections.<String, Value> emptyMap();
    }

    public void testAddingDifferentEntries() throws Exception {
        JackrabbitAccessControlList acl = createAccessControList(testRoot);

        // allow read to unknownPrincipal
        Privilege[] p = privilegesFromName(Privilege.JCR_READ);
        acl.addAccessControlEntry(unknownPrincipal, p);

        // allow addChildNodes to secondPrincipal
        p = privilegesFromName(Privilege.JCR_ADD_CHILD_NODES);
        acl.addAccessControlEntry(knownPrincipal, p);

        // deny modifyAccessControl to 'unknown' principal
        p = privilegesFromName(Privilege.JCR_MODIFY_ACCESS_CONTROL);
        acl.addEntry(unknownPrincipal, p, false);

        // deny jcr:nodeTypeManagement to secondPrincipal
        p = privilegesFromName(Privilege.JCR_NODE_TYPE_MANAGEMENT);
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

    public void testMultipleEntryEffect() throws Exception {
        JackrabbitAccessControlList acl = createAccessControList(testRoot);
        Privilege[] privileges = privilegesFromName(Privilege.JCR_READ);

        // GRANT 'read' privilege to the Admin user -> list now contains one
        // allow entry
        assertTrue(acl.addAccessControlEntry(unknownPrincipal, privileges));

        // policy contains a single entry
        assertEquals(1, acl.size());

        AccessControlEntry[] entries = acl.getAccessControlEntries();

        // ... and the entry grants a single privilege
        assertEquals(1, entries[0].getPrivileges().length);
        assertEquals("jcr:read", entries[0].getPrivileges()[0].getName());

        // GRANT 'add_child_node' privilege for the admin user -> same entry but
        // with an additional 'add_child_node' privilege.
        privileges = privilegesFromNames(new String[] {Privilege.JCR_ADD_CHILD_NODES, Privilege.JCR_READ });
        assertTrue(acl.addAccessControlEntry(unknownPrincipal, privileges));

        // A new Entry was added -> entries count should be 2.
        assertEquals(2, acl.size());

        // The single entry should now contain both 'read' and 'add_child_nodes'
        // privileges for the same principal.
        assertEquals(1, acl.getAccessControlEntries()[0].getPrivileges().length);
        assertEquals(2, acl.getAccessControlEntries()[1].getPrivileges().length);

        // adding a privilege that's already granted for the same principal ->
        // again modified as the client doesn't care about possible compaction the
        // server may want to make.
        privileges = privilegesFromNames(new String[] { Privilege.JCR_READ });
        assertTrue(acl.addAccessControlEntry(unknownPrincipal, privileges));
        assertEquals(3, acl.size());

        // revoke the read privilege
        assertTrue("Fail to revoke read privilege", acl.addEntry(unknownPrincipal, privileges, false, createEmptyRestriction()));

        // should now be 3 entries -> 2 allow entry + a deny entry
        assertEquals(4, acl.size());
    }

    public void testMultipleEntryEffect2() throws Exception {
        JackrabbitAccessControlList acl = createAccessControList(testRoot);
        // GRANT a read privilege
        Privilege[] privileges = privilegesFromNames(new String[] { Privilege.JCR_READ });
        assertTrue("New Entry -> grants read privilege", acl.addAccessControlEntry(unknownPrincipal, privileges));

        assertTrue("Fail to revoke the read privilege", acl.addEntry(unknownPrincipal, privileges, false, createEmptyRestriction()));
        Assert.assertEquals(2, acl.size());
    }

    // -------------------------------------------------------< utility methods >---

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
