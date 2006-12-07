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
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.value.QValue;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.ItemExistsException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
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
 * and {@link #createNewPropertyState(QName, NodeState, QPropertyDefinition, QValue[], int)}.
 */
public class TransientItemStateManager extends CachingItemStateManager
    implements ItemStateCreationListener {

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
     * The root node state or <code>null</code> if it hasn't been retrieved yet.
     */
    private NodeState rootNodeState;

    /**
     *
     * @param idFactory
     * @param parent
     */
    TransientItemStateManager(IdFactory idFactory, ItemStateManager parent) {
        super(new TransientISFactory(idFactory, parent), idFactory);
        this.changeLog = new ChangeLog(null);
        getTransientFactory().setListener(this);
    }


    private TransientItemStateFactory getTransientFactory() {
        return (TransientItemStateFactory) getItemStateFactory();
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
     * TODO: throw ItemExistsException? how to check?
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
                                 QNodeDefinition definition, NodeState parent) {
        NodeState nodeState = getTransientFactory().createNewNodeState(nodeName, uniqueID, parent, nodeTypeName, definition);

        parent.addChildNodeState(nodeState);
        changeLog.added(nodeState);
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
        PropertyState propState = getTransientFactory().createNewPropertyState(propName, parent, definition);
        // NOTE: callers must make sure, the property type is not 'undefined'
        propState.init(propertyType, values);

        parent.addPropertyState(propState);
        changeLog.added(propState);
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

    //---------------------------------------------------< ItemStateManager >---
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
            rootNodeState = getItemStateFactory().createRootState(this);
        }
        return rootNodeState;
    }

    /**
     * Return an item state given its id. Please note that this implementation
     * also returns item states that are in removed state ({@link
     * Status#EXISTING_REMOVED} but not yet saved.
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
                // only non transient states can change their status to
                // invalidated -> nothing to do here.
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
