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
package org.apache.jackrabbit.core.version;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.LazyItemIterator;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Implementation of the {@link javax.jcr.version.VersionManager}.
 * <p/>
 * Note: For a cleaner architecture, we should probably rename the existing classes
 * that implement the internal version manager, and name this VersionManagerImpl.
 */
public class JcrVersionManagerImpl implements javax.jcr.version.VersionManager {

    /**
     * default logger
     */
    private static final Logger log = LoggerFactory.getLogger(JcrVersionManagerImpl.class);

    /**
     * workspace session
     */
    private final SessionImpl session;

    /**
     * the node id of the current activity
     */
    private NodeId currentActivity;


    /**
     * Creates a new version manager for the given session
     * @param session workspace sesion
     */
    public JcrVersionManagerImpl(SessionImpl session) {
        this.session = session;
    }

    /**
     * {@inheritDoc}
     */
    public Version checkin(String absPath) throws RepositoryException {
        return session.getNode(absPath).checkin();
    }

    /**
     * {@inheritDoc}
     */
    public void checkout(String absPath) throws RepositoryException {
        session.getNode(absPath).checkout();
    }

    /**
     * {@inheritDoc}
     */
    public Version checkpoint(String absPath) throws RepositoryException {
        // this is not quite correct, since the entire checkpoint operation
        // should be atomic
        Node node = session.getNode(absPath);
        Version v = node.checkin();
        node.checkout();
        return v;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCheckedOut(String absPath) throws RepositoryException {
        return session.getNode(absPath).isCheckedOut();
    }

    /**
     * {@inheritDoc}
     */
    public VersionHistory getVersionHistory(String absPath)
            throws RepositoryException {
        return session.getNode(absPath).getVersionHistory();
    }

    /**
     * {@inheritDoc}
     */
    public Version getBaseVersion(String absPath)
            throws RepositoryException {
        return session.getNode(absPath).getBaseVersion();
    }

    /**
     * {@inheritDoc}
     */
    public void restore(Version[] versions, boolean removeExisting)
            throws RepositoryException {
        session.getWorkspace().restore(versions, removeExisting);
    }

    /**
     * {@inheritDoc}
     */
    public void restore(String absPath, String versionName,
                        boolean removeExisting)
            throws RepositoryException {
        session.getNode(absPath).restore(versionName, removeExisting);
    }

    /**
     * {@inheritDoc}
     */
    public void restore(Version version, boolean removeExisting)
            throws RepositoryException {
        session.getWorkspace().restore(new Version[]{version}, removeExisting);
    }

    /**
     * {@inheritDoc}
     */
    public void restore(String absPath, Version version, boolean removeExisting)
            throws RepositoryException {
        session.getNode(absPath).restore(version, removeExisting);
    }

    /**
     * {@inheritDoc}
     */
    public void restoreByLabel(String absPath, String versionLabel,
                               boolean removeExisting)
            throws RepositoryException {
        session.getNode(absPath).restoreByLabel(versionLabel, removeExisting);
    }

    /**
     * {@inheritDoc}
     */
    public NodeIterator merge(String absPath, String srcWorkspace,
                              boolean bestEffort)
            throws RepositoryException {
        return ((NodeImpl) session.getNode(absPath))
                .merge(srcWorkspace, bestEffort, false);
    }

    /**
     * {@inheritDoc}
     */
    public NodeIterator merge(String absPath, String srcWorkspace,
                              boolean bestEffort, boolean isShallow)
            throws RepositoryException {
        return ((NodeImpl) session.getNode(absPath))
                .merge(srcWorkspace, bestEffort, isShallow);
    }

    /**
     * {@inheritDoc}
     */
    public void doneMerge(String absPath, Version version)
            throws RepositoryException {
        session.getNode(absPath).doneMerge(version);
    }

    /**
     * {@inheritDoc}
     */
    public void cancelMerge(String absPath, Version version)
            throws RepositoryException {
        session.getNode(absPath).cancelMerge(version);
    }

    /**
     * {@inheritDoc}
     */
    public Node createConfiguration(String absPath, Version baseline)
            throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("comming soon...");
    }

