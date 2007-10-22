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
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
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

        if (!entry.isAvailable()) {
            entry.setItemState(this);
        }
    }

    /**
     *
     * @param parent
     * @return
     */
    private static int getInitialStatus(NodeEntry parent) {
        int status = Status.EXISTING;
        // walk up hiearchy and check if any of the parents is transiently
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
    public abstract ItemId getId();

    /**
     * Utility method:
     * Returns the identifier of this item state. Shortcut for calling 'getWorkspaceId'
     * on the NodeEntry or PropertyEntry respectively.
     *
     * @return the identifier of this item state..
     */
    public abstract ItemId getWorkspaceId();

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
        if (Status.isValidStatusChange(oldStatus, newStatus)) {
            status = Status.getNewStatus(oldStatus, newStatus);
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
     * @return true if this state has been modified
     */
    public abstract boolean merge(ItemState another, boolean keepChanges);

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
     * @return
     */
    public Iterator getListeners() {
        return Collections.unmodifiableCollection(listeners).iterator();
    }

    /**
     * Used on the target state of a save call AFTER the changelog has been
     * successfully submitted to the SPI..
     *
     * @param changeLog
     * @throws IllegalStateException if this state is a 'workspace' state.
     */
    abstract void persisted(ChangeLog changeLog) throws IllegalStateException;

    /**
     * Retrieved a fresh ItemState from the persistent layer and merge its
     * data with this state in order to reload it. In case of a NEW state retrieving
     * the state from the persistent layer is only possible if the state has
     * been persisted.
     *
     * @param keepChanges
     */
    public void reload(boolean keepChanges) {
        ItemId id = getWorkspaceId();
        ItemState tmp;
        try {
            if (isNode()) {
                tmp = isf.createNodeState((NodeId) id, (NodeEntry) getHierarchyEntry());
            } else {
                tmp = isf.createPropertyState((PropertyId) id, (PropertyEntry) getHierarchyEntry());
            }
        } catch (ItemNotFoundException e) {
            // TODO: deal with moved items separately
            // remove hierarchyEntry (including all children and set
            // state-status to REMOVED (or STALE_DESTROYED)
            log.debug("Item '" + id + "' cannot be found on the persistent layer -> remove.");
            getHierarchyEntry().remove();
            return;
        } catch (RepositoryException e) {
            // TODO: rather throw? remove from parent?
            log.warn("Exception while reloading item state: " + e);
            log.debug("Stacktrace: ", e);
            return;
        }

        boolean modified = merge(tmp, keepChanges);
        if (status == Status.NEW || status == Status.INVALIDATED) {
            setStatus(Status.EXISTING);
        } else if (modified) {
            // start notification by marking this state modified.
            setStatus(Status.MODIFIED);
        }
    }

    /**
     * Marks this item state as modified.
     */
    void markModified() {
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
