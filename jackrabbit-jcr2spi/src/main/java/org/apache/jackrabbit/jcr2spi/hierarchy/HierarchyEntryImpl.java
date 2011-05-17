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

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.ItemStateFactory;
import org.apache.jackrabbit.jcr2spi.state.Status;
import org.apache.jackrabbit.jcr2spi.state.TransientItemStateFactory;
import org.apache.jackrabbit.jcr2spi.state.ItemState.MergeResult;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.ItemInfoCache;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>HierarchyEntryImpl</code> implements base functionality for child node
 * and property references.
 */
abstract class HierarchyEntryImpl implements HierarchyEntry {

    private static Logger log = LoggerFactory.getLogger(HierarchyEntryImpl.class);

    /**
     * The required generation of this entry. This is used by the
     * {@link ItemInfoCache} to determine whether an item info in the cache is
     * up to date or not. That is whether the generation of the item info in the
     * cache is the same or more recent as the required generation of this entry.
     */
    private long generation;

    /**
     * Cached soft reference to the target ItemState.
     */
    private Reference<ItemState> target;

    /**
     * The name of the target item state.
     */
    protected Name name;

    /**
     * Hard reference to the parent <code>NodeEntry</code>.
     */
    protected NodeEntryImpl parent;

    /**
     * The item state factory to create the item state.
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
    HierarchyEntryImpl(NodeEntryImpl parent, Name name, EntryFactory factory) {
        this.parent = parent;
        this.name = name;
        this.factory = factory;
    }

    /**
     * Shortcut for {@link EntryFactory#getItemStateFactory()}
     * @return
     */
    protected TransientItemStateFactory getItemStateFactory() {
        return factory.getItemStateFactory();
    }

    /**
     * Shortcut for {@link EntryFactory#getPathFactory()}
     * @return
     */
    protected PathFactory getPathFactory() {
        return factory.getPathFactory();
    }

