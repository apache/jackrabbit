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
package org.apache.jackrabbit.value;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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
 * 
 * @version $Revision$, $Date$
 * @author Jukka Zitting
 * @since 0.16.4.1
 * 
 * @see org.apache.jackrabbit.value.SerialValue
 */
public class BinaryValue implements StatefullValue {

    /** The <code>InputStream</code> providing the value */
    private final InputStream stream;

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
     */
    public InputStream getStream() {
        return stream;
    }

    /**
     * Returns <code>PropertyType.BINARY</code>.
     */
    public int getType() {
        return PropertyType.BINARY;
    }

    /**
     * Always throws <code>IllegalStateException</code> because only an
     * <code>InputStream</code> is available from this implementation.
     * 
     * @throws IllegalStateException as defined above. 
     */
    public String getString() {
        throw new IllegalStateException("Stream already retrieved");
    }

    /**
     * Always throws <code>IllegalStateException</code> because only an
     * <code>InputStream</code> is available from this implementation.
     * 
     * @throws IllegalStateException as defined above. 
     */
    public long getLong() {
        throw new IllegalStateException("Stream already retrieved");
    }

    /**
     * Always throws <code>IllegalStateException</code> because only an
     * <code>InputStream</code> is available from this implementation.
     * 
     * @throws IllegalStateException as defined above. 
     */
    public double getDouble() {
        throw new IllegalStateException("Stream already retrieved");
    }

    /**
     * Always throws <code>IllegalStateException</code> because only an
     * <code>InputStream</code> is available from this implementation.
     * 
     * @throws IllegalStateException as defined above. 
     */
    public Calendar getDate() {
        throw new IllegalStateException("Stream already retrieved");
    }

    /**
     * Always throws <code>IllegalStateException</code> because only an
     * <code>InputStream</code> is available from this implementation.
     * 
     * @throws IllegalStateException as defined above. 
     */
    public boolean getBoolean() {
        throw new IllegalStateException("Stream already retrieved");
    }
}