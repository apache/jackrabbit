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

import java.util.ArrayList;
import java.util.List;
import javax.jcr.RepositoryException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.Tree;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.tree.AbstractTree;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

class DocumentTree extends AbstractTree {

    private final List<Property> properties = new ArrayList<Property>();

    protected DocumentTree(Name nodeName, Name ntName, String uniqueId, NamePathResolver resolver) {
        super(nodeName, ntName, uniqueId, resolver);
    }

    //-------------------------------------------------------< AbstractTree >---
    @Override
    protected Tree createChild(Name name, Name primaryTypeName, String uniqueId) {
        return new DocumentTree(name, primaryTypeName, uniqueId, getResolver());
    }

    //---------------------------------------------------------------< Tree >---
    @Override
    public void addProperty(NodeId parentId, Name propertyName, int propertyType, QValue value) throws RepositoryException {
        addProperty(parentId, propertyName, propertyType, new QValue[]{value});
    }

    @Override
    public void addProperty(NodeId parentId, Name propertyName, int propertyType, QValue[] values) throws RepositoryException {
        properties.add(new Property(propertyName, propertyType, values));
    }

    //--------------------------------------------------------------------------
    Document toDocument() throws RepositoryException {
        try {
            Document body = DomUtil.createDocument();
            buildNodeInfo(body, this);
            return body;
        } catch (ParserConfigurationException e) {
            throw new RepositoryException(e);
        }
    }

    //--------------------------------------------------------------------------
    private void buildNodeInfo(Node parent, DocumentTree tree) throws RepositoryException {
        Element node = BatchUtils.createNodeElement(parent, tree.getName(), tree.getPrimaryTypeName(), tree.getUniqueId(), getResolver());
        for (Property prop : properties) {
            BatchUtils.importProperty(node, prop.name, prop.type, prop.values, getResolver());
        }
        for (Tree child : tree.getChildren()) {
            buildNodeInfo(node, (DocumentTree) child);
        }
    }

    private final static class Property {

        private final Name name;
        private final int type;
        private final QValue[] values;

        private Property(Name name, int type, QValue[] values) {
            this.name = name;
            this.type = type;
            this.values = values;
        }
    }
}
