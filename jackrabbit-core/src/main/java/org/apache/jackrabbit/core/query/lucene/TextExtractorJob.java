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

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.extractor.TextExtractor;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.InputStream;
import java.io.Reader;

/**
 * <code>TextExtractorJob</code> implements a future result and is runnable
 * in a background thread.
 */
public class TextExtractorJob implements Runnable {

    /**
     * The logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(TextExtractorJob.class);

    /**
     * The text extractor.
     */
    private final TextExtractor extractor;

    /**
     * The binary stream.
     */
    private final InputStream stream;

    /**
     * The mime type of the resource to extract text from.
     */
    private final String type;

    /**
     * The encoding of the binary content, or <code>null</code>.
     */
    private final String encoding;

    /**
     * The extracted text. Set when the text extraction task completes.
     */
    private volatile String text = null;

    /**
     * Creates a new <code>TextExtractorJob</code> with the given
     * <code>extractor</code> on the <code>stream</code>.
     *
     * @param extractor the text extractor
     * @param stream    the stream of the binary property.
     * @param type      the mime-type of the binary content.
     * @param encoding  the encoding of the binary content. May be
     *                  <code>null</code>.
     */
    public TextExtractorJob(
            TextExtractor extractor,
            InputStream stream, String type, String encoding) {
        this.extractor = extractor;
        this.stream = stream;
        this.type = type;
        this.encoding = encoding;
    }

    public boolean hasExtractedText() {
        return text != null;
    }

    /**
     * Returns the reader with the extracted text from the input stream passed
     * to the constructor of this <code>TextExtractorJob</code>. Returns
     * <code>null</code> if a <code>timeout</code>occurs while waiting for the
     * text extractor to get the reader.
     *
     * @return the extracted text, or <code>null</code> if a timeout or
     *         an exception occurred while extracting the text
     */
    public synchronized String getExtractedText(long timeout) {
        if (text == null) {
            try {
                wait(timeout);
            } catch (InterruptedException e) {
                if (text == null) {
                    log.debug("Text extraction for {} timed out (> {}ms)",
                            type, timeout);
                }
            }
        }
        return text;
    }

    /**
     * @return a String description for this job with the mime type.
     */
    public String toString() {
        return "TextExtractorJob for " + type;
    }

    //----------------------------< Runnable >----------------------------------

    /**
     * Runs the actual text extraction.
     */
    public void run() {
        try {
            try {
                Reader reader = extractor.extractText(stream, type, encoding);
                this.text = IOUtils.toString(reader);
            } finally {
                stream.close();
            }
        } catch (Throwable e) {
            log.warn("Text extraction failed for type " + type, e);
            this.text = "";
        }
        synchronized (this) {
            notifyAll();
        }
    }

}
