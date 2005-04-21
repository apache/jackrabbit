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
package org.apache.jackrabbit.core.xml;

import org.apache.jackrabbit.core.Constants;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.PropertyImpl;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.log4j.Logger;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

/**
 * <code>AbstractSAXEventGenerator</code> serves as the base class for
 * <code>SysViewSAXEventGenerator</code> and <code>DocViewSAXEventGenerator</code>
 * <p/>
 * It traverses a tree of <code>Node</code> & <code>Property</code>
 * instances, and calls the abstract methods
 * <ul>
 * <li><code>{@link #entering(NodeImpl, int)}</code></li>
 * <li><code>{@link #enteringProperties(NodeImpl, int)}</code></li>
 * <li><code>{@link #leavingProperties(NodeImpl, int)}</code></li>
 * <li><code>{@link #leaving(NodeImpl, int)}</code></li>
 * <li><code>{@link #entering(PropertyImpl, int)}</code></li>
 * <li><code>{@link #leaving(PropertyImpl, int)}</code></li>
 * </ul>
 * for every item it encounters.
 */
abstract class AbstractSAXEventGenerator implements Constants {

    private static Logger log = Logger.getLogger(AbstractSAXEventGenerator.class);

    protected final SessionImpl session;
    protected final ContentHandler contentHandler;
    protected final NodeImpl startNode;
    protected final boolean skipBinary;
    protected final boolean noRecurse;

    /**
     * Constructor
     *
     * @param node           the node state which should be serialized
     * @param noRecurse      if true, only <code>node</code> and its properties will
     *                       be serialized; otherwise the entire hierarchy starting with
     *                       <code>node</code> will be serialized.
     * @param skipBinary     flag governing whether binary properties are to be serialized.
     * @param session        the session to be used for resolving namespace mappings
     * @param contentHandler the content handler to feed the SAX events to
     */
    protected AbstractSAXEventGenerator(NodeImpl node, boolean noRecurse,
                                        boolean skipBinary,
                                        SessionImpl session,
                                        ContentHandler contentHandler) {
        this.session = session;
        startNode = node;
        this.contentHandler = contentHandler;
        this.skipBinary = skipBinary;
        this.noRecurse = noRecurse;
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
            if (NS_XML_PREFIX.equals(prefix)) {
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
            if (NS_XML_PREFIX.equals(prefix)) {
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
    protected void process(NodeImpl node, int level)
            throws RepositoryException, SAXException {
        // enter node
        entering(node, level);

        // enter properties
        enteringProperties(node, level);

        // serialize jcr:primaryType, jcr:mixinTypes & jcr:uuid first:
        // jcr:primaryType
        if (node.hasProperty(JCR_PRIMARYTYPE)) {
            process(node.getProperty(JCR_PRIMARYTYPE), level + 1);
        } else {
            String msg = "internal error: missing jcr:primaryType property on node " + node.safeGetJCRPath();
            log.debug(msg);
            throw new RepositoryException(msg);
        }
        // jcr:mixinTypes
        if (node.hasProperty(JCR_MIXINTYPES)) {
            process(node.getProperty(JCR_MIXINTYPES), level + 1);
        }
        // jcr:uuid
        if (node.hasProperty(JCR_UUID)) {
            process(node.getProperty(JCR_UUID), level + 1);
        }

        // serialize remaining properties
        PropertyIterator propIter = node.getProperties();
        while (propIter.hasNext()) {
            PropertyImpl prop = (PropertyImpl) propIter.nextProperty();
            QName name = prop.getQName();
            if (JCR_PRIMARYTYPE.equals(name)
                    || JCR_MIXINTYPES.equals(name)
                    || JCR_UUID.equals(name)) {
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
                NodeImpl childNode = (NodeImpl) nodeIter.nextNode();
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
    protected void process(PropertyImpl prop, int level)
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
    protected abstract void entering(NodeImpl node, int level)
            throws RepositoryException, SAXException;

    /**
     * @param node
     * @param level
     * @throws RepositoryException
     * @throws SAXException
     */
    protected abstract void enteringProperties(NodeImpl node, int level)
            throws RepositoryException, SAXException;

    /**
     * @param node
     * @param level
     * @throws RepositoryException
     * @throws SAXException
     */
    protected abstract void leavingProperties(NodeImpl node, int level)
            throws RepositoryException, SAXException;

    /**
     * @param node
     * @param level
     * @throws RepositoryException
     * @throws SAXException
     */
    protected abstract void leaving(NodeImpl node, int level)
            throws RepositoryException, SAXException;

    /**
     * @param prop
     * @param level
     * @throws RepositoryException
     * @throws SAXException
     */
    protected abstract void entering(PropertyImpl prop, int level)
            throws RepositoryException, SAXException;

    /**
     * @param prop
     * @param level
     * @throws RepositoryException
     * @throws SAXException
     */
    protected abstract void leaving(PropertyImpl prop, int level)
            throws RepositoryException, SAXException;
}
