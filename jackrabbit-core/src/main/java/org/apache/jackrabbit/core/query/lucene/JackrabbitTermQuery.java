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

import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexReader;

/**
 * <code>JackrabbitTermQuery</code> implements a {@link TermQuery} where score
 * values are retrieved on a per index segment basis using {@link MultiScorer}.
 */
public class JackrabbitTermQuery extends TermQuery {

    private static final long serialVersionUID = 4244799812287335957L;

    public JackrabbitTermQuery(Term t) {
        super(t);
    }

    public Weight createWeight(Searcher searcher) throws IOException {
        // use a FilterSearcher to prevent per segment searches
        // done by lucene. we handle that on our own
        // see instanceof check in lucene TermWeight constructor
        return new JackrabbitTermWeight(searcher,
                super.createWeight(new FilterSearcher(searcher)));
    }

    /**
     * The weight implementation.
     */
    protected class JackrabbitTermWeight extends AbstractWeight {

        private static final long serialVersionUID = -2070964510010945854L;

        /**
         * The default lucene TermQuery weight.
         */
        private final Weight weight;

        public JackrabbitTermWeight(Searcher searcher, Weight weight) {
            super(searcher);
            this.weight = weight;
        }

        /**
         * {@inheritDoc}
         */
        protected Scorer createScorer(IndexReader reader, boolean scoreDocsInOrder,
                boolean topScorer) throws IOException {
            return weight.scorer(reader, scoreDocsInOrder, topScorer);
        }

        /**
         * {@inheritDoc}
         */
        public Query getQuery() {
            return JackrabbitTermQuery.this;
        }

        /**
         * {@inheritDoc}
         */
        public float getValue() {
            return weight.getValue();
        }

        /**
         * {@inheritDoc}
         */
        public float sumOfSquaredWeights() throws IOException {
            return weight.sumOfSquaredWeights();
        }

        /**
         * {@inheritDoc}
         */
        public void normalize(float norm) {
            weight.normalize(norm);
        }

        /**
         * {@inheritDoc}
         */
        public Explanation explain(IndexReader reader, int doc) throws
                IOException {
            return weight.explain(reader, doc);
        }
    }
}
