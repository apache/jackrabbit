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
 * @sources CanSetPropertyLongTest.java
 * @executeClass org.apache.jackrabbit.test.api.nodetype.CanSetPropertyLongTest
 * @keywords level1
 */
public class CanSetPropertyLongTest extends AbstractJCRTest {
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
     * returns true if value and its type are convertible to LongValue.
     */
    public void testConversions()
            throws NotExecutableException, RepositoryException {

        PropertyDef propDef =
                NodeTypeUtil.locatePropertyDef(session, PropertyType.DOUBLE, false, false, false, false);

        if (propDef == null) {
            throw new NotExecutableException("No long property def that meets the " +
                    "requirements of the test has been found");
        }

        NodeType nodeType = propDef.getDeclaringNodeType();


        Value anyStringValue = NodeTypeUtil.getValueOfType(PropertyType.STRING);
        assertFalse("canSetProperty(String propertyName, Value value) must return " +
                "false if the property is of type Long and value is a StringValue " +
                "that is not convertible to a LongValue",
                nodeType.canSetProperty(propDef.getName(), anyStringValue));

        Value longStringValue =
                new StringValue(NodeTypeUtil.getValueOfType(PropertyType.LONG).getString());
        assertTrue("canSetProperty(String propertyName, Value value) must return " +
                "true if the property is of type Long and value is a StringValue " +
                "that is convertible to a LongValue",
                nodeType.canSetProperty(propDef.getName(), longStringValue));

        Value anyBinaryValue = NodeTypeUtil.getValueOfType(PropertyType.BINARY);
        assertFalse("canSetProperty(String propertyName, Value value) must return " +
                "false if the property is of type Long and value is a UTF-8 " +
                "BinaryValue that is not convertible to a LongValue",
                nodeType.canSetProperty(propDef.getName(), anyBinaryValue));

        Value longBinaryValue =
                new BinaryValue(NodeTypeUtil.getValueOfType(PropertyType.LONG).getString());
        assertTrue("canSetProperty(String propertyName, Value value) must return " +
                "true if the property is of type Long and value is a UTF-8 " +
                "BinaryValue that is convertible to a LongValue",
                nodeType.canSetProperty(propDef.getName(), longBinaryValue));

        Value dateValue = NodeTypeUtil.getValueOfType(PropertyType.DATE);
        assertTrue("canSetProperty(String propertyName, Value value) must return " +
                "true if the property is of type Long and value is a DateValue",
                nodeType.canSetProperty(propDef.getName(), dateValue));

        Value doubleValue = NodeTypeUtil.getValueOfType(PropertyType.DOUBLE);
        assertTrue("canSetProperty(String propertyName, Value value) must return " +
                "true if the property is of type Long and value is a DoubleValue",
                nodeType.canSetProperty(propDef.getName(), doubleValue));

        Value longValue = NodeTypeUtil.getValueOfType(PropertyType.LONG);
        assertTrue("canSetProperty(String propertyName, Value value) must return " +
                "true if the property is of type Long and value is a LongValue",
                nodeType.canSetProperty(propDef.getName(), longValue));

        Value booleanValue = NodeTypeUtil.getValueOfType(PropertyType.BOOLEAN);
        assertFalse("canSetProperty(String propertyName, Value value) must return " +
                "false if the property is of type Long and value is a BooleanValue",
                nodeType.canSetProperty(propDef.getName(), booleanValue));

        Value nameValue = NodeTypeUtil.getValueOfType(PropertyType.NAME);
        assertFalse("canSetProperty(String propertyName, Value value) must return " +
                "false if the property is of type Long and value is a NameValue",
                nodeType.canSetProperty(propDef.getName(), nameValue));

        Value pathValue = NodeTypeUtil.getValueOfType(PropertyType.PATH);
        assertFalse("canSetProperty(String propertyName, Value value) must return " +
                "false if the property is of type Long and value is a PathValue",
                nodeType.canSetProperty(propDef.getName(), pathValue));
    }

