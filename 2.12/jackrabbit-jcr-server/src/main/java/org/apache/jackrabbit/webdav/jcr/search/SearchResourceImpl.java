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
package org.apache.jackrabbit.webdav.jcr.search;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.jcr.ItemResourceConstants;
import org.apache.jackrabbit.webdav.jcr.JcrDavException;
import org.apache.jackrabbit.webdav.jcr.JcrDavSession;
import org.apache.jackrabbit.webdav.search.QueryGrammerSet;
import org.apache.jackrabbit.webdav.search.SearchInfo;
import org.apache.jackrabbit.webdav.search.SearchResource;
import org.apache.jackrabbit.webdav.xml.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.util.ISO9075;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.NamespaceRegistry;
import javax.jcr.ValueFactory;
import javax.jcr.PropertyType;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * <code>SearchResourceImpl</code>...
 */
public class SearchResourceImpl implements SearchResource {

    private static Logger log = LoggerFactory.getLogger(SearchResourceImpl.class);

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
            for (String lang : langs) {
                // Note: Existing clients already assume that the
                // query languages returned in the DASL header are
                // not prefixed with any namespace, so we probably
                // shouldn't use an explicit namespace here.
                qgs.addQueryLanguage(lang, Namespace.EMPTY_NAMESPACE);
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
            QueryResult result = getQuery(sInfo).execute();

            MultiStatus ms = new MultiStatus();

            if (ItemResourceConstants.NAMESPACE.equals(
                    sInfo.getLanguageNameSpace())) {
                ms.setResponseDescription(
                        "Columns: " + encode(result.getColumnNames())
                        + "\nSelectors: " + encode(result.getSelectorNames()));
            } else {
                ms.setResponseDescription(encode(result.getColumnNames()));
            }

            queryResultToMultiStatus(result, ms);

            return ms;
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

        Session session = getRepositorySession();
        NamespaceRegistry nsReg = session.getWorkspace().getNamespaceRegistry();
        Node rootNode = session.getRootNode();
        QueryManager qMgr = getRepositorySession().getWorkspace().getQueryManager();

        // test if query is defined by requested repository node
        String itemPath = locator.getRepositoryPath();
        if (itemPath != null && !rootNode.getPath().equals(itemPath)) {
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
            // apply namespace mappings to session
            Map<String, String> namespaces = sInfo.getNamespaces();
            try {
                for (Map.Entry<String, String> entry : namespaces.entrySet()) {
                    String prefix = entry.getKey();
                    String uri = entry.getValue();
                    session.setNamespacePrefix(prefix, uri);
                }
                q = qMgr.createQuery(sInfo.getQuery(), sInfo.getLanguageName());

                if (SearchInfo.NRESULTS_UNDEFINED != sInfo.getNumberResults()) {
                    q.setLimit(sInfo.getNumberResults());
                }
                if (SearchInfo.OFFSET_UNDEFINED != sInfo.getOffset()) {
                    q.setOffset(sInfo.getOffset());
                }
            } finally {
                // reset namespace mappings
                for (String uri : namespaces.values()) {
                    try {
                        session.setNamespacePrefix(nsReg.getPrefix(uri), uri);
                    } catch (RepositoryException e) {
                        log.warn("Unable to reset mapping of namespace: " + uri);
                    }
                }
            }
        } else {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST, locator.getResourcePath() + " is not a nt:query node -> searchRequest body required.");
        }

