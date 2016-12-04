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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.ChildInfo;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.NodeInfo;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

/**
 * <code>NodeInfoImpl</code>...
 */
public class NodeInfoImpl extends ItemInfoImpl implements NodeInfo {

    // data deduced from property values
    private NodeId id;
    private Name primaryNodeTypeName;
    private Name[] mixinNodeTypeNames = Name.EMPTY_ARRAY;

    private final List<PropertyId> propertyIds = new ArrayList<PropertyId>(8);
    private List<ChildInfo> childInfos = null;

    /**
     * Creates a new <code>NodeInfo</code>.
     *
     * @param id The node id.
     * @param path the path to this item.
     * @throws javax.jcr.RepositoryException If an error occurs.
     */
    public NodeInfoImpl(NodeId id, Path path) throws RepositoryException {
        super(path, true);
        this.id = id;
    }

    //-----------------------------------------------------------< NodeInfo >---
    public NodeId getId() {
        return id;
    }

    public int getIndex() {
        return getPath().getNormalizedIndex();
    }

    public Name getNodetype() {
        return primaryNodeTypeName;
    }

    public Name[] getMixins() {
        return mixinNodeTypeNames;
    }

    public PropertyId[] getReferences() {
        return new PropertyId[0];
    }

    public Iterator<PropertyId> getPropertyIds() {
        return propertyIds.iterator();
    }

    public Iterator<ChildInfo> getChildInfos() {
        return (childInfos == null) ? null : childInfos.iterator();
    }

    //--------------------------------------------------------------------------
    void setPropertyInfos(PropertyInfoImpl[] propInfos, IdFactory idFactory) throws RepositoryException {
        boolean resolveUUID = false;
        for (PropertyInfoImpl propInfo : propInfos) {
            Name pn = propInfo.getId().getName();
            if (NameConstants.JCR_UUID.equals(pn)) {
                id = idFactory.createNodeId(propInfo.getValues()[0].getString());
                resolveUUID = true;
            } else if (NameConstants.JCR_PRIMARYTYPE.equals(pn)) {
                primaryNodeTypeName = propInfo.getValues()[0].getName();
            } else if (NameConstants.JCR_MIXINTYPES.equals(pn)) {
                QValue[] vs = propInfo.getValues();
                Name[] mixins = new Name[vs.length];
                for (int i = 0; i < vs.length; i++) {
                    mixins[i] = vs[i].getName();
                }
                mixinNodeTypeNames = mixins;
            }
        }

        propertyIds.clear();
        for (PropertyInfoImpl propInfo : propInfos) {
            if (resolveUUID) {
                propInfo.setId(idFactory.createPropertyId(id, propInfo.getName()));
            }
            propertyIds.add(propInfo.getId());
        }

    }

    void addChildInfo(ChildInfo childInfo) {
        if (childInfos == null) {
            childInfos = new ArrayList<ChildInfo>();
        }
        childInfos.add(childInfo);
    }

    void markAsLeafNode() {
        childInfos = Collections.emptyList();
    }

    boolean isCompleted() {
        return (id != null && primaryNodeTypeName != null && !propertyIds.isEmpty());
    }

    String getUniqueID() {
        if (id.getUniqueID() != null && id.getPath() == null) {
            return id.getUniqueID();
        } else {
            return null;
        }
    }
}