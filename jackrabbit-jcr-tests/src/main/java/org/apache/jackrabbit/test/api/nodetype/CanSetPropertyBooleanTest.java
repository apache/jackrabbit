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

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import java.text.ParseException;

/**
 * Test of <code>NodeType.canSetProperty(String propertyName, Value
 * value)</code> and <code>NodeType.canSetProperty(String propertyName, Value[]
 * values)</code> where property is of type Boolean.
 *
 * @test
 * @sources CanSetPropertyBooleanTest.java
 * @executeClass org.apache.jackrabbit.test.api.nodetype.CanSetPropertyBooleanTest
 * @keywords level2
 */
public class CanSetPropertyBooleanTest extends AbstractJCRTest {
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
     * returns true if value and its type are convertible to BooleanValue.
     */
    public void testConversions()
            throws NotExecutableException, RepositoryException {

        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(session, PropertyType.BOOLEAN, false, false, false, false);

        if (propDef == null) {
            throw new NotExecutableException("No boolean property def that meets the " +
                    "requirements of the test has been found");
        }

        NodeType nodeType = propDef.getDeclaringNodeType();


        Value stringValue = NodeTypeUtil.getValueOfType(session, PropertyType.STRING);
        assertTrue("canSetProperty(String propertyName, Value value) must return " +
                "true if the property is of type Boolean and value is a StringValue",
                nodeType.canSetProperty(propDef.getName(), stringValue));

        Value binaryValue = NodeTypeUtil.getValueOfType(session, PropertyType.BINARY);
        assertTrue("canSetProperty(String propertyName, Value value) must return " +
                "true if the property is of type Boolean and value is a BinaryValue" +
                "in UTF-8",
                nodeType.canSetProperty(propDef.getName(), binaryValue));

        Value dateValue = NodeTypeUtil.getValueOfType(session, PropertyType.DATE);
        assertFalse("canSetProperty(String propertyName, Value value) must return " +
                "false if the property is of type Boolean and value is a DateValue",
                nodeType.canSetProperty(propDef.getName(), dateValue));

        Value doubleValue = NodeTypeUtil.getValueOfType(session, PropertyType.DOUBLE);
        assertFalse("canSetProperty(String propertyName, Value value) must return " +
                "false if the property is of type Boolean and value is a DoubleValue",
                nodeType.canSetProperty(propDef.getName(), doubleValue));

        Value longValue = NodeTypeUtil.getValueOfType(session, PropertyType.LONG);
        assertFalse("canSetProperty(String propertyName, Value value) must return " +
                "false if the property is of type Boolean and value is a LongValue",
                nodeType.canSetProperty(propDef.getName(), longValue));

        Value booleanValue = NodeTypeUtil.getValueOfType(session, PropertyType.BOOLEAN);
        assertTrue("canSetProperty(String propertyName, Value value) must return " +
                "true if the property is of type Boolean and value is a BooleanValue",
                nodeType.canSetProperty(propDef.getName(), booleanValue));

        Value nameValue = NodeTypeUtil.getValueOfType(session, PropertyType.NAME);
        assertFalse("canSetProperty(String propertyName, Value value) must return " +
                "false if the property is of type Boolean and value is a NameValue",
                nodeType.canSetProperty(propDef.getName(), nameValue));

        Value pathValue = NodeTypeUtil.getValueOfType(session, PropertyType.PATH);
        assertFalse("canSetProperty(String propertyName, Value value) must return " +
                "false if the property is of type Boolean and value is a PathValue",
                nodeType.canSetProperty(propDef.getName(), pathValue));
    }

