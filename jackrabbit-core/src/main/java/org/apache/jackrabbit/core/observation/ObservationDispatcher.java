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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.jackrabbit.core.state.ChangeLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private BlockingQueue<DispatchAction> eventQueue = new LinkedBlockingQueue<>();

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
            log.debug("while joining notificationThread", e);
        }
        log.info("Notification of EventListeners stopped.");
    }

    /**
     * Returns an unmodifiable <code>Set</code> of <code>EventConsumer</code>s.
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
        boolean done = false;
        do {
            try {
                DispatchAction action = eventQueue.take();
                done = action == DISPOSE_MARKER;
                if (!done) {
                    eventQueueSize.getAndAdd(-action.getEventStates().size());
                    log.debug("got EventStateCollection");
                    log.debug("event delivery to " + action.getEventConsumers().size() + " consumers started...");
                    for (EventConsumer c : action.getEventConsumers()) {
                        try {
                            c.consumeEvents(action.getEventStates());
                        } catch (Throwable t) {
                            log.warn("EventConsumer " + c.getEventListener().getClass().getName() + " threw exception", t);
                            // move on to the next consumer
                        }
                    }
                }
            } catch (InterruptedException ex) {
                log.debug("event delivery interrupted", ex);
            }
        } while (!done);
        log.debug("event delivery finished.");
    }

    /**
     * {@inheritDoc}
     * <p>
     * Gives this observation manager the opportunity to
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
     * <p>
     * Dispatches the {@link EventStateCollection events} to all
     * registered {@link javax.jcr.observation.EventListener}s.
     */
    void dispatchEvents(EventStateCollection events) {
        // JCR-3426: log warning when changes are done
        // with the notification thread
        if (Thread.currentThread() == notificationThread) {
            log.warn("Save call with event notification thread detected. This " +
                    "may lead to a growing event queue. Enable debug log to " +
                    "see the stack trace with the class calling save().");
            if (log.isDebugEnabled()) {
                log.debug("Stack trace:", new Exception());
            }
        }
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
        eventQueueSize.addAndGet(events.size());
    }

    /**
     * Checks if the observation event queue contains more than the
     * configured {@link #MAX_QUEUED_EVENTS maximum number of events},
     * and delays the current thread in such cases. No delay is added
     * if the current thread is the observation thread, for example if
     * an observation listener writes to the repository.
     * <p>
     * This method should only be called outside the scope of internal
     * repository access locks.
     */
    public void delayIfEventQueueOverloaded() {
        if (eventQueueSize.get() > MAX_QUEUED_EVENTS) {
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
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    log.warn("Interrupted while rate-limiting writes", e);
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
