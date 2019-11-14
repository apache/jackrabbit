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
package org.apache.jackrabbit.util;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import junit.framework.TestCase;

public class ISO8601Test extends TestCase {

    private TimeZone customPlus = new SimpleTimeZone(83 * 60 * 1000, "test");
    private TimeZone customMinus = new SimpleTimeZone(-1202 * 60 * 1000, "test");

    public void testFormatThrowsIllegalArgumentException() {
        try {
            ISO8601.format(null);
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testParseThrowsIllegalArgumentException() {
        try {
            ISO8601.parse(null);
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testFormatUTC() {
        Calendar c = Calendar.getInstance();
        c.setTimeZone(TimeZone.getTimeZone("UTC"));

        c.setTimeInMillis(0);
        assertEquals("1970-01-01T00:00:00.000Z", ISO8601.format(c));

        c.setTimeInMillis(123456789012L);
        assertEquals("1973-11-29T21:33:09.012Z", ISO8601.format(c));
    }

    public void testFormatCustomTz() {
        Calendar c = Calendar.getInstance();
        c.setTimeZone(customPlus);

        c.setTimeInMillis(0);
        assertEquals("1970-01-01T01:23:00.000+01:23", ISO8601.format(c));

        c.setTimeInMillis(123456789012L);
        assertEquals("1973-11-29T22:56:09.012+01:23", ISO8601.format(c));

        c.setTimeZone(customMinus);

        c.setTimeInMillis(0);
        assertEquals("1969-12-31T03:58:00.000-20:02", ISO8601.format(c));

        c.setTimeInMillis(123456789012L);
        assertEquals("1973-11-29T01:31:09.012-20:02", ISO8601.format(c));
    }

    public void testParseUTC() {
        Calendar c = ISO8601.parse("1970-01-01T00:00:00.000Z");
        assertEquals(0, c.getTimeInMillis());
        assertEquals(0, c.getTimeZone().getRawOffset());

        c = ISO8601.parse("1973-11-29T21:33:09.012Z");
        assertEquals(123456789012L, c.getTimeInMillis());
        assertEquals(0, c.getTimeZone().getRawOffset());
    }

    public void testParseCustomTZ() {
        Calendar c = ISO8601.parse("1970-01-01T01:23:00.000+01:23");
        assertEquals(0, c.getTimeInMillis());
        assertEquals(customPlus.getRawOffset(), c.getTimeZone().getRawOffset());

        c = ISO8601.parse("1973-11-29T22:56:09.012+01:23");
        assertEquals(123456789012L, c.getTimeInMillis());
        assertEquals(customPlus.getRawOffset(), c.getTimeZone().getRawOffset());

        c = ISO8601.parse("1969-12-31T03:58:00.000-20:02");
        assertEquals(0, c.getTimeInMillis());
        assertEquals(customMinus.getRawOffset(), c.getTimeZone().getRawOffset());

        c = ISO8601.parse("1973-11-29T01:31:09.012-20:02");
        assertEquals(123456789012L, c.getTimeInMillis());
        assertEquals(customMinus.getRawOffset(), c.getTimeZone().getRawOffset());
    }

    public void testBC() {
        Calendar c = ISO8601.parse("-0001-01-01T00:00:00.000Z");
        assertEquals(2, c.get(Calendar.YEAR));
        assertEquals(GregorianCalendar.BC, c.get(Calendar.ERA));
    }

    public void testAD() {
        Calendar c = ISO8601.parse("+1970-01-01T00:00:00.000Z");
        assertEquals(1970, c.get(Calendar.YEAR));
        assertEquals(GregorianCalendar.AD, c.get(Calendar.ERA));
    }

    public void testParseErrors() {
        assertNull(ISO8601.parse(""));
        assertNull(ISO8601.parse("x"));
        assertNull(ISO8601.parse("19701211100908Z"));
        assertNull(ISO8601.parse("1970-1211100908Z"));
        assertNull(ISO8601.parse("1970-12-11100908Z"));
        assertNull(ISO8601.parse("1970-12-11T100908Z"));
        assertNull(ISO8601.parse("1970-12-11T10:0908Z"));
        assertNull(ISO8601.parse("1970-12-11T10:09:08Z"));
    }
}