        /* test if resource path does not exist -> thus indicating that
        the query must be made persistent by calling Query.save(String) */
        if (itemPath != null && !getRepositorySession().itemExists(itemPath)) {
            try {
                q.storeAsNode(itemPath);
            } catch (RepositoryException e) {
                // ItemExistsException should never occur.
                throw new JcrDavException(e);
            }
        }
        return q;
    }

    /**
     * Build a <code>MultiStatus</code> object from the specified query result.
     *
     * @param query the query to execute.
     * @return <code>MultiStatus</code> object listing the query result in
     * Webdav compatible form.
     * @throws RepositoryException if an error occurs.
     */
    private void queryResultToMultiStatus(QueryResult result, MultiStatus ms)
            throws RepositoryException {
        List<String> columnNames = new ArrayList<String>();

        ValueFactory vf = getRepositorySession().getValueFactory();
        List<RowValue> descr = new ArrayList<RowValue>();
        for (String columnName : result.getColumnNames()) {
            if (!isPathOrScore(columnName)) {
                columnNames.add(columnName);
                descr.add(new PlainValue(columnName, null, vf));
            }
        }

        // add path and score for each selector
        String[] sns = result.getSelectorNames();
        boolean join = sns.length > 1;
        for (String selectorName : sns) {
            descr.add(new PathValue(JcrConstants.JCR_PATH, selectorName, vf));
            columnNames.add(JcrConstants.JCR_PATH);
            descr.add(new ScoreValue(JcrConstants.JCR_SCORE, selectorName, vf));
            columnNames.add(JcrConstants.JCR_SCORE);
        }

        int n = 0;
        String root = getHref("/");
        String[] selectorNames = createSelectorNames(descr);
        String[] colNames = columnNames.toArray(new String[columnNames.size()]);
        RowIterator rowIter = result.getRows();
        while (rowIter.hasNext()) {
            Row row = rowIter.nextRow();
            List<Value> values = new ArrayList<Value>();
            for (RowValue rv : descr) {
                values.add(rv.getValue(row));
            }

            // create a new ms-response for this row of the result set
            String href;
            if (join) {
                // We need a distinct href for each join result row to
                // allow the MultiStatus response to keep them separate
                href = root + "?" + n++;
            } else {
                href = getHref(row.getPath());
            }
            MultiStatusResponse resp = new MultiStatusResponse(href, null);

            // build the s-r-property
            SearchResultProperty srp = new SearchResultProperty(colNames,
                    selectorNames, values.toArray(new Value[values.size()]));
            resp.add(srp);
            ms.addResponse(resp);
        }
    }

    /**
     * Returns the resource location of the given query result row.
     * The result rows of join queries have no meaningful single resource
     * location, so we'll just default to the root node for all such rows.
     *
     * @param row query result row
     * @param join flag to indicate a join query
     * @return resource location of the row
     */
    private String getHref(String path) throws RepositoryException {
        DavResourceLocator l = locator.getFactory().createResourceLocator(
                locator.getPrefix(), locator.getWorkspacePath(), path, false);
        return l.getHref(true);
    }

    private String encode(String[] names) {
        StringBuilder builder = new StringBuilder();
        String delim = "";
        for (String name : names) {
            builder.append(delim);
            builder.append(ISO9075.encode(name));
            delim = " ";
        }
        return builder.toString();
    }

    private static String[] createSelectorNames(Iterable<RowValue> rows)
            throws RepositoryException {
        List<String> sn = new ArrayList<String>();
        for (RowValue rv : rows) {
            sn.add(rv.getSelectorName());
        }
        return sn.toArray(new String[sn.size()]);
    }

    /**
     * @param columnName a column name.
     * @return <code>true</code> if <code>columnName</code> is either
     *         <code>jcr:path</code> or <code>jcr:score</code>;
     *         <code>false</code> otherwise.
     */
    private static boolean isPathOrScore(String columnName) {
        return JcrConstants.JCR_PATH.equals(columnName)
                || JcrConstants.JCR_SCORE.equals(columnName);
    }

    /**
     * @return the session associated with this resource.
     */
    private Session getRepositorySession() {
        return session.getRepositorySession();
    }

    private interface RowValue {

        public Value getValue(Row row) throws RepositoryException;

        public String getColumnName() throws RepositoryException;

        public String getSelectorName() throws RepositoryException;
    }

    private static final class PlainValue extends SelectorValue {

        public PlainValue(String columnName,
                          String selectorName,
                          ValueFactory vf) {
            super(columnName, selectorName, vf);
        }

        public Value getValue(Row row) throws RepositoryException {
            return row.getValue(columnName);
        }
    }

    private static abstract class SelectorValue implements RowValue {

        protected final String columnName;

        protected final String selectorName;

        protected final ValueFactory vf;

        public SelectorValue(String columnName,
                             String selectorName,
                             ValueFactory vf) {
            this.columnName = columnName;
            this.selectorName = selectorName;
            this.vf = vf;
        }

        public String getColumnName() throws RepositoryException {
            return columnName;
        }

        public String getSelectorName() throws RepositoryException {
            return selectorName;
        }
    }

    private static final class ScoreValue extends SelectorValue {

        public ScoreValue(String columnName,
                          String selectorName,
                          ValueFactory vf) {
            super(columnName, selectorName, vf);
        }

        public Value getValue(Row row) throws RepositoryException {
            double score;
            if (selectorName != null) {
                score = row.getScore(selectorName);
            } else {
                score = row.getScore();
            }
            return vf.createValue(score);
        }
    }

    private static final class PathValue extends SelectorValue {

        public PathValue(String columnName,
                         String selectorName,
                         ValueFactory vf) {
            super(columnName, selectorName, vf);
        }

        public Value getValue(Row row) throws RepositoryException {
            String path;
            if (selectorName != null) {
                path = row.getPath(selectorName);
            } else {
                path = row.getPath();
            }
            return (path == null) ? null : vf.createValue(path, PropertyType.PATH);
        }
    }

}