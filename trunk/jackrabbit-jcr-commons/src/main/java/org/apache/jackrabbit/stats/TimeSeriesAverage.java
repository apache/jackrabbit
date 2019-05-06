/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.stats;

import org.apache.jackrabbit.api.stats.TimeSeries;

/**
 * Time series of the average calculated by dividing a measured
 * value by the counter of events during the measurement period.
 */
public class TimeSeriesAverage implements TimeSeries {

    /** Value */
    private final TimeSeries value;

    /** Value */
    private final TimeSeries counter;

    /** The value used to encode missing values */
    private final long missingValue;

    /**
     * Same as {@link #TimeSeriesAverage(TimeSeries, TimeSeries, long)} passing 0 for the 3rd argument.
     * @param value         {@code TimeSeries} of values
     * @param counter       {@code TimeSeries} of counts
     */
    public TimeSeriesAverage(TimeSeries value, TimeSeries counter) {
        this(value, counter, 0);
    }

    /**
     * @param value         {@code TimeSeries} of values
     * @param counter       {@code TimeSeries} of counts
     * @param missingValue  The value used to encode missing values
     */
    public TimeSeriesAverage(TimeSeries value, TimeSeries counter, long missingValue) {
        this.value = value;
        this.counter = counter;
        this.missingValue = missingValue;
    }

    //----------------------------------------------------------< TimeSeries >

    @Override
    public long getMissingValue() {
        return missingValue;
    }

    @Override
    public long[] getValuePerSecond() {
        long[] values = value.getValuePerSecond();
        long[] counts = counter.getValuePerSecond();
        return divide(values, counts);
    }

    @Override
    public long[] getValuePerMinute() {
        long[] values = value.getValuePerMinute();
        long[] counts = counter.getValuePerMinute();
        return divide(values, counts);
    }

    @Override
    public synchronized long[] getValuePerHour() {
        long[] values = value.getValuePerHour();
        long[] counts = counter.getValuePerHour();
        return divide(values, counts);
    }

    @Override
    public synchronized long[] getValuePerWeek() {
        long[] values = value.getValuePerWeek();
        long[] counts = counter.getValuePerWeek();
        return divide(values, counts);
    }

    //-------------------------------------------------------------< private >

    /**
     * Per-entry division of two arrays.
     *
     * @param v array
     * @param c array
     * @return result of division
     */
    private long[] divide(long[] v, long[] c) {
        long[] avg = new long[v.length];
        for (int i = 0; i < v.length; i++) {
            if (c[i] == 0 || v[i] == value.getMissingValue() || c[i] == counter.getMissingValue()) {
                avg[i] = missingValue;
            } else {
                avg[i] = v[i] / c[i];
            }
        }
        return avg;
    }

}
