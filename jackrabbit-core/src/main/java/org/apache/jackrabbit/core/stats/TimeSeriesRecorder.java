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

/**
 * Recorder of a time series. An instance of this class records (and clears)
 * the state of a given {@link AtomicLong} counter once every second and
 * exposes the collected time series through the {@link TimeSeries}
 * interface.
 */
class TimeSeriesRecorder implements TimeSeries {

    /** Event counter */
    private final AtomicLong counter = new AtomicLong();

    /** Number of events per second over the last minute. */
    private final long[] eventsPerSecond = new long[60];

    /** Number of events per minute over the last hour. */
    private final long[] eventsPerMinute = new long[60];

    /** Number of events per hour over the last week. */
    private final long[] eventsPerHour = new long[7 * 24];

    /** Number of events per week over the last three years. */
    private final long[] eventsPerWeek = new long[3 * 52];

    /** Current second (index in {@link #eventsPerSecond}) */
    private int seconds = 0;

    /** Current minute (index in {@link #eventsPerMinute}) */
    private int minutes = 0;

    /** Current hour (index in {@link #eventsPerHour}) */
    private int hours = 0;

    /** Current week (index in {@link #eventsPerWeek}) */
    private int weeks = 0;

    /**
     * Returns the {@link AtomicLong} instance used to count events for
     * the time series.
     *
     * @return event counter
     */
    public AtomicLong getCounter() {
        return counter;
    }

    /**
     * Records the number of counted events over the past second and resets
     * the counter. This method should be scheduled to be called once per
     * second.
     */
    public synchronized void recordOneSecond() {
        eventsPerSecond[seconds++] = counter.getAndSet(0);
        if (seconds == eventsPerSecond.length) {
            seconds = 0;
            eventsPerMinute[minutes++] = sum(eventsPerSecond);
        }
        if (minutes == eventsPerMinute.length) {
            minutes = 0;
            eventsPerHour[hours++] = sum(eventsPerMinute);
        }
        if (hours == eventsPerHour.length) {
            hours = 0;
            eventsPerWeek[weeks++] = sum(eventsPerHour);
        }
        if (weeks == eventsPerWeek.length) {
            weeks = 0;
        }
    }

    //----------------------------------------------------------< TimeSeries >

    public synchronized long[] getEventsPerSecond() {
        return cyclicCopyFrom(eventsPerSecond, seconds);
    }

    public synchronized long[] getEventsPerMinute() {
        return cyclicCopyFrom(eventsPerMinute, minutes);
    }

    public synchronized long[] getEventsPerHour() {
        return cyclicCopyFrom(eventsPerHour, hours);
    }

    public synchronized long[] getEventsPerWeek() {
        return cyclicCopyFrom(eventsPerWeek, weeks);
    }

    //-------------------------------------------------------------< private >

    /**
     * Returns the sum of all entries in the given array.
     *
     * @param array array to be summed
     * @return sum of entries
     */
    private static long sum(long[] array) {
        long sum = 0;
        for (int i = 0; i < array.length; i++) {
            sum += array[i];
        }
        return sum;
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
