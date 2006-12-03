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

import java.util.GregorianCalendar;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.ConstraintViolationException;

/**
 * <code>VersionStorageTest</code> provides tests regarding {@link
 * javax.jcr.version.VersionHistory#addVersionLabel(String, String, boolean)}
 *
 * @test
 * @sources VersionStorageTest.java
 * @executeClass org.apache.jackrabbit.test.api.version.VersionStorageTest
 * @keywords versioning
 */
public class VersionStorageTest extends AbstractVersionTest {

    // path to version storage
    protected String versionStoragePath;

    protected void setUp() throws Exception {
        super.setUp();

        // get versionStorage path
        versionStoragePath = superuser.getNamespacePrefix(NS_JCR_URI) + ":system/" + superuser.getNamespacePrefix(NS_JCR_URI) + ":versionStorage";
    }

    /**
     * Entire subtree is protected.
     */
    public void testVersionStorageProtected() throws RepositoryException {
        try {
            versionableNode.getBaseVersion().setProperty(jcrCreated, GregorianCalendar.getInstance());
            fail("It should not be possible to modify a subnode/version in version storage.");
        } catch (ConstraintViolationException e) {
            // success
        }
    }

    /**
     * The full set of version histories in the version storage, though stored
     * in a single location in the repository, must be reflected in each
     * workspace as a subtree below the node /jcr:system/jcr:versionStorage.
     * Entire subtree must be identical across all workspaces and is protected.
     */
    public void testVersionStorageIdenticalAcrossAllWorkspaces() throws RepositoryException {
        // The superuser session for the second workspace
        Session superuserW2 = helper.getSuperuserSession(workspaceName);

        try {
            // check path to version storage
            assertTrue("Version strorage must be reflected as a subtree below the node '" + versionStoragePath + "'", superuserW2.getRootNode().hasNode(versionStoragePath));

            // check if subnodes in versionStorage are protected
            try {
                // try to create a version node
                Node versionStorageNodeW2 = superuserW2.getRootNode().getNode(versionStoragePath);
                versionStorageNodeW2.addNode(nodeName1, ntVersion);
                fail("It should not be possible to add a subnode/version in version storage.");
            } catch (ConstraintViolationException e) {
                // success
            }
        } finally {
            superuserW2.logout();
        }
    }

}