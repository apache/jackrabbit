/*
 * Copyright 2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.jcr.core.observation;

import org.apache.log4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author Marcel Reutegger
 */
class FilteredEventIterator implements EventIterator {

    /**
     * Logger instance for this class
     */
    private static final Logger log
	    = Logger.getLogger(FilteredEventIterator.class);

    /**
     * The actual {@link EventState}s fired by the workspace (unfiltered)
     */
    private final Iterator actualEvents;

    /**
     * For filtering the {@link javax.jcr.observation.Event}s.
     */
    private final EventFilter filter;

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
     * @param c      an unmodifiable Collection of
     *               {@link javax.jcr.observation.Event}s.
     * @param filter only event that pass the filter will be
     *               dispatched to the event listener.
     */
    public FilteredEventIterator(EventStateCollection c, EventFilter filter) {
	actualEvents = c.iterator();
	this.filter = filter;
	fetchNext();
    }

    /**
     * @see Iterator#next()
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
     * @see EventIterator#nextEvent()
     */
    public Event nextEvent() {
	return (Event) next();
    }

    /**
     * @see javax.jcr.RangeIterator#skip(long)
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
     * @see javax.jcr.RangeIterator#getPos()
     */
    public long getPos() {
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
	    try {
		next = filter.blocks(state) ? null : new EventImpl(filter.getSession(),
			filter.getItemManager(),
			state);
	    } catch (RepositoryException e) {
		log.error("Exception while applying filter.", e);
	    }
	}
    }
}
