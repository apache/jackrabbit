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
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.commons.collections.iterators.IteratorChain;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.ItemExistsException;
import java.util.Iterator;
import java.util.Collection;

/**
 * <code>TransientItemStateManager</code> implements a {@link ItemStateManager}
 * and adds support for transient changes on {@link ItemState}s. This item
 * state manager also returns item states that are transiently deleted. It is
 * the responsiblity of the caller to check whether a certain item state is
 * still valid. This item state manager also provides methods to create new
 * item states. While all other modifications can be invoked on the item state
 * instances itself, creating a new node state is done using
 * {@link #createNewNodeState(QName, String, QName, QNodeDefinition, NodeState)}
 * and {@link #createNewPropertyState(QName, NodeState, QPropertyDefinition)}.
 */
public class TransientItemStateManager extends CachingItemStateManager
        implements ItemStateLifeCycleListener {

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
     * The parent item state manager, which return item states that are then
     * overlayed by transient item states created by this TransientItemStateManager.
     */
    private final ItemStateManager parent;

    /**
     * The transient item state factory to create new and existing item state
     * instances.
     */
    private final TransientISFactory isf;

    /**
     * The root node state or <code>null</code> if it hasn't been retrieved yet.
     */
    private NodeState rootNodeState;

    TransientItemStateManager(IdFactory idFactory, ItemStateManager parent) {
        super(new TransientISFactory(idFactory, parent), idFactory);
        this.changeLog = new ChangeLog();
        this.parent = parent;
        this.isf = (TransientISFactory) getItemStateFactory();
        this.isf.setListener(this);
    }

    //-----------------------< ItemStateManager >-------------------------------

    /**
     * Return the root node state.
     *
     * @return the root node state.
     * @throws ItemStateException if an error occurs while retrieving the root
     *                            node state.
     * @see ItemStateManager#getRootState()
     */
    public NodeState getRootState() throws ItemStateException {
        if (rootNodeState == null) {
            rootNodeState = isf.createNodeState(parent.getRootState().getNodeId(), this);
            rootNodeState.addListener(this);
        }
        return rootNodeState;
    }

    /**
     * Return an item state given its id. Please note that this implementation
     * also returns item states that are in removed state ({@link
     * ItemState.STATUS_EXISTING_REMOVED} but not yet saved.
     *
     * @return item state.
     * @throws NoSuchItemStateException if there is no item state (not even a
     *                                  removed item state) with the given id.
     * @see ItemStateManager#getItemState(ItemId)
     */
    public ItemState getItemState(ItemId id) throws NoSuchItemStateException, ItemStateException {
        return super.getItemState(id);
    }

    /**
     * Return a flag indicating whether a given item state exists.
     *
     * @return <code>true</code> if item state exists within this item state
     *         manager; <code>false</code> otherwise
     * @see ItemStateManager#hasItemState(ItemId)
     */
    public boolean hasItemState(ItemId id) {
        return super.hasItemState(id);
    }

    /**
     * Always throws an {@link UnsupportedOperationException}. A transient item
     * state manager cannot not maintain node references.
     *
     * @param nodeState
     * @throws UnsupportedOperationException
     * @see ItemStateManager#getReferingStates(NodeState)
     */
    public Collection getReferingStates(NodeState nodeState) {
        throw new UnsupportedOperationException("getNodeReferences() not implemented");
    }

    /**
     * Always throws an {@link UnsupportedOperationException}. A transient item
     * state manager cannot not maintain node references.
     *
     * @param nodeState
     * @throws UnsupportedOperationException
     * @see ItemStateManager#hasReferingStates(NodeState)
     */
    public boolean hasReferingStates(NodeState nodeState) {
        throw new UnsupportedOperationException("hasNodeReferences() not implemented");
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
     * @return <code>true</code> if this transient ISM has pending changes.
     */
    public boolean hasPendingChanges() {
        return !changeLog.isEmpty();
    }

    /**
     * @return <code>true</code> if there are any deleted item states.
     */
    public boolean hasDeletedItemStates() {
        return !changeLog.deletedStates.isEmpty();
    }

    /**
     * @return an iterator over all modified or added item states.
     */
    public Iterator getModifiedOrAddedItemStates() {
        IteratorChain it = new IteratorChain();
        it.addIterator(changeLog.modifiedStates());
        it.addIterator(changeLog.addedStates());
        return it;
    }

    /**
     * @return an iterator over all deleted item states.
     */
    public Iterator getDeletedItemStates() {
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
     * @param definition   The qualified definition for the new node state.
     * @param parent       the parent of the new node state.
     * @return a new transient {@link NodeState}.
     */
    NodeState createNewNodeState(QName nodeName, String uuid, QName nodeTypeName,
                                 QNodeDefinition definition, NodeState parent) {
        NodeState nodeState = isf.createNewNodeState(nodeName, uuid, parent, nodeTypeName, definition);

        parent.addChildNodeState(nodeState, uuid);
        changeLog.added(nodeState);
        nodeState.addListener(this);
        return nodeState;
    }

    /**
     * Creates a new transient property state for a given <code>parent</code>
     * node state.
     *
     * @param propName the name of the property state to create.
     * @param parent   the node state where to the new property is added.
     * @param definition
     * @return the created property state.
     * @throws ItemExistsException if <code>parent</code> already has a property
     *                             with the given name.
     */
    PropertyState createNewPropertyState(QName propName, NodeState parent, QPropertyDefinition definition)
            throws ItemExistsException {
        PropertyState propState = isf.createNewPropertyState(propName, parent, definition);

        parent.addPropertyState(propState);
        changeLog.added(propState);
        propState.addListener(this);
        return propState;
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
                // new state has been created
                changeLog.added(state);
                break;
            case ItemState.STATUS_UNDEFINED:
                // should never happen
                log.warn("ItemState changed status to 'undefined'");
                break;
            default:
                log.warn("ItemState has invalid status: " + state.getStatus());
        }
    }

    //----------------------< TransientItemStateFactory >-----------------------

    private final static class TransientISFactory implements TransientItemStateFactory {

        private final IdFactory idFactory;

        private ItemStateLifeCycleListener listener;

        private final ItemStateManager parent;

        private TransientISFactory(IdFactory idFactory,
                                   ItemStateManager parent) {
            this.idFactory = idFactory;
            this.parent = parent;
        }

        private void setListener(ItemStateLifeCycleListener listener) {
            this.listener = listener;
        }

        /**
         * @inheritDoc
         * @see TransientItemStateFactory#createNewNodeState(QName, String, NodeState, QName, QNodeDefinition)
         */
        public NodeState createNewNodeState(QName name, String uuid,
                                            NodeState parent, QName nodetypeName,
                                            QNodeDefinition definition) {
            NodeState nodeState = new NodeState(name, uuid, parent, nodetypeName,
                definition, ItemState.STATUS_NEW, true, this, idFactory);
            // get a notification when this item state is saved or invalidated
            nodeState.addListener(listener);
            // notify listener that a node state has been created
            listener.statusChanged(nodeState, ItemState.STATUS_NEW);
            return nodeState;
        }

        /**
         * @inheritDoc
         * @see TransientItemStateFactory#createNewPropertyState(QName, NodeState, QPropertyDefinition)
         */
        public PropertyState createNewPropertyState(QName name, NodeState parent, QPropertyDefinition definition) {
            PropertyState propState = new PropertyState(name, parent,
                definition, ItemState.STATUS_NEW, true, idFactory);
            // get a notification when this item state is saved or invalidated
            propState.addListener(listener);
            // notify listener that a property state has been created
            listener.statusChanged(propState, ItemState.STATUS_NEW);
            return propState;
        }

        /**
         * @inheritDoc
         * @see ItemStateFactory#createNodeState(NodeId, ItemStateManager)
         */
        public NodeState createNodeState(NodeId nodeId, ItemStateManager ism)
                throws NoSuchItemStateException, ItemStateException {
            // retrieve state to overlay
            NodeState overlayedState = (NodeState) parent.getItemState(nodeId);
            NodeState overlayedParent = overlayedState.getParent();
            NodeState parentState = null;
            if (overlayedParent != null) {
                parentState = (NodeState) ism.getItemState(overlayedParent.getId());
            }
            NodeState nodeState = new NodeState(overlayedState, parentState,
                    ItemState.STATUS_EXISTING, true, this, idFactory);
            nodeState.addListener(listener);
            return nodeState;
        }

        /**
         * @inheritDoc
         * @see ItemStateFactory#createNodeState(NodeId, NodeState)
         */
        public NodeState createNodeState(NodeId nodeId, NodeState parentState)
                throws NoSuchItemStateException, ItemStateException {
            // retrieve state to overlay
            NodeState overlayedState = (NodeState) parent.getItemState(nodeId);
            NodeState nodeState = new NodeState(overlayedState, parentState,
                    ItemState.STATUS_EXISTING, true, this, idFactory);
            nodeState.addListener(listener);
            return nodeState;
        }

        /**
         * @inheritDoc
         * @see ItemStateFactory#createPropertyState(PropertyId, NodeState)
         */
        public PropertyState createPropertyState(PropertyId propertyId,
                                                 NodeState parentState)
                throws NoSuchItemStateException, ItemStateException {
            // retrieve state to overlay
            PropertyState overlayedState = (PropertyState) parent.getItemState(propertyId);
            PropertyState propState = new PropertyState(overlayedState, parentState,
                    ItemState.STATUS_EXISTING, true, idFactory);
            propState.addListener(listener);
            return propState;
        }
    }
}
