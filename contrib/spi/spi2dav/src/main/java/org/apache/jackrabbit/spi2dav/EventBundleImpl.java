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
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.observation.ObservationConstants;
import org.w3c.dom.Element;

/**
 * <code>EventBundleImpl</code> implements a bundle of events. The individual
 * events can be retrieved when calling {@link #getEvents()}.
 */
class EventBundleImpl implements EventBundle, ObservationConstants {

    static final EventBundle EMPTY = new EventBundleImpl(null, null, null) {
        public EventIterator getEvents() {
            return IteratorHelper.EMPTY;
        }
    };

    private final Element eventBundleElement;

    private final URIResolver uriResolver;

    private final SessionInfo sessionInfo;

    private final boolean isLocal;

    /**
     * Creates a new event bundle.
     *
     * @param eventBundleElement
     * @param uriResolver
     * @param sessionInfo
     */
    EventBundleImpl(Element eventBundleElement,
                    URIResolver uriResolver,
                    SessionInfo sessionInfo) {
        this.eventBundleElement = eventBundleElement;
        this.uriResolver = uriResolver;
        this.sessionInfo = sessionInfo;
        String value = null;
        if (eventBundleElement != null) {
            value = DomUtil.getAttribute(eventBundleElement,
                        XML_EVENT_IS_LOCAL, NAMESPACE);
        }
        this.isLocal = (value != null) ? Boolean.valueOf(value).booleanValue() : false;
    }

    /**
     * @inheritDoc
     */
    public EventIterator getEvents() {
        return new EventIteratorImpl(eventBundleElement, uriResolver, sessionInfo);
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
