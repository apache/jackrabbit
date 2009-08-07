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

import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.version.VersionException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Property;
import javax.jcr.Value;
import javax.jcr.MergeException;
import javax.jcr.Session;
import javax.jcr.Repository;
import javax.jcr.lock.LockException;

/**
 * <code>MergeNodeTest</code> contains tests dealing with general merge node
 * calls.
 *
 * @test
 * @sources MergeNodeTest.java
 * @executeClass org.apache.jackrabbit.test.api.version.MergeNodeTest
 * @keywords versioning
 */

public class MergeNodeTest extends AbstractMergeTest {

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
     * Node.merge(): InvalidItemStateException if unsaved changes within the
     * current Session<br>
     */
    public void testMergeNodeWithUnsavedStates() throws RepositoryException {
        // set a property and do not save workspace
        nodeToMerge.setProperty(propertyName1, CHANGED_STRING);
        try {
            nodeToMerge.merge(workspace.getName(), false);
            fail("InvalidItemStateException if unsaved changes within the current Session was expected.");
        } catch (InvalidItemStateException e) {
            // success
        }
    }

    /**
     * Perform a merge on a node with a unkwnown workspacename
     */
    public void testMergeUnknownWorkspaceName() throws RepositoryException {
        try {
            nodeToMerge.merge(getNonExistingWorkspaceName(superuser), false);
        } catch (NoSuchWorkspaceException e) {
            // success expected exception
        }
    }

    /**
     * Node.merge(): If this node does not have a corresponding node in the
     * indicated workspace <br> then the merge method returns quietly and no
     * changes are made.<br>
     */
    public void testMergeNodeNonCorrespondingNode() throws RepositoryException {
        // create new node - this node has no corresponding node in default workspace
        Node subNode = nodeToMerge.addNode(nodeName3, versionableNodeType);
        subNode.setProperty(propertyName1, CHANGED_STRING);
        superuserW2.save();
        subNode.checkin();

        subNode.merge(workspace.getName(), true);
        assertTrue(subNode.getProperty(propertyName1).getString().equals(CHANGED_STRING));
    }

    /**
     * Node.merge(): versionable subNode N checked-in: If V is neither a
     * successor of, predecessor of, nor identical with V', then the merge
     * result for N is failed<br>
     */
    public void testMergeNodeVersionAmbiguous() throws RepositoryException {
        // create 2 independent versions for a node and its corresponding node
        // so merge fails for this node

        // default workspace
        Node originalNode = testRootNode.getNode(nodeName1);
        originalNode.checkout();
        originalNode.checkin();

        // second workspace
        nodeToMerge.checkin();

        // "merge" the clonedNode with the newNode from the default workspace
        // besteffort set to false to stop at the first failure
        try {
            nodeToMerge.checkout();
            nodeToMerge.merge(workspace.getName(), false);
            fail("Node has ambigous versions. Merge must throw a MergeException");
        } catch (MergeException e) {
            // success if the merge exception thrown
        }
    }

    /**
     * Node.merge(): bestEffort is true > any merge-failure (represented by the
     * version in the workspace) is reported in the jcrMergeFailed property<br>
     */
    public void testMergeNodeBestEffortTrueCheckMergeFailedProperty() throws RepositoryException {
        // create 2 independent versions for a node and its corresponding node
        // so merge fails for this node

        // default workspace
        Node originalNode = testRootNode.getNode(nodeName1);
        originalNode.checkout();
        originalNode.checkin();

        // second workspace
        nodeToMerge.checkin();

        // "merge" the clonedNode with the newNode from the default workspace
        // besteffort set to true to report all failures
        nodeToMerge.checkout();
        nodeToMerge.merge(workspace.getName(), true);

        // success merge exception was raised as expected
        // jcrMergeFailed should contains reference to the V' as it is a different branche
        String expectedReferenceUUID = originalNode.getBaseVersion().getUUID();
        Property mergeFailedProperty = nodeToMerge.getProperty(jcrMergeFailed);
        Value[] references = mergeFailedProperty.getValues();
        boolean referenceFound = false;
        if (references != null) {
            for (int i = 0; i < references.length; i++) {
                String referenceUUID = references[i].getString();
                if (referenceUUID.equals(expectedReferenceUUID)) {
                    referenceFound = true;
                    break; // it's not necessary to loop thru all the references
                }
            }

            assertTrue("reference to expected version that give the failure wasnt found in the mergeFailed", referenceFound);
        }
    }

