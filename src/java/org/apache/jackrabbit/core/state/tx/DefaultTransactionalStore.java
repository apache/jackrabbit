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
import org.apache.jackrabbit.core.state.*;

/**
 * Represents the default transactional store that will sit on top of a
 * {@link PersistentItemStateProvider}. Updates to this store are not actually
 * transactional, since failures in later updates will not undo earlier changes.
 */
public class DefaultTransactionalStore implements TransactionalStore {

    /**
     * Provider to use
     */
    private PersistentItemStateProvider provider;

    /**
     * Create a new instance of this class
     *
     * @param provider peristent item state provider
     */
    public DefaultTransactionalStore(PersistentItemStateProvider provider) {
        this.provider = provider;
    }

    //----------------------------------------------------< TransactionalStore >

    /**
     * @see TransactionalStore#load
     */
    public ItemState load(ItemId id)
            throws NoSuchItemStateException, ItemStateException {

        return provider.getItemState(id);
    }

    /**
     * @see TransactionalStore#beginUpdate
     */
    public TransactionalStore.Update beginUpdate() {
        return new Update();
    }

    /**
     * Update implementation
     */
    class Update implements TransactionalStore.Update {

        /**
         * Flag indicating whether this update has already ended
         */
        private boolean ended;

        //-----------------------------------------< TransactionalStore.Update >

        /**
         * @see TransactionalStore.Update#put
         */
        public void put(ItemState state) throws ItemStateException {
            if (ended) {
                throw new IllegalStateException("Update already ended.");
            }

            if (state.isNode()) {
                put((NodeState) state);
            } else {
                put((PropertyState) state);
            }
        }

        /**
         * Put a node state
         *
         * @param from node state
         * @throws ItemStateException if an error occurs
         */
        private void put(NodeState from) throws ItemStateException {
            PersistentNodeState to = getOrCreateState(from);
            to.setParentUUIDs(from.getParentUUIDs());
            to.setMixinTypeNames(from.getMixinTypeNames());
            to.setDefinitionId(from.getDefinitionId());
            to.setChildNodeEntries(from.getChildNodeEntries());
            to.setPropertyEntries(from.getPropertyEntries());
            to.store();
        }

        /**
         * Get or create node state from underlying provider
         *
         * @param state state to retrieve
         * @return provider's node state
         */
        private PersistentNodeState getOrCreateState(NodeState state)
                throws ItemStateException {

            if (provider.hasItemState(state.getId())) {
                return (PersistentNodeState) provider.getItemState(state.getId());
            }
            return provider.createNodeState(state.getUUID(),
                    state.getNodeTypeName(), state.getParentUUID());
        }

        /**
         * Put a property state
         *
         * @param from property state
         * @throws ItemStateException if an error occurs
         */
        private void put(PropertyState from) throws ItemStateException {
            PersistentPropertyState to = getOrCreateState(from);
            to.setDefinitionId(from.getDefinitionId());
            to.setType(from.getType());
            to.setValues(from.getValues());
            to.store();
        }

        /**
         * Get or create property state from underlying provider
         *
         * @param state state to retrieve
         * @return provider's property state
         */
        private PersistentPropertyState getOrCreateState(PropertyState state)
                throws ItemStateException {

            if (provider.hasItemState(state.getId())) {
                return (PersistentPropertyState) provider.getItemState(state.getId());
            }
            return provider.createPropertyState(state.getParentUUID(), state.getName());
        }

        /**
         * @see TransactionalStore.Update#delete
         */
        public void delete(ItemState state) throws ItemStateException {
            if (ended) {
                throw new IllegalStateException("Update already ended.");
            }

            PersistableItemState to = (PersistableItemState)
                    provider.getItemState(state.getId());
            to.destroy();
        }

        /**
         * @see TransactionalStore.Update#end
         */
        public void end() throws ItemStateException {
            ended = true;
        }
    }
}
