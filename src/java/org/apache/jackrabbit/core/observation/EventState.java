/*
 * Copyright 2004 The Apache Software Foundation.
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
package org.apache.jackrabbit.core.observation;

import org.apache.jackrabbit.core.Path;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;

import javax.jcr.Session;
import javax.jcr.observation.Event;

/**
 * The <code>EventState</code> class encapsulates the session
 * independent state of an {@link javax.jcr.observation.Event}.
 */
class EventState {

    /**
     * The {@link javax.jcr.observation.Event} of this event.
     */
    private final int type;

    /**
     * The UUID of the parent node associated with this event.
     */
    private final String parentUUID;

    /**
     * The path of the parent node associated with this event.
     */
    private final Path parentPath;

    /**
     * The UUID of a child node, in case this EventState is of type
     * {@link javax.jcr.observation.Event#NODE_ADDED} or
     * {@link javax.jcr.observation.Event#NODE_REMOVED}.
     */
    private final String childUUID;

    /**
     * The qualified name of the child item associated with this event.
     */
    private final QName childName;

    /**
     * The node type of the parent node.
     */
    private final NodeTypeImpl nodeType;

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
     * @param type       the type of this event.
     * @param parentUUID the uuid of the parent node associated with this
     *                   event.
     * @param parentPath the path of the parent node associated with this
     *                   event.
     * @param childUUID  the uuid of the child node associated with this event.
     *                   If the event type is one of: <code>PROPERTY_ADDED</code>,
     *                   <code>PROPERTY_CHANGED</code> or <code>PROPERTY_REMOVED</code>
     *                   this parameter must be <code>null</code>.
     * @param childName  the qualified name of the child item associated with
     *                   this event.
     * @param nodeType   the node type of the parent node.
     * @param session    the {@link javax.jcr.Session} that caused this event.
     */
    private EventState(int type,
                       String parentUUID,
                       Path parentPath,
                       String childUUID,
                       QName childName,
                       NodeTypeImpl nodeType,
                       Session session) {
        int mask = (Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED);
        if ((type & mask) > 0) {
            if (childUUID != null) {
                throw new IllegalArgumentException("childUUID only allowed for Node events.");
            }
        } else {
            if (childUUID == null) {
                throw new IllegalArgumentException("childUUID must not be null for Node events.");
            }
        }
        this.type = type;
        this.parentUUID = parentUUID;
        this.parentPath = parentPath;
        this.childUUID = childUUID;
        this.childName = childName;
        this.nodeType = nodeType;
        this.session = session;
    }

    //-----------------< factory methods >--------------------------------------

    /**
     * Creates a new {@link javax.jcr.observation.Event} of type
     * {@link javax.jcr.observation.Event#NODE_ADDED}.
     *
     * @param parentUUID the uuid of the parent node associated with
     *                   this <code>EventState</code>.
     * @param parentPath the path of the parent node associated with
     *                   this <code>EventState</code>.
     * @param childUUID  the uuid of the child node associated with this event.
     * @param childName  the qualified name of the child node that was added.
     * @param nodeType   the node type of the parent node.
     * @param session    the session that added the node.
     * @return an <code>EventState</code> instance.
     */
    public static EventState childNodeAdded(String parentUUID,
                                            Path parentPath,
                                            String childUUID,
                                            QName childName,
                                            NodeTypeImpl nodeType,
                                            Session session) {
        return new EventState(Event.NODE_ADDED,
                parentUUID,
                parentPath,
                childUUID,
                childName,
                nodeType,
                session);
    }

    /**
     * Creates a new {@link javax.jcr.observation.Event} of type
     * {@link javax.jcr.observation.Event#NODE_REMOVED}.
     *
     * @param parentUUID the uuid of the parent node associated with
     *                   this <code>EventState</code>.
     * @param parentPath the path of the parent node associated with
     *                   this <code>EventState</code>.
     * @param childUUID  the uuid of the child node associated with this event.
     * @param childName  the qualified name of the child node that was removed.
     * @param nodeType   the node type of the parent node.
     * @param session    the session that removed the node.
     * @return an <code>EventState</code> instance.
     */
    public static EventState childNodeRemoved(String parentUUID,
                                              Path parentPath,
                                              String childUUID,
                                              QName childName,
                                              NodeTypeImpl nodeType,
                                              Session session) {
        return new EventState(Event.NODE_REMOVED,
                parentUUID,
                parentPath,
                childUUID,
                childName,
                nodeType,
                session);
    }

