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
package org.apache.jackrabbit.core.state;

import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>ItemState</code> represents the state of an <code>Item</code>.
 */
public abstract class ItemState {

    /**
     * Logger instance
     */
    private static Logger log = LoggerFactory.getLogger(ItemState.class);

     //------------------< flags defining the current status of this instance >
    /**
     * the status is undefined
     */
    public static final int STATUS_UNDEFINED = 0;
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
     * 'existing', i.e. persistent state that has been destroyed by somebody else
     */
    public static final int STATUS_STALE_DESTROYED = 6;

    /**
     * the internal status of this item state
     */
    protected int status = STATUS_UNDEFINED;

    /**
     * a modification counter used to prevent concurrent modifications
     */
    private short modCount;

    /**
     * Flag indicating whether this state is transient
     */
    private final boolean isTransient;

    /**
     * Parent container.
     */
    private ItemStateListener container;

    /**
     * the backing persistent item state (may be null)
     */
    protected ItemState overlayedState;

    /**
     * Constructs a new unconnected item state
     *
     * @param initialStatus the initial status of the item state object
     * @param isTransient   flag indicating whether this state is transient or not
     */
    protected ItemState(int initialStatus, boolean isTransient) {
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
        modCount = 0;
        overlayedState = null;
        this.isTransient = isTransient;
    }

    /**
     * Constructs a new item state that is initially connected to an overlayed
     * state.
     *
     * @param overlayedState the backing item state being overlayed
     * @param initialStatus the initial status of the new <code>ItemState</code> instance
     * @param isTransient   flag indicating whether this state is transient or not
     */
    protected ItemState(ItemState overlayedState, int initialStatus, boolean isTransient) {
        switch (initialStatus) {
            case STATUS_EXISTING:
            case STATUS_EXISTING_MODIFIED:
            case STATUS_EXISTING_REMOVED:
                status = initialStatus;
                break;

            case STATUS_UNDEFINED:
                // see http://issues.apache.org/jira/browse/JCR-897
                log.debug("creating ItemState instance with initialStatus=" + STATUS_UNDEFINED + ", id=" + overlayedState.getId());
                status = initialStatus;
                break;

            default:
                String msg = "illegal status: " + initialStatus;
                log.debug(msg);
                throw new IllegalArgumentException(msg);
        }
        this.isTransient = isTransient;
        this.overlayedState = overlayedState;
    }

    /**
     * Copy state information from another state into this state
     * @param state source state information
     * @param syncModCount if the modCount should be synchronized.
     */
    public abstract void copy(ItemState state, boolean syncModCount);

    /**
     * Pull state information from overlayed state.
     */
    synchronized void pull() {
        ItemState state = overlayedState;
        if (state != null) {
            // sync modification count
            copy(state, true);
        }
    }

    /**
     * Push state information into overlayed state.
     */
    void push() {
        ItemState state = overlayedState;
        if (state != null) {
            state.copy(this, false);
        }
    }

    /**
     * Called by <code>TransientItemStateManager</code> and
     * <code>LocalItemStateManager</code> when this item state has been disposed.
     */
    void onDisposed() {
        disconnect();
        overlayedState = null;
        status = STATUS_UNDEFINED;
    }

    /**
     * Connect this state to some underlying overlayed state.
     */
    public void connect(ItemState overlayedState)
            throws ItemStateException {
        if (this.overlayedState != null
                && this.overlayedState != overlayedState) {
            throw new ItemStateException(
                    "Item state already connected to another"
                            + " underlying state: " + this);
        }
        this.overlayedState = overlayedState;
    }

    /**
     * Reconnect this state to the overlayed state that it has been
     * disconnected from earlier.
     */
    protected void reconnect() throws ItemStateException {
        if (this.overlayedState == null) {
            throw new ItemStateException(
                    "Item state cannot be reconnected because there's no"
                    + " underlying state to reconnect to: " + this);
        }
    }

    /**
     * Disconnect this state from the underlying overlayed state.
     */
    protected void disconnect() {
        if (overlayedState != null) {
            overlayedState = null;
        }
    }

    /**
     * Return a flag indicating whether this state is connected to some other state.
     * @return <code>true</code> if this state is connected, <code>false</code> otherwise.
     */
    protected boolean isConnected() {
        return overlayedState != null;
    }

    /**
     * Notify the parent container about changes to this state.
     */
    protected void notifyStateDiscarded() {
        if (container != null) {
            container.stateDiscarded(this);
        }
    }

    /**
     * Notify the parent container about changes to this state.
     */
    protected void notifyStateCreated() {
        if (container != null) {
            container.stateCreated(this);
        }
    }

    /**
     * Notify the parent container about changes to this state.
     */
    public void notifyStateUpdated() {
        if (container != null) {
            container.stateModified(this);
        }
    }

    /**
     * Notify the parent container about changes to this state.
     */
    protected void notifyStateDestroyed() {
        if (container != null) {
            container.stateDestroyed(this);
        }
    }

    //-------------------------------------------------------< public methods >
    /**
     * Determines if this item state represents a node.
     *
     * @return true if this item state represents a node, otherwise false.
     */
    public abstract boolean isNode();

    /**
     * Returns the identifier of this item.
     *
     * @return the id of this item.
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
     * Determines whether this item state has become stale.
     * @return true if this item state has become stale, false otherwise.
     */
    public boolean isStale() {
        return overlayedState != null
                && modCount != overlayedState.getModCount();
    }

    /**
     * Returns the NodeId of the parent <code>NodeState</code> or <code>null</code>
     * if either this item state represents the root node or this item state is
     * 'free floating', i.e. not attached to the repository's hierarchy.
     *
     * @return the parent <code>NodeState</code>'s Id
     */
    public abstract NodeId getParentId();

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
    public void setStatus(int newStatus) {
        switch (newStatus) {
            case STATUS_NEW:
            case STATUS_EXISTING:
            case STATUS_EXISTING_REMOVED:
            case STATUS_EXISTING_MODIFIED:
            case STATUS_STALE_DESTROYED:
            case STATUS_UNDEFINED:
                status = newStatus;
                return;
            default:
                String msg = "illegal status: " + newStatus;
                log.debug(msg);
                throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Returns the modification count.
     *
     * @return the modification count.
     */
    public short getModCount() {
        return modCount;
    }

    /**
     * Sets the modification count.
     *
     * @param modCount the modification count of this item
     */
    public void setModCount(short modCount) {
        this.modCount = modCount;
    }

    /**
     * Updates the modification count.
     */
    synchronized void touch() {
        modCount++;
    }

    /**
     * Discards this instance, i.e. renders it 'invalid'.
     */
    public void discard() {
        if (status != STATUS_UNDEFINED) {
            // notify listeners
            notifyStateDiscarded();
            // reset status
            status = STATUS_UNDEFINED;
        }
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
     * Set the parent container that will receive notifications about changes to this state.
     * @param container container to be informed on modifications
     */
    public void setContainer(ItemStateListener container) {
        if (this.container != null) {
            throw new IllegalStateException("State already connected to a container: " + this.container);
        }
        this.container = container;
    }

    /**
     * Return the parent container that will receive notifications about changes to this state. Returns
     * <code>null</code> if none has been yet assigned.
     * @return container or <code>null</code>
     */
    public ItemStateListener getContainer() {
        return container;
    }

    /**
     * Returns the approximate memory consumption of this state.
     *
     * @return the approximate memory consumption of this state.
     */
    public abstract long calculateMemoryFootprint();

}
