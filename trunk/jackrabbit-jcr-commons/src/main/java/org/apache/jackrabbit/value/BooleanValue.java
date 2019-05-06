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
import java.math.BigDecimal;

/**
 * A <code>BooleanValue</code> provides an implementation
 * of the <code>Value</code> interface representing a boolean value.
 */
public class BooleanValue extends BaseValue {

    public static final int TYPE = PropertyType.BOOLEAN;

    private final Boolean bool;

    /**
     * Constructs a <code>BooleanValue</code> object representing a boolean.
     *
     * @param bool the boolean this <code>BooleanValue</code> should represent
     */
    public BooleanValue(Boolean bool) {
        super(TYPE);
        this.bool = bool;
    }

    /**
     * Constructs a <code>BooleanValue</code> object representing a boolean.
     *
     * @param bool the boolean this <code>BooleanValue</code> should represent
     */
    public BooleanValue(boolean bool) {
        super(TYPE);
        this.bool = bool;
    }

    /**
     * Returns a new <code>BooleanValue</code> initialized to the value
     * represented by the specified <code>String</code>.
     *
     * @param s the string to be parsed.
     * @return a newly constructed <code>BooleanValue</code> representing the
     *         the specified value.
     */
    public static BooleanValue valueOf(String s) {
        return new BooleanValue(Boolean.valueOf(s));
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * <p>
     * The result is <code>true</code> if and only if the argument is not
     * <code>null</code> and is a <code>BooleanValue</code> object that
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
        if (obj instanceof BooleanValue) {
            BooleanValue other = (BooleanValue) obj;
            if (bool == other.bool) {
                return true;
            } else if (bool != null && other.bool != null) {
                return bool.equals(other.bool);
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
        if (bool != null) {
            return bool.toString();
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
        if (bool != null) {
            return bool;
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
