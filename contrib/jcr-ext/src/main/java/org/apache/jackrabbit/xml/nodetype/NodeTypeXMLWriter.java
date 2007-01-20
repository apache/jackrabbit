/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.xml.nodetype;

import java.util.Iterator;
import java.util.Map;

import javax.jcr.PropertyType;
import javax.jcr.version.OnParentVersionAction;

import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.state.nodetype.NodeDefinitionState;
import org.apache.jackrabbit.state.nodetype.NodeTypeManagerState;
import org.apache.jackrabbit.state.nodetype.NodeTypeState;
import org.apache.jackrabbit.state.nodetype.PropertyDefinitionState;
import org.apache.xerces.util.XMLChar;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * TODO
 */
class NodeTypeXMLWriter {

    private final NodeTypeManagerState nodeTypeManagerState;

    private Map namespaces;

    private ContentHandler handler;

    public NodeTypeXMLWriter(NodeTypeManagerState nodeTypeManagerState) {
        this.nodeTypeManagerState = nodeTypeManagerState;
    }

    private void startElement(String name) throws SAXException {
        handler.startElement("", name, name, null);
    }

    private void startElement(String name, Attributes attributes)
            throws SAXException {
        handler.startElement("", name, name, attributes);
    }

    private void endElement(String name) throws SAXException {
        handler.endElement("", name, name);
    }

    public void write(ContentHandler handler) throws SAXException {
        this.namespaces =
            NamespaceExtractor.extractNamespaces(nodeTypeManagerState);
        this.handler = handler;
        handler.startDocument();

        Iterator entries = namespaces.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();
            handler.startPrefixMapping(
                    (String) entry.getValue(), (String) entry.getKey()); 
        }

        write(nodeTypeManagerState);

        Iterator values = namespaces.values().iterator();
        while (values.hasNext()) {
            handler.endPrefixMapping((String) values.next()); 
        }

