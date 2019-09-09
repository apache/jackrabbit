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

import java.util.List;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import javax.jcr.observation.EventJournal;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.Event;
import org.apache.jackrabbit.spi.EventFilter;
import org.apache.jackrabbit.spi.EventBundle;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.jcr2spi.WorkspaceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>EventJournalImpl</code> implement the JSR 283 event journal over SPI.
 */
public class EventJournalImpl implements EventJournal {

    /**
     * The logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(EventJournalImpl.class);

    private final WorkspaceManager wspMgr;

    private final EventFilter filter;

    private final NamePathResolver resolver;

    private List<Event> buffer = new LinkedList<Event>();

    private long lastTimestamp = 0;

    /**
     * Current position.
     */
    private long position = 0;

    public EventJournalImpl(WorkspaceManager wspMgr,
                            EventFilter filter,
                            NamePathResolver resolver) {
        this.wspMgr = wspMgr;
        this.filter = filter;
        this.resolver = resolver;
    }

    /**
     * {@inheritDoc}
     */
    public void skipTo(long date) {
        // first try to skip in buffer
        while (!buffer.isEmpty()) {
            long eDate;
            try {
                eDate = buffer.get(0).getDate();
            } catch (RepositoryException e) {
                eDate = 0;
            }
            if (eDate <= date) {
                buffer.remove(0);
            } else {
                return;
            }
        }
        // if we get here then we need to refill the buffer after
        // the given date
        lastTimestamp = date;
        refill();
    }

    /**
     * {@inheritDoc}
     */
    public javax.jcr.observation.Event nextEvent() {
        return (javax.jcr.observation.Event) next();
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
     * {@inheritDoc}
     */
    public long getSize() {
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    public long getPosition() {
        return position;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasNext() {
        if (buffer.isEmpty()) {
            refill();
        }
        return !buffer.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    public Object next() {
        if (hasNext()) {
            position++;
            return new EventImpl(buffer.remove(0),
                    resolver, wspMgr.getIdFactory());
        } else {
            throw new NoSuchElementException();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }

    //----------------------------< internal >----------------------------------

    private void refill() {
        try {
            EventBundle bundle = wspMgr.getEvents(filter, lastTimestamp);
            for (Event e : bundle) {
                buffer.add(e);
                lastTimestamp = e.getDate();
            }
        } catch (RepositoryException e) {
            log.warn("Exception while refilling event journal buffer", e);
        }
    }
}
