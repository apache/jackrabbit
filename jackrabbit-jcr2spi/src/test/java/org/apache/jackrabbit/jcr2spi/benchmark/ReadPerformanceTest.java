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
package org.apache.jackrabbit.jcr2spi.benchmark;

import org.apache.jackrabbit.jcr2spi.AbstractJCR2SPITest;
import org.apache.jackrabbit.spi.ChildInfo;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.ItemInfo;
import org.apache.jackrabbit.spi.ItemInfoCache;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.NodeInfo;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.PropertyInfo;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.commons.ItemInfoBuilder.NodeInfoBuilder;
import org.apache.jackrabbit.spi.commons.ItemInfoBuilder.PropertyInfoBuilder;
import org.apache.jackrabbit.spi.commons.ItemInfoCacheImpl;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

import static org.apache.jackrabbit.spi.commons.iterator.Iterators.filterIterator;
import static org.apache.jackrabbit.spi.commons.iterator.Iterators.iteratorChain;
import static org.apache.jackrabbit.spi.commons.iterator.Iterators.singleton;

/**
 * Utility for testing jcr2spi read performance
 */
public class ReadPerformanceTest extends AbstractJCR2SPITest {

    /**
     * Depth of the content tree
     */
    private static int TREE_DEPTH = 3;

    /**
     * Number of child nodes of each internal node
     */
    private static int NODE_COUNT = 6;

    /**
     * Number of properties of each node
     */
    private static int PROPERTY_COUNT = 60;

    /**
     * Number of JCR operations to perform per run
     */
    private static int OP_COUNT = 500;

    /**
     * Size of the item info cache.
     * @see ItemInfoCache
     */
    private static int ITEM_INFO_CACHE_SIZE = 50000;

    /**
     * Ratios of the number of items in the whole content tree compared to the number of items
     * in a batch of a {@link RepositoryService#getItemInfos(SessionInfo, NodeId)} call.
     * The array contains one ratio per run
     */
    private static int[] BATCH_RATIOS = {1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384};

    /**
     * Valid paths of nodes in the mock repository
     */
    private final List<String> nodePaths = new ArrayList<String>();

    /**
     * Valid paths of properties in the mock repository
     */
    private final List<String> propertyPaths = new ArrayList<String>();

    private final Random rnd = new Random(12345);

    /**
     * This implementation overrides the default cache size with the value of
     * {@value #ITEM_INFO_CACHE_SIZE}
     */
    @Override
    public ItemInfoCache getItemInfoCache(SessionInfo sessionInfo) throws RepositoryException {
        return new ItemInfoCacheImpl(ITEM_INFO_CACHE_SIZE);
    }

    /**
     * This implementation adds a tree of nodes and properties up to certain {@link #TREE_DEPTH}.
     * Each node has {@link #NODE_COUNT} child nodes and {@link #PROPERTY_COUNT} properties.
     * {@inheritDoc}
     */
    @Override
    protected void initInfosStore(NodeInfoBuilder builder) throws RepositoryException {
        addNodes(builder, "");
    }

    private void addNodes(NodeInfoBuilder builder, String name) throws RepositoryException {
        if (name.length() >= TREE_DEPTH) {
            builder.build();
            nodePaths.add(toJCRPath(builder.getNodeInfo().getPath()));
            return;
        }

        for (int k = 0; k <= NODE_COUNT; k++) {
            String n = name + k;
            addNodes(addProperties(builder.createNodeInfo(n), PROPERTY_COUNT), n);
        }
        builder.build();
    }

    private NodeInfoBuilder addProperties(NodeInfoBuilder builder, int count) throws RepositoryException {
        for (int k = 0; k < count; k++) {
            PropertyInfoBuilder pBuilder = builder.createPropertyInfo("property_" + k, "Just some string value " + k);
            pBuilder.build();
            propertyPaths.add(toJCRPath(pBuilder.getPropertyInfo().getPath()));
        }

        return builder;
    }

