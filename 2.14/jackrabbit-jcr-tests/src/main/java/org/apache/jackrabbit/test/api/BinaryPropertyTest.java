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

import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;

import javax.jcr.Binary;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

/**
 * Tests a binary property. If the workspace does not contain a node with a
 * binary property a {@link org.apache.jackrabbit.test.NotExecutableException}
 * is thrown.
 *
 */
public class BinaryPropertyTest extends AbstractPropertyTest {

    /**
     * Returns {@link javax.jcr.PropertyType#BINARY}.
     * @return {@link javax.jcr.PropertyType#BINARY}.
     */
    protected int getPropertyType() {
        return PropertyType.BINARY;
    }

    /**
     * Returns "does not matter" (<code>null</code>).
     * @return <code>null</code>.
     */
    protected Boolean getPropertyIsMultivalued() {
        return null;
    }

    /**
     * Tests that when Value.getStream() is called a second time the same Stream
     * object is returned. Also tests that when a new Value object is requested
     * also a new Stream object is returned by calling getStream() on the new
     * Value object.
     */
    public void testSameStream() throws RepositoryException, IOException {
        Value val = PropertyUtil.getValue(prop);
        InputStream in = val.getStream();
        InputStream in2 = val.getStream();
        Value otherVal = PropertyUtil.getValue(prop);
        InputStream in3 = otherVal.getStream();
        try {
            assertSame("Same InputStream object expected when " +
                    "Value.getStream is called twice.", in, in2);
            assertNotSame("Value.getStream() called on a new value " +
                    "object should return a different Stream object.", in, in3);
        } finally {
            // cleaning up
            try {
                in.close();
            } catch (IOException ignore) {}
            if (in2 != in) {
                try {
                    in2.close();
                } catch (IOException ignore) {}
            }
            if (in3 != in) {
                try {
                    in3.close();
                } catch (IOException ignore) {}
            }
        }
    }

    /**
     * Tests that when Binary.getStream() is called a second time a new stream
     * object is returned.
     */
    public void testSameStreamJcr2() throws RepositoryException, IOException {
        Value val = PropertyUtil.getValue(prop);
        Binary bin = val.getBinary();
        try {
            InputStream in = bin.getStream();
            InputStream in2 = bin.getStream();
            try {
                assertNotSame("Value.getStream() called on a new value " +
                        "object should return a different Stream object.", in, in2);
                //check if both streams can be read independently but contain the same bytes
                int n,n2;
                while ((n = in.read()) != -1) {
                    n2 = in2.read();
                    assertEquals("streams from the same binary object should have identical content", n, n2);
                }
                assertEquals("streams from the same binary object should have identical content", -1, in2.read());
            } finally {
                // cleaning up
                try {
                    in.close();
                } catch (IOException ignore) {}
                if (in2 != in) {
                    try {
                        in2.close();
                    } catch (IOException ignore) {}
                }
            }
        } finally {
            bin.dispose();
        }
    }

