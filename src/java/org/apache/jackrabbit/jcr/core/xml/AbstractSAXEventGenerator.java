/*
 * Copyright 2004 The Apache Software Foundation.
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
package org.apache.jackrabbit.jcr.core.xml;

import org.apache.jackrabbit.jcr.core.state.NodeState;
import org.apache.jackrabbit.jcr.core.state.PropertyState;
import org.apache.jackrabbit.jcr.core.state.ItemStateProvider;
import org.apache.jackrabbit.jcr.core.state.ItemStateException;
import org.apache.jackrabbit.jcr.core.*;
import org.apache.log4j.Logger;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import javax.jcr.RepositoryException;
import javax.jcr.access.Permission;
import java.util.Iterator;

/**
 * <code>AbstractSAXEventGenerator</code> serves as the base class for
 * <code>SysViewSAXEventGenerator</code> and <code>DocViewSAXEventGenerator</code>
 * <p/>
 * It traverses a tree of <code>NodeState</code> & <code>PropertyState</code>
 * instances, and calls the abstract methods
 * <ul>
 * <li><code>{@link #entering(NodeState, org.apache.jackrabbit.jcr.core.QName, int)}</code></li>
 * <li><code>{@link #enteringProperties(NodeState, org.apache.jackrabbit.jcr.core.QName, int)}</code></li>
 * <li><code>{@link #leavingProperties(NodeState, org.apache.jackrabbit.jcr.core.QName, int)}</code></li>
 * <li><code>{@link #leaving(NodeState, org.apache.jackrabbit.jcr.core.QName, int)}</code></li>
 * <li><code>{@link #entering(PropertyState, int)}</code></li>
 * <li><code>{@link #leaving(PropertyState, int)}</code></li>
 * </ul>
 * for every item state that is granted read access.
 *
 * @author Stefan Guggisberg
 * @version $Revision: 1.1 $, $Date: 2004/08/24 09:30:55 $
 */
abstract class AbstractSAXEventGenerator {

    private static Logger log = Logger.getLogger(AbstractSAXEventGenerator.class);

    protected final ItemStateProvider stateProvider;
    protected final NamespaceRegistryImpl nsReg;
    protected final AccessManagerImpl accessMgr;
    protected final ContentHandler contentHandler;
    protected final NodeState startNodeState;
    protected final QName startNodeName;
    protected final boolean binaryAsLink;
    protected final boolean noRecurse;

    // dummy name for root node (jcr:root)
    public static final QName NODENAME_ROOT =
	    new QName(NamespaceRegistryImpl.NS_JCR_URI, "root");
    // jcr:uuid
    protected static final QName PROPNAME_UUID = ItemImpl.PROPNAME_UUID;
    // jcr:primaryType
    protected static final QName PROPNAME_PRIMARYTYPE = ItemImpl.PROPNAME_PRIMARYTYPE;
    // jcr:mixinTypes
    protected static final QName PROPNAME_MIXINTYPES = ItemImpl.PROPNAME_MIXINTYPES;

    /**
     * Constructor
     *
     * @param nodeState      the node state which should be serialized
     * @param noRecurse      if true, only <code>nodeState</code> and its properties will
     *                       be serialized; otherwise the entire hierarchy starting with
     *                       <code>nodeState</code> will be serialized.
     * @param binaryAsLink   specifies if binary properties are turned into links
     * @param nodeName       name of the node to be serialized
     * @param stateProvider  item state provider for retrieving child item state
     * @param nsReg          the namespace registry to be used for namespace declarations
     * @param accessMgr      the access manager
     * @param contentHandler the content handler to feed the SAX events to
     */
    protected AbstractSAXEventGenerator(NodeState nodeState, QName nodeName,
					boolean noRecurse, boolean binaryAsLink,
					ItemStateProvider stateProvider,
					NamespaceRegistryImpl nsReg,
					AccessManagerImpl accessMgr,
					ContentHandler contentHandler) {
	this.stateProvider = stateProvider;
	this.nsReg = nsReg;
	this.accessMgr = accessMgr;
	startNodeState = nodeState;
	startNodeName = nodeName;
	this.contentHandler = contentHandler;
	this.binaryAsLink = binaryAsLink;
	this.noRecurse = noRecurse;
    }

    /**
     * Serializes the hierarchy of nodes and properties.
     *
     * @throws javax.jcr.RepositoryException if an error occurs while traversing the hierarchy
     * @throws org.xml.sax.SAXException      if an error occured while feeding the events to the content handler
     */
    public void serialize() throws RepositoryException, SAXException {
	contentHandler.startDocument();
	// namespace declarations
	documentPrefixMappings();
	// start serializing node state(s)
	process(startNodeState, startNodeName, 0);

	contentHandler.endDocument();
    }

    /**
     *
     * @throws RepositoryException
     * @throws SAXException
     */
    protected void documentPrefixMappings() throws RepositoryException, SAXException {
	// namespace declarations
	String[] prefixes = nsReg.getPrefixes();
	for (int i = 0; i < prefixes.length; i++) {
	    String prefix = prefixes[i];
	    String uri = nsReg.getURI(prefix);
	    contentHandler.startPrefixMapping(prefix, uri);
	}
    }

