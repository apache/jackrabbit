/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  The ASF licenses this file to You
* under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License.
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
package org.apache.jackrabbit.spi2dav;

import org.apache.jackrabbit.spi.EventIterator;
import org.apache.jackrabbit.spi.Event;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.NoSuchElementException;
import java.util.Collection;
import java.util.Iterator;

/**
 * <code>EventIteratorImpl</code>...
 */
public class EventIteratorImpl implements EventIterator {

    private static Logger log = LoggerFactory.getLogger(EventIteratorImpl.class);

    private Iterator eventIterator;

    private Event next;
    private long pos;

    public EventIteratorImpl(Collection events) {
        this.eventIterator = events.iterator();
        retrieveNextEvent();
    }

    public Event nextEvent() {
        if (next == null) {
            throw new NoSuchElementException();
        }
        Event event = next;
        retrieveNextEvent();
        pos++;
        return event;
    }

    public void skip(long skipNum) {
        while (skipNum-- > 0) {
            nextEvent();
        }
    }

    public long getSize() {
        return -1; // size undefined
    }

    public long getPosition() {
        return pos;
    }

    public void remove() {
        throw new UnsupportedOperationException("Remove not implemented.");
    }

    public boolean hasNext() {
        return next != null;
    }

    public Object next() {
        return nextEvent();
    }

    //------------------------------------------------------------< private >---
    private void retrieveNextEvent() {
        next = null;
        if (eventIterator != null) {
            while (next == null && eventIterator.hasNext()) {
                Object o = eventIterator.next();
                if (o instanceof Event) {
                    next = (Event)o;
                }
            }
        }
    }
}