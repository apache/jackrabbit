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

import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>RemoveNodeTest</code>...
 */
public class RemoveNodeTest extends RemoveItemTest {

    private static Logger log = LoggerFactory.getLogger(RemoveNodeTest.class);

    @Override
    protected Item createRemoveItem() throws NotExecutableException, RepositoryException {
        if (testRootNode.hasNode(nodeName1)) {
            throw new NotExecutableException("Parent node must not yet contain a child node '" + nodeName1 + "'.");
        }
        Node removeNode = testRootNode.addNode(nodeName1, testNodeType);
        // make sure the new node is persisted.
        testRootNode.save();

        return removeNode;
    }

    /**
     * Transiently removes a persisted node using {@link javax.jcr.Node#remove()}
     * and test, whether that node cannot be access from its parent.
     */
    public void testRemoveNode() throws RepositoryException {
        removeItem.remove();

        // check if the node has been properly removed
        try {
            String relPath = removePath.substring(removePath.lastIndexOf('/') + 1);
            testRootNode.getNode(relPath);
            fail("Transiently removed node should no longer be accessible from parent node.");
        } catch (PathNotFoundException e) {
            // ok , works as expected
        }
    }

    /**
     * Same as {@link #testRemoveNode()}, but calls save() (persisting the removal)
     * before executing the test.
     */
    public void testRemoveNode2() throws RepositoryException, NotExecutableException {
        removeItem.remove();
        testRootNode.save();
        try {
            String relPath = removePath.substring(removePath.lastIndexOf('/') + 1);
            testRootNode.getNode(relPath);
            fail("Persistently removed node should no longer be accessible from parent node.");
        } catch (PathNotFoundException e) {
            // ok , works as expected
        }
    }

    /**
     * A removed node must throw InvalidItemStateException upon any call to a
     * node specific method.
     */
    public void testInvalidStateRemovedNode() throws RepositoryException {
        removeItem.remove();
        try {
            ((Node)removeItem).getPrimaryNodeType();
            fail("Calling getPrimaryNodeType() on a removed node must throw InvalidItemStateException.");
        } catch (InvalidItemStateException e) {
            //ok
        }

        try {
            ((Node)removeItem).getProperty(jcrPrimaryType);
            fail("Calling getProperty(String) on a removed node must throw InvalidItemStateException.");
        } catch (InvalidItemStateException e) {
            //ok
        }
    }

    /**
     * Same as {@link #testInvalidStateRemovedNode()} but calls save() before
     * executing the test.
     */
    public void testInvalidStateRemovedNode2() throws RepositoryException {
        removeItem.remove();
        testRootNode.save();

        try {
            ((Node)removeItem).getPrimaryNodeType();
            fail("Calling getPrimaryNodeType() on a removed node must throw InvalidItemStateException.");
        } catch (InvalidItemStateException e) {
            //ok
        }

        try {
            ((Node)removeItem).getProperty(jcrPrimaryType);
            fail("Calling getProperty(String) on a removed node must throw InvalidItemStateException.");
        } catch (InvalidItemStateException e) {
            //ok
        }
    }

    public void testInvalidStateRemovedNode3() throws RepositoryException {
        Node childNode = testRootNode.addNode(nodeName1, testNodeType);
        superuser.save();

        // get the node with session 2
        Session otherSession = getHelper().getReadWriteSession();
        try {
            Node childNode2 = (Node) otherSession.getItem(childNode.getPath());

            childNode.remove();
            superuser.save();

            // try to remove already removed node with session 2
            try {
                childNode2.refresh(false);
                childNode2.remove();
                otherSession.save();
                fail("Removing a node already removed by other session should throw an InvalidItemStateException!");
            } catch (InvalidItemStateException e) {
                //ok, works as expected
            }
        } finally {
            otherSession.logout();
        }
    }
}