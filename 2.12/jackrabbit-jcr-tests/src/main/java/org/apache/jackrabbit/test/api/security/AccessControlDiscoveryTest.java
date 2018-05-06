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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.test.NotExecutableException;

/**
 * <code>AccessControlDiscoveryTest</code>...
 */
public class AccessControlDiscoveryTest extends AbstractAccessControlTest {

    public void testGetSupportedPrivileges() throws RepositoryException {
        // retrieving supported privileges:
        // Quote from spec:
        // "[...] it returns the privileges that the repository supports."
        Privilege[] privileges = acMgr.getSupportedPrivileges(testRootNode.getPath());

        // Quote from spec:
        // "A repository must support the following standard privileges."
        List<String> names = new ArrayList<String>(privileges.length);
        for (int i = 0; i < privileges.length; i++) {
            names.add(privileges[i].getName());
        }

        // test if those privileges are present:
        String msg = "A repository must support the privilege ";
        assertTrue(msg + Privilege.JCR_READ, names.contains(getJCRName(Privilege.JCR_READ, superuser)));
        assertTrue(msg + Privilege.JCR_ADD_CHILD_NODES, names.contains(getJCRName(Privilege.JCR_ADD_CHILD_NODES, superuser)));
        assertTrue(msg + Privilege.JCR_REMOVE_CHILD_NODES, names.contains(getJCRName(Privilege.JCR_REMOVE_CHILD_NODES, superuser)));
        assertTrue(msg + Privilege.JCR_MODIFY_PROPERTIES, names.contains(getJCRName(Privilege.JCR_MODIFY_PROPERTIES, superuser)));
        assertTrue(msg + Privilege.JCR_REMOVE_NODE, names.contains(getJCRName(Privilege.JCR_REMOVE_NODE, superuser)));
        assertTrue(msg + Privilege.JCR_READ_ACCESS_CONTROL, names.contains(getJCRName(Privilege.JCR_READ_ACCESS_CONTROL, superuser)));
        assertTrue(msg + Privilege.JCR_MODIFY_ACCESS_CONTROL, names.contains(getJCRName(Privilege.JCR_MODIFY_ACCESS_CONTROL, superuser)));
        assertTrue(msg + Privilege.JCR_WRITE, names.contains(getJCRName(Privilege.JCR_WRITE, superuser)));
        assertTrue(msg + Privilege.JCR_ALL, names.contains(getJCRName(Privilege.JCR_ALL, superuser)));
    }

    public void testPrivilegeFromName() throws RepositoryException {
        Privilege[] privileges = acMgr.getSupportedPrivileges(testRootNode.getPath());
        for (int i = 0; i < privileges.length; i++) {
            Privilege p = acMgr.privilegeFromName(privileges[i].getName());
            assertEquals("Expected equal privilege name.", privileges[i].getName(), p.getName());
            assertEquals("Expected equal privilege.", privileges[i], p);
        }
    }

    public void testMandatoryPrivilegeFromName() throws RepositoryException {
        List<String> l = new ArrayList<String>();
        l.add(getJCRName(Privilege.JCR_READ, superuser));
        l.add(getJCRName(Privilege.JCR_ADD_CHILD_NODES, superuser));
        l.add(getJCRName(Privilege.JCR_REMOVE_CHILD_NODES, superuser));
        l.add(getJCRName(Privilege.JCR_MODIFY_PROPERTIES, superuser));
        l.add(getJCRName(Privilege.JCR_REMOVE_NODE, superuser));
        l.add(getJCRName(Privilege.JCR_READ_ACCESS_CONTROL, superuser));
        l.add(getJCRName(Privilege.JCR_MODIFY_ACCESS_CONTROL, superuser));
        l.add(getJCRName(Privilege.JCR_WRITE, superuser));
        l.add(getJCRName(Privilege.JCR_ALL, superuser));

        for (Iterator<String> it = l.iterator(); it.hasNext();) {
            String privName = it.next();
            Privilege p = acMgr.privilegeFromName(privName);
            assertEquals("Expected equal privilege name.", privName, p.getName());
        }
    }

    public void testUnknownPrivilegeFromName() throws RepositoryException {
        String unknownPrivilegeName = Math.random() + "";
        try {
            acMgr.privilegeFromName(unknownPrivilegeName);
            fail(unknownPrivilegeName + " isn't the name of a known privilege.");
        } catch (AccessControlException e) {
            // success
        }
    }

