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
import javax.jcr.version.VersionManager;

/**
 * <code>MergeShallowTest</code> contains tests dealing with general shallow
 * merge calls.
 *
 */

public class MergeShallowTest extends AbstractMergeTest {

    /**
     * node to merge
     */
    Node nodeToMerge;

    String newValue;

    protected void setUp() throws Exception {
        super.setUp();

        nodeToMerge = testRootNodeW2.getNode(nodeName1);

    }

    protected void tearDown() throws Exception {
        nodeToMerge = null;
        super.tearDown();
    }

    public void testMergeRecursive() throws RepositoryException {
        VersionManager vm2 = testRootNodeW2.getSession().getWorkspace().getVersionManager();
        NodeIterator iter = vm2.merge(nodeToMerge.getPath(),
                superuser.getWorkspace().getName(), true, false);
        if (iter.hasNext()) {
            StringBuffer failed = new StringBuffer();
            while (iter.hasNext()) {
                failed.append(iter.nextNode().getPath());
                failed.append(", ");
            }
            fail("Merge must not fail. failed nodes: " + failed);
            return;
        }

        String p1 = nodeToMerge.getProperty(propertyName1).getString();
        String p2 = nodeToMerge.getProperty(nodeName2 + "/" + propertyName1).getString();
        assertEquals("Recursive merge did not restore property on level 1.", newValue, p1);
        assertEquals("Recursive merge did not restore property on level 2.", newValue, p2);

    }

    public void testMergeShallow() throws RepositoryException {
        String oldP2 = nodeToMerge.getProperty(nodeName2 + "/" + propertyName1).getString();

        VersionManager vm2 = testRootNodeW2.getSession().getWorkspace().getVersionManager();
        NodeIterator iter = vm2.merge(nodeToMerge.getPath(),
                superuser.getWorkspace().getName(), true, true);
        if (iter.hasNext()) {
            StringBuffer failed = new StringBuffer();
            while (iter.hasNext()) {
                failed.append(iter.nextNode().getPath());
                failed.append(", ");
            }
            fail("Merge must not fail. failed nodes: " + failed);
            return;
        }

        String p1 = nodeToMerge.getProperty(propertyName1).getString();
        String p2 = nodeToMerge.getProperty(nodeName2 + "/" + propertyName1).getString();
        assertEquals("Shallow merge did not restore property on level 1.", newValue, p1);
        assertEquals("Shallow merge did restore property on level 2.", oldP2, p2);

    }

    /**
     * initialize a versionable node on default and second workspace
     */
    protected void initNodes() throws RepositoryException {

        VersionManager versionManager = testRootNode.getSession().getWorkspace().getVersionManager();

        // create a versionable node
        // nodeName1
        Node topVNode = testRootNode.addNode(nodeName1, versionableNodeType);
        topVNode.setProperty(propertyName1, topVNode.getName());
        String path = topVNode.getPath();

        // create a versionable sub node
        // nodeName1/nodeName2
        Node subNvNode = topVNode.addNode(nodeName2, versionableNodeType);
        subNvNode.setProperty(propertyName1, subNvNode.getName());
        String path2 = subNvNode.getPath();

        // save default workspace
        testRootNode.getSession().save();
        versionManager.checkin(path);
        versionManager.checkin(path2);

        log.println("test nodes created successfully on " + workspace.getName());

        // clone the newly created node from src workspace into second workspace
        // todo clone on testRootNode does not seem to work.
        // workspaceW2.clone(workspace.getName(), testRootNode.getPath(), testRootNode.getPath(), true);
        workspaceW2.clone(workspace.getName(), topVNode.getPath(), topVNode.getPath(), true);
        log.println(topVNode.getPath() + " cloned on " + superuserW2.getWorkspace().getName() + " at " + topVNode.getPath());

        testRootNodeW2 = (Node) superuserW2.getItem(testRoot);

        versionManager.checkout(path);
        versionManager.checkout(path2);

        // update properties on source nodes
        newValue = String.valueOf(System.currentTimeMillis());

        Node n1 = testRootNode.getNode(nodeName1);
        n1.setProperty(propertyName1, newValue);
        Node n2 = n1.getNode(nodeName2);
        n2.setProperty(propertyName1, newValue);
        testRootNode.getSession().save();

        VersionManager vm1 = testRootNode.getSession().getWorkspace().getVersionManager();
        vm1.checkpoint(n2.getPath());
        vm1.checkpoint(n1.getPath());
    }
}
