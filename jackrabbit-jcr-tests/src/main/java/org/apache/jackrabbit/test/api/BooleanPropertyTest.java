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

import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.InputStream;

/**
 * Tests a boolean property. If the workspace does not contain a node with a
 * boolean property a {@link org.apache.jackrabbit.test.NotExecutableException}
 * is thrown.
 *
 * @test
 * @sources BooleanPropertyTest.java
 * @executeClass org.apache.jackrabbit.test.api.BooleanPropertyTest
 * @keywords level1
 */
public class BooleanPropertyTest extends AbstractPropertyTest {

    /**
     * Returns {@link javax.jcr.PropertyType#BOOLEAN}.
     *
     * @return {@link javax.jcr.PropertyType#BOOLEAN}.
     */
    protected int getPropertyType() {
        return PropertyType.BOOLEAN;
    }

    /**
     * Returns "does not matter" (<code>null</code>).
     * @return <code>null</code>.
     */
    protected Boolean getPropertyIsMultivalued() {
        return null;
    }

    /**
     * Tests that Property.getBoolean() delivers the same as Value.getBoolean()
     * and that in case of a multivalue property Property.getBoolean() throws a
     * ValueFormatException.
     */
    public void testValue() throws RepositoryException {
        if (multiple) {
            try {
                prop.getBoolean();
                fail("Property.getBoolean() called on a multivalue property " +
                        "should throw a ValueFormatException.");
            } catch (ValueFormatException vfe) {
                // ok
            }
        } else {
            boolean bool = prop.getValue().getBoolean();
            boolean otherBool = prop.getBoolean();
            assertTrue("Value.getBoolean() and Property.getBoolean() " +
                    "return different values.", bool == otherBool);
        }
    }

    /**
     * Tests failure of conversion from Boolean type to Date type.
     */
    public void testGetDate() throws RepositoryException {
        try {
            Value val = PropertyUtil.getValue(prop);
            val.getDate();
            fail("Conversion from a Boolean value to a Date value " +
                    "should throw a ValueFormatException");
        } catch (ValueFormatException vfe) {
            //ok
        }
    }

    /**
     * Tests failure from Boolean type to Double type.
     */
    public void testGetDouble() throws RepositoryException {
        try {
            Value val = PropertyUtil.getValue(prop);
            val.getDouble();
            fail("Conversion from a Boolean value to a Double value " +
                    "should throw a ValueFormatException");
        } catch (ValueFormatException vfe) {
            //ok
        }
    }

    /**
     * Tests failure of conversion from Boolean type to Long type.
     */
    public void testGetLong() throws RepositoryException {
        try {
            Value val = PropertyUtil.getValue(prop);
            val.getLong();
            fail("Conversion from a Boolean value to a Long value " +
                    "should throw a ValueFormatException");
        } catch (ValueFormatException vfe) {
            //ok
        }
    }

    /**
     * Tests conversion from Boolean type to Binary type.
     */
    public void testGetStream() throws RepositoryException, IOException {
        Value val = PropertyUtil.getValue(prop);
        BufferedInputStream in = new BufferedInputStream(val.getStream());
        Value otherVal = PropertyUtil.getValue(prop);
        InputStream ins = null;
        byte[] utf8bytes = otherVal.getString().getBytes(UTF8);
        // if yet utf-8 encoded these bytes should be equal
        // to the ones received from the stream
        int i = 0;
        byte[] b = new byte[1];
        while (in.read(b) != -1) {
            assertTrue("Boolean as a Stream is not utf-8 encoded",
                    b[0] == utf8bytes[i]);
            i++;
        }
        try {
            val.getBoolean();
            fail("Non stream method call after stream method call " +
                    "should throw an IllegalStateException");
        } catch (IllegalStateException ise) {
            //ok
        }
        try {
            ins = otherVal.getStream();
            fail("Stream method call after a non stream method call " +
                    "should throw an IllegalStateException");
        } catch (IllegalStateException ise) {
            // ok
        } finally {
            if (in != null) {
                in.close();
            }
            if (ins != null) {
                ins.close();
            }
        }
    }

    /**
     * Tests the conversion from a Boolean to a String Value.
     */
    public void testGetString() throws RepositoryException {
        Value val = PropertyUtil.getValue(prop);
        String str = val.getString();
        String otherStr = new Boolean(val.getBoolean()).toString();
        assertEquals("Conversion from a Boolean value to a String value failed.",
                str, otherStr);
    }

    /**
     * Tests if Value.getType() returns the same as Property.getType() and also
     * tests that prop.getDefinition().getRequiredType() returns the same type
     * in case it is not of Undefined type.
     */
    public void testGetType() throws RepositoryException {
        assertTrue("Value.getType() returns wrong type.",
                PropertyUtil.checkGetType(prop, PropertyType.BOOLEAN));
    }

    /**
     * Tests failure of conversion from Boolean type to Reference type.
     */
    public void testAsReference() throws RepositoryException {
        if (!multiple) {
            try {
                prop.getNode();
                fail("Conversion from a Boolean value to a Reference value " +
                        "should throw a ValueFormatException");
            } catch (ValueFormatException vfe) {
                //ok
            }
        } else {
            try {
                prop.getNode();
                fail("Property.getNode() called on a multivalue property " +
                        "should throw a ValueFormatException.");
            } catch (ValueFormatException vfe) {
                // ok
            }
        }
    }

    /**
     * Tests the Property.getLength() method. The length returned is either -1
     * or it is the length of the string received by conversion.
     */
    public void testGetLength() throws RepositoryException {
        if (multiple) {
            try {
                prop.getLength();
                fail("Property.getLength() called on a multivalue property " +
                        "should throw a ValueFormatException.");

            } catch (ValueFormatException vfe) {
                // ok
            }
        } else {
            long length = prop.getLength();
            if (length > -1) {
                assertEquals("Property.getLength() returns wrong number of bytes.",
                        length, prop.getString().length());
            }
        }
    }

    /**
     * Tests the Property.getLengths() method. The returned values are either -1
     * or the lengths of the according conversions to strings.
     */
    public void testGetLengths() throws RepositoryException {
        if (multiple) {
            Value[] values = prop.getValues();
            long[] lengths = prop.getLengths();
            for (int i = 0; i < lengths.length; i++) {
                if (lengths[i] > -1) {
                    assertEquals("Property.getLengths() returns " +
                            "wrong array of the lengths of a multivalue property.",
                            values[i].getString().length(), lengths[i]);
                }
            }
        } else {
            try {
                prop.getLengths();
                fail("Property.getLengths() called on a sinlge value property " +
                        "should throw a ValueFormatException.");

            } catch (ValueFormatException vfe) {
                // ok
            }
        }
    }
}