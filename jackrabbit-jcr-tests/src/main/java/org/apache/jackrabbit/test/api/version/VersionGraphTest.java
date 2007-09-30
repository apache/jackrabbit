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

import javax.jcr.version.Version;
import javax.jcr.RepositoryException;
import javax.jcr.Property;
import javax.jcr.Value;
import javax.jcr.UnsupportedRepositoryOperationException;

/**
 * <code>VersionGraphTest</code> contains test methods related to version graph
 * issues.
 *
 * @test
 * @sources VersionGraphTest.java
 * @executeClass org.apache.jackrabbit.test.api.version.VersionGraphTest
 * @keywords versioning
 */
public class VersionGraphTest extends AbstractVersionTest {

    /**
     * Test that the initial base version after creation of a versionable node
     * points to the root version.
     *
     * @throws javax.jcr.RepositoryException
     */
    public void testInitialBaseVersionPointsToRootVersion() throws RepositoryException {

        Version rV = versionableNode.getVersionHistory().getRootVersion();
        Version bV = versionableNode.getBaseVersion();

        assertTrue("After creation of a versionable node the node's baseVersion must point to the rootVersion in the version history.", rV.isSame(bV));
    }

    /**
     * Test if after creation of a versionable node N the multi-value
     * REFERENCE property jcr:predecessors of N is initialized to contain a
     * single UUID, that of the root version (the same as jcr:baseVersion).
     *
     * @throws RepositoryException
     */
    public void testInitialNodePredecessors() throws RepositoryException {

        Property predecessors = versionableNode.getProperty(jcrPredecessors);
        Value[] values = predecessors.getValues();
        Version rV = versionableNode.getVersionHistory().getRootVersion();
        if (values.length != 1) {
            fail("The jcr:predecessors property of a versionable node must be initialized to contain a single value");
        }

        Value initialVal = values[0];

        assertTrue("The jcr:predecessors property of a versionable node is initialized to contain a single UUID, that of the root version", initialVal.equals(superuser.getValueFactory().createValue(rV)));
    }

    /**
     * Test if the root version does not have any predecessor versions.
     *
     * @throws RepositoryException
     */
    public void testRootVersionHasNoPredecessor() throws RepositoryException {
        Version[] predec = versionableNode.getVersionHistory().getRootVersion().getPredecessors();
        assertTrue("The root version may not have any predecessors.", predec.length == 0);
    }

    /**
     * Test if UnsupportedRepositoryOperationException is thrown when calling
     * Node.getVersionHistory() on a non-versionable node.
     *
     * @throws RepositoryException
     */
    public void testGetBaseVersionOnNonVersionableNode() throws RepositoryException {
        try {
            nonVersionableNode.getBaseVersion();
            fail("Node.getBaseVersion() must throw UnsupportedRepositoryOperationException if the node is not versionable.");
        } catch (UnsupportedRepositoryOperationException e) {
            //success
        }
    }
}