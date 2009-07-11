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
package org.apache.jackrabbit.core.observation;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.ZombieHierarchyManager;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.CachingHierarchyManager;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.Name;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import java.util.HashMap;
import java.util.Map;

/**
 * Implements a {@link HierarchyManager} that uses a {@link ChangeLog} for
 * the 'transient' changes on an underlying {@link ItemStateManager}.
 * {@link ItemState}s in attic are provided from the removed {@link ItemState}s
 * in the {@link ChangeLog}. The modified and added {@link ItemState}s in
 * the {@link ChangeLog} overlay the {@link ItemState}s in the
 * {@link ItemStateManager}.
 */
class ChangeLogBasedHierarchyMgr extends CachingHierarchyManager {

    ZombieHierarchyManager zombieHierMgr;

    /**
     * Creates a new <code>ChangeLogBasedHierarchyMgr</code> that overlays
     * <code>manager</code> with <code>changes</code> and uses the deleted
     * map of the <code>changes</code> as an attic <code>ItemStateManager</code>.
     * @param rootNodeId the id of the root node.
     * @param manager the item state manager.
     * @param changes the changes that will be applied on the item state manager.
     */
    ChangeLogBasedHierarchyMgr(NodeId rootNodeId,
                               ItemStateManager manager,
                               ChangeLog changes) {
        super(rootNodeId, new ChangeLogItemStateManager(manager, changes));
        zombieHierMgr = new ZombieHierarchyManager(
                this, provider, new AtticItemStateManager(changes));
    }

    /**
     * Same as {@link #getPath(ItemId)}} except that the <i>old</i> path is
     * returned in case of a moved/removed item.
     *
     * @param id the id of the node for which to retrieve the path.
     * @return the path of the item.
     * @throws ItemNotFoundException if an item state cannot be found.
     * @throws RepositoryException if another error occurs.
     */
    public Path getZombiePath(ItemId id)
            throws ItemNotFoundException, RepositoryException {
        return zombieHierMgr.getPath(id);
    }

    /**
     * Same as {@link #getName(NodeId, NodeId)} except that the <i>old</i> path
     * is returned in case of moved/removed item.
     *
     * @param id the id of the node for which to retrieve the name.
     * @param parentId the id of the parent node.
     * @return the name of the node.
     * @throws ItemNotFoundException if an item state cannot be found.
     * @throws RepositoryException if another error occurs.
     */
    public Name getZombieName(NodeId id, NodeId parentId)
            throws ItemNotFoundException, RepositoryException {
        return zombieHierMgr.getName(id, parentId);
    }

    /**
     * Implements an ItemStateManager that is overlayed by a ChangeLog.
     */
    private static class ChangeLogItemStateManager implements ItemStateManager {

        /**
         * The changes that will be applied to the {@link #base}.
         */
        private final ChangeLog changes;

        /**
         * The underlying {@link ItemStateManager}.
         */
        private final ItemStateManager base;

        /**
         * Creates a new <code>ChangeLogItemStateManager</code> that overlays
         * the {@link ItemState}s in <code>base</code> with the one found in
         * <code>changes</code>.
         * @param base the underlying {@link ItemStateManager}.
         * @param changes
         */
        private ChangeLogItemStateManager(ItemStateManager base, ChangeLog changes) {
            this.base = base;
            this.changes = changes;
        }

        /**
         * Returns the {@link ItemState} with the <code>id</code>. This
         * ItemState manager first looks up the <code>ChangeLog</code> and then
         * tries to find the ItemState in the base {@link ItemStateManager}.
         * @param id the id of the {@link ItemState}.
         * @return the {@link ItemState} with <code>id</code>.
         * @throws NoSuchItemStateException if there is no ItemState with
         * <code>id</code>.
         * @throws ItemStateException if any other error occurs.
         */
        public ItemState getItemState(ItemId id) throws NoSuchItemStateException, ItemStateException {
            // check ChangeLog first
            try {
                ItemState state = changes.get(id);
                if (state != null) {
                    return state;
                }
            } catch (NoSuchItemStateException e) {
                // item has been deleted, but we still return it by asking base
            }
            return base.getItemState(id);
        }

        /**
         * Returns <code>true</code> if there exists a {@link ItemState} either
         * in the {@link ChangeLog} or the base {@link ItemStateManager};
         * otherwise <code>false</code> is returned.
         * @param id the id of the {@link ItemState}.
         * @return <code>true</code> if there exists a {@link ItemState} either
         * in the {@link ChangeLog} or the base {@link ItemStateManager};
         * otherwise <code>false</code>.
         */
        public boolean hasItemState(ItemId id) {
            // check ChangeLog first
            try {
                ItemState state = changes.get(id);
                if (state != null) {
                    return true;
                }
            } catch (NoSuchItemStateException e) {
                // item has been deleted, but we still might return true by asking base
            }
            return base.hasItemState(id);
        }

        /**
         * Always throws a {@link UnsupportedOperationException}.
         */
        public NodeReferences getNodeReferences(NodeId id)
                throws NoSuchItemStateException, ItemStateException {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasNodeReferences(NodeId id) {
            return false;
        }
    }

    /**
     * Returns the removed {@link ItemState}s from the ChangeLog.
     */
    private static class AtticItemStateManager implements ItemStateManager {

        /**
         * Map of deleted {@link ItemState}s indexed by {@link ItemId}.
         */
        private final Map<ItemId, ItemState> deleted =
            new HashMap<ItemId, ItemState>();

        /**
         * Creates a new <code>AtticItemStateManager</code> based on
         * <code>changes</code>.
         * @param changes deleted {@link ItemState} are retrieved from this
         *  <code>ChangeLog</code>.
         */
        private AtticItemStateManager(ChangeLog changes) {
            for (ItemState state : changes.deletedStates()) {
                deleted.put(state.getId(), state);
            }
        }

        /**
         * Returns an {@link ItemState} if it is found in the deleted map of the
         * {@link ChangeLog}.
         * @param id the id of the {@link ItemState}.
         * @return the deleted {@link ItemState}.
         * @throws NoSuchItemStateException if the {@link ItemState} cannot
         * be found in the deleted map.
         * @throws ItemStateException if any other error occurs.
         */
        public ItemState getItemState(ItemId id) throws NoSuchItemStateException, ItemStateException {
            ItemState state = (ItemState) deleted.get(id);
            if (state != null) {
                return state;
            } else {
                throw new NoSuchItemStateException("Item not in the attic: " + id);
            }
        }

        /**
         * Returns <code>true</code> if an {@link ItemState} with <code>id</code>
         * is found in the deleted map of the {@link ChangeLog}; <code>false</code>
         * otherwise.
         * @param id the id of the {@link ItemState}.
         * @return <code>true</code> if an {@link ItemState} with <code>id</code>
         * is found in the deleted map of the {@link ChangeLog}; <code>false</code>
         * otherwise.
         */
        public boolean hasItemState(ItemId id) {
            return deleted.containsKey(id);
        }

        /**
         * Always throws a {@link UnsupportedOperationException}.
         */
        public NodeReferences getNodeReferences(NodeId id)
                throws NoSuchItemStateException, ItemStateException {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasNodeReferences(NodeId id) {
            return false;
        }
    }
}
