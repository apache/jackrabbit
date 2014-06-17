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

import javax.jcr.InvalidItemStateException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.NodeIdFactory;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.observation.EventStateCollectionFactory;
import org.apache.jackrabbit.spi.Name;

/**
 * Local <code>ItemStateManager</code> that isolates changes to
 * persistent states from other clients.
 */
public class LocalItemStateManager
        implements UpdatableItemStateManager, NodeStateListener {

    /**
     * cache of weak references to ItemState objects issued by this
     * ItemStateManager
     */
    private final ItemStateCache cache;

    /**
     * Shared item state manager
     */
    protected final SharedItemStateManager sharedStateMgr;

    /**
     * Event state collection factory.
     */
    protected final EventStateCollectionFactory factory;

    /**
     * Flag indicating whether this item state manager is in edit mode
     */
    private boolean editMode;

    /**
     * Change log
     */
    private final ChangeLog changeLog = new ChangeLog();

    /**
     * State change dispatcher.
     */
    private final transient StateChangeDispatcher dispatcher = new StateChangeDispatcher();

    /**
     * Creates a new <code>LocalItemStateManager</code> instance.
     * @param sharedStateMgr shared state manager
     * @param factory event state collection factory
     */
    protected LocalItemStateManager(SharedItemStateManager sharedStateMgr,
                                 EventStateCollectionFactory factory, ItemStateCacheFactory cacheFactory) {
        cache = new ItemStateReferenceCache(cacheFactory);
        this.sharedStateMgr = sharedStateMgr;
        this.factory = factory;
    }

    /**
     * Creates a new {@code LocalItemStateManager} instance and registers it as an {@link ItemStateListener}
     * with the given {@link SharedItemStateManager}.
     *
     * @param sharedStateMgr the {@link SharedItemStateManager}
     * @param factory the {@link EventStateCollectionFactory}
     * @param cacheFactory the {@link ItemStateCacheFactory}
     * @return a new {@code LocalItemStateManager} instance
     */
    public static LocalItemStateManager createInstance(SharedItemStateManager sharedStateMgr,
            EventStateCollectionFactory factory, ItemStateCacheFactory cacheFactory) {
        LocalItemStateManager mgr = new LocalItemStateManager(sharedStateMgr, factory, cacheFactory);
        sharedStateMgr.addListener(mgr);
        return mgr;
    }

    /**
     * Retrieve a node state from the parent shared state manager and
     * wraps it into a intermediate object that helps us handle local
     * modifications.
     *
     * @param id node id
     * @return node state
     * @throws NoSuchItemStateException
     * @throws ItemStateException
     */
    protected NodeState getNodeState(NodeId id)
            throws NoSuchItemStateException, ItemStateException {

        // load from parent manager and wrap
        NodeState state = (NodeState) sharedStateMgr.getItemState(id);
        state = new NodeState(state, state.getStatus(), false);

        // put it in cache
        cache.cache(state);

        // set parent container
        state.setContainer(this);
        return state;
    }

    /**
     * Retrieve a property state from the parent shared state manager and
     * wraps it into a intermediate object that helps us handle local
     * modifications.
     *
     * @param id property id
     * @return property state
     * @throws NoSuchItemStateException
     * @throws ItemStateException
     */
    protected PropertyState getPropertyState(PropertyId id)
            throws NoSuchItemStateException, ItemStateException {

        // load from parent manager and wrap
        PropertyState state = (PropertyState) sharedStateMgr.getItemState(id);
        state = new PropertyState(state, state.getStatus(), false);

        // put it in cache
        cache.cache(state);

        // set parent container
        state.setContainer(this);
        return state;
    }

    /**
     * Returns the change log that contains the current changes in this local
     * item state manager.
     *
     * @return the change log with the current changes.
     */
    protected ChangeLog getChanges() {
        return changeLog;
    }

    //-----------------------------------------------------< ItemStateManager >
    /**
     * {@inheritDoc}
     */
    public ItemState getItemState(ItemId id)
            throws NoSuchItemStateException, ItemStateException {

        // check change log
        ItemState state = changeLog.get(id);
        if (state != null) {
            return state;
        }

        // check cache. synchronized to ensure an entry is not created twice.
        synchronized (this) {
            state = cache.retrieve(id);
            if (state == null) {
                // regular behaviour
                if (id.denotesNode()) {
                    state = getNodeState((NodeId) id);
                } else {
                    state = getPropertyState((PropertyId) id);
                }
            }
            return state;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasItemState(ItemId id) {

        // check items in change log
        try {
            ItemState state = changeLog.get(id);
            if (state != null) {
                return true;
            }
        } catch (NoSuchItemStateException e) {
            return false;
        }

        // check cache
        if (cache.isCached(id)) {
            return true;
        }

        // regular behaviour
        return sharedStateMgr.hasItemState(id);
    }

    /**
     * {@inheritDoc}
     */
    public NodeReferences getNodeReferences(NodeId id)
            throws NoSuchItemStateException, ItemStateException {

        // check change log
        NodeReferences refs = changeLog.getReferencesTo(id);
        if (refs != null) {
            return refs;
        }
        return sharedStateMgr.getNodeReferences(id);
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasNodeReferences(NodeId id) {
        // check change log
        if (changeLog.getReferencesTo(id) != null) {
            return true;
        }
        return sharedStateMgr.hasNodeReferences(id);
    }


    //--------------------------------------------< UpdatableItemStateManager >
    /**
     * {@inheritDoc}
     */
    public synchronized void edit() throws IllegalStateException {
        if (editMode) {
            throw new IllegalStateException("Already in edit mode");
        }
        changeLog.reset();

        editMode = true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean inEditMode() {
        return editMode;
    }

    /**
     * {@inheritDoc}
     */
    public NodeState createNew(
            NodeId id, Name nodeTypeName, NodeId parentId)
            throws RepositoryException {
        if (!editMode) {
            throw new RepositoryException("Not in edit mode");
        }

        boolean nonRandomId = true;
        if (id == null) {
            id = getNodeIdFactory().newNodeId();
            nonRandomId = false;
        }

        NodeState state = new NodeState(
                id, nodeTypeName, parentId, ItemState.STATUS_NEW, false);
        changeLog.added(state);
        state.setContainer(this);

        if (nonRandomId && !changeLog.deleted(id)
                && sharedStateMgr.hasItemState(id)) {
            throw new InvalidItemStateException(
                    "Node " + id + " already exists");
        }

        return state;
    }

    /**
     * Returns the local node state below the given transient one. If given
     * a fresh new node state, then a new local state is created and added
     * to the change log.
     *
     * @param transientState transient state
     * @return local node state
     * @throws RepositoryException if the local state could not be created
     */
    public NodeState getOrCreateLocalState(NodeState transientState)
            throws RepositoryException {
        NodeState localState = (NodeState) transientState.getOverlayedState();
        if (localState == null) {
            // The transient node state is new, create a new local state
            localState = new NodeState(
                    transientState.getNodeId(),
                    transientState.getNodeTypeName(),
                    transientState.getParentId(),
                    ItemState.STATUS_NEW,
                    false);
            changeLog.added(localState);
            localState.setContainer(this);
            try {
                transientState.connect(localState);
            } catch (ItemStateException e) {
                // should never happen
                throw new RepositoryException(e);
            }
        }
        return localState;
    }

    /**
     * {@inheritDoc}
     */
    public PropertyState createNew(Name propName, NodeId parentId)
            throws IllegalStateException {
        if (!editMode) {
            throw new IllegalStateException("Not in edit mode");
        }
        PropertyState state = new PropertyState(
                new PropertyId(parentId, propName), ItemState.STATUS_NEW, false);
        changeLog.added(state);
        state.setContainer(this);
        return state;
    }

    /**
     * {@inheritDoc}
     */
    public void store(ItemState state) throws IllegalStateException {
        if (!editMode) {
            throw new IllegalStateException("Not in edit mode");
        }
        changeLog.modified(state);
    }

    /**
     * {@inheritDoc}
     */
    public void destroy(ItemState state) throws IllegalStateException {
        assert state != null;
        if (!editMode) {
            throw new IllegalStateException("Not in edit mode");
        }
        changeLog.deleted(state);
    }

    /**
     * {@inheritDoc}
     */
    public void cancel() throws IllegalStateException {
        if (!editMode) {
            throw new IllegalStateException("Not in edit mode");
        }
        changeLog.undo(sharedStateMgr);

        editMode = false;
    }

    /**
     * {@inheritDoc}
     */
    public void update()
            throws ReferentialIntegrityException, StaleItemStateException,
            ItemStateException, IllegalStateException {
        if (!editMode) {
            throw new IllegalStateException("Not in edit mode");
        }
        // JCR-1813: Only execute the update when there are some changes
        if (changeLog.hasUpdates()) {
            update(changeLog);
            changeLog.reset();
        }

        editMode = false;
    }

    /**
     * End an update operation. Fetch the states and references from
     * the parent (shared) item manager, reconnect them to the items
     * collected in our (local) change log and overwrite the shared
     * items with our copies.
     *
     * @param changeLog change log containing local states and references
     * @throws ReferentialIntegrityException if a new or modified REFERENCE
     *                                       property refers to a non-existent
     *                                       target or if a removed node is still
     *                                       being referenced
     * @throws StaleItemStateException       if at least one of the affected item
     *                                       states has become stale in the meantime
     * @throws ItemStateException            if an error occurs
     */
    protected void update(ChangeLog changeLog)
            throws ReferentialIntegrityException, StaleItemStateException, ItemStateException {

        sharedStateMgr.update(changeLog, factory);
        changeLog.persisted();
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        sharedStateMgr.removeListener(this);

        // this LocalItemStateManager instance is no longer needed;
        // cached item states can now be safely discarded
        ItemState[] states = cache.retrieveAll();
        for (int i = 0; i < states.length; i++) {
            ItemState state = states[i];
            if (state != null) {
                dispatcher.notifyStateDiscarded(state);
                // let the item state know that it has been disposed
                state.onDisposed();
            }
        }

        // clear cache
        cache.evictAll();
        cache.dispose();
    }

    /**
     * Add an <code>ItemStateListener</code>
     * @param listener the new listener to be informed on modifications
     */
    public void addListener(ItemStateListener listener) {
        dispatcher.addListener(listener);
    }

    /**
     * Remove an <code>ItemStateListener</code>
     * @param listener an existing listener
     */
    public void removeListener(ItemStateListener listener) {
        dispatcher.removeListener(listener);
    }

    //----------------------------------------------------< ItemStateListener >

    /**
     * {@inheritDoc}
     * <p>
     * Notification handler gets called for both local states that this state manager
     * has created, as well as states that were created by the shared state manager
     * we're listening to.
     */
    public void stateCreated(ItemState created) {
        ItemState local = null;
        if (created.getContainer() != this) {
            // shared state was created
            try {
                local = changeLog.get(created.getId());
                if (local != null) {
                    if (local.isNode() && local.getOverlayedState() != created) {
                        // mid-air collision of concurrent node state creation
                        // with same id (JCR-2272)
                        if (local.getStatus() == ItemState.STATUS_NEW) {
                            local.setStatus(ItemState.STATUS_UNDEFINED); // we need a state that is != NEW
                        }
                    } else {
                        if (local.getOverlayedState() == created) {
                            // underlying state has been permanently created
                            local.pull();
                            local.setStatus(ItemState.STATUS_EXISTING);
                            cache.cache(local);
                        }
                    }
                }
            } catch (NoSuchItemStateException e) {
                /* ignore */
            }
        } else {
            // local state was created
            local = created;
            // just ensure that the newly created state is still cached. it can
            // happen during a restore operation that a state with the same id
            // is deleted and created (JCR-1197)
            if (!cache.isCached(created.getId())) {
                cache.cache(local);
            }
        }
        dispatcher.notifyStateCreated(created);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Notification handler gets called for both local states that this state manager
     * has created, as well as states that were created by the shared state manager
     * we're listening to.
     */
    public void stateModified(ItemState modified) {
        ItemState local;
        if (modified.getContainer() != this) {
            // shared state was modified
            local = cache.retrieve(modified.getId());
            if (local != null && local.isConnected()) {
                // this instance represents existing state, update it
                local.pull();
            }
        } else {
            // local state was modified
            local = modified;
        }
        if (local != null) {
            dispatcher.notifyStateModified(local);
        } else if (modified.isNode()) {
            // if the state is not ours (and is not cached) it could have
            // vanished from the weak-ref cache due to a gc. but there could
            // still be some listeners (e.g. CachingHierarchyManager) that want
            // to get notified.
            dispatcher.notifyNodeModified((NodeState) modified);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Notification handler gets called for both local states that this state manager
     * has created, as well as states that were created by the shared state manager
     * we're listening to.
     */
    public void stateDestroyed(ItemState destroyed) {
        ItemState local = null;
        if (destroyed.getContainer() != this) {
            // shared state was destroyed
            local = cache.retrieve(destroyed.getId());
            if (local != null && local.isConnected()) {
                local.setStatus(ItemState.STATUS_EXISTING_REMOVED);
            }
        } else {
            // local state was destroyed
            local = destroyed;
        }
        cache.evict(destroyed.getId());
        if (local != null) {
            dispatcher.notifyStateDestroyed(local);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Notification handler gets called for both local states that this state manager
     * has created, as well as states that were created by the shared state manager
     * we're listening to.
     */
    public void stateDiscarded(ItemState discarded) {
        ItemState local = null;
        if (discarded.getContainer() != this) {
            // shared state was discarded
            local = cache.retrieve(discarded.getId());
            if (local != null && local.isConnected()) {
                local.setStatus(ItemState.STATUS_UNDEFINED);
            }
        } else {
            // local state was discarded
            local = discarded;
        }
        cache.evict(discarded.getId());
        if (local != null) {
            dispatcher.notifyStateDiscarded(local);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Optimization: shared state manager we're listening to does not deliver node state changes, therefore the state
     * concerned must be a local state.
     */
    public void nodeAdded(NodeState state, Name name, int index, NodeId id) {
        dispatcher.notifyNodeAdded(state, name, index, id);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Optimization: shared state manager we're listening to does not deliver node state changes, therefore the state
     * concerned must be a local state.
     */
    public void nodesReplaced(NodeState state) {
        dispatcher.notifyNodesReplaced(state);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Optimization: shared state manager we're listening to does not deliver node state changes, therefore the state
     * concerned must be a local state.
     */
    public void nodeModified(NodeState state) {
        dispatcher.notifyNodeModified(state);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Optimization: shared state manager we're listening to does not deliver node state changes, therefore the state
     * concerned must be a local state.
     */
    public void nodeRemoved(NodeState state, Name name, int index, NodeId id) {
        dispatcher.notifyNodeRemoved(state, name, index, id);
    }

    public NodeIdFactory getNodeIdFactory() {
        return sharedStateMgr.getNodeIdFactory();
    }

}
