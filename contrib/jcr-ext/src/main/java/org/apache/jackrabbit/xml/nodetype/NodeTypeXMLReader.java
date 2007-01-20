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

import java.util.LinkedList;
import java.util.List;

import javax.jcr.PropertyType;
import javax.jcr.version.OnParentVersionAction;

import org.apache.jackrabbit.name.IllegalNameException;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.UnknownPrefixException;
import org.apache.jackrabbit.state.nodetype.NodeDefinitionState;
import org.apache.jackrabbit.state.nodetype.NodeTypeManagerState;
import org.apache.jackrabbit.state.nodetype.NodeTypeState;
import org.apache.jackrabbit.state.nodetype.PropertyDefinitionState;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * TODO
 */
class NodeTypeXMLReader extends DefaultHandler {

    private StringBuffer buffer;

    private SAXNamespaceResolver resolver;

    private NodeTypeManagerState nodeTypeManagerState;

    private List nodeTypeStates;

    private NodeTypeState nodeTypeState;

    private List supertypeNames;

    private List propertyDefinitionStates;

    private PropertyDefinitionState propertyDefinitionState;

    private List defaultValues;

    private List valueConstraints;

    private List childNodeDefinitionStates;

    private NodeDefinitionState childNodeDefinitionState;

    private List requiredPrimaryTypeNames;

    public NodeTypeManagerState getNodeTypeManagerState() {
        return nodeTypeManagerState;
    }

    public void startDocument() {
        buffer = new StringBuffer();
        resolver = new SAXNamespaceResolver();
        resolver.startDocument();
        nodeTypeManagerState = new NodeTypeManagerState();
    }

    public void endDocument() {
        resolver.endDocument();
    }

    public void startPrefixMapping(String prefix, String uri) {
        resolver.startPrefixMapping(prefix, uri);
    }

    public void endPrefixMapping(String prefix) {
        resolver.endPrefixMapping(prefix);
    }

    /** {@inheritDoc} */
    public void startElement(
            String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
        buffer.setLength(0);

        if (uri == null || uri.length() == 0) {
            System.out.println("<" + localName + ">");
            if (localName.equals("nodeTypes")) {
                nodeTypeStates = new LinkedList();
            } else if (localName.equals("nodeType")) {
                nodeTypeState = getNodeTypeState(attributes);
                supertypeNames = new LinkedList();
                propertyDefinitionStates = new LinkedList();
                childNodeDefinitionStates = new LinkedList();
            } else if (localName.equals("supertypes")) {
            } else if (localName.equals("supertype")) {
            } else if (localName.equals("propertyDefinition")) {
                propertyDefinitionState = getPropertyDefinitionState(attributes);
            } else if (localName.equals("valueConstraints")) {
            } else if (localName.equals("valueConstraint")) {
            } else if (localName.equals("defaultValues")) {
            } else if (localName.equals("defaultValue")) {
            } else if (localName.equals("childNodeDefinition")) {
                childNodeDefinitionState = getNodeDefinitionState(attributes);
            } else if (localName.equals("requiredPrimaryTypes")) {
                requiredPrimaryTypeNames = new LinkedList();
            } else if (localName.equals("requiredPrimaryType")) {
            } else {
                throw new SAXException("Unknown element: " + localName);
            }
        }

        resolver.startElement();
    }

    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        resolver.endElement();

        if (uri == null || uri.length() == 0) {
            System.out.println("</" + localName + ">");
            if (localName.equals("nodeTypes")) {
                nodeTypeManagerState.setNodeTypeStates((NodeTypeState[])
                        nodeTypeStates.toArray(new NodeTypeState[nodeTypeStates.size()]));
            } else if (localName.equals("nodeType")) {
                nodeTypeState.setSupertypeNames((QName[])
                        supertypeNames.toArray(new QName[supertypeNames.size()]));
                nodeTypeState.setPropertyDefinitionStates((PropertyDefinitionState[])
                        propertyDefinitionStates.toArray(new PropertyDefinitionState[propertyDefinitionStates.size()]));
                nodeTypeState.setChildNodeDefinitionStates((NodeDefinitionState[])
                        childNodeDefinitionStates.toArray(new NodeDefinitionState[childNodeDefinitionStates.size()]));
                nodeTypeStates.add(nodeTypeState);
            } else if (localName.equals("supertypes")) {
            } else if (localName.equals("supertype")) {
                supertypeNames.add(getName());
            } else if (localName.equals("propertyDefinition")) {
                propertyDefinitionStates.add(propertyDefinitionState);
            } else if (localName.equals("valueConstraints")) {
            } else if (localName.equals("valueConstraint")) {
            } else if (localName.equals("defaultValues")) {
            } else if (localName.equals("defaultValue")) {
            } else if (localName.equals("childNodeDefinition")) {
                childNodeDefinitionStates.add(childNodeDefinitionState);
            } else if (localName.equals("requiredPrimaryTypes")) {
                childNodeDefinitionState.setRequiredPrimaryTypeName((QName[])
                        requiredPrimaryTypeNames.toArray(new QName[requiredPrimaryTypeNames.size()]));
            } else if (localName.equals("requiredPrimaryType")) {
                requiredPrimaryTypeNames.add(getName());
            } else {
                throw new SAXException("Unknown element: " + localName);
            }
        }

