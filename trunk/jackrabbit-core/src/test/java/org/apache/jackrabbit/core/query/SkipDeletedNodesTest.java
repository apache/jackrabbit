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
package org.apache.jackrabbit.core.query;

import javax.jcr.Session;
import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.InvalidItemStateException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.QueryManager;

/**
 * Tests if the NodeIterator returned by {@link javax.jcr.query.QueryResult#getNodes()}
 * skips Nodes that have been deleted in the meantime.
 */
public class SkipDeletedNodesTest extends AbstractQueryTest {

    private Session s2;

    private QueryManager qm;

    protected void setUp() throws Exception {
        super.setUp();
        s2 = getHelper().getSuperuserSession();
        qm = s2.getWorkspace().getQueryManager();
    }

    protected void tearDown() throws Exception {
        try {
            if (s2 != null) {
                s2.logout();
                s2 = null;
            }
        } finally {
            qm = null;
            super.tearDown();
        }
    }

    /**
     * Executes a query with one session and removes a node from that query
     * result with another session.
     */
    public void testRemoveFirstNode() throws RepositoryException {
        Node n1 = testRootNode.addNode("node1");
        testRootNode.addNode("node2");
        testRootNode.addNode("node3");
        testRootNode.save();

        // query the workspace for all three nodes
        String stmt = testPath + "/*";
        QueryResult res = qm.createQuery(stmt, Query.XPATH).execute();

        // now remove the first node
        n1.remove();
        testRootNode.save();

        // iterate over nodes
        log.println("Result nodes:");
        int count = 0;
        for (NodeIterator it = res.getNodes(); it.hasNext(); ) {
            assertEquals("Wrong value for getPosition().", count++, it.getPosition());
            try {
                log.println(it.nextNode().getPath());
            } catch (InvalidItemStateException e) {
                // this is allowed
                log.println("Invalid: <deleted>");
            }
        }
    }

    /**
     * Executes a query with one session and removes a node from that query
     * result with another session.
     */
    public void testRemoveSomeNode() throws RepositoryException {
        testRootNode.addNode("node1");
        Node n2 = testRootNode.addNode("node2");
        testRootNode.addNode("node3");
        testRootNode.save();

        // query the workspace for all three nodes
        String stmt = testPath + "/*";
        QueryResult res = qm.createQuery(stmt, Query.XPATH).execute();

        // now remove the second node
        n2.remove();
        testRootNode.save();

        // iterate over nodes
        int count = 0;
        log.println("Result nodes:");
        for (NodeIterator it = res.getNodes(); it.hasNext(); ) {
            assertEquals("Wrong value for getPosition().", count++, it.getPosition());
            try {
                log.println(it.nextNode().getPath());
            } catch (InvalidItemStateException e) {
                // this is allowed
                log.println("Invalid: <deleted>");
            }
        }
    }

    /**
     * Executes a query with one session and removes a node from that query
     * result with another session.
     */
    public void testRemoveLastNode() throws RepositoryException {
        testRootNode.addNode("node1");
        testRootNode.addNode("node2");
        Node n3 = testRootNode.addNode("node3");
        testRootNode.save();

        // query the workspace for all three nodes
        String stmt = testPath + "/*";
        QueryResult res = qm.createQuery(stmt, Query.XPATH).execute();

        // now remove the last node
        n3.remove();
        testRootNode.save();

        // iterate over nodes
        int count = 0;
        log.println("Result nodes:");
        for (NodeIterator it = res.getNodes(); it.hasNext(); ) {
            assertEquals("Wrong value for getPosition().", count++, it.getPosition());
            try {
                log.println(it.nextNode().getPath());
            } catch (InvalidItemStateException e) {
                // this is allowed
                log.println("Invalid: <deleted>");
            }
        }
    }

    /**
     * Executes a query with one session and removes a node from that query
     * result with another session.
     * </p>This test is different from the other tests that it removes the
     * node after another session has called hasNext() to retrieve the node
     * that gets deleted.
     */
    public void testRemoveFirstNodeAfterHasNext() throws RepositoryException {
        Node n1 = testRootNode.addNode("node1");
        testRootNode.addNode("node2");
        testRootNode.addNode("node3");
        testRootNode.save();

        // query the workspace for all three nodes
        String stmt = testPath + "/*";
        QueryResult res = qm.createQuery(stmt, Query.XPATH).execute();

        NodeIterator it = res.getNodes();
        it.hasNext();

        // now remove the first node
        n1.remove();
        testRootNode.save();

        // iterate over nodes
        int count = 0;
        log.println("Result nodes:");
        while (it.hasNext()) {
            assertEquals("Wrong value for getPosition().", count++, it.getPosition());
            try {
                log.println(it.nextNode().getPath());
            } catch (InvalidItemStateException e) {
                // this is allowed
                log.println("Invalid: <deleted>");
            }
        }
    }

    /**
     * Executes a query with one session and removes a node from that query
     * result with another session.
     * </p>This test is different from the other tests that it removes the
     * node after another session has called hasNext() to retrieve the node
     * that gets deleted.
     */
    public void testRemoveSomeNodeAfterHasNext() throws RepositoryException {
        testRootNode.addNode("node1");
        Node n2 = testRootNode.addNode("node2");
        testRootNode.addNode("node3");
        testRootNode.save();

        // query the workspace for all three nodes
        String stmt = testPath + "/*";
        QueryResult res = qm.createQuery(stmt, Query.XPATH).execute();

        NodeIterator it = res.getNodes();
        it.hasNext();

        // now remove the second node
        n2.remove();
        testRootNode.save();

        // iterate over nodes
        int count = 0;
        log.println("Result nodes:");
        while (it.hasNext()) {
            assertEquals("Wrong value for getPosition().", count++, it.getPosition());
            try {
                log.println(it.nextNode().getPath());
            } catch (InvalidItemStateException e) {
                // this is allowed
                log.println("Invalid: <deleted>");
            }
        }
    }
}
