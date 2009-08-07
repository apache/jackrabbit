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

import java.util.Arrays;
import java.util.HashSet;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

/**
 * <code>VersionLabelTest</code> covers methods related to version label such as
 * <ul>
 * <li>{@link VersionHistory#addVersionLabel(String, String, boolean)}</li>
 * <li>{@link VersionHistory#removeVersionLabel(String)}</li>
 * <li>{@link VersionHistory#restoreByLabel(String, boolean)} </li>
 * <li>{@link VersionHistory#getVersionByLabel(String)}</li>
 * <li>{@link javax.jcr.version.VersionHistory#getVersionLabels()} </li>
 * <li>{@link javax.jcr.version.VersionHistory#hasVersionLabel(javax.jcr.version.Version, String)}</li>
 * <li>{@link VersionHistory#hasVersionLabel(String)}</li>
 * <li>{@link VersionHistory#hasVersionLabel(javax.jcr.version.Version, String)} </li>
 * </ul>
 *
 * @test
 * @sources VersionLabelTest.java
 * @executeClass org.apache.jackrabbit.test.api.version.VersionLabelTest
 * @keywords versioning
 */
public class VersionLabelTest extends AbstractVersionTest {

    protected String versionLabel = "foo";
    protected String versionLabel2 = "bar";

    protected VersionHistory vHistory;
    protected Version rootVersion;

    /**
     * JCR Name jcr:versionLabels using the namespace resolver of the current session.
     */
    protected String jcrVersionLabels;

    protected void setUp() throws Exception {
        super.setUp();

        jcrVersionLabels = superuser.getNamespacePrefix(NS_JCR_URI) + ":versionLabels";

        vHistory = versionableNode.getVersionHistory();
        rootVersion = vHistory.getRootVersion();

        if (vHistory.hasVersionLabel(versionLabel)) {
            fail("Version label '" + versionLabel + "' is already present in this version history. Label test cannot be performed.");
        }

        if (vHistory.hasVersionLabel(versionLabel2)) {
            fail("Version label '" + versionLabel2 + "' is already present in this version history. Label test cannot be performed.");
        }
    }

    protected void tearDown() throws Exception {
        try {
            // clean up: remove the version labels again.
            vHistory.removeVersionLabel(versionLabel);
            vHistory.removeVersionLabel(versionLabel2);
        } catch (RepositoryException e) {
            // ignore
        }
        vHistory = null;
        rootVersion = null;
        super.tearDown();
    }

    /**
     * Test if the number of labels available in the version history is increased
     * by added a new label.
     *
     * @throws RepositoryException
     * @see VersionHistory#addVersionLabel(String, String, boolean)
     */
    public void testAddVersionLabel() throws RepositoryException {
        int initialLength = vHistory.getVersionLabels().length;
        vHistory.addVersionLabel(rootVersion.getName(), versionLabel, false);
        String[] labels = vHistory.getVersionLabels();

        assertEquals("A version label that has been successfully added must increes the total number of version labels available in the history.", initialLength + 1, labels.length);
    }

    /**
     * Test if the a label added with VersionHistory.addVersionLabel(String, String, boolean)
     * is present in the array returned by VersionHistory.getVersionLabels(), if
     * the label has not been present before.
     *
     * @throws RepositoryException
     * @see VersionHistory#addVersionLabel(String, String, boolean)
     */
    public void testAddVersionLabel2() throws RepositoryException {
        vHistory.addVersionLabel(rootVersion.getName(), versionLabel, false);
        String[] labels = vHistory.getVersionLabels();
        boolean found = false;
        for (int i = 0; i < labels.length; i++) {
            if (labels[i].equals(versionLabel)) {
                found = true;
                break;
            }
        }
        assertTrue("The version label that has been successfully added must be present in the array containing all labels.", found);
    }

    /**
     * Test if the a label added with VersionHistory.addVersionLabel(String,
     * String, boolean) corresponds to adding a reference property to the
     * jcr:versionLabels node of this history node, with the label as name of
     * the property, and the reference targeting the version.
     *
     * @see VersionHistory#addVersionLabel(String, String, boolean)
     */
    public void testAddVersionCheckVersionLabelsNode() throws RepositoryException {
        vHistory.addVersionLabel(rootVersion.getName(), versionLabel, false);

        // get jcr:versionLabels node
        vHistory = versionableNode.getVersionHistory();
        Node versionLabelNode = vHistory.getNode(jcrVersionLabels);

        assertTrue("The version label that has been successfully added must be present in the node '" + jcrVersionLabels + "'.", versionLabelNode.getProperty(versionLabel).getString().equals(rootVersion.getUUID()));
    }

