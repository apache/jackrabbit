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
package org.apache.jackrabbit.jcr2spi.hierarchy;

import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.MalformedPathException;
import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.ItemStateException;
import org.apache.jackrabbit.jcr2spi.state.NoSuchItemStateException;
import org.apache.jackrabbit.jcr2spi.state.ChangeLog;
import org.apache.jackrabbit.jcr2spi.state.StaleItemStateException;
import org.apache.jackrabbit.jcr2spi.state.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.lang.ref.WeakReference;

/**
 * <code>HierarchyEntryImpl</code> implements base functionality for child node
 * and property references.
 */
abstract class HierarchyEntryImpl implements HierarchyEntry {

    private static Logger log = LoggerFactory.getLogger(HierarchyEntryImpl.class);

    /**
     * Cached weak reference to the target ItemState.
     * // TODO: check correct?
     */
    private WeakReference target;

    /**
     * The name of the target item state.
     */
    protected QName name;

    /**
     * The parent <code>HierarchyEntry</code>.
     */
    protected NodeEntryImpl parent;

    /**
     * The item state factory to create the the item state.
     */
    protected final EntryFactory factory;

    /**
     * Creates a new <code>HierarchyEntryImpl</code> with the given parent
     * <code>NodeState</code>.
     *
     * @param parent the <code>NodeEntry</code> that owns this child node
     *               reference.
     * @param name   the name of the child item.
     * @param factory
     */
    HierarchyEntryImpl(NodeEntryImpl parent, QName name, EntryFactory factory) {
        this.parent = parent;
        this.name = name;
        this.factory = factory;
    }
    
    /**
     * Resolves this <code>HierarchyEntryImpl</code> and returns the target
     * <code>ItemState</code> of this reference. This method may return a
     * cached <code>ItemState</code> if this method was called before already
     * otherwise this method will forward the call to {@link #doResolve()}
     * and cache its return value. If an existing state has been invalidated
     * before, an attempt is made to reload it in order to make sure, that
     * a call to {@link ItemState#isValid()} does not equivocally return false.
     *
     * @return the <code>ItemState</code> where this reference points to.
     * @throws NoSuchItemStateException if the referenced <code>ItemState</code>
     *                                  does not exist.
     * @throws ItemStateException       if an error occurs.
     */
    ItemState resolve() throws NoSuchItemStateException, ItemStateException {
        // check if already resolved
        ItemState state = internalGetItemState();
        // not yet resolved. retrieve and keep weak reference to state
        if (state == null) {
            state = doResolve();
            target = new WeakReference(state);
        } else if (state.getStatus() == Status.INVALIDATED) {
            // completely reload this entry, but don't reload recursively
            reload(false, false);
        }
        return state;
    }

    /**
     * Resolves this <code>HierarchyEntryImpl</code> and returns the target
     * <code>ItemState</code> of this reference.
     *
     * @return the <code>ItemState</code> where this reference points to.
     * @throws NoSuchItemStateException if the referenced <code>ItemState</code>
     *                                  does not exist.
     * @throws ItemStateException       if an error occurs.
     */
    abstract ItemState doResolve() throws NoSuchItemStateException, ItemStateException;

    /**
     * 
     * @return
     */
    ItemState internalGetItemState() {
        if (target != null) {
            ItemState state = (ItemState) target.get();
            if (state != null) {
                return state;
            }
        }
        return null;
    }

    /**
     * Set the target of this HierarchyEntry to the given new ItemState.
     *
     * @throws IllegalStateException if this entry has already been resolved.
     * @throws IllegalArgumentException if the given state is <code>null</code>
     * or has another Status than {@link Status#NEW} or in case of class mismatch.
     */
    void internalSetItemState(ItemState newItemState) {
        if (target != null || newItemState == null) {
            throw new IllegalStateException();
        }

        if ((denotesNode() && newItemState.isNode()) || (!denotesNode() && !newItemState.isNode())) {
            target = new WeakReference(newItemState);
        } else {
            throw new IllegalArgumentException();
        }
    }

    //-----------------------------------------------------< HierarchyEntry >---
    /**
     * @inheritDoc
     * @see HierarchyEntry#getQName()
     */
    public QName getQName() {
        return name;
    }

