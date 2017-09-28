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
package org.apache.jackrabbit.test.api.query;

import javax.jcr.query.Query;
import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.test.NotExecutableException;

/**
 * Test the method {@link Query#getLanguage()}.
 *
 */
public class GetLanguageTest extends AbstractQueryTest {

    /** A read-only session */
    private Session session;

    /**
     * Sets up the test cases
     */
    protected void setUp() throws Exception {
        isReadOnly = true;
        super.setUp();
        session = getHelper().getReadOnlySession();
        testRootNode = session.getRootNode().getNode(testPath);
    }

    /**
     * Releases the session acquired in setUp().
     */
    protected void tearDown() throws Exception {
        if (session != null) {
            session.logout();
            session = null;
        }
        super.tearDown();
    }

    /**
     * Tests if a XPath query returns {@link Query#XPATH} when calling
     * {@link Query#getLanguage()}.
     */
    public void testGetLanguage() throws RepositoryException {
        String statement = "/" + jcrRoot;
        Query q = session.getWorkspace().getQueryManager().createQuery(statement, qsXPATH);
        assertEquals("Query returns wrong language.", qsXPATH, q.getLanguage());
    }

    /**
     * Tests if a SQL query returns {@link Query#SQL} when calling
     * {@link Query#getLanguage()}.
     */
    public void testSQL() throws RepositoryException, NotExecutableException {
        if (isSupportedLanguage(qsSQL)) {
            String stmt = "select * from " + testNodeType;
            Query q = session.getWorkspace().getQueryManager().createQuery(stmt, qsSQL);
            assertEquals("Query returns wrong language.", qsSQL, q.getLanguage());
        } else {
            throw new NotExecutableException("SQL not supported");
        }
    }

    /**
     * Tests if a JCR_SQL2 query returns {@link Query#JCR_SQL2} when calling
     * {@link Query#getLanguage()}.
     */
    public void testJCRSQL2() throws RepositoryException {
        String stmt = "SELECT * FROM [" + testNodeType + "]";
        Query q = session.getWorkspace().getQueryManager().createQuery(stmt, Query.JCR_SQL2);
        assertEquals("Query returns wrong language.", Query.JCR_SQL2, q.getLanguage());
    }

    /**
     * Tests if a query object model returns {@link Query#JCR_JQOM} when calling
     * {@link Query#getLanguage()}.
     */
    public void testJCRQOM() throws RepositoryException {
        QueryObjectModel qom = qf.createQuery(
                qf.selector(testNodeType, "s"),
                null, null, null
        );
        assertEquals("Query returns wrong language.", Query.JCR_JQOM, qom.getLanguage());
    }
}
