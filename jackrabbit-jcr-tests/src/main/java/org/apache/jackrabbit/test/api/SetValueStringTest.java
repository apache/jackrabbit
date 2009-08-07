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
package org.apache.jackrabbit.test.api;

import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.Property;
import javax.jcr.Value;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.PathNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

/**
 * Tests the various {@link Property#setValue(Value)} methods.
 * <p/>
 * Configuration requirements:<br/>
 * The node at {@link #testRoot} must allow a child node of type
 * {@link #testNodeType} with name {@link #nodeName1}. The node type
 * {@link #testNodeType} must define a single value string property with
 * name {@link #propertyName1} and a multi value string property with name
 * {@link #propertyName2}.
 *
 * @test
 * @sources SetValueStringTest.java
 * @executeClass org.apache.jackrabbit.test.api.SetValueStringTest
 * @keywords level2
 */
public class SetValueStringTest extends AbstractJCRTest {

    private static final String PROP_VALUE_1 = "JCR";
    private static final String PROP_VALUE_2 = "JSR-170";

    private Property property1 = null;
    private Property property2 = null;
    private Node node = null;
    private Value sv1, sv2 = null;
    private Value[] mv1, mv2 = null;

    protected void setUp() throws Exception {
        super.setUp();

        // initialize some multi-value properties
        sv1 = superuser.getValueFactory().createValue(PROP_VALUE_1);
        sv2 = superuser.getValueFactory().createValue(PROP_VALUE_2);
        mv1 = new Value[]{sv1};
        mv2 = new Value[]{sv1, sv2};

        // create a new node under the testRootNode
        node = testRootNode.addNode(nodeName1, testNodeType);
        superuser.save();

        // create a new single-value property and save it
        property1 = node.setProperty(propertyName1, PROP_VALUE_1);
        superuser.save();

        // create a new multi-value property and save it
        property2 = node.setProperty(propertyName2, mv1);
        superuser.save();
    }

    protected void tearDown() throws Exception {
        property1 = null;
        property2 = null;
        node = null;
        sv1 = null;
        sv2 = null;
        mv1 = null;
        mv2 = null;
        super.tearDown();
    }

    // Value tests

    /**
     * Test the persistence of a property modified with a Value parameter and
     * saved from the Session Requires a single-value Value (sv2)
     */
    public void testValueSession() throws RepositoryException {
        property1.setValue(sv2);
        superuser.save();
        assertEquals("Value node property not saved", sv2, property1.getValue());
    }

    /**
     * Test the persistence of a property modified with a Value parameter, and
     * saved from the parent Node Requires a single-value Value (sv2)
     */
    public void testValueParent() throws RepositoryException {
        property1.setValue(sv2);
        testRootNode.save();
        assertEquals("Value node property not saved", sv2, property1.getValue());
    }

    /**
     * Test the modification of a single-value property updated with a
     * multi-value parameter Requires a multi-value Value (mv)
     */
    public void testMultiValue() throws RepositoryException {
        Value[] mv = new Value[]{sv1, sv2}; // multi-value
        try {
            property1.setValue(mv);
            fail("Assigning multiple Value values to a single-valued property should throw a ValueFormatException");
        } catch (ValueFormatException e) {
            //success : ValueFormatException as required by the specification
        }
    }

    /**
     * Test the deletion of a property by assigning it a null value, saved from
     * the Session
     */
    public void testRemoveValueSession() throws RepositoryException {
        Value sv = null;
        property1.setValue(sv);
        superuser.save();

        try {
            node.getProperty(propertyName1);
            fail("The property should not exist anymore, as a null Value has been assigned");
        } catch (PathNotFoundException e) {
            //success : the property has been deleted by assigning it a null value
        }
    }

    /**
     * Test the deletion of a property by assigning it a null value, saved from
     * the parent Node
     */
    public void testRemoveValueParent() throws RepositoryException {
        Value sv = null;
        property1.setValue(sv);
        node.save();

        try {
            node.getProperty(propertyName1);
            fail("The property should not exist anymore, as a null Value has been assigned");
        } catch (PathNotFoundException e) {
            //success : the property has been deleted by assigning it a null value
        }
    }

