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
     * VersionManager.merge(): nonversionable subNode N: if it has no versionable
     * ancestor, then it is updated to reflect the state of its corresponding
     * node.<br>
     */
    public void testMergeNodeNonVersionableSubNodeNonVersionableAncestorJcr2() throws RepositoryException {
        String nodeToMergePath = nodeName1 + "/" + nodeName2 + "/" + nodeName3;

        // node to merge in second workspace
        Node nodeToMerge = testRootNodeW2.getNode(nodeToMergePath);
        // corresponding node to nodeToMerge in default workspace
        Node correspondingNode = testRootNode.getNode(nodeToMergePath);

        // modify value for non'v node in workspace2 so we can check if node in workspace2 after merge is updated
        // to reflect the state of its corresponding node in default workspace....
        nodeToMerge.setProperty(propertyName1, CHANGED_STRING);
        nodeToMerge.getSession().save();
        nodeToMerge.getSession().getWorkspace().getVersionManager().merge(
                nodeToMerge.getPath(), workspace.getName(), true);

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
        String changedString = CHANGED_STRING + System.currentTimeMillis();
        String nvSubNodePath = nodeName2 + "/" + nodeName3;

        // versionable ancestor to merge in first workspace (N)
        Node n = testRootNodeW2.getNode(nodeName1);

        // versionable ancestor to merge in second workspace (N')
        Node np = testRootNodeW2.getNode(nodeName1);

        // checkout N and make change
        n.checkout();
        Node nvSubNode = n.getNode(nvSubNodePath);
        nvSubNode.setProperty(propertyName1, changedString);
        n.save();
        n.checkin();

        // merge change into N'
        np.merge(workspaceW2.getName(), true);

        // corresponding node to nvSubNode in 2nd workspace
        Node nvSubNodeP = np.getNode(nvSubNodePath);

        // test if modification on N was merged into N' subnode
        assertTrue(nvSubNodeP.getProperty(propertyName1).getString().equals(changedString));
    }

    /**
     * VersionManager.merge(): nonversionable subNode N: if the merge result of its
     * nearest versionable ancestor is update,<br> then it is updated to reflect
     * the state of its corresponding node.<br>
     */
    public void testMergeNodeNonVersionableSubNodeUpdateJcr2() throws RepositoryException {
        // modify non versionable subnode so we can check if it's updated after merge
        String changedString = CHANGED_STRING + System.currentTimeMillis();
        String nvSubNodePath = nodeName2 + "/" + nodeName3;

        // versionable ancestor to merge in first workspace (N)
        Node n = testRootNodeW2.getNode(nodeName1);

        // versionable ancestor to merge in second workspace (N')
        Node np = testRootNodeW2.getNode(nodeName1);

        // checkout N and make change
        n.getSession().getWorkspace().getVersionManager().checkout(n.getPath());
        Node nvSubNode = n.getNode(nvSubNodePath);
        nvSubNode.setProperty(propertyName1, changedString);
        n.getSession().save();
        n.getSession().getWorkspace().getVersionManager().checkin(n.getPath());

        // merge change into N'
        np.getSession().getWorkspace().getVersionManager().merge(np.getPath(), workspaceW2.getName(), true);

        // corresponding node to nvSubNode in 2nd workspace
        Node nvSubNodeP = np.getNode(nvSubNodePath);

        // test if modification on N was merged into N' subnode
        assertTrue(nvSubNodeP.getProperty(propertyName1).getString().equals(changedString));
    }

    /**
     * Node.merge(): nonversionable subNode N: is left unchanged if the nearest
     * versionable ancestor has state leave.<br>
     */
    public void testMergeNodeNonVersionableSubNodeLeave() throws RepositoryException {
        // modify non versionable subnode so we can check if it's updated after merge
        String changedString = CHANGED_STRING + System.currentTimeMillis();
        String nvSubNodePath = nodeName2 + "/" + nodeName3;

        // versionable ancestor to merge in first workspace (N)
        Node n = testRootNodeW2.getNode(nodeName1);

        // versionable ancestor to merge in second workspace (N')
        Node np = testRootNodeW2.getNode(nodeName1);

        // checkout N' and make change
        np.checkout();
        Node nvSubNodeP = np.getNode(nvSubNodePath);
        nvSubNodeP.setProperty(propertyName1, changedString);
        np.save();
        np.checkin();

        // merge into N'
        np.merge(workspaceW2.getName(), true);

        // corresponding node to nvSubNode in 2nd workspace
        Node nvSubNode = np.getNode(nvSubNodePath);

        // test if modification on N' was not modified
        assertTrue(nvSubNode.getProperty(propertyName1).getString().equals(changedString));
    }

    /**
     * VersionManager.merge(): nonversionable subNode N: is left unchanged if the nearest
     * versionable ancestor has state leave.<br>
     */
    public void testMergeNodeNonVersionableSubNodeLeaveJcr2() throws RepositoryException {
        // modify non versionable subnode so we can check if it's updated after merge
        String changedString = CHANGED_STRING + System.currentTimeMillis();
        String nvSubNodePath = nodeName2 + "/" + nodeName3;

        // versionable ancestor to merge in first workspace (N)
        Node n = testRootNodeW2.getNode(nodeName1);

        // versionable ancestor to merge in second workspace (N')
        Node np = testRootNodeW2.getNode(nodeName1);

        // checkout N' and make change
        np.getSession().getWorkspace().getVersionManager().checkout(np.getPath());
        Node nvSubNodeP = np.getNode(nvSubNodePath);
        nvSubNodeP.setProperty(propertyName1, changedString);
        np.getSession().save();
        np.getSession().getWorkspace().getVersionManager().checkin(np.getPath());

        // merge into N'
        np.getSession().getWorkspace().getVersionManager().merge(np.getPath(), workspaceW2.getName(), true);

        // corresponding node to nvSubNode in 2nd workspace
        Node nvSubNode = np.getNode(nvSubNodePath);

        // test if modification on N' was not modified
        assertTrue(nvSubNode.getProperty(propertyName1).getString().equals(changedString));
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
        testRootNode.getSession().save();

        log.println("test nodes created successfully on " + workspace.getName());

        // clone the newly created node from src workspace into second workspace
        workspaceW2.clone(workspace.getName(), topVNode.getPath(), topVNode.getPath(), true);
        log.println(topVNode.getPath() + " cloned on " + superuserW2.getWorkspace().getName() + " at " + topVNode.getPath());

        testRootNodeW2 = (Node) superuserW2.getItem(testRoot);
    }

}
