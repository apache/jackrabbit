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
package org.apache.jackrabbit.core.util;

import java.io.InputStream;
import java.io.IOException;

/**
 * Implements a input stream that keeps track of the number of bytes read.
 */
public class TrackingInputStream extends InputStream {

    /**
     * The underlying input stream
     */
    private final InputStream in;

    /**
     * the current position
     */
    private long position;

    /**
     * the mark position
     */
    private long markPos;

    /**
     * Creates a new tracking input stream
     * @param in the underlying input stream
     */
    public TrackingInputStream(InputStream in) {
        this.in = in;
    }

    /**
     * {@inheritDoc}
     */
    public int available() throws IOException {
        return in.available();
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws IOException {
        in.close();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void reset() throws IOException {
        in.reset();
        position = markPos;
    }

    /**
     * {@inheritDoc}
     */
    public boolean markSupported() {
        return in.markSupported();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void mark(int readlimit) {
        in.mark(readlimit);
        markPos = position;
    }

    /**
     * {@inheritDoc}
     */
    public long skip(long n) throws IOException {
        long read = in.skip(n);
        if (read > 0) {
            position += read;
        }
        return read;
    }

    /**
     * {@inheritDoc}
     */
    public int read(byte[] b) throws IOException {
        int read = in.read(b);
        if (read > 0) {
            position += read;
        }
        return read;
    }

    /**
     * {@inheritDoc}
     */
    public int read(byte[] b, int off, int len) throws IOException {
        int read = in.read(b, off, len);
        if (read > 0) {
            position += read;
        }
        return read;
    }

    /**
     * {@inheritDoc}
     */
    public int read() throws IOException {
        int read = in.read();
        if (read >= 0) {
            position++;
        }
        return read;
    }

    /**
     * Returns the number of bytes read so far.
     * @return the number of bytes.
     */
    public long getPosition() {
        return position;
    }

}
