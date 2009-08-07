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
import javax.jcr.nodetype.PropertyDefinition;

/**
 * Tests <code>NodeType.canRemoveItem(String itemName)</code> returns true if a
 * node or property is removable
 *
 * @test
 * @sources CanRemoveItemTest.java
 * @executeClass org.apache.jackrabbit.test.api.nodetype.CanRemoveItemTest
 * @keywords level1
 */
public class CanRemoveItemTest extends AbstractJCRTest {

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
     * Tests if <code>NodeType.canRemoveItem(String itemName)</code> removes
     * true if <code>itemName</code> is nor a protected nor a mandatory
     * property.
     */
    public void testRemovableProperty()
            throws NotExecutableException, RepositoryException {

        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(session, false, false);

        if (propDef == null) {
            throw new NotExecutableException("No mandatory property def found.");
        }

        NodeType type = propDef.getDeclaringNodeType();

        assertTrue("NodeType.canRemoveIten(String itemName) must return true " +
                "if itemName is nor a protected nor a mandatory property def.",
                type.canRemoveItem(propDef.getName()));
    }

    /**
     * Tests if <code>NodeType.canRemoveItem(String itemName)</code> removes
     * false if <code>itemName</code> is a protected property.
     */
    public void testProtectedProperty()
            throws NotExecutableException, RepositoryException {

        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(session, true, false);

        if (propDef == null) {
            throw new NotExecutableException("No protected property def found.");
        }

        NodeType type = propDef.getDeclaringNodeType();

        assertFalse("NodeType.canRemoveIten(String itemName) must return false " +
                "if itemName is a protected property def.",
                type.canRemoveItem(propDef.getName()));
    }

    /**
     * Tests if <code>NodeType.canRemoveItem(String itemName)</code> removes
     * false if <code>itemName</code> is a mandatory property.
     */
    public void testMandatoryProperty()
            throws NotExecutableException, RepositoryException {

        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(session, false, true);

        if (propDef == null) {
            throw new NotExecutableException("No mandatory property def found.");
        }

        NodeType type = propDef.getDeclaringNodeType();

        assertFalse("NodeType.canRemoveIten(String itemName) must return false " +
                "if itemName is a mandatory property def.",
                type.canRemoveItem(propDef.getName()));
    }

    /**
     * Tests if <code>NodeType.canRemoveItem(String itemName)</code> removes
     * true if <code>itemName</code> is nor a protected nor a mandatory
     * child node.
     */
    public void testRemovableChildNode()
            throws NotExecutableException, RepositoryException {

        NodeDefinition nodeDef =
                NodeTypeUtil.locateChildNodeDef(session, false, false);

        if (nodeDef == null) {
            throw new NotExecutableException("No mandatory property def found.");
        }

        NodeType type = nodeDef.getDeclaringNodeType();

        assertTrue("NodeType.canRemoveIten(String itemName) must return true " +
                "if itemName is nor a protected nor a mandatory child node def.",
                type.canRemoveItem(nodeDef.getName()));
    }

    /**
     * Tests if <code>NodeType.canRemoveItem(String itemName)</code> removes
     * false if <code>itemName</code> is a protected child node.
     */
    public void testProtectedChildNode()
            throws NotExecutableException, RepositoryException {

        NodeDefinition nodeDef =
                NodeTypeUtil.locateChildNodeDef(session, true, false);

        if (nodeDef == null) {
            throw new NotExecutableException("No mandatory property def found.");
        }

        NodeType type = nodeDef.getDeclaringNodeType();

        assertFalse("NodeType.canRemoveIten(String itemName) must return false " +
                "if itemName is a protected child node def.",
                type.canRemoveItem(nodeDef.getName()));
    }

    /**
     * Tests if <code>NodeType.canRemoveItem(String itemName)</code> removes
     * false if <code>itemName</code> is a mandatory child node.
     */
    public void testMandatoryChildNode()
            throws NotExecutableException, RepositoryException {

        NodeDefinition nodeDef =
                NodeTypeUtil.locateChildNodeDef(session, true, false);

        if (nodeDef == null) {
            throw new NotExecutableException("No mandatory property def found.");
        }

        NodeType type = nodeDef.getDeclaringNodeType();

        assertFalse("NodeType.canRemoveIten(String itemName) must return false " +
                "if itemName is a mandatory child node def.",
                type.canRemoveItem(nodeDef.getName()));
    }
}
