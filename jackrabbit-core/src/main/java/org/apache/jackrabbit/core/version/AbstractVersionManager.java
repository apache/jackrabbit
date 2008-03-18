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

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.ReentrantWriterPreferenceReadWriteLock;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.LocalItemStateManager;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.uuid.UUID;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.InvalidItemStateException;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

/**
 * Base implementation of the {@link VersionManager} interface.
 * <p/>
 * All read operations must aquire the read lock before reading, all write
 * operations must aquire the write lock.
 */
abstract class AbstractVersionManager implements VersionManager {

    /**
     * Logger instance.
     */
    private static Logger log = LoggerFactory.getLogger(AbstractVersionManager.class);

    /**
     * State manager for the version storage.
     */
    protected LocalItemStateManager stateMgr;

    /**
     * Node type registry.
     */
    protected final NodeTypeRegistry ntReg;

    /**
     * Persistent root node of the version histories.
     */
    protected NodeStateEx historyRoot;

    /**
     * the lock on this version manager
     */
    private final ReadWriteLock rwLock =
            new ReentrantWriterPreferenceReadWriteLock() {
                /**
                 * Allow reader when there is no active writer, or current
                 * thread owns the write lock (reentrant).
                 */
                protected boolean allowReader() {
                    return activeWriter_ == null
                        || activeWriter_ == Thread.currentThread();
                }
            };

    public AbstractVersionManager(NodeTypeRegistry ntReg) {
        this.ntReg = ntReg;
    }

