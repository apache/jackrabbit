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
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.ItemExistsException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import java.util.Iterator;

/**
 * <code>TransientItemStateManager</code> adds support for transient changes on
 * {@link ItemState}s and also provides methods to create new item states.
 * While all other modifications can be invoked on the item state instances itself,
 * creating a new node state is done using
 * {@link #createNewNodeState(QName, String, QName, QNodeDefinition, NodeState)}
 * and
 * {@link #createNewPropertyState(QName, NodeState, QPropertyDefinition, QValue[], int)}.
 */
public class TransientItemStateManager implements ItemStateCreationListener {

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
    TransientItemStateManager() {
        this.changeLog = new ChangeLog(null);
    }

    /**
     * @return the operations that have been recorded until now.
     */
    Iterator getOperations() {
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
     * @return <code>true</code> if this transient ISM has pending changes.
     */
    boolean hasPendingChanges() {
        return !changeLog.isEmpty();
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
     * @param definition   The qualified definition for the new node state.
     * @param parent       the parent of the new node state.
     * @return a new transient {@link NodeState}.
     */
    NodeState createNewNodeState(QName nodeName, String uniqueID, QName nodeTypeName,
                                 QNodeDefinition definition, NodeState parent)
        throws ItemExistsException {

        parent.checkIsSessionState();
        NodeState nodeState = ((NodeEntry) parent.getHierarchyEntry()).addNewNodeEntry(nodeName, uniqueID, nodeTypeName, definition);
        parent.markModified();

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
     * with the given name.
     * @throws ConstraintViolationException
     * @throws RepositoryException
     */
    PropertyState createNewPropertyState(QName propName, NodeState parent,
                                         QPropertyDefinition definition,
                                         QValue[] values, int propertyType)
        throws ItemExistsException, ConstraintViolationException, RepositoryException {

        parent.checkIsSessionState();
        PropertyState propState = ((NodeEntry) parent.getHierarchyEntry()).addNewPropertyEntry(propName, definition);
        // NOTE: callers must make sure, the property type is not 'undefined'
        propState.init(propertyType, values);
        parent.markModified();

        return propState;
    }

    /**
     * Disposes this transient item state manager. Clears all references to
     * transiently modified item states.
     */
    void dispose() {
        changeLog.reset();
    }

    /**
     * Remove the states and operations listed in the changeLog from the
     * internal changeLog.
     *
     * @param subChangeLog
     */
    void dispose(ChangeLog subChangeLog) {
        changeLog.removeAll(subChangeLog);
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
        if (changeLog.isEmpty()) {
            return;
        }
        switch (state.getStatus()) {
            case Status.EXISTING:
            case Status.EXISTING_MODIFIED:
            case Status.EXISTING_REMOVED:
            case Status.REMOVED:
            case Status.STALE_DESTROYED:
                changeLog.removeAffected(state, previousStatus);
                break;
            case Status.STALE_MODIFIED:
                // state is now stale. keep in modified. wait until refreshed
            case Status.MODIFIED:
                // MODIFIED is only possible on EXISTING states -> thus, there
                // must not be any transient modifications for that state.
                // we ignore it.
            case Status.INVALIDATED:
                // -> nothing to do here.
                break;
            default:
                log.error("ItemState has invalid status: " + state.getStatus());
        }
    }

    //-----------------------------------------< ItemStateCreationListener >---
    /**
     * @see ItemStateCreationListener#created(ItemState)
     */
    public void created(ItemState state) {
        // new state has been created
        if (state.getStatus() == Status.NEW) {
            changeLog.added(state);
        }
    }
}
