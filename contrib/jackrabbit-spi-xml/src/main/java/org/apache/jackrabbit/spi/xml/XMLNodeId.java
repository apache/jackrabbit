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
package org.apache.jackrabbit.spi.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.jackrabbit.name.MalformedPathException;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.ChildInfo;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PropertyId;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLNodeId implements NodeId, PropertyId, ChildInfo {

    private final Node node;

    public XMLNodeId(Node node) {
        this.node = node;
    }

    public NodeId getParentId() {
        Node parent = node.getParentNode();
        if (parent != null) {
            return new XMLNodeId(parent);
        } else {
            return null;
        }
    }

    public Iterator getPropertyIds() {
        return new XMLPropertyIdIterator(
                new XMLPrimaryTypeId(this), node.getAttributes());
    }

    public Iterator getChildInfos() {
        Collection infos = new ArrayList();
        NodeList nodes = node.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node child = nodes.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                infos.add(new XMLNodeId(child));
            }
        }
        return infos.iterator();
    }

    public XMLNodeId getNodeId(Path path) {
        Node current = node;
        Path.PathElement[] elements = path.getElements();
        for (int i = 0; i < elements.length; i++) {
            if (elements[i].denotesRoot()) {
                current = node;
            } else if (elements[i].denotesParent()) {
                if (current.getParentNode() != null) {
                    current = current.getParentNode();
                } else {
                    return null;
                }
            } else if (elements[i].denotesCurrent()) {
                // do nothing
            } else {
                QName name = elements[i].getName();
                int index = elements[i].getNormalizedIndex();
                NodeList nodes = current.getChildNodes();
                for (int j = 0; index > 0 && j < nodes.getLength(); j++) {
                    Node child = nodes.item(j);
                    if (child.getNodeType() == Node.ELEMENT_NODE
                            && name.equals(new XMLNodeId(child).getName())) {
                        if (--index == 0) {
                            current = child;
                        }
                    }
                }
                if (index > 0) {
                    return null;
                }
            }
        }
        return new XMLNodeId(current);
    }

    public XMLNodeId getPropertyId(QName name) {
        String uri = name.getNamespaceURI();
        if (QName.NS_DEFAULT_URI.equals(uri)) {
            uri = null;
        }

        NamedNodeMap attributes = node.getAttributes();
        Node attribute = attributes.getNamedItemNS(uri, name.getLocalName());
        if (attribute != null) {
            return new XMLNodeId(attribute);
        } else {
            return null;
        }
    }

    public String getValue() {
        return node.getNodeValue();
    }

    //--------------------------------------------------------------< NodeId >

    public boolean denotesNode() {
        return node.getNodeType() == Node.ELEMENT_NODE
            || node.getNodeType() == Node.DOCUMENT_NODE;
    }

    public Path getPath() {
        try {
            Node parent = node.getParentNode();
            if (parent != null) {
                Path parentPath = new XMLNodeId(parent).getPath();
                int index = getIndex();
                if (index > 0) {
                    return Path.create(parentPath, getName(), index, false);
                } else {
                    return Path.create(parentPath, getName(), false);
                }
            } else {
                return Path.ROOT;
            }
        } catch (MalformedPathException e) {
            throw new IllegalStateException("Invalid path: " + e.getMessage());
        }
    }

    /**
     * Returns <code>null</code>.
     *
     * @return <code>null</code>
     */
    public String getUniqueID() {
        return null;
    }

    public int getIndex() {
        int index = 0;
        int count = 0;

        Node parent = node.getParentNode();
        if (parent != null) {
            NodeList nodes = parent.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node current = nodes.item(i);
                if (current.getNodeType() != Node.ELEMENT_NODE) {
                    // skip
                } else if (current == node) {
                    index = count + 1;
                } else if (getName().equals(new XMLNodeId(current).getName())) {
                    count++;
                }
            }
        }

        if (count > 0) {
            return index;
        } else {
            return 0;
        }
    }

    public QName getName() {
        if (node.getParentNode() == null) {
            return new QName(QName.NS_DEFAULT_URI, "");
        } else if (node.getNamespaceURI() == null) {
            return new QName(QName.NS_DEFAULT_URI, node.getLocalName());
        } else {
            return new QName(node.getNamespaceURI(), node.getLocalName());
        }
    }

    public QName getQName() {
        return getName();
    }

    //--------------------------------------------------------------< Object >

    public boolean equals(Object that) {
        return that instanceof XMLNodeId
            && node.equals(((XMLNodeId) that).node);
    }

    public int hashCode() {
        return node.hashCode();
    }

}
