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
package org.apache.jackrabbit.commons.xml;

import java.io.StringWriter;

import junit.framework.TestCase;

import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

public class SerializingContentHandlerTest extends TestCase {

    public void testSerializingContentHandler() throws Exception {
        StringWriter writer = new StringWriter();

        ContentHandler handler =
            SerializingContentHandler.getSerializer(writer);
        handler.startDocument();
        handler.startPrefixMapping("p", "uri");
        handler.startElement("uri", "a", "p:a", new AttributesImpl());
        AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute("uri", "foo", "p:foo", "CDATA", "bar");
        handler.startElement(null, "b", "b", attributes);
        handler.characters("abc".toCharArray(), 0, 3);
        handler.endElement(null, "b", "b");
        handler.startElement(null, "c", "c", new AttributesImpl());
        handler.endElement(null, "c", "c");
        handler.characters("xyz".toCharArray(), 0, 3);
        handler.endElement("uri", "a", "p:a");
        handler.endPrefixMapping("p");
        handler.endDocument();

        String xml = writer.toString();
        assertContains(xml, "<p:a");
        assertContains(xml, "xmlns:p");
        assertContains(xml, "=");
        assertContains(xml, "uri");
        assertContains(xml, ">");
        assertContains(xml, "<b");
        assertContains(xml, "p:foo");
        assertContains(xml, "bar");
        assertContains(xml, "abc");
        assertContains(xml, "</b>");
        assertContains(xml, "<c/>");
        assertContains(xml, "xyz");
        assertContains(xml, "</p:a>");
    }

    /**
     * Test case for JCR-1767.
     *
     * @see <a href="https://issues.apache.org/jira/browse/JCR-1767">JCR-1767</a>
     */
    public void testNoPrefixMappingCalls() throws Exception {
        StringWriter writer = new StringWriter();

        ContentHandler handler =
            SerializingContentHandler.getSerializer(writer);
        handler.startDocument();
        handler.startElement("uri", "a", "p:a", new AttributesImpl());
        AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute("uri", "foo", "p:foo", "CDATA", "bar");
        handler.startElement(null, "b", "b", attributes);
        handler.characters("abc".toCharArray(), 0, 3);
        handler.endElement(null, "b", "b");
        handler.startElement(null, "c", "c", new AttributesImpl());
        handler.endElement(null, "c", "c");
        handler.characters("xyz".toCharArray(), 0, 3);
        handler.endElement("uri", "a", "p:a");
        handler.endDocument();

        String xml = writer.toString();
        assertContains(xml, "<p:a");
        assertContains(xml, "xmlns:p");
        assertContains(xml, "=");
        assertContains(xml, "uri");
        assertContains(xml, ">");
        assertContains(xml, "<b");
        assertContains(xml, "p:foo");
        assertContains(xml, "bar");
        assertContains(xml, "abc");
        assertContains(xml, "</b>");
        assertContains(xml, "<c/>");
        assertContains(xml, "xyz");
        assertContains(xml, "</p:a>");
    }

    private void assertContains(String haystack, String needle) {
        if (haystack.indexOf(needle) == -1) {
            fail("'" + haystack + "' does not contain '" + needle+ "'");
        }
    }
}