    /**
     * @inheritDoc
     * @see HierarchyEntry#getPath()
     */
    public Path getPath() throws RepositoryException {
        // shortcut for root state
        if (parent == null) {
            return Path.ROOT;
        }

        // build path otherwise
        try {
            Path.PathBuilder builder = new Path.PathBuilder();
            buildPath(builder, this);
            return builder.getPath();
        } catch (MalformedPathException e) {
            String msg = "Failed to build path of " + this;
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * Adds the path element of an item id to the path currently being built.
     * On exit, <code>builder</code> contains the path of <code>state</code>.
     *
     * @param builder builder currently being used
     * @param hEntry HierarchyEntry of the state the path should be built for.
     */
    private void buildPath(Path.PathBuilder builder, HierarchyEntry hEntry) {
        NodeEntry parentEntry = hEntry.getParent();
        // shortcut for root state
        if (parentEntry == null) {
            builder.addRoot();
            return;
        }

        // recursively build path of parent
        buildPath(builder, parentEntry);

        QName name = hEntry.getQName();
        if (hEntry.denotesNode()) {
            int index = ((NodeEntry) hEntry).getIndex();
            // add to path
            if (index == Path.INDEX_DEFAULT) {
                builder.addLast(name);
            } else {
                builder.addLast(name, index);
            }
        } else {
            // property-state: add to path
            builder.addLast(name);
        }
    }

    /**
     * @inheritDoc
     * @see HierarchyEntry#getParent()
     */
    public NodeEntry getParent() {
        return parent;
    }

    /**
     * @inheritDoc
     * @see HierarchyEntry#getStatus()
     */
    public int getStatus() {
        ItemState state = internalGetItemState();
        if (state == null) {
            return Status._UNDEFINED_;
        } else {
            return state.getStatus();
        }
    }

    /**
     * @inheritDoc
     * @see HierarchyEntry#isAvailable()
     */
    public boolean isAvailable() {
        ItemState state = null;
        if (target != null) {
            state = (ItemState) target.get();
        }
        return state != null;
    }

    /**
     * {@inheritDoc}<br>
     * @see HierarchyEntry#getItemState()
     */
    public ItemState getItemState() throws NoSuchItemStateException, ItemStateException {
        ItemState state = resolve();
        return state;
    }

    /**
     * {@inheritDoc}<br>
     * @see HierarchyEntry#invalidate(boolean)
     */
    public void invalidate(boolean recursive) {
        ItemState state = internalGetItemState();
        if (state != null) {
            // session-state TODO: only invalidate if existing?
            if (state.getStatus() == Status.EXISTING) {
                state.setStatus(Status.INVALIDATED);
            }
        }
    }

    /**
     * {@inheritDoc}
     * @see HierarchyEntry#revert()
     */
    public void revert() throws ItemStateException {
        ItemState state = internalGetItemState();
        if (state == null) {
            // nothing to do
            return;
        }

        int oldStatus = state.getStatus();
        switch (oldStatus) {
            case Status.EXISTING_MODIFIED:
            case Status.STALE_MODIFIED:
                // revert state from overlayed
                state.merge(state.getWorkspaceState(), false);
                state.setStatus(Status.EXISTING);
                break;
            case Status.EXISTING_REMOVED:
                // revert state from overlayed
                state.merge(state.getWorkspaceState(), false);
                state.setStatus(Status.EXISTING);
                if (!denotesNode()) {
                    parent.revertPropertyRemoval((PropertyEntry) this);
                }
                break;
            case Status.NEW:
                // reverting a NEW state is equivalent to its removal.
                remove();
                break;
            case Status.STALE_DESTROYED:
                // overlayed does not exist any more -> remove it
                remove();
                break;
            default:
                // Cannot revert EXISTING, REMOVED, INVALIDATED, MODIFIED states.
                // State was implicitely reverted
                log.debug("State with status " + oldStatus + " cannot be reverted.");
        }
    }

     /**
     * {@inheritDoc}
     * @see HierarchyEntry#reload(boolean, boolean)
     */
    public void reload(boolean keepChanges, boolean recursive) {
        ItemState state = internalGetItemState();
        if (state == null) {
            // nothing to do. entry will be validated upon resolution.
            return;
        }
        /*
        if keepChanges is true only existing or invalidated states must be
        updated. otherwise the state gets updated and might be marked 'Stale'
        if transient changes are present and the workspace-state is modified.
        */
        // TODO: check again if 'reconnect' is not possible for transiently-modified state
        if (!keepChanges || state.getStatus() == Status.EXISTING
            || state.getStatus() == Status.INVALIDATED) {
            // reload the workspace state from the persistent layer
            try {
                state.reconnect(keepChanges);
            } catch (NoSuchItemStateException e) {
                // remove hierarchyEntry (including all children and set
                // state-status to REMOVED (or STALE_DESTROYED)
                remove();
            } catch (ItemStateException e) {
                // TODO: rather throw? remove from parent?
                log.warn("Exception while reloading property state: " + e);
                log.debug("Stacktrace: ", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     * @see HierarchyEntry#transientRemove()
     */
    public void transientRemove() throws ItemStateException {
        ItemState state = internalGetItemState();
        if (state == null) {
            // nothing to do -> correct status must be set upon resolution.
            return;
        }

        state.checkIsSessionState();
        // if during recursive removal an invalidated entry is found, reload
        // it in order to determine the current status.
        if (state.getStatus() == Status.INVALIDATED) {
            reload(false, false);
            // check if upon reload the item has been removed -> nothing to do
            if (Status.isTerminal(state.getStatus())) {
                return;
            }
        }

        switch (state.getStatus()) {
            case Status.NEW:
                remove();
                break;
            case Status.EXISTING:
            case Status.EXISTING_MODIFIED:
                state.setStatus(Status.EXISTING_REMOVED);
                // NOTE: parent does not need to be informed. an transiently
                // removed propertyEntry is automatically moved to the 'attic'
                // if a conflict with a new entry occurs.
                break;
            default:
                throw new ItemStateException("Cannot transiently remove an ItemState with status " + Status.getName(state.getStatus()));
        }
    }

    /**
     * {@inheritDoc}
     * @see HierarchyEntry#collectStates(ChangeLog, boolean)
     */
    public void collectStates(ChangeLog changeLog, boolean throwOnStale) throws StaleItemStateException {
        ItemState state = internalGetItemState();
        if (state == null) {
            // nothing to do
            return;
        }

        if (throwOnStale && Status.isStale(state.getStatus())) {
            String msg = "Cannot save changes: " + state + " has been modified externally.";
            log.debug(msg);
            throw new StaleItemStateException(msg);
        }
        // only interested in transient modifications or stale-modified states
        switch (state.getStatus()) {
            case Status.NEW:
                changeLog.added(state);
                break;
            case Status.EXISTING_MODIFIED:
            case Status.STALE_MODIFIED:
                changeLog.modified(state);
                break;
            case Status.EXISTING_REMOVED:
                changeLog.deleted(state);
                break;
            default:
                log.debug("Collecting states: Ignored ItemState with status " + Status.getName(state.getStatus()));
        }
    }
}
