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
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermPositions;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.SortComparatorSource;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import javax.jcr.PropertyType;

/**
 * Implements a variant of the lucene class <code>org.apache.lucene.search.FieldCacheImpl</code>.
 * The lucene FieldCache class has some sort of support for custom comparators
 * but it only works on the basis of a field name. There is no further control
 * over the terms to iterate, that's why we use our own implementation.
 */
public class SharedFieldCache {

    /**
     * Expert: Stores term text values and document ordering data.
     */
    public static class ValueIndex {

        /**
         * Some heuristic factor that determines whether the array is sparse. Note that if less then
         * 1% is set, we already count the array as sparse. This is because it will become memory consuming
         * quickly by keeping the (sparse) arrays 
         */
        private static final int SPARSE_FACTOR = 100;

        /**
         * Values indexed by document id.
         */
        private final Comparable[] values;

        /**
         * Values (Comparable) map indexed by document id.
         */
        public final Map<Integer, Comparable> valuesMap;

        /**
         * Boolean indicating whether the {@link #valuesMap} impl has to be used
         */
        public final boolean sparse;

        /**
         * Creates one of these objects
         */
        public ValueIndex(Comparable[] values, int setValues) {
            if (isSparse(values, setValues)) {
                this.sparse = true;
                this.values = null;
                if (setValues == 0) {
                    this.valuesMap = null;
                } else {
                    this.valuesMap = getValuesMap(values, setValues);
                }
            } else {
                this.sparse = false;
                this.values = values;
                this.valuesMap = null;
            }
        }

        public Comparable getValue(int i) {
            if (sparse) {
                return valuesMap == null ? null : valuesMap.get(i);
            } else {
                return values[i];
            }
        }

        private Map<Integer, Comparable> getValuesMap(Comparable[] values, int setValues) {
            Map<Integer, Comparable> map = new HashMap<Integer, Comparable>(setValues);
            for (int i = 0; i < values.length && setValues > 0; i++) {
                if (values[i] != null) {
                    map.put(i, values[i]);
                    setValues--;
                }
            }
            return map;
        }

        private boolean isSparse(Comparable[] values, int setValues) {
            // some really simple test to test whether the array is sparse. Currently, when less then 1% is set, the array is already sparse 
            // for this typical cache to avoid memory issues
            if (setValues * SPARSE_FACTOR < values.length) {
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
    private final Map<IndexReader, Map<Key, ValueIndex>> cache = new WeakHashMap<IndexReader, Map<Key, ValueIndex>>();

    /**
     * Private constructor.
     */
    private SharedFieldCache() {
    }

    /**
     * Creates a <code>ValueIndex</code> for a <code>field</code> and a term
     * <code>prefix</code>. The term prefix acts as the property name for the
     * shared <code>field</code>.
     * <p/>
     * This method is an adapted version of: <code>FieldCacheImpl.getStringIndex()</code>
     *
     * @param reader     the <code>IndexReader</code>.
     * @param field      name of the shared field.
     * @param prefix     the property name, will be used as term prefix.
     * @param comparator the sort comparator instance.
     * @return a ValueIndex that contains the field values and order
     *         information.
     * @throws IOException if an error occurs while reading from the index.
     */
    public ValueIndex getValueIndex(IndexReader reader,
                                    String field,
                                    String prefix,
                                    SortComparatorSource comparator)
            throws IOException {

        if (reader instanceof ReadOnlyIndexReader) {
            reader = ((ReadOnlyIndexReader) reader).getBase();
        }

        field = field.intern();
        ValueIndex ret = lookup(reader, field, prefix, comparator);
        if (ret == null) {
            Comparable[] retArray = new Comparable[reader.maxDoc()];
            int setValues = 0;
            if (retArray.length > 0) {
                IndexFormatVersion version = IndexFormatVersion.getVersion(reader);
                boolean hasPayloads = version.isAtLeast(IndexFormatVersion.V3);
                TermDocs termDocs;
                byte[] payload = null;
                int type;
                if (hasPayloads) {
                    termDocs = reader.termPositions();
                    payload = new byte[1];
                } else {
                    termDocs = reader.termDocs();
                }
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
                            type = PropertyType.UNDEFINED;
                            if (hasPayloads) {
                                TermPositions termPos = (TermPositions) termDocs;
                                termPos.nextPosition();
                                if (termPos.isPayloadAvailable()) {
                                    payload = termPos.getPayload(payload, 0);
                                    type = PropertyMetaData.fromByteArray(payload).getPropertyType();
                                }
                            }
                            setValues++;
                            retArray[termDocs.doc()] = getValue(value, type);
                        }
                    } while (termEnum.next());
                } finally {
                    termDocs.close();
                    termEnum.close();
                }
            }
            ValueIndex value = new ValueIndex(retArray, setValues);
            store(reader, field, prefix, comparator, value);
            return value;
        }
        return ret;
    }

    /**
     * See if a <code>ValueIndex</code> object is in the cache.
     */
    ValueIndex lookup(IndexReader reader, String field,
                      String prefix, SortComparatorSource comparer) {
        Key key = new Key(field, prefix, comparer);
        synchronized (this) {
            Map<Key, ValueIndex> readerCache = cache.get(reader);
            if (readerCache == null) {
                return null;
            }
            return readerCache.get(key);
        }
    }

    /**
     * Put a <code>ValueIndex</code> <code>value</code> to cache.
     */
    ValueIndex store(IndexReader reader, String field, String prefix,
                 SortComparatorSource comparer, ValueIndex value) {
        Key key = new Key(field, prefix, comparer);
        synchronized (this) {
            Map<Key, ValueIndex> readerCache = cache.get(reader);
            if (readerCache == null) {
                readerCache = new HashMap<Key, ValueIndex>();
                cache.put(reader, readerCache);
            }
            return readerCache.put(key, value);
        }
    }

    /**
     * Returns a comparable for the given <code>value</code> that is read from
     * the index.
     *
     * @param value the value as read from the index.
     * @param type the property type.
     * @return a comparable for the <code>value</code>.
     */
    private Comparable getValue(String value, int type) {
        switch (type) {
            case PropertyType.BOOLEAN:
                return Boolean.valueOf(value);
            case PropertyType.DATE:
                return DateField.stringToTime(value);
            case PropertyType.LONG:
                return LongField.stringToLong(value);
            case PropertyType.DOUBLE:
                return DoubleField.stringToDouble(value);
            case PropertyType.DECIMAL:
                return DecimalField.stringToDecimal(value);
            default:
                return value;
        }
    }

    /**
     * A compound <code>Key</code> that consist of <code>field</code>
     * <code>prefix</code> and <code>comparator</code>.
     */
    static class Key {

        private final String field;
        private final String prefix;
        private final SortComparatorSource comparator;

        /**
         * Creates <code>Key</code> for ValueIndex lookup.
         */
        Key(String field, String prefix, SortComparatorSource comparator) {
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
