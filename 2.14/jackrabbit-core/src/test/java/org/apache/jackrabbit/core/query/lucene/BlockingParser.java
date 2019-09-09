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
package org.apache.jackrabbit.core.query.lucene;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.EmptyParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.XHTMLContentHandler;
import org.w3c.dom.Document;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class BlockingParser extends EmptyParser {

    public static final MediaType TYPE = MediaType.application("x-blocked");

    /**
     * Flag for blocking text extraction.
     */
    private static volatile boolean blocked = false;

    /**
     * Waits until text extraction is no longer blocked.
     */
    private synchronized static void waitIfBlocked() {
        try {
            while (blocked) {
                BlockingParser.class.wait();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Text extraction block interrupted", e);
        }
    }

    /**
     * Blocks text extraction.
     */
    static synchronized void block() {
        blocked = true;
    }

    /**
     * Unblocks text extraction.
     */
    static synchronized void unblock() {
        blocked = false;
        BlockingParser.class.notifyAll();
    }

    @Override
    public Set<MediaType> getSupportedTypes(ParseContext context) {
        return Collections.singleton(TYPE);
    }

    @Override
    public void parse(
            InputStream stream, ContentHandler handler,
            Metadata metadata, ParseContext context)
            throws SAXException {
        waitIfBlocked();
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            Document doc = dbf.newDocumentBuilder().parse(new InputSource(stream));
            String contents = doc.getDocumentElement().getTextContent();
            XHTMLContentHandler xhtml = new XHTMLContentHandler(handler, metadata);
            xhtml.startDocument();
            xhtml.element("p", contents);
            xhtml.endDocument();
        } catch (ParserConfigurationException ex) {
            throw new SAXException(ex);
        } catch (IOException ex) {
            throw new SAXException(ex);
        }
    }

}
