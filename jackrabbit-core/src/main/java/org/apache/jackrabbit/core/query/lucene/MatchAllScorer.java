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
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Similarity;

import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

/**
 * The MatchAllScorer implements a Scorer that scores / collects all
 * documents in the index that match a field.
 */
class MatchAllScorer extends Scorer {

    /**
     * next doc number
     */
    private int nextDoc = -1;

    /**
     * IndexReader giving access to index
     */
    private IndexReader reader;

    /**
     * The field to match
     */
    private String field;

    /**
     * BitSet filtering documents without content is specified field
     */
    private BitSet docFilter;

    /**
     * Creates a new MatchAllScorer.
     *
     * @param reader the IndexReader
     * @param field  the field name to match.
     * @throws IOException if an error occurs while collecting hits.
     *                     e.g. while reading from the search index.
     */
    MatchAllScorer(IndexReader reader, String field, PerQueryCache cache)
            throws IOException {
        super(Similarity.getDefault());
        this.reader = reader;
        this.field = field;
        calculateDocFilter(cache);
    }

    @Override
    public void score(Collector collector) throws IOException {
        collector.setScorer(this);

        while (nextDoc() != NO_MORE_DOCS) {
            collector.collect(docID());
        }
    }

    @Override
    public int nextDoc() throws IOException {
        if (nextDoc == NO_MORE_DOCS) {
            return nextDoc;
        }

        nextDoc = docFilter.nextSetBit(nextDoc + 1);
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

        nextDoc = target - 1;
        return nextDoc();
    }

    /**
     * Calculates a BitSet filter that includes all the nodes
     * that have content in properties according to the field name
     * passed in the constructor of this MatchAllScorer.
     *
     * @throws IOException if an error occurs while reading from
     *                     the search index.
     */
    @SuppressWarnings({"unchecked"})
    private void calculateDocFilter(PerQueryCache cache) throws IOException {
        Map<String, BitSet> readerCache = (Map<String, BitSet>) cache.get(MatchAllScorer.class, reader);
        if (readerCache == null) {
            readerCache = new HashMap<String, BitSet>();
            cache.put(MatchAllScorer.class, reader, readerCache);
        }
        // get BitSet for field
        docFilter = readerCache.get(field);

        if (docFilter != null) {
            // use cached BitSet;
            return;
        }

        // otherwise calculate new
        docFilter = new BitSet(reader.maxDoc());
        // we match all terms
        String namedValue = FieldNames.createNamedValue(field, "");
        TermEnum terms = reader.terms(new Term(FieldNames.PROPERTIES, namedValue));
        try {
            TermDocs docs = reader.termDocs();
            try {
                while (terms.term() != null
                        && terms.term().field() == FieldNames.PROPERTIES
                        && terms.term().text().startsWith(namedValue)) {
                    docs.seek(terms);
                    while (docs.next()) {
                        docFilter.set(docs.doc());
                    }
                    terms.next();
                }
            } finally {
                docs.close();
            }
        } finally {
            terms.close();
        }

        // put BitSet into cache
        readerCache.put(field, docFilter);
    }
}
