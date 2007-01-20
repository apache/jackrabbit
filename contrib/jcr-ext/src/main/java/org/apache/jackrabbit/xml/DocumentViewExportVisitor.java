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
package org.apache.jackrabbit.xml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.apache.commons.codec.binary.Base64;
import org.apache.jackrabbit.name.QName;
import org.apache.xerces.util.XMLChar;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Generic document view exporter for JCR content repositories.
 * This class can be used to implement the XML document view export
 * operations using nothing but the standard JCR interfaces. The
 * export operation is implemented as an ItemVisitor that generates
 * the document view SAX event stream as it traverses the selected
 * JCR content tree.
 *
 * <h2>Implementing a customized XML serializer</h2>
 * <p>
 * A client can extend this class to provide customized XML serialization
 * formats. By overriding the protected includeProperty() and includeNode()
 * methods, a subclass can select which properties and nodes will be included
 * in the serialized XML stream.
 * <p>
 * For example, the following code implements an XML serialization that only
 * contains the titles of the first two levels of the node tree.
 * <pre>
 *     ContentHandler handler = ...;
 *     final Node parent = ...;
 *     parent.accept(
 *         new DocumentViewExportVisitor(handler, true, false) {
 *
 *             protected boolean includeProperty(Property property)
 *                     throws RepositoryException {
 *                 return property.getName().equals("title");
 *             }
 *
 *             protected boolean includeNode(Node node)
 *                     throws RepositoryException {
 *                 return (node.getDepth() <= parent.getDepth() + 2);
 *             }
 *
 *         });
 * </pre>
 *
 * <h2>Implementing the standard export methods</h2>
 * <p>
 * The following is an example of the
 * Session.exportDocumentView(String, ContentHandler, boolean, boolean)
 * method implemented in terms of this exporter class:
 * <pre>
 *     public void exportDocumentView(
 *             String absPath, ContentHandler handler,
 *             boolean skipBinary, boolean noRecurse)
 *             throws PathNotFoundException, SAXException, RepositoryException {
 *         Item item = getItem(absPath);
 *         if (item.isNode()) {
 *             item.accept(new DocumentViewExportVisitor(
 *                     handler, skipBinary, noRecurse));
 *         } else {
 *             throw new PathNotFoundException("Invalid node path: " + path);
 *         }
 *     }
 * </pre>
 * <p>
 * The companion method
 * Session.exportDocumentView(String, OutputStream, boolean, boolean)
 * can be implemented in terms of the above method:
 * <pre>
 *     public void exportDocumentView(
 *             String absPath, OutputStream output,
 *             boolean skipBinary, boolean noRecurse)
 *             throws PathNotFoundException, IOException, RepositoryException {
 *         try {
 *             SAXTransformerFactory factory = (SAXTransformerFactory)
 *                 SAXTransformerFactory.newInstance();
 *             TransformerHandler handler = factory.newTransformerHandler();
 *             handler.setResult(new StreamResult(out));
 *             exportDocumentView(absPath, handler, skipBinary, noRecurse);
 *         } catch (TransformerConfigurationException e) {
 *             throw new IOException(
 *                     "Unable to configure a SAX transformer: " + e.getMessage());
 *         } catch (SAXException e) {
 *             throw new IOException(
 *                     "Unable to serialize a SAX stream: " + e.getMessage());
 *         }
 *     }
 * </pre>
 *
 * @see ItemVisitor
 * @see Session#exportDocumentView(String, ContentHandler, boolean, boolean)
 * @see Session#exportDocumentView(String, java.io.OutputStream, boolean, boolean)
 */
public class DocumentViewExportVisitor implements ItemVisitor {

    /**
     * The SAX content handler for the serialized XML stream.
     */
    private final ContentHandler handler;

    /**
     * Flag to skip all binary properties.
     */
    private final boolean skipBinary;

    /**
     * Flag to only serialize the selected node.
     */
    private final boolean noRecurse;

