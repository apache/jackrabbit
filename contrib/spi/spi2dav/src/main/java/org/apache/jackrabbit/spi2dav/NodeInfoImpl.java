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
import org.apache.jackrabbit.spi.NodeInfo;
import org.apache.jackrabbit.spi.IdIterator;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.ItemId;
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

    private QName primaryNodeTypeName = null;
    private QName[] mixinNodeTypeNames = new QName[0];
    private List references = new ArrayList();

    private final List nodeIds = new ArrayList();
    private final List propertyIds = new ArrayList();

    public NodeInfoImpl(NodeId id, NodeId parentId, DavPropertySet propSet,
                        NamespaceResolver nsResolver) throws RepositoryException {
        super(parentId);

        // set id
        this.id = id;

        // retrieve name
        if (id.getRelativePath() == null) {
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
            Path.PathElement el = id.getRelativePath().getNameElement();
            qName = (Path.CURRENT_ELEMENT == el) ? QName.ROOT : el.getName();
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

    public QName getNodetype() {
        return primaryNodeTypeName;
    }

    public QName[] getMixins() {
        return mixinNodeTypeNames;
    }

    public PropertyId[] getReferences() {
        return (PropertyId[]) references.toArray(new PropertyId[references.size()]);
    }

    public IdIterator getNodeIds() {
        return new IteratorHelper(nodeIds);
    }

    public IdIterator getPropertyIds() {
        return new IteratorHelper(propertyIds);
    }

    //--------------------------------------------------------------------------
    void addReference(PropertyId referenceId) {
        references.add(referenceId);
    }

    void addChildId(ItemId childId) {
        if (childId.denotesNode()) {
           nodeIds.add(childId);
        } else {
           propertyIds.add(childId);
        }
    }
}