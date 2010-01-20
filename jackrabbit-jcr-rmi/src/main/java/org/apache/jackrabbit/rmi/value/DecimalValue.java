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
 * Decimal value.
 */
class DecimalValue extends AbstractValue {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 2077767642124007133L;

    /**
     * The minimum value for date conversion.
     */
    private static final BigDecimal MIN_DATE =
        BigDecimal.valueOf(Long.MIN_VALUE);

    /**
     * The maximum value for date conversion.
     */
    private static final BigDecimal MAX_DATE =
        BigDecimal.valueOf(Long.MAX_VALUE);

    /**
     * The decimal value.
     */
    private final BigDecimal value;

    /**
     * Creates an instance for the given decimal value.
     */
    public DecimalValue(BigDecimal value) {
        this.value = value;
    }

    /**
     * Returns {@link PropertyType#DECIMAL}.
     */
    public int getType() {
        return PropertyType.DECIMAL;
    }

    /**
     * The decimal is converted to a long and interpreted as the number of
     * milliseconds since 00:00 (UTC) 1 January 1970 (1970-01-01T00:00:00.000Z).
     * If the resulting value is out of range for a date,
     * a {@link ValueFormatException} is thrown.
     */
    @Override
    public Calendar getDate() throws ValueFormatException {
        if (value.compareTo(MIN_DATE) >= 0 && value.compareTo(MAX_DATE) <= 0) {
            Calendar date = Calendar.getInstance();
            date.setTimeInMillis(getLong());
            return date;
        } else {
            throw new ValueFormatException(
                    "Decimal value is outside the date range: " + value);
        }
    }

    /**
     * Returns the decimal value.
     */
    @Override
    public BigDecimal getDecimal() {
        return value;
    }

    /**
     * The decimal is converted using {@link BigDecimal#doubleValue()}.
     */
    @Override
    public double getDouble() {
        return value.doubleValue();
    }

    /**
     * The decimal is converted using {@link BigDecimal#longValue()}.
     */
    @Override
    public long getLong() {
        return value.longValue();
    }

    /**
     * The decimal is converted using {@link BigDecimal#toString()}.
     */
    public String getString() {
        return value.toString();
    }

}
