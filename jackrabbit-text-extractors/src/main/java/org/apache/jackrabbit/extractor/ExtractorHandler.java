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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Writer;

import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Utility class for extracting text content from an XML document.
 * An instance of this class is a SAX event handler that extracts
 * character data and attribute values from the SAX events and writes
 * the extracted content to a given {@link Writer}.
 * <p>
 * Any whitespace sequences are imploded into a single space character
 * and consecutive attribute values and character data are delimited
 * using spaces.
 * <p>
 * This class also implements the {@link ErrorHandler} interface by
 * ignoring all errors and warnings. This is useful in avoiding the
 * default console output or other error logging of many XML parsers.
 *
 * @see XMLTextExtractor
 */
class ExtractorHandler extends DefaultHandler implements ErrorHandler {

    /**
     * Separator that is written between consecutive text and attribute values.
     */
    private static final char SPACE = ' ';

    /**
     * The writer to which the selected text content is written.
     */
    private final Writer writer;

    /**
     * Flag for outputting a space before the next character to be outputted.
     * Used to implode all whitespace sequences and to separate consecutive
     * attribute values and text elements.
     */
    private boolean space;

    /**
     * Creates an extractor handler that writes text content to the given
     * writer.
     *
     * @param writer writer to which the XML text content is written
     */
    public ExtractorHandler(Writer writer) {
        this.writer = writer;
        this.space = false;
    }

    //------------------------------------------------------< DefaultHandler >

    /**
     * Writes attribute values to the underlying writer.
     *
     * @param uri ignored
     * @param local ignored
     * @param name ignored
     * @param attributes attributes, whose values to extract
     * @throws SAXException on IO errors
     */
    public void startElement(
            String uri, String local, String name, Attributes attributes)
            throws SAXException {
        for (int i = 0; i < attributes.getLength(); i++) {
            String value = attributes.getValue(i);
            characters(value.toCharArray(), 0, value.length());
        }
    }

    /**
     * Writes the given characters to the underlying writer.
     *
     * @param ch character array that contains the characters to be written
     * @param start start index within the array
     * @param length number of characters to write
     * @throws SAXException on IO errors
     */
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        try {
            for (int i = 0; i < length; i++) {
                if (Character.isSpaceChar(ch[start + i])) {
                    space = true;
                } else {
                    if (space) {
                        writer.write(SPACE);
                        space = false;
                    }
                    writer.write(ch[start + i]);
                }
            }
            space = true;
        } catch (IOException e) {
            throw new SAXException(e.getMessage());
        }
    }

    //------------------------------------------------------< EntityResolver >

    /**
     * Disables loading of external entities.
     */
    public InputSource resolveEntity(String publicId, String systemId) { 
        return new InputSource(new ByteArrayInputStream(new byte[0])); 
    }

    //--------------------------------------------------------< ErrorHandler >

    /**
     * Ignored.
     *
     * @param exception ignored
     */
    public void warning(SAXParseException exception) {
    }

    /**
     * Ignored.
     *
     * @param exception ignored
     */
    public void error(SAXParseException exception) {
    }

    /**
     * Ignored.
     *
     * @param exception ignored
     */
    public void fatalError(SAXParseException exception) {
    }

}
