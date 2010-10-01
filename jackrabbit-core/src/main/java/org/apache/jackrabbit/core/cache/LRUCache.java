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

import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections.map.LinkedMap;
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
public class LRUCache<Key, Value> implements Cache {

    /** Logger instance */
    private static Logger log = LoggerFactory.getLogger(LRUCache.class);

    /**
     * Map of cache entries.
     */
    private final ConcurrentHashMap<Key, Entry<Key, Value>> entries =
        new ConcurrentHashMap<Key, Entry<Key, Value>>();

    /**
     * Most recently used entry, or <code>null</code> if the cache is empty.
     */
    private Entry<Key, Value> first = null;

    /**
     * Least recently used entry, or <code>null</code> if the cache is empty.
     */
    private Entry<Key, Value> last = null;

    /**
     * Current size of the cache; sum of the sizes of all the cached entries.
     */
    private long currentSize = 0;

    /**
     * Maximum size of the cache
     */
    private long maximumSize;

    /**
     * Cache access listener
     */
    private CacheAccessListener listener = null;

    /**
     * Access count used to fire {@link CacheAccessListener#cacheAccessed()}
     * calls once every {@link CacheAccessListener#ACCESS_INTERVAL} hits.
     */
    private int accessCount;

    /**
     * Constructs a new, empty <code>ItemStateCache</code> with the specified
     * maximum memory.
     *
     * @param maximumSize maximum size of the cache
     */
    public LRUCache(long maximumSize) {
        this.maximumSize = maximumSize;
    }

    public boolean containsKey(Key key) {
        return entries.containsKey(key);
    }

    public Value get(Key key) {
        Entry<Key, Value> entry = entries.get(key);
        if (entry != null) {
            boolean notifyAccessListener;
            synchronized (this) {
                // Check if we should notify the access listener
                notifyAccessListener =
                    (++accessCount % CacheAccessListener.ACCESS_INTERVAL) == 0;

                // Move this entry to the beginning of the LRU linked list
                if (entry.prev != null) {
                    entry.prev.next = entry.next;
                    if (entry.next != null) {
                        entry.next.prev = entry.prev;
                    } else if (entry.prev != null) {
                        last = entry.prev;
                        entry.prev = null;
                    }
                    first.prev = entry;
                    entry.next = first;
                    first = entry;
                }
            }

            // Notify the access listener outside the synchronized block
            if (notifyAccessListener && listener != null) {
                listener.cacheAccessed();
            }

            return entry.value;
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public synchronized Value[] values() {
        Object[] values = new Object[entries.size()];
        Entry<Key, Value> entry = first;
        for (int i = 0; i < values.length && entry != null; i++) {
            values[i] = entry.value;
            entry = entry.next;
        }
        return (Value[]) values;
    }

    public synchronized void put(Key key, Value value, long size) {
        Entry<Key, Value> entry = new Entry<Key, Value>(key, value, size);
        if (first != null) {
            first.prev = entry;
        }
        entry.next = first;
        first = entry;
        if (last == null) {
            last = entry;
        }

        currentSize += size;

        Entry<Key, Value> previous = entries.put(key, entry);
        if (previous != null) {
            log.warn("Overwriting cached entry {}", key);
            currentSize -= previous.size;
        }

        shrinkIfNeeded();
    }

    public synchronized Value remove(Key key) {
        Entry<Key, Value> entry = entries.remove(key);
        if (entry != null) {
            if (entry.prev != null) {
                entry.prev.next = entry.next;
            } else {
                first = entry.next;
            }
            if (entry.next != null) {
                entry.next.prev = entry.prev;
            } else {
                last = entry.prev;
            }

            currentSize -= entry.size;

            return entry.value;
        } else {
            return null;
        }
    }

    public synchronized void clear() {
        entries.clear();
        first = null;
        last = null;
        currentSize = 0;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public synchronized long getAccessCount() {
        return accessCount;
    }

    public synchronized void resetAccessCount() {
        accessCount = 0;
    }

    public synchronized long getMemoryUsed() {
        return currentSize;
    }

    public synchronized long getMaxMemorySize() {
        return maximumSize;
    }

    public synchronized void setMaxMemorySize(long size) {
        this.maximumSize = size;
        shrinkIfNeeded();
    }

    private void shrinkIfNeeded() {
        while (currentSize > maximumSize && last != null) {
            entries.remove(last.key);
            currentSize -= last.size;
            if (last.prev != null) {
                last.prev.next = null;
                last = last.prev;
            } else {
                first = null;
                last = null;
            }
        }
    }

    /**
     * Set the cache access listener. Only one listener per cache is supported.
     *
     * @param listener the new listener
     */
    public synchronized void setAccessListener(CacheAccessListener listener) {
        this.listener = listener;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void dispose() {
        if (listener != null) {
            listener.disposeCache(this);
        }
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{ ");
        Entry<Key, Value> entry = first;
        while (entry != null) {
            builder.append(entry.key);
            builder.append(" => ");
            builder.append(entry.value);
            builder.append(" ");
        }
        builder.append("}");
        return builder.toString();
    }

    /**
     * Internal cache entry.
     */
    private static class Entry<Key, Value> {

        final Key key;

        final Value value;

        final long size;

        Entry<Key, Value> prev = null;

        Entry<Key, Value> next = null;

        public Entry(Key key, Value value, long size) {
            this.key = key;
            this.value = value;
            this.size = size;
        }

    }

}
