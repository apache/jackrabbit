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
package org.apache.jackrabbit.spi.commons.conversion;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Generational cache. The cache implemented by this class consists of three
 * parts: a long term cache and two generations of recent entries. The two
 * generations are used to collect recent new entries, and those entries that
 * are used within two successive generations get promoted to the long term
 * cache. The entries within the long term cache are discarded only when the
 * size of the cache exceeds the given maximum cache size.
 */
class GenerationalCache {

    /**
     * Default maximum cache size.
     */
    private static final int DEFAULT_CACHE_SIZE = 1000;

    /**
     * Divisor used to determine the default generation age from the
     * maximum cache size.
     */
    private static final int DEFAULT_SIZE_AGE_RATIO = 10;

    /**
     * Maximum size of the name cache.
     */
    private final int maxSize;

    /**
     * Maximum age of a cache generation.
     */
    private final int maxAge;

    /**
     * Long term cache. Read only.
     */
    private Map cache = new HashMap();

    /**
     * Old cache generation.
     */
    private Map old = new HashMap();

    /**
     * Young cache generation.
     */
    private Map young = new HashMap();

    /**
     * Age of the young cache generation.
     */
    private int age = 0;

    /**
     * Creates a caching resolver.
     *
     * @param maxSize maximum size of the long term cache
     * @param maxAge maximum age of a cache generation
     */
    public GenerationalCache(int maxSize, int maxAge) {
        this.maxSize = maxSize;
        this.maxAge = maxAge;
    }

    /**
     * Creates a caching resolver using the default generation age for
     * the given cache size.
     *
     * @param maxSize maximum size of the long term cache
     */
    public GenerationalCache(int maxSize) {
        this(maxSize, maxSize / DEFAULT_SIZE_AGE_RATIO);
    }

    /**
     * Creates a caching resolver using the default size and generation age.
     */
    public GenerationalCache() {
        this(DEFAULT_CACHE_SIZE);
    }

    /**
     * Returns the cached value (if any) for the given key. The value is
     * looked up both from the long term cache and the old cache generation.
     * If the value is only found in the old cache generation, it gets added
     * to the young generation via a call to {@link #put(Object, Object)}.
     *
     * @param key key of the cache entry
     * @return value of the cache entry, or <code>null</code>
     */
    public Object get(Object key) {
        Object value = cache.get(key);
        if (value == null) {
            value = old.get(key);
            if (value != null) {
                put(key, value);
            }
        }
        return value;
    }

    /**
     * Caches the given key-value pair and increases the age of the current
     * cache generation. When the maximum age of a generation is reached,
     * the following steps are taken:
     * <ol>
     *   <li>The union of the two cache generations is calculated</li>
     *   <li>The union is added to the long term name cache</li>
     *   <li>If the cache size exceeds the maximum, only the union is kept</li>
     *   <li>A new cache generation is started</li>
     * </ol>
     *
     * @param key key of the cache entry
     * @param value value of the cache entry
     */
    public synchronized void put(Object key, Object value) {
        young.put(key, value);

        if (++age == maxAge) {
            Map union = new HashMap();
            Iterator iterator = old.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                if (young.containsKey(entry.getKey())) {
                    union.put(entry.getKey(), entry.getValue());
                }
            }

            if (!union.isEmpty()) {
                if (cache.size() + union.size() <= maxSize) {
                    union.putAll(cache);
                }
                cache = union;
            }

            old = young;
            young = new HashMap();
            age = 0;
        }
    }

}
