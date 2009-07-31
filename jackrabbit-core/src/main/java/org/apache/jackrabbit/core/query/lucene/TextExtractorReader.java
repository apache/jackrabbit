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

import java.io.Reader;
import java.io.IOException;
import java.io.StringReader;

/**
 * <code>TextExtractorReader</code> implements a specialized reader that runs
 * the text extractor in a background thread.
 */
class TextExtractorReader extends Reader {

    /**
     * Reference to the extracted text. This reference is initially
     * <code>null</code> and later set to a valid reader when the text extractor
     * finished its work.
     */
    private Reader extractedText;

    /**
     * The extractor job.
     */
    private TextExtractorJob job;

    /**
     * The timeout in milliseconds to wait at most for the text extractor
     * when {@link #isExtractorFinished()} is called.
     */
    private final long timeout;

    /**
     * Creates a new <code>TextExtractorReader</code> with the given
     * <code>job</code>.
     *
     * @param job      the extractor job.
     * @param timeout  the timeout to wait at most for the text extractor.
     */
    TextExtractorReader(TextExtractorJob job, long timeout) {
        this.job = job;
        this.timeout = timeout;
    }

    /**
     * Closes this reader and discards the contained {@link TextExtractorJob}.
     *
     * @throws IOException if an error occurs while closing this reader.
     */
    public void close() throws IOException {
        if (extractedText != null) {
            extractedText.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int read(char[] cbuf, int off, int len) throws IOException {
        if (extractedText == null) {
            String text = job.getExtractedText(timeout);
            if (text == null) {
                text = "";
            }
            extractedText = new StringReader(text);
        }
        return extractedText.read(cbuf, off, len);
    }

    /**
     * @return <code>true</code> if the text extractor within this reader has
     *         finished its work and this reader will return extracted text.
     */
    public boolean isExtractorFinished() {
        return job.hasExtractedText();
    }
}
