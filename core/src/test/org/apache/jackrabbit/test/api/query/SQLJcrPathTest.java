/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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
package org.apache.jackrabbit.test.api.query;

import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

/**
 * Tests if the jcr:path property is returned at the correct position in the
 * query result.
 *
 * @test
 * @sources SQLJcrPathTest.java
 * @executeClass org.apache.jackrabbit.test.api.query.SQLJcrPathTest
 * @keywords sql
 */
public class SQLJcrPathTest extends AbstractQueryTest {

    /** A read-only session */
    private Session session;

    /**
     * Sets up the test cases
     */
    protected void setUp() throws Exception {
        isReadOnly = true;
        super.setUp();
        session = helper.getReadOnlySession();
        testRootNode = session.getRootNode().getNode(testPath);
    }

    /**
     * Releases the session acquired in setUp().
     */
    protected void tearDown() throws Exception {
        if (session != null) {
            session.logout();
        }
        super.tearDown();
    }
    
    /**
     * Verify that the jcr:path is the last property from the found property
     * names when the query statement does not use a contains function.
     */
    public void testJcrPath() throws RepositoryException, NotExecutableException {
        String nodeTypeName = session.getRootNode().getPrimaryNodeType().getName();
        String queryStatement = "select * from " + nodeTypeName;

        // execute the search query
        Query query = session.getWorkspace().getQueryManager().createQuery(queryStatement, Query.SQL);
        QueryResult result = query.execute();

        String[] propNames = result.getColumnNames();
        if (propNames.length > 0) {
            // jcr:path should be the last column
            assertEquals(jcrPath + " should be the last property", jcrPath, propNames[propNames.length - 1]);
        }
    }
}