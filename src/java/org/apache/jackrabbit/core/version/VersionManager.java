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
package org.apache.jackrabbit.core.version;

import org.apache.jackrabbit.core.*;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.PersistentItemStateProvider;
import org.apache.jackrabbit.core.state.PersistentNodeState;
import org.apache.log4j.Logger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

/**
 * This Class provides general versioning functions.
 */
public class VersionManager {

    private static Logger log = Logger.getLogger(VersionManager.class);

    /**
     * root path for version storage
     */
    public static final QName VERSION_HISTORY_ROOT_NAME = new QName(NamespaceRegistryImpl.NS_JCR_URI, "versionStorage");

    /**
     * name of the 'jcr:frozenUUID' property
     */
    public static final QName PROPNAME_FROZEN_UUID = new QName(NamespaceRegistryImpl.NS_JCR_URI, "frozenUUID");

    /**
     * name of the 'jcr:frozenPrimaryType' property
     */
    public static final QName PROPNAME_FROZEN_PRIMARY_TYPE = new QName(NamespaceRegistryImpl.NS_JCR_URI, "frozenPrimaryType");

    /**
     * name of the 'jcr:frozenMixinTypes' property
     */
    public static final QName PROPNAME_FROZEN_MIXIN_TYPES = new QName(NamespaceRegistryImpl.NS_JCR_URI, "frozenMixinTypes");

    /**
     * name of the 'jcr:versionLabels' node
     */
    public static final QName NODENAME_VERSION_LABELS = new QName(NamespaceRegistryImpl.NS_JCR_URI, "versionLabels");

    /**
     * name of the 'jcr:frozen' property
     */
    public static final QName NODENAME_FROZEN = new QName(NamespaceRegistryImpl.NS_JCR_URI, "frozen");

    /**
     * name of the 'jcr:predecessors' property
     */
    public static final QName PROPNAME_PREDECESSORS = new QName(NamespaceRegistryImpl.NS_JCR_URI, "predecessors");

    /**
     * name of the 'jcr:successors' property
     */
    public static final QName PROPNAME_SUCCESSORS = new QName(NamespaceRegistryImpl.NS_JCR_URI, "successors");

    /**
     * name of the 'jcr:name' property
     */
    public static final QName PROPNAME_NAME = new QName(NamespaceRegistryImpl.NS_JCR_URI, "name");

    /**
     * name of the 'jcr:version' property
     */
    public static final QName PROPNAME_VERSION = new QName(NamespaceRegistryImpl.NS_JCR_URI, "version");

    /**
     * name of the 'jcr:isCheckedOut' property
     */
    public static final QName PROPNAME_IS_CHECKED_OUT = new QName(NamespaceRegistryImpl.NS_JCR_URI, "isCheckedOut");

    /**
     * name of the 'jcr:versionHistory' property
     */
    public static final QName PROPNAME_VERSION_HISTORY = new QName(NamespaceRegistryImpl.NS_JCR_URI, "versionHistory");

    /**
     * name of the 'jcr:baseVersion' property
     */
    public static final QName PROPNAME_BASE_VERSION = new QName(NamespaceRegistryImpl.NS_JCR_URI, "baseVersion");

    /**
     * name of the 'jcr:created' property
     */
    public static final QName PROPNAME_CREATED = new QName(NamespaceRegistryImpl.NS_JCR_URI, "created");

    /**
     * the name of the 'jcr:rootVersion' node
     */
    public static final QName NODENAME_ROOTVERSION = new QName(NamespaceRegistryImpl.NS_JCR_URI, "rootVersion");

    /**
     * the root node of the version histories
     */
    private final PersistentNode historyRoot;

    /**
     * the state manager for the version storage
     */
    private PersistentItemStateProvider stateMgr;

    /**
     * the nodetype manager for the version storage
     */
    private NodeTypeManagerImpl ntMgr;

    /**
     * Creates a new VersionManager.
     *
     * @param session
     * @throws RepositoryException
     */
    public VersionManager(SessionImpl session) throws RepositoryException {
        this.stateMgr = ((WorkspaceImpl) session.getWorkspace()).getPersistentStateManager();
        this.ntMgr = session.getNodeTypeManager();

        // check for versionhistory root
        NodeImpl systemRoot = ((RepositoryImpl) session.getRepository()).getSystemRootNode(session);
        if (!systemRoot.hasNode(VERSION_HISTORY_ROOT_NAME)) {
            // if not exist, create
            systemRoot.addNode(VERSION_HISTORY_ROOT_NAME, NodeTypeRegistry.NT_UNSTRUCTURED);
            systemRoot.save();
        }

        try {
            PersistentNodeState nodeState = (PersistentNodeState) stateMgr.getItemState(new NodeId(systemRoot.getNode(VERSION_HISTORY_ROOT_NAME).internalGetUUID()));
            historyRoot = new PersistentNode(stateMgr, ntMgr, nodeState);
        } catch (ItemStateException e) {
            throw new RepositoryException("Unable to initialize VersionManager: " + e.toString());
        }
    }

