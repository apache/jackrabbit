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

import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.Session;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

/**
 * Test of <code>NodeType.canSetProperty(String propertyName, Value[]
 * values)</code>
 *
 * @test
 * @sources CanSetPropertyMultipleTest.java
 * @executeClass org.apache.jackrabbit.test.api.nodetype.CanSetPropertyMultipleTest
 * @keywords level2
 */
public class CanSetPropertyMultipleTest extends AbstractJCRTest {
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
     * Tests if NodeType.canSetProperty(String propertyName, Value[] values)
     * returns false if the property is protected.
     */
    public void testReturnFalseBecauseIsProtected()
            throws NotExecutableException, RepositoryException {

        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(session, NodeTypeUtil.ANY_PROPERTY_TYPE, true, true, false, false);

        // will never happen since at least jcr:mixinTypes of nt:base accomplish the request
        if (propDef == null) {
            throw new NotExecutableException("No protected, multiple property def found");
        }

        NodeType nodeType = propDef.getDeclaringNodeType();
        Value value = NodeTypeUtil.getValueOfType(superuser, propDef.getRequiredType());
        Value values[] = new Value[] {value, value};

        assertFalse("canSetProperty(String propertyName, Value[] values) must " +
                "return true if the property is protected.",
                nodeType.canSetProperty(propDef.getName(), values));
    }


    /**
     * Tests if NodeType.canSetProperty(String propertyName, Value[] values)
     * returns false if the property is not multiple
     */
    public void testReturnFalseBecauseIsNotMultiple()
            throws NotExecutableException, RepositoryException {

        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(session, NodeTypeUtil.ANY_PROPERTY_TYPE, false, false, false, false);

        if (propDef == null) {
            throw new NotExecutableException("No not multiple, not protected property def found");
        }

        NodeType nodeType = propDef.getDeclaringNodeType();
        Value value = NodeTypeUtil.getValueOfType(superuser, propDef.getRequiredType());
        Value values[] = new Value[] {value};

        assertFalse("canSetProperty(String propertyName, Value[] values) must " +
                "return false if the property is not multiple",
                nodeType.canSetProperty(propDef.getName(), values));
    }

    /**
     * Tests if canSetProperty(String propertyName, Value[] values) where values
     * is null returns the same as canRemoveItem
     */
    public void testMultipleValuesNull()
            throws NotExecutableException, RepositoryException {

        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(session, NodeTypeUtil.ANY_PROPERTY_TYPE, true, false, false, false);

        if (propDef == null) {
            throw new NotExecutableException("No not protected, multiple property def found");
        }

        NodeType nodeType = propDef.getDeclaringNodeType();

        assertEquals("nodeType.canSetProperty(String propertyName, Value[] values) " +
                "where values is null must return the same result as " +
                "nodeType.canRemoveItem(String propertyName)",
                nodeType.canRemoveItem(propDef.getName()),
                nodeType.canSetProperty(propDef.getName(), (Value[]) null));
    }


}