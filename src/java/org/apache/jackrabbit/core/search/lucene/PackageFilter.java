/*
 * Copyright 2004 The Apache Software Foundation.
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
import org.apache.lucene.search.Filter;

import java.io.IOException;
import java.util.BitSet;

/**
 * Implements a fulltext filter, based on a {@link PathFilter}.
 */
class PackageFilter extends Filter {

    /**
     * the <code>BitSet</code> representing the filter
     */
    private BitSet filter = null;

    /**
     * number of bits set true
     */
    private int bitCount = 0;

    /**
     * the {@link PathFilter} defining the filter range
     */
    private final PathFilter pathFilter;

    /**
     * Creates a {@link PackageFilter} for the {@link PathFilter}
     * <code>filter</code>.<br>
     *
     * @param filter the {@link PathFilter} defining the filter range
     */
    public PackageFilter(PathFilter filter) {
        pathFilter = filter;
    }

    /**
     * Returns a <code>BitSet</code> where bits are set true for documents
     * which are contained in the {@link PathFilter} provided in the
     * Constructor of this class.
     *
     * @param reader the <code>IndexReader</code> of the search index.
     * @return a BitSet with true for documents which should be permitted in search
     *         results, and false for those that should not.
     * @throws IOException if an error occurs while reading from the search index.
     */
    public synchronized BitSet bits(IndexReader reader) throws IOException {
        // check if previously calculated
        if (filter == null) {
            filter = new BitSet(reader.maxDoc());

            // Iterate over all docs
            for (int i = 0; i < reader.maxDoc(); i++) {
                if (!reader.isDeleted(i)) {
                    // check if document is in ContentPackage
                    if (pathFilter.includes(reader.document(i).getField(FieldNames.PATH).stringValue())) {
                        filter.set(i);
                        bitCount++;
                    }
                }
            }
        }
        return filter;
    }

    /**
     * Returns true if this PackageFilter blocks all pages, false
     * otherwise. Please note that this method returns false when
     * this PackageFilter was never called with a reader before.
     * See {@link #bits}.
     *
     * @return true if this <code>PackageFilter</code> blocks all pages.
     */
    public boolean blocksAll() {
        return (filter != null && bitCount == 0);
    }
}
