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
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;

import java.io.IOException;

/**
 * This class implements the Weight calculation for the MatchAllQuery.
 */
@SuppressWarnings("serial")
class MatchAllWeight extends AbstractWeight {

    /**
     * Name of the field to match.
     */
    private final String field;

    /**
     * the MatchAllQuery
     */
    private final Query query;

    private final PerQueryCache cache;

    /**
     * the weight value
     */
    private float value;

    /**
     * doc frequency for this weight
     */
    private float idf;

    /**
     * the query weight
     */
    private float queryWeight;

    /**
     * @param query
     * @param searcher
     * @param field name of the field to match
     */
    MatchAllWeight(
            Query query, Searcher searcher, String field, PerQueryCache cache) {
        super(searcher);
        this.query = query;
        this.field = field;
        this.cache = cache;
    }

    /**
     * Creates a {@link MatchAllScorer} instance.
     *
     * @param reader index reader
     * @return a {@link MatchAllScorer} instance
     */
    protected Scorer createScorer(IndexReader reader, boolean scoreDocsInOrder,
            boolean topScorer) throws IOException {
        return new MatchAllScorer(reader, field, cache);
    }

    /**
     * {@inheritDoc}
     */
    public Query getQuery() {
        return query;
    }

    /**
     * {@inheritDoc}
     */
    public float getValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public float sumOfSquaredWeights() throws IOException {
        idf = searcher.getSimilarity().idf(searcher.maxDoc(), searcher.maxDoc()); // compute idf
        queryWeight = idf * 1.0f; // boost         // compute query weight
        return queryWeight * queryWeight;           // square it
    }

    /**
     * {@inheritDoc}
     */
    public void normalize(float queryNorm) {
        queryWeight *= queryNorm;                   // normalize query weight
        value = queryWeight * idf;                  // idf for document
    }

    /**
     * {@inheritDoc}
     */
    public Explanation explain(IndexReader reader, int doc) throws IOException {
        return new Explanation(Similarity.getDefault().idf(reader.maxDoc(), reader.maxDoc()),
                "matchAll");
    }
}
