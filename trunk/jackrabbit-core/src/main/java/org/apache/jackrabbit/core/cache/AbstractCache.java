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

import static org.apache.jackrabbit.core.cache.CacheAccessListener.ACCESS_INTERVAL;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Abstract base class for managed {@link Cache}s. This class uses atomic
 * variables to track the current and maximum size of the cache, the cache
 * access count and a possible {@link CacheAccessListener} instance.
 * <p>
 * A subclass should call the protected {@link #recordCacheAccess()} method
 * whenever the cache is accessed (even cache misses should be reported).
 * The subclass should also use the {@link #recordSizeChange(long)} method
 * to record all changes in the cache size, and automatically evict excess
 * items when the {@link #isTooBig()} method returns <code>true</code>.
 */
public abstract class AbstractCache implements Cache {

    /**
     * The estimated amount of memory currently used by this cache. The
     * current value is returned by the {@link #getMemoryUsed()} method
     * and can be updated by a subclass using the protected
     * {@link #recordSizeChange(long)} method.
     */
    private final AtomicLong memoryUsed = new AtomicLong();

    /**
     * The allocated maximum size of this cache. A {@link CacheManager} uses
     * the {@link #getMaxMemorySize()} and {@link #setMaxMemorySize(long)}
     * methods to control the target size of a cache. Subclasses can use the
     * protected {@link #isTooBig()} method to determine whether the current
     * size of the cache exceeds this size limit.
     */
    private final AtomicLong maxMemorySize = new AtomicLong();

    /**
     * Cache access counter. Used to fire periodic
     * {@link CacheAccessListener#cacheAccessed()} events once every
     * {@link CacheAccessListener#ACCESS_INTERVAL} calls to the protected
     * {@link #recordCacheAccess()} method.
     * <p>
     * A long counter is used to prevent integer overflow. Even if the cache
     * was accessed once every nanosecond, an overflow would only occur in
     * about 300 years. See
     * <a href="https://issues.apache.org/jira/browse/JCR-3013">JCR-3013</a>.
     */
    private final AtomicLong accessCount = new AtomicLong();

    /**
     * Cache access counter. Unike his counterpart {@link #accessCount}, this
     * does not get reset.
     * 
     * It is used in the cases where a cache listener needs to call
     * {@link Cache#resetAccessCount()}, but also needs a total access count. If
     * you are sure that nobody calls reset, you can just use
     * {@link #accessCount}.
     */
    private final AtomicLong totalAccessCount = new AtomicLong();

    /**
     * Cache miss counter.
     */
    private final AtomicLong missCount = new AtomicLong();

    /**
     * Cache access listener. Set in the
     * {@link #setAccessListener(CacheAccessListener)} method and accessed
     * by periodically by the {@link #recordCacheAccess()} method.
     */
    private final AtomicReference<CacheAccessListener> accessListener =
        new AtomicReference<CacheAccessListener>();

    /**
     * Checks whether the current estimate of the amount of memory used
     * by this cache exceeds the allocated maximum amount of memory.
     *
     * @return <code>true</code> if the cache size is too big,
     *         <code>false</code> otherwise
     */
    protected boolean isTooBig() {
        return memoryUsed.get() > maxMemorySize.get();
    }

    /**
     * Updates the current memory use estimate of this cache.
     *
     * @param delta number of bytes added or removed
     */
    protected void recordSizeChange(long delta) {
        memoryUsed.addAndGet(delta); // ignore the return value
    }

    /**
     * Records a single cache access and calls the configured
     * {@link CacheAccessListener} (if any) whenever the constant access
     * interval has passed since the previous listener call.
     */
    protected void recordCacheAccess() {
        totalAccessCount.incrementAndGet();
        long count = accessCount.incrementAndGet();
        if (count % ACCESS_INTERVAL == 0) {
            CacheAccessListener listener = accessListener.get();
            if (listener != null) {
                listener.cacheAccessed(ACCESS_INTERVAL);
            }
        }
    }

    protected void recordCacheMiss() {
        missCount.incrementAndGet();
    }

    public long getAccessCount() {
        return accessCount.get();
    }

    public void resetAccessCount() {
        accessCount.set(0);
    }
    
    public long getTotalAccessCount(){
        return totalAccessCount.get();
    }

    public long getMissCount() {
        return missCount.get();
    }

    public void resetMissCount() {
        missCount.set(0);
    }

    public long getMemoryUsed() {
        return memoryUsed.get();
    }

    public long getMaxMemorySize() {
        return maxMemorySize.get();
    }

    public void setMaxMemorySize(long size) {
        maxMemorySize.set(size);
    }

    /**
     * Set the cache access listener. Only one listener per cache is supported.
     *
     * @param listener the new listener
     */
    public void setAccessListener(CacheAccessListener listener) {
        accessListener.set(listener);
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        CacheAccessListener listener = accessListener.get();
        if (listener != null) {
            listener.disposeCache(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getCacheInfoAsString() {
        long u = getMemoryUsed() / 1024;
        long m = getMaxMemorySize() / 1024;
        StringBuilder c = new StringBuilder();
        c.append("cachename=");
        c.append(this.toString());
        c.append(", elements=");
        c.append(getElementCount());
        c.append(", usedmemorykb=");
        c.append(u);
        c.append(", maxmemorykb=");
        c.append(m);
        c.append(", access=");
        c.append(getTotalAccessCount());
        c.append(", miss=");
        c.append(getMissCount());
        return c.toString();
    }
}
