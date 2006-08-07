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
import org.apache.jackrabbit.jcr2spi.state.SessionItemStateManager;
import org.apache.jackrabbit.jcr2spi.state.TransientItemStateListener;
import org.apache.jackrabbit.jcr2spi.state.ItemStateException;
import org.apache.jackrabbit.jcr2spi.state.StaleItemStateException;
import org.apache.jackrabbit.jcr2spi.state.ItemStateValidator;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.PropertyState;
import org.apache.jackrabbit.jcr2spi.operation.Remove;
import org.apache.jackrabbit.jcr2spi.operation.Operation;
import org.apache.jackrabbit.name.NoPrefixDeclaredException;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.PathFormat;
import org.apache.jackrabbit.spi.ItemId;
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
public abstract class ItemImpl implements Item, TransientItemStateListener {

    private static Logger log = LoggerFactory.getLogger(ItemImpl.class);

    protected static final int STATUS_NORMAL = 0;
    protected static final int STATUS_MODIFIED = 1;
    protected static final int STATUS_DESTROYED = 2;
    protected static final int STATUS_INVALIDATED = 3;

    private ItemId id;

    private int status;
    private ItemState state;

    protected SessionItemStateManager itemStateMgr;
    protected ItemManager itemMgr;
    protected SessionImpl session;

    /**
     * Listeners (weak references)
     */
    protected final Map listeners = Collections.synchronizedMap(new ReferenceMap(ReferenceMap.WEAK, ReferenceMap.WEAK));

    public ItemImpl(ItemManager itemManager, SessionImpl session, ItemState state,
                    ItemLifeCycleListener[] listeners) {
        this.session = session;
        //DIFF JACKRABBIT: rep = (RepositoryImpl) session.getRepository();
        //DIFF JACKRABBIT: stateMgr = session.getSessionItemStateManager();
        itemStateMgr = session.getSessionItemStateManager();
        this.id = state.getId();
        this.itemMgr = itemManager;
        this.state = state;
        status = STATUS_NORMAL;

        if (listeners != null) {
            for (int i = 0; i < listeners.length; i++) {
                addLifeCycleListener(listeners[i]);
            }
        }
        notifyCreated();

        // add this item as listener to events of the underlying state object
        this.state.addListener(this);
    }

