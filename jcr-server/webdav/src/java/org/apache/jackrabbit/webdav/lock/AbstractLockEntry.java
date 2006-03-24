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

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Document;

/**
 * <code>AbstractLockEntry</code> provides the generic {@link org.apache.jackrabbit.webdav.xml.XmlSerializable#toXml} method.
 */
public abstract class AbstractLockEntry implements LockEntry, DavConstants {

    private static Logger log = Logger.getLogger(AbstractLockEntry.class);

    /**
     * Returns the Xml representation of this <code>LockEntry</code>.
     *
     * @return Xml representation
     * @see org.apache.jackrabbit.webdav.xml.XmlSerializable#toXml(Document)
     * @param document
     */
    public Element toXml(Document document) {
        Element entry = DomUtil.createElement(document, XML_LOCKENTRY, NAMESPACE);
        entry.appendChild(getScope().toXml(document));
        entry.appendChild(getType().toXml(document));
        return entry;
    }

}