        buffer.setLength(0);
    }

    public void characters(char[] ch, int start, int length) {
        buffer.append(ch, start, length);
    }

    public NodeTypeState getNodeTypeState(Attributes attributes) throws SAXException {
        NodeTypeState state = new NodeTypeState();
        state.setName(getName(attributes, "name"));
        if (getValue(attributes, "primaryItemName").length() > 0) {
            state.setPrimaryItemName(getName(attributes, "primaryItemName"));
        }
        state.setMixin(getBoolean(attributes, "isMixin"));
        state.setHasOrderableChildNodes(getBoolean(attributes, "hasOrderableChildNodes"));
        return state;
    }
    
    public NodeDefinitionState getNodeDefinitionState(Attributes attributes) throws SAXException {
        NodeDefinitionState state = new NodeDefinitionState();
        if (!getValue(attributes, "name").equals("*")) {
            state.setName(getName(attributes, "name"));
        }
        if (getValue(attributes, "defaultPrimaryType").length() > 0) {
            state.setDefaultPrimaryTypeName(getName(attributes, "defaultPrimaryType"));
        }
        state.setAutoCreated(getBoolean(attributes, "autoCreated"));
        state.setMandatory(getBoolean(attributes, "mandatory"));
        state.setOnParentVersion(OnParentVersionAction.valueFromName(getValue(attributes, "onParentVersion")));
        state.setProtected(getBoolean(attributes, "protected"));
        state.setAllowsSameNameSiblings(getBoolean(attributes, "sameNameSiblings"));
        return state;
    }

    private PropertyDefinitionState getPropertyDefinitionState(Attributes attributes) throws SAXException {
        PropertyDefinitionState state = new PropertyDefinitionState();
        if (!getValue(attributes, "name").equals("*")) {
            state.setName(getName(attributes, "name"));
        }
        state.setRequiredType(PropertyType.valueFromName(getValue(attributes, "requiredType")));
        state.setAutoCreated(getBoolean(attributes, "autoCreated"));
        state.setMandatory(getBoolean(attributes, "mandatory"));
        state.setOnParentVersion(OnParentVersionAction.valueFromName(getValue(attributes, "onParentVersion")));
        state.setProtected(getBoolean(attributes, "protected"));
        state.setMultiple(getBoolean(attributes, "multiple"));
        return state;
    }

    private QName getName() throws SAXException {
        String value = buffer.toString();
        try {
            return QName.fromJCRName(value, resolver);
        } catch (UnknownPrefixException e) {
            throw new SAXException("Unknown prefix: " + value, e);
        } catch (IllegalNameException e) {
            throw new SAXException("Illegal name: " + value, e);
        }
    }

    private String getValue(Attributes attributes, String name) throws SAXException {
        String value = attributes.getValue("", name);
        if (value != null) {
            return value;
        } else {
            throw new SAXException("Required attribute " + name + " not found");
        }
    }

    private QName getName(Attributes attributes, String name) throws SAXException {
        String value = getValue(attributes, name);
        try {
            return QName.fromJCRName(value, resolver);
        } catch (UnknownPrefixException e) {
            throw new SAXException("Unknown prefix: " + value, e);
        } catch (IllegalNameException e) {
            throw new SAXException("Illegal name: " + value, e);
        }
    }

    private boolean getBoolean(Attributes attributes, String name)
            throws SAXException {
        String value = getValue(attributes, name);
        return Boolean.valueOf(value).booleanValue();
    }

}
