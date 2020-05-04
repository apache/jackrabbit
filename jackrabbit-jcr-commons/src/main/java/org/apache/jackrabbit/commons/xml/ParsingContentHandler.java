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
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Utility class that decorates a {@link ContentHandler} instance with
 * simple XML parsing capability.
 *
 * @since Jackrabbit JCR Commons 1.5
 */
public class ParsingContentHandler extends DefaultContentHandler {

    private static final SAXParserFactory SAX_PARSER_FACTORY;

    static {
        SAX_PARSER_FACTORY = SAXParserFactory.newInstance();
        SAX_PARSER_FACTORY.setNamespaceAware(true);
    }

    /**
     * Creates a {@link DefaultHandler} adapter for the given content
     * handler.
     *
     * @param handler content handler
     */
    public ParsingContentHandler(ContentHandler handler) {
        super(handler);
    }

    /**
     * Utility method that parses the given input stream using this handler.
     * The parser is namespace-aware and will not resolve external entity
     * references.
     *
     * @param in XML input stream
     * @throws IOException if an I/O error occurs
     * @throws SAXException if an XML parsing error occurs
     */
    public void parse(InputStream in) throws IOException, SAXException {
        try {
            SAX_PARSER_FACTORY.newSAXParser().parse(new InputSource(in), this);
        } catch (ParserConfigurationException e) {
            throw new SAXException("SAX parser configuration error", e);
        }
    }

    /**
     * Returns an empty stream to prevent the XML parser from attempting
     * to resolve external entity references.
     */
    public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException {
        return new InputSource(new ByteArrayInputStream(new byte[0]));
    }

}
