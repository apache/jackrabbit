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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import junit.framework.TestCase;

public class ISO8601Test extends TestCase {

    private TimeZone customPlus = new SimpleTimeZone(83 * 60 * 1000, "test");
    private TimeZone customMinus = new SimpleTimeZone(-122 * 60 * 1000, "test");
    private ZoneId customPlusZI = ZoneId.of("+01:23");
    private ZoneId customMinusZI = ZoneId.of("-02:02");

    public void testFormatThrowsIllegalArgumentException() {
        try {
            ISO8601.format((Calendar) null);
        } catch (IllegalArgumentException expected) {
        }
        try {
            ISO8601.format((Date) null);
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
        Clock clock;

        c.setTimeInMillis(0);
        clock = Clock.fixed(Instant.ofEpochMilli(0), ZoneId.of("Z"));
        assertEquals("1970-01-01T00:00:00.000Z", ISO8601.format(c));
        assertEquals("1970-01-01T00:00:00.000Z", ISO8601.format(clock));
        assertEquals("1970-01-01T00:00:00.000Z", ISO8601.format(0));
        assertEquals("1970-01-01T00:00:00.000Z", ISO8601.format(new Date(0)));
        assertEquals("1970-01-01T00:00:00.000Z", ISO8601.format(0, 0));
        assertEquals("1970-01-01T00:00:00.000Z", ISO8601.format(new Date(0), 0));

        c.setTimeInMillis(123456789012L);
        clock = Clock.fixed(Instant.ofEpochMilli(123456789012L), ZoneId.of("Z"));
        assertEquals("1973-11-29T21:33:09.012Z", ISO8601.format(c));
        assertEquals("1973-11-29T21:33:09.012Z", ISO8601.format(clock));
        assertEquals("1973-11-29T21:33:09.012Z", ISO8601.format(123456789012L));
        assertEquals("1973-11-29T21:33:09.012Z", ISO8601.format(new Date(123456789012L)));
        assertEquals("1973-11-29T21:33:09.012Z", ISO8601.format(123456789012L, 0));
        assertEquals("1973-11-29T21:33:09.012Z", ISO8601.format(new Date(123456789012L), 0));
    }

    public void testFormatUTCShort() {
        Calendar c = Calendar.getInstance();
        c.setTimeZone(TimeZone.getTimeZone("UTC"));
        Clock clock;

        c.setTimeInMillis(0);
        clock = Clock.fixed(Instant.ofEpochMilli(0), ZoneId.of("Z"));
        assertEquals("1970-01-01T00:00:00Z", ISO8601.SHORT.format(c));
        assertEquals("1970-01-01T00:00:00Z", ISO8601.SHORT.format(clock));
        assertEquals("1970-01-01T00:00:00Z", ISO8601.SHORT.format(0));
        assertEquals("1970-01-01T00:00:00Z", ISO8601.SHORT.format(new Date(0)));
        assertEquals("1970-01-01T00:00:00Z", ISO8601.SHORT.format(0, 0));
        assertEquals("1970-01-01T00:00:00Z", ISO8601.SHORT.format(new Date(0), 0));

        c.setTimeInMillis(123456789012L);
        clock = Clock.fixed(Instant.ofEpochMilli(123456789012L), ZoneId.of("Z"));
        assertEquals("1973-11-29T21:33:09Z", ISO8601.SHORT.format(c));
        assertEquals("1973-11-29T21:33:09Z", ISO8601.SHORT.format(clock));
        assertEquals("1973-11-29T21:33:09Z", ISO8601.SHORT.format(123456789012L));
        assertEquals("1973-11-29T21:33:09Z", ISO8601.SHORT.format(new Date(123456789012L)));
        assertEquals("1973-11-29T21:33:09Z", ISO8601.SHORT.format(123456789012L, 0));
        assertEquals("1973-11-29T21:33:09Z", ISO8601.SHORT.format(new Date(123456789012L), 0));
    }

    public void testFormatCustomTz() {
        Calendar c = Calendar.getInstance();
        c.setTimeZone(customPlus);
        Clock clock;

        c.setTimeInMillis(0);
        clock = Clock.fixed(Instant.ofEpochMilli(0), customPlusZI);
        assertEquals("1970-01-01T01:23:00.000+01:23", ISO8601.format(c));
        assertEquals("1970-01-01T01:23:00.000+01:23", ISO8601.format(clock));
        assertEquals("1970-01-01T01:23:00.000+01:23", ISO8601.format(0, customPlus.getRawOffset() / 1000));
        assertEquals("1970-01-01T01:23:00.000+01:23", ISO8601.format(new Date(0), customPlus.getRawOffset() / 1000));

        c.setTimeInMillis(123456789012L);
        clock = Clock.fixed(Instant.ofEpochMilli(123456789012L), customPlusZI);
        assertEquals("1973-11-29T22:56:09.012+01:23", ISO8601.format(c));
        assertEquals("1973-11-29T22:56:09.012+01:23", ISO8601.format(clock));
        assertEquals("1973-11-29T22:56:09.012+01:23", ISO8601.format(123456789012L, customPlus.getRawOffset() / 1000));
        assertEquals("1973-11-29T22:56:09.012+01:23", ISO8601.format(new Date(123456789012L), customPlus.getRawOffset() / 1000));

        c.setTimeZone(customMinus);

        c.setTimeInMillis(0);
        clock = Clock.fixed(Instant.ofEpochMilli(0), customMinusZI);
        assertEquals("1969-12-31T21:58:00.000-02:02", ISO8601.format(c));
        assertEquals("1969-12-31T21:58:00.000-02:02", ISO8601.format(clock));
        assertEquals("1969-12-31T21:58:00.000-02:02", ISO8601.format(0, customMinus.getRawOffset() / 1000));
        assertEquals("1969-12-31T21:58:00.000-02:02", ISO8601.format(new Date(0), customMinus.getRawOffset() / 1000));

        c.setTimeInMillis(123456789012L);
        clock = Clock.fixed(Instant.ofEpochMilli(123456789012L), customMinusZI);
        assertEquals("1973-11-29T19:31:09.012-02:02", ISO8601.format(c));
        assertEquals("1973-11-29T19:31:09.012-02:02", ISO8601.format(clock));
        assertEquals("1973-11-29T19:31:09.012-02:02", ISO8601.format(123456789012L, customMinus.getRawOffset() / 1000));
        assertEquals("1973-11-29T19:31:09.012-02:02", ISO8601.format(new Date(123456789012L), customMinus.getRawOffset() / 1000));
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

        c = ISO8601.parse("1969-12-31T21:58:00.000-02:02");
        assertEquals(0, c.getTimeInMillis());
        assertEquals(customMinus.getRawOffset(), c.getTimeZone().getRawOffset());

        c = ISO8601.parse("1973-11-29T19:31:09.012-02:02");
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
