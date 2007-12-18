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
package org.apache.jackrabbit.core.xml;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.namespace.SessionNamespaceResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.NamespaceException;

/**
 * <code>AbstractSAXEventGenerator</code> serves as the base class for
 * <code>SysViewSAXEventGenerator</code> and <code>DocViewSAXEventGenerator</code>
 * <p/>
 * It traverses a tree of <code>Node</code> & <code>Property</code>
 * instances, and calls the abstract methods
 * <ul>
 * <li><code>{@link #entering(Node, int)}</code></li>
 * <li><code>{@link #enteringProperties(Node, int)}</code></li>
 * <li><code>{@link #leavingProperties(Node, int)}</code></li>
 * <li><code>{@link #leaving(Node, int)}</code></li>
 * <li><code>{@link #entering(Property, int)}</code></li>
 * <li><code>{@link #leaving(Property, int)}</code></li>
 * </ul>
 * for every item it encounters.
 */
abstract class AbstractSAXEventGenerator {

    private static Logger log = LoggerFactory.getLogger(AbstractSAXEventGenerator.class);

    /**
     * the session to be used for resolving namespace mappings
     */
    protected final Session session;
    /**
     * the session's namespace resolver
     */
    protected final NamespaceResolver nsResolver;

    protected final NamePathResolver resolver;

    /**
     * the content handler to feed the SAX events to
     */
    protected final ContentHandler contentHandler;

    protected final Node startNode;
    protected final boolean skipBinary;
    protected final boolean noRecurse;

    /**
     * the set of namespace declarations that have already been serialized
     */
    protected NamespaceStack namespaces;

    /**
     * The jcr:primaryType property name (allowed for session-local prefix mappings)
     */
    protected final String jcrPrimaryType;
    /**
     * The jcr:mixinTypes property name (allowed for session-local prefix mappings)
     */
    protected final String jcrMixinTypes;
    /**
     * The jcr:uuid property name (allowed for session-local prefix mappings)
     */
    protected final String jcrUUID;
    /**
     * The jcr:root node name (allowed for session-local prefix mappings)
     */
    protected final String jcrRoot;
    /**
     * The jcr:xmltext node name (allowed for session-local prefix mappings)
     */
    protected final String jcrXMLText;
    /**
     * The jcr:xmlCharacters property name (allowed for session-local prefix mappings)
     */
    protected final String jcrXMLCharacters;

