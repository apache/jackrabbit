/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
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
package org.apache.jackrabbit.webdav.jcr.observation;

import org.apache.jackrabbit.uuid.UUID;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.observation.EventBundle;
import org.apache.jackrabbit.webdav.observation.EventDiscovery;
import org.apache.jackrabbit.webdav.observation.EventType;
import org.apache.jackrabbit.webdav.observation.Filter;
import org.apache.jackrabbit.webdav.observation.ObservationConstants;
import org.apache.jackrabbit.webdav.observation.ObservationResource;
import org.apache.jackrabbit.webdav.observation.Subscription;
import org.apache.jackrabbit.webdav.observation.SubscriptionInfo;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The <code>Subscription</code> class encapsulates a single subscription with
 * the following responsibilities:<ul>
 * <li>Providing access to the subscription info,</li>
 * <li>Recording events this subscription is interested in,</li>
 * <li>Providing access to the events.</li>
 * </ul>
 */
public class SubscriptionImpl implements Subscription, ObservationConstants, EventListener {

    private static Logger log = Logger.getLogger(SubscriptionImpl.class);
    private static final long DEFAULT_TIMEOUT = 300000; // 5 minutes

    /**
     * Element representing the 'nodeadded' event type.
     * @see javax.jcr.observation.Event#NODE_ADDED
     */
    private static final String EVENT_NODEADDED = "nodeadded";

    /**
     * Element representing the 'noderemoved' event type.
     * @see javax.jcr.observation.Event#NODE_REMOVED
     */
    private static final String EVENT_NODEREMOVED = "noderemoved";

    /**
     * Element representing the 'propertyadded' event type.
     * @see javax.jcr.observation.Event#PROPERTY_ADDED
     */
    private static final String EVENT_PROPERTYADDED = "propertyadded";

    /**
     * Element representing the 'propertyremoved' event type.
     * @see javax.jcr.observation.Event#PROPERTY_REMOVED
     */
    private static final String EVENT_PROPERTYREMOVED = "propertyremoved";

    /**
     * Element representing the 'propertychanged' event type.
     * @see javax.jcr.observation.Event#PROPERTY_CHANGED
     */
    private static final String EVENT_PROPERTYCHANGED = "propertychanged";

    private SubscriptionInfo info;
    private long expirationTime;

    private final DavResourceLocator locator;
    private final String subscriptionId = UUID.randomUUID().toString();
    private final List eventBundles = new ArrayList();

    /**
     * Create a new <code>Subscription</code> with the given {@link SubscriptionInfo}
     * and {@link org.apache.jackrabbit.webdav.observation.ObservationResource resource}.
     *
     * @param info
     * @param resource
     */
    public SubscriptionImpl(SubscriptionInfo info, ObservationResource resource) {
        setInfo(info);
        locator = resource.getLocator();
    }

    /**
     * Returns the id of this subscription.
     *
     * @return subscriptionId
     */
    public String getSubscriptionId() {
        return subscriptionId;
    }

    /**
     * Return the Xml representation of this <code>Subscription</code> as required
     * for the {@link org.apache.jackrabbit.webdav.observation.SubscriptionDiscovery}
     * webdav property that in included in the response body of a sucessful SUBSCRIBE
     * request or as part of a PROPFIND response.
     *
     * @return Xml representation
     * @see org.apache.jackrabbit.webdav.xml.XmlSerializable#toXml(Document)
     * @param document
     */
    public Element toXml(Document document) {
        Element subscr = DomUtil.createElement(document, XML_SUBSCRIPTION, NAMESPACE);

        subscr.appendChild(info.toXml(document));
        subscr.appendChild(DomUtil.depthToXml(info.isDeep(), document));
        subscr.appendChild(DomUtil.timeoutToXml(info.getTimeOut(), document));

        Element id = DomUtil.addChildElement(subscr, XML_SUBSCRIPTIONID, NAMESPACE);
        id.appendChild(DomUtil.hrefToXml(subscriptionId, document));
        return subscr;
    }

