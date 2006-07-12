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
import org.apache.jackrabbit.jcr2spi.IdKeyMap;
import org.apache.jackrabbit.jcr2spi.DefaultIdKeyMap;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.NodeId;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * Registers changes made to states and references and consolidates
 * empty changes.
 */
// DIFF JR: implements ItemStateManager instead of separate 'get' 'has'
public class ChangeLog implements ItemStateManager {

    /**
     * Added states
     */
    // TODO: TO-BE-FIXED. With SPI_ItemId simple map cannot be used any more
    protected final IdKeyMap addedStates = new DefaultIdKeyMap(); // JR: new LinkedMap();

    /**
     * Modified states
     */
    // TODO: TO-BE-FIXED. With SPI_ItemId simple map cannot be used any more
    protected final IdKeyMap modifiedStates = new DefaultIdKeyMap(); // JR: new LinkedMap();

    /**
     * Deleted states
     */
    // TODO: TO-BE-FIXED. With SPI_ItemId simple map cannot be used any more
    protected final IdKeyMap deletedStates = new DefaultIdKeyMap(); // JR: new LinkedMap();

    /**
     * Modified references
     */
    // TODO: TO-BE-FIXED. With SPI_ItemId simple map cannot be used any more
    protected final IdKeyMap modifiedRefs = new DefaultIdKeyMap(); // JR: new LinkedMap();

    /**
     * Type of operation this changelog is collection state modifications for.
     */  
    private List operations = new ArrayList();

    //-----------------------------------------------< Inform the ChangeLog >---
    // DIFF JR: method added
    /**
     * Add the given operation to the list of operations to be recorded within
     * the current update cycle of this ChangeLog.
     *
     * @param operation
     */
    public void addOperation(Operation operation) {
        operations.add(operation);
    }

    /**
     * A state has been added
     *
     * @param state state that has been added
     */
    public void added(ItemState state) {
        addedStates.put(state.getId(), state);
    }

    /**
     * A state has been modified. If the state is not a new state
     * (not in the collection of added ones), then disconnect
     * the local state from its underlying shared state and add
     * it to the modified states collection.
     *
     * @param state state that has been modified
     */
    public void modified(ItemState state) {
        if (!addedStates.containsKey(state.getId())) {
            state.disconnect();
            modifiedStates.put(state.getId(), state);
        }
    }

    /**
     * A state has been deleted. If the state is not a new state
     * (not in the collection of added ones), then disconnect
     * the local state from its underlying shared state, remove
     * it from the modified states collection and add it to the
     * deleted states collection.
     *
     * @param state state that has been deleted
     */
    public void deleted(ItemState state) {
        if (addedStates.remove(state.getId()) == null) {
            state.disconnect();
            modifiedStates.remove(state.getId());
            deletedStates.put(state.getId(), state);
        }
    }

    /**
     * A references has been modified
     *
     * @param refs refs that has been modified
     */
    public void modified(NodeReferences refs) {
        modifiedRefs.put(refs.getId(), refs);
    }

    //----------------------< Retrieve information present in the ChangeLog >---
    // DIFF JR: method added
    public boolean isEmpty() {
        return operations.isEmpty();
    }

    // DIFF JR: method added
    /**
     *
     * @return
     */
    public Iterator getOperations() {
        return operations.iterator();
    }

    /**
     * Return an iterator over all added states.
     *
     * @return iterator over all added states.
     */
    public Iterator addedStates() {
        return addedStates.values().iterator();
    }

    /**
     * Return an iterator over all modified states.
     *
     * @return iterator over all modified states.
     */
    public Iterator modifiedStates() {
        return modifiedStates.values().iterator();
    }

    /**
     * Return an iterator over all deleted states.
     *
     * @return iterator over all deleted states.
     */
    public Iterator deletedStates() {
        return deletedStates.values().iterator();
    }

    /**
     * Return an iterator over all modified references.
     *
     * @return iterator over all modified references.
     */
    public Iterator modifiedRefs() {
        return modifiedRefs.values().iterator();
    }

    //-----------------------------< Inform ChangeLog about Success/Failure >---
    /**
     * Push all states contained in the various maps of
     * items we have.
     */
    public void push() {
        Iterator iter = modifiedStates();
        while (iter.hasNext()) {
            ((ItemState) iter.next()).push();
        }
        iter = deletedStates();
        while (iter.hasNext()) {
            ((ItemState) iter.next()).push();
        }
        iter = addedStates();
        while (iter.hasNext()) {
            ((ItemState) iter.next()).push();
        }
    }

