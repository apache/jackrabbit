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

import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.observation.EventStateCollectionFactory;
import org.apache.jackrabbit.name.QName;

import javax.jcr.ReferentialIntegrityException;
import java.util.Iterator;

/**
 * Local <code>ItemStateManager</code> that isolates changes to
 * persistent states from other clients.
 */
public class LocalItemStateManager
        implements UpdatableItemStateManager, ItemStateListener {

    /**
     * cache of weak references to ItemState objects issued by this
     * ItemStateManager
     */
    private final ItemStateReferenceCache cache;

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
     * Creates a new <code>LocalItemStateManager</code> instance.
     * @param sharedStateMgr shared state manager
     * @param factory event state collection factory
     */
    public LocalItemStateManager(SharedItemStateManager sharedStateMgr,
                                 EventStateCollectionFactory factory) {
        cache = new ItemStateReferenceCache();
        this.sharedStateMgr = sharedStateMgr;
        this.factory = factory;
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

        // register as listener
        state.addListener(this);
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

        // register as listener
        state.addListener(this);
        return state;
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
        synchronized (cache) {
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
    public NodeReferences getNodeReferences(NodeReferencesId id)
            throws NoSuchItemStateException, ItemStateException {

        // check change log
        NodeReferences refs = changeLog.get(id);
        if (refs != null) {
            return refs;
        }
        return sharedStateMgr.getNodeReferences(id);
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasNodeReferences(NodeReferencesId id) {
        // check change log
        if (changeLog.get(id) != null) {
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
    public NodeState createNew(NodeId id, QName nodeTypeName,
                               NodeId parentId)
            throws IllegalStateException {
        if (!editMode) {
            throw new IllegalStateException("Not in edit mode");
        }

        NodeState state = new NodeState(id, nodeTypeName, parentId,
                ItemState.STATUS_NEW, false);
        changeLog.added(state);
        return state;
    }

    /**
     * {@inheritDoc}
     */
    public PropertyState createNew(QName propName, NodeId parentId)
            throws IllegalStateException {
        if (!editMode) {
            throw new IllegalStateException("Not in edit mode");
        }
        PropertyState state = new PropertyState(
                new PropertyId(parentId, propName), ItemState.STATUS_NEW, false);
        changeLog.added(state);
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
        update(changeLog);
        changeLog.reset();

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
        // this LocalItemStateManager instance is no longer needed;
        // cached item states can now be safely discarded
        Iterator iter = cache.values().iterator();
        while (iter.hasNext()) {
            ItemState state = (ItemState) iter.next();
            // we're no longer interested in status changes of this item state
            state.removeListener(this);
            // discard item state; any remaining listeners will be informed
            // about this status change
            state.discard();
            // let the item state know that it has been disposed
            state.onDisposed();
        }
        // clear cache
        cache.evictAll();
    }

    //----------------------------------------------------< ItemStateListener >
    /**
     * {@inheritDoc}
     */
    public void stateCreated(ItemState created) {
    }

    /**
     * {@inheritDoc}
     */
    public void stateModified(ItemState modified) {
    }

    /**
     * {@inheritDoc}
     */
    public void stateDestroyed(ItemState destroyed) {
        destroyed.removeListener(this);

        cache.evict(destroyed.getId());
    }

    /**
     * {@inheritDoc}
     */
    public void stateDiscarded(ItemState discarded) {
        discarded.removeListener(this);

        cache.evict(discarded.getId());
    }
}
