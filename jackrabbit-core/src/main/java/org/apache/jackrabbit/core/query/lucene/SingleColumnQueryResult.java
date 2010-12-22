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

import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.query.qom.ColumnImpl;
import org.apache.lucene.search.Query;

import javax.jcr.RepositoryException;
import java.io.IOException;

/**
 * <code>SingleColumnQueryResult</code> implements a query result that returns
 * a single column. That is, executes a lucene query.
 */
public class SingleColumnQueryResult extends QueryResultImpl {

    /**
     * The query to execute.
     */
    private final Query query;

    /**
     * The relative paths of properties to use for ordering the result set.
     */
    protected final Path[] orderProps;

    /**
     * The order specifier for each of the order properties.
     */
    protected final boolean[] orderSpecs;

    /**
     * Function for each of the order properties (or <code>null</code> if none).
     */
    private String[] orderFuncs;

    public SingleColumnQueryResult(
            SearchIndex index, SessionContext sessionContext,
            AbstractQueryImpl queryImpl, Query query,
            SpellSuggestion spellSuggestion, ColumnImpl[] columns,
            Path[] orderProps, boolean[] orderSpecs, String[] orderFuncs, boolean documentOrder,
            long offset, long limit) throws RepositoryException {
        super(index, sessionContext, queryImpl, spellSuggestion,
                columns, documentOrder, offset, limit);
        this.query = query;
        this.orderProps = orderProps;
        this.orderSpecs = orderSpecs;
        this.orderFuncs = orderFuncs;
        // if document order is requested get all results right away
        getResults(docOrder ? Integer.MAX_VALUE : index.getResultFetchSize());
    }

    /**
     * {@inheritDoc}
     */
    protected MultiColumnQueryHits executeQuery(long resultFetchHint)
            throws IOException {
        return index.executeQuery(
                sessionContext.getSessionImpl(), queryImpl, query,
                orderProps, orderSpecs, orderFuncs, resultFetchHint);
    }

    /**
     * {@inheritDoc}
     */
    protected ExcerptProvider createExcerptProvider() throws IOException {
        return index.createExcerptProvider(query);
    }

}
