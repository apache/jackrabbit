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

/**
 * Tests <code>NodeType.canAddChildNode(String childNodeName)</code> returns true if
 * a node of name <code>childNodeName</code> could be added to a node of
 * type <code>NodeType</code>.
 *
 * @test
 * @sources CanAddChildNodeCallWithoutNodeTypeTest.java
 * @executeClass org.apache.jackrabbit.test.api.nodetype.CanAddChildNodeCallWithoutNodeTypeTest
 * @keywords level1
 */
public class CanAddChildNodeCallWithoutNodeTypeTest extends AbstractJCRTest {

    /**
     * The session we use for the tests
     */
    private Session session;

    /**
     * Sets up the fixture for the test cases.
     */
    protected void setUp() throws Exception {
        isReadOnly = true;
        super.setUp();

        session = helper.getReadOnlySession();
    }

    /**
     * Releases the session aquired in {@link #setUp()}.
     */
    protected void tearDown() throws Exception {
        if (session != null) {
            session.logout();
            session = null;
        }
        super.tearDown();
    }

    /**
     * Tests if <code>NodeType.canAddChildNode(String childNodeName)</code> returns
     * true if <code>NodeType</code> contains a <code>NodeDef</code>  named
     * <code>childNodeName</code> with a default primary type.
     */
    public void testDefinedWithDefault()
            throws NotExecutableException, RepositoryException {

        NodeDefinition nodeDef = NodeTypeUtil.locateChildNodeDef(session, true, true, false);

        if (nodeDef == null) {
            throw new NotExecutableException("No child node def with " +
                    "defaultPrimaryType found");
        }

        NodeType nodeType = nodeDef.getDeclaringNodeType();

        assertTrue("NodeType.canAddChildNode(String childNodeName) must return " +
                "true if child node def 'childNodeName' defines a defaultPrimaryType.",
                nodeType.canAddChildNode(nodeDef.getName()));
    }

    /**
     * Tests if <code>NodeType.canAddChildNode(String childNodeName)</code> returns
     * true if <code>NodeType</code> contains a <code>NodeDef</code>  named
     * <code>childNodeName</code> without a default primary type.
     */
    public void testDefinedWithoutDefault()
            throws NotExecutableException, RepositoryException {

        NodeDefinition nodeDef = NodeTypeUtil.locateChildNodeDef(session, true, false, false);

        if (nodeDef == null) {
            throw new NotExecutableException("No child node def without " +
                    "defaultPrimaryType found");
        }

        NodeType nodeType = nodeDef.getDeclaringNodeType();

        assertFalse("NodeType.canAddChildNode(String childNodeName) must return false " +
                "if child node def 'childNodeName' does not define a defaultPrimaryType.",
                nodeType.canAddChildNode(nodeDef.getName()));
    }

    /**
     * Tests if <code>NodeType.canAddChildNode(String childNodeName)</code> returns
     * true if <code>NodeType</code> nor does contain a <code>NodeDef</code>  named
     * <code>childNodeName</code> nor a residual definition.
     */
    public void testUndefined()
            throws NotExecutableException, RepositoryException {

        NodeDefinition nodeDef = NodeTypeUtil.locateChildNodeDef(session, false, true, false);

        if (nodeDef == null) {
            throw new NotExecutableException("No testable node type found.");
        }

        NodeType nodeType = nodeDef.getDeclaringNodeType();
        String undefinedName = NodeTypeUtil.getUndefinedChildNodeName(nodeType);

        assertFalse("NodeType.canAddChildNode(String childNodeName) must return " +
                "false if 'childNodeName' is a undefined child node def",
                nodeType.canAddChildNode(undefinedName));
    }

    /**
     * Tests if <code>NodeType.canAddChildNode(String childNodeName)</code> returns
     * true if <code>NodeType</code> contains a residual <code>NodeDef</code>
     * with a default primary type.
     */
    public void testResidualWithDefault()
            throws NotExecutableException, RepositoryException {

        NodeDefinition nodeDef = NodeTypeUtil.locateChildNodeDef(session, true, true, true);

        if (nodeDef == null) {
            throw new NotExecutableException("No residual child node def " +
                    "without a defaultPrimaryType found.");
        }

        NodeType nodeType = nodeDef.getDeclaringNodeType();
        String undefinedName = NodeTypeUtil.getUndefinedChildNodeName(nodeType);

        assertTrue("NodeType.canAddChildNode(String childNodeName) must return " +
                "true for a not defined childNodeName if NodeType has a residual child node " +
                "definition with a defaultPrimaryType",
                nodeType.canAddChildNode(undefinedName));
    }

    /**
     * Tests if <code>NodeType.canAddChildNode(String childNodeName)</code> returns
     * true if <code>NodeType</code> contains a residual <code>NodeDef</code>
     * without a default primary type.
     */
    public void testResidualWithoutDefault()
            throws NotExecutableException, RepositoryException {

        NodeDefinition nodeDef = NodeTypeUtil.locateChildNodeDef(session, true, false, true);

        if (nodeDef == null) {
            throw new NotExecutableException("No residual child node def " +
                    "with a defaultPrimaryType found.");
        }

        NodeType nodeType = nodeDef.getDeclaringNodeType();
        String undefinedName = NodeTypeUtil.getUndefinedChildNodeName(nodeType);

        assertFalse("NodeType.canAddChildNode(String childNodeName) must return " +
                "false for a not defiend childNodeName if NodeType has a " +
                "residual child node definition without a defaultPrimaryType",
                nodeType.canAddChildNode(undefinedName));
    }
}