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
package org.apache.jackrabbit.core.state;

import org.apache.commons.collections.ReferenceMap;
import org.apache.jackrabbit.core.ItemId;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * An <code>ItemStateCache</code> maintains a cache of <code>ItemState</code>
 * instances.
 */
abstract class ItemStateCache {
    private static Logger log = Logger.getLogger(ItemStateCache.class);

    /**
     * A cache for <code>ItemState</code> instances
     */
    private final Map cache;

    /**
     * Creates a new <code>ItemStateCache</code> that will use instance
     * hard references to keys and soft references to values.
     */
    protected ItemStateCache() {
        // setup cache with soft references to ItemState instances
        this(ReferenceMap.HARD, ReferenceMap.SOFT);
    }

    /**
     * Creates a new <code>ItemStateCache</code> instance that will use the
     * specified types of references.
     *
     * @param keyType   the type of reference to use for keys (i.e. the item paths)
     * @param valueType the type of reference to use for values (i.e. the item-datas)
     * @see org.apache.commons.collections.ReferenceMap#HARD
     * @see org.apache.commons.collections.ReferenceMap#SOFT
     * @see org.apache.commons.collections.ReferenceMap#WEAK
     */
    protected ItemStateCache(int keyType, int valueType) {
        cache = new ReferenceMap(keyType, valueType);
    }

    /**
     * Checks if there's already a corresponding cache entry for
     * <code>id</code>.
     *
     * @param id id of a <code>ItemState</code> object
     * @return true if there's a corresponding cache entry, otherwise false.
     */
    protected boolean isCached(ItemId id) {
        return cache.containsKey(id);
    }

    /**
     * Returns an item-state reference from the cache.
     *
     * @param id the id of the <code>ItemState</code> object whose
     *           reference should be retrieved from the cache.
     * @return the <code>ItemState</code> reference stored in the corresponding
     *         cache entry or <code>null</code> if there's no corresponding
     *         cache entry.
     */
    protected ItemState retrieve(ItemId id) {
        return (ItemState) cache.get(id);
    }

    /**
     * Puts the reference to an <code>ItemState</code> object in the cache using its path as the key.
     *
     * @param state the <code>ItemState</code> object to cache
     */
    protected void cache(ItemState state) {
        ItemId id = state.getId();
        if (cache.containsKey(id)) {
            log.warn("overwriting cached entry " + id);
        }
        log.debug("caching " + id);
        cache.put(id, state);
    }

    /**
     * Removes a cache entry for a specific id.
     *
     * @param id the id of the <code>ItemState</code> object whose
     *           reference should be removed from the cache.
     */
    protected void evict(ItemId id) {
        log.debug("removing entry " + id + " from cache");
        cache.remove(id);
    }

    /**
     * Clears all entries from the cache.
     */
    protected void evictAll() {
        log.debug("removing all entries from cache");
        cache.clear();
    }

    /**
     * Returns <code>true</code> if the cache contains no entries.
     *
     * @return <code>true</code> if the cache contains no entries.
     */
    protected boolean isEmpty() {
        return cache.isEmpty();
    }

    /**
     * Returns the number of entries in the cache.
     *
     * @return number of entries in the cache.
     */
    protected int size() {
        return cache.size();
    }

    /**
     * Returns an iterator of the keys (i.e. <code>ItemId</code> objects)
     * of the cached entries.
     *
     * @return an iterator of the keys of the cached entries.
     */
    protected Iterator keys() {
        // use temp collection to avoid ConcurrentModificationException
        Collection tmp = new ArrayList(cache.keySet());
        return tmp.iterator();
    }

    /**
     * Returns an iterator of the entries (i.e. <code>ItemState</code> objects)
     * in the cache.
     *
     * @return an iterator of the entries in the cache.
     */
    protected Iterator entries() {
        // use temp collection to avoid ConcurrentModificationException
        Collection tmp = new ArrayList(cache.values());
        return tmp.iterator();
    }
}
