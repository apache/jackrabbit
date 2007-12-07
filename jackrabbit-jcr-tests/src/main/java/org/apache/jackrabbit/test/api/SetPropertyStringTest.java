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
 * <code>SetPropertyStringTest</code> tests the methods
 * <code>Node.setProperty(String, String)</code>, <code>Node.setProperty(String,
 * String[])</code> and <code>Node.setProperty(String, String[], int)</code>
 *
 * @test
 * @sources SetPropertyStringTest.java
 * @executeClass org.apache.jackrabbit.test.api.SetPropertyStringTest
 * @keywords level2
 */
public class SetPropertyStringTest extends AbstractJCRTest {

    private Node testNode;

    private String s1 = "abc";
    private String s2 = "xyz";

    private String[] sArray1 = new String[3];
    private String[] sArray2 = new String[3];
    private String[] sArrayNull = new String[3];

    private Value[] vArray1 = new Value[3];
    private Value[] vArray2 = new Value[3];


    protected void setUp() throws Exception {
        super.setUp();
        testNode = testRootNode.addNode(nodeName1, testNodeType);

        sArray1[0] = "a";
        sArray1[1] = "b";
        sArray1[2] = "c";
        sArray2[0] = "x";
        sArray2[1] = "y";
        sArray2[2] = "z";
        sArrayNull[0] = null;
        sArrayNull[1] = null;
        sArrayNull[2] = null;

        vArray1[0] = superuser.getValueFactory().createValue("a");
        vArray1[1] = superuser.getValueFactory().createValue("b");
        vArray1[2] = superuser.getValueFactory().createValue("c");

        vArray2[0] = superuser.getValueFactory().createValue("x");
        vArray2[1] = superuser.getValueFactory().createValue("y");
        vArray2[2] = superuser.getValueFactory().createValue("z");
    }

    protected void tearDown() throws Exception {
        testNode = null;
        for (int i = 0; i < vArray1.length; i++) {
            vArray1[i] = null;
        }
        for (int i = 0; i < vArray2.length; i++) {
            vArray2[i] = null;
        }
        super.tearDown();
    }

    // String

    /**
     * Tests if adding a property with <code>Node.setProperty(String,
     * String)</code> works with <code>Session.save()</code>
     */
    public void testNewStringPropertySession() throws Exception {
        testNode.setProperty(propertyName1, s1);
        superuser.save();
        assertEquals("Setting property with Node.setProperty(String, String) and Session.save() not working",
                s1,
                testNode.getProperty(propertyName1).getString());
    }

    /**
     * Tests if modifying a property with <code>Node.setProperty(String,
     * String)</code> works with <code>Session.save()</code>
     */
    public void testModifyStringPropertySession() throws Exception {
        testNode.setProperty(propertyName1, s1);
        superuser.save();
        testNode.setProperty(propertyName1, s2);
        superuser.save();
        assertEquals("Modifying property with Node.setProperty(String, String) and Session.save() not working",
                s2,
                testNode.getProperty(propertyName1).getString());
    }

    /**
     * Tests if adding a property with <code>Node.setProperty(String,
     * String)</code> works with <code>parentNode.save()</code>
     */
    public void testNewStringPropertyParent() throws Exception {
        testNode.setProperty(propertyName1, s1);
        testRootNode.save();
        assertEquals("Setting property with Node.setProperty(String, String) and parentNode.save() not working",
                s1,
                testNode.getProperty(propertyName1).getString());
    }

    /**
     * Tests if modifying a property with <code>Node.setProperty(String,
     * String)</code> works with <code>parentNode.save()</code>
     */
    public void testModifyStringPropertyParent() throws Exception {
        testNode.setProperty(propertyName1, s1);
        testRootNode.save();
        testNode.setProperty(propertyName1, s2);
        testRootNode.save();
        assertEquals("Modifying property with Node.setProperty(String, String) and parentNode.save() not working",
                s2,
                testNode.getProperty(propertyName1).getString());
    }

    /**
     * Tests if removing a <code>String</code> property with
     * <code>Node.setProperty(String, null)</code> works with
     * <code>Session.save()</code>
     */
    public void testRemoveStringPropertySession() throws Exception {
        testNode.setProperty(propertyName1, s1);
        superuser.save();
        testNode.setProperty(propertyName1, (String) null);
        superuser.save();
        assertFalse("Removing property with Node.setProperty(String, (String)null) and Session.save() not working",
                testNode.hasProperty(propertyName1));
    }

