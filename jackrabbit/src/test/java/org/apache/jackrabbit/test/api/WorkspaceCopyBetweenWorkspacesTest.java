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

import javax.jcr.AccessDeniedException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Repository;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;

/**
 * <code>WorkspaceCopyBetweenWorkspacesTest</code> contains tests for copying
 * nodes between workspace.<br>
 *
 * @test
 * @sources WorkspaceCopyBetweenWorkspacesTest.java
 * @executeClass org.apache.jackrabbit.test.api.WorkspaceCopyBetweenWorkspacesTest
 * @keywords level2
 */
public class WorkspaceCopyBetweenWorkspacesTest extends AbstractWorkspaceCopyBetweenTest {

    /**
     * Operation is performed entirely within the persistent workspace, it does
     * not involve transient storage and therefore does not require a save
     */
    public void testCopyNodes() throws RepositoryException {
        // copy referenceable node below non-referenceable node
        String dstAbsPath = node2W2.getPath() + "/" + node1.getName();
        workspaceW2.copy(workspace.getName(), node1.getPath(), dstAbsPath);

        // there should not be any pending changes after copy
        assertFalse(superuserW2.hasPendingChanges());
    }

    /**
     * A NoSuchWorkspaceException is thrown if srcWorkspace does not exist.
     */
    public void testCopyNodesInvalidWorkspace() throws RepositoryException {
        // copy referenceable node below non-referenceable node to a non-existing workspace
        String dstAbsPath = node2W2.getPath() + "/" + node1.getName();
        try {
            superuserW2.getWorkspace().copy(getNonExistingWorkspaceName(superuser), node1.getPath(), dstAbsPath);
            fail("Invalid Source Workspace should throw NoSuchWorkspaceException.");
        } catch (NoSuchWorkspaceException e) {
            // successful
        }
    }


    /**
     * The destAbsPath provided must not have an index on its final element. If
     * it does, then a RepositoryException is thrown. Strictly speaking, the
     * destAbsPath parameter is actually an absolute path to the parent node of
     * the new location, appended with the new name desired for the copied node.
     * It does not specify a position within the child node ordering.
     */
    public void testCopyNodesAbsolutePath() {
        try {
            // copy referenceable node to an absolute path containing index
            String dstAbsPath = node2W2.getPath() + "/" + node1.getName() + "[2]";
            workspaceW2.copy(workspace.getName(), node1.getPath(), dstAbsPath);
            fail("Copying a node to an absolute path containing index should not be possible.");
        } catch (RepositoryException e) {
            // successful
        }
    }

    /**
     * A ConstraintViolationException is thrown if the operation would violate a
     * node-type or other implementation-specific constraint.
     */
    public void testCopyNodesConstraintViolationException() throws RepositoryException {
        // if parent node is nt:base then no sub nodes can be created
        Node subNodesNotAllowedNode = testRootNodeW2.addNode(nodeName3, ntBase);
        testRootNodeW2.save();
        try {
            String dstAbsPath = subNodesNotAllowedNode.getPath() + "/" + node2.getName();
            workspaceW2.copy(workspace.getName(), node2.getPath(), dstAbsPath);
            fail("Copying a node below a node which can not have any sub nodes should throw a ConstraintViolationException.");
        } catch (ConstraintViolationException e) {
            // successful
        }
    }


    /**
     * An AccessDeniedException is thrown if the current session (i.e., the
     * session that was used to acquire this Workspace object) does not have
     * sufficient access permissions to complete the operation.
     */
    public void testCopyNodesAccessDenied() throws RepositoryException {
        Session readOnlySuperuser = helper.getReadOnlySession();
        try {
            String dstAbsPath = node2.getPath() + "/" + node1.getName();
            try {
                readOnlySuperuser.getWorkspace().copy(workspaceW2.getName(), node1W2.getPath(), dstAbsPath);
                fail("Copy in a read-only session should throw an AccessDeniedException.");
            } catch (AccessDeniedException e) {
                // successful
            }
        } finally {
            readOnlySuperuser.logout();
        }
    }

    /**
     * A PathNotFoundException is thrown if the node at srcAbsPath or the parent
     * of the new node at destAbsPath does not exist.
     */
    public void testCopyNodesPathNotExisting() throws RepositoryException {

        String srcAbsPath = node1.getPath();
        String dstAbsPath = node2W2.getPath() + "/" + node1.getName();

        // srcAbsPath is not existing
        String invalidSrcPath = srcAbsPath + "invalid";
        try {
            workspaceW2.copy(workspace.getName(), invalidSrcPath, dstAbsPath);
            fail("Not existing source path '" + invalidSrcPath + "' should throw PathNotFoundException.");
        } catch (PathNotFoundException e) {
            // successful
        }

        // dstAbsPath parent is not existing
        String invalidDstParentPath = node2W2.getPath() + "invalid/" + node1.getName();
        try {
            workspaceW2.copy(workspace.getName(), srcAbsPath, invalidDstParentPath);
            fail("Not existing destination parent path '" + invalidDstParentPath + "' should throw PathNotFoundException.");
        } catch (PathNotFoundException e) {
            // successful
        }
    }

    /**
     * A LockException is thrown if a lock prevents the copy.
     */
    public void testCopyNodesLocked() throws RepositoryException, NotExecutableException {
        if (!isSupported(Repository.OPTION_LOCKING_SUPPORTED)) {
            throw new NotExecutableException("Repository does not support locking.");
        }
        // we assume repository supports locking
        String dstAbsPath = node2W2.getPath() + "/" + node1.getName();

        // get lock target node in destination wsp through other session
        Node lockTarget = (Node) rwSessionW2.getItem(node2W2.getPath());

        // add mixin "lockable" to be able to lock the node
        if (!lockTarget.getPrimaryNodeType().isNodeType(mixLockable)) {
            lockTarget.addMixin(mixLockable);
            lockTarget.getParent().save();
        }

        // lock dst parent node using other session
        lockTarget.lock(true, true);

        try {
            workspaceW2.copy(workspace.getName(), node1.getPath(), dstAbsPath);
            fail("LockException was expected.");
        } catch (LockException e) {
            // successful
        } finally {
            lockTarget.unlock();
        }
    }

}
