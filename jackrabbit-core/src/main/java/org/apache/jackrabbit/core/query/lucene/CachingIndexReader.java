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

import org.apache.lucene.document.Document;
import org.apache.lucene.index.FilterIndexReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.jackrabbit.uuid.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.BitSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.text.NumberFormat;

import EDU.oswego.cs.dl.util.concurrent.Executor;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

/**
 * Implements an <code>IndexReader</code> that maintains caches to resolve
 * {@link #getParent(int, BitSet)} calls efficiently.
 * <p/>
 */
class CachingIndexReader extends FilterIndexReader {

    /**
     * The logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(CachingIndexReader.class);

    /**
     * The single thread of this executor initializes the
     * {@link #parents} when background initialization is requested.
     */
    private static final Executor SERIAL_EXECUTOR = new PooledExecutor(
            new LinkedQueue(), 1) {
        {
            setKeepAliveTime(500);
        }
    };

    /**
     * The current value of the global creation tick counter.
     */
    private static long currentTick;

    /**
     * Cache of nodes parent relation. If an entry in the array is not null,
     * that means the node with the document number = array-index has the node
     * with <code>DocId</code> as parent.
     */
    private final DocId[] parents;

    /**
     * Initializes the {@link #parents} cache.
     */
    private CacheInitializer cacheInitializer;

    /**
     * Tick when this index reader was created.
     */
    private final long creationTick = getNextCreationTick();

    /**
     * Document number cache if available. May be <code>null</code>.
     */
    private final DocNumberCache cache;

