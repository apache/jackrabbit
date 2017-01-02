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
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.version.VersionManager;

import org.apache.jackrabbit.test.NotExecutableException;
/**
 * <code>MergeActivityTest</code> contains tests dealing with merging activities
 *
 */

public class MergeActivityTest extends AbstractMergeTest {

    String newValue;

    Node activityNode;

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        if (activityNode != null) {
            superuser.getWorkspace().getVersionManager().removeActivity(activityNode);
            activityNode = null;
        }
        super.tearDown();
    }

    public void testMergeActivity() throws RepositoryException {
        String p1 = testRootNodeW2.getProperty(nodeName1 + "/" + propertyName1).getString();
        String p2 = testRootNodeW2.getProperty(nodeName2 + "/" + propertyName1).getString();
        assertEquals("Cloned node has wrong property on node 1.", nodeName1, p1);
        assertEquals("Cloned node has wrong property on node 2.", nodeName2, p2);

        VersionManager vm2 = testRootNodeW2.getSession().getWorkspace().getVersionManager();
        NodeIterator iter = vm2.merge(activityNode);
        if (iter.hasNext()) {
            StringBuffer failed = new StringBuffer();
            while (iter.hasNext()) {
                failed.append(iter.nextNode().getPath());
                failed.append(", ");
            }
            fail("Merge must not fail. failed nodes: " + failed);
            return;
        }

        p1 = testRootNodeW2.getProperty(nodeName1 + "/" + propertyName1).getString();
        p2 = testRootNodeW2.getProperty(nodeName2 + "/" + propertyName1).getString();
        assertEquals("Activity merge did not restore property on node 1.", newValue, p1);
        assertEquals("Activity merge did not restore property on node 2.", newValue, p2);
    }

    /**
     * initialize a versionable node on default and second workspace
     */
    protected void initNodes() throws RepositoryException, NotExecutableException {

        checkSupportedOption(Repository.OPTION_ACTIVITIES_SUPPORTED);

        VersionManager versionManager = testRootNode.getSession().getWorkspace().getVersionManager();

        // create 2 a versionable nodes
        Node node1 = testRootNode.addNode(nodeName1, versionableNodeType);
        node1.setProperty(propertyName1, nodeName1);
        String path1 = node1.getPath();
        Node node2 = testRootNode.addNode(nodeName2, versionableNodeType);
        node2.setProperty(propertyName1, nodeName2);
        String path2 = node2.getPath();

        // save default workspace
        testRootNode.getSession().save();
        versionManager.checkin(path1);
        versionManager.checkin(path2);

        log.println("test nodes created successfully on " + workspace.getName());

        // clone the newly created node from src workspace into second workspace
        // todo clone on testRootNode does not seem to work.
        // workspaceW2.clone(workspace.getName(), testRootNode.getPath(), testRootNode.getPath(), true);
        workspaceW2.clone(workspace.getName(), path1, path1, true);
        workspaceW2.clone(workspace.getName(), path2, path2, true);

        testRootNodeW2 = (Node) superuserW2.getItem(testRoot);

        activityNode = versionManager.createActivity("foobar");
        versionManager.setActivity(activityNode);

        // update properties on source nodes

        versionManager.checkout(path1);
        versionManager.checkout(path2);

        newValue = String.valueOf(System.currentTimeMillis());
        node1.setProperty(propertyName1, newValue);
        node2.setProperty(propertyName1, newValue);
        testRootNode.getSession().save();

        versionManager.checkin(path1);
        versionManager.checkin(path2);
    }
}
