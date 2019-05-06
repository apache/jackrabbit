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
package org.apache.jackrabbit.core;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

/**
 * Tests features available with shareable nodes that require multiple
 * workspaces.
 */
public class MultiWorkspaceShareableNodeTest extends AbstractJCRTest {

    /**
     * The superuser session for the non default workspace
     */
    protected Session superuserW2;

    /**
     * A read-write session for the non default workspace
     */
    protected Session rwSessionW2;

    /**
     * The workspace in the non default session.
     */
    Workspace workspaceW2;

    /**
     * The testroot node in the non default session
     */
    Node testRootNodeW2;

    /**
     * A referenceable node in default workspace
     */
    protected Node node1W2;

    /**
     * A non-referenceable node in default workspace
     */
    protected Node node2W2;

    /**
     * The workspace in the default session.
     */
    Workspace workspace;

    protected void setUp() throws Exception {
        super.setUp();

        workspace = superuser.getWorkspace();

        // init second workspace
        String otherWspName = getOtherWorkspaceName();
        superuserW2 = getHelper().getSuperuserSession(otherWspName);
        rwSessionW2 = getHelper().getReadWriteSession(otherWspName);
        workspaceW2 = superuserW2.getWorkspace();

        initNodesW2();
    }

    protected void tearDown() throws Exception {
        workspace = null;

        // remove all test nodes in second workspace
        if (superuserW2 != null) {
            try {
                if (!isReadOnly) {
                    // do a 'rollback'
                    cleanUpTestRoot(superuserW2);
                }
            } finally {
                superuserW2.logout();
                superuserW2 = null;
            }
        }
        if (rwSessionW2 != null) {
            rwSessionW2.logout();
            rwSessionW2 = null;
        }
        workspaceW2 = null;
        testRootNodeW2 = null;
        node1W2 = null;
        node2W2 = null;
        super.tearDown();
    }

    protected String getOtherWorkspaceName() throws NotExecutableException {
        if (workspace.getName().equals(workspaceName)) {
            throw new NotExecutableException("Cannot test copy between workspaces. 'workspaceName' points to default workspace as well.");
        }
        return workspaceName;
    }

    protected void initNodesW2() throws RepositoryException {

        // testroot
        if (superuserW2.getRootNode().hasNode(testPath)) {
            testRootNodeW2 = superuserW2.getRootNode().getNode(testPath);
            // clean test root
            for (NodeIterator it = testRootNodeW2.getNodes(); it.hasNext(); ) {
                it.nextNode().remove();
            }
            testRootNodeW2.save();
        } else {
            testRootNodeW2 = superuserW2.getRootNode().addNode(testPath, testNodeType);
            superuserW2.save();
        }
    }

    /**
     * Test cloning a hierarchy of nodes containing a set of shareable nodes.
     * See also JCR-2473
     *
     * @throws Exception if the test fails
     */
    public void testClone() throws Exception {
        Session s1 = testRootNode.getSession();
        Node a = testRootNode.addNode("a");
        Node b = a.addNode("b");
        Node c = a.addNode("c");
        Node d = b.addNode("d");
        ensureMixinType(d, mixShareable);
        s1.save();

        String path = c.getPath() + "/d";
        workspace.clone(workspace.getName(), d.getPath(), path, false);

        // we now have this hierarchy of nodes:
        //   a
        //  / \
        //  b c
        //  \ /
        //   d

        // now clone 'a' to another workspace
        workspaceW2.clone(workspace.getName(), a.getPath(), a.getPath(), false);
    }
}
