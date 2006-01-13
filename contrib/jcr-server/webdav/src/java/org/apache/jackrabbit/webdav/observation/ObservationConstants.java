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
package org.apache.jackrabbit.webdav.observation;

import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.xml.Namespace;

/**
 * <code>ObservationConstants</code> interface provide constants for request
 * and response headers, Xml elements and property names used for handling
 * observation over WebDAV. There exists no public standard for this
 * functionality.
 */
public interface ObservationConstants {

    /**
     * The namespace
     */
    public static final Namespace NAMESPACE = Namespace.getNamespace("dcr", "http://www.day.com/jcr/webdav/1.0");

    /**
     * The SubscriptionId request header<br>
     */
    public static final String HEADER_SUBSCRIPTIONID = "SubscriptionId";

    /**
     * subscription Xml element<br>
     * Mandatory element inside the {@link #SUBSCRIPTIONDISCOVERY subscriptiondiscovery}
     * property indicating the event listeners present for this session.<br>
     * NOTE, that this will not reveal any subscription made by another session.
     */
    public static final String XML_SUBSCRIPTION = "subscription";
    
    /**
     * Xml elements
     */
    public static final String XML_SUBSCRIPTIONINFO = "subscriptioninfo";

    public static final String XML_EVENTTYPE = "eventtype";
    public static final String XML_NOLOCAL = "nolocal";
    public static final String XML_FILTER = "filter";
    public static final String XML_SUBSCRIPTIONID = "subscriptionid";
    public static final String XML_UUID = "uuid";
    public static final String XML_NODETYPE_NAME = "nodetype-name";

    public static final String XML_EVENTDISCOVERY = "eventdiscovery";
    public static final String XML_EVENTBUNDLE = "eventbundle";
    public static final String XML_EVENT = "event";
    public static final String XML_EVENTUSERID = "eventuserid";

    /**
     * Element representing the 'nodeadded' event type.
     * @see javax.jcr.observation.Event#NODE_ADDED
     */
    public static final String EVENT_NODEADDED = "nodeadded";

    /**
     * Element representing the 'noderemoved' event type.
     * @see javax.jcr.observation.Event#NODE_REMOVED
     */
    public static final String EVENT_NODEREMOVED = "noderemoved";

    /**
     * Element representing the 'propertyadded' event type.
     * @see javax.jcr.observation.Event#PROPERTY_ADDED
     */
    public static final String EVENT_PROPERTYADDED = "propertyadded";

    /**
     * Element representing the 'propertyremoved' event type.
     * @see javax.jcr.observation.Event#PROPERTY_REMOVED
     */
    public static final String EVENT_PROPERTYREMOVED = "propertyremoved";

    /**
     * Element representing the 'propertychanged' event type.
     * @see javax.jcr.observation.Event#PROPERTY_CHANGED
     */
    public static final String EVENT_PROPERTYCHANGED = "propertychanged";

    /**
     * The protected subscription discovery property is used to find out about
     * existing subscriptions present on the specified resource.
     */
    public static final DavPropertyName SUBSCRIPTIONDISCOVERY = DavPropertyName.create("subscriptiondiscovery", NAMESPACE);
}