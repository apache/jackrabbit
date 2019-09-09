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
package org.apache.jackrabbit.commons.xml;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.apache.jackrabbit.commons.NamespaceHelper;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Abstract base class for document and system view exporters. This class
 * takes care of all the details related to namespace mappings, shareable
 * nodes, recursive exports, binary values, and so on while leaving the
 * decisions about what kind of SAX events to generate to subclasses.
 * <p>
 * A subclass should only need to implement the abstract methods of this
 * class to produce a fully functional exporter.
 *
 * @since Jackrabbit JCR Commons 1.5
 */
public abstract class Exporter {

    /**
     * Attributes of the next element. This single instance is reused for
     * all elements by simply clearing it after each element has been emitted.
     */
    private final AttributesImpl attributes = new AttributesImpl();

    /**
     * Stack of namespace mappings.
     */
    private final LinkedList stack = new LinkedList();

    /**
     * The UUID strings of all shareable nodes already exported.
     */
    private final Set shareables = new HashSet();

    /**
     * Whether the current node is a shareable node that has already been
     * exported.
     */
    private boolean share = false;

    /**
     * Current session.
     */
    private final Session session;

    /**
     * Namespace helper.
     */
    protected final NamespaceHelper helper;

    /**
     * SAX event handler to which the export events are sent.
     */
    private final ContentHandler handler;

    /**
     * Whether to export the subtree or just the given node.
     */
    private final boolean recurse;

    /**
     * Whether to export binary values.
     */
    private final boolean binary;

    /**
     * Creates an exporter instance.
     *
     * @param session current session
     * @param handler SAX event handler
     * @param recurse whether the export should be recursive
     * @param binary whether the export should include binary values
     */
    protected Exporter(
            Session session, ContentHandler handler,
            boolean recurse, boolean binary) {
        this.session = session;
        this.helper = new NamespaceHelper(session);
        this.handler = handler;
        this.recurse = recurse;
        this.binary = binary;
        stack.add(new HashMap());
    }

    /**
     * Exports the given node by preparing the export and calling the
     * abstract {@link #exportNode(String, String, Node)} method to give
     * control of the export format to a subclass.
     * <p>
     * This method should be called only once for an exporter instance.
     *
     * @param node node to be exported
     * @throws SAXException if a SAX error occurs
     * @throws RepositoryException if a repository error occurs
     */
    public void export(Node node) throws RepositoryException, SAXException {
        handler.startDocument();

        String[] prefixes = session.getNamespacePrefixes();
        for (int i = 0; i < prefixes.length; i++) {
            if (prefixes[i].length() > 0 && !prefixes[i].equals("xml") ) {
                addNamespace(prefixes[i], session.getNamespaceURI(prefixes[i]));
            }
        }

        exportNode(node);

        handler.endDocument();
    }

    /**
     * Called to export the given node. The node name (or <code>jcr:root</code>
     * if the node is the root node) is given as an explicit pair of the
     * resolved namespace URI and local part of the name.
     * <p>
     * The implementation of this method should call the methods
     * {@link #exportProperties(Node)} and {@link #exportProperties(Node)}
     * to respectively export the properties and child nodes of the given node.
     * Those methods will call back to the implementations of this method and
     * the abstract property export methods so the subclass can decide what
     * SAX events to emit for each exported item.
     *
     * @param uri node namespace
     * @param local node name
     * @param node node
     * @throws RepositoryException if a repository error occurs
     * @throws SAXException if a SAX error occurs
     */
    protected abstract void exportNode(String uri, String local, Node node)
            throws RepositoryException, SAXException;

    /**
     * Called by {@link #exportProperties(Node)} to process a single-valued
     * property.
     *
     * @param uri property namespace
     * @param local property name
     * @param value property value
     * @throws RepositoryException if a repository error occurs
     * @throws SAXException if a SAX error occurs
     */
    protected abstract void exportProperty(
            String uri, String local, Value value)
            throws RepositoryException, SAXException;

