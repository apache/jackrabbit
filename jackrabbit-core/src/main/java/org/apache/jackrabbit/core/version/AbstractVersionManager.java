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

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.state.DefaultISMLocking;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.LocalItemStateManager;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.ISMLocking.ReadLock;
import org.apache.jackrabbit.core.state.ISMLocking.WriteLock;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ItemNotFoundException;
import javax.jcr.version.VersionException;

/**
 * Base implementation of the {@link VersionManager} interface.
 * <p/>
 * All read operations must acquire the read lock before reading, all write
 * operations must acquire the write lock.
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
    private final DefaultISMLocking rwLock = new DefaultISMLocking();

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
    public InternalVersionHistory getVersionHistoryOfNode(NodeId id)
            throws RepositoryException {
        ReadLock lock = acquireReadLock();
        try {
            String uuid = id.getUUID().toString();
            Name name = getName(uuid);

            NodeStateEx parent = getParentNode(uuid, false);
            if (parent != null && parent.hasNode(name)) {
                NodeStateEx history = parent.getNode(name, 1);
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
     */
    protected WriteLock acquireWriteLock() {
        while (true) {
            try {
                return rwLock.acquireWriteLock(null);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    /**
     * acquires the read lock on this version manager.
     */
    protected ReadLock acquireReadLock() {
        while (true) {
            try {
                return rwLock.acquireReadLock(null);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    /**
     * Helper for managing write operations.
     */
    private class WriteOperation {

        /**
         * Flag for successful completion of the write operation.
         */
        private boolean success = false;

        private final WriteLock lock;

        public WriteOperation(WriteLock lock) {
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
        WriteLock lock = acquireWriteLock();
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
     * {@inheritDoc}
     */
    public VersionHistoryInfo getVersionHistory(Session session, NodeState node)
            throws RepositoryException {
        VersionHistoryInfo info = null;

        ReadLock lock = acquireReadLock();
        try {
            String uuid = node.getNodeId().getUUID().toString();
            Name name = getName(uuid);

            NodeStateEx parent = getParentNode(uuid, false);
            if (parent != null && parent.hasNode(name)) {
                NodeStateEx history = parent.getNode(name, 1);
                Name root = NameConstants.JCR_ROOTVERSION;
                info = new VersionHistoryInfo(
                        history.getNodeId(),
                        history.getState().getChildNodeEntry(root, 1).getId());
            }
        } finally {
            lock.release();
        }

        if (info == null) {
            info = createVersionHistory(session, node);
        }

        return info;
    }

    /**
     * Creates a new version history. This action is needed either when creating
     * a new 'mix:versionable' node or when adding the 'mix:versionable' mixin
     * to a node.
     *
     * @param session
     * @param node NodeState
     * @return identifier of the new version history node
     * @throws RepositoryException
     * @see #getVersionHistory(Session, NodeState)
     */
    protected abstract VersionHistoryInfo createVersionHistory(
            Session session, NodeState node) throws RepositoryException;

    /**
     * Returns the item with the given persistent id. Subclass responsibility.
     * <p/>
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
     * <p/>
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
     * <p/>
     * Please note, that the overridden method must acquire the readlock before
     * reading the state manager.
     *
     * @param id the id of the node
     * @throws RepositoryException if an error occurs while reading from the
     *                             repository.
     */
    protected abstract NodeStateEx getNodeStateEx(NodeId id)
            throws RepositoryException;

    /**
     * Creates a new Version History.
     *
     * @param node the node for which the version history is to be initialized
     * @return the identifiers of the newly created version history and root version
     * @throws javax.jcr.RepositoryException
     */
    NodeStateEx createVersionHistory(NodeState node)
            throws RepositoryException {
        WriteOperation operation = startWriteOperation();
        try {
            // create deep path
            String uuid = node.getNodeId().getUUID().toString();
            NodeStateEx parent = getParentNode(uuid, true);
            Name name = getName(uuid);
            if (parent.hasNode(name)) {
                // already exists
                return null;
            }

            // create new history node in the persistent state
            NodeStateEx history =
                InternalVersionHistoryImpl.create(this, parent, name, node);

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
     * Utility method that returns the given string as a name in the default
     * namespace.
     *
     * @param name string name
     * @return qualified name
     */
    private Name getName(String name) {
        return NameFactoryImpl.getInstance().create(Name.NS_DEFAULT_URI, name);
    }

    /**
     * Utility method that returns the parent node under which the version
     * history of the identified versionable node is or will be stored. If
     * the create flag is set, then the returned parent node and any ancestor
     * nodes are automatically created if they do not already exist. Otherwise
     * <code>null</code> is returned if the parent node does not exist.
     *
     * @param uuid UUID of a versionable node
     * @param create whether to create missing nodes
     * @return parent node of the version history, or <code>null</code>
     * @throws RepositoryException if an error occurs
     */
    private NodeStateEx getParentNode(String uuid, boolean create)
            throws RepositoryException {
        NodeStateEx n = historyRoot;
        for (int i = 0; i < 3; i++) {
            Name name = getName(uuid.substring(i * 2, i * 2 + 2));
            if (n.hasNode(name)) {
                n = n.getNode(name, 1);
            } else if (create) {
                n.addNode(name, NameConstants.REP_VERSIONSTORAGE, null, false);
                n.store();
                n = n.getNode(name, 1);
            } else {
                return null;
            }
        }
        return n;
    }

    /**
     * Checks in a node
     *
     * @param history the version history
     * @param node node to checkin
     * @param simple flag indicates simple versioning
     * @param cal create time of the new version, or <code>null</code>
     * @return internal version
     * @throws javax.jcr.RepositoryException if an error occurs
     * @see javax.jcr.Node#checkin()
     */
    protected InternalVersion checkin(InternalVersionHistoryImpl history,
            NodeImpl node, boolean simple, Calendar cal)
            throws RepositoryException {
        WriteOperation operation = startWriteOperation();
        try {
            String versionName = calculateCheckinVersionName(history, node, simple);
            InternalVersionImpl v = history.checkin(
                NameFactoryImpl.getInstance().create("", versionName), node, cal);
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
     * @param simple if <code>true</code> indicates simple versioning
     * @return the new version name
     * @throws RepositoryException if an error occurs.
     */
    protected String calculateCheckinVersionName(InternalVersionHistoryImpl history,
                                                 NodeImpl node, boolean simple)
            throws RepositoryException {
        InternalVersion best = null;
        if (simple) {
            // 1. in simple versioning just take the 'head' version
            Name[] names = history.getVersionNames();
            best = history.getVersion(names[names.length - 1]);
        } else {
            // 1. search a predecessor, suitable for generating the new name
            Value[] values = node.getProperty(NameConstants.JCR_PREDECESSORS).getValues();
            for (int i = 0; i < values.length; i++) {
                InternalVersion pred = history.getVersion(NodeId.valueOf(values[i].getString()));
                if (best == null
                        || pred.getName().getLocalName().length() < best.getName().getLocalName().length()) {
                    best = pred;
                }
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
