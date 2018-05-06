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

import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.authorization.AbstractWriteTest;
import org.apache.jackrabbit.core.security.authorization.AccessControlConstants;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.core.security.principal.EveryonePrincipal;
import org.apache.jackrabbit.core.security.TestPrincipal;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.util.Text;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;
import javax.jcr.security.AccessControlEntry;
import java.security.Principal;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * <code>EvaluationTest</code>...
 */
public class WriteTest extends AbstractWriteTest {

    @Override
    protected boolean isExecutable() {
        return EvaluationUtil.isExecutable(acMgr);
    }

    @Override
    protected JackrabbitAccessControlList getPolicy(AccessControlManager acM, String path, Principal principal) throws RepositoryException, AccessDeniedException, NotExecutableException {
        return EvaluationUtil.getPolicy(acM, path, principal);
    }

    @Override
    protected Map<String, Value> getRestrictions(Session s, String path) {
        return Collections.emptyMap();
    }

    public void testAccessControlModification2() throws RepositoryException, NotExecutableException {
        /*
         precondition:
         testuser must have READ-only permission on test-node and below
        */
        checkReadOnly(path);

        // give 'testUser' READ_AC|MODIFY_AC privileges at 'path'
        Privilege[] privileges = privilegesFromNames(new String[] {
                Privilege.JCR_READ_ACCESS_CONTROL,
                Privilege.JCR_MODIFY_ACCESS_CONTROL
        });
        JackrabbitAccessControlList tmpl = givePrivileges(path, privileges, getRestrictions(superuser, path));
        /*
         testuser must
         - still have the inherited READ permission.
         - must have permission to view AC items at 'path' (and below)
         - must have permission to modify AC items at 'path'

         testuser must not have
         - permission to view AC items outside of the tree defined by path.
        */

        // make sure the 'rep:policy' node has been created.
        assertTrue(superuser.itemExists(tmpl.getPath() + "/rep:policy"));

        Session testSession = getTestSession();
        AccessControlManager testAcMgr = getTestACManager();
        // test: MODIFY_AC granted at 'path'
        assertTrue(testAcMgr.hasPrivileges(path, privilegesFromName(Privilege.JCR_MODIFY_ACCESS_CONTROL)));

        // test if testuser can READ access control on the path and on the
        // entire subtree that gets the policy inherited.
        AccessControlPolicy[] policies = testAcMgr.getPolicies(path);
        testAcMgr.getPolicies(childNPath);

        // test: READ_AC privilege does not apply outside of the tree.
        try {
            testAcMgr.getPolicies(siblingPath);
            fail("READ_AC privilege must not apply outside of the tree it has applied to.");
        } catch (AccessDeniedException e) {
            // success
        }

        // test: MODIFY_AC privilege does not apply outside of the tree.
        try {
            testAcMgr.setPolicy(siblingPath, policies[0]);
            fail("MODIFY_AC privilege must not apply outside of the tree it has applied to.");
        } catch (AccessDeniedException e) {
            // success
        }

        // test if testuser can modify AC-items
        // 1) add an ac-entry
        ACLTemplate acl = (ACLTemplate) policies[0];
        acl.addAccessControlEntry(testUser.getPrincipal(), privilegesFromName(PrivilegeRegistry.REP_WRITE));
        testAcMgr.setPolicy(path, acl);
        testSession.save();

        assertTrue(testAcMgr.hasPrivileges(path,
                privilegesFromName(Privilege.JCR_REMOVE_CHILD_NODES)));

        // 2) remove the policy
        testAcMgr.removePolicy(path, policies[0]);
        testSession.save();

        // Finally: testuser removed the policy that granted him permission
        // to modify the AC content. Since testuser removed the policy, it's
        // privileges must be gone again...
        try {
            testAcMgr.getEffectivePolicies(childNPath);
            fail("READ_AC privilege has been revoked -> must throw again.");
        } catch (AccessDeniedException e) {
            // success
        }
        // ... and since the ACE is stored with the policy all right except
        // READ must be gone.
        checkReadOnly(path);
    }

