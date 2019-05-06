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

import static org.apache.jackrabbit.core.ItemValidator.CHECK_HOLD;
import static org.apache.jackrabbit.core.ItemValidator.CHECK_LOCK;
import static org.apache.jackrabbit.core.ItemValidator.CHECK_PENDING_CHANGES;
import static org.apache.jackrabbit.core.ItemValidator.CHECK_PENDING_CHANGES_ON_NODE;

import java.util.ArrayList;
import java.util.Calendar;
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
import javax.jcr.version.VersionManager;

import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.core.session.SessionOperation;
import org.apache.jackrabbit.core.session.SessionWriteOperation;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.UpdatableItemStateManager;
import org.apache.jackrabbit.core.version.InconsistentVersioningState;
import org.apache.jackrabbit.core.version.InternalActivity;
import org.apache.jackrabbit.core.version.InternalBaseline;
import org.apache.jackrabbit.core.version.InternalVersion;
import org.apache.jackrabbit.core.version.InternalVersionHistory;
import org.apache.jackrabbit.core.version.NodeStateEx;
import org.apache.jackrabbit.core.version.VersionImpl;
import org.apache.jackrabbit.core.version.VersionManagerImplConfig;
import org.apache.jackrabbit.core.version.VersionSet;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the {@link javax.jcr.version.VersionManager}.
 * <p>
 * This class implements the JCR Version Manager interface but most of the
 * operations are performed in the super classes. this is only cosmetic to
 * avoid huge source files.
 */
