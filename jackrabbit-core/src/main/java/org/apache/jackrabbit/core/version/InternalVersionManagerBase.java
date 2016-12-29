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

import static org.apache.jackrabbit.spi.commons.name.NameConstants.JCR_ACTIVITY;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.JCR_ROOTVERSION;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.JCR_VERSIONHISTORY;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.MIX_VERSIONABLE;

import java.util.Calendar;

import javax.jcr.ItemNotFoundException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.NodeIdFactory;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.LocalItemStateManager;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base implementation of the {@link InternalVersionManager} interface.
 * <p>
 * All read operations must acquire the read lock before reading, all write
 * operations must acquire the write lock.
 */
abstract class InternalVersionManagerBase implements InternalVersionManager {

    /**
     * Logger instance.
     */
    private static Logger log = LoggerFactory.getLogger(InternalVersionManagerBase.class);

    /**
     * State manager for the version storage.
     */
    protected LocalItemStateManager stateMgr;

    /**
     * Node type registry.
     */
    protected final NodeTypeRegistry ntReg;

    protected final NodeId historiesId;

    protected final NodeId activitiesId;

    /**
     * the lock on this version manager
     */
    private final VersioningLock rwLock = new VersioningLock();

    private final NodeIdFactory nodeIdFactory;

    protected InternalVersionManagerBase(NodeTypeRegistry ntReg,
                                         NodeId historiesId,
                                         NodeId activitiesId,
                                         NodeIdFactory nodeIdFactory) {
        this.ntReg = ntReg;
        this.historiesId = historiesId;
        this.activitiesId = activitiesId;
        this.nodeIdFactory = nodeIdFactory;
    }

//-------------------------------------------------------< InternalVersionManager >

    /**
     * {@inheritDoc}
     */
    public InternalVersion getVersion(NodeId id) throws RepositoryException {
        // lock handling via getItem()
        InternalVersion v = (InternalVersion) getItem(id);
        if (v == null) {
            log.warn("Versioning item not found: " + id);
        }
        return v;
    }

    /**
     * {@inheritDoc}
     */
    public InternalBaseline getBaseline(NodeId id) throws RepositoryException {
        // lock handling via getItem()
        InternalVersionItem item = getItem(id);
        if (item == null) {
            log.warn("Versioning item not found: " + id);
        } else if (!(item instanceof InternalBaseline)) {
            log.warn("Versioning item is not a baseline: " + id);
            item = null;
        }
        return (InternalBaseline) item;
    }

    /**
     * {@inheritDoc}
     */
    public InternalActivity getActivity(NodeId id) throws RepositoryException {
        // lock handling via getItem()
        InternalActivity v = (InternalActivity) getItem(id);
        if (v == null) {
            log.warn("Versioning item not found: " + id);
        }
        return v;
    }

    /**
     * {@inheritDoc}
     */
    public InternalVersionHistory getVersionHistory(NodeId id)
            throws RepositoryException {
        // lock handling via getItem()
        return (InternalVersionHistory) getItem(id);
    }

    /**
     * {@inheritDoc}
     */
    public InternalVersionHistory getVersionHistoryOfNode(NodeId id)
            throws RepositoryException {
        VersioningLock.ReadLock lock = acquireReadLock();
        try {
            String uuid = id.toString();
            Name name = getName(uuid);

            NodeStateEx parent = getParentNode(getHistoryRoot(), uuid, null);
            if (parent != null && parent.hasNode(name)) {
                NodeStateEx history = parent.getNode(name, 1);
                if (history == null) {
                    throw new InconsistentVersioningState("Unexpected failure to get child node " + name + " on parent node" + parent.getNodeId());
                }
                return getVersionHistory(history.getNodeId());
            } else {
                throw new ItemNotFoundException("Version history of node " + id + " not found.");
            }
        } finally {
            lock.release();
        }
    }

