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

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.FieldCache;
import org.apache.lucene.search.SortComparator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Implements a variant of the lucene class <code>org.apache.lucene.search.FieldCacheImpl</code>.
 * The lucene FieldCache class has some sort of support for custom comparators
 * but it only works on the basis of a field name. There is no further control
 * over the terms to iterate, that's why we use our own implementation.
 */
class SharedFieldCache {

    /**
     * Reference to the single instance of <code>SharedFieldCache</code>.
     */
    public static final SharedFieldCache INSTANCE = new SharedFieldCache();

    /**
     * The internal cache. Maps Entry to array of interpreted term values.
     */
    private final Map cache = new WeakHashMap();

    /**
     * Private constructor.
     */
    private SharedFieldCache() {
    }

    /**
     * Creates a <code>StringIndex</code> for a <code>field</code> and a term
     * <code>prefix</code>. The term prefix acts as the property name for the
     * shared <code>field</code>.
     * <p/>
     * This method is an adapted version of: <code>org.apache.lucene.search.FieldCacheImpl.getStringIndex()</code>
     *
     * @param reader     the <code>IndexReader</code>.
     * @param field      name of the shared field.
     * @param prefix     the property name, will be used as term prefix.
     * @param comparator the sort comparator instance.
     * @return a StringIndex that contains the field values and order
     *         information.
     * @throws IOException if an error occurs while reading from the index.
     */
    public FieldCache.StringIndex getStringIndex(IndexReader reader,
                                                 String field,
                                                 String prefix,
                                                 SortComparator comparator)
            throws IOException {
        field = field.intern();
        FieldCache.StringIndex ret = lookup(reader, field, prefix, comparator);
        if (ret == null) {
            final int[] retArray = new int[reader.maxDoc()];
            String[] mterms = new String[reader.maxDoc() + 1];
            if (retArray.length > 0) {
                TermDocs termDocs = reader.termDocs();
                TermEnum termEnum = reader.terms(new Term(field, prefix));
                int t = 0;  // current term number

                // an entry for documents that have no terms in this field
                // should a document with no terms be at top or bottom?
                // this puts them at the top - if it is changed, FieldDocSortedHitQueue
                // needs to change as well.
                mterms[t++] = null;

                try {
                    if (termEnum.term() == null) {
                        throw new RuntimeException("no terms in field " + field);
                    }
                    do {
                        Term term = termEnum.term();
                        if (term.field() != field || !term.text().startsWith(prefix)) {
                            break;
                        }

                        // store term text
                        // we expect that there is at most one term per document
                        if (t >= mterms.length) {
                            throw new RuntimeException("there are more terms than documents in field \"" + field + "\"");
                        }
                        mterms[t] = term.text();

                        termDocs.seek(termEnum);
                        while (termDocs.next()) {
                            retArray[termDocs.doc()] = t;
                        }

                        t++;
                    } while (termEnum.next());
                } finally {
                    termDocs.close();
                    termEnum.close();
                }

                if (t == 0) {
                    // if there are no terms, make the term array
                    // have a single null entry
                    mterms = new String[1];
                } else if (t < mterms.length) {
                    // if there are less terms than documents,
                    // trim off the dead array space
                    String[] terms = new String[t];
                    System.arraycopy(mterms, 0, terms, 0, t);
                    mterms = terms;
                }
            }
            FieldCache.StringIndex value = new FieldCache.StringIndex(retArray, mterms);
            store(reader, field, prefix, comparator, value);
            return value;
        }
        return ret;
    }

    /**
     * See if a <code>StringIndex</code> object is in the cache.
     */
    FieldCache.StringIndex lookup(IndexReader reader, String field,
                                  String prefix, SortComparator comparer) {
        Key key = new Key(field, prefix, comparer);
        synchronized (this) {
            HashMap readerCache = (HashMap) cache.get(reader);
            if (readerCache == null) {
                return null;
            }
            return (FieldCache.StringIndex) readerCache.get(key);
        }
    }

    /**
     * Put a <code>StringIndex</code> <code>value</code> to cache.
     */
    Object store(IndexReader reader, String field, String prefix,
                 SortComparator comparer, FieldCache.StringIndex value) {
        Key key = new Key(field, prefix, comparer);
        synchronized (this) {
            HashMap readerCache = (HashMap) cache.get(reader);
            if (readerCache == null) {
                readerCache = new HashMap();
                cache.put(reader, readerCache);
            }
            return readerCache.put(key, value);
        }
    }

    /**
     * A compound <code>Key</code> that consist of <code>field</code>
     * <code>prefix</code> and <code>comparator</code>.
     */
    static class Key {

        private final String field;
        private final String prefix;
        private final SortComparator comparator;

        /**
         * Creates <code>Key</code> for StringIndex lookup.
         */
        Key(String field, String prefix, SortComparator comparator) {
            this.field = field.intern();
            this.prefix = prefix.intern();
            this.comparator = comparator;
        }

        /**
         * Returns <code>true</code> if <code>o</code> is a <code>Key</code>
         * instance and refers to the same field, prefix and comparator object.
         */
        public boolean equals(Object o) {
            if (o instanceof Key) {
                Key other = (Key) o;
                return other.field == field
                        && other.prefix == prefix
                        && other.comparator.equals(comparator);
            }
            return false;
        }

        /**
         * Composes a hashcode based on the field, prefix and comparator.
         */
        public int hashCode() {
            return field.hashCode() ^ prefix.hashCode() ^ comparator.hashCode();
        }
    }

}
