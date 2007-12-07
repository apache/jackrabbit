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

import javax.jcr.MergeException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * <code>MergeCheckedoutSubNodeTest</code> contains tests dealing with
 * checked-out nodes in the subtree of the node on which merge is called.
 *
 * @test
 * @sources MergeCheckedoutSubNodeTest.java
 * @executeClass org.apache.jackrabbit.test.api.version.MergeCheckedoutSubNodeTest
 * @keywords versioning
 */
public class MergeCheckedoutSubNodeTest extends AbstractMergeTest {

    /**
     * node to merge
     */
    Node nodeToMerge;

    protected void setUp() throws Exception {
        super.setUp();

        nodeToMerge = testRootNodeW2.getNode(nodeName1);
        // node has to be checked out while merging
        nodeToMerge.checkout();
    }

    protected void tearDown() throws Exception {
        nodeToMerge = null;
        super.tearDown();
    }

    /**
     * Node.merge(): If V' of a versionable subnode N' in the source workspace
     * is a successor of V (the base version of a subnode N in this workspace),
     * calling merge must fail.
     */
    public void testFailIfCorrespondingNodeIsSuccessor() throws RepositoryException {
        // make V' of a subnode N' in source workspace be a successor version of
        // the base version of the corresponding subnode.
        Node n = testRootNode.getNode(nodeName1 + "/" + nodeName2);
        n.checkout();
        n.checkin();

        n.checkout();

        try {
            // merge, besteffort set to false to stop at the first failure
            nodeToMerge.merge(workspace.getName(), false);
            fail("Merging a checkedout node if the version V' of the corresponding node is a successor of this node's base version must fail.");

        } catch (MergeException e) {
            // success
        }
    }

    /**
     * Node.merge(): If V' of a versionable subnode N' in the source workspace
     * is a predeccessor of V or V' identical to V (the base version of a
     * subnode N in this workspace), calling merge must be leave.
     */
    public void testLeaveIfCorrespondingNodeIsPredeccessor() throws RepositoryException {
        // make V' of a subnode N' in source workspace be a predeccessor version of
        // the base version of the corresponding subnode.
        Node n = testRootNodeW2.getNode(nodeName1 + "/" + nodeName2);
        n.checkout();
        n.setProperty(propertyName1, CHANGED_STRING);
        testRootNodeW2.save();
        n.checkin();

        n.checkout();

        // merge, besteffort set to false to stop at the first failure
        nodeToMerge.merge(workspace.getName(), false);

        // check if subnode has status "leave"
        assertTrue(n.getProperty(propertyName1).getString().equals(CHANGED_STRING));
    }

    /**
     * initialize a two-step-hierarchy on default and second workspace
     */
    protected void initNodes() throws RepositoryException {
        // create a versionable parent node
        // nodeName1
        Node topVNode = testRootNode.addNode(nodeName1, versionableNodeType);
        topVNode.setProperty(propertyName1, topVNode.getName());

        // create a versionable sub node
        // nodeName1/nodeName2
        Node subNvNode = topVNode.addNode(nodeName2, versionableNodeType);
        subNvNode.setProperty(propertyName1, subNvNode.getName());

        // save default workspace
        testRootNode.save();

        log.println("test nodes created successfully on " + workspace.getName());

        // clone the newly created node from src workspace into second workspace
        workspaceW2.clone(workspace.getName(), topVNode.getPath(), topVNode.getPath(), true);
        log.println(topVNode.getPath() + " cloned on " + superuserW2.getWorkspace().getName() + " at " + topVNode.getPath());

        testRootNodeW2 = (Node) superuserW2.getItem(testRoot);
    }
}