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
 * values)</code> where property is of type Name.
 *
 * @test
 * @sources CanSetPropertyNameTest.java
 * @executeClass org.apache.jackrabbit.test.api.nodetype.CanSetPropertyNameTest
 * @keywords level2
 */
public class CanSetPropertyNameTest extends AbstractJCRTest {
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
     * returns true if value and its type are convertible to NameValue.
     */
    public void testConversions()
            throws NotExecutableException, RepositoryException {

        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(session, PropertyType.NAME, false, false, false, false);

        if (propDef == null) {
            throw new NotExecutableException("No name property def that meets the " +
                    "requirements of the test has been found");
        }

        NodeType nodeType = propDef.getDeclaringNodeType();


        Value nameStringValue = superuser.getValueFactory().createValue("abc");
        assertTrue("canSetProperty(String propertyName, Value value) must return " +
                "true if the property is of type Name and value is a StringValue " +
                "that is convertible to a NameValue",
                nodeType.canSetProperty(propDef.getName(), nameStringValue));

        Value noNameStringValue = superuser.getValueFactory().createValue("a:b:c");
        assertFalse("canSetProperty(String propertyName, Value value) must return " +
                "false if the property is of type Name and value is a StringValue " +
                "that is not convertible to a NameValue",
                nodeType.canSetProperty(propDef.getName(), noNameStringValue));

        Value nameBinaryValue = superuser.getValueFactory().createValue("abc", PropertyType.BINARY);
        assertTrue("canSetProperty(String propertyName, Value value) must return " +
                "true if the property is of type Name and value is a UTF-8 " +
                "BinaryValue that is convertible to a NameValue",
                nodeType.canSetProperty(propDef.getName(), nameBinaryValue));

        Value noNameBinaryValue = superuser.getValueFactory().createValue("a:b:c", PropertyType.BINARY);
        assertFalse("canSetProperty(String propertyName, Value value) must return " +
                "false if the property is of type Name and value is a UTF-8 " +
                "BinaryValue that is not convertible to a NameValue",
                nodeType.canSetProperty(propDef.getName(), noNameBinaryValue));

        Value dateValue = NodeTypeUtil.getValueOfType(superuser, PropertyType.DATE);
        assertFalse("canSetProperty(String propertyName, Value value) must return " +
                "false if the property is of type Name and value is a DateValue",
                nodeType.canSetProperty(propDef.getName(), dateValue));

        Value doubleValue = NodeTypeUtil.getValueOfType(superuser, PropertyType.DOUBLE);
        assertFalse("canSetProperty(String propertyName, Value value) must return " +
                "false if the property is of type Name and value is a DoubleValue",
                nodeType.canSetProperty(propDef.getName(), doubleValue));

        Value longValue = NodeTypeUtil.getValueOfType(superuser, PropertyType.LONG);
        assertFalse("canSetProperty(String propertyName, Value value) must return " +
                "false if the property is of type Name and value is a LongValue",
                nodeType.canSetProperty(propDef.getName(), longValue));

        Value booleanValue = NodeTypeUtil.getValueOfType(superuser, PropertyType.BOOLEAN);
        assertFalse("canSetProperty(String propertyName, Value value) must return " +
                "false if the property is of type Name and value is a BooleanValue",
                nodeType.canSetProperty(propDef.getName(), booleanValue));

        Value nameValue = NodeTypeUtil.getValueOfType(superuser, PropertyType.NAME);
        assertTrue("canSetProperty(String propertyName, Value value) must return " +
                "true if the property is of type Name and value is a NameValue",
                nodeType.canSetProperty(propDef.getName(), nameValue));

        Value namePathValue = superuser.getValueFactory().createValue("abc", PropertyType.PATH);
        assertTrue("canSetProperty(String propertyName, Value value) must return " +
                "true if the property is of type Name and value is a PathValue " +
                "if Path is relative, is one element long and has no index",
                nodeType.canSetProperty(propDef.getName(), namePathValue));

        Value noNamePathValue = superuser.getValueFactory().createValue("/abc", PropertyType.PATH);
        assertFalse("canSetProperty(String propertyName, Value value) must return " +
                "false if the property is of type Name and value is a PathValue " +
                "if Path is not relative, is more than one element long or has an index",
                nodeType.canSetProperty(propDef.getName(), noNamePathValue));
    }

