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

import junit.framework.TestCase;

public class TimeSeriesMaxTest extends TestCase {
    private TimeSeriesMax max;

    public void testMax() {
        max = new TimeSeriesMax();

        // initial values
        assertValues(max.getValuePerSecond());
        assertValues(max.getValuePerMinute());
        assertValues(max.getValuePerHour());
        assertValues(max.getValuePerWeek());

        // no changes in first second
        max.recordOneSecond();
        assertValues(max.getValuePerSecond());
        assertValues(max.getValuePerMinute());
        assertValues(max.getValuePerHour());
        assertValues(max.getValuePerWeek());

        // 2 seconds
        max.recordValue(42);
        max.recordOneSecond();
        assertValues(max.getValuePerSecond(), 42);
        assertValues(max.getValuePerMinute());
        assertValues(max.getValuePerHour());
        assertValues(max.getValuePerWeek());

        // no changes in 3rd second
        max.recordOneSecond();
        assertValues(max.getValuePerSecond(), 0, 42);
        assertValues(max.getValuePerMinute());
        assertValues(max.getValuePerHour());
        assertValues(max.getValuePerWeek());

        // 4th second
        max.recordValue(99);
        max.recordOneSecond();
        assertValues(max.getValuePerSecond(), 99, 0, 42);
        assertValues(max.getValuePerMinute());
        assertValues(max.getValuePerHour());
        assertValues(max.getValuePerWeek());

        // one minute later
        for (int i = 0; i < 60; i++) {
            max.recordOneSecond();
        }
        assertValues(max.getValuePerSecond());
        assertValues(max.getValuePerMinute(), 99);
        assertValues(max.getValuePerHour());
        assertValues(max.getValuePerWeek());

        // another minute later
        for (int i = 0; i < 60; i++) {
            max.recordOneSecond();
        }
        assertValues(max.getValuePerSecond());
        assertValues(max.getValuePerMinute(), 0, 99);
        assertValues(max.getValuePerHour());
        assertValues(max.getValuePerWeek());

        // one hour
        for (int i = 0; i < 60 * 60; i++) {
            max.recordOneSecond();
        }
        assertValues(max.getValuePerSecond());
        assertValues(max.getValuePerMinute());
        assertValues(max.getValuePerHour(), 99);
        assertValues(max.getValuePerWeek());

        // one week
        for (int i = 0; i < 7 * 24 * 60 * 60; i++) {
            max.recordOneSecond();
        }
        assertValues(max.getValuePerSecond());
        assertValues(max.getValuePerMinute());
        assertValues(max.getValuePerHour());
        assertValues(max.getValuePerWeek(), 99);
    }

    public void testMaxWithMissing() {
        for (long m : new long[]{-42, 42}) {
            max = new TimeSeriesMax(m);

            // initial values
            assertValues(max.getValuePerSecond());
            assertValues(max.getValuePerMinute());
            assertValues(max.getValuePerHour());
            assertValues(max.getValuePerWeek());

            // no changes in first second
            max.recordOneSecond();
            assertValues(max.getValuePerSecond());
            assertValues(max.getValuePerMinute());
            assertValues(max.getValuePerHour());
            assertValues(max.getValuePerWeek());

            // 2 seconds
            max.recordValue(42);
            max.recordOneSecond();
            assertValues(max.getValuePerSecond(), 42);
            assertValues(max.getValuePerMinute());
            assertValues(max.getValuePerHour());
            assertValues(max.getValuePerWeek());

            // no changes in 3rd second
            max.recordOneSecond();
            assertValues(max.getValuePerSecond(), max.getMissingValue(), 42);
            assertValues(max.getValuePerMinute());
            assertValues(max.getValuePerHour());
            assertValues(max.getValuePerWeek());

            // 4th second
            max.recordValue(99);
            max.recordOneSecond();
            assertValues(max.getValuePerSecond(), 99, max.getMissingValue(), 42);
            assertValues(max.getValuePerMinute());
            assertValues(max.getValuePerHour());
            assertValues(max.getValuePerWeek());

            // one minute later
            for (int i = 0; i < 60; i++) {
                max.recordOneSecond();
            }
            assertValues(max.getValuePerSecond());
            assertValues(max.getValuePerMinute(), 99);
            assertValues(max.getValuePerHour());
            assertValues(max.getValuePerWeek());

            // another minute later
            for (int i = 0; i < 60; i++) {
                max.recordOneSecond();
            }
            assertValues(max.getValuePerSecond());
            assertValues(max.getValuePerMinute(), max.getMissingValue(), 99);
            assertValues(max.getValuePerHour());
            assertValues(max.getValuePerWeek());

            // one hour
            for (int i = 0; i < 60 * 60; i++) {
                max.recordOneSecond();
            }
            assertValues(max.getValuePerSecond());
            assertValues(max.getValuePerMinute());
            assertValues(max.getValuePerHour(), 99);
            assertValues(max.getValuePerWeek());

            // one week
            for (int i = 0; i < 7 * 24 * 60 * 60; i++) {
                max.recordOneSecond();
            }
            assertValues(max.getValuePerSecond());
            assertValues(max.getValuePerMinute());
            assertValues(max.getValuePerHour());
            assertValues(max.getValuePerWeek(), 99);
        }
    }

    private void assertValues(long[] values, long... expected) {
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], values[values.length - i - 1]);
        }
        for (int i = expected.length; i < values.length; i++) {
            assertEquals(max.getMissingValue(), values[values.length - i - 1]);
        }
    }
}
