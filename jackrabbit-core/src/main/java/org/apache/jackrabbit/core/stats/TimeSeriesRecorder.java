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
package org.apache.jackrabbit.core.stats;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.jackrabbit.api.stats.TimeSeries;
import org.apache.jackrabbit.api.stats.RepositoryStatistics.Type;

/**
 * Recorder of a time series. An instance of this class records (and clears)
 * the state of a given {@link AtomicLong} counter once every second and
 * exposes the collected time series through the {@link TimeSeries}
 * interface.
 */
class TimeSeriesRecorder implements TimeSeries {

    /** Value */
    private final AtomicLong counter = new AtomicLong();

    /** Whether to reset value each second */
    private final boolean resetValueEachSecond;

    /** Measured value per second over the last minute. */
    private final long[] valuePerSecond = new long[60];

    /** Measured value per minute over the last hour. */
    private final long[] valuePerMinute = new long[60];

    /** Measured value per hour over the last week. */
    private final long[] valuePerHour = new long[7 * 24];

    /** Measured value per week over the last three years. */
    private final long[] valuePerWeek = new long[3 * 52];

    /** Current second (index in {@link #valuePerSecond}) */
    private int seconds = 0;

    /** Current minute (index in {@link #valuePerMinute}) */
    private int minutes = 0;

    /** Current hour (index in {@link #valuePerHour}) */
    private int hours = 0;

    /** Current week (index in {@link #valuePerWeek}) */
    private int weeks = 0;

    public TimeSeriesRecorder(Type type) {
        this(type.isResetValueEachSecond());
    }

    public TimeSeriesRecorder(boolean resetValueEachSecond) {
        this.resetValueEachSecond = resetValueEachSecond;
    }

    /**
     * Returns the {@link AtomicLong} instance used to measure the value for
     * the time series.
     *
     * @return value
     */
    public AtomicLong getCounter() {
        return counter;
    }

    /**
     * Records the number of measured values over the past second and resets
     * the counter. This method should be scheduled to be called once per
     * second.
     */
    public synchronized void recordOneSecond() {
        if (resetValueEachSecond) {
            valuePerSecond[seconds++] = counter.getAndSet(0);
        } else {
            valuePerSecond[seconds++] = counter.get();
        }
        if (seconds == valuePerSecond.length) {
            seconds = 0;
            valuePerMinute[minutes++] = aggregate(valuePerSecond);
        }
        if (minutes == valuePerMinute.length) {
            minutes = 0;
            valuePerHour[hours++] = aggregate(valuePerMinute);
        }
        if (hours == valuePerHour.length) {
            hours = 0;
            valuePerWeek[weeks++] = aggregate(valuePerHour);
        }
        if (weeks == valuePerWeek.length) {
            weeks = 0;
        }
    }

    //----------------------------------------------------------< TimeSeries >

    public synchronized long[] getValuePerSecond() {
        return cyclicCopyFrom(valuePerSecond, seconds);
    }

    public synchronized long[] getValuePerMinute() {
        return cyclicCopyFrom(valuePerMinute, minutes);
    }

    public synchronized long[] getValuePerHour() {
        return cyclicCopyFrom(valuePerHour, hours);
    }

    public synchronized long[] getValuePerWeek() {
        return cyclicCopyFrom(valuePerWeek, weeks);
    }

    //-------------------------------------------------------------< private >

    /**
     * Returns the sum of all entries in the given array.
     *
     * @param array array to be summed
     * @return sum of entries
     */
    private long aggregate(long[] array) {
        long sum = 0;
        for (int i = 0; i < array.length; i++) {

            sum += array[i];
        }
        if (resetValueEachSecond) {
            return sum;
        }
        return sum / array.length;
    }

    /**
     * Returns a copy of the given cyclical array, with the element at
     * the given position as the first element of the returned array.
     *
     * @param array cyclical array
     * @param pos position of the first element
     * @return copy of the array
     */
    private static long[] cyclicCopyFrom(long[] array, int pos) {
        long[] reverse = new long[array.length];
        for (int i = 0; i < array.length; i++) {
            reverse[i] = array[(pos + i) % array.length];
        }
        return reverse;
    }
}
