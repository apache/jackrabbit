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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.jcr.PropertyType;
import javax.jcr.ValueFormatException;

/**
 * The <code>DateValue</code> class implements the committed value state for
 * Date values as a part of the State design pattern (Gof) used by this
 * package.
 * <p>
 * To convert <code>Calendar</code> instances to and from strings, this class
 * uses a <code>SimpleDateFormat</code> instance with the pattern
 * <code>yyyy-MM-dd'T'HH:mm:ss'Z'</code>. The issue with this pattern is that
 * the era specification as defined in the JCR specification (+/- prefix) as
 * well as full time zone naming are not supported.
 *
 * @since 0.16.4.1
 * @see org.apache.jackrabbit.rmi.value.SerialValue
 */
public class DateValue extends BaseNonStreamValue
        implements Serializable, StatefulValue {

    /** The serial version UID */
    private static final long serialVersionUID = 7158448867450810873L;

    /** The <code>Calendar</code> value */
    private final Calendar value;

    /**
     * This should probably actually be a reference to the ISO8601 utility
     * class.
     */
    private static final DateFormat DATE_FORMAT =
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    /**
     * Creates an instance for the given <code>Calendar</code> <code>value</code>.
     */
    protected DateValue(Calendar value) {
        this.value = value;
    }

   /**
    * Creates an instance for the given string representation of a
    * <code>Calendar</code>.
    * <p>
    * This implementation uses a <code>SimpleDateFormat</code> instance with
    * the pattern <code>yyyy-MM-dd'T'HH:mm:ss'Z'</code> to parse the string into
    * a <code>Calendar</code> object. See the class comment for issues regarding
    * this pattern.
    */
    protected DateValue(String value) throws ValueFormatException {
        this(toCalendar(value));
    }

    /**
     * Returns the string <code>value</code> parsed into a
     * <code>Calendar</code> instance.
     *
     * @param value The string value.
     * @return The <code>Calendar</code> instance parsed from the string
     *         value.
     * @throws ValueFormatException if the string value cannot be parsed into a
     *             <code>Calendar</code> instance.
     */
    protected static Calendar toCalendar(String value) throws ValueFormatException {
        synchronized (DATE_FORMAT) {
            try {
                Calendar time = Calendar.getInstance();
                time.setTime(DATE_FORMAT.parse(value));
                return time;
            } catch (ParseException pe) {
                throw new ValueFormatException(pe.getMessage());
            }
        }
    }

    /**
     * Returns <code>PropertyType.DATE</code>.
     */
    public int getType() {
        return PropertyType.DATE;
    }

    /**
     * Returns the time represented by this instance as the number of
     * milliseconds since the epoch (1.1.1970, 0:00, UTC).
     */
    public double getDouble() {
        return value.getTimeInMillis();
    }

    /**
     * Returns the string represented of this <code>Calendar</code> value
     * formatted using a <code>SimpleDateFormatter</code> with the pattern
     * <code>yyyy-MM-dd'T'HH:mm:ss'Z'</code>. See the class comment for issues
     * regarding this pattern.
     */
    public String getString() {
        synchronized (DATE_FORMAT) {
            return DATE_FORMAT.format(value.getTime());
        }
    }

    /**
     * Returns the time represented by this instance as the number of
     * milliseconds since the epoch (1.1.1970, 0:00, UTC).
     */
    public long getLong() {
        return value.getTimeInMillis();
    }

    /**
     * Returns (a copy) of this <code>Calendar</code> value. Modifying the
     * returned <code>Calendar</code> does not change the value of this
     * instance.
     */
    public Calendar getDate() {
        return (Calendar) value.clone();
    }
}
