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
package org.apache.jackrabbit.rmi.xml;

import java.io.ByteArrayOutputStream;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * Base class for a SAX content handler for importing XML data. This
 * class provides a general mechanism for converting a SAX event stream
 * to raw XML data and feeding the received byte array into an import
 * method. Subclasses can provide different import mechanisms simply by
 * implementing the abstract {@link #importXML(byte[]) importXML(byte[])}
 * method.
 */
public abstract class ImportContentHandler implements ContentHandler {

    /** Internal buffer for the XML byte stream. */
    private ByteArrayOutputStream buffer;

    /** The internal XML serializer. */
    private ContentHandler handler;

    /**
     * Creates a SAX content handler for importing XML data.
     */
    public ImportContentHandler() {
        this.buffer = new ByteArrayOutputStream();
        this.handler = new XMLSerializer(buffer, new OutputFormat());
    }

    /**
     * Imports the given XML data. This method is called by the
     * {@link #endDocument() endDocument()} method after the received
     * XML stream has been serialized.
     * <p>
     * Subclasses must implement this method to provide the actual
     * import mechanism.
     *
     * @param xml the XML data to import
     * @throws Exception on import errors
     */
    protected abstract void importXML(byte[] xml) throws Exception;

    /** {@inheritDoc} */
    public void setDocumentLocator(Locator locator) {
        handler.setDocumentLocator(locator);
    }

    /** {@inheritDoc} */
    public void startDocument() throws SAXException {
        handler.startDocument();
    }

    /** {@inheritDoc} */
    public void endDocument() throws SAXException {
        handler.endDocument();
        try {
            importXML(buffer.toByteArray());
        } catch (Exception ex) {
            throw new SAXException(ex);
        }
    }

    /** {@inheritDoc} */
    public void startPrefixMapping(String prefix, String uri)
            throws SAXException {
        handler.startPrefixMapping(prefix, uri);
    }

    /** {@inheritDoc} */
    public void endPrefixMapping(String prefix) throws SAXException {
        handler.endPrefixMapping(prefix);
    }

    /** {@inheritDoc} */
    public void startElement(String uri, String localName, String qName,
            Attributes atts) throws SAXException {
        handler.startElement(uri, localName, qName, atts);
    }

    /** {@inheritDoc} */
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        handler.endElement(uri, localName, qName);
    }

    /** {@inheritDoc} */
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        handler.characters(ch, start, length);
    }

    /** {@inheritDoc} */
    public void ignorableWhitespace(char[] ch, int start, int length)
            throws SAXException {
        handler.ignorableWhitespace(ch, start, length);
    }

    /** {@inheritDoc} */
    public void processingInstruction(String target, String data)
            throws SAXException {
        handler.processingInstruction(target, data);
    }

    /** {@inheritDoc} */
    public void skippedEntity(String name) throws SAXException {
        handler.skippedEntity(name);
    }

}
