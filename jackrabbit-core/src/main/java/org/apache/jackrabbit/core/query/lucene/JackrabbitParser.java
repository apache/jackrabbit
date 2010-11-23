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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.parser.image.ImageParser;
import org.apache.tika.parser.microsoft.OfficeParser;
import org.apache.tika.parser.odf.OpenDocumentParser;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.parser.rtf.RTFParser;
import org.apache.tika.parser.txt.TXTParser;
import org.apache.tika.parser.xml.XMLParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Jackrabbit wrapper for Tika parsers. Uses a Tika {@link AutoDetectParser}
 * for all parsing requests, but sets it up with Jackrabbit-specific
 * configuration and implements backwards compatibility support for old
 * <code>textExtractorClasses</code> configurations.
 *
 * @since Apache Jackrabbit 2.0
 */
class JackrabbitParser implements Parser {

    /**
     * Logger instance.
     */
    private static final Logger logger =
        LoggerFactory.getLogger(JackrabbitParser.class);

    /**
     * Flag for blocking all text extraction. Used by the Jackrabbit test suite.
     */
    private static volatile boolean blocked = false;

    /**
     * The configured Tika parser.
     */
    private final AutoDetectParser parser;

    /**
     * Creates a parser using the default Jackrabbit-specific configuration
     * settings.
     */
    public JackrabbitParser() {
        InputStream stream =
            JackrabbitParser.class.getResourceAsStream("tika-config.xml");
        try {
            if (stream != null) {
                try {
                    parser = new AutoDetectParser(new TikaConfig(stream));
                } finally {
                    stream.close();
                }
            } else {
                parser = new AutoDetectParser();
            }
        } catch (Exception e) {
            // Should never happen
            throw new RuntimeException(
                    "Unable to load embedded Tika configuration", e);
        }
    }

