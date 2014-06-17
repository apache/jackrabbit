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
import java.util.Date;
import java.math.BigDecimal;

/**
 * A <code>LongValue</code> provides an implementation
 * of the <code>Value</code> interface representing a long value.
 */
public class LongValue extends BaseValue {

    public static final int TYPE = PropertyType.LONG;

    private final Long lNumber;

    /**
     * Constructs a <code>LongValue</code> object representing a long.
     *
     * @param lNumber the long this <code>LongValue</code> should represent
     */
    public LongValue(Long lNumber) {
        super(TYPE);
        this.lNumber = lNumber;
    }

    /**
     * Constructs a <code>LongValue</code> object representing a long.
     *
     * @param l the long this <code>LongValue</code> should represent
     */
    public LongValue(long l) {
        super(TYPE);
        this.lNumber = l;
    }

    /**
     * Returns a new <code>LongValue</code> initialized to the value
     * represented by the specified <code>String</code>.
     *
     * @param s the string to be parsed.
     * @return a newly constructed <code>LongValue</code> representing the
     *         the specified value.
     * @throws javax.jcr.ValueFormatException If the <code>String</code> does not
     *                                        contain a parsable <code>long</code>.
     */
    public static LongValue valueOf(String s) throws ValueFormatException {
        try {
            return new LongValue(Long.parseLong(s));
        } catch (NumberFormatException e) {
            throw new ValueFormatException("not a valid long format: " + s, e);
        }
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * <p>
     * The result is <code>true</code> if and only if the argument is not
     * <code>null</code> and is a <code>LongValue</code> object that
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
        if (obj instanceof LongValue) {
            LongValue other = (LongValue) obj;
            if (lNumber == other.lNumber) {
                return true;
            } else if (lNumber != null && other.lNumber != null) {
                return lNumber.equals(other.lNumber);
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
        if (lNumber != null) {
            return lNumber.toString();
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
        if (lNumber != null) {
            // loosing timezone information...
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date(lNumber));
            return cal;
        } else {
            throw new ValueFormatException("empty value");
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getLong()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        if (lNumber != null) {
            return lNumber;
        } else {
            throw new ValueFormatException("empty value");
        }
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
        if (lNumber != null) {
            return lNumber.doubleValue();
        } else {
            throw new ValueFormatException("empty value");
        }
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal getDecimal()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        if (lNumber != null) {
            return new BigDecimal(lNumber);
        } else {
            throw new ValueFormatException("empty value");
        }
    }
}
