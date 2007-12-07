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

import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import java.util.Arrays;

/**
 * Tests if the jcr:path property is returned in the query result.
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
            session = null;
        }
        super.tearDown();
    }
    
    /**
     * Verify that the jcr:path is present in the query result.
     */
    public void testJcrPath() throws RepositoryException, NotExecutableException {
        String nodeTypeName = session.getRootNode().getPrimaryNodeType().getName();
        String queryStatement = "select * from " + nodeTypeName;

        // execute the search query
        Query query = session.getWorkspace().getQueryManager().createQuery(queryStatement, Query.SQL);
        QueryResult result = query.execute();

        assertTrue("jcr:path must be present in query result row",
                Arrays.asList(result.getColumnNames()).contains(jcrPath));
    }
}