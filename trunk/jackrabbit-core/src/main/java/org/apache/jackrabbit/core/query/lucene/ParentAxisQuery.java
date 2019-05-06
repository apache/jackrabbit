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

import org.apache.jackrabbit.core.query.lucene.hits.AbstractHitCollector;
import org.apache.jackrabbit.core.query.lucene.hits.Hits;
import org.apache.jackrabbit.core.query.lucene.hits.ScorerHits;
import org.apache.jackrabbit.spi.Name;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.Weight;

import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * <code>ParentAxisQuery</code> selects the parent nodes of a context query.
 */
@SuppressWarnings("serial")
class ParentAxisQuery extends Query {

    /**
     * The context query
     */
    private final Query contextQuery;

    /**
     * The nameTest to apply on the parent axis, or <code>null</code> if any
     * parent node should be selected.
     */
    private final Name nameTest;

    /**
     * The index format version.
     */
    private final IndexFormatVersion version;

    /**
     * The internal namespace mappings.
     */
    private final NamespaceMappings nsMappings;

    /**
     * The scorer of the context query
     */
    private Scorer contextScorer;

    /**
     * Creates a new <code>ParentAxisQuery</code> based on a
     * <code>context</code> query.
     *
     * @param context  the context for this query.
     * @param nameTest a name test or <code>null</code> if any parent node is
     *                 selected.
     * @param version the index format version.
     * @param nsMappings the internal namespace mappings.
     */
    ParentAxisQuery(Query context, Name nameTest,
                   IndexFormatVersion version, NamespaceMappings nsMappings) {
        this.contextQuery = context;
        this.nameTest = nameTest;
        this.version = version;
        this.nsMappings = nsMappings;
    }

    /**
     * Creates a <code>Weight</code> instance for this query.
     *
     * @param searcher the <code>Searcher</code> instance to use.
     * @return a <code>ParentAxisWeight</code>.
     */
    public Weight createWeight(Searcher searcher) {
        return new ParentAxisWeight(searcher);
    }

    /**
     * {@inheritDoc}
     */
    public void extractTerms(Set<Term> terms) {
        contextQuery.extractTerms(terms);
    }

    /**
     * {@inheritDoc}
     */
    public Query rewrite(IndexReader reader) throws IOException {
        Query cQuery = contextQuery.rewrite(reader);
        if (cQuery == contextQuery) {
            return this;
        } else {
            return new ParentAxisQuery(cQuery, nameTest, version, nsMappings);
        }
    }

    /**
     * Always returns 'ParentAxisQuery'.
     *
     * @param field the name of a field.
     * @return 'ParentAxisQuery'.
     */
    public String toString(String field) {
        StringBuffer sb = new StringBuffer();
        sb.append("ParentAxisQuery(");
        sb.append(contextQuery);
        sb.append(", ");
        sb.append(nameTest);
        sb.append(")");
        return sb.toString();
    }

    //-----------------------< ParentAxisWeight >-------------------------------

    /**
     * The <code>Weight</code> implementation for this <code>ParentAxisQuery</code>.
     */
    private class ParentAxisWeight extends Weight {

        /**
         * The searcher in use
         */
        private final Searcher searcher;

        /**
         * Creates a new <code>ParentAxisWeight</code> instance using
         * <code>searcher</code>.
         *
         * @param searcher a <code>Searcher</code> instance.
         */
        private ParentAxisWeight(Searcher searcher) {
            this.searcher = searcher;
        }

        /**
         * Returns this <code>ParentAxisQuery</code>.
         *
         * @return this <code>ParentAxisQuery</code>.
         */
        public Query getQuery() {
            return ParentAxisQuery.this;
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
         * Creates a scorer for this <code>ParentAxisQuery</code>.
         *
         * @param reader a reader for accessing the index.
         * @return a <code>ParentAxisScorer</code>.
         * @throws IOException if an error occurs while reading from the index.
         */
        public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder,
                boolean topScorer) throws IOException {
            contextScorer = contextQuery.weight(searcher).scorer(reader, scoreDocsInOrder, false);
            HierarchyResolver resolver = (HierarchyResolver) reader;
            return new ParentAxisScorer(searcher.getSimilarity(),
                    reader, searcher, resolver);
        }

