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

import org.pdfbox.pdfparser.PDFParser;
import org.pdfbox.pdmodel.PDDocument;
import org.pdfbox.util.PDFTextStripper;

import java.io.Reader;
import java.io.InputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.CharArrayWriter;
import java.io.CharArrayReader;
import java.io.StringReader;

/**
 * Text extractor for Portable Document Format (PDF).
 */
public class PdfTextExtractor extends AbstractTextExtractor {

    /**
     * Force loading of dependent class.
     */
    static {
        PDFParser.class.getName();
    }

    /**
     * Creates a new <code>PdfTextExtractor</code> instance.
     */
    public PdfTextExtractor() {
        super(new String[]{"application/pdf"});
    }

    //-------------------------------------------------------< TextExtractor >

    /**
     * {@inheritDoc}
     */ 
    public Reader extractText(InputStream stream,
                              String type,
                              String encoding) throws IOException {
        try {
            PDFParser parser = new PDFParser(new BufferedInputStream(stream));
            try {
                parser.parse();
                PDDocument document = parser.getPDDocument();
                CharArrayWriter writer = new CharArrayWriter();

                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setLineSeparator("\n");
                stripper.writeText(document, writer);

                return new CharArrayReader(writer.toCharArray());
            } finally {
                PDDocument doc = parser.getPDDocument();
                if (doc != null) {
                    doc.close();
                }
            }
        } catch (Exception e) {
            // it may happen that PDFParser throws a runtime
            // exception when parsing certain pdf documents
            return new StringReader("");
        } finally {
            stream.close();
        }
    }
}
