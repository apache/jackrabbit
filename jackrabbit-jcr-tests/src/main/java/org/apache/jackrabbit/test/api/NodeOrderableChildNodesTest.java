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
package org.apache.jackrabbit.test.api;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.nodetype.NodeType;
import javax.jcr.Node;
import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

/**
 * <code>NodeOrderableChildNodesTest</code> contains all node writing tests (LEVEL 2) that require a node
 * that allows child node ordering (tests therefore are optional).
 * <p/>
 * If the repository does not support a node type with orderable child nodes
 * a {@link NotExecutableException} exception is thrown.
 * <p/>
 * Prerequisites:
 * <ul>
 * <li><code>javax.jcr.tck.NodeOrderableChildNodesTest.nodetype2</code>Name of a
 * valid node type that allows orderable child nodes</li>
 * <li><code>javax.jcr.tck.NodeOrderableChildNodesTest.nodetype3</code>Name of a
 * valid node type that can be added as child node</li>
 * </ul>
 *
 * @test
 * @sources NodeOrderableChildNodesTest.java
 * @executeClass org.apache.jackrabbit.test.api.NodeOrderableChildNodesTest
 * @keywords level2
 */
public class NodeOrderableChildNodesTest extends AbstractJCRTest {

    /**
     * A child Node of {@link #parentNode}.
     */
    private Node initialFirstNode;

    /**
     * A child Node of {@link #parentNode}.
     */
    private Node initialSecondNode;

    /**
     * The node that allows orderable child nodes
     */
    private Node parentNode;

    protected void tearDown() throws Exception {
        initialFirstNode = null;
        initialSecondNode = null;
        parentNode = null;
        super.tearDown();
    }

    /**
     * Tries to reorder child nodes using {@link Node#orderBefore(String, String)}
     * with an invalid destination reference. <br/><br/> This should
     * throw an {@link ItemNotFoundException}.
     */
    public void testOrderBeforeInvalidDest()
            throws RepositoryException, NotExecutableException {
        checkOrderableNodeType(getProperty("nodetype2"));
        prepareTest();

        // ok lets try to reorder
        try {
            parentNode.orderBefore(initialSecondNode.getName(), "invalid");
            fail("Trying to reorder child nodes using Node.orderBefore() where destination is invalid" +
                    " should throw ItemNotFoundException!");
        } catch (ItemNotFoundException e) {
            // ok
        }
    }

    /**
     * Tries to reorder child nodes using {@link Node#orderBefore(String,
            * String)}  with an invalid source reference. <br/><br/> This should throw
     * an {@link ItemNotFoundException}.
     */
    public void testOrderBeforeInvalidSrc()
            throws RepositoryException, NotExecutableException {
        checkOrderableNodeType(getProperty("nodetype2"));
        prepareTest();

        // ok lets try to reorder
        try {
            parentNode.orderBefore("invalid", initialFirstNode.getName());
            fail("Trying to reorder child nodes using Node.orderBefore() where source is invalid " +
                    "should throw ItemNotFoundException!");
        } catch (ItemNotFoundException e) {
            // ok
        }
    }

    /**
     * Tries to reorder on a node using {@link Node#orderBefore(String, String)}
     * that does not support child reordering. <br/><br/> This should throw and
     * {@link UnsupportedRepositoryOperationException}. Prequisites: <ul>
     * <li>javax.jcr.tck.NodeOrderableChildNodesTest.testOrderBeforeUnsupportedRepositoryOperationException.nodetype2</li>
     * A valid node type that does not support child node ordering.</li>
     * <li>javax.jcr.tck.NodeOrderableChildNodesTest.testOrderBeforeUnsupportedRepositoryOperationException.nodetype3</li>
     * A valid node type that can be added as a child. </ul>
     */
    public void testOrderBeforeUnsupportedRepositoryOperationException()
            throws RepositoryException, NotExecutableException {

        // create testNode
        parentNode = testRootNode.addNode(nodeName1, getProperty("nodetype2"));
        // add child node
        Node firstNode = parentNode.addNode(nodeName2, getProperty("nodetype3"));
        // add a second child node
        Node secondNode = parentNode.addNode(nodeName3, getProperty("nodetype3"));
        // save the new nodes
        superuser.save();

        // ok lets try to reorder
        try {
            parentNode.orderBefore(secondNode.getName(), firstNode.getName());
            fail("Trying to reorder child nodes using Node.orderBefore() on node that " +
                    "does not support ordering should throw UnsupportedRepositoryException!");
        } catch (UnsupportedRepositoryOperationException e) {
            // ok
        }
    }


