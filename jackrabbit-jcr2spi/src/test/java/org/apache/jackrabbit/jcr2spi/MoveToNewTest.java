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
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.util.Text;

/** <code>MoveToNewTest</code>... */
public class MoveToNewTest extends AbstractJCRTest {

    protected Node srcParentNode;
    protected Node destParentNode;
    protected Node moveNode;
    protected String destinationPath;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // create parent node
        srcParentNode = testRootNode.addNode(nodeName1, testNodeType);
        // create node to be moved
        moveNode = srcParentNode.addNode(nodeName2, testNodeType);
        // save the new nodes
        testRootNode.save();

        // create a NEW node that will serve as destination parent
        destParentNode = testRootNode.addNode(nodeName3, testNodeType);
        destinationPath = destParentNode.getPath() + "/" + nodeName2;
    }

    @Override
    protected void tearDown() throws Exception {
        srcParentNode = null;
        destParentNode = null;
        moveNode = null;
        super.tearDown();
    }

    public void testMove() throws RepositoryException {
        String srcPath = moveNode.getPath();
        testRootNode.getSession().move(srcPath, destinationPath);

        assertTrue(destParentNode.isNew());
        assertTrue(moveNode.isModified());

        assertTrue(testRootNode.getSession().itemExists(destinationPath));
        assertFalse(testRootNode.getSession().itemExists(srcPath));
    }

    public void testMoveSaved() throws RepositoryException {
        String srcPath = moveNode.getPath();
        testRootNode.getSession().move(srcPath, destinationPath);
        testRootNode.save();

        assertFalse(destParentNode.isNew());
        assertFalse(srcParentNode.isModified());
        assertFalse(moveNode.isModified());

        assertTrue(testRootNode.getSession().itemExists(destinationPath));
        assertFalse(testRootNode.getSession().itemExists(srcPath));
    }

    public void testRevertMovedNode() throws RepositoryException {
        String srcPath = moveNode.getPath();
        testRootNode.getSession().move(srcPath, destinationPath);

        try {
            destParentNode.refresh(false);
            fail("Incomplete 'changelog'");
        } catch (RepositoryException e) {
            // ok
        }
    }

    public void testRemoveDestParent() throws RepositoryException {
        String srcPath = moveNode.getPath();
        testRootNode.getSession().move(srcPath, destinationPath);
        destParentNode.remove();

        assertFalse(destParentNode.isNew());
        assertFalse(destParentNode.isModified());

        assertFalse(moveNode.isModified());
        assertTrue(srcParentNode.isModified());
        assertFalse(testRootNode.getSession().itemExists(srcPath));
    }

    public void testRevertRemoveDestParent() throws RepositoryException {
        String srcPath = moveNode.getPath();
        testRootNode.getSession().move(srcPath, destinationPath);
        destParentNode.remove();
        testRootNode.refresh(false);

        assertFalse(destParentNode.isModified());
        assertFalse(destParentNode.isNew());

        try {
            destParentNode.hasNode(nodeName2);
            fail("The new destParent must have been removed.");
        } catch (InvalidItemStateException e) {
            // success
        }
        assertTrue(srcParentNode.hasNode(nodeName2));
        assertFalse(srcParentNode.isModified());

        assertFalse(testRootNode.getSession().itemExists(destinationPath));
        assertTrue(testRootNode.getSession().itemExists(srcPath));
    }

    public void testMoveTwice() throws RepositoryException {
        Session s = testRootNode.getSession();

        String srcPath = moveNode.getPath();
        s.move(srcPath, destinationPath);

        srcParentNode.remove();

        // create new parent
        Node newParent = testRootNode.addNode(nodeName1);
        s.move(destinationPath, srcPath);

        assertTrue(newParent.isNew());
        assertTrue(newParent.hasNode(nodeName2));

        assertTrue(destParentNode.isNew());
        assertFalse(destParentNode.hasNode(nodeName2));

        // remove the tmp destination parent node.
        destParentNode.remove();

        assertTrue(newParent.isNew());
        assertTrue(newParent.hasNode(nodeName2));
        assertTrue(moveNode.isModified());

        testRootNode.save();

        assertFalse(s.itemExists(Text.getRelativeParent(destinationPath, 1)));
        assertTrue(s.itemExists(srcPath));

        assertFalse(moveNode.isModified() || newParent.isNew() || srcParentNode.isModified());
        try {
            srcParentNode.getNode(nodeName2);
            fail("src parent must be removed");
        } catch (InvalidItemStateException e) {
            // ok.
        }
        assertTrue(moveNode.isSame(newParent.getNode(nodeName2)));
    }

    public void testMoveTwiceWithSecondSession() throws RepositoryException {
        Session s = testRootNode.getSession();
        String srcPath = moveNode.getPath();

        // move away the 'moveNode'
        s.move(srcPath, destinationPath);
        // rm the original parent
        srcParentNode.remove();
        // create new parent and move the 'moveNode' back
        Node newParent = testRootNode.addNode(nodeName1);
        newParent.setProperty(propertyName1, "marker");
        s.move(destinationPath, srcPath);
        // remove the tmp. destination parent
        destParentNode.remove();

        testRootNode.save();

        Session readOnly = getHelper().getReadOnlySession();
        try {
            Node trn = (Node) readOnly.getItem(testRootNode.getPath());
            NodeIterator it = trn.getNodes(nodeName1);

            String msg = "testRootNode must have a single child node with name " + nodeName1;
            if (it.hasNext()) {
                Node parent = it.nextNode();
                assertTrue(parent.hasProperty(propertyName1));
                assertEquals("The 'newParent' must have the marker property","marker", parent.getProperty(propertyName1).getString());
                assertTrue("moveNode must be present below the 'newParent'.", parent.hasNode(Text.getName(srcPath)));
                assertFalse(msg, it.hasNext());
            } else {
                fail(msg);
            }
        } finally {
            readOnly.logout();
        }
    }
}