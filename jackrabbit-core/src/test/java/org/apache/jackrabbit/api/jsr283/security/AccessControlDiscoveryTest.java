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

import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <code>AccessControlDiscoveryTest</code>...
 */
public class AccessControlDiscoveryTest extends AbstractAccessControlTest {

    private Privilege getPrivilege(String name) throws RepositoryException, NotExecutableException {
        Privilege[] privileges = acMgr.getSupportedPrivileges(testRootNode.getPath());
        for (int i = 0; i < privileges.length; i++) {
            if (name.equals(privileges[i].getName())) {
                return privileges[i];
            }
        }
        throw new NotExecutableException();
    }

    public void testGetSupportedPrivileges() throws RepositoryException {
        // retrieving supported privileges:
        // Quote from spec:
        // "[...] it returns the privileges that the repository supports."
        Privilege[] privileges = acMgr.getSupportedPrivileges(testRootNode.getPath());

        // Quote from spec:
        // "A repository must support the following standard privileges."
        List names = new ArrayList(privileges.length);
        for (int i = 0; i < privileges.length; i++) {
            names.add(privileges[i].getName());
        }

        // test if those privileges are present:
        String msg = "A repository must support the privilege ";
        assertTrue(msg + Privilege.READ, names.contains(Privilege.READ));
        assertTrue(msg + Privilege.ADD_CHILD_NODES, names.contains(Privilege.ADD_CHILD_NODES));
        assertTrue(msg + Privilege.REMOVE_CHILD_NODES, names.contains(Privilege.REMOVE_CHILD_NODES));
        assertTrue(msg + Privilege.MODIFY_PROPERTIES, names.contains(Privilege.MODIFY_PROPERTIES));
        assertTrue(msg + Privilege.READ_ACCESS_CONTROL, names.contains(Privilege.READ_ACCESS_CONTROL));
        assertTrue(msg + Privilege.MODIFY_ACCESS_CONTROL, names.contains(Privilege.MODIFY_ACCESS_CONTROL));
        assertTrue(msg + Privilege.WRITE, names.contains(Privilege.WRITE));
        assertTrue(msg + Privilege.ALL, names.contains(Privilege.ALL));
    }

    public void testAllPrivilegeContainsAll() throws RepositoryException, NotExecutableException {
        Privilege[] supported = acMgr.getSupportedPrivileges(testRootNode.getPath());

        Set allSet = new HashSet();
        Privilege all = getPrivilege(Privilege.ALL);
        allSet.addAll(Arrays.asList(all.getAggregatePrivileges()));

        String msg = "The all privilege must also contain ";
        for (int i=0; i < supported.length; i++) {
            Privilege sp = supported[i];
            if (sp.isAggregate()) {
                Collection col = Arrays.asList(sp.getAggregatePrivileges());
                assertTrue(msg + sp.getName(), allSet.containsAll(col));
            } else {
                assertTrue(msg + sp.getName(), allSet.contains(sp));
            }
        }
    }

    public void testAllPrivilege() throws RepositoryException, NotExecutableException {
        Privilege all = getPrivilege(Privilege.ALL);
        assertFalse("All privilege must be not be abstract.", all.isAbstract());
        assertTrue("All privilege must be an aggregate privilege.", all.isAggregate());
        assertEquals("The name of the all privilege must be " + Privilege.ALL, all.getName(), Privilege.ALL);
    }

    public void testWritePrivilege() throws RepositoryException, NotExecutableException {
        Privilege w = getPrivilege(Privilege.WRITE);
        assertTrue("Write privilege must be an aggregate privilege.", w.isAggregate());
        assertEquals("The name of the write privilege must be " + Privilege.WRITE, w.getName(), Privilege.WRITE);
    }

    public void testGetPrivileges() throws RepositoryException {
        acMgr.getPrivileges(testRootNode.getPath());
    }

    public void testGetPrivilegesOnNonExistingNode() throws RepositoryException {
        String path = getPathToNonExistingNode();
        try {
            acMgr.getPrivileges(path);
            fail("AccessControlManager.getPrivileges for an invalid absPath must throw PathNotFoundException.");
        } catch (PathNotFoundException e) {
            // ok
        }
    }

    public void testGetPrivilegesOnProperty() throws RepositoryException, NotExecutableException {
        String path = getPathToProperty();
        try {
            acMgr.getPrivileges(path);
            fail("AccessControlManager.getPrivileges for a property path must throw PathNotFoundException.");
        } catch (PathNotFoundException e) {
            // ok
        }
    }

    public void testHasPrivileges() throws RepositoryException {
        Privilege[] privs = acMgr.getPrivileges(testRootNode.getPath());
        assertTrue(acMgr.hasPrivileges(testRootNode.getPath(), privs));
    }

    public void testHasIndividualPrivileges() throws RepositoryException {
        Privilege[] privs = acMgr.getPrivileges(testRootNode.getPath());

        for (int i = 0; i < privs.length; i++) {
            Privilege[] single = new Privilege[] {privs[i]};
            assertTrue(acMgr.hasPrivileges(testRootNode.getPath(), single));
        }
    }

    public void testNotHasPrivileges() throws RepositoryException, NotExecutableException {
        Privilege[] privs = acMgr.getPrivileges(testRootNode.getPath());
        Privilege all = getPrivilege(Privilege.ALL);

        // remove all privileges that are granted.
        Set notGranted = new HashSet(Arrays.asList(all.getAggregatePrivileges()));
        for (int i = 0; i < privs.length; i++) {
            if (privs[i].isAggregate()) {
                notGranted.removeAll(Arrays.asList(privs[i].getAggregatePrivileges()));
            } else {
                notGranted.remove(privs[i]);
            }
        }

        // make sure that either 'all' are granted or the 'diff' is denied.
        if (notGranted.isEmpty()) {
            assertTrue(acMgr.hasPrivileges(testRootNode.getPath(), new Privilege[] {all}));
        } else {
            Privilege[] toTest = (Privilege[]) notGranted.toArray(new Privilege[notGranted.size()]);
            assertTrue(!acMgr.hasPrivileges(testRootNode.getPath(), toTest));
        }
    }

    public void testHasPrivilegesOnNotExistingNode() throws RepositoryException {
        String path = getPathToNonExistingNode();
        try {
            acMgr.hasPrivileges(path, new Privilege[0]);
            fail("AccessControlManager.hasPrivileges for an invalid absPath must throw PathNotFoundException.");
        } catch (PathNotFoundException e) {
            // success
        }
    }

    public void testHasPrivilegesOnProperty() throws RepositoryException, NotExecutableException {
        String path = getPathToProperty();
        try {
            acMgr.hasPrivileges(path, new Privilege[0]);
            fail("AccessControlManager.hasPrivileges for a property path must throw PathNotFoundException.");
        } catch (PathNotFoundException e) {
            // success
        }
    }

    public void testHasPrivilegesEmptyArray() throws RepositoryException, NotExecutableException {
        assertTrue(acMgr.hasPrivileges(testRootNode.getPath(), new Privilege[0]));
    }
}