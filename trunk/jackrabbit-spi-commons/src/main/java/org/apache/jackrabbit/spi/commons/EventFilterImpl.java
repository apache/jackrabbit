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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.Event;
import org.apache.jackrabbit.spi.EventFilter;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.Path;

/**
 * <code>EventFilterImpl</code> is the simple bean style implementation of an
 * {@link EventFilter}.
 */
public class EventFilterImpl implements EventFilter, Serializable {

    private final int eventTypes;

    private final boolean isDeep;

    private final Path absPath;

    private final Set<String> uuids;

    private final Set<Name> nodeTypeNames;

    private final boolean noLocal;

    /**
     * Creates a new <code>EventFilterImpl</code>.
     *
     * @param eventTypes    the event types this filter is interested in.
     * @param absPath       filter events that are below this path.
     * @param isDeep        whether this filter is applied deep.
     * @param uuids         the jcr:uuid of the nodes this filter allows.
     * @param nodeTypeNames the Names of the already resolved node types this
     *                      filter allows.
     * @param noLocal       whether this filter accepts local events or not.
     */
    public EventFilterImpl(int eventTypes,
                    Path absPath,
                    boolean isDeep,
                    String[] uuids,
                    Set<Name> nodeTypeNames,
                    boolean noLocal) {
        this.eventTypes = eventTypes;
        this.absPath = absPath;
        this.isDeep = isDeep;
        this.uuids = uuids != null ? new HashSet<String>(Arrays.asList(uuids)) : null;
        this.nodeTypeNames = nodeTypeNames != null ? new HashSet<Name>(nodeTypeNames) : null;
        this.noLocal = noLocal;
    }

    /**
     * {@inheritDoc}
     */
    public boolean accept(Event event, boolean isLocal) {
        int type = event.getType();
        // check type
        if ((type & eventTypes) == 0) {
            return false;
        }

        // check local flag
        if (isLocal && noLocal) {
            return false;
        }

        // UUIDs, types, and paths do not need to match for persist
        if (event.getType() == Event.PERSIST) {
            return true;
        }

        // check UUIDs
        NodeId parentId = event.getParentId();
        if (uuids != null) {
            if (parentId.getPath() == null) {
                if (!uuids.contains(parentId.getUniqueID())) {
                    return false;
                }
            } else {
                return false;
            }
        }

        // check node types
        if (nodeTypeNames != null) {
            Set<Name> eventTypes = new HashSet<Name>();
            eventTypes.addAll(Arrays.asList(event.getMixinTypeNames()));
            eventTypes.add(event.getPrimaryNodeTypeName());
            // create intersection
            eventTypes.retainAll(nodeTypeNames);
            if (eventTypes.isEmpty()) {
                return false;
            }
        }

        // finally check path
        try {
            Path eventPath = event.getPath().getAncestor(1);
            boolean match = eventPath.equals(absPath);
            if (!match && isDeep) {
                match = eventPath.isDescendantOf(absPath);
            }
            return match;
        } catch (RepositoryException e) {
            // should never get here
        }
        // if we get here an exception occurred while checking for the path
        return false;
    }

    /**
     * @return the event types this event filter accepts.
     */
    public int getEventTypes() {
        return eventTypes;
    }

    /**
     * @return <code>true</code> if this event filter is deep.
     */
    public boolean isDeep() {
        return isDeep;
    }

    /**
     * @return the path to the item where events are filtered.
     */
    public Path getAbsPath() {
        return absPath;
    }

    /**
     * @return the uuids of the nodes of this filter or <code>null</code> if
     *         this filter does not care about uuids.
     */
    public String[] getUUIDs() {
        if (uuids == null) {
            return null;
        } else {
            return uuids.toArray(new String[uuids.size()]);
        }
    }

    /**
     * @return an unmodifiable set of node type names or <code>null</code> if
     *         this filter does not care about node types.
     */
    public Set<Name> getNodeTypeNames() {
        if (nodeTypeNames == null) {
            return null;
        } else {
            return Collections.unmodifiableSet(nodeTypeNames);
        }
    }

    /**
     * @return if this filter accepts local events.
     */
    public boolean getNoLocal() {
        return noLocal;
    }

    /**
     * Returns a string representation of this EventFilter instance.
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return new StringBuffer(getClass().getName())
            .append("[")
            .append("eventTypes: ").append(eventTypes).append(", ")
            .append("absPath: ").append(absPath).append(", ")
            .append("isDeep: ").append(isDeep).append(", ")
            .append("uuids: ").append(uuids).append(", ")
            .append("nodeTypeNames: ").append(nodeTypeNames).append(", ")
            .append("noLocal: ").append(noLocal)
            .append("]")
            .toString();
    }

}
