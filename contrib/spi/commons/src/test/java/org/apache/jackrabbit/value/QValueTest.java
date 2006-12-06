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
package org.apache.jackrabbit.value;

import junit.framework.TestCase;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import java.util.Calendar;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;

import org.apache.jackrabbit.util.ISO8601;
import org.apache.jackrabbit.uuid.UUID;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.Path;

/**
 * <code>QValueTest</code>...
 */
public class QValueTest extends TestCase {

    private final Calendar CALENDAR = Calendar.getInstance();
    private static final String REFERENCE = UUID.randomUUID().toString();

    //---------------------------------------------------------------< DATE >---
    public void testNullDateValue() throws IOException {
        try {
            QValue.create((Calendar) null);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            QValue.create((String) null, PropertyType.DATE);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            QValue.create(new String[] {null}, PropertyType.DATE);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            QValue.create((InputStream) null, PropertyType.DATE);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            QValue.create(new InputStream[] {null}, PropertyType.DATE);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    public void testDateValueType() {
        QValue v = QValue.create(CALENDAR);
        assertTrue("Type of a date value must be PropertyType.DATE", v.getType() == PropertyType.DATE);
    }
    public void testDateValueEquality() {
        QValue v = QValue.create(CALENDAR);
        QValue otherV = QValue.create(CALENDAR);
        assertEquals("Equality of qualified date value must be calculated based on their String representation.", v, otherV);
    }

    public void testDateValueEquality2() throws RepositoryException {
        QValue v = QValue.create(CALENDAR);
        QValue otherV = QValue.create(v.getString(), PropertyType.DATE);
        assertEquals("Equality of qualified date value must be calculated based on their String representation.", v, otherV);
    }

    public void testDateValueStringRepresentation() throws RepositoryException {
        QValue v = QValue.create(CALENDAR);
        String s = ISO8601.format(CALENDAR);
        assertEquals("Expected String representation of qualified date value to be ISO8601 compliant.", s, v.getString());
    }

    public void testDateValueCopy() throws RepositoryException {
        QValue v = QValue.create(CALENDAR);
        QValue copy = v.createCopy();
        assertTrue(copy.getType() == PropertyType.DATE);
        assertNotSame(v, copy);
        assertEquals(v, copy);
    }

    //----------------------------------------------------------< REFERENCE >---
    public void testNullReferenceValue() throws IOException {
        try {
            QValue.create((UUID) null);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            QValue.create((String) null, PropertyType.REFERENCE);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            QValue.create(new String[] {null}, PropertyType.REFERENCE);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            QValue.create((InputStream) null, PropertyType.REFERENCE);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            QValue.create(new InputStream[] {null}, PropertyType.REFERENCE);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    public void testReferenceValueType() {
        QValue v = QValue.create(REFERENCE, PropertyType.REFERENCE);
        assertTrue("Type of a date value must be PropertyType.REFERENCE.", v.getType() == PropertyType.REFERENCE);
    }

    public void testReferenceValueEquality() {
        QValue v = QValue.create(REFERENCE, PropertyType.REFERENCE);
        QValue otherV = QValue.create(REFERENCE, PropertyType.REFERENCE);
        assertEquals("Qualified ref values created from the same string must be equal.", v, otherV);
    }

    public void testReferenceValueCopy() throws RepositoryException {
        QValue v = QValue.create(REFERENCE, PropertyType.REFERENCE);
        QValue copy = v.createCopy();
        assertTrue(copy.getType() == PropertyType.REFERENCE);
        assertNotSame(v, copy);
        assertEquals(v, copy);
    }

    public void testEqualityDifferentTypes() {
        QValue v = QValue.create(REFERENCE, PropertyType.REFERENCE);
        QValue v2 = QValue.create(REFERENCE, PropertyType.STRING);
        assertFalse(v.equals(v2));
    }


    //--------------------------------------------------------------< QName >---
    public void testNullQNameValue() throws IOException {
        try {
            QValue.create((QName) null);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            QValue.create(new QName[] {null});
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    public void testQNameValueType() throws IOException {
        QValue v = QValue.create(QName.JCR_DATA);
        assertTrue(v.getType() == PropertyType.NAME);
        v = QValue.create(QName.JCR_DATA.toString(), PropertyType.NAME);
        assertTrue(v.getType() == PropertyType.NAME);
    }

    //--------------------------------------------------------------< QPath >---
    public void testNullPathValue() throws IOException {
        try {
            QValue.create((Path) null);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    public void testPathValueType() throws IOException {
        QValue v = QValue.create(Path.ROOT);
        assertTrue(v.getType() == PropertyType.PATH);
        v = QValue.create(Path.ROOT.toString(), PropertyType.PATH);
        assertTrue(v.getType() == PropertyType.PATH);
    }

    //-------------------------------------------------------------< BINARY >---
    public void testNullBinaryValue() throws IOException {
        try {
            QValue.create((byte[]) null);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            QValue.create((InputStream) null);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            QValue.create((File) null);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    public void testBinaryValueType() throws IOException {
        QValue v = QValue.create(new byte[] {'a', 'b', 'c'});
        assertTrue(v.getType() == PropertyType.BINARY);
    }
}