    /**
     * Creates a new Version History..
     *
     * @param node the node for which the version history is to be initialized
     * @return the newly created version history.
     * @throws RepositoryException
     */
    public VersionHistory createVersionHistory(NodeImpl node)
            throws RepositoryException {

        // create deep path
        String uuid = node.getUUID();
        PersistentNode parent = historyRoot;
        QName historyNodeName = new QName(NamespaceRegistryImpl.NS_DEFAULT_URI, uuid.substring(0, 2));
        if (!parent.hasNode(historyNodeName)) {
            parent = parent.addNode(historyNodeName, NodeTypeRegistry.NT_UNSTRUCTURED);
        } else {
            parent = parent.getNode(historyNodeName, 1);
        }
        historyNodeName = new QName(NamespaceRegistryImpl.NS_DEFAULT_URI, uuid.substring(2, 4));
        if (!parent.hasNode(historyNodeName)) {
            parent = parent.addNode(historyNodeName, NodeTypeRegistry.NT_UNSTRUCTURED);
        } else {
            parent = parent.getNode(historyNodeName, 1);
        }

        historyNodeName = new QName(NamespaceRegistryImpl.NS_DEFAULT_URI, uuid.substring(4));
        if (parent.hasNode(historyNodeName)) {
            parent.removeNode(historyNodeName);
        }
        historyRoot.store();

        // create new history node in the persistent state
        InternalVersionHistory history = InternalVersionHistory.create(parent, historyNodeName);
        return new VersionHistoryImpl(node.getSession(), history);
    }

    /**
     * Returns the base version of the given node. assuming mix:versionable
     *
     * @param node
     * @return
     * @throws RepositoryException
     */
    public Version getBaseVersion(NodeImpl node) throws RepositoryException {
        InternalVersionHistory history = getInternalVersionHistory(node);
        InternalVersion version = history.getVersion(node.getProperty(PROPNAME_BASE_VERSION).getString());
        return version == null ? null : new VersionImpl(node.getSession(), version);
    }

    /**
     * Returns the version history for the given node. assuming mix:versionable
     * and version history set in property
     *
     * @param node
     * @return
     * @throws RepositoryException
     */
    public VersionHistory getVersionHistory(NodeImpl node)
            throws RepositoryException {
        InternalVersionHistory history = getInternalVersionHistory(node);
        return new VersionHistoryImpl(node.getSession(), history);
    }

    /**
     * returns the internal version history for the given node
     *
     * @param node
     * @return
     * @throws RepositoryException
     */
    private InternalVersionHistory getInternalVersionHistory(NodeImpl node)
            throws RepositoryException {
        String histUUID = node.getProperty(PROPNAME_VERSION_HISTORY).getString();
        try {
            PersistentNodeState state = (PersistentNodeState) stateMgr.getItemState(new NodeId(histUUID));
            PersistentNode hNode = new PersistentNode(stateMgr, ntMgr, state);
            return new InternalVersionHistory(hNode);
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        }
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
        InternalVersionHistory history = getInternalVersionHistory(node);

        // 0. resolve the predecessors
        Value[] values = node.getProperty(PROPNAME_PREDECESSORS).getValues();
        InternalVersion[] preds = new InternalVersion[values.length];
        for (int i = 0; i < values.length; i++) {
            preds[i] = history.getVersion(values[i].getString());
        }

        // 0.1 search a predecessor, suitable for generating the new name
        String versionName = null;
        int maxDots = Integer.MAX_VALUE;
        for (int i = 0; i < preds.length; i++) {
            // take the first pred. without a successor
            if (preds[i].getSuccessors().length == 0) {
                versionName = preds[i].getName().getLocalName(); //assuming no namespaces in version names
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
        if (versionName == null) {
            versionName = preds[0].getName().getLocalName();
            do {
                versionName += ".1";
            } while (history.hasVersion(new QName("", versionName)));
        }

        InternalVersion v = history.checkin(new QName("", versionName), node);
        return new VersionImpl(node.getSession(), v);
    }

}
