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
package org.apache.jackrabbit.core.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class manages the size of the caches used in Jackrabbit. The combined
 * size of all caches must be limited to avoid out of memory problems. The
 * available memory is dynamically distributed across the caches each second.
 * This class tries to calculates the best cache sizes by comparing the access
 * counts of each cache, and the used memory. The idea is, the more a cache is
 * accessed, the more memory it should get, while the cache should not shrink
 * too quickly. A minimum and maximum size per cache is defined as well. After
 * distributing the memory in this way, there might be some unused memory (if
 * one or more caches did not use some of the allocated memory). This unused
 * memory is distributed evenly across the full caches.
 */
public class CacheManager implements CacheAccessListener {

    /** The logger instance. */
    private static Logger log = LoggerFactory.getLogger(CacheManager.class);

    /** The default maximum amount of memory to distribute across the caches. */
    private static final long DEFAULT_MAX_MEMORY = 16 * 1024 * 1024;

    /** The default minimum size of a cache. */
    private static final long DEFAULT_MIN_MEMORY_PER_CACHE = 128 * 1024;

    /** The default maximum memory per cache. */
    private static final long DEFAULT_MAX_MEMORY_PER_CACHE = 4 * 1024 * 1024;

    /** The set of caches (weakly referenced). */
    private WeakHashMap<Cache, Object> caches = new WeakHashMap<Cache, Object>();

    /** The default minimum resize interval (in ms). */
    private static final int DEFAULT_MIN_RESIZE_INTERVAL = 1000;

    /** The default minimum stats logging interval (in ms). */
    private static final int DEFAULT_LOG_STATS_INTERVAL = 60 * 1000;

    /** The size of a big object, to detect if a cache is full or not. */
    private static final int BIG_OBJECT_SIZE = 16 * 1024;

    /** The amount of memory to distribute across the caches. */
    private long maxMemory = Long.getLong(
            "org.apache.jackrabbit.maxCacheMemory",
            DEFAULT_MAX_MEMORY);

    /** The minimum size of a cache. */
    private long minMemoryPerCache = Long.getLong(
            "org.apache.jackrabbit.minMemoryPerCache",
            DEFAULT_MIN_MEMORY_PER_CACHE);

    /** The maximum memory per cache (unless, there is some unused memory). */
    private long maxMemoryPerCache = Long.getLong(
            "org.apache.jackrabbit.maxMemoryPerCache",
            DEFAULT_MAX_MEMORY_PER_CACHE);

    /** The minimum resize interval time */
    private long minResizeInterval = Long.getLong(
            "org.apache.jackrabbit.cacheResizeInterval",
            DEFAULT_MIN_RESIZE_INTERVAL);

    /** The minimum interval time between stats are logged */
    private long minLogStatsInterval = Long.getLong(
            "org.apache.jackrabbit.cacheLogStatsInterval",
            DEFAULT_LOG_STATS_INTERVAL);

    /** The last time the caches where resized. */
    private volatile long nextResize =
        System.currentTimeMillis() + DEFAULT_MIN_RESIZE_INTERVAL;


    /** The last time the cache stats were logged. */
    private volatile long nextLogStats =
            System.currentTimeMillis() + DEFAULT_LOG_STATS_INTERVAL;


    public long getMaxMemory() {
        return maxMemory;
    }

    public void setMaxMemory(final long maxMemory) {
        this.maxMemory = maxMemory;
    }

    public long getMaxMemoryPerCache() {
        return maxMemoryPerCache;
    }

    public void setMaxMemoryPerCache(final long maxMemoryPerCache) {
        this.maxMemoryPerCache = maxMemoryPerCache;
    }

    public long getMinMemoryPerCache() {
        return minMemoryPerCache;
    }

    public void setMinMemoryPerCache(final long minMemoryPerCache) {
        this.minMemoryPerCache = minMemoryPerCache;
    }

    public long getMinResizeInterval() {
        return minResizeInterval;
    }

    public void setMinResizeInterval(long minResizeInterval) {
        this.minResizeInterval = minResizeInterval;
    }

    /**
     * After one of the caches is accessed a number of times, this method is called.
     * Resize the caches if required.
     */
    public void cacheAccessed(long accessCount) {

        logCacheStats();

        long now = System.currentTimeMillis();
        if (now < nextResize) {
            return;
        }
        synchronized (this) {
            // the previous test was not synchronized (for speed)
            // so we need another synchronized test
            if (now < nextResize) {
                return;
            }
            nextResize = now + minResizeInterval;
            resizeAll();
            nextResize = System.currentTimeMillis() + minResizeInterval;
        }
    }

    /**
     * Log info about the caches.
     */
    private void logCacheStats() {
        if (log.isDebugEnabled()) {
            long now = System.currentTimeMillis();
            if (now < nextLogStats) {
                return;
            }
            // JCR-3194 avoid ConcurrentModificationException
            List<Cache> list = new ArrayList<Cache>();
            synchronized (caches) {
                list.addAll(caches.keySet());
            }
            for (Cache cache : list) {
                log.debug(cache.getCacheInfoAsString());
            }
            nextLogStats = now + minLogStatsInterval;
        }
    }

