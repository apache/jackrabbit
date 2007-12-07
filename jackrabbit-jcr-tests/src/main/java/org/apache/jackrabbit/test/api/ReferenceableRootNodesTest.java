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

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * <code>ReferenceableRootNodesTest</code> contains tests with referenceable
 * nodes between different workspaces.
 *
 * @test
 * @sources ReferenceableRootNodesTest.java
 * @executeClass org.apache.jackrabbit.test.api.ReferenceableRootNodesTest
 * @keywords level1
 */
public class ReferenceableRootNodesTest extends AbstractJCRTest {

    /**
     * The read-only session for the second workspace
     */
    protected Session sessionW2;

    /**
     * The read-only session for the default workspace
     */
    protected Session session;

    protected void setUp() throws Exception {
        isReadOnly = true;
        super.setUp();

        session = helper.getReadOnlySession();
        sessionW2 = helper.getReadOnlySession(workspaceName);
                
        String wspName = session.getWorkspace().getName();
        boolean sameWsp = (wspName == null) ? workspaceName == null : wspName.equals(workspaceName);
        if (sameWsp) {
            throw new NotExecutableException("Cannot compare uuid behaviour of different workspaces. Only a single workspace configured.");
        }
    }

    /**
     * Releases the sessions aquired in {@link #setUp()}.
     */
    protected void tearDown() throws Exception {
        if (sessionW2 != null) {
            sessionW2.logout();
            sessionW2 = null;
        }
        if (session != null) {
            session.logout();
            session = null;
        }
        super.tearDown();
    }

    /**
     * A repository implementation may make its workspace root nodes
     * mix:referenceable. If so, then the root node of all workspaces must be
     * referenceable, and all must have the same UUID.
     */
    public void testReferenceableRootNode()
            throws RepositoryException, NotExecutableException {
        // compare UUID of default workspace and a second workspace
        Node rootNode = session.getRootNode();
        if (rootNode.isNodeType(mixReferenceable)) {

            // check if root node in second workspace is referenceable too
            Node rootNodeW2 = sessionW2.getRootNode();
            if (!rootNodeW2.isNodeType(mixReferenceable)) {
                fail("Root node in second workspace is not referenceable.");
            }

            // check if all root nodes have the same UUID
            assertEquals("Referenceable root nodes of different workspaces must have same UUID.",
                    rootNode.getUUID(),
                    rootNodeW2.getUUID());
        } else {
            throw new NotExecutableException("Root node is not referenceable");
        }
    }
}
