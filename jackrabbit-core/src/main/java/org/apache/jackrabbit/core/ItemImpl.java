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
import java.util.Iterator;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.core.session.SessionOperation;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.SessionItemStateManager;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>ItemImpl</code> implements the <code>Item</code> interface.
 */
public abstract class ItemImpl implements Item {

    /**
     * Logger instance
     */
    private static Logger log = LoggerFactory.getLogger(ItemImpl.class);

    protected static final int STATUS_NORMAL = 0;
    protected static final int STATUS_MODIFIED = 1;
    protected static final int STATUS_DESTROYED = 2;
    protected static final int STATUS_INVALIDATED = 3;

    protected final ItemId id;

    /**
     * The component context of the session to which this item is associated.
     */
    protected final SessionContext sessionContext;

    /**
     * The session to which this item is associated.
     */
    protected final SessionImpl session;

    /**
     * Item data associated with this item.
     */
    protected final ItemData data;

    /**
     * <code>ItemManager</code> that created this <code>Item</code>
     */
    protected final ItemManager itemMgr;

    /**
     * <code>SessionItemStateManager</code> associated with this <code>Item</code>
     */
    protected final SessionItemStateManager stateMgr;

    /**
     * Package private constructor.
     *
     * @param itemMgr   the <code>ItemManager</code> that created this <code>Item</code>
     * @param sessionContext the component context of the associated session
     * @param data      ItemData of this <code>Item</code>
     */
    ItemImpl(ItemManager itemMgr, SessionContext sessionContext, ItemData data) {
        this.sessionContext = sessionContext;
        this.session = sessionContext.getSessionImpl();
        this.stateMgr = sessionContext.getItemStateManager();
        this.id = data.getId();
        this.itemMgr = itemMgr;
        this.data = data;
    }

    protected void perform(SessionOperation operation)
            throws RepositoryException {
        sessionContext.getSessionState().perform(operation);
    }

    /**
     * Performs a sanity check on this item and the associated session.
     *
     * @throws RepositoryException if this item has been rendered invalid for some reason
     */
    protected void sanityCheck() throws RepositoryException {
        // check session status
        perform(new SessionOperation("sanity check"));

        // check status of this item for read operation
        final int status = data.getStatus();
        if (status == STATUS_DESTROYED || status == STATUS_INVALIDATED) {
            throw new InvalidItemStateException(id + ": the item does not exist anymore");
        }
    }

    protected boolean isTransient() {
        return getItemState().isTransient();
    }

    protected abstract ItemState getOrCreateTransientItemState() throws RepositoryException;

    protected abstract void makePersistent() throws InvalidItemStateException;

    /**
     * Marks this instance as 'removed' and notifies its listeners.
     * The resulting state is either 'temporarily invalidated' or
     * 'permanently invalidated', depending on the initial state.
     *
     * @throws RepositoryException if an error occurs
     */
    protected void setRemoved() throws RepositoryException {
        final int status = data.getStatus();
        if (status == STATUS_INVALIDATED || status == STATUS_DESTROYED) {
            // this instance is already 'invalid', get outta here
            return;
        }

        ItemState transientState = getOrCreateTransientItemState();
        if (transientState.getStatus() == ItemState.STATUS_NEW) {
            // this is a 'new' item, simply dispose the transient state
            // (it is no longer used); this will indirectly (through
            // stateDiscarded listener method) invalidate this instance permanently
            stateMgr.disposeTransientItemState(transientState);
        } else {
            // this is an 'existing' item (i.e. it is backed by persistent
            // state), mark it as 'removed'
            transientState.setStatus(ItemState.STATUS_EXISTING_REMOVED);
            // transfer the transient state to the attic
            stateMgr.moveTransientItemStateToAttic(transientState);

            // set state of this instance to 'invalid'
            data.setStatus(STATUS_INVALIDATED);
            // notify the manager that this instance has been
            // temporarily invalidated
            itemMgr.itemInvalidated(id, data);
        }
    }

    /**
     * Returns the item-state associated with this <code>Item</code>.
     *
     * @return state associated with this <code>Item</code>
     */
    ItemState getItemState() {
        return data.getState();
    }

    /**
     * Return the id of this <code>Item</code>.
     *
     * @return the id of this <code>Item</code>
     */
    public ItemId getId() {
        return id;
    }

