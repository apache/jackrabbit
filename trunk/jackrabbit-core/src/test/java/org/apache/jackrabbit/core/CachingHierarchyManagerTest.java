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
package org.apache.jackrabbit.core;

import java.util.ArrayList;
import java.util.HashMap;

import junit.framework.TestCase;

import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.NodeStateListener;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;

public class CachingHierarchyManagerTest extends TestCase {

    volatile Exception exception;
    volatile boolean stop;
    CachingHierarchyManager cache;

    /**
     * Test multi-threaded read and write access to the cache.
     */
    public void testResolveNodePath() throws Exception {
        StaticItemStateManager ism = new StaticItemStateManager();
        cache = new CachingHierarchyManager(ism.getRootNodeId(), ism);
        ism.setContainer(cache);
        ism.addNode(ism.getRoot(), "a");
        ism.addNode(ism.getRoot(), "b");
        final Path aPath = toPath("/a");
        final Path bPath = toPath("/b");
        for (int i = 0; i < 3; i++) {
            new Thread(new Runnable() {
                public void run() {
                    while (!stop) {
                        try {
                            cache.resolveNodePath(aPath);
                            cache.resolveNodePath(bPath);
                        } catch (Exception e) {
                            exception = e;
                        }
                    }
                }
            }).start();
        }
        Thread.sleep(1000);
        stop = true;
        if (exception != null) {
            throw exception;
        }
    }

    //-------------------------------------------------------------- basic tests

    /**
     * Verify that resolving node and property paths will only return valid hits.
     */
    public void testResolveNodePropertyPath() throws Exception {
        StaticItemStateManager ism = new StaticItemStateManager();
        cache = new CachingHierarchyManager(ism.getRootNodeId(), ism);
        ism.setContainer(cache);
        NodeState a = ism.addNode(ism.getRoot(), "a");
        NodeState b = ism.addNode(a, "b");

        Path path = toPath("/a/b");

        // /a/b points to node only
        assertIsNodeId(cache.resolvePath(path));
        assertIsNodeId(cache.resolveNodePath(path));
        assertNull(cache.resolvePropertyPath(path));

        ism.addProperty(a, "b");

        // /a/b points to node and property
        assertNotNull(cache.resolvePath(path));
        assertIsNodeId(cache.resolveNodePath(path));
        assertIsPropertyId(cache.resolvePropertyPath(path));

        ism.removeNode(b);

        // /a/b points to property only
        assertIsPropertyId(cache.resolvePath(path));
        assertNull(cache.resolveNodePath(path));
        assertIsPropertyId(cache.resolvePropertyPath(path));
    }

    /**
     * Assert that an item id is a property id.
     * @param id item id
     */
    private static void assertIsPropertyId(ItemId id) {
        assertTrue(id instanceof PropertyId);
    }

    /**
     * Assert that an item id is a node id.
     * @param id item id
     */
    private static void assertIsNodeId(ItemId id) {
        assertTrue(id instanceof NodeId);
    }

    //------------------------------------------------------------ caching tests

    /**
     * Add a SNS (same name sibling) and verify that cached paths are
     * adapted accordingly.
     */
    public void testAddSNS() throws Exception {
        StaticItemStateManager ism = new StaticItemStateManager();
        cache = new CachingHierarchyManager(ism.getRootNodeId(), ism);
        ism.setContainer(cache);
        NodeState a = ism.addNode(ism.getRoot(), "a");
        NodeState b1 = ism.addNode(a, "b");
        Path path = cache.getPath(b1.getNodeId());
        assertEquals(toPath("/a/b"), path);
        NodeState b2 = ism.addNode(a, "b");
        ism.orderBefore(b2, b1);
        assertTrue(cache.isCached(b1.getNodeId(), null));
        path = cache.getPath(b1.getNodeId());
        assertEquals(toPath("/a/b[2]"), path);
    }

    /**
     * Clone a node, cache its path and remove it afterwards. Should remove
     * the cached path as well.
     */
    public void testCloneAndRemove() throws Exception {
        StaticItemStateManager ism = new StaticItemStateManager();
        cache = new CachingHierarchyManager(ism.getRootNodeId(), ism);
        ism.setContainer(cache);
        NodeState a1 = ism.addNode(ism.getRoot(), "a1");
        NodeState a2 = ism.addNode(ism.getRoot(), "a2");
        NodeState b1 = ism.addNode(a1, "b1");
        b1.addShare(b1.getParentId());
        ism.cloneNode(b1, a2, "b2");

        Path path1 = toPath("/a1/b1");
        Path path2 = toPath("/a2/b2");

        assertNotNull(cache.resolvePath(path1));
        assertTrue(cache.isCached(b1.getNodeId(), path1));

        ism.removeNode(b1);

        assertNull(cache.resolvePath(path1));
        assertNotNull(cache.resolvePath(path2));
    }

