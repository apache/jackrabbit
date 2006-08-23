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
package org.apache.jackrabbit.jcr2spi.state;

import org.apache.jackrabbit.util.WeakIdentityCollection;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.IdFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;

/**
 * <code>ItemState</code> represents the state of an <code>Item</code>.
 */
public abstract class ItemState implements ItemStateListener {

    /**
     * Logger instance
     */
    private static Logger log = LoggerFactory.getLogger(ItemState.class);

     //----------------< flags defining the current status of this instance >---
    /**
     * 'existing', i.e. persistent state
     */
    public static final int STATUS_EXISTING = 1;
    /**
     * 'existing', i.e. persistent state that has been transiently modified (copy-on-write)
     */
    public static final int STATUS_EXISTING_MODIFIED = 2;
    /**
     * 'existing', i.e. persistent state that has been transiently removed (copy-on-write)
     */
    public static final int STATUS_EXISTING_REMOVED = 3;
    /**
     * 'new' state
     */
    public static final int STATUS_NEW = 4;
    /**
     * 'existing', i.e. persistent state that has been persistently modified by somebody else
     */
    public static final int STATUS_STALE_MODIFIED = 5;
    /**
     * 'existing', i.e. persistent state that has been destroyed by somebody else
     */
    public static final int STATUS_STALE_DESTROYED = 6;

    /**
     * a new state was deleted and is now 'removed'
     */
    public static final int STATUS_REMOVED = 7;

    /**
     * the internal status of this item state
     */
    protected int status;

    /**
     * Flag indicating whether this state is transient
     */
    private final boolean isTransient;

    /**
     * Listeners (weak references)
     */
    private final transient Collection listeners = new WeakIdentityCollection(5);

    // TODO: check again...
    /**
     *  IdFactory used to build id of the states
     */
    final IdFactory idFactory;

    /**
     * The parent <code>NodeState</code> or <code>null</code> if this
     * instance represents the root node.
     */
    NodeState parent;

    /**
     * the backing persistent item state (may be null)
     */
    transient ItemState overlayedState;

    /**
     * Constructs a new unconnected item state
     *
     * @param parent
     * @param initialStatus the initial status of the item state object
     * @param isTransient   flag indicating whether this state is transient or not
     */
    protected ItemState(NodeState parent, int initialStatus, boolean isTransient,
                        IdFactory idFactory) {
        switch (initialStatus) {
            case STATUS_EXISTING:
            case STATUS_NEW:
                status = initialStatus;
                break;
            default:
                String msg = "illegal status: " + initialStatus;
                log.debug(msg);
                throw new IllegalArgumentException(msg);
        }
        this.parent = parent;
        overlayedState = null;
        this.isTransient = isTransient;
        this.idFactory = idFactory;
    }

    /**
     * Constructs a new item state that is initially connected to an overlayed
     * state.
     *
     * @param overlayedState the backing item state being overlayed
     * @param initialStatus the initial status of the new <code>ItemState</code> instance
     * @param isTransient   flag indicating whether this state is transient or not
     */
    protected ItemState(ItemState overlayedState, NodeState parent, int initialStatus,
                        boolean isTransient, IdFactory idFactory) {
        switch (initialStatus) {
            case STATUS_EXISTING:
            case STATUS_EXISTING_MODIFIED:
            case STATUS_EXISTING_REMOVED:
                status = initialStatus;
                break;
            default:
                String msg = "illegal status: " + initialStatus;
                log.debug(msg);
                throw new IllegalArgumentException(msg);
        }
        this.isTransient = isTransient;
        this.parent = parent;
        this.idFactory = idFactory;
        connect(overlayedState);
    }

    /**
     * Copy state information from another state into this state
     * @param state source state information
     */
    abstract void copyFrom(ItemState state);

    /**
     * Pull state information from overlayed state.
     */
    void pull() {
        if (overlayedState != null) {
            copyFrom(overlayedState);
        }
    }

    /**
     * Push state information into overlayed state.
     */
    void push() {
        if (overlayedState != null) {
            overlayedState.copyFrom(this);
        }
    }

    /**
     * Connect this state to some underlying overlayed state.
     */
    protected void connect(ItemState overlayedState) {
        if (this.overlayedState != null) {
            if (this.overlayedState != overlayedState) {
                throw new IllegalStateException("Item state already connected to another underlying state: " + this);
            }
        }
        this.overlayedState = overlayedState;
        this.overlayedState.addListener(this);
    }

