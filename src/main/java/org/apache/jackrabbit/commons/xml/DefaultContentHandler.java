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

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Adapter class for exposing a {@link ContentHandler} instance as
 * {@link DefaultHandler} object.
 *
 * @since Jackrabbit JCR Commons 1.5
 */
public class DefaultContentHandler extends DefaultHandler {

    /**
     * The adapted content handler instance.
     */
    private final ContentHandler handler;

    /**
     * Creates a {@link DefaultHandler} adapter for the given content
     * handler.
     *
     * @param handler content handler
     */
    public DefaultContentHandler(ContentHandler handler) {
        this.handler = handler;
    }

    //------------------------------------------------------< ContentHandler >

    /**
     * Delegated to {@link #handler}.
     *
     * @param ch passed through
     * @param start passed through
     * @param length passed through
     * @throws SAXException if an error occurs
     */
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        handler.characters(ch, start, length);
    }

    /**
     * Delegated to {@link #handler}.
     *
     * @throws SAXException if an error occurs
     */
    public void endDocument() throws SAXException {
        handler.endDocument();
    }

    /**
     * Delegated to {@link #handler}.
     *
     * @param namespaceURI passed through
     * @param localName passed through
     * @param qName passed through
     * @throws SAXException if an error occurs
     */
    public void endElement(
            String namespaceURI, String localName, String qName)
            throws SAXException {
        handler.endElement(namespaceURI, localName, qName);
    }

    /**
     * Delegated to {@link #handler}.
     *
     * @param prefix passed through
     * @throws SAXException if an error occurs
     */
    public void endPrefixMapping(String prefix) throws SAXException {
        handler.endPrefixMapping(prefix);
    }

    /**
     * Delegated to {@link #handler}.
     *
     * @param ch passed through
     * @param start passed through
     * @param length passed through
     * @throws SAXException if an error occurs
     */
    public void ignorableWhitespace(char[] ch, int start, int length)
            throws SAXException {
        handler.ignorableWhitespace(ch, start, length);
    }

    /**
     * Delegated to {@link #handler}.
     *
     * @param target passed through
     * @param data passed through
     * @throws SAXException if an error occurs
     */
    public void processingInstruction(String target, String data)
            throws SAXException {
        handler.processingInstruction(target, data);
    }

    /**
     * Delegated to {@link #handler}.
     *
     * @param locator passed through
     */
    public void setDocumentLocator(Locator locator) {
        handler.setDocumentLocator(locator);
    }

    /**
     * Delegated to {@link #handler}.
     *
     * @param name passed through
     * @throws SAXException if an error occurs
     */
    public void skippedEntity(String name) throws SAXException {
        handler.skippedEntity(name);
    }

    /**
     * Delegated to {@link #handler}.
     *
     * @throws SAXException if an error occurs
     */
    public void startDocument() throws SAXException {
        handler.startDocument();
    }

    /**
     * Delegated to {@link #handler}.
     *
     * @param namespaceURI passed through
     * @param localName passed through
     * @param qName passed through
     * @param atts passed through
     * @throws SAXException if an error occurs
     */
    public void startElement(
            String namespaceURI, String localName, String qName,
            Attributes atts) throws SAXException {
        handler.startElement(namespaceURI, localName, qName, atts);
    }

    /**
     * Delegated to {@link #handler}.
     *
     * @param prefix passed through
     * @param uri passed through
     * @throws SAXException if an error occurs
     */
    public void startPrefixMapping(String prefix, String uri)
            throws SAXException {
        handler.startPrefixMapping(prefix, uri);
    }

}
