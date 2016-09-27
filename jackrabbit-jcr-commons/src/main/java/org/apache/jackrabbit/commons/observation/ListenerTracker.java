/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.jackrabbit.commons.observation;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.nanoTime;
import static org.apache.jackrabbit.stats.TimeSeriesStatsUtil.asCompositeData;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.management.openmbean.CompositeData;

import org.apache.jackrabbit.api.jmx.EventListenerMBean;
import org.apache.jackrabbit.api.observation.JackrabbitEvent;
import org.apache.jackrabbit.commons.iterator.EventIteratorAdapter;
import org.apache.jackrabbit.stats.TimeSeriesMax;
import org.apache.jackrabbit.stats.TimeSeriesRecorder;

/**
 * Tracks event deliveries to an event listener and the way the listener
 * processes the events. The collected information is made available through
 * the {@link EventListenerMBean} interface.
 */
public class ListenerTracker {

    private final EventListener listener;

    private final int eventTypes;

    private final String absPath;

    private final boolean isDeep;

    private final String[] uuid;

    private final String[] nodeTypeName;

    private final boolean noLocal;

    protected final Exception initStackTrace =
            new Exception("The event listener was registered here:");

    private final long startTime = currentTimeMillis();

    private final AtomicLong eventDeliveries = new AtomicLong();

    private final AtomicLong eventsDelivered = new AtomicLong();

    private final AtomicLong eventDeliveryTime = new AtomicLong();

    private final AtomicLong headTimestamp = new AtomicLong();

    private final TimeSeriesMax queueLength = new TimeSeriesMax();

    private final TimeSeriesRecorder eventCount = new TimeSeriesRecorder(true);

    private final TimeSeriesRecorder eventConsumerTime = new TimeSeriesRecorder(true);

    private final TimeSeriesRecorder eventProducerTime = new TimeSeriesRecorder(true);

    final AtomicBoolean userInfoAccessedWithoutExternalsCheck =
            new AtomicBoolean();

    final AtomicBoolean userInfoAccessedFromExternalEvent =
            new AtomicBoolean();

    final AtomicBoolean dateAccessedWithoutExternalsCheck =
            new AtomicBoolean();

    final AtomicBoolean dateAccessedFromExternalEvent =
            new AtomicBoolean();

    public ListenerTracker(
            EventListener listener, int eventTypes, String absPath, boolean isDeep, String[] uuid,
            String[] nodeTypeName, boolean noLocal) {
        this.listener = listener;
        this.eventTypes = eventTypes;
        this.absPath = absPath;
        this.isDeep = isDeep;
        this.uuid = copy(uuid);
        this.nodeTypeName = copy(nodeTypeName);
        this.noLocal = noLocal;
    }

    /**
     * Called to log a deprecation warning about the detected behavior of
     * the decorated listener. Subclasses should override this method that
     * by default does nothing.
     *
     * @param message warning message
     */
    protected void warn(String message) {
        // do nothing
    }

    /**
     * Called just before the {@link EventListener#onEvent(EventIterator)}
     * method is called. The default implementation of this method does
     * nothing, but subclasses can override it to add custom processing.
     */
    protected void beforeEventDelivery() {
        // do nothing
    }

    /**
     * Called just after the {@link EventListener#onEvent(EventIterator)}
     * method has been called (even if the call threw an exception). The
     * default implementation of this method does nothing, but subclasses
     * can override it to add custom processing.
     */
    protected void afterEventDelivery() {
        // do nothing
    }

    /**
     * Applications should call this to report the current queue length.
     * @param length
     */
    public void recordQueueLength(long length) {
        queueLength.recordValue(length);
    }

    /**
     * Applications should call this to report the current queue length when an
     * item is removed from the queue.
     *
     * @param length        the length of the queue after the item was removed.
     * @param headTimestamp the time in milliseconds when the head item was
     *                      created and put into the queue.
     */
    public void recordQueueLength(long length, long headTimestamp) {
        queueLength.recordValue(length);
        this.headTimestamp.set(length == 0 ? 0 : headTimestamp);
    }

