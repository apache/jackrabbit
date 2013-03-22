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
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.Weight;

import java.io.IOException;
import java.util.Set;

/**
 * Implements a query that negates documents of a context query. Documents
 * that matched the context query will not match the <code>NotQuery</code> and
 * Documents that did not match the context query will be selected by this
 * <code>NotQuery</code>.
 */
@SuppressWarnings("serial")
class NotQuery extends Query {

    /**
     * The context query to invert.
     */
    private final Query context;

    /**
     * The context scorer to invert.
     */
    private Scorer contextScorer;

    /**
     * Creates a new <code>NotQuery</code>.
     * @param context the context query.
     */
    NotQuery(Query context) {
        this.context = context;
    }

    /**
     * {@inheritDoc}
     */
    public Weight createWeight(Searcher searcher) {
        return new NotQueryWeight(searcher);
    }

    /**
     * {@inheritDoc}
     */
    public String toString(String field) {
        return "NotQuery";
    }

    /**
     * {@inheritDoc}
     */
    public void extractTerms(Set<Term> terms) {
        context.extractTerms(terms);
    }

    /**
     * {@inheritDoc}
     */
    public Query rewrite(IndexReader reader) throws IOException {
        Query cQuery = context.rewrite(reader);
        if (cQuery == context) {
            return this;
        } else {
            return new NotQuery(cQuery);
        }
    }

    /**
     * Implements a weight for this <code>NotQuery</code>.
     */
    private class NotQueryWeight extends Weight {

        /**
         * The searcher to access the index.
         */
        private final Searcher searcher;

        /**
         * Creates a new NotQueryWeight with a searcher.
         * @param searcher the searcher.
         */
        NotQueryWeight(Searcher searcher) {
            this.searcher = searcher;
        }

        /**
         * @inheritDoc
         */
        public Query getQuery() {
            return NotQuery.this;
        }

        /**
         * @inheritDoc
         */
        public float getValue() {
            return 1.0f;
        }

        /**
         * @inheritDoc
         */
        public float sumOfSquaredWeights() throws IOException {
            return 1.0f;
        }

        /**
         * @inheritDoc
         */
        public void normalize(float norm) {
        }

        /**
         * @inheritDoc
         */
        public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder,
                boolean topScorer) throws IOException {
            contextScorer = context.weight(searcher).scorer(reader, scoreDocsInOrder, topScorer);
            if (contextScorer == null) {
                // context query does not match any node
                // the inverse is to match all nodes
                return new MatchAllDocsQuery().createWeight(searcher).scorer(
                        reader, scoreDocsInOrder, false);
            }
            return new NotQueryScorer(reader);
        }

        /**
         * @throws UnsupportedOperationException always
         */
        public Explanation explain(IndexReader reader, int doc) throws IOException {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Implements a scorer that inverts the document matches of the context
     * scorer.
     */
    private class NotQueryScorer extends Scorer {

        /**
         * The index reader.
         */
        private final IndexReader reader;

        /**
         * Current document number.
         */
        private int docNo = -1;

        /**
         * Current document number of the context scorer;
         */
        private int contextNo = -1;

        private boolean firstTime = true;

        /**
         * Creates a new scorer
         * @param reader
         */
        NotQueryScorer(IndexReader reader) {
            super(Similarity.getDefault());
            this.reader = reader;
        }

        @Override
        public int nextDoc() throws IOException {
            if (docNo == NO_MORE_DOCS) {
                return docNo;
            }

            if (firstTime) {
                firstTime = false;
                // get first doc of context scorer
                int docId = contextScorer.nextDoc();
                if (docId != NO_MORE_DOCS) {
                    contextNo = docId;
                }
            }
            // move to next candidate
            do {
                docNo++;
            } while (reader.isDeleted(docNo) && docNo < reader.maxDoc());

            // check with contextScorer
            while (contextNo != -1 && contextNo == docNo) {
                docNo++;
                int docId = contextScorer.nextDoc();
                contextNo = docId == NO_MORE_DOCS ? -1 : docId;
            }
            if (docNo >= reader.maxDoc()) {
                docNo = NO_MORE_DOCS;
            }
            return docNo;
        }

        @Override
        public int docID() {
            return docNo;
        }

        @Override
        public float score() throws IOException {
            return 1.0f;
        }

        @Override
        public int advance(int target) throws IOException {
            if (docNo == NO_MORE_DOCS) {
                return docNo;
            }

            // optimize in the case of an advance to finish.
            // see https://issues.apache.org/jira/browse/JCR-3091
            if (target == NO_MORE_DOCS) {
                contextScorer.advance(target);
                docNo = NO_MORE_DOCS;
                return docNo;
            }

            if (contextNo != -1 && contextNo < target) {
                int docId = contextScorer.advance(target);
                contextNo = docId == NO_MORE_DOCS ? -1 : docId;
            }
            docNo = target - 1;
            return nextDoc();
        }

    }
}
