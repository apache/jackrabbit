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
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.HitCollector;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.Term;

import java.io.IOException;
import java.util.BitSet;
import java.util.Set;
import java.util.HashSet;

/**
 * Implements a lucene <code>Query</code> which filters a sub query by checking
 * whether the nodes selected by that sub query are descendants or self of
 * nodes selected by a context query.
 */
class DescendantSelfAxisQuery extends Query {

    /** The context query */
    private final Query contextQuery;

    /** The scorer of the context query */
    private Scorer contextScorer;

    /** The sub query to filter */
    private final Query subQuery;

    /** The scorer of the sub query to filter */
    private Scorer subScorer;

    /**
     * Creates a new <code>DescendantSelfAxisQuery</code> based on a
     * <code>context</code> query and filtering the <code>sub</code> query.
     * @param context the context for this query.
     * @param sub the sub query.
     */
    public DescendantSelfAxisQuery(Query context, Query sub) {
        this.contextQuery = context;
        this.subQuery = sub;
    }

    /**
     * Creates a <code>Weight</code> instance for this query.
     * @param searcher the <code>Searcher</code> instance to use.
     * @return a <code>DescendantSelfAxisWeight</code>.
     */
    protected Weight createWeight(Searcher searcher) {
        return new DescendantSelfAxisWeight(searcher);
    }

    /**
     * Always returns 'DescendantSelfAxisQuery'.
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

        /** The searcher in use */
        private final Searcher searcher;

        /**
         * Creates a new <code>DescendantSelfAxisWeight</code> instance using
         * <code>searcher</code>.
         * @param searcher a <code>Searcher</code> instance.
         */
        private DescendantSelfAxisWeight(Searcher searcher) {
            this.searcher = searcher;
        }

        /**
         * Returns this <code>DescendantSelfAxisQuery</code>.
         * @return this <code>DescendantSelfAxisQuery</code>.
         */
        public Query getQuery() {
            return DescendantSelfAxisQuery.this;
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
         * Creates a scorer for this <code>DescendantSelfAxisScorer</code>.
         * @param reader a reader for accessing the index.
         * @return a <code>DescendantSelfAxisScorer</code>.
         * @throws IOException if an error occurs while reading from the index.
         */
        public Scorer scorer(IndexReader reader) throws IOException {
            contextScorer = contextQuery.weight(searcher).scorer(reader);
            subScorer = subQuery.weight(searcher).scorer(reader);
            return new DescendantSelfAxisScorer(searcher.getSimilarity(), reader);
        }

        /**
         * @see org.apache.lucene.search.Weight#explain(org.apache.lucene.index.IndexReader, int)
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

        /** An <code>IndexReader</code> to access the index. */
        private final IndexReader reader;

        /** BitSet storing the id's of selected documents */
        private final BitSet hits;

        /** BitSet storing the id's of selected documents from the sub query */
        private final BitSet subHits;

        /** List of UUIDs of selected nodes by the context query */
        private Set contextUUIDs = null;

        /** The next document id to return */
        private int nextDoc = -1;

        /**
         * Creates a new <code>DescendantSelfAxisScorer</code>.
         * @param similarity the <code>Similarity</code> instance to use.
         * @param reader for index access.
         */
        protected DescendantSelfAxisScorer(Similarity similarity, IndexReader reader) {
            super(similarity);
            this.reader = reader;
            this.hits = new BitSet(reader.maxDoc());
            this.subHits = new BitSet(reader.maxDoc());
        }

        /**
         * @see Scorer#score(org.apache.lucene.search.HitCollector)
         */
        public void score(HitCollector hc) throws IOException {
            while (next()) {
                hc.collect(doc(), score());
            }
        }

        public boolean next() throws IOException {
            calculateSubHits();
            nextDoc = subHits.nextSetBit(nextDoc + 1);
            while (nextDoc > -1) {
                // check if nextDoc is really valid
                String uuid = reader.document(nextDoc).get(FieldNames.UUID);
                if (contextUUIDs.contains(uuid)) {
                    return true;
                }
                // check if nextDoc is a descendant of one of the context nodes
                String parentUUID = reader.document(nextDoc).get(FieldNames.PARENT);
                while (parentUUID != null && !contextUUIDs.contains(parentUUID)) {
                    // traverse
                    TermDocs ancestor = reader.termDocs(new Term(FieldNames.UUID, parentUUID));
                    if (ancestor.next()) {
                        parentUUID = reader.document(ancestor.doc()).get(FieldNames.PARENT);
                        if (parentUUID.length() == 0) {
                            parentUUID = null;
                        }
                    } else {
                        parentUUID = null;
                    }
                }
                if (parentUUID != null) {
                    return true;
                }
                // try next
                nextDoc = subHits.nextSetBit(nextDoc + 1);
            }
            return false;
        }

        public int doc() {
            return nextDoc;
        }

        public float score() throws IOException {
            return 1.0f;
        }

        public boolean skipTo(int target) throws IOException {
            nextDoc = target - 1;
            return next();
        }

        private void calculateSubHits() throws IOException {
            if (contextUUIDs == null) {
                contextUUIDs = new HashSet();
                contextScorer.score(new HitCollector() {
                    public void collect(int doc, float score) {
                        // @todo maintain cache of doc id hierarchy
                        hits.set(doc);
                    }
                }); // find all
                for (int i = hits.nextSetBit(0); i >= 0; i = hits.nextSetBit(i + 1)) {
                    contextUUIDs.add(reader.document(i).get(FieldNames.UUID));
                }

                // reuse for final hits
                hits.clear();

                subScorer.score(new HitCollector() {
                    public void collect(int doc, float score) {
                        subHits.set(doc);
                    }
                });
            }
        }

        /**
         * @exception UnsupportedOperationException this implementation always
         * throws an <code>UnsupportedOperationException</code>.
         */
        public Explanation explain(int doc) throws IOException {
            throw new UnsupportedOperationException();
        }
    }


}