    /**
     * Clone a node, create a child and resolve its path in all valid
     * combinations. Then, move the child away. Should remove the cached
     * paths as well.
     */
    public void testCloneAndAddChildAndMove() throws Exception {
        StaticItemStateManager ism = new StaticItemStateManager();
        cache = new CachingHierarchyManager(ism.getRootNodeId(), ism);
        ism.setContainer(cache);
        NodeState a1 = ism.addNode(ism.getRoot(), "a1");
        NodeState a2 = ism.addNode(ism.getRoot(), "a2");
        NodeState b1 = ism.addNode(a1, "b1");
        b1.addShare(b1.getParentId());
        ism.cloneNode(b1, a2, "b2");
        NodeState c = ism.addNode(b1, "c");

        Path path1 = toPath("/a1/b1/c");
        Path path2 = toPath("/a2/b2/c");

        assertNotNull(cache.resolvePath(path1));
        assertTrue(cache.isCached(c.getNodeId(), path1));
        assertNotNull(cache.resolvePath(path2));
        assertTrue(cache.isCached(c.getNodeId(), path2));

        ism.moveNode(c, a1, "c");

        assertNull(cache.resolvePath(path1));
        assertNull(cache.resolvePath(path2));
    }

    /**
     * Move a node and verify that cached path is adapted.
     */
    public void testMove() throws Exception {
        StaticItemStateManager ism = new StaticItemStateManager();
        cache = new CachingHierarchyManager(ism.getRootNodeId(), ism);
        ism.setContainer(cache);
        NodeState a1 = ism.addNode(ism.getRoot(), "a1");
        NodeState a2 = ism.addNode(ism.getRoot(), "a2");
        NodeState b1 = ism.addNode(a1, "b1");
        Path path = cache.getPath(b1.getNodeId());
        assertEquals(toPath("/a1/b1"), path);
        ism.moveNode(b1, a2, "b2");
        path = cache.getPath(b1.getNodeId());
        assertEquals(toPath("/a2/b2"), path);
    }

    /**
     * Reorder child nodes and verify that cached paths are still adequate.
     */
    public void testOrderBefore() throws Exception {
        StaticItemStateManager ism = new StaticItemStateManager();
        cache = new CachingHierarchyManager(ism.getRootNodeId(), ism);
        ism.setContainer(cache);
        NodeState a = ism.addNode(ism.getRoot(), "a");
        NodeState b1 = ism.addNode(a, "b");
        NodeState b2 = ism.addNode(a, "b");
        NodeState b3 = ism.addNode(a, "b");
        Path path = cache.getPath(b1.getNodeId());
        assertEquals(toPath("/a/b"), path);
        ism.orderBefore(b2, b1);
        ism.orderBefore(b1, b3);
        path = cache.getPath(b1.getNodeId());
        assertEquals(toPath("/a/b[2]"), path);
    }

    /**
     * Remove a node and verify that cached path is gone.
     */
    public void testRemove() throws Exception {
        StaticItemStateManager ism = new StaticItemStateManager();
        cache = new CachingHierarchyManager(ism.getRootNodeId(), ism);
        ism.setContainer(cache);
        NodeState a = ism.addNode(ism.getRoot(), "a");
        NodeState b = ism.addNode(a, "b");
        NodeState c = ism.addNode(b, "c");
        cache.getPath(c.getNodeId());
        assertTrue(cache.isCached((NodeId) c.getId(), null));
        ism.removeNode(b);
        assertFalse(cache.isCached((NodeId) c.getId(), null));
    }

    /**
     * Remove a SNS (same name sibling) and verify that cached paths are
     * adapted accordingly. The removed SNS's path is not cached.
     */
    public void testRemoveSNS() throws Exception {
        StaticItemStateManager ism = new StaticItemStateManager();
        cache = new CachingHierarchyManager(ism.getRootNodeId(), ism);
        ism.setContainer(cache);
        NodeState a = ism.addNode(ism.getRoot(), "a");
        NodeState b1 = ism.addNode(a, "b");
        NodeState b2 = ism.addNode(a, "b");
        Path path = cache.getPath(b2.getNodeId());
        assertEquals(toPath("/a/b[2]"), path);
        ism.removeNode(b1);
        path = cache.getPath(b2.getNodeId());
        assertEquals(toPath("/a/b"), path);
    }

