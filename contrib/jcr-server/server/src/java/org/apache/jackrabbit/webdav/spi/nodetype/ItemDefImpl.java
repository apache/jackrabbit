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

import javax.jcr.nodetype.ItemDef;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.OnParentVersionAction;

/**
 * <code>ItemDefImpl</code>...
 */
abstract public class ItemDefImpl implements ItemDef, NodeTypeConstants {

    private static Logger log = Logger.getLogger(ItemDefImpl.class);

    private final String name;
    private NodeType declaringNodeType;
    private final boolean isAutoCreated;
    private final boolean isMandatory;
    private final boolean isProtected;
    private final int onParentVersion;

    ItemDefImpl(ItemDef definition) {
	if (definition == null) {
            throw new IllegalArgumentException("PropDef argument can not be null");
        }

        name = definition.getName();
	declaringNodeType = definition.getDeclaringNodeType();
	isAutoCreated = definition.isAutoCreate();
	isMandatory = definition.isMandatory();
	isProtected = definition.isProtected();
	onParentVersion = definition.getOnParentVersion();
    }

    public NodeType getDeclaringNodeType() {
	return declaringNodeType;
    }

    public String getName() {
	return name;
    }

    public boolean isAutoCreate() {
	return isAutoCreated;
    }

    public boolean isMandatory() {
	return isMandatory;
    }

    public int getOnParentVersion() {
	return onParentVersion;
    }

    public boolean isProtected() {
	return isProtected;
    }

    //-------------------------------------< implementation specific method >---
    /**
     * Returns the Xml representation of a {@link ItemDef} object.
     *
     * @return Xml representation of the specified {@link ItemDef def}.
     */
    public Element toXml() {
	Element elem = new Element(getElementName());
        elem.setAttribute(ATTR_NAME, getName());
        elem.setAttribute(ATTR_AUTOCREATE, Boolean.toString(isAutoCreate()));
        elem.setAttribute(ATTR_MANDATORY, Boolean.toString(isMandatory()));
        elem.setAttribute(ATTR_ONPARENTVERSION, OnParentVersionAction.nameFromValue(getOnParentVersion()));
        elem.setAttribute(ATTR_PROTECTED, Boolean.toString(isProtected()));
        return elem;
    }

    public abstract String getElementName();
}