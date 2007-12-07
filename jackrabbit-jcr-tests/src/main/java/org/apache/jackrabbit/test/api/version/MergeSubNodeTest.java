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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

/**
 * <code>MergeSubNodeTest</code> contains tests dealing with sub nodes in the
 * subtree of the node on which merge is called.
 *
 * @test
 * @sources MergeSubNodeTest.java
 * @executeClass org.apache.jackrabbit.test.api.version.MergeSubNodeTest
 * @keywords versioning
 */

public class MergeSubNodeTest extends AbstractMergeTest {

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
     * Node.merge(): versionable subNode N: If N has status leave but parent is
     * update, then the subnode N is removed<br> retrieve the initialised node
     * to perform operations we need before for this test<br>
     */
    public void disable_testRemoveNodeFromSourceWorkspaceAndMergeWithUpdate() throws RepositoryException {
        // status 'update' for parent
        nodeToMerge.checkin();
        nodeToMerge.checkout();

        // status 'leave' for subnode
        Node originalNode = testRootNode.getNode(nodeName1);
        Node originalSubNode = originalNode.getNode(nodeName2);
        originalSubNode.checkout();
        originalSubNode.checkin();

        // "merge" the nodeToMerge with the newNode from the default workspace
        // besteffort set to false to stop at the first failure
        nodeToMerge.merge(workspace.getName(), false);

        // if merge passed newSubNode1 should be also removed from workspace2
        assertFalse("subNode1 not removed from " + workspaceW2.getName() + " as expected", nodeToMerge.hasNode(nodeName2));

        // return version info about the clonedNode as it must also be updated
        final String originalBaseVersionUUID = originalNode.getBaseVersion().getUUID();
        final String clonedBaseVersionUUID = nodeToMerge.getBaseVersion().getUUID();

        assertTrue("clonedNode has different version UUID than expected, it should be updated with the newNode version UUID", originalBaseVersionUUID.equals(clonedBaseVersionUUID));
    }

    /**
     * Node.merge(): versionable subNode N checked-in: If V' is a successor (to
     * any degree) of V, then the merge result for N is update<br> modify a node
     * on the workspace1 and then merge the one in workspace2 with the one in
     * workspace1 precondition is that the node in workspace2 is checked in
     */
    public void disable_testMergeNodeFromUpdatedSourceWorkspace() throws RepositoryException {
        Node originalNode = testRootNode.getNode(nodeName1);

        // update nodeName1 on workspace1
        originalNode.checkout();
        originalNode.checkin();

        testRootNode.save();

        // "merge" the clonedNode with the newNode from the default workspace
        // besteffort set to false to stop at the first failure
        nodeToMerge.merge(workspace.getName(), false);

        final String originalBaseVersionUUID = originalNode.getBaseVersion().getUUID();
        final String clonedBaseVersionUUID = nodeToMerge.getBaseVersion().getUUID();

        assertTrue("clonedNode has different version UUID than expected, it should be updated with the newNode version UUID", originalBaseVersionUUID.equals(clonedBaseVersionUUID));

    }

    /**
     * Node.merge(): versionable subNode N checked-in: If V' is a predecessor
     * (to any degree) of V or if V and V' are identical (i.e., are actually the
     * same version), then the merge result for N is leave<br> modify a node on
     * the workspace2 and then merge the one in workspace2 with the one in
     * workspace1<br> the node in workspace2 should be updated<br> precondition
     * is that the node in workspace2 is checked in
     */
    public void testMergeNodeFromOlderSourceWorkspace() throws RepositoryException {
        // touch the version on workspace2
        nodeToMerge.checkin();
        nodeToMerge.checkout();

        String baseVersionUUIDbeforeMerge = nodeToMerge.getBaseVersion().getUUID();

        // "merge" the clonedNode with the newNode from the default workspace
        // besteffort set to false to stop at the first failure
        nodeToMerge.merge(workspace.getName(), false);

        assertTrue("clonedNode has different UUID than expected, it should be left unchanged", baseVersionUUIDbeforeMerge.equals(nodeToMerge.getBaseVersion().getUUID()));
    }

    /**
     * Node.merge(): bestEffort is true > (sub)node which could not be merged
     * are not affected.<br>
     */
    public void disable_testMergeNodeBestEffortTrue() throws RepositoryException {
        // create 2 new nodes with two independent versions
        // so merge fails for this node
        Node originalNode = testRootNode.getNode(nodeName1);
        originalNode.checkout();
        Node subNode = originalNode.getNode(nodeName2);
        // will be unchanged after merge
        subNode.checkout();
        subNode.setProperty(propertyName1, CHANGED_STRING);
        // will be updated
        originalNode.setProperty(propertyName1, CHANGED_STRING);
        superuser.save();
        subNode.checkin();
        originalNode.checkin();

        Node subNodeW2 = nodeToMerge.getNode(nodeName2);
        subNodeW2.checkout();
        subNodeW2.setProperty(propertyName1, CHANGED_STRING);
        superuserW2.save();
        subNodeW2.checkin();

        nodeToMerge.checkout();

        // merge, besteffort set to true
        nodeToMerge.merge(workspace.getName(), true);

        // sub node should not be touched because merging failed
        assertTrue(subNodeW2.getProperty(propertyName1).getString().equals(""));

        // test root node should be touched because update
        assertFalse(nodeToMerge.getProperty(propertyName1).getString().equals(nodeToMerge.getName()));
    }

    /**
     * Node.merge(): For each versionable node N in the subtree rooted at this
     * node,<br> a merge test is performed comparing N with its corresponding
     * node in workspace, N'.<br>
     */
    public void disable_testMergeNodeSubNodesMergeTest() throws RepositoryException {
        //setCheckProperty(nodeToMerge);
        nodeToMerge.checkout();

        nodeToMerge.merge(workspace.getName(), true);

        // check subnodes if they were touched
        for (NodeIterator ni = nodeToMerge.getNodes(); ni.hasNext();) {
            Node n = ni.nextNode();
            if (n.getBaseVersion() != null) {
                assertTrue(n.getProperty(propertyName1).getString().equals(CHANGED_STRING));
            }
        }
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
