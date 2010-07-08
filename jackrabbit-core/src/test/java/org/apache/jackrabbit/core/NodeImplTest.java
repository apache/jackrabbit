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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.uuid.UUID;
import org.apache.jackrabbit.api.jsr283.security.AccessControlManager;
import org.apache.jackrabbit.api.jsr283.security.AccessControlPolicyIterator;
import org.apache.jackrabbit.api.jsr283.security.AccessControlPolicy;
import org.apache.jackrabbit.api.jsr283.security.Privilege;
import org.apache.jackrabbit.core.security.authorization.JackrabbitAccessControlList;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.RepositoryException;
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

    private static Principal getReadOnlyPrincipal() throws RepositoryException, NotExecutableException {
        SessionImpl s = (SessionImpl) helper.getReadOnlySession();
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
     * Test case for JCR-2130 and JCR-2659.
     *
     * @throws RepositoryException
     */
    public void testAddRemoveMixin() throws RepositoryException {
        // add mix:title to a nt:folder node and set jcr:title property
        Node n = testRootNode.addNode(nodeName1, "nt:folder");
        n.addMixin("mix:referenceable");
        testRootNode.getSession().save();
        assertTrue(n.hasProperty("jcr:uuid"));

        // remove mix:title, jcr:title should be gone as there's no matching
        // definition in nt:folder
        n.removeMixin("mix:referenceable");
        testRootNode.getSession().save();
        assertFalse(n.hasProperty("jcr:uuid"));

        // add mix:referenceable to a nt:unstructured node, jcr:uuid is
        // automatically added
        Node n2 = testRootNode.addNode(nodeName3, "nt:unstructured");
        n2.addMixin(mixReferenceable);
        testRootNode.getSession().save();
        assertTrue(n2.hasProperty("jcr:uuid"));

        // remove mix:referenceable, jcr:uuid should always get removed
        // since it is a protcted property
        n2.removeMixin(mixReferenceable);
        testRootNode.getSession().save();
        assertFalse(n2.hasProperty("jcr:uuid"));
    }

    /**
     * Test case for #JCR-1729. Note, that test will only be executable with
     * a security configurations that allows to set Deny-ACEs.
     *
     * @throws RepositoryException
     * @throws NotExecutableException
     */
    public void testInternalIsCheckedOut() throws RepositoryException, NotExecutableException {
        Node n = testRootNode.addNode(nodeName1);
        NodeImpl testNode = (NodeImpl) n.addNode(nodeName2);
        testRootNode.save();

        Principal principal = getReadOnlyPrincipal();
        changeReadPermission(principal, n, false);
        changeReadPermission(principal, testNode, true);

        Session readOnly = helper.getReadOnlySession();
        try {
            NodeImpl tn = (NodeImpl) readOnly.getItem(testNode.getPath());
            assertTrue(tn.internalIsCheckedOut());

            n.addMixin(mixVersionable);
            testRootNode.save();
            n.checkin();

            assertFalse(tn.internalIsCheckedOut());
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
        testNode.addMixin(JcrConstants.MIX_REFERENCEABLE);
        testRootNode.save();
        assertEquals("Node UUID should be: "+uuid, uuid, testNode.getUUID());
    }
    
    public void testAddNodeUuidCollision() throws RepositoryException, NotExecutableException {
        String uuid = "f81d4fae-7dec-11d0-a765-00a0c91e6bf6";
        Node n = testRootNode.addNode(nodeName1);
        Node testNode1 = ((NodeImpl) n).addNodeWithUuid(nodeName2, uuid);
        testNode1.addMixin(JcrConstants.MIX_REFERENCEABLE);
        testRootNode.save();
        boolean collisionDetected = false;
        
        try {
            Node testNode2 = ((NodeImpl) n).addNodeWithUuid(nodeName2, uuid);
            testNode1.addMixin(JcrConstants.MIX_REFERENCEABLE);
            testRootNode.save();
        } catch (ItemExistsException iee) {
            collisionDetected = true;
        }
        assertTrue("Node collision detected: "+uuid, collisionDetected);
    }    
}