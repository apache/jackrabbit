/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

    private org.apache.jackrabbit.core.query.XMLTextFilter.XMLParser parser;

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

        if (parser == null) {
            initParser();
        }

        InternalValue[] values = data.getValues();
        if (values.length > 0) {
            try {
                try {
                    BLOBFileValue blob = (BLOBFileValue) values[0].internalValue();
                    parser.parse(blob.getStream());
                    String text = parser.getContents();

                    Map result = new HashMap();
                    result.put(FieldNames.FULLTEXT, new StringReader(text));
                    return result;
                } catch (IOException ioe) {
                    throw new RepositoryException(ioe);
                }

            } catch (IllegalStateException e) {
                throw new RepositoryException(e);
            }
        } else {
            // multi value not supported
            throw new RepositoryException("Multi-valued binary properties not supported.");
        }
    }

    /**
     * Inits the parser engine
     *
     * @throws javax.jcr.RepositoryException If some error happens
     */
    private void initParser() throws javax.jcr.RepositoryException {

        try {
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            saxParserFactory.setValidating(false);
            SAXParser saxParser = saxParserFactory.newSAXParser();
            XMLReader xmlReader = saxParser.getXMLReader();
            parser = new XMLParser(xmlReader);
        } catch (ParserConfigurationException e) {
            throw new javax.jcr.RepositoryException();
        } catch (SAXException e) {
            throw new javax.jcr.RepositoryException();
        }
    }

    /**
     * Private helper XML parser. It only processes text elements. Feel free to
     * change for adding support for attributes or tags text extraction.
     */
    private class XMLParser extends DefaultHandler implements ErrorHandler {

        private XMLReader xmlReader;
        private StringBuffer buffer;

        public XMLParser(XMLReader xmlReader) {

            try {

                this.xmlReader = xmlReader;
                this.xmlReader.setContentHandler(this);
                this.xmlReader.setErrorHandler(this);

            } catch (Exception ex) {

            }
        }

        public void startDocument() throws SAXException {

            buffer = new StringBuffer();
        }

        public void startElement(String namespaceURI, String localName,
                                 String rawName, Attributes atts)
                throws SAXException {
        }

        public void characters(char[] ch,
                               int start,
                               int length) throws SAXException {


            buffer.append(ch, start, length);
        }

        public void endElement(java.lang.String namespaceURI,
                               java.lang.String localName,
                               java.lang.String qName)
                throws SAXException {
        }


        public void warning(SAXParseException spe) throws SAXException {


        }

        public void error(SAXParseException spe) throws SAXException {

        }

        public void fatalError(SAXParseException spe) throws SAXException {

        }

        public void parse(InputStream is) throws IOException {

            try {
                InputSource source = new InputSource(is);
                xmlReader.parse(source);
            } catch (SAXException se) {
                // ignore errors
            }
        }

        private String filterAndJoin(String text) {

            boolean space = false;
            StringBuffer buffer = new StringBuffer();
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);

                if ((c == '\n') || (c == ' ') || Character.isWhitespace(c)) {
                    if (space) {
                        continue;
                    } else {
                        space = true;
                        buffer.append(' ');
                        continue;
                    }
                } else {
                    if (!Character.isLetter(c)) {
                        if (!space) {
                            space = true;
                            buffer.append(' ');
                            continue;
                        }
                        continue;
                    }
                }
                space = false;
                buffer.append(c);
            }
            return buffer.toString();
        }

        public String getContents() {

            String text = filterAndJoin(buffer.toString());
            return text;
        }

    }
}
