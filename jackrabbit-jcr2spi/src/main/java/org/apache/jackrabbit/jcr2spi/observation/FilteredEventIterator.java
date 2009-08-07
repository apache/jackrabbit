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

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.apache.jackrabbit.spi.EventBundle;
import org.apache.jackrabbit.spi.EventFilter;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;

import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 */
class FilteredEventIterator implements EventIterator {

    /**
     * Logger instance for this class
     */
    private static final Logger log = LoggerFactory.getLogger(FilteredEventIterator.class);

    /**
     * The actual {@link org.apache.jackrabbit.spi.Event}s fired by the repository service
     * (unfiltered).
     */
    private final Iterator actualEvents;

    /**
     * For filtering the {@link javax.jcr.observation.Event}s.
     */
    private final EventFilter filter;

    /**
     * If <code>true</code> these events are local.
     */
    private final boolean isLocal;

    /**
     * The namespace resolver of the session that created this event iterator.
     */
    private final NamePathResolver resolver;

    /**
     * The next {@link javax.jcr.observation.Event} in this iterator
     */
    private Event next;

    /**
     * Current position
     */
    private long pos = 0;

    /**
     * Creates a new <code>FilteredEventIterator</code>.
     *
     * @param events     the {@link org.apache.jackrabbit.spi.Event}s as a
     *                   bundle.
     * @param filter     only event that pass the filter will be dispatched to
     *                   the event listener.
     * @param resolver
     */
    public FilteredEventIterator(EventBundle events,
                                 EventFilter filter,
                                 NamePathResolver resolver) {
        this.actualEvents = events.getEvents();
        this.filter = filter;
        this.isLocal = events.isLocal();
        this.resolver = resolver;
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
        org.apache.jackrabbit.spi.Event event;
        next = null;
        while (next == null && actualEvents.hasNext()) {
            event = (org.apache.jackrabbit.spi.Event) actualEvents.next();
            next = filter.accept(event, isLocal) ? new EventImpl(resolver, event) : null;
        }
    }
}
