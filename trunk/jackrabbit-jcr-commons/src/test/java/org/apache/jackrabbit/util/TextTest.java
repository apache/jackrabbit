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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

/**
 * Test cases for the Text utility class.
 */
public class TextTest extends TestCase {

    private void checkEscape(String name) {
        String escaped = Text.escapeIllegalJcrChars(name);
        assertEquals(name, Text.unescapeIllegalJcrChars(escaped));
    }

    public void testEscape() {
        // Handling of normal names
        checkEscape("foobar");
        // Handling of short names
        checkEscape("x");
        checkEscape("xx");
        checkEscape("xxx");
        // Handling of spaces
        checkEscape("x x");
        checkEscape("x x x");
        checkEscape(" ");
        checkEscape(" x");
        checkEscape("x ");
        checkEscape("  ");
        checkEscape("   ");
        checkEscape(" x ");
        // Handling of dots within the first two characters
        checkEscape(".");
        checkEscape("..");
        checkEscape(".x");
        checkEscape("x.");
        checkEscape(". ");
        checkEscape(" .");
        // Handling of the special characters
        checkEscape("%/:[]*'\"|\t\r\n");
    }

    public void testIsDescendant() {
        String parent = "/";
        List descendants = new ArrayList();
        descendants.add("/a");
        descendants.add("/a/b");
        for (Iterator it = descendants.iterator(); it.hasNext();) {
            String desc = it.next().toString();
            assertTrue(desc + " must be descendant of " + parent, Text.isDescendant(parent, desc));
        }
        List nonDescendants = new ArrayList();
        nonDescendants.add("/");
        nonDescendants.add("a");
        for (Iterator it = nonDescendants.iterator(); it.hasNext();) {
            String nonDesc = it.next().toString();
            assertFalse(nonDesc + " isn't a descendant of " + parent,Text.isDescendant(parent, nonDesc));
        }

        parent = "/a/b";
        descendants = new ArrayList();
        descendants.add("/a/b/c");
        descendants.add("/a/b/c/");
        for (Iterator it = descendants.iterator(); it.hasNext();) {
            String desc = it.next().toString();
            assertTrue(desc + " must be descendant of " + parent, Text.isDescendant(parent, desc));
        }
        nonDescendants = new ArrayList();
        nonDescendants.add("/");
        nonDescendants.add("/a");
        nonDescendants.add("/a/b");
        nonDescendants.add("/a/b/");
        nonDescendants.add("/d");
        nonDescendants.add("/d/b");
        for (Iterator it = nonDescendants.iterator(); it.hasNext();) {
            String nonDesc = it.next().toString();
            assertFalse(nonDesc + " isn't a descendant of " + parent, Text.isDescendant(parent, nonDesc));
        }
    }

    public void testGetName() {
        List l = new ArrayList();
        l.add(new String[] {"/a/b/c", "c"});
        l.add(new String[] {"c", "c"});
        l.add(new String[] {null, null});
        l.add(new String[] {"", ""});
        l.add(new String[] {"/", ""});
        l.add(new String[] {"http://jackrabbit.apache.org/jackrabbit", "jackrabbit"});
        l.add(new String[] {"http://jackrabbit.apache.org/jackrabbit/", ""});

        for (Iterator it = l.iterator(); it.hasNext();) {
            String[] strs = (String[]) it.next();
            assertEquals(strs[1], Text.getName(strs[0]));
        }

        // Text.getName(String path, boolean ignoreTrailingSlash)
        l = new ArrayList();
        l.add(new String[] {"http://jackrabbit.apache.org/jackrabbit", "jackrabbit"});
        l.add(new String[] {"http://jackrabbit.apache.org/jackrabbit/", "jackrabbit"});

        for (Iterator it = l.iterator(); it.hasNext();) {
            String[] strs = (String[]) it.next();
            assertEquals(strs[1], Text.getName(strs[0], true));
        }

        // Text.getName(String path, char delim)
        l = new ArrayList();
        l.add(new String[] {"/a=b/c", "b/c"});
        l.add(new String[] {"c", "c"});
        l.add(new String[] {null, null});
        l.add(new String[] {"", ""});
        l.add(new String[] {"http:/=jackrabbit.apache.org/jackrabbit", "jackrabbit.apache.org/jackrabbit"});
        l.add(new String[] {"http:=//jackrabbit.apache.org/jackrabbit/", "//jackrabbit.apache.org/jackrabbit/"});

        for (Iterator it = l.iterator(); it.hasNext();) {
            String[] strs = (String[]) it.next();
            assertEquals(strs[1], Text.getName(strs[0], '='));
        }
    }

    public void testUrlEscape() {
        String testString = "\u4e2d\u56fd\u7684\u7f51\u9875 $% \u20acuro %$ ";

        String escaped = testString
            .replaceAll("%", "%25")
            .replaceAll(" ", "%20");
        String unescaped = Text.unescape(escaped);
        assertEquals(testString, unescaped);

        escaped = Text.escape(testString);
        unescaped = Text.unescape(escaped);
        assertEquals(testString, unescaped);

        assertEquals("%", Text.unescape("%25"));
        assertEquals("", Text.unescape(""));
        assertEquals("\u4e2d\u56fd\u7684\u7f51\u9875", Text.unescape("\u4e2d\u56fd\u7684\u7f51\u9875"));
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/JCR-1926">JCR-1926</a>
     */
    public void testUnescapeWithInvalidInput() {
        assertInvalidUnescape("%");   // too short
        assertInvalidUnescape("anything%");   // too short
        assertInvalidUnescape("%%");  // too short
        assertInvalidUnescape("anything%%");  // too short
        assertInvalidUnescape("%1");  // too short
        assertInvalidUnescape("anything%1");  // too short
        assertInvalidUnescape("%%%"); // not a number
        assertInvalidUnescape("%ag"); // not a number
        assertInvalidUnescape("anything%%%"); // not a number
        assertInvalidUnescape("anything%ag"); // not a number
        assertInvalidUnescape("anything%%%anything"); // not a number
        assertInvalidUnescape("anything%aganything"); // not a number
    }

    private void assertInvalidUnescape(String string) {
        try {
            Text.unescape(string);
            fail("Text.unescape(" + string + ") should throw IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        } catch (RuntimeException unexpected) {
            fail("Text.unescape(" + string + "): " + unexpected.getMessage());
        }
    }

    public void testEscapeIllegalJcr10Chars() throws Exception {
        // single and double quote are illegal in JCR 1.0
        assertEquals("local%27name", Text.escapeIllegalJcr10Chars("local'name"));
        assertEquals("local%22name", Text.escapeIllegalJcr10Chars("local\"name"));
    }

    public void testEscapeIllegalJcrChars() throws Exception {
        // single and double quote are valid since JCR 2.0
        assertEquals("local'name", Text.escapeIllegalJcrChars("local'name"));
        assertEquals("local\"name", Text.escapeIllegalJcrChars("local\"name"));
    }

    public void testEscapeXML() {
        assertEquals("&amp;&lt;&gt;&apos;&quot;", Text.encodeIllegalXMLCharacters("&<>'\""));
    }

    public void testEscapeHTML() {
        assertEquals("&amp;&lt;&gt;&#39;&quot;", Text.encodeIllegalHTMLCharacters("&<>'\""));
    }
}
