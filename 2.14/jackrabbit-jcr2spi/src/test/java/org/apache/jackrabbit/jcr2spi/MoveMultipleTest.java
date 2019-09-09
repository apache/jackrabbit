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
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>MoveMultipleTest</code>...
 */
public class MoveMultipleTest extends AbstractMoveTest {

    private static Logger log = LoggerFactory.getLogger(MoveMultipleTest.class);

    private String originalPath;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        originalPath = moveNode.getPath();
    }

    @Override
    protected boolean isSessionMove() {
        return true;
    }

    /**
     * Transiently move a persisted node multiple times and check results
     * after each move as well as after saving.
     */
    public void testMultipleMove() throws RepositoryException {
        // 1. move
        doMove(moveNode.getPath(), destinationPath);

        // 2. move
        String destPath2 = srcParentNode.getPath()+"/"+nodeName1;
        doMove(destinationPath, destPath2);
        assertTrue(moveNode.getParent().isSame(srcParentNode));
        assertEquals(moveNode.getName(), Text.getName(destPath2));
        assertEquals(moveNode.getPath(), destPath2);
        assertFalse(destParentNode.hasNode(Text.getName(destinationPath)));

        // 3. move
        String destPath3 = destParentNode.getPath()+"/"+nodeName4;
        doMove(destPath2, destPath3);
        assertTrue(moveNode.getParent().isSame(destParentNode));
        assertEquals(moveNode.getName(), Text.getName(destPath3));
        assertEquals(moveNode.getPath(), destPath3);
        assertFalse(srcParentNode.hasNode(Text.getName(destPath2)));

        testRootNode.save();

        assertTrue(moveNode.getParent().isSame(destParentNode));
        assertEquals(moveNode.getName(), Text.getName(destPath3));
        assertEquals(moveNode.getPath(), destPath3);
        assertFalse(srcParentNode.hasNode(Text.getName(destPath2)));
    }

    /**
     * Test revert of persisted node after multiple transient moves
     */
    public void testRevertingMultipleMove() throws RepositoryException {
        doMove(moveNode.getPath(), destinationPath);
        String destPath2 = srcParentNode.getPath()+"/"+nodeName1;
        doMove(destinationPath, destPath2);
        String destPath3 = destParentNode.getPath()+"/"+nodeName4;
        doMove(destPath2, destPath3);

        superuser.refresh(false);

        assertEquals(moveNode.getPath(), originalPath);
        assertTrue(srcParentNode.hasNode(Text.getName(originalPath)));
        assertFalse(srcParentNode.hasNode(Text.getName(destPath2)));
        assertFalse(destParentNode.hasNodes());
    }

    /**
     * Move a new node multiple times and check the hierarchy after saving.
     */
    public void testMultipleMoveNewNode() throws RepositoryException {
        // add additional nodes
        Node moveNode2 = moveNode.addNode(nodeName3, testNodeType);

        doMove(moveNode2.getPath(), destinationPath);
        String destPath2 = destParentNode.getPath()+"/"+nodeName4;
        doMove(moveNode2.getPath(), destPath2);
        String destPath3 = srcParentNode.getPath()+"/"+nodeName4;
        doMove(moveNode2.getPath(), destPath3);
        doMove(moveNode2.getPath(), destinationPath);

        testRootNode.save();

        assertTrue(moveNode2.getParent().isSame(destParentNode));
        assertEquals(moveNode2.getName(), Text.getName(destinationPath));
        assertEquals(moveNode2.getPath(), destinationPath);
        assertFalse(moveNode2.hasNodes());

        superuser.save();
    }

    /**
     * Move destination after moving the target node.
     */
    public void testMoveDestination() throws RepositoryException {
        doMove(moveNode.getPath(), destinationPath);
        doMove(destParentNode.getPath(), srcParentNode.getPath() + "/" + destParentNode.getName());

        superuser.save();
        assertTrue(destParentNode.getParent().isSame(srcParentNode));
        assertTrue(moveNode.getParent().isSame(destParentNode));
    }

    /**
     * Separately move the persisted 'moveNode' and its transiently added
     * child node.
     */
    public void testMoveParentAndChild() throws RepositoryException {
        // add additional nodes
        Node moveNode2 = moveNode.addNode(nodeName3, testNodeType);
        Property childProperty = moveNode2.setProperty(propertyName2, "anyString");
        Node childNode = moveNode2.addNode(nodeName4, testNodeType);

        doMove(moveNode.getPath(), destinationPath);
        doMove(moveNode2.getPath(), srcParentNode.getPath() + "/" + moveNode2.getName());

        assertFalse(moveNode.hasNode(moveNode2.getName()));
        assertFalse(moveNode.hasNodes());
        assertTrue(srcParentNode.getNode(moveNode2.getName()).isSame(moveNode2));

        doMove(moveNode.getPath(), originalPath);

        assertEquals(moveNode.getPath(), originalPath);
        assertFalse(destParentNode.hasNode(Text.getName(destinationPath)));

        assertFalse(moveNode.hasNode(moveNode2.getName()));
        assertFalse(moveNode.hasNodes());
        assertTrue(srcParentNode.getNode(moveNode2.getName()).isSame(moveNode2));

        superuser.save();

        assertFalse(moveNode.hasNodes());
        assertTrue(moveNode2.hasNode(childNode.getName()));
        assertTrue(moveNode2.hasProperty(childProperty.getName()));

        assertTrue(srcParentNode.getNode(moveNode.getName()).isSame(moveNode));
        assertTrue(srcParentNode.getNode(moveNode2.getName()).isSame(moveNode2));
    }

    /**
     * Move a node that has a child node and finally revert the 'move' operations.
     */
    public void testRevertingMoveParentAndChild() throws RepositoryException {
        Node moveNode2 = moveNode.addNode(nodeName3, testNodeType);
        // moveNode2 must be persisted in order not to have it removed upon
        // refresh(false).
        moveNode.save();

        doMove(moveNode.getPath(), destinationPath);
        doMove(moveNode2.getPath(), srcParentNode.getPath() + "/" + moveNode2.getName());
        doMove(moveNode.getPath(), originalPath);

        testRootNode.refresh(false);

        // now all 3 move ops must be reverted
        assertTrue(moveNode2.getParent().isSame(moveNode));
        assertTrue(moveNode.getParent().isSame(srcParentNode));
        assertFalse(destParentNode.hasNodes());
        assertFalse(srcParentNode.hasNode(moveNode2.getName()));
    }

    /**
     * Separately move the new 'moveNode' and its child node. Save 'add' and
     * 'move' ops in one step.
     */
    public void testMoveNewParentAndNewChild() throws RepositoryException {
        Node moveNode2 = moveNode.addNode("moveNode2", testNodeType);
        Property childProperty = moveNode2.setProperty(propertyName2, "anyString");
        Node childNode = moveNode2.addNode("childNode", testNodeType);

        doMove(moveNode2.getPath(), destinationPath);
        doMove(childNode.getPath(), srcParentNode.getPath() + "/" + childNode.getName());
        doMove(moveNode2.getPath(), srcParentNode.getPath() + "/" + nodeName4);

        superuser.save();

        assertTrue(moveNode2.getName().equals(nodeName4));
        assertFalse(moveNode2.hasNodes());
        assertTrue(moveNode2.hasProperty(childProperty.getName()));

        assertTrue(moveNode2.getParent().isSame(srcParentNode));
        assertTrue(childNode.getParent().isSame(srcParentNode));
    }

    /**
     * Separately move the persisted 'moveNode' and its 'new' child node.
     * Check if reverting the changes removes the 'new' child and moves
     * the persisted moveNode back.
     */
    public void testRevertingMoveParentAndNewChild() throws RepositoryException {
        Node moveNode2 = moveNode.addNode(nodeName3, testNodeType);

        doMove(moveNode.getPath(), destinationPath);
        doMove(moveNode2.getPath(), srcParentNode.getPath() + "/" + moveNode2.getName());
        doMove(moveNode.getPath(), originalPath);

        testRootNode.refresh(false);

        // moveNode2 which has never been saved, must be removed
        try {
            moveNode2.getParent();
            fail("Reverting the move of a 'new' node must remove the new node as well.");
        } catch (InvalidItemStateException e) {
            // ok
        }
        // the persistent 'moveNode' must be moved back to its original position.
        assertTrue(moveNode.getParent().isSame(srcParentNode));
        assertFalse(destParentNode.hasNodes());
    }

    /**
     * Move a node with child items without having loaded the children before.
     * Test if children can be accessed afterwards.
     */
    public void testAccessChildrenAfterMove() throws RepositoryException {
        Property childProperty = moveNode.setProperty(propertyName2, "anyString");
        Node childNode = moveNode.addNode(nodeName2, testNodeType);
        testRootNode.save();

        Session otherSession = getHelper().getReadWriteSession();
        try {
            otherSession.move(originalPath, destinationPath);
            Node mv = (Node) otherSession.getItem(destinationPath);

            testRootNode.refresh(false);
            assertTrue(childNode.isSame(mv.getNode(nodeName2)));
            assertTrue(childProperty.isSame(mv.getProperty(propertyName2)));
        } finally {
            otherSession.logout();
        }
    }
}