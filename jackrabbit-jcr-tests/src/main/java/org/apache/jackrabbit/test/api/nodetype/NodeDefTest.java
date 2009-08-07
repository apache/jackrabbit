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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;

/**
 * Tests if node definitions are respected in node instances in the workspace.
 *
 * @test
 * @sources NodeDefTest.java
 * @executeClass org.apache.jackrabbit.test.api.nodetype.NodeDefTest
 * @keywords level1
 */
public class NodeDefTest extends AbstractJCRTest {

    /**
     * The session we use for the tests
     */
    private Session session;

    /**
     * The node type manager of the session
     */
    private NodeTypeManager manager;

    /**
     * If <code>true</code> indicates that the test found a mandatory node
     */
    private boolean foundMandatoryNode = false;

    /**
     * Sets up the fixture for the test cases.
     */
    protected void setUp() throws Exception {
        isReadOnly = true;
        super.setUp();

        session = helper.getReadOnlySession();
        manager = session.getWorkspace().getNodeTypeManager();
        // re-fetch testRootNode with read-only session
        testRootNode = (Node) session.getItem(testRoot);
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
        super.tearDown();
    }

    /**
     * Test getDeclaringNodeType() returns the node type which is defining the
     * requested child node def. Test runs for all existing node types.
     */
    public void testGetDeclaringNodeType()
            throws RepositoryException {

        NodeTypeIterator types = manager.getAllNodeTypes();
        // loop all node types
        while (types.hasNext()) {
            NodeType currentType = types.nextNodeType();
            NodeDefinition defsOfCurrentType[] =
                    currentType.getChildNodeDefinitions();

            // loop all child node defs of each node type
            for (int i = 0; i < defsOfCurrentType.length; i++) {
                NodeDefinition def = defsOfCurrentType[i];
                NodeType type = def.getDeclaringNodeType();

                // check if def is part of the child node defs of the
                // declaring node type
                NodeDefinition defs[] = type.getChildNodeDefinitions();
                boolean hasType = false;
                for (int j = 0; j < defs.length; j++) {
                    if (defs[j].getName().equals(def.getName())) {
                        hasType = true;
                        break;
                    }
                }
                assertTrue("getDeclaringNodeType() must return the node " +
                        "which defines the corresponding child node def.",
                        hasType);
            }
        }
    }


    /**
     * Tests if auto create nodes are not a residual set definition (getName()
     * does not return "*")
     */
    public void testIsAutoCreate()
            throws RepositoryException {

        NodeTypeIterator types = manager.getAllNodeTypes();
        // loop all node types
        while (types.hasNext()) {
            NodeType type = types.nextNodeType();
            NodeDefinition defs[] = type.getChildNodeDefinitions();
            for (int i = 0; i < defs.length; i++) {
                if (defs[i].isAutoCreated()) {
                    assertFalse("An auto create node must not be a " +
                            "residual set definition.",
                            defs[i].getName().equals("*"));
                }
            }
        }
    }


    /**
     * This test checks if item definitions with mandatory constraints are
     * respected.
     * <p/>
     * If the default workspace does not contain a node with a node type
     * definition that specifies a mandatory child node a {@link
     * org.apache.jackrabbit.test.NotExecutableException} is thrown.
     */
    public void testIsMandatory() throws RepositoryException, NotExecutableException {
        traverse(testRootNode);
        if (!foundMandatoryNode) {
            throw new NotExecutableException("Workspace does not contain any node with a mandatory child node definition");
        }
    }


    /**
     * Tests if getRequiredPrimaryTypes() does not return an empty array. Test
     * runs for all existing node types.
     */
    public void testGetRequiredPrimaryTypes()
            throws RepositoryException {

        NodeTypeIterator types = manager.getAllNodeTypes();
        // loop all node types
        while (types.hasNext()) {
            NodeType type = types.nextNodeType();
            NodeDefinition defs[] = type.getChildNodeDefinitions();

            for (int i = 0; i < defs.length; i++) {
                assertTrue("getRequiredPrimaryTypes() must never return an " +
                        "empty array.",
                        defs[i].getRequiredPrimaryTypes().length > 0);
            }
        }
    }


    /**
     * Tests if the default primary type is of the same or a sub node type as the
     * the required primary types. Test runs for all existing node types.
     */
    public void testGetDefaultPrimaryTypes()
            throws RepositoryException {

        NodeTypeIterator types = manager.getAllNodeTypes();
        // loop all node types
        while (types.hasNext()) {
            NodeType type = types.nextNodeType();
            NodeDefinition defs[] = type.getChildNodeDefinitions();

            for (int i = 0; i < defs.length; i++) {

                NodeDefinition def = defs[i];
                NodeType defaultType = def.getDefaultPrimaryType();
                if (defaultType != null) {

                    NodeType requiredTypes[] =
                            def.getRequiredPrimaryTypes();

                    for (int j = 0; j < requiredTypes.length; j++) {
                        NodeType requiredType = requiredTypes[j];

                        boolean isSubType = compareWithRequiredType(requiredType,
                                defaultType);

                        assertTrue("The NodeType returned by " +
                                "getDefaultPrimaryType or one of its " +
                                "supertypes must match all NodeTypes " +
                                "returned by getRequiredPrimaryTypes()",
                                isSubType);
                    }
                }
            }
        }
    }


    //-----------------------< internal >---------------------------------------

    /**
     * Traverses the node hierarchy and applies
     * {@link #checkMandatoryConstraint(javax.jcr.Node, javax.jcr.nodetype.NodeType)}
     * to all descendant nodes of <code>parentNode</code>.
     */
    private void traverse(Node parentNode)
            throws RepositoryException {

        NodeIterator nodes = parentNode.getNodes();
        while (nodes.hasNext()) {
            Node node = nodes.nextNode();

            NodeType primaryType = node.getPrimaryNodeType();
            checkMandatoryConstraint(node, primaryType);

            NodeType mixins[] = node.getMixinNodeTypes();
            for (int i = 0; i < mixins.length; i++) {
                checkMandatoryConstraint(node, mixins[i]);
            }

            traverse(node);
        }
    }


    /**
     * Checks if mandatory node definitions are respected.
     */
    private void checkMandatoryConstraint(Node node, NodeType type)
            throws RepositoryException {

        // test if node contains all mandatory nodes of current type
        NodeDefinition nodeDefs[] = type.getChildNodeDefinitions();
        for (int i = 0; i < nodeDefs.length; i++) {
            NodeDefinition nodeDef = nodeDefs[i];
            if (nodeDef.isMandatory()) {
                foundMandatoryNode = true;
                try {
                    node.getNode(nodeDef.getName());
                } catch (PathNotFoundException e) {
                    fail("Mandatory child " + nodeDef.getName() + " for " +
                            node.getPath() + " does not exist.");
                }
            }
        }
    }


    /**
     * Returns true if defaultType or one of its supertypes is of the same
     * NodeType as requiredType.
     *
     * @param requiredType one of the required primary types of a NodeDef
     * @param defaultType  the default primary type of a NodeDef
     */
    private boolean compareWithRequiredType(NodeType requiredType,
                                            NodeType defaultType) {

        // if (defaultType == requiredType) return true;
        // rather use:
        if (defaultType.getName().equals(requiredType.getName())) {
            return true;
        }

        NodeType superTypes[] = defaultType.getSupertypes();
        for (int i = 0; i < superTypes.length; i++) {
            // if (superTypes[i] == requiredType) return true;
            // rather use:
            if (superTypes[i].getName().equals(requiredType.getName())) {
                return true;
            }
        }
        return false;
    }


}