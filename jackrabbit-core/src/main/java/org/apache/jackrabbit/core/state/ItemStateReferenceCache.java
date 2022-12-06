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

import org.apache.commons.collections4.map.AbstractReferenceMap.ReferenceStrength;
import org.apache.commons.collections4.map.ReferenceMap;
import org.apache.jackrabbit.core.id.ItemId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <code>ItemStateReferenceCache</code> internally consists of 2 components:
 * <ul>
 * <li>an <code>ItemStateReferenceMap</code> serving as the primary (or main)
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
 * <p>
 * This implementation of ItemStateCache is thread-safe.
 */
public class ItemStateReferenceCache implements ItemStateCache {

    /** Logger instance */
    private static final Logger log =
        LoggerFactory.getLogger(ItemStateReferenceCache.class);

    /**
     * The number of cache segments to use. Use the number of available
     * processors (even if that might change during runtime!) as a reasonable
     * approximation of the amount of parallelism we should expect in the
     * worst case.
     * <p>
     * One reason for this value being a constant is that the
     * {@link Runtime#availableProcessors()} call is somewhat expensive at
     * least in some environments.
     */
    private static int NUMBER_OF_SEGMENTS =
        Runtime.getRuntime().availableProcessors();

    /**
     * Cache that automatically flushes entries based on some eviction policy;
     * entries flushed from the secondary cache will be indirectly flushed
     * from the reference map by the garbage collector if they thus are
     * rendered weakly reachable.
     */
    private final ItemStateCache cache;

    /**
     * Segments of the weak reference map used to keep track of item states.
     */
    private final Map<ItemId, ItemState>[] segments;

    /**
     * Creates a new <code>ItemStateReferenceCache</code> that uses a
     * <code>MLRUItemStateCache</code> instance as internal cache.
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
    @SuppressWarnings("unchecked")
    public ItemStateReferenceCache(ItemStateCache cache) {
        this.cache = cache;
        this.segments = new Map[NUMBER_OF_SEGMENTS];
        for (int i = 0; i < segments.length; i++) {
            // I tried using soft instead of weak references here, but that
            // seems to have some unexpected performance consequences (notable
            // increase in the JCR TCK run time). So even though soft references
            // are generally recommended over weak references for caching
            // purposes, it seems that using weak references is safer here.
            segments[i] =
                new ReferenceMap<>(ReferenceStrength.HARD, ReferenceStrength.WEAK);
        }
    }

    /**
     * Returns the reference map segment for the given entry key. The segment
     * is selected based on the hash code of the key, after a transformation
     * to prevent interfering with the optimal performance of the segment
     * hash map.
     *
     * @param id item identifier
     * @return reference map segment
     */
    private Map<ItemId, ItemState> getSegment(ItemId id) {
        // Unsigned shift right to prevent negative indexes and to
        // prevent too similar keys to all get stored in the same segment
        return segments[(id.hashCode() >>> 1) % segments.length];
    }

    //-------------------------------------------------------< ItemStateCache >
    /**
     * {@inheritDoc}
     */
    public boolean isCached(ItemId id) {
        Map<ItemId, ItemState> segment = getSegment(id);
        synchronized (segment) {
            return segment.containsKey(id);
        }
    }

    /**
     * {@inheritDoc}
     */
    public ItemState retrieve(ItemId id) {
        // Update the access statistics in the cache
        ItemState state = cache.retrieve(id);
        if (state != null) {
            // Return fast to avoid the second lookup below
            return state;
        }

        Map<ItemId, ItemState> segment = getSegment(id);
        synchronized (segment) {
            return segment.get(id);
        }
    }

    /**
     * {@inheritDoc}
     */
    public ItemState[] retrieveAll() {
        List<ItemState> states = new ArrayList<ItemState>();
        for (int i = 0; i < segments.length; i++) {
            synchronized (segments[i]) {
                states.addAll(segments[i].values());
            }
        }
        return states.toArray(new ItemState[states.size()]);
    }

    /**
     * {@inheritDoc}
     */
    public void cache(ItemState state) {
        // Update the cache
        cache.cache(state);

        // Store a weak reference in the reference map
        ItemId id = state.getId();
        Map<ItemId, ItemState> segment = getSegment(id);
        synchronized (segment) {
            ItemState s = segment.put(id, state);
            // overwriting the same instance is OK
            if (s != null && s != state) {
                log.warn("overwriting cached entry " + id);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void evict(ItemId id) {
        // Update the cache
        cache.evict(id);
        // Remove from reference map
        // TODO: Allow the weak reference to be cleared automatically?
        Map<ItemId, ItemState> segment = getSegment(id);
        synchronized (segment) {
            segment.remove(id);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        cache.dispose();
    }

    /**
     * {@inheritDoc}
     */
    public void evictAll() {
        // Update the cache
        cache.evictAll();
        // remove all weak references from reference map
        // TODO: Allow the weak reference to be cleared automatically?
        for (int i = 0; i < segments.length; i++) {
            synchronized (segments[i]) {
                segments[i].clear();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        for (int i = 0; i < segments.length; i++) {
            synchronized (segments[i]) {
                if (!segments[i].isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

}
