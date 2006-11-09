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
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.observation.ObservationConstants;
import org.w3c.dom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>EventImpl</code>...
 */
public class EventImpl implements Event, ObservationConstants {

    private static Logger log = LoggerFactory.getLogger(EventImpl.class);

    private final ItemId eventId;
    private final int type;
    private final Path eventPath;
    private final NodeId parentId;

    private final Element eventElement;

    public EventImpl(ItemId eventId, Path eventPath, NodeId parentId, int eventType,
                     Element eventElement) {
        this.eventId = eventId;
        this.eventPath = eventPath;
        this.parentId = parentId;
        type = getSpiEventType(eventType);

        this.eventElement = eventElement;
    }

    public int getType() {
        return type;
    }

    public Path getQPath() {
        return eventPath;
    }

    public ItemId getItemId() {
        return eventId;
    }

    public NodeId getParentId() {
        return parentId;
    }

    public String getUUID() {
        return getItemId().getUUID();
    }

    public QName getPrimaryNodeTypeName() {
        // TODO not available from XML_EVENT element
        return null;
    }

    public QName[] getMixinTypeNames() {
        // TODO not available from XML_EVENT element
        return new QName[0];
    }

    public String getUserID() {
        return DomUtil.getChildTextTrim(eventElement, XML_EVENTUSERID, NAMESPACE);
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