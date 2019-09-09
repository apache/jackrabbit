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
package org.apache.jackrabbit.core.query.lucene;

import java.util.Date;

/**
 * Implements <code>Date</code> &lt;-&gt; <code>String</code> conversions in
 * a way that the resulting <code>String</code> is suitable for indexing and
 * sorting.
 */
public class DateField {

    private DateField() {
    }

    /**
     * Date string length for about 3000 years
     */
    private static final int DATE_LEN = Long.toString(1000L * 365 * 24 * 60 * 60 * 3000,
            Character.MAX_RADIX).length();

    /**
     * Date shift of 2'000 years this allows dates back to 30 BC
     */
    private static final long DATE_SHIFT = 1000L * 365 * 24 * 60 * 60 * 2000;


    /**
     * Returns '000000000' -&gt; something around 30 BC
     */
    public static final String MIN_DATE_STRING = timeToString(-DATE_SHIFT);

    /**
     * Returns 'zzzzzzzzz' -&gt; something around 3189
     */
    public static final String MAX_DATE_STRING;

    /**
     * Initializes the constant {@link #MAX_DATE_STRING}.
     */
    static {
        char[] buffer = new char[DATE_LEN];
        char c = Character.forDigit(Character.MAX_RADIX - 1, Character.MAX_RADIX);
        for (int i = 0; i < DATE_LEN; i++) {
            buffer[i] = c;
        }
        MAX_DATE_STRING = new String(buffer);
    }

    /**
     * Converts a Date to a string suitable for indexing. This method will throw
     * a RuntimeException if the date specified in the method argument is before
     * 30 BC or after 3189.
     */
    public static String dateToString(Date date) {
        return timeToString(date.getTime());
    }

    /**
     * Converts a millisecond time to a string suitable for indexing.
     * Supported date range is: 30 BC - 3189
     * @throws IllegalArgumentException if the given <code>time</code> is not
     *                                  within the supported date range.
     */
    public static String timeToString(long time) {

        time += DATE_SHIFT;


        if (time < 0) {
            throw new IllegalArgumentException("time too early");
        }

        String s = Long.toString(time, Character.MAX_RADIX);

        if (s.length() > DATE_LEN) {
            throw new IllegalArgumentException("time too late");
        }

        // Pad with leading zeros
        if (s.length() < DATE_LEN) {
            StringBuffer sb = new StringBuffer(s);
            while (sb.length() < DATE_LEN) {
                sb.insert(0, 0);
            }
            s = sb.toString();
        }

        return s;
    }


    /**
     * Converts a string-encoded date into a millisecond time.
     */
    public static long stringToTime(String s) {
        return Long.parseLong(s, Character.MAX_RADIX) - DATE_SHIFT;
    }

    /**
     * Converts a string-encoded date into a Date object.
     */
    public static Date stringToDate(String s) {
        return new Date(stringToTime(s));
    }
}
