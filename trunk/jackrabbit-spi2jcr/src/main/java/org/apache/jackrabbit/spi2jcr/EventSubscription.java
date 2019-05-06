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
package org.apache.jackrabbit.spi2jcr;

import org.apache.jackrabbit.spi.EventBundle;
import org.apache.jackrabbit.spi.EventFilter;
import org.apache.jackrabbit.spi.Event;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.Subscription;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.commons.EventBundleImpl;
import org.apache.jackrabbit.spi.commons.EventFilterImpl;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Arrays;
import java.util.Collections;

/**
 * <code>EventSubscription</code> listens for JCR events and creates SPI event
 * bundles for them.
 */
class EventSubscription implements Subscription, EventListener {

    /**
     * Logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(EventSubscription.class);

    /**
     * Mask for all events.
     */
    static final int ALL_EVENTS = javax.jcr.observation.Event.NODE_ADDED
            | javax.jcr.observation.Event.NODE_REMOVED
            | javax.jcr.observation.Event.PROPERTY_ADDED
            | javax.jcr.observation.Event.PROPERTY_CHANGED
            | javax.jcr.observation.Event.PROPERTY_REMOVED
            | javax.jcr.observation.Event.NODE_MOVED
            | javax.jcr.observation.Event.PERSIST;

    private final List<EventBundle> eventBundles = new ArrayList<EventBundle>();

    private final SessionInfoImpl sessionInfo;

    /**
     * Current list of filters. Copy on write is performed on this list.
     */
    private volatile List<EventFilter> filters;

    /**
     * Set to <code>true</code> if this subscription has been disposed.
     */
    private volatile boolean disposed = false;

    /**
     * The event factory.
     */
    private final EventFactory eventFactory;

    /**
     * Creates a new subscription for the passed session.
     *
     * @param idFactory     the id factory.
     * @param qValueFactory the QValueFactory.
     * @param sessionInfo   the session info.
     * @param filters       the filters that should be applied to the generated
     *                      events.
     * @throws RepositoryException if an error occurs while an event listener is
     *                             registered with the session.
     */
    EventSubscription(IdFactory idFactory,
                      QValueFactory qValueFactory,
                      SessionInfoImpl sessionInfo,
                      EventFilter[] filters) throws RepositoryException {
        this.sessionInfo = sessionInfo;
        this.eventFactory = new EventFactory(sessionInfo.getSession(),
                sessionInfo.getNamePathResolver(), idFactory, qValueFactory);
        setFilters(filters);
        ObservationManager obsMgr = sessionInfo.getSession().getWorkspace().getObservationManager();
        obsMgr.addEventListener(this, EventSubscription.ALL_EVENTS, "/", true, null, null, true);
    }

    /**
     * Sets a new list of event filters for this subscription.
     *
     * @param filters the new filters.
     * @throws RepositoryException if the filters array contains a unknown
     *                             implementation of EventFilters.
     */
    void setFilters(EventFilter[] filters) throws RepositoryException {
        // check type
        for (EventFilter filter : filters) {
            if (!(filter instanceof EventFilterImpl)) {
                throw new RepositoryException("Unknown filter implementation");
            }
        }
        List<EventFilter> tmp = new ArrayList<EventFilter>(Arrays.asList(filters));
        this.filters = Collections.unmodifiableList(tmp);

    }

    /**
     * Removes this subscription as a listener from the observation manager and
     * marks itself as disposed.
     */
    void dispose() throws RepositoryException {
        sessionInfo.removeSubscription(this);
        sessionInfo.getSession().getWorkspace().getObservationManager().removeEventListener(this);
        disposed = true;
        synchronized (eventBundles) {
            eventBundles.notify();
        }
    }

    //--------------------------< EventListener >-------------------------------

    /**
     * Adds the events to the list of pending event bundles.
     *
     * @param events the events that occurred.
     */
    public void onEvent(javax.jcr.observation.EventIterator events) {
        createEventBundle(events, false);
    }

    /**
     * @return a temporary event listener that will create local event bundles
     *         for delivered events.
     */
    EventListener getLocalEventListener() {
        return new EventListener() {
            public void onEvent(javax.jcr.observation.EventIterator events) {
                createEventBundle(events, true);
            }
        };
    }

    /**
     * @return all the pending event bundles.
     */
    EventBundle[] getEventBundles(long timeout) {
        EventBundle[] bundles;
        synchronized (eventBundles) {
            if (eventBundles.isEmpty()) {
                try {
                    eventBundles.wait(timeout);
                } catch (InterruptedException e) {
                    // continue
                }
            }
            bundles = eventBundles.toArray(new EventBundle[eventBundles.size()]);
            eventBundles.clear();
        }
        EventFilter[] eventFilters = filters.toArray(new EventFilter[filters.size()]);
        // apply filters to bundles
        for (int i = 0; i < bundles.length; i++) {
            List<Event> filteredEvents = new ArrayList<Event>();
            for (Iterator<Event> it = bundles[i].getEvents(); it.hasNext(); ) {
                Event e = it.next();
                // TODO: this is actually not correct. if filters are empty no event should go out
                if (eventFilters == null || eventFilters.length == 0) {
                    filteredEvents.add(e);
                } else {
                    for (EventFilter eventFilter : eventFilters) {
                        if (eventFilter.accept(e, bundles[i].isLocal())) {
                            filteredEvents.add(e);
                            break;
                        }
                    }
                }
            }
            bundles[i] = new EventBundleImpl(filteredEvents, bundles[i].isLocal());
        }
        return bundles;
    }

    //--------------------------------< internal >------------------------------

    private void createEventBundle(javax.jcr.observation.EventIterator events,
                                   boolean isLocal) {
        // do not create events when disposed
        if (disposed) {
            return;
        }
        List<Event> spiEvents = new ArrayList<Event>();
        while (events.hasNext()) {
            try {
                Event spiEvent = eventFactory.fromJCREvent(events.nextEvent());
                spiEvents.add(spiEvent);
            } catch (Exception ex) {
                log.warn("Unable to create SPI Event: " + ex);
            }
        }
        EventBundle bundle = new EventBundleImpl(spiEvents, isLocal);
        synchronized (eventBundles) {
            eventBundles.add(bundle);
            eventBundles.notify();
        }
    }
}
