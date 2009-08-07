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
package org.apache.jackrabbit.test.api;

import javax.jcr.RepositoryException;
import javax.jcr.version.VersionException;


/**
 * <code>WorkspaceCloneVersionableTest</code> contains tests for cloning
 * versionable nodes between workspace.
 *
 * @test
 * @sources WorkspaceCloneVersionableTest.java
 * @executeClass org.apache.jackrabbit.test.api.WorkspaceCloneVersionableTest
 * @keywords level2 versioning
 */
public class WorkspaceCloneVersionableTest extends AbstractWorkspaceVersionableTest {

    /**
     * A VersionException is thrown if the parent node of destAbsPath is
     * versionable and checked-in, or is non-versionable but its nearest
     * versionable ancestor is checked-in.
     */
    public void testCloneNodesVersionableAndCheckedIn() throws RepositoryException {
        // prepare the test data
        // create a non-versionable node below a versionable node
        // required for having a nearest versionable ancestor to a nonversionable sub node
        String dstAbsPath = node1W2.getPath() + "/" + node2.getName();
        workspaceW2.copy(workspace.getName(), node1.getPath(), dstAbsPath);

        // make parent node versionable and check-in
        addMixinVersionableToNode(testRootNodeW2, node1W2);
        node1W2.checkin();

        // 1. parent node of destAbsPath is non-versionable but its nearest versionable ancestor is checked-in
        try {
            workspaceW2.clone(workspace.getName(), node2.getPath(), dstAbsPath + "/" + node2.getName(), true);
            fail("Cloning a node to a node's versionable and checked-in nearest ancestor node of destAbsPath should throw VersionException.");
        } catch (VersionException e) {
            // successful
        }

        // 2. parent node of destAbsPath is versionable and checked-in
        try {
            workspaceW2.clone(workspace.getName(), node2.getPath(), node1W2.getPath() + "/" + node2.getName(), true);
            fail("Cloning a node to a versionable and checked-in parent node of destAbsPath should throw VersionException.");
        } catch (VersionException e) {
            // successful
        }
    }
}