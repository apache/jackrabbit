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

import static java.lang.Math.round;

import static java.util.Arrays.fill;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.jackrabbit.api.stats.RepositoryStatistics.Type;
import org.apache.jackrabbit.api.stats.TimeSeries;

/**
 * Recorder of a time series. An instance of this class records (and clears)
 * the state of a given {@link AtomicLong} counter once every second and
 * exposes the collected time series through the {@link TimeSeries}
 * interface.
 */
public class TimeSeriesRecorder implements TimeSeries {

    /** Value */
    private final AtomicLong counter;

    /** Whether to reset value each second */
    private final boolean resetValueEachSecond;

    /** The value used to encode missing values */
    private final long missingValue;

    /** Measured value per second over the last minute. */
    private final long[] valuePerSecond;

    /** Measured value per minute over the last hour. */
    private final long[] valuePerMinute;

    /** Measured value per hour over the last week. */
    private final long[] valuePerHour;

    /** Measured value per week over the last three years. */
    private final long[] valuePerWeek;

    /** Current second (index in {@link #valuePerSecond}) */
    private int seconds;

    /** Current minute (index in {@link #valuePerMinute}) */
    private int minutes;

    /** Current hour (index in {@link #valuePerHour}) */
    private int hours;

    /** Current week (index in {@link #valuePerWeek}) */
    private int weeks;

    public TimeSeriesRecorder(Type type) {
        this(type.isResetValueEachSecond());
    }

    /**
     * Same as {@link #TimeSeriesRecorder(boolean, long)} passing long for the 2nd argument
     * @param resetValueEachSecond    Whether to reset value each second
     */
    public TimeSeriesRecorder(boolean resetValueEachSecond) {
        this(resetValueEachSecond, 0);
    }

    /**
     * @param resetValueEachSecond    Whether to reset value each second
     * @param missingValue            The value used to encode missing values
     */
    public TimeSeriesRecorder(boolean resetValueEachSecond, long missingValue) {
        this.resetValueEachSecond = resetValueEachSecond;
        this.missingValue = missingValue;
        counter = new AtomicLong(missingValue);
        valuePerSecond = newArray(60, missingValue);
        valuePerMinute = newArray(60, missingValue);
        valuePerHour = newArray(7 * 24, missingValue);
        valuePerWeek = newArray(3 * 52, missingValue);
    }

    private static long[] newArray(int size, long value) {
        long[] array = new long[size];
        fill(array, value);
        return array;
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
            valuePerSecond[seconds++] = counter.getAndSet(missingValue);
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


    @Override
    public long getMissingValue() {
        return missingValue;
    }

    @Override
    public synchronized long[] getValuePerSecond() {
        return cyclicCopyFrom(valuePerSecond, seconds);
    }

    @Override
    public synchronized long[] getValuePerMinute() {
        return cyclicCopyFrom(valuePerMinute, minutes);
    }

    @Override
    public synchronized long[] getValuePerHour() {
        return cyclicCopyFrom(valuePerHour, hours);
    }

    @Override
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
        int count = 0;
        for (long value : array) {
            if (value != missingValue) {
                count++;
                sum += value;
            }
        }
        if (count == 0) {
            return missingValue;
        } else if (resetValueEachSecond) {
            return sum;
        } else {
            return round((double) sum / count);
        }
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
