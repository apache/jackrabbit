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
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.jcr.PropertyType;
import javax.jcr.ValueFormatException;

/**
 * Date value.
 */
class DateValue extends AbstractValue {

    /**
     * Serial version UID
     */
    private static final long serialVersionUID = -2382837055824423966L;

    // misc. numeric formats used in formatting
    private static final DecimalFormat XX_FORMAT = new DecimalFormat("00");
    private static final DecimalFormat XXX_FORMAT = new DecimalFormat("000");
    private static final DecimalFormat XXXX_FORMAT = new DecimalFormat("0000");

    /**
     * The date value
     */
    private final Calendar value;

    /**
     * Creates an instance for the given date value.
     *
     * @param value the date value
     */
    public DateValue(Calendar value) {
        this.value = value;
    }

    /**
     * Returns {@link PropertyType#DATE}.
     */
    public int getType() {
        return PropertyType.DATE;
    }

    /**
     * Returns a copy of this <code>Calendar</code> value. Modifying the
     * returned <code>Calendar</code> does not change the value of this
     * instance.
     */
    @Override
    public Calendar getDate() {
        return (Calendar) value.clone();
    }

    /**
     * The date is converted to the number of milliseconds since
     * 00:00 (UTC) 1 January 1970 (1970-01-01T00:00:00.000Z).
     */
    @Override
    public BigDecimal getDecimal() {
        return new BigDecimal(value.getTimeInMillis());
    }

    /**
     * The date is converted to the number of milliseconds since
     * 00:00 (UTC) 1 January 1970 (1970-01-01T00:00:00.000Z). If this number
     * is out-of-range for a double, a ValueFormatException is thrown.
     */
    @Override
    public double getDouble() {
        return value.getTimeInMillis();
    }

    /**
     * The date is converted to the number of milliseconds since
     * 00:00 (UTC) 1 January 1970 (1970-01-01T00:00:00.000Z). If this number
     * is out-of-range for a long, a ValueFormatException is thrown.
     */
    @Override
    public long getLong() {
        return value.getTimeInMillis();
    }

    /**
     * The date is converted to the following format:
     * <code>sYYYY-MM-DDThh:mm:ss.sssTZD</code>
     * where:
     * <dl>
     *   <dt>sYYYY</dt>
     *   <dd>
     *     Four-digit year with optional leading positive (‘+’) or
     *     negative (‘-’) sign. 0000 , -0000 and +0000 all indicate
     *     the year 1 BCE. –YYYY where YYYY is the number y indicates
     *     the year (y+1) BCE. The absence of a sign or the presence
     *     of a positive sign indicates a year CE. For example, -0054
     *     would indicate the year 55 BCE, while +1969 and 1969
     *     indicate the year 1969 CE.
     *   </dd>
     *   <dt>MM</dt>
     *   <dd>
     *     Two-digit month (01 = January, etc.)
     *   </dd>
     *   <dt>DD</dt>
     *   <dd>
     *     Two-digit day of month (01 through 31)
     *   </dd>
     *   <dt>hh</dt>
     *   <dd>
     *     Two digits of hour (00 through 23, or 24 if mm is 00 and
     *     ss.sss is 00.000)
     *   </dd>
     *   <dt>mm</dt>
     *   <dd>
     *     Two digits of minute (00 through 59)
     *   </dd>
     *   <dt>ss.sss</dt>
     *   <dd>
     *     Seconds, to three decimal places (00.000 through 59.999 or
     *     60.999 in the case of leap seconds)
     *   </dd>
     *   <dt>TZD</dt>
     *   <dd>
     *     Time zone designator (either Z for Zulu, i.e. UTC, or +hh:mm or
     *     -hh:mm, i.e. an offset from UTC)
     *   </dd>
     * </dl>
     * <p>
     * Note that the “T” separating the date from the time and the
     * separators “-”and “:” appear literally in the string.
     * <p>
     * This format is a subset of the format defined by ISO 8601:2004.
     * If the DATE value cannot be represented in this format a
     * {@link ValueFormatException} is thrown.
     */
    public String getString() {
        // determine era and adjust year if necessary
        int year = value.get(Calendar.YEAR);
        if (value.isSet(Calendar.ERA)
                && value.get(Calendar.ERA) == GregorianCalendar.BC) {
            // calculate year using astronomical system:
            // year n BCE => astronomical year - n + 1
            year = 0 - year + 1;
        }

        // note that we cannot use java.text.SimpleDateFormat for
        // formatting because it can't handle years <= 0 and TZD's
        StringBuilder buf = new StringBuilder(32);

        // year ([-]YYYY)
        buf.append(XXXX_FORMAT.format(year));
        buf.append('-');
        // month (MM)
        buf.append(XX_FORMAT.format(value.get(Calendar.MONTH) + 1));
        buf.append('-');
        // day (DD)
        buf.append(XX_FORMAT.format(value.get(Calendar.DAY_OF_MONTH)));
        buf.append('T');
        // hour (hh)
        buf.append(XX_FORMAT.format(value.get(Calendar.HOUR_OF_DAY)));
        buf.append(':');
        // minute (mm)
        buf.append(XX_FORMAT.format(value.get(Calendar.MINUTE)));
        buf.append(':');
        // second (ss)
        buf.append(XX_FORMAT.format(value.get(Calendar.SECOND)));
        buf.append('.');
        // millisecond (SSS)
        buf.append(XXX_FORMAT.format(value.get(Calendar.MILLISECOND)));

        // time zone designator (Z or +00:00 or -00:00)
        TimeZone tz = value.getTimeZone();
        // time zone offset (in minutes) from UTC (including daylight saving)
        int offset = tz.getOffset(value.getTimeInMillis()) / 1000 / 60;
        if (offset != 0) {
            int hours = Math.abs(offset / 60);
            int minutes = Math.abs(offset % 60);
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
