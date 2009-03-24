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

import org.apache.jackrabbit.commons.json.JsonHandler;
import org.apache.jackrabbit.spi.ChildInfo;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.NodeInfo;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

/**
 * <code>ItemInfoJSONHandler</code>...
 */
class ItemInfoJsonHandler implements JsonHandler {

    private static Logger log = LoggerFactory.getLogger(ItemInfoJsonHandler.class);

    private static final int SPECIAL_JSON_PAIR = Integer.MAX_VALUE;

    private final List itemInfos;
    private final NamePathResolver resolver;
    private final String rootURI;

    private final QValueFactoryImpl vFactory;
    private final PathFactory pFactory;
    private final IdFactory idFactory;

    private Name name;
    private int propertyType;
    private int index = Path.INDEX_DEFAULT;

    private Stack nodeInfos = new Stack();
    private PropertyInfoImpl mvPropInfo;

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

        itemInfos = new ArrayList();
        itemInfos.add(nInfo);
        nodeInfos.push(nInfo);
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
                itemInfos.add(nInfo);
            } catch (RepositoryException e) {
                throw new IOException(e.getMessage());
            }
        }
    }

    public void endObject() throws IOException {
        try {
            NodeInfoImpl nInfo = (NodeInfoImpl) nodeInfos.pop();
            NodeInfo parent = getCurrentNodeInfo();
            if (parent != null) {
                if (nInfo.getPath().getAncestor(1).equals(parent.getPath())) {
                    ChildInfo ci = new ChildInfoImpl(nInfo.getName(), nInfo.getUniqueID(), nInfo.getIndex());
                    ((NodeInfoImpl) parent).addChildInfo(ci);
                } else {
                    log.debug("NodeInfo '"+ nInfo.getPath() + "' out of hierarchy. Parent path = " + parent.getPath());
                }
            }
            if (!nInfo.isCompleted()) {
                log.debug("Incomplete NodeInfo '"+ nInfo.getPath() + "' -> Only present as ChildInfo with its parent.");
                itemInfos.remove(nInfo);
            }
        } catch (RepositoryException e) {
            throw new IOException(e.getMessage());
        }
    }

    public void array() throws IOException {
        try {
            mvPropInfo = createPropertyInfo(null, true);
        } catch (RepositoryException e) {
            throw new IOException(e.getMessage());
        }
    }

    public void endArray() throws IOException {
        try {
            // make sure that type is set for mv-properties with empty value array.
            if (propertyType == PropertyType.UNDEFINED &&
                    mvPropInfo.numberOfValues() == 0) {
                int type = vFactory.retrieveType(getValueURI());
                mvPropInfo.setType(type);
            }
            mvPropInfo.checkCompleted();
            getCurrentNodeInfo().addPropertyInfo(mvPropInfo, idFactory);
            mvPropInfo = null;
        } catch (RepositoryException e) {
            throw new IOException(e.getMessage());
        }
    }

    public void key(String key) throws IOException {
        try {
            if (key.equals("::NodeIteratorSize")) {
                propertyType = SPECIAL_JSON_PAIR;
                // TODO: if additional JSON pairs are created -> set name
            } else if (key.startsWith(":")) {
                // binary property
                name = resolver.getQName(key.substring(1));
                propertyType = PropertyType.BINARY;
                index = Path.INDEX_DEFAULT;
            } else if (key.endsWith("]")) {
                // sns-node name
                int pos = key.lastIndexOf('[');
                name = resolver.getQName(key.substring(0, pos));
                propertyType = PropertyType.UNDEFINED;
                index = Integer.parseInt(key.substring(pos + 1, key.length() - 1));
            } else {
                // either node or property
                Name previousName = name;
                name = resolver.getQName(key);
                propertyType = guessPropertyType(name, previousName);
                // property type is defined through json value OR special property
                // :propertyName = type.
                index = Path.INDEX_DEFAULT;
            }
        } catch (RepositoryException e) {
            throw new IOException(e.getMessage());
        }
    }

    public void value(String value) throws IOException {
        try {
            QValue v;
            switch (propertyType) {
                case SPECIAL_JSON_PAIR:
                    // currently no special boolean value pair -> ignore
                    return;
                case PropertyType.BINARY:
                    // key started with ':' but value is String instead of
                    // long. value therefore reflects the property type.
                    // -> reset the property type.
                    // -> omit creation of value AND call to value(QValue)
                    propertyType = PropertyType.valueFromName(value);
                    return;
                case PropertyType.UNDEFINED:
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
            throw new IOException(e.getMessage());
        }
    }

    public void value(boolean value) throws IOException {
        if (propertyType == SPECIAL_JSON_PAIR) {
            // currently no special boolean value pair -> ignore
            return;
        }
        try {
            value(vFactory.create(value));
        } catch (RepositoryException e) {
            throw new IOException(e.getMessage());
        }
    }

    public void value(long value) throws IOException {
        if (propertyType == SPECIAL_JSON_PAIR) {
            NodeInfoImpl parent = getCurrentNodeInfo();
            if (parent != null) {
                parent.setNumberOfChildNodes(value);
            }
            return;
        }
        try {
            QValue v;
            if (propertyType == PropertyType.BINARY) {
                int indx = (mvPropInfo == null) ? -1 : mvPropInfo.numberOfValues();
                v = vFactory.create(value, getValueURI(), indx);
            } else {
                v = vFactory.create(value);
            }
            value(v);
        } catch (RepositoryException e) {
            throw new IOException(e.getMessage());
        }
    }

    public void value(double value) throws IOException {
        if (propertyType == SPECIAL_JSON_PAIR) {
            // currently no special double value pair -> ignore
            return;
        }
        try {
            value(vFactory.create(value));
        } catch (RepositoryException e) {
            throw new IOException(e.getMessage());
        }
    }

    //--------------------------------------------------------------------------
    /**
     *
     * @param value
     * @throws RepositoryException
     */
    private void value(QValue value) throws RepositoryException {
        if (mvPropInfo == null) {
            createPropertyInfo(value, false);
        } else {
            mvPropInfo.addValue(value);
        }
    }

    Iterator getItemInfos() {
        return Collections.unmodifiableList(itemInfos).iterator();
    }

    private NodeInfoImpl getCurrentNodeInfo() {
        return  (nodeInfos.isEmpty()) ? (NodeInfoImpl) null : (NodeInfoImpl) nodeInfos.peek();
    }

    private PropertyInfoImpl createPropertyInfo(QValue value, boolean isMultiValued) throws RepositoryException {
        NodeInfoImpl parent = getCurrentNodeInfo();
        Path p = pFactory.create(parent.getPath(), name, true);
        PropertyId id = idFactory.createPropertyId(parent.getId(), name);

        PropertyInfoImpl pInfo;
        if (isMultiValued) {
            pInfo = new PropertyInfoImpl(id, p, propertyType);
            // not added to parent but upon having read all values.
        } else {
            pInfo = new PropertyInfoImpl(id, p, propertyType, value);
            parent.addPropertyInfo(pInfo, idFactory);
        }
        itemInfos.add(pInfo);
        return pInfo;
    }

    private String getValueURI() throws RepositoryException {
        Path propertyPath;
        if (mvPropInfo == null) {
            propertyPath = pFactory.create(getCurrentNodeInfo().getPath(), name, true);
        } else {
            propertyPath = mvPropInfo.getPath();
        }
        StringBuffer sb = new StringBuffer(rootURI);
        sb.append(Text.escapePath(resolver.getJCRPath(propertyPath)));
        return sb.toString();
    }

    private int guessPropertyType(Name name, Name previousName) {
        if (name.equals(previousName)) {
            // property has been previously retrieved from :name : "typeName"
            // entry in the JSON string. if by coincidence the previous key
            // is equal but belongs to an JSON object (-> node) the prop type
            // has been reset to UNDEFINED anyway.
            return propertyType;
        } else {
            // default: determine type upon Property.getType() only.
            return PropertyType.UNDEFINED;
        }
    }
}