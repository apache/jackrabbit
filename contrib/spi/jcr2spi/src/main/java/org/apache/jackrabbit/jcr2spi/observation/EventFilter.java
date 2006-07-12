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
package org.apache.jackrabbit.jcr2spi.observation;

import org.apache.jackrabbit.jcr2spi.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.jcr2spi.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.jcr2spi.nodetype.NodeTypeConflictException;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.Event;
import org.apache.jackrabbit.name.Path;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;

/**
 * The <code>EventFilter</code> class implements the filter logic based
 * on the session's access rights and the specified filter rules.
 */
class EventFilter {

    /**
     * Logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(EventFilter.class);

    static final EventFilter BLOCK_ALL = new BlockAllFilter();

    /**
     * The namespaceResolver of the session this EventFilter belongs to.
     */
    private final NamespaceResolver nsResolver;

    /**
     * The node type registry.
     */
    private final NodeTypeRegistry ntReg;

    /**
     * This <code>EventFilter</code> should only allow events with the
     * specified types.
     */
    private final long eventTypes;

    /**
     * Only allow Items with the specified <code>path</code>
     */
    private final Path path;

    /**
     * If <code>isDeep</code> is <code>true</code> also Items under <code>absPath</code>
     * are allowed.
     */
    private final boolean isDeep;

    /**
     * Only allow Nodes with the specified <code>uuids</code>.
     */
    private final String[] uuids;

    /**
     * Only allow Nodes with the specified node type name.
     */
    private final QName[] nodeTypes;

    /**
     * If <code>noLocal</code> is true this filter will block events from
     * the session that registerd this filter.
     */
    private final boolean noLocal;

    /**
     * Creates a new <code>EventFilter</code> instance.
     *
     * @param nsResolver <code>NamespaceResolver</code> attached to the
     *                   <code>Session</code> that registered the {@link
     *                   javax.jcr.observation.EventListener}.
     * @param ntReg      the node type registry.
     * @param eventTypes only allow specified {@link javax.jcr.observation.Event} types.
     * @param path       only allow {@link javax.jcr.Item} with
     *                   <code>path</code>.
     * @param isDeep     if <code>true</code> also allow events for {@link
     *                   Item}s below <code>absPath</code>.
     * @param uuids      only allow events for {@link javax.jcr.Node}s with
     *                   specified UUIDs. If <code>null</code> is passed no
     *                   restriction regarding UUIDs is applied.
     * @param nodeTypes  only allow events for specified node types named:
     *                   nodeTypes. If <code>null</code> no node type
     *                   restriction is applied.
     * @param noLocal    if <code>true</code> no events are allowed that were
     *                   created from changes related to the <code>Session</code>
     *                   that registered the {@link javax.jcr.observation.EventListener}.
     */
    EventFilter(NamespaceResolver nsResolver,
                NodeTypeRegistry ntReg,
                long eventTypes,
                Path path,
                boolean isDeep,
                String[] uuids,
                QName[] nodeTypes,
                boolean noLocal) {

        this.nsResolver = nsResolver;
        this.ntReg = ntReg;
        this.eventTypes = eventTypes;
        this.path = path;
        this.isDeep = isDeep;
        this.uuids = uuids;
        this.noLocal = noLocal;
        this.nodeTypes = nodeTypes;
    }

    /**
     * Returns the <code>NamespaceResolver</code> of the <code>Session</code>
     * associated with this <code>EventFilter</code>.
     *
     * @return the <code>Session</code> associated with this
     *         <code>EventFilter</code>.
     */
    NamespaceResolver getNamespaceResolver() {
        return nsResolver;
    }

    /**
     * Returns <code>true</code> if this <code>EventFilter</code> does not allow
     * the specified <code>Event</code>; <code>false</code> otherwise.
     *
     * @param event the <code>EventState</code> in question.
     * @param isLocal <code>true</code> if <code>event</code> is local.
     * @return <code>true</code> if this <code>EventFilter</code> blocks the
     *         <code>EventState</code>.
     * @throws RepositoryException if an error occurs while checking.
     */
    boolean blocks(Event event, boolean isLocal) throws RepositoryException {
        // first do cheap checks

        if (isLocal && noLocal) {
            return true;
        }

        // check event type
        long type = event.getType();
        if ((eventTypes & type) == 0) {
            return true;
        }

        // check UUIDs
        String uuid = event.getUUID();
        if (uuids != null) {
            boolean match = false;
            for (int i = 0; i < uuids.length && !match && uuid != null; i++) {
                match = uuids[i].equals(uuid);
            }
            if (!match) {
                return true;
            }
        }

        // check node types
        if (nodeTypes != null) {
            boolean match = false;
            QName[] mixinNames = event.getMixinTypeNames();
            QName[] typeNames = new QName[mixinNames.length + 1];
            System.arraycopy(mixinNames, 0, typeNames, 0, mixinNames.length);
            typeNames[typeNames.length - 1] = event.getPrimaryNodeTypeName();
            EffectiveNodeType effectiveNt;
            try {
                effectiveNt = ntReg.getEffectiveNodeType(typeNames);
            } catch (NodeTypeConflictException e) {
                log.error("Internal error: conflicting node types", e);
                // block this weird node
                return true;
            }
            for (int i = 0; i < nodeTypes.length && !match; i++) {
                match = effectiveNt.includesNodeType(nodeTypes[i]);
            }
            if (!match) {
                return true;
            }
        }

        // finally check path
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
        boolean match = eventPath.equals(path);
        if (!match && isDeep) {
            try {
                match = eventPath.isDescendantOf(path);
            } catch (org.apache.jackrabbit.name.MalformedPathException e) {
                e.printStackTrace();
            }
        }

        return !match;
    }



    /**
     * This class implements an <code>EventFilter</code> that blocks
     * all {@link Event}s.
     */
    private static final class BlockAllFilter extends EventFilter {

        /**
         * Creates a new <code>BlockAllFilter</code>.
         */
        BlockAllFilter() {
            super(null, null, 0, null, true, null, null, true);
        }

        /**
         * Always return <code>true</code>.
         *
         * @return always <code>true</code>.
         */
        boolean blocks(Event event, boolean isLocal) {
            return true;
        }
    }

}
