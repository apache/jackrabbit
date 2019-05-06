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

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopFieldCollector;
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
     * The index searcher.
     */
    private final IndexSearcher searcher;

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
    private ScoreDoc[] scoreDocs = new ScoreDoc[0];

    /**
     * The total number of hits.
     */
    private int size;

    /**
     * Number of hits to be pre-fetched from the lucene index (will be around 2x
     * resultFetchHint).
     */
    private int numHits;

    private int offset = 0;

    /**
     * Creates a new <code>QueryHits</code> instance wrapping <code>hits</code>.
     * 
     * @param searcher
     *            the index searcher.
     * @param query
     *            the query to execute.
     * @param sort
     *            the sort criteria.
     * @param resultFetchHint
     *            a hint on how many results should be pre-fetched from the
     *            lucene index.
     * @throws IOException
     *             if an error occurs while reading from the index.
     */
    public SortedLuceneQueryHits(IndexSearcher searcher, Query query,
            Sort sort, long resultFetchHint) throws IOException {
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
        } else if (hitIndex - offset >= scoreDocs.length) {
            // refill at least numHits or twice hitIndex
            this.numHits = Math.max(this.numHits, hitIndex * 2);
            getHits();
        }
        ScoreDoc doc = scoreDocs[hitIndex - offset];
        String uuid = searcher.doc(doc.doc,
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
    	long time = System.nanoTime();
        TopFieldCollector collector = TopFieldCollector.create(sort, numHits, false, true, false, false);
        searcher.search(query, collector);
        size = collector.getTotalHits();
        offset += scoreDocs.length;
        scoreDocs = collector.topDocs(offset, numHits).scoreDocs;
        time = System.nanoTime() - time;
        final long timeMs = time / 1000000;
        log.debug("getHits() in {} ms. {}/{}/{}", new Object[] {timeMs, scoreDocs.length, numHits, size});
        // double hits for next round
        numHits *= 2;
    }
}
