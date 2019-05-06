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
package org.apache.jackrabbit.spi2jcr;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.Tree;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.tree.AbstractTree;

class XmlTree extends AbstractTree {

    private final StringBuilder properties = new StringBuilder();

    protected XmlTree(Name nodeName, Name ntName, String uniqueId, NamePathResolver resolver) {
        super(nodeName, ntName, uniqueId, resolver);
    }

    //-------------------------------------------------------< AbstractTree >---
    @Override
    protected Tree createChild(Name name, Name primaryTypeName, String uniqueId) {
        return new XmlTree(name, primaryTypeName, uniqueId, getResolver());
    }

    //---------------------------------------------------------------< Tree >---
    @Override
    public void addProperty(NodeId parentId, Name propertyName, int propertyType, QValue value) throws RepositoryException {
        addProperty(parentId, propertyName, propertyType, new QValue[] {value});

    }

    @Override
    public void addProperty(NodeId parentId, Name propertyName, int propertyType, QValue[] values) throws RepositoryException {
        properties.append("<sv:property sv:name=\"").append(getResolver().getJCRName(propertyName)).append("\"");
        properties.append(" sv:type=\"").append(PropertyType.nameFromValue(propertyType) + "\">");
        for (QValue value : values) {
            properties.append("<sv:value>").append(value.getString()).append("</sv:value>");
        }
        properties.append("</sv:property>");
    }

    //--------------------------------------------------------------------------
    String toXML() throws RepositoryException {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        createXMLNodeFragment(xml, this, getResolver(), true);
        return xml.toString();
    }

    private static void createXMLNodeFragment(StringBuilder xml, XmlTree tree, NamePathResolver resolver, boolean includeNsInfo) throws RepositoryException {
        xml.append("<sv:node ");
        if (includeNsInfo) {
           xml.append("xmlns:jcr=\"http://www.jcp.org/jcr/1.0\" xmlns:sv=\"http://www.jcp.org/jcr/sv/1.0\" ");
        }
        xml.append("sv:name=\"").append(resolver.getJCRName(tree.getName())).append("\">");
        // jcr:primaryType
        xml.append("<sv:property sv:name=\"jcr:primaryType\" sv:type=\"Name\">");
        xml.append("<sv:value>").append(resolver.getJCRName(tree.getPrimaryTypeName())).append("</sv:value>");
        xml.append("</sv:property>");
        // jcr:uuid
        String uniqueId = tree.getUniqueId();
        if (uniqueId != null) {
            xml.append("<sv:property sv:name=\"jcr:uuid\" sv:type=\"String\">");
            xml.append("<sv:value>").append(uniqueId).append("</sv:value>");
            xml.append("</sv:property>");
        }

        // create the xml fragment for all the child properties.
        xml.append(tree.properties);

        // create xml for all child nodes
        for (Tree child : tree.getChildren()) {
            createXMLNodeFragment(xml, (XmlTree) child, resolver, false);
        }

        xml.append("</sv:node>");
    }
}
