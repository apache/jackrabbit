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
package org.apache.jackrabbit.webdav.lock;

import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.util.XmlUtil;
import org.jdom.Element;

/**
 * <code>AbstractActiveLock</code>...
 */
public abstract class AbstractActiveLock implements ActiveLock, DavConstants {

    /**
     * Returns the default Xml representation of the 'activelock' element
     * as defined by RFC 2518.
     *
     * @return Xml representation
     */
    public Element toXml() {
        Element activeLock = new Element(XML_ACTIVELOCK, NAMESPACE);

        // locktype property
        Element property = new Element(XML_LOCKTYPE, NAMESPACE);
        property.addContent(getType().toXml());
        activeLock.addContent(property);

        // lockscope property
        property = new Element(XML_LOCKSCOPE, NAMESPACE);
        property.addContent(getScope().toXml());
        activeLock.addContent(property);

        // depth
        activeLock.addContent(XmlUtil.depthToXml(isDeep()));
        // timeout
        long timeout = getTimeout();
        if (!isExpired() && timeout != UNDEFINED_TIMEOUT) {
            activeLock.addContent(XmlUtil.timeoutToXml(getTimeout()));
        }

        // owner
        if (getOwner() != null) {
            property = new Element(XML_OWNER, NAMESPACE);
            property.setText(getOwner());
            activeLock.addContent(property);
        }

        // locktoken
        if (getToken() != null) {
            property = new Element(XML_LOCKTOKEN, NAMESPACE);
            Element href = new Element(XML_HREF, NAMESPACE);
            href.setText(getToken());
            property.addContent(href);
            activeLock.addContent(property);
        }
        return activeLock;
    }
}