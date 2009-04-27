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
package org.apache.jackrabbit.core.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.io.input.AutoCloseInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This input stream delays opening the file until the first byte is read, and
 * closes and discards the underlying stream as soon as the end of input has
 * been reached or when the stream is explicitly closed.
 */
public class LazyFileInputStream extends AutoCloseInputStream {

    private static Logger log = LoggerFactory.getLogger(LazyFileInputStream.class);

    /**
     * The file to read from.
     */
    protected final File file;

    /**
     * True if the input stream was opened. It is also set to true if the stream
     * was closed without reading (to avoid opening the file after the stream
     * was closed).
     */
    protected boolean opened;

    /**
     * Create a lazy input stream for the given file.
     * The file is not opened until the first byte is read from the stream.
     * 
     * @param file the file
     */
    protected LazyFileInputStream(File file) {
        super(null);
        this.file = file;
    }

    /**
     * Open the stream if required.
     * 
     * @throws IOException
     */
    protected void openStream() throws IOException {
        if (!opened) {
            opened = true;
            in = new FileInputStream(file);
        }
    }

    /**
     * {@inheritDoc}
     * When the stream is consumed, the database objects held by the instance are closed.
     */
    public int read() throws IOException {
        openStream();
        return super.read();
    }

    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        openStream();
        return super.read(b, off, len);
    }

    public void close() throws IOException {
        // make sure the file is not opened afterwards
        opened = true;
        super.close();
    }

    public long skip(long n) throws IOException {
        openStream();
        return super.skip(n);
    }

    public int available() throws IOException {
        openStream();
        return super.available();
    }

    public void mark(int readlimit) {
        try {
            openStream();
        } catch (IOException e) {
            log.info("Error getting underlying stream: ", e);
        }
        super.mark(readlimit);
    }

    public void reset() throws IOException {
        openStream();
        super.reset();
    }

    public boolean markSupported() {
        try {
            openStream();
        } catch (IOException e) {
            log.info("Error getting underlying stream: ", e);
            return false;
        }
        return super.markSupported();
    }

}
