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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.jcr2spi.nodetype.ItemDefinitionProvider;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.util.WeakIdentityCollection;
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

    /**
     * the internal status of this item state
     */
    private int status;

    /**
     * The hierarchy entry this state belongs to.
     */
    private final HierarchyEntry hierarchyEntry;

    /**
     * Listeners (weak references)
     */
    @SuppressWarnings("unchecked")
    private final transient Collection<ItemStateLifeCycleListener> listeners = new WeakIdentityCollection(5);

    /**
     * The <code>ItemStateFactory</code> which is used to create new
     * <code>ItemState</code> instances.
     */
    final ItemStateFactory isf;

    final ItemDefinitionProvider definitionProvider;

    /**
     * Constructs an item state
     *
     * @param entry
     * @param isf
     * @param definitionProvider
     */
    protected ItemState(HierarchyEntry entry, ItemStateFactory isf,
                        ItemDefinitionProvider definitionProvider) {
        this(getInitialStatus(entry.getParent()), entry, isf, definitionProvider);
    }

    /**
     * Constructs an item state
     *
     * @param entry
     * @param isf
     * @param definitionProvider
     */
    protected ItemState(int initialStatus, HierarchyEntry entry,
                        ItemStateFactory isf,
                        ItemDefinitionProvider definitionProvider) {
        if (entry == null) {
            throw new IllegalArgumentException("Cannot build ItemState from 'null' HierarchyEntry");
        }
        switch (initialStatus) {
            case Status.EXISTING:
            case Status.NEW:
            case Status.EXISTING_REMOVED:
                status = initialStatus;
                break;
            default:
                String msg = "illegal status: " + initialStatus;
                log.debug(msg);
                throw new IllegalArgumentException(msg);
        }
        this.hierarchyEntry = entry;
        this.isf = isf;
        this.definitionProvider = definitionProvider;
    }

    /**
     *
     * @param parent
     * @return
     */
    private static int getInitialStatus(NodeEntry parent) {
        int status = Status.EXISTING;
        // walk up hierarchy and check if any of the parents is transiently
        // removed, in which case the status must be set to EXISTING_REMOVED.
        while (parent != null) {
            if (parent.getStatus() == Status.EXISTING_REMOVED) {
                status = Status.EXISTING_REMOVED;
                break;
            }
            parent = parent.getParent();
        }
        return status;
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
     * Returns <code>true</code> if this item state is valid and can be accessed.
     * @return
     * @see Status#isValid(int)
     * @see Status#isStale(int)
     */
    public boolean isValid() {
        return Status.isValid(getStatus()) || Status.isStale(getStatus());
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
     * Returns the name of this state. Shortcut for calling 'getName' on the
     * {@link ItemState#getHierarchyEntry() hierarchy entry}.
     *
     * @return name of this state
     */
    public Name getName() {
        return getHierarchyEntry().getName();
    }

    /**
     * Utility method:
     * Returns the identifier of this item state. Shortcut for calling 'getId'
     * on the {@link ItemState#getHierarchyEntry() hierarchy entry}.
     *
     * @return the identifier of this item state..
     */
    public abstract ItemId getId() throws RepositoryException;

    /**
     * Utility method:
     * Returns the identifier of this item state. Shortcut for calling 'getWorkspaceId'
     * on the NodeEntry or PropertyEntry respectively.
     *
     * @return the identifier of this item state..
     */
    public abstract ItemId getWorkspaceId() throws RepositoryException;

    /**
     * Utility method:
     * Returns the path of this item state. Shortcut for calling
     * 'getPath' on the {@link ItemState#getHierarchyEntry() hierarchy entry}.
     *
     * @return
     * @throws RepositoryException if an error occurs
     */
    public Path getPath() throws RepositoryException {
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
        // safeguard against root node's null parent
        NodeEntry parent = getHierarchyEntry().getParent();
        if (parent != null) {
            return getHierarchyEntry().getParent().getNodeState();
        }
        return null;
    }

    /**
     * Returns the status of this item.
     *
     * @return the status of this item.
     */
    public final int getStatus() {
        // Call calculateStatus to apply a possible pending invalidation
        // in the entry hierarchy.
        getHierarchyEntry().calculateStatus();
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

        if (oldStatus == Status.REMOVED) {
            throw new IllegalStateException("State is already in terminal status " + Status.getName(oldStatus));
        }
        if (Status.isValidStatusChange(oldStatus, newStatus)) {
            status = Status.getNewStatus(oldStatus, newStatus);
        } else {
            throw new IllegalArgumentException("Invalid new status " + Status.getName(newStatus) + " for state with status " + Status.getName(oldStatus));
        }
        // Notify listeners about status change
        // copy listeners to array to avoid ConcurrentModificationException
        ItemStateLifeCycleListener[] la;
        synchronized (listeners) {
            la = listeners.toArray(new ItemStateLifeCycleListener[listeners.size()]);
        }
        for (int i = 0; i < la.length; i++) {
            if (la[i] != null) {
                la[i].statusChanged(this, oldStatus);
            }
        }
        if (status == Status.MODIFIED) {
            /*
            change back tmp MODIFIED status, that is used as marker only
            inform listeners about (external) changes.
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
     * @return a MergeResult instance which represent the result of the merge operation
     */
    public abstract MergeResult merge(ItemState another, boolean keepChanges);

    /**
     * Revert all transient modifications made to this ItemState.
     *
     * @return true if this state has been modified i.e. if there was anything
     * to revert.
     */
    public abstract boolean revert();

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
     * @return iterator over <code>ItemStateLifeCycleListener</code>s.
     */
    public Iterator<ItemStateLifeCycleListener> getListeners() {
        return Collections.unmodifiableCollection(listeners).iterator();
    }

    /**
     * Invalidates this state: set its {@link Status} to {@link Status#INVALIDATED}
     * if the current status is {@link Status#EXISTING}. Does nothing otherwise.
     */
    public void invalidate() {
        if (status == Status.EXISTING) {
            setStatus(Status.INVALIDATED);
        } else {
            log.debug("Skip invalidation for item {} with status {}", getName(), Status.getName(status));
        }
    }

    /**
     * Marks this item state as modified.
     */
    void markModified() throws InvalidItemStateException {
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
                throw new InvalidItemStateException("Cannot mark stale state modified.");

            case Status.EXISTING_REMOVED:
            default:
                String msg = "Cannot mark item state with status '" + Status.getName(status) + "' modified.";
                throw new InvalidItemStateException(msg);
        }
    }

    // -----------------------------------------------------< MergeResult >---

    /**
     * A MergeResult represents the result of a {@link ItemState#merge(ItemState, boolean)}
     * operation.
     */
    public interface MergeResult {

        /**
         * @return  true iff the target state of {@link ItemState#merge(ItemState, boolean)}
         * was modified.
         */
        public boolean modified();

        /**
         * Dispose this MergeResult and release all internal resources that
         * are not needed any more.
         */
        public void dispose();
    }

    /**
     * A SimpleMergeResult is just a holder for a modification status.
     * The {@link #modified()} method just returns the modification status passed
     * to the constructor.
     */
    protected class SimpleMergeResult implements MergeResult {
        private final boolean modified;

        /**
         * @param modified  modification status
         */
        public SimpleMergeResult(boolean modified) {
            this.modified = modified;
        }

        public boolean modified() {
            return modified;
        }

        public void dispose() {
            // nothing to do.
        }
    }

}