    /**
     * Re-calculate the maximum memory for each cache, and set the new limits.
     */
    private void resizeAll() {
        if (log.isTraceEnabled()) {
            log.trace("resizeAll size=" + caches.size());
        }
        // get strong references
        // entries in a weak hash map may disappear any time
        // so can't use size() / keySet() directly
        // only using the iterator guarantees that we don't get null references
        List<Cache> list = new ArrayList<Cache>();
        synchronized (caches) {
            list.addAll(caches.keySet());
        }
        if (list.size() == 0) {
            // nothing to do
            return;
        }
        CacheInfo[] infos = new CacheInfo[list.size()];
        for (int i = 0; i < list.size(); i++) {
            infos[i] = new CacheInfo((Cache) list.get(i));
        }
        // calculate the total access count and memory used
        long totalAccessCount = 0;
        long totalMemoryUsed = 0;
        for (CacheInfo info : infos) {
            totalAccessCount += info.getAccessCount();
            totalMemoryUsed += info.getMemoryUsed();
        }
        // try to distribute the memory based on the access count
        // and memory used (higher numbers - more memory)
        // and find out how many caches are full
        // 50% is distributed according to access count,
        // and 50% according to memory used
        double memoryPerAccess = (double) maxMemory / 2.
                / Math.max(1., (double) totalAccessCount);
        double memoryPerUsed = (double) maxMemory / 2.
                / Math.max(1., (double) totalMemoryUsed);
        int fullCacheCount = 0;
        for (CacheInfo info : infos) {
            long mem = (long) (memoryPerAccess * info.getAccessCount());
            mem += (long) (memoryPerUsed * info.getMemoryUsed());
            mem = Math.min(mem, maxMemoryPerCache);
            if (info.wasFull()) {
                fullCacheCount++;
            } else {
                mem = Math.min(mem, info.getMemoryUsed());
            }
            mem = Math.min(mem, maxMemoryPerCache);
            mem = Math.max(mem, minMemoryPerCache);
            info.setMemory(mem);
        }
        // calculate the unused memory
        long unusedMemory = maxMemory;
        for (CacheInfo info : infos) {
            unusedMemory -= info.getMemory();
        }
        // distribute the remaining memory evenly across the full caches
        if (unusedMemory > 0 && fullCacheCount > 0) {
            for (CacheInfo info : infos) {
                if (info.wasFull()) {
                    info.setMemory(info.getMemory() + unusedMemory
                            / fullCacheCount);
                }
            }
        }
        // set the new limit
        for (CacheInfo info : infos) {
            Cache cache = info.getCache();
            if (log.isTraceEnabled()) {
                log.trace(cache + " now:" + cache.getMaxMemorySize() + " used:"
                        + info.getMemoryUsed() + " access:" + info.getAccessCount()
                        + " new:" + info.getMemory());
            }
            cache.setMaxMemorySize(info.getMemory());
        }
    }

    /**
     * Add a new cache to the list.
     * This call does not trigger recalculating the cache sizes.
     *
     * @param cache the cache to add
     */
    public void add(Cache cache) {
        synchronized (caches) {
            caches.put(cache, null);
        }
    }

    /**
     * Remove a cache. As this class only has a weak reference to each cache,
     * calling this method is not strictly required.
     * This call does not trigger recalculating the cache sizes.
     *
     * @param cache
     *            the cache to remove
     */
    public void remove(Cache cache) {
        synchronized (caches) {
            caches.remove(cache);
        }
    }

    /**
     * Internal copy of the cache information.
     */
    public static class CacheInfo {
        private Cache cache;

        private long accessCount;

        private long memory;

        private long memoryUsed;

        private boolean wasFull;

        CacheInfo(Cache cache) {
            this.cache = cache;
            // copy the data as this runs in a different thread
            // the exact values are not important, but it is important that the
            // values don't change
            this.memory = cache.getMaxMemorySize();
            this.memoryUsed = cache.getMemoryUsed();
            this.accessCount = cache.getAccessCount();
            // reset the access count, so that concurrent cache access is not lost
            cache.resetAccessCount();
            // if the memory used plus one large object is smaller than the
            // allocated memory,
            // then the memory was not fully used
            wasFull = (memoryUsed + BIG_OBJECT_SIZE) >= memory;
        }

        boolean wasFull() {
            return wasFull;
        }

        long getAccessCount() {
            return accessCount;
        }

        long getMemoryUsed() {
            return memoryUsed;
        }

        void setMemory(long mem) {
            this.memory = mem;
        }

        long getMemory() {
            return memory;
        }

        Cache getCache() {
            return cache;
        }

    }

    public void disposeCache(Cache cache) {
        remove(cache);
    }

}
