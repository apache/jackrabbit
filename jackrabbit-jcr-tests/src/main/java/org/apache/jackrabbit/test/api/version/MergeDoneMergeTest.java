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
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.version.Version;

/**
 * <code>MergeDoneMergeTest</code> contains test dealing with nodes on which
 * doneMerge is called.
 *
 * @test
 * @sources MergeDoneMergeTest.java
 * @executeClass org.apache.jackrabbit.test.api.version.MergeDoneMergeTest
 * @keywords versioning
 */
public class MergeDoneMergeTest extends AbstractMergeTest {
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
     * Node.doneMerge(V) throws VersionException if V is not among the Vs in the
     * jcr:mergeFailed prop. <br> with adding it to jcr:predecessors.<br>
     * Branches will be joined.<br>
     */
    public void testMergeNodeDoneMerge() throws RepositoryException {
        // create 2 independent versions for a node and its corresponding node
        // so merge fails for this node

        // default workspace
        Node originalNode = testRootNode.getNode(nodeName1);
        originalNode.checkout();
        originalNode.checkin();

        // second workspace
        nodeToMerge.checkin();

        // "merge" the clonedNode with the newNode from the default workspace
        nodeToMerge.checkout();
        nodeToMerge.merge(workspace.getName(), true);

        // get predecessors
        Version[] predecessors = nodeToMerge.getBaseVersion().getPredecessors();
        // get mergeFailed property
        Property mergeFailedProperty = nodeToMerge.getProperty(jcrMergeFailed);
        Value[] mergeFailedReferences = mergeFailedProperty.getValues();

        for (int i = 0; i < mergeFailedReferences.length; i++) {
            String uuid = mergeFailedReferences[i].getString();
            nodeToMerge.doneMerge((Version) superuser.getNodeByUUID(uuid));
        }

        // check mergeFailed property - reference moved to predecessor
        if (nodeToMerge.hasProperty(jcrMergeFailed)) {
            Property mergeFailedPropertyAfterCancelMerge = nodeToMerge.getProperty(jcrMergeFailed);
            Value[] mergeFailedReferencesAfterCancelMerge = mergeFailedPropertyAfterCancelMerge.getValues();
            assertTrue(mergeFailedReferences.length > mergeFailedReferencesAfterCancelMerge.length);
        }
    }

    /**
     * initialize a versionable node on default and second workspace
     */
    protected void initNodes() throws RepositoryException {
        // create a versionable node
        // nodeName1
        Node topVNode = testRootNode.addNode(nodeName1, versionableNodeType);
        topVNode.setProperty(propertyName1, topVNode.getName());

        // save default workspace
        testRootNode.save();
        topVNode.checkin();
        topVNode.checkout();

        log.println("test nodes created successfully on " + workspace.getName());

        // clone the newly created node from src workspace into second workspace
        workspaceW2.clone(workspace.getName(), topVNode.getPath(), topVNode.getPath(), true);
        log.println(topVNode.getPath() + " cloned on " + superuserW2.getWorkspace().getName() + " at " + topVNode.getPath());

        testRootNodeW2 = (Node) superuserW2.getItem(testRoot);
    }

}