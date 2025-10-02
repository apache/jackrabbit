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
package org.apache.jackrabbit.core.config;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.apache.jackrabbit.util.Text;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Configuration parser base class. This class provides the basic
 * functionality for parsing Jackrabbit configuration files. Subclasses
 * extend this functionality with knowledge of the exact structure of the
 * different configuration files. Each configuration parser instance
 * contains a set of parser variables that are used for variable replacement
 * in the configuration file.
 */
public class ConfigurationParser {

    /** Name of the bean parameter configuration element. */
    public static final String PARAM_ELEMENT = "param";

    /** Name of the bean implementation class configuration attribute. */
    public static final String CLASS_ATTRIBUTE = "class";

    /** Name of the bean parameter name configuration attribute. */
    public static final String NAME_ATTRIBUTE = "name";

    /** Name of the bean parameter value configuration attribute. */
    public static final String VALUE_ATTRIBUTE = "value";

    /**
     * The configuration parser variables. These name-value pairs
     * are used to substitute <code>${...}</code> variable references
     * with context-dependent values in the configuration.
     *
     * @see #replaceVariables(String)
     */
    private final Properties variables;

    /**
     * Creates a new configuration parser with the given parser variables.
     *
     * @param variables parser variables
     */
    public ConfigurationParser(Properties variables) {
        this.variables = variables;
    }

    /**
     * Returns the variables.
     * @return the variables.
     */
    public Properties getVariables() {
        return variables;
    }

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
     * @throws ConfigurationException if the configuration element does not
     *                                exist or is broken
     */
    protected BeanConfig parseBeanConfig(Element parent, String name)
            throws ConfigurationException {
        // Bean configuration element
        Element element = getElement(parent, name);

        return parseBeanConfig(element);
    }

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
     * @param element
     * @return bean configuration,
     * @throws ConfigurationException if the configuration element does not
     *                                exist or is broken
     */
    protected BeanConfig parseBeanConfig(Element element)
            throws ConfigurationException {
        // Bean implementation class
        String className = getAttribute(element, CLASS_ATTRIBUTE);

        // Bean properties
        Properties properties = parseParameters(element);

        return new BeanConfig(className, properties);
    }

