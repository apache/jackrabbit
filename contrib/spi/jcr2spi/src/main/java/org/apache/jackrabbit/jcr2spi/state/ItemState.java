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
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.PropertyEntry;
import org.apache.jackrabbit.jcr2spi.nodetype.ItemDefinitionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.ItemNotFoundException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Collections;

/**
 * <code>ItemState</code> represents the state of an <code>Item</code>.
 */
public abstract class ItemState implements ItemStateLifeCycleListener {

    /**
     * Logger instance
     */
    private static Logger log = LoggerFactory.getLogger(ItemState.class);

    /**
     * Flag used to distinguish workspace states from session states. The latter
     * will be able to handle the various methods related to transient
     * modifications.
     */
    private final boolean isWorkspaceState;

    /**
     * the internal status of this item state
     */
    private int status;

    /**
     * The hierarchy entry this state belongs to.
     */
    private HierarchyEntry hierarchyEntry;

    /**
     * Listeners (weak references)
     */
    private final transient Collection listeners = new WeakIdentityCollection(5);

    /**
     * The <code>ItemStateFactory</code> which is used to create new
     * <code>ItemState</code> instances.
     */
    final ItemStateFactory isf;

    final ItemDefinitionProvider definitionProvider;

    /**
     * the backing persistent item state (may be null)
     */
    transient ItemState overlayedState;

    /**
     * Constructs a new unconnected item state
     *
     * @param initialStatus
     * @param isWorkspaceState
     */
    protected ItemState(int initialStatus, boolean isWorkspaceState,
                        HierarchyEntry entry, ItemStateFactory isf,
                        ItemDefinitionProvider definitionProvider) {
        switch (initialStatus) {
            case Status.EXISTING:
            case Status.NEW:
                status = initialStatus;
                break;
            default:
                String msg = "illegal status: " + initialStatus;
                log.debug(msg);
                throw new IllegalArgumentException(msg);
        }
        if (entry == null) {
            throw new IllegalArgumentException("Cannot build ItemState from 'null' HierarchyEntry");
        }
        this.hierarchyEntry = entry;
        this.isf = isf;
        this.definitionProvider = definitionProvider;
        this.isWorkspaceState = isWorkspaceState;

        overlayedState = null;
    }

    /**
     * Constructs a new item state that is initially connected to an overlayed
     * state.
     *
     * @param overlayedState
     * @param initialStatus
     */
    protected ItemState(ItemState overlayedState, int initialStatus, ItemStateFactory isf) {
        switch (initialStatus) {
            case Status.EXISTING:
            case Status.EXISTING_MODIFIED:
            case Status.EXISTING_REMOVED:
                status = initialStatus;
                break;
            default:
                String msg = "illegal status: " + initialStatus;
                log.debug(msg);
                throw new IllegalArgumentException(msg);
        }
        if (overlayedState.getHierarchyEntry() == null) {
            throw new IllegalArgumentException("Cannot build ItemState from 'null' HierarchyEntry");
        }
        this.hierarchyEntry = overlayedState.getHierarchyEntry();
        this.isf = isf;
        this.isWorkspaceState = false;
        this.definitionProvider = overlayedState.definitionProvider;
        connect(overlayedState);
    }

    //----------------------------------------------------------< ItemState >---
    /**
     * The <code>HierarchyEntry</code> corresponding to this <code>ItemState</code>.
     *
     * @return The <code>HierarchyEntry</code> corresponding to this <code>ItemState</code>.
     */
    public HierarchyEntry getHierarchyEntry() {
        return hierarchyEntry;
    }

    /**
     * Returns <code>true</code> if this item state is valid, that is its status
     * is one of:
     * <ul>
     * <li>{@link Status#EXISTING}</li>
     * <li>{@link Status#EXISTING_MODIFIED}</li>
     * <li>{@link Status#NEW}</li>
     * </ul>
     * @return
     */
    public boolean isValid() {
        return Status.isValid(getStatus());
    }

    /**
     * Utility method:
     * Determines if this item state represents a node.
     *
     * @return true if this item state represents a node, otherwise false.
     */
    public abstract boolean isNode();

    /**
     * Utility method:
     * Returns the name of this state. Shortcut for calling 'getQName' on the
     * {@link ItemState#getHierarchyEntry() hierarchy entry}.
     *
     * @return name of this state
     */
    public QName getQName() {
        return getHierarchyEntry().getQName();
    }

    /**
     * Utility method:
     * Returns the identifier of this item state. Shortcut for calling 'getId'
     * on the {@link ItemState#getHierarchyEntry() hierarchy entry}.
     *
     * @return the identifier of this item state..
     */
    public abstract ItemId getId();

