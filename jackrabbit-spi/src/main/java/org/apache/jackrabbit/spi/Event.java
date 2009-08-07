/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.spi;

/**
 * <code>Event</code> is similar to the regular JCR Event and adds additional
 * information about the affected item.
 */
public interface Event {

    /**
     * An event of this type is generated when a node is added.
     */
    public static final int NODE_ADDED = javax.jcr.observation.Event.NODE_ADDED;

    /**
     * An event of this type is generated when a node is removed.
     */
    public static final int NODE_REMOVED = javax.jcr.observation.Event.NODE_REMOVED;

    /**
     * An event of this type is generated when a property is added.
     */
    public static final int PROPERTY_ADDED = javax.jcr.observation.Event.PROPERTY_ADDED;

    /**
     * An event of this type is generated when a property is removed.
     */
    public static final int PROPERTY_REMOVED = javax.jcr.observation.Event.PROPERTY_REMOVED;

    /**
     * An event of this type is generated when a property is changed.
     */
    public static final int PROPERTY_CHANGED = javax.jcr.observation.Event.PROPERTY_CHANGED;

    /**
     * Constant for observation listener interested in all types of events.
     */
    public static final int ALL_TYPES = Event.NODE_ADDED | Event.NODE_REMOVED |
    Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED;

    /**
     * Returns the type of this event: a constant defined by this interface.
     * One of:
     * <ul>
     * <li><code>{@link #NODE_ADDED}</code></li>
     * <li><code>{@link #NODE_REMOVED}</code></li>
     * <li><code>{@link #PROPERTY_ADDED}</code></li>
     * <li><code>{@link #PROPERTY_REMOVED}</code></li>
     * <li><code>{@link #PROPERTY_CHANGED}</code></li>
     * </ul>
     *
     * @return the type of this event.
     */
    public int getType();

    /**
     * @return the path of the affected item. E.g. the added/removed node or the
     *         property that was added/removed/changed.
     */
    public Path getPath();

    /**
     * @return the id of the affected item.
     */
    public ItemId getItemId();

    /**
     * @return the id of the parent node of the affected item.
     */
    public NodeId getParentId();

    /**
     * @return the name of the primary node type of the 'associated' node of
     *         this event.
     * @see javax.jcr.observation.ObservationManager#addEventListener
     */
    public Name getPrimaryNodeTypeName();

    /**
     * @return the names of the mixin types of the 'associated' node of this
     *         event.
     * @see javax.jcr.observation.ObservationManager#addEventListener
     */
    public Name[] getMixinTypeNames();

    /**
     * Returns the user ID connected with this event. This is the string
     * returned by getUserID of the session that caused the event.
     *
     * @return a <code>String</code>.
     */
    public String getUserID();
}
