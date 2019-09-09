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

import java.util.Calendar;
import java.util.Set;
import java.util.HashSet;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.ItemNotFoundException;
import javax.jcr.version.Version;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.LocalItemStateManager;
import org.apache.jackrabbit.core.state.UpdatableItemStateManager;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.Path;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * The JCR Version Manager implementation is split in several classes in order to
 * group related methods together.
 * <p>
 * This class provides basic routines for all operations and the methods related
 * to checkin and checkout.
 */
abstract public class VersionManagerImplBase {

    /**
     * default logger
     */
    private static final Logger log = LoggerFactory.getLogger(VersionManagerImplBase.class);

    /**
     * Component context of the current session
     */
    protected final SessionContext context;

    /**
     * workspace session
     */
    protected final SessionImpl session;

    /**
     * item state manager for all workspace operations
     */
    protected final UpdatableItemStateManager stateMgr;

    /**
     * hierarchy manager that operates on the locale state manager
     */
    protected final HierarchyManager hierMgr;

    /**
     * node type registry
     */
    protected final NodeTypeRegistry ntReg;

    /**
     * the session version manager.
     */
    protected final InternalVersionManager vMgr;

    /**
     * the lock on this version manager
     */
    private final VersioningLock rwLock = new VersioningLock();

    /**
     * the node id of the current activity
     */
    protected NodeId currentActivity;

    /**
     * Creates a new version manager base for the given session
     *
     * @param context component context of the current session
     * @param stateMgr the underlying state manager
     * @param hierMgr local hierarchy manager
     */
    protected VersionManagerImplBase(
            SessionContext context, UpdatableItemStateManager stateMgr,
            HierarchyManager hierMgr) {
        this.context = context;
        this.session = context.getSessionImpl();
        this.stateMgr = stateMgr;
        this.hierMgr = hierMgr;
        this.ntReg = session.getNodeTypeManager().getNodeTypeRegistry();
        this.vMgr = session.getInternalVersionManager();
    }

    /**
     * Performs a checkin or checkout operation. if <code>checkin</code> is
     * <code>true</code> the node is checked in. if <code>checkout</code> is
     * <code>true</code> the node is checked out. if both flags are <code>true</code>
     * the checkin is performed prior to the checkout and the operation is
     * equivalent to a checkpoint operation.
     *
     * @param state node state
     * @param checkin if <code>true</code> the node is checked in.
     * @param checkout if <code>true</code> the node is checked out.
     * @param created create time of the new version (if any),
     *                or <code>null</code> for the current time
     * @return the node id of the base version or <code>null</code> for a pure
     *         checkout.
     * @throws RepositoryException if an error occurs
     */
    protected NodeId checkoutCheckin(
            NodeStateEx state,
            boolean checkin, boolean checkout, Calendar created)
            throws RepositoryException {
        assert(checkin || checkout);

        // check if versionable
        boolean isFull = checkVersionable(state);

        // check flags
        if (isCheckedOut(state)) {
            if (checkout && !checkin) {
                // pure checkout
                String msg = safeGetJCRPath(state) + ": Node is already checked-out. ignoring.";
                log.debug(msg);
                return null;
            }
        } else {
            if (!checkout) {
                // pure checkin
                String msg = safeGetJCRPath(state) + ": Node is already checked-in. ignoring.";
                log.debug(msg);
                if (isFull) {
                    return getBaseVersionId(state);
                } else {
                    // get base version from version history
                    return vMgr.getHeadVersionOfNode(state.getNodeId()).getId();
                }
            }
            checkin = false;
        }

        NodeId baseId = isFull && checkout
                ? vMgr.canCheckout(state, currentActivity)
                : null;

        // perform operation
        WriteOperation ops = startWriteOperation();
        try {
            // the 2 cases could be consolidated but is clearer this way
            if (checkin) {
                // check for configuration
                if (state.getEffectiveNodeType().includesNodeType(NameConstants.NT_CONFIGURATION)) {
                    // collect the base versions and the the rep:versions property of the configuration
                    Set<NodeId> baseVersions = collectBaseVersions(state);
                    InternalValue[] vs = new InternalValue[baseVersions.size()];
                    int i=0;
                    for (NodeId id: baseVersions) {
                        vs[i++] = InternalValue.create(id);
                    }
                    state.setPropertyValues(NameConstants.REP_VERSIONS, PropertyType.REFERENCE, vs);
                    state.store();
                }
                InternalVersion v = vMgr.checkin(session, state, created);
                baseId = v.getId();
                if (isFull) {
                    state.setPropertyValue(
                            NameConstants.JCR_BASEVERSION,
                            InternalValue.create(baseId));
                    state.setPropertyValues(NameConstants.JCR_PREDECESSORS, PropertyType.REFERENCE, InternalValue.EMPTY_ARRAY);
                    state.removeProperty(NameConstants.JCR_ACTIVITY);
                }
            }
            if (checkout) {
                if (isFull) {
                    state.setPropertyValues(
                            NameConstants.JCR_PREDECESSORS,
                            PropertyType.REFERENCE,
                            new InternalValue[]{InternalValue.create(baseId)}
                    );
                    if (currentActivity != null) {
                        state.setPropertyValue(
                                NameConstants.JCR_ACTIVITY,
                                InternalValue.create(currentActivity)
                        );
                    }
                }
            }
            state.setPropertyValue(NameConstants.JCR_ISCHECKEDOUT, InternalValue.create(checkout));
            state.store();
            ops.save();
            return baseId;
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        } finally {
            ops.close();
        }
    }

