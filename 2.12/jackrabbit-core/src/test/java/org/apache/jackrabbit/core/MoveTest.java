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
import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * Tests moving, refreshing, and saving nodes.
 */
public class MoveTest extends AbstractJCRTest {

    /**
     * Test case for
     * <a href="https://issues.apache.org/jira/browse/JCR-2720">JCR-2720</a>
     */
    public void testMoveVisibilityAcrossSessions() throws RepositoryException {
        Session session1 = getHelper().getReadWriteSession();
        Session session2 = getHelper().getReadWriteSession();

        if (session1.itemExists("/foo")) {
            session1.removeItem("/foo");
            session1.save();
        }

        session1.getRootNode().addNode("libs").addNode("foo").addNode("install");
        session1.save();

        assertTrue(session1.itemExists("/libs/foo/install"));
        assertFalse(session1.itemExists("/foo"));

        assertTrue(session2.itemExists("/libs/foo/install"));
        assertFalse(session2.itemExists("/foo"));

        session1.move("/libs", "/foo");
        session1.save();

        assertFalse(session1.itemExists("/libs/foo/install"));

        session2.refresh(false);

        assertFalse("JCR-2720", session2.itemExists("/libs/foo/install"));
    }

    /**
     * Tests moving a node, and then refreshing or saving it.
     */
    public void testMove() throws RepositoryException {
        doTestMove(true);
        doTestMove(false);
    }

    private void doTestMove(boolean save) throws RepositoryException {
        Session session = testRootNode.getSession();
        for (NodeIterator it = testRootNode.getNodes(); it.hasNext();) {
            it.nextNode().remove();
            session.save();
        }
        Node node1 = testRootNode.addNode(nodeName1);
        Node node2 = node1.addNode(nodeName2);
        session.save();
        String from = node2.getPath();
        String to = node1.getParent().getPath() + "/" + nodeName2;
        session.move(from, to);
        try {
            if (save) {
                node2.save();
            } else {
                node2.refresh(false);
            }
            fail("Refresh and Save should not work for moved nodes");
        } catch (RepositoryException e) {
            // expected
        }
        session.save();
        NodeIterator it = node2.getParent().getNodes(nodeName2);
        assertTrue(it.hasNext());
        it.nextNode();
        assertFalse(it.hasNext());
        node2.getParent().getPath();

        // for (it = testRootNode.getNodes(); it.hasNext();) {
             // System.out.println(it.nextNode().getPath());
        // }
        String now = node2.getPath();
        assertEquals(testRootNode.getPath() + "/" + nodeName2, now);
    }

    /**
     * Test reordering same-name-siblings using move
     */
    public void testReorderSameNameSiblingsUsingMove() throws RepositoryException {
        Session session = testRootNode.getSession();
        for (NodeIterator it = testRootNode.getNodes(); it.hasNext();) {
            it.nextNode().remove();
            session.save();
        }
        Node node1 = testRootNode.addNode(nodeName1);
        Node node2 = testRootNode.addNode(nodeName1);
        String path = node1.getPath();

        // re-order the nodes using move
        session.move(path, path);

        assertEquals(path + "[2]", node1.getPath());
        assertEquals(path, node2.getPath());
    }

    /**
     * Verify paths of same name siblings are correct after a (reordering) move
     * Issue JCR-1880
     */
    public void testGetPathDoesNotInfluencePathsAfterMove() throws RepositoryException {
        doTestMoveWithGetPath(false);
        doTestMoveWithGetPath(true);
    }

    private void doTestMoveWithGetPath(boolean index) throws RepositoryException {
        Session session = testRootNode.getSession();
        for (NodeIterator it = testRootNode.getNodes(); it.hasNext();) {
            it.nextNode().remove();
            session.save();
        }
        String testPath = testRootNode.getPath();
        Node a = testRootNode.addNode("a");
        Node b = a.addNode("b");
        session.save();
        session.move(testPath + "/a/b", testPath + "/a");
        if (index) {
            b.getPath();
        }
        session.move(testPath + "/a", testPath + "/a");
        assertEquals(testPath + "/a[2]", a.getPath());
        assertEquals(testPath + "/a", b.getPath());
    }

}
