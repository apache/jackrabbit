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

import javax.jcr.Value;
import javax.jcr.Node;

import javax.jcr.ValueFormatException;
import javax.jcr.PropertyType;

import java.util.Arrays;

/**
 * <code>SetPropertyValueTest</code> tests the methods <code>Node.setProperty(String,
 * Value)</code>, <code>Node.setProperty(String, Value[])</code> and
 * <code>Node.setProperty(String, Value[], int)</code>
 *
 * @test
 * @sources SetPropertyValueTest.java
 * @executeClass org.apache.jackrabbit.test.api.SetPropertyValueTest
 * @keywords level2
 */
public class SetPropertyValueTest extends AbstractJCRTest {

    private Node testNode;

    private Value v1;
    private Value v2;

    private Value[] vArray1 = new Value[3];
    private Value[] vArray2 = new Value[3];
    private Value[] vArrayMixed = new Value[3];
    private Value[] vArrayNull = new Value[3];
    private Value[] vArrayWithNulls = new Value[5];

    protected void setUp() throws Exception {
        super.setUp();
        testNode = testRootNode.addNode(nodeName1, testNodeType);

        v1 = superuser.getValueFactory().createValue("abc");
        v2 = superuser.getValueFactory().createValue("xyz");

        vArray1[0] = superuser.getValueFactory().createValue("a");
        vArray1[1] = superuser.getValueFactory().createValue("b");
        vArray1[2] = superuser.getValueFactory().createValue("c");

        vArray2[0] = superuser.getValueFactory().createValue("x");
        vArray2[1] = superuser.getValueFactory().createValue("y");
        vArray2[2] = superuser.getValueFactory().createValue("z");

        vArrayMixed[0] = superuser.getValueFactory().createValue("abc");
        vArrayMixed[1] = superuser.getValueFactory().createValue(true);
        vArrayMixed[2] = superuser.getValueFactory().createValue(2147483650L);

        vArrayWithNulls[1] = superuser.getValueFactory().createValue("a");
        vArrayWithNulls[3] = superuser.getValueFactory().createValue("z");
    }

    protected void tearDown() throws Exception {
        testNode = null;
        v1 = null;
        v2 = null;
        for (int i = 0; i < vArray1.length; i++) {
            vArray1[i] = null;
        }
        for (int i = 0; i < vArray2.length; i++) {
            vArray2[i] = null;
        }
        for (int i = 0; i < vArrayMixed.length; i++) {
            vArrayMixed[i] = null;
        }
        for (int i = 0; i < vArrayNull.length; i++) {
            vArrayNull[i] = null;
        }
        for (int i = 0; i < vArrayWithNulls.length; i++) {
            vArrayWithNulls[i] = null;
        }
        super.tearDown();
    }

    /**
     * Value
     */

    /**
     * Tests if adding a property with <code>Node.setProperty(String,
     * Value)</code> works with <code>Session.save()</code>
     */
    public void testNewValuePropertySession() throws Exception {
        testNode.setProperty(propertyName1, v1);
        superuser.save();
        assertEquals("Setting property with Node.setProperty(String, Value) and Session.save() not working",
                v1,
                testNode.getProperty(propertyName1).getValue());
    }

    /**
     * Tests if modifying a property with <code>Node.setProperty(String,
     * Value)</code> works with <code>Session.save()</code>
     */
    public void testModifyValuePropertySession() throws Exception {
        testNode.setProperty(propertyName1, v1);
        superuser.save();
        testNode.setProperty(propertyName1, v2);
        superuser.save();
        assertEquals("Modifying property with Node.setProperty(String, Value) and Session.save() not working",
                v2,
                testNode.getProperty(propertyName1).getValue());
    }

    /**
     * Tests if adding a property with <code>Node.setProperty(String,
     * Value)</code> works with <code>parentNode.save()</code>
     */
    public void testNewValuePropertyParent() throws Exception {
        testNode.setProperty(propertyName1, v1);
        testRootNode.save();
        assertEquals("Setting property with Node.setProperty(String, Value) and parentNode.save() not working",
                v1,
                testNode.getProperty(propertyName1).getValue());
    }

