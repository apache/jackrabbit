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

import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.OnParentVersionAction;
import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;

/**
 * <code>WorkspaceRestoreTest</code> provides test methods for the {@link javax.jcr.Workspace#restore(javax.jcr.version.Version[], boolean)}
 * method.
 *
 * @test
 * @sources WorkspaceRestoreTest.java
 * @executeClass org.apache.jackrabbit.test.api.version.WorkspaceRestoreTest
 * @keywords versioning
 */
public class WorkspaceRestoreTest extends AbstractVersionTest {

    Session wSuperuser;

    Version version;
    Version version2;
    Version rootVersion;

    Node versionableNode2;
    Node wTestRoot;
    Node wVersionableNode;
    Node wVersionableNode2;
    Node wVersionableChildNode;

    Version wChildVersion;

    protected void setUp() throws Exception {
        super.setUp();

        version = versionableNode.checkin();
        versionableNode.checkout();
        version2 = versionableNode.checkin();
        versionableNode.checkout();
        rootVersion = versionableNode.getVersionHistory().getRootVersion();

        // build a second versionable node below the testroot
        try {
            versionableNode2 = createVersionableNode(testRootNode, nodeName2, versionableNodeType);
        } catch (RepositoryException e) {
            fail("Failed to create a second versionable node: " + e.getMessage());
        }
        try {
            wSuperuser = helper.getSuperuserSession(workspaceName);
        } catch (RepositoryException e) {
            fail("Failed to retrieve superuser session for second workspace '" + workspaceName + "': " + e.getMessage());
        }

        // test if the required nodes exist in the second workspace if not try to clone them
        try {
            testRootNode.getCorrespondingNodePath(workspaceName);
        } catch (ItemNotFoundException e) {
            // clone testRoot
            wSuperuser.getWorkspace().clone(superuser.getWorkspace().getName(), testRoot, testRoot, true);
        }

        try {
            versionableNode.getCorrespondingNodePath(workspaceName);
        } catch (ItemNotFoundException e) {
            // clone versionable node
            wSuperuser.getWorkspace().clone(superuser.getWorkspace().getName(), versionableNode.getPath(), versionableNode.getPath(), true);
        }

        try {
            versionableNode2.getCorrespondingNodePath(workspaceName);
        } catch (ItemNotFoundException e) {
            // clone second versionable node
            wSuperuser.getWorkspace().clone(superuser.getWorkspace().getName(), versionableNode2.getPath(), versionableNode2.getPath(), true);
        }

        try {
            // set node-fields (wTestRoot, wVersionableNode, wVersionableNode2)
            // and check versionable nodes out.
            wTestRoot = (Node) wSuperuser.getItem(testRootNode.getPath());

            wVersionableNode = wSuperuser.getNodeByUUID(versionableNode.getUUID());
            wVersionableNode.checkout();

            wVersionableNode2 = wSuperuser.getNodeByUUID(versionableNode2.getUUID());
            wVersionableNode2.checkout();

        } catch (RepositoryException e) {
            fail("Failed to setup test environment in workspace: " + e.toString());
        }

        // create persistent versionable CHILD-node below wVersionableNode in workspace 2
        // that is not present in the default workspace.
        try {
            wVersionableChildNode = createVersionableNode(wVersionableNode, nodeName4, versionableNodeType);
        } catch (RepositoryException e) {
            fail("Failed to create versionable child node in second workspace: " + e.getMessage());
        }

        // create a version of the versionable child node
        wVersionableChildNode.checkout();
        wChildVersion = wVersionableChildNode.checkin();
        wVersionableChildNode.checkout();
    }


    protected void tearDown() throws Exception {
        try {
            // remove all versionable nodes below the test
            versionableNode2.remove();
            wVersionableNode.remove();
            wVersionableNode2.remove();
            wTestRoot.save();
        } finally {
            if (wSuperuser != null) {
                wSuperuser.logout();
                wSuperuser = null;
            }
            version = null;
            version2 = null;
            rootVersion = null;
            versionableNode2 = null;
            wTestRoot = null;
            wVersionableNode = null;
            wVersionableNode2 = null;
            wVersionableChildNode = null;
            wChildVersion = null;
            super.tearDown();
        }
    }

