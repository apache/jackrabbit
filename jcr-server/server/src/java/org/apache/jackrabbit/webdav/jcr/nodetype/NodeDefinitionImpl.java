/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
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
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Document;

import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeDefinition;

/**
 * <code>NodeDefinitionImpl</code>...
 */
public final class NodeDefinitionImpl extends ItemDefinitionImpl implements NodeDefinition {

    private static Logger log = Logger.getLogger(NodeDefinitionImpl.class);

    private final NodeType[] requiredPrimaryTypes;
    private final NodeType defaultPrimaryType;
    private final boolean allowsSameNameSiblings;

    private NodeDefinitionImpl(NodeDefinition definition) {
	super(definition);

	requiredPrimaryTypes = definition.getRequiredPrimaryTypes();
	defaultPrimaryType = definition.getDefaultPrimaryType();
	allowsSameNameSiblings = definition.allowsSameNameSiblings();
    }

    public static NodeDefinitionImpl create(NodeDefinition definition) {
	if (definition instanceof NodeDefinitionImpl) {
	    return (NodeDefinitionImpl) definition;
	} else {
	    return new NodeDefinitionImpl(definition);
	}
    }

    //--------------------------------------------------< NodeDef interface >---
    /**
     * @see javax.jcr.nodetype.NodeDefinition#getRequiredPrimaryTypes()
     */
    public NodeType[] getRequiredPrimaryTypes() {
	return requiredPrimaryTypes;
    }

    /**
     * @see javax.jcr.nodetype.NodeDefinition#getDefaultPrimaryType()
     */
    public NodeType getDefaultPrimaryType() {
	return defaultPrimaryType;
    }

    /**
     * @see javax.jcr.nodetype.NodeDefinition#allowsSameNameSiblings()
     */
    public boolean allowsSameNameSiblings() {
        return allowsSameNameSiblings;
    }

    //-------------------------------------< implementation specific method >---
    /**
     * Returns xml representation
     *
     * @return xml representation
     * @param document
     */
    public Element toXml(Document document) {
        Element elem = super.toXml(document);
        elem.setAttribute(SAMENAMESIBLINGS_ATTRIBUTE, Boolean.toString(allowsSameNameSiblings()));
        // defaultPrimaryType can be 'null'
        NodeType defaultPrimaryType = getDefaultPrimaryType();
        if (defaultPrimaryType != null) {
            elem.setAttribute(DEFAULTPRIMARYTYPE_ATTRIBUTE, defaultPrimaryType.getName());
        }
        // reqPrimaryTypes: minimal set is nt:base.
        NodeType[] nts = getRequiredPrimaryTypes();
        Element reqPrimaryTypes = document.createElement(REQUIREDPRIMARYTYPES_ELEMENT);
	for (int i = 0; i < nts.length; i++) {
            Element rptElem = document.createElement(REQUIREDPRIMARYTYPE_ELEMENT);
            DomUtil.setText(rptElem, nts[i].getName());
	    reqPrimaryTypes.appendChild(rptElem);

	}
        elem.appendChild(reqPrimaryTypes);
        return elem;
    }

    /**
     * Returns {@link #CHILDNODEDEFINITION_ELEMENT}
     *
     * @return always returns {@link #CHILDNODEDEFINITION_ELEMENT}.
     */
    String getElementName() {
	return CHILDNODEDEFINITION_ELEMENT;
    }
}