    //--------------------------------------------< implementation specific >---
    /**
     * Modify the {@link SubscriptionInfo} for this subscription.
     *
     * @param info
     */
    void setInfo(SubscriptionInfo info) {
        this.info = info;
        // validate the timeout and adjust value, if it is invalid or missing
        long timeout = info.getTimeOut();
        if (timeout <= 0) {
            timeout = DEFAULT_TIMEOUT;
        }
        expirationTime = System.currentTimeMillis() + timeout;
    }

    /**
     * @return JCR compliant integer representation of the event types defined
     * for this {@link SubscriptionInfo}.
     */
    int getEventTypes() throws DavException {
        EventType[] eventTypes = info.getEventTypes();
        int events = 0;
        for (int i = 0; i < eventTypes.length; i++) {
            events |= getEventType(eventTypes[i].getName());
        }
        return events;
    }

    /**
     * @return a String array with size > 0 or <code>null</code>
     */
    String[] getUuidFilters() {
        return getFilterValues(XML_UUID);
    }

    /**
     * @return a String array with size > 0 or <code>null</code>
     */
    String[] getNodetypeNameFilters() {
        return getFilterValues(XML_NODETYPE_NAME);
    }

    private String[] getFilterValues(String filterLocalName) {
        Filter[] filters = info.getFilters(filterLocalName, NAMESPACE);
        List values = new ArrayList();
        for (int i = 0; i < filters.length; i++) {
            String val = filters[i].getValue();
            if (val != null) {
                values.add(val);
            }
        }
        return (values.size() > 0) ? (String[])values.toArray(new String[values.size()]) : null;
    }

    /**
     *
     * @return true if a {@link ObservationConstants#XML_NOLOCAL} element
     * is present in the {@link SubscriptionInfo}.
     */
    boolean isNoLocal() {
        return info.isNoLocal();
    }

    /**
     * @return true if this subscription is intended to be deep.
     */
    boolean isDeep() {
        return info.isDeep();
    }

    /**
     * @return the locator of the {@link ObservationResource resource} this
     * <code>Subscription</code> was requested for.
     */
    DavResourceLocator getLocator() {
        return locator;
    }

    /**
     * Returns true if this <code>Subscription</code> matches the given
     * resource.
     *
     * @param resource
     * @return true if this <code>Subscription</code> matches the given
     * resource.
     */
    boolean isSubscribedToResource(ObservationResource resource) {
        return locator.equals(resource.getLocator());
    }

    /**
     * Returns true if this <code>Subscription</code> is expired and therefore
     * stopped recording events.
     *
     * @return true if this <code>Subscription</code> is expired
     */
    boolean isExpired() {
        return System.currentTimeMillis() > expirationTime;
    }

    /**
     * Returns a {@link org.apache.jackrabbit.webdav.observation.EventDiscovery} object listing all events that were
     * recorded since the last call to this method and clears the list of event
     * bundles.
     *
     * @return object listing all events that were recorded.
     * @see #onEvent(EventIterator)
     */
    synchronized EventDiscovery discoverEvents() {
        EventDiscovery ed = new EventDiscovery();
        Iterator it = eventBundles.iterator();
        while (it.hasNext()) {
            EventBundle eb = (EventBundle) it.next();
            ed.addEventBundle(eb);
        }
        // clear list
        eventBundles.clear();
        return ed;
    }

    //--------------------------------------------< EventListener interface >---
    /**
     * Records the events passed as a new event bundle in order to make them
     * available with the next {@link #discoverEvents()} request.
     *
     * @param events to be recorded.
     * @see EventListener#onEvent(EventIterator)
     * @see #discoverEvents()
     */
    public synchronized void onEvent(EventIterator events) {
        // TODO: correct not to accept events after expiration? without unsubscribing?
        if (!isExpired()) {
            eventBundles.add(new EventBundleImpl(events));
        }
    }

