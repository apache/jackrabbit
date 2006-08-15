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

import org.apache.jackrabbit.jcr2spi.operation.Operation;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.commons.collections.iterators.IteratorChain;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.ItemExistsException;
import java.util.Iterator;

/**
 * <code>TransientItemStateManager</code> implements a {@link ItemStateManager}
 * and adds more methods that support transient changes (e.g. resurrect deleted
 * state).
 */
public class TransientItemStateManager
        implements ItemStateManager, TransientItemStateFactory, ItemStateLifeCycleListener {

    /**
     * Logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(TransientItemStateManager.class);

    /**
     * The change log which keeps track of changes and maintains hard references
     * to changed item states.
     */
    private final ChangeLog changeLog;

    /**
     *
     */
    private final IdFactory idFactory;

    /**
     * The parent item state manager, which return item states that are then
     * overlayed by transient item states created by this TransientItemStateManager.
     */
    private final ItemStateManager parent;

    /**
     * ItemStateManager view of the states in the attic; lazily instantiated
     * in {@link #getAttic()}
     */
    private AtticItemStateManager attic;

    TransientItemStateManager(IdFactory idFactory, ItemStateManager parent) {
        this.changeLog = new ChangeLog();
        this.idFactory = idFactory;
        this.parent = parent;
    }

    //-----------------------< ItemStateManager >-------------------------------

    /**
     * Return the root node state or <code>null</code> if the root state has
     * not been modified yet.
     *
     * @return
     * @throws ItemStateException
     * @see ItemStateManager#getRootState()
     */
    public NodeState getRootState() throws ItemStateException {
        // TODO
        return null;
    }

    /**
     * Return an item state given its id. Returns <code>null</code>
     * if the item state is neither in the added nor in the modified
     * section. Throws a <code>NoSuchItemStateException</code> if
     * the item state is in the deleted section.
     *
     * @return item state or <code>null</code>
     * @throws NoSuchItemStateException if the item has been deleted
     * @see ItemStateManager#getItemState(ItemId)
     */
    public ItemState getItemState(ItemId id) throws NoSuchItemStateException, ItemStateException {
        // TODO: this is expensive. Improvement: Lookup item, then check its state
        ItemState state = null;
        for (Iterator it = changeLog.addedStates(); it.hasNext(); ) {
            ItemState s = (ItemState) it.next();
            if (s.getId().equals(id)) {
                state = s;
                break;
            }
        }
        if (state == null) {
            for (Iterator it = changeLog.modifiedStates(); it.hasNext(); ) {
                ItemState s = (ItemState) it.next();
                if (s.getId().equals(id)) {
                    state = s;
                    break;
                }
            }
            if (state == null) {
                for (Iterator it = changeLog.deletedStates(); it.hasNext(); ) {
                    ItemState s = (ItemState) it.next();
                    if (s.getId().equals(id)) {
                        throw new NoSuchItemStateException("State has been marked destroyed: " + id);
                    }
                }
            }
        }
        return state;
    }

    /**
     * Return a flag indicating whether a given item state exists.
     *
     * @return <code>true</code> if item state exists within this
     *         log; <code>false</code> otherwise
     * @see ItemStateManager#hasItemState(ItemId)
     */
    public boolean hasItemState(ItemId id) {
        // TODO: too expensive. lookup item and check status
        for (Iterator it = changeLog.addedStates(); it.hasNext(); ) {
            ItemState s = (ItemState) it.next();
            if (s.getId().equals(id)) {
                return true;
            }
        }
        for (Iterator it = changeLog.modifiedStates(); it.hasNext(); ) {
            ItemState s = (ItemState) it.next();
            if (s.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return a node references object given its id. Returns
     * <code>null</code> if the node reference is not in the modified
     * section.
     *
     * @return node references or <code>null</code>
     * @see ItemStateManager#getNodeReferences(NodeId)
     */
    public NodeReferences getNodeReferences(NodeId id) {
        // TODO: improve
        for (Iterator it = changeLog.modifiedRefs(); it.hasNext(); ) {
            NodeReferences refs = (NodeReferences) it.next();
            if (refs.getId().equals(id)) {
                return refs;
            }
        }
        return null;
    }

    /**
     * Returns <code>false</code> if the node reference is not in the modified
     * section.
     *
     * @return false if no references are present in this changelog for the
     * given id.
     * @see ItemStateManager#hasNodeReferences(NodeId)
     */
    public boolean hasNodeReferences(NodeId id) {
        return getNodeReferences(id) != null;
    }

    /**
     * @return the operations that have been recorded until now.
     */
    public Iterator getOperations() {
        return changeLog.getOperations();
    }

    /**
     * Add the given operation to the list of operations to be recorded within
     * this TransientItemStateManager.
     *
     * @param operation
     */
    void addOperation(Operation operation) {
        changeLog.addOperation(operation);
    }

    /**
     * Removes the <code>operation</code> from the list of operations.
     * @param operation the Operation to remove.
     * @return <code>true</code> if the operation was removed.
     */
    boolean removeOperation(Operation operation) {
        return changeLog.removeOperation(operation);
    }

    /**
     * @return the number of entries
     */
    public int getEntriesCount() {
        return changeLog.addedStates.size() + changeLog.modifiedStates.size();
    }

    /**
     * @return <code>true</code> if there are any entries in attic.
     */
    public boolean hasEntriesInAttic() {
        return changeLog.deletedStates.size() > 0;
    }

    /**
     * @return an iterator over all entries
     */
    public Iterator getEntries() {
        IteratorChain it = new IteratorChain();
        it.addIterator(changeLog.modifiedStates());
        it.addIterator(changeLog.addedStates());
        return it;
    }

    /**
     * @return an iterator over all entries in attic
     */
    public Iterator getEntriesInAttic() {
        return changeLog.deletedStates();
    }

    /**
     * TODO: throw ItemExistsException? how to check?
     * Creates a new transient {@link NodeState} that does not overlay any other
     * {@link NodeState}.
     *
     * @param nodeName     the name of the <code>NodeState</code> to create.
     * @param uuid         the uuid of the <code>NodeState</code> to create or
     *                     <code>null</code> if the created <code>NodeState</code>
     *                     cannot be identified by a UUID.
     * @param nodeTypeName name of the node type of the new node state.
     * @param parent       the parent of the new node state.
     * @return a new transient {@link NodeState}.
     */
    public NodeState createNodeState(QName nodeName,
                                     String uuid,
                                     QName nodeTypeName,
                                     NodeState parent) {
        NodeState nodeState = createNewNodeState(nodeName, uuid, parent);
        nodeState.setNodeTypeName(nodeTypeName);
        parent.addChildNodeState(nodeState, uuid);
        changeLog.added(nodeState);
        nodeState.addListener(this);
        return nodeState;
    }

    /**
     * Creates a new transient property state for a given <code>parent</code>
     * node state.
     *
     * @param parent   the node state where to the new property is added.
     * @param propName the name of the property state to create.
     * @return the created property state.
     * @throws ItemExistsException if <code>parent</code> already has a property
     *                             with the given name.
     */
    public PropertyState createPropertyState(NodeState parent, QName propName)
            throws ItemExistsException {
        PropertyState propState = createNewPropertyState(propName, parent);
        parent.addPropertyState(propState);
        changeLog.added(propState);
        propState.addListener(this);
        return propState;
    }

    /**
     * Disposes a single item <code>state</code>. The state is discarded removed
     * from the map of added or modified states and disconnected from the
     * underlying state. This method does not take states into account that are
     * marked as deleted.
     *
     * @param state the item state to dispose.
     */
    public void disposeItemState(ItemState state) {
        state.discard();
        if (changeLog.addedStates.remove(state)) {
            changeLog.modifiedStates.remove(state);
        }
        state.onDisposed();
    }

    /**
     * A state has been deleted. If the state is not a new state
     * (not in the collection of added ones), then remove
     * it from the modified states collection.
     * The state is added to the deleted states collection in any case.
     *
     * @param state state that has been deleted
     */
    public void moveItemStateToAttic(ItemState state) {
        if (changeLog.addedStates.remove(state)) {
            changeLog.modifiedStates.remove(state);
        }
        changeLog.deleted(state);
    }

    /**
     * Disposes a single item <code>state</code> that is marked as deleted. The
     * state is discarded removed from the map of removed states and
     * disconnected from the underlying state.
     *
     * @param state the item state to dispose.
     */
    public void disposeItemStateInAttic(ItemState state) {
        state.discard();
        changeLog.deletedStates.remove(state);
        state.onDisposed();
    }

    /**
     * Disposes all transient item states in the cache and in the attic.
     */
    public void disposeAllItemStates() {
        IteratorChain it = new IteratorChain();
        it.addIterator(changeLog.modifiedStates());
        it.addIterator(changeLog.addedStates());
        it.addIterator(changeLog.deletedStates());
        while (it.hasNext()) {
            ItemState state = (ItemState) it.next();
            state.discard();
            state.onDisposed();
        }
        changeLog.reset();
    }

    /**
     * Return the attic item state provider that holds all items
     * moved into the attic.
     *
     * @return attic
     */
    public ItemStateManager getAttic() {
        if (attic == null) {
            attic = new AtticItemStateManager();
        }
        return attic;
    }

    /**
     * Disposes a collection of {@link org.apache.jackrabbit.jcr2spi.operation.Operation}s.
     *
     * @param operations the operations.
     */
    public void disposeOperations(Iterator operations) {
        while (operations.hasNext()) {
            changeLog.removeOperation((Operation) operations.next());
        }
    }

    /**
     * TODO: remove this method when not used anymore
     * Return an iterator over all added states.
     *
     * @return iterator over all added states.
     */
    public Iterator addedStates() {
        return changeLog.addedStates();
    }

    /**
     * TODO: remove this method when not used anymore
     * Return an iterator over all modified states.
     *
     * @return iterator over all modified states.
     */
    public Iterator modifiedStates() {
        return changeLog.modifiedStates();
    }

    /**
     * TODO: remove this method when not used anymore
     * Return an iterator over all deleted states.
     *
     * @return iterator over all deleted states.
     */
    public Iterator deletedStates() {
        return changeLog.deletedStates();
    }

    //----------------------< TransientItemStateFactory >-----------------------

    /**
     * @inheritDoc
     * @see TransientItemStateFactory#createNewNodeState(QName, String, NodeState)
     */
    public NodeState createNewNodeState(QName name, String uuid, NodeState parent) {
        NodeState nodeState = new NodeState(name, uuid, parent, null,
                ItemState.STATUS_NEW, true, this, idFactory);
        // get a notification when this item state is saved or invalidated
        nodeState.addListener(this);
        changeLog.added(nodeState);
        return nodeState;
    }

    /**
     * @inheritDoc
     * @see TransientItemStateFactory#createNewPropertyState(QName, NodeState)
     */
    public PropertyState createNewPropertyState(QName name, NodeState parent) {
        PropertyState propState = new PropertyState(name, parent,
                ItemState.STATUS_NEW, true, idFactory);
        // get a notification when this item state is saved or invalidated
        propState.addListener(this);
        changeLog.added(propState);
        return propState;
    }

    /**
     * @inheritDoc
     * @see TransientItemStateFactory#createNodeState(NodeId, ItemStateManager)
     */
    public NodeState createNodeState(NodeId nodeId, ItemStateManager ism)
            throws NoSuchItemStateException, ItemStateException {
        // retrieve state to overlay
        NodeState overlayedState = (NodeState) parent.getItemState(nodeId);
        NodeId parentId = overlayedState.getParent().getNodeId();
        NodeState parentState = (NodeState) ism.getItemState(parentId);
        NodeState nodeState = new NodeState(overlayedState, parentState,
                ItemState.STATUS_EXISTING, true, this, idFactory);
        nodeState.addListener(this);
        return nodeState;
    }

    /**
     * @inheritDoc
     * @see TransientItemStateFactory#createNodeState(NodeId, NodeState)
     */
    public NodeState createNodeState(NodeId nodeId, NodeState parentState)
            throws NoSuchItemStateException, ItemStateException {
        // retrieve state to overlay
        NodeState overlayedState = (NodeState) parent.getItemState(nodeId);
        NodeState nodeState = new NodeState(overlayedState, parentState,
                ItemState.STATUS_EXISTING, true, this, idFactory);
        nodeState.addListener(this);
        return nodeState;
    }

    /**
     * @inheritDoc
     * @see TransientItemStateFactory#createPropertyState(PropertyId, ItemStateManager)
     */
    public PropertyState createPropertyState(PropertyId propertyId,
                                             ItemStateManager ism)
            throws NoSuchItemStateException, ItemStateException {
        // retrieve state to overlay
        PropertyState overlayedState = (PropertyState) parent.getItemState(propertyId);
        NodeId parentId = overlayedState.getParent().getNodeId();
        NodeState parentState = (NodeState) ism.getItemState(parentId);
        PropertyState propState = new PropertyState(overlayedState, parentState,
                ItemState.STATUS_EXISTING, true, idFactory);
        propState.addListener(this);
        return propState;
    }

    /**
     * @inheritDoc
     * @see TransientItemStateFactory#createPropertyState(PropertyId, NodeState)
     */
    public PropertyState createPropertyState(PropertyId propertyId,
                                             NodeState parentState)
            throws NoSuchItemStateException, ItemStateException {
        // retrieve state to overlay
        PropertyState overlayedState = (PropertyState) parent.getItemState(propertyId);
        PropertyState propState = new PropertyState(overlayedState, parentState,
                ItemState.STATUS_EXISTING, true, idFactory);
        propState.addListener(this);
        return propState;
    }

    //-----------------------< ItemStateLifeCycleListener >---------------------

    /**
     * @inheritDoc
     * @see ItemStateListener#stateCreated(ItemState)
     */
    public void stateCreated(ItemState created) {
    }

    /**
     * @inheritDoc
     * @see ItemStateListener#stateModified(ItemState)
     */
    public void stateModified(ItemState modified) {
    }

    /**
     * @inheritDoc
     * @see ItemStateListener#stateDestroyed(ItemState)
     */
    public void stateDestroyed(ItemState destroyed) {
        changeLog.deletedStates.remove(destroyed);
    }

    /**
     * @inheritDoc
     * @see ItemStateListener#stateDiscarded(ItemState)
     */
    public void stateDiscarded(ItemState discarded) {
        // TODO: remove from modified (and deleted?) set of change log
    }

    /**
     * @inheritDoc
     * @see ItemStateLifeCycleListener#statusChanged(ItemState, int)
     */
    public void statusChanged(ItemState state, int previousStatus) {
        // TODO: depending on status of state adapt change log
        // e.g. a revert on states will reset the status from
        // 'existing modified' to 'existing'.
        // a state which changes from 'existing' to 'existing modified' will
        // go into the modified set of the change log, etc.
        switch (state.getStatus()) {
            case ItemState.STATUS_EXISTING:
                if (previousStatus == ItemState.STATUS_EXISTING_MODIFIED) {
                    // was modified and is now refreshed
                    changeLog.modifiedStates.remove(state);
                } else if (previousStatus == ItemState.STATUS_EXISTING_REMOVED) {
                    // was removed and is now refreshed
                    changeLog.deletedStates.remove(state);
                } else if (previousStatus == ItemState.STATUS_STALE_MODIFIED) {
                    // was modified and state and is now refreshed
                    changeLog.modifiedStates.remove(state);
                } else if (previousStatus == ItemState.STATUS_NEW) {
                    // was new and has been saved now
                    changeLog.addedStates.remove(state);
                }
                break;
            case ItemState.STATUS_EXISTING_MODIFIED:
                changeLog.modified(state);
                break;
            case ItemState.STATUS_EXISTING_REMOVED:
                // check if modified earlier
                if (previousStatus == ItemState.STATUS_EXISTING_MODIFIED) {
                    changeLog.modifiedStates.remove(state);
                }
                changeLog.deleted(state);
                break;
            case ItemState.STATUS_REMOVED:
                if (previousStatus == ItemState.STATUS_NEW) {
                    // was new and now removed again
                    changeLog.addedStates.remove(state);
                } else if (previousStatus == ItemState.STATUS_EXISTING_REMOVED) {
                    // was removed and is now saved
                    changeLog.deletedStates.remove(state);
                }
                break;
            case ItemState.STATUS_STALE_DESTROYED:
                // state is now stale. remove from modified
                changeLog.modifiedStates.remove(state);
                break;
            case ItemState.STATUS_STALE_MODIFIED:
                // state is now stale. keep in modified. wait until refreshed
                break;
            case ItemState.STATUS_NEW:
                // should never happen
                log.warn("ItemState changed status to 'new'");
                break;
            case ItemState.STATUS_UNDEFINED:
                // should never happen
                log.warn("ItemState changed status to 'undefined'");
                break;
            default:
                log.warn("ItemState has invalid status: " + state.getStatus());
        }
    }

    //--------------------------------------------------------< inner classes >

    /**
     * ItemStateManager view of the states in the attic
     *
     * @see TransientItemStateManager#getAttic()
     */
    private class AtticItemStateManager implements ItemStateManager {

        AtticItemStateManager() {
        }

        /**
         * Since the root node may never be removed, this method always returns
         * <code>null</code>.
         *
         * @return <code>null</code> since the root node cannot be removed.
         * @throws ItemStateException
         * @see ItemStateManager#getRootState()
         */
        public NodeState getRootState() throws ItemStateException {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        public ItemState getItemState(ItemId id)
                throws NoSuchItemStateException, ItemStateException {

            // TODO: too expensive. rather lookup item and check state
            ItemState state = null;
            for (Iterator it = changeLog.deletedStates(); it.hasNext(); ) {
                ItemState s = (ItemState) it.next();
                if (s.getId().equals(id)) {
                    state = s;
                }
            }
            if (state != null) {
                return state;
            } else {
                throw new NoSuchItemStateException(id.toString());
            }
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasItemState(ItemId id) {
            // TODO: too expensive. rather lookup item and check state
            for (Iterator it = changeLog.deletedStates(); it.hasNext(); ) {
                ItemState s = (ItemState) it.next();
                if (s.getId().equals(id)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public NodeReferences getNodeReferences(NodeId id)
                throws NoSuchItemStateException, ItemStateException {
            // n/a
            throw new ItemStateException("getNodeReferences() not implemented");
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasNodeReferences(NodeId id) {
            // n/a
            return false;
        }
    }
}
