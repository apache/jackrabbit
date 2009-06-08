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

import org.apache.lucene.search.ScoreDocComparator;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortComparatorSource;
import org.apache.lucene.index.IndexReader;

/**
 * <code>LowerCaseSortComparator</code> implements a sort comparator that
 * compares the lower-cased string values of a base sort comparator.
 */
public class LowerCaseSortComparator implements SortComparatorSource {

    private static final long serialVersionUID = 5396206509020979445L;

    /**
     * The base sort comparator.
     */
    private final SortComparatorSource base;

    /**
     * Creates a new lower case sort comparator.
     *
     * @param base the base sort comparator source.
     */
    public LowerCaseSortComparator(SortComparatorSource base) {
        this.base = base;
    }

    /**
     * {@inheritDoc}
     */
    public ScoreDocComparator newComparator(IndexReader reader,
                                            String fieldname)
            throws IOException {
        return new Comparator(base.newComparator(reader, fieldname));
    }

    private static final class Comparator implements ScoreDocComparator {

        private ScoreDocComparator base;

        private Comparator(ScoreDocComparator base) {
            this.base = base;
        }

        /**
         * @see Util#compare(Comparable, Comparable)
         */
        public int compare(ScoreDoc i, ScoreDoc j) {
            return Util.compare(sortValue(i), sortValue(j));
        }

        public Comparable sortValue(ScoreDoc i) {
            Comparable c = base.sortValue(i);
            if (c != null) {
                return c.toString().toLowerCase();
            } else {
                return null;
            }
        }

        public int sortType() {
            return SortField.CUSTOM;
        }
    }
}
