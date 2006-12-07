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
package org.apache.jackrabbit.spi2dav;

import org.apache.jackrabbit.spi.EventFilter;
import org.apache.jackrabbit.spi.Event;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.MalformedPathException;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.PathNotFoundException;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

/**
 * <code>EventFilterImpl</code> is the spi2dav implementation of an {@link
 * EventFilter}.
 */
class EventFilterImpl implements EventFilter {

    /**
     * The logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(EventFilterImpl.class);

    private final int eventTypes;

    private final boolean isDeep;

    private final Path absPath;

    private final Set uuids;

    private final Set nodeTypeNames;

    private final boolean noLocal;

    /**
     * Creates a new <code>EventFilterImpl</code>.
     *
     * @param eventTypes    the event types this filter is interested in.
     * @param absPath       filter events that are below this path.
     * @param isDeep        whether this filter is applied deep.
     * @param uuids         the jcr:uuid of the nodes this filter allows.
     * @param nodeTypeNames the QNames of the already resolved node types this
     *                      filter allows.
     * @param noLocal       whether this filter accepts local events or not.
     */
    EventFilterImpl(int eventTypes,
                    Path absPath,
                    boolean isDeep,
                    String[] uuids,
                    Set nodeTypeNames,
                    boolean noLocal) {
        this.eventTypes = eventTypes;
        this.absPath = absPath;
        this.isDeep = isDeep;
        this.uuids = uuids != null ? new HashSet(Arrays.asList(uuids)) : null;
        this.nodeTypeNames = nodeTypeNames != null ? new HashSet(nodeTypeNames) : null;
        this.noLocal = noLocal;
    }

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
            Set eventTypes = new HashSet();
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
            // the relevant path for the path filter depends on the event type
            // for node events, the relevant path is the one returned by
            // Event.getPath().
            // for property events, the relevant path is the path of the
            // node where the property belongs to.
            Path eventPath;
            if (type == Event.NODE_ADDED || type == Event.NODE_REMOVED) {
                eventPath = event.getQPath();
            } else {
                eventPath = event.getQPath().getAncestor(1);
            }

            boolean match = eventPath.equals(absPath);
            if (!match && isDeep) {
                match = eventPath.isDescendantOf(absPath);
            }
            return match;
        } catch (MalformedPathException e) {
            // should never get here
            log.warn("malformed path: " + e);
            log.debug("Exception: ", e);
        } catch (PathNotFoundException e) {
            // should never get here
            log.warn("invalid property path: " + e);
            log.debug("Exception: ", e);
        }
        // if we get here an exception occurred while checking for the path
        return false;
    }
}
