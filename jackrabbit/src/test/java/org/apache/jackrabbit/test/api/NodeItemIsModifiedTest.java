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

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Test cases for {@link Item#isModified()} on a node.
 * <p/>
 * Configuration requirements:<br/>
 * The node at {@link #testRoot} must allow a child node of type
 * {@link #testNodeType} with name {@link #nodeName1}. The node type must
 * support a non-mandatory string property with name {@link #propertyName1}.
 *
 * @test
 * @sources NodeItemIsModifiedTest.java
 * @executeClass org.apache.jackrabbit.test.api.NodeItemIsModifiedTest
 * @keywords level2
 */
public class NodeItemIsModifiedTest extends AbstractJCRTest {

    /**
     * Test if Item.isModified() returns false after a new NodeItem is set
     * (before node is saved (transient). That means the NodeItem don't exists
     * persistent).
     *
     * @see javax.jcr.Item#isModified()
     */
    public void testTransientNewNodeItemIsModified () throws RepositoryException {
        Node testNode = testRootNode.addNode(nodeName1, testNodeType);
        Item testNodeItem = superuser.getItem(testNode.getPath());
        // check testNodeItem.isModified() for a new NodeItem before save
        assertFalse("Item.isModified() must return false directly after a new NodeItem is added (before save of the parent node)", testNodeItem.isModified());
    }

    /**
     * Test if Item.isModified() returns false after a new NodeItem is set
     * and saved (persistent). That means the NodeItem exists persistently but
     * isn't modified after save.
     * This is equivalent to the test if Item.isModified() returns false for an
     * already exixting and not modified NodeItem.
     *
     * @see javax.jcr.Item#isModified()
     */
    public void testPersistentNewNodeItemIsModified () throws RepositoryException {
        Node testNode = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.save();
        Item testNodeItem = superuser.getItem(testNode.getPath());
        // check testNodeItem.isModified() for a new NodeItem after save
        assertFalse("Item.isModified() must return false after a new NodeItem is added and the parent Node is saved", testNodeItem.isModified());
    }

    /**
     * Test if Item.isModified() returns true for an already existing and modified
     * NodeItem (modifications aren't saved (transient)).
     * Modified means that a property is added with a string value.
     *
     * @see javax.jcr.Item#isModified()
     */
    public void testTransientNodeItemIsModified () throws RepositoryException {
        Node testNode = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.save();
        // modify the persistent testNode
        testNode.setProperty(propertyName1, "test");
        Item testNodeItem = superuser.getItem(testNode.getPath());
        // check testNodeItem.isModified() before save
        assertTrue("Item.isModified() must return true directly after an existing NodeItem is modified (before current node is saved)", testNodeItem.isModified());
    }

    /**
     * Test if Item.isModified() returns false for an already exixting and modified
     * NodeItem after the node is saved (persistent).
     * Modified means that a property is added with a string value.
     *
     * @see javax.jcr.Item#isModified()
     */
    public void testPersistentNodeItemIsModified () throws RepositoryException {
        Node testNode = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.save();
        // modify the persistent testNode
        testNode.setProperty(propertyName1, "test");
        testNode.save();
        Item testNodeItem = superuser.getItem(testNode.getPath());
        // check testNodeItem.isModified() after save
        assertFalse("Item.isModified() must return false after an existing Property is modified and the current Node is saved", testNodeItem.isModified());
    }

}