    /**
     * Tests if modifying a property with <code>Node.setProperty(String,
     * Value)</code> works with <code>parentNode.save()</code>
     */
    public void testModifyValuePropertyParent() throws Exception {
        testNode.setProperty(propertyName1, v1);
        testRootNode.save();
        testNode.setProperty(propertyName1, v2);
        testRootNode.save();
        assertEquals("Modifying property with Node.setProperty(String, Value) and parentNode.save() not working",
                v2,
                testNode.getProperty(propertyName1).getValue());
    }

    /**
     * Tests if removing a <code>Value</code> property with
     * <code>Node.setProperty(String, null)</code> works with
     * <code>Session.save()</code>
     */
    public void testRemoveValuePropertySession() throws Exception {
        testNode.setProperty(propertyName1, v1);
        superuser.save();
        testNode.setProperty(propertyName1, (Value) null);
        superuser.save();
        assertFalse("Removing property with Node.setProperty(String, (Value)null) and Session.save() not working",
                testNode.hasProperty(propertyName1));
    }

    /**
     * Tests if removing a <code>Value</code> property with
     * <code>Node.setProperty(String, null)</code> works with
     * <code>parentNode.save()</code>
     */
    public void testRemoveValuePropertyParent() throws Exception {
        testNode.setProperty(propertyName1, v1);
        testRootNode.save();
        testNode.setProperty(propertyName1, (Value) null);
        testRootNode.save();
        assertFalse("Removing property with Node.setProperty(String, (Value)null) and parentNode.save() not working",
                testNode.hasProperty(propertyName1));
    }

    /**
     * Value with PropertyType
     */

    /**
     * Tests if adding a property with <code>Node.setProperty(String,
     * Value, int)</code> works with <code>Session.save()</code>
     */
    public void testNewValuePropertySessionWithPropertyType() throws Exception {
        testNode.setProperty(propertyName1, v1, PropertyType.STRING);
        superuser.save();
        assertEquals("Setting property with Node.setProperty(String, Value, int) and Session.save() not working",
                v1,
                testNode.getProperty(propertyName1).getValue());
    }

    /**
     * Tests if modifying a property with <code>Node.setProperty(String,
     * Value, int)</code> works with <code>Session.save()</code>
     */
    public void testModifyValuePropertySessionWithPropertyType() throws Exception {
        testNode.setProperty(propertyName1, v1, PropertyType.STRING);
        superuser.save();
        testNode.setProperty(propertyName1, v2, PropertyType.STRING);
        superuser.save();
        assertEquals("Modifying property with Node.setProperty(String, Value, int) and Session.save() not working",
                v2,
                testNode.getProperty(propertyName1).getValue());
    }

    /**
     * Tests if adding a property with <code>Node.setProperty(String,
     * Value, int)</code> works with <code>parentNode.save()</code>
     */
    public void testNewValuePropertyParentWithPropertyType() throws Exception {
        testNode.setProperty(propertyName1, v1, PropertyType.STRING);
        testRootNode.save();
        assertEquals("Setting property with Node.setProperty(String, Value, int) and parentNode.save() not working",
                v1,
                testNode.getProperty(propertyName1).getValue());
    }

    /**
     * Tests if modifying a property with <code>Node.setProperty(String,
     * Value, int)</code> works with <code>parentNode.save()</code>
     */
    public void testModifyValuePropertyParentWithPropertyType() throws Exception {
        testNode.setProperty(propertyName1, v1, PropertyType.STRING);
        testRootNode.save();
        testNode.setProperty(propertyName1, v2, PropertyType.STRING);
        testRootNode.save();
        assertEquals("Modifying property with Node.setProperty(String, Value, int) and parentNode.save() not working",
                v2,
                testNode.getProperty(propertyName1).getValue());
    }

