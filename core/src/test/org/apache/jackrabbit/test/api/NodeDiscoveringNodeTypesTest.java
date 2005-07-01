/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Value;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import java.util.NoSuchElementException;

/**
 * All test cases in this class rely on content in the repository. That is the
 * default workspace must at least contain one child node under {@link #testRoot}
 * otherwise a {@link NotExecutableException} is thrown.
 *
 * @test
 * @sources NodeDiscoveringNodeTypesTest.java
 * @executeClass org.apache.jackrabbit.test.api.NodeDiscoveringNodeTypesTest
 * @keywords level1
 */
public class NodeDiscoveringNodeTypesTest extends AbstractJCRTest {

    /**
     * The session we use for the tests
     */
    private Session session;

    /**
     * A child node of the root node in the default workspace.
     */
    private Node childNode;

    /**
     * Sets up the fixture for the test cases.
     */
    protected void setUp() throws Exception {
        isReadOnly = true;
        super.setUp();

        session = helper.getReadOnlySession();
        testRootNode = session.getRootNode().getNode(testPath);
        NodeIterator nodes = testRootNode.getNodes();
        try {
            childNode = nodes.nextNode();
        } catch (NoSuchElementException e) {
        }
    }

    /**
     * Releases the session aquired in {@link #setUp()}.
     */
    protected void tearDown() throws Exception {
        if (session != null) {
            session.logout();
        }
        super.tearDown();
    }

    /**
     * Test if getPrimaryNodeType() returns the node type according to the
     * property "jcr:primaryType"
     */
    public void testGetPrimaryNodeType()
            throws NotExecutableException, RepositoryException {

        if (childNode == null) {
            throw new NotExecutableException("Workspace does not have sufficient content for this test. " +
                    "Root node must have at least one child node.");
        }

        NodeType type = childNode.getPrimaryNodeType();
        String name = childNode.getProperty(jcrPrimaryType).getString();

        assertEquals("getPrimaryNodeType() must return the node type stored " +
                "as property \"jcr:primaryType\"",
                name, type.getName());

        assertFalse("getPrimaryNodeType() must return a primary node type",
                type.isMixin());
    }


    /**
     * Test if getMixinNodeType returns the node types according to the property
     * "jcr:mixinTypes". Therefor a node with mixin types is located recursively
     * in the entire repository. A NotExecutableException is thrown when no such
     * node is found.
     */
    public void testGetMixinNodeTypes()
            throws NotExecutableException, RepositoryException {

        if (childNode == null) {
            throw new NotExecutableException("Workspace does not have sufficient content for this test. " +
                    "Root node must have at least one child node.");
        }

        Node node = locateNodeWithMixinNodeTypes(testRootNode);

        if (node == null) {
            throw new NotExecutableException("Workspace does not contain a node with mixin node types defined");
        }

        Value names[] = node.getProperty(jcrMixinTypes).getValues();
        NodeType types[] = node.getMixinNodeTypes();

        assertEquals("getMixinNodeTypes() does not return the same number of " +
                "node types as " +
                "getProperty(\"jcr:mixinTypes\").getValues()",
                types.length,
                names.length);

        StringBuffer namesString = new StringBuffer();
        for (int i = 0; i < names.length; i++) {
            namesString.append("|" + names[i].getString() + "|");
        }

        for (int i = 0; i < types.length; i++) {
            String pattern = "|" + types[i].getName() + "|";

            assertTrue("getMixinNodeTypes() does not return the same node" +
                    "types as getProperty(\"jcr:mixinTypes\").getValues()",
                    namesString.indexOf(pattern) != -1);

            assertTrue("All nodes returned by getMixinNodeTypes() must be" +
                    "mixin",
                    types[i].isMixin());
        }
    }


    /**
     * Test if isNodeTye(String nodeTypeName) returns true if nodeTypeName is the
     * name of the primary node type, the name of a mixin node type and the name
     * of a supertype.
     */
    public void testIsNodeType()
            throws NotExecutableException, RepositoryException {

        String nodeTypeName;

        // test with primary node's name
        nodeTypeName = testRootNode.getPrimaryNodeType().getName();
        assertTrue("isNodeType(String nodeTypeName) must return true if " +
                "nodeTypeName is the name of the primary node type",
                testRootNode.isNodeType(nodeTypeName));

        // test with mixin node's name
        // (if such a node is available)
        Node nodeWithMixin = locateNodeWithMixinNodeTypes(testRootNode);
        if (nodeWithMixin != null) {
            NodeType types[] = nodeWithMixin.getMixinNodeTypes();
            nodeTypeName = types[0].getName();
            assertTrue("isNodeType(String nodeTypeName) must return true if " +
                    "nodeTypeName is the name of one of the " +
                    "mixin node types",
                    nodeWithMixin.isNodeType(nodeTypeName));
        }

        // test with the name of predefined supertype "nt:base"
        assertTrue("isNodeType(String nodeTypeName) must return true if " +
                "nodeTypeName is the name of a node type of a supertype",
                testRootNode.isNodeType(ntBase));
    }

    //-----------------------< internal >---------------------------------------

    /**
     * Returns the first descendant of <code>node</code> which defines mixin
     * node type(s).
     *
     * @param node <code>Node</code> to start traversal.
     * @return first node with mixin node type(s)
     */
    private Node locateNodeWithMixinNodeTypes(Node node)
            throws RepositoryException {

        if (node.getMixinNodeTypes().length != 0) {
            return node;
        }

        NodeIterator nodes = node.getNodes();
        while (nodes.hasNext()) {
            Node returnedNode = this.locateNodeWithMixinNodeTypes(nodes.nextNode());
            if (returnedNode != null) {
                return returnedNode;
            }
        }
        return null;
    }


}