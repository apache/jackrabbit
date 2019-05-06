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
package org.apache.jackrabbit.core.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.cluster.ClusterException;
import org.apache.jackrabbit.core.cluster.ClusterNode;
import org.apache.jackrabbit.core.cluster.SimpleClusterContext;
import org.apache.jackrabbit.core.cluster.UpdateEventChannel;
import org.apache.jackrabbit.core.cluster.UpdateEventListener;
import org.apache.jackrabbit.core.config.ClusterConfig;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.journal.Journal;
import org.apache.jackrabbit.core.journal.JournalFactory;
import org.apache.jackrabbit.core.journal.MemoryJournal;
import org.apache.jackrabbit.core.journal.MemoryJournal.MemoryRecord;
import org.apache.jackrabbit.core.observation.EventState;
import org.apache.jackrabbit.core.persistence.bundle.AbstractBundlePersistenceManager;
import org.apache.jackrabbit.core.persistence.bundle.ConsistencyCheckerImpl;
import org.apache.jackrabbit.core.persistence.check.ReportItem;
import org.apache.jackrabbit.core.persistence.util.BLOBStore;
import org.apache.jackrabbit.core.persistence.util.NodePropBundle;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;

import junit.framework.TestCase;

public class ConsistencyCheckerImplTest extends TestCase {

    private static final NameFactory nameFactory = NameFactoryImpl.getInstance();

    /** Default sync delay: 5 seconds. */
    private static final long SYNC_DELAY = 5000;

    private List<MemoryRecord> records = new ArrayList<MemoryRecord>();

    private ClusterNode master;
    private ClusterNode slave;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        master = createClusterNode("master");
        master.start();

