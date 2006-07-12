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
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.name.QName;

import java.util.Iterator;

/**
 * <code>TransientItemStateManager</code> ...
 */
interface TransientItemStateManager extends ItemStateManager {

    /**
     * @return the number of entries
     */
    public int getEntriesCount();

    /**
     * @return the number of entries in attic
     */
    public int getEntriesInAtticCount();

    /**
     * @return an iterator over all entries
     */
    public Iterator getEntries();

    /**
     * @return an iterator over all entries in attic
     */
    public Iterator getEntriesInAttic();

    /**
     * Adds an operation to this transient item state manager.
     *
     * @param operationType the operation.
     * @throws IllegalStateException if <code>operationType</code> is not
     * compatible with the previously executed operation (invalid sequence of
     * operations).
     */
    public void addOperation(Operation operationType) throws IllegalStateException;

    //----------------< methods for creating & discarding ItemState instances >

    /**
     * DIFF JACKRABBIT: does not throw ItemStateException
     * Creates a new transient {@link NodeState} that does not overlay any other
     * {@link NodeState}.
     *
     * @param id the <code>NodeId</code> of the new node state.
     * @param nodeTypeName name of the node type of the new node state.
     * @param parentId the parent id of the new node state.
     * @return a new transient {@link NodeState}.
     */
    NodeState createNodeState(NodeId id, QName nodeTypeName,
                              NodeId parentId);

    /**
     * DIFF JACKRABBIT: does not throw ItemStateException
     * @param overlayedState
     * @return
     */
    NodeState createNodeState(NodeState overlayedState);

    /**
     * DIFF JACKRABBIT: does not throw ItemStateException
     * @param parentId
     * @param propName
     * @return
     */
    PropertyState createPropertyState(NodeId parentId, QName propName);

    /**
     * DIFF JACKRABBIT: does not throw ItemStateException
     * @param overlayedState
     * @return
     */
    PropertyState createPropertyState(PropertyState overlayedState);

    /**
     * Disposes the specified instance, i.e. discards it and removes it from
     * the map.
     *
     * @param state the <code>ItemState</code> instance that should be disposed
     * @see ItemState#discard()
     */
    public void disposeItemState(ItemState state);

    /**
     * Transfers the specified instance from the 'active' map to the attic.
     *
     * @param state the <code>ItemState</code> instance that should be moved to
     *              the attic
     */
    public void moveItemStateToAttic(ItemState state);

    /**
     * Disposes the specified instance in the attic, i.e. discards it and
     * removes it from the attic.
     *
     * @param state the <code>ItemState</code> instance that should be disposed
     * @see ItemState#discard()
     */
    public void disposeItemStateInAttic(ItemState state);

    /**
     * Disposes all transient item states in the cache and in the attic.
     */
    public void disposeAllItemStates();

    /**
     * Return the attic item state provider that holds all items
     * moved into the attic.
     *
     * @return attic
     */
    public ItemStateManager getAttic();

    //-----------------------------------< methods for controlling operations >

    /**
     * Disposes a collection of {@link org.apache.jackrabbit.jcr2spi.operation.Operation}s.
     *
     * @param operations the operations.
     */
    public void disposeOperations(Iterator operations);
}