    /**
     * Reconnect this state to the overlayed state that it has been
     * disconnected from earlier.
     */
    protected void reconnect() {
        if (this.overlayedState == null) {
            throw new IllegalStateException("Item state cannot be reconnected because there's no underlying state to reconnect to: " + this);
        }
        this.overlayedState.addListener(this);
    }

    /**
     * Disconnect this state from the underlying overlayed state.
     */
    protected void disconnect() {
        if (overlayedState != null) {
            // de-register listener on overlayed state...
            overlayedState.removeListener(this);
            overlayedState = null;
        }
    }

    /**
     * Refreshes this item state
     */
    protected void refresh() {
        // TODO: how is this done? where is the new state retrieved from???
        // TODO: pass in as argument?
    }

    /**
     * Notify the listeners that the persistent state this object is
     * representing has been created.
     */
    protected void notifyStateCreated() {
        // copy listeners to array to avoid ConcurrentModificationException
        ItemStateListener[] la;
        synchronized (listeners) {
            la = (ItemStateListener[]) listeners.toArray(new ItemStateListener[listeners.size()]);
        }
        for (int i = 0; i < la.length; i++) {
            if (la[i] != null) {
                la[i].stateCreated(this);
            }
        }
    }

    /**
     * Notify the listeners that the persistent state this object is
     * representing has been updated.
     */
    protected void notifyStateUpdated() {
        // copy listeners to array to avoid ConcurrentModificationException
        ItemStateListener[] la;
        synchronized (listeners) {
            la = (ItemStateListener[]) listeners.toArray(new ItemStateListener[listeners.size()]);
        }
        for (int i = 0; i < la.length; i++) {
            if (la[i] != null) {
                la[i].stateModified(this);
            }
        }
    }

    /**
     * Notify the listeners that the persistent state this object is
     * representing has been destroyed.
     */
    protected void notifyStateDestroyed() {
        // copy listeners to array to avoid ConcurrentModificationException
        ItemStateListener[] la;
        synchronized (listeners) {
            la = (ItemStateListener[]) listeners.toArray(new ItemStateListener[listeners.size()]);
        }
        for (int i = 0; i < la.length; i++) {
            if (la[i] != null) {
                la[i].stateDestroyed(this);
            }
        }
    }

    /**
     * Notify the life cycle listeners that this state has changed its status.
     */
    private void notifyStatusChanged(int oldStatus) {
        // copy listeners to array to avoid ConcurrentModificationException
        ItemStateListener[] la;
        synchronized (listeners) {
            la = (ItemStateListener[]) listeners.toArray(new ItemStateListener[listeners.size()]);
        }
        for (int i = 0; i < la.length; i++) {
            if (la[i] instanceof ItemStateLifeCycleListener) {
                ((ItemStateLifeCycleListener) la[i]).statusChanged(this, oldStatus);
            }
        }
    }

    /**
     * Marks this item state as modified.
     */
    protected void markModified() {
        switch (status) {
            case STATUS_EXISTING:
                setStatus(STATUS_EXISTING_MODIFIED);
                break;
            case STATUS_EXISTING_MODIFIED:
                // already modified, do nothing
                break;
            case STATUS_NEW:
                // still new, do nothing
                break;
            case STATUS_STALE_DESTROYED:
            case STATUS_STALE_MODIFIED:
                // should actually get here because item should check before
                // it modifies an item state. do nothing because item state
                // is stale anyway.
                break;
            case STATUS_EXISTING_REMOVED:
            default:
                String msg = "Cannot mark item state with status " + status + " modified.";
                throw new IllegalStateException(msg);
        }
    }


    //--------------------< public READ methods and package private Setters >---
    /**
     * Determines if this item state represents a node.
     *
     * @return true if this item state represents a node, otherwise false.
     */
    public abstract boolean isNode();

    /**
     * Returns the identifier of this item state.
     *
     * @return the identifier of this item state..
     */
    public abstract ItemId getId();

    /**
     * Returns <code>true</code> if this item state represents new or modified
     * state (i.e. the result of copy-on-write) or <code>false</code> if it
     * represents existing, unmodified state.
     *
     * @return <code>true</code> if this item state is modified or new,
     *         otherwise <code>false</code>
     */
    public boolean isTransient() {
        return isTransient;
    }

    /**
     * Returns <code>true</code> if this item state is valid, that is its status
     * is one of:
     * <ul>
     * <li>{@link #STATUS_EXISTING}</li>
     * <li>{@link #STATUS_EXISTING_MODIFIED}</li>
     * <li>{@link #STATUS_NEW}</li>
     * </ul>
     * @return
     */
    public boolean isValid() {
        return status == STATUS_EXISTING || status == STATUS_EXISTING_MODIFIED || status == STATUS_NEW;
    }

