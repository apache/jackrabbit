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
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.Weight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implements a variant of the lucene class {@code org.apache.lucene.search.RangeQuery}.
 * This class does not rewrite to basic {@link org.apache.lucene.search.TermQuery}
 * but will calculate the matching documents itself. That way a
 * <code>TooManyClauses</code> can be avoided.
 */
@SuppressWarnings("serial")
public class RangeQuery extends Query implements Transformable {

    /**
     * Logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(RangeQuery.class);

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
     * How the term enum is transformed before it is compared to lower and upper
     * term.
     */
    private int transform = TRANSFORM_NONE;

    private final PerQueryCache cache;

    /**
     * The rewritten range query or <code>null</code> if the range spans more
     * than {@link org.apache.lucene.search.BooleanQuery#maxClauseCount} terms.
     */
    private Query stdRangeQuery;

    /**
     * Creates a new RangeQuery. The lower or the upper term may be
     * <code>null</code>, but not both!
     *
     * @param lowerTerm the lower term of the interval, or <code>null</code>
     * @param upperTerm the upper term of the interval, or <code>null</code>.
     * @param inclusive if <code>true</code> the interval is inclusive.
     */
    public RangeQuery(
            Term lowerTerm, Term upperTerm, boolean inclusive,
            PerQueryCache cache) {
        this(lowerTerm, upperTerm, inclusive, TRANSFORM_NONE, cache);
    }

    /**
     * Creates a new RangeQuery. The lower or the upper term may be
     * <code>null</code>, but not both!
     *
     * @param lowerTerm the lower term of the interval, or <code>null</code>
     * @param upperTerm the upper term of the interval, or <code>null</code>.
     * @param inclusive if <code>true</code> the interval is inclusive.
     * @param transform how term enums are transformed when read from the index.
     */
    public RangeQuery(
            Term lowerTerm, Term upperTerm, boolean inclusive, int transform,
            PerQueryCache cache) {
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
        this.transform = transform;
        this.cache = cache;
    }

    /**
     * {@inheritDoc}
     */
    public void setTransformation(int transformation) {
        this.transform = transformation;
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
        if (transform == TRANSFORM_NONE) {
            TermRangeQuery stdRangeQueryImpl = new TermRangeQuery(
                    lowerTerm.field(), lowerTerm.text(), upperTerm.text(),
                    inclusive, inclusive);
            stdRangeQueryImpl
                    .setRewriteMethod(MultiTermQuery.CONSTANT_SCORE_BOOLEAN_QUERY_REWRITE);
            try {
                stdRangeQuery = stdRangeQueryImpl.rewrite(reader);
                return stdRangeQuery;
            } catch (BooleanQuery.TooManyClauses e) {
                log.debug("Too many terms to enumerate, using custom RangeQuery");
                // failed, use own implementation
                return this;
            }
        } else {
            // always use our implementation when we need to transform the
            // term enum
            return this;
        }
    }

