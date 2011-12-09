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

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import org.apache.jackrabbit.commons.iterator.EventIteratorAdapter;
import org.apache.jackrabbit.commons.iterator.FilteredRangeIterator;
import org.apache.jackrabbit.commons.predicate.Predicate;
import org.apache.jackrabbit.core.SessionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
class FilteredEventIterator extends EventIteratorAdapter {

    /**
     * Logger instance for this class
     */
    private static final Logger log
            = LoggerFactory.getLogger(FilteredEventIterator.class);

    /**
     * Target session
     */
    private final SessionImpl session;

    /**
     * The timestamp when the events occurred.
     */
    private final long timestamp;

    /**
     * The user data associated with these events.
     */
    private final String userData;

    /**
     * Creates a new <code>FilteredEventIterator</code>.
     *
     * @param session target session
     * @param eventStates an iterator over unfiltered {@link EventState}s.
     * @param timestamp the time when the event were created.
     * @param userData   the user data associated with these events.
     * @param filter only event that pass the filter will be dispatched to the
     *               event listener.
     * @param denied <code>Set</code> of <code>ItemId</code>s of denied <code>ItemState</code>s
     *               rejected by the <code>AccessManager</code>
     * @param includePersistEvent whether or not to include the {@link Event#PERSIST} event
     */
    public FilteredEventIterator(
            SessionImpl session, Iterator<EventState> eventStates,
            long timestamp, String userData,
            final EventFilter filter, final Set<?> denied, boolean includePersistEvent) {
        super(new FilteredRangeIterator(wrapAndAddPersist(eventStates, includePersistEvent), new Predicate() {
            public boolean evaluate(Object object) {
                try {
                    EventState state = (EventState) object;
                    return !denied.contains(state.getTargetId())
                        && !filter.blocks(state);
                } catch (RepositoryException e) {
                    log.error("Exception while applying event filter", e);
                    return false;
                }
            }
        }));
        this.session = session;
        this.timestamp = timestamp;
        this.userData = userData;
    }

    @Override
    public Object next() {
        return new EventImpl(
                session, (EventState) super.next(), timestamp, userData);
    }

    /**
     * Optionally wrap the iterator into one that adds PERSIST events
     */
    private static Iterator<EventState> wrapAndAddPersist(final Iterator<EventState> states,
            boolean includePersistEvents) {
        if (includePersistEvents) {
            return new PersistEventAddingWrapper(states);
        }
        else {
            return states;
        }
    }

    /**
     * A wrapper around {@link Iterator} that adds a "PERSIST" event at the end.
     */
    private static class PersistEventAddingWrapper implements Iterator<EventState> {

        private Iterator<EventState> states;
        private boolean persistSent = false;
        private EventState previous = null;

        public PersistEventAddingWrapper(Iterator<EventState> states) {
            this.states = states;
        }

        public boolean hasNext() {
            if (states.hasNext()) {
                return true;
            } else {
                return !persistSent;
            }
        }

        public EventState next() {
            if (states.hasNext()) {
                previous = states.next();
                return previous;
            }
            else if (persistSent || previous == null) {
                // we are at the end; either because we already sent
                // PERSIST, or because the iterator was empty anyway
                throw new NoSuchElementException();
            }
            else {
                persistSent = true;
                return EventState.persist(previous.getSession(), previous.isExternal());
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
