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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>NodeOrderTest</code>...
 */
public class NodeOrderTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(NodeOrderTest.class);

    private Node[] children;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (!testRootNode.getPrimaryNodeType().hasOrderableChildNodes()) {
            throw new NotExecutableException("Test node does not have orderable children.");
        }
        children = new Node[4];
        children[0] = testRootNode.addNode(nodeName1, testNodeType);
        children[1] = testRootNode.addNode(nodeName2, testNodeType);
        children[2] = testRootNode.addNode(nodeName3, testNodeType);
        children[3] = testRootNode.addNode(nodeName4, testNodeType);
        testRootNode.save();
    }

    @Override
    protected void tearDown() throws Exception {
        children = null;
        super.tearDown();
    }

    private static void checkOrder(NodeIterator it, Node[] children) throws RepositoryException {
        int i = 0;
        while (it.hasNext()) {
            if (i >= children.length) {
                fail("Node.getNodes() return more child nodes, that have been created.");
            }
            Node n = it.nextNode();
            assertTrue("Wrong order of child nodes returned by Node.getNodes().", n.isSame(children[i]));
            i++;
        }

        if (i != children.length) {
            fail("Node.getNodes() did not return all child nodes.");
        }
    }

    /**
     * Test if the order of Nodes is maintained across multiple calls to
     * <code>Node.getNodes()</code>.
     */
    public void testOrder() throws RepositoryException {
        NodeIterator it = testRootNode.getNodes();
        checkOrder(it, children);
        it = testRootNode.getNodes();
        checkOrder(it, children);
    }

    /**
     * Test if the order of Nodes is the same when accessed through another
     * <code>Session</code>.
     */
    public void testOrder2() throws RepositoryException {
        Session another = getHelper().getReadOnlySession();
        try {
            NodeIterator it = ((Node) another.getItem(testRootNode.getPath())).getNodes();
            checkOrder(it, children);
        } finally {
            another.logout();
        }
    }

    /**
     * Test if the order of Nodes is the same when accessed through another
     * <code>Session</code> after having accessed some of the nodes individually.
     */
    public void testOrderAfterIndividualAccess() throws RepositoryException {
        Session another = getHelper().getReadOnlySession();
        try {
            Node n2 = (Node) another.getItem(children[2].getPath());
            Node n0 = (Node) another.getItem(children[0].getPath());
            NodeIterator it = ((Node) another.getItem(testRootNode.getPath())).getNodes();
            checkOrder(it, children);
        } finally {
            another.logout();
        }
    }

    /**
     * Test if the order of Nodes is the same when accessed through another
     * <code>Session</code> after having accessed some of the nodes individually.
     */
    public void testOrderAfterIndividualAccess2() throws RepositoryException {
        Session another = getHelper().getReadOnlySession();
        try {
            Node n2 = (Node) another.getItem(children[3].getPath());
            Node n3 = (Node) another.getItem(children[1].getPath());
            NodeIterator it = ((Node) another.getItem(testRootNode.getPath())).getNodes();
            checkOrder(it, children);
        } finally {
            another.logout();
        }
    }
}