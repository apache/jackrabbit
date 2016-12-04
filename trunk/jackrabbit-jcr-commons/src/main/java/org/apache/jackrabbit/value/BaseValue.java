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
package org.apache.jackrabbit.value;

import org.apache.jackrabbit.util.ISO8601;

import javax.jcr.Binary;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Calendar;

/**
 * This class is the superclass of the type-specific
 * classes implementing the <code>Value</code> interfaces.
 *
 * @see javax.jcr.Value
 * @see StringValue
 * @see LongValue
 * @see DoubleValue
 * @see DecimalValue
 * @see BooleanValue
 * @see DateValue
 * @see BinaryValue
 * @see NameValue
 * @see PathValue
 * @see URIValue
 * @see ReferenceValue
 * @see WeakReferenceValue
 */
public abstract class BaseValue implements Value {

    protected static final String DEFAULT_ENCODING = "UTF-8";

    protected final int type;

    protected InputStream stream = null;

    /**
     * Package-private default constructor.
     *
     * @param type The type of this value.
     */
    BaseValue(int type) {
        this.type = type;
    }

    /**
     * Returns the internal string representation of this value without modifying
     * the value state.
     *
     * @return the internal string representation
     * @throws javax.jcr.ValueFormatException if the value can not be represented as a
     *                              <code>String</code> or if the value is
     *                              <code>null</code>.
     * @throws javax.jcr.RepositoryException  if another error occurs.
     */
    protected abstract String getInternalString()
            throws ValueFormatException, RepositoryException;

    //----------------------------------------------------------------< Value >
    /**
     * {@inheritDoc}
     */
    public int getType() {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    public Calendar getDate()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        Calendar cal = ISO8601.parse(getInternalString());
        if (cal == null) {
            throw new ValueFormatException("not a valid date format");
        } else {
            return cal;
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getLong()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        try {
            return Long.parseLong(getInternalString());
        } catch (NumberFormatException e) {
            throw new ValueFormatException("conversion to long failed", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean getBoolean()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        return Boolean.valueOf(getInternalString());
    }

    /**
     * {@inheritDoc}
     */
    public double getDouble()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        try {
            return Double.parseDouble(getInternalString());
        } catch (NumberFormatException e) {
            throw new ValueFormatException("conversion to double failed", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal getDecimal()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        try {
            return new BigDecimal(getInternalString());
        } catch (NumberFormatException e) {
            throw new ValueFormatException("conversion to Decimal failed", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public InputStream getStream()
            throws IllegalStateException, RepositoryException {
        if (stream != null) {
            return stream;
        }

        try {
            // convert via string
            stream = new ByteArrayInputStream(getInternalString().getBytes(DEFAULT_ENCODING));
            return stream;
        } catch (UnsupportedEncodingException e) {
            throw new RepositoryException(DEFAULT_ENCODING
                    + " not supported on this platform", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Binary getBinary()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        try {
            // convert via string
            return new BinaryImpl(new ByteArrayInputStream(getInternalString().getBytes(DEFAULT_ENCODING)));
        } catch (UnsupportedEncodingException e) {
            throw new RepositoryException(DEFAULT_ENCODING
                    + " not supported on this platform", e);
        } catch (IOException e) {
            throw new RepositoryException("failed to create Binary instance", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getString()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        return getInternalString();
    }
}