    /**
     * Records the number of measured values over the past second and resets
     * the counter. This method should be scheduled to be called once per
     * second.
     */
    public void recordOneSecond() {
        queueLength.recordOneSecond();
        eventCount.recordOneSecond();
        eventConsumerTime.recordOneSecond();
        eventProducerTime.recordOneSecond();
    }

    /**
     * Record additional producer time spent outside of the listeners, e.g.
     * before {@code onEvent()} is called.
     *
     * @param time the amount of time.
     * @param unit the time unit.
     */
    public void recordProducerTime(long time, TimeUnit unit) {
        eventProducerTime.getCounter().addAndGet(unit.toNanos(time));
    }

    public EventListener getTrackedListener() {
        return new EventListener() {
            @Override
            public void onEvent(EventIterator events) {
                eventDeliveries.incrementAndGet();
                final long start = nanoTime();
                try {
                    beforeEventDelivery();
                    listener.onEvent(new EventIteratorAdapter(events) {
                        long t0 = start;

                        private void recordTime(TimeSeriesRecorder recorder) {
                            recorder.getCounter().addAndGet(-(t0 - (t0 = nanoTime())));
                        }

                        @Override
                        public Object next() {
                            recordTime(eventConsumerTime);
                            eventsDelivered.incrementAndGet();
                            eventCount.getCounter().incrementAndGet();
                            Object object = super.next();
                            if (object instanceof JackrabbitEvent) {
                                object = new JackrabbitEventTracker(
                                        ListenerTracker.this,
                                        (JackrabbitEvent) object);
                            } else if (object instanceof Event) {
                                object = new EventTracker(
                                        ListenerTracker.this, (Event) object);
                            }
                            recordTime(eventProducerTime);
                            return object;
                        }

                        @Override
                        public boolean hasNext() {
                            recordTime(eventConsumerTime);
                            boolean result = super.hasNext();
                            recordTime(eventProducerTime);
                            return result;
                        }
                    });
                } finally {
                    afterEventDelivery();
                    eventDeliveryTime.addAndGet(nanoTime() - start);
                }
            }

            @Override
            public String toString() {
                return ListenerTracker.this.toString();
            }
        };
    }

