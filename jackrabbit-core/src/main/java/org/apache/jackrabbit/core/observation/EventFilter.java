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
package org.apache.jackrabbit.core.observation;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.observation.Event;

import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.spi.Path;

/**
 * The <code>EventFilter</code> class implements the filter logic based
 * on the session's access rights and the specified filter rules.
 */
public class EventFilter {

    static final EventFilter BLOCK_ALL = new BlockAllFilter();

    /**
     * The session this EventFilter belongs to.
     */
    private final SessionImpl session;

    /**
     * This <code>EventFilter</code> should only allow events with the
     * specified types.
     */
    private final long eventTypes;

    /**
     * Only allow Items with the specified <code>paths</code>
     */
    private final List<Path> paths;

    /**
     * If <code>isDeep</code> is <code>true</code> also Items under <code>absPath</code>
     * are allowed.
     */
    private final boolean isDeep;

    /**
     * Only allow Nodes with the specified <code>uuids</code>.
     */
    private final NodeId[] ids;

    /**
     * Only allow Nodes with the specified {@link javax.jcr.nodetype.NodeType}s.
     */
    private final NodeTypeImpl[] nodeTypes;

    /**
     * If <code>noLocal</code> is true this filter will block events from
     * the session that registered this filter.
     */
    private final boolean noLocal;

    /**
     * If <code>noExternal</code> is true this filter will block events from
     * other cluster nodes.
     */
    private final boolean noExternal;

    /**
     * If <code>noInternal</code> is true this filter will block events from
     * this cluster nodes.
     */
    private final boolean noInternal;

    /**
     * Creates a new <code>EventFilter</code> instance.
     *
     * @param session    the <code>Session</code> that registered the {@link
     *                   javax.jcr.observation.EventListener}.
     * @param eventTypes only allow specified {@link javax.jcr.observation.Event} types.
     * @param paths      only allow {@link javax.jcr.Item} with a path in
     *                   <code>paths</code>.
     * @param isDeep     if <code>true</code> also allow events for {@link
     *                   javax.jcr.Item}s below <code>absPath</code>.
     * @param ids        only allow events for {@link javax.jcr.Node}s with
     *                   specified NodeIDs. If <code>null</code> is passed no
     *                   restriction regarding NodeIds is applied.
     * @param nodeTypes  only allow events for specified {@link
     *                   javax.jcr.nodetype.NodeType}s. If <code>null</code> no
     *                   node type restriction is applied.
     * @param noLocal    if <code>true</code> no events are allowed that were
     *                   created from changes related to the <code>Session</code>
     *                   that registered the {@link javax.jcr.observation.EventListener}.
     * @param noExternal if <code>true</code> no events are allowed that were
     *                   created from changes on an external cluster node.
     * @param noInternal if <code>true</code> no events are allowed that were
     *                   created from changes on the local cluster node.
     */
    EventFilter(SessionImpl session,
                long eventTypes,
                List<Path> paths,
                boolean isDeep,
                NodeId[] ids,
                NodeTypeImpl[] nodeTypes,
                boolean noLocal,
                boolean noExternal,
                boolean noInternal) {
        this.session = session;
        this.eventTypes = eventTypes;
        this.paths = paths;
        this.isDeep = isDeep;
        this.ids = ids;
        this.noLocal = noLocal;
        this.noExternal = noExternal;
        this.noInternal = noInternal;
        this.nodeTypes = nodeTypes;
    }

    /**
     * Returns <code>true</code> if this <code>EventFilter</code> does not allow
     * the specified <code>EventState</code>; <code>false</code> otherwise.
     *
     * @param eventState the <code>EventState</code> in question.
     * @return <code>true</code> if this <code>EventFilter</code> blocks the
     *         <code>EventState</code>.
     * @throws RepositoryException if an error occurs while checking.
     */
    boolean blocks(EventState eventState) throws RepositoryException {
        // first do cheap checks

        // check event type
        long type = eventState.getType();
        if ((eventTypes & type) == 0) {
            return true;
        }

        // check for session local changes
        if (noLocal && session.equals(eventState.getSession())) {
            // listener does not wish to get local events
            return true;
        }

        if (noExternal && eventState.isExternal()) {
            return true;
        }

        if (noInternal && !eventState.isExternal()) {
            return true;
        }

        // UUIDs, types, and paths do not need to match for persist
        if (eventState.getType() == Event.PERSIST) {
            return false;
        }

        // check UUIDs
        NodeId parentId = eventState.getParentId();
        if (ids != null) {
            boolean match = false;
            for (int i = 0; i < ids.length && !match; i++) {
                match |= parentId.equals(ids[i]);
            }
            if (!match) {
                return true;
            }
        }

        // check node types
        if (nodeTypes != null) {
            Set<NodeType> eventTypes = eventState.getNodeTypes(session.getNodeTypeManager());
            boolean match = false;
            for (int i = 0; i < nodeTypes.length && !match; i++) {
                for (NodeType eventType : eventTypes) {
                    NodeTypeImpl nodeType = (NodeTypeImpl) eventType;
                    match |= nodeType.getQName().equals(nodeTypes[i].getQName())
                            || nodeType.isDerivedFrom(nodeTypes[i].getQName());
                }
            }
            if (!match) {
                return true;
            }
        }

        // finally check paths
        Path eventPath = eventState.getParentPath();
        boolean match = false;
        for (Path path : paths) {
            if (eventPath.equals(path) || isDeep && eventPath.isDescendantOf(path)) {
                match = true;
                break;
            }
        }

        return !match;
    }

    /**
     * This class implements an <code>EventFilter</code> that blocks
     * all {@link EventState}s.
     */
    private static final class BlockAllFilter extends EventFilter {

        /**
         * Creates a new <code>BlockAllFilter</code>.
         */
        BlockAllFilter() {
            super(null, 0, Collections.<Path>emptyList(), true, null, null, true, true, true);
        }

        /**
         * Always return <code>true</code>.
         *
         * @return always <code>true</code>.
         */
        @Override
        boolean blocks(EventState eventState) {
            return true;
        }
    }

}
