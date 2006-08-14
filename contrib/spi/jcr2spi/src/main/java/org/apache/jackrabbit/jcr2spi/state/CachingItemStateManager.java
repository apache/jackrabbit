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
import org.apache.jackrabbit.spi.EventIterator;
import org.apache.jackrabbit.spi.Event;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.jcr2spi.observation.InternalEventListener;
import org.apache.commons.collections.map.ReferenceMap;
import org.apache.commons.collections.map.LRUMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;

/**
 * <code>CachingItemStateManager</code> implements an {@link ItemStateManager}
 * and decorates it with a caching facility.
 */
public class CachingItemStateManager implements ItemStateManager, InternalEventListener {

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
     * The root node of the workspace.
     */
    private final NodeState root;

    /**
     * The Id factory.
     */
    private final IdFactory idFactory;

    /**
     * Creates a new <code>CachingItemStateManager</code>.
     *
     * @param isf       the item state factory to create item state instances.
     * @param idFactory the id factory.
     * @throws NoSuchItemStateException if the root node cannot be obtained.
     * @throws ItemStateException       if any other error occurs while
     *                                  obtaining the root node.
     */
    public CachingItemStateManager(ItemStateFactory isf, IdFactory idFactory)
            throws ItemStateException, NoSuchItemStateException {
        this.isf = isf;
        this.idFactory = idFactory;
        this.uuid2NodeState = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.WEAK);
        this.recentlyUsed = new LRUMap(1000); // TODO: make configurable
        // initialize root
        root = isf.createNodeState(idFactory.createNodeId((String) null, Path.ROOT), this);
    }

    //---------------------------------------------------< ItemStateManager >---

    public NodeState getRootState() throws ItemStateException {
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
     * @see ItemStateManager#getNodeReferences(NodeId)
     */
    public NodeReferences getNodeReferences(NodeId id)
            throws NoSuchItemStateException, ItemStateException {
        // TODO: implement
        return null;
    }

    /**
     * @inheritDoc
     * @see ItemStateManager#hasNodeReferences(NodeId)
     */
    public boolean hasNodeReferences(NodeId id) {
        // TODO: caching implement
        return false;
    }

    //-------------------------------< InternalEventListener >------------------

    /**
     * Processes <code>events</code> and invalidates cached <code>ItemState</code>s
     * accordingly.
     * @param events 
     * @param isLocal
     */
    public void onEvent(EventIterator events, boolean isLocal) {
        // if events origin from local changes then
        // cache does not need invalidation
        if (isLocal) {
            return;
        }

        // collect set of removed node ids
        Set removedNodeIds = new HashSet();
        List eventList = new ArrayList();
        while (events.hasNext()) {
            Event e = events.nextEvent();
            eventList.add(e);
        }

        for (Iterator it = eventList.iterator(); it.hasNext(); ) {
            Event e = (Event) it.next();
            ItemId itemId = e.getItemId();
            NodeId parentId = e.getParentId();
            ItemState state;
            NodeState parent;
            switch (e.getType()) {
                case Event.NODE_ADDED:
                case Event.PROPERTY_ADDED:
                    state = lookup(itemId);
                    if (state != null) {
                        // TODO: item already exists ???
                        // remove from cache and invalidate
                        recentlyUsed.remove(state);
                        state.discard();
                    }
                    parent = (NodeState) lookup(parentId);
                    if (parent != null) {
                        // discard and let wsp manager reload state when accessed next time
                        recentlyUsed.remove(parent);
                        parent.discard();
                    }
                    break;
                case Event.NODE_REMOVED:
                case Event.PROPERTY_REMOVED:
                    state = lookup(itemId);
                    if (state != null) {
                        if (itemId.denotesNode()) {
                            if (itemId.getRelativePath() == null) {
                                // also remove mapping from uuid
                                uuid2NodeState.remove(itemId.getUUID());
                            }
                        }
                        recentlyUsed.remove(state);
                        state.notifyStateDestroyed();
                    }
                    state = lookup(parentId);
                    if (state != null) {
                        parent = (NodeState) state;
                        // check if removed as well
                        if (removedNodeIds.contains(parent.getId())) {
                            // do not invalidate here
                        } else {
                            // discard and let wsp manager reload state when accessed next time
                            recentlyUsed.remove(parent);
                            parent.discard();
                        }
                    }
                    break;
                case Event.PROPERTY_CHANGED:
                    state = lookup(itemId);
                    // discard and let wsp manager reload state when accessed next time
                    if (state != null) {
                        recentlyUsed.remove(state);
                        state.discard();
                    }
            }
        }
    }

    //------------------------------< internal >--------------------------------

    /**
     * Called whenever an item state is accessed. Calling this method will update
     * the LRU map which keeps track of most recently used item states.
     *
     * @param state the touched state.
     */
    private void touch(ItemState state) {
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

        NodeState nodeState = null;
        // resolve uuid part
        if (uuid != null) {
            nodeState = (NodeState) uuid2NodeState.get(uuid);
            if (nodeState == null) {
                // state identified by the uuid is not yet cached -> get from ISM
                NodeId refId = (relPath == null) ? (NodeId) id : idFactory.createNodeId(uuid);
                nodeState = isf.createNodeState(refId, this);
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
    private ItemState lookup(ItemId id) {
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
            state = root;
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
}
