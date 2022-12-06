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
package org.apache.jackrabbit.core.query.lucene;

import org.apache.commons.collections4.map.LRUMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements a Document number cache with a fixed size and a LRU strategy.
 */
final class DocNumberCache {

    /**
     * Logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(DocNumberCache.class);

    /**
     * Log cache statistics at most every 10 seconds.
     */
    private static final long LOG_INTERVAL = 1000 * 10;

    /**
     * The number of cache segments.
     */
    private static final int CACHE_SEGMENTS = 0x10;

    /**
     * Mask to calculate segment number.
     */
    private static final int CACHE_SEGMENTS_MASK = CACHE_SEGMENTS - 1;

    /**
     * LRU Maps where key=uuid value=reader;docNumber
     */
    @SuppressWarnings("unchecked")
    private final LRUMap<String, Entry>[] docNumbers = new LRUMap[CACHE_SEGMENTS];

    /**
     * Timestamp of the last cache statistics log.
     */
    private long lastLog;

    /**
     * Cache misses.
     */
    private long misses;

    /**
     * Cache accesses;
     */
    private long accesses;

    /**
     * Creates a new <code>DocNumberCache</code> with a limiting
     * <code>size</code>.
     *
     * @param size the cache limit.
     */
    DocNumberCache(int size) {
        size = size / CACHE_SEGMENTS;
        if (size < 0x40) {
            // minimum size is 0x40 * 0x10 = 1024
            size = 0x40;
        }
        for (int i = 0; i < docNumbers.length; i++) {
            docNumbers[i] = new LRUMap<>(size);
        }
    }

    /**
     * Puts a document number into the cache using a uuid as key. An entry is
     * only overwritten if the according reader is younger than the reader
     * associated with the existing entry.
     *
     * @param uuid the key.
     * @param reader the index reader from where the document number was read.
     * @param n the document number.
     */
    void put(String uuid, CachingIndexReader reader, int n) {
        LRUMap<String, Entry> cacheSegment = docNumbers[getSegmentIndex(uuid.charAt(0))];
        synchronized (cacheSegment) {
            Entry e = cacheSegment.get(uuid);
            if (e != null) {
                // existing entry
                // ignore if reader is older than the one in entry
                if (reader.getCreationTick() <= e.creationTick) {
                    if (log.isDebugEnabled()) {
                        log.debug("Ignoring put(). New entry is not from a newer reader. "
                                + "existing: " + e.creationTick
                                + ", new: " + reader.getCreationTick());
                    }
                    e = null;
                }
            } else {
                // entry did not exist
                e = new Entry(reader.getCreationTick(), n);
            }

            if (e != null) {
                cacheSegment.put(uuid, e);
            }
        }
    }

    /**
     * Returns the cache entry for <code>uuid</code>, or <code>null</code> if
     * no entry exists for <code>uuid</code>.
     *
     * @param uuid the key.
     * @return cache entry or <code>null</code>.
     */
    Entry get(String uuid) {
        LRUMap<String, Entry> cacheSegment = docNumbers[getSegmentIndex(uuid.charAt(0))];
        Entry entry;
        synchronized (cacheSegment) {
            entry = cacheSegment.get(uuid);
        }
        if (log.isInfoEnabled()) {
            accesses++;
            if (entry == null) {
                misses++;
            }
            // log at most after 1000 accesses and every 10 seconds
            if (accesses > 1000 && System.currentTimeMillis() - lastLog > LOG_INTERVAL) {
                long ratio = 100;
                if (misses != 0) {
                    ratio -= misses * 100L / accesses;
                }
                StringBuffer statistics = new StringBuffer();
                int inUse = 0;
                for (LRUMap<String, Entry> docNumber : docNumbers) {
                    inUse += docNumber.size();
                }
                statistics.append("size=").append(inUse);
                statistics.append("/").append(docNumbers[0].maxSize() * CACHE_SEGMENTS);
                statistics.append(", #accesses=").append(accesses);
                statistics.append(", #hits=").append((accesses - misses));
                statistics.append(", #misses=").append(misses);
                statistics.append(", cacheRatio=").append(ratio).append("%");
                log.info(statistics.toString());
                accesses = 0;
                misses = 0;
                lastLog = System.currentTimeMillis();
            }
        }
        return entry;
    }

    /**
     * Returns the segment index for character c.
     */
    private static int getSegmentIndex(char c) {
        if (c > '9') {
            c += 9;
        }
        return c & CACHE_SEGMENTS_MASK;
    }

    public static final class Entry {

        /**
         * The creation tick of the IndexReader.
         */
        final long creationTick;

        /**
         * The document number.
         */
        final int doc;

        Entry(long creationTick, int doc) {
            this.creationTick = creationTick;
            this.doc = doc;
        }
    }
}
