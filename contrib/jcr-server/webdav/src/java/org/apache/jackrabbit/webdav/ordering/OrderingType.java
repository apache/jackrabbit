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
package org.apache.jackrabbit.webdav.ordering;

import org.jdom.Element;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.util.XmlUtil;

/**
 * <code>OrderingType</code> represents the {@link #ORDERING_TYPE
 * DAV:ordering-type} property as defined by
 * <a href="http://www.ietf.org/rfc/rfc3648.txt">RFC 3648</a>. This property is
 * protected cannot be set using PROPPATCH. Its value may only be set by
 * including the Ordering-Type header with a MKCOL request or by submitting an
 * ORDERPATCH request.
 *
 * @see org.apache.jackrabbit.webdav.property.DavProperty#isProtected()
 */
public class OrderingType extends DefaultDavProperty implements OrderingConstants {

    /**
     * Create an OrderingType with the given ordering.<br>
     * NOTE: the ordering-type property is defined to be protected.
     *
     * @param href
     * @see org.apache.jackrabbit.webdav.property.DavProperty#isProtected()
     */
    public OrderingType(String href) {
        super(ORDERING_TYPE, href, true);
    }

    /**
     * Returns the Xml representation of this property. If the property has
     * a <code>null</code> value, the default ({@link #ORDERING_TYPE_UNORDERED
     * DAV:unordered}) is assumed.
     *
     * @return Xml representation
     */
    public Element toXml() {
        Element elem = getName().toXml();
        // spec requires that the default is 'DAV:unordered'
        String href = (getValue() != null) ? getValue().toString() : ORDERING_TYPE_UNORDERED;
        XmlUtil.hrefToXml(href);
        return elem;
    }
}