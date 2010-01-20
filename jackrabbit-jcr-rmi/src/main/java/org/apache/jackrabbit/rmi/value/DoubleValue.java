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

import java.math.BigDecimal;
import java.util.Calendar;

import javax.jcr.PropertyType;
import javax.jcr.ValueFormatException;

/**
 * Double value.
 */
class DoubleValue extends AbstractValue {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = -2767063038068929611L;

    /**
     * The double value.
     */
    private final double value;

    /**
     * Creates an instance for the given double value.
     */
    public DoubleValue(double value) {
        this.value = value;
    }

    /**
     * Returns {@link PropertyType#DOUBLE}.
     */
    public int getType() {
        return PropertyType.DOUBLE;
    }

    /**
     * Returns a <code>Calendar</code> instance interpreting the double as the
     * time in milliseconds since the epoch (1.1.1970, 0:00, UTC). If the
     * resulting value is out of range for a date,
     * a {@link ValueFormatException} is thrown.
     */
    @Override
    public Calendar getDate() throws ValueFormatException {
        if (Long.MIN_VALUE <= value && value <= Long.MAX_VALUE) {
            Calendar date = Calendar.getInstance();
            date.setTimeInMillis((long) value);
            return date;
        } else {
            throw new ValueFormatException(
                    "Double value is outside the date range: " + value);
        }
    }

    /**
     * The double is converted using the constructor
     * {@link BigDecimal#BigDecimal(double)}.
     */
    @Override
    public BigDecimal getDecimal() {
        return new BigDecimal(value);
    }

    /**
     * Returns the double value.
     */
    @Override
    public double getDouble() {
        return value;
    }

    /**
     * Standard Java type coercion is used.
     */
    @Override
    public long getLong() {
        return (long) value;
    }

    /**
     * The double is converted using {@link Double#toString(double)}.
     */
    public String getString() {
        return Double.toString(value);
    }

}
