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
package org.apache.jackrabbit.ntdoc.parser;

import java.io.*;
import java.util.*;
import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.apache.jackrabbit.ntdoc.model.*;

/**
 * This class implements the XML parser.
 */
public final class XMLNodeTypeParser
        extends NodeTypeParser {
    /**
     * Static factory for creating stream to DOM transformers.
     */
    private final static DocumentBuilderFactory BUILDER_FACTORY =
            DocumentBuilderFactory.newInstance();

    /**
     * Parse the file.
     */
    public void parse()
            throws IOException {
        Document doc = parseDocument();
        Element root = doc.getDocumentElement();

        if (root.getNodeName().equals("nodeTypes")) {
            parseNamespaces(root);
            parseNodeTypes(root);
        }
    }

    /**
     * Parse the xml file.
     */
    private Document parseDocument()
            throws IOException {
        try {
            DocumentBuilder builder = BUILDER_FACTORY.newDocumentBuilder();
            return builder.parse(getInputStream(), getSystemId());
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Parse all namespaces.
     */
    private void parseNamespaces(Element root)
            throws IOException {
        NamedNodeMap attributes = root.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attr = (Attr) attributes.item(i);
            if (attr.getName().startsWith("xmlns:")) {
                addNamespace(attr.getName().substring(6), attr.getValue());
            }
        }
    }

    /**
     * Parse all node types.
     */
    private void parseNodeTypes(Element root)
            throws IOException {
        NodeList list = root.getElementsByTagName("nodeType");
        for (int i = 0; i < list.getLength(); i++) {
            parseNodeType((Element) list.item(i));
        }
    }

    /**
     * Parse single node type.
     */
    private void parseNodeType(Element root)
            throws IOException {
        NodeType nt = addNodeType(root.getAttribute("name"));
        nt.setMixin(Boolean.valueOf(root.getAttribute("isMixin")).booleanValue());
        nt.setOrderable(Boolean.valueOf(root.getAttribute("hasOrderableChildNodes")).booleanValue());
        String primaryItemName = root.getAttribute("primaryItemName");
        nt.setSuperTypes(parseValueList(root, "supertypes", "supertype"));

        NodeList list = root.getElementsByTagName("propertyDefinition");
        for (int i = 0; i < list.getLength(); i++) {
            PropertyDef def = parsePropertyDef((Element) list.item(i));
            nt.addItemDef(def);

            if (def.getName().equals(primaryItemName)) {
                def.setPrimary(true);
            }
        }

        list = root.getElementsByTagName("childNodeDefinition");
        for (int i = 0; i < list.getLength(); i++) {
            NodeDef def = parseChildNodeDef((Element) list.item(i));
            nt.addItemDef(def);

            if (def.getName().equals(primaryItemName)) {
                def.setPrimary(true);
            }
        }
    }

    /**
     * Parse property definition.
     */
    private PropertyDef parsePropertyDef(Element root)
            throws IOException {
        PropertyDef def = new PropertyDef(root.getAttribute("name"));
        def.setAutoCreated(Boolean.valueOf(root.getAttribute("autoCreated")).booleanValue());
        def.setMandatory(Boolean.valueOf(root.getAttribute("mandatory")).booleanValue());
        def.setProtected(Boolean.valueOf(root.getAttribute("protected")).booleanValue());
        def.setMultiple(Boolean.valueOf(root.getAttribute("multiple")).booleanValue());
        def.setOnParentVersion(parseOnParentVersion(root));
        def.setRequiredType(parseRequiredType(root));
        def.setConstraints(parseValueList(root, "valueConstraints", "valueConstraints"));
        def.setDefaultValues(parseValueList(root, "defaultValues", "defaultValue"));
        return def;
    }

    /**
     * Parse property definition.
     */
    private NodeDef parseChildNodeDef(Element root)
            throws IOException {
        NodeDef def = new NodeDef(root.getAttribute("name"));
        def.setAutoCreated(Boolean.valueOf(root.getAttribute("autoCreated")).booleanValue());
        def.setMandatory(Boolean.valueOf(root.getAttribute("mandatory")).booleanValue());
        def.setProtected(Boolean.valueOf(root.getAttribute("protected")).booleanValue());
        def.setMultiple(Boolean.valueOf(root.getAttribute("sameNameSiblings")).booleanValue());
        def.setOnParentVersion(parseOnParentVersion(root));
        def.setDefaultPrimaryType(parseElementValue(root, "defaultPrimaryType"));
        def.setRequiredPrimaryTypes(parseValueList(root, "requiredPrimaryTypes", "requiredPrimaryType"));
        return def;
    }

    /**
     * Parse on parent version.
     */
    private int parseOnParentVersion(Element root)
            throws IOException {
        String opv = root.getAttribute("onParentVersion");
        if ("version".equalsIgnoreCase(opv)) {
            return ItemDef.OPV_VERSION;
        } else if ("initialize".equalsIgnoreCase(opv)) {
            return ItemDef.OPV_INITIALIZE;
        } else if ("compute".equalsIgnoreCase(opv)) {
            return ItemDef.OPV_COMPUTE;
        } else if ("ignore".equalsIgnoreCase(opv)) {
            return ItemDef.OPV_IGNORE;
        } else if ("abort".equalsIgnoreCase(opv)) {
            return ItemDef.OPV_ABORT;
        } else {
            return ItemDef.OPV_COPY;
        }
    }

    /**
     * Parse required type.
     */
    private int parseRequiredType(Element root)
            throws IOException {
        String type = root.getAttribute("requiredType");
        if ("binary".equalsIgnoreCase(type)) {
            return PropertyDef.TYPE_BINARY;
        } else if ("long".equalsIgnoreCase(type)) {
            return PropertyDef.TYPE_LONG;
        } else if ("double".equalsIgnoreCase(type)) {
            return PropertyDef.TYPE_DOUBLE;
        } else if ("boolean".equalsIgnoreCase(type)) {
            return PropertyDef.TYPE_BOOLEAN;
        } else if ("date".equalsIgnoreCase(type)) {
            return PropertyDef.TYPE_DATE;
        } else if ("name".equalsIgnoreCase(type)) {
            return PropertyDef.TYPE_NAME;
        } else if ("path".equalsIgnoreCase(type)) {
            return PropertyDef.TYPE_PATH;
        } else if ("reference".equalsIgnoreCase(type)) {
            return PropertyDef.TYPE_REFERENCE;
        } else if ("undefined".equalsIgnoreCase(type) || "*".equals(type)) {
            return PropertyDef.TYPE_UNDEFINED;
        } else {
            return PropertyDef.TYPE_STRING;
        }
    }

    /**
     * Return a value list.
     */
    private List parseValueList(Element root, String listName, String elemName)
            throws IOException {
        ArrayList list = new ArrayList();
        NodeList nodes = root.getElementsByTagName(listName);
        Element listElem = nodes.getLength() > 0 ? (Element) nodes.item(0) : null;

        if (listElem != null) {
            nodes = listElem.getElementsByTagName("elemName");
            for (int i = 0; i < nodes.getLength(); i++) {
                list.add(nodes.item(i).getTextContent());
            }
        }

        return list;
    }

    /**
     * Return value for an element.
     */
    private String parseElementValue(Element root, String elemName)
            throws IOException {
        NodeList nodes = root.getElementsByTagName(elemName);
        Element listElem = nodes.getLength() > 0 ? (Element) nodes.item(0) : null;
        return listElem != null ? listElem.getTextContent() : null;
    }
}
