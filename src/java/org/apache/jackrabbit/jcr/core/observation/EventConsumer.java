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

import javax.jcr.Session;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

/**
 * The <code>EventConsumer</code> class combines the {@link
 * javax.jcr.observation.EventListener} with the implementation of specified
 * filter for the listener: {@link EventFilter}.
 * <p/>
 * Collections of {@link EventState} objects will be dispatched to {@link
 * #consumeEvents}.
 *
 * @author Marcel Reutegger
 * @version $Revision: 1.7 $, $Date: 2004/08/25 16:44:50 $
 */
class EventConsumer {

    /**
     * The <code>Session</code> associated with this <code>EventConsumer</code>.
     */
    private final Session session;

    /**
     * The listener part of this <code>EventConsumer</code>.
     */
    private final EventListener listener;

    /**
     * The <code>EventFilter</code> for this <code>EventConsumer</code>.
     */
    private final EventFilter filter;

    /**
     * cached hash code value
     */
    private int hashCode;

    /**
     * An <code>EventConsumer</code> consists of a <code>Session</code>, the
     * attached <code>EventListener</code> and an <code>EventFilter</code>.
     *
     * @param session  the <code>Session</code> that created this
     *                 <code>EventConsumer</code>.
     * @param listener the actual <code>EventListener</code> to call back.
     * @param filter   only pass an <code>Event</code> to the listener if the
     *                 <code>EventFilter</code> allows the <code>Event</code>.
     * @throws NullPointerException if <code>session</code>, <code>listener</code>
     *                              or <code>filter</code> is<code>null</code>.
     */
    EventConsumer(Session session, EventListener listener, EventFilter filter) {
	if (session == null) {
	    throw new NullPointerException("session");
	}
	if (listener == null) {
	    throw new NullPointerException("listener");
	}
	if (filter == null) {
	    throw new NullPointerException("filter");
	}

	this.session = session;
	this.listener = listener;
	this.filter = filter;
    }

    /**
     * Returns the <code>Session</code> that is associated
     * with this <code>EventConsumer</code>.
     *
     * @return the <code>Session</code> of this <code>EventConsumer</code>.
     */
    Session getSession() {
	return session;
    }

    /**
     * Returns the <code>EventListener</code> that is associated with this
     * <code>EventConsumer</code>.
     *
     * @return the <code>EventListener</code> of this <code>EventConsumer</code>.
     */
    EventListener getEventListener() {
	return listener;
    }

    /**
     * Dispatches the events to the <code>EventListener</code>.
     *
     * @param events a collection of {@link EventState}s
     *               to dispatch.
     */
    void consumeEvents(EventStateCollection events) {
	// check if filtered iterator has at least one event
	EventIterator it = new FilteredEventIterator(events, filter);
	if (it.hasNext()) {
	    listener.onEvent(it);
	} else {
	    // otherwise skip this listener
	}
    }

    /**
     * Returns <code>true</code> if this <code>EventConsumer</code> is equal to
     * some other object, <code>false</code> otherwise.
     * <p/>
     * Two <code>EventConsumer</code>s are considered equal if they refer to the
     * same <code>Session</code> and the <code>EventListener</code>s they
     * reference are equal. Note that the <code>EventFilter</code> is ignored in
     * this check.
     *
     * @param obj the reference object with which to compare.
     * @return <code>true</code> if this <code>EventConsumer</code> is equal the
     *         other <code>EventConsumer</code>.
     */
    public boolean equals(Object obj) {
	if (this == obj) {
	    return true;
	}
	if (obj instanceof EventConsumer) {
	    EventConsumer other = (EventConsumer) obj;
	    return session.equals(other.session)
		    && listener.equals(other.listener);
	}
	return false;
    }

    /**
     * Returns the hash code for this <code>EventConsumer</code>.
     *
     * @return the hash code for this <code>EventConsumer</code>.
     */
    public int hashCode() {
	if (hashCode == 0) {
	    hashCode = session.hashCode() ^ listener.hashCode();
	}
	return hashCode;
    }
}