    /**
     * Creates the <code>Weight</code> for this query.
     *
     * @param searcher the searcher to use for the <code>Weight</code>.
     * @return the <code>Weigth</code> for this query.
     */
    public Weight createWeight(Searcher searcher) {
        return new RangeQueryWeight(searcher, cache);
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
     * {@inheritDoc}
     */
    public void extractTerms(Set<Term> terms) {
        if (stdRangeQuery != null) {
            stdRangeQuery.extractTerms(terms);
        }
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
    private class RangeQueryWeight extends AbstractWeight {

        private final PerQueryCache cache;

        /**
         * Creates a new <code>RangeQueryWeight</code> instance using
         * <code>searcher</code>.
         *
         * @param searcher a <code>Searcher</code> instance.
         */
        RangeQueryWeight(Searcher searcher, PerQueryCache cache) {
            super(searcher);
            this.cache = cache;
        }

        /**
         * Creates a {@link RangeQueryScorer} instance.
         *
         * @param reader index reader
         * @return a {@link RangeQueryScorer} instance
         */
        protected Scorer createScorer(IndexReader reader, boolean scoreDocsInOrder,
                boolean topScorer) {
            return new RangeQueryScorer(searcher.getSimilarity(), reader, cache);
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
        private final Map<String, BitSet> resultMap;

        /**
         * Creates a new RangeQueryScorer.
         * @param similarity the similarity implementation.
         * @param reader the index reader to use.
         */
        @SuppressWarnings({"unchecked"})
        RangeQueryScorer(
                Similarity similarity, IndexReader reader,
                PerQueryCache cache) {
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
            key.append('\uFFFF');
            key.append(transform);
            this.cacheKey = key.toString();
            // check cache
            Map<String, BitSet> m = (Map<String, BitSet>) cache.get(RangeQueryScorer.class, reader);
            if (m == null) {
                m = new HashMap<String, BitSet>();
                cache.put(RangeQueryScorer.class, reader, m);
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

        @Override
        public int nextDoc() throws IOException {
            if (nextDoc == NO_MORE_DOCS) {
                return nextDoc;
            }

            calculateHits();
            nextDoc = hits.nextSetBit(nextDoc + 1);
            if (nextDoc < 0) {
                nextDoc = NO_MORE_DOCS;
            }
            return nextDoc;
        }

        @Override
        public int docID() {
            return nextDoc;
        }

        @Override
        public float score() {
            return 1.0f;
        }

        @Override
        public int advance(int target) throws IOException {
            if (nextDoc == NO_MORE_DOCS) {
                return nextDoc;
            }
            // optimize in the case of an advance to finish.
            // see https://issues.apache.org/jira/browse/JCR-3091
            if (target == NO_MORE_DOCS) {
                nextDoc = NO_MORE_DOCS;
                return nextDoc;
            }

            calculateHits();
            nextDoc = hits.nextSetBit(target);
            if (nextDoc < 0) {
                nextDoc = NO_MORE_DOCS;
            }
            return nextDoc;
        }

        /**
         * Calculates the ids of the documents matching this range query.
         * @throws IOException if an error occurs while reading from the index.
         */
        private void calculateHits() throws IOException {
            if (hitsCalculated) {
                return;
            }

            String testField = getField();

            boolean checkLower = false;
            if (!inclusive || transform != TRANSFORM_NONE) {
                // make adjustments to set to exclusive
                checkLower = true;
            }

            int propNameLength = FieldNames.getNameLength(lowerTerm.text());
            String namePrefix = "";
            if (propNameLength > 0) {
                namePrefix = lowerTerm.text().substring(0, propNameLength);
            }
            List<Term> startTerms = new ArrayList<Term>(2);

            if (transform == TRANSFORM_NONE || lowerTerm.text().length() <= propNameLength) {
                // use lowerTerm as is
                startTerms.add(lowerTerm);
            } else {
                // first enumerate terms using lower case start character
                StringBuffer termText = new StringBuffer(propNameLength + 1);
                termText.append(lowerTerm.text().subSequence(0, propNameLength));
                char startCharacter = lowerTerm.text().charAt(propNameLength);
                termText.append(Character.toLowerCase(startCharacter));
                startTerms.add(new Term(lowerTerm.field(), termText.toString()));
                // second enumerate terms using upper case start character
                termText.setCharAt(termText.length() - 1, Character.toUpperCase(startCharacter));
                startTerms.add(new Term(lowerTerm.field(), termText.toString()));
            }

            for (Term startTerm : startTerms) {
                TermEnum terms = reader.terms(startTerm);
                try {
                    TermDocs docs = reader.termDocs();
                    try {
                        do {
                            Term term = terms.term();
                            if (term != null && term.field() == testField && term.text().startsWith(namePrefix)) {
                                if (checkLower) {
                                    int compare = termCompare(term.text(), lowerTerm.text(), propNameLength);
                                    if (compare > 0 || compare == 0 && inclusive) {
                                        // do not check lower term anymore if no
                                        // transformation is done on the term enum
                                        checkLower = transform != TRANSFORM_NONE;
                                    } else {
                                        // continue with next term
                                        continue;
                                    }
                                }
                                if (upperTerm != null) {
                                    int compare = termCompare(term.text(), upperTerm.text(), propNameLength);
                                    // if beyond the upper term, or is exclusive and
                                    // this is equal to the upper term
                                    if ((compare > 0) || (!inclusive && compare == 0)) {
                                        // only break out if no transformation
                                        // was done on the term from the enum
                                        if (transform == TRANSFORM_NONE) {
                                            break;
                                        } else {
                                            // because of the transformation
                                            // it is possible that the next
                                            // term will be included again if
                                            // we still enumerate on the same
                                            // property name
                                            if (term.text().startsWith(namePrefix)) {
                                                continue;
                                            } else {
                                                break;
                                            }
                                        }
                                    }
                                }

                                docs.seek(terms);
                                while (docs.next()) {
                                    hits.set(docs.doc());
                                }
                            } else {
                                break;
                            }
                        } while (terms.next());
                    } finally {
                        docs.close();
                    }
                } finally {
                    terms.close();
                }
            }

            hitsCalculated = true;
            // put to cache
            resultMap.put(cacheKey, hits);
        }

        /**
         * Compares the <code>text</code> with the <code>other</code> String. This
         * implementation behaves like {@link String#compareTo(Object)} but also
         * respects the {@link RangeQuery#transform} property.
         *
         * @param text   the text to compare to <code>other</code>. The
         *               transformation function is applied to this parameter before
         *               it is compared to <code>other</code>.
         * @param other  the other String.
         * @param offset start comparing the two strings at <code>offset</code>.
         * @return see {@link String#compareTo(Object)}. But also respects {@link
         *         RangeQuery#transform}.
         */
        private int termCompare(String text, String other, int offset) {
            OffsetCharSequence seq1 = new OffsetCharSequence(offset, text, transform);
            OffsetCharSequence seq2 = new OffsetCharSequence(offset, other);
            return seq1.compareTo(seq2);
        }
    }
}
