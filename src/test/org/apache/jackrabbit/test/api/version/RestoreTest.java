/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
package org.apache.jackrabbit.test.api.version;

import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.nodetype.NodeDef;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.OnParentVersionAction;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ItemExistsException;

/**
 * <code>RestoreTest</code> covers tests related to the restore methods available
 * on {@link javax.jcr.Node}:
 * <ul>
 * <li>{@link javax.jcr.Node#restore(String, boolean)}</li>
 * <li>{@link javax.jcr.Node#restore(javax.jcr.version.Version, boolean)}</li>
 * <li>{@link javax.jcr.Node#restore(javax.jcr.version.Version, String, boolean)}</li>
 * </ul>
 *
 * @test
 * @sources RestoreTest.java
 * @executeClass org.apache.jackrabbit.test.api.version.RestoreTest
 * @keywords versioning
 */
public class RestoreTest extends AbstractVersionTest {

    Version version;
    Version rootVersion;

    Node versionableNode2;

    protected void setUp() throws Exception {
        super.setUp();

        version = versionableNode.checkin();
        versionableNode.checkout();
        rootVersion = versionableNode.getVersionHistory().getRootVersion();

        // build a second versionable node below the testroot
        try {
            versionableNode2 = createVersionableNode(testRootNode, nodeName2, versionableNodeType);
        } catch (RepositoryException e) {
            fail("Failed to create a second versionable node: "+e.getMessage());
        }
    }

    protected void tearDown() throws Exception {
        try {
            versionableNode2.remove();
            testRootNode.save();
        } finally {
            super.tearDown();
        }
    }

    /**
     * Test if restoring a node works on checked-in node.
     *
     * @throws RepositoryException
     */
    public void testRestoreOnCheckedInNode() throws RepositoryException {
        versionableNode.checkin();
        versionableNode.restore(rootVersion, true);
    }

    /**
     * Test if restoring a node works on checked-out node.
     *
     * @throws RepositoryException
     */
    public void testRestoreOnCheckedOutNode() throws RepositoryException {
        versionableNode.restore(rootVersion, true);
    }

    /**
     * Restoring a node set the jcr:isCheckedOut property to false.
     *
     * @throws RepositoryException
     */
    public void testRestoreSetsIsCheckedOutToFalse() throws RepositoryException {
        versionableNode.restore(rootVersion, true);

        assertFalse("Restoring a node sets the jcr:isCheckedOut property to false", versionableNode.isCheckedOut());
    }

    /**
     * Test if restoring a node sets the jcr:baseVersion property correctly.
     *
     * @throws javax.jcr.RepositoryException
     */
    public void testRestoreSetsBaseVersion() throws RepositoryException {
        versionableNode.restore(rootVersion, true);
        Version baseV = versionableNode.getBaseVersion();

        assertEquals("Restoring a node must set node's base version in order to point to the restored version.", baseV, rootVersion);
    }

    /**
     * Test if InvalidItemStateException is thrown if the node has pending changes.
     *
     * @throws RepositoryException
     */
    public void testRestoreWithPendingChanges() throws RepositoryException {
        // modify node without calling save()
        try {
            versionableNode.setProperty(propertyName1, propertyValue);
            versionableNode.restore(rootVersion, true);

            fail("InvalidItemStateException must be thrown on attempt to restore a node having any unsaved changes pending.");
        } catch (InvalidItemStateException e) {
            // ok
        }
    }

    /**
     * VersionException expected on Node.restore(Version, boolean) if the
     * specified version is not part of this node's version history.
     *
     * @throws RepositoryException
     */
    public void testRestoreInvalidVersion() throws RepositoryException {
        Version vNode2 = versionableNode2.checkin();
        try {
            versionableNode.restore(vNode2, true);

            fail("VersionException expected on Node.restore(Version, boolean) if the specified version is not part of this node's version history.");
        } catch (VersionException e) {
            // ok
        }
    }

