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
package org.apache.jackrabbit.core.query;

import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.query.lucene.FieldNames;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.BLOBFileValue;
import org.apache.jackrabbit.core.value.InternalValue;
import org.pdfbox.pdfparser.PDFParser;
import org.pdfbox.pdmodel.PDDocument;
import org.pdfbox.util.PDFTextStripper;

/**
 * Extracts texts from Adobe PDF document binary data.
 * Taken from Jakarta Slide class
 * <code>org.apache.slide.extractor.PDFExtractor</code>
 */
public class PdfTextFilter implements TextFilter {

    /**
     * Force loading of dependent class.
     */
    static {
        PDFParser.class.getName();
    }

    /**
     * @return <code>true</code> for <code>application/pdf</code>, <code>false</code> otherwise.
     */
    public boolean canFilter(String mimeType) {
        return "application/pdf".equalsIgnoreCase(mimeType);
    }

    /**
     * Returns a map with a single entry for field {@link FieldNames#FULLTEXT}.
     * @param data object containing Adobe PDF document data.
     * @param encoding text encoding is not used, since it is specified in the data.
     * @return a map with a single Reader value for field {@link FieldNames#FULLTEXT}.
     * @throws RepositoryException if data is a multi-value property or it does not
     * contain valid PDF document.
     */
    public Map doFilter(PropertyState data, String encoding) throws RepositoryException {
        InternalValue[] values = data.getValues();
        if (values.length > 0) {
            final BLOBFileValue blob = (BLOBFileValue) values[0].internalValue();
            LazyReader reader = new LazyReader() {
                protected void initializeReader() throws IOException {
                    PDFParser parser;
                    InputStream in;
                    try {
                        in = blob.getStream();
                    } catch (RepositoryException e) {
                        throw new IOException(e.getMessage());
                    }

                    try {
                        parser = new PDFParser(new BufferedInputStream(in));
                        parser.parse();

                        PDDocument document = parser.getPDDocument();
                        try {
                            CharArrayWriter writer = new CharArrayWriter();

                            PDFTextStripper stripper = new PDFTextStripper();
                            stripper.setLineSeparator("\n");
                            stripper.writeText(document, writer);

                            delegate = new CharArrayReader(writer.toCharArray());
                        } finally {
                            document.close();
                        }
                    } catch (Exception e) {
                        // it may happen that PDFParser throws a runtime
                        // exception when parsing certain pdf documents
                        throw new IOException(e.getMessage());
                    } finally {
                        in.close();
                    }
                }
            };

            Map result = new HashMap();
            result.put(FieldNames.FULLTEXT, reader);
            return result;
        } else {
            // multi value not supported
            throw new RepositoryException("Multi-valued binary properties not supported.");
        }
    }
}