    /**
     * Tests if NodeType.canSetProperty(String propertyName, Value[] values)
     * returns true if all values and its types are convertible to
     * BooleanValue.
     */
    public void testConversionsMultiple()
            throws NotExecutableException, RepositoryException {

        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(session, PropertyType.BOOLEAN, true, false, false, false);

        if (propDef == null) {
            throw new NotExecutableException("No multiple boolean property def that meets the " +
                    "requirements of the test has been found");
        }

        NodeType nodeType = propDef.getDeclaringNodeType();


        Value booleanValue = NodeTypeUtil.getValueOfType(session, PropertyType.BOOLEAN);

        Value stringValue = NodeTypeUtil.getValueOfType(session, PropertyType.STRING);
        Value stringValues[] = new Value[] {stringValue};
        assertTrue("canSetProperty(String propertyName, Value[] values) must return " +
                "true if the property is of type Boolean and values are of type StringValue",
                nodeType.canSetProperty(propDef.getName(), stringValues));

        Value binaryValue = NodeTypeUtil.getValueOfType(session, PropertyType.BINARY);
        Value binaryValues[] = new Value[] {binaryValue};
        assertTrue("canSetProperty(String propertyName, Value[] values) must return " +
                "true if the property is of type Boolean and values are of type BinaryValue",
                nodeType.canSetProperty(propDef.getName(), binaryValues));

        Value dateValue = NodeTypeUtil.getValueOfType(session, PropertyType.DATE);
        Value dateValues[] = new Value[] {booleanValue, dateValue};
        assertFalse("canSetProperty(String propertyName, Value[] values) must return " +
                "false if the property is of type Boolean and values are of type DateValue",
                nodeType.canSetProperty(propDef.getName(), dateValues));

        Value doubleValue = NodeTypeUtil.getValueOfType(session, PropertyType.DOUBLE);
        Value doubleValues[] = new Value[] {booleanValue, doubleValue};
        assertFalse("canSetProperty(String propertyName, Value[] values) must return " +
                "false if the property is of type Boolean and values are of type DoubleValue",
                nodeType.canSetProperty(propDef.getName(), doubleValues));

        Value longValue = NodeTypeUtil.getValueOfType(session, PropertyType.LONG);
        Value longValues[] = new Value[] {booleanValue, longValue};
        assertFalse("canSetProperty(String propertyName, Value[] values) must return " +
                "false if the property is of type Boolean and values are of type LongValue",
                nodeType.canSetProperty(propDef.getName(), longValues));

        Value booleanValues[] = new Value[] {booleanValue};
        assertTrue("canSetProperty(String propertyName, Value[] values) must return " +
                "true if the property is of type Boolean and values are of type BooleanValue",
                nodeType.canSetProperty(propDef.getName(), booleanValues));

        Value nameValue = NodeTypeUtil.getValueOfType(session, PropertyType.NAME);
        Value nameValues[] = new Value[] {booleanValue, nameValue};
        assertFalse("canSetProperty(String propertyName, Value[] values) must return " +
                "false if the property is of type Boolean and values are of type NameValue",
                nodeType.canSetProperty(propDef.getName(), nameValues));

        Value pathValue = NodeTypeUtil.getValueOfType(session, PropertyType.PATH);
        Value pathValues[] = new Value[] {booleanValue, pathValue};
        assertFalse("canSetProperty(String propertyName, Value[] values) must return " +
                "false if the property is of type Boolean and values are of type PathValue",
                nodeType.canSetProperty(propDef.getName(), pathValues));
    }

    /**
     * Tests if canSetProperty(String propertyName, Value value) returns false
     * if value does not satisfy the value constraints of the property def
     */
    public void testValueConstraintNotSatisfied()
            throws NotExecutableException, ParseException, RepositoryException {

        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(session, PropertyType.BOOLEAN, false, false, true, false);

        if (propDef == null) {
            throw new NotExecutableException("No boolean property def with " +
                    "testable value constraints has been found");
        }

        Value value = NodeTypeUtil.getValueAccordingToValueConstraints(session, propDef, false);
        if (value == null) {
            throw new NotExecutableException("No boolean property def with " +
                    "testable value constraints has been found");
        }

        NodeType nodeType = propDef.getDeclaringNodeType();

        assertFalse("canSetProperty(String propertyName, Value value) must " +
                "return false if value does not match the value constraints.",
                nodeType.canSetProperty(propDef.getName(), value));
    }

    /**
     * Tests if canSetProperty(String propertyName, Value[] values) returns
     * false if values do not satisfy the value constraints of the property def
     */
    public void testValueConstraintNotSatisfiedMultiple()
            throws NotExecutableException, ParseException, RepositoryException {

        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(session, PropertyType.BOOLEAN, true, false, true, false);

        if (propDef == null) {
            throw new NotExecutableException("No multiple boolean property def with " +
                    "testable value constraints has been found");
        }

        Value value = NodeTypeUtil.getValueAccordingToValueConstraints(session, propDef, false);
        if (value == null) {
            throw new NotExecutableException("No multiple boolean property def with " +
                    "testable value constraints has been found");
        }

        NodeType nodeType = propDef.getDeclaringNodeType();
        Value values[] = new Value[] {value};

        assertFalse("canSetProperty(String propertyName, Value[] values) must " +
                "return false if values do not match the value constraints.",
                nodeType.canSetProperty(propDef.getName(), values));
    }
}