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
 * The <code>StringValue</code> class implements the committed value state for
 * String values as a part of the State design pattern (Gof) used by this
 * package.
 *
 * @since 0.16.4.1
 * @see org.apache.jackrabbit.rmi.value.SerialValue
 */
public class StringValue extends BaseNonStreamValue
        implements Serializable, StatefulValue {

    /** The serial version UID */
    private static final long serialVersionUID = -6456025645604163205L;

    /** The string value */
    private final String value;

    /**
     * Creates an instance for the given string <code>value</code>.
     */
    protected StringValue(String value) {
        this.value = value;
    }

    /**
     * Returns <code>PropertyType.STRING</code>.
     */
    public int getType() {
        return PropertyType.STRING;
    }

    /**
     * Returns the string value.
     */
    public String getString() {
        return value;
    }

    /**
     * Returns the string value parsed to a long calling the
     * <code>Long.valueOf(String)</code> method.
     *
     * @throws ValueFormatException if the string cannot be parsed to long.
     */
    public long getLong() throws ValueFormatException {
        return LongValue.toLong(value);
    }

    /**
     * Returns the string value parsed to a double calling the
     * <code>Double.valueOf(String)</code> method.
     *
     * @throws ValueFormatException if the string cannot be parsed to double.
     */
    public double getDouble() throws ValueFormatException {
        return DoubleValue.toDouble(value);
    }

    /**
     * Returns the string value parsed to a <code>Calendar</code> using the
     * same formatter as the {@link DateValue} class. This formatting bears the
     * same issues as parsing and formatting that class.
     *
     * @throws ValueFormatException if the string cannot be parsed into a
     *      <code>Calendar</code> instance.
     */
    public Calendar getDate() throws ValueFormatException {
        return ISO8601.parse(value);
    }

    /**
     * Returns the string value parsed to a boolean calling the
     * <code>Boolean.valueOf(String)</code> method.
     */
    public boolean getBoolean() {
        return BooleanValue.toBoolean(value);
    }
}