    /**
     * Test if VersionHistory.hasVersionLabel(String) returns true, if the label
     * has beed successfully added before.
     *
     * @throws RepositoryException
     * @see VersionHistory#hasVersionLabel(String)
     */
    public void testHasVersionLabel() throws RepositoryException {
        vHistory.addVersionLabel(rootVersion.getName(), versionLabel, false);
        assertTrue("VersionHistory.hasVersionLabel(String) must return true if the label has been sucessfully added.", vHistory.hasVersionLabel(versionLabel));
    }

    /**
     * Test if VersionHistory.hasVersionLabel(Version, String) returns true, if the label
     * has beed successfully added before to the specified version.
     *
     * @throws RepositoryException
     * @see VersionHistory#hasVersionLabel(javax.jcr.version.Version, String)
     */
    public void testHasVersionLabelForVersion() throws RepositoryException {
        vHistory.addVersionLabel(rootVersion.getName(), versionLabel, false);
        assertTrue("VersionHistory.hasVersionLabel(Version, String) must return true if the label has been sucessfully added.", vHistory.hasVersionLabel(rootVersion, versionLabel));
    }

    /**
     * Test if multiple distinct version labels can be added for a given version.
     *
     * @throws RepositoryException
     */
    public void testAddMultipleVersionLabels() throws RepositoryException {
        try {
            vHistory.addVersionLabel(rootVersion.getName(), versionLabel, false);
            vHistory.addVersionLabel(rootVersion.getName(), versionLabel2, false);
        } catch (VersionException e) {
            fail("Adding multiple distict version labels to a version must be allowed.");
        }
    }

    /**
     * Test if VersionHistory.addVersionLabel(versionName, label, moveLabel)
     * throws VersionException the label already exists and if moveLabel is false)
     *
     * @throws RepositoryException
     */
    public void testAddDuplicateVersionLabel() throws RepositoryException {
        vHistory.addVersionLabel(rootVersion.getName(), versionLabel, false);
        try {
            versionableNode.checkout();
            Version v = versionableNode.checkin();
            vHistory.addVersionLabel(v.getName(), versionLabel, false);

            fail("Adding a version label that already exist in the version history must throw a VersionException.");
        } catch (VersionException e) {
            //success
        }
    }

    /**
     * Test if the 'moveLabel' flag moves an existing version label.
     *
     * @throws RepositoryException
     * @see VersionHistory#addVersionLabel(String, String, boolean)  with boolan flag equals true.
     */
    public void testMoveLabel() throws RepositoryException {
        vHistory.addVersionLabel(rootVersion.getName(), versionLabel, false);
        try {
            versionableNode.checkout();
            Version v = versionableNode.checkin();
            vHistory.addVersionLabel(v.getName(), versionLabel, true);

            if (!vHistory.hasVersionLabel(v, versionLabel)) {
                fail("If 'moveLabel' is true, an existing version label must be moved to the indicated version.");
            }

        } catch (VersionException e) {
            fail("If 'moveLabel' is true, an existing version label must be moved to the indicated version.");
        }
    }

    /**
     * Test the removal of an version label that does not exist (must throw VersionException).
     *
     * @throws RepositoryException
     */
    public void testRemoveNonExistingLabel() throws RepositoryException {
        if (vHistory.hasVersionLabel(versionLabel)) {
            fail("Testing the removal on a non-existing version label failed: '" + versionLabel + "' exists on version history.");
        }
        try {
            vHistory.removeVersionLabel(versionLabel);
            fail("VersionHistory.removeLabel(String) must throw a VersionException if the label does not exist.");
        } catch (VersionException e) {
            // success
        }
    }

    /**
     * Test the removal of an version label that has successfully been added
     * before.
     *
     * @throws RepositoryException
     * @see VersionHistory#removeVersionLabel(String)
     */
    public void testRemoveLabel() throws RepositoryException {

        vHistory.addVersionLabel(rootVersion.getName(), versionLabel, false);
        vHistory.removeVersionLabel(versionLabel);

        assertFalse("VersionHistory.removeLabel(String) must remove the version label if it exists (has successfully been added before).", vHistory.hasVersionLabel(versionLabel));
    }

