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
import java.util.Properties;

import javax.jcr.Session;
import javax.jcr.observation.Event;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.cluster.SimpleEventListener.LockEvent;
import org.apache.jackrabbit.core.cluster.SimpleEventListener.NamespaceEvent;
import org.apache.jackrabbit.core.cluster.SimpleEventListener.NodeTypeEvent;
import org.apache.jackrabbit.core.cluster.SimpleEventListener.UnlockEvent;
import org.apache.jackrabbit.core.cluster.SimpleEventListener.UpdateEvent;
import org.apache.jackrabbit.core.config.BeanConfig;
import org.apache.jackrabbit.core.config.ClusterConfig;
import org.apache.jackrabbit.core.config.JournalConfig;
import org.apache.jackrabbit.core.journal.MemoryJournal;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.observation.EventState;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.test.JUnitTest;
import org.apache.jackrabbit.uuid.UUID;

/**
 * Test cases for cluster record production and consumption. Verifies that
 * cluster record serialization and deseralization is correct.
 */
public class ClusterRecordTest extends JUnitTest {

    /**
     * Defaut workspace name.
     */
    private static final String DEFAULT_WORKSPACE = "default";

    /**
     * Default user.
     */
    private static final String DEFAULT_USER = "admin";

    /**
     * Root node id.
     */
    private static final NodeId ROOT_NODE_ID = RepositoryImpl.ROOT_NODE_ID;

    /**
     * Default sync delay: 5 seconds.
     */
    private static final long SYNC_DELAY = 5000;

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
     * Records shared among multiple memory journals.
     */
    private ArrayList records = new ArrayList();

    /**
     * Master.
     */
    private ClusterNode master;

    /**
     * Slave.
     */
    private ClusterNode slave;

    /**
     * {@inheritDoc}
     */
    protected void setUp() throws Exception {
        master = createClusterNode("master", records);
        master.start();

        slave = createClusterNode("slave", records);

        super.setUp();
    }

    /**
     * {@inheritDoc}
     */
    protected void tearDown() throws Exception {
        if (master != null) {
            master.stop();
        }
        if (slave != null) {
            slave.stop();
        }
        super.tearDown();
    }

    /**
     * Test producing and consuming an update.
     * @throws Exception
     */
    public void testUpdateOperation() throws Exception {
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
        events.add(createEventState(n1, Event.NODE_ADDED, "{}n1"));
        events.add(createEventState(p1, n1, Event.PROPERTY_ADDED));
        events.add(createEventState(p2, n2, Event.PROPERTY_REMOVED));
        events.add(createEventState(n3, Event.NODE_REMOVED, "{}n3"));

        UpdateEvent update = new UpdateEvent(changes, events);

        UpdateEventChannel channel = master.createUpdateChannel(DEFAULT_WORKSPACE);
        channel.updateCreated(update);
        channel.updatePrepared(update);
        channel.updateCommitted(update);

        SimpleEventListener listener = new SimpleEventListener();
        slave.createUpdateChannel(DEFAULT_WORKSPACE).setListener(listener);
        slave.sync();

        assertEquals(1, listener.getClusterEvents().size());
        assertEquals(listener.getClusterEvents().get(0), update);
    }

    /**
     * Test producing and consuming a lock operation.
     * @throws Exception
     */
    public void testLockOperation() throws Exception {
        LockEvent event = new LockEvent(new NodeId(UUID.randomUUID()), true, "admin");

        master.createLockChannel(DEFAULT_WORKSPACE).create(event.getNodeId(),
                event.isDeep(), event.getUserId()).ended(true);

        SimpleEventListener listener = new SimpleEventListener();
        slave.createLockChannel(DEFAULT_WORKSPACE).setListener(listener);
        slave.sync();

        assertEquals(1, listener.getClusterEvents().size());
        assertEquals(listener.getClusterEvents().get(0), event);
    }

    /**
     * Test producing and consuming an unlock operation.
     * @throws Exception
     */
    public void testUnlockOperation() throws Exception {
        UnlockEvent event = new UnlockEvent(new NodeId(UUID.randomUUID()));

        master.createLockChannel(DEFAULT_WORKSPACE).create(event.getNodeId()).ended(true);

        SimpleEventListener listener = new SimpleEventListener();
        slave.createLockChannel(DEFAULT_WORKSPACE).setListener(listener);
        slave.sync();

        assertEquals(1, listener.getClusterEvents().size());
        assertEquals(listener.getClusterEvents().get(0), event);
    }

    /**
     * Test producing and consuming a node type registration.
     * @throws Exception
     */
    public void testNodeTypeRegistration() throws Exception {
        NodeTypeDef ntd = new NodeTypeDef();
        ntd.setName(NameFactoryImpl.getInstance().create("", "test"));
        ntd.setSupertypes(new Name[]{NameConstants.NT_BASE});

        ArrayList list = new ArrayList();
        list.add(ntd);

        NodeTypeEvent event = new NodeTypeEvent(NodeTypeEvent.REGISTER, list);
        master.registered(event.getCollection());

        SimpleEventListener listener = new SimpleEventListener();
        slave.setListener((NodeTypeEventListener) listener);
        slave.sync();

        assertEquals(1, listener.getClusterEvents().size());
        assertEquals(listener.getClusterEvents().get(0), event);
    }

