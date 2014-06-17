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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import javax.jcr.PropertyType;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermPositions;

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
        private final Comparable<?>[] values;

        /**
         * Values (Comparable) map indexed by document id.
         */
        public final Map<Integer, Comparable<?>> valuesMap;

        /**
         * Boolean indicating whether the {@link #valuesMap} impl has to be used
         */
        public final boolean sparse;

        /**
         * Creates one of these objects
         */
        public ValueIndex(Comparable<?>[] values, int setValues) {
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

        public Comparable<?> getValue(int i) {
            if (sparse) {
                return valuesMap == null ? null : valuesMap.get(i);
            } else {
                return values[i];
            }
        }

        private static Map<Integer, Comparable<?>> getValuesMap(Comparable<?>[] values, int setValues) {
            Map<Integer, Comparable<?>> map = new HashMap<Integer, Comparable<?>>(setValues);
            for (int i = 0; i < values.length && setValues > 0; i++) {
                if (values[i] != null) {
                    map.put(i, values[i]);
                    setValues--;
                }
            }
            return map;
        }

        private static boolean isSparse(Comparable<?>[] values, int setValues) {
            // some really simple test to test whether the array is sparse. Currently, when less then 1% is set, the array is already sparse 
            // for this typical cache to avoid memory issues
            if (setValues * SPARSE_FACTOR < values.length) {
                return true;
            }
            return false;
        }
    }

    static class ComparableArray implements Comparable<ComparableArray> {

        private int offset = 0;

        private Comparable<?>[] c = new Comparable[0];

        public ComparableArray(Comparable<?> item, int index) {
            insert(item, index);
        }

        public int compareTo(ComparableArray o) {
            return Util.compare(c, o.c);
        }

        /**
         * testing purpose only.
         * 
         * @return the offset
         */
        int getOffset() {
            return offset;
        }

        public ComparableArray insert(Comparable<?> item, int index) {
            // optimize for most common scenario
            if (c.length == 0) {
                offset = index;
                c = new Comparable<?>[] { item };
                return this;
            }

            // inside
            if (index >= offset && index < offset + c.length) {
                c[index - offset] = item;
                return this;
            }

            // before
            if (index < offset) {
                int relativeOffset = offset - index;
                Comparable<?>[] newC = new Comparable[relativeOffset + c.length];
                newC[0] = item;
                System.arraycopy(c, 0, newC, relativeOffset, c.length);
                c = newC;
                offset = index;
                return this;
            }

            // after
            if (index >= offset + c.length) {
                Comparable<?>[] newC = new Comparable[index - offset + 1];
                System.arraycopy(c, 0, newC, 0, c.length);
                newC[index - offset] = item;
                c = newC;
                return this;
            }
            return this;
        }

        /*
         * This is needed by {@link UpperCaseSortComparator} and {@link LowerCaseSortComparator}
         */
        @Override
        public String toString() {
            if (c == null) {
                return null;
            }
            if (c.length == 1) {
                return c[0].toString();
            }
            return Arrays.toString(c);
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
     * <p>
     * This method is an adapted version of: <code>FieldCacheImpl.getStringIndex()</code>
     *
     * @param reader     the <code>IndexReader</code>.
     * @param field      name of the shared field.
     * @param prefix     the property name, will be used as term prefix.
     * @return a ValueIndex that contains the field values and order
     *         information.
     * @throws IOException if an error occurs while reading from the index.
     */
    public ValueIndex getValueIndex(IndexReader reader, String field,
            String prefix) throws IOException {

        if (reader instanceof ReadOnlyIndexReader) {
            reader = ((ReadOnlyIndexReader) reader).getBase();
        }

        field = field.intern();
        ValueIndex ret = lookup(reader, field, prefix);
        if (ret == null) {
            final int maxDocs = reader.maxDoc();
            Comparable<?>[] retArray = new Comparable<?>[maxDocs];
            Map<Integer, Integer> positions = new HashMap<Integer, Integer>();
            boolean usingSimpleComparable = true;
            int setValues = 0;
            if (maxDocs > 0) {
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
                try {
                    if (termEnum.term() == null) {
                        throw new RuntimeException("no terms in field " + field);
                    }
                    do {
                        Term term = termEnum.term();
                        if (term.field() != field || !term.text().startsWith(prefix)) {
                            break;
                        }
                        final String value = termValueAsString(term, prefix);
                        termDocs.seek(term);
                        while (termDocs.next()) {
                            int termPosition = 0;
                            type = PropertyType.UNDEFINED;
                            if (hasPayloads) {
                                TermPositions termPos = (TermPositions) termDocs;
                                termPosition = termPos.nextPosition();
                                if (termPos.isPayloadAvailable()) {
                                    payload = termPos.getPayload(payload, 0);
                                    type = PropertyMetaData.fromByteArray(payload).getPropertyType();
                                }
                            }
                            setValues++;
                            Comparable<?> v = getValue(value, type);
                            int doc = termDocs.doc();
                            Comparable<?> ca = retArray[doc];
                            if (ca == null) {
                                if (usingSimpleComparable) {
                                    // put simple value on the queue
                                    positions.put(doc, termPosition);
                                    retArray[doc] = v;
                                } else {
                                    retArray[doc] = new ComparableArray(v,
                                            termPosition);
                                }
                            } else {
                                if (ca instanceof ComparableArray) {
                                    ((ComparableArray) ca).insert(v,
                                            termPosition);
                                } else {
                                    // transform all of the existing values from
                                    // Comparable to ComparableArray
                                    for (int pos : positions.keySet()) {
                                        retArray[pos] = new ComparableArray(
                                                retArray[pos],
                                                positions.get(pos));
                                    }
                                    positions = null;
                                    usingSimpleComparable = false;
                                    ComparableArray caNew = (ComparableArray) retArray[doc];
                                    retArray[doc] = caNew.insert(v,
                                            termPosition);
                                }
                            }
                        }
                    } while (termEnum.next());
                } finally {
                    termDocs.close();
                    termEnum.close();
                }
            }
            ValueIndex value = new ValueIndex(retArray, setValues);
            store(reader, field, prefix, value);
            return value;
        }
        return ret;
    }

    /**
     * Extracts the value from a given Term as a String
     * 
     * @param term
     * @param prefix
     * @return string value contained in the term
     */
    private static String termValueAsString(Term term, String prefix) {
        // make sure term is compacted
        String text = term.text();
        int length = text.length() - prefix.length();
        char[] tmp = new char[length];
        text.getChars(prefix.length(), text.length(), tmp, 0);
        return new String(tmp, 0, length);
    }

    /**
     * See if a <code>ValueIndex</code> object is in the cache.
     */
    ValueIndex lookup(IndexReader reader, String field, String prefix) {
        synchronized (cache) {
            Map<Key, ValueIndex> readerCache = cache.get(reader);
            if (readerCache == null) {
                return null;
            }
            return readerCache.get(new Key(field, prefix));
        }
    }

    /**
     * Put a <code>ValueIndex</code> <code>value</code> to cache.
     */
    void store(IndexReader reader, String field, String prefix, ValueIndex value) {
        synchronized (cache) {
            Map<Key, ValueIndex> readerCache = cache.get(reader);
            if (readerCache == null) {
                readerCache = new HashMap<Key, ValueIndex>();
                cache.put(reader, readerCache);
            }
            readerCache.put(new Key(field, prefix), value);
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
    private Comparable<?> getValue(String value, int type) {
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
     * and <code>prefix</code>.
     */
    static class Key {

        private final String field;
        private final String prefix;

        /**
         * Creates <code>Key</code> for ValueIndex lookup.
         */
        Key(String field, String prefix) { 
            this.field = field.intern();
            this.prefix = prefix.intern();
        }

        /**
         * Returns <code>true</code> if <code>o</code> is a <code>Key</code>
         * instance and refers to the same field and prefix.
         */
        public boolean equals(Object o) {
            if (o instanceof Key) {
                Key other = (Key) o;
                return other.field == field
                        && other.prefix == prefix;
            }
            return false;
        }

        /**
         * Composes a hashcode based on the field and prefix.
         */
        public int hashCode() {
            return field.hashCode() ^ prefix.hashCode();
        }
    }

}
