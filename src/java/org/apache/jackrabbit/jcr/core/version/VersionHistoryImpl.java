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
package org.apache.jackrabbit.jcr.core.version;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.jcr.core.*;
import org.apache.jackrabbit.jcr.core.state.NodeState;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeDef;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import java.util.HashMap;

/**
 * This Class implements a version history.
 *
 * @author Tobias Strasser
 * @version $Revision: 1.7 $, $Date: 2004/09/14 08:50:07 $
 */
public class VersionHistoryImpl extends NodeImpl implements VersionHistory {

    /**
     * default logger
     */
    private static Logger log = Logger.getLogger(VersionHistoryImpl.class);

    /**
     * the name of the 'jcr:rootVersion' node
     */
    public static final QName NODENAME_ROOTVERSION = new QName(NamespaceRegistryImpl.NS_JCR_URI, "rootVersion");

    /**
     * the cache of the version labels (initialized on first usage)
     * key = version label (String)
     * value = version name (String)
     */
    private HashMap labelCache = null;

    /**
     * Creates a new VersionHistory object for the given node.
     * <p/>
     * Currently, it just casts the given node into a VersionHistoryImpl, but
     * this might change, if in the future spec the VersionHistory does not
     * extend Node anymore.
     *
     * @param node
     * @return a VersionHistory
     */
    protected static VersionHistoryImpl create(NodeImpl node) {
	return (VersionHistoryImpl) node;
    }

    /**
     * Creates a new VersionHistoryImpl object. This is only called by the
     * item manager, when creating new node instances.
     *
     * @see org.apache.jackrabbit.jcr.core.ItemManager#createNodeInstance(org.apache.jackrabbit.jcr.core.state.NodeState, javax.jcr.nodetype.NodeDef)
     */
    public VersionHistoryImpl(ItemManager itemMgr, SessionImpl session, NodeId id,
			      NodeState state, NodeDef definition,
			      ItemLifeCycleListener[] listeners)
	    throws RepositoryException {
	super(itemMgr, session, id, state, definition, listeners);
    }

    /**
     * @see VersionHistory#getRootVersion()
     */
    public Version getRootVersion() throws RepositoryException {
	return (Version) getNode(NODENAME_ROOTVERSION);
    }

    /**
     * @see VersionHistory#getAllVersions()
     */
    public VersionIterator getAllVersions() throws RepositoryException {
	return new VersionIteratorImpl(getRootVersion());
    }

    /**
     * @see VersionHistory#getVersion(java.lang.String)
     */
    public Version getVersion(String versionName) throws RepositoryException {
	return (Version) getNode(versionName);
    }

    /**
     * @see VersionHistory#getVersionByLabel(java.lang.String)
     */
    public Version getVersionByLabel(String label) throws RepositoryException {
	initLabelCache();
	return labelCache.containsKey(label)
		? (Version) getNode((String) labelCache.get(label))
		: null;
    }

    /**
     * Removes the indicated version from this VersionHistory. If the specified
     * vesion does not exist, if it specifies the root version or if it is
     * referenced by any node e.g. as base version, a VersionException is thrown.
     * <p/>
     * all successors of the removed version become successors of the
     * predecessors of the removed version and vice versa. then, the entire
     * version node and all its subnodes are removed.
     *
     * @param versionName
     * @throws RepositoryException todo: add to spec
     */
    public void removeVersion(String versionName) throws RepositoryException {
	VersionImpl v = (VersionImpl) getVersion(versionName);
	if (v.isSame(getRootVersion())) {
	    String msg = "Removal of " + versionName + " not allowed.";
	    log.error(msg);
	    throw new VersionException(msg);
	}
	// check if any references to this node exist outside the version graph
	// todo: check this

	// detach from the version graph
	v.internalDetach();

	// and remove from history
	remove(versionName);
	save();
    }

    /**
     * Initializes the label cache
     *
     * @throws RepositoryException
     */
    private void initLabelCache() throws RepositoryException {
	if (labelCache != null) {
	    return;
	}
	labelCache = new HashMap();
	NodeIterator iter = getNodes();
	while (iter.hasNext()) {
	    // assuming all subnodes are 'versions'
	    Version v = (Version) iter.nextNode();
	    String[] labels = v.getVersionLabels();
	    for (int i = 0; i < labels.length; i++) {
		if (labelCache.containsKey(labels[i])) {
		    log.error("Label " + labels[i] + " duplicate: in " + v.getName() + " and in " + labelCache.get(labels[i]));
		} else {
		    labelCache.put(labels[i], v.getName());
		}
	    }
	}
    }

    /**
     * Adds a label to a version
     * @param version
     * @param label
     * @throws RepositoryException
     */
    public void addVersionLabel(Version version, String label) throws RepositoryException {
	initLabelCache();
	String vname = (String) labelCache.get(label);
	if (vname == null) {
	    // if not exists, add
	    labelCache.put(label, version.getName());
	    ((VersionImpl) version).internalAddVersionLabel(label);
	} else if (vname.equals(version.getName())) {
	    // if already defined to this version, ignore
	} else {
	    // already defined eslwhere, throw
	    throw new RepositoryException("Version label " + label + " already defined for version " + vname);
	}
    }

    /**
     * Removes the label from the respective version
     *
     * @param label
     * @throws RepositoryException if the label does not exist
     */
    public void removeVersionLabel(String label) throws RepositoryException {
	initLabelCache();
	String name = (String) labelCache.remove(label);
	if (name == null) {
	    throw new RepositoryException("Version label " + label + " is not in version history.");
	}
	((VersionImpl) getVersion(name)).internalRemoveVersionLabel(label);
    }
}


