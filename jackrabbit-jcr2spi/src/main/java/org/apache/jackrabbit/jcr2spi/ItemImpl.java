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
package org.apache.jackrabbit.jcr2spi;

import java.util.Collections;
import java.util.Map;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.commons.collections4.map.AbstractReferenceMap.ReferenceStrength;
import org.apache.commons.collections4.map.ReferenceMap;
import org.apache.jackrabbit.jcr2spi.config.CacheBehaviour;
import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.jcr2spi.operation.Operation;
import org.apache.jackrabbit.jcr2spi.operation.Remove;
import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.ItemStateLifeCycleListener;
import org.apache.jackrabbit.jcr2spi.state.ItemStateValidator;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.Status;
import org.apache.jackrabbit.jcr2spi.util.LogUtil;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>ItemImpl</code>...
 */
public abstract class ItemImpl implements Item, ItemStateLifeCycleListener {

    private static Logger log = LoggerFactory.getLogger(ItemImpl.class);

    private final ItemState state;

    /**
     * The session that created this item.
     */
    protected SessionImpl session;

    /**
     * Listeners (weak references)
     */
    protected final Map<ItemLifeCycleListener, ItemLifeCycleListener> listeners =
        Collections.synchronizedMap(new ReferenceMap<>(ReferenceStrength.WEAK, ReferenceStrength.WEAK));

    public ItemImpl(SessionImpl session, ItemState state,
                    ItemLifeCycleListener[] listeners) {
        this.session = session;
        this.state = state;

        if (listeners != null) {
            for (int i = 0; i < listeners.length; i++) {
                addLifeCycleListener(listeners[i]);
            }
        }
        notifyCreated();

        // add this item as listener to events of the underlying state object
        state.addListener(this);
    }

    //-----------------------------------------------------< Item interface >---
    /**
     * @see javax.jcr.Item#getPath()
     */
    public String getPath() throws RepositoryException {
        checkStatus();
        return session.getPathResolver().getJCRPath(getQPath());
    }

    /**
     * @see javax.jcr.Item#getName()
     */
    public abstract String getName() throws RepositoryException;

