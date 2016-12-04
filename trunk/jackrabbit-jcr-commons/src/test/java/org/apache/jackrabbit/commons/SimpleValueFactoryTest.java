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
package org.apache.jackrabbit.commons;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.TimeZone;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;

import junit.framework.TestCase;

/**
 * Test cases for the {@link SimpleValueFactory} class.
 */
public class SimpleValueFactoryTest extends TestCase {

    private final ValueFactory factory = new SimpleValueFactory();

    public void testBoolean() throws RepositoryException {
        Value value = factory.createValue(true);

        assertEquals(PropertyType.BOOLEAN, value.getType());
        assertEquals(value, factory.createValue(true));

        assertTrue(value.getBoolean());
        try { value.getDate(); fail(); } catch (ValueFormatException e) {}
        try { value.getDecimal(); fail(); } catch (ValueFormatException e) {}
        try { value.getDouble(); fail(); } catch (ValueFormatException e) {}
        try { value.getLong(); fail(); } catch (ValueFormatException e) {}
        assertEquals(Boolean.TRUE.toString(), value.getString());

        // TODO: binary representation
    }

    public void testDate() throws RepositoryException {
        Calendar a = Calendar.getInstance();
        a.setTimeInMillis(1234567890);
        a.setTimeZone(TimeZone.getTimeZone("GMT"));
        Value value = factory.createValue(a);

        assertEquals(PropertyType.DATE, value.getType());
        assertEquals(value, factory.createValue(a));

        try { value.getBoolean(); fail(); } catch (ValueFormatException e) {}
        assertEquals(a, value.getDate());
        assertEquals(new BigDecimal(a.getTimeInMillis()), value.getDecimal());
        assertEquals((double) a.getTimeInMillis(), value.getDouble());
        assertEquals(a.getTimeInMillis(), value.getLong());
        assertEquals("1970-01-15T06:56:07.890Z", value.getString());

        // TODO: binary representation
    }

    public void testDecimal() throws RepositoryException {
        BigDecimal a = new BigDecimal(1234567890);
        Value value = factory.createValue(a);

        assertEquals(PropertyType.DECIMAL, value.getType());
        assertEquals(value, factory.createValue(a));

        try { value.getBoolean(); fail(); } catch (ValueFormatException e) {}
        assertEquals(a.longValue(), value.getDate().getTimeInMillis());
        assertEquals(a, value.getDecimal());
        assertEquals(a.doubleValue(), value.getDouble());
        assertEquals(a.longValue(), value.getLong());
        assertEquals(a.toString(), value.getString());

        // TODO: binary representation
    }

    public void testDouble() throws RepositoryException {
        double a = 123456789.0;
        Value value = factory.createValue(a);

        assertEquals(PropertyType.DOUBLE, value.getType());
        assertEquals(value, factory.createValue(a));

        try { value.getBoolean(); fail(); } catch (ValueFormatException e) {}
        assertEquals((long) a, value.getDate().getTimeInMillis());
        assertEquals(new BigDecimal(a), value.getDecimal());
        assertEquals(a, value.getDouble());
        assertEquals((long) a, value.getLong());
        assertEquals(Double.toString(a), value.getString());

        // TODO: binary representation
    }

    public void testLong() throws RepositoryException {
        long a = 1234567890;
        Value value = factory.createValue(a);

        assertEquals(PropertyType.LONG, value.getType());
        assertEquals(value, factory.createValue(a));

        try { value.getBoolean(); fail(); } catch (ValueFormatException e) {}
        assertEquals(a, value.getDate().getTimeInMillis());
        assertEquals(new BigDecimal(a), value.getDecimal());
        assertEquals((double) a, value.getDouble());
        assertEquals(a, value.getLong());
        assertEquals(Long.toString(a), value.getString());

        // TODO: binary representation
    }

    public void testString() throws RepositoryException {
        String a = "test";
        Value value = factory.createValue(a);

        assertEquals(PropertyType.STRING, value.getType());
        assertEquals(value, factory.createValue(a));

        assertFalse(value.getBoolean());
        try { value.getDate(); fail(); } catch (ValueFormatException e) {}
        try { value.getDecimal(); fail(); } catch (ValueFormatException e) {}
        try { value.getDouble(); fail(); } catch (ValueFormatException e) {}
        try { value.getLong(); fail(); } catch (ValueFormatException e) {}
        assertEquals(a, value.getString());

        // TODO: binary representation
    }

}
