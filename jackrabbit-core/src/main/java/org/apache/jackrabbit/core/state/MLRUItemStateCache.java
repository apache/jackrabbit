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

import java.util.List;

import org.apache.jackrabbit.core.cache.CacheManager;
import org.apache.jackrabbit.core.cache.ConcurrentCache;
import org.apache.jackrabbit.core.id.ItemId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An <code>ItemStateCache</code> implementation that internally uses a
 * {@link ConcurrentCache} to maintain a cache of <code>ItemState</code> objects. The
 * cache uses a rough estimate of the memory consumption of the cached item
 * states for calculating the maximum number of entries. The oldest entries
 * are flushed once the cache size has exceeded a certain limit.
 * <p>
 * TODO rename class to something more appropriate, e.g. FIFOItemSateCache since
 * it doesn't use a LRU eviction policy anymore.
 */
public class MLRUItemStateCache implements ItemStateCache {

    /** Logger instance */
    private static Logger log = LoggerFactory.getLogger(MLRUItemStateCache.class);

    /** default maximum memory to use */
    public static final int DEFAULT_MAX_MEM = 4 * 1024 * 1024;

    /** the number of writes */
    private volatile long numWrites = 0;

    private final ConcurrentCache<ItemId, ItemState> cache =
        new ConcurrentCache<ItemId, ItemState>(MLRUItemStateCache.class.getSimpleName());

    public MLRUItemStateCache(CacheManager cacheMgr) {
        cache.setMaxMemorySize(DEFAULT_MAX_MEM);
        cache.setAccessListener(cacheMgr);
        cacheMgr.add(cache);
    }

    //-------------------------------------------------------< ItemStateCache >

    /**
     * {@inheritDoc}
     */
    public boolean isCached(ItemId id) {
        return cache.containsKey(id);
    }

    /**
     * {@inheritDoc}
     */
    public ItemState retrieve(ItemId id) {
        return cache.get(id);
    }

    /**
     * {@inheritDoc}
     */
    public ItemState[] retrieveAll() {
        List<ItemState> values = cache.values();
        return values.toArray(new ItemState[values.size()]);
    }

    /**
     * {@inheritDoc}
     */
    public void cache(ItemState state) {
        cache.put(state.getId(), state, state.calculateMemoryFootprint());

        if (numWrites++ % 10000 == 0 && log.isDebugEnabled()) {
            log.debug("Item state cache size: {}% of {} bytes",
                    cache.getMemoryUsed() * 100 / cache.getMaxMemorySize(),
                    cache.getMaxMemorySize());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void evict(ItemId id) {
        cache.remove(id);
    }

    /**
     * {@inheritDoc}
     */
    public void evictAll() {
        cache.clear();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        return cache.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        cache.dispose();
    }

}