        slave = createClusterNode("slave");
        slave.start();
    }


    // Abandoned nodes are nodes that have a link to a parent but that
    // parent does not have a link back to the child
    public void testFixAbandonedNode() throws RepositoryException, ClusterException {
        NodePropBundle bundle1 = new NodePropBundle(new NodeId(0, 0));
        NodePropBundle bundle2 = new NodePropBundle(new NodeId(0, 1));

        // node2 has a reference to node 1 as its parent, but node 1 doesn't have
        // a corresponding child node entry
        bundle2.setParentId(bundle1.getId());

        MockPersistenceManager pm = new MockPersistenceManager(Arrays.asList(bundle1, bundle2));
        ConsistencyCheckerImpl checker = new ConsistencyCheckerImpl(pm, null, null, master.createUpdateChannel("default"));

        // set up cluster event update listener
        final TestUpdateEventListener listener = new TestUpdateEventListener();
        final UpdateEventChannel slaveEventChannel = slave.createUpdateChannel("default");
        slaveEventChannel.setListener(listener);

        checker.check(null, false);

        Set<ReportItem> reportItems = checker.getReport().getItems();
        assertEquals(1, reportItems.size());
        ReportItem reportItem = reportItems.iterator().next();
        assertEquals(ReportItem.Type.ABANDONED, reportItem.getType());
        assertEquals(bundle2.getId().toString(), reportItem.getNodeId());

        checker.repair();

        // node1 should now have a child node entry for node2
        bundle1 = pm.loadBundle(bundle1.getId());
        assertEquals(1, bundle1.getChildNodeEntries().size());
        assertEquals(bundle2.getId(), bundle1.getChildNodeEntries().get(0).getId());

        slave.sync();

        // verify events were correctly broadcast to cluster
        assertNotNull("Cluster node did not receive update event", listener.changes);
        assertTrue("Expected node1 to be modified", listener.changes.isModified(bundle1.getId()));
    }

    public void testDoubleCheckAbandonedNode() throws RepositoryException {
        NodePropBundle bundle1 = new NodePropBundle(new NodeId(0, 0));
        NodePropBundle bundle2 = new NodePropBundle(new NodeId(0, 1));

        // node2 has a reference to node 1 as its parent, but node 1 doesn't have
        // a corresponding child node entry
        bundle2.setParentId(bundle1.getId());

        MockPersistenceManager pm = new MockPersistenceManager(Arrays.asList(bundle1, bundle2));
        ConsistencyCheckerImpl checker = new ConsistencyCheckerImpl(pm, null, null, null);

        checker.check(null, false);

        Set<ReportItem> reportItems = checker.getReport().getItems();
        assertEquals(1, reportItems.size());
        ReportItem reportItem = reportItems.iterator().next();
        assertEquals(ReportItem.Type.ABANDONED, reportItem.getType());
        assertEquals(bundle2.getId().toString(), reportItem.getNodeId());

        checker.doubleCheckErrors();

        assertFalse("Double check removed valid error", checker.getReport().getItems().isEmpty());

        // fix the error
        bundle1.addChildNodeEntry(nameFactory.create("", "test"), bundle2.getId());

        checker.doubleCheckErrors();

        assertTrue("Double check didn't remove invalid error", checker.getReport().getItems().isEmpty());
    }

    /*
     * There was a bug where when there were multiple abandoned nodes by the same parent
     * only one of them was fixed. Hence this separate test case for this scenario.
     */
    public void testFixMultipleAbandonedNodesBySameParent() throws RepositoryException {
        NodePropBundle bundle1 = new NodePropBundle(new NodeId(0, 0));
        NodePropBundle bundle2 = new NodePropBundle(new NodeId(0, 1));
        NodePropBundle bundle3 = new NodePropBundle(new NodeId(1, 0));

        // node2 and node3 have a reference to node1 as its parent, but node1 doesn't have
        // corresponding child node entries
        bundle2.setParentId(bundle1.getId());
        bundle3.setParentId(bundle1.getId());

        MockPersistenceManager pm = new MockPersistenceManager(Arrays.asList(bundle1, bundle2, bundle3));
        ConsistencyCheckerImpl checker = new ConsistencyCheckerImpl(pm, null, null, null);

        checker.check(null, false);
        checker.repair();

        // node1 should now have child node entries for node2 and node3
        bundle1 = pm.loadBundle(bundle1.getId());
        assertEquals(2, bundle1.getChildNodeEntries().size());
        assertEquals(bundle2.getId(), bundle1.getChildNodeEntries().get(0).getId());
        assertEquals(bundle3.getId(), bundle1.getChildNodeEntries().get(1).getId());
    }

    // Orphaned nodes are those nodes who's parent does not exist
    public void testAddOrphanedNodeToLostAndFound() throws RepositoryException, ClusterException {
        final NodeId lostAndFoundId = new NodeId(0, 0);
        NodePropBundle lostAndFound = new NodePropBundle(lostAndFoundId);
        // lost and found must be of type nt:unstructured
        lostAndFound.setNodeTypeName(NameConstants.NT_UNSTRUCTURED);

        final NodeId orphanedId = new NodeId(0, 1);
        NodePropBundle orphaned = new NodePropBundle(orphanedId);
        // set non-existent parent node id
        orphaned.setParentId(new NodeId(1, 0));

        MockPersistenceManager pm = new MockPersistenceManager(Arrays.asList(lostAndFound, orphaned));
        ConsistencyCheckerImpl checker = new ConsistencyCheckerImpl(pm, null, lostAndFoundId.toString(),
                master.createUpdateChannel("default"));

        // set up cluster event update listener
        final TestUpdateEventListener listener = new TestUpdateEventListener();
        final UpdateEventChannel slaveEventChannel = slave.createUpdateChannel("default");
        slaveEventChannel.setListener(listener);

        checker.check(null, false);

        Set<ReportItem> reportItems = checker.getReport().getItems();
        assertEquals(1, reportItems.size());
        ReportItem reportItem = reportItems.iterator().next();
        assertEquals(ReportItem.Type.ORPHANED, reportItem.getType());
        assertEquals(orphanedId.toString(), reportItem.getNodeId());

        checker.repair();

        // orphan should have been added to lost+found
        lostAndFound = pm.loadBundle(lostAndFoundId);
        assertEquals(1, lostAndFound.getChildNodeEntries().size());
        assertEquals(orphanedId, lostAndFound.getChildNodeEntries().get(0).getId());

        orphaned = pm.loadBundle(orphanedId);
        assertEquals(lostAndFoundId, orphaned.getParentId());

        slave.sync();

        // verify events were correctly broadcast to cluster
        assertNotNull("Cluster node did not receive update event", listener.changes);
        assertTrue("Expected lostAndFound to be modified", listener.changes.isModified(lostAndFoundId));
        assertTrue("Expected orphan to be modified", listener.changes.isModified(orphanedId));
    }

    public void testEmptyRepo() throws RepositoryException {
        MockPersistenceManager pm = new MockPersistenceManager(Collections.emptyList());
        ConsistencyCheckerImpl checker = new ConsistencyCheckerImpl(pm, null, null, null);
        checker.check(null, false);
    }

    public void testDoubleCheckOrphanedNode() throws RepositoryException {
        NodePropBundle orphaned = new NodePropBundle(new NodeId(0, 1));
        orphaned.setParentId(new NodeId(1, 0));

        MockPersistenceManager pm = new MockPersistenceManager(Arrays.asList(orphaned));
        ConsistencyCheckerImpl checker = new ConsistencyCheckerImpl(pm, null, null, null);

        checker.check(null, false);

        Set<ReportItem> reportItems = checker.getReport().getItems();
        assertEquals(1, reportItems.size());
        ReportItem reportItem = reportItems.iterator().next();
        assertEquals(ReportItem.Type.ORPHANED, reportItem.getType());
        assertEquals(orphaned.getId().toString(), reportItem.getNodeId());

        checker.doubleCheckErrors();

        assertFalse("Double check removed valid error", checker.getReport().getItems().isEmpty());

        // fix the error
        NodePropBundle parent = new NodePropBundle(orphaned.getParentId());
        pm.bundles.put(parent.getId(), parent);

        checker.doubleCheckErrors();

        assertTrue("Double check didn't remove invalid error", checker.getReport().getItems().isEmpty());
    }

    // Disconnected nodes are those nodes for which there are nodes
    // that have the node as its child, but the node itself does not
    // have those nodes as its parent
    public void testFixDisconnectedNode() throws RepositoryException, ClusterException {
        NodePropBundle bundle1 = new NodePropBundle(new NodeId(0, 0));
        NodePropBundle bundle2 = new NodePropBundle(new NodeId(0, 1));
        NodePropBundle bundle3 = new NodePropBundle(new NodeId(1, 0));

        // node1 has child node3
        bundle1.addChildNodeEntry(nameFactory.create("", "test"), bundle3.getId());
        // node2 also has child node3
        bundle2.addChildNodeEntry(nameFactory.create("", "test"), bundle3.getId());
        // node3 has node2 as parent
        bundle3.setParentId(bundle2.getId());

        MockPersistenceManager pm = new MockPersistenceManager(Arrays.asList(bundle1, bundle2, bundle3));
        ConsistencyCheckerImpl checker = new ConsistencyCheckerImpl(pm, null, null, master.createUpdateChannel("default"));

        // set up cluster event update listener
        final TestUpdateEventListener listener = new TestUpdateEventListener();
        final UpdateEventChannel slaveEventChannel = slave.createUpdateChannel("default");
        slaveEventChannel.setListener(listener);

        checker.check(null, false);

        Set<ReportItem> reportItems = checker.getReport().getItems();
        assertEquals(1, reportItems.size());
        ReportItem reportItem = reportItems.iterator().next();
        assertEquals(ReportItem.Type.DISCONNECTED, reportItem.getType());
        assertEquals(bundle1.getId().toString(), reportItem.getNodeId());

        checker.repair();

        bundle1 = pm.loadBundle(bundle1.getId());
        bundle2 = pm.loadBundle(bundle2.getId());
        bundle3 = pm.loadBundle(bundle3.getId());

        // node3 should have been removed as child node entry of node1
        assertEquals(0, bundle1.getChildNodeEntries().size());

        // node3 should still be a child of node2
        assertEquals(1, bundle2.getChildNodeEntries().size());
        assertEquals(bundle2.getId(), bundle3.getParentId());

        slave.sync();

        // verify events were correctly broadcast to cluster
        assertNotNull("Cluster node did not receive update event", listener.changes);
        assertTrue("Expected node1 to be modified", listener.changes.isModified(bundle1.getId()));
    }

    public void testDoubleCheckDisonnectedNode() throws RepositoryException {
        NodePropBundle bundle1 = new NodePropBundle(new NodeId(0, 0));
        NodePropBundle bundle2 = new NodePropBundle(new NodeId(0, 1));
        NodePropBundle bundle3 = new NodePropBundle(new NodeId(1, 0));

        // node1 has child node3
        bundle1.addChildNodeEntry(nameFactory.create("", "test"), bundle3.getId());
        // node2 also has child node3
        bundle2.addChildNodeEntry(nameFactory.create("", "test"), bundle3.getId());
        // node3 has node2 as parent
        bundle3.setParentId(bundle2.getId());

        MockPersistenceManager pm = new MockPersistenceManager(Arrays.asList(bundle1, bundle2, bundle3));
        ConsistencyCheckerImpl checker = new ConsistencyCheckerImpl(pm, null, null, null);

        checker.check(null, false);

        Set<ReportItem> reportItems = checker.getReport().getItems();
        assertEquals(1, reportItems.size());
        ReportItem reportItem = reportItems.iterator().next();
        assertEquals(ReportItem.Type.DISCONNECTED, reportItem.getType());
        assertEquals(bundle1.getId().toString(), reportItem.getNodeId());

        checker.doubleCheckErrors();

        assertFalse("Double check removed valid error", checker.getReport().getItems().isEmpty());

        // fix the error
        bundle1.getChildNodeEntries().remove(0);

        checker.doubleCheckErrors();

        assertTrue("Double check didn't remove invalid error", checker.getReport().getItems().isEmpty());
    }

    public void testFixMissingNode() throws RepositoryException, ClusterException {
        NodePropBundle bundle = new NodePropBundle(new NodeId(0, 0));
        bundle.addChildNodeEntry(nameFactory.create("", "test"), new NodeId(0, 1));

        MockPersistenceManager pm = new MockPersistenceManager(Arrays.asList(bundle));

        ConsistencyCheckerImpl checker = new ConsistencyCheckerImpl(pm, null, null, master.createUpdateChannel("default"));

        // set up cluster event update listener
        final TestUpdateEventListener listener = new TestUpdateEventListener();
        final UpdateEventChannel slaveEventChannel = slave.createUpdateChannel("default");
        slaveEventChannel.setListener(listener);

        checker.check(null, false);

        Set<ReportItem> reportItems = checker.getReport().getItems();
        assertEquals(1, reportItems.size());
        ReportItem reportItem = reportItems.iterator().next();
        assertEquals(ReportItem.Type.MISSING, reportItem.getType());
        assertEquals(bundle.getId().toString(), reportItem.getNodeId());

        checker.repair();

        // node should have no child no entries
        assertTrue(bundle.getChildNodeEntries().isEmpty());

        slave.sync();

        // verify events were correctly broadcast to cluster
        assertNotNull("Cluster node did not receive update event", listener.changes);
        assertTrue("Expected node to be modified", listener.changes.isModified(bundle.getId()));
    }

    public void testDoubleCheckMissingNode() throws RepositoryException {
        NodePropBundle bundle = new NodePropBundle(new NodeId(0, 0));
        final NodeId childNodeId = new NodeId(0, 1);
        bundle.addChildNodeEntry(nameFactory.create("", "test"), childNodeId);

        MockPersistenceManager pm = new MockPersistenceManager(Arrays.asList(bundle));

        ConsistencyCheckerImpl checker = new ConsistencyCheckerImpl(pm, null, null, null);

        checker.check(null, false);

        Set<ReportItem> reportItems = checker.getReport().getItems();
        assertEquals(1, reportItems.size());
        ReportItem reportItem = reportItems.iterator().next();
        assertEquals(ReportItem.Type.MISSING, reportItem.getType());
        assertEquals(bundle.getId().toString(), reportItem.getNodeId());

        checker.doubleCheckErrors();

        assertFalse("Double check removed valid error", checker.getReport().getItems().isEmpty());

        // fix the error
        NodePropBundle child = new NodePropBundle(childNodeId);
        pm.bundles.put(childNodeId, child);

        checker.doubleCheckErrors();

        assertTrue("Double check didn't remove invalid error", checker.getReport().getItems().isEmpty());

    }

    private ClusterNode createClusterNode(String id) throws Exception {
        final MemoryJournal journal = new MemoryJournal() {
            protected boolean syncAgainOnNewRecords() {
                return true;
            }
        };
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
        journal.setRecords(records);

        ClusterNode clusterNode = new ClusterNode();
        clusterNode.init(context);
        return clusterNode;
    }

    private static class MockPersistenceManager extends AbstractBundlePersistenceManager {

        private Map<NodeId, NodePropBundle> bundles = new LinkedHashMap<NodeId, NodePropBundle>();

        private MockPersistenceManager(List<NodePropBundle> bundles) {
            for (NodePropBundle bundle : bundles) {
                this.bundles.put(bundle.getId(), bundle);
            }
        }

        public List<NodeId> getAllNodeIds(final NodeId after, final int maxCount) throws ItemStateException, RepositoryException {
            List<NodeId> allNodeIds = new ArrayList<NodeId>();
            boolean add = after == null;
            for (NodeId nodeId : bundles.keySet()) {
                if (add) {
                    allNodeIds.add(nodeId);
                }
                if (!add) {
                    add = nodeId.equals(after);
                }
            }
            return allNodeIds;
        }

        @Override
        protected NodePropBundle loadBundle(final NodeId id) {
            return bundles.get(id);
        }

        @Override
        public boolean exists(final NodeId id) {
            return bundles.containsKey(id);
        }

        @Override
        protected void evictBundle(final NodeId id) {
        }

        @Override
        protected void storeBundle(final NodePropBundle bundle) throws ItemStateException {
            bundles.put(bundle.getId(), bundle);
        }

        @Override
        protected void destroyBundle(final NodePropBundle bundle) throws ItemStateException {
            bundles.remove(bundle.getId());
        }

        @Override
        protected void destroy(final NodeReferences refs) throws ItemStateException {
        }

        @Override
        protected void store(final NodeReferences refs) throws ItemStateException {
        }

        @Override
        protected BLOBStore getBlobStore() {
            return null;
        }

        public NodeReferences loadReferencesTo(final NodeId id) throws NoSuchItemStateException, ItemStateException {
            return null;
        }

        public boolean existsReferencesTo(final NodeId targetId) throws ItemStateException {
            return false;
        }
    }

    private static class TestUpdateEventListener implements UpdateEventListener {

        private ChangeLog changes;

        @Override
        public void externalUpdate(final ChangeLog changes, final List<EventState> events, final long timestamp, final String userData) throws RepositoryException {
            this.changes = changes;
        }
    }
}