    //-------------------------------------------------------< VersionManager >

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
    public InternalVersionHistory getVersionHistory(NodeId id)
            throws RepositoryException {
        // lock handling via getItem()
        return (InternalVersionHistory) getItem(id);
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasVersionHistory(NodeId id) {
        // lock handling via hasItem()
        return hasItem(id);
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasVersion(NodeId id) {
        // lock handling via hasItem()
        return hasItem(id);
    }

    //-------------------------------------------------------< implementation >

    /**
     * aquires the write lock on this version manager.
     */
    protected void acquireWriteLock() {
        while (true) {
            try {
                rwLock.writeLock().acquire();
                return;
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    /**
     * releases the write lock on this version manager.
     */
    protected void releaseWriteLock() {
        rwLock.writeLock().release();
    }

    /**
     * aquires the read lock on this version manager.
     */
    protected void acquireReadLock() {
        while (true) {
            try {
                rwLock.readLock().acquire();
                return;
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    /**
     * releases the read lock on this version manager.
     */
    protected void releaseReadLock() {
        rwLock.readLock().release();
    }

    /**
     * Helper for managing write operations.
     */
    private class WriteOperation {

        /**
         * Flag for successful completion of the write operation.
         */
        private boolean success = false;

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
                releaseWriteLock();
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
        acquireWriteLock();
        try {
            stateMgr.edit();
            success = true;
            return new WriteOperation();
        } catch (IllegalStateException e) {
            throw new RepositoryException("Unable to start edit operation.", e);
        } finally {
            if (!success) {
                releaseWriteLock();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public VersionHistory getVersionHistory(Session session, NodeState node)
            throws RepositoryException {
        acquireReadLock();
        try {
            NodeId vhId = getVersionHistoryId(node);
            if (vhId == null) {
                return null;
            }
            return (VersionHistory) ((SessionImpl) session).getNodeById(vhId);
        } finally {
            releaseReadLock();
        }
    }

    /**
     * Returns the item with the given persistent id. Subclass responsibility.
     * <p/>
     * Please note, that the overridden method must aquire the readlock before
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
     * that reference the given version item. Subclass responsiblity.
     * <p/>
     * Please note, that the overridden method must aquire the readlock before
     * reading the state manager.
     *
     * @param item version item
     * @return <code>true</code> if there are item references from outside the
     *         version storage; <code>false</code> otherwise.
     * @throws RepositoryException if an error occurs while reading from the
     *                             repository.
     */
    protected abstract boolean hasItemReferences(InternalVersionItem item)
            throws RepositoryException;

    /**
     * Creates a new Version History.
     *
     * @param node the node for which the version history is to be initialized
     * @return the newly created version history.
     * @throws javax.jcr.RepositoryException
     */
    InternalVersionHistory createVersionHistory(NodeState node)
            throws RepositoryException {
        WriteOperation operation = startWriteOperation();
        try {
            // create deep path
            String uuid = node.getNodeId().getUUID().toString();
            NodeStateEx root = historyRoot;
            for (int i = 0; i < 3; i++) {
                Name name = NameFactoryImpl.getInstance().create(Name.NS_DEFAULT_URI, uuid.substring(i * 2, i * 2 + 2));
                if (!root.hasNode(name)) {
                    root.addNode(name, NameConstants.REP_VERSIONSTORAGE, null, false);
                    root.store();
                }
                root = root.getNode(name, 1);
                if (root == null) {
                    throw new InvalidItemStateException();
                }
            }
            Name historyNodeName = NameFactoryImpl.getInstance().create(Name.NS_DEFAULT_URI, uuid);
            if (root.hasNode(historyNodeName)) {
                // already exists
                return null;
            }

            // create new history node in the persistent state
            InternalVersionHistoryImpl hist = InternalVersionHistoryImpl.create(
                    this, root, new NodeId(UUID.randomUUID()), historyNodeName, node);

            // end update
            operation.save();

            log.debug("Created new version history " + hist.getId() + " for " + node + ".");
            return hist;

        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        } finally {
            operation.close();
        }
    }

    /**
     * Returns the id of the version history associated with the given node
     * or <code>null</code> if that node doesn't have a version history.
     *
     * @param node the node whose version history's id is to be returned.
     * @return the the id of the version history associated with the given node
     *         or <code>null</code> if that node doesn't have a version history.
     * @throws javax.jcr.RepositoryException if an error occurs
     */
    private NodeId getVersionHistoryId(NodeState node)
            throws RepositoryException {
        // build and traverse path
        String uuid = node.getNodeId().getUUID().toString();
        NodeStateEx n = historyRoot;
        for (int i = 0; i < 3; i++) {
            Name name = NameFactoryImpl.getInstance().create(Name.NS_DEFAULT_URI, uuid.substring(i * 2, i * 2 + 2));
            if (!n.hasNode(name)) {
                return null;
            }
            n = n.getNode(name, 1);
        }
        Name historyNodeName = NameFactoryImpl.getInstance().create(Name.NS_DEFAULT_URI, uuid);
        if (!n.hasNode(historyNodeName)) {
            return null;
        }
        return n.getNode(historyNodeName, 1).getNodeId();
    }

    /**
     * Checks in a node
     *
     * @param history the version history
     * @param node node to checkin
     * @return internal version
     * @throws javax.jcr.RepositoryException if an error occurs
     * @see javax.jcr.Node#checkin()
     */
    protected InternalVersion checkin(InternalVersionHistoryImpl history, NodeImpl node)
            throws RepositoryException {
        WriteOperation operation = startWriteOperation();
        try {
            String versionName = calculateCheckinVersionName(history, node);
            InternalVersionImpl v = history.checkin(NameFactoryImpl.getInstance().create("", versionName), node);
            operation.save();
            return v;
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        } finally {
            operation.close();
        }
    }

    /**
     * Calculates the name of the new version that will be created by a
     * checkin call. The name is determined as follows:
     * <ul>
     * <li> first the predecessor version with the shortes name is searched.
     * <li> if that predecessor version is the root version, the new version gets
     *      the name "{number of successors}+1" + ".0"
     * <li> if that predecessor version has no successor, the last digit of it's
     *      version number is incremented.
     * <li> if that predecessor version has successors but the incremented name
     *      does not exist, that name is used.
     * <li> otherwise a ".0" is added to the name until a non conflicting name
     *      is found.
     * <ul>
     *
     * Example Graph:
     * <xmp>
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
     * </xmp>
     *
     * @param history the version history
     * @param node the node to checkin
     * @return the new version name
     * @throws RepositoryException if an error occurs.
     */
    protected String calculateCheckinVersionName(InternalVersionHistoryImpl history,
                                                 NodeImpl node)
            throws RepositoryException {
        // 1. search a predecessor, suitable for generating the new name
        Value[] values = node.getProperty(NameConstants.JCR_PREDECESSORS).getValues();
        InternalVersion best = null;
        for (int i = 0; i < values.length; i++) {
            InternalVersion pred = history.getVersion(NodeId.valueOf(values[i].getString()));
            if (best == null
                    || pred.getName().getLocalName().length() < best.getName().getLocalName().length()) {
                best = pred;
            }
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
            return String.valueOf(best.getSuccessors().length + 1) + ".0";
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
    protected void removeVersion(InternalVersionHistoryImpl history, Name name)
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
     * Set version label on the specified version.
     * @param history version history
     * @param version version name
     * @param label version label
     * @param move <code>true</code> to move from existing version;
     *             <code>false</code> otherwise
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
     * @param item
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
}
