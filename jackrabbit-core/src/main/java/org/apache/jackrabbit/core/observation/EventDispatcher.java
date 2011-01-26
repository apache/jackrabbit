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

import org.apache.jackrabbit.core.state.ChangeLog;

/**
 * Defines an object that prepares and dispatches events. Made into an abstract
 * class rather than an interface in order not to exhibit internal methods
 * that should not be visible to everybody.
 */
abstract class EventDispatcher {

    /**
     * Gives this dispatcher the opportunity to prepare the events for
     * dispatching.
     *
     * @param events the {@link EventState}s to prepare.
     */
    abstract void prepareEvents(EventStateCollection events);

    /**
     * Prepares changes that involve deleted item states.
     *
     * @param events the event state collection.
     * @param changes the changes.
     */
    abstract void prepareDeleted(EventStateCollection events, ChangeLog changes);

    /**
     * Dispatches the {@link EventStateCollection events}.
     *
     * @param events the {@link EventState}s to dispatch.
     */
    abstract void dispatchEvents(EventStateCollection events);
}
