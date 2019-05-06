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

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>ReorderMoveTest</code> testing various combinations of move/rename
 * and reorder with and without intermediate save, revert and other transient
 * modifications.
 */
public class ReorderMoveTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(ReorderMoveTest.class);

    private Node destParent;
    private Node srcParent;
    private String destPath;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (!testRootNode.getPrimaryNodeType().hasOrderableChildNodes()) {
            throw new NotExecutableException("Test node does not have orderable children.");
        }

        // create move-destination
        destParent = testRootNode.addNode(nodeName4, testNodeType);
        srcParent = testRootNode.addNode(nodeName2, testNodeType);

        destPath = destParent.getPath() + "/" + nodeName3;
        testRootNode.save();
    }

    @Override
    protected void tearDown() throws Exception {
        destParent = null;
        srcParent = null;
        super.tearDown();
    }

   private Node[] createOrderableChildren(boolean sns) throws RepositoryException {
        String[] childNames;
        if (sns) {
            childNames = new String[] {nodeName2, nodeName2, nodeName2, nodeName2};
        } else {
            childNames = new String[] {nodeName1, nodeName2, nodeName3, nodeName4};
        }
        Node[] children = new Node[4];
        children[0] = srcParent.addNode(childNames[0], testNodeType);
        children[1] = srcParent.addNode(childNames[1], testNodeType);
        children[2] = srcParent.addNode(childNames[2], testNodeType);
        children[3] = srcParent.addNode(childNames[3], testNodeType);

        testRootNode.save();
        return children;
   }

    private static String getRelPath(Node child) throws RepositoryException {
        if (child == null) {
            return null;
        }
        String path = child.getPath();
        return path.substring(path.lastIndexOf('/')+1);
    }

    private static void testOrder(Node parent, Node[] children) throws RepositoryException {
        NodeIterator it = parent.getNodes();
        int i = 0;
        while (it.hasNext()) {
            Node child = it.nextNode();
            assertTrue(child.isSame(children[i]));
            i++;
        }
    }

    /**
     * Move a orderable child node and reorder the remaining nodes.
     */
    public void testMoveAndReorder() throws RepositoryException {
        Node[] children = createOrderableChildren(false);
        String oldName = children[2].getName();
        // move
        testRootNode.getSession().move(children[2].getPath(), destPath);
        // reorder
        srcParent.orderBefore(getRelPath(children[1]), null);
        testOrder(srcParent, new Node[] {children[0], children[3], children[1]});

        testRootNode.save();
        testOrder(srcParent, new Node[] {children[0], children[3], children[1]});
        assertFalse(srcParent.hasNode(oldName));
    }

    /**
     * Move a orderable SNS-node and reorder the remaining nodes at source-parent.
     */
    public void testMoveAndReorderSNS() throws RepositoryException {
        Node[] children = createOrderableChildren(true);
        String snsName = children[0].getName();

        // move
        testRootNode.getSession().move(children[2].getPath(), destPath);
        testRootNode.getSession().move(children[1].getPath(), destPath);

        // reorder
        srcParent.orderBefore(getRelPath(children[0]), null);
        testOrder(srcParent, new Node[] {children[3], children[0]});
        assertTrue(srcParent.hasNode(snsName+"[1]"));
        assertTrue(srcParent.hasNode(snsName+"[2]"));
        assertFalse(srcParent.hasNode(snsName+"[3]"));
        assertFalse(srcParent.hasNode(snsName+"[4]"));
        assertFalse(srcParent.hasNode(snsName+"[5]"));

        testRootNode.save();
        testOrder(srcParent, new Node[] {children[3], children[0]});
        assertTrue(srcParent.hasNode(snsName+"[1]"));
        assertTrue(srcParent.hasNode(snsName+"[2]"));
        assertFalse(srcParent.hasNode(snsName+"[3]"));
        assertFalse(srcParent.hasNode(snsName+"[4]"));
        assertFalse(srcParent.hasNode(snsName+"[5]"));

        // check if move have been successful
        assertEquals(children[2].getPath(), destPath);
        assertTrue(children[2].getIndex() == Path.INDEX_DEFAULT);
        assertEquals(children[1].getPath(), destPath+"[2]");
    }

    /**
     * Reorder nodes and move one of the reordered siblings
     * away. Test the ordering of the remaining siblings.
     */
    public void testReorderAndMove() throws RepositoryException {
        Node[] children = createOrderableChildren(false);

        // reorder first
        srcParent.orderBefore(getRelPath(children[0]), null);
        srcParent.orderBefore(getRelPath(children[3]), getRelPath(children[1]));
        // move
        testRootNode.getSession().move(children[3].getPath(), destPath);

        testOrder(srcParent, new Node[] {children[1], children[2], children[0]});

        testRootNode.save();
        testOrder(srcParent, new Node[] {children[1], children[2], children[0]});
    }

    /**
     * Reorder same-name-sibling nodes and move one of the reordered siblings
     * away. Test the ordering of the remaining siblings.
     */
    public void testReorderAndMoveSNS() throws RepositoryException {
        Node[] children = createOrderableChildren(true);

        // reorder first
        srcParent.orderBefore(getRelPath(children[0]), null);
        srcParent.orderBefore(getRelPath(children[3]), getRelPath(children[1]));
        // move
        testRootNode.getSession().move(children[3].getPath(), destPath);

        testOrder(srcParent, new Node[] {children[1], children[2], children[0]});

        testRootNode.save();
        testOrder(srcParent, new Node[] {children[1], children[2], children[0]});
    }

    /**
     * Any attempt reorder a moved node at its original position must fail.
     */
    public void testReorderMovedNode() throws RepositoryException {
        Node[] children = createOrderableChildren(false);

        String relPath = getRelPath(children[2]);
        testRootNode.getSession().move(children[2].getPath(), destPath);

        try {
            srcParent.orderBefore(relPath, null);
            fail("Reordering a child node that has been moved away must fail.");
        } catch (ItemNotFoundException e) {
            // ok
        }
    }

    /**
     * Move a SNS-node and reorder its original siblings afterwards.
     * Test if reverting the changes results in the original ordering and
     * hierarchy.
     */
    public void testRevertMoveAndReorderSNS() throws RepositoryException {
        Node[] children = createOrderableChildren(true);
        // move then reorder
        testRootNode.getSession().move(children[2].getPath(), destPath);
        srcParent.orderBefore(getRelPath(children[1]), null);
        srcParent.orderBefore(getRelPath(children[3]), getRelPath(children[0]));

        testRootNode.refresh(false);
        testOrder(srcParent, new Node[] {children[0], children[1], children[2], children[3]});
        assertFalse(destParent.hasNode(Text.getName(destPath)));
    }

    /**
     * Move a SNS-node, that got its siblings reordered before.
     * Test if reverting the changes results in the original ordering and
     * hierarchy.
     */
    public void testRevertReorderAndMoveSNS() throws RepositoryException {
        Node[] children = createOrderableChildren(true);
        // reorder then move
        srcParent.orderBefore(getRelPath(children[1]), null);
        srcParent.orderBefore(getRelPath(children[3]), getRelPath(children[2]));
        srcParent.getSession().move(children[2].getPath(), destPath);

        testRootNode.refresh(false);
        testOrder(srcParent, new Node[] {children[0], children[1], children[2], children[3]});
        assertFalse(destParent.hasNode(Text.getName(destPath)));
    }

    /**
     * Move a SNS-node, that has been reordered before.
     * Test if reverting the changes results in the original ordering and
     * hierarchy.
     */
    public void testRevertMoveReorderedSNS() throws RepositoryException {
        Node[] children = createOrderableChildren(true);
        // reorder then move
        srcParent.orderBefore(getRelPath(children[1]), null);
        srcParent.orderBefore(getRelPath(children[3]), getRelPath(children[2]));
        srcParent.getSession().move(children[1].getPath(), destPath);

        testRootNode.refresh(false);
        testOrder(srcParent, new Node[] {children[0], children[1], children[2], children[3]});
        assertFalse(destParent.hasNode(Text.getName(destPath)));
    }
}
