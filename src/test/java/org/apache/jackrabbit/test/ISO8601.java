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
package org.apache.jackrabbit.test;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * The <code>ISO8601</code> utility class provides helper methods
 * to deal with date/time formatting using a specific ISO8601-compliant
 * format (see <a href="http://www.w3.org/TR/NOTE-datetime">ISO 8601</a>).
 * <p/>
 * The currently supported format is:
 * <pre>
 *   &plusmn;YYYY-MM-DDThh:mm:ss.SSSTZD
 * </pre>
 * where:
 * <pre>
 *   &plusmn;YYYY = four-digit year with optional sign where values <= 0 are
 *           denoting years BCE and values > 0 are denoting years CE,
 *           e.g. -0001 denotes the year 2 BCE, 0000 denotes the year 1 BCE,
 *           0001 denotes the year 1 CE, and so on...
 *   MM    = two-digit month (01=January, etc.)
 *   DD    = two-digit day of month (01 through 31)
 *   hh    = two digits of hour (00 through 23) (am/pm NOT allowed)
 *   mm    = two digits of minute (00 through 59)
 *   ss    = two digits of second (00 through 59)
 *   SSS   = three digits of milliseconds (000 through 999)
 *   TZD   = time zone designator, Z for Zulu (i.e. UTC) or an offset from UTC
 *           in the form of +hh:mm or -hh:mm
 * </pre>
 */
public final class ISO8601 {
    /**
     * misc. numeric formats used in formatting
     */
    private static final DecimalFormat XX_FORMAT = new DecimalFormat("00");
    private static final DecimalFormat XXX_FORMAT = new DecimalFormat("000");
    private static final DecimalFormat XXXX_FORMAT = new DecimalFormat("0000");

    /**
     * Parses an ISO8601-compliant date/time string.
     *
     * @param text the date/time string to be parsed
     * @return a <code>Calendar</code>, or <code>null</code> if the input could
     *         not be parsed
     * @throws IllegalArgumentException if a <code>null</code> argument is passed
     */
    public static Calendar parse(String text) {
        if (text == null) {
            throw new IllegalArgumentException("argument can not be null");
        }

        // check optional leading sign
        char sign;
        int start;
        if (text.startsWith("-")) {
            sign = '-';
            start = 1;
        } else if (text.startsWith("+")) {
            sign = '+';
            start = 1;
        } else {
            sign = '+'; // no sign specified, implied '+'
            start = 0;
        }

        /**
         * the expected format of the remainder of the string is:
         * YYYY-MM-DDThh:mm:ss.SSSTZD
         *
         * note that we cannot use java.text.SimpleDateFormat for
         * parsing because it can't handle years <= 0 and TZD's
         */

        int year, month, day, hour, min, sec, ms;
        String tzID;
        try {
            // year (YYYY)
            year = Integer.parseInt(text.substring(start, start + 4));
            start += 4;
            // delimiter '-'
            if (text.charAt(start) != '-') {
                return null;
            }
            start++;
            // month (MM)
            month = Integer.parseInt(text.substring(start, start + 2));
            start += 2;
            // delimiter '-'
            if (text.charAt(start) != '-') {
                return null;
            }
            start++;
            // day (DD)
            day = Integer.parseInt(text.substring(start, start + 2));
            start += 2;
            // delimiter 'T'
            if (text.charAt(start) != 'T') {
                return null;
            }
            start++;
            // hour (hh)
            hour = Integer.parseInt(text.substring(start, start + 2));
            start += 2;
            // delimiter ':'
            if (text.charAt(start) != ':') {
                return null;
            }
            start++;
            // minute (mm)
            min = Integer.parseInt(text.substring(start, start + 2));
            start += 2;
            // delimiter ':'
            if (text.charAt(start) != ':') {
                return null;
            }
            start++;
            // second (ss)
            sec = Integer.parseInt(text.substring(start, start + 2));
            start += 2;
            // delimiter '.'
            if (text.charAt(start) != '.') {
                return null;
            }
            start++;
            // millisecond (SSS)
            ms = Integer.parseInt(text.substring(start, start + 3));
            start += 3;
            // time zone designator (Z or +00:00 or -00:00)
            if (text.charAt(start) == '+' || text.charAt(start) == '-') {
                // offset to UTC specified in the format +00:00/-00:00
                tzID = "GMT" + text.substring(start);
            } else if (text.substring(start).equals("Z")) {
                tzID = "GMT";
            } else {
                // invalid time zone designator
                return null;
            }
        } catch (IndexOutOfBoundsException e) {
            return null;
        } catch (NumberFormatException e) {
            return null;
        }

        TimeZone tz = TimeZone.getTimeZone(tzID);
        // verify id of returned time zone (getTimeZone defaults to "GMT")
        if (!tz.getID().equals(tzID)) {
            // invalid time zone
            return null;
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
            /**
             * the following call will trigger an IllegalArgumentException
             * if any of the set values are illegal or out of range
             */
            cal.getTime();
        } catch (IllegalArgumentException e) {
            return null;
        }

        return cal;
    }

