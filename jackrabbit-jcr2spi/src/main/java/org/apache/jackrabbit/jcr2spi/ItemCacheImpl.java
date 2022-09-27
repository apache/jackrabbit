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
package org.apache.jackrabbit.jcr2spi;

import java.util.Map;

import javax.jcr.Item;
import javax.jcr.RepositoryException;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>ItemCacheImpl</code>...
 */
public class ItemCacheImpl implements ItemCache {

    private static Logger log = LoggerFactory.getLogger(ItemCacheImpl.class);

    private final Map<ItemState, Item> cache;

    ItemCacheImpl(int maxSize) {
        cache = new LRUMap<>(maxSize);
    }

    //----------------------------------------------------------< ItemCache >---
    /**
     * @see ItemCache#getItem(ItemState)
     */
    public Item getItem(ItemState state) {
        return cache.get(state);
    }

    /**
     * @see ItemCache#clear()
     */
    public void clear() {
        cache.clear();
    }

    //----------------------------------------------< ItemLifeCycleListener >---
    /**
     * @see ItemLifeCycleListener#itemCreated(Item)
     */
    public void itemCreated(Item item) {
        if (!(item instanceof ItemImpl)) {
            String msg = "Incompatible Item object: " + ItemImpl.class.getName() + " expected.";
            throw new IllegalArgumentException(msg);
        }
        if (log.isDebugEnabled()) {
            log.debug("created item " + item);
        }
        // add instance to cache
        cacheItem(((ItemImpl)item).getItemState(), item);
    }

    public void itemUpdated(Item item, boolean modified) {
        if (!(item instanceof ItemImpl)) {
            String msg = "Incompatible Item object: " + ItemImpl.class.getName() + " expected.";
            throw new IllegalArgumentException(msg);
        }
        if (log.isDebugEnabled()) {
            log.debug("update item " + item);
        }

        ItemState state = ((ItemImpl) item).getItemState();
        // touch the corresponding cache entry
        Item cacheEntry = getItem(state);
        if (cacheEntry == null) {
            // .. or add the item to the cache, if not present yet.
            cacheItem(state, item);
        }
    }

    /**
     * @see ItemLifeCycleListener#itemDestroyed(Item)
     */
    public void itemDestroyed(Item item) {
        if (!(item instanceof ItemImpl)) {
            String msg = "Incompatible Item object: " + ItemImpl.class.getName() + " expected.";
            throw new IllegalArgumentException(msg);
        }
        if (log.isDebugEnabled()) {
            log.debug("destroyed item " + item);
        }
        // we're no longer interested in this item
        ((ItemImpl)item).removeLifeCycleListener(this);
        // remove instance from cache
        evictItem(((ItemImpl)item).getItemState());
    }

    //-------------------------------------------------< item cache methods >---
    /**
     * Puts the reference of an item in the cache with
     * the item's path as the key.
     *
     * @param item the item to cache
     */
    private synchronized void cacheItem(ItemState state, Item item) {
        if (cache.containsKey(state)) {
            log.warn("overwriting cached item " + state);
        }
        if (log.isDebugEnabled()) {
            log.debug("caching item " + state);
        }
        cache.put(state, item);
    }

    /**
     * Removes a cache entry for a specific item.
     *
     * @param itemState state of the item to remove from the cache
     */
    private synchronized void evictItem(ItemState itemState) {
        if (log.isDebugEnabled()) {
            log.debug("removing item " + itemState + " from cache");
        }
        cache.remove(itemState);
    }

    //--------------------------------------------------------==---< Object >---

    /**
     * Returns the the state of this instance in a human readable format.
     */
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<ItemState, Item> entry : cache.entrySet()) {
            ItemState state = entry.getKey();
            Item item = entry.getValue();
            if (item.isNode()) {
                builder.append("Node: ");
            } else {
                builder.append("Property: ");
            }
            if (item.isNew()) {
                builder.append("new ");
            } else if (item.isModified()) {
                builder.append("modified ");
            } else {
                builder.append("- ");
            }
            String path;
            try {
                path = item.getPath();
            } catch (RepositoryException e) {
                path = "-";
            }
            builder.append(state + "\t" + path + " (" + item + ")\n");
        }
        return builder.toString();
    }
}