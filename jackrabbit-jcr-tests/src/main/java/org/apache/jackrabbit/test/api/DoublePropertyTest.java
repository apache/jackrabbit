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
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.Value;
import java.util.Calendar;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Tests a double property. If the workspace does not contain a node with a
 * double property a {@link org.apache.jackrabbit.test.NotExecutableException}
 * is thrown.
 *
 * @test
 * @sources DoublePropertyTest.java
 * @executeClass org.apache.jackrabbit.test.api.DoublePropertyTest
 * @keywords level1
 */
public class DoublePropertyTest extends AbstractPropertyTest {

    /**
     * Returns {@link javax.jcr.PropertyType#DOUBLE}.
     *
     * @return {@link javax.jcr.PropertyType#DOUBLE}.
     */
    protected int getPropertyType() {
        return PropertyType.DOUBLE;
    }

    /**
     * Returns "does not matter" (<code>null</code>).
     * @return <code>null</code>.
     */
    protected Boolean getPropertyIsMultivalued() {
        return null;
    }

    /**
     * tests that Property.getDouble() delivers the same as Value.getDouble()
     * and if in case of a multivalue property a ValueFormatException is
     * thrown.
     */
    public void testValue() throws RepositoryException {
        if (multiple) {
            try {
                prop.getDouble();
                fail("Property.getDouble() called on a multivalue property " +
                        "should throw a ValueFormatException.");
            } catch (ValueFormatException vfe) {
                // ok
            }
        } else {
            double d = prop.getValue().getDouble();
            double dd = prop.getDouble();
            assertTrue("Value.getDouble() and Property.getDouble() return different values.", d == dd);
        }
    }

    /**
     * tests failure of conversion from Double type to Boolean type
     */
    public void testGetBoolean() throws RepositoryException {
        try {
            Value val = PropertyUtil.getValue(prop);
            val.getBoolean();
            fail("Conversion from a Double value to a Boolean value " +
                    "should throw a ValueFormatException.");
        } catch (ValueFormatException vfe) {
            //ok
        }
    }

    /**
     * tests conversion from Double type to Date type
     */
    public void testGetDate() throws RepositoryException {
        Value val = PropertyUtil.getValue(prop);
        Calendar calendar = val.getDate();
        assertEquals("Conversion from Double value to Date value is not correct.",
                calendar.getTimeInMillis(), new Double(val.getDouble()).longValue());
    }

    /**
     * tests the conversion from a Double to a Long Value
     */
    public void testGetLong() throws RepositoryException {
        Value val = PropertyUtil.getValue(prop);
        long l = val.getLong();
        long ll = new Double(val.getDouble()).longValue();
        assertTrue("Conversion from Double value to Long value is not correct.", l == ll);
    }

    /**
     * tests conversion from Double type to Binary type
     */
    public void testGetStream() throws RepositoryException, IOException {
        Value val = PropertyUtil.getValue(prop);
        BufferedInputStream in = new BufferedInputStream(val.getStream());
        Value otherVal = PropertyUtil.getValue(prop);
        InputStream ins = null;
        byte[] utf8bytes = otherVal.getString().getBytes();
        // if yet utf-8 encoded these bytes should be equal
        // to the ones received from the stream
        int i = 0;
        byte b = (byte) in.read();
        while (b != -1) {
            assertTrue("Double as a Stream is not utf-8 encoded.",
                    b == utf8bytes[i]);
            b = (byte) in.read();
            i++;
        }
        try {
            val.getDouble();
            fail("Non stream method call after stream method call " +
                    "should throw an IllegalStateException.");
        } catch (IllegalStateException ise) {
            //ok
        }
        try {
            ins = otherVal.getStream();
            fail("Stream method call after a non stream method call " +
                    "should throw an IllegalStateException.");
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
     * tests the conversion from a Double to a String Value
     */
    public void testGetString() throws RepositoryException {
        Value val = PropertyUtil.getValue(prop);
        String str = val.getString();
        String otherStr = Double.toString(val.getDouble());
        assertEquals("Conversion from Double value to String value is not correct.", str, otherStr);
    }

    /**
     * Tests if Value.getType() returns the same as Property.getType() and also
     * tests that prop.getDefinition().getRequiredType() returns the same type
     * in case it is not of Undefined type.
     */
    public void testGetType() throws RepositoryException {
        assertTrue("Value.getType() returns wrong type.",
                PropertyUtil.checkGetType(prop, PropertyType.DOUBLE));
    }

    /**
     * tests failure of conversion from Double type to Reference type
     */
    public void testAsReference() throws RepositoryException {
        if (!multiple) {
            try {
                prop.getNode();
                fail("Conversion from a Double value to a Reference value " +
                        "should throw a ValueFormatException.");
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