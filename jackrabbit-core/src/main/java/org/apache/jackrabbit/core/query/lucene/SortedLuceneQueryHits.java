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
import java.util.ArrayList;
import java.util.List;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopFieldDocCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps a lucene query result and adds a close method that allows to release
 * resources after a query has been executed and the results have been read
 * completely.
 */
public final class SortedLuceneQueryHits extends AbstractQueryHits {

    /**
     * The Logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(SortedLuceneQueryHits.class);

    /**
     * The upper limit for the initial fetch size.
     */
    private static final int MAX_FETCH_SIZE = 32 * 1024;

    /**
     * The lower limit for the initial fetch size.
     */
    private static final int MIN_FETCH_SIZE = 32;

    /**
     * The IndexReader in use by the lucene hits.
     */
    private final IndexReader reader;

    /**
     * The index searcher.
     */
    private final JackrabbitIndexSearcher searcher;

    /**
     * The query to execute.
     */
    private final Query query;

    /**
     * The sort criteria.
     */
    private final Sort sort;

    /**
     * The index of the current hit. Initially invalid.
     */
    private int hitIndex = -1;

    /**
     * The score docs.
     */
    private final List<ScoreDoc> scoreDocs = new ArrayList<ScoreDoc>();

    /**
     * The total number of hits.
     */
    private int size;

    /**
     * Number of hits to retrieve.
     */
    private int numHits;

    /**
     * Creates a new <code>QueryHits</code> instance wrapping <code>hits</code>.
     *
     * @param reader          the IndexReader in use.
     * @param searcher        the index searcher.
     * @param query           the query to execute.
     * @param sort            the sort criteria.
     * @param resultFetchHint a hint on how many results should be fetched.
     * @throws IOException if an error occurs while reading from the index.
     */
    public SortedLuceneQueryHits(IndexReader reader,
                                 JackrabbitIndexSearcher searcher,
                                 Query query,
                                 Sort sort,
                                 long resultFetchHint) throws IOException {
        this.reader = reader;
        this.searcher = searcher;
        this.query = query;
        this.sort = sort;
        this.numHits = (int) Math.min(
                Math.max(resultFetchHint, MIN_FETCH_SIZE),
                MAX_FETCH_SIZE);
        getHits();
    }

    /**
     * {@inheritDoc}
     */
    public int getSize() {
      return size;
    }

    /**
     * {@inheritDoc}
     */
    public ScoreNode nextScoreNode() throws IOException {
        if (++hitIndex >= size) {
            // no more score nodes
            return null;
        } else if (hitIndex >= scoreDocs.size()) {
            // refill at least numHits or twice hitIndex
            this.numHits = Math.max(this.numHits, hitIndex * 2);
            getHits();
        }
        ScoreDoc doc = scoreDocs.get(hitIndex);
        String uuid = reader.document(doc.doc,
                FieldSelectors.UUID).get(FieldNames.UUID);
        NodeId id = new NodeId(uuid);
        return new ScoreNode(id, doc.score, doc.doc);
    }

    /**
     * Skips <code>n</code> hits.
     *
     * @param n the number of hits to skip.
     * @throws IOException if an error occurs while skipping.
     */
    public void skip(int n) throws IOException {
        hitIndex += n;
    }

    //-------------------------------< internal >-------------------------------

    private void getHits() throws IOException {
        TopFieldDocCollector collector = new TopFieldDocCollector(reader, sort, numHits);
        searcher.search(query, collector);
        this.size = collector.getTotalHits();
        ScoreDoc[] docs = collector.topDocs().scoreDocs;
        for (int i = scoreDocs.size(); i < docs.length; i++) {
            scoreDocs.add(docs[i]);
        }
        log.debug("getHits() {}/{}", scoreDocs.size(), numHits);
        // double hits for next round
        numHits *= 2;
    }
}
