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
package org.apache.jackrabbit.core.fs.local;

import org.apache.jackrabbit.core.fs.RandomAccessOutputStream;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Implements a buffered output stream on a random access file.
 */
class RAFOutputStream extends RandomAccessOutputStream {

    /**
     * The default size of the write buffer in bytes.
     */
    static final int DEFAULT_BUFFER_SIZE = 1024;

    /**
     * The write buffer.
     */
    private final byte[] buffer;

    /**
     * The underlying <code>RandomAccessFile</code>.
     */
    protected RandomAccessFile raf;

    /**
     * The starting position of the buffer in the code.
     */
    private long bufferStart;

    /**
     * The end of valid data in the buffer.
     */
    private int bufferEnd;

    /**
     * Dummy buffer for {@link #write(int)}.
     */
    private byte[] one = new byte[1];

    /**
     * Contructs a new output stream with the given buffer size.
     *
     * @param raf  the underlying <code>RandomAccessFile</code>.
     * @param size the size of the buffer.
     */
    public RAFOutputStream(RandomAccessFile raf, int size) throws IOException {
        this.raf = raf;
        this.buffer = new byte[size];
        bufferStart = raf.getFilePointer();
    }

    /**
     * Contructs a new output stream with the default buffer size:
     * {@link #DEFAULT_BUFFER_SIZE}.
     *
     * @param raf the underlying <code>RandomAccessFile</code>.
     */
    public RAFOutputStream(RandomAccessFile raf) throws IOException {
        this(raf, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Returns the current filepointer
     *
     * @return the current filepointer
     */
    public long getFilePointer() {
        return bufferStart + bufferEnd;
    }

    /**
     * {@inheritDoc}
     */
    public void seek(long position) throws IOException {
        flush();
        raf.seek(position);
        bufferStart = position;
    }

    //---------------------------------------------------------< OutputStream >
    /**
     * {@inheritDoc}
     */
    public void write(int b) throws IOException {
        one[0] = (byte) b;
        write(one, 0, 1);
    }

    /**
     * {@inheritDoc}
     */
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    /**
     * {@inheritDoc}
     */
    public void write(byte[] b, int off, int len) throws IOException {
        if (len > buffer.length - bufferEnd) {
            flush();
            raf.write(b, off, len);
        } else {
            System.arraycopy(b, off, buffer, bufferEnd, len);
            bufferEnd += len;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void flush() throws IOException {
        raf.write(buffer, 0, bufferEnd);
        bufferEnd = 0;
        bufferStart = raf.getFilePointer();
    }

    /**
     * This method also closes the underlying <code>RandomAccessFile</code>.
     * <p/>
     * {@inheritDoc}
     */
    public void close() throws IOException {
        flush();
        raf.close();
        raf = null;
    }
}
