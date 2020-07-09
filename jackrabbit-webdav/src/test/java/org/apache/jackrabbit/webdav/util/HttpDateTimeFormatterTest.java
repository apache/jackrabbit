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
package org.apache.jackrabbit.webdav.util;

import java.time.format.DateTimeParseException;

import junit.framework.TestCase;

public class HttpDateTimeFormatterTest extends TestCase {

    public void testImfDate() {
        long t = HttpDateTimeFormatter.parseImfFixedDate("Sun, 06 Nov 1994 08:49:37 GMT");
        assertEquals(784111777000l, t);
        assertEquals("Sun, 06 Nov 1994 08:49:37 GMT", HttpDateTimeFormatter.formatImfFixed(t));
    }

    public void testImfDateWrongTZ() {
        try {
            HttpDateTimeFormatter.parseImfFixedDate("Sun, 06 Nov 1994 08:49:37 CET");
            fail("should fail for incorrec tz");
        } catch (DateTimeParseException expected) {
        }
    }

    public void testImfDateWrongWeekday() {
        try {
            HttpDateTimeFormatter.parseImfFixedDate("Mon, 06 Nov 1994 08:49:37 GMT");
            fail("should fail for incorrec tz");
        } catch (DateTimeParseException expected) {
        }
    }

    // will fail after 2044
    public void testRFC850Date() {
        long t = HttpDateTimeFormatter.parseRfc850Date("Sunday, 06-Nov-94 08:49:37 GMT");
        assertEquals(784111777000l, t);
        assertEquals("Sunday, 06-Nov-94 08:49:37 GMT", HttpDateTimeFormatter.formatRfc850(t));
    }

    public void testAscTimeDate() {
        long t = HttpDateTimeFormatter.parseAscTimeDate("Sun Nov  6 08:49:37 1994");
        assertEquals(784111777000l, t);
        assertEquals("Sun Nov  6 08:49:37 1994", HttpDateTimeFormatter.formatAscTime(t));
    }

    public void testAscTimeDateZeroPad() {
        long t = HttpDateTimeFormatter.parseAscTimeDate("Sun Nov 06 08:49:37 1994");
        assertEquals(784111777000l, t);
        assertEquals("Sun Nov  6 08:49:37 1994", HttpDateTimeFormatter.formatAscTime(t));
    }
}