    /**
     * Creates two child nodes, reorders first node to end, uses parentNode's
     * {@link Node#save()}.
     */
    public void testOrderBeforePlaceAtEndParentSave()
            throws RepositoryException, NotExecutableException {
        checkOrderableNodeType(getProperty("nodetype2"));
        prepareTest();

        // ok lets reorder and save
        parentNode.orderBefore(initialFirstNode.getName(), null);
        parentNode.save();

        // get child node refs
        NodeIterator it = parentNode.getNodes();
        Node firstNode = it.nextNode();
        Node secondNode = it.nextNode();

        // lets see if reordering worked
        assertTrue("Child nodes are not added in proper order after Node.orderBefore()!", firstNode.isSame(initialSecondNode));
        assertTrue("Child nodes are not added in proper order after Node.orderBefore()!", secondNode.isSame(initialFirstNode));
    }

    /**
     * Test Creates two child nodes, verifies that they are added propery.
     * reorders first node to the end , checks again. uses session.save();
     */
    public void testOrderBeforePlaceAtEndSessionSave()
            throws RepositoryException, NotExecutableException {
        checkOrderableNodeType(getProperty("nodetype2"));
        prepareTest();

        // ok lets reorder and save
        parentNode.orderBefore(initialFirstNode.getName(), null);
        superuser.save();

        // get child node refs
        NodeIterator it = parentNode.getNodes();
        Node firstNode = it.nextNode();
        Node secondNode = it.nextNode();

        // lets see if reordering worked
        assertTrue("Child nodes are not added in proper order after Node.orderBefore()!", firstNode.isSame(initialSecondNode));
        assertTrue("Child nodes are not added in proper order after Node.orderBefore()!", secondNode.isSame(initialFirstNode));
    }


    /**
     * Creates two child nodes, reorders second node before first, uses
     * parentNode's {@link Node#save()}.
     */
    public void testOrderBeforeSecondToFirstParentSave()
            throws RepositoryException, NotExecutableException {
        checkOrderableNodeType(getProperty("nodetype2"));
        prepareTest();

        // ok lets reorder and save
        parentNode.orderBefore(initialSecondNode.getName(), initialFirstNode.getName());
        parentNode.save();

        // get child node refs
        NodeIterator it = parentNode.getNodes();
        Node firstNode = it.nextNode();
        Node secondNode = it.nextNode();

        // lets see if reordering worked
        assertTrue("Child nodes are not added in proper order after Node.orderBefore()!", firstNode.isSame(initialSecondNode));
        assertTrue("Child nodes are not added in proper order after Node.orderBefore()!", secondNode.isSame(initialFirstNode));
    }

    /**
     * Creates two child nodes than reorders second node before first, saves
     * using {@link Session#save()}.
     */
    public void testOrderBeforeSecondToFirstSessionSave()
            throws RepositoryException, NotExecutableException {
        checkOrderableNodeType(getProperty("nodetype2"));
        prepareTest();

        // ok lets reorder and save
        parentNode.orderBefore(initialSecondNode.getName(), initialFirstNode.getName());
        superuser.save();

        // get child node refs
        NodeIterator it = parentNode.getNodes();
        Node firstNode = it.nextNode();
        Node secondNode = it.nextNode();

        // lets see if reordering worked
        assertTrue("Child nodes are not added in proper order after Node.orderBefore()!", firstNode.isSame(initialSecondNode));
        assertTrue("Child nodes are not added in proper order after Node.orderBefore()!", secondNode.isSame(initialFirstNode));
    }

    /**
     * Sets up the test content needed for the test cases.
     */
    private void prepareTest() throws RepositoryException {
        // get root node
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());
        // create testNode
        parentNode = defaultRootNode.addNode(nodeName1, getProperty("nodetype2"));
        // add child node
        Node firstNode = parentNode.addNode(nodeName2, getProperty("nodetype3"));
        // add a second child node
        Node secondNode = parentNode.addNode(nodeName3, getProperty("nodetype3"));
        // save the new nodes
        superuser.save();

        // get child node refs
        NodeIterator it = parentNode.getNodes();
        initialFirstNode = it.nextNode();
        initialSecondNode = it.nextNode();

        // first lets test if the nodes have been added in the right order
        assertTrue("Child nodes are not added in proper order ", firstNode.isSame(initialFirstNode));
        assertTrue("Child nodes are not added in proper order ", secondNode.isSame(initialSecondNode));
    }

    /**
     * Checks if the NodeType with name <code>ntName</code> supports orderable
     * child nodes. If not a {@link NotExecutableException} is thrown.
     * @param ntName the name of the node type.
     */
    private void checkOrderableNodeType(String ntName)
            throws RepositoryException, NotExecutableException {
        NodeType nt = superuser.getWorkspace().getNodeTypeManager().getNodeType(ntName);
        if (!nt.hasOrderableChildNodes()) {
            throw new NotExecutableException("NodeType: " + ntName + " does not support orderable child nodes.");
        }
    }
}
