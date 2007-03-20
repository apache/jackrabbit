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

import org.apache.commons.collections.map.ReferenceMap;
import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.ItemStateValidator;
import org.apache.jackrabbit.jcr2spi.state.ItemStateLifeCycleListener;
import org.apache.jackrabbit.jcr2spi.state.Status;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.operation.Remove;
import org.apache.jackrabbit.jcr2spi.operation.Operation;
import org.apache.jackrabbit.jcr2spi.util.LogUtil;
import org.apache.jackrabbit.jcr2spi.config.CacheBehaviour;
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.name.NoPrefixDeclaredException;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.PathFormat;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.lock.LockException;
import javax.jcr.version.VersionException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.RepositoryException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.AccessDeniedException;
import javax.jcr.Item;
import javax.jcr.ItemVisitor;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.Repository;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.PathNotFoundException;
import javax.jcr.Node;
import javax.jcr.Session;

import java.util.Map;
import java.util.Collections;

/**
 * <code>ItemImpl</code>...
 */
public abstract class ItemImpl implements Item, ItemStateLifeCycleListener {

    private static Logger log = LoggerFactory.getLogger(ItemImpl.class);

    private ItemState state;

    // protected fields for VersionImpl and VersionHistoryImpl
    protected ItemManager itemMgr;
    protected SessionImpl session;

    /**
     * Listeners (weak references)
     */
    protected final Map listeners = Collections.synchronizedMap(new ReferenceMap(ReferenceMap.WEAK, ReferenceMap.WEAK));