    /**
     * Creates a new {@link javax.jcr.observation.Event} of type
     * {@link javax.jcr.observation.Event#PROPERTY_ADDED}.
     *
     * @param parentUUID the uuid of the parent node associated with
     *                   this <code>EventState</code>.
     * @param parentPath the path of the parent node associated with
     *                   this <code>EventState</code>.
     * @param childName  the qualified name of the property that was added.
     * @param nodeType   the node type of the parent node.
     * @param session    the session that added the property.
     * @return an <code>EventState</code> instance.
     */
    public static EventState propertyAdded(String parentUUID,
                                           Path parentPath,
                                           QName childName,
                                           NodeTypeImpl nodeType,
                                           Session session) {
        return new EventState(Event.PROPERTY_ADDED,
                parentUUID,
                parentPath,
                null,
                childName,
                nodeType,
                session);
    }

    /**
     * Creates a new {@link javax.jcr.observation.Event} of type
     * {@link javax.jcr.observation.Event#PROPERTY_REMOVED}.
     *
     * @param parentUUID the uuid of the parent node associated with
     *                   this <code>EventState</code>.
     * @param parentPath the path of the parent node associated with
     *                   this <code>EventState</code>.
     * @param childName  the qualified name of the property that was removed.
     * @param nodeType   the node type of the parent node.
     * @param session    the session that removed the property.
     * @return an <code>EventState</code> instance.
     */
    public static EventState propertyRemoved(String parentUUID,
                                             Path parentPath,
                                             QName childName,
                                             NodeTypeImpl nodeType,
                                             Session session) {
        return new EventState(Event.PROPERTY_REMOVED,
                parentUUID,
                parentPath,
                null,
                childName,
                nodeType,
                session);
    }

    /**
     * Creates a new {@link javax.jcr.observation.Event} of type
     * {@link javax.jcr.observation.Event#PROPERTY_CHANGED}.
     *
     * @param parentUUID the uuid of the parent node associated with
     *                   this <code>EventState</code>.
     * @param parentPath the path of the parent node associated with
     *                   this <code>EventState</code>.
     * @param childName  the qualified name of the property that changed.
     * @param nodeType   the node type of the parent node.
     * @param session    the session that changed the property.
     * @return an <code>EventState</code> instance.
     */
    public static EventState propertyChanged(String parentUUID,
                                             Path parentPath,
                                             QName childName,
                                             NodeTypeImpl nodeType,
                                             Session session) {
        return new EventState(Event.PROPERTY_CHANGED,
                parentUUID,
                parentPath,
                null,
                childName,
                nodeType,
                session);
    }

    /**
     * @see javax.jcr.observation.Event#getType()
     */
    public int getType() {
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
     * Returns the path of the parent node.
     *
     * @return the path of the parent node.
     */
    public Path getParentPath() {
        return parentPath;
    }

    /**
     * Returns the UUID of a child node operation.
     * If this <code>EventState</code> was generated for a property
     * operation this method returns <code>null</code>.
     *
     * @return the UUID of a child node operation.
     */
    public String getChildUUID() {
        return childUUID;
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
     * Returns the node type of the parent node associated with this event.
     *
     * @return the node type of the parent associated with this event.
     */
    public NodeTypeImpl getNodeType() {
        return nodeType;
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
     * @param eventType an event type defined by {@link Event}.
     * @return a String representation of <code>eventType</code>.
     */
    public static String valueOf(int eventType) {
        if (eventType == Event.NODE_ADDED) {
            return "NodeAdded";
        } else if (eventType == Event.NODE_REMOVED) {
            return "NodeRemoved";
        } else if (eventType == Event.PROPERTY_ADDED) {
            return "PropertyAdded";
        } else if (eventType == Event.PROPERTY_CHANGED) {
            return "PropertyChanged";
        } else if (eventType == Event.PROPERTY_REMOVED) {
            return "PropertyRemoved";
        } else {
            return "UnknownEventType";
        }
    }

}
