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
package org.apache.jackrabbit.spi.commons;

import static org.apache.jackrabbit.spi.commons.iterator.Iterators.filterIterator;
import static org.apache.jackrabbit.spi.commons.iterator.Iterators.transformIterator;

import org.apache.jackrabbit.spi.ChildInfo;
import org.apache.jackrabbit.spi.ItemInfo;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.NodeInfo;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.PropertyInfo;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.commons.identifier.IdFactoryImpl;
import org.apache.jackrabbit.spi.commons.iterator.Iterators;
import org.apache.jackrabbit.spi.commons.iterator.Transformer;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.commons.value.QValueFactoryImpl;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Utility class providing a {@link NodeInfoBuilder} for building {@link NodeInfo}.
 * Example usage:
 * <pre>
 * ItemInfoBuilder.nodeInfoBuilder()
 *     .createNodeInfo("node1")
 *         .createPropertyInfo("prop1", "value1").build()
 *         .createPropertyInfo("prop2")
 *             .addValue(1.2)
 *             .addValue(2.3)
 *         .build()
 *     .build()
 *     .createNodeInfo("node2")
 *         .setPrimaryType(NameConstants.NT_BASE)
 *         .addMixin(NameConstants.MIX_LOCKABLE)
 *         .createPropertyInfo("prop3")
 *             .setType(PropertyType.BINARY)
 *         .build()
 *     .build()
 * .build();
 * </pre>
 */
public final class ItemInfoBuilder {

    private ItemInfoBuilder() {
        super();
    }

    /**
     * Same as <code>nodeInfoBuilder("", listener)</code>
     * @param listener
     * @return
     */
    public static NodeInfoBuilder nodeInfoBuilder(Listener listener) {
        return nodeInfoBuilder("", listener);
    }

    /**
     * Same as <code>nodeInfoBuilder("", null)</code>
     * @return
     */
    public static NodeInfoBuilder nodeInfoBuilder() {
        return nodeInfoBuilder("", null);
    }

    /**
     * Same as <code>nodeInfoBuilder(localName, null)</code>
     * @param localName
     * @return
     */
    public static NodeInfoBuilder nodeInfoBuilder(String localName) {
        return nodeInfoBuilder(localName, null);
    }

    /**
     * Return a {@link NodeInfoBuilder} for a node with a given <code>localName</code>.
     * @param localName  localName of the node
     * @param listener  {@link Listener} to receive notifications about {@link NodeInfo}s,
     *                  {@link PropertyInfo}s and {@link ChildInfo}s built.
     * @return
     */
    public static NodeInfoBuilder nodeInfoBuilder(String localName, Listener listener) {
        return new NodeInfoBuilder(null, localName, listener);
    }

    /**
     * Return a {@link NodeInfoBuilder} for a node with a given <code>name</code>.
     * @param name  name of the node
     * @param listener  {@link Listener} to receive notifications about {@link NodeInfo}s,
     *                  {@link PropertyInfo}s and {@link ChildInfo}s built.
     * @return
     */
    public static NodeInfoBuilder nodeInfoBuilder(Name name, Listener listener) {
        return new NodeInfoBuilder(null, name, listener);
    }

    /**
     * A listener for receiving notifications about items built by the builders in this class.
     */
    public interface Listener {

        /**
         * Notification that a new {@link NodeInfo} has been built.
         * @param nodeInfo
         */
        void createNodeInfo(NodeInfo nodeInfo);

        /**
         * Notification that new {@link ChildInfo}s have been built.
         * @param id  Id of the parent to which the <code>childInfos</code> belong
         * @param childInfos
         */
        void createChildInfos(NodeId id, Iterator<ChildInfo> childInfos);

        /**
         * Notification that a new {@link PropertyInfo} has been built.
         * @param propertyInfo
         */
        void createPropertyInfo(PropertyInfo propertyInfo);
    }

    /**
     * Builder for {@link NodeInfo}s. Use one of the {@link ItemInfoBuilder#nodeInfoBuilder()}
     * methods to create instances of this class.
     */
    public static class NodeInfoBuilder {
        private final NodeInfoBuilder parent;
        private final Listener listener;

        private Path parentPath;
        private String localName;
        private String namespace;
        private Name name;
        private int index = Path.INDEX_DEFAULT;
        private String uuid;
        private Name primaryTypeName = NameConstants.NT_UNSTRUCTURED;
        private final List<Name> mixins = new ArrayList<Name>();
        private boolean includeChildInfos = true;

