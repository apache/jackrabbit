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

import junit.framework.TestCase;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class ToXmlContentHandlerTest extends TestCase {

    public void testMinimalDocument() throws SAXException {
        ContentHandler handler = new ToXmlContentHandler();
        handler.startDocument();
        handler.startElement("", "test", "test", new AttributesImpl());
        handler.endElement("", "test", "test");
        handler.endDocument();
        assertEquals("<?xml version=\"1.0\"?><test/>", handler.toString());
    }

    public void testAttribute() throws SAXException {
        ContentHandler handler = new ToXmlContentHandler();
        handler.startDocument();
        AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute("", "foo", "foo", "CDATA", "bar");
        handler.startElement("", "test", "test", attributes);
        handler.endElement("", "test", "test");
        handler.endDocument();
        assertEquals(
                "<?xml version=\"1.0\"?><test foo=\"bar\"/>",
                handler.toString());
    }

    public void testAttributeOrder() throws SAXException {
        ContentHandler handler;
        AttributesImpl attributes;

        handler = new ToXmlContentHandler();
        handler.startDocument();
        attributes = new AttributesImpl();
        attributes.addAttribute("", "foo", "foo", "CDATA", "A");
        attributes.addAttribute("", "bar", "bar", "CDATA", "B");
        handler.startElement("", "test", "test", attributes);
        handler.endElement("", "test", "test");
        handler.endDocument();
        assertEquals(
                "<?xml version=\"1.0\"?><test foo=\"A\" bar=\"B\"/>",
                handler.toString());

        handler = new ToXmlContentHandler();
        handler.startDocument();
        attributes = new AttributesImpl();
        attributes.addAttribute("", "bar", "bar", "CDATA", "B");
        attributes.addAttribute("", "foo", "foo", "CDATA", "A");
        handler.startElement("", "test", "test", attributes);
        handler.endElement("", "test", "test");
        handler.endDocument();
        assertEquals(
                "<?xml version=\"1.0\"?><test bar=\"B\" foo=\"A\"/>",
                handler.toString());
    }

    public void testChildElements() throws SAXException {
        ContentHandler handler = new ToXmlContentHandler();
        handler.startDocument();
        handler.startElement("", "test", "test", new AttributesImpl());
        handler.startElement("", "foo", "foo", new AttributesImpl());
        handler.endElement("", "foo", "foo");
        handler.startElement("", "bar", "bar", new AttributesImpl());
        handler.endElement("", "bar", "bar");
        handler.endElement("", "test", "test");
        handler.endDocument();
        assertEquals(
                "<?xml version=\"1.0\"?><test><foo/><bar/></test>",
                handler.toString());
    }

    public void testCharacters() throws SAXException {
        ContentHandler handler = new ToXmlContentHandler();
        handler.startDocument();
        handler.startElement("", "test", "test", new AttributesImpl());
        handler.characters("foo".toCharArray(), 0, 3);
        handler.endElement("", "test", "test");
        handler.endDocument();
        assertEquals(
                "<?xml version=\"1.0\"?><test>foo</test>",
                handler.toString());
    }

    public void testIgnorableWhitespace() throws SAXException {
        ContentHandler handler = new ToXmlContentHandler();
        handler.startDocument();
        handler.ignorableWhitespace("\n".toCharArray(), 0, 1);
        handler.startElement("", "test", "test", new AttributesImpl());
        handler.ignorableWhitespace("\n".toCharArray(), 0, 1);
        handler.endElement("", "test", "test");
        handler.ignorableWhitespace("\n".toCharArray(), 0, 1);
        handler.endDocument();
        assertEquals(
                "<?xml version=\"1.0\"?>\n<test>\n</test>\n",
                handler.toString());
    }

    public void testProcessingInstruction() throws SAXException {
        ContentHandler handler = new ToXmlContentHandler();
        handler.startDocument();
        handler.processingInstruction("foo", "abc=\"xyz\"");
        handler.startElement("", "test", "test", new AttributesImpl());
        handler.processingInstruction("bar", null);
        handler.endElement("", "test", "test");
        handler.endDocument();
        assertEquals(
                "<?xml version=\"1.0\"?>"
                + "<?foo abc=\"xyz\"?><test><?bar?></test>",
                handler.toString());
    }

    public void testComplexDocument() throws SAXException {
        ContentHandler handler = new ToXmlContentHandler();
        handler.startDocument();
        handler.ignorableWhitespace("\n".toCharArray(), 0, 1);
        handler.processingInstruction("foo", "abc=\"xyz\"");
        handler.ignorableWhitespace("\n".toCharArray(), 0, 1);
        AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute("", "version", "version", "CDATA", "1.0");
        attributes.addAttribute(
                "http://www.w3.org/2000/xmlns/", "xmlns", "xmlns",
                "CDATA", "http://x.y.z/");
        attributes.addAttribute(
                "http://www.w3.org/2000/xmlns/", "xmlns", "xmlns:abc",
                "CDATA", "http://a.b.c/");
        handler.startElement("", "test", "test", attributes);
        handler.ignorableWhitespace("\n  ".toCharArray(), 0, 3);
        handler.characters("abc\n".toCharArray(), 0, 4);
        handler.characters("  ".toCharArray(), 0, 2);
        attributes = new AttributesImpl();
        attributes.addAttribute("", "escape", "escape", "CDATA", "\"'<>&");
        handler.startElement("http://a.b.c/", "foo", "abc:foo", attributes);
        handler.characters("def".toCharArray(), 0, 3);
        handler.endElement("http://a.b.c/", "foo", "abc:foo");
        handler.ignorableWhitespace("\n  ".toCharArray(), 0, 3);
        char[] ch = "<bar a=\"&amp;\" b=''/>".toCharArray();
        handler.characters(ch, 0, ch.length);
        handler.characters("\n".toCharArray(), 0, 1);
        handler.endElement("", "test", "test");
        handler.characters("\n".toCharArray(), 0, 1);
        handler.endDocument();
        assertEquals(
                "<?xml version=\"1.0\"?>\n"
                + "<?foo abc=\"xyz\"?>\n"
                + "<test version=\"1.0\""
                + " xmlns=\"http://x.y.z/\" xmlns:abc=\"http://a.b.c/\">\n"
                + "  abc\n"
                + "  <abc:foo escape=\"&quot;&apos;&lt;&gt;&amp;\">"
                + "def</abc:foo>\n"
                + "  &lt;bar a=\"&amp;amp;\" b=''/&gt;\n"
                + "</test>\n",
                handler.toString());
    }

}