    /**
     * Creates a new <code>CachingIndexReader</code> based on
     * <code>delegatee</code>
     *
     * @param delegatee the base <code>IndexReader</code>.
     * @param cache     a document number cache, or <code>null</code> if not
     *                  available to this reader.
     * @param initCache if the {@link #parents} cache should be initialized
     *                  when this index reader is constructed. Otherwise
     *                  initialization happens in a background thread.
     */
    CachingIndexReader(IndexReader delegatee,
                       DocNumberCache cache,
                       boolean initCache) {
        super(delegatee);
        this.cache = cache;
        parents = new DocId[delegatee.maxDoc()];
        this.cacheInitializer = new CacheInitializer(delegatee);
        if (initCache) {
            cacheInitializer.run();
        } else {
            try {
                SERIAL_EXECUTOR.execute(cacheInitializer);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    /**
     * Returns the <code>DocId</code> of the parent of <code>n</code> or
     * {@link DocId#NULL} if <code>n</code> does not have a parent
     * (<code>n</code> is the root node).
     *
     * @param n the document number.
     * @param deleted the documents that should be regarded as deleted.
     * @return the <code>DocId</code> of <code>n</code>'s parent.
     * @throws IOException if an error occurs while reading from the index.
     */
    DocId getParent(int n, BitSet deleted) throws IOException {
        DocId parent;
        boolean existing = false;
        parent = parents[n];

        if (parent != null) {
            existing = true;

            // check if valid and reset if necessary
            if (!parent.isValid(deleted)) {
                if (log.isDebugEnabled()) {
                    log.debug(parent + " not valid anymore.");
                }
                parent = null;
            }
        }

        if (parent == null) {
            Document doc = document(n, FieldSelectors.UUID_AND_PARENT);
            String parentUUID = doc.get(FieldNames.PARENT);
            if (parentUUID == null || parentUUID.length() == 0) {
                parent = DocId.NULL;
            } else {
                // only create a DocId from document number if there is no
                // existing DocId
                if (!existing) {
                    Term id = new Term(FieldNames.UUID, parentUUID);
                    TermDocs docs = termDocs(id);
                    try {
                        while (docs.next()) {
                            if (!deleted.get(docs.doc())) {
                                parent = DocId.create(docs.doc());
                                break;
                            }
                        }
                    } finally {
                        docs.close();
                    }
                }

                // if still null, then parent is not in this index, or existing
                // DocId was invalid. thus, only allowed to create DocId from uuid
                if (parent == null) {
                    parent = DocId.create(parentUUID);
                }
            }

            // finally put to cache
            parents[n] = parent;
        }
        return parent;
    }

    /**
     * Returns the tick value when this reader was created.
     *
     * @return the creation tick for this reader.
     */
    public long getCreationTick() {
        return creationTick;
    }

    //--------------------< FilterIndexReader overwrites >----------------------

    /**
     * If the field of <code>term</code> is {@link FieldNames#UUID} this
     * <code>CachingIndexReader</code> returns a <code>TermDocs</code> instance
     * with a cached document id. If <code>term</code> has any other field
     * the call is delegated to the base <code>IndexReader</code>.<br/>
     * If <code>term</code> is for a {@link FieldNames#UUID} field and this
     * <code>CachingIndexReader</code> does not have such a document,
     * {@link #EMPTY} is returned.
     *
     * @param term the term to start the <code>TermDocs</code> enumeration.
     * @return a TermDocs instance.
     * @throws IOException if an error occurs while reading from the index.
     */
    public TermDocs termDocs(Term term) throws IOException {
        if (term.field() == FieldNames.UUID) {
            // check cache if we have one
            if (cache != null) {
                DocNumberCache.Entry e = cache.get(term.text());
                if (e != null) {
                    // check if valid
                    // the cache may contain entries from a different reader
                    // with the same uuid. that happens when a node is updated
                    // and is reindexed. the node 'travels' from an older index
                    // to a newer one. the cache will still contain a cache
                    // entry from the old until it is overwritten by the
                    // newer index.
                    if (e.creationTick == creationTick && !isDeleted(e.doc)) {
                        return new SingleTermDocs(e.doc);
                    }
                }

                // not in cache or invalid
                TermDocs docs = in.termDocs(term);
                try {
                    if (docs.next()) {
                        // put to cache
                        cache.put(term.text(), this, docs.doc());
                        // and return
                        return new SingleTermDocs(docs.doc());
                    } else {
                        return EMPTY;
                    }
                } finally {
                    docs.close();
                }
            }
        }
        return super.termDocs(term);
    }

    protected void doClose() throws IOException {
        try {
            cacheInitializer.waitUntilStopped();
        } catch (InterruptedException e) {
            // ignore
        }
        super.doClose();
    }

    //----------------------< internal >----------------------------------------

    /**
     * Returns the next creation tick value.
     *
     * @return the next creation tick value.
     */
    private static long getNextCreationTick() {
        synchronized (CachingIndexReader.class) {
            return currentTick++;
        }
    }

    /**
     * Initializes the {@link CachingIndexReader#parents} cache.
     */
    private class CacheInitializer implements Runnable {

        /**
         * From where to read.
         */
        private final IndexReader reader;

        /**
         * Set to <code>true</code> while this initializer does its work.
         */
        private boolean running = false;

        /**
         * Set to <code>true</code> when this index reader is about to be closed.
         */
        private volatile boolean stopRequested = false;

        /**
         * Creates a new initializer with the given <code>reader</code>.
         *
         * @param reader an index reader.
         */
        public CacheInitializer(IndexReader reader) {
            this.reader = reader;
        }

        /**
         * Initializes the cache.
         */
        public void run() {
            synchronized (this) {
                running = true;
            }
            try {
                if (stopRequested) {
                    // immediately return when stop is requested
                    return;
                }
                initializeParents(reader);
            } catch (Exception e) {
                // only log warn message during regular operation
                if (!stopRequested) {
                    log.warn("Error initializing parents cache.", e);
                }
            } finally {
                synchronized (this) {
                    running = false;
                    notifyAll();
                }
            }
        }

        /**
         * Waits until this cache initializer is stopped.
         *
         * @throws InterruptedException if the current thread is interrupted.
         */
        public void waitUntilStopped() throws InterruptedException {
            stopRequested = true;
            synchronized (this) {
                while (running) {
                    wait();
                }
            }
        }

        /**
         * Initializes the {@link CachingIndexReader#parents} <code>DocId</code>
         * array.
         *
         * @param reader the underlying index reader.
         * @throws IOException if an error occurs while reading from the index.
         */
        private void initializeParents(IndexReader reader) throws IOException {
            long time = System.currentTimeMillis();
            final Map docs = new HashMap();
            // read UUIDs
            collectTermDocs(reader, new Term(FieldNames.UUID, ""), new TermDocsCollector() {
                public void collect(Term term, TermDocs tDocs) throws IOException {
                    UUID uuid = UUID.fromString(term.text());
                    if (tDocs.next()) {
                        NodeInfo info = new NodeInfo(tDocs.doc(), uuid);
                        docs.put(new Integer(info.docId), info);
                    }
                }
            });

            // read PARENTs
            collectTermDocs(reader, new Term(FieldNames.PARENT, "0"), new TermDocsCollector() {
                public void collect(Term term, TermDocs tDocs) throws IOException {
                    while (tDocs.next()) {
                        UUID uuid = UUID.fromString(term.text());
                        Integer docId = new Integer(tDocs.doc());
                        NodeInfo info = (NodeInfo) docs.get(docId);
                        info.parent = uuid;
                        docs.remove(docId);
                        docs.put(info.uuid, info);
                    }
                }
            });

            if (stopRequested) {
                return;
            }

            double foreignParents = 0;
            Iterator it = docs.values().iterator();
            while (it.hasNext()) {
                NodeInfo info = (NodeInfo) it.next();
                NodeInfo parent = (NodeInfo) docs.get(info.parent);
                if (parent != null) {
                    parents[info.docId] = DocId.create(parent.docId);
                } else if (info.parent != null) {
                    foreignParents++;
                    parents[info.docId] = DocId.create(info.parent);
                } else {
                    // no parent -> root node
                    parents[info.docId] = DocId.NULL;
                }
            }
            if (log.isDebugEnabled()) {
                NumberFormat nf = NumberFormat.getPercentInstance();
                nf.setMaximumFractionDigits(1);
                time = System.currentTimeMillis() - time;
                if (parents.length > 0) {
                    foreignParents /= parents.length;
                }
                log.debug("initialized {} DocIds in {} ms, {} foreign parents",
                        new Object[]{
                            new Integer(parents.length),
                            new Long(time),
                            nf.format(foreignParents)
                        });
            }
        }

        /**
         * Collects term docs for a given start term. All terms with the same
         * field as <code>start</code> are enumerated.
         *
         * @param reader the index reader.
         * @param start the term where to start the term enumeration.
         * @param collector collects the term docs for each term.
         * @throws IOException if an error occurs while reading from the index.
         */
        private void collectTermDocs(IndexReader reader,
                                     Term start,
                                     TermDocsCollector collector)
                throws IOException {
            TermDocs tDocs = reader.termDocs();
            try {
                TermEnum terms = reader.terms(start);
                try {
                    int count = 0;
                    do {
                        Term t = terms.term();
                        if (t != null && t.field() == start.field()) {
                            tDocs.seek(terms);
                            collector.collect(t, tDocs);
                        } else {
                            break;
                        }
                        // once in a while check if we should quit
                        if (++count % 10000 == 0) {
                            if (stopRequested) {
                                break;
                            }
                        }
                    } while (terms.next());
                } finally {
                    terms.close();
                }
            } finally {
                tDocs.close();
            }
        }
    }

    /**
     * Simple interface to collect a term and its term docs.
     */
    private interface TermDocsCollector {

        /**
         * Called for each term encountered.
         *
         * @param term the term.
         * @param tDocs the term docs of <code>term</code>.
         * @throws IOException if an error occurs while reading from the index.
         */
        void collect(Term term, TermDocs tDocs) throws IOException;
    }

    private static class NodeInfo {

        final int docId;

        final UUID uuid;

        UUID parent;

        public NodeInfo(int docId, UUID uuid) {
            this.docId = docId;
            this.uuid = uuid;
        }
    }

    /**
     * Implements an empty TermDocs.
     */
    static final TermDocs EMPTY = new TermDocs() {

        public void seek(Term term) {
        }

        public void seek(TermEnum termEnum) {
        }

        public int doc() {
            return -1;
        }

        public int freq() {
            return -1;
        }

        public boolean next() {
            return false;
        }

        public int read(int[] docs, int[] freqs) {
            return 0;
        }

        public boolean skipTo(int target) {
            return false;
        }

        public void close() {
        }
    };
}