        private boolean stale;
        private final List<ItemInfo> itemInfos = new ArrayList<ItemInfo>();
        private NodeInfo nodeInfo;

        private NodeInfoBuilder(NodeInfoBuilder nodeInfoBuilder, String localName, Listener listener) {
            super();
            parent = nodeInfoBuilder;
            this.localName = localName;
            this.listener = listener;
        }

        private NodeInfoBuilder(NodeInfoBuilder nodeInfoBuilder, Name name, Listener listener) {
            super();
            parent = nodeInfoBuilder;
            this.name = name;
            this.listener = listener;
        }

        /**
         * Create a new child {@link PropertyInfo} with a given <code>localName</code> and a given
         * <code>value</code> of type <code>String</code> on this {@link NodeInfo}.
         *
         * @param localName
         * @param value
         * @return  <code>this</code>
         * @throws RepositoryException
         */
        public PropertyInfoBuilder createPropertyInfo(String localName, String value) throws RepositoryException {
            PropertyInfoBuilder pBuilder = new PropertyInfoBuilder(this, localName, listener);
            pBuilder.addValue(value);
            return  pBuilder;
        }

        /**
         * Create a new child {@link PropertyInfo} with a given
         * <code>localName</code> on this {@link NodeInfo}.
         *
         * @param localName
         * @return  <code>this</code>
         */
        public PropertyInfoBuilder createPropertyInfo(String localName) {
            return new PropertyInfoBuilder(this, localName, listener);
        }

        /**
         * Create a new child {@link PropertyInfo} on this {@link NodeInfo}.
         *
         * @return  <code>this</code>
         */
        public PropertyInfoBuilder createPropertyInfo() {
            return new PropertyInfoBuilder(this, null, listener);
        }

        /**
         * Create a new child {@link NodeInfo} on this NodeInfo with a given <code>localName</code>.
         * @param localName
         * @return  <code>this</code>
         */
        public NodeInfoBuilder createNodeInfo(String localName) {
            return new NodeInfoBuilder(this, localName, listener);
        }

        /**
         * Create a new child {@link NodeInfo} on this NodeInfo.

         * @return  <code>this</code>
         */
        public NodeInfoBuilder createNodeInfo() {
            return new NodeInfoBuilder(this, (String) null, listener);
        }

        /**
         * Set the <code>name</code> of the node
         *
         * @param name
         * @return
         */
        public NodeInfoBuilder setName(Name name) {
            this.name = name;
            return this;
        }

        /**
         * Set the <code>localName</code> of the node
         *
         * @param localName
         * @return
         */
        public NodeInfoBuilder setName(String localName) {
            this.localName = localName;
            return this;
        }
        /**
         * Set the namespace
         *
         * @param namespace
         * @return
         */
        public NodeInfoBuilder setNamespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        /**
         * Set the index.
         * @see NodeInfo#getIndex()
         *
         * @param index
         * @return
         */
        public NodeInfoBuilder setIndex(int index) {
            this.index = index;
            return this;
        }

        /**
         * Set the uuid
         *
         * @param uuid
         * @return
         */
        public NodeInfoBuilder setUUID(String uuid) {
            this.uuid = uuid;
            return this;
        }

        /**
         * Set the parent's path of the node
         * 
         * @param parentPath
         * @return
         */
        public NodeInfoBuilder setParentPath(Path parentPath) {
            this.parentPath = parentPath;
            return this;
        }

        /**
         * Set the name of the primary type.
         * @param name
         * @see NodeInfo#getNodetype()
         *
         * @return
         */
        public NodeInfoBuilder setPrimaryType(Name name) {
            primaryTypeName = name;
            return this;
        }

        /**
         * Add a mixin type
         * @see NodeInfo#getMixins()
         *
         * @param name
         * @return
         */
        public NodeInfoBuilder addMixin(Name name) {
            mixins.add(name);
            return this;
        }

        /**
         * Whether the {@link ChildInfo}s should be included or not.
         * @see NodeInfo#getChildInfos()
         *
         * @param include
         * @return
         */
        public NodeInfoBuilder includeChildInfos(boolean include) {
            includeChildInfos = include;
            return this;
        }

