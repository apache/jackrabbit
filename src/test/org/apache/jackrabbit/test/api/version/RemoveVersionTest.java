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

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Node;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import java.util.Arrays;
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

        vHistory = versionableNode.getVersionHistory();

        if (vHistory == null) {
            fail("VersionHistory must be created on persistent creation of a versionable node.");
        }

        Version testV = versionableNode.checkin();
        try {
            vHistory.removeVersion(testV.getName());
        } catch (UnsupportedRepositoryOperationException e) {
            throw new NotExecutableException("Removing version is not supported: "+e.getMessage());
        }

        versionableNode.checkout();
        version = versionableNode.checkin();
        // create a second version
        versionableNode.checkout();
        version2 = versionableNode.checkin();

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
     * Test if the predecessors of the removed version are made predecessor of
     * its original successor version.
     *
     * @throws RepositoryException
     */
    public void testRemoveVersionAdjustPredecessorSet() throws RepositoryException {

        // retrieve predecessors to test and remove the version
        List predecList = new ArrayList(Arrays.asList(version.getPredecessors()));
        vHistory.removeVersion(version.getName());

        // new predecessors of the additional version
        Version[] predec2 = version2.getPredecessors();
        for (int i = 0; i < predec2.length; i++) {
            if (!predecList.remove(predec2[i])) {
                fail("All predecessors of the removed version must be made predecessors of it's original successor version.");
            }
        }

        if (!predecList.isEmpty()) {
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
            List successorList = Arrays.asList(predec[i].getSuccessors());
            if (!successorList.contains(version2)) {
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
}