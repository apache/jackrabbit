/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

/**
 * <code>WorkspaceMoveSameNameSibsTest</code> contains tests for moving nodes
 * with same name siblings supported in one workspace.
 *
 * @test
 * @sources WorkspaceMoveSameNameSibsTest.java
 * @executeClass org.apache.jackrabbit.test.api.WorkspaceMoveSameNameSibsTest
 * @keywords level2
 */
public class WorkspaceMoveSameNameSibsTest extends AbstractWorkspaceSameNameSibsTest {

    /**
     * If ordering is supported by the node type of the parent node of the new
     * location, then the newly moved node is appended to the end of the child
     * node list.
     */
    public void testMoveNodesOrderingSupportedByParent() throws RepositoryException {
        // test assumes that repositry supports Orderable Child Node Support (optional)
        String[] orderList = {nodeName1, nodeName2, nodeName3};

        // create a new node to move node three times
        Node newNode = testRootNode.addNode(nodeName2, testNodeType);
        testRootNode.save();

        // copy node three times below a node and check the order
        for (int i = 0; i < orderList.length; i++) {
            workspace.copy(node1.getPath(), newNode.getPath() + "/" + orderList[i]);
        }

        // check regarding orderList with the counter if nodes are added at the end
        int cnt = 0;
        NodeIterator iter = node2.getNodes();
        while (iter.hasNext()) {
            Node n = (Node) iter.nextNode();

            assertTrue(n.getName().equals(orderList[cnt]));
            cnt++;
        }
    }

    /**
     * An ItemExistsException is thrown if a node or property already exists at
     * destAbsPath.
     * @tck.config sameNameSibsFalseNodeType name of a node type that does not
     * allows same name siblings.
     * @tck.config nodeName3 name of a child node that does not allow same name
     * siblings..
     */
    public void testMoveNodesNodeExistsAtDestPath() throws RepositoryException {
        // create a parent node where allowSameNameSiblings are set to false
        Node snsfNode = testRootNode.addNode(nodeName3, sameNameSibsFalseNodeType.getName());
        testRootNode.save();

        String dstAbsPath = snsfNode.getPath() + "/" + node1.getName();
        workspace.copy(node1.getPath(), dstAbsPath);

        // try to copy again the node to same destAbsPath
        // property already exist
        try {
            workspace.move(node1.getPath(), dstAbsPath);
            fail("Node exists below '" + dstAbsPath + "'. Test should fail.");
        } catch (ItemExistsException e) {
            // successful
        }
    }
}