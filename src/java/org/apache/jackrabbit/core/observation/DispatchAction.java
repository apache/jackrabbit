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
package org.apache.jackrabbit.core.observation;

import java.util.Collection;

/**
 * The <code>DispatchAction</code> class is a simple struct that defines what
 * <code>EventState</code>s should be dispatched to which
 * <code>EventConsumer</code>s.
 *
 * @author Marcel Reutegger
 * @version $Revision: 1.3 $, $Date: 2004/08/02 16:19:47 $
 */
class DispatchAction {

    /**
     * The collection of <code>EventState</code>s
     */
    private final EventStateCollection eventStates;

    /**
     * <code>EventStates</code> are dispatched to these
     * <code>EventConsumer</code>s.
     */
    private final Collection eventConsumers;

    /**
     * Creates a new <code>DispatchAction</code> struct with
     * <code>eventStates</code> and <code>eventConsumers</code>.
     */
    DispatchAction(EventStateCollection eventStates, Collection eventConsumers) {
	this.eventStates = eventStates;
	this.eventConsumers = eventConsumers;
    }

    /**
     * Returns a collection of {@link EventState}s to dispatch.
     * @return a collection of {@link EventState}s to dispatch.
     */
    EventStateCollection getEventStates() {
	return eventStates;
    }

    /**
     * Returns a <code>Collection</code> of {@link EventConsumer}s where
     * the events should be dispatched to.
     * @return a <code>Collection</code> of {@link EventConsumer}s.
     */
    Collection getEventConsumers() {
	return eventConsumers;
    }
}
