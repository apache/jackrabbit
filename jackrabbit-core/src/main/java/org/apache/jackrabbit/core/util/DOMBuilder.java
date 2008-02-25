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
package org.apache.jackrabbit.core.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Document builder class. This class provides an intuitive
 * interface for incrementally building DOM documents.
 */
public final class DOMBuilder {

    /** Static factory for creating DOM DocumentBuilder instances. */
    private static final DocumentBuilderFactory BUILDER_FACTORY =
        DocumentBuilderFactory.newInstance();

    /** Static factory for creating document to output stream transformers. */
    private static final TransformerFactory TRANSFORMER_FACTORY =
        TransformerFactory.newInstance();

    /** The DOM document being built by this builder. */
    private final Document document;

    /** The current element. */
    private Element current;

    /**
     * Creates a builder for a new DOM document. A new DOM document is
     * instantiated and initialized to contain a root element with the given
     * name. The root element is set as the current element of this builder.
     *
     * @param name name of the root element
     * @throws ParserConfigurationException if a document cannot be created
     */
    public DOMBuilder(String name) throws ParserConfigurationException  {
        DocumentBuilder builder = BUILDER_FACTORY.newDocumentBuilder();
        document = builder.newDocument();
        current = document.createElement(name);
        document.appendChild(current);
    }


    /**
     * Writes the document built by this builder into the given output stream.
     * This method is normally invoked only when the document is fully built.
     *
     * @param xml XML output stream
     * @throws IOException if the document could not be written
     */
    public void write(OutputStream xml) throws IOException {
        try {
            Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
            transformer.transform(
                    new DOMSource(document), new StreamResult(xml));
        } catch (TransformerConfigurationException e) {
            IOException ioe = new IOException(e.getMessage());
            ioe.initCause(e);
            throw ioe;
        } catch (TransformerException e) {
            IOException ioe = new IOException(e.getMessage());
            ioe.initCause(e);
            throw ioe;
        }
    }

    /**
     * Creates a new element with the given name as the child of the
     * current element and makes the created element current. The
     * {@link #endElement() endElement} method needs to be called
     * to return back to the original element.
     *
     * @param name name of the new element
     */
    public void startElement(String name) {
        Element element = document.createElement(name);
        current.appendChild(element);
        current = element;
    }

    /**
     * Makes the parent element current. This method should be invoked
     * after a child element created with the
     * {@link #startElement(String) startElement} method has been fully
     * built.
     */
    public void endElement() {
        current = (Element) current.getParentNode();
    }

    /**
     * Sets the named attribute of the current element.
     *
     * @param name attribute name
     * @param value attribute value
     */
    public void setAttribute(String name, String value) {
        current.setAttribute(name, value);
    }

    /**
     * Sets the named boolean attribute of the current element.
     *
     * @param name attribute name
     * @param value boolean attribute value
     */
    public void setAttribute(String name, boolean value) {
        setAttribute(name, String.valueOf(value));
    }

    /**
     * Adds the given string as text content to the current element.
     *
     * @param content text content
     */
    public void addContent(String content) {
        current.appendChild(document.createTextNode(content));
    }

    /**
     * Adds a new child element with the given name and text content.
     * The created element will contain no attributes and no child elements
     * of its own.
     *
     * @param name child element name
     * @param content child element content
     */
    public void addContentElement(String name, String content) {
        startElement(name);
        addContent(content);
        endElement();
    }

}