    /**
     * Create <code>count</number> JCR operations for a <code>session</code>
     * @param session
     * @param count
     * @return
     */
    protected Iterable<Callable<Long>> getOperations(final Session session, int count) {
        ArrayList<Callable<Long>> callables = new ArrayList<Callable<Long>>();
        final List<Item> items = new ArrayList<Item>();

        for (int k = 0; k < count; k ++) {
            switch (rnd.nextInt(4)) {
                case 0: { // getItem
                    callables.add(new Callable<Long>() {
                        public Long call() throws Exception {
                            int i = rnd.nextInt(nodePaths.size() + propertyPaths.size());
                            String path = i < nodePaths.size()
                                ? nodePaths.get(i)
                                : propertyPaths.get(i - nodePaths.size());
                            long t1 = System.currentTimeMillis();
                            Item item = session.getItem(path);
                            long t2 = System.currentTimeMillis();
                            items.add(item);
                            return t2 - t1;
                        }

                        @Override
                        public String toString() {
                            return "getItem";
                        }
                    });
                    break;
                }

                case 1: { // getNode
                    callables.add(new Callable<Long>() {
                        public Long call() throws Exception {
                            String path = nodePaths.get(rnd.nextInt(nodePaths.size()));
                            long t1 = System.currentTimeMillis();
                            Node node = session.getNode(path);
                            long t2 = System.currentTimeMillis();
                            items.add(node);
                            return t2 - t1;
                        }

                        @Override
                        public String toString() {
                            return "getNode";
                        }
                    });
                    break;
                }

                case 2: { // getProperty
                    callables.add(new Callable<Long>() {
                        public Long call() throws Exception {
                            String path = propertyPaths.get(rnd.nextInt(propertyPaths.size()));
                            long t1 = System.currentTimeMillis();
                            Property property = session.getProperty(path);
                            long t2 = System.currentTimeMillis();
                            items.add(property);
                            return t2 - t1;
                        }

                        @Override
                        public String toString() {
                            return "getProperty";
                        }
                    });
                    break;
                }

                case 3: { // refresh
                    callables.add(new Callable<Long>() {
                        public Long call() throws Exception {
                            if (items.isEmpty()) {
                                return 0L;
                            }
                            Item item = items.get(rnd.nextInt(items.size()));
                            long t1 = System.currentTimeMillis();
                            item.refresh(rnd.nextBoolean());
                            long t2 = System.currentTimeMillis();
                            return t2 - t1;
                        }

                        @Override
                        public String toString() {
                            return "refresh";
                        }
                    });
                    break;
                }

                default:
                    fail("Invalid case in switch");
            }
        }

        return callables;
    }

    private int roundTripCount;
    private int batchRatio;

    /**
     * Perform {@link #OP_COUNT} JCR operations on a fresh session once for each batch ratio value
     * given in {@link #BATCH_RATIOS}.
     * @throws Exception
     */
    public void testReadOperations() throws Exception {
        for (int ratio : BATCH_RATIOS) {
            testReadOperations(OP_COUNT, ratio);
        }
    }

    private void testReadOperations(int opCount, int batchRatio) throws Exception {
        this.batchRatio = batchRatio;
        this.roundTripCount = 0;

        Map<String, Integer> opCounts = new HashMap<String, Integer>();
        Map<String, Long> opTimes = new HashMap<String, Long>();
        Session session = repository.login();

        Iterable<Callable<Long>> operations = getOperations(session, opCount);
        for (Callable<Long> operation : operations) {
            String opName = operation.toString();
            Long t = operation.call();

            if (opCounts.containsKey(opName)) {
                opCounts.put(opName, opCounts.get(opName) + 1);
                opTimes.put(opName, opTimes.get(opName) + t);
            }
            else {
                opCounts.put(opName, 1);
                opTimes.put(opName, t);
            }
        }

        System.out.println("Batch ratio: " + batchRatio);
        System.out.println("Round trips: " + roundTripCount);

        int count = 0;
        long time = 0L;
        for (String opName : opCounts.keySet()) {
            int c = opCounts.get(opName);
            count += c;
            System.out.println(opName + " count: " + c);

            long t = opTimes.get(opName);
            time += t;
            System.out.println(opName + " time: " + t);
        }

        System.out.println("Total count: " + count);
        System.out.println("Total time: " + time);

        session.logout();
    }

    private Iterator<ItemInfo> getBatch() {
        return filterIterator(itemInfoStore.getItemInfos(), new Predicate<ItemInfo>() {
            public boolean test(ItemInfo value) {
                return rnd.nextInt(batchRatio) == 0;
            }
        });
    }

    // -----------------------------------------------------< RepositoryService >---

    @Override
    protected QNodeDefinition createRootNodeDefinition() {
        fail("not implemented: createRootNodeDefinition");
        return null;
    }

    @Override
    public Iterator<? extends ItemInfo> getItemInfos(SessionInfo sessionInfo, ItemId itemId)
            throws ItemNotFoundException, RepositoryException {

        roundTripCount++;
        ItemInfo itemInfo = itemInfoStore.getItemInfo(itemId);
        return iteratorChain(singleton(itemInfo), getBatch());
    }

    @Override
    public NodeInfo getNodeInfo(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException {
        roundTripCount++;
        return itemInfoStore.getNodeInfo(nodeId);
    }

    @Override
    public PropertyInfo getPropertyInfo(SessionInfo sessionInfo, PropertyId propertyId)
            throws ItemNotFoundException {

        roundTripCount++;
        return itemInfoStore.getPropertyInfo(propertyId);
    }

    @Override
    public Iterator<ChildInfo> getChildInfos(SessionInfo sessionInfo, NodeId parentId)
            throws ItemNotFoundException, RepositoryException {

        roundTripCount++;
        return itemInfoStore.getChildInfos(parentId);
    }

}
