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
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.commons.EventImpl;
import org.apache.jackrabbit.spi.commons.EventBundleImpl;
import org.apache.jackrabbit.uuid.UUID;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.PathFormat;
import org.apache.jackrabbit.name.QName;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.observation.EventListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

/**
 * <code>EventSubscription</code> listens for JCR events and creates SPI event
 * bundles for them.
 */
class EventSubscription implements EventListener {

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
            | javax.jcr.observation.Event.PROPERTY_REMOVED;

    private final List eventBundles = new ArrayList();

    private final IdFactory idFactory;

    private final NamespaceResolver nsResolver;

    EventSubscription(IdFactory idFactory, NamespaceResolver nsResolver) {
        this.idFactory = idFactory;
        this.nsResolver = nsResolver;
    }

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
    EventBundle[] getEventBundles(EventFilter[] filters, long timeout) {
        EventBundle[] bundles;
        synchronized (eventBundles) {
            while (eventBundles.isEmpty()) {
                try {
                    eventBundles.wait(timeout);
                } catch (InterruptedException e) {
                    // continue
                }
            }
            bundles = (EventBundle[]) eventBundles.toArray(new EventBundle[eventBundles.size()]);
            eventBundles.clear();
        }
        // apply filters to bundles
        for (int i = 0; i < bundles.length; i++) {
            List filteredEvents = new ArrayList();
            for (Iterator it = bundles[i].getEvents(); it.hasNext(); ) {
                Event e = (Event) it.next();
                // TODO: this is actually not correct. if filters are empty no event should go out
                if (filters == null || filters.length == 0) {
                    filteredEvents.add(e);
                } else {
                    for (int j = 0; j < filters.length; j++) {
                        if (filters[j].accept(e, bundles[i].isLocal())) {
                            filteredEvents.add(e);
                            break;
                        }
                    }
                }
            }
            bundles[i] = new EventBundleImpl(filteredEvents,
                    bundles[i].isLocal(), bundles[i].getBundleId());
        }
        return bundles;
    }

    //--------------------------------< internal >------------------------------

    private void createEventBundle(javax.jcr.observation.EventIterator events,
                                   boolean isLocal) {
        List spiEvents = new ArrayList();
        while (events.hasNext()) {
            try {
                javax.jcr.observation.Event e = events.nextEvent();
                Path p = PathFormat.parse(e.getPath(), nsResolver);
                Path parent = p.getAncestor(1);
                NodeId parentId = idFactory.createNodeId((String) null, parent);
                ItemId itemId = null;
                switch (e.getType()) {
                    case Event.NODE_ADDED:
                    case Event.NODE_REMOVED:
                        itemId = idFactory.createNodeId((String) null, p);
                        break;
                    case Event.PROPERTY_ADDED:
                    case Event.PROPERTY_CHANGED:
                    case Event.PROPERTY_REMOVED:
                        itemId = idFactory.createPropertyId(parentId,
                                p.getNameElement().getName());
                        break;
                }
                Event spiEvent = new EventImpl(e.getType(), p, itemId, parentId,
                        null, new QName[0], e.getUserID());
                spiEvents.add(spiEvent);
            } catch (Exception ex) {
                log.warn("Unable to create SPI Event: " + ex);
            }
        }
        String bundleId = UUID.randomUUID().toString();
        EventBundle bundle = new EventBundleImpl(spiEvents, isLocal, bundleId);
        synchronized (eventBundles) {
            eventBundles.add(bundle);
            eventBundles.notify();
        }
    }
}
