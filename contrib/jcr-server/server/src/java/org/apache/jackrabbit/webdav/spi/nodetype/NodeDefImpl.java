/*
 * Copyright 2004 The Apache Software Foundation.
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
import org.jdom.Element;

import javax.jcr.nodetype.NodeDef;
import javax.jcr.nodetype.NodeType;

/**
 * <code>NodeDefImpl</code>...
 */
public final class NodeDefImpl extends ItemDefImpl implements NodeDef {

    private static Logger log = Logger.getLogger(NodeDefImpl.class);

    private final NodeType[] requiredPrimaryTypes;
    private final NodeType defaultPrimaryType;
    private final boolean allowSameNameSibs;

    private NodeDefImpl(NodeDef definition) {
	super(definition);

	requiredPrimaryTypes = definition.getRequiredPrimaryTypes();
	defaultPrimaryType = definition.getDefaultPrimaryType();
	allowSameNameSibs = definition.allowSameNameSibs();
    }

    public static NodeDefImpl create(NodeDef definition) {
	if (definition instanceof NodeDefImpl) {
	    return (NodeDefImpl) definition;
	} else {
	    return new NodeDefImpl(definition);
	}
    }

    //--------------------------------------------------< NodeDef interface >---
    public NodeType[] getRequiredPrimaryTypes() {
	return requiredPrimaryTypes;
    }

    public NodeType getDefaultPrimaryType() {
	return defaultPrimaryType;
    }

    public boolean allowSameNameSibs() {
	return allowSameNameSibs;
    }

    //-------------------------------------< implementation specific method >---
    public Element toXml() {
        Element elem = super.toXml();

        elem.setAttribute(ATTR_SAMENAMESIBS, Boolean.toString(allowSameNameSibs()));

        // defaultPrimaryType can be 'null'
        NodeType defaultPrimaryType = getDefaultPrimaryType();
        if (defaultPrimaryType != null) {
            elem.setAttribute(ATTR_DEFAULTPRIMARYTYPE, defaultPrimaryType.getName());
        }
        // reqPrimaryTypes: minimal set is nt:base.
        NodeType[] nts = getRequiredPrimaryTypes();
        Element reqPrimaryTypes = new Element(XML_REQUIREDPRIMARYTYPES);
	for (int i = 0; i < nts.length; i++) {
	    reqPrimaryTypes.addContent(new Element(XML_REQUIREDPRIMARYTYPE).setText(nts[i].getName()));
	}
        elem.addContent(reqPrimaryTypes);

        return elem;
    }

    public String getElementName() {
	return XML_CHILDNODEDEF;
    }
}