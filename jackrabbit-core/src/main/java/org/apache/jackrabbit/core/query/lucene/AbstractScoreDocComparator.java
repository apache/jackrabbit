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

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

import org.apache.lucene.search.ScoreDocComparator;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SortField;
import org.apache.lucene.index.IndexReader;

/**
 * Abstract base class of {@link ScoreDocComparator} implementations.
 */
abstract class AbstractScoreDocComparator implements ScoreDocComparator {

    /**
     * The index readers.
     */
    protected final List<IndexReader> readers = new ArrayList<IndexReader>();

    /**
     * The document number starts for the {@link #readers}.
     */
    protected final int[] starts;

    public AbstractScoreDocComparator(IndexReader reader)
            throws IOException {
        getIndexReaders(readers, reader);

        int maxDoc = 0;
        this.starts = new int[readers.size() + 1];

        for (int i = 0; i < readers.size(); i++) {
            IndexReader r = readers.get(i);
            starts[i] = maxDoc;
            maxDoc += r.maxDoc();
        }
        starts[readers.size()] = maxDoc;
    }

    /**
     * Compares sort values of <code>i</code> and <code>j</code>. If the
     * sort values have differing types, then the sort order is defined on
     * the type itself by calling <code>compareTo()</code> on the respective
     * type class names.
     *
     * @param i first score doc.
     * @param j second score doc.
     * @return a negative integer if <code>i</code> should come before
     *         <code>j</code><br> a positive integer if <code>i</code>
     *         should come after <code>j</code><br> <code>0</code> if they
     *         are equal
     */
    public int compare(ScoreDoc i, ScoreDoc j) {
        return Util.compare(sortValue(i), sortValue(j));
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
    protected int readerIndex(int n) {
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

    /**
     * Checks if <code>reader</code> is of type {@link MultiIndexReader} and if
     * that's the case calls this method recursively for each reader within the
     * multi index reader; otherwise the reader is simply added to the list.
     *
     * @param readers the list of index readers.
     * @param reader  the reader to check.
     */
    private static void getIndexReaders(List<IndexReader> readers,
                                        IndexReader reader) {
        if (reader instanceof MultiIndexReader) {
            for (IndexReader r : ((MultiIndexReader) reader).getIndexReaders()) {
                getIndexReaders(readers, r);
            }
        } else {
            readers.add(reader);
        }
    }
}