    /**
     * Tests if removing a <code>Value</code> property with
     * <code>Node.setProperty(String, null, int)</code> works with
     * <code>Session.save()</code>
     */
    public void testRemoveValuePropertySessionWithPropertyType() throws Exception {
        testNode.setProperty(propertyName1, v1, PropertyType.STRING);
        superuser.save();
        testNode.setProperty(propertyName1, (Value) null, PropertyType.STRING);
        superuser.save();
        assertFalse("Removing property with Node.setProperty(String, (Value)null, int) and Session.save() not working",
                testNode.hasProperty(propertyName1));
    }

    /**
     * Tests if removing a <code>Value</code> property with
     * <code>Node.setProperty(String, null, int)</code> works with
     * <code>parentNode.save()</code>
     */
    public void testRemoveValuePropertyParentWithPropertyType() throws Exception {
        testNode.setProperty(propertyName1, v1, PropertyType.STRING);
        testRootNode.save();
        testNode.setProperty(propertyName1, (Value) null, PropertyType.STRING);
        testRootNode.save();
        assertFalse("Removing property with Node.setProperty(String, (Value)null, int) and parentNode.save() not working",
                testNode.hasProperty(propertyName1));
    }

    /**
     * Value[]
     */

    /**
     * Tests if adding properties with <code>Node.setProperty(String,
     * Value[])</code> works with <code>Session.save()</code>
     */
    public void testNewValueArrayPropertySession() throws Exception {
        testNode.setProperty(propertyName2, vArray1);
        superuser.save();
        assertEquals("Setting properties with Node.setProperty(String, Value[]) and Session.save() not working",
                Arrays.asList(vArray1),
                Arrays.asList(testNode.getProperty(propertyName2).getValues()));
    }

    /**
     * Tests if modifying properties with <code>Node.setProperty(String,
     * Value[])</code> works with <code>Session.save()</code>
     */
    public void testModifyValueArrayPropertySession() throws Exception {
        testNode.setProperty(propertyName2, vArray1);
        superuser.save();
        testNode.setProperty(propertyName2, vArray2);
        superuser.save();
        assertEquals("Modifying properties with Node.setProperty(String, Value[]) and Session.save() not working",
                Arrays.asList(vArray2),
                Arrays.asList(testNode.getProperty(propertyName2).getValues()));
    }

    /**
     * Tests if adding properties with <code>Node.setProperty(String,
     * Value[])</code> works with <code>parentNode.save()</code>
     */
    public void testNewValueArrayPropertyParent() throws Exception {
        testNode.setProperty(propertyName2, vArray1);
        testRootNode.save();
        assertEquals("Setting properties with Node.setProperty(String, Value[]) and parentNode.save() not working",
                Arrays.asList(vArray1),
                Arrays.asList(testNode.getProperty(propertyName2).getValues()));
    }

    /**
     * Tests if modifying properties with <code>Node.setProperty(String,
     * Value[])</code> works with <code>parentNode.save()</code>
     */
    public void testModifyValueArrayPropertyParent() throws Exception {
        testNode.setProperty(propertyName2, vArray1);
        testRootNode.save();
        testNode.setProperty(propertyName2, vArray2);
        testRootNode.save();
        assertEquals("Modifying properties with Node.setProperty(String, Value[]) and parentNode.save() not working",
                Arrays.asList(vArray2),
                Arrays.asList(testNode.getProperty(propertyName2).getValues()));
    }

    /**
     * Tests if <code>Node.setProperty(String, Value[])</code> throws a {@link
     * javax.jcr.ValueFormatException} when trying to set a multi-value property
     * to an array of values with different types
     */
    public void testSetMixedValueArrayValueFormatException() throws Exception {
        try {
            testNode.setProperty(propertyName2, vArrayMixed);
            fail("setProperty(String, mixedValueArray[]) not throwing a ValueFormatException");
        } catch (ValueFormatException success) {
        }
    }