    /**
     * if mergeFailedProperty is present > VersionException<br>
     */
    public void disable_testMergeNodeForceFailure() throws RepositoryException {
        // create 2 independent versions for a node and its corresponding node
        // so merge fails for this node

        // default workspace
        Node originalNode = testRootNode.getNode(nodeName1);
        originalNode.checkout();
        originalNode.checkin();

        // second workspace
        nodeToMerge.checkin();

        // "merge" the clonedNode with the newNode from the default workspace
        // besteffort set to true to report all failures
        nodeToMerge.checkout();
        nodeToMerge.merge(workspace.getName(), true);

        try {
            nodeToMerge.merge(workspace.getName(), true);
            fail("Merge failed for node in earlier merge operations. Because the mergeFailedProperty is present, merge must throw a VersionException");
        } catch (VersionException e) {
            // success version exception expected
        }
    }

    /**
     * Node.merge(): bestEffort is false and any merge fails a MergeException is
     * thrown.<br>
     */
    public void testMergeNodeBestEffortFalse() throws RepositoryException {
        /// create successor versions for a node
        // so merge fails for this node

        // default workspace
        Node originalNode = testRootNode.getNode(nodeName1);
        originalNode.checkout();
        originalNode.checkin();

        // "merge" the clonedNode with the newNode from the default workspace
        // merge, besteffort set to false
        try {
            nodeToMerge.merge(workspace.getName(), false);
            fail("bestEffort is false and any merge should throw a MergeException.");
        } catch (MergeException e) {
            // successful
        }
    }

    /**
     * A MergeVersionException is thrown if bestEffort is false and a
     * versionable node is encountered whose corresponding node's base version
     * is on a divergent branch from this node's base version.
     */
    public void testMergeNodeBestEffortFalseAmbiguousVersions() throws RepositoryException {
        /// create 2 independent base versions for a node and its corresponding node
        // so merge fails for this node

        // default workspace
        Node originalNode = testRootNode.getNode(nodeName1);
        originalNode.checkout();
        originalNode.checkin();

        // second workspace
        nodeToMerge.checkin();

        // "merge" the clonedNode with the newNode from the default workspace
        nodeToMerge.checkout();

        // merge, besteffort set to false
        try {
            nodeToMerge.merge(workspace.getName(), false);
            fail("BestEffort is false and corresponding node's version is ambiguous. Merge should throw a MergeException.");
        } catch (MergeException e) {
            // successful
        }
    }

    /**
     * Tests if a {@link LockException} is thrown when merge is called on a
     * locked node.
     * @throws NotExecutableException if repository does not support locking.
     */
    public void disable_testMergeLocked()
            throws NotExecutableException, RepositoryException {

        Session session = testRootNode.getSession();

        if (!isSupported(Repository.OPTION_LOCKING_SUPPORTED)) {
            throw new NotExecutableException("Locking is not supported.");
        }

        // try to make nodeToMerge lockable if it is not
        if (!nodeToMerge.isNodeType(mixLockable)) {
            if (nodeToMerge.canAddMixin(mixLockable)) {
                nodeToMerge.addMixin(mixLockable);
            } else {
                throw new NotExecutableException("Node " + nodeToMerge.getName() + " is not lockable and " +
                        "does not allow to add mix:lockable");
            }
        }
        nodeToMerge.getParent().save();

        // lock the node
        // remove first slash of path to get rel path to root
        String pathRelToRoot = nodeToMerge.getPath().substring(1);
        // access node through another session to lock it
        Session session2 = helper.getSuperuserSession();
        try {
            Node node2 = session2.getRootNode().getNode(pathRelToRoot);
            node2.lock(false, false);

            try {
                nodeToMerge.merge(workspace.getName(), false);
                fail("merge must throw a LockException if applied on a " +
                        "locked node");
            } catch (LockException e) {
                // success
            }

            node2.unlock();
        } finally {
            session2.logout();
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
        // todo clone on testRootNode does not seem to work.
        // workspaceW2.clone(workspace.getName(), testRootNode.getPath(), testRootNode.getPath(), true);
        workspaceW2.clone(workspace.getName(), topVNode.getPath(), topVNode.getPath(), true);
        log.println(topVNode.getPath() + " cloned on " + superuserW2.getWorkspace().getName() + " at " + topVNode.getPath());

        testRootNodeW2 = (Node) superuserW2.getItem(testRoot);
    }
}
