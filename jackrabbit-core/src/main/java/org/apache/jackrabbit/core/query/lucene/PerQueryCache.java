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
package org.apache.jackrabbit.core.query.lucene;

import java.util.HashMap;
import java.util.Map;

/**
 * <code>PerQueryCache</code> implements a hash map on a per thread basis for
 * the purpose of caching results while a query is executed. When the query
 * finished the cache can be disposed by calling:
 * <code>PerQueryCache.getInstance().dispose()</code>.
 */
class PerQueryCache {

    /**
     * The internal map of this <code>PerQueryCache</code>.
     */
    private final Map<Key, Object> map = new HashMap<Key, Object>();

    /**
     * Private constructor.
     */
    private PerQueryCache() {
    }

    /**
     * The per thread cache instance.
     */
    private static final ThreadLocal<PerQueryCache> CACHE = new ThreadLocal<PerQueryCache>();

    /**
     * @return <code>PerQueryCache</code> for the current thread.
     */
    static PerQueryCache getInstance() {
        PerQueryCache cache = CACHE.get();
        if (cache == null) {
            cache = new PerQueryCache();
            CACHE.set(cache);
        }
        return cache;
    }

    /**
     * Returns the value from the cache with the given <code>type</code> and
     * <code>key</code>.
     *
     * @param type the query type.
     * @param key  the key object.
     * @return the value assigned to <code>type</code> and <code>key</code> or
     *         <code>null</code> if it does not exist in the cache.
     */
    Object get(Class type, Object key) {
        return map.get(new Key(type, key));
    }

    /**
     * Puts the <code>value</code> into the cache and assigns it to
     * <code>type</code> and <code>key</code>.
     *
     * @param type  the query type.
     * @param key   the key object.
     * @param value the value to cache.
     * @return the existing value in the cache assigned to <code>type</code> and
     *         <code>key</code> or <code>null</code> if there was none.
     */
    Object put(Class type, Object key, Object value) {
        return map.put(new Key(type, key), value);
    }

    /**
     * Disposes the <code>PerQueryCache</code> for the current thread.
     */
    void dispose() {
        CACHE.set(null);
    }

    /**
     * Simple key class.
     */
    private static final class Key {

        /**
         * The query type.
         */
        private final Class type;

        /**
         * The key object.
         */
        private final Object key;

        /**
         * Creates a new internal <code>Key</code> object.
         *
         * @param type the query type.
         * @param key the key object.
         */
        private Key(Class type, Object key) {
            this.type = type;
            this.key = key;
        }

        /**
         * {@inheritDoc}
         */
        public int hashCode() {
            return type.hashCode() ^ key.hashCode();
        }

        /**
         * {@inheritDoc}
         */
        public boolean equals(Object obj) {
            if (obj instanceof Key) {
                Key other = (Key) obj;
                return type == other.type && key.equals(other.key);
            }
            return false;
        }
    }
}