    /**
     * Remove a SNS (same name sibling) and verify that cached paths are
     * adapted accordingly. The removed SNS's path is cached.
     */
    public void testRemoveSNSWithCachedPath() throws Exception {
        StaticItemStateManager ism = new StaticItemStateManager();
        cache = new CachingHierarchyManager(ism.getRootNodeId(), ism);
        ism.setContainer(cache);
        NodeState a = ism.addNode(ism.getRoot(), "a");
        NodeState b1 = ism.addNode(a, "b");
        NodeState b2 = ism.addNode(a, "b");
        cache.getPath(b1.getNodeId());
        Path path = cache.getPath(b2.getNodeId());
        assertEquals(toPath("/a/b[2]"), path);
        ism.removeNode(b1);
        path = cache.getPath(b2.getNodeId());
        assertEquals(toPath("/a/b"), path);
    }

    /**
     * Rename a node and verify that cached path is adapted.
     */
    public void testRename() throws Exception {
        StaticItemStateManager ism = new StaticItemStateManager();
        cache = new CachingHierarchyManager(ism.getRootNodeId(), ism);
        ism.setContainer(cache);
        NodeState a1 = ism.addNode(ism.getRoot(), "a1");
        NodeState b1 = ism.addNode(a1, "b");
        NodeState b2 = ism.addNode(a1, "b");
        Path path = cache.getPath(b1.getNodeId());
        assertEquals(toPath("/a1/b"), path);
        path = cache.getPath(b2.getNodeId());
        assertEquals(toPath("/a1/b[2]"), path);
        ism.renameNode(b1, "b1");
        assertTrue(cache.isCached(b1.getNodeId(), null));
        assertTrue(cache.isCached(b2.getNodeId(), null));
        path = cache.getPath(b1.getNodeId());
        assertEquals(toPath("/a1/b1"), path);
    }

    /**
     * Static item state manager, that can be filled programmatically and that
     * keeps a hash map of item states. <code>ItemId</code>s generated by
     * this state manager start with <code>0</code>.
     */
    static class StaticItemStateManager implements ItemStateManager {

        /** Root node id */
        private final NodeId rootNodeId;

        /** Map of item states */
        private final HashMap states = new HashMap();

        /** UUID generator base */
        private long lsbGenerator;

        /** Root node state */
        private NodeState root;

        /** Node state listener to register in item states */
        private NodeStateListener listener;

        /**
         * Create a new instance of this class.
         */
        public StaticItemStateManager() {
            rootNodeId = nextNodeId();
        }

        /**
         * Return the root node id.
         *
         * @return root node id
         */
        public NodeId getRootNodeId() {
            return rootNodeId;
        }

        /**
         * Return the root node.
         *
         * @return root node
         */
        public NodeState getRoot() {
            if (root == null) {
                root = new NodeState(rootNodeId, NameConstants.JCR_ROOT,
                        null, NodeState.STATUS_EXISTING, false);
                if (listener != null) {
                    root.setContainer(listener);
                }
            }
            return root;
        }

        /**
         * Set the listener that should be registered in new item states.
         *
         * @param listener listener
         */
        public void setContainer(NodeStateListener listener) {
            this.listener = listener;
        }

        /**
         * Add a node.
         *
         * @param parent parent node
         * @param name node name
         * @return new node
         */
        public NodeState addNode(NodeState parent, String name) {
            NodeId id = nextNodeId();
            NodeState child = new NodeState(id, NameConstants.NT_UNSTRUCTURED,
                    parent.getNodeId(), NodeState.STATUS_EXISTING, false);
            if (listener != null) {
                child.setContainer(listener);
            }
            states.put(id, child);
            parent.addChildNodeEntry(toName(name), child.getNodeId());
            return child;
        }

        /**
         * Add a property.
         *
         * @param parent parent node
         * @param name property name
         * @return new property
         */
        public PropertyState addProperty(NodeState parent, String name) {
            PropertyId id = new PropertyId(parent.getNodeId(), toName(name));
            PropertyState child = new PropertyState(id,
                    PropertyState.STATUS_EXISTING, false);
            if (listener != null) {
                child.setContainer(listener);
            }
            states.put(id, child);
            parent.addPropertyName(toName(name));
            return child;
        }

        /**
         * Clone a node.
         *
         * @param src node to clone
         * @param parent destination parent node
         * @param name node name
         */
        public void cloneNode(NodeState src, NodeState parent, String name) {
            src.addShare(parent.getNodeId());
            parent.addChildNodeEntry(toName(name), src.getNodeId());
        }

