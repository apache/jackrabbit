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
package org.apache.jackrabbit.core.query.lucene.sort;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.jackrabbit.core.query.lucene.FieldComparatorBase;
import org.apache.jackrabbit.core.query.lucene.FieldNames;
import org.apache.jackrabbit.core.query.lucene.MultiIndexReader;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;

/**
 * Abstract base class for <code>FieldComparator</code>s which keep their values
 * (<code>Comparable</code>s) in an array.
 */
public abstract class AbstractFieldComparator extends FieldComparatorBase {

    /**
     * The values for comparing.
     */
    private final Comparable<?>[] values;

    /**
     * The index readers.
     */

    protected final List<IndexReader> readers = new ArrayList<IndexReader>();
    /**
     * The document number starts for the {@link #readers}.
     */
    protected int[] starts;

    /**
     * Create a new instance with the given number of values.
     *
     * @param numHits  the number of values
     */
    protected AbstractFieldComparator(int numHits) {
        values = new Comparable[numHits];
    }

    /**
     * Returns the reader index for document <code>n</code>.
     *
     * @param n document number.
     * @return the reader index.
     */
    protected final int readerIndex(int n) {
        int lo = 0;
        int hi = readers.size() - 1;

        while (hi >= lo) {
            int mid = (lo + hi) >> 1;
            int midValue = starts[mid];
            if (n < midValue) {
                hi = mid - 1;
            }
            else if (n > midValue) {
                lo = mid + 1;
            }
            else {
                while (mid + 1 < readers.size() && starts[mid + 1] == midValue) {
                    mid++;
                }
                return mid;
            }
        }
        return hi;
    }

    /**
     * Add the given value to the values array
     *
     * @param slot   index into values
     * @param value  value for adding
     */
    @Override
    public void setValue(int slot, Comparable<?> value) {
        values[slot] = value;
    }

    /**
     * Return a value from the values array
     *
     * @param slot  index to retrieve
     * @return  the retrieved value
     */
    @Override
    public Comparable<?> getValue(int slot) {
        return values[slot];
    }

    @Override
    public void setNextReader(IndexReader reader, int docBase) throws IOException {
        getIndexReaders(readers, reader);

        int maxDoc = 0;
        starts = new int[readers.size() + 1];

        for (int i = 0; i < readers.size(); i++) {
            IndexReader r = readers.get(i);
            starts[i] = maxDoc;
            maxDoc += r.maxDoc();
        }
        starts[readers.size()] = maxDoc;
    }

    /**
     * Checks if <code>reader</code> is of type {@link MultiIndexReader} and if
     * so calls itself recursively for each reader within the
     * <code>MultiIndexReader</code> or otherwise adds the reader to the list.
     *
     * @param readers  list of index readers.
     * @param reader   reader to decompose
     */
    private static void getIndexReaders(List<IndexReader> readers, IndexReader reader) {
        if (reader instanceof MultiIndexReader) {
            for (IndexReader r : ((MultiIndexReader) reader).getIndexReaders()) {
                getIndexReaders(readers, r);
            }
        }
        else {
            readers.add(reader);
        }
    }

    protected String getUUIDForIndex(int doc) throws IOException {
        int idx = readerIndex(doc);
        IndexReader reader = readers.get(idx);
        Document document = reader.document(doc - starts[idx]);
        return document.get(FieldNames.UUID);
    }
}