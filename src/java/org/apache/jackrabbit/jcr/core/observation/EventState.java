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

import org.apache.jackrabbit.jcr.core.QName;

import javax.jcr.Session;
import javax.jcr.observation.EventType;

/**
 * The <code>EventState</code> class encapsulates the session
 * independent state of an {@link javax.jcr.observation.Event}.
 *
 * @author Marcel Reutegger
 * @version $Revision: 1.6 $
 */
class EventState {

    /**
     * The {@link javax.jcr.observation.EventType} of this event.
     */
    private final long type;

    /**
     * The UUID of the parent node associated with this event.
     */
    private final String parentUUID;

    /**
     * The qualified name of the child item associated with this event.
     */
    private final QName childName;

    /**
     * The session that caused this event.
     */
    private final Session session;

    /**
     * Cached String representation of this <code>EventState</code>.
     */
    private String stringValue;

    /**
     * Cached hashCode value for this <code>Event</code>.
     */
    private int hashCode;

    /**
     * Creates a new <code>EventState</code> instance.
     *
     * @param type       the {@link javax.jcr.observation.EventType} of this
     *                   event.
     * @param parentUUID the uuid of the parent node associated with this event.
     * @param childName  the qualified name of the child item associated with
     *                   this event.
     * @param session    the {@link javax.jcr.Session} that
     *                   caused this event.
     */
    private EventState(long type, String parentUUID, QName childName, Session session) {
	this.type = type;
	this.parentUUID = parentUUID;
	this.childName = childName;
	this.session = session;
    }

    //-----------------< factory methods >--------------------------------------

    /**
     * Creates a new {@link javax.jcr.observation.Event} of type
     * {@link javax.jcr.observation.EventType#CHILD_NODE_ADDED}.
     *
     * @param parentUUID the uuid of the parent node associated with
     *                   this <code>EventState</code>.
     * @param childName  the qualified name of the child node that was added.
     * @param session    the session that added the node.
     * @return an <code>EventState</code> instance.
     */
    public static EventState ChildNodeAdded(String parentUUID,
					    QName childName,
					    Session session) {
	return new EventState(EventType.CHILD_NODE_ADDED,
		parentUUID,
		childName,
		session);
    }

    /**
     * Creates a new {@link javax.jcr.observation.Event} of type
     * {@link javax.jcr.observation.EventType#CHILD_NODE_REMOVED}.
     *
     * @param parentUUID the uuid of the parent node associated with
     *                   this <code>EventState</code>.
     * @param childName  the qualified name of the child node that was removed.
     * @param session    the session that removed the node.
     * @return an <code>EventState</code> instance.
     */
    public static EventState ChildNodeRemoved(String parentUUID,
					      QName childName,
					      Session session) {
	return new EventState(EventType.CHILD_NODE_REMOVED,
		parentUUID,
		childName,
		session);
    }

    /**
     * Creates a new {@link javax.jcr.observation.Event} of type
     * {@link javax.jcr.observation.EventType#PROPERTY_ADDED}.
     *
     * @param parentUUID the uuid of the parent node associated with
     *                   this <code>EventState</code>.
     * @param childName  the qualified name of the property that was added.
     * @param session    the session that added the property.
     * @return an <code>EventState</code> instance.
     */
    public static EventState PropertyAdded(String parentUUID,
					   QName childName,
					   Session session) {
	return new EventState(EventType.PROPERTY_ADDED,
		parentUUID,
		childName,
		session);
    }

    /**
     * Creates a new {@link javax.jcr.observation.Event} of type
     * {@link javax.jcr.observation.EventType#PROPERTY_REMOVED}.
     *
     * @param parentUUID the uuid of the parent node associated with
     *                   this <code>EventState</code>.
     * @param childName  the qualified name of the property that was removed.
     * @param session    the session that removed the property.
     * @return an <code>EventState</code> instance.
     */
    public static EventState PropertyRemoved(String parentUUID,
					     QName childName,
					     Session session) {
	return new EventState(EventType.PROPERTY_REMOVED,
		parentUUID,
		childName,
		session);
    }

