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
package org.apache.jackrabbit.rmi.value;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;

import javax.jcr.PropertyType;
import javax.jcr.ValueFormatException;

/**
 * The <code>BinaryValue</code> class implements the committed value state for
 * Binary values as a part of the State design pattern (Gof) used by this
 * package.
 * <p>
 * NOTE: This class forwards the <code>InputStream</code> from which it was
 * created through the {@link #getStream()} method but does not close the
 * stream. It is the sole responsibility of the user of this value to close the
 * stream if not needed anymore to prevent memory loss.
 * <p>
 * This class implements {@link #readObject(ObjectInputStream)} and
 * {@link #writeObject(ObjectOutputStream)} methods to (de-)serialize the
 * data.
 *
 * @since 0.16.4.1
 * @see org.apache.jackrabbit.rmi.value.SerialValue
 */
public class BinaryValue implements Serializable, StatefulValue {

    /** The serial version UID */
    private static final long serialVersionUID = -2410070522924274051L;

    /** The <code>InputStream</code> providing the value */
    private InputStream stream;

    /**
     * Creates an instance on the given <code>InputStream</code>. This exact
     * stream will be provided by the {@link #getStream()}, thus care must be
     * taken to not inadvertendly read or close the stream.
     *
     * @param stream The <code>InputStream</code> providing the value.
     */
    protected BinaryValue(InputStream stream) {
        this.stream = stream;
    }

    /**
     * Creates an instance providing the UTF-8 representation of the given
     * string value.
     *
     * @param value The string whose UTF-8 representation is provided as the
     *      value of this instance.
     *
     * @throws ValueFormatException If the platform does not support UTF-8
     *      encoding (which is unlikely as UTF-8 is required to be available
     *      on all platforms).
     */
    protected BinaryValue(String value) throws ValueFormatException {
        this(toStream(value));
    }

    /**
     * Helper method to convert a string value into an <code>InputStream</code>
     * from which the UTF-8 representation can be read.
     *
     * @param value The string value to be made available through a stream.
     *
     * @return The <code>InputStream</code> from which the UTF-8 representation
     *      of the <code>value</code> may be read.
     *
     * @throws ValueFormatException If the platform does not support UTF-8
     *      encoding (which is unlikely as UTF-8 is required to be available
     *      on all platforms).
     */
    protected static InputStream toStream(String value)
            throws ValueFormatException {
        try {
            return new ByteArrayInputStream(value.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new ValueFormatException("Invalid string value encoding", e);
        }
    }

    /**
     * Returns the <code>InputStream</code> from which this instance has been
     * created.
     *
     * @return value stream
     */
    public InputStream getStream() {
        return stream;
    }

    /**
     * Returns <code>PropertyType.BINARY</code>.
     *
     * @return property type
     */
    public int getType() {
        return PropertyType.BINARY;
    }

    /**
     * Always throws <code>IllegalStateException</code> because only an
     * <code>InputStream</code> is available from this implementation.
     *
     * @return nothing
     * @throws IllegalStateException as defined above.
     */
    public String getString() throws IllegalStateException {
        throw new IllegalStateException("Stream already retrieved");
    }

    /**
     * Always throws <code>IllegalStateException</code> because only an
     * <code>InputStream</code> is available from this implementation.
     *
     * @return nothing
     * @throws IllegalStateException as defined above.
     */
    public long getLong() throws IllegalStateException {
        throw new IllegalStateException("Stream already retrieved");
    }

    /**
     * Always throws <code>IllegalStateException</code> because only an
     * <code>InputStream</code> is available from this implementation.
     *
     * @return nothing
     * @throws IllegalStateException as defined above.
     */
    public double getDouble() throws IllegalStateException {
        throw new IllegalStateException("Stream already retrieved");
    }

    /**
     * Always throws <code>IllegalStateException</code> because only an
     * <code>InputStream</code> is available from this implementation.
     *
     * @return nothing
     * @throws IllegalStateException as defined above.
     */
    public Calendar getDate() throws IllegalStateException {
        throw new IllegalStateException("Stream already retrieved");
    }

    /**
     * Always throws <code>IllegalStateException</code> because only an
     * <code>InputStream</code> is available from this implementation.
     *
     * @return nothing
     * @throws IllegalStateException as defined above.
     */
    public boolean getBoolean() throws IllegalStateException {
        throw new IllegalStateException("Stream already retrieved");
    }

    /**
     * Writes the contents of the underlying stream to the
     * <code>ObjectOutputStream</code>.
     *
     * @param out The <code>ObjectOutputStream</code> to where the binary
     *      data is copied.
     * @throws IOException If an error occurs writing the binary data.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        byte[] buffer = new byte[4096];
        int bytes = 0;
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
     * Reads the binary data from the <code>ObjectInputStream</code> into
     * a temporary file that is used to back up the binary stream contents
     * of the constructed value instance. The temporary file gets deleted
     * when the binary stream is closed or garbage collected.
     *
     * @param in The <code>ObjectInputStream</code> from where to get the
     *      binary data.
     * @throws IOException If an error occurs reading the binary data.
     */
    private void readObject(ObjectInputStream in) throws IOException {
        final File file = File.createTempFile("jcr-value", "bin");

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
