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

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.util.XmlUtil;
import org.jdom.Element;

import java.util.List;

/**
 * <code>SubscriptionInfo</code> class encapsulates the subscription info
 * that forms the request body of a SUBSCRIBE request.
 * @see ObservationConstants#XML_SUBSCRIPTIONINFO
 */
public class SubscriptionInfo implements ObservationConstants {

    private static Logger log = Logger.getLogger(SubscriptionInfo.class);

    private Element info;
    private List eventTypes;
    private long timeout;
    private boolean isDeep;

    /**
     * Create a new <code>SubscriptionInfo</code>
     *
     * @param reqInfo Xml element present in the request body.
     * @param timeout as defined by the {@link org.apache.jackrabbit.webdav.DavConstants#HEADER_TIMEOUT timeout header}.
     * @param isDeep as defined by the {@link org.apache.jackrabbit.webdav.DavConstants#HEADER_DEPTH depth header}.
     * @throws IllegalArgumentException if the reqInfo element does not contain the mandatory elements.
     */
    public SubscriptionInfo(Element reqInfo, long timeout, boolean isDeep) {
        if (!XML_SUBSCRIPTIONINFO.equals(reqInfo.getName())) {
            throw new IllegalArgumentException("Element with name 'subscriptioninfo' expected");
        }
        if (reqInfo.getChild(XML_EVENTTYPE, NAMESPACE) == null ) {
            throw new IllegalArgumentException("'subscriptioninfo' must contain an 'eventtype' child element.");
        }

        eventTypes = reqInfo.getChild(XML_EVENTTYPE, NAMESPACE).getChildren();
        if (eventTypes.size() == 0) {
            throw new IllegalArgumentException("'subscriptioninfo' must at least indicate a single event type.");
        }

        // detach the request info, in order to remove the reference to the parent
        this.info = (Element)reqInfo.detach();
        this.isDeep = isDeep;
        setTimeOut(timeout);
    }

    /**
     * Return list of event types Xml elements present in the subscription info.
     * NOTE: the elements need to be detached in order to be added as content
     * to any other Xml element.
     *
     * @return List of Xml elements defining which events this subscription should
     * listen to.
     *
     */
    public List getEventTypes() {
        return eventTypes;
    }

    /**
     * Return array of filters with the specified name.
     *
     * @param name the filter elments must provide.
     * @return array containing the text of the filter elements with the given
     * name.
     */
    public String[] getFilters(String name) {
        String[] filters = null;
        Element filter = info.getChild(XML_FILTER);
        if (filter != null) {
            List li = filter.getChildren(name);
            if (!li.isEmpty()) {
                filters = new String[li.size()];
                for (int i = 0; i < filters.length; i++) {
                    filters[i] = ((Element)li.get(i)).getText();
                }
            }
        }
        return filters;
    }

    /**
     * Returns true if the {@link #XML_NOLOCAL} element is present in this
     * subscription info.
     *
     * @return if {@link #XML_NOLOCAL} element is present.
     */
    public boolean isNoLocal() {
        return info.getChild(XML_NOLOCAL, NAMESPACE) != null;
    }

    /**
     * Returns true if the {@link org.apache.jackrabbit.webdav.DavConstants#HEADER_DEPTH
     * depths header} defined a depth other than '0'.
     *
     * @return true if this subscription info was created with <code>isDeep</code>
     * true.
     */
    public boolean isDeep() {
        return isDeep;
    }

    /**
     * Return the timeout as retrieved from the request.
     *
     * @return timeout.
     */
    public long getTimeOut() {
        return timeout;
    }

    /**
     * Set the timeout. NOTE: no validation is made.
     *
     * @param timeout as defined by the {@link org.apache.jackrabbit.webdav.DavConstants#HEADER_TIMEOUT}.
     */
    public void setTimeOut(long timeout) {
        this.timeout = timeout;
    }

    /**
     * Xml representation of this <code>SubscriptionInfo</code>.
     *
     * @return Xml representation
     */
    public Element[] toXml() {
        Element[] elems = { info, XmlUtil.depthToXml(isDeep), XmlUtil.timeoutToXml(timeout)};
        return elems;
    }
}