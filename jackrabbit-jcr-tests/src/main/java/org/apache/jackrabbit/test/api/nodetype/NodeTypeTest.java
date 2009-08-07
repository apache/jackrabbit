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
package org.apache.jackrabbit.test.api.nodetype;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;

/**
 * Tests if the node type hierarchy is correctly mapped to the methods
 * defined in {@link NodeType}.
 *
 * @test
 * @sources NodeTypeTest.java
 * @executeClass org.apache.jackrabbit.test.api.nodetype.NodeTypeTest
 * @keywords level1
 */
public class NodeTypeTest extends AbstractJCRTest {

    /**
     * The session we use for the tests
     */
    private Session session;

    /**
     * The node type manager of the session
     */
    private NodeTypeManager manager;

    /**
     * The root node of the default workspace
     */
    private Node rootNode;

    /**
     * Sets up the fixture for the test cases.
     */
    protected void setUp() throws Exception {
        isReadOnly = true;
        super.setUp();

        session = helper.getReadOnlySession();
        manager = session.getWorkspace().getNodeTypeManager();
        rootNode = session.getRootNode();
    }

    /**
     * Releases the session aquired in {@link #setUp()}.
     */
    protected void tearDown() throws Exception {
        if (session != null) {
            session.logout();
            session = null;
        }
        manager = null;
        rootNode = null;
        super.tearDown();
    }

    /**
     * Test if getNode() returns the name of a node type.
     */
    public void testGetName() throws RepositoryException {
        NodeType type = manager.getNodeType(ntBase);
        assertEquals("getName() must return the name of the node",
                ntBase, type.getName());
    }

    /**
     * Test if isMixin() returns false if applied on a primary node type and true
     * on a mixin node type.
     */
    public void testIsMixin() throws RepositoryException {

        NodeTypeIterator primaryTypes = manager.getPrimaryNodeTypes();
        assertFalse("testIsMixin() must return false if applied on a " +
                "primary node type",
                primaryTypes.nextNodeType().isMixin());

        // if a mixin node type exist, test if isMixin() returns true
        NodeTypeIterator mixinTypes = manager.getMixinNodeTypes();
        if (getSize(mixinTypes) > 0) {
            // need to re-aquire iterator {@link #getSize} may consume iterator
            mixinTypes = manager.getMixinNodeTypes();
            assertTrue("testIsMixin() must return true if applied on a " +
                    "mixin node type",
                    mixinTypes.nextNodeType().isMixin());
        }
        // else skip the test for mixin node types
    }

    /**
     * Test if node.getPrimaryItemName() returns the same name as
     * node.getPrimaryItem().getName()
     */
    public void testGetPrimaryItemName()
            throws NotExecutableException, RepositoryException {

        Node node = locateNodeWithPrimaryItem(rootNode);

        if (node == null) {
            throw new NotExecutableException("Workspace does not contain a node with primary item defined");
        }

        String name = node.getPrimaryItem().getName();
        NodeType type = node.getPrimaryNodeType();

        assertEquals("node.getPrimaryNodeType().getPrimaryItemName() " +
                "must return the same name as " +
                "node.getPrimaryItem().getName()",
                name, type.getPrimaryItemName());
    }

    /**
     * Test if node.getPrimaryItemName() returns null if no primary item is
     * defined
     */
    public void testGetPrimaryItemNameNotExisting()
            throws NotExecutableException, RepositoryException {

        Node node = locateNodeWithoutPrimaryItem(rootNode);

        if (node == null) {
            throw new NotExecutableException("Workspace does not contain a node without primary item defined");
        }

        NodeType type = node.getPrimaryNodeType();

        assertNull("getPrimaryItemName() must return null if NodeType " +
                "does not define a primary item",
                type.getPrimaryItemName());
    }