    /**
     * {@inheritDoc}
     */
    public Node setActivity(Node activity) throws RepositoryException {
        Node oldActivity = getActivity();
        if (activity == null) {
            currentActivity = null;
        } else {
            NodeImpl actNode = (NodeImpl) activity;
            if (!actNode.isNodeType(NameConstants.NT_ACTIVITY)) {
                throw new UnsupportedRepositoryOperationException("Given node is not an activity.");
            }
            currentActivity = actNode.getNodeId();
        }
        return oldActivity;
    }

    /**
     * {@inheritDoc}
     */
    public Node getActivity() throws RepositoryException {
        if (currentActivity == null) {
            return null;
        } else {
            return session.getNodeById(currentActivity);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Node createActivity(String title) throws RepositoryException {
        NodeId id = session.getVersionManager().createActivity(session, title);
        return session.getNodeById(id);
    }

    /**
     * {@inheritDoc}
     */
    public void removeActivity(Node node) throws RepositoryException {
        NodeImpl actNode = (NodeImpl) node;
        if (!actNode.isNodeType(NameConstants.NT_ACTIVITY)) {
            throw new UnsupportedRepositoryOperationException("Given node is not an activity.");
        }
        NodeId actId = actNode.getNodeId();
        session.getVersionManager().removeActivity(session, actId);
        if (currentActivity.equals(actId)) {
            currentActivity = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public NodeIterator merge(Node activityNode) throws RepositoryException {
        NodeImpl actNode = (NodeImpl) activityNode;
        if (!actNode.isNodeType(NameConstants.NT_ACTIVITY)) {
            throw new UnsupportedRepositoryOperationException("Given node is not an activity.");
        }
        InternalActivity activity = session.getVersionManager().getActivity(actNode.getNodeId());
        if (activity == null) {
            throw new UnsupportedRepositoryOperationException("Given activity not found.");
        }
        boolean success = false;
        try {
            NodeIterator ret = internalMerge(activity);
            session.save();
            success = true;
            return ret;
        } finally {
            if (!success) {
                // revert session
                try {
                    log.debug("reverting changes applied during merge...");
                    session.refresh(false);
                } catch (RepositoryException e) {
                    log.error("Error while reverting changes applied merge restore.", e);
                }
            }
        }
    }

    /**
     * Internally does the merge without saving the changes.
     * @param activity internal activity
     * @throws RepositoryException if an error occurs
     * @return a node iterator of all failed nodes
     */
    private NodeIterator internalMerge(InternalActivity activity)
            throws RepositoryException {
        List<ItemId> failedIds = new ArrayList<ItemId>();
        Map<NodeId, InternalVersion> changeSet = activity.getChangeSet();
        ChangeSetVersionSelector vsel = new ChangeSetVersionSelector(changeSet);
        Iterator<NodeId> iter = changeSet.keySet().iterator();
        while (iter.hasNext()) {
            InternalVersion v = changeSet.remove(iter.next());
            NodeId nodeId = new NodeId(v.getVersionHistory().getVersionableUUID());
            try {
                NodeImpl node = session.getNodeById(nodeId);
                InternalVersion base = ((VersionImpl) node.getBaseVersion()).getInternalVersion();
                VersionImpl version = (VersionImpl) session.getNodeById(v.getId());
                // if base version is newer than version, add to failed list
                // but merge it anyways
                if (base.isMoreRecent(version.getInternalVersion())) {
                    failedIds.add(node.getNodeId());
                    // should we add it to the jcr:mergeFailed property ?
                } else {
                    Version[] vs = node.internalRestore(version, vsel, true);
                    for (Version restored: vs) {
                        changeSet.remove(((VersionImpl) restored).getNodeId());
                    }
                }
            } catch (ItemNotFoundException e) {
                // ignore nodes not present in this workspace (not best practice)
            }

            // reset iterator
            iter = changeSet.keySet().iterator();
        }
        return new LazyItemIterator(session.getItemManager(), failedIds);
    }

    /**
     * Internal version selector that selects the version in the changeset.
     */
    private class ChangeSetVersionSelector implements VersionSelector {

        private final Map<NodeId, InternalVersion> changeSet;

        private ChangeSetVersionSelector(Map<NodeId, InternalVersion> changeSet) {
            this.changeSet = changeSet;
        }

        public Version select(VersionHistory vh) throws RepositoryException {
            InternalVersion v = changeSet.get(((VersionHistoryImpl) vh).getNodeId());
            if (v != null) {
                return (Version) session.getNodeById(v.getId());
            } else {
                return null;
            }
        }
    }
}