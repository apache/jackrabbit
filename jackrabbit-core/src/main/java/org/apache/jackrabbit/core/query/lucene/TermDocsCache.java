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

import java.io.IOException;
import java.util.Map;
import java.util.Collections;
import java.util.BitSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>TermDocsCache</code> implements a cache for frequently read
 * {@link TermDocs}.
 */
public class TermDocsCache {

    /**
     * The logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(TermDocsCache.class);

    /**
     * The default cache size.
     */
    private static final int CACHE_SIZE = 10;

    /**
     * The underlying index reader.
     */
    private final IndexReader reader;

    /**
     * Only TermDocs for the given <code>field</code> are cached.
     */
    private final String field;

    /**
     * Map of {@link Term#text()} that are unknown to the underlying index.
     */
    private final Map<String, String> unknownValues = Collections.synchronizedMap(new LinkedHashMap<String, String>() {
        private static final long serialVersionUID = 1443679637070403838L;

        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > 100;
        }
    });

    /**
     * The cache of the {@link #CACHE_SIZE} most frequently requested TermDocs.
     * Maps term text <code>String</code> to {@link CacheEntry}.
     */
    private final Map<String, CacheEntry> cache = new LinkedHashMap<String, CacheEntry>();

    /**
     * Creates a new cache for the given <code>reader</code> and
     * <code>field</code>.
     *
     * @param reader the index reader.
     * @param field the field name of the terms to potentially cache.
     */
    public TermDocsCache(IndexReader reader, String field) {
        this.reader = reader;
        this.field = field;
    }

    /**
     * Returns the {@link TermDocs} for the given term.
     *
     * @param t the term.
     * @return the term docs for the given term.
     * @throws IOException if an error occurs while reading from the index.
     */
    public TermDocs termDocs(final Term t) throws IOException {
        if (t == null || t.field() != field) {
            return reader.termDocs(t);
        }

        String text = t.text();
        if (unknownValues.get(text) != null) {
            log.debug("EmptyTermDocs({},{})", field, text);
            return EmptyTermDocs.INSTANCE;
        }

        // maintain cache
        CacheEntry entry;
        synchronized (cache) {
            entry = cache.get(text);
            if (entry == null) {
                // check space
                if (cache.size() >= CACHE_SIZE) {
                    // prune half of them and adjust the rest
                    CacheEntry[] entries = cache.values().toArray(
                            new CacheEntry[cache.size()]);
                    Arrays.sort(entries);
                    int threshold = entries[CACHE_SIZE / 2].numAccessed;
                    for (Iterator<Map.Entry<String, CacheEntry>> it = cache.entrySet().iterator(); it.hasNext(); ) {
                        Map.Entry<String, CacheEntry> e = it.next();
                        if (e.getValue().numAccessed <= threshold) {
                            // prune
                            it.remove();
                        } else {
                            // adjust
                            CacheEntry ce = e.getValue();
                            ce.numAccessed = (int) Math.sqrt(ce.numAccessed);
                        }
                    }
                }
                entry = new CacheEntry();
                cache.put(text, entry);
            } else {
                entry.numAccessed++;
            }
        }

        // this is a threshold to prevent caching of TermDocs
        // that are read only irregularly.
        if (entry.numAccessed < 10) {
            if (log.isDebugEnabled()) {
                log.debug("#{} TermDocs({},{})",
                        new Object[]{entry.numAccessed, field, text});
            }
            return reader.termDocs(t);
        }

        if (entry.bits == null) {
            // collect bits
            BitSet bits = null;
            TermDocs tDocs = reader.termDocs(t);
            try {
                while (tDocs.next()) {
                    if (bits == null) {
                        bits = new BitSet(reader.maxDoc());
                    }
                    bits.set(tDocs.doc());
                }
            } finally {
                tDocs.close();
            }
            if (bits != null) {
                entry.bits = bits;
            }
        }

        if (entry.bits == null) {
            // none collected
            unknownValues.put(text, text);
            return EmptyTermDocs.INSTANCE;
        } else {
            if (log.isDebugEnabled()) {
                log.debug("CachedTermDocs({},{},{}/{})", new Object[]{
                        field, text, entry.bits.cardinality(), reader.maxDoc()});
            }
            return new CachedTermDocs(entry.bits);
        }
    }

    /**
     * Implements a {@link TermDocs} base on a {@link BitSet}.
     */
    private static final class CachedTermDocs implements TermDocs {

        /**
         * The cached docs for this term.
         */
        private final BitSet docs;

        /**
         * The current position into the {@link #docs}.
         */
        private int position = -1;

        /**
         * <code>true</code> if there are potentially more docs.
         */
        private boolean moreDocs = true;

        public CachedTermDocs(BitSet docs) {
            this.docs = docs;
        }

        /**
         * @throws UnsupportedOperationException always.
         */
        public void seek(Term term) throws IOException {
            throw new UnsupportedOperationException();
        }

        /**
         * @throws UnsupportedOperationException always.
         */
        public void seek(TermEnum termEnum) throws IOException {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         */
        public int doc() {
            return position;
        }

        /**
         * {@inheritDoc}
         */
        public int freq() {
            return 1;
        }

        /**
         * {@inheritDoc}
         */
        public boolean next() throws IOException {
            if (moreDocs) {
                position = docs.nextSetBit(position + 1);
                moreDocs = position != -1;
            }
            return moreDocs;
        }

        /**
         * {@inheritDoc}
         */
        public int read(int[] docs, int[] freqs) throws IOException {
            int count;
            for (count = 0; count < docs.length && next(); count++) {
                docs[count] = doc();
                freqs[count] = 1;
            }
            return count;
        }

        /**
         * {@inheritDoc}
         */
        public boolean skipTo(int target) throws IOException {
            if (moreDocs) {
                position = docs.nextSetBit(target);
                moreDocs = position != -1;
            }
            return moreDocs;
        }

        /**
         * {@inheritDoc}
         */
        public void close() throws IOException {
        }
    }

    private static final class CacheEntry implements Comparable<CacheEntry> {

        private volatile int numAccessed = 1;

        private volatile BitSet bits;

        public int compareTo(CacheEntry other) {
            return (numAccessed < other.numAccessed ? -1 : (numAccessed == other.numAccessed ? 0 : 1));
        }
    }
}