    public void testRemovePermission9() throws NotExecutableException, RepositoryException {
        AccessControlManager testAcMgr = getTestACManager();
        /*
          precondition:
          testuser must have READ-only permission on test-node and below
        */
        checkReadOnly(path);
        checkReadOnly(childNPath);

        Privilege[] rmChildNodes = privilegesFromName(Privilege.JCR_REMOVE_CHILD_NODES);
        Privilege[] rmNode = privilegesFromName(Privilege.JCR_REMOVE_NODE);

        // add 'remove_child_nodes' at 'path and allow 'remove_node' at childNPath
        givePrivileges(path, rmChildNodes, getRestrictions(superuser, path));
        givePrivileges(childNPath, rmNode, getRestrictions(superuser, childNPath));
        /*
         expected result:
         - rep:policy node can still not be remove for it is access-control
           content that requires jcr:modifyAccessControl privilege instead.
         */
        String policyPath = childNPath + "/rep:policy";
        assertFalse(getTestSession().hasPermission(policyPath, javax.jcr.Session.ACTION_REMOVE));
        assertTrue(testAcMgr.hasPrivileges(policyPath, new Privilege[] {rmChildNodes[0], rmNode[0]}));
    }

    public void testApplicablePolicies() throws RepositoryException {
        AccessControlPolicyIterator it = acMgr.getApplicablePolicies(childNPath);
        assertTrue(it.hasNext());

        // the same should be true, if the rep:AccessControllable mixin has
        // been manually added
        Node n = (Node) superuser.getItem(childNPath);
        n.addMixin(((SessionImpl) superuser).getJCRName(AccessControlConstants.NT_REP_ACCESS_CONTROLLABLE));
        it = acMgr.getApplicablePolicies(childNPath);
        assertTrue(it.hasNext());
    }

    public void testInheritance2() throws RepositoryException, NotExecutableException {
        Session testSession = getTestSession();
        AccessControlManager testAcMgr = getTestACManager();

        /*
          precondition:
          testuser must have READ-only permission on test-node and below
        */
        checkReadOnly(path);
        checkReadOnly(childNPath);

        // give jcr:write privilege on 'path' and withdraw them on 'childNPath'
        Privilege[] privileges = privilegesFromNames(new String[] {Privilege.JCR_WRITE});
        givePrivileges(path, privileges, getRestrictions(superuser, path));
        withdrawPrivileges(childNPath, privileges, getRestrictions(superuser, path));

        /*
        since evaluation respects inheritance through the node
        hierarchy, the jcr:write privilege must not be granted at childNPath
        */
        assertFalse(testAcMgr.hasPrivileges(childNPath, privileges));

        /*
         ... same for permissions at 'childNPath'
         */
        String actions = Session.ACTION_SET_PROPERTY + "," + Session.ACTION_REMOVE + "," + Session.ACTION_ADD_NODE;

        String nonExistingItemPath = childNPath + "/anyItem";
        assertFalse(testSession.hasPermission(nonExistingItemPath, actions));

        // yet another level in the hierarchy
        Node grandChild = superuser.getNode(childNPath).addNode(nodeName3);
        superuser.save();
        String gcPath = grandChild.getPath();

        // grant write privilege again
        givePrivileges(gcPath, privileges, getRestrictions(superuser, path));
        assertTrue(testAcMgr.hasPrivileges(gcPath, privileges));
        assertTrue(testSession.hasPermission(gcPath + "/anyProp", Session.ACTION_SET_PROPERTY));
        // however: removing the grand-child nodes must not be allowed as
        // remove_child_node privilege is missing on the direct ancestor.
        assertFalse(testSession.hasPermission(gcPath, Session.ACTION_REMOVE));
    }

    public void testInheritedGroupPermissions() throws NotExecutableException, RepositoryException {
        Group testGroup = getTestGroup();
        AccessControlManager testAcMgr = getTestACManager();
        /*
         precondition:
         testuser must have READ-only permission on test-node and below
        */
        checkReadOnly(path);

        Privilege[] privileges = privilegesFromName(Privilege.JCR_MODIFY_PROPERTIES);

        /* give MODIFY_PROPERTIES privilege for testGroup at 'path' */
        givePrivileges(path, testGroup.getPrincipal(), privileges, getRestrictions(superuser, path));
        /*
         withdraw MODIFY_PROPERTIES privilege for everyone at 'childNPath'
         */
        withdrawPrivileges(childNPath, EveryonePrincipal.getInstance(), privileges, getRestrictions(superuser, path));

        // result at 'child path' must be deny
        assertFalse(testAcMgr.hasPrivileges(childNPath, privilegesFromName(Privilege.JCR_MODIFY_PROPERTIES)));
    }

