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
import org.apache.jackrabbit.core.util.uuid.UUID;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.NodeDefId;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.PersistentNodeState;
import org.apache.jackrabbit.core.state.PersistentItemStateProvider;
import org.apache.log4j.Logger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.Workspace;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This Class provides general versioning functions.
 */
public class PersistentVersionManager {

    /**
     * the logger
     */
    private static Logger log = Logger.getLogger(PersistentVersionManager.class);

    /**
     * root path for version storage
     */
    public static final QName VERSION_HISTORY_ROOT_NAME = new QName(NamespaceRegistryImpl.NS_JCR_URI, "persistentVersionStorage");

    /**
     * name of the 'jcr:historyId' property
     */
    public static final QName PROPNAME_HISTORY_ID = new QName(NamespaceRegistryImpl.NS_JCR_URI, "historyId");
    /**
     * name of the 'jcr:versionId' property
     */
    public static final QName PROPNAME_VERSION_ID = new QName(NamespaceRegistryImpl.NS_JCR_URI, "versionId");
    /**
     * name of the 'jcr:versionLabels' node
     */
    public static final QName NODENAME_VERSION_LABELS = new QName(NamespaceRegistryImpl.NS_JCR_URI, "versionLabels");
    /**
     * name of the 'jcr:frozen' property
     */
    public static final QName NODENAME_FROZEN = new QName(NamespaceRegistryImpl.NS_JCR_URI, "frozen");
    /**
     * name of the 'jcr:name' property
     */
    public static final QName PROPNAME_NAME = new QName(NamespaceRegistryImpl.NS_JCR_URI, "name");
    /**
     * name of the 'jcr:version' property
     */
    public static final QName PROPNAME_VERSION = new QName(NamespaceRegistryImpl.NS_JCR_URI, "version");

    /**
     * the id of the persisten root node
     */
    private static final NodeId PERSISTENT_ROOT_ID = new NodeId("faceface-ab3b-48a9-b31b-e7d0a9c1c3b1");

    /**
     * the persistent root node of the version histories
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
     * The representation version managers (per workspace)
     */
    private HashMap versionManagers = new HashMap();

    /**
     * the version histories
     */
    private HashMap histories = new HashMap();

    /**
     * Creates a new PersistentVersionManager.
     *
     * @param session
     * @throws RepositoryException
     */
    public PersistentVersionManager(SessionImpl session) throws RepositoryException {
        this.stateMgr = ((WorkspaceImpl) session.getWorkspace()).getPersistentStateManager();
        this.ntMgr = session.getNodeTypeManager();

        try {
            NodeImpl systemRoot = ((RepositoryImpl) session.getRepository()).getSystemRootNode(session);
            // enable this to make the persistence storage visible
            if (false) {
                // check for versionhistory root
                if (!systemRoot.hasNode(VERSION_HISTORY_ROOT_NAME)) {
                    // if not exist, create
                    systemRoot.addNode(VERSION_HISTORY_ROOT_NAME, NodeTypeRegistry.NT_UNSTRUCTURED);
                    systemRoot.save();
                }
                PersistentNodeState nodeState = (PersistentNodeState) stateMgr.getItemState(new NodeId(systemRoot.getNode(VERSION_HISTORY_ROOT_NAME).internalGetUUID()));
                historyRoot = new PersistentNode(stateMgr, ntMgr, nodeState);
            } else {
                if (!stateMgr.hasItemState(PERSISTENT_ROOT_ID)) {
                    PersistentNodeState nodeState = stateMgr.createNodeState(PERSISTENT_ROOT_ID.getUUID(), NodeTypeRegistry.NT_UNSTRUCTURED, null);
                    nodeState.setDefinitionId(new NodeDefId(ntMgr.getRootNodeDefinition().unwrap()));
                    nodeState.store();
                    historyRoot = new PersistentNode(stateMgr, ntMgr, nodeState);
                } else {
                    PersistentNodeState nodeState = (PersistentNodeState) stateMgr.getItemState(PERSISTENT_ROOT_ID);
                    historyRoot = new PersistentNode(stateMgr, ntMgr, nodeState);
                }
            }
        } catch (ItemStateException e) {
            throw new RepositoryException("Unable to initialize PersistentVersionManager: " + e.toString());
        }
    }

