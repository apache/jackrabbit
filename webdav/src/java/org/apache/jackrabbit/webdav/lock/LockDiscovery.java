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

import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.AbstractDavProperty;
import org.w3c.dom.Element;
import org.w3c.dom.Document;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * The <code>LockDiscovery</code> class encapsulates the webdav lock discovery
 * property that is sent in the request body (PROPFIND and LOCK) and received
 * in a LOCK response body.
 */
public class LockDiscovery extends AbstractDavProperty {

    /**
     * Listing of existing locks applied to the resource this discovery was
     * requested for. Each entry reveals, who has a lock, what type of lock he has,
     * the timeout type and the time remaining on the timeout, and the lock-type.
     * NOTE, that any of the information listed may be not availble for the
     * server is free to withhold any or all of this information.
     */
    private List activeLocks = new ArrayList();

    /**
     * Creates a new empty LockDiscovery property
     */
    public LockDiscovery() {
        super(DavPropertyName.LOCKDISCOVERY, false);
    }

    /**
     * Create a new LockDiscovery property
     *
     * @param lock
     */
    public LockDiscovery(ActiveLock lock) {
        super(DavPropertyName.LOCKDISCOVERY, false);
        addActiveLock(lock);
    }

    /**
     * Create a new LockDiscovery property
     *
     * @param locks
     */
    public LockDiscovery(ActiveLock[] locks) {
        super(DavPropertyName.LOCKDISCOVERY, false);
        for (int i = 0; i < locks.length; i++) {
            addActiveLock(locks[i]);
        }
    }

    private void addActiveLock(ActiveLock lock) {
        if (lock != null) {
            activeLocks.add(lock);
        }
    }

    /**
     * Returns the list of active locks.
     *
     * @return list of active locks
     * @see org.apache.jackrabbit.webdav.property.DavProperty#getValue()
     */
    public Object getValue() {
        return activeLocks;
    }

    /**
     * Creates a JDOM  <code>&lt;lockdiscovery&gt;</code> element in order to respond to a LOCK
     * request or to the lockdiscovery property of a PROPFIND request.<br>
     * NOTE: if the {@link #activeLocks} list is empty an empty lockdiscovery
     * property is created ( <code>&lt;lockdiscovery/&gt;</code>)
     * @return A JDOM element of the &lt;active> lock tag.
     * @param document
     */
    public Element toXml(Document document) {
        Element lockdiscovery = getName().toXml(document);
        Iterator it = activeLocks.iterator();
        while (it.hasNext()) {
            ActiveLock lock = (ActiveLock) it.next();
            lockdiscovery.appendChild(lock.toXml(document));
	}
	return lockdiscovery;
    }

}
