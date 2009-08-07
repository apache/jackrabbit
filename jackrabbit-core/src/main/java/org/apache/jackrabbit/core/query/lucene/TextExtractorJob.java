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

import EDU.oswego.cs.dl.util.concurrent.FutureResult;
import EDU.oswego.cs.dl.util.concurrent.Callable;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.extractor.TextExtractor;
import org.apache.jackrabbit.util.LazyFileInputStream;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.InputStream;
import java.io.Reader;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;

/**
 * <code>TextExtractorJob</code> implements a future result and is runnable
 * in a background thread.
 */
public class TextExtractorJob extends FutureResult implements Runnable {

    /**
     * UTF-8 encoding.
     */
    private static final String ENCODING_UTF8 = "UTF-8";

    /**
     * The logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(TextExtractorJob.class);

    /**
     * The command of the future result.
     */
    private final Runnable cmd;

    /**
     * The mime type of the resource to extract text from.
     */
    private final String type;

    /**
     * Set to <code>true</code> if this job timed out.
     */
    private transient boolean timedOut = false;

    /**
     * <code>true</code> if this extractor job has been flaged as discarded.
     */
    private transient boolean discarded = false;

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
    public TextExtractorJob(final TextExtractor extractor,
                            final InputStream stream,
                            final String type,
                            final String encoding) {
        this.type = type;
        this.cmd = setter(new Callable() {
            public Object call() throws Exception {
                Reader r = extractor.extractText(stream, type, encoding);
                if (r != null) {
                    if (discarded) {
                        r.close();
                        r = null;
                    } else if (timedOut) {
                        // spool a temp file to save memory
                        r = getSwappedOutReader(r);
                    }
                }
                return r;
            }
        });
    }

    /**
     * Returns the reader with the extracted text from the input stream passed
     * to the constructor of this <code>TextExtractorJob</code>. The caller of
     * this method is responsible for closing the returned reader. Returns
     * <code>null</code> if a <code>timeout</code>occurs while waiting for the
     * text extractor to get the reader.
     *
     * @return the Reader with the extracted text. Returns <code>null</code> if
     *         a timeout or an exception occured extracting the text.
     */
    public Reader getReader(long timeout) {
        Reader reader = null;
        try {
            reader = (Reader) timedGet(timeout);
        } catch (InterruptedException e) {
            // also covers TimeoutException
            // text not extracted within timeout or interrupted
            if (timeout > 0) {
                log.debug("Text extraction for {} timed out (>{}ms).",
                        type, new Long(timeout));
                timedOut = true;
            }
        } catch (InvocationTargetException e) {
            // extraction failed
            log.warn("Exception while indexing binary property: " + e.getCause());
            log.debug("Dump: ", e.getCause());
        }
        return reader;
    }

    /**
     * Discards this extractor job. If the reader within this job is ready at
     * the time of this call, it is closed. If the reader is not yet ready this
     * job will be flaged as discarded and any later call to
     * {@link #getReader(long)} will return <code>null</code>. The reader that
     * is about to be constructed by a background thread will be closed
     * automatically as soon as it becomes ready.
     */
    void discard() {
        discarded = true;
        Reader r = (Reader) peek();
        if (r != null) {
            try {
                r.close();
            } catch (IOException e) {
                log.warn("Exception when trying to discard extractor job: " + e);
            }
        }
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
        // forward to command
        cmd.run();
    }

    //----------------------------< internal >----------------------------------

    /**
     * Returns a <code>Reader</code> for <code>r</code> using a temp file.
     *
     * @param r the reader to swap out into a temp file.
     * @return a reader to the temp file.
     */
    private Reader getSwappedOutReader(Reader r) {
        final File temp;
        try {
            temp = File.createTempFile("extractor", null);
        } catch (IOException e) {
            // unable to create temp file
            // return reader as is
            return r;
        }
        Writer out;
        try {
            out = new BufferedWriter(new OutputStreamWriter(
                            new FileOutputStream(temp), ENCODING_UTF8));
        } catch (IOException e) {
            // should never happend actually
            if (!temp.delete()) {
                temp.deleteOnExit();
            }
            return r;
        }

        // spool into temp file
        InputStream in = null;
        try {
            try {
                IOUtils.copy(r, out);
                out.close();
            } finally {
                r.close();
            }
            in = new LazyFileInputStream(temp);

            return new InputStreamReader(in, ENCODING_UTF8) {
                public void close() throws IOException {
                    super.close();
                    // delete file
                    if (!temp.delete()) {
                        temp.deleteOnExit();
                    }
                }
            };
        } catch (IOException e) {
            // do some clean up
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(in);

            if (!temp.delete()) {
                temp.deleteOnExit();
            }
            // use empty string reader as fallback
            return new StringReader("");
        }
    }
}
