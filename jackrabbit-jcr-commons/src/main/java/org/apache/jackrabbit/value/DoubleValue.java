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
 * A <code>DoubleValue</code> provides an implementation
 * of the <code>Value</code> interface representing a double value.
 */
public class DoubleValue extends BaseValue {

    public static final int TYPE = PropertyType.DOUBLE;

    private final Double dblNumber;

    /**
     * Constructs a <code>DoubleValue</code> object representing a double.
     *
     * @param dblNumber the double this <code>DoubleValue</code> should represent
     */
    public DoubleValue(Double dblNumber) {
        super(TYPE);
        this.dblNumber = dblNumber;
    }

    /**
     * Constructs a <code>DoubleValue</code> object representing a double.
     *
     * @param dbl the double this <code>DoubleValue</code> should represent
     */
    public DoubleValue(double dbl) {
        super(TYPE);
        this.dblNumber = dbl;
    }

    /**
     * Returns a new <code>DoubleValue</code> initialized to the value
     * represented by the specified <code>String</code>.
     *
     * @param s the string to be parsed.
     * @return a newly constructed <code>DoubleValue</code> representing the
     *         the specified value.
     * @throws javax.jcr.ValueFormatException If the <code>String</code> does not
     *                                        contain a parsable <code>double</code>.
     */
    public static DoubleValue valueOf(String s) throws ValueFormatException {
        try {
            return new DoubleValue(Double.parseDouble(s));
        } catch (NumberFormatException e) {
            throw new ValueFormatException("not a valid double format: " + s, e);
        }
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * <p>
     * The result is <code>true</code> if and only if the argument is not
     * <code>null</code> and is a <code>DoubleValue</code> object that
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
        if (obj instanceof DoubleValue) {
            DoubleValue other = (DoubleValue) obj;
            if (dblNumber == other.dblNumber) {
                return true;
            } else if (dblNumber != null && other.dblNumber != null) {
                return dblNumber.equals(other.dblNumber);
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
        if (dblNumber != null) {
            return dblNumber.toString();
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
        if (dblNumber != null) {
            // loosing timezone information...
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date(dblNumber.longValue()));
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
        if (dblNumber != null) {
            return dblNumber.longValue();
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
        if (dblNumber != null) {
            return dblNumber;
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
        if (dblNumber != null) {
            return new BigDecimal(dblNumber);
        } else {
            throw new ValueFormatException("empty value");
        }
    }
}
