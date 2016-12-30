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
package org.apache.jackrabbit.spi;

/**
 * <code>ItemInfoCache</code> instances are responsible for caching
 * {@link ItemInfo}s along with an opaque generation counter. Implementations
 * are free on the particular caching policy. That is, how long (if at all) item
 * infos are cached.
 *
 * An <code>ItemInfoCache</code> is supplied per session from the {@link RepositoryService}. It is used
 * to cache <code>ItemInfo</code>s read from the <code>RepositoryService</code>.
 *
 * @see RepositoryService#getItemInfos(SessionInfo, ItemId)
 */
public interface ItemInfoCache {

    /**
     * This class represents a cache entry.
     * @param <T> Either a {@link NodeInfo} or a {@link PropertyInfo}.
     */
    class Entry<T extends ItemInfo> {

        /**
         * The {@link ItemInfo}
         */
        public final T info;

        /**
         * The generation
         */
        public final long generation;

        /**
         * Create a new cache entry containing <code>info</code> with a given <code>generation</code>.
         * @param info
         * @param generation
         */
        public Entry(T info, long generation) {
            this.info = info;
            this.generation = generation;
        }

        @Override
        public int hashCode() {
            return info.hashCode() + (int) generation;
        }

        @Override
        public boolean equals(Object that) {
            if (that == null) {
                return false;
            }

            if (that == this) {
                return true;
            }

            if (that instanceof ItemInfoCache.Entry) {
                ItemInfoCache.Entry other = (ItemInfoCache.Entry) that;
                return generation == other.generation && info.equals(other.info);
            }

            return false;
        }
    }

    /**
     * Retrieve a cache entry for the given <code>nodeId</code> or <code>null</code>
     * if no such entry is in the cache.
     *
     * @param nodeId  id of the entry to lookup.
     * @return a <code>Entry&lt;NodeInfo&gt;</code> instance or <code>null</code>
     * if not found.
     */
    ItemInfoCache.Entry<NodeInfo> getNodeInfo(NodeId nodeId);

    /**
     * Retrieve a cache entry for the given <code>propertyId</code> or <code>null</code>
     * if no such entry is in the cache.
     *
     * @param propertyId  id of the entry to lookup.
     * @return  a <code>Entry&lt;PropertyInfo&gt;</code> instance or
     * <code>null</code> if not found.
     */
    ItemInfoCache.Entry<PropertyInfo> getPropertyInfo(PropertyId propertyId);

    /**
     * Create a {@link Entry} for <code>info</code> and <code>generation</code> and put it into
     * the cache.
     * @param info
     * @param generation
     */
    void put(ItemInfo info, long generation);

    /**
     * Clear the cache and dispose all entries.
     */
    void dispose();
}