    /**
     * Utility method:
     * Returns the qualified path of this item state. Shortcut for calling
     * 'getPath' on the {@link ItemState#getHierarchyEntry() hierarchy entry}.
     *
     * @return
     * @throws RepositoryException if an error occurs
     */
    public Path getQPath() throws RepositoryException {
        return getHierarchyEntry().getPath();
    }

    /**
     * Utility method: Shortcut for calling
     * 'getParent().getNodeState()' on the {@link ItemState#getHierarchyEntry()
     * hierarchy entry}.
     *
     * @return
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    public NodeState getParent() throws ItemNotFoundException, RepositoryException {
        return getHierarchyEntry().getParent().getNodeState();
    }

    /**
     * Returns the status of this item.
     *
     * @return the status of this item.
     */
    public final int getStatus() {
        return status;
    }

    /**
     * Sets the new status of this item.
     *
     * @param newStatus the new status
     */
    public void setStatus(int newStatus) {
        int oldStatus = status;
        if (oldStatus == newStatus) {
            return;
        }

        if (Status.isTerminal(oldStatus)) {
            throw new IllegalStateException("State is already in terminal status " + Status.getName(oldStatus));
        }
        if (Status.isValidStatusChange(oldStatus, newStatus, isWorkspaceState)) {
            status = newStatus;
        } else {
            throw new IllegalArgumentException("Invalid new status " + Status.getName(newStatus) + " for state with status " + Status.getName(oldStatus));
        }
        // notifiy listeners about status change
        // copy listeners to array to avoid ConcurrentModificationException
        ItemStateLifeCycleListener[] la;
        synchronized (listeners) {
            la = (ItemStateLifeCycleListener[]) listeners.toArray(new ItemStateLifeCycleListener[listeners.size()]);
        }
        for (int i = 0; i < la.length; i++) {
            if (la[i] != null) {
                la[i].statusChanged(this, oldStatus);
            }
        }
        if (status == Status.MODIFIED) {
            /*
            change back tmp MODIFIED status, that is used as marker only to
            force the overlaying state to synchronize as well as to inform
            other listeners about changes.
            */
            status = Status.EXISTING;
        }
    }

    /**
     * Merge all data from the given state into this state. If
     * '<code>keepChanges</code>' is true, transient modifications present on
     * this state are not touched. Otherwise this state is completely reset
     * according to the given other state.
     *
     * @param another
     * @param keepChanges
     * @return true if this state has been modified
     */
     public abstract boolean merge(ItemState another, boolean keepChanges);