    /**
     * Creates a new Version History..
     *
     * @param node the node for which the version history is to be initialized
     * @return the newly created version history.
     * @throws RepositoryException
     */
    public synchronized InternalVersionHistory createVersionHistory(NodeImpl node)
            throws RepositoryException {

        // create deep path
        String uuid = UUID.randomUUID().toString();
        QName historyNodeName = new QName(NamespaceRegistryImpl.NS_DEFAULT_URI, uuid);
        if (historyRoot.hasNode(historyNodeName)) {
            historyRoot.removeNode(historyNodeName);
            historyRoot.store();
        }

        // create new history node in the persistent state
        InternalVersionHistory hist = InternalVersionHistory.create(this, historyRoot, uuid, historyNodeName, node);
        histories.put(hist.getId(), hist);

        // notify version managers
        onVersionHistoryModified(hist);
        return hist;
    }

    /**
     * returns the internal version history for the id
     *
     * @param histId the id of the history
     * @return
     * @throws RepositoryException
     */
    public synchronized InternalVersionHistory getVersionHistory(String histId)
            throws RepositoryException {

        InternalVersionHistory hist = (InternalVersionHistory) histories.get(histId);
        if (hist==null) {
            // we cannot used the uuid, since the persistent state do not share the same ids
            QName historyNodeName = new QName(NamespaceRegistryImpl.NS_DEFAULT_URI, histId);
            PersistentNode hNode = historyRoot.getNode(historyNodeName, 1);
            hist = new InternalVersionHistory(this, hNode);
            histories.put(histId, hist);
        }
        return hist;
    }

    /**
     * returns an iterator over all existing version histories
     * @return
     * @throws RepositoryException
     */
    public synchronized Iterator getVersionHistories() throws RepositoryException {
        PersistentNode[] ph = historyRoot.getChildNodes();
        ArrayList list = new ArrayList(ph.length);
        for (int i=0; i<ph.length; i++) {
            list.add(getVersionHistory(ph[i].getName().getLocalName()));
        }
        return list.iterator();
    }

    /**
     * returns the internal version for the id
     *
     * @param versionId
     * @return
     * @throws RepositoryException
     */
    public synchronized InternalVersion getVersion(String histId, String versionId)
            throws RepositoryException {
        InternalVersionHistory history = getVersionHistory(histId);
        return history.getVersion(versionId);
    }

    /**
     * is informed by the versions if they were modified
     * @param version
     */
    protected void onVersionModified(InternalVersion version)  throws RepositoryException {
        // check if version manager already generated item states
        Iterator iter = versionManagers.values().iterator();
        while (iter.hasNext()) {
            ((VersionManager) iter.next()).onVersionModified(version);
        }
    }

    /**
     * is informed by the versions if they were modified
     * @param vh
     */
    protected void onVersionHistoryModified(InternalVersionHistory vh)  throws RepositoryException {
        // check if version manager already generated item states
        Iterator iter = versionManagers.values().iterator();
        while (iter.hasNext()) {
            ((VersionManager) iter.next()).onVersionHistoryModified(vh);
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
    public synchronized InternalVersion checkin(NodeImpl node) throws RepositoryException {
        // assuming node is versionable and checkout (check in nodeimpl)
        // To create a new version of a versionable node N, the client calls N.checkin.
        // This causes the following series of events:
        String histUUID = node.getProperty(VersionManager.PROPNAME_VERSION_HISTORY).getString();
        InternalVersionHistory history = getVersionHistory(histUUID);

        // 0. resolve the predecessors
        Value[] values = node.getProperty(VersionManager.PROPNAME_PREDECESSORS).getValues();
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

        return history.checkin(new QName("", versionName), node);
    }

    /**
     * returns the version manager
     * @return
     */
    public synchronized VersionManager getVersionManager(Workspace wsp) {
        VersionManager vm = (VersionManager) versionManagers.get(wsp.getName());
        if (vm==null) {
            vm = new VersionManager(this);
            versionManagers.put(wsp.getName(), vm);
        }
        return vm;
    }

    /**
     * returns the node type manager of the version storage
     * @return
     */
    public NodeTypeManagerImpl getNodeTypeManager(){
        return ntMgr;
    }
}