    public void testAllPrivilegeContainsAll() throws RepositoryException, NotExecutableException {
        Privilege[] supported = acMgr.getSupportedPrivileges(testRootNode.getPath());

        Privilege all = acMgr.privilegeFromName(Privilege.JCR_ALL);
        Set<Privilege> allSet = new HashSet<Privilege>();
        allSet.addAll(Arrays.asList(all.getAggregatePrivileges()));

        String msg = "The all privilege must also contain ";
        for (int i=0; i < supported.length; i++) {
            Privilege sp = supported[i];
            if (sp.isAggregate()) {
                Collection<Privilege> col = Arrays.asList(sp.getAggregatePrivileges());
                assertTrue(msg + sp.getName(), allSet.containsAll(col));
            } else {
                assertTrue(msg + sp.getName(), allSet.contains(sp));
            }
        }
    }

    /**
     * Test the jcr:all privilege.
     * 
     * @throws RepositoryException
     * @throws NotExecutableException
     */
    public void testAllPrivilege() throws RepositoryException, NotExecutableException {
        Privilege all = acMgr.privilegeFromName(Privilege.JCR_ALL);
        assertFalse("All privilege must be not be abstract.", all.isAbstract());
        assertTrue("All privilege must be an aggregate privilege.", all.isAggregate());
        String expected = getJCRName(Privilege.JCR_ALL, superuser);
        assertEquals("The name of the all privilege must be " + expected, expected, all.getName());
    }

    /**
     * Test the jcr:write privilege.
     *
     * @throws RepositoryException If an error occurs.
     * @throws NotExecutableException If the test cannot be executed.
     */
    public void testWritePrivilege() throws RepositoryException, NotExecutableException {
        Privilege w = acMgr.privilegeFromName(Privilege.JCR_WRITE);
        assertTrue("Write privilege must be an aggregate privilege.", w.isAggregate());
        String expected = getJCRName(Privilege.JCR_WRITE, superuser);
        assertEquals("The name of the write privilege must be " + expected, expected, w.getName());
    }

    /**
     * Test some simple characteristics of the known aggregate privileges (jcr:write and jcr:all).
     *
     * @throws RepositoryException
     */
    public void testAggregregatePrivileges() throws RepositoryException {
        List<String> l = new ArrayList<String>();
        l.add(getJCRName(Privilege.JCR_WRITE, superuser));
        l.add(getJCRName(Privilege.JCR_ALL, superuser));

        for (Iterator<String> it = l.iterator(); it.hasNext();) {
            String privName = it.next();
            Privilege p = acMgr.privilegeFromName(privName);

            assertTrue("write and all must always be aggregate privileges.", p.isAggregate());
            Privilege[] aggregatedPrvs = p.getAggregatePrivileges();
            Privilege[] declaredPrvs = p.getDeclaredAggregatePrivileges();

            assertNotNull("An aggregate privilege must return the aggregated privileges", aggregatedPrvs);
            assertTrue("An aggregate privilege must return the aggregated privileges", aggregatedPrvs.length > 0);

            assertNotNull("An aggregate privilege must return the declared aggregated privileges", declaredPrvs);
            assertTrue("An aggregate privilege must return the declared aggregated privileges", declaredPrvs.length > 0);

            assertTrue("The may be at least the same amount of declared aggregated privileges.", aggregatedPrvs.length >= declaredPrvs.length);
        }

    }
    
    /**
     * Tests if the privilege name is treated as JCR Name and consequently
     * reflects changes made to the namespace prefix.
     * 
     *  @throws RepositoryException If an error occurs.
     */
    public void testPrivilegeName() throws RepositoryException {
        Privilege allPriv = acMgr.privilegeFromName(Privilege.JCR_ALL);
        try {
            String remappedPrefix = "_jcr";
            superuser.setNamespacePrefix(remappedPrefix, "http://www.jcp.org/jcr/1.0");

            List<Privilege> l = new ArrayList<Privilege>();
            l.add(acMgr.privilegeFromName(Privilege.JCR_ALL));
            l.add(acMgr.privilegeFromName(remappedPrefix + ":all"));

            for (Iterator<Privilege> it = l.iterator(); it.hasNext();) {
                Privilege p = it.next();

                assertEquals("The privilege name must reflect the modified namespace prefix.",remappedPrefix + ":all", p.getName());
                assertEquals("jcr:all privileges must be equal.",allPriv, p);
            }

            try {
                acMgr.privilegeFromName("jcr:all");
                fail("Modified namespace prefix: 'jcr:all' privilege must not exist.");
            } catch (RepositoryException e) {
                // success
            }
        } finally {
            superuser.setNamespacePrefix("jcr", "http://www.jcp.org/jcr/1.0");
        }
    }

