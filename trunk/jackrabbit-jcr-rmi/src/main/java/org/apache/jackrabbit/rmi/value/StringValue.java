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
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.jcr.PropertyType;
import javax.jcr.ValueFormatException;

/**
 * String value.
 */
class StringValue extends AbstractValue {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 220963478492833703L;

    /** The string value */
    private final String value;

    /**
     * Creates an instance for the given string <code>value</code>.
     */
    public StringValue(String value) {
        this.value = value;
    }

    /**
     * Returns {@link PropertyType#STRING}.
     */
    public int getType() {
        return PropertyType.STRING;
    }

    /**
     * The string is converted using {@link Boolean#valueOf(String)}.
     */
    @Override
    public boolean getBoolean() {
        return Boolean.valueOf(value);
    }

    /**
     * If the string is in the format described in
     * {@link DateValue#getString()}, it is converted directly, otherwise
     * a {@link ValueFormatException} is thrown.
     */
    @Override
    public Calendar getDate() throws ValueFormatException {
        // check optional leading sign
        char sign = '+';
        int start = 0;
        if (value.startsWith("-")) {
            sign = '-';
            start = 1;
        } else if (value.startsWith("+")) {
            sign = '+';
            start = 1;
        }

        // note that we cannot use java.text.SimpleDateFormat for
        // parsing because it can't handle years <= 0 and TZD's
        int year, month, day, hour, min, sec, ms;
        String tzID;
        try {
            // year (YYYY)
            year = Integer.parseInt(value.substring(start, start + 4));
            start += 4;
            // delimiter '-'
            if (value.charAt(start) != '-') {
                throw new ValueFormatException("Not a date: " + value);
            }
            start++;
            // month (MM)
            month = Integer.parseInt(value.substring(start, start + 2));
            start += 2;
            // delimiter '-'
            if (value.charAt(start) != '-') {
                throw new ValueFormatException("Not a date: " + value);
            }
            start++;
            // day (DD)
            day = Integer.parseInt(value.substring(start, start + 2));
            start += 2;
            // delimiter 'T'
            if (value.charAt(start) != 'T') {
                throw new ValueFormatException("Not a date: " + value);
            }
            start++;
            // hour (hh)
            hour = Integer.parseInt(value.substring(start, start + 2));
            start += 2;
            // delimiter ':'
            if (value.charAt(start) != ':') {
                throw new ValueFormatException("Not a date: " + value);
            }
            start++;
            // minute (mm)
            min = Integer.parseInt(value.substring(start, start + 2));
            start += 2;
            // delimiter ':'
            if (value.charAt(start) != ':') {
                throw new ValueFormatException("Not a date: " + value);
            }
            start++;
            // second (ss)
            sec = Integer.parseInt(value.substring(start, start + 2));
            start += 2;
            // delimiter '.'
            if (value.charAt(start) != '.') {
                throw new ValueFormatException("Not a date: " + value);
            }
            start++;
            // millisecond (SSS)
            ms = Integer.parseInt(value.substring(start, start + 3));
            start += 3;
            // time zone designator (Z or +00:00 or -00:00)
            if (value.charAt(start) == '+' || value.charAt(start) == '-') {
                // offset to UTC specified in the format +00:00/-00:00
                tzID = "GMT" + value.substring(start);
            } else if (value.substring(start).equals("Z")) {
                tzID = "GMT";
            } else {
                throw new ValueFormatException(
                        "Invalid time zone in a date: " + value);
            }
        } catch (IndexOutOfBoundsException e) {
            throw new ValueFormatException("Not a date: " + value, e);
        } catch (NumberFormatException e) {
            throw new ValueFormatException("Not a date: " + value, e);
        }

        TimeZone tz = TimeZone.getTimeZone(tzID);
        // verify id of returned time zone (getTimeZone defaults to "GMT")
        if (!tz.getID().equals(tzID)) {
            throw new ValueFormatException(
                    "Invalid time zone in a date: " + value);
        }

        // initialize Calendar object
        Calendar cal = Calendar.getInstance(tz);
        cal.setLenient(false);
        // year and era
        if (sign == '-' || year == 0) {
            // not CE, need to set era (BCE) and adjust year
            cal.set(Calendar.YEAR, year + 1);
            cal.set(Calendar.ERA, GregorianCalendar.BC);
        } else {
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.ERA, GregorianCalendar.AD);
        }
        // month (0-based!)
        cal.set(Calendar.MONTH, month - 1);
        // day of month
        cal.set(Calendar.DAY_OF_MONTH, day);
        // hour
        cal.set(Calendar.HOUR_OF_DAY, hour);
        // minute
        cal.set(Calendar.MINUTE, min);
        // second
        cal.set(Calendar.SECOND, sec);
        // millisecond
        cal.set(Calendar.MILLISECOND, ms);

        try {
            // the following call will trigger an IllegalArgumentException
            // if any of the set values are illegal or out of range
            cal.getTime();
        } catch (IllegalArgumentException e) {
            throw new ValueFormatException("Not a date: " + value, e);
        }

        return cal;
    }

    /**
     * The string is converted using the constructor
     * {@link BigDecimal#BigDecimal(String)}.
     */
    @Override
    public BigDecimal getDecimal() throws ValueFormatException {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new ValueFormatException("Not a decimal value: " + value, e);
        }
    }


    /**
     * The string is converted using {@link Double#valueOf(String)}.
     */
    @Override
    public double getDouble() throws ValueFormatException {
        try {
            return Double.valueOf(value);
        } catch (NumberFormatException e) {
            throw new ValueFormatException("Not a double value: " + value, e);
        }
    }

    /**
     * The string is converted using {@link Long#valueOf(String)}.
     */
    @Override
    public long getLong() throws ValueFormatException {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            throw new ValueFormatException("Not a long value: " + value, e);
        }
    }

    /**
     * Returns the string value.
     */
    public String getString() {
        return value;
    }

}
