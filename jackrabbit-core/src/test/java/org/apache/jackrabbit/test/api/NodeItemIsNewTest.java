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
 * Test cases for {@link Item#isNew()} on a node.
 * <p/>
 * Configuration requirements:<br/>
 * The node at {@link #testRoot} must allow a child node of type
 * {@link #testNodeType} with name {@link #nodeName1}.
 *
 * @test
 * @sources NodeItemIsNewTest.java
 * @executeClass org.apache.jackrabbit.test.api.NodeItemIsNewTest
 * @keywords level2
 */
public class NodeItemIsNewTest extends AbstractJCRTest {

    /**
     * Test if Item.isNew() returns true directly after a new NodeItem is added
     * (before node is saved (transient)).
     *
     * @see javax.jcr.Item#isNew()
     */
    public void testTransientNodeItemIsNew () throws RepositoryException {
        Node testNode = testRootNode.addNode(nodeName1, testNodeType);
        Item testNodeItem = superuser.getItem(testNode.getPath());
        // check testNodeItem is new before save
        assertTrue("Item.isNew() must return true directly after a new NodeItem is added (before save of the parent node)", testNodeItem.isNew());
    }

    /**
     * Test if Item.isNew() returns false after a NodeItem is added and
     * the node is saved (persistent).
     *
     * @see javax.jcr.Item#isNew()
     */
    public void testPersistentNodeItemIsNew () throws RepositoryException {
        Node testNode = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.save();
        Item testNodeItem = superuser.getItem(testNode.getPath());
        // check testNodeItem is new after save
        assertFalse("Item.isNew() must return false after a new NodeItem is added and the parent Node is saved", testNodeItem.isNew());
    }

}