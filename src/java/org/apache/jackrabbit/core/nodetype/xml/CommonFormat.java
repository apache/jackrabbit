/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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
package org.apache.jackrabbit.core.nodetype.xml;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

import org.apache.jackrabbit.core.Constants;
import org.apache.jackrabbit.core.IllegalNameException;
import org.apache.jackrabbit.core.NamespaceResolver;
import org.apache.jackrabbit.core.NoPrefixDeclaredException;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.UnknownPrefixException;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * Common functionality shared by the format classes.
 */
class CommonFormat {

    /** Name of the <code>name</code> attribute. */
    private static final String NAME_ATTRIBUTE = "name";

    /** The wildcard name */
    private static final String WILDCARD = "*";

    /** The wildcard qualified name. */
    private static final QName WILDCARD_NAME =
        new QName(Constants.NS_DEFAULT_URI, WILDCARD);

    /** The namespace resolver. */
    private final NamespaceResolver resolver;

    /** The formatted XML element. */
    private final Element element;

    /**
     * Creates a common format object.
     *
     * @param resolver namespace resolver
     * @param element XML element
     */
    protected CommonFormat(NamespaceResolver resolver, Element element) {
        this.resolver = resolver;
        this.element = element;
    }

    /**
     * Returns the namespace resolver associated with this format instance.
     *
     * @return namespace resolver
     */
    protected NamespaceResolver getNamespaceResolver() {
        return resolver;
    }

    /**
     * Converts the given JCR name to a qualified name using the associated
     * namespace resolver. The wildcard name are converted to the special
     * wildcard qualified name.
     *
     * @param name JCR name
     * @return qualified name
     * @throws InvalidNodeTypeDefException if the name is invalid
     */
    protected QName fromJCRName(String name)
            throws InvalidNodeTypeDefException {
        try {
            if (WILDCARD.equals(name)) {
                return WILDCARD_NAME;
            } else {
                return QName.fromJCRName(name, resolver);
            }
        } catch (IllegalNameException e) {
            throw new InvalidNodeTypeDefException(
                    "Illegal JCR name " + name, e);
        } catch (UnknownPrefixException e) {
            throw new InvalidNodeTypeDefException(
                    "Unknown prefix in JCR name " + name, e);
        }
    }

    /**
     * Converts the given qualified name to a JCR name using the associated
     * namespace resolver.
     * <p>
     * This method throws an IllegalArgumentException if the given qualified
     * name cannot be represented as a JCR name. This problem should never
     * happen in normal circumstances, and callers may safely ignore the
     * exception.
     *
     * @param name qualified name
     * @return JCR name
     * @throws IllegalArgumentException if the name is invalid
     */
    protected String toJCRName(QName name) throws IllegalArgumentException {
        try {
            return name.toJCRName(resolver);
        } catch (NoPrefixDeclaredException e) {
            throw new IllegalArgumentException(
                    "No prefix declared for namespace "
                    + name.getNamespaceURI());
        }
    }

    /**
     * Returns the value of the named attribute of the XML element.
     *
     * @param name attribute name
     * @return attribute value
     * @throws InvalidNodeTypeDefException if the attribute does not exist
     */
    protected String getAttribute(String name) throws InvalidNodeTypeDefException {
        String value = element.getAttribute(name);
        if (value != null) {
            return value;
        } else {
            throw new InvalidNodeTypeDefException(
                    "Missing attribute " + name
                    + " in element " + element.getNodeName());
        }
    }

    /**
     * Sets the named attribute of the XML element.
     *
     * @param name attribute name
     * @param value attribute value
     */
    protected void setAttribute(String name, String value) {
        element.setAttribute(name, value);
    }

    /**
     * Returns an iterator of all the named child elements of the XML element.
     *
     * @param childName child element name
     * @return child element iterator
     */
    protected Iterator getChildElements(String childName) {
        Vector children = new Vector();

        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE
                    && childName.equals(node.getNodeName())) {
                children.add(node);
            }
        }

        return children.iterator();
    }

    /**
     * Returns the text contents of all the named grand child elements
     * of the XML element. Returns <code>null</code> if the named child
     * element does not exist.
     *
     * @param childName child element name
     * @param grandChildName grand child element name
     * @return grand child contents, or <code>null</code>
     */
    protected Collection getGrandChildContents(
            String childName, String grandChildName) {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++ ) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE
                    && childName.equals(child.getNodeName())) {
                Vector contents = new Vector();

                NodeList nodes = child.getChildNodes();
                for (int j = 0; j < nodes.getLength(); j++) {
                    Node node = nodes.item(j);
                    if (node.getNodeType() == Node.ELEMENT_NODE
                            && grandChildName.equals(node.getNodeName())) {
                        contents.add(getContents(node));
                    }
                }

                return contents;
            }
        }

        return null;
    }

    /**
     * Utility method to get the text contents of an XML element.
     *
     * @param element XML element
     * @return trimmed text contents of the element
     */
    private static String getContents(Node element) {
        StringBuffer text = new StringBuffer();

        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.TEXT_NODE) {
                text.append(((CharacterData) node).getData());
            }
        }

        return text.toString().trim();
    }

    /**
     * Adds the given collection of string values as a grand child
     * element structure. Each value in the collection is added as
     * a separate grand child element below the created child element.
     *
     * @param childName child element name
     * @param grandChildName grand child element name
     * @param contents string value collection
     */
    protected void setGrandChildContents(
            String childName, String grandChildName, Collection contents) {
        Element child = newElement(childName);

        Iterator iterator = contents.iterator();
        while (iterator.hasNext()) {
            String value = (String) iterator.next();
            child.appendChild(newElement(grandChildName, value));
        }

        addChild(child);
    }

    /**
     * Creates a new XML element.
     *
     * @param name element name
     * @return XML element
     */
    protected Element newElement(String name) {
        return element.getOwnerDocument().createElement(name);
    }

    /**
     * Creates a new XML element with the given text content.
     *
     * @param name element name
     * @param content element content
     * @return XML element
     */
    protected Element newElement(String name, String content) {
        Element element = newElement(name);
        Text text = element.getOwnerDocument().createTextNode(content);
        element.appendChild(text);
        return element;
    }

    /**
     * Adds the given child element to the XML element.
     *
     * @param child child element
     */
    protected void addChild(Element child) {
        element.appendChild(child);
    }

    /**
     * Returns the qualified name stored as the <code>name</code> attribute.
     *
     * @return qualified name
     * @throws InvalidNodeTypeDefException if the format of the XML
     *                                    element is invalid
     */
    protected QName getName() throws InvalidNodeTypeDefException {
        return fromJCRName(getAttribute(NAME_ATTRIBUTE));
    }

    /**
     * Sets the given name as the <code>name</code> attribute.
     *
     * @param name qualified name
     */
    protected void setName(QName name) {
        setAttribute(NAME_ATTRIBUTE, toJCRName(name));
    }

}
