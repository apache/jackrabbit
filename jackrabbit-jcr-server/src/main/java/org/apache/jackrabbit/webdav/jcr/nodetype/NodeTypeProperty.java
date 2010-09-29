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
package org.apache.jackrabbit.webdav.jcr.nodetype;

import org.apache.jackrabbit.commons.webdav.NodeTypeConstants;
import org.apache.jackrabbit.commons.webdav.NodeTypeUtil;
import org.apache.jackrabbit.webdav.property.AbstractDavProperty;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.jcr.nodetype.NodeType;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * <code>NodeTypeProperty</code>...
 */
public class NodeTypeProperty extends AbstractDavProperty<Set<String>> implements NodeTypeConstants {

    private final Set<String> nodetypeNames = new HashSet<String>();

    public NodeTypeProperty(DavPropertyName name, NodeType nodeType, boolean isProtected) {
        this(name, new NodeType[]{nodeType}, isProtected);
    }

    public NodeTypeProperty(DavPropertyName name, NodeType[] nodeTypes, boolean isProtected) {
        super(name, isProtected);
        for (NodeType nt : nodeTypes) {
            if (nt != null) {
                nodetypeNames.add(nt.getName());
            }
        }
    }

    public NodeTypeProperty(DavPropertyName name, String[] nodeTypeNames, boolean isProtected) {
        super(name, isProtected);
        for (String nodeTypeName : nodeTypeNames) {
            if (nodeTypeName != null) {
                nodetypeNames.add(nodeTypeName);
            }
        }
    }

    /**
     * Create a new <code>NodeTypeProperty</code> from the specified general
     * DavProperty object.
     *
     * @param property
     */
    public NodeTypeProperty(DavProperty<?> property) {
        super(property.getName(), property.isInvisibleInAllprop());
        if (property instanceof NodeTypeProperty) {
            nodetypeNames.addAll(((NodeTypeProperty) property).nodetypeNames);
        } else {
            nodetypeNames.addAll(NodeTypeUtil.ntNamesFromXml(property.getValue()));
        }
    }

    /**
     * Return a set of node type names present in this property.
     *
     * @return set of node type names
     */
    public Set<String> getNodeTypeNames() {
        return Collections.unmodifiableSet(nodetypeNames);
    }

    /**
     * Returns the value of this property which is a Set of nodetype names.
     *
     * @return a Set of nodetype names (String).
     */
    public Set<String> getValue() {
        return Collections.unmodifiableSet(nodetypeNames);
    }

    /**
     * @see org.apache.jackrabbit.webdav.xml.XmlSerializable#toXml(Document)
     */
    @Override
    public Element toXml(Document document) {
        Element elem = getName().toXml(document);
        for (String name : getNodeTypeNames()) {
            elem.appendChild(NodeTypeUtil.ntNameToXml(name, document));
        }
        return elem;
    }
}
