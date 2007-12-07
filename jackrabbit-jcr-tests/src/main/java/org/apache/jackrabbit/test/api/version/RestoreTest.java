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
import javax.jcr.version.VersionIterator;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ItemExistsException;
import javax.jcr.NodeIterator;

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
    Version version2;
    Version rootVersion;

    Node versionableNode2;

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
    }

    protected void tearDown() throws Exception {
        try {
            versionableNode2.remove();
            testRootNode.save();
        } finally {
            version = null;
            version2 = null;
            rootVersion = null;
            versionableNode2 = null;
            super.tearDown();
        }
    }

    /**
     * Test if restoring the root version fails.
     *
     * @throws RepositoryException
     */
    public void testRestoreRootVersionFail() throws RepositoryException {
        try {
            versionableNode.restore(rootVersion, true);
            fail("Restore of jcr:rootVersion must throw VersionException.");
        } catch (VersionException e) {
            // success
        }
    }

    /**
     * Test if restoring a node works on checked-in node.
     *
     * @throws RepositoryException
     */
    public void testRestoreOnCheckedInNode() throws RepositoryException {
        versionableNode.checkin();
        versionableNode.restore(version, true);
    }

    /**
     * Test if restoring a node works on checked-out node.
     *
     * @throws RepositoryException
     */
    public void testRestoreOnCheckedOutNode() throws RepositoryException {
        versionableNode.restore(version, true);
    }

    /**
     * Restoring a node set the jcr:isCheckedOut property to false.
     *
     * @throws RepositoryException
     */
    public void testRestoreSetsIsCheckedOutToFalse() throws RepositoryException {
        versionableNode.restore(version, true);

        assertFalse("Restoring a node sets the jcr:isCheckedOut property to false", versionableNode.isCheckedOut());
    }

    /**
     * Test if restoring a node sets the jcr:baseVersion property correctly.
     *
     * @throws javax.jcr.RepositoryException
     */
    public void testRestoreSetsBaseVersion() throws RepositoryException {
        versionableNode.restore(version, true);
        Version baseV = versionableNode.getBaseVersion();
        assertTrue("Restoring a node must set node's base version in order to point to the restored version.", version.isSame(baseV));
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
            versionableNode.restore(version, true);

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
        String invalidName;
        do {
            invalidName = createRandomString(3);
            for (VersionIterator it = versionableNode.getVersionHistory().getAllVersions(); it.hasNext();) {
                Version v = it.nextVersion();
                if (invalidName.equals(v.getName())) {
                    invalidName = null;
                    break;
                }
            }
        } while (invalidName == null);

        try {
            versionableNode.restore(invalidName, true);
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
        // the 'version' will be restored at location 'foo'.

        try {
            nonVersionableNode.getParent().restore(version, nonVersionableNode.getName(), true);
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
            nonVersionableNode.restore(version, true);
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
     * Tests if restoring the <code>Version</code> of an existing node throws an
     * <code>ItemExistsException</code> if removeExisting is set to FALSE.
     */
    public void testRestoreWithUUIDConflict() throws RepositoryException, NotExecutableException {
        try {
            Node naa = createVersionableNode(versionableNode, nodeName4, versionableNodeType);
            // Verify that nodes used for the test have proper opv behaviour
            NodeDefinition nd = naa.getDefinition();
            if (nd.getOnParentVersion() != OnParentVersionAction.COPY && nd.getOnParentVersion() != OnParentVersionAction.VERSION) {
                throw new NotExecutableException("Child nodes must have OPV COPY or VERSION in order to be able to test Node.restore with uuid conflict.");
            }

            Version v = versionableNode.checkin();
            versionableNode.checkout();
            superuser.move(naa.getPath(), versionableNode2.getPath() + "/" + naa.getName());
            superuser.save();
            versionableNode.restore(v, false);

            fail("Node.restore( Version, boolean ): An ItemExistsException must be thrown if the node to be restored already exsits and removeExisting was set to false.");
        } catch (ItemExistsException e) {
            // success
        }
    }

    public void testRestoreChild1() throws RepositoryException {
        versionableNode.addNode("child1");
        versionableNode.save();
        Version v1 = versionableNode.checkin();
        versionableNode.checkout();
        Version v2 = versionableNode.checkin();

        versionableNode.restore(v1, true);
        assertTrue("Node.restore('1.2') must not remove child node.", versionableNode.hasNode("child1"));

        versionableNode.restore(version, true);
        assertFalse("Node.restore('1.0') must remove child node.", versionableNode.hasNode("child1"));

        try {
            versionableNode.restore(v2, true);
        } catch (RepositoryException e) {
            fail("Node.restore('1.3') must fail.");
        }
    }

    /**
     * Test the restore of a versionable node using a label.
     * @throws RepositoryException
     */
    public void testRestoreLabel() throws RepositoryException {
        // V1 of versionable node
        Version v1 = versionableNode.checkin();
        String v1Name = v1.getName();

        // mark V1 with label test1
        versionableNode.getVersionHistory().addVersionLabel(v1Name, "test", true);

        // create a new version
        versionableNode.checkout();
        Version v2 = versionableNode.checkin();

        // restore V1 via label.
        versionableNode.restoreByLabel("test", true);
        assertEquals("Node.restore('test') not correctly restored",
                v1Name, versionableNode.getBaseVersion().getName());
    }

    /**
     * Test the restore of the OPV=Version child nodes.
     * @throws RepositoryException
     */
    public void testRestoreName() throws RepositoryException {
        // V1.0 of versionableNode has no child
        Node child1 = versionableNode.addNode(nodeName4);
        if (!child1.isNodeType(mixVersionable)) {
            child1.addMixin(mixVersionable);
        }
        versionableNode.save();
        // create v1.0 of child
        Version v1Child = child1.checkin();

        // V1 of versionable node has child1
        String v1 = versionableNode.checkin().getName();

        // create V1.1 of child
        child1.checkout();
        Version v11Child = child1.checkin();

        // V2 of versionable node has child1
        versionableNode.checkout();
        String v2 = versionableNode.checkin().getName();

        // restore 1.0 of versionable node --> no child
        versionableNode.restore(version, true);
        assertFalse("Node.restore('1.0') must remove child node.", versionableNode.hasNode(nodeName4));

        // restore V1 via name. since child was checkin first, 1.0 should be restored
        versionableNode.restore(v1, true);
        assertTrue("Node.restore('test') must restore child node.", versionableNode.hasNode(nodeName4));
        child1 = versionableNode.getNode(nodeName4);
        assertEquals("Node.restore('test') must restore child node version 1.0.", v1Child.getName(), child1.getBaseVersion().getName());

        // restore V2 via name. child should be 1.1
        versionableNode.restore(v2, true);
        child1 = versionableNode.getNode(nodeName4);
        assertEquals("Node.restore('foo') must restore child node version 1.1.", v11Child.getName(), child1.getBaseVersion().getName());
    }

    /**
     * Test the child ordering of restored nodes.
     * @throws RepositoryException
     */
    public void testRestoreOrder() throws RepositoryException {
        // create a test-root that has orderable child nodes
        Node testRoot = versionableNode.addNode(nodeName4, "nt:unstructured");
        testRoot.addMixin(mixVersionable);
        versionableNode.save();

        // create children of vNode and checkin
        Node child1 = testRoot.addNode(nodeName1);
        if (!child1.isNodeType(mixVersionable)) {
            child1.addMixin(mixVersionable);
        }
        Node child2 = testRoot.addNode(nodeName2);
        if (!child2.isNodeType(mixVersionable)) {
            child2.addMixin(mixVersionable);
        }
        testRoot.save();
        child1.checkin();
        child2.checkin();
        Version v1 = testRoot.checkin();

        // remove node 1
        testRoot.checkout();
        child1.remove();
        testRoot.save();
        testRoot.checkin();

        // restore version 1.0
        testRoot.restore(v1, true);

        // check order
        NodeIterator iter = testRoot.getNodes();
        assertTrue(testRoot.getName() + " should have 2 child nodes.", iter.hasNext());
        Node n1 = iter.nextNode();
        assertTrue(testRoot.getName() + " should have 2 child nodes.", iter.hasNext());
        Node n2 = iter.nextNode();
        String orderOk = nodeName1 + ", " + nodeName2;
        String order = n1.getName() + ", " + n2.getName();
        assertEquals("Invalid child node ordering", orderOk, order);
    }

    /**
     * Test the child ordering of restored nodes.
     * @throws RepositoryException
     */
    public void testRestoreOrder2() throws RepositoryException {
        // create a test-root that has orderable child nodes
        Node testRoot = versionableNode.addNode(nodeName4, "nt:unstructured");
        testRoot.addMixin(mixVersionable);
        versionableNode.save();

        // create children of vNode and checkin
        Node child1 = testRoot.addNode(nodeName1);
        if (!child1.isNodeType(mixVersionable)) {
            child1.addMixin(mixVersionable);
        }
        Node child2 = testRoot.addNode(nodeName2);
        if (!child2.isNodeType(mixVersionable)) {
            child2.addMixin(mixVersionable);
        }
        testRoot.save();
        child1.checkin();
        child2.checkin();
        Version v1 = testRoot.checkin();

        // reoder nodes
        testRoot.checkout();
        testRoot.orderBefore(nodeName2, nodeName1);
        testRoot.save();
        testRoot.checkin();

        // restore version 1.0
        testRoot.restore(v1, true);

        // check order
        NodeIterator iter = testRoot.getNodes();
        assertTrue(testRoot.getName() + " should have 2 child nodes.", iter.hasNext());
        Node n1 = iter.nextNode();
        assertTrue(testRoot.getName() + " should have 2 child nodes.", iter.hasNext());
        Node n2 = iter.nextNode();
        String orderOk = nodeName1 + ", " + nodeName2;
        String order = n1.getName() + ", " + n2.getName();
        assertEquals("Invalid child node ordering", orderOk, order);
    }

}