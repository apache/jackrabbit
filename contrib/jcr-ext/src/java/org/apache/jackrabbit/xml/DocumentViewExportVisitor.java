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
package org.apache.jackrabbit.xml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Item;
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
import org.apache.jackrabbit.name.Name;
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
 *     Node parent = ...;
 *     parent.accept(
 *         new DocumentViewExportVisitor(handler, true, false) {
 *
 *             protected boolean includeProperty(Property property)
 *                     throws RepositoryException {
 *                 if (property.getName().equals("title")) {
 *                     return super.includeProperty(property);
 *                 } else {
 *                     return false;
 *                 }
 *             }
 *
 *             protected boolean includeNode(Node node)
 *                     throws RepositoryException {
 *                 return (node.getDepth() <= root.getDepth() + 2);
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
 * can be implemented in terms of the above method and the XMLSerializer
 * class from the Xerces library:
 * <pre>
 * import org.apache.xml.serialize.XMLSerializer;
 * import org.apache.xml.serialize.OutputFormat;
 *
 *     public void exportDocumentView(
 *             String absPath, OutputStream output,
 *             boolean skipBinary, boolean noRecurse)
 *             throws PathNotFoundException, IOException, RepositoryException {
 *         try {
 *             XMLSerializer serializer =
 *                 new XMLSerializer(output, new OutputFormat());
 *             exportDocView(
 *                     absPath, serializer.asContentHandler(),
 *                     binaryAsLink, noRecurse);
 *         } catch (SAXException e) {
 *             throw new IOException(e.getMessage());
 *         }
 *     }
 * </pre>
 *
 * @see ItemVisitor
 * @see Session#exportDocumentView(String, ContentHandler, boolean, boolean)
 * @see Session#exportDocumentView(String, java.io.OutputStream, boolean, boolean)
 */
public class DocumentViewExportVisitor implements ItemVisitor {

    /** The special jcr:xmltext property name. */
    private static final String XMLTEXT = "jcr:xmltext";

    /** The special jcr:xmlcharacters property name. */
    private static final String XMLCHARACTERS = "jcr:xmlcharacters";

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
    protected Node root;

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
    public void visit(Property property) {
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
    public void visit(Node node) throws RepositoryException {
        try {
            // start document
            if (root == null) {
                root = node;
                handler.startDocument();

                Session session = root.getSession();
                String[] prefixes = session.getNamespacePrefixes();
                for (int i = 0; i < prefixes.length; i++) {
                    handler.startPrefixMapping(prefixes[i],
                            session.getNamespaceURI(prefixes[i]));
                }
            }

            // export current node
            if (!node.getName().equals(XMLTEXT)) {
                exportNode(node);
            } else if (node != root) {
                exportText(node);
            } else {
                throw new RepositoryException("Cannot export jcr:xmltext");
            }

            // end document
            if (root == node) {
                handler.endDocument();
            }
        } catch (SAXException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Checks whether the given property should be included in the XML
     * serialization. By default this method returns false for the special
     * "jcr:xmlcharacters" properties and for binary properties if the
     * skipBinary flag is set. Subclasses can extend this behaviour to
     * implement more selective XML serialization.
     * <p>
     * To avoid losing the default behaviour described above, subclasses
     * should always call super.includeProperty(property) instead of
     * simply returning true for a property.
     *
     * @param property the property to check
     * @return true if the property should be included, false otherwise
     * @throws RepositoryException on repository errors
     */
    protected boolean includeProperty(Property property)
            throws RepositoryException {
        return !property.getName().equals(XMLCHARACTERS)
            && (!skipBinary || property.getType() != PropertyType.BINARY);
    }

    /**
     * Checks whether the given node should be included in the XML
     * serialization. This method returns true by default, but subclasses
     * can extend this behaviour to implement selective XML serialization.
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
            Property property = node.getProperty(XMLCHARACTERS);
            char[] characters = property.getString().toCharArray();
            handler.characters(characters, 0, characters.length);
        } catch (PathNotFoundException ex) {
            // ignore empty jcr:xmltext nodes
        } catch (ValueFormatException ex) {
            // ignore non-string jcr:xmlcharacters properties
        }
    }

    /**
     * Serializes the given node to the XML stream. Generates an element
     * with the same name as this node, and maps node properties to
     * attributes of the generated element. If the noRecurse flag is false,
     * then child nodes are serialized as sub-elements.
     *
     * @param node the given node
     * @throws SAXException on SAX errors
     * @throws RepositoryException on repository errors
     */
    private void exportNode(Node node)
            throws SAXException, RepositoryException {
        Name qname = getQName(node);
        String localName = escapeName(qname.getLocalPart());
        String prefixedName =
            node.getSession().getNamespacePrefix(qname.getNamespaceURI())
            + ":" + localName;

        // Start element
        handler.startElement(
                qname.getNamespaceURI(), localName, prefixedName,
                getAttributes(node));

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
        handler.endElement(
                qname.getNamespaceURI(), qname.getLocalPart(), node.getName());
    }

    /**
     * Returns the document view attributes of the given Node. The
     * properties of the node are mapped to XML attributes directly as
     * name-value pairs.
     *
     * @param node the given node
     * @return document view attributes of the node
     * @throws RepositoryException on repository errors
     */
    private Attributes getAttributes(Node node) throws RepositoryException {
        AttributesImpl attributes = new AttributesImpl();

        PropertyIterator properties = node.getProperties();
        while (properties.hasNext()) {
            Property property = properties.nextProperty();
            if (includeProperty(property)) {
                Name qname = getQName(property);
                String value = getValue(property);
                attributes.addAttribute(qname.getNamespaceURI(),
                        qname.getLocalPart(), property.getName(),
                        "CDATA", value);
            }
        }

        return attributes;
    }

    /**
     * Returns the qualified XML name of the given item. If the item name
     * is prefixed, then the respective namespace URI is looked up from the
     * session and returned as a part of the qualified name. If the item
     * name is not prefixed, then the returned qualified name will use the
     * null namespace and the default prefix. The local part of the qualified
     * name will be escaped using ISO 9075 rules. If the given item is the
     * root node, then the special name "jcr:root" is returned
     * <p>
     * See section 6.4.2 of the JCR specification for more details about
     * name handling in the XML document view serialization.
     *
     * @param item the given item
     * @return qualified XML name of the item
     * @throws RepositoryException on repository errors
     * @see Name
     */
    private Name getQName(Item item) throws RepositoryException {
        String name = item.getName();
        if (name.length() == 0) {
            name = "jcr:root";
        }
        return Name.fromJCRName(item.getSession(), name);
    }

    /**
     * Escapes the given name or prefix according to the rules of section
     * 6.4.3 of the JSR 170 specification (version 0.16.2).
     *
     * @param name original name or prefix
     * @return escaped name or prefix
     */
    private String escapeName(String name) {
        if (name.length() == 0) {
            return name;
        }

        StringBuffer buffer = new StringBuffer();

        // First character
        if (!XMLChar.isNameStart(name.charAt(0)) || name.startsWith("_x")
                || (name.length() >= 3
                        && "xml".equalsIgnoreCase(name.substring(0, 3)))) {
            String hex =
                "000" + Integer.toHexString(name.charAt(0)).toUpperCase();
            buffer.append("_x" + hex.substring(hex.length() - 4) + "_");
        } else {
            buffer.append(name.charAt(0));
        }

        // Rest of the characters
        for (int i = 1; i < name.length(); i++) {
            if (!XMLChar.isName(name.charAt(i)) || name.startsWith("_x", i)) {
                String hex =
                    "000" + Integer.toHexString(name.charAt(0)).toUpperCase();
                buffer.append("_x" + hex.substring(hex.length() - 4) + "_");
            } else {
                buffer.append(name.charAt(i));
            }
        }

        return buffer.toString();
    }

    /**
     * Escapes the given value according to the rules of section 6.4.4 of
     * the JSR 170 specification (version 0.16.2).
     *
     * @param value original value
     * @return escaped value
     */
    private String escapeValue(String value) {
        StringBuffer buffer = new StringBuffer();

        for (int i = 1; i < value.length(); i++) {
            if (value.charAt(i) == ' ') {
                buffer.append("_x0020_");
            } else if (value.startsWith("_x", i)) {
                buffer.append("_x005F_");
            } else {
                buffer.append(value.charAt(i));
            }
        }

        return buffer.toString();
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
     * @throws RepositoryException on repository errors
     */
    private String getValue(Property property) throws RepositoryException {
        try {
            if (property.getDefinition().isMultiple()) {
                StringBuffer buffer = new StringBuffer();

                Value[] values = property.getValues();
                for (int i = 0; i < values.length; i++) {
                    if (i > 0) {
                        buffer.append(" ");
                    }
                    if (values[i].getType() == PropertyType.BINARY) {
                        buffer.append(encodeValue(values[i].getStream()));
                    } else {
                        buffer.append(escapeValue(values[i].getString()));
                    }
                }

                return buffer.toString();
            } else if (property.getType() == PropertyType.BINARY) {
                return encodeValue(property.getStream());
            } else {
                return property.getString();
            }
        } catch (IOException ex) {
            throw new RepositoryException(ex);
        }
    }

    /**
     * Encodes the given binary stream using Base64 encoding.
     *
     * @param input original binary value
     * @return Base64-encoded value
     * @throws IOException on IO errors
     */
    private String encodeValue(InputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] bytes = new byte[4096];
        for (int n = input.read(bytes); n != -1; n = input.read(bytes)) {
            buffer.write(bytes, 0, n);
        }
        return
            new String(Base64.encodeBase64(buffer.toByteArray()), "US-ASCII");
    }

}
