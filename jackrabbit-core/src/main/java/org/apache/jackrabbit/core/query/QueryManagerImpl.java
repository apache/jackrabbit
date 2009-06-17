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
package org.apache.jackrabbit.core.query;

import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.qom.QueryObjectModelFactory;

import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.SearchManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.spi.commons.query.qom.QueryObjectModelFactoryImpl;
import org.apache.jackrabbit.spi.commons.query.qom.QueryObjectModelTree;

/**
 * This class implements the {@link QueryManager} interface.
 */
public class QueryManagerImpl implements QueryManager {

    /**
     * The <code>Session</code> for this QueryManager.
     */
    private final SessionImpl session;

    /**
     * The <code>ItemManager</code> of for item retrieval in search results
     */
    private final ItemManager itemMgr;

    /**
     * The <code>SearchManager</code> holding the search index.
     */
    private final SearchManager searchMgr;

    /**
     * The <code>QueryObjectModelFactory</code> for this query manager.
     */
    private final QueryObjectModelFactoryImpl qomFactory;

    /**
     * The query factory which is responsible to create query instances base
     * on the passed query language.
     */
    private final QueryFactory queryFactory;

    /**
     * Creates a new <code>QueryManagerImpl</code> for the passed
     * <code>session</code>
     *
     * @param session   the session for this query manager.
     * @param itemMgr   the item manager of the session.
     * @param searchMgr the search manager of this workspace.
     * @throws RepositoryException if an error occurs while initializing the
     *                             query manager.
     */
    public QueryManagerImpl(final SessionImpl session,
                            final ItemManager itemMgr,
                            final SearchManager searchMgr)
            throws RepositoryException {
        this.session = session;
        this.itemMgr = itemMgr;
        this.searchMgr = searchMgr;
        this.qomFactory = new QueryObjectModelFactoryImpl(
                session, session.getValueFactory()) {
            protected QueryObjectModel createQuery(QueryObjectModelTree qomTree)
                    throws InvalidQueryException, RepositoryException {
                return searchMgr.createQueryObjectModel(
                        session, qomTree, Query.JCR_SQL2);
            }
        };
        this.queryFactory = new CompoundQueryFactory(Arrays.asList(
                new QOMQueryFactory(qomFactory, session.getValueFactory()),
                new AQTQueryFactory() {
                    public Query createQuery(String statement, String language)
                            throws InvalidQueryException, RepositoryException {
                        return searchMgr.createQuery(session, itemMgr, statement, language);
                    }
                }));
    }

    /**
     * {@inheritDoc}
     */
    public Query createQuery(String statement, String language)
            throws InvalidQueryException, RepositoryException {
        sanityCheck();
        return queryFactory.createQuery(statement, language);
    }

    /**
     * {@inheritDoc}
     */
    public Query getQuery(Node node)
            throws InvalidQueryException, RepositoryException {
        sanityCheck();
        // TODO: support SQL2 and QOM
        return searchMgr.createQuery(session, itemMgr, node);
    }

    /**
     * {@inheritDoc}
     */
    public String[] getSupportedQueryLanguages() throws RepositoryException {
        List<String> languages = queryFactory.getSupportedLanguages();
        return languages.toArray(new String[languages.size()]);
    }

    //---------------------------< JSR 283 >------------------------------------

    /**
     * Returns a <code>QueryObjectModelFactory</code> with which a JCR-JQOM
     * query can be built programmatically.
     *
     * @return a <code>QueryObjectModelFactory</code> object
     * @since JCR 2.0
     */
    public QueryObjectModelFactory getQOMFactory() {
        return qomFactory;
    }

    //------------------------< testing only >----------------------------------

    /**
     * @return the query handler implementation.
     */
    QueryHandler getQueryHandler() {
        return searchMgr.getQueryHandler();
    }

    //---------------------------< internal >-----------------------------------

    /**
     * Checks if this <code>QueryManagerImpl</code> instance is still usable,
     * otherwise throws a {@link javax.jcr.RepositoryException}.
     *
     * @throws RepositoryException if this query manager is not usable anymore,
     *                             e.g. the corresponding session is closed.
     */
    private void sanityCheck() throws RepositoryException {
        if (!session.isLive()) {
            throw new RepositoryException("corresponding session has been closed");
        }
    }
}
