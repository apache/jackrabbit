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

import java.security.Principal;
import java.util.Calendar;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.core.security.principal.GroupPrincipals;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.test.RepositoryHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** <code>NodeImplTest</code>... */
public class NodeImplTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(NodeImplTest.class);

    protected void setUp() throws Exception {
        super.setUp();
        if (!(testRootNode instanceof NodeImpl) && !(testRootNode.getSession() instanceof SessionImpl)) {
            throw new NotExecutableException();
        }
    }

    public static void changeReadPermission(Principal principal, Node n, boolean allowRead) throws RepositoryException, NotExecutableException {
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
            for (AccessControlPolicy acp : acps) {
                if (acp instanceof JackrabbitAccessControlList) {
                    acl = (JackrabbitAccessControlList) acp;
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

    public static Principal getReadOnlyPrincipal(RepositoryHelper helper) throws RepositoryException, NotExecutableException {
        SessionImpl s = (SessionImpl) helper.getReadOnlySession();
        try {
            for (Principal p : s.getSubject().getPrincipals()) {
                if (!GroupPrincipals.isGroup(p)) {
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

        Principal principal = getReadOnlyPrincipal(getHelper());
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
    /**
     * Test case for JCR-2336. Setting jcr:data (of type BINARY) must convert
     * the String value to a binary.
     *
     * @throws RepositoryException -
     */
    public void testSetPropertyConvertValue() throws RepositoryException {
        Node content = testRootNode.addNode("jcr:content", "nt:resource");
        content.setProperty("jcr:lastModified", Calendar.getInstance());
        content.setProperty("jcr:mimeType", "text/plain");
        content.setProperty("jcr:data", "Hello");
        superuser.save();
    }

    public void testSetPropertyConvertToString() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1, "nt:folder");
        n.addMixin("mix:title");
        // must convert to string there is no other definition for this property
        Property p = n.setProperty("jcr:title", 123);
        assertEquals(PropertyType.nameFromValue(PropertyType.STRING),
                PropertyType.nameFromValue(p.getType()));
    }

    public void testSetPropertyExplicitType() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1, ntUnstructured);
        n.addMixin("mix:title");
        Property p = n.setProperty("jcr:title", "foo");
        assertEquals(PropertyType.nameFromValue(PropertyType.STRING),
                PropertyType.nameFromValue(p.getType()));
        assertEquals(PropertyType.nameFromValue(PropertyType.STRING),
                PropertyType.nameFromValue(p.getDefinition().getRequiredType()));
        p.remove();
        // must use residual definition from nt:unstructured
        p = n.setProperty("jcr:title", 123);
        assertEquals(PropertyType.nameFromValue(PropertyType.LONG),
                PropertyType.nameFromValue(p.getType()));
        assertEquals(PropertyType.nameFromValue(PropertyType.UNDEFINED),
                PropertyType.nameFromValue(p.getDefinition().getRequiredType()));
    }

    public void testSetPropertyConvertMultiValued() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1, "test:canSetProperty");
        // must convert to long there is no other definition for this property
        Property p = n.setProperty("LongMultiple", new String[]{"123", "456"});
        assertEquals(PropertyType.nameFromValue(PropertyType.LONG),
                PropertyType.nameFromValue(p.getType()));
    }

    /**
     * Test case for JCR-2130 and JCR-2408.
     *
     * @throws RepositoryException
     */
    public void testAddRemoveMixin() throws RepositoryException {
        // add mix:title to a nt:folder node and set jcr:title property
        Node n = testRootNode.addNode(nodeName1, "nt:folder");
        n.addMixin("mix:title");
        n.setProperty("jcr:title", "blah blah");
        testRootNode.getSession().save();

        // remove mix:title, jcr:title should be gone as there's no matching
        // definition in nt:folder
        n.removeMixin("mix:title");
        testRootNode.getSession().save();
        assertFalse(n.hasProperty("jcr:title"));

        // add mix:title to a nt:unstructured node and set jcr:title property
        Node n1 = testRootNode.addNode(nodeName2, ntUnstructured);
        n1.addMixin("mix:title");
        n1.setProperty("jcr:title", "blah blah");
        assertEquals(
                n1.getProperty("jcr:title").getDefinition().getDeclaringNodeType().getName(),
                "mix:title");

        // remove mix:title, jcr:title should stay since it adopts the residual
        // property definition declared in nt:unstructured
        testRootNode.getSession().save();

        n1.removeMixin("mix:title");
        testRootNode.getSession().save();
        assertTrue(n1.hasProperty("jcr:title"));

        assertEquals(
                n1.getProperty("jcr:title").getDefinition().getDeclaringNodeType().getName(),
                ntUnstructured);

        // add mix:referenceable to a nt:unstructured node, jcr:uuid is
        // automatically added
        Node n2 = testRootNode.addNode(nodeName3, ntUnstructured);
        n2.addMixin(mixReferenceable);
        testRootNode.getSession().save();

        // remove mix:referenceable, jcr:uuid should always get removed
        // since it is a protcted property
        n2.removeMixin(mixReferenceable);
        testRootNode.getSession().save();
        assertFalse(n2.hasProperty("jcr:uuid"));
    }


    /**
     * Test corruption in session / persistence state after
     * {@link ReferentialIntegrityException}.
     *
     * @see <a href="https://issues.apache.org/jira/browse/JCR-2503">JCR-2503</a>
     */
    public void testReferentialIntegrityCorruption() throws Exception {
        Session session = testRootNode.getSession();
        Node root = testRootNode.addNode("testReferentialIntegrityCorruption");

        // Create test nodes P1 and P2
        Node nodeP1 = root.addNode("P1");
        nodeP1.addMixin("mix:referenceable");
        Node nodeP2 = root.addNode("P2");
        nodeP2.addMixin("mix:referenceable");
        session.save();

        // Create reference from P2 to P1 and save
        nodeP2.setProperty("referencetoP1", nodeP1);
        session.save();

        // Add node P3
        Node nodeP3 = root.addNode("P3");
        nodeP3.addMixin("mix:referenceable");

        // And try to remove P1 while P2 still references P1
        nodeP1.remove();
        try {
            session.save();
        } catch (ReferentialIntegrityException expected) {
            // Got ReferentialIntegrityException as expected
        }

        // Remove P2 and save again, this will succeed. As P1, P2
        // should be removed and P3 should exist
        try {
            nodeP2.remove();
            session.save();
        } catch (Exception e) {
            String msg =
                "JCR-2503: Saving delete after"
                + " ReferentialIntegrityException failed";
            log.error(msg, e);
            fail(msg);
        }

        try {
            nodeP3 = session.getNodeByIdentifier(nodeP3.getIdentifier());
        } catch (Exception e) {
            String msg =
                "JCR-2503: Retrieving P3 by uuid failed. Corrupt session?";
            log.error(msg, e);
            fail(msg);
        }

        try {
            nodeP3.remove();
            session.save();
        } catch (Exception e) {
            String msg = "JCR-2503: Removing P3 failed. Corrupt session?";
            log.error(msg, e);
            fail(msg);
        }

        try {
            root = testRootNode.getNode("testReferentialIntegrityCorruption");
            for (Node ignore : JcrUtils.getChildNodes(root)) {
            }
        } catch (Exception e) {
            String msg = "JCR-2503: Failed to scan empty node. Corrupt session?";
            log.error(msg, e);
            fail(msg);
        }

        root.remove();
        session.save();
    }

    /**
     * Test corruption in session / persistence state after
     * {@link ReferentialIntegrityException}.
     * <p>
     * This is a variant of {@link #testReferentialIntegrityCorruption()}
     * that checks that {@link Node#getPath()} works after the save operation.
     * 
     * @see <a href="https://issues.apache.org/jira/browse/JCR-3018">JCR-3018</a>
     */
    public void testReferentialIntegrityCorruptionGetPath() throws Exception {
        Session session = testRootNode.getSession();
        Node root = testRootNode.addNode("testReferentialIntegrityCorruption");

        // Create test nodes P1 and P2
        Node nodeP1 = root.addNode("P1");
        nodeP1.addMixin("mix:referenceable");
        Node nodeP2 = root.addNode("P2");
        nodeP2.addMixin("mix:referenceable");
        session.save();

        // Create reference from P2 to P1 and save
        nodeP2.setProperty("referencetoP1", nodeP1);
        session.save();

        // Add node P3
        Node nodeP3 = root.addNode("P3");
        nodeP3.addMixin("mix:referenceable");

        String nodeP3path = nodeP3.getPath();

        // And try to remove P1 while P2 still references P1
        nodeP1.remove();
        try {
            session.save();
        } catch (ReferentialIntegrityException expected) {
            // Got ReferentialIntegrityException as expected
        }

        // Remove P2 and save again, this will succeed. As P1, P2
        // should be removed and P3 should exist
        try {
            nodeP2.remove();
            session.save();
        } catch (Exception e) {
            String msg = "JCR-3018: Saving delete after"
                    + " ReferentialIntegrityException failed";
            log.error(msg, e);
            fail(msg);
        }

        try {
            assertEquals(nodeP3path, nodeP3.getPath());
            nodeP3 = session.getNodeByIdentifier(nodeP3.getIdentifier());
        } catch (Exception e) {
            String msg = "JCR-3018: getting path of P3 failed. Corrupt session?";
            log.error(msg, e);
            fail(msg);
        }

        root.remove();
        session.save();
    }

    public void testBracketsInNodeName() throws Exception {
        final Node root = testRootNode.addNode("testBracketsInNodeName");

        final String[] childNames = { "{A}", "B}", "{C", "(D)", "E)", "(F", };

        for (String name : childNames) {
            root.addNode(name);
            root.getSession().save();
            assertTrue("Expecting child " + name + " to have been created", root.hasNode(name));
        }
    }
}
