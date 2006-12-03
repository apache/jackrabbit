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
package org.apache.jackrabbit.test.api.version;

import javax.jcr.RepositoryException;
import javax.jcr.Node;

/**
 * <code>MergeNonVersionableSubNodeTest</code> contains test dealing with
 * nonversionable nodes in the subtree of the node on which merge is called.
 *
 * @test
 * @sources MergeNonVersionableSubNodeTest.java
 * @executeClass org.apache.jackrabbit.test.api.version.MergeNonVersionableSubNodeTest
 * @keywords versioning
 */
public class MergeNonVersionableSubNodeTest extends AbstractMergeTest {

    protected void setUp() throws Exception {
        super.setUp();
    }

    /**
     * Node.merge(): nonversionable subNode N: if it has no versionable
     * ancestor, then it is updated to reflect the state of its corresponding
     * node.<br>
     */
    public void testMergeNodeNonVersionableSubNodeNonVersionableAncestor() throws RepositoryException {
        String nodeToMergePath = nodeName1 + "/" + nodeName2 + "/" + nodeName3;

        // node to merge in second workspace
        Node nodeToMerge = testRootNodeW2.getNode(nodeToMergePath);
        // corresponding node to nodeToMerge in default workspace
        Node correspondingNode = testRootNode.getNode(nodeToMergePath);

        // modify value for non'v node in workspace2 so we can check if node in workspace2 after merge is updated
        // to reflect the state of its corresponding node in default workspace....
        nodeToMerge.setProperty(propertyName1, CHANGED_STRING);
        nodeToMerge.save();
        nodeToMerge.merge(workspace.getName(), true);

        // test if modification on non-v node is done according to corresponding node.
        assertTrue(nodeToMerge.getProperty(propertyName1).getString().equals(correspondingNode.getName()));
    }

    /**
     * Node.merge(): nonversionable subNode N: if the merge result of its
     * nearest versionable ancestor is update,<br> then it is updated to reflect
     * the state of its corresponding node.<br>
     */
    public void testMergeNodeNonVersionableSubNodeUpdate() throws RepositoryException {
        // modify non versionable subnode so we can check if it's updated after merge
        String nvSubNodePath = nodeName1 + "/" + nodeName2 + "/" + nodeName3;
        Node nvSubNode = testRootNodeW2.getNode(nvSubNodePath);
        nvSubNode.setProperty(propertyName1, CHANGED_STRING);
        superuserW2.save();

        // versionable ancestor to merge in second workspace
        Node nodeToMerge = testRootNodeW2.getNode(nodeName1);

        // make sure the ancestor will get status 'update' on merge: V is predeccessor of V'
        nodeToMerge.checkout();
        nodeToMerge.checkin();

        nodeToMerge.checkout();
        nodeToMerge.merge(workspace.getName(), true);

        // corresponding node to nvSubNode in default workspace
        Node correspondingSubNode = testRootNode.getNode(nvSubNodePath);

        // test if modification on non-v node is done according to corresponding node.
        assertTrue(nvSubNode.getProperty(propertyName1).getString().equals(correspondingSubNode.getName()));
    }

    /**
     * Node.merge(): nonversionable subNode N: is left unchanged if the nearest
     * versionable ancestor has state leave.<br>
     */
    public void disable_testMergeNodeNonVersionableSubNodeLeave() throws RepositoryException {
        // modify non versionable subnode so we can check if it's updated after merge
        String nvSubNodePath = nodeName1 + "/" + nodeName2 + "/" + nodeName3;
        Node nvSubNode = testRootNodeW2.getNode(nvSubNodePath);
        nvSubNode.setProperty(propertyName1, CHANGED_STRING);
        superuserW2.save();

        // versionable ancestor to merge in second workspace
        Node nodeToMerge = testRootNodeW2.getNode(nodeName1);

        // make sure the ancestor will get status 'leave' on merge: V is successor of V'
        Node correspondingNodeToMerge = testRootNode.getNode(nodeName1);
        correspondingNodeToMerge.checkout();
        correspondingNodeToMerge.checkin();

        nodeToMerge.checkout();
        nodeToMerge.merge(workspace.getName(), true);

        // test if modification on non-v node is unchanged.
        assertTrue(nvSubNode.getProperty(propertyName1).getString().equals(CHANGED_STRING));
    }

    /**
     * initialize a three-step-hierarchy on default and second workspace
     */
    protected void initNodes() throws RepositoryException {
        // create a versionable parent node
        // nodeName1
        Node topVNode = testRootNode.addNode(nodeName1, versionableNodeType);
        topVNode.setProperty(propertyName1, topVNode.getName());

        // create a non'versionable sub node
        // nodeName1/nodeName2
        Node subNvNode = topVNode.addNode(nodeName2, testNodeType);
        subNvNode.setProperty(propertyName1, subNvNode.getName());

        // create a non'versionable sub node below nonversionable node
        // nodeName1/nodeName2/nodeName3
        Node subSubNvNode = subNvNode.addNode(nodeName3, testNodeType);
        subSubNvNode.setProperty(propertyName1, subSubNvNode.getName());

        // save default workspace
        testRootNode.save();

        log.println("test nodes created successfully on " + workspace.getName());

        // clone the newly created node from src workspace into second workspace
        workspaceW2.clone(workspace.getName(), topVNode.getPath(), topVNode.getPath(), true);
        log.println(topVNode.getPath() + " cloned on " + superuserW2.getWorkspace().getName() + " at " + topVNode.getPath());

        testRootNodeW2 = (Node) superuserW2.getItem(testRoot);
    }

}