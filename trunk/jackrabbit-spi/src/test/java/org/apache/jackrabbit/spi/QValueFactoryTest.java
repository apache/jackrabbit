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
package org.apache.jackrabbit.spi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;

/** <code>QValueFactoryTest</code>... */
public class QValueFactoryTest extends AbstractSPITest {

    private static Logger log = LoggerFactory.getLogger(QValueFactoryTest.class);

    protected QValueFactory factory;

    private final Calendar calendar = Calendar.getInstance();
    protected Path rootPath;
    protected Name testName;
    protected String reference;

    protected void setUp() throws Exception {
        super.setUp();
        RepositoryService service = helper.getRepositoryService();
        factory = service.getQValueFactory();

        rootPath = service.getPathFactory().getRootPath();
        testName = service.getNameFactory().create(Name.NS_JCR_URI, "data");
        reference = getProperty("reference");
    }

    private static void assertValueLength(QValue v, long expectedLength) throws RepositoryException {
        long length = v.getLength();
        if (length != -1) {
            assertEquals(expectedLength, length);
        }
    }

    public void testIllegalType() throws RepositoryException {
        try {
            factory.create("any", 54);
            fail("54 is not a valid property type");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    //-------------------------------------------------------------< DOUBLE >---

    public void testCreateInvalidDoubleValue() throws RepositoryException {
        try {
            factory.create("any", PropertyType.DOUBLE);
            fail("'any' cannot be converted to a valid double value.");
        } catch (ValueFormatException e) {
            // ok
        }
    }

    public void testGetDoubleOnBooleanValue() throws RepositoryException {
        try {
            QValue v = factory.create(true);
            v.getDouble();
            fail("'true' cannot be converted to a valid double value.");
        } catch (ValueFormatException e) {
            // ok
        }
    }

    //---------------------------------------------------------------< LONG >---

    public void testCreateInvalidLongValue() throws RepositoryException {
        try {
            factory.create("any", PropertyType.LONG);
            fail("'any' cannot be converted to a valid long value.");
        } catch (ValueFormatException e) {
            // ok
        }
    }

    public void testGetLongOnBooleanValue() throws RepositoryException {
        try {
            QValue v = factory.create(true);
            v.getLong();
            fail("'true' cannot be converted to a valid long value.");
        } catch (ValueFormatException e) {
            // ok
        }
    }

    //------------------------------------------------------------< BOOLEAN >---
    /**
     * QValueImpl has a final static constant for the TRUE and the FALSE boolean
     * values. Test if the various create methods use the constants (thus always
     * return the 'same' object.
     *
     * @throws RepositoryException
     */
    public void testFinalBooleanValue() throws RepositoryException {
        assertSame(factory.create(true), factory.create(Boolean.TRUE.toString(), PropertyType.BOOLEAN));
        assertSame(factory.create(true), factory.create(true));

        assertSame(factory.create(false), factory.create(Boolean.FALSE.toString(), PropertyType.BOOLEAN));
        assertSame(factory.create(false), factory.create(false));
    }

    /**
     * Test if creating Boolean QValue from boolean and from String with boolean
     * type return equal objects.
     *
     * @throws RepositoryException
     */
    public void testCreateBooleanValueFromString() throws RepositoryException {
        QValue v = factory.create(Boolean.TRUE.toString(), PropertyType.BOOLEAN);
        assertEquals("Creating boolean type QValue from boolean or String must be equal.",
                factory.create(true), v);

        v = factory.create(Boolean.FALSE.toString(), PropertyType.BOOLEAN);
        assertEquals("Creating boolean type QValue from boolean or String must be equal.",
                factory.create(false), v);
    }

    public void testCreateTrueBooleanValue() throws RepositoryException {
        QValue v = factory.create(true);
        assertEquals("Boolean value must be true", Boolean.TRUE.toString(), v.getString());
        assertEquals("Boolean value must be true", true, v.getBoolean());
    }

    public void testCreateFalseBooleanValue() throws RepositoryException {
        QValue v = factory.create(false);
        assertEquals("Boolean value must be false", Boolean.FALSE.toString(), v.getString());
        assertEquals("Boolean value must be false", false, v.getBoolean());
    }

    public void testCreateTrueFromString() throws ValueFormatException, RepositoryException {
        QValue v = factory.create(Boolean.TRUE.toString(), PropertyType.STRING);
        assertEquals("Boolean value must be true", true, v.getBoolean());
    }

    public void testCreateFalseFromString() throws ValueFormatException, RepositoryException {
        QValue v = factory.create("any", PropertyType.STRING);
        assertEquals("Boolean value must be false", false, v.getBoolean());
    }

    public void testReadBooleanAsLong() throws RepositoryException {
        try {
            QValue v = factory.create(true);
            v.getLong();
        }
        catch (ValueFormatException e) {
            return; // ok
        }
        assertTrue("Cannot convert value to long", false);
    }

    //---------------------------------------------------------------< DATE >---
    public void testNullDateValue() throws IOException, RepositoryException {
        try {
            factory.create((Calendar) null);
            fail();
        } catch (IllegalArgumentException e) {
          // ok
        } catch (NullPointerException e) {
          // ok
        }
        try {
            factory.create(null, PropertyType.DATE);
            fail();
        } catch (IllegalArgumentException e) {
          // ok
        } catch (NullPointerException e) {
          // ok
        }
    }

    public void testDateValueType() throws RepositoryException {
        QValue v = factory.create(calendar);
        assertTrue("Type of a date value must be PropertyType.DATE", v.getType() == PropertyType.DATE);
    }
    public void testDateValueEquality() throws RepositoryException {
        QValue v = factory.create(calendar);
        QValue otherV = factory.create(calendar);
        assertEquals("Equality of date value must be calculated based on their String representation.", v, otherV);
    }

    public void testDateValueEquality2() throws RepositoryException {
        QValue v = factory.create(calendar);
        QValue otherV = factory.create(v.getString(), PropertyType.DATE);
        assertEquals("Equality of date value must be calculated based on their String representation.", v, otherV);
    }

    //----------------------------------------------------------< REFERENCE >---

    public void testNullReferenceValue() throws IOException, RepositoryException {
        try {
            factory.create(null, PropertyType.REFERENCE);
            fail();
        } catch (IllegalArgumentException e) {
          // ok
        } catch (NullPointerException e) {
          // ok
        }
    }

    public void testReferenceValueType() throws RepositoryException {
        if (reference != null) {
            QValue v = factory.create(reference, PropertyType.REFERENCE);
            assertTrue("Type of a date value must be PropertyType.REFERENCE.", v.getType() == PropertyType.REFERENCE);
        } else {
            log.warn("Configuration entry 'QValueFactoryTest.reference' is missing -> skip test 'testReferenceValueType'.");
        }
    }

    public void testReferenceValueEquality() throws RepositoryException {
        if (reference != null) {
            QValue v = factory.create(reference, PropertyType.REFERENCE);
            QValue otherV = factory.create(reference, PropertyType.REFERENCE);
            assertEquals("Reference values created from the same string must be equal.", v, otherV);
        } else {
            log.warn("Configuration entry 'QValueFactoryTest.reference' is missing -> skip test 'testReferenceValueEquality'.");
        }
    }

    public void testEqualityDifferentTypes() throws RepositoryException {
        if (reference != null) {
            QValue v = factory.create(reference, PropertyType.REFERENCE);
            QValue v2 = factory.create(reference, PropertyType.STRING);
            assertFalse(v.equals(v2));
        } else {
            log.warn("Configuration entry 'QValueFactoryTest.reference' is missing -> skip test 'testEqualityDifferentTypes'.");
        }
    }


    //---------------------------------------------------------------< Name >---

    public void testNullNameValue() throws IOException, RepositoryException {
        try {
            factory.create((Name) null);
            fail();
        } catch (IllegalArgumentException e) {
          // ok
        } catch (NullPointerException e) {
          // ok
        }
    }

    public void testNameValueType() throws IOException, RepositoryException {
        QValue v = factory.create(testName);
        assertTrue(v.getType() == PropertyType.NAME);
        v = factory.create(testName.toString(), PropertyType.NAME);
        assertTrue(v.getType() == PropertyType.NAME);
    }

    public void testNameValueEquality() throws IOException, RepositoryException {
        QValue v = factory.create(testName);
        QValue v2 = factory.create(testName.toString(), PropertyType.NAME);
        assertTrue(v.equals(v2));
    }

    public void testNameValueGetString() throws IOException, RepositoryException {
        QValue v = factory.create(testName);
        assertTrue(v.getString().equals(testName.toString()));
    }

    public void testNameValueGetName() throws RepositoryException {
        QValue v = factory.create(testName);
        assertTrue(v.getName().equals(testName));
    }

    public void testInvalidNameValue() throws RepositoryException {
        try {
            factory.create("abc", PropertyType.NAME);
            fail("'abc' is not a valid Name -> creating QValue should fail.");
        } catch (ValueFormatException e) {
            // ok
        }
    }

    public void testAnyValueGetName() throws RepositoryException {
        try {
            factory.create(12345).getName();
            fail("12345 is not a valid Name value -> QValue.getName() should fail.");
        } catch (ValueFormatException e) {
            // ok
        }
    }

    //---------------------------------------------------------------< Path >---

    public void testNullPathValue() throws IOException, RepositoryException {
        try {
            factory.create((Path) null);
            fail();
        } catch (IllegalArgumentException e) {
          // ok
        } catch (NullPointerException e) {
          // ok
        }
    }

    public void testPathValueType() throws IOException, RepositoryException {
        QValue v = factory.create(rootPath);
        assertTrue(v.getType() == PropertyType.PATH);
        v = factory.create(rootPath.toString(), PropertyType.PATH);
        assertTrue(v.getType() == PropertyType.PATH);
    }


    public void testPathValueEquality() throws IOException, RepositoryException {
        QValue v = factory.create(rootPath);
        QValue v2 = factory.create(rootPath.toString(), PropertyType.PATH);
        assertTrue(v.equals(v2));
    }

    public void testPathValueGetString() throws IOException, RepositoryException {
        QValue v = factory.create(rootPath);
        assertTrue(v.getString().equals(rootPath.toString()));
    }

    public void testPathValueGetPath() throws RepositoryException {
        QValue v = factory.create(rootPath);
        assertTrue(v.getPath().equals(rootPath));
    }

    public void testInvalidPathValue() throws RepositoryException {
        try {
            factory.create("abc", PropertyType.PATH);
            fail("'abc' is not a valid Path -> creating QValue should fail.");
        } catch (ValueFormatException e) {
            // ok
        }
    }

    public void testAnyValueGetPath() throws RepositoryException {
        try {
            factory.create(12345).getPath();
            fail("12345 is not a valid Path value -> QValue.getPath() should fail.");
        } catch (ValueFormatException e) {
            // ok
        }
    }

    //-------------------------------------------------------------< BINARY >---

    public void testNullBinaryValue() throws IOException, RepositoryException {
        try {
            factory.create((byte[]) null);
            fail();
        } catch (IllegalArgumentException e) {
          // ok
        } catch (NullPointerException e) {
          // ok
        }
        try {
            factory.create((InputStream) null);
            fail();
        } catch (IllegalArgumentException e) {
          // ok
        } catch (NullPointerException e) {
          // ok
        }
        try {
            factory.create((File) null);
            fail();
        } catch (IllegalArgumentException e) {
          // ok
        } catch (NullPointerException e) {
          // ok
        }
    }

    public void testBinaryValueType() throws IOException, RepositoryException {
        QValue v = factory.create(new byte[] {'a', 'b', 'c'});
        assertTrue(v.getType() == PropertyType.BINARY);
    }


    public void testBinaryFromByteArray() throws RepositoryException, IOException {
        QValue v = factory.create(new byte[] {'a', 'b', 'c'});

        assertEquals(PropertyType.BINARY, v.getType());
        assertValueLength(v, 3);

        assertEquals("abc", v.getString());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        spool(out, v.getStream());
        assertEquals("abc", new String(out.toByteArray()));
    }

    public void testEmptyBinaryFromByteArray() throws RepositoryException, IOException {
        QValue v = factory.create(new byte[0]);

        assertEquals(PropertyType.BINARY, v.getType());
        assertValueLength(v, 0);

        assertEquals("", v.getString());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        spool(out, v.getStream());
        assertEquals("", new String(out.toByteArray()));
    }

    public void testBinaryFromInputStream() throws RepositoryException, IOException {
        InputStream in = new ByteArrayInputStream(new byte[] {'a', 'b', 'c'});

        QValue v = factory.create(in);

        assertEquals(PropertyType.BINARY, v.getType());
        assertValueLength(v, 3);

        assertEquals("abc", v.getString());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        spool(out, v.getStream());
        assertEquals("abc", new String(out.toByteArray()));
    }

    public void testEmptyBinaryFromInputStream() throws RepositoryException, IOException {
        InputStream in = new ByteArrayInputStream(new byte[0]);

        QValue v = factory.create(in);

        assertEquals(PropertyType.BINARY, v.getType());
        assertValueLength(v, 0);

        assertEquals("", v.getString());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        spool(out, v.getStream());
        assertEquals("", new String(out.toByteArray()));
    }


    public void testBinaryFromFile() throws RepositoryException, IOException {
        File f = File.createTempFile("QValueFactoryImplTest", ".txt");
        f.deleteOnExit();
        FileWriter fw = new FileWriter(f);
        fw.write("abc");
        fw.close();

        QValue v = factory.create(f);

        assertEquals(PropertyType.BINARY, v.getType());
        assertValueLength(v, 3);

        assertEquals("abc", v.getString());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        spool(out, v.getStream());
        assertEquals("abc", new String(out.toByteArray()));
    }

    public void testEmptyBinaryFromFile() throws RepositoryException, IOException {
        File f = File.createTempFile("QValueFactoryImplTest", ".txt");
        f.deleteOnExit();

        QValue v = factory.create(f);

        assertEquals(PropertyType.BINARY, v.getType());
        assertValueLength(v, 0);

        assertEquals("", v.getString());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        spool(out, v.getStream());
        assertEquals("", new String(out.toByteArray()));
    }

    /**
     *
     * @param out
     * @param in
     * @throws RepositoryException
     * @throws IOException
     */
    private static void spool(OutputStream out, InputStream in) throws RepositoryException, IOException {
        try {
            byte[] buffer = new byte[0x2000];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
        } finally {
            try {
                in.close();
            } catch (IOException ignore) {
            }
        }
    }
}