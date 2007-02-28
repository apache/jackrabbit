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
package org.apache.jackrabbit.spi.rmi.common;

import java.io.InputStream;
import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;

/**
 * <code>SerializableInputStream</code> implements an input stream that can
 * be serialized.
 */
public class SerializableInputStream extends InputStream implements Serializable {

    private InputStream stream;

    /**
     * Creates a new serializable stream. A client must not use any of the
     * available methods on this input stream if he wishes to serialize this
     * stream, otherwise the passed stream is only serialized partially!
     *
     * @param stream the stream to serialize.
     */
    public SerializableInputStream(InputStream stream) {
        this.stream = stream;
    }

    //--------------------------< InputStream >---------------------------------

    /**
     * Forwards the call to the underlying stream.
     */
    public int read() throws IOException {
        return stream.read();
    }

    /**
     * Forwards the call to the underlying stream.
     */
    public int available() throws IOException {
        return stream.available();
    }

    /**
     * Forwards the call to the underlying stream.
     */
    public void close() throws IOException {
        stream.close();
    }

    /**
     * Forwards the call to the underlying stream.
     */
    public void reset() throws IOException {
        stream.reset();
    }

    /**
     * Forwards the call to the underlying stream.
     */
    public boolean markSupported() {
        return stream.markSupported();
    }

    /**
     * Forwards the call to the underlying stream.
     */
    public void mark(int readlimit) {
        stream.mark(readlimit);
    }

    /**
     * Forwards the call to the underlying stream.
     */
    public long skip(long n) throws IOException {
        return stream.skip(n);
    }

    /**
     * Forwards the call to the underlying stream.
     */
    public int read(byte b[]) throws IOException {
        return stream.read(b);
    }

    /**
     * Forwards the call to the underlying stream.
     */
    public int read(byte b[], int off, int len) throws IOException {
        return stream.read(b, off, len);
    }

    //---------------------------< Serializable >-------------------------------

    /**
     * Writes the contents of the underlying stream to the
     * <code>ObjectOutputStream</code>.
     *
     * @param out The <code>ObjectOutputStream</code> to where the binary data
     *            is copied.
     * @throws IOException If an error occurs writing the binary data.
     */
    private void writeObject(ObjectOutputStream out)
            throws IOException {
        byte[] buffer = new byte[4096];
        int bytes;
        while ((bytes = stream.read(buffer)) >= 0) {
            // Write a segment of the input stream
            if (bytes > 0) {
                // just to ensure that no 0 is written
                out.writeInt(bytes);
                out.write(buffer, 0, bytes);
            }
        }
        // Write the end of stream marker
        out.writeInt(0);
        // close stream
        stream.close();
    }

    /**
     * Reads the binary data from the <code>ObjectInputStream</code> into a
     * temporary file that is used to back up the binary stream contents of the
     * constructed value instance. The temporary file gets deleted when the
     * binary stream is closed or garbage collected.
     *
     * @param in The <code>ObjectInputStream</code> from where to get the binary
     *           data.
     * @throws IOException If an error occurs reading the binary data.
     */
    private void readObject(ObjectInputStream in)
            throws IOException {
        final File file = File.createTempFile("serializable-stream", "bin");

        OutputStream out = new FileOutputStream(file);
        byte[] buffer = new byte[4096];
        for (int bytes = in.readInt(); bytes > 0; bytes = in.readInt()) {
            if (buffer.length < bytes) {
                buffer = new byte[bytes];
            }
            in.readFully(buffer, 0, bytes);
            out.write(buffer, 0, bytes);
        }
        out.close();

        stream = new FileInputStream(file) {

            private boolean closed = false;

            public void close() throws IOException {
                super.close();
                closed = true;
                file.delete();
            }

            protected void finalize() throws IOException {
                try {
                    if (!closed) {
                        file.delete();
                    }
                } finally {
                    super.finalize();
                }
            }
        };
    }
}
