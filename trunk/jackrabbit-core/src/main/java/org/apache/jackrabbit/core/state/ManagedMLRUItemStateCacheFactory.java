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

import org.apache.jackrabbit.core.cache.CacheManager;

/**
 * This class constructs new MLRUItemStateCache.
 * This class adds the new caches to the cache manager,
 * and links the caches to the cache manager.
 */
public class ManagedMLRUItemStateCacheFactory implements ItemStateCacheFactory {

    /** The cache manager. */
    private CacheManager cacheMgr;

    /**
     * Construct a new factory using a cache manager.
     *
     * @param cacheMgr the cache manager
     */
    public ManagedMLRUItemStateCacheFactory(CacheManager cacheMgr) {
        this.cacheMgr = cacheMgr;
    }

    /**
     * Create a new cache instance and link it to the cache manager.
     */
    public ItemStateCache newItemStateCache() {
        return new MLRUItemStateCache(cacheMgr);
    }

}