    /**
     * Formats a <code>Calendar</code> value into an ISO8601-compliant
     * date/time string.
     *
     * @param cal the time value to be formatted into a date/time string.
     * @return the formatted date/time string.
     * @throws IllegalArgumentException if a <code>null</code> argument is passed
     */
    public static String format(Calendar cal) {
        if (cal == null) {
            throw new IllegalArgumentException("argument can not be null");
        }

        // determine era and adjust year if necessary
        int year = cal.get(Calendar.YEAR);
        if (cal.isSet(Calendar.ERA)
                && cal.get(Calendar.ERA) == GregorianCalendar.BC) {
            /**
             * calculate year using astronomical system:
             * year n BCE => astronomical year -n + 1
             */
            year = 0 - year + 1;
        }

        /**
         * the format of the date/time string is:
         * YYYY-MM-DDThh:mm:ss.SSSTZD
         *
         * note that we cannot use java.text.SimpleDateFormat for
         * formatting because it can't handle years <= 0 and TZD's
         */
        StringBuffer buf = new StringBuffer();
        // year ([-]YYYY)
        buf.append(XXXX_FORMAT.format(year));
        buf.append('-');
        // month (MM)
        buf.append(XX_FORMAT.format(cal.get(Calendar.MONTH) + 1));
        buf.append('-');
        // day (DD)
        buf.append(XX_FORMAT.format(cal.get(Calendar.DAY_OF_MONTH)));
        buf.append('T');
        // hour (hh)
        buf.append(XX_FORMAT.format(cal.get(Calendar.HOUR_OF_DAY)));
        buf.append(':');
        // minute (mm)
        buf.append(XX_FORMAT.format(cal.get(Calendar.MINUTE)));
        buf.append(':');
        // second (ss)
        buf.append(XX_FORMAT.format(cal.get(Calendar.SECOND)));
        buf.append('.');
        // millisecond (SSS)
        buf.append(XXX_FORMAT.format(cal.get(Calendar.MILLISECOND)));
        // time zone designator (Z or +00:00 or -00:00)
        TimeZone tz = cal.getTimeZone();
        // determine offset of timezone from UTC (incl. daylight saving) 
        int offset = tz.getOffset(cal.getTimeInMillis());
        if (offset != 0) {
            int hours = Math.abs((offset / (60 * 1000)) / 60);
            int minutes = Math.abs((offset / (60 * 1000)) % 60);
            buf.append(offset < 0 ? '-' : '+');
            buf.append(XX_FORMAT.format(hours));
            buf.append(':');
            buf.append(XX_FORMAT.format(minutes));
        } else {
            buf.append('Z');
        }
        return buf.toString();
    }
}
