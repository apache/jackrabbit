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
package org.apache.jackrabbit.extractor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.ParserConfigurationException;
import java.io.Reader;
import java.io.InputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;

/**
 * Text extractor for OpenOffice documents.
 */
public class OpenOfficeTextExtractor extends AbstractTextExtractor {

    /**
     * Logger instance.
     */
    private static final Logger logger =
        LoggerFactory.getLogger(OpenOfficeTextExtractor.class);

    /**
     * Creates a new <code>OpenOfficeTextExtractor</code> instance.
     */
    public OpenOfficeTextExtractor() {
        super(new String[]{"application/vnd.oasis.opendocument.database",
                           "application/vnd.oasis.opendocument.formula",
                           "application/vnd.oasis.opendocument.graphics",
                           "application/vnd.oasis.opendocument.presentation",
                           "application/vnd.oasis.opendocument.spreadsheet",
                           "application/vnd.oasis.opendocument.text"});
    }

    //-------------------------------------------------------< TextExtractor >

    /**
     * {@inheritDoc}
     */
    public Reader extractText(InputStream stream,
                              String type,
                              String encoding) throws IOException {
        try {
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            saxParserFactory.setValidating(false);
            SAXParser saxParser = saxParserFactory.newSAXParser();
            XMLReader xmlReader = saxParser.getXMLReader();
            xmlReader.setFeature("http://xml.org/sax/features/validation", false);
            xmlReader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            ZipInputStream zis = new ZipInputStream(stream);
            ZipEntry ze = zis.getNextEntry();
            while (!ze.getName().equals("content.xml")) {
                ze = zis.getNextEntry();
            }

            OpenOfficeContentHandler contentHandler =
                    new OpenOfficeContentHandler();
            xmlReader.setContentHandler(contentHandler);
            try {
                xmlReader.parse(new InputSource(zis));
            } finally {
                zis.close();
            }

            return new StringReader(contentHandler.getContent());
        } catch (ParserConfigurationException e) {
            logger.warn("Failed to extract OpenOffice text content", e);
            return new StringReader("");
        } catch (SAXException e) {
            logger.warn("Failed to extract OpenOffice text content", e);
            return new StringReader("");
        } finally {
            stream.close();
        }
    }

    //--------------------------------------------< OpenOfficeContentHandler >

    private class OpenOfficeContentHandler extends DefaultHandler {

        private StringBuffer content;
        private boolean appendChar;

        public OpenOfficeContentHandler() {
            content = new StringBuffer();
            appendChar = false;
        }

        /**
         * Returns the text content extracted from parsed content.xml
         */
        public String getContent() {
            return content.toString();
        }

        public void startElement(String namespaceURI, String localName,
                                 String rawName, Attributes atts)
                throws SAXException {
            if (rawName.startsWith("text:")) {
                appendChar = true;
            }
        }

        public void characters(char[] ch, int start, int length)
                throws SAXException {
            if (appendChar) {
                content.append(ch, start, length).append(" ");
            }
        }

        public void endElement(java.lang.String namespaceURI,
                               java.lang.String localName,
                               java.lang.String qName)
                throws SAXException {
            appendChar = false;
        }
    }

}
