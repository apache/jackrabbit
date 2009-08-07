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
package org.apache.jackrabbit.api.jsr283.observation;

import java.util.Map;
import java.util.Arrays;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import org.apache.jackrabbit.test.api.observation.EventResult;
import org.apache.jackrabbit.core.observation.ObservationManagerImpl;

/**
 * <code>AbstractObservationTest</code> is a base class with utility methods
 * for observation related tests.
 */
public abstract class AbstractObservationTest
        extends org.apache.jackrabbit.test.api.observation.AbstractObservationTest {

    /**
     * TODO: remove when JSR 283 is final
     */
    protected static final int NODE_MOVED = org.apache.jackrabbit.api.jsr283.observation.Event.NODE_MOVED;

    protected static final int ALL_TYPES = Event.NODE_ADDED | Event.NODE_REMOVED | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED | org.apache.jackrabbit.api.jsr283.observation.Event.NODE_MOVED;

    protected void setUserData(String userData) throws RepositoryException {
        // TODO: remove when JCR 2.0 is final
        ((ObservationManagerImpl) obsMgr).setUserData(userData);
    }

    protected static String getUserData(Event e) throws RepositoryException {
        // TODO: remove when JCR 2.0 is final
        return ((org.apache.jackrabbit.api.jsr283.observation.Event) e).getUserData();
    }

    protected String getIdentifier(Node node) throws RepositoryException {
        // TODO: remove when JSR 283 is final
        return ((org.apache.jackrabbit.api.jsr283.Node) node).getIdentifier();
    }

    protected String getIdentifier(Event event) throws RepositoryException {
        // TODO: remove when JSR 283 is final
        return ((org.apache.jackrabbit.api.jsr283.observation.Event) event).getIdentifier();
    }

    protected Map getInfo(Event event) throws RepositoryException {
        // TODO: remove when JSR 283 is final
        return ((org.apache.jackrabbit.api.jsr283.observation.Event) event).getInfo();
    }

    protected long getDate(Event event) throws RepositoryException {
        // TODO: remove when JSR 283 is final
        return ((org.apache.jackrabbit.api.jsr283.observation.Event) event).getDate();
    }

    /**
     * Returns the first event with the given <code>path</code>.
     *
     * @param events the events.
     * @param path   the path.
     * @return the event with the given <code>path</code> or {@link #fail()}s if
     *         no such event exists.
     * @throws RepositoryException if an error occurs while reading from the
     *                             repository.
     */
    protected Event getEventByPath(Event[] events, String path)
            throws RepositoryException {
        for (int i = 0; i < events.length; i++) {
            if (events[i].getPath().equals(path)) {
                return events[i];
            }
        }
        fail("no event with path: " + path + " in " + Arrays.asList(events));
        return null;
    }

    /**
     * Registers an event listener for the passed <code>eventTypes</code> and
     * calls the callable.
     *
     * @param call       the callable.
     * @param eventTypes the types of the events to listen for.
     * @return the events that were generated during execution of the callable.
     * @throws RepositoryException if an error occurs.
     */
    protected Event[] getEvents(Callable call, int eventTypes)
            throws RepositoryException {
        EventResult result = new EventResult(log);
        addEventListener(result, eventTypes);
        call.call();
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        removeEventListener(result);
        return events;
    }

    /**
     * Helper interface.
     */
    protected interface Callable {
        public void call() throws RepositoryException;
    }
}
