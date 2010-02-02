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
package org.apache.jackrabbit.jcr2spi;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>RefreshMovedTest</code>...
 */
public class RefreshMovedTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(RefreshMovedTest.class);

    protected Node moveNode;
    protected String srcPath;
    protected String destinationPath;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // create parent node
        Node srcParentNode = testRootNode.addNode(nodeName1, testNodeType);
        // create node to be moved
        moveNode = srcParentNode.addNode(nodeName2, testNodeType);
        // create a node that will serve as new parent
        Node destParentNode = testRootNode.addNode(nodeName3, testNodeType);
        // save the new nodes
        testRootNode.save();

        srcPath = moveNode.getPath();
        destinationPath = destParentNode.getPath() + "/" + nodeName2;
    }

    @Override
    protected void tearDown() throws Exception {
        moveNode = null;
        super.tearDown();
    }

    /**
     * Test if refresh(true) does not affect a moved node.
     *
     * @throws RepositoryException
     */
    public void testRefreshTrue() throws RepositoryException {
        testRootNode.getSession().move(srcPath, destinationPath);
        testRootNode.getSession().refresh(true);

        assertTrue("Refresh with pending move operation must not remove the node at destination path.", testRootNode.getSession().itemExists(destinationPath));
        assertFalse("Refresh with pending move operation must not re-add the node at its original position.", testRootNode.getSession().itemExists(srcPath));
        assertFalse("Refresh with pending move operation must not re-add the node at its original position.", srcPath.equals(moveNode.getPath()));
    }

    /**
     * Test if refresh(false) affecting a node that has been moved by another
     * session invalidates the node properly in termes of either moving it to
     * the new destination or marking it 'removed'.
     *
     * @throws RepositoryException
     */
    public void testRefreshOtherSession() throws RepositoryException {
        Session readSession = getHelper().getReadOnlySession();
        try {
            Node anotherNode = (Node) readSession.getItem(srcPath);
            // workspace move
            testRootNode.getSession().getWorkspace().move(srcPath, destinationPath);

            readSession.refresh(false);
            try {
                String p = anotherNode.getPath();
                // unless InvalidItemStateException is thrown the node must have
                // been 'moved' to its new position.
                assertTrue("Upon refresh of a node moved by another session it must be moved to the new destination (or removed).", p.equals(destinationPath));
            } catch (InvalidItemStateException e) {
                // ok as well.
            }
        } finally {
            readSession.logout();
        }
    }
}