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
import org.apache.jackrabbit.core.util.Dumpable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;

/**
 * <code>ItemStateReferenceCache</code> internally consists of 2 components:
 * <ul>
 * <li>an <code>ItemStateReferenceMap<code> serving as the primary (or main)
 * cache; it holds weak references to <code>ItemState</code> instances. This
 * <code>ItemStateCache</code> implementation directly represents the
 * contents of the primary cache, i.e. {@link #isCached(ItemId)},
 * {@link #retrieve(ItemId)}}, {@link #isEmpty()} etc. only refer to the contents
 * of the primary cache.</li>
 * <li>an <code>ItemStateCache</code> implementing a custom eviction policy and
 * serving as the secondary (or auxiliary) cache; entries that are automatically
 * flushed from this secondary cache through its eviction policy (LRU, etc.)
 * will be indirectly flushed from the primary (reference) cache by the garbage
 * collector if they are thus rendered weakly reachable.
 * </li>
 * </ul>
 * This implementation of ItemStateCache is thread-safe.
 */
public class ItemStateReferenceCache implements ItemStateCache, Dumpable {

    /** Logger instance */
    private static Logger log = LoggerFactory.getLogger(ItemStateReferenceCache.class);

    /**
     * primary cache storing weak references to <code>ItemState</code>
     * instances.
     */
    private final ItemStateReferenceMap refs;
    /**
     * secondary cache that automatically flushes entries based on some
     * eviction policy; entries flushed from the secondary cache will be
     * indirectly flushed from the primary (reference) cache by the garbage
     * collector if they thus are rendered weakly reachable.
     */
    private final ItemStateCache cache;

    /**
     * Creates a new <code>ItemStateReferenceCache</code> that uses a
     * <code>MLRUItemStateCache</code> instance as internal secondary
     * cache.
     */
    public ItemStateReferenceCache(ItemStateCacheFactory cacheFactory) {
        this(cacheFactory.newItemStateCache());
    }

    /**
     * Creates a new <code>ItemStateReferenceCache</code> that uses the
     * specified <code>ItemStateCache</code> instance as internal secondary
     * cache.
     *
     * @param cache secondary cache implementing a custom eviction policy
     */
    public ItemStateReferenceCache(ItemStateCache cache) {
        this.cache = cache;
        refs = new ItemStateReferenceMap();
    }

    //-------------------------------------------------------< ItemStateCache >
    /**
     * {@inheritDoc}
     */
    public synchronized boolean isCached(ItemId id) {
        // check primary cache
        return refs.contains(id);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized ItemState retrieve(ItemId id) {
        // fake call to update stats of secondary cache
        cache.retrieve(id);

        // retrieve from primary cache
        return refs.get(id);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized ItemState[] retrieveAll() {
        // values of primary cache
        return (ItemState[]) refs.values().toArray(new ItemState[refs.size()]);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void cache(ItemState state) {
        ItemId id = state.getId();
        if (refs.contains(id)) {
            log.warn("overwriting cached entry " + id);
        }
        // fake call to update stats of secondary cache
        cache.cache(state);
        // store weak reference in primary cache
        refs.put(state);

    }

    /**
     * {@inheritDoc}
     */
    public synchronized void evict(ItemId id) {
        // fake call to update stats of secondary cache
        cache.evict(id);
        // remove from primary cache
        refs.remove(id);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void dispose() {
        cache.dispose();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void evictAll() {
        // fake call to update stats of secondary cache
        cache.evictAll();
        // remove all weak references from primary cache
        refs.clear();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void update(ItemId id) {
        // delegate
        cache.update(id);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized boolean isEmpty() {
        // check primary cache
        return refs.isEmpty();
    }

    //-------------------------------------------------------------< Dumpable >
    /**
     * {@inheritDoc}
     */
    public synchronized void dump(PrintStream ps) {
        ps.println("ItemStateReferenceCache (" + this + ")");
        ps.println();
        ps.print("[refs] ");
        refs.dump(ps);
    }
}