    /**
     *
     * @throws RepositoryException If an error occurs.
     */
    public void testGetPrivileges() throws RepositoryException {
        acMgr.getPrivileges(testRootNode.getPath());
    }

    /**
     *
     * @throws RepositoryException If an error occurs.
     */
    public void testGetPrivilegesOnNonExistingNode() throws RepositoryException {
        String path = getPathToNonExistingNode();
        try {
            acMgr.getPrivileges(path);
            fail("AccessControlManager.getPrivileges for an invalid absPath must throw PathNotFoundException.");
        } catch (PathNotFoundException e) {
            // ok
        }
    }

    /**
     *
     * @throws RepositoryException If an error occurs.
     * @throws NotExecutableException If the test cannot be executed.
     */
    public void testGetPrivilegesOnProperty() throws RepositoryException, NotExecutableException {
        String path = getPathToProperty();
        try {
            acMgr.getPrivileges(path);
            fail("AccessControlManager.getPrivileges for a property path must throw PathNotFoundException.");
        } catch (PathNotFoundException e) {
            // ok
        }
    }

    /**
     *
     * @throws RepositoryException If an error occurs.
     */
    public void testHasPrivileges() throws RepositoryException {
        Privilege[] privs = acMgr.getPrivileges(testRootNode.getPath());
        assertTrue(acMgr.hasPrivileges(testRootNode.getPath(), privs));
    }

    /**
     *
     * @throws RepositoryException If an error occurs.
     */
    public void testHasIndividualPrivileges() throws RepositoryException {
        Privilege[] privs = acMgr.getPrivileges(testRootNode.getPath());

        for (int i = 0; i < privs.length; i++) {
            Privilege[] single = new Privilege[] {privs[i]};
            assertTrue(acMgr.hasPrivileges(testRootNode.getPath(), single));
        }
    }

    /**
     *
     * @throws RepositoryException If an error occurs.
     * @throws NotExecutableException If the test cannot be executed.
     */
    public void testNotHasPrivileges() throws RepositoryException, NotExecutableException {
        Privilege[] privs = acMgr.getPrivileges(testRootNode.getPath());
        Privilege all = acMgr.privilegeFromName(Privilege.JCR_ALL);

        // remove all privileges that are granted.
        Set<Privilege> notGranted = new HashSet<Privilege>(Arrays.asList(all.getAggregatePrivileges()));
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
            Privilege[] toTest = notGranted.toArray(new Privilege[notGranted.size()]);
            assertTrue(!acMgr.hasPrivileges(testRootNode.getPath(), toTest));
        }
    }

    /**
     *
     * @throws RepositoryException If an error occurs.
     */
    public void testHasPrivilegesOnNotExistingNode() throws RepositoryException {
        String path = getPathToNonExistingNode();
        try {
            acMgr.hasPrivileges(path, new Privilege[0]);
            fail("AccessControlManager.hasPrivileges for an invalid absPath must throw PathNotFoundException.");
        } catch (PathNotFoundException e) {
            // success
        }
    }

    /**
     *
     * @throws RepositoryException If an error occurs.
     * @throws NotExecutableException If the test cannot be executed.
     */
    public void testHasPrivilegesOnProperty() throws RepositoryException, NotExecutableException {
        String path = getPathToProperty();
        try {
            acMgr.hasPrivileges(path, new Privilege[0]);
            fail("AccessControlManager.hasPrivileges for a property path must throw PathNotFoundException.");
        } catch (PathNotFoundException e) {
            // success
        }
    }

    /**
     *
     * @throws RepositoryException If an error occurs.
     * @throws NotExecutableException If the test cannot be executed.
     */
    public void testHasPrivilegesEmptyArray() throws RepositoryException, NotExecutableException {
        assertTrue(acMgr.hasPrivileges(testRootNode.getPath(), new Privilege[0]));
    }

    //--------------------------------------------------------------------------
    /**
     * Retrieve the prefixed jcr name from a given privilege name constant.
     *
     * @param privilegeNameConstant
     * @param session
     * @return
     * @throws RepositoryException If an error occurs.
     */
    private static String getJCRName(String privilegeNameConstant, Session session) throws RepositoryException {
        int pos = privilegeNameConstant.indexOf('}');
        String uri = privilegeNameConstant.substring(1, pos);
        String localName = privilegeNameConstant.substring(pos + 1);
        return session.getNamespacePrefix(uri) + ":" + localName;
    }
}