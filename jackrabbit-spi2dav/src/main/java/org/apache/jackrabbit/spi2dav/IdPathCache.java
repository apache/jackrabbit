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
package org.apache.jackrabbit.spi2dav;

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.jackrabbit.spi.ItemId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>IdPathCache</code> ...
 */
class IdPathCache {

    private static Logger log = LoggerFactory.getLogger(IdPathCache.class);

    /**
     * @see <a href="https://issues.apache.org/jira/browse/JCR-3305">JCR-3305</a>: limit cache size
     */
    private static final int CACHESIZE = 10000;

    private Map<ItemId, String> idToPathCache;
    private Map<String, ItemId> pathToIdCache;

    public IdPathCache() {
        idToPathCache = new LinkedHashMap<ItemId, String>(CACHESIZE, 1) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<ItemId, String> eldest) {
                return this.size() > CACHESIZE;
            }
        };
        pathToIdCache = new LinkedHashMap<String, ItemId>(CACHESIZE, 1) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, ItemId> eldest) {
                return this.size() > CACHESIZE;
            }
        };
    }

    public ItemId getItemId(String path) {
        return pathToIdCache.get(path);
    }

    public String getPath(ItemId itemId) {
        return idToPathCache.get(itemId);
    }

    public boolean containsPath(String path) {
        return pathToIdCache.containsKey(path);
    }

    public boolean containsItemId(ItemId itemId) {
        return idToPathCache.containsKey(itemId);
    }

    public void add(String path, ItemId itemId) {
        pathToIdCache.put(path, itemId);
        idToPathCache.put(itemId, path);
        log.debug("Added: ItemId = " + itemId + " PATH = {}", path);
    }

    public void remove(String path) {
        ItemId itemId = pathToIdCache.remove(path);
        if (itemId != null) {
            idToPathCache.remove(itemId);
        }
        log.debug("Removed: ItemId = " + itemId + " PATH = {}", path);
    }

    public void remove(ItemId itemId) {
        String path = idToPathCache.remove(itemId);
        if (path != null) {
            pathToIdCache.remove(path);
        }
        log.debug("Removed: ItemId = " + itemId + " PATH = {}", path);
    }

    public void clear() {
        idToPathCache.clear();
        pathToIdCache.clear();
    }
}
