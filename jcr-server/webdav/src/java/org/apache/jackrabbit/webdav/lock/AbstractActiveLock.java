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
package org.apache.jackrabbit.webdav.lock;

import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Document;

/**
 * <code>AbstractActiveLock</code>...
 */
public abstract class AbstractActiveLock implements ActiveLock, DavConstants {

    /**
     * Returns the default Xml representation of the 'activelock' element
     * as defined by RFC 2518.
     *
     * @return Xml representation
     * @param document
     */
    public Element toXml(Document document) {
        Element activeLock = DomUtil.createElement(document, XML_ACTIVELOCK, NAMESPACE);

        // lockscope property
        activeLock.appendChild(getScope().toXml(document));
        // locktype property
        activeLock.appendChild(getType().toXml(document));
        // depth
        activeLock.appendChild(DomUtil.depthToXml(isDeep(), document));
        // timeout
        long timeout = getTimeout();
        if (!isExpired() && timeout != UNDEFINED_TIMEOUT) {
            activeLock.appendChild(DomUtil.timeoutToXml(timeout, document));
        }

        // owner
        if (getOwner() != null) {
            DomUtil.addChildElement(activeLock, XML_OWNER, NAMESPACE, getOwner());
        }

        // locktoken
        if (getToken() != null) {
            Element lToken = DomUtil.addChildElement(activeLock, XML_LOCKTOKEN, NAMESPACE);
            lToken.appendChild(DomUtil.hrefToXml(getToken(), document));
        }
        return activeLock;
    }

}