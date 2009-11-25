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
package org.apache.jackrabbit.core.observation;

import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.BufferUtils;
import org.apache.commons.collections.buffer.UnboundedFifoBuffer;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dispatcher for dispatching events to listeners within a single workspace.
 */
public final class ObservationDispatcher extends EventDispatcher
        implements Runnable {

    /**
     * Logger instance for this class
     */
    private static final Logger log
            = LoggerFactory.getLogger(ObservationDispatcher.class);

    /**
     * Dummy DispatchAction indicating the notification thread to end
     */
    private static final DispatchAction DISPOSE_MARKER = new DispatchAction(null, null);

    /**
     * The maximum number of queued asynchronous events. To avoid of of memory
     * problems, the default value is 200'000. To change the default, set the
     * system property jackrabbit.maxQueuedEvents to the required value. If more
     * events are in the queue, the current thread waits, unless the current thread is
     * the observation dispatcher itself (in which case only a warning is logged
     * - usually observation listeners shouldn't cause new events).
     */
    private static final int MAX_QUEUED_EVENTS = Integer.parseInt(System.getProperty("jackrabbit.maxQueuedEvents", "200000"));

    /**
     * Currently active <code>EventConsumer</code>s for notification.
     */
    private Set<EventConsumer> activeConsumers = new HashSet<EventConsumer>();

    /**
     * Currently active synchronous <code>EventConsumer</code>s for notification.
     */
    private Set<EventConsumer> synchronousConsumers = new HashSet<EventConsumer>();

    /**
     * Set of <code>EventConsumer</code>s for read only Set access
     */
    private Set<EventConsumer> readOnlyConsumers;

    /**
     * Set of synchronous <code>EventConsumer</code>s for read only Set access.
     */
    private Set<EventConsumer> synchronousReadOnlyConsumers;

    /**
     * synchronization monitor for listener changes
     */
    private Object consumerChange = new Object();

    /**
     * Contains the pending events that will be delivered to event listeners
     */
    private Buffer eventQueue
            = BufferUtils.blockingBuffer(new UnboundedFifoBuffer());

    private AtomicInteger eventQueueSize = new AtomicInteger();

    /**
     * The background notification thread
     */
    private Thread notificationThread;

    private long lastError;

    /**
     * Creates a new <code>ObservationDispatcher</code> instance
     * and starts the notification thread daemon.
     */
    public ObservationDispatcher() {
        notificationThread = new Thread(this, "ObservationManager");
        notificationThread.setDaemon(true);
        notificationThread.start();
    }

    /**
     * Disposes this <code>ObservationManager</code>. This will
     * effectively stop the background notification thread.
     */
    public void dispose() {
        // dispatch dummy event to mark end of notification
        eventQueue.add(DISPOSE_MARKER);
        try {
            notificationThread.join();
        } catch (InterruptedException e) {
            // FIXME log exception ?
        }
        log.info("Notification of EventListeners stopped.");
    }

    /**
     * Returns an unmodifieable <code>Set</code> of <code>EventConsumer</code>s.
     *
     * @return <code>Set</code> of <code>EventConsumer</code>s.
     */
    Set<EventConsumer> getAsynchronousConsumers() {
        synchronized (consumerChange) {
            if (readOnlyConsumers == null) {
                readOnlyConsumers = Collections.unmodifiableSet(new HashSet<EventConsumer>(activeConsumers));
            }
            return readOnlyConsumers;
        }
    }

    Set<EventConsumer> getSynchronousConsumers() {
        synchronized (consumerChange) {
            if (synchronousReadOnlyConsumers == null) {
                synchronousReadOnlyConsumers = Collections.unmodifiableSet(new HashSet<EventConsumer>(synchronousConsumers));
            }
            return synchronousReadOnlyConsumers;
        }
    }

    /**
     * Implements the run method of the background notification
     * thread.
     */
    public void run() {
        DispatchAction action;
        while ((action = (DispatchAction) eventQueue.remove()) != DISPOSE_MARKER) {

            eventQueueSize.getAndAdd(-action.getEventStates().size());
            log.debug("got EventStateCollection");
            log.debug("event delivery to " + action.getEventConsumers().size() + " consumers started...");
            for (Iterator<EventConsumer> it = action.getEventConsumers().iterator(); it.hasNext();) {
                EventConsumer c = it.next();
                try {
                    c.consumeEvents(action.getEventStates());
                } catch (Throwable t) {
                    log.warn("EventConsumer threw exception: " + t.toString());
                    log.debug("Stacktrace: ", t);
                    // move on to the next consumer
                }
            }
            log.debug("event delivery finished.");

        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Gives this observation manager the oportunity to
     * prepare the events for dispatching.
     */
    void prepareEvents(EventStateCollection events) {
        Set<EventConsumer> consumers = new HashSet<EventConsumer>();
        consumers.addAll(getSynchronousConsumers());
        consumers.addAll(getAsynchronousConsumers());
        for (EventConsumer c : consumers) {
            c.prepareEvents(events);
        }
    }

    /**
     * {@inheritDoc}
     */
    void prepareDeleted(EventStateCollection events, ChangeLog changes) {
        Set<EventConsumer> consumers = new HashSet<EventConsumer>();
        consumers.addAll(getSynchronousConsumers());
        consumers.addAll(getAsynchronousConsumers());
        for (EventConsumer c : consumers) {
            c.prepareDeleted(events, changes.deletedStates());
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Dispatches the {@link EventStateCollection events} to all
     * registered {@link javax.jcr.observation.EventListener}s.
     */
    void dispatchEvents(EventStateCollection events) {
        // notify synchronous listeners
        Set<EventConsumer> synchronous = getSynchronousConsumers();
        if (log.isDebugEnabled()) {
            log.debug("notifying " + synchronous.size() + " synchronous listeners.");
        }
        for (EventConsumer c : synchronous) {
            try {
                c.consumeEvents(events);
            } catch (Throwable t) {
                log.error("Synchronous EventConsumer threw exception.", t);
                // move on to next consumer
            }
        }
        eventQueue.add(new DispatchAction(events, getAsynchronousConsumers()));
        int size = eventQueueSize.addAndGet(events.size());
        if (size > MAX_QUEUED_EVENTS) {
            boolean logWarning = false;
            long now = System.currentTimeMillis();
            // log a warning at most every 5 seconds (to avoid filling the log file)
            if (lastError == 0 || now > lastError + 5000) {
                logWarning = true;
                log.warn("More than " + MAX_QUEUED_EVENTS + " events in the queue", new Exception("Stack Trace"));
                lastError = now;
            }
            if (Thread.currentThread() == notificationThread) {
                if (logWarning) {
                    log.warn("Recursive notification?");
                }
            } else {
                if (logWarning) {
                    log.warn("Waiting");
                }
                while (eventQueueSize.get() > MAX_QUEUED_EVENTS) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
            }
        }
    }

    /**
     * Adds or replaces an event consumer.
     * @param consumer the <code>EventConsumer</code> to add or replace.
     */
    void addConsumer(EventConsumer consumer) {
        synchronized (consumerChange) {
            if (consumer.getEventListener() instanceof SynchronousEventListener) {
                // remove existing if any
                synchronousConsumers.remove(consumer);
                // re-add it
                synchronousConsumers.add(consumer);
                // reset read only consumer set
                synchronousReadOnlyConsumers = null;
            } else {
                // remove existing if any
                activeConsumers.remove(consumer);
                // re-add it
                activeConsumers.add(consumer);
                // reset read only consumer set
                readOnlyConsumers = null;
            }
        }
    }

    /**
     * Unregisters an event consumer from event notification.
     * @param consumer the consumer to deregister.
     */
    void removeConsumer(EventConsumer consumer) {
        synchronized (consumerChange) {
            if (consumer.getEventListener() instanceof SynchronousEventListener) {
                synchronousConsumers.remove(consumer);
                // reset read only listener set
                synchronousReadOnlyConsumers = null;
            } else {
                activeConsumers.remove(consumer);
                // reset read only listener set
                readOnlyConsumers = null;
            }
        }
    }

}
