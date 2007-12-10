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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.BitSet;

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
     */
    CachingIndexReader(IndexReader delegatee, DocNumberCache cache) {
        super(delegatee);
        this.cache = cache;
        parents = new DocId[delegatee.maxDoc()];
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
            Document doc = document(n);
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
