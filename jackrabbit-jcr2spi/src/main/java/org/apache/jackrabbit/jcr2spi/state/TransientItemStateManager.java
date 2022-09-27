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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;

import org.apache.commons.collections4.iterators.IteratorChain;
import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.PropertyEntry;
import org.apache.jackrabbit.jcr2spi.operation.Operation;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>TransientItemStateManager</code> adds support for transient changes on
 * {@link ItemState}s and also provides methods to create new item states.
 * While all other modifications can be invoked on the item state instances itself,
 * creating a new node state is done using
 * {@link #createNewNodeState(Name, String, Name, QNodeDefinition, NodeState)}
 * and
 * {@link #createNewPropertyState(Name, NodeState, QPropertyDefinition, QValue[], int)}.
 */
public class TransientItemStateManager implements ItemStateCreationListener {

    /**
     * Logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(TransientItemStateManager.class);

    /**
     * Added states
     */
    private final Set<ItemState> addedStates = new LinkedHashSet<ItemState>();

    /**
     * Modified states
     */
    private final Set<ItemState> modifiedStates = new LinkedHashSet<ItemState>();

    /**
     * Removed states
     */
    private final Set<ItemState> removedStates = new LinkedHashSet<ItemState>();
    /**
     * Stale states
     */
    private final Set<ItemState> staleStates = new LinkedHashSet<ItemState>();

    /**
     * Set of operations
     */
    private final Set<Operation> operations = new LinkedHashSet<Operation>();

    /**
     *
     */
    TransientItemStateManager() {
    }

    /**
     * @return the operations that have been recorded until now.
     */
    Iterator<Operation> getOperations() {
        return operations.iterator();
    }

    /**
     * Add the given operation to the list of operations to be recorded within
     * this TransientItemStateManager.
     *
     * @param operation
     */
    void addOperation(Operation operation) {
        operations.add(operation);
    }

    /**
     * @return <code>true</code> if this transient ISM has pending changes.
     */
    boolean hasPendingChanges() {
        return !operations.isEmpty();
    }

    /**
     * Create the change log for the tree starting at <code>target</code>. This
     * includes a  check if the ChangeLog to be created is totally 'self-contained'
     * and independent; items within the scope of this update operation (i.e.
     * below the target) must not have dependencies outside of this tree (e.g.
     * moving a node requires that the target node including both old and new
     * parents are saved).
     *
     * @param target
     * @param throwOnStale Throws InvalidItemStateException if either the given
     * <code>ItemState</code> or any of its descendants is stale and the flag is true.
     * @return
     * @throws InvalidItemStateException if a stale <code>ItemState</code> is
     * encountered while traversing the state hierarchy. The <code>changeLog</code>
     * might have been populated with some transient item states. A client should
     * therefore not reuse the <code>changeLog</code> if such an exception is thrown.
     * @throws RepositoryException if <code>state</code> is a new item state.
     */
    ChangeLog getChangeLog(ItemState target, boolean throwOnStale) throws InvalidItemStateException, ConstraintViolationException, RepositoryException {
        // fail-fast test: check status of this item's state
        if (target.getStatus() == Status.NEW) {
            String msg = "Cannot save/revert an item with status NEW (" +target+ ").";
            log.debug(msg);
            throw new RepositoryException(msg);
        }
        if (throwOnStale && Status.isStale(target.getStatus())) {
            String msg =  "Attempt to save/revert an item, that has been externally modified (" +target+ ").";
            log.debug(msg);
            throw new InvalidItemStateException(msg);
        }

        Set<Operation> ops = new LinkedHashSet<Operation>();
        Set<ItemState> affectedStates = new LinkedHashSet<ItemState>();

        HierarchyEntry he = target.getHierarchyEntry();
        if (he.getParent() == null) {
            // the root entry -> the complete change log can be used for
            // simplicity. collecting ops, states can be omitted.
            if (throwOnStale && !staleStates.isEmpty()) {
                String msg = "Cannot save changes: States has been modified externally.";
                log.debug(msg);
                throw new InvalidItemStateException(msg);
            } else {
                affectedStates.addAll(staleStates);
            }
            ops.addAll(operations);
            affectedStates.addAll(addedStates);
            affectedStates.addAll(modifiedStates);
            affectedStates.addAll(removedStates);
        } else {
            // not root entry:
            // - check if there is a stale state in the scope (save only)
            if (throwOnStale) {
                for (ItemState state : staleStates) {
                    if (containedInTree(target, state)) {
                        String msg = "Cannot save changes: States has been modified externally.";
                        log.debug(msg);
                        throw new InvalidItemStateException(msg);
                    }
                }
            }
            // - collect all affected states within the scope of save/undo
            @SuppressWarnings("unchecked")
            IteratorChain<ItemState> chain = new IteratorChain<>(addedStates.iterator(),
                    removedStates.iterator(),
                    modifiedStates.iterator());
            if (!throwOnStale) {
                chain.addIterator(staleStates.iterator());
            }
            while (chain.hasNext()) {
                ItemState state = (ItemState) chain.next();
                if (containedInTree(target, state)) {
                    affectedStates.add(state);
                }
            }
            // - collect the set of operations and
            //   check if the affected states listed by the operations are all
            //   listed in the modified,removed or added states collected by this
            //   changelog.
            for (Operation op : operations) {
                Collection<ItemState> opStates = op.getAffectedItemStates();
                for (ItemState state : opStates) {
                    if (affectedStates.contains(state)) {
                        // operation needs to be included
                        if (!affectedStates.containsAll(opStates)) {
                            // incomplete changelog: need to save a parent as well
                            String msg = "ChangeLog is not self contained.";
                            throw new ConstraintViolationException(msg);
                        }
                        // no violation: add operation an stop iteration over
                        // all affected states present in the operation.
                        ops.add(op);
                        break;
                    }
                }
            }
        }

        ChangeLog cl = new ChangeLog(target, ops, affectedStates);
        return cl;
    }

    /**
     * Creates a new transient {@link NodeState} that does not overlay any other
     * {@link NodeState}.
     *
     * @param nodeName     the name of the <code>NodeState</code> to create.
     * @param uniqueID     the uniqueID of the <code>NodeState</code> to create or
     *                     <code>null</code> if the created <code>NodeState</code>
     *                     cannot be identified by a unique ID.
     * @param nodeTypeName name of the node type of the new node state.
     * @param definition   The definition for the new node state.
     * @param parent       the parent of the new node state.
     * @return a new transient {@link NodeState}.
     */
    NodeState createNewNodeState(Name nodeName, String uniqueID, Name nodeTypeName,
                                 QNodeDefinition definition, NodeState parent)
            throws RepositoryException {
        NodeEntry ne = ((NodeEntry) parent.getHierarchyEntry()).addNewNodeEntry(nodeName, uniqueID, nodeTypeName, definition);
        try {
            parent.markModified();
        } catch (RepositoryException e) {
            ne.remove();
            throw e;
        }
        return ne.getNodeState();
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
     * with the given name.
     * @throws ConstraintViolationException
     * @throws RepositoryException
     */
    PropertyState createNewPropertyState(Name propName, NodeState parent,
                                         QPropertyDefinition definition,
                                         QValue[] values, int propertyType)
            throws ItemExistsException, ConstraintViolationException, RepositoryException {
        // NOTE: callers must make sure, the property type is not 'undefined'
        NodeEntry nodeEntry = (NodeEntry) parent.getHierarchyEntry();
        PropertyEntry pe = nodeEntry.addNewPropertyEntry(propName, definition, values, propertyType);
        try {
            parent.markModified();
        } catch (RepositoryException e) {
            pe.remove();
            throw e;
        }
        return pe.getPropertyState();
    }

    /**
     * Disposes this transient item state manager. Clears all references to
     * transiently modified item states.
     */
    void dispose() {
        addedStates.clear();
        modifiedStates.clear();
        removedStates.clear();
        staleStates.clear();
        // also clear all operations
        operations.clear();
    }

    /**
     * Remove the states and operations listed in the changeLog from internal
     * list of modifications.
     *
     * @param subChangeLog
     */
    void dispose(ChangeLog subChangeLog) {
        Set<ItemState> affectedStates = subChangeLog.getAffectedStates();
        addedStates.removeAll(affectedStates);
        modifiedStates.removeAll(affectedStates);
        removedStates.removeAll(affectedStates);
        staleStates.removeAll(affectedStates);

        operations.removeAll(subChangeLog.getOperations());
    }

    /**
     * A state has been removed. If the state is not a new state
     * (not in the collection of added ones), then remove
     * it from the modified states collection and add it to the
     * removed states collection.
     *
     * @param state state that has been removed
     */
    private void removed(ItemState state) {
        if (!addedStates.remove(state)) {
            modifiedStates.remove(state);
        }
        removedStates.add(state);
    }

    /**
     *
     * @param parent
     * @param state
     * @return
     */
    private static boolean containedInTree(ItemState parent, ItemState state) {
        HierarchyEntry he = state.getHierarchyEntry();
        HierarchyEntry pHe = parent.getHierarchyEntry();
        // short cuts first
        if (he == pHe || he.getParent() == pHe) {
            return true;
        }
        if (!parent.isNode() || he == pHe.getParent()) {
            return false;
        }
        // none of the simple cases: walk up hierarchy
        HierarchyEntry pe = he.getParent();
        while (pe != null) {
            if (pe == pHe) {
                return true;
            }
            pe = pe.getParent();
        }

        // state isn't descendant of 'parent'
        return false;
    }

    //-----------------------------------------< ItemStateLifeCycleListener >---
    /**
     * Depending on status of the given state adapt change log.
     * E.g. a revert on states will reset the status from 'existing modified' to
     * 'existing'. A state which changes from 'existing' to 'existing modified'
     * will go into the modified set of the change log, etc.
     *
     * @see ItemStateLifeCycleListener#statusChanged(ItemState, int)
     */
    public void statusChanged(ItemState state, int previousStatus) {
        /*
        Update the collections of states that were transiently modified.
        NOTE: cleanup of operations is omitted here. this is expected to
        occur upon {@link ChangeLog#save()} and {@link ChangeLog#undo()}.
        External modifications in contrast that clash with transient modifications
        render the corresponding states stale.
        */
        switch (state.getStatus()) {
            case (Status.EXISTING):
                switch (previousStatus) {
                    case Status.EXISTING_MODIFIED:
                        // was modified and got persisted or reverted
                        modifiedStates.remove(state);
                        break;
                    case Status.EXISTING_REMOVED:
                        // was transiently removed and is now reverted
                        removedStates.remove(state);
                        break;
                    case Status.STALE_MODIFIED:
                        // was modified and stale and is now reverted
                        staleStates.remove(state);
                        break;
                    case Status.NEW:
                        // was new and has been saved now
                        addedStates.remove(state);
                        break;
                    //default:
                        // INVALIDATED, MODIFIED ignore. no effect to transient modifications.
                        // any other status change is invalid -> see Status#isValidStatusChange(int, int
                }
                break;
            case Status.EXISTING_MODIFIED:
                // transition from EXISTING to EXISTING_MODIFIED
                modifiedStates.add(state);
                break;
            case (Status.EXISTING_REMOVED):
                // transition from EXISTING or EXISTING_MODIFIED to EXISTING_REMOVED
                removed(state);
                break;
            case (Status.REMOVED):
                switch (previousStatus) {
                    case Status.EXISTING_REMOVED:
                        // was transiently removed and removal was persisted.
                        // -> ignore
                        break;
                    case Status.NEW:
                        // a new entry was removed again: remember as removed
                        // in order to keep the operations and the affected
                        // states in sync
                        removed(state);
                        break;
                }
                // in any case: stop listening to status changes
                state.removeListener(this);
                break;
            case Status.STALE_DESTROYED:
            case Status.STALE_MODIFIED:
                /**
                 state is stale due to external modification -> move it to
                 the collection of stale item states.
                 validation omitted for only 'existing_modified' states can
                 become stale see {@link Status#isValidStatusChange(int, int)}
                 */
                modifiedStates.remove(state);
                staleStates.add(state);
                break;
            case Status.MODIFIED:
            case Status.INVALIDATED:
                // MODIFIED, INVALIDATED: ignore.
                log.debug("Item " + state.getName() + " changed status from " + Status.getName(previousStatus) + " to " + Status.getName(state.getStatus()) + ".");
                break;
            default:
                log.error("ItemState "+ state.getName() + " has invalid status: " + state.getStatus());
        }
    }

    //-----------------------------------------< ItemStateCreationListener >---
    /**
     * @see ItemStateCreationListener#created(ItemState)
     */
    public void created(ItemState state) {
        // new state has been created
        if (state.getStatus() == Status.NEW) {
            addedStates.add(state);
        }
    }
}
