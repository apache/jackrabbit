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
package org.apache.jackrabbit.spi2jcr;

import org.apache.jackrabbit.spi.EventBundle;
import org.apache.jackrabbit.spi.EventIterator;

import java.util.Collection;

/**
 * <code>EventBundleImpl</code> implements a {@link EventBundle}.
 */
class EventBundleImpl implements EventBundle {

    /**
     * Indicates if this bundle was created due to a local change.
     */
    private final boolean isLocal;

    /**
     * The bundle id.
     */
    private final String bundleId;

    /**
     * The events in this bundle.
     */
    private final Collection events;

    /**
     * Creates a new event bundle with <code>events</code>.
     *
     * @param events   the events for this bundle.
     * @param isLocal  if this events were created due to a local change.
     * @param bundleId the bundle id.
     */
    EventBundleImpl(Collection events, boolean isLocal, String bundleId) {
        this.events = events;
        this.isLocal = isLocal;
        this.bundleId = bundleId;
    }

    /**
     * {@inheritDoc}
     */
    public EventIterator getEvents() {
        return new IteratorHelper(events);
    }

    /**
     * {@inheritDoc}
     */
    public String getBundleId() {
        return bundleId;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLocal() {
        return isLocal;
    }
}
