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

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import java.util.Calendar;
import java.net.URI;
import java.net.URISyntaxException;
import java.math.BigDecimal;

/**
 * A <code>URIValue</code> provides an implementation
 * of the <code>Value</code> interface representing a <code>URI</code> value
 * (an absolute or relative workspace path).
 */
public class URIValue extends BaseValue {

    public static final int TYPE = PropertyType.URI;

    private final URI uri;

    /**
     * Returns a new <code>URIValue</code> initialized to the value
     * represented by the specified <code>String</code>.
     * <p>
     * The specified <code>String</code> must be a valid URI.
     *
     * @param s the string to be parsed.
     * @return a newly constructed <code>URIValue</code> representing the
     *         the specified value.
     * @throws javax.jcr.ValueFormatException If the <code>String</code> is not a valid URI.
     */
    public static URIValue valueOf(String s) throws ValueFormatException {
        if (s != null) {
            try {
                return new URIValue(new URI(s));
            } catch (URISyntaxException e) {
                throw new ValueFormatException(e.getMessage());
            }
        } else {
            throw new ValueFormatException("not a valid uri format: " + s);
        }
    }

    /**
     * Returns a new <code>URIValue</code> initialized to the value of the
     * specified URI.
     *
     * @param uri the path this <code>URIValue</code> should represent
     * @see #valueOf
     */
    public URIValue(URI uri) {
        super(TYPE);
        this.uri = uri;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * <p>
     * The result is <code>true</code> if and only if the argument is not
     * <code>null</code> and is a <code>URIValue</code> object that
     * represents the same value as this object.
     *
     * @param obj the reference object with which to compare.
     * @return <code>true</code> if this object is the same as the obj
     *         argument; <code>false</code> otherwise.
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof URIValue) {
            URIValue other = (URIValue) obj;
            if (uri == other.uri) {
                return true;
            } else if (uri != null && other.uri != null) {
                return uri.equals(other.uri);
            }
        }
        return false;
    }

    /**
     * Returns zero to satisfy the Object equals/hashCode contract.
     * This class is mutable and not meant to be used as a hash key.
     *
     * @return always zero
     * @see Object#hashCode()
     */
    public int hashCode() {
        return 0;
    }

    //------------------------------------------------------------< BaseValue >
    /**
     * {@inheritDoc}
     */
    protected String getInternalString() throws ValueFormatException {
        if (uri != null) {
            return uri.toString();
        } else {
            throw new ValueFormatException("empty value");
        }
    }

    //----------------------------------------------------------------< Value >
    /**
     * {@inheritDoc}
     */
    public Calendar getDate()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        throw new ValueFormatException("conversion to date failed: inconvertible types");
    }

    /**
     * {@inheritDoc}
     */
    public long getLong()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        throw new ValueFormatException("conversion to long failed: inconvertible types");
    }

    /**
     * {@inheritDoc}
     */
    public boolean getBoolean()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        throw new ValueFormatException("conversion to boolean failed: inconvertible types");
    }

    /**
     * {@inheritDoc}
     */
    public double getDouble()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        throw new ValueFormatException("conversion to double failed: inconvertible types");
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal getDecimal()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        throw new ValueFormatException("conversion to Decimal failed: inconvertible types");
    }
}