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
package org.apache.jackrabbit.core.search.lucene;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.HitCollector;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;

import java.io.IOException;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * Implements a lucene <code>Query</code> which returns the child nodes of the
 * nodes selected by another <code>Query</code>.
 */
class ChildAxisQuery extends Query {

    /** The context query */
    private final Query contextQuery;

    /** The scorer of the context query */
    private Scorer contextScorer;

    /**
     * Creates a new <code>ChildAxisQuery</code> based on a <code>context</code>
     * query.
     * @param context the context for this query.
     */
    ChildAxisQuery(Query context) {
        this.contextQuery = context;
    }

    /**
     * Creates a <code>Weight</code> instance for this query.
     * @param searcher the <code>Searcher</code> instance to use.
     * @return a <code>ChildAxisWeight</code>.
     */
    protected Weight createWeight(Searcher searcher) {
        return new ChildAxisWeight(searcher);
    }

    /**
     * Always returns 'ChildAxisQuery'.
     * @param field the name of a field.
     * @return 'ChildAxisQuery'.
     */
    public String toString(String field) {
        return "ChildAxisQuery";
    }

    //-------------------< ChildAxisWeight >------------------------------------

    /**
     * The <code>Weight</code> implementation for this <code>ChildAxisQuery</code>.
     */
    private class ChildAxisWeight implements Weight {

        /** The searcher in use */
        private final Searcher searcher;

        /**
         * Creates a new <code>ChildAxisWeight</code> instance using
         * <code>searcher</code>.
         * @param searcher a <code>Searcher</code> instance.
         */
        private ChildAxisWeight(Searcher searcher) {
            this.searcher = searcher;
        }

        /**
         * Returns this <code>ChildAxisQuery</code>.
         * @return this <code>ChildAxisQuery</code>.
         */
        public Query getQuery() {
            return ChildAxisQuery.this;
        }

        /**
         * @see org.apache.lucene.search.Weight#getValue()
         */
        public float getValue() {
            // @todo implement properly
            return 0;
        }

        /**
         * @see org.apache.lucene.search.Weight#sumOfSquaredWeights()
         */
        public float sumOfSquaredWeights() throws IOException {
            // @todo implement properly
            return 0;
        }

        /**
         * @see org.apache.lucene.search.Weight#normalize(float)
         */
        public void normalize(float norm) {
            // @todo implement properly
        }

        /**
         * Creates a scorer for this <code>ChildAxisQuery</code>.
         * @param reader a reader for accessing the index.
         * @return a <code>ChildAxisScorer</code>.
         * @throws IOException if an error occurs while reading from the index.
         */
        public Scorer scorer(IndexReader reader) throws IOException {
            contextScorer = contextQuery.weight(searcher).scorer(reader);
            return new ChildAxisScorer(searcher.getSimilarity(), reader);
        }

        /**
         * @see org.apache.lucene.search.Weight#explain(org.apache.lucene.index.IndexReader, int)
         */
        public Explanation explain(IndexReader reader, int doc) throws IOException {
            return new Explanation();
        }
    }

    //----------------------< ChildAxisScorer >---------------------------------

    /**
     * Implements a <code>Scorer</code> for this <code>ChildAxisQuery</code>.
     */
    private class ChildAxisScorer extends Scorer {

        /** An <code>IndexReader</code> to access the index. */
        private final IndexReader reader;

        /** BitSet storing the id's of selected documents */
        private final BitSet hits;

        /** List of UUIDs of selected nodes */
        private List uuids = null;

        /** The next document id to return */
        private int nextDoc = -1;

        /**
         * Creates a new <code>ChildAxisScorer</code>.
         * @param similarity the <code>Similarity</code> instance to use.
         * @param reader for index access.
         */
        protected ChildAxisScorer(Similarity similarity, IndexReader reader) {
            super(similarity);
            this.reader = reader;
            this.hits = new BitSet(reader.maxDoc());
        }

        /**
         * @see Scorer#score(org.apache.lucene.search.HitCollector)
         */
        public void score(HitCollector hc) throws IOException {
            calculateChildren();

            int next = hits.nextSetBit(0);
            while (next > -1) {
                hc.collect(next, 1.0f);
                // move to next doc
                next = hits.nextSetBit(next + 1);
            }
        }

        public boolean next() throws IOException {
            calculateChildren();
            nextDoc = hits.nextSetBit(nextDoc + 1);
            return nextDoc > -1;
        }

        public int doc() {
            return nextDoc;
        }

        public float score() throws IOException {
            // todo implement
            return 1.0f;
        }

        public boolean skipTo(int target) throws IOException {
            nextDoc = hits.nextSetBit(target);
            return nextDoc > -1;
        }

        /**
         * @exception UnsupportedOperationException this implementation always
         * throws an <code>UnsupportedOperationException</code>.
         */
        public Explanation explain(int doc) throws IOException {
            throw new UnsupportedOperationException();
        }

        private void calculateChildren() throws IOException {
            if (uuids == null) {
                uuids = new ArrayList();
                contextScorer.score(new HitCollector() {
                    public void collect(int doc, float score) {
                        // @todo maintain cache of doc id hierarchy
                        hits.set(doc);
                    }
                }); // find all
                for (int i = hits.nextSetBit(0); i >= 0; i = hits.nextSetBit(i + 1)) {
                    String uuid = reader.document(i).get(FieldNames.UUID);
                    uuids.add(uuid);
                }

                hits.clear();
                for (Iterator it = uuids.iterator(); it.hasNext();) {
                    TermDocs children = reader.termDocs(new Term(FieldNames.PARENT, (String) it.next()));
                    while (children.next()) {
                        hits.set(children.doc());
                    }
                }
            }
        }
    }
}
