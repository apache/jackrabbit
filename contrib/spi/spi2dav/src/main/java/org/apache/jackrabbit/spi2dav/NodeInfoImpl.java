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

import org.apache.jackrabbit.webdav.jcr.nodetype.NodeTypeProperty;
import org.apache.jackrabbit.webdav.jcr.ItemResourceConstants;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.name.NameException;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.NameFormat;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.MalformedPathException;
import org.apache.jackrabbit.spi.NodeInfo;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PropertyId;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import java.util.List;
import java.util.Iterator;
import java.util.Set;
import java.util.ArrayList;

/**
 * <code>NodeInfoImpl</code>...
 */
public class NodeInfoImpl extends ItemInfoImpl implements NodeInfo {

    private static Logger log = LoggerFactory.getLogger(NodeInfoImpl.class);

    private final NodeId id;
    private final QName qName;
    private final int index;

    private final QName primaryNodeTypeName;
    private final QName[] mixinNodeTypeNames;

    private final List references = new ArrayList();
    private final List propertyIds = new ArrayList();

    public NodeInfoImpl(NodeId id, NodeId parentId, DavPropertySet propSet,
                        NamespaceResolver nsResolver) throws RepositoryException, MalformedPathException {
        super(parentId, propSet, nsResolver);

        // set id
        this.id = id;

        // retrieve name
        if (id.getPath() == null) {
            DavProperty nameProp = propSet.get(ItemResourceConstants.JCR_NAME);
            if (nameProp != null && nameProp.getValue() != null) {
                // not root node
                // jcrName is transported from jackrabbit-webdav -> convert
                // note, that unescaping is not required.
                String jcrName = nameProp.getValue().toString();
                try {
                    qName = NameFormat.parse(jcrName, nsResolver);
                } catch (NameException e) {
                    throw new RepositoryException("Unable to build ItemInfo object, invalid name found: " + jcrName);
                }
            } else {
                // root
                qName = QName.ROOT;
            }
        } else {
            Path.PathElement el = id.getPath().getNameElement();
            qName = (Path.ROOT_ELEMENT == el) ? QName.ROOT : el.getName();
        }

        DavProperty indexProp = propSet.get(ItemResourceConstants.JCR_INDEX);
        if (indexProp != null && indexProp.getValue() != null) {
            index = Integer.parseInt(indexProp.getValue().toString());
        } else {
            index = Path.INDEX_DEFAULT;
        }

        // retrieve properties
        try {
            if (propSet.contains(ItemResourceConstants.JCR_PRIMARYNODETYPE)) {
                Iterator it = new NodeTypeProperty(propSet.get(ItemResourceConstants.JCR_PRIMARYNODETYPE)).getNodeTypeNames().iterator();
                if (it.hasNext()) {
                    String jcrName = it.next().toString();
                    primaryNodeTypeName = NameFormat.parse(jcrName, nsResolver);
                } else {
                    throw new RepositoryException("Missing primary nodetype for node " + id + ".");
                }
            } else {
                throw new RepositoryException("Missing primary nodetype for node " + id);
            }
            if (propSet.contains(ItemResourceConstants.JCR_MIXINNODETYPES)) {
                Set mixinNames = new NodeTypeProperty(propSet.get(ItemResourceConstants.JCR_MIXINNODETYPES)).getNodeTypeNames();
                mixinNodeTypeNames = new QName[mixinNames.size()];
                Iterator it = mixinNames.iterator();
                int i = 0;
                while(it.hasNext()) {
                    String jcrName = it.next().toString();
                    mixinNodeTypeNames[i] = NameFormat.parse(jcrName, nsResolver);
                    i++;
                }
            } else {
                mixinNodeTypeNames = QName.EMPTY_ARRAY;
            }
        } catch (NameException e) {
            throw new RepositoryException("Error while resolving nodetype names: " + e.getMessage());
        }
    }

    //-----------------------------------------------------------< ItemInfo >---
    public boolean denotesNode() {
        return true;
    }

    public QName getQName() {
        return qName;
    }

    //-----------------------------------------------------------< NodeInfo >---
    public NodeId getId() {
        return id;
    }

    public int getIndex() {
        return index;
    }

    public QName getNodetype() {
        return primaryNodeTypeName;
    }

    public QName[] getMixins() {
        return mixinNodeTypeNames;
    }

    public PropertyId[] getReferences() {
        return (PropertyId[]) references.toArray(new PropertyId[references.size()]);
    }

    public Iterator getPropertyIds() {
        return propertyIds.iterator();
    }

    //--------------------------------------------------------------------------
    void addReference(PropertyId referenceId) {
        references.add(referenceId);
    }

    void addPropertyId(PropertyId childId) {
        propertyIds.add(childId);
    }
}