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

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.version.VersionManager;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * <code>MergeNodeIteratorTest</code> tests if Node.merge(String, boolean) if
 * bestEffort is true returns a NodeIterator over all versionalbe nodes in the
 * subtree that received a merge result of fail.
 *
 */
public class MergeNodeIteratorTest extends AbstractMergeTest {

    Node expectedFailedNodes[] = new Node[3];

    protected void tearDown() throws Exception {
        for (int i = 0; i < expectedFailedNodes.length; i++) {
            expectedFailedNodes[i] = null;

        }
        super.tearDown();
    }

    /**
     * Tests if Node.merge() when bestEffort is true returns a NodeIterator
     * containing all nodes that received a fail.
     */
    public void testNodeIterator() throws RepositoryException {

        Node nodeToMerge = testRootNodeW2.getNode(nodeName1);

        NodeIterator failedNodes1 = nodeToMerge.merge(workspace.getName(), true);

        List<Node> nodeList = new ArrayList<Node>();
        while (failedNodes1.hasNext()) {
            nodeList.add(failedNodes1.nextNode());
        }

        assertEquals("Node.merge() does not return a NodeIterator with " +
                "expected number of elements.",
                expectedFailedNodes.length,
                nodeList.size());

        // re-aquire iterator, has been consumed to get size
        Iterator<Node> failedNodes2 = nodeList.iterator();
        compareReturnedWithExpected:
        while (failedNodes2.hasNext()) {
            String path = failedNodes2.next().getPath();
            for (int i = 0; i < expectedFailedNodes.length; i++) {
                if (expectedFailedNodes[i] != null) {
                    String expectedPath = expectedFailedNodes[i].getPath();
                    if (path.equals(expectedPath)) {
                        // to assure every failed node appears only once in the
                        // NodeIterator, set each found expected node to null
                        expectedFailedNodes[i] = null;
                        continue compareReturnedWithExpected;
                    }
                }
            }
            fail("Node.merge() must return a NodeIterator over all " +
                    "nodes that did receive a result of fail.");
        }
    }

    /**
     * Tests if VersionManager.merge() when bestEffort is true returns a NodeIterator
     * containing all nodes that received a fail.
     */
    public void testNodeIteratorJcr2() throws RepositoryException {

        Node nodeToMerge = testRootNodeW2.getNode(nodeName1);

        NodeIterator failedNodes1 = nodeToMerge.getSession().getWorkspace().getVersionManager().merge(
                nodeToMerge.getPath(), workspace.getName(), true);

        List<Node> nodeList = new ArrayList<Node>();
        while (failedNodes1.hasNext()) {
            nodeList.add(failedNodes1.nextNode());
        }

        assertEquals("Node.merge() does not return a NodeIterator with " +
                "expected number of elements.",
                expectedFailedNodes.length,
                nodeList.size());

        // re-aquire iterator, has been consumed to get size
        Iterator<Node> failedNodes2 = nodeList.iterator();
        compareReturnedWithExpected:
        while (failedNodes2.hasNext()) {
            String path = failedNodes2.next().getPath();
            for (int i = 0; i < expectedFailedNodes.length; i++) {
                if (expectedFailedNodes[i] != null) {
                    String expectedPath = expectedFailedNodes[i].getPath();
                    if (path.equals(expectedPath)) {
                        // to assure every failed node appears only once in the
                        // NodeIterator, set each found expected node to null
                        expectedFailedNodes[i] = null;
                        continue compareReturnedWithExpected;
                    }
                }
            }
            fail("Node.merge() must return a NodeIterator over all " +
                    "nodes that did receive a result of fail.");
        }
    }

    /**
     * initialize some versionable nodes on default and second workspace
     */
    protected void initNodes() throws RepositoryException {

        // create some versionable node in default workspace (WS1)

        VersionManager versionManager = testRootNode.getSession().getWorkspace().getVersionManager();

        Node mergeRootNode = testRootNode.addNode(nodeName1, versionableNodeType);

        Node nodeWS1_1 = mergeRootNode.addNode(nodeName1, versionableNodeType);
        Node nodeWS1_1Sub1 = nodeWS1_1.addNode(nodeName1, versionableNodeType);
        Node nodeWS1_2 = mergeRootNode.addNode(nodeName2, versionableNodeType);
        Node nodeWS1_2Sub1 = nodeWS1_2.addNode(nodeName1, versionableNodeType);
        Node nodeWS1_3 = mergeRootNode.addNode(nodeName3, versionableNodeType);

        testRootNode.getSession().save();

        versionManager.checkin(nodeWS1_1.getPath()); // create version 1.0
        versionManager.checkout(nodeWS1_1.getPath());

        versionManager.checkin(nodeWS1_1Sub1.getPath()); // create version 1.0
        versionManager.checkout(nodeWS1_1Sub1.getPath());

        versionManager.checkin(nodeWS1_2.getPath()); // create version 1.0
        versionManager.checkout(nodeWS1_2.getPath());

        versionManager.checkin(nodeWS1_2Sub1.getPath()); // create version 1.0
        versionManager.checkout(nodeWS1_2Sub1.getPath());

        versionManager.checkin(nodeWS1_3.getPath()); // create version 1.0
        versionManager.checkout(nodeWS1_3.getPath());

        workspaceW2.clone(workspace.getName(), mergeRootNode.getPath(), mergeRootNode.getPath(), true);

        // get nodes in workspace 2
        Node nodeWS2_1 = (Node) superuserW2.getItem(nodeWS1_1.getPath());
        Node nodeWS2_2 = (Node) superuserW2.getItem(nodeWS1_2.getPath());
        Node nodeWS2_2Sub1 = (Node) superuserW2.getItem(nodeWS1_2Sub1.getPath());

        // create version branches for some of the nodes

        versionManager.checkin(nodeWS2_1.getPath()); // create version 1.1
        versionManager.checkin(nodeWS1_1.getPath()); // create version 1.0.1

        versionManager.checkin(nodeWS2_2.getPath()); // create version 1.1
        versionManager.checkin(nodeWS1_2.getPath()); // create version 1.0.1

        versionManager.checkin(nodeWS2_2Sub1.getPath()); // create version 1.1
        versionManager.checkin(nodeWS1_2Sub1.getPath()); // create version 1.0.1

        // set the nodes with version branches in expectedFailedNodes
        expectedFailedNodes[0] = nodeWS1_1;
        expectedFailedNodes[1] = nodeWS1_2;
        expectedFailedNodes[2] = nodeWS1_2Sub1;
    }

}
