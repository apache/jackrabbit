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

import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.Session;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

/**
 * Test of <code>NodeType.canSetProperty(String propertyName, Value
 * value)</code>
 *
 * @test
 * @sources CanSetPropertyTest.java
 * @executeClass org.apache.jackrabbit.test.api.nodetype.CanSetPropertyTest
 * @keywords level2
 */
public class CanSetPropertyTest extends AbstractJCRTest {
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
     * Tests if NodeType.canSetProperty(String propertyName, Value value)
     * returns false if the property is protected.
     */
    public void testReturnFalseBecauseIsProtected()
            throws NotExecutableException, RepositoryException {

        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(session, NodeTypeUtil.ANY_PROPERTY_TYPE, false, true, false, false);

        // will never happen since at least jcr:primaryType of nt:base accomplish the request
        if (propDef == null) {
            throw new NotExecutableException("No protected property def found.");
        }

        NodeType nodeType = propDef.getDeclaringNodeType();
        Value value = NodeTypeUtil.getValueOfType(superuser, propDef.getRequiredType());

        assertFalse("canSetProperty(String propertyName, Value value) must " +
                "return false if the property is protected.",
                nodeType.canSetProperty(propDef.getName(), value));
    }


    /**
     * Tests if NodeType.canSetProperty(String propertyName, Value value)
     * returns false if the property is multiple
     */
    public void testReturnFalseBecauseIsMultiple()
            throws NotExecutableException, RepositoryException {

        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(session, NodeTypeUtil.ANY_PROPERTY_TYPE, true, false, false, false);

        if (propDef == null) {
            throw new NotExecutableException("No multiple, not protected property def found.");
        }

        NodeType nodeType = propDef.getDeclaringNodeType();
        Value value = NodeTypeUtil.getValueOfType(superuser, propDef.getRequiredType());

        assertFalse("canSetProperty(String propertyName, Value value) must " +
                "return false if the property is multiple.",
                nodeType.canSetProperty(propDef.getName(), value));
    }

    /**
     * Tests if canSetProperty(String propertyName, Value value) where value is
     * null returns the same as canRemoveItem
     */
    public void testValueNull()
            throws NotExecutableException, RepositoryException {

        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(session, NodeTypeUtil.ANY_PROPERTY_TYPE, false, false, false, false);

        if (propDef == null) {
            throw new NotExecutableException("No not protected property def found.");
        }

        NodeType nodeType = propDef.getDeclaringNodeType();

        assertEquals("nodeType.canSetProperty(String propertyName, Value value) " +
                "where value is null must return the same result as " +
                "nodeType.canRemoveItem(String propertyName).",
                nodeType.canRemoveItem(propDef.getName()),
                nodeType.canSetProperty(propDef.getName(), (Value) null));
    }

}