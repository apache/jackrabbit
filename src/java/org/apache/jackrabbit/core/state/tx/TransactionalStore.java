/*
 * Copyright 2004 The Apache Software Foundation.
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
package org.apache.jackrabbit.core.state.tx;

import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;

/**
 * Represents a store that atomically saves its changes, i.e. either everything
 * is saved or nothing.
 */
public interface TransactionalStore {

    /**
     * Load an item from the store.
     */
    public ItemState load(ItemId id)
            throws NoSuchItemStateException, ItemStateException;

    /**
     * Begin an update operation. Subsequent save operations are passed to
     * the update object returned.
     *
     * @return update object
     */
    public Update beginUpdate();

    /**
     * Update operation. Created by a {@link TransactionalStore#beginUpdate}
     * operation.
     */
    public static interface Update {

        /**
         * Put a state as part of an update operation
         *
         * @param state state to store
         * @throws ItemStateException if an error occurs
         */
        public void put(ItemState state) throws ItemStateException;

        /**
         * Delete a state as part of an update operation
         *
         * @param state state to delete
         * @throws ItemStateException if an error occurs
         */
        public void delete(ItemState state) throws ItemStateException;

        /**
         * End this update operation. After having invoked this operation,
         * the update object should be considered invalid.
         *
         * @throws ItemStateException if an error occurs
         */
        public void end() throws ItemStateException;
    }
}
