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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.jcr.InvalidItemStateException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;

import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyEntry;
import org.apache.jackrabbit.jcr2spi.operation.Operation;
import org.apache.jackrabbit.jcr2spi.operation.SetMixin;
import org.apache.jackrabbit.jcr2spi.operation.SetPrimaryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers changes made to states and references and consolidates
 * empty changes.
 */
public class ChangeLog {

    /**
     * Logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(ChangeLog.class);

    /**
     * The changelog target: Root item of the tree whose changes are contained
     * in this changelog.
     */
    private final ItemState target;

    /**
     * Set of operations
     */
    private final Set<Operation> operations;

    private final Set<ItemState> affectedStates;

    /**
     * Create a new change log and populates it with operations and states
     * that are within the scope of this change set.
     *
     * @param target
     * @param operations
     * @param affectedStates
     * @throws InvalidItemStateException
     * @throws ConstraintViolationException
     */
    ChangeLog(ItemState target, Set<Operation> operations, Set<ItemState> affectedStates)
            throws InvalidItemStateException, ConstraintViolationException {
        this.target = target;
        this.operations = operations;
        this.affectedStates = affectedStates;
    }

    //-----------------------------------------------< Inform the ChangeLog >---
    /**
     * Call this method when this change log has been successfully persisted.
     * This implementation will call {@link Operation#persisted()} on the
     * individual operations followed by setting all remaining modified
     * states to EXISTING.
     */
    public void persisted() throws RepositoryException {
        List<NodeState> changedMixins = new ArrayList<NodeState>();
        List<NodeState> changedPrimaryTypes = new ArrayList<NodeState>();

        Operation[] ops = operations.toArray(new Operation[operations.size()]);
        for (int i = 0; i < ops.length; i++) {
            ops[i].persisted();
            if (ops[i] instanceof SetMixin) {
                changedMixins.add(((SetMixin) ops[i]).getNodeState());
            } else if (ops[i] instanceof SetPrimaryType) {
                changedPrimaryTypes.add(((SetPrimaryType) ops[i]).getNodeState());
            }
        }
        // process all remaining states that were not covered by the
        // operation persistence.
        for (ItemState state : affectedStates) {
            HierarchyEntry he = state.getHierarchyEntry();

            switch (state.getStatus()) {
                case Status.EXISTING_MODIFIED:
                    state.setStatus(Status.EXISTING);
                    if (state.isNode()) {
                        if (changedPrimaryTypes.contains(state)) {
                            // primary type changed for a node -> force reloading upon next
                            // access in order to be aware of modified definition etc...
                            he.invalidate(true);
                        } else if (changedMixins.contains(state)) {
                            // mixin changed for a node -> force reloading upon next
                            // access in order to be aware of modified uniqueID.
                            he.invalidate(false);
                        }
                    }
                    break;
                case Status.EXISTING_REMOVED:
                    he.remove();
                    break;
                case Status.NEW:
                    // illegal. should not get here.
                    log.error("ChangeLog still contains NEW state: " + state.getName());
                    state.setStatus(Status.EXISTING);
                    break;
                case Status.MODIFIED:
                case Status._UNDEFINED_:
                case Status.STALE_DESTROYED:
                case Status.STALE_MODIFIED:
                    // illegal.
                    log.error("ChangeLog contains state (" + state.getName() + ") with illegal status " + Status.getName(state.getStatus()));
                    break;
                case Status.EXISTING:
                    if (state.isNode() && changedMixins.contains(state)) {
                        // mixin changed for a node -> force reloading upon next
                        // access in order to be aware of modified uniqueID.
                        he.invalidate(false);
                    }
                    // otherwise: ignore. operations already have been completed
                    break;
                case Status.INVALIDATED:
                case Status.REMOVED:
                    he.invalidate(false);
                    break;
            }
        }
    }

    /**
     * Revert the changes listed within this changelog
     */
    public void undo() throws RepositoryException {
        Operation[] ops = operations.toArray(new Operation[operations.size()]);
        for (int i = ops.length - 1; i >= 0; i--) {
            ops[i].undo();
        }

        // process all remaining states that were not covered by the
        // operation undo.
        for (ItemState state : affectedStates) {
            switch (state.getStatus()) {
                case Status.EXISTING_MODIFIED:
                case Status.EXISTING_REMOVED:
                case Status.STALE_MODIFIED:
                case Status.STALE_DESTROYED:
                    state.getHierarchyEntry().revert();
                    break;
                case Status.NEW:
                    // illegal. should not get here.
                    log.error("ChangeLog still contains NEW state: " + state.getName());
                    state.getHierarchyEntry().revert();
                    break;
                case Status.MODIFIED:
                case Status._UNDEFINED_:
                    // illegal.
                    log.error("ChangeLog contains state (" + state.getName() + ") with illegal status " + Status.getName(state.getStatus()));
                    break;
                case Status.EXISTING:
                case Status.REMOVED:
                case Status.INVALIDATED:
                    // ignore already processed
                    break;
            }
        }
    }
    //----------------------< Retrieve information present in the ChangeLog >---
    /**
     * @return the target state
     */
    public ItemState getTarget() {
        return target;
    }

    /**
     * @return true if no <code>operations</code> are present.
     */
    public boolean isEmpty() {
        return operations.isEmpty();
    }

    /**
     * @return set of operations.
     */
    public Set<Operation> getOperations() {
        return operations;
    }

    /**
     * @return set of the affected states.
     */
    public Set<ItemState> getAffectedStates() {
        return affectedStates;
    }

    /**
     * Reset this change log, removing all members inside the
     * maps we built.
     */
    void reset() {
        affectedStates.clear();
        // also clear all operations
        operations.clear();
    }
}
