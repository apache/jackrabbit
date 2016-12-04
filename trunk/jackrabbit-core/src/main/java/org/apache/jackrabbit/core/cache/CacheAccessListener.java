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
 * The cache access listener can be registered to a class.
 * From time to time, the method cacheAccess is called.
 */
public interface CacheAccessListener {

    /**
     * The access listener is only called each x accesses.
     */
    int ACCESS_INTERVAL = 127;

    /**
     * The cache calls this method after a number of cache accesses.<br>
     * 
     * For statistical purposes, the cache access count since the last call is
     * included. In normal circumstances this is equal to
     * {@link CacheAccessListener#ACCESS_INTERVAL}
     * 
     * @param accessCount
     *            number of cache accesses since the last call
     * 
     */
    void cacheAccessed(long accessCount);

    /**
     * Called after the cache is no longer used.
     */
    void disposeCache(Cache cache);

}
