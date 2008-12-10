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

}
