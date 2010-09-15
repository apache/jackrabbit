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

import java.util.Map;
import java.util.HashMap;

import org.apache.jackrabbit.api.observation.JackrabbitEvent;
import javax.jcr.observation.Event;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.commons.value.ValueFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;

/**
 * Implementation of the {@link javax.jcr.observation.Event} and
 * the {@link JackrabbitEvent} interface.
 */
public final class EventImpl implements JackrabbitEvent, Event {

    /**
     * Logger instance for this class
     */
    private static final Logger log = LoggerFactory.getLogger(EventImpl.class);

    /**
     * The session of the {@link javax.jcr.observation.EventListener} this
     * event will be delivered to.
     */
    private final SessionImpl session;

    /**
     * The shared {@link EventState} object.
     */
    private final EventState eventState;

    /**
     * The timestamp of this event.
     */
    private final long timestamp;

    /**
     * The user data associated with this event.
     */
    private final String userData;

    /**
     * Cached String value of this <code>Event</code> instance.
     */
    private String stringValue;

    /**
     * Creates a new {@link javax.jcr.observation.Event} instance based on an
     * {@link EventState eventState}.
     *
     * @param session    the session of the registerd <code>EventListener</code>
     *                   where this <code>Event</code> will be delivered to.
     * @param eventState the underlying <code>EventState</code>.
     * @param timestamp  the time when the change occured that caused this event.
     * @param userData   the user data associated with this event.
     */
    EventImpl(SessionImpl session, EventState eventState,
              long timestamp, String userData) {
        this.session = session;
        this.eventState = eventState;
        this.timestamp = timestamp;
        this.userData = userData;
    }

    //---------------------------------------------------------------< Event >

    /**
     * {@inheritDoc}
     */
    public int getType() {
        return eventState.getType();
    }

    /**
     * {@inheritDoc}
     */
    public String getPath() throws RepositoryException {
        return session.getJCRPath(getQPath());
    }

    /**
     * {@inheritDoc}
     */
    public String getUserID() {
        return eventState.getUserId();
    }

    /**
     * {@inheritDoc}
     */
    public long getDate() {
        return timestamp;
    }

    /**
     * {@inheritDoc}
     */
    public String getUserData() {
        return userData;
    }

    /**
     * {@inheritDoc}
     */
    public String getIdentifier() throws RepositoryException {
        NodeId id = eventState.getChildId();
        if (id == null) {
            // property event
            id = eventState.getParentId();
        }
        return id.toString();
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, String> getInfo() throws RepositoryException {
        Map<String, String> info = new HashMap<String, String>();
        for (Map.Entry<String, InternalValue> entry : eventState.getInfo().entrySet()) {
            InternalValue value = entry.getValue();
            String strValue = null;
            if (value != null) {
                strValue = ValueFormat.getJCRString(value, session);
            }
            info.put(entry.getKey(), strValue);
        }
        return info;
    }

    //-----------------------------------------------------------< EventImpl >

    /**
     * Returns the <code>Path</code> of this event.
     *
     * @return path
     * @throws RepositoryException if the path can't be constructed
     */
    public Path getQPath() throws RepositoryException {
        try {
            Path parent = eventState.getParentPath();
            Path child = eventState.getChildRelPath();
            int index = child.getIndex();
            if (index > 0) {
                return PathFactoryImpl.getInstance().create(parent, child.getName(), index, false);
            } else {
                return PathFactoryImpl.getInstance().create(parent, child.getName(), false);
            }
        } catch (MalformedPathException e) {
            String msg = "internal error: malformed path for event";
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * Returns the uuid of the parent node.
     *
     * @return the uuid of the parent node.
     */
    public NodeId getParentId() {
        return eventState.getParentId();
    }

    /**
     * Returns the id of a child node operation.
     * If this <code>Event</code> was generated for a property
     * operation this method returns <code>null</code>.
     *
     * @return the id of a child node operation.
     */
    public NodeId getChildId() {
        return eventState.getChildId();
    }

    /**
     * Returns a flag indicating whether the child node of this event is a
     * shareable node. Only applies to node added/removed events.
     *
     * @return <code>true</code> for a shareable child node, <code>false</code>
     *         otherwise.
     */
    public boolean isShareableChildNode() {
        return eventState.isShareableNode();
    }

    /**
     * Return a flag indicating whether this is an externally generated event.
     *
     * @return <code>true</code> if this is an external event;
     *         <code>false</code> otherwise
     * @see JackrabbitEvent#isExternal()
     */
    public boolean isExternal() {
        return eventState.isExternal();
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
            sb.append(", ").append(EventState.valueOf(getType())).append(": ");
            sb.append(", UserId: ").append(getUserID());
            sb.append(", Timestamp: ").append(timestamp);
            sb.append(", UserData: ").append(userData);
            sb.append(", Info: ").append(eventState.getInfo());
            stringValue = sb.toString();
        }
        return stringValue;
    }

    /**
     * @see Object#hashCode()
     */
    public int hashCode() {
        int h = eventState.hashCode() ^ new Long(timestamp).hashCode() ^ session.hashCode();
        if (userData != null) {
            h = h ^ userData.hashCode();
        }
        return h;
    }

    /**
     * Returns <code>true</code> if this <code>Event</code> is equal to another
     * object.
     * <p/>
     * Two <code>Event</code> instances are equal if their respective
     * <code>EventState</code> instances are equal and both <code>Event</code>
     * instances are intended for the same <code>Session</code> that registerd
     * the <code>EventListener</code>.
     *
     * @param obj the reference object with which to compare.
     * @return <code>true</code> if this <code>Event</code> is equal to another
     *         object.
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof EventImpl) {
            EventImpl other = (EventImpl) obj;
            return this.eventState.equals(other.eventState)
                    && this.session.equals(other.session)
                    && this.timestamp == other.timestamp
                    && equals(this.userData, other.userData);
        }
        return false;
    }

    /**
     * Returns <code>true</code> if the objects are equal or both are
     * <code>null</code>; otherwise returns <code>false</code>.
     *
     * @param o1 an object.
     * @param o2 another object.
     * @return <code>true</code> if equal; <code>false</code> otherwise.
     */
    private static boolean equals(Object o1, Object o2) {
        if (o1 == null) {
            return o2 == null;
        } else {
            return o1.equals(o2);
        }
    }
}
