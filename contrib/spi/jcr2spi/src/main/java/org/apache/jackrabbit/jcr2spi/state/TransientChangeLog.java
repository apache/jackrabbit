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
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.commons.collections.iterators.IteratorChain;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.util.Iterator;

/**
 * <code>TransientChangeLog</code> extends a {@link ChangeLog} and adds
 * more methods that support transient changes (e.g. resurrect deleted state).
 * Furthermore the item states of a transient change log are not disconnected
 * when added.
 */
public class TransientChangeLog extends ChangeLog
        implements TransientItemStateManager, TransientItemStateFactory, ItemStateListener {

    // TODO: TO-BE-FIXED. Usage of SPI_ItemId requries different handling

    /**
     * Logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(TransientChangeLog.class);

    /**
     *
     */
    private final IdFactory idFactory;

    /**
     * The parent item state manager, which return item states that are then
     * overlayed by transient item states created by this TransientChangeLog.
     */
    private final ItemStateManager parent;

    /**
     * ItemStateManager view of the states in the attic; lazily instantiated
     * in {@link #getAttic()}
     */
    private AtticItemStateManager attic;


    TransientChangeLog(IdFactory idFactory, ItemStateManager parent) {
        this.idFactory = idFactory;
        this.parent = parent;
    }

    //------------------< ChangeLog overwrites >--------------------------------

    /**
     * A state has been modified. If the state is not a new state
     * (not in the collection of added ones), then add
     * it to the modified states collection.
     *
     * @param state state that has been modified
     */
    public void modified(ItemState state) {
        if (!addedStates.contains(state)) {
            modifiedStates.add(state);
        }
    }

    /**
     * A state has been deleted. If the state is not a new state
     * (not in the collection of added ones), then remove
     * it from the modified states collection and add it to the
     * deleted states collection.
     *
     * @param state state that has been deleted
     */
    public void deleted(ItemState state) {
        if (addedStates.remove(state)) {
            modifiedStates.remove(state);
            deletedStates.add(state);
        }
    }

    //-----------------------< ItemStateManager >-------------------------------

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
        for (Iterator it = addedStates.iterator(); it.hasNext(); ) {
            ItemState s = (ItemState) it.next();
            if (s.getId().equals(id)) {
                state = s;
                break;
            }
        }
        if (state == null) {
            for (Iterator it = modifiedStates.iterator(); it.hasNext(); ) {
                ItemState s = (ItemState) it.next();
                if (s.getId().equals(id)) {
                    state = s;
                    break;
                }
            }
            if (state == null) {
                for (Iterator it = deletedStates.iterator(); it.hasNext(); ) {
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
        for (Iterator it = addedStates.iterator(); it.hasNext(); ) {
            ItemState s = (ItemState) it.next();
            if (s.getId().equals(id)) {
                return true;
            }
        }
        for (Iterator it = modifiedStates.iterator(); it.hasNext(); ) {
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
        for (Iterator it = modifiedRefs.iterator(); it.hasNext(); ) {
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

    //-----------------< TransientItemStateManager >----------------------------

    /**
     * @inheritDoc
     */
    public int getEntriesCount() {
        return addedStates.size() + modifiedStates.size();
    }

    /**
     * @inheritDoc
     */
    public boolean hasEntriesInAttic() {
        return deletedStates.size() > 0;
    }

    /**
     * @inheritDoc
     */
    public Iterator getEntries() {
        IteratorChain it = new IteratorChain();
        it.addIterator(modifiedStates());
        it.addIterator(addedStates());
        return it;
    }

    /**
     * @inheritDoc
     */
    public Iterator getEntriesInAttic() {
        return deletedStates();
    }

    /**
     * TODO: remove this method
     * @inheritDoc
     */
    public NodeState createNodeState(NodeId id,
                                     QName nodeTypeName,
                                     NodeState parent) {
        // DIFF JACKRABBIT: not needed anymore
        // check map; synchronized to ensure an entry is not created twice.
//        synchronized (addedStates) {
//            if (addedStates.containsKey(id) || modifiedStates.containsKey(id)) {
//                String msg = "there's already a node state instance with id " + id;
//                log.debug(msg);
//                throw new ItemStateException(msg);
//            }
//
//            NodeState state = new NodeState(id, nodeTypeName, parentId,
//                    initialStatus, true);
//            // put transient state in the map
//            addedStates.put(id, state);
//            return state;
//        }

        // TODO: replace with call to createNewNodeState() and finally remove method
        return new NodeState(id, parent, nodeTypeName, ItemState.STATUS_NEW, true, this);
    }

    /**
     * TODO: remove this method
     * @inheritDoc
     */
    public NodeState createNodeState(NodeState overlayedState) {
        ItemId id = overlayedState.getNodeId();

        // check map; synchronized to ensure an entry is not created twice.
        synchronized (addedStates) {
            NodeState state;
//            if ((state = (NodeState) addedStates.get(id)) != null
//                    || (state = (NodeState) modifiedStates.get(id)) != null) {
//                String msg = "there's already a node state instance with id " + id;
//                log.warn(msg);
//                return state;
//            }

            state = new NodeState(overlayedState, null, ItemState.STATUS_EXISTING_MODIFIED, true, this);
            // put transient state in the map
            modifiedStates.add(state);
            return state;
        }
    }

    /**
     * TODO: remove this method
     * @inheritDoc
     */
    public PropertyState createPropertyState(NodeState parentState, QName propName) {
        PropertyId id = idFactory.createPropertyId(parentState.getNodeId(), propName);
        return new PropertyState(id, parentState, ItemState.STATUS_NEW, true);
    }

    /**
     * TODO: remove this method
     * @inheritDoc
     */
    public PropertyState createPropertyState(PropertyState overlayedState) {

        PropertyId id = overlayedState.getPropertyId();

        // check map; synchronized to ensure an entry is not created twice.
        synchronized (addedStates) {
            PropertyState state;
//            if ((state = (PropertyState) addedStates.get(id)) != null
//                    || (state = (PropertyState) modifiedStates.get(id)) != null) {
//                String msg = "there's already a property state instance with id " + id;
//                log.warn(msg);
//                return state;
//            }

            state = new PropertyState(overlayedState, null, ItemState.STATUS_EXISTING_MODIFIED, true);
            // put transient state in the map
            modifiedStates.add(state);
            return state;
        }
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
        if (addedStates.remove(state)) {
            modifiedStates.remove(state);
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
        if (addedStates.remove(state)) {
            modifiedStates.remove(state);
        }
        deletedStates.add(state);
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
        deletedStates.remove(state);
        state.onDisposed();
    }

    /**
     * @inheritDoc
     */
    public void disposeAllItemStates() {
        IteratorChain it = new IteratorChain();
        it.addIterator(modifiedStates());
        it.addIterator(addedStates());
        it.addIterator(deletedStates());
        while (it.hasNext()) {
            ItemState state = (ItemState) it.next();
            state.discard();
            state.onDisposed();
        }
        reset();
    }

    /**
     * @inheritDoc
     */
    public ItemStateManager getAttic() {
        if (attic == null) {
            attic = new AtticItemStateManager();
        }
        return attic;
    }

    /**
     * @inheritDoc
     */
    public void disposeOperations(Iterator operations) {
        while (operations.hasNext()) {
            removeOperation((Operation) operations.next());
        }
    }

    //----------------------< TransientItemStateFactory >-----------------------

    /**
     * @inheritDoc
     * @see TransientItemStateFactory#createNewNodeState(QName, String, NodeState)
     */
    public NodeState createNewNodeState(QName name, String uuid, NodeState parent) {
        NodeId id;
        if (uuid == null) {
            id = idFactory.createNodeId(parent.getNodeId(), Path.create(name, 0));
        } else {
            id = idFactory.createNodeId(uuid);
        }
        NodeState nodeState = new NodeState(id, parent, null, ItemState.STATUS_NEW, true, this);
        // get a notification when this item state is saved or invalidated
        nodeState.addListener(this);
        added(nodeState);
        return nodeState;
    }

    /**
     * @inheritDoc
     * @see TransientItemStateFactory#createNewPropertyState(QName, NodeState)
     */
    public PropertyState createNewPropertyState(QName name, NodeState parent) {
        PropertyId id = idFactory.createPropertyId(parent.getNodeId(), name);
        PropertyState propState = new PropertyState(id, parent, ItemState.STATUS_NEW, true);
        // get a notification when this item state is saved or invalidated
        propState.addListener(this);
        added(propState);
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
        return new NodeState(overlayedState, parentState, ItemState.STATUS_EXISTING, true, this);
    }

    /**
     * @inheritDoc
     * @see TransientItemStateFactory#createNodeState(NodeId, NodeState)
     */
    public NodeState createNodeState(NodeId nodeId, NodeState parentState)
            throws NoSuchItemStateException, ItemStateException {
        // retrieve state to overlay
        NodeState overlayedState = (NodeState) parent.getItemState(nodeId);
        return new NodeState(overlayedState, parentState, ItemState.STATUS_EXISTING, true, this);
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
        return new PropertyState(overlayedState, parentState, ItemState.STATUS_EXISTING, true);
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
        return new PropertyState(overlayedState, parentState, ItemState.STATUS_EXISTING, true);
    }

    //---------------------------< ItemStateListener >--------------------------

    /**
     * @inheritDoc
     * @see ItemStateListener#stateCreated(ItemState)
     */
    public void stateCreated(ItemState created) {
        // TODO: remove from added set of change log
    }

    /**
     * @inheritDoc
     * @see ItemStateListener#stateModified(ItemState)
     */
    public void stateModified(ItemState modified) {
        // TODO: remove from modified set of change log
    }

    /**
     * @inheritDoc
     * @see ItemStateListener#stateDestroyed(ItemState)
     */
    public void stateDestroyed(ItemState destroyed) {
        // TODO: remove from deleted set of change log
    }

    /**
     * @inheritDoc
     * @see ItemStateListener#stateDiscarded(ItemState)
     */
    public void stateDiscarded(ItemState discarded) {
        // TODO: remove from modified (and deleted?) set of change log
    }

    //--------------------------------------------------------< inner classes >

    /**
     * ItemStateManager view of the states in the attic
     *
     * @see TransientItemStateManager#getAttic
     */
    private class AtticItemStateManager implements ItemStateManager {

        AtticItemStateManager() {
        }

        /**
         * {@inheritDoc}
         */
        public ItemState getItemState(ItemId id)
                throws NoSuchItemStateException, ItemStateException {

            // TODO: too expensive. rather lookup item and check state
            ItemState state = null;
            for (Iterator it = deletedStates.iterator(); it.hasNext(); ) {
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
            for (Iterator it = deletedStates.iterator(); it.hasNext(); ) {
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
