/*
 * Copyright 2002-2004 The Apache Software Foundation.
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
import org.apache.jackrabbit.jcr.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.jcr.util.uuid.UUID;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

/**
 * This Class implements...
 *
 * @author Tobias Strasser
 * @version $Revision: 1.11 $, $Date: 2004/09/14 12:49:00 $
 */
public class VersionManager {

    private static Logger log = Logger.getLogger(VersionManager.class);

    // root path for version storage
    public static final QName VERSION_HISTORY_ROOT_NAME =
	    new QName(NamespaceRegistryImpl.NS_JCR_URI, "versionStorage");

    // the system session for the versioning
    private final SessionImpl session;

    /**
     * the root node of the version histories
     */
    private final NodeImpl historyRoot;

    /**
     * Creates a new VersionManager.
     *
     * @param session
     * @throws RepositoryException
     */
    public VersionManager(SessionImpl session) throws RepositoryException {
	this.session = session;

	// check for versionhistory root
	NodeImpl systemRoot = ((RepositoryImpl) session.getRepository()).getSystemRootNode(session);
	if (!systemRoot.hasNode(VERSION_HISTORY_ROOT_NAME)) {
	    // if not exist, create
	    systemRoot.addNode(VERSION_HISTORY_ROOT_NAME, NodeTypeRegistry.NT_UNSTRUCTURED);
	    systemRoot.save();
	}
	historyRoot = systemRoot.getNode(VERSION_HISTORY_ROOT_NAME);
    }

    /**
     * Creates a new Version History and returns the UUID of it.
     *
     * @param node the node for which the version history is to be initialized
     * @return the UUID of the new version history node
     * @throws RepositoryException
     */
    public VersionHistory createVersionHistory(NodeImpl node)
	    throws RepositoryException {

	// check if history already exists
	QName historyNodeName = new QName(NamespaceRegistryImpl.NS_DEFAULT_URI, node.getUUID());
	if (historyRoot.hasNode(historyNodeName)) {
	    //throw new RepositoryException("Unable to initialize version history. Already exists");
	    historyRoot.getNode(historyNodeName).remove(".");
	}

	// create new history node
	VersionHistoryImpl vh =	VersionHistoryImpl.create(historyRoot.addNode(historyNodeName,
		NodeTypeRegistry.NT_VERSION_HISTORY));

	// and initialize the root version
	((VersionImpl) vh.getRootVersion()).initFrozenState(node);

	// save new history
	historyRoot.save();

	// must aquire version history with the node's session
	return VersionHistoryImpl.create((NodeImpl) node.getSession().getNodeByUUID(vh.getUUID()));
    }

    /**
     * Returns the base version of the given node. assuming mix:versionable
     *
     * @param node
     * @return
     * @throws RepositoryException
     */
    public Version getBaseVersion(NodeImpl node) throws RepositoryException {
	return (Version) node.getSession().getNodeByUUID(node.getProperty(VersionImpl.PROPNAME_BASE_VERSION).getString());
    }

    /**
     * Returns the version history for the given node. assuming mix:versionable
     * and version history set in property
     *
     * @param node
     * @return
     * @throws RepositoryException
     */
    public VersionHistoryImpl getVersionHistory(NodeImpl node) throws RepositoryException {
	return VersionHistoryImpl.create((NodeImpl) node.getSession().getNodeByUUID(node.getProperty(VersionImpl.PROPNAME_VERSION_HISTORY).getString()));
    }

    /**
     * Checks in a node
     *
     * @param node
     * @return
     * @throws RepositoryException
     * @see Node#checkin()
     */
    public Version checkin(NodeImpl node) throws RepositoryException {
	// assuming node is versionable and checkout (check in nodeimpl)
	// To create a new version of a versionable node N, the client calls N.checkin.
	// This causes the following series of events:

	// 0. resolve the predecessors
	Value[] values = node.getProperty(VersionImpl.PROPNAME_PREDECESSORS).getValues();
	VersionImpl[] preds = new VersionImpl[values.length];
	for (int i = 0; i < values.length; i++) {
	    preds[i] = (VersionImpl) node.getSession().getNodeByUUID(values[i].getString());
	}

	// 0.1 search a predecessor, suitable for generating the new name
	String versionName = null;
	int maxDots = Integer.MAX_VALUE;
	for (int i = 0; i < preds.length; i++) {
	    // take the first pred. without a successor
	    if (preds[i].getSuccessors().length == 0) {
		versionName = preds[i].getName();
		// need to count the dots
		int pos = -1;
		int numDots = 0;
		while (versionName.indexOf('.', pos + 1) >= 0) {
		    pos = versionName.indexOf('.', pos + 1);
		    numDots++;
		}
		if (numDots < maxDots) {
		    maxDots = numDots;
		    versionName = pos < 0 ? "1.0" : versionName.substring(0, pos + 1) + (Integer.parseInt(versionName.substring(pos + 1)) + 1);
		}
		break;
	    }
	}
	// if no empty found, generate new name
	VersionHistoryImpl vh = getVersionHistory(node);
	if (versionName == null) {
	    versionName = preds[0].getName();
	    do {
		versionName += ".1";
	    } while (vh.hasNode(versionName));
	}

	try {
	    // 1. A new nt:version node V is created and added as a child node to VH,
	    //    the nt:versionHistory pointed to by N’s jcr:versionHistory property.
	    VersionImpl v = (VersionImpl) vh.addNode(versionName,
		    NodeTypeRegistry.NT_VERSION.toJCRName(session.getNamespaceResolver()));

	    // 3. N’s base version is changed to V by altering N’s jcr:baseVersion
	    //    property to point to V.
	    //   (will be done in the nodeimpl)

	    // 4. N’s checked-in/checked-out status is changed to checked-in by
	    //    changing its jcr:isCheckedOut property to false.
	    //    (will be done in NodeImpl)

	    // 5. The state of N is recorded in V by storing information about
	    //    N’s child items (properties or child nodes) to V, as prescribed by
	    //    the OnParentVersion attribute of each of N’s child items.
	    //    See 7.2.8, below, for the details. The jcr:primaryType,
	    //    jcr:mixinTypes and jcr:uuid properties of N are copied over to V
	    //    but renamed to jcr:frozenPrimaryType, jcr:frozenMixinTypes and
	    //    jcr:frozenUUID to avoid conflict with V's own properties with these names.
	    v.createFrozenState(node);

	    // 2. N’s current jcr:predecessors property is copied to V, and N’s
	    //    jcr:predecessors property is then set to null.  A reference to V
	    //    is then added to the jcr:successors property of each of the versions
	    //    identified in V’s jcr:predecessors property.
	    InternalValue[] ivPreds = new InternalValue[preds.length];
	    for (int i = 0; i < preds.length; i++) {
		ivPreds[i] = InternalValue.create(new UUID(preds[i].getUUID()));
		preds[i].internalAddSuccessor(v);
	    }
	    v.internalSetProperty(VersionImpl.PROPNAME_PREDECESSORS, ivPreds);

	    // 6. V is given a name, sometimes based upon the name of V’s predecessor.
	    //    For example, an increment from “1.5” to “1.6”.
	    // (is done before)
	    vh.save();
	    return v;
	} catch (RepositoryException e) {
	    log.error("Aborting checkin. Error while creating version: " + e.toString());
	    vh.refresh(false);
	    throw e;
	} catch (NoPrefixDeclaredException npde) {
	    String msg = "Aborting checkin. Error while creating version: " + npde.toString();
	    log.error(msg, npde);
	    vh.refresh(false);
	    throw new RepositoryException(msg, npde);
	}
    }

}
