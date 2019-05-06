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

import javax.jcr.Session;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 */
class EventListenerIteratorImpl implements EventListenerIterator {

    /**
     * This iterator will return {@link EventListener}s registered by this
     * <code>Session</code>.
     */
    private final Session session;

    /**
     * Iterator over {@link EventConsumer} instances
     */
    private final Iterator<EventConsumer> consumers;

    /**
     * The next <code>EventListener</code> that belongs to the session
     * passed in the constructor of this <code>EventListenerIteratorImpl</code>.
     */
    private EventListener next;

    /**
     * Current position
     */
    private long pos;

    /**
     * Creates a new <code>EventListenerIteratorImpl</code>.
     *
     * @param session
     * @param sConsumers synchronous consumers.
     * @param aConsumers asynchronous consumers.
     * @throws NullPointerException if <code>ticket</code> or <code>consumer</code>
     *                              is <code>null</code>.
     */
    EventListenerIteratorImpl(Session session, Collection<EventConsumer> sConsumers, Collection<EventConsumer> aConsumers)
            throws NullPointerException {
        if (session == null) {
            throw new NullPointerException("session");
        }
        if (sConsumers == null) {
            throw new NullPointerException("consumers");
        }
        if (aConsumers == null) {
            throw new NullPointerException("consumers");
        }
        this.session = session;
        Collection<EventConsumer> allConsumers = new ArrayList<EventConsumer>(sConsumers);
        allConsumers.addAll(aConsumers);
        this.consumers = allConsumers.iterator();
        fetchNext();
    }

    /**
     * {@inheritDoc}
     */
    public EventListener nextEventListener() {
        if (next == null) {
            throw new NoSuchElementException();
        }
        EventListener l = next;
        fetchNext();
        pos++;
        return l;
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
     * Remove is not supported on this Iterator.
     *
     * @throws UnsupportedOperationException
     */
    public void remove() {
        throw new UnsupportedOperationException("EventListenerIterator.remove()");
    }

    /**
     * Returns {@code true} if the iteration has more elements. (In other
     * words, returns {@code true} if {@code next} would return an element
     * rather than throwing an exception.)
     *
     * @return {@code true} if the consumers has more elements.
     */
    public boolean hasNext() {
        return (next != null);
    }

    /**
     * {@inheritDoc}
     */
    public Object next() {
        return nextEventListener();
    }

    /**
     * Fetches the next {@link javax.jcr.observation.EventListener} associated
     * with the <code>Session</code> passed in the constructor of this
     * <code>EventListenerIteratorImpl</code> from all register
     * <code>EventListener</code>s
     */
    private void fetchNext() {
        EventConsumer consumer;
        next = null;
        while (next == null && consumers.hasNext()) {
            consumer = (EventConsumer) consumers.next();
            // only return EventConsumers that belong to our session
            if (consumer.getSession().equals(session)) {
                next = consumer.getEventListener();
            }

        }
    }
}
