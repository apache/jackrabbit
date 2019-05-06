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
package org.apache.jackrabbit.core.util;

import org.w3c.dom.Attr;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Document walker class. This class provides an intuitive
 * interface for traversing a parsed DOM document.
 */
public final class DOMWalker {

    /** Static factory for creating stream to DOM transformers. */
    private static final DocumentBuilderFactory factory =
        DocumentBuilderFactory.newInstance();

    /** The DOM document being traversed by this walker. */
    private final Document document;

    /** The current element. */
    private Element current;

    /**
     * Creates a walker for traversing a DOM document read from the given
     * input stream. The root element of the document is set as the current
     * element.
     *
     * @param xml XML input stream
     * @throws IOException if a document cannot be read from the stream
     */
    public DOMWalker(InputStream xml) throws IOException {
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(xml);
            current = document.getDocumentElement();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        }
    }

    /**
     * Returns the namespace mappings defined in the current element.
     * The returned property set contains the prefix to namespace
     * mappings specified by the <code>xmlns</code> attributes of the
     * current element.
     *
     * @return prefix to namespace mappings of the current element
     */
    public Properties getNamespaces() {
        Properties namespaces = new Properties();
        NamedNodeMap attributes = current.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attribute = (Attr) attributes.item(i);
            if (attribute.getName().startsWith("xmlns:")) {
                namespaces.setProperty(
                        attribute.getName().substring(6), attribute.getValue());
            }
        }
        return namespaces;
    }

    /**
     * Returns the name of the current element.
     *
     * @return element name
     */
    public String getName() {
        return current.getNodeName();
    }

    /**
     * Returns the value of the named attribute of the current element.
     *
     * @param name attribute name
     * @return attribute value, or <code>null</code> if not found
     */
    public String getAttribute(String name) {
        Attr attribute = current.getAttributeNode(name);
        if (attribute != null) {
            return attribute.getValue();
        } else {
            return null;
        }
    }

    /**
     * Returns the text content of the current element.
     *
     * @return text content
     */
    public String getContent() {
        StringBuilder content = new StringBuilder();

        NodeList nodes = current.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.TEXT_NODE) {
                content.append(((CharacterData) node).getData());
            }
        }

        return content.toString();
    }

    /**
     * Enters the named child element. If the named child element is
     * found, then it is made the current element and <code>true</code>
     * is returned. Otherwise the current element is not changed and
     * <code>false</code> is returned.
     * <p>
     * The standard call sequence for this method is show below.
     * <pre>
     *     DOMWalker walker = ...;
     *     if (walker.enterElement("...")) {
     *         ...;
     *         walker.leaveElement();
     *     }
     * </pre>
     *
     * @param name child element name
     * @return <code>true</code> if the element was entered,
     *         <code>false</code> otherwise
     */
    public boolean enterElement(String name) {
        NodeList children = current.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE
                    && name.equals(child.getNodeName())) {
                current = (Element) child;
                return true;
            }
        }
        return false;
    }

    /**
     * Leaves the current element. The parent element is set as the new
     * current element.
     *
     * @see #enterElement(String)
     */
    public void leaveElement() {
        current = (Element) current.getParentNode();
    }

    /**
     * Iterates through the named child elements over multiple calls.
     * This method makes it possible to use the following code to
     * walk through all the child elements with the given name.
     * <pre>
     *     DOMWalker walker = ...;
     *     while (walker.iterateElements("...")) {
     *         ...;
     *     }
     * </pre>
     * <p>
     * <strong>WARNING:</strong> This method should only be used when
     * <code>walker.getName()</code> does not equal <code>name</code> when
     * the while loop is started. Otherwise the walker will not be positioned
     * at the same node when the while loop ends.
     *
     * @param name name of the iterated elements
     * @return <code>true</code> if another iterated element was entered, or
     *         <code>false</code> if no more iterated elements were found
     *         and the original element is restored as the current element
     */
    public boolean iterateElements(String name) {
        Node next;
        if (name.equals(current.getNodeName())) {
            next = current.getNextSibling();
        } else {
            next = current.getFirstChild();
        }

        while (next != null) {
            if (next.getNodeType() == Node.ELEMENT_NODE
                    && name.equals(next.getNodeName())) {
                current = (Element) next;
                return true;
            } else {
                next = next.getNextSibling();
            }
        }

        if (name.equals(current.getNodeName())) {
            Node parent = current.getParentNode();
            if (parent instanceof Element) {
                current = (Element) parent;
            }
        }
        return false;
    }

}
