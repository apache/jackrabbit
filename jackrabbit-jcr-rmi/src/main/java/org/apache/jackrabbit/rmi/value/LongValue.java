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

/**
 * Long value.
 */
class LongValue extends AbstractValue {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = -5983072186237752887L;

    /** The long value */
    private final long value;

    /**
     * Creates an instance for the given long <code>value</code>.
     */
    public LongValue(long value) {
        this.value = value;
    }

    /**
     * Returns <code>PropertyType.LONG</code>.
     */
    public int getType() {
        return PropertyType.LONG;
    }

    /**
     * The long is interpreted as the number of milliseconds since
     * 00:00 (UTC) 1 January 1970 (1970-01-01T00:00:00.000Z).
     */
    @Override
    public Calendar getDate() {
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(value);
        return date;
    }

    /**
     * The long is converted using the method {@link BigDecimal#valueOf(long)}.
     */
    @Override
    public BigDecimal getDecimal() {
        return BigDecimal.valueOf(value);
    }

    /**
     * Standard Java type coercion is used.
     */
    @Override
    public double getDouble() {
        return value;
    }

    /**
     * Returns the long value.
     */
    @Override
    public long getLong() {
        return value;
    }

    /**
     * The long is converted using {@link Long#toString(long)}.
     */
    public String getString() {
        return Long.toString(value);
    }

}