    //-----------------------------------------------------< Item interface >---
    /**
     * @see javax.jcr.Item#getPath()
     */
    public String getPath() throws RepositoryException {
        checkStatus();
        try {
            // DIFF JR: use nsResolver
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
        return session.getHierarchyManager().getDepth(state.getParent().getId());
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
        // short-cut: read-only implementations always return false.
        if (!isSupportedOption(Repository.LEVEL_2_SUPPORTED)) {
            return false;
        }
        return state.isTransient() && state.getOverlayedState() == null;
    }

    /**
     * @see javax.jcr.Item#isModified()
     */
    public boolean isModified() {
        // short-cut: read-only implementations always return false.
        if (!isSupportedOption(Repository.LEVEL_2_SUPPORTED)) {
            return false;
        }
        return state.isTransient() && state.getOverlayedState() != null;
    }

    /**
     * @see javax.jcr.Item#isSame(Item)
     */
    public boolean isSame(Item otherItem) {
        if (this == otherItem) {
            return true;
        }
        if (otherItem instanceof ItemImpl) {
            ItemImpl other = (ItemImpl) otherItem;
            // 2 items may only be the same if the were accessed from Sessions
            // bound to the same workspace
            String otherWspName = other.session.getWorkspace().getName();
            if (session.getWorkspace().getName().equals(otherWspName)) {
                // in addition they must provide the same id irrespective of
                // any transient modifications.
                // TODO: TO_BE_FIXED check if this is sufficient check (SPI-id)
                ItemId thisId = (state.hasOverlayedState()) ? state.getOverlayedState().getId() : state.getId();
                ItemId otherId = (other.getItemState().hasOverlayedState()) ? other.getItemState().getOverlayedState().getId() : other.getId();
                return thisId.equals(otherId);
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
            itemStateMgr.save(getItemState());
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
            /** todo FIXME should reset Item#status field to STATUS_NORMAL
             * of all descendent non-transient instances; maybe also
             * have to reset stale ItemState instances */
            return;
        }

        // check status of this item's state
        if (isTransient()) {
            switch (state.getStatus()) {
                case ItemState.STATUS_NEW:
                    String msg = "Cannot refresh a new item (" + safeGetJCRPath() + ").";
                    log.debug(msg);
                    throw new RepositoryException(msg);
                case ItemState.STATUS_STALE_DESTROYED:
                    msg = "Cannot refresh on a deleted item (" + safeGetJCRPath() + ").";
                    log.debug(msg);
                    throw new InvalidItemStateException(msg);
            }
        }
        // reset all transient modifications from this item and its decendants.
        try {
            itemStateMgr.undo(state);
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
        itemStateMgr.execute(rm);
    }

    //--------------------------------------------< TransientItemStateListener >
    /**
     * {@inheritDoc}
     */
    public void stateCreated(ItemState created) {
        status = STATUS_NORMAL;
    }

    /**
     * {@inheritDoc}
     */
    public void stateDestroyed(ItemState destroyed) {
        // underlying state has been permanently destroyed

        // set state of this instance to 'destroyed'
        status = STATUS_DESTROYED;
        // dispose state
        if (state == destroyed) {
            state.removeListener(this);
            state = null;
        }
        /**
         * notify the listeners that this instance has been
         * permanently invalidated
         */
        notifyDestroyed();
    }

    /**
     * {@inheritDoc}
     */
    public void stateModified(ItemState modified) {
        status = STATUS_MODIFIED;
    }

    /**
     * {@inheritDoc}
     */
    public void stateDiscarded(ItemState discarded) {
        /**
         * the state of this item has been discarded, probably as a result
         * of calling Node.revert() or ItemImpl.setRemoved()
         */
        if (isTransient()) {
            switch (state.getStatus()) {
                /**
                 * persistent item that has been transiently removed
                 */
                case ItemState.STATUS_EXISTING_REMOVED:
                    /**
                     * persistent item that has been transiently modified
                     */
                case ItemState.STATUS_EXISTING_MODIFIED:
                    /**
                     * persistent item that has been transiently modified or removed
                     * and the underlying persistent state has been externally
                     * modified since the transient modification/removal.
                     */
                case ItemState.STATUS_STALE_MODIFIED:
                    ItemState persistentState = state.getOverlayedState();
                    /**
                     * the state is a transient wrapper for the underlying
                     * persistent state, therefore restore the
                     * persistent state and resurrect this item instance
                     * if necessary
                     */
                    // DIFF JACKRABBIT: this is now done in stateUncovering()
//                    state.removeListener(this);
//                    persistentState.addListener(this);
//                    itemStateMgr.disconnectTransientItemState(state);
//                    state = persistentState;

                    return;

                    /**
                     * persistent item that has been transiently modified or removed
                     * and the underlying persistent state has been externally
                     * destroyed since the transient modification/removal.
                     */
                case ItemState.STATUS_STALE_DESTROYED:
                    /**
                     * first notify the listeners that this instance has been
                     * permanently invalidated
                     */
                    notifyDestroyed();
                    // now set state of this instance to 'destroyed'
                    status = STATUS_DESTROYED;
                    // finally dispose state
                    state.removeListener(this);
                    state = null;
                    return;

                    /**
                     * new item that has been transiently added
                     */
                case ItemState.STATUS_NEW:
                    /**
                     * first notify the listeners that this instance has been
                     * permanently invalidated
                     */
                    notifyDestroyed();
                    // now set state of this instance to 'destroyed'
                    status = STATUS_DESTROYED;
                    // finally dispose state
                    state.removeListener(this);
                    state = null;
                    return;
            }
        }

        /**
         * first notify the listeners that this instance has been
         * invalidated
         */
        notifyInvalidated();
        // now render this instance 'invalid'
        status = STATUS_INVALIDATED;
    }

    /**
     * @inheritDoc
     */
    public void stateOverlaid(ItemState overlayer) {
        state.removeListener(this);
        state = overlayer;
        state.addListener(this);
        status = STATUS_MODIFIED;
    }

    /**
     * @inheritDoc
     */
    public void stateUncovering(ItemState overlayer) {
        state.removeListener(this);
        state = overlayer.getOverlayedState();
        state.addListener(this);
        status = STATUS_NORMAL;
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
            la[i].itemCreated(id, this);
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
                la[i].itemInvalidated(id, this);
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
                la[i].itemDestroyed(id, this);
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
    // DIFF TO JACKRABBIT: consistenly naming of 'checkMethods'
    /**
     * Performs a sanity check on this item and the associated session.
     *
     * @throws RepositoryException if this item has been rendered invalid for some reason
     */
    void checkStatus() throws RepositoryException {
        // check session status
        session.checkIsAlive();
        // check status of this item for read operation
        switch (status) {
            case STATUS_NORMAL:
            case STATUS_MODIFIED:
                return;

            case STATUS_DESTROYED:
            case STATUS_INVALIDATED:
                throw new InvalidItemStateException("Item '" + id + "' doesn't exist anymore");
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
     * @see ItemStateValidator#checkAddNode(NodeState, QName, QName, int)
     * @see ItemStateValidator#checkAddProperty(NodeState, QName, QPropertyDefinition, int)
     * @see ItemStateValidator#checkSetProperty(PropertyState, int)
     */
    void checkIsWritable() throws UnsupportedRepositoryOperationException, ConstraintViolationException, RepositoryException {
        checkSupportedOption(Repository.LEVEL_2_SUPPORTED);
        checkStatus();
    }

    //------------------------------------< Implementation specific methods >---
    /**
     * Return the id of this <code>Item</code>.
     *
     * @return the id of this <code>Item</code>
     */
    public ItemId getId() {
        return id;
    }

    /**
     * Same as <code>{@link Item#getName()}</code> except that
     * this method returns a <code>QName</code> instead of a
     * <code>String</code>.
     *
     * @return the name of this item as <code>QName</code>
     * @throws RepositoryException if an error occurs.
     */
    abstract QName getQName() throws RepositoryException;

    // DIFF JR: getPrimaryPath
    /**
     * Returns the primary path to this <code>Item</code>.
     *
     * @return the primary path to this <code>Item</code>
     */
    Path getQPath() throws RepositoryException {
        return session.getHierarchyManager().getQPath(id);
    }

    /**
     * Returns the item-state associated with this <code>Item</code>.
     *
     * @return state associated with this <code>Item</code>
     */
    ItemState getItemState() {
        return state;
    }

    /**
     *
     * @return
     */
    boolean isTransient() {
        return state.isTransient();
    }


    /**
     * DIFF JACKRABBIT
     * Failsafe conversion of internal <code>Path</code> to JCR path for use in
     * error messages etc.
     *
     * @return JCR path
     */
    String safeGetJCRPath() {
        return session.getHierarchyManager().safeGetJCRPath(getId());
    }
}
