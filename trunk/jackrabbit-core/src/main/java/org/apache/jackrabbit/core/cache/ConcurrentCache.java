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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Concurrent cache implementation that uses cache segments to minimize
 * the chance of lock contention. The LRU algorithm is used to evict excess
 * entries from each cache segment separately, which makes the combined
 * eviction algorithm similar but not exactly the same as LRU. None of the
 * methods of this class are synchronized, but they are all thread-safe.
 */
public class ConcurrentCache<K, V> extends AbstractCache {

    /**
     * Default number of cache segments to use. Use the number of available
     * processors (even if that might change during runtime!) as a reasonable
     * approximation of the amount of parallelism we should expect in the
     * worst case.
     * <p>
     * One reason for this value being a constant is that the
     * {@link Runtime#availableProcessors()} call is somewhat expensive at
     * least in some environments.
     */
    private static int DEFAULT_NUMBER_OF_SEGMENTS =
        Runtime.getRuntime().availableProcessors();

    private static class E<V> {

        private final V value;

        private final long size;

        public E(V value, long size) {
            this.value = value;
            this.size = size;
        }

    }

    private final String name;
    private final Map<K, E<V>>[] segments;

    @SuppressWarnings({ "unchecked", "serial" })
    public ConcurrentCache(String name, int numberOfSegments) {
        this.name = name;
        this.segments = new Map[numberOfSegments];
        for (int i = 0; i < segments.length; i++) {
            segments[i] = new LinkedHashMap<K, E<V>>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<K, E<V>> eldest) {
                    if (isTooBig()) {
                        recordSizeChange(-eldest.getValue().size);
                        return true;
                    } else {
                        return false;
                    }
                }
            };
        }
    }

    public ConcurrentCache(String name) {
        this(name, DEFAULT_NUMBER_OF_SEGMENTS);
    }

    /**
     * Returns the cache segment for the given entry key. The segment is
     * selected based on the hash code of the key, after a transformation
     * to prevent interfering with the optimal performance of the segment
     * hash map.
     *
     * @param key entry key
     * @return cache segment
     */
    private Map<K, E<V>> getSegment(K key) {
        // Unsigned shift right to prevent negative indexes and to
        // prevent too similar keys to all get stored in the same segment
        return segments[(key.hashCode() >>> 1) % segments.length];
    }

    /**
     * Checks if the identified entry is cached.
     *
     * @param key entry key
     * @return <code>true</code> if the entry is cached,
     *         <code>false</code> otherwise
     */
    public boolean containsKey(K key) {
        Map<K, E<V>> segment = getSegment(key);
        synchronized (segment) {
            return segment.containsKey(key);
        }
    }

    /**
     * Returns the identified cache entry.
     *
     * @param key entry key
     * @return entry value, or <code>null</code> if not found
     */
    public V get(K key) {
        recordCacheAccess();

        Map<K, E<V>> segment = getSegment(key);
        synchronized (segment) {
            E<V> entry = segment.get(key);
            if (entry != null) {
                return entry.value;
            }
        }
        recordCacheMiss();
        return null;
    }

    /**
     * Returns all values in the cache. Note that this method is not
     * synchronized over the entire cache, so it is only guaranteed to
     * return accurate results when there are no concurrent threads modifying
     * the cache.
     *
     * @return cached values
     */
    public List<V> values() {
        List<V> values = new ArrayList<V>();
        for (int i = 0; i < segments.length; i++) {
            synchronized (segments[i]) {
                for (E<V> entry : segments[i].values()) {
                    values.add(entry.value);
                }
            }
        }
        return values;
    }

    /**
     * Adds the given entry to the cache.
     *
     * @param key entry key
     * @param value entry value
     * @param size entry size
     * @return the previous value, or <code>null</code>
     */
    public V put(K key, V value, long size) {
        E<V> previous;

        Map<K, E<V>> segment = getSegment(key);
        synchronized (segment) {
            recordSizeChange(size);
            previous = segment.put(key, new E<V>(value, size));
        }

        if (previous != null) {
            recordSizeChange(-previous.size);
            shrinkIfNeeded();
            return previous.value;
        } else {
            shrinkIfNeeded();
            return null;
        }
    }

    /**
     * Removes the identified entry from the cache.
     *
     * @param key entry key
     * @return removed entry, or <code>null</code> if not found
     */
    public V remove(K key) {
        Map<K, E<V>> segment = getSegment(key);
        synchronized (segment) {
            E<V> entry = segment.remove(key);
            if (entry != null) {
                recordSizeChange(-entry.size);
                return entry.value;
            } else {
                return null;
            }
        }
    }

    /**
     * Clears all segments of the cache. Note that even this method is not
     * synchronized over the entire cache, so it needs to explicitly count
     * the cache size changes and may return with a non-empty cache if
     * other threads have concurrently been adding new entries.
     */
    public void clear() {
        for (int i = 0; i < segments.length; i++) {
            synchronized (segments[i]) {
                for (E<V> entry : segments[i].values()) {
                    recordSizeChange(-entry.size);
                }
                segments[i].clear();
            }
        }
    }

    /**
     * Checks if the cache size is zero.
     */
    public boolean isEmpty() {
        return getMemoryUsed() == 0;
    }

    /**
     * Sets the maximum size of the cache and evicts any excess items until
     * the current size falls within the given limit.
     */
    @Override
    public void setMaxMemorySize(long size) {
        super.setMaxMemorySize(size);
        shrinkIfNeeded();
    }

    /**
     * Removes old entries from the cache until the cache is small enough.
     */
    private void shrinkIfNeeded() {
        // Semi-random start index to prevent bias against the first segments
        int start = (int) Math.abs(getAccessCount() % segments.length);
        for (int i = start; isTooBig(); i = (i + 1) % segments.length) {
            synchronized (segments[i]) {
                Iterator<Map.Entry<K, E<V>>> iterator =
                    segments[i].entrySet().iterator();
                if (iterator.hasNext()) {
                    // Removing and re-adding the first entry will
                    // evict the last entry if the cache is too big
                    Map.Entry<K, E<V>> entry = iterator.next();
                    segments[i].remove(entry.getKey());
                    segments[i].put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    public long getElementCount() {
        long count = 0;
        for (int i = 0; i < segments.length; i++) {
            count += segments[i].size();
        }
        return count;
    }

    @Override
    public String toString() {
        return name + "[" + getClass().getSimpleName() + "@"
                + Integer.toHexString(hashCode()) + "]";
    }
}
