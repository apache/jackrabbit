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
import org.apache.jackrabbit.jcr2spi.state.ItemStateException;
import org.apache.jackrabbit.jcr2spi.state.StaleItemStateException;
import org.apache.jackrabbit.jcr2spi.state.ItemStateValidator;
import org.apache.jackrabbit.jcr2spi.state.ItemStateLifeCycleListener;
import org.apache.jackrabbit.jcr2spi.state.Status;
import org.apache.jackrabbit.jcr2spi.operation.Remove;
import org.apache.jackrabbit.jcr2spi.operation.Operation;
import org.apache.jackrabbit.jcr2spi.util.LogUtil;
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
     * @see javax.jcr.Item#getParent()
     */
    public abstract Node getParent() throws ItemNotFoundException, AccessDeniedException, RepositoryException;

    /**
     * @see javax.jcr.Item#getDepth()
     */
    public int getDepth() throws RepositoryException {
        checkStatus();
        if (state.getParent() == null) {
            // shortcut
            return Path.ROOT_DEPTH;
        }
        return session.getHierarchyManager().getDepth(state);
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
        try {
            session.getSessionItemStateManager().save(getItemState());
        } catch (StaleItemStateException e) {
            throw new InvalidItemStateException(e);
        } catch (ItemStateException e) {
            String msg = "Unable to update item (" + safeGetJCRPath() + ")";
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * @see javax.jcr.Item#refresh(boolean)
     */
    public void refresh(boolean keepChanges) throws InvalidItemStateException, RepositoryException {
        checkStatus();

        if (keepChanges) {
            // TODO: TOBEFIXED. make sure item is updated to status present on the server.
            return;
        }

        // check status of this item's state
        switch (state.getStatus()) {
            case Status.NEW:
                String msg = "Cannot refresh a new item (" + safeGetJCRPath() + ").";
                log.debug(msg);
                throw new RepositoryException(msg);
            case Status.STALE_DESTROYED:
                msg = "Cannot refresh on a deleted item (" + safeGetJCRPath() + ").";
                log.debug(msg);
                throw new InvalidItemStateException(msg);
        }

        // reset all transient modifications from this item and its decendants.
        try {
            session.getSessionItemStateManager().undo(state);
        } catch (ItemStateException e) {
            String msg = "Unable to update item (" + safeGetJCRPath() + ")";
            log.debug(msg);
            throw new RepositoryException(msg, e);
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
        // TODO: ev. assert that state is this.state?

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
     * Performs a sanity check on this item and the associated session.
     *
     * @throws RepositoryException if this item has been rendered invalid for some reason
     */
    protected void checkStatus() throws RepositoryException {
        // check session status
        session.checkIsAlive();
        // check status of this item for read operation
        if (state == null || !state.isValid()) {
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
