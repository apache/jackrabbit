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

import java.util.Iterator;

import org.apache.jackrabbit.core.Constants;
import org.apache.jackrabbit.core.IllegalNameException;
import org.apache.jackrabbit.core.NamespaceResolver;
import org.apache.jackrabbit.core.NoPrefixDeclaredException;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.UnknownPrefixException;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.jdom.Element;

/**
 * Common functionality shared by the format classes.
 */
class CommonFormat {

    /** Name of the <code>name</code> attribute. */
    private static final String NAME_ATTRIBUTE = "name";

    /** The wildcard name */
    private static final String WILDCARD = "*";

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
     * Returns the formatted XML element.
     *
     * @return XML element
     */
    public Element getElement() {
        return element;
    }

    /**
     * Converts the given JCR name to a qualified name using the associated
     * namespace resolver.
     *
     * @param name JCR name
     * @return qualified name
     * @throws InvalidNodeTypeDefException if the name is invalid
     */
    protected QName fromJCRName(String name)
            throws InvalidNodeTypeDefException {
        try {
            if (WILDCARD.equals(name)) {
                return new QName(Constants.NS_DEFAULT_URI, WILDCARD);
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
        String value = element.getAttributeValue(name);
        if (value != null) {
            return value;
        } else {
            throw new InvalidNodeTypeDefException(
                    "Missing attribute " + name
                    + " in element " + element.getName());
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
     * Returns the named child element of the XML element.
     *
     * @param name child element name
     * @return child element
     */
    protected Element getChild(String name) {
        return element.getChild(name);
    }

    /**
     * Returns an iterator of all the named child elements of the XML element.
     *
     * @param name child element name
     * @return child element iterator
     */
    protected Iterator getChildIterator(String name) {
        return element.getChildren(name).iterator();
    }

    /**
     * Adds the given child element to the XML element.
     *
     * @param child child element
     */
    protected void addChild(Element child) {
        element.addContent(child);
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
