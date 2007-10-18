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

import org.apache.jackrabbit.spi.Event;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.observation.ObservationConstants;
import org.w3c.dom.Element;

/**
 * <code>EventImpl</code>...
 */
public class EventImpl
        extends org.apache.jackrabbit.spi.commons.EventImpl
        implements ObservationConstants {

    public EventImpl(ItemId eventId, Path eventPath, NodeId parentId, int eventType,
                     Element eventElement) {
        super(getSpiEventType(eventType), eventPath, eventId, parentId,
                null, // TODO not available from XML_EVENT element
                null, // TODO not available from XML_EVENT element
                DomUtil.getChildTextTrim(eventElement, XML_EVENTUSERID, NAMESPACE));
    }

    //--------------------------------------------------------------------------
    private static int getSpiEventType(int jcrEventType) {
        switch (jcrEventType) {
            case javax.jcr.observation.Event.NODE_ADDED:
                return Event.NODE_ADDED;
            case javax.jcr.observation.Event.NODE_REMOVED:
                return Event.NODE_REMOVED;
            case javax.jcr.observation.Event.PROPERTY_ADDED:
                return Event.PROPERTY_ADDED;
            case javax.jcr.observation.Event.PROPERTY_CHANGED:
                return Event.PROPERTY_CHANGED;
            case javax.jcr.observation.Event.PROPERTY_REMOVED:
                return Event.PROPERTY_REMOVED;
            default:
                throw new IllegalArgumentException("Invalid event type: " + jcrEventType);
        }
    }
}