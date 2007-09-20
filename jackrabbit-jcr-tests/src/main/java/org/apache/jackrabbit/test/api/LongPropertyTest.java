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
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.InputStream;

/**
 * Tests a long property. If the workspace does not contain a node with a long
 * property a {@link org.apache.jackrabbit.test.NotExecutableException} is
 * thrown.
 *
 * @test
 * @sources LongPropertyTest.java
 * @executeClass org.apache.jackrabbit.test.api.LongPropertyTest
 * @keywords level1
 */
public class LongPropertyTest extends AbstractPropertyTest {

    /**
     * Returns {@link javax.jcr.PropertyType#LONG}.
     *
     * @return {@link javax.jcr.PropertyType#LONG}.
     */
    protected int getPropertyType() {
        return PropertyType.LONG;
    }

    /**
     * Returns "does not matter" (<code>null</code>).
     * @return <code>null</code>.
     */
    protected Boolean getPropertyIsMultivalued() {
        return null;
    }

    /**
     * Tests that Property.getLong() delivers the same as Value.getLong() and if
     * in case of a multivalue property a ValueFormatException is thrown.
     */
    public void testValue() throws RepositoryException {
        if (multiple) {
            try {
                prop.getLong();
                fail("Property.getLong() called on a multivalue property " +
                        "should throw a ValueFormatException.");
            } catch (ValueFormatException vfe) {
                // ok
            }
        } else {
            long l = prop.getValue().getLong();
            long ll = prop.getLong();
            assertEquals("Value.getLong() and Property.getLong() return different values.", l, ll);
        }
    }

    /**
     * Tests failure of conversion from Long type to Boolean type.
     */
    public void testGetBoolean() throws RepositoryException {
        try {
            Value val = PropertyUtil.getValue(prop);
            val.getBoolean();
            fail("Conversion from a Long value to a Boolean value " +
                    "should throw a ValueFormatException.");
        } catch (ValueFormatException vfe) {
            //ok
        }
    }

    /**
     * Tests conversion from Long type to Date type.
     */
    public void testGetDate() throws RepositoryException {
        Value val = PropertyUtil.getValue(prop);
        Calendar calendar = val.getDate();
        assertEquals("Conversion from Long value to Date value is not correct.",
                val.getLong(), calendar.getTimeInMillis());
    }

    /**
     * Tests conversion from Long type to Double type.
     */
    public void testGetDouble() throws RepositoryException {
        Value val = PropertyUtil.getValue(prop);
        double d = val.getDouble();
        assertEquals("Conversion from Long value to Double value is not correct.",
                new Long(val.getLong()).doubleValue(), d, 0d);
    }

    /**
     * Tests conversion from Long type to Binary type.
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
        byte b[] = new byte[1];
        while (in.read(b) != -1) {
            assertEquals("Long as a Stream is not utf-8 encoded.",
                    utf8bytes[i], b[0]);
            i++;
        }
        try {
            val.getLong();
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
     * Tests the conversion from a Long to a String Value.
     */
    public void testGetString() throws RepositoryException {
        Value val = PropertyUtil.getValue(prop);
        String str = val.getString();
        String otherStr = new Long(val.getLong()).toString();
        assertEquals("Conversion from a Long value to a String value is not correct",
                str, otherStr);
    }

    /**
     * Tests if Value.getType() returns the same as Property.getType() and also
     * tests that prop.getDefinition().getRequiredType() returns the same type
     * in case it is not of Undefined type.
     */
    public void testGetType() throws RepositoryException {
        assertTrue("Value.getType() returns wrong type.",
                PropertyUtil.checkGetType(prop, PropertyType.LONG));
    }

    /**
     * Tests failure of conversion from Long type to Reference type.
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