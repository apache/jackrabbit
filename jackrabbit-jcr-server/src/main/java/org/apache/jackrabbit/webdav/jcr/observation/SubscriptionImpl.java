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
package org.apache.jackrabbit.webdav.jcr.observation;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.transaction.TransactionResource;
import org.apache.jackrabbit.webdav.jcr.transaction.TransactionListener;
import org.apache.jackrabbit.webdav.jcr.JcrDavSession;
import org.apache.jackrabbit.webdav.jcr.JcrDavException;
import org.apache.jackrabbit.webdav.observation.EventBundle;
import org.apache.jackrabbit.webdav.observation.EventDiscovery;
import org.apache.jackrabbit.webdav.observation.EventType;
import org.apache.jackrabbit.webdav.observation.Filter;
import org.apache.jackrabbit.webdav.observation.ObservationConstants;
import org.apache.jackrabbit.webdav.observation.ObservationResource;
import org.apache.jackrabbit.webdav.observation.Subscription;
import org.apache.jackrabbit.webdav.observation.SubscriptionInfo;
import org.apache.jackrabbit.webdav.observation.DefaultEventType;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.xml.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The <code>Subscription</code> class encapsulates a single subscription with
 * the following responsibilities:<ul>
 * <li>Providing access to the subscription info,</li>
 * <li>Recording events this subscription is interested in,</li>
 * <li>Providing access to the events.</li>
 * </ul>
 */
public class SubscriptionImpl implements Subscription, ObservationConstants, EventListener {

    private static Logger log = LoggerFactory.getLogger(SubscriptionImpl.class);
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

    /**
     * Element representing the 'nodemoved' event type.
     * @see javax.jcr.observation.Event#NODE_MOVED
     */
    private static final String EVENT_NODEMOVED = "nodemoved";

    /**
     * Element representing the 'persist' event type.
     * @see javax.jcr.observation.Event#PERSIST
     */
    private static final String EVENT_PERSIST = "persist";

    private SubscriptionInfo info;
    private long expirationTime;

    private final DavResourceLocator locator;
    private final String subscriptionId = UUID.randomUUID().toString();
    private final List eventBundles = new ArrayList();
    private final ObservationManager obsMgr;

