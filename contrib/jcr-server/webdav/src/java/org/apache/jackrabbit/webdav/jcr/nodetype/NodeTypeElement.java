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
package org.apache.jackrabbit.webdav.jcr.nodetype;

import org.apache.log4j.Logger;
import org.jdom.Element;

import javax.jcr.nodetype.NodeType;

/**
 * <code>NodeTypeElement</code>...
 */
public class NodeTypeElement extends Element implements NodeTypeConstants {

    private static Logger log = Logger.getLogger(NodeTypeElement.class);

    public NodeTypeElement(NodeType nt) {
        super(XML_NODETYPE, NodeTypeConstants.NAMESPACE);
        addNodeTypeNameElement(nt.getName());
    }

    public NodeTypeElement(Element ntElement) {
        super(XML_NODETYPE, NodeTypeConstants.NAMESPACE);
        if (!XML_NODETYPE.equals(ntElement.getName())) {
            throw new IllegalArgumentException("jcr:nodetype element expected.");
        }
        addNodeTypeNameElement(ntElement.getChildText(XML_NODETYPENAME, NodeTypeConstants.NAMESPACE));
    }

    private void addNodeTypeNameElement(String nodeTypeName) {
        if (nodeTypeName != null) {
        addContent(new Element(XML_NODETYPENAME, NodeTypeConstants.NAMESPACE).setText(nodeTypeName));
        }
    }

    public String getNodeTypeName() {
        return getChildText(XML_NODETYPENAME, NodeTypeConstants.NAMESPACE);
    }
    
    public static NodeTypeElement[] create(NodeType[] nts) {
        NodeTypeElement[] elems = new NodeTypeElement[nts.length];
        for (int i = 0; i < nts.length; i++) {
            elems[i] = new NodeTypeElement(nts[i]);
        }
        return elems;
    }
}