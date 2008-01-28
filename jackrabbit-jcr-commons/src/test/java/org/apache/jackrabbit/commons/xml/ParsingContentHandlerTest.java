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

import java.io.ByteArrayInputStream;
import java.io.StringWriter;

import junit.framework.TestCase;

import org.xml.sax.ContentHandler;

public class ParsingContentHandlerTest extends TestCase {

    public void testSerializingContentHandler() throws Exception {
        String source =
            "<p:a xmlns:p=\"uri\"><b p:foo=\"bar\">abc</b><c/>xyz</p:a>";
        StringWriter writer = new StringWriter();

        ContentHandler handler =
            SerializingContentHandler.getSerializer(writer);
        new ParsingContentHandler(handler).parse(
                new ByteArrayInputStream(source.getBytes("UTF-8")));

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
