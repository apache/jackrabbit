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
package org.apache.jackrabbit.spi2davex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.commons.json.JsonHandler;
import org.apache.jackrabbit.spi.ChildInfo;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.ItemInfo;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.NodeInfo;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.util.StringCache;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>ItemInfoJSONHandler</code>...
 */
class ItemInfoJsonHandler implements JsonHandler {

    private static Logger log = LoggerFactory.getLogger(ItemInfoJsonHandler.class);

    private static final String LEAF_NODE_HINT = "::NodeIteratorSize";

    private final List<ItemInfo> itemInfos;
    private final NamePathResolver resolver;
    private final String rootURI;

    private final QValueFactoryImpl vFactory;
    private final PathFactory pFactory;
    private final IdFactory idFactory;

    private boolean expectingHintValue = false;

    private Name name;
    private int index = Path.INDEX_DEFAULT;

    // temp. property state
    private int propertyType;
    private boolean multiValuedProperty = false;
    private List<QValue> propValues = new ArrayList<QValue>();

    private Stack<NodeInfo> nodeInfos = new Stack<NodeInfo>();

    private Stack<List<PropertyInfoImpl>> propInfoLists = new Stack<List<PropertyInfoImpl>>();

    ItemInfoJsonHandler(NamePathResolver resolver, NodeInfo nInfo,
                        String rootURI,
                        QValueFactoryImpl vFactory,
                        PathFactory pFactory,
                        IdFactory idFactory) {
        this.resolver = resolver;
        this.rootURI = rootURI;

        this.vFactory = vFactory;
        this.pFactory = pFactory;
        this.idFactory = idFactory;

        itemInfos = new ArrayList<ItemInfo>();
        itemInfos.add(nInfo);
        nodeInfos.push(nInfo);
        propInfoLists.push(new ArrayList<PropertyInfoImpl>(8));
    }

    public void object() throws IOException {
        if (name != null) {
            try {
                NodeInfo current = getCurrentNodeInfo();
                Path relPath = pFactory.create(name, index);
                NodeId id = idFactory.createNodeId(current.getId(), relPath);
                Path currentPath = current.getPath();
                Path p = pFactory.create(currentPath, relPath, true);
                NodeInfo nInfo = new NodeInfoImpl(id, p);
                nodeInfos.push(nInfo);
                propInfoLists.push(new ArrayList<PropertyInfoImpl>(8));
            } catch (RepositoryException e) {
                throw new IOException(e.getMessage(), e);
            }
        }
    }

    public void endObject() throws IOException {
        try {
            NodeInfoImpl nInfo = (NodeInfoImpl) nodeInfos.pop();
            List<PropertyInfoImpl> props = propInfoLists.pop();
            // all required information to create a node info should now be gathered
            nInfo.setPropertyInfos(props.toArray(new PropertyInfoImpl[props.size()]), idFactory);
            NodeInfo parent = getCurrentNodeInfo();
            if (parent != null) {
                if (nInfo.getPath().getAncestor(1).equals(parent.getPath())) {
                    ChildInfo ci = new ChildInfoImpl(nInfo.getName(), nInfo.getUniqueID(), nInfo.getIndex());
                    ((NodeInfoImpl) parent).addChildInfo(ci);
                } else {
                    log.debug("NodeInfo '"+ nInfo.getPath() + "' out of hierarchy. Parent path = " + parent.getPath());
                }
            }
            if (nInfo.isCompleted()) {
                itemInfos.addAll(props);
                itemInfos.add(nInfo);
            } else {
                log.debug("Incomplete NodeInfo '"+ nInfo.getPath() + "' -> Only present as ChildInfo with its parent.");
            }
        } catch (RepositoryException e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            // reset all node-related handler state
            name = null;
            index = Path.INDEX_DEFAULT;
        }
    }

    public void array() throws IOException {
        multiValuedProperty = true;
        propValues.clear();
    }

    public void endArray() throws IOException {
        try {
            if (propertyType == PropertyType.UNDEFINED) {
                if (propValues.isEmpty()) {
                    // make sure that type is set for mv-properties with empty value array.
                    propertyType = vFactory.retrieveType(getValueURI());
                } else {
                    propertyType = propValues.get(0).getType();
                }
            }
            // create multi-valued property info
            NodeInfoImpl parent = getCurrentNodeInfo();
            Path p = pFactory.create(parent.getPath(), name, true);
            PropertyId id = idFactory.createPropertyId(parent.getId(), name);
            PropertyInfoImpl propInfo = new PropertyInfoImpl(id, p, propertyType, propValues.toArray(new QValue[propValues.size()]));
            propInfo.checkCompleted();
            getCurrentPropInfos().add(propInfo);
        } catch (RepositoryException e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            // reset property-related handler state
            propertyType = PropertyType.UNDEFINED;
            multiValuedProperty = false;
            propValues.clear();
            name = null;
        }
    }

