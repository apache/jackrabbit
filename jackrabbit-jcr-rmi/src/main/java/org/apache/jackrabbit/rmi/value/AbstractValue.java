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
import java.io.IOException;
import java.io.InputStream;
import java.io.FilterInputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;

import javax.jcr.Binary;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

/**
 * Abstract base class for {@link Value} implementations. This class
 * implements all {@link Value} methods except <code>getString</code> and
 * <code>getType</code>.
 * <p>
 * Most of the default value getters always throw {@link ValueFormatException}s
 * and expect type-specific subclasses to override that behaviour with the
 * appropriate value conversions.
 * <p>
 * The {@link #getBinary()} method is implemented based on the abstract
 * {@link #getString()} method, but subclasses can override that default
 * implementation.
 * <p>
 * The {@link #getStream()} method uses {@link #getBinary()} to implement
 * the deprecated JCR 1.0 behaviour. This method must not be overridden.
 */
abstract class AbstractValue implements Value, Serializable {

    /**
     * Serial version UID
     */
    private static final long serialVersionUID = -1989277354799918598L;

    /**
     * The stream instance returned by {@link #getStream()}. Note that
     * the stream is not included when serializing the value.
     */
    private transient InputStream stream = null;

    /**
     * Returns the stream representation of this value. This method implements
     * the deprecated JCR 1.0 behaviour of always returning the same stream
     * instance. The stream is retrieved from a {@link Binary} instance
     * returned by {@link #getBinary()}.
     *
     * @return stream representation of this value
     * @throws RepositoryException if the stream can not be created
     */
    public synchronized final InputStream getStream()
            throws RepositoryException {
        if (stream == null) {
            final Binary binary = getBinary();
            try {
                stream = new FilterInputStream(binary.getStream()) {
                    @Override
                    public void close() throws IOException {
                        super.close();
                        binary.dispose();
                    }
                };
            } finally {
                // Proper cleanup also when binary.getStream() fails
                if (stream == null) {
                    binary.dispose();
                }
            }
        }
        return stream;
    }

    /**
     * Returns the binary representation of this value. The default
     * implementation uses the UTF-8 serialization of the string returned
     * by {@link #getString()}. Subclasses 
     */
    public Binary getBinary() throws RepositoryException {
        final byte[] value = getString().getBytes(StandardCharsets.UTF_8);
        return new Binary() {
            public int read(byte[] b, long position) {
                if (position >= value.length) {
                    return -1;
                } else {
                    int p = (int) position;
                    int n = Math.min(b.length, value.length - p);
                    System.arraycopy(value, p, b, 0, n);
                    return n;
                }
            }
            public InputStream getStream() {
                return new ByteArrayInputStream(value);
            }
            public long getSize() {
                return value.length;
            }
            public void dispose() {
            }
        };
    }

    /**
     * Always throws a <code>ValueFormatException</code>. Implementations should
     * overwrite if conversion to boolean is supported.
     *
     * @return nothing
     * @throws ValueFormatException If the value cannot be converted to a
     *      boolean.
     */
    public boolean getBoolean() throws ValueFormatException {
        throw getValueFormatException(PropertyType.TYPENAME_BOOLEAN);
    }

    /**
     * Always throws a <code>ValueFormatException</code>. Implementations should
     * overwrite if conversion to <code>Calender</code> is supported.
     *
     * @return nothing
     * @throws ValueFormatException If the value cannot be converted to a
     *      <code>Calendar</code> instance.
     */
    public Calendar getDate() throws ValueFormatException {
        throw getValueFormatException(PropertyType.TYPENAME_DATE);
    }

    /**
     * Always throws a <code>ValueFormatException</code>. Implementations should
     * overwrite if conversion to a {@link BigDecimal} is supported.
     *
     * @return nothing
     * @throws ValueFormatException If the value cannot be converted to a
     *      {@link BigDecimal}.
     */
    public BigDecimal getDecimal() throws RepositoryException {
        throw getValueFormatException(PropertyType.TYPENAME_DECIMAL);
    }

    /**
     * Always throws a <code>ValueFormatException</code>. Implementations should
     * overwrite if conversion to double is supported.
     *
     * @return nothing
     * @throws ValueFormatException If the value cannot be converted to a
     *      double.
     */
    public double getDouble() throws ValueFormatException {
        throw getValueFormatException(PropertyType.TYPENAME_DOUBLE);
    }

    /**
     * Always throws a <code>ValueFormatException</code>. Implementations should
     * overwrite if conversion to long is supported.
     *
     * @return nothing
     * @throws ValueFormatException If the value cannot be converted to a
     *      long.
     */
    public long getLong() throws ValueFormatException {
        throw getValueFormatException(PropertyType.TYPENAME_LONG);
    }

    /**
     * Returns a <code>ValueFormatException</code> with a message indicating
     * what kind of type conversion is not supported.
     *
     * @return nothing
     * @param destType The name of the value type to which this value cannot
     *      be converted.
     */
    protected ValueFormatException getValueFormatException(String destType) {
        return new ValueFormatException(
                "Cannot convert value \"" + this + "\" of type "
                + PropertyType.nameFromValue(getType()) + " to " + destType);
    }

    //--------------------------------------------------------------< Object >

    /**
     * Compares values as defined in the JCR specification.
     *
     * @param object value for comparison
     * @return <code>true</code> if the values are equal,
     *         <code>false</code> otherwise
     * @see <a href="https://issues.apache.org/jira/browse/JCRRMI-16">JCRRMI-16</a>
     */
    public boolean equals(Object object) {
        try {
            return (object instanceof Value)
                && getType() == ((Value) object).getType()
                && getString().equals(((Value) object).getString());
        } catch (RepositoryException e) {
            return false;
        }
    }

    /**
     * Returns a hash code that's in line with how the {@link #equals(Object)}
     * method is implemented.
     *
     * @return hash code of this value
     */
    public int hashCode() {
        try {
            return getType() + getString().hashCode();
        } catch (RepositoryException e) {
            return getType();
        }
    }

    /**
     * Returns a string representation of this value.
     *
     * @return value string
     */
    public String toString() {
        try {
            return getString();
        } catch (RepositoryException e) {
            return PropertyType.nameFromValue(getType());
        }
    }

}