    /**
     * Tests if removing a <code>String</code> property with
     * <code>Node.setProperty(String, null)</code> works with
     * <code>parentNode.save()</code>
     */
    public void testRemoveStringPropertyParent() throws Exception {
        testNode.setProperty(propertyName1, s1);
        testRootNode.save();
        testNode.setProperty(propertyName1, (String) null);
        testRootNode.save();
        assertFalse("Removing property with Node.setProperty(String, (String)null) and parentNode.save() not working",
                testNode.hasProperty(propertyName1));
    }


    // String with PropertyType

    /**
     * Tests if adding a property with <code>Node.setProperty(String,
     * String, int)</code> works with <code>Session.save()</code>
     */
    public void testNewStringPropertySessionWithPropertyType() throws Exception {
        testNode.setProperty(propertyName1, s1, PropertyType.STRING);
        superuser.save();
        assertEquals("Setting property with Node.setProperty(String, String, int) and Session.save() not working",
                s1,
                testNode.getProperty(propertyName1).getString());
    }

    /**
     * Tests if modifying a property with <code>Node.setProperty(String,
     * String, int)</code> works with <code>Session.save()</code>
     */
    public void testModifyStringPropertySessionWithPropertyType() throws Exception {
        testNode.setProperty(propertyName1, s1, PropertyType.STRING);
        superuser.save();
        testNode.setProperty(propertyName1, s2, PropertyType.STRING);
        superuser.save();
        assertEquals("Modifying property with Node.setProperty(String, String, int) and Session.save() not working",
                s2,
                testNode.getProperty(propertyName1).getString());
    }

    /**
     * Tests if adding a property with <code>Node.setProperty(String,
     * String, int)</code> works with <code>parentNode.save()</code>
     */
    public void testNewStringPropertyParentWithPropertyType() throws Exception {
        testNode.setProperty(propertyName1, s1, PropertyType.STRING);
        testRootNode.save();
        assertEquals("Setting property with Node.setProperty(String, String, int) and parentNode.save() not working",
                s1,
                testNode.getProperty(propertyName1).getString());
    }

    /**
     * Tests if modifying a property with <code>Node.setProperty(String,
     * String, int)</code> works with <code>parentNode.save()</code>
     */
    public void testModifyStringPropertyParentWithPropertyType() throws Exception {
        testNode.setProperty(propertyName1, s1, PropertyType.STRING);
        testRootNode.save();
        testNode.setProperty(propertyName1, s2, PropertyType.STRING);
        testRootNode.save();
        assertEquals("Modifying property with Node.setProperty(String, String, int) and parentNode.save() not working",
                s2,
                testNode.getProperty(propertyName1).getString());
    }

    /**
     * Tests if removing a <code>String</code> property with
     * <code>Node.setProperty(String, null, int)</code> works with
     * <code>Session.save()</code>
     */
    public void testRemoveStringPropertySessionWithPropertyType() throws Exception {
        testNode.setProperty(propertyName1, s1, PropertyType.STRING);
        superuser.save();
        testNode.setProperty(propertyName1, (String) null, PropertyType.STRING);
        superuser.save();
        assertFalse("Removing property with Node.setProperty(String, (String)null, int) and Session.save() not working",
                testNode.hasProperty(propertyName1));
    }

    /**
     * Tests if removing a <code>String</code> property with
     * <code>Node.setProperty(String, null, int)</code> works with
     * <code>parentNode.save()</code>
     */
    public void testRemoveStringPropertyParentWithPropertyType() throws Exception {
        testNode.setProperty(propertyName1, s1, PropertyType.STRING);
        testRootNode.save();
        testNode.setProperty(propertyName1, (String) null, PropertyType.STRING);
        testRootNode.save();
        assertFalse("Removing property with Node.setProperty(String, (String)null, int) and parentNode.save() not working",
                testNode.hasProperty(propertyName1));
    }


    // String[]

    /**
     * Tests if adding properties with <code>Node.setProperty(String,
     * String[])</code> works with <code>Session.save()</code>
     */
    public void testNewStringArrayPropertySession() throws Exception {
        testNode.setProperty(propertyName2, sArray1);
        superuser.save();
        assertEquals("Setting properties with Node.setProperty(String, String[]) and Session.save() not working",
                Arrays.asList(vArray1),
                Arrays.asList(testNode.getProperty(propertyName2).getValues()));
    }