    public void key(String key) throws IOException {
        expectingHintValue = false;
        try {
            if (key.equals(LEAF_NODE_HINT)) {
                expectingHintValue = true;
                // TODO: remember name of hint if there will be additional types of hints
                name = null;
            } else if (key.startsWith(":")) {
                expectingHintValue = true;
                // either
                //   :<nameOfProperty> : "PropertyTypeName"
                // or
                //   :<nameOfBinaryProperty> : <lengthOfBinaryProperty>
                //name = resolver.getQName(key.substring(1));
                name = resolver.getQName(StringCache.fromCacheOrNew(key.substring(1)));
                index = Path.INDEX_DEFAULT;
            } else if (key.endsWith("]")) {
                // sns-node name
                int pos = key.lastIndexOf('[');
                //name = resolver.getQName(key.substring(0, pos));
                name = resolver.getQName(StringCache.fromCacheOrNew(key.substring(0, pos)));
                propertyType = PropertyType.UNDEFINED;
                index = Integer.parseInt(key.substring(pos + 1, key.length() - 1));
            } else {
                // either node or property
                name = resolver.getQName(StringCache.cache(key));
                index = Path.INDEX_DEFAULT;
            }
        } catch (RepositoryException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * there is currently one special string-value hint:
     *
     *   :<nameOfProperty> : "PropertyTypeName"
     *
     * @param value The value.
     * @throws IOException
     */
    public void value(String value) throws IOException {
        if (expectingHintValue) {
            // :<nameOfProperty> : "PropertyTypeName"
            propertyType = PropertyType.valueFromName(value);
            return;
        }
        try {
            QValue v;
            switch (propertyType) {
                case PropertyType.UNDEFINED:
                    if (!NameConstants.JCR_UUID.equals(name)) {
                        value = StringCache.cache(value);
                    }
                    v = vFactory.create(value, PropertyType.STRING);
                    break;
                case PropertyType.NAME:
                    v = vFactory.create(resolver.getQName(value));
                    break;
                case PropertyType.PATH:
                    v = vFactory.create(resolver.getQPath(value));
                    break;
                default:
                    v = vFactory.create(value, propertyType);
            }
            value(v);
        } catch (RepositoryException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    public void value(boolean value) throws IOException {
        if (expectingHintValue) {
            // there are currently no special boolean value hints:
            return;
        }
        try {
            value(vFactory.create(value));
        } catch (RepositoryException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * there are currently 2 types of special long value hints:
     *
     * a) ::NodeIteratorSize : 0
     *    ==> denotes the current node as leaf node
     *
     * b) :<nameOfBinaryProperty> : <lengthOfBinaryProperty>
     *
     * @param value The value.
     * @throws IOException
     */
    public void value(long value) throws IOException {
        if (expectingHintValue) {
            if (name == null) {
                // ::NodeIteratorSize : 0
                NodeInfoImpl parent = getCurrentNodeInfo();
                if (parent != null) {
                    parent.markAsLeafNode();
                }
            } else {
                // :<nameOfBinaryProperty> : <lengthOfBinaryProperty>
                propertyType = PropertyType.BINARY;
                try {
                    int indx = (!multiValuedProperty) ? -1 : propValues.size();
                    value(vFactory.create(value, getValueURI(), indx));
                } catch (RepositoryException e) {
                    throw new IOException(e.getMessage(), e);
                }
            }
            return;
        }
        try {
            value(vFactory.create(value));
        } catch (RepositoryException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    public void value(double value) throws IOException {
        if (expectingHintValue) {
            // currently no special double value pair -> ignore
            return;
        }
        try {
            value(vFactory.create(value));
        } catch (RepositoryException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    //--------------------------------------------------------------------------
    /**
     *
     * @param value
     * @throws RepositoryException
     */
    private void value(QValue value) throws RepositoryException {
        if (!multiValuedProperty) {
            try {
                if (propertyType == PropertyType.UNDEFINED) {
                    propertyType = value.getType();
                }
                // create single-valued property info
                NodeInfoImpl parent = getCurrentNodeInfo();
                Path p = pFactory.create(parent.getPath(), name, true);
                PropertyId id = idFactory.createPropertyId(parent.getId(), name);
                PropertyInfoImpl propInfo = new PropertyInfoImpl(id, p, propertyType, value);
                propInfo.checkCompleted();
                // add property info to current list, will be processed on endObject() event
                getCurrentPropInfos().add(propInfo);
            } finally {
                // reset property-related handler state
                propertyType = PropertyType.UNDEFINED;
                multiValuedProperty = false;
                propValues.clear();
                name = null;
                expectingHintValue = false;
            }
        } else {
            // multi-valued property
            // add value to current list, will be processed on endArray() event
            propValues.add(value);
        }
    }

    Iterator<? extends ItemInfo> getItemInfos() {
        return Collections.unmodifiableList(itemInfos).iterator();
    }

    private NodeInfoImpl getCurrentNodeInfo() {
        return (nodeInfos.isEmpty()) ? null : (NodeInfoImpl) nodeInfos.peek();
    }

    private List<PropertyInfoImpl> getCurrentPropInfos() {
        return (propInfoLists.isEmpty()) ? null : propInfoLists.peek();
    }

    private String getValueURI() throws RepositoryException {
        Path propertyPath = pFactory.create(getCurrentNodeInfo().getPath(), name, true);
        StringBuffer sb = new StringBuffer(rootURI);
        sb.append(Text.escapePath(resolver.getJCRPath(propertyPath)));
        return sb.toString();
    }
}
