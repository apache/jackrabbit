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

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.DavConstants;
import org.jdom.Element;

/**
 * <code>AbstractLockEntry</code> provides the generic {@link #toXml} method.
 */
public abstract class AbstractLockEntry implements LockEntry, DavConstants {

    private static Logger log = Logger.getLogger(AbstractLockEntry.class);

    /**
     * Returns the Xml representation of this <code>LockEntry</code>.
     *
     * @return Xml representation
     */
    public Element toXml() {
        Element entry = new Element(XML_LOCKENTRY, NAMESPACE);
        Element prop = new Element(XML_LOCKSCOPE, NAMESPACE);
        prop.addContent(getScope().toXml());
        entry.addContent(prop);
        prop = new Element(XML_LOCKTYPE, NAMESPACE);
        prop.addContent(getType().toXml());
        entry.addContent(prop);
        return entry;
    }
}