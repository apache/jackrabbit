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
package org.apache.jackrabbit.jcr2spi.observation;

import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

/**
 * Implementation of the {@link javax.jcr.observation.Event} interface.
 */
final class EventImpl implements Event {

    /**
     * Logger instance for this class
     */
    private static final Logger log = LoggerFactory.getLogger(EventImpl.class);

    /**
     * The session of the {@link javax.jcr.observation.EventListener} this
     * event will be delivered to.
     */
    private final NamePathResolver resolver;

    /**
     * The underlying SPI event.
     */
    private final org.apache.jackrabbit.spi.Event event;

    /**
     * Cached String value of this <code>Event</code> instance.
     */
    private String stringValue;

    /**
     * Creates a new {@link javax.jcr.observation.Event} instance based on an
     * {@link org.apache.jackrabbit.spi.Event SPI Event}.
     *
     * @param resolver
     * @param event   the underlying SPI <code>Event</code>.
     */
    EventImpl(NamePathResolver resolver, org.apache.jackrabbit.spi.Event event) {
        this.resolver = resolver;
        this.event = event;
    }

    /**
     * {@inheritDoc}
     */
    public int getType() {
        return event.getType();
    }

    /**
     * {@inheritDoc}
     */
    public String getPath() throws RepositoryException {
        return resolver.getJCRPath(event.getPath());
    }

    /**
     * {@inheritDoc}
     */
    public String getUserID() {
        return event.getUserID();
    }

    /**
     * Returns a String representation of this <code>Event</code>.
     *
     * @return a String representation of this <code>Event</code>.
     */
    public String toString() {
        if (stringValue == null) {
            StringBuffer sb = new StringBuffer();
            sb.append("Event: Path: ");
            try {
                sb.append(getPath());
            } catch (RepositoryException e) {
                log.error("Exception retrieving path: " + e);
                sb.append("[Error retrieving path]");
            }
            sb.append(", ").append(valueOf(getType())).append(": ");
            sb.append(", UserId: ").append(getUserID());
            stringValue = sb.toString();
        }
        return stringValue;
    }

    //----------------------------------< internal >----------------------------

    /**
     * Returns a String representation of <code>eventType</code>.
     *
     * @param eventType an event type defined by {@link Event}.
     * @return a String representation of <code>eventType</code>.
     */
    private static String valueOf(int eventType) {
        if (eventType == Event.NODE_ADDED) {
            return "NodeAdded";
        } else if (eventType == Event.NODE_REMOVED) {
            return "NodeRemoved";
        } else if (eventType == Event.PROPERTY_ADDED) {
            return "PropertyAdded";
        } else if (eventType == Event.PROPERTY_CHANGED) {
            return "PropertyChanged";
        } else if (eventType == Event.PROPERTY_REMOVED) {
            return "PropertyRemoved";
        } else {
            return "UnknownEventType";
        }
    }
}
