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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.jackrabbit.core.Constants;
import org.apache.jackrabbit.core.NamespaceResolver;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Facade class for reading and writing node type definition files.
 * A NodeTypeFormatter instance uses a simple XML transformer instance
 * to convert XML between DOM and stream representations. The task
 * of actually interpreting the XML elements is delegated to the
 * package-local Format classes.
 * <p>
 * This class (and through it the entire .xml subpackage) is used only by the
 * {@link org.apache.jackrabbit.core.nodetype.NodeTypeDefStore NodeTypeDefStore}
 * class.
 */
public class NodeTypeFormatter {

    /** Path of the node type definition template file. */
    private static final String TEMPLATE =
        "org/apache/jackrabbit/core/nodetype/xml/template.xml";

    /** Name of the node type definition root element. */
    private static final String NODETYPES_ELEMENT = "nodeTypes";

    /** Name of the node type definition element. */
    private static final String NODETYPE_ELEMENT = "nodeType";

    /** The transformer used to serialize and deserialise DOM objects. */
    private Transformer transformer;

    /**
     * Creates a node type formatter object.
     *
     * @throws RepositoryException if the formatter initialization fails
     */
    public NodeTypeFormatter() throws RepositoryException {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        } catch (TransformerConfigurationException e) {
            throw new RepositoryException(
                    "Unable to create a node type formatter", e);
        }
    }

    /**
     * Reads a set of node type definitions from the given input stream.
     *
     * @param xml XML input stream
     * @return node type definitions
     * @throws IOException on IO errors
     * @throws InvalidNodeTypeDefException if the node type definition
     *                                     format is invalid
     * @throws RepositoryException on repository errors
     */
    public Collection read(InputStream xml)
            throws IOException, InvalidNodeTypeDefException,
            RepositoryException {
        Vector types = new Vector();

        Document document = readDocument(xml);
        Element root = document.getDocumentElement();
        NamespaceResolver resolver = new AdditionalNamespaceResolver(root);
        if (!NODETYPES_ELEMENT.equals(root.getNodeName())) {
            throw new InvalidNodeTypeDefException(
                    "Invalid node type definition root "
                    + root.getNodeName());
        }

        NodeList nodes = root.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE
                    && NODETYPE_ELEMENT.equals(node.getNodeName())) {
                NodeTypeDef def = new NodeTypeDef();
                NodeTypeFormat format =
                    new NodeTypeFormat(resolver, (Element) node, def);
                format.read();
                types.add(def);
            }
        }

        return types;
    }

    /**
     * Writes the given set of node type definitions to the given output
     * stream.
     *
     * @param xml XML output stream
     * @param registry namespace registry
     * @param types node types
     * @throws IOException on IO errors
     * @throws RepositoryException on repository errors
     */
    public void write(
            OutputStream xml, NamespaceRegistry registry, Collection types)
            throws IOException, RepositoryException {
        InputStream template =
            getClass().getClassLoader().getResourceAsStream(TEMPLATE);
        Document document =  readDocument(template);
        Element root = document.getDocumentElement();

        String[] prefixes = registry.getPrefixes();
        for (int i = 0; i < prefixes.length; i++) {
            if (!Constants.NS_EMPTY_PREFIX.equals(prefixes[i])) {
                String uri = registry.getURI(prefixes[i]);
                root.setAttribute(
                        Constants.NS_XMLNS_PREFIX + ":" + prefixes[i], uri);
            }
        }

        NamespaceResolver resolver =
            new AdditionalNamespaceResolver(registry);
        Iterator iterator = types.iterator();
        while (iterator.hasNext()) {
            Element element =
                root.getOwnerDocument().createElement(NODETYPE_ELEMENT);
            NodeTypeDef type = (NodeTypeDef) iterator.next();
            NodeTypeFormat format =
                new NodeTypeFormat(resolver, element, type);
            format.write();
            root.appendChild(element);
        }

        writeDocument(xml, document);
    }

    /**
     * Reads a DOM document from the given XML stream.
     *
     * @param xml XML stream
     * @return DOM document
     * @throws RepositoryException if the document could not be read
     */
    private Document readDocument(InputStream xml) throws RepositoryException {
        try {
            StreamSource source = new StreamSource(xml);
            DOMResult result = new DOMResult();
            transformer.transform(source, result);
            return (Document) result.getNode();
        } catch (TransformerException e) {
            throw new RepositoryException(
                    "Unable to read a node type definition file", e);
        }
    }

    /**
     * Writes a DOM document to the given XML stream.
     *
     * @param xml XML stream
     * @param document DOM document
     * @throws RepositoryException if the document could not be written
     */
    private void writeDocument(OutputStream xml, Document document)
            throws RepositoryException {
        try {
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(xml);
            transformer.transform(source, result);
        } catch (TransformerException e) {
            throw new RepositoryException(
                    "Unable to write a node type definition file", e);
        }
    }
}
