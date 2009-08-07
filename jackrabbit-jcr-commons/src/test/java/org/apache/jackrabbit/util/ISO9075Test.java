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

import org.apache.jackrabbit.name.QName;
import junit.framework.TestCase;

/**
 * Test cases for ISO9075 encode / decode.
 */
public class ISO9075Test extends TestCase {

    public void testSpecExamples() {
        assertEquals("My_x0020_Documents", ISO9075.encode("My Documents"));
        assertEquals("_x0031_234id", ISO9075.encode("1234id"));
        assertEquals("My_Documents", ISO9075.encode("My_Documents"));
        assertEquals("My_x005f_x0020Documents", ISO9075.encode("My_x0020Documents"));
        assertEquals("My_x005f_x0020_Documents", ISO9075.encode("My_x0020_Documents"));
        assertEquals("My_x005f_x0020_", ISO9075.encode("My_x0020_"));
        assertEquals("My_x002", ISO9075.encode("My_x002"));
        assertEquals("My_x005f_x0020", ISO9075.encode("My_x0020"));
        assertEquals("My_", ISO9075.encode("My_"));
        assertEquals("My_x005f_x0020_x0020_Documents", ISO9075.encode("My_x0020 Documents"));
    }

    public void testMatcherEscapes() {
        assertEquals(
                "StringWith$inside", ISO9075.decode("StringWith$inside"));
        assertEquals(
                "StringWith$inside", ISO9075.decode("StringWith_x0024_inside"));
        assertEquals(
                "StringWith_x0024_inside", ISO9075.encode("StringWith$inside"));
        assertEquals(
                "StringWith\\inside", ISO9075.decode("StringWith\\inside"));
        assertEquals(
                "StringWith\\inside", ISO9075.decode("StringWith_x005c_inside"));
        assertEquals(
                "StringWith_x005c_inside", ISO9075.encode("StringWith\\inside"));
    }

    public void testPath() {
        assertEquals("foo/bar", ISO9075.encodePath("foo/bar"));
        assertEquals("/foo/bar", ISO9075.encodePath("/foo/bar"));
        assertEquals("/foo/bar/", ISO9075.encodePath("/foo/bar/"));
        assertEquals("/foo/bar[3]", ISO9075.encodePath("/foo/bar[3]"));
        assertEquals("home/My_x0020_Documents", ISO9075.encodePath("home/My Documents"));
        assertEquals("year/_x0032_007", ISO9075.encodePath("year/2007"));
        assertEquals("year/_x0032_007[2]", ISO9075.encodePath("year/2007[2]"));
    }

    /**
     * This is a disabled brute force test. It tests permutations of characters:
     * <code>' ', '_', 'x', '0', '2', 'a', 'b', '{'</code>, encodes and decodes
     * the sequences and test whether the initial sequence equals the resulting
     * sequence that went through the encoding / decoding process.
     * </p>
     * The test takes about 30 seconds on my 1.2G P3.
     * </p>
     * To enable the test remove the 'disabled_' refix from the method name.
     */
    public void disabled_testBrute() {
        char[] chars = new char[] {' ', '_', 'x', '0', '2', 'a', 'b', '{'};
        long start = Long.parseLong("1000000", 8);
        long end = Long.parseLong("7777777", 8);
        for (long i = start; i < end; i++) {
            String s = Long.toString(i, chars.length);
            StringBuffer b = new StringBuffer(s.length());
            for (int j = 0; j < s.length(); j++) {
                b.append(chars[s.charAt(j) - '0']);
            }
            // encode and decode
            QName initial = new QName("", b.toString());
            if ((i % 100000) == 0) {
                System.out.println(initial);
            }
            QName encoded = ISO9075.encode(initial);
            assertTrue(XMLChar.isValidName(encoded.getLocalName()));
            QName decoded = ISO9075.decode(encoded);
            assertEquals(initial, decoded);
        }
    }

}
