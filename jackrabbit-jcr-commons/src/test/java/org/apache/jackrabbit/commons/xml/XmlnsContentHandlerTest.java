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

public class XmlnsContentHandlerTest extends TestCase {

    public void testXmlns() throws SAXException {
        ContentHandler handler =
            new XmlnsContentHandler(new ToXmlContentHandler());
        handler.startDocument();
        handler.startPrefixMapping("foo", "http://x.y.z/");
        handler.startPrefixMapping("bar", "http://a.b.c/");
        handler.startElement("", "test", "test", new AttributesImpl());
        handler.startPrefixMapping("foo", "http://a.b.c/");
        handler.startElement("", "tset", "tset", new AttributesImpl());
        handler.endElement("", "tset", "tset");
        handler.endPrefixMapping("foo");
        handler.startElement("", "tset", "tset", new AttributesImpl());
        handler.endElement("", "tset", "tset");
        handler.endElement("", "test", "test");
        handler.endPrefixMapping("bar");
        handler.endPrefixMapping("foo");
        handler.endDocument();
        assertEquals(
                "<?xml version=\"1.0\"?>"
                + "<test xmlns:foo=\"http://x.y.z/\" xmlns:bar=\"http://a.b.c/\">"
                + "<tset xmlns:foo=\"http://a.b.c/\"/><tset/>"
                + "</test>",
                handler.toString());
    }

}