    public void testInheritedGroupPermissions2() throws NotExecutableException, RepositoryException {
        Group testGroup = getTestGroup();
        AccessControlManager testAcMgr = getTestACManager();
        /*
         precondition:
         testuser must have READ-only permission on test-node and below
        */
        checkReadOnly(path);

        Privilege[] privileges = privilegesFromName(Privilege.JCR_MODIFY_PROPERTIES);

        // NOTE: same as testInheritedGroupPermissions above but using
        // everyone on path, testgroup on childpath -> result must be the same

        /* give MODIFY_PROPERTIES privilege for everyone at 'path' */
        givePrivileges(path, EveryonePrincipal.getInstance(), privileges, getRestrictions(superuser, path));
        /*
         withdraw MODIFY_PROPERTIES privilege for testGroup at 'childNPath'
         */
        withdrawPrivileges(childNPath, testGroup.getPrincipal(), privileges, getRestrictions(superuser, path));

        // result at 'child path' must be deny
        assertFalse(testAcMgr.hasPrivileges(childNPath, privilegesFromName(Privilege.JCR_MODIFY_PROPERTIES)));
    }

    public void testMultipleGroupPermissionsOnNode() throws NotExecutableException, RepositoryException {
        Group testGroup = getTestGroup();

        /* create a second group the test user is member of */
        Principal principal = new TestPrincipal("testGroup" + UUID.randomUUID());
        UserManager umgr = getUserManager(superuser);
        Group group2 = umgr.createGroup(principal);
        try {
            group2.addMember(testUser);
            if (!umgr.isAutoSave() && superuser.hasPendingChanges()) {
                superuser.save();
            }

            /* add privileges for the Group the test-user is member of */
            Privilege[] privileges = privilegesFromName(Privilege.JCR_MODIFY_PROPERTIES);
            givePrivileges(path, testGroup.getPrincipal(), privileges, getRestrictions(superuser, path));

            withdrawPrivileges(path, group2.getPrincipal(), privileges, getRestrictions(superuser, path));

            /*
             testuser must get the permissions/privileges inherited from
             the group it is member of.
             the denial of group2 must succeed
            */
            String actions = javax.jcr.Session.ACTION_SET_PROPERTY + "," + javax.jcr.Session.ACTION_READ;

            AccessControlManager testAcMgr = getTestACManager();

            assertFalse(getTestSession().hasPermission(path, actions));
            Privilege[] privs = privilegesFromName(Privilege.JCR_MODIFY_PROPERTIES);
            assertFalse(testAcMgr.hasPrivileges(path, privs));
        } finally {
            group2.remove();
        }
    }

    public void testMultipleGroupPermissionsOnNode2() throws NotExecutableException, RepositoryException {
        Group testGroup = getTestGroup();

        /* create a second group the test user is member of */
        Principal principal = new TestPrincipal("testGroup" + UUID.randomUUID());
        UserManager umgr = getUserManager(superuser);
        Group group2 = umgr.createGroup(principal);

        try {
            group2.addMember(testUser);
            if (!umgr.isAutoSave() && superuser.hasPendingChanges()) {
                superuser.save();
            }

            /* add privileges for the Group the test-user is member of */
            Privilege[] privileges = privilegesFromName(Privilege.JCR_MODIFY_PROPERTIES);
            withdrawPrivileges(path, testGroup.getPrincipal(), privileges, getRestrictions(superuser, path));

            givePrivileges(path, group2.getPrincipal(), privileges, getRestrictions(superuser, path));

            /*
             testuser must get the permissions/privileges inherited from
             the group it is member of.
             granting permissions for group2 must be effective
            */
            String actions = javax.jcr.Session.ACTION_SET_PROPERTY + "," + javax.jcr.Session.ACTION_READ;

            AccessControlManager testAcMgr = getTestACManager();
            assertTrue(getTestSession().hasPermission(path, actions));
            Privilege[] privs = privilegesFromName(Privilege.JCR_MODIFY_PROPERTIES);
            assertTrue(testAcMgr.hasPrivileges(path, privs));
        } finally {
            group2.remove();
        }
    }

