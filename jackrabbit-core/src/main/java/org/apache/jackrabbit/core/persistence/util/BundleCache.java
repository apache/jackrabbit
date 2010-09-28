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

import java.util.LinkedHashMap;
import java.util.Map;

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
    private final LinkedHashMap<NodeId, NodePropBundle> bundles;

    /**
     * Creates a new BundleCache
     *
     * @param maxSize the maximum size of this cache in bytes.
     */
    @SuppressWarnings("serial")
    public BundleCache(long maxSize) {
        this.maxSize = maxSize;
        this.bundles = new LinkedHashMap<NodeId, NodePropBundle>(
                (int) maxSize / 1024, 0.75f, true /* access-ordered */) {
            @Override
            protected boolean removeEldestEntry(
                    Map.Entry<NodeId, NodePropBundle> e) {
                if (curSize > BundleCache.this.maxSize) {
                    curSize -= e.getValue().getSize();
                    return true;
                } else {
                    return false;
                }
            }
        };
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
    public synchronized NodePropBundle get(NodeId id) {
        NodePropBundle bundle = bundles.get(id);
        if (bundle != null) {
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
        return bundle;
    }

    /**
     * Puts a bundle to the cache.
     *
     * @param bundle the bunlde to put to the cache
     */
    public synchronized void put(NodePropBundle bundle) {
        NodePropBundle previous = bundles.get(bundle.getId());
        if (previous != null) {
            curSize -= previous.getSize();
        }
        bundles.put(bundle.getId(), bundle);
        curSize += bundle.getSize();
    }

    /**
     * Checks if the bundle with the given id is cached.
     *
     * @param id the id of the bundle
     * @return <code>true</code> if the bundle is cached;
     *         <code>false</code> otherwise.
     */
    public synchronized boolean contains(NodeId id) {
        return bundles.containsKey(id);
    }

    /**
     * Removes a bundle from this cache.
     *
     * @param id the id of the bunlde to remove.
     * @return the previously cached bunlde or <code>null</code> of the bundle
     *         was not cached.
     */
    public synchronized NodePropBundle remove(NodeId id) {
        NodePropBundle bundle = bundles.remove(id);
        if (bundle != null) {
            curSize -= bundle.getSize();
        }
        return bundle;
    }

    /**
     * Clears this cache and removes all bundles.
     */
    public synchronized void clear() {
        bundles.clear();
        curSize = 0;
        hits = 0;
        misses = 0;
    }

}
