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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.ChildInfo;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.NodeInfo;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.PropertyInfo;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

/**
 * <code>NodeInfoImpl</code>...
 */
public class NodeInfoImpl extends ItemInfoImpl implements NodeInfo {

    private NodeId id;
    private String uniqueID;
    private Name primaryNodeTypeName;
    private Name[] mixinNodeTypeNames = new Name[0];

    private final Set<PropertyInfo> propertyInfos = new LinkedHashSet<PropertyInfo>();
    private Set<ChildInfo> childInfos = null;

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
        return getPath().getNameElement().getNormalizedIndex();
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
        List<PropertyId> l = new ArrayList<PropertyId>();
        for (PropertyInfo propertyInfo : propertyInfos) {
            l.add(propertyInfo.getId());
        }
        return l.iterator();
    }

    public Iterator<ChildInfo> getChildInfos() {
        return (childInfos == null) ? null : childInfos.iterator();
    }

    //--------------------------------------------------------------------------
    void addPropertyInfo(PropertyInfoImpl propInfo, IdFactory idFactory) throws RepositoryException {
        propertyInfos.add(propInfo);

        Name pn = propInfo.getId().getName();
        if (NameConstants.JCR_UUID.equals(pn)) {
            uniqueID = propInfo.getValues()[0].getString();
            id = idFactory.createNodeId(uniqueID);
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

    void resolveUUID(IdFactory idFactory) {
        if (uniqueID != null) {
            for (Object o : propertyInfos) {
                PropertyInfoImpl propInfo = (PropertyInfoImpl) o;
                propInfo.setId(idFactory.createPropertyId(id, propInfo.getName()));
            }
        }
    }

    void addChildInfo(ChildInfo childInfo) {
        if (childInfos == null) {
            childInfos = new LinkedHashSet<ChildInfo>();
        }
        childInfos.add(childInfo);
    }

    void setNumberOfChildNodes(long numberOfChildNodes) {
        if (numberOfChildNodes == 0) {
            childInfos = Collections.<ChildInfo>emptySet();
        } // else: wait for calls to #addChildInfo
    }

    boolean isCompleted() {
        return !(id == null || primaryNodeTypeName == null || propertyInfos.isEmpty());
    }

    void checkCompleted() throws RepositoryException {
        if (!isCompleted()) {
            throw new RepositoryException("Incomplete NodeInfo");
        }
    }

    String getUniqueID() {
        return uniqueID;
    }
}