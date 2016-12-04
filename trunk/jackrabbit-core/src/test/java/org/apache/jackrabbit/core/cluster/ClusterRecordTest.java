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

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.cluster.SimpleEventListener.LockEvent;
import org.apache.jackrabbit.core.cluster.SimpleEventListener.NamespaceEvent;
import org.apache.jackrabbit.core.cluster.SimpleEventListener.NodeTypeEvent;
import org.apache.jackrabbit.core.cluster.SimpleEventListener.PrivilegeEvent;
import org.apache.jackrabbit.core.cluster.SimpleEventListener.UnlockEvent;
import org.apache.jackrabbit.core.cluster.SimpleEventListener.UpdateEvent;
import org.apache.jackrabbit.core.config.ClusterConfig;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.journal.Journal;
import org.apache.jackrabbit.core.journal.JournalFactory;
import org.apache.jackrabbit.core.journal.MemoryJournal;
import org.apache.jackrabbit.core.journal.MemoryJournal.MemoryRecord;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.PrivilegeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.spi.commons.nodetype.QNodeTypeDefinitionBuilder;
import org.apache.jackrabbit.spi.commons.privilege.PrivilegeDefinitionImpl;
import org.apache.jackrabbit.test.JUnitTest;

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
    private ArrayList<MemoryRecord> records = new ArrayList<MemoryRecord>();

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
    @Override
    protected void setUp() throws Exception {
        master = createClusterNode("master", records);
        master.start();

        slave = createClusterNode("slave", records);

        super.setUp();
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
        LockEvent event = new LockEvent(NodeId.randomId(), true, "admin");

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
        UnlockEvent event = new UnlockEvent(NodeId.randomId());

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
        QNodeTypeDefinitionBuilder ntd = new QNodeTypeDefinitionBuilder();
        ntd.setName(NameFactoryImpl.getInstance().create("", "test"));
        ntd.setSupertypes(new Name[]{NameConstants.NT_BASE});

        ArrayList<QNodeTypeDefinition> list = new ArrayList<QNodeTypeDefinition>();
        list.add(ntd.build());

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
        QNodeTypeDefinitionBuilder ntd = new QNodeTypeDefinitionBuilder();
        ntd.setName(NameFactoryImpl.getInstance().create("", "test"));
        ntd.setSupertypes(new Name[]{NameConstants.NT_BASE});

        ArrayList<QNodeTypeDefinition> list = new ArrayList<QNodeTypeDefinition>();
        list.add(ntd.build());

        NodeTypeEvent event = new NodeTypeEvent(NodeTypeEvent.REREGISTER, list);
        master.reregistered(ntd.build());

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

        ArrayList<Name> list = new ArrayList<Name>();
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
     * Test producing and consuming a privilege registration.
     * @throws Exception
     */
    public void testPrivilegeRegistration() throws Exception {
        PrivilegeDefinition pdf = new PrivilegeDefinitionImpl(NameFactoryImpl.getInstance().create("", "test"), false, null);

        PrivilegeEvent event = new PrivilegeEvent(Collections.singletonList(pdf));
        master.registeredPrivileges(event.getDefinitions());

        SimpleEventListener listener = new SimpleEventListener();
        slave.setListener((PrivilegeEventListener) listener);
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
    private ClusterNode createClusterNode(
            String id, ArrayList<MemoryRecord> records) throws Exception {
        final MemoryJournal journal = new MemoryJournal();
        JournalFactory jf = new JournalFactory() {
            public Journal getJournal(NamespaceResolver resolver)
                    throws RepositoryException {
                return journal;
            }
        };
        ClusterConfig cc = new ClusterConfig(id, SYNC_DELAY, jf);
        SimpleClusterContext context = new SimpleClusterContext(cc);

        journal.setRepositoryHome(context.getRepositoryHome());
        journal.init(id, context.getNamespaceResolver());
        if (records != null) {
            journal.setRecords(records);
        }

        ClusterNode clusterNode = new ClusterNode();
        clusterNode.init(context);
        return clusterNode;
    }
}
