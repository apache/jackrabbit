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

import org.apache.commons.collections.map.LRUMap;
import org.apache.jackrabbit.core.ItemId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * An <code>ItemStateCache</code> implementation that internally uses a
 * {@link LRUMap} to maintain a cache of <code>ItemState</code> objects.
 */
public class LRUItemStateCache implements ItemStateCache {
    /** Logger instance */
    private static Logger log = LoggerFactory.getLogger(LRUItemStateCache.class);

    /** default maximum size of this cache */
    public static final int DEFAULT_MAX_SIZE = 1000;

    /**
     * A cache for <code>ItemState</code> instances
     */
    private final LRUMap cache;

    /**
     * Constructs a new, empty <code>ItemStateCache</code> with a maximum size
     * of 1000.
     */
    public LRUItemStateCache() {
        this(DEFAULT_MAX_SIZE);
    }

    /**
     * Constructs a new, empty <code>ItemStateCache</code> with the specified
     * maximum size.
     *
     * @param maxSize the maximum size of the cache, -1 for no limit,
     */
    public LRUItemStateCache(int maxSize) {
        // setup cache
        cache = new LRUMap(maxSize, true);
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
        return (ItemState) cache.get(id);
    }

    /**
     * {@inheritDoc}
     */
    public void cache(ItemState state) {
        ItemId id = state.getId();
        if (cache.containsKey(id)) {
            log.warn("overwriting cached entry " + id);
        }
        cache.put(id, state);
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
    public void update(ItemId id) {
        // do nothing
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
    public int size() {
        return cache.size();
    }

    /**
     * {@inheritDoc}
     */
    public Set keySet() {
        return Collections.unmodifiableSet(cache.keySet());
    }

    /**
     * {@inheritDoc}
     */
    public Collection values() {
        return Collections.unmodifiableCollection(cache.values());
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        cache.clear();
    }
}
