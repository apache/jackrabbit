/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import org.apache.jackrabbit.core.ItemId;

/**
 * The <code>ItemStateManager</code> interface...
 */
public interface ItemStateManager {

    /**
     * Return an item state, given its item id.
     * @param id item id
     * @return item state
     * @throws NoSuchItemStateException if the item does not exist
     * @throws ItemStateException if an error occurs
     */
    public ItemState getItemState(ItemId id)
            throws NoSuchItemStateException, ItemStateException;

    /**
     * Return a flag indicating whether an item state for a given
     * item id exists.
     * @param id item id
     * @return <code>true</code> if an item state exists,
     *         otherwise <code>false</code>
     */
    public boolean hasItemState(ItemId id);

    /**
     * Return a node references object, given its target id
     * @param id target id
     * @return node references object
     * @throws NoSuchItemStateException if the item does not exist
     * @throws ItemStateException if an error occurs
     */
    public NodeReferences getNodeReferences(NodeReferencesId id)
        throws NoSuchItemStateException, ItemStateException;
}
