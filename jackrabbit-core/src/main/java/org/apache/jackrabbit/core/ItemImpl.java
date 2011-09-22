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

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.core.session.SessionOperation;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.SessionItemStateManager;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.value.ValueHelper;

/**
 * <code>ItemImpl</code> implements the <code>Item</code> interface.
 */
public abstract class ItemImpl implements Item {

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
        this.stateMgr = sessionContext.getItemStateManager();
        this.id = data.getId();
        this.itemMgr = itemMgr;
        this.data = data;
    }

    protected <T> T perform(final SessionOperation<T> operation)
            throws RepositoryException {
        itemSanityCheck();
        return sessionContext.getSessionState().perform(operation);
    }

    /**
     * Performs a sanity check on this item and the associated session.
     *
     * @throws RepositoryException if this item has been rendered invalid for some reason
     */
    protected void sanityCheck() throws RepositoryException {
        // check session status
        sessionContext.getSessionState().checkAlive();

        // check status of this item for read operation
        itemSanityCheck();
    }

    /**
     * Checks the status of this item.
     *
     * @throws RepositoryException if this item no longer exists
     */
    protected void itemSanityCheck() throws RepositoryException {
        // check status of this item for read operation
        final int status = data.getStatus();
        if (status == STATUS_DESTROYED || status == STATUS_INVALIDATED) {
            throw new InvalidItemStateException(
                    "Item does not exist anymore: " + id);
        }
    }

    protected boolean isTransient() {
        return getItemState().isTransient();
    }

    protected abstract ItemState getOrCreateTransientItemState() throws RepositoryException;

    protected abstract void makePersistent() throws RepositoryException;

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
        return sessionContext.getHierarchyManager().getPath(id);
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
     * Same as <code>{@link Item#getName()}</code> except that
     * this method returns a <code>Name</code> instead of a
     * <code>String</code>.
     *
     * @return the name of this item as <code>Name</code>
     * @throws RepositoryException if an error occurs.
     */
    public abstract Name getQName() throws RepositoryException;

    /**
     * Utility method that converts the given string into a qualified JCR name.
     *
     * @param name name string
     * @return qualified name
     * @throws RepositoryException if the given name is invalid
     */
    protected Name getQName(String name) throws RepositoryException {
        return sessionContext.getQName(name);
    }

    /**
     * Utility method that returns the value factory of this session.
     *
     * @return value factory
     * @throws RepositoryException if the value factory is not available
     */
    protected ValueFactory getValueFactory() throws RepositoryException {
        return getSession().getValueFactory();
    }

    /**
     * Utility method that converts the given strings into JCR values of the
     * given type
     *
     * @param values value strings
     * @param type value type
     * @return JCR values
     * @throws RepositoryException if the values can not be converted
     */
    protected Value[] getValues(String[] values, int type)
            throws RepositoryException {
        if (values != null) {
            return ValueHelper.convert(values, type, getValueFactory());
        } else {
            return null;
        }
    }

    /**
     * Utility method that returns the type of the first of the given values,
     * or {@link PropertyType#UNDEFINED} when given no values.
     *
     * @param values given values, or <code>null</code>
     * @return value type, or {@link PropertyType#UNDEFINED}
     */
    protected int getType(Value[] values) {
        if (values != null) {
            for (Value value : values) {
                if (value != null) {
                    return value.getType();
                }
            }
        }
        return PropertyType.UNDEFINED;
    }

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
    public void remove() throws RepositoryException {
        perform(new ItemRemoveOperation(this, true));
    }

    /**
     * {@inheritDoc}
     */
    public void save() throws RepositoryException {
        perform(new ItemSaveOperation(getItemState()));
    }

    /**
     * {@inheritDoc}
     */
    public void refresh(boolean keepChanges) throws RepositoryException {
        perform(new ItemRefreshOperation(getItemState(), keepChanges));
    }

    /**
     * {@inheritDoc}
     */
    public Item getAncestor(final int degree) throws RepositoryException {
        return perform(new SessionOperation<Item>() {
            public Item perform(SessionContext context)
                    throws RepositoryException {
                if (degree == 0) {
                    return context.getItemManager().getRootNode();
                }

                try {
                    // Path.getAncestor requires relative degree, i.e. we need
                    // to convert absolute to relative ancestor degree
                    Path path = getPrimaryPath();
                    int relDegree = path.getAncestorCount() - degree;
                    if (relDegree < 0) {
                        throw new ItemNotFoundException();
                    } else if (relDegree == 0) {
                        return ItemImpl.this; // shortcut
                    }
                    Path ancestorPath = path.getAncestor(relDegree);
                    return context.getItemManager().getNode(ancestorPath);
                } catch (PathNotFoundException e) {
                    throw new ItemNotFoundException("Ancestor not found", e);
                }
            }
            public String toString() {
                return "item.getAncestor(" + degree + ")";
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public String getPath() throws RepositoryException {
        return perform(new SessionOperation<String>() {
            public String perform(SessionContext context)
                    throws RepositoryException {
                return context.getJCRPath(getPrimaryPath());
            }
            public String toString() {
                return "item.getPath()";
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public int getDepth() throws RepositoryException {
        return perform(new SessionOperation<Integer>() {
            public Integer perform(SessionContext context)
                    throws RepositoryException {
                ItemState state = getItemState();
                if (state.getParentId() == null) {
                    return 0; // shortcut
                } else {
                    return context.getHierarchyManager().getDepth(id);
                }
            }
            public String toString() {
                return "item.getDepth()";
            }
        });
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
        return sessionContext.getSessionImpl();
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
                    && getSession().getWorkspace().getName().equals(
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