    /**
     * Shortcut for {@link EntryFactory#getIdFactory()}
     * @return
     */
    protected IdFactory getIdFactory() {
        return factory.getIdFactory();
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
     * @throws ItemNotFoundException if the referenced <code>ItemState</code>
     * does not exist.
     * @throws RepositoryException if an error occurs.
     */
    ItemState resolve() throws ItemNotFoundException, RepositoryException {
        // check if already resolved
        ItemState state = internalGetItemState();
        // not yet resolved. retrieve and keep soft reference to state
        if (state == null) {
            try {
                state = doResolve();
                // set the item state unless 'setItemState' has already been
                // called by the ItemStateFactory (recall internalGetItemState)
                if (internalGetItemState() == null) {
                    setItemState(state);
                }
            } catch (ItemNotFoundException e) {
                remove();
                throw e;
            }
        } else if (state.getStatus() == Status.INVALIDATED) {
            // completely reload this entry, but don't reload recursively
            reload(false);
        }
        return state;
    }

    /**
     * Resolves this <code>HierarchyEntryImpl</code> and returns the target
     * <code>ItemState</code> of this reference.
     *
     * @return the <code>ItemState</code> where this reference points to.
     * @throws ItemNotFoundException if the referenced <code>ItemState</code>
     * does not exist.
     * @throws RepositoryException if another error occurs.
     */
    abstract ItemState doResolve() throws ItemNotFoundException, RepositoryException;

    /**
     * Build the Path of this entry
     *
     * @param workspacePath
     * @return
     * @throws RepositoryException
     */
    abstract Path buildPath(boolean workspacePath) throws RepositoryException;

    /**
     * @return the item state or <code>null</code> if the entry isn't resolved.
     */
    ItemState internalGetItemState() {
        ItemState state = null;
        if (target != null) {
            state = target.get();
        }
        return state;
    }

    protected EntryFactory.InvalidationStrategy getInvalidationStrategy() {
        return factory.getInvalidationStrategy();
    }

    /**
     * Invalidates the underlying {@link ItemState}. If <code>recursive</code> is
     * true also invalidates the underlying item states of all child entries.
     * @param recursive
     */
    protected void invalidateInternal(boolean recursive) {
        ItemState state = internalGetItemState();
        if (state == null) {
            log.debug("Skip invalidation for unresolved HierarchyEntry " + name);
        } else {
            state.invalidate();
        }
    }

    //-----------------------------------------------------< HierarchyEntry >---
    /**
     * @see HierarchyEntry#getName()
     */
    public Name getName() {
        return name;
    }

    /**
     * @see HierarchyEntry#getPath()
     */
    public Path getPath() throws RepositoryException {
        return buildPath(false);
    }

    /**
     * @see HierarchyEntry#getWorkspacePath()
     */
    public Path getWorkspacePath() throws RepositoryException {
        return buildPath(true);
    }

    /**
     * @see HierarchyEntry#getParent()
     */
    public NodeEntry getParent() {
        return parent;
    }

    /**
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
     * @see HierarchyEntry#isAvailable()
     */
    public boolean isAvailable() {
        return internalGetItemState() != null;
    }

    /**
     * {@inheritDoc}<br>
     * @see HierarchyEntry#getItemState()
     */
    public ItemState getItemState() throws ItemNotFoundException, RepositoryException {
        ItemState state = resolve();
        return state;
    }

    /**
     * {@inheritDoc}<br>
     * @see HierarchyEntry#setItemState(ItemState)
     */
    public synchronized void setItemState(ItemState state) {
        ItemState currentState = internalGetItemState();
        if (state == null || state == currentState || denotesNode() != state.isNode()) {
            throw new IllegalArgumentException();
        }
        if (currentState == null) {
            // not connected yet to an item state. either a new entry or
            // an unresolved hierarchy entry.
            target = new SoftReference<ItemState>(state);
        } else {
            // was already resolved before -> merge the existing state
            // with the passed state.
            int currentStatus = currentState.getStatus();
            boolean keepChanges = Status.isTransient(currentStatus) || Status.isStale(currentStatus);
            MergeResult mergeResult = currentState.merge(state, keepChanges);
            if (currentStatus == Status.INVALIDATED) {
                currentState.setStatus(Status.EXISTING);
            } else if (mergeResult.modified()) {
                currentState.setStatus(Status.MODIFIED);
            } // else: not modified. just leave status as it is.
            mergeResult.dispose();
        }
    }

    /**
     * {@inheritDoc}<br>
     * @see HierarchyEntry#invalidate(boolean)
     */
    public void invalidate(boolean recursive) {
        getInvalidationStrategy().invalidate(this, recursive);
    }

    public void calculateStatus() {
        getInvalidationStrategy().applyPending(this);
    }

    /**
     * {@inheritDoc}
     * @see HierarchyEntry#revert()
     */
    public void revert() throws RepositoryException {
        ItemState state = internalGetItemState();
        if (state == null) {
            // nothing to do
            return;
        }

        int oldStatus = state.getStatus();
        switch (oldStatus) {
            case Status.EXISTING_MODIFIED:
            case Status.STALE_MODIFIED:
                // revert state modifications
                state.revert();
                state.setStatus(Status.EXISTING);
                break;
            case Status.EXISTING_REMOVED:
                // revert state modifications
                state.revert();
                state.setStatus(Status.EXISTING);
                break;
            case Status.NEW:
                // reverting a NEW state is equivalent to its removal.
                // however: no need remove the complete hierarchy as revert is
                // always related to Item#refresh(false) which affects the
                // complete tree (and all add-operations within it) anyway.
                state.setStatus(Status.REMOVED);
                parent.internalRemoveChildEntry(this);
                break;
            case Status.STALE_DESTROYED:
                // state does not exist any more -> reverting of pending
                // transient changes (that lead to the stale status) can be
                // omitted and the entry is complete removed instead.
                remove();
                break;
            default:
                // Cannot revert EXISTING, REMOVED, INVALIDATED, MODIFIED states.
                // State was implicitly reverted or external modifications
                // reverted the modification.
                log.debug("State with status " + oldStatus + " cannot be reverted.");
        }
    }

    /**
     * {@inheritDoc}
     * @see HierarchyEntry#reload(boolean)
     */
    public void reload(boolean recursive) {
        int status = getStatus();
        if (status == Status._UNDEFINED_) {
            // unresolved: entry will be loaded and validated upon resolution.
            return;
        }
        if (Status.isTransient(status) || Status.isStale(status) || Status.isTerminal(status)) {
            // transient || stale: avoid reloading
            // new || terminal: cannot be reloaded from persistent layer anyway.
            log.debug("Skip reload for item with status " + Status.getName(status) + ".");
            return;
        }
        /**
         * Retrieved a fresh ItemState from the persistent layer. Which will
         * then be merged into the current state.
         */
        try {
            ItemStateFactory isf = getItemStateFactory();
            if (denotesNode()) {
                NodeEntry ne = (NodeEntry) this;
                isf.createNodeState(ne.getWorkspaceId(), ne);
            } else {
                PropertyEntry pe = (PropertyEntry) this;
                isf.createPropertyState(pe.getWorkspaceId(), pe);
            }
        } catch (ItemNotFoundException e) {
            // remove hierarchyEntry including all children
            log.debug("Item '" + getName() + "' cannot be found on the persistent layer -> remove.");
            remove();
        } catch (RepositoryException e) {
            // TODO: rather throw?
            log.error("Exception while reloading item: " + e);
        }
    }

    /**
     * {@inheritDoc}
     * @see HierarchyEntry#transientRemove()
     */
    public void transientRemove() throws InvalidItemStateException, RepositoryException {
        ItemState state = internalGetItemState();
        if (state == null) {
            // nothing to do -> correct status must be set upon resolution.
            return;
        }
        // if during recursive removal an invalidated entry is found, reload
        // it in order to determine the current status.
        if (state.getStatus() == Status.INVALIDATED) {
            reload(false);
        }

        switch (state.getStatus()) {
            case Status.NEW:
                state.setStatus(Status.REMOVED);
                parent.internalRemoveChildEntry(this);
                break;
            case Status.EXISTING:
            case Status.EXISTING_MODIFIED:
                state.setStatus(Status.EXISTING_REMOVED);
                // NOTE: parent does not need to be informed. an transiently
                // removed propertyEntry is automatically moved to the 'attic'
                // if a conflict with a new entry occurs.
                break;
            case Status.REMOVED:
            case Status.STALE_DESTROYED:
                throw new InvalidItemStateException("Item has already been removed by someone else. Status = " + Status.getName(state.getStatus()));
            default:
                throw new RepositoryException("Cannot transiently remove an ItemState with status " + Status.getName(state.getStatus()));
        }
    }

    /**
     * @see HierarchyEntry#remove()
     */
    public void remove() {
        internalRemove(false);
    }

    public long getGeneration() {
        calculateStatus();
        return generation;
    }

    //--------------------------------------------------------------------------

    /**
     * @param staleParent
     */
    void internalRemove(boolean staleParent) {
        ItemState state = internalGetItemState();
        int status = getStatus();
        if (state != null) {
            if (status == Status.EXISTING_MODIFIED) {
                state.setStatus(Status.STALE_DESTROYED);
            } else if (status == Status.NEW && staleParent) {
                // keep status NEW
            } else {
                state.setStatus(Status.REMOVED);
                if (!staleParent) {
                    parent.internalRemoveChildEntry(this);
                }
            }
        } else {
            // unresolved
            if (!staleParent && parent != null) {
                parent.internalRemoveChildEntry(this);
            }
        }
    }

    // ----------------------------------------------< InvalidationStrategy >---
    /**
     * An implementation of <code>InvalidationStrategy</code> which lazily invalidates
     * the underlying {@link ItemState}s.
     */
    static class LazyInvalidation implements EntryFactory.InvalidationStrategy {

        /**
         * Marker for entries with a pending recursive invalidation.
         */
        private static long INVALIDATION_PENDING = -1;

        /**
         * Number of the current generation
         */
        private long currentGeneration;

        /**
         * Increment for obtaining the next generation from the current generation.
         */
        private int nextGeneration;

        /**
         * A recursive invalidation is being processed if <code>true</code>.
         * This flag is for preventing re-entrance.
         */
        private boolean invalidating;

        /**
         * Records a pending recursive {@link ItemState#invalidate() invalidation} for
         * <code>entry</code> if <code>recursive</code> is <code>true</code>. Otherwise
         * invalidates the entry right away.
         * {@inheritDoc}
         */
        public void invalidate(HierarchyEntry entry, boolean recursive) {
            HierarchyEntryImpl he = (HierarchyEntryImpl) entry;
            if (recursive) {
                he.generation = INVALIDATION_PENDING;
                if (!invalidating) {
                    nextGeneration = 1;
                }
            } else {
                if (!invalidating) {
                    nextGeneration = 1;
                }
                he.invalidateInternal(false);
            }
        }

        /**
         * Checks whether <code>entry</code> itself has a invalidation pending.
         * If so, the <code>entry</code> is invalidated. Otherwise check
         * whether an invalidation occurred after the entry has last been
         * invalidated. If so, search the path to the root for an originator of
         * the pending invalidation.
         * If such an originator is found, invalidate each entry on the path.
         * Otherwise this method does nothing.
         * {@inheritDoc}
         */
        public void applyPending(HierarchyEntry entry) {
            if (!invalidating) {
                invalidating = true;
                currentGeneration += nextGeneration;
                nextGeneration = 0;
                try {
                    HierarchyEntryImpl he = (HierarchyEntryImpl) entry;
                    if (he.generation == INVALIDATION_PENDING) {
                        he.invalidateInternal(true);
                        he.generation = currentGeneration;
                    } else if (he.generation < currentGeneration) {
                        resolvePendingInvalidation(he);
                    }
                } finally {
                    invalidating = false;
                }
            }
        }

        /**
         * Search the path to the root for an originator of a pending invalidation of
         * this <code>entry</code>. If such an originator is found, invalidate each
         * entry on the path. Otherwise do nothing.
         *
         * @param entry
         */
        private void resolvePendingInvalidation(HierarchyEntryImpl entry) {
            if (entry != null) {

                // First recursively travel up to the first parent node
                // which has invalidation pending or to the root node if
                // no such node exists.
                if (entry.generation != INVALIDATION_PENDING) {
                    resolvePendingInvalidation(entry.parent);
                }

                // Then travel the path backwards invalidating as required
                if (entry.generation == INVALIDATION_PENDING) {
                    entry.invalidateInternal(true);
                }
                entry.generation = currentGeneration;
            }
        }
    }

}