    /**
     * {@inheritDoc}
     *
     * Assumes that all versions are stored chronologically below the version
     * history and just returns the last one. i.e. currently only works for
     * simple versioning.
     */
    public InternalVersion getHeadVersionOfNode(NodeId id) throws RepositoryException {
        InternalVersionHistory vh = getVersionHistoryOfNode(id);
        Name[] names = vh.getVersionNames();
        InternalVersion last = vh.getVersion(names[names.length - 1]);
        return getVersion(last.getId());
    }

    //-------------------------------------------------------< implementation >

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
    public VersioningLock.ReadLock acquireReadLock() {
        while (true) {
            try {
                return rwLock.acquireReadLock();
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    /**
     * returns the id of the version history root node
     *
     * @return the id of the version history root node
     */
    abstract protected NodeStateEx getHistoryRoot();

    /**
     * returns the id of the activities root node
     *
     * @return the id of the activities root node
     */
    abstract protected NodeStateEx getActivitiesRoot();


    /**
     * Helper for managing write operations.
     */
    private class WriteOperation {

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
         * Closes the write operation. The pending operations are canceled
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
    private WriteOperation startWriteOperation() throws RepositoryException {
        boolean success = false;
        VersioningLock.WriteLock lock = acquireWriteLock();
        try {
            stateMgr.edit();
            success = true;
            return new WriteOperation(lock);
        } catch (IllegalStateException e) {
            throw new RepositoryException("Unable to start edit operation.", e);
        } finally {
            if (!success) {
                lock.release();
            }
        }
    }

    /**
     * Returns information about the version history of the specified node
     * or <code>null</code> when unavailable.
     *
     * @param node node whose version history should be returned
     * @return identifiers of the version history and root version nodes
     * @throws RepositoryException if an error occurs
     */
    public VersionHistoryInfo getVersionHistoryInfoForNode(NodeState node) throws RepositoryException {
        VersionHistoryInfo info = null;

        VersioningLock.ReadLock lock = acquireReadLock();
        try {
            String uuid = node.getNodeId().toString();
            Name name = getName(uuid);

            NodeStateEx parent = getParentNode(getHistoryRoot(), uuid, null);
            if (parent != null && parent.hasNode(name)) {
                NodeStateEx history = parent.getNode(name, 1);
                if (history == null) {
                    throw new InconsistentVersioningState("Unexpected failure to get child node " + name + " on parent node " + parent.getNodeId());
                }
                ChildNodeEntry rootv = history.getState().getChildNodeEntry(JCR_ROOTVERSION, 1);
                if (rootv == null) {
                    throw new InconsistentVersioningState("missing child node entry for " + JCR_ROOTVERSION + " on version history node " + history.getNodeId(),
                            history.getNodeId(), null);
                }
                info = new VersionHistoryInfo(history.getNodeId(),
                        rootv.getId());
            }
        } finally {
            lock.release();
        }

        return info;
    }

    /**
     * {@inheritDoc}
     */
    public VersionHistoryInfo getVersionHistory(Session session, NodeState node,
                                                NodeId copiedFrom)
            throws RepositoryException {
        VersionHistoryInfo info = getVersionHistoryInfoForNode(node);

        if (info == null) {
            info = createVersionHistory(session, node, copiedFrom);
        }

        return info;
    }

    /**
     * Creates a new version history. This action is needed either when creating
     * a new 'mix:versionable' node or when adding the 'mix:versionable' mixin
     * to a node.
     *
     * @param session repository session
     * @param node versionable node state
     * @param copiedFrom node id for the jcr:copiedFrom property
     * @return identifier of the new version history node
     * @throws RepositoryException if an error occurs
     * @see #getVersionHistory(Session, NodeState, NodeId)
     */
    protected abstract VersionHistoryInfo createVersionHistory(Session session,
                                                               NodeState node,
                                                               NodeId copiedFrom)
            throws RepositoryException;

    /**
     * Returns the item with the given persistent id. Subclass responsibility.
     * <p>
     * Please note, that the overridden method must acquire the readlock before
     * reading the state manager.
     *
     * @param id the id of the item
     * @return version item
     * @throws RepositoryException if an error occurs
     */
    protected abstract InternalVersionItem getItem(NodeId id)
            throws RepositoryException;

    /**
     * Return a flag indicating if the item specified exists.
     * Subclass responsibility.
     * @param id the id of the item
     * @return <code>true</code> if the item exists;
     *         <code>false</code> otherwise
     */
    protected abstract boolean hasItem(NodeId id);

    /**
     * Checks if there are item references (from outside the version storage)
     * that reference the given node. Subclass responsibility.
     * <p>
     * Please note, that the overridden method must acquire the readlock before
     * reading the state manager.
     *
     * @param id the id of the node
     * @return <code>true</code> if there are item references from outside the
     *         version storage; <code>false</code> otherwise.
     * @throws RepositoryException if an error occurs while reading from the
     *                             repository.
     */
    protected abstract boolean hasItemReferences(NodeId id)
            throws RepositoryException;

    /**
     * Returns the node with the given persistent id. Subclass responsibility.
     * <p>
     * Please note, that the overridden method must acquire the readlock before
     * reading the state manager.
     *
     * @param id the id of the node
     * @throws RepositoryException if an error occurs while reading from the
     *                             repository.
     * @return the nodestate for the given id.
     */
    protected abstract NodeStateEx getNodeStateEx(NodeId id)
            throws RepositoryException;

    /**
     * Creates a new Version History.
     *
     * @param node the node for which the version history is to be initialized
     * @param copiedFrom node id for the jcr:copiedFrom parameter
     * @return the identifiers of the newly created version history and root version
     * @throws RepositoryException if an error occurs
     */
    NodeStateEx internalCreateVersionHistory(NodeState node, NodeId copiedFrom)
            throws RepositoryException {
        WriteOperation operation = startWriteOperation();
        try {
            // create deep path
            String uuid = node.getNodeId().toString();
            NodeStateEx parent = getParentNode(getHistoryRoot(), uuid, NameConstants.REP_VERSIONSTORAGE);
            Name name = getName(uuid);
            if (parent.hasNode(name)) {
                // already exists
                return null;
            }

            // create new history node in the persistent state
            NodeStateEx history =
                InternalVersionHistoryImpl.create(this, parent, name, node, copiedFrom);

            // end update
            operation.save();

            log.debug(
                    "Created new version history " + history.getNodeId()
                    + " for " + node + ".");
            return history;
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        } finally {
            operation.close();
        }
    }

    /**
     * Creates a new activity.
     *
     * @param title title of the new activity
     * @return the id of the newly created activity
     * @throws RepositoryException if an error occurs
     */
    NodeStateEx internalCreateActivity(String title)
            throws RepositoryException {
        WriteOperation operation = startWriteOperation();
        try {
            // create deep path
            NodeId activityId = nodeIdFactory.newNodeId();
            NodeStateEx parent = getParentNode(getActivitiesRoot(), activityId.toString(), NameConstants.REP_ACTIVITIES);
            Name name = getName(activityId.toString());

            // create new activity node in the persistent state
            NodeStateEx pNode = InternalActivityImpl.create(parent, name, activityId, title);

            // end update
            operation.save();

            log.debug("Created new activity " + activityId
                    + " with title " + title + ".");
            return pNode;
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        } finally {
            operation.close();
        }
    }

    /**
     * Removes the specified activity
     *
     * @param activity the activity to remove
     * @throws javax.jcr.RepositoryException if any other error occurs.
     */
    protected void internalRemoveActivity(InternalActivityImpl activity)
            throws RepositoryException {
        WriteOperation operation = startWriteOperation();
        try {
            // check if the activity has any references in the workspaces
            NodeId nodeId = activity.getId();
            if (stateMgr.hasNodeReferences(nodeId)) {
                NodeReferences refs = stateMgr.getNodeReferences(nodeId);
                if (refs.hasReferences()) {
                    throw new ReferentialIntegrityException("Unable to delete activity. still referenced.");
                }
            }
            // TODO:
            // check if the activity is used in anywhere in the version storage
            // and reject removal

            // remove activity and possible empty parent directories
            NodeStateEx act = getNodeStateEx(nodeId);
            NodeId parentId = act.getParentId();
            Name name = act.getName();
            while (parentId != null) {
                NodeStateEx parent = getNodeStateEx(parentId);
                parent.removeNode(name);
                parent.store();
                if (parent.getChildNodes().length == 0 && !parentId.equals(activitiesId)) {
                    name = parent.getName();
                    parentId = parent.getParentId();
                } else {
                    parentId = null;
                }
            }
            operation.save();
        } catch (ItemStateException e) {
            log.error("Error while storing: " + e.toString());
        } finally {
            operation.close();
        }
    }

    /**
     * Utility method that returns the given string as a name in the default
     * namespace.
     *
     * @param name string name
     * @return A <code>Name</code> object.
     */
    protected static Name getName(String name) {
        return NameFactoryImpl.getInstance().create(Name.NS_DEFAULT_URI, name);
    }

    /**
     * Utility method that returns the parent node under which the version
     * history of the identified versionable node is or will be stored. If
     * the <code>interNT</code> is not <code>null</code> then the returned
     * parent node and any ancestor nodes are automatically created if they do
     * not already exist. Otherwise
     * <code>null</code> is returned if the parent node does not exist.
     *
     * @param parent the parent node
     * @param uuid UUID of a versionable node
     * @param interNT intermediate nodetype.
     * @return parent node of the version history, or <code>null</code>
     * @throws RepositoryException if an error occurs
     */
    protected static NodeStateEx getParentNode(NodeStateEx parent, String uuid, Name interNT)
            throws RepositoryException {
        NodeStateEx n = parent;
        for (int i = 0; i < 3; i++) {
            Name name = getName(uuid.substring(i * 2, i * 2 + 2));
            NodeStateEx childn = n.getNode(name, 1);
            if (childn != null) {
                n = childn;
            } else if (interNT != null) {
                childn = n.addNode(name, interNT, null, false);
                n.store(false);
                childn.store(true);
                n = childn;
            } else {
                return null;
            }
        }
        return n;
    }

    /**
     * Creates a new version of the given node using the given version
     * creation time.
     *
     * @param node the node to be checked in
     * @param created version creation time
     * @return the new version
     * @throws RepositoryException if an error occurs
     */
    protected InternalVersion checkin(NodeStateEx node, Calendar created)
            throws RepositoryException {
        WriteOperation operation = startWriteOperation();
        try {
            boolean simple =
                !node.getEffectiveNodeType().includesNodeType(MIX_VERSIONABLE);
            InternalVersionHistoryImpl vh;
            if (simple) {
                // in simple versioning the history id needs to be calculated
                vh = (InternalVersionHistoryImpl) getVersionHistoryOfNode(
                        node.getNodeId());
            } else {
                // in full versioning, the history id can be retrieved via
                // the property
                vh = (InternalVersionHistoryImpl) getVersionHistory(
                        node.getPropertyValue(JCR_VERSIONHISTORY).getNodeId());
            }

            InternalVersion version =
                internalCheckin(vh, node, simple, created);

            operation.save();
            return version;
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        } finally {
            operation.close();
        }
    }

    /**
     * Checks in a node
     *
     * @param history the version history
     * @param node node to checkin
     * @param simple flag indicates simple versioning
     * @param created optional created date.
     * @return internal version
     * @throws javax.jcr.RepositoryException if an error occurs
     * @see javax.jcr.Node#checkin()
     */
    protected InternalVersion internalCheckin(
            InternalVersionHistoryImpl history,
            NodeStateEx node, boolean simple, Calendar created)
            throws RepositoryException {
        String versionName = calculateCheckinVersionName(history, node, simple);
        InternalVersionImpl v = history.checkin(
                NameFactoryImpl.getInstance().create("", versionName),
                node, created);

        // check for jcr:activity
        if (node.hasProperty(JCR_ACTIVITY)) {
            NodeId actId = node.getPropertyValue(JCR_ACTIVITY).getNodeId();
            InternalActivityImpl act = (InternalActivityImpl) getItem(actId);
            act.addVersion(v);
        }

        return v;
    }

    /**
     * Calculates the name of the new version that will be created by a
     * checkin call. The name is determined as follows:
     * <ul>
     * <li> first the predecessor version with the shortest name is searched.
     * <li> if that predecessor version is the root version, the new version gets
     *      the name "{number of successors}+1" + ".0"
     * <li> if that predecessor version has no successor, the last digit of it's
     *      version number is incremented.
     * <li> if that predecessor version has successors but the incremented name
     *      does not exist, that name is used.
     * <li> otherwise a ".0" is added to the name until a non conflicting name
     *      is found.
     * </ul>
     *
     * Example Graph:
     * <pre>
     * jcr:rootVersion
     *  |     |
     * 1.0   2.0
     *  |
     * 1.1
     *  |
     * 1.2 ---\  ------\
     *  |      \        \
     * 1.3   1.2.0   1.2.0.0
     *  |      |
     * 1.4   1.2.1 ----\
     *  |      |        \
     * 1.5   1.2.2   1.2.1.0
     *  |      |        |
     * 1.6     |     1.2.1.1
     *  |-----/
     * 1.7
     * </pre>
     *
     * @param history the version history
     * @param node the node to checkin
     * @param simple if <code>true</code> indicates simple versioning
     * @return the new version name
     * @throws RepositoryException if an error occurs.
     */
    protected String calculateCheckinVersionName(InternalVersionHistoryImpl history,
                                                 NodeStateEx node, boolean simple)
            throws RepositoryException {

        if (history == null) {
            String message = "Node " + node.getNodeId() + " has no version history";
            log.error(message);
            throw new VersionException(message);
        }

        InternalVersion best = null;
        if (simple) {
            // 1. in simple versioning just take the 'head' version
            Name[] names = history.getVersionNames();
            best = history.getVersion(names[names.length - 1]);
        } else {
            // 1. search a predecessor, suitable for generating the new name
            InternalValue[] values = node.getPropertyValues(NameConstants.JCR_PREDECESSORS);

            if (values == null || values.length == 0) {
                String message;
                if (values == null) {
                    message = "Mandatory jcr:predecessors property missing on node " + node.getNodeId();
                } else {
                    message = "Mandatory jcr:predecessors property is empty on node " + node.getNodeId();
                }
                log.error(message);
                throw new VersionException(message);
            }

            for (InternalValue value: values) {
                InternalVersion pred = history.getVersion(value.getNodeId());
                if (pred == null) {
                    String message = "Could not instantiate InternalVersion for nodeId " + value.getNodeId() + " (VHR + " + history.getId() + ", node " + node.getNodeId() + ")";
                    log.error(message);
                    throw new VersionException(message);
                }
                if (best == null
                        || pred.getName().getLocalName().length() < best.getName().getLocalName().length()) {
                    best = pred;
                }
            }
        }

        if (best == null) {
            String message = "Could not find 'best' predecessor node for " + node.getNodeId();
            log.error(message);
            throw new VersionException(message);
        }

        // 2. generate version name (assume no namespaces in version names)
        String versionName = best.getName().getLocalName();
        int pos = versionName.lastIndexOf('.');
        if (pos > 0) {
            String newVersionName = versionName.substring(0, pos + 1)
                + (Integer.parseInt(versionName.substring(pos + 1)) + 1);
            while (history.hasVersion(NameFactoryImpl.getInstance().create("", newVersionName))) {
                versionName += ".0";
                newVersionName = versionName;
            }
            return newVersionName;
        } else {
            // best is root version
            return String.valueOf(best.getSuccessors().size() + 1) + ".0";
        }
    }

    /**
     * Removes the specified version from the history
     *
     * @param history the version history from where to remove the version.
     * @param name the name of the version to remove.
     * @throws javax.jcr.version.VersionException if the version <code>history</code> does
     *  not have a version with <code>name</code>.
     * @throws javax.jcr.RepositoryException if any other error occurs.
     */
    protected void internalRemoveVersion(InternalVersionHistoryImpl history, Name name)
            throws VersionException, RepositoryException {
        WriteOperation operation = startWriteOperation();
        try {
            history.removeVersion(name);
            operation.save();
        } catch (ItemStateException e) {
            log.error("Error while storing: " + e.toString());
        } finally {
            operation.close();
        }
    }

    /**
     * Removes the specified history from the storage
     *
     * @param history the version history to remove
     * @throws VersionException
     * @throws RepositoryException
     */
    public void internalRemoveVersionHistory(InternalVersionHistoryImpl history)
            throws VersionException, RepositoryException {
        String versionableUuid = history.getVersionableId().toString();
        WriteOperation operation = startWriteOperation();
        try {
            NodeStateEx parent = getParentNode(getHistoryRoot(), versionableUuid, null);
            parent.removeNode(history.node.getName());
            parent.store();
            operation.save();
        } catch (ItemStateException e) {
            log.error("Error while storing: " + e.toString());
        } finally {
            operation.close();
        }
    }

    /**
     * Set version label on the specified version.
     *
     * @param history version history
     * @param version version name
     * @param label version label
     * @param move <code>true</code> to move from existing version;
     *             <code>false</code> otherwise.
     * @return The internal version.
     * @throws RepositoryException if an error occurs
     */
    protected InternalVersion setVersionLabel(InternalVersionHistoryImpl history,
                                              Name version, Name label,
                                              boolean move)
            throws RepositoryException {
        WriteOperation operation = startWriteOperation();
        try {
            InternalVersion v = history.setVersionLabel(version, label, move);
            operation.save();
            return v;
        } catch (ItemStateException e) {
            log.error("Error while storing: " + e.toString());
            return null;
        } finally {
            operation.close();
        }
    }

    /**
     * Invoked when a new internal item has been created.
     * @param version internal version item
     */
    protected void versionCreated(InternalVersion version) {
    }

    /**
     * Invoked when a new internal item has been destroyed.
     * @param version internal version item
     */
    protected void versionDestroyed(InternalVersion version) {
    }

    /**
     * Invoked by the internal version item itself, when it's underlying
     * persistence state was discarded.
     *
     * @param item item that was discarded
     */
    protected void itemDiscarded(InternalVersionItem item) {
    }

    /**
     * Creates an {@link InternalVersionItem} based on the {@link NodeState}
     * identified by <code>id</code>.
     *
     * @param id    the node id of the version item.
     * @return the version item or <code>null</code> if there is no node state
     *         with the given <code>id</code>.
     * @throws RepositoryException if an error occurs while reading from the
     *                             version storage.
     */
    protected InternalVersionItem createInternalVersionItem(NodeId id)
            throws RepositoryException {
        try {
            if (stateMgr.hasItemState(id)) {
                NodeState state = (NodeState) stateMgr.getItemState(id);
                NodeStateEx pNode = new NodeStateEx(stateMgr, ntReg, state, null);
                NodeId parentId = pNode.getParentId();
                InternalVersionItem parent = getItem(parentId);
                Name ntName = state.getNodeTypeName();
                if (ntName.equals(NameConstants.NT_FROZENNODE)) {
                    return new InternalFrozenNodeImpl(this, pNode, parent);
                } else if (ntName.equals(NameConstants.NT_VERSIONEDCHILD)) {
                    return new InternalFrozenVHImpl(this, pNode, parent);
                } else if (ntName.equals(NameConstants.NT_VERSION)) {
                    return ((InternalVersionHistory) parent).getVersion(id);
                } else if (ntName.equals(NameConstants.NT_VERSIONHISTORY)) {
                    return new InternalVersionHistoryImpl(this, pNode);
                } else if (ntName.equals(NameConstants.NT_ACTIVITY)) {
                    return new InternalActivityImpl(this, pNode);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        }
    }

    public NodeIdFactory getNodeIdFactory() {
        return nodeIdFactory;
    }

}
