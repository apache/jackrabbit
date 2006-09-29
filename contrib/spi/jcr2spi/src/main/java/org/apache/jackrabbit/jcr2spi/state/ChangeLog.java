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
import org.apache.commons.collections.iterators.IteratorChain;

import java.util.Iterator;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.HashSet;

/**
 * Registers changes made to states and references and consolidates
 * empty changes.
 */
public class ChangeLog {

    private final ItemState target;
    /**
     * Added states
     */
    final Set addedStates = new LinkedHashSet();

    /**
     * Modified states
     */
    final Set modifiedStates = new LinkedHashSet();

    /**
     * Deleted states
     */
    final Set deletedStates = new LinkedHashSet();

    /**
     * Modified references
     */
    final Set modifiedRefs = new LinkedHashSet();

    /**
     * Type of operation this changelog is collection state modifications for.
     */  
    private Set operations = new LinkedHashSet();

    /**
     *
     * @param target
     */
    ChangeLog(ItemState target) {
        this.target = target;
    }

    //-----------------------------------------------< Inform the ChangeLog >---
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
        addedStates.add(state);
    }

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

    /**
     * A references has been modified
     *
     * @param refs refs that has been modified
     */
    public void modified(NodeReferences refs) {
        modifiedRefs.add(refs);
    }

    //----------------------< Retrieve information present in the ChangeLog >---
    /**
     *
     * @return
     */
    public ItemState getTarget() {
        return target;
    }

    /**
     *
     * @return
     */
    public boolean isEmpty() {
        return operations.isEmpty();
    }

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
        return addedStates.iterator();
    }

    /**
     * Return an iterator over all modified states.
     *
     * @return iterator over all modified states.
     */
    public Iterator modifiedStates() {
        return modifiedStates.iterator();
    }

    /**
     * Return an iterator over all deleted states.
     *
     * @return iterator over all deleted states.
     */
    public Iterator deletedStates() {
        return deletedStates.iterator();
    }

    /**
     * Return an iterator over all modified references.
     *
     * @return iterator over all modified references.
     */
    public Iterator modifiedRefs() {
        return modifiedRefs.iterator();
    }

    /**
     * Make sure that this ChangeLog is totally 'self-contained'
     * and independant; items within the scope of this update operation
     * must not have 'external' dependencies;
     * (e.g. moving a node requires that the target node including both
     * old and new parents are saved)
     */
    public void checkIsSelfContained()
            throws ItemStateException {
        Set affectedStates = new HashSet();
        affectedStates.addAll(modifiedStates);
        affectedStates.addAll(deletedStates);
        affectedStates.addAll(addedStates);
        Iterator it = new IteratorChain(modifiedStates(), deletedStates());
        while (it.hasNext()) {
            ItemState transientState = (ItemState) it.next();
            if (transientState.isNode()) {
                NodeState nodeState = (NodeState) transientState;
                Set dependentStates = new HashSet();
                if (nodeState.hasOverlayedState()) {
                    // TODO: oldParentState is overlayed state from workspace. do not use!
                    NodeState oldParentState = nodeState.getOverlayedState().getParent();
                    NodeState newParentState = nodeState.getParent();
                    if (oldParentState != null) {
                        if (newParentState == null) {
                            // node has been removed, add old parent
                            // to dependencies
                            dependentStates.add(oldParentState);
                        } else {
                            if (!oldParentState.equals(newParentState)) {
                                // node has been moved, add old and new parent
                                // to dependencies
                                dependentStates.add(oldParentState);
                                dependentStates.add(newParentState);
                            }
                        }
                    }
                }
                // removed child node entries
                Iterator cneIt = nodeState.getRemovedChildNodeEntries().iterator();
                while (cneIt.hasNext()) {
                    ChildNodeEntry cne = (ChildNodeEntry) cneIt.next();
                    dependentStates.add(cne.getNodeState());
                }
                // added child node entries
                cneIt = nodeState.getAddedChildNodeEntries().iterator();
                while (cneIt.hasNext()) {
                    ChildNodeEntry cne = (ChildNodeEntry) cneIt.next();
                    dependentStates.add(cne.getNodeState());
                }

                // now walk through dependencies and check whether they
                // are within the scope of this save operation
                Iterator depIt = dependentStates.iterator();
                while (depIt.hasNext()) {
                    NodeState dependantState = (NodeState) depIt.next();
                    if (!affectedStates.contains(dependantState)) {
                        // need to save the parent as well
                        String msg = dependantState.getNodeId().toString() + " needs to be saved as well.";
                        throw new ItemStateException(msg);
                    }
                }
            }
        }
    }

    /**
     * Populates this <code>ChangeLog</code> with operations that are within the
     * scope of this change set.
     *
     * @param operations an Iterator of <code>Operation</code>s which are the
     *                   candidates to be included in this <code>ChangeLog</code>.
     */
    public void collectOperations(Iterator operations) {
        Set affectedStates = new HashSet();
        affectedStates.addAll(deletedStates);
        affectedStates.addAll(modifiedStates);
        while (operations.hasNext()) {
            Operation op = (Operation) operations.next();
            Iterator states = op.getAffectedItemStates().iterator();
            while (states.hasNext()) {
                ItemState state = (ItemState) states.next();
                if (affectedStates.contains(state)) {
                    addOperation(op);
                    break;
                }
            }
        }
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
            state.notifyStateUpdated();  // TODO: is this needed anymore?
        }
        iter = deletedStates();
        while (iter.hasNext()) {
            ItemState state = (ItemState) iter.next();
            state.setStatus(ItemState.STATUS_REMOVED);
            state.notifyStateDestroyed();  // TODO: is this needed anymore?
        }
        iter = addedStates();
        while (iter.hasNext()) {
            ItemState state = (ItemState) iter.next();
            state.setStatus(ItemState.STATUS_EXISTING);
            state.notifyStateCreated();  // TODO: is this needed anymore?
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
        return operations.remove(operation);
    }
}
