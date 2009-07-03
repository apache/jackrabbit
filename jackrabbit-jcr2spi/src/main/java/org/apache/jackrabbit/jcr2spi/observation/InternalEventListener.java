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

import org.apache.jackrabbit.spi.EventBundle;
import org.apache.jackrabbit.spi.EventFilter;

import java.util.Collection;

/**
 * <code>InternalEventListener</code> receives changes as a result of a local
 * or an external modification.
 */
public interface InternalEventListener {

    /**
     * Gets called when an event occurs.
     *
     * @param eventBundle the event set received.
     */
    public void onEvent(EventBundle eventBundle);

    /**
     * Returns a collection of event filters which is in use by this event
     * listener. The event bundles delivered to {@link #onEvent} will be filtered
     * using the collection returned by this method. An event is included
     * in an event bundles if it is accepted by at least one of the filters
     * returned by this method.
     *
     * @return an unmodifiable collection of {@link EventFilter}s currently
     * in use by this event listener.
     */
    public Collection<EventFilter> getEventFilters();
}
