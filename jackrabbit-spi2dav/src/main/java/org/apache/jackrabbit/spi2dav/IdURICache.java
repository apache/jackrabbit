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
        idToUriCache = new LRULinkedHashMap<>(CACHESIZE, 1);
        uriToIdCache = new LRULinkedHashMap<>(CACHESIZE, 1);
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
        String cleanUri = checkedIsUnderWorkspace(getCleanUri(uri));
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

    private String checkedIsUnderWorkspace(String uri) {
        if (uri.startsWith(workspaceUri)) {
            return uri;
        } else {
            int ml = Math.max(uri.length(), workspaceUri.length());
            int match = 0;
            for (int i = 0; i < ml; i++) {
                if (uri.charAt(i) != workspaceUri.charAt(i)) {
                    break;
                }
                match = i;
            }
            String diags = "";
            if (uri.length() > match) {
                String expected = (workspaceUri.length() > match) ? String.format(", expected: '%s'", workspaceUri.substring(match + 1)): ""; 
                diags = String.format(" (position %d: '{%s}%s'%s)", match, uri.substring(0, match + 1), uri.substring(match + 1), expected);
            }
            throw new IllegalArgumentException("Workspace mismatch: '" + uri + "' not under workspace '" + workspaceUri + "'" + diags);
        }
    }

    private static String getCleanUri(String uri) {
        if (uri.endsWith("/")) {
            return uri.substring(0, uri.length() - 1);
        } else {
            return uri;
        }
    }

    private class LRULinkedHashMap<K, V> extends LinkedHashMap<K, V> {

        private static final long serialVersionUID = 4463208266433931306L;

        private int capacity;

        LRULinkedHashMap(int capacity, float loadFactor) {
            super(capacity, loadFactor);
            this.capacity = capacity;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return this.size() > this.capacity;
        }
    }
}
