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
package org.apache.jackrabbit.core.search.lucene;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.FilterIndexReader;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.document.Document;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Implements an <code>IndexReader</code> that caches document ids for the
 * {@link FieldNames.UUID} field.
 */
class CachingIndexReader extends FilterIndexReader {

    /**
     * The document id cache. Maps UUIDs to document number.
     */
    private Map cache;

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
                final Integer docNo = (Integer) cache.get(term.text());
                if (docNo == null) {
                    return EMPTY;
                } else {
                    return new TermDocs() {

                        private boolean consumed = false;

                        private int doc = -1;

                        public void seek(Term term) {
                            throw new UnsupportedOperationException();
                        }

                        public void seek(TermEnum termEnum) {
                            throw new UnsupportedOperationException();
                        }

                        public int doc() {
                            return doc;
                        }

                        public int freq() {
                            return 1;
                        }

                        public boolean next() {
                            if (consumed) {
                                return false;
                            } else {
                                doc = docNo.intValue();
                                consumed = true;
                                return true;
                            }
                        }

                        public int read(int[] docs, int[] freqs) {
                            docs[0] = docNo.intValue();
                            freqs[0] = 1;
                            return 1;
                        }

                        public boolean skipTo(int target) {
                            throw new UnsupportedOperationException();
                        }

                        public void close() {
                        }
                    };
                }
            }
        } else {
            return super.termDocs(term);
        }
    }

    /**
     * Removes the <code>TermEnum</code> from the cache and calls the base
     * <code>IndexReader</code>.
     * @param n the number of the document to delete.
     * @throws IOException if an error occurs while deleting the document.
     */
    protected synchronized void doDelete(int n) throws IOException {
        if (cache != null) {
            // todo keep a second map which contains the doc number to UUID mapping?
            for (Iterator it = cache.values().iterator(); it.hasNext();) {
                if (((Integer) it.next()).intValue() == n) {
                    it.remove();
                    break;
                }
            }
        }
        super.doDelete(n);
    }

    /**
     * Initially fills the cache with all the UUID to document number mappings.
     * @throws IOException if an error occurs while reading from the index.
     */
    private void cacheInit() throws IOException {
        if (cache == null) {
            Map tmp = new HashMap();
            for (int i = 0; i < in.maxDoc(); i++) {
                if (!in.isDeleted(i)) {
                    Document d = in.document(i);
                    tmp.put(d.get(FieldNames.UUID), new Integer(i));
                }
            }
            cache = tmp;
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
