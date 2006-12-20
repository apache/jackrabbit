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
import java.io.Reader;

import org.apache.jackrabbit.extractor.TextExtractor;

/**
 * Reader that extracts the text content of a binary stream for reading
 * only when the first character is requested. This class is used by the
 * {@link NodeIndexer} class to postpone text extraction to when the
 * content is actually needs.
 *
 * @see http://issues.apache.org/jira/browse/JCR-264
 */
public class TextExtractorReader extends Reader {

    /**
     * Text extractor to use in extracting text content from the binary stream.
     */
    private final TextExtractor extractor;

    /**
     * Binary stream from which to extract the content for reading.
     */
    private final InputStream stream;

    /**
     * Content type of the binary stream.
     */
    private final String type;

    /**
     * Character encoding of the binary stream, or <code>null</code>.
     */
    private final String encoding;

    /**
     * Reader for the extracted text content. Set to <code>null</code> until
     * the first character request triggers the text extraction.
     */
    private Reader reader;

    /**
     * Creates a reader that extracts the text content from the given binary
     * stream.
     *
     * @param extractor text extractor
     * @param stream binary stream
     * @param type content type
     * @param encoding character encoding, or <code>null</code>
     */
    public TextExtractorReader(
            TextExtractor extractor, InputStream stream,
            String type, String encoding) {
        this.extractor = extractor;
        this.stream = stream;
        this.type = type;
        this.encoding = encoding;
        this.reader = null;
    }

    //---------------------------------------------------------< InputStream >

    /**
     * Reads up to the given number of characters to the given buffer position
     * from the extracted text content reader. Uses the text extractor to
     * create the text content reader when first invoked.
     *
     * @param buffer buffer to place characters in
     * @param offset buffer offset
     * @param length maximum number of characters to read
     * @return number of read characters
     * @throws IOException if text extraction fails
     */
    public int read(char[] buffer, int offset, int length) throws IOException {
        if (reader == null) {
            reader = extractor.extractText(stream, type, encoding);
        }
        return reader.read(buffer, offset, length);
    }

    /**
     * Closes the reader of the extracted text, or the binary stream if the
     * text content was never extracted.
     *
     * @throws IOException if the reader or stream can not be closed
     */
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
        } else {
            stream.close();
        }
    }

}
