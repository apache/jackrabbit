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

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Simple XML serializer. This content handler serializes the received
 * SAX events as XML to a given {@link Writer} or {@link OutputStream}.
 * The serialization assumes that the incoming SAX events are well-formed,
 * i.e. that all elements are properly nested, that element and attribute
 * names are valid and that no invalid XML characters are included. Assuming
 * these preconditions are met, the result will be a well-formed XML stream.
 * <p>
 * This serializer does not have any special support for namespaces. For
 * example, namespace prefixes are declared in the resulting XML stream
 * if and only if the corresponding "xmlns" attributes are explicitly
 * included in the {@link Attributes} instances passed in
 * {@link #startElement(String, String, String, Attributes)} calls.
 * <p>
 * As a convenience this class inherits the {@link DefaultHandler} class
 * instead of just the {@link ContentHandler} interface. This makes it
 * easier to pass instances of this class to methods like
 * {@link javax.xml.parsers.SAXParser#parse(String, DefaultHandler)} that
 * expect a DefaultHandler instance instead of a ContentHandler.
 */
public class ToXmlContentHandler extends DefaultHandler {

    /**
     * The XML stream.
     */
    private final Writer writer;

    /**
     * The data part of the &lt;?xml?&gt; processing instruction included
     * at the beginning of the XML stream.
     */
    private final String declaration;

    /**
     * Flag variable that is used to track whether a start tag has had it's
     * closing ">" appended. Set to <code>true</code> by the
     * {@link #startElement(String, String, String, Attributes)} method that
     * <em>does not</em> output the closing ">". If this flag is still set
     * when the {#link {@link #endElement(String, String, String)}} method
     * is called, then the method knows that the element is empty and can
     * close it with "/>". Any other SAX event will cause the open start tag
     * to be closed with a normal ">".
     *
     * @see #closeStartTagIfOpen()
     */
    private boolean startTagIsOpen = false;

    //--------------------------------------------------------< constructors >

    /**
     * Creates an XML serializer that writes the serialized XML stream
     * to the given output stream using the given character encoding.
     *
     * @param stream XML output stream
     * @param encoding character encoding
     * @throws UnsupportedEncodingException if the encoding is not supported
     */
    public ToXmlContentHandler(OutputStream stream, String encoding)
            throws UnsupportedEncodingException {
        this.writer = new OutputStreamWriter(stream, encoding);
        this.declaration = "version=\"1.0\" encoding=\"" + encoding + "\"";
    }

    /**
     * Creates an XML serializer that writes the serialized XML stream
     * to the given output stream using the UTF-8 character encoding.
     *
     * @param stream XML output stream
     */
    public ToXmlContentHandler(OutputStream stream) {
        this.writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8);
        this.declaration = "version=\"1.0\" encoding=\"UTF-8\"";
    }

    /**
     * Creates an XML serializer that writes the serialized XML stream
     * to the given writer.
     *
     * @param writer XML output stream
     */
    public ToXmlContentHandler(Writer writer) {
        this.writer = writer;
        this.declaration = "version=\"1.0\"";
    }

    /**
     * Creates an XML serializer that writes the serialized XML stream
     * to an internal buffer. Use the {@link #toString()} method to access
     * the serialized XML document.
     */
    public ToXmlContentHandler() {
        this(new StringWriter());
    }

    //-------------------------------------------------------------< private >

    private void write(char[] ch, int start, int length, boolean attribute)
            throws SAXException {
        for (int i = start; i < start + length; i++) {
            try {
                if (ch[i] == '>') {
                    writer.write("&gt;");
                } else if (ch[i] == '<') {
                    writer.write("&lt;");
                } else if (ch[i] == '&') {
                    writer.write("&amp;");
                } else if (attribute && ch[i] == '"') {
                    writer.write("&quot;");
                } else if (attribute && ch[i] == '\'') {
                    writer.write("&apos;");
                } else {
                    writer.write(ch[i]);
                }
            } catch (IOException e) {
                throw new SAXException(
                        "Failed to output XML character: " + ch[i], e);
            }
        }
    }

    private void closeStartTagIfOpen() throws SAXException {
        if (startTagIsOpen) {
            try {
                writer.write(">");
            } catch (IOException e) {
                throw new SAXException(
                        "Failed to output XML bracket: >", e);
            }
            startTagIsOpen = false;
        }
    }

    //------------------------------------------------------< ContentHandler >

    /**
     * Starts the XML serialization by outputting the &lt;?xml?&gt; header.
     */
    public void startDocument() throws SAXException {
        processingInstruction("xml", declaration);
    }

    /**
     * Ends the XML serialization by flushing the output stream.
     */
    public void endDocument() throws SAXException {
        try {
            writer.flush();
        } catch (IOException e) {
            throw new SAXException("Failed to flush XML output", e);
        }
    }

    /**
     * Serializes a processing instruction.
     */
    public void processingInstruction(String target, String data)
            throws SAXException {
        closeStartTagIfOpen();
        try {
            writer.write("<?");
            writer.write(target);
            if (data != null) {
                writer.write(" ");
                writer.write(data);
            }
            writer.write("?>");
        } catch (IOException e) {
            throw new SAXException(
                    "Failed to output XML processing instruction: " + target, e);
        }
    }

    /**
     * Outputs the specified start tag with the given attributes.
     */
    public void startElement(
            String namespaceURI, String localName, String qName,
            Attributes atts) throws SAXException {
        closeStartTagIfOpen();
        try {
            writer.write("<");
            writer.write(qName);
            for (int i = 0; i < atts.getLength(); i++) {
                writer.write(" ");
                writer.write(atts.getQName(i));
                writer.write("=\"");
                char[] ch = atts.getValue(i).toCharArray();
                write(ch, 0, ch.length, true);
                writer.write("\"");
            }
            startTagIsOpen = true;
        } catch (IOException e) {
            throw new SAXException(
                    "Failed to output XML end tag: " + qName, e);
        }
    }

    /**
     * Escapes and outputs the given characters.
     */
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        closeStartTagIfOpen();
        write(ch, start, length, false);
    }

    /**
     * Escapes and outputs the given characters.
     */
    public void ignorableWhitespace(char[] ch, int start, int length)
            throws SAXException {
        characters(ch, start, length);
    }

    /**
     * Outputs the specified end tag.
     */
    public void endElement(
            String namespaceURI, String localName, String qName)
            throws SAXException {
        try {
            if (startTagIsOpen) {
                writer.write("/>");
                startTagIsOpen = false;
            } else {
                writer.write("</");
                writer.write(qName);
                writer.write(">");
            }
        } catch (IOException e) {
            throw new SAXException(
                    "Failed to output XML end tag: " + qName, e);
        }
    }

    //--------------------------------------------------------------< Object >

    /**
     * Returns the serialized XML document (assuming the default no-argument
     * constructor was used).
     *
     * @return serialized XML document
     */
    public String toString() {
        return writer.toString();
    }

}
