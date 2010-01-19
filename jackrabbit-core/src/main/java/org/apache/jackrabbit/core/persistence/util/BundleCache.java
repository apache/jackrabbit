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
package org.apache.jackrabbit.core.persistence.util;

import org.apache.commons.collections.map.LinkedMap;
import org.apache.jackrabbit.core.id.NodeId;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * This Class implements a simple cache for nodeprop bundles
 */
public class BundleCache {

    /**
     * the default logger
     */
    private static Logger log = LoggerFactory.getLogger(BundleCache.class);

    /**
     * the current memory usage of this cache
     */
    private long curSize;

    /**
     * the maximum chache size
     */
    private long maxSize;

    /**
     * the number of cache hits
     */
    private long hits;

    /**
     * the number of cache misses
     */
    private long misses;

    /**
     * a map of the cache entries
     */
    private LinkedMap bundles = new LinkedMap();

    /**
     * Creates a new BundleCache
     *
     * @param maxSize the maximum size of this cache in bytes.
     */
    public BundleCache(long maxSize) {
        this.maxSize = maxSize;
    }

    /**
     * Returns the maximum cache size in bytes.
     *
     * @return the maximum cache size in bytes.
     */
    public long getMaxSize() {
        return maxSize;
    }

    /**
     * Sets the maximum cache size in bytes.
     *
     * @param maxSize the maximum cache size in bytes.
     */
    public void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
    }

    /**
     * Returns the bundle with the given <code>id</code> or <code>null</code>
     * if the bundle is not cached.
     *
     * @param id the id of the bundle
     * @return the cached bundle or <code>null</code>
     */
    public NodePropBundle get(NodeId id) {
        Entry entry = (Entry) bundles.remove(id);
        if (entry != null) {
            // at end
            bundles.put(id, entry);
            hits++;
        } else {
            misses++;
        }
        if (log.isInfoEnabled() && (hits + misses) % 10000 == 0) {
            long c = curSize / 1024;
            long m = maxSize / 1024;
            long a = bundles.size() > 0 ? curSize / bundles.size() : 0;
            log.info("num=" + bundles.size() + " mem=" + c + "k max=" + m + "k avg=" + a
                    + " hits=" + hits + " miss=" + misses);
        }
        return entry == null ? null : entry.bundle;
    }

    /**
     * Puts a bunlde to the cache. If the new size of the cache exceeds the
     * {@link #getMaxSize() max size} of the cache it will remove bundles from
     * this cache until the limit is satisfied.
     *
     * @param bundle the bunlde to put to the cache
     */
    public void put(NodePropBundle bundle) {
        Entry entry = (Entry) bundles.remove(bundle.getId());
        if (entry == null) {
            entry = new Entry(bundle, bundle.getSize());
        } else {
            curSize -= entry.size;
            entry.bundle = bundle;
            entry.size = bundle.getSize();
        }
        bundles.put(bundle.getId(), entry);
        curSize += entry.size;
        // now limit size of cache
        while (curSize > maxSize) {
            entry = (Entry) bundles.remove(0);
            curSize -= entry.size;
        }
    }

    /**
     * Checks if the bundle with the given id is cached.
     *
     * @param id the id of the bundle
     * @return <code>true</code> if the bundle is cached;
     *         <code>false</code> otherwise.
     */
    public boolean contains(NodeId id) {
        return bundles.containsKey(id);
    }

    /**
     * Removes a bundle from this cache.
     *
     * @param id the id of the bunlde to remove.
     * @return the previously cached bunlde or <code>null</code> of the bundle
     *         was not cached.
     */
    public NodePropBundle remove(NodeId id) {
        Entry entry = (Entry) bundles.remove(id);
        if (entry != null) {
            curSize -= entry.size;
            return entry.bundle;
        } else {
            return null;
        }
    }

    /**
     * Clears this cache and removes all bundles.
     */
    public void clear() {
        bundles.clear();
        curSize = 0;
        hits = 0;
        misses = 0;
    }

    /**
     * Internal class that holds the bundles.
     */
    private static final class Entry {

        /**
         * the cached bundle
         */
        private NodePropBundle bundle;

        /**
         * the memory usage of the bundle in bytes
         */
        private long size;

        /**
         * Creates a new entry.
         *
         * @param bundle the bundle to cache
         * @param size the size of the bundle
         */
        public Entry(NodePropBundle bundle, long size) {
            this.bundle = bundle;
            this.size = size;
        }
    }

}
