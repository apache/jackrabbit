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
package org.apache.jackrabbit.util;

import org.w3c.dom.Attr;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * <code>XMLUtil</code>...
 */
public class XMLUtil {

    /**
     * @param node
     * @return true if the given node is of type text or CDATA.
     */
    public static boolean isText(Node node) {
        int ntype = node.getNodeType();
        return ntype == Node.TEXT_NODE || ntype == Node.CDATA_SECTION_NODE;
    }

    /**
     * Concatenates the values of all child nodes of type 'Text' or 'CDATA'/
     *
     * @param element
     * @return String representing the value of all Text and CDATA child nodes or
     * <code>null</code> if the length of the resulting String is 0.
     * @see #isText(org.w3c.dom.Node)
     */
    public static String getText(Element element) {
        StringBuilder content = new StringBuilder();
        if (element != null) {
            NodeList nodes = element.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node child = nodes.item(i);
                if (isText(child)) {
                    // cast to super class that contains Text and CData
                    content.append(((CharacterData) child).getData());
                }
            }
        }
        return (content.length()==0) ? null : content.toString();
    }

    /**
     * Same as {@link #getText(Element)} except that 'defaultValue' is returned
     * instead of <code>null</code>, if the element does not contain any text.
     *
     * @param element
     * @param defaultValue
     * @return the text contained in the specified element or
     * <code>defaultValue</code> if the element does not contain any text.
     */
    public static String getText(Element element, String defaultValue) {
        String txt = getText(element);
        return (txt == null) ? defaultValue : txt;
    }

    /**
     * Calls {@link #getText(Element)} on the first child element that matches
     * the given local name and namespace.
     *
     * @param parent
     * @param childLocalName
     * @param childNamespaceURI
     * @return text contained in the first child that matches the given local name
     * and namespace or <code>null</code>.
     * @see #getText(Element)
     */
    public static String getChildText(Element parent, String childLocalName, String childNamespaceURI) {
        Element child = getChildElement(parent, childLocalName, childNamespaceURI);
        return (child == null) ? null : getText(child);
    }

    /**
     * Returns the first child element that matches the given local name and
     * namespace. If no child element is present or no child element matches,
     * <code>null</code> is returned.
     *
     * @param parent
     * @param childLocalName
     * @param childNamespaceURI
     * @return first child element matching the specified names or <code>null</code>.
     */
    public static Element getChildElement(Node parent, String childLocalName, String childNamespaceURI) {
        if (parent != null) {
            NodeList children = parent.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE && childLocalName.equals(child.getLocalName()) && childNamespaceURI.equals(child.getNamespaceURI())) {
                    return (Element)child;
                }
            }
        }
        return null;
    }

    /**
     * Returns the value of the named attribute of the current element.
     *
     * @param parent
     * @param localName attribute local name or 'nodeName' if no namespace is
     * specified.
     * @param  namespaceURI or <code>null</code>
     * @return attribute value, or <code>null</code> if not found
     */
    public static String getAttribute(Element parent, String localName, String namespaceURI) {
        if (parent == null) {
            return null;
        }
        Attr attribute;
        if (namespaceURI == null) {
            attribute = parent.getAttributeNode(localName);
        } else {
            attribute = parent.getAttributeNodeNS(namespaceURI, localName);
        }
        if (attribute != null) {
            return attribute.getValue();
        } else {
            return null;
        }
    }
}