    /**
     * Parses the configuration parameters of the given element.
     * Parameters are stored as
     * <code>&lt;param name="..." value="..."/&gt;</code>
     * child elements. This method parses all param elements,
     * performs {@link #replaceVariables(String) variable replacement}
     * on parameter values, and returns the resulting name-value pairs.
     *
     * @param element configuration element
     * @return configuration parameters
     * @throws ConfigurationException if a <code>param</code> element does
     *                                not contain the <code>name</code> and
     *                                <code>value</code> attributes
     */
    protected Properties parseParameters(Element element)
            throws ConfigurationException {
        Properties parameters = new Properties();

        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE
                    && PARAM_ELEMENT.equals(child.getNodeName())) {
                Element parameter = (Element) child;
                Attr name = parameter.getAttributeNode(NAME_ATTRIBUTE);
                if (name == null) {
                    throw new ConfigurationException("Parameter name not set");
                }
                Attr value = parameter.getAttributeNode(VALUE_ATTRIBUTE);
                if (value == null) {
                    throw new ConfigurationException("Parameter value not set");
                }
                parameters.put(
                        name.getValue().trim(),
                        replaceVariables(value.getValue()));
            }
        }

        return parameters;
    }

    /**
     * Performs variable replacement on the given string value.
     * Each <code>${...}</code> sequence within the given value is replaced
     * with the value of the named parser variable. The replacement is not
     * done if the named variable does not exist.
     *
     * @param value original value
     * @return value after variable replacements
     * @throws ConfigurationException if the replacement of a referenced
     *                                variable is not found
     */
    protected String replaceVariables(String value)
            throws ConfigurationException {
        try {
            return Text.replaceVariables(variables, value, false);
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException(e.getMessage(), e);
        }
    }

    /**
     * Parses the given XML document and returns the DOM root element.
     * A custom entity resolver is used to make the included configuration
     * file DTD available using the specified public identifiers.
     * This implementation does not validate the XML.
     *
     * @see ConfigurationEntityResolver
     * @param xml xml document
     * @return root element
     * @throws ConfigurationException if the configuration document could
     *                                not be read or parsed
     */
    protected Element parseXML(InputSource xml) throws ConfigurationException {
        return parseXML(xml, false);
    }

    /**
     * Returns the error handler to be used when parsing configuration
     * documents. Subclasses can override this method to provide custom
     * error handling.
     *
     * @since Apache Jackrabbit 2.0
     * @return error handler
     */
    protected ErrorHandler getErrorHandler() {
        return new ConfigurationErrorHandler();
    }

    /**
     * Returns the entity resolver to be used when parsing configuration
     * documents. Subclasses can override this method to provide custom
     * entity resolution rules.
     *
     * @since Apache Jackrabbit 2.0
     * @return error handler
     */
    protected EntityResolver getEntityResolver() {
        return ConfigurationEntityResolver.INSTANCE;
    }

    /**
     * A post-processing hook for the parsed repository or workspace
     * configuration documents. This hook makes it possible to make custom
     * DOM modifications for backwards-compatibility or other reasons.
     *
     * @since Apache Jackrabbit 2.0
     * @param document the parsed configuration document
     * @return the configuration document after any modifications
     */
    protected Document postParseModificationHook(Document document) {
        return document;
    }

    /**
     * Parses the given XML document and returns the DOM root element.
     * A custom entity resolver is used to make the included configuration
     * file DTD available using the specified public identifiers.
     *
     * @see ConfigurationEntityResolver
     * @param xml xml document
     * @param validate whether the XML should be validated
     * @return root element
     * @throws ConfigurationException if the configuration document could
     *                                not be read or parsed
     */
    protected Element parseXML(InputSource xml, boolean validate) throws ConfigurationException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(validate);
            DocumentBuilder builder = factory.newDocumentBuilder();
            if (validate) {
                builder.setErrorHandler(getErrorHandler());
            }
            builder.setEntityResolver(getEntityResolver());
            Document document = builder.parse(xml);
            return postParseModificationHook(document).getDocumentElement();
        } catch (ParserConfigurationException e) {
            throw new ConfigurationException(
                    "Unable to create configuration XML parser", e);
        } catch (SAXParseException e) {
            throw new ConfigurationException(
                    "Configuration file syntax error. (Line: " + e.getLineNumber() + " Column: " + e.getColumnNumber() + ")", e);
        } catch (SAXException e) {
            throw new ConfigurationException(
                    "Configuration file syntax error. ", e);
        } catch (IOException e) {
            throw new ConfigurationException(
                    "Configuration file could not be read.", e);
        }
    }

    /**
     * Returns the named child of the given parent element.
     *
     * @param parent parent element
     * @param name name of the child element
     * @return named child element
     * @throws ConfigurationException
     * @throws ConfigurationException if the child element is not found
     */
    protected Element getElement(Element parent, String name)
            throws ConfigurationException {
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
     * @throws ConfigurationException if the child element is not found and
     *         <code>required</code> is <code>true</code>;
     *         or if more than one element with this name exists.
     */
    protected Element getElement(Element parent, String name, boolean required)
            throws ConfigurationException {
        NodeList children = parent.getChildNodes();
        Element found = null;
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE
                    && name.equals(child.getNodeName())) {
                if (found != null)  {
                    throw new ConfigurationException(
                        "Duplicate configuration element " + name + " in "
                        + parent.getNodeName() + ".");
                }
                found = (Element) child;
            }
        }
        if (required && found == null) {
            throw new ConfigurationException(
                    "Configuration element " + name + " not found in "
                    + parent.getNodeName() + ".");
        }
        return found;
    }

    /**
     * Returns the named child of the given parent element.
     *
     * @param parent parent element
     * @param name name of the child element
     * @param required indicates if the child element is required
     * @return named child element, or <code>null</code> if not found and
     *         <code>required</code> is <code>false</code>.
     * @throws ConfigurationException if the child element is not found and
     *         <code>required</code> is <code>true</code>;
     *         or if more than one element with this name exists.
     */
    protected Element[] getElements(Element parent, String name, boolean required)
            throws ConfigurationException {
        NodeList children = parent.getChildNodes();
        List<Element> found = new ArrayList<Element>();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE
                    && name.equals(child.getNodeName())) {
                found.add((Element) child);
            }
        }
        if (required && found.isEmpty()) {
            throw new ConfigurationException(
                    "Configuration element " + name + " not found in "
                    + parent.getNodeName() + ".");
        }
        return found.toArray(new Element[found.size()]);
    }

    /**
     * Returns the value of the named attribute of the given element.
     *
     * @param element element
     * @param name attribute name
     * @return attribute value
     * @throws ConfigurationException if the attribute is not found
     */
    protected String getAttribute(Element element, String name)
            throws ConfigurationException {
        Attr attribute = element.getAttributeNode(name);
        if (attribute != null) {
            return attribute.getValue();
        } else {
            throw new ConfigurationException(
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
