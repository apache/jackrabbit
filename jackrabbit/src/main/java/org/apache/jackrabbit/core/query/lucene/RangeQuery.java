/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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
package org.apache.jackrabbit.core.query.lucene;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermDocs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.collections.map.LRUMap;

import java.io.IOException;
import java.util.BitSet;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Implements a variant of the lucene class {@link org.apache.lucene.search.RangeQuery}.
 * This class does not rewrite to basic {@link org.apache.lucene.search.TermQuery}
 * but will calculate the matching documents itself. That way a
 * <code>TooManyClauses</code> can be avoided.
 */
public class RangeQuery extends Query {

    /**
     * Logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(RangeQuery.class);

    /**
     * Simple result cache for previously calculated hits.
     * key=IndexReader value=Map{key=String:range,value=BitSet:hits}
     */
    private static final Map cache = new WeakHashMap();

    /**
     * The lower term. May be <code>null</code> if <code>upperTerm</code> is not
     * <code>null</code>.
     */
    private Term lowerTerm;

    /**
     * The upper term. May be <code>null</code> if <code>lowerTerm</code> is not
     * <code>null</code>.
     */
    private Term upperTerm;

    /**
     * If <code>true</code> the range interval is inclusive.
     */
    private boolean inclusive;

    /**
     * Creates a new RangeQuery. The lower or the upper term may be
     * <code>null</code>, but not both!
     *
     * @param lowerTerm the lower term of the interval, or <code>null</code>
     * @param upperTerm the upper term of the interval, or <code>null</code>.
     * @param inclusive if <code>true</code> the interval is inclusive.
     */
    public RangeQuery(Term lowerTerm, Term upperTerm, boolean inclusive) {
        if (lowerTerm == null && upperTerm == null) {
            throw new IllegalArgumentException("At least one term must be non-null");
        }
        if (lowerTerm != null && upperTerm != null && lowerTerm.field() != upperTerm.field()) {
            throw new IllegalArgumentException("Both terms must be for the same field");
        }

        // if we have a lowerTerm, start there. otherwise, start at beginning
        if (lowerTerm != null) {
            this.lowerTerm = lowerTerm;
        } else {
            this.lowerTerm = new Term(upperTerm.field(), "");
        }

        this.upperTerm = upperTerm;
        this.inclusive = inclusive;
    }

    /**
     * Tries to rewrite this query into a standard lucene RangeQuery.
     * This rewrite might fail with a TooManyClauses exception. If that
     * happens, we use our own implementation.
     *
     * @param reader the index reader.
     * @return the rewritten query or this query if rewriting is not possible.
     * @throws IOException if an error occurs.
     */
    public Query rewrite(IndexReader reader) throws IOException {
        Query stdRangeQueryImpl
                = new org.apache.lucene.search.RangeQuery(lowerTerm, upperTerm, inclusive);
        try {
            return stdRangeQueryImpl.rewrite(reader);
        } catch (BooleanQuery.TooManyClauses e) {
            log.debug("Too many terms to enumerate, using custom RangeQuery");
            // failed, use own implementation
            return this;
        }
    }

    /**
     * Creates the <code>Weight</code> for this query.
     *
     * @param searcher the searcher to use for the <code>Weight</code>.
     * @return the <code>Weigth</code> for this query.
     */
    protected Weight createWeight(Searcher searcher) {
        return new RangeQueryWeight(searcher);
    }

    /**
     * Returns a string representation of this query.
     * @param field the field name for which to create a string representation.
     * @return a string representation of this query.
     */
    public String toString(String field) {
        StringBuffer buffer = new StringBuffer();
        if (!getField().equals(field)) {
            buffer.append(getField());
            buffer.append(":");
        }
        buffer.append(inclusive ? "[" : "{");
        buffer.append(lowerTerm != null ? lowerTerm.text() : "null");
        buffer.append(" TO ");
        buffer.append(upperTerm != null ? upperTerm.text() : "null");
        buffer.append(inclusive ? "]" : "}");
        if (getBoost() != 1.0f) {
            buffer.append("^");
            buffer.append(Float.toString(getBoost()));
        }
        return buffer.toString();
    }

    /**
     * Returns the field name for this query.
     */
    private String getField() {
        return (lowerTerm != null ? lowerTerm.field() : upperTerm.field());
    }

    //--------------------------< RangeQueryWeight >----------------------------

    /**
     * The <code>Weight</code> implementation for this <code>RangeQuery</code>.
     */
    private class RangeQueryWeight implements Weight {

        /**
         * The searcher in use
         */
        private final Searcher searcher;

        /**
         * Creates a new <code>RangeQueryWeight</code> instance using
         * <code>searcher</code>.
         *
         * @param searcher a <code>Searcher</code> instance.
         */
        RangeQueryWeight(Searcher searcher) {
            this.searcher = searcher;
        }

