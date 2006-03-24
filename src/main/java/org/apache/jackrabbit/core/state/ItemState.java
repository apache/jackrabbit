/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.util.WeakIdentityCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;

/**
 * <code>ItemState</code> represents the state of an <code>Item</code>.
 */
public abstract class ItemState implements ItemStateListener, Serializable {

    /** Serialization UID of this class. */
    static final long serialVersionUID = -1473610775880779769L;

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
     * 'existing', i.e. persistent state that has been persistently modified by somebody else
     */
    public static final int STATUS_STALE_MODIFIED = 5;
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
     * Listeners (weak references)
     */
    private final transient Collection listeners = new WeakIdentityCollection(5);

    /**
     * the backing persistent item state (may be null)
     */
    protected transient ItemState overlayedState;

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
            default:
                String msg = "illegal status: " + initialStatus;
                log.debug(msg);
                throw new IllegalArgumentException(msg);
        }
        this.isTransient = isTransient;
        connect(overlayedState);
    }

    /**
     * Copy state information from another state into this state
     * @param state source state information
     */
    protected abstract void copy(ItemState state);

    /**
     * Pull state information from overlayed state.
     */
    void pull() {
        if (overlayedState != null) {
            copy(overlayedState);
            // sync modification count
            modCount = overlayedState.getModCount();
        }
    }

    /**
     * Push state information into overlayed state.
     */
    void push() {
        if (overlayedState != null) {
            overlayedState.copy(this);
        }
    }

    /**
     * Called by <code>TransientItemStateManager</code> and
     * <code>LocalItemStateManager</code> when this item state has been disposed.
     */
    void onDisposed() {
        // prepare this instance so it can be gc'ed
        synchronized (listeners) {
            listeners.clear();
        }
        disconnect();
        overlayedState = null;
        status = STATUS_UNDEFINED;
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
     * Notify the listeners that the persistent state this object is
     * representing has been discarded.
     */
    protected void notifyStateDiscarded() {
        // copy listeners to array to avoid ConcurrentModificationException
        ItemStateListener[] la;
        synchronized (listeners) {
            la = (ItemStateListener[]) listeners.toArray(new ItemStateListener[listeners.size()]);
        }
        for (int i = 0; i < la.length; i++) {
            if (la[i] != null) {
                la[i].stateDiscarded(this);
            }
        }
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
    public void notifyStateUpdated() {
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
        if (isTransient) {
            return status == STATUS_STALE_MODIFIED
                    || status == STATUS_STALE_DESTROYED;
        } else {
            return overlayedState != null
                    && modCount != overlayedState.getModCount();
        }
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
            case STATUS_STALE_MODIFIED:
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

    //----------------------------------------------------< ItemStateListener >
    /**
     * {@inheritDoc}
     */
    public void stateCreated(ItemState created) {
        // underlying state has been permanently created
        status = STATUS_EXISTING;
        pull();
    }

    /**
     * {@inheritDoc}
     */
    public void stateDestroyed(ItemState destroyed) {
        // underlying state has been permanently destroyed
        if (isTransient) {
            status = STATUS_STALE_DESTROYED;
        } else {
            status = STATUS_EXISTING_REMOVED;
            notifyStateDestroyed();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stateModified(ItemState modified) {
        // underlying state has been modified
        if (isTransient) {
            status = STATUS_STALE_MODIFIED;
        } else {
            synchronized (this) {
                // this instance represents existing state, update it
                pull();
                notifyStateUpdated();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stateDiscarded(ItemState discarded) {
        // underlying persistent state has been discarded, discard this instance too
        discard();
    }

    //-------------------------------------------------< Serializable support >
    private void writeObject(ObjectOutputStream out) throws IOException {
        // delegate to default implementation
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        // delegate to default implementation
        in.defaultReadObject();
    }
}
