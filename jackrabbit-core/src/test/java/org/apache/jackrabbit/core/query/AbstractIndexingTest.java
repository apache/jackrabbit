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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.jackrabbit.core.TestHelper;
import org.apache.jackrabbit.core.query.lucene.SearchIndex;

/**
 * <code>AbstractIndexingTest</code> is a base class for all indexing
 * configuration tests.
 */
public class AbstractIndexingTest extends AbstractQueryTest {

    private static final String WORKSPACE_NAME = "indexing-test";

    protected Session session;

    protected Node testRootNode;

    protected void setUp() throws Exception {
        super.setUp();
        session = getHelper().getSuperuserSession(getWorkspaceName());
        testRootNode = cleanUpTestRoot(session);
        // overwrite query manager
        qm = session.getWorkspace().getQueryManager();
    }

    protected void tearDown() throws Exception {
        if (session != null) {
            cleanUpTestRoot(session);
            session.logout();
            session = null;
        }
        testRootNode = null;
        super.tearDown();
    }

    protected String getWorkspaceName() {
        return WORKSPACE_NAME;
    }

    /**
     * wait for async text-extraction tasks to finish
     */
    protected void waitForTextExtractionTasksToFinish() throws Exception {
        TestHelper.waitForTextExtractionTasksToFinish(session);
        flushSearchIndex();
    }

    protected void flushSearchIndex() throws RepositoryException {
        SearchIndex si = getSearchIndex();
        if (si != null) {
            si.flush();
        }
    }

    /**
     * Returns a reference to the underlying search index.
     * 
     * @return the query handler inside the {@link #qm query manager}.
     */
    protected SearchIndex getSearchIndex() {
        if (qm instanceof QueryManagerImpl) {
            return (SearchIndex) ((QueryManagerImpl) qm).getQueryHandler();
        }
        return null;
    }

    /**
     * Returns a reference to the session's search index.
     * 
     * @return the session's query handler.
     */
    protected static SearchIndex getSearchIndex(Session session)
            throws RepositoryException {
        QueryManager qm = session.getWorkspace().getQueryManager();
        if (qm instanceof QueryManagerImpl) {
            return (SearchIndex) ((QueryManagerImpl) qm).getQueryHandler();
        }
        return null;
    }

    protected QueryResult executeQuery(String statement)
            throws RepositoryException {
        flushSearchIndex();
        return super.executeQuery(statement);
    }

    protected void executeXPathQuery(String xpath, Node[] nodes)
            throws RepositoryException {
        flushSearchIndex();
        super.executeXPathQuery(xpath, nodes);
    }

    protected void executeSQLQuery(String sql, Node[] nodes)
            throws RepositoryException {
        flushSearchIndex();
        super.executeSQLQuery(sql, nodes);
    }

    protected void executeSQL2Query(String statement, Node[] nodes)
            throws RepositoryException {
        flushSearchIndex();
        super.executeSQL2Query(statement, nodes);
    }
}
