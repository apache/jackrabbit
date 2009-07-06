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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
class FilteredEventIterator implements javax.jcr.observation.EventIterator {

    /**
     * Logger instance for this class
     */
    private static final Logger log
            = LoggerFactory.getLogger(FilteredEventIterator.class);

    /**
     * The actual {@link EventState}s fired by the workspace (unfiltered)
     */
    private final Iterator actualEvents;

    /**
     * For filtering the {@link javax.jcr.observation.Event}s.
     */
    private final EventFilter filter;

    /**
     * Set of <code>ItemId</code>s of denied <code>ItemState</code>s.
     */
    private final Set denied;

    /**
     * The next {@link javax.jcr.observation.Event} in this iterator
     */
    private Event next;

    /**
     * Current position
     */
    private long pos;

    /**
     * The timestamp when the events occured.
     */
    private long timestamp;

    /**
     * The user data associated with these events.
     */
    private final String userData;

    /**
     * Creates a new <code>FilteredEventIterator</code>.
     *
     * @param eventStates an iterator over unfiltered {@link EventState}s.
     * @param timestamp the time when the event were created.
     * @param userData   the user data associated with these events.
     * @param filter only event that pass the filter will be dispatched to the
     *               event listener.
     * @param denied <code>Set</code> of <code>ItemId</code>s of denied <code>ItemState</code>s
     *               rejected by the <code>AccessManager</code>. If
     *               <code>null</code> no <code>ItemState</code> is denied.
     */
    public FilteredEventIterator(Iterator eventStates,
                                 long timestamp,
                                 String userData,
                                 EventFilter filter,
                                 Set denied) {
        this.actualEvents = eventStates;
        this.filter = filter;
        this.denied = denied;
        this.timestamp = timestamp;
        this.userData = userData;
        fetchNext();
    }

    /**
     * {@inheritDoc}
     */
    public Object next() {
        if (next == null) {
            throw new NoSuchElementException();
        }
        Event e = next;
        fetchNext();
        pos++;
        return e;
    }

    /**
     * {@inheritDoc}
     */
    public Event nextEvent() {
        return (Event) next();
    }

    /**
     * {@inheritDoc}
     */
    public void skip(long skipNum) {
        while (skipNum-- > 0) {
            next();
        }
    }

    /**
     * Always returns <code>-1</code>.
     *
     * @return <code>-1</code>.
     */
    public long getSize() {
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    public long getPosition() {
        return pos;
    }

    /**
     * This method is not supported.
     * Always throws a <code>UnsupportedOperationException</code>.
     */
    public void remove() {
        throw new UnsupportedOperationException("EventIterator.remove()");
    }

    /**
     * Returns <tt>true</tt> if the iteration has more elements. (In other
     * words, returns <tt>true</tt> if <tt>next</tt> would return an element
     * rather than throwing an exception.)
     *
     * @return <tt>true</tt> if the iterator has more elements.
     */
    public boolean hasNext() {
        return (next != null);
    }

    /**
     * Fetches the next Event from the collection of events
     * passed in the constructor of <code>FilteredEventIterator</code>
     * that is allowed by the {@link EventFilter}.
     */
    private void fetchNext() {
        EventState state;
        next = null;
        while (next == null && actualEvents.hasNext()) {
            state = (EventState) actualEvents.next();
            // check denied set
            if (denied == null || !denied.contains(state.getTargetId())) {
                try {
                    next = filter.blocks(state) ? null : new EventImpl(
                            filter.getSession(), state, timestamp, userData);
                } catch (RepositoryException e) {
                    log.error("Exception while applying filter.", e);
                }
            }
        }
    }
}
