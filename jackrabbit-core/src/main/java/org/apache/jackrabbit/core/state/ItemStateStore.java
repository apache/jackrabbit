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

import org.apache.jackrabbit.core.id.ItemId;

import java.util.Set;
import java.util.Collection;

/**
 * <code>ItemStateStore</code> is similar to a typed <code>Map</code>:
 * <p/>
 * An <code>ItemStateStore</code> temporarily stores and retrieves
 * <code>ItemState</code> instances using their <code>ItemId</code>s as key.
 */
public interface ItemStateStore {
    /**
     * Returns <code>true</code> if this store contains an <code>ItemState</code>
     * object with the specified <code>id</code>.
     *
     * @param id id of <code>ItemState</code> object whose presence should be
     *           tested.
     * @return <code>true</code> if this store contains a corresponding entry,
     *         otherwise <code>false</code>.
     */
    boolean contains(ItemId id);

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
    ItemState get(ItemId id);

    /**
     * Stores the specified <code>ItemState</code> object in the store
     * using its <code>ItemId</code> as the key.
     *
     * @param state the <code>ItemState</code> object to store
     */
    void put(ItemState state);

    /**
     * Removes the <code>ItemState</code> object with the specified id from
     * this store if it is present.
     *
     * @param id the id of the <code>ItemState</code> object which should be
     *           removed from this store.
     */
    void remove(ItemId id);

    /**
     * Removes all entries from this store.
     */
    void clear();

    /**
     * Returns <code>true</code> if this store contains no entries.
     *
     * @return <code>true</code> if this store contains no entries.
     */
    boolean isEmpty();

    /**
     * Returns the number of entries in this store.
     *
     * @return number of entries in this store.
     */
    int size();

    /**
     * Returns an unmodifiable set view of the keys (i.e. <code>ItemId</code>
     * objects) contained in this store.
     *
     * @return a set view of the keys contained in this store.
     */
    Set<ItemId> keySet();

    /**
     * Returns an unmodifiable collection view of the values (i.e.
     * <code>ItemState</code> objects) contained in this store.
     *
     * @return a collection view of the values contained in this store.
     */
    Collection<ItemState> values();
}