    /**
     * Returns the primary path to this <code>Item</code>.
     *
     * @return the primary path to this <code>Item</code>
     */
    public Path getPrimaryPath() throws RepositoryException {
        return session.getHierarchyManager().getPath(id);
    }

    /**
     * Failsafe mapping of internal <code>id</code> to JCR path for use in
     * diagnostic output, error messages etc.
     *
     * @return JCR path or some fallback value
     */
    public String safeGetJCRPath() {
        return itemMgr.safeGetJCRPath(id);
    }

    /**
     * Same as <code>{@link Item#remove()}</code> except for the
     * <code>noChecks</code> parameter.
     *
     * @param noChecks
     * @throws VersionException
     * @throws LockException
     * @throws RepositoryException
     */
    protected void internalRemove(boolean noChecks)
            throws VersionException, LockException,
            ConstraintViolationException, RepositoryException {

        // check state of this instance
        sanityCheck();

        // check if this is the root node
        if (getDepth() == 0) {
            throw new RepositoryException("Cannot remove the root node");
        }

        NodeImpl parentNode = (NodeImpl) getParent();
        if (!noChecks) {
            // check if protected and not under retention/hold
            int options = ItemValidator.CHECK_CONSTRAINTS | ItemValidator.CHECK_HOLD |
                    ItemValidator.CHECK_RETENTION;
            session.getValidator().checkRemove(this, options, Permission.NONE);

            // parent node: make sure it is checked-out and not protected nor locked.
            options = ItemValidator.CHECK_LOCK | ItemValidator.CHECK_CHECKED_OUT |
                    ItemValidator.CHECK_CONSTRAINTS;
            session.getValidator().checkModify(parentNode, options, Permission.NONE);
        }

        // delegate the removal of the child item to the parent node
        if (isNode()) {
            parentNode.removeChildNode((NodeId) getId());
        } else {
            Path.Element thisName = getPrimaryPath().getNameElement();
            parentNode.removeChildProperty(thisName.getName());
        }
    }

    /**
     * Same as <code>{@link Item#getName()}</code> except that
     * this method returns a <code>Name</code> instead of a
     * <code>String</code>.
     *
     * @return the name of this item as <code>Name</code>
     * @throws RepositoryException if an error occurs.
     */
    public abstract Name getQName() throws RepositoryException;

    //-----------------------------------------------------------------< Item >

    /**
     * {@inheritDoc}
     */
    public abstract void accept(ItemVisitor visitor)
            throws RepositoryException;

    /**
     * {@inheritDoc}
     */
    public abstract boolean isNode();

    /**
     * {@inheritDoc}
     */
    public abstract String getName() throws RepositoryException;

    /**
     * {@inheritDoc}
     */
    public abstract Node getParent()
            throws ItemNotFoundException, AccessDeniedException, RepositoryException;

    /**
     * {@inheritDoc}
     */
    public boolean isNew() {
        final ItemState state = getItemState();
        return state.isTransient() && state.getOverlayedState() == null;
    }

    /**
     * checks if this item is new. running outside of transactions, this
     * is the same as {@link #isNew()} but within a transaction an item can
     * be saved but not yet persisted.
     */
    protected boolean isTransactionalNew() {
        final ItemState state = getItemState();
        return state.getStatus() == ItemState.STATUS_NEW;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isModified() {
        final ItemState state = getItemState();
        return state.isTransient() && state.getOverlayedState() != null;
    }

    /**
     * {@inheritDoc}
     */
    public void remove()
            throws VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        internalRemove(false);
    }

