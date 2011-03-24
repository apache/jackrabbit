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
package org.apache.jackrabbit.commons.privilege;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <code>PrivilegeXmlHandler</code> 
 */
class PrivilegeXmlHandler implements PrivilegeHandler {

    private static final String TEXT_XML = "text/xml";
    private static final String APPLICATION_XML = "application/xml";

    private static final String XML_PRIVILEGES = "privileges";
    private static final String XML_PRIVILEGE = "privilege";
    private static final String XML_CONTAINS = "contains";

    private static final String ATTR_NAME = "name";
    private static final String ATTR_ABSTRACT = "abstract";

    private static final String ATTR_XMLNS = "xmlns:";

    /**
     * Constant for <code>DocumentBuilderFactory</code>.
     */
    private static DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = createFactory();

    private static DocumentBuilderFactory createFactory() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setIgnoringComments(true);
        factory.setIgnoringElementContentWhitespace(true);
        return factory;
    }

    /**
     * Constant for <code>TransformerFactory</code>
     */
    private static TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();

    /**
     * Create a new instance.
     */
    PrivilegeXmlHandler() {

    }

    /**
     * Returns <code>true</code> if the specified content type can be handled by this class.
     *
     * @param contentType
     * @return <code>true</code> if the specified content type can be handled
     * by this class; <code>false</code> otherwise.
     */
    static boolean isSupportedContentType(String contentType) {
        return TEXT_XML.equals(contentType) || APPLICATION_XML.equals(contentType);
    }

    //---------------------------------------------------< PrivilegeHandler >---
    /**
     * @see PrivilegeHandler#readDefinitions(java.io.InputStream, java.util.Map)
     */
    public PrivilegeDefinition[] readDefinitions(InputStream in, Map<String, String> namespaces) throws ParseException {
        try {
            List<PrivilegeDefinition> defs = new ArrayList<PrivilegeDefinition>();

            DocumentBuilder builder = createDocumentBuilder();
            Document doc = builder.parse(in);
            Element root = doc.getDocumentElement();
            if (!XML_PRIVILEGES.equals(root.getNodeName())) {
                throw new IllegalArgumentException("root element must be named 'privileges'");
            }

            updateNamespaceMapping(root, namespaces);

            NodeList nl = root.getElementsByTagName(XML_PRIVILEGE);
            for (int i = 0; i < nl.getLength(); i++) {
                Node n = nl.item(i);
                PrivilegeDefinition def = parseDefinition(n, namespaces);
                if (def != null) {
                    defs.add(def);
                }
            }
            return defs.toArray(new PrivilegeDefinition[defs.size()]);

        } catch (SAXException e) {
            throw new ParseException(e);
        } catch (IOException e) {
            throw new ParseException(e);
        } catch (ParserConfigurationException e) {
            throw new ParseException(e);
        }

    }

    /**
     * @see PrivilegeHandler#writeDefinitions(java.io.OutputStream, PrivilegeDefinition[], java.util.Map) 
     */
    public void writeDefinitions(OutputStream out, PrivilegeDefinition[] definitions, Map<String, String> namespaces) throws IOException {
        try {
            DocumentBuilder builder = createDocumentBuilder();
            Document doc = builder.newDocument();
            Element privileges = (Element) doc.appendChild(doc.createElement(XML_PRIVILEGES));

            for (Map.Entry<String, String> entry : namespaces.entrySet()) {
                privileges.setAttribute(ATTR_XMLNS + entry.getKey(), entry.getValue());
            }

            for (PrivilegeDefinition def : definitions) {
                Element priv = (Element) privileges.appendChild(doc.createElement(XML_PRIVILEGE));
                priv.setAttribute(ATTR_NAME, def.getName());
                priv.setAttribute(ATTR_ABSTRACT, Boolean.valueOf(def.isAbstract()).toString());

                for (String aggrName : def.getDeclaredAggregateNames()) {
                    Element contains = (Element) priv.appendChild(doc.createElement(XML_CONTAINS));
                    contains.setAttribute(ATTR_NAME, aggrName);
                }
            }

            Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
            transformer.transform(new DOMSource(doc), new StreamResult(out));

        } catch (Exception e) {
            IOException io = new IOException(e.getMessage());
            io.initCause(e);
            throw io;
        }
    }

    //--------------------------------------------------------------------------
    /**
     * Build a new <code>PrivilegeDefinition</code> from the given XML node.
     * @param n
     * @param namespaces
     * @return
     */
    private PrivilegeDefinition parseDefinition(Node n, Map<String, String> namespaces) {
        if (n.getNodeType() == Node.ELEMENT_NODE) {
            Element elem = (Element) n;

            updateNamespaceMapping(elem, namespaces);

            String name = elem.getAttribute(ATTR_NAME);
            boolean isAbstract = Boolean.parseBoolean(elem.getAttribute(ATTR_ABSTRACT));

            List<String> aggrNames = new ArrayList<String>();
            NodeList nodeList = elem.getChildNodes();
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node contains = nodeList.item(i);
                if (isElement(n) && XML_CONTAINS.equals(contains.getNodeName())) {
                    String aggrName = ((Element) contains).getAttribute(ATTR_NAME);
                    if (aggrName != null) {
                        aggrNames.add(aggrName);
                    }
                }
            }
            return new PrivilegeDefinition(name, isAbstract, aggrNames.toArray(new String[aggrNames.size()]));
        }

        // could not parse into privilege definition
        return null;
    }

    /**
     * Create a new <code>DocumentBuilder</code>
     *
     * @return a new <code>DocumentBuilder</code>
     * @throws ParserConfigurationException
     */
    private static DocumentBuilder createDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilder builder = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
        builder.setErrorHandler(new DefaultHandler());
        return builder;
    }

    /**
     * Update the specified namespace mappings with the namespace declarations
     * defined by the given XML element.
     * 
     * @param elem
     * @param namespaces
     */
    private static void updateNamespaceMapping(Element elem, Map<String, String> namespaces) {
        NamedNodeMap attributes = elem.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attr = (Attr) attributes.item(i);
            if (attr.getName().startsWith(ATTR_XMLNS)) {
                String prefix = attr.getName().substring(ATTR_XMLNS.length());
                String uri = attr.getValue();
                namespaces.put(prefix, uri);
            }
        }
    }

    /**
     * Returns <code>true</code> if the given XML node is an element.
     *
     * @param n
     * @return <code>true</code> if the given XML node is an element; <code>false</code> otherwise.
     */
    private static boolean isElement(Node n) {
        return n.getNodeType() == Node.ELEMENT_NODE;
    }
}