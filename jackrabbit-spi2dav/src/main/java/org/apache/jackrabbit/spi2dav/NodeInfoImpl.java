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
package org.apache.jackrabbit.spi2dav;

import org.apache.jackrabbit.commons.webdav.JcrRemotingConstants;
import org.apache.jackrabbit.commons.webdav.NodeTypeUtil;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.NodeInfo;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.ChildInfo;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import java.util.Collection;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * <code>NodeInfoImpl</code>...
 */
public class NodeInfoImpl extends ItemInfoImpl implements NodeInfo {

    private static Logger log = LoggerFactory.getLogger(NodeInfoImpl.class);

    private final NodeId id;
    private final int index;

    private final Name primaryNodeTypeName;
    private final Name[] mixinNodeTypeNames;

    private final List<PropertyId> references = new ArrayList<PropertyId>();
    private final List<PropertyId> propertyIds = new ArrayList<PropertyId>();
    private List<ChildInfo> childInfos = null;

    public NodeInfoImpl(NodeId id, DavPropertySet propSet,
                        NamePathResolver resolver) throws RepositoryException, NameException {
        super(propSet, resolver);

        // set id
        this.id = id;

        DavProperty<?> indexProp = propSet.get(JcrRemotingConstants.JCR_INDEX_LN, ItemResourceConstants.NAMESPACE);
        if (indexProp != null && indexProp.getValue() != null) {
            index = Integer.parseInt(indexProp.getValue().toString());
        } else {
            index = Path.INDEX_DEFAULT;
        }

        // retrieve properties
        try {
            DavProperty<?> prop = propSet.get(JcrRemotingConstants.JCR_PRIMARYNODETYPE_LN, ItemResourceConstants.NAMESPACE);
            if (prop != null) {
                Iterator<String> it = NodeTypeUtil.ntNamesFromXml(prop.getValue()).iterator();
                if (it.hasNext()) {
                    String jcrName = it.next();
                    primaryNodeTypeName = resolver.getQName(jcrName);
                } else {
                    throw new RepositoryException("Missing primary nodetype for node " + id + ".");
                }
            } else {
                throw new RepositoryException("Missing primary nodetype for node " + id);
            }

            prop = propSet.get(JcrRemotingConstants.JCR_MIXINNODETYPES_LN, ItemResourceConstants.NAMESPACE);
            if (prop != null) {
                Collection<String> mixinNames = NodeTypeUtil.ntNamesFromXml(prop.getValue());
                mixinNodeTypeNames = new Name[mixinNames.size()];
                int i = 0;
                for (String jcrName : mixinNames) {
                    mixinNodeTypeNames[i] = resolver.getQName(jcrName);
                    i++;
                }
            } else {
                mixinNodeTypeNames = Name.EMPTY_ARRAY;
            }
        } catch (NameException e) {
            throw new RepositoryException("Error while resolving nodetype names: " + e.getMessage());
        }
    }

    //-----------------------------------------------------------< ItemInfo >---
    public boolean denotesNode() {
        return true;
    }

    //-----------------------------------------------------------< NodeInfo >---
    public NodeId getId() {
        return id;
    }

    public int getIndex() {
        return index;
    }

    public Name getNodetype() {
        return primaryNodeTypeName;
    }

    public Name[] getMixins() {
        return mixinNodeTypeNames;
    }

    public PropertyId[] getReferences() {
        return references.toArray(new PropertyId[references.size()]);
    }

    public Iterator<PropertyId> getPropertyIds() {
        return propertyIds.iterator();
    }

    public Iterator<ChildInfo> getChildInfos() {
        return (childInfos == null) ? null : childInfos.iterator();
    }

    //--------------------------------------------------------------------------
    void addReference(PropertyId referenceId) {
        references.add(referenceId);
    }

    void addPropertyId(PropertyId childId) {
        propertyIds.add(childId);
    }

    void addChildInfo(ChildInfo childInfo) {
        if (childInfos == null) {
            childInfos = new ArrayList<ChildInfo>();
        }
        childInfos.add(childInfo);
    }
}
