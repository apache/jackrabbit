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

import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.Value;
import java.io.IOException;
import java.io.BufferedInputStream;

/**
 * <code>StringPropertyTest</code> tests a String property against the
 * conversions to other Properties (except Name and Path property). If no String
 * property is found or only a multivalue String property with an empty array, a
 * NotExecutableException is thrown on setUp. More precisely, the tests are:
 * <p/>
 * - Value.getString() should return a string equals to Property.getString(),
 * and in case of a multivalue property the failure of Property.getString() is
 * checked.
 * <p/>
 * - Value.getBoolean() Conversion to Boolean property.
 * <p/>
 * - Value.getDate() Conversion to Date property is only valid when the String
 * follows the required Date pattern (6.2.5.1 of jsr170 specification).
 * <p/>
 * - Value.getDouble() Conversion to Double are only valid when the String
 * follows the correct patterns as required by the according Java classes.
 * <p/>
 * - Value.getLong() Conversion to Double are only valid when the String follows
 * the correct patterns as required by the according Java classes.
 * <p/>
 * - Value.getStream() Conversion to a Binary property follows the rules of
 * Value.getStream() as explained in chapter 6.2.7 of the jsr170 specification.
 * The required encoding is utf-8.
 * <p/>
 * - Property.getNode() Conversion to a Reference property is tested with
 * Property.getNode. The String should match the UUID pattern but this doesn't
 * guarantee to be a reference (which especially requires integrity).
 * <p/>
 * - Property.getLength() .
 * <p/>
 * - Property.getLengths() .
 * <p/>
 * - Property.getType() is compared to Value.getType() .
 *
 * @test
 * @sources StringPropertyTest.java
 * @executeClass org.apache.jackrabbit.test.api.StringPropertyTest
 * @keywords level1
 */
public class StringPropertyTest extends AbstractPropertyTest {

    /**
     * Returns {@link javax.jcr.PropertyType#STRING}.
     *
     * @return {@link javax.jcr.PropertyType#STRING}.
     */
    protected int getPropertyType() {
        return PropertyType.STRING;
    }

    /**
     * Returns "does not matter" (<code>null</code>).
     * @return <code>null</code>.
     */
    protected Boolean getPropertyIsMultivalued() {
        return null;
    }

    /**
     * Tests that Property.getString() delivers a string equal to the string
     * received with Value.getString().
     */
    public void testValue() throws RepositoryException {
        if (multiple) {
            try {
                prop.getString();
                fail("Property.getString() called on a multivalue property " +
                        "should throw a ValueFormatException.");
            } catch (ValueFormatException vfe) {
                // ok
            }
        } else {
            assertEquals("Value.getString() and Property.getString() return different values.",
                    prop.getValue().getString(), prop.getString());
        }
    }

    /**
     * Tests conversion from String type to Boolean type.
     */
    public void testGetBoolean() throws RepositoryException {
        Value val = PropertyUtil.getValue(prop);
        String str = val.getString();
        boolean bool = val.getBoolean();
        assertEquals("Wrong conversion from String to Boolean.",
                new Boolean(bool), Boolean.valueOf(str));
    }

    /**
     * Tests conversion from String type to Date type.
     */
    public void testGetDate() throws RepositoryException {
        Value val = PropertyUtil.getValue(prop);
        if (PropertyUtil.isDateFormat(val.getString())) {
            val.getDate();
        } else {
            try {
                val.getDate();
                fail("Conversion from a malformed String to a Date " +
                        "should throw a ValueFormatException.");
            } catch (ValueFormatException vfe) {
                // ok
            }
        }
    }

    /**
     * Tests conversion from String type to Double type.
     */
    public void testGetDouble() throws RepositoryException {
        Value val = PropertyUtil.getValue(prop);
        String str = val.getString();
        // double
        try {
            Double.parseDouble(str);
            double d = val.getDouble();
            assertEquals("Wrong conversion from String to Double.",
                    new Double(d), Double.valueOf(str));
        } catch (NumberFormatException nfe) {
            try {
                val.getDouble();
                fail("Conversion from malformed String to Double should throw ValueFormatException.");
            } catch (ValueFormatException vfe) {
                // ok
            }
        }
    }

    /**
     * Tests conversion from String type to Long type.
     */
    public void testGetLong() throws RepositoryException {
        Value val = PropertyUtil.getValue(prop);
        String str = val.getString();
        try {
            Long.parseLong(str);
            long l = val.getLong();
            assertEquals("Wrong conversion from String to Long.",
                    new Long(l), Long.valueOf(str));
        } catch (NumberFormatException nfe) {
            try {
                val.getLong();
                fail("Conversion from malformed String to Long " +
                        "should throw ValueFormatException.");
            } catch (ValueFormatException vfe) {
                // ok
            }
        }
    }

    /**
     * Tests conversion from String type to Binary type.
     */
    public void testGetStream() throws RepositoryException, IOException {
        Value val = PropertyUtil.getValue(prop);
        BufferedInputStream in = new BufferedInputStream(val.getStream());
        Value otherVal = PropertyUtil.getValue(prop);
        byte[] utf8bytes = otherVal.getString().getBytes(UTF8);
        // compare the bytearray with the bytes received from a Stream created with this String
        int i = 0;
        byte b[] = new byte[1];
        while (in.read(b) != -1) {
            assertEquals("String as a Stream is not utf-8 encoded", utf8bytes[i], b[0]);
            i++;
        }
        try {
            val.getString();
            fail("Non stream method call after stream method call " +
                    "should throw an IllegalStateException.");
        } catch (IllegalStateException ise) {
            //ok
        }
        try {
            otherVal.getStream();
            fail("Stream method call after a non stream method call " +
                    "should throw an IllegalStateException.");
        } catch (IllegalStateException ise) {
            // ok
        }
        in.close();
    }

    /**
     * Tests conversion from String type to Reference type.
     */
    public void testAsReference() throws RepositoryException, NotExecutableException {
        if (!multiple) {
            // not testable since format of ID is implementation specific
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
     * or it is the length of the string.
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
     * or the lengths of the according strings.
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

    /**
     * Tests if Value.getType() returns the same as Property.getType() and also
     * tests that prop.getDefinition().getRequiredType() returns the same type
     * in case it is not of Undefined type.
     */
    public void testGetType() throws RepositoryException {
        assertTrue("Value.getType() returns wrong type.",
                PropertyUtil.checkGetType(prop, PropertyType.STRING));
    }
}
