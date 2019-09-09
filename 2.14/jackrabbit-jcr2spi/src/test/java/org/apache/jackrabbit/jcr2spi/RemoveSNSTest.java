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
package org.apache.jackrabbit.jcr2spi;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>RemoveSNSTest</code> (Implementation specific tests. JSR170 only
 * expects orderable same-name-siblings to have a consistent and testable
 * order.)
 */
public class RemoveSNSTest extends RemoveNodeTest {

    private static Logger log = LoggerFactory.getLogger(RemoveSNSTest.class);

    private Node firstSiblingNode;
    private String firstSiblingPath;

    @Override
    protected void tearDown() throws Exception {
        firstSiblingNode = null;
        super.tearDown();
    }

    @Override
    protected Item createRemoveItem() throws NotExecutableException, RepositoryException {
        if (testRootNode.hasNode(nodeName1)) {
            fail("Setup: Parent node must not yet contain a child node '" + nodeName1 + "'.");
        }
        firstSiblingNode = testRootNode.addNode(nodeName1, testNodeType);
        if (!firstSiblingNode.getDefinition().allowsSameNameSiblings()) {
            fail("Setup: RemoveSNSTest cannot be execute. Unable to create SameNameSiblings.");
        }
        firstSiblingPath = firstSiblingNode.getPath();

        Node removeNode = testRootNode.addNode(nodeName1, testNodeType);
        // make sure the new node is persisted.
        testRootNode.save();
        return removeNode;
    }

    /**
     * Transiently removes the first SNS-node using {@link javax.jcr.Node#remove()}
     * and test, whether the remaining sibling 'replaces' the removed node and
     * is the same as the node added as second sibling.
     */
    public void testRemoveFirstSibling() throws RepositoryException {
        firstSiblingNode.remove();

        // check if the node has been properly removed
        try {
            Node secondSibling = testRootNode.getNode(nodeName1);
            // implementation specific:
            assertTrue("", removeItem.isSame(secondSibling));
        } catch (PathNotFoundException e) {
            fail("Second sibling must still be available.");
        }
    }

    /**
     * Same as {@link #testRemoveNode()}, but calls save() (persisting the removal)
     * before executing the test.
     */
    public void testRemoveFirstSibling2() throws RepositoryException, NotExecutableException {
        firstSiblingNode.remove();
        testRootNode.save();

        // check if the node has been properly removed
        try {
            Node secondSibling = testRootNode.getNode(nodeName1);
            // implementation specific:
            assertTrue("", removeItem.isSame(secondSibling));
        } catch (PathNotFoundException e) {
            fail("Second sibling must still be available.");
        }
    }

    /**
     * Transiently removes a persisted item using {@link javax.jcr.Item#remove()}
     * and test, whether the successor sibling is returned when retrieving the
     * item with the path of the removed node.
     */
    public void testRemoveFirstSibling3() throws RepositoryException {
        firstSiblingNode.remove();

        // check if the node has been properly removed
        try {
            Item secondSibling = superuser.getItem(firstSiblingPath);
            // implementation specific:
            assertTrue("", removeItem.isSame(secondSibling));
        } catch (PathNotFoundException e) {
            fail("Removing a SNS Node -> successor must be accessible from the session by removed path.");
        }
    }

    /**
     * Same as {@link #testRemoveFirstSibling3()} but calls save() before
     * executing the test.
     */
    public void testRemoveFirstSibling4() throws RepositoryException {
        firstSiblingNode.remove();
        testRootNode.save();

        // check if the node has been properly removed
        try {
            Item secondSibling = superuser.getItem(firstSiblingPath);
            // implementation specific:
            assertTrue("", removeItem.isSame(secondSibling));
        } catch (PathNotFoundException e) {
            fail("Removing a SNS Node -> successor must be accessible from the session by removed path.");
        }
    }
}