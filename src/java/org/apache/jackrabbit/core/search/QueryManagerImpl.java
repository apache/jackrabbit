/*
 * Copyright 2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.search;

import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.SearchManager;
import org.apache.jackrabbit.core.search.lucene.QueryImpl;

import javax.jcr.query.QueryManager;
import javax.jcr.query.Query;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.RepositoryException;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;

/**
 * This class implements the {@link javax.jcr.query.QueryManager} interface.
 *
 * @author Marcel Reutegger
 * @version $Revision:  $, $Date:  $
 */
public class QueryManagerImpl implements QueryManager {

    /** Defines all supported query languages */
    private static final String[] SUPPORTED_QUERIES = new String[] {
	Query.JCRQL, Query.XPATH_DOCUMENT_VIEW, Query.XPATH_SYSTEM_VIEW
    };

    /** List of all supported query languages */
    private static final List SUPPORTED_QUERIES_LIST
	    = Collections.unmodifiableList(Arrays.asList(SUPPORTED_QUERIES));

    /** The <code>Session</code> for this QueryManager. */
    private final SessionImpl session;

    /** The <code>ItemManager</code> of for item retrieval in search results */
    private final ItemManager itemMgr;

    /** The <code>SearchManager</code> holding the search index. */
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
     * @see QueryManager#createQuery(java.lang.String, java.lang.String)
     */
    public Query createQuery(String statement, String language)
	    throws InvalidQueryException, RepositoryException {

	return new QueryImpl(session, itemMgr, searchMgr, statement, language);
    }

    /**
     * @see QueryManager#getQuery(java.lang.String)
     */
    public Query getQuery(String absPath)
	    throws InvalidQueryException, RepositoryException {
	return new QueryImpl(session, itemMgr, searchMgr, absPath);
    }

    /**
     * @see QueryManager#getQueryByUUID(java.lang.String)
     */
    public Query getQueryByUUID(String uuid)
	    throws InvalidQueryException, RepositoryException {
	return getQuery(session.getNodeByUUID(uuid).getPath());
    }

    /**
     * @see QueryManager#getSupportedQueryLanguages()
     */
    public String[] getSupportedQueryLanguages() {
	return (String[])SUPPORTED_QUERIES_LIST.toArray(new String[SUPPORTED_QUERIES.length]);
    }
}
