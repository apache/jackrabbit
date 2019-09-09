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

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.core.security.authorization.AbstractEvaluationTest;
import org.apache.jackrabbit.core.security.authorization.AccessControlConstants;
import org.apache.jackrabbit.test.NotExecutableException;
import org.junit.Test;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * <code>ReadTest</code>...
 */
public class ReadTest extends AbstractEvaluationTest {

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
    }

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

    public void testReadDenied() throws Exception {
        Privilege[] privileges = privilegesFromName(Privilege.JCR_READ);

        /* deny READ privilege for testUser at 'path' */
        withdrawPrivileges(path, privileges, getRestrictions(superuser, path));
        /*
         allow READ privilege for testUser at 'childNPath'
         */
        givePrivileges(childNPath, privileges, getRestrictions(superuser, childNPath));


        Session testSession = getTestSession();

        assertFalse(testSession.nodeExists(path));
        assertTrue(testSession.nodeExists(childNPath));
        Node n = testSession.getNode(childNPath);
        n.getDefinition();
    }

    public void testDenyUserAllowGroup() throws Exception {
        Privilege[] privileges = privilegesFromName(Privilege.JCR_READ);
        Principal group = getTestGroup().getPrincipal();

        /*
         deny READ privilege for testUser at 'path'
         */
        withdrawPrivileges(path, testUser.getPrincipal(), privileges, getRestrictions(superuser, path));
        /*
         allow READ privilege for group at 'path'
         */
        givePrivileges(path, group, privileges, getRestrictions(superuser, path));

        Session testSession = getTestSession();
        assertFalse(testSession.nodeExists(path));
    }

    public void testAllowGroupDenyUser() throws Exception {
        Privilege[] privileges = privilegesFromName(Privilege.JCR_READ);
        Principal group = getTestGroup().getPrincipal();

        /*
        allow READ privilege for group at 'path'
        */
        givePrivileges(path, group, privileges, getRestrictions(superuser, path));
        /*
        deny READ privilege for testUser at 'path'
        */
        withdrawPrivileges(path, testUser.getPrincipal(), privileges, getRestrictions(superuser, path));

        Session testSession = getTestSession();
        assertFalse(testSession.nodeExists(path));
    }

    public void testAllowUserDenyGroup() throws Exception {
        Privilege[] privileges = privilegesFromName(Privilege.JCR_READ);
        Principal group = getTestGroup().getPrincipal();

        /*
         allow READ privilege for testUser at 'path'
         */
        givePrivileges(path, testUser.getPrincipal(), privileges, getRestrictions(superuser, path));
        /*
         deny READ privilege for group at 'path'
         */
        withdrawPrivileges(path, group, privileges, getRestrictions(superuser, path));

        Session testSession = getTestSession();
        assertTrue(testSession.nodeExists(path));
    }

    public void testDenyGroupAllowUser() throws Exception {
        Privilege[] privileges = privilegesFromName(Privilege.JCR_READ);
        Principal group = getTestGroup().getPrincipal();

        /*
         deny READ privilege for group at 'path'
         */
        withdrawPrivileges(path, group, privileges, getRestrictions(superuser, path));

        /*
         allow READ privilege for testUser at 'path'
         */
        givePrivileges(path, testUser.getPrincipal(), privileges, getRestrictions(superuser, path));

        Session testSession = getTestSession();
        assertTrue(testSession.nodeExists(path));
    }

    public void testDenyGroupAllowEveryone() throws Exception {
        Privilege[] privileges = privilegesFromName(Privilege.JCR_READ);
        Principal group = getTestGroup().getPrincipal();
        Principal everyone = ((JackrabbitSession) superuser).getPrincipalManager().getEveryone();

        /*
         deny READ privilege for group at 'path'
         */
        withdrawPrivileges(path, group, privileges, getRestrictions(superuser, path));

        /*
         allow READ privilege for everyone at 'path'
         */
        givePrivileges(path, everyone, privileges, getRestrictions(superuser, path));

        Session testSession = getTestSession();
        assertTrue(testSession.nodeExists(path));
    }

    public void testAllowEveryoneDenyGroup() throws Exception {
        Privilege[] privileges = privilegesFromName(Privilege.JCR_READ);
        Principal group = getTestGroup().getPrincipal();
        Principal everyone = ((JackrabbitSession) superuser).getPrincipalManager().getEveryone();

        /*
         allow READ privilege for everyone at 'path'
         */
        givePrivileges(path, everyone, privileges, getRestrictions(superuser, path));

        /*
         deny READ privilege for group at 'path'
         */
        withdrawPrivileges(path, group, privileges, getRestrictions(superuser, path));

        Session testSession = getTestSession();
        assertFalse(testSession.nodeExists(path));
    }

    public void testDenyGroupPathAllowEveryoneChildPath() throws Exception {
        Privilege[] privileges = privilegesFromName(Privilege.JCR_READ);
        Principal group = getTestGroup().getPrincipal();
        Principal everyone = ((JackrabbitSession) superuser).getPrincipalManager().getEveryone();

        /*
         deny READ privilege for group at 'path'
         */
        withdrawPrivileges(path, group, privileges, getRestrictions(superuser, path));

        /*
         allow READ privilege for everyone at 'childNPath'
         */
        givePrivileges(path, everyone, privileges, getRestrictions(superuser, childNPath));

        Session testSession = getTestSession();
        assertTrue(testSession.nodeExists(childNPath));
    }

    public void testAllowEveryonePathDenyGroupChildPath() throws Exception {
        Privilege[] privileges = privilegesFromName(Privilege.JCR_READ);
        Principal group = getTestGroup().getPrincipal();
        Principal everyone = ((JackrabbitSession) superuser).getPrincipalManager().getEveryone();

        /*
         allow READ privilege for everyone at 'path'
         */
        givePrivileges(path, everyone, privileges, getRestrictions(superuser, path));

        /*
         deny READ privilege for group at 'childNPath'
         */
        withdrawPrivileges(path, group, privileges, getRestrictions(superuser, childNPath));

        Session testSession = getTestSession();
        assertFalse(testSession.nodeExists(childNPath));
    }

    public void testAllowUserPathDenyGroupChildPath() throws Exception {
        Privilege[] privileges = privilegesFromName(Privilege.JCR_READ);
        Principal group = getTestGroup().getPrincipal();

        /*
         allow READ privilege for testUser at 'path'
         */
        givePrivileges(path, testUser.getPrincipal(), privileges, getRestrictions(superuser, path));
        /*
         deny READ privilege for group at 'childPath'
         */
        withdrawPrivileges(path, group, privileges, getRestrictions(superuser, childNPath));

        Session testSession = getTestSession();
        assertTrue(testSession.nodeExists(childNPath));
    }

    public void testDenyGroupPathAllowUserChildPath() throws Exception {
        Privilege[] privileges = privilegesFromName(Privilege.JCR_READ);
        Principal group = getTestGroup().getPrincipal();

        /*
         deny READ privilege for group at 'path'
         */
        withdrawPrivileges(path, group, privileges, getRestrictions(superuser, path));

        /*
         allow READ privilege for testUser at 'childNPath'
         */
        givePrivileges(path, testUser.getPrincipal(), privileges, getRestrictions(superuser, childNPath));

        Session testSession = getTestSession();
        assertTrue(testSession.nodeExists(childNPath));
    }

    public void testDenyUserPathAllowGroupChildPath() throws Exception {
        Privilege[] privileges = privilegesFromName(Privilege.JCR_READ);
        Principal group = getTestGroup().getPrincipal();

        /*
         deny READ privilege for testUser at 'path'
         */
        withdrawPrivileges(path, testUser.getPrincipal(), privileges, getRestrictions(superuser, path));
        /*
         allow READ privilege for group at 'childNPath'
         */
        givePrivileges(path, group, privileges, getRestrictions(superuser, childNPath));

        Session testSession = getTestSession();
        assertFalse(testSession.nodeExists(childNPath));
    }

    public void testAllowGroupPathDenyUserChildPath() throws Exception {
        Privilege[] privileges = privilegesFromName(Privilege.JCR_READ);
        Principal group = getTestGroup().getPrincipal();

        /*
        allow READ privilege for everyone at 'path'
        */
        givePrivileges(path, group, privileges, getRestrictions(superuser, path));
        /*
        deny READ privilege for testUser at 'childNPath'
        */
        withdrawPrivileges(path, testUser.getPrincipal(), privileges, getRestrictions(superuser, childNPath));

        Session testSession = getTestSession();
        assertFalse(testSession.nodeExists(childNPath));
    }

    public void testGlobRestriction() throws Exception {
        Session testSession = getTestSession();
        AccessControlManager testAcMgr = getTestACManager();
        ValueFactory vf = superuser.getValueFactory();
        /*
          precondition:
          testuser must have READ-only permission on test-node and below
        */
        checkReadOnly(path);
        checkReadOnly(childNPath);

        Privilege[] read = privilegesFromName(Privilege.JCR_READ);

        Map<String, Value> restrictions = new HashMap<String, Value>(getRestrictions(superuser, path));
        restrictions.put(AccessControlConstants.P_GLOB.toString(), vf.createValue("*/"+jcrPrimaryType));

        withdrawPrivileges(path, read, restrictions);

        assertTrue(testAcMgr.hasPrivileges(path, read));
        assertTrue(testSession.hasPermission(path, javax.jcr.Session.ACTION_READ));
        testSession.getNode(path);

        assertTrue(testAcMgr.hasPrivileges(childNPath, read));
        assertTrue(testSession.hasPermission(childNPath, javax.jcr.Session.ACTION_READ));
        testSession.getNode(childNPath);

        String propPath = path + "/" + jcrPrimaryType;
        assertFalse(testSession.hasPermission(propPath, javax.jcr.Session.ACTION_READ));
        assertFalse(testSession.propertyExists(propPath));

        propPath = childNPath + "/" + jcrPrimaryType;
        assertFalse(testSession.hasPermission(propPath, javax.jcr.Session.ACTION_READ));
        assertFalse(testSession.propertyExists(propPath));
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/OAK-2412">OAK-2412</a>
     */
    @Test
    public void testEmptyGlobRestriction()throws Exception{
        Node grandchild = superuser.getNode(childNPath).addNode("child");
        String ccPath = grandchild.getPath();
        superuser.save();

        // first deny access to 'path' (read-access is granted in the test setup)
        Privilege[] read = privilegesFromName(Privilege.JCR_READ);
        withdrawPrivileges(path, read, Collections.EMPTY_MAP);

        Session testSession = getTestSession();
        assertFalse(testSession.nodeExists(path));
        assertFalse(canGetNode(testSession, path));
        assertFalse(testSession.nodeExists(childNPath));
        assertFalse(canGetNode(testSession, childNPath));
        assertFalse(testSession.nodeExists(ccPath));
        assertFalse(canGetNode(testSession, ccPath));
        assertFalse(testSession.propertyExists(childNPath + '/' + JcrConstants.JCR_PRIMARYTYPE));

        Map<String, Value> emptyStringRestriction = new HashMap<String, Value>(getRestrictions(superuser, childNPath));
        emptyStringRestriction.put(AccessControlConstants.P_GLOB.toString(), vf.createValue(""));

        givePrivileges(childNPath, read, emptyStringRestriction);
        assertFalse(testSession.nodeExists(path));
        assertFalse(canGetNode(testSession, path));
        assertTrue(testSession.nodeExists(childNPath));
        assertTrue(canGetNode(testSession, childNPath));
        assertFalse(testSession.nodeExists(ccPath));
        assertFalse(canGetNode(testSession, ccPath));
        assertFalse(testSession.propertyExists(childNPath + '/' + JcrConstants.JCR_PRIMARYTYPE));

        givePrivileges(ccPath, read, Collections.EMPTY_MAP);
        assertTrue(testSession.nodeExists(ccPath));
        assertTrue(canGetNode(testSession, ccPath));
        assertTrue(testSession.propertyExists(ccPath + '/' + JcrConstants.JCR_PRIMARYTYPE));
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/OAK-2412">OAK-2412</a>
     */
    @Test
    public void testEmptyGlobRestriction2()throws Exception{
        Node grandchild = superuser.getNode(childNPath).addNode("child");
        String ccPath = grandchild.getPath();
        superuser.save();

        // first deny access to 'path' (read-access is granted in the test setup)
        Privilege[] read = privilegesFromName(Privilege.JCR_READ);
        withdrawPrivileges(path, read, Collections.EMPTY_MAP);

        Session testSession = getTestSession();
        assertFalse(testSession.nodeExists(path));
        assertFalse(canGetNode(testSession, path));
        assertFalse(testSession.nodeExists(childNPath));
        assertFalse(canGetNode(testSession, childNPath));
        assertFalse(testSession.nodeExists(ccPath));
        assertFalse(canGetNode(testSession, ccPath));
        assertFalse(testSession.propertyExists(childNPath + '/' + JcrConstants.JCR_PRIMARYTYPE));

        Map<String, Value> emptyStringRestriction = new HashMap<String, Value>(getRestrictions(superuser, path));
        emptyStringRestriction.put(AccessControlConstants.P_GLOB.toString(), vf.createValue(""));

        givePrivileges(path, read, emptyStringRestriction);
        assertTrue(testSession.nodeExists(path));
        assertTrue(canGetNode(testSession, path));
        assertFalse(testSession.nodeExists(childNPath));
        assertFalse(canGetNode(testSession, childNPath));
        assertFalse(testSession.nodeExists(ccPath));
        assertFalse(canGetNode(testSession, ccPath));
        assertFalse(testSession.propertyExists(childNPath + '/' + JcrConstants.JCR_PRIMARYTYPE));
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/OAK-2412">OAK-2412</a>
     */
    @Test
    public void testEmptyGlobRestriction3()throws Exception{
        Node child2 = superuser.getNode(path).addNode("child2");
        String childNPath2 = child2.getPath();
        superuser.save();

        try {
            Group group1 = getTestGroup();
            Group group2 = getUserManager(superuser).createGroup("group2");
            group2.addMember(testUser);
            Group group3 = getUserManager(superuser).createGroup("group3");
            superuser.save();

            assertTrue(group1.isDeclaredMember(testUser));
            assertTrue(group2.isDeclaredMember(testUser));
            assertFalse(group3.isDeclaredMember(testUser));

            Privilege[] read = privilegesFromName(Privilege.JCR_READ);

            withdrawPrivileges(path, group1.getPrincipal(), read, Collections.EMPTY_MAP);
            Map<String, Value> emptyStringRestriction = new HashMap<String, Value>(getRestrictions(superuser, path));
            emptyStringRestriction.put(AccessControlConstants.P_GLOB.toString(), vf.createValue(""));
            givePrivileges(path, group1.getPrincipal(), read, emptyStringRestriction);

            withdrawPrivileges(childNPath, group2.getPrincipal(), read, Collections.EMPTY_MAP);
            emptyStringRestriction = new HashMap<String, Value>(getRestrictions(superuser, childNPath));
            emptyStringRestriction.put(AccessControlConstants.P_GLOB.toString(), vf.createValue(""));
            givePrivileges(childNPath, group2.getPrincipal(), read, emptyStringRestriction);

            withdrawPrivileges(childNPath2, group3.getPrincipal(), read, Collections.EMPTY_MAP);
            emptyStringRestriction = new HashMap<String, Value>(getRestrictions(superuser, childNPath2));
            emptyStringRestriction.put(AccessControlConstants.P_GLOB.toString(), vf.createValue(""));
            givePrivileges(childNPath2, group3.getPrincipal(), read, emptyStringRestriction);

            // NOTE: test-session is created here and is expected to reflect the
            // group membership changes made above.
            Session testSession = getTestSession();
            assertTrue(testSession.nodeExists(path));
            assertTrue(testSession.nodeExists(childNPath));
            assertFalse(testSession.nodeExists(childNPath2));
        } finally {
            Authorizable g2 = getUserManager(superuser).getAuthorizable("group2");
            if (g2 != null) {
                g2.remove();
            }
            Authorizable g3 = getUserManager(superuser).getAuthorizable("group3");
            if (g3 != null) {
                g3.remove();
            }
            superuser.save();
        }
    }

    private static boolean canGetNode(Session session, String nodePath) throws RepositoryException {
        try {
            session.getNode(nodePath);
            return true;
        } catch (PathNotFoundException e) {
            return false;
        }
    }

    public void testRemoveMixin() throws Exception {
        Node n = superuser.getNode(path);
        
        Privilege[] privileges = privilegesFromName(Privilege.JCR_READ);

        withdrawPrivileges(path, privileges, getRestrictions(superuser, path));

        assertTrue(n.hasNode("rep:policy"));
        assertTrue(n.isNodeType("rep:AccessControllable"));

        n.removeMixin("rep:AccessControllable");

        superuser.save();
        assertFalse(n.hasNode("rep:policy"));
        assertFalse(n.isNodeType("rep:AccessControllable"));
    }
}