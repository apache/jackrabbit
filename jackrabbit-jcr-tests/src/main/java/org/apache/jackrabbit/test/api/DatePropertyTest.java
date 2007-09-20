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
import java.util.Calendar;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.InputStream;

/**
 * Tests a date property. If the workspace does not contain a node with a date
 * property a {@link org.apache.jackrabbit.test.NotExecutableException} is
 * thrown.
 *
 * @test
 * @sources DatePropertyTest.java
 * @executeClass org.apache.jackrabbit.test.api.DatePropertyTest
 * @keywords level1
 */
public class DatePropertyTest extends AbstractPropertyTest {

    /**
     * Returns {@link javax.jcr.PropertyType#DATE}.
     * @return {@link javax.jcr.PropertyType#DATE}.
     */
    protected int getPropertyType() {
        return PropertyType.DATE;
    }

    /**
     * Returns "does not matter" (<code>null</code>).
     * @return <code>null</code>.
     */
    protected Boolean getPropertyIsMultivalued() {
        return null;
    }

    /**
     * Tests that Property.getDate() delivers the same as Value.getDate() and
     * that in case of a multivalue property a ValueFormatException is thrown.
     */
    public void testValue() throws RepositoryException {
        if (multiple) {
            try {
                prop.getDate();
                fail("Property.getDate() called on a multivalue property " +
                        "should throw a ValueFormatException.");
            } catch (ValueFormatException vfe) {
                // ok
            }
        } else {
            Calendar calendar = prop.getValue().getDate();
            Calendar calendar2 = prop.getDate();
            assertEquals("Value.getDate() and Property.getDate() return different values.",
                    calendar, calendar2);
        }
    }

    /**
     * Tests if a calendar is returned and if the conversion to a string has
     * correct format.
     */
    public void testGetString() throws RepositoryException {
        Value val = PropertyUtil.getValue(prop);
        // correct format:  YYYY-MM-DDThh:mm:ss.sssTZD
        // month(01-12), day(01-31), hours(00-23), minutes(00-59), seconds(00-59),
        // TZD(Z or +hh:mm or -hh:mm)
        // String aDay="2005-01-19T15:34:15.917+01:00";
        String date = val.getString();
        log.println("date str = " + date);
        boolean match = PropertyUtil.isDateFormat(prop.getString());
        assertTrue("Date not in correct String format.", match);
    }

    /**
     * Tests failure of conversion from Date type to Boolean type.
     */
    public void testGetBoolean() throws RepositoryException {
        try {
            Value val = PropertyUtil.getValue(prop);
            val.getBoolean();
            fail("Conversion from a Date value to a Boolean value " +
                    "should throw a ValueFormatException.");
        } catch (ValueFormatException vfe) {
            //ok
        }
    }

    /**
     * Tests conversion from Date type to Double type.
     */
    public void testGetDouble() throws RepositoryException {
        Value val = PropertyUtil.getValue(prop);
        double d = val.getDouble();
        long mili = val.getDate().getTimeInMillis();
        assertEquals("Conversion from a Date value to a Double value " +
                "returns a different number of miliseconds.", mili, (long) d);
    }

    /**
     * Tests conversion from Date type to Long type.
     */
    public void testGetLong() throws RepositoryException {
        Value val = PropertyUtil.getValue(prop);
        long l = val.getLong();
        long mili = val.getDate().getTimeInMillis();
        assertEquals("Conversion from a Date value to a Long value " +
                "returns a different number of miliseconds.", mili, l);
    }

    /**
     * Tests conversion from Date type to Binary type.
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
            assertTrue("Date as a Stream is not utf-8 encoded.",
                    b[0] == utf8bytes[i]);
            i++;
        }
        try {
            val.getDate();
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
     * Tests if Value.getType() returns the same as Property.getType() and
     * also tests that prop.getDefinition().getRequiredType() returns the same
     * type in case it is not of Undefined type.
     */
    public void testGetType() throws RepositoryException {
        assertTrue("Value.getType() returns wrong type.",
                PropertyUtil.checkGetType(prop, PropertyType.DATE));
    }


    /**
     * Tests failure of conversion from Date type to Reference type.
     */
    public void testAsReference() throws RepositoryException {
        if (!multiple) {
            try {
                prop.getNode();
                fail("Conversion from a Date value to a Reference value " +
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