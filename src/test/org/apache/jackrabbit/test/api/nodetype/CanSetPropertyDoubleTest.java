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
package org.apache.jackrabbit.test.api.nodetype;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.nodetype.PropertyDef;
import javax.jcr.nodetype.NodeType;
import javax.jcr.Session;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.StringValue;
import javax.jcr.BinaryValue;
import java.text.ParseException;

/**
 * Test of <code>NodeType.canSetProperty(String propertyName, Value
 * value)</code> and <code>NodeType.canSetProperty(String propertyName, Value[]
 * values)</code> where property is of type Double.
 *
 * @test
 * @sources CanSetPropertyDoubleTest.java
 * @executeClass org.apache.jackrabbit.test.api.nodetype.CanSetPropertyDoubleTest
 * @keywords level1
 */
public class CanSetPropertyDoubleTest extends AbstractJCRTest {
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
        }
        super.tearDown();
    }


    /**
     * Tests if NodeType.canSetProperty(String propertyName, Value value)
     * returns true if value and its type are convertible to DoubleValue.
     */
    public void testConversions()
            throws NotExecutableException, RepositoryException {

        PropertyDef propDef =
                NodeTypeUtil.locatePropertyDef(session, PropertyType.DOUBLE, false, false, false, false);

        if (propDef == null) {
            throw new NotExecutableException("No double property def that meets the " +
                    "requirements of the test has been found");
        }

        NodeType nodeType = propDef.getDeclaringNodeType();


        Value anyStringValue = NodeTypeUtil.getValueOfType(PropertyType.STRING);
        assertFalse("canSetProperty(String propertyName, Value value) must return " +
                "false if the property is of type Double and value is a StringValue " +
                "that is not convertible to a DoubleValue",
                nodeType.canSetProperty(propDef.getName(), anyStringValue));

        Value doubleStringValue =
                new StringValue(NodeTypeUtil.getValueOfType(PropertyType.DOUBLE).getString());
        assertTrue("canSetProperty(String propertyName, Value value) must return " +
                "true if the property is of type Double and value is a StringValue " +
                "that is convertible to a DoubleValue",
                nodeType.canSetProperty(propDef.getName(), doubleStringValue));

        Value anyBinaryValue = NodeTypeUtil.getValueOfType(PropertyType.BINARY);
        assertFalse("canSetProperty(String propertyName, Value value) must return " +
                "false if the property is of type Double and value is a UTF-8 " +
                "BinaryValue that is not convertible to a DoubleValue",
                nodeType.canSetProperty(propDef.getName(), anyBinaryValue));

        Value doubleBinaryValue =
                new BinaryValue(NodeTypeUtil.getValueOfType(PropertyType.DOUBLE).getString());
        assertTrue("canSetProperty(String propertyName, Value value) must return " +
                "true if the property is of type Double and value is a UTF-8 " +
                "BinaryValue that is convertible to a DoubleValue",
                nodeType.canSetProperty(propDef.getName(), doubleBinaryValue));

        Value dateValue = NodeTypeUtil.getValueOfType(PropertyType.DATE);
        assertTrue("canSetProperty(String propertyName, Value value) must return " +
                "true if the property is of type Double and value is a DateValue",
                nodeType.canSetProperty(propDef.getName(), dateValue));

        Value doubleValue = NodeTypeUtil.getValueOfType(PropertyType.DOUBLE);
        assertTrue("canSetProperty(String propertyName, Value value) must return " +
                "true if the property is of type Double and value is a DoubleValue",
                nodeType.canSetProperty(propDef.getName(), doubleValue));

        Value longValue = NodeTypeUtil.getValueOfType(PropertyType.LONG);
        assertTrue("canSetProperty(String propertyName, Value value) must return " +
                "true if the property is of type Double and value is a LongValue",
                nodeType.canSetProperty(propDef.getName(), longValue));

        Value booleanValue = NodeTypeUtil.getValueOfType(PropertyType.BOOLEAN);
        assertFalse("canSetProperty(String propertyName, Value value) must return " +
                "false if the property is of type Double and value is a BooleanValue",
                nodeType.canSetProperty(propDef.getName(), booleanValue));

        Value nameValue = NodeTypeUtil.getValueOfType(PropertyType.NAME);
        assertFalse("canSetProperty(String propertyName, Value value) must return " +
                "false if the property is of type Double and value is a NameValue",
                nodeType.canSetProperty(propDef.getName(), nameValue));

        Value pathValue = NodeTypeUtil.getValueOfType(PropertyType.PATH);
        assertFalse("canSetProperty(String propertyName, Value value) must return " +
                "false if the property is of type Double and value is a PathValue",
                nodeType.canSetProperty(propDef.getName(), pathValue));
    }

    /**
     * Tests if NodeType.canSetProperty(String propertyName, Value[] values)
     * returns true if all values and its types are convertible to DoubleValue.
     */
    public void testConversionsMultiple()
            throws NotExecutableException, RepositoryException {

        PropertyDef propDef =
                NodeTypeUtil.locatePropertyDef(session, PropertyType.DOUBLE, true, false, false, false);

        if (propDef == null) {
            throw new NotExecutableException("No multiple double property def that meets the " +
                    "requirements of the test has been found");
        }

        NodeType nodeType = propDef.getDeclaringNodeType();


        Value doubleValue = NodeTypeUtil.getValueOfType(PropertyType.DOUBLE);

        Value anyStringValue = NodeTypeUtil.getValueOfType(PropertyType.STRING);
        Value anyStringValues[] = {doubleValue, anyStringValue};
        assertFalse("canSetProperty(String propertyName, Value[] values) must return " +
                "false if the property is of type Double and values are of type StringValue " +
                "that are not convertible to DoubleValues",
                nodeType.canSetProperty(propDef.getName(), anyStringValues));

        Value doubleStringValue =
                new StringValue(NodeTypeUtil.getValueOfType(PropertyType.DOUBLE).getString());
        Value doubleStringValues[] = {doubleStringValue};
        assertTrue("canSetProperty(String propertyName, Value[] values) must return " +
                "true if the property is of type Double and values are of type StringValue " +
                "that are convertible to DoubleValues",
                nodeType.canSetProperty(propDef.getName(), doubleStringValues));

        Value anyBinaryValue = NodeTypeUtil.getValueOfType(PropertyType.BINARY);
        Value anyBinaryValues[] = {doubleValue, anyBinaryValue};
        assertFalse("canSetProperty(String propertyName, Value[] values) must return " +
                "false if the property is of type Double and values are of type BinaryValue " +
                "that are not convertible to DoubleValues",
                nodeType.canSetProperty(propDef.getName(), anyBinaryValues));

        Value doubleBinaryValue =
                new BinaryValue(NodeTypeUtil.getValueOfType(PropertyType.DOUBLE).getString());
        Value doubleBinaryValues[] = {doubleBinaryValue};
        assertTrue("canSetProperty(String propertyName, Value[] values) must return " +
                "true if the property is of type Double and values are of type BinaryValue " +
                "that are convertible to DoubleValues",
                nodeType.canSetProperty(propDef.getName(), doubleBinaryValues));

        Value dateValue = NodeTypeUtil.getValueOfType(PropertyType.DATE);
        Value dateValues[] = {dateValue};
        assertTrue("canSetProperty(String propertyName, Value[] values) must return " +
                "true if the property is of type Double and values are of type DateValue",
                nodeType.canSetProperty(propDef.getName(), dateValues));

        Value doubleValues[] = {doubleValue};
        assertTrue("canSetProperty(String propertyName, Value[] values) must return " +
                "true if the property is of type Double and values are of type DoubleValue",
                nodeType.canSetProperty(propDef.getName(), doubleValues));

        Value longValue = NodeTypeUtil.getValueOfType(PropertyType.LONG);
        Value longValues[] = {longValue};
        assertTrue("canSetProperty(String propertyName, Value[] values) must return " +
                "true if the property is of type Double and values are of type LongValue",
                nodeType.canSetProperty(propDef.getName(), longValues));

        Value booleanValue = NodeTypeUtil.getValueOfType(PropertyType.BOOLEAN);
        Value booleanValues[] = {doubleValue, booleanValue};
        assertFalse("canSetProperty(String propertyName, Value[] values) must return " +
                "false if the property is of type Double and values are of type BooleanValue",
                nodeType.canSetProperty(propDef.getName(), booleanValues));

        Value nameValue = NodeTypeUtil.getValueOfType(PropertyType.NAME);
        Value nameValues[] = {doubleValue, nameValue};
        assertFalse("canSetProperty(String propertyName, Value[] values) must return " +
                "false if the property is of type Double and values are of type NameValue",
                nodeType.canSetProperty(propDef.getName(), nameValues));

        Value pathValue = NodeTypeUtil.getValueOfType(PropertyType.PATH);
        Value pathValues[] = {doubleValue, pathValue};
        assertFalse("canSetProperty(String propertyName, Value[] values) must return " +
                "false if the property is of type Double and values are of type PathValue",
                nodeType.canSetProperty(propDef.getName(), pathValues));
    }

    /**
     * Tests if canSetProperty(String propertyName, Value value) returns false
     * if value does not match the value constraints of the property def
     */
    public void testOutOfValueConstraint()
            throws NotExecutableException, ParseException, RepositoryException {

        PropertyDef propDef =
                NodeTypeUtil.locatePropertyDef(session, PropertyType.DOUBLE, false, false, true, false);

        if (propDef == null) {
            throw new NotExecutableException("No double property def with " +
                    "testable value constraints has been found");
        }

        Value value = NodeTypeUtil.getValueOutOfContstraint(propDef);
        if (value == null) {
            // value should never be null since this is catched already in locatePropertyDef
            throw new NotExecutableException("No double property def with " +
                    "testable value constraints has been found");
        }

        NodeType nodeType = propDef.getDeclaringNodeType();

        assertFalse("canSetProperty(String propertyName, Value value) must " +
                "return false if value does not match the value constraints.",
                nodeType.canSetProperty(propDef.getName(), value));
    }

    /**
     * Tests if canSetProperty(String propertyName, Value[] values) returns
     * false if values do not match the value constraints of the property def
     */
    public void testOutOfValueConstraintMultiple()
            throws NotExecutableException, ParseException, RepositoryException {

        PropertyDef propDef =
                NodeTypeUtil.locatePropertyDef(session, PropertyType.DOUBLE, true, false, true, false);

        if (propDef == null) {
            throw new NotExecutableException("No multiple double property def with " +
                    "testable value constraints has been found");
        }

        Value value = NodeTypeUtil.getValueOutOfContstraint(propDef);
        if (value == null) {
            // value should never be null since this is catched already in locatePropertyDef
            throw new NotExecutableException("No multiple double property def with " +
                    "testable value constraints has been found");
        }

        NodeType nodeType = propDef.getDeclaringNodeType();
        Value values[] = {value};

        assertFalse("canSetProperty(String propertyName, Value[] values) must " +
                "return false if values do not match the value constraints.",
                nodeType.canSetProperty(propDef.getName(), values));
    }
}