    /**
     * Test if VersionHistory.getVersionByLabel(String) returns the version that
     * has been specified with the addVersionLabel call.
     *
     * @throws RepositoryException
     * @see VersionHistory#getVersionByLabel(String)
     */
    public void testGetVersionByLabel() throws RepositoryException {
        vHistory.addVersionLabel(rootVersion.getName(), versionLabel, true);
        Version v = vHistory.getVersionByLabel(versionLabel);

        assertTrue("VersionHistory.getVersionByLabel(String) must retrieve the particular version that was specified in addVersionLabel call.", v.isSame(rootVersion));
    }

    /**
     * Test VersionHistory.getVersionLabels() returns all labels present on the version history.
     *
     * @throws RepositoryException
     * @see javax.jcr.version.VersionHistory#getVersionLabels()
     */
    public void testGetVersionLabels() throws RepositoryException {

        HashSet testLabels = new HashSet(Arrays.asList(vHistory.getVersionLabels()));
        versionableNode.checkout();
        Version v = versionableNode.checkin();

        vHistory.addVersionLabel(v.getName(), versionLabel, false);
        testLabels.add(versionLabel);
        vHistory.addVersionLabel(rootVersion.getName(), versionLabel2, false);
        testLabels.add(versionLabel2);

        String[] labels = vHistory.getVersionLabels();
        for (int i = 0; i < labels.length; i++) {
            String l = labels[i];
            if (!testLabels.contains(l)) {
                fail("VersionHistory.getVersionLabels() must only return labels, that have been added to the history.");
            }
            testLabels.remove(l);
        }

        assertTrue("VersionHistory.getVersionLabels() must return all labels, that have been added to the history.", testLabels.isEmpty());
    }

    /**
     * Test VersionHistory.getVersionLabels(Version) only returns all labels present
     * for the specified version.
     *
     * @throws RepositoryException
     * @see VersionHistory#getVersionLabels(javax.jcr.version.Version)
     */
    public void testGetVersionLabelsForVersion() throws RepositoryException {

        HashSet testLabels = new HashSet(Arrays.asList(vHistory.getVersionLabels(rootVersion)));

        vHistory.addVersionLabel(rootVersion.getName(), versionLabel, false);
        testLabels.add(versionLabel);

        // add a version label to another version (not added to the testLabel set)
        versionableNode.checkout();
        Version v = versionableNode.checkin();
        vHistory.addVersionLabel(v.getName(), versionLabel2, false);

        String[] labels = vHistory.getVersionLabels(rootVersion);
        for (int i = 0; i < labels.length; i++) {
            String l = labels[i];
            if (!testLabels.contains(l)) {
                fail("VersionHistory.getVersionLabels(Version) must only return labels, that have been added for this version.");
            }
            testLabels.remove(l);
        }

        assertTrue("VersionHistory.getVersionLabels(Version)  must return all labels, that have been added for this version.", testLabels.isEmpty());
    }

    /**
     * Test calling Node.restoreByLabel(String, boolean) on a non-versionable node.
     *
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Node#restoreByLabel(String, boolean)
     */
    public void testRestoreByLabelNonVersionableNode() throws RepositoryException {
        try {
            nonVersionableNode.restoreByLabel(versionLabel, true);
            fail("Node.restoreByLabel(String, boolean) on a non versionable node must throw UnsupportedRepositoryOperationException");
        } catch (UnsupportedRepositoryOperationException e) {
            //success
        }
    }

    /**
     * Test if versionHistory.getVersionLabels(Version) throws a VersionException if the
     * specified version is not in this version history.
     */
    public void testGetVersionLabelsForInvalidVersion() throws Exception {
        // build a second versionable node below the testroot to get it's version.
        Node versionableNode2 = createVersionableNode(testRootNode, nodeName2, versionableNodeType);
        Version invalidV = versionableNode2.checkin();

        try {
            vHistory.getVersionLabels(invalidV);
            fail("VersionHistory.getVersionLabels(Version) must throw a VersionException if the specified version is not in this version history");
        } catch (VersionException ve) {
            // success
        }
    }
}
