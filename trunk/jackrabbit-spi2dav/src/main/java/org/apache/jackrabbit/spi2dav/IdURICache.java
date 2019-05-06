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
 * <code>IdURICache</code>...
 */
class IdURICache {
    private static Logger log = LoggerFactory.getLogger(IdURICache.class);

    /**
     * @see <a href="https://issues.apache.org/jira/browse/JCR-3305">JCR-3305</a>: limit cache size
     */
    private static final int CACHESIZE = 10000;

    private final String workspaceUri;
    private Map<ItemId, String> idToUriCache;
    private Map<String, ItemId> uriToIdCache;

    IdURICache(String workspaceUri) {
        this.workspaceUri = workspaceUri;
        idToUriCache = new LinkedHashMap<ItemId, String>(CACHESIZE, 1) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<ItemId, String> eldest) {
                return this.size() > CACHESIZE;
            }
        };
        uriToIdCache = new LinkedHashMap<String, ItemId>(CACHESIZE, 1) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, ItemId> eldest) {
                return this.size() > CACHESIZE;
            }
        };
    }

    public ItemId getItemId(String uri) {
        return uriToIdCache.get(getCleanUri(uri));
    }

    public String getUri(ItemId itemId) {
        return idToUriCache.get(itemId);
    }

    public boolean containsUri(String uri) {
        return uriToIdCache.containsKey(getCleanUri(uri));
    }

    public boolean containsItemId(ItemId itemId) {
        return idToUriCache.containsKey(itemId);
    }

    public void add(String uri, ItemId itemId) {
        if (!uri.startsWith(workspaceUri)) {
            throw new IllegalArgumentException("Workspace mismatch: '" + uri + "' not under '" + workspaceUri + "'");
        }
        String cleanUri = getCleanUri(uri);
        uriToIdCache.put(cleanUri, itemId);
        idToUriCache.put(itemId, cleanUri);
        log.debug("Added: ItemId = " + itemId + " URI = " + cleanUri);
    }

    public void remove(String uri) {
        String cleanUri = getCleanUri(uri);
        ItemId itemId = uriToIdCache.remove(cleanUri);
        if (itemId != null) {
            idToUriCache.remove(itemId);
        }
        log.debug("Removed: ItemId = " + itemId + " URI = " + cleanUri);
    }

    public void remove(ItemId itemId) {
        String uri = idToUriCache.remove(itemId);
        if (uri != null) {
            uriToIdCache.remove(uri);
        }
        log.debug("Removed: ItemId = " + itemId + " URI = " + uri);
    }

    public void clear() {
        idToUriCache.clear();
        uriToIdCache.clear();
    }

    private static String getCleanUri(String uri) {
        if (uri.endsWith("/")) {
            return uri.substring(0, uri.length() - 1);
        } else {
            return uri;
        }
    }
}
