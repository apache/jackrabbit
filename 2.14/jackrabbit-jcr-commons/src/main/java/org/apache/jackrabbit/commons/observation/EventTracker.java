/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.jackrabbit.commons.observation;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

/**
 * Event decorator that tracks whether user and date information is being
 * accessed from external events or without checking for externality.
 *
 * @see ListenerTracker
 */
class EventTracker implements Event {

    /** The enclosing listener tracker */
    private final ListenerTracker listener;

    protected final Event event;

    protected final AtomicBoolean externalAccessed =
            new AtomicBoolean();

    public EventTracker(ListenerTracker listenerTracker, Event event) {
        listener = listenerTracker;
        this.event = event;
    }

    private void userInfoAccessed() {
        if (!externalAccessed.get()
                && !listener.userInfoAccessedWithoutExternalsCheck.getAndSet(true)) {
            listener.warn("Event listener " + listener + " is trying"
                    + " to access user information of event " + event
                    + " without checking whether the event is external.");
        }
        if (eventIsExternal()
                && !listener.userInfoAccessedFromExternalEvent.getAndSet(true)) {
            listener.warn("Event listener " + listener + " is trying"
                    + " to access user information of external event "
                    + event + ".");
        }
    }

    private void dateInfoAccessed() {
        if (!externalAccessed.get()
                && !listener.dateAccessedWithoutExternalsCheck.getAndSet(true)) {
            listener.warn("Event listener " + listener + " is trying"
                    + " to access date information of event " + event
                    + " without checking whether the event is external.");
        }
        if (eventIsExternal()
                && !listener.dateAccessedFromExternalEvent.getAndSet(true)) {
            listener.warn("Event listener " + listener + " is trying"
                    + " to access date information of external event "
                    + event + ".");
        }
    }

    protected boolean eventIsExternal() {
        return false;
    }

    @Override
    public String toString() {
        return event.toString();
    }

    @Override
    public int hashCode() {
        return event.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (other instanceof EventTracker) {
            return event.equals(other);
        } else {
            return false;
        }
    }

    //---------------------------------------------------------< Event >--

    @Override
    public int getType() {
        return event.getType();
    }

    @Override
    public String getPath() throws RepositoryException {
        return event.getPath();
    }

    @Override
    public String getUserID() {
        userInfoAccessed();
        return event.getUserID();
    }

    @Override
    public String getIdentifier() throws RepositoryException {
        return event.getIdentifier();
    }

    @Override
    public Map<?, ?> getInfo() throws RepositoryException {
        return event.getInfo();
    }

    @Override
    public String getUserData() throws RepositoryException {
        userInfoAccessed();
        return event.getUserData();
    }

    @Override
    public long getDate() throws RepositoryException {
        dateInfoAccessed();
        return event.getDate();
    }

}