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
package org.apache.jackrabbit.commons.iterator;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.jcr.RangeIterator;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

/**
 * Adapter class for turning {@link RangeIterator}s or {@link Iterator}s
 * into {@link EventIterator}s.
 */
public class EventIteratorAdapter extends RangeIteratorDecorator
        implements EventIterator {

    /**
     * Creates an adapter for the given {@link RangeIterator}.
     *
     * @param iterator iterator of {@link Event}s
     */
    public EventIteratorAdapter(EventIterator iterator) {
        super(iterator);
    }

    /**
     * Creates an adapter for the given {@link RangeIterator}.
     *
     * @param iterator iterator of {@link Event}s
     */
    public EventIteratorAdapter(RangeIterator iterator) {
        super(iterator);
    }

    /**
     * Creates an adapter for the given {@link Iterator}.
     *
     * @param iterator iterator of {@link Event}s.
     */
    public EventIteratorAdapter(Iterator iterator) {
        super(new RangeIteratorAdapter(iterator));
    }

    /**
     * Creates an iterator for the given collection.
     *
     * @param collection collection of {@link Event}s
     */
    public EventIteratorAdapter(Collection collection) {
        super(new RangeIteratorAdapter(collection));
    }

    //-------------------------------------------------------< EventIterator >

    /**
     * Returns the next event.
     *
     * @return next event
     * @throws NoSuchElementException if there is no next event
     */
    public Event nextEvent() throws NoSuchElementException {
        return (Event) next();
    }

}
