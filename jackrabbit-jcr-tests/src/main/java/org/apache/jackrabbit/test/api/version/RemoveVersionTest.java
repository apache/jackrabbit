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

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Node;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.Value;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import java.util.ArrayList;
import java.util.List;

/**
 * <code>RemoveVersionTest</code> provides test methods covering {@link VersionHistory#removeVersion(String)}.
 * Please note, that removing versions is defined to be an optional feature in
 * the JSR 170 specification. The setup therefore includes a initial removal,
 * in order to test, whether removing versions is supported.
 *
 * @test
 * @sources RemoveVersionTest.java
 * @executeClass org.apache.jackrabbit.test.api.version.RemoveVersionTest
 * @keywords versioning
 */
public class RemoveVersionTest extends AbstractVersionTest {

    protected Node versionableNode2;
    protected Version version;
    protected Version version2;

    protected VersionHistory vHistory;

    protected void setUp() throws Exception {
        super.setUp();

        Version testV = versionableNode.checkin(); // create 1.0
        versionableNode.checkout();
        versionableNode.checkin(); // create 1.1
        versionableNode.checkout();
        versionableNode.checkin(); // create 1.2
        try {
            versionableNode.getVersionHistory().removeVersion(testV.getName());
        } catch (UnsupportedRepositoryOperationException e) {
            throw new NotExecutableException("Removing version is not supported: " + e.getMessage());
        }

        versionableNode.checkout();
        version = versionableNode.checkin();
        // create a second version
        versionableNode.checkout();
        version2 = versionableNode.checkin();

        vHistory = versionableNode.getVersionHistory();

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
        } finally {
            versionableNode2 = null;
            version = null;
            version2 = null;
            vHistory = null;
            super.tearDown();
        }
    }

    /**
     * Test removed version gets invalid
     */
    public void testRemovedInvalid() throws Exception {
        versionableNode.getVersionHistory().removeVersion(version.getName());
        // assert: version has become invalid
        try {
            version.getPredecessors();
            fail("Removed version still operational.");
        } catch (RepositoryException e) {
            // expected
        }
    }

    /**
     * Test if the predecessors of the removed version are made predecessor of
     * its original successor version.
     *
     * @throws RepositoryException
     */
    public void testRemoveVersionAdjustPredecessorSet() throws RepositoryException {

        // retrieve predecessors to test and remove the version
        List predecPaths = new ArrayList();
        Version[] predec = version.getPredecessors();
        for (int i = 0; i < predec.length; i++) {
            predecPaths.add(predec[i].getPath());
        }
        vHistory.removeVersion(version.getName());

        // new predecessors of the additional version
        Version[] predec2 = version2.getPredecessors();
        for (int i = 0; i < predec2.length; i++) {
            if (!predecPaths.remove(predec2[i].getPath())) {
                fail("All predecessors of the removed version must be made predecessors of it's original successor version.");
            }
        }

        if (!predecPaths.isEmpty()) {
            fail("All predecessors of the removed version must be made predecessors of it's original successor version.");
        }
    }

    /**
     * Test if the successors of the removed version are made successors of
     * all predecessors of the the removed version.
     *
     * @throws RepositoryException
     */
    public void testRemoveVersionAdjustSucessorSet() throws RepositoryException {

        // retrieve predecessors to test and remove the version
        Version[] predec = version.getPredecessors();
        vHistory.removeVersion(version.getName());

        for (int i = 0; i < predec.length; i++) {
            boolean isContained = false;
            Version[] succ = predec[i].getSuccessors();
            for (int j = 0; j < succ.length; j++) {
                isContained |= succ[j].isSame(version2);
            }
            if (!isContained) {
                fail("Removing a version must make all it's successor version to successors of the removed version's predecessors.");
            }
        }
    }

    /**
     * Test if removing a version from the version history throws a VersionException
     * if the specified version does not exist.
     *
     * @throws RepositoryException
     * @throws NotExecutableException
     */
    public void testRemoveInvalidVersion() throws RepositoryException, NotExecutableException {
        Version invalidV = versionableNode2.checkin();
        String invalidName = invalidV.getName();

        // build a version name that is not present in the current history
        boolean found = false;
        for (int i = 0; i < 10 && !found; i++) {
            try {
                vHistory.getVersion(invalidName);
                invalidName += i;
            } catch (VersionException e) {
                // ok > found a name that is invalid.
                found = true;
            }
        }

        if (!found) {
            throw new NotExecutableException("Failed to create an invalid name in order to test the removal of versions.");
        }

        try {
            vHistory.removeVersion(invalidName);
            fail("Removing a version that does not exist must fail with a VersionException.");
        } catch (VersionException e) {
            // success
        }
    }

    /**
     * Checks if {@link javax.jcr.version.VersionHistory#removeVersion(String)}
     * throws a {@link javax.jcr.ReferentialIntegrityException} if the named
     * version is still referenced by another node.
     * @tck.config nodetype name of a node type that supports a reference
     *  property.
     * @tck.config nodename4 name of the node created with <code>nodetype</code>.
     * @tck.config propertyname1 a single value reference property available
     *  in <code>nodetype</code>.
     */
    public void testReferentialIntegrityException() throws RepositoryException, NotExecutableException {
        // create reference: n1.p1 -> version
        Node n1 = testRootNode.addNode(nodeName4, testNodeType);
        Value refValue = superuser.getValueFactory().createValue(version);
        ensureCanSetProperty(n1, propertyName1, refValue);
        n1.setProperty(propertyName1, refValue);
        testRootNode.save();

        try {
            vHistory.removeVersion(version.getName());
            fail("Method removeVersion() must throw a ReferentialIntegrityException " +
                 "if the version is the target of a REFERENCE property and the current " +
                 "Session has read access to that REFERENCE property");
        }
        catch (ReferentialIntegrityException e) {
            // success
        }
    }

    /**
     * Checks if all versions but the base and root one can be removed.
     */
    public void testRemoveAllBut2() throws RepositoryException {
        String baseVersion = versionableNode.getBaseVersion().getName();
        VersionHistory vh = versionableNode.getVersionHistory();
        VersionIterator vi = vh.getAllVersions();
        while (vi.hasNext()) {
            Version currenVersion = vi.nextVersion();
            String versionName = currenVersion.getName();
            if (!versionName.equals("jcr:rootVersion") && !versionName.equals(baseVersion)) {
                vh.removeVersion(versionName);
            }
        }
    }

    /**
     * Checks if all versions by the base and root one can be removed.
     */
    public void testRemoveRootVersion() throws RepositoryException {
        try {
            versionableNode.getVersionHistory().getRootVersion().remove();
            fail("Removal of root version should throw an exception.");
        } catch (RepositoryException e) {
            // ignore
        }
    }

    /**
     * Checks if all versions by the base and root one can be removed.
     */
    public void testRemoveBaseVersion() throws RepositoryException {
        try {
            versionableNode.getBaseVersion().remove();
            fail("Removal of base version should throw an exception.");
        } catch (RepositoryException e) {
            // ignore
        }
    }
}