    /**
     * Tests if modifying properties with <code>Node.setProperty(String,
     * String[])</code> works with <code>Session.save()</code>
     */
    public void testModifyStringArrayPropertySession() throws Exception {
        testNode.setProperty(propertyName2, sArray1);
        superuser.save();
        testNode.setProperty(propertyName2, sArray2);
        superuser.save();
        assertEquals("Modifying properties with Node.setProperty(String, String[]) and Session.save() not working",
                Arrays.asList(vArray2),
                Arrays.asList(testNode.getProperty(propertyName2).getValues()));
    }

    /**
     * Tests if adding properties with <code>Node.setProperty(String,
     * String[])</code> works with <code>parentNode.save()</code>
     */
    public void testNewStringArrayPropertyParent() throws Exception {
        testNode.setProperty(propertyName2, sArray1);
        testRootNode.save();
        assertEquals("Setting properties with Node.setProperty(String, String[]) and parentNode.save() not working",
                Arrays.asList(vArray1),
                Arrays.asList(testNode.getProperty(propertyName2).getValues()));
    }

    /**
     * Tests if modifying properties with <code>Node.setProperty(String,
     * String[])</code> works with <code>parentNode.save()</code>
     */
    public void testModifyStringArrayPropertyParent() throws Exception {
        testNode.setProperty(propertyName2, sArray1);
        testRootNode.save();
        testNode.setProperty(propertyName2, sArray2);
        testRootNode.save();
        assertEquals("Modifying properties with Node.setProperty(String, String[]) and parentNode.save() not working",
                Arrays.asList(vArray2),
                Arrays.asList(testNode.getProperty(propertyName2).getValues()));
    }

    /**
     * Tests if <code>Node.setProperty(String, String[])</code> throws a {@link
     * javax.jcr.ValueFormatException} when trying to set an existing
     * single-valued property to a multi-value
     */
    public void testSetSingleStringArrayValueFormatException() throws Exception {
        // prerequisite: existing single-valued STRING property
        if (!testNode.hasProperty(propertyName1)) {
            testNode.setProperty(propertyName1, s1);
            testNode.getParent().save();
        }

        try {
            testNode.setProperty(propertyName1, sArray1);
            fail("setProperty(singleValueProperty, String[]) not throwing a ValueFormatException");
        } catch (ValueFormatException success) {
        }
    }

    /**
     * Tests if removing a <code>String[]</code> property with
     * <code>Node.setProperty(String, null)</code> works with
     * <code>Session.save()</code>
     */
    public void testRemoveStringArrayPropertySession() throws Exception {
        testNode.setProperty(propertyName2, sArray1);
        superuser.save();
        testNode.setProperty(propertyName2, (String[]) null);
        superuser.save();
        assertFalse("Removing property with Node.setProperty(String, (String[])null) and Session.save() not working",
                testNode.hasProperty(propertyName2));
    }

    /**
     * Tests if removing a <code>String[]</code> property with
     * <code>Node.setProperty(String, null)</code> works with
     * <code>parentNode.save()</code>
     */
    public void testRemoveStringArrayPropertyParent() throws Exception {
        testNode.setProperty(propertyName2, sArray1);
        testRootNode.save();
        testNode.setProperty(propertyName2, (String[]) null);
        testRootNode.save();
        assertFalse("Removing property with Node.setProperty(String, (String[])null) and parentNode.save() not working",
                testNode.hasProperty(propertyName2));
    }

    /**
     * Tests if <code>Node.setProperty(String, String[])</code> saves an array
     * of null values as an empty String[]
     */
    public void testSetNullStringArray() throws Exception {
        testNode.setProperty(propertyName2, sArrayNull);
        superuser.save();
        assertEquals("Node.setProperty(String, nullStringArray[]) did not set the property to an empty String[]",
                Arrays.asList(new Value[0]),
                Arrays.asList(testNode.getProperty(propertyName2).getValues()));
    }


    // String[] with PropertyType

    /**
     * Tests if adding properties with <code>Node.setProperty(String, String[],
     * int)</code> works with <code>Session.save()</code>
     */
    public void testNewStringArrayPropertySessionWithPropertyType() throws Exception {
        testNode.setProperty(propertyName2, sArray1, PropertyType.STRING);
        superuser.save();
        assertEquals("Setting properties with Node.setProperty(String, String[], int) and Session.save() not working",
                Arrays.asList(vArray1),
                Arrays.asList(testNode.getProperty(propertyName2).getValues()));
    }

