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

import java.io.Serializable;
import java.util.Calendar;

import javax.jcr.PropertyType;
import javax.jcr.ValueFormatException;

/**
 * The <code>DoubleValue</code> class implements the committed value state for
 * Double values as a part of the State design pattern (Gof) used by this
 * package.
 *
 * @since 0.16.4.1
 * @see org.apache.jackrabbit.rmi.value.SerialValue
 */
public class DoubleValue extends BaseNonStreamValue
        implements Serializable, StatefulValue {

    /** The serial version UID */
    private static final long serialVersionUID = 1008752925622023274L;

    /** The double value */
    private final double value;

    /**
     * Creates an instance for the given double <code>value</code>.
     */
    protected DoubleValue(double value) {
        this.value = value;
    }

    /**
     * Creates an instance for the given string representation of a double.
     * <p>
     * This implementation uses the <code>Double.valueOf(String)</code> method
     * to convert the string to a double.
     *
     * @throws ValueFormatException if the string <code>value</code> cannot be
     *      parsed to double.
     */
    protected DoubleValue(String value) throws ValueFormatException {
        this(toDouble(value));
    }

    /**
     * Returns the double value represented by the string <code>value</code>.
     *
     * @throws ValueFormatException if the string <code>value</code> cannot be
     *      parsed to double.
     */
    protected static double toDouble(String value) throws ValueFormatException {
        try {
            return Double.valueOf(value).doubleValue();
        } catch (NumberFormatException e) {
            throw new ValueFormatException(e);
        }
    }

    /**
     * Returns <code>PropertyType.DOUBLE</code>.
     */
    public int getType() {
        return PropertyType.DOUBLE;
    }

    /**
     * Returns a <code>Calendar</code> instance interpreting the double as the
     * time in milliseconds since the epoch (1.1.1970, 0:00, UTC).
     */
    public Calendar getDate() throws ValueFormatException {
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis((long) value);
        return date;
    }

    /**
     * Returns the double value.
     */
    public double getDouble() {
        return value;
    }

    /**
     * Returns the double as a string converted by the
     * <code>Double.toString(double)</code>.
     */
    public String getString() {
        return Double.toString(value);
    }

    /**
     * Returns the value converted to a long.
     */
    public long getLong() {
        return (long) value;
    }
}
