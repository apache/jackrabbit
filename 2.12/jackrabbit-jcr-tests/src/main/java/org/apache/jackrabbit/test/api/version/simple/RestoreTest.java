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
package org.apache.jackrabbit.test.api.version.simple;

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.version.OnParentVersionAction;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;

import org.apache.jackrabbit.test.NotExecutableException;

/**
 * <code>RestoreTest</code> covers tests related to the restore methods available
 * on {@link Node} in simple versioning:
 * <ul>
 * <li>{@link Node#restore(String, boolean)}</li>
 * <li>{@link Node#restore(Version, boolean)}</li>
 * <li>{@link Node#restore(Version, String, boolean)}</li>
 * </ul>
 *
 */
public class RestoreTest extends AbstractVersionTest {

    VersionManager versionManager;

    Version version;
    Version version2;
    Version rootVersion;

    Node versionableNode2;

    String propertyValue1;
    String propertyValue2;

    protected void setUp() throws Exception {
        super.setUp();
        try {
            versionManager = versionableNode.getSession().getWorkspace().getVersionManager();
            String path = versionableNode.getPath();
            propertyValue1 = getProperty("propertyValue1");
            propertyValue2 = getProperty("propertyValue2");
            versionableNode.setProperty(propertyName1, propertyValue1);
            versionableNode.getSession().save();
            version = versionManager.checkin(path);
            versionManager.checkout(path);
            versionableNode.setProperty(propertyName1, propertyValue2);
            versionableNode.getSession().save();
            version2 = versionManager.checkin(path);
            versionManager.checkout(path);
            rootVersion = versionManager.getVersionHistory(path).getRootVersion();
        } catch (RepositoryException e) {
            cleanUp();
            fail("Failed to setup test: " + e.getMessage());
        }

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
            testRootNode.getSession().save();
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
    @SuppressWarnings("deprecation")
    public void testRestoreRootVersionFail() throws RepositoryException {
        try {
            versionableNode.restore(rootVersion, true);
            fail("Restore of jcr:rootVersion must throw VersionException.");
        } catch (VersionException e) {
            // success
        }
    }

    /**
     * Test if restoring the root version fails.
     *
     * @throws RepositoryException
     */
    public void testRestoreRootVersionFailJcr2() throws RepositoryException {
        try {
            versionManager.restore(rootVersion, true);
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
    @SuppressWarnings("deprecation")
    public void testRestoreOnCheckedInNode() throws RepositoryException {
        versionableNode.checkin();
        versionableNode.restore(version, true);
    }

    /**
     * Test if restoring a node works on checked-in node.
     *
     * @throws RepositoryException
     */
    public void testRestoreOnCheckedInNodeJcr2_1() throws RepositoryException {
        versionManager.checkin(versionableNode.getPath());
        versionManager.restore(version, true);
    }

    /**
     * Test if restoring a node works on checked-in node.
     *
     * @throws RepositoryException
     */
    public void testRestoreOnCheckedInNodeJcr2_2() throws RepositoryException {
        versionManager.checkin(versionableNode.getPath());
        try {
            versionManager.restore(versionableNode.getPath(), version, true);
            fail("VersionManager.restore(String, Version, boolean) must fail on existing nodes.");
        } catch (RepositoryException e) {
            // ok
        }
        versionManager.restore(version, true);
    }

    /**
     * Test if restoring a node works on checked-in node.
     *
     * @throws RepositoryException
     */
    public void testRestoreOnCheckedInNodeJcr2_3() throws RepositoryException {
        versionManager.checkin(versionableNode.getPath());
        versionManager.restore(versionableNode.getPath(), version.getName(), true);
    }

    /**
     * Test if restoring a node works on checked-in node.
     *
     * @throws RepositoryException
     */
    public void testRestoreOnCheckedInNodeJcr2_4() throws RepositoryException {
        versionManager.checkin(versionableNode.getPath());
        versionManager.restore(new Version[] {version}, true);
    }

    /**
     * Test if restoring a node works on checked-out node.
     *
     * @throws RepositoryException
     */
    @SuppressWarnings("deprecation")
    public void testRestoreOnCheckedOutNode() throws RepositoryException {
        versionableNode.restore(version, true);
    }

    /**
     * Test if restoring a node works on checked-out node.
     *
     * @throws RepositoryException
     */
    public void testRestoreOnCheckedOutNodeJcr2() throws RepositoryException {
        versionManager.restore(version, true);
    }

    /**
     * Test if restoring a node works on checked-out node.
     *
     * @throws RepositoryException
     */
    public void testRestoreOnCheckedOutNodeJcr2_2() throws RepositoryException {
        try {
            versionManager.restore(versionableNode.getPath(), version, true);
            fail("VersionManager.restore(String, Version, boolean) must fail on existing nodes.");
        } catch (RepositoryException e) {
            // ok
        }
        versionManager.restore(version, true);
    }

    /**
     * Test if restoring a node works on checked-out node.
     *
     * @throws RepositoryException
     */
    public void testRestoreOnCheckedOutNodeJcr2_3() throws RepositoryException {
        versionManager.restore(versionableNode.getPath(), version.getName(), true);
    }

    /**
     * Test if restoring a node works on checked-out node.
     *
     * @throws RepositoryException
     */
    public void testRestoreOnCheckedOutNodeJcr2_4() throws RepositoryException {
        versionManager.restore(new Version[] {version}, true);
    }

    /**
     * Restoring a node set the jcr:isCheckedOut property to false.
     *
     * @throws RepositoryException
     */
    @SuppressWarnings("deprecation")
    public void testRestoreSetsIsCheckedOutToFalse() throws RepositoryException {
        versionableNode.restore(version, true);
        assertFalse("Restoring a node sets the jcr:isCheckedOut property to false", versionableNode.isCheckedOut());
    }

    /**
     * Restoring a node set the jcr:isCheckedOut property to false.
     *
     * @throws RepositoryException
     */
    public void testRestoreSetsIsCheckedOutToFalseJcr2() throws RepositoryException {
        versionManager.restore(version, true);
        assertFalse("Restoring a node sets the jcr:isCheckedOut property to false", versionManager.isCheckedOut(versionableNode.getPath()));
    }

    /**
     * Restoring a node set the jcr:isCheckedOut property to false.
     *
     * @throws RepositoryException
     */
    public void testRestoreSetsIsCheckedOutToFalseJcr2_2() throws RepositoryException {
        try {
            versionManager.restore(versionableNode.getPath(), version, true);
            fail("VersionManager.restore(String, Version, boolean) must fail on existing nodes.");
        } catch (RepositoryException e) {
            // ok
        }
        versionManager.restore(version, true);
        assertFalse("Restoring a node sets the jcr:isCheckedOut property to false", versionManager.isCheckedOut(versionableNode.getPath()));
    }

    /**
     * Restoring a node set the jcr:isCheckedOut property to false.
     *
     * @throws RepositoryException
     */
    public void testRestoreSetsIsCheckedOutToFalseJcr3() throws RepositoryException {
        versionManager.restore(versionableNode.getPath(), version.getName(), true);
        assertFalse("Restoring a node sets the jcr:isCheckedOut property to false", versionManager.isCheckedOut(versionableNode.getPath()));
    }

    /**
     * Restoring a node set the jcr:isCheckedOut property to false.
     *
     * @throws RepositoryException
     */
    public void testRestoreSetsIsCheckedOutToFalseJcr2_4() throws RepositoryException {
        versionManager.restore(new Version[] {version}, true);
        assertFalse("Restoring a node sets the jcr:isCheckedOut property to false", versionManager.isCheckedOut(versionableNode.getPath()));
    }

    /**
     * Test if restoring a node restores the correct property
     *
     * @throws RepositoryException
     */
    @SuppressWarnings("deprecation")
    public void testRestoreCorrectProperty() throws RepositoryException {
        versionableNode.restore(version, true);
        String value = versionableNode.getProperty(propertyName1).getString();
        assertEquals("Restoring a node must set the correct property.", propertyValue1, value);
    }

    /**
     * Test if restoring a node restores the correct property
     *
     * @throws RepositoryException
     */
    public void testRestoreCorrectPropertyJcr2() throws RepositoryException {
        versionManager.restore(version, true);
        String value = versionableNode.getProperty(propertyName1).getString();
        assertEquals("Restoring a node must set the correct property.", propertyValue1, value);
    }

    /**
     * Test if restoring a node restores the correct property
     *
     * @throws RepositoryException
     */
    public void testRestoreCorrectPropertyJcr2_2() throws RepositoryException {
        try {
            versionManager.restore(versionableNode.getPath(), version, true);
            fail("VersionManager.restore(String, Version, boolean) must fail on existing nodes.");
        } catch (RepositoryException e) {
            // ok
        }
        versionManager.restore(version, true);
        String value = versionableNode.getProperty(propertyName1).getString();
        assertEquals("Restoring a node must set the correct property.", propertyValue1, value);
    }

    /**
     * Test if restoring a node restores the correct property
     *
     * @throws RepositoryException
     */
    public void testRestoreCorrectPropertyJcr2_3() throws RepositoryException {
        versionManager.restore(versionableNode.getPath(), version.getName(), true);
        String value = versionableNode.getProperty(propertyName1).getString();
        assertEquals("Restoring a node must set the correct property.", propertyValue1, value);
    }

    /**
     * Test if restoring a node restores the correct property
     *
     * @throws RepositoryException
     */
    public void testRestoreCorrectPropertyJcr2_4() throws RepositoryException {
        versionManager.restore(new Version[] {version}, true);
        String value = versionableNode.getProperty(propertyName1).getString();
        assertEquals("Restoring a node must set the correct property.", propertyValue1, value);
    }

    /**
     * Test if InvalidItemStateException is thrown if the node has pending changes.
     *
     * @throws RepositoryException
     */
    @SuppressWarnings("deprecation")
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
     * Test if InvalidItemStateException is thrown if the node has pending changes.
     *
     * @throws RepositoryException
     */
    public void testRestoreWithPendingChangesJcr2() throws RepositoryException {
        // modify node without calling save()
        try {
            versionableNode.setProperty(propertyName1, propertyValue);
            versionManager.restore(version, true);

            fail("InvalidItemStateException must be thrown on attempt to restore a node having any unsaved changes pending.");
        } catch (InvalidItemStateException e) {
            // ok
        }
    }

    /**
     * Test if InvalidItemStateException is thrown if the node has pending changes.
     *
     * @throws RepositoryException
     */
    public void testRestoreWithPendingChangesJcr2_2() throws RepositoryException {
        // modify node without calling save()
        try {
            versionableNode.setProperty(propertyName1, propertyValue);
            versionManager.restore(version, true);

            fail("InvalidItemStateException must be thrown on attempt to restore a node having any unsaved changes pending.");
        } catch (InvalidItemStateException e) {
            // ok
        }
    }

    /**
     * Test if InvalidItemStateException is thrown if the node has pending changes.
     *
     * @throws RepositoryException
     */
    public void testRestoreWithPendingChangesJcr2_3() throws RepositoryException {
        // modify node without calling save()
        try {
            versionableNode.setProperty(propertyName1, propertyValue);
            versionManager.restore(versionableNode.getPath(), version.getName(), true);

            fail("InvalidItemStateException must be thrown on attempt to restore a node having any unsaved changes pending.");
        } catch (InvalidItemStateException e) {
            // ok
        }
    }

    /**
     * Test if InvalidItemStateException is thrown if the node has pending changes.
     *
     * @throws RepositoryException
     */
    public void testRestoreWithPendingChangesJcr2_4() throws RepositoryException {
        // modify node without calling save()
        try {
            versionableNode.setProperty(propertyName1, propertyValue);
            versionManager.restore(new Version[] {version}, true);

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
    @SuppressWarnings("deprecation")
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
     * VersionException expected on restore if the
     * specified version is not part of this node's version history.
     *
     * @throws RepositoryException
     */
    public void testRestoreInvalidVersionJcr2() throws RepositoryException {
        Version vNode2 = versionManager.checkin(versionableNode2.getPath());
        try {
            versionManager.restore(versionableNode.getPath(), vNode2, true);

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
    @SuppressWarnings("deprecation")
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
     * VersionException expected on Node.restore(String, boolean) if the specified version is not part of this node's version history.
     *
     * @throws RepositoryException
     */
    public void testRestoreInvalidVersion2Jcr2() throws RepositoryException {
        String invalidName;
        do {
            invalidName = createRandomString(3);
            for (VersionIterator it = versionManager.getVersionHistory(versionableNode.getPath()).getAllVersions(); it.hasNext();) {
                Version v = it.nextVersion();
                if (invalidName.equals(v.getName())) {
                    invalidName = null;
                    break;
                }
            }
        } while (invalidName == null);

        try {
            versionManager.restore(versionableNode.getPath(), invalidName, true);
            fail("VersionException expected on Node.restore(String, boolean) if the specified version is not part of this node's version history.");
        } catch (VersionException e) {
            // ok
        }
    }

    /**
     * Test calling Node.restore(String, boolean) on a non-versionable node.
     *
     * @throws RepositoryException
     * @see Node#restore(String, boolean)
     */
    @SuppressWarnings("deprecation")
    public void testRestoreNonVersionableNode() throws RepositoryException {
        try {
            nonVersionableNode.restore("foo", true);
            fail("Node.restore(String, boolean) on a non versionable node must throw UnsupportedRepositoryOperationException");
        } catch (UnsupportedRepositoryOperationException e) {
            //success
        }
    }

    /**
     * Test restoring on a non-versionable node.
     *
     * @throws RepositoryException
     * @see Node#restore(String, boolean)
     */
    public void testRestoreNonVersionableNodeJcr2_2() throws RepositoryException {
        try {
            versionManager.restore(nonVersionableNode.getPath(), "foo", true);
            fail("trying to restore on a non versionable node must throw UnsupportedRepositoryOperationException");
        } catch (UnsupportedRepositoryOperationException e) {
            //success
        }
    }

    /**
     * Test calling Node.restore(Version, String, boolean) on a non-versionable node.
     *
     * @throws RepositoryException
     * @see Node#restore(Version, String, boolean)
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
     * @throws RepositoryException
     * @see Node#restore(Version, boolean)
     */
    @SuppressWarnings("deprecation")
    public void testRestoreNonVersionableNode3() throws RepositoryException {
        try {
            nonVersionableNode.restore(version, true);
            fail("Node.restore(Version, boolean) on a non versionable node must throw UnsupportedRepositoryOperationException");
        } catch (UnsupportedRepositoryOperationException e) {
            //success
        }
    }

    /**
     * Test restoring on a non-versionable node.
     *
     * @throws RepositoryException
     * @see Node#restore(Version, boolean)
     */
    public void testRestoreNonVersionableNode3Jcr2_2() throws RepositoryException {
        try {
            versionManager.restore(nonVersionableNode.getPath(), version.getName(), true);
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
    @SuppressWarnings("deprecation")
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
     * Test if restoring a node with an invalid Version throws a VersionException
     *
     * @throws RepositoryException
     */
    public void testRestoreWithInvalidVersionJcr2() throws RepositoryException {
        Version invalidVersion = versionManager.checkin(versionableNode2.getPath());
        try {
            versionManager.restore(versionableNode.getPath(), invalidVersion, true);
            fail("Node.restore(Version, boolean): A VersionException must be thrown if the specified version does not exists in this node's version history.");
        } catch (VersionException e) {
            // success
        }
    }

    /**
     * Tests if restoring the <code>Version</code> of an existing node throws an
     * <code>ItemExistsException</code> if removeExisting is set to FALSE.
     */
    @SuppressWarnings("deprecation")
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

    /**
     * Tests if restoring the <code>Version</code> of an existing node throws an
     * <code>ItemExistsException</code> if removeExisting is set to FALSE.
     */
    public void testRestoreWithUUIDConflictJcr2() throws RepositoryException, NotExecutableException {
        try {
            Node naa = createVersionableNode(versionableNode, nodeName4, versionableNodeType);
            // Verify that nodes used for the test have proper opv behaviour
            NodeDefinition nd = naa.getDefinition();
            if (nd.getOnParentVersion() != OnParentVersionAction.COPY && nd.getOnParentVersion() != OnParentVersionAction.VERSION) {
                throw new NotExecutableException("Child nodes must have OPV COPY or VERSION in order to be able to test Node.restore with uuid conflict.");
            }

            Version v = versionManager.checkin(versionableNode.getPath());
            versionManager.checkout(versionableNode.getPath());
            superuser.move(naa.getPath(), versionableNode2.getPath() + "/" + naa.getName());
            superuser.save();
            versionManager.restore(v, false);

            fail("Node.restore( Version, boolean ): An ItemExistsException must be thrown if the node to be restored already exsits and removeExisting was set to false.");
        } catch (ItemExistsException e) {
            // success
        }
    }

    /**
     * Tests if restoring the <code>Version</code> of an existing node throws an
     * <code>ItemExistsException</code> if removeExisting is set to FALSE.
     */
    public void testRestoreWithUUIDConflictJcr2_2() throws RepositoryException, NotExecutableException {
        try {
            Node naa = createVersionableNode(versionableNode, nodeName4, versionableNodeType);
            // Verify that nodes used for the test have proper opv behaviour
            NodeDefinition nd = naa.getDefinition();
            if (nd.getOnParentVersion() != OnParentVersionAction.COPY && nd.getOnParentVersion() != OnParentVersionAction.VERSION) {
                throw new NotExecutableException("Child nodes must have OPV COPY or VERSION in order to be able to test Node.restore with uuid conflict.");
            }

            Version v = versionManager.checkin(versionableNode.getPath());
            versionManager.checkout(versionableNode.getPath());
            superuser.move(naa.getPath(), versionableNode2.getPath() + "/" + naa.getName());
            superuser.save();
            versionManager.restore(v, false);

            fail("Node.restore( Version, boolean ): An ItemExistsException must be thrown if the node to be restored already exsits and removeExisting was set to false.");
        } catch (ItemExistsException e) {
            // success
        }
    }

    /**
     * Tests if restoring the <code>Version</code> of an existing node throws an
     * <code>ItemExistsException</code> if removeExisting is set to FALSE.
     */
    public void testRestoreWithUUIDConflictJcr2_3() throws RepositoryException, NotExecutableException {
        try {
            Node naa = createVersionableNode(versionableNode, nodeName4, versionableNodeType);
            // Verify that nodes used for the test have proper opv behaviour
            NodeDefinition nd = naa.getDefinition();
            if (nd.getOnParentVersion() != OnParentVersionAction.COPY && nd.getOnParentVersion() != OnParentVersionAction.VERSION) {
                throw new NotExecutableException("Child nodes must have OPV COPY or VERSION in order to be able to test Node.restore with uuid conflict.");
            }

            Version v = versionManager.checkin(versionableNode.getPath());
            versionManager.checkout(versionableNode.getPath());
            superuser.move(naa.getPath(), versionableNode2.getPath() + "/" + naa.getName());
            superuser.save();
            versionManager.restore(versionableNode.getPath(), v.getName(), false);

            fail("Node.restore( Version, boolean ): An ItemExistsException must be thrown if the node to be restored already exsits and removeExisting was set to false.");
        } catch (ItemExistsException e) {
            // success
        }
    }

    /**
     * Tests if restoring the <code>Version</code> of an existing node throws an
     * <code>ItemExistsException</code> if removeExisting is set to FALSE.
     */
    public void testRestoreWithUUIDConflictJcr2_4() throws RepositoryException, NotExecutableException {
        try {
            Node naa = createVersionableNode(versionableNode, nodeName4, versionableNodeType);
            // Verify that nodes used for the test have proper opv behaviour
            NodeDefinition nd = naa.getDefinition();
            if (nd.getOnParentVersion() != OnParentVersionAction.COPY && nd.getOnParentVersion() != OnParentVersionAction.VERSION) {
                throw new NotExecutableException("Child nodes must have OPV COPY or VERSION in order to be able to test Node.restore with uuid conflict.");
            }

            Version v = versionManager.checkin(versionableNode.getPath());
            versionManager.checkout(versionableNode.getPath());
            superuser.move(naa.getPath(), versionableNode2.getPath() + "/" + naa.getName());
            superuser.save();
            versionManager.restore(new Version[] {v}, false);

            fail("Node.restore( Version, boolean ): An ItemExistsException must be thrown if the node to be restored already exsits and removeExisting was set to false.");
        } catch (ItemExistsException e) {
            // success
        }
    }

    @SuppressWarnings("deprecation")
    public void testRestoreChild1() throws RepositoryException {
        versionableNode.addNode("child1");
        versionableNode.getSession().save();
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

    public void testRestoreChild1Jcr2() throws RepositoryException {
        versionableNode.addNode("child1");
        versionableNode.getSession().save();
        Version v1 = versionManager.checkin(versionableNode.getPath());
        versionManager.checkout(versionableNode.getPath());
        Version v2 = versionManager.checkin(versionableNode.getPath());

        versionManager.restore(v1, true);
        assertTrue("Node.restore('1.2') must not remove child node.", versionableNode.hasNode("child1"));

        versionManager.restore(version, true);
        assertFalse("Node.restore('1.0') must remove child node.", versionableNode.hasNode("child1"));

        try {
            versionManager.restore(v2, true);
        } catch (RepositoryException e) {
            fail("Node.restore('1.3') must fail.");
        }
    }

    public void testRestoreChild1Jcr2_2() throws RepositoryException {
        versionableNode.addNode("child1");
        versionableNode.getSession().save();
        Version v1 = versionManager.checkin(versionableNode.getPath());
        versionManager.checkout(versionableNode.getPath());
        Version v2 = versionManager.checkin(versionableNode.getPath());

        versionManager.restore(v1, true);
        assertTrue("Node.restore('1.2') must not remove child node.", versionableNode.hasNode("child1"));

        versionManager.restore(version, true);
        assertFalse("Node.restore('1.0') must remove child node.", versionableNode.hasNode("child1"));

        try {
            versionManager.restore(v2, true);
        } catch (RepositoryException e) {
            fail("Node.restore('1.3') must fail.");
        }
    }

    public void testRestoreChild1Jcr2_3() throws RepositoryException {
        versionableNode.addNode("child1");
        versionableNode.getSession().save();
        Version v1 = versionManager.checkin(versionableNode.getPath());
        versionManager.checkout(versionableNode.getPath());
        Version v2 = versionManager.checkin(versionableNode.getPath());

        versionManager.restore(versionableNode.getPath(), v1.getName(), true);
        assertTrue("Node.restore('1.2') must not remove child node.", versionableNode.hasNode("child1"));

        versionManager.restore(versionableNode.getPath(), version.getName(), true);
        assertFalse("Node.restore('1.0') must remove child node.", versionableNode.hasNode("child1"));

        try {
            versionManager.restore(versionableNode.getPath(), v2.getName(), true);
        } catch (RepositoryException e) {
            fail("Node.restore('1.3') must fail.");
        }
    }

    public void testRestoreChild1Jcr2_4() throws RepositoryException {
        versionableNode.addNode("child1");
        versionableNode.getSession().save();
        Version v1 = versionManager.checkin(versionableNode.getPath());
        versionManager.checkout(versionableNode.getPath());
        Version v2 = versionManager.checkin(versionableNode.getPath());

        versionManager.restore(new Version[] {v1}, true);
        assertTrue("Node.restore('1.2') must not remove child node.", versionableNode.hasNode("child1"));

        versionManager.restore(new Version[] {version}, true);
        assertFalse("Node.restore('1.0') must remove child node.", versionableNode.hasNode("child1"));

        try {
            versionManager.restore(new Version[] {v2}, true);
        } catch (RepositoryException e) {
            fail("Node.restore('1.3') must fail.");
        }
    }

    /**
     * Test the restore of a versionable node using a label.
     * @throws RepositoryException
     */
    @SuppressWarnings("deprecation")
    public void testRestoreLabel() throws RepositoryException {
        // mark V1 with label test1
        versionableNode.getVersionHistory().addVersionLabel(version.getName(), "test", true);

        // restore V1 via label.
        versionableNode.restoreByLabel("test", true);
        String value = versionableNode.getProperty(propertyName1).getString();
        assertEquals("Node.restore('test') not correctly restored", propertyValue1, value);
    }

    /**
     * Test the restore of a versionable node using a label.
     * @throws RepositoryException
     */
    public void testRestoreLabelJcr2() throws RepositoryException {
        // mark V1 with label test1
        versionManager.getVersionHistory(versionableNode.getPath()).addVersionLabel(version.getName(), "test", true);

        // restore V1 via label.
        versionManager.restoreByLabel(versionableNode.getPath(), "test", true);
        String value = versionableNode.getProperty(propertyName1).getString();
        assertEquals("Node.restore('test') not correctly restored", propertyValue1, value);
    }

    /**
     * Test the restore of the OPV=Version child nodes.
     * @throws RepositoryException
     */
    @SuppressWarnings("deprecation")
    public void testRestoreName() throws RepositoryException,
            NotExecutableException {
        // V1.0 of versionableNode has no child
        Node child1 = versionableNode.addNode(nodeName4);
        ensureMixinType(child1, mixVersionable);
        versionableNode.getSession().save();
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
     * Test the restore of the OPV=Version child nodes.
     * @throws RepositoryException
     */
    public void testRestoreNameJcr2() throws RepositoryException,
            NotExecutableException {
        // V1.0 of versionableNode has no child
        Node child1 = versionableNode.addNode(nodeName4);
        ensureMixinType(child1, mixVersionable);
        versionableNode.getSession().save();
        // create v1.0 of child
        Version v1Child = child1.checkin();

        // V1 of versionable node has child1
        String v1 = versionManager.checkin(versionableNode.getPath()).getName();

        // create V1.1 of child
        versionManager.checkout(child1.getPath());
        Version v11Child = versionManager.checkin(child1.getPath());

        // V2 of versionable node has child1
        versionManager.checkout(versionableNode.getPath());
        String v2 = versionManager.checkin(versionableNode.getPath()).getName();

        // restore 1.0 of versionable node --> no child
        versionManager.restore(version, true);
        assertFalse("restore must remove child node.", versionableNode.hasNode(nodeName4));

        // restore V1 via name. since child was checkin first, 1.0 should be restored
        versionManager.restore(versionableNode.getPath(), v1, true);
        assertTrue("restore must restore child node.", versionableNode.hasNode(nodeName4));
        child1 = versionableNode.getNode(nodeName4);
        assertEquals("restore must restore child node version 1.0.", v1Child.getName(), versionManager.getBaseVersion(child1.getPath()).getName());

        // restore V2 via name. child should be 1.1
        versionManager.restore(versionableNode.getPath(), v2, true);
        child1 = versionableNode.getNode(nodeName4);
        assertEquals("Node.restore('foo') must restore child node version 1.1.", v11Child.getName(), versionManager.getBaseVersion(child1.getPath()).getName());
    }

    /**
     * Test the child ordering of restored nodes.
     * @throws RepositoryException
     */
    @SuppressWarnings("deprecation")
    public void testRestoreOrder() throws RepositoryException,
            NotExecutableException {
        // create a test-root that has orderable child nodes
        Node testRoot = versionableNode.addNode(nodeName4, "nt:unstructured");
        ensureMixinType(testRoot, mixVersionable);
        versionableNode.getSession().save();

        // create children of vNode and checkin
        Node child1 = testRoot.addNode(nodeName1);
        ensureMixinType(child1, mixVersionable);
        Node child2 = testRoot.addNode(nodeName2);
        ensureMixinType(child2, mixVersionable);
        testRoot.getSession().save();
        child1.checkin();
        child2.checkin();
        Version v1 = testRoot.checkin();

        // remove node 1
        testRoot.checkout();
        child1.remove();
        testRoot.getSession().save();
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
    public void testRestoreOrderJcr2() throws RepositoryException,
            NotExecutableException {
        // create a test-root that has orderable child nodes
        Node testRoot = versionableNode.addNode(nodeName4, "nt:unstructured");
        ensureMixinType(testRoot, mixVersionable);
        versionableNode.getSession().save();

        // create children of vNode and checkin
        Node child1 = testRoot.addNode(nodeName1);
        ensureMixinType(child1, mixVersionable);
        Node child2 = testRoot.addNode(nodeName2);
        ensureMixinType(child2, mixVersionable);
        testRoot.getSession().save();
        versionManager.checkin(child1.getPath());
        versionManager.checkin(child2.getPath());
        Version v1 = versionManager.checkin(testRoot.getPath());

        // remove node 1
        versionManager.checkout(testRoot.getPath());
        child1.remove();
        testRoot.getSession().save();
        versionManager.checkout(testRoot.getPath());

        // restore version 1.0
        versionManager.restore(v1, true);

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
    public void testRestoreOrderJcr2_2() throws RepositoryException,
            NotExecutableException {
        // create a test-root that has orderable child nodes
        Node testRoot = versionableNode.addNode(nodeName4, "nt:unstructured");
        ensureMixinType(testRoot, mixVersionable);
        versionableNode.getSession().save();

        // create children of vNode and checkin
        Node child1 = testRoot.addNode(nodeName1);
        ensureMixinType(child1, mixVersionable);
        Node child2 = testRoot.addNode(nodeName2);
        ensureMixinType(child2, mixVersionable);
        testRoot.getSession().save();
        versionManager.checkin(child1.getPath());
        versionManager.checkin(child2.getPath());
        Version v1 = versionManager.checkin(testRoot.getPath());

        // remove node 1
        versionManager.checkout(testRoot.getPath());
        child1.remove();
        testRoot.getSession().save();
        versionManager.checkout(testRoot.getPath());

        // restore version 1.0
        versionManager.restore(v1, true);

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
    public void testRestoreOrderJcr2_3() throws RepositoryException,
            NotExecutableException {
        // create a test-root that has orderable child nodes
        Node testRoot = versionableNode.addNode(nodeName4, "nt:unstructured");
        ensureMixinType(testRoot, mixVersionable);
        versionableNode.getSession().save();

        // create children of vNode and checkin
        Node child1 = testRoot.addNode(nodeName1);
        ensureMixinType(child1, mixVersionable);
        Node child2 = testRoot.addNode(nodeName2);
        ensureMixinType(child2, mixVersionable);
        testRoot.getSession().save();
        versionManager.checkin(child1.getPath());
        versionManager.checkin(child2.getPath());
        Version v1 = versionManager.checkin(testRoot.getPath());

        // remove node 1
        versionManager.checkout(testRoot.getPath());
        child1.remove();
        testRoot.getSession().save();
        versionManager.checkout(testRoot.getPath());

        // restore version 1.0
        versionManager.restore(testRoot.getPath(), v1.getName(), true);

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
    public void testRestoreOrderJcr2_4() throws RepositoryException,
            NotExecutableException {
        // create a test-root that has orderable child nodes
        Node testRoot = versionableNode.addNode(nodeName4, "nt:unstructured");
        ensureMixinType(testRoot, mixVersionable);
        versionableNode.getSession().save();

        // create children of vNode and checkin
        Node child1 = testRoot.addNode(nodeName1);
        ensureMixinType(child1, mixVersionable);
        Node child2 = testRoot.addNode(nodeName2);
        ensureMixinType(child2, mixVersionable);
        testRoot.getSession().save();
        versionManager.checkin(child1.getPath());
        versionManager.checkin(child2.getPath());
        Version v1 = versionManager.checkin(testRoot.getPath());

        // remove node 1
        versionManager.checkout(testRoot.getPath());
        child1.remove();
        testRoot.getSession().save();
        versionManager.checkout(testRoot.getPath());

        // restore version 1.0
        versionManager.restore(new Version[] {v1}, true);

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
    @SuppressWarnings("deprecation")
    public void testRestoreOrder2() throws RepositoryException,
            NotExecutableException {
        // create a test-root that has orderable child nodes
        Node testRoot = versionableNode.addNode(nodeName4, "nt:unstructured");
        ensureMixinType(testRoot, mixVersionable);
        versionableNode.getSession().save();

        // create children of vNode and checkin
        Node child1 = testRoot.addNode(nodeName1);
        ensureMixinType(child1, mixVersionable);
        Node child2 = testRoot.addNode(nodeName2);
        ensureMixinType(child2, mixVersionable);
        testRoot.getSession().save();
        child1.checkin();
        child2.checkin();
        Version v1 = testRoot.checkin();

        // reoder nodes
        testRoot.checkout();
        testRoot.orderBefore(nodeName2, nodeName1);
        testRoot.getSession().save();
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
    public void testRestoreOrder2Jcr2() throws RepositoryException,
            NotExecutableException {
        // create a test-root that has orderable child nodes
        Node testRoot = versionableNode.addNode(nodeName4, "nt:unstructured");
        ensureMixinType(testRoot, mixVersionable);
        versionableNode.getSession().save();

        // create children of vNode and checkin
        Node child1 = testRoot.addNode(nodeName1);
        ensureMixinType(child1, mixVersionable);
        Node child2 = testRoot.addNode(nodeName2);
        ensureMixinType(child2, mixVersionable);
        testRoot.getSession().save();
        versionManager.checkin(child1.getPath());
        versionManager.checkin(child2.getPath());
        Version v1 =  versionManager.checkin(testRoot.getPath());

        // reoder nodes
        versionManager.checkout(testRoot.getPath());
        testRoot.orderBefore(nodeName2, nodeName1);
        testRoot.getSession().save();
        versionManager.checkin(testRoot.getPath());

        // restore version 1.0
        versionManager.restore(v1, true);

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
    public void testRestoreOrder2Jcr2_2() throws RepositoryException,
            NotExecutableException {
        // create a test-root that has orderable child nodes
        Node testRoot = versionableNode.addNode(nodeName4, "nt:unstructured");
        ensureMixinType(testRoot, mixVersionable);
        versionableNode.getSession().save();

        // create children of vNode and checkin
        Node child1 = testRoot.addNode(nodeName1);
        ensureMixinType(child1, mixVersionable);
        Node child2 = testRoot.addNode(nodeName2);
        ensureMixinType(child2, mixVersionable);
        testRoot.getSession().save();
        versionManager.checkin(child1.getPath());
        versionManager.checkin(child2.getPath());
        Version v1 =  versionManager.checkin(testRoot.getPath());

        // reoder nodes
        versionManager.checkout(testRoot.getPath());
        testRoot.orderBefore(nodeName2, nodeName1);
        testRoot.getSession().save();
        versionManager.checkin(testRoot.getPath());

        // restore version 1.0
        versionManager.restore(v1, true);

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
    public void testRestoreOrder2Jcr2_3() throws RepositoryException,
            NotExecutableException {
        // create a test-root that has orderable child nodes
        Node testRoot = versionableNode.addNode(nodeName4, "nt:unstructured");
        ensureMixinType(testRoot, mixVersionable);
        versionableNode.getSession().save();

        // create children of vNode and checkin
        Node child1 = testRoot.addNode(nodeName1);
        ensureMixinType(child1, mixVersionable);
        Node child2 = testRoot.addNode(nodeName2);
        ensureMixinType(child2, mixVersionable);
        testRoot.getSession().save();
        versionManager.checkin(child1.getPath());
        versionManager.checkin(child2.getPath());
        Version v1 =  versionManager.checkin(testRoot.getPath());

        // reoder nodes
        versionManager.checkout(testRoot.getPath());
        testRoot.orderBefore(nodeName2, nodeName1);
        testRoot.getSession().save();
        versionManager.checkin(testRoot.getPath());

        // restore version 1.0
        versionManager.restore(v1, true);

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
    public void testRestoreOrder2Jcr2_4() throws RepositoryException,
            NotExecutableException {
        // create a test-root that has orderable child nodes
        Node testRoot = versionableNode.addNode(nodeName4, "nt:unstructured");
        ensureMixinType(testRoot, mixVersionable);
        versionableNode.getSession().save();

        // create children of vNode and checkin
        Node child1 = testRoot.addNode(nodeName1);
        ensureMixinType(child1, mixVersionable);
        Node child2 = testRoot.addNode(nodeName2);
        ensureMixinType(child2, mixVersionable);
        testRoot.getSession().save();
        versionManager.checkin(child1.getPath());
        versionManager.checkin(child2.getPath());
        Version v1 =  versionManager.checkin(testRoot.getPath());

        // reoder nodes
        versionManager.checkout(testRoot.getPath());
        testRoot.orderBefore(nodeName2, nodeName1);
        testRoot.getSession().save();
        versionManager.checkin(testRoot.getPath());

        // restore version 1.0
        versionManager.restore(new Version[] {v1}, true);

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
     * Tests if restore on simple versioning creates a new version that is
     * in the correct linear order.
     */
    @SuppressWarnings("deprecation")
    public void testLinearVersions() throws Exception {
        // first get all linear versions
        VersionIterator iter = versionableNode.getVersionHistory().getAllLinearVersions();
        StringBuffer expected = new StringBuffer();
        while (iter.hasNext()) {
            expected.append(iter.nextVersion().getName()).append(",");
        }
        // restore version
        versionableNode.restore(version, true);
        // append new base version
        expected.append(versionableNode.getBaseVersion().getName()).append(",");

        // get the version names again
        iter = versionableNode.getVersionHistory().getAllLinearVersions();
        StringBuffer actual = new StringBuffer();
        while (iter.hasNext()) {
            actual.append(iter.nextVersion().getName()).append(",");
        }
        assertEquals("Node.restore() on simple versioning must create a new version.",
                expected.toString(), actual.toString());
    }

    /**
     * Tests if restore on simple versioning creates a new version that is
     * in the correct linear order.
     */
    public void testLinearVersionsJcr2() throws Exception {
        // first get all linear versions
        VersionIterator iter = versionManager.getVersionHistory(versionableNode.getPath()).getAllLinearVersions();
        StringBuffer expected = new StringBuffer();
        while (iter.hasNext()) {
            expected.append(iter.nextVersion().getName()).append(",");
        }
        // restore version
        versionManager.restore(version, true);
        // append new base version
        expected.append(versionManager.getBaseVersion(versionableNode.getPath()).getName()).append(",");

        // get the version names again
        iter = versionManager.getVersionHistory(versionableNode.getPath()).getAllLinearVersions();
        StringBuffer actual = new StringBuffer();
        while (iter.hasNext()) {
            actual.append(iter.nextVersion().getName()).append(",");
        }
        assertEquals("Node.restore() on simple versioning must create a new version.",
                expected.toString(), actual.toString());
    }

    /**
     * Tests if restore on simple versioning creates a new version that is
     * in the correct linear order.
     */
    public void testLinearVersionsJcr2_2() throws Exception {
        // first get all linear versions
        VersionIterator iter = versionManager.getVersionHistory(versionableNode.getPath()).getAllLinearVersions();
        StringBuffer expected = new StringBuffer();
        while (iter.hasNext()) {
            expected.append(iter.nextVersion().getName()).append(",");
        }
        // restore version
        versionManager.restore(version, true);
        // append new base version
        expected.append(versionManager.getBaseVersion(versionableNode.getPath()).getName()).append(",");

        // get the version names again
        iter = versionManager.getVersionHistory(versionableNode.getPath()).getAllLinearVersions();
        StringBuffer actual = new StringBuffer();
        while (iter.hasNext()) {
            actual.append(iter.nextVersion().getName()).append(",");
        }
        assertEquals("Node.restore() on simple versioning must create a new version.",
                expected.toString(), actual.toString());
    }

    /**
     * Tests if restore on simple versioning creates a new version that is
     * in the correct linear order.
     */
    public void testLinearVersionsJcr2_3() throws Exception {
        // first get all linear versions
        VersionIterator iter = versionManager.getVersionHistory(versionableNode.getPath()).getAllLinearVersions();
        StringBuffer expected = new StringBuffer();
        while (iter.hasNext()) {
            expected.append(iter.nextVersion().getName()).append(",");
        }
        // restore version
        versionManager.restore(versionableNode.getPath(), version.getName(), true);
        // append new base version
        expected.append(versionManager.getBaseVersion(versionableNode.getPath()).getName()).append(",");

        // get the version names again
        iter = versionManager.getVersionHistory(versionableNode.getPath()).getAllLinearVersions();
        StringBuffer actual = new StringBuffer();
        while (iter.hasNext()) {
            actual.append(iter.nextVersion().getName()).append(",");
        }
        assertEquals("Node.restore() on simple versioning must create a new version.",
                expected.toString(), actual.toString());
    }

    /**
     * Tests if restore on simple versioning creates a new version that is
     * in the correct linear order.
     */
    public void testLinearVersionsJcr2_4() throws Exception {
        // first get all linear versions
        VersionIterator iter = versionManager.getVersionHistory(versionableNode.getPath()).getAllLinearVersions();
        StringBuffer expected = new StringBuffer();
        while (iter.hasNext()) {
            expected.append(iter.nextVersion().getName()).append(",");
        }
        // restore version
        versionManager.restore(new Version[] {version}, true);
        // append new base version
        expected.append(versionManager.getBaseVersion(versionableNode.getPath()).getName()).append(",");

        // get the version names again
        iter = versionManager.getVersionHistory(versionableNode.getPath()).getAllLinearVersions();
        StringBuffer actual = new StringBuffer();
        while (iter.hasNext()) {
            actual.append(iter.nextVersion().getName()).append(",");
        }
        assertEquals("Node.restore() on simple versioning must create a new version.",
                expected.toString(), actual.toString());
    }
}
