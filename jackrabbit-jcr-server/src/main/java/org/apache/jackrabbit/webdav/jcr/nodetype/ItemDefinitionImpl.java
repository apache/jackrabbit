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
import org.apache.jackrabbit.webdav.xml.XmlSerializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.OnParentVersionAction;

/**
 * <code>ItemDefinitionImpl</code>...
 */
abstract public class ItemDefinitionImpl implements ItemDefinition, NodeTypeConstants, XmlSerializable {

    private static Logger log = LoggerFactory.getLogger(ItemDefinitionImpl.class);

    private final String name;
    private NodeType declaringNodeType;
    private final boolean isAutoCreated;
    private final boolean isMandatory;
    private final boolean isProtected;
    private final int onParentVersion;

    ItemDefinitionImpl(ItemDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("PropDef argument can not be null");
        }
        name = definition.getName();
        declaringNodeType = definition.getDeclaringNodeType();
        isAutoCreated = definition.isAutoCreated();
        isMandatory = definition.isMandatory();
        isProtected = definition.isProtected();
        onParentVersion = definition.getOnParentVersion();
    }

    /**
     * @see ItemDefinition#getDeclaringNodeType()
     */
    public NodeType getDeclaringNodeType() {
        return declaringNodeType;
    }

    /**
     * @see ItemDefinition#getName()
     */
    public String getName() {
        return name;
    }

    /**
     * @see ItemDefinition#isAutoCreated()
     */
    public boolean isAutoCreated() {
        return isAutoCreated;
    }

    /**
     * @see ItemDefinition#isMandatory()
     */
    public boolean isMandatory() {
        return isMandatory;
    }

    /**
     * @see ItemDefinition#getOnParentVersion()
     */
    public int getOnParentVersion() {
        return onParentVersion;
    }

    /**
     * @see ItemDefinition#isProtected()
     */
    public boolean isProtected() {
        return isProtected;
    }

    //------------------------------------------< XmlSerializable interface >---
    /**
     * Returns the Xml representation of a {@link ItemDefinition} object.
     *
     * @return Xml representation of the specified {@link ItemDefinition def}.
     * @param document
     */
    public Element toXml(Document document) {
        Element elem = document.createElement(getElementName());
        NodeType dnt = getDeclaringNodeType();
        if (dnt != null) {
            elem.setAttribute(DECLARINGNODETYPE_ATTRIBUTE, dnt.getName());
        }
        elem.setAttribute(NAME_ATTRIBUTE, getName());
        elem.setAttribute(AUTOCREATED_ATTRIBUTE, Boolean.toString(isAutoCreated()));
        elem.setAttribute(MANDATORY_ATTRIBUTE, Boolean.toString(isMandatory()));
        elem.setAttribute(ONPARENTVERSION_ATTRIBUTE, OnParentVersionAction.nameFromValue(getOnParentVersion()));
        elem.setAttribute(PROTECTED_ATTRIBUTE, Boolean.toString(isProtected()));
        return elem;
    }

    //-------------------------------------< implementation specific method >---
    /**
     * Returns the name of the root element
     *
     * @return the name of the root element
     */
    abstract String getElementName();
}