        /**
         * Build the {@link NodeInfo}. If a {@link Listener} is associated with this
         * instance, then its {@link Listener#createChildInfos(NodeId, Iterator)} and
         * its {@link Listener#createNodeInfo(NodeInfo)} methods are called.
         *
         * @return the parent builder of this builder
         * @throws RepositoryException
         * @throws IllegalStateException  if build has been called before
         */
        public NodeInfoBuilder build() throws RepositoryException {
            if (stale) {
                throw new IllegalStateException("Builder is stale");
            }
            else {
                stale = true;
                NodeId id = getId();

                nodeInfo = new NodeInfoImpl(getPath(), id, index, primaryTypeName,
                        mixins.toArray(new Name[mixins.size()]), Iterators.<PropertyId>empty(),
                        getPropertyIds(), includeChildInfos ? getChildInfos() : null);

                if (listener != null) {
                    listener.createNodeInfo(nodeInfo);
                    listener.createChildInfos(id, getChildInfos());
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

        /**
         * @return  the parent builder of this builder
         */
        public NodeInfoBuilder getParent() {
            return parent;
        }

        /**
         * Returns the {@link NodeInfo} which has been built by this builder.
         *
         * @return
         * @throws IllegalStateException  if {@link #build()} has not been called before.
         */
        public NodeInfo getNodeInfo() {
            if (!stale) {
                throw new IllegalStateException("NodeInfo not built yet");
            }
            return nodeInfo;
        }

        /**
         * Add a {@link PropertyInfo}
         *
         * @param propertyInfo
         * @return <code>this</code>
         */
        public NodeInfoBuilder addPropertyInfo(PropertyInfo propertyInfo) {
            itemInfos.add(propertyInfo);
            return this;
        }

        /**
         * Add a {@link NodeInfo}
         *
         * @param nodeInfo
         * @return <code>this</code>
         */
        public NodeInfoBuilder addNodeInfo(NodeInfo nodeInfo) {
            itemInfos.add(nodeInfo);
            return this;
        }

        private NodeId getId() throws RepositoryException {
            if (uuid == null) {
                return IdFactoryImpl.getInstance().createNodeId((String) null, getPath());
            }
            else {
                return IdFactoryImpl.getInstance().createNodeId(uuid);
            }
        }

        private Path getPath() throws RepositoryException {
            if (localName == null && name == null) {
                throw new IllegalStateException("Name not set");
            }
            
            if (parent == null && parentPath == null) {
                return PathFactoryImpl.getInstance().getRootPath();
            }
            else {
                Path path = parentPath == null ? parent.getPath() : parentPath;
                if (name == null) {
                    String ns = namespace == null ? Name.NS_DEFAULT_URI : namespace;
                    name = NameFactoryImpl.getInstance().create(ns, localName);
                }
                return PathFactoryImpl.getInstance().create(path, name, true);
            }
        }

        private Iterator<ChildInfo> getChildInfos() {
            return transformIterator(filterIterator(itemInfos.iterator(),
                    new Predicate<ItemInfo>(){
                        public boolean test(ItemInfo info) {
                            return info.denotesNode();
                        }
                    }),
                    new Transformer<ItemInfo, ChildInfo>(){
                        public ChildInfo transform(ItemInfo info) {
                            return new ChildInfoImpl(
                                    info.getPath().getName(), null,
                                    Path.INDEX_DEFAULT);
                        }
                    });
        }

        private Iterator<PropertyId> getPropertyIds() {
            return transformIterator(filterIterator(itemInfos.iterator(),
                    new Predicate<ItemInfo>(){
                        public boolean test(ItemInfo info) {
                            return !info.denotesNode();
                        }
                    }),
                    new Transformer<ItemInfo, PropertyId>(){
                        public PropertyId transform(ItemInfo info) {
                            return (PropertyId) info.getId();
                        }
                    });
        }

    }

    /**
     * Builder for {@link PropertyInfo}s. Use {@link NodeInfoBuilder#createPropertyInfo(String)}
     * to create an instance of this class.
     */
    public static class PropertyInfoBuilder {
        private final NodeInfoBuilder parent;
        private final Listener listener;

        private Name name;
        private String localName;
        private String namespace;
        private final List<QValue> values = new ArrayList<QValue>();
        private int type = PropertyType.UNDEFINED;
        private boolean isMultivalued = true;

        private boolean stale;
        private PropertyInfo propertyInfo;

        private PropertyInfoBuilder(NodeInfoBuilder nodeInfoBuilder, String localName, Listener listener) {
            super();
            parent = nodeInfoBuilder;
            this.localName = localName;
            this.listener = listener;
        }

        /**
         * Set the <code>name</code> of this property
         *
         * @param name
         * @return
         */
        public PropertyInfoBuilder setName(Name name) {
            this.name = name;
            return this;
        }

        /**
         * Set the <code>localName</code> of this property
         *
         * @param localName
         * @return
         */
        public PropertyInfoBuilder setName(String localName) {
            this.localName = localName;
            return this;
        }

        /**
         * Set the namespace
         *
         * @param namespace
         * @return
         */
        public PropertyInfoBuilder setNamespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        /**
         * Set the <code>{@link PropertyType type}</code> of this property
         *
         * @param type
         * @return <code>this</code>
         * @throws IllegalStateException  if a property of a different type has been added before.
         */
        public PropertyInfoBuilder setType(int type) {
            if (values.size() > 0 && type != values.get(0).getType()) {
                throw new IllegalStateException("Type mismatch. " +
                        "Required " + PropertyType.nameFromValue(values.get(0).getType()) +
                        " found " + PropertyType.nameFromValue(type));
            }

            this.type = type;
            return this;
        }

        /**
         * Add a <code>value</code> to this property. Sets this property to single valued if
         * this is the first value. Otherwise sets this property to multi-valued.
         *
         * @param value
         * @return <code>this</code>
         * @throws IllegalStateException  if the type of the value does not match the type of this property
         */
        public PropertyInfoBuilder addValue(QValue value) {
            int actualType = value.getType();
            if (type != PropertyType.UNDEFINED && type != actualType) {
                throw new IllegalStateException("Type mismatch. " +
                        "Required " + PropertyType.nameFromValue(type) +
                        " found " + PropertyType.nameFromValue(value.getType()));
            }

            values.add(value);
            type = actualType;
            isMultivalued = values.size() != 1;
            return this;
        }

        /**
         * Add a {@link PropertyType#STRING} value to this property.
         *
         * @param value
         * @return <code>this</code>
         * @throws RepositoryException
         * @throws IllegalStateException  if the type of the value does not match the type of this property
         */
        public PropertyInfoBuilder addValue(String value) throws RepositoryException {
            return addValue(QValueFactoryImpl.getInstance().create(value, PropertyType.STRING));
        }

        /**
         * Add a {@link PropertyType#DATE} value to this property.
         *
         * @param value
         * @return <code>this</code>
         * @throws RepositoryException
         * @throws IllegalStateException  if the type of the value does not match the type of this property
         */
        public PropertyInfoBuilder addValue(Calendar  value) throws RepositoryException {
            return addValue(QValueFactoryImpl.getInstance().create(value));
        }

        /**
         * Add a {@link PropertyType#DOUBLE} value to this property.
         *
         * @param value
         * @return <code>this</code>
         * @throws RepositoryException
         * @throws IllegalStateException  if the type of the value does not match the type of this property
         */
        public PropertyInfoBuilder addValue(double value) throws RepositoryException {
            return addValue(QValueFactoryImpl.getInstance().create(value));
        }

        /**
         * Add a {@link PropertyType#LONG} value to this property.
         *
         * @param value
         * @return <code>this</code>
         * @throws RepositoryException
         * @throws IllegalStateException  if the type of the value does not match the type of this property
         */
        public PropertyInfoBuilder addValue(long value) throws RepositoryException {
            return addValue(QValueFactoryImpl.getInstance().create(value));
        }

        /**
         * Add a {@link PropertyType#BOOLEAN} value to this property.
         *
         * @param value
         * @return <code>this</code>
         * @throws RepositoryException
         * @throws IllegalStateException  if the type of the value does not match the type of this property
         */
        public PropertyInfoBuilder addValue(boolean value) throws RepositoryException {
            return addValue(QValueFactoryImpl.getInstance().create(value));
        }

        /**
         * Add a {@link PropertyType#NAME} value to this property.
         *
         * @param value
         * @return <code>this</code>
         * @throws RepositoryException
         * @throws IllegalStateException  if the type of the value does not match the type of this property
         */
        public PropertyInfoBuilder addValue(Name value) throws RepositoryException {
            return addValue(QValueFactoryImpl.getInstance().create(value));
        }

        /**
         * Add a {@link PropertyType#PATH} value to this property.
         *
         * @param value
         * @return <code>this</code>
         * @throws RepositoryException
         * @throws IllegalStateException  if the type of the value does not match the type of this property
         */
        public PropertyInfoBuilder addValue(Path value) throws RepositoryException {
            return addValue(QValueFactoryImpl.getInstance().create(value));
        }

        /**
         * Add a {@link PropertyType#DECIMAL} value to this property.
         *
         * @param value
         * @return <code>this</code>
         * @throws RepositoryException
         * @throws IllegalStateException  if the type of the value does not match the type of this property
         */
        public PropertyInfoBuilder addValue(BigDecimal  value) throws RepositoryException {
            return addValue(QValueFactoryImpl.getInstance().create(value));
        }

        /**
         * Add a {@link PropertyType#URI} value to this property.
         *
         * @param value
         * @return <code>this</code>
         * @throws RepositoryException
         * @throws IllegalStateException  if the type of the value does not match the type of this property
         */
        public PropertyInfoBuilder addValue(URI value) throws RepositoryException {
            return addValue(QValueFactoryImpl.getInstance().create(value));
        }

        /**
         * Add a {@link PropertyType#BINARY} value to this property.
         *
         * @param value
         * @return <code>this</code>
         * @throws RepositoryException
         * @throws IllegalStateException  if the type of the value does not match the type of this property
         */
        public PropertyInfoBuilder addValue(byte[] value) throws RepositoryException {
            return addValue(QValueFactoryImpl.getInstance().create(value));
        }

        /**
         * Add a {@link PropertyType#BINARY} value to this property.
         *
         * @param value
         * @return <code>this</code>
         * @throws RepositoryException
         * @throws IllegalStateException  if the type of the value does not match the type of this property
         */
        public PropertyInfoBuilder addValue(InputStream value) throws RepositoryException, IOException {
            return addValue(QValueFactoryImpl.getInstance().create(value));
        }

        /**
         * Add a {@link PropertyType#BINARY} value to this property.
         *
         * @param value
         * @return <code>this</code>
         * @throws RepositoryException
         * @throws IllegalStateException  if the type of the value does not match the type of this property
         */
        public PropertyInfoBuilder addValue(File value) throws RepositoryException, IOException {
            return addValue(QValueFactoryImpl.getInstance().create(value));
        }

        /**
         * Set this property to multi-values.
         *
         * @param on
         * @return <code>this</code>
         * @throws IllegalStateException if this property does not contain exactly on value
         */
        public PropertyInfoBuilder setMultivalued(boolean on) {
            if (!on && values.size() != 1) {
                throw new IllegalStateException(
                        "Cannot create single valued property when multiple values are present");
            }
            isMultivalued = true;
            return this;
        }

        /**
         * Build the {@link PropertyInfo}. If a {@link Listener} is associated with this
         * instance, then its {@link Listener#createPropertyInfo(PropertyInfo)} methods
         * is called.
         *
         * @return the parent builder of this builder
         * @throws RepositoryException
         * @throws IllegalStateException  if build has been called before
         * @throws IllegalStateException  if the type is not set
         */
        public NodeInfoBuilder build() throws RepositoryException {
            if (stale) {
                throw new IllegalStateException("Builder is stale");
            }
            else if (type == PropertyType.UNDEFINED) {
                throw new IllegalStateException("Type not set");
            }
            else if (localName == null && name == null) {
                throw new IllegalStateException("Name not set");
            }
            else {
                stale = true;

                NodeId parentId = parent.getId();
                if (name == null) {
                    String ns = namespace == null ? Name.NS_DEFAULT_URI : namespace;
                    name = NameFactoryImpl.getInstance().create(ns, localName);
                }
                Path path = PathFactoryImpl.getInstance().create(parent.getPath(), name, true);
                PropertyId id = IdFactoryImpl.getInstance().createPropertyId(parentId, name);

                propertyInfo = new PropertyInfoImpl(path, id, type, isMultivalued,
                        values.toArray(new QValue[values.size()]));

                if (listener != null) {
                    listener.createPropertyInfo(propertyInfo);
                }
                return parent.addPropertyInfo(propertyInfo);
            }
        }

        /**
         * @return  the parent builder of this builder
         */
        public NodeInfoBuilder getParent() {
            return parent;
        }

        /**
         * Returns the {@link PropertyInfo} which has been built by this builder.
         *
         * @return
         * @throws IllegalStateException  if {@link #build()} has not been called before.
         */
        public PropertyInfo getPropertyInfo() {
            if (!stale) {
                throw new IllegalStateException("PropertyInfo not built yet");
            }
            return propertyInfo;
        }

    }

}
