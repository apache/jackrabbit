/*
 * Copyright 2005 The Apache Software Foundation.
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
package org.apache.jackrabbit.webdav.spi.search;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.*;
import org.apache.jackrabbit.webdav.search.SearchResource;
import org.apache.jackrabbit.webdav.search.QueryGrammerSet;
import org.apache.jackrabbit.webdav.search.SearchRequest;
import org.apache.jackrabbit.webdav.spi.JcrDavException;
import org.apache.jackrabbit.webdav.spi.ItemResourceConstants;

import javax.jcr.*;
import javax.jcr.query.*;

/**
 * <code>SearchResourceImpl</code>...
 */
public class SearchResourceImpl implements SearchResource, ItemResourceConstants {

    private static Logger log = Logger.getLogger(SearchResourceImpl.class);

    private final DavSession session;
    private final DavResourceLocator locator;

    public SearchResourceImpl(DavResourceLocator locator, DavSession session) {
        this.session = session;
        this.locator = locator;
    }

    //-------------------------------------------< SearchResource interface >---
    /**
     * @see SearchResource#getQueryGrammerSet()
     */
    public QueryGrammerSet getQueryGrammerSet()  {
        QueryGrammerSet qgs;
        try {
            QueryManager qMgr = session.getRepositorySession().getWorkspace().getQueryManager();
            String[] langs = qMgr.getSupportedQueryLanguages();
            qgs = new QueryGrammerSet(langs);
        } catch (RepositoryException e) {
            qgs = new QueryGrammerSet(new String[0]);
        }
        return qgs;
    }

    /**
     * Execute the query defined by the given <code>sRequest</code>.
     *
     * @see SearchResource#search(org.apache.jackrabbit.webdav.search.SearchRequest)
     */
    public MultiStatus search(SearchRequest sRequest) throws DavException {
        try {
            Query q = getQuery(sRequest);
            QueryResult qR = q.execute();
            return queryResultToMultiStatus(qR);

        } catch (RepositoryException e) {
            throw new JcrDavException(e);
        }
    }

    /**
     * Create a query from the information present in the <code>sRequest</code>
     * object.<br>The following JCR specific logic is applied:
     * <ul>
     * <li>If the requested resource represents a node with nodetype nt:query, the
     * request body is ignored and the query defined with the node is executed
     * instead.</li>
     * <li>If the requested resource does not represent an existing item, the
     * specified query is persisted by calling {@link Query#save(String)}.</li>
     * </ul>
     * @param sRequest defining the query to be executed
     * @return <code>Query</code> object.
     * @throws InvalidQueryException if the query defined by <code>sRequest</code> is invalid
     * @throws RepositoryException the query manager cannot be accessed or if
     * another error occurs.
     * @throws DavException if <code>sRequest</code> is <code>null</code> and
     * the underlaying repository item is not an nt:query node or if an error
     * occurs when calling {@link Query#save(String)}/
     */
    private Query getQuery(SearchRequest sRequest)
            throws InvalidQueryException, RepositoryException, DavException {

        Node rootNode = session.getRepositorySession().getRootNode();
        QueryManager qMgr = session.getRepositorySession().getWorkspace().getQueryManager();
        String resourcePath = locator.getResourcePath();

        // test if query is defined by requested repository node
        if (!rootNode.getPath().equals(resourcePath)) {
            String qNodeRelPath = resourcePath.substring(1);
            if (rootNode.hasNode(qNodeRelPath)) {
                Node qNode = rootNode.getNode(qNodeRelPath);
                if (qNode.isNodeType("nt:query")) {
                    return qMgr.getQuery(qNode);
                }
            }
        }

        Query q;
        if (sRequest != null) {
            q = qMgr.createQuery(sRequest.getQuery(), sRequest.getLanguageName());
        } else {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST, resourcePath + " is not a nt:query node -> searchRequest body required.");
        }

        /* test if resource path does not exist -> thus indicating that
        the query must be made persistent by calling Query.save(String) */
        if (!session.getRepositorySession().itemExists(resourcePath)) {
            try {
                q.save(resourcePath);
            } catch (RepositoryException e) {
                // ItemExistsException should never occur.
                new JcrDavException(e);
            }
        }
        return q;
    }

    /**
     * Build a <code>MultiStatus</code> object from the specified query result.
     *
     * @param qResult <code>QueryResult</code> as obtained from {@link javax.jcr.query.Query#execute()}.
     * @return <code>MultiStatus</code> object listing the query result in
     * Webdav compatible form.
     * @throws RepositoryException
     */
    private MultiStatus queryResultToMultiStatus(QueryResult qResult)
            throws RepositoryException {
        MultiStatus ms = new MultiStatus();

        String[] propertyNames = qResult.getPropertyNames();
        RowIterator rowIter = qResult.getRows();
        while (rowIter.hasNext()) {
            Row row = rowIter.nextRow();
            Value[] values = row.getValues();
            String nodePath = values[0].getString();

            // create a new ms-response for each row of the result set 
            DavResourceLocator loc = locator.getFactory().createResourceLocator(locator.getPrefix(), locator.getWorkspacePath(), nodePath);
            String nodeHref = loc.getHref(true);
            MultiStatusResponse resp = new MultiStatusResponse(nodeHref);
            // add a search-result-property for each value column
            for (int i = 0; i < values.length; i++) {
                resp.add(new SearchResultProperty(propertyNames[i], values[i]));
            }
            ms.addResponse(resp);
        }
        return ms;
    }
}