    /**
     * Backwards compatibility method to support old Jackrabbit 1.x
     * <code>textExtractorClasses</code> configurations. Implements a best
     * effort mapping from the old-style text extractor classes to
     * corresponding Tika parsers.
     *
     * @param classes configured list of text extractor classes
     */
    public void setTextFilterClasses(String classes) {
        Map<MediaType, Parser> parsers = new HashMap<MediaType, Parser>();

        StringTokenizer tokenizer = new StringTokenizer(classes, ", \t\n\r\f");
        while (tokenizer.hasMoreTokens()) {
            String name = tokenizer.nextToken();
            if (name.equals(
                    "org.apache.jackrabbit.extractor.HTMLTextExtractor")) {
                parsers.put(MediaType.text("html"), new HtmlParser());
            } else if (name.equals("org.apache.jackrabbit.extractor.MsExcelTextExtractor")) {
                Parser parser = new OfficeParser();
                parsers.put(MediaType.application("vnd.ms-excel"), parser);
                parsers.put(MediaType.application("msexcel"), parser);
                parsers.put(MediaType.application("excel"), parser);
            } else if (name.equals("org.apache.jackrabbit.extractor.MsOutlookTextExtractor")) {
                parsers.put(MediaType.application("vnd.ms-outlook"), new OfficeParser());
            } else if (name.equals("org.apache.jackrabbit.extractor.MsPowerPointExtractor")
                    || name.equals("org.apache.jackrabbit.extractor.MsPowerPointTextExtractor")) {
                Parser parser = new OfficeParser();
                parsers.put(MediaType.application("vnd.ms-powerpoint"), parser);
                parsers.put(MediaType.application("mspowerpoint"), parser);
                parsers.put(MediaType.application("powerpoint"), parser);
            } else if (name.equals("org.apache.jackrabbit.extractor.MsWordTextExtractor")) {
                Parser parser = new OfficeParser();
                parsers.put(MediaType.application("vnd.ms-word"), parser);
                parsers.put(MediaType.application("msword"), parser);
            } else if (name.equals("org.apache.jackrabbit.extractor.MsTextExtractor")) {
                Parser parser = new OfficeParser();
                parsers.put(MediaType.application("vnd.ms-word"), parser); 
                parsers.put(MediaType.application("msword"), parser);
                parsers.put(MediaType.application("vnd.ms-powerpoint"), parser);
                parsers.put(MediaType.application("mspowerpoint"), parser);
                parsers.put(MediaType.application("vnd.ms-excel"), parser);
                parsers.put(MediaType.application("vnd.openxmlformats-officedocument.wordprocessingml.document"), parser);
                parsers.put(MediaType.application("vnd.openxmlformats-officedocument.presentationml.presentation"), parser);
                parsers.put(MediaType.application("vnd.openxmlformats-officedocument.spreadsheetml.sheet"), parser);
            } else if (name.equals("org.apache.jackrabbit.extractor.OpenOfficeTextExtractor")) {
                Parser parser = new OpenDocumentParser();
                parsers.put(MediaType.application("vnd.oasis.opendocument.database"), parser);
                parsers.put(MediaType.application("vnd.oasis.opendocument.formula"), parser);
                parsers.put(MediaType.application("vnd.oasis.opendocument.graphics"), parser);
                parsers.put(MediaType.application("vnd.oasis.opendocument.presentation"), parser);
                parsers.put(MediaType.application("vnd.oasis.opendocument.spreadsheet"), parser);
                parsers.put(MediaType.application("vnd.oasis.opendocument.text"), parser);
                parsers.put(MediaType.application("vnd.sun.xml.calc"), parser);
                parsers.put(MediaType.application("vnd.sun.xml.draw"), parser);
                parsers.put(MediaType.application("vnd.sun.xml.impress"), parser);
                parsers.put(MediaType.application("vnd.sun.xml.writer"), parser);
            } else if (name.equals("org.apache.jackrabbit.extractor.PdfTextExtractor")) {
                parsers.put(MediaType.application("pdf"), new PDFParser());
            } else if (name.equals("org.apache.jackrabbit.extractor.PlainTextExtractor")) {
                parsers.put(MediaType.TEXT_PLAIN, new TXTParser());
            } else if (name.equals("org.apache.jackrabbit.extractor.PngTextExtractor")) {
                Parser parser = new ImageParser();
                parsers.put(MediaType.image("png"), parser);
                parsers.put(MediaType.image("apng"), parser);
                parsers.put(MediaType.image("mng"), parser);
            } else if (name.equals("org.apache.jackrabbit.extractor.RTFTextExtractor")) {
                Parser parser = new RTFParser();
                parsers.put(MediaType.application("rtf"), parser);
                parsers.put(MediaType.text("rtf"), parser);
            } else if (name.equals("org.apache.jackrabbit.extractor.XMLTextExtractor")) {
                Parser parser = new XMLParser();
                parsers.put(MediaType.APPLICATION_XML, parser);
                parsers.put(MediaType.text("xml"), parser);
            } else {
                logger.warn("Ignoring unknown text extractor class: {}", name);
            }
        }

        parser.setParsers(parsers);
    }

    /**
     * Delegates the call to the configured {@link AutoDetectParser}.
     */
    public Set<MediaType> getSupportedTypes(ParseContext context) {
        return parser.getSupportedTypes(context);
    }

    /**
     * Delegates the call to the configured {@link AutoDetectParser}.
     */
    public void parse(
            InputStream stream, ContentHandler handler,
            Metadata metadata, ParseContext context)
            throws IOException, SAXException, TikaException {
        waitIfBlocked();
        parser.parse(stream, handler, metadata, context);
    }

    public void parse(
            InputStream stream, ContentHandler handler, Metadata metadata)
            throws IOException, SAXException, TikaException {
        parse(stream, handler, metadata, new ParseContext());
    }

    /**
     * Waits until text extraction is no longer blocked. The block is only
     * ever activated in the Jackrabbit test suite when testing delayed
     * text extraction.
     *
     * @throws TikaException if the block was interrupted
     */
    private synchronized static void waitIfBlocked() throws TikaException {
        try {
            while (blocked) {
                JackrabbitParser.class.wait();
            }
        } catch (InterruptedException e) {
            throw new TikaException("Text extraction block interrupted", e);
        }
    }

    /**
     * Blocks all text extraction tasks.
     */
    static synchronized void block() {
        blocked = true;
    }

    /**
     * Unblocks all text extraction tasks.
     */
    static synchronized void unblock() {
        blocked = false;
        JackrabbitParser.class.notifyAll();
    }

}
