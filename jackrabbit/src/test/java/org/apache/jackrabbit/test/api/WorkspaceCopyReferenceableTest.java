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

import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * <code>WorkspaceCopyReferenceableTest</code> contains tests for copying
 * referenceable nodes in one workspace.
 *
 * @test
 * @sources WorkspaceCopyReferenceableTest.java
 * @executeClass org.apache.jackrabbit.test.api.WorkspaceCopyReferenceableTest
 * @keywords level2
 */
public class WorkspaceCopyReferenceableTest extends AbstractWorkspaceReferenceableTest {

    protected String getOtherWorkspaceName() throws NotExecutableException {
        return workspace.getName();
    }

    protected void initNodesW2() throws RepositoryException {
        // nothing to do.
    }

    /**
     * Copies of referenceable nodes (nodes with UUIDs) are automatically given
     * new UUIDs.
     */
    public void testCopyNodesNewUUID() throws RepositoryException {
        // add mixin referenceable to node1
        addMixinReferenceableToNode(node1);
        
        // copy referenceable node below non-referenceable node
        String dstAbsPath = node2.getPath() + "/" + node1.getName();
        workspace.copy(node1.getPath(), dstAbsPath);

        // uuid of copied node should be different than original node uuid
        String originalUUID = node1.getUUID();
        Node copiedNode = node2.getNode(node1.getName());
        String copiedUUID = copiedNode.getUUID();

        assertFalse(originalUUID.equals(copiedUUID));
    }
}
