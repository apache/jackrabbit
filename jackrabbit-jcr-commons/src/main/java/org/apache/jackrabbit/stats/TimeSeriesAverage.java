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

    public TimeSeriesAverage(TimeSeries value, TimeSeries counter) {
        this.value = value;
        this.counter = counter;
    }

    //----------------------------------------------------------< TimeSeries >

    public long[] getValuePerSecond() {
        long[] values = value.getValuePerSecond();
        long[] counts = counter.getValuePerSecond();
        return divide(values, counts);
    }

    public long[] getValuePerMinute() {
        long[] values = value.getValuePerMinute();
        long[] counts = counter.getValuePerMinute();
        return divide(values, counts);
    }

    public synchronized long[] getValuePerHour() {
        long[] values = value.getValuePerHour();
        long[] counts = counter.getValuePerHour();
        return divide(values, counts);
    }

    public synchronized long[] getValuePerWeek() {
        long[] values = value.getValuePerWeek();
        long[] counts = counter.getValuePerWeek();
        return divide(values, counts);
    }

    //-------------------------------------------------------------< private >

    /**
     * Per-entry division of two arrays.
     *
     * @param a array
     * @param b array
     * @return result of division
     */
    private long[] divide(long[] a, long[] b) {
        long[] c = new long[a.length];
        for (int i = 0; i < a.length; i++) {
            if (b[i] != 0) {
                c[i] = a[i] / b[i];
            } else {
                c[i] = 0;
            }
        }
        return c;
    }

}