    // String tests

    /**
     * Test the persistence of a property modified with a String parameter and
     * saved from the Session Requires a single-value String (PROP_VALUE_2)
     */
    public void testStringSession() throws RepositoryException {
        property1.setValue(PROP_VALUE_2);
        superuser.save();
        assertEquals("String node property not saved", PROP_VALUE_2, property1.getString());
    }

    /**
     * Test the persistence of a property modified with a String parameter and
     * saved from the parent Node Requires a single-value String (PROP_VALUE_2)
     */
    public void testStringParent() throws RepositoryException {
        property1.setValue(PROP_VALUE_2);
        node.save();
        assertEquals("String node property not saved", PROP_VALUE_2, property1.getString());
    }

    /**
     * Test the modification of a single-value property updated with a
     * multi-value parameter Requires a multi-value String (mv)
     */
    public void testMultiString() throws RepositoryException {
        String[] mv = new String[]{PROP_VALUE_1, PROP_VALUE_2}; // multi-value
        try {
            property1.setValue(mv);
            fail("Assigning multiple String values to a single-valued property should throw a ValueFormatException");
        } catch (ValueFormatException e) {
            //success : ValueFormatException as required by the specification
        }
    }

    /**
     * Test the deletion of a property by assigning it a null value, saved from
     * the Session
     */
    public void testRemoveStringSession() throws RepositoryException {
        String sv = null;
        property1.setValue(sv);
        superuser.save();

        try {
            node.getProperty(propertyName1);
            fail("The property should not exist anymore, as a null String has been assigned");
        } catch (PathNotFoundException e) {
            //success : the property has been deleted by assigning it a null value
        }
    }

    /**
     * Test the deletion of a property by assigning it a null value, saved from
     * the parent Node
     */
    public void testRemoveStringParent() throws RepositoryException {
        String sv = null;
        property1.setValue(sv);
        node.save();

        try {
            node.getProperty(propertyName1);
            fail("The property should not exist anymore, as a null String has been assigned");
        } catch (PathNotFoundException e) {
            //success : the property has been deleted by assigning it a null value
        }
    }


    // Multi-value Value tests

    /**
     * Test the persistence of a property modified with an multi-value Value
     * parameter and saved from the Session Requires a multi-value Value (mv2)
     */
    public void testMultiValueSession() throws RepositoryException {
        property2.setValue(mv2);
        superuser.save();
        assertEquals("Node property not saved", Arrays.asList(mv2), Arrays.asList(property2.getValues()));
    }

    /**
     * Test the persistence of a property modified with an multi-value Value
     * parameter and saved from the parent Node Requires a multi-value Value
     * (mv2)
     */
    public void testMultiValueParent() throws RepositoryException {
        property2.setValue(mv2);
        node.save();
        assertEquals("Node property not saved", Arrays.asList(mv2), Arrays.asList(property2.getValues()));
    }

    /**
     * Test the assignment of a single-value to a multi-value property Requires
     * a single-value Value (sv1)
     */
    public void testMultiValueSingle() throws RepositoryException {
        try {
            property2.setValue(sv1);
            fail("Assigning a single Value value to a multi-valued property should throw a ValueFormatException");
        } catch (ValueFormatException e) {
            //success : ValueFormatException as required by the specification
        }
    }

    /**
     * Test the deletion of a property by assigning it a null value, saved from
     * the Session
     */
    public void testRemoveMultiValueSession() throws RepositoryException {
        property2.setValue((Value[]) null);
        superuser.save();

        try {
            node.getProperty(propertyName2);
            fail("The property should not exist anymore, as a null Value has been assigned");
        } catch (PathNotFoundException e) {
            //success : the property has been deleted by assigning it a null value
        }
    }