    /**
     * Tests if <code>Node.setProperty(String, Value[])</code> throws a {@link
     * javax.jcr.ValueFormatException} when trying to set an existing
     * single-valued property to a multi-value
     */
    public void testSetSingleValueArrayValueFormatException() throws Exception {
        // prerequisite: existing single-valued property
        if (!testNode.hasProperty(propertyName1)) {
            testNode.setProperty(propertyName1, v1);
            testNode.getParent().save();
        }

        try {
            testNode.setProperty(propertyName1, vArray1);
            fail("setProperty(singleValueProperty, Value[]) not throwing a ValueFormatException");
        } catch (ValueFormatException success) {
        }
    }

    /**
     * Tests if removing a <code>Value[]</code> property with
     * <code>Node.setProperty(String, null)</code> works with
     * <code>Session.save()</code>
     */
    public void testRemoveValueArrayPropertySession() throws Exception {
        testNode.setProperty(propertyName2, vArray1);
        superuser.save();
        testNode.setProperty(propertyName2, (Value[]) null);
        superuser.save();
        assertFalse("Removing property with Node.setProperty(String, (Value[])null) and Session.save() not working",
                testNode.hasProperty(propertyName2));
    }

    /**
     * Tests if removing a <code>Value[]</code> property with
     * <code>Node.setProperty(String, null)</code> works with
     * <code>parentNode.save()</code>
     */
    public void testRemoveValueArrayPropertyParent() throws Exception {
        testNode.setProperty(propertyName2, vArray1);
        testRootNode.save();
        testNode.setProperty(propertyName2, (Value[]) null);
        testRootNode.save();
        assertFalse("Removing property with Node.setProperty(String, (Value[])null) and parentNode.save() not working",
                testNode.hasProperty(propertyName2));
    }

    /**
     * Tests if <code>Node.setProperty(String, Value[])</code> saves an array of
     * null values as an empty Value[]
     */
    public void testSetNullValueArray() throws Exception {
        testNode.setProperty(propertyName2, vArrayNull);
        superuser.save();
        assertEquals("Node.setProperty(String, nullValueArray[]) did not set the property to an empty Value[]",
                Arrays.asList(new Value[0]),
                Arrays.asList(testNode.getProperty(propertyName2).getValues()));
    }

    /**
     * Tests if <code>Node.setProperty(String, Value[])</code> correctly compacts
     * the value array by removing all null values
     */
    public void testCompactValueArrayWithNulls() throws Exception {
        testNode.setProperty(propertyName2, vArrayWithNulls);
        superuser.save();
        assertEquals("Node.setProperty(String, valueArrayWithNulls[]) did not compact the value array by removing the null values",
                2,
                testNode.getProperty(propertyName2).getValues().length);
    }

    /**
     * Value[] with PropertyType
     */

    /**
     * Tests if adding properties with <code>Node.setProperty(String, Value[],
     * int)</code> works with <code>Session.save()</code>
     */
    public void testNewValueArrayPropertySessionWithPropertyType() throws Exception {
        testNode.setProperty(propertyName2, vArray1, PropertyType.STRING);
        superuser.save();
        assertEquals("Setting properties with Node.setProperty(String, Value[], int) and Session.save() not working",
                Arrays.asList(vArray1),
                Arrays.asList(testNode.getProperty(propertyName2).getValues()));
    }

    /**
     * Tests if modifying properties with <code>Node.setProperty(String,
     * Value[], int)</code> works with <code>Session.save()</code>
     */
    public void testModifyValueArrayPropertySessionWithPropertyType() throws Exception {
        testNode.setProperty(propertyName2, vArray1, PropertyType.STRING);
        superuser.save();
        testNode.setProperty(propertyName2, vArray2, PropertyType.STRING);
        superuser.save();
        assertEquals("Modifying properties with Node.setProperty(String, Value[], int) and Session.save() not working",
                Arrays.asList(vArray2),
                Arrays.asList(testNode.getProperty(propertyName2).getValues()));
    }

    /**
     * Tests if adding properties with <code>Node.setProperty(String, Value[],
     * int)</code> works with <code>parentNode.save()</code>
     */
    public void testNewValueArrayPropertyParentWithPropertyType() throws Exception {
        testNode.setProperty(propertyName2, vArray1, PropertyType.STRING);
        testRootNode.save();
        assertEquals("Setting properties with Node.setProperty(String, Value[], int) and parentNode.save() not working",
                Arrays.asList(vArray1),
                Arrays.asList(testNode.getProperty(propertyName2).getValues()));
    }