public class VersionManagerImpl extends VersionManagerImplConfig
        implements VersionManager {

    /**
     * default logger
     */
    private static final Logger log = LoggerFactory.getLogger(VersionManagerImpl.class);

    /**
     * Creates a new version manager
     *
     * @param context component context of the current session
     * @param stateMgr the underlying state manager
     * @param hierMgr local hierarchy manager
     */
    public VersionManagerImpl(
            SessionContext context, UpdatableItemStateManager stateMgr,
            HierarchyManager hierMgr) {
        super(context, stateMgr, hierMgr);
    }

    private <T> T perform(SessionOperation<T> operation)
            throws RepositoryException {
        return context.getSessionState().perform(operation);
    }

    /** Wrapper around {@link #checkin(String, Calendar)}. */
    public Version checkin(String absPath) throws RepositoryException {
        return checkin(absPath, null);
    }

    /**
     * Creates a new version of the node at the given path.
     *
     * @param absPath node path
     * @param created create time of the new version,
     *                or <code>null</code> for the current time
     * @return new version
     * @throws RepositoryException if the version can not be created
     */
    public Version checkin(final String absPath, final Calendar created)
            throws RepositoryException {
        return perform(new SessionWriteOperation<Version> () {
            public Version perform(SessionContext context)
                    throws RepositoryException {
                NodeStateEx state = getNodeState(
                        absPath,
                        CHECK_LOCK | CHECK_HOLD | CHECK_PENDING_CHANGES_ON_NODE,
                        Permission.VERSION_MNGMT);
                NodeId baseId = checkoutCheckin(state, true, false, created);
                return (Version) session.getNodeById(baseId);
            }
            public String toString() {
                return "versionManager.checkin(" + absPath + ", " + created + ")";
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public void checkout(final String absPath) throws RepositoryException {
        perform(new SessionWriteOperation<NodeId> () {
            public NodeId perform(SessionContext context)
                    throws RepositoryException {
                NodeStateEx state = getNodeState(
                        absPath,
                        CHECK_LOCK | CHECK_HOLD,
                        Permission.VERSION_MNGMT);
                return checkoutCheckin(state, false, true, null);
            }
            public String toString() {
                return "versionManager.checkout(" + absPath + ")";
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public Version checkpoint(final String absPath) throws RepositoryException {
        return perform(new SessionWriteOperation<Version> () {
            public Version perform(SessionContext context)
                    throws RepositoryException {
                NodeStateEx state = getNodeState(
                        absPath,
                        CHECK_LOCK | CHECK_HOLD | CHECK_PENDING_CHANGES_ON_NODE,
                        Permission.VERSION_MNGMT);
                NodeId baseId = checkoutCheckin(state, true, true, null);
                return (Version) session.getNodeById(baseId);
            }
            public String toString() {
                return "versionManager.checkpoint(" + absPath + ")";
            }
        });
    }

    /** Wrapper around {@link Node#isCheckedOut()}. */
    public boolean isCheckedOut(String absPath) throws RepositoryException {
        return session.getNode(absPath).isCheckedOut();
    }

    /**
     * {@inheritDoc}
     */
    public VersionHistory getVersionHistory(final String absPath)
            throws RepositoryException {
        return perform(new SessionOperation<VersionHistory> () {
            public VersionHistory perform(SessionContext context)
                    throws RepositoryException {
                NodeStateEx state = getNodeState(absPath);
                InternalVersionHistory vh = getVersionHistory(state);
                if (vh == null) {
                    throw new InconsistentVersioningState("Couldn't get version history for node " + state.getNodeId());
                }
                return (VersionHistory) session.getNodeById(vh.getId());
            }
            public String toString() {
                return "versionManager.getVersionHistory(" + absPath + ")";
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public Version getBaseVersion(final String absPath)
            throws RepositoryException {
        return perform(new SessionOperation<Version> () {
            public Version perform(SessionContext context)
                    throws RepositoryException {
                NodeStateEx state = getNodeState(absPath);
                InternalVersion v = getBaseVersion(state);
                return (Version) session.getNodeById(v.getId());
            }
            public String toString() {
                return "versionManager.getBaseVersion(" + absPath + ")";
            }
        });
    }

    /** Wrapper around {@link #restore(Version[], boolean)}. */
    public void restore(Version version, boolean removeExisting)
            throws RepositoryException {
        restore(new Version[]{version}, removeExisting);
    }

    /**
     * {@inheritDoc}
     */
    public void restore(final Version[] versions, final boolean removeExisting)
            throws RepositoryException {
        perform(new SessionWriteOperation<Object> () {
            public Object perform(SessionContext context)
                    throws RepositoryException {
                // check for pending changes
                if (session.hasPendingChanges()) {
                    throw new InvalidItemStateException(
                            "Unable to restore version. Session has pending changes.");
                }

                // add all versions to map of versions to restore
                Map<NodeId, InternalVersion> toRestore =
                    new HashMap<NodeId, InternalVersion>();
                for (Version version : versions) {
                    InternalVersion v =
                        vMgr.getVersion(((VersionImpl) version).getNodeId());
                    // check for collision
                    NodeId historyId = v.getVersionHistory().getId();
                    if (toRestore.containsKey(historyId)) {
                        throw new VersionException(
                                "Unable to restore. Two or more versions have same version history.");
                    }
                    toRestore.put(historyId, v);
                }

                WriteOperation ops = startWriteOperation();
                try {
                    internalRestore(
                            new VersionSet(toRestore, true), removeExisting);
                    ops.save();
                } catch (ItemStateException e) {
                    throw new RepositoryException(e);
                } finally {
                    ops.close();
                }

                return this;
            }
            public String toString() {
                return "versionManager.restore(versions, " + removeExisting + ")";
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public void restore(
            final String absPath, final String versionName,
            final boolean removeExisting) throws RepositoryException {
        perform(new SessionWriteOperation<Object> () {
            public Object perform(SessionContext context)
                    throws RepositoryException {
                NodeStateEx state = getNodeState(
                        absPath,
                        CHECK_PENDING_CHANGES | CHECK_LOCK | CHECK_HOLD,
                        Permission.NONE);
                restore(state, context.getQName(versionName), removeExisting);
                return this;
            }
            public String toString() {
                return "versionManager.restore("
                    + absPath + ", " +  versionName + ", "
                    +  removeExisting + ")";
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public void restore(
            final String absPath, final Version version, final boolean removeExisting)
            throws RepositoryException {
        perform(new SessionWriteOperation<Object> () {
            public Object perform(SessionContext context)
                    throws RepositoryException {
                // first check if node exists
                if (session.nodeExists(absPath)) {
                    throw new VersionException(
                            "VersionManager.restore(String, Version, boolean)"
                            + " not allowed on existing nodes; use"
                            + " VersionManager.restore(Version, boolean) instead: "
                            + absPath);
                } else {
                    // parent has to exist
                    Path path = context.getQPath(absPath);
                    Path parentPath = path.getAncestor(1);
                    Name name = path.getName();
                    NodeImpl parent = context.getItemManager().getNode(parentPath);

                    NodeStateEx state = getNodeState(
                            parent,
                            CHECK_PENDING_CHANGES | ItemValidator.CHECK_LOCK | CHECK_HOLD,
                            Permission.NONE);

                    // check if given version is a baseline
                    InternalVersion v = getVersion(version);
                    if (v instanceof InternalBaseline) {
                        restore(state, name, (InternalBaseline) v);
                    } else {
                        restore(state, name, v, removeExisting);
                    }
                }
                return this;
            }
            public String toString() {
                return "versionManager.restore("
                    + absPath + ", version, " + removeExisting + ")";
            }
        });
    }

    /**
     * Same as {@link #restore(String, String, boolean)} but to ensure
     * backward compatibility for Node.restore(Version, boolean).
     *
     * @param node the node to restore
     * @param version the version to restore
     * @param removeExisting the remove existing flag
     * @throws RepositoryException if an error occurs
     */
    protected void restore(NodeImpl node, Version version, boolean removeExisting)
            throws RepositoryException {
        NodeStateEx state = getNodeState(
                node.getPath(),
                CHECK_PENDING_CHANGES | CHECK_LOCK | CHECK_HOLD,
                Permission.NONE);
        InternalVersion v = getVersion(version);
        restore(state, v, removeExisting);
    }

    /**
     * {@inheritDoc}
     */
    public void restoreByLabel(
            final String absPath, final String versionLabel,
            final boolean removeExisting) throws RepositoryException {
        perform(new SessionWriteOperation<Object> () {
            public Object perform(SessionContext context)
                    throws RepositoryException {
                NodeStateEx state = getNodeState(
                        absPath,
                        CHECK_PENDING_CHANGES | CHECK_LOCK | CHECK_HOLD,
                        Permission.NONE);
                restoreByLabel(
                        state, context.getQName(versionLabel), removeExisting);
                return this;
            }
            public String toString() {
                return "versionManager.restoreByLabel("
                    + absPath + ", " +  versionLabel + ", "
                    + removeExisting + ")";
            }
        });
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
        NodeStateEx state = getNodeState(node,
                ItemValidator.CHECK_PENDING_CHANGES,
                Permission.VERSION_MNGMT);
        mergeOrUpdate(state, srcWorkspaceName, null, false, false);
    }

    /** Wrapper around {@link #merge(String, String, boolean, boolean)}. */
    public NodeIterator merge(
            String absPath, String srcWorkspace, boolean bestEffort)
            throws RepositoryException {
        return merge(absPath, srcWorkspace, bestEffort, false);
    }

    /**
     * {@inheritDoc}
     */
    public NodeIterator merge(
            final String absPath, final String srcWorkspaceName,
            final boolean bestEffort, final boolean isShallow)
            throws RepositoryException {
        return perform(new SessionWriteOperation<NodeIterator> () {
            public NodeIterator perform(SessionContext context)
                    throws RepositoryException {
                NodeStateEx state = getNodeState(
                        absPath,
                        CHECK_PENDING_CHANGES,
                        Permission.VERSION_MNGMT);
                List<ItemId> failedIds = new LinkedList<ItemId>();
                mergeOrUpdate(state, srcWorkspaceName, failedIds, bestEffort, isShallow);
                return new LazyItemIterator(context, failedIds);
            }
            public String toString() {
                return "versionManager.merge("
                    + absPath + ", " +  srcWorkspaceName + ", "
                    + bestEffort + ", " +  isShallow + ")";
            }
        });
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
                String msg = "not authorized to access " + srcWorkspaceName;
                log.error(msg);
                throw new AccessDeniedException(msg);
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
        NodeStateEx state = getNodeState(absPath,
                ItemValidator.CHECK_LOCK | ItemValidator.CHECK_PENDING_CHANGES_ON_NODE | ItemValidator.CHECK_HOLD,
                Permission.VERSION_MNGMT);
        finishMerge(state, version, false);
    }

    /**
     * {@inheritDoc}
     */
    public void cancelMerge(String absPath, Version version)
            throws RepositoryException {
        NodeStateEx state = getNodeState(absPath,
                ItemValidator.CHECK_LOCK | ItemValidator.CHECK_PENDING_CHANGES_ON_NODE | ItemValidator.CHECK_HOLD,
                Permission.VERSION_MNGMT);
        finishMerge(state, version, true);
    }

    /**
     * {@inheritDoc}
     */
    public Node createConfiguration(String absPath) throws RepositoryException {
        if (session.nodeExists(absPath)) {
            NodeStateEx state = getNodeState(absPath,
                    ItemValidator.CHECK_LOCK | ItemValidator.CHECK_PENDING_CHANGES_ON_NODE | ItemValidator.CHECK_HOLD,
                    Permission.VERSION_MNGMT);
            // check versionable
            if (!checkVersionable(state)) {
                throw new UnsupportedRepositoryOperationException("Node not full versionable: " + absPath);
            }
            if (state.getPropertyValue(NameConstants.JCR_CONFIGURATION) != null) {
                String msg = "Node is already a configuration root: " + absPath;
                log.error(msg);
                throw new UnsupportedRepositoryOperationException(msg);
            }

            NodeId configId = createConfiguration(state);
            return session.getNodeById(configId);
        } else {
            String msg = "Create configuration node must exist: " + absPath;
            log.error(msg);
            throw new UnsupportedRepositoryOperationException(msg);
        }
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
                String msg = "Given node is not an activity: " + actNode.safeGetJCRPath();
                log.error(msg);
                throw new UnsupportedRepositoryOperationException(msg);
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
        NodeId id = vMgr.createActivity(session, title);
        return session.getNodeById(id);
    }

    /**
     * {@inheritDoc}
     */
    public void removeActivity(Node node) throws RepositoryException {
        NodeImpl actNode = (NodeImpl) node;
        if (!actNode.isNodeType(NameConstants.NT_ACTIVITY)) {
            String msg = "Given node is not an activity: " + actNode.safeGetJCRPath();
            log.error(msg);
            throw new UnsupportedRepositoryOperationException(msg);
        }
        NodeId actId = actNode.getNodeId();
        vMgr.removeActivity(session, actId);
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
            String msg = "Given node is not an activity: " + actNode.safeGetJCRPath();
            log.error(msg);
            throw new UnsupportedRepositoryOperationException(msg);
        }
        InternalActivity activity = vMgr.getActivity(actNode.getNodeId());
        if (activity == null) {
            String msg = "Given activity not found in version storage.";
            log.error(msg);
            throw new UnsupportedRepositoryOperationException(msg);
        }
        List<ItemId> failedIds = new ArrayList<ItemId>();
        merge(activity, failedIds);
        return new LazyItemIterator(context, failedIds);
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
                context.getItemValidator().checkModify(node, options, permissions);
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
