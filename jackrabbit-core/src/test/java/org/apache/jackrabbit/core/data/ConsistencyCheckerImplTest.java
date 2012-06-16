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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.persistence.bundle.AbstractBundlePersistenceManager;
import org.apache.jackrabbit.core.persistence.bundle.ConsistencyCheckerImpl;
import org.apache.jackrabbit.core.persistence.check.ConsistencyChecker;
import org.apache.jackrabbit.core.persistence.util.BLOBStore;
import org.apache.jackrabbit.core.persistence.util.NodePropBundle;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;

public class ConsistencyCheckerImplTest extends TestCase {

    private static final Logger log = LoggerFactory.getLogger(ConsistencyCheckerImplTest.class);

    private static final NameFactory nameFactory = NameFactoryImpl.getInstance();

    // Abandoned nodes are nodes that have a link to a parent but that
    // parent does not have a link back to the child
    public void testFixAbandonedNode() throws RepositoryException {
        NodePropBundle bundle1 = new NodePropBundle(new NodeId(0, 0));
        NodePropBundle bundle2 = new NodePropBundle(new NodeId(0, 1));

        // node2 has a reference to node 1 as its parent, but node 1 doesn't have
        // a corresponding child node entry
        bundle2.setParentId(bundle1.getId());

        MockPersistenceManager pm = new MockPersistenceManager(Arrays.asList(bundle1, bundle2));
        ConsistencyCheckerImpl checker = new ConsistencyCheckerImpl(pm, null);

        // run checker with fix = true
        checker.check(null, false, true, null);

        // node1 should now have a child node entry for node2
        bundle1 = pm.loadBundle(bundle1.getId());
        assertEquals(1, bundle1.getChildNodeEntries().size());
        assertEquals(bundle2.getId(), bundle1.getChildNodeEntries().get(0).getId());
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
        ConsistencyCheckerImpl checker = new ConsistencyCheckerImpl(pm, null);

        // run checker with fix = true
        checker.check(null, false, true, null);

        // node1 should now have child node entries for node2 and node3
        bundle1 = pm.loadBundle(bundle1.getId());
        assertEquals(2, bundle1.getChildNodeEntries().size());
        assertEquals(bundle2.getId(), bundle1.getChildNodeEntries().get(0).getId());
        assertEquals(bundle3.getId(), bundle1.getChildNodeEntries().get(1).getId());
    }

    // Orphaned nodes are those nodes who's parent does not exist
    public void testAddOrphanedNodeToLostAndFound() throws RepositoryException {
        NodePropBundle lostAndFound = new NodePropBundle(new NodeId(0, 0));
        // lost and found must be of type nt:unstructured
        lostAndFound.setNodeTypeName(NameConstants.NT_UNSTRUCTURED);

        NodePropBundle orphaned = new NodePropBundle(new NodeId(0, 1));
        // set non-existent parent node id
        orphaned.setParentId(new NodeId(1, 0));

        MockPersistenceManager pm = new MockPersistenceManager(Arrays.asList(lostAndFound, orphaned));
        ConsistencyCheckerImpl checker = new ConsistencyCheckerImpl(pm, null);

        // run checker with fix = true
        checker.check(null, false, true, lostAndFound.getId().toString());

        // orphan should have been added to lost+found
        lostAndFound = pm.loadBundle(lostAndFound.getId());
        assertEquals(1, lostAndFound.getChildNodeEntries().size());
        assertEquals(orphaned.getId(), lostAndFound.getChildNodeEntries().get(0).getId());

        orphaned = pm.loadBundle(orphaned.getId());
        assertEquals(lostAndFound.getId(), orphaned.getParentId());
    }

    // Disconnected nodes are those nodes for which there are nodes
    // that have the node as its child, but the node itself does not
    // have those nodes as its parent
    public void testFixDisconnectedNode() throws RepositoryException {
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
        ConsistencyCheckerImpl checker = new ConsistencyCheckerImpl(pm, null);

        // run checker with fix = true
        checker.check(null, false, true, null);

        bundle1 = pm.loadBundle(bundle1.getId());
        bundle2 = pm.loadBundle(bundle2.getId());
        bundle3 = pm.loadBundle(bundle3.getId());

        // node3 should have been removed as child node entry of node1
        assertEquals(0, bundle1.getChildNodeEntries().size());

        // node3 should still be a child of node2
        assertEquals(1, bundle2.getChildNodeEntries().size());
        assertEquals(bundle2.getId(), bundle3.getParentId());
    }

    // make sure we don't fix anything in check mode, we can't be careful enough
    public void testDontFixInCheckMode() throws RepositoryException {
        /** abandoned node, also see {@link #testFixAbandonedNode} */
        NodePropBundle bundle1 = new NodePropBundle(new NodeId(0, 0));
        NodePropBundle bundle2 = new NodePropBundle(new NodeId(0, 1));
        bundle2.setParentId(bundle1.getId());

        /** orphaned node, also see {@link #testAddOrphanedNodeToLostAndFound} */
        NodePropBundle lostAndFound = new NodePropBundle(new NodeId(1, 0));
        lostAndFound.setNodeTypeName(NameConstants.NT_UNSTRUCTURED);
        NodePropBundle orphaned = new NodePropBundle(new NodeId(1, 1));
        orphaned.setParentId(new NodeId(0, 2));

        /** disconnected node, also see {@link #testFixDisconnectedNode} */
        NodePropBundle bundle3 = new NodePropBundle(new NodeId(1, 2));
        NodePropBundle bundle4 = new NodePropBundle(new NodeId(2, 2));
        NodePropBundle bundle5 = new NodePropBundle(new NodeId(0, 3));
        bundle3.addChildNodeEntry(nameFactory.create("", "test"), bundle5.getId());
        bundle4.addChildNodeEntry(nameFactory.create("", "test"), bundle5.getId());
        bundle5.setParentId(bundle4.getId());

        MockPersistenceManager pm = new MockPersistenceManager(Arrays.asList(bundle1, bundle2, bundle3, bundle4, bundle5, lostAndFound, orphaned));
        ConsistencyCheckerImpl checker = new ConsistencyCheckerImpl(pm, null);

        // run checker with fix = false
        checker.check(null, false, false, null);

        bundle1 = pm.loadBundle(bundle1.getId());
        lostAndFound = pm.loadBundle(lostAndFound.getId());
        bundle3 = pm.loadBundle(bundle3.getId());

        // abandoned node should not have been un-abandoned
        assertEquals(0, bundle1.getChildNodeEntries().size());

        // orphan should not have been added to lost and found
        assertEquals(0, lostAndFound.getChildNodeEntries().size());

        // disconnected entries should not have been removed
        assertEquals(1, bundle3.getChildNodeEntries().size());

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
}