    /**
     * Tests the failure of calling Property.getStream() on a multivalue
     * property.
     */
    public void testMultiValue() throws RepositoryException, IOException {
        if (multiple) {
            InputStream in = null;
            try {
                in = prop.getStream();
                fail("Calling getStream() on a multivalue property " +
                        "should throw a ValueFormatException.");
            } catch (ValueFormatException vfe) {
                // ok
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        }
    }

    /**
     * Tests the failure of calling Property.getBinary() on a multivalue
     * property.
     */
    public void testMultiValueJcr2() throws RepositoryException, IOException {
        if (multiple) {
            try {
                prop.getBinary();
                fail("Calling getStream() on a multivalue property " +
                        "should throw a ValueFormatException.");
            } catch (ValueFormatException vfe) {
                // ok
            }
        }
    }

    /**
     * Tests that Property.getStream() delivers the same as Value.getStream().
     * We check this by reading each byte of the two streams and assuring that
     * they are equal.
     */
    public void testValue() throws IOException, RepositoryException {
        Value val = PropertyUtil.getValue(prop);
        InputStream in = val.getStream();
        InputStream in2;
        if (prop.getDefinition().isMultiple()) {
            // prop has at least one value (checked in #setUp())
            in2 = prop.getValues()[0].getStream();
        } else {
            in2 = prop.getStream();
        }
        try {
            int b = in.read();
            while (b != -1) {
                int b2 = in2.read();
                assertEquals("Value.getStream() and Property.getStream() " +
                        "return different values.", b, b2);
                b = in.read();
            }
            assertEquals("Value.getStream() and Property.getStream() " +
                    "return different values.", -1, in2.read());
        } finally {
            try {
                in.close();
            } catch (IOException ignore) {}
            try {
                in2.close();
            } catch (IOException ignore) {}
        }
    }

    /**
     * Tests that Value.getStream() delivers the same as Value.getBinary.getStream().
     * We check this by reading each byte of the two streams and assuring that
     * they are equal.
     */
    public void testValueJcr2() throws IOException, RepositoryException {
        Value val = PropertyUtil.getValue(prop);
        InputStream in = val.getStream();
        Binary bin = val.getBinary();
        try {
            InputStream in2 = bin.getStream();
            try {
                int b = in.read();
                while (b != -1) {
                    int b2 = in2.read();
                    assertEquals("Value.getStream() and Value.getBinary().getStream() " +
                            "return different values.", b, b2);
                    b = in.read();
                }
                assertEquals("Value.getStream() and Value.getBinary().getStream() " +
                        "return different values.", -1, in2.read());
            } finally {
                try {
                    in.close();
                } catch (IOException ignore) {}
                try {
                    in2.close();
                } catch (IOException ignore) {}
            }
        } finally {
            bin.dispose();
        }
    }

    /**
     * Tests conversion from Binary type to Boolean type. This is done via
     * String conversion.
     */
    public void testGetBoolean() throws RepositoryException {
        Value val = PropertyUtil.getValue(prop);
        String str = val.getString();
        boolean bool = val.getBoolean();
        assertEquals("Wrong conversion from Binary to Boolean.",
                new Boolean(bool), Boolean.valueOf(str));
    }

    /**
     * Tests conversion from Binary type to Date type. This is done via String
     * conversion.
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
     * Tests conversion from Binary type to Double type. This is done via String
     * conversion.
     */
    public void testGetDouble() throws RepositoryException {
        Value val = PropertyUtil.getValue(prop);
        String str = val.getString();
        // double
        try {
            Double.parseDouble(str);
            double d = val.getDouble();
            assertEquals("Wrong conversion from Binary to Double",
                    new Double(d), Double.valueOf(str));
        } catch (NumberFormatException nfe) {
            try {
                val.getDouble();
                fail("Conversion from malformed Binary to Double " +
                        "should throw ValueFormatException.");
            } catch (ValueFormatException vfe) {
                // ok
            }
        }
    }

    /**
     * Tests conversion from Binary type to Long type. This is done via String
     * conversion.
     */
    public void testGetLong() throws RepositoryException {
        Value val = PropertyUtil.getValue(prop);
        String str = val.getString();
        try {
            Long.parseLong(str);
            long l = val.getLong();
            assertEquals("Wrong conversion from Binary to Long",
                    new Long(l), Long.valueOf(str));
        } catch (NumberFormatException nfe) {
            try {
                val.getLong();
                fail("Conversion from malformed Binary to Long " +
                        "should throw ValueFormatException.");
            } catch (ValueFormatException vfe) {
                // ok
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
                PropertyUtil.checkGetType(prop, PropertyType.BINARY));
    }

    /**
     * Tests the conversion from Binary type to Reference or Path type. This conversion
     * passes through previous String conversion.
     */
    public void testGetNode() throws RepositoryException, NotExecutableException {
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
     * Tests the conversion from Binary type to Path type. This conversion
     * passes through previous String conversion.
     */
    public void testGetProperty() throws RepositoryException, NotExecutableException {
        if (!multiple) {
            // not testable since format of ID is implementation specific
        } else {
            try {
                prop.getProperty();
                fail("Property.getProperty() called on a multivalue property " +
                        "should throw a ValueFormatException.");
            } catch (ValueFormatException vfe) {
                // ok
            }
        }
    }
    
    /**
     * Tests the Property.getLength() method.
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
                long bytes = PropertyUtil.countBytes(prop.getValue());
                if (bytes != -1) {
                    assertEquals("Property.getLength() returns wrong number of bytes.",
                            bytes, length);
                }

            }
        }
    }

    /**
     * Tests the Binary.getSize() method.
     */
    public void testGetLengthJcr2() throws RepositoryException {
        Value val = PropertyUtil.getValue(prop);
        Binary binary = val.getBinary();
        long length;
        try {
            length = binary.getSize();
        } finally {
            binary.dispose();
        }
        long bytes = PropertyUtil.countBytes(val);
        if (bytes != -1) {
            assertEquals("Binary.getSize() returns wrong number of bytes.",
                    bytes, length);
        }
    }

    /**
     * Tests the Property.getLengths() method. The test is successful, if either
     * -1 is returned
     */
    public void testGetLengths() throws RepositoryException {
        if (multiple) {
            Value[] values = prop.getValues();
            long[] lengths = prop.getLengths();
            for (int i = 0; i < lengths.length; i++) {
                long length = PropertyUtil.countBytes(values[i]);
                assertEquals("Property.getLengths() returns " +
                        "wrong array of the lengths of a multivalue property.",
                        length, lengths[i]);
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
     * Tests the Binary.read() method.
     */
    public void testRandomAccess() throws RepositoryException, IOException {
        Value val = PropertyUtil.getValue(prop);
        Binary bin = val.getBinary();
        try {
            byte[] buf = new byte[0x1000];

            //verify that reading behind EOF returns -1
            assertEquals("reading behind EOF must return -1", -1, bin.read(buf, bin.getSize()));

            //read content using Binary.read()
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (int cnt, pos = 0; (cnt = bin.read(buf, pos)) > 0; pos += cnt) {
                out.write(buf, 0, cnt);
            }
            byte[] content = out.toByteArray();
            assertEquals("unexpected content length", bin.getSize(), content.length);

            //verify against stream
            InputStream in = val.getStream();
            try {
                int k = 0;
                for (int b; (b = in.read()) != -1; k++) {
                    assertEquals("Value.getStream().read() and Value.getBinary().read() " +
                            "return different values.", (byte) b, content[k]);
                }
                assertEquals("unexpected content length", k, content.length);
            } finally {
                try {
                    in.close();
                } catch (IOException ignore) {}
            }

            //verify random access
            buf = new byte[1];
            assertTrue("unexpected result of Value.getBinary.read()", -1 != bin.read(buf, 0));
            assertEquals("unexpected result of Value.getBinary.read()", content[0], buf[0]);
            if (content.length > 0) {
                assertTrue("unexpected result of Value.getBinary.read()", -1 != bin.read(buf, content.length - 1));
                assertEquals("unexpected result of Value.getBinary.read()", content[content.length - 1], buf[0]);
                assertTrue("unexpected result of Value.getBinary.read()", -1 != bin.read(buf, 0));
                assertEquals("unexpected result of Value.getBinary.read()", content[0], buf[0]);
            }
        } finally {
            bin.dispose();
        }
    }
    
}
