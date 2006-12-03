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
package org.apache.jackrabbit.test.api.observation;

import EDU.oswego.cs.dl.util.concurrent.Mutex;
import EDU.oswego.cs.dl.util.concurrent.Sync;

import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import java.util.ArrayList;
import java.util.List;
import java.io.PrintWriter;

/**
 * Utility class for <code>Event</code> retrieval with an
 * <code>EventListener</code>.
 * <p/>
 * The {@link #getEventIterator(long)} and {@link #getEvents(long)} methods
 * will block until an event is delivered and then return the events. Note, that
 * only one of the methods can be called for an expected event delivery. Calling
 * the 'other' method will block until the next events are delivered.
 */
public class EventResult implements EventListener {

    /**
     * The <code>EventIterator</code> delivered to this <code>EventListener</code>
     */
    private EventIterator events;

    /**
     * Sync object for result synchronization
     */
    private Sync sync = new Mutex();

    /**
     * <code>PrintWriter</code> where log messages are written.
     */
    private final PrintWriter log;

    /**
     * Creates a new <code>EventResult</code>.
     *
     * @param log log messages are written to this <code>Logger</code>.
     */
    public EventResult(PrintWriter log) {
        this.log = log;
        try {
            sync.acquire();
        } catch (InterruptedException e) {
            log.println("Could not aquire sync.");
            throw new RuntimeException("EventResult: Interrupted while aquiring sync.");
        }
    }

    /**
     * Gets the events from the EventListener. Waits at most <code>wait</code>
     * milliseconds for the events.
     * <p/>
     * If the events are not delivered within <code>wait</code> time an empty
     * array is returned and a log message is written.
     *
     * @param wait time in milliseconds to wait at most for <code>Event</code>s.
     * @return <code>Event</code>s.
     */
    public Event[] getEvents(long wait) {
        EventIterator events = getEventIterator(wait);
        if (events != null) {
            return getEvents(events);
        } else {
            return new Event[0];
        }
    }

    /**
     * Gets the events from the EventListener. Waits at most <code>wait</code>
     * milliseconds for the events.
     * <p/>
     * If the events are not delivered within <code>wait</code> time
     * <code>null</code> is returned and a log message is written.
     * @param wait time in milliseconds to wait at most for
     * <code>EventIterator</code>.
     * @return <code>EventIterator</code>.
     */
    public EventIterator getEventIterator(long wait) {
        try {
            if (sync.attempt(wait)) {
                // result ready
                return events;
            }
        } catch (InterruptedException e) {
            log.println("Interrupted while waiting for EventIterator");
        }
        return null;
    }

    //--------------------< EventListener >-------------------------------------

    /**
     * Called when events are delivered.
     *
     * @param events the events.
     */
    public void onEvent(EventIterator events) {
        this.events = events;
        sync.release();
    }

    /**
     * Returns the <code>events</code> as an array of <code>Event</code>
     * instances.
     * @param events the event iterator.
     * @return the events from the iterator.
     */
    private Event[] getEvents(EventIterator events) {
        List eventList = new ArrayList();
        while (events.hasNext()) {
            eventList.add(events.nextEvent());
        }
        return (Event[]) eventList.toArray(new Event[eventList.size()]);
    }
}
