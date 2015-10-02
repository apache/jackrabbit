package org.apache.jackrabbit.stats;

import junit.framework.TestCase;

/**
 * michid document
 */
public class TimeSeriesMaxTest extends TestCase {

    public void testMax() {
        TimeSeriesMax values = new TimeSeriesMax();

        // initial values
        assertValues(values.getValuePerSecond());
        assertValues(values.getValuePerMinute());
        assertValues(values.getValuePerHour());
        assertValues(values.getValuePerWeek());

        // no changes in first second
        values.recordOneSecond();
        assertValues(values.getValuePerSecond());
        assertValues(values.getValuePerMinute());
        assertValues(values.getValuePerHour());
        assertValues(values.getValuePerWeek());

        // 2 seconds
        values.recordValue(42);
        values.recordOneSecond();
        assertValues(values.getValuePerSecond(), 42);
        assertValues(values.getValuePerMinute());
        assertValues(values.getValuePerHour());
        assertValues(values.getValuePerWeek());

        // no changes in 3rd second
        values.recordOneSecond();
        assertValues(values.getValuePerSecond(), 0, 42);
        assertValues(values.getValuePerMinute());
        assertValues(values.getValuePerHour());
        assertValues(values.getValuePerWeek());

        // 4th second
        values.recordValue(99);
        values.recordOneSecond();
        assertValues(values.getValuePerSecond(), 99, 0, 42);
        assertValues(values.getValuePerMinute());
        assertValues(values.getValuePerHour());
        assertValues(values.getValuePerWeek());

        // one minute later
        for (int i = 0; i < 60; i++) {
            values.recordOneSecond();
        }
        assertValues(values.getValuePerSecond());
        assertValues(values.getValuePerMinute(), 99);
        assertValues(values.getValuePerHour());
        assertValues(values.getValuePerWeek());

        // another minute later
        for (int i = 0; i < 60; i++) {
            values.recordOneSecond();
        }
        assertValues(values.getValuePerSecond());
        assertValues(values.getValuePerMinute(), 0, 99);
        assertValues(values.getValuePerHour());
        assertValues(values.getValuePerWeek());

        // one hour
        for (int i = 0; i < 60 * 60; i++) {
            values.recordOneSecond();
        }
        assertValues(values.getValuePerSecond());
        assertValues(values.getValuePerMinute());
        assertValues(values.getValuePerHour(), 99);
        assertValues(values.getValuePerWeek());

        // one week
        for (int i = 0; i < 7 * 24 * 60 * 60; i++) {
            values.recordOneSecond();
        }
        assertValues(values.getValuePerSecond());
        assertValues(values.getValuePerMinute());
        assertValues(values.getValuePerHour());
        assertValues(values.getValuePerWeek(), 99);
    }

    private static void assertValues(long[] values, long... expected) {
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], values[values.length - i - 1]);
        }
        for (int i = expected.length; i < values.length; i++) {
            assertEquals(0, values[values.length - i - 1]);
        }
    }
}
