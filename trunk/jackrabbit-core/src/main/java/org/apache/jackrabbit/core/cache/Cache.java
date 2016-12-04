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

/**
 * A <code>Cache</code> object
 * A cache must call <code>CacheManager.getInstance().add(this)</code>
 * to take part in the dynamic memory distribution.
 */
public interface Cache {

    /**
     * Set the new memory limit.
     * @param size the size in bytes
     */
    void setMaxMemorySize(long size);

    /**
     * Get the current limit.
     * @return the size in bytes
     */
    long getMaxMemorySize();

    /**
     * Get the amount of used memory.
     * @return the size in bytes
     */
    long getMemoryUsed();

    /**
     * Get the number of accesses (get or set) until resetAccessCount was called.
     * @return the count
     */
    long getAccessCount();

    /**
     * Reset the access counter.
     */
    void resetAccessCount();

    /**
     * Get the total number of cache accesses.
     * @return the number of hits
     */
    long getTotalAccessCount();

    /**
     * Get the number of cache misses.
     * 
     * @return the number of misses
     */
    long getMissCount();

    /**
     * Reset the cache miss counter.
     */
    void resetMissCount();

    /**
     * Get the number of elements/objects in the cache.
     * @return the number of elements
     */
    long getElementCount();

    /**
     * Add a listener to this cache that is informed after a number of accesses.
     */
    void setAccessListener(CacheAccessListener listener);

    /**
     * Gathers the stats of the cache for logging.
     */
    String getCacheInfoAsString();

}
