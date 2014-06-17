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

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import java.util.Calendar;
import java.math.BigDecimal;

/**
 * A <code>DateValue</code> provides an implementation
 * of the <code>Value</code> interface representing a date value.
 */
public class DateValue extends BaseValue {

    public static final int TYPE = PropertyType.DATE;

    private final Calendar date;

    /**
     * Constructs a <code>DateValue</code> object representing a date.
     *
     * @param date the date this <code>DateValue</code> should represent
     * @throws IllegalArgumentException if the given date cannot be represented
     * as defined by ISO 8601.
     */
    public DateValue(Calendar date) throws IllegalArgumentException {
        super(TYPE);
        this.date = date;
        ISO8601.getYear(date);
    }

    /**
     * Returns a new <code>DateValue</code> initialized to the value
     * represented by the specified <code>String</code>.
     * <p>
     * The specified <code>String</code> must be a ISO8601-compliant date/time
     * string.
     *
     * @param s the string to be parsed.
     * @return a newly constructed <code>DateValue</code> representing the
     *         the specified value.
     * @throws javax.jcr.ValueFormatException If the <code>String</code> is not a valid
     *                              ISO8601-compliant date/time string.
     * @see ISO8601
     */
    public static DateValue valueOf(String s) throws ValueFormatException {
        Calendar cal = ISO8601.parse(s);
        if (cal != null) {
            return new DateValue(cal);
        } else {
            throw new ValueFormatException("not a valid date format: " + s);
        }
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * <p>
     * The result is <code>true</code> if and only if the argument is not
     * <code>null</code> and is a <code>DateValue</code> object that
     * represents the same value as this object.
     * <p>
     * The value comparison is performed using the ISO 8601 string
     * representation of the dates, since the native Calendar.equals()
     * method may produce false negatives (see JSR-598).
     * <p>
     * Note that the comparison still returns false when comparing the
     * same time in different time zones, but that seems to be the intent
     * of JSR 170. Compare the Value.getDate().getTime() values if you need
     * an exact time comparison in UTC.
     *
     * @param obj the reference object with which to compare.
     * @return <code>true</code> if this object is the same as the obj
     *         argument; <code>false</code> otherwise.
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DateValue) {
            DateValue other = (DateValue) obj;
            if (date == other.date) {
                return true;
            } else if (date != null && other.date != null) {
                return ISO8601.format(date).equals(ISO8601.format(other.date));
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
        if (date != null) {
            return ISO8601.format(date);
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
        if (date != null) {
            return (Calendar) date.clone();
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
        if (date != null) {
            return date.getTimeInMillis();
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
        if (date != null) {
            throw new ValueFormatException("cannot convert date to boolean");
        } else {
            throw new ValueFormatException("empty value");
        }
    }

    /**
     * {@inheritDoc}
     */
    public double getDouble()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        if (date != null) {
            long ms = date.getTimeInMillis();
            if (ms <= Double.MAX_VALUE) {
                return ms;
            }
            throw new ValueFormatException("conversion from date to double failed: inconvertible types");
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
        if (date != null) {
            return new BigDecimal(date.getTimeInMillis());
        } else {
            throw new ValueFormatException("empty value");
        }
    }
}
