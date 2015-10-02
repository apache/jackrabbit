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

import java.util.concurrent.atomic.AtomicLong;

import junit.framework.TestCase;

public class TimeSeriesAverageTest extends TestCase {
    private TimeSeriesAverage avg;

    public void testAverage() {
        TimeSeriesRecorder values = new TimeSeriesRecorder(true);
        TimeSeriesRecorder counts = new TimeSeriesRecorder(true);
        avg = new TimeSeriesAverage(values, counts);
        AtomicLong value = values.getCounter();
        AtomicLong count = counts.getCounter();

        // initial values
        assertValues(avg.getValuePerSecond());
        assertValues(avg.getValuePerMinute());
        assertValues(avg.getValuePerHour());
        assertValues(avg.getValuePerWeek());

        // no changes in first second
        values.recordOneSecond();
        counts.recordOneSecond();
        assertValues(avg.getValuePerSecond());
        assertValues(avg.getValuePerMinute());
        assertValues(avg.getValuePerHour());
        assertValues(avg.getValuePerWeek());

        // 2 seconds
        value.set(42);
        count.set(2);
        values.recordOneSecond();
        counts.recordOneSecond();
        assertValues(avg.getValuePerSecond(), 21);
        assertValues(avg.getValuePerMinute());
        assertValues(avg.getValuePerHour());
        assertValues(avg.getValuePerWeek());

        // no changes in 3rd second
        values.recordOneSecond();
        counts.recordOneSecond();
        assertValues(avg.getValuePerSecond(), 0, 21);
        assertValues(avg.getValuePerMinute());
        assertValues(avg.getValuePerHour());
        assertValues(avg.getValuePerWeek());

        // one minute later
        for (int i = 0; i < 60; i++) {
            values.recordOneSecond();
            counts.recordOneSecond();
        }
        assertValues(avg.getValuePerSecond());
        assertValues(avg.getValuePerMinute(), 21);
        assertValues(avg.getValuePerHour());
        assertValues(avg.getValuePerWeek());

        // another minute later
        for (int i = 0; i < 60; i++) {
            values.recordOneSecond();
            counts.recordOneSecond();
        }
        assertValues(avg.getValuePerSecond());
        assertValues(avg.getValuePerMinute(), 0, 21);
        assertValues(avg.getValuePerHour());
        assertValues(avg.getValuePerWeek());

        // one hour
        for (int i = 0; i < 60 * 60; i++) {
            values.recordOneSecond();
            counts.recordOneSecond();
        }
        assertValues(avg.getValuePerSecond());
        assertValues(avg.getValuePerMinute());
        assertValues(avg.getValuePerHour(), 21);
        assertValues(avg.getValuePerWeek());

        // one week
        for (int i = 0; i < 7 * 24 * 60 * 60; i++) {
            values.recordOneSecond();
            counts.recordOneSecond();
        }
        assertValues(avg.getValuePerSecond());
        assertValues(avg.getValuePerMinute());
        assertValues(avg.getValuePerHour());
        assertValues(avg.getValuePerWeek(), 21);
    }

    public void testAverageWithMissing() {
        for (long m : new long[]{-42, 42}) {
            TimeSeriesRecorder values = new TimeSeriesRecorder(true);
            TimeSeriesRecorder counts = new TimeSeriesRecorder(true);
            avg = new TimeSeriesAverage(values, counts, m);
            AtomicLong value = values.getCounter();
            AtomicLong count = counts.getCounter();

            // initial values
            assertValues(avg.getValuePerSecond());
            assertValues(avg.getValuePerMinute());
            assertValues(avg.getValuePerHour());
            assertValues(avg.getValuePerWeek());

            // no changes in first second
            values.recordOneSecond();
            counts.recordOneSecond();
            assertValues(avg.getValuePerSecond());
            assertValues(avg.getValuePerMinute());
            assertValues(avg.getValuePerHour());
            assertValues(avg.getValuePerWeek());

            // 2 seconds
            value.set(42);
            count.set(2);
            values.recordOneSecond();
            counts.recordOneSecond();
            assertValues(avg.getValuePerSecond(), 21);
            assertValues(avg.getValuePerMinute());
            assertValues(avg.getValuePerHour());
            assertValues(avg.getValuePerWeek());

            // no changes in 3rd second
            values.recordOneSecond();
            counts.recordOneSecond();
            assertValues(avg.getValuePerSecond(), avg.getMissingValue(), 21);
            assertValues(avg.getValuePerMinute());
            assertValues(avg.getValuePerHour());
            assertValues(avg.getValuePerWeek());

            // Division by 0 reported as missing
            value.set(1);
            count.set(0);
            values.recordOneSecond();
            counts.recordOneSecond();

            // one minute later
            for (int i = 0; i < 60; i++) {
                values.recordOneSecond();
                counts.recordOneSecond();
            }
            assertValues(avg.getValuePerSecond());
            assertValues(avg.getValuePerMinute(), 21);
            assertValues(avg.getValuePerHour());
            assertValues(avg.getValuePerWeek());

            // another minute later
            for (int i = 0; i < 60; i++) {
                values.recordOneSecond();
                counts.recordOneSecond();
            }
            assertValues(avg.getValuePerSecond());
            assertValues(avg.getValuePerMinute(), avg.getMissingValue(), 21);
            assertValues(avg.getValuePerHour());
            assertValues(avg.getValuePerWeek());

            // one hour
            for (int i = 0; i < 60 * 60; i++) {
                values.recordOneSecond();
                counts.recordOneSecond();
            }
            assertValues(avg.getValuePerSecond());
            assertValues(avg.getValuePerMinute());
            assertValues(avg.getValuePerHour(), 21);
            assertValues(avg.getValuePerWeek());

            // one week
            for (int i = 0; i < 7 * 24 * 60 * 60; i++) {
                values.recordOneSecond();
                counts.recordOneSecond();
            }
            assertValues(avg.getValuePerSecond());
            assertValues(avg.getValuePerMinute());
            assertValues(avg.getValuePerHour());
            assertValues(avg.getValuePerWeek(), 21);
        }
    }

    private void assertValues(long[] values, long... expected) {
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], values[values.length - i - 1]);
        }
        for (int i = expected.length; i < values.length; i++) {
            assertEquals(avg.getMissingValue(), values[values.length - i - 1]);
        }
    }
}
