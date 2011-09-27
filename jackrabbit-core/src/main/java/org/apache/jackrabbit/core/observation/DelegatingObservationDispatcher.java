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

import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.spi.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;

/**
 * This Class implements an observation dispatcher, that delegates events to
 * a set of underlying dispatchers.
 */
public class DelegatingObservationDispatcher extends EventDispatcher {

    /**
     * Logger instance.
     */
    private static Logger log = LoggerFactory.getLogger(DelegatingObservationDispatcher.class);

    /**
     * the set of dispatchers
     */
    private final HashSet<ObservationDispatcher> dispatchers = new HashSet<ObservationDispatcher>();

    /**
     * Adds a new observation dispatcher to the set of dispatchers
     *
     * @param dispatcher observation dispatcher
     */
    public void addDispatcher(ObservationDispatcher dispatcher) {
        synchronized (dispatchers) {
            dispatchers.add(dispatcher);
        }
    }

    /**
     * Removes a observation dispatcher from the set of dispatchers
     *
     * @param dispatcher observation dispatcher
     */
    public void removeDispatcher(ObservationDispatcher dispatcher) {
        synchronized (dispatchers) {
            dispatchers.remove(dispatcher);
        }
    }

    /**
     * Creates an <code>EventStateCollection</code> tied to the session
     * given as argument.
     *
     * @param session event source
     * @param pathPrefix event path prefix
     * @return new <code>EventStateCollection</code> instance
     */
    public EventStateCollection createEventStateCollection(
            SessionImpl session, Path pathPrefix) {
        return new EventStateCollection(this, session, pathPrefix);
    }

    //------------------------------------------------------< EventDispatcher >

    /**
     * {@inheritDoc}
     */
    void prepareEvents(EventStateCollection events) {
        // events will get prepared on dispatch
    }

    /**
     * {@inheritDoc}
     */
    void prepareDeleted(EventStateCollection events, ChangeLog changes) {
        // events will get prepared on dispatch
    }

    /**
     * {@inheritDoc}
     */
    void dispatchEvents(EventStateCollection events) {
        dispatch(events.getEvents(), events.getSession(),
                events.getPathPrefix(), events.getUserData());
    }

    /**
     * Dispatchers a list of events to all registered dispatchers. A new
     * {@link EventStateCollection} is created for every dispatcher, fille with
     * the given event list and then dispatched.
     *
     * @param eventList list of events
     * @param session current session
     * @param pathPrefix event path prefix
     * @param userData the user data
     */
    public void dispatch(List<EventState> eventList, SessionImpl session,
                         Path pathPrefix, String userData) {
        ObservationDispatcher[] disp;
        synchronized (dispatchers) {
            disp = (ObservationDispatcher[]) dispatchers.toArray(
                    new ObservationDispatcher[dispatchers.size()]);
        }
        for (int i = 0; i < disp.length; i++) {
            EventStateCollection events =
                    new EventStateCollection(disp[i], session, pathPrefix);
            events.setUserData(userData);
            try {
                events.addAll(eventList);
                events.prepare();
                events.dispatch();
            } catch (Exception e) {
                log.error("Error while dispatching events.", e);
            }
        }
    }
}