    /**
     * @see javax.jcr.Item#getAncestor(int)
     */
    public Item getAncestor(int depth) throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        checkStatus();
        if (depth == 0) {
            return session.getRootNode();
        }
        String msg = "No ancestor at depth = " + depth;
        try {
            // Path.getAncestor requires relative degree, i.e. we need
            // to convert absolute to relative ancestor degree
            Path path = getQPath();
            int relDegree = path.getAncestorCount() - depth;
            if (relDegree < 0) {
                throw new ItemNotFoundException(msg);
            }
            Path ancestorPath = path.getAncestor(relDegree);
            if (relDegree == 0) {
                return this;
            } else {
                return getItemManager().getNode(ancestorPath);
            }
        } catch (PathNotFoundException e) {
            throw new ItemNotFoundException(msg);
        }
    }

    /**
     * @see Item#getParent()
     */
    public Node getParent() throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        checkStatus();

        // special treatment for root node
        if (state.isNode() && ((NodeState)state).isRoot()) {
            String msg = "Root node doesn't have a parent.";
            log.debug(msg);
            throw new ItemNotFoundException(msg);
        }

        NodeEntry parentEntry = getItemState().getHierarchyEntry().getParent();
        return (Node) getItemManager().getItem(parentEntry);
    }

    /**
     * @see javax.jcr.Item#getDepth()
     */
    public int getDepth() throws RepositoryException {
        checkStatus();
        if (state.isNode() && ((NodeState)state).isRoot()) {
            // shortcut
            return Path.ROOT_DEPTH;
        }
        return session.getHierarchyManager().getDepth(state.getHierarchyEntry());
    }

    /**
     * Note: as of 2.x this method returns the session irrespective of the item's
     * status.
     * 
     * @see javax.jcr.Item#getSession()
     * @see <a href="http://issues.apache.org/jira/browse/JCR-2529">Issue JCR-2529</a>
     */
    public Session getSession() throws RepositoryException {
        return session;
    }

    /**
     * @see javax.jcr.Item#isNew()
     */
    public boolean isNew() {
        return state.getStatus() == Status.NEW;
    }

    /**
     * @see javax.jcr.Item#isModified()
     */
    public boolean isModified() {
        return state.getStatus() == Status.EXISTING_MODIFIED;
    }

    /**
     * @see javax.jcr.Item#isSame(Item)
     */
    public boolean isSame(Item otherItem) throws RepositoryException {
        checkStatus();
        if (this == otherItem) {
            return true;
        }
        if (isNode() != otherItem.isNode()) {
            return false;
        }
        if (otherItem instanceof ItemImpl) {
            ItemImpl other = (ItemImpl) otherItem;
            if (this.state == other.state) {
                return true;
            }
            // check status of the other item.
            other.checkStatus();

            // 2 items may only be the same if the were accessed from Sessions
            // bound to the same workspace
            String otherWspName = other.session.getWorkspace().getName();
            if (session.getWorkspace().getName().equals(otherWspName)) {
                // in addition they must provide the same id irrespective of
                // any transient modifications.
                if (state.getStatus() != Status.NEW && other.state.getStatus() != Status.NEW ) {
                    // if any ancestor is _invalidated_ force it's reload in
                    // order to detect id changes.
                    updateId(state);
                    updateId(other.state);
                    return state.getWorkspaceId().equals(other.state.getWorkspaceId());
                }
                /* else:
                - if both wsp-states are null, the items are both transiently
                  added and are only the same if they are obtained from the same
                  session. in this case, their states must be the same object,
                  which is covered above.
                - either of the two items does not have a workspace state.
                  therefore the items cannot be the same, since one has been
                  transiently added in one but not the other session.
                */
            }
        }
        return false;
    }

    /**
     * @see javax.jcr.Item#accept(ItemVisitor)
     */
    public abstract void accept(ItemVisitor visitor) throws RepositoryException;

    /**
     * @see javax.jcr.Item#isNode()
     */
    public abstract boolean isNode();

    /**
     * @see javax.jcr.Item#save()
     */
    public void save() throws AccessDeniedException, ConstraintViolationException, InvalidItemStateException, ReferentialIntegrityException, VersionException, LockException, RepositoryException {
        // check state of this instance
        checkStatus();
        session.getSessionItemStateManager().save(getItemState());
    }

    /**
     * @see javax.jcr.Item#refresh(boolean)
     */
    public void refresh(boolean keepChanges) throws InvalidItemStateException, RepositoryException {
        // check session status
        session.checkIsAlive();
        int status = state.getStatus();
        // check if item has been removed by this or another session
        if (Status.isTerminal(status) || Status.EXISTING_REMOVED == status) {
            throw new InvalidItemStateException("Item '" + this + "' doesn't exist anymore");
        }

        /* If 'keepChanges' is true, items that do not have changes pending have
           their state refreshed to reflect the current saved state */
        if (keepChanges) {
            if (status != Status.NEW  &&
                session.getCacheBehaviour() != CacheBehaviour.OBSERVATION) {
                // merge current transient modifications with latest changes
                // from the 'server'.
                // Note, that with Observation-CacheBehaviour no manual refresh
                // is required. changes get pushed automatically.
                state.getHierarchyEntry().invalidate(true);
            }
        } else {
            // check status of item state
            if (status == Status.NEW) {
                String msg = "Cannot refresh a new item (" + safeGetJCRPath() + ").";
                log.debug(msg);
                throw new RepositoryException(msg);
            }

            /*
            Reset all transient modifications from this item and its descendants.
            */
            session.getSessionItemStateManager().undo(state);

            /* Unless the session is in 'observation' mode, mark all states
               within this tree 'invalidated' in order to have them refreshed
               from the server upon the next access.*/
            if (session.getCacheBehaviour() != CacheBehaviour.OBSERVATION) {
                state.getHierarchyEntry().invalidate(true);
            }
        }
    }

    /**
     * @see javax.jcr.Item#remove()
     */
    public void remove() throws VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkSupportedOption(Repository.LEVEL_2_SUPPORTED);
        checkStatus();

        // validation checks are performed within remove operation
        Operation rm = Remove.create(getItemState());
        session.getSessionItemStateManager().execute(rm);
    }

    //-----------------------------------------< ItemStateLifeCycleListener >---
    /**
     *
     * @param state
     * @param previousStatus
     */
    public void statusChanged(ItemState state, int previousStatus) {
        if (state != this.state) {
            throw new IllegalArgumentException("Invalid argument: ItemState with changed status must be this.state.");
        }

        switch (state.getStatus()) {
            /**
             * Nothing to do for
             * - Status#EXISTING : modifications reverted or saved
             *   inform listeners about an update (status was MODIFIED before)
             *   or a simple refresh without modification (status was INVALIDATED).
             */
            case Status.EXISTING:
                if (previousStatus == Status.INVALIDATED || previousStatus == Status.MODIFIED) {
                    notifyUpdated(previousStatus == Status.MODIFIED);
                }
                break;
            /**
             * Nothing to do for
             * - Status#EXISTING_MODIFIED : transient modification
             * - Status#STALE_MODIFIED : external modifications while transient changes pending
             * - Status#STALE_DESTROYED : external modifications while transient changes pending
             * - Status#MODIFIED : externally modified -> marker for sessionISM states only
             * - Status#EXISTING_REMOVED : transient removal
             */
            case Status.EXISTING_MODIFIED:
            case Status.STALE_MODIFIED:
            case Status.STALE_DESTROYED:
            case Status.MODIFIED:
            case Status.EXISTING_REMOVED:
                break;
            /**
             * Notify listeners that this item is transiently or permanently
             * destroyed.
             * - Status#REMOVED : permanent removal. item will never get back to life
             */
            case Status.REMOVED:
                state.removeListener(this);
                notifyDestroyed();
                break;
            /**
             * Invalid status. A state can never change its state to 'New'.
             */
            case Status.NEW:
                // should never happen.
                log.error("invalid state change to STATUS_NEW");
                break;
        }
    }

    //----------------------------------------------------------< LiveCycle >---

    /**
     * Notify the listeners that this instance has been created.
     */
    private void notifyCreated() {
        // copy listeners to array to avoid ConcurrentModificationException
        ItemLifeCycleListener[] la = listeners.values().toArray(new ItemLifeCycleListener[listeners.size()]);
        for (int i = 0; i < la.length; i++) {
            la[i].itemCreated(this);
        }
    }

    /**
     * Notify the listeners that this instance has been updated.
     */
    private void notifyUpdated(boolean modified) {
        // copy listeners to array to avoid ConcurrentModificationException
        ItemLifeCycleListener[] la = listeners.values().toArray(new ItemLifeCycleListener[listeners.size()]);
        for (int i = 0; i < la.length; i++) {
            if (la[i] != null) {
                la[i].itemUpdated(this, modified);
            }
        }
    }

    /**
     * Notify the listeners that this instance has been destroyed.
     */
    private void notifyDestroyed() {
        // copy listeners to array to avoid ConcurrentModificationException
        ItemLifeCycleListener[] la = listeners.values().toArray(new ItemLifeCycleListener[listeners.size()]);
        for (int i = 0; i < la.length; i++) {
            if (la[i] != null) {
                la[i].itemDestroyed(this);
            }
        }
    }

    /**
     * Add an <code>ItemLifeCycleListener</code>
     *
     * @param listener the new listener to be informed on life cycle changes
     */
    void addLifeCycleListener(ItemLifeCycleListener listener) {
        if (!listeners.containsKey(listener)) {
            listeners.put(listener, listener);
        }
    }

    /**
     * Remove an <code>ItemLifeCycleListener</code>
     *
     * @param listener an existing listener
     */
    void removeLifeCycleListener(ItemLifeCycleListener listener) {
        listeners.remove(listener);
    }

    //------------------------------------------------------< check methods >---
    /**
     * Performs a sanity check on this item and the associated session. If
     * the underlying item state is in an invalidated state then it will be
     * refreshed to get the current status of the item state. The status
     * check is then performed on the newly retrieved status.
     *
     * @throws RepositoryException if this item has been rendered invalid for some reason
     */
    protected void checkStatus() throws RepositoryException {
        // check session status
        session.checkIsAlive();
        // check status of this item for read operation
        if (state.getStatus() == Status.INVALIDATED) {
            // refresh to get current status from persistent storage
            state.getHierarchyEntry().reload(false);
        }
        // now check if valid
        if (!state.isValid()) {
            throw new InvalidItemStateException("Item '" + this + "' doesn't exist anymore. (Status = " +Status.getName(state.getStatus())+ ")");
        }
    }

    /**
     * Returns true if the repository supports the given option. False otherwise.
     *
     * @param option Any of the option constants defined by {@link Repository}
     * that either returns 'true' or 'false'. I.e.
     * <ul>
     * <li>{@link Repository#LEVEL_1_SUPPORTED}</li>
     * <li>{@link Repository#LEVEL_2_SUPPORTED}</li>
     * <li>{@link Repository#OPTION_TRANSACTIONS_SUPPORTED}</li>
     * <li>{@link Repository#OPTION_VERSIONING_SUPPORTED}</li>
     * <li>{@link Repository#OPTION_OBSERVATION_SUPPORTED}</li>
     * <li>{@link Repository#OPTION_LOCKING_SUPPORTED}</li>
     * <li>{@link Repository#OPTION_QUERY_SQL_SUPPORTED}</li>
     * </ul>
     * @return true if the repository supports the given option. False otherwise.
     */
    boolean isSupportedOption(String option) {
        return session.isSupportedOption(option);
    }

    /**
     * Check if the given option is supported by the repository.
     *
     * @param option Any of the option constants defined by {@link Repository}
     * that either returns 'true' or 'false'. I.e.
     * <ul>
     * <li>{@link Repository#LEVEL_1_SUPPORTED}</li>
     * <li>{@link Repository#LEVEL_2_SUPPORTED}</li>
     * <li>{@link Repository#OPTION_ACCESS_CONTROL_SUPPORTED}</li>
     * <li>{@link Repository#OPTION_ACTIVITIES_SUPPORTED}</li>
     * <li>{@link Repository#OPTION_BASELINES_SUPPORTED}</li>
     * <li>{@link Repository#OPTION_JOURNALED_OBSERVATION_SUPPORTED}</li>
     * <li>{@link Repository#OPTION_LIFECYCLE_SUPPORTED}</li>
     * <li>{@link Repository#OPTION_LOCKING_SUPPORTED}</li>
     * <li>{@link Repository#OPTION_NODE_AND_PROPERTY_WITH_SAME_NAME_SUPPORTED}</li>
     * <li>{@link Repository#OPTION_NODE_TYPE_MANAGEMENT_SUPPORTED}</li>
     * <li>{@link Repository#OPTION_OBSERVATION_SUPPORTED}</li>
     * <li>{@link Repository#OPTION_QUERY_SQL_SUPPORTED}</li>
     * <li>{@link Repository#OPTION_RETENTION_SUPPORTED}</li>
     * <li>{@link Repository#OPTION_SHAREABLE_NODES_SUPPORTED}</li>
     * <li>{@link Repository#OPTION_SIMPLE_VERSIONING_SUPPORTED}</li>
     * <li>{@link Repository#OPTION_TRANSACTIONS_SUPPORTED}</li>
     * <li>{@link Repository#OPTION_UNFILED_CONTENT_SUPPORTED}</li>
     * <li>{@link Repository#OPTION_UPDATE_MIXIN_NODE_TYPES_SUPPORTED}</li>
     * <li>{@link Repository#OPTION_UPDATE_PRIMARY_NODE_TYPE_SUPPORTED}</li>
     * <li>{@link Repository#OPTION_VERSIONING_SUPPORTED}</li>
     * <li>{@link Repository#OPTION_WORKSPACE_MANAGEMENT_SUPPORTED}</li>
     * <li>{@link Repository#OPTION_XML_EXPORT_SUPPORTED}</li>
     * <li>{@link Repository#OPTION_XML_IMPORT_SUPPORTED}</li>
     * <li>{@link Repository#WRITE_SUPPORTED}</li>
     * </ul>
     * @throws UnsupportedRepositoryOperationException
     * @throws RepositoryException
     */
    void checkSupportedOption(String option) throws UnsupportedRepositoryOperationException, RepositoryException {
        session.checkSupportedOption(option);
    }

    /**
     * Checks if the repository supports level 2 (writing) and the status of
     * this item. Note, that this method does not perform any additional
     * validation checks such as access restrictions, locking, checkin status
     * or protection that affect the writing to nodes and properties.
     *
     * @throws UnsupportedRepositoryOperationException
     * @throws RepositoryException
     * @see ItemStateValidator
     */
    protected void checkIsWritable() throws UnsupportedRepositoryOperationException, ConstraintViolationException, RepositoryException {
        checkSupportedOption(Repository.LEVEL_2_SUPPORTED);
        checkStatus();
    }

    /**
     * Returns true if the repository supports level 2 (writing). Note, that
     * this method does not perform any additional validation tests such as
     * access restrictions, locking, checkin status or protection that affect
     * the writing to nodes and properties.
     *
     * @throws UnsupportedRepositoryOperationException
     * @throws RepositoryException if the sanity check on this item fails.
     * See {@link ItemImpl#checkStatus()}.
     * @see ItemStateValidator
     */
    protected boolean isWritable() throws RepositoryException {
        checkStatus();
        return session.isSupportedOption(Repository.LEVEL_2_SUPPORTED);
    }

    //------------------------------------< Implementation specific methods >---
    /**
     * Same as <code>{@link Item#getName()}</code> except that
     * this method returns a <code>Name</code> instead of a
     * <code>String</code>.
     *
     * @return the name of this item as <code>Name</code>
     * @throws RepositoryException if an error occurs.
     */
    abstract Name getQName() throws RepositoryException;

    /**
     * Returns the primary path to this <code>Item</code>.
     *
     * @return the primary path to this <code>Item</code>
     */
    Path getQPath() throws RepositoryException {
        return state.getPath();
    }

    /**
     * Returns the item-state associated with this <code>Item</code>.
     *
     * @return state associated with this <code>Item</code>
     */
    protected ItemState getItemState() {
        return state;
    }

    /**
     * Returns the ItemManager associated with this item's Session.
     *
     * @return ItemManager
     */
    protected ItemManager getItemManager() {
        return session.getItemManager();
    }

    /**
     * Failsafe conversion of internal <code>Path</code> to JCR path for use in
     * error messages etc.
     *
     * @return JCR path
     */
    String safeGetJCRPath() {
        return LogUtil.safeGetJCRPath(getItemState(), session.getPathResolver());
    }

    /**
     *
     * @param state
     * @throws RepositoryException
     */
    private static void updateId(ItemState state) throws RepositoryException {
        HierarchyEntry he = state.getHierarchyEntry();
        while (he.getStatus() != Status.INVALIDATED) {
            he = he.getParent();
            if (he == null) {
                // root reached without intermediate invalidated entry
                return;
            }
        }
        // he is INVALIDATED -> force reloading in order to be aware of id changes
        he.getItemState();
    }
}
