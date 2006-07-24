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

import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.jcr.nodetype.NodeTypeProperty;
import org.apache.jackrabbit.webdav.jcr.ItemResourceConstants;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.HrefProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.name.NameException;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.NameFormat;
import org.apache.jackrabbit.spi.NodeInfo;
import org.apache.jackrabbit.spi.IdIterator;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.SessionInfo;
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

    private QName primaryNodeTypeName = null;
    private QName[] mixinNodeTypeNames = new QName[0];
    private List references = new ArrayList();

    private final List nodeIds = new ArrayList();
    private final List propertyIds = new ArrayList();

    public NodeInfoImpl(MultiStatusResponse response, List childItemResponses, URIResolver uriResolver, SessionInfo sessionInfo) throws RepositoryException, DavException {
        super(response, uriResolver, sessionInfo);

        id = uriResolver.getNodeId(getParentId(), response);
        DavPropertySet propSet = response.getProperties(DavServletResponse.SC_OK);
        try {
            if (propSet.contains(ItemResourceConstants.JCR_PRIMARYNODETYPE)) {
                Iterator it = new NodeTypeProperty(propSet.get(ItemResourceConstants.JCR_PRIMARYNODETYPE)).getNodeTypeNames().iterator();
                if (it.hasNext()) {
                    String jcrName = it.next().toString();
                    primaryNodeTypeName = NameFormat.parse(jcrName, uriResolver);
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
                    mixinNodeTypeNames[i] = NameFormat.parse(jcrName, uriResolver);
                    i++;
                }
            }
        } catch (NameException e) {
            throw new RepositoryException("Error while resolving nodetype names: " + e.getMessage());
        }

        if (propSet.contains(ItemResourceConstants.JCR_REFERENCES)) {
            HrefProperty refProp = new HrefProperty(propSet.get(ItemResourceConstants.JCR_REFERENCES));
            Iterator hrefIter = refProp.getHrefs().iterator();
            while(hrefIter.hasNext()) {
                String propertyHref = hrefIter.next().toString();
                PropertyId propertyId = uriResolver.getPropertyId(propertyHref, sessionInfo);
                references.add(propertyId);
            }
        }

        // build the child-item entries
        Iterator it = childItemResponses.iterator();
        while (it.hasNext()) {
            MultiStatusResponse resp = (MultiStatusResponse)it.next();
            DavPropertySet childProps = resp.getProperties(DavServletResponse.SC_OK);
            if (childProps.contains(DavPropertyName.RESOURCETYPE) &&
                childProps.get(DavPropertyName.RESOURCETYPE).getValue() != null) {
                // any other resource type than default (empty) is represented by a node item
                NodeId childId = uriResolver.getNodeId(id, resp);
                nodeIds.add(childId);
            } else {
                PropertyId propertyId = uriResolver.getPropertyId(id, resp);
                propertyIds.add(propertyId);
            }
        }
    }

    public boolean denotesNode() {
        return true;
    }

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
}