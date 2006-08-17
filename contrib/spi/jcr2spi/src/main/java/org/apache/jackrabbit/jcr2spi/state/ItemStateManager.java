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

import org.apache.jackrabbit.spi.ItemId;

import java.util.Collection;

/**
 * The <code>ItemStateManager</code> interface provides methods for retrieving
 * <code>ItemState</code> and <code>NodeReferences</code> instances by id.
 */
public interface ItemStateManager {

    /**
     * Returns the <code>NodeState</code> of the root node.
     *
     * @return node state of the root node.
     * @throws ItemStateException
     */
    NodeState getRootState() throws ItemStateException;

    /**
     * Return an item state, given its item id.
     *
     * @param id item id
     * @return item state
     * @throws NoSuchItemStateException if the item does not exist
     * @throws ItemStateException if an error occurs
     */
    ItemState getItemState(ItemId id) throws NoSuchItemStateException, ItemStateException;

    /**
     * Return a flag indicating whether an item state for a given
     * item id exists.
     *
     * @param id item id
     * @return <code>true</code> if an item state exists,
     *         otherwise <code>false</code>
     */
    boolean hasItemState(ItemId id);  // TODO: throw ItemStateException in case of error?

    /**
     * Return a collection of <code>PropertyState</code>s referring to the
     * Node identified by the given <code>NodeState</code>.
     *
     * @param nodeState
     * @return property states refering to the node identified by the given id.
     * @throws ItemStateException if the <code>PropertyState</code>s could not
     * be retrieved or if some other error occurs.
     */
    Collection getReferingStates(NodeState nodeState) throws ItemStateException;

    /**
     * Return a flag indicating whether any references to the <code>Node</code>
     * identified by the given state exist. In case the <code>Node</code> is not
     * referenceable this method will always return false.
     *
     * @param nodeState
     * @return <code>true</code> if any references exist to the <code>Node</code>
     * identified by the given node state.
     */
    boolean hasReferingStates(NodeState nodeState);
}