    /**
     * Test if getSupertypes() of a primary node that is not "nt:base" returns at
     * least "nt:base". NotExecutableException is thrown if no primary node type
     * apart from "nt:base".
     */
    public void testGetSupertypes()
            throws NotExecutableException, RepositoryException {

        // find a primary node type but not "nt:base"
        NodeTypeIterator types = manager.getPrimaryNodeTypes();
        NodeType type = null;
        while (types.hasNext()) {
            type = types.nextNodeType();
            if (!type.getName().equals(ntBase)) {
                break;
            }
        }

        // note: type is never null, since at least "nt:base" must exist
        if (type.getName().equals("nt:base")) {
            throw new NotExecutableException("Workspace does not have sufficient primary node types to run " +
                    "this test. At least nt:base plus anther type are required.");
        }

        NodeType supertypes[] = type.getSupertypes();
        boolean hasNTBase = false;
        for (int i = 0; i < supertypes.length; i++) {
            if (supertypes[i].getName().equals(ntBase)) {
                hasNTBase = true;
                break;
            }
        }
        assertTrue("getSupertypes() of a primary node type that is not " +
                "\"nt:base\" must at least return \"nt:base\"",
                hasNTBase);
    }

    /**
     * Test if all node types returned by getDeclatedSupertypes() are also
     * returned by getSupertypes(). All existing node types are tested.
     */
    public void testGetDeclaredSupertypes()
            throws RepositoryException {

        NodeTypeIterator types = manager.getAllNodeTypes();
        while (types.hasNext()) {
            NodeType type = types.nextNodeType();

            NodeType declaredSupertypes[] = type.getDeclaredSupertypes();
            NodeType supertypes[] = type.getSupertypes();

            try {
                for (int i = 0; i < declaredSupertypes.length; i++) {
                    boolean exists = false;
                    for (int j = 0; j < supertypes.length; j++) {
                        if (supertypes[j].getName().equals(declaredSupertypes[i].getName())) {
                            exists = true;
                            break;
                        }
                    }
                    assertTrue("All node types returned by " +
                            "getDeclaredSupertypes() must also be " +
                            "returned by getSupertypes()",
                            exists);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                fail("The array returned by " +
                        "getDeclaredSupertypes() must not exceed " +
                        "the one returned by getSupertypes()");
            }
        }
    }

    /**
     * Test if isNodeType(String nodeTypeName) returns true if nodeTypeName is
     * the name of the node itself. Also, primary node types must return true if
     * nodeTypeName is "nt:base", and mixin node types must return false in that
     * case.
     */
    public void testIsNodeType()
            throws RepositoryException {

        // find a primary node type but not "nt:base"
        NodeTypeIterator types = manager.getPrimaryNodeTypes();
        while (types.hasNext()) {
            NodeType type = types.nextNodeType();
            assertTrue("isNodeType(String nodeTypeName) must return true if " +
                    "NodeType is nodeTypeName",
                    type.isNodeType(type.getName()));
            if (type.isMixin()) {
                assertFalse("isNodeType(String nodeTypeName) must return " +
                        "false if NodeType is not a subtype of " +
                        "nodeTypeName",
                        type.isNodeType(ntBase));
            } else {
                assertTrue("isNodeType(String nodeTypeName) must return true if " +
                        "NodeType is a subtype of nodeTypeName",
                        type.isNodeType(ntBase));
            }
        }
    }

    /**
     * Test if all property defs returned by getDeclatedPropertyDefs() are also
     * returned by getPropertyDefs(). All existing node types are tested.
     */
    public void testGetDeclaredPropertyDefs()
            throws RepositoryException {

        NodeTypeIterator types = manager.getAllNodeTypes();
        while (types.hasNext()) {
            NodeType type = types.nextNodeType();

            PropertyDefinition declaredDefs[] = type.getDeclaredPropertyDefinitions();
            PropertyDefinition defs[] = type.getPropertyDefinitions();

            try {
                for (int i = 0; i < declaredDefs.length; i++) {
                    boolean exists = false;
                    for (int j = 0; j < defs.length; j++) {
                        if (defs[j].getName().equals(declaredDefs[i].getName())) {
                            exists = true;
                            break;
                        }
                    }
                    assertTrue("All property defs returned by " +
                            "getDeclaredPropertyDefs() must also be " +
                            "returned by getPropertyDefs()",
                            exists);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                fail("The array returned by " +
                        "getDeclaredPropertyDefs() must not exceed " +
                        "the one returned by getPropertyDefs()");
            }
        }
    }

    /**
     * Test if getPropertyDefs() of a primary node returns also "jcr:primaryType"
     * which is inherited from "nt:base".
     */
    public void testGetPropertyDefs()
            throws NotExecutableException, RepositoryException {

        // find a primary node type but not "nt:base"
        NodeTypeIterator types = manager.getPrimaryNodeTypes();
        while (types.hasNext()) {
            NodeType type = types.nextNodeType();
            PropertyDefinition defs[] = type.getPropertyDefinitions();
            boolean hasJCRPrimaryType = false;
            for (int i = 0; i < defs.length; i++) {
                if (defs[i].getName().equals(jcrPrimaryType)) {
                    hasJCRPrimaryType = true;
                    break;
                }
            }
            assertTrue("getPropertyDefs() of a primary node type " +
                    "must return also \"jcr:primaryType\".",
                    hasJCRPrimaryType);
        }
    }

    /**
     * Test if all node defs returned by getDeclaredChildNodeDefs() are also
     * returned by getChildNodeDefs(). All existing node types are tested.
     */
    public void testGetDeclaredChildNodeDefs()
            throws RepositoryException {

        NodeTypeIterator types = manager.getAllNodeTypes();
        while (types.hasNext()) {
            NodeType type = types.nextNodeType();

            NodeDefinition declaredDefs[] = type.getDeclaredChildNodeDefinitions();
            NodeDefinition defs[] = type.getChildNodeDefinitions();

            try {
                for (int i = 0; i < declaredDefs.length; i++) {
                    boolean exists = false;
                    for (int j = 0; j < defs.length; j++) {
                        if (defs[j].getName().equals(declaredDefs[i].getName())) {
                            exists = true;
                            break;
                        }
                    }
                    assertTrue("All node defs returned by " +
                            "getDeclaredChildNodeDefs() must also be " +
                            "returned by getChildNodeDefs().",
                            exists);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                fail("The array returned by " +
                        "getDeclaredChildNodeDefs() must not exceed " +
                        "the one returned by getChildNodeDefs()");
            }
        }
    }

    //-----------------------< internal >---------------------------------------

    /**
     * Returns the first descendant of <code>node</code> which defines primary
     * item
     *
     * @param node <code>Node</code> to start traversal.
     * @return first node with primary item
     */
    private Node locateNodeWithPrimaryItem(Node node)
            throws RepositoryException {

        try {
            node.getPrimaryItem();
            return node;
        } catch (ItemNotFoundException e) {

        }

        NodeIterator nodes = node.getNodes();
        while (nodes.hasNext()) {
            Node returnedNode = locateNodeWithPrimaryItem(nodes.nextNode());
            if (returnedNode != null) {
                return returnedNode;
            }
        }
        return null;
    }

    /**
     * Returns the first descendant of <code>node</code> without a primary item
     *
     * @param node
     * @return first node without primary item
     */
    private Node locateNodeWithoutPrimaryItem(Node node)
            throws RepositoryException {

        try {
            node.getPrimaryItem();
        } catch (ItemNotFoundException e) {
            return node;
        }

        NodeIterator nodes = node.getNodes();
        while (nodes.hasNext()) {
            Node returnedNode = this.locateNodeWithoutPrimaryItem(nodes.nextNode());
            if (returnedNode != null) {
                return returnedNode;
            }
        }
        return null;
    }

}