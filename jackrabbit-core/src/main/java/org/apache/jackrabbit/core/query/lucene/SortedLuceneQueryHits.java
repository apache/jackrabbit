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

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopFieldDocCollector;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.index.IndexReader;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.uuid.UUID;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * Wraps a lucene query result and adds a close method that allows to release
 * resources after a query has been executed and the results have been read
 * completely.
 */
public final class SortedLuceneQueryHits extends AbstractQueryHits {

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
     * The score nodes.
     */
    private final List scoreNodes = new ArrayList();

    /**
     * The total number of hits.
     */
    private int size;

    /**
     * Number of hits to retrieve.
     */
    private int numHits = 50;

    /**
     * Creates a new <code>QueryHits</code> instance wrapping <code>hits</code>.
     *
     * @param reader the IndexReader in use.
     * @param searcher the index searcher.
     * @param query the query to execute.
     * @param sort the sort criteria.
     * @throws IOException if an error occurs while reading from the index.
     */
    public SortedLuceneQueryHits(IndexReader reader,
                                 JackrabbitIndexSearcher searcher,
                                 Query query,
                                 Sort sort) throws IOException {
        this.reader = reader;
        this.searcher = searcher;
        this.query = query;
        this.sort = sort;
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
        } else if (hitIndex >= scoreNodes.size()) {
            // refill
            getHits();
        }
        return (ScoreNode) scoreNodes.get(hitIndex);
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

    private int getHits() throws IOException {
        // double hits
        numHits *= 2;
        TopFieldDocCollector collector = new TopFieldDocCollector(reader, sort, numHits);
        searcher.search(query, collector);
        this.size = collector.getTotalHits();
        ScoreDoc[] docs = collector.topDocs().scoreDocs;
        int num = 0;
        for (int i = scoreNodes.size(); i < docs.length; i++) {
            String uuid = reader.document(docs[i].doc).get(FieldNames.UUID);
            NodeId id = new NodeId(UUID.fromString(uuid));
            scoreNodes.add(new ScoreNode(id, docs[i].score));
            num++;
        }
        return num;
    }
}