    /**
     * Returns the parent <code>NodeState</code> or <code>null</code>
     * if either this item state represents the root node or this item state is
     * 'free floating', i.e. not attached to the repository's hierarchy.
     *
     * @return the parent <code>NodeState</code>
     */
    public NodeState getParent() {
        return parent;
    }

    /**
     * Returns the status of this item.
     *
     * @return the status of this item.
     */
    public int getStatus() {
        return status;
    }

    /**
     * Sets the new status of this item.
     *
     * @param newStatus the new status
     */
    void setStatus(int newStatus) {
        if (status == newStatus) {
            return;
        }
        int oldStatus = status;
        switch (newStatus) {
            case STATUS_NEW:
            case STATUS_EXISTING:
            case STATUS_EXISTING_REMOVED:
            case STATUS_EXISTING_MODIFIED:
            case STATUS_STALE_MODIFIED:
            case STATUS_STALE_DESTROYED:
                status = newStatus;
                break;
            default:
                String msg = "illegal status: " + newStatus;
                log.debug(msg);
                throw new IllegalArgumentException(msg);
        }
        notifyStatusChanged(oldStatus);
    }

    /**
     * Determines if this item state is overlying persistent state.
     *
     * @return <code>true</code> if this item state is overlying persistent
     *         state, otherwise <code>false</code>.
     */
    public boolean hasOverlayedState() {
        return overlayedState != null;
    }

    /**
     * Returns the persistent state backing <i>this</i> transient state or
     * <code>null</code> if there is no persistent state (i.e.. <i>this</i>
     * state is purely transient).
     *
     * @return the persistent item state or <code>null</code> if there is
     *         no persistent state.
     */
    public ItemState getOverlayedState() {
        return overlayedState;
    }

    /**
     * Removes this item state. This will change the status of this property
     * state to either {@link #STATUS_EXISTING_REMOVED} or {@link
     * #STATUS_REMOVED} depending on the current status.
     *
     * @throws ItemStateException if an error occurs while removing this item
     *                            state. e.g. this item state is not valid
     *                            anymore.
     */
    public abstract void remove() throws ItemStateException;

    /**
     * Reverts this item state to its initial status and adds itself to the Set
     * of <code>affectedItemStates</code> if it reverted itself.
     *
     * @param affectedItemStates the set of affected item states that reverted
     *                           themselfes.
     */
    public abstract void revert(Set affectedItemStates);

    /**
     * Checks if this <code>ItemState</code> is transiently modified or new and
     * adds itself to the <code>Set</code> of <code>transientStates</code> if
     * that is the case. It this <code>ItemState</code> has children it will
     * call the method {@link #collectTransientStates(java.util.Set)} on those
     * <code>ItemState</code>s.
     *
     * @param transientStates the <code>Set</code> of transient <code>ItemState</code>,
     *                        collected while the <code>ItemState</code>
     *                        hierarchy is traversed.
     */
    public abstract void collectTransientStates(Set transientStates);

    /**
     * Add an <code>ItemStateListener</code>
     *
     * @param listener the new listener to be informed on modifications
     */
    public void addListener(ItemStateListener listener) {
        synchronized (listeners) {
            assert (!listeners.contains(listener));
            listeners.add(listener);
        }
    }

    /**
     * Remove an <code>ItemStateListener</code>
     *
     * @param listener an existing listener
     */
    public void removeListener(ItemStateListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    //--------------------------------------------------< ItemStateListener >---
    /**
     * {@inheritDoc}
     */
    public void stateCreated(ItemState created) {
        // underlying state has been permanently created
        pull();
        setStatus(STATUS_EXISTING);
    }

    /**
     * {@inheritDoc}
     */
    public void stateDestroyed(ItemState destroyed) {
        // underlying state has been permanently destroyed
        if (isTransient) {
            setStatus(STATUS_STALE_DESTROYED);
        } else {
            setStatus(STATUS_EXISTING_REMOVED);
            notifyStateDestroyed();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stateModified(ItemState modified) {
        // underlying state has been modified
        if (isTransient) {
            setStatus(STATUS_STALE_MODIFIED);
        } else {
            synchronized (this) {
                // this instance represents existing state, update it
                pull();
                notifyStateUpdated();
            }
        }
    }
}
