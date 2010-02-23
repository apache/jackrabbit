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

import org.apache.jackrabbit.webdav.property.AbstractDavProperty;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.jcr.nodetype.NodeType;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <code>NodeTypeProperty</code>...
 */
public class NodeTypeProperty extends AbstractDavProperty<Set<String>> implements NodeTypeConstants {

    private static Logger log = LoggerFactory.getLogger(NodeTypeProperty.class);

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
            // assume property has be built from xml
            Object propValue = property.getValue();
            if (propValue instanceof List) {
                retrieveNodeTypeNames(((List<?>)propValue));
            } else if (propValue instanceof Element) {
                List<Element> l = Collections.singletonList((Element) propValue);
                retrieveNodeTypeNames(l);
            } else {
                log.debug("NodeTypeProperty '" + property.getName() + "' has no/unparsable value.");
            }
        }
    }

    private void retrieveNodeTypeNames(List<?> elementList) {
        for (Object content : elementList) {
            if (!(content instanceof Element)) {
                continue;
            }
            Element el = (Element) content;
            if (XML_NODETYPE.equals(el.getLocalName()) && NodeTypeConstants.NAMESPACE.isSame(el.getNamespaceURI())) {
                String nodetypeName = DomUtil.getChildText(el, XML_NODETYPENAME, NodeTypeConstants.NAMESPACE);
                if (nodetypeName != null && !"".equals(nodetypeName)) {
                    nodetypeNames.add(nodetypeName);
                }
            } else {
                log.debug("'dcr:nodetype' element expected -> ignoring element '" + ((Element) content).getNodeName() + "'");
            }
        }
    }

    /**
     * Return a set of nodetype names present in this property.
     *
     * @return set of nodetype names
     */
    public Set<String> getNodeTypeNames() {
        return nodetypeNames;
    }

    /**
     * Returns the value of this property which is a Set of nodetype names.
     *
     * @return a Set of nodetype names (String).
     */
    public Set<String> getValue() {
        return nodetypeNames;
    }

    /**
     * @see org.apache.jackrabbit.webdav.xml.XmlSerializable#toXml(Document)
     */
    @Override
    public Element toXml(Document document) {
        Element elem = getName().toXml(document);
        for (String name : getNodeTypeNames()) {
            Element ntElem = DomUtil.addChildElement(elem, XML_NODETYPE, NodeTypeConstants.NAMESPACE);
            DomUtil.addChildElement(ntElem, XML_NODETYPENAME, NodeTypeConstants.NAMESPACE, name);
        }
        return elem;
    }
}
