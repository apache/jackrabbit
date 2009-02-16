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

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
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
     * Expert: Stores term text values and document ordering data.
     */
    public static class StringIndex {

        /**
         * Some heuristic factor that determines whether the array is sparse. Note that if less then
         * 1% is set, we already count the array as sparse. This is because it will become memory consuming
         * quickly by keeping the (sparse) arrays 
         */
        private static final int SPARSE_FACTOR = 100;

        /**
         * Terms indexed by document id.
         */
        private final String[] terms;

        /**
         * Terms map indexed by document id.
         */
        public final Map termsMap;

        /**
         * Boolean indicating whether the hashMap impl has to be used
         */
        public final boolean sparse;

        /**
         * Creates one of these objects
         */
        public StringIndex(String[] terms, int setValues) {
            if (isSparse(terms, setValues)) {
                this.sparse = true;
                this.terms = null;
                if (setValues == 0) {
                    this.termsMap = null;
                } else {
                    this.termsMap = getTermsMap(terms, setValues);
                }
            } else {
                this.sparse = false;
                this.terms = terms;
                this.termsMap = null;
            }
        }

        public String getTerm(int i) {
            if (sparse) {
                return termsMap == null ? null : (String) termsMap.get(new Integer(i));
            } else {
                return terms[i];
            }
        }

        private Map getTermsMap(String[] terms, int setValues) {
            Map map = new HashMap(setValues);
            for (int i = 0; i < terms.length && setValues > 0; i++) {
                if (terms[i] != null) {
                    map.put(new Integer(i), terms[i]);
                    setValues--;
                }
            }
            return map;
        }

        private boolean isSparse(String[] terms, int setValues) {
            // some really simple test to test whether the array is sparse. Currently, when less then 1% is set, the array is already sparse 
            // for this typical cache to avoid memory issues
            if (setValues * SPARSE_FACTOR < terms.length) {
                return true;
            }
            return false;
        }
    }

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
     * This method is an adapted version of: <code>FieldCacheImpl.getStringIndex()</code>
     * The returned string index will <b>not</b> have a term lookup array!
     * See {@link SharedFieldSortComparator} for more info.
     *
     * @param reader     the <code>IndexReader</code>.
     * @param field      name of the shared field.
     * @param prefix     the property name, will be used as term prefix.
     * @param comparator the sort comparator instance.
     * @return a StringIndex that contains the field values and order
     *         information.
     * @throws IOException if an error occurs while reading from the index.
     */
    public SharedFieldCache.StringIndex getStringIndex(IndexReader reader,
                                                 String field,
                                                 String prefix,
                                                 SortComparator comparator)
            throws IOException {

        if (reader instanceof ReadOnlyIndexReader) {
            reader = ((ReadOnlyIndexReader) reader).getBase();
        }

        field = field.intern();
        SharedFieldCache.StringIndex ret = lookup(reader, field, prefix, comparator);
        if (ret == null) {
            final String[] retArray = new String[reader.maxDoc()];
            int setValues = 0;
            if (retArray.length > 0) {
                TermDocs termDocs = reader.termDocs();
                TermEnum termEnum = reader.terms(new Term(field, prefix));

                char[] tmp = new char[16];
                try {
                    if (termEnum.term() == null) {
                        throw new RuntimeException("no terms in field " + field);
                    }
                    do {
                        Term term = termEnum.term();
                        if (term.field() != field || !term.text().startsWith(prefix)) {
                            break;
                        }

                        // make sure term is compacted
                        String text = term.text();
                        int len = text.length() - prefix.length();
                        if (tmp.length < len) {
                            // grow tmp
                            tmp = new char[len];
                        }
                        text.getChars(prefix.length(), text.length(), tmp, 0);
                        String value = new String(tmp, 0, len);

                        termDocs.seek(termEnum);
                        while (termDocs.next()) {
                            setValues++;
                            retArray[termDocs.doc()] = value;
                        }
                    } while (termEnum.next());
                } finally {
                    termDocs.close();
                    termEnum.close();
                }
            }
            SharedFieldCache.StringIndex value = new SharedFieldCache.StringIndex(retArray, setValues);
            store(reader, field, prefix, comparator, value);
            return value;
        }
        return ret;
    }

    /**
     * See if a <code>StringIndex</code> object is in the cache.
     */
    SharedFieldCache.StringIndex lookup(IndexReader reader, String field,
                                  String prefix, SortComparator comparer) {
        Key key = new Key(field, prefix, comparer);
        synchronized (this) {
            HashMap readerCache = (HashMap) cache.get(reader);
            if (readerCache == null) {
                return null;
            }
            return (SharedFieldCache.StringIndex) readerCache.get(key);
        }
    }

    /**
     * Put a <code>StringIndex</code> <code>value</code> to cache.
     */
    Object store(IndexReader reader, String field, String prefix,
                 SortComparator comparer, SharedFieldCache.StringIndex value) {
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
