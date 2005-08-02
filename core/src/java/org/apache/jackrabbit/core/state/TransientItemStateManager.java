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
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.name.QName;
import org.apache.log4j.Logger;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * <code>TransientItemStateManager</code> ...
 */
class TransientItemStateManager implements ItemStateManager {

    private static Logger log = Logger.getLogger(TransientItemStateManager.class);

    /**
     * map of those states that have been removed transiently
     */
    private final ItemStateMap atticMap;

    /**
     * map of new or modified transient states
     */
    private final ItemStateMap transientMap;

    /**
     * ItemStateManager view of the states in the attic; lazily instantiated
     * in {@link #getAttic()}
     */
    private AtticItemStateManager attic;

    /**
     * Creates a new <code>TransientItemStateManager</code> instance.
     */
    TransientItemStateManager() {
        transientMap = new ItemStateMap();
        atticMap = new ItemStateMap();
    }

    /**
     * Dumps the state of this <code>TransientItemStateManager</code> instance
     * (used for diagnostic purposes).
     *
     * @param ps
     */
    void dump(PrintStream ps) {
        ps.println("TransientItemStateManager (" + this + ")");
        ps.println();
        ps.print("[transient] ");
        transientMap.dump(ps);
        ps.println();
        ps.print("[attic]     ");
        atticMap.dump(ps);
    }

