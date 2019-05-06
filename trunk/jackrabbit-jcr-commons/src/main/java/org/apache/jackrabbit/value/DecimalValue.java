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
 * A <code>DecimalValue</code> provides an implementation
 * of the <code>Value</code> interface representing a <code>DECIMAL</code> value.
 */
public class DecimalValue extends BaseValue {

    public static final int TYPE = PropertyType.DECIMAL;

    private final BigDecimal number;

    /**
     * Constructs a <code>DecimalValue</code> object representing a decimal.
     *
     * @param number the decimal this <code>DecimalValue</code> should represent
     */
    public DecimalValue(BigDecimal number) {
        super(TYPE);
        this.number = number;
    }

    /**
     * Returns a new <code>DecimalValue</code> initialized to the value
     * represented by the specified <code>String</code>.
     *
     * @param s the string to be parsed.
     * @return a newly constructed <code>DecimalValue</code> representing the
     *         the specified value.
     * @throws javax.jcr.ValueFormatException If the <code>String</code> does not
     *                                        contain a parsable <code>decimal</code>.
     */
    public static DecimalValue valueOf(String s) throws ValueFormatException {
        try {
            return new DecimalValue(new BigDecimal(s));
        } catch (NumberFormatException e) {
            throw new ValueFormatException("not a valid decimal format: " + s, e);
        }
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * <p>
     * The result is <code>true</code> if and only if the argument is not
     * <code>null</code> and is a <code>DecimalValue</code> object that
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
        if (obj instanceof DecimalValue) {
            DecimalValue other = (DecimalValue) obj;
            if (number == other.number) {
                return true;
            } else if (number != null && other.number != null) {
                return number.compareTo(other.number) == 0;
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
        if (number != null) {
            return number.toString();
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
        if (number != null) {
            // loosing timezone information...
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date(number.longValue()));
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
        if (number != null) {
            return number.longValue();
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
        if (number != null) {
            return number.doubleValue();
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
        if (number != null) {
            return number;
        } else {
            throw new ValueFormatException("empty value");
        }
    }
}
