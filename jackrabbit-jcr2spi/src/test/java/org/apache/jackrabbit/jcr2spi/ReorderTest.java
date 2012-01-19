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

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>ReorderTest</code>...
 */
public class ReorderTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(ReorderTest.class);

    protected Node child1;
    protected Node child2;
    protected Node child3;
    protected Node child4;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (!testRootNode.getPrimaryNodeType().hasOrderableChildNodes()) {
            throw new NotExecutableException("Test node does not have orderable children.");
        }
        NodeIterator it = testRootNode.getNodes();
        if (it.hasNext()) {
            throw new NotExecutableException("Test node already contains child nodes");
        }
        createOrderableChildren();
    }

    @Override
    protected void tearDown() throws Exception {
        child1 = null;
        child2 = null;
        child3 = null;
        child4 = null;
        super.tearDown();
    }

    protected void createOrderableChildren() throws RepositoryException, LockException, ConstraintViolationException, NoSuchNodeTypeException, ItemExistsException, VersionException, NotExecutableException {
        child1 = testRootNode.addNode(nodeName1, testNodeType);
        child2 = testRootNode.addNode(nodeName2, testNodeType);
        child3 = testRootNode.addNode(nodeName3, testNodeType);
        child4 = testRootNode.addNode(nodeName4, testNodeType);

        testRootNode.save();
    }

    protected static String getRelPath(Node child) throws RepositoryException {
        if (child == null) {
            return null;
        }
        String path = child.getPath();
        return path.substring(path.lastIndexOf('/')+1);
    }

    protected static void testOrder(Node parent, Node[] children) throws RepositoryException {
        NodeIterator it = parent.getNodes();
        int i = 0;
        while (it.hasNext()) {
            Node child = it.nextNode();
            if (i >= children.length) {
                fail("Reorder added a child node.");
            }
            assertTrue("Wrong order of children: " + child + " is not the same as " + children[i], child.isSame(children[i]));
            i++;
        }

        if (i < children.length-1) {
            fail("Reorder removed a child node.");
        }
    }

    public void testReorder() throws RepositoryException {
        testRootNode.orderBefore(getRelPath(child1), getRelPath(child3));
        testOrder(testRootNode, new Node[] { child2, child1, child3, child4});

        testRootNode.save();
        testOrder(testRootNode, new Node[] { child2, child1, child3, child4});
    }

    public void testReorderToEnd() throws RepositoryException, ConstraintViolationException, UnsupportedRepositoryOperationException, VersionException {
        testRootNode.orderBefore(getRelPath(child2), null);
        testOrder(testRootNode, new Node[] { child1, child3, child4, child2});

        testRootNode.save();
        testOrder(testRootNode, new Node[] { child1, child3, child4, child2});
    }

    public void testRevertReorder() throws RepositoryException {
        testRootNode.orderBefore(getRelPath(child4), getRelPath(child2));
        testOrder(testRootNode, new Node[] { child1, child4, child2, child3});

        testRootNode.refresh(false);
        testOrder(testRootNode, new Node[] { child1, child2, child3, child4});
    }

    public void testRevertReorderToEnd() throws RepositoryException {
        testRootNode.orderBefore(getRelPath(child1), null);
        testOrder(testRootNode, new Node[] { child2, child3, child4, child1});

        testRootNode.refresh(false);
        testOrder(testRootNode, new Node[] { child1, child2, child3, child4});
    }

    public void testReorder2() throws RepositoryException {
        testRootNode.orderBefore(getRelPath(child3), getRelPath(child1));
        testRootNode.save();

        Session otherSession = getHelper().getReadOnlySession();
        try {
            testOrder((Node) otherSession.getItem(testRootNode.getPath()), new Node[] {child3, child1, child2, child4});
        } finally {
            otherSession.logout();
        }
    }

    public void testReorderTwice() throws RepositoryException {
        testRootNode.orderBefore(getRelPath(child2), null);
        testRootNode.orderBefore(getRelPath(child4), getRelPath(child1));

        testOrder(testRootNode, new Node[] { child4, child1, child3, child2});
        testRootNode.save();
        testOrder(testRootNode, new Node[] { child4, child1, child3, child2});
    }

    public void testReorderFinallyOriginalOrder() throws RepositoryException {
        testRootNode.orderBefore(getRelPath(child4), getRelPath(child1));
        testRootNode.orderBefore(getRelPath(child3), getRelPath(child4));
        testRootNode.orderBefore(getRelPath(child2), getRelPath(child3));
        testRootNode.orderBefore(getRelPath(child1), getRelPath(child2));

        testOrder(testRootNode, new Node[] { child1, child2, child3, child4});
        testRootNode.save();
        testOrder(testRootNode, new Node[] { child1, child2, child3, child4});
    }
}