    /**
     * @param nodeState
     * @param level
     * @throws RepositoryException
     * @throws SAXException
     */
    protected void process(NodeState nodeState, QName nodeName, int level)
	    throws RepositoryException, SAXException {

	// enter node
	entering(nodeState, nodeName, level);

	// enter properties
	enteringProperties(nodeState, nodeName, level);

	// serialize jcr:primaryType, jcr:mixinTypes & jcr:uuid first:
	// jcr:primaryType
	if (nodeState.hasPropertyEntry(PROPNAME_PRIMARYTYPE)) {
	    process(nodeState.getPropertyEntry(PROPNAME_PRIMARYTYPE), nodeState.getUUID(), level + 1);
	} else {
	    String msg = "internal error: missing jcr:primaryType property on node " + nodeState.getUUID();
	    log.error(msg);
	    throw new RepositoryException(msg);
	}
	// jcr:mixinTypes
	if (nodeState.hasPropertyEntry(PROPNAME_MIXINTYPES)) {
	    process(nodeState.getPropertyEntry(PROPNAME_MIXINTYPES), nodeState.getUUID(), level + 1);
	}
	// jcr:uuid
	if (nodeState.hasPropertyEntry(PROPNAME_UUID)) {
	    process(nodeState.getPropertyEntry(PROPNAME_UUID), nodeState.getUUID(), level + 1);
	}

	// serialize remaining properties
	Iterator iter = nodeState.getPropertyEntries().iterator();
	while (iter.hasNext()) {
	    NodeState.PropertyEntry pe = (NodeState.PropertyEntry) iter.next();
	    if (PROPNAME_PRIMARYTYPE.equals(pe.getName()) ||
		    PROPNAME_MIXINTYPES.equals(pe.getName()) ||
		    PROPNAME_UUID.equals(pe.getName())) {
		continue;
	    }
	    PropertyId propId = new PropertyId(nodeState.getUUID(), pe.getName());
	    // check read access
	    if (accessMgr.isGranted(propId, Permission.READ_ITEM)) {
		// serialize property
		process(pe, nodeState.getUUID(), level + 1);
	    }
	}

	// leaving properties
	leavingProperties(nodeState, nodeName, level);

	if (!noRecurse) {
	    // child nodes
	    iter = nodeState.getChildNodeEntries().iterator();
	    while (iter.hasNext()) {
		NodeState.ChildNodeEntry cne = (NodeState.ChildNodeEntry) iter.next();
		NodeId childId = new NodeId(cne.getUUID());
		// check read access
		if (accessMgr.isGranted(childId, Permission.READ_ITEM)) {
		    NodeState childState;
		    try {
			childState = (NodeState) stateProvider.getItemState(childId);
		    } catch (ItemStateException ise) {
			String msg = "internal error: failed to retrieve state of node " + childId;
			log.error(msg, ise);
			throw new RepositoryException(msg, ise);
		    }
		    // recurse
		    process(childState, cne.getName(), level + 1);
		}
	    }
	}

	// leaving node
	leaving(nodeState, nodeName, level);
    }

    /**
     *
     * @param propEntry
     * @param parentUUID
     * @param level
     * @throws RepositoryException
     * @throws SAXException
     */
    protected void process(NodeState.PropertyEntry propEntry, String parentUUID, int level)
	    throws RepositoryException, SAXException {
	PropertyId propId = new PropertyId(parentUUID, propEntry.getName());
	try {
	    PropertyState propState = (PropertyState) stateProvider.getItemState(propId);
	    // serialize property
	    entering(propState, level);
	    leaving(propState, level);
	} catch (ItemStateException ise) {
	    String msg = "internal error: failed to retrieve state of property " + propId;
	    log.error(msg, ise);
	    throw new RepositoryException(msg, ise);
	}
    }

    /**
     *
     * @param state
     * @param name
     * @param level
     * @throws RepositoryException
     * @throws SAXException
     */
    abstract protected void entering(NodeState state, QName name, int level)
	    throws RepositoryException, SAXException;

    /**
     *
     * @param state
     * @param name
     * @param level
     * @throws RepositoryException
     * @throws SAXException
     */
    abstract protected void enteringProperties(NodeState state, QName name, int level)
	    throws RepositoryException, SAXException;

    /**
     *
     * @param state
     * @param name
     * @param level
     * @throws RepositoryException
     * @throws SAXException
     */
    abstract protected void leavingProperties(NodeState state, QName name, int level)
	    throws RepositoryException, SAXException;

    /**
     *
     * @param state
     * @param name
     * @param level
     * @throws RepositoryException
     * @throws SAXException
     */
    abstract protected void leaving(NodeState state, QName name, int level)
	    throws RepositoryException, SAXException;

    /**
     *
     * @param state
     * @param level
     * @throws RepositoryException
     * @throws SAXException
     */
    abstract protected void entering(PropertyState state, int level)
	    throws RepositoryException, SAXException;

    /**
     *
     * @param state
     * @param level
     * @throws RepositoryException
     * @throws SAXException
     */
    abstract protected void leaving(PropertyState state, int level)
	    throws RepositoryException, SAXException;
}