    /**
     * Tests if NodeType.canSetProperty(String propertyName, Value[] values)
     * returns true if all values and its types are convertible to NameValue.
     */
    public void testConversionsMultiple()
            throws NotExecutableException, RepositoryException {

        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(session, PropertyType.NAME, true, false, false, false);

        if (propDef == null) {
            throw new NotExecutableException("No multiple name property def that meets the " +
                    "requirements of the test has been found");
        }

        NodeType nodeType = propDef.getDeclaringNodeType();


        Value nameValue = NodeTypeUtil.getValueOfType(superuser, PropertyType.NAME);

        Value nameStringValue = superuser.getValueFactory().createValue("abc");
        Value nameStringValues[] = new Value[] {nameStringValue};
        assertTrue("canSetProperty(String propertyName, Value[] values) must return " +
                "true if the property is of type Name and values are of type StringValue " +
                "that are convertible to NameValues",
                nodeType.canSetProperty(propDef.getName(), nameStringValues));

        Value notNameStringValue = superuser.getValueFactory().createValue("a:b:c");
        Value notNameStringValues[] = new Value[] {nameValue, notNameStringValue};
        assertFalse("canSetProperty(String propertyName, Value[] values) must return " +
                "false if the property is of type Name and values are of type StringValue " +
                "that are not convertible to NameValues ",
                nodeType.canSetProperty(propDef.getName(), notNameStringValues));

        Value nameBinaryValue = superuser.getValueFactory().createValue("abc", PropertyType.BINARY);
        Value nameBinaryValues[] = new Value[] {nameBinaryValue};
        assertTrue("canSetProperty(String propertyName, Value[] values) must return " +
                "true if the property is of type Name and values are of type BinaryValue " +
                "that are convertible to NameValues",
                nodeType.canSetProperty(propDef.getName(), nameBinaryValues));

        Value notNameBinaryValue = superuser.getValueFactory().createValue("a:b:c", PropertyType.BINARY);
        Value notNameBinaryValues[] = new Value[] {nameValue, notNameBinaryValue};
        assertFalse("canSetProperty(String propertyName, Value[] values) must return " +
                "false if the property is of type Name and values are of type BinaryValue " +
                "that are not convertible to NameValues",
                nodeType.canSetProperty(propDef.getName(), notNameBinaryValues));

        Value dateValue = NodeTypeUtil.getValueOfType(superuser, PropertyType.DATE);
        Value dateValues[] = new Value[] {nameValue, dateValue};
        assertFalse("canSetProperty(String propertyName, Value[] values) must return " +
                "false if the property is of type Name and values are of type DateValue",
                nodeType.canSetProperty(propDef.getName(), dateValues));

        Value doubleValue = NodeTypeUtil.getValueOfType(superuser, PropertyType.DOUBLE);
        Value doubleValues[] = new Value[] {nameValue, doubleValue};
        assertFalse("canSetProperty(String propertyName, Value[] values) must return " +
                "false if the property is of type Name and values are of type DoubleValue",
                nodeType.canSetProperty(propDef.getName(), doubleValues));

        Value longValue = NodeTypeUtil.getValueOfType(superuser, PropertyType.LONG);
        Value longValues[] = new Value[] {nameValue, longValue};
        assertFalse("canSetProperty(String propertyName, Value[] values) must return " +
                "false if the property is of type Name and values are of type LongValue",
                nodeType.canSetProperty(propDef.getName(), longValues));

        Value booleanValue = NodeTypeUtil.getValueOfType(superuser, PropertyType.BOOLEAN);
        Value booleanValues[] = new Value[] {booleanValue};
        assertFalse("canSetProperty(String propertyName, Value[] values) must return " +
                "false if the property is of type Name and values are of type BooleanValue",
                nodeType.canSetProperty(propDef.getName(), booleanValues));

        Value nameValues[] = new Value[] {nameValue};
        assertTrue("canSetProperty(String propertyName, Value[] values) must return " +
                "true if the property is of type Name and values are of type NameValue",
                nodeType.canSetProperty(propDef.getName(), nameValues));

        Value namePathValue = superuser.getValueFactory().createValue("abc", PropertyType.PATH);
        Value namePathValues[] = new Value[] {namePathValue};
        assertTrue("canSetProperty(String propertyName, Value[] values) must return " +
                "true if the property is of type Name and values are of type PathValue " +
                "if Path is relative, is one element long and has no index",
                nodeType.canSetProperty(propDef.getName(), namePathValues));

        Value notNamePathValue =superuser.getValueFactory().createValue("/abc", PropertyType.PATH);
        Value notNamePathValues[] = new Value[] {nameValue, notNamePathValue};
        assertFalse("canSetProperty(String propertyName, Value[] values) must return " +
                "false if the property is of type Name and values are of type PathValue " +
                "if Path is not relative, is more than one element long or has an index",
                nodeType.canSetProperty(propDef.getName(), notNamePathValues));
    }

    /**
     * Tests if canSetProperty(String propertyName, Value value) returns false
     * if value does not satisfy the value constraints of the property def
     */
    public void testValueConstraintNotSatisfied()
            throws NotExecutableException, ParseException, RepositoryException {

        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(session, PropertyType.NAME, false, false, true, false);

        if (propDef == null) {
            throw new NotExecutableException("No name property def with " +
                    "testable value constraints has been found");
        }

        Value value = NodeTypeUtil.getValueAccordingToValueConstraints(superuser, propDef, false);
        if (value == null) {
            throw new NotExecutableException("No name property def with " +
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
                NodeTypeUtil.locatePropertyDef(session, PropertyType.NAME, true, false, true, false);

        if (propDef == null) {
            throw new NotExecutableException("No multiple name property def with " +
                    "testable value constraints has been found");
        }

        Value value = NodeTypeUtil.getValueAccordingToValueConstraints(superuser, propDef, false);
        if (value == null) {
            throw new NotExecutableException("No multiple name property def with " +
                    "testable value constraints has been found");
        }

        NodeType nodeType = propDef.getDeclaringNodeType();
        Value values[] = new Value[] {value};

        assertFalse("canSetProperty(String propertyName, Value[] values) must " +
                "return false if values do not match the value constraints.",
                nodeType.canSetProperty(propDef.getName(), values));
    }
}