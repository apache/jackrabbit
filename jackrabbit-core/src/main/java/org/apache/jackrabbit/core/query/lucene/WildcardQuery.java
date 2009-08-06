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
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.FilteredTermEnum;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Similarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.BitSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

/**
 * Implements a wildcard query on a lucene field with an embedded property name
 * and a pattern.
 * <p/>
 * Wildcards are:
 * <ul>
 * <li><code>%</code> : matches zero or more characters</li>
 * <li><code>_</code> : matches exactly one character</li>
 * </ul>
 */
public class WildcardQuery extends Query implements Transformable {

    /**
     * Logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(WildcardQuery.class);

    /**
     * Name of the field to search.
     */
    private final String field;

    /**
     * Name of the property to search.
     */
    private final String propName;

    /**
     * The wildcard pattern.
     */
    private final String pattern;

    /**
     * How property values are tranformed before they are matched using the
     * provided pattern.
     */
    private int transform = TRANSFORM_NONE;

    /**
     * The standard multi term query to execute wildcard queries. This is only
     * set if the pattern matches less than {@link org.apache.lucene.search.BooleanQuery#maxClauseCount}.
     */
    private Query multiTermQuery;

    /**
     * Creates a new <code>WildcardQuery</code>.
     *
     * @param field     the name of the field to search.
     * @param propName  name of the property to search.
     * @param pattern   the wildcard pattern.
     * @param transform how property values are transformed before they are
     *                  matched using the <code>pattern</code>.
     */
    public WildcardQuery(String field, String propName, String pattern, int transform) {
        this.field = field.intern();
        this.propName = propName;
        this.pattern = pattern;
        this.transform = transform;
    }

    /**
     * Creates a new <code>WildcardQuery</code>.
     *
     * @param field    the name of the field to search.
     * @param propName name of the property to search.
     * @param pattern  the wildcard pattern.
     */
    public WildcardQuery(String field, String propName, String pattern) {
        this(field, propName, pattern, TRANSFORM_NONE);
    }

    /**
     * {@inheritDoc}
     */
    public void setTransformation(int transformation) {
        this.transform = transformation;
    }

    /**
     * Either rewrites this query to a lucene MultiTermQuery or in case of
     * a TooManyClauses exception to a custom jackrabbit query implementation
     * that uses a BitSet to collect all hits.
     *
     * @param reader the index reader to use for the search.
     * @return the rewritten query.
     * @throws IOException if an error occurs while reading from the index.
     */
    public Query rewrite(IndexReader reader) throws IOException {
        Query stdWildcardQuery = new MultiTermQuery(new Term(field, pattern)) {
            protected FilteredTermEnum getEnum(IndexReader reader) throws IOException {
                return new WildcardTermEnum(reader, field, propName, pattern, transform);
            }
        };
        try {
            multiTermQuery = stdWildcardQuery.rewrite(reader);
            return multiTermQuery;
        } catch (BooleanQuery.TooManyClauses e) {
            // MultiTermQuery not possible
            log.debug("Too many terms to enumerate, using custom WildcardQuery.");
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
        return new WildcardQueryWeight(searcher);
    }

    /**
     * Returns a string representation of this query.
     *
     * @param field the field name for which to create a string representation.
     * @return a string representation of this query.
     */
    public String toString(String field) {
        return propName + ":" + pattern;
    }

    /**
     * {@inheritDoc}
     */
    public void extractTerms(Set terms) {
        if (multiTermQuery != null) {
            multiTermQuery.extractTerms(terms);
        }
    }

    /**
     * The <code>Weight</code> implementation for this <code>WildcardQuery</code>.
     */
    private class WildcardQueryWeight extends AbstractWeight {

        /**
         * Creates a new <code>WildcardQueryWeight</code> instance using
         * <code>searcher</code>.
         *
         * @param searcher a <code>Searcher</code> instance.
         */
        public WildcardQueryWeight(Searcher searcher) {
            super(searcher);
        }

        /**
         * Creates a {@link WildcardQueryScorer} instance.
         *
         * @param reader index reader
         * @return a {@link WildcardQueryScorer} instance
         */
        protected Scorer createScorer(IndexReader reader) {
            return new WildcardQueryScorer(searcher.getSimilarity(), reader);
        }

        /**
         * Returns this <code>WildcardQuery</code>.
         *
         * @return this <code>WildcardQuery</code>.
         */
        public Query getQuery() {
            return WildcardQuery.this;
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
         * {@inheritDoc}
         */
        public Explanation explain(IndexReader reader, int doc) throws IOException {
            return new Explanation();
        }
    }

    /**
     * Implements a <code>Scorer</code> for this <code>WildcardQuery</code>.
     */
    private final class WildcardQueryScorer extends Scorer {

        /**
         * The index reader to use for calculating the matching documents.
         */
        private final IndexReader reader;

        /**
         * The documents ids that match this wildcard query.
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
        private final Map<String, BitSet> resultMap;

        /**
         * Creates a new WildcardQueryScorer.
         *
         * @param similarity the similarity implementation.
         * @param reader     the index reader to use.
         */
        @SuppressWarnings({"unchecked"})
        WildcardQueryScorer(Similarity similarity, IndexReader reader) {
            super(similarity);
            this.reader = reader;
            this.cacheKey = field + '\uFFFF' + propName + '\uFFFF' + transform + '\uFFFF' + pattern;
            // check cache
            PerQueryCache cache = PerQueryCache.getInstance();
            Map<String, BitSet> m = (Map<String, BitSet>) cache.get(WildcardQueryScorer.class, reader);
            if (m == null) {
                m = new HashMap<String, BitSet>();
                cache.put(WildcardQueryScorer.class, reader, m);
            }
            resultMap = m;

            BitSet result = resultMap.get(cacheKey);
            if (result == null) {
                result = new BitSet(reader.maxDoc());
            } else {
                hitsCalculated = true;
            }
            hits = result;
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
        public boolean skipTo(int target) throws IOException {
            calculateHits();
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
         * Calculates the ids of the documents matching this wildcard query.
         * @throws IOException if an error occurs while reading from the index.
         */
        private void calculateHits() throws IOException {
            if (hitsCalculated) {
                return;
            }
            TermEnum terms = new WildcardTermEnum(reader, field, propName, pattern, transform);
            try {
                // use unpositioned TermDocs
                TermDocs docs = reader.termDocs();
                try {
                    while (terms.term() != null) {
                        docs.seek(terms);
                        while (docs.next()) {
                            hits.set(docs.doc());
                        }
                        if (!terms.next()) {
                            break;
                        }
                    }
                } finally {
                    docs.close();
                }
            } finally {
                terms.close();
            }
            hitsCalculated = true;
            // put to cache
            resultMap.put(cacheKey, hits);
        }

    }
}
