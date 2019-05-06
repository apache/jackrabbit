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

import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

/**
 * <code>WorkspaceCopySameNameSibsTest</code> contains tests for copying nodes
 * as same name siblings in one workspace.
 *
 */
public class WorkspaceCopySameNameSibsTest extends AbstractWorkspaceSameNameSibsTest {

    protected String getOtherWorkspaceName() throws NotExecutableException {
        return workspace.getName();
    }

    protected void initNodesW2() throws RepositoryException {
        // nothing to do.
    }

    /**
     * If ordering is supported by the node type of the parent node of the new
     * location, then the newly moved node is appended to the end of the child
     * node list.
     */
    public void testCopyNodesOrderingSupportedByParent() throws RepositoryException {
        // test assumes that repositry supports Orderable Child Node Support (optional)
        String[] orderList = {nodeName1, nodeName2, nodeName3};

        // copy node three times below a node and check the order
        for (int i = 0; i < orderList.length; i++) {
            workspace.copy(node1.getPath(), node2.getPath() + "/" + orderList[i]);
        }

        // check regarding orderList if nodes are added at the end
        int cnt = 0;
        NodeIterator iter = node2.getNodes();
        while (iter.hasNext()) {
            Node n = iter.nextNode();

            assertTrue(n.getName().equals(orderList[cnt]));
            cnt++;
        }
    }

    /**
     * An ItemExistsException is thrown if a node or property already exists at
     * destAbsPath.
     * <ul>
     * <li>{@code sameNameSibsFalseNodeType} name of a node type that does not
     * allows same name siblings.
     * <li>{@code nodeName3} name of a child node that does not allow same name
     * siblings..
     * </ul>
     */
    public void testCopyNodesNodeExistsAtDestPath() throws RepositoryException {
        // create a parent node where allowSameNameSiblings are set to false
        Node snsfNode = testRootNode.addNode(nodeName3, sameNameSibsFalseNodeType.getName());
        testRootNode.getSession().save();

        String dstAbsPath = snsfNode.getPath() + "/" + node1.getName();
        workspace.copy(node1.getPath(), dstAbsPath);

        // try to copy again the node to same destAbsPath where node already exists
        try {
            workspace.copy(node1.getPath(), dstAbsPath);
            fail("Node exists below '" + dstAbsPath + "'. Test should fail.");
        } catch (ItemExistsException e) {
            // successful
        }
    }

    /**
     * NO ItemExistsException is thrown if a node already exists at destAbsPath
     * and the node allows same-name-siblings.
     * <ul>
     * <li>{@code sameNameSibsTrueNodeType} name of a node type that
     * allows same name siblings.
     * <li>{@code nodeName3} name of a child node that allows children with
     * same name.
     * </ul>
     */
    public void testCopyNodesNodeExistsAtDestPath2() throws RepositoryException {
        // create a parent node where allowSameNameSiblings are set to true
        Node snsfNode = testRootNode.addNode(nodeName3, sameNameSibsTrueNodeType.getName());
        testRootNode.getSession().save();

        String dstAbsPath = snsfNode.getPath() + "/" + node1.getName();
        workspace.copy(node1.getPath(), dstAbsPath);

        // try to copy again the node to same destAbsPath where node already exists
        // must succeed
        workspace.copy(node1.getPath(), dstAbsPath);

        // make sure the parent now has 2 children with the same name
        NodeIterator it = snsfNode.getNodes(node1.getName());
        long size = it.getSize();
        if (it.getSize() == -1) {
            size = 0;
            while (it.hasNext()) {
                it.nextNode();
                size++;
            }
        }
        assertEquals("After second copy 2 same-name-siblings must exist",2, size);
    }
}
