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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Item;

/**
 * <code>MoveTest</code>...
 */
public class MoveTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(MoveTest.class);

    protected Node srcParentNode;
    protected Node destParentNode;
    protected Node moveNode;

    protected String destinationPath;

    protected void setUp() throws Exception {
        super.setUp();

        // create parent node
        srcParentNode = testRootNode.addNode(nodeName1, testNodeType);
        // create node to be moved
        moveNode = srcParentNode.addNode(nodeName2, testNodeType);
        // create a node that will serve as new parent
        destParentNode = testRootNode.addNode(nodeName3, testNodeType);
        // save the new nodes
        testRootNode.save();

        destinationPath = destParentNode.getPath() + "/" + nodeName2;
    }

    public void testMoveRoot() throws RepositoryException {
        Node root = superuser.getRootNode();
        try {
            superuser.move(root.getPath(), destinationPath);
            fail("Moving the root node must fail with RepositoryException.");
        } catch (RepositoryException e) {
            // OK
        }
    }

    public void testMoveBelowDescendant() throws RepositoryException {
        try {
            superuser.move(srcParentNode.getPath(), moveNode.getPath() + "/" + nodeName2);
            fail("Moving the ancestor node below descendant must fail with RepositoryException.");
        } catch (RepositoryException e) {
            // OK
        }
    }

    public void testMoveDestinationWithIndex() throws RepositoryException {
        try {
            superuser.move(moveNode.getPath(), destinationPath + "[1]");
            fail("Moving to destination with index must fail with RepositoryException.");
        } catch (RepositoryException e) {
            // OK
        }
    }

    /**
     * Test if a moved node returns the specified destination path and by the
     * way test, if the moved node is still valid.
     */
    public void testMovedNodeGetPath() throws RepositoryException, NotExecutableException {
        String oldPath = moveNode.getPath();

        if (destParentNode.hasNode(nodeName2)) {
            throw new NotExecutableException("Move destination already contains a child node with name " + nodeName2);
        }
        //move the node
        superuser.move(oldPath,destinationPath);
        assertEquals("After successful move the moved node must return the destination path.", destinationPath, moveNode.getPath());
    }

    /**
     * Same as {@link #testMovedNodeGetPath()}, but calls save prior to the
     * test.
     */
    public void testMovedNodeGetPath2() throws RepositoryException, NotExecutableException {
        String oldPath = moveNode.getPath();

        if (destParentNode.hasNode(nodeName2)) {
            throw new NotExecutableException("Move destination already contains a child node with name " + nodeName2);
        }
        //move the node
        superuser.move(oldPath, destParentNode.getPath() + "/" + nodeName2);
        superuser.save();
        assertEquals("After successful move the moved node must return the destination path.", destinationPath, moveNode.getPath());
    }

    /**
     * Test if a moved node is not accessible by its old path any more
     */
    public void testAccessMovedNodeByOldPath() throws RepositoryException, NotExecutableException {
        NodeIterator it = srcParentNode.getNodes(moveNode.getName());
        int cnt = 0;
        while (it.hasNext()) {
            it.nextNode();
            cnt++;
        }
        if (cnt > 1) {
            throw new NotExecutableException("Move source parent has multiple child nodes with name " + moveNode.getName());
        }

        String oldPath = moveNode.getPath();

        //move the node
        superuser.move(oldPath, destinationPath);
        try {
            superuser.getItem(oldPath);
            fail("A moved node must not be accessible by its old path any more.");
        } catch (PathNotFoundException e) {
            // ok.
        }
    }

    /**
     * Same as {@link #testAccessMovedNodeByOldPath()} but calls save() prior to
     * the test.
     */
    public void testAccessMovedNodeByOldPath2() throws RepositoryException, NotExecutableException {
        NodeIterator it = srcParentNode.getNodes(moveNode.getName());
        int cnt = 0;
        while (it.hasNext()) {
            it.nextNode();
            cnt++;
        }
        if (cnt > 1) {
           throw new NotExecutableException("Move source parent has multiple child nodes with name " + moveNode.getName());
        }

        String oldPath = moveNode.getPath();

        //move the node
        superuser.move(oldPath, destinationPath);
        superuser.save();
        try {
            superuser.getItem(oldPath);
            fail("A moved node must not be accessible by its old path any more.");
        } catch (PathNotFoundException e) {
            // ok.
        }
    }

    /**
     * Test if the accessing the moved node from the session returns the same
     * Node object, than the Node which was moved before.
     *
     * @throws RepositoryException
     * @throws NotExecutableException
     */
    public void testMovedNodeIsSame() throws RepositoryException, NotExecutableException {
        if (destParentNode.hasNode(nodeName2)) {
            throw new NotExecutableException(destParentNode + " already has child node " + ". Test cannot be preformed if SNS is present.");
        }

        String oldPath = moveNode.getPath();
        String newPath = destParentNode.getPath() + "/" + nodeName2;

        //move the node
        superuser.move(oldPath, destinationPath);
        Item movedItem = superuser.getItem(newPath);
        assertTrue("Moved Node must be the same after the move.", movedItem.isSame(moveNode));
        // NOTE: implementation specific test
        assertTrue("After successful moving a referenceable node node, accessing the node by uuid be the identical node.", movedItem == moveNode);
    }

    /**
     * Same as {@link #testMovedNodeIsSame()}, but calls save() before executing
     * the comparison.
     *
     * @throws RepositoryException
     * @throws NotExecutableException
     */
    public void testMovedNodeIsSame2() throws RepositoryException, NotExecutableException {
        if (destParentNode.hasNode(nodeName2)) {
            throw new NotExecutableException(destParentNode + " already has child node " + ". Test cannot be preformed if SNS is present.");
        }

        String oldPath = moveNode.getPath();

        //move the node
        superuser.move(oldPath, destinationPath);
        superuser.save();

        Item movedItem = superuser.getItem(destinationPath);
        assertTrue("Moved Node must be the same after the move.", movedItem.isSame(moveNode));
        // NOTE: implementation specific test
        assertTrue("After successful moving a referenceable node node, accessing the node by uuid be the identical node.", movedItem == moveNode);
    }

    /**
     * Test if after the move, <code>Node.getParent()</code> returns the
     * destination parent.
     *
     * @throws RepositoryException
     */
    public void testMovedNodeParent() throws RepositoryException {
        //move the node
        superuser.move(moveNode.getPath(), destinationPath);
        assertTrue("Parent of moved node must be the destination parent node.", moveNode.getParent().isSame(destParentNode));
        // NOTE: implementation specific test
        assertTrue("After successful moving a referenceable node node, accessing the node by uuid be the identical node.", moveNode.getParent() == destParentNode);
    }

    /**
     * Same as {@link #testMovedNodeParent()}, but calls save before executing
     * the comparison.
     *
     * @throws RepositoryException
     */
    public void testMovedNodeParent2() throws RepositoryException {
        //move the node
        superuser.move(moveNode.getPath(), destinationPath);
        superuser.save();

        assertTrue("Parent of moved node must be the destination parent node.", moveNode.getParent().isSame(destParentNode));
        // NOTE: implementation specific test
        assertTrue("After successful moving a referenceable node node, accessing the node by uuid be the identical node.", moveNode.getParent() == destParentNode);
    }
}