    /**
     * Constructor
     *
     * @param node           the node state which should be serialized
     * @param noRecurse      if true, only <code>node</code> and its properties will
     *                       be serialized; otherwise the entire hierarchy starting with
     *                       <code>node</code> will be serialized.
     * @param skipBinary     flag governing whether binary properties are to be serialized.
     * @param contentHandler the content handler to feed the SAX events to
     * @throws RepositoryException if an error occurs
     */
    protected AbstractSAXEventGenerator(Node node, boolean noRecurse,
                                        boolean skipBinary,
                                        ContentHandler contentHandler)
            throws RepositoryException {
        startNode = node;
        session = node.getSession();
        nsResolver = new SessionNamespaceResolver(session);

        this.contentHandler = contentHandler;
        this.skipBinary = skipBinary;
        this.noRecurse = noRecurse;
        // start with an empty set of known prefixes
        this.namespaces = new NamespaceStack(null);

        resolver = new DefaultNamePathResolver(nsResolver);

        // resolve the names of some wellknown properties
        // allowing for session-local prefix mappings
        try {
            jcrPrimaryType = resolver.getJCRName(NameConstants.JCR_PRIMARYTYPE);
            jcrMixinTypes = resolver.getJCRName(NameConstants.JCR_MIXINTYPES);
            jcrUUID = resolver.getJCRName(NameConstants.JCR_UUID);
            jcrRoot = resolver.getJCRName(NameConstants.JCR_ROOT);
            jcrXMLText = resolver.getJCRName(NameConstants.JCR_XMLTEXT);
            jcrXMLCharacters = resolver.getJCRName(NameConstants.JCR_XMLCHARACTERS);
        } catch (NamespaceException e) {
            // should never get here...
            String msg = "internal error: failed to resolve namespace mappings";
            log.error(msg, e);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * Serializes the hierarchy of nodes and properties.
     *
     * @throws RepositoryException if an error occurs while traversing the hierarchy
     * @throws SAXException        if an error occured while feeding the events
     *                             to the content handler
     */
    public void serialize() throws RepositoryException, SAXException {
        // start document and declare namespaces
        contentHandler.startDocument();
        startNamespaceDeclarations();

        // serialize node and subtree
        process(startNode, 0);

        // clear namespace declarations and end document
        endNamespaceDeclarations();
        contentHandler.endDocument();
    }

    /**
     * @throws RepositoryException
     * @throws SAXException
     */
    protected void startNamespaceDeclarations()
            throws RepositoryException, SAXException {
        // start namespace declarations
        String[] prefixes = session.getNamespacePrefixes();
        for (int i = 0; i < prefixes.length; i++) {
            String prefix = prefixes[i];
            if (Name.NS_XML_PREFIX.equals(prefix)) {
                // skip 'xml' prefix as this would be an illegal namespace declaration
                continue;
            }
            String uri = session.getNamespaceURI(prefix);
            contentHandler.startPrefixMapping(prefix, uri);
        }
    }

    /**
     * @throws RepositoryException
     * @throws SAXException
     */
    protected void endNamespaceDeclarations()
            throws RepositoryException, SAXException {
        // end namespace declarations
        String[] prefixes = session.getNamespacePrefixes();
        for (int i = 0; i < prefixes.length; i++) {
            String prefix = prefixes[i];
            if (Name.NS_XML_PREFIX.equals(prefix)) {
                // skip 'xml' prefix as this would be an illegal namespace declaration
                continue;
            }
            contentHandler.endPrefixMapping(prefix);
        }
    }

    /**
     * Adds explicit <code>xmlns:prefix="uri"</code> attributes to the
     * XML element as required (e.g., normally just on the root
     * element). The effect is the same as setting the
     * "<code>http://xml.org/sax/features/namespace-prefixes</code>"
     * property on an SAX parser.
     *
     * @param level level of the current XML element
     * @param attributes attributes of the current XML element
     * @throws RepositoryException on a repository error
     */
    protected void addNamespacePrefixes(int level, AttributesImpl attributes)
            throws RepositoryException {
        String[] prefixes = session.getNamespacePrefixes();
        NamespaceStack newNamespaces = null;

        for (int i = 0; i < prefixes.length; i++) {
            String prefix = prefixes[i];

            if (prefix.length() > 0
                    && !Name.NS_XML_PREFIX.equals(prefix)) {
                String uri = session.getNamespaceURI(prefix);

                // get the matching namespace from previous declarations
                String mappedToNs = this.namespaces.getNamespaceURI(prefix);

                if (!uri.equals(mappedToNs)) {
                    // when not the same, add a declaration
                    attributes.addAttribute(
                        Name.NS_XMLNS_URI,
                        prefix,
                        Name.NS_XMLNS_PREFIX + ":" + prefix,
                        "CDATA",
                        uri);

                    if (newNamespaces == null) {
                        // replace current namespace stack when needed
                        newNamespaces = new NamespaceStack(this.namespaces);
                        this.namespaces = newNamespaces;
                    }

                    // remember the new declaration
                    newNamespaces.setNamespacePrefix(prefix, uri);
                }
            }
        }
    }

    /**
     * @param node
     * @param level
     * @throws RepositoryException
     * @throws SAXException
     */
    protected void process(Node node, int level)
            throws RepositoryException, SAXException {
        // enter node
        entering(node, level);

        // enter properties
        enteringProperties(node, level);

        // Collect all properties (and sort them, see JCR-1084)
        SortedMap properties = new TreeMap();
        PropertyIterator propIter = node.getProperties();
        while (propIter.hasNext()) {
            Property property = propIter.nextProperty();
            properties.put(property.getName(), property);
        }

        // serialize jcr:primaryType, jcr:mixinTypes & jcr:uuid first:
        if (properties.containsKey(jcrPrimaryType)) {
            process((Property) properties.remove(jcrPrimaryType), level + 1);
        } else {
            throw new RepositoryException(
                    "Missing jcr:primaryType property: " + node.getPath());
        }
        if (properties.containsKey(jcrMixinTypes)) {
            process((Property) properties.remove(jcrMixinTypes), level + 1);
        }
        if (properties.containsKey(jcrUUID)) {
            process((Property) properties.remove(jcrUUID), level + 1);
        }

        // serialize remaining properties
        Iterator iterator = properties.values().iterator();
        while (iterator.hasNext()) {
            process((Property) iterator.next(), level + 1);
        }

        // leaving properties
        leavingProperties(node, level);

        if (!noRecurse) {
            // child nodes
            NodeIterator nodeIter = node.getNodes();
            while (nodeIter.hasNext()) {
                // recurse
                Node childNode = nodeIter.nextNode();
                // remember the current namespace declarations
                NamespaceStack previousNamespaces = this.namespaces;

                process(childNode, level + 1);

                // restore the effective namespace declarations
                // (from before visiting the child node)
                this.namespaces = previousNamespaces;
            }
        }

        // leaving node
        leaving(node, level);
    }

    /**
     * @param prop
     * @param level
     * @throws RepositoryException
     * @throws SAXException
     */
    protected void process(Property prop, int level)
            throws RepositoryException, SAXException {
        // serialize property
        entering(prop, level);
        leaving(prop, level);
    }

    /**
     * @param node
     * @param level
     * @throws RepositoryException
     * @throws SAXException
     */
    protected abstract void entering(Node node, int level)
            throws RepositoryException, SAXException;

    /**
     * @param node
     * @param level
     * @throws RepositoryException
     * @throws SAXException
     */
    protected abstract void enteringProperties(Node node, int level)
            throws RepositoryException, SAXException;

    /**
     * @param node
     * @param level
     * @throws RepositoryException
     * @throws SAXException
     */
    protected abstract void leavingProperties(Node node, int level)
            throws RepositoryException, SAXException;

    /**
     * @param node
     * @param level
     * @throws RepositoryException
     * @throws SAXException
     */
    protected abstract void leaving(Node node, int level)
            throws RepositoryException, SAXException;

    /**
     * @param prop
     * @param level
     * @throws RepositoryException
     * @throws SAXException
     */
    protected abstract void entering(Property prop, int level)
            throws RepositoryException, SAXException;

    /**
     * @param prop
     * @param level
     * @throws RepositoryException
     * @throws SAXException
     */
    protected abstract void leaving(Property prop, int level)
            throws RepositoryException, SAXException;

    /**
     * Implements a simple stack of namespace declarations.
     */
    private static class NamespaceStack {

        /**
         * Parent stack (may be <code>null</code>)
         */
        private final NamespaceStack parent;

        /**
         * Local namespace declarations.
         */
        private final Map namespaces;

        /**
         * Instantiate a new stack
         *
         * @param parent parent stack (may be <code>null</code> for the initial stack)
         */
        public NamespaceStack(NamespaceStack parent) {
            this.parent = parent;
            this.namespaces = new HashMap();
        }

        /**
         * Obtain namespace URI for a prefix
         *
         * @param prefix prefix
         * @return namespace URI (or <code>null</code> when unknown)
         */
        public String getNamespaceURI(String prefix) {
            String namespace = (String) namespaces.get(prefix);
            if (namespace != null) {
                // found in this element, return right away
                return namespace;
            } else if (parent != null) {
                // ask parent, when present
                return parent.getNamespaceURI(prefix);
            } else {
                return null;
            }
        }

        /**
         * Add a new prefix mapping
         *
         * @param prefix namespace prefix
         * @param uri namespace URI
         */
        public void setNamespacePrefix(String prefix, String uri) {
            namespaces.put(prefix, uri);
        }

    }

}
