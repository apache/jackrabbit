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

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.index.IndexReader;
import org.apache.jackrabbit.core.SessionImpl;

import javax.jcr.RepositoryException;
import java.io.IOException;

/**
 * <code>JackrabbitIndexSearcher</code> implements an index searcher with
 * jackrabbit specific optimizations.
 */
public class JackrabbitIndexSearcher extends IndexSearcher {

    /**
     * The session that executes the query.
     */
    private final SessionImpl session;

    /**
     * The underlying index reader.
     */
    private final IndexReader reader;

    /**
     * Creates a new jackrabbit index searcher.
     *
     * @param s the session that executes the query.
     * @param r the index reader.
     */
    public JackrabbitIndexSearcher(SessionImpl s, IndexReader r) {
        super(r);
        this.session = s;
        this.reader = r;
    }

    /**
     * Executes the query and returns the hits that match the query.
     *
     * @param query the query to execute.
     * @param sort  the sort criteria.
     * @return the query hits.
     * @throws IOException if an error occurs while executing the query.
     */
    public QueryHits execute(Query query, Sort sort) throws IOException {
        // optimize certain queries
        if (sort.getSort().length == 0) {
            query = query.rewrite(reader);
            if (query instanceof MatchAllDocsQuery) {
                try {
                    return new NodeTraversingQueryHits(
                            session.getRootNode(), true);
                } catch (RepositoryException e) {
                    IOException ex = new IOException(e.getMessage());
                    ex.initCause(e);
                    throw ex;
                }
            }
        }
        return new LuceneQueryHits(search(query, sort), reader);
    }
}
