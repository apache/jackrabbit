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

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.*;

import java.io.IOException;
import java.util.BitSet;

/**
 * The MatchAllScorer implements a Scorer that scores / collects all
 * documents in the index that match a field.
 * In case there are no filters, this MatchAllScores simply collects
 * all documents in the index that are not marked as deleted.
 */
class MatchAllScorer extends Scorer {

    /**
     * next doc number
     */
    private int nextDoc = 0;

    /**
     * IndexReader giving access to index
     */
    private IndexReader reader;

    /**
     * Weight associated with this Scorer
     */
    private Weight weight;

    /**
     * The field to match
     */
    private String field;

    /**
     * BitSet filtering documents without content is specified field
     */
    private BitSet docFilter;

    /**
     * Explanation object. the same for all docs
     */
    private final Explanation matchExpl;

    /**
     * Creates a new MatchAllScorer.
     *
     * @param reader the IndexReader
     * @param weight associated Weight for this Scorer
     * @param field  the field name to match.
     * @throws IOException if an error occurs while collecting hits.
     *                     e.g. while reading from the search index.
     */
    MatchAllScorer(IndexReader reader, Weight weight, String field)
            throws IOException {
        super(Similarity.getDefault());
        this.reader = reader;
        this.weight = weight;
        this.field = field;
        matchExpl
                = new Explanation(Similarity.getDefault().idf(reader.maxDoc(),
                        reader.maxDoc()),
                        "matchAll");
        calculateDocFilter();
    }

    /**
     * @see Scorer#score(org.apache.lucene.search.HitCollector)
     */
    public void score(HitCollector hc) throws IOException {
        while (next()) {
            hc.collect(doc(), score());
        }
    }


    /**
     * @see org.apache.lucene.search.Scorer#next()
     */
    public boolean next() throws IOException {
        nextDoc = docFilter.nextSetBit(nextDoc + 1);
        return nextDoc > -1;
    }

    /**
     * @see org.apache.lucene.search.Scorer#doc()
     */
    public int doc() {
        return nextDoc;
    }

    /**
     * @see org.apache.lucene.search.Scorer#score()
     */
    public float score() throws IOException {
        return 1.0f;
    }

    /**
     * @see org.apache.lucene.search.Scorer#skipTo(int)
     */
    public boolean skipTo(int target) throws IOException {
        nextDoc = target - 1;
        return next();
    }

    /**
     * @see Scorer#explain
     */
    public Explanation explain(int doc) {
        return matchExpl;
    }

    /**
     * Calculates a BitSet filter that includes all the nodes
     * that have content in properties according to the field name
     * passed in the constructor of this MatchAllScorer.
     *
     * @throws IOException if an error occurs while reading from
     *                     the search index.
     */
    private void calculateDocFilter() throws IOException {
        docFilter = new BitSet(reader.maxDoc());
        // we match all terms
        TermEnum terms = null;
        try {
            terms = reader.terms(new Term(field, ""));
            while (terms.term() != null && terms.term().field() == field) {
                TermDocs termDocs = null;
                try {
                    termDocs = reader.termDocs(terms.term());
                    while (termDocs.next()) {
                        docFilter.set(termDocs.doc());
                    }
                } finally {
                    if (termDocs != null) {
                        termDocs.close();
                    }
                }
                terms.next();
            }
        } finally {
            if (terms != null) {
                terms.close();
            }
        }
    }
}
