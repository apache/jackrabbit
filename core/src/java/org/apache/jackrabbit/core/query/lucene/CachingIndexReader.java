/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.FilterIndexReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Implements an <code>IndexReader</code> that maintains caches to resolve
 * {@link IndexReader#termDocs(Term)} calls efficiently.
 * <p/>
 * The caches are:
 * <ul>
 * <li>idCache: maps UUID to document number</li>
 * <li>documentCache: maps document number to {@link Document} instance</li>
 * <li>parentCache: maps parentUUID to List of document numbers</li>
 * </ul>
 */
class CachingIndexReader extends FilterIndexReader {

    /**
     * The logger instance for this class.
     */
    private static final Logger log = Logger.getLogger(CachingIndexReader.class);

    /**
     * The document idCache. Maps UUIDs to document number.
     */
    private Map idCache;

    /**
     * The document cache. Maps document number to Document instance.
     */
    private Map documentCache;

    /**
     * The parent id cache. Maps parent UUID to List of document numbers.
     */
    private Map parentCache;

    /**
     * Creates a new <code>CachingIndexReader</code> based on
     * <code>delegatee</code>
     * @param delegatee the base <code>IndexReader</code>.
     */
    CachingIndexReader(IndexReader delegatee) {
        super(delegatee);
    }

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
            synchronized (this) {
                cacheInit();
                Integer docNo = (Integer) idCache.get(term.text());
                if (docNo == null) {
                    return EMPTY;
                } else {
                    return new CachingTermDocs(docNo);
                }
            }
        } else if (term.field() == FieldNames.PARENT) {
            synchronized (this) {
                cacheInit();
                List idList = (List) parentCache.get(term.text());
                if (idList == null) {
                    return EMPTY;
                } else {
                    return new CachingTermDocs(idList.iterator());
                }
            }
        } else {
            return super.termDocs(term);
        }
    }

    /**
     * Returns the stored fields of the <code>n</code><sup>th</sup>
     * <code>Document</code> in this index. This implementation returns cached
     * versions of <code>Document</code> instance. Thus, the returned document
     * must not be modified!
     *
     * @param n the document number.
     * @return the <code>n</code><sup>th</sup> <code>Document</code> in this
     *         index
     * @throws IOException              if an error occurs while reading from
     *                                  the index.
     * @throws IllegalArgumentException if the document with number
     *                                  <code>n</code> is deleted.
     */
    public Document document(int n) throws IOException, IllegalArgumentException {
        if (isDeleted(n)) {
            throw new IllegalArgumentException("attempt to access a deleted document");
        }
        synchronized (this) {
            cacheInit();
            return (Document) documentCache.get(new Integer(n));
        }
    }

    /**
     * Commits pending changes to disc.
     * @throws IOException if an error occurs while writing changes.
     */
    public void commitDeleted() throws IOException {
        commit();
    }

    /**
     * Provides an efficient lookup of document frequency for terms with field
     * {@link FieldNames#UUID} and {@link FieldNames#PARENT}. All other calles
     * are handled by the base class.
     *
     * @param t the term to look up the document frequency.
     * @return the document frequency of term <code>t</code>.
     * @throws IOException if an error occurs while reading from the index.
     */
    public int docFreq(Term t) throws IOException {
        synchronized (this) {
            cacheInit();
            if (t.field() == FieldNames.UUID) {
                return idCache.containsKey(t.text()) ? 1 : 0;
            } else if (t.field() == FieldNames.PARENT) {
                List children = (List) parentCache.get(t.text());
                return children == null ? 0 : children.size();
            }
        }
        return super.docFreq(t);
    }

    /**
     * Removes the <code>TermEnum</code> from the idCache and calls the base
     * <code>IndexReader</code>.
     * @param n the number of the document to delete.
     * @throws IOException if an error occurs while deleting the document.
     */
    protected synchronized void doDelete(int n) throws IOException {
        if (idCache != null) {
            Document d = (Document) documentCache.remove(new Integer(n));
            if (d != null) {
                idCache.remove(d.get(FieldNames.UUID));
                String parentUUID = d.get(FieldNames.PARENT);
                List parents = (List) parentCache.get(parentUUID);
                if (parents.size() == 1) {
                    parentCache.remove(parentUUID);
                } else {
                    // replace existing list, other threads might use iterator
                    // on existing list
                    List repl = new ArrayList(parents);
                    repl.remove(new Integer(n));
                    parentCache.put(parentUUID, repl);
                }
            }
        }
        super.doDelete(n);
    }

    /**
     * Initially fills the caches: idCache, documentCache, parentCache.
     * @throws IOException if an error occurs while reading from the index.
     */
    private void cacheInit() throws IOException {
        if (idCache == null) {
            long time = System.currentTimeMillis();
            Map ids = new HashMap(in.numDocs());
            Map documents = new HashMap(in.numDocs());
            Map parents = new HashMap(in.numDocs());
            for (int i = 0; i < in.maxDoc(); i++) {
                if (!in.isDeleted(i)) {
                    Document d = in.document(i);
                    Integer docId = new Integer(i);
                    if (ids.put(d.get(FieldNames.UUID), docId) != null) {
                        log.warn("Duplicate index entry for node: " + d.get(FieldNames.UUID));
                    }
                    documents.put(docId, d);
                    String parentUUID = d.get(FieldNames.PARENT);
                    List docIds = (List) parents.get(parentUUID);
                    if (docIds == null) {
                        docIds = new ArrayList();
                        parents.put(parentUUID, docIds);
                    }
                    docIds.add(docId);
                }
            }
            idCache = ids;
            documentCache = documents;
            parentCache = parents;
            time = System.currentTimeMillis() - time;
            log.debug("IndexReader cache populated in: " + time + " ms.");
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

    /**
     * Implements a <code>TermDocs</code> that takes a list of document
     * ids.
     */
    private static final class CachingTermDocs implements TermDocs {

        /**
         * The current document number.
         */
        private int current = -1;

        /**
         * Iterator over document numbers as <code>Integer</code> values.
         */
        private final Iterator docIds;

        /**
         * Creates a new <code>CachingTermDocs</code> instance with a single
         * document id.
         * @param docId the single document id.
         */
        CachingTermDocs(Integer docId) {
            this(Arrays.asList(new Integer[]{docId}).iterator());
        }

        /**
         * Creates a new <code>CachingTermDocs</code> instance that iterates
         * over the <code>docIds</code>.
         * @param docIds the actual document numbers / ids.
         */
        CachingTermDocs(Iterator docIds) {
            this.docIds = docIds;
        }

        /**
         * @throws UnsupportedOperationException always
         */
        public void seek(Term term) {
            throw new UnsupportedOperationException();
        }

        /**
         * @throws UnsupportedOperationException always
         */
        public void seek(TermEnum termEnum) {
            throw new UnsupportedOperationException();
        }


        /**
         * {@inheritDoc}
         */
        public int doc() {
            return current;
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
        public boolean next() {
            boolean next = docIds.hasNext();
            if (next) {
                current = ((Integer) docIds.next()).intValue();
            }
            return next;
        }

        /**
         * @throws UnsupportedOperationException always
         */
        public int read(int[] docs, int[] freqs) {
            throw new UnsupportedOperationException();
        }

        /**
         * @throws UnsupportedOperationException always
         */
        public boolean skipTo(int target) {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         */
        public void close() {
        }
    }
}