    public void testReorderGroupPermissions() throws NotExecutableException, RepositoryException {
        Group testGroup = getTestGroup();

        /* create a second group the test user is member of */
        Principal principal = new TestPrincipal("testGroup" + UUID.randomUUID());
        UserManager umgr = getUserManager(superuser);
        Group group2 = umgr.createGroup(principal);

        try {
            group2.addMember(testUser);
            if (!umgr.isAutoSave() && superuser.hasPendingChanges()) {
                superuser.save();
            }

            /* add privileges for the Group the test-user is member of */
            Privilege[] privileges = privilegesFromName(Privilege.JCR_MODIFY_PROPERTIES);
            withdrawPrivileges(path, testGroup.getPrincipal(), privileges, getRestrictions(superuser, path));
            givePrivileges(path, group2.getPrincipal(), privileges, getRestrictions(superuser, path));

            /*
             testuser must get the permissions/privileges inherited from
             the group it is member of.
             granting permissions for group2 must be effective
            */
            String actions = javax.jcr.Session.ACTION_SET_PROPERTY + "," + javax.jcr.Session.ACTION_READ;

            AccessControlManager testAcMgr = getTestACManager();
            assertTrue(getTestSession().hasPermission(path, actions));
            Privilege[] privs = privilegesFromName(Privilege.JCR_MODIFY_PROPERTIES);
            assertTrue(testAcMgr.hasPrivileges(path, privs));

            // reorder the ACEs
            AccessControlEntry srcEntry = null;
            AccessControlEntry destEntry = null;
            JackrabbitAccessControlList acl = (JackrabbitAccessControlList) acMgr.getPolicies(path)[0];
            for (AccessControlEntry entry : acl.getAccessControlEntries()) {
                Principal princ = entry.getPrincipal();
                if (testGroup.getPrincipal().equals(princ)) {
                    destEntry = entry;
                } else if (group2.getPrincipal().equals(princ)) {
                    srcEntry = entry;
                }

            }

            acl.orderBefore(srcEntry, destEntry);
            acMgr.setPolicy(path, acl);
            superuser.save();

            /* after reordering the permissions must be denied */
            assertFalse(getTestSession().hasPermission(path, actions));
            assertFalse(testAcMgr.hasPrivileges(path, privs));

        } finally {
            group2.remove();
        }
    }

    public void testWriteIfReadingParentIsDenied() throws Exception {
        Privilege[] privileges = privilegesFromNames(new String[] {Privilege.JCR_READ, Privilege.JCR_WRITE});

        /* deny READ/WRITE privilege for testUser at 'path' */
        withdrawPrivileges(path, testUser.getPrincipal(), privileges, getRestrictions(superuser, path));
        /*
        allow READ/WRITE privilege for testUser at 'childNPath'
        */
        givePrivileges(childNPath, testUser.getPrincipal(), privileges, getRestrictions(superuser, childNPath));


        Session testSession = getTestSession();

        assertFalse(testSession.nodeExists(path));

        // reading the node and it's definition must succeed.
        assertTrue(testSession.nodeExists(childNPath));
        Node n = testSession.getNode(childNPath);

        n.addNode("someChild");
        n.save();
    }

    public void testRemoveNodeWithPolicy() throws Exception {
        Privilege[] privileges = privilegesFromNames(new String[] {Privilege.JCR_READ, Privilege.JCR_WRITE});

        /* allow READ/WRITE privilege for testUser at 'path' */
        givePrivileges(path, testUser.getPrincipal(), privileges, getRestrictions(superuser, path));
        /* allow READ/WRITE privilege for testUser at 'childPath' */
        givePrivileges(childNPath, testUser.getPrincipal(), privileges, getRestrictions(superuser, path));

        Session testSession = getTestSession();

        assertTrue(testSession.nodeExists(childNPath));
        assertTrue(testSession.hasPermission(childNPath, Session.ACTION_REMOVE));

        Node n = testSession.getNode(childNPath);

        // removing the child node must succeed as both remove-node and
        // remove-child-nodes are granted to testsession.
        // the policy node underneath childNPath should silently be removed
        // as the editing session has no knowledge about it's existence.
        n.remove();
        testSession.save();
    }