    /**
     * Collects the base versions for the workspace configuration referenced by
     * the given config node.
     * @param config the config
     * @return the id of the new base version
     * @throws RepositoryException if an error occurs
     */
    private Set<NodeId> collectBaseVersions(NodeStateEx config) throws RepositoryException {
        NodeId rootId = config.getPropertyValue(NameConstants.JCR_ROOT).getNodeId();
        NodeStateEx root = getNodeStateEx(rootId);
        if (root == null) {
            String msg = "Configuration root node for " + safeGetJCRPath(config) + " not found.";
            log.error(msg);
            throw new ItemNotFoundException(msg);
        }
        Set<NodeId> baseVersions = new HashSet<NodeId>();
        collectBaseVersions(root, baseVersions);
        return baseVersions;
    }

    /**
     * Recursively collects all base versions of this configuration tree.
     * 
     * @param root node to traverse
     * @param baseVersions set of base versions to fill
     * @throws RepositoryException if an error occurs
     */
    private void collectBaseVersions(NodeStateEx root, Set<NodeId> baseVersions)
            throws RepositoryException {
        if (!baseVersions.isEmpty()) {
            // base version of configuration root already recorded
            if (root.hasProperty(NameConstants.JCR_CONFIGURATION)
                    && root.getEffectiveNodeType().includesNodeType(NameConstants.MIX_VERSIONABLE)) {
                // don't traverse into child nodes that have a jcr:configuration
                // property as they belong to a different configuration.
                return;
            }
        }
        InternalVersion baseVersion = getBaseVersion(root);
        if (baseVersion.isRootVersion()) {
            String msg = "Unable to checkin configuration as it has unversioned child node: " + safeGetJCRPath(root);
            log.debug(msg);
            throw new UnsupportedRepositoryOperationException(msg);
        }
        baseVersions.add(baseVersion.getId());

        for (NodeStateEx child: root.getChildNodes()) {
            collectBaseVersions(child, baseVersions);
        }
    }

    /**
     * Checks if the underlying node is versionable, i.e. has 'mix:versionable' or a
     * 'mix:simpleVersionable'.
     * @param state node state
     * @return <code>true</code> if this node is full versionable, i.e. is
     *         of nodetype mix:versionable
     * @throws UnsupportedRepositoryOperationException if this node is not versionable at all
     */
    protected boolean checkVersionable(NodeStateEx state)
            throws UnsupportedRepositoryOperationException, RepositoryException {
        if (state.getEffectiveNodeType().includesNodeType(NameConstants.MIX_VERSIONABLE)) {
            return true;
        } else if (state.getEffectiveNodeType().includesNodeType(NameConstants.MIX_SIMPLE_VERSIONABLE)) {
            return false;
        } else {
            String msg = "Unable to perform a versioning operation on a " +
                         "non versionable node: " + safeGetJCRPath(state);
            log.debug(msg);
            throw new UnsupportedRepositoryOperationException(msg);
        }
    }

    /**
     * Returns the JCR path for the given node state without throwing an exception.
     * @param state node state
     * @return a JCR path string
     */
    protected String safeGetJCRPath(NodeStateEx state) {
        Path path;
        try {
            path = hierMgr.getPath(state.getNodeId());
        } catch (RepositoryException e) {
            log.warn("unable to calculate path for {}", state.getNodeId());
            return state.getNodeId().toString();
        }
        try {
            return session.getJCRPath(path);
        } catch (NamespaceException e) {
            log.warn("unable to calculate path for {}", path);
            return path.toString();
        }
    }

    /**
     * Determines the checked-out status of the given node state.
     * <p>
     * A node is considered <i>checked-out</i> if it is versionable and
     * checked-out, or is non-versionable but its nearest versionable ancestor
     * is checked-out, or is non-versionable and there are no versionable
     * ancestors.
     *
     * @param state node state
     * @return a boolean
     * @see javax.jcr.version.VersionManager#isCheckedOut(String)
     * @see Node#isCheckedOut()
     * @throws RepositoryException if an error occurs
     */
    protected boolean isCheckedOut(NodeStateEx state) throws RepositoryException {
        return state.getPropertyValue(NameConstants.JCR_ISCHECKEDOUT).getBoolean();
    }

    /**
     * Returns the node id of the base version, retrieved from the node state
     * @param state node state
     * @return the node id of the base version or <code>null</code> if not defined
     */
    protected NodeId getBaseVersionId(NodeStateEx state) {
        InternalValue value = state.getPropertyValue(NameConstants.JCR_BASEVERSION);
        return value == null ? null : value.getNodeId();
    }

