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
package org.apache.jackrabbit.core.state;

import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.util.Dumpable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A {@link java.util.Map} based <code>ItemStateStore</code> implementation.
 */
public class ItemStateMap implements ItemStateStore, Dumpable {

    /** Logger instance */
    private static Logger log = LoggerFactory.getLogger(ItemStateMap.class);

    /**
     * the map backing this <code>ItemStateStore</code> implementation
     */
    protected final Map map;

    /**
     * Creates a new HashMap-backed <code>ItemStateStore</code> implementation.
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

    //-------------------------------------------------------< ItemStateStore >
    /**
     * {@inheritDoc}
     */
    public boolean contains(ItemId id) {
        return map.containsKey(id);
    }

    /**
     * {@inheritDoc}
     */
    public ItemState get(ItemId id) {
        return (ItemState) map.get(id);
    }

    /**
     * {@inheritDoc}
     */
    public void put(ItemState state) {
        ItemId id = state.getId();
        if (map.containsKey(id)) {
            log.warn("overwriting map entry " + id);
        }
        map.put(id, state);
    }

    /**
     * {@inheritDoc}
     */
    public void remove(ItemId id) {
        map.remove(id);
    }

    /**
     * {@inheritDoc}
     */
    public void clear() {
        map.clear();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        return map.size();
    }

    /**
     * {@inheritDoc}
     */
    public Set keySet() {
        return Collections.unmodifiableSet(map.keySet());
    }

    /**
     * {@inheritDoc}
     */
    public Collection values() {
        return Collections.unmodifiableCollection(map.values());
    }

    //-------------------------------------------------------------< Dumpable >
    /**
     * {@inheritDoc}
     */
    public void dump(PrintStream ps) {
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
