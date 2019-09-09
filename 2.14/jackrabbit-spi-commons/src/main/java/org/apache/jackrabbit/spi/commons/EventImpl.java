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
package org.apache.jackrabbit.spi.commons;

import org.apache.jackrabbit.spi.Event;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.QValue;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.Collections;
import java.util.HashMap;

/**
 * <code>EventImpl</code> implements a serializable SPI
 * {@link org.apache.jackrabbit.spi.Event}.
 */
public class EventImpl implements Event, Serializable {

    /**
     * The SPI event type.
     * @see Event
     */
    private final int type;

    /**
     * The path of the affected item.
     */
    private final Path path;

    /**
     * The id of the affected item.
     */
    private final ItemId itemId;

    /**
     * The id of the affected item.
     */
    private final NodeId parentId;

    /**
     * The name of the primary node type of the 'associated' node of this event.
     */
    private final Name primaryNodeTypeName;

    /**
     * The names of the mixin types of the 'associated' node of this event.
     */
    private final Name[] mixinTypeNames;

    /**
     * The user ID connected with this event.
     */
    private final String userId;

    private final String userData;
    private final long timestamp;
    private final Map<Name, QValue> info;

    /**
     * Creates a new serializable event.
     * @deprecated
     */
    public EventImpl(int type, Path path, ItemId itemId, NodeId parentId,
                     Name primaryNodeTypeName, Name[] mixinTypeNames,
                     String userId) {
        this(type, path, itemId, parentId, primaryNodeTypeName, mixinTypeNames, userId, null, Long.MIN_VALUE, Collections.EMPTY_MAP);
    }

    /**
     * Creates a new serializable event.
     */
    public EventImpl(int type, Path path, ItemId itemId, NodeId parentId,
                     Name primaryNodeTypeName, Name[] mixinTypeNames,
                     String userId, String userData, long timestamp,
                     Map<Name, QValue> info) {
        this.type = type;
        this.path = path;
        this.itemId = itemId;
        this.parentId = parentId;
        this.primaryNodeTypeName = primaryNodeTypeName;
        this.mixinTypeNames = mixinTypeNames;
        this.userId = userId;

        this.userData = userData;
        this.info = new HashMap<Name, QValue>(info);
        this.timestamp = timestamp;
    }

    //--------------------------------------------------------------< Event >---
    /**
     * {@inheritDoc}
     */
    public int getType() {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    public Path getPath() {
        return path;
    }

    /**
     * {@inheritDoc}
     */
    public ItemId getItemId() {
        return itemId;
    }

    /**
     * {@inheritDoc}
     */
    public NodeId getParentId() {
        return parentId;
    }

    /**
     * {@inheritDoc}
     */
    public Name getPrimaryNodeTypeName() {
        return primaryNodeTypeName;
    }

    /**
     * {@inheritDoc}
     */
    public Name[] getMixinTypeNames() {
        Name[] mixins = new Name[mixinTypeNames.length];
        System.arraycopy(mixinTypeNames, 0, mixins, 0, mixinTypeNames.length);
        return mixins;
    }

    /**
     * {@inheritDoc}
     */
    public String getUserID() {
        return userId;
    }

    /**
     * {@inheritDoc}
     */
    public Map<Name, QValue> getInfo() throws RepositoryException {
        return info;
    }

    /**
     * {@inheritDoc}
     */
    public String getUserData() {
        return userData;
    }

    /**
     * {@inheritDoc}
     */
    public long getDate() throws RepositoryException {
        if (timestamp == Long.MIN_VALUE) {
            throw new UnsupportedRepositoryOperationException("Event.getDate() not supported");
        } else {
            return timestamp;
        }
    }

    //-------------------------------------------------------------< Object >---
    @Override
    public String toString() {
        return new StringBuffer(getClass().getName())
            .append("[")
            .append("eventTypes: ").append(type).append(", ")
            .append("absPath: ").append(path).append(", ")
            .append("itemId: ").append(itemId).append(", ")
            .append("parentId: ").append(parentId).append(", ")
            .append("primaryNodeTypeName: ").append(primaryNodeTypeName).append(", ")
            .append("mixinTypeNames: ").append(Arrays.toString(mixinTypeNames)).append(", ")
            .append("userId").append(userId)
            .append("]")
            .toString();
    }
    
}
