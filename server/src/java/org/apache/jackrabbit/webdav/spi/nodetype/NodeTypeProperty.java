/*
 * Copyright 2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.webdav.spi.nodetype;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.AbstractDavProperty;
import org.jdom.Element;

import javax.jcr.nodetype.NodeType;
import java.util.*;

/**
 * <code>NodeTypeProperty</code>...
 */
public class NodeTypeProperty extends AbstractDavProperty {

    private static Logger log = Logger.getLogger(NodeTypeProperty.class);

    private final NodeTypeElement[] value;

    public NodeTypeProperty(DavPropertyName name, NodeType nodeType, boolean isProtected) {
        super(name, isProtected);
        value = new NodeTypeElement[] {new NodeTypeElement(nodeType)};
    }

    public NodeTypeProperty(DavPropertyName name, NodeType[] nodeTypes, boolean isProtected) {
        super(name, isProtected);
        value = NodeTypeElement.create(nodeTypes);
    }

    /**
     * Create a new <code>NodeTypeProperty</code> from the specified general
     * DavProperty object.
     *
     * @param property
     * @throws IllegalArgumentException if the content of the specified property
     * contains elements other than {@link NodeTypeConstants#XML_NODETYPE}.
     */
    public NodeTypeProperty(DavProperty property) {
        super(property.getName(), property.isProtected());

        if (property.getValue() instanceof List) {
            List ntElemList = new ArrayList();
            Iterator it = ((List) property.getValue()).iterator();
            while (it.hasNext()) {
                Object content = it.next();
                if (content instanceof Element) {
                    ntElemList.add(new NodeTypeElement((Element)content));
                }
            }
            value = (NodeTypeElement[]) ntElemList.toArray(new NodeTypeElement[ntElemList.size()]);
        } else {
            value = new NodeTypeElement[0];
        }
    }

    /**
     * Return a set of nodetype names present in this property.
     *
     * @return set of nodetype names
     */
    public Set getNodeTypeNames() {
        HashSet names = new HashSet();
        Object val = getValue();
        if (val instanceof NodeTypeElement[]) {
            NodeTypeElement[] elems = (NodeTypeElement[])val;
            for (int i = 0; i < elems.length; i++) {
                String ntName = elems[i].getNodeTypeName();
                if (ntName != null) {
                    names.add(ntName);
                }
            }
        }
        return names;
    }

    /**
     * Returns the value of this property which is an array of {@link NodeTypeElement}
     * objects.
     *
     * @return an array of {@link NodeTypeElement}s
     */
    public Object getValue() {
        return value;
    }
}