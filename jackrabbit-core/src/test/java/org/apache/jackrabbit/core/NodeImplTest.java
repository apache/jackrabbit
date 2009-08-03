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
package org.apache.jackrabbit.core;

import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;
import java.security.Principal;
import java.security.acl.Group;
import java.util.Iterator;

/** <code>NodeImplTest</code>... */
public class NodeImplTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(NodeImplTest.class);

    protected void setUp() throws Exception {
        super.setUp();
        if (!(testRootNode instanceof NodeImpl) && !(testRootNode.getSession() instanceof SessionImpl)) {
            throw new NotExecutableException();
        }
    }

    private static void changeReadPermission(Principal principal, Node n, boolean allowRead) throws RepositoryException, NotExecutableException {
        SessionImpl s = (SessionImpl) n.getSession();
        JackrabbitAccessControlList acl = null;
        AccessControlManager acMgr = s.getAccessControlManager();
        AccessControlPolicyIterator it = acMgr.getApplicablePolicies(n.getPath());
        while (it.hasNext()) {
            AccessControlPolicy acp = it.nextAccessControlPolicy();
            if (acp instanceof JackrabbitAccessControlList) {
                acl = (JackrabbitAccessControlList) acp;
                break;
            }
        }
        if (acl == null) {
            AccessControlPolicy[] acps = acMgr.getPolicies(n.getPath());
            for (int i = 0; i < acps.length; i++) {
                if (acps[i] instanceof JackrabbitAccessControlList) {
                    acl = (JackrabbitAccessControlList) acps[i];
                    break;
                }
            }
        }

        if (acl != null) {
            acl.addEntry(principal, new Privilege[] {acMgr.privilegeFromName(Privilege.JCR_READ)}, allowRead);
            acMgr.setPolicy(n.getPath(), acl);
            s.save();
        } else {
            // no JackrabbitAccessControlList found.
            throw new NotExecutableException();
        }
    }

    private Principal getReadOnlyPrincipal() throws RepositoryException, NotExecutableException {
        SessionImpl s = (SessionImpl) getHelper().getReadOnlySession();
        try {
            for (Iterator it = s.getSubject().getPrincipals().iterator(); it.hasNext();) {
                Principal p = (Principal) it.next();
                if (!(p instanceof Group)) {
                    return p;
                }
            }
        } finally {
            s.logout();
        }
        throw new NotExecutableException();
    }

    /**
     * Test case for #JCR-1729. Note, that test will only be executable with
     * a security configurations that allows to set Deny-ACEs.
     *
     * @throws RepositoryException
     * @throws NotExecutableException
     */
    public void testIsCheckedOut() throws RepositoryException, NotExecutableException {
        Node n = testRootNode.addNode(nodeName1);
        NodeImpl testNode = (NodeImpl) n.addNode(nodeName2);
        testRootNode.save();

        Principal principal = getReadOnlyPrincipal();
        changeReadPermission(principal, n, false);
        changeReadPermission(principal, testNode, true);

        Session readOnly = getHelper().getReadOnlySession();
        try {
            NodeImpl tn = (NodeImpl) readOnly.getItem(testNode.getPath());
            assertTrue(tn.isCheckedOut());

            n.addMixin(mixVersionable);
            testRootNode.save();
            n.checkin();

            assertFalse(tn.isCheckedOut());
        } finally {
            readOnly.logout();
            // reset the denied read-access
            n.checkout();
            changeReadPermission(principal, n, true);
        }
    }

    public void testAddNodeUuid() throws RepositoryException, NotExecutableException {
        String uuid = "f81d4fae-7dec-11d0-a765-00a0c91e6bf6";
        Node n = testRootNode.addNode(nodeName1);
        Node testNode = ((NodeImpl) n).addNodeWithUuid(nodeName2, uuid);
        testNode.addMixin(NodeType.MIX_REFERENCEABLE);
        testRootNode.getSession().save();
        assertEquals(
                "Node identifier should be: " + uuid,
                uuid, testNode.getIdentifier());
    }

    public void testAddNodeUuidCollision() throws RepositoryException, NotExecutableException {
        String uuid = "f81d4fae-7dec-11d0-a765-00a0c91e6bf6";
        Node n = testRootNode.addNode(nodeName1);
        Node testNode1 = ((NodeImpl) n).addNodeWithUuid(nodeName2, uuid);
        testNode1.addMixin(NodeType.MIX_REFERENCEABLE);
        testRootNode.getSession().save();

        try {
            ((NodeImpl) n).addNodeWithUuid(nodeName2, uuid);
            fail("UUID collision not detected by addNodeWithUuid");
        } catch (ItemExistsException e) {
        }
    }

}
