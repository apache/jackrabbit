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
 * <code>WorkspaceMoveReferenceableTest</code> contains tests for moving
 * referenceable nodes in one workspace.
 *
 * @test
 * @sources WorkspaceMoveReferenceableTest.java
 * @executeClass org.apache.jackrabbit.test.api.WorkspaceMoveReferenceableTest
 * @keywords level2
 */
public class WorkspaceMoveReferenceableTest extends AbstractWorkspaceReferenceableTest {

    protected String getOtherWorkspaceName() throws NotExecutableException {
        return workspace.getName();
    }

    protected void initNodesW2() throws RepositoryException {
        // nothing to do.
    }
    
    /**
     * Copies of referenceable nodes (nodes with UUIDs) remains their original
     * UUIDs.
     */
    public void testMoveNodesReferenceableNodesNewUUID() throws RepositoryException {
        // add mixin referenceable to node1
        addMixinReferenceableToNode(node1);
        
        // copy referenceable node below non-referenceable node
        String dstAbsPath = node2.getPath() + "/" + node1.getName();
        String originalUUID = node1.getUUID(); // remember for check
        workspace.move(node1.getPath(), dstAbsPath);

        // uuid of copied node should be different than original node uuid
        Node movedNode = node2.getNode(node1.getName());
        String movedUUID = movedNode.getUUID();

        assertTrue(originalUUID.equals(movedUUID));
    }
}