    /**
     * The root node of the serialization tree. This is the node that
     * is mapped to the root element of the serialized XML stream.
     */
    private Node root;

    /**
     * The current session.
     */
    private Session session;

    /**
     * The prefix mapped to the <code>http://www.jcp.org/jcr/1.0</code>
     * namespace in the current session.
     */
    private String jcr;

    /**
     * Creates an visitor for exporting content using the document view
     * format. To actually perform the export operation, you need to pass
     * the visitor instance to the selected content node using the
     * Node.accept(ItemVisitor) method.
     *
     * @param handler the SAX event handler
     * @param skipBinary flag for ignoring binary properties
     * @param noRecurse flag for not exporting an entire content subtree
     */
    public DocumentViewExportVisitor(
            ContentHandler handler, boolean skipBinary, boolean noRecurse) {
        this.handler = handler;
        this.skipBinary = skipBinary;
        this.noRecurse = noRecurse;
        this.root = null;
    }

    /**
     * Ignored. Properties are included as attributes of node elements.
     *
     * @param property ignored property
     * @see ItemVisitor#visit(Property)
     */
    public final void visit(Property property) {
    }

    /**
     * Exports the visited node using the document view serialization format.
     * This method is the main entry point to the serialization mechanism.
     * It manages the opening and closing of the SAX event stream and the
     * registration of the namespace mappings. The process of actually
     * generating the document view SAX events is spread into various
     * private methods, and can be controlled by overriding the protected
     * includeProperty() and includeNode() methods.
     *
     * @param node the node to visit
     * @throws RepositoryException on repository errors
     * @see ItemVisitor#visit(Node)
     * @see #includeProperty(Property)
     * @see #includeNode(Node)
     */
    public final void visit(Node node) throws RepositoryException {
        try {
            // start document
            if (root == null) {
                root = node;
                session = node.getSession();
                jcr = session.getNamespacePrefix(QName.NS_JCR_URI); 
                handler.startDocument();

                String[] prefixes = session.getNamespacePrefixes();
                for (int i = 0; i < prefixes.length; i++) {
                    String uri = session.getNamespaceURI(prefixes[i]);
                    if (!uri.equals(QName.NS_XML_URI)) {
                        handler.startPrefixMapping(prefixes[i], uri);
                    }
                }
            }

            // export current node
            String name = node.getName();
            if (!name.equals(jcr + ":xmltext")) {
                int colon = name.indexOf(':');
                if (colon != -1) {
                    String prefix = name.substring(0, colon);
                    name = name.substring(colon + 1);
                    exportNode(node, prefix, escapeName(name));
                } else if (name.length() > 0) {
                    exportNode(node, "", escapeName(name));
                } else {
                    exportNode(node, jcr, "root");
                }
            } else if (node != root) {
                exportText(node);
            } else {
                throw new RepositoryException("Cannot export jcr:xmltext");
            }

            // end document
            if (root == node) {
                String[] prefixes = session.getNamespacePrefixes();
                for (int i = 0; i < prefixes.length; i++) {
                    String uri = session.getNamespaceURI(prefixes[i]);
                    if (!uri.equals(QName.NS_XML_URI)) {
                        handler.endPrefixMapping(prefixes[i]);
                    }
                }
                handler.endDocument();
            }
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (SAXException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Checks whether the given property should be included in the XML
     * serialization. This method returns <code>true</code> by default,
     * but subclasses can override this method to implement more selective
     * XML serialization.
     *
     * @param property the property to check
     * @return true if the property should be included, false otherwise
     * @throws RepositoryException on repository errors
     */
    protected boolean includeProperty(Property property)
            throws RepositoryException {
        return true;
    }

    /**
     * Checks whether the given node should be included in the XML
     * serialization. This method returns <code>true</code> by default,
     * but subclasses override this method to implement selective
     * XML serialization.
     * <p>
     * Note that this method is only called for the descendants of the
     * root node of the serialized tree. Also, this method is never called
     * if the noRecurse flag is set because no descendant nodes will be
     * serialized anyway.
     *
     * @param node the node to check
     * @return true if the node should be included, false otherwise
     * @throws RepositoryException on repository errors
     */
    protected boolean includeNode(Node node) throws RepositoryException {
        return true;
    }

    /**
     * Serializes a special "jcr:xmltext" node. Only the contents of the
     * "jcr:xmlcharacters" property will be written as characters to the
     * XML stream and no elements or attributes will be generated for
     * this node or any other child nodes or properties.
     *
     * @param node the "jcr:xmltext" node
     * @throws SAXException on SAX errors
     * @throws RepositoryException on repository errors
     */
    private void exportText(Node node)
            throws SAXException, RepositoryException {
        try {
            Property property = node.getProperty(jcr + ":xmlcharacters");
            char[] characters = filterXML(property.getString());
            handler.characters(characters, 0, characters.length);
        } catch (PathNotFoundException ex) {
            // ignore empty jcr:xmltext nodes
        } catch (ValueFormatException ex) {
            // ignore non-string jcr:xmlcharacters properties
        }
    }

    /**
     * Serializes the given node to the XML stream. Generates an element
     * with the given name, and maps node properties to attributes of the
     * generated element. If the noRecurse flag is false, then child nodes
     * are serialized as sub-elements.
     *
     * @param node the given node
     * @param prefix namespace prefix
     * @param name escaped local name
     * @throws IOException if a problem with binary values occurred
     * @throws SAXException on SAX errors
     * @throws RepositoryException on repository errors
     */
    private void exportNode(Node node, String prefix, String name)
            throws IOException, SAXException, RepositoryException {
        // Set up element name components
        String prefixedName = name;
        if (prefix.length() > 0) {
            prefixedName = prefix + ":" + name;
        } else {
            prefixedName = name;
        }
        String uri = session.getNamespaceURI(prefix);
        if (uri.length() == 0) {
            uri = null;
        }

        // Start element
        handler.startElement(uri, name, prefixedName, getAttributes(node));

        // Visit child nodes (unless denied by the noRecurse flag)
        if (!noRecurse) {
            NodeIterator children = node.getNodes();
            while (children.hasNext()) {
                Node child = children.nextNode();
                if (includeNode(child)) {
                    child.accept(this);
                }
            }
        }

        // End element
        handler.endElement(uri, name, prefixedName);
    }

    /**
     * Returns the document view attributes of the given Node. The
     * properties of the node are mapped to XML attributes directly as
     * name-value pairs.
     *
     * @param node the given node
     * @return document view attributes of the node
     * @throws IOException if a problem with binary values occurred
     * @throws RepositoryException on repository errors
     */
    private Attributes getAttributes(Node node)
            throws IOException, RepositoryException {
        AttributesImpl attributes = new AttributesImpl();
        
        PropertyIterator properties = node.getProperties();
        while (properties.hasNext()) {
            Property property = properties.nextProperty();
            if (!(skipBinary && property.getType() == PropertyType.BINARY)
                    && includeProperty(property)) {
                String name = property.getName();
                String value = escapeValue(property);

                String prefixedName;
                String uri;
                int colon = name.indexOf(':');
                if (colon != -1) {
                    String prefix = name.substring(0, colon);
                    uri = session.getNamespaceURI(prefix);
                    name = escapeName(name.substring(colon + 1));
                    prefixedName = prefix + ":" + name;
                } else {
                    uri = session.getNamespaceURI("");
                    name = escapeName(name);
                    prefixedName = name;
                }
                attributes.addAttribute(uri, name, prefixedName, "CDATA", value);
            }
        }
        
        return attributes;
    }

    private static char[] filterXML(String value) {
        char[] characters = value.toCharArray();
        for (int i = 0; i < characters.length; i++) {
            if (XMLChar.isInvalid(characters[i])) {
                characters[i] = ' '; // TODO: What's the correct escape?
            }
        }
        return characters;
    }

    /**
     * Escapes the given JCR name according to the rules of section
     * 6.4.3 of the JSR 170 specification.
     *
     * @param name JCR name
     * @return escaped name
     */
    private static String escapeName(String name) {
        StringBuffer buffer = new StringBuffer();

        int colon = name.indexOf(':');
        if (colon != -1) {
            buffer.append(name.substring(0, colon + 1));
            name = name.substring(colon + 1);
        }

        Pattern pattern = Pattern.compile("_([0-9a-fA-F]{4}_)");
        Matcher matcher = pattern.matcher(name);
        char[] characters = filterXML(matcher.replaceAll("_x005f_$1"));

        for (int i = 0; i < characters.length; i++) {
            char ch = characters[i];
            if ((i == 0) ? XMLChar.isNCNameStart(ch) : XMLChar.isNCName(ch)) {
                String hex = Integer.toHexString((int) ch);
                buffer.append("_x");
                for (int j = 4; j > hex.length(); j--) {
                    buffer.append('0');
                }
                buffer.append(hex);
                buffer.append('_');
            } else {
                buffer.append(ch);
            }
        }

        return buffer.toString();
    }

    /**
     * Returns the string representation of the given value. Binary values
     * are encoded in Base64, while other values are just converted to their
     * string format.
     *
     * @param value original value
     * @param escape whether to apply value escapes
     * @return escaped value
     * @throws IOException if a problem with binary values occurred
     * @throws RepositoryException on repository errors
     */
    private static String escapeValue(Value value, boolean escape)
            throws IOException, RepositoryException {
        if (value.getType() == PropertyType.BINARY) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            InputStream input = value.getStream();
            try {
                byte[] bytes = new byte[4096];
                for (int n = input.read(bytes); n != -1; n = input.read(bytes)) {
                    buffer.write(bytes, 0, n);
                }
            } finally {
                input.close();
            }
            return new String(Base64.encodeBase64(buffer.toByteArray()), "ASCII");
        } else if (escape) {
            StringBuffer buffer = new StringBuffer();
            Pattern pattern = Pattern.compile("_([0-9a-fA-F]{4}_)");
            Matcher matcher = pattern.matcher(value.getString());
            char[] characters = filterXML(matcher.replaceAll("_x005f_$1"));
            for (int i = 0; i < characters.length; i++) {
                if (characters[i] == ' ') {
                    buffer.append("_x0020_");
                } else if (characters[i] == '\t') {
                    buffer.append("_x0009_");
                } else if (characters[i] == '\r') {
                    buffer.append("_x000D_");
                } else if (characters[i] == '\n') {
                    buffer.append("_x000A_");
                } else {
                    buffer.append(characters[i]);
                }
            }
            return buffer.toString();
        } else {
            return new String(filterXML(value.getString()));
        }
    }

    /**
     * Returns the document view representation of the given property.
     * Multiple values are combined into a space-separated list of
     * space-escaped string values, binary values are encoded using the
     * Base64 encoding, and other values are simply returned using their
     * default string representation.
     *
     * @param property the given property
     * @return document view representation of the property value
     * @throws IOException if a problem with binary values occurred
     * @throws RepositoryException on repository errors
     */
    private static String escapeValue(Property property)
            throws IOException, RepositoryException {
        if (property.getDefinition().isMultiple()) {
            StringBuffer buffer = new StringBuffer();
            Value[] values = property.getValues();
            for (int i = 0; i < values.length; i++) {
                if (i > 0) {
                    buffer.append(' ');
                }
                buffer.append(escapeValue(values[i], true));
            }
            return buffer.toString();
        } else {
            return escapeValue(property.getValue(), false);
        }
    }

}