        /**
         * Returns this <code>RangeQuery</code>.
         *
         * @return this <code>RangeQuery</code>.
         */
        public Query getQuery() {
            return RangeQuery.this;
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
         * Creates a scorer for this <code>Rangequery</code>.
         *
         * @param reader a reader for accessing the index.
         * @return a <code>RangeQueryScorer</code>.
         * @throws IOException if an error occurs while reading from the index.
         */
        public Scorer scorer(IndexReader reader) throws IOException {
            return new RangeQueryScorer(searcher.getSimilarity(), reader);
        }

        /**
         * {@inheritDoc}
         */
        public Explanation explain(IndexReader reader, int doc) throws IOException {
            return new Explanation();
        }
    }

    //------------------------< RangeQueryScorer >------------------------------

    /**
     * Implements a <code>Scorer</code> for this <code>RangeQuery</code>.
     */
    private final class RangeQueryScorer extends Scorer {

        /**
         * The index reader to use for calculating the matching documents.
         */
        private final IndexReader reader;

        /**
         * The documents ids that match this range query.
         */
        private final BitSet hits;

        /**
         * Set to <code>true</code> when the hits have been calculated.
         */
        private boolean hitsCalculated = false;

        /**
         * The next document id to return
         */
        private int nextDoc = -1;

        /**
         * The cache key to use to store the results.
         */
        private final String cacheKey;

        /**
         * The map to store the results.
         */
        private final Map resultMap;

        /**
         * Creates a new RangeQueryScorer.
         * @param similarity the similarity implementation.
         * @param reader the index reader to use.
         */
        RangeQueryScorer(Similarity similarity, IndexReader reader) {
            super(similarity);
            this.reader = reader;
            StringBuffer key = new StringBuffer();
            key.append(lowerTerm != null ? lowerTerm.field() : upperTerm.field());
            key.append('\uFFFF');
            key.append(lowerTerm != null ? lowerTerm.text() : "");
            key.append('\uFFFF');
            key.append(upperTerm != null ? upperTerm.text() : "");
            key.append('\uFFFF');
            key.append(inclusive);
            this.cacheKey = key.toString();
            // check cache
            synchronized (cache) {
                Map m = (Map) cache.get(reader);
                if (m == null) {
                    m = new LRUMap(10);
                    cache.put(reader, m);
                }
                resultMap = m;
            }
            synchronized (resultMap) {
                BitSet result = (BitSet) resultMap.get(cacheKey);
                if (result == null) {
                    result = new BitSet(reader.maxDoc());
                } else {
                    hitsCalculated = true;
                }
                hits = result;
            }
        }

        /**
         * {@inheritDoc}
         */
        public boolean next() throws IOException {
            calculateHits();
            nextDoc = hits.nextSetBit(nextDoc + 1);
            return nextDoc > -1;
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
        public float score() {
            return 1.0f;
        }

        /**
         * {@inheritDoc}
         */
        public boolean skipTo(int target) {
            nextDoc = hits.nextSetBit(target);
            return nextDoc > -1;
        }

        /**
         * Returns an empty Explanation object.
         * @return an empty Explanation object.
         */
        public Explanation explain(int doc) {
            return new Explanation();
        }

        /**
         * Calculates the ids of the documents matching this range query.
         * @throws IOException if an error occurs while reading from the index.
         */
        private void calculateHits() throws IOException {
            if (hitsCalculated) {
                return;
            }

            TermEnum enumerator = reader.terms(lowerTerm);

            try {
                boolean checkLower = false;
                if (!inclusive) {
                    // make adjustments to set to exclusive
                    checkLower = true;
                }

                String testField = getField();

                TermDocs docs = reader.termDocs();
                try {
                    do {
                        Term term = enumerator.term();
                        if (term != null && term.field() == testField) {
                            if (!checkLower || term.text().compareTo(lowerTerm.text()) > 0) {
                                checkLower = false;
                                if (upperTerm != null) {
                                    int compare = upperTerm.text().compareTo(term.text());
                                    // if beyond the upper term, or is exclusive and
                                    // this is equal to the upper term, break out
                                    if ((compare < 0) || (!inclusive && compare == 0)) {
                                        break;
                                    }
                                }

                                docs.seek(enumerator);
                                while (docs.next()) {
                                    hits.set(docs.doc());
                                }
                            }
                        } else {
                            break;
                        }
                    } while (enumerator.next());
                } finally {
                    docs.close();
                }
            } finally {
                enumerator.close();
            }
            hitsCalculated = true;
            // put to cache
            synchronized (resultMap) {
                resultMap.put(cacheKey, hits);
            }
        }
    }
}
