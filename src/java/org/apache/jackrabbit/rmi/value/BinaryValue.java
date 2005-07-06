/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
     * <code>ObjectOutputStream</code> by first copying to an internal byte
     * array.
     *
     * @param out The <code>ObjectOutputStream</code> to where the binary
     *      data is copied.
     *
     * @throws IOException If an error occurrs writing the binary data.
     * @throws OutOfMemoryError If not enough memory is available to store the
     *      binary data in the internal byte array.
     */
    private void writeObject(ObjectOutputStream out)
            throws IOException, OutOfMemoryError {
        // read the input into a byte array - limited by memory available !!
        ByteArrayOutputStream bos =
            new ByteArrayOutputStream(stream.available());
        byte[] buf = new byte[2048];
        int rd = 0;
        while ((rd = stream.read(buf)) >= 0) {
            bos.write(buf, 0, rd);
        }

        // stream the data to the object output
        out.writeInt(bos.size());
        out.write(bos.toByteArray());
    }

    /**
     * Reads the binary data from the <code>ObjectInputStream</code> into an
     * internal byte array, which is then provided through a
     * <code>ByteArrayInputStream</code>.
     *
     * @param in The <code>ObjectInputStream</code> from where to get the
     *      binary data.
     *
     * @throws IOException If an error occurrs reading the binary data.
     * @throws OutOfMemoryError If not enouhg memory is available to store the
     *      binary data in the internal byte array.
     */
    private void readObject(ObjectInputStream in)
            throws IOException, OutOfMemoryError {
        int size = in.readInt();
        byte[] buf = new byte[size];
        in.readFully(buf);
        stream = new ByteArrayInputStream(buf);
    }

}
