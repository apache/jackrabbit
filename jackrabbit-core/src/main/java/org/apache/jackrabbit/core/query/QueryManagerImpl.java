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

import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.SearchManager;
import org.apache.jackrabbit.core.SessionImpl;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This class implements the {@link QueryManager} interface.
 */
public class QueryManagerImpl implements QueryManager {

    /**
     * Defines all supported query languages
     */
    private static final String[] SUPPORTED_QUERIES = QueryTreeBuilderRegistry.getSupportedLanguages();

    /**
     * List of all supported query languages
     */
    private static final List SUPPORTED_QUERIES_LIST
            = Collections.unmodifiableList(Arrays.asList(SUPPORTED_QUERIES));

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
     * Creates a new <code>QueryManagerImpl</code> for the passed
     * <code>session</code>
     *
     * @param session
     * @param itemMgr
     * @param searchMgr
     */
    public QueryManagerImpl(SessionImpl session,
                            ItemManager itemMgr,
                            SearchManager searchMgr) {
        this.session = session;
        this.itemMgr = itemMgr;
        this.searchMgr = searchMgr;
    }

    /**
     * {@inheritDoc}
     */
    public Query createQuery(String statement, String language)
            throws InvalidQueryException, RepositoryException {
        sanityCheck();
        return searchMgr.createQuery(session, itemMgr, statement, language);
    }

    /**
     * {@inheritDoc}
     */
    public Query getQuery(Node node)
            throws InvalidQueryException, RepositoryException {
        sanityCheck();
        return searchMgr.createQuery(session, itemMgr, node);
    }

    /**
     * {@inheritDoc}
     */
    public String[] getSupportedQueryLanguages() throws RepositoryException {
        return (String[]) SUPPORTED_QUERIES_LIST.toArray(new String[SUPPORTED_QUERIES.length]);
    }

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
