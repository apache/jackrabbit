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
package org.apache.jackrabbit.stats;

import java.util.concurrent.atomic.AtomicLong;

import junit.framework.TestCase;
import org.apache.jackrabbit.api.stats.RepositoryStatistics;

public class TimeSeriesRecorderTest extends TestCase {

    private TimeSeriesRecorder recorder;

    public void testCounter() {
        recorder = new TimeSeriesRecorder(RepositoryStatistics.Type.SESSION_READ_COUNTER);
        AtomicLong counter = recorder.getCounter();

        // initial values
        assertValues(recorder.getValuePerSecond());
        assertValues(recorder.getValuePerMinute());
        assertValues(recorder.getValuePerHour());
        assertValues(recorder.getValuePerWeek());

        // no changes in first second
        recorder.recordOneSecond();
        assertValues(recorder.getValuePerSecond());
        assertValues(recorder.getValuePerMinute());
        assertValues(recorder.getValuePerHour());
        assertValues(recorder.getValuePerWeek());

        // one increment in second
        counter.incrementAndGet();
        recorder.recordOneSecond();
        assertValues(recorder.getValuePerSecond(), 1);
        assertValues(recorder.getValuePerMinute());
        assertValues(recorder.getValuePerHour());
        assertValues(recorder.getValuePerWeek());

        // two increments in second
        counter.incrementAndGet();
        counter.incrementAndGet();
        recorder.recordOneSecond();
        assertValues(recorder.getValuePerSecond(), 2, 1);
        assertValues(recorder.getValuePerMinute());
        assertValues(recorder.getValuePerHour());
        assertValues(recorder.getValuePerWeek());

        // no changes in a second
        recorder.recordOneSecond();
        assertValues(recorder.getValuePerSecond(), 0, 2, 1);
        assertValues(recorder.getValuePerMinute());
        assertValues(recorder.getValuePerHour());
        assertValues(recorder.getValuePerWeek());

        // ten increments in a second
        counter.addAndGet(10);
        recorder.recordOneSecond();
        assertValues(recorder.getValuePerSecond(), 10, 0, 2, 1);
        assertValues(recorder.getValuePerMinute());
        assertValues(recorder.getValuePerHour());
        assertValues(recorder.getValuePerWeek());

        // one minute
        for (int i = 0; i < 60; i++) {
            recorder.recordOneSecond();
        }
        assertValues(recorder.getValuePerSecond());
        assertValues(recorder.getValuePerMinute(), 13);
        assertValues(recorder.getValuePerHour());
        assertValues(recorder.getValuePerWeek());

        // second minute
        for (int i = 0; i < 60; i++) {
            recorder.recordOneSecond();
        }
        assertValues(recorder.getValuePerSecond());
        assertValues(recorder.getValuePerMinute(), 0, 13);
        assertValues(recorder.getValuePerHour());
        assertValues(recorder.getValuePerWeek());

        // one hour
        for (int i = 0; i < 60 * 60; i++) {
            recorder.recordOneSecond();
        }
        assertValues(recorder.getValuePerSecond());
        assertValues(recorder.getValuePerMinute());
        assertValues(recorder.getValuePerHour(), 13);
        assertValues(recorder.getValuePerWeek());

        // one week
        for (int i = 0; i < 7 * 24 * 60 * 60; i++) {
            recorder.recordOneSecond();
        }
        assertValues(recorder.getValuePerSecond());
        assertValues(recorder.getValuePerMinute());
        assertValues(recorder.getValuePerHour());
        assertValues(recorder.getValuePerWeek(), 13);
    }

    public void testCounterWithMissing() {
        for (long m : new long[]{-42, 42}) {
            recorder = new TimeSeriesRecorder(true, m);
            AtomicLong counter = recorder.getCounter();

            // initial values
            assertValues(recorder.getValuePerSecond());
            assertValues(recorder.getValuePerMinute());
            assertValues(recorder.getValuePerHour());
            assertValues(recorder.getValuePerWeek());

            // no changes in first second
            recorder.recordOneSecond();
            assertValues(recorder.getValuePerSecond());
            assertValues(recorder.getValuePerMinute());
            assertValues(recorder.getValuePerHour());
            assertValues(recorder.getValuePerWeek());

            // one increment in second
            counter.set(0);
            counter.incrementAndGet();
            recorder.recordOneSecond();
            assertValues(recorder.getValuePerSecond(), 1);
            assertValues(recorder.getValuePerMinute());
            assertValues(recorder.getValuePerHour());
            assertValues(recorder.getValuePerWeek());

            // two increments in second
            counter.set(0);
            counter.incrementAndGet();
            counter.incrementAndGet();
            recorder.recordOneSecond();
            assertValues(recorder.getValuePerSecond(), 2, 1);
            assertValues(recorder.getValuePerMinute());
            assertValues(recorder.getValuePerHour());
            assertValues(recorder.getValuePerWeek());

            // no changes in a second
            recorder.recordOneSecond();
            assertValues(recorder.getValuePerSecond(), recorder.getMissingValue(), 2, 1);
            assertValues(recorder.getValuePerMinute());
            assertValues(recorder.getValuePerHour());
            assertValues(recorder.getValuePerWeek());

            // ten increments in a second
            counter.set(0);
            counter.addAndGet(10);
            recorder.recordOneSecond();
            assertValues(recorder.getValuePerSecond(), 10, recorder.getMissingValue(), 2, 1);
            assertValues(recorder.getValuePerMinute());
            assertValues(recorder.getValuePerHour());
            assertValues(recorder.getValuePerWeek());

            // one minute
            for (int i = 0; i < 60; i++) {
                recorder.recordOneSecond();
            }
            assertValues(recorder.getValuePerSecond());
            assertValues(recorder.getValuePerMinute(), 13);
            assertValues(recorder.getValuePerHour());
            assertValues(recorder.getValuePerWeek());

            // second minute
            for (int i = 0; i < 60; i++) {
                recorder.recordOneSecond();
            }
            assertValues(recorder.getValuePerSecond());
            assertValues(recorder.getValuePerMinute(), recorder.getMissingValue(), 13);
            assertValues(recorder.getValuePerHour());
            assertValues(recorder.getValuePerWeek());

            // one hour
            for (int i = 0; i < 60 * 60; i++) {
                recorder.recordOneSecond();
            }
            assertValues(recorder.getValuePerSecond());
            assertValues(recorder.getValuePerMinute());
            assertValues(recorder.getValuePerHour(), 13);
            assertValues(recorder.getValuePerWeek());

            // one week
            for (int i = 0; i < 7 * 24 * 60 * 60; i++) {
                recorder.recordOneSecond();
            }
            assertValues(recorder.getValuePerSecond());
            assertValues(recorder.getValuePerMinute());
            assertValues(recorder.getValuePerHour());
            assertValues(recorder.getValuePerWeek(), 13);
        }
    }

    private void assertValues(long[] values, long... expected) {
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], values[values.length - i - 1]);
        }
        for (int i = expected.length; i < values.length; i++) {
            assertEquals(recorder.getMissingValue(), values[values.length - i - 1]);
        }
    }

}
