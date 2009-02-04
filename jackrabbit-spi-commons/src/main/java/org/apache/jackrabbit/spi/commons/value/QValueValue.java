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
import java.util.Calendar;

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

    private static final short STATE_UNDEFINED = 0;

    private static final short STATE_VALUE_CONSUMED = 1;

    private static final short STATE_STREAM_CONSUMED = 2;

    // the state of this value instance
    private short state = STATE_UNDEFINED;

    // wrapped QValue
    private final QValue qvalue;

    // used for keeping track of input streams that have already been passed back
    private InputStream stream = null;

    // for converting the internal NAME/PATH format to JCR format
    private final NamePathResolver resolver;

    /**
     * Constructs a <code>QValueValue</code> object representing an SPI
     * <codeQValue</code>.
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
        setValueConsumed();
        if (getType() == PropertyType.STRING || getType() == PropertyType.BINARY || getType() == PropertyType.BOOLEAN) {
            return Boolean.valueOf(qvalue.getString()).booleanValue();
        } else {
            throw new ValueFormatException("incompatible type " + PropertyType.nameFromValue(qvalue.getType()));
        }
    }

    /**
     * @see javax.jcr.Value#getDate()
     */
    public Calendar getDate() throws RepositoryException {
        setValueConsumed();
        return qvalue.getCalendar();
    }

    /**
     * @see javax.jcr.Value#getDouble()
     */
    public double getDouble() throws RepositoryException {
        setValueConsumed();
        return qvalue.getDouble();
    }

    /**
     * @see javax.jcr.Value#getLong()
     */
    public long getLong() throws RepositoryException {
        setValueConsumed();
        return qvalue.getLong();
    }

    /**
     * @see javax.jcr.Value#getStream()
     */
    public InputStream getStream() throws IllegalStateException, RepositoryException {
        setStreamConsumed();
        if (stream == null) {
            if (getType() == PropertyType.NAME || getType() == PropertyType.PATH) {
                // needs namespace mapping
                try {
                    String l_s = getType() == PropertyType.NAME
                      ? resolver.getJCRName(qvalue.getName())
                      : resolver.getJCRPath(qvalue.getPath());
                    stream = new ByteArrayInputStream(l_s.getBytes("UTF-8"));
                } catch (UnsupportedEncodingException ex) {
                    throw new RepositoryException(ex);
                }
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
        setValueConsumed();
        if (getType() == PropertyType.NAME) {
            // needs formatting
            return resolver.getJCRName(qvalue.getName());
        } else if (getType() == PropertyType.PATH) {
            // needs formatting
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
    public boolean equals(Object obj) {
        if (obj instanceof QValueValue) {
            return qvalue.equals(((QValueValue) obj).qvalue);
        }
        else {
            return false;
        }
    }

    /**
     * @see Object#hashCode() 
     */
    public int hashCode() {
        return qvalue.hashCode();
    }

    //--------------------------------------------------------------------------
    /**
     * Checks if the non-stream value of this instance has already been
     * consumed (if any getter methods except <code>{@link #getStream()}</code> and
     * <code>{@link #getType()}</code> have been previously called at least once) and
     * sets the state to <code>STATE_STREAM_CONSUMED</code>.
     *
     * @throws IllegalStateException if any getter methods other than
     *                               <code>getStream()</code> and
     *                               <code>getType()</code> have been
     *                               previously called at least once.
     */
    private void setStreamConsumed() throws IllegalStateException {
        if (state == STATE_VALUE_CONSUMED) {
            throw new IllegalStateException("non-stream value has already been consumed");
        }
        state = STATE_STREAM_CONSUMED;
    }

    /**
     * Checks if the stream value of this instance has already been
     * consumed (if {@link #getStream()} has been previously called
     * at least once) and sets the state to <code>STATE_VALUE_CONSUMED</code>.
     *
     * @throws IllegalStateException if <code>getStream()</code> has been
     *                               previously called at least once.
     */
    private void setValueConsumed() throws IllegalStateException {
        if (state == STATE_STREAM_CONSUMED) {
            throw new IllegalStateException("stream value has already been consumed");
        }
        state = STATE_VALUE_CONSUMED;
    }
}
