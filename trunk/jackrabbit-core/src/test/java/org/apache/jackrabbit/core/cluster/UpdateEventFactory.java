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
package org.apache.jackrabbit.core.cluster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.Session;
import javax.jcr.observation.Event;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.cluster.SimpleEventListener.UpdateEvent;
import org.apache.jackrabbit.core.observation.EventState;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;

/**
 * Simplistic factory that produces update events, consisting of node identifiers,
 * property identifiers and event states.
 */
public class UpdateEventFactory {

    /**
     * Root node id.
     */
    private static final NodeId ROOT_NODE_ID = RepositoryImpl.ROOT_NODE_ID;

    /**
     * Default user.
     */
    private static final String DEFAULT_USER = "admin";

    /**
     * Instance of this class.
     */
    private static final UpdateEventFactory INSTANCE = new UpdateEventFactory();

    /**
     * Default session, used for event state creation.
     */
    private final Session session = new ClusterSession(DEFAULT_USER);

    /**
     * Name factory.
     */
    private NameFactory nameFactory = NameFactoryImpl.getInstance();

    /**
     * Path factory.
     */
    private PathFactory pathFactory = PathFactoryImpl.getInstance();

    /**
     * Create a new instance of this class. Private as there is only one
     * instance acting as singleton.
     */
    private UpdateEventFactory() {
    }

    /**
     * Return the instance of this class.
     *
     * @return factory
     */
    public static UpdateEventFactory getInstance() {
        return INSTANCE;
    }

    /**
     * Create an update operation.
     *
     * @return update operation
     */
    public UpdateEvent createUpdateOperation() {
        NodeState n1 = createNodeState();
        NodeState n2 = createNodeState();
        NodeState n3 = createNodeState();
        PropertyState p1 = createPropertyState(n1.getNodeId(), "{}a");
        PropertyState p2 = createPropertyState(n2.getNodeId(), "{}b");

        ChangeLog changes = new ChangeLog();
        changes.added(n1);
        changes.added(p1);
        changes.deleted(p2);
        changes.modified(n2);
        changes.deleted(n3);

        List events = new ArrayList();
        events.add(createEventState(n1, Event.NODE_ADDED, "{}n1", session));
        events.add(createEventState(p1, n1, Event.PROPERTY_ADDED, session));
        events.add(createEventState(p2, n2, Event.PROPERTY_REMOVED, session));
        events.add(createEventState(n3, Event.NODE_REMOVED, "{}n3", session));

        return new UpdateEvent(changes, events, System.currentTimeMillis(), "user-data");
    }

    /**
     * Create an update operation.
     *
     * @return update operation
     */
    public UpdateEvent createUpdateOperationWithNullUserId() {
        NodeState n1 = createNodeState();
        NodeState n2 = createNodeState();
        NodeState n3 = createNodeState();
        PropertyState p1 = createPropertyState(n1.getNodeId(), "{}a");
        PropertyState p2 = createPropertyState(n2.getNodeId(), "{}b");

        ChangeLog changes = new ChangeLog();
        changes.added(n1);
        changes.added(p1);
        changes.deleted(p2);
        changes.modified(n2);
        changes.deleted(n3);

        Session s = new ClusterSession(null);
        List events = new ArrayList();
        events.add(createEventState(n1, Event.NODE_ADDED, "{}n1", s));
        events.add(createEventState(p1, n1, Event.PROPERTY_ADDED, s));
        events.add(createEventState(p2, n2, Event.PROPERTY_REMOVED, s));
        events.add(createEventState(n3, Event.NODE_REMOVED, "{}n3", s));

        return new UpdateEvent(changes, events, System.currentTimeMillis(), "user-data");
    }


    /**
     * Create a node state.
     *
     * @return node state
     */
    protected NodeState createNodeState() {
        Name ntName = nameFactory.create("{}testnt");
        NodeState n = new NodeState(
                NodeId.randomId(), ntName,
                ROOT_NODE_ID, NodeState.STATUS_EXISTING, false);
        n.setMixinTypeNames(Collections.EMPTY_SET);
        return n;
    }

    /**
     * Create a property state.
     *
     * @param parentId parent node id
     * @param name property name
     * @return property state.
     */
    protected PropertyState createPropertyState(NodeId parentId, String name) {
        Name propName = nameFactory.create(name);
        return new PropertyState(
                new PropertyId(parentId, propName),
                NodeState.STATUS_EXISTING, false);
    }

    /**
     * Create an event state for an operation on a node.
     *
     * @param n node state
     * @param type <code>Event.NODE_ADDED</code> or <code>Event.NODE_REMOVED</code>
     * @param name node name
     * @param session the session that produced the event.
     * @return event state
     */
    protected EventState createEventState(NodeState n, int type, String name,
                                          Session session) {
        Path relPath = pathFactory.create(nameFactory.create(name));

        switch (type) {
        case Event.NODE_ADDED:
            return EventState.childNodeAdded(
                    n.getParentId(), pathFactory.getRootPath(),
                    n.getNodeId(), relPath, n.getNodeTypeName(),
                    n.getMixinTypeNames(), session);
        case Event.NODE_REMOVED:
            return EventState.childNodeRemoved(
                    n.getParentId(), pathFactory.getRootPath(),
                    n.getNodeId(), relPath, n.getNodeTypeName(),
                    n.getMixinTypeNames(), session);
        }
        return null;
    }

    /**
     * Create an event state for a property operation.
     *
     * @param p property state
     * @param parent parent node state
     * @param type <code>Event.NODE_ADDED</code> or <code>Event.NODE_REMOVED</code>
     * @param session the session that produces the event.
     * @return event state
     */
    protected EventState createEventState(PropertyState p, NodeState parent, int type,
                                          Session session) {
        Path relPath = pathFactory.create(p.getName());

        switch (type) {
        case Event.PROPERTY_ADDED:
            return EventState.propertyAdded(
                    p.getParentId(), pathFactory.getRootPath(), relPath,
                    parent.getNodeTypeName(), parent.getMixinTypeNames(),
                    session);
        case Event.PROPERTY_CHANGED:
            return EventState.propertyChanged(
                    p.getParentId(), pathFactory.getRootPath(), relPath,
                    parent.getNodeTypeName(), parent.getMixinTypeNames(),
                    session);
        case Event.PROPERTY_REMOVED:
            return EventState.propertyRemoved(
                    p.getParentId(), pathFactory.getRootPath(), relPath,
                    parent.getNodeTypeName(), parent.getMixinTypeNames(),
                    session);
        }
        return null;
    }
}