    public void testRemoveNodeWithInvisibleChild() throws Exception {
        Privilege[] privileges = privilegesFromNames(new String[] {Privilege.JCR_READ, Privilege.JCR_WRITE});

        Node invisible = superuser.getNode(childNPath).addNode(nodeName3);
        superuser.save();

        /* allow READ/WRITE privilege for testUser at 'path' */
        givePrivileges(path, testUser.getPrincipal(), privileges, getRestrictions(superuser, path));
        /* deny READ privilege at invisible node. (removal is still granted) */
        withdrawPrivileges(invisible.getPath(), testUser.getPrincipal(),
                privilegesFromNames(new String[] {Privilege.JCR_READ}),
                getRestrictions(superuser, path));

        Session testSession = getTestSession();

        assertTrue(testSession.nodeExists(childNPath));
        assertTrue(testSession.hasPermission(childNPath, Session.ACTION_REMOVE));

        Node n = testSession.getNode(childNPath);

        // removing the child node must succeed as both remove-node and
        // remove-child-nodes are granted to testsession.
        // the policy node underneath childNPath should silently be removed
        // as the editing session has no knowledge about it's existence.
        n.remove();
        testSession.save();
    }

    public void testRemoveNodeWithInvisibleNonRemovableChild() throws Exception {
        Privilege[] privileges = privilegesFromNames(new String[] {Privilege.JCR_READ, Privilege.JCR_WRITE});

        Node invisible = superuser.getNode(childNPath).addNode(nodeName3);
        superuser.save();

        /* allow READ/WRITE privilege for testUser at 'path' */
        givePrivileges(path, testUser.getPrincipal(), privileges, getRestrictions(superuser, path));
        /* deny READ privilege at invisible node. (removal is still granted) */
        withdrawPrivileges(invisible.getPath(), testUser.getPrincipal(),
                privileges,
                getRestrictions(superuser, path));

        Session testSession = getTestSession();

        assertTrue(testSession.nodeExists(childNPath));
        assertTrue(testSession.hasPermission(childNPath, Session.ACTION_REMOVE));

        Node n = testSession.getNode(childNPath);

        // removing the child node must fail as a hidden child node cannot
        // be removed.
        try {
            n.remove();
            testSession.save();
            fail();
        } catch (AccessDeniedException e) {
            // success
        }
    }
    
    // https://issues.apache.org/jira/browse/JCR-3131
    public void testEmptySaveNoRootAccess() throws RepositoryException, NotExecutableException {

        Session s = getTestSession();
        s.save();

        Privilege[] read = privilegesFromName(Privilege.JCR_READ);

        try {
            JackrabbitAccessControlList tmpl = getPolicy(acMgr, "/", testUser.getPrincipal());
            tmpl.addEntry(testUser.getPrincipal(), read, false, getRestrictions(superuser, path));
            acMgr.setPolicy(tmpl.getPath(), tmpl);
            superuser.save();

            // empty save operation
            s.save();
        }
        finally {
            // undo revocation of read privilege
            JackrabbitAccessControlList tmpl = getPolicy(acMgr, "/", testUser.getPrincipal());
            tmpl.addEntry(testUser.getPrincipal(), read, true, getRestrictions(superuser, path));
            acMgr.setPolicy(tmpl.getPath(), tmpl);
            superuser.save();
        }
    }

    public void testReorderPolicyNode() throws RepositoryException, NotExecutableException {
        Session testSession = getTestSession();
        Node n = testSession.getNode(path);
        try {
            if (!n.getPrimaryNodeType().hasOrderableChildNodes()) {
                throw new NotExecutableException("Reordering child nodes is not supported..");
            }

            n.orderBefore(Text.getName(childNPath), Text.getName(childNPath2));
            testSession.save();
            fail("test session must not be allowed to reorder nodes.");
        } catch (AccessDeniedException e) {
            // success.
        }

        // grant all privileges
        givePrivileges(path, privilegesFromNames(new String[] {Privilege.JCR_ALL}), getRestrictions(superuser, path));

        n.orderBefore("rep:policy", Text.getName(childNPath2));
        testSession.save();
    }
}