    /**
     * Tests if NodeType.canSetProperty(String propertyName, Value[] values)
     * returns true if all values and its types are convertible to LongValue.
     */
    public void testConversionsMultiple()
            throws NotExecutableException, RepositoryException {

        PropertyDef propDef =
                NodeTypeUtil.locatePropertyDef(session, PropertyType.DOUBLE, true, false, false, false);

        if (propDef == null) {
            throw new NotExecutableException("No multiple long property def that meets the " +
                    "requirements of the test has been found");
        }

        NodeType nodeType = propDef.getDeclaringNodeType();


        Value longValue = NodeTypeUtil.getValueOfType(PropertyType.LONG);

        Value anyStringValue = NodeTypeUtil.getValueOfType(PropertyType.STRING);
        Value anyStringValues[] = {longValue, anyStringValue};
        assertFalse("canSetProperty(String propertyName, Value[] values) must return " +
                "false if the property is of type Long and values are of type StringValue " +
                "that are not convertible to LongValues",
                nodeType.canSetProperty(propDef.getName(), anyStringValues));

        Value longStringValue =
                new StringValue(NodeTypeUtil.getValueOfType(PropertyType.LONG).getString());
        Value longStringValues[] = {longStringValue};
        assertTrue("canSetProperty(String propertyName, Value[] values) must return " +
                "true if the property is of type Long and values are of type StringValue " +
                "that are convertible to LongValues",
                nodeType.canSetProperty(propDef.getName(), longStringValues));

        Value anyBinaryValue = NodeTypeUtil.getValueOfType(PropertyType.BINARY);
        Value anyBinaryValues[] = {longValue, anyBinaryValue};
        assertFalse("canSetProperty(String propertyName, Value[] values) must return " +
                "false if the property is of type Long and values are of type BinaryValue " +
                "that are not convertible to LongValues",
                nodeType.canSetProperty(propDef.getName(), anyBinaryValues));

        Value longBinaryValue =
                new BinaryValue(NodeTypeUtil.getValueOfType(PropertyType.LONG).getString());
        Value longBinaryValues[] = {longBinaryValue};
        assertTrue("canSetProperty(String propertyName, Value[] values) must return " +
                "true if the property is of type Long and values are of type BinaryValue " +
                "that are convertible to LongValues",
                nodeType.canSetProperty(propDef.getName(), longBinaryValues));

        Value dateValue = NodeTypeUtil.getValueOfType(PropertyType.DATE);
        Value dateValues[] = {dateValue};
        assertTrue("canSetProperty(String propertyName, Value[] values) must return " +
                "true if the property is of type Long and values are of type DateValue",
                nodeType.canSetProperty(propDef.getName(), dateValues));

        Value doubleValue = NodeTypeUtil.getValueOfType(PropertyType.DOUBLE);
        Value doubleValues[] = {doubleValue};
        assertTrue("canSetProperty(String propertyName, Value[] values) must return " +
                "true if the property is of type Long and values are of type DoubleValue",
                nodeType.canSetProperty(propDef.getName(), doubleValues));

        Value longValues[] = {longValue};
        assertTrue("canSetProperty(String propertyName, Value[] values) must return " +
                "true if the property is of type Long and values are of type LongValue",
                nodeType.canSetProperty(propDef.getName(), longValues));

        Value booleanValue = NodeTypeUtil.getValueOfType(PropertyType.BOOLEAN);
        Value booleanValues[] = {longValue, booleanValue};
        assertFalse("canSetProperty(String propertyName, Value[] values) must return " +
                "false if the property is of type Long and values are of type BooleanValue",
                nodeType.canSetProperty(propDef.getName(), booleanValues));

        Value nameValue = NodeTypeUtil.getValueOfType(PropertyType.NAME);
        Value nameValues[] = {longValue, nameValue};
        assertFalse("canSetProperty(String propertyName, Value[] values) must return " +
                "false if the property is of type Long and values are of type NameValue",
                nodeType.canSetProperty(propDef.getName(), nameValues));

        Value pathValue = NodeTypeUtil.getValueOfType(PropertyType.PATH);
        Value pathValues[] = {longValue, pathValue};
        assertFalse("canSetProperty(String propertyName, Value[] values) must return " +
                "false if the property is of type Long and values are of type PathValue",
                nodeType.canSetProperty(propDef.getName(), pathValues));
    }

    /**
     * Tests if canSetProperty(String propertyName, Value value) returns false
     * if value does not match the value constraints of the property def
     */
    public void testOutOfValueConstraint()
            throws NotExecutableException, ParseException, RepositoryException {

        PropertyDef propDef =
                NodeTypeUtil.locatePropertyDef(session, PropertyType.LONG, false, false, true, false);

        if (propDef == null) {
            throw new NotExecutableException("No long property def with " +
                    "testable value constraints has been found");
        }

        Value value = NodeTypeUtil.getValueOutOfContstraint(propDef);
        if (value == null) {
            // value should never be null since this is catched already in locatePropertyDef
            throw new NotExecutableException("No long property def with " +
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
                NodeTypeUtil.locatePropertyDef(session, PropertyType.LONG, true, false, true, false);

        if (propDef == null) {
            throw new NotExecutableException("No multiple long property def with " +
                    "testable value constraints has been found");
        }

        Value value = NodeTypeUtil.getValueOutOfContstraint(propDef);
        if (value == null) {
            // value should never be null since this is catched already in locatePropertyDef
            throw new NotExecutableException("No multiple long property def with " +
                    "testable value constraints has been found");
        }

        NodeType nodeType = propDef.getDeclaringNodeType();
        Value values[] = {value};

        assertFalse("canSetProperty(String propertyName, Value[] values) must " +
                "return false if values do not match the value constraints.",
                nodeType.canSetProperty(propDef.getName(), values));
    }
}