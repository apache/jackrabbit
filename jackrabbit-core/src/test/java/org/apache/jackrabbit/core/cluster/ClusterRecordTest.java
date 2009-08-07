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
import java.util.Properties;

import org.apache.jackrabbit.core.NodeId;
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
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
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
     * Default sync delay: 5 seconds.
     */
    private static final long SYNC_DELAY = 5000;

    /**
     * Update event factory.
     */
    private final UpdateEventFactory factory = UpdateEventFactory.getInstance();

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
        UpdateEvent update = factory.createUpdateOperation();

        UpdateEventChannel channel = master.createUpdateChannel(DEFAULT_WORKSPACE);
        channel.updateCreated(update);
        channel.updatePrepared(update);
        channel.updateCommitted(update, null);

        SimpleEventListener listener = new SimpleEventListener();
        slave.createUpdateChannel(DEFAULT_WORKSPACE).setListener(listener);
        slave.sync();

        assertEquals(1, listener.getClusterEvents().size());
        assertEquals(listener.getClusterEvents().get(0), update);
    }

    /**
     * Test producing and consuming an update with a null userId
     */
    public void testUpdateOperationWithNullUserId() throws Exception {
        UpdateEvent update = factory.createUpdateOperationWithNullUserId();

        UpdateEventChannel channel = master.createUpdateChannel(DEFAULT_WORKSPACE);
        channel.updateCreated(update);
        channel.updatePrepared(update);
        channel.updateCommitted(update, null);

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
}