    /**
     * Test if InvalidItemStateException is thrown if the session affected by
     * Workspace.restore(Version[], boolean) has pending changes.
     */
    public void testWorkspaceRestoreWithPendingChanges() throws RepositoryException {
        versionableNode.checkout();
        try {
            // modify node without calling save()
            versionableNode.setProperty(propertyName1, propertyValue);

            // create version in second workspace
            Version v = wVersionableNode.checkin();
            // try to restore that version
            superuser.getWorkspace().restore(new Version[]{v}, false);

            fail("InvalidItemStateException must be thrown on attempt to call Workspace.restore(Version[], boolean) in a session having any unsaved changes pending.");
        } catch (InvalidItemStateException e) {
            // success
        }
    }

    /**
     * Test if VersionException is thrown if the specified version array does
     * not contain a version that has a corresponding node in this workspace.
     */
    public void testWorkspaceRestoreHasCorrespondingNode() throws RepositoryException {
        try {
            superuser.getWorkspace().restore(new Version[]{wChildVersion}, false);
            fail("Workspace.restore(Version[], boolean) must throw VersionException if non of the specified versions has a corresponding node in the workspace.");
        } catch (VersionException e) {
            // success
        }
    }

    /**
     * Test if Workspace.restore(Version[], boolean) succeeds if the following two
     * preconditions are fulfilled:<ul>
     * <li>For every version V in S that corresponds to a missing node in the workspace,
     * there must also be a parent of V in S.</li>
     * <li>S must contain at least one version that corresponds to an existing
     * node in the workspace.</li>
     * </ul>
     */
    public void testWorkspaceRestoreWithParent() throws RepositoryException {

        try {
            Version parentV = wVersionableNode.checkin();
            superuser.getWorkspace().restore(new Version[]{parentV, wChildVersion}, false);
        } catch (RepositoryException e) {
            fail("Workspace.restore(Version[], boolean) with a version that has no corresponding node must succeed if a version of a parent with correspondance is present in the version array.");
        }
    }

    /**
     * Test if the removeExisting-flag removes an existing node in case of uuid conflict.
     */
    public void testWorkspaceRestoreWithRemoveExisting() throws NotExecutableException, RepositoryException {
        // create version for parentNode of childNode
        superuser.getWorkspace().clone(workspaceName, wVersionableChildNode.getPath(), wVersionableChildNode.getPath(), false);
        Version parentV = versionableNode.checkin();

        // move child node in order to produce the uuid conflict
        String newChildPath = wVersionableNode2.getPath() + "/" + wVersionableChildNode.getName();
        wSuperuser.move(wVersionableChildNode.getPath(), newChildPath);
        wSuperuser.save();

        // restore the parent with removeExisting == true >> moved child node
        // must be removed.
        wSuperuser.getWorkspace().restore(new Version[]{parentV}, true);
        if (wSuperuser.itemExists(newChildPath)) {
            fail("Workspace.restore(Version[], boolean) with the boolean flag set to true, must remove the existing node in case of Uuid conflict.");
        }
    }

    /**
     * Tests if restoring the <code>Version</code> of an existing node throws an
     * <code>ItemExistsException</code> if removeExisting is set to FALSE.
     */
    public void testWorkspaceRestoreWithUUIDConflict() throws RepositoryException, NotExecutableException {
        try {
            // Verify that nodes used for the test are indeed versionable
            NodeDefinition nd = wVersionableNode.getDefinition();
            if (nd.getOnParentVersion() != OnParentVersionAction.COPY && nd.getOnParentVersion() != OnParentVersionAction.VERSION) {
                throw new NotExecutableException("Nodes must be versionable in order to run this test.");
            }

            Version v = wVersionableNode.checkin();
            wVersionableNode.checkout();
            wSuperuser.move(wVersionableChildNode.getPath(), wVersionableNode2.getPath() + "/" + wVersionableChildNode.getName());
            wSuperuser.save();
            wSuperuser.getWorkspace().restore(new Version[]{v}, false);

            fail("Node.restore( Version, boolean ): An ItemExistsException must be thrown if the node to be restored already exsits and removeExisting was set to false.");
        } catch (ItemExistsException e) {
            // success
        }
    }


    /**
     * Test if workspace-restoring a node works on checked-in node.
     */
    public void testWorkspaceRestoreOnCheckedInNode() throws RepositoryException {
        if (versionableNode.isCheckedOut()) {
            versionableNode.checkin();
        }
        superuser.getWorkspace().restore(new Version[]{version}, true);
    }

    /**
     * Test if workspace-restoring a node works on checked-out node.
     */
    public void testWorkspaceRestoreOnCheckedOutNode() throws RepositoryException {
        if (!versionableNode.isCheckedOut()) {
            versionableNode.checkout();
        }
        superuser.getWorkspace().restore(new Version[]{version}, true);
    }

}
