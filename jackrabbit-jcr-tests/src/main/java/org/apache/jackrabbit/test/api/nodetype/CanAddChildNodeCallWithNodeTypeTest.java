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

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;

/**
 * Tests <code>NodeType.canAddChildNode(String childNodeName, String nodeTypeName)</code>
 * returns true if a node of name <code>childNodeName</code> and of node type
 * <code>childNodeName</code> could be added to a node of type <code>NodeType</code>.
 *
 * @test
 * @sources CanAddChildNodeCallWithNodeTypeTest.java
 * @executeClass org.apache.jackrabbit.test.api.nodetype.CanAddChildNodeCallWithNodeTypeTest
 * @keywords level1
 */
public class CanAddChildNodeCallWithNodeTypeTest extends AbstractJCRTest {
    /**
     * The session we use for the tests
     */
    private Session session;

    /**
     * The node type manager we use for the tests
     */
    private NodeTypeManager manager;

    /**
     * Sets up the fixture for the test cases.
     */
    protected void setUp() throws Exception {
        isReadOnly = true;
        super.setUp();

        session = helper.getReadOnlySession();
        manager = session.getWorkspace().getNodeTypeManager();
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
     * Tests if <code>NodeType.canAddChildNode(String childNodeName, String nodeTypeName)</code>
     * returns true if <code>childNodeName</code> and <code>nodeTypeName</code>
     * match the <code>NodeDef</code>.
     */
    public void testDefinedAndLegalType()
            throws NotExecutableException, RepositoryException {

        NodeDefinition nodeDef = NodeTypeUtil.locateChildNodeDef(session, false, false, false);

        if (nodeDef == null) {
            throw new NotExecutableException("No child node def with " +
                    "defaultPrimaryType found");
        }

        NodeType nodeType = nodeDef.getDeclaringNodeType();
        String childNodeName = nodeDef.getName();
        String nodeTypeName = nodeDef.getRequiredPrimaryTypes()[0].getName();

        assertTrue("NodeType.canAddChildNode(String childNodeName, String nodeTypeName) " +
                "must return true if childNodeName and nodeTypeName match the " +
                "child node def of NodeType.",
                nodeType.canAddChildNode(childNodeName, nodeTypeName));
    }

    /**
     * Tests if <code>NodeType.canAddChildNode(String childNodeName, String nodeTypeName)</code>
     * returns false if <code>childNodeName</code> does and <code>nodeTypeName</code>
     * does not match the <code>NodeDef</code>.
     */
    public void testDefinedAndIllegalType()
            throws NotExecutableException, RepositoryException {

        NodeDefinition nodeDef = NodeTypeUtil.locateChildNodeDef(session, false, false, false);

        if (nodeDef == null) {
            throw new NotExecutableException("No testable node type found.");
        }

        NodeType nodeType = nodeDef.getDeclaringNodeType();
        String childNodeName = nodeDef.getName();

        String legalType = nodeDef.getRequiredPrimaryTypes()[0].getName();
        String illegalType = NodeTypeUtil.getIllegalChildNodeType(manager, legalType);
        if (illegalType == null) {
            throw new NotExecutableException("No illegal node type name found");
        }

        assertFalse("NodeType.canAddChildNode(String childNodeName, String nodeTypeName) " +
                "must return false if childNodeName does and nodeTypeName does not " +
                "match the child node def of NodeType.",
                nodeType.canAddChildNode(childNodeName, illegalType));
    }

    /**
     * Tests if <code>NodeType.canAddChildNode(String childNodeName, String nodeTypeName)</code>
     * returns false if <code>childNodeName</code> does not match the <code>NodeDef</code>.
     */
    public void testUndefined()
            throws NotExecutableException, RepositoryException {

        NodeDefinition nodeDef = NodeTypeUtil.locateChildNodeDef(session, false, true, false);

        if (nodeDef == null) {
            throw new NotExecutableException("No testable node type found.");
        }

        String type = nodeDef.getRequiredPrimaryTypes()[0].getName();
        NodeType nodeType = nodeDef.getDeclaringNodeType();
        String undefinedName = NodeTypeUtil.getUndefinedChildNodeName(nodeType);

        assertFalse("NodeType.canAddChildNode(String childNodeName, String nodeTypeName) " +
                "must return false if childNodeName does not match the " +
                "child node def of NodeType.",
                nodeType.canAddChildNode(undefinedName, type));
    }

    /**
     * Tests if <code>NodeType.canAddChildNode(String childNodeName, String nodeTypeName)</code>
     * returns true if <code>childNodeName</code> does not match the <code>NodeDef</code>
     * but <code>nodeTypeName</code> matches the node type of a residual <code>NodeDef</code>.
     */
    public void testResidualAndLegalType()
            throws NotExecutableException, RepositoryException {

        NodeDefinition nodeDef = NodeTypeUtil.locateChildNodeDef(session, false, false, true);

        if (nodeDef == null) {
            throw new NotExecutableException("No testable residual child node def.");
        }

        String type = nodeDef.getRequiredPrimaryTypes()[0].getName();
        NodeType nodeType = nodeDef.getDeclaringNodeType();
        String undefinedName = NodeTypeUtil.getUndefinedChildNodeName(nodeType);

        assertTrue("NodeType.canAddChildNode(String childNodeName, String nodeTypeName) " +
                "must return true for a not defined childNodeName if nodeTypeName " +
                "matches the type of a residual child node def",
                nodeType.canAddChildNode(undefinedName, type));
    }

    /**
     * Tests if <code>NodeType.canAddChildNode(String childNodeName, String nodeTypeName)</code>
     * returns false if <code>childNodeName</code> does not match the <code>NodeDef</code>
     * and <code>nodeTypeName</code> does not matches the node type of a residual
     * <code>NodeDef</code>.
     */
    public void testResidualAndIllegalType()
            throws NotExecutableException, RepositoryException {

        NodeDefinition nodeDef = NodeTypeUtil.locateChildNodeDef(session, false, false, true);

        if (nodeDef == null) {
            throw new NotExecutableException("No testable residual child node def.");
        }

        NodeType nodeType = nodeDef.getDeclaringNodeType();
        String undefinedName = NodeTypeUtil.getUndefinedChildNodeName(nodeType);

        String legalType = nodeDef.getRequiredPrimaryTypes()[0].getName();
        String illegalType = NodeTypeUtil.getIllegalChildNodeType(manager, legalType);
        if (illegalType == null) {
            throw new NotExecutableException("No illegal node type name found");
        }

        assertFalse("NodeType.canAddChildNode(String childNodeName, String nodeTypeName) " +
                "must return false for a not defined childNodeName if nodeTypeName " +
                "does not matches the type of a residual child node def",
                nodeType.canAddChildNode(undefinedName, illegalType));
    }
}