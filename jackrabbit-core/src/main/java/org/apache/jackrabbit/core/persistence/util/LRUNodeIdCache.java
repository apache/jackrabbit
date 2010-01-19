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
 * Implements a LRU NodeId cache.
 */
public class LRUNodeIdCache {

    /**
     * The default logger
     */
    private static Logger log = LoggerFactory.getLogger(LRUNodeIdCache.class);

    /**
     * The maximum number of ids to cache
     */
    private long maxSize = 10240;

    /**
     * the number of cache hits
     */
    private long hits;

    /**
     * the number of cache misses
     */
    private long misses;

    /**
     * the map of cached ids
     */
    private final LinkedMap missing = new LinkedMap();

    /**
     * Checks if the given id is contained in this cached.
     *
     * @param id the id to check
     * @return <code>true</code> if the id is cached;
     *         <code>false</code> otherwise.
     */
    public boolean contains(NodeId id) {
        Object o = missing.remove(id);
        if (o == null) {
            misses++;
        } else {
            missing.put(id, id);
            hits++;
        }
        if (log.isInfoEnabled() && (hits + misses) % 10000 == 0) {
            log.info("num=" + missing.size() + "/" + maxSize + " hits=" + hits + " miss=" + misses);
        }
        return o != null;
    }

    /**
     * Puts the given id to this cache.
     * @param id the id to put.
     */
    public void put(NodeId id) {
        if (!missing.containsKey(id)) {
            if (missing.size() == maxSize) {
                missing.remove(0);
            }
            missing.put(id, id);
        }
    }

    /**
     * Removes the it to this cache
     * @param id the id to remove
     * @return <code>true</code> if the id was cached;
     *         <code>false</code> otherwise.
     */
    public boolean remove(NodeId id) {
        return missing.remove(id) != null;
    }

    /**
     * Clears this cache.
     */
    public void clear() {
        missing.clear();
    }

}
