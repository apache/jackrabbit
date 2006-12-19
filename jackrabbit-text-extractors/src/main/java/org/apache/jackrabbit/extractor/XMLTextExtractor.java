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

import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Text extractor for XML documents. This class extracts the text content
 * and attribute values from XML documents.
 * <p>
 * This class can handle any XML-based format
 * (<code>application/xml+something</code>), not just the base XML content
 * types reported by {@link #getContentTypes()}. However, it often makes
 * sense to use more specialized extractors that better understand the
 * specific content type.
 */
public class XMLTextExtractor extends AbstractTextExtractor {

    /**
     * Creates a new <code>XMLTextExtractor</code> instance.
     */
    public XMLTextExtractor() {
        super(new String[]{"text/xml", "application/xml"});
    }

    //-------------------------------------------------------< TextExtractor >

    /**
     * Returns a reader for the text content of the given XML document.
     * Returns an empty reader if the given encoding is not supported or
     * if the XML document could not be parsed.
     *
     * @param stream XML document
     * @param type XML content type
     * @param encoding character encoding, or <code>null</code>
     * @return reader for the text content of the given XML document,
     *         or an empty reader if the document could not be parsed
     * @throws IOException if the XML document stream can not be closed
     */
    public Reader extractText(InputStream stream, String type, String encoding)
            throws IOException {
        try {
            CharArrayWriter writer = new CharArrayWriter();
            ExtractorHandler handler = new ExtractorHandler(writer);

            // TODO: Use a pull parser to avoid the memory overhead
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            XMLReader reader = parser.getXMLReader();
            reader.setContentHandler(handler);
            reader.setErrorHandler(handler);

            // It is unspecified whether the XML parser closes the stream when
            // done parsing. To ensure that the stream gets closed just once,
            // we prevent the parser from closing it by catching the close()
            // call and explicitly close the stream in a finally block.
            InputSource source = new InputSource(new FilterInputStream(stream) {
                public void close() {
                }
            });
            if (encoding != null) {
                source.setEncoding(encoding);
            }
            reader.parse(source);

            return new CharArrayReader(writer.toCharArray());
        } catch (ParserConfigurationException e) {
            return new StringReader("");
        } catch (SAXException e) {
            return new StringReader("");
        } finally {
            stream.close();
        }
    }

}