    //----------------------------------------------------< ItemStateProvider >
    /**
     * {@inheritDoc}
     */
    public ItemState getItemState(ItemId id)
            throws NoSuchItemStateException, ItemStateException {

        ItemState state = transientMap.get(id);
        if (state != null) {
            return state;
        } else {
            throw new NoSuchItemStateException(id.toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasItemState(ItemId id) {
        return transientMap.contains(id);
    }

    /**
     * {@inheritDoc}
     */
    public NodeReferences getNodeReferences(NodeReferencesId id)
            throws NoSuchItemStateException, ItemStateException {

        throw new ItemStateException("getNodeReferences() not implemented");
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasNodeReferences(NodeReferencesId id) {
        return false;
    }

    //------------------< methods for listing & querying state of cache/attic >

    /**
     * @return
     */
    boolean hasAnyItemStates() {
        return !transientMap.isEmpty();
    }

    /**
     * @return
     */
    boolean hasAnyItemStatesInAttic() {
        return !atticMap.isEmpty();
    }

    /**
     * @return
     */
    int getEntriesCount() {
        return transientMap.size();
    }

    /**
     * @return
     */
    int getEntriesInAtticCount() {
        return atticMap.size();
    }

    /**
     * @return
     */
    Iterator getEntries() {
        return transientMap.values().iterator();
    }

    /**
     * @return
     */
    Iterator getEntriesInAttic() {
        return atticMap.values().iterator();
    }

    //----------------< methods for creating & discarding ItemState instances >
    /**
     * @param uuid
     * @param nodeTypeName
     * @param parentUUID
     * @param initialStatus
     * @return
     * @throws ItemStateException
     */
    NodeState createNodeState(String uuid, QName nodeTypeName,
                              String parentUUID, int initialStatus)
            throws ItemStateException {

        NodeId id = new NodeId(uuid);

        // check map; synchronized to ensure an entry is not created twice.
        synchronized (transientMap) {
            if (transientMap.contains(id)) {
                String msg = "there's already a node state instance with id " + id;
                log.debug(msg);
                throw new ItemStateException(msg);
            }

            NodeState state = new NodeState(uuid, nodeTypeName, parentUUID,
                    initialStatus, true);
            // put transient state in the map
            transientMap.put(state);
            return state;
        }
    }

    /**
     * @param overlayedState
     * @param initialStatus
     * @return
     * @throws ItemStateException
     */
    NodeState createNodeState(NodeState overlayedState, int initialStatus)
            throws ItemStateException {

        ItemId id = overlayedState.getId();

        // check map; synchronized to ensure an entry is not created twice.
        synchronized (transientMap) {
            if (transientMap.contains(id)) {
                String msg = "there's already a node state instance with id " + id;
                log.debug(msg);
                throw new ItemStateException(msg);
            }

            NodeState state = new NodeState(overlayedState, initialStatus, true);
            // put transient state in the map
            transientMap.put(state);
            return state;
        }
    }

    /**
     * @param parentUUID
     * @param propName
     * @param initialStatus
     * @return
     * @throws ItemStateException
     */
    PropertyState createPropertyState(String parentUUID, QName propName, int initialStatus)
            throws ItemStateException {

        PropertyId id = new PropertyId(parentUUID, propName);

        // check map; synchronized to ensure an entry is not created twice.
        synchronized (transientMap) {
            if (transientMap.contains(id)) {
                String msg = "there's already a property state instance with id " + id;
                log.debug(msg);
                throw new ItemStateException(msg);
            }

            PropertyState state = new PropertyState(propName, parentUUID, initialStatus, true);
            // put transient state in the map
            transientMap.put(state);
            return state;
        }
    }

    /**
     * @param overlayedState
     * @param initialStatus
     * @return
     * @throws ItemStateException
     */
    PropertyState createPropertyState(PropertyState overlayedState, int initialStatus)
            throws ItemStateException {

        PropertyId id = new PropertyId(overlayedState.getParentUUID(),
                overlayedState.getName());

        // check map; synchronized to ensure an entry is not created twice.
        synchronized (transientMap) {
            if (transientMap.contains(id)) {
                String msg = "there's already a property state instance with id " + id;
                log.debug(msg);
                throw new ItemStateException(msg);
            }

            PropertyState state = new PropertyState(overlayedState, initialStatus, true);
            // put transient state in the map
            transientMap.put(state);
            return state;
        }
    }

    /**
     * Disposes the specified instance, i.e. discards it and removes it from
     * the map.
     *
     * @param state the <code>ItemState</code> instance that should be disposed
     * @see ItemState#discard()
     */
    void disposeItemState(ItemState state) {
        // discard item state, this will invalidate the wrapping Item
        // instance of the transient state
        state.discard();
        // remove from map
        transientMap.remove(state.getId());
        // give the instance a chance to prepare to get gc'ed
        state.onDisposed();
    }

    /**
     * Transfers the specified instance from the 'active' map to the attic.
     *
     * @param state the <code>ItemState</code> instance that should be moved to
     *              the attic
     */
    void moveItemStateToAttic(ItemState state) {
        // remove from map
        transientMap.remove(state.getId());
        // add to attic
        atticMap.put(state);
    }

    /**
     * Disposes the specified instance in the attic, i.e. discards it and
     * removes it from the attic.
     *
     * @param state the <code>ItemState</code> instance that should be disposed
     * @see ItemState#discard()
     */
    void disposeItemStateInAttic(ItemState state) {
        // discard item state, this will invalidate the wrapping Item
        // instance of the transient state
        state.discard();
        // remove from attic
        atticMap.remove(state.getId());
        // give the instance a chance to prepare to get gc'ed
        state.onDisposed();
    }

    /**
     * Disposes all transient item states in the cache and in the attic.
     */
    void disposeAllItemStates() {
        // dispose item states in transient map & attic
        // (use temp collection to avoid ConcurrentModificationException)
        Collection tmp = new ArrayList(transientMap.values());
        Iterator iter = tmp.iterator();
        while (iter.hasNext()) {
            ItemState state = (ItemState) iter.next();
            disposeItemState(state);
        }
        tmp = new ArrayList(atticMap.values());
        iter = tmp.iterator();
        while (iter.hasNext()) {
            ItemState state = (ItemState) iter.next();
            disposeItemStateInAttic(state);
        }
    }

    /**
     * Return the attic item state provider that holds all items
     * moved into the attic.
     *
     * @return attic
     */
    ItemStateManager getAttic() {
        if (attic == null) {
            attic = new AtticItemStateManager();
        }
        return attic;
    }

    //--------------------------------------------------------< inner classes >
    /**
     * ItemStateManager view of the states in the attic
     *
     * @see TransientItemStateManager#getAttic
     */
    private class AtticItemStateManager implements ItemStateManager {

        AtticItemStateManager() {
        }

        /**
         * {@inheritDoc}
         */
        public ItemState getItemState(ItemId id)
                throws NoSuchItemStateException, ItemStateException {

            ItemState state = atticMap.get(id);
            if (state != null) {
                return state;
            } else {
                throw new NoSuchItemStateException(id.toString());
            }
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasItemState(ItemId id) {
            return atticMap.contains(id);
        }

        /**
         * {@inheritDoc}
         */
        public NodeReferences getNodeReferences(NodeReferencesId id)
                throws NoSuchItemStateException, ItemStateException {
            // n/a
            throw new ItemStateException("getNodeReferences() not implemented");
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasNodeReferences(NodeReferencesId id) {
            // n/a
            return false;
        }
    }
}
