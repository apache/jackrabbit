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
import org.apache.log4j.Logger;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * An <code>ItemStateMap</code> stores <code>ItemState</code> instances using
 * their <code>ItemId</code>s as key.
 */
public class ItemStateMap {
    private static Logger log = Logger.getLogger(ItemStateMap.class);

    /**
     * the map backing this <code>ItemStateMap</code> instance
     */
    protected final Map map;

    /**
     * Creates a new HashMap-backed <code>ItemStateMap</code> instance.
     */
    public ItemStateMap() {
        this(new HashMap());
    }

    /**
     * Protected constructor for specialized subclasses
     *
     * @param map <code>Map</code> implementation to be used as backing store.
     */
    protected ItemStateMap(Map map) {
        this.map = map;
    }

    //-------------------------------------------------------< public methods >
    /**
     * Returns <code>true</code> if this map contains an <code>ItemState</code>
     * object with the specified <code>id</code>.
     *
     * @param id id of <code>ItemState</code> object whose presence should be
     *           tested.
     * @return <code>true</code> if there's a corresponding map entry,
     *         otherwise <code>false</code>.
     */
    public boolean contains(ItemId id) {
        return map.containsKey(id);
    }

    /**
     * Returns the <code>ItemState</code> object with the specified
     * <code>id</code> if it is present or <code>null</code> if no entry exists
     * with that <code>id</code>.
     *
     * @param id the id of the <code>ItemState</code> object to be returned.
     * @return the <code>ItemState</code> object with the specified
     *         <code>id</code> or or <code>null</code> if no entry exists
     *         with that <code>id</code>
     */
    public ItemState get(ItemId id) {
        return (ItemState) map.get(id);
    }

    /**
     * Stores the specified <code>ItemState</code> object in the map
     * using its <code>ItemId</code> as the key.
     *
     * @param state the <code>ItemState</code> object to store
     */
    public void put(ItemState state) {
        ItemId id = state.getId();
        if (map.containsKey(id)) {
            log.warn("overwriting map entry " + id);
        }
        map.put(id, state);
    }

    /**
     * Removes the <code>ItemState</code> object with the specified id from
     * this map if it is present.
     *
     * @param id the id of the <code>ItemState</code> object which should be
     *           removed from this map.
     */
    public void remove(ItemId id) {
        map.remove(id);
    }

    /**
     * Removes all entries from this map.
     */
    public void clear() {
        map.clear();
    }

    /**
     * Returns <code>true</code> if the map contains no entries.
     *
     * @return <code>true</code> if the map contains no entries.
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Returns the number of entries in the map.
     *
     * @return number of entries in the map.
     */
    public int size() {
        return map.size();
    }

    /**
     * Returns an unmodifiable set view of the keys (i.e. <code>ItemId</code>
     * objects) contained in this map.
     *
     * @return a set view of the keys contained in this map.
     */
    public Set keySet() {
        return Collections.unmodifiableSet(map.keySet());
    }

    /**
     * Returns an unmodifiable collection view of the values (i.e.
     * <code>ItemState</code> objects) contained in this map.
     *
     * @return a collection view of the values contained in this map.
     */
    public Collection values() {
        return Collections.unmodifiableCollection(map.values());
    }

    //-------------------------------------------------------< implementation >
    /**
     * Dumps the state of this <code>ItemStateMap</code> instance
     * (used for diagnostic purposes).
     *
     * @param ps
     */
    protected void dump(PrintStream ps) {
        ps.println("map entries:");
        ps.println();
        Iterator iter = keySet().iterator();
        while (iter.hasNext()) {
            ItemId id = (ItemId) iter.next();
            ItemState state = get(id);
            dumpItemState(id, state, ps);
        }
    }

    private void dumpItemState(ItemId id, ItemState state, PrintStream ps) {
        ps.print(state.isNode() ? "Node: " : "Prop: ");
        switch (state.getStatus()) {
            case ItemState.STATUS_EXISTING:
                ps.print("[existing]           ");
                break;
            case ItemState.STATUS_EXISTING_MODIFIED:
                ps.print("[existing, modified] ");
                break;
            case ItemState.STATUS_EXISTING_REMOVED:
                ps.print("[existing, removed]  ");
                break;
            case ItemState.STATUS_NEW:
                ps.print("[new]                ");
                break;
            case ItemState.STATUS_STALE_DESTROYED:
                ps.print("[stale, destroyed]   ");
                break;
            case ItemState.STATUS_STALE_MODIFIED:
                ps.print("[stale, modified]    ");
                break;
            case ItemState.STATUS_UNDEFINED:
                ps.print("[undefined]          ");
                break;
        }
        ps.println(id + " (" + state + ")");
    }
}
