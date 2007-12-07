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
package org.apache.jackrabbit.test.api;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NodeIterator;

/**
 * <code>SessionReadMethodsTest</code>...
 *
 * @test
 * @sources SessionReadMethodsTest.java
 * @executeClass org.apache.jackrabbit.test.api.SessionReadMethodsTest
 * @keywords level1
 */
public class SessionReadMethodsTest extends AbstractJCRTest {

    /**
     * A Version 1 UUID
     */
    private final String RANDOM_UUID = "710def90-80cd-11d9-9669-0800200c9a66";

    /**
     * The read only session for the tests
     */
    private Session session;

    protected void setUp() throws Exception {
        isReadOnly = true;
        super.setUp();
        session = helper.getReadOnlySession();
    }

    /**
     * Releases the session aquired in {@link #setUp()}.
     */
    protected void tearDown() throws Exception {
        if (session != null && session.isLive()) {
            session.logout();
            session = null;
        }
        super.tearDown();
    }

    /**
     * Tests that session.getItem() throws a PathNotFoundException with a given
     * path to nowhere.
     */
    public void testGetItemFailure()
            throws RepositoryException, NotExecutableException {
        try {
            session.getItem(getNonExistingPath());
            fail("Session.getItem() does not throw PathNotFoundException in " +
                    "case a invalid path is provided.");
        } catch (PathNotFoundException pnfe) {
            // ok
        }
    }

    /**
     * Tests session.getItem() with the testRootNode and assures that the
     * returned node represents the same node in the repository as
     * testRootNode.
     */
    public void testGetItem() throws RepositoryException {
        Item item = session.getItem(testRoot);
        assertTrue("Session.getItem doesn't return the correct item.",
                item.isSame(testRootNode));
    }

    /**
     * Tests session.itemExists() in the case of a valid and an invalid path.
     */
    public void testItemExists() throws RepositoryException {
        assertTrue("Session.itemExists() returns false on the testRootNode.",
                session.itemExists(testRootNode.getPath()));
        assertFalse("Session.itemExists() returns true on a malformed path.",
                session.itemExists(getNonExistingPath()));
    }

    /**
     * Tests that session.getNodeByUUID() throws a ItemNotFoundException in case
     * of an invalid uuid.
     */
    public void testGetNodeByUUIDFailure() throws RepositoryException {
        try {
            session.getNodeByUUID(RANDOM_UUID);
            fail("Not valid UUID should throw a ItemNotFoundException.");
        } catch (ItemNotFoundException infe) {
            // ok
        }
    }

    /**
     * Tests session.getNodeByUUID() using a valid uuid of a referenceable node
     */
    public void testGetNodeByUUID() throws RepositoryException, NotExecutableException {
        Node referenced = findReferenceable(testRootNode);
        if (referenced == null) {
            throw new NotExecutableException("Workspace does not contain a referenceable node.");
        }
        String uuid = referenced.getProperty(jcrUUID).getString();
        Node node = session.getNodeByUUID(uuid);
        assertTrue("Node retrieved with session.getNodeByUUID is not the same " +
                "as the node having the given uuid.",
                referenced.isSame(node));
    }

    /**
     * Tests if getAttribute(String name) returns not null if the requested
     * attribute is existing
     */
    public void testGetAttribute() throws NotExecutableException {
        String names[] = session.getAttributeNames();
        if (names.length == 0) {
            throw new NotExecutableException("No attributes set in this session.");
        }
        for (int i = 0; i < names.length; i++) {
            assertNotNull("getAttribute(String name) returned null although the " +
                    "requested attribute is existing.",
                    session.getAttribute(names[i]));
        }
    }

    /**
     * Tests if getAttribute(String name) returns null if the requested attribute
     * is not existing
     */
    public void testGetAttributeFailure() {
        String names[] = session.getAttributeNames();
        StringBuffer notExistingName = new StringBuffer("X");
        for (int i = 0; i < names.length; i++) {
            notExistingName.append(names[i]);
        }
        assertNull("getAttribute(String name) must return null if the " +
                "requested attribute is not existing",
                session.getAttribute(notExistingName.toString()));
    }

    /**
     * Tests if attribute names returned by getAttributeNames() do not return
     * null if used for getAttribute(String name)
     */
    public void testGetAttributeNames() {
        String names[] = session.getAttributeNames();
        for (int i = 0; i < names.length; i++) {
            assertNotNull("An attribute name returned by getAttributeNames() " +
                    "does not exist.",
                    session.getAttribute(names[i]));
        }
    }

    /**
     * Tests if isLive() returns true if the <code>Session</code> is usable by
     * the client and false if it is not usable
     */
    public void testIsLive() {
        assertTrue("Method isLive() must return true if the session " +
                "is usable by the client.",
                session.isLive());

        session.logout();
        assertFalse("Method isLive() must return false if the session " +
                "is not usable by the client, e.g. if the session is " +
                "logged-out.",
                session.isLive());
    }

    //----------------------< internal >----------------------------------------

    /**
     * Returns a path to a node that does not exist.
     *
     * @return a path to a node that does not exist.
     */
    private String getNonExistingPath() throws RepositoryException {
        // build path to a node that does not exist
        StringBuffer tmp = new StringBuffer();
        if (testRootNode.getName().length() > 0) {
            tmp.append("/").append(testRootNode.getName());
        }
        int count = 0;
        String nodeName = "node";
        while (testRootNode.hasNode(nodeName + count)) {
            count++;
        }
        tmp.append("/").append(nodeName + count);
        return tmp.toString();
    }

    /**
     * Find a referenceable node for uuid test.
     *
     * @param node the <code>Node</code> where to start the search.
     * @return a referenceable node or <code>null</code> if none was found.
     */
    private Node findReferenceable(Node node) throws RepositoryException {
        Node referenced = null;
        if (node.isNodeType(mixReferenceable)) {
            return node;
        } else {
            NodeIterator iter = node.getNodes();
            while (iter.hasNext()) {
                Node n = iter.nextNode();
                referenced = findReferenceable(n);
                if (referenced != null) {
                    return referenced;
                }
            }
        }
        return referenced;
    }

}
