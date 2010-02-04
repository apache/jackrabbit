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
package org.apache.jackrabbit.jcr2spi;

import java.util.Iterator;

import javax.jcr.AccessDeniedException;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.functors.NotPredicate;
import org.apache.commons.collections.iterators.FilterIterator;
import org.apache.commons.collections.iterators.IteratorChain;
import org.apache.jackrabbit.spi.ChildInfo;
import org.apache.jackrabbit.spi.ItemInfo;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.NodeInfo;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.PropertyInfo;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.Path.Element;
import org.apache.jackrabbit.spi.commons.ItemInfoBuilder.NodeInfoBuilder;

/**
 * Test cases for {@link RepositoryService#getItemInfos(SessionInfo, NodeId)}. Specifically
 * for JCR-1797.
 */
public class GetItemsTest extends AbstractJCR2SPITest {
    private Session session;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        session = repository.login("default");
    }

    @Override
    protected void initInfosStore(NodeInfoBuilder builder) throws RepositoryException {
        // build up a hierarchy of items
        builder
            .createNodeInfo("node1")
                .createNodeInfo("node11").build()
                .createNodeInfo("node12").build()
                .createNodeInfo("node13").build()
                .createPropertyInfo("property11", "value11").build()
                .createPropertyInfo("property12", "value12").build()
            .build()
            .createNodeInfo("node2")
                .createNodeInfo("node21")
                    .createNodeInfo("node211")
                        .createNodeInfo("node2111")
                            .createNodeInfo("node21111")
                                .createNodeInfo("node211111")
                                    .createNodeInfo("node2111111").build()
                                .build()
                            .build()
                        .build()
                    .build()
                .build()
            .build()
            .createNodeInfo("node3").build()
            .build();
    }

    @Override
    protected void tearDown() throws Exception {
        if (session != null) {
            session.logout();
            session = null;
        }
        super.tearDown();
    }

    private Iterable itemInfosProvider;

    /**
     * Check whether we can traverse the hierarchy when the item info for root is
     * retrieved first.
     * @throws RepositoryException
     */
    public void testGetItemInfosRootFirst() throws RepositoryException {
        itemInfosProvider = new Iterable() {
            Predicate isRoot = new Predicate() {
                public boolean evaluate(Object object) {
                    ItemInfo itemInfo = (ItemInfo) object;
                    return itemInfo.getPath().denotesRoot();
                }
            };

            @SuppressWarnings("unchecked")
            public Iterator<ItemInfo> iterator() {
                return new IteratorChain(
                        new FilterIterator(itemInfoStore.getItemInfos(), isRoot),
                        new FilterIterator(itemInfoStore.getItemInfos(), NotPredicate.getInstance(isRoot)));
            }
        };

        assertTrue(session.getRootNode().getDepth() == 0);
        checkHierarchy();
    }

    /**
     * Check whether we can traverse the hierarchy when the item info for a deep item
     * is retrieved first.
     * @throws RepositoryException
     */
    public void testGetItemInfosDeepFirst() throws RepositoryException {
        final String targetPath = "/node2/node21/node211/node2111/node21111/node211111/node2111111";

        itemInfosProvider = new Iterable() {
            Predicate isTarget = new Predicate() {
                public boolean evaluate(Object object) {
                    ItemInfo itemInfo = (ItemInfo) object;
                    return targetPath.equals(toJCRPath(itemInfo.getPath()));
                }
            };

            @SuppressWarnings("unchecked")
            public Iterator<ItemInfo> iterator() {
                return new IteratorChain(
                        new FilterIterator(itemInfoStore.getItemInfos(), isTarget),
                        new FilterIterator(itemInfoStore.getItemInfos(), NotPredicate.getInstance(isTarget)));
            }
        };

        assertEquals(targetPath, session.getItem(targetPath).getPath());
        checkHierarchy();
    }

    private void checkHierarchy() throws PathNotFoundException, RepositoryException, ItemNotFoundException,
            AccessDeniedException {
        for (Iterator<ItemInfo> itemInfos = itemInfosProvider.iterator(); itemInfos.hasNext();) {
            ItemInfo itemInfo = itemInfos.next();
            String jcrPath = toJCRPath(itemInfo.getPath());
            Item item = session.getItem(jcrPath);
            assertEquals(jcrPath, item.getPath());

            if (item.getDepth() > 0) {
                Node parent = item.getParent();
                if (item.isNode()) {
                    assertEquals(item, parent.getNode(item.getName()));
                }
                else {
                    assertEquals(item, parent.getProperty(item.getName()));
                }
            }

        }
    }

    private static String toJCRPath(Path path) {
        Element[] elems = path.getElements();
        StringBuffer jcrPath = new StringBuffer();

        for (int k = 0; k < elems.length; k++) {
            jcrPath.append(elems[k].getName().getLocalName());
            if (k + 1 < elems.length || elems.length == 1) {
                jcrPath.append('/');
            }
        }

        return jcrPath.toString();
    }

    @Override
    protected QNodeDefinition createRootNodeDefinition() {
        fail("Not implemented");
        return null;
    }

    @Override
    public Iterator<ChildInfo> getChildInfos(SessionInfo sessionInfo, NodeId parentId)
            throws RepositoryException {

        fail("Not implemented");
        return null;
    }

    @Override
    public Iterator<ItemInfo> getItemInfos(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException {
        return itemInfosProvider.iterator();
    }

    @Override
    public NodeInfo getNodeInfo(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException {
        fail("Not implemented");
        return null;
    }

    @Override
    public PropertyInfo getPropertyInfo(SessionInfo sessionInfo, PropertyId propertyId) {
        fail("Not implemented");
        return null;
    }

    interface Iterable {
        public Iterator<ItemInfo> iterator();
    }

}
