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

import org.apache.jackrabbit.util.PathMap;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.EventIterator;
import org.apache.jackrabbit.spi.Event;
import org.apache.jackrabbit.jcr2spi.observation.InternalEventListener;

import java.util.Map;
import java.util.HashMap;
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
     * The underlying item state manager.
     */
    private final ItemStateManager ism;

    /**
     * Maps a String uuid to a {@link NodeState}.
     */
    private final Map uuid2PathElement;

    /**
     * Maps a path to an {@link ItemState}.
     */
    private final PathMap path2State;

    public CachingItemStateManager(ItemStateManager ism) {
        this.ism = ism;
        this.uuid2PathElement = new HashMap(); // TODO: must use weak references
        path2State = new PathMap();      // TODO: must use weak references
    }

    //---------------------------------------------------< ItemStateManager >---
    /**
     * @inheritDoc
     * @see ItemStateManager#getItemState(ItemId)
     */
    public ItemState getItemState(ItemId id) throws NoSuchItemStateException, ItemStateException {
        return (ItemState) getPathElement(id).get();
    }

    /**
     * @inheritDoc
     * @see ItemStateManager#hasItemState(ItemId)
     */
    public boolean hasItemState(ItemId id) {
        try {
            getPathElement(id);
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
        // TODO: caching missing
        return ism.getNodeReferences(id);
    }

    /**
     * @inheritDoc
     * @see ItemStateManager#hasNodeReferences(NodeId)
     */
    public boolean hasNodeReferences(NodeId id) {
        // TODO: caching missing
        return ism.hasNodeReferences(id);
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
                    PathMap.Element elem = lookup(itemId);
                    if (elem != null) {
                        // TODO: item already exists ???
                        // remove from cache and invalidate
                        state = (ItemState) elem.get();
                        if (itemId.denotesNode()) {
                            elem.set(null);
                        } else {
                            elem.remove();
                        }
                        if (state != null) {
                            state.discard();
                        }
                    }
                    elem = lookup(parentId);
                    if (elem != null) {
                        // discard and let wsp manager reload state when accessed next time
                        parent = (NodeState) elem.get();
                        if (parent != null) {
                            parent.discard();
                            elem.set(null);
                        }
                    }
                    break;
                case Event.NODE_REMOVED:
                case Event.PROPERTY_REMOVED:
                    elem = lookup(itemId);
                    if (elem != null) {
                        state = (ItemState) elem.get();
                        if (itemId.denotesNode()) {
                            if (itemId.getRelativePath() == null) {
                                // also remove mapping from uuid
                                uuid2PathElement.remove(itemId.getUUID());
                            }
                        }

                        elem.remove();

                        if (state != null) {
                            state.notifyStateDestroyed();
                        }
                    }
                    elem = lookup(parentId);
                    if (elem != null && elem.get() != null) {
                        parent = (NodeState) elem.get();
                        // check if removed as well
                        if (removedNodeIds.contains(parent.getId())) {
                            // do not invalidate here
                        } else {
                            // discard and let wsp manager reload state when accessed next time
                            parent.discard();
                            elem.set(null);
                        }
                    }
                    break;
                case Event.PROPERTY_CHANGED:
                    elem = lookup(itemId);
                    // discard and let wsp manager reload state when accessed next time
                    if (elem != null) {
                        state = (ItemState) elem.get();
                        state.discard();
                        elem.remove();
                    }
            }
        }
    }

    //------------------------------< internal >--------------------------------

    /**
     * Returns the PathMap.Element which holds the ItemState with
     * <code>id</code>. The returned <code>PathMap.Element</code> is guaranteed
     * to reference an <code>ItemState</code> calling {@link
     * PathMap.Element#get()};
     *
     * @param id the id of the item state.
     * @return the PathElement.Element.
     * @throws NoSuchItemStateException if there is no ItemState with this id.
     * @throws ItemStateException       if another error occurs.
     */
    private PathMap.Element getPathElement(ItemId id)
            throws NoSuchItemStateException, ItemStateException {
        String uuid = id.getUUID();
        Path relPath = id.getRelativePath();
        PathMap.Element elem = null;

        // first get PathElement of uuid part
        if (uuid != null) {
            elem = (PathMap.Element) uuid2PathElement.get(uuid);
            if (elem == null || elem.get() == null) {
                // state identified by the uuid is not yet cached -> get from ISM
                ItemId refId = (relPath == null) ? id : new NodeIdImpl(uuid);
                ItemState state = ism.getItemState(refId);

                // put this state to cache
                // but first we have to make sure that the parent of this state
                // is already cached

                if (state.getParentId() == null) {
                    // shortcut for the root node
                    elem = path2State.map(Path.ROOT, true);
                    elem.set(state);
                    uuid2PathElement.put(uuid, elem);
                    return elem;
                } else {
                    // get element of parent this will force loading of
                    // parent into cache if needed
                    PathMap.Element parentElement = getPathElement(state.getParentId());
                    // create path element if necessary
                    if (elem == null) {
                        // TODO put element (Marcel, here goes your extra method)
                        // i removed it, since its not present in JR commons (anchela)
                    }
                    elem.set(state);
                    // now put current state to cache
                    uuid2PathElement.put(uuid, elem);
                }
            }
        }

        // at this point we are guaranteed to have an element
        // now resolve relative path part of id if there is one
        if (relPath != null) {
            // TODO map element (Marcel, here goes your extra method)
            // i removed it, since its not present in JR commons (anchela)
            PathMap.Element tmp = null;
            if (tmp == null) {
                // not yet cached, load from ism
                ItemState state = ism.getItemState(id);
                // put to cache
                // TODO put element (Marcel, here goes your extra method)
                // i removed it, since its not present in JR commons (anchela)
                tmp = null;
                tmp.set(state);
            }
            elem = tmp;
        }
        return elem;
    }

    /**
     * Looks up the <code>Element</code> with id but does not try to load the
     * item if it is not present in the cache.
     *
     * @param id the id of the ItemState to look up.
     * @return the cached <code>Element</code> or <code>null</code> if it is not
     *         present in the cache.
     */
    private PathMap.Element lookup(ItemId id) {
        PathMap.Element elem = null;
        // resolve UUID
        if (id.getUUID() != null) {
            elem = (PathMap.Element) uuid2PathElement.get(id.getUUID());
            if (elem == null) {
                // not cached
                return null;
            }
        }

        // resolve relative path
        if (id.getRelativePath() != null) {
            // TODO map element (Marcel, here goes your extra method)
            // i removed it, since its not present in JR commons (anchela)
            elem = null;
        }

        return elem;
    }

    /**
     * Returns the name of <code>state</code> starting from
     * <code>parent</code>. This method only works for a direct parent of
     * <code>state</code>.
     *
     * @param parent the parent of <code>state</code>.
     * @param state the state.
     * @return the name element for <code>state</code> starting from
     * <code>parent</code>
     * @throws ItemStateException if an error occurs
     */
    private static Path.PathElement getNameElement(NodeState parent, ItemState state)
            throws ItemStateException {
        if (state.isNode()) {
            NodeState.ChildNodeEntry entry = parent.getChildNodeEntry((NodeId) state.getId());
            if (entry == null) {
                throw new ItemStateException("No child node entry " +
                        state.getId() + " found in " + parent.getId());
            } else {
                return Path.create(entry.getName(), entry.getIndex()).getNameElement();
            }
        } else {
            return state.getId().getRelativePath().getNameElement();
        }
    }

    //--------------------------------------------------------< Inner class >---
    /**
     * Simple implementation of the NodeId interface that always wraps around a
     * UUID String only and never takes a relative path.
     * Since the uuid is retrieved from an existing <code>ItemId</code> there is
     * no need to pass the IdFactory and using this simple implementation instead.
     */
    private static final class NodeIdImpl implements NodeId {

        private final String uuid;

        public NodeIdImpl(String uuid) {
            if (uuid == null) {
                throw new IllegalArgumentException("Expected non-null uuid.");
            }
            this.uuid = uuid;
        }

        /**
         * Always return <code>true</code>.
         *
         * @return true
         */
        public boolean denotesNode() {
            return true;
        }

        /**
         * Always returns a non-null string.
         *
         * @return uuid passed to the constructor, which is never <code>null</code>.
         */
        public String getUUID() {
            return uuid;
        }

        /**
         * Always return <code>null</code>.
         *
         * @return <code>null</code>
         */
        public Path getRelativePath() {
            return null;
        }
    }
}
