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

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.core.security.jsr283.security.AccessControlException;
import org.apache.jackrabbit.test.JUnitTest;
import org.apache.jackrabbit.uuid.UUID;

import javax.jcr.RepositoryException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * <code>ACLImplTest</code>...
 */
public class ACLImplTest extends JUnitTest {

    private static Principal testPrincipal = new Principal() {
        public String getName() {
            return "TestPrincipal";
        }
    };

    private static ACEImpl createACE(int privileges, boolean isAllow) {
        return new ACEImpl(testPrincipal, privileges, isAllow);
    }

    private static ACLImpl getEmptyACL(NodeId nid) {
        NodeId id = (nid == null) ? new NodeId(UUID.randomUUID()) : nid;
        return new ACLImpl(id, Collections.EMPTY_LIST, null, false);
    }

    private static ACLImpl getLocalACL(List localEntries, boolean protectsAcl) {
        NodeId id = new NodeId(UUID.randomUUID());
        return new ACLImpl(id, localEntries, null, protectsAcl);
    }

    private static ACLImpl getComplexACL(List localEntries, ACLImpl base,
                                         boolean protectsAcl) {
        NodeId id = new NodeId(UUID.randomUUID());
        return new ACLImpl(id, localEntries, base, protectsAcl);
    }

    public void testGetName() throws RepositoryException {
        ACLImpl acl = getEmptyACL(null);

        assertNotNull(acl.getName());
        assertEquals(ACLImpl.POLICY_NAME, acl.getName());
    }

    public void testGetId() {
        NodeId nid = new NodeId(UUID.randomUUID());
        ACLImpl acl = getEmptyACL(nid);

        assertEquals(nid, acl.getId());
    }

    public void testGetEntries() {
        // an empty acl must not have any entries not even inherited onces.
        ACLImpl acl = getEmptyACL(null);
        assertNotNull(acl.getEntries());
        assertFalse(acl.getEntries().hasNext());

        // create an acl with local entries but no inherited onces
        List aces = new ArrayList();
        aces.add(createACE(PrivilegeRegistry.ALL, true));
        aces.add(createACE(PrivilegeRegistry.ADD_CHILD_NODES, false));
        aces.add(createACE(PrivilegeRegistry.READ_AC | PrivilegeRegistry.MODIFY_AC, false));

        acl = getLocalACL(aces, false);
        int i = 0;
        for (Iterator it = acl.getEntries(); it.hasNext();) {
            ACEImpl ace = (ACEImpl) it.next();
            assertEquals(aces.get(i), ace);
            i++;
        }

        // create an acl with inherited entries but no local
        acl = getComplexACL(Collections.EMPTY_LIST, acl, false);
        i = 0;
        for (Iterator it = acl.getEntries(); it.hasNext();) {
            ACEImpl ace = (ACEImpl) it.next();
            assertEquals(aces.get(i), ace);
            i++;
        }

        // create acl with inherited and local entries
        List local = new ArrayList();
        local.add(createACE(PrivilegeRegistry.ALL, false));
        local.add(createACE(PrivilegeRegistry.READ, true));
        local.add(createACE(PrivilegeRegistry.READ_AC, true));

        acl = getComplexACL(local, acl, false);
        List test = new ArrayList();
        test.addAll(local);
        test.addAll(aces);
        i = 0;
        for (Iterator it = acl.getEntries(); it.hasNext();) {
            ACEImpl ace = (ACEImpl) it.next();
            assertEquals(test.get(i), ace);
            i++;
        }
    }

    public void testGetPrivileges() throws AccessControlException {
        ACLImpl acl = getEmptyACL(null);
        assertTrue(PrivilegeRegistry.NO_PRIVILEGE == acl.getPrivileges());

        // TODO: inherited
        // TODO: locals
    }

    public void testGetPermissions() throws AccessControlException {
        ACLImpl acl = getEmptyACL(null);
        assertTrue(Permission.NONE == acl.getPermissions("any"));

        // TODO: inherited
        // TODO: locals
        // TODO: test effect of protectsACL flag
    }

    public void testProtectsFlag() {
        // TODO

    }

}