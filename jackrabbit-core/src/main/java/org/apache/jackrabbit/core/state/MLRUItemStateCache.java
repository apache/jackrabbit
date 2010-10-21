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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.LinkedMap;
import org.apache.jackrabbit.core.cache.Cache;
import org.apache.jackrabbit.core.cache.CacheAccessListener;
import org.apache.jackrabbit.core.id.ItemId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An <code>ItemStateCache</code> implementation that internally uses a
 * {@link LinkedMap} to maintain a cache of <code>ItemState</code> objects. The
 * cache uses a rough estimate of the memory consumption of the cached item
 * states for calculating the maximum number of entries. The oldest entries
 * are flushed once the cache size has exceeded a certain limit.
 * <p/>
 * TODO rename class to something more appropriate, e.g. FIFOItemSateCache since
 * it doesn't use a LRU eviction policy anymore.
 */
public class MLRUItemStateCache implements ItemStateCache, Cache {
    /** Logger instance */
    private static Logger log = LoggerFactory.getLogger(MLRUItemStateCache.class);

    /** default maximum memory to use */
    public static final int DEFAULT_MAX_MEM = 4 * 1024 * 1024;

    /** the amount of memory the entries use */
    private volatile long totalMem;

    /** the maximum of memory the cache may use */
    private volatile long maxMem;

    /** the number of writes */
    private volatile long numWrites;

    /** the access count */
    private volatile long accessCount;

    /** the cache access listeners */
    private CacheAccessListener accessListener;

    /**
     * A cache for <code>ItemState</code> instances
     */
    private final Map<ItemId, Entry> cache;

    /**
     * Constructs a new, empty <code>ItemStateCache</code> with a maximum amount
     * of memory of {@link #DEFAULT_MAX_MEM}.
     */
    public MLRUItemStateCache() {
        this(DEFAULT_MAX_MEM);
    }

    /**
     * Constructs a new, empty <code>ItemStateCache</code> with the specified
     * maximum memory.
     *
     * @param maxMem the maximum amount of memory this cache may use.
     */
    @SuppressWarnings("serial")
    private MLRUItemStateCache(int maxMem) {
        this.maxMem = maxMem;
        this.cache = new LinkedHashMap<ItemId, MLRUItemStateCache.Entry>(
                maxMem / 1024, 0.75f, true /* access-ordered */) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<ItemId, Entry> e) {
                long maxMem = MLRUItemStateCache.this.maxMem;
                if (totalMem <= maxMem) {
                    return false;
                } else if (totalMem - e.getValue().size <= maxMem) {
                    totalMem -= e.getValue().size;
                    return true;
                } else {
                    shrink();
                    return false;
                }
            }
        };
    }

    //-------------------------------------------------------< ItemStateCache >
    /**
     * {@inheritDoc}
     */
    public boolean isCached(ItemId id) {
        synchronized (cache) {
            return cache.containsKey(id);
        }
    }

    /**
     * {@inheritDoc}
     */
    public ItemState retrieve(ItemId id) {
        touch();
        synchronized (cache) {
            Entry entry = cache.get(id);
            if (entry != null) {
                return entry.state;
            } else {
                return null;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public ItemState[] retrieveAll() {
        synchronized (cache) {
            ItemState[] states = new ItemState[cache.size()];
            int i = 0;
            for (Entry entry : cache.values()) {
                states[i++] = entry.state;
            }
            return states;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void update(ItemId id) {
        touch();
        synchronized (cache) {
            Entry entry = cache.get(id);
            if (entry != null) {
                totalMem -= entry.size;
                entry.recalc();
                totalMem += entry.size;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void cache(ItemState state) {
        touch();
        synchronized (cache) {
            ItemId id = state.getId();
            if (cache.containsKey(id)) {
                log.warn("overwriting cached entry " + id);
                evict(id);
            }
            Entry entry = new Entry(state);
            totalMem += entry.size;
            cache.put(id, entry);
            if (numWrites++ % 10000 == 0 && log.isDebugEnabled()) {
                log.debug(this + " size=" + cache.size() + ", " + totalMem + "/" + maxMem);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void evict(ItemId id) {
        touch();
        synchronized (cache) {
            Entry entry = cache.remove(id);
            if (entry != null) {
                totalMem -= entry.size;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void evictAll() {
        synchronized (cache) {
            cache.clear();
            totalMem = 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        synchronized (cache) {
            return cache.isEmpty();
        }
    }

    private void touch() {
        accessCount++;
        if ((accessCount % CacheAccessListener.ACCESS_INTERVAL) == 0) {
            if (accessListener != null) {
                accessListener.cacheAccessed();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getAccessCount() {
        return accessCount;
    }

    /**
     * {@inheritDoc}
     */
    public long getMaxMemorySize() {
        return maxMem;
    }

    /**
     * {@inheritDoc}
     */
    public long getMemoryUsed() {
        return totalMem;
    }

    /**
     * {@inheritDoc}
     */
    public void resetAccessCount() {
        synchronized (cache) {
            accessCount = 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setMaxMemorySize(long size) {
        synchronized (cache) {
            this.maxMem = size;

            // remove items, if too many
            if (totalMem > maxMem) {
                shrink();
            }
        }
    }

    private void shrink() {
        List<Map.Entry<ItemId, Entry>> list =
            new ArrayList<Map.Entry<ItemId, Entry>>(cache.entrySet());
        for (int i = list.size() - 1; totalMem > maxMem && i >= 0; i--) {
            Map.Entry<ItemId, Entry> last = list.get(i);
            totalMem -= last.getValue().size;
            cache.remove(last.getKey());
        }
    }

    /**
     * Set the cache access listener. Only one listener per cache is supported.
     *
     * @param listener the new listener
     */
    public void setAccessListener(CacheAccessListener listener) {
        this.accessListener = listener;
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        synchronized (cache) {
            if (accessListener != null) {
                accessListener.disposeCache(this);
            }
        }
    }


    /**
     * Internal cache entry.
     */
    private static class Entry {

        private final ItemState state;

        private long size;

        public Entry(ItemState state) {
            this.state = state;
            this.size = 64 + state.calculateMemoryFootprint();
        }

        public void recalc() {
            size = 64 + state.calculateMemoryFootprint();
        }
    }

}