    /**
     * VersionException expected on Node.restore(String, boolean) if the specified version is not part of this node's version history.
     *
     * @throws RepositoryException
     */
    public void testRestoreInvalidVersion2() throws RepositoryException {
        Version vNode2 = versionableNode2.checkin();
        try {
            versionableNode.restore(vNode2.getName(), true);

            fail("VersionException expected on Node.restore(String, boolean) if the specified version is not part of this node's version history.");
        } catch (VersionException e) {
            // ok
        }
    }

    /**
     * Test calling Node.restore(String, boolean) on a non-versionable node.
     *
     * @throws javax.jcr.RepositoryException
     * @see Node#restore(String, boolean)
     */
    public void testRestoreNonVersionableNode() throws RepositoryException {
        try {
            nonVersionableNode.restore("foo", true);
            fail("Node.restore(String, boolean) on a non versionable node must throw UnsupportedRepositoryOperationException");
        } catch (UnsupportedRepositoryOperationException e) {
            //success
        }
    }

    /**
     * Test calling Node.restore(Version, String, boolean) on a non-versionable node.
     *
     * @throws javax.jcr.RepositoryException
     * @see Node#restore(javax.jcr.version.Version, String, boolean)
     */
    public void testRestoreNonVersionableNode2() throws RepositoryException {
        try {
            nonVersionableNode.restore(rootVersion, "foo", true);
            fail("Node.restore(Version, String, boolean) on a non versionable node must throw UnsupportedRepositoryOperationException");
        } catch (UnsupportedRepositoryOperationException e) {
            //success
        }
    }

    /**
     * Test calling Node.restore(Version, boolean) on a non-versionable node.
     *
     * @throws javax.jcr.RepositoryException
     * @see Node#restore(Version, boolean)
     */
    public void testRestoreNonVersionableNode3() throws RepositoryException {
        try {
            nonVersionableNode.restore(rootVersion, true);
            fail("Node.restore(Version, boolean) on a non versionable node must throw UnsupportedRepositoryOperationException");
        } catch (UnsupportedRepositoryOperationException e) {
            //success
        }
    }

    /**
     * Test if restoring a node with an invalid Version throws a VersionException
     *
     * @throws RepositoryException
     */
    public void testRestoreWithInvalidVersion() throws RepositoryException {
        Version invalidVersion = versionableNode2.checkin();
        try {
            versionableNode.restore(invalidVersion, true);
            fail("Node.restore(Version, boolean): A VersionException must be thrown if the specified version does not exists in this node's version history.");
        } catch (VersionException e) {
            // success
        }
    }

    /**
     * Test if restoring a node with an invalid version name throws a VersionException
     *
     * @throws RepositoryException
     */
    public void testRestoreWithInvalidVersionName() throws RepositoryException {
        Version invalidVersion = versionableNode2.checkin();
        try {
            versionableNode.restore(invalidVersion.getName(), true);
            fail("Node.restore(Version, boolean): A VersionException must be thrown if the specified version does not exists in this node's version history.");
        } catch (VersionException e) {
            // success
        }
    }

    /**
     * Tests if restoring the <code>Version</code> of an existing node throws an
     * <code>ItemExistsException</code> if removeExisting is set to FALSE.
     */
    public void testRestoreWithUUIDConflict() throws RepositoryException, NotExecutableException {
        try {
            Node naa = createVersionableNode(versionableNode, nodeName4, versionableNodeType);
            // Verify that nodes used for the test have proper opv behaviour
            NodeDef nd = naa.getDefinition();
            if ( nd.getOnParentVersion() != OnParentVersionAction.COPY || nd.getOnParentVersion() != OnParentVersionAction.VERSION ) {
                throw new NotExecutableException( "Child nodes must have OPV COPY or VERSION in order to be able to test Node.restore with uuid conflict." );
            }

            Version v = versionableNode.checkin();
            superuser.move( naa.getPath(), versionableNode2.getPath() + "/" + naa.getName() );
            versionableNode.restore( v, false );

            fail( "Node.restore( Version, boolean ): An ItemExistsException must be thrown if the node to be restored already exsits and removeExisting was set to false." );
        } catch (ItemExistsException e ) {
            // success
        }
    }
}