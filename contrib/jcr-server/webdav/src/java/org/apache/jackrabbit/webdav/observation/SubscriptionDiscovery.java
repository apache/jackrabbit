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

import org.apache.jackrabbit.webdav.property.AbstractDavProperty;
import org.jdom.Element;

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
     * Returns the Xml representation of the subscription discovery.
     *
     * @return Xml representation
     * @see org.apache.jackrabbit.webdav.property.DavProperty#toXml()
     */
    public Element toXml() {
        Element elem = getName().toXml();
        for (int i = 0; i < subscriptions.length; i++) {
            elem.addContent(subscriptions[i].toXml());
        }
        return elem;
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
}