    /**
     * {@inheritDoc}
     */
    public void save() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        perform(new ItemSaveOperation(isNode(), getItemState()));
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void refresh(boolean keepChanges)
            throws InvalidItemStateException, RepositoryException {
        // check state of this instance
        sanityCheck();

        if (keepChanges) {
            /** todo FIXME should reset Item#status field to STATUS_NORMAL
             * of all descendant non-transient instances; maybe also
             * have to reset stale ItemState instances */
            return;
        }

        if (isNode()) {
            // check if this is the root node
            if (getDepth() == 0) {
                // optimization
                stateMgr.disposeAllTransientItemStates();
                return;
            }
        }

        // list of transient items that should be discarded
        ArrayList<ItemState> list = new ArrayList<ItemState>();
        ItemState transientState;

        // check status of this item's state
        if (isTransient()) {
            transientState = getItemState();
            switch (transientState.getStatus()) {
                case ItemState.STATUS_STALE_MODIFIED:
                case ItemState.STATUS_STALE_DESTROYED:
                    // add this item's state to the list
                    list.add(transientState);
                    break;

                case ItemState.STATUS_EXISTING_MODIFIED:
                    if (!transientState.getParentId().equals(
                            transientState.getOverlayedState().getParentId())) {
                        throw new RepositoryException(
                                "Cannot refresh a moved item: " + this +
                                " - possible solution: refresh the parent");
                    }
                    list.add(transientState);
                    break;

                case ItemState.STATUS_NEW:
                    throw new RepositoryException(
                            "Cannot refresh a new item: " + this);

                default:
                    log.warn("Unexpected item state status:"
                            + transientState.getStatus() + " of " + this);
                    // ignore
                    break;
            }
        }

        if (isNode()) {
            // build list of 'new', 'modified' or 'stale' descendants
            Iterator<ItemState> iter = stateMgr.getDescendantTransientItemStates((NodeId) id);
            while (iter.hasNext()) {
                transientState = iter.next();
                switch (transientState.getStatus()) {
                    case ItemState.STATUS_STALE_MODIFIED:
                    case ItemState.STATUS_STALE_DESTROYED:
                    case ItemState.STATUS_NEW:
                    case ItemState.STATUS_EXISTING_MODIFIED:
                        // add new or modified state to the list
                        list.add(transientState);
                        break;

                    default:
                        log.debug("unexpected state status (" + transientState.getStatus() + ")");
                        // ignore
                        break;
                }
            }
        }

        // process list of 'new', 'modified' or 'stale' transient states
        for (ItemState state : list) {
            // dispose the transient state, it is no longer used;
            // this will indirectly (through stateDiscarded listener method)
            // either restore or permanently invalidate the wrapping Item instances
            stateMgr.disposeTransientItemState(state);
        }

        if (isNode()) {
            // discard all transient descendants in the attic (i.e. those marked
            // as 'removed'); this will resurrect the removed items
            Iterator<ItemState> iter = stateMgr.getDescendantTransientItemStatesInAttic((NodeId) id);
            while (iter.hasNext()) {
                transientState = iter.next();
                // dispose the transient state; this will indirectly (through
                // stateDiscarded listener method) resurrect the wrapping Item instances
                stateMgr.disposeTransientItemStateInAttic(transientState);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public Item getAncestor(int degree)
            throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        // check state of this instance
        sanityCheck();

        if (degree == 0) {
            return itemMgr.getRootNode();
        }

        try {
            // Path.getAncestor requires relative degree, i.e. we need
            // to convert absolute to relative ancestor degree
            Path path = getPrimaryPath();
            int relDegree = path.getAncestorCount() - degree;
            if (relDegree < 0) {
                throw new ItemNotFoundException();
            }
            // shortcut
            if (relDegree == 0) {
                return this;
            }
            Path ancestorPath = path.getAncestor(relDegree);
            return itemMgr.getNode(ancestorPath);
        } catch (PathNotFoundException pnfe) {
            throw new ItemNotFoundException();
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getPath() throws RepositoryException {
        // check state of this instance
        sanityCheck();
        return session.getJCRPath(getPrimaryPath());
    }

    /**
     * {@inheritDoc}
     */
    public int getDepth() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        final ItemState state = getItemState();
        if (state.getParentId() == null) {
            // shortcut
            return 0;
        }
        return session.getHierarchyManager().getDepth(id);
    }

    /**
     * Returns the session associated with this item.
     * <p>
     * Since Jackrabbit 1.4 it is safe to use this method regardless
     * of item state.
     *
     * @see <a href="http://issues.apache.org/jira/browse/JCR-911">Issue JCR-911</a>
     * @return current session
     */
    public Session getSession() {
        return session;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSame(Item otherItem) throws RepositoryException {
        // check state of this instance
        sanityCheck();

        if (this == otherItem) {
            return true;
        }
        if (otherItem instanceof ItemImpl) {
            ItemImpl other = (ItemImpl) otherItem;
            return id.equals(other.id)
                    && session.getWorkspace().getName().equals(
                            other.getSession().getWorkspace().getName());
        }
        return false;
    }

    //--------------------------------------------------------------< Object >

    /**
     * Returns the({@link #safeGetJCRPath() safe}) path of this item for use
     * in diagnostic output.
     *
     * @return "/path/to/item"
     */
    public String toString() {
        return safeGetJCRPath();
    }

}
