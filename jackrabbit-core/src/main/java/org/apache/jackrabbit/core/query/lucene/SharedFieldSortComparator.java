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
import java.util.ArrayList;
import java.util.List;

import org.apache.jackrabbit.core.query.lucene.SharedFieldCache.StringIndex;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.ScoreDocComparator;
import org.apache.lucene.search.SortComparator;
import org.apache.lucene.search.SortField;

/**
 * Implements a <code>SortComparator</code> which knows how to sort on a lucene
 * field that contains values for multiple properties.
 * <p/>
 * <b>Important:</b> The ScoreDocComparator returned by {@link #newComparator}
 * does not implement the contract for {@link ScoreDocComparator#sortValue(ScoreDoc)}
 * properly. The method will always return an empty String to save memory consumption
 * on large property ranges. Those values are only of relevance when queries
 * are executed with a <code>MultiSearcher</code>, which is currently not the
 * case in Jackrabbit.
 */
public class SharedFieldSortComparator extends SortComparator {

    /**
     * A <code>SharedFieldSortComparator</code> that is based on
     * {@link FieldNames#PROPERTIES}.
     */
    static final SortComparator PROPERTIES = new SharedFieldSortComparator(FieldNames.PROPERTIES);

    /**
     * The name of the shared field in the lucene index.
     */
    private final String field;

    /**
     * If <code>true</code> <code>ScoreDocComparator</code> will returns term
     * values when {@link org.apache.lucene.search.ScoreDocComparator#sortValue(org.apache.lucene.search.ScoreDoc)}
     * is called, otherwise only a dummy value is returned.
     */
    private final boolean createComparatorValues;

    /**
     * Creates a new <code>SharedFieldSortComparator</code> for a given shared
     * field.
     *
     * @param fieldname the shared field.
     */
    public SharedFieldSortComparator(String fieldname) {
        this(fieldname, false);
    }

    /**
     * Creates a new <code>SharedFieldSortComparator</code> for a given shared
     * field.
     *
     * @param fieldname              the shared field.
     * @param createComparatorValues if <code>true</code> creates values
     * for the <code>ScoreDocComparator</code>s.
     * @see #createComparatorValues
     */
    public SharedFieldSortComparator(String fieldname, boolean createComparatorValues) {
        this.field = fieldname;
        this.createComparatorValues = createComparatorValues;
    }

    /**
     * Creates a new <code>ScoreDocComparator</code> for an embedded
     * <code>propertyName</code> and a <code>reader</code>.
     * @param reader the index reader.
     * @param propertyName the name of the property to sort.
     * @return a <code>ScoreDocComparator</code> for the
     * @throws IOException
     * @throws IOException
     */
    public ScoreDocComparator newComparator(final IndexReader reader, final String propertyName) throws IOException {

        final List readers = new ArrayList();
        getIndexReaders(readers, reader);

        final SharedFieldCache.StringIndex[] indexes = new SharedFieldCache.StringIndex[readers.size()];

        int maxDoc = 0;
        final int[] starts = new int[readers.size() + 1];

        for (int i = 0; i < readers.size(); i++) {
            IndexReader r = (IndexReader) readers.get(i);
            starts[i] = maxDoc;
            maxDoc += r.maxDoc();
            indexes[i] = SharedFieldCache.INSTANCE.getStringIndex(r, field,
                    FieldNames.createNamedValue(propertyName, ""),
                    SharedFieldSortComparator.this, createComparatorValues);
        }
        starts[readers.size()] = maxDoc;

        return new ScoreDocComparator() {

            public final int compare(final ScoreDoc i, final ScoreDoc j) {
                int idx1 = readerIndex(i.doc);
                int idx2 = readerIndex(j.doc);             	

                String iTerm = indexes[idx1].terms[i.doc - starts[idx1]];
                String jTerm = indexes[idx2].terms[j.doc - starts[idx2]];

                if (iTerm == jTerm) return 0;
                if (iTerm == null) return -1;
                if (jTerm == null) return 1;

                return iTerm.compareTo(jTerm);
            }

            /**
             * Returns an empty if no lookup table is available otherwise the
             * index term for the score doc <code>i</code>.
             *
             * @param i
             *            the score doc.
             * @return the sort value if available.
             */
            public Comparable sortValue(final ScoreDoc i) {
                if (createComparatorValues) {
                    StringIndex index = indexes[readerIndex(i.doc)];
                    return index.terms[i.doc];
                } else {
                    // return dummy value
                    return "";
                }
            }

            public int sortType() {
                return SortField.CUSTOM;
            }

            /**
             * Returns the reader index for document <code>n</code>.
             *
             * @param n document number.
             * @return the reader index.
             */
            private int readerIndex(int n) {
                int lo = 0;
                int hi = readers.size() - 1;

                while (hi >= lo) {
                    int mid = (lo + hi) >> 1;
                    int midValue = starts[mid];
                    if (n < midValue) {
                        hi = mid - 1;
                    } else if (n > midValue) {
                        lo = mid + 1;
                    } else {
                        while (mid + 1 < readers.size() && starts[mid + 1] == midValue) {
                            mid++;
                        }
                        return mid;
                    }
                }
                return hi;
            }

        };
    }

    /**
     * @throws UnsupportedOperationException always.
     */
    protected Comparable getComparable(String termtext) {
        throw new UnsupportedOperationException();
    }

    /**
     * Checks if <code>reader</code> is of type {@link MultiIndexReader} and if
     * that's the case calls this method recursively for each reader within the
     * multi index reader; otherwise the reader is simply added to the list.
     *
     * @param readers the list of index readers.
     * @param reader  the reader to check.
     */
    private void getIndexReaders(List readers, IndexReader reader) {
        if (reader instanceof MultiIndexReader) {
            IndexReader[] r = ((MultiIndexReader) reader).getIndexReaders();
            for (int i = 0; i < r.length; i++) {
                getIndexReaders(readers, r[i]);
            }
        } else {
            readers.add(reader);
        }
    }
}
