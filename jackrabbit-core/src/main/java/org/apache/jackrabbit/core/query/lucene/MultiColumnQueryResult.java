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

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.spi.commons.query.qom.ColumnImpl;
import org.apache.jackrabbit.spi.commons.query.qom.OrderingImpl;

/**
 * <code>MultiColumnQueryResult</code> implements a query result that executes
 * a {@link MultiColumnQuery}.
 */
public class MultiColumnQueryResult extends QueryResultImpl {

    /**
     * The query to execute.
     */
    private final MultiColumnQuery query;

    /**
     * The order specifier for each of the order properties.
     */
    protected final Ordering[] orderings;

    public MultiColumnQueryResult(
            SearchIndex index, SessionContext sessionContext,
            AbstractQueryImpl queryImpl, MultiColumnQuery query,
            SpellSuggestion spellSuggestion,  ColumnImpl[] columns,
            OrderingImpl[] orderings, boolean documentOrder,
            long offset, long limit) throws RepositoryException {
        super(index, sessionContext, queryImpl, spellSuggestion,
                columns, documentOrder, offset, limit);
        this.query = query;
        this.orderings = index.createOrderings(orderings);
        // if document order is requested get all results right away
        getResults(docOrder ? Integer.MAX_VALUE : index.getResultFetchSize());
    }

    /**
     * {@inheritDoc}
     */
    protected MultiColumnQueryHits executeQuery(long resultFetchHint)
            throws IOException {
        return index.executeQuery(
                sessionContext.getSessionImpl(),
                query, orderings, resultFetchHint);
    }

    /**
     * {@inheritDoc}
     */
    protected ExcerptProvider createExcerptProvider() throws IOException {
        // TODO
        return null;
    }
}
