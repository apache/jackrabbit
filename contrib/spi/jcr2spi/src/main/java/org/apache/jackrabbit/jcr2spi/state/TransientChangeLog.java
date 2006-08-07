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
import java.util.Iterator;

/**
 * <code>TransientChangeLog</code> extends a {@link ChangeLog} and adds
 * more methods that support transient changes (e.g. resurrect deleted state).
 * Furthermore the item states of a transient change log are not disconnected
 * when added.
 */
public class TransientChangeLog extends ChangeLog implements TransientItemStateManager {

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
     * ItemStateManager view of the states in the attic; lazily instantiated
     * in {@link #getAttic()}
     */
    private AtticItemStateManager attic;


    TransientChangeLog(IdFactory idFactory) {
        this.idFactory = idFactory;
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
        if (!addedStates.containsKey(state.getId())) {
            modifiedStates.put(state.getId(), state);
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
        if (addedStates.remove(state.getId()) == null) {
            modifiedStates.remove(state.getId());
            deletedStates.put(state.getId(), state);
        }
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

        return new NodeState(id, nodeTypeName, parent, ItemState.STATUS_NEW, true, idFactory);
    }

    /**
     * @inheritDoc
     */
    public NodeState createNodeState(NodeState overlayedState) {
        ItemId id = overlayedState.getNodeId();

        // check map; synchronized to ensure an entry is not created twice.
        synchronized (addedStates) {
            NodeState state;
            if ((state = (NodeState) addedStates.get(id)) != null
                    || (state = (NodeState) modifiedStates.get(id)) != null) {
                String msg = "there's already a node state instance with id " + id;
                log.warn(msg);
                return state;
            }

            state = new NodeState(overlayedState, ItemState.STATUS_EXISTING_MODIFIED, true);
            // put transient state in the map
            modifiedStates.put(id, state);
            return state;
        }
    }

    /**
     * @inheritDoc
     */
    public PropertyState createPropertyState(NodeState parentState, QName propName) {
        PropertyId id = idFactory.createPropertyId(parentState.getNodeId(), propName);
        return new PropertyState(id, parentState, ItemState.STATUS_NEW, true);
    }

    /**
     * @inheritDoc
     */
    public PropertyState createPropertyState(PropertyState overlayedState) {

        PropertyId id = overlayedState.getPropertyId();

        // check map; synchronized to ensure an entry is not created twice.
        synchronized (addedStates) {
            PropertyState state;
            if ((state = (PropertyState) addedStates.get(id)) != null
                    || (state = (PropertyState) modifiedStates.get(id)) != null) {
                String msg = "there's already a property state instance with id " + id;
                log.warn(msg);
                return state;
            }

            state = new PropertyState(overlayedState, ItemState.STATUS_EXISTING_MODIFIED, true);
            // put transient state in the map
            modifiedStates.put(id, state);
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
        if (addedStates.remove(state.getId()) == null) {
            modifiedStates.remove(state.getId());
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
        if (addedStates.remove(state.getId()) == null) {
            modifiedStates.remove(state.getId());
        }
        deletedStates.put(state.getId(), state);
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
        deletedStates.remove(state.getId());
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

            ItemState state = (ItemState) deletedStates.get(id);
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
            return deletedStates.containsKey(id);
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