    /**
     * Create a new <code>Subscription</code> with the given {@link SubscriptionInfo}
     * and {@link org.apache.jackrabbit.webdav.observation.ObservationResource resource}.
     *
     * @param info
     * @param resource
     * @throws DavException if resource is not based on a JCR repository or
     * the repository does not support observation.
     */
    public SubscriptionImpl(SubscriptionInfo info, ObservationResource resource)
            throws DavException {
        setInfo(info);
        locator = resource.getLocator();
        Session s = JcrDavSession.getRepositorySession(resource.getSession());
        try {
            obsMgr = s.getWorkspace().getObservationManager();
        } catch (RepositoryException e) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        }
    }

    //-------------------------------------------------------< Subscription >---
    /**
     * Returns the id of this subscription.
     *
     * @return subscriptionId
     */
    public String getSubscriptionId() {
        return subscriptionId;
    }

    //----------------------------------------------------< XmlSerializable >---
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

        if (getSubscriptionId() != null) {
            Element id = DomUtil.addChildElement(subscr, XML_SUBSCRIPTIONID, NAMESPACE);
            id.appendChild(DomUtil.hrefToXml(getSubscriptionId(), document));
        }
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
    int getJcrEventTypes() throws DavException {
        EventType[] eventTypes = info.getEventTypes();
        int events = 0;
        for (int i = 0; i < eventTypes.length; i++) {
            events |= getJcrEventType(eventTypes[i]);
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
        return locator.getResourcePath().equals(resource.getResourcePath());
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
     * @param timeout time in milliseconds to wait at most for events if none
     *                are present currently.
     * @return object listing all events that were recorded.
     * @see #onEvent(EventIterator)
     */
    synchronized EventDiscovery discoverEvents(long timeout) {
        EventDiscovery ed = new EventDiscovery();
        if (eventBundles.isEmpty() && timeout > 0) {
            try {
                wait(timeout);
            } catch (InterruptedException e) {
                // continue and possibly return empty event discovery
            }
        }
        Iterator it = eventBundles.iterator();
        while (it.hasNext()) {
            EventBundle eb = (EventBundle) it.next();
            ed.addEventBundle(eb);
        }
        // clear list
        eventBundles.clear();
        return ed;
    }

    /**
     * Creates a new transaction listener for the scope of a transaction
     * commit (save call).
     * @return a transaction listener for this subscription.
     */
    TransactionListener createTransactionListener() {
        if (info.isNoLocal()) {
            // a subscription which is not interested in local changes does
            // not need the transaction id
            return new TransactionEvent() {
                public void onEvent(EventIterator events) {
                    // ignore
                }

                public void beforeCommit(TransactionResource resource, String lockToken) {
                    // ignore
                }

                public void afterCommit(TransactionResource resource,
                                        String lockToken,
                                        boolean success) {
                    // ignore
                }
            };
        } else {
            return new TransactionEvent();
        }
    }

    /**
     * Suspend this subscription. This call will remove this subscription as
     * event listener from the observation manager.
     */
    void suspend() throws DavException {
        try {
            obsMgr.removeEventListener(this);
        } catch (RepositoryException e) {
            throw new JcrDavException(e);
        }
    }

    /**
     * Resumes this subscription. This call will register this subscription
     * again as event listener to the observation manager.
     */
    void resume() throws DavException {
        try {
            obsMgr.addEventListener(this, getJcrEventTypes(),
                    getLocator().getRepositoryPath(), isDeep(), getUuidFilters(),
                    getNodetypeNameFilters(), isNoLocal());
        } catch (RepositoryException e) {
            throw new JcrDavException(e);
        }
    }

    //--------------------------------------------< EventListener interface >---
    /**
     * Records the events passed as a new event bundle in order to make them
     * available with the next {@link #discoverEvents(long)} request. If this
     * subscription is expired it will remove itself as listener from the
     * observation manager.
     *
     * @param events to be recorded.
     * @see EventListener#onEvent(EventIterator)
     * @see #discoverEvents(long)
     */
    public synchronized void onEvent(EventIterator events) {
        if (!isExpired()) {
            eventBundles.add(new EventBundleImpl(events));
        } else {
            // expired -> unsubscribe
            try {
                obsMgr.removeEventListener(this);
            } catch (RepositoryException e) {
                log.warn("Exception while unsubscribing: " + e);
            }
        }
        notifyAll();
    }

    //--------------------------------------------------------------------------
    /**
     * Static utility method to convert the type defined by a
     * {@link javax.jcr.observation.Event JCR event} into an <code>EventType</code>
     * object.
     *
     * @param jcrEventType
     * @return <code>EventType</code> representation of the given JCR event type.
     * @throws IllegalArgumentException if the given int does not represent a
     * valid type constants as defined by {@link Event}.<br>
     * Valid values are
     * <ul>
     * <li>{@link Event#NODE_ADDED}</li>
     * <li>{@link Event#NODE_REMOVED}</li>
     * <li>{@link Event#PROPERTY_ADDED}</li>
     * <li>{@link Event#PROPERTY_REMOVED}</li>
     * <li>{@link Event#PROPERTY_CHANGED}</li>
     * <li>{@link Event#NODE_MOVED}</li>
     * <li>{@link Event#PERSIST}</li>
     * </ul>
     */
    public static EventType getEventType(int jcrEventType) {
        String localName;
        switch (jcrEventType) {
            case Event.NODE_ADDED:
                localName = EVENT_NODEADDED;
                break;
            case Event.NODE_REMOVED:
                localName = EVENT_NODEREMOVED;
                break;
            case Event.PROPERTY_ADDED:
                localName = EVENT_PROPERTYADDED;
                break;
            case Event.PROPERTY_CHANGED:
                localName = EVENT_PROPERTYCHANGED;
                break;
            case Event.PROPERTY_REMOVED:
                localName = EVENT_PROPERTYREMOVED;
                break;
            case Event.NODE_MOVED:
                localName = EVENT_NODEMOVED;
                break;
            case Event.PERSIST:
                localName = EVENT_PERSIST;
                break;
            default: // no default
                throw new IllegalArgumentException("Invalid JCR event type: " + jcrEventType);
        }
        return DefaultEventType.create(localName, NAMESPACE);
    }

    /**
     * Static utility method to convert an <code>EventType</code> as present in
     * the Xml body into the corresponding JCR event constant defined by
     * {@link javax.jcr.observation.Event}.
     *
     * @param eventType
     * @return Any of the event types defined by {@link Event}.<br>
     * Possible values are
     * <ul>
     * <li>{@link Event#NODE_ADDED}</li>
     * <li>{@link Event#NODE_REMOVED}</li>
     * <li>{@link Event#PROPERTY_ADDED}</li>
     * <li>{@link Event#PROPERTY_REMOVED}</li>
     * <li>{@link Event#PROPERTY_CHANGED}</li>
     * <li>{@link Event#NODE_MOVED}</li>
     * <li>{@link Event#PERSIST}</li>
     * </ul>
     * @throws DavException if the given event type does not define a valid
     * JCR event type, such as returned by {@link #getEventType(int)}.
     */
    public static int getJcrEventType(EventType eventType) throws DavException {
        if (eventType == null || !NAMESPACE.equals(eventType.getNamespace())) {
            throw new DavException(DavServletResponse.SC_UNPROCESSABLE_ENTITY, "Invalid JCR event type: "+ eventType + ": Namespace mismatch.");
        }
        int eType;
        String eventName = eventType.getName();
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
        } else if (EVENT_NODEMOVED.equals(eventName)) {
            eType = Event.NODE_MOVED;
        } else if (EVENT_PERSIST.equals(eventName)) {
            eType = Event.PERSIST;
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
     * @see SubscriptionImpl#discoverEvents(long)
     */
    private class EventBundleImpl implements EventBundle {

        private final EventIterator events;

        private final String transactionId;

        private EventBundleImpl(EventIterator events) {
            this(events, null);
        }

        private EventBundleImpl(EventIterator events, String transactionId) {
            this.events = events;
            this.transactionId = transactionId;
        }

        public Element toXml(Document document) {
            Element bundle = DomUtil.createElement(document, XML_EVENTBUNDLE, NAMESPACE);
            if (transactionId != null) {
                DomUtil.setAttribute(bundle, XML_EVENT_TRANSACTION_ID, NAMESPACE, transactionId);
            }
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
                eType.appendChild(getEventType(event.getType()).toXml(document));
                // user id
                DomUtil.addChildElement(eventElem, XML_EVENTUSERID, NAMESPACE, event.getUserID());

                // Additional JCR 2.0 event information
                // userdata
                try {
                    DomUtil.addChildElement(eventElem, XML_EVENTUSERDATA, NAMESPACE, event.getUserData());
                } catch (RepositoryException e) {
                    log.error("Internal error while retrieving event user data.", e.getMessage());
                }
                // timestamp
                try {
                    DomUtil.addChildElement(eventElem, XML_EVENTDATE, NAMESPACE, String.valueOf(event.getDate()));
                } catch (RepositoryException e) {
                    log.error("Internal error while retrieving event date.", e.getMessage());
                }
                // identifier
                try {
                    DomUtil.addChildElement(eventElem, XML_EVENTIDENTIFIER, NAMESPACE, event.getIdentifier());
                } catch (RepositoryException e) {
                    log.error("Internal error while retrieving event identifier.", e.getMessage());
                }
                // info
                Element info = DomUtil.addChildElement(eventElem, XML_EVENTINFO, NAMESPACE);
                try {
                    Map m = event.getInfo();
                    for (Iterator it = m.keySet().iterator(); it.hasNext();) {
                        String key = it.next().toString();
                        Object value = m.get(key);
                        if (value != null) {
                            DomUtil.addChildElement(info, key, Namespace.EMPTY_NAMESPACE, value.toString());
                        } else {
                            DomUtil.addChildElement(info, key, Namespace.EMPTY_NAMESPACE);
                        }
                    }
                } catch (RepositoryException e) {
                    log.error("Internal error while retrieving event info.", e.getMessage());
                }
            }
            return bundle;
        }
    }

    //----------------------------< TransactionEvent >------------------------

    /**
     * Implements a transaction event which listenes for events during a save
     * call on the repository.
     */
    private class TransactionEvent implements EventListener, TransactionListener {

        private String transactionId;

        /**
         * {@inheritDoc}
         */
        public void onEvent(EventIterator events) {
            String tId = transactionId;
            if (tId == null) {
                tId = UUID.randomUUID().toString();
            }
            synchronized (SubscriptionImpl.this) {
                eventBundles.add(new EventBundleImpl(events, tId));
                SubscriptionImpl.this.notifyAll();
            }
        }

        //-----------------------------< TransactionListener >------------------

        /**
         * {@inheritDoc}
         */
        public void beforeCommit(TransactionResource resource, String lockToken) {
            try {
                transactionId = lockToken;
                obsMgr.addEventListener(this, getJcrEventTypes(),
                        getLocator().getRepositoryPath(), isDeep(), getUuidFilters(),
                        getNodetypeNameFilters(), isNoLocal());
                // suspend the subscription
                suspend();
            } catch (RepositoryException e) {
                log.warn("Unable to register TransactionListener: " + e);
            } catch (DavException e) {
                log.warn("Unable to register TransactionListener: " + e);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void afterCommit(TransactionResource resource,
                                String lockToken,
                                boolean success) {
            try {
                // resume the subscription
                resume();
                // remove this transaction event
                obsMgr.removeEventListener(this);
            } catch (RepositoryException e) {
                log.warn("Unable to remove listener: " + e);
            } catch (DavException e) {
                log.warn("Unable to resume Subscription: " + e);
            }
        }
    }
}
