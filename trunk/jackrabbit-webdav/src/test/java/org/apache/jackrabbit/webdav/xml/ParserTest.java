/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the \"License\"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an \"AS IS\" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.webdav.xml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import junit.framework.TestCase;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ParserTest extends TestCase {

    // see <http://en.wikipedia.org/wiki/Billion_laughs#Details>
    public void testBillionLaughs() throws UnsupportedEncodingException {

        String testBody = "<?xml version=\"1.0\"?>" + "<!DOCTYPE lolz [" + " <!ENTITY lol \"lol\">" + " <!ELEMENT lolz (#PCDATA)>"
                + " <!ENTITY lol1 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\">"
                + " <!ENTITY lol2 \"&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;\">"
                + " <!ENTITY lol3 \"&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;\">"
                + " <!ENTITY lol4 \"&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;\">"
                + " <!ENTITY lol5 \"&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;\">"
                + " <!ENTITY lol6 \"&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;\">"
                + " <!ENTITY lol7 \"&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;\">"
                + " <!ENTITY lol8 \"&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;\">"
                + " <!ENTITY lol9 \"&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;\">" + "]>" + "<lolz>&lol9;</lolz>";
        InputStream is = new ByteArrayInputStream(testBody.getBytes("UTF-8"));

        try {
            DomUtil.parseDocument(is);
            fail("parsing this document should cause an exception");
        } catch (Exception expected) {
        }
    }

    public void testExternalEntities() throws IOException {

        String dname = "target";
        String fname = "test.xml";

        File f = new File(dname, fname);
        OutputStream os = new FileOutputStream(f);
        os.write("testdata".getBytes());
        os.close();

        String testBody = "<?xml version='1.0'?>\n<!DOCTYPE foo [" + " <!ENTITY test SYSTEM \"file:" + dname + "/" + fname + "\">"
                + "]>\n<foo>&test;</foo>";
        InputStream is = new ByteArrayInputStream(testBody.getBytes("UTF-8"));

        try {
            Document d = DomUtil.parseDocument(is);
            Element root = d.getDocumentElement();
            String text = DomUtil.getText(root);
            fail("parsing this document should cause an exception, but the following external content was included: " + text);
        } catch (Exception expected) {
        }
    }

    public void testCustomEntityResolver() throws ParserConfigurationException, SAXException, IOException {

        try {
            DocumentBuilderFactory dbf = new DocumentBuilderFactory() {

                DocumentBuilderFactory def = DocumentBuilderFactory.newInstance();

                @Override
                public void setFeature(String name, boolean value) throws ParserConfigurationException {
                    def.setFeature(name, value);
                }

                @Override
                public void setAttribute(String name, Object value) throws IllegalArgumentException {
                    def.setAttribute(name, value);
                }

                @Override
                public DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
                    DocumentBuilder db = def.newDocumentBuilder();
                    db.setEntityResolver(new EntityResolver() {
                        @Override
                        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                            if ("foo:test".equals(systemId)) {
                                return new InputSource(new ByteArrayInputStream("foo&amp;bar".getBytes("UTF-8")));
                            } else {
                                return null;
                            }
                        }
                    });
                    return db;
                }

                @Override
                public boolean getFeature(String name) throws ParserConfigurationException {
                    return def.getFeature(name);
                }

                @Override
                public Object getAttribute(String name) throws IllegalArgumentException {
                    return def.getAttribute(name);
                }
            };

            DomUtil.setBuilderFactory(dbf);
            String testBody = "<?xml version='1.0'?>\n<!DOCTYPE foo [" + " <!ENTITY test SYSTEM \"foo:test\">"
                    + "]>\n<foo>&test;</foo>";
            InputStream is = new ByteArrayInputStream(testBody.getBytes("UTF-8"));

            Document d = DomUtil.parseDocument(is);
            Element root = d.getDocumentElement();
            String text = DomUtil.getText(root);
            assertEquals("custom entity resolver apparently not called", "foo&bar", text);
        } finally {
            DomUtil.setBuilderFactory(null);
        }
    }
}