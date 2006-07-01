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
package org.apache.jackrabbit.core.query;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.jackrabbit.core.query.lucene.FieldNames;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.BLOBFileValue;
import org.apache.jackrabbit.core.value.InternalValue;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Extracts texts from XML documents.
 */
public class XMLTextFilter implements TextFilter {

    /**
     * @return <code>true</code> for <code>text/xml</code>, <code>false</code>
     *         otherwise.
     */
    public boolean canFilter(String mimeType) {
        return "text/xml".equalsIgnoreCase(mimeType);
    }

    /**
     * Returns a map with a single entry for field {@link FieldNames#FULLTEXT}.
     *
     * @param data     object containing RTF document data
     * @param encoding text encoding is not used, since it is specified in the
     *                 data.
     * @return a map with a single Reader value for field {@link
     *         FieldNames#FULLTEXT}.
     * @throws RepositoryException if data is a multi-value property or if the
     *                             content can't be extracted
     */
    public Map doFilter(PropertyState data, String encoding)
            throws RepositoryException {
        InternalValue[] values = data.getValues();
        if (values.length > 0) {
            final BLOBFileValue blob = (BLOBFileValue) values[0].internalValue();
            LazyReader reader = new LazyReader() {
                protected void initializeReader() throws IOException {
                    try {
                        StringBuffer buffer = new StringBuffer();
                        XMLParser parser = new XMLParser(buffer);

                        SAXParserFactory saxParserFactory =
                            SAXParserFactory.newInstance();
                        saxParserFactory.setValidating(false);
                        SAXParser saxParser = saxParserFactory.newSAXParser();
                        XMLReader xmlReader = saxParser.getXMLReader();
                        xmlReader.setContentHandler(parser);
                        xmlReader.setErrorHandler(parser);

                        InputStream in = blob.getStream();
                        try {
                            InputSource source = new InputSource(in);
                            xmlReader.parse(source);
                            delegate = new StringReader(buffer.toString());
                        } finally {
                            in.close();
                        }
                    } catch (SAXException se) {
                        throw new IOException(se.getMessage());
                    } catch (RepositoryException se) {
                        throw new IOException(se.getMessage());
                    } catch (ParserConfigurationException e) {
                        throw new IOException(e.getMessage());
                    }
                }
            };

            Map result = new HashMap();
            result.put(FieldNames.FULLTEXT, reader);
            return result;
        } else {
            // multi value not supported
            throw new RepositoryException("Multi-valued binary properties not supported.");
        }
    }

    /**
     * Private helper XML parser. It only processes text elements and
     * attributes. Feel free to change for adding support for tags text
     * extraction.
     */
    private static class XMLParser extends DefaultHandler implements ErrorHandler {

        private final StringBuffer buffer;

        public XMLParser(StringBuffer buffer) {
            this.buffer = buffer;
        }

        public void startElement(
                String uri, String local, String name, Attributes attributes) {
            for (int i = 0; i < attributes.getLength(); i++) {
                // Add spaces to separate the attribute value from other content
                String value = " " + attributes.getValue(i) + " ";
                characters(value.toCharArray(), 0, value.length());
            }
        }

        public void characters(char[] ch, int start, int length) {
            boolean space = false;
            for (int i = start; i < length; i++) {
                if (Character.isLetterOrDigit(ch[i])) {
                    if (space) {
                        buffer.append(' ');
                        space = false;
                    }
                    buffer.append(ch[i]);
                } else {
                    space = true;
                }
            }
            if (space) {
                buffer.append(' ');
            }
        }

        public void warning(SAXParseException spe) {
        }

        public void error(SAXParseException spe) {
        }

        public void fatalError(SAXParseException spe) {
        }

    }

}
