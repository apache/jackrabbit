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
package org.apache.jackrabbit.spi.commons;

import org.apache.commons.collections4.map.LinkedMap;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.ItemInfo;
import org.apache.jackrabbit.spi.ItemInfoCache;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.NodeInfo;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.PropertyInfo;
import org.apache.jackrabbit.spi.RepositoryService;

/**
 * This implementation of {@link ItemInfoCache} has a default size of 5000 items.
 * Item infos are put into the cache after they have been read from the {@link RepositoryService}.
 * If the cache is full, the oldest item is discarded. Reading items removes them
 * from the cache.
 *
 * The underlying idea here is, that {@link ItemInfo}s which are supplied by the
 * <code>RepositoryService</code> but not immediately needed are put into the
 * cache to avoid further round trips to <code>RepositoryService</code>.
 * When they are needed later, they are read from the cache. There is no need to
 * keep them in this cache after that point since they are present in the
 * hierarchy from then on.
 */
public class ItemInfoCacheImpl implements ItemInfoCache {

    /**
     * Default size of the cache.
     */
    public static final int DEFAULT_CACHE_SIZE = 5000;

    private final int cacheSize;
    private final LinkedMap entries;

    /**
     * Create a new instance with the default cache size.
     * @see #DEFAULT_CACHE_SIZE
     */
    public ItemInfoCacheImpl() {
        this(DEFAULT_CACHE_SIZE);
    }

    /**
     * Create a new instance with a given cache size.
     * @param cacheSize
     */
    public ItemInfoCacheImpl(int cacheSize) {
        super();
        this.cacheSize = cacheSize;
        entries = new LinkedMap(cacheSize);
    }

    /**
     * This implementation removes the item from the cache
     * if it is present. Furthermore if the <code>nodeId</code>
     * id uuid based, and no item is found by the <code>nodeId</code>
     * a second lookup is done by the path.
     */
    public Entry<NodeInfo> getNodeInfo(NodeId nodeId) {
        Object entry = entries.remove(nodeId);
        if (entry == null) {
            entry = entries.remove(nodeId.getPath());
        } else {
            // there might be a corresponding path-indexed entry, clear it as well
            entries.remove(node(entry).info.getPath());
        }

        return node(entry);
    }

    /**
     * This implementation removes the item from the cache
     * if it is present. Furthermore if the <code>propertyId</code>
     * id uuid based, and no item is found by the <code>propertyId</code>
     * a second lookup is done by the path.
     */
    public Entry<PropertyInfo> getPropertyInfo(PropertyId propertyId) {
        Object entry = entries.remove(propertyId);
        if (entry == null) {
            entry = entries.remove(propertyId.getPath());
        } else {
            // there might be a corresponding path-indexed entry, clear it as well
            entries.remove(property(entry).info.getPath());
        }

        return property(entry);
    }

    /**
     * This implementation cached the item by its id and if the id
     * is uuid based but has no path, also by its path.
     */
    public void put(ItemInfo info, long generation) {
        ItemId id = info.getId();
        Entry<? extends ItemInfo> entry = info.denotesNode()
            ? new Entry<NodeInfo>((NodeInfo) info, generation)
            : new Entry<PropertyInfo>((PropertyInfo) info, generation);

        put(id, entry);
        if (id.getUniqueID() != null && id.getPath() == null) {
            put(info.getPath(), entry);
        }
    }

    public void dispose() {
        entries.clear();
    }

    // -----------------------------------------------------< private >---

    private void put(Object key, Entry<? extends ItemInfo> entry) {
        entries.remove(key);
        if (entries.size() >= cacheSize) {
            entries.remove(entries.firstKey());  // xxx AbstractLinkedMap#firstKey() Javadoc is wrong. See COLLECTIONS-353
        }
        entries.put(key, entry);
    }

    @SuppressWarnings("unchecked")
    private static Entry<NodeInfo> node(Object entry) {
        if (entry != null && ((Entry<? extends ItemInfo>) entry).info.denotesNode()) {
            return (Entry<NodeInfo>) entry;
        }
        else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Entry<PropertyInfo> property(Object entry) {
        if (entry != null && !((Entry<? extends ItemInfo>) entry).info.denotesNode()) {
            return (Entry<PropertyInfo>) entry;
        }
        else {
            return null;
        }
    }
}