    /**
     * Returns the internal version history for the underlying node.
     * @param state node state
     * @return internal version history
     * @throws RepositoryException if an error occurs
     */
    protected InternalVersionHistory getVersionHistory(NodeStateEx state)
            throws RepositoryException {
        boolean isFull = checkVersionable(state);
        if (isFull) {
            NodeId id = state.getPropertyValue(NameConstants.JCR_VERSIONHISTORY).getNodeId();
            return vMgr.getVersionHistory(id);
        } else {
            return vMgr.getVersionHistoryOfNode(state.getNodeId());
        }
    }

    /**
     * helper class that returns the internal version for a JCR one.
     * @param v the jcr version
     * @return the internal version
     * @throws RepositoryException if an error occurs
     */
    protected InternalVersion getVersion(Version v) throws RepositoryException {
        if (v == null) {
            return null;
        } else {
            return vMgr.getVersion(((VersionImpl) v).getNodeId());
        }
    }

    /**
     * Returns the internal base version for the underlying node.
     * @param state node state
     * @return internal base version
     * @throws RepositoryException if an error occurs
     */
    protected InternalVersion getBaseVersion(NodeStateEx state) throws RepositoryException {
        boolean isFull = checkVersionable(state);
        if (isFull) {
            NodeId id = getBaseVersionId(state);
            return vMgr.getVersion(id);
        } else {
            // note, that the method currently only works for linear version
            // graphs (i.e. simple versioning)
            return vMgr.getHeadVersionOfNode(state.getNodeId());
        }
    }

    /**
     * returns the node state for the given node id
     * @param nodeId the node id
     * @throws RepositoryException if an error occurs
     * @return the node state or null if not found
     */
    protected NodeStateEx getNodeStateEx(NodeId nodeId) throws RepositoryException {
        if (!stateMgr.hasItemState(nodeId)) {
            return null;
        }
        try {
            return new NodeStateEx(
                    stateMgr,
                    ntReg,
                    (NodeState) stateMgr.getItemState(nodeId),
                    null);
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Checks modify and permissions
     * @param state state to check
     * @param options options to check
     * @param permissions permissions to check
     * @throws RepositoryException if an error occurs
     */
    protected void checkModify(NodeStateEx state, int options, int permissions)
            throws RepositoryException {
        NodeImpl node;
        try {
            node = session.getNodeById(state.getNodeId());
        } catch (RepositoryException e) {
            // ignore
            return;
        }
        context.getItemValidator().checkModify(node, options, permissions);
    }

    /**
     * Checks modify and permissions
     * @param node node to check
     * @param options options to check
     * @param permissions permissions to check
     * @throws RepositoryException if an error occurs
     */
    protected void checkModify(NodeImpl node, int options, int permissions)
            throws RepositoryException {
        context.getItemValidator().checkModify(node, options, permissions);
    }

    /**
     * Helper for managing write operations.
     */
    public class WriteOperation {

        /**
         * Flag for successful completion of the write operation.
         */
        private boolean success = false;

        private final VersioningLock.WriteLock lock;

        public WriteOperation(VersioningLock.WriteLock lock) {
            this.lock = lock;
        }

        /**
         * Saves the pending operations in the {@link LocalItemStateManager}.
         *
         * @throws ItemStateException if the pending state is invalid
         * @throws RepositoryException if the pending state could not be persisted
         */
        public void save() throws ItemStateException, RepositoryException {
            stateMgr.update();
            success = true;
        }

        /**
         * Closes the write operation. The pending operations are cancelled
         * if they could not be properly saved. Finally the write lock is
         * released.
         */
        public void close() {
            try {
                if (!success) {
                    // update operation failed, cancel all modifications
                    stateMgr.cancel();
                }
            } finally {
                lock.release();
            }
        }
    }

    /**
     * Acquires the write lock on this version manager.
     * @return returns the write lock
     */
    protected VersioningLock.WriteLock acquireWriteLock() {
        while (true) {
            try {
                return rwLock.acquireWriteLock();
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    /**
     * acquires the read lock on this version manager.
     * @return returns the read lock
     */
    protected VersioningLock.ReadLock acquireReadLock() {
        while (true) {
            try {
                return rwLock.acquireReadLock();
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }


    /**
     * Starts a write operation by acquiring the write lock and setting the
     * item state manager to the "edit" state. If something goes wrong, the
     * write lock is released and an exception is thrown.
     * <p>
     * The pattern for using this method and the returned helper instance is:
     * <pre>
     *     WriteOperation operation = startWriteOperation();
     *     try {
     *         ...
     *         operation.save(); // if everything is OK
     *         ...
     *     } catch (...) {
     *         ...
     *     } finally {
     *         operation.close();
     *     }
     * </pre>
     *
     * @return write operation helper
     * @throws RepositoryException if the write operation could not be started
     */
    public WriteOperation startWriteOperation() throws RepositoryException {
        boolean success = false;
        VersioningLock.WriteLock lock = acquireWriteLock();
        try {
            stateMgr.edit();
            success = true;
            return new WriteOperation(lock);
        } catch (IllegalStateException e) {
            String msg = "Unable to start edit operation.";
            throw new RepositoryException(msg, e);
        } finally {
            if (!success) {
                lock.release();
            }
        }
    }

}