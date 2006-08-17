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

import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.commons.collections.map.ReferenceMap;
import org.apache.commons.collections.map.LRUMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Collection;
import java.util.Iterator;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

/**
 * <code>CachingItemStateManager</code> implements an {@link ItemStateManager}
 * and decorates it with a caching facility.
 */
public class CachingItemStateManager implements ItemStateManager {

    /**
     * The logger instance for this class.
     */
    private static Logger log = LoggerFactory.getLogger(CachingItemStateManager.class);

    /**
     * The item state factory to create <code>ItemState</code>s that are not
     * present in the cache.
     */
    private final ItemStateFactory isf;

    /**
     * Maps a String uuid to a {@link NodeState}.
     */
    private final Map uuid2NodeState;

    /**
     * Map of recently used <code>ItemState</code>.
     */
    private final Map recentlyUsed;

    /**
     * The root node of the workspace or <code>null</code> if it has not been
     * retrieved yet.
     */
    private NodeState root;

    /**
     * The Id factory.
     */
    private final IdFactory idFactory;

    /**
     * An {@link ItemStateLifeCycleListener} to maintain the LRU and UUID
     * reference cache.
     */
    private final ItemStateLifeCycleListener lifeCycleListener;

    /**
     * Creates a new <code>CachingItemStateManager</code>.
     *
     * @param isf       the item state factory to create item state instances.
     * @param idFactory the id factory.
     */
    public CachingItemStateManager(ItemStateFactory isf, IdFactory idFactory) {
        this.isf = isf;
        this.idFactory = idFactory;
        this.uuid2NodeState = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.WEAK);
        this.recentlyUsed = new LRUMap(1000); // TODO: make configurable
        this.lifeCycleListener = new ISLifeCycleListener();
    }

    //---------------------------------------------------< ItemStateManager >---

    public NodeState getRootState() throws ItemStateException {
        if (root == null) {
            root = isf.createNodeState(idFactory.createNodeId((String) null, Path.ROOT), this);
            root.addListener(lifeCycleListener);
        }
        return root;
    }

    /**
     * @inheritDoc
     * @see ItemStateManager#getItemState(ItemId)
     */
    public ItemState getItemState(ItemId id) throws NoSuchItemStateException, ItemStateException {
        return resolve(id);
    }

    /**
     * @inheritDoc
     * @see ItemStateManager#hasItemState(ItemId)
     */
    public boolean hasItemState(ItemId id) {
        try {
            resolve(id);
        } catch (ItemStateException e) {
            return false;
        }
        return true;
    }

    /**
     * @inheritDoc
     * @see ItemStateManager#getReferingStates(NodeState)
     * @param nodeState
     */
    public Collection getReferingStates(NodeState nodeState) throws ItemStateException {
        if (hasReferingStates(nodeState)) {
            Set refStates = new HashSet();
            Iterator it =  nodeState.getNodeReferences().iterator();
            while (it.hasNext()) {
                PropertyId pId = (PropertyId) it.next();
                refStates.add(getItemState(pId));
            }
            return Collections.unmodifiableCollection(refStates);
        } else {
            return Collections.EMPTY_SET;
        }
    }

    /**
     * @inheritDoc
     * @see ItemStateManager#hasReferingStates(NodeState)
     * @param nodeState
     */
    public boolean hasReferingStates(NodeState nodeState) {
        NodeReferences nr = nodeState.getNodeReferences();
        return nr != null && !nr.isEmpty();
    }

    //------------------------------< internal >--------------------------------

    /**
     * @return the item state factory of this <code>ItemStateManager</code>.
     */
    protected final ItemStateFactory getItemStateFactory() {
        return isf;
    }

    /**
     * Called whenever an item state is accessed. Calling this method will update
     * the LRU map which keeps track of most recently used item states.
     *
     * @param state the touched state.
     */
    protected void touch(ItemState state) {
        recentlyUsed.put(state, state);
    }

    /**
     * Resolves the id into an <code>ItemState</code>.
     *
     * @param id the id of the <code>ItemState</code> to resolve.
     * @return the <code>ItemState</code>.
     * @throws NoSuchItemStateException if there is no <code>ItemState</code>
     *                                  with <code>id</code>
     * @throws ItemStateException       if any other error occurs.
     */
    private ItemState resolve(ItemId id) throws NoSuchItemStateException, ItemStateException {
        String uuid = id.getUUID();
        Path relPath = id.getRelativePath();

        // start with root node if no uuid part in id
        NodeState nodeState = getRootState();
        // resolve uuid part
        if (uuid != null) {
            nodeState = (NodeState) uuid2NodeState.get(uuid);
            if (nodeState == null) {
                // state identified by the uuid is not yet cached -> get from ISM
                NodeId refId = (relPath == null) ? (NodeId) id : idFactory.createNodeId(uuid);
                nodeState = isf.createNodeState(refId, this);
                nodeState.addListener(lifeCycleListener);
                uuid2NodeState.put(uuid, nodeState);
            }
        }

        ItemState s = nodeState;
        if (relPath != null) {
            s = PathResolver.resolve(nodeState, relPath);
        }
        touch(s);
        return s;
    }

    /**
     * Looks up the <code>ItemState</code> with id but does not try to load the
     * item if it is not present in the cache.
     *
     * @param id the id of the ItemState to look up.
     * @return the cached <code>ItemState</code> or <code>null</code> if it is not
     *         present in the cache.
     */
    protected ItemState lookup(ItemId id) {
        ItemState state;
        // resolve UUID
        if (id.getUUID() != null) {
            state = (ItemState) uuid2NodeState.get(id.getUUID());
            if (state == null) {
                // not cached
                return null;
            }
        } else {
            // start from root
            try {
                state = getRootState();
            } catch (ItemStateException e) {
                log.warn("unable to get root node state:" + e.getMessage());
                return null;
            }
        }

        // resolve relative path
        if (id.getRelativePath() != null) {
            try {
                state = PathResolver.lookup(state, id.getRelativePath());
            } catch (ItemStateException e) {
                log.warn("exception while looking up state with id: " + id);
                return null;
            }
        }

        return state;
    }

    //------------------------< ItemStateListener >-----------------------------

    private class ISLifeCycleListener implements ItemStateLifeCycleListener {

        public void statusChanged(ItemState state, int previousStatus) {
            if (state.getStatus() == ItemState.STATUS_REMOVED ||
                    state.getStatus() == ItemState.STATUS_STALE_DESTROYED) {
                recentlyUsed.remove(state);
                if (state.isNode()) {
                    NodeState nodeState = (NodeState) state;
                    if (nodeState.getUUID() != null) {
                        uuid2NodeState.remove(nodeState.getUUID());
                    }
                }
            }
        }

        public void stateCreated(ItemState created) {
        }

        public void stateModified(ItemState modified) {
        }

        public void stateDestroyed(ItemState destroyed) {
        }

        public void stateDiscarded(ItemState discarded) {
        }
    }
}