    //--------------------------------------------------------------------------
    /**
     * Static utility method in order to convert the types defined by
     * {@link javax.jcr.observation.Event} into their Xml representation.
     *
     * @param eventType The jcr event type
     * @return Xml representation of the event type.
     */
    private static String getEventName(int eventType) {
        String eventName;
        switch (eventType) {
            case Event.NODE_ADDED:
                eventName = EVENT_NODEADDED;
                break;
            case Event.NODE_REMOVED:
                eventName = EVENT_NODEREMOVED;
                break;
            case Event.PROPERTY_ADDED:
                eventName = EVENT_PROPERTYADDED;
                break;
            case Event.PROPERTY_CHANGED:
                eventName = EVENT_PROPERTYCHANGED;
                break;
            default:
                //Event.PROPERTY_REMOVED:
                eventName = EVENT_PROPERTYREMOVED;
                break;
        }
        return eventName;
    }

    /**
     * Static utility method to convert an event type name as present in the
     * Xml request body into the corresponding constant defined by
     * {@link javax.jcr.observation.Event}.
     *
     * @param eventName
     * @return event type as defined by {@link javax.jcr.observation.Event}.
     * @throws DavException if the given element cannot be translated
     * to any of the events defined by {@link javax.jcr.observation.Event}.
     */
    private static int getEventType(String eventName) throws DavException {
        int eType;
        if (EVENT_NODEADDED.equals(eventName)) {
            eType = Event.NODE_ADDED;
        } else if (EVENT_NODEREMOVED.equals(eventName)) {
            eType = Event.NODE_REMOVED;
        } else if (EVENT_PROPERTYADDED.equals(eventName)) {
            eType = Event.PROPERTY_ADDED;
        } else if (EVENT_PROPERTYCHANGED.equals(eventName)) {
            eType = Event.PROPERTY_CHANGED;
        } else if (EVENT_PROPERTYREMOVED.equals(eventName)) {
            eType = Event.PROPERTY_REMOVED;
        } else {
            throw new DavException(DavServletResponse.SC_UNPROCESSABLE_ENTITY, "Invalid event type: "+eventName);
        }
        return eType;
    }

    /**
     * Inner class <code>EventBundle</code> encapsulats an event bundle as
     * recorded {@link SubscriptionImpl#onEvent(EventIterator) on event} and
     * provides the possibility to retrieve the Xml representation of the
     * bundle and the events included in order to respond to a POLL request.
     *
     * @see SubscriptionImpl#discoverEvents()
     */
    private class EventBundleImpl implements EventBundle {

        private final EventIterator events;

        private EventBundleImpl(EventIterator events) {
            this.events = events;
        }

        public Element toXml(Document document) {
            Element bundle = DomUtil.createElement(document, XML_EVENTBUNDLE, NAMESPACE);
            while (events.hasNext()) {
                Event event = events.nextEvent();
                Element eventElem = DomUtil.addChildElement(bundle, XML_EVENT, NAMESPACE);
                // href
                String eHref = "";
                try {
                    boolean isCollection = (event.getType() == Event.NODE_ADDED || event.getType() == Event.NODE_REMOVED);
                    eHref = locator.getFactory().createResourceLocator(locator.getPrefix(), locator.getWorkspacePath(), event.getPath(), false).getHref(isCollection);
                } catch (RepositoryException e) {
                    // should not occur....
                    log.error(e.getMessage());
                }
                eventElem.appendChild(DomUtil.hrefToXml(eHref, document));
                // eventtype
                Element eType = DomUtil.addChildElement(eventElem, XML_EVENTTYPE, NAMESPACE);
                DomUtil.addChildElement(eType, getEventName(event.getType()), NAMESPACE);
                // user id
                DomUtil.addChildElement(eventElem, XML_EVENTUSERID, NAMESPACE, event.getUserID());
            }
            return bundle;
        }

    }
}