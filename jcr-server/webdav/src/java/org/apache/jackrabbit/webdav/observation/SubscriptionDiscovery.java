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
package org.apache.jackrabbit.webdav.observation;

import org.apache.jackrabbit.webdav.property.AbstractDavProperty;
import org.w3c.dom.Element;
import org.w3c.dom.Document;

/**
 * <code>SubscriptionDiscovery</code> encapsulates the 'subscriptiondiscovery'
 * property of a webdav resource.
 */
public class SubscriptionDiscovery extends AbstractDavProperty {

    Subscription[] subscriptions = new Subscription[0];

    /**
     * Create a new <code>SubscriptionDiscovery</code> that lists the given
     * subscriptions.
     *
     * @param subscriptions
     */
    public SubscriptionDiscovery(Subscription[] subscriptions) {
        super(ObservationConstants.SUBSCRIPTIONDISCOVERY, true);
        if (subscriptions != null) {
            this.subscriptions = subscriptions;
        }
    }

    /**
     * Create a new <code>SubscriptionDiscovery</code> that contains a single
     * subscription entry.
     * 
     * @param subscription
     */
    public SubscriptionDiscovery(Subscription subscription) {
        super(ObservationConstants.SUBSCRIPTIONDISCOVERY, true);
        if (subscription != null) {
            this.subscriptions = new Subscription[]{subscription};
        }
    }

    /**
     * Returns an array of {@link Subscription}s.
     *
     * @return an array of {@link Subscription}s
     * @see org.apache.jackrabbit.webdav.property.DavProperty#getValue()
     */
    public Object getValue() {
        return subscriptions;
    }

    /**
     * Returns the Xml representation of the subscription discovery.
     *
     * @return Xml representation
     * @see org.apache.jackrabbit.webdav.xml.XmlSerializable#toXml(Document)
     * @param document
     */
    public Element toXml(Document document) {
        Element elem = getName().toXml(document);
        for (int i = 0; i < subscriptions.length; i++) {
            elem.appendChild(subscriptions[i].toXml(document));
        }
        return elem;
    }

}