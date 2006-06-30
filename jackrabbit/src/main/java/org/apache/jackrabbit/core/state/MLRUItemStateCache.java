/*
 * $URL$
 * $Id$
 *
 * Copyright 1997-2006 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package org.apache.jackrabbit.core.state;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.collections.map.LinkedMap;
import org.apache.jackrabbit.core.ItemId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.Collections;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * An <code>ItemStateCache</code> implementation that internally uses a
 * {@link LRUMap} to maintain a cache of <code>ItemState</code> objects. the
 * cache uses a rough estimate of the memory consuption of the cache item
 * states for calculating the maximum number of entries.
 */
public class MLRUItemStateCache implements ItemStateCache {
    /** Logger instance */
    private static Logger log = LoggerFactory.getLogger(LRUItemStateCache.class);

    /** default maximum memory to use */
    public static final int DEFAULT_MAX_MEM = 8 * 1024 * 1024;

    /** the amount of memory the entries use */
    private long totalMem;

    /** the maximum of memory the cache may use */
    private final long maxMem;

    /**
     * A cache for <code>ItemState</code> instances
     */
    private final LinkedMap cache = new LinkedMap();

    /**
     * Constructs a new, empty <code>ItemStateCache</code> with a maximum amount
     * of memory of {@link #DEFAULT_MAX_MEM}.
     */
    public MLRUItemStateCache() {
        this(DEFAULT_MAX_MEM);
    }

    /**
     * Constructs a new, empty <code>ItemStateCache</code> with the specified
     * maximum memory.
     *
     * @param maxMem the maximum amount of memory this cache may use.
     */
    public MLRUItemStateCache(int maxMem) {
        this.maxMem = maxMem;
    }

    //-------------------------------------------------------< ItemStateCache >
    /**
     * {@inheritDoc}
     */
    public boolean isCached(ItemId id) {
        synchronized (cache) {
            return cache.containsKey(id);
        }
    }

    /**
     * {@inheritDoc}
     */
    public ItemState retrieve(ItemId id) {
        synchronized (cache) {
            Entry entry = (Entry) cache.remove(id);
            if (entry != null) {
                // 'touch' item, by adding at end of list
                cache.put(id, entry);
                return entry.state;
            } else {
                return null;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void cache(ItemState state) {
        synchronized (cache) {
            ItemId id = state.getId();
            if (cache.containsKey(id)) {
                log.warn("overwriting cached entry " + id);
                evict(id);
            }
            Entry entry = new Entry(state);
            cache.put(id, entry);
            totalMem += entry.size;
            // remove items, if too many
            while (totalMem > maxMem) {
                id = (ItemId) cache.firstKey();
                evict(id);
            }
            if (log.isDebugEnabled()) {
                log.info(this + " size=" + cache.size() + ", " + totalMem + "/" + maxMem);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void evict(ItemId id) {
        synchronized (cache) {
            Entry entry = (Entry) cache.remove(id);
            if (entry != null) {
                totalMem -= entry.size;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void evictAll() {
        synchronized (cache) {
            cache.clear();
            totalMem = 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        synchronized (cache) {
            return cache.isEmpty();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        synchronized (cache) {
            return cache.size();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Set keySet() {
        synchronized (cache) {
            return Collections.unmodifiableSet(cache.keySet());
        }
    }

    /**
     * {@inheritDoc}
     */
    public Collection values() {
        synchronized (cache) {
            ArrayList list = new ArrayList(cache.size());
            Iterator iter = cache.values().iterator();
            while (iter.hasNext()) {
                Entry entry = (Entry) iter.next();
                list.add(entry.state);
            }
            return list;
        }
    }

    /**
     * Internal cache entry
     */
    private static class Entry {

        private final ItemState state;

        private final long size;

        public Entry(ItemState state) {
            this.state = state;
            this.size = 64 + state.getMemoryConsumption();
        }
    }
}