    public EventListenerMBean getListenerMBean() {
        return new EventListenerMBean() {
            @Override
            public String getClassName() {
                return listener.getClass().getName();
            }
            @Override
            public String getToString() {
                return listener.toString();
            }
            @Override
            public String getInitStackTrace() {
                StringWriter writer = new StringWriter();
                initStackTrace.printStackTrace(new PrintWriter(writer));
                return writer.toString();
            }
            @Override
            public int getEventTypes() {
                return eventTypes;
            }
            @Override
            public String getAbsPath() {
                return absPath;
            }
            @Override
            public boolean isDeep() {
                return isDeep;
            }
            @Override
            public String[] getUuid() {
                return copy(uuid);
            }
            @Override
            public String[] getNodeTypeName() {
                return copy(nodeTypeName);
            }
            @Override
            public boolean isNoLocal() {
                return noLocal;
            }
            @Override
            public long getEventDeliveries() {
                return eventDeliveries.get();
            }
            @Override
            public long getEventDeliveriesPerHour() {
                return TimeUnit.HOURS.toMillis(getEventDeliveries())
                        / Math.max(currentTimeMillis() - startTime, 1);
            }
            @Override
            public long getMicrosecondsPerEventDelivery() {
                return TimeUnit.NANOSECONDS.toMicros(eventDeliveryTime.get())
                        / Math.max(getEventDeliveries(), 1);
            }
            @Override
            public long getEventsDelivered() {
                return eventsDelivered.get();
            }
            @Override
            public long getEventsDeliveredPerHour() {
                return TimeUnit.HOURS.toMillis(getEventsDelivered())
                        / Math.max(currentTimeMillis() - startTime, 1);
            }
            @Override
            public long getMicrosecondsPerEventDelivered() {
                return TimeUnit.NANOSECONDS.toMicros(eventDeliveryTime.get())
                        / Math.max(getEventsDelivered(), 1);
            }
            @Override
            public double getRatioOfTimeSpentProcessingEvents() {
                double timeSpentProcessingEvents =
                        TimeUnit.NANOSECONDS.toMillis(eventDeliveryTime.get());
                return timeSpentProcessingEvents
                        / Math.max(currentTimeMillis() - startTime, 1);
            }
            @Override
            public double getEventConsumerTimeRatio() {
                double consumerTime = sum(eventConsumerTime);
                double producerTime = sum(eventProducerTime);
                return consumerTime / Math.max(consumerTime + producerTime, 1);
            }
            @Override
            public boolean isUserInfoAccessedWithoutExternalsCheck() {
                return userInfoAccessedWithoutExternalsCheck.get();
            }
            @Override
            public synchronized boolean isUserInfoAccessedFromExternalEvent() {
                return userInfoAccessedFromExternalEvent.get();
            }
            @Override
            public synchronized boolean isDateAccessedWithoutExternalsCheck() {
                return dateAccessedWithoutExternalsCheck.get();
            }
            @Override
            public synchronized boolean isDateAccessedFromExternalEvent() {
                return dateAccessedFromExternalEvent.get();
            }
            @Override
            public long getQueueBacklogMillis() {
                long t = headTimestamp.get();
                if (t > 0) {
                    t = currentTimeMillis() - t;
                }
                return t;
            }
            @Override
            public CompositeData getQueueLength() {
                return asCompositeData(queueLength, "queueLength");
            }
            @Override
            public CompositeData getEventCount() {
                return asCompositeData(eventCount, "eventCount");
            }
            @Override
            public CompositeData getEventConsumerTime() {
                return asCompositeData(eventConsumerTime, "eventConsumerTime");
            }
            @Override
            public CompositeData getEventProducerTime() {
                return asCompositeData(eventProducerTime, "eventProducerTime");
            }
        };
    }

    //------------------------------------------------------------< Object >--

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (absPath != null) {
            builder.append(absPath);
        }
        if (isDeep) {
            builder.append("//*");
        } else {
            builder.append("/*");
        }
        builder.append('[');
        builder.append(Integer.toBinaryString(eventTypes));
        builder.append('b');
        if (uuid != null) {
            for (String id : uuid) {
                builder.append(", ");
                builder.append(id);
            }
        }
        if (nodeTypeName != null) {
            for (String name : nodeTypeName) {
                builder.append(", ");
                builder.append(name);
            }
        }
        if (noLocal) {
            builder.append(", no local");
        }
        builder.append("]@");
        builder.append(listener.getClass().getName());
        return builder.toString();
    }

    //-----------------------------------------------------------< private >--

    private static String[] copy(String[] array) {
        if (array != null && array.length > 0) {
            String[] copy = new String[array.length];
            System.arraycopy(array, 0, copy, 0, array.length);
            return copy;
        } else {
            return array;
        }
    }

    private static long sum(TimeSeriesRecorder timeSeries) {
        long missingValue = timeSeries.getMissingValue();
        long sum = 0;
        sum += sum(timeSeries.getValuePerSecond(), missingValue);
        sum += sum(timeSeries.getValuePerMinute(), missingValue);
        sum += sum(timeSeries.getValuePerHour(), missingValue);
        sum += sum(timeSeries.getValuePerWeek(), missingValue);
        return sum;
    }

    private static long sum(long[] values, long missing) {
        long sum = 0;
        for (long v : values) {
            if (v != missing) {
                sum += v;
            }
        }
        return sum;
    }

}
