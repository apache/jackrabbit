/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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
package org.apache.jackrabbit.core.observation;

import org.apache.jackrabbit.core.SessionImpl;

import javax.jcr.RepositoryException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * This Class implements an observation dispatcher, that delegates events to
 * a set of underlaying dispatchers.
 */
public class DelegatingObservationDispatcher {

    /**
     * the set of dispatchers
     */
    private final HashSet dispatchers = new HashSet();

    /**
     * Adds a new observation factory to the set of dispatchers
     *
     * @param disp
     */
    public void addDispatcher(ObservationManagerFactory disp) {
        dispatchers.add(disp);
    }

    /**
     * Removes a observation factory from the set of dispatchers
     *
     * @param disp
     */
    public void removeDispatcher(ObservationManagerFactory disp) {
        dispatchers.remove(disp);
    }

    /**
     * Dispatchers a list of events to all registered dispatchers. A new
     * {@link EventStateCollection} is created for every dispatcher, fille with
     * the given event list and then dispatched.
     * 
     * @param eventList
     * @param session
     * @throws RepositoryException
     */
    public void dispatch(List eventList, SessionImpl session) throws RepositoryException {
        Iterator iter = dispatchers.iterator();
        while (iter.hasNext()) {
            ObservationManagerFactory fac = (ObservationManagerFactory) iter.next();
            EventStateCollection events = new EventStateCollection(fac, session);
            events.addAll(eventList);
            events.prepare();
            events.dispatch();
        }
    }
}