    /**
     * After the states have actually been persisted, update their
     * internal states and notify listeners.
     */
    public void persisted() {
        Iterator iter = modifiedStates();
        while (iter.hasNext()) {
            ItemState state = (ItemState) iter.next();
            state.setStatus(ItemState.STATUS_EXISTING);
            state.notifyStateUpdated();
        }
        iter = deletedStates();
        while (iter.hasNext()) {
            ItemState state = (ItemState) iter.next();
            state.setStatus(ItemState.STATUS_EXISTING_REMOVED);
            state.notifyStateDestroyed();
            state.discard();
        }
        iter = addedStates();
        while (iter.hasNext()) {
            ItemState state = (ItemState) iter.next();
            state.setStatus(ItemState.STATUS_EXISTING);
            state.notifyStateCreated();
        }
    }

    /**
     * Reset this change log, removing all members inside the
     * maps we built.
     */
    public void reset() {
        addedStates.clear();
        modifiedStates.clear();
        deletedStates.clear();
        modifiedRefs.clear();
        // also clear all operations
        operations.clear();
    }

    /**
     * Disconnect all states in the change log from their overlaid
     * states.
     */
    public void disconnect() {
        Iterator iter = modifiedStates();
        while (iter.hasNext()) {
            ((ItemState) iter.next()).disconnect();
        }
        iter = deletedStates();
        while (iter.hasNext()) {
            ((ItemState) iter.next()).disconnect();
        }
        iter = addedStates();
        while (iter.hasNext()) {
            ((ItemState) iter.next()).disconnect();
        }
    }

    /**
     * Undo changes made to items in the change log. Discards
     * added items, refreshes modified and resurrects deleted
     * items.
     *
     * @param parent parent manager that will hold current data
     */
    public void undo(ItemStateManager parent) {
        Iterator iter = modifiedStates();
        while (iter.hasNext()) {
            ItemState state = (ItemState) iter.next();
            try {
                state.connect(parent.getItemState(state.getId()));
                state.pull();
            } catch (ItemStateException e) {
                state.discard();
            }
        }
        iter = deletedStates();
        while (iter.hasNext()) {
            ItemState state = (ItemState) iter.next();
            try {
                state.connect(parent.getItemState(state.getId()));
                state.pull();
            } catch (ItemStateException e) {
                state.discard();
            }
        }
        iter = addedStates();
        while (iter.hasNext()) {
            ((ItemState) iter.next()).discard();
        }
        reset();
    }

    //---------------------------------------------------< ItemStateManager >---
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
        ItemState state = (ItemState) addedStates.get(id);
        if (state == null) {
            state = (ItemState) modifiedStates.get(id);
            if (state == null) {
                if (deletedStates.containsKey(id)) {
                    throw new NoSuchItemStateException("State has been marked destroyed: " + id);
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
        return addedStates.containsKey(id) || modifiedStates.containsKey(id);
    }

    /**
     * Return a node references object given its id. Returns
     * <code>null</code> if the node reference is not in the modified
     * section.
     *
     * @return node references or <code>null</code>
     * @see ItemStateManager#getNodeReferences(NodeId)
     */
    public NodeReferences getNodeReferences(NodeId id) throws NoSuchItemStateException, ItemStateException {
        return (NodeReferences) modifiedRefs.get(id);
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
        return modifiedRefs.get(id) != null;
    }

    //-------------------------------------------------------------< Object >---
    /**
     * Returns a string representation of this change log for diagnostic
     * purposes.
     *
     * @return a string representation of this change log
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("{");
        buf.append("#addedStates=").append(addedStates.size());
        buf.append(", #modifiedStates=").append(modifiedStates.size());
        buf.append(", #deletedStates=").append(deletedStates.size());
        buf.append(", #modifiedRefs=").append(modifiedRefs.size());
        buf.append("}");
        return buf.toString();
    }

    //----------------------------------< for derived classes >-----------------

    /**
     * Removes the <code>operation</code> from the list of operations.
     * @param operation the Operation to remove.
     * @return <code>true</code> if the operation was removed.
     */
    protected boolean removeOperation(Operation operation) {
        // @todo optimize
        return operations.remove(operation);
    }
}
