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
import java.util.Set;

import javax.jcr.RepositoryException;

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
     */
    public FilteredEventIterator(
            SessionImpl session, Iterator<?> eventStates,
            long timestamp, String userData,
            final EventFilter filter, final Set<?> denied) {
        super(new FilteredRangeIterator(eventStates, new Predicate() {
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

}
