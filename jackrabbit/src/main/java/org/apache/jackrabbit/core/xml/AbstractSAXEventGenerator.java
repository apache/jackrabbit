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

import org.apache.jackrabbit.name.NameException;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.SessionNamespaceResolver;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

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

    /**
     * the content handler to feed the SAX events to
     */
    protected final ContentHandler contentHandler;

    protected final Node startNode;
    protected final boolean skipBinary;
    protected final boolean noRecurse;

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

        // resolve the names of some wellknown properties
        // allowing for session-local prefix mappings
        try {
            jcrPrimaryType = QName.JCR_PRIMARYTYPE.toJCRName(nsResolver);
            jcrMixinTypes = QName.JCR_MIXINTYPES.toJCRName(nsResolver);
            jcrUUID = QName.JCR_UUID.toJCRName(nsResolver);
            jcrRoot = QName.JCR_ROOT.toJCRName(nsResolver);
            jcrXMLText = QName.JCR_XMLTEXT.toJCRName(nsResolver);
            jcrXMLCharacters = QName.JCR_XMLCHARACTERS.toJCRName(nsResolver);
        } catch (NameException e) {
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
            if (QName.NS_XML_PREFIX.equals(prefix)) {
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
            if (QName.NS_XML_PREFIX.equals(prefix)) {
                // skip 'xml' prefix as this would be an illegal namespace declaration
                continue;
            }
            contentHandler.endPrefixMapping(prefix);
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

        // serialize jcr:primaryType, jcr:mixinTypes & jcr:uuid first:
        // jcr:primaryType
        if (node.hasProperty(jcrPrimaryType)) {
            process(node.getProperty(jcrPrimaryType), level + 1);
        } else {
            String msg = "internal error: missing jcr:primaryType property on node "
                    + node.getPath();
            log.debug(msg);
            throw new RepositoryException(msg);
        }
        // jcr:mixinTypes
        if (node.hasProperty(jcrMixinTypes)) {
            process(node.getProperty(jcrMixinTypes), level + 1);
        }
        // jcr:uuid
        if (node.hasProperty(jcrUUID)) {
            process(node.getProperty(jcrUUID), level + 1);
        }

        // serialize remaining properties
        PropertyIterator propIter = node.getProperties();
        while (propIter.hasNext()) {
            Property prop = propIter.nextProperty();
            String name = prop.getName();
            if (jcrPrimaryType.equals(name)
                    || jcrMixinTypes.equals(name)
                    || jcrUUID.equals(name)) {
                continue;
            }
            // serialize property
            process(prop, level + 1);
        }

        // leaving properties
        leavingProperties(node, level);

        if (!noRecurse) {
            // child nodes
            NodeIterator nodeIter = node.getNodes();
            while (nodeIter.hasNext()) {
                Node childNode = nodeIter.nextNode();
                // recurse
                process(childNode, level + 1);
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

}
