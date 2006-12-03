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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.ItemExistsException;

/**
 * <code>WorkspaceCloneReferenceableTest</code> contains tests for cloning
 * referenceable nodes between workspaces.
 *
 * @test
 * @sources WorkspaceCloneReferenceableTest.java
 * @executeClass org.apache.jackrabbit.test.api.WorkspaceCloneReferenceableTest
 * @keywords level2
 */
public class WorkspaceCloneReferenceableTest extends AbstractWorkspaceReferenceableTest {

    /**
     * In the case of referenceable nodes clone preserves the node's UUID so
     * that the new node in the destination workspcace has the same UUID as the
     * node in the source workspace.
     */
    public void testCloneNodesReferenceableNodesOriginalUUID() throws RepositoryException {
        // add mixin referenceable to node1
        addMixinReferenceableToNode(node1);

        // copy referenceable node below non-referenceable node
        String dstAbsPath = node2W2.getPath() + "/" + node1.getName();
        workspaceW2.clone(workspace.getName(), node1.getPath(), dstAbsPath, true);

        // uuid of copied node should be different than original node uuid
        String originalUUID = node1.getUUID();
        Node copiedNode = node2W2.getNode(node1.getName());
        String copiedUUID = copiedNode.getUUID();

        assertTrue(originalUUID.equals(copiedUUID));
    }

    /**
     * If removeExisting is true then the existing node is removed from its
     * current location and the cloned node with the same UUID from srcWorkspace
     * is copied to this workspace as part of the copied subtree (that is, not
     * into the former location of the old node). The subtree of the cloned node
     * will reflect the clones state in srcWorkspace, in other words the
     * existing node will be moved and changed.
     */
    public void testCloneNodesRemoveExistingTrue() throws RepositoryException {
        // add mixin referenceable to node1
        addMixinReferenceableToNode(node1);

        // clone a node from default workspace to have the same uuid on second workspace
        String dstAbsPath = node2W2.getPath() + "/" + nodeName2;
        workspaceW2.clone(workspace.getName(), node1.getPath(), dstAbsPath, true);
        Node clonedNode = node2W2.getNode(nodeName2);

        // clone node1 from default workspace to second workspace
        dstAbsPath = node2W2.getPath() + "/" + nodeName3;
        workspaceW2.clone(workspace.getName(), node1.getPath(), dstAbsPath, true);
        Node clonedNode2 = node2W2.getNode(nodeName3);

        // because a node with same uuid exists (cloned node in earlier step - nodeName2), the existing node (and its subtree)
        // should be removed ...
        assertFalse(node2W2.hasNode(nodeName2));
    }

    /**
     * If removeExisting is false then a UUID collision causes this method to
     * throw a ItemExistsException and no changes are made.
     */
    public void testCloneNodesRemoveExistingFalse() throws RepositoryException {
        // add mixin referenceable to node1
        addMixinReferenceableToNode(node1);

        // clone a node from default workspace to have the same uuid on second workspace
        workspaceW2.clone(workspace.getName(), node1.getPath(), testRootNodeW2.getPath() + "/" + nodeName3, false);

        // clone node1 from default workspace to second workspace
        try {
            workspaceW2.clone(workspace.getName(), node1.getPath(), testRootNodeW2.getPath() + "/" + nodeName4, false);
            fail("If removeExisting is false then a UUID collision should throw a ItemExistsException");
        } catch (ItemExistsException e) {
            // successful
        }
    }

    /**
     * The clone method clones both referenceable and nonreferenceable nodes.
     */
    public void testCloneNodesReferenceableAndNonreferenceable() throws RepositoryException {
        // clone referenceable node

        // add mixin referenceable to node1
        addMixinReferenceableToNode(node1);
        if (node1.isNodeType(mixReferenceable)) {
            workspaceW2.clone(workspace.getName(), node1.getPath(), testRootNodeW2.getPath() + "/" + nodeName3, false);
        } else {
            fail("Node should be referenceable.");
        }

        // clone nonreferenceable node
        if (node2.isNodeType(mixReferenceable)) {
            fail("Node should not be referenceable.");
        } else {
            workspaceW2.clone(workspace.getName(), node2.getPath(), testRootNodeW2.getPath() + "/" + nodeName4, false);
            assertTrue(testRootNodeW2.hasNode(nodeName4));
        }
    }

}
