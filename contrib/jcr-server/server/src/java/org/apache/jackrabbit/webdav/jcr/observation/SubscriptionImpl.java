/*
 * Copyright 2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.observation.*;
import org.apache.jackrabbit.webdav.util.XmlUtil;
import org.apache.jackrabbit.uuid.UUID;
import org.jdom.Element;

import javax.jcr.observation.*;
import javax.jcr.observation.EventListener;
import javax.jcr.RepositoryException;
import java.util.*;

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

    private SubscriptionInfo info;

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
     * for the {@link org.apache.jackrabbit.webdav.observation.SubscriptionDiscovery} webdav property that in included
     * in the response body of a sucessful SUBSCRIBE request or as part of a
     * PROPFIND response.
     *
     * @return Xml representation
     */
    public Element toXml() {
        Element subscr = new Element(XML_SUBSCRIPTION, NAMESPACE);
        Element[] elems = info.toXml();
        for (int i = 0; i < elems.length; i++) {
            subscr.addContent(elems[i]);
        }

        Element id = new Element(XML_SUBSCRIPTIONID);
        id.addContent(XmlUtil.hrefToXml(subscriptionId));
        subscr.addContent(id);
        return subscr;
    }

    /**
     * Modify the {@link SubscriptionInfo} for this subscription.
     *
     * @param info
     */
    void setInfo(SubscriptionInfo info) {
        this.info = info;
        // validate the timeout and adjust value, if it is invalid or missing
        long timeout = info.getTimeOut();
        if (timeout == DavConstants.UNDEFINED_TIMEOUT) {
            info.setTimeOut(DEFAULT_TIMEOUT);
        }
    }

    /**
     * @return JCR compliant integer representation of the event types defined
     * for this {@link SubscriptionInfo}.
     */
    int getEventTypes() {
        Iterator xmlTypes = info.getEventTypes().iterator();
        int eventTypes = 0;
        while (xmlTypes.hasNext()) {
            eventTypes |= nametoTypeConstant(((Element)xmlTypes.next()).getName());
        }
        return eventTypes;
    }

    /**
     * @return a String array with size > 0 or <code>null</code>
     */
    String[] getUuidFilters() {
        return info.getFilters(XML_UUID);
    }

    /**
     * @return a String array with size > 0 or <code>null</code>
     */
    String[] getNodetypeNameFilters() {
        return info.getFilters(XML_NODETYPE_NAME);
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
        return System.currentTimeMillis() > info.getTimeOut() + System.currentTimeMillis();
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
            ed.addEventBundle(eb.toXml());
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
            eventBundles.add(new EventBundle(events));
        }
    }

    //--------------------------------------------------------------------------
    /**
     * Static utility method in order to convert the types defined by
     * {@link javax.jcr.observation.Event} into their Xml representation.
     *
     * @param jcrEventType
     * @return Xml representation of the event type.
     */
    static Element typeConstantToXml(int jcrEventType) {
        String eventName;
        switch (jcrEventType) {
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
        return new Element(eventName, NAMESPACE);
    }

    /**
     * Static utility method to convert an event type name as present in the
     * Xml request body into the corresponding constant defined by
     * {@link javax.jcr.observation.Event}.
     *
     * @param eventTypeName
     * @return event type as defined by {@link javax.jcr.observation.Event}.
     * @throws IllegalArgumentException if the given element cannot be translated
     * to any of the events defined by {@link javax.jcr.observation.Event}.
     */
    static int nametoTypeConstant(String eventTypeName) {
        int eType;
        if (EVENT_NODEADDED.equals(eventTypeName)) {
            eType = Event.NODE_ADDED;
        } else if (EVENT_NODEREMOVED.equals(eventTypeName)) {
            eType = Event.NODE_REMOVED;
        } else if (EVENT_PROPERTYADDED.equals(eventTypeName)) {
            eType = Event.PROPERTY_ADDED;
        } else if (EVENT_PROPERTYCHANGED.equals(eventTypeName)) {
            eType = Event.PROPERTY_CHANGED;
        } else if (EVENT_PROPERTYREMOVED.equals(eventTypeName)) {
            eType = Event.PROPERTY_REMOVED;
        } else {
            throw new IllegalArgumentException("Invalid event type: "+eventTypeName);
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
    private class EventBundle {

        private final EventIterator events;

        private EventBundle(EventIterator events) {
            this.events = events;
        }

        private Element toXml() {
            Element bundle = new Element(XML_EVENTBUNDLE, NAMESPACE);
            while (events.hasNext()) {
                Event event = events.nextEvent();

                Element eventElem = new Element(XML_EVENT, NAMESPACE);
                // href
                String eHref = "";
                try {
                    boolean isCollection = (event.getType() == Event.NODE_ADDED || event.getType() == Event.NODE_REMOVED);
                    eHref = locator.getFactory().createResourceLocator(locator.getPrefix(), locator.getWorkspacePath(), event.getPath()).getHref(isCollection);
                } catch (RepositoryException e) {
                    // should not occur....
                    log.error(e.getMessage());
                }
                eventElem.addContent(XmlUtil.hrefToXml(eHref));
                // eventtype
                Element eType = new Element(XML_EVENTTYPE, NAMESPACE).addContent(typeConstantToXml(event.getType()));
                eventElem.addContent(eType);
                // user id
                Element eUserId = new Element(XML_EVENTUSERID, NAMESPACE).setText(event.getUserID());
                eventElem.addContent(eUserId);
            }
            return bundle;
        }
    }
}