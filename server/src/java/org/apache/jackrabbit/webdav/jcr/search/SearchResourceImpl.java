/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
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
package org.apache.jackrabbit.webdav.jcr.search;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.xml.Namespace;
import org.apache.jackrabbit.webdav.search.SearchResource;
import org.apache.jackrabbit.webdav.search.QueryGrammerSet;
import org.apache.jackrabbit.webdav.search.SearchInfo;
import org.apache.jackrabbit.webdav.jcr.JcrDavException;
import org.apache.jackrabbit.webdav.jcr.JcrDavSession;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.JcrConstants;

import javax.jcr.query.QueryManager;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.RowIterator;
import javax.jcr.query.Row;
import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.Value;
import javax.jcr.Session;

/**
 * <code>SearchResourceImpl</code>...
 */
public class SearchResourceImpl implements SearchResource {

    private static Logger log = Logger.getLogger(SearchResourceImpl.class);

    private final JcrDavSession session;
    private final DavResourceLocator locator;

    public SearchResourceImpl(DavResourceLocator locator, JcrDavSession session) {
        this.session = session;
        this.locator = locator;
    }

    //-------------------------------------------< SearchResource interface >---
    /**
     * @see SearchResource#getQueryGrammerSet()
     */
    public QueryGrammerSet getQueryGrammerSet()  {
        QueryGrammerSet qgs = new QueryGrammerSet();
        try {
            QueryManager qMgr = getRepositorySession().getWorkspace().getQueryManager();
            String[] langs = qMgr.getSupportedQueryLanguages();
            for (int i = 0; i < langs.length; i++) {
                // todo: define proper namespace
                qgs.addQueryLanguage(langs[i], Namespace.EMPTY_NAMESPACE);
            }
        } catch (RepositoryException e) {
            log.debug(e.getMessage());
        }
        return qgs;
    }

    /**
     * Execute the query defined by the given <code>sInfo</code>.
     *
     * @see SearchResource#search(org.apache.jackrabbit.webdav.search.SearchInfo)
     */
    public MultiStatus search(SearchInfo sInfo) throws DavException {
        try {
            Query q = getQuery(sInfo);
            QueryResult qR = q.execute();
            return queryResultToMultiStatus(qR);

        } catch (RepositoryException e) {
            throw new JcrDavException(e);
        }
    }

    /**
     * Create a query from the information present in the <code>sInfo</code>
     * object.<br>The following JCR specific logic is applied:
     * <ul>
     * <li>If the requested resource represents a node with nodetype nt:query, the
     * request body is ignored and the query defined with the node is executed
     * instead.</li>
     * <li>If the requested resource does not represent an existing item, the
     * specified query is persisted by calling {@link Query#storeAsNode(String)}.</li>
     * </ul>
     * @param sInfo defining the query to be executed
     * @return <code>Query</code> object.
     * @throws javax.jcr.query.InvalidQueryException if the query defined by <code>sInfo</code> is invalid
     * @throws RepositoryException the query manager cannot be accessed or if
     * another error occurs.
     * @throws DavException if <code>sInfo</code> is <code>null</code> and
     * the underlying repository item is not an nt:query node or if an error
     * occurs when calling {@link Query#storeAsNode(String)}/
     */
    private Query getQuery(SearchInfo sInfo)
            throws InvalidQueryException, RepositoryException, DavException {

        Node rootNode = getRepositorySession().getRootNode();
        QueryManager qMgr = getRepositorySession().getWorkspace().getQueryManager();

        // test if query is defined by requested repository node
        String itemPath = locator.getRepositoryPath();
        if (!rootNode.getPath().equals(itemPath)) {
            String qNodeRelPath = itemPath.substring(1);
            if (rootNode.hasNode(qNodeRelPath)) {
                Node qNode = rootNode.getNode(qNodeRelPath);
                if (qNode.isNodeType(JcrConstants.NT_QUERY)) {
                    return qMgr.getQuery(qNode);
                }
            }
        }

        Query q;
        if (sInfo != null) {
            q = qMgr.createQuery(sInfo.getQuery(), sInfo.getLanguageName());
        } else {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST, locator.getResourcePath() + " is not a nt:query node -> searchRequest body required.");
        }

        /* test if resource path does not exist -> thus indicating that
        the query must be made persistent by calling Query.save(String) */
        if (!getRepositorySession().itemExists(itemPath)) {
            try {
                q.storeAsNode(itemPath);
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

        String[] columnNames = qResult.getColumnNames();
        RowIterator rowIter = qResult.getRows();
        while (rowIter.hasNext()) {
            Row row = rowIter.nextRow();
            Value[] values = row.getValues();

            // get the jcr:path column indicating the node path and build
            // a webdav compliant resource path of it.
            String itemPath = row.getValue(JcrConstants.JCR_PATH).getString();
            // create a new ms-response for this row of the result set
            DavResourceLocator loc = locator.getFactory().createResourceLocator(locator.getPrefix(), locator.getWorkspacePath(), itemPath, false);
            String href = loc.getHref(true);
            MultiStatusResponse resp = new MultiStatusResponse(href, null);
            // build the s-r-property
            SearchResultProperty srp = new SearchResultProperty(columnNames, values);
            resp.add(srp);
            ms.addResponse(resp);
        }
        return ms;
    }

    /**
     * @return
     */
    private Session getRepositorySession() {
        return session.getRepositorySession();
    }
}