    /**
     * Called by {@link #exportProperties(Node)} to process a multivalued
     * property.
     *
     * @param uri property namespace
     * @param local property name
     * @param type property type
     * @param values property values
     * @throws RepositoryException if a repository error occurs
     * @throws SAXException if a SAX error occurs
     */
    protected abstract void exportProperty(
            String uri, String local, int type, Value[] values)
            throws RepositoryException, SAXException;

    /**
     * Called by {@link #exportNode(String, String, Node)} to recursively
     * call {@link #exportNode(String, String, Node)} for each child node.
     * Does nothing if this exporter is not recursive. 
     *
     * @param node parent node
     * @throws RepositoryException if a repository error occurs
     * @throws SAXException if a SAX error occurs
     */
    protected void exportNodes(Node node)
            throws RepositoryException, SAXException {
        if (recurse && !share) {
            NodeIterator iterator = node.getNodes();
            while (iterator.hasNext()) {
                Node child = iterator.nextNode();
                exportNode(child);
            }
        }
    }

    /**
     * Processes all properties of the given node by calling the abstract
     * {@link #exportProperty(String, String, Value)} and
     * {@link #exportProperty(String, String, int, Value[])} methods for
     * each property depending on whether the property is single- or
     * multivalued.
     * <p>
     * The first properties to be processed are <code>jcr:primaryType</code>,
     * <code>jcr:mixinTypes</code>, and <code>jcr:uuid</code>, and then the
     * remaining properties ordered by their names.
     * <p>
     * If the node is a shareable node that has already been encountered by
     * this event generator, then only a <code>jcr:primaryType</code> property
     * with the fixed value "nt:share" and the <code>jcr:uuid</code> property
     * of the shareable node are exported.
     *
     * @see <a href="https://issues.apache.org/jira/browse/JCR-1084">JCR-1084</a>
     * @param node node
     * @throws RepositoryException if a repository error occurs
     * @throws SAXException if a SAX error occurs
     */
    protected void exportProperties(Node node)
            throws RepositoryException, SAXException {
        // if node is shareable and has already been serialized, change its
        // type to nt:share and process only the properties jcr:primaryType
        // and jcr:uuid (mix:shareable is referenceable, so jcr:uuid exists)
        if (share) {
            ValueFactory factory = session.getValueFactory();
            exportProperty(
                    NamespaceHelper.JCR, "primaryType",
                    factory.createValue(
                            helper.getJcrName("nt:share"), PropertyType.NAME));
            exportProperty(
                    NamespaceHelper.JCR, "uuid",
                    factory.createValue(node.getUUID()));
        } else {
            // Standard behaviour: return all properties (sorted, see JCR-1084)
            SortedMap properties = getProperties(node);

            // serialize jcr:primaryType, jcr:mixinTypes & jcr:uuid first:
            exportProperty(properties, helper.getJcrName("jcr:primaryType"));
            exportProperty(properties, helper.getJcrName("jcr:mixinTypes"));
            exportProperty(properties, helper.getJcrName("jcr:uuid"));

            // serialize remaining properties
            Iterator iterator = properties.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                String name = (String) entry.getKey();
                exportProperty(name, (Property) entry.getValue());
            }
        }
    }

    /**
     * Utility method for exporting the given node. Parses the node name
     * (or <code>jcr:root</code> if given the root node) and calls
     * {@link #exportNode(String, String, Node)} with the resolved namespace
     * URI and the local part of the name.
     *
     * @param node node
     * @throws RepositoryException if a repository error occurs
     * @throws SAXException if a SAX error occurs
     */
    private void exportNode(Node node)
            throws RepositoryException, SAXException {
        share = node.isNodeType(helper.getJcrName("mix:shareable"))
            && !shareables.add(node.getUUID());

        if (node.getDepth() == 0) {
            exportNode(NamespaceHelper.JCR, "root", node);
        } else {
            String name = node.getName();
            int colon = name.indexOf(':');
            if (colon == -1) {
                exportNode("", name, node);
            } else {
                String uri = session.getNamespaceURI(name.substring(0, colon));
                exportNode(uri, name.substring(colon + 1), node);
            }
        }
    }

    /**
     * Returns a sorted map of the properties of the given node.
     *
     * @param node JCR node
     * @return sorted map (keyed by name) of properties
     * @throws RepositoryException if a repository error occurs
     */
    private SortedMap getProperties(Node node) throws RepositoryException {
        SortedMap properties = new TreeMap();
        PropertyIterator iterator = node.getProperties();
        while (iterator.hasNext()) {
            Property property = iterator.nextProperty();
            properties.put(property.getName(), property);
        }
        return properties;
    }

    /**
     * Utility method for processing the named property from the given
     * map of properties. If the property exists, it is removed from the
     * given map and passed to {@link #exportProperty(Property)}.
     * The property is ignored if it does not exist.
     *
     * @param properties map of properties
     * @param name property name
     * @throws RepositoryException if a repository error occurs
     * @throws SAXException if a SAX error occurs
     */
    private void exportProperty(Map properties, String name)
            throws RepositoryException, SAXException {
        Property property = (Property) properties.remove(name);
        if (property != null) {
            exportProperty(name, property);
        }
    }

    /**
     * Utility method for processing the given property. Calls either
     * {@link #exportProperty(Value)} or {@link #exportProperty(int, Value[])}
     * depending on whether the the property is single- or multivalued.
     *
     * @param name property name
     * @param property property
     * @throws RepositoryException if a repository error occurs
     * @throws SAXException if a SAX error occurs
     */
    private void exportProperty(String name, Property property)
            throws RepositoryException, SAXException {
        String uri = "";
        String local = name;
        int colon = name.indexOf(':');
        if (colon != -1) {
            uri = session.getNamespaceURI(name.substring(0, colon));
            local = name.substring(colon + 1);
        }

        int type = property.getType();
        if (type != PropertyType.BINARY || binary) {
            if (property.isMultiple()) {
                exportProperty(uri, local, type, property.getValues());
            } else {
                exportProperty(uri, local, property.getValue());
            }
        } else {
            ValueFactory factory = session.getValueFactory();
            Value value = factory.createValue("", PropertyType.BINARY);
            if (property.isMultiple()) {
                exportProperty(uri, local, type, new Value[] { value });
            } else {
                exportProperty(uri, local, value);
            }
        }
    }

    //---------------------------------------------< XML handling methods >--

    /**
     * Emits a characters event with the given character content.
     *
     * @param ch character array
     * @param start start offset within the array
     * @param length number of characters to emit
     * @throws SAXException if a SAX error occurs
     */
    protected void characters(char[] ch, int start, int length)
            throws SAXException {
        handler.characters(ch, start, length);
    }

    /**
     * Adds the given attribute to be included in the next element.
     *
     * @param uri namespace URI of the attribute
     * @param local local name of the attribute
     * @param value attribute value
     * @throws RepositoryException if a repository error occurs
     */
    protected void addAttribute(String uri, String local, String value)
            throws RepositoryException {
        attributes.addAttribute(
                uri, local, getXMLName(uri, local), "CDATA", value);
    }

    /**
     * Emits the start element event for an element with the given name.
     * All the attributes added using
     * {@link #addAttribute(String, String, String)} are included in the
     * element along with any new namespace mappings. The namespace stack
     * is extended for potential child elements.
     *
     * @param uri namespace URI or the element
     * @param local local name of the element
     * @throws RepositoryException if a repository error occurs
     * @throws SAXException if a SAX error occurs
     */
    protected void startElement(String uri, String local)
            throws SAXException, RepositoryException {
        // Prefixed name is generated before namespace handling so that a
        // potential new prefix mapping gets included as a xmlns attribute
        String name = getXMLName(uri, local);

        // Add namespace mappings
        Map namespaces = (Map) stack.getFirst();
        Iterator iterator = namespaces.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String namespace = (String) entry.getKey();
            String prefix = (String) entry.getValue();
            handler.startPrefixMapping(prefix, namespace);
            attributes.addAttribute(
                    "http://www.w3.org/2000/xmlns/", prefix, "xmlns:" + prefix,
                    "CDATA", namespace);
        }

        // Emit the start element event, and clear things for the next element
        handler.startElement(uri, local, name, attributes);
        attributes.clear();
        stack.addFirst(new HashMap());
    }

    /**
     * Emits the end element event for an element with the given name.
     * The namespace stack and mappings are automatically updated.
     *
     * @param uri namespace URI or the element
     * @param local local name of the element
     * @throws RepositoryException if a repository error occurs
     * @throws SAXException if a SAX error occurs
     */
    protected void endElement(String uri, String local)
            throws SAXException, RepositoryException {
        stack.removeFirst();
        handler.endElement(uri, local, getXMLName(uri, local));

        Map namespaces = (Map) stack.getFirst();
        Iterator iterator = namespaces.values().iterator();
        while (iterator.hasNext()) {
            handler.endPrefixMapping((String) iterator.next());
        }
        namespaces.clear();
    }

    /**
     * Returns a prefixed XML name for the given namespace URI and local
     * name. If a prefix mapping for the namespace URI is not yet available,
     * it is created based on the namespace mappings of the current JCR
     * session.
     *
     * @param uri namespace URI
     * @param local local name
     * @return prefixed XML name
     * @throws RepositoryException if a JCR namespace mapping is not available
     */
    protected String getXMLName(String uri, String local)
            throws RepositoryException {
        if (uri.length() == 0) {
            return local;
        } else {
            String prefix = getPrefix(uri);
            if (prefix == null) {
                prefix = getUniquePrefix(session.getNamespacePrefix(uri));
                ((Map) stack.getFirst()).put(uri, prefix);
            }
            return prefix + ":" + local;
        }
    }

    /**
     * Adds the given namespace to the export. A unique prefix based on
     * the given prefix hint is mapped to the given namespace URI. If the
     * namespace is already mapped, then the existing prefix is returned.
     *
     * @param hint prefix hint
     * @param uri namespace URI
     * @return registered prefix
     */
    protected String addNamespace(String hint, String uri) {
        String prefix = getPrefix(uri);
        if (prefix == null) {
            prefix = getUniquePrefix(hint);
            ((Map) stack.getFirst()).put(uri, prefix);
        }
        return prefix;
    }

    /**
     * Returns the namespace prefix mapped to the given URI. Returns
     * <code>null</code> if the namespace URI is not registered.
     *
     * @param uri namespace URI
     * @return prefix mapped to the URI, or <code>null</code>
     */
    private String getPrefix(String uri) {
        Iterator iterator = stack.iterator();
        while (iterator.hasNext()) {
            String prefix = (String) ((Map) iterator.next()).get(uri);
            if (prefix != null) {
                return prefix;
            }
        }
        return null;
    }

    /**
     * Returns a unique namespace prefix based on the given hint.
     * We need prefixes to be unique within the current namespace
     * stack as otherwise for example a previously added attribute
     * to the current element might incorrectly be using a prefix
     * that is being redefined in this element.
     *
     * @param hint prefix hint
     * @return unique prefix
     */
    private String getUniquePrefix(String hint) {
        String prefix = hint;
        for (int i = 2; prefixExists(prefix); i++) {
            prefix = hint + i;
        }
        return prefix;
    }

    /**
     * Checks whether the given prefix is already mapped within the
     * current namespace stack.
     *
     * @param prefix namespace prefix
     * @return <code>true</code> if the prefix is mapped,
     *         <code>false</code> otherwise
     */
    private boolean prefixExists(String prefix) {
        Iterator iterator = stack.iterator();
        while (iterator.hasNext()) {
            if (((Map) iterator.next()).containsValue(prefix)) {
                return true;
            }
        }
        return false;
    }

}