    /**
     * Tests if modifying properties with <code>Node.setProperty(String,
     * String[], int)</code> works with <code>Session.save()</code>
     */
    public void testModifyStringArrayPropertySessionWithPropertyType() throws Exception {
        testNode.setProperty(propertyName2, sArray1, PropertyType.STRING);
        superuser.save();
        testNode.setProperty(propertyName2, sArray2, PropertyType.STRING);
        superuser.save();
        assertEquals("Modifying properties with Node.setProperty(String, String[], int) and Session.save() not working",
                Arrays.asList(vArray2),
                Arrays.asList(testNode.getProperty(propertyName2).getValues()));
    }

    /**
     * Tests if adding properties with <code>Node.setProperty(String, String[],
     * int)</code> works with <code>parentNode.save()</code>
     */
    public void testNewStringArrayPropertyParentWithPropertyType() throws Exception {
        testNode.setProperty(propertyName2, sArray1, PropertyType.STRING);
        testRootNode.save();
        assertEquals("Setting properties with Node.setProperty(String, String[], int) and parentNode.save() not working",
                Arrays.asList(vArray1),
                Arrays.asList(testNode.getProperty(propertyName2).getValues()));
    }

    /**
     * Tests if modifying properties with <code>Node.setProperty(String,
     * String[], int)</code> works with <code>parentNode.save()</code>
     */
    public void testModifyStringArrayPropertyParentWithPropertyType() throws Exception {
        testNode.setProperty(propertyName2, sArray1, PropertyType.STRING);
        testRootNode.save();
        testNode.setProperty(propertyName2, sArray2, PropertyType.STRING);
        testRootNode.save();
        assertEquals("Modifying properties with Node.setProperty(String, String[], int) and parentNode.save() not working",
                Arrays.asList(vArray2),
                Arrays.asList(testNode.getProperty(propertyName2).getValues()));
    }

    /**
     * Tests if <code>Node.setProperty(String, String[], int)</code> throws a
     * {@link javax.jcr.ValueFormatException} when trying to set an existing
     * single-value property to a multi-value
     */
    public void testSetSingleStringArrayValueFormatExceptionWithPropertyType() throws Exception {
        // prerequisite: existing single-valued STRING property
        if (!testNode.hasProperty(propertyName1)) {
            testNode.setProperty(propertyName1, s1);
            testNode.getParent().save();
        }

        try {
            testNode.setProperty(propertyName1, sArray1, PropertyType.STRING);
            fail("setProperty(singleValueProperty, String[], int) not throwing a ValueFormatException");
        } catch (ValueFormatException success) {
        }
    }

    /**
     * Tests if removing a <code>String[]</code> property with
     * <code>Node.setProperty(String, null, int)</code> works with
     * <code>Session.save()</code>
     */
    public void testRemoveStringArrayPropertySessionWithPropertyType() throws Exception {
        testNode.setProperty(propertyName2, sArray1, PropertyType.STRING);
        superuser.save();
        testNode.setProperty(propertyName2, (String[]) null, PropertyType.STRING);
        superuser.save();
        assertFalse("Removing property with Node.setProperty(String, (String[])null, int) and Session.save() not working",
                testNode.hasProperty(propertyName2));
    }

    /**
     * Tests if removing a <code>String[]</code> property with
     * <code>Node.setProperty(String, null, int)</code> works with
     * <code>parentNode.save()</code>
     */
    public void testRemoveStringArrayPropertyParentWithPropertyType() throws Exception {
        testNode.setProperty(propertyName2, sArray1, PropertyType.STRING);
        testRootNode.save();
        testNode.setProperty(propertyName2, (String[]) null, PropertyType.STRING);
        testRootNode.save();
        assertFalse("Removing property with Node.setProperty(String, (String[])null, int) and parentNode.save() not working",
                testNode.hasProperty(propertyName2));
    }

    /**
     * Tests if <code>Node.setProperty(String, String[], int)</code> saves an
     * array of null values as an empty String[]
     */
    public void testSetNullStringArrayWithPropertyType() throws Exception {
        testNode.setProperty(propertyName2, sArrayNull, PropertyType.STRING);
        superuser.save();
        assertEquals("Node.setProperty(String, nullStringArray[], int) did not set the property to an empty Value[]",
                Arrays.asList(new Value[0]),
                Arrays.asList(testNode.getProperty(propertyName2).getValues()));
    }

}
