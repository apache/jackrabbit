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
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.HitCollector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.Weight;

import java.io.IOException;
import java.util.BitSet;
import java.util.Map;
import java.util.HashMap;

/**
 * Implements a lucene <code>Query</code> which filters a sub query by checking
 * whether the nodes selected by that sub query are descendants or self of
 * nodes selected by a context query.
 */
class DescendantSelfAxisQuery extends Query {

    /**
     * Default score is 1.0f.
     */
    private static final Float DEFAULT_SCORE = new Float(1.0f);

    /**
     * The context query
     */
    private final Query contextQuery;

    /**
     * The scorer of the context query
     */
    private Scorer contextScorer;

    /**
     * The sub query to filter
     */
    private final Query subQuery;

    /**
     * If <code>true</code> this query acts on the descendant-or-self axis.
     * If <code>false</code> this query acts on the descendant axis.
     */
    private final boolean includeSelf;

    /**
     * The scorer of the sub query to filter
     */
    private Scorer subScorer;

    /**
     * Creates a new <code>DescendantSelfAxisQuery</code> based on a
     * <code>context</code> query and filtering the <code>sub</code> query.
     *
     * @param context the context for this query.
     * @param sub     the sub query.
     */
    public DescendantSelfAxisQuery(Query context, Query sub) {
        this(context, sub, true);
    }

    /**
     * Creates a new <code>DescendantSelfAxisQuery</code> based on a
     * <code>context</code> query and filtering the <code>sub</code> query.
     *
     * @param context     the context for this query.
     * @param sub         the sub query.
     * @param includeSelf if <code>true</code> this query acts like a
     *                    descendant-or-self axis. If <code>false</code> this query acts like
     *                    a descendant axis.
     */
    public DescendantSelfAxisQuery(Query context, Query sub, boolean includeSelf) {
        this.contextQuery = context;
        this.subQuery = sub;
        this.includeSelf = includeSelf;
    }

    /**
     * Creates a <code>Weight</code> instance for this query.
     *
     * @param searcher the <code>Searcher</code> instance to use.
     * @return a <code>DescendantSelfAxisWeight</code>.
     */
    protected Weight createWeight(Searcher searcher) {
        return new DescendantSelfAxisWeight(searcher);
    }

    /**
     * Always returns 'DescendantSelfAxisQuery'.
     *
     * @param field the name of a field.
     * @return 'DescendantSelfAxisQuery'.
     */
    public String toString(String field) {
        return "DescendantSelfAxisQuery";
    }

    //------------------------< DescendantSelfAxisWeight >--------------------------

    /**
     * The <code>Weight</code> implementation for this
     * <code>DescendantSelfAxisWeight</code>.
     */
    private class DescendantSelfAxisWeight implements Weight {

        /**
         * The searcher in use
         */
        private final Searcher searcher;

        /**
         * Creates a new <code>DescendantSelfAxisWeight</code> instance using
         * <code>searcher</code>.
         *
         * @param searcher a <code>Searcher</code> instance.
         */
        private DescendantSelfAxisWeight(Searcher searcher) {
            this.searcher = searcher;
        }

        /**
         * Returns this <code>DescendantSelfAxisQuery</code>.
         *
         * @return this <code>DescendantSelfAxisQuery</code>.
         */
        public Query getQuery() {
            return DescendantSelfAxisQuery.this;
        }

        /**
         * {@inheritDoc}
         */
        public float getValue() {
            return 1.0f;
        }

        /**
         * {@inheritDoc}
         */
        public float sumOfSquaredWeights() throws IOException {
            return 1.0f;
        }

        /**
         * {@inheritDoc}
         */
        public void normalize(float norm) {
        }

        /**
         * Creates a scorer for this <code>DescendantSelfAxisScorer</code>.
         *
         * @param reader a reader for accessing the index.
         * @return a <code>DescendantSelfAxisScorer</code>.
         * @throws IOException if an error occurs while reading from the index.
         */
        public Scorer scorer(IndexReader reader) throws IOException {
            contextScorer = contextQuery.weight(searcher).scorer(reader);
            subScorer = subQuery.weight(searcher).scorer(reader);
            HierarchyResolver resolver = (HierarchyResolver) reader;
            return new DescendantSelfAxisScorer(searcher.getSimilarity(), reader, resolver);
        }

