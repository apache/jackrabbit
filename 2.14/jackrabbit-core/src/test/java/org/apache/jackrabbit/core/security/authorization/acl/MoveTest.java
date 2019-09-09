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
import org.apache.jackrabbit.core.security.authorization.AbstractEvaluationTest;
import org.apache.jackrabbit.core.security.authorization.AccessControlConstants;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
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
 * <code>MoveTest</code>...
 */
public class MoveTest extends AbstractEvaluationTest {

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

    public void testMoveAccessControlledNode() throws Exception {
        Session testSession = getTestSession();
        AccessControlManager testAcMgr = getTestACManager();

        /*
        precondition:
        testuser must have READ-only permission on test-node and below
        */
        checkReadOnly(path);
        checkReadOnly(childNPath);

        Node node3 = superuser.getNode(childNPath).addNode(nodeName3);
        superuser.save();

        String node3Path = node3.getPath();
        Privilege[] privileges = privilegesFromName(NameConstants.JCR_READ.toString());

        // permissions defined @ childNode
        // -> revoke read permission
        withdrawPrivileges(childNPath, privileges, getRestrictions(superuser, childNPath));

        assertFalse(testSession.nodeExists(childNPath));
        assertFalse(testAcMgr.hasPrivileges(childNPath, privileges));
        assertFalse(testSession.nodeExists(node3Path));
        assertFalse(testAcMgr.hasPrivileges(node3Path, privileges));

        // move the ancestor node
        String movedChildNPath = path + "/movedNode";
        String movedNode3Path = movedChildNPath + "/" + nodeName3;

        superuser.move(childNPath, movedChildNPath);
        superuser.save();

        // expected behavior:
        // the AC-content present on childNode is still enforced both on
        // the node itself and on the subtree.
        assertFalse(testSession.nodeExists(movedChildNPath));
        assertFalse(testAcMgr.hasPrivileges(movedChildNPath, privileges));
        assertFalse(testSession.nodeExists(movedNode3Path));
        assertFalse(testAcMgr.hasPrivileges(movedNode3Path, privileges));
    }

    public void testMoveAccessControlledNodeInSubtree() throws Exception {
        Session testSession = getTestSession();
        AccessControlManager testAcMgr = getTestACManager();

        /*
        precondition:
        testuser must have READ-only permission on test-node and below
        */
        checkReadOnly(path);
        checkReadOnly(childNPath);

        Node node3 = superuser.getNode(childNPath).addNode(nodeName3);
        superuser.save();

        String node3Path = node3.getPath();
        Privilege[] privileges = privilegesFromName(NameConstants.JCR_READ.toString());

        // permissions defined @ node3Path
        // -> revoke read permission
        withdrawPrivileges(node3Path, privileges, getRestrictions(superuser, node3Path));

        assertFalse(testSession.nodeExists(node3Path));
        assertFalse(testAcMgr.hasPrivileges(node3Path, privileges));

        // move the ancestor node
        String movedChildNPath = path + "/movedNode";
        String movedNode3Path = movedChildNPath + "/" + nodeName3;

        superuser.move(childNPath, movedChildNPath);
        superuser.save();

        // expected behavior:
        // the AC-content present on node3 is still enforced
        assertFalse(testSession.nodeExists(movedNode3Path));
        assertFalse(testAcMgr.hasPrivileges(movedNode3Path, privileges));
    }

    public void testMoveWithDifferentEffectiveAc() throws Exception {
        Session testSession = getTestSession();
        AccessControlManager testAcMgr = getTestACManager();
        ValueFactory vf = superuser.getValueFactory();

        /*
        precondition:
        testuser must have READ-only permission on test-node and below
        */
        checkReadOnly(path);
        checkReadOnly(childNPath);

        Node node3 = superuser.getNode(childNPath).addNode(nodeName3);
        superuser.save();

        String node3Path = node3.getPath();
        Privilege[] privileges = privilegesFromName(NameConstants.JCR_READ.toString());

        // @path read is denied, @childNode its allowed again
        withdrawPrivileges(path, privileges, getRestrictions(superuser, path));
        givePrivileges(childNPath, privileges, getRestrictions(superuser, childNPath));

        assertTrue(testSession.nodeExists(node3Path));
        assertTrue(testAcMgr.hasPrivileges(node3Path, privileges));

        // move the ancestor node
        String movedPath = path + "/movedNode";

        superuser.move(node3Path, movedPath);
        superuser.save();

        // expected behavior:
        // due to move node3 should not e visible any more
        assertFalse(testSession.nodeExists(movedPath));
        assertFalse(testAcMgr.hasPrivileges(movedPath, privileges));
    }

    public void testMoveNodeWithGlobRestriction() throws Exception {
        Session testSession = getTestSession();
        AccessControlManager testAcMgr = getTestACManager();
        ValueFactory vf = superuser.getValueFactory();

        /*
        precondition:
        testuser must have READ-only permission on test-node and below
        */
        checkReadOnly(path);
        checkReadOnly(childNPath);

        Node node3 = superuser.getNode(childNPath).addNode(nodeName3);
        superuser.save();

        String node3Path = node3.getPath();
        Privilege[] privileges = privilegesFromName(NameConstants.JCR_READ.toString());

        // permissions defined @ path
        // restriction: remove read priv to nodeName3 node
        Map<String, Value> restrictions = new HashMap<String, Value>(getRestrictions(superuser, childNPath));
        restrictions.put(AccessControlConstants.P_GLOB.toString(), vf.createValue("/"+nodeName3));
        withdrawPrivileges(childNPath, privileges, restrictions);

        assertFalse(testSession.nodeExists(node3Path));
        assertFalse(testAcMgr.hasPrivileges(node3Path, privileges));

        String movedChildNPath = path + "/movedNode";
        String movedNode3Path = movedChildNPath + "/" + node3.getName();

        superuser.move(childNPath, movedChildNPath);
        superuser.save();

        assertFalse(testSession.nodeExists(movedNode3Path));
        assertFalse(testAcMgr.hasPrivileges(movedNode3Path, privileges));
    }

    public void testMoveNodeWithGlobRestriction2() throws Exception {
        Session testSession = getTestSession();
        AccessControlManager testAcMgr = getTestACManager();
        ValueFactory vf = superuser.getValueFactory();

        /*
        precondition:
        testuser must have READ-only permission on test-node and below
        */
        checkReadOnly(path);
        checkReadOnly(childNPath);

        Node node3 = superuser.getNode(childNPath).addNode(nodeName3);
        superuser.save();

        Privilege[] privileges = privilegesFromName(NameConstants.JCR_READ.toString());

        // permissions defined @ path
        // restriction: remove read priv to nodeName3 node
        Map<String, Value> restrictions = new HashMap<String, Value>(getRestrictions(superuser, childNPath));
        restrictions.put(AccessControlConstants.P_GLOB.toString(), vf.createValue("/"+nodeName3));
        withdrawPrivileges(childNPath, privileges, restrictions);

        // don't fill the per-session read-cache by calling Session.nodeExists
        assertFalse(testAcMgr.hasPrivileges(node3.getPath(), privileges));

        String movedChildNPath = path + "/movedNode";
        String movedNode3Path = movedChildNPath + "/" + node3.getName();

        superuser.move(childNPath, movedChildNPath);
        superuser.save();

        assertFalse(testSession.nodeExists(movedNode3Path));
        assertFalse(testAcMgr.hasPrivileges(movedNode3Path, privileges));
    }
}