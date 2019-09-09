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

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>MoveSNSTest</code> (Implementation specific tests. JSR170 only
 * expects orderable same-name-siblings to have a consistent and testable
 * order.)
 */
public class MoveSNSTest extends AbstractMoveTest {

    private static Logger log = LoggerFactory.getLogger(MoveSNSTest.class);

    private Node sourceSibling;
    private Node destSibling;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (destParentNode.hasNode(nodeName2)) {
            fail("Setup: Move destination already contains a child node with name " + nodeName2);
        }

        if (!moveNode.getDefinition().allowsSameNameSiblings()) {
            fail("Setup: Unable to create SNS-node for MoveSNSTest.");
        }
        sourceSibling = srcParentNode.addNode(nodeName2, testNodeType);
        destSibling = destParentNode.addNode(nodeName2, testNodeType);

        if (!destSibling.getDefinition().allowsSameNameSiblings()) {
           fail("Setup: Unable to create SNS-node at move destination.");
        }
        testRootNode.save();
    }

    @Override
    protected void tearDown() throws Exception {
        sourceSibling = null;
        destSibling = null;
        super.tearDown();
    }

    @Override
    protected boolean isSessionMove() {
        return true;
    }

    /**
     * Implementation specific:
     * Test if the path of a moved node, contains the index of the last sibling.
     */
    public void testMovedNodeGetPath() throws RepositoryException, NotExecutableException {
        int index = destSibling.getIndex() + 1;
        //move the node
        doMove(moveNode.getPath(),destinationPath);
        assertEquals("After successful move the moved node must return the destination path.", destinationPath + "["+ index +"]", moveNode.getPath());
    }

    /**
     * Implementation specific:
     * Same as {@link #testMovedNodeGetPath()}, but calls save prior to the
     * test.
     */
    public void testMovedNodeGetPath2() throws RepositoryException, NotExecutableException {
        int index = destSibling.getIndex() + 1;
        //move the node
        doMove(moveNode.getPath(), destParentNode.getPath() + "/" + nodeName2);
        superuser.save();
        assertEquals("After successful move the moved node must return the destination path.", destinationPath + "["+ index +"]", moveNode.getPath());
    }


    /**
     * Test if a moved node is 'replaced' by its SNS.
     */
    public void testAccessMovedNodeByOldPath() throws RepositoryException, NotExecutableException {
        String oldPath = moveNode.getPath();
        //move the node
        doMove(oldPath, destinationPath);
        try {
            Item item = superuser.getItem(oldPath);
            // Implementation specific:
            assertTrue("A moved SNS node must be 'replaced' but is successor sibling.", item.isSame(sourceSibling));
        } catch (PathNotFoundException e) {
            fail("A moved SNS node must be 'replaced' but is successor sibling.");
        }
    }

    /**
     * Same as {@link #testAccessMovedNodeByOldPath()} but calls save() prior to
     * the test.
     */
    public void testAccessMovedNodeByOldPath2() throws RepositoryException, NotExecutableException {
        String oldPath = moveNode.getPath();
        //move the node
        doMove(oldPath, destinationPath);
        superuser.save();
        try {
            Item item = superuser.getItem(oldPath);
            // Implementation specific:
            assertTrue("A moved SNS node must be 'replaced' but is successor sibling.", item.isSame(sourceSibling));
        } catch (PathNotFoundException e) {
            fail("A moved SNS node must be 'replaced' but is successor sibling.");
        }
    }

    /**
     * Implementation specific:
     * Test if the moved node is appended to the list of SNSs at the destination.
     *
     * @throws RepositoryException
     * @throws NotExecutableException
     */
    public void testMovedNodeIsSame() throws RepositoryException, NotExecutableException {
        //move the node
        doMove(moveNode.getPath(), destinationPath);

        int cnt = 0;
        for (NodeIterator it = destParentNode.getNodes(nodeName2); it.hasNext();) {
            Node n = it.nextNode();
            if (cnt == 0) {
                assertTrue("Moved node must be appended to list of SNSs.", destSibling.isSame(n));
            } else {
                assertTrue("Moved node must be appended to list of SNSs.", moveNode.isSame(n));
            }
            cnt++;
        }
    }

    /**
     * Implementation specific:
     * Same as {@link #testMovedNodeIsSame()}, but calls save() before executing
     * the comparison.
     *
     * @throws RepositoryException
     * @throws NotExecutableException
     */
    public void testMovedNodeIsSame2() throws RepositoryException, NotExecutableException {
        //move the node
        doMove(moveNode.getPath(), destinationPath);
        superuser.save();

        int cnt = 0;
        for (NodeIterator it = destParentNode.getNodes(nodeName2); it.hasNext();) {
            Node n = it.nextNode();
            if (cnt == 0) {
                assertTrue("Moved node must be appended to list of SNSs.", destSibling.isSame(n));
            } else {
                assertTrue("Moved node must be appended to list of SNSs.", moveNode.isSame(n));
            }
            cnt++;
        }
    }
}