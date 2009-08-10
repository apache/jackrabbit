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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.NotPredicate;
import org.apache.commons.collections.iterators.EmptyIterator;
import org.apache.commons.collections.iterators.FilterIterator;
import org.apache.commons.collections.iterators.IteratorChain;
import org.apache.commons.collections.iterators.TransformIterator;
import org.apache.jackrabbit.spi.ChildInfo;
import org.apache.jackrabbit.spi.ItemInfo;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.NodeInfo;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.PropertyInfo;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.Path.Element;
import org.apache.jackrabbit.spi.commons.ChildInfoImpl;
import org.apache.jackrabbit.spi.commons.NodeInfoImpl;
import org.apache.jackrabbit.spi.commons.PropertyInfoImpl;
import org.apache.jackrabbit.spi.commons.identifier.IdFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.commons.value.QValueFactoryImpl;

/**
 * Test cases for {@link RepositoryService#getItemInfos(SessionInfo, NodeId)}. Sepcifically
 * for JCR-1797.
 */
public class GetItemsTest extends AbstractJCR2SPITest {
    private List<ItemInfo> itemInfos;
    private Session session;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        itemInfos = new ArrayList<ItemInfo>();

        // build up a hierarchy of items
        new NodeInfoBuilder()
            .createNodeInfo("node1")
                .createNodeInfo("node11").build(itemInfos)
                .createNodeInfo("node12").build(itemInfos)
                .createNodeInfo("node13").build(itemInfos)
                .createPropertyInfo("property11", "value11").build(itemInfos)
                .createPropertyInfo("property12", "value12").build(itemInfos)
            .build(itemInfos)
            .createNodeInfo("node2")
                .createNodeInfo("node21")
                    .createNodeInfo("node211")
                        .createNodeInfo("node2111")
                            .createNodeInfo("node21111")
                                .createNodeInfo("node211111")
                                    .createNodeInfo("node2111111").build(itemInfos)
                                .build(itemInfos)
                            .build(itemInfos)
                        .build(itemInfos)
                    .build(itemInfos)
                .build(itemInfos)
            .build(itemInfos)
            .createNodeInfo("node3").build(itemInfos)
            .build(itemInfos);

        session = repository.login("default");
    }

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
                        new FilterIterator(itemInfos.iterator(), isRoot),
                        new FilterIterator(itemInfos.iterator(), NotPredicate.getInstance(isRoot)));
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
                        new FilterIterator(itemInfos.iterator(), isTarget),
                        new FilterIterator(itemInfos.iterator(), NotPredicate.getInstance(isTarget)));
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

    /**
     * todo: we might want to grow this in a full blown helper class
     */
    class NodeInfoBuilder {
        private final NodeInfoBuilder parent;
        private final String name;

        protected boolean stale;
        private final List<ItemInfo> itemInfos = new ArrayList<ItemInfo>();
        private NodeInfo nodeInfo;

        public NodeInfoBuilder() {
            this(null, null);
        }

        public NodeInfoBuilder(NodeInfoBuilder nodeInfoBuilder, String name) {
            super();
            parent = nodeInfoBuilder;
            this.name = name;
        }

        public PropertyInfoBuilder createPropertyInfo(String name, String value) {
            return new PropertyInfoBuilder(this, name, value);
        }

        public NodeInfoBuilder createNodeInfo(String name) {
            return new NodeInfoBuilder(this, name);
        }

        @SuppressWarnings("unchecked")
        public NodeInfoBuilder build(List<ItemInfo> infos) throws RepositoryException {
            if (stale) {
                throw new IllegalStateException("Builder is stale");
            }
            else {
                stale = true;

                NodeId id = getId();
                Path path = id.getPath();

                Iterator<ItemInfo> propertyIds = new TransformIterator(new FilterIterator(itemInfos.iterator(),
                        new Predicate() {
                            public boolean evaluate(Object object) {
                                return object instanceof PropertyInfo;
                            }
                        }),
                        new Transformer() {
                            public Object transform(Object input) {
                                PropertyInfo info = (PropertyInfo) input;
                                return info.getId();
                            }});

                Iterator<ItemInfo> childInfos = new TransformIterator(itemInfos.iterator(), new Transformer(){
                    public Object transform(Object input) {
                        ItemInfo info = (ItemInfo) input;
                        Name name = info.getPath().getNameElement().getName();
                        return new ChildInfoImpl(name, null, Path.INDEX_DEFAULT);
                    }});

                nodeInfo = new NodeInfoImpl(path, id, Path.INDEX_DEFAULT, NameConstants.NT_UNSTRUCTURED,
                        Name.EMPTY_ARRAY, EmptyIterator.INSTANCE, propertyIds, childInfos);

                if (infos != null) {
                    infos.add(nodeInfo);
                }

                if (parent == null) {
                    return this;
                }
                else {
                    parent.addNodeInfo(nodeInfo);
                    return parent;
                }
            }
        }

        public NodeInfo getNodeInfo() throws RepositoryException {
            if (!stale) {
                build(null);
            }
            return nodeInfo;
        }

        NodeInfoBuilder addPropertyInfo(PropertyInfo propertyInfo) {
            itemInfos.add(propertyInfo);
            return this;
        }

        NodeInfoBuilder addNodeInfo(NodeInfo nodeInfo) {
            itemInfos.add(nodeInfo);
            return this;
        }

        NodeId getId() throws RepositoryException {
            return IdFactoryImpl.getInstance().createNodeId((String) null, getPath());
        }

        Path getPath() throws RepositoryException {
            if (parent == null) {
                return PathFactoryImpl.getInstance().getRootPath();
            }
            else {
                Name name = NameFactoryImpl.getInstance().create(Name.NS_DEFAULT_URI, this.name);
                return PathFactoryImpl.getInstance().create(parent.getPath(), name, true);
            }
        }

    }

    /**
     * todo: we might want to grow this in a full blown helper class
     */
    class PropertyInfoBuilder {
        private final NodeInfoBuilder parent;
        private final String name;
        private final String value;

        private boolean stale;
        private PropertyInfo propertyInfo;

        public PropertyInfoBuilder(NodeInfoBuilder nodeInfoBuilder, String name, String value) {
            super();
            parent = nodeInfoBuilder;
            this.name = name;
            this.value = value;
        }

        public NodeInfoBuilder build(List<ItemInfo> infos) throws RepositoryException {
            if (parent == null) {
                return null;
            }

            if (stale) {
                throw new IllegalStateException("Builder is stale");
            }
            else {
                stale = true;

                NodeId parentId = parent.getId();
                Name propertyName = NameFactoryImpl.getInstance().create(Name.NS_DEFAULT_URI, this.name);
                Path path = PathFactoryImpl.getInstance().create(parentId.getPath(), propertyName, true);
                PropertyId id = IdFactoryImpl.getInstance().createPropertyId(parentId, propertyName);
                QValue qvalue = QValueFactoryImpl.getInstance().create(value, PropertyType.STRING);
                propertyInfo = new PropertyInfoImpl(path, id, PropertyType.STRING, false, new QValue[] { qvalue });

                if (infos != null) {
                    infos.add(propertyInfo);
                }
                return parent.addPropertyInfo(propertyInfo);
            }
        }

        public PropertyInfo getPropertyInfo() {
            return propertyInfo;
        }

    }

}
