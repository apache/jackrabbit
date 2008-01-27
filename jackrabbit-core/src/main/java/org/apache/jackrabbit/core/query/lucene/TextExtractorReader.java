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

import EDU.oswego.cs.dl.util.concurrent.Executor;
import EDU.oswego.cs.dl.util.concurrent.DirectExecutor;

/**
 * <code>TextExtractorReader</code> implements a specialized reader that runs
 * the text extractor in a background thread.
 */
class TextExtractorReader extends Reader {

    /**
     * A direct executor in case text extraction is requested for immediate use.
     */
    private static final Executor DIRECT_EXECUTOR = new DirectExecutor();

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
     * The pooled executor.
     */
    private final Executor executor;

    /**
     * The timeout in milliseconds to wait at most for the text extractor
     * when {@link #isExtractorFinished()} is called.
     */
    private final long timeout;

    /**
     * Set to <code>true</code> when the text extractor job has been started
     * and is running.
     */
    private boolean jobStarted = false;

    /**
     * Creates a new <code>TextExtractorReader</code> with the given
     * <code>job</code>.
     *
     * @param job      the extractor job.
     * @param executor the executor to use when text extraction is requested.
     * @param timeout  the timeout to wait at most for the text extractor.
     */
    TextExtractorReader(TextExtractorJob job, Executor executor, long timeout) {
        this.job = job;
        this.executor = executor;
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
        if (jobStarted) {
            job.discard();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int read(char[] cbuf, int off, int len) throws IOException {
        if (extractedText == null) {
            // no reader present
            // check if job is started already
            if (jobStarted) {
                // wait until available
                extractedText = job.getReader(Long.MAX_VALUE);
            } else {
                // execute with current thread
                try {
                    DIRECT_EXECUTOR.execute(job);
                } catch (InterruptedException e) {
                    // current thread is in interrupted state
                    // -> ignore (job will not return a reader, which is fine)
                }
                extractedText = job.getReader(0);
            }

            if (extractedText == null) {
                // exception occurred
                extractedText = new StringReader("");
            }
        }
        return extractedText.read(cbuf, off, len);
    }

    /**
     * @return <code>true</code> if the text extractor within this reader has
     *         finished its work and this reader will return extracted text.
     */
    public boolean isExtractorFinished() {
        if (!jobStarted) {
            try {
                executor.execute(job);
                jobStarted = true;
            } catch (InterruptedException e) {
                // this thread is in interrupted state
                return false;
            }
            extractedText = job.getReader(timeout);
        } else {
            // job is already running, check for immediate result
            extractedText = job.getReader(0);
        }

        if (extractedText == null && job.getException() != null) {
            // exception occurred
            extractedText = new StringReader("");
        }

        return extractedText != null;
    }
}