    /**
     * Creates a new {@link javax.jcr.observation.Event} of type
     * {@link javax.jcr.observation.EventType#PROPERTY_CHANGED}.
     *
     * @param parentUUID the uuid of the parent node associated with
     *                   this <code>EventState</code>.
     * @param childName  the qualified name of the property that changed.
     * @param session    the session that changed the property.
     * @return an <code>EventState</code> instance.
     */
    public static EventState PropertyChanged(String parentUUID,
					     QName childName,
					     Session session) {
	return new EventState(EventType.PROPERTY_CHANGED,
		parentUUID,
		childName,
		session);
    }

    /**
     * @see javax.jcr.observation.Event#getType()
     */
    public long getType() {
	return type;
    }

    /**
     * Returns the uuid of the parent node.
     *
     * @return the uuid of the parent node.
     */
    public String getParentUUID() {
	return parentUUID;
    }

    /**
     * Returns the {@link QName} of the
     * {@link javax.jcr.Item} associated with this event.
     *
     * @return the <code>QName</code> associated with this event.
     */
    public QName getChildItemQName() {
	return childName;
    }

    /**
     * @see javax.jcr.observation.Event#getUserId()
     */
    public String getUserId() {
	return session.getUserId();
    }

    /**
     * Returns the <code>Session</code> that caused / created this
     * <code>EventState</code>.
     *
     * @return the <code>Session</code> that caused / created this
     *         <code>EventState</code>.
     */
    Session getSession() {
	return session;
    }

    /**
     * Returns a String representation of this <code>EventState</code>.
     *
     * @return a String representation of this <code>EventState</code>.
     */
    public String toString() {
	if (stringValue == null) {
	    StringBuffer sb = new StringBuffer();
	    sb.append("EventState: ").append(valueOf(type));
	    sb.append(", Parent: ").append(parentUUID);
	    sb.append(", Child: ").append(childName);
	    sb.append(", UserId: ").append(session.getUserId());
	    stringValue = sb.toString();
	}
	return stringValue;
    }

    /**
     * Returns a hashCode for this <code>EventState</code>.
     *
     * @return a hashCode for this <code>EventState</code>.
     */
    public int hashCode() {
	int h = hashCode;
	if (h == 0) {
	    h = 37;
	    h = 37 * h + (int) type;
	    h = 37 * h + parentUUID.hashCode();
	    h = 37 * h + childName.hashCode();
	    h = 37 * h + session.hashCode();
	    hashCode = h;
	}
	return hashCode;
    }

    /**
     * Returns <code>true</code> if this <code>EventState</code> is equal to
     * another object.
     *
     * @param obj the reference object with which to compare.
     * @return <code>true</code> if object <code>obj</code> is equal to this
     *         <code>EventState</code>; <code>false</code> otherwise.
     */
    public boolean equals(Object obj) {
	if (obj == this) {
	    return true;
	}
	if (obj instanceof EventState) {
	    EventState other = (EventState) obj;
	    return this.type == other.type
		    && this.parentUUID.equals(other.parentUUID)
		    && this.childName.equals(other.childName)
		    && this.session.equals(other.session);
	}
	return false;
    }

    /**
     * Returns a String representation of <code>eventType</code>.
     *
     * @param eventType an event type defined by {@link EventType}.
     * @return a String representation of <code>eventType</code>.
     */
    public static String valueOf(long eventType) {
	if (eventType == EventType.CHILD_NODE_ADDED) {
	    return "ChildNodeAdded";
	} else if (eventType == EventType.CHILD_NODE_REMOVED) {
	    return "ChildNodeRemoved";
	} else if (eventType == EventType.PROPERTY_ADDED) {
	    return "PropertyAdded";
	} else if (eventType == EventType.PROPERTY_CHANGED) {
	    return "PropertyChanged";
	} else if (eventType == EventType.PROPERTY_REMOVED) {
	    return "PropertyRemoved";
	} else {
	    return "UnknownEventType";
	}
    }

}
