/*
 * Copyright 2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.test.observation;

import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.Mutex;

import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.Event;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Utility class for <code>Event</code> retrieval with an
 * <code>EventListener</code>.
 *
 * @author Marcel Reutegger
 * @version $Revision:  $, $Date:  $
 */
public class EventResult implements EventListener {

    /** The actual <code>Event</code>s delivered to this <code>EventListener</code> */
    private Event[] events;

    /** Sync object for result synchronization */
    private Sync sync = new Mutex();

    /** <code>Logger</code> where log message are written. */
    private final Logger log;

    /**
     * Creates a new <code>EventResult</code>.
     *
     * @param log log messages are written to this <code>Logger</code>.
     */
    EventResult(Logger log) {
        this.log = log;
        try {
            sync.acquire();
        } catch (InterruptedException e) {
            log.error("Could not aquire sync.");
            throw new RuntimeException("EventResult: Interrupted while aquiring sync.");
        }
    }

    /**
     * Gets the events from the EventListener. Waits at most <code>wait</code>
     * milliseconds for the events.
     * <p>
     * If the events are not delivered within <code>wait</code> time an empty
     * array is returned and a log message is written.
     *
     * @param wait time in milliseconds to wait at most for <code>Event</code>s.
     * @return <code>Event</code>s.
     */
    public Event[] getEvents(long wait) {
        try {
            if (sync.attempt(wait)) {
                // result ready

                // release sync again for following getEvents() calls
                sync.release();
                return events;
            } else {
                log.error("Events not delivered within " + wait + " ms.");
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for EventIterator.");
        }
        return new Event[0];
    }

    //--------------------< EventListener >-------------------------------------

    /**
     * Called when events are delivered.
     * @param events the events.
     */
    public void onEvent(EventIterator events) {
        List eventList = new ArrayList();
        while (events.hasNext()) {
            eventList.add(events.nextEvent());
        }
        this.events = (Event[])eventList.toArray(new Event[eventList.size()]);
        sync.release();
    }
}
