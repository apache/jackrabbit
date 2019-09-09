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

import org.apache.jackrabbit.spi.Name;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SortField;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * <code>SortedMultiColumnQueryHits</code> implements sorting of query hits
 * based on {@link Ordering}s.
 */
public class SortedMultiColumnQueryHits extends FilterMultiColumnQueryHits {

    /**
     * Iterator over sorted ScoreNode[]s.
     */
    private final Iterator<ScoreNode[]> it;

    /**
     * Creates sorted query hits.
     *
     * @param hits      the hits to sort.
     * @param orderings the ordering specifications.
     * @param reader    the current index reader.
     * @throws IOException if an error occurs while reading from the index.
     */
    public SortedMultiColumnQueryHits(MultiColumnQueryHits hits,
                                      Ordering[] orderings,
                                      IndexReader reader)
            throws IOException {
        super(hits);
        List<ScoreNode[]> sortedHits = new ArrayList<ScoreNode[]>();
        ScoreNode[] next;
        while ((next = hits.nextScoreNodes()) != null) {
            sortedHits.add(next);
        }
        try {
            Collections.sort(sortedHits, new ScoreNodeComparator(
                    reader, orderings, hits.getSelectorNames(), sortedHits.size()));
        } catch (RuntimeException e) {
            // might be thrown by ScoreNodeComparator#compare
            throw Util.createIOException(e);
        }
        this.it = sortedHits.iterator();
    }

    /**
     * {@inheritDoc}
     */
    public ScoreNode[] nextScoreNodes() throws IOException {
        if (it.hasNext()) {
            return it.next();
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void skip(int n) throws IOException {
        while (n-- > 0) {
            nextScoreNodes();
        }
    }

    /**
     * A comparator that compares ScoreNode[].
     */
    private static final class ScoreNodeComparator
            implements Comparator<ScoreNode[]> {

        /**
         * The current index reader.
         */
        private final IndexReader reader;

        /**
         * The ordering specifications.
         */
        private final Ordering[] orderings;

        /**
         * The selector name index for each of the {@link #orderings}.
         */
        private final int[] idx;

        /**
         * The score doc comparator for each of the {@link #orderings}.
         */
        private final ScoreDocComparator[] comparators;

        /**
         * The reverse flag for each of the {@link #orderings}.
         */
        private final boolean[] isReverse;

        /**
         * Reusable ScoreDoc for use in {@link #compare(ScoreNode[], ScoreNode[])}.
         */
        private final ScoreDoc doc1 = new ScoreDoc(0, 1.0f);

        /**
         * Reusable ScoreDoc for use in {@link #compare(ScoreNode[], ScoreNode[])}.
         */
        private final ScoreDoc doc2 = new ScoreDoc(0, 1.0f);

        /**
         * Creates a new comparator.
         *
         * @param reader        the current index reader.
         * @param orderings     the ordering specifications.
         * @param selectorNames the selector names associated with the
         *                      ScoreNode[] used in
         *                      {@link #compare(ScoreNode[], ScoreNode[])}.
         * @throws IOException if an error occurs while reading from the index.
         */
        private ScoreNodeComparator(IndexReader reader,
                                    Ordering[] orderings,
                                    Name[] selectorNames,
                                    int numHits)
                throws IOException {
            this.reader = reader;
            this.orderings = orderings;
            List<Name> names = Arrays.asList(selectorNames);
            this.idx = new int[orderings.length];
            this.comparators = new ScoreDocComparator[orderings.length];
            this.isReverse = new boolean[orderings.length];
            for (int i = 0; i < orderings.length; i++) {
                idx[i] = names.indexOf(orderings[i].getSelectorName());
                SortField sf = orderings[i].getSortField();
                if (sf.getComparatorSource() != null) {
                    FieldComparator c = sf.getComparatorSource().newComparator(sf.getField(), numHits, 0, false);
                    assert c instanceof FieldComparatorBase;
                    comparators[i] = new ScoreDocComparator((FieldComparatorBase) c);
                    comparators[i].setNextReader(reader, 0);
                }
                isReverse[i] = sf.getReverse();
            }
        }

        /**
         * {@inheritDoc}
         */
        public int compare(ScoreNode[] sn1, ScoreNode[] sn2) {
            for (int i = 0; i < orderings.length; i++) {
                int c;
                int scoreNodeIndex = idx[i];
                ScoreNode n1 = sn1[scoreNodeIndex];
                ScoreNode n2 = sn2[scoreNodeIndex];
                if (n1 == n2) {
                    continue;
                } else if (n1 == null) {
                    c = -1;
                } else if (n2 == null) {
                    c = 1;
                } else if (comparators[i] != null) {
                    try {
                        doc1.doc = n1.getDoc(reader);
                        doc1.score = n1.getScore();
                        doc2.doc = n2.getDoc(reader);
                        doc2.score = n2.getScore();
                    } catch (IOException e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                    c = comparators[i].compareDocs(doc1.doc, doc2.doc);
                } else {
                    // compare score
                    c = new Float(n1.getScore()).compareTo(n2.getScore());
                }
                if (c != 0) {
                    if (isReverse[i]) {
                        c = -c;
                    }
                    return c;
                }
            }
            return 0;
        }

    }

    private static final class ScoreDocComparator extends FieldComparatorDecorator {

        public ScoreDocComparator(FieldComparatorBase base) {
            super(base);
        }

        public int compareDocs(int doc1, int doc2) {
            return compare(sortValue(doc1), sortValue(doc2));
        }

    }

}
