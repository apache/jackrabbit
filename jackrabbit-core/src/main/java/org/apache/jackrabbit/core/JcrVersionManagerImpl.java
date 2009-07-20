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
package org.apache.jackrabbit.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.UpdatableItemStateManager;
import org.apache.jackrabbit.core.version.InternalActivity;
import org.apache.jackrabbit.core.version.InternalVersion;
import org.apache.jackrabbit.core.version.InternalVersionHistory;
import org.apache.jackrabbit.core.version.JcrVersionManagerImplMerge;
import org.apache.jackrabbit.core.version.NodeStateEx;
import org.apache.jackrabbit.core.version.VersionImpl;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the {@link javax.jcr.version.VersionManager}.
 * <p/>
 * This class implements the JCR Version Manager interface but most of the
 * operations are performed in the super classes. this is only cosmetic to
 * avoid huge source files.
 * <p/>
 * Note: For a cleaner architecture, we should probably rename the existing classes
 * that implement the internal version manager, and name this VersionManagerImpl.
 */
public class JcrVersionManagerImpl extends JcrVersionManagerImplMerge
        implements javax.jcr.version.VersionManager {

    /**
     * default logger
     */
    private static final Logger log = LoggerFactory.getLogger(JcrVersionManagerImpl.class);

    /**
     * Creates a new version manager for the given session
     * @param session workspace sesion
     * @param stateMgr the underlying state manager
     * @param hierMgr local hierarchy manager
     */
    public JcrVersionManagerImpl(SessionImpl session,
                                 UpdatableItemStateManager stateMgr,
                                 HierarchyManager hierMgr) {
        super(session, stateMgr, hierMgr);
    }

    /**
     * {@inheritDoc}
     */
    public Version checkin(String absPath) throws RepositoryException {
        // check lock status, holds and permissions
        int options = ItemValidator.CHECK_LOCK | ItemValidator.CHECK_HOLD |
                ItemValidator.CHECK_PENDING_CHANGES_ON_NODE;
        NodeStateEx state = getNodeState(absPath, options, Permission.VERSION_MNGMT);
        NodeId baseId = checkoutCheckin(state, true, false);
        return (VersionImpl) session.getNodeById(baseId);
    }

    /**
     * {@inheritDoc}
     */
    public void checkout(String absPath) throws RepositoryException {
        // check lock status, holds and permissions
        int options = ItemValidator.CHECK_LOCK | ItemValidator.CHECK_HOLD;
        NodeStateEx state = getNodeState(absPath, options, Permission.VERSION_MNGMT);
        checkoutCheckin(state, false, true);
    }

    /**
     * {@inheritDoc}
     */
    public Version checkpoint(String absPath) throws RepositoryException {
        int options = ItemValidator.CHECK_LOCK | ItemValidator.CHECK_HOLD |
                ItemValidator.CHECK_PENDING_CHANGES_ON_NODE;
        NodeStateEx state = getNodeState(absPath, options, Permission.VERSION_MNGMT);
        NodeId baseId = checkoutCheckin(state, true, true);
        return (VersionImpl) session.getNodeById(baseId);
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
        NodeStateEx state = getNodeState(absPath);
        InternalVersionHistory vh = getVersionHistory(state);
        return (VersionHistory) session.getNodeById(vh.getId());
    }

    /**
     * {@inheritDoc}
     */
    public Version getBaseVersion(String absPath)
            throws RepositoryException {
        NodeStateEx state = getNodeState(absPath);
        InternalVersion v = getBaseVersion(state);
        return (Version) session.getNodeById(v.getId());
    }

    /**
     * {@inheritDoc}
     */
    public void restore(Version version, boolean removeExisting)
            throws RepositoryException {
        restore(new Version[]{version}, removeExisting);
    }

    /**
     * {@inheritDoc}
     */
    public void restore(Version[] versions, boolean removeExisting)
            throws RepositoryException {
        // check for pending changes
        if (session.hasPendingChanges()) {
            String msg = "Unable to restore version. Session has pending changes.";
            log.debug(msg);
            throw new InvalidItemStateException(msg);
        }
        // add all versions to map of versions to restore
        final Map<NodeId, InternalVersion> toRestore = new HashMap<NodeId, InternalVersion>();
        for (Version version : versions) {
            InternalVersion v = vMgr.getVersion(((VersionImpl) version).getNodeId());
            NodeId historyId = v.getVersionHistory().getId();
            // check for collision
            if (toRestore.containsKey(historyId)) {
                throw new VersionException("Unable to restore. Two or more versions have same version history.");
            }
            toRestore.put(historyId, v);
        }
        restore(toRestore, removeExisting);
    }

    /**
     * {@inheritDoc}
     */
    public void restore(String absPath, String versionName,
                        boolean removeExisting)
            throws RepositoryException {
        int options = ItemValidator.CHECK_PENDING_CHANGES | ItemValidator.CHECK_LOCK | ItemValidator.CHECK_HOLD;
        NodeStateEx state = getNodeState(absPath, options, Permission.NONE);
        restore(state, session.getQName(versionName), removeExisting);
    }


    /**
     * {@inheritDoc}
     */
    public void restore(String absPath, Version version, boolean removeExisting)
            throws RepositoryException {

        // first check if node exists
        if (session.nodeExists(absPath)) {
            // normal restore
            int options = ItemValidator.CHECK_PENDING_CHANGES | ItemValidator.CHECK_LOCK | ItemValidator.CHECK_HOLD;
            NodeStateEx state = getNodeState(absPath, options, Permission.NONE);
            restore(state, version, removeExisting);
        } else {
            // parent has to exist
            Path path = session.getQPath(absPath);
            Path parentPath = path.getAncestor(1);
            Name name = path.getNameElement().getName();
            NodeImpl parent = session.getItemManager().getNode(parentPath);

            int options = ItemValidator.CHECK_PENDING_CHANGES | ItemValidator.CHECK_LOCK | ItemValidator.CHECK_HOLD;
            NodeStateEx state = getNodeState(parent, options, Permission.NONE);
            restore(state, name, version, removeExisting);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void restoreByLabel(String absPath, String versionLabel,
                               boolean removeExisting)
            throws RepositoryException {
        int options = ItemValidator.CHECK_PENDING_CHANGES | ItemValidator.CHECK_LOCK | ItemValidator.CHECK_HOLD;
        NodeStateEx state = getNodeState(absPath, options, Permission.NONE);
        restoreByLabel(state, session.getQName(versionLabel), removeExisting);
    }

    /**
     * Does an update.
     * @see javax.jcr.Node#update(String)
     *
     * @param node the node to update
     * @param srcWorkspaceName the source workspace name
     * @throws RepositoryException if an error occurs
     */
    public void update(NodeImpl node, String srcWorkspaceName)
            throws RepositoryException {
        NodeStateEx state = getNodeState(node, ItemValidator.CHECK_PENDING_CHANGES, Permission.VERSION_MNGMT);
        mergeOrUpdate(state, srcWorkspaceName, null, false, false);
    }

    /**
     * {@inheritDoc}
     */
    public NodeIterator merge(String absPath, String srcWorkspace,
                              boolean bestEffort)
            throws RepositoryException {
        return merge(absPath, srcWorkspace, bestEffort, false);
    }

    /**
     * {@inheritDoc}
     */
    public NodeIterator merge(String absPath, String srcWorkspaceName,
                              boolean bestEffort, boolean isShallow)
            throws RepositoryException {
        NodeStateEx state = getNodeState(absPath, ItemValidator.CHECK_PENDING_CHANGES, Permission.VERSION_MNGMT);
        List<ItemId> failedIds = new LinkedList<ItemId>();
        mergeOrUpdate(state, srcWorkspaceName, failedIds, bestEffort, isShallow);
        return new LazyItemIterator(session.getItemManager(), failedIds);
    }

    /**
     * Combines merge and update method
     * @param state the state to merge or update
     * @param srcWorkspaceName source workspace name
     * @param failedIds list that will contain the failed ids.
     *        if <code>null</code> and update will be performed.
     * @param bestEffort best effort flag
     * @param isShallow is shallow flag
     * @throws RepositoryException if an error occurs
     */
    private void mergeOrUpdate(NodeStateEx state, String srcWorkspaceName,
                               List<ItemId> failedIds, boolean bestEffort,
                               boolean isShallow)
            throws RepositoryException {
        // if same workspace, ignore
        if (!srcWorkspaceName.equals(session.getWorkspace().getName())) {
            // check authorization for specified workspace
            if (!session.getAccessManager().canAccess(srcWorkspaceName)) {
                throw new AccessDeniedException("not authorized to access " + srcWorkspaceName);
            }
            // get root node of src workspace
            SessionImpl srcSession = null;
            try {
                // create session on other workspace for current subject
                // (may throw NoSuchWorkspaceException and AccessDeniedException)
                srcSession = ((RepositoryImpl) session.getRepository())
                        .createSession(session.getSubject(), srcWorkspaceName);
                WorkspaceImpl srcWsp = (WorkspaceImpl) srcSession.getWorkspace();
                NodeId rootNodeId = ((NodeImpl) srcSession.getRootNode()).getNodeId();
                NodeStateEx srcRoot = new NodeStateEx(
                        srcWsp.getItemStateManager(),
                        ntReg,
                        rootNodeId);
                merge(state, srcRoot, failedIds, bestEffort, isShallow);
            } catch (ItemStateException e) {
                throw new RepositoryException(e);
            } finally {
                if (srcSession != null) {
                    // we don't need the other session anymore, logout
                    srcSession.logout();
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void doneMerge(String absPath, Version version)
            throws RepositoryException {
        int options = ItemValidator.CHECK_LOCK | ItemValidator.CHECK_VERSIONING | ItemValidator.CHECK_PENDING_CHANGES_ON_NODE | ItemValidator.CHECK_HOLD;
        NodeStateEx state = getNodeState(absPath, options, Permission.VERSION_MNGMT);
        finishMerge(state, version, false);
    }

    /**
     * {@inheritDoc}
     */
    public void cancelMerge(String absPath, Version version)
            throws RepositoryException {
        int options = ItemValidator.CHECK_LOCK | ItemValidator.CHECK_VERSIONING | ItemValidator.CHECK_PENDING_CHANGES_ON_NODE | ItemValidator.CHECK_HOLD;
        NodeStateEx state = getNodeState(absPath, options, Permission.VERSION_MNGMT);
        finishMerge(state, version, true);
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
        if (actId.equals(currentActivity)) {
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
        List<ItemId> failedIds = new ArrayList<ItemId>();
        merge(activity, failedIds);
        return new LazyItemIterator(session.getItemManager(), failedIds);
    }

    /**
     * returns the node state for the given path
     * @param path path of the node
     * @throws RepositoryException if an error occurs
     * @return the node state
     */
    private NodeStateEx getNodeState(String path) throws RepositoryException {
        return getNodeState(path, 0, 0);
    }

    /**
     * checks the permissions for the given path and returns the node state
     * @param path path of the node
     * @param options options to check
     * @param permissions permissions to check
     * @throws RepositoryException if an error occurs
     * @return the node state
     */
    private NodeStateEx getNodeState(String path, int options, int permissions)
            throws RepositoryException {
        return getNodeState((NodeImpl) session.getNode(path), options, permissions);
    }

    /**
     * checks the permissions for the given path and returns the node state
     * @param node the node
     * @param options options to check
     * @param permissions permissions to check
     * @throws RepositoryException if an error occurs
     * @return the node state
     */
    private NodeStateEx getNodeState(NodeImpl node, int options, int permissions)
            throws RepositoryException {
        try {
            if (options > 0 || permissions > 0) {
                session.getValidator().checkModify(node, options, permissions);
            }
            return new NodeStateEx(
                    stateMgr,
                    ntReg,
                    (NodeState) stateMgr.getItemState(node.getNodeId()),
                    node.getQName());
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        }
    }
}