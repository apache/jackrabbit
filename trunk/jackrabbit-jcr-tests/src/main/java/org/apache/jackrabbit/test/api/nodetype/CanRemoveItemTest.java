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
 * Tests that {@link NodeType#canRemoveItem(String)} returns true
 * node or property is removable (same for {@link NodeType#canRemoveNode(String)}
 * and {@link NodeType#canRemoveProperty(String)}).
 *
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

        session = getHelper().getReadOnlySession();
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
     * Tests that {@link NodeType#canRemoveItem(String)} and
     * {@link NodeType#canRemoveProperty(String)} return true
     * if the specified property is not a protected nor a mandatory
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

        assertTrue("NodeType.canRemoveItem(String itemName) must return true " +
                "if itemName is not a protected nor a mandatory property def.",
                type.canRemoveItem(propDef.getName()));

        assertTrue("NodeType.canRemoveProperty(String propertyName) must return true " +
                "if propertyName is not a protected nor a mandatory property def.",
                type.canRemoveProperty(propDef.getName()));
    }

    /**
     * Tests if {@link NodeType#canRemoveItem(String)} and
     * {@link NodeType#canRemoveProperty(String)} return false
     * if the specified property is a protected property.
     */
    public void testProtectedProperty()
            throws NotExecutableException, RepositoryException {

        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(session, true, false);

        if (propDef == null) {
            throw new NotExecutableException("No protected property def found.");
        }

        NodeType type = propDef.getDeclaringNodeType();

        assertFalse("NodeType.canRemoveItem(String itemName) must return false " +
                "if itemName is a protected property def.",
                type.canRemoveItem(propDef.getName()));

        assertFalse("NodeType.canRemoveProperty(String propertyName) must return false " +
                "if propertyName is a protected property def.",
                type.canRemoveProperty(propDef.getName()));
    }

    /**
     * Tests if {@link NodeType#canRemoveItem(String)} and
     * {@link NodeType#canRemoveProperty(String)} return false
     * if the specified property is a mandatory property.
     */
    public void testMandatoryProperty()
            throws NotExecutableException, RepositoryException {

        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(session, false, true);

        if (propDef == null) {
            throw new NotExecutableException("No mandatory property def found.");
        }

        NodeType type = propDef.getDeclaringNodeType();

        assertFalse("NodeType.canRemoveItem(String itemName) must return false " +
                "if itemName is a mandatory property def.",
                type.canRemoveItem(propDef.getName()));

        assertFalse("NodeType.canRemoveProperty(String propertyName) must return false " +
                "if propertyName is a mandatory property def.",
                type.canRemoveProperty(propDef.getName()));
    }

    /**
     * Tests if {@link NodeType#canRemoveItem(String)} and
     * {@link NodeType#canRemoveNode(String)} return true
     * if the specified node is not a protected nor a mandatory
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

        assertTrue("NodeType.canRemoveItem(String itemName) must return true " +
                "if itemName is not a protected nor a mandatory child node def.",
                type.canRemoveItem(nodeDef.getName()));

        assertTrue("NodeType.canRemoveNode(String nodeName) must return true " +
                "if nodeName is not a protected nor a mandatory child node def.",
                type.canRemoveNode(nodeDef.getName()));
}

    /**
     * Tests if {@link NodeType#canRemoveItem(String)} and
     * {@link NodeType#canRemoveNode(String)} return 
     * false if the specified node is a protected child node.
     */
    public void testProtectedChildNode()
            throws NotExecutableException, RepositoryException {

        NodeDefinition nodeDef =
                NodeTypeUtil.locateChildNodeDef(session, true, false);

        if (nodeDef == null) {
            throw new NotExecutableException("No mandatory property def found.");
        }

        NodeType type = nodeDef.getDeclaringNodeType();

        assertFalse("NodeType.canRemoveItem(String itemName) must return false " +
                "if itemName is a protected child node def.",
                type.canRemoveItem(nodeDef.getName()));

        assertFalse("NodeType.canRemoveNode(String nodeName) must return false " +
                "if nodeName is a protected child node def.",
                type.canRemoveNode(nodeDef.getName()));
}

    /**
     * Tests if {@link NodeType#canRemoveItem(String)} and
     * {@link NodeType#canRemoveNode(String)} return 
     * false if the specified node is a mandatory child node.
     */
    public void testMandatoryChildNode()
            throws NotExecutableException, RepositoryException {

        NodeDefinition nodeDef =
                NodeTypeUtil.locateChildNodeDef(session, true, false);

        if (nodeDef == null) {
            throw new NotExecutableException("No mandatory property def found.");
        }

        NodeType type = nodeDef.getDeclaringNodeType();

        assertFalse("NodeType.canRemoveItem(String itemName) must return false " +
                "if itemName is a mandatory child node def.",
                type.canRemoveItem(nodeDef.getName()));
    
        assertFalse("NodeType.canRemoveNode(String nodeName) must return false " +
                "if nodeName is a mandatory child node def.",
                type.canRemoveNode(nodeDef.getName()));
    }
}
