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

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.Version;
import javax.jcr.version.VersionIterator;
import java.util.HashSet;

/**
 * <code>VersionHistoryTest</code> provides test methods related to version
 * history methods and general version history issues.
 *
 * @test
 * @sources VersionHistoryTest.java
 * @executeClass org.apache.jackrabbit.test.api.version.VersionHistoryTest
 * @keywords versioning
 */
public class VersionHistoryTest extends AbstractVersionTest {

    protected VersionHistory vHistory;

    protected void setUp() throws Exception {
        super.setUp();

        vHistory = versionableNode.getVersionHistory();

        if (vHistory == null) {
            fail("VersionHistory must be created on persistent creation of a versionable node.");
        }
    }

    /**
     * Test if initially there is an auto-created root version present in the version history.
     *
     * @throws RepositoryException
     */
    public void testAutocreatedRootVersion() throws RepositoryException {
        Version rootVersion = vHistory.getRootVersion();
        if (rootVersion == null) {
            fail("The version history must contain an autocreated root version");
        }
    }

    /**
     * The version history must initially contain a single version (root version).
     *
     * @throws RepositoryException
     */
    public void testInitialNumberOfVersions() throws RepositoryException {
        long initialSize = getNumberOfVersions(vHistory);
        assertTrue("VersionHistory.getAllVersions() initially returns an iterator with a single version.", initialSize == 1);
    }

    /**
     * Test if the iterator returned by {@link javax.jcr.version.VersionHistory#getAllVersions()}
     * contains the root version upon creation of the version history.
     *
     * @throws RepositoryException
     * @see javax.jcr.version.VersionHistory#getRootVersion()
     */
    public void testInitallyGetAllVersionsContainsTheRootVersion() throws RepositoryException {
        Version rootVersion = vHistory.getRootVersion();
        Version v = null;
        VersionIterator it = vHistory.getAllVersions();
        while(it.hasNext()) {
            // break after the first version, that MUST be the root version
            v = it.nextVersion();
            break;
        }
        assertEquals("The version that is autocreated on version history creation must be the root version", rootVersion, v);
    }

    /**
     * Test that {@link VersionHistory#getAllVersions()} returns an iterator containing the
     * root version and all versions that have been created by Node.checkin().
     *
     * @throws RepositoryException
     * @see javax.jcr.version.VersionHistory#getAllVersions()
     */
    public void testGetAllVersions() throws RepositoryException {
        int cnt = 5;
        HashSet versions = new HashSet();
        versions.add(vHistory.getRootVersion());
        for (int i = 0; i < cnt; i++) {
            versions.add(versionableNode.checkin());
            versionableNode.checkout();
        }

        VersionIterator it = vHistory.getAllVersions();
        while (it.hasNext()) {
            Version v = it.nextVersion();
            if (!versions.contains(v)) {
                fail("VersionHistory.getAllVersions() must only contain the root version and versions, that have been created by a Node.checkin() call.");
            }
            versions.remove(v);
        }
        assertTrue("VersionHistory.getAllVersions() must contain the root version and all versions that have been created with a Node.checkin() call.", versions.isEmpty());
    }

    /**
     * Test if UnsupportedRepositoryOperationException is thrown when calling
     * Node.getVersionHistory() on a non-versionable node.
     *
     * @throws RepositoryException
     */
    public void testGetVersionHistoryOnNonVersionableNode() throws RepositoryException {
        try {
            nonVersionableNode.getVersionHistory();
            fail("Node.getVersionHistory() must throw UnsupportedRepositoryOperationException if the node is not versionable.");
        } catch (UnsupportedRepositoryOperationException e) {
            //success
        }
    }

    /**
     * Test VersionHistory.getVersion(String versionName) if 'versionName' is
     * the name of an existing version (created by Node.checkin()).
     *
     * @throws RepositoryException
     * @see VersionHistory#getVersion(String)
     */
    public void testGetVersion() throws RepositoryException {

        Version v = versionableNode.checkin();
        Version v2 = vHistory.getVersion(v.getName());

        assertEquals("VersionHistory.getVersion(String versionName) must return the version that is identified by the versionName specified, if versionName is the name of a version created by Node.checkin().", v, v2);
    }
}