        /**
         * Move a node.
         *
         * @param child node to move
         * @param newParent destination parent node
         * @param name node name
         * @throws ItemStateException if getting the old parent node fails
         */
        public void moveNode(NodeState child, NodeState newParent, String name)
                throws ItemStateException {

            NodeState oldParent = (NodeState) getItemState(child.getParentId());
            ChildNodeEntry cne = oldParent.getChildNodeEntry(child.getNodeId());
            if (cne == null) {
                throw new ItemStateException(child.getNodeId().toString());
            }
            oldParent.removeChildNodeEntry(cne.getName(), cne.getIndex());
            child.setParentId(newParent.getNodeId());
            newParent.addChildNodeEntry(toName(name), child.getNodeId());
        }

        /**
         * Order a child node before another node.
         *
         * @param src src node
         * @param dest destination node, may be <code>null</code>
         * @throws ItemStateException if getting the parent node fails
         */
        public void orderBefore(NodeState src, NodeState dest)
                throws ItemStateException {

            NodeState parent = (NodeState) getItemState(src.getParentId());

            ArrayList list = new ArrayList(parent.getChildNodeEntries());

            int srcIndex = -1, destIndex = -1;
            for (int i = 0; i < list.size(); i++) {
                ChildNodeEntry cne = (ChildNodeEntry) list.get(i);
                if (cne.getId().equals(src.getId())) {
                    srcIndex = i;
                } else if (dest != null && cne.getId().equals(dest.getId())) {
                    destIndex = i;
                }
            }
            if (destIndex == -1) {
                list.add(list.remove(srcIndex));
            } else {
                if (srcIndex < destIndex) {
                    list.add(destIndex, list.get(srcIndex));
                    list.remove(srcIndex);
                } else {
                    list.add(destIndex, list.remove(srcIndex));
                }
            }
            parent.setChildNodeEntries(list);
        }

        /**
         * Remove a node.
         *
         * @param child node to remove
         * @throws ItemStateException if getting the parent node fails
         */
        public void removeNode(NodeState child) throws ItemStateException {
            NodeState parent = (NodeState) getItemState(child.getParentId());
            if (child.isShareable()) {
                if (child.removeShare(parent.getNodeId()) == 0) {
                    child.setParentId(null);
                }
            }
            parent.removeChildNodeEntry(child.getNodeId());
        }

        /**
         * Rename a node.
         *
         * @param child node to rename
         * @param newName new name
         * @throws ItemStateException if getting the parent node fails
         */
        public void renameNode(NodeState child, String newName) throws ItemStateException {
            NodeState parent = (NodeState) getItemState(child.getParentId());
            ChildNodeEntry cne = parent.getChildNodeEntry(child.getNodeId());
            if (cne == null) {
                throw new ItemStateException(child.getNodeId().toString());
            }
            parent.renameChildNodeEntry(cne.getName(), cne.getIndex(), toName(newName));
        }

        /**
         * Return the next available node id. Simply increments the last UUID
         * returned by <code>1</code>.
         *
         * @return next UUID
         */
        private NodeId nextNodeId() {
            return new NodeId(0, lsbGenerator++);
        }

        //----------------------------------------------------- ItemStateManager

        /**
         * {@inheritDoc}
         */
        public ItemState getItemState(ItemId id)
                throws NoSuchItemStateException, ItemStateException {

            if (id.equals(root.getId())) {
                return root;
            }
            ItemState item = (ItemState) states.get(id);
            if (item == null) {
                throw new NoSuchItemStateException(id.toString());
            }
            return item;
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasItemState(ItemId id) {
            if (id.equals(root.getId())) {
                return true;
            }
            return states.containsKey(id);
        }

        /**
         * {@inheritDoc}
         */
        public NodeReferences getNodeReferences(NodeId id)
                throws NoSuchItemStateException, ItemStateException {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasNodeReferences(NodeId id) {
            return false;
        }
    }

    /**
     * Utility method, converting a string into a path.
     *
     * @param s string
     * @return path
     */
    private static Path toPath(String s) {
        StringBuffer buf = new StringBuffer("{}");
        int start = 1, length = s.length();
        while (start < length) {
            int end = s.indexOf('/', start);
            if (end == -1) {
                end = length;
            }
            String name = s.substring(start, end);
            if (name.length() > 0) {
                buf.append("\t{}");
                buf.append(name);
            }
            start = end + 1;
        }
        return PathFactoryImpl.getInstance().create(buf.toString());
    }

    /**
     * Utility method, converting a string into a name.
     *
     * @param s string
     * @return name
     */
    private static Name toName(String s) {
        return NameFactoryImpl.getInstance().create("", s);
    }
}
