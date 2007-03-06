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
import org.apache.jackrabbit.jcr2spi.operation.AddNode;
import org.apache.jackrabbit.jcr2spi.operation.AddProperty;
import org.apache.jackrabbit.jcr2spi.operation.SetMixin;
import org.apache.jackrabbit.jcr2spi.config.CacheBehaviour;
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.name.QName;
import org.apache.commons.collections.iterators.IteratorChain;

import javax.jcr.nodetype.ConstraintViolationException;
import java.util.Iterator;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.Collection;

/**
 * Registers changes made to states and references and consolidates
 * empty changes.
 */
public class ChangeLog {

    private final ItemState target;
    /**
     * Added states
     */
    private final Set addedStates = new LinkedHashSet();

    /**
     * Modified states
     */
    private final Set modifiedStates = new LinkedHashSet();

    /**
     * Deleted states
     */
    private final Set deletedStates = new LinkedHashSet();

    /**
     * Set of operations
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
        if (!addedStates.remove(state)) {
            modifiedStates.remove(state);
            deletedStates.add(state);
        }
    }

    /**
     * Call this method when this change log has been sucessfully persisted.
     * This implementation will call {@link ItemState#persisted(ChangeLog, CacheBehaviour)
     * ItemState.refresh(this)} on the target item of this change log.
     * TODO: remove parameter CacheBehaviour
     */
    public void persisted(CacheBehaviour cacheBehaviour) {
        target.persisted(this, cacheBehaviour);
    }

    /**
     * Revert the changes listed within this changelog
     */
    public void undo() throws ItemStateException {
        // TODO: check if states are reverted in the correct order
        Iterator[] its = new Iterator[] {addedStates(), deletedStates(), modifiedStates()};
        IteratorChain chain = new IteratorChain(its);
        while (chain.hasNext()) {
            ItemState state = (ItemState) chain.next();
            state.getHierarchyEntry().revert();
        }
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
     * Returns true, if this change log contains the given <code>ItemState</code>
     * in the set of transiently removed states.
     *
     * @param state
     * @return
     */
    public boolean containsDeletedState(ItemState state) {
        return deletedStates.contains(state);
    }

    /**
     * Removes the subset of this changelog represented by the given
     * <code>ChangeLog</code> from this changelog.
     *
     * @param subChangeLog remove all entries (states, operations) present in
     * the given changelog from this changelog.
     */
    public void removeAll(ChangeLog subChangeLog) {
        addedStates.removeAll(subChangeLog.addedStates);
        modifiedStates.removeAll(subChangeLog.modifiedStates);
        deletedStates.removeAll(subChangeLog.deletedStates);

        operations.removeAll(subChangeLog.operations);
    }

    /**
     * Remove all entries and operation related to the given ItemState, that
     * are not used any more (respecting the status change).
     *
     * @param state
     */
    public void removeAffected(ItemState state, int previousStatus) {
        switch (state.getStatus()) {
            case (Status.EXISTING):
                switch (previousStatus) {
                    case Status.EXISTING_MODIFIED:
                        // was modified and is now refreshed
                        modifiedStates.remove(state);
                        break;
                    case Status.EXISTING_REMOVED:
                        // was removed and is now refreshed
                        deletedStates.remove(state);
                        break;
                    case Status.STALE_MODIFIED:
                        // was modified and state and is now refreshed
                        modifiedStates.remove(state);
                        break;
                    case Status.NEW:
                        // was new and has been saved now
                        addedStates.remove(state);
                        break;
                }
                // TODO: check if correct: changelog gets cleared any way -> no need to remove operations
                break;
            case Status.EXISTING_MODIFIED:
                modified(state);
                break;
            case (Status.REMOVED):
                if (previousStatus == Status.NEW) {
                    // was new and now removed again
                    addedStates.remove(state);
                    deletedStates.remove(state);
                    // remove operations performed on the removed state
                    removeAffectedOperations(state);
                    /* remove the add-operation as well:
                       since the affected state of an 'ADD' operation is the parent
                       instead of the added-state, the set of operations
                       need to be searched for the parent state && the proper
                       operation type.
                       SET_MIXIN can be is a special case of adding a property */
                    NodeEntry parentEntry = state.getHierarchyEntry().getParent();
                    if (parentEntry != null && parentEntry.isAvailable()) {
                        try {
                            NodeState parent = parentEntry.getNodeState();
                            if (parent.getStatus() != Status.REMOVED) {
                                for (Iterator it = operations.iterator(); it.hasNext();) {
                                    Operation op = (Operation) it.next();
                                    if (op instanceof AddNode) {
                                        AddNode operation = (AddNode) op;
                                        if (operation.getParentState() == parent
                                                && operation.getNodeName().equals(state.getQName())) {
                                            // TODO: this will not work for name name siblings!
                                            it.remove();
                                            break;
                                        }
                                    } else if (op instanceof AddProperty) {
                                        AddProperty operation = (AddProperty) op;
                                        if (operation.getParentState() == parent
                                                && operation.getPropertyName().equals(state.getQName())) {
                                            it.remove();
                                            break;
                                        }
                                    } else if (op instanceof SetMixin &&
                                        QName.JCR_MIXINTYPES.equals(state.getQName()) &&
                                        ((SetMixin)op).getNodeState() == parent) {
                                        it.remove();
                                        break;
                                    }
                                }
                            }
                        } catch (ItemStateException e) {
                            // should never occur -> ignore
                        }
                    }
                } else if (previousStatus == Status.EXISTING_REMOVED) {
                    // was removed and is now saved
                    deletedStates.remove(state);
                    removeAffectedOperations(state);
                }
                break;
            case (Status.EXISTING_REMOVED):
                deleted(state);
                removeAffectedOperations(state);
                break;
            case Status.STALE_DESTROYED:
                // state is now stale. remove from modified
                modifiedStates.remove(state);
                removeAffectedOperations(state);
                break;
        }
    }

    private void removeAffectedOperations(ItemState state) {
        for (Iterator it = operations.iterator(); it.hasNext();) {
            Operation op = (Operation) it.next();
            if (op.getAffectedItemStates().contains(state)) {
                it.remove();
            }
        }
    }

    /**
     * Make sure that this ChangeLog is totally 'self-contained'
     * and independant; items within the scope of this update operation
     * must not have 'external' dependencies;
     * (e.g. moving a node requires that the target node including both
     * old and new parents are saved)
     */
    public void checkIsSelfContained() throws ConstraintViolationException {
        Set affectedStates = new HashSet();
        affectedStates.addAll(modifiedStates);
        affectedStates.addAll(deletedStates);
        affectedStates.addAll(addedStates);

        // check if the affected states listed by the operations are all
        // listed in the modified,deleted or added states collected by this
        // changelog.
        Iterator it = getOperations();
        while (it.hasNext()) {
            Operation op = (Operation) it.next();
            Collection opStates = op.getAffectedItemStates();
            if (!affectedStates.containsAll(opStates)) {
                // need to save the parent as well
                String msg = "ChangeLog is not self contained.";
                throw new ConstraintViolationException(msg);
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
        affectedStates.addAll(addedStates);
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

    /**
     * Reset this change log, removing all members inside the
     * maps we built.
     */
    public void reset() {
        addedStates.clear();
        modifiedStates.clear();
        deletedStates.clear();
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
        buf.append("}");
        return buf.toString();
    }
}
