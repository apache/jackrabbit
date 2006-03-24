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

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.xml.XmlSerializable;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Document;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * <code>EventDiscovery</code> represents the request body of a successfull
 * POLL request. It reveals all events that occured since the last POLL. The
 * definition what events that particular subscription is interested in was
 * specified with the initial SUBSCRIPTION that started the event listening.
 */
public class EventDiscovery implements ObservationConstants, XmlSerializable {

    private static Logger log = Logger.getLogger(EventDiscovery.class);

    private List bundles = new ArrayList();

    /**
     * Add the Xml representation of an single 'eventBundle' listing the
     * events that resulted from a change in the server, filtered by the
     * restrictions present in the corresponding subscription.
     *
     * @param eventBundle
     * @see Subscription
     */
    public void addEventBundle(EventBundle eventBundle) {
        if (eventBundle != null) {
            bundles.add(eventBundle);
        }
    }

    /**
     * Returns the Xml representation of this <code>EventDiscovery</code> as
     * being present in the POLL response body.
     *
     * @return Xml representation
     * @see org.apache.jackrabbit.webdav.xml.XmlSerializable#toXml(Document)
     * @param document
     */
    public Element toXml(Document document) {
        Element ed = DomUtil.createElement(document, XML_EVENTDISCOVERY, NAMESPACE);
        Iterator it = bundles.iterator();
        while (it.hasNext()) {
            EventBundle bundle = (EventBundle)it.next();
            ed.appendChild(bundle.toXml(document));
        }
        return ed;
    }

}