        /**
         * {@inheritDoc}
         */
        public Explanation explain(IndexReader reader, int doc) throws IOException {
            return new Explanation();
        }
    }

    //----------------------< DescendantSelfAxisScorer >---------------------------------
    /**
     * Implements a <code>Scorer</code> for this
     * <code>DescendantSelfAxisQuery</code>.
     */
    private class DescendantSelfAxisScorer extends Scorer {

        /**
         * The <code>HierarchyResolver</code> of the index.
         */
        private final HierarchyResolver hResolver;

        /**
         * BitSet storing the id's of selected documents
         */
        private final BitSet contextHits;

        /**
         * BitSet storing the id's of selected documents from the sub query
         */
        private final BitSet subHits;

        /**
         * Map that contains the scores for the sub hits. To save memory
         * only scores that are not equal to 1.0f are put to this map.
         * <p/>
         * key=[Integer] id of selected document from sub query<br>
         * value=[Float] score for that document
         */
        private final Map scores = new HashMap();

        /**
         * The next document id to return
         */
        private int nextDoc = -1;

        /**
         * Set <code>true</code> once the sub contextHits have been calculated.
         */
        private boolean subHitsCalculated = false;

        /**
         * Creates a new <code>DescendantSelfAxisScorer</code>.
         *
         * @param similarity the <code>Similarity</code> instance to use.
         * @param reader     for index access.
         * @param hResolver  the hierarchy resolver of <code>reader</code>.
         */
        protected DescendantSelfAxisScorer(Similarity similarity,
                                           IndexReader reader,
                                           HierarchyResolver hResolver) {
            super(similarity);
            this.hResolver = hResolver;
            // todo reuse BitSets?
            this.contextHits = new BitSet(reader.maxDoc());
            this.subHits = new BitSet(reader.maxDoc());
        }

        /**
         * {@inheritDoc}
         */
        public boolean next() throws IOException {
            calculateSubHits();
            nextDoc = subHits.nextSetBit(nextDoc + 1);
            while (nextDoc > -1) {
                // check if nextDoc is really valid against the context query

                // check self if necessary
                if (includeSelf) {
                    if (contextHits.get(nextDoc)) {
                        return true;
                    }
                }

                // check if nextDoc is a descendant of one of the context nodes
                int parentDoc = hResolver.getParent(nextDoc);
                while (parentDoc != -1 && !contextHits.get(parentDoc)) {
                    // traverse
                    parentDoc = hResolver.getParent(parentDoc);
                }

                if (parentDoc != -1) {
                    // since current parentDoc is a descendant of one of the context
                    // docs we can promote parentDoc to the context hits
                    contextHits.set(parentDoc);
                    return true;
                }

                // try next
                nextDoc = subHits.nextSetBit(nextDoc + 1);
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public int doc() {
            return nextDoc;
        }

        /**
         * {@inheritDoc}
         */
        public float score() throws IOException {
            Float score = (Float) scores.get(new Integer(nextDoc));
            if (score == null) {
                score = DEFAULT_SCORE;
            }
            return score.floatValue();
        }

        /**
         * {@inheritDoc}
         */
        public boolean skipTo(int target) throws IOException {
            nextDoc = target - 1;
            return next();
        }

        private void calculateSubHits() throws IOException {
            if (!subHitsCalculated) {

                contextScorer.score(new HitCollector() {
                    public void collect(int doc, float score) {
                        contextHits.set(doc);
                    }
                }); // find all

                if (contextHits.isEmpty()) {
                    // no need to execute sub scorer, context is empty
                } else {
                    subScorer.score(new HitCollector() {
                        public void collect(int doc, float score) {
                            subHits.set(doc);
                            if (score != DEFAULT_SCORE.floatValue()) {
                                scores.put(new Integer(doc), new Float(score));
                            }
                        }
                    });
                }

                subHitsCalculated = true;
            }
        }

        /**
         * @throws UnsupportedOperationException this implementation always
         *                                       throws an <code>UnsupportedOperationException</code>.
         */
        public Explanation explain(int doc) throws IOException {
            throw new UnsupportedOperationException();
        }
    }


}