    /**
     * Test producing and consuming a node type reregistration.
     * @throws Exception
     */
    public void testNodeTypeReregistration() throws Exception {
        NodeTypeDef ntd = new NodeTypeDef();
        ntd.setName(NameFactoryImpl.getInstance().create("", "test"));
        ntd.setSupertypes(new Name[]{NameConstants.NT_BASE});

        ArrayList list = new ArrayList();
        list.add(ntd);

        NodeTypeEvent event = new NodeTypeEvent(NodeTypeEvent.REREGISTER, list);
        master.reregistered(ntd);

        SimpleEventListener listener = new SimpleEventListener();
        slave.setListener((NodeTypeEventListener) listener);
        slave.sync();

        assertEquals(1, listener.getClusterEvents().size());
        assertEquals(listener.getClusterEvents().get(0), event);
    }

    /**
     * Test producing and consuming a node type unregistration.
     * @throws Exception
     */
    public void testNodeTypeUnregistration() throws Exception {
        Name name = NameFactoryImpl.getInstance().create("", "test");

        ArrayList list = new ArrayList();
        list.add(name);

        NodeTypeEvent event = new NodeTypeEvent(NodeTypeEvent.UNREGISTER, list);
        master.unregistered(list);

        SimpleEventListener listener = new SimpleEventListener();
        slave.setListener((NodeTypeEventListener) listener);
        slave.sync();

        assertEquals(1, listener.getClusterEvents().size());
        assertEquals(listener.getClusterEvents().get(0), event);
    }

    /**
     * Test producing and consuming a namespace registration.
     * @throws Exception
     */
    public void testNamespaceRegistration() throws Exception {
        NamespaceEvent event = new NamespaceEvent(null, "test", "http://www.test.com");

        master.remapped(event.getOldPrefix(), event.getNewPrefix(), event.getUri());

        SimpleEventListener listener = new SimpleEventListener();
        slave.setListener((NamespaceEventListener) listener);
        slave.sync();

        assertEquals(1, listener.getClusterEvents().size());
        assertEquals(listener.getClusterEvents().get(0), event);
    }

    /**
     * Test producing and consuming a namespace unregistration.
     * @throws Exception
     */
    public void testNamespaceUnregistration() throws Exception {
        NamespaceEvent event = new NamespaceEvent("test", null, null);

        master.remapped(event.getOldPrefix(), event.getNewPrefix(), event.getUri());

        SimpleEventListener listener = new SimpleEventListener();
        slave.setListener((NamespaceEventListener) listener);
        slave.sync();

        assertEquals(1, listener.getClusterEvents().size());
        assertEquals(listener.getClusterEvents().get(0), event);
    }

    /**
     * Create a cluster node, with a memory journal referencing a list of records.
     *
     * @param id cluster node id
     * @param records memory journal's list of records
     */
    private ClusterNode createClusterNode(String id, ArrayList records)
            throws ClusterException {

        BeanConfig bc = new BeanConfig(MemoryJournal.class.getName(), new Properties());
        JournalConfig jc = new JournalConfig(bc);
        ClusterConfig cc = new ClusterConfig(id, SYNC_DELAY, jc);
        SimpleClusterContext context = new SimpleClusterContext(cc);

        ClusterNode clusterNode = new ClusterNode();
        clusterNode.init(context);
        if (records != null) {
            ((MemoryJournal) clusterNode.getJournal()).setRecords(records);
        }
        return clusterNode;
    }

    /**
     * Create a node state.
     *
     * @return node state
     */
    private NodeState createNodeState() {
        Name ntName = nameFactory.create("{}testnt");
        NodeState n = new NodeState(
                new NodeId(UUID.randomUUID()), ntName,
                ROOT_NODE_ID, NodeState.STATUS_EXISTING, false);
        n.setMixinTypeNames(Collections.EMPTY_SET);
        return n;
    }

    /**
     * Create a property state.
     *
     * @param parentId parent node id
     * @param name property name
     */
    private PropertyState createPropertyState(NodeId parentId, String name) {
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
     * @return event state
     */
    private EventState createEventState(NodeState n, int type, String name) {
        Path.Element relPath = pathFactory.createElement(nameFactory.create(name));

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
     * @param n node state
     * @param type <code>Event.NODE_ADDED</code> or <code>Event.NODE_REMOVED</code>
     * @param name property name
     * @return event state
     */
    private EventState createEventState(PropertyState p, NodeState parent, int type) {
        Path.Element relPath = pathFactory.createElement(p.getName());

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