    /**
     * Test the deletion of a property by assigning it a null value, saved from
     * the parent Node
     */
    public void testRemoveMultiValueParent() throws RepositoryException {
        property2.setValue((Value[]) null);
        node.save();

        try {
            node.getProperty(propertyName2);
            fail("The property should not exist anymore, as a null Value has been assigned");
        } catch (PathNotFoundException e) {
            //success : the property has been deleted by assigning it a null value
        }
    }


    // Assignment of a null Value in a multi-value array

    /**
     * Test the deletion of a value in a multi-value property
     */
    public void testNullMultiValue() throws RepositoryException {
        property2.setValue(new Value[]{null, sv2});
        superuser.save();
        assertEquals("Null value not removed", Arrays.asList(property2.getValues()), Arrays.asList(new Value[]{sv2}));
    }


    // Multi-value String tests

    /**
     * Test the persistence of a property modified with an multi-value String
     * parameter and saved from the Session Requires a multi-value String (mv)
     */
    public void testMultiStringSession() throws RepositoryException {
        String[] mv = new String[]{PROP_VALUE_1, PROP_VALUE_2};
        property2.setValue(mv);
        superuser.save();
        Value[] values = property2.getValues();
        List strValues = new ArrayList();
        for (int i = 0; i < values.length; i++) {
            strValues.add(values[i].getString());
        }
        assertEquals("Node property not saved", Arrays.asList(mv), strValues);
    }

    /**
     * Test the persistence of a property modified with an multi-value String
     * parameter and saved from the parent Node Requires a multi-value String
     * (mv)
     */
    public void testMultiStringParent() throws RepositoryException {
        String[] mv = new String[]{PROP_VALUE_1, PROP_VALUE_2};
        property2.setValue(mv);
        node.save();
        Value[] values = property2.getValues();
        List strValues = new ArrayList();
        for (int i = 0; i < values.length; i++) {
            strValues.add(values[i].getString());
        }
        assertEquals("Node property not saved", Arrays.asList(mv), strValues);
    }

    /**
     * Test the assignment of a single-value to a multi-value property Requires
     * a single-value String (PROP_VALUE_1)
     */
    public void testMultiStringSingle() throws RepositoryException {
        try {
            property2.setValue(PROP_VALUE_1);
            fail("Assigning a single String value to a multi-valued property should throw a ValueFormatException");
        } catch (ValueFormatException e) {
            //success : ValueFormatException as required by the specification
        }
    }

    /**
     * Test the deletion of a property by assigning it a null value, saved from
     * the Session
     */
    public void testRemoveMultiStringSession() throws RepositoryException {
        property2.setValue((String[]) null);
        superuser.save();

        try {
            node.getProperty(propertyName2);
            fail("The property should not exist anymore, as a null Value has been assigned");
        } catch (PathNotFoundException e) {
            //success : the property has been deleted by assigning it a null value
        }
    }

    /**
     * Test the deletion of a property by assigning it a null value, saved from
     * the parent Node
     */
    public void testRemoveMultiStringParent() throws RepositoryException {
        property2.setValue((String[]) null);
        node.save();

        try {
            node.getProperty(propertyName2);
            fail("The property should not exist anymore, as a null Value has been assigned");
        } catch (PathNotFoundException e) {
            //success : the property has been deleted by assigning it a null value
        }
    }

    /**
     * Test the assignment of an empty property by assigning it a null array,
     * saved from the parent Node
     */
    public void testEmptyMultiStringParent() throws RepositoryException {
        String[] emptyStringArray = new String[]{null};
        property2.setValue(emptyStringArray);
        node.save();

        assertEquals("Property.setValue(emptyStringArray) did not set the property to an empty array", 0, property2.getValues().length);
    }

    /**
     * Test the assignment of an empty property by assigning it a null array,
     * saved from the Session
     */
    public void testEmptyMultiStringSession() throws RepositoryException {
        String[] emptyStringArray = new String[]{null};
        property2.setValue(emptyStringArray);
        superuser.save();

        assertEquals("Property.setValue(emptyStringArray) did not set the property to an empty array", 0, property2.getValues().length);
    }
}