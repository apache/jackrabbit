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

import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.commons.codec.binary.Base64;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Generic system view exporter for JCR content repositories.
 * This class can be used to implement the XML system view export
 * operations using nothing but the standard JCR interfaces. The
 * export operation is implemented as an ItemVisitor that generates
 * the system view SAX event stream as it traverses the selected
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
 *         new SystemViewExportVisitor(handler, true, false) {
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
 * Session.exportSysView(String, ContentHandler, boolean, boolean)
 * method implemented in terms of this exporter class:
 * <pre>
 *     public void exportSysView(String absPath, ContentHandler handler,
 *             boolean skipBinary, boolean noRecurse) throws
 *             InvalidSerializedDataException, PathNotFoundException,
 *             SAXException, RepositoryException {
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
 * Session.exportSysView(String, OutputStream, boolean, boolean)
 * can be implemented in terms of the above method and the XMLSerializer
 * class from the Xerces library:
 * <pre>
 * import org.apache.xml.serialize.XMLSerializer;
 * import org.apache.xml.serialize.OutputFormat;
 *
 *     public void exportSysView(String absPath, OutputStream output,
 *             boolean skipBinary, boolean noRecurse) throws
 *             InvalidSerializedDataException, PathNotFoundException,
 *             IOException, RepositoryException {
 *         try {
 *             XMLSerializer serializer =
 *                 new XMLSerializer(output, new OutputFormat());
 *             exportSysView(absPath, serializer.asContentHandler(),
 *                     binaryAsLink, noRecurse);
 *         } catch (SAXException ex) {
 *             throw new IOException(ex.getMessage());
 *         }
 *     }
 * </pre>
 *
 * @see ItemVisitor
 * @see Session#exportSystemView(String, ContentHandler, boolean, boolean)
 * @see Session#exportSystemView(String, java.io.OutputStream, boolean, boolean)
 */
public class SystemViewExportVisitor implements ItemVisitor {

    /** The system view namespace URI. */
    private static final String SV = "http://www.jcp.org/jcr/sv/1.0";

    /** The special jcr:root node name. */
    private static final String JCR_ROOT = "jcr:root";

    /** The special jcr:uuid property name. */
    private static final String JCR_UUID = "jcr:uuid";

    /** The special jcr:primaryType property name. */
    private static final String JCR_MIXINTYPES = "jcr:mixinTypes";

    /** The special jcr:mixinTypes property name. */
    private static final String JCR_PRIMARYTYPE = "jcr:primaryType";

    /** The special sv:node element name. */
    private static final String SV_NODE = "sv:node";

    /** Local part of the special sv:node element name. */
    private static final String NODE = "node";

    /** The special sv:value element name. */
    private static final String SV_VALUE = "sv:value";

    /** Local part of the special sv:value element name. */
    private static final String VALUE = "value";

    /** The special sv:property element name. */
    private static final String SV_PROPERTY = "sv:property";

    /** Local part of the special sv:property element name. */
    private static final String PROPERTY = "property";

    /** The special sv:type element name. */
    private static final String SV_TYPE = "sv:type";

    /** Local part of the special sv:type element name. */
    private static final String TYPE = "type";

    /** The special sv:name element name. */
    private static final String SV_NAME = "sv:name";

    /** Local part of the special sv:name element name. */
    private static final String NAME = "name";

    /**
     * The SAX content handler for the serialized XML stream.
     */
    private ContentHandler handler;

    /**
     * Flag to skip all binary properties.
     */
    private boolean skipBinary;

    /**
     * Flag to only serialize the selected node.
     */
    private boolean noRecurse;

    /**
     * The root node of the serialization tree. This is the node that
     * is mapped to the root element of the serialized XML stream.
     */
    protected Node root;

    /**
     * Creates an visitor for exporting content using the system view
     * format. To actually perform the export operation, you need to pass
     * the visitor instance to the selected content node using the
     * Node.accept(ItemVisitor) method.
     *
     * @param handler the SAX event handler
     * @param skipBinary flag for ignoring binary properties
     * @param noRecurse flag for not exporting an entire content subtree
     */
    public SystemViewExportVisitor(
            ContentHandler handler, boolean skipBinary, boolean noRecurse) {
        this.handler = handler;
        this.skipBinary = skipBinary;
        this.noRecurse = noRecurse;
        this.root = null;
    }

    /**
     * Exports the visited property using the system view serialization
     * format. This method generates an sv:property element with appropriate
     * sv:name and sv:type attributes. The value or values of the node
     * are included as sv:value sub-elements.
     *
     * @param property the visited property
     * @throws RepositoryException on repository errors
     */
    public void visit(Property property) throws RepositoryException {
        try {
            AttributesImpl attributes = new AttributesImpl();
            attributes.addAttribute(SV, NAME, SV_NAME,
                    "CDATA", property.getName());
            attributes.addAttribute(SV, TYPE, SV_TYPE,
                    "CDATA", PropertyType.nameFromValue(property.getType()));
            handler.startElement(SV, PROPERTY, SV_PROPERTY, attributes);

            if (property.getDefinition().isMultiple()) {
                Value[] values = property.getValues();
                for (int i = 0; i < values.length; i++) {
                    exportValue(values[i]);
                }
            } else {
                exportValue(property.getValue());
            }

            handler.endElement(SV, PROPERTY, SV_PROPERTY);
        } catch (SAXException ex) {
            throw new RepositoryException(ex);
        }
    }

    /**
     * Exports the visited node using the system view serialization format.
     * This method is the main entry point to the serialization mechanism.
     * It manages the opening and closing of the SAX event stream and the
     * registration of the namespace mappings. The process of actually
     * generating the document view SAX events is spread into various
     * private methods, and can be controlled by overriding the protected
     * includeProperty() and includeNode() methods.
     *
     * @param node the node to visit
     * @throws RepositoryException on repository errors
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
            exportNode(node);

            // end document
            if (root == node) {
                handler.endDocument();
            }
        } catch (SAXException ex) {
            throw new RepositoryException(ex);
        }
    }

    /**
     * Checks whether the given property should be included in the XML
     * serialization. By default this method returns false for the special
     * jcr:primaryType, jcr:mixinTypes, and jcr:uuid properties that are
     * required by the system view format. This method also returns false
     * for all binary properties if the skipBinary flag is set.
     * Subclasses can extend this default behaviour to implement more
     * selective XML serialization.
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
        String name = property.getName();
        return !name.equals(JCR_PRIMARYTYPE)
            && !name.equals(JCR_MIXINTYPES)
            && !name.equals(JCR_UUID)
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
     * Serializes the given node to the XML stream. This method generates
     * an sv:node element that contains the node name as the sv:name
     * attribute. Node properties are included as sv:property elements
     * and child nodes as other sv:node sub-elements.
     *
     * @param node the given node
     * @throws SAXException on SAX errors
     * @throws RepositoryException on repository errors
     */
    private void exportNode(Node node)
            throws SAXException, RepositoryException {
        String name = node.getName();
        if (name.length() == 0) {
            name = JCR_ROOT;
        }

        // Start element
        AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute(SV, NAME, SV_NAME, "CDATA", name);
        handler.startElement(SV, NODE, SV_NODE, attributes);

        // Visit the meta-properties
        node.getProperty(JCR_PRIMARYTYPE).accept(this);
        if (node.hasProperty(JCR_MIXINTYPES)) {
            node.getProperty(JCR_MIXINTYPES).accept(this);
        }
        if (node.hasProperty(JCR_UUID)) {
            node.getProperty(JCR_UUID).accept(this);
        }

        // Visit other properties
        PropertyIterator properties = node.getProperties();
        while (properties.hasNext()) {
            Property property = properties.nextProperty();
            if (includeProperty(property)) {
                property.accept(this);
            }
        }

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
        handler.endElement(SV, NODE, SV_NODE);
    }

    /**
     * Serializes the given value to the XML stream. This method generates
     * an sv:value element and writes the string representation of the
     * given value as the character content of the element. Binary values
     * are encoded using the Base64 encoding.
     *
     * @param value the given value
     * @throws SAXException on SAX errors
     * @throws RepositoryException on repository errors
     */
    private void exportValue(Value value)
            throws SAXException, RepositoryException {
        try {
            handler.startElement(SV, VALUE, SV_VALUE, new AttributesImpl());

            if (value.getType() != PropertyType.BINARY) {
                char[] characters = value.getString().toCharArray();
                handler.characters(characters, 0, characters.length);
            } else {
                char[] characters =
                    encodeValue(value.getStream()).toCharArray();
                handler.characters(characters, 0, characters.length);
            }

            handler.endElement(SV, VALUE, SV_VALUE);
        } catch (IOException e) {
            throw new RepositoryException(e);
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
