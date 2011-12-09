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

import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.value.ValueFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * The IdFactory
     */
    private final IdFactory idFactory;

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
     * @param event   the underlying SPI <code>Event</code>.
     * @param resolver
     * @param idFactory
     */
    EventImpl(org.apache.jackrabbit.spi.Event event,
              NamePathResolver resolver, IdFactory idFactory) {
        this.event = event;
        this.resolver = resolver;
        this.idFactory = idFactory;
    }

    //--------------------------------------------------------------< Event >---
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
        return event.getPath() != null ? resolver.getJCRPath(event.getPath()) : null;
    }

    /**
     * {@inheritDoc}
     */
    public String getUserID() {
        return event.getUserID();
    }

    /**
     * @see javax.jcr.observation.Event#getIdentifier()
     */
    public String getIdentifier() throws RepositoryException {
        ItemId itemId = event.getItemId();
        if (itemId == null) {
            return null;
        } else {
            NodeId nodeId = (itemId.denotesNode()) ? (NodeId) itemId : ((PropertyId) itemId).getParentId();
            return idFactory.toJcrIdentifier(nodeId);
        }
    }

    /**
     * @see javax.jcr.observation.Event#getInfo()
     */
    public Map<String, String> getInfo() throws RepositoryException {
        Map<String, String> jcrInfo = new HashMap<String, String>();
        for (Map.Entry<Name, QValue> entry : event.getInfo().entrySet()) {
            Name key = entry.getKey();
            QValue value = entry.getValue();
            String strValue = null;
            if (value != null) {
                strValue = ValueFormat.getJCRString(value, resolver);
            }
            jcrInfo.put(resolver.getJCRName(key), strValue);
        }
        return jcrInfo;
    }

    /**
     * @see javax.jcr.observation.Event#getUserData()
     */
    public String getUserData() throws RepositoryException {
        return event.getUserData();
    }

    /**
     * @see javax.jcr.observation.Event#getDate()
     */
    public long getDate() throws RepositoryException {
        return event.getDate();
    }

    //-------------------------------------------------------------< Object >---
    /**
     * Returns a String representation of this <code>Event</code>.
     *
     * @return a String representation of this <code>Event</code>.
     */
    @Override
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
        } else if (eventType == Event.NODE_MOVED) {
            return "NodeMoved";
        } else if (eventType == Event.PERSIST) {
            return "Persist";
        } else {
            return "UnknownEventType";
        }
    }
}
