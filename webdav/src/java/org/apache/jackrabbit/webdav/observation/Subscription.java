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

import org.jdom.Element;

/**
 * <code>Subscription</code> represents public representation of the event
 * listener created (or modified) by a successful SUBSCRIBE request.
 */
public interface Subscription {

    /**
     * Returns the id of this subscription, that must be used for unsubscribing
     * as well as for event discovery later on.
     *
     * @return subscriptionId
     */
    public String getSubscriptionId();

    /**
     * Return the Xml representation of this <code>Subscription</code> that is
     * returned in the response to a successful SUBSCRIBE request as well
     * as in a PROPFIND request. In both cases the subscription is packed into
     * a {@link SubscriptionDiscovery} property object.
     *
     * @return Xml representation
     */
    public Element toXml();

}