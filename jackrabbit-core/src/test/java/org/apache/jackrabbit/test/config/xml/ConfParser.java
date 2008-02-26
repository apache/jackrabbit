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
package org.apache.jackrabbit.test.config.xml;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.apache.jackrabbit.test.config.BeanConf;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.IOException;
import java.util.Properties;

/**
 * Configuration parser base class. This class provides the basic
 * functionality for parsing Jackrabbit configuration files. Subclasses
 * extend this functionality with knowledge of the exact structure of the
 * different configuration files. Each configuration parser instance
 * contains a set of parser variables that are used for variable replacement
 * in the configuration file.
 */
public class ConfParser {

    /** Name of the bean parameter configuration element. */
    public static final String PARAM_ELEMENT = "param";

    /** Name of the bean implementation class configuration attribute. */
    public static final String CLASS_ATTRIBUTE = "class";

    /** Name of the bean parameter name configuration attribute. */
    public static final String NAME_ATTRIBUTE = "name";

    /** Name of the bean parameter value configuration attribute. */
    public static final String VALUE_ATTRIBUTE = "value";

    /**
     * Parses a named bean configuration from the given element.
     * Bean configuration uses the following format:
     * <pre>
     *   &lt;BeanName class="..."&gt;
     *     &lt;param name="..." value="..."/&gt;
     *     ...
     *   &lt;/BeanName&gt;
     * </pre>
     * <p>
     * The returned bean configuration object contains the configured
     * class name and configuration parameters. Variable replacement
     * is performed on the parameter values.
     *
     * @param parent parent element
     * @param name name of the bean configuration element
     * @return bean configuration,
     * @throws ConfException if the configuration element does not
     *                                exist or is broken
     */
    protected BeanConf parseBeanConf(Element parent, String name)
            throws ConfException {
        // Bean configuration element
        Element element = getElement(parent, name);

        // Bean implementation class
        String className = getAttribute(element, CLASS_ATTRIBUTE);

        // Bean properties
        Properties properties = parseParameters(element);

        return new BeanConf(className, properties);
    }

    /**
     * Parses the configuration parameters of the given element.
     * Parameters are stored as
     * <code>&lt;param name="..." value="..."/&gt;</code>
     * child elements. This method parses all param elements without
     * modifying parameter values, and returns the resulting name-value pairs.
     *
     * @param element configuration element
     * @return configuration parameters
     * @throws ConfException if a <code>param</code> element does
     *                                not contain the <code>name</code> and
     *                                <code>value</code> attributes
     */
    protected Properties parseParameters(Element element)
            throws ConfException {
        Properties parameters = new Properties();

        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE
                    && PARAM_ELEMENT.equals(child.getNodeName())) {
                Element parameter = (Element) child;
                Attr name = parameter.getAttributeNode(NAME_ATTRIBUTE);
                if (name == null) {
                    throw new ConfException("Parameter name not set");
                }
                Attr value = parameter.getAttributeNode(VALUE_ATTRIBUTE);
                if (value == null) {
                    throw new ConfException("Parameter value not set");
                }
                parameters.put(name.getValue(), value.getValue());
            }
        }

        return parameters;
    }

    /**
     * Parses the given XML document and returns the DOM root element.
     * A custom entity resolver is used to make the included configuration
     * file DTD available using the specified public identifiers.
     *
     * @see ConfEntityResolver
     * @param xml xml document
     * @return root element
     * @throws ConfException if the configuration document could
     *                                not be read or parsed
     */
    protected Element parseXML(InputSource xml) throws ConfException {
        try {
            DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setEntityResolver(ConfEntityResolver.INSTANCE);
            Document document = builder.parse(xml);
            return document.getDocumentElement();
        } catch (ParserConfigurationException e) {
            throw new ConfException(
                    "Unable to create configuration XML parser", e);
        } catch (SAXException e) {
            throw new ConfException(
                    "Configuration file syntax error.", e);
        } catch (IOException e) {
            throw new ConfException(
                    "Configuration file could not be read.", e);
        }
    }

    /**
     * Returns the named child of the given parent element.
     *
     * @param parent parent element
     * @param name name of the child element
     * @return named child element
     * @throws ConfException
     * @throws ConfException if the child element is not found
     */
    protected Element getElement(Element parent, String name)
            throws ConfException {
        return getElement(parent, name, true);
    }

    /**
     * Returns the named child of the given parent element.
     *
     * @param parent parent element
     * @param name name of the child element
     * @param required indicates if the child element is required
     * @return named child element, or <code>null</code> if not found and
     *         <code>required</code> is <code>false</code>.
     * @throws ConfException if the child element is not found and
     *         <code>required</code> is <code>true</code>.
     */
    protected Element getElement(Element parent, String name, boolean required)
            throws ConfException {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE
                    && name.equals(child.getNodeName())) {
                return (Element) child;
            }
        }
        if (required) {
            throw new ConfException(
                    "Configuration element " + name + " not found in "
                    + parent.getNodeName() + ".");
        } else {
            return null;
        }
    }

    /**
     * Returns the value of the named attribute of the given element.
     *
     * @param element element
     * @param name attribute name
     * @return attribute value
     * @throws ConfException if the attribute is not found
     */
    protected String getAttribute(Element element, String name)
            throws ConfException {
        Attr attribute = element.getAttributeNode(name);
        if (attribute != null) {
            return attribute.getValue();
        } else {
            throw new ConfException(
                    "Configuration attribute " + name + " not found in "
                    + element.getNodeName() + ".");
        }
    }

    /**
     * Returns the value of the named attribute of the given element.
     * If the attribute is not found, then the given default value is returned.
     *
     * @param element element
     * @param name attribute name
     * @param def default value
     * @return attribute value, or the default value
     */
    protected String getAttribute(Element element, String name, String def) {
        Attr attribute = element.getAttributeNode(name);
        if (attribute != null) {
            return attribute.getValue();
        } else {
            return def;
        }
    }

}