        /**
         * {@inheritDoc}
         */
        public Explanation explain(IndexReader reader, int doc) throws IOException {
            return new Explanation();
        }
    }

    //--------------------------< ParentAxisScorer >----------------------------

    /**
     * Implements a <code>Scorer</code> for this <code>ParentAxisQuery</code>.
     */
    private class ParentAxisScorer extends Scorer {

        /**
         * An <code>IndexReader</code> to access the index.
         */
        private final IndexReader reader;

        /**
         * The <code>HierarchyResolver</code> of the index.
         */
        private final HierarchyResolver hResolver;

        /**
         * The searcher instance.
         */
        private final Searcher searcher;

        /**
         * BitSet storing the id's of selected documents
         */
        private BitSet hits;

        /**
         * Map that contains the scores from matching documents from the context
         * query. To save memory only scores that are not equal to the score
         * value of the first match are put to this map.
         * <p>
         * key=[Integer] id of selected document from context query<br>
         * value=[Float] score for that document
         */
        private final Map<Integer, Float> scores = new HashMap<Integer, Float>();

        /**
         * The next document id to return
         */
        private int nextDoc = -1;

        /**
         * The score of the first match.
         */
        private Float firstScore;

        /**
         * Creates a new <code>ParentAxisScorer</code>.
         *
         * @param similarity the <code>Similarity</code> instance to use.
         * @param reader     for index access.
         * @param searcher   the index searcher.
         * @param resolver   the hierarchy resolver.
         */
        protected ParentAxisScorer(Similarity similarity,
                                   IndexReader reader,
                                   Searcher searcher,
                                   HierarchyResolver resolver) {
            super(similarity);
            this.reader = reader;
            this.searcher = searcher;
            this.hResolver = resolver;
        }

        @Override
        public int nextDoc() throws IOException {
            if (nextDoc == NO_MORE_DOCS) {
                return nextDoc;
            }

            calculateParent();
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
        public float score() throws IOException {
            Float score = scores.get(nextDoc);
            if (score == null) {
                score = firstScore;
            }
            return score;
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
            calculateParent();
            nextDoc = hits.nextSetBit(target);
            if (nextDoc < 0) {
                nextDoc = NO_MORE_DOCS;
            }
            return nextDoc;
        }

        private void calculateParent() throws IOException {
            if (hits == null) {
                hits = new BitSet(reader.maxDoc());

                final IOException[] ex = new IOException[1];
                if (contextScorer != null) {
                    contextScorer.score(new AbstractHitCollector() {
                        private int[] docs = new int[1];

                        @Override
                        protected void collect(int doc, float score) {
                            try {
                                docs = hResolver.getParents(doc, docs);
                                if (docs.length == 1) {
                                    // optimize single value
                                    hits.set(docs[0]);
                                    if (firstScore == null) {
                                        firstScore = score;
                                    } else if (firstScore != score) {
                                        scores.put(doc, score);
                                    }
                                } else {
                                    for (int docNum : docs) {
                                        hits.set(docNum);
                                        if (firstScore == null) {
                                            firstScore = score;
                                        } else if (firstScore != score) {
                                            scores.put(doc, score);
                                        }
                                    }
                                }
                            } catch (IOException e) {
                                ex[0] = e;
                            }
                        }
                    });
                }

                if (ex[0] != null) {
                    throw ex[0];
                }

                // filter out documents that do not match the name test
                if (nameTest != null) {
                    Query nameQuery = new NameQuery(nameTest, version, nsMappings);
                    Hits nameHits = new ScorerHits(nameQuery.weight(searcher).scorer(reader, true, false));
                    for (int i = hits.nextSetBit(0); i >= 0; i = hits.nextSetBit(i + 1)) {
                        int doc = nameHits.skipTo(i);
                        if (doc == -1) {
                            // no more name tests, clear remaining
                            hits.clear(i, hits.length());
                        } else {
                            // assert doc >= i
                            if (doc > i) {
                                // clear hits
                                hits.clear(i, doc);
                                i = doc;
                            }
                        }
                    }
                }
            }
        }
    }
}
