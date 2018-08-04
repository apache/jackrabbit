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

import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionManager;

/**
 * <code>GetPredecessorsTest</code>  provides test methods covering {@link
 * Version#getPredecessors()}, {@link Version#getLinearPredecessor()} and
 * {@link Version#getLinearSuccessor()}.
 *
 */
public class GetPredecessorsTest extends AbstractVersionTest {

    /**
     * Returns the predecessor versions of this version. This corresponds to
     * returning all the nt:version nodes whose jcr:successors property includes
     * a reference to the nt:version node that represents this version. A
     * RepositoryException is thrown if an error occurs.
     */
    public void testGetPredecessors() throws RepositoryException {
        // create a new version
        versionableNode.checkout();
        Version version = versionableNode.checkin();

        assertTrue("Version should have at minimum one predecessor version.", version.getPredecessors().length > 0);
    }

    /**
     * Checks obtaining the linear predecessor.
     * @since JCR 2.0
     */
    public void testGetLinearPredecessorSuccessor() throws RepositoryException {

        String path = versionableNode.getPath();

        VersionManager vm = versionableNode.getSession().getWorkspace().getVersionManager();

        // get the previous version
        Version pred = vm.getBaseVersion(path);

        // shouldn't have a predecessor
        assertNull(pred.getLinearPredecessor());

        // shouldn't have a successor yet
        assertNull(pred.getLinearSuccessor());

        // check root version
        Version root = vm.getVersionHistory(path).getRootVersion();
        assertNull(root.getLinearSuccessor());

        // create a new version
        vm.checkout(path);
        Version version = vm.checkin(path);

        // refresh the predecessor
        pred = (Version)versionableNode.getSession().getNode(pred.getPath());

        assertTrue("linear predecessor of new version should be previous version",
                version.getLinearPredecessor().isSame(pred));
        assertTrue("linear successor of previous version should be new version",
                pred.getLinearSuccessor().isSame(version));
    }

}
