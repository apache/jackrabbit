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

import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

import org.apache.jackrabbit.spi.EventFilter;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;

/**
 * Implements an event iterator that converts SPI events into JCR events and
 * filters out the ones that are not accepted by an {@link EventFilter}.
 */
class FilteredEventIterator implements EventIterator {

    /**
     * The actual {@link org.apache.jackrabbit.spi.Event}s fired by the repository service
     * (unfiltered).
     */
    protected final Iterator<org.apache.jackrabbit.spi.Event> actualEvents;

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
     * The IdFactory
     */
    private final IdFactory idFactory;

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
     * @param events    the {@link org.apache.jackrabbit.spi.Event}s as an
     *                  iterator.
     * @param isLocal   whether the events were caused by the local session.
     * @param filter    only event that pass the filter will be dispatched to
     *                  the event listener.
     * @param resolver  the name path resolver.
     * @param idFactory the id factory.
     */
    public FilteredEventIterator(Iterator<org.apache.jackrabbit.spi.Event> events,
                                 boolean isLocal,
                                 EventFilter filter,
                                 NamePathResolver resolver,
                                 IdFactory idFactory) {
        this.actualEvents = events;
        this.filter = filter;
        this.isLocal = isLocal;
        this.resolver = resolver;
        this.idFactory = idFactory;
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
     * Returns {@code true} if the iteration has more elements. (In other
     * words, returns {@code true} if {@code next} would return an element
     * rather than throwing an exception.)
     *
     * @return {@code true} if the iterator has more elements.
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
            event = actualEvents.next();
            next = filter.accept(event, isLocal) ? new EventImpl(event, resolver, idFactory) : null;
        }
    }
}