    /**
     * Add an <code>ItemStateLifeCycleListener</code>
     *
     * @param listener the new listener to be informed on modifications
     */
    public void addListener(ItemStateLifeCycleListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    /**
     * Remove an <code>ItemStateLifeCycleListener</code>
     *
     * @param listener an existing listener
     */
    public void removeListener(ItemStateLifeCycleListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    /**
     * Unmodifiable iterator over the listeners present on this item state.
     *
     * @return
     */
    public Iterator getListeners() {
        return Collections.unmodifiableCollection(listeners).iterator();
    }

    //-----------------------------------------< ItemStateLifeCycleListener >---
    /**
     *
     * @param overlayed
     * @param previousStatus
     */
    public void statusChanged(ItemState overlayed, int previousStatus) {
        checkIsSessionState();
        overlayed.checkIsWorkspaceState();

        // the given state is the overlayed state this state (session) is listening to.
        if (overlayed == overlayedState) {
            switch (overlayed.getStatus()) {
                case Status.MODIFIED:
                    // underlying state has been modified by external changes
                    if (status == Status.EXISTING || status == Status.INVALIDATED) {
                        // temporarily set the state to MODIFIED in order to inform listeners.
                        setStatus(Status.MODIFIED);
                    } else if (status == Status.EXISTING_MODIFIED) {
                        // TODO: try to merge changes
                        setStatus(Status.STALE_MODIFIED);
                    }
                    // else: this status is EXISTING_REMOVED => ignore.
                    // no other status is possible.
                    break;
                case Status.REMOVED:
                    if (status == Status.EXISTING_MODIFIED) {
                        setStatus(Status.STALE_DESTROYED);
                    } else {
                        setStatus(Status.REMOVED);
                    }
                    break;
                case Status.INVALIDATED:
                    // invalidate this session state if it is EXISTING.
                    if (status == Status.EXISTING) {
                        setStatus(Status.INVALIDATED);
                    }
                    break;
                default:
                    // Should never occur, since 'setStatus(int)' already validates
                    log.error("Workspace state cannot have its state changed to " + overlayed.getStatus());
                    break;
            }
        }
    }

    //--------------------------------------------------------< State types >---
    /**
     * @return true if this state is a workspace state.
     */
    public boolean isWorkspaceState() {
        return isWorkspaceState;
    }

    /**
     * Returns <i>this</i>, if {@link #isWorkspaceState()} returns <code>true</code>.
     * Otherwise this method returns the workspace state backing <i>this</i>
     * 'session' state or <code>null</code> if this state is new.
     *
     * @return the workspace state or <code>null</code> if this state is new.
     */
    public ItemState getWorkspaceState() {
        if (isWorkspaceState) {
            return this;
        } else {
            return overlayedState;
        }
    }

    /**
     * @throws IllegalStateException if this state is a 'session' state.
     */
    public void checkIsWorkspaceState() {
        if (!isWorkspaceState) {
            throw new IllegalStateException("State " + this + " is not a 'workspace' state.");
        }
    }

    /**
     * @throws IllegalStateException if this state is a 'workspace' state.
     */
    public void checkIsSessionState() {
        if (isWorkspaceState) {
            throw new IllegalStateException("State " + this + " is not a 'session' state.");
        }
    }

    /**
     * @return true, if this state is overlaying a workspace state.
     */
    public boolean hasOverlayedState() {
        return overlayedState != null;
    }

    //----------------------------------------------------< Session - State >---

    /**
     * Used on the target state of a save call AFTER the changelog has been
     * successfully submitted to the SPI..
     *
     * @param changeLog
     * @throws IllegalStateException if this state is a 'workspace' state.
     */
    abstract void persisted(ChangeLog changeLog) throws IllegalStateException;

    /**
     * Connect this state to some underlying overlayed state.
     */
    private void connect(ItemState overlayedState) {
        checkIsSessionState();
        overlayedState.checkIsWorkspaceState();

        if (this.overlayedState == null) {
            setOverLayedState(overlayedState);
        } else if (this.overlayedState != overlayedState) {
            throw new IllegalStateException("Item state already connected to another underlying state: " + this);
        } // attempt to connect state to its ol-state again -> nothing to do.
    }

    /**
     * Replaces the overlayedState with a new instance retrieved from the
     * persistent layer thus forcing a reload of this ItemState or in case
     * of a NEW state, retrieves the overlayed state after the state has been
     * persisted and connects the NEW state. Note, that in the latter case,
     * the parent must already be connected to its overlayed state.
     *
     * @param keepChanges
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    public void reconnect(boolean keepChanges) throws ItemNotFoundException, RepositoryException {
        checkIsSessionState();
        // Need to use the workspace-ISF in order not to create a session-state.
        ItemStateFactory wspIsf;
        if (overlayedState != null) {
            wspIsf = overlayedState.isf;
        } else {
            wspIsf = getParent().overlayedState.isf;
        }
        ItemId id = (overlayedState == null) ? getId() : overlayedState.getId();
        ItemState overlayed;
        if (isNode()) {
            overlayed = wspIsf.createNodeState((NodeId) id, (NodeEntry) getHierarchyEntry());
        } else {
            overlayed = wspIsf.createPropertyState((PropertyId) id, (PropertyEntry) getHierarchyEntry());
        }
        setOverLayedState(overlayed);
        boolean modified = merge(overlayed, keepChanges);
        if (status == Status.NEW || status == Status.INVALIDATED) {
            setStatus(Status.EXISTING);
        } else if (modified) {
            // start notification by marking ol-state modified.
            overlayed.setStatus(Status.MODIFIED);
        }
    }

    /**
     *
     * @param overlayedState
     */
    private void setOverLayedState(ItemState overlayedState) {
        if (this.overlayedState != null) {
           this.overlayedState.removeListener(this);
        }
        this.overlayedState = overlayedState;
        this.overlayedState.addListener(this);
    }

    /**
     * Marks this item state as modified.
     */
    void markModified() {
        checkIsSessionState();

        switch (status) {
            case Status.EXISTING:
                setStatus(Status.EXISTING_MODIFIED);
                break;
            case Status.EXISTING_MODIFIED:
                // already modified, do nothing
                break;
            case Status.NEW:
                // still new, do nothing
                break;
            case Status.STALE_DESTROYED:
            case Status.STALE_MODIFIED:
                // should actually not get here because item should check before
                // it modifies an item state.
                throw new IllegalStateException("Cannot mark stale state modified.");

            case Status.EXISTING_REMOVED:
            default:
                String msg = "Cannot mark item state with status " + status + " modified.";
                throw new IllegalStateException(msg);
        }
    }
}
