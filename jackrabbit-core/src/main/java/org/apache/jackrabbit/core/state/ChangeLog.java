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

import java.util.Map;

import org.apache.commons.collections4.map.LinkedMap;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.version.VersionItemStateManager;

/**
 * Registers changes made to states and references and consolidates
 * empty changes.
 */
public class ChangeLog {

    /**
     * Added states
     */
    private final Map<ItemId, ItemState> addedStates = new LinkedMap<>();

    /**
     * Modified states
     */
    private final Map<ItemId, ItemState> modifiedStates = new LinkedMap<>();

    /**
     * Deleted states
     */
    private final Map<ItemId, ItemState> deletedStates = new LinkedMap<>();

    /**
     * Modified references
     */
    private final Map<NodeId, NodeReferences> modifiedRefs = new LinkedMap<>();

    private long updateSize;

    /**
     * Checks whether this change log contains any changes. This method is
     * used to avoid extra work on updates that contain no changes.
     *
     * @since Apache Jackrabbit 1.5
     * @see <a href="https://issues.apache.org/jira/browse/JCR-1813">JCR-1813</a>
     * @return <code>true</code> if this log contains at least one change,
     *         <code>false</code> otherwise
     */
    public boolean hasUpdates() { 
        return !(addedStates.isEmpty() && modifiedStates.isEmpty()
                && deletedStates.isEmpty() && modifiedRefs.isEmpty()); 
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
        assert state != null;
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
        modifiedRefs.put(refs.id, refs);
    }

    /**
     * Removes the references entry with the given target node id.
     * This method is called by {@link VersionItemStateManager} to drop
     * references to virtual nodes.
     *
     * @param targetId target node id
     */
    public void removeReferencesEntry(NodeId targetId) {
        modifiedRefs.remove(targetId);
    }

