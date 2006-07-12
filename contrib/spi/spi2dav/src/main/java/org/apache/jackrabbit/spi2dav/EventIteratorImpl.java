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
package org.apache.jackrabbit.spi2dav;

import org.apache.jackrabbit.webdav.xml.ElementIterator;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.observation.ObservationConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.spi.EventIterator;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.Event;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.w3c.dom.Element;

import javax.jcr.RepositoryException;
import java.util.NoSuchElementException;

/**
 * <code>EventIteratorImpl</code>...
 */
public class EventIteratorImpl implements EventIterator {

    private static Logger log = LoggerFactory.getLogger(EventIteratorImpl.class);

    private final SessionInfo sessionInfo;
    private final URIResolver uriResolver;
    private final ElementIterator eventElementIterator;

    private Event next;
    private long pos;

    public EventIteratorImpl(Element eventBundleElement, URIResolver uriResolver, SessionInfo sessionInfo) {
        if (!DomUtil.matches(eventBundleElement, ObservationConstants.XML_EVENTBUNDLE, ObservationConstants.NAMESPACE)) {
            throw new IllegalArgumentException("eventbundle element expected.");
        }

        this.sessionInfo = sessionInfo;
        this.uriResolver = uriResolver;
        eventElementIterator = DomUtil.getChildren(eventBundleElement, ObservationConstants.XML_EVENT, ObservationConstants.NAMESPACE);
        retrieveNext();
    }

    public Event nextEvent() {
        if (next == null) {
            throw new NoSuchElementException();
        }
        Event event = next;
        retrieveNext();
        pos++;
        return event;
    }

    public void skip(long skipNum) {
        while (skipNum-- > 0) {
            next();
        }
    }

    public long getSize() {
        return -1; // size undefined
    }

    public long getPosition() {
        return pos;
    }

    public void remove() {
        eventElementIterator.remove();
    }

    public boolean hasNext() {
        return eventElementIterator.hasNext();
    }

    public Object next() {
        return nextEvent();
    }

    //------------------------------------------------------------< private >---
    /**
     *
     */
    private void retrieveNext() {
        next = null;
        while (next == null && eventElementIterator.hasNext()) {
            Element evElem = eventElementIterator.nextElement();
            try {
                next = new EventImpl(evElem, uriResolver, sessionInfo);
            } catch (RepositoryException e) {
                log.error("Unexpected error while creating event.", e);
            } catch (DavException e) {
                log.error("Unexpected error while creating event.", e);
            }
        }
    }
}