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

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlManager;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.security.TestPrincipal;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.test.api.security.AbstractAccessControlTest;

/**
 * <code>EntryCollectorTest</code>...
 */
public class EntryCollectorTest extends AbstractAccessControlTest {

    private Group testGroup;
    private User testUser;

    private String path;
    private String childNPath;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // create some nodes below the test root in order to apply ac-stuff
        Node node = testRootNode.addNode(nodeName1, testNodeType);
        Node cn1 = node.addNode(nodeName2, testNodeType);
        superuser.save();

        path = node.getPath();
        childNPath = cn1.getPath();

        // create the testGroup
        UserManager umgr = getUserManager(superuser);

        Principal groupPrincipal = new TestPrincipal("testGroup" + UUID.randomUUID());
        testGroup = umgr.createGroup(groupPrincipal);
        testUser = umgr.createUser("testUser" + UUID.randomUUID(), "pw");
        if (!umgr.isAutoSave() && superuser.hasPendingChanges()) {
            superuser.save();
        }
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            if (testGroup != null) {
                testGroup.remove();
                if (!getUserManager(superuser).isAutoSave() && superuser.hasPendingChanges()) {
                    superuser.save();
                }
            }
            if (testUser != null) {
                testUser.remove();
                if (!getUserManager(superuser).isAutoSave() && superuser.hasPendingChanges()) {
                    superuser.save();
                }
            }
        } finally {
            super.tearDown();
        }
    }

    private static UserManager getUserManager(Session session) throws
            NotExecutableException {
        if (!(session instanceof JackrabbitSession)) {
            throw new NotExecutableException();
        }
        try {
            return ((JackrabbitSession) session).getUserManager();
        } catch (RepositoryException e) {
            throw new NotExecutableException();
        }
    }

    private ACLTemplate getPolicy(AccessControlManager acM, String path, Principal principal) throws RepositoryException,
            AccessDeniedException, NotExecutableException {
        // try applicable (new) ACLs first
        AccessControlPolicyIterator itr = acM.getApplicablePolicies(path);
        while (itr.hasNext()) {
            AccessControlPolicy policy = itr.nextAccessControlPolicy();
            if (policy instanceof ACLTemplate) {
                return (ACLTemplate) policy;
            }
        }
        // try if there is an acl that has been set before:
        AccessControlPolicy[] pcls = acM.getPolicies(path);
        for (AccessControlPolicy policy : pcls) {
            if (policy instanceof ACLTemplate) {
                return (ACLTemplate) policy;
            }
        }
        // no applicable or existing ACLTemplate to edit -> not executable.
        throw new NotExecutableException();
    }

    private ACLTemplate modifyPrivileges(String path, Principal principal, Privilege[] privileges, boolean isAllow) throws NotExecutableException, RepositoryException {
        ACLTemplate tmpl = getPolicy(acMgr, path, principal);
        tmpl.addEntry(principal, privileges, isAllow);

        acMgr.setPolicy(tmpl.getPath(), tmpl);
        superuser.save();

        return tmpl;
    }

    private static void verifyACEs(AccessControlPolicy[] policies,
                                   String policyPath, int numberOfAce) throws RepositoryException {
        JackrabbitAccessControlList acl = null;
        for (AccessControlPolicy p : policies) {
            if (p instanceof JackrabbitAccessControlList) {
                if (policyPath.equals(((JackrabbitAccessControlList) p).getPath())) {
                    acl = (JackrabbitAccessControlList) p;
                }
            }
        }

        if (acl == null) {
            fail("No Jackrabbit ACL found at " + policyPath);
        } else {
            assertEquals(numberOfAce, acl.getAccessControlEntries().length);
        }
    }

    public void testCache() throws Exception {

        // --- test1 : add an ACE at path --------------------------------------
        modifyPrivileges(path, testGroup.getPrincipal(), privilegesFromName(Privilege.JCR_READ), true);
        AccessControlPolicy[] plcs = acMgr.getEffectivePolicies(path);
        AccessControlPolicy[] plcs2 = acMgr.getEffectivePolicies(childNPath);
        // effective policies must be the equal on path and childPath
        assertTrue(Arrays.equals(plcs, plcs2));
        // the policy at 'path' must contain a single ACE
        verifyACEs(plcs2, path, 1);

        // --- test2: modify the policy at 'path' ------------------------------
        modifyPrivileges(path, testGroup.getPrincipal(), privilegesFromName(Privilege.JCR_WRITE), false);
        plcs = acMgr.getEffectivePolicies(path);
        plcs2 = acMgr.getEffectivePolicies(childNPath);
        // effective policies must be the equal on path and childNPath
        assertTrue(Arrays.equals(plcs, plcs2));
        verifyACEs(plcs2, path, 2);

        // --- test3: add an policy at childNPath ------------------------------
        modifyPrivileges(childNPath, testGroup.getPrincipal(), privilegesFromName(Privilege.JCR_ADD_CHILD_NODES), true);
        plcs = acMgr.getEffectivePolicies(path);
        plcs2 = acMgr.getEffectivePolicies(childNPath);
        assertFalse(Arrays.equals(plcs, plcs2));
        verifyACEs(plcs2, path, 2);
        verifyACEs(plcs2, childNPath, 1);

        // --- test4: modify policy at childNPath ------------------------------
        modifyPrivileges(childNPath, testGroup.getPrincipal(), privilegesFromName(Privilege.JCR_REMOVE_CHILD_NODES), true);
        plcs = acMgr.getEffectivePolicies(path);
        plcs2 = acMgr.getEffectivePolicies(childNPath);
        assertFalse(Arrays.equals(plcs, plcs2));
        verifyACEs(plcs2, path, 2);
        // still a single ACE at childNPath. but privileges must be adjusted
        verifyACEs(plcs2, childNPath, 1);
        AccessControlList acl = null;
        for (AccessControlPolicy p : plcs2) {
            if (p instanceof JackrabbitAccessControlList && childNPath.equals(((JackrabbitAccessControlList) p).getPath())) {
                acl = (AccessControlList) p;
            }
        }
        Privilege[] privs = privilegesFromNames(new String[] {Privilege.JCR_ADD_CHILD_NODES, Privilege.JCR_REMOVE_CHILD_NODES});
        assertEquals(privs, acl.getAccessControlEntries()[0].getPrivileges());

        // --- test4: remove policy at childNPath ------------------------------
        acMgr.removePolicy(childNPath, acMgr.getPolicies(childNPath)[0]);
        superuser.save();
        
        plcs = acMgr.getEffectivePolicies(path);
        AccessControlPolicy[] plcs3 = acMgr.getEffectivePolicies(childNPath);

        assertTrue(Arrays.equals(plcs, plcs3));
        assertFalse(Arrays.equals(plcs2, plcs3));

        for (AccessControlPolicy p : plcs3) {
            if (p instanceof JackrabbitAccessControlList) {
                if (childNPath.equals(((JackrabbitAccessControlList) p).getPath())) {
                    fail("Policy at path has been removed.");
                }
            }
        }
        verifyACEs(plcs, path, 2);
    }

    /**
     * Asserts that the given privilege sets are equal, regardless of ordering.
     */
    private void assertEquals(Privilege[] expected, Privilege[] actual) {
        assertEquals(getPrivilegeNames(expected), getPrivilegeNames(actual));
    }

    private Set<String> getPrivilegeNames(Privilege[] privileges) {
        Set<String> names = new HashSet<String>();
        for (Privilege privilege : privileges) {
            names.add(privilege.getName());
        }
        return names;
    }

    public void testPermissions() throws Exception {
        Session superuser2 = getHelper().getSuperuserSession();
        try {
            JackrabbitAccessControlManager acM = (JackrabbitAccessControlManager) acMgr;
            JackrabbitAccessControlManager acM2 = (JackrabbitAccessControlManager) superuser2.getAccessControlManager();
            Set<Principal> principals = Collections.singleton(testGroup.getPrincipal());

            // --- test1 : add an ACE at path ----------------------------------
            Privilege[] privs = privilegesFromName(Privilege.JCR_LOCK_MANAGEMENT);
            modifyPrivileges(path, testGroup.getPrincipal(), privs, true);

            assertTrue(acM.hasPrivileges(path, principals, privs));
            assertTrue(acM2.hasPrivileges(path, principals, privs));

            assertTrue(acM.hasPrivileges(childNPath, principals, privs));
            assertTrue(acM2.hasPrivileges(childNPath, principals, privs));

            // --- test2: modify the policy at 'path' ------------------------------
            modifyPrivileges(path, testGroup.getPrincipal(), privilegesFromName(Privilege.JCR_WRITE), true);

            privs = privilegesFromNames(new String[] {
                    Privilege.JCR_LOCK_MANAGEMENT,
                    Privilege.JCR_WRITE});
            assertTrue(acM.hasPrivileges(path, principals, privs));
            assertTrue(acM2.hasPrivileges(path, principals, privs));

            assertTrue(acM.hasPrivileges(childNPath, principals, privs));
            assertTrue(acM2.hasPrivileges(childNPath, principals, privs));

            // --- test3: add an policy at childNPath ------------------------------
            modifyPrivileges(childNPath, testGroup.getPrincipal(),
                    privilegesFromName(Privilege.JCR_ADD_CHILD_NODES), false);

            privs = privilegesFromNames(new String[] {
                    Privilege.JCR_LOCK_MANAGEMENT,
                    Privilege.JCR_WRITE});
            assertTrue(acM.hasPrivileges(path, principals, privs));
            assertTrue(acM2.hasPrivileges(path, principals, privs));

            privs = privilegesFromNames(new String[] {
                    Privilege.JCR_LOCK_MANAGEMENT, 
                    Privilege.JCR_MODIFY_PROPERTIES,
                    Privilege.JCR_REMOVE_CHILD_NODES,
                    Privilege.JCR_REMOVE_NODE});
            assertTrue(acM.hasPrivileges(childNPath, principals, privs));
            assertTrue(acM2.hasPrivileges(childNPath, principals, privs));


            // --- test4: modify policy at childNPath --------------------------
            modifyPrivileges(childNPath, testGroup.getPrincipal(),
                    privilegesFromName(Privilege.JCR_REMOVE_CHILD_NODES), false);

            privs = privilegesFromNames(new String[] {
                    Privilege.JCR_LOCK_MANAGEMENT,
                    Privilege.JCR_WRITE});
            assertTrue(acM.hasPrivileges(path, principals, privs));
            assertTrue(acM2.hasPrivileges(path, principals, privs));

            privs = privilegesFromNames(new String[] {
                    Privilege.JCR_LOCK_MANAGEMENT,
                    Privilege.JCR_MODIFY_PROPERTIES,
                    Privilege.JCR_REMOVE_NODE});
            assertTrue(acM.hasPrivileges(childNPath, principals, privs));
            assertTrue(acM2.hasPrivileges(childNPath, principals, privs));

            // --- test4: remove policy at childNPath --------------------------
            acMgr.removePolicy(childNPath, acMgr.getPolicies(childNPath)[0]);
            superuser.save();

            privs = privilegesFromNames(new String[] {
                    Privilege.JCR_LOCK_MANAGEMENT,
                    Privilege.JCR_WRITE});
            
            assertTrue(acM.hasPrivileges(path, principals, privs));
            assertTrue(acM2.hasPrivileges(path, principals, privs));

            assertTrue(acM.hasPrivileges(childNPath, principals, privs));
            assertTrue(acM2.hasPrivileges(childNPath, principals, privs));
            
        } finally {
            superuser2.logout();
        }
    }

    static interface TestInvokation {
        public void runTest() throws Exception;
    }

    private void runTestUnderLoad(TestInvokation ti) throws Exception {

        JcrTestThread t[] = new JcrTestThread[4];

        for (int i = 0; i < t.length; i++) {
            t[i] = new JcrTestThread();
        }

        try {
            for (int i = 0; i < t.length; i++) {
                t[i].start();
            }
            ti.runTest();
        }
        finally {
            for (int i = 0; i < t.length; i++) {
                t[i].stopMe();
                t[i].join();
                Throwable th = t[i].getLastExc();
                if (th != null) {
                    fail("failure in load thread: " + th);
                }
            }
        }
    }

    public void testCacheUnderLoad() throws Exception {
        runTestUnderLoad(new TestInvokation() {
            public void runTest() throws Exception {
                testCache();
            }
        });
    }

    public void testPermissionsUnderLoad() throws Exception {
        runTestUnderLoad(new TestInvokation() {
            public void runTest() throws Exception {
                testPermissions();
            }
        });
    }

    /**
     * Test code that that walks the repository.
     */
    private class JcrTestThread extends Thread {

        private boolean stopme = false;
        private Throwable lastErr;

        @Override
        public void run() {
            while (!this.stopme) {
                Session session = null;
                try {
                    session = getHelper().getReadOnlySession();
                    walk(session.getRootNode());
                }
                catch (RepositoryException ex) {
                    // ignored
                } catch (Throwable ex) {
                    lastErr = ex;
                }
                finally {
                    if (session != null) {
                        session.logout();
                        session = null;
                    }
                }
            }
        }

        public void stopMe() {
            this.stopme = true;
        }

        public Throwable getLastExc() {
            return lastErr;
        }

        private void walk(Node node) {
            if (stopme) {
                return;
            }

            try {
                if ("/jcr:system".equals(node.getPath())) {
                    // do not descend into an non-interesting subtree
                    return;
                }

                NodeIterator ni = node.getNodes();
                while (ni.hasNext()) {
                    walk(ni.nextNode());
                }
            } catch (RepositoryException ex) {
                // ignore
            } catch (Throwable ex) {
                lastErr = ex;
            }
        }
   }
}