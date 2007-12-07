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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;

import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * <code>AbstractWorkspaceCopyTest</code> is the abstract base class for all
 * copying/moving related test classes in one workspace.
 */
abstract class AbstractWorkspaceCopyTest extends AbstractJCRTest {

    /**
     * A referenceable node in default workspace
     */
    protected Node node1;

    /**
     * A non-referenceable node in default workspace
     */
    protected Node node2;


    /**
     * The workspace in the default session.
     */
    Workspace workspace;

    protected void setUp() throws Exception {
        super.setUp();

        initNodes();

        workspace = superuser.getWorkspace();
    }

    protected void tearDown() throws Exception {
        node1 = null;
        node2 = null;
        workspace = null;
        super.tearDown();
    }


    /**
     * Build persistent referenceable and non-referenceable nodes<br>
     */
    private void initNodes() {
        // create a referenceable node
        try {
            node1 = testRootNode.addNode(nodeName1, testNodeType);
        } catch (RepositoryException e) {
            fail("Failed to create test node." + e.getMessage());
        }
        // create a non-referenceable node
        try {
            node2 = testRootNode.addNode(nodeName2, testNodeType);
            testRootNode.save();
        } catch (RepositoryException e) {
            fail("Failed to createtest node." + e.getMessage());
        }
    }

}