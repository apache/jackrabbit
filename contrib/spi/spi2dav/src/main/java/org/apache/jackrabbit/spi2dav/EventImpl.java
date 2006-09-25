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
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.observation.ObservationConstants;
import org.apache.jackrabbit.webdav.observation.EventType;
import org.apache.jackrabbit.webdav.observation.DefaultEventType;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.jcr.observation.SubscriptionImpl;
import org.w3c.dom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;

/**
 * <code>EventImpl</code>...
 */
public class EventImpl implements Event, ObservationConstants {

    private static Logger log = LoggerFactory.getLogger(EventImpl.class);

    private final Element eventElement;
    private final int type;
    private final ItemId itemId;
    private final Path qPath;

    public EventImpl(Element eventElement, URIResolver uriResolver,
                     SessionInfo sessionInfo) throws RepositoryException, DavException {
        this.eventElement = eventElement;

        Element typeEl = DomUtil.getChildElement(eventElement, XML_EVENTTYPE, NAMESPACE);
        EventType[] et = DefaultEventType.createFromXml(typeEl);
        if (et.length == 0 || et.length > 1) {
            throw new IllegalArgumentException("Ambigous event type definition: expected one single eventtype.");
        }
        type = getSpiEventType(SubscriptionImpl.getJcrEventType(et[0]));

        String href = DomUtil.getChildTextTrim(eventElement, DavConstants.XML_HREF, DavConstants.NAMESPACE);
        if (type == Event.NODE_ADDED || type == Event.NODE_REMOVED) {
            itemId = uriResolver.getNodeId(href, sessionInfo);
        } else {
            itemId = uriResolver.getPropertyId(href, sessionInfo);
        }
        qPath = uriResolver.getQPath(href, sessionInfo);
    }

    public int getType() {
        return type;
    }

    public Path getQPath() {
        return qPath;
    }

    public ItemId getItemId() {
        return itemId;
    }

    public NodeId getParentId() {
        // TODO not available from XML_EVENT element
        return null;
    }

    public String getUUID() {
        return itemId.getUUID();
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