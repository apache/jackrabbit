/*
 * Copyright 2002-2004 The Apache Software Foundation.
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
package org.apache.jackrabbit.jcr.core.observation;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.jcr.core.ItemManager;
import org.apache.jackrabbit.jcr.core.NoPrefixDeclaredException;
import org.apache.jackrabbit.jcr.core.NodeId;
import org.apache.jackrabbit.jcr.core.SessionImpl;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

/**
 * Implementation of the {@link javax.jcr.observation.Event} interface.
 *
 * @author Marcel Reutegger
 * @version $Revision: 1.6 $
 */
final class EventImpl implements Event {

    /**
     * Logger instance for this class
     */
    private static final Logger log = Logger.getLogger(EventImpl.class);

    /**
     * The session of the {@link javax.jcr.observation.EventListener} this
     * event will be delivered to.
     */
    private final SessionImpl session;

    /**
     * The <code>ItemManager</code> of the session.
     */
    private final ItemManager itemMgr;

    /**
     * The shared {@link EventState} object.
     */
    private final EventState eventState;

    /**
     * Cached String value of this <code>Event</code> instance.
     */
    private String stringValue;

    /**
     * Creates a new {@link javax.jcr.observation.Event} instance based on an
     * {@link EventState eventState}.
     *
     * @param session    the session of the registerd <code>EventListener</code>
     *                   where this <code>Event</code> will be delivered to.
     * @param itemMgr    the <code>ItemManager</code> of the above
     *                   <code>Session</code>.
     * @param eventState the underlying <code>EventState</code>.
     */
    EventImpl(SessionImpl session, ItemManager itemMgr, EventState eventState) {
	this.session = session;
	this.itemMgr = itemMgr;
	this.eventState = eventState;
    }

    /**
     * @see Event#getType()
     */
    public long getType() {
	return eventState.getType();
    }

    /**
     * @see Event#getNodePath()
     */
    public String getNodePath() throws RepositoryException {
	return itemMgr.getItem(new NodeId(eventState.getParentUUID())).getPath();
    }

    /**
     * @see Event#getChildName()
     */
    public String getChildName() throws RepositoryException {
	try {
	    return eventState.getChildItemQName().toJCRName(session.getNamespaceResolver());
	} catch (NoPrefixDeclaredException npde) {
	    // should never get here...
	    String msg = "internal error: encountered unregistered namespace in name";
	    log.error(msg, npde);
	    throw new RepositoryException(msg, npde);
	}
    }

    /**
     * @see Event#getUserId()
     */
    public String getUserId() {
	return eventState.getUserId();
    }

    /**
     * Returns a String representation of this <code>Event</code>.
     *
     * @return a String representation of this <code>Event</code>.
     */
    public String toString() {
	if (stringValue == null) {
	    StringBuffer sb = new StringBuffer();
	    sb.append("Event: Path: ");
	    try {
		sb.append(getNodePath());
	    } catch (RepositoryException e) {
		log.error("Exception retrieving path: " + e);
		sb.append("[Error retrieving path]");
	    }
	    sb.append(", ").append(EventState.valueOf(getType())).append(": ");
	    try {
		sb.append(getChildName());
	    } catch (RepositoryException e) {
		log.error("Exception retrieving child item name: " + e);
		sb.append("[Error retrieving child item name]");
	    }
	    sb.append(", UserId: ").append(getUserId());
	    stringValue = sb.toString();
	}
	return stringValue;
    }

    /**
     * @see Object#hashCode()
     */
    public int hashCode() {
	return eventState.hashCode() ^ session.hashCode();
    }

    /**
     * Returns <code>true</code> if this <code>Event</code> is equal to another
     * object.
     * <p/>
     * Two <code>Event</code> instances are equal if their respective
     * <code>EventState</code> instances are equal and both <code>Event</code>
     * instances are intended for the same <code>Session</code> that registerd
     * the <code>EventListener</code>.
     *
     * @param obj the reference object with which to compare.
     * @return <code>true</code> if this <code>Event</code> is equal to another
     *         object.
     */
    public boolean equals(Object obj) {
	if (this == obj) {
	    return true;
	}
	if (obj instanceof EventImpl) {
	    EventImpl other = (EventImpl) obj;
	    return this.eventState.equals(other.eventState)
		    && this.session.equals(other.session);
	}
	return false;
    }
}
