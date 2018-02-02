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
package org.apache.jackrabbit.spi.commons.value;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;

import javax.jcr.Binary;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.QValue;

/**
 * A <code>QValueValue</code> provides an implementation
 * of the <code>Value</code> interface representing an SPI
 * <code>QValue</code>.
 */
public final class QValueValue implements Value {

    // wrapped QValue
    private final QValue qvalue;

    // used for keeping track of input streams that have already been passed back
    private InputStream stream = null;

    // for converting the internal NAME/PATH format to JCR format
    private final NamePathResolver resolver;

    /**
     * Constructs a <code>QValueValue</code> object representing an SPI
     * <code>QValue</code>.
     *
     * @param qvalue the QValue this <code>QValueValue</code> should represent
     * @param resolver fore resolving namespace URIs to prefixes in NAME/PATH properties
     */
    public QValueValue(QValue qvalue, NamePathResolver resolver) {
        this.qvalue = qvalue;
        this.resolver = resolver;
    }

    /**
     * Returns the embedded <code>QValue</code>.
     *
     * @return the embedded <code>QValue</code>
     */
    public QValue getQValue() {
        return qvalue;
    }

    //--------------------------------------------------------------< Value >---
    /**
     * @see javax.jcr.Value#getBoolean()
     */
    public boolean getBoolean() throws RepositoryException {
        if (getType() == PropertyType.STRING || getType() == PropertyType.BINARY || getType() == PropertyType.BOOLEAN) {
            return Boolean.valueOf(qvalue.getString());
        } else {
            throw new ValueFormatException("incompatible type " + PropertyType.nameFromValue(qvalue.getType()));
        }
    }

    /**
     * @see javax.jcr.Value#getDecimal()
     */
    public BigDecimal getDecimal() throws ValueFormatException, IllegalStateException, RepositoryException {
        switch (getType()) {
            case PropertyType.DECIMAL:
            case PropertyType.DOUBLE:
            case PropertyType.LONG:
            case PropertyType.DATE:
            case PropertyType.STRING:
                return qvalue.getDecimal();
            default:
                throw new ValueFormatException("incompatible type " + PropertyType.nameFromValue(qvalue.getType()));
        }
    }

    /**
     * @see javax.jcr.Value#getBinary()
     */
    public Binary getBinary() throws RepositoryException {
        // JCR-2511 Value#getBinary() and #getStream() return internal representation for type PATH and NAME
        if (getType() == PropertyType.NAME || getType() == PropertyType.PATH) {
            // qualified name/path value needs to be resolved,
            // delegate conversion to getString() method
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
        } else {
            return qvalue.getBinary();
        }
    }

    /**
     * @see javax.jcr.Value#getDate()
     */
    public Calendar getDate() throws RepositoryException {
        return qvalue.getCalendar();
    }

    /**
     * @see javax.jcr.Value#getDouble()
     */
    public double getDouble() throws RepositoryException {
        return qvalue.getDouble();
    }

    /**
     * @see javax.jcr.Value#getLong()
     */
    public long getLong() throws RepositoryException {
        return qvalue.getLong();
    }

    /**
     * @see javax.jcr.Value#getStream()
     */
    public InputStream getStream() throws IllegalStateException, RepositoryException {
        if (stream == null) {
            if (getType() == PropertyType.NAME || getType() == PropertyType.PATH) {
                // qualified name/path value needs to be resolved
                stream = new ByteArrayInputStream(getString().getBytes(StandardCharsets.UTF_8));
            } else {
                stream = qvalue.getStream();
            }
        }
        return stream;
    }

    /**
     * @see javax.jcr.Value#getString()
     */
    public String getString() throws RepositoryException {
        if (getType() == PropertyType.NAME) {
            // qualified name value needs to be resolved
            return resolver.getJCRName(qvalue.getName());
        } else if (getType() == PropertyType.PATH) {
            // qualified path value needs to be resolved
            return resolver.getJCRPath(qvalue.getPath());
        } else {
            return qvalue.getString();
        }
    }

    /**
     * @see javax.jcr.Value#getType()
     */
    public int getType() {
        return qvalue.getType();
    }

    //-------------------------------------------------------------< Object >---
    /**
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof QValueValue) {
            return qvalue.equals(((QValueValue) obj).qvalue);
        } else {
            return false;
        }
    }

    /**
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        return qvalue.hashCode();
    }
}
