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
package org.apache.jackrabbit.spi2dav;

import org.apache.jackrabbit.spi.EventBundle;
import org.apache.jackrabbit.spi.EventIterator;
import org.apache.jackrabbit.webdav.observation.ObservationConstants;

import java.util.Collection;

/**
 * <code>EventBundleImpl</code> implements a bundle of events. The individual
 * events can be retrieved when calling {@link #getEvents()}.
 */
class EventBundleImpl implements EventBundle, ObservationConstants {

    static final EventBundle EMPTY = new EventBundleImpl();

    private final Collection events;
    private final boolean isLocal;

    private EventBundleImpl() {
        events = null;
        isLocal = false;
    }
    /**
     * Creates a new event bundle.
     *
     * @param events
     * @param isLocal
     */
    EventBundleImpl(Collection events, boolean isLocal) {
        this.events = events;
        this.isLocal = isLocal;
    }

    /**
     * @inheritDoc
     */
    public EventIterator getEvents() {
        if (events == null || events.isEmpty()) {
            return IteratorHelper.EMPTY;
        } else {
            return new EventIteratorImpl(events);
        }
    }

    /**
     * @inheritDoc
     * <p/>
     * TODO implement
     */
    public String getBundleId() {
        return null;
    }

    /**
     * @inheritDoc
     */
    public boolean isLocal() {
        return isLocal;
    }
}
