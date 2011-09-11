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

import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.query.lucene.constraint.EvaluationContext;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.spi.Name;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;

/**
 * <code>JackrabbitIndexSearcher</code> implements an index searcher with
 * jackrabbit specific optimizations.
 */
public class JackrabbitIndexSearcher
        extends IndexSearcher
        implements EvaluationContext {

    /**
     * The session that executes the query.
     */
    private final SessionImpl session;

    /**
     * The underlying index reader.
     */
    private final IndexReader reader;

    /**
     * The item state manager of the workspace.
     */
    private final ItemStateManager ism;

    /**
     * Creates a new jackrabbit index searcher.
     *
     * @param s the session that executes the query.
     * @param r the index reader.
     * @param ism the shared item state manager.
     */
    public JackrabbitIndexSearcher(SessionImpl s,
                                   IndexReader r,
                                   ItemStateManager ism) {
        super(r);
        this.session = s;
        this.reader = r;
        this.ism = ism;
    }

    /**
     * Executes the query and returns the hits that match the query.
     *
     * @param query           the query to execute.
     * @param sort            the sort criteria.
     * @param resultFetchHint a hint on how many results should be fetched.
     * @param selectorName    the single selector name for the query hits.
     * @return the query hits.
     * @throws IOException if an error occurs while executing the query.
     */
    public MultiColumnQueryHits execute(Query query,
                                        Sort sort,
                                        long resultFetchHint,
                                        Name selectorName)
            throws IOException {
        return new QueryHitsAdapter(
                evaluate(query, sort, resultFetchHint), selectorName);
    }

    /**
     * Evaluates the query and returns the hits that match the query.
     *
     * @param query           the query to execute.
     * @param sort            the sort criteria.
     * @param resultFetchHint a hint on how many results should be fetched.
     * @return the query hits.
     * @throws IOException if an error occurs while executing the query.
     */
    public QueryHits evaluate(Query query, Sort sort, long resultFetchHint)
            throws IOException {
        query = query.rewrite(reader);
        QueryHits hits = null;
        if (query instanceof JackrabbitQuery) {
            hits = ((JackrabbitQuery) query).execute(this, session, sort);
        }
        if (hits == null) {
            if (sort.getSort().length == 0) {
                hits = new LuceneQueryHits(reader, this, query);
            } else {
                hits = new SortedLuceneQueryHits(this, query, sort,
                        resultFetchHint);
            }
        }
        return hits;
    }

    //---------------------------< IndexSearcher >------------------------------

    @Override
    public int docFreq(Term term) throws IOException {
        // provide a fixed document frequency for fields that are not fulltext
        // indexed. correct frequency is only useful for fulltext queries.
        if (FieldNames.isFulltextField(term.field())) {
            return super.docFreq(term);
        } else {
            return 1;
        }
    }

    //------------------------< EvaluationContext >-----------------------------

    /**
     * Evaluates the query and returns the hits that match the query.
     *
     * @param query           the query to execute.
     * @return the query hits.
     * @throws IOException if an error occurs while executing the query.
     */
    public QueryHits evaluate(Query query) throws IOException {
        return evaluate(query, new Sort(), Integer.MAX_VALUE);
    }

    /**
     * @return session that executes the query.
     */
    public SessionImpl getSession() {
        return session;
    }

    /**
     * @return the item state manager of the workspace.
     */
    public ItemStateManager getItemStateManager() {
        return ism;
    }
}
