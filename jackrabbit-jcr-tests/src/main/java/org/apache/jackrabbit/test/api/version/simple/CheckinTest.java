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
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionManager;

/**
 * <code>CheckinTest</code> covers tests related to {@link javax.jcr.Node#checkin()}
 * on simple versionable nodes.
 *
 */
public class CheckinTest extends AbstractVersionTest {

    protected void setUp() throws Exception {
        super.setUp();

        try {
            VersionManager versionManager = versionableNode.getSession().getWorkspace().getVersionManager();
            String path = versionableNode.getPath();
            versionManager.checkout(path);
        } catch (RepositoryException e) {
            cleanUp();
            throw e;
        }
    }

    /**
     * Test if Node.isCheckedOut() return false after calling Node.checkin()
     *
     * @throws javax.jcr.RepositoryException
     */
    @SuppressWarnings("deprecation")
    public void testIsCheckedOut() throws RepositoryException {
        versionableNode.checkin();
        assertTrue("After calling Node.checkin() on a versionable node N, N.isCheckedOut() must return false", versionableNode.isCheckedOut() == false);
    }

    /**
     * Test if VersionManager.isCheckedOut(P) returns false if P is the
     * absolute path of a checked-in versionable node.
     *
     * @throws javax.jcr.RepositoryException
     */
    public void testIsCheckedOutJcr2() throws RepositoryException {
        VersionManager versionManager = versionableNode.getSession().getWorkspace().getVersionManager();
        String path = versionableNode.getPath();
        versionManager.checkin(path);
        assertTrue("VersionManager.isCheckedOut(P) must return false if the path P resolves to a checked-in node.", versionManager.isCheckedOut(path) == false);
    }

    /**
     * Test if Node.checkin() on a checked-in node has no effect.
     *
     * @throws RepositoryException
     */
    @SuppressWarnings("deprecation")
    public void testMultipleCheckinHasNoEffect() throws RepositoryException {

        Version v = versionableNode.checkin();
        try {
            Version v2 = versionableNode.checkin();

            assertTrue("Calling checkin() on a node that is already checked-in must not have an effect.", v.isSame(v2));
        } catch (RepositoryException e) {
            fail("Calling checkin() on a node that is already checked-in must not throw an exception.");
        }
    }

    /**
     * Test if VersionManager.checkin(P) has no effect if the path P resolves
     * to a checked-in node.
     *
     * @throws RepositoryException
     */
    public void testMultipleCheckinHasNoEffectJcr2() throws RepositoryException {

        VersionManager versionManager = versionableNode.getSession().getWorkspace().getVersionManager();
        String path = versionableNode.getPath();
        Version v = versionManager.checkin(path);
        try {
            Version v2 = versionManager.checkin(path);

            assertTrue("Calling VersionManager.checkin(P) must not have an if the path P resolves to a node that is already checked-in.", v.isSame(v2));
        } catch (RepositoryException e) {
            fail("Calling VersionManager.checkin(P) must not throw an exception if the path P resolves to a node that is already checked-in.");
        }
    }

    /**
     * Test if Node.checkin() throws InvalidItemStateException if the node
     * has unsaved changes pending.
     *
     * @throws RepositoryException
     */
    @SuppressWarnings("deprecation")
    public void testCheckinWithPendingChanges() throws RepositoryException {
        try {
            // modify node without calling save()
            versionableNode.setProperty(propertyName1, propertyValue);
            versionableNode.checkin();

            fail("InvalidItemStateException must be thrown on attempt to checkin a node having any unsaved changes pending.");
        } catch (InvalidItemStateException e) {
            // ok
        }
    }

    /**
     * Test if VersionManager.checkin(P) throws InvalidItemStateException if
     * the path P resolves to a node that has unsaved changes pending.
     *
     * @throws RepositoryException
     */
    public void testCheckinWithPendingChangesJcr2() throws RepositoryException {
        try {
            // modify node without calling save()
            versionableNode.setProperty(propertyName1, propertyValue);
            VersionManager versionManager = versionableNode.getSession().getWorkspace().getVersionManager();
            String path = versionableNode.getPath();
            versionManager.checkin(path);

            fail("InvalidItemStateException must be thrown on attempt to checkin a node having any unsaved changes pending.");
        } catch (InvalidItemStateException e) {
            // ok
        }
    }

    /**
     * Test if Node.isCheckedOut() returns false after Node.checkin().
     *
     * @throws RepositoryException
     */
    @SuppressWarnings("deprecation")
    public void testIsNotCheckedOut() throws RepositoryException {
        versionableNode.checkin();
        boolean isCheckedOut = versionableNode.isCheckedOut();

        assertFalse("Node.isCheckedOut() must return false after Node.checkin().", isCheckedOut);
    }

    /**
     * Test if VersionManager.isCheckedOut(P) returns false after calling VersionManager.checkin(P).
     *
     * @throws RepositoryException
     */
    public void testIsNotCheckedOutJcr2() throws RepositoryException {
        VersionManager versionManager = versionableNode.getSession().getWorkspace().getVersionManager();
        String path = versionableNode.getPath();
        versionManager.checkin(path);
        boolean isCheckedOut = versionManager.isCheckedOut(path);

        assertFalse("VersionManager.isCheckedOut(P) must return false after VersionManager.checkin(P).", isCheckedOut);
    }

    /**
     * Test if Node.checkin() adds another version to the VersionHistory
     *
     * @throws RepositoryException
     */
    @SuppressWarnings("deprecation")
    public void testCheckinCreatesNewVersion() throws RepositoryException {

        long initialNumberOfVersions = getNumberOfVersions(versionableNode.getVersionHistory());
        versionableNode.checkin();
        long numberOfVersions = getNumberOfVersions(versionableNode.getVersionHistory());

        assertTrue("Checkin must create a new Version in the VersionHistory.", numberOfVersions == initialNumberOfVersions + 1);
    }

    /**
     * Test if VersionManager.checkin(String) adds another version to the VersionHistory
     *
     * @throws RepositoryException
     */
    public void testCheckinCreatesNewVersionJcr2() throws RepositoryException {

        VersionManager versionManager = versionableNode.getSession().getWorkspace().getVersionManager();
        String path = versionableNode.getPath();
        long initialNumberOfVersions = getNumberOfVersions(versionManager.getVersionHistory(path));
        versionManager.checkin(path);
        long numberOfVersions = getNumberOfVersions(versionManager.getVersionHistory(path));

        assertTrue("Checkin must create a new Version in the VersionHistory.", numberOfVersions == initialNumberOfVersions + 1);
    }

    /**
     * Test calling Node.checkin() on a non-versionable node.
     *
     * @throws RepositoryException
     */
    @SuppressWarnings("deprecation")
    public void testCheckinNonVersionableNode() throws RepositoryException {
        try {
            nonVersionableNode.checkin();
            fail("Node.checkin() on a non-versionable node must throw UnsupportedRepositoryOperationException");
        } catch (UnsupportedRepositoryOperationException e) {
            //success
        }
    }

    /**
     * Test calling VersionManager.checkin(P) with the path P resolving to
     * a non-versionable node.
     *
     * @throws RepositoryException
     */
    public void testCheckinNonVersionableNodeJcr2() throws RepositoryException {
        try {
            VersionManager versionManager = nonVersionableNode.getSession().getWorkspace().getVersionManager();
            String path = nonVersionableNode.getPath();
            versionManager.checkin(path);
            fail("VersionManager.checkin(P) must throw UnsupportedRepositoryOperationException if the path P resolves to a non-versionable node.");
        } catch (UnsupportedRepositoryOperationException e) {
            //success
        }
    }
}
