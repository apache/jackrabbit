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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>EventConsumer</code> class combines the {@link
 * javax.jcr.observation.EventListener} with the implementation of specified
 * filter for the listener: {@link EventFilter}.
 * <p>
 * Collections of {@link EventState} objects will be dispatched to {@link
 * #consumeEvents}.
 */
class EventConsumer {

    /**
     * The default Logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(EventConsumer.class);

    private final PathFactory pathFactory = PathFactoryImpl.getInstance();

    /**
     * The <code>Session</code> associated with this <code>EventConsumer</code>.
     */
    private final SessionImpl session;

    /**
     * The listener part of this <code>EventConsumer</code>.
     */
    private final EventListener listener;

    /**
     * The <code>EventFilter</code> for this <code>EventConsumer</code>.
     */
    private final EventFilter filter;

    /**
     * A map of <code>Set</code> objects that hold references to
     * <code>ItemId</code>s of denied <code>ItemState</code>s. The map uses the
     * <code>EventStateCollection</code> as the key to reference a deny Set.
     */
    private final Map<EventStateCollection, Set<ItemId>> accessDenied = Collections.synchronizedMap(new WeakHashMap<EventStateCollection, Set<ItemId>>());

    /**
     * cached hash code value
     */
    private int hashCode;

    /**
     * An <code>EventConsumer</code> consists of a <code>Session</code>, the
     * attached <code>EventListener</code> and an <code>EventFilter</code>.
     *
     * @param session  the <code>Session</code> that created this
     *                 <code>EventConsumer</code>.
     * @param listener the actual <code>EventListener</code> to call back.
     * @param filter   only pass an <code>Event</code> to the listener if the
     *                 <code>EventFilter</code> allows the <code>Event</code>.
     * @throws NullPointerException if <code>session</code>, <code>listener</code>
     *                              or <code>filter</code> is<code>null</code>.
     */
    EventConsumer(SessionImpl session, EventListener listener, EventFilter filter)
            throws NullPointerException {
        if (session == null) {
            throw new NullPointerException("session");
        }
        if (listener == null) {
            throw new NullPointerException("listener");
        }
        if (filter == null) {
            throw new NullPointerException("filter");
        }

        this.session = session;
        this.listener = listener;
        this.filter = filter;
    }

    /**
     * Returns the <code>Session</code> that is associated
     * with this <code>EventConsumer</code>.
     *
     * @return the <code>Session</code> of this <code>EventConsumer</code>.
     */
    Session getSession() {
        return session;
    }

    /**
     * Returns the <code>EventListener</code> that is associated with this
     * <code>EventConsumer</code>.
     *
     * @return the <code>EventListener</code> of this <code>EventConsumer</code>.
     */
    EventListener getEventListener() {
        return listener;
    }

    /**
     * Checks for what {@link EventState}s this <code>EventConsumer</code> has
     * enough access rights to see the event.
     *
     * @param events the collection of {@link EventState}s.
     */
    void prepareEvents(EventStateCollection events) {
        Iterator<EventState> it = events.iterator();
        Set<ItemId> denied = null;
        while (it.hasNext()) {
            EventState state = it.next();
            if (state.getType() == Event.NODE_REMOVED
                    || state.getType() == Event.PROPERTY_REMOVED) {

                if (session.equals(state.getSession())) {
                    // if we created the event, we can be sure that
                    // we have enough access rights to see the event
                    continue;
                }

                // check read permission
                ItemId targetId = state.getTargetId();
                boolean granted = false;
                try {
                    granted = canRead(state);
                } catch (RepositoryException e) {
                    log.warn("Unable to check access rights for item: " + targetId);
                }
                if (!granted) {
                    if (denied == null) {
                        denied = new HashSet<ItemId>();
                    }
                    denied.add(targetId);
                }
            }
        }
        if (denied != null) {
            accessDenied.put(events, denied);
        }
    }

    /**
     * Checks for which deleted <code>ItemStates</code> this
     * <code>EventConsumer</code> has enough access rights to see the event.
     *
     * @param events       the collection of {@link EventState}s.
     * @param deletedItems Iterator of deleted <code>ItemState</code>s.
     */
    void prepareDeleted(EventStateCollection events, Iterable<ItemState> deletedItems) {
        Set<ItemId> denied = null;
        Set<ItemId> deletedIds = new HashSet<ItemId>();
        for (ItemState state : deletedItems) {
            deletedIds.add(state.getId());
        }

        for (Iterator<EventState> it = events.iterator(); it.hasNext();) {
            EventState evState = it.next();
            ItemId targetId = evState.getTargetId();
            if (deletedIds.contains(targetId)) {
                // check read permission
                boolean granted = false;
                try {
                    granted = canRead(evState);
                } catch (RepositoryException e) {
                    log.warn("Unable to check access rights for item: " + targetId);
                }
                if (!granted) {
                    if (denied == null) {
                        denied = new HashSet<ItemId>();
                    }
                    denied.add(targetId);
                }
            }
        }
        if (denied != null) {
            accessDenied.put(events, denied);
        }
    }

    /**
     * Dispatches the events to the <code>EventListener</code>.
     *
     * @param events a collection of {@link EventState}s
     *               to dispatch.
     */
    void consumeEvents(EventStateCollection events) throws RepositoryException {
        // Set of ItemIds of denied ItemStates
        Set<ItemId> denied = accessDenied.remove(events);
        if (denied == null) {
            denied = new HashSet<ItemId>();
        }

        // check permissions
        for (Iterator<EventState> it = events.iterator(); it.hasNext() && session.isLive();) {
            EventState state = it.next();
            if (state.getType() == Event.NODE_ADDED
                    || state.getType() == Event.PROPERTY_ADDED
                    || state.getType() == Event.PROPERTY_CHANGED) {
                ItemId targetId = state.getTargetId();
                if (!canRead(state)) {
                    denied.add(targetId);
                }
            }
        }
        // only deliver if session is still live
        if (!session.isLive()) {
            return;
        }
        // check if filtered iterator has at least one event
        EventIterator it = new FilteredEventIterator(
                session, events.iterator(), events.getTimestamp(),
                events.getUserData(), filter, denied, false);
        if (it.hasNext()) {
            long time = System.currentTimeMillis();
            listener.onEvent(it);
            time = System.currentTimeMillis() - time;
            if (log.isDebugEnabled()) {
                log.debug("listener {} processed events in {} ms.",
                        listener.getClass().getName(), time);
            }
        } else {
            // otherwise skip this listener
        }
    }

    /**
     * Returns <code>true</code> if this <code>EventConsumer</code> is equal to
     * some other object, <code>false</code> otherwise.
     * <p>
     * Two <code>EventConsumer</code>s are considered equal if they refer to the
     * same <code>Session</code> and the <code>EventListener</code>s they
     * reference are equal. Note that the <code>EventFilter</code> is ignored in
     * this check.
     *
     * @param obj the reference object with which to compare.
     * @return <code>true</code> if this <code>EventConsumer</code> is equal the
     *         other <code>EventConsumer</code>.
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof EventConsumer) {
            EventConsumer other = (EventConsumer) obj;
            return session.equals(other.session)
                    && listener.equals(other.listener);
        }
        return false;
    }

    /**
     * Returns the hash code for this <code>EventConsumer</code>.
     *
     * @return the hash code for this <code>EventConsumer</code>.
     */
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = session.hashCode() ^ listener.hashCode();
        }
        return hashCode;
    }

    /**
     * Returns <code>true</code> if the item corresponding to the specified
     * <code>eventState</code> can be read the the current session.
     *
     * @param eventState
     * @return
     * @throws RepositoryException
     */
    private boolean canRead(EventState eventState) throws RepositoryException {
        Path targetPath = pathFactory.create(eventState.getParentPath(), eventState.getChildRelPath().getName(), eventState.getChildRelPath().getNormalizedIndex(), true);
        return session.getAccessManager().isGranted(targetPath, Permission.READ);
    }
}