        handler.endDocument();
    }

    private void write(NodeTypeManagerState nodeTypeManagerState)
            throws SAXException {
        startElement("nodeTypes");
        NodeTypeState[] nodeTypeStates = nodeTypeManagerState.getNodeTypeStates();
        for (int i = 0; i < nodeTypeStates.length; i++) {
            write(nodeTypeStates[i]);
        }
        endElement("nodeTypes");
    }

    private void write(NodeTypeState nodeTypeState) throws SAXException {
        AttributesImpl attributes = new AttributesImpl();
        addAttribute(attributes, "name", nodeTypeState.getName());
        QName primaryItemName = nodeTypeState.getPrimaryItemName();
        if (primaryItemName != null) {
            addAttribute(attributes, "primaryItemName", primaryItemName);
        } else {
            addAttribute(attributes, "primaryItemName", "");
        }
        addAttribute(attributes, "isMixin", nodeTypeState.isMixin());
        addAttribute(attributes, "hasOrderableChildNodes", nodeTypeState.hasOrderableChildNodes());
        startElement("nodeType", attributes);

        QName[] supertypeNames = nodeTypeState.getSupertypeNames();
        if (supertypeNames != null && supertypeNames.length > 0) {
            startElement("supertypes");
            for (int i = 0; i < supertypeNames.length; i++) {
                startElement("supertype");
                char[] characters = filterXML(toJCRName(supertypeNames[i]));
                handler.characters(characters, 0, characters.length);
                endElement("supertype");
            }
            endElement("supertypes");
        }

        PropertyDefinitionState[] propertyDefinitionStates =
            nodeTypeState.getPropertyDefinitionStates();
        for (int i = 0; i < propertyDefinitionStates.length; i++) {
            write(propertyDefinitionStates[i]);
        }

        NodeDefinitionState[] childNodeDefinitionStates =
            nodeTypeState.getChildNodeDefinitionStates();
        for (int i = 0; i < childNodeDefinitionStates.length; i++) {
            write(childNodeDefinitionStates[i]);
        }

        endElement("nodeType");
    }

    private void write(PropertyDefinitionState propertyDefinitionState)
            throws SAXException {
        AttributesImpl attributes = new AttributesImpl();
        if (propertyDefinitionState.getName() != null) {
            addAttribute(attributes, "name", propertyDefinitionState.getName());
        } else {
            addAttribute(attributes, "name", "*");
        }
        addAttribute(
                attributes, "requiredType",
                PropertyType.nameFromValue(propertyDefinitionState.getRequiredType()));
        addAttribute(attributes, "autoCreated", propertyDefinitionState.isAutoCreated());
        addAttribute(attributes, "mandatory", propertyDefinitionState.isMandatory());
        addAttribute(
                attributes, "onParentVersion",
                OnParentVersionAction.nameFromValue(propertyDefinitionState.getOnParentVersion()));
        addAttribute(attributes, "protected", propertyDefinitionState.isProtected());
        addAttribute(attributes, "multiple", propertyDefinitionState.isMultiple());
        startElement("propertyDefinition", attributes);
        // TODO: default values
        // TODO: value constraints
        endElement("propertyDefinition");
    }

    private void write(NodeDefinitionState childNodeDefinitionState)
            throws SAXException {
        AttributesImpl attributes = new AttributesImpl();
        if (childNodeDefinitionState.getName() != null) {
            addAttribute(attributes, "name", childNodeDefinitionState.getName());
        } else {
            addAttribute(attributes, "name", "*");
        }
        if (childNodeDefinitionState.getDefaultPrimaryTypeName() != null) {
            addAttribute(attributes, "defaultPrimaryType", childNodeDefinitionState.getDefaultPrimaryTypeName());
        } else {
            addAttribute(attributes, "defaultPrimaryType", "");
        }
        addAttribute(attributes, "autoCreated", childNodeDefinitionState.isAutoCreated());
        addAttribute(attributes, "mandatory", childNodeDefinitionState.isMandatory());
        addAttribute(
                attributes, "onParentVersion",
                OnParentVersionAction.nameFromValue(childNodeDefinitionState.getOnParentVersion()));
        addAttribute(attributes, "protected", childNodeDefinitionState.isProtected());
        addAttribute(attributes, "sameNameSiblings", childNodeDefinitionState.allowsSameNameSiblings());
        startElement("childNodeDefinition", attributes);

        QName[] requiredPrimaryTypeNames = childNodeDefinitionState.getRequiredPrimaryTypeNames();
        if (requiredPrimaryTypeNames != null && requiredPrimaryTypeNames.length > 0) {
            startElement("requiredPrimaryTypes");
            for (int i = 0; i < requiredPrimaryTypeNames.length; i++) {
                startElement("requiredPrimaryType");
                char[] characters = filterXML(toJCRName(requiredPrimaryTypeNames[i]));
                handler.characters(characters, 0, characters.length);
                endElement("requiredPrimaryType");
            }
            endElement("requiredPrimaryTypes");
        }

        endElement("childNodeDefinition");
    }

    private void addAttribute(AttributesImpl attributes, String name, boolean value) {
        addAttribute(attributes, name, Boolean.toString(value));
    }

    private void addAttribute(AttributesImpl attributes, String name, QName value) {
        addAttribute(attributes, name, toJCRName(value));
    }

    private void addAttribute(AttributesImpl attributes, String name, String value) {
        attributes.addAttribute(
                "", name, name, "CDATA", new String(filterXML(value)));
    }

    private String toJCRName(QName name) {
        String prefix = (String) namespaces.get(name.getNamespaceURI());
        return prefix + ":" + name.getLocalName();
    }

    private char[] filterXML(String value) {
        char[] characters = value.toCharArray();
        for (int i = 0; i < characters.length; i++) {
            if (XMLChar.isInvalid(characters[i])) {
                characters[i] = ' '; // TODO: better escape?
            }
        }
        return characters;
    }

}