    /**
     * Return an item state given its id. Returns <code>null</code>
     * if the item state is neither in the added nor in the modified
     * section. Throws a <code>NoSuchItemStateException</code> if
     * the item state is in the deleted section.
     *
     * @return item state or <code>null</code>
     * @throws NoSuchItemStateException if the item has been deleted
     */
    public ItemState get(ItemId id) throws NoSuchItemStateException {
        ItemState state = addedStates.get(id);
        if (state == null) {
            state = modifiedStates.get(id);
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
     */
    public boolean has(ItemId id) {
        return addedStates.containsKey(id) || modifiedStates.containsKey(id);
    }

    /**
     * Return a flag indicating whether a given item state is marked as
     * deleted in this log.
     *
     * @return <code>true</code> if item state is marked as deleted in this
     *         log; <code>false</code> otherwise
     */
    public boolean deleted(ItemId id) {
        return deletedStates.containsKey(id);
    }

    /**
     * Return a flag indicating whether a given item state is marked as
     * added in this log.
     *
     * @return <code>true</code> if item state is marked as added in this
     *         log; <code>false</code> otherwise
     */
    public boolean isAdded(ItemId id) {
        return addedStates.containsKey(id);
    }

    /**
     * Returns a flag indicating whether a given item state is marked as
     * modified in this log.
     *
     * @param id the id of the item.
     * @return <code>true</code> if the item state is marked as modified in this
     *         log; <code>false</code> otherwise.
     */
    public boolean isModified(ItemId id) {
        return modifiedStates.containsKey(id);
    }

    /**
     * Return a node references object given the target node id. Returns
     * <code>null</code> if the node reference is not in the modified
     * section.
     *
     * @return node references or <code>null</code>
     */
    public NodeReferences getReferencesTo(NodeId id) {
        return modifiedRefs.get(id);
    }

    /**
     * Return the added states in this change log.
     *
     * @return added states
     */
    public Iterable<ItemState> addedStates() {
        return addedStates.values();
    }

    /**
     * Return the modified states in this change log.
     * <p>
     * Note that this change log must not be modified while iterating
     * through the returned states.
     *
     * @return modified states
     */
    public Iterable<ItemState> modifiedStates() {
        return modifiedStates.values();
    }

    /**
     * Return the deleted states in this change log.
     * <p>
     * Note that this change log must not be modified while iterating
     * through the returned states.
     *
     * @return deleted states
     */
    public Iterable<ItemState> deletedStates() {
        return deletedStates.values();
    }

    /**
     * Return the modified references in this change log.
     * <p>
     * Note that this change log must not be modified while iterating
     * through the returned states.
     *
     * @return modified references
     */
    public Iterable<NodeReferences> modifiedRefs() {
        return modifiedRefs.values();
    }

    /**
     * Merge another change log with this change log
     *
     * @param other other change log
     */
    public void merge(ChangeLog other) {
        // Remove all states from our 'added' set that have now been deleted
        for (ItemState state : other.deletedStates()) {
            if (addedStates.remove(state.getId()) == null) {
                deletedStates.put(state.getId(), state);
            }
            // also remove from possibly modified state
            modifiedStates.remove(state.getId());
        }

        // only add modified states that are not already 'added'
        for (ItemState state : other.modifiedStates()) {
            if (!addedStates.containsKey(state.getId())) {
                modifiedStates.put(state.getId(), state);
            } else {
                // adapt status and replace 'added'
                state.setStatus(ItemState.STATUS_NEW);
                addedStates.put(state.getId(), state);
            }
        }

        // add 'added' states
        for (ItemState state : other.addedStates()) {
            addedStates.put(state.getId(), state);
        }

        // add refs
        modifiedRefs.putAll(other.modifiedRefs);
    }

    /**
     * Push all states contained in the various maps of
     * items we have.
     */
    public void push() {
        for (ItemState state : modifiedStates()) {
            state.push();
        }
        for (ItemState state : deletedStates()) {
            state.push();
        }
        for (ItemState state : addedStates()) {
            state.push();
        }
    }

    /**
     * After the states have actually been persisted, update their
     * internal states and notify listeners.
     */
    public void persisted() {
        for (ItemState state : modifiedStates()) {
            state.setStatus(ItemState.STATUS_EXISTING);
            state.notifyStateUpdated();
        }
        for (ItemState state : deletedStates()) {
            state.setStatus(ItemState.STATUS_EXISTING_REMOVED);
            state.notifyStateDestroyed();
            state.discard();
        }
        for (ItemState state : addedStates()) {
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
    }

    /**
     * Disconnect all states in the change log from their overlaid
     * states.
     */
    public void disconnect() {
        for (ItemState state : modifiedStates()) {
            state.disconnect();
        }
        for (ItemState state : deletedStates()) {
            state.disconnect();
        }
        for (ItemState state : addedStates()) {
            state.disconnect();
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
        for (ItemState state : modifiedStates()) {
            try {
                state.connect(parent.getItemState(state.getId()));
                state.pull();
            } catch (ItemStateException e) {
                state.discard();
            }
        }
        for (ItemState state : deletedStates()) {
            try {
                state.connect(parent.getItemState(state.getId()));
                state.pull();
            } catch (ItemStateException e) {
                state.discard();
            }
        }
        for (ItemState state : addedStates()) {
            state.discard();
        }
        reset();
    }

    /**
     * Returns the update size of the change log.
     * 
     * @return The update size.
     */
    public long getUpdateSize() {
        return updateSize;
    }

    /**
     * Sets the update size of the change log.
     * 
     * @param updateSize The update size.
     */
    public void setUpdateSize(long updateSize) {
        this.updateSize = updateSize;
    }

    /**
     * Returns a string representation of this change log for diagnostic
     * purposes.
     *
     * @return a string representation of this change log
     */
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("{");
        buf.append("#addedStates=").append(addedStates.size());
        buf.append(", #modifiedStates=").append(modifiedStates.size());
        buf.append(", #deletedStates=").append(deletedStates.size());
        buf.append(", #modifiedRefs=").append(modifiedRefs.size());
        buf.append("}");
        return buf.toString();
    }
}
