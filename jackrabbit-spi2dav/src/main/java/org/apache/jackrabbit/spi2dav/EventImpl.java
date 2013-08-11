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
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.value.ValueFormat;
import org.apache.jackrabbit.webdav.observation.ObservationConstants;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.xml.ElementIterator;
import org.apache.jackrabbit.webdav.xml.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <code>EventImpl</code>...
 */
public class EventImpl
        extends org.apache.jackrabbit.spi.commons.EventImpl
        implements ObservationConstants {

    private static Logger log = LoggerFactory.getLogger(EventImpl.class);

    private static final NameFactory N_FACTORY = NameFactoryImpl.getInstance();

    public EventImpl(ItemId eventId, Path eventPath, NodeId parentId, int eventType, String userId,
            Element eventElement, NamePathResolver resolver, QValueFactory qvFactory) throws NamespaceException,
            IllegalNameException {
        super(getSpiEventType(eventType), eventPath, eventId, parentId, getNameSafe(
                DomUtil.getChildTextTrim(eventElement, N_EVENTPRIMARYNODETYPE), resolver), getNames(
                DomUtil.getChildren(eventElement, N_EVENTMIXINNODETYPE), resolver), userId, DomUtil.getChildTextTrim(
                eventElement, N_EVENTUSERDATA), Long.parseLong(DomUtil.getChildTextTrim(eventElement, N_EVENTDATE)),
                getEventInfo(DomUtil.getChildElement(eventElement, N_EVENTINFO), resolver, qvFactory));
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
            case javax.jcr.observation.Event.NODE_MOVED:
                return Event.NODE_MOVED;
            case javax.jcr.observation.Event.PERSIST:
                return Event.PERSIST;
            default:
                throw new IllegalArgumentException("Invalid event type: " + jcrEventType);
        }
    }

    private static Map<Name, QValue> getEventInfo(Element infoElement,
                                                  NamePathResolver resolver,
                                                  QValueFactory qvFactory) {
        if (infoElement == null) {
            return Collections.emptyMap();
        }

        Map<Name, QValue> info = new HashMap<Name, QValue>();
        ElementIterator it = DomUtil.getChildren(infoElement);
        while (it.hasNext()) {
            Element el = it.nextElement();
            String uri = el.getNamespaceURI();
            if (uri == null) {
                uri = Namespace.EMPTY_NAMESPACE.getURI();
            }
            String localName = el.getLocalName();
            String value = DomUtil.getTextTrim(el);
            try {
                Name n = N_FACTORY.create(uri, localName);
                QValue qv = null;
                if (value != null) {
                    qv = ValueFormat.getQValue(value, PropertyType.PATH, resolver, qvFactory);
                }
                info.put(n, qv);
            } catch (RepositoryException e) {
                log.error("Internal Error: {}", e.getMessage());
            }
        }
        return info;
    }

    private static Name getNameSafe(String name, NamePathResolver resolver) throws IllegalNameException, NamespaceException {
        if (name == null) {
            return null;
        }
        else {
            return resolver.getQName(name);
        }
    }

    private static Name[] getNames(ElementIterator elements,
            NamePathResolver resolver) {

        List<Name> results = Collections.emptyList();

        while (elements.hasNext()) {
            String rawname = DomUtil.getText(elements.nextElement());
            Name name = null;

            try {
                name = resolver.getQName(rawname);

                if (results.size() == 0) {
                    results = Collections.singletonList(name);
                } else if (results.size() == 1) {
                    results = new ArrayList<Name>(results);
                    results.add(name);
                } else {
                    results.add(name);
                }
            } catch (Exception ex) {
                log.error("Exception converting name " + rawname, ex);
            }
        }

        return results.toArray(new Name[results.size()]);
    }
}