    public ItemImpl(ItemManager itemManager, SessionImpl session, ItemState state,
                    ItemLifeCycleListener[] listeners) {
        this.session = session;

        this.itemMgr = itemManager;
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
        try {
            return PathFormat.format(getQPath(), session.getNamespaceResolver());
        } catch (NoPrefixDeclaredException npde) {
            // should never get here...
            String msg = "Internal error: encountered unregistered namespace";
            log.debug(msg);
            throw new RepositoryException(msg, npde);
        }
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
        try {
            // Path.getAncestor requires relative degree, i.e. we need
            // to convert absolute to relative ancestor degree
            Path path = getQPath();
            int relDegree = path.getAncestorCount() - depth;
            if (relDegree < 0) {
                throw new ItemNotFoundException();
            }
            Path ancestorPath = path.getAncestor(relDegree);
            return itemMgr.getItem(ancestorPath);
        } catch (PathNotFoundException pnfe) {
            throw new ItemNotFoundException();
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
        return (Node) itemMgr.getItem(parentEntry);
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
     * @see javax.jcr.Item#getSession()
     */
    public Session getSession() throws RepositoryException {
        checkStatus();
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
        if (otherItem instanceof ItemImpl) {
            ItemImpl other = (ItemImpl) otherItem;
            if (this.state == other.state) {
                return true;
            }
            // 2 items may only be the same if the were accessed from Sessions
            // bound to the same workspace
            String otherWspName = other.session.getWorkspace().getName();
            if (session.getWorkspace().getName().equals(otherWspName)) {
                // in addition they must provide the same id irrespective of
                // any transient modifications.
                ItemState wspState = state.getWorkspaceState();
                ItemState otherWspState = other.state.getWorkspaceState();

                if (wspState != null && otherWspState != null) {
                    return wspState.getId().equals(otherWspState.getId());
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
        // check if item has been removed by this or another session
        if (Status.isTerminal(state.getStatus()) || Status.EXISTING_REMOVED == state.getStatus()) {
            throw new InvalidItemStateException("Item '" + this + "' doesn't exist anymore");
        }

        /* If 'keepChanges' is true, items that do not have changes pending have
           their state refreshed to reflect the current saved state */
        if (keepChanges) {
            if (state.getStatus() != Status.NEW  &&
                session.getCacheBehaviour() != CacheBehaviour.OBSERVATION) {
                // merge current transient modifications with latest changes
                // from the 'server'.
                // Note, that with Observation-CacheBehaviour no manuel refresh
                // is required. changes get pushed automatically.
                state.getHierarchyEntry().reload(true, true);
            }
        } else {
            // check status of item state
            if (state.getStatus() == Status.NEW) {
                String msg = "Cannot refresh a new item (" + safeGetJCRPath() + ").";
                log.debug(msg);
                throw new RepositoryException(msg);
            }

            /*
            Reset all transient modifications from this item and its decendants.
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
             * - Status#EXISTING_MODIFIED : transient modification
             * - Status#STALE_MODIFIED : external modifications while transient changes pending
             * - Status#MODIFIED : externaly modified -> marker for sessionISM states only
             */
            case Status.EXISTING:
            case Status.EXISTING_MODIFIED:
            case Status.STALE_MODIFIED:
            case Status.MODIFIED:
                break;
            /**
             * Notify listeners that this item is transiently or permanently
             * destroyed.
             * - Status#EXISTING_REMOVED : transient removal
             * - Status#REMOVED : permanent removal. item will never get back to life
             * - Status#STALE_DESTROYED : permanent removal. item will never get back to life
             */
            case Status.EXISTING_REMOVED:
                notifyDestroyed();
                break;
            case Status.REMOVED:
            case Status.STALE_DESTROYED:
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
     * Notify the listeners that this instance has been discarded
     * (i.e. it has been temporarily rendered 'invalid').
     */
    private void notifyCreated() {
        // copy listeners to array to avoid ConcurrentModificationException
        ItemLifeCycleListener[] la = (ItemLifeCycleListener[]) listeners.values().toArray(new ItemLifeCycleListener[listeners.size()]);
        for (int i = 0; i < la.length; i++) {
            la[i].itemCreated(this);
        }
    }

    /**
     * Notify the listeners that this instance has been invalidated
     * (i.e. it has been temporarily rendered 'invalid').
     */
    private  void notifyInvalidated() {
        // copy listeners to array to avoid ConcurrentModificationException
        ItemLifeCycleListener[] la = (ItemLifeCycleListener[]) listeners.values().toArray(new ItemLifeCycleListener[listeners.size()]);
        for (int i = 0; i < la.length; i++) {
            if (la[i] != null) {
                la[i].itemInvalidated(this);
            }
        }
    }

    /**
     * Notify the listeners that this instance has been destroyed
     * (i.e. it has been permanently rendered 'invalid').
     */
    private void notifyDestroyed() {
        // copy listeners to array to avoid ConcurrentModificationException
        ItemLifeCycleListener[] la = (ItemLifeCycleListener[]) listeners.values().toArray(new ItemLifeCycleListener[listeners.size()]);
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
            state.getHierarchyEntry().reload(false, false);
        }
        // now check if valid
        if (!state.isValid()) {
            throw new InvalidItemStateException("Item '" + this + "' doesn't exist anymore");
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
     * <li>{@link Repository#OPTION_TRANSACTIONS_SUPPORTED}</li>
     * <li>{@link Repository#OPTION_VERSIONING_SUPPORTED}</li>
     * <li>{@link Repository#OPTION_OBSERVATION_SUPPORTED}</li>
     * <li>{@link Repository#OPTION_LOCKING_SUPPORTED}</li>
     * <li>{@link Repository#OPTION_QUERY_SQL_SUPPORTED}</li>
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
     * this method returns a <code>QName</code> instead of a
     * <code>String</code>.
     *
     * @return the name of this item as <code>QName</code>
     * @throws RepositoryException if an error occurs.
     */
    abstract QName getQName() throws RepositoryException;

    /**
     * Returns the primary path to this <code>Item</code>.
     *
     * @return the primary path to this <code>Item</code>
     */
    Path getQPath() throws RepositoryException {
        return state.getQPath();
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
     * Failsafe conversion of internal <code>Path</code> to JCR path for use in
     * error messages etc.
     *
     * @return JCR path
     */
    String safeGetJCRPath() {
        return LogUtil.safeGetJCRPath(getItemState(), session.getNamespaceResolver());
    }
}