    /**
     * Tests if modifying properties with <code>Node.setProperty(String,
     * Value[], int)</code> works with <code>parentNode.save()</code>
     */
    public void testModifyValueArrayPropertyParentWithPropertyType() throws Exception {
        testNode.setProperty(propertyName2, vArray1, PropertyType.STRING);
        testRootNode.save();
        testNode.setProperty(propertyName2, vArray2, PropertyType.STRING);
        testRootNode.save();
        assertEquals("Modifying properties with Node.setProperty(String, Value[], int) and parentNode.save() not working",
                Arrays.asList(vArray2),
                Arrays.asList(testNode.getProperty(propertyName2).getValues()));
    }

    /**
     * Tests if <code>Node.setProperty(String, Value[], int)</code> throws a
     * {@link javax.jcr.ValueFormatException} when trying to set a multi-value
     * property to an array of values with different types
     */
    public void testSetMixedValueArrayValueFormatExceptionWithPropertyType() throws Exception {
        try {
            testNode.setProperty(propertyName2, vArrayMixed, PropertyType.STRING);
            fail("setProperty(String, mixedValueArray[], int) not throwing a ValueFormatException");
        } catch (ValueFormatException success) {
        }
    }

    /**
     * Tests if <code>Node.setProperty(String, Value[], int)</code> throws a
     * {@link javax.jcr.ValueFormatException} when trying to set an existing
     * single-valued property to a multi-value
     */
    public void testSetSingleValueArrayValueFormatExceptionWithPropertyType() throws Exception {
        // prerequisite: existing single-valued property
        if (!testNode.hasProperty(propertyName1)) {
            testNode.setProperty(propertyName1, v1);
            testNode.getParent().save();
        }

        try {
            testNode.setProperty(propertyName1, vArray1, PropertyType.STRING);
            fail("setProperty(singleValueProperty, Value[], int) not throwing a ValueFormatException");
        } catch (ValueFormatException success) {
        }
    }

    /**
     * Tests if removing a <code>Value[]</code> property with
     * <code>Node.setProperty(String, null, int)</code> works with
     * <code>Session.save()</code>
     */
    public void testRemoveValueArrayPropertySessionWithPropertyType() throws Exception {
        testNode.setProperty(propertyName2, vArray1, PropertyType.STRING);
        superuser.save();
        testNode.setProperty(propertyName2, (Value[]) null, PropertyType.STRING);
        superuser.save();
        assertFalse("Removing property with Node.setProperty(String, (Value[])null, int) and Session.save() not working",
                testNode.hasProperty(propertyName2));
    }

    /**
     * Tests if removing a <code>Value[]</code> property with
     * <code>Node.setProperty(String, null, int)</code> works with
     * <code>parentNode.save()</code>
     */
    public void testRemoveValueArrayPropertyParentWithPropertyType() throws Exception {
        testNode.setProperty(propertyName2, vArray1, PropertyType.STRING);
        testRootNode.save();
        testNode.setProperty(propertyName2, (Value[]) null, PropertyType.STRING);
        testRootNode.save();
        assertFalse("Removing property with Node.setProperty(String, (Value[])null, int) and parentNode.save() not working",
                testNode.hasProperty(propertyName2));
    }

    /**
     * Tests if <code>Node.setProperty(String, Value[], int)</code> saves an
     * array of null values as an empty Value[]
     */
    public void testSetNullValueArrayWithPropertyType() throws Exception {
        testNode.setProperty(propertyName2, vArrayNull, PropertyType.STRING);
        superuser.save();
        assertEquals("Node.setProperty(String, nullValueArray[], int) did not set the property to an empty Value[]",
                Arrays.asList(new Value[0]),
                Arrays.asList(testNode.getProperty(propertyName2).getValues()));
    }

}